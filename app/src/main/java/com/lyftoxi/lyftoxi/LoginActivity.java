package com.lyftoxi.lyftoxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lyftoxi.lyftoxi.dao.User;
import com.lyftoxi.lyftoxi.exception.LyftoxiClientBusinessException;
import com.lyftoxi.lyftoxi.exception.LyftoxiClientException;
import com.lyftoxi.lyftoxi.singletons.CurrentUserInfo;
import com.lyftoxi.lyftoxi.util.Constants;
import com.lyftoxi.lyftoxi.util.HttpRestUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lyftoxi.lyftoxi.util.ImageUtil;
import com.lyftoxi.lyftoxi.util.LyftoxiFirebase;
import com.lyftoxi.lyftoxi.util.PasswordUtil;
import com.lyftoxi.lyftoxi.util.RoundImage;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends Activity {

    private UserLoginTask mAuthTask = null;

    private TextView mMobileView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Class onSuccessActivity = null;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        session = new SessionManager(getApplicationContext());
        // Set up the login form.
        mMobileView = (TextView) findViewById(R.id.mobile);
       // populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mMobileSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mMobileSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        if(null!= getIntent().getExtras()) {
            Bundle extras = getIntent().getExtras();
            String classNameStr = extras.getString("activityOnSuccess");
            Log.d("lyftoxi.debug", "classNameReceived " + classNameStr);
            if (null != classNameStr) {
                try {
                    onSuccessActivity = Class.forName(classNameStr);
                } catch (ClassNotFoundException ex) {
                    Log.d("lyftoxi.debug", "ClassNotFound " + ex.getMessage());
                    ex.printStackTrace();
                    onSuccessActivity = null;
                }
            }
        }
    }

    /*private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }*/


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mMobileView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String mobile = mMobileView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mobile)) {
            mMobileView.setError(getString(R.string.error_field_required));
            focusView = mMobileView;
            cancel = true;
        } else if (!isMobileValid(mobile)) {
            mMobileView.setError(getString(R.string.error_invalid_email));
            focusView = mMobileView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(mobile, PasswordUtil.encryptPassword(password));
            mAuthTask.execute((Void) null);
        }
    }

    private void downloadUserProfilePic()
    {

        final String profilePicFileName = session.getUserDetails().getUID()+"_profile_pic.jpg";
        Log.d("gog.debug ","profilePicFileName "+profilePicFileName);
        StorageReference storageRef = LyftoxiFirebase.storageRef;
        StorageReference profileImageRef = storageRef.child("userProfilePics/"+profilePicFileName);

        // profileImageRef.getDownloadUrl();
        final long ONE_MEGABYTE = 1024 * 1024;
        profileImageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                /*RoundImage roundedImage = new RoundImage(bm);
                profileImage.setImageDrawable(roundedImage);*/

                Log.d("lyftoxi.debug"," downloaded profile pic");
                ImageUtil imageUtil = new ImageUtil();
                String filePath = imageUtil.saveToInternalStorage(getBaseContext(),bitmap,"user_avatar.jpg");
                CurrentUserInfo.getInstance().setProfilePicPath(filePath);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception exception) {
                Log.d("lyftoxi.debug","Firebase: profile pic download failed");
            }
        });
    }

    private boolean isMobileValid(String mobile) {
        if(mobile.length()!=10)
        {
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mMobile;
        private final String mPassword;
        String errorMessage;

        Gson gson = new GsonBuilder().setDateFormat(Constants.SIMPLE_DATE_FORMAT).create();
        UserLoginTask(String email, String password) {
            mMobile = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            User userInfo = null;
            try {
                HttpRestUtil httpRestUtil = new HttpRestUtil(getApplicationContext());
                String response = httpRestUtil.httpPostSimple("userService/userLogin?mobile="+mMobile+"&password="+mPassword);
                userInfo = gson.fromJson(response, new TypeToken<User>() {}.getType());

            }catch (IOException ioex) {
                Log.e("lyftoxi.error","Error occurred in REST WS call url cannot be reached "+ioex.getMessage());
                errorMessage = "Service Unavailable";
            }
            catch (LyftoxiClientBusinessException e) {
                Log.e("lyftoxi.error","Business Exception occurred in REST WS call "+e.getMessage());
                errorMessage = e.getMessage();
            }
            catch (LyftoxiClientException e) {
                Log.e("lyftoxi.error","Error occurred in REST WS call "+e.getMessage());
                errorMessage = "Some thing wrong happened.Contact support";
            }
            catch (Exception e) {
                Log.e("lyftoxi.error","Something really went wrong "+e.getMessage());
                errorMessage = "OMG you got us a defect. Contact support with screenshot";
            }
            if(null!= userInfo  && null!= userInfo.getId() )
            {
                session.createLoginSession(userInfo.getId(), userInfo.getName(),userInfo.getPhNo(),userInfo.getEmail(),userInfo.getGender());
                CurrentUserInfo currentUserInfo = CurrentUserInfo.getInstance();
                currentUserInfo.setId(userInfo.getId());
                currentUserInfo.setName(userInfo.getName());
                currentUserInfo.setGender(userInfo.getGender());
                currentUserInfo.setEmail(userInfo.getEmail());
                currentUserInfo.setPhNo(userInfo.getPhNo());
                currentUserInfo.setAddresses(userInfo.getAddresses());
                currentUserInfo.setCarDetails(userInfo.getCarDetails());
                currentUserInfo.setDob(userInfo.getDob());

               return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            if (success.equals(true)) {

                final String profilePicFileName = session.getUserDetails().getUID()+"_profile_pic.jpg";
                Log.d("gog.debug ","profilePicFileName "+profilePicFileName);
                StorageReference storageRef = LyftoxiFirebase.storageRef;
                StorageReference profileImageRef = storageRef.child("userProfilePics/"+profilePicFileName);

                final long ONE_MEGABYTE = 1024 * 1024;
                profileImageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Log.d("lyftoxi.debug"," downloaded profile pic");
                        ImageUtil imageUtil = new ImageUtil();
                        String filePath = imageUtil.saveToInternalStorage(getBaseContext(),bitmap,"user_avatar.jpg");
                        CurrentUserInfo.getInstance().setProfilePicPath(filePath);
                        if(null!=onSuccessActivity) {
                            Log.d("lyftoxi.debug","onSuccessActivity Starting....");
                            Intent onSuccessIntent = new Intent(getApplicationContext(), onSuccessActivity);
                            onSuccessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(onSuccessIntent);
                        }
                        else {
                            Log.d("lyftoxi.debug","onSuccessActivity is null");
                            finish();
                        }
                        showProgress(false);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception exception) {
                        Log.d("lyftoxi.debug","Firebase: profile pic download failed");
                        if(null!=onSuccessActivity) {
                            Log.d("lyftoxi.debug","onSuccessActivity Starting....");
                            Intent onSuccessIntent = new Intent(getApplicationContext(), onSuccessActivity);
                            startActivity(onSuccessIntent);
                        }
                        else {
                            Log.d("lyftoxi.debug","onSuccessActivity is null");
                            finish();
                        }
                        showProgress(false);
                    }
                });



            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
                showProgress(false);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public void openSignUpActivity(View view)
    {
        Intent signUpIntent = new Intent(this, SignupActivity.class);
        startActivity(signUpIntent);
    }

    public void openForgotPasswordActivity(View view)
    {
        Intent forgotPasswordActivity = new Intent(this, ForgotPasswordActivity.class);
        startActivity(forgotPasswordActivity);
    }
}

