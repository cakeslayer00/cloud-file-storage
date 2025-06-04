package com.vladsv.cloud_file_storage.exception;

public class DirectoryDoesNotExistException extends RuntimeException {
    public DirectoryDoesNotExistException(String message) {
        super(message);
    }
}
