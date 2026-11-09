package com.sisge.ms.repository; import com.sisge.ms.entity.Dependencia; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface DependenciaRepository extends JpaRepository<Dependencia,Long>{ Optional<Dependencia> findByIdAndOrganizacionIdAndActivoTrue(Long id,Long organizacionId); }
