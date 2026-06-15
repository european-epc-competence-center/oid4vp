package de.eecc.oid4vc.oid4vp.spring;

import de.eecc.oid4vc.oid4vp.exception.Oid4VpException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class Oid4VpExceptionHandler {

    @ExceptionHandler(Oid4VpException.class)
    ResponseEntity<Map<String, String>> handle(Oid4VpException ex) {
        return ResponseEntity.status(ex.error().suggestedHttpStatus())
                .body(Map.of("message", ex.getMessage()));
    }
}
