/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iswitch.iswitch;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity implements OnClickListener {
    // private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private DeviceListAdapter mDevListAdapter;
    ToggleButton tb_on_off;
    TextView btn_searchDev;
    Button btn_aboutUs;
    ListView lv_bleList;

    Timer timer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        lv_bleList = (ListView) findViewById(R.id.lv_bleList);

//		//tb_on_off = (ToggleButton) findViewById(R.id.tb_on_off);
//		btn_searchDev = (TextView) findViewById(R.id.btn_searchDev);
//		btn_aboutUs = (Button) findViewById(R.id.btn_aboutUs);
//		
//		btn_aboutUs.setText("");
//		btn_aboutUs.setOnClickListener(this);
//		btn_searchDev.setOnClickListener(this);

        mDevListAdapter = new DeviceListAdapter();
        lv_bleList.setAdapter(mDevListAdapter);

        lv_bleList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (mDevListAdapter.getCount() > 0) {
                    /*
                    BluetoothDevice device = mDevListAdapter.getItem(position);
					Intent intent = new Intent(DeviceScanActivity.this,
							DeviceControlActivity.class);
					Bundle bundle = new Bundle();
					bundle.putString("BLEDevName", device.getName());
					bundle.putString("BLEDevAddress", device.getAddress());
					intent.putExtras(bundle);
					DeviceScanActivity.this.startActivity(intent);
					*/


                    BluetoothDevice device1 = mDevListAdapter.getItem(position);
                    if (device1 == null) return;
                    Intent intent1 = new Intent(DeviceScanActivity.this,
                            MainActivity.class);
                    intent1.putExtra(EXTRAS_DEVICE_NAME, device1.getName());
                    intent1.putExtra(EXTRAS_DEVICE_ADDRESS, device1.getAddress());
                    if (mScanning) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        mScanning = false;
                    }
                    startActivity(intent1);
                }
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case 0:
                break;
//		case R.id.btn_searchDev:
//			//scanLeDevice(true);
//			break;

//		case R.id.btn_aboutUs:
//			 Intent intent = new Intent();
//		        intent.setAction("android.intent.action.VIEW");
//		        Uri content_url = Uri.parse("https://item.taobao.com/item.htm?spm=a1z10.1-c.w4004-11559702484.2.uKkX9H&id=44163359933");
//		        intent.setData(content_url);
//		        startActivity(intent);
//			break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                //mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                //mDevListAdapter.;
                mDevListAdapter.clear();
                mDevListAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }


    private LeScanCallback mLeScanCallback = new LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDevListAdapter.addDevice(device);
                    mDevListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onResume() {//打开APP时扫描设备
        super.onResume();
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {//停止扫描
        super.onPause();
        scanLeDevice(false);
    }


    class DeviceListAdapter extends BaseAdapter {

        private List<BluetoothDevice> mBleArray;
        private ViewHolder viewHolder;

        public DeviceListAdapter() {
            mBleArray = new ArrayList<BluetoothDevice>();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mBleArray.contains(device)) {
                mBleArray.add(device);
            }
        }

        public void clear() {
            mBleArray.clear();
        }

        @Override
        public int getCount() {
            return mBleArray.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return mBleArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(DeviceScanActivity.this).inflate(
                        R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.tv_devName = (TextView) convertView
                        .findViewById(R.id.device_name);
                viewHolder.tv_devAddress = (TextView) convertView
                        .findViewById(R.id.device_address);
                convertView.setTag(viewHolder);
            } else {
                convertView.getTag();
            }

            // add-Parameters
            BluetoothDevice device = mBleArray.get(position);
            String devName = device.getName();
            if (devName != null && devName.length() > 0) {
                viewHolder.tv_devName.setText(devName);
            } else {
                viewHolder.tv_devName.setText("unknow-device");
            }
            viewHolder.tv_devAddress.setText(device.getAddress());

            return convertView;
        }

    }

    class ViewHolder {
        TextView tv_devName, tv_devAddress;
    }


}