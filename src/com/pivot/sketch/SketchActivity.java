package com.pivot.sketch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.pivot.storage.ReadFromDropbox;
import com.pivot.storage.WriteIndexFile;
import com.pivot.storage.WriteToDropbox;
import com.pivot.storage.ReadFromDropbox.FileDownloadListener;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
	//private SketchView view;
	private MergeView view;
	private MergeView view2; 
	
	private RelativeLayout tabs;
	ImageButton startNewSession;
	ImageButton browser;
	ImageButton checkout;
	ImageButton commit;
	ImageButton branch;
	ImageButton color;
	ImageButton undo;
	ImageButton redo;
	ImageButton clear;
	ImageButton history;
	ImageButton save;
	
	//current file
	File mFile = null;
	
	//VICED current branch
	int mBranchNumber = 0;
	boolean isBranch = false;

	//Dropbox folder to store the sketches
	//public String sessionName = "/VICED/one";

	//App key and secret from Dropbox
	//final static private String APP_KEY = "ewvxxygoe6o8qax";
	//final static private String APP_SECRET = "urs4c0ylkf9ccew";

	//Accessing entire Dropbox
	//final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

	// You don't need to change these, leave them alone.
	//final static private String ACCOUNT_PREFS_NAME = "prefs";
	//final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	//final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	//DropboxAPI
	//DropboxAPI<AndroidAuthSession> mApi;

	
	boolean firstTime = true;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Android Application context
		mContext = this;
		
		//Time
		Date date = new Date();
        DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
        String newFile = df.format(date);
       
        
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
		int width = size.x;
		int height = size.y;

		//SketchView is the part of the UI 
		//view = new SketchView(this, width, height);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params.leftMargin = 20;
		params.topMargin = 20; 
		params.width = 2*width/5; 
		params.height = 3*height/4;
				
		view = new MergeView(this, 2*width/5, 3*height/4);
		sketching.addView(view, params);
		view.openBitmap("sketch1.xml");
		
		RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params2.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params2.leftMargin = 3*width/5 - 60;
		params2.topMargin = 20; 
		params2.width = 2*width/5; 
		params2.height = 3*height/4;
				
	    view2 = new MergeView(this, 2*width/5, 3*height/4);
		sketching.addView(view2, params2);
		view2.openBitmap("sketch2.xml");
		
		
		RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params3.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params3.leftMargin = width/2 - width/8 - 20;
		params3.topMargin = 3*height/4; 
		params3.width = width/4; 
		params3.height = height/4;
				
		MergeView view3 = new MergeView(this, width/4, height/4);
		sketching.addView(view3, params3);
		
			
		color = (ImageButton) v.findViewById(R.id.button4);
		undo = (ImageButton) v.findViewById(R.id.button5);
		redo = (ImageButton) v.findViewById(R.id.button6);
		clear = (ImageButton) v.findViewById(R.id.button7);
		history = (ImageButton) v.findViewById(R.id.button8);
		save = (ImageButton) v.findViewById(R.id.button9);

		
		color.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				view.changeColor();
			}
		});
		
		undo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				view.undo();
			}
		});
		
		redo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				view.redo();
			}
		});
		
		clear.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				view.clear();
				
			}
		});
		
		history.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				Aggregator a1 = new Aggregator(view.mLayer);
				Aggregator a2 = new Aggregator(view2.mLayer);
					
				int clusterNumber1 = view.mLayer.numberOfStrokes();
				int clusterNumber2 = view2.mLayer.numberOfStrokes();
				
				ArrayList<Cluster> clusters1 = a1.aggregate(10);
				ArrayList<Cluster> clusters2 = a2.aggregate(10);
				
				//now add this to the interface
				view.addClusters(clusters1);
				view2.addClusters(clusters2);
				
				
//				Bundle stroke_data = new Bundle();
//				stroke_data.putParcelable("layer", view.mLayer);
//				Intent intent = new Intent(mContext, HistoryBrowser.class);
//				intent.putExtras(stroke_data);
//				final int result = 1;
//				startActivityForResult(intent, result);
			}
		});
		
		save.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				saveBitmap();
			}
		});
		setContentView(v);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mContext = this;
		isBranch = false;
		//AndroidAuthSession session = mApi.getSession();

		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
//		if (session.authenticationSuccessful()) {
//			try {
//				// Mandatory call to complete the auth
//				session.finishAuthentication();
//				// Store it locally in our app for later use
//				TokenPair tokens = session.getAccessTokenPair();
//				storeKeys(tokens.key, tokens.secret);
//				mLoggedIn = true;
//				showToast("Session Authorized");
//			} catch (IllegalStateException e) {
//				showToast("Couldn't authenticate with Dropbox:"
//						+ e.getLocalizedMessage());
//				System.out.println("Error authenticating");
//			}
//		}

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
									//logOut();
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
	
	
	//Save as an image 
	private void saveBitmap() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Enter Filename");
		alert.setMessage("");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();

				FileOutputStream outStream = null;
				File mediaStorageDir = new File(Environment
						.getExternalStorageDirectory(), "MySketches");
				mediaStorageDir.mkdirs();

				mFile = new File(mediaStorageDir.getPath(), value
						.toString() + ".PNG");
				view.draw(view.mCanvas);
				try {
					outStream = new FileOutputStream(mFile);
					view.mBitmap.compress(Bitmap.CompressFormat.PNG, 100,
							outStream);
					outStream.flush();
					outStream.close();

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
			view.clear();
			return true;

		case R.id.Color:
			view.changeColor();
			return true;

		case R.id.undo:
			view.undo();
			return true;

		case R.id.Redo:
			view.redo();
			return true;

		case R.id.history:
			Bundle stroke_data = new Bundle();
			stroke_data.putParcelable("layer", view.mLayer);
			Intent intent = new Intent(this, HistoryBrowser.class);
			intent.putExtras(stroke_data);
			final int result = 1;
			startActivityForResult(intent, result);
			return true;

		}
		return false;
	}

//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//		super.onActivityResult(requestCode, resultCode, data);
//		String extraData = data.getStringExtra("ComingFrom");
//		System.out.println(extraData);
//
//		if (extraData.equals("File Browser")) {
//			Layer layer = data.getParcelableExtra("layer");
//			System.out.println("layer " + layer.getStrokes().size());
//			view.setLayer(layer);
//
//			String dir = data.getStringExtra("dir");
//			sessionDir = dir;
//
//			if (dir.charAt(dir.length() - 1) == '/') {
//				sessionName = dir.replace(dir.substring(dir.length() - 1), "");
//			}
//			System.out.println("Selected Dir " + dir);
//			isBranch = data.getBooleanExtra("branch", true);
//			mFileName = data.getStringExtra("filename");
//			firstTime = false;
//			/*
//			int number_of_branches = data.getIntExtra("branchNumber", 0);
//			if (isBranch == true) {
//				mBranchNumber = number_of_branches + 1;
//				mFileName = "branch" + mBranchNumber + ".xml";
//			} else {
//				mFileName = "main.xml";	
//				
//			}
//			*/
//			
//			}
//	}

	// From here are the required dropbox functions

//	private void logOut() {
//		// Remove credentials from the session
//		mApi.getSession().unlink();
//
//		// Clear our stored keys
//		clearKeys();
//		// Change UI state to display logged out version
//		mLoggedIn = false;
//	}

	/* toast */
	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		error.show();
	}


}
