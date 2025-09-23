package ru.maxow.mvpn.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByVerificationCode(UUID code);
  boolean existsByVerificationCode(UUID code);

  @Query("SELECT u FROM User u WHERE u.role = 'REGULAR'")
  List<User> getRegularUser();

  Optional<User> findByUserTelegramId(Long telegramId);
}
