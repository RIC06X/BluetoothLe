package yanjiex.calit2.uci;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.robinhood.spark.SparkAdapter;
import com.robinhood.spark.SparkView;

import org.bluetooth.bledemo.BleNamesResolver;
import org.bluetooth.bledemo.BleWrapper;
import org.bluetooth.bledemo.BleWrapperUiCallbacks;
import org.bluetooth.bledemo.R;
import org.bluetooth.bledemo.ScanningActivity;
import org.w3c.dom.ls.LSException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static java.util.UUID.fromString;

public class MainActivity extends Activity {
    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";
    public static final int REQUEST_CODE = 1111;

    private String mDeviceName;
    private String mDeviceAddress;

    private BleWrapper mBleWrapper;

    private TextView mDeviceAddressView;
    private TextView mDeviceStatus;
    private SparkView sparkView;

    Button tareBtn;
    Button zeroCalibrateBtn;
    Button hundredCalibrateBtn;
    ToggleButton startWriteBtn;

    private sensorUpdateAdpter sparkAdapter;
    private TextView scrubInfoTextView;

    final UUID URO_SERV = fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    final UUID TARE_CH  = fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    Boolean isHeader = true;
    Boolean isWriting = false;
    boolean MeasureStart;
    String filename;

    FileOutputStream outputStream;
    File file;
    File tempfile;


