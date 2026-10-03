package cn.xuexizhitu.files.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.files.domain.FileTypes.*;
import cn.xuexizhitu.files.domain.FileTypePolicy;
import cn.xuexizhitu.files.infrastructure.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.transaction.PlatformTransactionManager;
import java.io.*;
import java.util.*;
@Service @RequiredArgsConstructor @Slf4j public class PrivateFileStore {
    private final FileRepository repository;
    private final LocalPrivateStorage storage;
    private final PlatformTransactionManager transactions;
    private String inventoryCursor="";
    public Stored row(String id) {
        return repository.required(id);
    }
    public Metadata metadata(String id) {
        return row(id).metadata();
    }
    @Transactional     public String upload(MultipartFile file,String purpose,String owner,boolean containsAnswers) {
        require(Set.of("PAPER","MANUAL","SCORE_IMAGE").contains(purpose),ErrorCode.INVALID_PARAMETER,"文件用途无效");
        require(!file.isEmpty(),ErrorCode.INVALID_PARAMETER,"文件为空");
        require(file.getSize()<=8L*1024*1024,ErrorCode.PAYLOAD_TOO_LARGE,"文件超过8MB");
        String id=uuid();
        try {
            byte[] bytes=file.getBytes();
            require(bytes.length>0&&bytes.length<=8*1024*1024,ErrorCode.PAYLOAD_TOO_LARGE,"文件大小无效");
            String mime=new FileTypePolicy().detect(bytes,purpose);
            String name=Objects.toString(file.getOriginalFilename(),"文件").replace('\\','/');
            name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
            if(name.isBlank())name="文件";
            require(name.length()<=300,ErrorCode.INVALID_PARAMETER,"文件名过长");
            // Register before disk/DB writes so either failure reaches compensation.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    try {
                        if(status==STATUS_COMMITTED)storage.acknowledge(id);else storage.rollback(id);
                    }
                    catch(IOException|RuntimeException e) {
                        log.error("File compensation requires recovery: {} ({})",id,e.getClass().getSimpleName());
                    }
                }
            }
            );
            storage.write(id,bytes);
            repository.insert(new Stored(id,owner,"managed/"+id,new Metadata(id,name,mime,bytes.length,LocalPrivateStorage.hash(bytes),containsAnswers,"ACTIVE",purpose)));
            return id;
        }
        catch(IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,"私有文件保存失败");
        }
    }
    @Transactional     public String uploadJson(byte[] bytes,String name) {
        return upload(new MultipartFile() {
            public String getName() {
                return "file";
            }
            public String getOriginalFilename() {
                return name;
            }
            public String getContentType() {
                return "application/json";
            }
            public boolean isEmpty() {
                return bytes.length==0;
            }
            public long getSize() {
                return bytes.length;
            }
            public byte[] getBytes() {
                return bytes;
            }
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }
            public void transferTo(java.io.File dest)throws IOException {
                java.nio.file.Files.write(dest.toPath(),bytes);
            }
        }
        ,"MANUAL",null,false);
    }
    public Download download(String id) {
        Stored row=row(id);
        Metadata m=row.metadata();
        require(m.state().equals("ACTIVE"),ErrorCode.NOT_FOUND,"文件不可用");
        try {
            byte[] bytes=storage.read(row.storageKey());
            require(bytes.length==m.sizeBytes()&&LocalPrivateStorage.hash(bytes).equals(m.sha256()),ErrorCode.NOT_FOUND,"文件完整性检查失败");
            return new Download(m,Base64.getEncoder().encodeToString(bytes),"base64");
        }
        catch(IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND,"文件不可用");
        }
    }
    @Transactional public void delete(String id) {
        repository.delete(id);
    }
    public synchronized void cleanup() {
        var tx=new TransactionTemplate(transactions);
        for(String id:repository.pending()) {
            var task=tx.execute(s->repository.claim(id));
            if(task==null)continue;
            try {
                storage.delete(task.storageKey());
                tx.executeWithoutResult(s->repository.completed(task));
            }
            catch(IOException|RuntimeException e) {
                tx.executeWithoutResult(s->repository.failed(task,e.getClass().getSimpleName()));
                log.warn("File deletion will retry: {} ({})",id,e.getClass().getSimpleName());
            }
        }
        try {
            for(String id:storage.pending()) {
                if(repository.find(id).isPresent())storage.acknowledge(id);
                else {
                    storage.quarantine(id);
                    repository.reconcile(id,"CRASH_ORPHAN_QUARANTINED");
                }
            }
            // Bounded keyset scan, eventually visits all active files without loading the inventory.
            var rows=repository.inventory(inventoryCursor,100);
            for(Stored row:rows)if(!storage.exists(row.storageKey()))repository.reconcile(row.id(),"ACTIVE_FILE_MISSING");
            inventoryCursor=rows.size()<100?"":rows.get(rows.size()-1).id();
        }
        catch(IOException|RuntimeException e) {
            log.error("Private file reconciliation interrupted ({})",e.getClass().getSimpleName());
        }
    }
}
