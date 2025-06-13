package com.vladsv.cloud_file_storage.exception;

public class ConflictingResourceException extends RuntimeException {
    public ConflictingResourceException(String message) {
        super(message);
    }
}
