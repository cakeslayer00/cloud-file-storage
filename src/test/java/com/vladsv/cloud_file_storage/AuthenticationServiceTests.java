package com.vladsv.cloud_file_storage;

import com.vladsv.cloud_file_storage.dto.UserRequestDto;
import com.vladsv.cloud_file_storage.exception.UsernameAlreadyTakenException;
import com.vladsv.cloud_file_storage.repository.UserRepository;
import com.vladsv.cloud_file_storage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Testcontainers
public class AuthenticationServiceTests implements Containers {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @BeforeAll
    static void containerStart() {
        postgres.start();
        minio.start();
    }

    @AfterAll
    static void containerShutdown() {
        postgres.stop();
        minio.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.minio.endpoint", minio::getS3URL);
        registry.add("spring.minio.access_key", minio::getUserName);
        registry.add("spring.minio.secret_key", minio::getPassword);
    }

    @Test
    public void givenAuthService_whenRegistrationMethodInvoked_thenNewUserRecordAppearsInDatabase() {
        UserRequestDto mock = new UserRequestDto("J.R.R Tolkien", "password");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        authService.register(mock, request, response);

        boolean condition = userRepository.existsByUsername(mock.username());
        assertTrue(condition);
    }

    @Test
    public void givenAuthService_whenRegistrationMethodInvokedWithExistingUser_thenThrowAppropriateException() {
        UserRequestDto mock = new UserRequestDto("J.R.R Tolkien", "password");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        authService.register(mock, request, response);

        assertThrows(UsernameAlreadyTakenException.class,() -> authService.register(mock, request, response));
    }

}
