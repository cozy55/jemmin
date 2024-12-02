package com.cozy.jemmin.service.impl;

import com.cozy.jemmin.annotation.AdminId;
import com.cozy.jemmin.dto.AdminFields;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModelProcessor {

    public static AdminFields getShit(Class<?> clazz){
        var modelFields = Arrays.stream(clazz.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        //check ids annotation
        var modelIds = modelFields.entrySet().stream().filter(e -> e.getValue().isAnnotationPresent(AdminId.class))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        if(modelIds.size() > 1){
            throw new RuntimeException(String.format("%s model has multiple @AdminId annotation", clazz.getName()));
        }
        var modelId = modelIds.entrySet().stream().findFirst().orElseThrow(() ->new RuntimeException(String.format("%s model has no @AdminId annotation", clazz.getName())));

        modelFields.remove(modelId.getKey(), modelId.getValue());
        return new AdminFields(modelId.getValue(), modelFields.values());
    }
}

