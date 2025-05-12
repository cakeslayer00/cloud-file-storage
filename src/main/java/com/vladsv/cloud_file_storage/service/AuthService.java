package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.mapper.UserMapper;
import com.vladsv.cloud_file_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto addUser(UserRequestDto userRequestDto) {
        User user = UserMapper.INSTANCE.toEntity(userRequestDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.saveAndFlush(user);

        return UserMapper.INSTANCE.toDto(user);
    }

    public UserResponseDto authenticate(UserRequestDto userRequestDto) {
        User user = userRepository.findByUsername(userRequestDto.username())
                .orElseThrow(() -> new UsernameNotFoundException(userRequestDto.username()));

        return UserMapper.INSTANCE.toDto(user);
    }
}
