package com.cozy.jemmin.service.impl;

import com.cozy.jemmin.annotation.AdminMapping;
import com.cozy.jemmin.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminControllerRegistry {
    private final Map<String, AdminController> adminControllerMapping = new HashMap<>();
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping handlerMapping;

    @SneakyThrows
    @PostConstruct
    public void init() {
        var adminServices = applicationContext.getBeansWithAnnotation(AdminMapping.class);
        var mappings = adminServices.values().stream().map(adminService -> adminService.getClass().getDeclaredAnnotation(AdminMapping.class).value()).toList();
        for (var adminServiceEntry : adminServices.entrySet()) {
            var adminMapping = adminServiceEntry.getValue().getClass().getDeclaredAnnotation(AdminMapping.class);
            adminControllerMapping.put(adminMapping.value(), getAdminController((AdminService<?>) adminServiceEntry.getValue(), mappings, adminMapping.value()));
        }
        register();
    }

    public AdminController getAdminController(AdminService<?> adminService, List<String> mappings, String mapping) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return new ByteBuddy()
                .subclass(AdminController.class)
                .name(String.format("%sAdminController", adminService.getClass().getTypeName()))
                .annotateType(AnnotationDescription.Builder.ofType(Controller.class).build())
                .make()
                .load(getClass().getClassLoader())
                .getLoaded()
                .getDeclaredConstructor(AdminService.class, ObjectMapper.class, List.class, String.class)
                .newInstance(adminService, objectMapper, mappings, mapping);
    }

    public void register() {
        adminControllerMapping.forEach((mapping, adminController) -> {
            try {
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(mapping)
                                .methods(RequestMethod.GET)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("listAllPage", Model.class)
                );
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(String.format("%s/edit/{id}", mapping))
                                .methods(RequestMethod.GET)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("editPage", int.class, Model.class)
                );
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(String.format("%s/edit/{id}", mapping))
                                .methods(RequestMethod.POST)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("edit", int.class, Map.class)
                );
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(String.format("%s/add", mapping))
                                .methods(RequestMethod.GET)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("addPage", Model.class)
                );
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(String.format("%s/add", mapping))
                                .methods(RequestMethod.POST)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("add", Map.class)
                );
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(String.format("%s/delete/{id}", mapping))
                                .methods(RequestMethod.POST)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("delete", int.class)
                );
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("Registered {} admin endpoints", adminControllerMapping.size());
    }
}
