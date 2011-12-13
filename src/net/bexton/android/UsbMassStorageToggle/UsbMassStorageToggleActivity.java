package net.bexton.android.UsbMassStorageToggle;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;

import android.app.Activity;
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

    public Boolean enable;
    public Boolean disable;

    private static TextView UMSstate;

    /**
     *  Called when the activity is starting.
     *  Inflate the activity's UI, set OnClickListener ...
     */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Define content node.
            setContentView(R.layout.main);

            // Add App Info Text containing a clickable link to xda thread.
            TextView tv = (TextView)findViewById(R.id.infoTxt);
            tv.append(Html.fromHtml("App Info: <a href='http://forum.xda-developers.com/showthread.php?t=1389375'>http://forum.xda-developers.com/showthread.php?t=1389375</a>"));
            tv.setMovementMethod(LinkMovementMethod.getInstance());

            // Define the enable button node and add an OnClickListener.
            View enableUMS = findViewById(R.id.UMSon);
            enableUMS.setOnClickListener(this);

            // Define the disable button node and add an OnClickListener.
            View disableUMS = findViewById(R.id.UMSoff);
            disableUMS.setOnClickListener(this);

            // Define the text view node to hold the current UMS state.
            UMSstate = (TextView)findViewById(R.id.UMSstate);

            // Try to obtain the current UMS state.
            getUmsState();
        }

    /**
     *  Invoke when a view (Droid) is clicked.
     */
        public void onClick(View v) {
            switch(v.getId()){

                // Invoke when the green Droid (enable UMS) is clicked.
                case R.id.UMSon:
                    enable = runRootCommand("echo /dev/block/vold/179:1 > /sys/devices/platform/usb_mass_storage/lun0/file");
                    if(enable == true){
                        // Change text view content for current UMS state.
                        UMSstate.setText("enabled.");
                        // Show a toast for extra feedback.
                        popMsg("UMS successfully enabled.");
                    }
                    else {
                        // Show a toast if there were any errors with executing the shell command.
                        popMsg("Failure! Did you granted root permissions?");
                    }
                    break;

                // Invoke when the blue Droid (disable UMS) is clicked.
                case R.id.UMSoff:
                    disable = runRootCommand("echo 0 > /sys/devices/platform/usb_mass_storage/lun0/file");
                    if(disable == true){
                        // Change text view content for current UMS state.
                        UMSstate.setText("disabled.");
                        // Show a toast for extra feedback.
                        popMsg("UMS successfully disabled.");
                    }
                    else {
                        // Show a toast if there were any errors with executing the shell command.
                        popMsg("Failure! Did you granted root permissions?");
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
        public static void getUmsState() {
            String UMS_STATE = "0";
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/platform/usb_mass_storage/lun0/file"), 256);
                try {
                    UMS_STATE = reader.readLine();
                } finally {
                    reader.close();
                }

                if (UMS_STATE.equals("/dev/block/vold/179:1")) {
                    UMSstate.setText("enabled.");
                } else {
                    UMSstate.setText("disabled.");
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

}
