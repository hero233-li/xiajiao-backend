package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.security.*;

@Service @RequiredArgsConstructor
public class PrivateFileStore {
    private final JdbcTemplate jdbc;
    @Value("${app.files.root:./data/private}") private String root;
    public Map<String,Object> row(String id){return jdbc.queryForList("SELECT * FROM stored_file WHERE id=?",id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));}
    public Map<String,Object> metadata(String id){var r=row(id);return obj("id",id,"name",r.get("original_name"),"mimeType",r.get("mime_type"),"sizeBytes",r.get("size_bytes"),"sha256",r.get("sha256"),"containsAnswers",r.get("contains_answers"),"state",r.get("state"),"purpose",r.get("purpose"));}
    private Path path(String key)throws IOException {require(key.matches("managed/[0-9a-f-]{36}"),ErrorCode.NOT_FOUND,"文件位置无效");Path base=Path.of(root).toAbsolutePath().normalize();Files.createDirectories(base.resolve("managed"));require(!Files.isSymbolicLink(base)&&!Files.isSymbolicLink(base.resolve("managed")),ErrorCode.NOT_FOUND,"文件目录无效");Path file=base.resolve(key);require(!Files.isSymbolicLink(file),ErrorCode.NOT_FOUND,"文件位置无效");return file;}
    public String upload(MultipartFile file,String purpose,String owner,boolean containsAnswers){
        require(Set.of("PAPER","MANUAL","SCORE_IMAGE").contains(purpose),ErrorCode.INVALID_PARAMETER,"文件用途无效");
        require(!file.isEmpty(),ErrorCode.INVALID_PARAMETER,"文件为空");require(file.getSize()<=8L*1024*1024,ErrorCode.PAYLOAD_TOO_LARGE,"文件超过8MB");
        String id=uuid();Path target=null;
        try {
            byte[] bytes=file.getBytes();String mime;try{mime=detect(bytes,purpose);}catch(IOException e){throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA,"文件内容无效");}
            String name=Objects.toString(file.getOriginalFilename(),"文件").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");if(name.isBlank())name="文件";require(name.length()<=300,ErrorCode.INVALID_PARAMETER,"文件名过长");
            target=path("managed/"+id);Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            jdbc.update("INSERT INTO stored_file(id,owner_id,purpose,storage_key,original_name,mime_type,size_bytes,sha256,contains_answers,state) VALUES(?,?,?,?,?,?,?,?,?,'ACTIVE')",id,owner,purpose,"managed/"+id,name,mime,bytes.length,hash(bytes),containsAnswers);
            Path written=target;if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(written);}catch(IOException ignored){}}});
            return id;
        }catch(IOException e){throw new BusinessException(ErrorCode.INTERNAL_ERROR,"私有文件保存失败");}catch(RuntimeException e){if(target!=null)try{Files.deleteIfExists(target);}catch(IOException ignored){}throw e;}
    }
    public String uploadJson(byte[] bytes,String name){return upload(new MultipartFile(){
        public String getName(){return "file";} public String getOriginalFilename(){return name;} public String getContentType(){return "application/json";} public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes;}public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(java.io.File dest)throws IOException{Files.write(dest.toPath(),bytes);}
    },"MANUAL",null,false);}
    private String detect(byte[] b,String purpose)throws IOException {
        if(purpose.equals("PAPER")){String head=new String(b,0,Math.min(8,b.length),java.nio.charset.StandardCharsets.US_ASCII);String tail=new String(b,Math.max(0,b.length-1024),Math.min(b.length,1024),java.nio.charset.StandardCharsets.ISO_8859_1);require(head.matches("%PDF-[12]\\.[0-9].*")&&tail.contains("%%EOF"),ErrorCode.UNSUPPORTED_MEDIA,"试卷需为PDF文件");return "application/pdf";}
        if(purpose.equals("MANUAL")){try{new com.fasterxml.jackson.databind.ObjectMapper().readTree(b);}catch(Exception e){throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA,"手册需为JSON文件");}return "application/json";}
        String magic=HexFormat.of().formatHex(Arrays.copyOf(b,Math.min(b.length,8)));
        String mime=magic.startsWith("ffd8ff")?"image/jpeg":magic.equals("89504e470d0a1a0a")?"image/png":null;
        if(mime!=null){try(var in=javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(b))){var readers=javax.imageio.ImageIO.getImageReaders(in);require(readers.hasNext(),ErrorCode.UNSUPPORTED_MEDIA,"图片内容无效");var reader=readers.next();try{reader.setInput(in);long pixels=(long)reader.getWidth(0)*reader.getHeight(0);require(pixels>0&&pixels<=40_000_000,ErrorCode.INVALID_PARAMETER,"图片尺寸过大或无效");require(reader.read(0)!=null,ErrorCode.UNSUPPORTED_MEDIA,"图片内容无效");}finally{reader.dispose();}}return mime;}
        if(b.length>=30&&new String(b,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(b,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) {
            var buffer=java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN);require(Integer.toUnsignedLong(buffer.getInt(4))+8==b.length,ErrorCode.UNSUPPORTED_MEDIA,"WebP内容不完整");boolean frame=false;
            for(int pos=12;pos+8<=b.length;){long length=Integer.toUnsignedLong(buffer.getInt(pos+4));require(length>0&&pos+8+length<=b.length,ErrorCode.UNSUPPORTED_MEDIA,"WebP内容无效");String type=new String(b,pos,4,java.nio.charset.StandardCharsets.US_ASCII);if(Set.of("VP8 ","VP8L","ANMF").contains(type)&&length>=5)frame=true;pos+=(int)(8+length+(length%2));}
            require(frame,ErrorCode.UNSUPPORTED_MEDIA,"WebP缺少图像帧");return "image/webp";
        }
        throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA,"只支持真实JPG、PNG和WebP图片");
    }
    public Map<String,Object> download(String id){var r=row(id);require(r.get("state").equals("ACTIVE"),ErrorCode.NOT_FOUND,"文件不可用");try{String key=(String)r.get("storage_key");byte[] b;
        if(key.startsWith("classpath:")){try(var in=getClass().getResourceAsStream("/"+key.substring("classpath:".length()).replaceFirst("^/", ""))){if(in==null)throw new BusinessException(ErrorCode.NOT_FOUND);b=in.readAllBytes();}}
        else{Path file=path(key);require(Files.size(file)==((Number)r.get("size_bytes")).longValue(),ErrorCode.NOT_FOUND,"文件完整性检查失败");b=Files.readAllBytes(file);}
        require(b.length==((Number)r.get("size_bytes")).longValue()&&hash(b).equals(r.get("sha256")),ErrorCode.NOT_FOUND,"文件完整性检查失败");return obj("file",metadata(id),"contentBase64",Base64.getEncoder().encodeToString(b),"encoding","base64");
    }catch(IOException e){throw new BusinessException(ErrorCode.NOT_FOUND,"文件不可用");}}
    public void delete(String id){jdbc.update("UPDATE stored_file SET state='DELETE_PENDING' WHERE id=? AND state='ACTIVE'",id);}
    public void cleanup(){for(var r:jdbc.queryForList("SELECT id,storage_key FROM stored_file WHERE state='DELETE_PENDING' LIMIT 100"))try{if(!((String)r.get("storage_key")).startsWith("classpath:"))Files.deleteIfExists(path((String)r.get("storage_key")));jdbc.update("UPDATE stored_file SET state='DELETED',deleted_at=UTC_TIMESTAMP(6) WHERE id=? AND state='DELETE_PENDING'",r.get("id"));}catch(IOException ignored){}}
    private String hash(byte[] b){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
