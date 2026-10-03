package cn.xuexizhitu.exams.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.contract.WorkflowDtos.PaperWrite;
import cn.xuexizhitu.exams.domain.ExamTypes.Paper;
import cn.xuexizhitu.exams.infrastructure.ExamRepository;
import cn.xuexizhitu.files.application.PrivateFileStore;
import cn.xuexizhitu.learning.infrastructure.CurriculumRepository;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor public class PaperAdminService {
    private final ExamRepository repository;
    private final CurriculumRepository curriculum;
    private final ExamService exams;
    private final PrivateFileStore files;
    private final AuditRepository audit;
    @Transactional public Paper paper(String course,String id,PaperWrite w) {
        CurrentUser.requireAdmin();
        curriculum.lockCourse(course);
        require(!w.paperMonth().startsWith("0000"),ErrorCode.INVALID_RELATION,"试卷月份无效");
        List<UUID> ids=new ArrayList<>();
        ids.add(w.questionFileId());
        if(w.answerFileId()!=null)ids.add(w.answerFileId());
        for(UUID file:ids) {
            var f=files.metadata(file.toString());
            require(f.purpose().equals("PAPER")&&f.state().equals("ACTIVE"),ErrorCode.INVALID_RELATION,"试卷文件用途或状态无效");
        }
        require(!repository.paperMonthExists(course,w.paperMonth(),id),ErrorCode.CONFLICT,"课程与月份已有试卷");
        boolean create=id==null;
        if(create)id=uuid();
        else repository.paper(course,id);
        repository.writePaper(course,id,w,create);
        audit.add("PAPER_UPDATED","PAPER",id,null,obj("courseId",course,"paperMonth",w.paperMonth()));
        return exams.paper(course,id);
    }
}
