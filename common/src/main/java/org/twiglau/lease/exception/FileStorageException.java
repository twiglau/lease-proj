package org.twiglau.lease.exception;

/**
 * 文件存储异常，用于统一包装 MinIO SDK 抛出的受检异常
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
