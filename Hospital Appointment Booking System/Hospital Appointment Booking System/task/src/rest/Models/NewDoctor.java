package rest.Models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record NewDoctor (@NotNull @NotEmpty String doctorName) {
}
