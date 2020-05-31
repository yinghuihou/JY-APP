package com.iswitch.iswitch;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.iswitch.iswitch.service.BluetoothLeService;
import com.iswitch.iswitch.util.LongTouchUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static com.iswitch.iswitch.DeviceScanActivity.EXTRAS_DEVICE_ADDRESS;
import static com.iswitch.iswitch.DeviceScanActivity.EXTRAS_DEVICE_NAME;

public class MainActivity extends Activity implements OnClickListener {
    private Button kaiDianJi, guanDianJi, safeOn;
    private Button start, stop, reset, resetAll, jiTing;
    private Button heightAdd, heightSub, buChangAdd, buChangSub, speedAdd, speedSub, yuShengSong, yuShengJin;
    private Button zhiNengXueXi, ziDongTiChui, fangShangTianLun, zhiNengPaiSheng, jinChiJiaSu;

    private Button duanKai;
    private Button danshuang;
    //+++++++++++++++++++++
    private ImageView shache_jia;
    private ImageView shache_jian, lihe_jia, lihe_jian;
    //+++++++++++++++++++++
    private Switch aSwitch;
    // private Switch duanshuangda;
    public final static int SENDPOS = 100;
    public final static String FNAME = "FNAME";
    private final static int REQUEST_CONNECT_DEVICE = 1; // 宏定义查询设备句柄
    private final static int REQUEST_DATA = 2;
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号
    protected static final int REQUEST_ENABLE = 0;

    private InputStream is; // 输入流，用来接收蓝牙数据

    private TextView dis; // 接收数据显示句柄
    private ScrollView sv; // 翻页句柄
    private String smsg = ""; // 显示用数据缓存
    int MAX = 4096;
    byte[] all = new byte[MAX];
    int allPos = 0;


    BluetoothDevice _device = null; // 蓝牙设备
    BluetoothSocket _socket = null; // 蓝牙通信socket
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;
    boolean hex = false;
    boolean longPress = false;

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter(); // 获取本地蓝牙适配器，即蓝牙设备
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //+++++++++++++++++++++
    private TextView height_value;//高度值
    private TextView rope_value;
    private TextView speed_value;//速度
    private TextView shache_qiya;//刹车气压
    private TextView lihe_qiya;//离合气压
    private TextView tv_A;//大电流
    private TextView tv_V;//总气压
    private TextView textView28, textView29, textView30, textView32;
    private TextView imageView21, imageView22, imageView24, imageView25;
    private ImageView imageView;//刹车加
    private ImageView imageView2;
    private ImageView imageView3;
    private ImageView imageView4;


    private boolean dianjikai, dongzuo, dan_shuangda;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    boolean connect_status_bit = false;
    private StringBuffer sbValues = new StringBuffer();
    private LongTouchUtil longTouchUtil;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //mConnected = true;
                connect_status_bit = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;

