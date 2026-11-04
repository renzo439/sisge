package com.sisge.ms.repository;

import com.sisge.ms.entity.ImportacionPersonalFila;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacionPersonalFilaRepository extends JpaRepository<ImportacionPersonalFila, Long> {
    List<ImportacionPersonalFila> findByImportacionId(Long importacionId);
    Optional<ImportacionPersonalFila> findByIdAndImportacionId(Long id, Long importacionId);
}
