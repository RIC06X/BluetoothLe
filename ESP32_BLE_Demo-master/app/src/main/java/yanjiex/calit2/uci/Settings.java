package yanjiex.calit2.uci;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.bluetooth.bledemo.BleWrapper;
import org.bluetooth.bledemo.BleWrapperUiCallbacks;
import org.bluetooth.bledemo.R;

import java.nio.ByteBuffer;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static java.util.UUID.fromString;
import static yanjiex.calit2.uci.MainActivity.EXTRAS_DEVICE_ADDRESS;
import static yanjiex.calit2.uci.MainActivity.EXTRAS_DEVICE_NAME;
import static yanjiex.calit2.uci.MainActivity.EXTRAS_DEVICE_RSSI;

/**
 * Created by yanjie on 2/13/18.
 */

public class Settings extends Activity {
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceRSSI;
    private BleWrapper mBleWrapper;
    final UUID URO_SERV = fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    final UUID TARE_CH  = fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");


    final UUID THRESHOLD1 = fromString("b97f5987-dbe5-4bae-af26-ff34cbc32f07");
    final UUID THRESHOLD2 = fromString("c19c5488-bd8e-428e-a665-6f6a5d36f1d7");
    final UUID THRESHOLD3 = fromString("ef85fb2f-6256-4ba7-b7be-3af8fdbecab1");
    final UUID THRESHOLD4 = fromString("a732fcf0-2060-4026-9cac-775d0aade77b");
    final UUID THRESHOLD5 = fromString("e9cd9578-485e-41b9-b439-7bef16382342");


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threshold);

        final Intent intent = getIntent();
        if (intent!=null){
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null());
            mBleWrapper.connect(mDeviceAddress);
            getView();
        }
        this.getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    TextView oldValue1, oldValue2, oldValue3, oldValue4, oldValue5;
    EditText ThresholdNum1,ThresholdNum2,ThresholdNum3,ThresholdNum4,ThresholdNum5;
    Button SaveSetting, CalibrateStart, CalibrateEnd;

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
        oldValue1 = (TextView) findViewById(R.id.oldValueText1);
        oldValue2 = (TextView) findViewById(R.id.oldValueText2);
        oldValue3 = (TextView) findViewById(R.id.oldValueText3);
        oldValue4 = (TextView) findViewById(R.id.oldValueText4);
        oldValue5 = (TextView) findViewById(R.id.oldValueText5);

        //New Value
        ThresholdNum1 = (EditText) findViewById(R.id.ThreshNumInput1);
        ThresholdNum2 = (EditText) findViewById(R.id.ThreshNumInput2);
        ThresholdNum3 = (EditText) findViewById(R.id.ThreshNumInput3);
        ThresholdNum4 = (EditText) findViewById(R.id.ThreshNumInput4);
        ThresholdNum5 = (EditText) findViewById(R.id.ThreshNumInput5);

        SaveSetting = (Button) findViewById(R.id.SaveSettings);
        CalibrateStart = (Button) findViewById(R.id.CalibrateStart);
        CalibrateEnd = (Button) findViewById(R.id.CalibrateEnd);

        if(mBleWrapper.connect(mDeviceAddress)){
            BluetoothGatt gatt = mBleWrapper.getGatt();
            oldValue1.setText(gatt.getService(URO_SERV).getCharacteristic(THRESHOLD1).getIntValue(FORMAT_UINT32,0));
            oldValue2.setText(gatt.getService(URO_SERV).getCharacteristic(THRESHOLD2).getIntValue(FORMAT_UINT32,0));
            oldValue3.setText(gatt.getService(URO_SERV).getCharacteristic(THRESHOLD3).getIntValue(FORMAT_UINT32,0));
            oldValue4.setText(gatt.getService(URO_SERV).getCharacteristic(THRESHOLD4).getIntValue(FORMAT_UINT32,0));
            oldValue5.setText(gatt.getService(URO_SERV).getCharacteristic(THRESHOLD5).getIntValue(FORMAT_UINT32,0));
        }


        //Save threshold values
        SaveSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x40";     //??
                byte[] eepromSetting = parseHexStringToBytes(calibV);

                int newValue1 = Integer.parseInt(ThresholdNum1.getText().toString());
                int newValue2 = Integer.parseInt(ThresholdNum2.getText().toString());
                int newValue3 = Integer.parseInt(ThresholdNum3.getText().toString());
                int newValue4 = Integer.parseInt(ThresholdNum4.getText().toString());
                int newValue5 = Integer.parseInt(ThresholdNum5.getText().toString());
                if (mBleWrapper.connect(mDeviceAddress)) {
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic threashold1 = gatt.getService(URO_SERV).getCharacteristic(THRESHOLD1);
                    BluetoothGattCharacteristic threashold2 = gatt.getService(URO_SERV).getCharacteristic(THRESHOLD2);
                    BluetoothGattCharacteristic threashold3 = gatt.getService(URO_SERV).getCharacteristic(THRESHOLD3);
                    BluetoothGattCharacteristic threashold4 = gatt.getService(URO_SERV).getCharacteristic(THRESHOLD4);
                    BluetoothGattCharacteristic threashold5 = gatt.getService(URO_SERV).getCharacteristic(THRESHOLD5);
                    //write threshold values to device
                    mBleWrapper.writeDataToCharacteristic(threashold1, ByteBuffer.allocate(4).putInt(newValue1).array());
                    mBleWrapper.writeDataToCharacteristic(threashold2, ByteBuffer.allocate(4).putInt(newValue2).array());
                    mBleWrapper.writeDataToCharacteristic(threashold3, ByteBuffer.allocate(4).putInt(newValue3).array());
                    mBleWrapper.writeDataToCharacteristic(threashold4, ByteBuffer.allocate(4).putInt(newValue4).array());
                    mBleWrapper.writeDataToCharacteristic(threashold5, ByteBuffer.allocate(4).putInt(newValue5).array());
                    //write to EEPROM
                    BluetoothGattCharacteristic saveToeeprom = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(saveToeeprom, eepromSetting);
                }
            }
        });

        CalibrateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x24";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.connect(mDeviceAddress)) {
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                }
            }
        });

        CalibrateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x23";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.connect(mDeviceAddress)) {
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                }
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("On Pause","YES");
        if (mBleWrapper.isConnected()){
            mBleWrapper.diconnect();
            mBleWrapper.close();
        }
    };

}

