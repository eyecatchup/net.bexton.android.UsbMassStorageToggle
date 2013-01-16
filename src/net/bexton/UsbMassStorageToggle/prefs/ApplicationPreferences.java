package net.bexton.UsbMassStorageToggle.prefs;

import java.util.ArrayList;

import net.bexton.UsbMassStorageToggle.core.Constants;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ApplicationPreferences
{
	public ArrayList<String> LUNFilePaths = new ArrayList<String>();
	public ArrayList<String> SDCardPaths = new ArrayList<String>();
	public String SDExtPath = "";
	public String DeviceName = "";
	public boolean SupressMTP;
	public boolean ShowNotification;
	public boolean ShowPopups;
	public boolean AutoMount;
	public boolean WarnMount;
	public int VibrateMillis;
	public boolean LoggingEnabled;
	
	
	public void readFromSharedPrefs(final SharedPreferences preferences)
	{
        LUNFilePaths = explodeStringArray(preferences.getString("lunfilePaths", ""));
        SDCardPaths = explodeStringArray(preferences.getString("sdcardPaths", ""));
        SDExtPath = preferences.getString("sdextPath", "");
        DeviceName = preferences.getString("deviceName", "");
        SupressMTP = preferences.getBoolean("mtpSupport", false);
        ShowNotification = preferences.getBoolean("showNotifications", true);
        ShowPopups = preferences.getBoolean("showPopups", true);
        AutoMount = preferences.getBoolean("autoMount", false);
        WarnMount = preferences.getBoolean("mountWarning", false);
        VibrateMillis = Integer.parseInt(preferences.getString("vibrateMillis", Constants.VibrateMin));
        LoggingEnabled = preferences.getBoolean("loggingEnabled", true);		
	}
	
	public void writeToSharedPrefs(SharedPreferences preferences)
	{
        Editor edit = preferences.edit();
        
        // write LUNfile paths
        edit.putString("lunfilePaths", combineStringArray(LUNFilePaths));

        // write SDCard paths
        edit.putString("sdcardPaths", combineStringArray(SDCardPaths));
              
        // write SD-ext path
        edit.putString("sdextPath", SDExtPath);

        edit.putString("deviceName", DeviceName);
        edit.commit();
	}
	
	
	private static ArrayList<String> explodeStringArray(final String string)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		String[] tmp = string.split(";");
		result.ensureCapacity(tmp.length);
		
        for(int i = 0; i < tmp.length; ++i)
        {
        	result.add(tmp[i]);
        }        
        return result;
	}
	
	private static String combineStringArray(final ArrayList<String> array)
	{
		String result = "";
		
        for(int i = 0; i < array.size(); ++i)
        {
        	result += array.get(i);
        	
        	if(i < (array.size()-1))
        		result += ";";
        }        
        return result;
	}
	
}
