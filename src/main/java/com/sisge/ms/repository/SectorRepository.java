package com.sisge.ms.repository;

import com.sisge.ms.entity.Sector;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findByIdAndOrganizacionId(Long id, Long organizacionId);
}
