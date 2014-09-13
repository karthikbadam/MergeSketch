package edu.pivot.cluster;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.os.Handler;

import com.google.common.collect.Lists;
import com.pivot.sketch.Layer;
import com.pivot.sketch.Stroke;
import com.pivot.sketch.TouchPoint;

public class Aggregator {
	Layer layer;

	int totalHistorySize = 0;

	Cluster rootCluster = new Cluster(0); 
	
	public ArrayList<Cluster> clusters;
	public ArrayList<Cluster> backupClusters;
	
	public ArrayList<BoundingBox> boxes;

	//ProgressDialog dialog; 
	
	public Aggregator(Layer layer) {
		this.layer = layer;

		clusters = Lists.newArrayList();
		boxes = Lists.newArrayList();

		backupClusters = Lists.newArrayList();
		
		//this.dialog = dialog; 
		
	}

	Cluster newCluster;
	
	public ArrayList<Cluster> aggregate(int clusterNumber) {
		initLeafCluster();
		
		layer.computePoints();
		
		final int historySize = layer.getPoints().size(); 

		for (int i = 0; i < historySize - clusterNumber; i++) {
			
			int minIndex = getMinDistanceIndex();
			
			newCluster = new Cluster(
					clusters.get(minIndex - 1).leftDistance);
			
			newCluster.leftChild = clusters.get(minIndex - 1);
			newCluster.rightChild = clusters.get(minIndex);
			
			newCluster.leftChild.parent = newCluster; 
			newCluster.rightChild.parent = newCluster; 
			
			newCluster.startIndex = newCluster.leftChild.startIndex;
			newCluster.endIndex = newCluster.rightChild.endIndex;
			
			//get the points in the new cluster
			newCluster.setOriginalLayer(layer);

			/* bounding box */
			newCluster.boundingbox.leftBoundary = newCluster.leftChild.boundingbox.leftBoundary;
			newCluster.boundingbox.rightBoundary = newCluster.leftChild.boundingbox.rightBoundary;
			newCluster.boundingbox.upperBoundary = newCluster.leftChild.boundingbox.upperBoundary;
			newCluster.boundingbox.bottomBoundary = newCluster.leftChild.boundingbox.bottomBoundary;

			if (newCluster.leftChild.boundingbox.leftBoundary > newCluster.rightChild.boundingbox.leftBoundary)
				newCluster.boundingbox.leftBoundary = newCluster.rightChild.boundingbox.leftBoundary;
			if (newCluster.leftChild.boundingbox.rightBoundary < newCluster.rightChild.boundingbox.rightBoundary)
				newCluster.boundingbox.rightBoundary = newCluster.rightChild.boundingbox.rightBoundary;
			if (newCluster.leftChild.boundingbox.upperBoundary > newCluster.rightChild.boundingbox.upperBoundary)
				newCluster.boundingbox.upperBoundary = newCluster.rightChild.boundingbox.upperBoundary;
			if (newCluster.leftChild.boundingbox.bottomBoundary < newCluster.rightChild.boundingbox.bottomBoundary)
				newCluster.boundingbox.bottomBoundary = newCluster.rightChild.boundingbox.bottomBoundary;

			clusters.set(minIndex - 1, newCluster);
			clusters.remove(minIndex);
			
//			if ( i < totalHistorySize - clusterNumber) {
//			
//				clusters.set(minIndex - 1, newCluster);
//				clusters.remove(minIndex);
//			
//			} else if ( i == totalHistorySize - 2) {
//				
//				rootCluster = newCluster; 
//			}
			
//			updateBarHandler.post(new Runnable() {
//
//                public void run() {
//
//                	dialog.incrementProgressBy(100/historySize);
//
//                  }
//
//              });
			

		}
		
		backupClusters.addAll(clusters);
		
		for (int i = historySize - clusterNumber; i < historySize - 1; i++) {
			int minIndex = getMinDistanceIndex();

			
			newCluster = new Cluster(
					clusters.get(minIndex - 1).leftDistance);
			
			newCluster.leftChild = clusters.get(minIndex - 1);
			newCluster.rightChild = clusters.get(minIndex);
			
			newCluster.leftChild.parent = newCluster; 
			newCluster.rightChild.parent = newCluster; 
			
			newCluster.startIndex = newCluster.leftChild.startIndex;
			newCluster.endIndex = newCluster.rightChild.endIndex;
			
			//get the points in the new cluster
			newCluster.setOriginalLayer(layer);

			/* bounding box */
			newCluster.boundingbox.leftBoundary = newCluster.leftChild.boundingbox.leftBoundary;
			newCluster.boundingbox.rightBoundary = newCluster.leftChild.boundingbox.rightBoundary;
			newCluster.boundingbox.upperBoundary = newCluster.leftChild.boundingbox.upperBoundary;
			newCluster.boundingbox.bottomBoundary = newCluster.leftChild.boundingbox.bottomBoundary;

			if (newCluster.leftChild.boundingbox.leftBoundary > newCluster.rightChild.boundingbox.leftBoundary)
				newCluster.boundingbox.leftBoundary = newCluster.rightChild.boundingbox.leftBoundary;
			if (newCluster.leftChild.boundingbox.rightBoundary < newCluster.rightChild.boundingbox.rightBoundary)
				newCluster.boundingbox.rightBoundary = newCluster.rightChild.boundingbox.rightBoundary;
			if (newCluster.leftChild.boundingbox.upperBoundary > newCluster.rightChild.boundingbox.upperBoundary)
				newCluster.boundingbox.upperBoundary = newCluster.rightChild.boundingbox.upperBoundary;
			if (newCluster.leftChild.boundingbox.bottomBoundary < newCluster.rightChild.boundingbox.bottomBoundary)
				newCluster.boundingbox.bottomBoundary = newCluster.rightChild.boundingbox.bottomBoundary;

			clusters.set(minIndex - 1, newCluster);
			clusters.remove(minIndex);
			
		}
		
		
		return backupClusters;
	}

