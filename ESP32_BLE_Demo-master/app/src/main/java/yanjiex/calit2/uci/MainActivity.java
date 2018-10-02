package yanjiex.calit2.uci;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.bluetooth.bledemo.BleNamesResolver;
import org.bluetooth.bledemo.BleWrapper;
import org.bluetooth.bledemo.BleWrapperUiCallbacks;
import org.bluetooth.bledemo.R;
import org.bluetooth.bledemo.ScanningActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.util.UUID.fromString;

public class MainActivity extends Activity implements OnChartValueSelectedListener {
    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";
    public static final double GRAVATY_FACTOR = 0.0098066500286389;
    public static float Threshold1 = 100;
    public static float Threshold2 = 200;
    public static float Threshold3 = 400;
    public static float Threshold4 = 600;
    public static float Threshold5 = 800;
    public static final int REQUEST_CODE = 1111;
    public static boolean shouldChangeChart = false;
    private String mDeviceAddress;
    private BleWrapper mBleWrapper;
    final UUID URO_SERV     =   fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private TextView mDeviceStatus;
    final UUID TARE_CH      =   fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    final UUID FloatData    =   fromString("4a78b8dd-a43d-46cf-9270-f6b750a717c8");
    final UUID BATT_SERV = fromString("65f63692-5d2c-4e49-8143-289371e0cc70");
    final UUID BATT_INFO = fromString("57ee883f-82a4-4969-868d-be01d5018cf3");
    public String mDeviceName;
    float mFloatValue;
    //File processing variables
    Boolean isHeader = true;
    Boolean MeasureStart;
    YAxis leftAxis;
    XAxis xl;
    LimitLine ll1;
    long startTime;
    long LastEpoch;
    //For Delay Method Use
    Handler handler;
    Boolean isWriting = false;
    String filename;
    FileOutputStream outputStream;
    File file;
    File tempfile;

    LineChart mChart;
    //Widget
    private TextView mDeviceAddressView;
    private Button tareBtn;
    private ToggleButton startWriteBtn;
    private TextView batteryinfo;
    private ImageView batteryIcon;
    private ImageView StatusIcon;



