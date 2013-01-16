package net.bexton.UsbMassStorageToggle.prefs;

import net.bexton.UsbMassStorageToggle.R;
import net.bexton.UsbMassStorageToggle.core.Constants;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.InputFilter;

public class PreferencesFragment extends PreferenceFragment
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
        
        // limit input       
        EditTextPreference et = (EditTextPreference) getPreferenceManager().findPreference("vibrateMillis");
        if(et != null && et.getEditText() != null)
        {
        	et.getEditText().setFilters(	new InputFilter[]
        									{
        										new InputFilterNumberMinMax(Constants.VibrateMin, Constants.VibrateMax)
        									}
        								);       
        }
    }   
}
