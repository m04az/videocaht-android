package com.m0aaz.myandroidrtc;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;

import org.json.JSONObject;

import me.kevingleason.pnwebrtc.PnPeerConnectionClient;

/**
 * Created by MOaaZ on 8/10/16.. moaaz.elshazli@gmail.com
 */


public class IncomingCallActivity extends Activity {
    SharedPreferences prefs;
    private String username;
    private String callUser;

    private Pubnub mPubNub;
    private TextView mCallerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        prefs = getApplication().getSharedPreferences("mypubnub", 0);

        this.username = this.prefs.getString("username", "");

        Bundle extras = getIntent().getExtras();
        if (extras==null || !extras.containsKey(mConstants.CALL_USER)){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Need to pass username to IncomingCallActivity in intent extras (mConstants.CALL_USER).",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        this.callUser = extras.getString(mConstants.CALL_USER, "");
        this.mCallerID = (TextView) findViewById(R.id.caller_id);
        this.mCallerID.setText(this.callUser);

        this.mPubNub  = new Pubnub(mConstants.PUB_KEY, mConstants.SUB_KEY);
        this.mPubNub.setUUID(this.username);
    }

    public void acceptCall(View view){
        Intent intent = new Intent(IncomingCallActivity.this, VideoChatActivity.class);
        intent.putExtra(mConstants.USER_NAME, this.username);
        intent.putExtra(mConstants.CALL_USER, this.callUser);
        startActivity(intent);
        finish();
        return;
    }

    /**
     * Publish a hangup command if rejecting call.
     * @param view
     */
    public void rejectCall(View view){
        JSONObject hangupMsg = PnPeerConnectionClient.generateHangupPacket(this.username);
        this.mPubNub.publish(this.callUser,hangupMsg, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Intent intent = new Intent(IncomingCallActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(this.mPubNub!=null){
            this.mPubNub.unsubscribeAll();
            finish();
            return;
        }
    }
}
