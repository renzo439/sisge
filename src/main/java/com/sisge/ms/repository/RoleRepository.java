package com.sisge.ms.repository;
import com.sisge.ms.entity.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoleRepository extends JpaRepository<Rol, Long> { Optional<Rol> findByNombre(String nombre); }
