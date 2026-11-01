package com.mango.products.entity;

public class UserGrantEntity {

    private String username;
    private String endpoint;

    public UserGrantEntity(String username, String endpoint) {
        this.username = username;
        this.endpoint = endpoint;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
