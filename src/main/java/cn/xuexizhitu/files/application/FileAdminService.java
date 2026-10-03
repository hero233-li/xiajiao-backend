package cn.xuexizhitu.files.application;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.files.domain.FileTypes.Metadata;
import cn.xuexizhitu.files.infrastructure.FileRepository;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class FileAdminService {
    private final FileRepository repository;
    private final PrivateFileStore files;
    private final AuditRepository audit;
    public Page<Metadata> files(String purpose,int page,int size) {
        CurrentUser.requireAdmin();
        require(purpose==null||Set.of("PAPER","MANUAL").contains(purpose),ErrorCode.INVALID_PARAMETER,"用途无效");
        return repository.adminFiles(purpose,page,size);
    }
    @Transactional public Metadata upload(MultipartFile file,String purpose,boolean containsAnswers) {
        CurrentUser.requireAdmin();
        require(Set.of("PAPER","MANUAL").contains(purpose),ErrorCode.INVALID_PARAMETER,"用途无效");
        String id=files.upload(file,purpose,null,containsAnswers);
        audit.add("CONTENT_FILE_UPLOADED","STORED_FILE",id,null,obj("purpose",purpose,"containsAnswers",containsAnswers));
        return files.metadata(id);
    }
}
