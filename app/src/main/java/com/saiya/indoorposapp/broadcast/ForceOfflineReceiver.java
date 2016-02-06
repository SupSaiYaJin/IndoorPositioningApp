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
