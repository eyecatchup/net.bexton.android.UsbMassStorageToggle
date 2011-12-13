package net.bexton.android.UsbMassStorageToggle;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class UsbMassStorageToggleActivity extends Activity implements OnClickListener {

	public Boolean enable;
	public Boolean disable;
	
	
	private static TextView UMSstate;	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Define content node.
        setContentView(R.layout.main);
        
        TextView tv = (TextView)findViewById(R.id.infoTxt);
        tv.append(Html.fromHtml("App Info: <a href='http://forum.xda-developers.com/showthread.php?t=1389375'>http://forum.xda-developers.com/showthread.php?t=1389375</a>"));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        
        UMSstate = (TextView)findViewById(R.id.UMSstate);
        getUmsState();
        
        // Add Click listener for UMS enable button
        View enableUMS = findViewById(R.id.UMSon);
        enableUMS.setOnClickListener(this);
        
        // Add Click listener for UMS disable
        View disableUMS = findViewById(R.id.UMSoff);
        disableUMS.setOnClickListener(this);
    }
    
	public void onClick(View v) {
        switch(v.getId()){
        case R.id.UMSon:  
        	Log.i("Button","Enable UMS pressed.");
        	
        	enable = runRootCommand("echo /dev/block/vold/179:1 > /sys/devices/platform/usb_mass_storage/lun0/file");
        	if(enable == true){
        		UMSstate.setText("enabled.");
                popMsg("UMS successfully enabled.");
        	}
        	else if(enable == false){
        		popMsg("Failure! Did you granted root permissions?");
        	}
            break;
        
        case R.id.UMSoff:
            Log.i("Button","Disable UMS.");
            disable = runRootCommand("echo 0 > /sys/devices/platform/usb_mass_storage/lun0/file");
            if(disable == true){
            	UMSstate.setText("disabled.");
            	popMsg("UMS successfully disabled.");
            }
            else if(disable == false){
        		popMsg("Failure! Did you granted root permissions?");
        	}
            break;   
        }
	}
	
    // Add the menu buttons
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    } 	

    // Handle events from the popup menu above   
    public boolean onOptionsItemSelected(MenuItem item){
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){     
        
	        case R.id.menu_quit:
	        	finishUp();
	            return true;            
            
        }
        return false;
    } 	
	
    // Quit the application
    public void finishUp(){
        finish();
    }	
	
    //Displays a simple toast 
    /**
     * @param message
     */
    public void popMsg(String message) {
    	Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
    
    //Executes a given command in a separate process as superuser
    //and returns true if there were no errors.
    /**
     * @param command
     * @return
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
    
    public String getUmsState() 
    {
        String UMS_STATE;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/platform/usb_mass_storage/lun0/file"), 256);
            try {
            	UMS_STATE = reader.readLine();
            } finally {
                reader.close();
            }

            if (UMS_STATE.equals("/dev/block/vold/179:1")) {
            	Log.i("getUmsState","UMS enabled.");
            	UMSstate.setText("enabled.");
            	return "UMS enabled.";              
            } else {
            	Log.i("getUmsState","UMS disabled.");
            	UMSstate.setText("disabled.");
            	return "UMS disabled.";
            }
        } catch (Exception e) {  
        	Log.i("getUmsState","Can not obtain UMS state.");
            return "Can not obtain UMS state.";
        }
    }   
    
}