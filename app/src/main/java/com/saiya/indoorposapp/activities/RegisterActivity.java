package com.saiya.indoorposapp.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
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
 * 用户注册Activity
 */
public class RegisterActivity extends Activity implements View.OnClickListener{

    /** 未知错误 */
    public static final int UNEXPECTED_ERROR = -1;

    /** 注册成功 */
    public static final int REGISTER_SUCCEED = 3;

    /** 用户名重复 */
    public static final int DUPLICATE_USERNAME = 4;

    //声明控件
    private EditText edtTxt_register_username;
    private EditText edtTxt_register_password;
    private MyHandler myHandler = new MyHandler(this);

    public static class MyHandler extends Handler {

        private WeakReference<RegisterActivity> mActivity;

        public MyHandler(RegisterActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        //处理注册时发回的消息
        @Override
        public void handleMessage(Message msg) {
            if(mActivity.get() == null)
                return;
            switch (msg.what) {
                case REGISTER_SUCCEED:
                    Toast.makeText(mActivity.get(), R.string.activity_register_succeed, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(mActivity.get(), LoginActivity.class);
                    mActivity.get().startActivity(intent);
                    mActivity.get().finish();
                    break;
                case DUPLICATE_USERNAME:
                    Toast.makeText(mActivity.get(), R.string.activity_register_duplicateUsername, Toast.LENGTH_SHORT).show();
                    break;
                case UNEXPECTED_ERROR:
                    Toast.makeText(mActivity.get(), R.string.activity_common_unexpectedError, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
        ActivityCollector.addActivity(this);
    }

    /**
     * 初始化试图并找到控件
     */
    private void initView() {
        Button btn_register_confirm = (Button) findViewById(R.id.btn_register_confirm);
        edtTxt_register_username = (EditText) findViewById(R.id.edtTxt_register_username);
        edtTxt_register_password = (EditText) findViewById(R.id.edtTxt_register_password);
        btn_register_confirm.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    //控制点击确定按钮的事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_register_confirm:
                final String username = edtTxt_register_username.getText().toString();
                final String password = edtTxt_register_password.getText().toString();
                if(username.length() == 0 || password.length() == 0) {
                    Toast.makeText(this, R.string.activity_common_invalidInput, Toast.LENGTH_SHORT).show();
                    break;
                }
                if(username.length() > 20 || password.length() > 20) {
                    Toast.makeText(this, R.string.activity_common_oversizeInput, Toast.LENGTH_SHORT).show();
                    break;
                }
                //显示进度对话框,得到服务器响应后解除
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage(getString(R.string.activity_register_registering));
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what = HttpUtils.register(username, password);
                        myHandler.sendMessage(msg);
                        progressDialog.dismiss();
                    }
                }).start();
                break;
            default:
                break;
        }
    }
}
