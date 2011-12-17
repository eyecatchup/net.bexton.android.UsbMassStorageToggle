package net.bexton.android.UsbMassStorageToggle;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class UsbMassStorageToggleActivity extends Activity implements OnClickListener
{

    public boolean enable;
    public boolean disable;

    public boolean powerUp;
    public boolean usbConnected;

    public boolean umsEnabled;
    public boolean tetherEnabled;

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

            // Define content node.
            setContentView(R.layout.main);

            str_enabled = getString(R.string.str_enabled);
            str_disabled = getString(R.string.str_disabled);
            notUsbConnect = getString(R.string.notUsbConnect);
            notUsbDisconnect = getString(R.string.notUsbDisconnect);
            notUmsAutoDisabled = getString(R.string.notUmsAutoDisabled);
            errNoUsb = getString(R.string.errNoUsb);
            errNoRoot = getString(R.string.errNoRoot);

            // Add App Info Text containing a clickable link to xda thread.
            TextView tv = (TextView)findViewById(R.id.infoTxt);
            tv.append(Html.fromHtml("App Info: <a href='http://forum.xda-developers.com/showthread.php?t=1389375'>http://forum.xda-developers.com/showthread.php?t=1389375</a>"));
            tv.setMovementMethod(LinkMovementMethod.getInstance());

            // Define the disable button node and add an OnClickListener.
            UsbAndroid = findViewById(R.id.UsbDroid);
            UsbAndroid.setOnClickListener(this);

            // Define the text view node to hold the current UMS state.
            UMSstate = (TextView)findViewById(R.id.UMSstate);

            // Try to obtain the current UMS state.
            getUmsState();

            // Initialize a new intent filter for ACTION_BATTERY_CHANGED
            // and register a broadcast receiver for it.
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(mBroadcastReceiver, filter);

            notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
                            enable = runRootCommand("echo /dev/block/mmcblk0 > /sys/devices/platform/usb_mass_storage/lun0/file");
                            if(enable == true) {
                                // Change text view content for current UMS state.
                                UMSstate.setText(str_enabled + ".");
                                // Show a notification for extra feedback.
                                showNotification("UMS " + str_enabled + ".",false,0);
                                umsEnabled = true;
                                UsbAndroid.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_green));
                            }
                            else {
                                // Show a toast if there were any errors with executing the shell command.
                                popMsg(errNoRoot);
                            }
                        }
                        else {
                            // Show a toast if the device is not connected via USB.
                            popMsg(errNoUsb);
                        }
                    }
                    else {
                        disable = runRootCommand("echo \"\" > /sys/devices/platform/usb_mass_storage/lun0/file");
                        if(disable == true){
                            // Change text view content for current UMS state.
                            UMSstate.setText(str_disabled + ".");
                            // Show a toast for extra feedback.
                            popMsg("UMS " + str_disabled + ".");
                            umsEnabled = false;
                            UsbAndroid.setBackgroundDrawable(getResources().getDrawable(R.drawable.usbdroid_blue));
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

                case R.id.menu_quit:
                    finishUp();
                    return true;
            }
            return false;
        }

    /**
     * Executes a given command in a separate process as superuser
     * and returns true if there were no errors.
     */
        public static boolean runRootCommand(String command) {
            Process process = null;
            DataOutputStream os = null;
            try {
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
                BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/platform/usb_mass_storage/lun0/file"), 256);
                try {
                    UMS_STATE = reader.readLine();
                } finally {
                    reader.close();
                }

                if (UMS_STATE.equals("/dev/block/mmcblk0")) {
                    UMSstate.setText(str_enabled + ".");
                    umsEnabled = true;
                } else {
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
                            runRootCommand("echo \"\" > /sys/devices/platform/usb_mass_storage/lun0/file");
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

            notification.setLatestEventInfo(this, "USB Mass Storage", text,
                    contentIntent);
            notMan.notify(id, notification);

        }
}