package com.cozy.gemmin.service;

import java.util.List;

public interface AdminService<T> {

    List<T> getAll();

    T get(int id);

    T save(T object);
}
