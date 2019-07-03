package six.six.keycloak;

/**
 * Created by joris on 18/11/2016.
 */
public class KeycloakSmsConstants {
    public static final String ATTR_MOBILE = "mobile_number";
    public static final String ATTR_MOBILE_TMP = "mobile_number_tmp";
    public static final String ATTR_MOBILE_VERIFIED = "mobile_number_verified";
    public static final String ATTR_CODE_CONFIRMED = "sms_code_confirmed";
    public static final String ATTR_BACKUP_CODE = "sms2fa_backup_code";
    public static final String VERIFY_MOBILENUMBER_KEY = "VERIFY_MOBILENUMBER_KEY";
    public static final String ANSW_SMS_CODE = "smsCode";

    public static final String CONF_PRP_SMS_CODE_TTL = "sms-auth.code.ttl";
    public static final String CONF_PRP_SMS_CODE_LENGTH = "sms-auth.code.length";

    // Gateway
    public static final String CONF_PRP_SMS_GATEWAY = "sms-auth.sms.gateway";
    public static final String CONF_PRP_SMS_GATEWAY_ENDPOINT = "sms-auth.sms.gateway.endpoint";

    // User/Credential
    public static final String CONF_PRP_SMS_CLIENTTOKEN = "sms-auth.sms.clienttoken";
    public static final String CONF_PRP_SMS_CLIENTSECRET = "sms-auth.sms.clientsecret";

    // User credentials (used to persist the sent sms code + expiration time cluster wide)
    public static final String USR_CRED_MDL_SMS_CODE = "sms-auth.code";
    public static final String USR_CRED_MDL_SMS_EXP_TIME = "sms-auth.exp-time";
    
    // phone number regexp
    public static final String CONF_PRP_SMS_MOBILE_REGEXP = "sms-auth.mobile_number.regexp";
    
    // Mock config
    public static final String CONF_PRP_SMSM_USE_MOCK = "sms-auth.code.use-mock"; 
    public static final String CONF_PRP_SMSM_MOCK_CODE = "sms-auth.code.mock-code"; 
    
    // backup 2fa code
    public static final String CONF_PRP_SMSM_USE_2FA_BACKUP = "sms.auth.code.use-2fa-backup";
    public static final String CONF_PRP_SMS_BACKUP_CODE_LENGTH = "sms-auth.code-2fa-backup.length";
    
    // Messages
    public static final String MSG_SMS_TEXT = "sms-auth.msg.text";
    public static final String MSG_2FA_BACKUP_TEXT = "sms-auth.msg.text-2fa-backup";
    
    public static final String MSG_MOBILE_REGEXP = "mobile_number.regexp.validation";
    public static final String MSG_MOBILE_PREFIX_DEFAULT = "mobile_number.prefix.default";
    public static final String MSG_MOBILE_PREFIX_CONDITION = "mobile_number.prefix.condition";
    /*
    mobile_number.prefix.default=+44
    mobile_number.prefix.condition=07
    mobile_number.regexp.validation=^(?:(?:\\(?(?:0(?:0|11)\\)?[\\s-]?\\(?|\\+)44\\)?[\\s-]?(?:\\(?0\\)?[\\s-]?)?)|(?:\\(?0))(?:(?:\\d{5}\\)?[\\s-]?\\d{4,5})|(?:\\d{4}\\)?[\\s-]?(?:\\d{5}|\\d{3}[\\s-]?\\d{3}))|(?:\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{3,4})|(?:\\d{2}\\)?[\\s-]?\\d{4}[\\s-]?\\d{4}))(?:[\\s-]?(?:x|ext\\.?|\\#)\\d{3,4})?$
     */

    // Proxy
    public static final String PROXY_ENABLED = "proxy_enabled";
    public static final String PROXY_HOST= "proxyHost";
    public static final String PROXY_PORT= "proxyPort";

    // Verification
    public static final String MOBILE_VERIFICATION_ENABLED = "mobile_verification_enabled";
    public static final String MOBILE_ASKFOR_ENABLED = "mobile_askfor_enabled";
}
