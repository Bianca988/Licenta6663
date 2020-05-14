package com.example.licenta666;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.indooratlas.android.sdk.resources.IAResult;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import com.amazonaws.mobile.client.Callback;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import java.util.ArrayList;
import static com.example.licenta666.R.id.map;
public class MapsOverlayActivity<IAResourceManager, IATask> extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsOverlayActivity.class.getSimpleName();

    //current location info
    private String mCurrentFloorPlanId;

    //destination location info
    private ArrayList<Location> mDestinationLocation;
    //destination markers
    private ArrayList<Marker> mDestinationMarker;
    // Maximum dimension of the floor plan image
    private static final int MAX_DIMENSION = 2048;

    private GoogleMap mGoogleMap;
    private Marker mMapMarker;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private IALocationManager mIALocationManager;
    public IAResourceManager mResourceManager;
    public IATask mFetchFloorPlanTask;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true;
    //-------------------LISTENER 1
    private IALocationListener mLocationListener = new IALocationListenerSupport() {
        Location mCurrLocation;//current location

        public void onLocationChanged(IALocation location) {
            mCurrLocation = getLocationFromFloorId(mCurrentFloorPlanId);
            // if(!mPref.getString("destinationId", "none").equalsIgnoreCase("none") && mCurrentFloorPlanId != null) {
            LatLng updatedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mGoogleMap == null) {
                return;
            }
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mCameraPositionNeedsUpdating) {
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                final String newId = region.getId();
                mCurrentFloorPlanId = region.getId();
                if (mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // move camera to new floor plan.
                    if (mGroundOverlay != null) {
                        mGroundOverlay.remove();
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region;
                    region.getFloorPlan();
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }
            }
        }

        @Override
        public void onExitRegion(IARegion iaRegion) {
            if (mGroundOverlay != null) {
                mGroundOverlay.setTransparency(0.5f);
            }
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        findViewById(android.R.id.content).setKeepScreenOn(true);


        FloatingActionButton searchFab = (FloatingActionButton) findViewById(R.id.search_fab);
        searchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchIntent = new Intent(MapsOverlayActivity.this, SearchActivity.class);
                startActivity(searchIntent);
            }
        });
        mIALocationManager = IALocationManager.create(this);
       // mResourceManager = IAResourceManager.create(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleMap == null) {

            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(map))
                    .getMapAsync(this);
        }

        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mLocationListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap = googleMap;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIALocationManager.removeLocationUpdates(mLocationListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    /**
     * Sets floor plan image retrieved from IndoorAtlas as ground overlay on Google Maps
     * Note - this method definition is based on IndoorAtlas example code.
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {
        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }
        if (mGoogleMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()

                    .image(bitmapDescriptor)
                    .zIndex(0.0f)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());
            mGroundOverlay = mGoogleMap.addGroundOverlay(fpOverlay);

        }


    }

    private void getFloorPlanBitmap(final IAFloorPlan floorPlan) {
        final String floorPlanUrl = floorPlan.getUrl();
        if (mLoadTarget == null) {
            mLoadTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Log.d(TAG, "" + bitmap.getWidth() + "x" + bitmap.getHeight() + "y");
                    setupGroundOverlay(floorPlan, bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    Log.d(TAG, "FAIL TO LOAD BITMAP");
                    mOverlayFloorPlan = null;
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };

        }
        RequestCreator requestFloorPlan = Picasso.get().load(floorPlanUrl);

        final int imageWidth = floorPlan.getBitmapWidth();
        final int imageHeight = floorPlan.getBitmapHeight();

        if (imageHeight > MAX_DIMENSION) {
            requestFloorPlan.resize(0, MAX_DIMENSION);
        } else if (imageWidth > MAX_DIMENSION) {
            requestFloorPlan.resize(MAX_DIMENSION, 0);
        }
        requestFloorPlan.into(mLoadTarget);
    }

    public void getFloorPlan(String id){
        //if there is already running task cancel it
        cancelPendingNetworkCalls();
        final IATask indoorAtlasTask = null;
        indoorAtlasTask.setCallback(new IAResult<IAFloorPlan>()
        {
            public void onResult(IAResult<IAFloorPlan> result)
            {
                if(result.isSuccess() && result.getResult() != null){
                getFloorPlanBitmap(result.getResult());
                }else
                {
                    if(indoorAtlasTask.isCancelled())
                    {
                        Log.d(TAG,"FAIL"+result.getError());
                        mOverlayFloorPlan = null;
                    }
                }
            }

        },Looper.getMainLooper());
        mFetchFloorPlanTask = indoorAtlasTask;
    }
    private void cancelPendingNetworkCalls()
    {
        if(mFetchFloorPlanTask != null && mFetchFloorPlanTask.isCancelled())
        {
            mFetchFloorPlanTask.cancel();
        }
    }
    private Location getLocationFromFloorId(String floorId)
    {
        Location currLocation = mLocationRealm.where(Location.class).equalTo("floorid",floorId).findFirst();
        if(currLocation != null)
        {
            return currLocation;
        }
        else{
            Log.d(TAG,"");
            return null;
        }
    }
}


