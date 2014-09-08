package edu.pivot.history;

import java.util.ArrayList;

import com.pivot.sketch.Layer;
import com.pivot.sketch.R;
import com.pivot.sketch.Stroke;

import edu.pivot.cluster.Aggregator;
import edu.pivot.cluster.Cluster;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class HistoryBrowser extends Activity {

	Layer layer;
	// bitmap and canvas for the UI
	private Bitmap mBitmap;
	private Canvas mCanvas;

	ArrayList<Cluster> clusters;

	int clusterNumber = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("History Manager");

		Intent sender = getIntent();
		Bundle stroke_data = sender.getExtras();
		layer = stroke_data.getParcelable("layer");

		System.out.println("size:"
				+ layer.getStrokes().get(0).points.get(0).getX());

		setContentView(R.layout.history_browser);

		final TextView seekBarValue = (TextView) findViewById(R.id.hsize_value);

		SeekBar sizeSeekbar = (SeekBar) findViewById(R.id.seekBar);
		sizeSeekbar.setProgress(clusterNumber);
		seekBarValue.setText(String.valueOf(clusterNumber));

		sizeSeekbar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// Do something here with new value
						seekBarValue.setText(String.valueOf(progress));
						clusterNumber = progress;
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}
				});

		Button b = (Button) findViewById(R.id.button1);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Aggregator a = new Aggregator(layer);

				clusters = a.aggregate(clusterNumber);

				// main UI
				MyView view = new MyView(HistoryBrowser.this);
				setContentView(view);
				view.inValidate();

				// Perform action on click
			}
		});

		Intent intent = new Intent();
		intent.putExtra("ComingFrom", "Back to main");
		setResult(RESULT_OK, intent);

	}

	@SuppressLint("NewApi")
	public class MyView extends View {

		public MyView(Context c) {
			super(c);
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;

			/*
			 * For old versions! int width = display.getWidth(); // deprecated
			 * int height = display.getHeight(); //deprecated
			 */

			mBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);

		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
		}

		// onDraw called when invalidate()
		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE);
			for (int i = 0; i < layer.getStrokes().size(); i++) {
				Stroke currentStroke = layer.getStroke(i);
				currentStroke.buildStroke();
				canvas.drawPath(currentStroke.mPath, currentStroke.mPaint);

				Paint mPaint = new Paint();
				mPaint.setStrokeWidth(2);
				mPaint.setColor(Color.DKGRAY);
				mPaint.setAntiAlias(true);
				mPaint.setDither(true);
				mPaint.setStrokeJoin(Paint.Join.ROUND);
				mPaint.setStrokeCap(Paint.Cap.ROUND);
				mPaint.setStyle(Style.STROKE);

							

			}
			
			Paint mPaint = new Paint();
			mPaint.setStrokeWidth(2);
			mPaint.setColor(Color.DKGRAY);
			mPaint.setAntiAlias(true);
			mPaint.setDither(true);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStyle(Style.STROKE);
			
			Paint mPaint_t = new Paint();
			mPaint_t.setStrokeWidth(2);
			mPaint_t.setColor(Color.BLUE);
			mPaint_t.setTextSize(25);
			
			int count = 1;
			for (Cluster tempCluster : clusters) {
				canvas.drawRect(new RectF(
						tempCluster.boundingbox.leftBoundary,
						tempCluster.boundingbox.upperBoundary,
						tempCluster.boundingbox.rightBoundary,
						tempCluster.boundingbox.bottomBoundary), mPaint);
				canvas.drawText("" + count,
						tempCluster.boundingbox.leftBoundary - 8,
						tempCluster.boundingbox.upperBoundary - 8, mPaint_t);
				count++;
			}

		}

		public void inValidate() {
			invalidate();
		}

		/*
		 * public void redo() { numberOfStrokes += 1; invalidate(); }
		 * 
		 * public void clear() { // strokes.clear(); numberOfStrokes = 0;
		 * invalidate(); }
		 * 
		 * public void undo() { numberOfStrokes -= 1; invalidate(); }
		 * 
		 * // move path to the touch point private void touch_start(float x,
		 * float y) { mPath.moveTo(x, y); mX = x; mY = y; startX = x; startY =
		 * y; }
		 * 
		 * // Draw a belzier curve to connect the touch points private void
		 * touch_move(float x, float y) { float dx = Math.abs(x - mX); float dy
		 * = Math.abs(y - mY); if (dx >= TOUCH_TOLERANCE || dy >=
		 * TOUCH_TOLERANCE) { mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
		 * mX = x; mY = y; } }
		 * 
		 * // draw the new path onto the UI screen as stroke ends private void
		 * touch_up(float x, float y) { // if the touch even has only touch down
		 * and up if (startX == x && startY == y) { mPath.addCircle(x, y,
		 * (float) Math.sqrt(mStroke.strokeWidth / Math.PI),
		 * Path.Direction.CCW); } mPath.lineTo(mX, mY); mCanvas.drawPath(mPath,
		 * mStroke.mPaint); } // Register touch events
		 * 
		 * @Override public boolean onTouchEvent(MotionEvent event) { float x =
		 * event.getX(); float y = event.getY();
		 * 
		 * switch (event.getAction()) {
		 * 
		 * case MotionEvent.ACTION_DOWN:
		 * 
		 * mStroke = new Stroke(); mStroke.setStrokeType(Paint.Style.STROKE);
		 * mStroke.setStrokeWidth(brush_width); mStroke.setColor(color);
		 * mStroke.mPaint.setAlpha(opacity); mPath = mStroke.getPath();
		 * 
		 * // get rid of the undo -ed strokes if (numberOfStrokes <
		 * layer.numberOfStrokes()) { for (int i = layer.numberOfStrokes() - 1;
		 * i > numberOfStrokes - 1; i--) { layer.removeStroke(i); } }
		 * 
		 * layer.addStroke(mStroke); numberOfStrokes++; mStroke.addTouchPoint(x,
		 * y, false); touch_start(x, y); invalidate(); break;
		 * 
		 * case MotionEvent.ACTION_MOVE: mStroke.addTouchPoint(x, y, false);
		 * touch_move(x, y); invalidate(); break;
		 * 
		 * case MotionEvent.ACTION_UP: boolean is_end = true;
		 * mStroke.addTouchPoint(x, y, is_end); touch_up(x, y); invalidate();
		 * break; }
		 * 
		 * return true; }
		 */
	}

}
