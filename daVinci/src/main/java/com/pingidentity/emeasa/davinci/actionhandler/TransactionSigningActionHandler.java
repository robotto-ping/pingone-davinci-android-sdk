package com.pingidentity.emeasa.davinci.actionhandler;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import android.app.Activity;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;
import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;

import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

import com.pingidentity.pingidsdkv2.types.PairingInfo;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.CryptoPrimitive;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.VerificationJwkSelector;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Executor;


public class TransactionSigningActionHandler extends PingOneMFAActionHandler implements DaVinciFlowActionHandler {

    public static final String GET_JWT_ACTION = "getSigningKeyJWT";
    public static final String GET_SIGNATURE_ACTION = "getSignedTransactionJWT";

    public static final String ANDROID_KEY_STORE_PROVIDER_NAME = "AndroidKeyStore";
    private static final String DEVICE_PUBLIC_KEY = "device_public_key";
    private static final String MOBILE_PAYLOAD = "device_payload";
    private static final String APP_KEY_NAME = "app_key_name";
    private static final String TRANSACTION_JWT = "transaction_JWT" ;

    private static final String PING_ONE_USER_ID = "pingOneUserID";
    private static final String PING_ONE_DEVICE_ID = "ping_one_device_id";
    private static final String TX_DETAILS = "transaction_details" ;
    private static final String TX_DETAILS_HASH ="transaction_details_hash" ;

    private  String keyName;

    @Override
    protected void performAction() {
        Log.d("TransactionSigningActionHandler", "Starting  performAction");
        for (Action a: continueResponse.getActions()) {
            if (a.getType().equalsIgnoreCase(GET_JWT_ACTION)) {
                startDevicePairing(a);
            }
            else if (a.getType().equalsIgnoreCase(GET_SIGNATURE_ACTION)) {
                try {
                    signTransaction(a);
                } catch (Exception e) {
                    pingOneDaVinci.handleAsyncException(e);
                }
            }
        }
    }

