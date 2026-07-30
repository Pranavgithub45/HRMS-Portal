package com.billdesk.hrmsportal.exception;

public class ForbiddenException extends RuntimeException{
    public ForbiddenException(String msg){
        super(msg);
    }
}
