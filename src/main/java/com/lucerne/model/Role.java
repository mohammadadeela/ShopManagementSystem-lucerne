package com.lucerne.model;

public enum Role {
    OWNER, ADMIN, MANAGER, CASHIER, WAREHOUSE, CUSTOMER;

    public static Role fromDatabase(String value) {
        return Role.valueOf(value == null ? "CUSTOMER" : value.trim().toUpperCase());
    }
}
