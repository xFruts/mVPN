package ru.maxow.mvpn.minio;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioService {

  MinioClient minioClient;

  @Value("${minio.bucket.name}")
  @NonFinal
  String bucketName;

  @PostConstruct
  private void createBucket() {
    try {
      boolean bucketExists =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
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

  public String uploadFile(MultipartFile file) {
    try {
      String objectName = UUID.randomUUID()
          + "-" + file.getOriginalFilename();

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

  public InputStream downloadFile(String objectName) {
    try {
      return minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .build()
      );
    } catch (Exception e) {
      throw new RuntimeException("Could not download file from MinIO.", e);
    }
  }

  public byte[] downloadFileAsBytes(String objectName) {
    try (InputStream inputStream = downloadFile(objectName)) {
      return inputStream.readAllBytes();
    } catch (Exception e) {
      throw new RuntimeException("Could not read file from MinIO.", e);
    }
  }

  public void deleteFile(String objectName) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .build()
      );
      log.info("Deleted file {} from bucket {}", objectName, bucketName);
    } catch (Exception e) {
      throw new RuntimeException("Could not delete file from MinIO.", e);
    }
  }
}
