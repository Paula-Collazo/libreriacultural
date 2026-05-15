package com.proyectofinal.libreriacultural.Repositories;

import com.proyectofinal.libreriacultural.domain.CustomList;
import com.proyectofinal.libreriacultural.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomListRepository extends JpaRepository<CustomList, Long> {
    List<CustomList> findByUser(User user);
}
