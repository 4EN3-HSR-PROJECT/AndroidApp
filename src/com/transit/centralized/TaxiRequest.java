package com.transit.centralized;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.codebutler.android_websockets.SocketIOClient;

public class TaxiRequest extends Activity implements OnClickListener {

    // Debug variables
    private final static boolean DEBUG = true;
    public final static String TAG = "TaxiRequest";

    // Instance variables
    SocketIOClient mClient;
    Button mSendRequest, mResetFields;
    ImageButton mGeoLocate;
    EditText mName, mPhoneNumber, mAddress;
    Context mContext;
    LocateGpsTask mLocateGps;

    // GPS Icon States
    private final static int GPS_DEFAULT = 0;
    private final static int GPS_FOUND = 1;
    private final static int GPS_ERROR = 2;

    private class LocateGpsTask extends AsyncTask<Void, Void, Location> {

        @Override
        protected Location doInBackground(Void... values) {
            LocationManager locator = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locator.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locator.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            return location;
        }

        @Override
        protected void onPostExecute(Location location) {
            Geocoder geo = new Geocoder(getBaseContext(), Locale.getDefault());
            try {
                Address address = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
                mAddress.setText(address.getAddressLine(0) + " " + address.getAddressLine(1) + " " + address.getAddressLine(2));
                setGeoAnimation(false, GPS_FOUND);
            } catch (IOException e1) {
                e1.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage("Unable to retrieve location");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                setGeoAnimation(false, GPS_ERROR);
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.taxi);
        mSendRequest = (Button) findViewById(R.id.sendrequest);
        mResetFields = (Button) findViewById(R.id.resetfields);
        mGeoLocate = (ImageButton) findViewById(R.id.geolocate);
        mName = (EditText) findViewById(R.id.name);
        mPhoneNumber = (EditText) findViewById(R.id.phonenumber);
        mAddress = (EditText) findViewById(R.id.pickupaddress);
        mContext = this;
        mSendRequest.setOnClickListener(this);
        mResetFields.setOnClickListener(this);
        mGeoLocate.setOnClickListener(this);
        mName.setText("Tester");
        mGeoLocate.performClick();
        mPhoneNumber.setText("123-456-7888");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    String getLocationCoords(String strAddress) {
        Geocoder coder = new Geocoder(mContext);
        List<Address> address;
        try {
            address = coder.getFromLocationName(strAddress,1);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            return String.valueOf(location.getLatitude() + "|" + location.getLongitude());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void emitMessage(String msg, JSONArray args) {
        try {
            mClient.emit(msg, args);
        } catch (Exception e) {
            Log.e(TAG, "emitMessage " + msg + " with args " + args.toString(), e);
        }
    }

    private void printLog(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private void setGeoAnimation(boolean animate, int state) {
        if (animate) {
            mGeoLocate.setImageResource(R.drawable.gps_locate);
            ((AnimationDrawable) mGeoLocate.getDrawable()).start();
        } else {
            ((AnimationDrawable) mGeoLocate.getDrawable()).stop();
            int resourceId = 0;
            switch (state) {
            case GPS_DEFAULT:
                resourceId = R.drawable.device_access_location_searching;
                break;
            case GPS_FOUND:
                resourceId = R.drawable.device_access_location_found;
                break;
            case GPS_ERROR:
                resourceId = R.drawable.device_access_location_error;
                break;
            }
            mGeoLocate.setImageResource(resourceId);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSendRequest) {
            mClient = new SocketIOClient(URI.create("http://danesh.it.cx:8080"), new SocketIOClient.Handler() {

                @Override
                public void onConnect() {
                    printLog("Connected!");
                    JSONArray argumentsa = new JSONArray();
                    argumentsa.put("passengers");
                    emitMessage("joinroom", argumentsa);
                    JSONArray jsonArray = new JSONArray();
                    JSONObject jsonObjects = new JSONObject();
                    try {
                        jsonObjects.put("name", mName.getText().toString());
                        jsonObjects.put("number", mPhoneNumber.getText().toString());
                        jsonObjects.put("address", getLocationCoords(mAddress.getText().toString()));
                        jsonArray.put(jsonObjects);
                        emitMessage("requesttaxi", jsonArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                /**
                 *           iosocket.on('pendingRequest', function(message) {
            if(message != null && message.driver != undefined) {
              var form = $(buildPostString(message));
              $('body').append(form);
              $(form).submit();
            } else if (message != null) {
              $('#name').val(message.name);
              $('#phonenumber').val(message.number);
              $('#pickuplocation').val(message.formatted_address);
              $('#block-ui').show();
              $.mobile.loading( 'show', {
                text: 'Searching for available drivers in the area',
                textVisible: true,
                theme: 'a',
                html: ""
              });
            }
          });
        });

                 */
                @Override
                public void on(String event, final JSONArray arguments) {
                    printLog(String.format("Got event %s: %s", event, arguments.toString()));
                    if (event.equals("requestaccepted")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                try {
                                    String driverName = arguments.getJSONObject(0).getString("driver");
                                    String estimatedTime = arguments.getJSONObject(0).getString("distance");
                                    builder.setMessage("Taxi driver " + driverName + " will pick you up in " + estimatedTime);
                                    builder.setPositiveButton(android.R.string.ok, null);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                builder.show();
                            }
                        });
                        try {
                            mClient.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (event.equals("pendingRequest")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), arguments.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onDisconnect(int code, String reason) {
                    printLog(String.format("Disconnected! Code: %d Reason: %s", code, reason));
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error!", error);
                }

            });
            mClient.connect();
        } else if (v == mResetFields) {
            mName.setText("");
            mPhoneNumber.setText("");
            mAddress.setText("");
        } else if (v == mGeoLocate) {
            if (mLocateGps == null || mLocateGps.getStatus().equals(AsyncTask.Status.FINISHED)) {
                mLocateGps = new LocateGpsTask();
            } else if (!mLocateGps.getStatus().equals(AsyncTask.Status.FINISHED) && mLocateGps.cancel(true)) {
                setGeoAnimation(false, GPS_DEFAULT);
                return;
            }
            setGeoAnimation(true, GPS_DEFAULT);
            mLocateGps.execute();
        }
    }
}