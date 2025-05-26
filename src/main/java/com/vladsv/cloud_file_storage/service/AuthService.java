package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.exception.UserAlreadyExistsException;
import com.vladsv.cloud_file_storage.mapper.UserMapper;
import com.vladsv.cloud_file_storage.repository.UserRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String USER_ALREADY_EXISTS = "Username '%s' already exists";
    private static final String USERNAME_NOT_FOUND = "Username not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final MinioClient minioClient;

    public UserResponseDto addUser(UserRequestDto userRequestDto) {
        if (userRepository.existsByUsername(userRequestDto.username())) {
            String message = String.format(USER_ALREADY_EXISTS, userRequestDto.username());
            throw new UserAlreadyExistsException(message);
        }

        User user = UserMapper.INSTANCE.toEntity(userRequestDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.saveAndFlush(user);

        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            createUserSpecificFolder(user.getId());
        }

        return UserMapper.INSTANCE.toDto(user);
    }

    public UserResponseDto authenticate(UserRequestDto userRequestDto) {
        User user = userRepository.findByUsername(userRequestDto.username())
                .orElseThrow(() -> new UsernameNotFoundException(USERNAME_NOT_FOUND));

        if (!passwordEncoder.matches(userRequestDto.password(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect password or username");
        }

        return UserMapper.INSTANCE.toDto(user);
    }

    private void createUserSpecificFolder(Long userId) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket("user-files").object(String.format("user-%s-files/.init", userId)).stream(
                                    new ByteArrayInputStream(new byte[] {}), 0, -1)
                            .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }
}
