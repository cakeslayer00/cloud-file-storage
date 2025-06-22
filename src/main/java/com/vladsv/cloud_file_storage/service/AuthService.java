package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.exception.InvalidPasswordException;
import com.vladsv.cloud_file_storage.exception.UsernameAlreadyTakenException;
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

import static utils.PathUtils.USER_ROOT_DIR_PATTERN;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String USERNAME_ALREADY_TAKEN = "Username '%s' is already taken";
    private static final String USER_NOT_FOUND = "Username not found, try again!";
    private static final String INVALID_PASSWORD = "Invalid password, try again!";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    private final PasswordEncoder passwordEncoder;

    private final DirectoryService minioRepository;

    private final UserRepository userRepository;

    @Transactional
    public UserResponseDto register(UserRequestDto userRequestDto,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        boolean isUsernameTaken = userRepository.existsByUsername(userRequestDto.username());
        if (isUsernameTaken) {
            throw new UsernameAlreadyTakenException(
                    USERNAME_ALREADY_TAKEN.formatted(userRequestDto.username()));
        }

        User user = UserMapper.INSTANCE.toEntity(userRequestDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.saveAndFlush(user);

        commenceSecurityContextExplicitly(userRequestDto, request, response);
        minioRepository.createRootDirectory(USER_ROOT_DIR_PATTERN.formatted(user.getId()));

        return UserMapper.INSTANCE.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto authenticate(UserRequestDto userRequestDto,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = userRepository.findByUsername(userRequestDto.username())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(userRequestDto.password(), user.getPassword())) {
            throw new InvalidPasswordException(INVALID_PASSWORD);
        }

        commenceSecurityContextExplicitly(userRequestDto, request, response);
        return UserMapper.INSTANCE.toDto(user);
    }

    private void commenceSecurityContextExplicitly(UserRequestDto userRequestDto,
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
