package com.ciphermesh.room.repository;

import com.ciphermesh.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByName(String name);

    boolean existsByName(String name);

    /** Public rooms — no filter. */
    @Query(value = "SELECT r FROM Room r JOIN FETCH r.creator WHERE r.isPrivate = false",
           countQuery = "SELECT COUNT(r) FROM Room r WHERE r.isPrivate = false")
    Page<Room> findByIsPrivateFalse(Pageable pageable);

    /** Public rooms filtered by category. */
    @Query(value = "SELECT r FROM Room r JOIN FETCH r.creator WHERE r.isPrivate = false AND r.category = :category",
           countQuery = "SELECT COUNT(r) FROM Room r WHERE r.isPrivate = false AND r.category = :category")
    Page<Room> findByIsPrivateFalseAndCategory(String category, Pageable pageable);

    /** Public rooms filtered by name (case-insensitive contains). */
    @Query(value = "SELECT r FROM Room r JOIN FETCH r.creator WHERE r.isPrivate = false AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))",
           countQuery = "SELECT COUNT(r) FROM Room r WHERE r.isPrivate = false AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Room> findByIsPrivateFalseAndNameContainingIgnoreCase(String name, Pageable pageable);

    /** Public rooms filtered by category AND name. */
    @Query(value = "SELECT r FROM Room r JOIN FETCH r.creator WHERE r.isPrivate = false AND r.category = :category AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))",
           countQuery = "SELECT COUNT(r) FROM Room r WHERE r.isPrivate = false AND r.category = :category AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Room> findByIsPrivateFalseAndCategoryAndNameContainingIgnoreCase(
            String category, String name, Pageable pageable);

    /** Member count for a single room (avoids loading the full collection). */
    @Query("SELECT COUNT(m) FROM Room r JOIN r.members m WHERE r.id = :roomId")
    long countMembersById(UUID roomId);

    /** Count of all public rooms. */
    long countByIsPrivateFalse();
}
