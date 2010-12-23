/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.lge.cameratest;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lge.cameratest.gallery.IImage;
import com.lge.cameratest.gallery.IImageList;
import com.lge.cameratest.ui.CamcorderHeadUpDisplay;
import com.lge.cameratest.ui.GLRootView;
import com.lge.cameratest.ui.GLView;
import com.lge.cameratest.ui.HeadUpDisplay;

/**
 * The Camcorder activity.
 */
public class VideoCamera extends NoSearchActivity
        implements View.OnClickListener,
        ShutterButton.OnShutterButtonListener, SurfaceHolder.Callback,
        MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener,
        Switcher.OnSwitchListener, PreviewFrameLayout.OnSizeChangedListener {
	private android.hardware.Camera mCameraDevice;
	private static boolean mPhoneTestMode = false; // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation
    private static final int PHONE_TEST_MODE_PLAY = 0;
 // 20100802 TELEWORKS comespain@lge.com Phone Test Mode Implementation[start]
	public static boolean mATCMDMode = false; // 20100730 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation
	public static int mOperationMode = 0;
	private BroadcastReceiver mAtCmdReceiver = null;
	
	public static VideoCamera mContext = null;
	/*public static Handler mATCMDHandler = new Handler();
	public static Runnable mATCMDRunnable = new Runnable() {		
		public void run() {
			mOperationMode = ATCMDReceiver.mMode;
			VideoCamera context = mContext;
			if(mOperationMode !=0 && mContext.mSurfaceHolder!=null){
				switch(mOperationMode){				
				case 1:
					ATCMDReceiver.mCurrentMode = ATCMDReceiver.CAMCORDER_MODE_OFF;
					context.finish();
				case 2:
					if(context.mMediaRecorderRecording){
						return;
					}
					context.startVideoRecording();
					break;
				case 3:
					if(context.mMediaRecorderRecording){
						context.stopVideoRecording();
					}
					break;
				case 4:
					context.startPlayVideoActivity();
					break;
				case 5:
					context.discardCurrentVideoAndInitRecorder();					
					break;
				case 6:
					context.mParameters.setFlashMode(context.mParameters.FLASH_MODE_TORCH);
	                context.mCameraDevice.setParameters(context.mParameters);
					break;
				case 7:
					context.mParameters.setFlashMode(context.mParameters.FLASH_MODE_OFF);
	                context.mCameraDevice.setParameters(context.mParameters);
					break;

				default:
					break;
				}
//				mATCMDHandler.postDelayed(mATCMDRunnable, 500);
			}
		}
	};*/
	// 20100802 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
	
	private static boolean mAfter3secondRecording = false;
    private static final String TAG = "videocamera";

    private static final int INIT_RECORDER = 3;
    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    // The brightness settings used when it is set to automatic in the system.
    // The reason why it is set to 0.7 is just because 1.0 is too bright.
    private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;

    private static final long NO_STORAGE_ERROR = -1L;
    private static final long CANNOT_STAT_ERROR = -2L;
    private static final long LOW_STORAGE_THRESHOLD = 512L * 1024L;

    private static final int STORAGE_STATUS_OK = 0;
    private static final int STORAGE_STATUS_LOW = 1;
    private static final int STORAGE_STATUS_NONE = 2;

    private static final boolean SWITCH_CAMERA = true;
    private static final boolean SWITCH_VIDEO = false;

    private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms

    /**
     * An unpublished intent flag requesting to start recording straight away
     * and return as soon as recording is stopped.
     * TODO: consider publishing by moving into MediaStore.
     */
    private final static String EXTRA_QUICK_CAPTURE =
            "android.intent.extra.quickCapture";

    private SharedPreferences mPreferences;

    private PreviewFrameLayout mPreviewFrameLayout;
    private SurfaceView mVideoPreview;
    private SurfaceHolder mSurfaceHolder = null;
    private ImageView mVideoFrame;
    private GLRootView mGLRootView;
    private CamcorderHeadUpDisplay mHeadUpDisplay;

    private boolean mIsVideoCaptureIntent;
    private boolean mQuickCapture;
    // mLastPictureButton and mThumbController
    // are non-null only if mIsVideoCaptureIntent is true.
    private ImageView mLastPictureButton;
    private ThumbnailController mThumbController;
    private boolean mStartPreviewFail = false;

    private int mStorageStatus = STORAGE_STATUS_OK;


    private static final String PARM_CAPTURE = "capture";

    private static final String PARM_CAPTURE_VIDEO = "video";

    private static final String PARM_CAF = "caf";
    private static final String PARM_BRIGHTNESS = "brightness";
    private static final String PARM_CONTRAST = "contrast";
    private static final String PARM_SATURATION = "saturation";
    
    private MediaRecorder mMediaRecorder;
    private boolean mMediaRecorderRecording = false;
    private long mRecordingStartTime;
    // The video file that the hardware camera is about to record into
    // (or is recording into.)
    private String mCameraVideoFilename;
    private FileDescriptor mCameraVideoFileDescriptor;

    // The video file that has already been recorded, and that is being
    // examined by the user.
    private String mCurrentVideoFilename;
    private Uri mCurrentVideoUri;
    private ContentValues mCurrentVideoValues;

    private CamcorderProfile mProfile;

    // The video duration limit. 0 menas no limit.
    private int mMaxVideoDurationInMs;

    boolean mPausing = false;
    boolean mPreviewing = false; // True if preview is started.

    private ContentResolver mContentResolver;

    private ShutterButton mShutterButton;
    private TextView mRecordingTimeView;
    private Switcher mSwitcher;
    private boolean mRecordingTimeCountsDown = false;

    private final ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();

    private final Handler mHandler = new MainHandler();
    private final Handler mPhoneTestHandler = new TestHandler();
    private Parameters mParameters;

		//hyungtae.lee@lge.com S: block to go to sleep mode during test mode
    private PowerManager mPm = null;
    private PowerManager.WakeLock mWl = null;
    //hyungtae.lee@lge.com E: block to go to sleep mode during test mode
    
    // This Handler is used to post message back onto the main thread of the
    // application
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case ENABLE_SHUTTER_BUTTON:
                    mShutterButton.setEnabled(true);
                    break;

                case CLEAR_SCREEN_DELAY: {
//                    getWindow().clearFlags(
//                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // For DV Test Version comespain@lge.com
                    break;
                }

                case UPDATE_RECORD_TIME: {
                    updateRecordingTime();
                    break;
                }

                case INIT_RECORDER: {
                    initializeRecorder();
                    break;
                }

                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    }
    
    private class TestHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 0:
            	if(mSurfaceHolder != null && mMediaRecorder != null){
            		Log.i(TAG,"mSurfaceHolder != null && mMediaRecorder != null)");
//	            	initializeRecorder();
            		startVideoRecording();
            		
            	} else {
            		mPhoneTestHandler.sendEmptyMessageDelayed(0, 1000);
            	}
            }
                
        }
    }

    private BroadcastReceiver mReceiver = null;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                updateAndShowStorageHint(false);
                stopVideoRecording();
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                updateAndShowStorageHint(true);
                initializeRecorder();
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                // SD card unavailable
                // handled in ACTION_MEDIA_EJECT
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                Toast.makeText(VideoCamera.this,
                        getResources().getString(R.string.wait), 5000);
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                updateAndShowStorageHint(true);
            }
        }
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }

    private void showCameraBusyAndFinish() {
        Resources ress = getResources();
        Util.showFatalErrorAndFinish(VideoCamera.this,
                ress.getString(R.string.camera_error_title),
                ress.getString(R.string.cannot_connect_camera));
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPhoneTestMode = getIntent().getBooleanExtra("PhoneTestMode", false);
        
        // 20100810 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [start]
//      mATCMDMode = getIntent().getBooleanExtra("ATCMD", false);
        String actionName = getIntent().getAction();
		if (actionName != null) {
			if (getIntent().getAction().equals(
					"android.intent.action.atcmd.CAMCORDER_ON")) {
				// if(getIntent().getBooleanExtra("ATCMD", false)){
				mATCMDMode = true;
				mOperationMode = getIntent().getIntExtra("MODE", 1);
				atCommandListener();
				mContext = this;
			}
		}

   // 20100810 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [end]

        Window win = getWindow();

        // Overright the brightness settings if it is automatic
        int mode = Settings.System.getInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
            win.setAttributes(winParams);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        CameraSettings.upgradePreferences(mPreferences);

        readVideoPreferences();

        /*
         * To reduce startup time, we start the preview in another thread.
         * We make sure the preview is started at the end of onCreate.
         */
        Thread startPreviewThread = new Thread(new Runnable() {
            public void run() {
                try {
                    mStartPreviewFail = false;
                    startPreview();
                } catch (CameraHardwareException e) {
                    // In eng build, we throw the exception so that test tool
                    // can detect it and report it
                    if ("eng".equals(Build.TYPE)) {
                        throw new RuntimeException(e);
                    }
                    mStartPreviewFail = true;
                }
            }
        });
        startPreviewThread.start();

        mContentResolver = getContentResolver();

        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.video_camera);

        mPreviewFrameLayout = (PreviewFrameLayout)
                findViewById(R.id.frame_layout);
        mPreviewFrameLayout.setOnSizeChangedListener(this);
        resizeForPreviewAspectRatio();

        mVideoPreview = (SurfaceView) findViewById(R.id.camera_preview);
        mVideoFrame = (ImageView) findViewById(R.id.video_frame);

        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceCreated / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mIsVideoCaptureIntent = isVideoCaptureIntent();
        mQuickCapture = getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        mRecordingTimeView = (TextView) findViewById(R.id.recording_time);

        ViewGroup rootView = (ViewGroup) findViewById(R.id.video_camera);
        LayoutInflater inflater = this.getLayoutInflater();
        if (!mIsVideoCaptureIntent) {
            View controlBar = inflater.inflate(
                    R.layout.camera_control, rootView);
            mLastPictureButton =
                    (ImageView) controlBar.findViewById(R.id.review_thumbnail);
            mThumbController = new ThumbnailController(
                    getResources(), mLastPictureButton, mContentResolver);
            mLastPictureButton.setOnClickListener(this);
            mThumbController.loadData(ImageManager.getLastVideoThumbPath());
            mSwitcher = ((Switcher) findViewById(R.id.camera_switch));
            mSwitcher.setOnSwitchListener(this);
            mSwitcher.addTouchView(findViewById(R.id.camera_switch_set));
        } else {
            View controlBar = inflater.inflate(
                    R.layout.attach_camera_control, rootView);
            controlBar.findViewById(R.id.btn_cancel).setOnClickListener(this);
            ImageView retake =
                    (ImageView) controlBar.findViewById(R.id.btn_retake);
            retake.setOnClickListener(this);
            retake.setImageResource(R.drawable.btn_ic_review_retake_video);
            controlBar.findViewById(R.id.btn_play).setOnClickListener(this);
            controlBar.findViewById(R.id.btn_done).setOnClickListener(this);
        }

        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setImageResource(R.drawable.btn_ic_video_record);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.requestFocus();
        
        if(mPhoneTestMode){
        	mShutterButton.setVisibility(View.GONE);
        	mSwitcher.setVisibility(View.GONE);
        	mLastPictureButton.setVisibility(View.GONE); 
        	findViewById(R.id.camera_switch_set).setVisibility(View.GONE);
        }
        // Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mStartPreviewFail) {
                showCameraBusyAndFinish();
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }

    }

    private void changeHeadUpDisplayState() {
        // If the camera resumes behind the lock screen, the orientation
        // will be portrait. That causes OOM when we try to allocation GPU
        // memory for the GLSurfaceView again when the orientation changes. So,
        // we delayed initialization of HeadUpDisplay until the orientation
        // becomes landscape.
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
                && !mPausing && mGLRootView == null) {
            initializeHeadUpDisplay();
        } else if (mGLRootView != null) {
            finalizeHeadUpDisplay();
        }
    }

    private void initializeHeadUpDisplay() {
        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
        mGLRootView = new GLRootView(this);
        frame.addView(mGLRootView);

        mHeadUpDisplay = new CamcorderHeadUpDisplay(this);
        CameraSettings settings = new CameraSettings(this, mParameters);

        PreferenceGroup group =
                settings.getPreferenceGroup(R.xml.video_preferences);
        if (mIsVideoCaptureIntent) {
            group = filterPreferenceScreenByIntent(group);
        }
        mHeadUpDisplay.initialize(this, group);
        mGLRootView.setContentPane(mHeadUpDisplay);
        mHeadUpDisplay.setListener(new MyHeadUpDisplayListener());
        
        if(mPhoneTestMode){
        	mGLRootView.setVisibility(View.GONE);
        }
    }

    private void finalizeHeadUpDisplay() {
        mHeadUpDisplay.collapse();
        ((ViewGroup) mGLRootView.getParent()).removeView(mGLRootView);
        mGLRootView = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mIsVideoCaptureIntent) {
            mSwitcher.setSwitch(SWITCH_VIDEO);
        }
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW, mCurrentVideoUri);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't view video " + mCurrentVideoUri, ex);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_retake:
                discardCurrentVideoAndInitRecorder();
                break;
            case R.id.btn_play:
                startPlayVideoActivity();
                break;
            case R.id.btn_done:
                doReturnToCaller(true);
                break;
            case R.id.btn_cancel:
                stopVideoRecordingAndReturn(false);
                break;
            case R.id.review_thumbnail:
                if (!mMediaRecorderRecording) viewLastVideo();
                break;
        }
    }

    public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
        // Do nothing (everything happens in onShutterButtonClick).
    }

	protected void onStop() {
		Log.i(TAG,"park Video onStop");
		super.onStop();
	}

	private void onStopVideoRecording(boolean valid) {
        if (mIsVideoCaptureIntent) {
            if (mQuickCapture) {
                stopVideoRecordingAndReturn(valid);
            } else {
                stopVideoRecordingAndShowAlert();
            }
        } else {
            stopVideoRecordingAndGetThumbnail();
			if (mPhoneTestMode == true) {
				setVideoReview();
			} else {
				initializeRecorder();
			}
        }
    }

    public void onShutterButtonClick(ShutterButton button) {
        switch (button.getId()) {
            case R.id.shutter_button:
                if (mHeadUpDisplay.collapse()) return;

                if (mMediaRecorderRecording) {
                    onStopVideoRecording(true);
                } else if (mMediaRecorder != null) {
                    // If the click comes before recorder initialization, it is
                    // ignored. If users click the button during initialization,
                    // the event is put in the queue and record will be started
                    // eventually.
                    startVideoRecording();
                }
                mShutterButton.setEnabled(false);
                mHandler.sendEmptyMessageDelayed(
                        ENABLE_SHUTTER_BUTTON, SHUTTER_BUTTON_TIMEOUT);
                break;
        }
    }

    private void discardCurrentVideoAndInitRecorder() {
        deleteCurrentVideo();
        hideAlertAndInitializeRecorder();
    }

    private OnScreenHint mStorageHint;

    private void updateAndShowStorageHint(boolean mayHaveSd) {
        mStorageStatus = getStorageStatus(mayHaveSd);
        showStorageHint();
    }

    private void showStorageHint() {
        String errorMessage = null;
        switch (mStorageStatus) {
            case STORAGE_STATUS_NONE:
                errorMessage = getString(R.string.no_storage);
                break;
            case STORAGE_STATUS_LOW:
                errorMessage = getString(R.string.spaceIsLow_content);
        }
        if (errorMessage != null) {
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(this, errorMessage);
            } else {
                mStorageHint.setText(errorMessage);
            }
            mStorageHint.show();
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }

    private int getStorageStatus(boolean mayHaveSd) {
        long remaining = mayHaveSd ? getAvailableStorage() : NO_STORAGE_ERROR;
        if (remaining == NO_STORAGE_ERROR) {
            return STORAGE_STATUS_NONE;
        }
        return remaining < LOW_STORAGE_THRESHOLD
                ? STORAGE_STATUS_LOW
                : STORAGE_STATUS_OK;
    }

    private void readVideoPreferences() {
    	Log.i(TAG,"readVideoPreferences()");

        String quality = mPreferences.getString(
                                        CameraSettings.KEY_VIDEO_QUALITY,
                                        CameraSettings.DEFAULT_VIDEO_QUALITY_VALUE);
        boolean videoQualityHigh = CameraSettings.getVideoQuality(quality);

        int mVideoEncoder = 0;
        int mVideoFramerate = 0;
        int mVideoBitrate = 0;
        int mAudioEncoder = 0;
        int mVideoFormat = 0;
        int mOutputFormat = 0;

        mVideoFormat = getIntPreference(CameraSettings.KEY_VIDEO_FORMAT,
            CameraSettings.DEFAULT_VIDEO_FORMAT_VALUE);

        // Set video quality.
        Intent intent = getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int extraVideoQuality =
                    intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            videoQualityHigh = (extraVideoQuality > 0);
        }

        // Set video duration limit. The limit is read from the preference,
        // unless it is specified in the intent.
        if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)) {
            int seconds =
                    intent.getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0);
            mMaxVideoDurationInMs = 1000 * seconds;
        } else {
                    int minutes = getIntPreference(CameraSettings.KEY_VIDEO_DURATION, CameraSettings.DEFAULT_VIDEO_DURATION_VALUE);
                if (minutes == -1) {
                // This is a special case: the value -1 means we want to use the
                // device-dependent duration for MMS messages. The value is
                // represented in seconds.
                        mMaxVideoDurationInMs =
                        CameraSettings.getVidoeDurationInMillis("mms");
                } else {
                    // 1 minute = 60000ms
                    mMaxVideoDurationInMs = 60000 * minutes;
                }
        }
        if(mPhoneTestMode){
        	Log.i(TAG,"readVideoPreferences(), phonetestmode, setMaxDuration(3000)");
        	mMaxVideoDurationInMs = 3000;
//        	mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
        }
        Log.i(TAG,"Duration is now set to "+mMaxVideoDurationInMs+" ms");
        mProfile = CamcorderProfile.get(videoQualityHigh
                ? CamcorderProfile.QUALITY_HIGH
                : CamcorderProfile.QUALITY_LOW);

        mVideoEncoder = getIntPreference(CameraSettings.KEY_VIDEO_ENCODER,CameraSettings.DEFAULT_VIDEO_ENCODER_VALUE);

        mVideoFramerate = getIntPreference(CameraSettings.KEY_VIDEO_FRAMERATE,CameraSettings.DEFAULT_VIDEO_FRAMERATE_VALUE);

        mVideoBitrate = getIntPreference(CameraSettings.KEY_VIDEO_BITRATE,CameraSettings.DEFAULT_VIDEO_BITRATE_VALUE);

        mAudioEncoder = getIntPreference(CameraSettings.KEY_AUDIO_ENCODER,CameraSettings.DEFAULT_AUDIO_ENCODER_VALUE);

        mOutputFormat = getIntPreference(CameraSettings.KEY_OUTPUT_FORMAT,CameraSettings.DEFAULT_OUTPUT_FORMAT_VALUE);

        mProfile.videoFrameRate = mVideoFramerate;
