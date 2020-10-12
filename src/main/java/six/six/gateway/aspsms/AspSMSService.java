package six.six.gateway.aspsms;

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import six.six.gateway.SMSService;

public class AspSMSService implements SMSService
{
  private static Logger logger = Logger.getLogger(AspSMSService.class);
  
  private String url;
  
  private HttpClient httpClient;
  
  private Gson gson;
  
  public AspSMSService(final String url)
  {
    this.url = url;
    httpClient = org.apache.http.impl.client.HttpClients.custom()
        .setMaxConnPerRoute(10)
        .setMaxConnTotal(20)
        .build();
    gson = new GsonBuilder().disableHtmlEscaping().create();
  }

  @Override
  public boolean send(String phoneNumber, String message, String login, String pw)
  {
    logger.warn("AspSMSService::send : not implemented yet");
    // TODO Auto-generated method stub
    return false;
  }

}
