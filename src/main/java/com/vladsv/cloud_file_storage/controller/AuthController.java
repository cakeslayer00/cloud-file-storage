package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            tags = {"Authorization"},
            summary = "Register new user",
            description = "Registers a new user and returns basic user information",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User created",
                            content = @Content(schema = @Schema(implementation = UserResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Username already exists",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    )
            }
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sign-up")
    public UserResponseDto signUp(@RequestBody @Valid UserRequestDto userRequestDto,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        return authService.register(userRequestDto, request, response);
    }

    @Operation(
            tags = {"Authorization"},
            summary = "Authenticate user",
            description = "Authenticates existing user and returns basic user information",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User authenticated",
                            content = @Content(schema = @Schema(implementation = UserResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid input data",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    )
            }
    )
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/sign-in")
    public UserResponseDto signIn(@RequestBody @Valid UserRequestDto userRequestDto,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        return authService.authenticate(userRequestDto, request, response);
    }

}
