package com.pingidentity.emeasa.davinci;

import android.app.Activity;

import android.text.InputType;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import android.widget.TextView;

import com.pingidentity.emeasa.davinci.api.Action;
import com.pingidentity.emeasa.davinci.api.ContinueResponse;
import com.pingidentity.emeasa.davinci.api.Field;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DaVinciForm {

    private ViewGroup formLayout;
    private Activity activity;
    private JSONObject flowPayload = new JSONObject();
    private Map<String, Integer> inputFieldIdentifiers = new HashMap<>();
    private int fieldID = 1000;
    private PingOneDaVinci daVinci;
    private int buttonStyle = 0;
    private int editViewStyle = 0;

    public int getEditViewStyle() {
        return editViewStyle;
    }

    public void setEditViewStyle(int editViewStyle) {
        this.editViewStyle = editViewStyle;
    }

    public int getHeaderTextStyle() {
        return headerTextStyle;
    }

    public void setHeaderTextStyle(int headerTextStyle) {
        this.headerTextStyle = headerTextStyle;
    }

    public int getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(int textStyle) {
        this.textStyle = textStyle;
    }

    private int headerTextStyle = 0;
    private int textStyle = 0;




    public DaVinciForm(PingOneDaVinci daVinci, ViewGroup formLayout, Activity activity) {
        this.formLayout = formLayout;
        this.activity = activity;
        this.daVinci =daVinci;

    }


    public void buildView(ContinueResponse continueResponse) throws PingOneDaVinciException {
        // this is where we build the actual form
        try {
            //start with the title
            if (continueResponse.getTitle() != null) {
                TextView tView = new TextView(activity, null, 0, headerTextStyle);
                tView.setText(continueResponse.getTitle());
                formLayout.addView(tView);
            }
            // now the fields
            boolean focused = false;
            for (Field f : continueResponse.getFields()) {
                if (f.getType().equalsIgnoreCase(Field.TEXT)) {
                    // This is a text field - render a label
                    TextView tView = new TextView(activity, null, 0, textStyle);
                    tView.setText(f.getValue());
                    formLayout.addView(tView);
                } else if (f.getType().equalsIgnoreCase(Field.HIDDEN)) {
                    // This is a hidden field that we need to pass straight through
                    flowPayload.put(f.getParameterName(), f.getValue());
                }else if (f.getType().equalsIgnoreCase(Field.INPUT)) {
                    // This is an input field - render an EditText
                    EditText editView = new EditText(activity, null, 0, editViewStyle);

                    editView.setId(++fieldID);
                    if (f.getValue() !=null && !f.getValue().isEmpty()) {
                        editView.setText(f.getValue());
                    }
                    inputFieldIdentifiers.put(f.getParameterName(), fieldID);
                    if (f.isMasked()) {
                        editView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    formLayout.addView(editView);
                    if (!focused) {
                        editView.requestFocus();
                        focused = true;
                    }
                }


            }
            // finally the button actions
            for (Action a: continueResponse.getFormSubmitActions()) {

                Button button = new Button(activity, null, 0,buttonStyle);
                button.setText(a.getDescriptionText());
                button.setOnClickListener(new ButtonClickListener(a.getActionValue()));
                /*LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(5, 10, 5, 0);
                button.setLayoutParams(params); */
                formLayout.addView(button);
            }

        }catch (Exception e) {
            throw new PingOneDaVinciException(e.getMessage());
        }
    }

    public int getButtonStyle() {
        return buttonStyle;
    }

    public void setButtonStyle(int buttonStyle) {
        this.buttonStyle = buttonStyle;
    }


    private class ButtonClickListener implements View.OnClickListener {

        private final String actionValue;

        public ButtonClickListener(String actionValue) {
            this.actionValue = actionValue;
        }

        @Override
        public void onClick(View view) {
            try {
                flowPayload.put("actionValue", this.actionValue);
                for (String parameterName : inputFieldIdentifiers.keySet()) {
                    EditText e = (EditText) activity.findViewById(inputFieldIdentifiers.get(parameterName));
                    flowPayload.put(parameterName, e.getText());
                }
                formLayout.removeViews(0, formLayout.getChildCount());
                daVinci.continueFlow(flowPayload, activity);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
