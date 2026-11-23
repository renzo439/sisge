package com.sisge.ms.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sisge.ms.entity.Importacion;
import com.sisge.ms.entity.ImportacionPersonal;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.entity.Permission;
import com.sisge.ms.entity.Rol;
import com.sisge.ms.entity.UserOrganization;
import com.sisge.ms.entity.Usuario;
import com.sisge.ms.repository.ImportacionPersonalRepository;
import com.sisge.ms.repository.ImportacionRepository;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.PermissionRepository;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class RestrictedUserAccessTests {
    private static final MockMultipartFile FILE = new MockMultipartFile(
            "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1});

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private OrganizationRepository organizations;
    @Autowired private PermissionRepository permissions;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private UserOrganizationRepository memberships;
    @Autowired private ImportacionRepository imports;
    @Autowired private ImportacionPersonalRepository personalImports;
    @Autowired private UserAuthenticationService authentication;
    @Autowired private JwtService jwt;

    private MockMvc mvc;
    private String token;
    private Long organizationId;
    private Long importacionId;
    private Long personalImportId;

    @BeforeEach
    void setUp() {
        Permission permission = new Permission();
        permission.setCodigo("REEMPLAZANTES_VER");
        permissions.save(permission);

        Rol role = new Rol();
        role.setNombre("REEMPLAZOS_ONLY");
        role.setGlobalRole(false);
        role.setPermisos(Set.of(permission));
        roles.save(role);

        Organizacion organization = new Organizacion();
        organization.setNombre("Hospital Permitido");
        organization.setUdeo("HP");
        organizations.save(organization);
        organizationId = organization.getId();

        Usuario user = new Usuario();
        user.register("Ana", "Reemplazos", "reemplazos@example.com", "reemplazos", "unused-hash");
        users.save(user);

        UserOrganization membership = new UserOrganization();
        membership.assign(user, organization, role);
        memberships.save(membership);

        Importacion importacion = new Importacion();
        importacion.setOrganizacion(organization);
        importacion.setArchivoOrigen("test.xlsx");
        importacion.setCreadoPor(user);
        imports.save(importacion);
        importacionId = importacion.getId();

        ImportacionPersonal personalImport = new ImportacionPersonal();
        personalImport.setOrganizacion(organization);
        personalImport.setCreadoPor(user);
        personalImport.setArchivoOrigen("test.xlsx");
        personalImport.setCounts(0, 0, 0);
        personalImports.save(personalImport);
        personalImportId = personalImport.getId();

        token = jwt.create(authentication.load(user.getEmail()));
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain, "/*")
                .build();
    }

    @Test
    void deniesEveryNonReplacementEndpoint() throws Exception {
        for (Call call : forbiddenCalls()) {
            int actual = mvc.perform(build(call)).andReturn().getResponse().getStatus();
            assertEquals(403, actual, call.description());
        }
    }

    @Test
    void allowsReplacementSearchForAssignedOrganization() throws Exception {
        mvc.perform(auth(get("/api/v1/reemplazantes").param("organizacionId", organizationId.toString())))
                .andExpect(status().isOk());
    }

    @Test
    void generatesReplacementReportForAssignedOrganization() throws Exception {
        mvc.perform(auth(get("/api/v1/reemplazantes/reporte").param("organizacionId", organizationId.toString())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    void requiresOrganizationIdForNonGlobalReplacementSearch() throws Exception {
        mvc.perform(auth(get("/api/v1/reemplazantes"))).andExpect(status().isBadRequest());
    }

    @Test
    void deniesReplacementSearchForUnassignedOrganization() throws Exception {
        mvc.perform(auth(get("/api/v1/reemplazantes").param("organizacionId", "999999")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meReturnsEffectivePermissionsFromAuthorizationSources() throws Exception {
        mvc.perform(auth(get("/api/v1/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalPermissions").isEmpty())
                .andExpect(jsonPath("$.permissions[0]").value("REEMPLAZANTES_VER"))
                .andExpect(jsonPath("$.organizationPermissions['" + organizationId + "'][0]")
                        .value("REEMPLAZANTES_VER"))
                .andExpect(jsonPath("$.organizaciones.length()").value(1));
    }

    private List<Call> forbiddenCalls() {
        String reason = "{\"motivo\":\"x\"}";
        String roleCreate = "{\"nombre\":\"ROL_X\",\"descripcion\":\"x\",\"globalRole\":false,\"permisos\":[]}";
        String permissionCreate = "{\"codigo\":\"PERMISO_X\",\"descripcion\":\"x\"}";
        String organizationCreate = "{\"nombre\":\"Hospital X\",\"udeo\":\"HX\"}";
        String userCreate = "{\"nombre\":\"Nuevo\",\"apellido\":\"Usuario\",\"username\":\"nuevo.usuario\","
                + "\"email\":\"nuevo@example.com\",\"rol\":\"OPERADOR\"}";
        String personalRow = "{\"dni\":\"123\",\"nombres\":\"A\",\"apellido\":\"B\","
                + "\"organizacionId\":999999,\"afectado\":false}";

        return List.of(
                new Call("GET /organizaciones", "GET", "/api/v1/organizaciones", null, false),
                new Call("GET /roles", "GET", "/api/v1/roles", null, false),
                new Call("GET /roles/{id}", "GET", "/api/v1/roles/999999", null, false),
                new Call("POST /roles", "POST", "/api/v1/roles", roleCreate, false),
                new Call("PUT /roles/{id}", "PUT", "/api/v1/roles/999999", roleCreate, false),
                new Call("DELETE /roles/{id}", "DELETE", "/api/v1/roles/999999", null, false),
                new Call("GET /permisos", "GET", "/api/v1/permisos", null, false),
                new Call("GET /permisos/{id}", "GET", "/api/v1/permisos/999999", null, false),
                new Call("POST /permisos", "POST", "/api/v1/permisos", permissionCreate, false),
                new Call("PUT /permisos/{id}", "PUT", "/api/v1/permisos/999999", permissionCreate, false),
                new Call("DELETE /permisos/{id}", "DELETE", "/api/v1/permisos/999999", null, false),
                new Call("GET /admin/organizaciones", "GET", "/api/v1/admin/organizaciones", null, false),
                new Call("GET /admin/organizaciones/{id}", "GET", "/api/v1/admin/organizaciones/999999", null, false),
                new Call("POST /admin/organizaciones", "POST", "/api/v1/admin/organizaciones", organizationCreate, false),
                new Call("PUT /admin/organizaciones/{id}", "PUT", "/api/v1/admin/organizaciones/999999", organizationCreate, false),
                new Call("DELETE /admin/organizaciones/{id}", "DELETE", "/api/v1/admin/organizaciones/999999", null, false),
                new Call("GET /admin/usuarios", "GET", "/api/v1/admin/usuarios", null, false),
                new Call("GET /admin/usuarios/membresias", "GET", "/api/v1/admin/usuarios/membresias", null, false),
                new Call("GET /admin/usuarios/{id}", "GET", "/api/v1/admin/usuarios/999999", null, false),
                new Call("PUT /admin/usuarios/{id}/rol-global", "PUT", "/api/v1/admin/usuarios/999999/rol-global",
                        "{\"rol\":\"ADMIN_GENERAL\"}", false),
                new Call("GET /admin/importaciones", "GET", "/api/v1/admin/importaciones", null, false),
                new Call("POST /admin/importaciones/{id}/observar", "POST", "/api/v1/admin/importaciones/" + importacionId + "/observar", reason, false),
                new Call("POST /admin/importaciones/{id}/rechazar", "POST", "/api/v1/admin/importaciones/" + importacionId + "/rechazar", reason, false),
                new Call("POST /admin/importaciones/{id}/aprobar", "POST", "/api/v1/admin/importaciones/999999/aprobar", null, false),
                new Call("POST /admin/importaciones/{id}/cerrar", "POST", "/api/v1/admin/importaciones/999999/cerrar", null, false),
                new Call("POST /admin/importaciones/organizaciones/{id}/migrar-historico", "POST",
                        "/api/v1/admin/importaciones/organizaciones/999999/migrar-historico", null, true),
                new Call("POST /admin/importaciones/migrar-historico-consolidado", "POST",
                        "/api/v1/admin/importaciones/migrar-historico-consolidado", null, true),
                new Call("POST /admin/personal/importaciones/{id}/validar", "POST",
                        "/api/v1/admin/personal/importaciones/999999/validar", null, false),
                new Call("POST /admin/personal/importaciones/{id}/consolidar", "POST",
                        "/api/v1/admin/personal/importaciones/999999/consolidar", null, false),
                new Call("GET /organizaciones/{id}/usuarios", "GET", "/api/v1/organizaciones/999999/usuarios", null, false),
                new Call("GET /organizaciones/{id}/usuarios/{id}", "GET", "/api/v1/organizaciones/999999/usuarios/999999", null, false),
                new Call("POST /organizaciones/{id}/usuarios", "POST", "/api/v1/organizaciones/999999/usuarios", userCreate, false),
                new Call("PUT /organizaciones/{id}/usuarios/{id}/rol", "PUT",
                        "/api/v1/organizaciones/999999/usuarios/999999/rol", "{\"rol\":\"OPERADOR\"}", false),
                new Call("POST /organizaciones/{id}/importaciones/upload", "POST",
                        "/api/v1/organizaciones/999999/importaciones/upload", null, true),
                new Call("GET /importaciones/{id}", "GET", "/api/v1/importaciones/" + importacionId, null, false),
                new Call("GET /importaciones", "GET", "/api/v1/importaciones?organizacionId=999999", null, false),
                new Call("PUT /importaciones/{id}/reemplazantes/{id}", "PUT",
                        "/api/v1/importaciones/" + importacionId + "/reemplazantes/999999", "{}", false),
                new Call("POST /importaciones/{id}/enviar-revision", "POST",
                        "/api/v1/importaciones/" + importacionId + "/enviar-revision", null, false),
                new Call("POST /organizaciones/{id}/personal/importaciones", "POST",
                        "/api/v1/organizaciones/999999/personal/importaciones", null, true),
                new Call("GET /personal/importaciones", "GET", "/api/v1/personal/importaciones", null, false),
                new Call("GET /personal/importaciones/{id}", "GET", "/api/v1/personal/importaciones/" + personalImportId, null, false),
                new Call("PUT /personal/importaciones/{id}/filas/{id}", "PUT",
                        "/api/v1/personal/importaciones/" + personalImportId + "/filas/999999", personalRow, false),
                new Call("POST /personal/importaciones/{id}/aprobar", "POST",
                        "/api/v1/personal/importaciones/" + personalImportId + "/aprobar", null, false),
                new Call("GET /personas", "GET", "/api/v1/personas?organizacionId=999999", null, false));
    }

    private RequestBuilder build(Call call) {
        if (call.multipart()) {
            var builder = multipart(call.path()).file(FILE);
            return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        MockHttpServletRequestBuilder builder;
        switch (call.method()) {
            case "GET" -> builder = get(call.path());
            case "POST" -> builder = post(call.path());
            case "PUT" -> builder = put(call.path());
            case "DELETE" -> builder = delete(call.path());
            default -> throw new IllegalArgumentException(call.method());
        }
        if (call.body() != null) builder.contentType(MediaType.APPLICATION_JSON).content(call.body());
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private record Call(String description, String method, String path, String body, boolean multipart) {}
}
