package ru.gorbandev.facebookauth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;



import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private FirebaseAuth firebaseAuth;
    private AccessTokenTracker tokenTracker;
    private FirebaseAuth.AuthStateListener authStateListener;
    private LoginButton loginButton;
    private ImageView imageView;
    private TextView textView;
    public static Bitmap bitmap;
    private static final String TAG = "FacebookAuthentication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        AppEventsLogger.activateApp(getApplication());

        imageView = findViewById(R.id.avatar_iv);
        textView = findViewById(R.id.tv_username);
        loginButton = findViewById(R.id.login_button);
        loginButton.setPermissions("email", "public_profile");

        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "onSuccess" + loginResult);
                getProfilePicture(loginResult.getAccessToken());
                handleFacebookToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "onError" + error);
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    updateActivity(user);
                } else {
                    updateActivity(null);
                }
            }
        };
        tokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    firebaseAuth.signOut();
                }
            }
        };
    }


    private void handleFacebookToken(AccessToken accessToken) {
        Log.d(TAG, "handleFacebookToken" + accessToken);
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "sing in with credential: successful");
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    updateActivity(user);
                } else {
                    Log.d(TAG, "sing in with credential: failure");
                    Toast.makeText(MainActivity.this, "Authentication failure", Toast.LENGTH_SHORT).show();
                    updateActivity(null);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateActivity(FirebaseUser user) {
        if (user != null && bitmap != null) {
          textView.setText(user.getDisplayName());
          imageView.setImageBitmap(bitmap);
        } else {
            textView.setText("");
            imageView.setImageResource(R.drawable.avatar);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void getProfilePicture(AccessToken token) {
        GraphRequest request = GraphRequest.newMeRequest(token, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                if (response != null) {
                    if (object != null) {
                        JSONObject data = response.getJSONObject();
                        try {
                            String profilePicUrl = data.getJSONObject("picture").getJSONObject("data").getString("url");
                            new LoaderProfilePicture().execute(profilePicUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        Bundle parameters = new Bundle();
        //type enum{small, normal, album, large, square}
        //The size of this picture. It can be one of the following values: small, normal, large, square.
        parameters.putString("fields", "id,name,link,email,picture.type(large)");
        request.setParameters(parameters);
        request.executeAsync();
    }

    static class LoaderProfilePicture extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            return downloadBitmap(strings[0]);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            bitmap = result;
        }
        private Bitmap downloadBitmap(String url) {
            Bitmap picture = null;
            try {
                URL urlImage = new URL(url);
                picture = BitmapFactory.decodeStream(urlImage.openConnection().getInputStream());;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return picture;
        }
    }

}

