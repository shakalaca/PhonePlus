package com.corner23.android.phoneplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
	
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			SharedPreferences prefs = context.getSharedPreferences(Preference.SHARED_PREFS_NAME, 0);
			if (prefs != null) {
				Intent serviceIntent = new Intent(context, PhonePlus.class);
				boolean bPocketMode = prefs.getBoolean(Preference.PREF_POCKET_MODE, false);
				boolean bPoliteMode = prefs.getBoolean(Preference.PREF_POLITE_MODE, false);
				
				if (bPocketMode || bPoliteMode) {
					context.startService(serviceIntent);
				} else {
					context.stopService(serviceIntent);
				}
			}
		}
	}
}
