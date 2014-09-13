package com.pivot.sketch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.almeros.android.multitouch.MoveGestureDetector;
import com.almeros.android.multitouch.RotateGestureDetector;

import edu.pivot.cluster.Cluster;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.os.Environment;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;

// UI of the Sketching View
public class MergeView extends View implements
		SketchSettingsDialog.OnOptionsChangedListener {

	public Layer mLayer;

	// To work with minimize/maximize and coordinate transforms
	public float viewScale = 1;

	// initial brush width
	private int brush_width = 4;
	private Stroke mStroke;
	private int numberOfStrokes = 0;
	private int color = Color.BLACK;

	// bitmap and canvas for the UI
	public Bitmap mBitmap;
	public Canvas mCanvas;

	private int opacity = 255;

	int width = 0;
	int height = 0;

	Paint mPaint;
	Paint boxHoverPaint;
	Paint boxPaint;

	Boolean[] hovers;
	Boolean[] checked;

	ArrayList<Cluster> clusters = new ArrayList<Cluster>();

	Cluster currentSelection = null;

	public void set(int width, int height, float scale) {
		this.viewScale = scale;

		this.width = width;
		this.height = height;

		Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		mCanvas = new Canvas(mBitmap);
		for (Cluster cluster : clusters) {
			cluster.buildStrokes((float) viewScale);
		}

		invalidate();
	}

	@SuppressLint("NewApi")
	public MergeView(Context c, int width, int height, float scale) {
		super(c);
		mLayer = new Layer();

		/*
		 * For old versions! int width = display.getWidth(); // deprecated int
		 * height = display.getHeight(); //deprecated
		 */

		this.viewScale = scale;

		this.width = width;
		this.height = height;

		mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);

		mPaint = new Paint();
		mPaint.setStrokeWidth(1);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(Color.GRAY);
		mPaint.setPathEffect(new DashPathEffect(new float[] { 5, 10 }, 0));
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);

		boxHoverPaint = new Paint();
		boxHoverPaint.setStrokeWidth(1);
		boxHoverPaint.setColor(Color.argb(100, 233, 226, 218));
		boxHoverPaint.setAntiAlias(true);
		boxHoverPaint.setDither(true);
		boxHoverPaint.setStrokeJoin(Paint.Join.ROUND);
		boxHoverPaint.setStrokeCap(Paint.Cap.ROUND);
		boxHoverPaint.setStyle(Style.STROKE);

		boxPaint = new Paint();
		boxPaint.setStrokeWidth(1);
		boxPaint.setColor(Color.argb(100, 201, 193, 185));
		boxPaint.setAntiAlias(true);
		boxPaint.setDither(true);
		boxPaint.setStrokeJoin(Paint.Join.ROUND);
		boxPaint.setStrokeCap(Paint.Cap.ROUND);
		boxPaint.setStyle(Style.FILL);

		// scale listener
		mScaleDetector = new ScaleGestureDetector(c, new ScaleListener());
		mRotateDetector = new RotateGestureDetector(c, new RotateListener());
		mMoveDetector = new MoveGestureDetector(c, new MoveListener());

		this.setOnHoverListener(new OnHoverListener() {

			@Override
			public boolean onHover(View v, MotionEvent event) {

				float x = event.getX(0);
				float y = event.getY(0);

				// check in the clusters

				int count = 0;
				for (Cluster tempCluster : clusters) {
					hovers[count] = false;
					if (x > tempCluster.boundingbox.leftBoundary
							&& x < tempCluster.boundingbox.rightBoundary) {
						if (y > tempCluster.boundingbox.upperBoundary
								&& y < tempCluster.boundingbox.bottomBoundary) {
							hovers[count] = true;
						}
					}
					count++;
				}

				invalidate();
				return true;
			}

		});
	}

	Path tempPath = new Path();
	float temp_x = 0, temp_y = 0;

	// add selected clusters from sketchview!
	public void addSelection(Cluster cluster) {
		cluster.sliceLayerForTouchPoints();
		clusters.add(cluster);
		cluster.buildStrokes((float) viewScale);

		hovers = new Boolean[clusters.size()];
		checked = new Boolean[clusters.size()];

		for (int i = 0; i < clusters.size(); i++) {
			hovers[i] = false;
			checked[i] = false;
		}

		invalidate();
	}

	public void setLayer(Layer layer) {
		mLayer = layer;
		numberOfStrokes = layer.numberOfStrokes();
		for (int i = 0; i < numberOfStrokes; i++) {
			Stroke currentStroke = layer.getStroke(i);
			currentStroke.buildStroke();
		}
		invalidate();
	}

	@Override
	public void optionsChanged(int changed_color, int stroke_width,
			int opacity, boolean eraser) {
		this.color = changed_color;
		brush_width = stroke_width;
		this.opacity = opacity;
		if (eraser) {
			this.color = Color.WHITE;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	// onDraw called when invalidate()
	@Override
	protected void onDraw(Canvas canvas) {

		canvas.drawColor(Color.WHITE);

		canvas.drawRect(1, 1, this.width - 1, this.height - 1, mPaint);

		for (int i = 0; i < clusters.size(); i++) {
			List<Stroke> currentStrokes = clusters.get(i).getStrokes();
			for (int j = 0; j < currentStrokes.size(); j++) {
				canvas.drawPath(currentStrokes.get(j).mPath,
						currentStrokes.get(j).mPaint);
			}
		}

		if (currentSelection != null)
			canvas.drawRect(currentSelection.getBoundingRect(), boxPaint);

	}

	public void redo() {
		numberOfStrokes += 1;
		invalidate();
	}

	public void clear() {
		// strokes.clear();
		numberOfStrokes = 0;
		invalidate();
	}

	public void undo() {
		if (numberOfStrokes != 0) {
			numberOfStrokes -= 1;
			invalidate();
		}
	}

	// Advanced gestures!
	private ScaleGestureDetector mScaleDetector;
	private RotateGestureDetector mRotateDetector;
	private MoveGestureDetector mMoveDetector;

	private float mScaleSpan = 1.0f;
	private float mScaleFactor = 1.0f;
	private float mRotationDegrees = 0.f;
	private float mFocusX = 0.f;
	private float mFocusY = 0.f;

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleSpan = detector.getCurrentSpan(); // average distance between
													// fingers
			mScaleFactor = detector.getScaleFactor(); // scale change since
														// previous event
			if (currentSelection != null) {
				currentSelection.scalePoints(mScaleFactor);
				currentSelection.buildStrokes(viewScale);
			}

			return true; // indicate event was handled
		}
	}

	private class RotateListener extends
			RotateGestureDetector.SimpleOnRotateGestureListener {
		@Override
		public boolean onRotate(RotateGestureDetector detector) {
			mRotationDegrees = (float) (-0.1 * detector
					.getRotationDegreesDelta());
			if (currentSelection != null) {
				currentSelection.rotatePoints(mRotationDegrees);
				currentSelection.buildStrokes(viewScale);
			}
			return true;
		}
	}

	private class MoveListener extends
			MoveGestureDetector.SimpleOnMoveGestureListener {
		@Override
		public boolean onMove(MoveGestureDetector detector) {
			PointF d = detector.getFocusDelta();
			mFocusX += d.x;
			mFocusY += d.y;
			if (currentSelection != null) {
				currentSelection.translate(d.x, d.y);
				currentSelection.buildStrokes(viewScale);
			}
			return true;
		}
	}

	// Register touch events
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX(0);
		float y = event.getY(0);

		// check which clusters are directly underneath
		int count = 0;

		float minArea = 100000000;
		int minIndex = -1;
		

		if (event.getAction() == MotionEvent.ACTION_UP) {

			for (Cluster tempCluster : clusters) {
				checked[count] = false;
				if (x > tempCluster.boundingbox.leftBoundary
						&& x < tempCluster.boundingbox.rightBoundary) {
					if (y > tempCluster.boundingbox.upperBoundary
							&& y < tempCluster.boundingbox.bottomBoundary) {

						if (tempCluster.getArea() < minArea) {
							minArea = tempCluster.getArea();
							minIndex = count;
						}

					}
				}
				count++;
			}

			if (minIndex != -1 && checked[minIndex] == false) {
				checked[minIndex] = true;
				currentSelection = clusters.get(minIndex);

			} else if (minIndex!= -1 && checked[minIndex] == false) {
				checked[minIndex] = false; 
				currentSelection = null;
				
			} else {
				
				currentSelection = null;
			}
		}
		
		mRotateDetector.onTouchEvent(event);
		mScaleDetector.onTouchEvent(event);
		mMoveDetector.onTouchEvent(event);

		invalidate();

		/*
		 * if (tabs.getVisibility() == View.VISIBLE) { Animation out =
		 * AnimationUtils.makeOutAnimation( this.getContext(), true);
		 * out.setDuration(300); tabs.startAnimation(out);
		 * tabs.setVisibility(View.GONE); start = true; }
		 * 
		 * if (x < 30 && y < 30) { if (tabs.getVisibility() == View.GONE) {
		 * Animation in = AnimationUtils.loadAnimation( this.getContext(),
		 * android.R.anim.fade_in); tabs.startAnimation(in);
		 * tabs.setVisibility(View.VISIBLE); } return true; }
		 */
		// switch (event.getAction()) {
		//
		// case MotionEvent.ACTION_DOWN:
		//
		// if (start == true) {
		// break;
		// }
		// mStroke = new Stroke();
		// mStroke.setStrokeType(Paint.Style.STROKE);
		// mStroke.setStrokeWidth(brush_width);
		// mStroke.setColor(color);
		// mStroke.mPaint.setAlpha(opacity);
		// mPath = mStroke.getPath();
		//
		// // get rid of the undo -ed strokes
		// if (numberOfStrokes < mLayer.numberOfStrokes()) {
		// for (int i = mLayer.numberOfStrokes() - 1; i > numberOfStrokes - 1;
		// i--) {
		// mLayer.removeStroke(i);
		// }
		// }
		//
		// mLayer.addStroke(mStroke);
		// numberOfStrokes++;
		// mStroke.addTouchPoint(x, y, false);
		// touch_start(x, y);
		// invalidate();
		// break;
		//
		// case MotionEvent.ACTION_MOVE:
		// if (start == true) {
		// break;
		// }
		// mStroke.addTouchPoint(x, y, false);
		// touch_move(x, y);
		// invalidate();
		// break;
		//
		// case MotionEvent.ACTION_UP:
		// if (start == true) {
		// start = false;
		// break;
		// }
		// boolean is_end = true;
		// mStroke.addTouchPoint(x, y, is_end);
		// touch_up(x, y);
		// invalidate();
		// break;
		// }

		return true;
	}

	// Save as an image
	public void openBitmap(String filename) {

		File mediaStorageDir = new File(
				Environment.getExternalStorageDirectory(), "VICED");
		mediaStorageDir.mkdirs();

		File dataFile = new File(mediaStorageDir.getPath(), filename);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		mLayer = new Layer();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(dataFile));
			doc.getDocumentElement().normalize();

			NodeList layer_nodes = doc.getElementsByTagName("layer");

			for (int i = 0; i < layer_nodes.getLength(); i++) {
				Element layer_node = (Element) layer_nodes.item(i);
				NodeList stroke_nodes = layer_node
						.getElementsByTagName("stroke");
				for (int j = 0; j < stroke_nodes.getLength(); j++) {
					Node stroke_node = stroke_nodes.item(j);
					Element stroke_element = (Element) stroke_node;

					// Getting Width
					NodeList width_nodes = stroke_element
							.getElementsByTagName("width");
					Element width_element = (Element) width_nodes.item(0);

					NodeList width_values = width_element.getChildNodes();
					Float width = Float.valueOf(width_values.item(0)
							.getNodeValue());

					// Getting type

					NodeList type_nodes = stroke_element
							.getElementsByTagName("type");
					Element type_node = (Element) type_nodes.item(0);
					NodeList type_values = type_node.getChildNodes();
					String type = "";
					type = type_values.item(0).getNodeValue();

					// Getting color
					NodeList color_nodes = stroke_element
							.getElementsByTagName("color");
					Element color_node = (Element) color_nodes.item(0);
					NodeList color_values = color_node.getChildNodes();
					int color = Integer.valueOf(color_values.item(0)
							.getNodeValue());

					Stroke stroke = new Stroke();

					stroke.setColor(color);
					if (type.equals("STROKE"))
						stroke.setStrokeType(Paint.Style.STROKE);
					stroke.setStrokeWidth(width);

					// Getting trace
					NodeList trace_nodes = stroke_element
							.getElementsByTagName("trace");
					Element trace_node = (Element) trace_nodes.item(0);
					NodeList trace_values = trace_node.getChildNodes();
					String trace = trace_values.item(0).getNodeValue();
					String[] touch_points = trace.split(",");
					boolean is_end = false;
					for (int k = 0; k < touch_points.length; k++) {
						String[] touch_coordinates = touch_points[k].split(" ");
						Float x = (float) (0.5 * Float
								.valueOf(touch_coordinates[0]));
						Float y = (float) (0.5 * Float
								.valueOf(touch_coordinates[1]));

						stroke.addTouchPoint(x, y, is_end);

					}
					stroke.buildStroke();
					mLayer.addStroke(stroke);
				}

			}
			numberOfStrokes = mLayer.numberOfStrokes();
			invalidate();

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void changeColor() {
		SketchSettingsDialog d = new SketchSettingsDialog(this.getContext(),
				this, color, brush_width, opacity);
		d.show();
	}

	public void deleteActiveSelection() {
		
		if (currentSelection != null) {
			clusters.remove(currentSelection);
			currentSelection = null;
			invalidate();
			
		}
	}

}
