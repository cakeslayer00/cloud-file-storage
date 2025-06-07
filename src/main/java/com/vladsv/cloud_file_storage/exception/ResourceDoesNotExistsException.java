package com.vladsv.cloud_file_storage.exception;

public class ResourceDoesNotExistsException extends RuntimeException {
    public ResourceDoesNotExistsException(String message) {
        super(message);
    }
}
