package com.dji.ux.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ch.ielse.view.SwitchView;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;

/** Activity that shows all the UI elements together */
public class CompleteWidgetActivity extends Activity {
    private float throttle;
    private float pitch;
    private float roll;
    private float yaw;

    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    private float mSpeed = 3.0f;
    private float autospeed = 3.0f;

    Button btn_setip;
    Button waypointsbutton;
    SwitchView switchView;
    private String ipHost="none";
    private int portNum = 0;
    private Client mClient;
    private BaseProduct mProduct;

    //waypoint
    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMission mission;
    private WaypointMissionOperatorListener listener;
    private ArrayList myPointList;
    private float flyAltitude;
    private String testJson = "";
    private AlertDialog wayPointDialog;

    public Boolean X = false; //?????????
    public Boolean preY = false;
    public Boolean Z = false;

    public int index;

    Timer timer = new Timer();

    /**Handler**/
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String sss = msg.getData().getString("rece");
                    int task = gettaskFlag(sss);
                    //showResultToast("task:"+task);
                   MainActivity.writetxt("Received data:"+sss+",   taskFlag:"+task+"\n",true);
                   MainActivity.writetxt("Received task flag "+task+"\n",true);
                    if(task==0){
                        mClient.takeoff();
                    }
                    else if(task==2){
                        //virtual stick task
                        System.out.println("?????????mission2???............................................");
                        mClient.testTaskStart();
                        float [] resnums = getThreeFloat(msg.getData().getString("rece"));
                        System.out.println("?????????????????????................................................");
                        mClient.sendVirtualStickData(resnums[0],resnums[1],resnums[2],resnums[3]);
                    }
                    else if(task==3){
                        //virtual stick stop
                        //MainActivity.writetxt("Received task flag 3\n",true);
                        mClient.stopSendVirtualStick();
                    }else if(task==4) {
                        mClient.landing();
                    }else if(task ==1) {
                        //showNormalDialog(sss);
                        MainActivity.writetxt("\nReceived WayPoints data:\n", true);
                        MainActivity.writetxt(sss + "\n", true);
                        myPointList = getWayPoints(sss);  //??????????????????
                        MainActivity.writetxt("\nParse point list from json data:\n", true);
                        MainActivity.writetxt(myPointList + "\n", true);
//                    for(int k=0;k<myPointList.size();k++){
//                        MainActivity.writetxt(myPointList.get(k).toString()+"\n",true);
//                        showNormalDialog(myPointList.get(k).toString());
//                    }
                        execute_waypoint_task(0, myPointList);//load waypoint task
//                        execute_waypoint_task(1,myPointList);//start waypoint task
                        showNormalDialog("666");  //??????????????????,?????????OK???
//                    }else{
//                        Boolean isCollFlag = getPauseFlag(sss);
//                        myPointList = getWayPoints(sss);
//                        if(isCollFlag==true){
//                            //??????waypoints??????
//                            execute_waypoint_task(3,myPointList);
//                        }else{  //isCollFlag==false && X==true
//                            //??????waypoints??????
//                            execute_waypoint_task(4,myPointList);
//                        }
//                    }
                    }else{
                        Boolean isCollFlag = getPauseFlag(sss);
                        myPointList = getWayPoints(sss);
                        Z = isCollFlag;
                        if(Z!=preY){
                            if(isCollFlag==true){
                                //??????waypoints??????
                                execute_waypoint_task(3,myPointList);
                            }else {
                                //??????waypoints??????
                                execute_waypoint_task(4,myPointList);
                            }
                        }
                        //------?????????--------
                        if(isCollFlag==true){
                            mClient.testTaskStart();
                            float [] resnums = {0,0,0,0.03f};
                            mClient.sendVirtualStickData(resnums[0],resnums[1],resnums[2],resnums[3]);
                        }else {
                            mClient.stopSendVirtualStick();
                        }
                        //-------------------
                        preY = isCollFlag;
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);
        MainActivity.writetxt("************* Log Start("+MainActivity.getCurrentTimeStr()+") *************\n",true);
        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        mapWidget = (MapWidget) findViewById(R.id.map_widget);
        mapWidget.initAMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onViewClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);

        parentView = (ViewGroup) findViewById(R.id.root_view);
        btn_setip = (Button) findViewById(R.id.btn_setip);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(fpvWidget);
            }
        });

        //-------------------------------------------------------------------------------------
        waypointsbutton = (Button)findViewById(R.id.waypointsbutton);
        waypointsbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWaypointsSpeedSettingDialog();
            }
        });
        //------------------------------------------------------------------------------------

        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapVideoSource();
            }
        });
        switchView = (SwitchView)findViewById(R.id.toggle_button1) ;
        switchView.setOpened(true);
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isOpened = switchView.isOpened();
                if(isOpened){//???
                    mClient = new Client((Aircraft) mProduct,portNum,ipHost,handler);
                    Toast.makeText(CompleteWidgetActivity.this,"??????????????????",Toast.LENGTH_SHORT).show();
                }
                else{//???
                    mClient.stop();
                    Toast.makeText(CompleteWidgetActivity.this,"??????????????????",Toast.LENGTH_SHORT).show();
                }
            }
        });
        btn_setip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
        btn_setip.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Toast.makeText(CompleteWidgetActivity.this,"VirtualStick??????????????????",Toast.LENGTH_SHORT).show();
                //mClient.testTaskStart();
                return false;
            }
        });

        updateSecondaryVideoVisibility();
        //MainActivity.writetxt("**************************\n",true);

        //test
