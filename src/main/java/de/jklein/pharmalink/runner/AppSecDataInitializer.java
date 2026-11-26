package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.domain.auth.Role;
import de.jklein.pharmalink.domain.auth.User;
import de.jklein.pharmalink.repository.auth.RoleRepository;
import de.jklein.pharmalink.repository.auth.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class AppSecDataInitializer implements CommandLineRunner {

    @Value("${system.benutzername}")
    private String benutzername;

    @Value("${system.passwort}")
    private Path passwort;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppSecDataInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Role adminRole = createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("USER");

        if (userRepository.findByUsername(benutzername) == null) {
            User adminUser = new User();
            adminUser.setUsername(benutzername);
            adminUser.setPassword(passwordEncoder.encode(passwort.toString()));
            adminUser.setRoles(Collections.singleton(adminRole)); // Correct assignment of role to user
            userRepository.save(adminUser);
            log.info("Admin user '{}' created.", benutzername);
        } else {
            log.info("Admin user '{}' already exists.", benutzername);
        }
    }

    private Role createRoleIfNotExists(String roleName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
            log.info("Role '{}' created.", roleName);
        } else {
            log.info("Role '{}' already exists.", roleName);
        }
        return role;
    }
}