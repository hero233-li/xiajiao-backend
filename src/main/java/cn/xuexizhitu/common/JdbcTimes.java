package cn.xuexizhitu.common;
import java.time.*;
import java.util.Map;
public final class JdbcTimes {
    private JdbcTimes() {
    }
    public static Instant instant(Map<String,Object> row,String key) {
        Object v=row.get(key);
        return v==null?null:((LocalDateTime)v).toInstant(ZoneOffset.UTC);
    }
}
