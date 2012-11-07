package com.transit.centralized;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.view.MotionEvent;

public class TaxiRequest extends Activity {
	
	Button requestTaxi;
	GPSTracker gps;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.taxi);
		
		CheckBox onLocation = (CheckBox)findViewById(R.id.checkBox1);
		final LinearLayout address = (LinearLayout)this.findViewById(R.id.addressLayout);
		final LinearLayout constantAddress = (LinearLayout)this.findViewById(R.id.constantAddress);
		onLocation.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked)
			{
				if(isChecked)
				{
					address.setVisibility(LinearLayout.GONE);
					constantAddress.setVisibility(LinearLayout.VISIBLE);
				}
				else
				{
					address.setVisibility(LinearLayout.VISIBLE);
					constantAddress.setVisibility(LinearLayout.GONE);
				}
			}
			
		});
		
		
        requestTaxi = (Button) findViewById(R.id.button1);
        
        // show location button click event
        requestTaxi.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {		
				// create class object
		        gps = new GPSTracker(TaxiRequest.this);

				// check if GPS enabled		
		        if(gps.canGetLocation()){
		        	Geocoder geocode = new Geocoder(getBaseContext(), Locale.getDefault());
		        	double latitude = gps.getLatitude();
		        	double longitude = gps.getLongitude();
		        	try {
						List<Address> address = geocode.getFromLocation(latitude,longitude,1);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	
		        }else{
		        	// can't get location
		        	// GPS or Network is not enabled
		        	// Ask user to enable GPS/network in settings
		        	gps.showSettingsAlert();
		        }
				
			}
		});
	}

}
