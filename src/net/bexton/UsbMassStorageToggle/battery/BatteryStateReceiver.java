package net.bexton.UsbMassStorageToggle.battery;

import java.util.ArrayList;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BatteryStateReceiver extends BroadcastReceiver
{
	private ArrayList<BatteryStateListener> listeners = new ArrayList<BatteryStateListener>();

	
	public void addListener(BatteryStateListener listener)
	{
		if(listener != null)
			listeners.add(listener);
	}
	
	public void removeListener(BatteryStateListener listener)
	{
		if(listener != null)
			listeners.remove(listener);
	}
	
	
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED))
        {
            final USBConnectionState state = USBConnectionState.createFromIntent(intent);
            
            for(BatteryStateListener listener : listeners)
            	listener.onBatteryStatusChanged(state);
        }
    }
};

