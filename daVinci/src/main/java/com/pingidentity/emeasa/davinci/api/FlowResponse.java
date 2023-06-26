package com.pingidentity.emeasa.davinci.api;

import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FlowResponse {

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setData(JSONObject additionalParameters) throws JSONException {
        for (Iterator<String> it = additionalParameters.keys(); it.hasNext(); ) {
            String key = it.next();
            dataFields.put(key, additionalParameters.getString(key));
        }
    }

    public boolean hasDataFields() {
        return (this.dataFields != null && !this.dataFields.isEmpty());
    }
    public boolean hasDataField(String fieldName)  {
        return (this.dataFields != null && this.dataFields.containsKey(fieldName));
    }

    public String getDataFieldValue(String fieldName) {
       if (this.hasDataField(fieldName)) {
           return this.dataFields.get(fieldName);
       } else {
           return null;
       }
    }


    private String accessToken;
    private String idToken;
    private boolean success;
    private Map<String, String> dataFields = new HashMap<>();

    public UserSession getUserSession() {
        String stVal = null;
        if (this.hasDataField("sessionToken")) {
            stVal = this.getDataFieldValue("sessionToken");
        }
        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();
        try {
            JwtContext jwtContext = firstPassJwtConsumer.process(this.getIdToken());
            String userID = jwtContext.getJwtClaims().getSubject();
            UserSession us = new UserSession(userID,  jwtContext.getJwtClaims().getClaimsMap(), stVal);
            return us;
        } catch (InvalidJwtException | MalformedClaimException e) {
            e.printStackTrace();
        }
        return null;

    }
}
