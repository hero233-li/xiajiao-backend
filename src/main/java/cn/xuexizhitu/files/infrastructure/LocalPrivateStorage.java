package cn.xuexizhitu.files.infrastructure;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.require;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.security.*;
@Component public class LocalPrivateStorage {
    private final Path root;
    public LocalPrivateStorage(@Value("${app.files.root:./data/private}") String root) {
        this.root=Path.of(root).toAbsolutePath().normalize();
    }
    private Path directory(String name) throws IOException {
        Files.createDirectories(root);
        // Check every existing ancestor as well as the leaf directory.
        for(Path p=root;p!=null;p=p.getParent())require(!Files.isSymbolicLink(p),ErrorCode.NOT_FOUND,"文件目录无效");
        Path dir=root.resolve(name);
        require(!Files.isSymbolicLink(dir),ErrorCode.NOT_FOUND,"文件目录无效");
        Files.createDirectories(dir);
        return dir;
    }
    public Path managed(String key) throws IOException {
        require(key.matches("managed/[0-9a-f-]{36}"),ErrorCode.NOT_FOUND,"文件位置无效");
        Path file=directory("managed").resolve(key.substring(8));
        require(!Files.isSymbolicLink(file),ErrorCode.NOT_FOUND,"文件位置无效");
        return file;
    }
    public void write(String id,byte[] bytes) throws IOException {
        Path journal=directory("pending").resolve(id);
        Files.writeString(journal,Instant.now().toString(),StandardOpenOption.CREATE_NEW);
        // Journal survives a process crash; write only to a new UUID path.
        Files.write(managed("managed/"+id),bytes,StandardOpenOption.CREATE_NEW);
    }
    public void acknowledge(String id) throws IOException {
        Files.deleteIfExists(directory("pending").resolve(id));
    }
    public void rollback(String id) throws IOException {
        Files.deleteIfExists(managed("managed/"+id));
        acknowledge(id);
    }
    public List<String> pending() throws IOException {
        try(var stream=Files.list(directory("pending"))) {
            return stream.filter(p->p.getFileName().toString().matches("[0-9a-f-]{36}")).filter(p-> {
                try {
                    return Files.getLastModifiedTime(p).toInstant().isBefore(Instant.now().minus(Duration.ofHours(24)));
                }
                catch(IOException e) {
                    return false;
                }
            }
            ).limit(100).map(p->p.getFileName().toString()).toList();
        }
    }
    public void quarantine(String id) throws IOException {
        Path source=managed("managed/"+id),target=directory("quarantine").resolve(id);
        if(Files.exists(source,LinkOption.NOFOLLOW_LINKS))Files.move(source,target);
        acknowledge(id);
        // Never destroy crash-orphan bytes automatically.
    }
    public byte[] read(String key) throws IOException {
        if(key.startsWith("classpath:")) {
            require(key.matches("classpath:(manuals|papers)/[a-zA-Z0-9._/-]+" )&&!key.contains(".."),ErrorCode.NOT_FOUND,"文件位置无效");
            try(var in=getClass().getResourceAsStream("/"+key.substring(10))) {
                if(in==null)throw new FileNotFoundException();
                return in.readNBytes(8*1024*1024+1);
            }
        }
        Path file=managed(key);
        require(Files.size(file)<=8L*1024*1024,ErrorCode.NOT_FOUND,"文件完整性检查失败");
        return Files.readAllBytes(file);
    }
    public void delete(String key) throws IOException {
        if(!key.startsWith("classpath:"))Files.deleteIfExists(managed(key));
    }
    public boolean exists(String key) throws IOException {
        return Files.isRegularFile(managed(key),LinkOption.NOFOLLOW_LINKS);
    }
    public static String hash(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        }
        catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