    //Create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private TextView TapPrompt;

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
                    batteryIcon.setImageResource(R.drawable.dead);
                    batteryinfo.setText("N/A %");
                    mDeviceAddress = null;
                    setScreencolor(Color.parseColor("#ffffff"));
                    StatusIcon.setImageResource(R.drawable.redcircle);
                    startWriteBtn.setEnabled(false);
                }
                else
                    Toast.makeText(getApplicationContext(), "You have no device connected", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Retrive device info from Scanning activity.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (data != null) {
                    mDeviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
                    mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getViews();
        CreateChart();
        handler = new Handler();
        //To save Power of the BLE device, we set the interval of requesting Battery Percent as 50ms
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBleWrapper.isConnected()) {
                    try {
                        BluetoothGatt gatt = mBleWrapper.getGatt();
                        BluetoothGattService service = gatt.getService(BATT_SERV);
                        BluetoothGattCharacteristic c = service.getCharacteristic(BATT_INFO);
                        mBleWrapper.requestCharacteristicValue(c);
                        Log.d("REQ_BTY", "Request Battery Info");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                handler.postDelayed(this, 50);
            }
        }, 50);

    }

    //--------------Self Defined Functions-------------

    @Override
    protected void onResume() {
        if (shouldChangeChart)
            changeChart();
        super.onResume();
        if (mBleWrapper == null)
            mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
            //-----------------------------------------BLE_WrapperUiCallBack--START---------------------------------------
            String LOGTAG = "BLEWrapperUICallBacks:     ";
            @Override
            public void uiDeviceFound(BluetoothDevice device,
                                      int rssi,
                                      byte[] record) {

            }
            @Override
            public void uiDeviceConnected(BluetoothGatt gatt,
                                          BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceStatus.setText("Device connected");
                        StatusIcon.setImageResource(R.drawable.greencircle);
                        mDeviceAddressView.setText(mDeviceName);
                        Log.d(LOGTAG, "Device Connected");
                        startWriteBtn.setEnabled(true);

                    }
                });
            }
            @Override
            public void uiDeviceDisconnected(BluetoothGatt gatt,
                                             BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceStatus.setText("Disconnected");
                        StatusIcon.setImageResource(R.drawable.redcircle);
                        Log.d(LOGTAG, "Device Disconnected");
                        startWriteBtn.setEnabled(false);
                    }
                });
            }

            @Override
            public void uiAvailableServices(final BluetoothGatt gatt,
                                            final BluetoothDevice device,
                                            final List<BluetoothGattService> services) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d("IN uiAvailableServce","YES");
                        for(BluetoothGattService service : services) {
                            String uuid = service.getUuid().toString().toLowerCase(Locale.getDefault());
                            String name = BleNamesResolver.resolveServiceName(uuid);
                            String type = (service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY) ? "Primary" : "Secondary";
                            if (name == "Urology service" || name == "BATT_SERV") {
                                uiCharacteristicForService(gatt,device,service,service.getCharacteristics());
                            }
                        }
                    }
                });

            }
            @Override
            public void uiCharacteristicForService(final BluetoothGatt gatt,
                                                   final BluetoothDevice device,
                                                   final BluetoothGattService service,
                                                   final List<BluetoothGattCharacteristic> chars) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOGTAG, "uiCharForService");
                        String uuid = service.getUuid().toString().toLowerCase(Locale.getDefault());
                        String name = BleNamesResolver.resolveServiceName(uuid);
                        //Enable BLE notification Function, so the data can be automatically send to the Phone
                        if (name=="Urology service"){
                            for(BluetoothGattCharacteristic ch : chars) {
                                String uuidC = ch.getUuid().toString().toLowerCase(Locale.getDefault());
                                String nameC = BleNamesResolver.resolveCharacteristicName(uuidC);
                                if (nameC == "Weight"){
                                    mBleWrapper.setNotificationForCharacteristic(ch, true);
                                }
                            }
                        } else if (name == "BATT_SERV") {
                            for (BluetoothGattCharacteristic ch : chars) {
                                String uuidC = ch.getUuid().toString().toLowerCase(Locale.getDefault());
                                String nameC = BleNamesResolver.resolveCharacteristicName(uuidC);
                                if (nameC == "BATT_INFO") {
                                    mBleWrapper.setNotificationForCharacteristic(ch, true);
                                }
                            }
                        }
                    }
                });
            }

                @Override
                public void uiCharacteristicsDetails(BluetoothGatt gatt,
                                                     BluetoothDevice device,
                                                     BluetoothGattService service,
                                                     BluetoothGattCharacteristic characteristic) {
                }

                //The BLE data will COME HERE and Wait to be processed
                //There are two Kinds of value: Experiment Data and Battery Percentage
                @Override
                public void uiNewValueForCharacteristic(BluetoothGatt gatt,
                                                        BluetoothDevice device,
                                                        BluetoothGattService service,
                                                        BluetoothGattCharacteristic ch,
                                                        String strValue,
                                                        int intValue,
                                                        byte[] rawValue,
                                                        String timestamp) {
                    Log.d(LOGTAG, "uiNewValueForCharacteristic");
                    //Experiemnt Data Processing
                    if (ch.getUuid().equals(FloatData)) {
                        mFloatValue = resolveByteFloat(rawValue);
                        Log.d(LOGTAG, rawValue.toString() + "  data");
                        Log.d(LOGTAG, "got resolved Exp data: : " + mFloatValue);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTextViewandScreenColor(mFloatValue);
                            }
                        });
                        if (isWriting) {
                            writeTofile("tempData.txt", mFloatValue, LastEpoch);
                            Log.d(LOGTAG, "Write File:  IS WRITING");
                        }
                    }
                    //Battery Percentage Processing
                    else if (ch.getUuid().equals(BATT_INFO)) {
                        Log.d("BatteryInfo:  ", strValue + "%");
                        final int value = Integer.valueOf(strValue);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updataBatteryInfo(value);
                            }
                        });
                    }
                }

                @Override
                public void uiGotNotification(final BluetoothGatt gatt,
                                              final BluetoothDevice device,
                                              final BluetoothGattService service,
                                              final BluetoothGattCharacteristic characteristic) {
                    String ch = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString());
                    Log.d(LOGTAG, "uiGotNotification: " + ch);

                }

                @Override
                public void uiSuccessfulWrite(BluetoothGatt gatt,
                                              BluetoothDevice device,
                                              BluetoothGattService service,
                                              BluetoothGattCharacteristic ch,
                                              final String description) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                        Toast.makeText(getApplicationContext(),
//                                "Writing to " + description + " was finished successfully!", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void uiFailedWrite(BluetoothGatt gatt,
                                          BluetoothDevice device,
                                          BluetoothGattService service,
                                          BluetoothGattCharacteristic ch,
                                          final String description) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                        Toast.makeText(getApplicationContext(),
//                                "Writing to " + description + " FAILED!", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, final int rssi) {
                }
                //-----------------------------------------BLE_WrapperUiCallBack--END---------------------------------------
            });

        if (mBleWrapper.initialize() == false) {
            finish();
        }
        mBleWrapper.connect(mDeviceAddress);
    }

    public void getViews() {
        mDeviceAddressView = findViewById(R.id.Device_Rssi);
        mDeviceStatus = findViewById(R.id.Device_Connection_Info);
        tareBtn = findViewById(R.id.Main_tare);
        startWriteBtn = findViewById(R.id.writeFile);
        startWriteBtn.setEnabled(false);
        TapPrompt = findViewById(R.id.info_textview);
        batteryinfo = findViewById(R.id.battery_percnt);
        batteryIcon = findViewById(R.id.battery_icon);
        StatusIcon = findViewById(R.id.status_icon);

        //Tare: Click Method
        tareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBleWrapper.isConnected()) {
                    Toast.makeText(getApplicationContext(), "Please Hold Button for 1 second", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getApplicationContext(), "Please connect to the device", Toast.LENGTH_SHORT).show();
            }
        });

        //TARE: Press and Hold Click Method
        tareBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mBleWrapper.isConnected()) {
                    String calibV = "0x25";
                    byte[] callibrateData = parseHexStringToBytes(calibV);
                    try {
                        BluetoothGatt gatt = mBleWrapper.getGatt();
                        BluetoothGattCharacteristic c = gatt.getService(URO_SERV).getCharacteristic(TARE_CH);
                        mBleWrapper.writeDataToCharacteristic(c, callibrateData);
                        Toast.makeText(getApplicationContext(), "Tare command send attempt - confirmation tone on the device indicates Tare success", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Tare failed", Toast.LENGTH_SHORT).show();
                    }

                }
                return true;
            }
        });

        startWriteBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Create a Folder in the internal SDCARD, and then create file and initialize the file.
                if (isChecked) {
                    Context context = getApplicationContext();
                    if (FileManager.isExternalStorageReadable() && FileManager.isExternalStorageWritable()) {
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
                            if (isHeader) {
                                outputStream = openFileOutput("tempData.txt", MODE_APPEND);
                                outputStream.write("Experimental Time (Seconds),Absolute Time (Unix Epoch Seconds),Measurement (Newtons)\n".getBytes());
                                outputStream.flush();
                                isHeader = false;
                                isWriting = true;
                                MeasureStart = true;
                                outputStream.close();
                                Toast.makeText(getApplicationContext(), "Starting writing to " + filename, Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {

                        Toast.makeText(getApplicationContext(), "Failed to create new file, please check external storage space", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    isWriting = false;
                    isHeader = true;
                    startTime = 0;
                    try {
                        FileManager.copy(tempfile, file);
                        tempfile.delete();
                        Toast.makeText(getApplicationContext(), "File saved", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    //used for processing sending strings
    public byte[] parseHexStringToBytes(final String hex) {
        String tmp = hex.substring(2).replaceAll("[^[0-9][a-f]]", "");
        byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally

        String part = "";

        for (int i = 0; i < bytes.length; ++i) {
            part = "0x" + tmp.substring(i * 2, i * 2 + 2);
            bytes[i] = Long.decode(part).byteValue();
        }

        return bytes;
    }

    //Resolve little endian coded floating number
    public float resolveByteFloat(byte[] mRawValue) {
        float mFloatValue = 0;
        String temp1;
        String temp2;
        String temp3;
        String temp0;
        String sbits;

        //mRawValue is the Hex String,  mFloatValue is the float transfered from BLE;
        if (mRawValue != null && mRawValue.length > 0) {
            //Convert rawData to binary strings
            temp0 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[3])).replaceAll(".*(.{8})$", "$1");
            temp1 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[2])).replaceAll(".*(.{8})$", "$1");
            temp2 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[1])).replaceAll(".*(.{8})$", "$1");
            temp3 = ("0000000" + Integer.toBinaryString(0xFF & mRawValue[0])).replaceAll(".*(.{8})$", "$1");
            sbits = temp0 + temp1 + temp2 + temp3;

            //Check the positive float and negative float
            if (sbits.charAt(0) == '0') {
                int bits = Integer.valueOf(sbits, 2);
                mFloatValue = Float.intBitsToFloat(bits);
            } else {
                sbits = sbits.substring(1);
                int bits = Integer.valueOf(sbits, 2);
                mFloatValue = -Float.intBitsToFloat(bits);
            }
        }
        return mFloatValue;
    }

    //Set the Data TextView on the MainActivity
    private void setTextViewandScreenColor(float mFloatValue) {
        if (mFloatValue > Threshold5 || mFloatValue < 0) {
            mDeviceAddressView.setText("Warning! Force out of bound: " + String.format("%.2f", mFloatValue) + " g");
        } else
            mDeviceAddressView.setText(String.format("%.2f", mFloatValue * GRAVATY_FACTOR) + " Newton  " + "(" + String.format("%.2f", mFloatValue) + " grams-Force)");
        addEntry(mFloatValue);
        if (mFloatValue > Threshold5) {
            setScreencolor(Color.parseColor("#b61827"));
        } else if (mFloatValue > Threshold4 && mFloatValue <= Threshold5) {
            setScreencolor(Color.parseColor("#ef5350"));
        } else if (mFloatValue > Threshold3 && mFloatValue <= Threshold4) {
            setScreencolor(Color.parseColor("#ffca28"));
        } else if (mFloatValue < Threshold3 && mFloatValue >= 3) {
            setScreencolor(Color.parseColor("#aed581"));
        } else if (mFloatValue < 3 && mFloatValue >= 0) {
            setScreencolor(Color.parseColor("#ffffff"));
        }
    }

    private void setScreencolor(int color) {
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(color);
    }

    //Write Experiment Time,  Unix Epoch Time, Experiment data(Newton) to File
    protected void writeTofile(String filename, float data, long LastEpoch) {
        try{
            FileOutputStream outputStream = openFileOutput(filename, MODE_APPEND);
            long epoch = System.currentTimeMillis()/1000;
            long retamp = System.currentTimeMillis() % 1000;
            double reminder = retamp * 0.001;
            if (MeasureStart){
                startTime = epoch;
                MeasureStart = false;
            }
            if (LastEpoch != epoch){
                double temp = data * 0.0098066500286389;
                String measureMent = String.format("%.2f", temp);
                String expTime = String.format("%.2f", (epoch + reminder) - startTime);
                String inputline = expTime + "," + Long.toString(epoch) + "," + measureMent + "\n";
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

    //Update the Battery percentage and the Icon on the MainActivity
    public void updataBatteryInfo(int batt_data) {

        if (batt_data > 100 || batt_data < 0) {
            batteryIcon.setImageResource(R.drawable.dead);
            batteryinfo.setText("Sensor Communication Error");
        } else if (batt_data >= 50) {
            batteryIcon.setImageResource(R.drawable.full);
            batteryinfo.setText(Integer.toString(batt_data) + "%");
        } else if (batt_data >= 30 && batt_data < 50) {
            batteryIcon.setImageResource(R.drawable.half);
            batteryinfo.setText(Integer.toString(batt_data) + "%");
        } else if (batt_data >= 10 && batt_data < 30) {
            batteryIcon.setImageResource(R.drawable.low);
            batteryinfo.setText(Integer.toString(batt_data) + "%");
        } else if (batt_data >= 0 && batt_data < 10) {
            batteryIcon.setImageResource(R.drawable.dead);
            batteryinfo.setText(Integer.toString(batt_data) + "%");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("On Pause","YES");
    }

    //-------------------The Code Below is used in the MPAndroidChart Graphing Adapter--------------------------------
    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(4f);
        set.setCircleRadius(6f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(15f);
        set.setDrawValues(false);
        return set;
    }

    private void CreateChart() {
        mChart = findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(this);

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);

        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
        mv.setChartView(mChart); // For bounds control
        mChart.setMarker(mv); // Set the marker to the chart

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        //l.setTypeface(mTfLight);
        l.setTextColor(Color.WHITE);

        xl = mChart.getXAxis();
        //xl.setTypeface(mTfLight);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        ll1 = new LimitLine(Threshold5, "Upper Limit");
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(10f);

        leftAxis = mChart.getAxisLeft();
        //leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setTextSize(20f);
        xl.setTextSize(20);
        leftAxis.setAxisMaximum(Threshold5 + 200);
        leftAxis.addLimitLine(ll1);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
        mChart.getDescription().setEnabled(false);
    }
    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {

    }

    private void addEntry(float number) {
        LineData data = mChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), number), 0);
            data.notifyDataChanged();
            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();
            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(30);
            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());
            // this automatically refreshes the chart (calls invalidate())
        }
    }

    public void changeChart() {
        leftAxis.removeAllLimitLines();
        mChart.invalidate();
        mChart.clear();
        shouldChangeChart = false;
        CreateChart();
    }
}
