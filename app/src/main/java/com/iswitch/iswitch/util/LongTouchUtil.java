package com.iswitch.iswitch.util;

import android.view.MotionEvent;
import android.widget.Toast;

import com.iswitch.iswitch.MainActivity;

public class LongTouchUtil {

    private boolean longPress = true;
    private Thread longPressSendCmdThread;
    private MainActivity mActivity;

    public LongTouchUtil(MainActivity activity) {
        this.mActivity = activity;
    }

    public void LongTouchSendCmd(final String cmd, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                longPress = true;
                Toast.makeText(mActivity, "开始发送", Toast.LENGTH_SHORT).show();
                if (longPressSendCmdThread == null) {
                    longPressSendCmdThread = new Thread() {
                        public void run() {
                            super.run();
                            while (true) {
                                if (longPress)//长按连续发送命令
                                {
                                    try {
                                        mActivity.sendString(cmd);
                                        Thread.sleep(100);//1秒发送一次
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    break;//没有按下，退出循环
                                }
                            }
                        }
                    };
                }

                longPressSendCmdThread.start();
                break;
            }
            case MotionEvent.ACTION_UP: {
                longPress = false;
                Toast.makeText(mActivity, "结束发送", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