//        myPointList = getWayPoints(js);
//        System.out.println("List: "+myPointList);
//        for(int i = 0 ; i < myPointList.size() ; i++){
//            System.out.println(myPointList.get(i));
//            showNormalDialog(myPointList.get(i).toString());
//            MainActivity.writetxt(myPointList.get(i).toString()+"\n",true);
//        }
        //???????????????????????????waypoint??????
//        createWayPointDialog();
//        final Handler handler2 = new Handler();
//        Runnable runnable2 = new Runnable() {
//            @Override
//            public void run() {
//                handler2.postDelayed(this, 60000);
//                wayPointDialog.show();
//                //timer_tv_2.setText("Timer2-->" + getSystemTime());
//                //mytext.setText(getDatePoor());
//            }
//        };
//        handler2.postDelayed(runnable2, 60000);
        //mytest();
        //mClient.takeoff();
    }

    //-----------------------------------------------------------------------------------------------------

    private void showWaypointsSpeedSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.speed_config, null);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.LowSpeed) {
                    autospeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed) {
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed) {
                    mSpeed = 10.0f;
                }
            }

        });
    }
    //-------------------------------------------------------------------------------------------------------

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            resizeFPVWidget(width, height, margin, 5);
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) fpvWidget.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        fpvWidget.setLayoutParams(fpvParams);

        parentView.removeView(fpvWidget);
        parentView.addView(fpvWidget, fpvInsertPosition);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }

        initClient();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        MainActivity.writetxt("*********************** Log End ***********************\n\n",true);
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

