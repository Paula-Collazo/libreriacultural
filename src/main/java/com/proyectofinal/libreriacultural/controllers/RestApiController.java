package com.proyectofinal.libreriacultural.controllers;

import com.proyectofinal.libreriacultural.Repositories.*;
import com.proyectofinal.libreriacultural.domain.*;
import com.proyectofinal.libreriacultural.services.UserAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class RestApiController {

    private final UserRepository userRepository;
    private final UserContentRepository userContentRepository;
    private final CustomListRepository customListRepository;
    private final UserAccountService userAccountService;
    private final FriendshipRepository friendshipRepository;

    public RestApiController(UserRepository userRepository, 
                           UserContentRepository userContentRepository,
                           CustomListRepository customListRepository,
                           UserAccountService userAccountService,
                           FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.userContentRepository = userContentRepository;
        this.customListRepository = customListRepository;
        this.userAccountService = userAccountService;
        this.friendshipRepository = friendshipRepository;
    }

    private User getSessionUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("sessionUserId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        User user = userAccountService.authenticate(credentials.get("username"), credentials.get("password"));
        if (user != null) {
            session.setAttribute("sessionUserId", user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body("No autorizado");

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("content", userContentRepository.findByUserId(user.getId()));
        data.put("lists", customListRepository.findByUser(user));
        
        return ResponseEntity.ok(data);
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body("No autorizado");

        List<Friendship> friends = friendshipRepository.findByRequesterOrReceiverAndStatus(user, user, Friendship.FriendshipStatus.ACCEPTED);
        return ResponseEntity.ok(friends);
    }
}
