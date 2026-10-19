package com.sisge.ms.exception;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
@RestControllerAdvice public class ApiExceptionHandler {
 private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){return response(e.status(),e.getMessage(),e.details());}
 @ExceptionHandler(MissingServletRequestPartException.class) ResponseEntity<?> missingPart(MissingServletRequestPartException e){
  return response(HttpStatus.BAD_REQUEST,"Multipart field '"+e.getRequestPartName()+"' is required",List.of());
 }
 @ExceptionHandler(HttpMediaTypeNotSupportedException.class) ResponseEntity<?> unsupportedMediaType(HttpMediaTypeNotSupportedException e){
  return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Content-Type must be multipart/form-data",List.of());
 }
 @ExceptionHandler(MultipartException.class) ResponseEntity<?> malformedMultipart(MultipartException e){
  return response(HttpStatus.BAD_REQUEST,"Malformed multipart request",List.of());
 }
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){
  var errors=e.getBindingResult().getFieldErrors().stream().map(x->Map.of("field",x.getField(),"message",Objects.toString(x.getDefaultMessage(),"invalid"))).toList();
  return response(HttpStatus.BAD_REQUEST,"Validation failed",errors);
 }
 @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e){log.error("Unhandled API exception",e);return response(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error",List.of());}
 private ResponseEntity<?> response(HttpStatus s,String m,List<?> details){return ResponseEntity.status(s).body(Map.of("timestamp",Instant.now(),"status",s.value(),"message",m,"details",details));}
}
