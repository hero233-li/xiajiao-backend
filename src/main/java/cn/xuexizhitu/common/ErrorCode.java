package cn.xuexizhitu.common;
import org.springframework.http.HttpStatus;
public enum ErrorCode {
    INVALID_PARAMETER(40001, HttpStatus.BAD_REQUEST, "请求参数不合法"),     UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "请先登录或令牌已失效"),     BAD_CREDENTIALS(40102, HttpStatus.UNAUTHORIZED, "用户名、邮箱或密码不正确"),     ACCOUNT_DISABLED(40103, HttpStatus.UNAUTHORIZED, "账号已停用"),     FORBIDDEN(40301, HttpStatus.FORBIDDEN, "没有操作权限"),     EXAMS_LOCKED(40302, HttpStatus.FORBIDDEN, "真题或成绩录入尚未解锁"),     INVALID_DATE(42203, HttpStatus.UNPROCESSABLE_ENTITY, "日期未知或不在允许窗口"),     PLAN_GAP(42204, HttpStatus.UNPROCESSABLE_ENTITY, "计划无法排完"),     ASSESSMENT_BLOCKED(40303, HttpStatus.FORBIDDEN, "检测申请条件未满足"),     ANSWERS_LOCKED(40304, HttpStatus.FORBIDDEN, "交卷前不能查看答案"),     BANK_INSUFFICIENT(42201, HttpStatus.UNPROCESSABLE_ENTITY, "题量或考点覆盖不足，已记录告警"),     NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),     REVISION_CONFLICT(40901, HttpStatus.CONFLICT, "资源修订号冲突，请重新读取"),     IDEMPOTENCY_CONFLICT(40904, HttpStatus.CONFLICT, "幂等键已用于不同的请求"),     INVALID_RELATION(42202, HttpStatus.UNPROCESSABLE_ENTITY, "资源关联或业务参数不合法"),     CONFLICT(40902, HttpStatus.CONFLICT, "资源状态冲突"),     PAYLOAD_TOO_LARGE(41301, HttpStatus.PAYLOAD_TOO_LARGE, "请求内容过大"),     UNSUPPORTED_MEDIA(41501, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的请求格式"),     INTERNAL_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用");
    private final int code;
    private final HttpStatus status;
    private final String message;
    ErrorCode(int code, HttpStatus status, String message) {
        this.code=code;
        this.status=status;
        this.message=message;
    }
    public int code() {
        return code;
    }
    public HttpStatus status() {
        return status;
    }
    public String message() {
        return message;
    }
}
