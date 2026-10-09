package com.sisge.ms.repository;
import com.sisge.ms.entity.ReemplazanteHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface ReemplazanteHistoricoRepository extends JpaRepository<ReemplazanteHistorico, Long>, JpaSpecificationExecutor<ReemplazanteHistorico> { boolean existsByImportacionId(Long importacionId); }
