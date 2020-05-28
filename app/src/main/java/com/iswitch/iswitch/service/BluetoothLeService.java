package com.iswitch.iswitch.service;

/**
 * Created by Administrator on 2019/10/6 0006.
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private static final String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = 0;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public static final String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String ACTION_DATA_AVAILABLE1 = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE1";
    public static final String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public static final String EXTRA_DATA1 = "com.example.bluetooth.le.EXTRA_DATA1";
    public static final String EXTRA_UUID = "com.example.bluetooth.le.uuid_DATA";
    public static final String EXTRA_NAME = "com.example.bluetooth.le.name_DATA";
    public static final String EXTRA_PASSWORD = "com.example.bluetooth.le.password_DATA";
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList();
    public static final UUID UUID_HEART_RATE_MEASUREMENT;
    public static String Service_uuid;
    public static String Characteristic_uuid_TX;
    public static String Characteristic_uuid_FUNCTION;
    byte tx_cnt = 1;
    byte[] WriteBytes = new byte[200];
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if(newState == 2) {
                intentAction = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
               BluetoothLeService.this.mConnectionState = 2;
               BluetoothLeService.this.broadcastUpdate(intentAction);
                Log.i(BluetoothLeService.TAG, "Connected to GATT server.");
                Log.i(BluetoothLeService.TAG, "Attempting to start service discovery:" + BluetoothLeService.this.mBluetoothGatt.discoverServices());
            } else if(newState == 0) {
                intentAction = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
                BluetoothLeService.this.mConnectionState = 0;
                Log.i(BluetoothLeService.TAG, "Disconnected from GATT server.");
               BluetoothLeService.this.broadcastUpdate(intentAction);
            }

        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == 0) {
               BluetoothLeService.this.broadcastUpdate("com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED");
            } else {
                Log.w(BluetoothLeService.TAG, "onServicesDiscovered received: " + status);
            }

        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == 0) {
                if(UUID.fromString(BluetoothLeService.Characteristic_uuid_TX).equals(characteristic.getUuid())) {
                    BluetoothLeService.this.broadcastUpdate("com.example.bluetooth.le.ACTION_DATA_AVAILABLE", characteristic);
                } else if(UUID.fromString(BluetoothLeService.Characteristic_uuid_FUNCTION).equals(characteristic.getUuid())) {
                    BluetoothLeService.this.broadcastUpdate("com.example.bluetooth.le.ACTION_DATA_AVAILABLE1", characteristic);
                }
            }

        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(UUID.fromString(BluetoothLeService.Characteristic_uuid_TX).equals(characteristic.getUuid())) {
                BluetoothLeService.this.broadcastUpdate("com.example.bluetooth.le.ACTION_DATA_AVAILABLE", characteristic);
            } else if(UUID.fromString(BluetoothLeService.Characteristic_uuid_FUNCTION).equals(characteristic.getUuid())) {
                BluetoothLeService.this.broadcastUpdate("com.example.bluetooth.le.ACTION_DATA_AVAILABLE1", characteristic);
            }

        }
    };
    private final IBinder mBinder = new BluetoothLeService.LocalBinder();

    static {
        UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
        Service_uuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
        Characteristic_uuid_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
        Characteristic_uuid_FUNCTION = "0000ffe2-0000-1000-8000-00805f9b34fb";
    }

    public BluetoothLeService() {
    }

    public String bin2hex(String bin) {
        char[] digital = "0123456789ABCDEF".toCharArray();
        StringBuffer sb = new StringBuffer("");
        byte[] bs = bin.getBytes();

        for(int i = 0; i < bs.length; ++i) {
            int bit = (bs[i] & 240) >> 4;
            sb.append(digital[bit]);
            bit = bs[i] & 15;
            sb.append(digital[bit]);
        }

        return sb.toString();
    }

    public byte[] hex2byte(byte[] b) {
        if(b.length % 2 != 0) {
            throw new IllegalArgumentException("长度不是偶数");
        } else {
            byte[] b2 = new byte[b.length / 2];

            for(int n = 0; n < b.length; n += 2) {
                String item = new String(b, n, 2);
                b2[n / 2] = (byte)Integer.parseInt(item, 16);
            }

            Object b1 = null;
            return b2;
        }
    }

    void deley(int ms) {
        try {
            Thread.currentThread();
            Thread.sleep((long)ms);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

    }

    public String getStringByBytes(byte[] bytes) {
        String result = null;
        String hex = null;
        if(bytes != null && bytes.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(bytes.length);
            byte[] var8 = bytes;
            int var7 = bytes.length;

            for(int var6 = 0; var6 < var7; ++var6) {
                byte byteChar = var8[var6];
                hex = Integer.toHexString(byteChar & 255);
                if(hex.length() == 1) {
                    hex = '0' + hex;
                }

                stringBuilder.append(hex.toUpperCase());
            }

            result = stringBuilder.toString();
        }

        return result;
    }

    private static byte charToByte(char c) {
        return (byte)"0123456789ABCDEF".indexOf(c);
    }

    public static byte[] getBytesByString(String data) {
        byte[] bytes = null;
        if(data != null) {
            data = data.toUpperCase();
            int length = data.length() / 2;
            char[] dataChars = data.toCharArray();
            bytes = new byte[length];

            for(int i = 0; i < length; ++i) {
                int pos = i * 2;
                bytes[i] = (byte)(charToByte(dataChars[pos]) << 4 | charToByte(dataChars[pos + 1]));
            }
        }

        return bytes;
    }

    public String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder(src.length);
        byte[] var6 = src;
        int var5 = src.length;

        for(int var4 = 0; var4 < var5; ++var4) {
            byte byteChar = var6[var4];
            stringBuilder.append(String.format("%02X", new Object[]{Byte.valueOf(byteChar)}));
        }

        return stringBuilder.toString();
    }

    public String bytesToHexString1(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder(src.length);
        byte[] var6 = src;
        int var5 = src.length;

        for(int var4 = 0; var4 < var5; ++var4) {
            byte byteChar = var6[var4];
            stringBuilder.append(String.format(" %02X", new Object[]{Byte.valueOf(byteChar)}));
        }

        return stringBuilder.toString();
    }

    public String bytesToHexString1(byte[] src, int index) {
        if(src == null) {
            return null;
        } else {
            StringBuilder stringBuilder = new StringBuilder(src.length);

            for(int i = index; i < src.length; ++i) {
                stringBuilder.append(String.format(" %02X", new Object[]{Byte.valueOf(src[i])}));
            }

            return stringBuilder.toString();
        }
    }

    public String String_to_HexString0(String str) {
        String st = str.toString();
        byte[] WriteBytes = new byte[st.length()];
        WriteBytes = st.getBytes();
        return this.bytesToHexString(WriteBytes);
    }

    public String String_to_HexString(String str) {
        String st = str.toString();
        byte[] WriteBytes = new byte[st.length()];
        WriteBytes = st.getBytes();
        return this.bytesToHexString1(WriteBytes);
    }

    public byte[] String_to_byte(String str) {
        String st = str.toString();
        byte[] WriteBytes = new byte[st.length()];
        return WriteBytes;
    }

    public String byte_to_String(byte[] byt) {
        String t = new String(byt);
        return t;
    }

    public String byte_to_String(byte[] byt, int index) {
        if(byt == null) {
            return null;
        } else {
            byte[] WriteBytes = new byte[byt.length - index];

            for(int t = index; t < byt.length; ++t) {
                WriteBytes[t - index] = byt[t];
            }

            String var5 = new String(WriteBytes);
            return var5;
        }
    }

    public int sendMessage(String g, boolean string_or_hex_data) {
        int ic = 0;
        if(string_or_hex_data) {
            this.WriteBytes = g.getBytes();
        } else {
            this.WriteBytes = getBytesByString(g);
        }

        int length = this.WriteBytes.length;
        int data_len_20 = length / 20;
        int data_len_0 = length % 20;
        int i = 0;
        byte[] da;
        int gg;
        BluetoothGattCharacteristic var10;
        if(data_len_20 > 0) {
            while(i < data_len_20) {
                da = new byte[20];

                for(gg = 0; gg < 20; ++gg) {
                    da[gg] = this.WriteBytes[20 * i + gg];
                }

                var10 = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
                var10.setValue(da);
                this.mBluetoothGatt.writeCharacteristic(var10);
                this.deley(23);
                ic += 20;
                ++i;
            }
        }

        if(data_len_0 > 0) {
            da = new byte[data_len_0];

            for(gg = 0; gg < data_len_0; ++gg) {
                da[gg] = this.WriteBytes[20 * i + gg];
            }

            var10 = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
            var10.setValue(da);
            this.mBluetoothGatt.writeCharacteristic(var10);
            ic += data_len_0;
            this.deley(23);
        }

        return ic;
    }

    public void function_data(byte[] data) {
        this.WriteBytes = data;
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public int function_data(String data, String Target_address) {
        boolean p = false;
        if(data == null) {
            return 1;
        } else if(data.length() > 20) {
            return 2;
        } else if(Target_address != "" && Target_address != null) {
            if(Target_address.length() != 2) {
                return 4;
            } else {
                String txt = "FAff";
                String value = this.bin2hex(data);
                txt = txt + value;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return 0;
            }
        } else {
            return 3;
        }
    }

    public int function_fc(String data, String Target_address) {
        boolean p = false;
        if(data == null) {
            return 1;
        } else if(data.length() > 20) {
            return 2;
        } else if(Target_address != "" && Target_address != null) {
            if(Target_address.length() != 2) {
                return 4;
            } else {
                String txt = "FB" + Target_address;
                txt = txt + data;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return 0;
            }
        } else {
            return 3;
        }
    }

    public void enable_JDY_ble(int p) {
        try {
            BluetoothGattService e = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid));
            BluetoothGattCharacteristic ale;
            switch(p) {
                case 0:
                    ale = e.getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
                    break;
                case 1:
                    ale = e.getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                    break;
                default:
                    ale = e.getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
            }

            this.mBluetoothGatt.setCharacteristicNotification(ale, true);
            BluetoothGattDescriptor dsc = ale.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            byte[] bytes = new byte[]{1, 0};
            dsc.setValue(bytes);
            this.mBluetoothGatt.writeDescriptor(dsc);
        } catch (NumberFormatException var8) {
            var8.printStackTrace();
        }

    }

    public String get_mem_data(String key) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("jdy-ble", 0);
        String name = sharedPreferences.getString(key, "");
        return name != null && name != ""?name:"123456";
    }

    public void set_mem_data(String key, String values) {
        SharedPreferences mySharedPreferences = this.getSharedPreferences("jdy-ble", 0);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putString(key, values);
        editor.commit();
    }

    public boolean get_password(String password) {
        boolean p = true;
        if(password == null) {
            return false;
        } else if(password.length() != 6) {
            return false;
        } else {
            String txt = "E552";
            String value = this.bin2hex(password);
            txt = txt + value;
            this.WriteBytes = this.hex2byte(txt.toString().getBytes());
            BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
            gg.setValue(this.WriteBytes);
            this.mBluetoothGatt.writeCharacteristic(gg);
            return p;
        }
    }

    public boolean set_password(String password, String new_password) {
        boolean p = true;
        if(password != null && new_password != null) {
            String txt = "E551";
            String value = this.String_to_HexString0(password);
            txt = txt + value;
            value = this.String_to_HexString0(new_password);
            txt = txt + value;
            this.WriteBytes = this.hex2byte(txt.toString().getBytes());
            BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
            gg.setValue(this.WriteBytes);
            this.mBluetoothGatt.writeCharacteristic(gg);
            return p;
        } else {
            return false;
        }
    }

    String getutf8FromString(String str) {
        StringBuffer utfcode = new StringBuffer();

        try {
            byte[] var6;
            int var5 = (var6 = str.getBytes("utf-8")).length;

            for(int var4 = 0; var4 < var5; ++var4) {
                byte e = var6[var4];
                char hex = (char)(e & 255);
                utfcode.append(Integer.toHexString(hex));
            }
        } catch (UnsupportedEncodingException var8) {
            var8.printStackTrace();
        }

        return utfcode.toString();
    }

    public boolean set_name(String name) {
        boolean p = true;
        if(name == null) {
            return false;
        } else {
            String txt = "E661";
            String value = this.bin2hex(name);
            txt = txt + value;
            this.WriteBytes = this.hex2byte(txt.toString().getBytes());
            BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
            gg.setValue(this.WriteBytes);
            this.mBluetoothGatt.writeCharacteristic(gg);
            return p;
        }
    }

    public boolean MC_Set_angle(String angle) {
        int length = angle.length();
        if(angle == null) {
            return false;
        } else if(length == 0) {
            return false;
        } else {
            int angle_int_value = Integer.valueOf(angle).intValue();
            boolean m2 = true;
            boolean p11 = true;
            String txt = "E7f3";
            if(angle_int_value <= 9) {
                txt = txt + "0" + angle_int_value;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
            }

            return p11;
        }
    }

    public boolean MC_set_button(boolean p) {
        boolean m2 = true;
        boolean p11 = true;
        String txt = "E7f1";
        if(p) {
            txt = "E7f1";
            txt = txt + "01";
        } else {
            txt = "E7f2";
            txt = txt + "01";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
        return p11;
    }

    public boolean MC_set_password(String password, String new_password) {
        boolean p = true;
        if(password != null && new_password != null) {
            String txt = "E551";
            String value = this.String_to_HexString0(password);
            txt = txt + value;
            value = this.String_to_HexString0(new_password);
            txt = txt + value;
            this.WriteBytes = this.hex2byte(txt.toString().getBytes());
            BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
            gg.setValue(this.WriteBytes);
            this.mBluetoothGatt.writeCharacteristic(gg);
            return p;
        } else {
            return false;
        }
    }

    public boolean set_IO1(boolean p) {
        boolean p11 = true;
        String txt = "E7f1";
        if(p) {
            txt = txt + "01";
        } else {
            txt = txt + "00";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
        return p11;
    }

    public boolean set_IO2(boolean p) {
        boolean p11 = true;
        String txt = "E7f2";
        if(p) {
            txt = txt + "01";
        } else {
            txt = txt + "00";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
        return p11;
    }

    public boolean set_IO3(boolean p) {
        boolean p11 = true;
        String txt = "E7f3";
        if(p) {
            txt = txt + "01";
        } else {
            txt = txt + "00";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
        return p11;
    }

    public boolean set_IO4(boolean p) {
        boolean p11 = true;
        String txt = "E7f4";
        if(p) {
            txt = txt + "01";
        } else {
            txt = txt + "00";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
        return p11;
    }

    public boolean set_IO_ALL(boolean p) {
        boolean p11 = true;
        String txt;
        if(p) {
            txt = "E7f5";
        } else {
            txt = "E7f0";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
        return p11;
    }

    public void get_IO_ALL() {
        String txt = "E7f6";
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_APP_PASSWORD(String pss) {
        boolean p11 = true;
        String txt = "E555";
        String value = this.bin2hex(pss);
        txt = txt + value;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public boolean set_ibeacon_UUID(String uuid) {
        if(uuid.length() == 36) {
            String v1 = "";
            String v2 = "";
            String v3 = "";
            String v4 = "";
            v1 = uuid.substring(8, 9);
            v2 = uuid.substring(13, 14);
            v3 = uuid.substring(18, 19);
            v4 = uuid.substring(23, 24);
            if(v1.equals("-") && v2.equals("-") && v3.equals("-") && v4.equals("-")) {
                uuid = uuid.replace("-", "");
                uuid = "E111" + uuid;
                this.WriteBytes = this.hex2byte(uuid.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean set_ibeacon_MAJOR(String major) {
        if(major == null) {
            return false;
        } else if(major.length() == 0) {
            return false;
        } else {
            int i = Integer.valueOf(major).intValue();
            String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
            if(vs.length() == 2) {
                vs = "00" + vs;
            } else if(vs.length() == 3) {
                vs = "0" + vs;
            }

            String txt = "E221";
            txt = txt + vs;
            this.WriteBytes = this.hex2byte(txt.toString().getBytes());
            BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
            gg.setValue(this.WriteBytes);
            this.mBluetoothGatt.writeCharacteristic(gg);
            return true;
        }
    }

    public boolean set_ibeacon_MIMOR(String minor) {
        if(minor == null) {
            return false;
        } else if(minor.length() == 0) {
            return false;
        } else {
            int i = Integer.valueOf(minor).intValue();
            String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
            if(vs.length() == 2) {
                vs = "00" + vs;
            } else if(vs.length() == 3) {
                vs = "0" + vs;
            }

            String txt = "E331";
            txt = txt + vs;
            this.WriteBytes = this.hex2byte(txt.toString().getBytes());
            BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
            gg.setValue(this.WriteBytes);
            this.mBluetoothGatt.writeCharacteristic(gg);
            return true;
        }
    }

    public void set_BroadInterval(int interval) {
        String txt = "E441";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(interval)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void get_BroadInterval() {
        String txt = "E442";
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM_OPEN(int pwm) {
        String txt = "E8a1";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(pwm)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM_frequency(int frequency) {
        String txt = "E8a2";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(frequency)});
        if(vs.length() == 2) {
            vs = "00" + vs;
        } else if(vs.length() == 3) {
            vs = "0" + vs;
        }

        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM1_pulse(int pulse) {
        String txt = "E8a3";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(pulse)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM2_pulse(int pulse) {
        String txt = "E8a4";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(pulse)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM3_pulse(int pulse) {
        String txt = "E8a5";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(pulse)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM4_pulse(int pulse) {
        String txt = "E8a6";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(pulse)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_PWM_ALL_pulse(int PWM1_pulse, int PWM2_pulse, int PWM3_pulse, int PWM4_pulse) {
        String txt = "E8a7";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(PWM1_pulse)});
        txt = txt + vs;
        vs = String.format("%02x", new Object[]{Integer.valueOf(PWM2_pulse)});
        txt = txt + vs;
        vs = String.format("%02x", new Object[]{Integer.valueOf(PWM3_pulse)});
        txt = txt + vs;
        vs = String.format("%02x", new Object[]{Integer.valueOf(PWM4_pulse)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_AV_OPEN(int p) {
        String txt = "E9a501";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(p)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_AV_PULSE(int p) {
        String txt = "E9a502";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(p)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_LED_Mode(int i) {
        String txt = "E9b101";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_LED_Brightness(int i) {
        String txt = "E9b102";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_LED_T_J_F(int i) {
        String txt = "E9b103";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_LED_Speed(int i) {
        String txt = "E9b104";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void set_LED_Custom_LEN(int i) {
        String txt = "E9b1A0";
        String vs = String.format("%02x", new Object[]{Integer.valueOf(i)});
        txt = txt + vs;
        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public boolean set_LED_Custom1(String dd) {
        if(dd == null) {
            return false;
        } else {
            int len = dd.length();
            if(len == 24) {
                String txt = "E9b1A1";
                txt = txt + dd;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean set_LED_Custom2(String dd) {
        if(dd == null) {
            return false;
        } else {
            int len = dd.length();
            if(len == 24) {
                String txt = "E9b1A2";
                txt = txt + dd;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean set_LED_Custom3(String dd) {
        if(dd == null) {
            return false;
        } else {
            int len = dd.length();
            if(len == 24) {
                String txt = "E9b1A3";
                txt = txt + dd;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean set_LED_Custom4(String dd) {
        if(dd == null) {
            return false;
        } else {
            int len = dd.length();
            if(len == 24) {
                String txt = "E9b1A4";
                txt = txt + dd;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean set_LED_PAD_color(String dd) {
        if(dd == null) {
            return false;
        } else {
            int len = dd.length();
            if(len == 8) {
                String txt = "E9b1A5";
                txt = txt + dd;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return true;
            } else {
                return false;
            }
        }
    }

    public void set_LED_OPEN(boolean p) {
        String txt = "E9b1A9";
        if(p) {
            txt = txt + "01";
        } else {
            txt = txt + "00";
        }

        this.WriteBytes = this.hex2byte(txt.toString().getBytes());
        BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        gg.setValue(this.WriteBytes);
        this.mBluetoothGatt.writeCharacteristic(gg);
    }

    public void Delay_ms(int ms) {
        try {
            Thread.currentThread();
            Thread.sleep((long)ms);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

    }

    public Boolean set_uuid(String txt) {
        if(txt.length() == 36) {
            String v1 = "";
            String v2 = "";
            String v3 = "";
            String v4 = "";
            v1 = txt.substring(8, 9);
            v2 = txt.substring(13, 14);
            v3 = txt.substring(18, 19);
            v4 = txt.substring(23, 24);
            if(v1.equals("-") && v2.equals("-") && v3.equals("-") && v4.equals("-")) {
                txt = txt.replace("-", "");
                txt = "AAF1" + txt;
                this.WriteBytes = this.hex2byte(txt.toString().getBytes());
                BluetoothGattCharacteristic gg = this.mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
                gg.setValue(this.WriteBytes);
                this.mBluetoothGatt.writeCharacteristic(gg);
                return Boolean.valueOf(true);
            } else {
                return Boolean.valueOf(false);
            }
        } else {
            return Boolean.valueOf(false);
        }
    }

    public int get_connected_status(List<BluetoothGattService> gattServices) {
        boolean jdy_ble_server = false;
        boolean jdy_ble_ffe1 = false;
        boolean jdy_ble_ffe2 = false;
        String LIST_NAME1 = "NAME";
        String LIST_UUID1 = "UUID";
        String uuid = null;
        String unknownServiceString = "未知服务";
        String unknownCharaString = "未知字符";
        ArrayList gattServiceData = new ArrayList();
        ArrayList gattCharacteristicData = new ArrayList();
        Iterator var13 = gattServices.iterator();

        while(var13.hasNext()) {
            BluetoothGattService gattService = (BluetoothGattService)var13.next();
            HashMap currentServiceData = new HashMap();
            uuid = gattService.getUuid().toString();
            currentServiceData.put("NAME", SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put("UUID", uuid);
            gattServiceData.add(currentServiceData);
            ArrayList gattCharacteristicGroupData = new ArrayList();
            List gattCharacteristics = gattService.getCharacteristics();
            ArrayList charas = new ArrayList();
            if(Service_uuid.equals(uuid)) {
                jdy_ble_server = true;
            }

            Iterator var19 = gattCharacteristics.iterator();

            while(var19.hasNext()) {
                BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic)var19.next();
                charas.add(gattCharacteristic);
                HashMap currentCharaData = new HashMap();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put("NAME", SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put("UUID", uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                if(jdy_ble_server) {
                    if(Characteristic_uuid_TX.equals(uuid)) {
                        jdy_ble_ffe1 = true;
                    } else if(Characteristic_uuid_FUNCTION.equals(uuid)) {
                        jdy_ble_ffe2 = true;
                    }
                }
            }

            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        if(jdy_ble_ffe1 && jdy_ble_ffe2) {
            return 2;
        } else if(jdy_ble_ffe1 && !jdy_ble_ffe2) {
            return 1;
        } else {
            return 0;
        }
    }

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        this.sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        Log.d("getUuid", " len = " + characteristic.getUuid());
        if(UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int data = characteristic.getProperties();
            boolean format = true;
            if((data & 1) != 0) {
                format = true;
            } else {
                format = true;
            }
        } else {
            byte[] data1;
            if(UUID.fromString(Characteristic_uuid_TX).equals(characteristic.getUuid())) {
                data1 = characteristic.getValue();
                if(data1 != null && data1.length > 0) {
                    intent.putExtra("com.example.bluetooth.le.EXTRA_DATA", data1);
                }
            } else if(UUID.fromString(Characteristic_uuid_FUNCTION).equals(characteristic.getUuid())) {
                data1 = characteristic.getValue();
                if(data1 != null && data1.length > 0) {
                    intent.putExtra("com.example.bluetooth.le.EXTRA_DATA1", data1);
                }
            }
        }

        this.sendBroadcast(intent);
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        this.close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if(this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager)this.getSystemService("bluetooth");
            if(this.mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        if(this.mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        } else {
            return true;
        }
    }

    public boolean connect(String address) {
        if(this.mBluetoothAdapter != null && address != null) {
            if(this.mBluetoothDeviceAddress != null && address.equals(this.mBluetoothDeviceAddress) && this.mBluetoothGatt != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if(this.mBluetoothGatt.connect()) {
                    this.mConnectionState = 1;
                    return true;
                } else {
                    return false;
                }
            } else {
                BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);
                if(device == null) {
                    Log.w(TAG, "Device not found.  Unable to connect.");
                    return false;
                } else {
                    this.mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback);
                    Log.d(TAG, "Trying to create a new connection.");
                    this.mBluetoothDeviceAddress = address;
                    this.mConnectionState = 1;
                    return true;
                }
            }
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
    }

    public void disconnect() {
        if(this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            this.mBluetoothGatt.disconnect();
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
    }

    public boolean isconnect() {
        return this.mBluetoothGatt.connect();
    }

    public void close() {
        if(this.mBluetoothGatt != null) {
            this.mBluetoothGatt.close();
            this.mBluetoothGatt = null;
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            this.mBluetoothGatt.readCharacteristic(characteristic);
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if(this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            this.mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            if(UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                this.mBluetoothGatt.writeDescriptor(descriptor);
            }

        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        return this.mBluetoothGatt == null?null:this.mBluetoothGatt.getServices();
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public static enum function_type {
        iBeacon_UUID,
        iBeacon_Major,
        iBeacon_Minor,
        adv_intverl,
        pin_password,
        name,
        GPIO,
        PWM,
        Other,
        Power,
        RTC;

        private function_type() {
        }
    }
}

