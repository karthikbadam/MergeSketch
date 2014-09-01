package com.pivot.sketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

// UI of the Sketching View
public class MergeView extends View implements
		SketchSettingsDialog.OnOptionsChangedListener {

	public Layer mLayer;

	// initial brush width
	private int brush_width = 4;
	private Stroke mStroke;
	private int numberOfStrokes = 0;
	private int color = Color.BLACK;

	// bitmap and canvas for the UI
	public Bitmap mBitmap;
	public Canvas mCanvas;

	private int opacity = 255;

	private float mX, mY;
	private float startX, startY;
	private Path mPath;
	private static final float TOUCH_TOLERANCE = 3;
	
	public MergeView(Context c, int width, int height) {
		super(c);
		mLayer = new Layer();

		/*
		 * For old versions! int width = display.getWidth(); // deprecated
		 * int height = display.getHeight(); //deprecated
		 */

		mBitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);

	}
	
	public void setLayer(Layer layer){
		mLayer = layer;
		numberOfStrokes = layer.numberOfStrokes();
		for (int i = 0; i < numberOfStrokes; i++) {
			Stroke currentStroke = layer.getStroke(i);
			currentStroke.buildStroke();
		}
		invalidate();
	}
	
	@Override
	public void optionsChanged(int changed_color, int stroke_width, int opacity, boolean eraser) {
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
		for (int i = 0; i < numberOfStrokes; i++) {
			if (i < mLayer.numberOfStrokes()) {
				Stroke currentStroke = mLayer.getStroke(i);
				canvas.drawPath(currentStroke.mPath, currentStroke.mPaint);
			}
		}
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

	// move path to the touch point
	private void touch_start(float x, float y) {
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
		startX = x;
		startY = y;
	}

	// Draw a belzier curve to connect the touch points
	private void touch_move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	// draw the new path onto the UI screen as stroke ends
	private void touch_up(float x, float y) {
		// if the touch even has only touch down and up
		if (startX == x && startY == y) {
			mPath.addCircle(x, y,
					(float) Math.sqrt(mStroke.strokeWidth / Math.PI),
					Path.Direction.CCW);
		}
		mPath.lineTo(mX, mY);
		mCanvas.drawPath(mPath, mStroke.mPaint);
	}
	boolean start = false;
	
	
	
	// Register touch events
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX(0);
		float y = event.getY(0);
		
		/*
		if (tabs.getVisibility() == View.VISIBLE) {
			Animation out = AnimationUtils.makeOutAnimation(
					this.getContext(), true);
			out.setDuration(300);
			tabs.startAnimation(out);
			tabs.setVisibility(View.GONE);
			start = true;
		}

		if (x < 30 && y < 30) {
			if (tabs.getVisibility() == View.GONE) {
				Animation in = AnimationUtils.loadAnimation(
						this.getContext(), android.R.anim.fade_in);
				tabs.startAnimation(in);
				tabs.setVisibility(View.VISIBLE);
			}
			return true;
		}

		*/
		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:

			if (start == true) {
				break;
			}
			mStroke = new Stroke();
			mStroke.setStrokeType(Paint.Style.STROKE);
			mStroke.setStrokeWidth(brush_width);
			mStroke.setColor(color);
			mStroke.mPaint.setAlpha(opacity);
			mPath = mStroke.getPath();

			// get rid of the undo -ed strokes
			if (numberOfStrokes < mLayer.numberOfStrokes()) {
				for (int i = mLayer.numberOfStrokes() - 1; i > numberOfStrokes - 1; i--) {
					mLayer.removeStroke(i);
				}
			}

			mLayer.addStroke(mStroke);
			numberOfStrokes++;
			mStroke.addTouchPoint(x, y, false);
			touch_start(x, y);
			invalidate();
			break;

		case MotionEvent.ACTION_MOVE:
			if (start == true) {
				break;
			}
			mStroke.addTouchPoint(x, y, false);
			touch_move(x, y);
			invalidate();
			break;

		case MotionEvent.ACTION_UP:
			if (start == true) {
				start = false;
				break;
			}
			boolean is_end = true;
			mStroke.addTouchPoint(x, y, is_end);
			touch_up(x, y);
			invalidate();
			break;
		}

		return true;
	}

	public void changeColor() {
		SketchSettingsDialog d = new SketchSettingsDialog(this.getContext(),
				this, color, brush_width, opacity);
		d.show();
	}

}
