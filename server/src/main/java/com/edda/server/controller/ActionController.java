package com.edda.server.controller;

import com.edda.server.dto.ActionResponse;
import com.edda.server.repository.ActionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionRepository actionRepository;

    @GetMapping
    public List<ActionResponse> getActions() {
        return actionRepository.findAll().stream()
                .map(action -> new ActionResponse(action.getKey(), action.getName()))
                .toList();
    }
}