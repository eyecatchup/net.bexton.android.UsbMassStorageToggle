package net.bexton.UsbMassStorageToggle;

import net.bexton.UsbMassStorageToggle.R;
import net.bexton.UsbMassStorageToggle.core.Logger;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MassStorageWidgetIcon extends AppWidgetProvider
{
	private final String ClassTag = "USB Mass Storage Widget";
	private final String ActionClick = "ActionClick";
	
    private static MassStorageWidgetIcon instance = null;
  
    //! Constructor.
	public MassStorageWidgetIcon()
	{
		instance = this;	
	}
	
    //! returns the instance of this widget.
	public static MassStorageWidgetIcon getInstance()
	{
		return instance;
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{	
		ComponentName thisWidget = new ComponentName(context, MassStorageWidgetIcon.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		for (int widgetId : allWidgetIds)
		{
			// Register an onClickListener
			Intent intent = new Intent(context, MassStorageWidgetIcon.class);
			intent.setAction(ActionClick);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
			
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_icon_layout);
			remoteViews.setOnClickPendingIntent(R.id.WidgetLayout, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
		
		if(MassStorageActivity.getInstance() != null)
		{
			Logger.logD(ClassTag, "Widget setting initialState");
			
			boolean state = MassStorageActivity.getInstance().getState();
			CharSequence stateText = MassStorageActivity.getInstance().getStateText();			
			updateWidgetUI(state, stateText, context);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);
		if(intent.getAction().equals("android.appwidget.action.APPWIDGET_ENABLED"))
		{
			Logger.logD(ClassTag, "Widget created");
		}
		else if (intent.getAction().equals(ActionClick))
		{
			Logger.logD(ClassTag, "Widget clicked");
			
			if(MassStorageActivity.getInstance() == null)
			{
				Logger.logD(ClassTag, "Widget re-created app");
				
				Intent activityIntent = new Intent(context, MassStorageActivity.class);
				activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				activityIntent.putExtra(MassStorageActivity.ActivityFlags.FlagHide, true);
				activityIntent.putExtra(MassStorageActivity.ActivityFlags.FlagToggleState, true);
								
				context.startActivity(activityIntent);
			}
			else
			{
				Logger.logD(ClassTag, "Widget toggled app state");
				
				MassStorageActivity.getInstance().toggleMount();
				
				boolean state = MassStorageActivity.getInstance().getState();
				CharSequence stateText = MassStorageActivity.getInstance().getStateText();				
				updateWidgetUI(state, stateText, context);
			}
		}
	}

	//! updates the widget UI
	private void updateWidgetUI(boolean state, CharSequence stateText, Context context)
	{
		Logger.logD(ClassTag, "Widget updates state");
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		ComponentName thisWidget = new ComponentName(context, MassStorageWidgetIcon.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		for (int widgetId : allWidgetIds)
		{
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_icon_layout);
			
			int iconId = state ? R.drawable.usbdroid_green : R.drawable.usbdroid_blue;
			
			// TODO: can't set text here, we removed text from the widget unless someone is going
			// to write such a widget with perfect centering and whatnot.
			//remoteViews.setTextViewText(R.id.WidgetState, stateText);
			remoteViews.setImageViewResource(R.id.WidgetIcon, iconId);
			
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}	
	}

	//! public notify for the main-activity.
	public void stateUpdateNotify(boolean state, CharSequence stateText, Context context)
	{
		Logger.logD(ClassTag, "Widget receceived state-update notify");		
		updateWidgetUI(state, stateText, context);
	}	
}
