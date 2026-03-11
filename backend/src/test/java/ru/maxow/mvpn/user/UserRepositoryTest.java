package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import ru.maxow.mvpn.model.UserRole;

@DataJpaTest
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager entityManager;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setFullName("Test User");
    testUser.setRole(UserRole.REGULAR);
    testUser.setUserTelegramId(12345L);
    testUser.setVerificationCode(UUID.randomUUID());
  }

  @Test
  @DisplayName("findByVerificationCode: находит пользователя")
  void findByVerificationCode_found() {
    entityManager.persistAndFlush(testUser);

    Optional<User> found = userRepository.findByVerificationCode(testUser.getVerificationCode());

    assertThat(found).isPresent();
    assertThat(found.get().getFullName()).isEqualTo("Test User");
  }

  @Test
  @DisplayName("Должен вернуть Optional.empty, если verification code не существует")
  void testFindByVerificationCode_NotFound() {
    // Arrange
    UUID nonExistentCode = UUID.randomUUID();

    // Act
    Optional<User> found = userRepository.findByVerificationCode(nonExistentCode);

    // Assert
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("existsByVerificationCode: true для существующего кода")
  void existsByVerificationCode_true() {
    entityManager.persistAndFlush(testUser);

    assertThat(userRepository.existsByVerificationCode(testUser.getVerificationCode())).isTrue();
  }

  @Test
  @DisplayName("Должен вернуть false, если verification code не существует")
  void testExistsByVerificationCode_NotExists() {
    // Arrange
    UUID nonExistentCode = UUID.randomUUID();

    // Act
    boolean exists = userRepository.existsByVerificationCode(nonExistentCode);

    // Assert
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("findByUserTelegramId: находит пользователя")
  void findByUserTelegramId_found() {
    entityManager.persistAndFlush(testUser);

    Optional<User> found = userRepository.findByUserTelegramId(12345L);

    assertThat(found).isPresent();
    assertThat(found.get().getUserTelegramId()).isEqualTo(12345L);
  }

  @Test
  @DisplayName("Должен вернуть Optional.empty, если Telegram ID не существует")
  void testFindByUserTelegramId_NotFound() {
    // Arrange
    Long nonExistentTelegramId = 999999L;

    // Act
    Optional<User> found = userRepository.findByUserTelegramId(nonExistentTelegramId);

    // Assert
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("findAllByRole: возвращает пользователей только с нужной ролью")
  void findAllByRole_regularOnly() {
    User admin = new User();
    admin.setFullName("Admin");
    admin.setRole(UserRole.ADMIN);

    entityManager.persistAndFlush(testUser);
    entityManager.persistAndFlush(admin);

    List<User> users = userRepository.findAllByRole(UserRole.REGULAR);

    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getRole()).isEqualTo(UserRole.REGULAR);
  }

  @Test
  @DisplayName("Должен вернуть пустой список, если нет пользователей с указанной ролью")
  void testFindAllByRole_NoUsers() {
    // Arrange
    User regularUser = new User();
    regularUser.setFullName("Regular User");
    regularUser.setRole(UserRole.REGULAR);
    userRepository.save(regularUser);

    // Act
    List<User> adminUsers = userRepository.findAllByRole(UserRole.ADMIN);

    // Assert
    assertThat(adminUsers).isEmpty();
  }

  @Test
  @DisplayName("save: сохраняет пользователя")
  void save_success() {
    User saved = userRepository.save(testUser);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getFullName()).isEqualTo("Test User");
  }

  @Test
  @DisplayName("deleteById: удаляет пользователя")
  void deleteById_success() {
    User saved = userRepository.save(testUser);

    userRepository.deleteById(saved.getId());

    assertThat(userRepository.findById(saved.getId())).isEmpty();
  }
}
