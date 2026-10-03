package cn.xuexizhitu.operations.infrastructure;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
@Repository @RequiredArgsConstructor public class BackgroundFailures {
    private final JdbcTemplate jdbc;
    @Transactional(propagation=Propagation.REQUIRES_NEW)     public void failed(String kind,String id,Throwable error) {
        jdbc.update("INSERT INTO background_failure(task_kind,target_id,attempts,last_error,next_attempt_at,updated_at) VALUES(?,?,1,?,DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 30 SECOND),UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE attempts=attempts+1,last_error=VALUES(last_error),next_attempt_at=TIMESTAMPADD(SECOND,LEAST(3600,30*POW(2,LEAST(attempts,6))),UTC_TIMESTAMP(6)),updated_at=UTC_TIMESTAMP(6)",kind,id,error.getClass().getSimpleName());
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)     public void recovered(String kind,String id) {
        jdbc.update("DELETE FROM background_failure WHERE task_kind=? AND target_id=?",kind,id);
    }
}
