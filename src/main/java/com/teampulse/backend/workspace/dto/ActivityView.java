package com.teampulse.backend.workspace.dto;

public record ActivityView(long id, String actor, String at, String updatedAt, String summary) {

    public ActivityView(long id, String actor, String at, String summary) {
        this(id, actor, at, at, summary);
    }
}
