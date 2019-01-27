package com.example.honker.wristbandattack;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import android.os.Handler;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int UPDATE_VIEW=1;
    private ProgressDialog pd;
    public static int flag=3;
    public static char test;
    private static int count1=1,count2=1;
    private Button scan, stepNum, batteryLev, change12, modifyTime, change24, breakActivity;
    private TextView  cnt_state , dev_info;
    private ListView list;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case UPDATE_VIEW:
                    list.setAdapter(new MyAdapter(MainActivity.this , deviceList));
                    break;
                default:
                    break;
            }
            //pd.dismiss();// 关闭ProgressDialog
        }
    };
    private int opcode=10;
    public static String TAG = "shouhuan-MainActivity";
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    List<BluetoothDevice> deviceList = new ArrayList<>();
    List<String> serviceslist = new ArrayList<String>();
    BluetoothDevice bluetoothDevice;
    BluetoothGattService bluetoothGattServices;
    BluetoothGattCharacteristic characteristic_jb , characteristic_dl , characteristic_pi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        //蓝牙管理，这是系统服务可以通过getSystemService(BLUETOOTH_SERVICE)的方法获取实例
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        //通过蓝牙管理实例获取适配器，然后通过扫描方法（scan）获取设备(device)
        bluetoothAdapter = bluetoothManager.getAdapter();

    }

    private void initView() {
        scan = (Button) findViewById(R.id.scanner);
        stepNum = (Button) findViewById(R.id.step_number);
        batteryLev = (Button) findViewById(R.id.battery_level);
        breakActivity = (Button) findViewById(R.id.breakActivity);
        list = (ListView) findViewById(R.id.list);

        dev_info = (TextView) findViewById(R.id.device_information);
        cnt_state = (TextView) findViewById(R.id.connection_state);

        scan.setOnClickListener(this);
        stepNum.setOnClickListener(this);
        batteryLev.setOnClickListener(this);
        breakActivity.setOnClickListener(this);

        //乐康

        change12 = (Button) findViewById(R.id.change12);
        change24=(Button) findViewById(R.id.change24);
        modifyTime = (Button) findViewById(R.id.modifyTime);

        change12.setOnClickListener(this);
        change24.setOnClickListener(this);
        modifyTime.setOnClickListener(this);

        //item 监听事件
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothDevice = deviceList.get(i);
                //连接设备的方法,返回值为bluetoothgatt类型
                bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);
                cnt_state.setText("Connecting " + bluetoothDevice.getName() + "...");
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.scanner:
                //开始扫描前开启蓝牙
                Intent turn_on = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turn_on, 0);
                Toast.makeText(MainActivity.this, "Turn On Bluetooth", Toast.LENGTH_SHORT).show();

                Thread scanThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "run: saomiao ...");
                        scan();
                    }
                });
                scanThread.start();
                break;

            case R.id.step_number:
                flag=1;
                opcode=3;
                bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);
                break;

            case R.id.battery_level:
                flag=2;
                opcode=4;

                bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);

                break;
            case R.id.list:

                break;

            //以下为乐康手环代码

            case R.id.change24:
                opcode=0;
                flag=4;
                bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);
                if(test != 'L'){
                    if(opcode==0){
                        setDialog(7,"","","");
                    }
                }else{
                    setDialog(9,"","","");
                }
                break;

            case R.id.change12:
                opcode=1;
                flag=4;
                bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);
                if(test != 'L'){
                    if(opcode==1){
                        setDialog(8,"","","");
                    }
                }else {
                    setDialog(10,"","","");
                }

                break;
            case R.id.modifyTime:
                opcode=2;
                flag=4;
                bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);

                if(test != 'L'){
                    if(opcode==2){
                        setDialog(6,"","","");
                    }
                }

                break;
            case R.id.breakActivity:
                onDestroy();
                reStartActivity();
                break;
        }
    }

    public void scan(){
        deviceList.clear();
        bluetoothAdapter.startLeScan(callback);
    }

    //扫描回调
    public BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            Log.i("TAG", "onLeScan: " + bluetoothDevice.getName() + "/t" + bluetoothDevice.getAddress() + "/t" + bluetoothDevice.getBondState());

            //重复过滤方法，列表中包含不该设备才加入列表中，并刷新列表
            if (!deviceList.contains(bluetoothDevice)) {
                //将设备加入列表数据中
                deviceList.add(bluetoothDevice);

                Message message=new Message();
                message.what=UPDATE_VIEW;
                handler.sendMessage(message);

            }

        }
    };

    private BluetoothGattCallback gattcallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String status;
                    switch (newState) {
                        //已经连接
                        case BluetoothGatt.STATE_CONNECTED:
                            cnt_state.setText("Open");

                            //该方法用于获取设备的服务，寻找服务
                            bluetoothGatt.discoverServices();
                            break;
                        //正在连接
                        case BluetoothGatt.STATE_CONNECTING:
                            cnt_state.setText("Connecting");
                            break;
                        //连接断开
                        case BluetoothGatt.STATE_DISCONNECTED:
                            cnt_state.setText("OFF");
                            break;
                        //正在断开
                        case BluetoothGatt.STATE_DISCONNECTING:
                            cnt_state.setText("Breaking");
                            break;
                    }
                    dev_info.setText("Device Information：" + bluetoothDevice.getName());
                    //pd.dismiss();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == bluetoothGatt.GATT_SUCCESS) {
                final List<BluetoothGattService> services = bluetoothGatt.getServices();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //List<String> serlist = new ArrayList<>();
                        for (final BluetoothGattService bluetoothGattService : services) {
                            bluetoothGattServices = bluetoothGattService;

                            Log.i(TAG, "onServicesDiscovered: " + bluetoothGattService.getUuid());

                            List<BluetoothGattCharacteristic> charc = bluetoothGattService.getCharacteristics();
                            for (BluetoothGattCharacteristic charac : charc) {
                                Log.i(TAG, "run: " + charac.getUuid());
                                //第一个条件语句，为乐康手环代码
                                if (charac.getUuid().toString().equals("00008001-0000-1000-8000-00805f9b34fb")) {
                                    //设备 震动特征值
                                    try{
                                        Thread.sleep(150);
                                        BluetoothGattCharacteristic bjxiejun=bluetoothGatt.getService(UUID.fromString("00006006-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("00008002-0000-1000-8000-00805f9b34fb"));
                                        boolean ss = bluetoothGatt.setCharacteristicNotification(bjxiejun, true);
                                        List<BluetoothGattDescriptor>descriptors=bjxiejun.getDescriptors();
                                        for(BluetoothGattDescriptor dp:descriptors){
                                            dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                            bluetoothGatt.writeDescriptor(dp);
                                        }

                                    }
                                    catch (InterruptedException paramAnonymousBluetoothGatt){
                                        paramAnonymousBluetoothGatt.printStackTrace();

                                    }
                                    try{
                                        /////12小时制转换成24小时制
                                        Thread.sleep(200);
                                        if(opcode==0) {

                                            byte j = (byte) 0;
                                            //if (ATimeType == 12) {
                                            //j = (byte) 1;
                                            //}
                                            byte i = j;
                                            //if ("0".equals(str)) {
                                            i = (byte) (j | 0x2);
                                            //}
                                            j = i;
                                            ///if (this.daymonth) {
                                            j = (byte) (i | 0x4);
                                            //}
                                            byte[] arrayOfByte = new byte[5];
                                            arrayOfByte[0] = 110;
                                            arrayOfByte[1] = 1;
                                            arrayOfByte[2] = 52;
                                            arrayOfByte[3] = j;
                                            arrayOfByte[4] = -113;
                                            write(arrayOfByte, charac, bluetoothGatt);
                                        }
                                        ////////24小时转换成12小时
                                        Thread.sleep(200);
                                        if(opcode==1) {

                                            byte j = (byte) 0;
                                            //if (ATimeType == 12) {
                                            j = (byte) 1;
                                            //}
                                            byte i = j;
                                            //if ("0".equals(str)) {
                                            i = (byte) (j | 0x2);
                                            //}
                                            j = i;
                                            ///if (this.daymonth) {
                                            j = (byte) (i | 0x4);
                                            //}
                                            byte[] arrayOfByte = new byte[5];
                                            arrayOfByte[0] = 110;
                                            arrayOfByte[1] = 1;
                                            arrayOfByte[2] = 52;
                                            arrayOfByte[3] = j;
                                            arrayOfByte[4] = -113;
                                            write(arrayOfByte, charac, bluetoothGatt);
                                        }
                                        ///////修改手环时间
                                        Thread.sleep(200);
                                        if(opcode==2){
                                            write(getDateValue(),charac,bluetoothGatt);
                                            setDialog(5,"","","");
                                        }

                                        //////读取电池电量信息
                                        Thread.sleep(200);
                                        if(opcode==4){
                                            byte[] arrayOfByte = new byte[5];
                                            byte[] tmp119_118 = arrayOfByte;
                                            tmp119_118[0] = 110;
                                            byte[] tmp125_119 = tmp119_118;
                                            tmp125_119[1] = 1;
                                            byte[] tmp131_125 = tmp125_119;
                                            tmp131_125[2] = 15;
                                            byte[] tmp137_131 = tmp131_125;
                                            tmp137_131[3] = 1;
                                            byte[] tmp143_137 = tmp137_131;
                                            tmp143_137[4] = -113;
                                            write(tmp143_137,charac,bluetoothGatt);
                                        }
                                        //////读取运动信息
                                        Thread.sleep(200);
                                        if(opcode==3){
                                            byte[] arrayOfByte=new byte[5];
                                            byte[] tmp158_157 = arrayOfByte;
                                            tmp158_157[0] = 110;
                                            byte[] tmp164_158 = tmp158_157;
                                            tmp164_158[1] = 1;
                                            byte[] tmp170_164 = tmp164_158;
                                            tmp170_164[2] = 27;
                                            byte[] tmp176_170 = tmp170_164;
                                            tmp176_170[3] = 1;
                                            byte[] tmp182_176 = tmp176_170;
                                            tmp182_176[4] = -113;
                                            write(tmp182_176,charac,bluetoothGatt);
                                        }
                                    }
                                    catch (InterruptedException paramAnonymousBluetoothGatt){
                                        paramAnonymousBluetoothGatt.printStackTrace();
                                    }
                                    break;

                                } else if (flag==1){if (charac.getUuid().toString().equals("0000ff06-0000-1000-8000-00805f9b34fb")) {
                                    //设备 步数
                                    characteristic_jb = charac;

                                    bluetoothGatt.readCharacteristic(characteristic_jb);

                                    Log.i(TAG, "run: 正在尝试读取步数");}
                                } else if(flag==2) {if (charac.getUuid().toString().equals("0000ff0c-0000-1000-8000-00805f9b34fb")) {
                                    //设备 电量特征值
                                    characteristic_dl = charac;

                                    bluetoothGatt.readCharacteristic(characteristic_dl);

                                }}else if(true){
                                    if(charac.getUuid().toString().equals("00002a00-0000-1000-8000-00805f9b34fb")){
                                        characteristic_pi = charac;

                                        bluetoothGatt.readCharacteristic(characteristic_pi);
                                    }
                                }

                            }

                            serviceslist.add(bluetoothGattService.getUuid().toString());

                        }
                    }
                });
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == bluetoothGatt.GATT_SUCCESS) {
                final byte[] arraybyte=characteristic.getValue();
                final int batterylevel = characteristic.getValue()[0];

                MainActivity.this. runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (flag == 1) {
                            int stepnumber = ( arraybyte[0] & 0xFF | ( arraybyte[1] & 0xFF) << 8 );
                            final double distance1 = 0.81 * stepnumber;
                            final double energy1 = 52.3 * distance1 * 1.036 / 1000 / 2;
                            final String distance;

                            double distance3 = setTranslation(distance1,2);
                            double energy3 = setTranslation(energy1,1);

                            if(distance3>=1000){
                                double distance4 = distance3/1000;
                                double distance5 = setTranslation(distance4,1);
                                distance=String.valueOf(distance5)+"km";
                            }else{
                                distance=String.valueOf(distance3)+"m";
                            }
                            setDialog(flag,String.valueOf(stepnumber),String.valueOf(distance),String.valueOf(energy3));
                        }
                        if (flag == 2) {
                            int T1 = arraybyte[1];
                            int T2 = arraybyte[2];
                            int T3 = arraybyte[3];
                            int T4 = arraybyte[4];
                            int T5 = arraybyte[5];
                            String s1="Remaining battery：" + batterylevel + "%";
                            String s2="Last charge：" + T2 + "/" + T3 + "/"+ "20" + T1 + " "  + T4 + ":"+ T5;
                            setDialog(flag, s1 , s2 , "");
                        }
                        if (flag==3){
                            char C0 = (char)arraybyte[0];
                            char C1 = (char)arraybyte[1];
                            /*
                            if(C0 != 'M'){
                                char C2=(char)arraybyte[2];
                                char C3=(char)arraybyte[3];
                                char C4=(char)arraybyte[4];
                                char C5=(char)arraybyte[5];
                                char C6=(char)arraybyte[6];
                                char C7=(char)arraybyte[7];
                                char C8=(char)arraybyte[8];
                                char C9=(char)arraybyte[9];
                                dev_info.setText("设备信息：" + C0 + C1 + C2 + C3 + C4 + C5 + C6 + C7 + C8 + C9 );
                            }else {
                                dev_info.setText("设备信息：" + C0 + C1 );
                            }
                            */
                            test=C0;

                        }
                    }
                });

                Log.e(TAG, "onCharacteristicRead: " + characteristic.getValue()[0]);

            }
        }

        @Override
        //已修改为乐康代码
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == bluetoothGatt.GATT_SUCCESS) {
                byte[] result=characteristic.getValue();
                System.out.print("hello");
            }

        }


        @Override
        //已修改为乐康代码
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (bluetoothGatt == null) {
                return;
            }
            try{
                Thread.sleep(200);
                final byte[] arrayOfByte = characteristic.getValue();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(opcode==3){
                            int sum=byteReverseToInt(new byte[] { arrayOfByte[11], arrayOfByte[12], arrayOfByte[13], arrayOfByte[14] });
                            int energy=byteReverseToInt(new byte[] { arrayOfByte[7], arrayOfByte[8], arrayOfByte[9], arrayOfByte[10] });
                            double distance1=sum * 70.550003f/160934.4f;
                            double distance=setTranslation(distance1,2);
                            if(count1==1){
                                setDialog(opcode,String.valueOf(sum),String.valueOf(distance),String.valueOf(energy));
                                count1++;
                            }
                        }
                        if(opcode==4){
                            int barteryofValue=arrayOfByte[3]*5;
                            if(barteryofValue>100){
                                barteryofValue=100;
                            }
                            int count=1;
                            if(count2==1){
                                setDialog(opcode,String.valueOf(barteryofValue),"","");
                                count2++;
                            }
                        }
                    }
                });
            }
            catch (InterruptedException paramAnonymousBluetoothGatt){
                paramAnonymousBluetoothGatt.printStackTrace();

            }

            Log.i("BluetoothLeService", "-->>BLE onCharacteristicChanged------Has Changed");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        //已修改为乐康代码
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            byte[] reslut=descriptor.getCharacteristic().getValue();
            byte[] secresult=descriptor.getValue();
            System.out.println("hello");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    } ;
    private void write(byte[] commond,BluetoothGattCharacteristic bleGattCharacteristic,BluetoothGatt bluetoothGatt) {

        //Log.i(TAG, "write: in write \t " + bluetoothGatt + "\t" + bleGattCharacteristic);


        if (bleGattCharacteristic != null && bluetoothGatt != null) {
            bleGattCharacteristic.setValue(commond);

            bluetoothGatt.writeCharacteristic(bleGattCharacteristic);
            commond = null;
        }
    }

    //以下为乐康手环相关方法
    private byte[] getDateValue() {
        Calendar localCalendar = Calendar.getInstance();
        int i = localCalendar.get(1);
        return new byte[]{110, 1, 21, (byte) i, (byte) (i >> 8), (byte) (localCalendar.get(2) + 1), (byte) localCalendar.get(5), (byte) localCalendar.get(11), (byte) localCalendar.get(12), (byte) localCalendar.get(13), -113};

    }
    private int byteReverseToInt(byte[] paramArrayOfByte)
    {
        int j = 0;
        int i = paramArrayOfByte.length - 1;
        for (;;)
        {
            if (i <= -1) {
                return j;
            }
            j = j << 8 | paramArrayOfByte[i] & 0xFF;
            i -= 1;
        }
    }
    public void getgatt(){
        if(bluetoothGatt!=null){
            bluetoothGatt.disconnect();
            bluetoothGatt.close();;
        }
    }
    public void setProgressDialog(){
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("");
        progressDialog.setMessage("Loading……");
        progressDialog.setCancelable(true);
        progressDialog.show();
        progressDialog.dismiss();
    }
    public void setDialog(int a, String b , String c , String d){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        if(a==1){
            dialog.setTitle("Motion Data-Mi Fit");
            dialog.setMessage("Steps: " + b + " " + "\n" + "Distance: " + c + "\n" + "Burned: " + d + "Cal");
        }
        if(a==2){
            dialog.setTitle("Battery Level-Mi Fit");
            dialog.setMessage(b+"\n"+c);
        }
        if(a==3){
            dialog.setTitle("Motion Data");
            dialog.setMessage("Steps：" + b + "" + "\n" + "Distance：" + c +"mile"+ "\n" + "Calories：" + d + "Cal");
        }
        if(a==4){
            dialog.setTitle("Battery Power");
            dialog.setMessage("Remaining battery："+b+"%");
        }
        if(a==5){
            dialog.setTitle("Time Modification");
            dialog.setMessage("Modify time successfully！");
        }
        if(a==6){
            dialog.setTitle("Time Modification");
            dialog.setMessage("Sorry，current smartband does not support editing！");
        }
        if(a==7){
            dialog.setTitle("Time format modification");
            dialog.setMessage("Sorry，current smartband does not support editing！");
        }
        if(a==8){
            dialog.setTitle("Time format modification");
            dialog.setMessage("Sorry，current smartband does not support editing！");
        }
        if(a==9){
            dialog.setTitle("Time format modification");
            dialog.setMessage("Time format is changed from 12-hour to 24-hour");
        }
        if(a==10){
            dialog.setTitle("Time format modification");
            dialog.setMessage("Time format is changed from 24-hour to 12-hour");
        }
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK",new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
            }
        });
        dialog.setNegativeButton("Cancel",new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){}
        });
        dialog.show();
    }
    public double setTranslation(double value ,int count){
        BigDecimal temp = new BigDecimal(value);
        double result = temp.setScale(count, BigDecimal.ROUND_HALF_UP).doubleValue();
        return result;
    }
    private void reStartActivity() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

}

