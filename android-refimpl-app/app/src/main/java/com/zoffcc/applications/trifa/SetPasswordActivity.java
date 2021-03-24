/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import static com.zoffcc.applications.trifa.MainActivity.getRandomString;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LEN_TRIFA_AUTOGEN_PASSWORD;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF__DB_secrect_key__user_hash;

public class SetPasswordActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.SetPasswordActy";

    private check_user_password_criteria mAuthTask = null;

    // UI references.
    private EditText mPasswordView1;
    private EditText mPasswordView2;
    private View mProgressView;
    private View mLoginFormView;

    private SharedPreferences settings;
    private boolean runMainActivity = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        runMainActivity = extras.getBoolean("runMainActivity", true);

        Log.i(TAG, "onCreate");

        setContentView(R.layout.activity_set_password);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mPasswordView1 = (EditText) findViewById(R.id.password_1);
        mPasswordView1.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                return true;
            }
        });

        mPasswordView2 = (EditText) findViewById(R.id.password_2);
        mPasswordView2.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                return true;
            }
        });

        Button SignInButton = (Button) findViewById(R.id.set_button);
        SignInButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                check_password_validity();
            }
        });

        Button SkipButton = (Button) findViewById(R.id.skip_button);
        SkipButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                auto_create_password();
                settings.edit().putBoolean("PW_SET_SCREEN_DONE", true).commit();
                // ok open main activity
                if (runMainActivity == true) {
                    Intent main_act = new Intent(SetPasswordActivity.this, MyMainActivity.class);
                    startActivity(main_act);
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("PREF__DB_secrect_key__user_hash", PREF__DB_secrect_key__user_hash);
                    setResult(0, intent);
                }
                finish();
            }
        });


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    void auto_create_password()
    {
        // this does NOT need to be crpytographically secure, since it will be saved cleartext
        // it is just used if the user does not want to set a real password

        // create new key -------------
        String key = getRandomString(LEN_TRIFA_AUTOGEN_PASSWORD);
        settings.edit().putString("DB_secrect_key", key).commit();
        // create new key -------------
    }

    private void check_password_validity()
    {
        Log.i(TAG, "attemptLogin");

        if (mAuthTask != null)
        {
            return;
        }

        // Reset errors.
        mPasswordView1.setError(null);
        mPasswordView2.setError(null);

        // Store values at the time of the login attempt.
        String password1 = mPasswordView1.getText().toString();
        String password2 = mPasswordView2.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password1))
        {
            mPasswordView1.setError(this.getString(R.string.set_password_message_empty));
            focusView = mPasswordView1;
            cancel = true;
        }

        if (!isPasswordValid(password1))
        {
            mPasswordView1.setError(this.getString(R.string.set_password_message_password_invalid));
            focusView = mPasswordView1;
            cancel = true;
        }

        if (TextUtils.isEmpty(password2))
        {
            mPasswordView2.setError(this.getString(R.string.set_password_message_empty));
            focusView = mPasswordView2;
            cancel = true;
        }

        if (!isPasswordValid(password2))
        {
            mPasswordView2.setError(this.getString(R.string.set_password_message_password_invalid));
            focusView = mPasswordView2;
            cancel = true;
        }

        if (!TextUtils.equals(password1, password2))
        {
            mPasswordView2.setError(this.getString(R.string.set_password_message_password_dont_match));
            focusView = mPasswordView2;
            cancel = true;
        }

        if (cancel)
        {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        }
        else
        {
            // Show a progress spinner, and kick off a background task to
            // perform the user password criteria check.
            showProgress(true);
            mAuthTask = new check_user_password_criteria(password1, password2);
            mAuthTask.execute((Void) null);
        }
    }

    static boolean isPasswordValid(String password)
    {
        //TODO: Replace this with better criteria
        return password.length() > 7;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show)
    {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
        {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(
                    new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(
                    new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
        }
        else
        {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class check_user_password_criteria extends AsyncTask<Void, Void, Boolean>
    {

        private final String mPassword1;
        private final String mPassword2;

        check_user_password_criteria(String password1, String password2)
        {
            mPassword1 = password1;
            mPassword2 = password2;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            // just in case, check here again if both passwords actually match
            if (!TextUtils.equals(mPassword1, mPassword2))
            {
                return false;
            }

            String try_password_hash = TrifaSetPatternActivity.bytesToString(
                    TrifaSetPatternActivity.sha256(TrifaSetPatternActivity.StringToBytes2(mPassword1)));

            // remember hash ---------------
            PREF__DB_secrect_key__user_hash = try_password_hash;
            // remember hash ---------------

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success)
        {
            mAuthTask = null;
            showProgress(false);

            if (success)
            {
                settings.edit().putBoolean("PW_SET_SCREEN_DONE", true).commit();
                // ok open main activity
                if (runMainActivity == true) {
                    Intent main_act = new Intent(SetPasswordActivity.this, MyMainActivity.class);
                    startActivity(main_act);
                }
                finish();
            }
            else
            {
                mPasswordView1.setError("* Error *");
                mPasswordView1.requestFocus();
            }
        }

        @Override
        protected void onCancelled()
        {
            mAuthTask = null;
            showProgress(false);
        }
    }

    @Override
    public void onBackPressed()
    {
        // super.onBackPressed();
        // do nothing!!
    }
}

