package com.saiya.indoorposapp.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.activities.MainActivity;
import com.saiya.indoorposapp.bean.SceneInfo;
import com.saiya.indoorposapp.exceptions.UnauthorizedException;
import com.saiya.indoorposapp.tools.HttpUtils;
import com.saiya.indoorposapp.tools.PositioningResponse;
import com.saiya.indoorposapp.tools.PreferencessHelper;
import com.saiya.indoorposapp.ui.MapView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 定位Fragment
 */
public class PositioningFragment extends Fragment implements View.OnClickListener{

    /** 存储依附的MainActivity引用 */
    private MainActivity mActivity;
    /** 采用WiFi和地磁混合定位方式 */
    public static final int USE_ALL_METHOD = 0;
    /** 采用WiFi定位方式 */
    public static final int USE_WIFI_ONLY = 1;
    /** 采用地磁定位方式 */
    public static final int USE_GEOMAGNETIC_ONLY = 2;
    /** 当前采用的定位方式 */
    private int mLocationMethod;
    /** 当前定位间隔 */
    private int mLocationInterval;
    /** 上传到服务器的WiFi指纹AP个数 */
    private int mNumberOfAP = 6;
    /** 指示定位是否正在进行中 */
    private boolean isRunning = false;
    /** 得到当前用户的设置文件 */
    private PreferencessHelper preferences;
    /** 显示地图的MapView */
    private MapView mv_positioning_map;
    /** 循环发起定位请求的Handler */
    private Handler mHandler = new Handler();
    /** 定位的场景名 */
    private String mSceneName;
    /** 定位的场景地图的比例尺 */
    private float mMapScale;
    /** 发起定位请求的Runnable,使用Handler实现重复工作 */
    private Runnable mLocateRunnable = new Runnable() {
        private float[] result = new float[2];
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String[] wifiData = mActivity.getWifiScanResult(mNumberOfAP);
                    //若WiFi关闭,结束定位,并报网络错误
                    if (wifiData == null) {
                        isRunning = false;
                        mHandler.removeCallbacks(mLocateRunnable);
                        Message msg = new Message();
                        msg.obj = PositioningResponse.NETWORK_ERROR;
                        mActivity.getMyHandler().sendMessage(msg);
                        return;
                    }
                    String mac = wifiData[0];
                    String rssi = wifiData[1];
                    float[] MagneticRSS = mActivity.getGeomagneticRSS();
                    try {
                        switch (mLocationMethod) {
                            case USE_ALL_METHOD:
                                result = HttpUtils.locateOnBoth(mSceneName, mac, rssi,
                                        MagneticRSS[0], MagneticRSS[1]);
                                break;
                            case USE_WIFI_ONLY:
                                result = HttpUtils.locateOnWifi(mSceneName, mac, rssi);
                                break;
                            case USE_GEOMAGNETIC_ONLY:
                                result = HttpUtils.locateOnGeomagnetic(mSceneName,
                                        MagneticRSS[0], MagneticRSS[1]);
                                break;
                            default:
                                break;
                        }
                    } catch (UnauthorizedException e) {
                        e.printStackTrace();
                        isRunning = false;
                        mHandler.removeCallbacks(mLocateRunnable);
                        Message msg = new Message();
                        msg.obj = PositioningResponse.UNAUTHORIZED;
                        mActivity.getMyHandler().sendMessage(msg);
                    }
                    if (result[0] == -1 || result[1] == -1) {
                        isRunning = false;
                        mHandler.removeCallbacks(mLocateRunnable);
                        Message msg = new Message();
                        msg.obj = PositioningResponse.NETWORK_ERROR;
                        mActivity.getMyHandler().sendMessage(msg);
                    }
                    mv_positioning_map.setIndicator(result[0], result[1], false);
                }
            }).start();
            mHandler.postDelayed(this, mLocationInterval);
        }
    };

    /**
     * 设置定位方法
     * @param method 定位方法
     */
    public void setLocationMethod(int method) {
        mLocationMethod = method;
    }

    /**
     * 设置定位间隔
     * @param interval 定位间隔
     */
    public void setLocationInterval(int interval) {
        mLocationInterval = interval;
    }

    /**
     * 设置定位时上传到服务器的WiFiAP个数
     * @param numberOfAP 上传到服务器的WiFiAP个数
     */
    public void setNumberOfAP(int numberOfAP) {
        mNumberOfAP = numberOfAP;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_positioning, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity)getActivity();
        mv_positioning_map = (MapView) mActivity.findViewById(R.id.mv_positioning_map);
        mv_positioning_map.setOnMovingListener(mActivity.getMyViewPager());
        preferences =  mActivity.getPreferences();
        mLocationMethod = preferences.getLocationMethod();
        mLocationInterval = preferences.getLocationInterval();
        mNumberOfAP = preferences.getNumberOfWifiAp();
        Button btn_positioning_start = (Button) mActivity.findViewById(R.id.btn_positioning_start);
        Button btn_positioning_stop = (Button) mActivity.findViewById(R.id.btn_positioning_stop);
        Button btn_positioning_switch = (Button) mActivity.findViewById(R.id.btn_positioning_switch);
        btn_positioning_start.setOnClickListener(this);
        btn_positioning_stop.setOnClickListener(this);
        btn_positioning_switch.setOnClickListener(this);
        mSceneName = preferences.getLastSceneName();
        mMapScale = preferences.getLastSceneScale();
        if (mSceneName != null && mMapScale != 0f) {
            setMap(mSceneName, 0);
        }
    }

    /**
     * 设置显示的地图
     */
    private void setMap(String sceneName, long lastUpdateTime) {
        //对应地图文件的File对象
        File mapFile = new File(mActivity.getFilesDir(), sceneName + ".jpg");
        //若文件不存在,则下载地图
        if (!mapFile.exists()) {
            new DownloadMapTask().execute(sceneName);
        } else {
            //若开启了自动更新地图,且服务器的地图较新,则下载地图
            if (preferences.getAutoUpdateMap() && mapFile.lastModified() < lastUpdateTime) {
                new DownloadMapTask().execute(sceneName);
            //否则直接读入本地地图文件
            } else {
                try (FileInputStream in = new FileInputStream(mapFile)){
                    Bitmap map = BitmapFactory.decodeStream(in);
                    //若无法解析地图文件,重新下载地图
                    if (map == null) {
                        new DownloadMapTask().execute(sceneName);
                        return;
                    }
                    mv_positioning_map.setMap(map, mMapScale);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_positioning_start:
                startLocation();
                break;
            case R.id.btn_positioning_stop:
                stopLocation();
                break;
            case R.id.btn_positioning_switch:
                switchScene();
                break;
            default:
                break;
        }
    }

    /**
     * 开始定位
     */
    private void startLocation() {
        if (!mActivity.checkWifiState()) {
            Toast.makeText(mActivity, R.string.activity_main_wifiDisabled, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(mActivity, R.string.fragment_positioning_positioningStarted, Toast.LENGTH_SHORT).show();
        if (!isRunning) {
            isRunning = true;
            mHandler.post(mLocateRunnable);
        }
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        Toast.makeText(mActivity, R.string.fragment_positioning_positioningStoped, Toast.LENGTH_SHORT).show();
        mHandler.removeCallbacks(mLocateRunnable);
        isRunning = false;
    }

    /**
     * 切换场景
     */
    private void switchScene() {
        if (!isRunning) {
            new MainActivity.ChooseSceneTask(mActivity,
                    new MainActivity.ChooseSceneTask.OnChooseSceneListener() {
                @Override
                public void onChooseScene(SceneInfo sceneInfo) {
                    preferences.setLastSceneName(sceneInfo.getSceneName());
                    preferences.setLastSceneScale(sceneInfo.getScale());
                    mSceneName = sceneInfo.getSceneName();
                    mMapScale = sceneInfo.getScale();
                    setMap(sceneInfo.getSceneName(), sceneInfo.getLastUpdateTime());

                }
            }).execute();
        } else {
            Toast.makeText(mActivity, R.string.fragment_positioning_positioningRunning, Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 下载地图的异步任务
     */
    private class DownloadMapTask extends AsyncTask<String, Void, PositioningResponse> {

        private ProgressDialog mProgressDialog;
        private byte[] mapBytes;
        @Override
        protected void onPreExecute() {
            //创建一个进度条对话框
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(mActivity.getString(R.string.fragment_positioning_downloadingMap));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected PositioningResponse doInBackground(String... params) {
            try (FileOutputStream out = mActivity
                    .openFileOutput(params[0] + ".jpg", Context.MODE_PRIVATE)){
                mapBytes = HttpUtils.downloadMap(params[0]);
                if (mapBytes == null) {
                    return PositioningResponse.NETWORK_ERROR;
                }
                out.write(mapBytes);
                out.flush();
            } catch (UnauthorizedException e) {
                e.printStackTrace();
                return PositioningResponse.UNAUTHORIZED;
            } catch (IOException e) {
                e.printStackTrace();
                return PositioningResponse.NETWORK_ERROR;
            }
            return PositioningResponse.DOWNLOAD_MAP_SUCCEED;
        }

        @Override
        protected void onPostExecute(PositioningResponse response) {
            mProgressDialog.dismiss();
            Message msg = new Message();
            msg.obj = response;
            if (response == PositioningResponse.DOWNLOAD_MAP_SUCCEED) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(mapBytes, 0, mapBytes.length);
                if (bitmap != null) {
                    mv_positioning_map.setMap(bitmap, mMapScale);
                } else {
                    msg.obj = PositioningResponse.NETWORK_ERROR;
                }
            }
            mActivity.getMyHandler().sendMessage(msg);
        }

    }

}
