package com.pingidentity.emeasa.davinci.api;

import java.util.Map;

public class UserSession {

    public UserSession(String userID, Map<String, Object> userClaims, String sessionTokenValue) {
        this.mUserID = userID;
        this.mUserClaims = userClaims;
        this.mSessionTokenValue = sessionTokenValue;
    }
    public String getUserID() {
        return mUserID;
    }

    public void setUserID(String userID) {
        this.mUserID = userID;
    }


    public Map<String, Object> getUserClaims() {
        return mUserClaims;
    }

    public void setUserClaims(Map<String, Object> userClaims) {
        this.mUserClaims = userClaims;
    }

    public String getSessionTokenValue() {
        return mSessionTokenValue;
    }

    public void setSessionTokenValue(String sessionTokenValue) {
        this.mSessionTokenValue = sessionTokenValue;
    }

    public String getUserClaimAsString(String claim) {
        if (mUserClaims.containsKey(claim))
            return (String) mUserClaims.get(claim);
        else
            return null;
    }

    public Object getUserClaim(String claim) {
        if (mUserClaims.containsKey(claim))
            return mUserClaims.get(claim);
        else
            return null;
    }

    private String mUserID;
    private Map<String, Object> mUserClaims;
    private String mSessionTokenValue;

}
