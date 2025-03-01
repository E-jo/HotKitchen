package rest.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
public class Timeslot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long time_slot_id;

    private String availabletime;
    private boolean booked;
    public Timeslot(String availabletime, boolean booked) {
        this.availabletime = availabletime;
        this.booked = booked;
    }

    public Timeslot() {}

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    public String getAvailabletime() {
        return availabletime;
    }

    public void setAvailabletime(String availabletime) {
        this.availabletime = availabletime;
    }
}
