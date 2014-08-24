package com.pivot.storage;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ReadMetaData extends AsyncTask<String, Void, Entry> {

	DataDownloadListener dataDownloadListener;

	public static interface DataDownloadListener {
		void dataDownloadedSuccessfully(Entry data);

		void dataDownloadFailed();
	}

	public void setDataDownloadListener(
			DataDownloadListener dataDownloadListener) {
		this.dataDownloadListener = dataDownloadListener;
	}

	String mPath;
	private DropboxAPI<?> mApi;

	private Context mContext;

	private String mErrorMsg;

	public ReadMetaData(Context context, DropboxAPI<?> api, String dropboxPath) {
		mContext = context;
		mApi = api;
		mPath = dropboxPath;

	}

	@Override
	protected Entry doInBackground(String... params) {
		Entry dirent;
		try {
			// By creating a request, we get a handle to the putFile operation,
			// so we can cancel it later if we want to
			if (mPath.equals("/"))
				dirent = mApi.metadata("/", 1000, null, true, null);
			
			else
				dirent = mApi.metadata("/" + mPath + "/", 1000, null, true,
						null);

			return dirent;

		} catch (DropboxUnlinkedException e) {
			// This session wasn't authenticated properly or user unlinked
			mErrorMsg = "This app wasn't authenticated properly.";
		} catch (DropboxFileSizeException e) {
			// File size too big to upload via the API
			mErrorMsg = "This file is too big to upload";
		} catch (DropboxPartialFileException e) {
			// We canceled the operation
			mErrorMsg = "Upload canceled";
		} catch (DropboxServerException e) {
			// Server-side exception. These are examples of what could happen,
			// but we don't do anything special with them here.
			if (e.error == DropboxServerException._401_UNAUTHORIZED) {
				// Unauthorized, so we should unlink them. You may want to
				// automatically log the user out in this case.
			} else if (e.error == DropboxServerException._403_FORBIDDEN) {
				// Not allowed to access this
			} else if (e.error == DropboxServerException._404_NOT_FOUND) {
				// path not found (or if it was the thumbnail, can't be
				// thumbnailed)
			} else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
				// user is over quota
			} else {
				// Something else
			}
			// This gets the Dropbox error, translated into the user's language
			mErrorMsg = e.body.userError;
			if (mErrorMsg == null) {
				mErrorMsg = e.body.error;
			}
		} catch (DropboxIOException e) {
			// Happens all the time, probably want to retry automatically.
			mErrorMsg = "Network error.  Try again.";
		} catch (DropboxParseException e) {
			// Probably due to Dropbox server restarting, should retry
			mErrorMsg = "Dropbox error.  Try again.";
		} catch (DropboxException e) {
			// Unknown error
			mErrorMsg = "Unknown error.  Try again.";
		}
		return null;
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
		error.show();
	}

	protected void onPostExecute(Entry results) {
		if (results != null) {
			dataDownloadListener.dataDownloadedSuccessfully(results);
		} else
			dataDownloadListener.dataDownloadFailed();
	}

}
