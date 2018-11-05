package com.example.lehuy.lab4_rfid_http;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.galarzaa.androidthings.Rc522;
import com.android.volley.toolbox.StringRequest;

import java.io.IOException;

import java.lang.*;
import com.example.lehuy.lab4_rfid_http.MyRequest;




/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            readRFid();
            mHandler.postDelayed(mRunnable, INTERVAL);

        }
    };
    private static final int INTERVAL = 2000;

    private static final boolean LED_ON = true;
    private static final boolean LED_OFF = false;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SPI_PORT = "SPI0.0";
    private static final String PIN_RESET = "BCM25";
    private static final String LED_PIN = "BCM21";
    private static final String headerUrl = "http://demo1.chipfc.com/SensorValue/update?sensorid=7&sensorvalue=";

    private Rc522 mRc522 = null;
    private SpiDevice spiDevice = null;
    private Gpio resetPin = null;
    private Gpio ledPin = null;
    private MyRequest myRequest = null;



    /*
    StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d("StringRequest", "Success");
        }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d("StringRequest", "Failed");
        }
    });
    */



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myRequest = MyRequest.getInstance(this);
        //stringRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 1, 1f));

        //myRequest.addToRequestQueue(stringRequest);

        PeripheralManager pService = PeripheralManager.getInstance();

        try{
            /*Name based on Rasperry Pi 3*/
            spiDevice = pService.openSpiDevice(SPI_PORT);

            /*Reset Pin*/
            resetPin = pService.openGpio(PIN_RESET);
            configureButton(resetPin);
            mRc522 = new Rc522(spiDevice, resetPin);

            /*LED pin*/
            ledPin = pService.openGpio(LED_PIN);
            ledPin.setActiveType(Gpio.ACTIVE_HIGH);
            ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);



            mHandler.post(mRunnable);
        }catch (IOException e){
            Log.i(TAG,"Peripheral Device error: ");
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            if(spiDevice != null){
                spiDevice.close();
            }
            if(resetPin != null){
                resetPin.unregisterGpioCallback(mGpioCallBack);
                resetPin.close();
            }
            if(ledPin != null){
                ledPin.close();
            }

        }catch (IOException e) {
            Log.i(TAG, "onDestroy() error: ");
            e.printStackTrace();
        }
    }



    /*Initialize button configuration*/
    private void configureButton(Gpio button) throws IOException{
        button.setDirection(Gpio.DIRECTION_IN);
        button.setActiveType(Gpio.ACTIVE_HIGH);

        button.setEdgeTriggerType(Gpio.EDGE_RISING);
        button.registerGpioCallback(mGpioCallBack);
    }

    private GpioCallback mGpioCallBack = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error){
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    /*Send uid string to server */
    private void sendRFIDToServer(String rfid) {
        String url = headerUrl + rfid;
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("StringRequest", "Success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("StringRequest", "Failed");
            }
        });
        myRequest.addToRequestQueue(request);
    }


    /*Just like the name*/
    private void readRFid(){
        Log.i("Read RFID","Check in ");
        while(true){
            boolean success = mRc522.request();
            if(!success){

                continue;
            }
            success = mRc522.antiCollisionDetect();
            if(!success){

                continue;
            }



            byte[] uid = mRc522.getUid();
            mRc522.selectTag(uid);
            break;
        }


        try {
            ledPin.setValue(LED_ON);
        }catch (IOException e){
            e.printStackTrace();
        }

        /*Get uid string*/
        String uidString = mRc522.getUidString("");
        Log.i(TAG,"UID " + uidString);

        /*Send uid string*/
        sendRFIDToServer(uidString);

        //Stop crypto to allow subsequent readings
        mRc522.stopCrypto();
        try {
            ledPin.setValue(LED_OFF);
        }catch (IOException e){
            e.printStackTrace();
        }

    }


}
