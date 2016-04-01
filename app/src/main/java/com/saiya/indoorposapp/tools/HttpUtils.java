package com.saiya.indoorposapp.tools;

import android.content.Context;

import com.saiya.indoorposapp.activities.MainActivity;
import com.saiya.indoorposapp.bean.SceneInfo;
import com.saiya.indoorposapp.exceptions.UnauthorizedException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 处理所有Http请求的工具类
 */
public class HttpUtils {

    /** 服务器IP地址 */
    private static String serverIp = "10.210.42.108";
    /** 服务器地址,实验室IP为http://10.107.34.169 家里IP为192.168.1.104:8080 */
    private static String serverUrl = "http://" + serverIp + ":8080";
    /** 注册请求的服务器路径 */
    private static final String REGISTER_PATH = "/register";
    /** 登录请求的服务器路径 */
    private static final String LOGIN_PATH = "/login";
    /** 注销请求的服务器路径 */
    private static final String LOGOUT_PATH = "/logout";
    /** 获取终端所在场景的服务器路径 */
    private static final String LOCATE_SCENE_PATH = "/positioning/locatescene";
    /** 获取场景列表的服务器路径 */
    private static final String GET_SCENE_LIST_PATH = "/positioning/getscenelist";
    /** 下载场景地图的服务器路径 */
    private static final String DOWNLOAD_MAP_PATH = "/positioning/downloadmap";
    /** 上传场景地图的服务器路径 */
    private static final String UPLOAD_MAP_PATH = "/positioning/uploadmap";
    /** 定位请求的服务器路径 */
    private static final String LOCATION_PATH = "/positioning/locate";
    /** 更新指纹请求的服务器路径 */
    private static final String UPDATE_PATH = "/positioning/updatedata";
    /** 服务器设置的JSESSIONID */
    private static String JSESSIONID;
    /** 连接服务器超时时间 */
    private static int CONNECTION_TIMEOUT = 1000 * 5;

