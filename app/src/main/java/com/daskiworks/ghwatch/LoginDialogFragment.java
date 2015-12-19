/*
 * Copyright 2014 contributors as indicated by the @authors tag.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daskiworks.ghwatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.daskiworks.ghwatch.backend.AuthenticationManager;
import com.daskiworks.ghwatch.backend.AuthenticationManager.LoginViewData;
import com.daskiworks.ghwatch.model.BaseViewData;
import com.daskiworks.ghwatch.model.GHUserLoginInfo;
import com.daskiworks.ghwatch.model.LoadingStatus;

/**
 * {@link DialogFragment} for login dialog.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class LoginDialogFragment extends DialogFragment implements TextWatcher {

  private static final String TAG = LoginDialogFragment.class.getSimpleName();

  public static final String ARG_CANCELABLE = "CANCELABLE";

  private EditText fieldUsername;
  private EditText fieldPassword;
  private EditText fieldOtp;
  private TextView otpInfo;
  private Button buttonLogin;

  private AuthenticationManager authenticationManager;

  /**
   * The activity that creates an instance of this dialog fragment may implement this interface in order to receive event callbacks. Each method passes the
   * DialogFragment in case the host needs to query it.
   */
  public interface LoginDialogListener {
    public void afterLoginSuccess(LoginDialogFragment dialog);

    public FragmentManager getFragmentManager();
  }

  LoginDialogListener mListener;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof LoginDialogListener) {
      mListener = (LoginDialogListener) activity;
    } else {
      Log.d(TAG, "Activity is not LoginDialogListener");
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    authenticationManager = AuthenticationManager.getInstance();
  }

  @SuppressLint("InflateParams")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = new Dialog(getActivity());
    dialog.setTitle(R.string.dialog_login);
    View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_login, null);
    dialog.setContentView(v);

    fieldUsername = (EditText) v.findViewById(R.id.username);
    fieldUsername.addTextChangedListener(this);

    fieldPassword = (EditText) v.findViewById(R.id.password);
    fieldPassword.addTextChangedListener(this);
    fieldPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (event == null && actionId == EditorInfo.IME_ACTION_DONE) {
          loginClicked();
          return true;
        }
        return false;
      }
    });

    CheckBox ch = (CheckBox) v.findViewById(R.id.password_show);
    ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int ss = fieldPassword.getSelectionStart();
        int se = fieldPassword.getSelectionEnd();
        if (isChecked)
          fieldPassword.setTransformationMethod(null);
        else
          fieldPassword.setTransformationMethod(new PasswordTransformationMethod());
        fieldPassword.setSelection(ss, se);

      }

    });

    otpInfo = (TextView) v.findViewById(R.id.otp_info);
    fieldOtp = (EditText) v.findViewById(R.id.otp);
    fieldOtp.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (event == null && actionId == EditorInfo.IME_ACTION_DONE) {
          loginClicked();
          return true;
        }
        return false;
      }
    });

    Button buttonCancel = (Button) v.findViewById(R.id.button_cancel);
    if (getArguments().getBoolean(ARG_CANCELABLE, false)) {
      buttonCancel.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
          LoginDialogFragment.this.getDialog().dismiss();
        }
      });
      dialog.setCancelable(true);
      dialog.setCanceledOnTouchOutside(true);
    } else {
      buttonCancel.setVisibility(View.GONE);
      v.findViewById(R.id.btnDivider).setVisibility(View.GONE);
      dialog.setCancelable(false);
      dialog.setCanceledOnTouchOutside(false);
      // prevent dialog to be dismissed by back button
      dialog.setOnKeyListener(new OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_BACK) {
            getActivity().finish();
            return true;
          }
          return false;
        }

      });

    }

    buttonLogin = (Button) v.findViewById(R.id.button_login);
    buttonLogin.setEnabled(false);
    buttonLogin.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        loginClicked();
      }
    });

    // preset fields
    if (savedInstanceState == null) {
      GHUserLoginInfo cc = authenticationManager.loadCurrentUser(getActivity());
      if (cc != null) {
        fieldUsername.getText().append(cc.getUsername());
      }
    } else {
      if (savedInstanceState.getBoolean("OTP_VISIBLE", false)) {
        makeOtpFieldsVisible();
      }
    }
    ActivityTracker.sendView(getActivity(), TAG);
    return dialog;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("OTP_VISIBLE", fieldOtp.getVisibility() == View.VISIBLE);
  }

  private class LoginTask extends AsyncTask<String, String, LoginViewData> {

    ProgressDialog progress;

    @Override
    protected void onPreExecute() {
      progress = ProgressDialog.show(getActivity(), null, getString(R.string.progress_login_title), true);
      ActivityTracker.sendEvent(getActivity(), ActivityTracker.CAT_UI, "login", "", 0L);
    }

    @Override
    protected LoginViewData doInBackground(String... params) {
      return authenticationManager.login(getActivity(), params[0], params[1], params[2]);
    }

    @Override
    protected void onPostExecute(LoginViewData result) {
      try {
        if (isCancelled() || result == null) {
          return;
        }
        if (result.loadingStatus != LoadingStatus.OK) {
          if (result.loadingStatus == LoadingStatus.AUTH_ERROR && result.isOtp) {
            makeOtpFieldsVisible();
          } else {
            showErrorMessage(result);
          }
        } else {
          LoginDialogFragment.this.getDialog().dismiss();
          if (mListener != null)
            mListener.afterLoginSuccess(LoginDialogFragment.this);
        }
      } finally {
        progress.dismiss();
      }
    }

    protected void showErrorMessage(BaseViewData result) {
      StringBuilder sb = new StringBuilder();
      if (result.loadingStatus == LoadingStatus.AUTH_ERROR) {
        sb.append(getString(R.string.message_err_error_auth));
      } else {
        sb.append(getString(result.loadingStatus.getResId()));
      }
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(sb).setCancelable(true).setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
        }
      });

      builder.create().show();
    }
  }

  @Override
  public void afterTextChanged(Editable s) {
    buttonLogin.setEnabled(Utils.trimToNull(fieldUsername.getText().toString()) != null && Utils.trimToNull(fieldPassword.getText().toString()) != null);
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {

  }

  protected void loginClicked() {
    boolean go = true;
    if (Utils.trimToNull(fieldUsername.getText().toString()) == null) {
      fieldUsername.setError(getString(R.string.error_username_mandatory));
      go = false;
    }
    if (Utils.trimToNull(fieldPassword.getText().toString()) == null) {
      fieldPassword.setError(getString(R.string.error_password_mandatory));
      go = false;
    }

    String otp = null;
    if (fieldOtp.getVisibility() == View.VISIBLE) {
      otp = fieldOtp.getText().toString();
    }

    if (go) {
      new LoginTask().execute(fieldUsername.getText().toString(), fieldPassword.getText().toString(), otp);
    }
  }

  protected void makeOtpFieldsVisible() {
    fieldOtp.setVisibility(View.VISIBLE);
    fieldOtp.setFocusable(true);
    fieldOtp.requestFocus();
    otpInfo.setVisibility(View.VISIBLE);
  }
}
