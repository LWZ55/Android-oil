////////////////////////////////////////////////////////////////////////////////
//
//  Scope - An Android scope written in Java.
//
//  Copyright (C) 2014	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.scope;


import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// MainActivity 控制声音信号的时域显示,也是示波器程序的启动类
public class MainActivity extends Activity
{
    private static final String PREF_INPUT = "pref_input";
    private static final String PREF_SCREEN = "pref_screen";

    private static final String TAG = "Scope";
    private static final String STATE = "state";

    private static final String TIMEBASE = "timebase";
    private static final String PORT="port";
    private static final String STORAGE = "storage";

    private static final String START = "start";
    private static final String INDEX = "index";
    private static final String LEVEL = "level";

    private static final float values[] =
    {
        0.1f, 0.2f, 0.5f, 1.0f,
        2.0f, 5.0f, 10.0f, 20.0f,
        50.0f, 100.0f, 200.0f, 500.0f
    }; //对应以100为分界线

    private static final int ports[] = {8091,8092,8093};

    private static final String strings[] =
    {
        "0.1 ms", "0.2 ms", "0.5 ms",
        "1.0 ms", "2.0 ms", "5.0 ms",
        "10 ms", "20 ms", "50 ms",
        "0.1 sec", "0.2 sec", "0.5 sec"
    };

    private static final int counts[] =
    {
        256, 512, 1024, 2048,
        4096, 8192, 16384, 32768,
        65536, 131072, 262144, 524288
    };

    protected static final int SIZE = 20;
    protected static final int DEFAULT_TIMEBASE = 3;
    protected static final int DEFAULT_PORT=0;
    protected static final float SMALL_SCALE = 200;
    protected static final float LARGE_SCALE = 200000;

    protected int timebase;
    protected int port; //端口索引

    private Scope scope;
    private XScale xscale;
    private YScale yscale;
    private Unit unit;

    //private
    private Udpserver udpserver;
    private Toast toast;
    private SubMenu submenu;
    private SubMenu portmenu;

    private boolean screen;

    // On create
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scope = (Scope)findViewById(R.id.scope);
        xscale = (XScale)findViewById(R.id.xscale);
        yscale = (YScale)findViewById(R.id.yscale);
        unit = (Unit)findViewById(R.id.unit);

        // Get action bar
        ActionBar actionBar = getActionBar();

        // Set short title
        if (actionBar != null)
            actionBar.setTitle(R.string.short_name);

        // Create
        //audio = new Audio();
         udpserver=new Udpserver();

        if (scope != null)
             scope.udpserver=udpserver;
             timebase = DEFAULT_TIMEBASE;
              port = DEFAULT_PORT;
              setPort(port,true);

