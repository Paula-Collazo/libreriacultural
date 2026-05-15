package com.proyectofinal.libreriacultural.controllers;

import com.proyectofinal.libreriacultural.Repositories.FriendshipRepository;
import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.domain.Friendship;
import com.proyectofinal.libreriacultural.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/comunidad")
public class UserSocialController {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    private User getSessionUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("sessionUserId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @GetMapping("/friends")
    public String listFriends(Model model, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        List<Friendship> requests = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.PENDING);
        List<Friendship> friends = friendshipRepository.findAllAcceptedFriends(user);

        model.addAttribute("pendingRequests", requests);
        model.addAttribute("friends", friends);
        model.addAttribute("user", user);
        return "social/friends";
    }

    @PostMapping("/add")
    public String sendRequest(@RequestParam String username, HttpSession session, RedirectAttributes redirectAttributes) {
        User requester = getSessionUser(session);
        if (requester == null) return "redirect:/login";

        Optional<User> receiverOpt = userRepository.findByUsername(username);
        if (receiverOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Usuario no encontrado");
            return "redirect:/comunidad/friends";
        }

        User receiver = receiverOpt.get();
        if (receiver.getId().equals(requester.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No puedes agregarte a ti mismo");
            return "redirect:/social/friends";
        }

        if (friendshipRepository.findBetweenUsers(requester, receiver).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ya existe una solicitud o amistad");
            return "redirect:/comunidad/friends";
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        friendship.setStatus(Friendship.FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        redirectAttributes.addFlashAttribute("successMessage", "Solicitud enviada a " + username);
        return "redirect:/comunidad/friends";
    }

    @PostMapping("/accept/{id}")
    public String acceptRequest(@PathVariable Long id, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        friendshipRepository.findById(id).ifPresent(f -> {
            if (f.getReceiver().getId().equals(user.getId())) {
                f.setStatus(Friendship.FriendshipStatus.ACCEPTED);
                friendshipRepository.save(f);
            }
        });
        return "redirect:/comunidad/friends";
    }
}
