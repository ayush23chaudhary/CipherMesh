package com.ciphermesh.room.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class RoomResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private int maxUsers;
    private int memberCount;
    private boolean isPrivate;
    private Set<String> tags;
    private UUID creatorId;
    private String creatorUsername;
    private Instant createdAt;
}
