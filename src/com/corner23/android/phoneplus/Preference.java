package com.corner23.android.phoneplus;

import com.corner23.ringtoneplus.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preference extends PreferenceActivity {
	
	public static final String SHARED_PREFS_NAME = "ringtoneplus";
	public static final String PREF_POCKET_MODE = "pref_pocket_mode";
	public static final String PREF_POLITE_MODE = "pref_polite_mode";
	public static final String PREF_VIBRATE_ON_CONNECT = "pref_vibrate_on_connect";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.preferences);
    }

	@Override
	protected void onPause() {
		super.onPause();
		
		Intent serviceIntent = new Intent(this, PhonePlus.class);
		
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		boolean bPocketMode = prefs.getBoolean(PREF_POCKET_MODE, false);
		boolean bPoliteMode = prefs.getBoolean(PREF_POLITE_MODE, false);
		
		if (bPocketMode || bPoliteMode) {
	        startService(serviceIntent);
		} else {
			stopService(serviceIntent);
		}
	}
}
