package com.vladsv.cloud_file_storage.exception;

public class DirectoryDoesNotExistsException extends RuntimeException {
    public DirectoryDoesNotExistsException(String message) {
        super(message);
    }
}
