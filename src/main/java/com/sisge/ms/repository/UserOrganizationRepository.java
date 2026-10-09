package com.sisge.ms.repository;
import com.sisge.ms.entity.UserOrganization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> { List<UserOrganization> findByUsuarioIdAndEstado(Long userId, String estado); boolean existsByUsuarioIdAndOrganizacionIdAndEstado(Long userId, Long organizationId, String estado); }
