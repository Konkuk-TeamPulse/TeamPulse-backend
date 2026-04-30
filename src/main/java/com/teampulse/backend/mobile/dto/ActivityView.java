package com.teampulse.backend.mobile.dto;

public record ActivityView(long id, String actor, String at, String updatedAt, String summary) {

    public ActivityView(long id, String actor, String at, String summary) {
        this(id, actor, at, at, summary);
    }
}
