package com.sisge.ms.repository;
import com.sisge.ms.entity.ImportacionReemplazante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ImportacionReemplazanteRepository extends JpaRepository<ImportacionReemplazante, Long> { List<ImportacionReemplazante> findByImportacionId(Long importacionId); Optional<ImportacionReemplazante> findByIdAndImportacionId(Long id, Long importacionId); }
