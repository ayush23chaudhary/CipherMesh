package com.ciphermesh.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateRoomRequest {

    @NotBlank
    @Size(min = 3, max = 80)
    private String name;

    @Size(max = 300)
    private String description;

    @NotBlank
    @Size(max = 40)
    private String category = "general";

    @Min(2)
    @Max(500)
    private int maxUsers = 50;

    private boolean isPrivate = false;

    /** Plain-text password — only relevant when isPrivate = true. */
    private String password;

    @Size(max = 10)
    private Set<String> tags;
}