    public static void setServerIp(String ip) {
        serverIp = ip;
        serverUrl = "http://" + serverIp + ":8080";
    }
    /**
     * 以POST方法与服务器发起请求
     * @param path 向服务器请求的路径
     * @param requestPropertyMap 请求中附加的属性
     * @return 返回响应字符串,交给JSONHelper类进行解析
     * @see JSONHelper
     */
    private static String doPost(String path, Map<String, String> requestPropertyMap) {
        //存储要附在内容体中的属性
        byte[] content = new byte[0];
        //若有请求参数,将它按格式拼接好后写入字节数组content中
        if (requestPropertyMap != null) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : requestPropertyMap.entrySet()) {
                stringBuilder.append(entry.getKey());
                stringBuilder.append("=");
                stringBuilder.append(entry.getValue());
                stringBuilder.append("&");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            try {
                content = stringBuilder.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            URL url = new URL(serverUrl + path);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            //设置Http请求方式为POST,打开输出,禁用缓存,设置内容类型,设置内容大小
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(content.length));
            //发起请求的路径属于/positioning时,在头中加上JSESSIONID的Cookie,保证权限,JSESSIONID在登录时获得
            if (JSESSIONID != null && path.startsWith("/positioning")) {
                conn.setRequestProperty("Cookie", String.format("JSESSIONID=%s", JSESSIONID));
            }
            //获取输出流并输出内容体
            BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
            out.write(content);
            out.flush();
            long postTime = System.currentTimeMillis();
            //更新JSESSIONID,仅在登录成功后使用
            String cookie = conn.getHeaderField("Set-Cookie");
            if (cookie != null) {
                int start = cookie.indexOf("JSESSIONID=");
                int end = cookie.indexOf(';');
                JSESSIONID = cookie.substring(start + 11, end);
            }
            //读取响应,并转化为字符串返回
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(conn.getInputStream(), "utf-8"));
            while((line = reader.readLine()) != null)
                stringBuilder.append(line);
            long responseTime = System.currentTimeMillis();
            LogUtils.d("PostURL:" + url.toString(), String.valueOf(responseTime - postTime) + "ms");
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 向服务器发起注册请求
     * @param username 用户名
     * @param password 密码
     * @return 返回注册响应码
     */
    public static AuthResponse register(String username, String password) {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("username", username);
        requestPropertyMap.put("password", password);
        return JSONHelper.getRegisterResponse(doPost(REGISTER_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起登录请求
     * @param username 用户名
     * @param password 密码
     * @return 返回登录响应码
     */
    public static AuthResponse login(String username, String password) {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("username", username);
        requestPropertyMap.put("password", password);
        return JSONHelper.getLoginResponse(doPost(LOGIN_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起注销请求
     */
    public static void logout() {

         try {
            URL url = new URL(serverUrl + LOGOUT_PATH);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //设置请求方式为GET,并设置JSESSIONID
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", String.format("JSESSIONID=%s", JSESSIONID));
            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取终端所在场景信息
     * @param mac 终端采集到的MAC地址
     * @return 终端所在场景对象
     */
    public static SceneInfo locateScene(String mac) throws UnauthorizedException {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("mac", mac);
        return JSONHelper.getLocateScene(doPost(LOCATE_SCENE_PATH, requestPropertyMap));
    }
    /**
     * 向服务器发起获取场景列表的请求,因不用更新服务器数据,故使用Get方法
     * @return 返回Map,String为场景名,Long为对应场景的最后更新时间
     */
    public static List<SceneInfo> getSceneList() throws UnauthorizedException {

        try {
            URL url = new URL(serverUrl + GET_SCENE_LIST_PATH);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //设置请求方式为GET,并设置JSESSIONID
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", String.format("JSESSIONID=%s", JSESSIONID));
            //读取响应
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(conn.getInputStream(), "utf-8"));
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String response = stringBuilder.toString();
            return JSONHelper.getSceneListResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 向服务器发起定位请求,只使用WiFi指纹
     * @param sceneName 场景名称
     * @param mac 采集到的MAC地址,形式为mac1,mac2,...,macN
     * @param rssi 采集到的RSSI值,形式为rssi1,rssi2,...,rssiN
     * @return 返回float[],float[0]为X坐标值,float[1]为Y坐标值
     */
    public static float[] locateOnWifi(String sceneName, String mac, String rssi)
            throws UnauthorizedException {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("sceneName", sceneName);
        requestPropertyMap.put("locateType", "1");
        requestPropertyMap.put("mac", mac);
        requestPropertyMap.put("rssi", rssi);
        return JSONHelper.getLocateResponse(doPost(LOCATION_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起定位请求,只使用地磁指纹
     * @param sceneName 场景名称
     * @param geomagnetic_y 采集到的Y方向磁场强度
     * @param geomagnetic_z 采集到的Z方向磁场强度
     * @return 返回float[],float[0]为X坐标值,float[1]为Y坐标值
     */
    public static float[] locateOnGeomagnetic(String sceneName,
            float geomagnetic_y, float geomagnetic_z) throws UnauthorizedException {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("sceneName", sceneName);
        requestPropertyMap.put("locateType", "2");
        requestPropertyMap.put("geomagnetic_y", Float.toString(geomagnetic_y));
        requestPropertyMap.put("geomagnetic_z", Float.toString(geomagnetic_z));
        return JSONHelper.getLocateResponse(doPost(LOCATION_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起定位请求,同时使用WiFi指纹和地磁指纹
     * @param sceneName 场景名称
     * @param mac 采集到的MAC地址,形式为mac1,mac2,...,macN
     * @param rssi 采集到的RSSI值,形式为rssi1,rssi2,...,rssiN
     * @param geomagnetic_y 采集到的Y方向磁场强度
     * @param geomagnetic_z 采集到的Z方向磁场强度
     * @return 返回float[],float[0]为X坐标值,float[1]为Y坐标值
     */
    public static float[] locateOnBoth(String sceneName, String mac, String rssi,
            float geomagnetic_y, float geomagnetic_z) throws UnauthorizedException {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("sceneName", sceneName);
        requestPropertyMap.put("locateType", "0");
        requestPropertyMap.put("mac", mac);
        requestPropertyMap.put("rssi", rssi);
        requestPropertyMap.put("geomagnetic_y", Float.toString(geomagnetic_y));
        requestPropertyMap.put("geomagnetic_z", Float.toString(geomagnetic_z));
        return JSONHelper.getLocateResponse(doPost(LOCATION_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起更新WiFi指纹数据的请求
     * @param sceneName 场景名称
     * @param location_x 采集指纹的X坐标
     * @param location_y 采集指纹的Y坐标
     * @param mac 采集到的MAC地址,形式为mac1,mac2,...,macN
     * @param rssi 采集到的RSSI值,形式为rssi1,rssi2,...,rssiN
     * @return 返回true表示更新成功,false表示更新失败
     */
    public static boolean updateWifiFingerprint(String sceneName, float location_x,
            float location_y, String mac, String rssi) throws UnauthorizedException {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("updateType", "wifi");
        requestPropertyMap.put("sceneName", sceneName);
        requestPropertyMap.put("location_x", Float.toString(location_x));
        requestPropertyMap.put("location_y", Float.toString(location_y));
        requestPropertyMap.put("mac", mac);
        requestPropertyMap.put("rssi", rssi);
        return JSONHelper.getUpdateResponse(doPost(UPDATE_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起更新地磁指纹数据的请求
     * @param sceneName 场景名称
     * @param location_x 采集指纹的X坐标
     * @param location_y 采集指纹的Y坐标
     * @param geomagnetic_y 采集到的Y方向磁场强度
     * @param geomagnetic_z 采集到的Z方向磁场强度
     * @return 返回true表示更新成功,false表示更新失败
     */
    public static boolean updateGeomagneticFingerprint(String sceneName,
            float location_x, float location_y, float geomagnetic_y, float geomagnetic_z)
            throws UnauthorizedException {
        Map<String, String> requestPropertyMap = new HashMap<>();
        requestPropertyMap.put("updateType", "geomagnetic");
        requestPropertyMap.put("sceneName", sceneName);
        requestPropertyMap.put("location_x", Float.toString(location_x));
        requestPropertyMap.put("location_y", Float.toString(location_y));
        requestPropertyMap.put("geomagnetic_y", Float.toString(geomagnetic_y));
        requestPropertyMap.put("geomagnetic_z", Float.toString(geomagnetic_z));
        return JSONHelper.getUpdateResponse(doPost(UPDATE_PATH, requestPropertyMap));
    }

    /**
     * 向服务器发起下载地图的请求
     * @param sceneName 场景名称
     * @param activity MainActivity实例,用于获取文件存储路径
     * @return 返回代表地图文件的字节数组
     */
    public static boolean downloadMap(String sceneName, MainActivity activity)
            throws UnauthorizedException {
        try (FileOutputStream out = activity
                .openFileOutput(sceneName + ".jpg", Context.MODE_PRIVATE)) {
            String getURL = "?sceneName=" +  sceneName;
            URL url = new URL(serverUrl + DOWNLOAD_MAP_PATH + getURL);
            LogUtils.d("downloadMapURL", url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //设置Http请求使用GET方法,并设置JSESSIONID
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", String.format("JSESSIONID=%s", JSESSIONID));
            //读取响应
            byte buffer[] = new byte[1024];
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            in.mark(1);
            if ((byte)in.read() == "{".getBytes()[0]) {
                throw new UnauthorizedException();
            }
            in.reset();
            int n;
            while((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 向服务器发起上传地图的请求
     * @param sceneName 场景名称
     * @param scale 场景比例尺,意义为scale个像素表示1米
     * @param in 要上传的地图文件的输入流
     * @return 返回true代表上传成功,false代表上传失败
     */
    public static boolean uploadMap(String sceneName, float scale, InputStream in)
            throws UnauthorizedException {
        /** 边界标识,随机生成 */
        String BOUNDARY =  UUID.randomUUID().toString();
        String PREFIX = "--";
        String LINE_END = "\r\n";
        try {
            URL url = new URL(serverUrl + UPLOAD_MAP_PATH);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setRequestProperty("Cookie", String.format("JSESSIONID=%s", JSESSIONID));
            conn.setRequestProperty("Content-Type", "multipart/form-data" + ";boundary=" + BOUNDARY);
            conn.setRequestProperty("sceneName", URLEncoder.encode(sceneName, "utf-8"));
            conn.setRequestProperty("scale", Float.toString(scale));
            BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
            //name里面的值为服务器端的partName,filename是文件的名字，包含后缀名
            out.write((PREFIX + BOUNDARY + LINE_END +
                    "Content-Disposition: form-data; name=\"sceneMap\"; filename=\"" +
                    sceneName + ".jpg" + "\"" + LINE_END +
                    "Content-Type: application/octet-stream; charset=" +
                    "utf-8" + LINE_END + LINE_END).getBytes());
            //发送字节数组
            byte[] buffer = new byte[1024];
            int n;
            while((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            out.write(LINE_END.getBytes());
            out.write((PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes());
            out.flush();
            //读取响应
            StringBuilder response = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(conn.getInputStream(), "utf-8"));
            while((line = reader.readLine()) != null) {
                response.append(line);
            }
            return JSONHelper.getUploadMapResponse(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
