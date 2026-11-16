package com.sisge.ms.repository;
import com.sisge.ms.entity.Permission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PermissionRepository extends JpaRepository<Permission, Long> { List<Permission> findByCodigoIn(Collection<String> codigos); List<Permission> findAllByOrderByCodigoAsc(); boolean existsByCodigo(String codigo); }
