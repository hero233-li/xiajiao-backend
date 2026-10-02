package cn.xuexizhitu.common;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> business(BusinessException e) { return response(e.error(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        String message=e.getBindingResult().getFieldErrors().stream().findFirst()
            .map(x -> x.getField()+"："+x.getDefaultMessage()).orElse("请求参数不合法");
        return response(ErrorCode.INVALID_PARAMETER, message);
    }
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
        MissingServletRequestParameterException.class, org.springframework.web.bind.MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiResponse<Void>> invalid(Exception e) { return response(ErrorCode.INVALID_PARAMETER); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> denied() { return response(ErrorCode.FORBIDDEN); }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiResponse<Void>> notFound() { return response(ErrorCode.NOT_FOUND); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> methodNotAllowed() { return ResponseEntity.status(405).body(ApiResponse.failure(ErrorCode.INVALID_PARAMETER, "不支持的请求方法")); }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> media() { return response(ErrorCode.UNSUPPORTED_MEDIA); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> tooLarge() { return response(ErrorCode.PAYLOAD_TOO_LARGE); }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> relation() {return response(ErrorCode.INVALID_RELATION,"数据关联、唯一标识或字段精度不符合业务约束");}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        // 不记录正文、密码、令牌或可能包含SQL参数的异常消息。
        log.error("未处理异常类型：{}", e.getClass().getName());
        return response(ErrorCode.INTERNAL_ERROR);
    }
    private ResponseEntity<ApiResponse<Void>> response(ErrorCode error) { return response(error,error.message()); }
    private ResponseEntity<ApiResponse<Void>> response(ErrorCode error, String message) {
        return ResponseEntity.status(error.status()).body(ApiResponse.failure(error,message));
    }
}
