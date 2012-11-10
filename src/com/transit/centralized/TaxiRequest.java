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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.codebutler.android_websockets.SocketIOClient;

public class TaxiRequest extends Activity implements OnClickListener {

    private final static boolean DEBUG = true;
    public final static String TAG = "TaxiRequest";
    SocketIOClient mClient;
    Button mSendRequest, mResetFields;
    ImageButton mGeoLocate;
    EditText mName, mPhoneNumber, mAddress;
    Context mContext;

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
                    }
                    try {
                        mClient.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
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
            LocationManager locator = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location l = locator.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (l == null) {
                l = locator.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            Geocoder geo = new Geocoder(getBaseContext(), Locale.getDefault());
            try {
                Address firstAddr = geo.getFromLocation(l.getLatitude(), l.getLongitude(), 1).get(0);
                mAddress.setText(firstAddr.getAddressLine(0) + " " + firstAddr.getAddressLine(1) + " " + firstAddr.getAddressLine(2));
            } catch (IOException e1) {
                e1.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage("Unable to retrieve location");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        }
    }
}