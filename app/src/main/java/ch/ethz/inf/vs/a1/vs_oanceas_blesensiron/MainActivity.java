package ch.ethz.inf.vs.a1.vs_oanceas_blesensiron;


import android.os.ParcelUuid;

import android.Manifest;
import android.bluetooth.le.ScanFilter;
import android.content.pm.PackageManager;

import android.os.Bundle;


import android.bluetooth.BluetoothAdapter;
import android.support.v4.app.ActivityCompat;
import android.bluetooth.BluetoothManager;

import android.content.Context;
import android.os.Handler;


import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

import android.app.ListActivity;
import android.widget.Toast;


import java.util.List;
import java.util.Arrays;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;


public class MainActivity extends ListActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private List<ScanFilter> scanFilterList;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mHandler = new Handler();

        // warn if bluetooth not supported -> e.g in emulator
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // request BluetoothManager
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // create ScanFilter List for startLeScan
        ScanFilter.Builder aBuilder = new ScanFilter.Builder();
        ScanFilter.Builder bBuilder = new ScanFilter.Builder();

        ParcelUuid a = new ParcelUuid(SensirionSHT31UUIDS.UUID_HUMIDITY_SERVICE);
        aBuilder.setServiceUuid(a);
        ParcelUuid b = new ParcelUuid(SensirionSHT31UUIDS.UUID_TEMPERATURE_SERVICE);
        bBuilder.setServiceUuid(b);

        scanFilterList = Arrays.asList(aBuilder.build(),bBuilder.build());

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable -> request it (in-app)
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        0);

                // alternative over settings
                // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    // scan for devices -> asynchronous: mLeScanCallback to receive results
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            //mBluetoothAdapter.startLeScan(mLeScanCallback);
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanFilterList,null,mScanCallback);

        } else {
            mScanning = false;
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        }


    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);


            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;

    }


}

