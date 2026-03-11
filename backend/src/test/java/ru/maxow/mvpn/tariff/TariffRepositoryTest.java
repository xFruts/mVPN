package ru.maxow.mvpn.tariff;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("TariffRepository Integration Tests")
public class TariffRepositoryTest {
  @Autowired
  private TariffRepository tariffRepository;

  @Autowired
  private TestEntityManager entityManager;

  private Tariff testTariff;

  @BeforeEach
  void setUp() {
    testTariff = new Tariff();
    testTariff.setName("Test Tariff");
    testTariff.setMaxDevices(8);
    testTariff.setTrafficLimitGb(100);
  }

  @Test
  @DisplayName("Test Tariff Creation Test")
  void testTariffCreation() {

  }
}

