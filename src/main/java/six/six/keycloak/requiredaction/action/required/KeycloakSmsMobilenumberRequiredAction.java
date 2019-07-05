package six.six.keycloak.requiredaction.action.required;

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import six.six.keycloak.KeycloakSmsConstants;
import six.six.keycloak.authenticator.KeycloakSmsAuthenticatorUtil;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static six.six.keycloak.authenticator.KeycloakSmsAuthenticatorUtil.validateTelephoneNumber;

/**
 * Created by nickpack on 15/08/2017.
 */
public class KeycloakSmsMobilenumberRequiredAction implements RequiredActionProvider {
    private static Logger logger = Logger.getLogger(KeycloakSmsMobilenumberRequiredAction.class);
    public static final String PROVIDER_ID = "sms_auth_check_mobile";

    public void evaluateTriggers(RequiredActionContext context) {
        logger.debug("evaluateTriggers called ...");
    }



    public void requiredActionChallenge(RequiredActionContext context) {
        logger.debug("requiredActionChallenge called ...");

        UserModel user = context.getUser();

        List<String> mobileNumberCreds = user.getAttribute(KeycloakSmsConstants.ATTR_MOBILE);

        String mobileNumber = null;

        if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
            mobileNumber = mobileNumberCreds.get(0);
        }

        AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("sms-authenticator");
        String phoneNoRegexp = KeycloakSmsAuthenticatorUtil.getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMS_MOBILE_REGEXP);
        if (StringUtils.isNotBlank(mobileNumber) && validateTelephoneNumber(mobileNumber, phoneNoRegexp)) {
            // Mobile number is configured
            logger.warn("Ignored mobile number configuration, because mobile number is already configured");
            context.ignore();
        } else {
            // Mobile number is not configured or is invalid
            Response challenge = context.form().createForm("sms-validation-mobile-number.ftl");
            context.challenge(challenge);
        }
    }
    
    // Store the code + expiration time in a UserCredential. Keycloak will persist these in the DB.
    // When the code is validated on another node (in a clustered environment) the other nodes have access to it's values too.
    private void storeSMSCode(RequiredActionContext context, String code, Long expiringAt) {
        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        credentials.setValue(code);

        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);

        credentials.setType(KeycloakSmsConstants.USR_CRED_MDL_SMS_EXP_TIME);
        credentials.setValue((expiringAt).toString());
        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);
    }

    public void processAction(RequiredActionContext context) {
      
        logger.debug("processAction called ...");
        
        // check what action was called
        boolean isConfirmPhoneNo = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("submit_phone_no")) );
        boolean isConfirmSmsCode = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("submit_code")) );
        boolean isEditPhoneNo = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("change_phone_no")) );
        boolean isLoginReady = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("login_ready")) );
        // values
        String phoneNo = (context.getHttpRequest().getDecodedFormParameters().getFirst("mobile_number"));
        String smsCode = (context.getHttpRequest().getDecodedFormParameters().getFirst("sms_code_confirm"));
        
        // module configuration
        AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("sms-authenticator");
        boolean isUse2faBackup = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.CONF_PRP_SMSM_USE_2FA_BACKUP);
        boolean useMock = KeycloakSmsAuthenticatorUtil.getConfigBoolean(config, KeycloakSmsConstants.CONF_PRP_SMSM_USE_MOCK, false);

        KeycloakSession session = context.getSession();
        List codeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        String expectedCode = null;
        if (!codeCreds.isEmpty()) {
          CredentialModel creds = (CredentialModel) codeCreds.get(0);
          expectedCode = creds.getValue();
        }
        
        if (isLoginReady) {
          String codeConfirmed = getUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_CODE_CONFIRMED);
          if ("true".equals(codeConfirmed)) {
            context.getUser().removeAttribute(KeycloakSmsConstants.ATTR_CODE_CONFIRMED);
            context.success();
          } else {
            // error
            logger.warn("Flow error: login_ready step active, but sms-code-confirmed is "+codeConfirmed);
            context.failure();
          }
        } else if (isEditPhoneNo) {
          // back to sms-validation-mobile-number
          Response challenge = context.form().createForm("sms-validation-mobile-number.ftl");
          context.challenge(challenge);
        } else if (isConfirmSmsCode) {
          // verify the sms code
          logger.info("Expected code = " + expectedCode + "    entered code = " + smsCode);
          if (StringUtils.equals(expectedCode, smsCode)) {
            // write information that code is confirmed (to prevent manipulation from client side)
            writeUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_CODE_CONFIRMED, "true");
             // save temporary phone numer as real phone number
            String tmpPhoneNo = getUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_MOBILE_TMP);
            writeUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_MOBILE, tmpPhoneNo);
            context.getUser().removeAttribute(KeycloakSmsConstants.ATTR_MOBILE_TMP);
            
            if (isUse2faBackup) {
              long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_BACKUP_CODE_LENGTH, 10L);
              String code = KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);
              writeUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_BACKUP_CODE, code);
              // TODO show page with the code
              boolean isSent = useMock || KeycloakSmsAuthenticatorUtil.sendBackupCode(tmpPhoneNo, code, config, context.getSession(), context.getRealm(), context.getUser());
              if (!isSent) {
                logger.warn("Failed to send SMS with backup code to phone number "+tmpPhoneNo);
              }
              Response challenge = context.form()
                  .setAttribute("backup_code", code)
                  .createForm("sms-show-backup-code.ftl");
              context.challenge(challenge);
            } else {
              context.success();
            }
          } else {
            // wrong code
            String tmpPhoneNo = getUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_MOBILE_TMP);
            Response challenge = context.form()
                .setAttribute("mobile_number", tmpPhoneNo)
                .setError("sms_code.no.valid")
                .createForm("sms-validation-code.ftl");
            context.challenge(challenge);
          }
        } else {
          String phoneNoRegexp = KeycloakSmsAuthenticatorUtil.getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMS_MOBILE_REGEXP);
          // default action - when entering the flow
          // first validate phone number format, if correct try to send SMS
          boolean phoneNoValid = phoneNo != null && phoneNo.length() > 0 && validateTelephoneNumber(phoneNo,phoneNoRegexp);
          if (phoneNoValid) {
            // temporary storage
            writeUserAttribute(context.getUser(), KeycloakSmsConstants.ATTR_MOBILE_TMP, phoneNo);
            
            // generate and send SMS
            long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
            logger.debug("Using nrOfDigits " + nrOfDigits);
            long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s
            logger.debug("Using ttl " + ttl + " (s)");
            
            String mockCode = KeycloakSmsAuthenticatorUtil.getConfigString(config, KeycloakSmsConstants.CONF_PRP_SMSM_MOCK_CODE, "123456");
            String code = useMock ? mockCode : KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);
            
            storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms
            
            if (useMock || KeycloakSmsAuthenticatorUtil.sendSmsCode(phoneNo, code, config, context.getSession(), context.getRealm(), context.getUser())) {
              // goto sms-validation-code
              Response challenge = context.form()
                  .setAttribute("mobile_number", phoneNo)
                  .createForm("sms-validation-code.ftl");
              context.challenge(challenge);
            } else {
                Response challenge = context.form()
                        .setError("sms-auth.not.send")
                        .createForm("sms-validation-mobile-number.ftl");
                context.challenge(challenge);
            }

          } else {
            Response challenge = context.form()
                .setError("mobile_number.no.valid")
                .createForm("sms-validation-mobile-number.ftl");
            context.challenge(challenge);
          }
        }

    }
    
    private void writeUserAttribute(UserModel user, String attribute, String value) {
      List<String> list = new ArrayList<String>();
      list.add(value);
      // temporary storage
      user.setAttribute(attribute, list);
    }
    
    private String getUserAttribute(UserModel user, String attribute){
      List<String> list = user.getAttribute(attribute);

      String value = null;
      if (list != null && !list.isEmpty()) {
          value = list.get(0);
      }

      return  value;
  }

    public void close() {
        logger.debug("close called ...");
    }
}