//        mProfile.videoBitRate = mVideoBitrate;
        mProfile.videoBitRate = 4096*1000;
        Log.i(TAG,"readVideoPreferences(), mVideoBitrate = " + mProfile.videoBitRate);
        mProfile.videoCodec = mVideoEncoder;
        mProfile.audioCodec = mAudioEncoder;
        mProfile.fileFormat = mOutputFormat;
        updateVideoFormat(mVideoFormat);
    }

    private void resizeForPreviewAspectRatio() {
        mPreviewFrameLayout.setAspectRatio(
                (double) mProfile.videoFrameWidth / mProfile.videoFrameHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPausing = false;

			//hyungtae.lee@lge.com S: block to go to sleep mode during test mode
        if(mPm == null || mWl == null){
        	mPm = (PowerManager) getSystemService(POWER_SERVICE);
        	mWl = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Camera");
        }
        mWl.acquire();
      //hyungtae.lee@lge.com E: block to go to sleep mode during test mode
      
        mVideoPreview.setVisibility(View.VISIBLE);
      //hyungtae.lee@lge.com S: 테스트 시나리오에 불필요한 요소 제거
        if(mPhoneTestMode)
        	readVideoPreferences();
      //hyungtae.lee@lge.com E:
        resizeForPreviewAspectRatio();
        if (!mPreviewing && !mStartPreviewFail) {
            try {
                startPreview();
            } catch (CameraHardwareException e) {
                showCameraBusyAndFinish();
                return;
            }
        }
        keepScreenOnAwhile();

        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        mReceiver = new MyBroadcastReceiver();
        registerReceiver(mReceiver, intentFilter);
        mStorageStatus = getStorageStatus(true);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                showStorageHint();
            }
        }, 200);

     // 20100930 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [start]
