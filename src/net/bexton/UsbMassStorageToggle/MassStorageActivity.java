package net.bexton.UsbMassStorageToggle;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


import net.bexton.UsbMassStorageToggle.R;
import net.bexton.UsbMassStorageToggle.battery.BatteryStateListener;
import net.bexton.UsbMassStorageToggle.battery.BatteryStateReceiver;
import net.bexton.UsbMassStorageToggle.battery.USBConnectionState;
import net.bexton.UsbMassStorageToggle.core.Constants;
import net.bexton.UsbMassStorageToggle.core.Logger;
import net.bexton.UsbMassStorageToggle.prefs.ApplicationPreferences;
import net.bexton.UsbMassStorageToggle.prefs.MassStoragePreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;


public class MassStorageActivity extends Activity implements OnClickListener
{
	public class ActivityFlags
	{
		public final static String FlagHide = "FlagHide";
		public final static String FlagToggleState = "FlagToggleState";
	};
	
    private static final String ClassTag = "USB Mass Storage";

    private static MassStorageActivity instance = null;
    
    
    private ApplicationPreferences appPrefs;
    private SharedPreferences sharedPreferences;
    
    private BatteryStateReceiver batteryBCReceiver;
    private USBConnectionState connectionState;
    private boolean usbStorageActive;
  
    
    private NotificationManager notificationManager;
    private String lastNotification;
        
    private boolean isSetup;
	


