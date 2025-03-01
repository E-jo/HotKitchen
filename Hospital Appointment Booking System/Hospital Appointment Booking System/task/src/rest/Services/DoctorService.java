package rest.Services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import rest.Models.*;
import rest.Repositories.AppointmentRepository;
import rest.Repositories.DoctorRepository;

import java.util.*;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    private final DoctorScheduleService doctorScheduleService;

    private final TimeslotService timeslotService;

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         DoctorScheduleService doctorScheduleService,
                         TimeslotService timeslotService,
                         AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.doctorScheduleService = doctorScheduleService;
        this.timeslotService = timeslotService;
        this.appointmentRepository = appointmentRepository;
    }

    public void deleteById(Long id) {
        doctorRepository.deleteById(id);
    }

    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    public Optional<Doctor> findByDoctorName(String doctorName) {
        return doctorRepository.findByDoctorName(doctorName);
    }

    public Optional<Doctor> findById(Long id) {
        return doctorRepository.findById(id);
    }

    public Doctor save(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    public ResponseEntity<?> getAvailableDatesByDoctor(String doc) {
        doc = doc.toLowerCase();
        List<Doctor> allDoctors = findAll();

        if (allDoctors.isEmpty()) {
            return new ResponseEntity<>("No doctors in database.", HttpStatus.NO_CONTENT);
        }
        for (Doctor doctor : allDoctors) {
            System.out.println(doctor.getDoctorName());
            if (doctor.getDoctorName().equalsIgnoreCase(doc)) {
                return new ResponseEntity<>(doctor.getDoctorSchedule().getSchedule(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("Error, doctor not found in database.", HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<?> newDoctor(@Valid @RequestBody NewDoctor doc) {
        Doctor doctor = createDoctor(doc);
        if (doctor == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(doctor, HttpStatus.OK);
    }

    public Doctor createDoctor(NewDoctor doc) {
        if (doc.doctorName().isBlank()) {
            return null;
        }

        List<Doctor> allDoctors = findAll();

        for (Doctor doctor : allDoctors) {
            System.out.println(doctor.getDoctorName() + ":" + doc.doctorName());
            if (doctor.getDoctorName().equalsIgnoreCase(doc.doctorName())) {
                System.out.println("Name match");
                return null;
            }
        }

        Doctor newDoc = new Doctor();
        newDoc.setDoctorName(doc.doctorName().toLowerCase());
        newDoc.setDoctorSchedule(generateSchedule());

        return save(newDoc);
    }

    public DoctorSchedule generateSchedule() {
        List<Timeslot> scheduleList = new ArrayList<>();
        Timeslot one = new Timeslot("2023-10-09", false);
        one = timeslotService.save(one);
        scheduleList.add(one);
        Timeslot two = new Timeslot("2023-10-10", false);
        two = timeslotService.save(two);
        scheduleList.add(two);
        Timeslot three = new Timeslot("2023-10-11", false);
        three = timeslotService.save(three);
        scheduleList.add(three);
        Timeslot four = new Timeslot("2023-10-12", false);
        four = timeslotService.save(four);
        scheduleList.add(four);

        DoctorSchedule schedule = new DoctorSchedule(scheduleList);
        return doctorScheduleService.save(schedule);
    }

    public ResponseEntity<?> listDoctors() {
        List<Doctor> allDoctors = findAll();

        return !allDoctors.isEmpty() ? new ResponseEntity<>(allDoctors, HttpStatus.OK)
                : new ResponseEntity<>(allDoctors, HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<?> deleteDoctor(String deleteDoctorRequestName) {
        Optional<Doctor> optionalDoctor = findByDoctorName(deleteDoctorRequestName );
        if (optionalDoctor.isEmpty()) {
            System.out.println("Couldn't find doctor " + deleteDoctorRequestName);
            return new ResponseEntity<>("Doctor not found", HttpStatus.BAD_REQUEST);
        }

        Doctor doc = optionalDoctor.get();
        NewDoctor directorRequest = new NewDoctor("director");
        Doctor director = new Doctor();
        director.setDoctorSchedule(new DoctorSchedule(new ArrayList<>()));
        Optional<Doctor> optionalDirector = findByDoctorName("director");
        director = optionalDirector.orElseGet(() -> createDoctor(directorRequest));

        DoctorSchedule droppedDoctorSchedule = doc.getDoctorSchedule();
        DoctorSchedule directorSchedule = director.getDoctorSchedule();

        List<Appointment> droppedAppointments = droppedDoctorSchedule.getAppointments();
        System.out.println("droppedAppointments for " + deleteDoctorRequestName);

        List<Appointment> currentDirectorAppointments = directorSchedule.getAppointments();

        currentDirectorAppointments.addAll(droppedAppointments);

        directorSchedule.setAppointments(currentDirectorAppointments);
        director.setDoctorSchedule(directorSchedule);
        save(director);

        for (Appointment appointment : droppedAppointments) {
            System.out.println(appointment);
            if (deleteDoctorRequestName.equalsIgnoreCase("director")) {
                appointmentRepository.deleteById(appointment.getIdApp());
            } else {
                appointment.setDoctor("director");
                appointmentRepository.save(appointment);
            }
        }

        deleteById(doc.getId());

        return new ResponseEntity<>(doc, HttpStatus.OK);

    }

    public ResponseEntity<?> getDoctorStats() {
        Map<String, Integer> doctorAppointments = new HashMap<>();
        List<Doctor> allDoctors = findAll();
        for (Doctor doctor : allDoctors) {
            doctorAppointments.put(doctor.getDoctorName(), doctor.getDoctorSchedule().getAppointments().size());
        }
        if (doctorAppointments.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        JsonArray responseArray = new JsonArray();
        for (String date : doctorAppointments.keySet()) {
            int appointments = doctorAppointments.get(date);
            if (appointments > 0) {
                JsonObject response = new JsonObject();
                response.addProperty(date, doctorAppointments.get(date));
                responseArray.add(response);
            }
        }
        System.out.print("Appointments per doctor: " + responseArray);

        return new ResponseEntity<>(responseArray.toString(), HttpStatus.OK);
    }
}