//      mATCMDMode = getIntent().getBooleanExtra("ATCMD", false);
        String actionName = getIntent().getAction();
		if (actionName != null) {
			if (getIntent().getAction().equals(
					"android.intent.action.atcmd.CAMCORDER_ON")) {
				// if(getIntent().getBooleanExtra("ATCMD", false)){
				mATCMDMode = true;
				mOperationMode = getIntent().getIntExtra("MODE", 1);
				atCommandListener();
				mContext = this;
				initializeHeadUpDisplay(); // 20100930 comespain@lge.com added
			}
		}

   // 20100930 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [end]
     // 20100820 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
        if(mPhoneTestMode){
        	Log.i("park","startVideoRecording");
        	mPhoneTestHandler.sendEmptyMessageDelayed(0, 2000);
        } else if(mATCMDMode){
        	if(mLastPictureButton != null){
        	mLastPictureButton.setVisibility(View.GONE);
        	}
        }    	
     // 20100820 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
        if (mSurfaceHolder != null) {
            mHandler.sendEmptyMessage(INIT_RECORDER);
        }

        changeHeadUpDisplayState();
        

    }

    private void setPreviewDisplay(SurfaceHolder holder) {
        try {
            mCameraDevice.setPreviewDisplay(holder);
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }

    private void startPreview() throws CameraHardwareException {
        Log.v(TAG, "startPreview");
        if (mPreviewing) {
            // After recording a video, preview is not stopped. So just return.
            return;
        }

        if (mCameraDevice == null) {
            // If the activity is paused and resumed, camera device has been
            // released and we need to open the camera.
        	Log.i("park","mCameraDevice = CameraHolder.instance().open();");
            mCameraDevice = CameraHolder.instance().open();
        }

        mCameraDevice.lock();
        Log.i("lock","mCameraDevice.lock()");

        setCameraParameters();
        setPreviewDisplay(mSurfaceHolder);

        try {
            mCameraDevice.startPreview();
            mPreviewing = true;
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }

        // If setPreviewDisplay has been set with a valid surface, unlock now.
        // If surface is null, unlock later. Otherwise, setPreviewDisplay in
        // surfaceChanged will fail.
        if (mSurfaceHolder != null) {
            mCameraDevice.unlock();
            Log.i("lock","mCameraDevice.unlock()");
            
        }
    }

    private void closeCamera() {
        Log.v(TAG, "closeCamera");
        if (mCameraDevice == null) {
            Log.d(TAG, "already stopped.");
            return;
        }
        // If we don't lock the camera, release() will fail.
        mCameraDevice.lock();
        Log.i("lock","mCameraDevice.lock()");
        CameraHolder.instance().release();
        mCameraDevice = null;
        mPreviewing = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"park Video onPause");
        mPausing = true;

        changeHeadUpDisplayState();

        // Hide the preview now. Otherwise, the preview may be rotated during
        // onPause and it is annoying to users.
        mVideoPreview.setVisibility(View.INVISIBLE);

        // This is similar to what mShutterButton.performClick() does,
        // but not quite the same.
        if (mMediaRecorderRecording) {
            if (mIsVideoCaptureIntent) {
                stopVideoRecording();
                showAlert();
            } else {
                stopVideoRecordingAndGetThumbnail();
            }
        } else {
            stopVideoRecording();
        }
        closeCamera();

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        resetScreenOn();

        if (!mIsVideoCaptureIntent) {
            mThumbController.storeData(ImageManager.getLastVideoThumbPath());
        }

        if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }

        mHandler.removeMessages(INIT_RECORDER);
        
        if(mAtCmdReceiver != null){
        	unregisterReceiver(mAtCmdReceiver);
        	mAtCmdReceiver = null;
        }
        
         //hyungtae.lee@lge.com S: block to go to sleep mode during test mode
        if(mPm == null || mWl == null){
        	mPm = (PowerManager) getSystemService(POWER_SERVICE);
        	mWl = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Camera");
        }
        mWl.release();
      //hyungtae.lee@lge.com E: block to go to sleep mode during test mode
        //hyungtae.lee@lge.com S: 테스트 시나리오에 불필요한 요소 제거
        mPhoneTestMode = false ;
        //hyungtae.lee@lge.com E:

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!mMediaRecorderRecording) keepScreenOnAwhile();
    }

    @Override
    public void onBackPressed() {
        if (mPausing) return;
        if (mMediaRecorderRecording) {
            onStopVideoRecording(false);
        } else if (mHeadUpDisplay == null || !mHeadUpDisplay.collapse()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Do not handle any key if the activity is paused.
        if (mPausing) {
            return true;
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        	return true;
            case KeyEvent.KEYCODE_CAMERA:
                if (event.getRepeatCount() == 0) {
                    mShutterButton.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.getRepeatCount() == 0) {
                    mShutterButton.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                if (mMediaRecorderRecording) {
                    onStopVideoRecording(true);
                    return true;
                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                mShutterButton.setPressed(false);
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        if (mPausing) {
            // We're pausing, the screen is off and we already stopped
            // video recording. We don't want to start the camera again
            // in this case in order to conserve power.
            // The fact that surfaceChanged is called _after_ an onPause appears
            // to be legitimate since in that case the lockscreen always returns
            // to portrait orientation possibly triggering the notification.
            return;
        }

        // The mCameraDevice will be null if it is fail to connect to the
        // camera hardware. In this case we will show a dialog and then
        // finish the activity, so it's OK to ignore it.
        if (mCameraDevice == null) return;

        if (mMediaRecorderRecording) {
            stopVideoRecording();
        }

        // Set preview display if the surface is being created. Preview was
        // already started.
        if (holder.isCreating()) {
            setPreviewDisplay(holder);
            mCameraDevice.unlock();
            Log.i("lock","mCameraDevice.unlock()");
            mHandler.sendEmptyMessage(INIT_RECORDER);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    private void gotoGallery() {
        MenuHelper.gotoCameraVideoGallery(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (mIsVideoCaptureIntent) {
            // No options menu for attach mode.
            return false;
        } else {
            addBaseMenuItems(menu);
        }
        return true;
    }

    private boolean isVideoCaptureIntent() {
        String action = getIntent().getAction();
        return (MediaStore.ACTION_VIDEO_CAPTURE.equals(action));
    }

    private void doReturnToCaller(boolean valid) {
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = RESULT_OK;
            resultIntent.setData(mCurrentVideoUri);
        } else {
            resultCode = RESULT_CANCELED;
        }
        setResult(resultCode, resultIntent);
        finish();
    }

    /**
     * Returns
     *
     * @return number of bytes available, or an ERROR code.
     */
    private static long getAvailableStorage() {
        try {
            if (!ImageManager.hasStorage()) {
                return NO_STORAGE_ERROR;
            } else {
                String storageDirectory =
                        Environment.getExternalStorageDirectory().toString();
                StatFs stat = new StatFs(storageDirectory);
                return (long) stat.getAvailableBlocks()
                        * (long) stat.getBlockSize();
            }
        } catch (RuntimeException ex) {
            // if we can't stat the filesystem then we don't know how many
            // free bytes exist. It might be zero but just leave it
            // blank since we really don't know.
            return CANNOT_STAT_ERROR;
        }
    }

    private void cleanupEmptyFile() {
        if (mCameraVideoFilename != null) {
            File f = new File(mCameraVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.v(TAG, "Empty video file deleted: " + mCameraVideoFilename);
                mCameraVideoFilename = null;
            }
        }
    }

    

    // Prepares media recorder.
    private void initializeRecorder() {
        Log.v(TAG, "initializeRecorder");
        if (mMediaRecorder != null) {
        	Log.i(TAG,"mMediaRecorder !=null");
        	return;
        }

      //hyungtae.lee@lge.com S: 테스트 시나리오에 불필요한 요소 제거, 시나리오상 한번만 실행
        if(mPhoneTestMode){
        // We will call initializeRecorder() again when the alert is hidden.
        // If the mCameraDevice is null, then this activity is going to finish
        if (isAlertVisible() || mCameraDevice == null) return;

        Intent intent = getIntent();
        Bundle myExtras = intent.getExtras();

        long requestedSizeLimit = 0;
        if (mIsVideoCaptureIntent && myExtras != null) {
            Uri saveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                try {
                    mCameraVideoFileDescriptor =
                            mContentResolver.openFileDescriptor(saveUri, "rw")
                            .getFileDescriptor();
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    // invalid uri
                    Log.e(TAG, ex.toString());
                }
            }
            requestedSizeLimit = myExtras.getLong(MediaStore.EXTRA_SIZE_LIMIT);
        }
        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setCamera(mCameraDevice);
        Log.i(TAG,"mProfile.audioCodec = "+mProfile.audioCodec);
        if (mProfile.audioCodec != 0)
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
//        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setVideoSize(1280, 720);
        Log.i(TAG,"initializeRecorder, mProfile.videoBitRate = "+mProfile.videoBitRate);
//        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        mMediaRecorder.setVideoEncodingBitRate(4096000);

        if (mProfile.audioCodec != 0)
        {
            if (mProfile.audioCodec == 1)
                mMediaRecorder.setAudioEncodingBitRate(12200); //Bitrate for NBAMR
            else
//                mMediaRecorder.setAudioEncodingBitRate(8000); //Bitrate for AAC
            	Log.i(TAG,"mMediaRecorder.setAudioEncodingBitRate(64000);");
            	mMediaRecorder.setAudioEncodingBitRate(64000); 
            	mMediaRecorder.setAudioSamplingRate(22050);
            	mMediaRecorder.setAudioChannels(1);
        }
        Log.i(TAG,"mMediaRecorder, videoBitRate = "+mProfile.videoBitRate);
        Log.i(TAG,"mMediaRecorder, mProfile.videoFrameRate = "+mProfile.videoFrameRate);
        Log.i(TAG,"mMediaRecorder, mProfile.videoFrameWidth= "+mProfile.videoFrameWidth
        		+ "mProfile.videoFrameHeight = "+ mProfile.videoFrameHeight);
        Log.i(TAG,"mMediaRecorder, mProfile.fileFormat = "+ mProfile.fileFormat);

        // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
        if(mPhoneTestMode && mAfter3secondRecording == false){
        	Log.i(TAG,"initializeRecorder(), Phone Test Mode!! setMaxDuration(3000)");
        	mMaxVideoDurationInMs = 3000;
        	mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
        }
        // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
        else {
        	Log.i(TAG,"initializeRecorder. !!! mPhoneTestMode && mAfter3secondRecording == false");
        	Log.i(TAG,"mPhoneTestMode = "+mPhoneTestMode+", mAfter3secondRecording = "+mAfter3secondRecording);
        	mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
        }
//        if (mProfile.audioCodec != 0)
//            mMediaRecorder.setAudioSamplingRate(8000);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        if (mProfile.audioCodec != 0)
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);

        // Set output file.
        if (mStorageStatus != STORAGE_STATUS_OK) {
            mMediaRecorder.setOutputFile("/dev/null");
        } else {
            // Try Uri in the intent first. If it doesn't exist, use our own
            // instead.
            if (mCameraVideoFileDescriptor != null) {
                mMediaRecorder.setOutputFile(mCameraVideoFileDescriptor);
            } else {
                createVideoPath();
                mMediaRecorder.setOutputFile(mCameraVideoFilename);
            }
        }

        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        // Set maximum file size.
        // remaining >= LOW_STORAGE_THRESHOLD at this point, reserve a quarter
        // of that to make it more likely that recording can complete
        // successfully.
        long maxFileSize = getAvailableStorage() - LOW_STORAGE_THRESHOLD / 4;
        if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
            maxFileSize = requestedSizeLimit;
        }

        try {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        } catch (RuntimeException exception) {
            // We are going to ignore failure of setMaxFileSize here, as
            // a) The composer selected may simply not support it, or
            // b) The underlying media framework may not handle 64-bit range
            // on the size restriction.
        }

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare failed for " + mCameraVideoFilename);
            releaseMediaRecorder();
            throw new RuntimeException(e);
        }

        mMediaRecorderRecording = false;

        // Update the last video thumbnail.
        if (!mIsVideoCaptureIntent) {
            if (!mThumbController.isUriValid()) {
                updateLastVideo();
            }
            mThumbController.updateDisplayIfNeeded();
        }
		    //if(mPhoneTestMode){  // 조건문 중복이므로 삭제
        	setPhoneTestUi();
        }
        if(findViewById(R.id.testmode_video_play).getVisibility() == View.VISIBLE
        		&& mAfter3secondRecording == false){
        	findViewById(R.id.testmode_video_play).setVisibility(View.GONE);
			findViewById(R.id.testmode_pass).setVisibility(View.VISIBLE);
			findViewById(R.id.testmode_fail).setVisibility(View.VISIBLE);
	        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
	        am.setStreamVolume(AudioManager.STREAM_DTMF, am.getStreamMaxVolume(AudioManager.STREAM_DTMF), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			if (mGLRootView != null) {
				mGLRootView.setVisibility(View.GONE);
			}
        }
     
    }

	private void setVideoReview() {
		// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
        if(mPhoneTestMode && mAfter3secondRecording){
        	mAfter3secondRecording = false;
        	mPhoneTestMode = false;
        	Log.i(TAG,"findViewById(R.id.testmode_video_play).setVisibility(View.VISIBLE)");
        	findViewById(R.id.testmode_video_play).setVisibility(View.VISIBLE);
        	mShutterButton.setVisibility(View.GONE);
        	mSwitcher.setVisibility(View.GONE);
        	if(mPreviewing && mCameraDevice != null){
        		Log.i(TAG,"mPreviewing == true");
        		mPreviewing = false;        		
        	}
        }

     // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
//		Bitmap bmp = ThumbnailUtils.createVideoThumbnail(mCurrentVideoFilename, MediaStore.Video.Thumbnails.MINI_KIND);
//		ImageView showLastVideo = (ImageView)findViewById(R.id.showLastImageView);
//		showLastVideo.setImageBitmap(bmp);
//		showLastVideo.setVisibility(View.VISIBLE);

//		releaseMediaRecorder();
//		// mParameters.setFlashMode(mParameters.FLASH_MODE_OFF);
//		mCameraDevice.lock();
//		// mCameraDevice.setParameters(mParameters);
//		mCameraDevice.stopPreview();
//		Log.i(TAG, "stopVideoRecording, mCameraDevice.stopPreview()");
//		mCameraDevice.unlock();
		// initializeRecorder();
        
	}

	// 20101108 comespain@lge.com [start]
	private void setPhoneTestUi() {
		// mSwitcher.setVisibility(View.GONE);
		
//		findViewById(R.id.showLastImageView).setVisibility(View.GONE);
		/*findViewById(R.id.camera_switch_set).setVisibility(View.GONE);
		if (mLastPictureButton != null)
			mLastPictureButton.setVisibility(View.GONE);
		// mLastPictureButton.setVisibility(View.GONE);
		if (mGLRootView != null) {
			mGLRootView.setVisibility(View.GONE);
		}*/
		findViewById(R.id.testmode_video_play).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (!mMediaRecorderRecording) 
				{
			        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
			        am.setStreamVolume(AudioManager.STREAM_DTMF, 0, AudioManager.FLAG_PLAY_SOUND);
			        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			        Log.i(TAG,"mPhoneTestMode, setStreamVolume Max");
					File file = new File(mCurrentVideoFilename);
					Log.i(TAG,"mCurrentVideoFilename = "+mCurrentVideoFilename);
					Log.i(TAG,"mCurrentVideoUri = "+mCurrentVideoUri);
					Intent intent = new Intent(Intent.ACTION_VIEW, mCurrentVideoUri);					
					startActivity(intent);					
				
//				viewLastVideo();
//				v.setVisibility(View.INVISIBLE);
//				findViewById(R.id.testmode_pass).setVisibility(View.VISIBLE);
//				findViewById(R.id.testmode_fail).setVisibility(View.VISIBLE);
				}
				
			}
		});
		findViewById(R.id.testmode_pass).setOnClickListener(
				new OnClickListener() {

					public void onClick(View v) {
						Log.i(TAG, "Pass Clicked!");
//						Intent intent = new Intent();
//						intent.putExtra("VIDEO_TEST", true);
//						setResult(Activity.RESULT_OK, intent);
						setResult(Activity.RESULT_OK);
						finish();
					}
				});
		findViewById(R.id.testmode_fail).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Log.i(TAG, "Fail Clicked!");
						Intent intent = new Intent();
						intent.putExtra("VIDEO_TEST", false);
//						setResult(Activity.RESULT_OK, intent);
						setResult(Activity.RESULT_CANCELED);
						finish();
						// TODO Auto-generated method stub

					}
				});

	}

	// 20101108 comespain@lge.com [end]
    private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
