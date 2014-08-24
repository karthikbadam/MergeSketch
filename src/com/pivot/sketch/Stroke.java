package com.pivot.sketch;

import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.collect.Lists;

public class Stroke implements Parcelable{
	
	private static final float TOUCH_TOLERANCE = 3;
	public Path mPath;
	public float strokeWidth;
	public Paint mPaint;
	public Style strokeType;
	
	TouchPoint startPoint;
	TouchPoint endPoint;
	
	public int color;
	
	public List<TouchPoint> points;
	
	private int numberOfPoints = 0;

	public Stroke() {
		
		points = Lists.newArrayList();

		mPath = new Path();
		strokeWidth = 4;
		color = Color.BLACK;
		
		mPaint = new Paint();
		mPaint.setStrokeWidth(strokeWidth);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(color);
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);

	}

	public void setColor(int color) {
		this.color = color;
		mPaint.setColor(color);
	}

	public void setStrokeType(Style strokeType) {
		this.strokeType = strokeType;
		System.out.println("Setting Stroke type "+strokeType);
		mPaint.setStyle(strokeType);
	}

	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
		mPaint.setStrokeWidth(strokeWidth);
	}

	public void setPath(Path mPath) {
		this.mPath = mPath;
	}

	public void buildStroke() {
		
		if (points.size() != 1) {
			float temp_x, temp_y;
			mPath.moveTo(points.get(0).x,points.get(0).y);
			temp_x = points.get(0).x;
			temp_y = points.get(0).y;
			float x, y;
			float dx, dy;
			for (int i = 1; i < points.size()-1; i++) {
				x = points.get(i).x;
				y = points.get(i).y;
				dx = Math.abs(x - temp_x);
				dy = Math.abs(y - temp_y);
				if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
					mPath.quadTo(temp_x, temp_y, (x + temp_x) / 2, (y + temp_y) / 2);
					temp_x = x;
					temp_y = y;
				}
			}
			mPath.lineTo(points.get(points.size()-1).x, points.get(points.size()-1).y);
		} else {
			mPath.addCircle(points.get(0).x, points.get(0).y,
					(float) Math.sqrt(strokeWidth / Math.PI),
					Path.Direction.CCW);
		}
	}
//	private void buildStroke(int number_of_points) {
//		float temp_x, temp_y;
//		mPath.moveTo(startX, startY);
//		temp_x = startX;
//		temp_y = startY;
//		float x, y;
//		float dx, dy;
//		for (int i = 1; i < number_of_points; i++) {
//			if (xValues.size() > number_of_points) {
//				x = xValues.get(i);
//				y = yValues.get(i);
//				dx = Math.abs(x - temp_x);
//				dy = Math.abs(y - temp_y);
//				if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
//					mPath.quadTo(temp_x, temp_y, (x + temp_x) / 2,
//							(y + temp_y) / 2);
//					temp_x = x;
//					temp_y = y;
//				}
//			}
//		}
//		mPath.lineTo(endX, endY);
//	}

//	public void undo() {
//		numberOfPoints-=1;
//		buildStroke(numberOfPoints);
//	}
//	
//	public void redo() {
//		numberOfPoints+=1;
//		buildStroke(numberOfPoints);
//	}
	
	public void addTouchPoint(float x, float y, boolean is_end) {
		boolean is_start = false;
		if (numberOfPoints == 0) {
			is_start = true;
			startPoint = new TouchPoint(x, y, true, is_end);
		}
		
		if (!is_end) {
			TouchPoint current_point = new TouchPoint(x, y, is_start, is_end);
			points.add(current_point);
			numberOfPoints++;
		} else {
			endPoint = new TouchPoint(x, y, is_start, true);
		}
	}	
	
	public Path getPath() {
		return mPath;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeTypedList(points);
		dest.writeFloat(strokeWidth);
		dest.writeInt(color);
	
	}
	
	public static final Parcelable.Creator<Stroke> CREATOR = new Parcelable.Creator<Stroke>() {

        @Override
        public Stroke createFromParcel(Parcel in) {
            return new Stroke(in);
        }

        @Override
        public Stroke[] newArray(int size) {
            return new Stroke[size];
        }
    };
    
	public Stroke(Parcel in) {
		points = Lists.newArrayList();
		in.readTypedList(points, TouchPoint.CREATOR);
		strokeWidth = in.readFloat();
		color = in.readInt();
		
		mPaint = new Paint();
		mPaint.setStrokeWidth(strokeWidth);
		mPaint.setColor(color);
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStyle(Style.STROKE);
		
		mPath = new Path();
	}
	
}
