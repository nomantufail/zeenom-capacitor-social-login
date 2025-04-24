package com.zeenom.login.plugin;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

@CapacitorPlugin(name = "LoginPlugin")
public class LoginPluginPlugin extends Plugin {

    private LoginPlugin implementation = new LoginPlugin();

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    @ActivityCallback
    private void handleSignInResult(PluginCall call, ActivityResult result) {
        Log.i("Zeeshan_debug", "handleSignInResult called: " + result);
        Intent data = result.getData();

        if (data == null) {
            handleError(call, "No data returned from sign-in");
            return;
        }

        try {
            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();

            if (idToken != null) {
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                                        .addOnCompleteListener(tokenTask -> {
                                            if (tokenTask.isSuccessful()) {
                                                String firebaseToken = tokenTask.getResult().getToken();
                                                JSObject response = new JSObject();
                                                response.put("value", firebaseToken);
                                                call.resolve(response);
                                            } else {
                                                Log.e("Zeeshan_debug LoginPlugin", "Failed to get Firebase token: " + tokenTask.getException().getMessage());
                                                handleError(call, "Failed to get Firebase token: " + tokenTask.getException().getMessage());
                                            }
                                        });
                            } else {
                                Log.e("Zeeshan_debug LoginPlugin", "Firebase sign-in failed: " + task.getException().getMessage());
                                handleError(call, "Firebase sign-in failed: " + task.getException().getMessage());
                            }
                        });
            } else {
                handleError(call, "No Google ID token received");
            }

        } catch (ApiException e) {
            handleError(call, "Google sign-in failed: " + e.getMessage());
        }
    }

    private void handleError(PluginCall call, String errorMessage) {
        Log.e("Zeeshan_debug LoginPlugin", errorMessage);
        JSObject errorResponse = new JSObject();
        errorResponse.put("value", errorMessage);
        call.resolve(errorResponse);
    }

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
            oneTapClient = Identity.getSignInClient(activity);

            signInRequest = new BeginSignInRequest.Builder()
                    .setGoogleIdTokenRequestOptions(
                            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                    .setSupported(true)
                                    .setServerClientId("394556941374-r87tsj47981q4teq0mqvot913s44nrbp.apps.googleusercontent.com")
                                    .setFilterByAuthorizedAccounts(false)
                                    .build())
                    .build();
            Log.i("Zeeshan_debug", "Sign-in request created 2");
            oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener(activity, result -> {
                        Log.i("Zeeshan_debug", "Sign-in request successful");
                        try {
                            startActivityForResult(call, activity.getIntent(), "handleSignInResult");
                        } catch (Exception e) {
                            Log.e("Zeeshan_debug", "Error starting intent: " + e.getMessage());
                            JSObject response = new JSObject();
                            response.put("value", "Google sign-in failed: " + e.getMessage());
                            call.resolve(response);
                        }
                    })
                    .addOnFailureListener(activity, e -> {
                        Log.e("Zeeshan_debug", "Sign-in request failed: " + e.getMessage());
                        JSObject response = new JSObject();
                        response.put("value", "Google sign-in failed: " + e.getMessage());
                        call.resolve(response);
                    });
        });
    }

    private void initializeFirebase(String jsonConfig, FirebaseInitCallback callback) {
        try {
            JSONObject config = new JSONObject(jsonConfig);

//            String googleAppID = config.getString("GOOGLE_APP_ID");
            String gcmSenderID = config.getString("GCM_SENDER_ID");
            String apiKey = config.getString("API_KEY");
            String clientID = config.getString("CLIENT_ID");
            String projectID = config.getString("PROJECT_ID");
            String bundleID = config.getString("BUNDLE_ID");
            String storageBucket = config.optString("STORAGE_BUCKET", null);
            String googleAppID = "1:336545645239:android:2a62452a6d30fdaf220332";
//            String gcmSenderID = "394556941374";
//            String apiKey = "AIzaSyCON86ECP_4ZoUzyK1IZLhHDcmQIB6Vuf0";
//            String clientID = config.getString("CLIENT_ID");
//            String projectID = "login-plugin-cc824";
//            String bundleID = config.getString("BUNDLE_ID");
//            String storageBucket = "login-plugin-cc824.firebasestorage.app";

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(googleAppID)
                    .setGcmSenderId(gcmSenderID)
                    .setApiKey(apiKey)
                    .setProjectId(projectID)
                    .setStorageBucket(storageBucket)
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
