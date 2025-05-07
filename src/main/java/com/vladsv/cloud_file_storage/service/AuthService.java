package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.mapper.UserMapper;
import com.vladsv.cloud_file_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public UserResponseDto addUser(UserRequestDto userRequestDto) {
        User user = userRepository
                .saveAndFlush(UserMapper.INSTANCE.toEntity(userRequestDto));

        return UserMapper.INSTANCE.toDto(user);
    }

}
