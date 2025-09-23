package ru.maxow.mvpn.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByVerificationCode(UUID code);
  boolean existsByVerificationCode(UUID code);
}
