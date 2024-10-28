package org.example.temp.model;

public enum Status {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    PENDING("PENDING");


    private String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}