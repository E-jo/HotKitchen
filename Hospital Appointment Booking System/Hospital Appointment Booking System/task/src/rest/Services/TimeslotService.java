package rest.Services;

import rest.Models.Timeslot;
import rest.Repositories.TimeslotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TimeslotService {
    private final TimeslotRepository timeslotRepository;

    @Autowired
    public TimeslotService(TimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }

    public void deleteById(Long id) {
        timeslotRepository.deleteById(id);
    }

    public List<Timeslot> findAll() {
        return timeslotRepository.findAll();
    }

    public Optional<Timeslot> findById(Long id) {
        return timeslotRepository.findById(id);
    }

    public Timeslot save(Timeslot timeslot) {
        return timeslotRepository.save(timeslot);
    }
}
