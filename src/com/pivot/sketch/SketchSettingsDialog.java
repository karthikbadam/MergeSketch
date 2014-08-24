package com.pivot.sketch;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SketchSettingsDialog extends Dialog {

	private OnOptionsChangedListener mListener;
	private int mInitialColor;
	private int mInitialStrokeWidth;
	private int mInitialOpacity;
	private boolean eraser = false;

	private int mFinalColor;
	public int mFinalStrokeWidth;
	public int mFinalOpacity;

	public interface OnOptionsChangedListener {
		void optionsChanged(int color, int stroke_width, int opacity,
				boolean eraser);
	}

	public SketchSettingsDialog(Context context,
			OnOptionsChangedListener listener, int initialColor,
			int initial_stroke_width, int initial_opacity) {

		super(context);

		mListener = listener;
		mInitialColor = initialColor;
		mInitialStrokeWidth = initial_stroke_width;
		mInitialOpacity = initial_opacity;
		
		mFinalColor = initialColor;
		mFinalStrokeWidth = initial_stroke_width;
		mFinalOpacity = initial_opacity;

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ColorPickerView colorPickerView = new ColorPickerView(
				this.getContext(), mInitialColor);

		LayoutInflater layout = (LayoutInflater) this.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = layout.inflate(R.layout.sketch_settings, null);

		RelativeLayout color_layout = (RelativeLayout) v.findViewById(R.id.colorlayout);

		color_layout.addView(colorPickerView);
		setContentView(v);
		setTitle("Settings");
		
		
		SeekBar size_seekbar = (SeekBar) v.findViewById(R.id.size_seekbar);
		final TextView sizeBar_value = (TextView) v.findViewById(R.id.size_seekval);
		size_seekbar.setProgress(mInitialStrokeWidth);
		sizeBar_value.setText(String.valueOf(mInitialStrokeWidth));
		size_seekbar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// Do something here with new value
						sizeBar_value.setText(String.valueOf(progress));
						mFinalStrokeWidth = progress;
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
	
		SeekBar opacity_seekbar = (SeekBar) v
				.findViewById(R.id.opacity_seekbar);
		final TextView opacityBar_value = (TextView) v
				.findViewById(R.id.opacity_seekvalue);
		opacity_seekbar.setProgress(mInitialOpacity);
		opacityBar_value.setText(String.valueOf(mInitialOpacity));
		opacity_seekbar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// Do something here with new value
						opacityBar_value.setText(String.valueOf(progress));
						mFinalOpacity = progress;
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

		CheckBox eraser_checkbox = (CheckBox) v
				.findViewById(R.id.eraser_checkbox);
		eraser_checkbox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						if (buttonView.isChecked()) {
							eraser = true;
						} else {
							eraser = false;
						}

					}

				});

	}

	@Override
	protected void onStop() {
		super.onStop();
		mListener.optionsChanged(mFinalColor, mFinalStrokeWidth, mFinalOpacity, eraser);
	}
	
	
	
	private class ColorPickerView extends View {
		private Paint mPaint;
		private Paint mCenterPaint;
		private final int[] mColors;
		Paint black;
		Paint previous;

		ColorPickerView(Context c, int color) {
			super(c);
			mColors = new int[] { 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
					0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000 };
			Shader s = new SweepGradient(0, 0, mColors, null);
			black = new Paint();
			previous = new Paint();

			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setShader(s);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(50);

			mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mCenterPaint.setColor(color);
			mCenterPaint.setStrokeWidth(6);
		}

		private boolean mTrackingCenter;
		private boolean mHighlightCenter;

		@Override
		protected void onDraw(Canvas canvas) {
			float r = (CENTER_X - mPaint.getStrokeWidth() * 0.5f);

			canvas.translate(CENTER_X, CENTER_X);

			canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
			canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

			black.setColor(Color.BLACK);
			black.setStrokeWidth(0);

			previous.setColor(mInitialColor);
			previous.setStrokeWidth(0);

			canvas.drawCircle(0, -r / 2, CENTER_RADIUS, previous);
			canvas.drawCircle(0, r / 2, CENTER_RADIUS, black);

			if (mTrackingCenter) {
				int c = mCenterPaint.getColor();
				mCenterPaint.setStyle(Paint.Style.STROKE);

				if (mHighlightCenter) {
					mCenterPaint.setAlpha(0xFF);
				} else {
					mCenterPaint.setAlpha(0x80);
				}
				canvas.drawCircle(0, 0,
						CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
						mCenterPaint);

				mCenterPaint.setStyle(Paint.Style.FILL);
				mCenterPaint.setColor(c);
			}
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			setMeasuredDimension(CENTER_X * 2, CENTER_Y * 2);
		}

		private static final int CENTER_X = 200;
		private static final int CENTER_Y = 200;
		private static final int CENTER_RADIUS = 30;

		private int ave(int s, int d, float p) {
			return s + java.lang.Math.round(p * (d - s));
		}

		private int interpColor(int colors[], float unit) {
			if (unit <= 0) {
				return colors[0];
			}
			if (unit >= 1) {
				return colors[colors.length - 1];
			}

			float p = unit * (colors.length - 1);
			int i = (int) p;
			p -= i;

			// now p is just the fractional part [0...1) and i is the index
			int c0 = colors[i];
			int c1 = colors[i + 1];
			int a = ave(Color.alpha(c0), Color.alpha(c1), p);
			int r = ave(Color.red(c0), Color.red(c1), p);
			int g = ave(Color.green(c0), Color.green(c1), p);
			int b = ave(Color.blue(c0), Color.blue(c1), p);

			return Color.argb(a, r, g, b);
		}

		private static final float PI = 3.1415926f;

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX() - CENTER_X;
			float y = event.getY() - CENTER_Y;
			float r = (CENTER_X - mPaint.getStrokeWidth() * 0.5f);
			boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= CENTER_RADIUS;
			boolean inRing = Math.abs(java.lang.Math.sqrt(x * x + y * y) - r) <= 60;
			boolean inBlack = java.lang.Math.sqrt(x * x + (y - r / 2)
					* (y - r / 2)) <= CENTER_RADIUS;
			boolean inPrevious = java.lang.Math.sqrt(x * x + (y + r / 2)
					* (y + r / 2)) <= CENTER_RADIUS;

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mTrackingCenter = inCenter;
				if (inRing) {
					break;
				}
				if (inCenter) {
					mHighlightCenter = true;
					invalidate();
					break;
				}
				
			case MotionEvent.ACTION_MOVE:
				if (mTrackingCenter) {
					if (mHighlightCenter != inCenter) {
						mHighlightCenter = inCenter;
						invalidate();
					}
				} else if (inRing) {
					float angle = (float) java.lang.Math.atan2(y, x);
					float unit = angle / (2 * PI);
					if (unit < 0) {
						unit += 1;
					}
					mCenterPaint.setColor(interpColor(mColors, unit));
					mFinalColor = mCenterPaint.getColor();
					invalidate();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTrackingCenter) {
					mFinalColor = mCenterPaint.getColor();
					mTrackingCenter = false; // so we draw w/o halo
					
				} else if (inBlack) {
					mFinalColor = Color.BLACK;
					mCenterPaint.setColor(Color.BLACK);	
				} else if (inPrevious) {
					mCenterPaint.setColor(mInitialColor);	
					mFinalColor = mInitialColor;
				}
				invalidate();
				break;
			}
			return true;
		}
	}

}
