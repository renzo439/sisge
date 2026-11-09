package com.sisge.ms.repository;
import com.sisge.ms.entity.Maestro; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.repository.NoRepositoryBean;
@NoRepositoryBean public interface MaestroRepository<T extends Maestro> extends JpaRepository<T,Long> { Optional<T> findByNombreIgnoreCaseAndActivoTrue(String nombre); }
