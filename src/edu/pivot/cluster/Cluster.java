package edu.pivot.cluster;


public class Cluster {
	
	public Cluster parent = null;
	
	public Cluster leftChild = null;
	public Cluster rightChild = null;
	
	public float startIndex;
	public float endIndex;
	
	public BoundingBox boundingbox;
	
	
	float leftDistance;
	
	public int historyIndex = -1;
	
	public Cluster(float leftDistance) {
		super();
		this.leftDistance = leftDistance;
		boundingbox = new BoundingBox();

		
	}
	
	public Cluster(float leftDistance, int historyIndex) {
		super();
		this.leftDistance = leftDistance;
		this.historyIndex = historyIndex;
		boundingbox = new BoundingBox();
	}
	
	public void setStartEnd(int historyIndex) {
		startIndex = historyIndex;
		endIndex = historyIndex;
				
	}
	
	
	
	
}
