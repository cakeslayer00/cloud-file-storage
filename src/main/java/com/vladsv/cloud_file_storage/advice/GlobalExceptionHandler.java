package com.vladsv.cloud_file_storage.advice;

import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.MultipleErrorResponseDto;
import com.vladsv.cloud_file_storage.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MultipleErrorResponseDto handleNonValidInput(MethodArgumentNotValidException e) {
        return new MultipleErrorResponseDto(e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapToMessage)
                .toList());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ErrorResponseDto handleUserAlreadyExists(UserAlreadyExistsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UserNotFoundException.class)
    public ErrorResponseDto handleUserDoesNotExistsException(UserNotFoundException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    public ErrorResponseDto handleBadCredentials(BadCredentialsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponseDto handleUnknownException(Exception e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DirectoryAlreadyExistsException.class)
    public ErrorResponseDto handleFolderAlreadyExists(DirectoryAlreadyExistsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceDoesNotExistsException.class)
    public ErrorResponseDto handleResourceDoesNotExists(ResourceDoesNotExistsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(DirectoryDoesNotExistsException.class)
    public ErrorResponseDto handleDirectoryDoesNotExists(DirectoryDoesNotExistsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictingResourceException.class)
    public ErrorResponseDto handleResourceAlreadyExists(ConflictingResourceException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDirectoryPathException.class)
    public ErrorResponseDto handleAmbiguousPathException(InvalidDirectoryPathException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidResourcePathException.class)
    public ErrorResponseDto handleAmbiguousPathException(InvalidResourcePathException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    private String mapToMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
