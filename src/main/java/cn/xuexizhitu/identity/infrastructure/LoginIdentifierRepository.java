package cn.xuexizhitu.identity.infrastructure;
import cn.xuexizhitu.identity.domain.LoginIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LoginIdentifierRepository extends JpaRepository<LoginIdentifier,String> {
}
