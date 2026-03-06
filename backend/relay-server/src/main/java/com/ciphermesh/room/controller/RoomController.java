package com.ciphermesh.room.controller;

import com.ciphermesh.auth.jwt.JwtService;
import com.ciphermesh.common.response.ApiResponse;
import com.ciphermesh.room.dto.CreateRoomRequest;
import com.ciphermesh.room.dto.JoinRoomRequest;
import com.ciphermesh.room.dto.RoomResponse;
import com.ciphermesh.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final JwtService jwtService;

    /** GET /api/v1/rooms?category=general&search=crypto&page=0&size=20 */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> listRooms(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<RoomResponse> result = roomService.listPublic(
                category, search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** GET /api/v1/rooms/count — used by landing page */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countPublic() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", roomService.countPublic())));
    }

    /** GET /api/v1/rooms/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getById(id)));
    }

    /** POST /api/v1/rooms  — requires JWT */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @RequestHeader("Authorization") String authHeader) {

        UUID creatorId = extractUserId(authHeader);
        RoomResponse room = roomService.create(request, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Room created", room));
    }

    /** POST /api/v1/rooms/{id}/join  — requires JWT */
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @PathVariable UUID id,
            @RequestBody(required = false) JoinRoomRequest request,
            @RequestHeader("Authorization") String authHeader) {

        UUID userId = extractUserId(authHeader);
        RoomResponse room = roomService.join(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Joined room", room));
    }

    /** DELETE /api/v1/rooms/{id}/leave  — requires JWT */
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {

        UUID userId = extractUserId(authHeader);
        roomService.leave(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Left room", null));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
