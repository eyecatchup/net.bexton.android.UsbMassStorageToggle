package net.bexton.UsbMassStorageToggle;

import android.util.Log;


public class Logger
{
	private static boolean enabled = true;
		
	public static void setEnabled(boolean enable)
	{
		enabled = enable;
	}
	
	
	public static void logD(final String tag, final String msg)
	{
		if(enabled)
			Log.d(tag, msg);
	}
	
	public static void logV(final String tag, final String msg)
	{
		if(enabled)
			Log.v(tag, msg);
	}

	public static void logI(final String tag, final String msg)
	{
		if(enabled)
			Log.i(tag, msg);
	}
	
	public static void logW(final String tag, final String msg)
	{
		if(enabled)
			Log.w(tag, msg);
	}
	
	public static void logE(final String tag, final String msg)
	{
		if(enabled)
			Log.e(tag, msg);
	}


}
