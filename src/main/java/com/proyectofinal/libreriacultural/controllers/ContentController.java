package com.proyectofinal.libreriacultural.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectofinal.libreriacultural.Repositories.ContentRepository;
import com.proyectofinal.libreriacultural.domain.Content;

@RestController
@RequestMapping("/content")
public class ContentController {

    @Autowired
    private ContentRepository contentRepository;

    @GetMapping
    public List<Content> getAll() {
        return contentRepository.findAll();
    }

    @PostMapping
    public Content create(@RequestBody Content content) {
        return contentRepository.save(content);
    }
}