    private String StrAppname;
    private String StrUSBStorageActive;
    private String StrUSBCablePlugged;
    private String StrUSBCableUnplugged;
    private TextView UITextUMSState;
    private View UIToggleStateButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initialize();
    }
    
    private void initialize()
    {
    	isSetup = true;
    	instance = null;
    	
        usbStorageActive = false;
        
        appPrefs = new ApplicationPreferences();
       
        setContentView(R.layout.layout);
        
        connectionState = null;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
   
        ////////////////////////////////////////////////////////////
        // BATTERY STATE
        batteryBCReceiver = new BatteryStateReceiver();
        batteryBCReceiver.addListener(new BatteryStateListener()
        {
        	@Override
        	public void onBatteryStatusChanged(USBConnectionState state)
        	{
        		MassStorageActivity.this.onBatteryStateChanged(state);
        	}
        });
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryBCReceiver, filter);
        
        // init battery state from last state
		Intent intent = getApplicationContext().registerReceiver(null, filter);	
        USBConnectionState state = USBConnectionState.createFromIntent(intent);
        onBatteryStateChanged(state);
        ////////////////////////////////////////////////////////////
 
        ////////////////////////////////////////////////////////////
        // APP RESOURCES
        StrAppname = getString(R.string.AppName);
        StrUSBStorageActive = getString(R.string.USBStorageActive);
        StrUSBCablePlugged = getString(R.string.USBCablePlugged);
        StrUSBCableUnplugged = getString(R.string.USBCableUnplugged);
        getString(R.string.ErrorNoRoot);

        UITextUMSState = (TextView)findViewById(R.id.UI_TextUMSState);
        UIToggleStateButton = findViewById(R.id.UI_ToggleStateButton);
        UIToggleStateButton.setOnClickListener(this);
        ////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////
        // DEVICE STATS
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
        String setupResult = "";

        if(appPrefs.LUNFilePaths.isEmpty() || appPrefs.SDCardPaths.isEmpty())
        {
            Logger.logI(ClassTag, "Gathering device specific setup...");
            setupResult = detectDeviceStats();
        }
        else
        {
            Logger.logI(ClassTag, "Device specific settings loaded from shared preferences.");
        }
        
        logDeviceStats();
        detectMountState();
        ////////////////////////////////////////////////////////////
        
        ////////////////////////////////////////////////////////////
        // PREFERENCES
        // register listener to update them from preferences-activity
        sharedPreferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener()
        {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                onPreferencesUpdated();
            }
        });

        // update once
        onPreferencesUpdated();
        ////////////////////////////////////////////////////////////
        
        ////////////////////////////////////////////////////////////
        // SETUP
        if(isSetup)
        {
            Logger.logI(ClassTag, "Device specific setup successfully completed.");
            instance = this;
        }
        else
        {
            if(setupResult != "")
            {
                Logger.logW(ClassTag, "Device specific setup failed. Usage disabled and prompt for setup.");
                
                showNotification(setupResult);
                showPopup(setupResult);
            }
        }
        ////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////
        // FLAGS
        // do as set by the intent the activity got started from.
        intent = getIntent();
       
        if(intent.hasExtra(ActivityFlags.FlagHide))
        {
            hideActivity();
        }
                
        if(intent.hasExtra(ActivityFlags.FlagToggleState))
        {
            toggleMount();
        }
        ////////////////////////////////////////////////////////////
    }

    protected void onBatteryStateChanged(USBConnectionState state)
	{
    	boolean previouslyConnected = connectionState != null ? connectionState.isConnected() : usbStorageActive;
    	connectionState = state;
    	
        // autoMount (if cable got plugged in)
        if(state.isConnected())
        {
            if(appPrefs.AutoMount)
            {
                if(!previouslyConnected)
                    doMount();
            }

            // ready, but not enabled
            if(!usbStorageActive)
                showNotification(StrUSBCablePlugged);
            else // enabled
                showNotification(StrUSBStorageActive);
        }
        else // not connected anymore
        {
            if(usbStorageActive)
            {
                // force unmount, because disconnected
                doUnmount(true);
                showNotification(StrUSBCableUnplugged);
            }
            else
            {
                showNotification(StrUSBCablePlugged);
            }
        }		
	}

	@Override
    public void onBackPressed()
    {
        hideActivity();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        finishUp();
    }

    private void hideActivity()
    {
        moveTaskToBack(true);
    }

    private void finishUp()
    {
        if(batteryBCReceiver != null)
        {
            unregisterReceiver(batteryBCReceiver);
            batteryBCReceiver = null;
        }

        finish();
    }

    public void onClick(View v)
    {
        if(v.getId() == R.id.UI_ToggleStateButton)
        {
            toggleMount();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);

        switch(item.getItemId())
        {
            case R.id.ActionSettings:
            {
                Intent intent = new Intent(getBaseContext(), MassStoragePreferences.class);
                startActivity(intent);
                return true;
            }
            case R.id.ActionWebLink:
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/showthread.php?t=1389375"));
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private void onPreferencesUpdated()
    {
    	// store old values
        boolean lastNotifSetting = appPrefs.ShowNotification;
        boolean lastAutoMountSetting = appPrefs.AutoMount;

        // update
        appPrefs.readFromSharedPrefs(sharedPreferences);

        
        // use new values
        Logger.setEnabled(appPrefs.LoggingEnabled);

        if(appPrefs.ShowNotification != lastNotifSetting)
        {
            // re-post last notification
            if(lastNotification != "")
            	showNotification(lastNotification);    
        }

        if(appPrefs.AutoMount != lastAutoMountSetting)
        {
            if(appPrefs.AutoMount)
                doMount();
        }

        if(appPrefs.SupressMTP)
        {
            final String psuc = strRootCommand("getprop persist.sys.usb.config | sed -n '1,0p'");
            if(psuc.indexOf(String.valueOf("mtp")) != -1)
            {
                runRootCommand("setprop sys.usb.config mass_storage,adb");
                runRootCommand("setprop persist.sys.usb.config mass_storage,adb");
                runRootCommand("setprop sys.usb.state mass_storage,adb");
            }
            final String propFile = "/data/property/persist.sys.usb.config";
            if(fileExists(propFile))
            {
                runRootCommand("echo \"mass_storage,adb\" > " + propFile);
            }            
        }
    }

    private void doMount()
    {
    	if(!isSetup)
    	{
    		return;
    	}
    	
        if(appPrefs.WarnMount && !appPrefs.AutoMount)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.MountWarning))
                    .setTitle(android.R.string.dialog_alert_title)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                           doRealMount();
                    }
                   })
                   .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                   }
                   })
                   .create().show();
        }
        else
        {
            doRealMount();
        }
    }

    private void doRealMount()
    {
    	if(!isSetup)
    	{
    		return;
    	}
    	
        if(!usbStorageActive)
        {
            if(connectionState.isConnected())
            {
            	boolean allSucceeded = true;       	
            	for(int i = 0; i < appPrefs.LUNFilePaths.size(); ++i)
            	{
	                if(!runRootCommand("echo \"" + appPrefs.SDCardPaths.get(i) + "\" > " + appPrefs.LUNFilePaths.get(i)))
	                {
	                	allSucceeded = false;
	                }
            	}
            	
            	if(allSucceeded)
            	{
            		usbStorageActive = true;
            		stateUpdated();
            	}	            		
            }
        }
    }

    private void doUnmount(boolean forced)
    {
    	if(!isSetup)
    	{
    		return;
    	}
    	
        if(usbStorageActive || forced)
        {
            if((connectionState.isConnected()) || forced)
            {
 				boolean allSucceeded = true;
            	
            	for(String path : appPrefs.LUNFilePaths)
            	{
	                if(!runRootCommand("echo \"\" > " + path))
	                {
	                	allSucceeded = false;
	                }
            	}
 				          	
            	if(allSucceeded)
            	{
            		usbStorageActive = false;
            		stateUpdated();
            	}
            }
        }
    }

    private String detectDeviceStats()
    {
        if(!detectSUBinary())
        {
        	isSetup = false;
            Logger.logW(ClassTag, "Found no SU binary. Setup failed.");
            return getString(R.string.ErrorNoSuperUser);
        }
        else
        {
            Logger.logI(ClassTag, "SU Binary found.");
        }

        appPrefs.DeviceName = detectDeviceName();         
		appPrefs.LUNFilePaths = detectLunPaths();
       	appPrefs.SDCardPaths = detectSDCardPaths();
        appPrefs.SDExtPath = detectSDExtPath();

        if(appPrefs.LUNFilePaths.isEmpty())
        {
        	isSetup = false;
            return getString(R.string.ErrorNoLunFileFound);
        }

        if(appPrefs.SDCardPaths.isEmpty() && appPrefs.SDExtPath == "")
        {
        	isSetup = false;
        	return getString(R.string.ErrorNoPartitionsFound);
        }
        
        appPrefs.writeToSharedPrefs(sharedPreferences);

        Logger.logI(ClassTag, "Device specific setup completed and saved as shared preferences.");
        return "";
    }
 
    private void logDeviceStats()
    {
        Logger.logI(ClassTag, "Device name: " + appPrefs.DeviceName);
             
        for(int i = 0; i < appPrefs.SDCardPaths.size(); ++i)
        	Logger.logI(ClassTag, String.format("VFAT Partition: sdcard %d mounted @ ", i+1) + appPrefs.SDCardPaths.get(i));
      
        if(!appPrefs.SDExtPath.isEmpty())
        {
            Logger.logI(ClassTag, "EXT Partition: sd-ext mounted @ " + appPrefs.SDExtPath);
        }     

        for(int i = 0; i < appPrefs.LUNFilePaths.size(); ++i)
        	Logger.logI(ClassTag, String.format("Lunfile path %d @ ", i+1) + appPrefs.LUNFilePaths.get(i));
    }

    private static boolean fileExists(final String path)
    {
    	final String result = strRootCommand("test -e " + path + " && echo \"file exists\"");
    	return result.equals("file exists");
    }

    private static String makeMultiLineCommand(final String command)
    {
    	return "for i in $(" + command + "); do echo \"$i;\"; done";
    }
    
    private static ArrayList<String> splitCommandResult(final String splitBy, final String command)
    {
    	final String[] lines = command.split(splitBy);
        ArrayList<String> results = new ArrayList<String>();
        
        for(final String line : lines)
        {
        	if(line.length() > 0)
        		results.add(line);
        }
     	return results;    	
    }
    
    private static ArrayList<String> detectLunPaths()
    {	
        final String lunPaths = strRootCommand(makeMultiLineCommand("find /sys/devices/ -name 'file' | grep lun"));
        // TODO: check for file-existence? if so, why?
        return splitCommandResult(";", lunPaths);
    }

    private static ArrayList<String> detectSDCardPaths()
    {  	
        // fileExists(/dev/block/vold/179:1) won't work. But mounting /storage/sdcard0 won't work neither.
        // Thus, we verify the mount path but use the device map refer for mounting, instead.
        final String vfatPaths = strRootCommand(makeMultiLineCommand("cat /proc/mounts | grep fat | grep sdcard | awk '{print $1}'"));
        // TODO: check for file-existence? if so, why?
        //final String sdPaths = strRootCommand(makeMultiLineCommand("cat /proc/mounts | grep fat | grep sdcard | awk '{print $2}'");

        return splitCommandResult(";", vfatPaths);
    }

    private static String detectSDExtPath()
    {
        final String path = strRootCommand("cat /proc/mounts | grep ext | grep sd-ext | awk '{print $1}' | sed -n '1,0p'");
        // TODO: check for file-existence? if so, why?
        return path;
    }

    private static String detectDeviceName()
    {
        final String name = strRootCommand("getprop ro.product.name | sed -n '1,0p'");
        final String man = strRootCommand("getprop ro.product.manufacturer | sed -n '1,0p'");
        final String mdl = strRootCommand("getprop ro.product.model | sed -n '1,0p'");

        return man + " " + mdl + " (" + name + ")";
    }

    private boolean detectSUBinary()
    {
    	boolean result = fileExists("/system/bin/su") | fileExists("/system/xbin/su");
    	return result;
    }

    //! Executes a given command in a separate process as superuser and returns the return value of the commandline if there was one.
    private static String strRootCommand(final String command)
    {
         final StringBuilder output = new StringBuilder();
         Process a;
         BufferedReader read = null;

         try
         {
            a = Runtime.getRuntime().exec("su");   // launch the shell (i.e., either su or sh)
            DataOutputStream b = new DataOutputStream(a.getOutputStream());
            b.writeBytes(command + "\n");          // send the command (\n is probably not needed if your command has it already)
            read = new BufferedReader(new InputStreamReader(a.getInputStream()), 8192);
            b.writeBytes("exit\n");                // exit the shell
            b.flush();                             // flush the buffer
            String line;

            while ((line = read.readLine()) != null)   // read any output the command produced
                output.append(line);

            try
            {
                a.waitFor();
                if (a.exitValue() == 255)                     // error occurred, exit value 255
                    output.append("su/root command failed");
            }
            catch (InterruptedException e)
            {
                output.append("su/root command failed ");     // SU command failed to execute
            }
        }
        catch (IOException e)
        {
            output.append("su/root command failed ");        // not rooted or su permissions not granted
        }

        final String op = output.toString();
        Logger.logD(ClassTag, "su -c " + command + " ==> " + op);

        return op;    // any residual return value from the command
    }

    //! Executes a given command in a separate process as superuser and returns true if there were no errors.
    private static boolean runRootCommand(final String command)
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            Logger.logD(ClassTag, "Executing command: su -c " + command);

            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                process.destroy();
            }
            catch (Exception e)
            {

            }
        }
        return true;
    }

    private void detectMountState()
    {
        boolean allEnabled = true;
     	
    	for(int i = 0; i < appPrefs.LUNFilePaths.size(); ++i)
    	{
            final String state = strRootCommand("cat " + appPrefs.LUNFilePaths.get(i));
            
            if(state.equals(""))
            {
            	allEnabled = false;
                break;                	
            }
            else if (!state.equals(appPrefs.SDCardPaths.get(i)) && !state.equals(appPrefs.SDExtPath))
            {
            	allEnabled = false;
                break;
            }
    	}

        if (allEnabled)
        {
            usbStorageActive = true;
        }
        else
        {
            usbStorageActive = false;
        }
        
        stateUpdated();
    }

    private void stateUpdated()
	{
    	if(!isSetup)
    	{
    		return;
    	}
    	
    	Logger.logD(ClassTag, "State updated");
    	
        if (usbStorageActive)
        {
        	if(connectionState.isConnected())
        	{
	            UIToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_green));
	            UITextUMSState.setText(StrUSBStorageActive);
	            showNotification(StrUSBStorageActive);
	            showPopup(StrUSBStorageActive);
        	}
        }
        else
        {
           	if(connectionState.isConnected())
            {
                UIToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_blue));
                UITextUMSState.setText(StrUSBCablePlugged);
                showNotification(StrUSBCablePlugged);
                showPopup(StrUSBCablePlugged);            
            }   	
            else
            {
                UIToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_blue));
                UITextUMSState.setText(StrUSBCableUnplugged);
                showNotification(StrUSBCableUnplugged);
                showPopup(StrUSBCableUnplugged);            
            }
        }

    	if(appPrefs.VibrateMillis > 0)
    	{
    		Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
    		vibrator.vibrate(appPrefs.VibrateMillis);
    	}
    	
		if(MassStorageWidgetIcon.getInstance() != null)
		{
	    	Logger.logD(ClassTag, "State update passed to widget");
			MassStorageWidgetIcon.getInstance().stateUpdateNotify(usbStorageActive, getStateText(), getApplicationContext());
		}
	}

	private void showPopup(final String text)
    {
        if(appPrefs.ShowPopups && text != null && text != "")
        {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        }
    }

    private void showNotification(final String text)
    {
        if (!appPrefs.ShowNotification || text == null || text.equals(""))
        {
   	        notificationManager.cancel(Constants.AppNotificationID);
        	return;
        }
        
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, 
        		new Intent(getApplicationContext(), MassStorageActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(StrAppname);
        builder.setContentText(text);

		if (usbStorageActive)
			builder.setSmallIcon(R.drawable.usbdroid_green_small);
		else
			builder.setSmallIcon(R.drawable.usbdroid_blue_small);

        builder.setOngoing(true);
        builder.setWhen(0);
        builder.setContentIntent(contentIntent);

        Notification notification = builder.getNotification();
        notificationManager.notify(Constants.AppNotificationID, notification);

        lastNotification = text;
    }

	public static MassStorageActivity getInstance()
	{
		return instance;
	}
	
	public void toggleMount()
	{
    	if(!isSetup)
    	{
    		return;
    	}
		
		boolean lastState = usbStorageActive;
		
		if(!usbStorageActive)
			doMount();
		else
			doUnmount(false);
		
		if(lastState != usbStorageActive)
		{
			Logger.logD(ClassTag, "State toggle request successful");
		}
	}

	public boolean getState()
	{
		return usbStorageActive;
	}
	
	public CharSequence getStateText()
	{
		if(connectionState.isConnected())
			return usbStorageActive ? StrUSBStorageActive : StrUSBCablePlugged;
		
		return StrUSBCableUnplugged;
	}
	
}


