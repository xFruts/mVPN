package ru.maxow.mvpn.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinioService {

  final MinioClient minioClient;

  @Value("${minio.bucket.name}")
  String bucketName;

  @PostConstruct
  private void createBucket() {
    try {
      boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!bucketExists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        log.info("Bucket {} created successfully.", bucketName);
      } else {
        log.info("Bucket {} already exists.", bucketName);
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not create or check bucket.", e);
    }
  }

  public String uploadFile(MultipartFile file, Long userId) {
    try {
      String objectName = "user-" + userId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .stream(file.getInputStream(), file.getSize(), -1)
              .contentType(file.getContentType())
              .build()
      );
      log.info("Uploaded file {} to bucket {}", file.getOriginalFilename(), bucketName);
      return objectName;
    } catch (Exception e) {
      throw new RuntimeException("Could not upload file to MinIO.", e);
    }
  }

}
