package cn.xuexizhitu.learning.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import cn.xuexizhitu.learning.api.LearningDtos.Course;
import cn.xuexizhitu.learning.api.LearningDtos.ExamCycle;
import cn.xuexizhitu.learning.infrastructure.*;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor public class CurriculumAdminService {
    private final CurriculumRepository repository;
    private final LearningRepository learning;
    private final LearningService courses;
    private final AuditRepository audit;
    @Transactional public Course course(String id,CourseAdminWrite w,String cycle) {
        CurrentUser.requireAdmin();
        learning.requireCycle(cycle);
        if(id==null) {
            id=uuid();
            repository.createCourse(id,cycle,w);
        }
        else {
            repository.lockCourse(id);
            repository.updateCourse(id,w);
        }
        audit.add("COURSE_UPDATED","COURSE",id,null,obj("code",w.code()));
        return courses.historicalCourse(id,cycle);
    }
    private LocalTime time(String value) {
        try {
            return LocalTime.parse(value);
        }
        catch(DateTimeException e) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,"考试时段格式无效");
        }
    }
    @Transactional public ExamCycle cycle(String id,CycleWrite w) {
        CurrentUser.requireAdmin();
        require(!w.endDate().isBefore(w.startDate()),ErrorCode.INVALID_RELATION,"周期结束早于开始");
        Set<String> ids=new HashSet<>();
        for(var c:w.courses()) {
            require(ids.add(c.courseId().toString()),ErrorCode.INVALID_RELATION,"周期课程重复");
            learning.historicalCourse(c.courseId().toString());
            require((c.startsAt()==null)==(c.endsAt()==null),ErrorCode.INVALID_RELATION,"考试时段需成对填写");
            if(c.startsAt()!=null)require(c.examDate()!=null&&time(c.startsAt()).isBefore(time(c.endsAt())),ErrorCode.INVALID_RELATION,"考试时段无效");
        }
        if(id==null) {
            id=uuid();
            repository.createCycle(id,w);
        }
        else {
            repository.lockCycle(id);
            repository.updateCycle(id,w);
            for(String old:repository.cycleCourses(id))if(!ids.contains(old))repository.removeCycleCourse(id,old);
        }
        repository.putCycleCourses(id,w);
        audit.add("CYCLE_UPDATED","EXAM_CYCLE",id,null,obj("courseCount",ids.size()));
        return learning.cycle(id);
    }
}
