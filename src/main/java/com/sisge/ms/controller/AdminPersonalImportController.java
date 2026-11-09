package com.sisge.ms.controller;
import com.sisge.ms.dto.personal.PersonalAdminDtos; import com.sisge.ms.service.PersonalConsolidationService; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/personal/importaciones") public class AdminPersonalImportController {
 private final PersonalConsolidationService service; public AdminPersonalImportController(PersonalConsolidationService service){this.service=service;}
 @PostMapping("/{id}/validar") public PersonalAdminDtos.Validation validate(@PathVariable Long id){return service.validate(id);}
 @PostMapping("/{id}/consolidar") public PersonalAdminDtos.Consolidation consolidate(@PathVariable Long id){return service.consolidate(id);}
}
