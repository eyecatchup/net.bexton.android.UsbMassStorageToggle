package net.bexton.android.UsbMassStorageToggle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BootUpReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {

            int status = intent.getIntExtra("status", 0);
            int plugged = intent.getIntExtra("plugged", 0);

            Boolean powerUp = false;

            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    powerUp = true;
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    powerUp = true;
                    break;
            }

            Boolean usbConnected = false;

            switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_USB:
                    usbConnected = true;
                    break;
                case BatteryManager.BATTERY_PLUGGED_AC:
                    usbConnected = false;
                    break;
            }

            if(usbConnected == true && powerUp == true){
                Intent i = new Intent(context, UsbMassStorageToggleActivity.class);  
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i); 
            }
            else if(usbConnected == false && powerUp == false) {
                Intent i = new Intent(context, UsbMassStorageToggleActivity.class);  
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i); 
            } else { }
        } 
    }

}
