package com.sisge.ms.repository;

import com.sisge.ms.entity.Designacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DesignacionRepository extends JpaRepository<Designacion, Long>, JpaSpecificationExecutor<Designacion> {
    java.util.Optional<Designacion> findFirstByPersonaIdAndOrganizacionIdAndSectorIdAndFechaInicio(Long personaId, Long organizacionId, Long sectorId, java.time.LocalDate fechaInicio);
}
