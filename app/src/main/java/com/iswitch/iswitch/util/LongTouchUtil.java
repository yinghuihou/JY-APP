package com.iswitch.iswitch.util;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.iswitch.iswitch.MainActivity;

public class LongTouchUtil {

    private boolean longPress = true;
    private Thread longPressSendCmdThread;
    private MainActivity mActivity;
    private long sleepTime = 100;//这是100ms，就是0.1s，在这里调整时间

    public LongTouchUtil(MainActivity activity) {
        this.mActivity = activity;
    }

    public void setLongClick(View view, final String str) {
        if (view != null) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    LongTouchSendCmd(str, motionEvent);
                    return true;
                }
            });
        }
    }

    private void LongTouchSendCmd(final String cmd, MotionEvent event) {
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
                                        Thread.sleep(sleepTime);
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
