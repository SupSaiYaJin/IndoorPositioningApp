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
import com.saiya.indoorposapp.tools.PreferencessHelper;

import java.io.File;

/**
 * 设置Fragment
 */
public class SettingsFragment extends Fragment implements View.OnClickListener, CompoundButton
        .OnCheckedChangeListener {
    
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
    private TextView tv_settings_numberOfWifiAP;
    private TextView tv_settings_numberOfAcquisition;

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
        ToggleButton tglBtn_settings_autoLogin = (ToggleButton) mActivity.findViewById(R.id.tglBtn_settings_autoLogin);
        ToggleButton tglBtn_settings_autoUpdateMap = (ToggleButton) mActivity.findViewById(R.id.tglBtn_settings_autoUpdateMap);
        TextView tv_settings_deleteMapCache = (TextView) mActivity.findViewById(R.id.tv_settings_deleteMapCache);
        tv_settings_locationMethod = (TextView) mActivity.findViewById(R.id.tv_settings_locationMethod);
        tv_settings_locationInterval = (TextView) mActivity.findViewById(R.id.tv_settings_locationInterval);
        tv_settings_numberOfWifiAP = (TextView) mActivity.findViewById(R.id.tv_settings_numberOfWifiAP);
        tv_settings_numberOfAcquisition = (TextView) mActivity.findViewById(R.id.tv_settings_numberOfAcquisition);
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
        tv_settings_numberOfWifiAP.setText(String.valueOf(preferences.getNumberOfWifiAP()));
        //设置控件监听器
        tglBtn_settings_autoLogin.setOnCheckedChangeListener(this);
        tglBtn_settings_autoUpdateMap.setOnCheckedChangeListener(this);
        tv_settings_deleteMapCache.setOnClickListener(this);
        tv_settings_locationMethod.setOnClickListener(this);
        tv_settings_locationInterval.setOnClickListener(this);
        tv_settings_numberOfWifiAP.setOnClickListener(this);
        tv_settings_numberOfAcquisition.setOnClickListener(this);
        btn_settings_logout.setOnClickListener(this);
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
                    ((PositioningFragment) mActivity.getFragment(0)).setLocationMethod(mSelectedWhich);
                }
                //若点击单选项,仅改变mSelectedWhich的值
                else
                    mSelectedWhich = which;
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
        View view = getLayoutInflater(null).inflate(R.layout.dlg_skbar, null);
        final TextView tv_dlg_locationInterval = (TextView) view.findViewById(R.id.tv_dlg);
        final SeekBar skbar_dlg_locationInterval = (SeekBar) view.findViewById(R.id.skbar_dlg);
        //构造选择定位间隔的对话框,由一个SeekBar和一个TextView组成
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.fragment_settings_locationInterval);
        builder.setView(view);
        //初始化SeekBar和TextView状态
        skbar_dlg_locationInterval.setProgress((preferences.getLocationInterval() /
                LOCATION_INTERVAL_MIN - 1) * 25);
        tv_dlg_locationInterval.setText(String.valueOf(preferences.getLocationInterval()));
        //设置SeekBar监听器
        skbar_dlg_locationInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //按移动SeekBar的情况更新TextView
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 13)
                    tv_dlg_locationInterval.setText(String.valueOf(LOCATION_INTERVAL_MIN));
                else if (progress >= 13 && progress < 38)
                    tv_dlg_locationInterval.setText(String.valueOf(LOCATION_INTERVAL_MIN * 2));
                else if (progress >= 38 && progress < 63)
                    tv_dlg_locationInterval.setText(String.valueOf(LOCATION_INTERVAL_MIN * 3));
                else if (progress >= 63 && progress < 88)
                    tv_dlg_locationInterval.setText(String.valueOf(LOCATION_INTERVAL_MIN * 4));
                else
                    tv_dlg_locationInterval.setText(String.valueOf(LOCATION_INTERVAL_MIN * 5));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //使SeekBar结束移动时只落在五分之X处,分别代表0,1,2,3,4
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 13)
                    seekBar.setProgress(0);
                else if (progress >= 13 && progress < 38)
                    seekBar.setProgress(25);
                else if (progress >= 38 && progress < 63)
                    seekBar.setProgress(50);
                else if (progress >= 63 && progress < 88)
                    seekBar.setProgress(75);
                else
                    seekBar.setProgress(100);
            }
        });
        //设置对话框的确定按钮
        builder.setPositiveButton(R.string.fragment_settings_confirm, new DialogInterface.OnClickListener() {
            //确定后将值存入SharedPrefernces,并且更新主界面TextView
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int locationInterval = (skbar_dlg_locationInterval.getProgress() / 25 + 1) * LOCATION_INTERVAL_MIN;
                preferences.setLocationInterval(locationInterval);
                tv_settings_locationInterval.setText(String.valueOf(locationInterval));
                ((PositioningFragment)  mActivity.getFragment(0)).setLocationInterval(locationInterval);
            }
        });
        builder.show();

    }

    /**
     * 点击使用AP个数设置进行的操作
     */
    private void numberOfWifiAPOnClick() {
        View view = getLayoutInflater(null).inflate(R.layout.dlg_skbar, null);
        final TextView tv_dlg_numberOfWifiAP = (TextView) view.findViewById(R.id.tv_dlg);
        final SeekBar skbar_dlg_numberOfWifiAP = (SeekBar) view.findViewById(R.id.skbar_dlg);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.fragment_settings_numberOfWifiAP);
        builder.setView(view);
        //初始化SeekBar和TextView状态
        skbar_dlg_numberOfWifiAP.setProgress((preferences.getNumberOfWifiAP() - 6) * 25);
        tv_dlg_numberOfWifiAP.setText(String.valueOf(preferences.getNumberOfWifiAP()));
        //设置SeekBar监听器
        skbar_dlg_numberOfWifiAP.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //按移动SeekBar的情况更新TextView
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 13)
                    tv_dlg_numberOfWifiAP.setText(String.valueOf(6));
                else if (progress >= 13 && progress < 38)
                    tv_dlg_numberOfWifiAP.setText(String.valueOf(7));
                else if (progress >= 38 && progress < 63)
                    tv_dlg_numberOfWifiAP.setText(String.valueOf(8));
                else if (progress >= 63 && progress < 88)
                    tv_dlg_numberOfWifiAP.setText(String.valueOf(9));
                else
                    tv_dlg_numberOfWifiAP.setText(String.valueOf(10));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //使SeekBar结束移动时只落在五分之X处,分别代表0,1,2,3,4
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 13)
                    seekBar.setProgress(0);
                else if (progress >= 13 && progress < 38)
                    seekBar.setProgress(25);
                else if (progress >= 38 && progress < 63)
                    seekBar.setProgress(50);
                else if (progress >= 63 && progress < 88)
                    seekBar.setProgress(75);
                else
                    seekBar.setProgress(100);
            }
        });
        //设置对话框的确定按钮
        builder.setPositiveButton(R.string.fragment_settings_confirm, new DialogInterface.OnClickListener
                () {
            //确定后将值存入SharedPrefernces,并且更新主界面TextView
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //得到SharedPreferences编辑器
                int numberOfWifiAP = skbar_dlg_numberOfWifiAP.getProgress() / 25 + 6;
                preferences.setNumberOfWifiAP(numberOfWifiAP);
                tv_settings_numberOfWifiAP.setText(String.valueOf(numberOfWifiAP));
                ((PositioningFragment)mActivity.getFragment(0)).setNumberOfAP(numberOfWifiAP);
            }
        });
        builder.show();
    }

    /**
     * 点击采集次数设置进行的操作
     */
    private void numberOfAcquisitionOnClick() {
        View view = getLayoutInflater(null).inflate(R.layout.dlg_skbar, null);
        final TextView tv_dlg_numberOfAcquisition = (TextView) view.findViewById(R.id.tv_dlg);
        final SeekBar skbar_dlg_numberOfAcquisition = (SeekBar) view.findViewById(R.id.skbar_dlg);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.fragment_settings_numberOfAcquisition);
        builder.setView(view);
        //初始化SeekBar和TextView状态
        skbar_dlg_numberOfAcquisition.setProgress((preferences.getNumberOfAcquisition() /
                NUMBER_OF_ACQUISITION_MIN - 1) * 25);
        tv_dlg_numberOfAcquisition.setText(String.valueOf(preferences.getNumberOfAcquisition()));
        //设置SeekBar监听器
        skbar_dlg_numberOfAcquisition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //按移动SeekBar的情况更新TextView
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 13)
                    tv_dlg_numberOfAcquisition.setText(String.valueOf(NUMBER_OF_ACQUISITION_MIN));
                else if (progress >= 13 && progress < 38)
                    tv_dlg_numberOfAcquisition.setText(String.valueOf(NUMBER_OF_ACQUISITION_MIN * 2));
                else if (progress >= 38 && progress < 63)
                    tv_dlg_numberOfAcquisition.setText(String.valueOf(NUMBER_OF_ACQUISITION_MIN * 3));
                else if (progress >= 63 && progress < 88)
                    tv_dlg_numberOfAcquisition.setText(String.valueOf(NUMBER_OF_ACQUISITION_MIN * 4));
                else
                    tv_dlg_numberOfAcquisition.setText(String.valueOf(NUMBER_OF_ACQUISITION_MIN * 5));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //使SeekBar结束移动时只落在五分之X处,分别代表0,1,2,3,4
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 13)
                    seekBar.setProgress(0);
                else if (progress >= 13 && progress < 38)
                    seekBar.setProgress(25);
                else if (progress >= 38 && progress < 63)
                    seekBar.setProgress(50);
                else if (progress >= 63 && progress < 88)
                    seekBar.setProgress(75);
                else
                    seekBar.setProgress(100);
            }
        });
        //设置对话框的确定按钮
        builder.setPositiveButton(R.string.fragment_settings_confirm, new DialogInterface.OnClickListener
                () {
            //确定后将值存入SharedPrefernces,并且更新主界面TextView
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int numberOfAcquisition = (skbar_dlg_numberOfAcquisition.getProgress() / 25 + 1) * NUMBER_OF_ACQUISITION_MIN;
                preferences.setNumberOfAcquisition(numberOfAcquisition);
                tv_settings_numberOfAcquisition.setText(String.valueOf(numberOfAcquisition));
                ((UpdateFPFragment)mActivity.getFragment(1)).setNumberOfAcquision(numberOfAcquisition);
            }
        });
        builder.show();
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
            case R.id.tv_settings_numberOfWifiAP:
                numberOfWifiAPOnClick();
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
