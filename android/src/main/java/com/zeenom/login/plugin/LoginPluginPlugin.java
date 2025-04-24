package com.zeenom.login.plugin;

import android.app.Activity;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

@CapacitorPlugin(name = "LoginPlugin")
public class LoginPluginPlugin extends Plugin {

    private LoginPlugin implementation = new LoginPlugin();

//  private SignInClient oneTapClient;
//    private BeginSignInRequest signInRequest;

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value", "");

        Log.i("Zeeshan_debug", "Firebase config: " + value);

        initializeFirebase(value, (res, error) -> {
            Log.i("Zeeshan_debug", "Firebase initialized: " + res);
            if (!res) {
                JSObject response = new JSObject();
                response.put("value", error);
                call.resolve(response);
                return;
            }

            Activity activity = getActivity();
//            oneTapClient = Identity.getSignInClient(activity);
//
//            signInRequest = new BeginSignInRequest.Builder()
//                    .setGoogleIdTokenRequestOptions(
//                            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                                    .setSupported(true)
//                                    .setServerClientId(Objects.requireNonNull(FirebaseApp.getInstance().getOptions().getGcmSenderId()))
//                                    .setFilterByAuthorizedAccounts(false)
//                                    .build())
//                    .build();
//            Log.i("Zeeshan_debug", "Sign-in request created 2");
//            oneTapClient.beginSignIn(signInRequest)
//                    .addOnSuccessListener(activity, result -> {
//                        Log.i("Zeeshan_debug", "Sign-in request successful");
//                        try {
//                            activity.startIntentSenderForResult(
//                                    result.getPendingIntent().getIntentSender(),
//                                    1001, null, 0, 0, 0);
//                        } catch (Exception e) {
//                            Log.e("Zeeshan_debug", "Error starting intent: " + e.getMessage());
//                            JSObject response = new JSObject();
//                            response.put("value", "Google sign-in failed: " + e.getMessage());
//                            call.resolve(response);
//                        }
//                    })
//                    .addOnFailureListener(activity, e -> {
//                        Log.e("Zeeshan_debug", "Sign-in request failed: " + e.getMessage());
//                        JSObject response = new JSObject();
//                        response.put("value", "Google sign-in failed: " + e.getMessage());
//                        call.resolve(response);
//                    });
        });
    }


    private void initializeFirebase(String jsonConfig, FirebaseInitCallback callback) {
        try {
            JSONObject config = new JSONObject(jsonConfig);

//            String googleAppID = config.getString("GOOGLE_APP_ID");
//            String gcmSenderID = config.getString("GCM_SENDER_ID");
//            String apiKey = config.getString("API_KEY");
//            String clientID = config.getString("CLIENT_ID");
//            String projectID = config.getString("PROJECT_ID");
//            String bundleID = config.getString("BUNDLE_ID");
//            String storageBucket = config.optString("STORAGE_BUCKET", null);
            String googleAppID = "1:336545645239:android:2a62452a6d30fdaf220332";
            String gcmSenderID = "336545645239";
            String apiKey = "AIzaSyBMR-gwrkzM-oxUaQqcHNAmlDlpNNRuIg8";
            String clientID = config.getString("CLIENT_ID");
            String projectID = "loan-tracker-9b25d";
            String bundleID = config.getString("BUNDLE_ID");
            String storageBucket = config.optString("STORAGE_BUCKET", null);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(googleAppID)
                    .setGcmSenderId(gcmSenderID)
                    .setApiKey(apiKey)
                    .setProjectId(projectID)
                    .setStorageBucket(storageBucket)
                    .setDatabaseUrl(bundleID)
                    .build();

            if (FirebaseApp.getApps(getContext()).isEmpty()) {
                FirebaseApp.initializeApp(getContext(), options);
            }

            callback.onComplete(true, null);
        } catch (JSONException e) {
            Log.e("LoginPlugin", "Error parsing Firebase config: " + e.getMessage());
            callback.onComplete(false, "Invalid Firebase config");
        }
    }

    private interface FirebaseInitCallback {
        void onComplete(boolean result, String error);
    }
}
