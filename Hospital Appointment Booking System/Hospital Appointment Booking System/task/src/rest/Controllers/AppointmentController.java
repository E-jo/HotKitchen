package rest.Controllers;

import jakarta.validation.Valid;
import rest.Models.Doctor;
import rest.Models.Timeslot;
import rest.Services.DoctorService;
import rest.Services.TimeslotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rest.Models.Appointment;
import rest.Services.AppointmentService;

import java.util.List;
import java.util.Optional;

@RestController
public class AppointmentController {
    @Autowired
    AppointmentService appointmentService;

    @PostMapping("/setAppointment")
    ResponseEntity<?> setAppointment(@Valid @RequestBody Appointment appointmentRequest) {
       return appointmentService.setAppointment(appointmentRequest);
    }

    @DeleteMapping("/deleteAppointment")
    ResponseEntity<?> deleteAppointment(@RequestParam long id) {
        return appointmentService.deleteAppointment(id);
    }

    @GetMapping("/appointment")
    ResponseEntity<?> getAppointmentById(@RequestParam long id) {
        return appointmentService.getAppointmentById(id);
    }

    @GetMapping(value = "/appointments", produces = "application/json")
    ResponseEntity<?> getAllAppointments() {
        List<Appointment> allAppointments = appointmentService.findAll();

        return !allAppointments.isEmpty() ? new ResponseEntity<>(allAppointments, HttpStatus.OK)
                : new ResponseEntity<>(allAppointments, HttpStatus.NO_CONTENT);
    }

}
