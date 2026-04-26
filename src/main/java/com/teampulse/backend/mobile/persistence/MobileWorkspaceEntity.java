package com.teampulse.backend.mobile.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
public class MobileWorkspaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean initialized;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName = "";

    @Column(name = "user_email", nullable = false, length = 191)
    private String userEmail = "";

    @Column(name = "team_name", nullable = false, length = 80)
    private String teamName = "";

    @Column(name = "course_name", nullable = false, length = 80)
    private String courseName = "";

    @Column(nullable = false, length = 20)
    private String semester = "2026-1";

    @Column(name = "due_date", nullable = false, length = 10)
    private String dueDate = "";

    @Column(name = "invite_code", nullable = false, length = 16)
    private String inviteCode = "";

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MobileMemberEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MobileTaskEntity> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MobileMeetingEntity> meetings = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MobileActivityEntity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MobileReportEntity> reports = new ArrayList<>();
}
