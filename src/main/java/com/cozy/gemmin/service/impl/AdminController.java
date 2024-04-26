package com.cozy.gemmin.service.impl;

import com.cozy.gemmin.service.AdminService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

@RequiredArgsConstructor
public class AdminController {
    private final AdminService<?> adminService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @GetMapping
    public String getAll(Model model) {
        var typeReference = new TypeReference<Map<String, String>>() {};
        var objectType = adminService.getClass().getDeclaredMethod("get", int.class).getReturnType();
        var typeFields = Arrays.stream(adminService.getClass().getDeclaredMethod("get", int.class).getReturnType().getDeclaredFields()).map(Field::getName).toList();
        var data = adminService.getAll().stream().map(item -> objectMapper.convertValue(item, typeReference).values()).toList();

        model.addAttribute("pageName", String.format("%s's Page", objectType.getSimpleName()));
        model.addAttribute("headers", typeFields);
        model.addAttribute("items", data);
        return "allBooks.html";
    }
}
