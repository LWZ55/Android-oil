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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

// YScale 时域图的Y轴显示控制
public class YScale extends View  //视图类
{
    private static final int WIDTH_FRACTION = 24; //宽度分割比例

    private int width;  //视图的宽与高
    private int height;

    protected float index;  //触摸的Y位置(相对于中间原点)

    private Matrix matrix;   //变换矩阵
    private Paint paint;
    private Path thumb;

    // YScale
    public YScale(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // Create paint
        matrix = new Matrix();
        paint = new Paint();
    }

    // onMeasure 实质设置子视图大小
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get offered dimension 模式默认为exactly
        int h = MeasureSpec.getSize(heightMeasureSpec);

        // Set wanted dimensions
        setMeasuredDimension(h / WIDTH_FRACTION, h);
    }

    // onSizeChanged
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        // Get actual dimensions
        width = w;  //宽与高均为子视图的宽高
        height = h;

        // Create a path for the thumb
        thumb = new Path();

        thumb.moveTo(-1, -1);   //绘制五边形
        thumb.lineTo(-1, 1);
        thumb.lineTo(1, 1);
        thumb.lineTo(2, 0);
        thumb.lineTo(1, -1);
        thumb.close();       //闭合

        // Create a matrix to scale the thumb,按比例放缩
        matrix.setScale(width / 4, width / 4);//设置矩阵参数

        // Scale the thumb
        thumb.transform(matrix); //五边形缩放
    }

    // onDraw
    @Override
    protected void onDraw(Canvas canvas)
    {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        canvas.translate(0, height / 2);  //重点,变换坐标原点为(0,h/2)

        // Draw scale ticks
        for (int i = 0; i < height / 2; i += MainActivity.SIZE)
        {
            canvas.drawLine(width * 2 / 3, i, width, i, paint);
            canvas.drawLine(width * 2 / 3, -i, width, -i, paint);
        }
        //宽度为新视图宽度
        //画短线.20(size值)像素一个短线

        for (int i = 0; i < height / 2; i += MainActivity.SIZE * 5)
        {
            canvas.drawLine(width / 3, i, width, i, paint);
            canvas.drawLine(width / 3, -i, width, -i, paint);
        }
        //画长线.100像素一长线

        // Draw sync level thumb if not zero
        if (index != 0)
        {
            canvas.translate(width / 3, index);  //坐标原点到触摸所在位置
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(thumb, paint); //画五边形
        }
    }

    // On touch event 触摸位置获取
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x = event.getX();
        float y = event.getY();

        // Set the index from the touch dimension
        switch (event.getAction())
        {
        case MotionEvent.ACTION_DOWN:
            index = y - (height / 2);  //转化为以中间为原点的坐标表示
            break;

        case MotionEvent.ACTION_MOVE:
            index = y - (height / 2);
            break;

        case MotionEvent.ACTION_UP:
            index = y - (height / 2);
            break;
        }

        invalidate();
        return true;
    }
}
