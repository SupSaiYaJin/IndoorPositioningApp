package com.saiya.indoorposapp.tools;

/**
 * 表示服务器对定位请求的响应码的Enum类
 */
public enum PositioningResponse {

    /** 账户过期的消息代号 */
    UNAUTHORIZED,

    /** 网络错误的消息代号 */
    NETWORK_ERROR,

    /** 更新指纹数据成功的消息代号 */
    UPDATE_FP_SUCCEED,

    /** 更新地图成功的消息代号 */
    UPDATE_MAP_SUCCEED,

    /** 下载地图成功的消息代号 */
    DOWNLOAD_MAP_SUCCEED;

    public static PositioningResponse getInstance(int responsecode) {
        switch (responsecode) {
            case -1:
                return UNAUTHORIZED;
            case 1:
                return UPDATE_FP_SUCCEED;
            case 2:
                return UPDATE_MAP_SUCCEED;
            case 3:
                return DOWNLOAD_MAP_SUCCEED;
            default:
                return NETWORK_ERROR;
        }
    }
}
