package ru.maxow.mvpn.server;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.minio.MinioService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerSshKeyStorageService {

  MinioService minioService;

  byte[] downloadPrivateKey(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      throw new IllegalArgumentException("SSH private key object key is not configured");
    }

    return minioService.downloadFileAsBytes(objectKey);
  }
}

