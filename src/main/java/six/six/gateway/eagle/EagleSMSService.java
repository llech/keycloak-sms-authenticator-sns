package six.six.gateway.eagle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import six.six.gateway.SMSService;

public class EagleSMSService implements SMSService
{
  private static Logger logger = Logger.getLogger(EagleSMSService.class);
  
  private String url;
  
  private HttpClient httpClient;
  
  private Gson gson;
  
  public EagleSMSService(String url)
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
    EagleSmsRequest request = new EagleSmsRequest();
    request.getParams().setLogin(login);
    request.getParams().setPass(pw);
    request.getParams().setTo(phoneNumber);
    request.getParams().setMessage(message);

    String gatewayUrl = url + "/index.php/jsonrpc/sms";
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
    if (statusCode != 200) {
      logger.error("HTTP Status "+statusCode+" "+httpResponse.getStatusLine().getReasonPhrase());
      // return false
    } 
    String responseText;
    try 
    {
      InputStream input = httpResponse.getEntity().getContent();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      org.apache.commons.io.IOUtils.copy(input, baos);
      org.apache.commons.io.IOUtils.closeQuietly(input);
      responseText = new String(baos.toByteArray(), "UTF-8");
    }
    catch(IOException e)
    {
      logger.error(e.getClass().getName()+" "+e.getMessage());
      return false;
    }
    boolean isOk = responseText.contains("\"OK;");
    if (!isOk) {
      logger.warn(responseText);
    }

    return true;
  }
}
