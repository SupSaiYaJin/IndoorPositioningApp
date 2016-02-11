package com.saiya.indoorposapp.fragments;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.activities.MainActivity;
import com.saiya.indoorposapp.bean.WifiFingerprint;
import com.saiya.indoorposapp.exceptions.UnauthorizedException;
import com.saiya.indoorposapp.tools.Algorithms;
import com.saiya.indoorposapp.tools.HttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上传指纹信息Fragment
 */
public class UpdateFPFragment extends Fragment implements View.OnClickListener{

    /** 存储依附的MainActivity引用 */
    private MainActivity mActivity;
    /** 更新WiFi指纹的AP数 */
    private static final int NUMBER_OF_UPDATE_WIFIAP = 10;
    /** 显示要更新的场景名称 */
    private TextView tv_updateFP_sceneName;
    /** 输入更新地点X坐标的EditText */
    private EditText edtTxt_updateFP_coorX;
    /** 输入更新地点Y坐标的EditText */
    private EditText edtTxt_updateFP_coorY;

    /** 每个地点采集指纹的次数 */
    private int numberOfAcquision;

    /**
     * 设置采集指纹的次数
     * @param numberOfAcquision 新的采集次数
     */
    public void setNumberOfAcquision(int numberOfAcquision) {
        this.numberOfAcquision = numberOfAcquision;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_updatefp, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
    }

    /**
     * 初始化布局以及控件状态
     */
    private void initView() {
        mActivity = (MainActivity)getActivity();
        edtTxt_updateFP_coorX = (EditText) mActivity.findViewById(R.id.edtTxt_updateFP_coorX);
        edtTxt_updateFP_coorY = (EditText) mActivity.findViewById(R.id.edtTxt_updateFP_coorY);
        tv_updateFP_sceneName = (TextView) mActivity.findViewById(R.id.tv_updateFP_sceneName);
        Button btn_updateFingerprint_wifi = (Button) mActivity.findViewById(R.id.btn_updateFP_wifi);
        Button btn_updateFingerprint_geomagnetic =
                (Button) mActivity.findViewById(R.id.btn_updateFP_geomagnetic);
        tv_updateFP_sceneName.setOnClickListener(this);
        btn_updateFingerprint_wifi.setOnClickListener(this);
        btn_updateFingerprint_geomagnetic.setOnClickListener(this);
        tv_updateFP_sceneName.setText(R.string.activity_main_defaultScene);
        numberOfAcquision = mActivity.getPreferences().getNumberOfAcquisition();
    }

