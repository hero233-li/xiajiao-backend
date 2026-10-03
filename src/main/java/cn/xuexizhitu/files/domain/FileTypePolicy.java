package cn.xuexizhitu.files.domain;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.require;
import java.io.*;
import java.util.*;
public final class FileTypePolicy {
    public String detect(byte[] b,String purpose)throws IOException {
        if(purpose.equals("PAPER")) {
            String head=new String(b,0,Math.min(8,b.length),java.nio.charset.StandardCharsets.US_ASCII);
            String tail=new String(b,Math.max(0,b.length-1024),Math.min(b.length,1024),java.nio.charset.StandardCharsets.ISO_8859_1);
            require(head.matches("%PDF-[12]\\.[0-9].*")&&tail.contains("%%EOF"),ErrorCode.UNSUPPORTED_MEDIA,"试卷需为PDF文件");
            return "application/pdf";
        }
        if(purpose.equals("MANUAL")) {
            try {
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(b);
            }
            catch(Exception e) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA,"手册需为JSON文件");
            }
            return "application/json";
        }
        String magic=HexFormat.of().formatHex(Arrays.copyOf(b,Math.min(b.length,8)));
        String mime=magic.startsWith("ffd8ff")?"image/jpeg":magic.equals("89504e470d0a1a0a")?"image/png":null;
        if(mime!=null) {
            try(var in=javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
                var readers=javax.imageio.ImageIO.getImageReaders(in);
                require(readers.hasNext(),ErrorCode.UNSUPPORTED_MEDIA,"图片内容无效");
                var reader=readers.next();
                try {
                    reader.setInput(in);
                    long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                    require(pixels>0&&pixels<=40_000_000,ErrorCode.INVALID_PARAMETER,"图片尺寸过大或无效");
                    require(reader.read(0)!=null,ErrorCode.UNSUPPORTED_MEDIA,"图片内容无效");
                }
                finally {
                    reader.dispose();
                }
            }
            return mime;
        }
        if(b.length>=30&&new String(b,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(b,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) {
            var buffer=java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            require(Integer.toUnsignedLong(buffer.getInt(4))+8==b.length,ErrorCode.UNSUPPORTED_MEDIA,"WebP内容不完整");
            boolean frame=false;
            for(int pos=12;pos+8<=b.length;) {
                long length=Integer.toUnsignedLong(buffer.getInt(pos+4));
                require(length>0&&pos+8+length<=b.length,ErrorCode.UNSUPPORTED_MEDIA,"WebP内容无效");
                String type=new String(b,pos,4,java.nio.charset.StandardCharsets.US_ASCII);
                if(Set.of("VP8 ","VP8L","ANMF").contains(type)&&length>=5)frame=true;
                pos+=(int)(8+length+(length%2));
            }
            require(frame,ErrorCode.UNSUPPORTED_MEDIA,"WebP缺少图像帧");
            return "image/webp";
        }
        throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA,"只支持真实JPG、PNG和WebP图片");
    }
}
