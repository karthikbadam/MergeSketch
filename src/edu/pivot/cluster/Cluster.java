package edu.pivot.cluster;

import java.util.Iterator;
import java.util.List;

import android.graphics.Path;
import android.graphics.RectF;

import com.google.common.collect.Lists;
import com.pivot.sketch.Layer;
import com.pivot.sketch.Stroke;
import com.pivot.sketch.TouchPoint;

public class Cluster {

	public Cluster parent = null;

	public Cluster leftChild = null;
	public Cluster rightChild = null;

	public float startIndex;
	public float endIndex;

	public BoundingBox boundingbox;

	float leftDistance;

	public int historyIndex = -1;

	private List<Stroke> strokes;
	private List<TouchPoint> points;

	Layer layer;

	public float area;

	RectF boundingRect;
	
	TouchPoint centroid = null; 
	
	int size; 

	public Cluster(Cluster cluster) {

		this.parent = cluster.parent;
		this.leftChild = cluster.leftChild;
		this.rightChild = cluster.rightChild;
		this.startIndex = cluster.startIndex;
		this.endIndex = cluster.endIndex;
		this.boundingbox = new BoundingBox();
		this.boundingbox.leftBoundary = cluster.boundingbox.leftBoundary;
		this.boundingbox.rightBoundary = cluster.boundingbox.rightBoundary;
		this.boundingbox.upperBoundary = cluster.boundingbox.upperBoundary;
		this.boundingbox.bottomBoundary = cluster.boundingbox.bottomBoundary;
		
		this.leftDistance = cluster.leftDistance;
		this.historyIndex = cluster.historyIndex;
		this.strokes = Lists.newArrayList();
		this.points = Lists.newArrayList();
		this.layer = cluster.layer;
		this.area = cluster.area;
		
	}

	public Cluster(float leftDistance) {
		super();
		this.leftDistance = leftDistance;
		boundingbox = new BoundingBox();
		strokes = Lists.newArrayList();
		points = Lists.newArrayList();
	}

	public Cluster(float leftDistance, int historyIndex) {
		super();
		this.leftDistance = leftDistance;
		this.historyIndex = historyIndex;
		boundingbox = new BoundingBox();
		strokes = Lists.newArrayList();
		points = Lists.newArrayList();
	}

	public void setStartEnd(int historyIndex) {
		startIndex = historyIndex;
		endIndex = historyIndex;
	}

	public void setOriginalLayer(Layer layer) {
		this.layer = layer;
	}

	public List<TouchPoint> sliceLayerForTouchPoints() {
		layer.computePoints();
		List<TouchPoint> all = layer.getPoints(); 
		points.clear();
		for (int i = (int) startIndex; i <= endIndex; i++) {
			
			TouchPoint tempPoint = all.get(i);
			TouchPoint p = new TouchPoint(tempPoint.x, tempPoint.y, tempPoint.is_start, tempPoint.is_end);
			points.add(p);
		}
//		this.points = layer.getPoints().subList((int) startIndex,
//				(int) endIndex);
		this.size = points.size();
		return points;
	}

	public void addStroke(Stroke stroke) {
		strokes.add(stroke);
	}

	public Stroke getStroke(int i) {
		return strokes.get(i);
	}

	public void removeStroke(int i) {
		strokes.remove(i);
	}

	public int numberOfStrokes() {
		return strokes.size();
	}

	public List<Stroke> getStrokes() {
		return strokes;
	}

	public List<TouchPoint> getPoints() {
		return points;
	}

	private TouchPoint getCentroid() {
		float sumX = 0, sumY = 0;
		for (TouchPoint point : points) {
			sumX += point.getX();
			sumY += point.getY();
		}

		centroid = new TouchPoint(sumX / size, sumY
				/ size, false, false);
		return centroid;
	}

	public RectF getBoundingRect() {
		boundingRect = new RectF(this.boundingbox.leftBoundary,
				this.boundingbox.upperBoundary, this.boundingbox.rightBoundary,
				this.boundingbox.bottomBoundary);

		return boundingRect;
	}

