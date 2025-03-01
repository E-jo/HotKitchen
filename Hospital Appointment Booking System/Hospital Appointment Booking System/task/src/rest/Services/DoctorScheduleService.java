package rest.Services;

import rest.Models.DoctorSchedule;
import rest.Repositories.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorScheduleService {
    private final DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    public DoctorScheduleService(DoctorScheduleRepository doctorScheduleRepository) {
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    public void deleteById(Long id) {
        doctorScheduleRepository.deleteById(id);
    }

    public List<DoctorSchedule> findAll() {
        return doctorScheduleRepository.findAll();
    }

    public Optional<DoctorSchedule> findById(Long id) {
        return doctorScheduleRepository.findById(id);
    }

    public DoctorSchedule save(DoctorSchedule doctorSchedule) {
        return doctorScheduleRepository.save(doctorSchedule);
    }
}
