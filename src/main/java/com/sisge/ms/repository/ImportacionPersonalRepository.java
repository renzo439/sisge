package com.sisge.ms.repository;

import com.sisge.ms.entity.ImportacionPersonal;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface ImportacionPersonalRepository extends JpaRepository<ImportacionPersonal, Long> {
    Page<ImportacionPersonal> findByOrganizacionIdIn(Collection<Long> organizationIds, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImportacionPersonal i where i.id = :id")
    Optional<ImportacionPersonal> lockById(Long id);
}
