package com.pingidentity.emeasa.davinci;

import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.CANNOT_INSTANTIATE_HANDLER;
import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.ERROR_INVALID_HANDLER_CLASS;
import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.FLOW_ERROR;
import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.NO_HANDLER_FOR_ACTION;
import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.NO_HANDLER_FOR_CAPABILITY;
import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.NO_MAPPER_FOR_TEMPLATE;
import static com.pingidentity.emeasa.davinci.PingOneDaVinciException.TOO_MANY_ACTIONS;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.Field;
import com.pingidentity.emeasa.davinci.api.FlowResponse;
import com.pingidentity.emeasa.davinci.payloadhandler.DaVinciJSONResponsePayloadHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class PingOneDaVinci {

    private static final Object DV_API_URL_BASE = "https://orchestrate-api.pingone.";
    private static final Object DV_AUTH_URL_BASE = "https://auth.pingone.";

    private static final String TOKEN_URL_PATTERN = "%s%s/v1/company/%s/sdktoken";
    private static final String POLICY_URL_PATTERN = "%s%s/%s/davinci/policy/%s/start";
    private static final String BEARER_TOKEN_HEADER_PATTERN = "Bearer %s";
    private static final String ACCESS_TOKEN = "access_token";

    private static final String AUTHORIZATION_HEADER_NAME = "AUTHORIZATION";
    private static final String API_KEY_HEADER_NAME = "X-SK-API-KEY";
    private static final String VALUE_PREFIX = "$";
    private static final String NEXT_URL_PATTERN = "%s%s/%s/davinci/connections/%s/capabilities/%s";


    private DaVinciFlowUI flowUI;
    private String companyID;
    private String location;

    private String flowInitiationToken;

    private DaVinciFlowContext flowContext;
    private List<String> inProgressMappers = new ArrayList<>();
    private ContinueResponse continueResponse;


    public static String US = "com";
    public static String EU = "eu";
    public static String ASIA = "asia";
    public static String CA = "ca";

    private Map<String, String> responsePayloadHandlers = new HashMap<>();
    private Map<String, String> flowActionHandlers = new HashMap<>();
    private Map<String, String> valueMappers = new HashMap<>();

    public PingOneDaVinci(DaVinciFlowUI flowUI, String companyID, String location) {
        this.flowUI = flowUI;
        this.companyID = companyID;
        this.location = location;
        this.responsePayloadHandlers.put("customHTMLTemplate", "com.pingidentity.emeasa.davinci.payloadhandler.CustomHTMLTemplateResponsePayloadHandler");
        this.responsePayloadHandlers.put("createSession", "com.pingidentity.emeasa.davinci.payloadhandler.DaVinciTokenPayloadHandler");
        this.responsePayloadHandlers.put("createSessionWithCustomClaims", "com.pingidentity.emeasa.davinci.payloadhandler.DaVinciTokenPayloadHandler");
        this.responsePayloadHandlers.put("createSuccessResponse", "com.pingidentity.emeasa.davinci.payloadhandler.DaVinciJSONResponsePayloadHandler");
        this.valueMappers.put("phoneNumber", "com.pingidentity.emeasa.davinci.mapper.PhoneNumberMapper");
        this.valueMappers.put("location", "com.pingidentity.emeasa.davinci.mapper.LocationMapper");
        this.flowActionHandlers.put("submitForm", "com.pingidentity.emeasa.davinci.actionhandler.FormSubmitActionHandler");
        this.flowActionHandlers.put("platformAttestation", "com.pingidentity.emeasa.davinci.actionhandler.FIDOAttestationActionHandler");
        this.flowActionHandlers.put("platformAssertion", "com.pingidentity.emeasa.davinci.actionhandler.FIDOAssertionActionHandler");
        this.flowActionHandlers.put("riskSignals", "com.pingidentity.emeasa.davinci.actionhandler.PingOneRiskSignalsActionHandler");
        this.flowActionHandlers.put("devicePayload", "com.pingidentity.emeasa.davinci.actionhandler.PingOneMFAActionHandler");
        this.flowActionHandlers.put("pairDevice", "com.pingidentity.emeasa.davinci.actionhandler.PingOneMFAActionHandler");
    }

    public void initialise(String apiKey) {
        fetchFlowInitiationToken(apiKey);
    }

    public void initialiseWithToken(String flowInitiationToken) {
        this.flowInitiationToken = flowInitiationToken;
        flowUI.onDaVinciReady();
    }

    public boolean hasValidToken() {
        if (this.flowInitiationToken != null) {
            JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();
            JwtClaims claims;
            try {
                JwtContext jwtContext = firstPassJwtConsumer.process(this.flowInitiationToken);
                claims = jwtContext.getJwtClaims();
                NumericDate expirationTime = claims.getExpirationTime();
                return !expirationTime.isBefore(NumericDate.now());
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public void addResponsePayloadHandler(String flowCapabilityName, String handlerClassName) throws PingOneDaVinciException {
        try {
            Object handlerInstance = Class.forName(handlerClassName).newInstance();
            if (handlerInstance instanceof DaVinciResponsePayloadHandler) {
                responsePayloadHandlers.put(flowCapabilityName, handlerClassName);
            } else {
                throw new PingOneDaVinciException(ERROR_INVALID_HANDLER_CLASS);
            }
        } catch (Exception e) {
            throw new PingOneDaVinciException(e.getMessage());
        }
    }

    public void addFlowActionHandler(String actionType, String handlerClassName) throws PingOneDaVinciException {
        try {
            Object handlerInstance = Class.forName(handlerClassName).newInstance();
            if (handlerInstance instanceof DaVinciFlowActionHandler) {
                flowActionHandlers.put(actionType, handlerClassName);
            } else {
                throw new PingOneDaVinciException(ERROR_INVALID_HANDLER_CLASS);
            }
        } catch (Exception e) {
            throw new PingOneDaVinciException(e.getMessage());
        }
    }

    public void addValueMapper(String templateName, String handlerClassName) throws PingOneDaVinciException {
        try {
            Object handlerInstance = Class.forName(handlerClassName).newInstance();
            if (handlerInstance instanceof DaVinciValueMapper) {
                valueMappers.put(templateName, handlerClassName);
            } else {
                throw new PingOneDaVinciException(ERROR_INVALID_HANDLER_CLASS);
            }
        } catch (Exception e) {
            throw new PingOneDaVinciException(e.getMessage());
        }
    }

    public void startFlowPolicy(String policyID, JSONObject flowInputParameters, Context context) {
        String requestURL = String.format(POLICY_URL_PATTERN, DV_AUTH_URL_BASE, location, companyID, policyID);
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader(AUTHORIZATION_HEADER_NAME, String.format(BEARER_TOKEN_HEADER_PATTERN, this.flowInitiationToken));
        StringEntity entity = new StringEntity(flowInputParameters == null ? "" : flowInputParameters.toString(), "UTF-8");
        client.post(context, requestURL, entity, "application/json", new DaVinciAPIResponseHandler(context));
    }

    public void startFlowPolicy(String policyID, Context context) {
        startFlowPolicy(policyID, null, context);
    }

    public String getEnvironmentId() {
        return this.companyID;
    }


    private void fetchFlowInitiationToken(String apiKey) {
        try {
            String requestURL = String.format(TOKEN_URL_PATTERN, DV_API_URL_BASE, location, companyID);
            AsyncHttpClient client = new AsyncHttpClient();
            client.addHeader(API_KEY_HEADER_NAME, apiKey);
            client.get(requestURL, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        flowInitiationToken = response.getString(ACCESS_TOKEN);
                        flowUI.onDaVinciReady();
                    } catch (Exception e) {
                        flowUI.onDaVinciError(e);

                    }
                }

                @Override
                public void onFailure(int statusCode,
                                      cz.msebera.android.httpclient.Header[] headers,
                                      java.lang.Throwable throwable,
                                      org.json.JSONObject errorResponse) {
                    flowUI.onDaVinciError(throwable);
                }
            });
        } catch (Exception e) {
            flowUI.onDaVinciError(e);
        }
    }

    private void substituteValues(ContinueResponse continueResponse, Context context) throws PingOneDaVinciException, InterruptedException {
        Map<Field, DaVinciValueMapper> mappersToRun = new HashMap<>();
        if (continueResponse.getFields() != null) {
            for (Field f : continueResponse.getFields()) {
                if (f.getValue() != null && f.getValue().startsWith(VALUE_PREFIX)) {
                    String templateName = f.getValue().substring(1);
                    if (valueMappers.containsKey(templateName)) {
                        String handlerClassname = valueMappers.get(templateName);
                        try {
                            DaVinciValueMapper valueMapper = (DaVinciValueMapper) Class.forName(handlerClassname).newInstance();
                            inProgressMappers.add(f.getParameterName());
                            mappersToRun.put(f, valueMapper);

                        } catch (Exception e) {
                            throw new PingOneDaVinciException(CANNOT_INSTANTIATE_HANDLER);
                        }
                    } else {
                        throw new PingOneDaVinciException(String.format(NO_MAPPER_FOR_TEMPLATE, templateName));
                    }
                }
            }
            if (mappersToRun.isEmpty()) {
                if (continueOnUI(continueResponse)) {
                    flowUI.onFlowContinue(continueResponse);
                } else {
                    processFlowAction(continueResponse, context);
                }
            } else {
                for (Field f : mappersToRun.keySet()) {
                    DaVinciValueMapper mapper = mappersToRun.get(f);
                    mapper.performSubstitution(f, context, this);
                }
            }
        } else {
            if (continueOnUI(continueResponse)) {
                flowUI.onFlowContinue(continueResponse);
            } else {
                processFlowAction(continueResponse, context);
            }
        }

    }


    private boolean continueOnUI(ContinueResponse continueResponse) {
        for (Action a : continueResponse.getActions()) {
            if (!a.getType().equalsIgnoreCase(Action.SUBMIT_FORM)) {
                return false;
            }
        }
        for (Field f : continueResponse.getFields()) {
            if (f.getType().equalsIgnoreCase(Field.TEXT) || f.getType().equalsIgnoreCase(Field.INPUT)) {
                return true;
            }
        }
        return false;
    }

    private void processFlowAction(@NonNull ContinueResponse continueResponse, Context context) {
        if (continueResponse.getActions().size() == 1) {
            String actionType = continueResponse.getActions().get(0).getType();
            if (flowActionHandlers.containsKey(actionType)) {
                String handlerClassname = flowActionHandlers.get(actionType);
                try {
                    DaVinciFlowActionHandler actionHandler = (DaVinciFlowActionHandler) Class.forName(handlerClassname).newInstance();
                    actionHandler.handle(continueResponse, context, this);

                } catch (Exception e) {
                    flowUI.onDaVinciError(new PingOneDaVinciException(CANNOT_INSTANTIATE_HANDLER));
                }
            } else {
                flowUI.onDaVinciError(new PingOneDaVinciException(String.format(NO_HANDLER_FOR_ACTION, actionType)));
            }
        } else {
            flowUI.onDaVinciError(new PingOneDaVinciException(TOO_MANY_ACTIONS));
        }
    }

    public void handleAsyncException(Throwable t) {
        flowUI.onDaVinciError(new PingOneDaVinciException(t.getMessage()));
    }

    public void mapperComplete(String parameterName, Context context) {

        inProgressMappers.remove(parameterName);
        if (inProgressMappers.size() == 0) {
            if (continueOnUI(continueResponse)) {
                flowUI.onFlowContinue(continueResponse);
            } else {
                processFlowAction(continueResponse, context);
            }
        }

    }

    public void continueFlow(JSONObject parameters, Context context) {
        try {
            JSONObject payload = flowContext.getNextPayload().toJSON(parameters);
            AsyncHttpClient client = new AsyncHttpClient();
            client.addHeader(AUTHORIZATION_HEADER_NAME, String.format(BEARER_TOKEN_HEADER_PATTERN, this.flowInitiationToken));
            client.addHeader("interactionId", flowContext.getInteractionId());
            client.addHeader("interactionToken", flowContext.getInteractionToken());
            StringEntity entity = new StringEntity(payload.toString(), "UTF-8");
            String nextURL = String.format(NEXT_URL_PATTERN, DV_AUTH_URL_BASE, this.location, this.companyID, flowContext.getConnectionId(), flowContext.getCapabilityName());
            client.post(context, nextURL, entity, "application/json", new DaVinciAPIResponseHandler(context));
        } catch (Exception e) {
            flowUI.onDaVinciError(new PingOneDaVinciException(e.getMessage()));
        }
    }


    private class DaVinciAPIResponseHandler extends JsonHttpResponseHandler {

        private Context context;

        public DaVinciAPIResponseHandler(Context context) {
            this.context = context;
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            flowContext = new DaVinciFlowContext(response);
            String capability = flowContext.getCapabilityName();
            if (responsePayloadHandlers.containsKey(capability)) {
                String handlerClassname = responsePayloadHandlers.get(capability);
                try {
                    DaVinciResponsePayloadHandler responseHandler = (DaVinciResponsePayloadHandler) Class.forName(handlerClassname).newInstance();
                    responseHandler.initialise(response);
                    if (responseHandler.isTerminalStep()) {
                        flowContext = null;
                        flowUI.onFlowComplete(responseHandler.getFlowResponse());
                    } else {
                        flowContext.setNextPayload(responseHandler.getNextPayload());
                        continueResponse = responseHandler.getContinueResponse();
                        substituteValues(continueResponse, context);
                    }

                } catch (Exception e) {
                    flowUI.onDaVinciError(new PingOneDaVinciException(CANNOT_INSTANTIATE_HANDLER));
                }
            } else {
                flowUI.onDaVinciError(new PingOneDaVinciException(String.format(NO_HANDLER_FOR_CAPABILITY, capability)));
            }

        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
            try {
                if (response.has("capabilityName") && response.getString("capabilityName").equalsIgnoreCase("createErrorResponse")) {
                    DaVinciJSONResponsePayloadHandler handler = new DaVinciJSONResponsePayloadHandler();
                    handler.initialise(response);
                    FlowResponse flowResponse = handler.getFlowResponse();
                    flowUI.onFlowComplete(flowResponse);
                    return;
                }
            } catch (Exception e) {
            }

            flowUI.onDaVinciError(new PingOneDaVinciException(String.format(FLOW_ERROR, "" + statusCode, t.getMessage())));
        }
    }


}
