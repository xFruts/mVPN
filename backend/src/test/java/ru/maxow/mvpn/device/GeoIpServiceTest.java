package ru.maxow.mvpn.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

@DisplayName("GeoIpService - unit tests")
class GeoIpServiceTest {

  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer mockServer;
  private GeoIpService geoIpService;

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    geoIpService = new GeoIpService("https://ip-api.com/json/", restClientBuilder.build());
  }

  @Test
  @DisplayName("lookup returns empty for null or blank or local IP")
  void shouldReturnEmptyForLocalOrBlankIp() {
    assertThat(geoIpService.lookup(null)).isEmpty();
    assertThat(geoIpService.lookup("   ")).isEmpty();
    assertThat(geoIpService.lookup("127.0.0.1")).isEmpty();
    assertThat(geoIpService.lookup("localhost")).isEmpty();
    assertThat(geoIpService.lookup("::1")).isEmpty();
    assertThat(geoIpService.lookup("10.0.0.1")).isEmpty();
    assertThat(geoIpService.lookup("192.168.1.1")).isEmpty();
    assertThat(geoIpService.lookup("172.16.0.1")).isEmpty();
    assertThat(geoIpService.lookup("172.31.255.255")).isEmpty();
  }

  @Test
  @DisplayName("lookup fetches info and cleans ISP legal prefix")
  void shouldFetchAndCleanIsp() {
    String json = """
        {
          "status": "success",
          "country": "Russia",
          "countryCode": "RU",
          "city": "Moscow",
          "isp": "PJSC Rostelecom",
          "org": "Rostelecom"
        }
        """;

    mockServer.expect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

    Optional<GeoIpInfo> result = geoIpService.lookup("8.8.8.8");

    assertThat(result).isPresent();
    assertThat(result.get().ip()).isEqualTo("8.8.8.8");
    assertThat(result.get().isp()).isEqualTo("Rostelecom");
    assertThat(result.get().city()).isEqualTo("Moscow");
    assertThat(result.get().country()).isEqualTo("Russia");
    assertThat(result.get().countryCode()).isEqualTo("RU");
    mockServer.verify();
  }

  @Test
  @DisplayName("lookup caches results on consecutive calls")
  void shouldCacheResults() {
    String json = """
        {
          "status": "success",
          "country": "Germany",
          "countryCode": "DE",
          "city": "Frankfurt",
          "isp": "Hetzner Online GmbH",
          "org": "Hetzner"
        }
        """;

    mockServer.expect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

    Optional<GeoIpInfo> first = geoIpService.lookup("1.1.1.1");
    Optional<GeoIpInfo> second = geoIpService.lookup("1.1.1.1");

    assertThat(first).isPresent();
    assertThat(second).isEqualTo(first);
    mockServer.verify();
  }

  @Test
  @DisplayName("circuit breaker fast-fails after failure threshold is reached")
  void shouldTripCircuitBreakerAfterConsecutiveFailures() {
    for (int i = 0; i < 5; i++) {
      mockServer.expect(MockRestRequestMatchers.method(HttpMethod.GET))
          .andRespond(MockRestResponseCreators.withServerError());
    }

    for (int i = 0; i < 5; i++) {
      Optional<GeoIpInfo> res = geoIpService.lookup("185.220.101." + (i + 1));
      assertThat(res).isEmpty();
    }
    mockServer.verify();

    // 6th call should trip circuit breaker without making an HTTP request
    Optional<GeoIpInfo> blocked = geoIpService.lookup("185.220.101.99");
    assertThat(blocked).isEmpty();
  }

  @Test
  @DisplayName("maskIp masks IPv4 and IPv6 addresses to protect PII")
  void shouldMaskIpCorrectly() {
    assertThat(GeoIpService.maskIp("192.168.1.50")).isEqualTo("192.168.1.***");
    assertThat(GeoIpService.maskIp("2001:db8:85a3::8a2e:370:7334"))
        .isEqualTo("2001:db8:85a3::8a2e:370:****");
    assertThat(GeoIpService.maskIp(null)).isEqualTo("***");
    assertThat(GeoIpService.maskIp("")).isEqualTo("***");
  }
}
