package six.six.gateway.eagle;

public class EagleSmsRequest
{
  
  private String method = "sms.send_sms";
  
  private final EagleSmsRequestParams params = new EagleSmsRequestParams();

  public EagleSmsRequestParams getParams()
  {
    return params;
  }

  public String getMethod()
  {
    return method;
  }

  public void setMethod(String method)
  {
    this.method = method;
  }

}
