package cn.xuexizhitu.identity.infrastructure;
import cn.xuexizhitu.identity.domain.LoginIdentifier;
import cn.xuexizhitu.identity.domain.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser,String> {
    @Query("select u from AppUser u, LoginIdentifier i where i.userId=u.id and i.identifier=:identifier")     Optional<AppUser> findByIdentifier(@Param("identifier") String identifier);
    @Lock(LockModeType.PESSIMISTIC_WRITE)     @Query("select u from AppUser u where u.id=:id")     Optional<AppUser> findLockedById(@Param("id") String id);
}
