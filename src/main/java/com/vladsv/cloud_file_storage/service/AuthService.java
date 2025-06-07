package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.exception.UserAlreadyExistsException;
import com.vladsv.cloud_file_storage.mapper.UserMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import com.vladsv.cloud_file_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String USER_ALREADY_EXISTS = "Username '%s' already exists";
    private static final String USERNAME_NOT_FOUND = "Username not found";
    private static final String USER_FOLDER_FORMAT = "user-%s-files/.init";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final MinioRepository minioRepository;

    public UserResponseDto register(UserRequestDto userRequestDto) {
        if (userRepository.existsByUsername(userRequestDto.username())) {
            String message = String.format(USER_ALREADY_EXISTS, userRequestDto.username());
            throw new UserAlreadyExistsException(message);
        }

        User user = UserMapper.INSTANCE.toEntity(userRequestDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.saveAndFlush(user);

        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            minioRepository.commenceDirectory(String.format(USER_FOLDER_FORMAT, user.getId()));
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

}
