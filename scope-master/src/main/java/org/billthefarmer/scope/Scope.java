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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

// Scope 画示波图像的关键类,继承视图类
public class Scope extends View
{
    private int width;   //可变视图示波器的宽与高
    private int height;

    private Path path;
    private Canvas cb;    //存储轨迹
    private Paint paint;
    private Bitmap bitmap;
    private Bitmap graticule;

    protected boolean storage;
    protected boolean clear;

    protected float step;
    protected float scale;
    protected float start;
    protected float index;  //触摸位置的X坐标

    protected float yscale;

    protected boolean points;
    protected MainActivity.Udpserver udpserver;

    // Scope
    public Scope(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // Create path and paint
        path = new Path();
        paint = new Paint();
    }

    // On size changed
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        // Get dimensions
        width = w;
        height = h;

        // Create a bitmap for trace storage
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cb = new Canvas(bitmap);

        // Create a bitmap for the graticule
        graticule = Bitmap.createBitmap(width, height,
                                        Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(graticule);

        // Black background
        canvas.drawColor(Color.BLACK);

        // Set up paint
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(255, 0, 63, 0));

        // Draw graticule画格子线 20像素,同短线
        for (int i = 0; i < width; i += MainActivity.SIZE)
            canvas.drawLine(i, 0, i, height, paint);
       //画竖线
        canvas.translate(0, height / 2);
        //坐标原点偏移
        for (int i = 0; i < height / 2; i += MainActivity.SIZE)
        {
            canvas.drawLine(0, i, width, i, paint);
            canvas.drawLine(0, -i, width, -i, paint);
        }
       //画横线
        // Draw the graticule on the bitmap
        cb.drawBitmap(graticule, 0, 0, null);
        //此处还未偏移 Bitmap：图片对象，left:偏移左边的位置，top： 偏移顶部的位置
        cb.translate(0, height / 2);
    }

    private int max;

    // On draw
    @Override
    protected void onDraw(Canvas canvas)
    {
        // Check for data 修改处1
        if ((udpserver == null) || (udpserver.data == null))
        {
            canvas.drawBitmap(graticule, 0, 0, null);
            Log.i("UDP","####"+"null");
            return;
            //只是画网格线
        }

        // Draw the graticule on the bitmap
        if (!storage || clear)
        {   //对比前面相似语句,注意不同的画布,cb的坐标原点调整过
            cb.drawBitmap(graticule, 0, -height / 2, null);
            clear = false;
        }
        // Check for data 修改处2
        // Calculate x scale etc
        float xscale = (float)(2.0 / ((udpserver.sample / 100000.0) * scale));
        int xstart = Math.round(start);
        int xstep = Math.round((float)1.0 / xscale);
        int xstop = Math.round(xstart + ((float)width / xscale));

        if (xstop > udpserver.length)  ;//索引为3,length取2048
            xstop = (int)udpserver.length;//只显示大小为length或count的数据量

        // Calculate y scale

        if (max < 4096)
            max = 4096;

        yscale = (float)(max / (height / 2.0));

        max = 0;

        // Draw the trace
        path.rewind();
        path.moveTo(0, 0);

        if (xscale < 1.0)
        {
            for (int i = 0; i < xstop - xstart; i += xstep) //先显示宽度的大小,相对索引而言的
            {
                if (max < Math.abs(udpserver.data[i + xstart]))
                    max = Math.abs(udpserver.data[i + xstart]); //max代指data数组的最大值

                float x = (float)i * xscale;
                float y = -(float)udpserver.data[i + xstart] / yscale;
                path.lineTo(x, y);
            }
        }

        else
        {
            for (int i = 0; i < xstop - xstart; i++)//递增方式改变
            {
                if (max < Math.abs(udpserver.data[i + xstart]))
                    max = Math.abs(udpserver.data[i + xstart]);

                float x = (float)i * xscale;
                float y = -(float)udpserver.data[i + xstart] / yscale;
                path.lineTo(x, y);

                // Draw points at max resolution
                if (points)
                {
                    path.addRect(x - 2, y - 2, x + 2, y + 2, Path.Direction.CW);
                    //以该点周围画正方形
                    path.moveTo(x, y);
                }
            }
        }

        // Green trace 绘制轨迹在画布上
        paint.setColor(Color.GREEN);
        paint.setAntiAlias(true);
        cb.drawPath(path, paint);

        // Draw index
        if (index > 0 && index < width)
        {
            // Yellow index
            paint.setColor(Color.YELLOW);

            paint.setAntiAlias(false);
            //原点始终在中间
            cb.drawLine(index, -height / 2, index, height / 2, paint);//绘制上下竖线
            //标定了两个值
            paint.setAntiAlias(true);
            paint.setTextSize(height / 48);
            paint.setTextAlign(Paint.Align.LEFT);

            // Get value
            int i = Math.round(index / xscale);
            if (i + xstart < udpserver.length)//索引控制
            {
                float y = -udpserver.data[i + xstart] / yscale;

                // Draw value

                String s = String.format(Locale.getDefault(), "%3.2f",
                                         udpserver.data[i + xstart] / 32768.0);
                cb.drawText(s, index, y, paint);
            }

            paint.setTextAlign(Paint.Align.CENTER);
                 //index代表X轴位置
            // Draw time value
            if (scale < 100.0)
            {
                String s = String.format(Locale.getDefault(),
                                         (scale < 1.0) ? "%3.3f" :
                                         (scale < 10.0) ? "%3.2f" : "%3.1f",
                                         (start + (index * scale)) /
                                         MainActivity.SMALL_SCALE);
                cb.drawText(s, index, height / 2, paint);//记得初始坐标原点位置,原本处在中间
            }

            else
            {
                String s = String.format(Locale.getDefault(), "%3.3f",
                                         (start + (index * scale)) /
                                         MainActivity.LARGE_SCALE);
                cb.drawText(s, index, height / 2, paint);
            }
        }

        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    // On touch event
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x = event.getX();
        float y = event.getY();

        // Set the index from the touch dimension
        switch (event.getAction())
        {
        case MotionEvent.ACTION_DOWN:
            index = x;
            break;

        case MotionEvent.ACTION_MOVE:
            index = x;
            break;

        case MotionEvent.ACTION_UP:
            index = x;
            break;
        }

        return true;
    }
}
