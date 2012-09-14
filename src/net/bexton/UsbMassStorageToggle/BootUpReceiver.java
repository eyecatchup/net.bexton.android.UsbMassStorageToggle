package net.bexton.UsbMassStorageToggle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver
{
	public static String Identifier = "fromBootUp";
	
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent i = new Intent(context, UsbMassStorageToggleActivity.class);  
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Identifier, true);
        context.startActivity(i);
    }
}
