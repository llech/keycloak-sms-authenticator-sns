package six.six.gateway.aspsms;

public class AspSmsRequest
{
  
  private String UserName;
  
  private String Password;
  
  private String Originator;
  
  private String[] Recipients;
  
  private String MessageText;

  public String getUserName()
  {
    return UserName;
  }

  public void setUserName(String userName)
  {
    UserName = userName;
  }

  public String getPassword()
  {
    return Password;
  }

  public void setPassword(String password)
  {
    Password = password;
  }

  public String getOriginator()
  {
    return Originator;
  }

  public void setOriginator(String originator)
  {
    Originator = originator;
  }

  public String[] getRecipients()
  {
    return Recipients;
  }

  public void setRecipients(String[] recipients)
  {
    Recipients = recipients;
  }

  public String getMessageText()
  {
    return MessageText;
  }

  public void setMessageText(String messageText)
  {
    MessageText = messageText;
  }

}
