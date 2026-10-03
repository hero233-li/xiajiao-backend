package cn.xuexizhitu.identity.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity @Table(name="login_identifier") @Getter @NoArgsConstructor public class LoginIdentifier {
    @Id @Column(length=254) private String identifier;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="user_id",nullable=false,length=36) private String userId;
    @Column(nullable=false,length=10) private String kind;
    public LoginIdentifier(String identifier,String userId,String kind) {
        this.identifier=identifier;
        this.userId=userId;
        this.kind=kind;
    }
}
