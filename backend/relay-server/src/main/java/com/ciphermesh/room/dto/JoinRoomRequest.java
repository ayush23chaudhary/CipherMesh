package com.ciphermesh.room.dto;

import lombok.Data;

/** Optional body for joining a private room. */
@Data
public class JoinRoomRequest {
    private String password;
}
