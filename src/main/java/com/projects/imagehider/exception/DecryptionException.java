package com.projects.imagehider.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DecryptionException extends RuntimeException
{
    public DecryptionException(String message)
    {
        super(message);
    }

    public DecryptionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
