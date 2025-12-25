package com.agenthub.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "agent_run")
public class AgentRunEntity {
    @Id
    private UUID id;

    @Column(name = "agent_name", nullable = false, length = 200)
    private String agentName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "stats_json", columnDefinition = "TEXT")
    private String statsJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}