    /**
     * 点击选择场景名后触发的事件
     */
    private void chooseSceneOnClick() {
        new MainActivity.ChooseSceneTask(mActivity) {
            @Override
            protected void onChooseScene(String sceneName, float mapScale) {
                tv_updateFP_sceneName.setText(sceneName);
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.tv_updateFP_sceneName) {
            chooseSceneOnClick();
            return;
        }
        float location_x;
        float location_y;
        try {
            location_x = Float.parseFloat(edtTxt_updateFP_coorX.getText().toString());
            location_y = Float.parseFloat(edtTxt_updateFP_coorY.getText().toString());
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(mActivity, R.string.fragment_updateFP_invalidCoord, Toast.LENGTH_SHORT).show();
            return;
        }
        if(tv_updateFP_sceneName.getText().equals(getString(R.string.activity_main_defaultScene))) {
            Toast.makeText(mActivity, R.string.fragment_updateFP_invalidCoord, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.btn_updateFP_wifi:
                new UpdateWifiFPTask().execute(location_x, location_y);
                break;
            case R.id.btn_updateFP_geomagnetic:
                new UpdateGeoFPTask().execute(location_x, location_y);
                break;
            default:
                break;
        }
    }
    /**
     * 更新WiFi指纹数据的异步任务类,需要提供当前地点的X，Y坐标
     */

    private class UpdateWifiFPTask extends AsyncTask<Float, Integer, Integer> {
        private ProgressDialog progressDialog;

        private String sceneName;

        @Override
        protected void onPreExecute() {
            sceneName = tv_updateFP_sceneName.getText().toString();
            progressDialog = new ProgressDialog(mActivity);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(getString(R.string.fragment_updateFP_collectingWifi));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMax(numberOfAcquision);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Float... params) {
            /** 存储所有WiFi强度信息,String为MAC地址,float数组第一位为多次RSS值的和,第二位为扫描到的次数 */
            Map<String, float[]> wifiScanResultSum = new HashMap<>();
            //进行numberOfAcquision次采集,并更新进度条
            for(int i = 0; i < numberOfAcquision; ++i) {
                publishProgress(i + 1);
                List<WifiFingerprint> wifiScanResult = mActivity.getWifiScanResult();
                for(WifiFingerprint wifiFingerprint : wifiScanResult) {
                    String mac = wifiFingerprint.getMac();
                    float rssi = wifiFingerprint.getRssi();
                    if(wifiScanResultSum.containsKey(mac)) {
                        float rssiSum = wifiScanResultSum.get(mac)[0];
                        float count = wifiScanResultSum.get(mac)[1];
                        wifiScanResultSum.put(mac, new float[]{rssiSum + rssi, count + 1});
                    } else {
                        wifiScanResultSum.put(mac, new float[]{rssi, 1});
                    }
                }
                if(i != numberOfAcquision - 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            /** 存储平均后的扫描结果 */
            List<WifiFingerprint> wifiScanResultAveraged = new ArrayList<>();
            //将WiFi扫描结果取平均
            for(Map.Entry<String, float[]> entry : wifiScanResultSum.entrySet())
                wifiScanResultAveraged.add(new WifiFingerprint
                        (entry.getKey(), entry.getValue()[0] / entry.getValue()[1]));
            //将WiFi扫描结果的前N强的信息取出
            Algorithms.findKStrongestRSSI(wifiScanResultAveraged,
                    0, wifiScanResultAveraged.size() - 1, NUMBER_OF_UPDATE_WIFIAP);
            //将WiFi扫描结果转为String以发送到服务器
            StringBuilder mac = new StringBuilder();
            StringBuilder rssi = new StringBuilder();
            for(int i = 0; i < NUMBER_OF_UPDATE_WIFIAP && i < wifiScanResultAveraged.size(); ++i) {
                mac.append(wifiScanResultAveraged.get(i).getMac()).append(",");
                rssi.append(wifiScanResultAveraged.get(i).getRssi()).append(",");
            }
            mac.deleteCharAt(mac.length() - 1);
            rssi.deleteCharAt(rssi.length() - 1);
            //向服务器发送WiFi信息,结果由myHandler处理
            boolean updateResult;
            try {
                updateResult = HttpUtils.updateWifiFingerprint
                        (sceneName, params[0], params[1], mac.toString(), rssi.toString());
            } catch (UnauthorizedException e) {
                e.printStackTrace();
                return MainActivity.UNAUTHORIZED;
            }
            if(updateResult) {
                return MainActivity.UPDATE_FP_SUCCEED;
            } else {
                return MainActivity.NETWORK_ERROR;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if(values[0] == numberOfAcquision) {
                progressDialog.setMessage(getString(R.string.fragment_updateFP_updating));
            }
            progressDialog.setProgress(values[0]);
        }
        @Override
        protected void onPostExecute(Integer integer) {
            progressDialog.dismiss();
            Message msg = new Message();
            msg.what = integer;
            mActivity.getMyHandler().sendMessage(msg);
        }

    }
    /**
     * 更新地磁指纹的异步任务,需要提供当前地点的X,Y坐标
     */

    private class UpdateGeoFPTask extends AsyncTask<Float, Integer, Integer> {
        private ProgressDialog progressDialog;
        private String sceneName;

        @Override
        protected void onPreExecute() {
            //获取场景名
            sceneName = tv_updateFP_sceneName.getText().toString();
            //创建一个进度条对话框
            progressDialog = new ProgressDialog(mActivity);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(getString(R.string.fragment_updateFP_collectingGeo));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMax(numberOfAcquision);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Float... params) {
            //存放采集的地磁指纹信息
            final float[] geomagneticResult = new float[2];
            //进行numberOfAcquision次采集,并更新进度条
            for(int i = 0; i < numberOfAcquision; ++i) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                float[] geo = mActivity.getGeomagneticRSS();
                geomagneticResult[0] += geo[0];
                geomagneticResult[1] += geo[1];
                publishProgress(i + 1);
            }
            //将地磁扫描结果取平均
            geomagneticResult[0] /= numberOfAcquision;
            geomagneticResult[1] /= numberOfAcquision;
            //向服务器发送地磁指纹信息,结果由myHandler处理
            boolean updateResult;
            try {
            updateResult = HttpUtils.updateGeomagneticFingerprint(sceneName, params[0], params[1], geomagneticResult[0], geomagneticResult[1]);
            } catch (UnauthorizedException e) {
                e.printStackTrace();
                return MainActivity.UNAUTHORIZED;
            }
            if(updateResult) {
                return MainActivity.UPDATE_FP_SUCCEED;
            } else {
                return MainActivity.NETWORK_ERROR;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if(values[0] == numberOfAcquision) {
                progressDialog.setMessage(getString(R.string.fragment_updateFP_updating));
            }
            progressDialog.setProgress(values[0]);
        }
        @Override
        protected void onPostExecute(Integer integer) {
            progressDialog.dismiss();
            Message msg = new Message();
            msg.what = integer;
            mActivity.getMyHandler().sendMessage(msg);
        }

    }

}
