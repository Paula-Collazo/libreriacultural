package com.proyectofinal.libreriacultural.Repositories;

import com.proyectofinal.libreriacultural.domain.Friendship;
import com.proyectofinal.libreriacultural.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByReceiverAndStatus(User receiver, Friendship.FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.receiver = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAllAcceptedFriends(@Param("user") User user);

    Optional<Friendship> findByRequesterAndReceiver(User requester, User receiver);

    @Query("SELECT f FROM Friendship f WHERE ((f.requester = :u1 AND f.receiver = :u2) OR (f.requester = :u2 AND f.receiver = :u1))")
    Optional<Friendship> findBetweenUsers(@Param("u1") User u1, @Param("u2") User u2);

    List<Friendship> findByRequesterOrReceiverAndStatus(User requester, User receiver, Friendship.FriendshipStatus status);
}
