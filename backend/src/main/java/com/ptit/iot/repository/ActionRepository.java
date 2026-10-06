package com.ptit.iot.repository;

import com.ptit.iot.entity.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActionRepository extends JpaRepository<Action, Integer>, JpaSpecificationExecutor<Action> {

    @EntityGraph(attributePaths = {"device", "user"})
    @Query("select a from Action a where a.device.id = :deviceId and a.status = :status order by a.time desc, a.id desc")
    List<Action> findByDeviceAndStatus(@Param("deviceId") Integer deviceId,
                                       @Param("status") String status,
                                       Pageable pageable);

    @EntityGraph(attributePaths = {"device", "user"})
    @Query("select a from Action a where a.status = :status and a.time < :before")
    List<Action> findStale(@Param("status") String status, @Param("before") LocalDateTime before);

    @EntityGraph(attributePaths = {"device", "user"})
    @Query("select a from Action a where a.device.id = :deviceId and a.status = 'SUCCESS' order by a.time desc, a.id desc")
    List<Action> findLastSuccess(@Param("deviceId") Integer deviceId, Pageable pageable);

    @EntityGraph(attributePaths = {"device", "user"})
    @Query("select a from Action a where a.device.id = :deviceId order by a.time desc, a.id desc")
    List<Action> findLastAny(@Param("deviceId") Integer deviceId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"device", "user"})
    Page<Action> findAll(Specification<Action> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"device", "user"})
    Optional<Action> findById(Integer id);
}
