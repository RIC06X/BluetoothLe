package yanjiex.calit2.uci;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.bluetooth.bledemo.BleNamesResolver;
import org.bluetooth.bledemo.BleWrapper;
import org.bluetooth.bledemo.BleWrapperUiCallbacks;
import org.bluetooth.bledemo.R;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.util.UUID.fromString;
import static yanjiex.calit2.uci.MainActivity.EXTRAS_DEVICE_ADDRESS;
import static yanjiex.calit2.uci.MainActivity.EXTRAS_DEVICE_NAME;

/**
 * Created by yanjie on 2/13/18.
 */

public class Settings extends Activity {
    private String mDeviceName;
    private String mDeviceAddress;
    private BleWrapper mBleWrapper;
    final UUID URO_SERV = fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    final UUID TARE_CH  = fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    final UUID Threashold_SERV = fromString("f3a7e7e3-2f1c-41d0-91f0-01a4c0a5c4a8");
    public static final UUID THRESHOLD1 = fromString("b97f5987-dbe5-4bae-af26-ff34cbc32f07");
    public static final UUID THRESHOLD2 = fromString("c19c5488-bd8e-428e-a665-6f6a5d36f1d7");
    public static final UUID THRESHOLD3 = fromString("ef85fb2f-6256-4ba7-b7be-3af8fdbecab1");
    public static final UUID THRESHOLD4 = fromString("a732fcf0-2060-4026-9cac-775d0aade77b");
    public static final UUID THRESHOLD5 = fromString("e9cd9578-485e-41b9-b439-7bef16382342");
    boolean isSet = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        final Intent intent = getIntent();
        if (intent!=null){
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        }
        this.getActionBar().setDisplayHomeAsUpEnabled(true);
        GetCurrentThreshold();
    }

    TextView oldValue1, oldValue2, oldValue3, oldValue4, oldValue5;
    EditText ThresholdNum1,ThresholdNum2,ThresholdNum3,ThresholdNum4,ThresholdNum5;
    Button SaveSetting,ReadThreshold, CalibrateStart, CalibrateEnd;

    @Override
    public void onBackPressed() {
        mBleWrapper.diconnect();
        mBleWrapper.close();
        super.onBackPressed();

    }

    //used for processing sending strings
    public byte[] parseHexStringToBytes(final String hex) {
        String tmp = hex.substring(2).replaceAll("[^[0-9][a-f]]", "");
        byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally

        String part = "";

        for(int i = 0; i < bytes.length; ++i) {
            part = "0x" + tmp.substring(i*2, i*2+2);
            bytes[i] = Long.decode(part).byteValue();
        }

        return bytes;
    }

    public void getView() {
        //Old threshold value
        oldValue1 = findViewById(R.id.oldValueText1);
        oldValue2 = findViewById(R.id.oldValueText2);
        oldValue3 = findViewById(R.id.oldValueText3);
        oldValue4 = findViewById(R.id.oldValueText4);
        oldValue5 = findViewById(R.id.oldValueText5);

        //New Value
        ThresholdNum1 = findViewById(R.id.ThreshNumInput1);
        ThresholdNum2 = findViewById(R.id.ThreshNumInput2);
        ThresholdNum3 = findViewById(R.id.ThreshNumInput3);
        ThresholdNum4 = findViewById(R.id.ThreshNumInput4);
        ThresholdNum5 = findViewById(R.id.ThreshNumInput5);

        SaveSetting = findViewById(R.id.SaveThreshold);
        CalibrateStart = findViewById(R.id.CalibrateStart);
        CalibrateEnd = findViewById(R.id.CalibrateEnd);
        ReadThreshold = findViewById(R.id.ReadThreshold);

        ReadThreshold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetCurrentThreshold();
                Toast.makeText(getApplicationContext(), "Read threshold command sent", Toast.LENGTH_SHORT).show();
            }
        });


        //Save threshold values
        SaveSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String calibV = "0x26";     //"&" correspond with the save function in the ESP32
                        byte[] eepromSetting = parseHexStringToBytes(calibV);
                        if (mBleWrapper.isConnected()) {
                            BluetoothGatt gatt = mBleWrapper.getGatt();
                            BluetoothGattCharacteristic threashold1 = gatt.getService(Threashold_SERV).getCharacteristic(THRESHOLD1);
                            BluetoothGattCharacteristic threashold2 = gatt.getService(Threashold_SERV).getCharacteristic(THRESHOLD2);
                            BluetoothGattCharacteristic threashold3 = gatt.getService(Threashold_SERV).getCharacteristic(THRESHOLD3);
                            BluetoothGattCharacteristic threashold4 = gatt.getService(Threashold_SERV).getCharacteristic(THRESHOLD4);
                            BluetoothGattCharacteristic threashold5 = gatt.getService(Threashold_SERV).getCharacteristic(THRESHOLD5);
                            //write threshold values to device

                            try{
                                MainActivity.Threshold1 = Float.parseFloat(ThresholdNum1.getText().toString());
                                MainActivity.Threshold2 = Float.parseFloat(ThresholdNum2.getText().toString());
                                MainActivity.Threshold3 = Float.parseFloat(ThresholdNum3.getText().toString());
                                MainActivity.Threshold4 = Float.parseFloat(ThresholdNum4.getText().toString());
                                MainActivity.Threshold5 = Float.parseFloat(ThresholdNum5.getText().toString());
                                Float.toString(MainActivity.Threshold1);

                                mBleWrapper.writeDataToCharacteristic(threashold1, ThresholdNum1.getText().toString().getBytes());
                                Thread.sleep(230);
                                mBleWrapper.writeDataToCharacteristic(threashold2, ThresholdNum2.getText().toString().getBytes());
                                Thread.sleep(230);
                                mBleWrapper.writeDataToCharacteristic(threashold3, ThresholdNum3.getText().toString().getBytes());
                                Thread.sleep(230);
                                mBleWrapper.writeDataToCharacteristic(threashold4, ThresholdNum4.getText().toString().getBytes());
                                Thread.sleep(230);
                                mBleWrapper.writeDataToCharacteristic(threashold5, ThresholdNum5.getText().toString().getBytes());
                                Thread.sleep(230);
//
//                                MainActivity.Threshold1 = Integer.parseInt(ThresholdNum1.getText().toString());
//                                MainActivity.Threshold2 = Integer.parseInt(ThresholdNum2.getText().toString());
//                                MainActivity.Threshold3 = Integer.parseInt(ThresholdNum3.getText().toString());
//                                MainActivity.Threshold4 = Integer.parseInt(ThresholdNum4.getText().toString());
//                                MainActivity.Threshold5 = Integer.parseInt(ThresholdNum5.getText().toString());
                                MainActivity.shouldChangeChart = true;
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }

                            //write to EEPROM
                            BluetoothGattCharacteristic saveToeeprom = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                            mBleWrapper.writeDataToCharacteristic(saveToeeprom, eepromSetting);
                        }
                    }
                }).start();
                Toast.makeText(getApplicationContext(), "Save threshold command sent, Please wait 3 second", Toast.LENGTH_LONG).show();
            }
        });


        CalibrateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x24";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.isConnected()) {
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                    isSet = true;
                }
            }
        });

        CalibrateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x23";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.isConnected()) {
                    if (isSet) {
                        BluetoothGatt gatt = mBleWrapper.getGatt();
                        BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                        mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                        isSet = false;
                    } else {
                        Toast.makeText(getApplicationContext(), "Please press the Calibrate Start button first.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void GetCurrentThreshold() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mBleWrapper != null) {
                        BluetoothGatt gatt = mBleWrapper.getGatt();
                        BluetoothGattService service = gatt.getService(Threashold_SERV);
                        BluetoothGattCharacteristic c = service.getCharacteristic(THRESHOLD1);
                        mBleWrapper.requestCharacteristicValue(c);
                        Thread.sleep(230);
                        c = service.getCharacteristic(THRESHOLD2);
                        mBleWrapper.requestCharacteristicValue(c);
                        Thread.sleep(230);
                        c = service.getCharacteristic(THRESHOLD3);
                        mBleWrapper.requestCharacteristicValue(c);
                        Thread.sleep(230);
                        c = service.getCharacteristic(THRESHOLD4);
                        mBleWrapper.requestCharacteristicValue(c);
                        Thread.sleep(230);
                        c = service.getCharacteristic(THRESHOLD5);
                        mBleWrapper.requestCharacteristicValue(c);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Toast.makeText(getApplicationContext(), "Please wait 2 second", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBleWrapper == null) mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
            //-----------------------------------------BLE_WrapperUiCallBack--START---------------------------------------
            String LOGTAG = "BLE WRAPPER";
            @Override
            public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {

            }
            @Override
            public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Device Connected","YES");
                    }
                });
            }
            @Override
            public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Device Disconnected","YES");
                    }
                });
            }

            @Override
            public void uiAvailableServices(final BluetoothGatt gatt, final BluetoothDevice device, final List<BluetoothGattService> services) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d("IN uiAvailableServce","YES");
                        for(BluetoothGattService service : services) {
                            String uuid = service.getUuid().toString().toLowerCase(Locale.getDefault());
                            String name = BleNamesResolver.resolveServiceName(uuid);
                            String type = (service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY) ? "Primary" : "Secondary";
                            if (name=="Urology service"){
                                //mDeviceStatus.append(name+"\n");
                                uiCharacteristicForService(gatt,device,service,service.getCharacteristics());
                            }
                        }
                    }
                });

            }
            @Override
            public void uiCharacteristicForService(final BluetoothGatt gatt, final BluetoothDevice device, final BluetoothGattService service, final List<BluetoothGattCharacteristic> chars) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("uiCharForService","YES");
                        String uuid = service.getUuid().toString().toLowerCase(Locale.getDefault());
                        String name = BleNamesResolver.resolveServiceName(uuid);
                        if (name=="Urology service"){
                            for(BluetoothGattCharacteristic ch : chars) {
                                String uuidC = ch.getUuid().toString().toLowerCase(Locale.getDefault());
                                String nameC = BleNamesResolver.resolveCharacteristicName(uuidC);
                                //mDeviceStatus.append(nameC+"\n");
                                uiCharacteristicsDetails(gatt,device,service,ch);
                                //AUTO READ, CAN BE disabled
                                if (nameC == "Weight"){
                                    mBleWrapper.setNotificationForCharacteristic(ch, true);
                                }
                            }
                        }}
                });
            }
            //Resolve little endian coded floating number
            public float resolveByteFloat(byte[] mRawValue){
                float mFloatValue = 0;
                String temp1;
                String temp2;
                String temp3;
                String temp0;
                String sbits;

                //mRawValue is the Hex String,  mFloatValue is the float transfered from BLE;
                if (mRawValue != null && mRawValue.length > 0) {
                    //Convert rawData to binary strings
                    temp0 =("0000000" + Integer.toBinaryString(0xFF & mRawValue[3])).replaceAll(".*(.{8})$", "$1");
                    temp1 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[2])).replaceAll(".*(.{8})$", "$1");
                    temp2 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[1])).replaceAll(".*(.{8})$", "$1");
                    temp3 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[0])).replaceAll(".*(.{8})$", "$1");
                    sbits = temp0+temp1+temp2+temp3;

                    //Check the positive float and negative float
                    if (sbits.charAt(0)=='0'){
                        int bits = Integer.valueOf(sbits,2);
                        mFloatValue =Float.intBitsToFloat(bits);
                    }else{
                        sbits = sbits.substring(1);
                        int bits = Integer.valueOf(sbits,2);
                        mFloatValue =-Float.intBitsToFloat(bits);
                    }
                }
                return mFloatValue;
            }
            @Override
            public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
                byte[] mRawValue= characteristic.getValue();
                final float mFloatValue = resolveByteFloat(mRawValue);
                Log.d(LOGTAG, "uiCharacteristicsDetails: " + mFloatValue);

            }

            //CRUCIAL IMPORTANT

            @Override
            public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
                Log.d("Setting: ", "IN SIDE THE SETTING");
                if (ch.getUuid().equals(THRESHOLD1)){
                    Log.d("Setting: ","The current UUID is: "+ch.getUuid().toString()+"The target uuid is: "+THRESHOLD1.toString());
                    Log.d("Setting: ", "The raw data is :  "+ strValue);
                    oldValue1.setText(strValue);
                }
                if (ch.getUuid().equals(THRESHOLD2)){
                    Log.d("Setting: ","The current UUID is: "+ch.getUuid().toString()+"The target uuid is: "+THRESHOLD2.toString());
                    Log.d("Setting: ", "The raw data is :  "+ strValue);
                    oldValue2.setText(strValue);
                }
                if (ch.getUuid().equals(THRESHOLD3)){
                    Log.d("Setting: ","The current UUID is: "+ch.getUuid().toString()+"The target uuid is: "+THRESHOLD3.toString());
                    Log.d("Setting: ", "The raw data is :  "+ strValue);
                    oldValue3.setText(strValue);
                }
                if (ch.getUuid().equals(THRESHOLD4)){
                    Log.d("Setting: ","The current UUID is: "+ch.getUuid().toString()+"The target uuid is: "+THRESHOLD4.toString());
                    Log.d("Setting: ", "The raw data is :  "+ strValue);
                    oldValue4.setText(strValue);
                }
                if (ch.getUuid().equals(THRESHOLD5)){
                    Log.d("Setting: ","The current UUID is: "+ch.getUuid().toString()+"The target uuid is: "+THRESHOLD5.toString());
                    Log.d("Setting: ", "The raw data is :  "+ strValue);
                    oldValue5.setText(strValue);
                }
            }

            @Override
            public void uiGotNotification(final BluetoothGatt gatt, final BluetoothDevice device, final BluetoothGattService service, final BluetoothGattCharacteristic characteristic) {
                String ch = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString());
                Log.d(LOGTAG,  "uiGotNotification: " + ch);

            }
            @Override
            public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, final String description) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(), "Writing to " + description + " was finished successfully!", Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override
            public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, final String description) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Writing to " + description + " FAILED!", Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override
            public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, final int rssi) {
            }
            //-----------------------------------------BLE_WrapperUiCallBack--END---------------------------------------
        });

        if(mBleWrapper.initialize() == false) {
            finish();
        }
        mBleWrapper.connect(mDeviceAddress);
        Log.d("Device address is", mDeviceAddress);
        getView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("OnStop","YES");
    }
}

