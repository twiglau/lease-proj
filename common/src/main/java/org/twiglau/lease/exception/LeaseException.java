package org.twiglau.lease.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.twiglau.lease.result.ResultCodeEnum;

@EqualsAndHashCode(callSuper = true)
@Data
public class LeaseException extends RuntimeException {
    private  Integer code;

    public LeaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public LeaseException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
