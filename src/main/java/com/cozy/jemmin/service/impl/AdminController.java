package com.cozy.jemmin.service.impl;

import com.cozy.jemmin.dto.AdminFields;
import com.cozy.jemmin.dto.InputDto;
import com.cozy.jemmin.dto.SelectDto;
import com.cozy.jemmin.dto.ViewData;
import com.cozy.jemmin.dto.ViewItem;
import com.cozy.jemmin.service.AdminService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

public class AdminController {
    private final AdminService<Object> adminService;
    private final ObjectMapper objectMapper;
    private final List<String> mappings;
    private final String mapping;
    private final Class<?> objectType;
    private final AdminFields adminFields;
    private final Map<String, Field> typeFields;

    @SneakyThrows
    public AdminController(AdminService<Object> adminService, ObjectMapper objectMapper, List<String> mappings, String mapping) {
        this.adminService = adminService;
        this.objectMapper = objectMapper;
        this.mappings = mappings;
        this.mapping = mapping;

        this.objectType = (Class<?>)((ParameterizedType) adminService.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        this.adminFields = ModelProcessor.getShit(objectType);

        this.typeFields = Arrays.stream(this.objectType.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    @SneakyThrows
    public String listAllPage(Model model) {
        var objectMapper = this.objectMapper;


        objectMapper.reader();
        var typeReference = new TypeReference<Map<String, String>>() {
        };
        var viewItems = adminService.getAll().stream()
                .map(adminModel -> objectMapper.convertValue(adminModel, typeReference))
                .map(adminModelData -> {
                    var idName = adminFields.id().getName();
                    var idValue = adminModelData.get(idName);
                    return new ViewItem(new ViewData(idName, idValue),
                            adminFields.fields().stream()
                                    .map(field -> {
                                        var fieldName = field.getName();
                                        var fieldValue = adminModelData.get(fieldName);
                                        return new ViewData(fieldName, fieldValue);
                                    }).toList());
                        })
                .toList();

        var headers = new ArrayList<>();
        headers.add(adminFields.id().getName());
        headers.addAll(adminFields.fields().stream().map(Field::getName).toList());

        model.addAttribute("pageName", String.format("%s Page", objectType.getSimpleName()));
        model.addAttribute("headers", headers);
        model.addAttribute("viewItems", viewItems);
        model.addAttribute("mappings", mappings);
        model.addAttribute("currentMapping", mapping);
        return "listAll.html";
    }

    @SneakyThrows
    public String editPage(@PathVariable int id, Model model) {
        var typeReference = new TypeReference<Map<String, String>>() {
        };
        var adminModelData = objectMapper.convertValue(adminService.get(id), typeReference);

        model.addAttribute("pageName", String.format("Edit %s", objectType.getSimpleName()));
        model.addAttribute("mappings", mappings);
        model.addAttribute("items", getInputType(adminFields, adminModelData));
        model.addAttribute("object", objectType.getSimpleName().toLowerCase());
        model.addAttribute("submitPath", String.format("%s/edit/%d", mapping, id));

        return "form.html";
    }

    private List<Object> getInputType(AdminFields adminFields, Map<String, String> adminModelData) {
        List<Object> objects = new ArrayList<>();

        objects.add(getInputType(adminFields.id(), adminFields.id().getName(), adminModelData.get(adminFields.id().getName())));
        for (var field : adminFields.fields()) {
            objects.add(getInputType(field, field.getName(), adminModelData.get(field.getName())));
        }
        return objects;
    }

    private Object getInputType(Field field, String name, Object value){
        var type = field.getType();
        if (type.isEnum()) {
            var enumValues = Arrays.stream(type.getEnumConstants()).map(Object::toString).toList();
             return new SelectDto("enum", name, enumValues, value);
        } else {
            return new InputDto("text", name, value);
        }
    }

    @SneakyThrows
    public String edit(@PathVariable int id, @RequestParam Map<String, String> request) {
        System.out.println("PUT");
        System.out.println(request);

        var object = objectMapper.convertValue(request, objectType);
        adminService.update(object);
        return String.format("redirect:%s", mapping);
    }

    @SneakyThrows
    public String addPage(Model model) {
        model.addAttribute("pageName", String.format("Add %s", objectType.getSimpleName()));
        model.addAttribute("mappings", mappings);
        model.addAttribute("items", getInputType(adminFields, Map.of()));
        model.addAttribute("object", objectType.getSimpleName().toLowerCase());
        model.addAttribute("submitPath", String.format("%s/add", mapping));
        return "form.html";
    }

    @SneakyThrows
    public String add(@RequestParam Map<String, String> request) {
        System.out.println("POST");
        System.out.println(request);

        var object = objectMapper.convertValue(request, objectType);
        adminService.save(object);
        return String.format("redirect:%s", mapping);
    }

    public String delete(@PathVariable int id) {
        adminService.delete(id);
        return String.format("redirect:%s", mapping);
    }
}
