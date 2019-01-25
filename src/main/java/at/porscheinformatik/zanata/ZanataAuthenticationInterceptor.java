package at.porscheinformatik.zanata;

import java.io.IOException;
import java.util.Collections;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds Zanata authentication information to HTTP requests.
 */
public class ZanataAuthenticationInterceptor implements ClientHttpRequestInterceptor {

  private final String authUser;
  private final String authToken;

  /**
   * @param authUser Zanata API user
   * @param authToken Zanata API token
   */
  public ZanataAuthenticationInterceptor(String authUser, String authToken) {
    this.authUser = authUser;
    this.authToken = authToken;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
    ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {

    httpRequest.getHeaders().add("X-Auth-User", authUser);
    httpRequest.getHeaders().add("X-Auth-Token", authToken);
    httpRequest.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    return clientHttpRequestExecution.execute(httpRequest, bytes);
  }
}