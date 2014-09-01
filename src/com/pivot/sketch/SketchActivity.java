package com.pivot.sketch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import edu.pivot.history.HistoryBrowser;

public class SketchActivity extends Activity {

	Context mContext;
		
	// main UI -- all buttons 
	//private SketchView view;
	private MergeView view;
	
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
	public String sessionName = "/VICED/one";

	//App key and secret from Dropbox
	final static private String APP_KEY = "ewvxxygoe6o8qax";
	final static private String APP_SECRET = "urs4c0ylkf9ccew";

	//Accessing entire Dropbox
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

	// You don't need to change these, leave them alone.
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	//DropboxAPI
	DropboxAPI<AndroidAuthSession> mApi;

	
	private boolean mLoggedIn = false;
	private String sessionDir = "";
	private String mFileName;
	
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
        mFileName = "branch_"+newFile+".xml";
		
        
        if (savedInstanceState != null) {
			sessionName = savedInstanceState.getString("sessionName");
		}

		sessionDir = sessionName + "/";
		
		/* Connect to dropbox */
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		checkAppKeySetup();
		mApi.getSession().startAuthentication(SketchActivity.this);
		mLoggedIn = true;

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
		params.height = 3*height/5;
				
		view = new MergeView(this, 2*width/5, 3*height/4);
		sketching.addView(view, params);
		
		RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params2.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params2.leftMargin = 3*width/5 - 50;
		params2.topMargin = 20; 
		params2.width = 2*width/5; 
		params2.height = 3*height/5;
				
		MergeView view2 = new MergeView(this, 2*width/5, 3*height/4);
		sketching.addView(view2, params2);
		
		RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params3.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		params3.leftMargin = width/2 - width/8 - 20;
		params3.topMargin = 3*height/4; 
		params3.width = width/4; 
		params3.height = height/4;
				
		MergeView view3 = new MergeView(this, width/4, height/4);
		sketching.addView(view3, params3);
		
		
		//All the buttons in the UI
		startNewSession = (ImageButton) v.findViewById(R.id.button0);
		browser = (ImageButton) v.findViewById(R.id.button1);
		checkout = (ImageButton) v.findViewById(R.id.button2);
		commit = (ImageButton) v.findViewById(R.id.button3);
		color = (ImageButton) v.findViewById(R.id.button4);
		undo = (ImageButton) v.findViewById(R.id.button5);
		redo = (ImageButton) v.findViewById(R.id.button6);
		clear = (ImageButton) v.findViewById(R.id.button7);
		history = (ImageButton) v.findViewById(R.id.button8);
		save = (ImageButton) v.findViewById(R.id.button9);
		
		//branch = (ImageButton) v.findViewById(R.id.button4);

		startNewSession.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startSession();
			}
		});

		browser.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				browseSession(sessionName);

			}
		});

		checkout.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				checkoutFromSession();
			}
		});

		commit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				commitToSession();
			}
		});

