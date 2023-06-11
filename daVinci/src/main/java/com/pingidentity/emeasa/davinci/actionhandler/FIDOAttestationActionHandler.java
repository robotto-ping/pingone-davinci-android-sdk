package com.pingidentity.emeasa.davinci.actionhandler;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.util.Base64;


import androidx.activity.result.ActivityResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.Attachment;
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity;
import com.google.android.gms.tasks.Task;
import com.pingidentity.emeasa.davinci.ActivityResultHanlder;
import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FIDOAttestationActionHandler implements DaVinciFlowActionHandler, ActivityResultHanlder {

    private static final String ACTION_VALUE = "actionValue";
    private Fido2ApiClient fido2ApiClient;
    private PingOneDaVinci pingOneDaVinci;
    private Context context;
    private ContinueResponse continueResponse;





    @Override
    public void handle(ContinueResponse continueResponse, Context context, PingOneDaVinci pingOneDaVinci) throws PingOneDaVinciException {
        this.pingOneDaVinci = pingOneDaVinci;
        this.context = context;
        this.continueResponse = continueResponse;
        pingOneDaVinci.setActivityResultHandler(this);
        fido2ApiClient = Fido.getFido2ApiClient(context);
        Task<Boolean> t = fido2ApiClient.isUserVerifyingPlatformAuthenticatorAvailable();
        t.addOnSuccessListener(
                b -> {
                  if (!b) {
                      try {
                          JSONObject parameters = new JSONObject();
                          parameters.put(ACTION_VALUE, "FIDONotAvailable");
                          pingOneDaVinci.continueFlow(parameters, context);
                      } catch (Exception e) {
                          pingOneDaVinci.handleAsyncException(e);
                      }
                  } else {
                      for (Action a: continueResponse.getActions()) {
                          if (a.getType().equalsIgnoreCase("platformAttestation")) {
                              try {
                              Map<String, Object> publicKeyCredentialCreationOptionsMap = (Map<String, Object> )a.getInputData().get("publicKeyCredentialCreationOptions");
                              ObjectMapper mapper = new ObjectMapper();
                              String publicKeyCredentialCreationOptionString =  mapper.writeValueAsString(publicKeyCredentialCreationOptionsMap);

                              JSONObject publicKeyCredentialCreationOptions =  new JSONObject(publicKeyCredentialCreationOptionString);

                                  PublicKeyCredentialCreationOptions opts = this.getCreationOptions(publicKeyCredentialCreationOptions);
                                  Task<PendingIntent> t2 = fido2ApiClient.getRegisterPendingIntent(opts);
                                  t2.addOnSuccessListener(
                                          p -> {
                                              pingOneDaVinci.launchPendingIntent(p);
                                          });
                                  t2.addOnFailureListener(e -> {
                                      e.printStackTrace();
                                      pingOneDaVinci.handleAsyncException(e);
                                  });
                              } catch (Exception e) {
                                  pingOneDaVinci.handleAsyncException(e);
                              }


                          }
                      }
                  }
                }
        );
    }

    private PublicKeyCredentialCreationOptions getCreationOptions(JSONObject publicKeyCredentialCreationOptions) throws JSONException, AttestationConveyancePreference.UnsupportedAttestationConveyancePreferenceException {
        PublicKeyCredentialCreationOptions.Builder b = new PublicKeyCredentialCreationOptions.Builder();
        JSONObject rpObj = (JSONObject) publicKeyCredentialCreationOptions.get("rp");
        b.setRp(new PublicKeyCredentialRpEntity(rpObj.getString("id"), rpObj.getString("id"), ""));
        String attestationConveyancePreference = publicKeyCredentialCreationOptions.getString("attestation");
        b.setAttestationConveyancePreference(AttestationConveyancePreference.fromString(attestationConveyancePreference));
        double timeoutSeconds = publicKeyCredentialCreationOptions.getDouble("timeout");
        b.setTimeoutSeconds(timeoutSeconds);
        JSONObject authenticatorSelection = (JSONObject) publicKeyCredentialCreationOptions.get("authenticatorSelection");
        AuthenticatorSelectionCriteria.Builder b2 = new AuthenticatorSelectionCriteria.Builder();
        b2.setAttachment(Attachment.valueOf(authenticatorSelection.getString("authenticatorAttachment").toUpperCase()));
        b.setAuthenticatorSelection(b2.build());
        JSONArray challenge = publicKeyCredentialCreationOptions.getJSONArray("challenge");
        byte[] cbytes = new byte[challenge.length()];
        for (int i = 0; i < challenge.length(); i++) {
            cbytes[i] = (byte) (((int) challenge.get(i)) & 0xFF);
        }
        b.setChallenge(cbytes);
        JSONArray pubKeyCredParams = publicKeyCredentialCreationOptions.getJSONArray("pubKeyCredParams");
        List<PublicKeyCredentialParameters> pubKeyCredParamList = new ArrayList<>();
        for (int i = 0; i < pubKeyCredParams.length(); i++) {
            JSONObject pKCP = pubKeyCredParams.getJSONObject(i);
            pubKeyCredParamList.add(new PublicKeyCredentialParameters(pKCP.getString("type"), pKCP.getInt("alg")));
        }
        b.setParameters(pubKeyCredParamList);
        JSONObject user = (JSONObject) publicKeyCredentialCreationOptions.get("user");
        JSONArray userid = user.getJSONArray("id");
        byte[] uidbytes = new byte[userid.length()];
        for (int i = 0; i < userid.length(); i++) {
            uidbytes[i] = (byte) (((int) userid.get(i)) & 0xFF);
        }
        PublicKeyCredentialUserEntity userEntity = new PublicKeyCredentialUserEntity(uidbytes, user.getString("name"), "", user.getString("displayName"));
        b.setUser(userEntity);
        b.setRequestId(1);
        return b.build();
    }

    public void handleAttestationResponse(int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                if (data.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                   // handleErrorResponse(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    AuthenticatorErrorResponse response = AuthenticatorErrorResponse.deserializeFromBytes(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    String errorName = response.getErrorCode().name();
                    String errorMessage = response.getErrorMessage();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(errorMessage));
                } else if (data.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
                    byte[] response = data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA);
                    byte[] credential = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
                    try {
                        AuthenticatorAttestationResponse aTTresponse =
                                AuthenticatorAttestationResponse.deserializeFromBytes(
                                        response);

                        PublicKeyCredential cred = PublicKeyCredential.deserializeFromBytes(credential);

                        JSONObject attestationResp = new JSONObject();
                        attestationResp.put("id", cred.getId());
                        attestationResp.put("type", cred.getType());
                        attestationResp.put("rawId", Base64.encodeToString(cred.getRawId(), 2));
                        JSONObject resp = new JSONObject();
                        resp.put("clientDataJSON", Base64.encodeToString(aTTresponse.getClientDataJSON(), 2));
                        resp.put("attestationObject", Base64.encodeToString(aTTresponse.getAttestationObject(), 2));
                        attestationResp.put("response", resp);
                        JSONObject parameters = new JSONObject();
                        try {
                            for (Action a: continueResponse.getActions()) {
                                if (a.getType().equalsIgnoreCase("platformAttestation")) {
                                    parameters.put(ACTION_VALUE, a.getActionValue());
                                    parameters.put(a.getParameterName(), attestationResp);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            throw new PingOneDaVinciException(e.getMessage());
                        }
                        pingOneDaVinci.continueFlow(parameters, context);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case RESULT_CANCELED:

                break;
            default:

        }
    }

    @Override
    public void processActvitiyResult(ActivityResult result) {
        handleAttestationResponse(result.getResultCode(),  result.getData());
    }
}
