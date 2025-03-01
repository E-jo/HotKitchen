package rest.Controllers;

import jakarta.validation.Valid;
import rest.Models.DoctorSchedule;
import rest.Models.NewDoctor;
import rest.Models.Timeslot;
import rest.Services.DoctorScheduleService;
import rest.Services.TimeslotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rest.Models.Doctor;
import rest.Services.DoctorService;

import java.util.ArrayList;
import java.util.List;

@RestController
public class DoctorController {
    @Autowired
    DoctorService doctorService;

    @PostMapping("/newDoctor")
    ResponseEntity<?> newDoctor(@Valid @RequestBody NewDoctor doc) {
        return doctorService.newDoctor(doc);
    }

    @GetMapping("/allDoctorslist")
    ResponseEntity<?> listDoctors() {
        return doctorService.listDoctors();
    }

    @GetMapping("/availableDatesByDoctor")
    ResponseEntity<?> getAvailableDatesByDoctor(@RequestParam String doc) {
        return doctorService.getAvailableDatesByDoctor(doc.toLowerCase());
    }

    @DeleteMapping("/deleteDoctor")
    ResponseEntity<?> deleteDoctor(@RequestParam String doc) {
        return doctorService.deleteDoctor(doc.toLowerCase());
    }

}
