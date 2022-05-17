package org.atsign.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.atsign.common.AtException;
import org.atsign.common.AtSign;

public class RegisterUtil {
    /**
     * Calls API to get atsigns which are ready to be claimed.
     * Returns a free atsign.
     * 
     * @param registrarUrl
     * @return
     * @throws AtException
     * @throws MalformedURLException
     * @throws IOException
     */

    public String getFreeAtsign(String registrarUrl, String apiKey) throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(registrarUrl + Constants.GET_FREE_ATSIGN);
        HttpsURLConnection connection = (HttpsURLConnection) urlObject.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", apiKey);
        if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Map<String, String>> responseData = new HashMap<>();
            responseData = objectMapper.readValue(response.toString(), Map.class);
            Map<String, String> data = responseData.get("data");
            return data.get("atsign");
        } else {
            throw new AtException(connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }

    /**
     * Accepts email and an unpaired atsign. Method pairs free atsign with email.
     * Sends the one-time-password to the provided email.
     * Returns bool, true if OTP sent or False otherwise.
     * 
     * @param email
     * @param atsign
     * @param registrarUrl
     * @return
     * @throws AtException
     * @throws MalformedURLException
     * @throws IOException
     */
    public Boolean registerAtsign(String email, AtSign atsign, String registrarUrl, String apiKey)
            throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(registrarUrl + Constants.REGISTER_ATSIGN);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) urlObject.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", apiKey);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(params.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseData = new HashMap<>();
            responseData = objectMapper.readValue(response.toString(), Map.class);
            String data = responseData.get("message");
            System.out.println("Got response: " + data);
            if (response.toString().contains("Sent Successfully")) {
                return true;
            }
            return false;
        }
        throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
    }

    /**
     * Accepts email, unpaired atsign, and the otp received on the provided email.
     * Validates the OTP against the atsign and registers it to the provided email
     * if OTP is valid.
     * Returns the CRAM secret of the atsign which is registered.
     * 
     * @param email
     * @param atsign
     * @param otp
     * @param registrarUrl
     * @return
     * @throws IOException
     * @throws AtException
     */
    public String validateOtp(String email, AtSign atsign, String otp, String registrarUrl, String apiKey)
            throws IOException, AtException {
        URL validateOtpUrl = new URL(registrarUrl + Constants.VALIDATE_OTP);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) validateOtpUrl.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\", \"otp\":\"" + otp
                + "\", \"confirmation\":\"" + "true\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", apiKey);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(params.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseData = objectMapper.readValue(response.toString(), Map.class);
            System.out.println("Got response: " + responseData.get("message"));
            if (responseData.get("message").equals("Verified")) {
                return responseData.get("cramkey");
            } else if (responseData.get("message").contains("Try again")) {
                return "retry";
            } else {
                return responseData.get("message");
            }
        } else {
            throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

}