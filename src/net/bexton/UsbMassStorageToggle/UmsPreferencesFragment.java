package net.bexton.UsbMassStorageToggle;

import net.bexton.UsbMassStorageToggle.R;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class UmsPreferencesFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        // must update this one manually.
        Preference pref = getPreferenceManager().findPreference("deviceName");
        if(pref != null)
        	pref.setSummary(getPreferenceManager().getSharedPreferences().getString("deviceName", "-"));
    }   
}