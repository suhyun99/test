/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lge.cameratest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.CameraProfile;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.lge.cameratest.gallery.IImage;
import com.lge.cameratest.gallery.IImageList;
import com.lge.cameratest.ui.CameraHeadUpDisplay;
import com.lge.cameratest.ui.GLRootView;
import com.lge.cameratest.ui.HeadUpDisplay;
import com.lge.cameratest.ui.ZoomController;

/** The Camera activity which can preview and take pictures. */
public class Camera extends NoSearchActivity implements View.OnClickListener,
        ShutterButton.OnShutterButtonListener, SurfaceHolder.Callback,
        Switcher.OnSwitchListener {
	
	private class TestHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 0:
            	doSnap();
            	
            case 1:
            	doFocus(false);
            }
                
        }
    }
	public TestHandler mTestHandler = new TestHandler();
	public static boolean mPhoneTestMode = false; // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation
	public static Uri mPhoneTestModeCapturedUri = null;
    private static final String TAG = "camera";
    public static boolean mCameraTestResult = false;

	// 20100802 TELEWORKS comespain@lge.com Phone Test Mode Implementation[start]
	public static boolean mATCMDMode = false; // 20100730 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation
	public static int mOperationMode = 0;
	public static boolean mShowLastImage = false;
	private BroadcastReceiver mAtCmdReceiver = null;	
	public static Camera mContext = null;
	/*public static Handler mATCMDHandler = new Handler();
	public static Runnable mATCMDRunnable = new Runnable() {		
		public void run() {
			mOperationMode = ATCMDReceiver.mMode;
			if(mOperationMode !=0 && mSurfaceHolder!=null){
				switch(mOperationMode){				
				case 1:
					mATCMDHandler.postDelayed(mATCMDRunnable, 500);
					break;
				case 2:
					doSnap();
					ATCMDReceiver.mMode = 1;
					mATCMDHandler.postDelayed(mATCMDRunnable, 500);
					break;
				case 4:
					mContext.viewLastImage();					
					break;
				case 5:
					ATCMDReceiver.mMode = 1;
					mATCMDHandler.postDelayed(mATCMDRunnable, 500);
					break;
				case 6:
					mContext.mParameters.setFlashMode(mContext.mParameters.FLASH_MODE_ON);
	                mContext.mCameraDevice.setParameters(mContext.mParameters);
	                mATCMDHandler.postDelayed(mATCMDRunnable, 500);
					break;
				case 9:
					mContext.mParameters.setFlashMode(mContext.mParameters.FLASH_MODE_OFF);
	                mContext.mCameraDevice.setParameters(mContext.mParameters);
	                mATCMDHandler.postDelayed(mATCMDRunnable, 500);
					break;
				case 10:
					mContext.finish();					
					break;
				
				case 3:
				case 7:	
				case 8:
				case 11:
				case 12:
				default:
					break;
				}
//				mATCMDHandler.postDelayed(mATCMDRunnable, 500);
			}
		}
	};*/
	// 20100802 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
    private static final int CROP_MSG = 1;
    private static final int FIRST_TIME_INIT = 2;
    private static final int RESTART_PREVIEW = 3;
    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 5;
    private static final int SHOW_LOW_LIGHT_INDICATOR = 6;
    private static final int HIDE_LOW_LIGHT_INDICATOR = 7;
    public static final int PHONE_TEST_MODE_RECORDING_START = 5;// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation
    public static final int PHONE_TEST_MODE_RECORDING_SHOW_CAPTURED_IMAGE = 6;// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation
    public static final int PHONE_TEST_MODE_RECORDING_SHOW_RECORDED_VIDEO = 7;// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_ZOOM = 2;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ALL = -1;

    // When setCameraParametersWhenIdle() is called, we accumulate the subsets
    // needed to be updated in mUpdateSet.
    private int mUpdateSet;

    // The brightness settings used when it is set to automatic in the system.
    // The reason why it is set to 0.7 is just because 1.0 is too bright.
    private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;
    private static final int FOCUS_BEEP_VOLUME = 100;


    private static final int ZOOM_STOPPED = 0;
    private static final int ZOOM_START = 1;
    private static final int ZOOM_STOPPING = 2;

    private int mZoomState = ZOOM_STOPPED;
    private boolean mSmoothZoomSupported = false;
    private int mZoomValue;  // The current zoom value.
    private int mZoomMax;
    private int mTargetZoomValue;

    private Parameters mParameters;
    private Parameters mInitialParams;

    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = 0;  // No rotation (landscape) by default.
    private SharedPreferences mPreferences;

    private static final int IDLE = 1;
    private static final int SNAPSHOT_IN_PROGRESS = 2;

    private static final boolean SWITCH_CAMERA = true;
    private static final boolean SWITCH_VIDEO = false;

    private int mStatus = IDLE;
    private static final String sTempCropFilename = "crop-temp";

    private android.hardware.Camera mCameraDevice;
    private ContentProviderClient mMediaProviderClient;
    private SurfaceView mSurfaceView;
    private static SurfaceHolder mSurfaceHolder = null;
    private ShutterButton mShutterButton;
    private FocusRectangle mFocusRectangle;
    private ToneGenerator mFocusToneGenerator;
    private GestureDetector mGestureDetector;
    private Switcher mSwitcher;
    private boolean mStartPreviewFail = false;

    private boolean mCaptureRunning = false;
    private boolean mLongPress = false;
    private static final int mFocusRectangleWidth = 200;
    private static final int mFocusRectangleHeight = 200;
    private int mFocusHorizontalOffset = 0;
    private int mFocusVerticalOffset = 0;

    private static final int mLowLightPeriodDelay = 5000;
    private LowLightThread mLowLightThread = new LowLightThread();
    private Timer mLowLightTimer;

    private LowLightIndicator mLowLightIndicator;

    private GLRootView mGLRootView;

    // mPostCaptureAlert, mLastPictureButton, mThumbController
    // are non-null only if isImageCaptureIntent() is true.
    private ImageView mLastPictureButton;
    private ThumbnailController mThumbController;

    // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
    private String mCropValue;
    private Uri mSaveUri;

    private static ImageCapture mImageCapture = null;
    private int mBurstImages = 0;

    private boolean mPreviewing;
    private boolean mPausing;
    private boolean mFirstTimeInitialized;
    private boolean mIsImageCaptureIntent;
    private boolean mRecordLocation;

    private static final int FOCUS_NOT_STARTED = 0;
    private static final int FOCUSING = 1;
    private static final int FOCUSING_SNAP_ON_FINISH = 2;
    private static final int FOCUS_SUCCESS = 3;
    private static final int FOCUS_FAIL = 4;
    private static int mFocusState = FOCUS_NOT_STARTED;

    private static final String PARM_CAPTURE = "capture";
    private static final String PARM_TOUCH_FOCUS = "touch-focus";
    private static final String PARM_SHARPNESS = "sharpness";
    private static final String PARM_BRIGHTNESS = "brightness";
    private static final String PARM_CONTRAST = "contrast";
    private static final String PARM_SATURATION = "saturation";
    private static final String PARM_MODE = "mode";
    private static final String PARM_IPP = "ippMode";
    private static final String PARM_SHUTTER = "shutter-enable";
    private static final String PARM_METER_MODE = "meter-mode";
    private static final String PARM_EXPOSURE_MODE = "exposure";
    private static final String PARM_ISO = "iso";
    private static final String PARM_CAF = "caf";

    private static final String PARM_CAPTURE_STILL = "still";
    private static final String PARM_ENABLED = "enabled";
    private static final String PARM_DISABLED = "disabled";

    private static final String PARM_RAW = "raw-dump";
    private static final String PARM_MANUALFOCUS = "manual-focus";
    private static final String PARM_ANTIBANDING = "antibanding";
    private static final String PARM_BURST_CAPTURE = "burst-capture";

	//woochan@lge.com, 20101208, INSERT START - AT command
    private static final String PARM_SWAP = "video-input";
    private static final int	VALUE_CAMERA_MAIN = 0;
    private static final int	VALUE_CAMERA_SEC = 1;
    private static int	mCameraSensor = VALUE_CAMERA_MAIN;
    
    private static final String PARM_ZOOM = "zoom";
    private static final int	VALUE_ZOOM_MIN = 0;
    private static final int	VALUE_ZOOM_MAX = 16;
    private static int	mZoomStatus = VALUE_ZOOM_MIN;
	//woochan@lge.com, 20101208, INSERT END - AT command
    
    private int mOrientation = 0;
    private static final int ORIENTATION_STEP = 90;
    private static final int ORIENTATION_LIMIT = 360;
    private static boolean mThumbnailPreview = true;
    private String mLastPreviewSize = null;
    private int mLastPreviewFramerate;
    private ContentResolver mContentResolver;
    private boolean mDidRegister = false;

    private final ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();

    private LocationManager mLocationManager = null;

    private final ShutterCallback mShutterCallback = new ShutterCallback();
    private final PostViewPictureCallback mPostViewPictureCallback =
            new PostViewPictureCallback();
    private final RawPictureCallback mRawPictureCallback =
            new RawPictureCallback();
    private final AutoFocusCallback mAutoFocusCallback =
            new AutoFocusCallback();
    private final ZoomListener mZoomListener = new ZoomListener();
    // Use the ErrorCallback to capture the crash count
    // on the mediaserver
    private final ErrorCallback mErrorCallback = new ErrorCallback();

    private long mFocusStartTime;
    private long mFocusCallbackTime;
    private long mCaptureStartTime;
    private long mShutterCallbackTime;
    private long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    private int mPicturesRemaining;

    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackFinishTime;

    // Add for test
    public static boolean mMediaServerDied = false;

    // Focus mode. Options are pref_omap3_camera_focusmode_entryvalues.
    private static String mFocusMode;
    private String mSceneMode;

    private final Handler mHandler = new MainHandler();
    private boolean mQuickCapture;
    private static CameraHeadUpDisplay mHeadUpDisplay;

  	//hyungtae.lee@lge.com S: block to go to sleep mode during test mode
    private PowerManager mPm = null;
    private PowerManager.WakeLock mWl = null;
    //hyungtae.lee@lge.com E: block to go to sleep mode during test mode
    
    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESTART_PREVIEW: {
                    restartPreview();
                    if (mJpegPictureCallbackTime != 0) {
                        long now = System.currentTimeMillis();
                        mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
                        Log.v(TAG, "mJpegCallbackFinishTime = "
                                + mJpegCallbackFinishTime + "ms");
                        mJpegPictureCallbackTime = 0;
                    }
                    break;
                }

                case CLEAR_SCREEN_DELAY: {
//                    getWindow().clearFlags(
//                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // For DV Test Version comespain@lge.com
                    break;
                }

                case FIRST_TIME_INIT: {
                    initializeFirstTime();
                    break;
                }

                case SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                    setCameraParametersWhenIdle(0);
                    break;
                }

                case SHOW_LOW_LIGHT_INDICATOR: {
                    mLowLightIndicator.activate();
                    break;
                }

                case HIDE_LOW_LIGHT_INDICATOR: {
                    mLowLightIndicator.deactivate();
                    break;
                }

            }
        }
    }

    private void resetExposureCompensation() {
        String value = mPreferences.getString(CameraSettings.KEY_EXPOSURE,
                CameraSettings.EXPOSURE_DEFAULT_VALUE);
        if (!CameraSettings.EXPOSURE_DEFAULT_VALUE.equals(value)) {
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_EXPOSURE, "0");
            editor.commit();
            if (mHeadUpDisplay != null) {
                mHeadUpDisplay.reloadPreferences();
            }
        }
    }

    private void keepMediaProviderInstance() {
        // We want to keep a reference to MediaProvider in camera's lifecycle.
        // TODO: Utilize mMediaProviderClient instance to replace
        // ContentResolver calls.
        if (mMediaProviderClient == null) {
            mMediaProviderClient = getContentResolver()
                    .acquireContentProviderClient(MediaStore.AUTHORITY);
        }
    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
    	Log.i(TAG,"initializeFirstTime()"); 
        if (mFirstTimeInitialized) return;

        // Create orientation listenter. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener = new OrientationEventListener(Camera.this) {
            @Override
            public void onOrientationChanged(int orientation) {
                // We keep the last known orientation. So if the user
                // first orient the camera then point the camera to
                //hyungtae.lee@lge.com S: Avoid to pop up SNR
//                if (orientation != ORIENTATION_UNKNOWN) {
//                    orientation += 90;
//                }
//                orientation = ImageManager.roundOrientation(orientation);
//                if (orientation != mLastOrientation) {
//                    mLastOrientation = orientation;
//                    Log.e("aaa","orientation = " +orientation);
//                    updateCameraParametersPreference();
//                    if (!mIsImageCaptureIntent)  {
//                        setOrientationIndicator(mLastOrientation);
//                    }
//                    if (mGLRootView != null) {
//                        mGLRootView.queueEvent(new Runnable() {
//                            public void run() {
//                                mHeadUpDisplay.setOrientation(mLastOrientation);
//                            }
//                        });
//                    }
//                } //hyungtae.lee@lge.com E: Avoid to pop up SNR
            }
        };
        mOrientationListener.enable();

        // Initialize location sevice.
        mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        mRecordLocation = RecordLocationPreference.get(
                mPreferences, getContentResolver());
        if (mRecordLocation) startReceivingLocationUpdates();

        keepMediaProviderInstance();
        checkStorage();

        // Initialize last picture button.
        mContentResolver = getContentResolver();
        if (!mIsImageCaptureIntent)  {
            findViewById(R.id.camera_switch).setOnClickListener(this);
            mLastPictureButton =
                    (ImageView) findViewById(R.id.review_thumbnail);
            mLastPictureButton.setOnClickListener(this);
            mThumbController = new ThumbnailController(
                    getResources(), mLastPictureButton, mContentResolver);
            mThumbController.loadData(ImageManager.getLastImageThumbPath());
            // Update last image thumbnail.
            updateThumbnailButton();
        }

        // Initialize shutter button.
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.setVisibility(View.VISIBLE);

        mFocusRectangle = (FocusRectangle) findViewById(R.id.focus_rectangle);
        updateFocusIndicator();
        mFocusRectangle.setPosition(mFocusRectangleWidth, mFocusRectangleHeight, (mSurfaceView.getWidth()/2 - mFocusRectangleWidth/2), (mSurfaceView.getHeight()/2 - mFocusRectangleHeight/2));

        initializeScreenBrightness();
        installIntentFilter();
        initializeFocusTone();
        initializeZoom();

        mFirstTimeInitialized = true;

        changeHeadUpDisplayState();
        addIdleHandler();
        
     // 20100810 TELEWORKS comespain@lge.com Phone Test Mode Implementation [START_LGE]
        if(mPhoneTestMode){
        	mParameters.setFlashMode(mParameters.FLASH_MODE_ON);
			mCameraDevice.setParameters(mParameters);
        } else if(mATCMDMode){
        	if(mLastPictureButton !=null){
        		mLastPictureButton.setVisibility(View.GONE);
        	}
        }
     // 20100810 TELEWORKS comespain@lge.com Phone Test Mode Implementation [END_LGE]
        
        // 20101108 comespain@lge.com [start]
      if(mPhoneTestMode){
    	setPhoneTestUi();
    }
    	// 20101108 comespain@lge.com [end]
    }

    private void addIdleHandler() {
        MessageQueue queue = getMainLooper().myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            public boolean queueIdle() {
                ImageManager.ensureOSXCompatibleFolder();
                return false;
            }
        });
    }

    private void updateThumbnailButton() {
        // Update last image if URI is invalid and the storage is ready.
        if (!mThumbController.isUriValid() && mPicturesRemaining >= 0) {
            updateLastImage();
        }
        mThumbController.updateDisplayIfNeeded();
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
    	Log.i(TAG,"initializeSecondTime()");
        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();

        // Start location update if needed.
        synchronized (mPreferences) {
            mRecordLocation = RecordLocationPreference.get(
                    mPreferences, getContentResolver());
        }
        if (mRecordLocation) startReceivingLocationUpdates();

        installIntentFilter();

        initializeFocusTone();

        keepMediaProviderInstance();
        checkStorage();

        mCameraDevice.setZoomChangeListener(mZoomListener);

        if (!mIsImageCaptureIntent) {
            updateThumbnailButton();
        }

        changeHeadUpDisplayState();
    }

    private void initializeZoom() {
        if (!mParameters.isZoomSupported()) return;

        // Maximum zoom value may change after preview size is set. Get the
        // latest parameters here.
        mParameters = mCameraDevice.getParameters();
        mZoomMax = mParameters.getMaxZoom();
        mSmoothZoomSupported = mParameters.isSmoothZoomSupported();
		if (!mPhoneTestMode) {
			mGestureDetector = new GestureDetector(this,
					new FocusGestureListener());
		}

        mCameraDevice.setZoomChangeListener(mZoomListener);
    }

    private void onZoomValueChanged(int index) {
    	Log.i(TAG,"onZoomValueChanged");
        if (mSmoothZoomSupported) {
        	Log.i(TAG,"onZoomValueChanged, mSmoothZoomSupported = true");
        	Log.i(TAG,"onZoomValueChanged");
            if (mTargetZoomValue != index && mZoomState != ZOOM_STOPPED) {
                mTargetZoomValue = index;
                if (mZoomState == ZOOM_START) {
                    mZoomState = ZOOM_STOPPING;
                    mCameraDevice.stopSmoothZoom();
                }
            } else if (mZoomState == ZOOM_STOPPED && mZoomValue != index) {
                mTargetZoomValue = index;
                mCameraDevice.startSmoothZoom(index);
                mZoomState = ZOOM_START;
            }
        } else {
        	Log.i(TAG,"onZoomValueChanged, mSmoothZoomSupported = false, mZoomValue = " + index);
            mZoomValue = index;
            setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
        }
    }

    private float[] getZoomRatios() {
        List<Integer> zoomRatios = mParameters.getZoomRatios();
        if (zoomRatios != null) {
            float result[] = new float[zoomRatios.size()];
            for (int i = 0, n = result.length; i < n; ++i) {
                result[i] = (float) zoomRatios.get(i) / 100f;
            }
            return result;
        } else {
            //1.0, 1.2, 1.44, 1.6, 1.8, 2.0
            float result[] = new float[mZoomMax + 1];
            for (int i = 0, n = result.length; i < n; ++i) {
                result[i] = 1 + i * 0.2f;
            }
            return result;
        }
    }

    private class FocusGestureListener extends
            GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            android.widget.FrameLayout frame = (android.widget.FrameLayout) findViewById(R.id.frame);
            mFocusHorizontalOffset = frame.getLeft();
            mFocusVerticalOffset = frame.getTop();
            mFocusHorizontalOffset += mSurfaceView.getLeft();
            mFocusVerticalOffset += mSurfaceView.getTop();

            x -= (mFocusRectangleWidth/2 + mFocusHorizontalOffset);
            y -= (mFocusRectangleHeight/2 + mFocusVerticalOffset);

            if(  ( x < 0 ) || ( x > mSurfaceView.getWidth() - mFocusRectangleWidth/2 ) )
                return true;

            if ( ( y < 0 ) || ( y > mSurfaceView.getHeight() - mFocusRectangleHeight/2 ) )
                return true;

            mFocusRectangle.setPosition(mFocusRectangleWidth, mFocusRectangleHeight, x, y);

            int previewWidth = mParameters.getPreviewSize().width;
            double scale = ((double) mSurfaceView.getWidth() ) / ( (double) previewWidth);
            x += mFocusRectangleWidth/2;
            y += mFocusRectangleHeight/2;
            x *= scale;
            y *= scale;

            mParameters.set(PARM_TOUCH_FOCUS, x + "," + y);
            mCameraDevice.setParameters(mParameters);
            doFocus(true);

            return true;
        }

        public boolean onSingleTapUp (MotionEvent e) {
            doFocus(false);

            return true;
        }

    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        if (!super.dispatchTouchEvent(m) && mGestureDetector != null) {
            return mGestureDetector.onTouchEvent(m);
        }
        return true;
    }

    LocationListener [] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                checkStorage();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                checkStorage();
                if (!mIsImageCaptureIntent)  {
                    updateThumbnailButton();
                }
            }
        }
    };

    private class LocationListener
            implements android.location.LocationListener {
        Location mLastLocation;
        boolean mValid = false;
        String mProvider;

        public LocationListener(String provider) {
            mProvider = provider;
            mLastLocation = new Location(mProvider);
        }

        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() == 0.0
                    && newLocation.getLongitude() == 0.0) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            // If GPS is available before start camera, we won't get status
            // update so update GPS indicator when we receive data.
            if (mRecordLocation
                    && LocationManager.GPS_PROVIDER.equals(mProvider)) {
                if (mHeadUpDisplay != null) {
                    mHeadUpDisplay.setGpsHasSignal(true);
                }
            }
            mLastLocation.set(newLocation);
            mValid = true;
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
            mValid = false;
        }

        public void onStatusChanged(
                String provider, int status, Bundle extras) {
            switch(status) {
                case LocationProvider.OUT_OF_SERVICE:
                case LocationProvider.TEMPORARILY_UNAVAILABLE: {
                    mValid = false;
                    if (mRecordLocation &&
                            LocationManager.GPS_PROVIDER.equals(provider)) {
                        if (mHeadUpDisplay != null) {
                            mHeadUpDisplay.setGpsHasSignal(false);
                        }
                    }
                    break;
                }
            }
        }

        public Location current() {
            return mValid ? mLastLocation : null;
        }
    }

    private final class ShutterCallback
            implements android.hardware.Camera.ShutterCallback {
        public void onShutter() {
            mShutterCallbackTime = System.currentTimeMillis();
            mShutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.v(TAG, "mShutterLag = " + mShutterLag + "ms");
            clearFocusState();
        }
    }

    private final class PostViewPictureCallback implements PictureCallback {
        public void onPictureTaken(
                byte [] data, android.hardware.Camera camera) {
            mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToPostViewCallbackTime = "
                    + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                    + "ms");
        }
    }

    private final class RawPictureCallback implements PictureCallback {
        public void onPictureTaken(
                byte [] rawData, android.hardware.Camera camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();

            if(mBurstImages > 0){
                mBurstImages--;
                Editor edit = mPreferences.edit();
                edit.putString(CameraSettings.KEY_BURST, Integer.toString(mBurstImages));
                edit.commit();

                try {
                    // TODO: 
                    //ListPreference burst_value = (ListPreference) mPreferenceScreen.findPreference(CameraSettings.KEY_BURST);
                    //ListPreference videoBitrate = group.findPreference(KEY_VIDEO_BITRATE);
                    //burst_value.setValue(Integer.toString(mBurstImages));
                } catch (Exception e) {
                    Log.e(TAG, "Error Exception message: " + e);
                }

                Log.v(TAG, "RawPictureCallback(): mBurstImages = " + mBurstImages);

            }

            if(mBurstImages == 0){
                mPreviewing = true;
                mStatus = IDLE;
            }
            Log.v(TAG, "State: mPreviewing = " + mPreviewing + "; mStatus = " + mStatus);
            Log.v(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
        }
    }

    private final class JpegPictureCallback implements PictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        public void onPictureTaken(
                final byte [] jpegData, final android.hardware.Camera camera) {
            if (mPausing) {
                return;
            }

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mShutterToPictureDisplayedTime =
                        mPostViewPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
            } else {
                mShutterToPictureDisplayedTime =
                        mRawPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mRawPictureCallbackTime;
            }
            Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");
            mHeadUpDisplay.setEnabled(true);

            if (!mIsImageCaptureIntent) {
                // We want to show the taken picture for a while, so we wait
                // for at least 1.2 second before restarting the preview.
                long delay = 1200 - mPictureDisplayedToJpegCallbackTime;
                if (delay < 0 || mQuickCapture) {
                    restartPreview();
                } else {
                    mHandler.sendEmptyMessageDelayed(RESTART_PREVIEW, delay);
                }
            }
            mImageCapture.storeImage(jpegData, camera, mLocation);

            // Calculate this in advance of each shot so we don't add to shutter
            // latency. It's true that someone else could write to the SD card in
            // the mean time and fill it, but that could have happened between the
            // shutter press and saving the JPEG too.
            calculatePicturesRemaining();

            if (mPicturesRemaining < 1) {
                updateStorageHint(mPicturesRemaining);
            }

            if (!mHandler.hasMessages(RESTART_PREVIEW)) {
                long now = System.currentTimeMillis();
                mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
                Log.v(TAG, "mJpegCallbackFinishTime = "
                        + mJpegCallbackFinishTime + "ms");
                mJpegPictureCallbackTime = 0;
            }
         // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
            if(mPhoneTestMode){
            mPhoneTestModeCapturedUri = mImageCapture.getLastCaptureUri();
            Log.i(TAG,"mPhoneTestModeCapturedUri = "+mPhoneTestModeCapturedUri);
            mShowLastImage = true;
            showLastImage();
            Button passButton = (Button)findViewById(R.id.testmode_pass);
            passButton.setVisibility(View.VISIBLE);
            Button failButton = (Button)findViewById(R.id.testmode_fail);
            failButton.setVisibility(View.VISIBLE);
            RotateAnimation ra = new RotateAnimation(0.0f, -90.0f, 0, 0);
            ra.setDuration(0);
            ra.setFillAfter(true);
//            passButton.startAnimation(ra);
//            failButton.startAnimation(ra);
//           
            }
         // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
        }
    }

    private final class AutoFocusCallback
            implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(
                boolean focused, android.hardware.Camera camera) {
            mFocusCallbackTime = System.currentTimeMillis();
            mAutoFocusTime = mFocusCallbackTime - mFocusStartTime;
            Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");
            mParameters.set(PARM_TOUCH_FOCUS, PARM_DISABLED);
            mCameraDevice.setParameters(mParameters);
            mFocusRectangle.setPosition(mFocusRectangleWidth, mFocusRectangleHeight, (mSurfaceView.getWidth()/2 - mFocusRectangleWidth/2), (mSurfaceView.getHeight()/2 - mFocusRectangleHeight/2));
            if (mFocusState == FOCUSING_SNAP_ON_FINISH) {
                // Take the picture no matter focus succeeds or fails. No need
                // to play the AF sound if we're about to play the shutter
                // sound.
                if (focused) {
                    mFocusState = FOCUS_SUCCESS;
                } else {
                    mFocusState = FOCUS_FAIL;
                }
                mImageCapture.onSnap();
            } else if (mFocusState == FOCUSING) {
                // User is half-pressing the focus key. Play the focus tone.
                // Do not take the picture now.
                ToneGenerator tg = mFocusToneGenerator;
                if (tg != null) {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                }
                if (focused) {
                    mFocusState = FOCUS_SUCCESS;
                } else {
                    mFocusState = FOCUS_FAIL;
                }
            } else if (mFocusState == FOCUS_NOT_STARTED) {
                // User has released the focus key before focus completes.
                // Do nothing.
            }
            updateFocusIndicator();
        }
    }

    private static final class ErrorCallback
        implements android.hardware.Camera.ErrorCallback {
        public void onError(int error, android.hardware.Camera camera) {
            if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
                 mMediaServerDied = true;
                 Log.v(TAG, "media server died");
            }
        }
    }

    private final class ZoomListener
            implements android.hardware.Camera.OnZoomChangeListener {
        public void onZoomChange(
                int value, boolean stopped, android.hardware.Camera camera) {
            Log.v(TAG, "Zoom changed: value=" + value + ". stopped="+ stopped);
            mZoomValue = value;
            // Keep mParameters up to date. We do not getParameter again in
            // takePicture. If we do not do this, wrong zoom value will be set.
            mParameters.setZoom(value);
            // We only care if the zoom is stopped. mZooming is set to true when
            // we start smooth zoom.
            if (stopped && mZoomState != ZOOM_STOPPED) {
                if (value != mTargetZoomValue) {
                    mCameraDevice.startSmoothZoom(mTargetZoomValue);
                    mZoomState = ZOOM_START;
                } else {
                    mZoomState = ZOOM_STOPPED;
                }
            }
        }
    }

    private class ImageCapture {

        private Uri mLastContentUri;

        byte[] mCaptureOnlyData;

        // Returns the rotation degree in the jpeg header.
        private int storeImage(byte[] data, Location loc) {
            try {
                long dateTaken = System.currentTimeMillis();
                String title = createName(dateTaken);
                String filename = title + ".jpg";
                Log.i(TAG,"filename = "+filename);
                int[] degree = new int[1];
                mLastContentUri = ImageManager.addImage(
                        mContentResolver,
                        title,
                        dateTaken,
                        loc, // location from gps/network
                        ImageManager.CAMERA_IMAGE_BUCKET_NAME, filename,
                        null, data,
                        degree, mThumbnailPreview);
                return degree[0];
            } catch (Exception ex) {
                Log.e(TAG, "Exception while compressing image.", ex);
                return 0;
            }
        }

        public void storeImage(final byte[] data,
                android.hardware.Camera camera, Location loc) {
            if (!mIsImageCaptureIntent) {
                int degree = storeImage(data, loc);
                sendBroadcast(new Intent(
                        "com.lge.cameratest.NEW_PICTURE", mLastContentUri));
                if ( mThumbnailPreview ) {
                    setLastPictureThumb(data, degree, mImageCapture.getLastCaptureUri());
                    mThumbController.updateDisplayIfNeeded();
                }
            } else {
                mCaptureOnlyData = data;
                showPostCaptureAlert();
            }
        }

        /**
         * Initiate the capture of an image.
         */
        public void initiate() {
            if (mCameraDevice == null) {
                return;
            }

            capture();
        }

        public Uri getLastCaptureUri() {
            return mLastContentUri;
        }

        public byte[] getLastCaptureData() {
            return mCaptureOnlyData;
        }

        private void capture() {
            mCaptureOnlyData = null;

            // Set rotation.
            mParameters.setRotation(mLastOrientation);

            // Clear previous GPS location from the parameters.
            mParameters.removeGpsData();

            // We always encode GpsTimeStamp
            mParameters.setGpsTimestamp(System.currentTimeMillis() / 1000);

            // Set GPS location.
            Location loc = mRecordLocation ? getCurrentLocation() : null;
            if (loc != null) {
                double lat = loc.getLatitude();
                double lon = loc.getLongitude();
                boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

                if (hasLatLon) {
                    mParameters.setGpsLatitude(lat);
                    mParameters.setGpsLongitude(lon);
                    mParameters.setGpsProcessingMethod(loc.getProvider().toUpperCase());
                    if (loc.hasAltitude()) {
                        mParameters.setGpsAltitude(loc.getAltitude());
                    } else {
                        // for NETWORK_PROVIDER location provider, we may have
                        // no altitude information, but the driver needs it, so
                        // we fake one.
                        mParameters.setGpsAltitude(0);
                    }
                    if (loc.getTime() != 0) {
                        // Location.getTime() is UTC in milliseconds.
                        // gps-timestamp is UTC in seconds.
                        long utcTimeSeconds = loc.getTime() / 1000;
                        mParameters.setGpsTimestamp(utcTimeSeconds);
                    }
                } else {
                    loc = null;
                }
            }

            mCameraDevice.setParameters(mParameters);

            mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
                    mPostViewPictureCallback, new JpegPictureCallback(loc));
            mPreviewing = false;
        }

        public void onSnap() {
            // If we are already in the middle of taking a snapshot then ignore.
            if (mPausing || mStatus == SNAPSHOT_IN_PROGRESS) {
                return;
            }
            mCaptureStartTime = System.currentTimeMillis();
            mPostViewPictureCallbackTime = 0;
            mHeadUpDisplay.setEnabled(false);
            mStatus = SNAPSHOT_IN_PROGRESS;

            mImageCapture.initiate();
        }

        private void clearLastData() {
            mCaptureOnlyData = null;
        }
    }

    private boolean saveDataToFile(String filePath, byte[] data) {
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(filePath);
            f.write(data);
        } catch (IOException e) {
            return false;
        } finally {
            MenuHelper.closeSilently(f);
        }
        return true;
    }

    private void setLastPictureThumb(byte[] data, int degree, Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 16;
        Bitmap lastPictureThumb =
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
        lastPictureThumb = Util.rotate(lastPictureThumb, degree);
        mThumbController.setData(uri, lastPictureThumb);
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                getString(R.string.image_file_name_format));

        return dateFormat.format(date);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = this;// 20100802 TELEWORKS comespain@lge.com Phone Test Mode Implementation
        mPhoneTestMode = getIntent().getBooleanExtra("PhoneTestMode", false);// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation

        // 20100810 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [start]
        Log.i(TAG,"getIntent().getAction() = "+getIntent().getAction());
        String actionName = getIntent().getAction();
		if (actionName != null) {
			if (getIntent().getAction().equals(
					"android.intent.action.atcmd.CAMERA_ON")) {
				// if(getIntent().getBooleanExtra("ATCMD", false)){
				// mOperationMode = getIntent().getIntExtra("MODE", 1);
				Log.i(TAG, "mATCMDMode = true");
				mATCMDMode = true;
				mOperationMode = 1;
				atCommandListener();
				// mATCMDHandler.postDelayed(mATCMDRunnable, 500);
			}
		}
        // 20100810 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [end]
        
        setContentView(R.layout.camera);
        mSurfaceView = (SurfaceView) findViewById(R.id.camera_preview);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        CameraSettings.upgradePreferences(mPreferences);

        mLastPreviewSize = getString(R.string.pref_omap3_camera_previewsize_default);
        mLastPreviewFramerate = Integer.parseInt(getString(R.string.pref_omap3_camera_previewframerate_default));

        mQuickCapture = getQuickCaptureSettings();

        // comment out -- unused now.
        //mQuickCapture = getQuickCaptureSettings();

        // we need to reset exposure for the preview
        resetExposureCompensation();
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

        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceChanged / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mIsImageCaptureIntent = isImageCaptureIntent();
        if (mIsImageCaptureIntent) {
            setupCaptureParams();
        }

        LayoutInflater inflater = getLayoutInflater();

        ViewGroup rootView = (ViewGroup) findViewById(R.id.camera);
        if (mIsImageCaptureIntent) {
            View controlBar = inflater.inflate(
                    R.layout.attach_camera_control, rootView);
            if(mPhoneTestMode){
                controlBar.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
                controlBar.findViewById(R.id.btn_retake).setVisibility(View.GONE);
                controlBar.findViewById(R.id.btn_done).setVisibility(View.GONE);
            } else {
            controlBar.findViewById(R.id.btn_cancel).setOnClickListener(this);
            controlBar.findViewById(R.id.btn_retake).setOnClickListener(this);
            controlBar.findViewById(R.id.btn_done).setOnClickListener(this);
            }
        } else {
            inflater.inflate(R.layout.camera_control, rootView);
            mSwitcher = ((Switcher) findViewById(R.id.camera_switch));
            mSwitcher.setOnSwitchListener(this);
            mSwitcher.addTouchView(findViewById(R.id.camera_switch_set));
        }

        mLowLightIndicator = (LowLightIndicator) findViewById(R.id.lowlight_indicator);
        mLowLightIndicator.deactivate();

        // Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mStartPreviewFail) {
                showCameraErrorAndFinish();
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
                && !mPausing && mFirstTimeInitialized) {
            if (mGLRootView == null) initializeHeadUpDisplay();
        } else if (mGLRootView != null) {
            finalizeHeadUpDisplay();
        }
    }

    private void overrideHudSettings(final String flashMode,
            final String whiteBalance, final String focusMode) {
        mGLRootView.queueEvent(new Runnable() {
            public void run() {
                mHeadUpDisplay.overrideSettings(
                        CameraSettings.KEY_FLASH_MODE, flashMode);
                mHeadUpDisplay.overrideSettings(
                        CameraSettings.KEY_WHITE_BALANCE, whiteBalance);
                mHeadUpDisplay.overrideSettings(
                        CameraSettings.KEY_FOCUS_MODE, focusMode);
            }});        
    }

    private void updateSceneModeInHud() {        
        // If scene mode is set, we cannot set flash mode, white balance, and
        // focus mode, instead, we read it from driver        
        if (!Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            overrideHudSettings(mParameters.getFlashMode(), 
                    mParameters.getWhiteBalance(), mParameters.getFocusMode());
        } else {
            overrideHudSettings(null, null, null);
        }
    }

    private void initializeHeadUpDisplay() {
        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
        mGLRootView = new GLRootView(this);
        frame.addView(mGLRootView);

        mHeadUpDisplay = new CameraHeadUpDisplay(this);
        CameraSettings settings = new CameraSettings(this, mInitialParams);
        mHeadUpDisplay.initialize(this,
                settings.getPreferenceGroup(R.xml.camera_preferences));
        mHeadUpDisplay.setListener(new MyHeadUpDisplayListener());
        mHeadUpDisplay.setOrientation(mLastOrientation);

        if (mParameters.isZoomSupported()) {
            mHeadUpDisplay.setZoomRatios(getZoomRatios());
            Log.i(TAG,"initializeHeadUpDisplay(), mParameters.isZoomSupported(), mZoomValue =  "+mZoomValue);
            mHeadUpDisplay.setZoomIndex(mZoomValue);
            mHeadUpDisplay.setZoomListener(new ZoomController.ZoomListener() {
                public void onZoomChanged(
                        final int index, float ratio, boolean isMoving) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            onZoomValueChanged(index);
                        }
                    });
                }
            });
        }

        updateSceneModeInHud();

        mGLRootView.setContentPane(mHeadUpDisplay);
    }

    private void finalizeHeadUpDisplay() {
        mHeadUpDisplay.setGpsHasSignal(false);
        mHeadUpDisplay.collapse();
        ((ViewGroup) mGLRootView.getParent()).removeView(mGLRootView);
        mGLRootView = null;
    }

    private void setOrientationIndicator(int degree) {
        ((RotateImageView) findViewById(
                R.id.review_thumbnail)).setDegree(degree);
        ((RotateImageView) findViewById(
                R.id.camera_switch_icon)).setDegree(degree);
        ((RotateImageView) findViewById(
                R.id.video_switch_icon)).setDegree(degree);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mIsImageCaptureIntent) {
            mSwitcher.setSwitch(SWITCH_CAMERA);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG,"park onStop");
        if (mMediaProviderClient != null) {
            mMediaProviderClient.release();
            mMediaProviderClient = null;
        }
        
    }

    private void checkStorage() {
        calculatePicturesRemaining();
        updateStorageHint(mPicturesRemaining);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_retake:
                hidePostCaptureAlert();
                restartPreview();
                break;
            case R.id.review_thumbnail:
                if (isCameraIdle()) {
                    viewLastImage();
                }
                break;
            case R.id.btn_done:
                doAttach();
                break;
            case R.id.btn_cancel:
                doCancel();
        }
    }

    private Bitmap createCaptureBitmap(byte[] data) {
        // This is really stupid...we just want to read the orientation in
        // the jpeg header.
        String filepath = ImageManager.getTempJpegPath();
        int degree = 0;
        if (saveDataToFile(filepath, data)) {
            degree = ImageManager.getExifOrientation(filepath);
            new File(filepath).delete();
        }

        // Limit to 50k pixels so we can return it in the intent.
        Bitmap bitmap = Util.makeBitmap(data, 50 * 1024);
        bitmap = Util.rotate(bitmap, degree);
        return bitmap;
    }

    private void doAttach() {
        if (mPausing) {
            return;
        }

        byte[] data = mImageCapture.getLastCaptureData();

        if (mCropValue == null) {
            // First handle the no crop case -- just return the value.  If the
            // caller specifies a "save uri" then write the data to it's
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mContentResolver.openOutputStream(mSaveUri);
                    outputStream.write(data);
                    outputStream.close();

                    setResult(RESULT_OK);
                    finish();
                } catch (IOException ex) {
                    // ignore exception
                } finally {
                    Util.closeSilently(outputStream);
                }
            } else {
                Bitmap bitmap = createCaptureBitmap(data);
                setResult(RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
                finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = getFileStreamPath(sTempCropFilename);
                path.delete();
                tempStream = openFileOutput(sTempCropFilename, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            } catch (IOException ex) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            } finally {
                Util.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (mCropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (mSaveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
            } else {
                newExtras.putBoolean("return-data", true);
            }

            Intent cropIntent = new Intent("com.lge.cameratest.action.CROP");

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            startActivityForResult(cropIntent, CROP_MSG);
        }
    }

    private void doCancel() {
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
    	Log.i(TAG,"onShutterButtonFocus");
        if (mPausing) {
            return;
        }
        switch (button.getId()) {
            case R.id.shutter_button:
                doFocus(pressed);
                break;
        }
    }

    public void onShutterButtonClick(ShutterButton button) {
    	Log.i(TAG,"onShutterButtonClick");
        if (mPausing) {
            return;
        }
        switch (button.getId()) {
            case R.id.shutter_button:
//            	onShutterButtonFocus(button, true);
            	doFocus(true);
//                doSnap();
                break;
        }
    }

    private OnScreenHint mStorageHint;

    private void updateStorageHint(int remaining) {
        String noStorageText = null;

        if (remaining == MenuHelper.NO_STORAGE_ERROR) {
            String state = Environment.getExternalStorageState();
            if (state == Environment.MEDIA_CHECKING) {
                noStorageText = getString(R.string.preparing_sd);
            } else {
                noStorageText = getString(R.string.no_storage);
            }
        } else if (remaining < 1) {
            noStorageText = getString(R.string.not_enough_space);
        }

        if (noStorageText != null) {
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(this, noStorageText);
            } else {
                mStorageHint.setText(noStorageText);
            }
            mStorageHint.show();
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }

    private void installIntentFilter() {
        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
        mDidRegister = true;
    }

    private void initializeFocusTone() {
        // Initialize focus tone generator.
        try {
            mFocusToneGenerator = new ToneGenerator(
                    AudioManager.STREAM_SYSTEM, FOCUS_BEEP_VOLUME);
        } catch (Throwable ex) {
            Log.w(TAG, "Exception caught while creating tone generator: ", ex);
            mFocusToneGenerator = null;
        }
    }

    private void initializeScreenBrightness() {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG," onResume!!!!!!!!!!!!!!!");

			//hyungtae.lee@lge.com S: block to go to sleep mode during test mode
        if(mPm == null || mWl == null){
        	mPm = (PowerManager) getSystemService(POWER_SERVICE);
        	mWl = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Camera");
        }
        mWl.acquire();
      //hyungtae.lee@lge.com E: block to go to sleep mode during test mode
      
        mPausing = false;
        mJpegPictureCallbackTime = 0;
        mZoomValue = 0;
        mImageCapture = new ImageCapture();

        // Start the preview if it is not started.
        if (!mPreviewing && !mStartPreviewFail) {
        	Log.i(TAG,"onResume, resetExposureCompensation");
            resetExposureCompensation();
            try {
                startPreview();
            } catch (CameraHardwareException e) {
                showCameraErrorAndFinish();
                return;
            }
        }

        if (mSurfaceHolder != null) {
            // If first time initialization is not finished, put it in the
            // message queue.
            if (!mFirstTimeInitialized) {
                mHandler.sendEmptyMessage(FIRST_TIME_INIT);
            } else {
                initializeSecondTime();
            }
        }
        keepScreenOnAwhile();
        
     // 20100730 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation[START]
        if(mATCMDMode){
        	if(mLastPictureButton !=null)
        	mLastPictureButton.setVisibility(View.GONE);
        }
     // 20100730 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation[END]

    }
 // 20101108 comespain@lge.com [start]
    private void setPhoneTestUi() {
//    	mSwitcher.setVisibility(View.GONE);
    	mLowLightIndicator.setVisibility(View.GONE);
    	findViewById(R.id.showLastImageView).setVisibility(View.GONE);
    	findViewById(R.id.camera_switch_set).setVisibility(View.GONE);
    	if(mLastPictureButton !=null)
        	mLastPictureButton.setVisibility(View.GONE);
//    	mLastPictureButton.setVisibility(View.GONE);
    	if(mGLRootView != null){
    		mGLRootView.setVisibility(View.GONE);
    	}
    	
    	findViewById(R.id.testmode_pass).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Log.i(TAG,"Pass Clicked!");
	            Context context = getApplicationContext();
	            Intent intent = new Intent(context, VideoCamera.class).putExtra("PhoneTestMode", true);
//	            CameraHolder.instance().keep();
//	            startActivity(intent);
//	            finish();
	            mHandler.removeMessages(FIRST_TIME_INIT);
	            mCameraTestResult = true;
	            startActivityForResult(intent, PHONE_TEST_MODE_RECORDING_START);

				// TODO Auto-generated method stub
				
			}
		});
    	findViewById(R.id.testmode_fail).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				mCameraTestResult = false;
				v.setVisibility(View.GONE);
				findViewById(R.id.testmode_pass).setVisibility(View.GONE);
				findViewById(R.id.showLastImageView).setVisibility(View.GONE);
				setResult(Activity.RESULT_CANCELED);
				finish();
				Log.i(TAG,"Fail Clicked!");
				// TODO Auto-generated method stub
				
			}
		});
	}
 // 20101108 comespain@lge.com [end]
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Log.i(TAG,"onConfigurationChanged, config = "+config.orientation);
        changeHeadUpDisplayState();
    }

    private static ImageManager.DataLocation dataLocation() {
        return ImageManager.DataLocation.EXTERNAL;
    }

    @Override
    protected void onPause() {
    	Log.i(TAG,"park onPause");
        mPausing = true;
        stopPreview();
        // Close the camera now because other activities may need to use it.
        closeCamera();
        resetScreenOn();
        changeHeadUpDisplayState();

        if (mFirstTimeInitialized) {
            mOrientationListener.disable();
            if (!mIsImageCaptureIntent) {
                mThumbController.storeData(
                        ImageManager.getLastImageThumbPath());
            }
            hidePostCaptureAlert();
        }

        if (mDidRegister) {
            unregisterReceiver(mReceiver);
            mDidRegister = false;
        }
        stopReceivingLocationUpdates();

        if (mFocusToneGenerator != null) {
            mFocusToneGenerator.release();
            mFocusToneGenerator = null;
        }

        if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }

        // If we are in an image capture intent and has taken
        // a picture, we just clear it in onPause.
        mImageCapture.clearLastData();
        mImageCapture = null;

        // Remove the messages in the event queue.
        mHandler.removeMessages(RESTART_PREVIEW);
        mHandler.removeMessages(FIRST_TIME_INIT);
        
        if(mAtCmdReceiver != null)
        unregisterReceiver(mAtCmdReceiver);
				
				 //hyungtae.lee@lge.com S: block to go to sleep mode during test mode
        if(mPm == null || mWl == null){
        	Log.i(TAG,"mPm == null || mWl == null");
        	mPm = (PowerManager) getSystemService(POWER_SERVICE);
        	mWl = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Camera");
        }
        mWl.release();
      //hyungtae.lee@lge.com E: block to go to sleep mode during test mode
      
        super.onPause();
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CROP_MSG: {
                Intent intent = new Intent();
                if (data != null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                }
                setResult(resultCode, intent);
                finish();

                File path = getFileStreamPath(sTempCropFilename);
                path.delete();

                break;
            }
         // 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
            case PHONE_TEST_MODE_RECORDING_START:{
            	if(resultCode == Activity.RESULT_OK){
            		/*Log.i(TAG,"PHONE_TEST_MODE_RECORDING_START, resultCode == Activity.RESULT_OK");
            		
            		String CAMERA_IMAGE_BUCKET =
            	        Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera";
            		Log.e(TAG,"CAMERA_IMAGE_BUCKET = "+CAMERA_IMAGE_BUCKET);
            	    String CAMERA_IMAGE_BUCKET_ID = String.valueOf(CAMERA_IMAGE_BUCKET.toLowerCase().hashCode());
            	    Uri target = Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
            		.appendQueryParameter("bucketId",CAMERA_IMAGE_BUCKET_ID).build();
            	    Log.e(TAG,"target = "+target);
//            		viewLastImage();
            	    Intent intent = new Intent(Intent.ACTION_VIEW, target);
//            		Intent intent = new Intent(Intent.ACTION_VIEW, mPhoneTestModeCapturedUri);
//            		Intent intent = new Intent(Intent.ACTION_VIEW, null);
            		
            		intent.putExtra("mediaTypes", (1 << 2));
            		startActivity(intent);
            		mPhoneTestMode = false;
            		mPhoneTestModeCapturedUri = null;
            		*/
            		setResult(Activity.RESULT_OK);            		
            		finish();

            	} else {
            		setResult(Activity.RESULT_CANCELED);
            		finish();
            	}
            }
        	// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
        }
    }

    private boolean canTakePicture() {
        return isCameraIdle() && mPreviewing && (mPicturesRemaining > 0);
    }

    private void autoFocus() {
        // Initiate autofocus only when preview is started and snapshot is not
        // in progress.
        if (canTakePicture()) {
            mHeadUpDisplay.setEnabled(false);
            Log.v(TAG, "Start autofocus.");
            mFocusStartTime = System.currentTimeMillis();
            mFocusState = FOCUSING;
            updateFocusIndicator();
            mCameraDevice.autoFocus(mAutoFocusCallback);
        }
    }

    private void cancelAutoFocus() {
        // User releases half-pressed focus key.
        if (mFocusState == FOCUSING || mFocusState == FOCUS_SUCCESS
                || mFocusState == FOCUS_FAIL) {
            Log.v(TAG, "Cancel autofocus.");
            mHeadUpDisplay.setEnabled(true);
            mCameraDevice.cancelAutoFocus();
        }
        if (mFocusState != FOCUSING_SNAP_ON_FINISH) {
            clearFocusState();
        }
    }

    private void clearFocusState() {
        mFocusState = FOCUS_NOT_STARTED;
        updateFocusIndicator();
    }

    private void updateFocusIndicator() {
    	Log.i(TAG,"updateFocusIndicator()");
        if (mFocusRectangle == null) {
        	Log.i(TAG,"updateFocusIndicator(), mFocusRectangle == null");
        	return;
        }
        if (mFocusState == FOCUSING || mFocusState == FOCUSING_SNAP_ON_FINISH) {
            mFocusRectangle.showStart();
        } else if (mFocusState == FOCUS_SUCCESS) {
        	Log.i(TAG,"mFocusState == FOCUS_SUCCESS");
            mFocusRectangle.showSuccess();
            mTestHandler.sendEmptyMessageDelayed(0, 500);
        } else if (mFocusState == FOCUS_FAIL) {
            mFocusRectangle.showFail();
            mTestHandler.sendEmptyMessageDelayed(1, 500);
        } else {
            mFocusRectangle.clear();
        }
    }

    @Override
    public void onBackPressed() {
        if (!isCameraIdle()) {
            // ignore backs while we're taking a picture
            return;
        } else if (mHeadUpDisplay == null || !mHeadUpDisplay.collapse()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        	return true;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_F:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    doFocus(true);
                }
                return true;
            case KeyEvent.KEYCODE_P:
                Log.e(TAG, "Key Event \"P\" at " + System.currentTimeMillis());

                checkStorage();
                if( ( !mCaptureRunning) && ( canTakePicture() ) && ( !mLongPress ) ) {
                    mCaptureRunning = true;

                    Log.e(TAG, "takePicture() called at " + System.currentTimeMillis());
                    mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback, new JpegPictureCallback(null));
                    mPreviewing = false;
                    mLongPress = true;
                }

                return true;
            case KeyEvent.KEYCODE_R:
                mOrientation += ORIENTATION_STEP;
                mOrientation %= ORIENTATION_LIMIT;

                mOrientationListener.onOrientationChanged(mOrientation);
                return true;
            case KeyEvent.KEYCODE_CAMERA:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    doSnap();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // If we get a dpad center event without any focused view, move
                // the focus to the shutter button and press it.
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    // Start auto-focus immediately to reduce shutter lag. After
                    // the shutter button gets the focus, doFocus() will be
                    // called again but it is fine.
                    if (mHeadUpDisplay.collapse()) return true;
                    doFocus(true);
                    if (mShutterButton.isInTouchMode()) {
                        mShutterButton.requestFocusFromTouch();
                    } else {
                        mShutterButton.requestFocus();
                    }
                    mShutterButton.setPressed(true);
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_P:
                mLongPress = false;
                return true;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_F:
                if (mFirstTimeInitialized) {
                    doFocus(false);
                }
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private static void doSnap() {
    	Log.i(TAG,"doSnap()");
        if (mHeadUpDisplay.collapse()) return;

        Log.v(TAG, "doSnap: mFocusState=" + mFocusState);
        // If the user has half-pressed the shutter and focus is completed, we
        // can take the photo right away. If the focus mode is infinity, we can
        // also take the photo.
        if (mCameraSensor == VALUE_CAMERA_SEC) {
        	Log.i(TAG,"2nd Camera captured without focusing.");
        	mImageCapture.onSnap();
        	return;
		}
        
        if (mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)
                || (mFocusState == FOCUS_SUCCESS
                || mFocusState == FOCUS_FAIL)) {
        	Log.i(TAG,"mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY");
            mImageCapture.onSnap();
        } else if (mFocusState == FOCUSING) {
            // Half pressing the shutter (i.e. the focus button event) will
            // already have requested AF for us, so just request capture on
            // focus here.
        	Log.i(TAG,"mFocusState == FOCUSING");
            mFocusState = FOCUSING_SNAP_ON_FINISH;
        } else if (mFocusState == FOCUS_NOT_STARTED) {
        	Log.i(TAG,"mFocusState == FOCUS_NOT_STARTED");
            // Focus key down event is dropped for some reasons. Just ignore.
        }
    }

    private void doFocus(boolean pressed) {
    	Log.i(TAG,"doFocus, pressed = "+pressed);
        // Do the focus if the mode is not infinity.
        if (mHeadUpDisplay.collapse()) return;
        if (!mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)) {
        	Log.i(TAG,"!mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY");
            if (pressed) {  // Focus key down.
                autoFocus();
            } else {  // Focus key up.
                cancelAutoFocus();
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Make sure we have a surface in the holder before proceeding.
    	Log.i(TAG,"surfaceChanged, w = "+w+", h = "+h);
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        // We need to save the holder for later use, even when the mCameraDevice
        // is null. This could happen if onResume() is invoked after this
        // function.
        mSurfaceHolder = holder;

        // The mCameraDevice will be null if it fails to connect to the camera
        // hardware. In this case we will show a dialog and then finish the
        // activity, so it's OK to ignore it.
        if (mCameraDevice == null) return;

        // Sometimes surfaceChanged is called after onPause or before onResume.
        // Ignore it.
        if (mPausing || isFinishing()) return;

        if (mPreviewing && holder.isCreating()) {
            // Set preview display if the surface is being created and preview
            // was already started. That means preview display was set to null
            // and we need to set it now.
            setPreviewDisplay(holder);
        } else {
            // 1. Restart the preview if the size of surface was changed. The
            // framework may not support changing preview display on the fly.
            // 2. Start the preview now if surface was destroyed and preview
            // stopped.
            restartPreview();
        }

        // If first time initialization is not finished, send a message to do
        // it later. We want to finish surfaceChanged as soon as possible to let
        // user see preview first.
        if (!mFirstTimeInitialized) {
            mHandler.sendEmptyMessage(FIRST_TIME_INIT);
        } else {
            initializeSecondTime();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
    	Log.i(TAG,"park surfaceCreated");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        mSurfaceHolder = null;
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
        	Log.i(TAG,"park CameraHolder.instance().release()");
            CameraHolder.instance().release();
            mCameraDevice.setZoomChangeListener(null);
            mCameraDevice = null;
            mPreviewing = false;
        }
    }

    private void ensureCameraDevice() throws CameraHardwareException {
        if (mCameraDevice == null) {
            mCameraDevice = CameraHolder.instance().open();
            mInitialParams = mCameraDevice.getParameters();
        }
    }

    private void updateLastImage() {
        IImageList list = ImageManager.makeImageList(
            mContentResolver,
            dataLocation(),
            ImageManager.INCLUDE_IMAGES,
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

    private void showCameraErrorAndFinish() {
        Resources ress = getResources();
        Util.showFatalErrorAndFinish(Camera.this,
                ress.getString(R.string.camera_error_title),
                ress.getString(R.string.cannot_connect_camera));
    }

    private void restartPreview() {
        try {
            startPreview();
        } catch (CameraHardwareException e) {
            showCameraErrorAndFinish();
            return;
        }
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
    	Log.i(TAG,"startPreview()");
        if (mPausing || isFinishing()) return;

        ensureCameraDevice();

        // If we're previewing already, stop the preview first (this will blank
        // the screen).
        if (mPreviewing) stopPreview();

        setPreviewDisplay(mSurfaceHolder);
        setCameraParameters(UPDATE_PARAM_ALL);

        final long wallTimeStart = SystemClock.elapsedRealtime();
        final long threadTimeStart = Debug.threadCpuTimeNanos();

        mCameraDevice.setErrorCallback(mErrorCallback);

        try {
            Log.v(TAG, "startPreview");
            mCameraDevice.startPreview();
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }
        mPreviewing = true;
        mZoomState = ZOOM_STOPPED;
        mStatus = IDLE;

        mLowLightTimer = new Timer("Low Light Timer");
        mLowLightThread = new LowLightThread();
        mLowLightTimer.schedule(mLowLightThread, mLowLightPeriodDelay, mLowLightPeriodDelay);

    }

    private void stopPreview() {
    	Log.i(TAG,"stopPreview()");
        mLowLightTimer.cancel();
        mLowLightTimer.purge();

        if (mCameraDevice != null && mPreviewing) {
            Log.v(TAG, "stopPreview");
            mCameraDevice.stopPreview();
        }
        mPreviewing = false;
        // If auto focus was in progress, it would have been canceled.
        clearFocusState();
    }

    private Size getOptimalPreviewSize(List<Size> sizes, double targetRatio) {
        final double ASPECT_TOLERANCE = 0.05;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of mSurfaceView. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size

        Display display = getWindowManager().getDefaultDisplay();
        int targetHeight = Math.min(display.getHeight(), display.getWidth());

        if (targetHeight <= 0) {
            // We don't know the size of SurefaceView, use screen height
            WindowManager windowManager = (WindowManager)
                    getSystemService(Context.WINDOW_SERVICE);
            targetHeight = windowManager.getDefaultDisplay().getHeight();
        }

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            Log.v(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private void updateCameraParametersInitialize() {
        mParameters.set(PARM_CAPTURE, PARM_CAPTURE_STILL);
        mParameters.setPreviewFrameRate(mLastPreviewFramerate);

        CameraSettings.setCameraPreviewSize(mLastPreviewSize,
            mParameters.getSupportedPreviewSizes(), mParameters);

    }

    private void updateCameraParametersZoom() {
        // Set zoom.
        if (mParameters.isZoomSupported()) {
            mParameters.setZoom(mZoomValue);
            Log.i(TAG,"updateCameraParametersZoom, mZoomValue = "+mZoomValue);
        }
    }

    private void updateCameraParametersPreference() {
        boolean restartPreview = false;

        // Set picture size.
        String pictureSize = mPreferences.getString(
                CameraSettings.KEY_PICTURE_SIZE, null);
        if (pictureSize == null) {
            CameraSettings.initialCameraPictureSize(this, mParameters);
        } else {
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize(
                    pictureSize, supported, mParameters);
        }

        Size size = mParameters.getPictureSize();
        PreviewFrameLayout frameLayout =
                    (PreviewFrameLayout) findViewById(R.id.frame_layout);
        frameLayout.setAspectRatio((double) size.width / size.height);

        String previewSize = mPreferences.getString(
                    CameraSettings.KEY_PREVIEW_SIZE,
                    getString(R.string.pref_omap3_camera_previewsize_default));

        if ( !mLastPreviewSize.equals(previewSize) ) {

            CameraSettings.setCameraPreviewSize(previewSize,
                mParameters.getSupportedPreviewSizes(), mParameters);

            mLastPreviewSize = previewSize;
            restartPreview = true;
        }

        int framerate = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_PREVIEW_FRAMERATE,
                    getString(R.string.pref_omap3_camera_previewframerate_default)) );

        if ( mLastPreviewFramerate != framerate ) {
            mParameters.setPreviewFrameRate(framerate);
            mLastPreviewFramerate = framerate;
            restartPreview = true;
        }

        int saturation = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_SATURATION,
                    getString(R.string.pref_omap3_camera_saturation_default)) );
        mParameters.set(PARM_SATURATION, saturation);

        int sharpness = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_SHARPNESS,
                    getString(R.string.pref_omap3_camera_sharpness_default)) );
        mParameters.set(PARM_SHARPNESS, sharpness);

        int brightness = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_BRIGHTNESS,
                    getString(R.string.pref_omap3_camera_brightness_default)) );
        mParameters.set(PARM_BRIGHTNESS, brightness);

        int contrast = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_CONTRAST,
                    getString(R.string.pref_omap3_camera_contrast_default)) );
        mParameters.set(PARM_CONTRAST, contrast);

        String mode = mPreferences.getString(
                    CameraSettings.KEY_MODE,
                    getString(R.string.pref_omap3_camera_mode_default));
        mParameters.set(PARM_MODE, mode);

        int ipp = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_IPP,
                    getString(R.string.pref_omap3_camera_ipp_default)) );
        mParameters.set(PARM_IPP, ipp);

        String shutter = mPreferences.getString(
                    CameraSettings.KEY_SHUTTER,
                    getString(R.string.pref_omap3_camera_shutter_default));
        mParameters.set(PARM_SHUTTER, shutter);

        String thumbnail = mPreferences.getString(
                    CameraSettings.KEY_THUMBNAIL,
                    getString(R.string.pref_omap3_camera_thumbnail_default));

        if( thumbnail.equals(PARM_ENABLED) )
            mThumbnailPreview = true;
        else if ( thumbnail.equals(PARM_DISABLED) )
            mThumbnailPreview = false;

        String meterMode = mPreferences.getString(
                    CameraSettings.KEY_METER_MODE,
                    getString(R.string.pref_omap3_camera_metermode_default));
        mParameters.set(PARM_METER_MODE, meterMode);

        String exposureMode = mPreferences.getString(
                    CameraSettings.KEY_EXPOSURE_MODE,
                    getString(R.string.pref_omap3_camera_exposuremode_default));
        mParameters.set(PARM_EXPOSURE_MODE, exposureMode);

        String iso = mPreferences.getString(
                    CameraSettings.KEY_ISO,
                    getString(R.string.pref_omap3_camera_iso_entry_default));
        mParameters.set(PARM_ISO, iso);

        int caf = Integer.parseInt( mPreferences.getString(
                    CameraSettings.KEY_CAF,
                    getString(R.string.pref_omap3_camera_caf_default)) );
        mParameters.set(PARM_CAF, caf);

        mParameters.setRotation( mLastOrientation % ORIENTATION_LIMIT );

        String raw = mPreferences.getString(
                CameraSettings.KEY_RAW,
                getString(R.string.pref_omap3_camera_raw_default));
        mParameters.set(PARM_RAW, raw);

        String manualfocus = mPreferences.getString(
                CameraSettings.KEY_MANUALFOCUS,
                getString(R.string.pref_omap3_camera_manualfocus_default));
        mParameters.set(PARM_MANUALFOCUS, manualfocus);

        String antibanding = mPreferences.getString(
                CameraSettings.KEY_ANTIBANDING,
                getString(R.string.pref_omap3_camera_antibanding_default));
        mParameters.set(PARM_ANTIBANDING, antibanding);

        mBurstImages = Integer.parseInt(mPreferences.getString(
                CameraSettings.KEY_BURST,
                getString(R.string.pref_omap3_camera_burst_default)));
        mParameters.set(PARM_BURST_CAPTURE, mBurstImages);
        Log.e(TAG, "Burst Images set to " + mBurstImages);

        mCameraDevice.setParameters(mParameters);

        // Since change scene mode may change supported values,
        // Set scene mode first,
        mSceneMode = mPreferences.getString(
                CameraSettings.KEY_SCENE_MODE,
                getString(R.string.pref_omap3_camera_scenemode_default));
        if (isSupported(mSceneMode, mParameters.getSupportedSceneModes())) {
            if (!mParameters.getSceneMode().equals(mSceneMode)) {
                mParameters.setSceneMode(mSceneMode);
                mCameraDevice.setParameters(mParameters);

                // Setting scene mode will change the settings of flash mode,
                // white balance, and focus mode. Here we read back the
                // parameters, so we can know those settings.
                mParameters = mCameraDevice.getParameters();
            }
        } else {
            mSceneMode = mParameters.getSceneMode();
            if (mSceneMode == null) {
                mSceneMode = Parameters.SCENE_MODE_AUTO;
            }
        }

        // Set JPEG quality.
        String jpegQuality = mPreferences.getString(
                CameraSettings.KEY_JPEG_QUALITY,
                getString(R.string.pref_omap3_camera_jpegquality_default));
        mParameters.setJpegQuality(JpegEncodingQualityMappings.getQualityNumber(jpegQuality));

        // For the following settings, we need to check if the settings are
        // still supported by latest driver, if not, ignore the settings.

        // Set color effect parameter.
        String colorEffect = mPreferences.getString(
                CameraSettings.KEY_COLOR_EFFECT,
                getString(R.string.pref_omap3_camera_coloreffect_default));
        if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
            mParameters.setColorEffect(colorEffect);
        }

        // Set exposure compensation
        String exposure = mPreferences.getString(
                CameraSettings.KEY_EXPOSURE,
                getString(R.string.pref_omap3_exposure_default));
        try {
            int value = Integer.parseInt(exposure);
            int max = mParameters.getMaxExposureCompensation();
            int min = mParameters.getMinExposureCompensation();
            if (value >= min && value <= max) {
                mParameters.setExposureCompensation(value);
            } else {
                Log.w(TAG, "invalid exposure range: " + exposure);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "invalid exposure: " + exposure);
        }

        if (mGLRootView != null) updateSceneModeInHud();

        if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            // Set flash mode.
            String flashMode = mPreferences.getString(
                    CameraSettings.KEY_FLASH_MODE,
                    getString(R.string.pref_omap3_camera_flashmode_default));
            List<String> supportedFlash = mParameters.getSupportedFlashModes();
            if (isSupported(flashMode, supportedFlash)) {
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

            // Set focus mode.
            mFocusMode = mPreferences.getString(
                    CameraSettings.KEY_FOCUS_MODE,
                    getString(R.string.pref_omap3_camera_focusmode_default));
            if (isSupported(mFocusMode, mParameters.getSupportedFocusModes())) {
                mParameters.setFocusMode(mFocusMode);
            } else {
                mFocusMode = mParameters.getFocusMode();
                if (mFocusMode == null) {
                    mFocusMode = Parameters.FOCUS_MODE_AUTO;
                }
            }
        } else {
            mFocusMode = mParameters.getFocusMode();
        }

        if ( restartPreview ) {
            mHandler.sendEmptyMessage(RESTART_PREVIEW);
        }
    }

    // We separate the parameters into several subsets, so we can update only
    // the subsets actually need updating. The PREFERENCE set needs extra
    // locking because the preference can be changed from GLThread as well.
    private void setCameraParameters(int updateSet) {
        mParameters = mCameraDevice.getParameters();

        if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
            updateCameraParametersInitialize();
        }

        if ((updateSet & UPDATE_PARAM_ZOOM) != 0) {
            updateCameraParametersZoom();
        }

        if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
            synchronized (mPreferences) {
                updateCameraParametersPreference();
            }
        }

        mCameraDevice.setParameters(mParameters);
    }

    // If the Camera is idle, update the parameters immediately, otherwise
    // accumulate them in mUpdateSet and update later.
    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
        mUpdateSet |= additionalUpdateSet;
        if (mCameraDevice == null) {
            // We will update all the parameters when we open the device, so
            // we don't need to do anything now.
            mUpdateSet = 0;
            return;
        } else if (isCameraIdle()) {
            setCameraParameters(mUpdateSet);
            mUpdateSet = 0;
        } else {
            if (!mHandler.hasMessages(SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
                mHandler.sendEmptyMessageDelayed(
                        SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
            }
        }
    }

    private void gotoGallery() {
        MenuHelper.gotoCameraImageGallery(this);
    }

    private void viewLastImage() {
        if (mThumbController.isUriValid()) {
            Intent intent = new Intent(Util.REVIEW_ACTION, mThumbController.getUri());
            try {
            	// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [start]
            	if(mPhoneTestMode){
            		Log.i(TAG,"viewLastImage(), PhoneTestMode");
            		intent.putExtra("PhoneTestMode", true);
            		startActivityForResult(intent, PHONE_TEST_MODE_RECORDING_SHOW_CAPTURED_IMAGE);
            	} else if(mATCMDMode){
            		intent.putExtra("ATCMD", true);
            		startActivity(intent);
            	} else {
                startActivity(intent);
            	}
            	// 20100727 TELEWORKS comespain@lge.com Phone Test Mode Implementation [end]
            } catch (ActivityNotFoundException ex) {
                Log.e(TAG, "review image fail", ex);
            }
        } else {
            Log.e(TAG, "Can't view last image.");
        }
    }

    private void startReceivingLocationUpdates() {
        if (mLocationManager != null) {
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000,
                        0F,
                        mLocationListeners[1]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "provider does not exist " + ex.getMessage());
            }
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,
                        0F,
                        mLocationListeners[0]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "provider does not exist " + ex.getMessage());
            }
        }
    }

    private void stopReceivingLocationUpdates() {
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private Location getCurrentLocation() {
        // go in best to worst order
        for (int i = 0; i < mLocationListeners.length; i++) {
            Location l = mLocationListeners[i].current();
            if (l != null) return l;
        }
        return null;
    }

    private boolean isCameraIdle() {
        return mStatus == IDLE && mFocusState == FOCUS_NOT_STARTED;
    }

    private boolean isImageCaptureIntent() {
        String action = getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action));
    }

    private void setupCaptureParams() {
        Bundle myExtras = getIntent().getExtras();
        if (myExtras != null) {
            mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            mCropValue = myExtras.getString("crop");
        }
    }

    private void showPostCaptureAlert() {
        if (mIsImageCaptureIntent) {
            findViewById(R.id.shutter_button).setVisibility(View.INVISIBLE);
            int[] pickIds = {R.id.btn_retake, R.id.btn_done};
            for (int id : pickIds) {
                View button = findViewById(id);
                ((View) button.getParent()).setVisibility(View.VISIBLE);
            }
        }
    }

    private void hidePostCaptureAlert() {
        if (mIsImageCaptureIntent) {
            findViewById(R.id.shutter_button).setVisibility(View.VISIBLE);
            int[] pickIds = {R.id.btn_retake, R.id.btn_done};
            for (int id : pickIds) {
                View button = findViewById(id);
                ((View) button.getParent()).setVisibility(View.GONE);
            }
        }
    }

    private int calculatePicturesRemaining() {
        mPicturesRemaining = MenuHelper.calculatePicturesRemaining();
        return mPicturesRemaining;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Only show the menu when camera is idle.
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(isCameraIdle());
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (mIsImageCaptureIntent) {
            // No options menu for attach mode.
            return false;
        } else {
            addBaseMenuItems(menu);
        }
        return true;
    }

    private void addBaseMenuItems(Menu menu) {
        MenuHelper.addSwitchModeMenuItem(menu, true, new Runnable() {
            public void run() {
                switchToVideoMode();
            }
        });
        MenuItem gallery = menu.add(Menu.NONE, Menu.NONE,
                MenuHelper.POSITION_GOTO_GALLERY,
                R.string.camera_gallery_photos_text)
                .setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                gotoGallery();
                return true;
            }
        });
        gallery.setIcon(android.R.drawable.ic_menu_gallery);
        mGalleryItems.add(gallery);
    }

    private boolean switchToVideoMode() {
        if (isFinishing() || !isCameraIdle()) return false;
        MenuHelper.gotoVideoMode(this);
        mHandler.removeMessages(FIRST_TIME_INIT);
        finish();
        return true;
    }

    public boolean onSwitchChanged(Switcher source, boolean onOff) {
        if (onOff == SWITCH_VIDEO) {
            return switchToVideoMode();
        } else {
            return true;
        }
    }

    private void onSharedPreferenceChanged() {
        // ignore the events after "onPause()"
        if (mPausing) return;

        boolean recordLocation;

        synchronized (mPreferences) {
            recordLocation = RecordLocationPreference.get(
                    mPreferences, getContentResolver());
            mQuickCapture = getQuickCaptureSettings();
        }

        if (mRecordLocation != recordLocation) {
            mRecordLocation = recordLocation;
            if (mRecordLocation) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
            }
        }

        setCameraParametersWhenIdle(UPDATE_PARAM_PREFERENCE);
    }

    private boolean getQuickCaptureSettings() {
        String value = mPreferences.getString(
                CameraSettings.KEY_QUICK_CAPTURE,
                getString(R.string.pref_omap3_camera_quickcapture_default));
        return CameraSettings.QUICK_CAPTURE_ON.equals(value);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        keepScreenOnAwhile();
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

    private class MyHeadUpDisplayListener implements HeadUpDisplay.Listener {

        // The callback functions here will be called from the GLThread. So,
        // we need to post these runnables to the main thread
        public void onSharedPreferencesChanged() {
            mHandler.post(new Runnable() {
                public void run() {
                    Camera.this.onSharedPreferenceChanged();
                }
            });
        }

        public void onRestorePreferencesClicked() {
            mHandler.post(new Runnable() {
                public void run() {
                    Camera.this.onRestorePreferencesClicked();
                }
            });
        }

        public void onPopupWindowVisibilityChanged(int visibility) {
        }
    }

    protected void onRestorePreferencesClicked() {
        if (mPausing) return;
        Runnable runnable = new Runnable() {
            public void run() {
                mHeadUpDisplay.restorePreferences(mParameters);
            }
        };
        MenuHelper.confirmAction(this,
                getString(R.string.confirm_restore_title),
                getString(R.string.confirm_restore_message),
                runnable);
    }    
    
 // 20100730 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation [START]
    private void showLastImage(){
    	Log.i(TAG,"showLastImage()");
    	Uri lastImageUri = null;
    	if(mShowLastImage && mThumbController.isUriValid()
    			|| mShowLastImage && mPhoneTestModeCapturedUri != null) {
    		 lastImageUri = mThumbController.getUri();
    	     if(mPhoneTestMode){
    	    	 lastImageUri = mPhoneTestModeCapturedUri;
    	     }
    		 Log.i(TAG,"lastImageUri = "+lastImageUri);
    		 BitmapFactory.Options opts = new BitmapFactory.Options();
    		 opts.inSampleSize = 4;
//    		 opts.outWidth = 640;
//    		 opts.outHeight = 480;
    		 Bitmap bmp = BitmapFactory.decodeFile(ImageManager.mLastCapturedFileFullPath, opts);
//    		 String filePath = lastImageUri.getPath();
    		 if(bmp == null){
    			 Log.e(TAG, "bmp is null !!!!");
    		 }
    		 ImageView view = (ImageView)findViewById(R.id.showLastImageView);
//    		 view.setImageURI(lastImageUri);  
    		 view.setImageBitmap(bmp);
    		 view.setVisibility(View.VISIBLE);  
    		 mShutterButton.setVisibility(View.GONE);

    	 }
    }
    
    private void deleteLastImage(){
    	if(mThumbController.isUriValid() && ImageManager.mLastCapturedFileFullPath != null){

			Log.i(TAG,"mLastCapturedFileFullPath = "+ImageManager.mLastCapturedFileFullPath);	
			
			File file = new File(ImageManager.mLastCapturedFileFullPath);
						
			if(file.delete()){
				File thumb = new File(ImageManager.getLastImageThumbPath());
				if(thumb.delete()){
					Log.i(TAG,"Thumb Delete Success!!");
					
				}
				Log.i(TAG,"File Delete Success!!");
			}
			resetExposureCompensation();
			initializeSecondTime();
			initializeHeadUpDisplay();
//			changeHeadUpDisplayState();
			updateThumbnailButton();
			
			if(mShowLastImage){
				mShowLastImage = false;
				ImageView view = (ImageView)findViewById(R.id.showLastImageView);
				view.setImageURI(null);
	    		view.setVisibility(View.GONE);
			}
    	}
    }

    private void atCommandListener() {
    	Log.i(TAG,"atCommandListener()");
		if (mAtCmdReceiver == null) {
			mAtCmdReceiver = new BroadcastReceiver() {
				private static final String TAG = "Phone Test Camera ATCMDReceiver";
//				public Camera mContext = null;
				public static final String CAMERA_MODE_OFF = "android.intent.action.atcmd.CAMERA_OFF";
				public static final String CAMERA_MODE_ON = "android.intent.action.atcmd.CAMERA_ON";
				public static final String CAMERA_MODE_SHOT = "android.intent.action.atcmd.CAMERA_SHOT";
				public static final String CAMERA_MODE_CALLIMAGE = "android.intent.action.atcmd.CALL_IMAGE";
				public static final String CAMERA_MODE_ERASEIMAGE = "android.intent.action.atcmd.ERASE_IMAGE";
				public static final String CAMERA_MODE_FLASHON = "android.intent.action.atcmd.FLASH_ON";
				public static final String CAMERA_MODE_FLASHOFF = "android.intent.action.atcmd.FLASH_OFF";
				public static final String CAMERA_MODE_SWAP = "android.intent.action.atcmd.SWAP_CAM";
				public static final String CAMERA_MODE_ZOOMIN = "android.intent.action.atcmd.ZOOM_IN";
				public static final String CAMERA_MODE_ZOOMOUT = "android.intent.action.atcmd.ZOOM_OUT";
				
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i("camera", "onReceive()");
					// TODO Auto-generated method stub
					String atcmd = intent.getAction();
//					 mContext = context.getApplicationContext();
					Log.i(TAG,"atcmd = "+atcmd);
					if(mShowLastImage){
						mShowLastImage = false;
						ImageView view = (ImageView)findViewById(R.id.showLastImageView);
						view.setImageURI(null);
			    		view.setVisibility(View.GONE);
					}

					if (atcmd.equals(CAMERA_MODE_OFF)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 0;
						if (mContext != null) {
							finish();
						}

						Log.i(TAG, "mode = " + mOperationMode);
					} else if (atcmd.equals(CAMERA_MODE_ON)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						if (mOperationMode == 1) {
							return;
						}
						mOperationMode = 1;

					} else if (atcmd.equals(CAMERA_MODE_SHOT)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 2;
						if (mCameraSensor == VALUE_CAMERA_MAIN) {
							doFocus(true);
						}
						doSnap();

					} else if (atcmd.equals(CAMERA_MODE_CALLIMAGE)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 4;
						mShowLastImage = true;
//						viewLastImage();
						showLastImage();

					} else if (atcmd.equals(CAMERA_MODE_ERASEIMAGE)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 5;
						deleteLastImage();						

					} else if (atcmd.equals(CAMERA_MODE_FLASHON)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 6;

						mParameters.setFlashMode(mParameters.FLASH_MODE_TORCH);
						mCameraDevice.setParameters(mContext.mParameters);

					} else if (atcmd.equals(CAMERA_MODE_FLASHOFF)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 7;
						mParameters.setFlashMode(mParameters.FLASH_MODE_OFF);
						mCameraDevice.setParameters(mParameters);

					} else if (atcmd.equals(CAMERA_MODE_SWAP)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 10;
						if (mCameraSensor == VALUE_CAMERA_MAIN) {
							mCameraSensor = VALUE_CAMERA_SEC;
						}
						else {
							mCameraSensor = VALUE_CAMERA_MAIN;
						}
						mParameters.set(PARM_SWAP, mCameraSensor);
						mCameraDevice.setParameters(mParameters);
						
						restartPreview();
					} else if (atcmd.equals(CAMERA_MODE_ZOOMIN)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 11;
						//onZoomValueChanged(1);
						
						if (++mZoomStatus >= VALUE_ZOOM_MAX) mZoomStatus = VALUE_ZOOM_MAX;
						mParameters.set(PARM_ZOOM, mZoomStatus);
						mCameraDevice.setParameters(mParameters);
					} else if (atcmd.equals(CAMERA_MODE_ZOOMOUT)) {
						Log.i(TAG,"ATCMD Recive, action = "+atcmd);
						mOperationMode = 12;
						//onZoomValueChanged(0);
						
						if (--mZoomStatus <= VALUE_ZOOM_MIN) mZoomStatus = VALUE_ZOOM_MIN;
						mParameters.set(PARM_ZOOM, mZoomStatus);
						mCameraDevice.setParameters(mParameters);
					} else {
						Log.i(TAG,"Not Supported!!!!!!!!!!!!");
					}

				}
			};
			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction("android.intent.action.atcmd.CAMERA_OFF");			
//			iFilter.addAction("android.intent.action.atcmd.CAMERA_ON");
			iFilter.addAction("android.intent.action.atcmd.CAMERA_SHOT");
			iFilter.addAction("android.intent.action.atcmd.CALL_IMAGE");
			iFilter.addAction("android.intent.action.atcmd.ERASE_IMAGE");
			iFilter.addAction("android.intent.action.atcmd.FLASH_ON");
			iFilter.addAction("android.intent.action.atcmd.FLASH_OFF");
			iFilter.addAction("android.intent.action.atcmd.SWAP_CAM");
			iFilter.addAction("android.intent.action.atcmd.ZOOM_IN");
			iFilter.addAction("android.intent.action.atcmd.ZOOM_OUT");
			
			iFilter.addCategory(Intent.CATEGORY_DEFAULT);
			registerReceiver(mAtCmdReceiver, iFilter);
		}
	}
    
 // 20100820 TELEWORKS comespain@lge.com ATCMD Test Mode Implementation[END]

    class LowLightThread extends TimerTask {
        private static final String PARM_LOWLIGHT = "low-light";
        private boolean running = true;
        String lowLightParm = null;
        private Parameters params;

        public void run() {
            if ( null != mCameraDevice ) {
                params = mCameraDevice.getParameters();

                lowLightParm = params.get(PARM_LOWLIGHT);
                if ( null != lowLightParm ) {
                    int mode = Integer.parseInt(lowLightParm);

                    if ( 1 == mode )
                        mHandler.sendEmptyMessage(SHOW_LOW_LIGHT_INDICATOR);
                    else
                        mHandler.sendEmptyMessage(HIDE_LOW_LIGHT_INDICATOR);
                }
            }
        }
    }

}

