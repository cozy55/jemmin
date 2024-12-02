package com.cozy.jemmin.dto;

import java.lang.reflect.Field;
import java.util.Collection;

public record AdminFields(Field id, Collection<Field> fields){}

