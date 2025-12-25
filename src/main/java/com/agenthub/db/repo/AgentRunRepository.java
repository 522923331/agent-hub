package com.agenthub.db.repo;

import com.agenthub.db.entity.AgentRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentRunRepository extends JpaRepository<AgentRunEntity, UUID> {
}