class LowLightIndicator extends View {
    private static final String TAG = "LowLightIndicator";

    public LowLightIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundDrawable(getResources().getDrawable(R.drawable.low_light_warning));
    }

    public void activate() {
        setBackgroundDrawable(getResources().getDrawable(R.drawable.low_light_warning));
    }

    public void deactivate() {
        setBackgroundDrawable(null);
    }
	
}

class FocusRectangle extends View {

    @SuppressWarnings("unused")
    private static final String TAG = "FocusRectangle";

    public FocusRectangle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void setDrawable(int resid) {
        setBackgroundDrawable(getResources().getDrawable(resid));
    }

    public void showStart() {
    	Log.i(TAG,"FocusRectangle, showStart()");
        setDrawable(R.drawable.focus_focusing);
    }

    public void showSuccess() {
    	Log.i(TAG,"FocusRectangle, showSuccess()");
        setDrawable(R.drawable.focus_focused);
    }

    public void showFail() {
    	Log.i(TAG,"FocusRectangle, showFail()");
        setDrawable(R.drawable.focus_focus_failed);
    }

    public void clear() {
    	Log.i(TAG,"FocusRectangle, clear()");
        setBackgroundDrawable(null);
    }

    public void setPosition(int width, int height, int x, int y) {
        android.widget.AbsoluteLayout.LayoutParams pos =  new android.widget.AbsoluteLayout.LayoutParams(width, height, x, y);
        setLayoutParams(pos);
    }

}

/*
 * Provide a mapping for Jpeg encoding quality levels
 * from String representation to numeric representation.
 */
class JpegEncodingQualityMappings {
    private static final String TAG = "JpegEncodingQualityMappings";
    private static final int DEFAULT_QUALITY = 85;
    private static HashMap<String, Integer> mHashMap =
            new HashMap<String, Integer>();

    static {
        mHashMap.put("normal",    CameraProfile.QUALITY_LOW);
        mHashMap.put("fine",      CameraProfile.QUALITY_MEDIUM);
        mHashMap.put("superfine", CameraProfile.QUALITY_HIGH);
    }

    // Retrieve and return the Jpeg encoding quality number
    // for the given quality level.
    public static int getQualityNumber(String jpegQuality) {
        Integer quality = mHashMap.get(jpegQuality);
        if (quality == null) {
            Log.w(TAG, "Unknown Jpeg quality: " + jpegQuality);
            return DEFAULT_QUALITY;
        }
        return CameraProfile.getJpegEncodingQualityParameter(quality.intValue());
    }
}
