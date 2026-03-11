package ru.maxow.mvpn.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.maxow.mvpn.model.UserRole;


public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByVerificationCode(UUID code);

  boolean existsByVerificationCode(UUID code);

  Optional<User> findByUserTelegramId(Long telegramId);

  List<User> findAllByRole(UserRole role);

  List<User> findByUserTelegramIdIn(List<Long> ids);
}