        // Set up scale
        if (scope != null && xscale != null && unit != null)
        {
            scope.scale = values[timebase];  //1.0f
            xscale.scale = scope.scale;       //1.0f
            xscale.step = 1000 * xscale.scale; //1000
            unit.scale = scope.scale;          //1.0f
        }
    }

    // 菜单项 onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem item;

        // Inflate the menu; this adds items to the action bar if it
        // is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Set menu state from restored state



        // Timebase
        item = menu.findItem(R.id.timebase);
        if (timebase != DEFAULT_TIMEBASE)
        {
            if (item.hasSubMenu())
            {
                submenu = item.getSubMenu();
                item = submenu.getItem(timebase);
                if (item != null)
                    item.setChecked(true);//设置选中
            }
        }
        //Port
        item = menu.findItem(R.id.port);
        {
            if (item.hasSubMenu())
            {
                portmenu = item.getSubMenu();
                item = portmenu.getItem(port);
                if (item != null)
                    item.setChecked(true);//设置选中
            }
        }

        // Storage
        item = menu.findItem(R.id.storage);
        item.setIcon(scope.storage ?
                     R.drawable.storage_checked :
                     R.drawable.action_storage);

        return true;
    }

    // Restore state 通过键值对储存
    // onRestoreInstanceState()会在onStart()和onResume()之间执行
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        // Get saved state bundle
        Bundle bundle = savedInstanceState.getBundle(STATE);




        // Timebase
        timebase = bundle.getInt(TIMEBASE, DEFAULT_TIMEBASE);
        setTimebase(timebase, false);
        //port
        port=bundle.getInt(PORT,DEFAULT_PORT);
        setPort(port,false);

        // Storage
        scope.storage = bundle.getBoolean(STORAGE, false);

        // Start
        scope.start = bundle.getFloat(START, 0);
        xscale.start = scope.start;
        xscale.postInvalidate();

        // Index
        scope.index = bundle.getFloat(INDEX, 0);

        // Level
        yscale.index = bundle.getFloat(LEVEL, 0);
        yscale.postInvalidate();//同时更新视图
    }

    // onSaveInstanceState()会在onPause()或onStop()之前执行
    // Save state
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        // State bundle
        Bundle bundle = new Bundle();

        // Timebase
        bundle.putInt(TIMEBASE, timebase);

        //port
        bundle.putInt(PORT,port);

        // Storage
        bundle.putBoolean(STORAGE, scope.storage);

        // Start
        bundle.putFloat(START, scope.start);

        // Index
        bundle.putFloat(INDEX, scope.index);

        // Level
        bundle.putFloat(LEVEL, yscale.index);

        // Save bundle
        outState.putBundle(STATE, bundle);
    }

    // On options item，选中item

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Get id
        int id = item.getItemId();
        switch (id)
        {
        case R.id.port:
            if(item.hasSubMenu())
                portmenu = item.getSubMenu();
            break;
        case R.id.port8091:
            port = 0;
            item.setChecked(true);
            setPort(port,true);
            break;
        case R.id.port8092:
            port = 1;
            item.setChecked(true);
            setPort(port,true);
            break;
        case R.id.port8093:
            port = 2;
            item.setChecked(true);
            setPort(port,true);
            break;


        // Timebase
        case R.id.timebase:
            if (item.hasSubMenu())
                submenu = item.getSubMenu();
            break;

        // 0.1 ms
        case R.id.tb0_1ms:
            timebase = 0;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 0.2 ms
        case R.id.tb0_2ms:
            timebase = 1;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 0.5 ms
        case R.id.tb0_5ms:
            timebase = 2;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 1.0 ms
        case R.id.tb1_0ms:
            timebase = 3;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 2.0 ms
        case R.id.tb2_0ms:
            timebase = 4;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 5.0 ms
        case R.id.tb5_0ms:
            timebase = 5;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 10 ms
        case R.id.tb10ms:
            timebase = 6;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 20 ms
        case R.id.tb20ms:
            timebase = 7;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 50 ms

        case R.id.tb50ms:
            timebase = 8;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 0.1 sec
        case R.id.tb0_1sec:
            timebase = 9;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 0.2 sec
        case R.id.tb0_2sec:
            timebase = 10;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // 0.5 sec
        case R.id.tb0_5sec:
            timebase = 11;
            item.setChecked(true);
            setTimebase(timebase, true);
            break;

        // Storage
        case R.id.storage:
            if (scope != null)
            {
                scope.storage = !scope.storage;
                item.setIcon(scope.storage ?
                             R.drawable.storage_checked :
                             R.drawable.action_storage);
                showToast(scope.storage ?
                          R.string.storage_on : R.string.storage_off);
            }
            break;

        // Clear
        case R.id.clear:
            if ((scope != null) && scope.storage)
                scope.clear = true;
            break;

        // Left 问题1
        case R.id.left:
            if (scope != null && xscale != null)
            {
                scope.start -= xscale.step;//费解
                if (scope.start < 0)
                    scope.start = 0;

                xscale.start = scope.start; //保持两者始终一致
                xscale.postInvalidate();

            }
            break;

        // Right
        case R.id.right:
            if (scope != null && xscale != null)
            {
                scope.start += xscale.step;
                if (scope.start >= udpserver.length)
                    scope.start -= xscale.step;

                xscale.start = scope.start;
                xscale.postInvalidate();


            }
            break;

        // Start 相当于初始化
        case R.id.start:
            if (scope != null && xscale != null)
            {
                scope.start = 0;
                scope.index = 0;
                xscale.start = 0;
                xscale.postInvalidate();
                yscale.index = 0;
                yscale.postInvalidate();
            }
            break;

        // End
        case R.id.end:
            if (scope != null && xscale != null)
            {
                while (scope.start < udpserver.length)
                    scope.start += xscale.step;//索引号一直到最后
                scope.start -= xscale.step;
                xscale.start = scope.start;
                xscale.postInvalidate();
            }
            break;

        // Spectrum
        case R.id.action_spectrum:
            return onSpectrumClick(item);

        // Settings
        case R.id.action_settings:
            return onSettingsClick(item);

        default:
            return false;
        }

        return true;
    }

    // On settings click
    private boolean onSettingsClick(MenuItem item)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        return true;
    }

    // On spectrum click
    private boolean onSpectrumClick(MenuItem item)
    {
        Intent intent = new Intent(this, SpectrumActivity.class);
        startActivity(intent);

        return true;
    }
    void setPort(int port,boolean show)
    {
        if (scope != null && xscale != null && unit != null)
        {
            if(udpserver.srcport!=ports[port])
            {

                udpserver.srcport = ports[port];

                // Reset start
                scope.start = 0;
                xscale.start = 0;
                // Update display
                xscale.postInvalidate();
                unit.postInvalidate();
                udpserver.stop();
                udpserver.start();
            }

        }

        // Show timebase
        if (show)
            showPort(port);
    }
    void showPort(int port)
    {
        String text = "Port: " + ports[port];
        showToast(text);
    }

    // Set timebase
    void setTimebase(int timebase, boolean show)
    {
        if (scope != null && xscale != null && unit != null)
        {
            // Set up scale
            scope.scale = values[timebase];
            xscale.scale = scope.scale;
            xscale.step = 1000 * xscale.scale;
            unit.scale = scope.scale;

            // Set up scope points
            if (timebase == 0)
                scope.points = true; //显示点

            else
                scope.points = false;

            // Reset start
            scope.start = 0;
            xscale.start = 0;

            // Update display
            xscale.postInvalidate();
            unit.postInvalidate();
        }

        // Show timebase
        if (show)
            showTimebase(timebase);
    }

    // Show timebase
    void showTimebase(int timebase)
    {
        String text = "Timebase: " + strings[timebase];

        showToast(text);
    }

    // Show toast
    void showToast(int key)
    {
        Resources resources = getResources();
        String text = resources.getString(key);

        showToast(text);
    }

    // Show toast
    void showToast(String text)
    {
        // Cancel the last one
        if (toast != null)
            toast.cancel();

        // Make a new one
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // On start
    @Override
    protected void onStart()
    {
        super.onStart();
    }

    // On Resume
    @Override
    protected void onResume()
    {
        super.onResume();

        // Get preferences
        getPreferences();

        // Start the audio thread 修改处
        udpserver.start();
    }

    // On pause
    @Override
    protected void onPause()
    {
        super.onPause();

        // Save preferences
        savePreferences();

        // Stop audio thread
        udpserver.stop();
    }

    // On stop
    @Override
    protected void onStop()
    {
        super.onStop();
    }

    // Get preferences
    void getPreferences()
    {
        // Load preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        // Set preferences
        if (udpserver != null)
        {
            udpserver.input =
                Integer.parseInt(preferences.getString(PREF_INPUT, "0"));
            //Integer.parseInt(String)就是将String字符类型数据转换为Integer整型数据。
        }

        screen = preferences.getBoolean(PREF_SCREEN, false);

        // Check screen
        if (screen)
        {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        else
        {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // Save preferences
    void savePreferences()
    {
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        // TODO
    }

    // Show alert
    void showAlert(int appName, int errorBuffer)
    {
        // Create an alert dialog builder
        AlertDialog.Builder builder =
            new AlertDialog.Builder(this);

        // Set the title, message and button
        builder.setTitle(appName);
        builder.setMessage(errorBuffer);
        builder.setNeutralButton(android.R.string.ok,
                                 new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog,
                                int which)
            {
                // Dismiss dialog
                dialog.dismiss();
            }
        });

        // Create the dialog
        AlertDialog dialog = builder.create();

        // Show it
        dialog.show();
    }

     protected class Udpserver implements Runnable{
         protected int input;
         protected int srcport=8091;
         static final int sample= 20000;//分辨率
         protected Thread thread;
         protected short data[];
         protected int length=4096;
         DatagramSocket ds;
         protected Udpserver()
         {
             data=new short[4096];
         }
         protected void start()
         {
             // Start the thread
             thread = new Thread(this, "Udpserver");
             thread.start();
         }
         protected void stop()
         {
             Thread t = thread;
             thread = null;
             // Wait for the thread to exit
             while (t != null && t.isAlive())
                 Thread.yield();
             //线程让步,重新争夺线程
         }

         // Run
         @Override
         public void run()
         {
             processUdpserver();
         }

         protected void processUdpserver()
         {
             try{
                 Log.i("UDP","####"+"prepared");
                 DatagramSocket ds = new DatagramSocket(srcport);
                 //ds.setSoTimeout(3000);
                 byte[] buf = new byte[8192];
                 DatagramPacket dp = new DatagramPacket(buf,8192);
                 Log.i("UDP","##"+srcport);
                 while(thread!=null)
                 {
                     ds.receive(dp);
                     ByteBuffer.wrap(dp.getData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
                     scope.postInvalidate();
                 }
                 ds.close();
                 Log.i("UDP","####"+"closed");
             }catch (Exception e){
                 Log.i("UDP","####"+"Exception"+e);
             }

         }
     }
}
