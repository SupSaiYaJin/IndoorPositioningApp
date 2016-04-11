//                            _ooOoo_  
02.//                           o8888888o  
03.//                           88" . "88  
04.//                           (| -_- |)  
05.//                            O\ = /O  
06.//                        ____/`---'\____  
07.//                      .   ' \\| |// `.  
08.//                       / \\||| : |||// \  
09.//                     / _||||| -:- |||||- \  
10.//                       | | \\\ - /// | |  
11.//                     | \_| ''\---/'' | |  
12.//                      \ .-\__ `-` ___/-. /  
13.//                   ___`. .' /--.--\ `. . __  
14.//                ."" '< `.___\_<|>_/___.' >'"".  
15.//               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
16.//                 \ \ `-. \_ __\ /__ _/ .-` / /  
17.//         ======`-.____`-.___\_____/___.-`____.-'======  
18.//                            `=---='  
19.//  
20.//         .............................................  
21.//                  佛祖保佑             永无BUG 
22.//          佛曰:  
23.//                  写字楼里写字间，写字间里程序员；  
24.//                  程序人员写程序，又拿程序换酒钱。  
25.//                  酒醒只在网上坐，酒醉还来网下眠；  
26.//                  酒醉酒醒日复日，网上网下年复年。  
27.//                  但愿老死电脑间，不愿鞠躬老板前；  
28.//                  奔驰宝马贵者趣，公交自行程序员。  
29.//                  别人笑我忒疯癫，我笑自己命太贱；  
30.//                  不见满街漂亮妹，哪个归得程序员？  

 
package com.saiya.indoorposapp.broadcast;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.WindowManager;

import com.saiya.indoorposapp.R;
import com.saiya.indoorposapp.activities.LoginActivity;
import com.saiya.indoorposapp.tools.ActivityCollector;

/**
 * 接收强制下线的广播并做处理
 */
public class ForceOfflineReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.broadcast_forceOffline_warning);
        builder.setMessage(R.string.broadcast_forceOffline_msg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.broadcast_forceOffline_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCollector.finishAll();
                Intent intent = new Intent(context, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("allowedAutoLogin", false);
                context.startActivity(intent);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();
    }

}
