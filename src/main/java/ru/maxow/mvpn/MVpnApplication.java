package ru.maxow.mvpn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableSpringDataWebSupport
@SpringBootApplication
public class MVpnApplication {

  public static void main(String[] args) {
    SpringApplication.run(MVpnApplication.class, args);
  }

}
