package com.saiya.indoorposapp.fragments;


import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    /** 定位的场景名 */
    private String mSceneName;
    /** 定位的场景地图的比例尺 */
    private float mMapScale;
    /** 用于发起定位请求的单线程池 */
    private ExecutorService singleThreadPool;
    /** 管理图片缓存与图片加载 */
    private ImageLoader imageLoader;
    /** 发起定位请求的Runnable,使用Handler实现重复工作 */
    private Runnable mLocateRunnable = new Runnable() {
        private float[] result = new float[2];
        @Override
        public void run() {
            while (isRunning) {
                String[] wifiData = mActivity.getWifiScanResult(mNumberOfAP);
                //若WiFi关闭,结束定位,并报网络错误
                if (wifiData == null) {
                    isRunning = false;
                    Message msg = Message.obtain();
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
                    Message msg = Message.obtain();
                    msg.obj = PositioningResponse.UNAUTHORIZED;
                    mActivity.getMyHandler().sendMessage(msg);
                }
                if (result[0] == -1 || result[1] == -1) {
                    isRunning = false;
                    Message msg = Message.obtain();
                    msg.obj = PositioningResponse.NETWORK_ERROR;
                    mActivity.getMyHandler().sendMessage(msg);
                }
                mv_positioning_map.setIndicator(result[0], result[1], false);
                try {
                    Thread.sleep(mLocationInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
        singleThreadPool = Executors.newSingleThreadExecutor();
        imageLoader = new ImageLoader();
        if (mSceneName != null && mMapScale != 0f) {
            new SetMapTask().execute(new SceneInfo(mSceneName, mMapScale, 0));
        }
    }

    /**
     * 被显示时自动开始定位
     */
    @Override
    public void onStart() {
        super.onStart();
        startLocation();
    }

    /**
     * fragment不显示时停止定位
     */
    @Override
    public void onStop() {
        super.onStop();
        isRunning = false;
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
        if (mSceneName.equals("")) {
            Toast.makeText(mActivity, R.string.fragment_positioning_plzChooseScene, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(mActivity, R.string.fragment_positioning_positioningStarted, Toast.LENGTH_SHORT).show();
        if (!isRunning) {
            isRunning = true;
            singleThreadPool.submit(mLocateRunnable);
        }
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        Toast.makeText(mActivity, R.string.fragment_positioning_positioningStoped, Toast.LENGTH_SHORT).show();
        isRunning = false;
    }

    /**
     * 切换场景
     */
    private void switchScene() {
        if (!isRunning) {
            mActivity.new ChooseSceneTask(new MainActivity.OnChooseSceneListener() {
                @Override
                public void onChooseScene(SceneInfo sceneInfo) {
                    if (!sceneInfo.getSceneName().equals(mSceneName)) {
                        new SetMapTask().execute(sceneInfo);
                    }
                    mSceneName = sceneInfo.getSceneName();
                    mMapScale = sceneInfo.getScale();
                    preferences.setLastSceneName(mSceneName);
                    preferences.setLastSceneScale(mMapScale);
                }
            }).execute();
        } else {
            Toast.makeText(mActivity, R.string.fragment_positioning_positioningRunning, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置地图的异步任务
     */
    private class SetMapTask extends AsyncTask<SceneInfo, Void, Bitmap> {

        private ProgressDialog mProgressDialog;
        private float mapScale;

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
        protected Bitmap doInBackground(SceneInfo... params) {
            mapScale = params[0].getScale();
            return imageLoader.getBitmap(params[0].getSceneName(), params[0].getLastUpdateTime());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mProgressDialog.dismiss();
            if (bitmap != null) {
                mv_positioning_map.setMap(bitmap, mapScale);
            }
        }

    }

    private class ImageLoader {
        private LinkedHashMap<String, Bitmap> memoryCache;
        private int bytesUsed;
        private static final int MaxBytes = 1024 * 1024 * 50;

        ImageLoader() {
            memoryCache = new LinkedHashMap<>(8, 0.75f, true);
        }

        private void addToCache(String sceneName, Bitmap bitmap) {
            if (!memoryCache.containsKey(sceneName)) {
                memoryCache.put(sceneName, bitmap);
                bytesUsed += bitmap.getAllocationByteCount();
                checkSize();
            }
        }

        private void checkSize() {
            Iterator<Map.Entry<String, Bitmap>> iterator = memoryCache.entrySet().iterator();
            while (iterator.hasNext()) {
                if (bytesUsed > MaxBytes) {
                    Map.Entry<String, Bitmap> entry = iterator.next();
                    bytesUsed -= entry.getValue().getAllocationByteCount();
                    iterator.remove();
                } else {
                    break;
                }
            }
        }

        public Bitmap getBitmap(String sceneName, long lastUpdateTime) {
            if (!isFileUpToDate(getFile(sceneName), lastUpdateTime)) {
                return getFromInternet(sceneName);
            }
            Bitmap bitmap = getFromMemory(sceneName);
            if (bitmap != null) {
                return bitmap;
            }
            bitmap = getFromFile(sceneName);
            if (bitmap != null) {
                return bitmap;
            }
            bitmap = getFromInternet(sceneName);
            return bitmap;
        }

        private Bitmap getFromFile(String sceneName) {
            Bitmap bitmap = null;
            File mapFile = new File(mActivity.getFilesDir(), sceneName + ".jpg");
            if (mapFile.exists()) {
                try (InputStream in = new FileInputStream(mapFile)) {
                    bitmap = BitmapFactory.decodeStream(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            addToCache(sceneName, bitmap);
            return bitmap;
        }

        private Bitmap getFromInternet(String sceneName) {
            boolean isSucceed = false;
            Message msg = Message.obtain();
            try {
                isSucceed = HttpUtils.downloadMap(sceneName, mActivity);
            } catch (UnauthorizedException e) {
                e.printStackTrace();
                msg.obj = PositioningResponse.UNAUTHORIZED;
                mActivity.getMyHandler().sendMessage(msg);
            }
            if (!isSucceed) {
                msg.obj = PositioningResponse.NETWORK_ERROR;
                mActivity.getMyHandler().sendMessage(msg);
            }
            msg.obj = PositioningResponse.DOWNLOAD_MAP_SUCCEED;
            mActivity.getMyHandler().sendMessage(msg);
            return getFromFile(sceneName);
        }

        private Bitmap getFromMemory(String sceneName) {
            return memoryCache.get(sceneName);
        }

        private File getFile(String sceneName) {
            return new File(mActivity.getFilesDir(), sceneName + ".jpg");
        }

        private boolean isFileUpToDate(File file, long lastUpdateTime) {
            return !preferences.getAutoUpdateMap() || file.lastModified() > lastUpdateTime;
        }

    }

}
