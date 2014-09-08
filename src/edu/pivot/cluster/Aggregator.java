package edu.pivot.cluster;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.pivot.sketch.Layer;
import com.pivot.sketch.Stroke;
import com.pivot.sketch.TouchPoint;

public class Aggregator {
	Layer layer;
	int totalHistorySize = 0;

	public ArrayList<Cluster> clusters;
	public ArrayList<BoundingBox> boxes;

	public Aggregator(Layer layer) {
		this.layer = layer;

		clusters = Lists.newArrayList();
		boxes = Lists.newArrayList();

	}

	
	public ArrayList<Cluster> aggregate(int clusterNumber) {
		initLeafCluster();

		for (Cluster tempCluster : clusters) {
			System.out.println(tempCluster.leftDistance);

		}

		for (int i = 0; i < totalHistorySize - clusterNumber; i++) {
			// *******find out the min distance and index for the right node
			int minIndex = getMinDistanceIndex();

			Cluster newCluster = new Cluster(
					clusters.get(minIndex - 1).leftDistance);
			newCluster.leftChild = clusters.get(minIndex - 1);
			newCluster.rightChild = clusters.get(minIndex);

			newCluster.startIndex = newCluster.leftChild.startIndex;
			newCluster.endIndex = newCluster.rightChild.endIndex;

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

		System.out.println("CLuster size " + clusters.size());

		int i = 0;
		for (Cluster tempCluster : clusters) {
			System.out.println(" cluster: " + i);
			System.out.println(tempCluster.startIndex + " "
					+ tempCluster.endIndex);
			// tempCluster.printString();
			i++;
		}
		
		return clusters;
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
			newCluster.boundingbox.leftBoundary = point.getX();
			newCluster.boundingbox.rightBoundary = point.getX();
			newCluster.boundingbox.upperBoundary = point.getY();
			newCluster.boundingbox.bottomBoundary = point.getY();

			clusters.add(newCluster);
			count++;

			for (int j = 1; j < points.size() - 1; j++) {
				point = points.get(j);
				point.setLeftDistance(point.distance(points.get(j-1)));
				newCluster = new Cluster(point.leftDistance, count);
				newCluster.setStartEnd(count);
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
