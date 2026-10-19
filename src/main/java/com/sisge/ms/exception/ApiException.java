package com.sisge.ms.exception;
import java.util.List;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final List<?> details;
  public ApiException(HttpStatus status, String message) { this(status,message,List.of()); }
  public ApiException(HttpStatus status, String message, List<?> details) { super(message); this.status=status; this.details=List.copyOf(details); }
  public HttpStatus status(){ return status; }
  public List<?> details(){ return details; }
  public static ApiException bad(String m){return new ApiException(HttpStatus.BAD_REQUEST,m);}
  public static ApiException forbidden(String m){return new ApiException(HttpStatus.FORBIDDEN,m);}
  public static ApiException conflict(String m){return new ApiException(HttpStatus.CONFLICT,m);}
  public static ApiException conflict(String m,List<?> details){return new ApiException(HttpStatus.CONFLICT,m,details);}
  public static ApiException notFound(String m){return new ApiException(HttpStatus.NOT_FOUND,m);}
}
