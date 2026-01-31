package com.kramp.productinfo.controller;

import com.kramp.productinfo.domain.ports.exception.UpstreamFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProductInfoController.class)
public class ProductInfoControllerAdvice {

    @ExceptionHandler(UpstreamFailureException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamFailure(UpstreamFailureException ex) {

        if ("catalog".equalsIgnoreCase(ex.service())) {

            if ("PRODUCT_NOT_FOUND".equalsIgnoreCase(ex.reason())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.of("PRODUCT_NOT_FOUND", ex.getMessage()));
            }

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ErrorResponse.of("CATALOG_UNAVAILABLE", ex.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", ex.getMessage()));
    }
}
