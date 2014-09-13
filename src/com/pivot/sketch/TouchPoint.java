package com.pivot.sketch;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class TouchPoint implements Parcelable {
	float INFINITY = 1000000;
	public float x, y;
	public float leftDistance = INFINITY;
	public boolean is_start = false;
	public boolean is_end = false;
	int index = 0; 
	
	public TouchPoint(float x, float y, boolean is_start, boolean is_end) {
		this.x = x;
		this.y = y;
		this.is_start = is_start;
		this.is_end = is_end;
	}

	public void putX(float x) {
		this.x = x;
	}

	public void putY(float y) {
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float distance(TouchPoint p) {
		return (float) Math.sqrt(Math.pow(x - p.x, 2) + Math.pow(y - p.y, 2));
	}

	public void setLeftDistance(float distance) {
		leftDistance = distance;
	}

	// parcel methods!!!
	public TouchPoint(Parcel read) {
		this.x = read.readFloat();
		this.y = read.readFloat();
		this.leftDistance = read.readFloat();
		int start = read.readInt();
		int end = read.readInt();

		is_start = false;
		is_end = false;
		if (start == 1) {
			is_start = true;
		}
		if (end == 1) {
			is_end = true;
		}
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeFloat(x);
		dest.writeFloat(y);
		dest.writeFloat(leftDistance);
		dest.writeInt(is_start ? 1 : 0);
		dest.writeInt(is_end ? 1 : 0);
	}

	public static final Parcelable.Creator<TouchPoint> CREATOR = new Parcelable.Creator<TouchPoint>() {

		@Override
		public TouchPoint createFromParcel(Parcel source) {
			return new TouchPoint(source);
		}

		@Override
		public TouchPoint[] newArray(int size) {
			return new TouchPoint[size];
		}
	};

	public void setIndex(int count) {
		this.index = count;
	}
	
	
	//TODO check point-in-polygon!
	public Boolean checkPIP(ArrayList<TouchPoint> points) {
		  boolean result = false;
	      for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
	        if ((points.get(i).y > this.y) != (points.get(j).y > this.y) &&
	            (this.x < (points.get(j).x - points.get(i).x) * (this.y - points.get(i).y) / (points.get(j).y-points.get(i).y) + points.get(i).x)) {
	          result = !result;
	         }
	      }
	      return result;
	}

}
