package com.pivot.sketch;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.collect.Lists;

public class Layer implements Parcelable {
	private List<Stroke> strokes;
	private List<TouchPoint> points; 
	
	public Layer() {
		strokes = Lists.newArrayList();
		points = Lists.newArrayList();
	}
	
	public void addStroke(Stroke stroke) {
		strokes.add(stroke);
	}
	
	public Stroke getStroke(int i){
		return strokes.get(i);
	}
	
	public void removeStroke(int i){
		strokes.remove(i);
	}
	
	public int numberOfStrokes() {
		return strokes.size(); 
	}
	
	public List<Stroke> getStrokes() {
		return strokes;
	}
	
	public void computePoints() {
		for (Stroke tempStroke: strokes) {
			points.addAll(tempStroke.points);
		}
	}
	
	public List<TouchPoint> getPoints() {
		return points;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeTypedList(strokes);
	}
	
	
	public Layer(Parcel in){
		strokes = Lists.newArrayList();
    	in.readTypedList(strokes, Stroke.CREATOR);
    }
	
	
	public static final Parcelable.Creator<Layer> CREATOR = new Parcelable.Creator<Layer>() {

        @Override
        public Layer createFromParcel(Parcel in) {
            return new Layer(in);
        }

        @Override
        public Layer[] newArray(int size) {
            return new Layer[size];
        }
    };
    
    
}
