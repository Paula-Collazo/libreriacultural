package com.proyectofinal.libreriacultural;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.proyectofinal.libreriacultural.Repositories.ContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.domain.Content;
import com.proyectofinal.libreriacultural.domain.User;

@SpringBootApplication
public class LibreriaculturalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibreriaculturalApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UserRepository userRepo, ContentRepository contentRepo) {
        return args -> {
            // Limpieza de duplicados si existen (para arreglar el error de "visto 53 resultados")
            java.util.List<User> paulas = userRepo.findAll().stream()
                .filter(u -> "paula".equals(u.getUsername()))
                .collect(java.util.stream.Collectors.toList());
            
            if (paulas.size() > 1) {
                System.out.println("Detectados " + paulas.size() + " usuarios 'paula'. Limpiando...");
                for (int i = 1; i < paulas.size(); i++) {
                    userRepo.delete(paulas.get(i));
                }
                System.out.println("Limpieza finalizada. Solo queda 1 'paula'.");
            } else if (paulas.isEmpty()) {
                User user = new User();
                user.setUsername("paula");
                user.setEmail("paula@test.com");
                user.setPassword("1234");
                userRepo.save(user);
                System.out.println("Usuario 'paula' insertado.");
            }

            if (contentRepo.findAll().isEmpty()) {
                Content content = new Content();
                content.setTitle("Breaking Bad");
                content.setType("serie");
                contentRepo.save(content);
                System.out.println("Contenido de prueba insertado.");
            }

            System.out.println("Inicialización finalizada.");
        };
    }

}
