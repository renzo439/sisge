package com.sisge.ms.controller;

import com.sisge.ms.dto.usuario.UserDtos;
import com.sisge.ms.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdminUserController {
    private final UserService service;

    public AdminUserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDtos.UserSummary> list(@RequestParam(required = false) String q) {
        return service.listUsers(q);
    }

    @GetMapping("/membresias")
    public List<UserDtos.UserOrganizationRow> listOrganizations(@RequestParam(required = false) String q) {
        return service.listUserOrganizations(q);
    }

    @GetMapping("/{usuarioId}")
    public UserDtos.UserSummary get(@PathVariable Long usuarioId) {
        return service.getUser(usuarioId);
    }

    @PutMapping("/{usuarioId}/rol-global")
    public UserDtos.GlobalRoleUpdated changeGlobalRole(@PathVariable Long usuarioId,
            @Valid @RequestBody UserDtos.GlobalRoleChange request) {
        return service.changeGlobalRole(usuarioId, request);
    }
}
