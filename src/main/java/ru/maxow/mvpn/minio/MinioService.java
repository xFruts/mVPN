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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling file operations with MinIO.
 */
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

  /**
   * Uploads a file to MinIO.
   *
   * @param file the file to upload
   * @return the name of the uploaded object
   */
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
  /**
   * Downloads a file from MinIO.
   *
   * @param objectName the name of the object to download
   * @return an InputStream of the downloaded file
   */
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

  /**
   * Deletes a file from MinIO.
   *
   * @param objectName the name of the object to delete
   */
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
