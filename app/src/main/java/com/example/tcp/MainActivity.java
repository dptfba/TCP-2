package com.example.tcp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.net.InetAddresses;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * APP做为客户端
 **/
public class MainActivity extends AppCompatActivity {

    String tag = "=======err";


    TextView tv_ip;//ip地址
    Button btn_connect;//连接

    TextView tv_voltage;//电压
    TextView tv_current;//电流
    TextView tv_power;//功率
    EditText et_setU;//U_SET
    EditText et_setI;//I_SET
    EditText et_ovp;//ovp
    EditText et_ocp;//ocp
    Button btn_send;//发送

    ImageButton btn_switch;//开关
    private boolean isOn = false;//用来判断是否是开机状态

    Socket socket;//创建socket对象
    boolean ConnectFlage = true;//连接标志,控制按钮显示连接和断开
    ThreadConnectService threadConnectService = new ThreadConnectService();//建立一个连接任务的变量
    InputStream inputStream;//获取输入流,可以用来判断有没有断开连接
    OutputStream outputStream;//获得输出流
    ThreadReadData threadReadData = new ThreadReadData();//接收数据的任务
    // ThreadSendData threadSendData = new ThreadSendData();//发送数据的任务
    boolean threadReadDataFlage = false;//接收数据任务一直运行控制
    boolean threadSendDataFlage = false;//发送数据任务一直运行控制
    byte[] ReadBuffer = new byte[2048];//存储接收到的数据
    byte[] SendBuffer = new byte[2048];//存储发送的数据
    int ReadBufferLenght = 0;//长度
    int SendDataCnt = 0;//控制发送数据的个数

    // private SharedPreferences sharedPreferences;//存储数据
    // private SharedPreferences.Editor editor;//存储数据

    String IPAdressSaveData = "";
    String sendDataString = "";

    Thread mthreadConnectService;//记录连接任务
    Thread mthreadSendData;//记录发送任务
    Thread mthreadReadData;//记录接收任务

    int data = 0;//switch语句

    final Timer timer = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_ip = findViewById(R.id.tv_ip);
        tv_ip.setText(getLocalIpAddress());
        btn_connect = findViewById(R.id.btn_connect);
        tv_voltage = findViewById(R.id.tv_voltage);
        tv_current = findViewById(R.id.tv_current);
        tv_power = findViewById(R.id.tv_power);
        et_setU = findViewById(R.id.et_setU);
        et_setI = findViewById(R.id.et_setI);
        et_ovp = findViewById(R.id.et_ovp);
        et_ocp = findViewById(R.id.et_ocp);
        btn_switch = findViewById(R.id.btn_switch);
        btn_send = findViewById(R.id.btn_send);

        //点击连接按钮
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectFlage) {//如果连接成功
                    try {
                        threadConnectService.start();//启动连接服务器的线程任务
                        mthreadConnectService = threadConnectService;
                        Toast.makeText(getApplicationContext(), "连接服务器成功", Toast.LENGTH_SHORT).show();

                        autoSendData();//自动发送数据


                    } catch (Exception e) {

                    }
                } else {
                    ConnectFlage = true;
                    threadSendDataFlage = false;//关掉发送任务,预防产生多的任务
                    SendDataCnt = 0;
                    threadReadDataFlage = false;//关掉接收任务,预防产生多的任务
                    Toast.makeText(getApplicationContext(), "请连接服务器", Toast.LENGTH_SHORT).show();

                    try {
                        mthreadConnectService.interrupt();
                    } catch (Exception e) {
                    }
                    try {
                        mthreadSendData.interrupt();
                    } catch (Exception e2) {
                    }
                    try {
                        mthreadReadData.interrupt();
                    } catch (Exception e2) {
                    }
                    try {
                        socket.close();//关闭socket
                        inputStream.close();//关闭数据流
                    } catch (Exception e) {
                        // TODO: handle exception
                    }


                }

            }
        });

        //点击发送时
