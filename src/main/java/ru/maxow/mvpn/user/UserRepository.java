package ru.maxow.mvpn.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  User findUsersById(Long id);
}
