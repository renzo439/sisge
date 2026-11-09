package com.sisge.ms.repository; import com.sisge.ms.entity.DesignacionAdicional; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface DesignacionAdicionalRepository extends JpaRepository<DesignacionAdicional,Long>{ Optional<DesignacionAdicional> findByDesignacionIdAndAdicionalIdAndVigenteTrue(Long designacionId,Long adicionalId); }