//            cleanupEmptyFile();
//            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void createVideoPath() {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        int mOutputFormat =  mProfile.fileFormat;
        String fileSufix = ".3gp";
        String fileMimetype = "video/3gpp";

        if(mOutputFormat == MediaRecorder.OutputFormat.THREE_GPP){
            fileSufix = ".3gp";
            fileMimetype = "video/3gpp";
        } else if(mOutputFormat == MediaRecorder.OutputFormat.MPEG_4){
            fileSufix = ".mp4";
            fileMimetype = "video/mp4";
        }

        String filename = title + fileSufix; // Used when emailing.
        String cameraDirPath = ImageManager.CAMERA_IMAGE_BUCKET_NAME;
        String filePath = cameraDirPath + "/" + filename;
        File cameraDir = new File(cameraDirPath);
        cameraDir.mkdirs();
        ContentValues values = new ContentValues(7);
        values.put(Video.Media.TITLE, title);
        values.put(Video.Media.DISPLAY_NAME, filename);
        values.put(Video.Media.DATE_TAKEN, dateTaken);
        values.put(Video.Media.MIME_TYPE, fileMimetype);
        values.put(Video.Media.DATA, filePath);
        mCameraVideoFilename = filePath;
        Log.v(TAG, "Current camera video filename: " + mCameraVideoFilename);
        mCurrentVideoValues = values;
    }

    private void registerVideo() {
        if (mCameraVideoFileDescriptor == null) {
            Uri videoTable = Uri.parse("content://media/external/video/media");
            mCurrentVideoValues.put(Video.Media.SIZE,
                    new File(mCurrentVideoFilename).length());
            try {
                mCurrentVideoUri = mContentResolver.insert(videoTable,
                        mCurrentVideoValues);
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                mCurrentVideoUri = null;
                mCurrentVideoFilename = null;
            } finally {
                Log.v(TAG, "Current video URI: " + mCurrentVideoUri);
            }
        }
        mCurrentVideoValues = null;
    }

    private void deleteCurrentVideo() {
        if (mCurrentVideoFilename != null) {
            deleteVideoFile(mCurrentVideoFilename);
            mCurrentVideoFilename = null;
        }
        if (mCurrentVideoUri != null) {
            mContentResolver.delete(mCurrentVideoUri, null, null);
            mCurrentVideoUri = null;
        }
        updateAndShowStorageHint(true);
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    private void addBaseMenuItems(Menu menu) {
        MenuHelper.addSwitchModeMenuItem(menu, false, new Runnable() {
            public void run() {
                switchToCameraMode();
            }
        });
        MenuItem gallery = menu.add(Menu.NONE, Menu.NONE,
                MenuHelper.POSITION_GOTO_GALLERY,
                R.string.camera_gallery_photos_text)
                .setOnMenuItemClickListener(
                    new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            gotoGallery();
                            return true;
                        }
                    });
        gallery.setIcon(android.R.drawable.ic_menu_gallery);
        mGalleryItems.add(gallery);
    }

    private PreferenceGroup filterPreferenceScreenByIntent(
            PreferenceGroup screen) {
        Intent intent = getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            CameraSettings.removePreferenceFromScreen(screen,
                    CameraSettings.KEY_VIDEO_QUALITY);
        }

        if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)) {
            CameraSettings.removePreferenceFromScreen(screen,
                    CameraSettings.KEY_VIDEO_QUALITY);
        }
        return screen;
    }

    // from MediaRecorder.OnErrorListener
    public void onError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
        	Log.i(TAG,"onError, stopVideoRecording()");
            stopVideoRecording();
            updateAndShowStorageHint(true);
        }
    }

    // from MediaRecorder.OnInfoListener
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
        	Log.i(TAG,"onInfo, MEDIA_RECORDER_INFO_MAX_DURATION_REACHED");
            if (mMediaRecorderRecording) onStopVideoRecording(true);
        } else if (what
                == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            if (mMediaRecorderRecording) onStopVideoRecording(true);

            // Show the toast.
            Toast.makeText(VideoCamera.this, R.string.video_reach_size_limit,
                           Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Make sure we're not recording music playing in the background, ask the
     * MediaPlaybackService to pause playback.
     */
    private void pauseAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which
        // should be public, but isn't.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");

        sendBroadcast(i);
    }

    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        if (!mMediaRecorderRecording) {

            if (mStorageStatus != STORAGE_STATUS_OK) {
                Log.v(TAG, "Storage issue, ignore the start request");
                return;
            }

            // Check mMediaRecorder to see whether it is initialized or not.
            if (mMediaRecorder == null) {
                Log.e(TAG, "MediaRecorder is not initialized.");
                return;
            }

            pauseAudioPlayback();

            try {
                mMediaRecorder.setOnErrorListener(this);
                mMediaRecorder.setOnInfoListener(this);
                mMediaRecorder.start(); // Recording is now started
                mRecordingStartTime = SystemClock.uptimeMillis();
            	//20101108 comespain@lge.com increase MIC performance  [start]
        		AudioManager sbm = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        		//sbm.setMicBoostModeOn(true); // this will be enabled by LGE
        		//20101108 comespain@lge.com increase MIC performance  [end]
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not start media recorder. ", e);
                return;
            }
            // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
            if(mPhoneTestMode){
            	mAfter3secondRecording = true;
            }
            // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
            mHeadUpDisplay.setEnabled(false);
            mMediaRecorderRecording = true;
            updateRecordingIndicator(false);
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            updateRecordingTime();
            keepScreenOn();
        }
    }

    private void updateRecordingIndicator(boolean showRecording) {
        int drawableId =
                showRecording ? R.drawable.btn_ic_video_record
                        : R.drawable.btn_ic_video_record_stop;
        Drawable drawable = getResources().getDrawable(drawableId);
        mShutterButton.setImageDrawable(drawable);
    }

    private void stopVideoRecordingAndGetThumbnail() {
    	Log.i(TAG,"stopVideoRecordingAndGetThumbnail, mPhoneTestMode = "+mPhoneTestMode);
        stopVideoRecording();
		if (mPhoneTestMode == false) {
			acquireVideoThumb();
		}
    }

    private void stopVideoRecordingAndReturn(boolean valid) {
        stopVideoRecording();
        doReturnToCaller(valid);
    }

    private void stopVideoRecordingAndShowAlert() {
        stopVideoRecording();
        showAlert();
    }

    private void showAlert() {
        fadeOut(findViewById(R.id.shutter_button));
        if (mCurrentVideoFilename != null) {
            mVideoFrame.setImageBitmap(
                    ThumbnailUtils.createVideoThumbnail(mCurrentVideoFilename,
                             Video.Thumbnails.MINI_KIND));
            mVideoFrame.setVisibility(View.VISIBLE);
        }
        int[] pickIds = {R.id.btn_retake, R.id.btn_done, R.id.btn_play};
        for (int id : pickIds) {
            View button = findViewById(id);
            fadeIn(((View) button.getParent()));
        }
    }

    private void hideAlert() {
        mVideoFrame.setVisibility(View.INVISIBLE);
        fadeIn(findViewById(R.id.shutter_button));
        int[] pickIds = {R.id.btn_retake, R.id.btn_done, R.id.btn_play};
        for (int id : pickIds) {
            View button = findViewById(id);
            fadeOut(((View) button.getParent()));
        }
    }

    private static void fadeIn(View view) {
        view.setVisibility(View.VISIBLE);
        Animation animation = new AlphaAnimation(0F, 1F);
        animation.setDuration(500);
        view.startAnimation(animation);
    }

    private static void fadeOut(View view) {
        view.setVisibility(View.INVISIBLE);
        Animation animation = new AlphaAnimation(1F, 0F);
        animation.setDuration(500);
        view.startAnimation(animation);
    }

    private boolean isAlertVisible() {
        return this.mVideoFrame.getVisibility() == View.VISIBLE;
    }

    private void viewLastVideo() {
        Intent intent = null;
        if (mThumbController.isUriValid()) {
        	Log.i(TAG,"viewLastVideo, mThumbController.isUriValid()");
        	intent = new Intent(Intent.ACTION_VIEW, mThumbController.getUri());
//            intent = new Intent(Util.REVIEW_ACTION, mThumbController.getUri());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                try {
                    intent = new Intent(Intent.ACTION_VIEW, mThumbController.getUri());
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "review video fail", e);
                }
            }
        } else {
            Log.e(TAG, "Can't view last video.");
        }
    }

    private void stopVideoRecording() {
        Log.v(TAG, "stopVideoRecording");

    	//20101108 comespain@lge.com increase MIC performance  [start]
		AudioManager sbm = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		//sbm.setMicBoostModeOn(false); // this will be enabled by LGE
		//20101108 comespain@lge.com increase MIC performance  [end]
        boolean needToRegisterRecording = false;
        if (mMediaRecorderRecording || mMediaRecorder != null) {
        	Log.i(TAG,"(mMediaRecorderRecording || mMediaRecorder != null)");
            if (mMediaRecorderRecording && mMediaRecorder != null) {
            	Log.i(TAG,"(mMediaRecorderRecording && mMediaRecorder != null)");
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.e(TAG, "stop fail: " + e.getMessage());
                }
                releaseMediaRecorder();
                if(mPhoneTestMode){
                    mCameraDevice.lock();
                    mCameraDevice.stopPreview();
                    mCameraDevice.unlock();
                    }
                mHeadUpDisplay.setEnabled(true);
                mCurrentVideoFilename = mCameraVideoFilename;
                Log.v(TAG, "Setting current video filename: "
                        + mCurrentVideoFilename);
                needToRegisterRecording = true;
                
            }
            mMediaRecorderRecording = false;
            mRecordingTimeView.setVisibility(View.GONE);
            releaseMediaRecorder();

            updateRecordingIndicator(true);
            
            keepScreenOnAwhile();
            
        }
        if (needToRegisterRecording && mStorageStatus == STORAGE_STATUS_OK) {
            registerVideo();
        }

        mCameraVideoFilename = null;
        mCameraVideoFileDescriptor = null;        

    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void keepScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void hideAlertAndInitializeRecorder() {
        hideAlert();
        mHandler.sendEmptyMessage(INIT_RECORDER);
    }

    private void acquireVideoThumb() {
        Bitmap videoFrame = ThumbnailUtils.createVideoThumbnail(
                mCurrentVideoFilename, Video.Thumbnails.MINI_KIND);
        mThumbController.setData(mCurrentVideoUri, videoFrame);
    }

    private static ImageManager.DataLocation dataLocation() {
        return ImageManager.DataLocation.EXTERNAL;
    }

    private void updateLastVideo() {
        IImageList list = ImageManager.makeImageList(
                        mContentResolver,
                        dataLocation(),
                        ImageManager.INCLUDE_VIDEOS,
                        ImageManager.SORT_ASCENDING,
                        ImageManager.CAMERA_IMAGE_BUCKET_ID);
        int count = list.getCount();
        if (count > 0) {
            IImage image = list.getImageAt(count - 1);
            Uri uri = image.fullSizeImageUri();
            mThumbController.setData(uri, image.miniThumbBitmap());
        } else {
            mThumbController.setData(null, null);
        }
        list.close();
    }

    private void updateRecordingTime() {
        if (!mMediaRecorderRecording) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;

        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0
                && delta >= mMaxVideoDurationInMs - 60000);
        
