package com.saiya.indoorposapp.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.tools.ActivityCollector;
import com.saiya.indoorposapp.tools.HttpUtils;

import java.lang.ref.WeakReference;

/**
 * 用户登录Activity
 */
public class LoginActivity extends Activity implements View.OnClickListener {

    /** 未知错误 */
    public static final int UNEXPECTED_ERROR = -1;

    /** 登录成功 */
    public static final int LOGIN_SUCCEED = 0;

    /** 用户名不存在 */
    public static final int USERNAME_NOT_EXIST = 1;

    /** 密码错误 */
    public static final int PASSWORD_ERROR = 2;

    private SharedPreferences activityPreferences;

    private EditText edtTxt_login_username;
    private EditText edtTxt_login_password;
    private MyHandler myHandler = new MyHandler(this);

    //用于在子线程更新UI的Handler
    private static class MyHandler extends Handler{

        private WeakReference<LoginActivity> mActivity;

        public MyHandler(LoginActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        //处理登录时返回的消息
        @Override
        public void handleMessage(Message msg) {
            if(mActivity.get() == null) {
                return;
            }
            switch (msg.what) {
                case LOGIN_SUCCEED:
                    Intent intent = new Intent(mActivity.get(), MainActivity.class);
                    intent.putExtra("username", mActivity.get()
                            .activityPreferences.getString("lastusername", ""));
                    mActivity.get().startActivity(intent);
                    mActivity.get().finish();
                    break;
                case USERNAME_NOT_EXIST:
                    Toast.makeText(mActivity.get(), R.string.activity_login_invalidUsername,
                            Toast.LENGTH_SHORT).show();
                    break;
                case PASSWORD_ERROR:
                    Toast.makeText(mActivity.get(), R.string.activity_login_invalidPassword,
                            Toast.LENGTH_SHORT).show();
                    break;
                case UNEXPECTED_ERROR:
                    Toast.makeText(mActivity.get(), R.string.activity_common_unexpectedError,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActivityCollector.addActivity(this);
        initView();
    }

    /**
     * 初始化布局并找到控件
     */
    private void initView() {
        //得到登录Activity的SharedPreferences
        activityPreferences = getPreferences(MODE_PRIVATE);

        //初始化控件
        edtTxt_login_username = (EditText) findViewById(R.id.edtTxt_login_username);
        edtTxt_login_password = (EditText) findViewById(R.id.edtTxt_login_password);
        Button btn_login_login = (Button) findViewById(R.id.btn_login_login);
        Button btn_login_register = (Button) findViewById(R.id.btn_login_register);
        btn_login_login.setOnClickListener(this);
        btn_login_register.setOnClickListener(this);

        /** 上次登录成功的用户名 */
        String lastusername = activityPreferences.getString("lastusername", "");
        //若上次成功登录的用户名不为空,则得到那个用户的SharedPreferences
        if(!lastusername.equals("")) {
            SharedPreferences preferences = getSharedPreferences
                    (activityPreferences.getString("lastusername", "default") + "-settings",
                    MODE_PRIVATE);
            edtTxt_login_username.setText(preferences.getString("username", ""));
            //查看用户是否开启了自动登录,若开启并且Activity不是由于未鉴权启动,则自动登录一次
            if(preferences.getBoolean("autoLogin", false) &&
                    getIntent().getBooleanExtra("allowedAutoLogin", true)) {
                login(preferences.getString("username", ""), preferences.getString("password", ""));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    //控制点击登录和注册按钮发生的事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login_login:
                login(edtTxt_login_username.getText().toString(),
                        edtTxt_login_password.getText().toString());
                break;
            case R.id.btn_login_register:
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    /**
     * 进行登录操作
     * @param username 用户名
     * @param password 密码
     */
    private void login(final String username, final String password) {
        if(username.length() == 0 || password.length() == 0) {
            Toast.makeText(this, R.string.activity_common_invalidInput, Toast.LENGTH_SHORT).show();
            return;
        }
        if(username.length() > 20 || password.length() > 20) {
            Toast.makeText(this, R.string.activity_common_oversizeInput, Toast.LENGTH_SHORT).show();
            return;
        }
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.activity_login_logining));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                int loginResult = HttpUtils.login(username, password);
                if(loginResult == LOGIN_SUCCEED) {
                    SharedPreferences.Editor editor = activityPreferences.edit();
                    editor.putString("lastusername", username);
                    editor.apply();
                    editor = getSharedPreferences(username + "-settings", MODE_PRIVATE).edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.apply();
                }
                msg.what = loginResult;
                myHandler.sendMessage(msg);
                progressDialog.dismiss();
            }
        }).start();
    }

}
