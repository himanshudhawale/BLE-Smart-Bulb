package com.example.inclass06;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Handler mHandler;
    public boolean mScanning = false;
    int MY_PERMISSIONS_REQUEST_LOCATION = 0;

    Button buttonON, buttonOFF;

    TextView textView;
    ImageView bulbOn, bulbOff;

    ToggleButton button;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> set= new HashSet<>();
//    static UUID UUIDGIVEN= UUID.fromString("dfbe3c19-c536-1d71-a54f-206b376a479a");  //Himanshu
    static UUID UUIDGIVEN= UUID.fromString("df1461fe-a719-d877-1816-2feec18481cd"); //Tejashree
//      static UUID UUIDGIVEN= UUID.fromString("df75ab9a-7b42-f39f-116a-b659a643a75c"); //Manali

    static UUID MY_CHARACTERISTIC = UUID.fromString("0CED9345-B31F-457D-A6A2-B3DB9B03E39A");
    static UUID MY_CHARACTERISTIC_BULB = UUID.fromString("FB959362-F26E-43A9-927C-7E17D8FB2D8D");
    static UUID MY_CHARACTERISTIC_BEEP = UUID.fromString("EC958823-F26E-43A9-927C-7E17D8F32A90");

    BluetoothDevice mDevice;

    BluetoothGatt gatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private String mBluetoothDeviceAddress;

    Map<String, String> map = new HashMap<>();
    Map<String, UUID> map2 = new HashMap<>();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        buttonON = findViewById(R.id.buttonOnId);
        buttonOFF = findViewById(R.id.buttonOffId);
        textView = findViewById(R.id.tempID);
        bulbOn = findViewById(R.id.imageView3);
        bulbOff = findViewById(R.id.imageView2);
        bulbOn.setVisibility(View.INVISIBLE);
        button = findViewById(R.id.toggleButton);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            }
        }


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        scanLeDevice(true);

        buttonON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData(new byte[] {Byte.parseByte("1")}, MY_CHARACTERISTIC_BULB);
            }
        });
        buttonOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData(new byte[] {Byte.parseByte("0")}, MY_CHARACTERISTIC_BULB);

            }
        });
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    sendData(new byte[] {Byte.parseByte("1")}, MY_CHARACTERISTIC_BEEP);
                }else{
                    sendData(new byte[] {Byte.parseByte("0")}, MY_CHARACTERISTIC_BEEP);

                }
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
            }
            return;
        }

        // other 'case' lines to check for other
        // permissions this app might request
    }

    public void markCharForNotification(BluetoothGattCharacteristic readableChar) {

        gatt.setCharacteristicNotification(readableChar, true);

        List<BluetoothGattDescriptor> listDescr = readableChar.getDescriptors();

        BluetoothGattDescriptor descriptor = listDescr.get(0);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);

    }



    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            UUID[] uuids = new UUID[1];
            uuids[0] = UUIDGIVEN;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(uuids,mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("lol", device.toString());
                            mDevice = device;
                            connect(device.getAddress());
                        }
                    });
                }
            };

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        set.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        List<BluetoothGattCharacteristic> chars = new ArrayList<>();


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("demo", "Connected to GATT client. Attempting to start service discovery");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("demo", "Disconnected from GATT client");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                }
            });
            readCounterCharacteristic(characteristic);

        }



        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {

            displayGattServices(gatt.getServices());


            // this will get called after the client initiates a luetoothGatt.discoverServices() call
            BluetoothGattService service = gatt.getService(UUIDGIVEN);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(MY_CHARACTERISTIC);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            gatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(MY_CHARACTERISTIC_BULB);
            characteristic1.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            gatt.setCharacteristicNotification(characteristic1, true);

            // Write on the config descriptor to be notified when the value changes
            BluetoothGattDescriptor descriptor1 =
                    characteristic1.getDescriptor(UUID.fromString("00002934-0000-1000-8000-00805f9b34fb"));
            descriptor1.setValue(new byte[] {1});
            gatt.writeDescriptor(descriptor1);

            // Write on the config descriptor to be notified when the value changes
            BluetoothGattDescriptor descriptor =
                    characteristic.getDescriptor(UUID.fromString("00002934-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(new byte[] {1});
            gatt.writeDescriptor(descriptor);

            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {

                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (MY_CHARACTERISTIC.equals(descriptor.getUuid())) {
                BluetoothGattCharacteristic characteristic = gatt
                        .getService(UUIDGIVEN)
                        .getCharacteristic(MY_CHARACTERISTIC);
                gatt.readCharacteristic(characteristic);
            }
        }

        @Override
        public void onReliableWriteCompleted (BluetoothGatt gatt, int status){
            super.onReliableWriteCompleted(gatt, status);
            BluetoothGattCharacteristic characteristic = gatt
                    .getService(UUIDGIVEN)
                    .getCharacteristic(MY_CHARACTERISTIC_BULB);
            markCharForNotification(characteristic);

            readCounterCharacteristic(characteristic);
        }



        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            readCounterCharacteristic(characteristic);
        }

    };

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w("Tag", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && gatt != null) {
            Log.d("Tag", "Trying to use an existing mBluetoothGatt for connection.");
            if (gatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("Tag", "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        gatt = device.connectGatt(this, false, gattCallback);
        Log.d("Tag", "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    private void readCounterCharacteristic(final BluetoothGattCharacteristic
                                                   characteristic) {
        if (MY_CHARACTERISTIC.equals(characteristic.getUuid())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] data = characteristic.getValue();
                    // Update UI
                    textView.setText(new String(data));
                }
            });
        }

            else if (MY_CHARACTERISTIC_BULB.equals(characteristic.getUuid())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update UI
                        String s = new String(characteristic.getValue());
                        if (s.equals("1")) {
                            bulbOff.setVisibility(View.INVISIBLE);
                            bulbOn.setVisibility(View.VISIBLE);

                        } else {
                            bulbOff.setVisibility(View.VISIBLE);
                            bulbOn.setVisibility(View.INVISIBLE);
                        }

                    }
                });
            }
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {

            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);

            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                }
            });
            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            Log.d("lolde", String.valueOf(gattCharacteristics.size()));

            for (final BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                for(BluetoothGattDescriptor cc : gattCharacteristic.getDescriptors())
                {
                    Log.d("lolde", String.valueOf(cc.getUuid()));
                    map2.put(charUuid,(cc.getUuid()));

                }
                Log.d("lolde", map2.toString());
                Log.d("lolde", map.toString());


                if(charUuid.equals("FB959362-F26E-43A9-927C-7E17D8FB2D8D"))
                {
                    map.put("bulb", charUuid);
                }
                else if(charUuid.equals("0CED9345-B31F-457D-A6A2-B3DB9B03E39A"))
                {
                    map.put("temperature",charUuid);

                }
                else if(charUuid.equals("EC958823-F26E-43A9-927C-7E17D8F32A90"))
                {
                    map.put("beep", charUuid);

                }

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {

                    }
                });

            }
        }
    }

    public void sendData(byte[] data, UUID blue){
        String lService = UUIDGIVEN.toString();
        String lCharacteristic = blue.toString();
        BluetoothGattService mBluetoothLeService = null;
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = null;

        for (BluetoothGattService service : gatt.getServices()) {
            if ((service == null) || (service.getUuid() == null)) {

                Log.d("TAG","Something is null");
                continue;
            }
            if (lService.equalsIgnoreCase(service.getUuid().toString())) {

                Log.d("TAG","service.getUuid().toString()="+service.getUuid().toString());
                mBluetoothLeService = service;
            }
        }
        if(mBluetoothLeService!=null) {
            mBluetoothGattCharacteristic =
                    mBluetoothLeService.getCharacteristic((blue));
        }
        else{
            Log.d("TAG","mBluetoothLeService is null");
        }

        if(mBluetoothGattCharacteristic!=null) {
            mBluetoothGattCharacteristic.setValue(data);

            boolean write = gatt.writeCharacteristic(mBluetoothGattCharacteristic);

            gatt.executeReliableWrite();

            Log.d("TAG","writeCharacteristic:"+write);
        }
        else{
            Log.d("TAG", "mBluetoothGattCharacteristic is null");
        }
    }


}

