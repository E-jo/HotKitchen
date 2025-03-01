package rest.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rest.Services.AppointmentService;
import rest.Services.DoctorService;

@RestController
public class StatsController {
    @Autowired
    AppointmentService appointmentService;
    @Autowired
    DoctorService doctorService;

    @GetMapping(value = "/statisticsDay", produces = "application/json")
    ResponseEntity<?> getDailyStats() {
        return appointmentService.getDailyStats();
    }

    @GetMapping(value = "/statisticsDoc", produces = "application/json")
    ResponseEntity<?> getDoctorStats() {
        return doctorService.getDoctorStats();
    }
}
