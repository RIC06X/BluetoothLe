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

import java.util.UUID;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threshold);
        getView();
        final Intent intent = getIntent();
        if (intent!=null){
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null());
            mBleWrapper.connect(mDeviceAddress);
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

        SaveSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x25";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.connect(mDeviceAddress)) {
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                }
            }
        });

        CalibrateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String calibV = "0x25";
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
                String calibV = "0x25";
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
