package net.bexton.UsbMassStorageToggle;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.bexton.UsbMassStorageToggle.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    private SharedPreferences preferences;
    private NotificationManager notificationManager;

    private boolean setup = false;
    @SuppressWarnings("unused")
    private boolean hasSu = false;
    @SuppressWarnings("unused")
    private boolean hasLun = false;
    private boolean hasFat = false;
    private boolean hasExt = false;
    private String deviceName;

    private String prefsLunfilePath;
    private String prefsVfatMountPoint;
    private String prefsExtMountPoint;
    @SuppressWarnings("unused")
    private boolean prefsVfatDefault;
    private boolean prefsDisableMtp;
    private boolean prefsStickyNotifications;
    private boolean prefsAutoMount;
    private boolean prefsWarnMount;
    private boolean prefsLoggingEnabled;


    private boolean powerUp;
    private boolean usbConnected;
    private boolean umsEnabled;

    private String StrAppname;
    private String StrStateEnabled;
    private String StrStateDisabled;
    private String StrNotificationUSBConnected;
    private String StrNotificationUSBDisconnected;
    private String StrNotificationUMSAutoDisabled;
    private String StrErrorNoUSB;
    private String StrErrorNoRoot;

    private TextView UI_TextUMSState;
    private View UI_ToggleStateButton;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // register change-listener to update prefs
        preferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener()
        {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                readPreferences();
            }
        });

        readPreferences();

        String initRet = "";
        if( prefsLunfilePath.equals(new String("")) || prefsVfatMountPoint.equals(new String("")) )
        {
            Logger.logI(TAG, "Gathering device specific setup...");
            initRet = detectDeviceStats();
        }
        else
        {
            Logger.logI(TAG, "Device specific settings loaded from shared preferences.");
        }
        logDeviceStats();
        initStringsFromResources();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        UI_TextUMSState = (TextView)findViewById(R.id.UI_TextUMSState);
        UI_ToggleStateButton = findViewById(R.id.UI_ToggleStateButton);
        UI_ToggleStateButton.setOnClickListener(this);

        readUmsState();

        registerReceiver(mBatteryStateBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if(setup)
        {
            Logger.logI(TAG, "Device specific setup successfully completed.");
        }
        else
        {
            if(initRet != "")
            {
                Logger.logW(TAG, "Device specific setup failed. Usage disabled and prompt for setup.");
                popMsg(initRet);
            }
        }

        // the intent the activity got started from.
        Intent intent = getIntent();
        if(intent.hasExtra(BootUpReceiver.Identifier))
        {
            hideActivity();
        }
    }

    @Override
    public void onBackPressed()
    {
        hideActivity();
    }

    private void initStringsFromResources()
    {
        StrAppname = getString(R.string.AppName);
        StrStateEnabled = getString(R.string.UMSEnabled);
        StrStateDisabled = getString(R.string.UMSDisabled);
        StrNotificationUSBConnected = getString(R.string.NotificationUSBConnected);
        StrNotificationUSBDisconnected = getString(R.string.NotificationUSBDisconnected);
        StrNotificationUMSAutoDisabled = getString(R.string.NotificationUMSAutoDisabled);
        StrErrorNoUSB = getString(R.string.ErrorNoUSB);
        StrErrorNoRoot = getString(R.string.ErrorNoRoot);
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
        if(mBatteryStateBroadcastReceiver != null)
        {
            unregisterReceiver(mBatteryStateBroadcastReceiver);
               mBatteryStateBroadcastReceiver = null;
        }

        finish();
    }

    public void onClick(View v)
    {
        if(v.getId() == R.id.UI_ToggleStateButton)
        {
            if(umsEnabled)
                tryUnmount(false);
            else
                tryMount();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);

        switch(item.getItemId())
        {
            case R.id.menu_settings:
            {
                Intent menuIntent = new Intent(getBaseContext(), UmsPreferencesActivity.class);
                startActivity(menuIntent);
                return true;
            }
            case R.id.menu_info:
            {
                Uri uri = Uri.parse("http://forum.xda-developers.com/showthread.php?t=1389375");

                Intent xdaintent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(xdaintent);
                return true;
            }
            case R.id.menu_quit:
            {
                notificationManager.cancelAll();

                finishUp();
                return true;
            }
        }
        return false;
    }

    private void readPreferences()
    {
        boolean lastStickyValue = prefsStickyNotifications;
        boolean lastAutoMount = prefsAutoMount;

        prefsVfatDefault = preferences.getBoolean("defaultMount", true);
        prefsDisableMtp = preferences.getBoolean("mtpSupport", false);
        prefsVfatMountPoint = preferences.getString("point", "");
        prefsExtMountPoint = preferences.getString("pointExt", "");
        prefsLunfilePath = preferences.getString("path", "");
        deviceName = preferences.getString("deviceName", "");
        prefsStickyNotifications = preferences.getBoolean("stickyNotifications", true);
        prefsAutoMount = preferences.getBoolean("usbAutoMount", false);
        prefsWarnMount = preferences.getBoolean("warnBeforeMount", false);
        prefsLoggingEnabled = preferences.getBoolean("loggingEnabled", true);

        Logger.setEnabled(prefsLoggingEnabled);

        if(prefsStickyNotifications != lastStickyValue)
        {
            repostLastNotification();
        }

        if(prefsAutoMount != lastAutoMount)
        {
            if(prefsAutoMount)
                tryMount();
        }

        if(prefsDisableMtp)
        {
            final String psuc = strRootCommand("getprop persist.sys.usb.config | sed -n '1,0p'");
            if(psuc.indexOf(new String("mtp")) != -1)
            {
                runRootCommand("setprop persist.sys.usb.config adb");
            }
        }
    }

    private void tryMount()
    {
        if(prefsWarnMount && !prefsAutoMount)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.MountWarning))
                    .setTitle(android.R.string.dialog_alert_title)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                           tryRealMount();
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
            tryRealMount();
        }
    }

    private void tryRealMount()
    {
        if(!umsEnabled)
        {
            if(usbConnected && powerUp)
            {
                boolean success = runRootCommand("echo \"" + prefsVfatMountPoint + "\" > " + prefsLunfilePath);
                if(success)
                {
                    umsEnabled = true;

                    UI_TextUMSState.setText(StrStateEnabled);
                    UI_ToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_green));

                    showNotification(StrStateEnabled);
                }
                else
                {
                    // Show a toast if there were any errors
                    // with executing the shell command.
                    popMsg(StrErrorNoRoot);
                }
            }
            else
            {
                // Show a toast if the device is not connected via USB.
                popMsg(StrErrorNoUSB);
            }
        }
    }

    private void tryUnmount(boolean forced)
    {
        if(umsEnabled || forced)
        {
            if((usbConnected && powerUp) || forced)
            {
                boolean success = runRootCommand("echo \"\" > " + prefsLunfilePath);
                if(success)
                {
                    umsEnabled = false;

                    UI_TextUMSState.setText(StrStateDisabled);
                    UI_ToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_blue));

                    showNotification(StrNotificationUSBConnected);
                    popMsg(StrStateDisabled);
                }
                else
                {
                    // Show a toast if there were any errors with executing the shell command.
                    popMsg(StrErrorNoRoot);
                }
            }
            else
            {
                // Show a toast if the device is not connected via USB.
                popMsg(StrErrorNoUSB);
            }
        }
    }

    private String detectDeviceStats()
    {
        String abortMsg = "";

        if(!hasSuBinary())
        {
            setup = false;
            Logger.logW(TAG, "Found no SU binary. Setup failed.");
            abortMsg = getString(R.string.ErrorNoSuperUser);
        }
        else
        {
            Logger.logI(TAG, "SU Binary found.");
        }

        deviceName = getDeviceName();

        prefsVfatMountPoint = getSdcard();
        prefsExtMountPoint = getSdExt();

        if(!hasFat && !hasExt)
        {
            setup = false;
            abortMsg = getString(R.string.ErrorNoPartitionsFound);
        }

        prefsLunfilePath = getLunPath();
        if(prefsLunfilePath.equals(new String("false")))
        {
            setup = false;
            abortMsg = getString(R.string.ErrorNoLunFileFound);
        }

        Editor edit = preferences.edit();
        edit.putString("point", prefsVfatMountPoint);
        edit.putString("pointExt", prefsExtMountPoint);
        edit.putString("path", prefsLunfilePath);
        edit.putString("deviceName", deviceName);
        edit.commit();

        Logger.logI(TAG, "Device specific setup completed and saved as shared preferences.");
        return abortMsg;
    }

    private void logDeviceStats()
    {
        Logger.logI(TAG, "Device name: " + deviceName);
        Logger.logI(TAG, "VFAT Partition: sdcard mounted @ "+prefsVfatMountPoint);
        if(hasExt)
        {
            Logger.logI(TAG, "EXT Partition: sd-ext mounted @ "+prefsExtMountPoint);
        }
        Logger.logI(TAG, "Lunfile path: "+prefsLunfilePath);
    }

    private static boolean fileExists(final String path)
    {
        try
        {
            File file = new File(path);
            return file.exists();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private String getLunPath()
    {
        final String path = strRootCommand("find /sys/devices/platform `pwd` -name \"file\" | grep \"usb\" | grep \"lun0\" | sed -n '1,0p'");

         if(fileExists(path))
        {
            hasLun = true;
            return path;
        }
        else
        {
            return new String("false");
        }
    }

    private String getSdcard()
    {
        final String point = strRootCommand("cat /proc/mounts | grep fat | grep sdcard | awk '{print $1}' | sed -n '1,0p'");
        final String path = strRootCommand("cat /proc/mounts | grep fat | grep sdcard | awk '{print $2}' | sed -n '1,0p'");

        // fileExists(/dev/block/vold/179:1) won't work. But mounting /storage/sdcard0 won't work neither.
        // Thus, we verify the mount path but use the device map refer for mounting, instead.
        if(fileExists(path))
        {
            hasFat = true;
            return point;
        }
        else
        {
            return new String("false");
        }
    }

    private String getSdExt()
    {
        final String path = strRootCommand("cat /proc/mounts | grep ext | grep sd-ext | awk '{print $1}' | sed -n '1,0p'");

        if(fileExists(path))
        {
            hasExt = true;
            return path;
        }
        else
        {
            return new String("false");
        }
    }

    private static String getDeviceName()
    {
        final String name = strRootCommand("getprop ro.product.name | sed -n '1,0p'");
        final String man = strRootCommand("getprop ro.product.manufacturer | sed -n '1,0p'");
        final String mdl = strRootCommand("getprop ro.product.model | sed -n '1,0p'");

        return man + " " + mdl + " (" + name + ")";
    }

    private boolean hasSuBinary()
    {
         hasSu = false;

         try
        {
            File su = new File("/system/bin/su");
            if (su.exists())
            {
                hasSu = true;
                return true;
            }

            su = new File("/system/xbin/su");
            if (su.exists())
            {
                hasSu = true;
                return true;
            }
        }
        catch (Exception e)
        {
              hasSu = false;
            return false;
        }

        return false;
    }

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
        Logger.logD(TAG, "su -c " + command + " ==> " + op);

        return op;    // any residual return value from the command
    }

    //! Executes a given command in a separate process as superuser and returns true if there were no errors.
    private static boolean runRootCommand(final String command)
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            Logger.logD(TAG, "Root-Command ==> su -c " + command);

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

    private void readUmsState()
    {
        String UMS_STATE = "0";
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(prefsLunfilePath), 256);
            try
            {
                UMS_STATE = reader.readLine();
            }
            finally
            {
                reader.close();
            }

            if (UMS_STATE.equals(prefsVfatMountPoint) || UMS_STATE.equals(prefsExtMountPoint))
            {
                UI_ToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_green));
                UI_TextUMSState.setText(StrStateEnabled);
                umsEnabled = true;
            }
            else
            {
                UI_ToggleStateButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_blue));
                UI_TextUMSState.setText(StrStateDisabled);
                umsEnabled = false;
            }
        }
        catch (Exception e)
        {

        }
    }

    private void popMsg(String message)
    {
        if(message != null && message != "")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private BroadcastReceiver mBatteryStateBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED))
            {
                int status = intent.getIntExtra("status", 0);
                int plugged = intent.getIntExtra("plugged", 0);

                powerUp = false;
                switch (status)
                {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                    case BatteryManager.BATTERY_STATUS_FULL:
                        powerUp = true;
                        break;
                }

                boolean previouslyConnected = usbConnected;
                usbConnected = false;
                switch (plugged)
                {
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        usbConnected = true;
                        break;
                }

                boolean connected = usbConnected && powerUp;

                // autoMount (if cable got plugged in)
                if(connected)
                {
                    if(prefsAutoMount)
                    {
                        if(!previouslyConnected)
                            tryMount();
                    }

                    // ready, but not enabled
                    if(!umsEnabled)
                        showNotification(StrNotificationUSBConnected);
                    else // enabled
                        showNotification(StrStateEnabled);
                }
                else
                {
                    if(umsEnabled)
                    {
                        // force anmount, must disconnect
                        tryUnmount(true);
                        showNotification(StrNotificationUMSAutoDisabled);
                    }
                    else
                    {
                        showNotification(StrNotificationUSBDisconnected);
                    }
                }
            }
        }
    };

    private String lastNotifText = null;

    private void repostLastNotification()
    {
        if(lastNotifText != null && lastNotifText != "")
            showNotification(lastNotifText);
    }

    private void showNotification(final String text)
    {
        final int NOTIFICATION_ID = 0;

        if(prefsStickyNotifications)
        {
            notificationManager.cancel(NOTIFICATION_ID);
        }

        PendingIntent contentIntent = PendingIntent
                .getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(),
                        UsbMassStorageToggleActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(StrAppname);
           builder.setContentText(text);

           if(umsEnabled)
               builder.setSmallIcon(R.drawable.usbdroid_green_small);
           else
               builder.setSmallIcon(R.drawable.usbdroid_blue_small);

        if (prefsStickyNotifications)
            builder.setOngoing(true);
        else
            builder.setAutoCancel(true);

        builder.setContentIntent(contentIntent);

        Notification notification = builder.getNotification();

        notificationManager.notify(NOTIFICATION_ID, notification);

        lastNotifText = text;
    }
}