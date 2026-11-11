package com.sisge.ms.repository;
import com.sisge.ms.entity.UserOrganization;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> { List<UserOrganization> findByUsuarioIdAndEstado(Long userId, String estado); List<UserOrganization> findByOrganizacionId(Long organizationId); Optional<UserOrganization> findByUsuarioIdAndOrganizacionId(Long userId, Long organizationId); boolean existsByUsuarioIdAndOrganizacionIdAndEstado(Long userId, Long organizationId, String estado); boolean existsByRolId(Long rolId); boolean existsByOrganizacionId(Long organizationId); }
