package ru.maxow.mvpn.configuration;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MinIO client.
 */
@Configuration
public class MinioConfig {

  @Value("${minio.url}")
  private String url;

  @Value("${minio.access.name}")
  private String accessKey;

  @Value("${minio.access.secret}")
  private String secretKey;

  /**
   * Creates and configures a MinioClient bean.
   *
   * @return the configured MinioClient instance
   */
  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(url)
        .credentials(accessKey, secretKey)
        .build();
  }
}
