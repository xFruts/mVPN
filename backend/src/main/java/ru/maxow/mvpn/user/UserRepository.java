package ru.maxow.mvpn.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.maxow.mvpn.model.UserRole;


public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  Optional<User> findByVerificationCode(UUID code);

  boolean existsByVerificationCode(UUID code);

  boolean existsByFullName(String fullName);

  Optional<User> findByUserTelegramId(Long telegramId);

  List<User> findAllByRole(UserRole role);

  List<User> findByUserTelegramIdIn(List<Long> ids);
}
