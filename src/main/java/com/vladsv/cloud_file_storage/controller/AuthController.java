package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.docs.auth.AuthenticationSwaggerDoc;
import com.vladsv.cloud_file_storage.docs.auth.RegistrationSwaggerDoc;
import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.service.AuthService;
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

    @RegistrationSwaggerDoc
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sign-up")
    public UserResponseDto signUp(@RequestBody @Valid UserRequestDto userRequestDto,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        return authService.register(userRequestDto, request, response);
    }

    @AuthenticationSwaggerDoc
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/sign-in")
    public UserResponseDto signIn(@RequestBody @Valid UserRequestDto userRequestDto,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        return authService.authenticate(userRequestDto, request, response);
    }

}
