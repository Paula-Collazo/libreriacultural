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

            User user = new User();
            user.setUsername("paula");
            user.setEmail("paula@test.com");
            user.setPassword("1234");

            userRepo.save(user);

            Content content = new Content();
            content.setTitle("Breaking Bad");
            content.setType("serie");

            contentRepo.save(content);

            System.out.println("Datos insertados correctamente");
        };
    }

}
