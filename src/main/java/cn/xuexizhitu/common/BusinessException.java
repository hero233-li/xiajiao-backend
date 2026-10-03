package cn.xuexizhitu.common;
public class BusinessException extends RuntimeException {
    private final ErrorCode error;
    public BusinessException(ErrorCode error) {
        super(error.message());
        this.error=error;
    }
    public BusinessException(ErrorCode error, String message) {
        super(message);
        this.error=error;
    }
    public ErrorCode error() {
        return error;
    }
}
