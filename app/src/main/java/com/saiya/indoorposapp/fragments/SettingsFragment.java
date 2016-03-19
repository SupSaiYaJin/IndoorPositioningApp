package com.saiya.indoorposapp.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.activities.LoginActivity;
import com.saiya.indoorposapp.activities.MainActivity;
import com.saiya.indoorposapp.tools.HttpUtils;
import com.saiya.indoorposapp.tools.PreferencessHelper;
import com.saiya.indoorposapp.ui.SeekbarSettingDialog;

import java.io.File;

/**
 * 设置Fragment
 */
public class SettingsFragment extends Fragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    /** 存储依附的MainActivity引用 */
    private MainActivity mActivity;
    /** 最小定位间隔 */
    private final static int LOCATION_INTERVAL_MIN = 500;
    /** 用户设置帮助对象 */
    private PreferencessHelper preferences;
    /** 最小采集次数 */
    private final static int NUMBER_OF_ACQUISITION_MIN = 5;

    private TextView tv_settings_locationMethod;
    private TextView tv_settings_locationInterval;
    private TextView tv_settings_numberOfWifiAp;
    private TextView tv_settings_numberOfAcquisition;

    private AlertDialog locationMethodDialog;
    private SeekbarSettingDialog locationIntervalDialog;
    private SeekbarSettingDialog numOfWifiApDialog;
    private SeekbarSettingDialog numOfAcquisitionDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
    }

    /**
     * 初始化布局与资源文件
     */
    private void initView() {
        mActivity = (MainActivity)getActivity();
        //在布局中找到控件
        ToggleButton tglBtn_settings_autoLogin =
                (ToggleButton) mActivity.findViewById(R.id.tglBtn_settings_autoLogin);
        ToggleButton tglBtn_settings_autoUpdateMap =
                (ToggleButton) mActivity.findViewById(R.id.tglBtn_settings_autoUpdateMap);

        TextView tv_settings_deleteMapCache =
                (TextView) mActivity.findViewById(R.id.tv_settings_deleteMapCache);

        tv_settings_locationMethod =
                (TextView) mActivity.findViewById(R.id.tv_settings_locationMethod);
        tv_settings_locationInterval =
                (TextView) mActivity.findViewById(R.id.tv_settings_locationInterval);
        tv_settings_numberOfWifiAp =
                (TextView) mActivity.findViewById(R.id.tv_settings_numberOfWifiAp);
        tv_settings_numberOfAcquisition =
                (TextView) mActivity.findViewById(R.id.tv_settings_numberOfAcquisition);

        RelativeLayout rl_settings_locationMethod =
                (RelativeLayout) mActivity.findViewById(R.id.rl_settings_locationMethod);
        RelativeLayout rl_settings_locationInterval =
                (RelativeLayout) mActivity.findViewById(R.id.rl_settings_locationInterval);
        RelativeLayout rl_settings_numberOfWifiAp =
                (RelativeLayout) mActivity.findViewById(R.id.rl_settings_numberOfWifiAp);
        RelativeLayout rl_settings_numberOfAcquisition =
                (RelativeLayout) mActivity.findViewById(R.id.rl_settings_numberOfAcquisition);

        Button btn_settings_logout = (Button) mActivity.findViewById(R.id.btn_settings_logout);
        //得到用户设置类对象
        preferences = mActivity.getPreferences();
        //由SharedPreferences中的信息初始化ToggleButton状态
        tglBtn_settings_autoLogin.setChecked(preferences.getAutoLogin());
        tglBtn_settings_autoUpdateMap.setChecked(preferences.getAutoUpdateMap());

        //由SharedPreferences中的信息初始化TextView状态
        switch(preferences.getLocationMethod()) {
            case PositioningFragment.USE_ALL_METHOD:
                tv_settings_locationMethod.setText(R.string.fragment_settings_useBothMethod);
                break;
            case PositioningFragment.USE_WIFI_ONLY:
                tv_settings_locationMethod.setText(R.string.fragment_settings_wifiOnly);
                break;
            case PositioningFragment.USE_GEOMAGNETIC_ONLY:
                tv_settings_locationMethod.setText(R.string.fragment_settings_geomagneticOnly);
                break;
            default:
                break;
        }
        tv_settings_locationInterval.setText(String.valueOf(preferences.getLocationInterval()));
        tv_settings_numberOfAcquisition.setText(String.valueOf(preferences.getNumberOfAcquisition()));
        tv_settings_numberOfWifiAp.setText(String.valueOf(preferences.getNumberOfWifiAp()));
        //设置控件监听器
        tglBtn_settings_autoLogin.setOnCheckedChangeListener(this);
        tglBtn_settings_autoUpdateMap.setOnCheckedChangeListener(this);
        tv_settings_deleteMapCache.setOnClickListener(this);
        rl_settings_locationMethod.setOnClickListener(this);
        rl_settings_locationInterval.setOnClickListener(this);
        rl_settings_numberOfWifiAp.setOnClickListener(this);
        rl_settings_numberOfAcquisition.setOnClickListener(this);
        btn_settings_logout.setOnClickListener(this);
    }


    /**
     * 点击删除地图缓存按钮进行的操作
     */
    private void deleteMapCache() {
        File files = mActivity.getFilesDir();
        if (files != null && files.exists() && files.isDirectory()) {
            for (File item : files.listFiles()) {
                if (item.isFile() && !item.delete()) {
                    Toast.makeText(mActivity, R.string.fragment_settings_deleteFailed, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        Toast.makeText(mActivity, R.string.fragment_settings_deleteSuceeed, Toast.LENGTH_SHORT).show();
    }

    /**
     * 点击定位方法设置进行的操作
     */
    private void changeLocationMethod() {
        if (locationMethodDialog == null) {
            //构造选择定位方法的对话框,由3个单选项组成
            String[] locationMethodString = new String[]{getString(R.string
                    .fragment_settings_useBothMethod), getString(R.string
                    .fragment_settings_wifiOnly), getString(R.string
                    .fragment_settings_geomagneticOnly)};
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                /**
                 * 记录被选择的单选项序号,初始化为当前选项
                 */
                private int mSelectedWhich = preferences.getLocationMethod();

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //若点确定按钮,做对应的操作
                    if (which == AlertDialog.BUTTON_POSITIVE) {
                        switch (mSelectedWhich) {
                            case PositioningFragment.USE_ALL_METHOD:
                                tv_settings_locationMethod.setText(R.string.fragment_settings_useBothMethod);
                                break;
                            case PositioningFragment.USE_WIFI_ONLY:
                                tv_settings_locationMethod.setText(R.string.fragment_settings_wifiOnly);
                                break;
                            case PositioningFragment.USE_GEOMAGNETIC_ONLY:
                                tv_settings_locationMethod.setText(R.string.fragment_settings_geomagneticOnly);
                                break;
                            default:
                                break;
                        }
                        preferences.setLocationMethod(mSelectedWhich);
                        mActivity.getPositioningFragment().setLocationMethod(mSelectedWhich);
                        //若点击单选项,仅改变mSelectedWhich的值
                    } else {
                        mSelectedWhich = which;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(R.string.fragment_settings_locationMethod);
            builder.setSingleChoiceItems(locationMethodString, preferences.getLocationMethod(), onClickListener);
            builder.setPositiveButton(R.string.fragment_settings_confirm, onClickListener);
            locationMethodDialog = builder.create();
            locationMethodDialog.show();
        } else {
            locationMethodDialog.show();
        }
    }

    /**
     * 点击定位间隔设置进行的操作
     */
    private void changeLocationInterval() {
        if (locationIntervalDialog == null) {
            int locationInterval = preferences.getLocationInterval();
            int oriIndex = locationInterval / LOCATION_INTERVAL_MIN - 1;
            int[] values = new int[5];
            for (int i = 0; i < values.length; ++i) {
                values[i] = (i + 1) * LOCATION_INTERVAL_MIN;
            }
            locationIntervalDialog = new SeekbarSettingDialog(mActivity);
            locationIntervalDialog.setProperties(R.string.fragment_settings_locationInterval,
                    oriIndex, values, new SeekbarSettingDialog.OnConfirmListener() {
                        @Override
                        public void process(int value) {
                            preferences.setLocationInterval(value);
                            tv_settings_locationInterval.setText(String.valueOf(value));
                            mActivity.getPositioningFragment().setLocationInterval(value);
                        }
                    });
            locationIntervalDialog.show();
        } else {
            locationIntervalDialog.show();
        }
    }

    /**
     * 点击使用AP个数设置进行的操作
     */
    private void changeNumberOfWifiAp() {
        if (numOfWifiApDialog == null) {
            int numberOfWifiAp = preferences.getNumberOfWifiAp();
            int[] values = new int[]{6, 7, 8, 9, 10};
            int oriIndex = numberOfWifiAp - values[0];
            numOfWifiApDialog = new SeekbarSettingDialog(mActivity);
            numOfWifiApDialog.setProperties(R.string.fragment_settings_numberOfWifiAP,
                    oriIndex, values, new SeekbarSettingDialog.OnConfirmListener() {
                        @Override
                        public void process(int value) {
                            preferences.setNumberOfWifiAp(value);
                            tv_settings_numberOfWifiAp.setText(String.valueOf(value));
                            mActivity.getPositioningFragment().setNumberOfAP(value);
                        }
                    });
            numOfWifiApDialog.show();
        } else {
            numOfWifiApDialog.show();
        }
    }

    /**
     * 点击采集次数设置进行的操作
     */
    private void changeNumberOfAcquisition() {
        if (numOfAcquisitionDialog == null) {
            int numberOfAquisition = preferences.getNumberOfAcquisition();
            int oriIndex = numberOfAquisition / NUMBER_OF_ACQUISITION_MIN - 1;
            int[] values = new int[5];
            for (int i = 0; i < values.length; ++i) {
                values[i] = (i + 1) * NUMBER_OF_ACQUISITION_MIN;
            }
            numOfAcquisitionDialog = new SeekbarSettingDialog(mActivity);
            numOfAcquisitionDialog.setProperties(R.string.fragment_settings_numberOfAcquisition, oriIndex,
                    values, new SeekbarSettingDialog.OnConfirmListener() {
                        @Override
                        public void process(int value) {
                            preferences.setNumberOfAcquisition(value);
                            tv_settings_numberOfAcquisition.setText(String.valueOf(value));
                            mActivity.getUpdateFPFragment().setNumberOfAcquision(value);
                        }
                    });
            numOfAcquisitionDialog.show();
        } else {
            numOfAcquisitionDialog.show();
        }
    }

    /**
     * 点击注销按钮时进行的操作
     */
    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.fragment_settings_logoutTitle);
        builder.setMessage(R.string.fragment_settings_logoutMSG);
        builder.setPositiveButton(R.string.fragment_settings_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpUtils.logout();
                    }
                }).start();
                mActivity.finish();
                Intent intent = new Intent(mActivity, LoginActivity.class);
                intent.putExtra("allowedAutoLogin", false);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.fragment_settings_cancel, null);
        builder.show();
    }

    //处理控件被点击时的事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_settings_deleteMapCache:
                deleteMapCache();
                break;
            case R.id.rl_settings_locationMethod:
                changeLocationMethod();
                break;
            case R.id.rl_settings_locationInterval:
                changeLocationInterval();
                break;
            case R.id.rl_settings_numberOfWifiAp:
                changeNumberOfWifiAp();
                break;
            case R.id.rl_settings_numberOfAcquisition:
                changeNumberOfAcquisition();
                break;
            case R.id.btn_settings_logout:
                logout();
                break;
            default:
                break;
        }
    }

    //处理ToggleButton状态改变时的事件
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()) {
            case R.id.tglBtn_settings_autoLogin:
                preferences.setAutoLogin(isChecked);
                break;
            case R.id.tglBtn_settings_autoUpdateMap:
                preferences.setAutoUpdateMap(isChecked);
                break;
        }
    }

}