//		branch.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				branchInSession();
//			}
//		});
		
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
				Date date = new Date();
		        DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
		        String newFile = df.format(date);
		        mFileName = "branch_"+newFile+".xml";
			}
		});
		history.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Bundle stroke_data = new Bundle();
				stroke_data.putParcelable("layer", view.mLayer);
				Intent intent = new Intent(mContext, HistoryBrowser.class);
				intent.putExtras(stroke_data);
				final int result = 1;
				startActivityForResult(intent, result);
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
		AndroidAuthSession session = mApi.getSession();

		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();
				// Store it locally in our app for later use
				TokenPair tokens = session.getAccessTokenPair();
				storeKeys(tokens.key, tokens.secret);
				mLoggedIn = true;
				showToast("Session Authorized");
			} catch (IllegalStateException e) {
				showToast("Couldn't authenticate with Dropbox:"
						+ e.getLocalizedMessage());
				System.out.println("Error authenticating");
			}
		}

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
									logOut();
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
	
	
	//Every sketching session has a name
	protected void startSession() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Enter session name");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				sessionName = "/VICED/" + value.toString();
				sessionDir = "/VICED/" + value.toString() + "/";
			}
		});
		alert.show();
		isBranch = false;
		Date date = new Date();
        DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
        String newFile = df.format(date);
        mFileName = "branch_"+newFile+".xml";
        firstTime = true;
        
        view.clear();
	}

	
	//Download a sketch from Dropbox
	protected void checkoutFromSession() {
		
		new AlertDialog.Builder(this).setTitle("Confirm")
		.setMessage("The sketch - " + sessionDir+mFileName+" will be downloaded. Continue?�")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ReadFromDropbox readData = new ReadFromDropbox(mContext, mApi, sessionDir,
						null, mFileName);
				readData.setFileDownloadListener(new FileDownloadListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void fileDownloadedSuccessfully(Layer layer) {
						view.setLayer(layer);
					}

					public void indexFileReadSucessful(int number_of_branches) {
						// not needed right now
					}

					@Override
					public void fileDownloadFailed() {
						// handler failure (e.g network not available etc.)
					}

				});
				readData.execute();
				isBranch = false;
			}
		}).show();
		firstTime = false;
	}

	
	//Save to Dropbox
	protected void commitToSession() {
		
		//handling first upload
		if (firstTime == true) {
			Date date = new Date();
            DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
            String newFile = df.format(date);
            mFileName = "branch_"+newFile+".xml";
            firstTime = false;
            System.out.println("new sketch ");
		}
		
		CheckBox checkbox1 = new CheckBox(this);
		checkbox1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    	isBranch = true;
		    	Date date = new Date();
	            DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
	            String newFile = df.format(date);
	            mFileName = "branch_"+newFile+".xml";
		    }
		});
		checkbox1.setText("Upload as a branch");
		CheckBox checkbox2 = new CheckBox(this);
		checkbox2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    	isBranch = false;
		    	mFileName = "main.xml";
		    }
		});
		checkbox2.setText("Upload as the final submission");
		
		if (isBranch) {
			Date date = new Date();
            DateFormat df = new SimpleDateFormat("dd-kk-mm-ss");
            String newFile = df.format(date);
            mFileName = "branch_"+newFile+".xml";
		} 
		
		
		LinearLayout v = new LinearLayout(this);
		v.addView(checkbox1);
		v.addView(checkbox2);
		v.setOrientation(1);
		new AlertDialog.Builder(this).setTitle("Confirm")
		.setView(v)
		.setMessage("Your sketch will be uploaded to "+ sessionDir+". Continue?")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FileOutputStream outStream = null;
				File mediaStorageDir = new File(Environment
						.getExternalStorageDirectory(), "MySketches");
				mediaStorageDir.mkdirs();

				mFile = new File(mediaStorageDir.getPath(), mFileName.replace(".xml", "")+ ".PNG");
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
				
				// Figure out this logic
				WriteToDropbox writer = new WriteToDropbox(mContext, mApi, sessionDir,
						view.mLayer, mFileName, null);
				WriteToDropbox image_writer = new WriteToDropbox(mContext, mApi, sessionDir,
						view.mLayer, mFileName, mFile);
				writer.execute();
				image_writer.execute();
				isBranch = false;
			}
		}).show();
		
	}
	
	//Allows branching by setting a boolean variable before commit
	protected void branchInSession() {
		isBranch = true;
		commitToSession();
		isBranch = false;
	}
	
	//Simple file browser UI
	protected void browseSession(String session_name) { 
		Intent intent = new Intent(this, FileBrowser.class);
		final int result = 1;
		startActivityForResult(intent, result);

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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		String extraData = data.getStringExtra("ComingFrom");
		System.out.println(extraData);

		if (extraData.equals("File Browser")) {
			Layer layer = data.getParcelableExtra("layer");
			System.out.println("layer " + layer.getStrokes().size());
			view.setLayer(layer);

			String dir = data.getStringExtra("dir");
			sessionDir = dir;

			if (dir.charAt(dir.length() - 1) == '/') {
				sessionName = dir.replace(dir.substring(dir.length() - 1), "");
			}
			System.out.println("Selected Dir " + dir);
			isBranch = data.getBooleanExtra("branch", true);
			mFileName = data.getStringExtra("filename");
			firstTime = false;
			/*
			int number_of_branches = data.getIntExtra("branchNumber", 0);
			if (isBranch == true) {
				mBranchNumber = number_of_branches + 1;
				mFileName = "branch" + mBranchNumber + ".xml";
			} else {
				mFileName = "main.xml";	
				
			}
			*/
			
			}
	}

	// From here are the required dropbox functions

	private void logOut() {
		// Remove credentials from the session
		mApi.getSession().unlink();

		// Clear our stored keys
		clearKeys();
		// Change UI state to display logged out version
		mLoggedIn = false;
	}

	/* Prepares appkey before starting the connection */
	private void checkAppKeySetup() {
		// Check to make sure that we have a valid app key
		if (APP_KEY.startsWith("CHANGE") || APP_SECRET.startsWith("CHANGE")) {
			showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
			finish();
			return;
		}

		// Check if the app has set up its manifest properly.
		Intent testIntent = new Intent(Intent.ACTION_VIEW);
		String scheme = "db-" + APP_KEY;
		String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
		testIntent.setData(Uri.parse(uri));
		PackageManager pm = getPackageManager();
		if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
			showToast("URL scheme in your app's "
					+ "manifest is not set up correctly. You should have a "
					+ "com.dropbox.client2.android.AuthActivity with the "
					+ "scheme: " + scheme);
			finish();
		}
	}

	/* toast */
	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		error.show();
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a
	 * local store, rather than storing user name & password, and
	 * re-authenticating each time (which is not to be done, ever).
	 * 
	 * @return Array of [access_key, access_secret], or null if none stored
	 */
	private String[] getKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a
	 * local store, rather than storing user name & password, and
	 * re-authenticating each time (which is not to be done, ever).
	 */
	private void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}

	private void clearKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session;

		String[] stored = getKeys();
		if (stored != null) {
			AccessTokenPair accessToken = new AccessTokenPair(stored[0],
					stored[1]);
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE,
					accessToken);
		} else {
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
		}

		return session;
	}

}
