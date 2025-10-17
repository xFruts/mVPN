package ru.maxow.mvpn.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
  List<Server> findAllByStatus(ServerStatus status);
}
