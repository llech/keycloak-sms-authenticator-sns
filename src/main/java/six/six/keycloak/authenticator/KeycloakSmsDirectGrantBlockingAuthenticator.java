package six.six.keycloak.authenticator;

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.events.Errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowError;

import six.six.keycloak.KeycloakSmsConstants;

public class KeycloakSmsDirectGrantBlockingAuthenticator implements Authenticator 
{
  
  private static Logger logger = Logger.getLogger(KeycloakSmsDirectGrantBlockingAuthenticator.class);

  @Override
  public void close()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void authenticate(AuthenticationFlowContext context)
  {
    logger.debug("authenticate called ... context = " + context);
    UserModel user = context.getUser();
    //AuthenticatorConfigModel config = context.getAuthenticatorConfig();
    AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("sms-authenticator");

    boolean onlyForVerification = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);

    String mobileNumber = KeycloakSmsAuthenticatorUtil.getMobileNumber(user);
    String mobileNumberVerified = KeycloakSmsAuthenticatorUtil.getMobileNumberVerified(user);
    
    if (StringUtils.isBlank(mobileNumber) || onlyForVerification) {
      context.success();
    } else {
      // direct grant flow has not feedback, no place to sent and validate SMS, therefore, if use has phone number configured, make decision based on optionality
      if (context.getExecution().isRequired()) {
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
        Response challengeResponse = Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).entity("Invalid user credentials").type(MediaType.TEXT_PLAIN).build();
        context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
      } else {
        context.attempted();
      }
    }
  }

  @Override
  public void action(AuthenticationFlowContext context)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean requiresUser()
  {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user)
  {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user)
  {
    // TODO Auto-generated method stub
    
  }

}
