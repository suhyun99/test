package com.lge.cameratest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ATCMDReceiver extends BroadcastReceiver {
	private static final String TAG = "Phone Test Camera ATCMDReceiver";
	public static int mMode = 0;
	public static final int CAMERA_MODE_OFF = 1;
	public static final int CAMERA_MODE_ON = 2;
	
	public static final int CAMCORDER_MODE_OFF = 3;
	public static final int CAMCORDER_MODE_ON = 4;
	
	public static int mCurrentMode = 0;
//	public static final String CAMERA_MODE_OFF = "android.intent.action.atcmd.CAMERAOFF";
//	public static final String CAMERA_MODE_ON = "android.intent.action.atcmd.CAMERAON";
	public static final String CAMERA_MODE_SHOT = "android.intent.action.atcmd.CAMERASHOT";
	public static final String CAMERA_MODE_CALLIMAGE = "android.intent.action.atcmd.CALLIMAGE";
	public static final String CAMERA_MODE_ERASEIMAGE = "android.intent.action.atcmd.ERASEIMAGE";
	public static final String CAMERA_MODE_FLASHON= "android.intent.action.atcmd.FLASHON";
	public static final String CAMERA_MODE_FLASHOFF= "android.intent.action.atcmd.FLASHOFF";
	public static final String CAMERA_MODE_SWAP= "android.intent.action.atcmd.SWAP_CAM";
	public static final String CAMERA_MODE_ZOOMIN = "android.intent.action.atcmd.ZOOM_IN";
	public static final String CAMERA_MODE_ZOOMOUT = "android.intent.action.atcmd.ZOOM_OUT";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG,"onReceive");
		String atcmd = intent.getAction();
		
		/*if(atcmd == CAMERA_MODE_OFF){
			mMode = 0;
			mCurrentMode = mMode;
			Log.i(TAG,"mode = "+mMode);
		} else if(atcmd == CAMERA_MODE_ON){
			if(mMode == 1){
				return;
			}
			mMode = 1;
			mCurrentMode = mMode;
			
			Intent cameraStart = new Intent().setClassName(context, "com.lge.cameratest.Camera");
			cameraStart.putExtra("ATCMD", true);
			cameraStart.putExtra("MODE", mMode);
			context.startActivity(cameraStart);		
			
		} else if(atcmd == CAMERA_MODE_SHOT){
			mMode = 2;
			mCurrentMode = mMode;
			
		}else if(atcmd == CAMERA_MODE_CALLIMAGE){
			mMode = 4;
			mCurrentMode = mMode;
			
		}else if(atcmd == CAMERA_MODE_ERASEIMAGE){
			mMode = 5;
			mCurrentMode = mMode;
			
		}else if(atcmd == CAMERA_MODE_FLASHON){
			mMode = 6;
			mCurrentMode = mMode;
			
		}else if(atcmd == CAMERA_MODE_FLASHOFF){
			mMode = 7;
			mCurrentMode = mMode;
			
		}*/
		
		
		
		int mode = intent.getIntExtra("SET", 0);
		switch(mode){
		case 0:
			mMode = 0;
			mCurrentMode = CAMERA_MODE_OFF;
			Log.i(TAG,"mode = "+mode);
			break;
		case 1:
			mMode = 1;
			mCurrentMode = CAMCORDER_MODE_ON;
			Intent cameraStart = new Intent().setAction("CAMERA_ATCMD");
			cameraStart.putExtra("ATCMD", true);
			cameraStart.putExtra("MODE", mode);
			context.startActivity(cameraStart);			
			Log.i(TAG,"mode = "+mode);
			break;

		case 3:
			mMode = 3;
			Log.i(TAG,"mode = "+mode);
			break;
		case 4:
			mMode = 4;
			Log.i(TAG,"mode = "+mode);
			break;
		case 5:
			mMode = 5;
			Log.i(TAG,"mode = "+mode);
			break;
		case 6:
			mMode = 6;
			Log.i(TAG,"mode = "+mode);
			break;
		case 7:
			mMode = 7;
			Log.i(TAG,"mode = "+mode);
			break;
		case 8:
			mMode = 8;
			Log.i(TAG,"mode = "+mode);
			break;
		case 9:
			mMode = 9;
			Log.i(TAG,"mode = "+mode);
			break;
		case 10:
			mMode = 10;
			Log.i(TAG,"mode = "+mode);
			break;
		case 11:
			mMode = 11;
			Log.i(TAG,"mode = "+mode);
			break;
		case 12:
			mMode = 12;
			Log.i(TAG,"mode = "+mode);
			break;
		case 2:
		default:
			Log.e(TAG,"Unsupported Mode!");
			break;			
		}	
		
	}

}
