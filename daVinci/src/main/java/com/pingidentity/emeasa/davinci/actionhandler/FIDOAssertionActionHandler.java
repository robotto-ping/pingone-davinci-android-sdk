package com.pingidentity.emeasa.davinci.actionhandler;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.pingidentity.emeasa.davinci.activity.DaVinciActivity.ASSERTION_REQUEST;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.common.Transport;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.tasks.Task;
import com.pingidentity.emeasa.davinci.DaVinciFlowActionHandler;
import com.pingidentity.emeasa.davinci.PingOneDaVinci;
import com.pingidentity.emeasa.davinci.PingOneDaVinciException;
import com.pingidentity.emeasa.davinci.activity.DaVinciActivity;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FIDOAssertionActionHandler implements DaVinciFlowActionHandler {

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
        Intent intent = new Intent(context, DaVinciActivity.class);
        context.startActivity(intent);
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
                            if (a.getType().equalsIgnoreCase("platformAssertion")) {
                                try {
                                    Map<String, Object> publicKeyCredentialRequestOptionsMap = (Map<String, Object> )a.getInputData().get("publicKeyCredentialRequestOptions");
                                    ObjectMapper mapper = new ObjectMapper();
                                    String publicKeyCredentialRequestOptionsString =  mapper.writeValueAsString(publicKeyCredentialRequestOptionsMap);

                                    JSONObject publicKeyCredentialRequestOptions =  new JSONObject(publicKeyCredentialRequestOptionsString);

                                    PublicKeyCredentialRequestOptions opts = this.getRequestOptions(publicKeyCredentialRequestOptions);
                                    Task<PendingIntent> t2 = fido2ApiClient.getSignPendingIntent(opts);
                                    t2.addOnSuccessListener(
                                            p -> {

                                                try {
                                                    DaVinciActivity act = DaVinciActivity.getInstance();
                                                    act.setDaVinci(pingOneDaVinci);
                                                    act.registerHandlerInstance("FIDOAssertionActionHandler", FIDOAssertionActionHandler.this);
                                                    act.startIntentSenderForResult(p.getIntentSender(), ASSERTION_REQUEST, null, 0, 0, 0);
                                                } catch (IntentSender.SendIntentException e) {
                                                    e.printStackTrace();
                                                }

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

    public PublicKeyCredentialRequestOptions getRequestOptions(JSONObject responseBody) throws JSONException, AttestationConveyancePreference.UnsupportedAttestationConveyancePreferenceException {
        PublicKeyCredentialRequestOptions.Builder b = new PublicKeyCredentialRequestOptions.Builder();

        b.setRpId(responseBody.getString("rpId"));
        double timeoutSeconds = responseBody.getDouble("timeout");
        b.setTimeoutSeconds(timeoutSeconds);
        JSONArray challenge = responseBody.getJSONArray("challenge");
        byte[] cbytes = new byte[challenge.length()];
        for (int i = 0; i < challenge.length(); i++) {
            cbytes[i]=(byte)(((int)challenge.get(i)) & 0xFF);
        }
        b.setChallenge(cbytes);
        JSONArray allowCredentials = responseBody.getJSONArray("allowCredentials");
        List<PublicKeyCredentialDescriptor> allowCredentialsList = new ArrayList<PublicKeyCredentialDescriptor>();
        for (int i = 0; i < allowCredentials.length(); i++) {
            JSONObject allowCredential = allowCredentials.getJSONObject(i);
            byte[] idbytes = new byte[allowCredential.getJSONArray("id").length()];
            for (int j = 0; j < allowCredential.getJSONArray("id").length(); j++) {
                idbytes[j]=(byte)(((int)allowCredential.getJSONArray("id").get(j)) & 0xFF);
            }
            List<Transport> transports = new ArrayList<Transport>();
            transports.add(Transport.INTERNAL);
            PublicKeyCredentialDescriptor cred = new PublicKeyCredentialDescriptor(allowCredential.getString("type"), idbytes, transports);
            allowCredentialsList.add(cred);

        }
        b.setAllowList(allowCredentialsList);
        b.setRequestId(1);
        return b.build();
    }

    public void handleAssertionResponse(int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                if (data.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                    AuthenticatorErrorResponse response = AuthenticatorErrorResponse.deserializeFromBytes(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                    String errorName = response.getErrorCode().name();
                    String errorMessage = response.getErrorMessage();
                    pingOneDaVinci.handleAsyncException(new PingOneDaVinciException(errorMessage));
                } else if (data.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
                    byte[] response = data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA);
                    byte[] credential = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
                    AuthenticatorAssertionResponse aSSresponse = AuthenticatorAssertionResponse.deserializeFromBytes(response);
                    try {
                        JSONObject assertionResp = new JSONObject();
                        JSONObject resp = new JSONObject();
                        resp.put("clientDataJSON", Base64.encodeToString(aSSresponse.getClientDataJSON(), 2));
                        resp.put("signature", Base64.encodeToString(aSSresponse.getSignature(), 2));
                        if (aSSresponse.getUserHandle() != null)
                            resp.put("userHandle", Base64.encodeToString(aSSresponse.getUserHandle(), 2));
                        else
                            resp.put("userHandle", "");
                        resp.put("authenticatorData", Base64.encodeToString(aSSresponse.getAuthenticatorData(), 2));
                        assertionResp.put("response", resp);

                        PublicKeyCredential cred = PublicKeyCredential.deserializeFromBytes(credential);

                        assertionResp.put("id", cred.getId());
                        assertionResp.put("type", cred.getType());
                        assertionResp.put("rawId", Base64.encodeToString(cred.getRawId(), 2));
                        JSONObject parameters = new JSONObject();
                        try {
                            for (Action a: continueResponse.getActions()) {
                                if (a.getType().equalsIgnoreCase("platformAssertion")) {
                                    parameters.put(ACTION_VALUE, a.getActionValue());
                                    parameters.put(a.getParameterName(), assertionResp);
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

}
