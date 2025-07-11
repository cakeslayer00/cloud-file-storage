package com.vladsv.cloud_file_storage.exception.handler;

import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.MultipleErrorResponseDto;
import com.vladsv.cloud_file_storage.exception.*;
import org.springframework.http.HttpStatus;
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
    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ErrorResponseDto handleUserAlreadyExists(UsernameAlreadyTakenException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UserNotFoundException.class)
    public ErrorResponseDto handleUserDoesNotExistsException(UserNotFoundException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidPasswordException.class)
    public ErrorResponseDto handleBadCredentials(InvalidPasswordException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponseDto handleUnknownException(Exception e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ErrorResponseDto handleFolderAlreadyExists(ResourceAlreadyExistsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceDoesNotExistsException.class)
    public ErrorResponseDto handleResourceDoesNotExists(ResourceDoesNotExistsException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidResourcePathException.class)
    public ErrorResponseDto handleAmbiguousPathException(InvalidResourcePathException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidResourceUploadBodyException.class)
    public ErrorResponseDto handleInvalidUploadBodyException(InvalidResourceUploadBodyException e) {
        return new ErrorResponseDto(e.getMessage());
    }

    private String mapToMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
