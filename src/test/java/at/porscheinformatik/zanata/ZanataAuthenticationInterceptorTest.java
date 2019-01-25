package at.porscheinformatik.zanata;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class ZanataAuthenticationInterceptorTest {

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;

  @Before
  public void setup() {
    restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new ZanataAuthenticationInterceptor("user", "token"));
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  public void addsAuthInfos() {
    mockServer
      .expect(header("X-Auth-User", "user"))
      .andExpect(header("X-Auth-Token", "token"))
      .andRespond(withSuccess());

    restTemplate.getForEntity("/", String.class);
  }

}
