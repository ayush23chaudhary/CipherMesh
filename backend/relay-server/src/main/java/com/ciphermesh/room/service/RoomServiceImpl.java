package com.ciphermesh.room.service;

import com.ciphermesh.common.exception.ConflictException;
import com.ciphermesh.common.exception.ResourceNotFoundException;
import com.ciphermesh.room.dto.CreateRoomRequest;
import com.ciphermesh.room.dto.JoinRoomRequest;
import com.ciphermesh.room.dto.RoomResponse;
import com.ciphermesh.room.entity.Room;
import com.ciphermesh.room.repository.RoomRepository;
import com.ciphermesh.user.entity.User;
import com.ciphermesh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RoomResponse create(CreateRoomRequest request, UUID creatorId) {
        if (roomRepository.existsByName(request.getName())) {
            throw new ConflictException("Room name '" + request.getName() + "' is already taken");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorId));

        String passwordHash = null;
        if (request.isPrivate() && request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        Room room = Room.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .maxUsers(request.getMaxUsers())
                .isPrivate(request.isPrivate())
                .passwordHash(passwordHash)
                .tags(request.getTags() != null ? request.getTags() : java.util.Set.of())
                .creator(creator)
                .build();

        room.getMembers().add(creator);
        room = roomRepository.save(room);
        log.info("Room '{}' created by user '{}'", room.getName(), creator.getUsername());
        return toResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponse> listPublic(String category, String search, Pageable pageable) {
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasSearch   = search    != null && !search.isBlank();

        Page<Room> page;
        if (hasCategory && hasSearch) {
            page = roomRepository.findByIsPrivateFalseAndCategoryAndNameContainingIgnoreCase(
                    category, search, pageable);
        } else if (hasCategory) {
            page = roomRepository.findByIsPrivateFalseAndCategory(category, pageable);
        } else if (hasSearch) {
            page = roomRepository.findByIsPrivateFalseAndNameContainingIgnoreCase(search, pageable);
        } else {
            page = roomRepository.findByIsPrivateFalse(pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getById(UUID roomId) {
        Room room = findRoom(roomId);
        return toResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse join(UUID roomId, UUID userId, JoinRoomRequest request) {
        Room room = findRoomWithMembers(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (room.getMembers().contains(user)) {
            // Already a member — just return current state
            return toResponse(room);
        }

        if (room.getMemberCount() >= room.getMaxUsers()) {
            throw new ConflictException("Room is full (" + room.getMaxUsers() + " max users)");
        }

        if (room.isPrivate()) {
            String provided = request != null ? request.getPassword() : null;
            if (provided == null || room.getPasswordHash() == null
                    || !passwordEncoder.matches(provided, room.getPasswordHash())) {
                throw new ConflictException("Invalid room password");
            }
        }

        room.getMembers().add(user);
        room = roomRepository.save(room);
        log.info("User '{}' joined room '{}'", user.getUsername(), room.getName());
        return toResponse(room);
    }

    @Override
    @Transactional
    public void leave(UUID roomId, UUID userId) {
        Room room = findRoomWithMembers(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        room.getMembers().remove(user);
        roomRepository.save(room);
        log.info("User '{}' left room '{}'", user.getUsername(), room.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublic() {
        return roomRepository.countByIsPrivateFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Room findRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }

    /** Load room WITH its members collection for mutation operations. */
    private Room findRoomWithMembers(UUID roomId) {
        // Re-use findById which loads lazy; trigger init inside transaction
        Room room = findRoom(roomId);
        room.getMembers().size(); // initialize lazy collection inside active TX
        return room;
    }

    private RoomResponse toResponse(Room room) {
        long memberCount = roomRepository.countMembersById(room.getId());
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .category(room.getCategory())
                .maxUsers(room.getMaxUsers())
                .memberCount((int) memberCount)
                .isPrivate(room.isPrivate())
                .tags(room.getTags())
                .creatorId(room.getCreator().getId())
                .creatorUsername(room.getCreator().getUsername())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
