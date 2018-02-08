package yanjiex.calit2.uci;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.bluetooth.bledemo.BleNamesResolver;
import org.bluetooth.bledemo.BleWrapper;
import org.bluetooth.bledemo.BleWrapperUiCallbacks;
import org.bluetooth.bledemo.CharacteristicDetailsAdapter;
import org.bluetooth.bledemo.CharacteristicsListAdapter;
import org.bluetooth.bledemo.PeripheralActivity;
import org.bluetooth.bledemo.R;
import org.bluetooth.bledemo.ScanningActivity;
import org.bluetooth.bledemo.ServicesListAdapter;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements BleWrapperUiCallbacks{
    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceRSSI;

    private BleWrapper mBleWrapper;

    private TextView mDeviceNameView;
    private TextView mDeviceAddressView;
    private TextView mDeviceRssiView;
    private TextView mDeviceStatus;
    private View mListViewHeader;
    private TextView mHeaderTitle;
    private TextView mHeaderBackButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (mBleWrapper.isConnected()) {
            menu.findItem(R.id.main_pairing).setVisible(false);
            menu.findItem(R.id.main_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.main_pairing).setVisible(true);
            menu.findItem(R.id.main_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.main_pairing:
                Intent intent = new Intent(this, ScanningActivity.class);
                startActivity(intent);
                return true;
            case R.id.device_disconnect:
                mBleWrapper.diconnect();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getViews(){
//        mDeviceNameView = (TextView) findViewById(R.id.peripheral_name);
        mDeviceAddressView = (TextView) findViewById(R.id.Device_Rssi);
//        mDeviceRssiView = (TextView) findViewById(R.id.peripheral_rssi);
        mDeviceStatus = (TextView) findViewById(R.id.Device_Info);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getViews();
        final Intent intent = getIntent();
        if (intent!=null){
                mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
                mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                mDeviceRSSI = intent.getIntExtra(EXTRAS_DEVICE_RSSI, 0) + " db";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBleWrapper == null) mBleWrapper = new BleWrapper(this, this);

        if(mBleWrapper.initialize() == false) {
            finish();
        }
        mDeviceStatus.setText("connecting ...");
        mBleWrapper.connect(mDeviceAddress);

    }


    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {

    }

    public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceStatus.setText("connected\n");
                mDeviceAddressView.setText(mDeviceName);
            }
        });
    }

    public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceStatus.setText("disconnected\n");

            }
        });
    }

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
                        mDeviceStatus.append(name+"\n");
                        uiCharacteristicForService(gatt,device,service,service.getCharacteristics());
                    }
                }
            }
        });

    }

    public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, final BluetoothGattService service, final List<BluetoothGattCharacteristic> chars) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("uiCharForService","YES");
                String uuid = service.getUuid().toString().toLowerCase(Locale.getDefault());
                String name = BleNamesResolver.resolveServiceName(uuid);
                if (name=="Urology service"){
                    for(BluetoothGattCharacteristic ch : chars) {
                        String uuidC = ch.getUuid().toString().toLowerCase(Locale.getDefault());
                        String nameC = BleNamesResolver.resolveCharacteristicName(uuid);
                        mDeviceStatus.append(nameC+"\n");
                    }
                }}
        });
    }


    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {

    }


    public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, final String description) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Writing to " + description + " was finished successfully!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Writing to " + description + " FAILED!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, final int rssi) {

    }
}
