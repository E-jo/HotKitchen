package rest.Repositories;

import rest.Models.Timeslot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeslotRepository extends CrudRepository<Timeslot, Long> {
    void deleteById(Long id);
    List<Timeslot> findAll();
    Optional<Timeslot> findById(Long id);
    Timeslot save(Timeslot timeslot);
}