                //updateConnectionState(R.string.disconnected);
                connect_status_bit = false;
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE1.equals(action)) {
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            if (result) {
                updateConnectionState(mDeviceName + "连接成功");
                mConnected = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null) {
            mDeviceName = getIntent().getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);
        }
        longTouchUtil = new LongTouchUtil(this);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        initView();
        initBlueTooth();
    }

    private void initBlueTooth() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        updateConnectionState("开始连接...");

        // 如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null) {
            Toast.makeText(this, "本机没有找到蓝牙硬件或驱动！", Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }
        if (_bluetooth.isEnabled() == false) { // 如果蓝牙服务不可用则提示
            Toast.makeText(MainActivity.this, " 打开蓝牙中...",
                    Toast.LENGTH_SHORT).show();
            new Thread() {
                public void run() {
                    if (_bluetooth.isEnabled() == false) {
                        _bluetooth.enable();
                    }
                }
            }.start();
        }
        if (_bluetooth.isEnabled() == false) {
            Toast.makeText(MainActivity.this, "等待蓝牙打开，5秒后，尝试连接！", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {   //延迟执行
                @Override
                public void run() {
                    if (_bluetooth.isEnabled() == false) {
                        Toast.makeText(MainActivity.this, "自动打开蓝牙失败，请手动打开蓝牙！", Toast.LENGTH_SHORT).show();
                        //询问打开蓝牙
                        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enabler, REQUEST_ENABLE);
                    } else
                        connect(); //自动进入连接
                }
            }, 5000);
        } else {
            connect(); //自动进入连接
        }
    }


    public void initView() {
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
                R.layout.device_name);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);

        //重置按钮点击事件
        findViewById(R.id.rec).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                smsg = "";
                dis.setText(smsg); // 显示数据
            }
        });

        dis = (TextView) findViewById(R.id.in); // 得到数据显示句柄
        sv = (ScrollView) findViewById(R.id.scroll_view);
        duanKai = (Button) findViewById(R.id.duan_kai);
        duanKai.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        // kaiDianJi = (Button) findViewById(R.id.kai);
        // kaiDianJi.setOnClickListener(this);

        // kaiDianJi = (Button) findViewById(R.id.kai);
        // kaiDianJi.setOnClickListener(this);
        //++++++++++++++
        imageView21 = (TextView) findViewById(R.id.imageView21);
        imageView24 = (TextView) findViewById(R.id.imageView24);
        imageView22 = (TextView) findViewById(R.id.imageView22);
        imageView25 = (TextView) findViewById(R.id.imageView25);
        textView28 = (TextView) findViewById(R.id.textView28);
        textView29 = (TextView) findViewById(R.id.textView_29);
        textView30 = (TextView) findViewById(R.id.textView30);
        textView32 = (TextView) findViewById(R.id.textView32);
        height_value = (TextView) findViewById(R.id.height_value);
        rope_value = (TextView) findViewById(R.id.rope_value);
        speed_value = (TextView) findViewById(R.id.speed_value);
        shache_qiya = (TextView) findViewById(R.id.shache_qiya);
        lihe_qiya = (TextView) findViewById(R.id.lihe_qiya);
        tv_A = (TextView) findViewById(R.id.tv_A);
        tv_V = (TextView) findViewById(R.id.tv_V);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        imageView3 = (ImageView) findViewById(R.id.imageView3);
        imageView4 = (ImageView) findViewById(R.id.imageView4);
        //+++++++++++++
        // guanDianJi = (Button) findViewById(R.id.guan);
        // guanDianJi.setOnClickListener(this);

        // safeOn =  (Button) findViewById(R.id.jingyan_qidong);
        // safeOn.setOnClickListener(this);

        start = (Button) findViewById(R.id.qidong);
        start.setOnClickListener(this);

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(this);
        //reset = (Button) findViewById(R.id.reset);
        //reset.setOnClickListener(this);

        // resetAll = (Button) findViewById(R.id.reset_all);
        // resetAll.setOnClickListener(this);

        jiTing = (Button) findViewById(R.id.emergency_stop);
        jiTing.setOnClickListener(this);

        heightAdd = (Button) findViewById(R.id.height_add);
        heightAdd.setOnClickListener(this);

        heightSub = (Button) findViewById(R.id.height_sub);
        heightSub.setOnClickListener(this);

        //  buChangAdd = (Button) findViewById(R.id.compensate_add);
        // buChangAdd.setOnClickListener(this);

        //  buChangSub = (Button) findViewById(R.id.compensate_sub);
        //  buChangSub.setOnClickListener(this);

        speedAdd = (Button) findViewById(R.id.speed_add);
        speedAdd.setOnClickListener(this);

        speedSub = (Button) findViewById(R.id.speed_sub);
        speedSub.setOnClickListener(this);

        yuShengSong = (Button) findViewById(R.id.rope_add);
        yuShengSong.setOnClickListener(this);

        yuShengJin = (Button) findViewById(R.id.rope_sub);
        yuShengJin.setOnClickListener(this);

        zhiNengXueXi = (Button) findViewById(R.id.zhixue);
        zhiNengXueXi.setOnClickListener(this);

        ziDongTiChui = (Button) findViewById(R.id.zidong);
        ziDongTiChui.setOnClickListener(this);

        // fangShangTianLun = (Button) findViewById(R.id.fang_shang_tian_lun);
        // fangShangTianLun.setOnClickListener(this);

        // zhiNengPaiSheng = (Button) findViewById(R.id.zhi_neng_pai_sheng);
        // zhiNengPaiSheng.setOnClickListener(this);

        //jinChiJiaSu = (Button) findViewById(R.id.jin_chi_jia_su);
        //jinChiJiaSu.setOnClickListener(this);
        //++++++++++++++++++
        shache_jia = (ImageView) findViewById(R.id.imageView);
        shache_jia.setOnClickListener(this);
        shache_jian = (ImageView) findViewById(R.id.imageView2);
        shache_jian.setOnClickListener(this);
        lihe_jia = (ImageView) findViewById(R.id.imageView3);
        lihe_jia.setOnClickListener(this);
        lihe_jian = (ImageView) findViewById(R.id.imageView4);
        lihe_jian.setOnClickListener(this);
        danshuang = (Button) findViewById(R.id.danshuang);
        danshuang.setOnClickListener(this);
        //++++++++++++++++++

        //这里是例子，添加setOnTouchListener这个的时候，上边就不要加setOnClickListener了
        longTouchUtil.setLongClick(lihe_jia,"0101");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //case R.id.kai:
            //开电机

            //  sendString("10");

            //  break;
            case R.id.danshuang:
                //单双打切换
                if (dan_shuangda) {
                    //停止
                    sendString("08");
                    dan_shuangda = false;
                } else {
                    //启动
                    sendString("08");
                    dan_shuangda = true;
                }
                break;

            case R.id.qidong:
                if (dianjikai) {
                    //停止
                    sendString("11");
                    dianjikai = false;
                } else {
                    //启动
                    sendString("10");
                    dianjikai = true;
                }
                break;
            case R.id.stop:
                //停止
                if (dongzuo) {
                    sendString("09");
                    dongzuo = false;
                } else {
                    sendString("12");
                    dongzuo = true;
                }
                break;
            // case R.id.reset:
            //复位

            //   break;

            case R.id.emergency_stop:

                //急停按钮
                break;
            case R.id.height_add:
                //高度加

                sendString("01");
                break;
            case R.id.height_sub:
                //高度减
                sendString("04");
                break;

            case R.id.speed_add:
                //速度加
                sendString("06");
                break;
            case R.id.speed_sub:
                //速度减
                sendString("07");
                break;
            case R.id.rope_add:
                //余绳松
                sendString("05");
                break;
            case R.id.rope_sub:
                //余绳紧
                sendString("02");
                break;
            case R.id.zhixue:
                //智能学习
                break;
            case R.id.zidong:
                //自动提锤
                break;

            case R.id.imageView:
                //刹车气压加
                sendString("41");
                break;
            case R.id.imageView2:
                //刹车气压减
                sendString("42");
                break;
            case R.id.imageView3:
                sendString("43");
                break;
            case R.id.imageView4:
                sendString("44");
                break;
            //   case R.id.switch2 :
            //单双打切换
            //      sendString("32");
            //      break;

            default:
                break;
        }
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dis.setText(status);
            }
        });
    }

    public void disconnect() {
        if (_socket != null) {
            //取消注册异常断开接收器
            this.unregisterReceiver(mReceiver);

            SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
            sharedata.clear();
            sharedata.commit();

            mPairedDevicesArrayAdapter.clear();
            Toast.makeText(this, "线路已断开，请重新连接！", Toast.LENGTH_SHORT).show();
            // 关闭连接socket
            try {
                bRun = false; // 一定要放在前面
                is.close();
                _socket.close();
                _socket = null;
                bRun = false;

            } catch (IOException e) {
            }
        }
    }

    public void connect() {
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.e("xxx", "Connect request result=" + result);
        }
    }

    // 发送响应
    // 调用这个方法发送数据到单片机。
    public void sendString(String str) {
        Log.e("xxx", "当前发送数据：" + str);

        if (!mConnected) {
            Toast.makeText(this, "未连接蓝牙", Toast.LENGTH_SHORT).show();
        }
        if (str == null) {
            Toast.makeText(this, "发送内容为空", Toast.LENGTH_SHORT).show();
        }

        try {
            mBluetoothLeService.sendMessage(str, hex);
        } catch (Exception e) {
        }
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mBluetoothLeService.Delay_ms(300);
        if (gattServices.size() > 0 && mBluetoothLeService.get_connected_status(gattServices) == 1) {
            // connect_count = 0;
            if (connect_status_bit) {
                mConnected = true;
            }
        } else if (gattServices.size() > 0) {
            //connect_count = 0;
            if (connect_status_bit) {
                mConnected = true;
                //  show_view(true);

                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(0);
                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(1);
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE1);
        return intentFilter;
    }

    //接收数据后处理方法
    private void displayData(byte[] data1) {
        if (data1 != null && data1.length > 0) {

            final StringBuilder stringBuilder = new StringBuilder(sbValues.length());
            byte[] WriteBytes = mBluetoothLeService.hex2byte(stringBuilder.toString().getBytes());

            for (byte byteChar : data1)
                stringBuilder.append(String.format(" %02X", byteChar));

            String da = stringBuilder.toString();
            sbValues.append(da);
            dis.setText(sbValues.toString());
            disp2Mobile(data1);
        }
    }

    // 关闭程序掉用处理部分
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        if (_socket != null) // 关闭连接socket
            try {
                _socket.close();
            } catch (IOException e) {
            }
        _bluetooth.disable(); //关闭蓝牙服务
    }

    // 接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE: // 连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) { // 连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras().getString(
                            DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    _device = _bluetooth.getRemoteDevice(address);
                    // 用服务号得到socket
                    try {
                        _socket = _device.createRfcommSocketToServiceRecord(UUID
                                .fromString(MY_UUID));
                    } catch (IOException e) {
                        Toast.makeText(this, "连接失败,无法得到Socket！" + e, Toast.LENGTH_SHORT).show();
                    }

                    // 连接socket
                    try {
                        _socket.connect();

                        Toast.makeText(this, "连接" + _device.getName() + "成功！",
                                Toast.LENGTH_SHORT).show();
                        mPairedDevicesArrayAdapter.add(_device.getName() + "\n"
                                + _device.getAddress());
                        SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
                        sharedata.putString(String.valueOf(0), _device.getName());
                        sharedata.putString(String.valueOf(1), _device.getAddress());
                        sharedata.commit();


                        //注册异常断开接收器  等连接成功后注册
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                        this.registerReceiver(mReceiver, filter);

                    } catch (IOException e) {

                        try {
                            Toast.makeText(this, "连接失败！" + e, Toast.LENGTH_SHORT)
                                    .show();
                            _socket.close();
                            _socket = null;
                        } catch (IOException ee) {
                        }
                        return;
                    }

                    // 打开接收线程
                    try {
                        is = _socket.getInputStream(); // 得到蓝牙数据输入流
                    } catch (IOException e) {
                        Toast.makeText(this, "异常：打开接收线程！" + e, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                break;
            case REQUEST_DATA:
                //Log.d("TAG", "REQUEST_DATA");
                //flushdata();

                //setbtn();
                break;
            default:
                break;
        }
    }


    int UN_I = 0;

    public void disp2Mobile(byte[] src) {
        if (src == null || src.length < 14) {
            return;
        }
        // gaodu
        //byte a1 = 33;//src[0];
        float sheche = 0;
        float dadianliu = 0;
        float lihe = 0;
        float zongqiya = 0;
        //int shache_kong = 0;//刹车电磁阀空锤
        // int lihe_kong = 0;//离合电磁阀空锤
        // int xieyan =0 ;//斜岩引起的锤倒
        // int lihe_ka = 0;//离合电磁阀卡顿空锤

        int a1 = src[0] & 0xff;
        int a2 = src[1] & 0xff;
        int a3 = src[2] & 0xff;
        int a4 = src[3] & 0xff;
        int a5 = src[4];
        if (a1 == 0xAA && a2 == 0xFB && a3 == 0xFF)
        {
            if (a5 == 0x1 || a5 == 0x11)//第一段数据
            {
                int a6 = src[5] & 0xff;//高度
                int a7 = src[6];//刹车提前
                int a8 = src[7] & 0xff;//刹车时间
                int a9 = src[8];//与绳长度
                int a10 = src[9] & 0xff;//电流高8位
                int a11 = src[10] & 0xff;//电流低8位
                int a12 = src[11];//电机状态
                int a13 = src[12];//启停状态
                int a14 = src[13];//单双打
                dadianliu = a10 << 8 | a11;
                height_value.setText((int) a6 + "," + (UN_I++) % 10);
                rope_value.setText((int) a7 + "  ");
                speed_value.setText((int) a8 + "  ");
                tv_A.setText((float) dadianliu + "  ");
                tv_V.setText((float) zongqiya + "  ");
                if (a14 == 1)
                    danshuang.setBackgroundResource(R.drawable.shuangda);
                if (a14 == 2)
                    danshuang.setBackgroundResource(R.drawable.danda);
                if (a12 == 0)
                    start.setBackgroundResource(R.drawable.dianjiqidong);
                if (a12 == 1)
                    start.setBackgroundResource(R.drawable.dianjiting);
                if (a13 == 1)
                    stop.setBackgroundResource(R.drawable.qidonghongse);
                if (a13 == 2)
                    stop.setBackgroundResource(R.drawable.qidong);
            }

        }

       /* int a15 = src[14];
        int a16 = src[15];
        int a17 = src[16];
        int a18 = src[17];
        int a19 = src[18];
        int a20 = src[19];
        int a21 = src[20];
        int a22 = src[21];
        int a23 = src[22];
        if (a1 < 0) {
            a1 = a1 + 256;
        }

        if (a3 < 0) {
            a3 = a3 + 256;
        }
        if (a5 < 0) {
            a5 = a5 + 256;
        }
        if (a6 < 0) {
            a6 = a6 + 256;
        }
        if (a7 < 0) {
            a7 = a7 + 256;
        }
        if (a8 < 0) {
            a8 = a8 + 256;
        }
        if (a9 < 0) {
            a9 = a9 + 256;
        }
        if (a10 < 0) {
            a10 = a10 + 256;
        }
        if (a11 < 0) {
            a11 = a11 + 256;
        }
        if (a12 < 0) {
            a12 = a12 + 256;
        }
        if (a20 < 0) {
            a20 = a20 + 256;
        }
        if (a21 < 0) {
            a21 = a21 + 256;
        }
        if (a22 < 0) {
            a22 = a22 + 256;
        }
        if (a23 < 0) {
            a23 = a23 + 256;
        }
        dadianliu = a5 << 8 | a6;
        zongqiya = a7 << 8 | a8;
        sheche = a9 << 8 | a10;
        lihe = a11 << 8 | a12;
        sheche = sheche / 100;
        lihe = lihe / 100;
        zongqiya = zongqiya / 100;
        //shache_kong = a20;
        // lihe_kong = a21;
        //xieyan = a22;
        //lihe_ka =a23;
        if (a16 == 1)
            imageView21.setText("良好");
        if (a16 == 2)
            imageView21.setText("一般");
        if (a16 == 3)
            imageView21.setText("较差");
        if (a16 == 4)
            imageView21.setText("损坏");

        if (a18 == 1)
            imageView22.setText("良好");
        if (a18 == 2)
            imageView22.setText("一般");
        if (a18 == 3)
            imageView22.setText("较差");
        if (a18 == 4)
            imageView22.setText("损坏");

        if (a17 == 1)
            imageView24.setText("良好");
        if (a17 == 2)
            imageView24.setText("一般");
        if (a17 == 3)
            imageView24.setText("较差");
        if (a17 == 4)
            imageView24.setText("损坏");

        if (a19 == 1)
            imageView25.setText("良好");
        if (a19 == 2)
            imageView25.setText("一般");
        if (a19 == 3)
            imageView25.setText("较差");
        if (a19 == 4)
            imageView25.setText("损坏");
        height_value.setText((int) a1 + "," + (UN_I++) % 10);
        rope_value.setText((int) a2 + "  ");
        speed_value.setText((int) a3 + "  ");
        tv_A.setText((float) dadianliu + "  ");
        tv_V.setText((float) zongqiya + "  ");
        shache_qiya.setText((float) sheche + "  ");
        lihe_qiya.setText((float) lihe + "  ");
        textView28.setText((int) a20 + "  ");
        textView29.setText((int) a21 + "  ");
        textView30.setText((int) a22 + "  ");
        textView32.setText((int) a23 + "  ");
        if (a13 == 0)
            start.setBackgroundResource(R.drawable.dianjiqidong);
        if (a13 == 1)
            start.setBackgroundResource(R.drawable.dianjiting);
        if (a14 == 1)
            stop.setBackgroundResource(R.drawable.qidonghongse);
        if (a14 == 2)
            stop.setBackgroundResource(R.drawable.qidong);
        if (a15 == 1)
            danshuang.setBackgroundResource(R.drawable.shuangda);
        if (a15 == 2)
            danshuang.setBackgroundResource(R.drawable.danda);
        //
        //*/


    }

    // public  byte  pinjie(byte gao,byte di,int he){
    //     he = gao<<8 | di;
    //return he;
    //  }
    public byte[] fromAll(byte[] src, int end) {
        byte[] buffer = new byte[end];
        for (int i = 0; i < end; i++) {
            buffer[i] = src[i];
        }
        return buffer;
    }

    public byte[] from(byte[] src, int end) {
        byte[] head = new byte[2];
        head[0] = 0x1d;
        head[1] = 0x2d;

        //start
        int index1 = -1;
        int index2 = -1;
        for (int i = 0; i < end; i++) {
            if (src[i] == head[0]) {
                index1 = i;
            }
            if (index1 != -1 && src[i] == head[1] && i == index1 + 1) {
                index2 = i;
                break;
            }
        }
        //
        if (!(index1 != -1 && index2 != -1)) {
            return null;
        }

        //
        //end
        byte[] tail = new byte[2];
        tail[0] = 0x2e;
        tail[1] = 0x3e;
        int t1 = -1;
        int t2 = -1;
        for (int i = index2 + 1; i < end; i++) {
            if (src[i] == tail[0]) {
                t1 = i;
            }
            if (t1 != -1 && src[i] == tail[1] && i == t1 + 1) {
                t2 = i;
                break;
            }
        }
        //
        if (!(t1 != -1 && t2 != -1)) {
            return null;
        }

        //read data
        byte[] data = copyBuffer(src, index2, t1);

        //validatte

        //
        return data;
    }


    public byte[] copyBuffer(byte[] src, int start, int end) {
        byte[] buffer = new byte[end - start - 1];
        int j = 0;
        for (int i = start + 1; i < end; i++) {
            buffer[j++] = src[i];
        }
        return buffer;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                disconnect();
            }
        }
    };

}

