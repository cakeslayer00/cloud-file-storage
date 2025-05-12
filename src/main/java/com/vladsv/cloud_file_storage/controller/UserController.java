package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    public UserResponseDto getAuthenticatedUser(Authentication authentication) {
        return new UserResponseDto(authentication.getName());
    }

}

