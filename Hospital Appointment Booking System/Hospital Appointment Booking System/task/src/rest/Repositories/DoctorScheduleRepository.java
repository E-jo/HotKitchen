package rest.Repositories;

import rest.Models.DoctorSchedule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends CrudRepository<DoctorSchedule, Long> {
    void deleteById(Long id);
    List<DoctorSchedule> findAll();
    Optional<DoctorSchedule> findById(Long id);
    DoctorSchedule save(DoctorSchedule doctorSchedule);
}
