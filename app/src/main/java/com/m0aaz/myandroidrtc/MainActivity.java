package com.m0aaz.myandroidrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity  extends Activity {
    SharedPreferences prefs ;
    private String username;
    private Pubnub mPubNub;
    private String stdByChannel;
    EditText usertocall;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getApplication().getSharedPreferences("mySharedPreferences", 0);
        final Intent intent = new Intent(MainActivity.this, Login.class );

        //get username from sharedpref
        getName();

        //if no username make user signup
        if(username ==null ){
            startActivity(intent);
            finish();
            return;


        }else {
            usertocall = (EditText) findViewById(R.id.usertocall);
            this.stdByChannel = this.username + mConstants.STDBY_SUFFIX;

            // init pubnub account
            initPubNub();


            Button call = (Button) findViewById(R.id.call);
            call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    makeCall();
                }
            });


            Button button = (Button) findViewById(R.id.changename);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    prefs.edit().remove("username").commit();
                    startActivity(intent);
                    finish();
                    return;
                }
            });




        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getName();
    }

    public String getName(){
        username = prefs.getString("username", null);
        if (username != null){
            Log.e("username : " , username);
        }else {
            Log.e("no user ", "no user");
        }
        return username;

    }



    /**
     * Subscribe to standby channel so that it doesn't interfere with the WebRTC Signaling.
     */
    public void initPubNub(){
        this.mPubNub  = new Pubnub(mConstants.PUB_KEY, mConstants.SUB_KEY);
        this.mPubNub.setUUID(this.username);
        Log.e("mpn : ", String.valueOf(mPubNub));
        Log.e("ath : ", String.valueOf(mPubNub.getAuthKey()));
        Log.e("uuid : ", String.valueOf(mPubNub.getUUID()));
        subscribeStdBy();
    }

    /**
     * Subscribe to standby channel
     */
    private void subscribeStdBy(){
        try {
            this.mPubNub.subscribe(this.stdByChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.e("std : ", stdByChannel);
                    Log.e("MA-iPN", "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    JSONObject jsonMsg = (JSONObject) message;
                    try {
                        if (!jsonMsg.has(mConstants.JSON_CALL_USER)) return;     //Ignore Signaling messages.
                        String user = jsonMsg.getString(mConstants.JSON_CALL_USER);
                        dispatchIncomingCall(user);
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectCallback(String channel, Object message) {
                    Log.e("std : ", stdByChannel);
                    Log.e("MA-iPN", "CONNECTED: " + message.toString());
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.e("MA-iPN","ERROR: " + error.toString());
                }
            });
        } catch (PubnubException e){
            Log.e("HERE", String.valueOf(e));
        }
    }

    private void dispatchIncomingCall(String userId){
        showToast("Call from: " + userId);
        Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
        intent.putExtra(mConstants.USER_NAME, username);
        intent.putExtra(mConstants.CALL_USER, userId);
        startActivity(intent);
    }
    private void showToast(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void makeCall(){
        String callNum = usertocall.getText().toString();
        Log.e("user to call" , callNum);
        if (callNum.isEmpty() || callNum.equals(this.username)){
            showToast("Enter a valid user ID to call.");
            Log.e("no user to call" , "non");

            return;
        }
        dispatchCall(callNum);
    }
    public void dispatchCall(final String callNum){
        final String callNumStdBy = callNum + mConstants.STDBY_SUFFIX;
        this.mPubNub.hereNow(callNumStdBy, new Callback() {

            @Override
            public void successCallback(String channel, Object message) {
                Log.e("msg", String.valueOf(message));
                Log.e("MA-dC", "HERE_NOW: " +" CH - " + callNumStdBy + " " + message.toString());
                try {
                    int occupancy = ((JSONObject) message).getInt(mConstants.JSON_OCCUPANCY);
                    if (occupancy == 0) {
                        showToast("User is not online!");
                        return;
                    }
                    JSONObject jsonCall = new JSONObject();
                    jsonCall.put(mConstants.JSON_CALL_USER, username);
                    jsonCall.put(mConstants.JSON_CALL_TIME, System.currentTimeMillis());
                    mPubNub.publish(callNumStdBy, jsonCall, new Callback() {
                        @Override
                        public void successCallback(String channel, Object message) {
                            Log.e("MA-dC", "SUCCESS: " + message.toString());
                            Intent intent = new Intent(MainActivity.this, VideoChatActivity.class);
                            intent.putExtra(mConstants.USER_NAME, username);
                            intent.putExtra(mConstants.CALL_USER, callNum);  // Only accept from this number?
                            startActivity(intent);
                            finish();
                            return;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
