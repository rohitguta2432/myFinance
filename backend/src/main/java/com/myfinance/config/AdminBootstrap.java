package com.myfinance.config;

import com.myfinance.model.User;
import com.myfinance.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private static final List<String> BOOTSTRAP_ADMIN_EMAILS = List.of(
            "rohitgupta2432@gmail.com",
            "myfinancial.cfp@gmail.com");

    private final UserRepository userRepo;

    @Override
    public void run(String... args) {
        if (userRepo.countByIsAdminTrue() > 0) {
            return;
        }
        for (String email : BOOTSTRAP_ADMIN_EMAILS) {
            userRepo.findByEmail(email).ifPresent(u -> {
                u.setIsAdmin(true);
                userRepo.save(u);
                log.info("admin.bootstrap granted isAdmin=true to email={}", email);
            });
        }
    }
}
