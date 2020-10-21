package six.six.gateway.aspsms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
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
  
  private String senderId; 
  
  public AspSMSService(final String url, final String senderId)
  {
    this.url = url;
    this.senderId = senderId;
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
    // clean-up input, removing whitespaces and hyphens
    String phoneNumberSanitized = phoneNumber.replaceAll("-|\\s", "");
    
    AspSmsRequest request = new AspSmsRequest();
    request.setUserName(login);
    request.setPassword(pw);
    request.setOriginator(senderId);
    request.setRecipients(new String[] {phoneNumberSanitized});
    request.setMessageText(message);
    String json = gson.toJson(request);

    String gatewayUrl = this.url + "/SendTextSMS";
    HttpPost post = new HttpPost(gatewayUrl);
    //post.addHeader("Content-Type", "application/json");
    post.addHeader("Accept", "application/json");
    try 
    {
      post.setEntity(new StringEntity(json, "application/json", "UTF-8"));
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
      // decode response
      try {
        InputStream input = httpResponse.getEntity().getContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(input, baos);
        org.apache.commons.io.IOUtils.closeQuietly(input);
        String responseText = new String(baos.toByteArray(), "UTF-8");
  
        logger.warn("HTTP Status "+statusCode+" "+responseText);
      } catch (IOException e) {
        logger.warn("Failed to decode response from server, HTTP Status "+httpResponse.getStatusLine());
      }
      return false;
    } else {
      EntityUtils.consumeQuietly(httpResponse.getEntity());
    }
    
    return true;
  }

}
