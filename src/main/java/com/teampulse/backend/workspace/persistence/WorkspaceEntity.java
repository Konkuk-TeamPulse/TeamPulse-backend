package com.teampulse.backend.workspace.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment2_workspaces")
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean initialized;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName = "";

    @Column(name = "user_email", nullable = false, length = 191)
    private String userEmail = "";

    @Column(name = "user_university", nullable = false, length = 80)
    private String userUniversity = "";

    @Column(name = "user_phone", nullable = false, length = 30)
    private String userPhone = "";

    @Column(name = "owner_email", nullable = false, length = 191, columnDefinition = "varchar(191) not null default ''")
    private String ownerEmail = "";

    @Column(name = "team_name", nullable = false, length = 80)
    private String teamName = "";

    @Column(name = "course_name", nullable = false, length = 80)
    private String courseName = "";

    @Column(nullable = false, length = 20)
    private String semester = "2026-1";

    @Column(name = "due_date", nullable = false, length = 10)
    private String dueDate = "";

    @Column(length = 500)
    private String description = "";

    @Column(name = "start_date", nullable = false, length = 10)
    private String startDate = "";

    @Column(name = "invite_code", nullable = false, length = 64)
    private String inviteCode = "";

    @Column(name = "invite_expires_at")
    private LocalDateTime inviteExpiresAt;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkspaceMemberEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkspaceTaskEntity> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkspaceMeetingEntity> meetings = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkspaceActivityEntity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkspaceReportEntity> reports = new ArrayList<>();
}
