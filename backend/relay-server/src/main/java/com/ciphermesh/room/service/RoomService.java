package com.ciphermesh.room.service;

import com.ciphermesh.room.dto.CreateRoomRequest;
import com.ciphermesh.room.dto.JoinRoomRequest;
import com.ciphermesh.room.dto.RoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoomService {

    RoomResponse create(CreateRoomRequest request, UUID creatorId);

    Page<RoomResponse> listPublic(String category, String search, Pageable pageable);

    RoomResponse getById(UUID roomId);

    RoomResponse join(UUID roomId, UUID userId, JoinRoomRequest request);

    void leave(UUID roomId, UUID userId);

    long countPublic();
}
