package cn.xuexizhitu.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
@Entity @Table(name="app_user") @Getter @NoArgsConstructor
public class AppUser {
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Id @Column(length=36) private String id;
    @Column(nullable=false,length=100,unique=true) private String username;
    @Column(nullable=false,length=254,unique=true) private String email;
    @Column(name="password_hash",nullable=false,length=200) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Role role;
    @Column(nullable=false) private boolean enabled;
    @Column(name="token_version",nullable=false) private int tokenVersion;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public AppUser(String id,String username,String email,String passwordHash,Role role) {
        this.id=id;this.username=username;this.email=email;this.passwordHash=passwordHash;this.role=role;this.enabled=true;
        this.createdAt=LocalDateTime.now(ZoneOffset.UTC);this.updatedAt=createdAt;
    }
    public void rotateTokenVersion() { tokenVersion=Math.incrementExact(tokenVersion);updatedAt=LocalDateTime.now(ZoneOffset.UTC); }
}
