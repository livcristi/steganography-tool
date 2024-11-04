package com.projects.imagehider.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ImageException extends RuntimeException
{
    public ImageException(String message)
    {
        super(message);
    }

    public ImageException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