//    private void showInputDialog(){
//        new LovelyTextInputDialog(this)
//                //.setTopColorRes(R.color.darkDeepOrange)
//                .setTitle("????????????IP??????")
//                .setMessage("?????????'x.x.x.x:x'??????????????????")
//                //.setIcon(R.drawable.ic_assignment_white_36dp)
//                .setInputFilter("??????????????????", new LovelyTextInputDialog.TextFilter() {
//                    @Override
//                    public boolean check(String text) {
//                        //return text.matches("\\w+");
//                        //112.112.112.122:5698
//                        String[] ss = text.split(":");
//                        if(ss.length == 2){
//                            return ipCheck(ss[0]);
//                        }
//                        else{
//                            return false;
//                        }
//                    }
//                })
//                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
//                    @Override
//                    public void onTextInputConfirmed(String text) {
//                        String[] s = text.split(":");
//                        SharedPreferences share = CompleteWidgetActivity.super.getSharedPreferences("savedip", MODE_PRIVATE);//?????????
//                        SharedPreferences.Editor editor = share.edit();	//????????????????????????
//                        editor.putString("host", s[0]);
//                        editor.putInt("port", Integer.parseInt(s[1]));	//?????????????????????
//                        editor.commit();	//??????????????????
//                        ipHost = s[0];
//                        portNum = Integer.parseInt(s[1]);
//                        Toast.makeText(CompleteWidgetActivity.this, "IP????????????", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .show();
//
//    }
    private void showInputDialog() {
        /*@setView ????????????EditView
         */
        final EditText editText = new EditText(CompleteWidgetActivity.this);
        editText.setHint("?????????'x.x.x.x:x'??????????????????");
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(CompleteWidgetActivity.this);
        inputDialog.setTitle("????????????IP??????").setView(editText);
        inputDialog.setPositiveButton("??????",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String[] ss = editText.getText().toString().split(":");
                        if(ss.length == 2){
                            if (ipCheck(ss[0])){
                                String[] s = ss;
                                SharedPreferences share = CompleteWidgetActivity.super.getSharedPreferences("savedip", MODE_PRIVATE);//?????????
                                SharedPreferences.Editor editor = share.edit();	//????????????????????????
                                editor.putString("host", s[0]);
                                editor.putInt("port", Integer.parseInt(s[1]));	//?????????????????????
                                editor.commit();	//??????????????????
                                ipHost = s[0];
                                portNum = Integer.parseInt(s[1]);
                                Toast.makeText(CompleteWidgetActivity.this, "IP????????????", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(CompleteWidgetActivity.this,"??????????????????!",Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            Toast.makeText(CompleteWidgetActivity.this,"??????????????????!",Toast.LENGTH_SHORT).show();
                        }
//                        Toast.makeText(CompleteWidgetActivity.this,
//                                editText.getText().toString(),
//                                Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void getsavedIP(){
        SharedPreferences share = super.getSharedPreferences("savedip", MODE_PRIVATE);
        int port = share.getInt("port", 0);// ????????????????????????????????????0
        String host = share.getString("host", "none");//????????????????????????????????????none
        if(port==0 && host.equals("none")){
            showInputDialog();
        }
        else {
            ipHost = host;
            portNum = port;
            System.out.println("host:" + ipHost+", portNum:"+port);
        }
    }
    /**
     * ??????IP?????????????????????????????????????????????????????????????????????
     * return true?????????
     * */
    private boolean ipCheck(String text) {
        if (text != null && !text.isEmpty()) {
            // ?????????????????????
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            // ??????ip????????????????????????????????????
            if (text.matches(regex)) {
                // ??????????????????
                return true;
            } else {
                // ??????????????????
                return false;
            }
        }
        return false;
    }
    private void showNormalDialog(final String msg){
        /* @setIcon ?????????????????????
         * @setTitle ?????????????????????
         * @setMessage ???????????????????????????
         * setXXX????????????Dialog???????????????????????????????????????
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(CompleteWidgetActivity.this);
        //normalDialog.setIcon(R.drawable.icon_dialog);
        normalDialog.setTitle("Message");
        normalDialog.setMessage(msg.replace("666","??????Load WayPoint??????????????????????????????"));
        normalDialog.setPositiveButton("ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                        if (msg.equals("666")){
                            //showResultToast("?????????waypoint??????");
                            execute_waypoint_task(1,myPointList);//start waypoint task
                        }
                    }
                });
//        normalDialog.setNegativeButton("??????",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        //...To-do
//                    }
//                });
        // ??????
        normalDialog.show();
    }
    private void initClient() {

        //System.out.println(getLocalIpAddress());
        mProduct =  DJISDKManager.getInstance().getProduct();
        //?????????????????????????????????????????????true??????????????????
        if (mProduct != null && mProduct.isConnected()) {
            mClient = new Client((Aircraft) mProduct,portNum,ipHost,handler);
            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            Toast.makeText(CompleteWidgetActivity.this,"Status: " + str + " connected",Toast.LENGTH_SHORT).show();
            getsavedIP();
        }
        else {
            //Toast.makeText(MainActivity.this,"no deviecs connected! ",Toast.LENGTH_SHORT).show();
            switchView.setVisibility(View.INVISIBLE);
            showNormalDialog("no device connected! ");
        }
    }
//--------------------------------------------------------------------------------------------------------------------------------------
    //??????json??????
    /*** json ??????
     * {"0000Lat":29.9988,
     *  "0000Lng":106.52332,
     *  "0001Lat":29.3666,
     *  "0001Lng":106.669,
     *     .......
     *  "baseAltitude":20.0,
     *  "way_point_num":5
     * }
     * */
    private ArrayList getWayPoints(String jsons){
        //String[] rr = jsons.split("\\{");
        ArrayList points = new ArrayList();
        try {
            JSONObject jsonObject = new JSONObject(jsons);
            int nums = jsonObject.getInt("way_point_num");
            flyAltitude = Float.parseFloat(jsonObject.getString("altitude"));
            for (int i = 0; i< nums; i++) {
                double lat = jsonObject.getDouble(i+"Lat");
                double lng = jsonObject.getDouble(i+"Lng");
                int head = jsonObject.getInt(i+"head");
                points.add(lat+","+lng+","+head);
                //System.out.println("list add: "+lat+","+lng);
                //???????????????????????????
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return points;
    }
    private float[] getThreeFloat(String jsons){
        //String[] rr = jsons.split("\\{");
        float [] numResult = {0,0,0,0};
        try {
            JSONObject jsonObject = new JSONObject(jsons);
            numResult[0] = (float) jsonObject.getDouble("pitch");
            numResult[1] = (float) jsonObject.getDouble("roll");
            numResult[2] = (float) jsonObject.getDouble("yaw");
            numResult[3] = (float) jsonObject.getDouble("throttle");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return numResult;
    }
    private int gettaskFlag(String jsons){
        int num = -1;
        try {
            JSONObject jsonObject = new JSONObject(jsons);
            num = jsonObject.getInt("mission");//0,takeoff; 1,waypoint; 2 virtual stick(yaw,pitch,roll,-1~1 3 ??????)
        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    private Boolean getPauseFlag(String jsons){
        Boolean flag = false;
        try{
            JSONObject jsonObject = new JSONObject(jsons);
//            MainActivity.writetxt("\njsonObject==================================="+jsonObject,true);
//            MainActivity.writetxt("\n=========================",true);
            flag = jsonObject.getBoolean("isCollFlag");
//            MainActivity.writetxt("\nbool value==================================="+flag,true);
//            MainActivity.writetxt("\n=========================",true);
        } catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    //??????waypoint??????
    private void execute_waypoint_task(int flag,ArrayList pointList){ //0 load, 1 start
        if (waypointMissionOperator == null) {
            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        }

//        listener = new WaypointMissionOperatorListener() {
//            @Override
//            public void onDownloadUpdate(WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
//
//            }
//
//            @Override
//            public void onUploadUpdate(WaypointMissionUploadEvent waypointMissionUploadEvent) {
//
//            }
//
//            @Override
//            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
//                // Example of Execution Listener
//                index = waypointMissionExecutionEvent.getProgress().targetWaypointIndex;
//                Boolean state = waypointMissionExecutionEvent.getProgress().isWaypointReached;
//
//                MainActivity.writetxt("?????????waypoints??????: "+index+"\n",true);
//                //--????????????????????????????????????????????????????????????????????????????????????????????????
////                updateWaypointMissionState();
//            }
//
//            @Override
//            public void onExecutionStart() {
//
//            }
//
//            @Override
//            public void onExecutionFinish(DJIError djiError) {
//
//            }
//
//        };


        switch (flag) {
            case 0:
                // Example of loading a Mission
                System.out.println("**** load waypoint ****");
                mission = createWaypointMission(pointList);  //???pointList?????????
                DJIError djiError = waypointMissionOperator.loadMission(mission);
                // Example of uploading a Mission
                if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                        || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
                    waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            showResultToast(djiError.getDescription());
                           MainActivity.writetxt("\nLoad result: "+djiError.getDescription(),true);
                        }
                    });
                } else {
                    showResultToast("Not ready!");
                }
                break;
            case 1:
                // Example of starting a Mission
                if (mission != null) {
                    MainActivity.writetxt("\npre start state:"+waypointMissionOperator.getCurrentState(),true);
                    waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            showResultToast(djiError.getDescription());
                           MainActivity.writetxt("\nExecution result:"+djiError.getDescription(),true);
                        }
                    });
                    MainActivity.writetxt("\nstarted state:"+waypointMissionOperator.getCurrentState(),true);
                } else {
                    showResultToast("Prepare Mission First!");
                }
                break;

            case 2:
                //Example of stopping a Mission
                if(mission!=null){
                    waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            showResultToast(djiError.getDescription());
                            MainActivity.writetxt("\nstop result:"+djiError.getDescription(),true);
                        }
                    });
                } else {
                    showResultToast("Prepare Mission First!");
                }
                break;

            case 3:
                //Example of pausing a Mission
                if(mission!=null){
                    //??????????????????????????????WaypointMissionState???EXECUTING?????????
                    MainActivity.writetxt("\npre pause state:"+waypointMissionOperator.getCurrentState(),true);
                    waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            showResultToast(djiError.getDescription());
                            MainActivity.writetxt("\npause result:"+djiError.getDescription(),true);
                            MainActivity.writetxt("\npause state:"+waypointMissionOperator.getCurrentState(),true);
                        }
                    });

//                    MainActivity.writetxt("\n?????????3???????????????????????????",true);
//                    mClient.testTaskStart();
//                    MainActivity.writetxt("\n?????????3???????????????????????????",true);
//                    float [] resnums = {0,0,0,0.03f};
////                    if(timer==null){
////                        Timer timer = new Timer();
////                    }
//                    for(int i=0;i<20;i++){
////                        timer.schedule(new TimerTask() {
////                            public void run() {
////                                mClient.sendVirtualStickData(resnums[0],resnums[1],resnums[2],resnums[3]);
////                            }
////                        }, 100);// ?????????????????????time,?????????100??????
//                        mClient.sendVirtualStickData(resnums[0],resnums[1],resnums[2],resnums[3]);
//                        MainActivity.writetxt("\n????????????for????????????",true);
//                    }
//
//                    MainActivity.writetxt("\n?????????3???????????????????????????",true);
//                    mClient.stopSendVirtualStick();
//                    MainActivity.writetxt("\n?????????3???????????????????????????",true);

                } else {
                    showResultToast("Prepare Mission First!");
                }
                break;

            case 4:
                //Example of resuming a Mission
                if(mission!=null){

//                    //??????????????????
////                    timer = null;
//                    MainActivity.writetxt("\n?????????4???????????????????????????",true);
//                    mClient.stopSendVirtualStick();
//                    MainActivity.writetxt("\n?????????4???????????????????????????",true);

                    //?????????????????????????????????getCurrentState???EXECUTION_PAUSED?????????
                    MainActivity.writetxt("\npre resume state:"+waypointMissionOperator.getCurrentState(),true);
                    waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            showResultToast(djiError.getDescription());
                            MainActivity.writetxt("\nresume result:"+djiError.getDescription(),true);
                            MainActivity.writetxt("\nresume state:"+waypointMissionOperator.getCurrentState(),true);
                        }
                    });
                } else {
                    showResultToast("Prepare Mission First!");
                }
                break;
        }
    }

    //????????????
