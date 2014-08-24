package com.pivot.sketch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

@SuppressLint("NewApi")
public class XmlParser {	
	File xmlDirectory;
	File traceFile;
	FileOutputStream fos;
	public boolean  CreateNewXMLFile() {
		xmlDirectory = new File(Environment.getExternalStorageDirectory()+"/VICED/XML/");
		xmlDirectory.mkdirs();
		traceFile = new File(xmlDirectory, "xmlData.xml");
		try {
			fos = new FileOutputStream(traceFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	public boolean writeTraceData(List<Stroke> strokes) {	
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("ink");
			doc.appendChild(rootElement);
			
			for (int i = 0; i < strokes.size(); i++) {
				Element traceElement = doc.createElement("trace");
				
				String trace = new String();
				trace = "";
				List<TouchPoint> points = strokes.get(i).points;
				
				for (int j = 0; j < points.size(); j++) {
					trace = trace +""+points.get(j).x+" "+points.get(j).y;
					if (j < points.size() - 1) {
						trace = trace+",";
					}
				}
				
				Log.d("com.sketchApp", "trace is"+ trace);
				traceElement.appendChild(doc.createTextNode(trace));
				rootElement.appendChild(traceElement);
			}
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(traceFile);
			transformer.transform(source, result);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
 
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);
 
		System.out.println("File saved!");
 
		return false;
		
	}
	
	public boolean readTraceData(ArrayList<Stroke> strokes) {
		return false;
	}
}
