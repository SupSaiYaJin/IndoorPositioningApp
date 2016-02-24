package com.saiya.indoorposapp.ui;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.saiya.indoorposapp.R;

/**
 * 设置定位参数的对话框,由一个SeekBar和一个TextView组成
 */
public class SeekbarSettingDialog extends AlertDialog {
    private Activity mActivity;
    private int[] mValues;
    private int mProgressUnit;
    private TextView mTextView;
    private SeekBar mSeekBar;
    private OnConfirmListener mOnConfirmListener;
    /**
     * 封装对设置结果执行的操作
     */
    public interface OnConfirmListener {
        /**
         * 对结果进行操作
         * @param value 最终要设置的值
         */
        void process(int value);
    }

    public SeekbarSettingDialog(Activity activity) {
        super(activity);
        mActivity = activity;
    }

    /**
     * 设置对话框属性
     *
     * @param title 对话框标题,为资源ID
     * @param oriIndex 初始值索引
     * @param values 可以调整的值的数组
     * @param onConfirmListener 点击确定按钮后执行的动作
     */
    public void setProperties(int title, int oriIndex, int[] values,
            OnConfirmListener onConfirmListener) {
        mValues = values;
        mOnConfirmListener = onConfirmListener;
        //由数组大小算出的最小进度单元
        mProgressUnit = 50 / (mValues.length - 1);
        View view = mActivity.getLayoutInflater().inflate(R.layout.dlg_skbar,
                (ViewGroup)mActivity.findViewById(R.id.sclVi_settings), false);
        mTextView = (TextView) view.findViewById(R.id.tv_dlg);
        mSeekBar = (SeekBar) view.findViewById(R.id.skbar_dlg);
        this.setTitle(title);
        this.setView(view);
        //初始化SeekBar和TextView状态
        mTextView.setText(String.valueOf(values[oriIndex]));
        mSeekBar.setProgress(oriIndex * mProgressUnit * 2);
        //设置SeekBar监听器
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //按移动SeekBar的情况更新TextView
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bound = mProgressUnit;
                for (int value : mValues) {
                    if (progress < bound) {
                        mTextView.setText(String.valueOf(value));
                        break;
                    } else {
                        bound += 2 * mProgressUnit;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            //使SeekBar结束移动时只落在n分之x处
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                int bound = mProgressUnit;
                for (int i = 0; i < mValues.length; ++i) {
                    if (progress < bound) {
                        seekBar.setProgress(2 * i * mProgressUnit);
                        break;
                    } else {
                        bound += 2 * mProgressUnit;
                    }
                }
            }
        });
        //设置对话框的确定按钮
        this.setButton(BUTTON_POSITIVE, mActivity.getResources().getString(R.string.fragment_settings_confirm),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mOnConfirmListener
                                .process(mValues[mSeekBar.getProgress() / mProgressUnit / 2]);
                    }
                });
    }

}
