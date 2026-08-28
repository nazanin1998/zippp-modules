package com.zippp.signature.dto;

public class ParsedJwtDto {
    public String getUser() {
        return user;
    }

    public ParsedJwtDto setUser(String user) {
        this.user = user;
        return this;
    }

    public String getValue() {
        return value;
    }

    public ParsedJwtDto setValue(String value) {
        this.value = value;
        return this;
    }

    private String user;
    private String value;
}
