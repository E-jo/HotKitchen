package hotkitchen.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import hotkitchen.audience
import hotkitchen.issuer
import hotkitchen.secret
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Application.configureRouting() {
    val json = Json { prettyPrint = true }

    Database.connect(
        "jdbc:postgresql://localhost:5432/hotkitchen",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "1111"
    )

    transaction {
        SchemaUtils.create(ApiUser, UserProfile, MealTable, CategoryTable, OrderTable)
    }

    routing {
        post("/signup") {
            val contentType = call.request.contentType()
            println("Content type of the request: $contentType")
            val signUpRequest = when {
                contentType.match(ContentType.Application.Json) -> {
                    println("Found JSON")

                    val requestBody = call.receive<String>()
                    println("Request Body: $requestBody")

                    try {
                        Json.decodeFromString<SignUpRequest>(requestBody)
                    } catch (e: UnsupportedMediaTypeException) {
                        println("Unsupported media type for JSON: ${e.message}")
                        null
                    } catch (e: Throwable) {
                        println("Error parsing JSON request: ${e.message}")
                        null
                    }
                }
                contentType.match(ContentType.MultiPart.FormData) -> {
                    println("Found form data parameters")
                    val params = call.receiveParameters()
                    SignUpRequest(
                        email = params["email"] ?: "",
                        userType = params["user_type"] ?: "",
                        password = params["password"] ?: ""
                    )
                }
                else -> null
            }

            println("Sign Up Request: $signUpRequest")

            if (signUpRequest == null || signUpRequest.userType.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
                return@post
            }

            val userEmail = signUpRequest.email
            val userType = signUpRequest.userType
            val password = signUpRequest.password

            var findUser: ResultRow? = null
            transaction {
                findUser = ApiUser.select { ApiUser.email eq userEmail }.firstOrNull()
                println("Found user $userEmail: ${findUser != null}")
            }

            if (findUser != null) {
                val response = Response("User already exists")
                println("User already exists")
                call.response.status(HttpStatusCode.Forbidden)
                call.respondText(Json.encodeToString(response))
            } else {
                if (!userEmail.matches(Regex(
                        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"))) {
                    val response = Response("Invalid email")
                    println("Invalid email")
                    call.response.status(HttpStatusCode.Forbidden)
                    call.respondText(Json.encodeToString(response))
                } else if (!password.contains(Regex("[a-zA-Z]")) ||
                        !password.contains(Regex("[0-9]")) ||
                        password.length < 6) {
                    val response = Response("Invalid password: $password")
                    println("Invalid password: $password")
                    call.response.status(HttpStatusCode.Forbidden)
                    call.respondText(Json.encodeToString(response))
                } else {
                    transaction {
                        ApiUser.insert { user ->
                            user[email] = userEmail
                            user[ApiUser.userType] = userType
                            user[ApiUser.password] = password
                        }
                        UserProfile.insert { profile ->
                            profile[name] = ""
                            profile[UserProfile.userType] = userType
                            profile[phone] = ""
                            profile[email] = userEmail
                            profile[address] = ""
                        }
                    }
                    val token = JWT.create()
                        .withAudience(audience)
                        .withIssuer(issuer)
                        .withClaim("username", userEmail)
                        .withClaim("usertype", userType)
                        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60000))
                        .sign(Algorithm.HMAC256(secret))

                    call.response.status(HttpStatusCode(200, "OK"))
                    val response = TokenResponse(token)
                    call.respondText(Json.encodeToString(response))
                }
            }
        }

        post("/signin") {
            val contentType = call.request.contentType()
            val signInRequest = when {
                contentType.match(ContentType.Application.Json) -> {
                    println("Found JSON")

                    val requestBody = call.receive<String>()
                    println("Request Body: $requestBody")

                    try {
                        Json.decodeFromString<SignInRequest>(requestBody)
                    } catch (e: UnsupportedMediaTypeException) {
                        println("Unsupported media type for JSON: ${e.message}")
                        null
                    } catch (e: Throwable) {
                        println("Error parsing JSON request: ${e.message}")
                        null
                    }
                }
                contentType.match(ContentType.MultiPart.FormData) -> {
                    println("Found form data parameters")

                    val userInfo = call.receiveParameters()
                    SignInRequest(userInfo["email"]!!, userInfo["userType"]!!, userInfo["password"]!!)
                }
                else -> null
            }

            println("Sign In Request: $signInRequest")

            var findUser: ResultRow? = null
            transaction {
                if (signInRequest != null ) {
                    findUser = ApiUser.select { ApiUser.email eq signInRequest.email }
                        .andWhere { ApiUser.password eq signInRequest.password }
                        .firstOrNull()
                }
            }
            if (findUser != null) {
                try {
                    val userType = findUser!![ApiUser.userType]
                    val userName = findUser!![ApiUser.email]
                    val token = JWT.create()
                        .withAudience(audience)
                        .withIssuer(issuer)
                        .withClaim("username", userName)
                        .withClaim("usertype", userType)
                        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60000))
                        .sign(Algorithm.HMAC256(secret))

                    call.response.status(HttpStatusCode(200, "OK"))
                    val response = TokenResponse(token)
                    call.respondText(Json.encodeToString(response))
                } catch (e: Exception) {
                    println(e.message)
                    println(e.cause)
                }
            } else {
                val response = Response("Invalid email or password")
                call.response.status(HttpStatusCode(403, "Forbidden"))
                call.respondText(Json.encodeToString(response))
            }
        }

        authenticate("myAuth") {
            get("/validate") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val usertype = principal.payload.getClaim("usertype").asString()

                call.respondText("Hello, $usertype $username")
            }

            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val profile: UserProfileDTO?
                var findUser: ResultRow? = null

                transaction {
                    findUser = UserProfile.select {
                        UserProfile.email eq username
                    }.firstOrNull()
                }
                if (findUser != null) {
                    val name = findUser!![UserProfile.name]
                    val userType = findUser!![UserProfile.userType]
                    val phone = findUser!![UserProfile.phone]
                    val email = findUser!![UserProfile.email]
                    val address = findUser!![UserProfile.address]

                    profile = UserProfileDTO(name, userType, phone, email, address)

                    call.response.status(HttpStatusCode(200, "OK"))
                    call.respondText(Json.encodeToString(profile))
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            put("/me") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()

                val contentType = call.request.contentType()
                println("Content type of the request: $contentType")
                val changeRequest = when {
                    contentType.match(ContentType.Application.Json) -> {
                        println("Found JSON")

                        val requestBody = call.receive<String>()
                        println("Request Body: $requestBody")

                        try {
                            Json.decodeFromString<UserProfileDTO>(requestBody)
                        } catch (e: UnsupportedMediaTypeException) {
                            println("Unsupported media type for JSON: ${e.message}")
                            null
                        } catch (e: Throwable) {
                            println("Error parsing JSON request: ${e.message}")
                            null
                        }
                    }
                    contentType.match(ContentType.MultiPart.FormData) -> {
                        println("Found form data parameters")

                        val params = call.receiveParameters()
                        UserProfileDTO(
                            name = params["name"] ?: "",
                            userType = params["user_type"] ?: "",
                            phone = params["phone"] ?: "",
                            email = params["email"] ?: "",
                            address = params["address"] ?: "",
                        )
                    }
                    else -> null
                }

                println("Profile Change Request: $changeRequest")

                if (changeRequest == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid parameters")
                    return@put
                }

                val profile: UserProfileDTO?
                var findUser: ResultRow? = null

                transaction {
                    findUser = UserProfile.select {
                        UserProfile.email eq username
                    }.firstOrNull()
                }
                var createNewProfile = false;
                if (findUser != null) {
                    val name = findUser!![UserProfile.name]
                    val userType = findUser!![UserProfile.userType]
                    val phone = findUser!![UserProfile.phone]
                    val email = findUser!![UserProfile.email]
                    val address = findUser!![UserProfile.address]
                    profile = UserProfileDTO(name, userType, phone, email, address)

                } else {
                    val name = changeRequest.name
                    val userType = changeRequest.userType
                    val phone = changeRequest.phone
                    val email = changeRequest.email
                    val address = changeRequest.address
                    profile = UserProfileDTO(name, userType, phone, email, address)
                    createNewProfile = true
                }

                if (changeRequest.email != profile.email) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot change email")
                    return@put
                }

                transaction {
                    if (createNewProfile) {
                        UserProfile.insert {
                            it[name] = changeRequest.name
                            it[userType] = changeRequest.userType
                            it[phone] = changeRequest.phone
                            it[email] = changeRequest.email
                            it[address] = changeRequest.address
                        }
                    } else {
                        UserProfile.update({ UserProfile.email eq username }) {
                            it[name] = changeRequest.name
                            it[userType] = changeRequest.userType
                            it[phone] = changeRequest.phone
                            it[email] = changeRequest.email
                            it[address] = changeRequest.address
                        }
                    }
                }
                call.response.status(HttpStatusCode(200, "OK"))
                call.respondText(Json.encodeToString(changeRequest))
            }

            delete("/me") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                var findUser: ResultRow? = null

                transaction {
                    findUser = UserProfile.select {
                        UserProfile.email eq username
                    }.firstOrNull()
                }
                if (findUser != null) {
                    transaction {
                        UserProfile.deleteWhere { UserProfile.email eq username }
                        ApiUser.deleteWhere { ApiUser.email eq username }
                    }
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/meals") {
                val params = call.request.queryParameters
                if (params.isEmpty()) {
                    val mealList = transaction {
                        MealTable.selectAll().map {
                            Meal(
                                it[MealTable.mealId],
                                it[MealTable.title],
                                it[MealTable.price],
                                it[MealTable.imageUrl],
                                Json.decodeFromString<List<Int>>(it[MealTable.categoryIds])
                            )
                        }
                    }
                    call.respond(json.encodeToString(mealList))
                } else {
                    val id = params["id"]!!
                    println("mealId: $id")
                    val requestedMeal = transaction {
                        MealTable.select {MealTable.mealId eq id.toInt()}.firstOrNull()
                    }

                    if (requestedMeal == null) {
                        call.respond(HttpStatusCode.NotFound)
                    }

                    val mealById = requestedMeal?.let {
                        Meal(
                            it[MealTable.mealId],
                            it[MealTable.title],
                            it[MealTable.price],
                            it[MealTable.imageUrl],
                            Json.decodeFromString<List<Int>>(it[MealTable.categoryIds])
                        )
                    }
                    println("Meal: $mealById")
                    println(Json.encodeToString(mealById))
                    call.respond(Json.encodeToString(mealById))
                }
            }

            post("/meals") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val role = transaction {
                        ApiUser.select {
                        ApiUser.email eq username
                    }.firstOrNull()?.get(ApiUser.userType) ?: ""
                }

                if (role != "staff") {
                    val response = Response("Access denied")
                    call.respond(HttpStatusCode.Forbidden, Json.encodeToString(response))
                    return@post
                }

                val contentType = call.request.contentType()
                println("Content type of the request: $contentType")
                val mealSubmission = when {
                    contentType.match(ContentType.Application.Json) -> {
                        println("Found JSON")

                        val requestBody = call.receive<String>()
                        println("Request Body: $requestBody")

                        try {
                            Json.decodeFromString<Meal>(requestBody)
                        } catch (e: UnsupportedMediaTypeException) {
                            println("Unsupported media type for JSON: ${e.message}")
                            null
                        } catch (e: Throwable) {
                            println("Error parsing JSON request: ${e.message}")
                            null
                        }
                    }
                    contentType.match(ContentType.MultiPart.FormData) -> {
                        println("Found form data parameters")
                        val params = call.receiveParameters()
                        Meal(
                            mealId = params["mealId"]!!.toInt(),
                            title = params["title"]!!,
                            price = params["price"]!!.toFloat(),
                            imageUrl = params["imageUrl"]!!,
                            categoryIds = params["categoryIds"]!!.toList().map { it.digitToInt() }
                        )
                    }
                    else -> null
                }

                if (mealSubmission == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val checkIdExists = transaction {
                    MealTable.select { MealTable.mealId eq mealSubmission.mealId }
                        .firstOrNull()
                }

                if (checkIdExists != null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                transaction {
                    MealTable.insert {
                        it[mealId] = mealSubmission.mealId
                        it[title] = mealSubmission.title
                        it[price] = mealSubmission.price
                        it[imageUrl] = mealSubmission.imageUrl
                        it[categoryIds] = Json.encodeToString(mealSubmission.categoryIds)
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            post("/categories") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val role = transaction {
                    ApiUser.select {
                        ApiUser.email eq username
                    }.firstOrNull()?.get(ApiUser.userType) ?: ""
                }

                if (role != "staff") {
                    val response = Response("Access denied")
                    call.respond(HttpStatusCode.Forbidden, Json.encodeToString(response))
                    return@post
                }

                val contentType = call.request.contentType()
                println("Content type of the request: $contentType")
                val categorySubmission = when {
                    contentType.match(ContentType.Application.Json) -> {
                        println("Found JSON")

                        val requestBody = call.receive<String>()
                        println("Request Body: $requestBody")

                        try {
                            Json.decodeFromString<Category>(requestBody)
                        } catch (e: UnsupportedMediaTypeException) {
                            println("Unsupported media type for JSON: ${e.message}")
                            null
                        } catch (e: Throwable) {
                            println("Error parsing JSON request: ${e.message}")
                            null
                        }
                    }
                    contentType.match(ContentType.MultiPart.FormData) -> {
                        println("Found form data parameters")
                        val params = call.receiveParameters()
                        Category(
                            categoryId = params["categoryId"]!!.toInt(),
                            title = params["title"]!!,
                            description = params["description"]!!,
                        )
                    }
                    else -> null
                }

                if (categorySubmission == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val checkIdExists = transaction {
                    CategoryTable.select { CategoryTable.categoryId eq categorySubmission.categoryId }
                        .firstOrNull()
                }

                if (checkIdExists != null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                transaction {
                    CategoryTable.insert {
                        it[categoryId] = categorySubmission.categoryId
                        it[title] = categorySubmission.title
                        it[description] = categorySubmission.description
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            get("/categories") {
                val params = call.request.queryParameters
                if (params.isEmpty()) {
                    val categoryList = transaction {
                        CategoryTable.selectAll().map {
                            Category(
                                it[CategoryTable.categoryId],
                                it[CategoryTable.title],
                                it[CategoryTable.description],
                            )
                        }
                    }
                    call.respond(json.encodeToString(categoryList))
                } else {
                    val id = params["id"]!!
                    val requestedCategory = transaction {
                        CategoryTable
                            .select {CategoryTable.categoryId eq id.toInt()}
                            .firstOrNull()
                    }

                    if (requestedCategory == null) {
                        call.respond(HttpStatusCode.NotFound)
                    }

                    val categoryById = requestedCategory?.let {
                        Category(
                            it[CategoryTable.categoryId],
                            it[CategoryTable.title],
                            it[CategoryTable.description],
                        )
                    }
                    call.respond(Json.encodeToString(categoryById))
                }
            }

            post("/order") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()

                val contentType = call.request.contentType()
                println("Content type of the request: $contentType")
                val orderRequest = when {
                    contentType.match(ContentType.Application.Json) -> {
                        println("Found JSON")

                        val requestBody = call.receive<String>()
                        println("Request Body: $requestBody")

                        val regex = Regex("-?\\d+")
                        regex.findAll(requestBody).map { it.value.toInt() }.toList()
                    }
                    contentType.match(ContentType.MultiPart.FormData) -> {
                        println("Found form data parameters")

                        val params = call.receiveParameters()
                        val ids = params["mealsIds"]!!
                        val regex = Regex("-?\\d+")
                        regex.findAll(ids).map { it.value.toInt() }.toList()
                    }
                    else -> null
                }

                println("Meal ids for order: $orderRequest")

                if (orderRequest == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val maxOrderId = transaction {
                    OrderTable
                        .slice(OrderTable.orderId.max())
                        .selectAll()
                        .map { it[OrderTable.orderId.max()] }
                        .singleOrNull() ?: 0
                }
                var totalPrice = 0.0f
                var invalidMealId = false
                transaction {
                    println("Calculating total price")
                    for (mealId in orderRequest) {
                        val price = MealTable.select { MealTable.mealId eq mealId }
                            .map { it[MealTable.price] }
                            .singleOrNull()
                        if (price == null) {
                            invalidMealId = true
                            break
                        }
                        println("Price: $price")
                        totalPrice += price
                    }
                }
                if (invalidMealId) {
                    println("Invalid meal")
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val userAddress = transaction {
                    UserProfile.select { UserProfile.email eq username }
                        .map { it[UserProfile.address] }
                        .singleOrNull() ?: "123 Address Lane"
                }
                val order = Order(maxOrderId + 1,
                    username,
                    orderRequest,
                    totalPrice,
                    userAddress,
                    "COOK"
                )
                transaction {
                    OrderTable.insert {
                        it[orderId] = order.orderId
                        it[userEmail] = order.userEmail
                        it[mealsIds] = Json.encodeToString(order.mealsIds)
                        it[price] = totalPrice
                        it[address] = order.address
                        it[status] = order.status
                    }
                }
                println(Json.encodeToString(order))
                call.respond(HttpStatusCode.OK, Json.encodeToString(order))
            }

            get("/orderHistory") {
                val orderList = transaction {
                    OrderTable.selectAll().map {
                        Order(
                            it[OrderTable.orderId],
                            it[OrderTable.userEmail],
                            Json.decodeFromString<List<Int>>(it[OrderTable.mealsIds]),
                            it[OrderTable.price],
                            it[OrderTable.address],
                            it[OrderTable.status]
                            )
                    }
                }
                call.respond(json.encodeToString(orderList))
            }

            get("/orderIncomplete") {
                val orderList = transaction {
                    OrderTable.select { OrderTable.status eq "IN PROGRESS" }.map {
                        Order(
                            it[OrderTable.orderId],
                            it[OrderTable.userEmail],
                            Json.decodeFromString<List<Int>>(it[OrderTable.mealsIds]),
                            it[OrderTable.price],
                            it[OrderTable.address],
                            it[OrderTable.status]
                        )
                    }
                }
                println(json.encodeToString(orderList))
                call.respond(json.encodeToString(orderList))
            }

            post("/order/{orderId}/markReady") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val role = transaction {
                    ApiUser.select {
                        ApiUser.email eq username
                    }.firstOrNull()?.get(ApiUser.userType) ?: ""
                }

                if (role != "staff") {
                    val response = Response("Access denied")
                    call.respond(HttpStatusCode.Forbidden, Json.encodeToString(response))
                    return@post
                }
                val orderId = call.parameters["orderId"]!!
                var findOrder: ResultRow? = null
                transaction {
                    findOrder = OrderTable.select { OrderTable.orderId eq orderId.toInt() }.firstOrNull()
                    if (findOrder != null) {
                        OrderTable.update({ OrderTable.orderId eq orderId.toInt() }) {
                            it[status] = "COMPLETE"
                        }
                    }
                }
                if (findOrder == null) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
    TransactionManager.currentOrNull()?.connection?.close()

}

@Serializable
data class Response(val status: String)

@Serializable
data class TokenResponse(val token: String)

@Serializable
data class SignUpRequest(val email: String, val userType: String, val password: String)

@Serializable
data class SignInRequest(val email: String, val userType: String, val password: String)

@Serializable
data class UserProfileDTO (val name: String,
                           val userType: String,
                           val phone: String,
                           val email: String,
                           val address: String)

@Serializable
data class Meal(val mealId: Int,
                val title: String,
                val price: Float,
                val imageUrl: String,
                val categoryIds: List<Int>)

@Serializable
data class Category(val categoryId: Int,
                    val title: String,
                    val description: String)

@Serializable
data class Order(val orderId: Int,
                 val userEmail: String,
                 val mealsIds: List<Int>,
                 val price: Float,
                 val address: String,
                 val status: String)

object ApiUser : IntIdTable() {
    val email: Column<String> = text("email")
    val userType: Column<String> = text("user_type")
    val password: Column<String> = text("password")
}

object UserProfile : IntIdTable() {
    val name: Column<String> = text("user_name")
    val userType: Column<String> = text("user_type")
    val phone: Column<String> = text("phone")
    val email: Column<String> = text("email")
    val address: Column<String> = text("address")
}

object MealTable : Table() {
    val mealId: Column<Int> = integer("mealId")
    val title: Column<String> = text("title")
    val price: Column<Float> = float("price")
    val imageUrl: Column<String> = text("imageUrl")
    val categoryIds: Column<String> = text("categoryIds")
}

object CategoryTable : Table() {
    val categoryId: Column<Int> = integer("categoryId")
    val title: Column<String> = text("title")
    val description: Column<String> = text("description")
}

object OrderTable : IntIdTable() {
    val orderId: Column<Int> = integer("orderId")
    val userEmail: Column<String> = text("userEmail")
    val mealsIds: Column<String> = text("mealsIds")
    val price: Column<Float> = float("price")
    val address: Column<String> = text("address")
    val status: Column<String> = text("status")
}
