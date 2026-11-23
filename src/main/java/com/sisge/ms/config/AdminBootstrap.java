package com.sisge.ms.config;

import com.sisge.ms.entity.Rol;
import com.sisge.ms.entity.Usuario;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    @Bean
    CommandLineRunner bootstrapAdmin(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
            @Value("${app.bootstrap.admin.email}") String email,
            @Value("${app.bootstrap.admin.password}") String password,
            @Value("${app.bootstrap.admin.username:admin}") String username) {
        return args -> {
            if (!email.isBlank() && !password.isBlank() && users.findByEmail(email).isEmpty()) {
                Rol role = roles.findByNombre("ADMIN_GENERAL").orElseThrow(() -> new IllegalStateException("Run database/02-reference-data.sql before bootstrapping"));
                Usuario user = new Usuario();
                user.bootstrap(email, username == null || username.isBlank()
                        ? "admin" : username.trim().toLowerCase(Locale.ROOT), encoder.encode(password), role);
                users.save(user);
            }
        };
    }
}
