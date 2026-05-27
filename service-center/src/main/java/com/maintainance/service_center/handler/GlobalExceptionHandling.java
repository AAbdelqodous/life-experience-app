package com.maintainance.service_center.handler;

import com.maintainance.service_center.auth.AccountRejectedException;
import com.maintainance.service_center.booking.ClaimException;
import com.maintainance.service_center.department.DepartmentOperationException;
import com.maintainance.service_center.staff.StaffOperationException;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashSet;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandling {

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ExceptionResponse> handleException(LockedException exp){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.ACCOUNT_LOCKED.getCode())
                                .businessErrorDescription(BusinessErrorCodes.ACCOUNT_LOCKED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ExceptionResponse> handleException(DisabledException exp){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.ACCOUNT_DISABLED.getCode())
                                .businessErrorDescription(BusinessErrorCodes.ACCOUNT_DISABLED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleException(BadCredentialsException exp){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.BAD_CREDENTIALS.getCode())
                                .businessErrorDescription(BusinessErrorCodes.BAD_CREDENTIALS.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ExceptionResponse> handleException(MessagingException exp){
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleException(MethodArgumentNotValidException exp){
        Set<String> errors = new HashSet<>();
        exp.getBindingResult().getAllErrors()
                .forEach( error -> {
                    var errorMessage =  error.getDefaultMessage();
                    errors.add(errorMessage);
                });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .validationErrors(errors)
                                .build()
                );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleException(EntityNotFoundException exp){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleException(IllegalArgumentException exp){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionResponse> handleException(IllegalStateException exp){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleException(AccessDeniedException exp){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(StaffOperationException.class)
    public ResponseEntity<ExceptionResponse> handleException(StaffOperationException exp){
        return ResponseEntity
                .status(exp.getErrorCode().getHttpStatus())
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(exp.getErrorCode().getCode())
                                .businessErrorDescription(exp.getErrorCode().getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(DepartmentOperationException.class)
    public ResponseEntity<ExceptionResponse> handleException(DepartmentOperationException exp){
        return ResponseEntity
                .status(exp.getErrorCode().getHttpStatus())
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(exp.getErrorCode().getCode())
                                .businessErrorDescription(exp.getErrorCode().getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    // Spec 021 — self-claim. The frontend reads err.data.error for the wire code,
    // so set `error` to the ClaimErrorCode enum name (e.g. "BOOKING_ALREADY_CLAIMED").
    @ExceptionHandler(ClaimException.class)
    public ResponseEntity<ExceptionResponse> handleException(ClaimException exp){
        var be = exp.getCode().getBusinessErrorCode();
        return ResponseEntity
                .status(be.getHttpStatus())
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(be.getCode())
                                .businessErrorDescription(be.getDescription())
                                .error(exp.getCode().name())
                                .build()
                );
    }

    @ExceptionHandler(AccountRejectedException.class)
    public ResponseEntity<ExceptionResponse> handleException(AccountRejectedException exp){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.ACCOUNT_REJECTED.getCode())
                                .businessErrorDescription(BusinessErrorCodes.ACCOUNT_REJECTED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception exp){
        exp.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorDescription("Internal error. plz contact the admin")
                                .error(exp.getMessage())
                                .build()
                );
    }
}