    public void getViews(){
        mDeviceAddressView = (TextView) findViewById(R.id.Device_Rssi);
        mDeviceStatus = (TextView) findViewById(R.id.Device_Info);
        tareBtn = (Button) findViewById(R.id.Main_tare);
        //not useful
        zeroCalibrateBtn = (Button) findViewById(R.id.Main_SetZero);
        hundredCalibrateBtn = (Button) findViewById(R.id.Main_SetHund);
        //not useful

        startWriteBtn = (ToggleButton) findViewById(R.id.writeFile);

        scrubInfoTextView = (TextView) findViewById(R.id.info_textview);
        sparkView = (SparkView) findViewById(R.id.sparkview);

        tareBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String calibV = "0x25";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.isConnected()){
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                }

            }
        });


        startWriteBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    Context context = getApplicationContext();
                    if (isExternalStorageReadable() && isExternalStorageWritable()){
                        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Urology_Data";
                        File dir = new File(path);
                        dir.mkdirs();
                        filename = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".csv";
                        file = new File(path + "/" + filename);

                        tempfile = new File(context.getFilesDir(), "tempData.txt");
                        try {
                            file.createNewFile();
                            file.setWritable(true);
                            tempfile.createNewFile();
                            tempfile.setWritable(true);
                            if (isHeader){
                                outputStream = openFileOutput("tempData.txt", MODE_APPEND);
                                outputStream.write("Experimental Time (Seconds),Absolute Time (Unix Epoch Seconds),Measurement (Newtons)\n".getBytes());
                                outputStream.flush();
                                isHeader = false;
                                isWriting = true;
                                MeasureStart = true;
                                outputStream.close();
                                Toast.makeText(getApplicationContext(),"Starting writing to "+filename, Toast.LENGTH_LONG).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    else{

                        Toast.makeText(getApplicationContext(),"Failed to create new file, please check external storage space", Toast.LENGTH_LONG).show();
                    }

                }
                else{
                    isWriting = false;
                    isHeader = true;
                    startTime = 0;
                    try{
                        copy(tempfile,file);
                        tempfile.delete();
                        Toast.makeText(getApplicationContext(),"File saved", Toast.LENGTH_LONG).show();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        zeroCalibrateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String calibV = "0x24";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.isConnected()){
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                }
            }
        });

        hundredCalibrateBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String calibV = "0x23";
                byte[] callibrateData = parseHexStringToBytes(calibV);
                if (mBleWrapper.isConnected()){
                    BluetoothGatt gatt = mBleWrapper.getGatt();
                    BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                    mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                }
            }
        });
    }
    //Create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    //Menu opetion select
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.main_pairing:
                Intent intent = new Intent(this, ScanningActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                return true;
            case R.id.main_setting:
                if (mBleWrapper.isConnected()){
                    Intent intent2 = new Intent(this, Settings.class);
                    intent2.putExtra(EXTRAS_DEVICE_NAME, mDeviceName);
                    intent2.putExtra(EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                    startActivity(intent2);
                }
                else
                    Toast.makeText(getApplicationContext(), "You have no device connected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.main_disconnect:
                Log.d("On Menu disconnect","");
                if (mBleWrapper.isConnected()){
                    Log.d("On Menu disconnect","true");
                    mBleWrapper.diconnect();
                    mBleWrapper.close();
                    mDeviceStatus.setText("Disconnected");
                }
                else
                    Toast.makeText(getApplicationContext(), "You have no device connected", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
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



    // Set views and get views
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getViews();
        //Set the graphing adpater
        sparkAdapter = new sensorUpdateAdpter();
        sparkView.setAdapter(sparkAdapter);
        setScrubHandler(sparkView);

    }


    // Retrive device info from Scanning activity.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
         case REQUEST_CODE:
            if (data!=null){
                mDeviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
                mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            }
        }
    }
    float mFloatValue;   //TODO VERY IMPORTANT DATA INPUT!!
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
                        mDeviceStatus.setText("connected\n");
                        mDeviceAddressView.setText(mDeviceName);
                        Log.d("Device Connected","YES");
                    }
                });
            }
            @Override
            public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceStatus.setText("Disconnected\n");
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
                if (mFloatValue > 1000){
                    setScreencolor(Color.parseColor("#ef5350"));

                }
                if (mFloatValue > 800){
                    setScreencolor(Color.parseColor("#ffca28"));
                }
                Log.d(LOGTAG, "uiCharacteristicsDetails: " + mFloatValue);

            }

            //CRUCIAL IMPORTANT

            @Override
            public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
                Log.d("BLE WRAPPER", "got new Value");
                mFloatValue= resolveByteFloat(rawValue);
                Log.d(LOGTAG, "uiCharacteristicsDetails: " + mFloatValue);
                if (isWriting){
                    writeTofile("tempData.txt", mFloatValue);
                    Log.d("WRITE__FILE", "IS WRITING");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFloatValue>1000 || mFloatValue < 0){
                            mDeviceAddressView.setText("Warning!! weight out of bound: "+ String.format("%f",mFloatValue)+" g");
                        }
                        else
                            mDeviceAddressView.setText(String.format("%.2f",mFloatValue)+" g     "+
                                                       String.format("%.2f",mFloatValue*0.0098066500286389)+" Newton");
                        //sparkAdapter.sensorUpdate(mFloatValue);
                        sparkAdapter.updateInfo(mFloatValue);
                        if (mFloatValue > 1000){
                            setScreencolor(Color.parseColor("#b61827"));
                        }
                        else if (mFloatValue > 800 && mFloatValue <= 1000){
                            setScreencolor(Color.parseColor("#ef5350"));
                        }
                        else if (mFloatValue > 500 && mFloatValue <= 800){
                            setScreencolor(Color.parseColor("#ffca28"));
                        }
                        else if (mFloatValue < 500 && mFloatValue >= 3){
                            setScreencolor(Color.parseColor("#aed581"));
                        }
                        else if (mFloatValue < 3 && mFloatValue >= 0){
                            setScreencolor(Color.parseColor("#ffffff"));
                        }


                    }
                });
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
                        Toast.makeText(getApplicationContext(), "Writing to " + description + " was finished successfully!", Toast.LENGTH_LONG).show();
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
        //mDeviceStatus.setText("Disconnected");
        mBleWrapper.connect(mDeviceAddress);
    }


    long startTime;
    long LastEpoch;
    protected void writeTofile(String filename,  float data){
        try{
            outputStream = openFileOutput(filename, MODE_APPEND);
            long epoch = System.currentTimeMillis()/1000;
            if (MeasureStart){
                startTime = epoch;
                MeasureStart = false;
            }
            if (LastEpoch != epoch){
                String inputline = Long.toString(epoch-startTime)+","+Long.toString(epoch)+","+Float.toString(data)+"\n";
                outputStream.write(inputline.getBytes());
                outputStream.close();
                Log.d("IN_Writing",filename+" "+ Float.toString(data));
                LastEpoch = epoch;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("On Pause","YES");
//        if (mBleWrapper.isConnected()){
//            mBleWrapper.diconnect();
//            mBleWrapper.close();
//        }
    };
//---------------------------------the code below is the graph view adpter---------------------------

    private void setScreencolor(int color){
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(color);
    }
    private void setScrubHandler(SparkView sparkView) {

        sparkView.setScrubListener(new SparkView.OnScrubListener() {
            @Override
            public void onScrubbed(Object value) {
                if (value == null) {
                    scrubInfoTextView.setText("Tap and hold to see the value ");
                } else {
                    scrubInfoTextView.setText(getString(R.string.scrub_format, value));
                }
            }
        });
    }


    public static class sensorUpdateAdpter extends SparkAdapter{
        private float[] yData;

        public sensorUpdateAdpter() {
            yData = new float[50];
            notifyDataSetChanged();
        }

        public void updateInfo(float number) {
            int count = yData.length;
            for (int i = 0; i < count-1; i++) {
                yData[i] = yData[i + 1];
            }
            yData[count -1] = number;
            notifyDataSetChanged();
        }

        @Override
        public boolean hasBaseLine() {
            return true;
        }

        @Override
        public float getBaseLine() {
            return 0;
        }

        @Override
        public int getCount() {
            return yData.length;
        }

        @Override
        public Object getItem(int index) {
            return yData[index];
        }

        @Override
        public float getY(int index) {
            return yData[index];
        }
    }
}
