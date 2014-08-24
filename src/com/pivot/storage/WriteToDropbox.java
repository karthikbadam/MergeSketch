package com.pivot.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.pivot.sketch.Layer;
import com.pivot.sketch.Stroke;
import com.pivot.sketch.TouchPoint;

public class WriteToDropbox extends AsyncTask<Void, Long, Boolean> {

	Document newDoc;
	DocumentBuilderFactory docFactory;
	DocumentBuilder docBuilder;

	private DropboxAPI<?> mApi;
	private String mPath;
	private File mFile;

	private long mFileLen;
	private UploadRequest mRequest;
	private Context mContext;
	private final ProgressDialog mDialog;

	private String mErrorMsg;

	private Layer mLayer;

	@SuppressWarnings("deprecation")
	public WriteToDropbox(Context context, DropboxAPI<?> api,
			String dropboxPath, Layer layer, String fileName, File file) {
		mContext = context.getApplicationContext();

		mApi = api;
		mPath = dropboxPath;

		mLayer = layer;

		File mediaStorageDir = new File(
				Environment.getExternalStorageDirectory(), "MySketches");
		mediaStorageDir.mkdirs();

		mFile = new File(mediaStorageDir.getPath(), fileName);
		
		if (file!=null) {
			mFile = file;
		}
		else {	
		createInkML();
		}
		mDialog = new ProgressDialog(context);
		mDialog.setMax(100);
		mDialog.setMessage("Uploading sketch");
		mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mDialog.setProgress(0);
		mDialog.setButton("Cancel", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// This will cancel the putFile operation
				mRequest.abort();
			}
		});

		mDialog.show();
	}

	private void createImage() {
		// TODO Auto-generated method stub
		
	}

	private void createInkML() {

		List<Stroke> strokes = mLayer.getStrokes();

		docFactory = DocumentBuilderFactory.newInstance();

		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("ink");
			doc.appendChild(rootElement);
			Element layerElement = doc.createElement("layer");
			rootElement.appendChild(layerElement);

			for (int i = 0; i < strokes.size(); i++) {
				Stroke current_stroke = strokes.get(i);

				Element strokeElement = doc.createElement("stroke");

				Element widthElement = doc.createElement("width");
				widthElement.appendChild(doc.createTextNode(""
						+ current_stroke.strokeWidth));
				Element typeElement = doc.createElement("type");
				typeElement.appendChild(doc.createTextNode(""
						+ current_stroke.strokeType));
				Element colorElement = doc.createElement("color");
				colorElement.appendChild(doc.createTextNode(""
						+ current_stroke.color));

				Element traceElement = doc.createElement("trace");
				String trace = new String();
				trace = "";
				List<TouchPoint> points = current_stroke.points;

				for (int j = 0; j < points.size(); j++) {
					trace = trace + "" + points.get(j).getX() + " "
							+ points.get(j).getY();
					trace = trace + ",";
				}

				traceElement.appendChild(doc.createTextNode(trace));
				strokeElement.appendChild(widthElement);
				strokeElement.appendChild(typeElement);
				strokeElement.appendChild(colorElement);
				strokeElement.appendChild(traceElement);
				layerElement.appendChild(strokeElement);
			}

			newDoc = doc;
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(mFile);
			transformer.transform(source, result);
		} catch (Exception e1) {
			e1.printStackTrace();
			showToast("something wrong: " + e1.getMessage());
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			// By creating a request, we get a handle to the putFile operation,
			// so we can cancel it later if we want to
			FileInputStream fis = new FileInputStream(mFile);
			String path = mPath + mFile.getName();
			mRequest = mApi.putFileOverwriteRequest(path, fis, mFile.length(),
					new ProgressListener() {
						@Override
						public long progressInterval() {
							// Update the progress bar every half-second or so
							return 50;
						}

						@Override
						public void onProgress(long bytes, long total) {
							publishProgress(bytes);
						}
					});

			if (mRequest != null) {
				mRequest.upload();
				return true;
			}

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
		} catch (FileNotFoundException e) {
		}
		return false;
	}

	@Override
	protected void onProgressUpdate(Long... progress) {
		int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
		mDialog.setProgress(percent);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mDialog.dismiss();
		if (result) {
			showToast("Image successfully uploaded");
		} else {
			showToast(mErrorMsg);
		}
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
		error.show();
	}

}
