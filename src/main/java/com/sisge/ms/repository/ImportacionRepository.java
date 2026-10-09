package com.sisge.ms.repository;
import com.sisge.ms.entity.Importacion;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
public interface ImportacionRepository extends JpaRepository<Importacion, Long>, JpaSpecificationExecutor<Importacion> { Page<Importacion> findByOrganizacionId(Long organizationId, Pageable pageable); @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Importacion i where i.id = :id") Optional<Importacion> lockById(Long id); }
