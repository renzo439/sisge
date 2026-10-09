package com.sisge.ms.repository;
import com.sisge.ms.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<Usuario, Long> { Optional<Usuario> findByEmail(String email); }
