package com.m0aaz.myandroidrtc;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnRTCListener;
import me.kevingleason.pnwebrtc.PnRTCMessage;

/**
 * This chat will begin/subscribe to a video chat.
 * REQUIRED: The intent must contain a
 */
public class VideoChatActivity extends Activity {

    public static final String VIDEO_TRACK_ID = "videoPN";
    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private GLSurfaceView myVideoView;


    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videochat);


        //Initializing Your PnWebRTC Client
        Bundle extras = getIntent().getExtras();
        //if no user name
        if (extras == null || !extras.containsKey(mConstants.USER_NAME)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Need to pass username to VideoChatActivity in intent extras (mConstants.USER_NAME).",
                    Toast.LENGTH_SHORT).show();
            Log.e("no username" , null);
            finish();
            return;
        }
        //get user name from extras
        this.username  = extras.getString(mConstants.USER_NAME, "");
        Log.e(" video username  ", username);


        //to set up global configuration
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();
        this.pnRTCClient = new PnRTCClient(mConstants.PUB_KEY, mConstants.SUB_KEY, this.username);

        Log.e("pnclient", String.valueOf(pnRTCClient));
        Log.e("pubnub", String.valueOf(pnRTCClient.getPubNub()));
        Log.e("uuid", String.valueOf(pnRTCClient.getUUID()));



        // Returns the number of cams & front/back face device name
        int camNumber = VideoCapturerAndroid.getDeviceCount();
        Log.e("cam # ", String.valueOf(camNumber));

        String frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        String backFacingCam  = VideoCapturerAndroid.getNameOfBackFacingDevice();


        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturerAndroid capturer = (VideoCapturerAndroid) VideoCapturerAndroid.create(frontFacingCam);

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, this.pnRTCClient.videoConstraints());
        Log.e("local video source", String.valueOf(localVideoSource));
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);
        Log.e("local video track", String.valueOf(localVideoTrack));


        // First we create an AudioSource then we can create our AudioTrack
        AudioSource audioSource = pcFactory.createAudioSource(this.pnRTCClient.audioConstraints());
        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);


        this.myVideoView = (GLSurfaceView) findViewById(R.id.gl_surface);
        VideoRendererGui.setView(myVideoView, null);


        //VideoRendererGui.create(x, y, width, height, ScaleType, mirror?)
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        localRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);


        //create mediaStream of the resources
        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
        Log.e("media stree ",mediaStream.toString());


        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack);
        mediaStream.addTrack(localAudioTrack);
        Log.e("media stream ",mediaStream.toString());


        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom



        this.pnRTCClient.attachRTCListener(new MyRTCListener());
        this.pnRTCClient.attachLocalMediaStream(mediaStream);

        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient.listenOn(this.username);
        this.pnRTCClient.setMaxConnections(1);

        // If mConstants.CALL_USER is in the intent extras, auto-connect them.
        if (extras.containsKey(mConstants.CALL_USER)) {
            String callUser = extras.getString(mConstants.CALL_USER, "");
            connectToUser(callUser);
            Log.e("connecting to ", callUser);
        }


    }

    private void connectToUser(String callUser) {
        this.pnRTCClient.connect(callUser);
        Log.e("pnclient", String.valueOf(pnRTCClient));


    }

    private class MyRTCListener extends PnRTCListener {
        @Override
        public void onCallReady(String callId) {
            super.onCallReady(callId);
            Log.e("ready2receive a WebRTC", callId);
        }

        @Override
        public void onConnected(String userId) {
            super.onConnected(userId);
            Log.e("Ready to receive calls", userId);
        }

        @Override
        public void onDebug(PnRTCMessage message) {
            super.onDebug(message);
            try {
                Log.e("debuge ", String.valueOf(message));

            }catch (Exception e){

            }
        }

        @Override
        public void onLocalStream(final MediaStream localStream) {
            super.onLocalStream(localStream); // Will log values
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(localStream.videoTracks.size()==0) {
                        Log.e("local streaming", "non");
                        return;
                    }
                    localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                    Log.e("locala streaming", String.valueOf(localStream.videoTracks.size()));
                }
            });
        }
        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            super.onAddRemoteStream(remoteStream, peer); // Will log values
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoChatActivity.this,"Connected to " + peer.getId(), Toast.LENGTH_SHORT).show();
                    Log.e("connected toooo", peer.getId());

                    try {
                        if(remoteStream.videoTracks.size()==0 || remoteStream.videoTracks.size()==0) {
                            Log.e("no remte", "non");
                            return;
                        }
                        Log.e(" remote", " ok");

                        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                        //update(Callbacks renderer, int x, int y, int width, int height, VideoRendererGui.ScalingType scalingType, boolean mirror)
                        VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                        VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);

                    }
                    catch (Exception e){
                    Log.e("remote fail ", String.valueOf(e));
                    }
                }
            });
        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            Intent intent = new Intent(VideoChatActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }


    }
    public void hangup(View view) {
        try{
            this.pnRTCClient.closeAllConnections();
            finish();
            return;
        }catch (Exception e){
            Log.e("hangup" , String.valueOf(e));
        }

        startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
        finish();
        return;
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.myVideoView.onPause();
        this.localVideoSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.myVideoView.onResume();
        this.localVideoSource.restart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.localVideoSource != null) {
            this.localVideoSource.stop();
            finish();
            return;
        }
        if (this.pnRTCClient != null) {
            this.pnRTCClient.onDestroy();
            finish();
            return;
        }
    }




}