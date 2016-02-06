package com.saiya.indoorposapp.tools;

import com.saiya.indoorposapp.bean.SceneInfo;
import com.saiya.indoorposapp.exceptions.UnauthorizedException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理JSON数据的帮助类
 */
public class JSONHelper {

    /**
     * 解析注册响应码
     * @param jsonResponse 服务器对注册请求的响应字符串
     * @return 注册响应码
     */
    public static int getRegisterResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            return jsonObject.getInt("registerResult");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 解析登录响应码
     * @param jsonResponse 服务器对登录请求的响应字符串
     * @return 登录响应码
     */
    public static int getLoginResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            return jsonObject.getInt("loginResult");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 解析获取地图列表的响应
     * @param jsonResponse 服务器对获取地图列表请求的响应字符串
     * @return 返回存储SceneInfo对象的List,信息包含场景名称,场景比例尺的值,场景的上次更新时间
     */
    public static List<SceneInfo> getSceneListResponse(String jsonResponse) throws UnauthorizedException {
        if(jsonResponse.equals("{\"authorized\":false}"))
            throw new UnauthorizedException();
        List<SceneInfo> result = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray jsonArray = jsonObject.getJSONArray("maps");
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject mapInfo = jsonArray.getJSONObject(i);
                result.add(new SceneInfo(mapInfo.getString("sceneName"), (float) mapInfo.getDouble("scale"), mapInfo.getLong("lastUpdateTime")));
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 解析定位的响应
     * @param jsonResponse 服务器对定位请求的响应字符串
     * @return 返回float[],float[0]为X坐标值,float[1]为Y坐标值
     */
    public static float[] getLocateResponse(String jsonResponse) throws UnauthorizedException {
        if(jsonResponse.equals("{\"authorized\":false}"))
            throw new UnauthorizedException();
        float[] result = new float[]{-1, -1};
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            result[0] = (float) jsonObject.getDouble("result_x");
            result[1] = (float) jsonObject.getDouble("result_y");
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 解析更新指纹数据的响应
     * @param jsonResponse 服务器对更新指纹数据请求的响应字符串
     * @return 返回true为更新成功,false为更新失败
     */
    public static boolean getUpdateResponse(String jsonResponse) throws UnauthorizedException {
        if(jsonResponse.equals("{\"authorized\":false}"))
            throw new UnauthorizedException();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            return jsonObject.getBoolean("updateSucceed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 解析上传地图的响应
     * @param jsonResponse 服务器对上传地图请求的响应字符串
     * @return 返回true为上传成功,false为上传失败
     */
    public static boolean getUploadMapResponse(String jsonResponse) throws UnauthorizedException {
        if(jsonResponse.equals("{\"authorized\":false}"))
            throw new UnauthorizedException();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            return jsonObject.getBoolean("uploadSucceed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
