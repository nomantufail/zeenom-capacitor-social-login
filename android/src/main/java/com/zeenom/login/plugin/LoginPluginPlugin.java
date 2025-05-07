package com.zeenom.login.plugin;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONObject;

@CapacitorPlugin(name = "LoginPlugin")
public class LoginPluginPlugin extends Plugin {

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<IntentSenderRequest> signInLauncher;
    private String callId;
    private String projectId;
    private String applicationId;
    private String apiKey;
    private String clientId;

    @NonNull
    private ActivityResultLauncher<IntentSenderRequest> signInLauncher() {
        return getActivity().registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    try {
                        com.google.android.gms.auth.api.identity.SignInCredential credential =
                                oneTapClient.getSignInCredentialFromIntent(data);
                        String idToken = credential.getGoogleIdToken();
                        if (idToken != null) {
                            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                            firebaseAuth.signInWithCredential(firebaseCredential)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            firebaseAuth.getCurrentUser().getIdToken(true)
                                                    .addOnCompleteListener(tokenTask -> {
                                                        if (tokenTask.isSuccessful()) {
                                                            String finalIdToken = tokenTask.getResult().getToken();
                                                            Log.d("MainActivity", "ID Token: " + finalIdToken);
                                                            PluginCall call = bridge.getSavedCall(callId);
                                                            call.resolve(new JSObject().put("value", finalIdToken));
                                                            // Send the ID token to your backend
                                                        } else {
                                                            Log.e("MainActivity", "Failed to get final ID Token", tokenTask.getException());
                                                        }
                                                    });
                                        } else {
                                            Log.e("MainActivity", "Firebase Sign-in failed", task.getException());
                                        }
                                    });
                        } else {
                            Log.e("MainActivity", "No ID token!");
                        }
                    } catch (ApiException e) {
                        Log.e("MainActivity", "Sign-in failed: " + e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    private void signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    try {
                        IntentSenderRequest request = new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                        signInLauncher.launch(request);
                    } catch (Exception e) {
                        Log.e("MainActivity", "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                    }
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "One Tap sign-in failed: " + e.getLocalizedMessage()));
    }

    @Override
    public void load() {
        super.load();
        signInLauncher = signInLauncher();

    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        Log.i("Zeeshan_debug", "Echo called: " + value);
        fetchGoogleJson(value);
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setProjectId(this.projectId)
                .setApplicationId(this.applicationId)
                .setApiKey(this.apiKey)
                .build();

        if (FirebaseApp.getApps(getActivity()).isEmpty()) {
            FirebaseApp.initializeApp(getActivity(), options);
        }

        firebaseAuth = FirebaseAuth.getInstance();

        oneTapClient = Identity.getSignInClient(getActivity());

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(this.clientId)
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                )
                .setAutoSelectEnabled(false)
                .build();

        bridge.saveCall(call);
        callId = call.getCallbackId();
        signInWithGoogle();
    }

    private void fetchGoogleJson(String value) {
        try {
            JSONObject config = new JSONObject(value);
            this.apiKey = config.getString("apiKey");
            this.clientId = config.getString("clientId");
            this.projectId = config.getString("projectId");
            this.applicationId = config.getString("appId");
        } catch (Exception e) {
            Log.e("MainActivity", "Error parsing JSON: " + e.getMessage());
        }
    }
}
