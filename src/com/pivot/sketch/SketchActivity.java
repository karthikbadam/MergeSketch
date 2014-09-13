package com.pivot.sketch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.pivot.merge.R;
import com.pivot.storage.ReadFromDropbox;
import com.pivot.storage.WriteIndexFile;
import com.pivot.storage.WriteToDropbox;
import com.pivot.storage.ReadFromDropbox.FileDownloadListener;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.text.Editable;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.pivot.cluster.Aggregator;
import edu.pivot.cluster.Cluster;
import edu.pivot.history.HistoryBrowser;

public class SketchActivity extends Activity {

	Context mContext;

	// main UI -- all buttons
	// private SketchView view;
	private SketchView viewLeft;
	private SketchView viewRight;

	private RelativeLayout tabs;
	ImageButton startNewSession;
	ImageButton browser;
	ImageButton checkout;
	ImageButton commit;
	ImageButton branch;
	// ImageButton color;
	// ImageButton undo;
	ImageButton redo;
	// ImageButton clear;
	ImageButton delete;
	ImageButton save;

	ImageButton grow;
	ImageButton shrink;
	ImageButton maximize;

	ProgressDialog dialog;

	MergeView mergeView;

	Handler updateBarHandler;

	String LOG;

	// current file
	File mFile = null;

	boolean firstTime = true;

	int width;
	int height;

	private Vibrator myVib;

	String filename1 = "a.xml";
	String filename2 = "b.xml";

	protected long startTime;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Android Application context
		mContext = this;

		// Time
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
		String newFile = df.format(date);

		myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

		dialog = new ProgressDialog(mContext);

		// Creating the UI
		LayoutInflater layout = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Read the content of main_ui xml file
		ViewGroup v = (ViewGroup) layout.inflate(R.layout.main_ui, null);

		tabs = (RelativeLayout) v.findViewById(R.id.tab_layout);

		// Get the size of the sketch layout
		RelativeLayout sketching = (RelativeLayout) v
				.findViewById(R.id.sketch_view);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;

		updateBarHandler = new Handler();

		// Sketch on the left
		RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params1.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params1.leftMargin = 0;
		params1.topMargin = 0;
		params1.width = width / 2 - 20;
		params1.height = 3 * height / 4;

		viewLeft = new SketchView(this, width / 2 - 20, 3 * height / 4,
				(float) 0.5, viewRight);

		sketching.addView(viewLeft, params1);

		// Sketch on the right
		RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params2.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params2.leftMargin = width / 2 + 20;
		params2.topMargin = 0;
		params2.width = width / 2 - 20;
		params2.height = 3 * height / 4;

		viewRight = new SketchView(this, width / 2 - 20, 3 * height / 4,
				(float) 0.5, viewLeft);
		sketching.addView(viewRight, params2);
		viewLeft.other = viewRight;
		// viewRight.openBitmap("b.xml");

		// Alert dialog asking for sketch xmls
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Enter sketch names");

		// Set an EditText view to get user input
		LinearLayout l1 = new LinearLayout(this);
		l1.setOrientation(LinearLayout.VERTICAL);
		final EditText input1 = new EditText(this);
		final EditText input2 = new EditText(this);
		l1.addView(input1);
		l1.addView(input2);
		alert.setView(l1);

		final AlertDialog.Builder alert2 = new AlertDialog.Builder(this);
		alert2.setTitle("Clustering is done");

		alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				startTime = SystemClock.uptimeMillis();
			}
		});

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input1.getText();
				viewLeft.openBitmap(value.toString());

				Editable value2 = input2.getText();
				viewRight.openBitmap(value2.toString());

				final Aggregator a1 = new Aggregator(viewLeft.mLayer);
				final Aggregator a2 = new Aggregator(viewRight.mLayer);

				int clusterNumber1 = viewLeft.mLayer.numberOfStrokes();
				int clusterNumber2 = viewRight.mLayer.numberOfStrokes();

				ArrayList<Cluster> clusters1 = a1
						.aggregate((int) clusterNumber1);
				viewLeft.addClusters(clusters1);

				ArrayList<Cluster> clusters2 = a2
						.aggregate((int) clusterNumber2);

				// now add this to the interface
				viewRight.addClusters(clusters2);

				alert2.show();

				LOG = "" + uuid + ";" + value.toString() + ","
						+ value2.toString()+";";

			}
		});

		alert.show();

		// Merged layout
		RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params3.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params3.leftMargin = width / 4;
		params3.topMargin = height / 2;
		params3.width = width / 2;
		params3.height = height / 2;

		mergeView = new MergeView(this, width / 2, height / 2, (float) 1);
		sketching.addView(mergeView, params3);

		// color = (ImageButton) v.findViewById(R.id.button4);
		// undo = (ImageButton) v.findViewById(R.id.button5);
		redo = (ImageButton) v.findViewById(R.id.button6);
		// clear = (ImageButton) v.findViewById(R.id.button7);
		delete = (ImageButton) v.findViewById(R.id.button8);
		save = (ImageButton) v.findViewById(R.id.button9);

		grow = (ImageButton) v.findViewById(R.id.button7);
		shrink = (ImageButton) v.findViewById(R.id.button10);
		maximize = (ImageButton) v.findViewById(R.id.button11);

		// undo.setOnClickListener(new View.OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		//
		// }
		// });

		grow.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				myVib.vibrate(50);

				Cluster selection = viewLeft.getSelection();

				if (selection != null && selection.parent != null) {
					viewLeft.currentSelection = selection.parent;
					viewLeft.currentSelection.sliceLayerForTouchPoints();
					viewLeft.currentSelection.buildStrokes(1);
					viewLeft.invalidate();

					System.out.println("Blah "
							+ viewLeft.currentSelection.startIndex + ", "
							+ viewLeft.currentSelection.endIndex);
				}

				selection = viewRight.getSelection();

				if (selection != null && selection.parent != null) {
					viewRight.currentSelection = selection.parent;
					viewRight.currentSelection.sliceLayerForTouchPoints();
					viewRight.currentSelection.buildStrokes(1);
					viewRight.invalidate();
				}
			}
		});

		shrink.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				myVib.vibrate(50);

				Cluster selection = viewLeft.getSelection();

				if (selection != null) {
					viewLeft.currentSelection = selection.leftChild;
					viewLeft.invalidate();
				}

				selection = viewRight.getSelection();

				if (selection != null)
					viewRight.currentSelection = selection.leftChild;
				viewRight.invalidate();

			}
		});

		maximize.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				myVib.vibrate(50);

				RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				params3.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
						RelativeLayout.TRUE);
				params3.leftMargin = width / 4;
				params3.topMargin = height / 2 - 30;
				params3.width = width / 2 + 30;
				params3.height = height / 2 + 30;

				mergeView.set(width / 2 + 30, height / 2 + 30, (float) 1);

				mergeView.setLayoutParams(params3);
			}
		});

		redo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// COPY THE SELECTED SKETCH CONTENT
				myVib.vibrate(50);
				Cluster selection = viewLeft.getSelection();

				if (selection != null) {
					mergeView.addSelection(selection);
					LOG += "Left: "+ selection.boundingbox.leftBoundary + ","
							+ selection.boundingbox.rightBoundary + ","
							+ selection.boundingbox.upperBoundary + ","
							+ selection.boundingbox.bottomBoundary + ";";
				}

				selection = viewRight.getSelection();

				if (selection != null) {
					mergeView.addSelection(selection);
					LOG += "Right: "+ selection.boundingbox.leftBoundary + ","
						+ selection.boundingbox.rightBoundary + ","
						+ selection.boundingbox.upperBoundary + ","
						+ selection.boundingbox.bottomBoundary + ";";
				}

			}
		});

		// clear.setOnClickListener(new View.OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		//
		// }
		// });

		delete.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				myVib.vibrate(50);

				mergeView.deleteActiveSelection();

				// if (!dialog.isShowing()) {
				//
				// dialog.setTitle("Sketcholution at work!");
				//
				// dialog.setMessage("Clustering the sketch. Please wait...");
				// dialog.setProgressStyle(dialog.STYLE_HORIZONTAL);
				// dialog.setProgress(0);
				// dialog.show();
				//
				// final Aggregator a1 = new Aggregator(viewLeft.mLayer,
				// dialog);
				// final Aggregator a2 = new Aggregator(viewRight.mLayer,
				// dialog);
				//
				// int clusterNumber1 = viewLeft.mLayer.numberOfStrokes();
				// int clusterNumber2 = viewRight.mLayer.numberOfStrokes();
				//
				// dialog.setMax(100);
				//
				// ArrayList<Cluster> clusters1 = a1
				// .aggregate((int) clusterNumber1, updateBarHandler);
				// viewLeft.addClusters(clusters1);
				//
				// ArrayList<Cluster> clusters2 = a2
				// .aggregate((int) clusterNumber2, updateBarHandler);
				// // now add this to the interface
				// viewRight.addClusters(clusters2);
				//
				// showToast("Sketcholution at work");
				//
				// dialog.dismiss();
				//
				// }

				// Bundle stroke_data = new Bundle();
				// stroke_data.putParcelable("layer", view.mLayer);
				// Intent intent = new Intent(mContext, HistoryBrowser.class);
				// intent.putExtras(stroke_data);
				// final int result = 1;
				// startActivityForResult(intent, result);
			}
		});

		save.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// save final sketch as bitmap
				saveBitmap();
			}
		});
		setContentView(v);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		mContext = this;
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;
		dialog = new ProgressDialog(mContext);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// End Activity when user clicks back
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Handle the back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Ask the user if they want to quit
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle("Exit")
					.setMessage("Are you sure you want to leave?")
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// Exit the activity
									// logOut();
									android.os.Process
											.killProcess(android.os.Process
													.myPid());
									finish();
								}
							}).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	String uuid = UUID.randomUUID().toString();

	// Save as an image
	private void saveBitmap() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Write Files");
		alert.setMessage("");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		// alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Editable value = input.getText();

				long timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
				LOG+= "time; "+timeInMilliseconds; 
				
				FileOutputStream outStream = null;
				File mediaStorageDir = new File(Environment
						.getExternalStorageDirectory(), "ClusterStudy");
				mediaStorageDir.mkdirs();

				mFile = new File(mediaStorageDir.getPath(), uuid + ".PNG");
				File mFile2 = new File(mediaStorageDir.getPath(), uuid+".csv");
				
				
				mergeView.draw(mergeView.mCanvas);

				try {
					outStream = new FileOutputStream(mFile);
					
					
					mergeView.mBitmap.compress(Bitmap.CompressFormat.PNG, 100,
							outStream);
					outStream.flush();
					outStream.close();
					
					BufferedWriter writer = new BufferedWriter(new FileWriter(mFile2, true));
					writer.write(LOG);
				      Toast.makeText(mContext.getApplicationContext(),
				          "Report successfully saved to: " +mFile2.getAbsolutePath(),
				          Toast.LENGTH_LONG).show();
				      writer.close();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				});

		alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_sketch, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.SaveImage:
			saveBitmap();
			return true;

		case R.id.clear:
			return true;

		case R.id.Color:
			return true;

		case R.id.undo:
			return true;

		case R.id.Redo:
			return true;

		case R.id.history:
			return true;
		}

		return false;
	}

	// @Override
	// public void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	//
	// super.onActivityResult(requestCode, resultCode, data);
	// String extraData = data.getStringExtra("ComingFrom");
	// System.out.println(extraData);
	//
	// if (extraData.equals("File Browser")) {
	// Layer layer = data.getParcelableExtra("layer");
	// System.out.println("layer " + layer.getStrokes().size());
	// view.setLayer(layer);
	//
	// String dir = data.getStringExtra("dir");
	// sessionDir = dir;
	//
	// if (dir.charAt(dir.length() - 1) == '/') {
	// sessionName = dir.replace(dir.substring(dir.length() - 1), "");
	// }
	// System.out.println("Selected Dir " + dir);
	// isBranch = data.getBooleanExtra("branch", true);
	// mFileName = data.getStringExtra("filename");
	// firstTime = false;
	// /*
	// int number_of_branches = data.getIntExtra("branchNumber", 0);
	// if (isBranch == true) {
	// mBranchNumber = number_of_branches + 1;
	// mFileName = "branch" + mBranchNumber + ".xml";
	// } else {
	// mFileName = "main.xml";
	//
	// }
	// */
	//
	// }
	// }

	/* toast */
	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

}