//        btn_send.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try{
//                    String SendVoltageStr = "AA 00 20 01 40 5D C0 00 00 00 00 00 00 00 00 00 00 00 00 28";
//                    byte[] SendBuffer0 = HexString2Bytes(SendVoltageStr.replace(" ", ""));//16进制发送
//
//
//                    for (int i = 0; i < SendBuffer0.length; i++) {
//                        SendBuffer[i] = SendBuffer0[i];
//
//                    }
//                    SendDataCnt = SendBuffer0.length;
//                    Log.d("==点击发送按钮发送的数据===", SendBuffer0.toString());
//
//                    outputStream.write(SendBuffer, 0, SendDataCnt);//byte[] SendBuffer=new byte[2048];
//                    // 存储发送的数据
//                    SendDataCnt=0;//清零发送的个数
//                }catch (Exception e){
//
//                }
//
//
//
//            }
//        });


    }


    /**
     * 连接服务器的线程任务
     **/
    class ThreadConnectService extends Thread {
        @Override
        public void run() {

            InetAddress iptAddresses;
            String IPAdressPort = "";

            try {
                // IPAdressPort=editTextIPAdress.getText().toString();
                String[] temp = IPAdressPort.split(":");

                // iptAddresses= InetAddress.getByName(temp[0]);//获取IP地址
                // int port=Integer.valueOf(temp[1]);//获取端口号


                String ip = "192.168.1.115";
                int port = 9999;//端口号

                socket = new Socket(ip, port);//创建连接地址和端口号

                // ConnectFlage=false;//是控制连接按钮显示的
                sendHandleMsg(mHandler, "ConState", "ConOK");//向Handle发送连接成功的消息
                //  editor=sharedPreferences.edit();
                // editor.putString("IPAdressData",IPAdressPort);
                // editor.commit();

                inputStream = socket.getInputStream();//获得通道的数据流
                outputStream = socket.getOutputStream();//获得通道的输出流
                threadReadDataFlage = true;//一直接受数据
                threadSendDataFlage = true;//一直循环的判断是否发送数据

//                ThreadSendData threadSendData = new ThreadSendData();
//                threadSendData.start();//发送数据线程开启
//                mthreadSendData = threadSendData;


                ThreadReadData threadReadData = new ThreadReadData();
                threadReadData.start();//接收数据线程开启
                mthreadReadData = threadReadData;


            } catch (Exception e) {
                Log.e(tag, e.toString());

            }
        }
    }



    /**
     * 用线程实现每隔一段时间自动执行发送代码
     **/
    private void autoSendData() {
        timer.scheduleAtFixedRate(new TimerTask() {


            @Override
            public void run() {

                   if(SendDataCnt>0){//要发送的数据个数大于0
                       try {
                           String SendVoltageStr = "AA 00 20 01 40 5D C0 00 00 00 00 00 00 00 00 00 00 00 00 28";
                           byte[] SendBuffer0 = HexString2Bytes(SendVoltageStr.replace(" ", ""));//16进制发送


                           for (int i = 0; i < SendBuffer0.length; i++) {
                               SendBuffer[i] = SendBuffer0[i];

                           }
                           SendDataCnt = SendBuffer0.length;
                           Log.d("==自动发送的数据===", SendBuffer0.toString());

                           outputStream.write(SendBuffer, 0, SendDataCnt);//byte[] SendBuffer=new byte[2048];
                           // 存储发送的数据
                           SendDataCnt = 0;//清零发送的个数

                       } catch (Exception e) {
                           sendHandleMsg(mHandler,"ConState","ConNo");//向Handle发送消息

                       }
                   }

            }

        }, 1000, 2000);
    }

    /**
     * 接收数据的任务
     ***/
    class ThreadReadData extends Thread {
        boolean mThreadReadDataFlage = true;

        @Override
        public void run() {
            while (mThreadReadDataFlage && threadReadDataFlage) {
                try {

                    ReadBufferLenght = inputStream.read(ReadBuffer);//服务器断开会返回-1,ReadBufferLenght是读取数据的长度
                    byte[] ReadBuffer0 = new byte[ReadBufferLenght];//存储接收到的数据
                    for (int i = 0; i < ReadBufferLenght; i++) {
                        ReadBuffer0[i] = ReadBuffer[i];
                    }

                     sendHandleMsg(mHandler, "ReadData", ReadBuffer0);//向Handle发送消息

                    if (ReadBufferLenght == -1) {
                        mThreadReadDataFlage = false;
                        threadReadDataFlage = false;//关掉接收任务,预防产生多的任务
                        // ConnectFlage=true;
                        threadSendDataFlage = false;//关掉发送任务,预防产生多的任务
                        SendDataCnt = 0;

                        sendHandleMsg(mHandler, "ConState", "ConNO");//向Handle发送消息

                    }

                } catch (Exception e) {
                    sendHandleMsg(mHandler,"ConState","ConNO");//向Handle发送消息
                    mThreadReadDataFlage=false;
                    threadSendDataFlage=false;//关掉接收任务,预防产生多的任务
                    threadSendDataFlage=false;//关掉发送任务,预防产生多的任务
                    try{mthreadReadData.interrupt();}catch (Exception e2){}
                    SendDataCnt=0;
                }

            }
        }
    }


    /**
     * Handler
     **/

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            String string = bundle.getString("ConState");
            try
            {
                if(string.equals("ConOK"))
                {
                    Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
                }
                else if (string.equals("ConNO")) {
                    Toast.makeText(getApplicationContext(), "与服务器断开连接", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                // TODO: handle exception
            }

            byte[] ReadByte = bundle.getByteArray("ReadData");
            if(ReadByte!=null){
                tv_voltage.setText(byteToHexStr(ReadByte));
            }



        }
    };

    /**
     * 向handle发送消息方法
     **/

    private void sendHandleMsg(Handler handler, String key, String Msg) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(key, Msg);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    /**
     * 发送的是byte
     **/

    private void sendHandleMsg(Handler handler, String key, byte[] byt) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putByteArray(key, byt);
        msg.setData(bundle);
        handler.sendMessage(msg);

    }


    /**
     * 获取WIFI下ip地址
     */
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    /**
     * 16进制byte转16进制String--用空格隔开
     *
     * @param bytes
     * @return
     */
    public static String byteToHexStr(byte[] bytes) {
        String str_msg = "";
        for (int i = 0; i < bytes.length; i++) {
            str_msg = str_msg + String.format("%02X", bytes[i]) + " ";
        }
        return str_msg;
    }


    //16进制字节数组转换为16进制字符串
    public static String bytyToHexstr(byte[] bytes) {

        String str_msg = "";
        for (int i = 0; i < bytes.length; i++) {
            str_msg = str_msg + String.format("%02X", bytes[i]) + "";

        }
        return str_msg;
    }
    //发送时,把获取到的字符串转换为16进制

    /**
     * 添上格式,实际上咱获取的文本框里面的都是字符串,咱需要把字符串转化为  如"33"==>0x33
     * 将已十六进制编码后的字符串src,以每两个字符分割转换为16进制形式
     * 如:"2B44EED9"--> byte[]{0x2B,0x44,0xEF0xD9}
     **/
    public static byte[] HexString2Bytes(String str) {
        StringBuilder sb = null;
        String src = null;
        if ((str.length() % 2) != 0) {//数据不是偶数
            sb = new StringBuilder(str);//构造一个StringBuilder对象
            sb.insert(str.length() - 1, "0");//在指定的位置1,插入指定的字符串
            src = sb.toString();

        } else {
            src = str;
        }
        Log.e("error", "str.length" + str.length());
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < tmp.length / 2; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);

        }
        return ret;

    }

    //将两个ASCII字符合成一个字节;如:"EF"-->0xEF.Byte.decode()将String解码为 Byte
    public static byte uniteBytes(byte src0, byte src1) {

        try {
            byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();//.byteValue()转换为byte类型的数
            // 该方法的作用是以byte类型返回该 Integer 的值。只取低八位的值，高位不要。
            _b0 = (byte) (_b0 << 4);//左移4位
            byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
            byte ret = (byte) (_b0 ^ _b1);//按位异或运算符(^)是二元运算符，要化为二进制才能进行计算
            return ret;

        } catch (Exception e) {
            //TODO:handle exception
        }

        return 0;
    }

    /**
     * CRC检验值
     *
     * @param modbusdata
     * @param length
     * @return CRC检验值
     */
    protected int crc16_modbus(byte[] modbusdata, int length) {
        int i = 0, j = 0;
        int crc = 0xffff;//有的用0,有的用0xff
        try {
            for (i = 0; i < length; i++) {
                //注意这里要&0xff,因为byte是-128~127,&0xff 就是0x0000 0000 0000 0000  0000 0000 1111 1111
                //参见:https://blog.csdn.net/ido1ok/article/details/85235955
                crc ^= (modbusdata[i] & (0xff));
                for (j = 0; j < 8; j++) {
                    if ((crc & 0x01) == 1) {
                        crc = (crc >> 1);
                        crc = crc ^ 0xa001;
                    } else {
                        crc >>= 1;
                    }
                }
            }
        } catch (Exception e) {

        }

        return crc;
    }

    /**
     * CRC校验正确标志
     *
     * @param modbusdata
     * @param length
     * @return 0-failed 1-success
     */
    protected int crc16_flage(byte[] modbusdata, int length) {
        int Receive_CRC = 0, calculation = 0;//接收到的CRC,计算的CRC

        Receive_CRC = crc16_modbus(modbusdata, length);
        calculation = modbusdata[length];
        calculation <<= 8;
        calculation += modbusdata[length + 1];
        if (calculation != Receive_CRC) {
            return 0;
        }
        return 1;
    }


}
