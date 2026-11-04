package com.sisge.ms.repository;

import com.sisge.ms.entity.Persona;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
    Optional<Persona> findByCuil(String cuil);
    Optional<Persona> findByDni(String dni);
}
