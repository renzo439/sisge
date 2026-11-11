package com.sisge.ms.controller;

import com.sisge.ms.dto.usuario.UserDtos;
import com.sisge.ms.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizaciones/{organizacionId}/usuarios")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDtos.Membership> list(@PathVariable Long organizacionId) {
        return service.listForOrganization(organizacionId);
    }

    @GetMapping("/{usuarioId}")
    public UserDtos.Membership get(@PathVariable Long organizacionId, @PathVariable Long usuarioId) {
        return service.getMembership(organizacionId, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.Created create(@PathVariable Long organizacionId, @Valid @RequestBody UserDtos.Create request) {
        return service.createForOrganization(organizacionId, request);
    }

    @PutMapping("/{usuarioId}/rol")
    public UserDtos.MembershipRoleUpdated changeRole(@PathVariable Long organizacionId,
            @PathVariable Long usuarioId, @Valid @RequestBody UserDtos.RoleChange request) {
        return service.changeMembershipRole(organizacionId, usuarioId, request);
    }
}
