package at.porscheinformatik.zanata;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ZanataMessageSourceTest {

  private ObjectMapper objectMapper = new ObjectMapper();
  private ZanataMessageSource messageSource;
  public static final ZanataMessageSource.TextFlowTarget TEXT_1 = new ZanataMessageSource.TextFlowTarget();
  public static final ZanataMessageSource.TextFlowTarget TEXT_2 = new ZanataMessageSource.TextFlowTarget();
  public static final ZanataMessageSource.TextFlowTarget TEXT_3 = new ZanataMessageSource.TextFlowTarget();
  public static final ZanataMessageSource.TextFlowTarget TEXT_4 = new ZanataMessageSource.TextFlowTarget();

  static {
    TEXT_1.resId = "text1";
    TEXT_1.content = "Hallo Welt";
    TEXT_1.state = ZanataMessageSource.ContentState.Translated;
    TEXT_2.resId = "text1";
    TEXT_2.content = "Hallo Welt 2";
    TEXT_2.state = ZanataMessageSource.ContentState.Translated;
    TEXT_3.resId = "text3";
    TEXT_3.content = "Hy there";
    TEXT_3.state = ZanataMessageSource.ContentState.Translated;
    TEXT_4.resId = "text3";
    TEXT_4.content = "Hi deer";
    TEXT_4.state = ZanataMessageSource.ContentState.Translated;
  }

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;

  @Before
  public void setup() {
    messageSource = new ZanataMessageSource();
    messageSource.setProject("MyApp");
    messageSource.setZanataBaseUrl("https://my-zanata/zanata");
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    messageSource.setRestTemplate(restTemplate);
  }

  @After
  public void verify() {
    mockServer.verify();
  }

  @Test
  public void simpleReseourceAndCache() throws JsonProcessingException {
    mockCall(Locale.GERMAN, TEXT_1);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
    // try another time for caching
    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
  }

  @Test
  public void clearCache() throws JsonProcessingException {
    mockCall(Locale.GERMAN, TEXT_1);
    mockCall(Locale.GERMAN, TEXT_1);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
    messageSource.clearCache();
    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
  }

  @Test
  public void langAndCountry() throws JsonProcessingException {

    mockCall(Locale.GERMANY, TEXT_2);
    mockCall(Locale.GERMAN, TEXT_1);

    assert "Hallo Welt 2".equals(messageSource.getMessage("text1", null, Locale.GERMANY));
  }

  @Test(expected = NoSuchMessageException.class)
  public void httpError() {
    mockServer.expect(anything()).andRespond(MockRestResponseCreators.withServerError());

    messageSource.getMessage("text1", null, Locale.ENGLISH);
  }

  @Test
  public void multipleBaseNames() throws JsonProcessingException {
    messageSource.setBaseNames("bundle1", "bundle2");
    mockCall(Locale.FRENCH, "bundle1", TEXT_1);
    mockCall(Locale.FRENCH, "bundle2", TEXT_2);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.FRENCH));
  }

  @Test
  public void allProperties() throws JsonProcessingException {
    mockCall(Locale.US, TEXT_4);
    mockCall(Locale.ENGLISH, TEXT_1, TEXT_3);

    final Properties allProperties = messageSource.getAllProperties(Locale.US);
    assert "Hallo Welt".equals(allProperties.getProperty("text1"));
    assert "Hi deer".equals(allProperties.getProperty("text3"));
  }

  private void mockCall(Locale locale, ZanataMessageSource.TextFlowTarget... textFlowTarget) throws JsonProcessingException {
    mockCall(locale, "messages", textFlowTarget);
  }

  private void mockCall(Locale locale, String resource, ZanataMessageSource.TextFlowTarget... text) throws JsonProcessingException {
    ZanataMessageSource.TranslationsResource answer2 = new ZanataMessageSource.TranslationsResource();
    answer2.textFlowTargets.addAll(Arrays.asList(text));
    mockServer
        .expect(requestTo("https://my-zanata/zanata/rest/projects/p/"
          + messageSource.getProject()
          + "/iterations/i/master/r/" + resource
          + "/translations/" + locale.toLanguageTag()))
        .andRespond(withSuccess(objectMapper.writeValueAsString(answer2), MediaType.APPLICATION_JSON));
  }
}