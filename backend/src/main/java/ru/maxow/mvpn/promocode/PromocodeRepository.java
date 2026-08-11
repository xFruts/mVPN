package ru.maxow.mvpn.promocode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.maxow.mvpn.model.PromocodeStatus;

import java.util.Optional;

public interface PromocodeRepository
    extends JpaRepository<Promocode, Long>, JpaSpecificationExecutor<Promocode> {

  Optional<Promocode> findByCode(String code);

  long countByStatus(PromocodeStatus status);

  @Query("SELECT COALESCE(SUM(p.usage), 0) FROM Promocode p")
  long sumUsage();
}
