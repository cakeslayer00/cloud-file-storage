package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.exception.InvalidPasswordException;
import com.vladsv.cloud_file_storage.exception.UserAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.UserNotFoundException;
import com.vladsv.cloud_file_storage.mapper.UserMapper;
import com.vladsv.cloud_file_storage.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String USER_ALREADY_EXISTS = "Username taken! Choose a different username";
    private static final String USER_NOT_FOUND = "Username not found, try again!";
    private static final String INVALID_PASSWORD = "Invalid password, try again!";
    private static final String USER_ROOT_DIRECTORY_FORMAT = "user-%s-files/";

    private final SecurityContextRepository securityContextRepository;

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DirectoryService minioRepository;

    public UserResponseDto register(UserRequestDto userRequestDto,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (userRepository.existsByUsername(userRequestDto.username())) {
            throw new UserAlreadyExistsException(USER_ALREADY_EXISTS);
        }

        User user = UserMapper.INSTANCE.toEntity(userRequestDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.saveAndFlush(user);

        setupSecurityContextExplicitly(userRequestDto, request, response);
        minioRepository.createRootDirectory(USER_ROOT_DIRECTORY_FORMAT.formatted(user.getId()));

        return UserMapper.INSTANCE.toDto(user);
    }

    public UserResponseDto authenticate(UserRequestDto userRequestDto,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = userRepository.findByUsername(userRequestDto.username())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(userRequestDto.password(), user.getPassword())) {
            throw new InvalidPasswordException(INVALID_PASSWORD);
        }

        setupSecurityContextExplicitly(userRequestDto, request, response);
        return UserMapper.INSTANCE.toDto(user);
    }

    private void setupSecurityContextExplicitly(UserRequestDto userRequestDto,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
                userRequestDto.username(), userRequestDto.password());
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

}
