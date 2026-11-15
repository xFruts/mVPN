package ru.maxow.mvpn.device;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA репозиторий для сущности {@link UserDevice}.
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

  List<UserDevice> findByUserIdOrderByLastSeenAtDesc(Long userId);

  Optional<UserDevice> findByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);

  void deleteByUserId(Long userId);
}
