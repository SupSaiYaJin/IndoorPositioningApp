package com.saiya.indoorposapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.bean.SceneInfo;
import com.saiya.indoorposapp.bean.WifiFingerprint;
import com.saiya.indoorposapp.exceptions.UnauthorizedException;
import com.saiya.indoorposapp.fragments.PositioningFragment;
import com.saiya.indoorposapp.fragments.SettingsFragment;
import com.saiya.indoorposapp.fragments.UpdateFPFragment;
import com.saiya.indoorposapp.fragments.UpdateMapFragment;
import com.saiya.indoorposapp.tools.ActivityCollector;
import com.saiya.indoorposapp.tools.HttpUtils;
import com.saiya.indoorposapp.tools.PositioningResponse;
import com.saiya.indoorposapp.tools.PreferencesHelper;
import com.saiya.indoorposapp.ui.BottomTabView;
import com.saiya.indoorposapp.ui.MyViewPager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * 主Activity,包含定位,更新指纹,更新地图,设置四个页面Fragment
 */
public class MainActivity extends FragmentActivity
        implements OnPageChangeListener, OnClickListener, SensorEventListener{

    /** 加速度阈值,超过则判定为移动了 */
    private static final float accThreshold = 1.6f;

    //用于显示Fragment
    private MyViewPager vp_main_pager;

    private FragmentPagerAdapter mAdapter;
    private List<BottomTabView> mTabIndicator = new ArrayList<>();
    private List<Fragment> mTabs = new ArrayList<>();

    /** 传感器管理器 */
    private SensorManager mSensorManager;
    /** WiFi管理器 */
    private WifiManager mWifiManager;
    /** 磁场强度,float[0]为Y方向,float[1]为Z方向 */
    private float[] mGeomagneticRSS;
    /** 用户设置信息帮助类 */
    private PreferencesHelper mPreferences;
    /** 处理异步更新UI的Handler实例 */
    private MyHandler myHandler;

    public static void start(Context context, String userName) {
        Intent starter = new Intent(context, MainActivity.class);
        starter.putExtra("username", userName);
        context.startActivity(starter);
    }

    public PreferencesHelper getPreferences() {
        return mPreferences;
    }

    public MyViewPager getMyViewPager() {
        return vp_main_pager;
    }

    public MyHandler getMyHandler() {
        return myHandler;
    }

    public PositioningFragment getPositioningFragment() {
        return (PositioningFragment) mTabs.get(0);
    }

    public UpdateFPFragment getUpdateFPFragment() {
        return (UpdateFPFragment) mTabs.get(1);
    }

    //用于在子线程更新UI的MyHandler类
    public static class MyHandler extends Handler {

        private WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        //处理更新指纹数据时返回的消息
        @Override
        public void handleMessage(Message msg) {
            if (mActivity.get() == null) {
                return;
            }
            switch ((PositioningResponse) msg.obj) {
                case UNAUTHORIZED:
                    Intent intent = new Intent("com.saiya.indoorposapp.FORCE_OFFLINE");
                    mActivity.get().sendBroadcast(intent);
                    break;
                case NETWORK_ERROR:
                    Toast.makeText(mActivity.get(), R.string.activity_common_unexpectedError,
                            Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_FP_SUCCEED:
                    Toast.makeText(mActivity.get(), R.string.fragment_updateFP_updateSucceed,
                            Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_MAP_SUCCEED:
                    Toast.makeText(mActivity.get(), R.string.fragment_updateMap_updateSucceed,
                            Toast.LENGTH_SHORT).show();
                    break;
                case DOWNLOAD_MAP_SUCCEED:
                    Toast.makeText(mActivity.get(), R.string.fragment_positioning_downloadMapSucceed,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * 封装选择场景时发生的事件
     */
    public interface OnChooseSceneListener {
        void onChooseScene(SceneInfo sceneInfo);
    }

    /**
     * 选择场景的异步任务类
     */
    public class ChooseSceneTask extends AsyncTask<Void, Void, String[]> {

        private ProgressDialog mProgressDialog;
        private List<SceneInfo> mSceneList;
        private OnChooseSceneListener mlistener;

        public ChooseSceneTask(OnChooseSceneListener listener) {
            mlistener = listener;
        }

        @Override
        protected void onPreExecute() {
            //创建一个进度条对话框
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(MainActivity.this.getString(R.string.fragment_updateFP_gettingList));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected String[] doInBackground(Void... params) {
            try {
                mSceneList = HttpUtils.getSceneList();
                if (mSceneList == null || mSceneList.size() == 0) {
                    Message msg = new Message();
                    msg.obj = PositioningResponse.NETWORK_ERROR;
                    MainActivity.this.getMyHandler().sendMessage(msg);
                }
                else {
                    String[] sceneListArray = new String[mSceneList.size()];
                    for (int i = 0; i < mSceneList.size(); ++i) {
                        sceneListArray[i] = mSceneList.get(i).getSceneName();
                    }
                    return sceneListArray;
                }
            } catch (UnauthorizedException e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.obj = PositioningResponse.UNAUTHORIZED;
                MainActivity.this.getMyHandler().sendMessage(msg);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            mProgressDialog.dismiss();
            if (strings == null) {
                return;
            }
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

                /** 记录被选择的单选项序号,初始化为-1,表示未选择 */
                private int mSelectedWhich = -1;

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //若点确定按钮,做对应的操作
                    if (which == AlertDialog.BUTTON_POSITIVE) {
                        if (mSelectedWhich != -1) {
                            SceneInfo sceneInfo = mSceneList.get(mSelectedWhich);
                            if (mlistener != null) {
                                mlistener.onChooseScene(sceneInfo);
                            }
                        }
                    //若点击单选项,仅改变mSelectedWhich的值
                    } else {
                        mSelectedWhich = which;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.fragment_updateMap_chooseSceneName);
            builder.setSingleChoiceItems(strings, -1, onClickListener);
            builder.setPositiveButton(R.string.fragment_settings_confirm, onClickListener);
            builder.show();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        /** 当前用户的用户名 */
        String username = getIntent().getStringExtra("username");
        vp_main_pager = (MyViewPager) findViewById(R.id.vp_main_pager);
        initDatas();
        vp_main_pager.setAdapter(mAdapter);
        vp_main_pager.addOnPageChangeListener(this);
        mGeomagneticRSS = new float[2];
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        myHandler = new MyHandler(this);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mPreferences = new PreferencesHelper(username, this);
        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    private void initDatas() {
        mTabs.add(new PositioningFragment());
        mTabs.add(new UpdateFPFragment());
        mTabs.add(new UpdateMapFragment());
        mTabs.add(new SettingsFragment());

        mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return mTabs.size();
            }

            @Override
            public Fragment getItem(int arg0) {
                return mTabs.get(arg0);
            }
        };

        initTabIndicator();

    }

    private void initTabIndicator() {
        BottomTabView btv_main_positioning = (BottomTabView) findViewById(R.id.btv_main_positioning);
        BottomTabView btn_main_undatefingerprint =
                (BottomTabView) findViewById(R.id.btv_main_updatefingerprint);
        BottomTabView btv_main_updatemap = (BottomTabView) findViewById(R.id.btv_main_updatemap);
        BottomTabView btv_main_settings = (BottomTabView) findViewById(R.id.btv_main_settings);

        mTabIndicator.add(btv_main_positioning);
        mTabIndicator.add(btn_main_undatefingerprint);
        mTabIndicator.add(btv_main_updatemap);
        mTabIndicator.add(btv_main_settings);

        btv_main_positioning.setOnClickListener(this);
        btn_main_undatefingerprint.setOnClickListener(this);
        btv_main_updatemap.setOnClickListener(this);
        btv_main_settings.setOnClickListener(this);

        btv_main_positioning.setIconAlpha(1.0f);
    }

    @Override
    public void onPageSelected(int arg0) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        if (positionOffset > 0) {
            BottomTabView left = mTabIndicator.get(position);
            BottomTabView right = mTabIndicator.get(position + 1);
            left.setIconAlpha(1 - positionOffset);
            right.setIconAlpha(positionOffset);
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        switch(state) {
            case ViewPager.SCROLL_STATE_IDLE:
                resetOtherTabs();
                mTabIndicator.get(vp_main_pager.getCurrentItem()).setIconAlpha(1.0f);
                break;
            default:
                break;
        }
    }

    /**
     * 重置其他的Tab
     */
    private void resetOtherTabs() {
        for (int i = 0; i < mTabIndicator.size(); i++) {
            mTabIndicator.get(i).setIconAlpha(0);
        }
    }

    /** 存储旋转矩阵的值 */
    private float[] r = new float[9];
    /** 存储大地坐标方向的加速度值 */
    private float[] fixedAccValues = new float[3];
    /** 存储地磁指纹的Y方向值和Z方向值 */
    private float[] fixedMagValues = new float[2];
    /** 用于存储加速度传感器的值 */
    private float[] aValues = new float[3];
    /** 用于存储地磁传感器的值 */
    private float[] mValues = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                aValues[0] = event.values[0];
                aValues[1] = event.values[1];
                aValues[2] = event.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mValues[0] = event.values[0];
                mValues[1] = event.values[1];
                mValues[2] = event.values[2];
                break;
            default:
                break;
        }
        if (SensorManager.getRotationMatrix(r, null, aValues, mValues)) {
            fixedAccValues[0] = r[0] * aValues[0] + r[1] * aValues[1] + r[2] * aValues[2];
            fixedAccValues[1] = r[3] * aValues[0] + r[4] * aValues[1] + r[5] * aValues[2];
            fixedAccValues[2] = r[6] * aValues[0] + r[7] * aValues[1] + r[8] * aValues[2];
            fixedMagValues[0] = r[3] * mValues[0] + r[4] * mValues[1] + r[5] * mValues[2];
            fixedMagValues[1] = r[6] * mValues[0] + r[7] * mValues[1] + r[8] * mValues[2];
        }
        if (fixedAccValues[0] + fixedAccValues[1] + fixedAccValues[2] > accThreshold) {
            isMoved = true;
        }
        MainActivity.this.setGeomagneticRSS(fixedMagValues[0], fixedMagValues[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /** 指示是否需要进行定位 */
    private boolean isMoved;
    public boolean isMoved() {
        return isMoved;
    }

    @Override
    public void onClick(View v) {
        
        switch (v.getId()) {
            case R.id.btv_main_positioning:
                vp_main_pager.setCurrentItem(0, true);
                break;
            case R.id.btv_main_updatefingerprint:
                vp_main_pager.setCurrentItem(1, true);
                break;
            case R.id.btv_main_updatemap:
                vp_main_pager.setCurrentItem(2, true);
                break;
            case R.id.btv_main_settings:
                vp_main_pager.setCurrentItem(3, true);
            default:
                break;
        }

    }

    /**
     * 检查WiFi状态
     * @return WiFi开启则返回true
     */
    public boolean checkWifiState() {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            return true;
        } else {
            Toast.makeText(this, R.string.activity_main_wifiDisabled, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private String[] wifiScanResultOfN = new String[2];
    /**
     * 获取信号强度最强的前n个WiFi信息
     * @param n 指定获取WiFi信息的最大个数,
     * @return 返回String[],其中mac字段存储了MAC地址,rssi字段存储了RSSI数据,都是用逗号拼接的String.
     * 若WiFi未打开返回null.
     */
    public String[] getWifiScanResult(int n) {
        //若未开WiFi返回null
        if (!checkWifiState()) {
            return null;
        }
        mWifiManager.startScan();
        List<ScanResult> scanResultList = mWifiManager.getScanResults();
        Collections.sort(scanResultList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                if (lhs.level > rhs.level)
                    return -1;
                else if (lhs.level < rhs.level)
                    return 1;
                else
                    return 0;
            }
        });
        StringBuilder mac = new StringBuilder();
        StringBuilder rssi = new StringBuilder();
        for (int i = 0; i < n && i < scanResultList.size(); ++i) {
            mac.append(scanResultList.get(i).BSSID).append(",");
            rssi.append(scanResultList.get(i).level).append(",");
        }
        mac.deleteCharAt(mac.length() - 1);
        rssi.deleteCharAt(rssi.length() - 1);
        wifiScanResultOfN[0] = mac.toString();
        wifiScanResultOfN[1] = rssi.toString();
        return wifiScanResultOfN;
    }

    List<WifiFingerprint> allWifiScanResult = new LinkedList<>();
    /**
     * 获取所有WiFi信号信息,String为MAC地址,Float为信号强度
     * @return 返回存储了WiFi信号信息的Map
     */
    public List<WifiFingerprint> getWifiScanResult() {
        allWifiScanResult.clear();
        if (!checkWifiState()) {
            return null;
        }
        mWifiManager.startScan();
        List<ScanResult> scanResultList = mWifiManager.getScanResults();
        for (ScanResult scanResult : scanResultList) {
            allWifiScanResult.add(new WifiFingerprint(scanResult.BSSID, (float) scanResult.level));
        }
        return allWifiScanResult;
    }

    public float[] getGeomagneticRSS() {
        return mGeomagneticRSS;
    }

    public void setGeomagneticRSS(float geomagnetic_y, float geomagnetic_z) {
        mGeomagneticRSS[0] = geomagnetic_y;
        mGeomagneticRSS[1] = geomagnetic_z;
    }

}
