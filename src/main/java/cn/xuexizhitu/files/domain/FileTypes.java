package cn.xuexizhitu.files.domain;
import java.time.Instant;
public final class FileTypes {
    private FileTypes() {
    }
    public record Metadata(String id, String name, String mimeType, long sizeBytes, String sha256,                            boolean containsAnswers, String state, String purpose) {
    }
    public record Stored(String id, String ownerId, String storageKey, Metadata metadata) {
    }
    public record Download(Metadata file, String contentBase64, String encoding) {
    }
    public record Deletion(String id, String state, boolean downloadAllowed, Instant acceptedAt) {
    }
}