	public Boolean scalePoints(float scaleFactor) {

		// TODO compute centroid -- translate -- scale -- translate back!
		if (centroid == null)
			centroid = getCentroid();

		try {
			float minX = 10000, maxX = -100000, minY = 100000, maxY = -100000;

			for (int i = 0; i < size; i++) {
				TouchPoint p = points.get(i);
				p.x = p.x - centroid.x;
				p.x = p.x * scaleFactor;
				p.y = p.y - centroid.y;
				p.y = p.y * scaleFactor;

				p.x = p.x + centroid.x;
				p.y = p.y + centroid.y;
				
				if (minX > p.x)
					minX = p.x;

				if (minY > p.y)
					minY = p.y;

				if (maxX < p.x)
					maxX = p.x;

				if (maxY < p.y)
					maxY = p.y;
			}


			boundingbox.leftBoundary = minX;
			boundingbox.rightBoundary = maxX;
			boundingbox.upperBoundary = minY;
			boundingbox.bottomBoundary = maxY;

		} catch (Exception e) {

			e.printStackTrace();
		}

		return true;
	}

	public Boolean rotatePoints(double angle) {
		// TODO compute centroid -- rotation matrix -- translate -- rotate --
		// translate back!

		if (centroid == null)
			centroid = getCentroid();

		try {
			float minX = 10000, maxX = -100000, minY = 100000, maxY = -100000;

			for (int i = 0; i < size; i++) {
				TouchPoint p = points.get(i);
				p.x = p.x - centroid.x;
				p.y = p.y - centroid.y;

				// rotate wrt centroid
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);

				float tempX = p.x * cos - p.y * sin;
				float tempY = p.x * sin + p.y * cos;

				p.x = tempX + centroid.x;
				p.y = tempY + centroid.y;
				
				if (minX > p.x)
					minX = p.x;

				if (minY > p.y)
					minY = p.y;

				if (maxX < p.x)
					maxX = p.x;

				if (maxY < p.y)
					maxY = p.y;

			}
			
			boundingbox.leftBoundary = minX;
			boundingbox.rightBoundary = maxX;
			boundingbox.upperBoundary = minY;
			boundingbox.bottomBoundary = maxY;
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}

	public Boolean translate(float pixelOffsetX, float pixelOffsetY) {

		try {
			 
			for (int i = 0; i < size; i++) {
				TouchPoint p = points.get(i);
				p.x = p.x + pixelOffsetX;
				p.y = p.y + pixelOffsetY;
			}

			boundingbox.leftBoundary += pixelOffsetX;
			boundingbox.rightBoundary += pixelOffsetX;
			boundingbox.upperBoundary += pixelOffsetY;
			boundingbox.bottomBoundary += pixelOffsetY;
			
			if (centroid == null)
				centroid = getCentroid();
			
			centroid.x += pixelOffsetX;
			centroid.y += pixelOffsetY;
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		return true;
	}

	public float getArea() {
		this.area = Math
				.abs((boundingbox.rightBoundary - boundingbox.leftBoundary)
						* (boundingbox.bottomBoundary - boundingbox.upperBoundary));
		return area;
	}

	Path tempPath;
	float temp_x = 0, temp_y = 0;

	public void buildStrokes(float viewScale) {
		tempPath = new Path();
		strokes = Lists.newArrayList();

		try {
			int i = 0;
			Iterator<TouchPoint> iter = points.iterator();
			while (iter.hasNext()) {

				// for (int i = 0; i < points.size(); i++) {

				TouchPoint point = iter.next();
				float x = (float) viewScale * point.getX();
				float y = (float) viewScale * point.getY();

				if (i == 0 || point.is_start) {
					tempPath = new Path();
					tempPath.moveTo(x, y);
					temp_x = x;
					temp_y = y;

				} else if (point.is_end
						|| i == size - 1
						|| (i < size - 1 && points.get(i + 1).is_start == true)) {
					tempPath.lineTo(x, y);
					Stroke currentStroke = new Stroke();
					currentStroke.mPath = tempPath;
					currentStroke.setStrokeWidth(viewScale);
					this.addStroke(currentStroke);
					//System.out.println("ADDING STROKE TO FINAL!!!");

				} else if (!point.is_end) {

					tempPath.quadTo(temp_x, temp_y, (x + temp_x) / 2,
							(y + temp_y) / 2);
					temp_x = x;
					temp_y = y;
					//System.out.println(temp_x + ", " + temp_y);

				}

				i++;
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

}
