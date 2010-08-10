package org.homelinux.pwarren.gpspeedo;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.WindowManager;

public class GPSPeedo extends Activity {
    private LocationManager lm;
    private LocationListener locationListener;
    private Integer data_points = 2; // how many data points to calculate for
    private TextView tv; // main data view
    private Integer units; // displayed units
    private Double[][] positions;
    private Long[] times;
    private Boolean mirror_pref;
    
    public static final String PREFS_PRIVATE = "PREFS_PRIVATE";
    public static final String KEY_PRIVATE = "KEY_PRIVATE";
    
    public SharedPreferences app_prefs;
    
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
    	case R.id.about_menu:
    		// show about dialog
    		about();
    		return true;
    	case R.id.preferences:
    		// show preferences menu
    		Intent i = new Intent(GPSPeedo.this, Preferences.class);
			startActivity(i);
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
        
        // get shared preferences
        app_prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        // two arrays for position and time.
        positions = new Double[data_points][2];
        times = new Long[data_points];

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set up speed text view
        tv = (TextView) findViewById(R.id.speed_view);
        tv.setTextSize(240.0f);
        tv.setText("000");
                
        // unmirror my default
        unMirror();
        
        // use the LocationManager class to obtain GPS locations
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);    
        locationListener = new MyLocationListener();
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	// retrieve preferences.
    	// Units to display
    	String units_str = app_prefs.getString("units","kmph");
    	units = parseUnits(units_str);
    	
    	// HUD or normal
    	mirror_pref = app_prefs.getBoolean("hud",false);
    	if (mirror_pref) {
    		reMirror();
    	}
    	else {	
    		unMirror();
    	}
    	
    	// display color
    	String color_str = app_prefs.getString("color","Green");
    	Integer parsed_color = parseColor(color_str);
    	Integer color_id = getResources().getColor(parsed_color);
    	tv.setTextColor(color_id);
    	}
    
	@Override
    public void onStop() {
    	super.onStop();
	}
    
    @Override
    public void onPause() {
    	lm.removeUpdates(locationListener);
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    
    // surely there's a better way to handle preferences involving lists of integers?
    public Integer parseUnits(String input) {
    	if (input.equals("kmph")) return R.id.kmph;
    	if (input.equals("mph")) return R.id.mph;
    	if (input.equals("knots"))return R.id.knots;
    	if (input.equals("mps")) return R.id.mps;
    	return null;
    }
    
   private Integer parseColor(String input) {
	   if (input.equals("Red")) return R.color.red;
	   if (input.equals("Green")) return R.color.green;	   
	   if (input.equals("Blue")) return R.color.blue;
	   if (input.equals("White")) return R.color.white;
	   return null;
	}

    private void unMirror() {        
    	Typeface seven_seg = Typeface.createFromAsset(getAssets(), "fonts/7seg.ttf");
    	tv.setTypeface(seven_seg);    	
    }
    
    private void reMirror() {
    	Typeface seven_seg = Typeface.createFromAsset(getAssets(), "fonts/7segm.ttf");
    	tv.setTypeface(seven_seg);
    	tv.setText(ReverseString.reverseIt(tv.getText().toString()));
    }
    
    private void about() {
    	// show about dialogue.
    	AlertDialog builder;
    	try {
    		builder = AboutDialogBuilder.create(this);
    		builder.show();
    	} catch (NameNotFoundException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	
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
                		d1 = distance(positions[counter][0], positions[counter][1], positions[(counter+(data_points - 1)) % data_points][0], positions[(counter + (data_points -1)) %data_points][1]);
                		t1 = times[counter] - times[(counter + (data_points - 1)) % data_points];
                 	} 
                	catch (NullPointerException e) {
                		//all good, just not enough data yet.
                	}
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
                if (mirror_pref) {
                	speed_string = ReverseString.reverseIt(speed_string);
                }
                tv.setText(speed_string);
            }
        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            Log.i(getResources().getString(R.string.app_name), "provider disabled : " + provider);
        }

 
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
            Log.i(getResources().getString(R.string.app_name), "provider enabled : " + provider);
        }

   
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
            Log.i(getResources().getString(R.string.app_name), "status changed : " + extras.toString());
        }
        
        // private functions       
        private double distance(double lat1, double lon1, double lat2, double lon2) {
        	// haversine great circle distance approximation, returns meters
        	double theta = lon1 - lon2;
        	double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        	dist = Math.acos(dist);
        	dist = rad2deg(dist);
        	dist = dist * 60; // 60 nautical miles per degree of seperation
        	dist = dist * 1852; // 1852 meters per nautical mile  
        	return (dist);
        	}

        	private double deg2rad(double deg) {
        	  return (deg * Math.PI / 180.0);
        	}

        	private double rad2deg(double rad) {
        	  return (rad * 180.0 / Math.PI);
        	}        	
    }
    
    public static class AboutDialogBuilder {
    	// simple dialog builder.
    	
    	public static AlertDialog create( Context context ) throws NameNotFoundException {
    		// grab the package details
    		PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
    		
    		// stuff the info into useful strings
    		String about_title = String.format("About %s", context.getString(R.string.app_name));
    		String version_string = String.format("Version: %s", pInfo.versionName);
    		String about_text = context.getString(R.string.about_string);
    				
    		final TextView about_view = new TextView(context);
    		
    		final SpannableString s = new SpannableString(about_text);
    		about_view.setPadding(3, 1, 3, 1);
    		about_view.setText(version_string + "\n" + s);
    		
    		// turn "http://*" into a clickable link
    		Linkify.addLinks(about_view, Linkify.ALL);

    		// build and return the dialog
    		return new AlertDialog.Builder(context).setTitle(about_title).setCancelable(true).setIcon(R.drawable.icon).setPositiveButton(
    			 context.getString(android.R.string.ok), null).setView(about_view).create();
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
