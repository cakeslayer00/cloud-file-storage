package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponseDto> signUp(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(userRequestDto, request, response));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserResponseDto> signIn(@RequestBody @Valid UserRequestDto userRequestDto,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        return ResponseEntity.ok(authService.authenticate(userRequestDto, request, response));
    }

}
