package six.six.gateway.horisen;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import six.six.gateway.SMSService;

public class HorisenSMSService implements SMSService
{
  private static Logger logger = Logger.getLogger(HorisenSMSService.class);
  
  private String url;
  
  private HttpClient httpClient;
  
  private Gson gson;
  
  public HorisenSMSService(String url)
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
    // first sanitize and verify phone number
    if (StringUtils.isBlank(phoneNumber))
      return false;
    // clean-up input, removing whitespaces, hyphens, trailing plus and zeros
    String phoneNumberSanitized = phoneNumber.replaceAll("-|\\s|^\\+|^0*", "");
    
    SmsAuth auth = new SmsAuth();
    auth.setUsername(login);
    auth.setPassword(pw);
    SmsRequest request = new SmsRequest();
    request.setAuth(auth);
    request.setSender("Keycloak");
    request.setReceiver(phoneNumberSanitized);
    request.setText(message);
    
    String gatewayUrl = url + "/bulk/sendsms";
    HttpPost post = new HttpPost(gatewayUrl);
    //post.addHeader("Content-Type", "application/json");
    post.addHeader("Accept", "application/json");
    try
    {
      post.setEntity(new StringEntity(gson.toJson(request), "application/json", "UTF-8"));
    }
    catch(UnsupportedEncodingException e)
    {
      logger.error("UnsupportedEncodingException: "+e.getMessage());
      return false;
    }
    HttpResponse httpResponse;
    try
    {
      httpResponse = httpClient.execute(post);
    }
    catch(IOException e)
    {
      logger.error(e.getClass().getName()+" "+e.getMessage());
      return false;
    }
    int statusCode = httpResponse.getStatusLine().getStatusCode();
    if (statusCode < 200 || statusCode >= 300) {
      logger.warn("HTTP Status "+statusCode+" "+httpResponse.getStatusLine().getReasonPhrase());
      return false;
    } 
    
    return true;
  }

}
