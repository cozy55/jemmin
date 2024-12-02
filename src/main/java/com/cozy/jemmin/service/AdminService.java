package com.cozy.jemmin.service;

import com.cozy.jemmin.example.model.Library;

import java.util.List;

public interface AdminService<T> {

    List<T> getAll();

    T get(int id);

    T save(T object);

    T update(T object);

    void delete(int id);
}
