package ru.maxow.mvpn.promocode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromocodeRepository extends JpaRepository<Promocode, Long> {

  Optional<Promocode> findByCode(String code);
}
