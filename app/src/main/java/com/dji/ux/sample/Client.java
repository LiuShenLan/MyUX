package com.dji.ux.sample;

import androidx.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import dji.common.battery.BatteryState;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.GimbalState;
import dji.common.util.CommonCallbacks;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;

import static java.lang.Thread.sleep;

public class Client {
    private static final String TAG = "MyClient";
//    private static final String HOST_NAME = "172.20.10.4";
//    private static final int PORT_NUM = 6666;

    private Aircraft mProduct;
    private int portNum;
    private String ipAddress;
    private boolean mStart = true;
    private Handler handler;//回传消息的handler
    private Boolean isRecording = false;
    public static String waypoint_longitude;  //经度
    public static String waypoint_latitude;   //维度
    public static String waypoint_altitude;   //高度

    private float mPitch, mRoll, mYaw, mThrottle;
    private Timer mSendVirtualStickDataTimer,testTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private FlightController testFlightController = null;


    public Client(Aircraft product,int port,String address,Handler mainHandler) {
        mProduct = product;
        portNum = port;
        ipAddress = address;
        handler = mainHandler;
        //String ip = getLocalIpAddress();

        final JSONObject jsonObject = new JSONObject();
        //JSONObject jsonObject = new JSONObject();
        mProduct.getFlightController().setStateCallback(new FlightControllerState.Callback(){
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {

                JSONObject GPSJson = new JSONObject();
                //String write_txt = "-- "+MainActivity.gettime()+" --"+"\n";
                try {
//                    String temp1 = String.valueOf(flightControllerState.getVelocityX())+","+String.valueOf(flightControllerState.getVelocityY())+","+String.valueOf(flightControllerState.getVelocityZ())+"\n";
//                    write_txt+=temp1;
//                    if (isRecording){
//                            MainActivity.writetxt(write_txt,true);
//                    }
                    if(flightControllerState.getGPSSignalLevel() != null) {
                        GPSSignalLevel gpsLevel = flightControllerState.getGPSSignalLevel();
                        GPSJson.put("gpsLevel", gpsLevel.toString());
                    }
                    if(flightControllerState.getAircraftLocation() != null) {
                        LocationCoordinate3D location = flightControllerState.getAircraftLocation();
//                        GPSJson.put("longitude", String.valueOf(location.getLongitude()));
//                        GPSJson.put("latitude", String.valueOf(location.getLatitude()));
//                        GPSJson.put("altitude", String.valueOf(location.getAltitude()));
                        GPSJson.put("longitude", location.getLongitude());
                        GPSJson.put("latitude", location.getLatitude());
                        GPSJson.put("altitude", location.getAltitude());

                        //执行waypoint的test代码
                        waypoint_longitude = String.valueOf(location.getLongitude());
                        waypoint_latitude = String.valueOf(location.getLatitude());
                        waypoint_altitude = String.valueOf(location.getAltitude());

//                        String temp2 = "longitude:"+String.valueOf(location.getLongitude())+",latitude:"+String.valueOf(location.getLatitude())+",altitude:"+String.valueOf(location.getAltitude())+"\n";
//                        write_txt+=temp2;
//                        if (isRecording){
//                            MainActivity.writetxt(write_txt,true);
//                        }
                    }
                    if(flightControllerState.getAttitude() != null) {
                        Attitude attitude = flightControllerState.getAttitude();
                        GPSJson.put("pitch", attitude.pitch);
                        GPSJson.put("roll", attitude.roll);
                        GPSJson.put("yaw", attitude.yaw);
                    }

                    GPSJson.put("velocityX", flightControllerState.getVelocityX());
                    GPSJson.put("velocityY", flightControllerState.getVelocityY());
                    GPSJson.put("velocityZ", flightControllerState.getVelocityZ());

                    // Update the values in GPS key
                    jsonObject.put("GPS", GPSJson);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        List<Battery> batteries = mProduct.getBatteries();
        for(final Battery battery : batteries) {
            battery.setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    JSONObject batteryJson = new JSONObject();
                    try {
                        batteryJson.put("BatteryEnergyRemainingPercent", batteryState.getChargeRemainingInPercent());
                        batteryJson.put("Voltage", batteryState.getVoltage());
                        batteryJson.put("Current", batteryState.getCurrent());

                        // Update the values in Battery key
                        jsonObject.put("Battery" + battery.getIndex(), batteryJson);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        mProduct.getGimbal().setStateCallback(new GimbalState.Callback() {
            @Override
            public void onUpdate(@NonNull GimbalState gimbalState) {
                JSONObject gimbalJson = new JSONObject();
                try {
                    if(gimbalState.getAttitudeInDegrees() != null) {
                        dji.common.gimbal.Attitude attitude = gimbalState.getAttitudeInDegrees();
                        gimbalJson.put("pitch", String.valueOf(attitude.getPitch()));
                        gimbalJson.put("roll", String.valueOf(attitude.getRoll()));
                        gimbalJson.put("yaw", String.valueOf(attitude.getYaw()));
                    }

                    // Update the values in Gimbal key
                    jsonObject.put("Gimbal", gimbalJson);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mProduct.getCamera().setSystemStateCallback(new SystemState.Callback() {
            @Override
            public void onUpdate(@NonNull SystemState systemState) {
                JSONObject cameraJson = new JSONObject();
                try {
                    if(systemState.isRecording()) {
                        cameraJson.put("isRecording", "yes");
                        isRecording = true;
                        //sendMsg("start recording");
                    }
                    else {
                        cameraJson.put("isRecording", "no");
                        isRecording = false;
                        //sendMsg("stop recording");
                    }

                    // Update the values in Gimbal key
                    jsonObject.put("Camera", cameraJson);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    sleep(5000);
//                    MainActivity.writetxt(jsonObject.toString(),true);
//                    MainActivity.writetxt("\n",true);
//                    //sendMsg("drone data saved to UXSDK_log.txt");
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }).start();


        //test
        //final JSONObject jsonObject_t = testJson();

        // 发送数据的线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Set up client socket
                    Socket socket = new Socket(ipAddress, portNum);

                    // Input and Output Streams
                    //创建数据输出流对象,创建成功后可以对流进行写操作或者其他操作
                    final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    while(mStart) {
                        try {
                            // Send the JsonObject every 2s
                            //Log.i(TAG, jsonObject_t.toString());
                            out.writeUTF(jsonObject.toString());

                            sleep(500);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //关闭socket，释放资源
                    out.close();
                    socket.close();
                }
                // Error Handling
                catch (UnknownHostException e) {
                    Log.e(TAG, "Don't know about host " + ipAddress);
                }
                catch (IOException e) {
                    Log.e(TAG, "Couldn't get I/O for the connection to " + ipAddress);
                    Log.e(TAG, "Maybe the Server is not online");
                    e.printStackTrace();
                }
                System.out.println("Send Thread run finish");
            }
        }).start();
        System.out.println("Send thread start……");

        // 接收数据的线程
        new Thread(new Runnable() {
            @Override
            public void run() {

//                ServerSocket serverSocket = null;
//                try {
//                    serverSocket = new ServerSocket(6666);
//                    serverSocket.setSoTimeout(1000000);
//                }catch (Exception e){
//
//                }
//
//                while(true)
//                {
//                    try
//                    {
//                        System.out.println("等待远程连接，端口号为：" + serverSocket.getLocalPort() + "...");
//                        Socket server = serverSocket.accept();
//                        System.out.println("远程主机地址：" + server.getRemoteSocketAddress());
//                        DataInputStream in = new DataInputStream(server.getInputStream());
//                        sendMsg(in.readUTF());
//                        //System.out.println(in.readUTF());
//                        server.close();
//                    }catch(SocketTimeoutException s)
//                    {
//                        System.out.println("Socket timed out!");
//                        break;
//                    }catch(IOException e)
//                    {
//                        e.printStackTrace();
//                        break;
//                    }
//                }

                try {
                    // Set up client socket
                    Socket socket = new Socket(ipAddress, 6665 );
                    //先发一条数据
                    final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    String test = new String("hello");
                    out.writeUTF("hello");

                    // 读Sock里面的数据
                    InputStream s = socket.getInputStream();
                    byte[] buf = new byte[1024];
                    int len = 0;
                    while (true) {
                        if ((len = s.read(buf)) != -1) {
                            System.out.println(new String(buf, 0, len));
                            sendMsg(new String(buf, 0, len));
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("Receive Thread run finish");
            }
        }).start();
        System.out.println("Receive thread start……");
        //MainActivity.writetxt("write log test: it is ok");
    }

    void stop() {
        mStart = false;
    }

    //起飞任务
    void takeoff(){
        mProduct.getFlightController().startTakeoff(new CommonCallbacks.CompletionCallback() {  //这是一个实现了接口的匿名内部类,为了使用这个类,必须将其实例化,相当于new了一个对象,这里参数需要的是一个接口(父类引用指向子类对象)
            @Override
            public void onResult(DJIError djiError) {
                //MainActivity.writetxt("\nTakeoff result: "+djiError.getDescription(),true);
                System.out.println("takeoff回调函数执行");
            }
        });
    }

    //降落任务
    void landing(){
        mProduct.getFlightController().startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                System.out.println("landing回调函数执行");
            }
        });

    }

    void testTaskStart(){
        System.out.println("hahahahahahahahahahahahahahahahahahhahahahahahahh");
//        if(testFlightController == null){
            testFlightController =  mProduct.getFlightController();
            testFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            testFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            testFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            testFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            System.out.println("heiheieheiheiheiheihei..............................................");
            testFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        //new MainActivity().showToast(djiError.getDescription());
                        MainActivity.writetxt("testTaskStart()::"+djiError.getDescription()+"\n",true);
                        System.out.println("虚拟控制失败了!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        //Toast.makeText();
                    }else
                    {
                        //new MainActivity().showToast("Enable Virtual Stick Success");
                        MainActivity.writetxt("\ntestTaskStart()::Enable Virtual Stick Success\n",true);
                        System.out.println("虚拟控制开始了!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        //showToast("Enable Virtual Stick Success");
                    }
                }
            });
//        }

//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                sendVirtualStickData(0.5f,0.5f,0.5f);
//            }
//        };
//        testTimer = new Timer();
//        testTimer.schedule(task, 0, 200);//每隔200ms调用一次sendVirtualStickData()

    }
    /** Called when the joystick is touched.
     * @ pX The x coordinate of the knob. Values are between -1 (left) and 1 (right).
     * @ pY The y coordinate of the knob. Values are between -1 (down) and 1 (up).
     */
    void sendVirtualStickData(float pitch,float roll,float yaw,float throttle){

        float pitchJoyControlMaxSpeed = 10;
        float rollJoyControlMaxSpeed = 10;
        mPitch = (float)(pitchJoyControlMaxSpeed * pitch);
        mRoll = (float)(rollJoyControlMaxSpeed * roll);

        float verticalJoyControlMaxSpeed = 2;
        float yawJoyControlMaxSpeed = 30;
        mYaw = (float)(yawJoyControlMaxSpeed * yaw);
        mThrottle = (float)(verticalJoyControlMaxSpeed * throttle);

        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 100);
        }
//        System.out.println("发送控制信息成功.........................................................");
        MainActivity.writetxt("\n发送控制信息成功.................................................",true);

    }

    void stopSendVirtualStick(){
        testFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null){
                    //new MainActivity().showToast(djiError.getDescription());
                   MainActivity.writetxt("stopSendVirtualStick()::"+djiError.getDescription()+"\n",true);
                    //Toast.makeText();
                }
                else {
                    //new MainActivity().showToast("Enable Virtual Stick Success");
                   MainActivity.writetxt("\nstopSendVirtualStick()::Disable Virtual Stick Success\n",true);
                    //showToast("Enable Virtual Stick Success");
                    if (null != mSendVirtualStickDataTimer) {
//                        mSendVirtualStickDataTask.cancel();
//                        mSendVirtualStickDataTask = null;
//                        testFlightController = null;
                        mSendVirtualStickDataTask.cancel();
                        mSendVirtualStickDataTask = null;
                        mSendVirtualStickDataTimer.cancel();
                        mSendVirtualStickDataTimer.purge();
                        mSendVirtualStickDataTimer = null;

                        testFlightController = null;
                    }

                }
            }
        });
    }

    // the test json string, only for test
    JSONObject testJson(){
        JSONObject jsonObject_test = new JSONObject();

        JSONObject GPSJson = new JSONObject();
        JSONObject batteryJson = new JSONObject();
        JSONObject gimbalJson = new JSONObject();
        try{
            GPSJson.put("gpsLevel", "10");
            GPSJson.put("longitude", String.valueOf("74.5"));
            GPSJson.put("latitude", String.valueOf("56.8"));
            GPSJson.put("altitude", String.valueOf("623.5"));
            GPSJson.put("pitch", String.valueOf("90"));
            GPSJson.put("roll", String.valueOf("90"));
            GPSJson.put("yaw", String.valueOf("90"));
            GPSJson.put("velocityX", String.valueOf("100"));
            GPSJson.put("velocityY", String.valueOf("100"));
            GPSJson.put("velocityZ", String.valueOf("100"));

            batteryJson.put("BatteryEnergyRemainingPercent", 80);
            batteryJson.put("Voltage", 30);
            batteryJson.put("Current", 30);

            gimbalJson.put("pitch", String.valueOf("90"));
            gimbalJson.put("roll", String.valueOf("90"));
            gimbalJson.put("yaw", String.valueOf("90"));

            // Update the values in Gimbal key
            jsonObject_test.put("Gimbal", gimbalJson);
            // Update the values in Battery key
            jsonObject_test.put("Battery1", batteryJson);
            // Update the values in GPS key
            jsonObject_test.put("GPS", GPSJson);
            // 发送app的IP端口给主机
            //jsonObject_test.put("AppHostIp",appipAddress+":6060");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject_test;
    }

    private void sendMsg(String receive) {
        Message msg = new Message();
        msg.what = 1;
        //使用Bundle绑定数据
        Bundle bundleData = new Bundle();
        bundleData.putString("rece", receive);
        msg.setData(bundleData);
        handler.sendMessage(msg);
    }


    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {

            if (testFlightController != null) {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                testFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null){
                                    //new MainActivity().showToast(djiError.getDescription());
                                   MainActivity.writetxt(djiError.getDescription(),true);
                                    //Toast.makeText();
                                }
                            }
                        }
                );
                MainActivity.writetxt("\n控制信息发送完成!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!",true);
            }
        }
    }
}
