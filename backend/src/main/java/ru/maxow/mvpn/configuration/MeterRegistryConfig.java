package ru.maxow.mvpn.configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterRegistryConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> commonTags(
      @Value("${spring.application.name:mVPN}") String applicationName,
      @Value("${info.app.version:1.0.0}") String apiVersion
  ) {
    return registry -> registry.config()
        .commonTags(
            "application", applicationName,
            "team", "backend",
            "version", apiVersion
        );
  }

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsFilters() {
    return registry -> registry.config()
        .meterFilter(new MeterFilter() {
          @Override
          public DistributionStatisticConfig configure(
              Meter.Id id, DistributionStatisticConfig config) {
            if (id.getType() == Meter.Type.TIMER
                && id.getName().startsWith("repository.")) {
              return DistributionStatisticConfig.builder()
                  .percentilesHistogram(true)
                  .build()
                  .merge(config);
            }
            return config;
          }
        });
  }
}