//    private void takeoff(){
//        Aircraft myproduct = (Aircraft) mProduct;
//        myproduct.getFlightController().startPrecisionTakeoff(new CommonCallbacks.CompletionCallback() {
//            @Override
//            public void onResult(DJIError djiError) {
//                showResultToast(djiError.getDescription());
//                MainActivity.writetxt("\nTakeoff result: "+djiError.getDescription(),true);
//            }
//        });
//    }

    private void proData(String s){
        s=s.replace(" ","");
    }

    private void showResultToast(String msg){
        Toast.makeText(CompleteWidgetActivity.this,msg,Toast.LENGTH_SHORT).show();
    }

    private WaypointMission createWaypointMission(ArrayList pointList) {
        System.out.println("**** create waypoints ****");
        WaypointMission.Builder builder = new WaypointMission.Builder();
        List<Waypoint> waypointList = new ArrayList<>();
//        double baseLatitude = 22;
//        double baseLongitude = 113;
//        Object latitudeValue = KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LATITUDE)));
//        Object longitudeValue =
//                KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LONGITUDE)));
//        if (latitudeValue != null && latitudeValue instanceof Double) {
//            baseLatitude = (double) latitudeValue;
//        }
//        if (longitudeValue != null && longitudeValue instanceof Double) {
//            baseLongitude = (double) longitudeValue;
//        }

        final float baseAltitude = 20.0f;
        builder.autoFlightSpeed(1f); //?????????3f
        builder.maxFlightSpeed(3f);  //?????????10f
        builder.setExitMissionOnRCSignalLostEnabled(false);
//        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        builder.finishedAction(WaypointMissionFinishedAction.AUTO_LAND);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
//        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
        builder.repeatTimes(1);
        System.out.println("pointlist size:"+pointList.size());
        for (int j=0;j<pointList.size();j++){
            String[] p = pointList.get(j).toString().split(",");
            System.out.println("out???"+p[0]+","+p[1]+","+p[2]);
            final Waypoint eachWaypoint = new Waypoint(Double.parseDouble(p[0]),Double.parseDouble(p[1]),flyAltitude);
            eachWaypoint.heading = Integer.parseInt(p[2]);
            System.out.println("mission info: "+eachWaypoint.toString()+", altitude:"+eachWaypoint.altitude);
           MainActivity.writetxt("\ncreate mission:"+eachWaypoint.toString()+", altitude:"+eachWaypoint.altitude+", head:"+eachWaypoint.heading,true);
           waypointList.add(eachWaypoint);
        }



//        Random randomGenerator = new Random(System.currentTimeMillis());
//        List<Waypoint> waypointList = new ArrayList<>();
//        for (int i = 0; i < numberOfWaypoint; i++) {
//            final double variation = (Math.floor(i / 4) + 1) * 2 * ONE_METER_OFFSET;
//            final float variationFloat = (baseAltitude + (i + 1) * 2);
//            final Waypoint eachWaypoint = new Waypoint(baseLatitude + variation * Math.pow(-1, i) * Math.pow(0, i % 2),
//                    baseLongitude + variation * Math.pow(-1, (i + 1)) * Math.pow(0, (i + 1) % 2),
//                    variationFloat);
//            for (int j = 0; j < numberOfAction; j++) {
//                final int randomNumber = randomGenerator.nextInt() % 6;
//                switch (randomNumber) {
//                    case 0:
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.STAY, 1));
//                        break;
//                    case 1:
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
//                        break;
//                    case 2:
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.START_RECORD, 1));
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.STOP_RECORD, 1));
//                        break;
//                    case 3:
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
//                                randomGenerator.nextInt() % 45 - 45));
//                        break;
//                    case 4:
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,
//                                randomGenerator.nextInt() % 180));
//                        break;
//                    default:
//                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
//                        break;
//                }
//            }
//            waypointList.add(eachWaypoint);
//        }

        builder.waypointList(waypointList).waypointCount(waypointList.size());
        return builder.build();
    }

    //????????????dialog???????????????waypoint??????
    private void createWayPointDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CompleteWidgetActivity.this);
        builder.setTitle("WayPointTask");
        builder.setMessage("?????????WayPoint??????????");
        //??????????????????????????????????????????????????????
        builder.setCancelable(false);
        //??????????????????
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                flyAltitude = Float.parseFloat(Client.waypoint_altitude);
                double lat = Float.parseFloat(Client.waypoint_latitude);
                double lng = Float.parseFloat(Client.waypoint_longitude);
                ArrayList tempList = new ArrayList();//longtitude?????????lat??????
                double lat1 = lat+0.0001;
                double lng1 = lng;
                double lat2 =lat1;
                double lng2 =lng1+0.0001;
                double lat3 =lat2-0.0001;
                double lng3 =lng2;
                double lat4 =lat3;
                double lng4 =lng3-0.0001;
                tempList.add(lat1+","+lng1); // point1
                tempList.add(lat2+","+lng2); // point2
                tempList.add(lat3+","+lng3); // point3
                tempList.add(lat4+","+lng4); // point4

                execute_waypoint_task( 0, tempList);//load waypoint task
                showNormalDialog("666");
                MainActivity.writetxt("WayPoints List: \n",true);
                MainActivity.writetxt(tempList.toString(),true);
            }
        });
        //??????????????????
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        wayPointDialog = builder.create();
        //???????????????
        //dialog.show();
    }

    //??????json??????????????????????????????
    private void mytest(){
//        String sss = "{\n" +
//                "    \"0Lat\": 36.654081,\n" +
//                "    \"0Lng\": 117.033885,\n" +
//                "    \"altitude\": 8.800000190734863,\n" +
//                "    \"way_point_num\": 1\n" +
//                "}";
        String sss = "{\n" +
                "    \"0Lat\": 36.654081,\n" +
                "    \"0Lng\": 117.033885,\n" +
                "    \"1Lat\": 36.654023,\n" +
                "    \"1Lng\": 117.035753,\n" +
                "    \"altitude\": 8.800000190734863,\n" +
                "    \"way_point_num\": 2\n" +
                "}";

        //MainActivity.writetxt("Received WayPoints List:\n",true);
        //MainActivity.writetxt(sss+"\n\n",true);
        flyAltitude = 8.800000190734863f;
        myPointList = getWayPoints(sss);
        System.out.println("debug:"+myPointList);
//                    for(int k=0;k<myPointList.size();k++){
//                        MainActivity.writetxt(myPointList.get(k).toString()+"\n",true);
//                        showNormalDialog(myPointList.get(k).toString());
//                    }
        execute_waypoint_task(0,myPointList);//load waypoint task
        showNormalDialog("666");
    }

//    private void sendVirtualStickData(){
//        FlightControlData controlData = new FlightControlData();
//        controlData.setPitch();
//        controlData.setRoll();
//        controlData.setYaw();
//
//        mProduct.getFlightController().sendVirtualStickFlightControlData(controlData,new CommonCallbacks.CompletionCallback() {
//            @Override
//            public void onResult(DJIError djiError) {
//                //MainActivity.writetxt("\nTakeoff result: "+djiError.getDescription(),true);
//                System.out.println("sendVirtualStick??????????????????");
//            }
//        });
//    }


}