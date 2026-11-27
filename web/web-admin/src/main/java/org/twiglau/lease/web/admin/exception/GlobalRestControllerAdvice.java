package org.twiglau.lease.web.admin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.twiglau.lease.exception.LeaseException;
import org.twiglau.lease.result.Result;

@RestControllerAdvice
@Slf4j
public class GlobalRestControllerAdvice {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception exception) {
        log.error("系统异常:{}", exception.getMessage(), exception);
        return Result.fail();
    }

    @ExceptionHandler(LeaseException.class)
    public Result<String> handleException(LeaseException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }
}
