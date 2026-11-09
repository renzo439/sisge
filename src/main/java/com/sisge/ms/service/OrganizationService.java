package com.sisge.ms.service;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private static final String PERSONAL_IMPORTAR = "PERSONAL_IMPORTAR";

    private final OrganizationRepository organizations;

    public OrganizationService(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDtos.Summary> listForPersonnelImport() {
        CurrentUser user = CurrentUser.get();
        if (user.global()) {
            user.requireGlobal(PERSONAL_IMPORTAR);
            return organizations.findAll(Sort.by("nombre"))
                    .stream()
                    .map(organization -> new OrganizationDtos.Summary(
                            organization.getId(), organization.getNombre(), organization.getUdeo()))
                    .toList();
        }

        return organizations.findAllById(user.organizationPermissions().entrySet().stream()
                        .filter(entry -> entry.getValue().contains(PERSONAL_IMPORTAR))
                        .map(java.util.Map.Entry::getKey)
                        .toList())
                .stream()
                .sorted(Comparator.comparing(organization -> organization.getNombre().toLowerCase()))
                .map(organization -> new OrganizationDtos.Summary(
                        organization.getId(), organization.getNombre(), organization.getUdeo()))
                .toList();
    }
}
