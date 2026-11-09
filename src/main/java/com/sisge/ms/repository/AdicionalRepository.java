package com.sisge.ms.repository; import com.sisge.ms.entity.Adicional; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface AdicionalRepository extends JpaRepository<Adicional,Long>{ Optional<Adicional> findByCodigoAndActivoTrue(String codigo); }
