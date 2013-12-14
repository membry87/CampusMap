package com.example.campusmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import net.simonvt.messagebar.MessageBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.campusmap.database.DB_Operations;
import com.example.campusmap.direction.GoogleRouteTask;
import com.example.campusmap.direction.Route;
import com.example.campusmap.direction.RouteRequestTask;
import com.example.campusmap.file.FileOperations;
import com.example.campusmap.geometry.HaoDistance;
import com.example.campusmap.geometry.NearestPoint;
import com.example.campusmap.location.MyLocation;
import com.example.campusmap.location.MyLocation.LocationResult;
import com.example.campusmap.location.MyLocationTask;
import com.example.campusmap.mapdrawing.BuildingDrawing;
import com.example.campusmap.mapdrawing.BuildingDrawing.Building;
import com.example.campusmap.mapdrawing.MarkerAndPolyLine;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapActivity extends Activity implements OnMapClickListener,
		OnMapLongClickListener, OnInfoWindowClickListener,
		MessageBar.OnMessageClickListener {

	private GoogleMap map;
	private ArrayList<LatLng> LongClickArrayPoints;
	private MyLocation ml;
	private MyLocationTask locationTask;
	private BuildingDrawing bd;
	private Marker currentMarker;
	private Marker LongClickMarkerA;
	private Marker LongClickMarkerB;
	private Polyline LongClickPolyLine;
	private MarkerAndPolyLine marker_polyline;
	private int LongClickCount;
	private GoogleRouteTask googleDirectionTask;
	private Location myLastLocation;
	private RouteRequestTask campusRouteTask;
	private MessageBar mMessageBar;
	private LocationManager lm;
	private String destination;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		setUpBroadCastManager();
		mapInitialization();
		setUpListeners();
		messageBarInit();
		LongClickArrayPoints = new ArrayList<LatLng>();

		// initialize all the builing-drawing on the map
		bd = new BuildingDrawing(map);

		// camera to my current location
		findMyLocation();

		// options for drawing markers and polylines
		marker_polyline = new MarkerAndPolyLine(map);

	}

	private void messageBarInit() {
		mMessageBar = new MessageBar(this);
		mMessageBar.setOnClickListener(this);

	}

	private void GPS_Network_Initialization() {
		// abstract class, define its abstract method
		LocationResult locationResult = new LocationResult() {
			@Override
			public void gotLocation(Location location) {// callback
				if (location != null) {
					myLastLocation = location;
				}
			}
		};
		ml = new MyLocation(this, map, bd);
		ml.setupLocation(this, locationResult);

	}

	private void findMyLocation() {// test, alternative
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String provider = lm.getBestProvider(criteria, true);
		Location myLocation = lm.getLastKnownLocation(provider);

		if (myLocation != null) {
			setUpMyLocationCamera(myLocation, 17);
			myLastLocation = myLocation;
		}
		GPS_Network_Initialization();

	}

	private void setUpMyLocationCamera(Location l, int zoomto) {
		map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
				l.getLatitude(), l.getLongitude())));
		map.animateCamera(CameraUpdateFactory.zoomTo(zoomto));
	}

	private void setUpListeners() {
		map.setOnMapClickListener(this);
		map.setOnMapLongClickListener(this);
		map.setOnInfoWindowClickListener(this);// click snippet
	}

	private void mapInitialization() {
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		map.setMyLocationEnabled(true);
	}

	@Override
	public void onMapClick(LatLng point) {

		if (LongClickCount == 2 || LongClickArrayPoints.size() == 1) {
			clearMarkersAndLine();
		}

		// if the clicked point is in polygon
		if (bd.pointIsInPolygon(point)) {

			Building tmpBuilding = bd.getCurrentTouchedBuilding();
			if (tmpBuilding != null) {
				addCampusBuildingMaker(point, tmpBuilding.getBuildingName(),
						tmpBuilding.getAddress());
			}
		} else {// add a simple marker
			addSimpleMaker(point);
		}

		// Clears the previously MapLongClick position
		LongClickArrayPoints.clear();
		LongClickArrayPoints.add(point);
		LongClickCount++;
	}

	private void addSimpleMaker(LatLng point) {
		MarkerOptions mo = marker_polyline.setupMarkerOptions(point,
				point.latitude + " , " + point.longitude,
				BitmapDescriptorFactory.HUE_RED, null, false);
		currentMarker = map.addMarker(mo);
		currentMarker.showInfoWindow();

	}

	private void addCampusBuildingMaker(LatLng point, String bn, String addr) {

		MarkerOptions mo = marker_polyline.setupMarkerOptions(point, bn,
				BitmapDescriptorFactory.HUE_RED, addr, true);
		currentMarker = map.addMarker(mo);
		currentMarker.showInfoWindow();
	}

	private void setUpBroadCastManager() {
		LocalBroadcastManager.getInstance(this).registerReceiver(
				BuildingNameReceiver, new IntentFilter("GetGoogleDirection"));
	}

	private BroadcastReceiver BuildingNameReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (googleDirectionTask != null) {
				Polyline lastGoogleLine = googleDirectionTask.getDrawnLine();
				if (lastGoogleLine != null) {
					lastGoogleLine.remove();
				}
			}

			String destination = intent.getStringExtra("BuildingName");
			// search the building lat & lng by bn
			
			LatLng to = getCenterPointOfABuildingFromDB(destination);
			
			// start an ansync task
			if (myLastLocation != null) {
				Location fromL = myLastLocation;
				LatLng from = new LatLng(fromL.getLatitude(),
						fromL.getLongitude());
				CallDirection(from, to);
			}
		}


	};

	private LatLng getCenterPointOfABuildingFromDB(String destination) {
		DB_Operations op = new DB_Operations();
		op.open();
		LatLng to = op.getLatLngFromDB(destination);
		op.close();
		return to;
	}
	
	@Override
	public void onMapLongClick(LatLng point) {
		MarkerOptions mo = marker_polyline.setupMarkerOptions(point, null,
				BitmapDescriptorFactory.HUE_RED, null, false);

		// set polyline in the map
		LongClickArrayPoints.add(point);

		if (LongClickCount == 2) {
			clearMarkersAndLine();
		}

		if (LongClickArrayPoints.size() == 1) {
			LongClickMarkerA = map.addMarker(mo);
		} else { // size is 2
			LongClickMarkerB = map.addMarker(mo);
		}

		if (LongClickArrayPoints.size() == 2) {// draw line

			PolylineOptions plo = marker_polyline.setupPolyLineOptions(
					Color.RED, 5, LongClickArrayPoints);
			LongClickPolyLine = map.addPolyline(plo);

			// start google direction async task
			CallDirection(LongClickArrayPoints.get(0),
					LongClickArrayPoints.get(1));
			LongClickArrayPoints.clear();

		}
		LongClickCount++;
	}

	private void clearMarkersAndLine() {
		// remove the last line
		if (LongClickPolyLine != null) {
			LongClickPolyLine.remove();
		}
		// remove the last two markers
		if (LongClickMarkerA != null) {
			LongClickMarkerA.remove();
		}
		if (LongClickMarkerB != null) {
			LongClickMarkerB.remove();
		}
		// remove currentmarker
		if (currentMarker != null) {
			currentMarker.remove();
		}

		LongClickCount = 0;
		if (googleDirectionTask != null) {
			Polyline lastGoogleLine = googleDirectionTask.getDrawnLine();
			if (lastGoogleLine != null) {
				lastGoogleLine.remove();
			}
		}
	}

	private void CallDirection(LatLng from, LatLng to) { // Async task
		googleDirectionTask = new GoogleRouteTask(this, map, from, to);
		googleDirectionTask.execute();
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		
		//destination string
		destination = marker.getTitle();
		
		
		
		alertDialog.setTitle(destination);
		alertDialog.setMessage(marker.getSnippet());

		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Get route",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {

						LatLng from = new LatLng(myLastLocation.getLatitude(),
								myLastLocation.getLongitude());

						//destination center point

						LatLng to = getCenterPointOfABuildingFromDB(destination);
						System.out.println("******start------" + from);
						System.out.println("******end-------"+to);
						campusRouteTask = new RouteRequestTask(
								MapActivity.this, map, from, to, mMessageBar);
						campusRouteTask.execute();
						
					}
				});

		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Stop",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {
						if (ml != null) {
							String returnFileName = ml
									.disableLocationUpdate(locationTask);

							// also draw the route as well
							Route tmpR = new Route(new FileOperations());
							tmpR.showTestRoute(returnFileName, map, Color.RED,false);
						}

					}
				});
		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Start",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {

						// //set up MyLocation.class, for recording
						// GPS_Network_Initialization();
						//
						// Async task, begin the route
						locationTask = new MyLocationTask(ml,destination);
						locationTask.execute();

					}
				});
		alertDialog.setIcon(R.drawable.ic_launcher);
		alertDialog.show();

	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onResume() {

		super.onResume();

	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				BuildingNameReceiver);

		// remove updates from Mylocation listeners
		if (ml != null) {
			ml.removeLMUpdate();
		}
		Toast.makeText(this, "Good Bye!", Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.route_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Bundle b = new Bundle();
		b.putInt("onMsgClick", 3);
		switch (item.getItemId()) {
		case R.id.route1:

			mMessageBar.show("You picked red route! Let's go!", "Stop",
					R.drawable.ic_messagebar_stop, b);

			//start recording
			//....
			break;
		case R.id.route2:
			mMessageBar.show("You picked blue route! Let's go!", "Stop",
					R.drawable.ic_messagebar_stop, b);
			
			//start recording
			//....
			
			break;
		case R.id.route3:
			mMessageBar.show("You picked black route! Let's go!", "Stop",
					R.drawable.ic_messagebar_stop, b);
			
			//start recording
			//....
			
			
			break;

		}
		return true;
	}

	/**
	 * message bar
	 * 
	 */
	@Override
	public void onMessageClick(Parcelable token) {
		Bundle b = (Bundle) token;
		final int onMsgClick = b.getInt("onMsgClick");

		switch (onMsgClick) {
		case 1:

			openOptionsMenu();

			break;
		case 3:
			//stop recording
			
			
			Toast.makeText(this, "Yeah~!", Toast.LENGTH_SHORT).show();
			break;

		}

	}
}
