package com.vladsv.cloud_file_storage.exception;

public class DirectoryAlreadyExistsException extends RuntimeException {
    public DirectoryAlreadyExistsException(String message) {
        super(message);
    }
}
