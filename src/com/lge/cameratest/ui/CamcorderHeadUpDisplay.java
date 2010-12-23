/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.lge.cameratest.ui;

import android.content.Context;

import com.lge.cameratest.CameraSettings;
import com.lge.cameratest.ListPreference;
import com.lge.cameratest.PreferenceGroup;

public class CamcorderHeadUpDisplay extends HeadUpDisplay {

    protected static final String TAG = "CamcorderHeadUpDisplay";

    private OtherSettingsIndicator mOtherSettings;

    public CamcorderHeadUpDisplay(Context context) {
        super(context);
    }

    @Override
    protected void initializeIndicatorBar(
            Context context, PreferenceGroup group) {
        super.initializeIndicatorBar(context, group);

        ListPreference[] prefs = getListPreferences(group,
                CameraSettings.KEY_EXPOSURE,
                CameraSettings.KEY_SCENE_MODE,
                CameraSettings.KEY_COLOR_EFFECT,
                CameraSettings.KEY_VIDEO_FRAMERATE,
                CameraSettings.KEY_VIDEO_BITRATE,
                CameraSettings.KEY_VIDEO_ENCODER,
                CameraSettings.KEY_CAF,
                CameraSettings.KEY_BRIGHTNESS,
                CameraSettings.KEY_CONTRAST,
                CameraSettings.KEY_SATURATION
                );

        mOtherSettings = new OtherSettingsIndicator(context, prefs);
        mOtherSettings.setOnRestorePreferencesClickedRunner(new Runnable() {
            public void run() {
                if (mListener != null) {
                    mListener.onRestorePreferencesClicked();
                }
            }
        });
        mIndicatorBar.addComponent(mOtherSettings);

        addIndicator(context, group, CameraSettings.KEY_WHITE_BALANCE);
        addIndicator(context, group, CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE);
//        addIndicator(context, group, CameraSettings.KEY_VIDEO_QUALITY);
        addIndicator(context, group, CameraSettings.KEY_VIDEO_DURATION);
        addIndicator(context, group, CameraSettings.KEY_VIDEO_FORMAT);
        addIndicator(context, group, CameraSettings.KEY_AUDIO_ENCODER);
        addIndicator(context, group, CameraSettings.KEY_OUTPUT_FORMAT);
    }
}
