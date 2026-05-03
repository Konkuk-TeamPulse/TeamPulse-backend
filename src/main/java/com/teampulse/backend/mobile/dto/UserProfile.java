package com.teampulse.backend.mobile.dto;

public record UserProfile(String name, String email, String university, String phone) {

    public UserProfile(String name, String email) {
        this(name, email, "", "");
    }
}
