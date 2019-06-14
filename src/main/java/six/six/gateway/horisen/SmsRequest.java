package six.six.gateway.horisen;

public class SmsRequest
{
  
  private String type = "text";
  
  private SmsAuth auth;
  
  private String sender;
  
  private String receiver;
  
  private String dcs = "GSM";
  
  private String text;

  public String getType()
  {
    return type;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  public SmsAuth getAuth()
  {
    return auth;
  }

  public void setAuth(SmsAuth auth)
  {
    this.auth = auth;
  }

  public String getSender()
  {
    return sender;
  }

  public void setSender(String sender)
  {
    this.sender = sender;
  }

  public String getReceiver()
  {
    return receiver;
  }

  public void setReceiver(String receiver)
  {
    this.receiver = receiver;
  }

  public String getDcs()
  {
    return dcs;
  }

  public void setDcs(String dcs)
  {
    this.dcs = dcs;
  }

  public String getText()
  {
    return text;
  }

  public void setText(String text)
  {
    this.text = text;
  }

}
