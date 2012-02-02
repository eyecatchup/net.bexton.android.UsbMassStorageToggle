package net.bexton.android.UsbMassStorageToggle;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class UmsPreferencesActivity extends PreferenceActivity
{	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);	    
	}		
	
}
