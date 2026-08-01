package ru.maxow.mvpn.promocode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PromocodeRepository
    extends JpaRepository<Promocode, Long>, JpaSpecificationExecutor<Promocode> {

  Optional<Promocode> findByCode(String code);
}
