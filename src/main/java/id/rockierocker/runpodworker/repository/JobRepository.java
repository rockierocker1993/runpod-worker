package id.rockierocker.runpodworker.repository;

import id.rockierocker.runpodworker.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByWorkerJobId(String workerJobId);

}

