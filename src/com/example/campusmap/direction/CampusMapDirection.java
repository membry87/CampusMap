package com.example.campusmap.direction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class CampusMapDirection {

	private JSONObject jObject;
 
	
	public CampusMapDirection() {
 
	}

	public JSONObject returnJSONObj(){
		return jObject;
	}
	
	public void initializeJSONOject(LatLng start, LatLng end) {

		String url = "http://1.campusgps.sinaapp.com/direction.php?"
				+ "olat=" + start.latitude + "&olng=" + start.longitude
				+ "&dlat=" + end.latitude + "&dlng=" + end.longitude;

		
		
		DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
		HttpGet httppost = new HttpGet(url);
		// Depends on your web service
		httppost.setHeader("Content-type", "application/json");

		InputStream inputStream = null;
		String result = null;
		try {
		    HttpResponse response = httpclient.execute(httppost);           
		    HttpEntity entity = response.getEntity();

		    inputStream = entity.getContent();
		    // json is UTF-8 by default
		    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
		    StringBuilder sb = new StringBuilder();

		    String line = null;
		    while ((line = reader.readLine()) != null)
		    {
		        sb.append(line + "\n");
		        System.out.println("Return JSON: "+line);
		    }
		    result = sb.toString();
		    jObject = new JSONObject(result);
		} catch (Exception e) { 
		    // Oops
		}
		finally {
		    try{
		    	if(inputStream != null)
		    		inputStream.close();
		    	}
		    catch(Exception squish){
		    	
		    }
		}
	}
	
	public int getStatus(){
		int status = -1;
		try {
			status = jObject.getInt("status");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		System.out.println("status-----> "+ status);
		return status;
	}
	
	public int test(){
		int r1, r2 ,r3, r4, r5;
		r1= r2 =r3= r4= r5 = -1;
		try {
			r1 = jObject.getInt("r1_d");
			r2 = jObject.getInt("r2_d");
			r3 = jObject.getInt("r3_d");
			r4 = jObject.getInt("r4_d");
			r5 = jObject.getInt("r5_d");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		System.out.println("testing-----> "+ r1);
		System.out.println("testing-----> "+ r2);
		System.out.println("testing-----> "+ r3);
		System.out.println("testing-----> "+ r4);
		System.out.println("testing-----> "+ r5);
		return r1;
	}

}
