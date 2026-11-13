package com.gym.foundation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationBoundaryTest {
    @Value("${local.server.port}") int port;
    @Autowired ApplicationContext context;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private HttpResponse<String> request(String method, String path, boolean forged) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method(method, HttpRequest.BodyPublishers.noBody());
        if (forged) builder.header("Authorization", "Bearer forged-identity");
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void healthIsMinimalAndAvailable() throws Exception {
        var response = request("GET", "/actuator/health", false);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
    }
    @Test void unconfiguredApplicationStartsWithoutDatasource() {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(context.getEnvironment().getProperty("spring.datasource.url")).isNull();
    }
    @Test void protectedPathsDenyAnonymous() throws Exception {
        for (String path : new String[]{"/v1/me/contexts", "/v1/orders", "/actuator/env", "/actuator/configprops", "/unknown", "/actuator/health/liveness"}) {
            assertThat(request("GET", path, false).statusCode()).as(path).isIn(401, 403);
        }
        for (String method : new String[]{"POST", "HEAD", "OPTIONS", "PUT", "DELETE"}) {
            assertThat(request(method, "/actuator/health", false).statusCode()).as(method).isIn(401, 403);
        }
    }
    @Test void forgedIdentityDoesNotGrantAccess() throws Exception {
        for (String path : new String[]{"/v1/me/contexts", "/v1/orders", "/actuator/env", "/actuator/configprops"}) {
            assertThat(request("GET", path + "?entry=platform-admin&merchantId=other&storeId=other", true).statusCode()).as(path).isIn(401, 403);
        }
    }
    @Test void noDevelopmentLoginExposed() throws Exception {
        assertThat(request("GET", "/login", false).statusCode()).isIn(401, 403);
        assertThat(request("GET", "/logout", false).statusCode()).isIn(401, 403);
        assertThat(context.getBeansOfType(org.springframework.security.core.userdetails.UserDetailsService.class)).isEmpty();
    }
    @Test void healthDoesNotLeakConfiguration() throws Exception {
        var response = request("GET", "/actuator/health?showDetails=always", true);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
    }
}
