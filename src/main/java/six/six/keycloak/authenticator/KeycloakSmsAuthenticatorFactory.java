package six.six.keycloak.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import six.six.gateway.Gateways;
import six.six.keycloak.KeycloakSmsConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * SMS validation Input
 * Created by joris on 11/11/2016.
 */
public class KeycloakSmsAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "sms-authentication";

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticatorFactory.class);
    private static final KeycloakSmsAuthenticator SINGLETON = new KeycloakSmsAuthenticator();


    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.OPTIONAL,
            AuthenticationExecutionModel.Requirement.DISABLED};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;

        // SMS Code
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_CODE_TTL);
        property.setLabel("SMS code time to live");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("The validity of the sent code in seconds.");
        property.setDefaultValue(60*5);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_CODE_LENGTH);
        property.setLabel("SMS code length");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Length of the SMS code.");
        property.setDefaultValue(6);
        configProperties.add(property);
        
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_MOBILE_REGEXP);
        property.setLabel("Phone number regex");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Regular expression for phone number validation.");
        configProperties.add(property);
        
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_MOBILE_PREFIX_DEFAULT);
        property.setLabel("Default country prefix");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Default country prefix to apply for numbers matching condition");
        configProperties.add(property);
        
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_MOBILE_PREFIX_CONDITION);
        property.setLabel("Condition for adding default country prefix");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(KeycloakSmsConstants.DEFVALUE_CONF_PRP_SMS_MOBILE_PREFIX_CONDITION);
        property.setHelpText("Condition in form of regular expression for adding default country prefix. Defaults to '"
            + KeycloakSmsConstants.DEFVALUE_CONF_PRP_SMS_MOBILE_PREFIX_CONDITION + "' (match number starting with single zero)");
        configProperties.add(property);

        // SMS gateway
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_GATEWAY);
        property.setLabel("SMS gateway");
        property.setHelpText("Select SMS gateway");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setDefaultValue(Gateways.AMAZON_SNS);
        property.setOptions(Stream.of(Gateways.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
        configProperties.add(property);

        // SMS Endpoint
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_GATEWAY_ENDPOINT);
        property.setLabel("SMS endpoint");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Not useful for AWS SNS.");
        configProperties.add(property);

        // Credential
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_CLIENTTOKEN);
        property.setLabel("Client id");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("AWS Client Token or LyraSMS User");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_CLIENTSECRET);
        property.setLabel("Client secret");
        property.setHelpText("AWS Client Secret or LyraSMS Password");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        
        // mock code (only for mock provider)
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMSM_USE_MOCK);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setLabel("Use mock");
        property.setHelpText("Use mock and defined code, instead of sending message");
        configProperties.add(property);
        
        
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMSM_MOCK_CODE);
        property.setLabel("Mock code");
        property.setHelpText("Mock code to use when 'Use mock' is set to true");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMSM_USE_2FA_BACKUP);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setLabel("Use 2fa backup code");
        property.setHelpText("Create and save 2fa backup code needed to reset the phone number (currently only through support)");
        configProperties.add(property);
        
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.CONF_PRP_SMS_BACKUP_CODE_LENGTH);
        property.setLabel("2FA backup code length");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Length of the 2fa backup code.");
        property.setDefaultValue(10);
        configProperties.add(property);

        // Proxy
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.PROXY_ENABLED);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setLabel("Use Proxy");
        property.setHelpText("Add Java Properties: http(s).proxyHost,http(s).proxyPort");
        configProperties.add(property);

        //First time verification
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.MOBILE_VERIFICATION_ENABLED);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setLabel("Verify mobilephone\nnumber ONLY");
        property.setHelpText("Send SMS code ONLY to verify mobile number (add or update)");
        configProperties.add(property);

        //Ask for mobile if not defined
        property = new ProviderConfigProperty();
        property.setName(KeycloakSmsConstants.MOBILE_ASKFOR_ENABLED);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setLabel("Ask for mobile number");
        property.setHelpText("Enable access and ask for mobilenumber if it isn't defined");
        configProperties.add(property);


    }

    public String getId() {
        logger.debug("getId called ... returning " + PROVIDER_ID);
        return PROVIDER_ID;
    }

    public Authenticator create(KeycloakSession session) {
        logger.debug("create called ... returning " + SINGLETON);
        return SINGLETON;
    }


    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        logger.debug("getRequirementChoices called ... returning " + REQUIREMENT_CHOICES);
        return REQUIREMENT_CHOICES;
    }

    public boolean isUserSetupAllowed() {
        logger.debug("isUserSetupAllowed called ... returning true");
        return true;
    }

    public boolean isConfigurable() {
        logger.debug("isConfigurable called ... returning true");
        return true;
    }

    public String getHelpText() {
        logger.debug("getHelpText called ...");
        return "Validates an OTP sent by SMS.";
    }

    public String getDisplayType() {
        String result = "SMS Authentication";
        logger.debug("getDisplayType called ... returning " + result);
        return result;
    }

    public String getReferenceCategory() {
        logger.debug("getReferenceCategory called ... returning sms-auth-code");
        return "sms-auth-code";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        logger.debug("getConfigProperties called ... returning " + configProperties);
        return configProperties;
    }

    public void init(Config.Scope config) {
        logger.debug("init called ... config.scope = " + config);
    }

    public void postInit(KeycloakSessionFactory factory) {
        logger.debug("postInit called ... factory = " + factory);
    }

    public void close() {
        logger.debug("close called ...");
    }
}
