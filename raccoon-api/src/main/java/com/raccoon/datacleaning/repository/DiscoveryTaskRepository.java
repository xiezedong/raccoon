package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.DiscoveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveryTaskRepository extends JpaRepository<DiscoveryTask, Long> {
    
    List<DiscoveryTask> findByStatusOrderByCreatedAtDesc(String status);
    
    List<DiscoveryTask> findTop10ByOrderByCreatedAtDesc();
    
    Optional<DiscoveryTask> findFirstByStatusInOrderByCreatedAtDesc(List<String> statuses);
    
    long countByStatus(String status);
}
