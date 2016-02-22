package com.saiya.indoorposapp.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.activities.MainActivity;
import com.saiya.indoorposapp.bean.SceneInfo;
import com.saiya.indoorposapp.exceptions.UnauthorizedException;
import com.saiya.indoorposapp.tools.HttpUtils;
import com.saiya.indoorposapp.tools.PositioningResponse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 上传地图的Fragment
 */
public class UpdateMapFragment extends Fragment implements View.OnClickListener{
    /** 存储依附的MainActivity引用 */
    private MainActivity mActivity;
    private EditText edtTxt_updateMap_sceneName;
    private EditText edtTxt_updateMap_scale;
    private EditText edtTxt_updateMap_filePath;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_updatemap, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
    }

    /**
     * 初始化布局和控件
     */
    private void initView() {
        mActivity = (MainActivity)getActivity();
        edtTxt_updateMap_sceneName =
                (EditText) mActivity.findViewById(R.id.edtTxt_updateMap_sceneName);
        edtTxt_updateMap_scale = (EditText) mActivity.findViewById(R.id.edtTxt_updateMap_scale);
        edtTxt_updateMap_filePath =
                (EditText) mActivity.findViewById(R.id.edtTxt_updateMap_filePath);
        Button btn_updateMap_chooseSceneName =
                (Button) mActivity.findViewById(R.id.btn_updateMap_chooseSceneName);
        Button btn_updateMap_chooseFile =
                (Button) mActivity.findViewById(R.id.btn_updateMap_chooseFile);
        Button btn_updateMap_confirm = (Button) mActivity.findViewById(R.id.btn_updateMap_confirm);
        btn_updateMap_chooseSceneName.setOnClickListener(this);
        btn_updateMap_chooseFile.setOnClickListener(this);
        btn_updateMap_confirm.setOnClickListener(this);
        edtTxt_updateMap_sceneName.setText(R.string.activity_main_defaultScene);
        progressDialog = new ProgressDialog(mActivity);
    }

    /**
     * 点击选择场景后触发的事件
     */
    private void chooseSceneNameOnClick() {
        new MainActivity.ChooseSceneTask(mActivity,
                new MainActivity.ChooseSceneTask.OnChooseSceneListener() {
            @Override
            public void onChooseScene(SceneInfo sceneInfo) {
                edtTxt_updateMap_sceneName.setText(sceneInfo.getSceneName());
            }
        }).execute();
    }

    /**
     * 点击选择文件后触发的事件
     */
    private void chooseFileOnClick() {
        Intent intent = new Intent();
        intent.setType("image/jpg");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    //创建一个进度条对话框
    private ProgressDialog progressDialog;
    /**
     * 点击上传地图后触发的事件
     */
    private void confirmOnClick() {
        //获取用户输入的参数
        final String sceneName = edtTxt_updateMap_sceneName.getText().toString();
        String scaleStr = edtTxt_updateMap_scale.getText().toString();
        String filePath = edtTxt_updateMap_filePath.getText().toString();
        //检查参数是否合法
        if (sceneName.equals(getString(R.string.activity_main_defaultScene)) || sceneName.length() == 0 || filePath.length() == 0) {
            Toast.makeText(mActivity, R.string.fragment_updateMap_confirmFailed, Toast.LENGTH_SHORT).show();
            return;
        }
        //将比例尺转为float
        final float scale;
        if (scaleStr.length() != 0) {
            scale = Float.parseFloat(scaleStr);
        } else {
            scale = 0;
        }
        //读取图片文件到byte[]数组mapBytes
        File file = new File(filePath);
        final byte[] mapBytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            int bufSize = 1024;
            byte[] buffer = new byte[bufSize];
            int len;
            while((len = in.read(buffer,0,bufSize)) != -1)
                out.write(buffer,0,len);
            mapBytes = out.toByteArray();
            mActivity.openFileOutput(sceneName + ".jpg", Context.MODE_PRIVATE).write(mapBytes);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mActivity, R.string.fragment_updateMap_streamFailed, Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.fragment_updateMap_updating));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean uploadResult;
                try {
                    uploadResult = HttpUtils.uploadMap(sceneName, scale, mapBytes);
                    if (uploadResult) {
                        Message msg = new Message();
                        msg.obj = PositioningResponse.UPDATE_MAP_SUCCEED;
                        mActivity.getMyHandler().sendMessage(msg);
                    } else {
                        Message msg = new Message();
                        msg.obj = PositioningResponse.NETWORK_ERROR;
                        mActivity.getMyHandler().sendMessage(msg);
                    }
                } catch (UnauthorizedException e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.obj = PositioningResponse.UNAUTHORIZED;
                    mActivity.getMyHandler().sendMessage(msg);
                } finally {
                    progressDialog.dismiss();
                }
            }
        }).start();
    }

    //处理按钮被点击时触发的事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_updateMap_chooseSceneName:
                chooseSceneNameOnClick();
                break;
            case R.id.btn_updateMap_chooseFile:
                chooseFileOnClick();
                break;
            case R.id.btn_updateMap_confirm:
                confirmOnClick();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case 1:
                    Uri uri = data.getData();
                    ContentResolver cr = mActivity.getContentResolver();
                    Cursor c = cr.query(uri, null, null, null, null);
                    if (c != null) {
                        c.moveToFirst();
                        //获取图片的位置
                        edtTxt_updateMap_filePath.setText(c.getString(c.getColumnIndex("_data")));
                        c.close();
                    } else {
                        Toast.makeText(mActivity, R.string.fragment_updateMap_chooseFileFailed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        } else {
            Toast.makeText(mActivity, R.string.fragment_updateMap_chooseFileFailed, Toast.LENGTH_SHORT).show();
        }
    }
}
