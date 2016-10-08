package ch.ethz.inf.vs.a1.vs_oanceas_blesensiron;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DeviceControlActivity extends Activity {

    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGatt mBluetoothGatt;

    private GraphView graph;
    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Log.e(TAG, mDeviceName);

        initialize();

        final boolean result = connect(mDeviceAddress);
        Log.e(TAG, "Connect request result=" + result);

        // graph-view
        graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        Viewport viewport = graph.getViewport();
        viewport.setScrollable(true);
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(40);
        viewport.setMinY(0);
        viewport.setMaxY(100);
        


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
    @Override
    protected void onPause() {
        super.onDestroy();
        disconnect();
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_CONNECTED) {

                Log.e(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.e(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());
                // mBluetoothGatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                Log.i(TAG, "Disconnected from GATT server.");

               // final boolean result = connect(mDeviceAddress);
               // Log.e(TAG, "Connect request result=" + result);

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "onServicesDiscovered");
            /*if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }*/

            // Humidity Service
            BluetoothGattService humidityService =
                    mBluetoothGatt.getService(SensirionSHT31UUIDS.UUID_HUMIDITY_SERVICE);
            BluetoothGattCharacteristic humidityCharacteristic =
                    humidityService.getCharacteristic(SensirionSHT31UUIDS.UUID_HUMIDITY_CHARACTERISTIC);

            // Notification Descriptor
            BluetoothGattDescriptor descriptor =
                    new BluetoothGattDescriptor(SensirionSHT31UUIDS.NOTIFICATION_DESCRIPTOR_UUID,BluetoothGattDescriptor.PERMISSION_WRITE);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            humidityCharacteristic.addDescriptor(descriptor);
            mBluetoothGatt.writeDescriptor(descriptor);

            // setup Notifications
            mBluetoothGatt.setCharacteristicNotification(humidityCharacteristic,true);



        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e(TAG, "onCharacteristicRead");
           /* if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }*/
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "onCharacteristicChanged");
           /* broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);*/

            // display Data
            if(characteristic.getValue() != null){
                Log.e(TAG, String.valueOf(convertRawValue(characteristic.getValue())));
                series.appendData(new DataPoint(lastX++,convertRawValue(characteristic.getValue())),true,40);

            }else{
                Log.e(TAG, "Data = null");
            }



        }
    };



    private float convertRawValue(byte[] raw){
        ByteBuffer wrapper = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        return wrapper.getFloat();
    }
}
