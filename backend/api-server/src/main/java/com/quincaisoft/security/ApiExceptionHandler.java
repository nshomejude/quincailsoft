package com.quincaisoft.security;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import com.quincaisoft.shared.RevisionConflict;
import java.util.NoSuchElementException;
import jakarta.servlet.http.HttpServletRequest;import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingRequestHeaderException.class}) ResponseEntity<Map<String,Object>> malformed(HttpServletRequest request){return ResponseEntity.badRequest().body(ApiErrors.body(request,"MALFORMED_REQUEST","Check the request body and required headers",false));}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String,Object>> denied(HttpServletRequest request){return ResponseEntity.status(403).body(ApiErrors.body(request,"ACCESS_DENIED","This operation is not permitted in your current scope",false));}
 @ExceptionHandler({IllegalArgumentException.class,ArithmeticException.class}) ResponseEntity<Map<String,Object>> invalid(HttpServletRequest request){return ResponseEntity.status(422).body(ApiErrors.body(request,"VALIDATION_FAILED","The command contains invalid or unrepresentable values",false));}
 @ExceptionHandler({OptimisticLockException.class,OptimisticLockingFailureException.class,RevisionConflict.class}) ResponseEntity<Map<String,Object>> stale(HttpServletRequest request){return ResponseEntity.status(412).body(ApiErrors.body(request,"VERSION_MISMATCH","Refresh the record before retrying",false));}
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Map<String,Object>> missing(HttpServletRequest request){return ResponseEntity.status(404).body(ApiErrors.body(request,"NOT_FOUND","Record not found in this scope",false));}
 @ExceptionHandler(IllegalStateException.class) ResponseEntity<Map<String,Object>> conflict(HttpServletRequest request){return ResponseEntity.status(409).body(ApiErrors.body(request,"STATE_CONFLICT","The operation is not valid in the current state",false));}
 @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> unexpected(HttpServletRequest request){return ResponseEntity.status(500).body(ApiErrors.body(request,"INTERNAL_ERROR","The request could not be completed",false));}
}
