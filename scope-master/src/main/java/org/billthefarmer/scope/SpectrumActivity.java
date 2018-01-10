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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

// SpectrumActivity 用于频谱显示控制
public class SpectrumActivity extends Activity
    implements View.OnClickListener
{
    private static final String PREF_INPUT = "pref_input";
    private static final String PREF_FILL = "pref_fill";
    private static final String PREF_SCREEN = "pref_screen";

    private Spectrum spectrum;
    private FreqScale scale;
    private TextView text;
    private Toast toast;
    private Unit unit;

    private MenuItem lockItem;

    //private Audio audio;
    private Udpserver udpserver;
    private boolean screen;

    // On create
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spectrum);

        spectrum = (Spectrum)findViewById(R.id.spectrum);
        scale = (FreqScale)findViewById(R.id.freqscale);
        unit = (Unit)findViewById(R.id.specunit);

        if (unit != null)
            unit.scale = 0;

        if (spectrum != null)
            spectrum.setOnClickListener(this);

        // Enable back navigation on action bar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.spectrum);

        actionBar.setCustomView(R.layout.text);
        actionBar.setDisplayShowCustomEnabled(true);
        text = (TextView)actionBar.getCustomView();

        udpserver = new Udpserver();

        if (spectrum != null)
            spectrum.udpserver = udpserver;

        if (scale != null)
            scale.udpserver = udpserver;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.spectrum, menu);

        lockItem = menu.findItem(R.id.action_lock);

        return true;
    }

    // On options item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;

        // Get id
        int id = item.getItemId();
        switch (id)
        {
        // Home
        case android.R.id.home:
            finish();
            break;

        // Lock
        case R.id.action_lock:
            udpserver.lock = !udpserver.lock;
            item.setIcon(udpserver.lock ? R.drawable.lock_checked :
                         R.drawable.ic_action_secure);
            showToast(udpserver.lock ? R.string.lock_on : R.string.lock_off);
            break;

        // Help
        case R.id.action_help:
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            break;

        // Settings
        case R.id.action_settings:
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            break;

        default:
            return false;
        }

        return true;
    }

    // On click
    @Override
    public void onClick(View v)
    {
        // Get id
        int id = v.getId();
        switch (id)
        {
        // Spectrum
        case R.id.spectrum:
            udpserver.lock = !udpserver.lock;
            if (lockItem != null)
                lockItem.setIcon(udpserver.lock ?
                                 R.drawable.lock_checked :
                                 R.drawable.ic_action_secure);
            showToast(udpserver.lock ? R.string.lock_on : R.string.lock_off);
            break;
        }
    }

    // Show toast.
    void showToast(int key)
    {
        Resources resources = getResources();
        String text = resources.getString(key);

        showToast(text);
    }

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

    // On Resume
    @Override
    protected void onResume()
    {
        super.onResume();

        // Get preferences
        getPreferences();

        // Start the audio thread
        udpserver.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Save preferences
        savePreferences();

        // Stop  thread
        udpserver.stop();
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
            udpserver.fill = preferences.getBoolean(PREF_FILL, true);
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

        protected boolean lock;
        protected boolean fill;

        // Data
        protected double frequency;
        protected double fps;

        private static final int OVERSAMPLE = 4;
        private static final int SAMPLES = 8192;
        private static final int RANGE = SAMPLES / 2;
        private static final int STEP = SAMPLES / OVERSAMPLE;

        private static final int N = 4;
        private static final int M = 16;

        private static final double MIN = 0.5;
        private static final double expect = 2.0 * Math.PI * STEP / SAMPLES;

        private long counter;
        private double buffer[];

        private double xr[];
        private double xi[];

        protected double xa[];

        private double xp[];
        private double xf[];

        DatagramSocket ds;
        protected int input;
        static final int sample= 50000;//分辨率
        protected Thread thread;
        protected short data[];
        protected int length=2048;
        protected Udpserver()
        {
            data = new short[STEP];
            buffer = new double[SAMPLES];
            xr = new double[SAMPLES];
            xi = new double[SAMPLES];
            xa = new double[RANGE];
            xp = new double[RANGE];
            xf = new double[RANGE];
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
            // Calculate fps
            fps = (double)sample / SAMPLES;
            // Max data
            double dmax = 0.0;
            try{
                Log.i("UDP","####"+"prepared");
                DatagramSocket ds = new DatagramSocket(8093);
                ds.setSoTimeout(3000);
                byte[] buf = new byte[4096];
                DatagramPacket dpa = new DatagramPacket(buf,4096);
                while(thread!=null)
                {
                    ds.receive(dpa);
                    //Log.i("UDP","##"+dpa.getData().length);
                    try{
                        ByteBuffer.wrap(dpa.getData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);

                    }catch(Exception e){
                        Log.e("UDP","##REV"+e);
                    }

                    // Move the main data buffer up
                    System.arraycopy(buffer, STEP, buffer, 0, SAMPLES - STEP);

                    for (int i = 0; i < STEP; i++)
                        buffer[(SAMPLES - STEP) + i] = data[i];

                    // Maximum value
                    if (dmax < 4096.0)
                        dmax = 4096.0;

                    // Calculate normalising value
                    double norm = dmax;

                    dmax = 0.0;

                    // Copy data to FFT input arrays
                    for (int i = 0; i < SAMPLES; i++)
                    {
                        // Find the magnitude
                        if (dmax < Math.abs(buffer[i]))
                            dmax = Math.abs(buffer[i]);

                        // Calculate the window
                        double window =
                                0.5 - 0.5 * Math.cos(2.0 * Math.PI *
                                        i / SAMPLES);

                        // Normalise and window the input data
                        xr[i] = buffer[i] / norm * window;
                    }

                    // do FFT
                    fftr(xr, xi);

                    // Process FFT output
                    for (int i = 1; i < RANGE; i++)
                    {
                        double real = xr[i];
                        double imag = xi[i];

                        xa[i] = Math.hypot(real, imag);

                        // Do frequency calculation
                        double p = Math.atan2(imag, real);
                        double dp = xp[i] - p;

                        xp[i] = p;

                        // Calculate phase difference
                        dp -= i * expect;

                        int qpd = (int)(dp / Math.PI);

                        if (qpd >= 0)
                            qpd += qpd & 1;

                        else
                            qpd -= qpd & 1;

                        dp -=  Math.PI * qpd;

                        // Calculate frequency difference
                        double df = OVERSAMPLE * dp / (2.0 * Math.PI);

                        // Calculate actual frequency from slot frequency plus
                        // frequency difference and correction value
                        xf[i] = i * fps + df * fps;
                    }

                    // Do a full process run every N
                    if (++counter % N != 0)
                        continue;

                    // Check display lock
                    if (lock)
                        continue;

                    // Update spectrum
                    spectrum.postInvalidate();

                    // Update frequency and dB every M
                    if (counter % M != 0)
                        continue;

                    // Maximum FFT output
                    double max = 0.0;

                    // Find maximum value
                    for (int i = 1; i < RANGE; i++)
                    {
                        if (xa[i] > max)
                        {
                            max = xa[i];
                            frequency = xf[i];
                        }
                    }

                    // Level
                    double level = 0.0;

                    for (int i = 0; i < STEP; i++)
                        level += ((double)data[i] / 32768.0) *
                                ((double)data[i] / 32768.0);

                    level = Math.sqrt(level / STEP) * 2.0;

                    double dB = Math.log10(level) * 20.0;

                    if (dB < -80.0)
                        dB = -80.0;

                    // Update frequency and dB display
                    if (max > MIN)
                    {
                        final String s = String.format(Locale.getDefault(),
                                "%1.1fHz  %1.1fdB",
                                frequency, dB);
                        text.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                text.setText(s);
                            }
                        });
                    }

                    else
                    {
                        frequency = 0.0;
                        final String s = String.format(Locale.getDefault(),
                                "%1.1fdB", dB);
                        text.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                text.setText(s);
                            }
                        });
                    }
                }
                ds.close();
            }catch (Exception e){
                Log.i("UDP","####"+"Exception"+e);
            }

        }
        // Real to complex FFT, ignores imaginary values in input array
        private void fftr(double ar[], double ai[])
        {
            final int n = ar.length;
            final double norm = Math.sqrt(1.0 / n);

            for (int i = 0, j = 0; i < n; i++)
            {
                if (j >= i)
                {
                    double tr = ar[j] * norm;

                    ar[j] = ar[i] * norm;
                    ai[j] = 0.0;

                    ar[i] = tr;
                    ai[i] = 0.0;
                }

                int m = n / 2;
                while (m >= 1 && j >= m)
                {
                    j -= m;
                    m /= 2;
                }
                j += m;
            }

            for (int mmax = 1, istep = 2 * mmax; mmax < n;
                 mmax = istep, istep = 2 * mmax)
            {
                double delta = Math.PI / mmax;
                for (int m = 0; m < mmax; m++)
                {
                    double w = m * delta;
                    double wr = Math.cos(w);
                    double wi = Math.sin(w);

                    for (int i = m; i < n; i += istep)
                    {
                        int j = i + mmax;
                        double tr = wr * ar[j] - wi * ai[j];
                        double ti = wr * ai[j] + wi * ar[j];
                        ar[j] = ar[i] - tr;
                        ai[j] = ai[i] - ti;
                        ar[i] += tr;
                        ai[i] += ti;
                    }
                }
            }
        }
    }
}
