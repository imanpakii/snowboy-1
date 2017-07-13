package ai.kitt.snowboy;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Created by imanyazdansepas on 6/10/17.
 */

public class SerialPort {
//===============================================================================================   Globals Variables   ===========================================================================================================

    private Process mProcess;
    public final static String SerialPort = "/dev/ttySAC0";
    final static String BaudRate = "38400";
    private boolean mStopSerial;
    private Button mBtn_WriteSerial;
    private EditText mET_Write;
    private ToggleButton mBtn_ReadSerial;
    private EditText mET_Read;
    private String mLine;
//===============================================================================================   NDK Prototype   ===========================================================================================================
//====================================================================================================   Functions   ==============================================================================================================
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   Set Baud Rate   ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    void setBaudRate() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            ///dev/ttySAC0
            //os.writeBytes("stty -F /dev/ttyS2 1152200\n");
            os.writeBytes("stty -F /dev/ttySAC0 38400\n");
            os.flush();
            Thread.sleep(100);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   Set Baud Rate   ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    void writeSerial(String data) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(SerialPort));
            bw.write(data);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   Recive serial Data   ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    void DataRecive() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(SerialPort));
            while (!mStopSerial) {
                while ((mLine = br.readLine()) != null) {
                    Log.e(TAG, mLine);
                    mHandler.sendEmptyMessage(0);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            mET_Read.setText(mLine);
        }
    };
    //------------------------------------------------------------------------------------------------------    super user call    ----------------------------------------------------------------------------------------------------------
    public void superUserCall() {
        try {
            mProcess = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   Send serial Data   ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    void SendData() {

    }
}