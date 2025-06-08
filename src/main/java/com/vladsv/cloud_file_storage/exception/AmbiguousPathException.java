package com.vladsv.cloud_file_storage.exception;

public class AmbiguousPathException extends RuntimeException {
    public AmbiguousPathException(String message) {
        super(message);
    }
}
