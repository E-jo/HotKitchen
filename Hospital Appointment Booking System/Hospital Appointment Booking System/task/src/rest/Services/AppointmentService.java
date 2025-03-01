package rest.Services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import rest.Models.*;
import rest.Repositories.AppointmentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final TimeslotService timeslotService;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorService doctorService,
                              TimeslotService timeslotService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.timeslotService = timeslotService;
    }

    public void deleteById(Long id) {
        appointmentRepository.deleteById(id);
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment save(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    public ResponseEntity<?> setAppointment(Appointment appointmentRequest) {
        String doctorName = appointmentRequest.getDoctor().toLowerCase();
        
        if (doctorName.equalsIgnoreCase("director")) {
            System.out.println("director can't be set appointments");
            return new ResponseEntity<>("director can't be set appointments", HttpStatus.BAD_REQUEST);
        }

        String patient = appointmentRequest.getPatient().toLowerCase();
        String date = appointmentRequest.getDate();
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println("Error, invalid date format");

            return new ResponseEntity<>("Error, invalid date format", HttpStatus.BAD_REQUEST);
        }

        Optional<Doctor> assigningDoctorOptional = doctorService
                .findByDoctorName(appointmentRequest.getDoctor().toLowerCase());
        if (assigningDoctorOptional.isEmpty()) {
            System.out.println("Doctor " + appointmentRequest.getDoctor() + "not found");

            return new ResponseEntity<>("Doctor not found", HttpStatus.BAD_REQUEST);
        }

        Doctor assigningDoctor = assigningDoctorOptional.get();
        List<Timeslot> doctorSchedule = assigningDoctor.getDoctorSchedule().getSchedule();
        for (Timeslot ts : doctorSchedule) {
            if (ts.getAvailabletime().equalsIgnoreCase(date)) {
                if (ts.isBooked()) {
                    System.out.println("timeslot already booked");
                    return new ResponseEntity<>(
                            "Sorry, that time is already booked.", HttpStatus.BAD_REQUEST);
                } else {
                    System.out.println("Booked!");
                    ts.setBooked(true);
                    timeslotService.save(ts);
                    break;
                }
            }
        }
        Appointment app = new Appointment();
        app.setDoctor(doctorName);
        app.setPatient(patient);
        app.setDate(date);
        Appointment savedAppointment = save(app);

        List<Appointment> newAppointmentList = assigningDoctor.getDoctorSchedule().getAppointments();
        newAppointmentList.add((savedAppointment));
        assigningDoctor.getDoctorSchedule().setAppointments(newAppointmentList);
        doctorService.save(assigningDoctor);
        System.out.println(savedAppointment);

        AppointmentDTO response = new AppointmentDTO(
                savedAppointment.getIdApp(),
                savedAppointment.getDoctor(),
                savedAppointment.getPatient(),
                savedAppointment.getDate());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> deleteAppointment(long id) {
        Optional<Appointment> targetAppointment = findById(id);
        if (targetAppointment.isPresent()) {
            deleteById(id);
            return new ResponseEntity<>(targetAppointment, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("{\n" +
                    "   \"error\": \"The appointment does not exist or was already cancelled\"\n" +
                    "}", HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> getAppointmentById(long id) {
        Optional<Appointment> targetAppointment = findById(id);
        if (targetAppointment.isPresent()) {
            return new ResponseEntity<>(targetAppointment, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("{\n" +
                    "   \"error\": \"The appointment does not exist or was already cancelled\"\n" +
                    "}", HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> getAllAppointments() {
        List<Appointment> allAppointments = findAll();

        return !allAppointments.isEmpty() ? new ResponseEntity<>(allAppointments, HttpStatus.OK)
                : new ResponseEntity<>(allAppointments, HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<?> getDailyStats() {
        Map<String, Integer> dailyAppointments = new HashMap<>();
        for (Appointment appointment : findAll()) {
            if (dailyAppointments.containsKey(appointment.getDate())) {
                dailyAppointments.compute(appointment.getDate(), (k, currentTotal) -> currentTotal + 1);
            } else {
                dailyAppointments.put(appointment.getDate(), 1);
            }
        }

        if (dailyAppointments.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        System.out.println("Daily appointments:\n" + dailyAppointments);
        JsonArray responseArray = new JsonArray();
        for (String date : dailyAppointments.keySet()) {
            JsonObject response = new JsonObject();
            response.addProperty(date, dailyAppointments.get(date));
            responseArray.add(response);
        }
        System.out.println("Daily appointments:\n" + responseArray);

        return new ResponseEntity<>(responseArray.toString(), HttpStatus.OK);
    }
}
