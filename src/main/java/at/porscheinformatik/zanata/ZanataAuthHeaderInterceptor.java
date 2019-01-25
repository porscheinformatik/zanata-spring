package at.porscheinformatik.zanata;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class ZanataAuthHeaderInterceptor implements ClientHttpRequestInterceptor {


  private final String apiUser;
  private final String apiToken;

  public ZanataAuthHeaderInterceptor(String apiUser, String apiToken) {
    this.apiUser = apiUser;
    this.apiToken = apiToken;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {

    httpRequest.getHeaders().add("X-Auth-User", apiUser);
    httpRequest.getHeaders().add("X-Auth-Token", apiToken);

    return clientHttpRequestExecution.execute(httpRequest, bytes);
  }
}
