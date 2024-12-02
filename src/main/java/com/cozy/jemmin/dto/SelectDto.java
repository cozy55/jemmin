package com.cozy.jemmin.dto;

import java.util.List;

public record SelectDto(String type, String name, List<String> allValues, Object selectedValue) {
}
