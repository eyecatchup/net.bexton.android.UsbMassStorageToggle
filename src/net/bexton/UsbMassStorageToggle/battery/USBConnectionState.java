package net.bexton.UsbMassStorageToggle.battery;

import android.content.Intent;
import android.os.BatteryManager;

public class USBConnectionState
{
	USBConnectionState(boolean connected, boolean poweringUp)
	{
		this.connected = connected;
		this.poweringUp = poweringUp;
	}
	
	public static USBConnectionState createFromIntent(Intent intent)
	{
        int status = intent.getIntExtra("status", 0);
        int plugged = intent.getIntExtra("plugged", 0);

        boolean poweringUp = false;
        switch (status)
        {
            case BatteryManager.BATTERY_STATUS_CHARGING:
            case BatteryManager.BATTERY_STATUS_FULL:
            	poweringUp = true;
                break;
        }

        boolean connected = false;
        switch (plugged)
        {
            case BatteryManager.BATTERY_PLUGGED_USB:
            	connected = true;
                break;
        }
        
        return new USBConnectionState(connected, poweringUp);
	}
	
	
	public boolean isConnected()
	{
		return connected && poweringUp;
	}
	
	private final boolean connected;
	private final boolean poweringUp;
}