	public void initLeafCluster() {
		List<Stroke> strokes = layer.getStrokes();
		TouchPoint last_point = null;
		TouchPoint point;

		int count = 0;
		for (int i = 0; i < strokes.size(); i++) {
			List<TouchPoint> points = strokes.get(i).points;

			point = points.get(0);
			if (i > 0) {
				point.setLeftDistance(point.distance(last_point));
			}

			Cluster newCluster = new Cluster(point.leftDistance, count);
			newCluster.setStartEnd(count);
			point.setIndex(count);
			newCluster.boundingbox.leftBoundary = point.getX();
			newCluster.boundingbox.rightBoundary = point.getX();
			newCluster.boundingbox.upperBoundary = point.getY();
			newCluster.boundingbox.bottomBoundary = point.getY();
			
			//get the points in the new cluster
			newCluster.setOriginalLayer(layer);

			clusters.add(newCluster);
			
			count++;

			for (int j = 1; j < points.size() - 1; j++) {
				point = points.get(j);
				point.setLeftDistance(point.distance(points.get(j-1)));
				newCluster = new Cluster(point.leftDistance, count);
				newCluster.setStartEnd(count);
				point.setIndex(count);
				
				newCluster.boundingbox.leftBoundary = point.getX();
				newCluster.boundingbox.rightBoundary = point.getX();
				newCluster.boundingbox.upperBoundary = point.getY();
				newCluster.boundingbox.bottomBoundary = point.getY();
				clusters.add(newCluster);
				count++;
			}
			
			last_point = points.get(points.size() - 1);
			if (points.size() > 1)
				last_point.setLeftDistance(last_point.distance(points.get(points
					.size() - 2)));
			newCluster = new Cluster(last_point.leftDistance, count);
			newCluster.setStartEnd(count);
			last_point.setIndex(count);
			
			newCluster.boundingbox.leftBoundary = point.getX();
			newCluster.boundingbox.rightBoundary = point.getX();
			newCluster.boundingbox.upperBoundary = point.getY();
			newCluster.boundingbox.bottomBoundary = point.getY();
			clusters.add(newCluster);
			count++;

		}

		totalHistorySize = count;

	}

	// Fastest way to find minimum!!!
	public int getMinDistanceIndex() {

		// Create a copy of the original data structure
		int index = 0;
		double min = 10000;

		int i = 0;
		for (Cluster tempCluster : clusters) {
			// *************inBoundary not including the lower boundary
			if (tempCluster.leftDistance < min) {
				min = tempCluster.leftDistance;
				index = i;
			}
			i++;

		}
		return index;

	}
}
