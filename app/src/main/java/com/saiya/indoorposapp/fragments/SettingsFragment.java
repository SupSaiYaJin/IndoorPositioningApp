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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.activities.LoginActivity;
import com.saiya.indoorposapp.activities.MainActivity;
import com.saiya.indoorposapp.tools.HttpUtils;
import com.saiya.indoorposapp.tools.PreferencessHelper;

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

    private AlertDialog locationIntervalDialog;
    private AlertDialog numOfWifiApDialog;
    private AlertDialog numOfAcquisitionDialog;

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
        tv_settings_locationMethod.setOnClickListener(this);
        tv_settings_locationInterval.setOnClickListener(this);
        tv_settings_numberOfWifiAp.setOnClickListener(this);
        tv_settings_numberOfAcquisition.setOnClickListener(this);
        btn_settings_logout.setOnClickListener(this);
    }

    /**
     * 封装对设置结果执行的操作
     */
    private interface OnConfirmListener {
        void process(int index);
    }
    /**
     * 创建进度条对话框的工厂方法
     * @param title 对话框标题,为资源ID
     * @param original 初始值
     * @param values 可以调整的值的数组
     * @param onConfirmListener 点击确定按钮后执行的动作
     * @return 返回一个AlertDialog对象
     */
    private AlertDialog
    createProgressDialog(int title, int original, final int[] values,
                         final OnConfirmListener onConfirmListener) {
        //由数组大小算出的最小进度单元
        final float unit = 50 / (values.length - 1);
        View view = getLayoutInflater(null).inflate(R.layout.dlg_skbar, null);
        final TextView tv_dlg_status = (TextView) view.findViewById(R.id.tv_dlg);
        final SeekBar skbar_dlg = (SeekBar) view.findViewById(R.id.skbar_dlg);
        //构造选择定位间隔的对话框,由一个SeekBar和一个TextView组成
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(title);
        builder.setView(view);
        //初始化SeekBar和TextView状态
        skbar_dlg.setProgress((original / values[0] - 1) * (100 / (values.length - 1)));
        tv_dlg_status.setText(String.valueOf(original));
        //设置SeekBar监听器
        skbar_dlg.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //按移动SeekBar的情况更新TextView
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float bound = unit;
                for (int value : values) {
                    if (progress < bound) {
                        tv_dlg_status.setText(String.valueOf(value));
                        break;
                    } else {
                        bound += 2 * unit;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            //使SeekBar结束移动时只落在n分之x处
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float bound = unit;
                for(int i = 0; i < values.length; ++i) {
                    if(progress < bound) {
                        seekBar.setProgress((int) (2 * i * unit));
                        break;
                    } else {
                        bound += 2 * unit;
                    }
                }
            }
        });
        //设置对话框的确定按钮
        builder.setPositiveButton(R.string.fragment_settings_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onConfirmListener.process((int) (skbar_dlg.getProgress() / unit / 2));
            }
        });
        return builder.create();
    }
    /**
     * 点击删除地图缓存按钮进行的操作
     */
    private void deleteMapCacheOnClick() {
        File files = mActivity.getFilesDir();
        if(files != null && files.exists() && files.isDirectory()) {
            for (File item : files.listFiles()) {
                if(!item.delete()) {
                    Toast.makeText(mActivity, R.string.fragment_settings_deleteFailed, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
        Toast.makeText(mActivity, R.string.fragment_settings_deleteSuceeed, Toast.LENGTH_SHORT).show();
    }

    /**
     * 点击定位方法设置进行的操作
     */
    private void locationMethodOnClick() {
        //构造选择定位方法的对话框,由3个单选项组成
        String[] locationMethodString = new String[]{getString(R.string
                .fragment_settings_useBothMethod), getString(R.string
                .fragment_settings_wifiOnly), getString(R.string
                .fragment_settings_geomagneticOnly)};
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            /** 记录被选择的单选项序号,初始化为当前选项 */
            private int mSelectedWhich = preferences.getLocationMethod();

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //若点确定按钮,做对应的操作
                if(which == AlertDialog.BUTTON_POSITIVE) {
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
        builder.show();
    }

    /**
     * 点击定位间隔设置进行的操作
     */
    private void locationIntervalOnClick() {
        if(locationIntervalDialog == null) {
            final int[] values = new int[5];
            for(int i = 0; i < values.length; ++i) {
                values[i] = (i + 1) * LOCATION_INTERVAL_MIN;
            }
            int locationInterval = preferences.getLocationInterval();
            locationIntervalDialog = createProgressDialog(R.string.fragment_settings_locationInterval,
                    locationInterval, values, new OnConfirmListener() {
                        @Override
                        public void process(int index) {
                            preferences.setLocationInterval(values[index]);
                            tv_settings_locationInterval.setText(String.valueOf(values[index]));
                            mActivity.getPositioningFragment().setLocationInterval(values[index]);
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
    private void numberOfWifiApOnClick() {
        if(numOfWifiApDialog == null) {
            final int[] values = new int[]{6, 7, 8, 9, 10};
            int numberOfWifiAp = preferences.getNumberOfWifiAp();
            numOfWifiApDialog = createProgressDialog(R.string.fragment_settings_numberOfWifiAP,
                    numberOfWifiAp, values, new OnConfirmListener() {
                        @Override
                        public void process(int index) {
                            preferences.setNumberOfWifiAp(values[index]);
                            tv_settings_numberOfWifiAp.setText(String.valueOf(values[index]));
                            mActivity.getPositioningFragment().setNumberOfAP(values[index]);
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
    private void numberOfAcquisitionOnClick() {
        if(numOfAcquisitionDialog == null) {
            final int[] values = new int[5];
            for(int i = 0; i < values.length; ++i) {
                values[i] = (i + 1) * NUMBER_OF_ACQUISITION_MIN;
            }
            int numberOfAquisition = preferences.getNumberOfAcquisition();
            numOfAcquisitionDialog = createProgressDialog(R.string.fragment_settings_numberOfAcquisition, numberOfAquisition,
                    values, new OnConfirmListener() {
                        @Override
                        public void process(int index) {
                            preferences.setNumberOfAcquisition(values[index]);
                            tv_settings_numberOfAcquisition.setText(String.valueOf(values[index]));
                            mActivity.getUpdateFPFragment().setNumberOfAcquision(values[index]);
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
    private void logoutOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.fragment_settings_logoutTitle);
        builder.setMessage(R.string.fragment_settings_logoutMSG);
        builder.setPositiveButton(R.string.fragment_settings_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HttpUtils.logout();
                Intent intent = new Intent(mActivity, LoginActivity.class);
                intent.putExtra("allowedAutoLogin", false);
                startActivity(intent);
                mActivity.finish();
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
                deleteMapCacheOnClick();
                break;
            case R.id.tv_settings_locationMethod:
                locationMethodOnClick();
                break;
            case R.id.tv_settings_locationInterval:
                locationIntervalOnClick();
                break;
            case R.id.tv_settings_numberOfWifiAp:
                numberOfWifiApOnClick();
                break;
            case R.id.tv_settings_numberOfAcquisition:
                numberOfAcquisitionOnClick();
                break;
            case R.id.btn_settings_logout:
                logoutOnClick();
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
