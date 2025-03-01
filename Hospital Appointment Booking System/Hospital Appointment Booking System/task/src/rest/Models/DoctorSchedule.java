package rest.Models;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class DoctorSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonAlias("idApp")
    private long schedule_id;

    private long doctor_id;

    @OneToMany
    @JoinColumn(name = "schedule_id")
    private List<Timeslot> schedule = new ArrayList<>();

    @OneToMany
    @JoinColumn(name = "appointment_id")
    private List<Appointment> appointments = new ArrayList<>();

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    public void setSchedule(List<Timeslot> schedule) {
        this.schedule = schedule;
    }


    public DoctorSchedule() {
    }

    public DoctorSchedule(List<Timeslot> schedule) {
        this.schedule = schedule;
        this.appointments = new ArrayList<>();
    }

    public List<Timeslot> getSchedule() {
        return schedule;
    }

}
