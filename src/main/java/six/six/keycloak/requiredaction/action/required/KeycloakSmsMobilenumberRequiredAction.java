package six.six.keycloak.requiredaction.action.required;

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
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

        if (mobileNumber != null && validateTelephoneNumber(mobileNumber, KeycloakSmsAuthenticatorUtil.getMessage(context, KeycloakSmsConstants.MSG_MOBILE_REGEXP))) {
            // Mobile number is configured
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
      /*
       * TODO
       * split to 2 steps / templates
       * 1st screen - type phone number, button 'send code'
       * the SMS is sent
       * 2nd screen - type validation code, button 'verify', 'resend code' and 'change phone number'
       * success after verify
       */
      
        logger.debug("processAction called ...");
        
        // check what action was called
        boolean isConfirmPhoneNo = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("submit_phone_no")) );
        boolean isConfirmSmsCode = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("submit_code")) );
        boolean isEditPhoneNo = StringUtils.isNotEmpty( (context.getHttpRequest().getDecodedFormParameters().getFirst("change_phone_no")) );
        // values
        String phoneNo = (context.getHttpRequest().getDecodedFormParameters().getFirst("mobile_number"));
        String smsCode = (context.getHttpRequest().getDecodedFormParameters().getFirst("sms_code_confirm"));

        KeycloakSession session = context.getSession();
        List codeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsConstants.USR_CRED_MDL_SMS_CODE);
        String expectedCode = null;
        if (!codeCreds.isEmpty()) {
          CredentialModel creds = (CredentialModel) codeCreds.get(0);
          expectedCode = creds.getValue();
        }
        
        if (isConfirmPhoneNo) {
          // first validate phone number format, if correct try to send SMS
          boolean phoneNoValid = phoneNo != null && phoneNo.length() > 0 && validateTelephoneNumber(phoneNo,KeycloakSmsAuthenticatorUtil.getMessage(context, KeycloakSmsConstants.MSG_MOBILE_REGEXP));
          if (phoneNoValid) {
            List<String> mobileNumber = new ArrayList<String>();
            mobileNumber.add(phoneNo);
            UserModel user = context.getUser();
            user.setAttribute(KeycloakSmsConstants.ATTR_MOBILE, mobileNumber);
            
            // generate and send SMS
            AuthenticatorConfigModel config = context.getAuthenticatorConfig();
            long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
            logger.debug("Using nrOfDigits " + nrOfDigits);
            long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s
            logger.debug("Using ttl " + ttl + " (s)");
            String code = KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);
            //String code = "123456";
            storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms
            
            // goto sms-validation-code
            Response challenge = context.form()
                .createForm("sms-validation-code.ftl");
            context.challenge(challenge);

          } else {
            Response challenge = context.form()
                .setError("mobile_number.no.valid")
                .createForm("sms-validation-mobile-number.ftl");
            context.challenge(challenge);
          }
        }
        
        String answer = (context.getHttpRequest().getDecodedFormParameters().getFirst("mobile_number"));
        String answer2 = (context.getHttpRequest().getDecodedFormParameters().getFirst("mobile_number_confirm"));
        String isResend = (context.getHttpRequest().getDecodedFormParameters().getFirst("resend"));
        String isLogin = (context.getHttpRequest().getDecodedFormParameters().getFirst("login"));
        
        logger.info("Expected code = " + expectedCode + "    entered code = " + smsCode);
        
        // TODO support resending SMS
        logger.info("isResend: "+isResend+", isLogin: "+isLogin);
        if (answer != null && answer.length() > 0 && answer.equals(answer2) && validateTelephoneNumber(answer,KeycloakSmsAuthenticatorUtil.getMessage(context, KeycloakSmsConstants.MSG_MOBILE_REGEXP))) {
            logger.debug("Valid matching mobile numbers supplied, save credential ...");
            List<String> mobileNumber = new ArrayList<String>();
            mobileNumber.add(answer);

            UserModel user = context.getUser();
            user.setAttribute(KeycloakSmsConstants.ATTR_MOBILE, mobileNumber);

            context.success();
        } else if (answer != null && answer2 !=null && !answer.equals(answer2)) {
            logger.debug("Supplied mobile number values do not match...");
            Response challenge = context.form()
                    .setError("mobile_number.no.match")
                    .createForm("sms-validation-mobile-number.ftl");
            context.challenge(challenge);
        } else {
            logger.debug("Either one of two fields wasn\'t complete, or the first contains an invalid number...");
            Response challenge = context.form()
                    .setError("mobile_number.no.valid")
                    .createForm("sms-validation-mobile-number.ftl");
            context.challenge(challenge);
        }
    }

    public void close() {
        logger.debug("close called ...");
    }
}
