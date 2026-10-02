package com.show.reservation.common;


public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
	public final int status; public final String code;
    public ApiException(int status, String code, String msg) { super(msg); this.status = status; this.code = code; }
}

