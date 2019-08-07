# keycloak-sms-authenticator-sns

To install the SMS Authenticator one has to:

* Add the jar to the Keycloak server:
  * `$ cp target/keycloak-sms-authenticator-sns-*.jar _KEYCLOAK_HOME_/standalone/deployments/`

* Add three templates to the Keycloak server:
  * `$ cp templates/*.ftl _KEYCLOAK_HOME_/themes/base/login/`

* Append the additional template messages to the Keycloak base template (xx stays for language code):
  * `$ cat templates/messages/messages_xx.properties >> _KEYCLOAK_HOME_/themes/base/login/messages/messages_xx.properties`

Configure your REALM to use the SMS Authentication.
First create a new REALM (or select a previously created REALM).

Under Authentication > Flows:
* Copy 'Browse' flow to 'Browser with SMS' flow
* Click on 'Actions > Add execution on the 'Browser with SMS Forms' line and add the 'SMS Authentication'
* Set 'SMS Authentication' to 'REQUIRED' or 'ALTERNATIVE'
* To configure the SMS Authenticator, click on Actions  Config and fill in the attributes.
* Alias must be set to 'sms-authenticator', because of limitations of Keycloak (required action has no custom configuration, it must rely on fixed id)
* You can use mock instead of sending real code.
* You can define regex to validate phone number, for example use regex '^(\+|0*){0,1}41((-|\s){0,1}[0-9]+)+[0-9]$' to allow only Swiss phone numbers.

Under Authentication > Bindings:
* Select 'Browser with SMS' as the 'Browser Flow' for the REALM.

Under Authentication > Required Actions:
* Click on Register and select 'SMS Authentication' to add the Required Action to the REALM.
* Make sure that for the 'SMS Authentication' both the 'Enabled' and 'Default Action' check boxes are checked.
* Click on Register and select 'Mobile Number' to add the Required Action to the REALM.
* Make sure that for the 'Mobile Number' both the 'Enabled' and 'Default Action' check boxes are checked.

Malys contributions (for [Lyra Network](https://www.lyra-network.com/))
* Internationalization support
* Vault, Java properties, environment variables parameters support
* Lyrasms gateway support
* Add mobilephone number verification
* Add input mobile phone number on authenticator
* Refactoring
* Template cleaning
* Documentation
