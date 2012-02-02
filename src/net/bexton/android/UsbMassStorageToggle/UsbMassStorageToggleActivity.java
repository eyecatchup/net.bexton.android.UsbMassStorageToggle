package net.bexton.android.UsbMassStorageToggle;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class UsbMassStorageToggleActivity extends Activity implements OnClickListener
{
	private static final String TAG = "USB Mass Storage";
	
	SharedPreferences preferences;

	public boolean setup = false;
	public boolean hasSu = false;
	public boolean hasLun = false;
	public boolean hasFat = false;
	public boolean hasExt = false;

    public String deviceName;
    
    public String prefsLunfilePath;
	public String prefsVfatMountPoint;
	public String prefsExtMountPoint;
	public boolean prefsVfatDefault;
	public boolean prefsDisableMtp;
	
    public boolean enable;
    public boolean disable;

    public boolean powerUp;
    public boolean usbConnected;

    public boolean umsEnabled;

    public String str_enabled;
    public String str_disabled;
    public String notUsbConnect;
    public String notUsbDisconnect;
    public String notUmsAutoDisabled;
    public String errNoUsb;
    public String errNoRoot;

    private static TextView UMSstate;
    private static View UsbAndroid;

    NotificationManager notMan;

    /**
     *  Called when the activity is starting.
     *  Inflate the activity's UI, set OnClickListener ...
     */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main);
            
            preferences = PreferenceManager.getDefaultSharedPreferences(
            		getApplicationContext());
            
            getPrefs();           
            String initRet = "";
            if( prefsLunfilePath.equals(new String("")) || 
            	prefsVfatMountPoint.equals(new String("")) ) 
            {
            	Log.i(TAG, "Gathering device specific setup...");   
            	initRet = detectPreferences();
            }  
            else {
            	Log.i(TAG, "Device specific settings loaded from shared preferences.");
            }
            logPrefs();          
            setupVariables();
            
            notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            UMSstate = (TextView)findViewById(R.id.UMSstate);
            getUmsState();
            
            UsbAndroid = findViewById(R.id.UsbDroid);
            UsbAndroid.setOnClickListener(this);            

            IntentFilter filter = new IntentFilter();	
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(mBroadcastReceiver, filter);
            
            if(setup == true) {
            	Log.i(TAG, "Device specific setup successfully completed.");
            } 
            else {           	
            	if(initRet != "") {
            		Log.w(TAG, "Device specific setup failed. " +
            				"Usage disabled and prompt for setup.");
                	popMsg(initRet);
            	}            	
            }
        }

    /**
     *  Invoke when a view (Droid) is clicked.
     */
    	public void onClick(View v) {
            switch(v.getId()){

                // Invoke when the green Droid (enable UMS) is clicked.
                case R.id.UsbDroid:
                    if(umsEnabled != true) {
                        if(usbConnected == true && powerUp == true) {
                        	enable = runRootCommand(
                        			"echo \"" + prefsVfatMountPoint + "\" > " + prefsLunfilePath);
                            if(enable == true) {
                                // Change text view content for current UMS state.
                                UMSstate.setText(str_enabled + ".");
                                // Show a notification for extra feedback.
                                showNotification("UMS " + str_enabled + ".",false,0);
                                umsEnabled = true;
                                UsbAndroid.setBackgroundDrawable(
                                		getResources().getDrawable(
                                				R.drawable.usbdroid_green));
                            }
                            else {
                                // Show a toast if there were any errors 
                            	// with executing the shell command.
                                popMsg(errNoRoot);
                            }
                        }
                        else {
                            // Show a toast if the device is not connected via USB.
                            popMsg(errNoUsb);
                        }
                    }
                    else {
                        disable = runRootCommand("echo \"\" > " + prefsLunfilePath);
                        if(disable == true){
                            // Change text view content for current UMS state.
                            UMSstate.setText(str_disabled + ".");
                            // Show a toast for extra feedback.
                            popMsg("UMS " + str_disabled + ".");
                            umsEnabled = false;
                            UsbAndroid.setBackgroundDrawable(
                            		getResources().getDrawable(R.drawable.usbdroid_blue));
                            if(usbConnected == true && powerUp == true){
                                showNotification(notUsbConnect,false,0);
                            }
                        }
                        else {
                            // Show a toast if there were any errors with executing the shell command.
                            popMsg(errNoRoot);
                        }
                    }
                    break;
            }
        }

    /**
     *  Add a menu.
     */
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
            return true;
        }

    /**
     *  Handle menu choice.
     *  (Currently just "Close Application")
     */
        public boolean onOptionsItemSelected(MenuItem item){
            super.onOptionsItemSelected(item);
            switch(item.getItemId()){
            
            	case R.id.menu_settings:
            		Intent menuIntent = new Intent(getBaseContext(), UmsPreferencesActivity.class);
            		startActivity(menuIntent);
            		return true;

            	case R.id.menu_info:
            		Uri uri = Uri.parse("http://forum.xda-developers.com/showthread.php?t=1389375"); 
            		Intent xdaintent = new Intent(Intent.ACTION_VIEW, uri); 
            		startActivity(xdaintent);
            		return true;
            		
                case R.id.menu_quit:
                    finishUp();
                    return true;
            }
            return false;
        } 

    /**
	 *  Get/Set Preferences.
	 */        
        public void getPrefs() {           
	        prefsVfatDefault = preferences.getBoolean("defaultMount", true);
	        prefsDisableMtp = preferences.getBoolean("mtpSupport", false);
	        prefsVfatMountPoint = preferences.getString("point", "");
	        prefsExtMountPoint = preferences.getString("pointExt", "");
	        prefsLunfilePath = preferences.getString("path", "");
	        deviceName = preferences.getString("deviceName", "");
	        
	        if(prefsDisableMtp == true) {
	        	String psuc = strRootCommand("getprop persist.sys.usb.config | sed -n '1,0p'");
	        	if(psuc.indexOf(new String("mtp")) != -1) {
	        		runRootCommand("setprop persist.sys.usb.config adb");
	        	}
	        }
        }
        
        public void logPrefs() {       	
        	Log.i(TAG, "Device codename: " + deviceName);
        	Log.i(TAG, "VFAT Partition: sdcard mounted @ "+prefsVfatMountPoint);
        	if(hasExt == true) {
        		Log.i(TAG, "EXT Partition: sd-ext mounted @ "+prefsExtMountPoint);
        	}
        	Log.i(TAG, "Lunfile path: "+prefsLunfilePath);        	
        }
    
    /**
	 *  Set basic some variables.
	 */
        public void setupVariables() {           
	        str_enabled = getString(R.string.str_enabled);
	        str_disabled = getString(R.string.str_disabled);
	        notUsbConnect = getString(R.string.notUsbConnect);
	        notUsbDisconnect = getString(R.string.notUsbDisconnect);
	        notUmsAutoDisabled = getString(R.string.notUmsAutoDisabled);
	        errNoUsb = getString(R.string.errNoUsb);
	        errNoRoot = getString(R.string.errNoRoot);
        }        
 
    /** 
     *  Initialize Settings / Gather device specific information.
     */
        public String detectPreferences() {

        	String abortMsg = "";
        	
            if(hasSuBinary() != true) {
            	setup = false;
            	Log.w(TAG, "Found no SU binary. Setup failed.");
            	abortMsg = getString(R.string.errNoSu); 
            } else {
            	Log.i(TAG, "SU Binary found.");
            }
        	
            deviceName = getDeviceName();
            
            prefsVfatMountPoint = getSdcard();        	
            prefsExtMountPoint = getSdExt();
        	
        	if(hasFat == false && hasExt == false) {
        		setup = false;
        		abortMsg = "No partitions found at all. Please use the menu to configure manually.";
        	}       	
        	
    		prefsLunfilePath = getLunPath();        	
        	if(prefsLunfilePath.equals(new String("false"))) {
            	setup = false;
            	abortMsg = getString(R.string.errNoLun);         		
        	}
        	
        	Editor edit = preferences.edit();
        	edit.putString("point", prefsVfatMountPoint);
        	edit.putString("pointExt", prefsExtMountPoint);
        	edit.putString("path", prefsLunfilePath);
        	edit.putString("deviceName", deviceName);
        	edit.commit();

        	Log.i(TAG, "Device specific setup completed and saved as shared preferences.");
        	return abortMsg;
        }         

        
        public boolean fileExists(String path) {
            boolean fileExists = true;
            try {
                File file = new File(path);
                if (file.exists() == false) {
                	fileExists = false;
                }
            } catch (Exception e) {
            	fileExists = false;
            }
            return fileExists;
        }   
        
        public String getLunPath() {
        	String path = strRootCommand(
        			"find /sys/devices/platform `pwd` -name \"file\" | grep \"usb\" | grep \"lun0\" | sed -n '1,0p'");
        	boolean pathVerified = fileExists(path);
        	if(pathVerified == true) {
        		hasLun = true;
        		return path;
        	} else {
        		return new String("false");
        	}
        }
        
        public String getSdcard() {
        	String path = strRootCommand(
        			"cat /proc/mounts | grep fat | grep sdcard | awk '{print $1}' | sed -n '1,0p'");
        	boolean pathVerified = fileExists(path);
        	if(pathVerified == true) {
        		hasFat = true;
        		return path;
        	} else {
        		return new String("false");
        	}        	
        }
        
        public String getSdExt() {
        	String path = strRootCommand(
        			"cat /proc/mounts | grep ext | grep sd-ext | awk '{print $1}' | sed -n '1,0p'");
        	boolean pathVerified = fileExists(path);
        	if(pathVerified == true) {
        		hasExt = true;
        		return path;
        	} else {
        		return new String("false");
        	}        	
        }
        
        public String getDeviceName() {
        	String dname = strRootCommand("getprop ro.product.name | sed -n '1,0p'");
        	return dname;
        }        
        
        public boolean hasSuBinary() {
            boolean rooted = true;
            try {
                File su = new File("/system/bin/su");
                if (su.exists() == false) {
                    su = new File("/system/xbin/su");
                    if (su.exists() == false) {
                        rooted = false;
                    } else {
                    	hasSu = true;
                    }
                } else {
                	hasSu = true;
                }
            } catch (Exception e) {
                rooted = false;
            }
            return rooted;
        }         
 
        public static String strRootCommand(String command)
        {  
         final StringBuilder output = new StringBuilder();
         Process a;
         BufferedReader read = null;
         try {
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
        String op = output.toString();
        Log.d(TAG, "su -c "+command+" ==> "+ op);
        return op;    // any residual return value from the command
        }        
        
        
    /**
     * Executes a given command in a separate process as superuser
     * and returns true if there were no errors.
     */
        public static boolean runRootCommand(String command) {
            Process process = null;
            DataOutputStream os = null;
            try {
            	Log.d(TAG, "Root-Command ==> su -c "+command);
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(command+"\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
            } catch (Exception e) {
                return false;
            }
            finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    process.destroy();
                } catch (Exception e) {}
            }
            return true;
        }
  
        
    /**
     *  Try to obtain the current UMS state
     *  by reading the content of the lunfile.
     */
        public void getUmsState() {
            String UMS_STATE = "0";
            try {
                BufferedReader reader = new BufferedReader(
                		new FileReader(prefsLunfilePath), 256);
                try {
                    UMS_STATE = reader.readLine();
                } finally {
                    reader.close();
                }

                if (UMS_STATE.equals(prefsVfatMountPoint) || 
                	UMS_STATE.equals(prefsExtMountPoint)) 
                {
	                    UMSstate.setText(str_enabled + ".");
	                    umsEnabled = true;
                } 
                else {
                    UMSstate.setText(str_disabled + ".");
                    umsEnabled = false;
                }
            } catch (Exception e) { }
        }

    /**
     * Displays a simple toast
     */
        public void popMsg(String message) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

    /**
     *  Quit the application
     */
        public void finishUp(){
            finish();
        }

    /**
     *  Broadcast Receiver to catch changes from the battery manager.
     */
        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {

                    int status = intent.getIntExtra("status", 0);
                    int plugged = intent.getIntExtra("plugged", 0);

                    powerUp = false;

                    switch (status) {
                        case BatteryManager.BATTERY_STATUS_CHARGING:
                            powerUp = true;
                            break;
                        case BatteryManager.BATTERY_STATUS_FULL:
                            powerUp = true;
                            break;
                    }

                    usbConnected = false;

                    switch (plugged) {
                        case BatteryManager.BATTERY_PLUGGED_USB:
                            usbConnected = true;
                            break;
                        case BatteryManager.BATTERY_PLUGGED_AC:
                            usbConnected = false;
                            break;
                    }

                    if(usbConnected == true && powerUp == true && umsEnabled == false){
                        showNotification(notUsbConnect,false,0);
                    }
                    else if(usbConnected == true && powerUp == true && umsEnabled == true){
                        showNotification("UMS " + str_enabled + ".",false,0);
                    }
                    else if(usbConnected == false && powerUp == false) {
                        if(umsEnabled == true) {
                            runRootCommand("echo \"\" > " + prefsLunfilePath);
                            umsEnabled = false;
                            UMSstate.setText(str_disabled + ".");
                            UsbAndroid.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_blue));
                            showNotification("UMS " + notUmsAutoDisabled + ".",false,0);
                        }
                        else {
                            showNotification(notUsbDisconnect,false,0);
                        }
                    } else { }
                }
            }
        };

        private void showNotification(String text, boolean ongoing, int id) {

            Notification notification = new Notification(
                    R.drawable.icon_blue_s, text,
                    System.currentTimeMillis());
            if (ongoing) {
                notification.flags = Notification.FLAG_ONGOING_EVENT;

            } else {
                notification.flags = Notification.FLAG_AUTO_CANCEL;
            }
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledOnMS=2000;
            notification.ledOffMS=1000;
            PendingIntent contentIntent = PendingIntent
                    .getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(),
                            UsbMassStorageToggleActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setLatestEventInfo(this, TAG, text,
                    contentIntent);
            notMan.notify(id, notification);

        }       
}