    private void startDevicePairing(Action action) {
        String pingOnePairingKey = (String) action.getInputData().get(PING_ONE_PAIRING_KEY);
        PingOne.pair(context, pingOnePairingKey, new PingOne.PingOneSDKPairingCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                //This is never called
            }

            @Override
            public void onComplete(@Nullable PairingInfo pairingInfo, @Nullable PingOneSDKError pingOneSDKError) {

                if (pingOneSDKError != null) {

                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(pingOneSDKError.getMessage()));
                } else {
                    ((Activity)context).runOnUiThread(() -> {
                        try {
                            PingOne.getInfo(context, new PingOne.PingOneGetInfoCallback() {
                                @Override
                                public void onComplete(@Nullable JsonObject jsonObject, @Nullable PingOneSDKError pingOneSDKError) {
                                    try {
                                        String userID = (String) action.getInputData().get(PING_ONE_USER_ID);
                                        JsonObject userEntry = jsonObject.getAsJsonArray("users").get(0).getAsJsonObject();
                                        if (userEntry.get("id").getAsString().equals(userID)) {
                                            String deviceID = userEntry.get("device").getAsJsonObject().get("id").getAsString();
                                            ((Activity)context).runOnUiThread(() -> {
                                                try {
                                                    getRegistrationToken(action, deviceID, userID);
                                                } catch (Exception e) {
                                                    pingOneDaVinci.handleAsyncException(e);
                                                }
                                            });
                                        }

                                    } catch (Exception e) {
                                        pingOneDaVinci.handleAsyncException(e);
                                    }
                                }
                            });

                        } catch (Exception e) {
                            pingOneDaVinci.handleAsyncException(e);
                        }
                    });
                }

            }

        });
    }

    private void signTransaction(Action action) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER_NAME);
        keyStore.load(null);
        String transactionJWT = (String) action.getInputData().get(TRANSACTION_JWT);
        //Get the DaVinci JWKS
        String jWKSLocation = pingOneDaVinci.getJWKSLocation();
        HttpsJwks jwks = new HttpsJwks(jWKSLocation);
        jwks.setDefaultCacheDuration(1000);
        Thread t = new Thread(() -> {
            try {
                jwks.refresh();
            } catch (Exception e) {
                pingOneDaVinci.handleAsyncException(e);
            }
        });
        t.start();
        t.join();

        //validate the signature
       JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,   AlgorithmIdentifiers.RSA_USING_SHA256));
        jws.setCompactSerialization(transactionJWT);
        VerificationJwkSelector jwkSelector = new VerificationJwkSelector();
        JsonWebKey jwk = jwkSelector.select(jws, jwks.getJsonWebKeys());
        jws.setKey(jwk.getKey());
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime() // the JWT must have an expiration time
                .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
                .setExpectedIssuer(pingOneDaVinci.getIssuer()) // whom the JWT needs to have been issued by
                .setExpectedAudience(this.getClass().getCanonicalName()) // to whom the JWT is intended for
                .setVerificationKey(jwk.getKey()) // verify the signature with the public key
                .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                        AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256) // which is only RS256 here
                .build(); // create the JwtConsumer instance

        try
        {
            //  Validate the JWT and process it to the Claims
            JwtClaims jwtClaims = jwtConsumer.processToClaims(transactionJWT);
            // Extract the Key ID  from the JWT
            this.keyName =jwtClaims.getClaimValueAsString(APP_KEY_NAME);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyName, null);
            // Extract the transaction message from the JWT
            String transaction = jwtClaims.getClaimValueAsString(TX_DETAILS);

            try {
                PingOne.getInfo(context, new PingOne.PingOneGetInfoCallback() {
                    @Override
                    public void onComplete(@Nullable JsonObject jsonObject, @Nullable PingOneSDKError pingOneSDKError) {
                        try {
                            JsonObject userEntry = jsonObject.getAsJsonArray("users").get(0).getAsJsonObject();
                            String userID = userEntry.get("id").getAsString();
                            String deviceID = userEntry.get("device").getAsJsonObject().get("id").getAsString();
                                ((Activity)context).runOnUiThread(() -> {
                                    try {
                                        getTransactionToken(action, transaction, deviceID, userID, privateKey);
                                    } catch (Exception e) {
                                        pingOneDaVinci.handleAsyncException(e);
                                    }
                                });


                        } catch (Exception e) {
                            pingOneDaVinci.handleAsyncException(e);
                        }
                    }
                });

            } catch (Exception e) {
                pingOneDaVinci.handleAsyncException(e);
            }

        } catch (Exception e ) {
            pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
        }






    }
    private void getRegistrationToken(Action action, String deviceID, String userID) throws Exception {
        KeyPair kp = this.createKeyPair();
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(this.getClass().getCanonicalName());
        claims.setAudience(pingOneDaVinci.getIssuer());
        claims.setExpirationTimeMinutesInTheFuture(1);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(1);
        claims.setSubject(userID);
        PublicJsonWebKey pjwk = PublicJsonWebKey.Factory.newPublicJwk(kp.getPublic());
        pjwk.setKeyId("k1");
        claims.setClaim(DEVICE_PUBLIC_KEY,pjwk.toParams(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
        claims.setClaim(MOBILE_PAYLOAD, PingOne.generateMobilePayload(context));
        claims.setClaim(APP_KEY_NAME, this.keyName);
        claims.setClaim(PING_ONE_DEVICE_ID, deviceID);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(kp.getPrivate());
        jws.setKeyIdHeaderValue("k1");
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        Executor executor = ContextCompat.getMainExecutor(context);
        CryptoPrimitive cryptoPrimitive = jws.prepareSigningPrimitive();
        Signature signature = cryptoPrimitive.getSignature();
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setDeviceCredentialAllowed(false)
                .setTitle("Biometric Authentication")
                .setDescription("Please verify your identity")
                .setNegativeButtonText("Cancel")
                .build();
        BiometricPrompt biometricPrompt = new BiometricPrompt((AppCompatActivity)context,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(context,
                        "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(errString.toString()));
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {

                    String signedJws = jws.getCompactSerialization();

                    JSONObject parameters = new JSONObject();
                    try {
                        parameters.put(ACTION_VALUE, action.getActionValue());
                        parameters.put(action.getParameterName(), signedJws);
                        pingOneDaVinci.continueFlow(parameters, context);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                    }

                } catch ( JoseException e) {
                    e.printStackTrace();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show();
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException("Authentication failed"));
            }
        });
        biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(signature));
    }

    private void getTransactionToken(Action action, String transaction, String deviceID, String userID, PrivateKey privateKey) throws Exception {

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(this.getClass().getCanonicalName());
        claims.setAudience(pingOneDaVinci.getIssuer());
        claims.setExpirationTimeMinutesInTheFuture(1);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(1);
        claims.setSubject(userID);
        claims.setClaim(MOBILE_PAYLOAD, PingOne.generateMobilePayload(context));
        claims.setClaim(PING_ONE_DEVICE_ID, deviceID);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] transactionHash = digest.digest(
                transaction.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * transactionHash.length);
        for (int i = 0; i < transactionHash.length; i++) {
            String hex = Integer.toHexString(0xff & transactionHash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        claims.setClaim(TX_DETAILS_HASH, hexString.toString());
        JSONObject txObj = new JSONObject(transaction);
        StringBuffer txMessage = new StringBuffer();
        for (Iterator<String> it = txObj.keys(); it.hasNext(); ) {
            String key = it.next();
            txMessage.append(key);
            txMessage.append(": ");
            txMessage.append(txObj.getString(key));
            if (it.hasNext()) txMessage.append(System.getProperty("line.separator"));
        }
        String promptString = txMessage.toString();
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue("k1");
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        Executor executor = ContextCompat.getMainExecutor(context);
        CryptoPrimitive cryptoPrimitive = jws.prepareSigningPrimitive();
        Signature signature = cryptoPrimitive.getSignature();
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setDeviceCredentialAllowed(false)
                .setTitle("Transaction Approval")
                .setDescription(promptString)
                .setNegativeButtonText("Cancel")
                .build();
        BiometricPrompt biometricPrompt = new BiometricPrompt((AppCompatActivity)context,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(context,
                        "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(errString.toString()));
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {

                    String signedJws = jws.getCompactSerialization();

                    JSONObject parameters = new JSONObject();
                    try {
                        parameters.put(ACTION_VALUE, action.getActionValue());
                        parameters.put(action.getParameterName(), signedJws);
                        pingOneDaVinci.continueFlow(parameters, context);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                    }

                } catch ( JoseException e) {
                    e.printStackTrace();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(e.getMessage()));
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show();
                pingOneDaVinci.handleAsyncException(new PingOneDaVinciException("Authentication failed"));
            }
        });
        biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(signature));
    }

    private KeyPair createKeyPair() throws Exception {
        this.keyName = getRandomString(48);
        Calendar end = new GregorianCalendar();
        end.add(Calendar.MONTH, 1);
        KeyGenParameterSpec.Builder builder =  new KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY );
        builder.setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(true);

        KeyGenParameterSpec keyGenParams = builder.build();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEY_STORE_PROVIDER_NAME);
        keyPairGenerator.initialize(keyGenParams);
        KeyPair kp = keyPairGenerator.genKeyPair();
        return kp;
    }

    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";

    private static String getRandomString(final int sizeOfRandomString)
    {
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(sizeOfRandomString);
        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}
