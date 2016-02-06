package com.saiya.indoorposapp.tools;

import com.saiya.indoorposapp.bean.WifiFingerprint;

import java.util.Collections;
import java.util.List;

/**
 * 算法工具类
 */
public class Algorithms {

    /**
     * 将rssiList中信号强度前K大的元素放到List的最前端
     * @param rssiList String为MAC地址,Float为信号强度
     * @param start 起始位置
     * @param end 终止位置
     * @param K 需要的个数
     */
    public static void findKStrongestRSSI(List<WifiFingerprint> rssiList, int start, int
            end, int K) {
        if(end - start + 1 < K)
            return;
        if(start < end) {
            int mid = partition(rssiList, start, end);
            if(mid - start + 1 == K)
                return;
            if(mid - start + 1 > K)
                findKStrongestRSSI(rssiList, start, mid - 1, K);
            else
                findKStrongestRSSI(rssiList, mid + 1, end, K - (mid - start + 1));
        }
    }

    /**
     * findKStrongestRSSI函数的分区函数
     * @param rssiList String为MAC地址,Float为信号强度
     * @param start 起始位置
     * @param end 终止位置
     * @return 分界位置
     */
    private static int partition (List<WifiFingerprint> rssiList, int start, int end) {
        float key = rssiList.get(end).getRssi();
        int result = start;
        for(int i = start; i < end; ++i) {
            if(rssiList.get(i).getRssi() > key) {
                Collections.swap(rssiList, result, i);
                ++result;
            }
        }
        Collections.swap(rssiList, result, end);
        return result;
    }

}
