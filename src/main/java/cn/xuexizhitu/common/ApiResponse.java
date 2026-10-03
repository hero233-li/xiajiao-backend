package cn.xuexizhitu.common;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "统一响应；失败data固定为null") public record ApiResponse<T>(int code, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, data, "ok");
    }
    public static ApiResponse<Void> failure(ErrorCode error) {
        return failure(error, error.message());
    }
    public static ApiResponse<Void> failure(ErrorCode error, String message) {
        return new ApiResponse<>(error.code(), null, message);
    }
}
