package com.raccoon.datacleaning.repository;

import com.raccoon.datacleaning.model.DiscoveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveryTaskRepository extends JpaRepository<DiscoveryTask, Long> {
    
    List<DiscoveryTask> findByStatusOrderByCreatedAtDesc(String status);
    
    List<DiscoveryTask> findTop10ByOrderByCreatedAtDesc();
    
    Optional<DiscoveryTask> findFirstByStatusInOrderByCreatedAtDesc(List<String> statuses);
    
    long countByStatus(String status);

    /**
     * 只推进度字段。整体 save 会带着内存里的旧 status 把刚写入的 cancelled 覆盖回 running，
     * 所以这里用条件更新，取消之后进度写不进去
     */
    @Modifying
    @Transactional
    @Query("UPDATE DiscoveryTask t SET t.currentTable = :currentTable, t.currentColumn = :currentColumn, "
            + "t.processedFields = :processedFields "
            + "WHERE t.id = :id AND t.status <> 'cancelled'")
    int updateProgress(@Param("id") Long id,
                       @Param("currentTable") String currentTable,
                       @Param("currentColumn") String currentColumn,
                       @Param("processedFields") int processedFields);
}
