package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import ru.maxow.mvpn.model.UserRole;

@DataJpaTest
@DisplayName("UserRepository")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager entityManager;

  private User testUser;
  private UUID verificationCode;

  @BeforeEach
  void setUp() {
    verificationCode = UUID.fromString("11111111-1111-1111-1111-111111111111");
    testUser = new User();
    testUser.setFullName("Test User");
    testUser.setRole(UserRole.REGULAR);
    testUser.setUserTelegramId(12345L);
    testUser.setVerificationCode(verificationCode);
  }

  @Nested
  @DisplayName("verification code")
  class VerificationCode {

    @Test
    @DisplayName("findByVerificationCode returns user for known code")
    void findFound() {
      entityManager.persistAndFlush(testUser);

      Optional<User> found = userRepository.findByVerificationCode(verificationCode);

      assertThat(found).isPresent();
      assertThat(found.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("findByVerificationCode is empty for unknown code")
    void findMissing() {
      assertThat(userRepository.findByVerificationCode(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("existsByVerificationCode reflects presence")
    void exists() {
      entityManager.persistAndFlush(testUser);

      assertThat(userRepository.existsByVerificationCode(verificationCode)).isTrue();
      assertThat(userRepository.existsByVerificationCode(UUID.randomUUID())).isFalse();
    }
  }

  @Nested
  @DisplayName("identity lookups")
  class IdentityLookups {

    @Test
    @DisplayName("existsByFullName is true only for exact stored name")
    void existsByFullName() {
      entityManager.persistAndFlush(testUser);

      assertThat(userRepository.existsByFullName("Test User")).isTrue();
      assertThat(userRepository.existsByFullName("Other User")).isFalse();
    }

    @Test
    @DisplayName("findByUserTelegramId returns matching user")
    void findByTelegramId() {
      entityManager.persistAndFlush(testUser);

      assertThat(userRepository.findByUserTelegramId(12345L))
          .isPresent()
          .get()
          .extracting(User::getFullName)
          .isEqualTo("Test User");
      assertThat(userRepository.findByUserTelegramId(999999L)).isEmpty();
    }

    @Test
    @DisplayName("findByUserTelegramIdIn returns only matching users")
    void findByTelegramIds() {
      User second = new User();
      second.setFullName("Second User");
      second.setRole(UserRole.REGULAR);
      second.setUserTelegramId(22222L);

      entityManager.persistAndFlush(testUser);
      entityManager.persistAndFlush(second);

      List<User> found = userRepository.findByUserTelegramIdIn(List.of(12345L, 99999L));

      assertThat(found)
          .extracting(User::getUserTelegramId)
          .containsExactly(12345L);
    }
  }

  @Nested
  @DisplayName("role queries")
  class RoleQueries {

    @Test
    @DisplayName("findAllByRole returns only users with requested role")
    void findByRole() {
      User admin = new User();
      admin.setFullName("Admin");
      admin.setRole(UserRole.ADMIN);

      entityManager.persistAndFlush(testUser);
      entityManager.persistAndFlush(admin);

      assertThat(userRepository.findAllByRole(UserRole.REGULAR))
          .extracting(User::getFullName)
          .containsExactly("Test User");
      assertThat(userRepository.findAllByRole(UserRole.SPECIAL)).isEmpty();
    }
  }

  @Nested
  @DisplayName("persistence")
  class Persistence {

    @Test
    @DisplayName("save assigns id and persists defaults")
    void save() {
      User saved = userRepository.saveAndFlush(testUser);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getVerificationCode()).isEqualTo(verificationCode);
      assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("deleteById removes persisted user")
    void delete() {
      User saved = userRepository.saveAndFlush(testUser);
      Long id = saved.getId();

      userRepository.deleteById(id);
      entityManager.flush();

      assertThat(userRepository.findById(id)).isEmpty();
    }
  }
}
