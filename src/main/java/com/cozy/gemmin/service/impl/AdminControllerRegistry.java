package com.cozy.gemmin.service.impl;

import com.cozy.gemmin.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AdminControllerRegistry {
    private final Map<String, AdminController> adminControllerMapping = new HashMap<>();
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping handlerMapping;

    public AdminControllerRegistry addService(String mapping, Class<? extends AdminService<?>> adminServiceClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var adminController = new ByteBuddy()
                .subclass(AdminController.class)
                .name(String.format("AdminController%s", adminServiceClass.getTypeName()))
                .annotateType(AnnotationDescription.Builder.ofType(Controller.class).build())
                .make()
                .load(getClass().getClassLoader())
                .getLoaded()
                .getDeclaredConstructor(AdminService.class, ObjectMapper.class).newInstance(applicationContext.getBean(adminServiceClass), objectMapper);

        adminControllerMapping.put(mapping, adminController);
        return this;
    }

    public void register(){
        adminControllerMapping.forEach((mapping, adminController) -> {
            try {
                handlerMapping.registerMapping(
                        RequestMappingInfo.paths(mapping)
                                .methods(RequestMethod.GET)
                                .produces(MediaType.APPLICATION_JSON_VALUE)
                                .build(),
                        adminController,
                        adminController.getClass().getMethod("getAll", Model.class)
                );
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("Registered {} admin endpoints", adminControllerMapping.size());
    }
}
