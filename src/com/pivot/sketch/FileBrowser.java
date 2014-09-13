package com.pivot.sketch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import com.pivot.storage.ReadFromDropbox.FileDownloadListener;
import com.pivot.storage.ReadMetaData;
import com.pivot.storage.ReadMetaData.DataDownloadListener;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileBrowser extends ListActivity {

	private List<String> item = null;
	private List<String> path = null;
	private List<Entry> thumbs = null;

	private String root = "/VICED";
	private TextView myPath;

	private String selectedFileDir = "/VICED";
	final static private String APP_KEY = "ewvxxygoe6o8qax";
	final static private String APP_SECRET = "urs4c0ylkf9ccew";


	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

	// You don't need to change these, leave them alone.
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	DropboxAPI<AndroidAuthSession> mApi;

	private boolean mLoggedIn = false;

	private String mErrorMsg;

	/** Called when the activity is first created. */

	AndroidAuthSession session;

	Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this.getApplicationContext();
		session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		checkAppKeySetup();
		mApi.getSession().startAuthentication(FileBrowser.this);
		mLoggedIn = true;

		session = mApi.getSession();

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

		myPath = (TextView) findViewById(R.id.path);
		setContentView(R.layout.file_browser);

		Button refresh = (Button) findViewById(R.id.refresh_button);
		refresh.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// This logs you out if you're logged in, or vice versa
				if (mLoggedIn) {
					getDir(selectedFileDir);
				}
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		if (mLoggedIn) {
			getDir(selectedFileDir);
		}
	}

	// End Activity when user clicks back
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Handle the back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			logOut();
			Intent intent = new Intent();
			intent.putExtra("ComingFrom", "Back to main");
			setResult(RESULT_OK, intent);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	Entry dirent;

	/*get files in a directory dirPath*/
	private boolean getDir(String dirPath) {
		try {
			ReadMetaData data = new ReadMetaData(this, mApi, dirPath);
			data.setDataDownloadListener(new DataDownloadListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void dataDownloadedSuccessfully(Entry data) {
					fillList(data);
				}

				@Override
				public void dataDownloadFailed() {
					// handler failure (e.g network not available etc.)
				}
			});
			data.execute();

		} catch (Exception e) {
			showToast("file not found error: " + e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	protected void fillList(Entry data) {

		root = "/";
		dirent = data;
		if (!dirent.isDir || dirent.contents == null) {
			// It's not a directory, or there's nothing in it
			mErrorMsg = "File or empty directory";
			showToast(mErrorMsg);
		}
		// Make a list of everything in it that we can get a thumbnail for
		thumbs = new ArrayList<Entry>();
		for (Entry ent : dirent.contents) {
			if (ent.thumbExists) {
				// Add it to the list of thumbs we can choose from
				thumbs.add(ent);
			}
		}

		item = new ArrayList<String>();
		path = new ArrayList<String>();

		if (!(dirent.fileName().equals(""))) {
			item.add(root);
			path.add(root);
			item.add("../");
			path.add(dirent.parentPath());
		}
		for (Entry ent : dirent.contents) {
			if (ent.isDir) {
				item.add(ent.fileName() + "/");
				path.add(ent.path);
			}
			else if (ent.fileName().contains("xml")) {
				if (!ent.fileName().contains("PNG")) {
					path.add(ent.path);
					item.add(ent.fileName());
				}
			}
				
		}

		ArrayAdapter<String> fileList = new ArrayAdapter<String>(this,
				R.layout.file_browser_row, item);
		setListAdapter(fileList);
	}

	private void handleDirectory(Entry file, int position) {
		
		if (file.isDir) {
			selectedFileDir = file.path;
			if (!file.contents.isEmpty())
				getDir(path.get(position));
			else {
				new AlertDialog.Builder(this)
						.setTitle(file.fileName() + "is empty!")
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();
			}
		}
	}

	Entry mFile;
	boolean isBranch = false;
	String fileName = "";
	
	private void handleFile(Entry file, int position) {
		mFile = file;
//		CheckBox checkbox = new CheckBox(this);
//		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//		    @Override
//		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//		    	if (isBranch == false) {
//		    		isBranch = true;
//		    	}
//		    }
//		});
//		checkbox.setText("Check out as a branch");

		new AlertDialog.Builder(this).setTitle(file.fileName()+ " has been selected. Continue?")
				//.setView(checkbox)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						fileName = mFile.fileName();
						readFile(mFile);
					}
				}).show();

	}

	public Layer mLayer;
	private int branches = 0;
	
	protected void readFile(Entry file) {
		selectedFileDir = file.parentPath();
		/*
		ReadFromDropbox readIndexData = new ReadFromDropbox(this, mApi, file.parentPath(), null, "index.txt");
		readIndexData.setFileDownloadListener(new FileDownloadListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void fileDownloadedSuccessfully(Layer layer) {
			}
			
			@Override
			public void indexFileReadSucessful(int number_of_branches) {
				//branches = number_of_branches;
			}
			
			@Override
			public void fileDownloadFailed() {
				// handler failure (e.g network not available etc.)
			}

		});
		readIndexData.execute();
		*/
			
		ReadFromDropbox readData = new ReadFromDropbox(this, mApi, file.parentPath(),
				file, file.fileName());
		readData.setFileDownloadListener(new FileDownloadListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void fileDownloadedSuccessfully(Layer layer) {
				mLayer = layer;
				returnLayer();
			}
			
			@Override
			public void indexFileReadSucessful(int number_of_branches) {
			}
			
			@Override
			public void fileDownloadFailed() {
				// handler failure (e.g network not available etc.)
			}

		});

		readData.execute();
	}

	// Returns Layer to the Sketch Activity
	protected void returnLayer() {
		logOut();
		Intent intent = new Intent();
		intent.putExtra("ComingFrom", "File Browser");
		intent.putExtra("layer", mLayer);
		intent.putExtra("dir", selectedFileDir);
		intent.putExtra("branch", isBranch);
		intent.putExtra("filename", fileName);
		//intent.putExtra("branchNumber", branch);
		setResult(RESULT_OK, intent);
		finish();
	}

	int position;

	@Override
	protected void onListItemClick(ListView l, View v, int pos, long id) {
		this.position = pos;
		try {
			ReadMetaData data = new ReadMetaData(this, mApi, path.get(position));
			data.setDataDownloadListener(new DataDownloadListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void dataDownloadedSuccessfully(Entry data) {
					if (data.isDir) {
						handleDirectory(data, position);
					} else {
						handleFile(data, position);
					}
				}

				@Override
				public void dataDownloadFailed() {
					// handler failure (e.g network not available etc.)
				}
			});
			data.execute();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		;
	}

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
