package com.saiya.indoorposapp.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    }

    /**
     * 计算图片压缩比
     * @param options 带有原图片的信息
     * @param reqWidth 希望压缩的宽度
     * @param reqHeight 希望压缩的高度
     * @return 返回图片压缩比
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        //源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            //计算出实际宽高和目标宽高的比率
            //选择宽和高中最小的比率作为inSampleSize的值,这样可以保证最终图片的宽和高都会大于等于目标的宽和高.
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }


    /**
     * 点击选择场景后触发的事件
     */
    private void chooseSceneNameOnClick() {
        mActivity. new ChooseSceneTask(new MainActivity.OnChooseSceneListener() {
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

    class UpdateMapTask extends AsyncTask<String, Void, PositioningResponse> {

        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(getString(R.string.fragment_updateMap_updating));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected PositioningResponse doInBackground(String... params) {
            //将比例尺转为float
            float scale;
            if (params[1].length() != 0) {
                scale = Float.parseFloat(params[1]);
            } else {
                scale = 0;
            }
            File file = new File(params[2]);
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(file);
                out = mActivity.openFileOutput(params[0] + ".jpg", Context.MODE_PRIVATE);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(in, null, opt);
                in.close();
                opt.inSampleSize = calculateInSampleSize(opt, 600, 1000);
                opt.inJustDecodeBounds = false;
                in = new FileInputStream(file);
                BitmapFactory.decodeStream(in, null, opt).compress(Bitmap.CompressFormat.JPEG, 80, out);
                scale /= opt.inSampleSize;
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(mActivity, R.string.fragment_updateMap_streamFailed, Toast.LENGTH_SHORT).show();
                return null;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            boolean uploadResult;
            try (InputStream fileInputStream = mActivity.openFileInput(params[0] + ".jpg")){
                uploadResult = HttpUtils.uploadMap(params[0], scale, fileInputStream);
                if (uploadResult) {
                    return PositioningResponse.UPDATE_MAP_SUCCEED;
                } else {
                    return PositioningResponse.NETWORK_ERROR;
                }
            } catch (UnauthorizedException e) {
                e.printStackTrace();
                return PositioningResponse.UNAUTHORIZED;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(PositioningResponse response) {
            if (response != null) {
                Message msg = Message.obtain();
                msg.obj = response;
                mActivity.getMyHandler().sendMessage(msg);
            }
            mProgressDialog.dismiss();
        }
    }
    /**
     * 点击上传地图后触发的事件
     */
    private void confirmOnClick() {
        //获取用户输入的参数
        String sceneName = edtTxt_updateMap_sceneName.getText().toString();
        String scaleStr = edtTxt_updateMap_scale.getText().toString();
        String filePath = edtTxt_updateMap_filePath.getText().toString();
        //检查参数是否合法
        if (sceneName.equals("") || sceneName.length() == 0 || filePath.length() == 0) {
            Toast.makeText(mActivity, R.string.fragment_updateMap_confirmFailed, Toast.LENGTH_SHORT).show();
        } else {
            new UpdateMapTask().execute(sceneName, scaleStr, filePath);
        }
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
