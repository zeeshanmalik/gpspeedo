package org.homelinux.pwarren.gpspeedo;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.WindowManager;

public class GPSPeedo extends Activity {
    private LocationManager lm;
    private LocationListener locationListener;
    private Integer data_points = 2; // how many data points to calculate for
    private TextView tv;
    private Integer units;
    
    private Double[][] positions;
    private Long[] times;
    private Boolean mirror;

    @Override
    /** Called when menu instantiated */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return true;
    }
    
    /** Called when menu accessed */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_toggle:
    		if (mirror) {
    			unMirror();
    		} else {
    			reMirror();
    		}
    		return true;
    	case R.id.kmph:
    		// change to specified units
    		units = R.id.kmph;
    		return true;
    	case R.id.mph:
    		// change to specified units
    		units = R.id.mph;
    		return true;
    	case R.id.knots:
    		// change to specified units
    		units = R.id.knots;
    		return true;
    	case R.id.mps:
    		// change to specified units
    		units = R.id.mps; 
    		return true;
    	}
		return false;
    }
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setup screen content to follow xml layout.
        setContentView(R.layout.main);         
        
        // two arrays for position and time.
        positions = new Double[data_points][2];
        times = new Long[data_points];

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set up speed text view
        tv = (TextView) findViewById(R.id.speed_view);
        tv.setTextSize(240.0f);
        tv.setTextColor(getResources().getColor(R.color.green));
        tv.setText("000");
        
        // set default units to Km/h
        units = R.id.kmph;
        
        // set to non-hud mode
        unMirror();        
                
        // use the LocationManager class to obtain GPS locations
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);    
        locationListener = new MyLocationListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);   
    }

    private void unMirror() {        
    	Typeface seven_seg = Typeface.createFromAsset(getAssets(), "fonts/7seg.ttf");
    	tv.setTypeface(seven_seg);
        mirror = false;
    }
    
    private void reMirror() {
    	Typeface seven_seg = Typeface.createFromAsset(getAssets(), "fonts/7segm.ttf");
    	tv.setTypeface(seven_seg);
    	tv.setText(ReverseString.reverseIt(tv.getText().toString()));
    	mirror = true;
    }
    
    @Override
    public void onPause() {
    	lm.removeUpdates(locationListener);
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    	super.onResume();
    }

    private class MyLocationListener implements LocationListener {
    	Integer counter = 0;
    	
        public void onLocationChanged(Location loc) {	
        	if (loc != null) {
            	String speed_string;
                Double d1;
                Long t1;
                Double speed = 0.0;
            	d1 = 0.0;
            	t1 = 0l;
   	
            	positions[counter][0] = loc.getLatitude();
                positions[counter][1] = loc.getLongitude();
                times[counter] = loc.getTime();
                
                if (loc.hasSpeed()) {
                	speed = loc.getSpeed() * 1.0; // need to * 1.0 to get into a double for some reason...
                	}
                else {
                	try {
                		// get the distance and time between the current position, and the previous position.
                		// using (counter - 1) % data_points doesn't wrap properly
                		d1 = distance(positions[counter][0], positions[counter][1], positions[(counter+(data_points - 1)) % data_points][0], positions[(counter + (data_points -1)) %data_points][1],'K');
                		t1 = times[counter] - times[(counter + (data_points - 1)) % data_points];
                 	} 
                	catch (NullPointerException e) {
                		//all good, just not enough data yet.
                	}
                d1 = d1 * 1000.0; // km -> m
                speed = d1 / t1; // m/s
                }                
                counter = (counter + 1) % data_points;
                
                // convert from m/s to specified units
                switch (units) {
                case R.id.kmph:
                	speed = speed * 3.6d;
                	break;
                case R.id.mph:
                	speed = speed * 2.23693629d;
                	break;
                case R.id.knots:
                	speed = speed * 1.94384449d;
                	break;
                }
                
                speed_string = String.format("%03d",  (int) Math.rint(speed));
                if (mirror) {
                	speed_string = ReverseString.reverseIt(speed_string);
                }
                tv.setText(speed_string);
            }
        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            Log.i("GPSpeedo", "provider disabled : " + provider);
        }

 
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
            Log.i("GPSpeedo", "provider enabled : " + provider);
        }

   
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
            Log.i("GPSpeedo", "status changed : " + extras.toString());
        }
        
        // private functions       
        private double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
        	// haversine great circle distance approximation.
        	double theta = lon1 - lon2;
        	double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        	dist = Math.acos(dist);
        	dist = rad2deg(dist);
        	dist = dist * 60 * 1.1515;
        	if (unit == 'K') {
        		dist = dist * 1.609344;
        	  	} else if (unit == 'N') {
        	  	dist = dist * 0.8684;
        	    } else if (unit == 'M') {
        	    	dist = dist * 1609.344;
        	    }
        	  return (dist);
        	}

        	private double deg2rad(double deg) {
        	  return (deg * Math.PI / 180.0);
        	}

        	private double rad2deg(double rad) {
        	  return (rad * 180.0 / Math.PI);
        	}        	
    }
}

class ReverseString {
	public static String reverseIt(String source) {
		int i, len = source.length();
		StringBuffer dest = new StringBuffer(len);
		
	    	for (i = (len - 1); i >= 0; i--)
	      dest.append(source.charAt(i));
	    return dest.toString();
	  }	
	}
