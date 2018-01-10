////////////////////////////////////////////////////////////////////////////////
//
//  Scope - An Android scope written in Java.
//
//  Copyright (C) 2014	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

// XScale X轴,视图类
public class XScale extends View  //视图类
{
    private static final int HEIGHT_FRACTION = 32; //分割比例
   //以下三个变量控制横坐标显示
    protected float step;  //初始值为10
    protected float scale; //初始值为1
    protected float start; //初始值为0

    private int width;
    private int height;  //自定义视图的宽与高

    private Paint paint;

    // XScale
    public XScale(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // Create paint
        paint = new Paint();

        // Set initial values
        start = 0;
        scale = 1;
        step = 10;
    }

    // onMeasure 自定义视图
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get offered dimension
        int w = MeasureSpec.getSize(widthMeasureSpec);

        // Set wanted dimensions
        setMeasuredDimension(w, w / HEIGHT_FRACTION); //横条状
    }

    // onSizeChanged 在ondraw前启动
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        // Get actual dimensions
        width = w;
        height = h; //子视图的宽与高
    }

    // onDraw
    @Override
    protected void onDraw(Canvas canvas)
    {
        // Set up paint
        paint.setStrokeWidth(2);
        paint.setColor(Color.BLACK);

        // Draw ticks
        for (int i = 0; i < width; i += MainActivity.SIZE)
            canvas.drawLine(i, 0, i, height / 4, paint);
          //从子视图最上往下,每20像素画短线
        for (int i = 0; i < width; i += MainActivity.SIZE * 5)
            canvas.drawLine(i, 0, i, height / 3, paint);
        //100像素画长线
        // Set up paint
        paint.setAntiAlias(true);
        paint.setTextSize(height * 2 / 3); //以SP为单位,设置字体大小
        paint.setTextAlign(Paint.Align.CENTER);
        //设置文本显示形式
        //canvas.drawText(text, x, y, paint)，第一个参数是我们需要绘制的文本
        //x默认是这个字符串的左边在屏幕的位置
        // 如果设置了paint.setTextAlign(Paint.Align.CENTER);那就是字符的中心，
        // y是指定这个字符baseline在屏幕上的位置，
        // Draw scale 初始值为1
        if (scale < 100.0)
        {
            canvas.drawText("ms", 0, height - (height / 6), paint);

            for (int i = MainActivity.SIZE * 10; i < width;
                    i += MainActivity.SIZE * 10)
            {
                String s = String.format(Locale.getDefault(),
                                         "%1.1f", (start + (i * scale)) /
                                         MainActivity.SMALL_SCALE); //scale初始值为1,start为0
                canvas.drawText(s, i, height - (height / 8), paint);
            }
        }

        else
        {
            canvas.drawText("sec", 0, height - (height / 6), paint);

            for (int i = MainActivity.SIZE * 10; i < width;
                    i += MainActivity.SIZE * 10)
            {
                String s = String.format(Locale.getDefault(),
                                         "%1.1f", (start + (i * scale)) /
                                         MainActivity.LARGE_SCALE); //区别在被除数因子
                canvas.drawText(s, i, height - (height / 8), paint);
            }
        }
    }
}
