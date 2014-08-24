package com.pivot.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.ThumbFormat;
import com.dropbox.client2.DropboxAPI.ThumbSize;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.pivot.sketch.Layer;
import com.pivot.sketch.Stroke;
import com.pivot.storage.ReadMetaData.DataDownloadListener;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ReadFromDropbox extends AsyncTask<Void, Long, Boolean> {

	private Context mContext;
	private Layer mLayer;
	private final ProgressDialog mDialog;
	private DropboxAPI<?> mApi;
	private String mPath;
	private Entry mFileData;
	private String mFileName;
	private UploadRequest mRequest;
	private boolean mCanceled;
	private String mErrorMsg;
	private double mFileLen;

	private File mFile;

	FileDownloadListener fileDownloadListener;

	public static interface FileDownloadListener {
        void fileDownloadedSuccessfully(Layer layer);
        void indexFileReadSucessful(int number_of_branches); 
        void fileDownloadFailed();
    }

	public void setFileDownloadListener(FileDownloadListener fileDownloadListener) {
        this.fileDownloadListener = fileDownloadListener;
    }
	
	/*
	 * Read File from Dropbox
	 */
	public ReadFromDropbox(Context context, DropboxAPI<?> api,
			String dropboxPath, Entry file, String fileName) {

		mApi = api;
		mContext = context;
		mPath = dropboxPath;
		mFileName = fileName;
		mFileData = file;
		
		File mediaStorageDir = new File(Environment
				.getExternalStorageDirectory(), "MySketches");
		mediaStorageDir.mkdirs();
		
		
		if (fileName == null) {
			mFileName = mFileData.fileName();
		}

		mFile = new File(mediaStorageDir.getPath(), mFileName);
	
		try {
			mFile.createNewFile();
		} catch (IOException e) {
			mErrorMsg = "File creationIO Exception";
			e.printStackTrace();
		};
		
		mDialog = new ProgressDialog(mContext);
		mDialog.setMax(100);
		mDialog.setMessage("Downloading Sketch");
		mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mDialog.setProgress(0);
		mDialog.setButton("Cancel", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// This will cancel the putFile operation
				mCanceled = true;
				mErrorMsg = "Canceled";
				mRequest.abort();
			}
		});
		if (!mFileName.equals("index.txt")) {
			mDialog.show();
		}
		
	}
	
	String revision = null;

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			if (mCanceled) {
				return false;
			}
			
			// Get the metadata for a directory
			if (mFileData == null) {
				mFileData = mApi.metadata(mPath+mFileName, 1,
						null, false, null);
			}

			if (mFileData.isDir) {
				// It's not a directory, or there's nothing in it
				mErrorMsg = "Its a directory";
				return false;
			}

			if (mCanceled) {
				return false;
			}
			
			mFileLen = mFileData.bytes;
			
			FileOutputStream fos = new FileOutputStream(mFile);
			
			Log.i("com.pivot.sketch", "The file is: " + mPath+mFileName);
			
			
			DropboxFileInfo info = mApi.getFile(mFileData.path, null, fos,
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
			Log.i("com.pivot.sketch", "The file's rev is: " + mFileData.rev);
			return true;
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// The AuthSession wasn't properly authenticated or user unlinked.
		} catch (DropboxPartialFileException e) {
			// We canceled the operation
			mErrorMsg = "Download canceled";
		} catch (DropboxServerException e) {
			// Server-side exception. These are examples of what could happen,
			// but we don't do anything special with them here.
			if (e.error == DropboxServerException._304_NOT_MODIFIED) {
				// won't happen since we don't pass in revision with metadata
			} else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
				// Unauthorized, so we should unlink them. You may want to
				// automatically log the user out in this case.
			} else if (e.error == DropboxServerException._403_FORBIDDEN) {
				// Not allowed to access this
			} else if (e.error == DropboxServerException._404_NOT_FOUND) {
				// path not found (or if it was the thumbnail, can't be
				// thumbnailed)
			} else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
				// too many entries to return
			} else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
				// can't be thumbnailed
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
		return false;
	}

	@Override
	protected void onProgressUpdate(Long... progress) {
		int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
		mDialog.setProgress(percent);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			if (mFileName.equals("index.txt")) {
				int number_of_branches = processIndexFile();
				fileDownloadListener.indexFileReadSucessful(number_of_branches);
			} else {
				mDialog.dismiss();
				convertXMLtoLayer();
			fileDownloadListener.fileDownloadedSuccessfully(mLayer);
			}
		} else {
			// Couldn't download it, so show an error
			showToast(mErrorMsg);
		}
	}

	private int processIndexFile() {
		int number_of_branches = 0;
		FileInputStream fis;
		try {
			fis = new FileInputStream(mFile);
			InputStreamReader isr = new InputStreamReader(fis);
	    	BufferedReader br = new BufferedReader(isr);  
			String line = br.readLine();
			if (line!= null) {
				number_of_branches = Integer.parseInt(line);
			}
			System.out.println("Branches " + number_of_branches);
			br.close();
			fis.close();
			isr.close();
			return number_of_branches;
		
		} catch (FileNotFoundException e) {
			mErrorMsg = "File not found";
			e.printStackTrace();
		} catch (IOException e) {
			mErrorMsg = "IO Exception";
			e.printStackTrace();
		}
     	showToast(mErrorMsg);
		return number_of_branches;
	}

	private void convertXMLtoLayer() {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		mLayer = new Layer();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(mFile));
			doc.getDocumentElement().normalize();
			
			NodeList layer_nodes = doc.getElementsByTagName("layer");
			
			for (int i = 0; i < layer_nodes.getLength(); i++) {
				Element layer_node = (Element) layer_nodes.item(i);
				NodeList stroke_nodes = layer_node.getElementsByTagName("stroke");
				for (int j = 0; j < stroke_nodes.getLength(); j++) {
					Node stroke_node =  stroke_nodes.item(j);
					Element stroke_element = (Element)stroke_node;
					//Getting Width
					NodeList width_nodes = stroke_element.getElementsByTagName("width");
					Element width_element = (Element) width_nodes.item(0);
					
					NodeList width_values = width_element.getChildNodes();
					Float width =  Float.valueOf(width_values.item(0).getNodeValue());
					
					//Getting type
					
					NodeList type_nodes = stroke_element.getElementsByTagName("type");
					Element type_node = (Element) type_nodes.item(0);
					NodeList type_values = type_node.getChildNodes();
					String type = "";
					type = type_values.item(0).getNodeValue();
					
					
					//Getting color
					NodeList color_nodes = stroke_element.getElementsByTagName("color");
					Element color_node = (Element) color_nodes.item(0);
					NodeList color_values = color_node.getChildNodes();
					int color = Integer.valueOf(color_values.item(0).getNodeValue());
					
					Stroke stroke = new Stroke();
					
					stroke.setColor(color);
					if (type.equals("STROKE"))
						stroke.setStrokeType(Paint.Style.STROKE);
					stroke.setStrokeWidth(width);
					
					//Getting trace
					NodeList trace_nodes = stroke_element.getElementsByTagName("trace");
					Element trace_node = (Element) trace_nodes.item(0);
					NodeList trace_values = trace_node.getChildNodes();
					String trace = trace_values.item(0).getNodeValue();
					String[] touch_points = trace.split(",");
					boolean is_end = false;
					for (int k = 0; k < touch_points.length; k++) {
						String[] touch_coordinates = touch_points[k].split(" ");
						Float x = Float.valueOf(touch_coordinates[0]);
						Float y = Float.valueOf(touch_coordinates[1]);
						
						stroke.addTouchPoint(x, y, is_end);
					}		
					mLayer.addStroke(stroke);
				}
			}
			
		} catch (ParserConfigurationException e) {
			mErrorMsg = "Parser Error";
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			mErrorMsg = "File not found";
			e.printStackTrace();
		} catch (SAXException e) {
			mErrorMsg = "SAX exception";
			e.printStackTrace();
		} catch (IOException e) {
			mErrorMsg = "IO Exception";
			e.printStackTrace();
		}
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
		error.show();
	}

}
