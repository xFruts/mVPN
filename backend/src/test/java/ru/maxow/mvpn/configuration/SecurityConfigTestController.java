package ru.maxow.mvpn.configuration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityConfigTestController {

  @GetMapping("/actuator/health")
  ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
  }

  @GetMapping("/v3/api-docs")
  ResponseEntity<String> docs() {
    return ResponseEntity.ok("docs");
  }

  @GetMapping("/v1/config/test")
  ResponseEntity<String> config() {
    return ResponseEntity.ok("config");
  }

  @GetMapping("/v1/users/test")
  ResponseEntity<String> users() {
    return ResponseEntity.ok("users");
  }

  @GetMapping("/secured/any")
  ResponseEntity<String> secured() {
    return ResponseEntity.ok("secured");
  }
}

