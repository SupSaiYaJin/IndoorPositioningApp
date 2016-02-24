package com.saiya.indoorposapp.tools;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * APP运行时管理所有Activity
 */
public class ActivityCollector {

    /** 存储所有运行中的Activity引用 */
    private static List<Activity> activities = new ArrayList<>();

    /**
     * Activity启动时将它加入List
     * @param activity 要加入的Activity实例
     */
    public static void addActivity(Activity activity) {
        activities.add(activity);
    }

    /**
     * Activity销毁时将它的引用从List中删除
     * @param activity 要删除的Activity引用
     */
    public static void removeActivity(Activity activity) {
        activities.remove(activity);
    }

    /**
     * 结束所有List中的Activity实例
     */
    public static void finishAll() {
        for (Activity activity : activities) {
            if (!activity.isFinishing()) {
                activity.finish();
            }
        }
    }
}
