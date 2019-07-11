package six.six.keycloak.authenticator;

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import six.six.keycloak.KeycloakSmsConstants;
import six.six.keycloak.requiredaction.action.required.KeycloakSmsMobilenumberRequiredAction;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 * Created by joris on 11/11/2016.
 */
public class KeycloakSmsAuthenticator implements Authenticator {

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticator.class);

    private enum CODE_STATUS {
        VALID,
        INVALID,
        EXPIRED
    }


    private boolean isOnlyForVerificationMode(boolean onlyForVerification,String mobileNumber,String mobileNumberVerified){
        return (mobileNumber ==null || onlyForVerification==true && !mobileNumber.equals(mobileNumberVerified) );
    }


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("authenticate called ... context = " + context);
        UserModel user = context.getUser();
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        boolean onlyForVerification=KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);

        String mobileNumber = KeycloakSmsAuthenticatorUtil.getMobileNumber(user);
        String mobileNumberVerified = KeycloakSmsAuthenticatorUtil.getMobileNumberVerified(user);

        if (onlyForVerification==false || isOnlyForVerificationMode(onlyForVerification, mobileNumber,mobileNumberVerified)){
            if (StringUtils.isNotBlank(mobileNumber)) {
                // The mobile number is configured --> send an SMS

                // TODO do not generate new token if old token is still valid?
              if (sendSMSToken(context)) {
                Response challenge = context.form().createForm("sms-validation.ftl");
                context.challenge(challenge);
              } else {
                Response challenge = context.form()
                    .setError("sms-auth.not.send")
                    .createForm("sms-validation-error.ftl");
                context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
              }
            } else {
                boolean isAskingFor=KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_ASKFOR_ENABLED);
                if(isAskingFor){
                    //Enable access and ask for mobilenumber
                    user.addRequiredAction(KeycloakSmsMobilenumberRequiredAction.PROVIDER_ID);
                    context.success();
                }else {
                    // The mobile number is NOT configured --> complain
                    Response challenge = context.form()
                            .setError("sms-auth.not.mobile")
                            .createForm("sms-validation-error.ftl");
                    context.failureChallenge(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challenge);
                }
            }
        }else{
            logger.debug("Skip SMS code because onlyForVerification " + onlyForVerification + " or  mobileNumber==mobileNumberVerified");
            context.success();
        }
    }
    
    private boolean sendSMSToken(AuthenticationFlowContext context){
      UserModel user = context.getUser();
      AuthenticatorConfigModel config = context.getAuthenticatorConfig();
      String mobileNumber = KeycloakSmsAuthenticatorUtil.getMobileNumber(user);
      
      long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
      logger.debug("Using nrOfDigits " + nrOfDigits);
      long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s
      logger.debug("Using ttl " + ttl + " (s)");
      boolean useMock = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.CONF_PRP_SMSM_USE_MOCK, false);
      String mockCode = KeycloakSmsAuthenticatorUtil.getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMSM_MOCK_CODE, "123456");
      String code = useMock ? mockCode : KeycloakSmsAuthenticatorUtil.generateSmsCode((int) nrOfDigits);

      storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms
      if (useMock || KeycloakSmsAuthenticatorUtil.sendSmsCode(mobileNumber, code, context.getAuthenticatorConfig(), context.getSession(), context.getRealm(), context.getUser())) {
        return true;
      } else {
        return false;
      }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.debug("action called ... context = " + context);
        
        boolean isReset = StringUtils.isNotEmpty( context.getHttpRequest().getDecodedFormParameters().getFirst("reset_credentials") );
        String backupCode = context.getHttpRequest().getDecodedFormParameters().getFirst(KeycloakSmsConstants.ANSW_BACKUP_CODE);
        
        Response challenge = null;
        
        if (isReset) {
          if (StringUtils.isNotEmpty(backupCode)) {
            CODE_STATUS status = validateBackupCode(context, backupCode);
            switch (status) {
              case VALID:
                // remove mobile number from user attributes and trigger action
                context.getUser().removeAttribute(KeycloakSmsConstants.ATTR_MOBILE);
                context.getUser().addRequiredAction(KeycloakSmsMobilenumberRequiredAction.PROVIDER_ID);
                context.success();
                break;
              case INVALID:  
                challenge = context.form()
                  .setError("sms-auth.backup-code.invalid")
                  .createForm("sms-backup-validation.ftl");
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                break;
              case EXPIRED:  
                challenge = context.form()
                  .setError("sms-auth.backup-code.expired")
                  .createForm("sms-backup-validation.ftl");
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
                break;
            }
          } else {
            // go to backup code validation page
            challenge = context.form().createForm("sms-backup-validation.ftl");
            context.challenge(challenge);
          }
          return;
        } 
        
        CODE_STATUS status = validateCode(context);
        
        switch (status) {
            case EXPIRED:
                // resend the token
                if (sendSMSToken(context)) {
                  challenge = context.form()
                      .setError("sms-auth.code.expired")
                      .createForm("sms-validation.ftl");
                  context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
                } else {
                  challenge = context.form()
                      .setError("sms-auth.not.send")
                      .createForm("sms-validation-error.ftl");
                  context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
                }
                break;

            case INVALID:
                if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.OPTIONAL ||
                        context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.ALTERNATIVE) {
                    logger.debug("Calling context.attempted()");
                    context.attempted();
                } else if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    challenge = context.form()
                            .setError("sms-auth.code.invalid")
                            .createForm("sms-validation.ftl");
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                } else {
                    // Something strange happened
                    logger.warn("Undefined execution ...");
                }
                break;

            case VALID:
                context.success();
                updateVerifiedMobilenumber(context);
                // remove stored SMS code
                storeSMSCode(context, null, null);
                break;

        }
    }

    /**
     * If necessary update verified mobilenumber
     * @param context
     */
    private void updateVerifiedMobilenumber(AuthenticationFlowContext context){
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();
        boolean onlyForVerification=KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);

        if(onlyForVerification){
            //Only verification mode
            List<String> mobileNumberCreds = user.getAttribute(KeycloakSmsConstants.ATTR_MOBILE);
            if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
                user.setAttribute(KeycloakSmsConstants.ATTR_MOBILE_VERIFIED,mobileNumberCreds);
            }
        }
    }

    // Store the code + expiration time in a UserCredential. Keycloak will persist these in the DB.
    // When the code is validated on another node (in a clustered environment) the other nodes have access to it's values too.
    private void storeSMSCode(AuthenticationFlowContext context, String code, Long expiringAt) {
        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        credentials.setValue(code);

        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);

        KeycloakSmsAuthenticatorUtil.writeUserAttribute(context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME, expiringAt == null ? null : Long.toString(expiringAt));
        //credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);
        //credentials.setValue(expiringAt == null ? null : Long.toString(expiringAt));
        //context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);
    }
    
    private String getStoredSMSCode(AuthenticationFlowContext context) {
      List codeCreds = context.getSession().userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
      if (codeCreds.isEmpty()) {
        return null;
      }
      CredentialModel expectedCode = (CredentialModel) codeCreds.get(0);
      return expectedCode.getValue();
    }
    
    private Long getStoredSMSCodeExpireTime(AuthenticationFlowContext context) {
//      List timeCreds = context.getSession().userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);
//      if (timeCreds.isEmpty()) {
//        return null;
//      }
//      String expTimeString = ((CredentialModel) timeCreds.get(0)).getValue();
      String expTimeString = KeycloakSmsAuthenticatorUtil.getUserAttribute(context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);
      if (StringUtils.isNotBlank(expTimeString)) {
        try {
          return Long.parseLong(expTimeString);
        } catch (NumberFormatException e) {
          logger.warn("Value stored as "+KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME+" is not a number");
        }
      }
      return null;
    }


    
    protected CODE_STATUS validateBackupCode(AuthenticationFlowContext context, String backupCode) {
      String backupCodeStored =  KeycloakSmsAuthenticatorUtil.getAttributeValue(context.getUser(), KeycloakSmsConstants.ATTR_BACKUP_CODE);
      if (StringUtils.isBlank(backupCodeStored)) {
        return CODE_STATUS.EXPIRED;
      }
      if (StringUtils.equals(backupCodeStored, backupCode)) {
        return CODE_STATUS.VALID;
      }
      return CODE_STATUS.INVALID;
    }

    protected CODE_STATUS validateCode(AuthenticationFlowContext context) {
        CODE_STATUS result = CODE_STATUS.INVALID;

        logger.debug("validateCode called ... ");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredCode = formData.getFirst(KeycloakSmsConstants.ANSW_SMS_CODE);

        String expectedCode = getStoredSMSCode(context);
        Long expireTime = getStoredSMSCodeExpireTime(context); 

        logger.debug("Expected code = " + expectedCode + "    entered code = " + enteredCode);

        if (expectedCode != null) {
            result = enteredCode.equals(expectedCode) ? CODE_STATUS.VALID : CODE_STATUS.INVALID;
            if (expireTime != null) {
              long now = new Date().getTime();
              logger.debug("Valid code expires in " + (expireTime - now) + " ms");
              if (result == CODE_STATUS.VALID) {
                  if (expireTime < now) {
                      logger.debug("Code is expired !!");
                      result = CODE_STATUS.EXPIRED;
                  }
              }
            }
        }
        logger.debug("result : " + result);
        return result;
    }
    
    @Override
    public boolean requiresUser() {
        logger.debug("requiresUser called ... returning true");
        return true;
    }
    
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("configuredFor called ... session=" + session + ", realm=" + realm + ", user=" + user);
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("setRequiredActions called ... session=" + session + ", realm=" + realm + ", user=" + user);
    }
    @Override
    public void close() {
        logger.debug("close called ...");
    }

}
