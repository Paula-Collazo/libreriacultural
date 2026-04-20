package com.proyectofinal.libreriacultural.controllers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.proyectofinal.libreriacultural.Repositories.ContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.Repositories.UserSeriesEpisodeProgressRepository;
import com.proyectofinal.libreriacultural.Repositories.UserSongProgressRepository;
import com.proyectofinal.libreriacultural.domain.Content;
import com.proyectofinal.libreriacultural.domain.User;
import com.proyectofinal.libreriacultural.domain.UserContent;

class UserContentControllerTest {

        private final UserContentRepository userContentRepository = mock(UserContentRepository.class);
        private final UserRepository userRepository = mock(UserRepository.class);
        private final ContentRepository contentRepository = mock(ContentRepository.class);
        private final UserSeriesEpisodeProgressRepository userSeriesEpisodeProgressRepository = mock(
                UserSeriesEpisodeProgressRepository.class);
        private final UserSongProgressRepository userSongProgressRepository = mock(UserSongProgressRepository.class);
        private final UserContentController controller = new UserContentController(userContentRepository, userRepository,
                contentRepository, userSeriesEpisodeProgressRepository, userSongProgressRepository);

    @Test
        void createMovieEntryShouldReturnCreatedData() {
        User user = new User();
        user.setId(1L);

        Content content = new Content();
        content.setId(2L);
        content.setType("pelicula");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(2L)).thenReturn(Optional.of(content));
        when(userContentRepository.existsByUserIdAndContentId(1L, 2L)).thenReturn(false);
        when(userContentRepository.save(any(UserContent.class))).thenAnswer(invocation -> {
            UserContent saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

                UserContent result = controller
                                .addToLibrary(new UserContentController.AddToLibraryRequest(1L, 2L, "visto", null, null));

                assertEquals(10L, result.getId());
                assertEquals("visto", result.getStatus());
                assertEquals(true, result.getMovieWatched());
                assertNotNull(result.getAddedDate());
    }

    @Test
        void createLibraryEntryShouldReturnConflictWhenDuplicate() {
        User user = new User();
        user.setId(1L);

        Content content = new Content();
        content.setId(2L);
        content.setType("pelicula");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(2L)).thenReturn(Optional.of(content));
        when(userContentRepository.existsByUserIdAndContentId(1L, 2L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.addToLibrary(new UserContentController.AddToLibraryRequest(1L, 2L, "visto", null,
                null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
        void updateStatusShouldReturnBadRequestForMovieType() {
        Content content = new Content();
        content.setType("pelicula");

        UserContent entry = new UserContent();
        entry.setId(5L);
        entry.setContent(content);

        when(userContentRepository.findById(5L)).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateStatus(5L, new UserContentController.UpdateStatusRequest("terminado")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void statsShouldReturnCountsByStatus() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userContentRepository.countByUserId(1L)).thenReturn(4L);
        when(userContentRepository.countByUserIdAndContentType(1L, "pelicula")).thenReturn(1L);
        when(userContentRepository.countByUserIdAndContentType(1L, "serie")).thenReturn(1L);
        when(userContentRepository.countByUserIdAndContentType(1L, "libro")).thenReturn(1L);
        when(userContentRepository.countByUserIdAndContentType(1L, "disco")).thenReturn(1L);
        when(userContentRepository.countByUserIdAndStatus(1L, "visto")).thenReturn(2L);
        when(userContentRepository.countByUserIdAndStatus(1L, "no_visto")).thenReturn(2L);

        UserContentController.LibraryStatsResponse stats = controller.getUserStats(1L);

        assertEquals(4L, stats.total());
        assertEquals(1L, stats.byType().get("pelicula"));
        assertEquals(1L, stats.byType().get("serie"));
        assertEquals(1L, stats.byType().get("libro"));
        assertEquals(1L, stats.byType().get("disco"));
        assertEquals(2L, stats.byMovieStatus().get("visto"));
        assertEquals(2L, stats.byMovieStatus().get("no_visto"));
    }
}
