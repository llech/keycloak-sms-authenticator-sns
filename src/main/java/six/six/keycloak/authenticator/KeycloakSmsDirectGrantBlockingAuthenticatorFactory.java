package six.six.keycloak.authenticator;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class KeycloakSmsDirectGrantBlockingAuthenticatorFactory implements AuthenticatorFactory
{
  private static Logger logger = Logger.getLogger(KeycloakSmsDirectGrantBlockingAuthenticatorFactory.class);

  public static final String PROVIDER_ID = "sms-authentication-dg-blocker";
  
  private KeycloakSmsDirectGrantBlockingAuthenticator SINGLETON = new KeycloakSmsDirectGrantBlockingAuthenticator();
  
  private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
      AuthenticationExecutionModel.Requirement.REQUIRED,
      AuthenticationExecutionModel.Requirement.OPTIONAL,
      AuthenticationExecutionModel.Requirement.DISABLED};

  @Override
  public Authenticator create(KeycloakSession session)
  {
    return SINGLETON;
  }

  @Override
  public void init(Scope config)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void postInit(KeycloakSessionFactory factory)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void close()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getId()
  {
    logger.debug("getId called ... returning " + PROVIDER_ID);
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayType()
  {
    String result = "SMS Authentication Direct Grant Blocker";
    logger.debug("getDisplayType called ... returning " + result);
    return result;
  }

  @Override
  public String getReferenceCategory()
  {
    return "sms-auth-code";
  }

  @Override
  public boolean isConfigurable()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Requirement[] getRequirementChoices()
  {
    logger.debug("getRequirementChoices called ... returning " + REQUIREMENT_CHOICES);
    return REQUIREMENT_CHOICES;
  }

  @Override
  public boolean isUserSetupAllowed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getHelpText()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

}
