package org.atsign.client.cli;

import java.util.Map;
import java.util.Scanner;

import org.atsign.client.util.RegisterUtil;
import org.atsign.common.AtSign;
import org.atsign.config.ConfigReader;

/**
 * Command line interface to claim a free atsign. Requires one-time-password
 * received on the provided email to validate.
 * Registers the free atsign to provided email
 */
public class Register {
    public static void main(String[] args) throws Exception {

        String apiKey = "";
        String email = "";
        boolean isRegistrarV3 = false;

        if (args.length == 2 && args[0].equals("-e")) {
            email = args[1];
        } else if (args.length == 2 && args[0].equals("-k")) {
            apiKey = args[1];
            isRegistrarV3 = true;
        } else if (args.length == 1 && args[0].contains("@")) {
            email = args[0];
            System.out.println(
                    "You are using an older version of providing args. Consider using the new syntax: \"Register -e <email@email.com>\"");
        } else {
            System.err.println(
                    "Usage: Register -e <email@email.com> (or)\nRegister -k <Your API Key>\nNOTE: Use email if you prefer activating using OTP. Go for API key option if you have your own API key. You cannot use both.");
            System.exit(1);
        }

        ConfigReader configReader = new ConfigReader();
        RegisterUtil registerUtil = new RegisterUtil();
        String cramSecret;
        String rootDomain;
        String rootPort;
        String registrarUrl;
        String otp;
        String validationResponse;
        AtSign atsign;

        rootDomain = configReader.getProperty("rootServer", "domain");
        if (rootDomain == null) {
            // reading config from older configuration syntax for backwards compatability
            rootDomain = configReader.getProperty("ROOT_DOMAIN");
        }

        rootPort = configReader.getProperty("rootServer", "port");
        if (rootPort == null) {
            // reading config from older configuration syntax for backwards compatability
            rootPort = configReader.getProperty("ROOT_PORT");
        }

        registrarUrl = isRegistrarV3 ? configReader.getProperty("registrarV3", "url")
                : configReader.getProperty("registrar", "url");
        if (registrarUrl == null) {
            // reading config from older configuration syntax for backwards compatability
            registrarUrl = configReader.getProperty("REGISTRAR_URL");
        }

        if (apiKey == null) {
            try {
                apiKey = configReader.getProperty("registrar", "apiKey");
            } catch (Exception e) {
                // reading config from older configuration syntax for backwards compatability
                apiKey = configReader.getProperty("API_KEY");
            }
        }

        if (rootDomain == null || rootPort == null || registrarUrl == null || apiKey == null) {
            System.err.println(
                    "Please make sure to set all relevant configuration in src/main/resources/config.yaml");
            System.exit(1);
        }

        if (!isRegistrarV3) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Getting free atsign");
            atsign = new AtSign(registerUtil.getFreeAtsign(registrarUrl, apiKey));
            System.out.println("Got atsign: " + atsign);

            System.out.println("Sending one-time-password to :" + email);
            if (registerUtil.registerAtsign(email, atsign, registrarUrl, apiKey)) {
                System.out.println("Enter OTP received on: " + email);

                otp = scanner.nextLine();
                System.out.println("Validating one-time-password");
                validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, false);
                // if validationResponse is retry, the OTP entered is incorrect. Ask user to
                // re-enter correct OTP
                if ("retry".equals(validationResponse)) {
                    while ("retry".equals(validationResponse)) {
                        System.out.println("Incorrect OTP entered. Re-enter the OTP: ");
                        otp = scanner.nextLine();
                        validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, false);
                    }
                    scanner.close();
                }
                // if validationResponse is follow-up, the atsign has been regstered to email.
                // Again call the API with "confirmation"=true to get the cram key
                if ("follow-up".equals(validationResponse)) {
                    validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, true);
                }
                // if validation response starts with @, that represents that validationResponse
                // contains cram
                if (validationResponse.startsWith("@")) {
                    System.out.println("One-time-password verified. OK");
                    // extract cram from response
                    cramSecret = validationResponse.split(":")[1];
                    System.out.println("Got cram secret for " + atsign + ": " + cramSecret);

                    String[] onboardArgs = new String[] { rootDomain + ":" + rootPort,
                            atsign.toString(), cramSecret };
                    Onboard.main(onboardArgs);
                } else {
                    System.err.println(validationResponse);
                }
            } else {
                System.err.println("Error while sending OTP. Please retry the process");
            }
        } else {
            String activationKey;
            Map<AtSign, String> responseMap;
            Map.Entry<AtSign, String> mapEntry;
            System.out.println("Getting AtSign...");
            responseMap = registerUtil.getAtsignV3(registrarUrl, apiKey);
            mapEntry = responseMap.entrySet().iterator().next();
            atsign = mapEntry.getKey();
            System.out.println("Got AtSign: " + atsign);
            activationKey = mapEntry.getValue();
            System.out.println("Activating atsign using activationKey...");
            cramSecret = registerUtil.activateAtsign(registrarUrl, apiKey, atsign, activationKey);
            cramSecret = cramSecret.split(":")[1];
            System.out.println("Your cramSecret is: " + cramSecret);
            System.out.println("Do you want to activate the atsign? [y/n] " + atsign);
            Scanner scanner = new Scanner(System.in);
            if (scanner.next() == "y") {
                String[] onboardArgs = new String[] { rootDomain + ":" + rootPort,
                        atsign.toString(), cramSecret };
                Onboard.main(onboardArgs);
            }
            scanner.close();
            System.out.println("Done.");
        }
    }
}
