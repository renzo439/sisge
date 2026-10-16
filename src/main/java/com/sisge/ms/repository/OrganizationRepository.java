package com.sisge.ms.repository;
import com.sisge.ms.entity.Organizacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrganizationRepository extends JpaRepository<Organizacion, Long> { Optional<Organizacion> findByUdeo(String udeo); }