//        String timer = "mm:ss";
        
//        countdownRemainingTime = false;
        long next_update_delay = 1000 - (delta % 1000);
        long seconds;
        if (countdownRemainingTime) {
            delta = Math.max(0, mMaxVideoDurationInMs - delta);
            seconds = (delta + 999) / 1000;
        } else {
            seconds = delta / 1000; // round to nearest
        }

        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        String secondsString = Long.toString(remainderSeconds);
        if (secondsString.length() < 2) {
            secondsString = "0" + secondsString;
        }
        String minutesString = Long.toString(remainderMinutes);
        if (minutesString.length() < 2) {
            minutesString = "0" + minutesString;
        }
        String text = minutesString + ":" + secondsString;
        if (hours > 0) {
            String hoursString = Long.toString(hours);
            if (hoursString.length() < 2) {
                hoursString = "0" + hoursString;
            }
            text = hoursString + ":" + text;
        }
        mRecordingTimeView.setText(text);

        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

            int color = getResources().getColor(countdownRemainingTime
                    ? R.color.recording_time_remaining_text
                    : R.color.recording_time_elapsed_text);

            mRecordingTimeView.setTextColor(color);
        }
        
        Log.i(TAG,"updaterecordingTime(), mMaxVideoDurationInMs = "+mMaxVideoDurationInMs);
        Log.i(TAG,"updaterecordingTime(), delta = "+delta);
        if(delta == 0 && mPhoneTestMode){
//        	mMediaRecorderRecording = false;  
//        	initializeRecorder();
        }
        mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 100);
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private void setCameraParameters() {
    	Log.i(TAG,"setCameraParameters");
        mParameters = mCameraDevice.getParameters();

        mParameters.set(PARM_CAPTURE, PARM_CAPTURE_VIDEO);

        mParameters.setPreviewSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        Log.e(TAG,"mProfile.videoFrameWidth = "+mProfile.videoFrameWidth
        		+", mProfile.videoFrameHeight = "+mProfile.videoFrameHeight);
   //     mParameters.setPreviewFrameRate(mProfile.videoFrameRate);

        // Set flash mode.
        String flashMode = mPreferences.getString(
                CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE,
                getString(R.string.pref_omap3_camera_video_flashmode_default));
        Log.i(TAG,"flashMode = "+flashMode);
        List<String> supportedFlash = mParameters.getSupportedFlashModes();
        if (isSupported(flashMode, supportedFlash)) {
        	Log.i(TAG,"isSupported, flashMode = "+flashMode);
            mParameters.setFlashMode(flashMode);
        } else {
            flashMode = mParameters.getFlashMode();
            if (flashMode == null) {
                flashMode = getString(
                        R.string.pref_omap3_camera_flashmode_no_flash);
            }
        }

        // Set white balance parameter.
        String whiteBalance = mPreferences.getString(
                CameraSettings.KEY_WHITE_BALANCE,
                getString(R.string.pref_omap3_camera_whitebalance_default));
        if (isSupported(whiteBalance,
                mParameters.getSupportedWhiteBalance())) {
            mParameters.setWhiteBalance(whiteBalance);
        } else {
            whiteBalance = mParameters.getWhiteBalance();
            if (whiteBalance == null) {
                whiteBalance = Parameters.WHITE_BALANCE_AUTO;
            }
        }

        // Set color effect parameter.
        String colorEffect = mPreferences.getString(
                CameraSettings.KEY_COLOR_EFFECT,
                getString(R.string.pref_omap3_camera_coloreffect_default));
        if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
            mParameters.setColorEffect(colorEffect);
        }

       //Set framerate
       String framerate = mPreferences.getString(CameraSettings.KEY_VIDEO_FRAMERATE, (getString(R.string.pref_camera_videoframerate_default)));
       mParameters.setPreviewFrameRate(Integer.parseInt(framerate));
       mProfile.videoFrameRate = Integer.parseInt(framerate);
       Log.e(TAG,"Framerate is set to "+ mProfile.videoFrameRate);

       //Set Bitrate
       String bitrate = mPreferences.getString(CameraSettings.KEY_VIDEO_BITRATE, (getString(R.string.pref_camera_videobitrate_default)));
       Log.i(TAG,"Bitrate = "+bitrate);
       mProfile.videoBitRate = Integer.parseInt(bitrate);
       if(mProfile.videoBitRate == 0){
    	   mProfile.videoBitRate = 4096*1000;
       }
       Log.i(TAG,"Bitrate is set to "+ mProfile.videoBitRate);
       Log.i(TAG,"getString(R.string.pref_camera_videobitrate_default) = "+getString(R.string.pref_camera_videobitrate_default));

       //Set Video format
        String vidFormat = mPreferences.getString(CameraSettings.KEY_VIDEO_FORMAT, (getString(R.string.pref_camera_video_format_default)));
        int optVideoFormat = Integer.parseInt(vidFormat);
        updateVideoFormat(optVideoFormat);
        mParameters.setPreviewSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        Log.e(TAG," Dimensions are set to W "+ mProfile.videoFrameWidth+" and H "+mProfile.videoFrameHeight);

      //Set Video encoder
        String vidEncoder = mPreferences.getString(CameraSettings.KEY_VIDEO_ENCODER, (getString(R.string.pref_camera_videoencoder_default)));
        mProfile.videoCodec = Integer.parseInt(vidEncoder);

       //Set Audio encoder
        String audioEncoder = mPreferences.getString(CameraSettings.KEY_AUDIO_ENCODER, (getString(R.string.pref_camera_audioencoder_default)));
        mProfile.audioCodec = Integer.parseInt(audioEncoder);
        Log.i(TAG,"Audiocodec is set to "+audioEncoder);

      //Set Output format
        String outpFormat = mPreferences.getString(CameraSettings.KEY_OUTPUT_FORMAT, (getString(R.string.pref_camera_outputformat_default)));
        mProfile.fileFormat = Integer.parseInt(outpFormat);
        Log.i(TAG, "Output format is set to "+outpFormat);

        int caf = Integer.parseInt( mPreferences.getString(
                CameraSettings.KEY_CAF,
                getString(R.string.pref_omap3_camera_caf_default)) );
        mParameters.set(PARM_CAF, caf);

        int saturation = Integer.parseInt( mPreferences.getString(
                CameraSettings.KEY_SATURATION,
                getString(R.string.pref_omap3_camera_saturation_default)) );
        mParameters.set(PARM_SATURATION, saturation);

        int brightness = Integer.parseInt( mPreferences.getString(
                CameraSettings.KEY_BRIGHTNESS,
                getString(R.string.pref_omap3_camera_brightness_default)) );
        mParameters.set(PARM_BRIGHTNESS, brightness);

        int contrast = Integer.parseInt( mPreferences.getString(
                CameraSettings.KEY_CONTRAST,
                getString(R.string.pref_omap3_camera_contrast_default)) );
        mParameters.set(PARM_CONTRAST, contrast);
        
        mCameraDevice.setParameters(mParameters);
    }

    private boolean switchToCameraMode() {
        if (isFinishing() || mMediaRecorderRecording) return false;
        MenuHelper.gotoCameraMode(this);
        finish();
        return true;
    }

    public boolean onSwitchChanged(Switcher source, boolean onOff) {
        if (onOff == SWITCH_CAMERA) {
            return switchToCameraMode();
        } else {
            return true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);

        // If the camera resumes behind the lock screen, the orientation
        // will be portrait. That causes OOM when we try to allocation GPU
        // memory for the GLSurfaceView again when the orientation changes. So,
        // we delayed initialization of HeadUpDisplay until the orientation
        // becomes landscape.
        changeHeadUpDisplayState();
    }

    private void resetCameraParameters() {
    	Log.i(TAG,"resetCameraParameters");
        // We need to restart the preview if preview size is changed.
        Size size = mParameters.getPreviewSize();
        if (size.width != mProfile.videoFrameWidth
                || size.height != mProfile.videoFrameHeight) {
            // It is assumed media recorder is released before
            // onSharedPreferenceChanged, so we can close the camera here.
            closeCamera();
            try {
                resizeForPreviewAspectRatio();
                startPreview(); // Parameters will be set in startPreview().
            } catch (CameraHardwareException e) {
                showCameraBusyAndFinish();
            }
        } else {
            try {
                // We need to lock the camera before writing parameters.
                mCameraDevice.lock();
                Log.i("lock","mCameraDevice.lock()");
            } catch (RuntimeException e) {
            	Log.e(TAG,"mCameraDevice.lock() fail!!!!!!!!!!!");
                // When preferences are added for the first time, this method
                // will be called. But OnScreenSetting is not displayed yet and
                // media recorder still owns the camera. Lock will fail and we
                // just ignore it.
                return;
            }
            setCameraParameters();
            mCameraDevice.unlock();
            Log.i("lock","mCameraDevice.unlock()");
            Log.i("unlock","mCameraDevice.unlock()");
        }
    }

    public void onSizeChanged() {
        // TODO: update the content on GLRootView
    }

    private class MyHeadUpDisplayListener implements HeadUpDisplay.Listener {
        public void onSharedPreferencesChanged() {
            mHandler.post(new Runnable() {
                public void run() {
                    VideoCamera.this.onSharedPreferencesChanged();
                }
            });
        }

        public void onRestorePreferencesClicked() {
            mHandler.post(new Runnable() {
                public void run() {
                    VideoCamera.this.onRestorePreferencesClicked();
                }
            });
        }

        public void onPopupWindowVisibilityChanged(final int visibility) {
            mHandler.post(new Runnable() {
                public void run() {
                    VideoCamera.this.onPopupWindowVisibilityChanged(visibility);
                }
            });
        }
    }

    private void onPopupWindowVisibilityChanged(int visibility) {
        if (visibility == GLView.VISIBLE) {
            releaseMediaRecorder();
        } else {
            if (!mPausing && mSurfaceHolder != null) initializeRecorder();
        }
    }

    private void onRestorePreferencesClicked() {
        Runnable runnable = new Runnable() {
            public void run() {
            	Log.i(TAG,"onRestorePreferencesClicked");
                mHeadUpDisplay.restorePreferences(mParameters);
            }
        };
        MenuHelper.confirmAction(this,
                getString(R.string.confirm_restore_title),
                getString(R.string.confirm_restore_message),
                runnable);
    }
    
    private void onSharedPreferencesChanged() {
    	Log.i("park","onSharedPreferencesChanged");
        // ignore the events after "onPause()" or preview has not started yet
        if (mPausing) return;
        synchronized (mPreferences) {
            readVideoPreferences();
            // If mCameraDevice is not ready then we can set the parameter in
            // startPreview().
            if (mCameraDevice == null) return;
            resetCameraParameters();
        }
    }

    private void updateVideoFormat(int vidQuality) {

        if (vidQuality == 0) {
            mProfile.videoFrameWidth = 128;
            mProfile.videoFrameHeight = 96;
        } else if (vidQuality == 1) {
            mProfile.videoFrameWidth  = 176;
            mProfile.videoFrameHeight = 144;
        } else if (vidQuality == 2) {
            mProfile.videoFrameWidth  = 352;
            mProfile.videoFrameHeight = 288;
        } else if (vidQuality == 3) {
            mProfile.videoFrameWidth  = 320;
            mProfile.videoFrameHeight = 240;
        } else if (vidQuality == 4) {
            mProfile.videoFrameWidth  = 640;
            mProfile.videoFrameHeight = 480;
        } else if (vidQuality == 5) {
            mProfile.videoFrameWidth  = 704;
            mProfile.videoFrameHeight = 480;
        } else if (vidQuality == 6) {
            mProfile.videoFrameWidth  = 704;
            mProfile.videoFrameHeight = 576;
        } else if (vidQuality == 7) {
            mProfile.videoFrameWidth  = 720;
            mProfile.videoFrameHeight = 480;
        } else if (vidQuality == 8) {
            mProfile.videoFrameWidth  = 720;
            mProfile.videoFrameHeight = 576;
        } else if (vidQuality == 9) {
            // WVGA resolution
            mProfile.videoFrameWidth  = 800;
            mProfile.videoFrameHeight = 480;
        } else if (vidQuality == 10) {
            // 720P resolution
	        mProfile.videoFrameWidth  = 1280;
            mProfile.videoFrameHeight = 720;
        }

    }

    private int getIntPreference(String key, int defaultValue) {
        String s = mPreferences.getString(key, "");
        int result = defaultValue;
        try {
            result = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            // Ignore, result is already the default value.
        }
        return result;
    }
    
 // 20100820 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [START]
    private void atCommandListener() {
		if (mAtCmdReceiver == null) {
			mAtCmdReceiver = new BroadcastReceiver() {
				private static final String TAG = "Phone Test Camera ATCMDReceiver";

				// public static final int CAMERA_MODE_OFF = 1;
				// public static final int CAMERA_MODE_ON = 2;
				//	
				// public static final int CAMCORDER_MODE_OFF = 3;
				// public static final int CAMCORDER_MODE_ON = 4;

				public Camera mContext = null;
				public static final String CAMCORDER_MODE_OFF = "android.intent.action.atcmd.CAMCORDER_OFF";
				public static final String CAMCORDER_MODE_ON = "android.intent.action.atcmd.CAMCORDER_ON";
				public static final String CAMCORDER_MODE_SHOT = "android.intent.action.atcmd.RECORDING_START";
				public static final String CAMCORDER_MODE_STOP_SAVE = "android.intent.action.atcmd.CAMCORDER_STOP";
				public static final String CAMCORDER_MODE_PLAY = "android.intent.action.atcmd.CAMCORDER_PLAY";
				public static final String CAMCORDER_MODE_ERASE = "android.intent.action.atcmd.CAMCORDER_ERASE";
				public static final String CAMCORDER_MODE_FLASHON = "android.intent.action.atcmd.CAMCORDER_FLASHON";
				public static final String CAMCORDER_MODE_FLASHOFF = "android.intent.action.atcmd.CAMCORDER_FALSHOFF";
				public static final String CAMCORDER_MODE_ZOOMIN = "android.intent.action.atcmd.CAMCORDER_ZOOM_IN";
				public static final String CAMCORDER_MODE_ZOOMOUT = "android.intent.action.atcmd.CAMCORDER_ZOOM_OUT";
	
				@Override
				public void onReceive(Context context, Intent intent) {
					// TODO Auto-generated method stub
					String atcmd = intent.getAction();
					// mContext = (Camera)getApplicationContext();
					Log.i(TAG,"omReceive, ATCMD!!!");
					if (atcmd.equals(CAMCORDER_MODE_OFF)) {
						Log.e(TAG,atcmd);
						mOperationMode = 0;
						finish();
						/*if (mContext != null) { //20100930 comespain@lge.com modified
							finish();
						}*/

						Log.e(TAG, "mode = " + mOperationMode);
					} else if (atcmd.equals(CAMCORDER_MODE_ON)) {
						Log.e(TAG,atcmd);
						if (mOperationMode == 1) {
							return;
						}
						mOperationMode = 1;
						// Intent cameraStart = new
						// Intent().setClassName(context,
						// "com.lge.cameratest.Camera");
						// cameraStart.putExtra("ATCMD", true);
						// cameraStart.putExtra("MODE", mMode);
						// context.startActivity(cameraStart);

					} else if (atcmd.equals(CAMCORDER_MODE_SHOT)) {
						Log.i(TAG,atcmd);
//						mOperationMode = 2;
//						doSnap();
						if(mMediaRecorderRecording) return;
						else mShutterButton.performClick();

					} else if (atcmd.equals(CAMCORDER_MODE_STOP_SAVE)) {
						Log.e(TAG,atcmd);
						if(mMediaRecorderRecording) mShutterButton.performClick();
						
						else return;
//						mOperationMode = 4;
//						viewLastImage();

					} else if (atcmd.equals(CAMCORDER_MODE_PLAY)) {
						Log.e(TAG,atcmd);
//						mOperationMode = 6;
						if (!mMediaRecorderRecording) 
							{
							Log.i(TAG,"mMediaRecorderRecording = false");
							viewLastVideo();
							}
//						mCameraDevice.setParameters(mContext.mParameters);

					} else if (atcmd.equals(CAMCORDER_MODE_ERASE)) {
						Log.e(TAG,atcmd);
//						mOperationMode = 7;
						if(mMediaRecorderRecording){
							Log.e(TAG,"mMediaRecorderRecording = true");
							return;
						}
//						discardCurrentVideoAndInitRecorder();
						deleteCurrentVideo();

					}else if (atcmd.equals(CAMCORDER_MODE_ZOOMIN)) {
						Log.e(TAG,atcmd);
						if(mMediaRecorderRecording){
							Log.e(TAG,"mMediaRecorderRecording = true");
							return;
						}
//						mOperationMode = 7;


					}else if (atcmd.equals(CAMCORDER_MODE_ZOOMOUT)) {
						Log.e(TAG,atcmd);
						if(mMediaRecorderRecording){
							Log.e(TAG,"mMediaRecorderRecording = true");
							return;
						}
//						mOperationMode = 7;


					}else if (atcmd.equals(CAMCORDER_MODE_FLASHON)) {
						Log.e(TAG,atcmd);
//						mOperationMode = 7;
						if(mMediaRecorderRecording){
							Log.e(TAG,"mMediaRecorderRecording, FLASHON CANCELED");
							return;
						}
		            	releaseMediaRecorder();
		            	mParameters.setFlashMode(mParameters.FLASH_MODE_TORCH);
		            	mCameraDevice.lock();
		            	mCameraDevice.setParameters(mParameters);
		            	mCameraDevice.unlock();
		            	initializeRecorder();

					}else if (atcmd.equals(CAMCORDER_MODE_FLASHOFF)) {
						Log.e(TAG,atcmd);
//						mOperationMode = 7;
						if(mMediaRecorderRecording){
							Log.i(TAG,"mMediaRecorderRecording = true");
							return;
						}
		            	releaseMediaRecorder();
		            	mParameters.setFlashMode(mParameters.FLASH_MODE_OFF);
		            	mCameraDevice.lock();
		            	mCameraDevice.setParameters(mParameters);
		            	mCameraDevice.unlock();
		            	initializeRecorder();

					}			

				}
			};
			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_OFF");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_ON");
			iFilter.addAction("android.intent.action.atcmd.RECORDING_START");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_STOP");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_PLAY");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_ERASE");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_FLASHON");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_FALSHOFF");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_ZOOM_IN");
			iFilter.addAction("android.intent.action.atcmd.CAMCORDER_ZOOM_OUT");

			registerReceiver(mAtCmdReceiver, iFilter);
		}
	}
    // 20100820 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [END]

}
