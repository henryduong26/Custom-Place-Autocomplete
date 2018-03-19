package autocomplete.places.com.placesautocomplete;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import autocomplete.places.com.placesautocomplete.adapter.AutoCompleteAdapter;
import autocomplete.places.com.placesautocomplete.model.PlacePredictions;
import autocomplete.places.com.placesautocomplete.utils.VolleyJSONRequest;


public class PickLocationActivity extends AppCompatActivity implements  Response.Listener<String>, Response.ErrorListener,  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    double latitude;
    double longitude;
    private ListView mAutoCompleteList;
    private EditText Address;
    private String GETPLACESHIT = "places_hit";
    private PlacePredictions predictions;
    private Location mLastLocation;
    private AutoCompleteAdapter mAutoCompleteAdapter;
    private int CUSTOM_AUTOCOMPLETE_REQUEST_CODE = 20;
    private static final int MY_PERMISSIONS_REQUEST_LOC = 30;
    private ImageView searchBtn;
    private FragmentManager fragmentManager;
    private String preFilledText;
    private Handler handler;
    private VolleyJSONRequest request;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        if(getIntent().hasExtra("Search Text")) {
            preFilledText = getIntent().getStringExtra("Search Text");
        }

        fragmentManager = getSupportFragmentManager();

        Address = (EditText) findViewById(R.id.adressText);
        mAutoCompleteList = (ListView) findViewById(R.id.searchResultLV);
        searchBtn=(ImageView) findViewById(R.id.search);


        //get permission for Android M
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            fetchLocation();
        } else {

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOC);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            } else {
                fetchLocation();
            }
        }


        //Add a text change listener to implement autocomplete functionality
        Address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // optimised way is to start searching for laction after user has typed minimum 3 chars
                if (Address.getText().length() > 3) {


                    searchBtn.setVisibility(View.GONE);

                    Runnable run = new Runnable() {


                        @Override
                        public void run() {

                            // cancel all the previous requests in the queue to optimise your network calls during autocomplete search
                            MyApplication.volleyQueueInstance.cancelRequestInQueue(GETPLACESHIT);

                            //build Get url of Place Autocomplete and hit the url to fetch result.
                            request = new VolleyJSONRequest(Request.Method.GET, getPlaceAutoCompleteUrl(Address.getText().toString()), null, null, PickLocationActivity.this, PickLocationActivity.this);

                            //Give a tag to your request so that you can use this tag to cancle request later.
                            request.setTag(GETPLACESHIT);

                            MyApplication.volleyQueueInstance.addToRequestQueue(request);

                        }

                    };

                    // only canceling the network calls will not help, you need to remove all callbacks as well
                    // otherwise the pending callbacks and messages will again invoke the handler and will send the request
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    } else {
                        handler = new Handler();
                    }
                    handler.postDelayed(run, 1000);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });

        Address.setText(preFilledText);
        Address.setSelection(Address.getText().length());

        mAutoCompleteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // pass the result to the calling activity
                Intent intent = new Intent();
                intent.putExtra("Location Address", predictions.getPlaces().get(position).getPlaceDesc());
                setResult(CUSTOM_AUTOCOMPLETE_REQUEST_CODE, intent);
                finish();
            }
        });

    }

    /*
        * Create a get url to fetch results from google place autocomplete api.
        * Append the input received from autocomplete edittext
        * Append your current location
        * Append radius you want to search results within
        * Choose a language you want to fetch data in
        * Append your google API Browser key
     */
    public String getPlaceAutoCompleteUrl(String input) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/place/autocomplete/json");
        urlString.append("?input=");
        try {
            urlString.append(URLEncoder.encode(input, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlString.append("&location=");
        urlString.append(latitude + "," + longitude); // append lat long of current location to show nearby results.
        urlString.append("&radius=500&language=en");
        urlString.append("&key=" + "AIzaSyC9e2ucXYRrs6ug5Lu5THhTNrxmOQd0AC0");

        Log.d("FINAL URL:::   ", urlString.toString());
        return urlString.toString();
    }

    @Override
    public void onErrorResponse(VolleyError error) {

        searchBtn.setVisibility(View.VISIBLE);

    }

    @Override
    public void onResponse(String response) {

        searchBtn.setVisibility(View.VISIBLE);
        Log.d("PLACES RESULT:::", response);
        Gson gson = new Gson();
        predictions = gson.fromJson(response, PlacePredictions.class);

        if (mAutoCompleteAdapter == null) {
            mAutoCompleteAdapter = new AutoCompleteAdapter(this, predictions.getPlaces(), PickLocationActivity.this);
            mAutoCompleteList.setAdapter(mAutoCompleteAdapter);
        } else {
            mAutoCompleteAdapter.clear();
            mAutoCompleteAdapter.addAll(predictions.getPlaces());
            mAutoCompleteAdapter.notifyDataSetChanged();
            mAutoCompleteList.invalidate();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                latitude = mLastLocation.getLatitude();
                longitude = mLastLocation.getLongitude();
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void fetchLocation(){
        //Build google API client to use fused location
        buildGoogleApiClient();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission granted!
                    fetchLocation();

                } else {
                    // permission denied!
                    
                    Toast.makeText(this, "Please grant permission for using this app!", Toast.LENGTH_LONG).show();
                }
                return;
            }


        }
    }
}

