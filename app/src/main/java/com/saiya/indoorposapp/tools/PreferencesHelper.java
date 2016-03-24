package com.saiya.indoorposapp.tools;

import android.content.Context;
import android.content.SharedPreferences;

import com.saiya.indoorposapp.fragments.PositioningFragment;

/**
 * 用户的SharedPreferences工具类
 */
public class PreferencesHelper {
    
    private SharedPreferences preferences;
    
    public PreferencesHelper(String userName, Context context) {
        preferences = context.getSharedPreferences(userName + "-settings", Context.MODE_PRIVATE);
    }

    /**
     * 获取自动登录的设置值
     * @return 返回true为开启自动登录,false为关闭
     */
    public boolean getAutoLogin() {
        return preferences.getBoolean("autoLogin", false);
    }

    /**
     * 设置是否自动登录
     * @param value true为开启自动登录,false为关闭
     */
    public void setAutoLogin(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("autoLogin", value);
        editor.apply();
    }

    /**
     * 获取自动更新地图的设置值
     * @return 返回true为开启自动更新,false为关闭
     */
    public boolean getAutoUpdateMap() {
        return preferences.getBoolean("autoUpdateMap", false);
    }

    /**
     * 设置是否自动更新地图
     * @param value true为开启自动更新,false为关闭
     */
    public void setAutoUpdateMap(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("autoUpdateMap", value);
        editor.apply();
    }

    /**
     * 获取上次定位的场景名称
     * @return 场景名
     */
    public String getLastSceneName() {
        return preferences.getString("lastSceneName", "");
    }

    /**
     * 设置上次定位的场景名称
     * @param value 场景名
     */
    public void setLastSceneName(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastSceneName", value);
        editor.apply();
    }

    /**
     * 获取上次定位的场景的比例尺
     * @return 比例尺
     */
    public float getLastSceneScale() {
        return preferences.getFloat("lastSceneScale", 0f);
    }

    /**
     * 设置上次定位的场景的比例尺
     * @param value 比例尺
     */
    public void setLastSceneScale(float value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("lastSceneScale", value);
        editor.apply();
    }

    /**
     * 获取定位方法的设置值
     * @return 返回值由PositioningFragment中的int常量定义
     */
    public int getLocationMethod() {
        return preferences.getInt("locationMethod", PositioningFragment.USE_WIFI_ONLY);
    }

    /**
     * 设置定位方法
     * @param value 由PositioningFragment中的int常量定义
     */
    public void setLocationMethod(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("locationMethod", value);
        editor.apply();
    }

    /**
     * 获取定位间隔的设置值
     * @return 返回定位间隔,单位为毫秒
     */
    public int getLocationInterval() {
        return preferences.getInt("locationInterval", 1000);
    }

    /**
     * 设置定位间隔
     * @param value 定位间隔,单位为毫秒
     */
    public void setLocationInterval(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("locationInterval", value);
        editor.apply();
    }

    /**
     * 获取采集指纹的次数
     * @return 返回采集指纹的次数
     */
    public int getNumberOfAcquisition() {
        return preferences.getInt("numberOfAcquisition", 10);
    }

    /**
     * 设置采集指纹的次数
     * @param value 采集指纹的次数
     */
    public void setNumberOfAcquisition(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("numberOfAcquisition", value);
        editor.apply();
    }

    /**
     * 获取定位时上传到服务器的WiFi信息个数
     * @return 返回上传到服务器的WiFi信息个数
     */
    public int getNumberOfWifiAp() {
        return preferences.getInt("numberOfWifiAp", 6);
    }

    /**
     * 设置定位时上传到服务器的WiFi信息个数
     * @param value 上传到服务器的WiFi信息个数
     */
    public void setNumberOfWifiAp(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("numberOfWifiAp", value);
        editor.apply();
    }
}
