package com.sisge.ms.exception;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){return response(e.status(),e.getMessage(),List.of());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){
  var errors=e.getBindingResult().getFieldErrors().stream().map(x->Map.of("field",x.getField(),"message",Objects.toString(x.getDefaultMessage(),"invalid"))).toList();
  return response(HttpStatus.BAD_REQUEST,"Validation failed",errors);
 }
 @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e){return response(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error",List.of());}
 private ResponseEntity<?> response(HttpStatus s,String m,List<?> details){return ResponseEntity.status(s).body(Map.of("timestamp",Instant.now(),"status",s.value(),"message",m,"details",details));}
}
