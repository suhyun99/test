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

import static com.lge.cameratest.ui.GLView.OnTouchListener;
import android.content.Context;
import android.util.Log;

import com.lge.cameratest.IconListPreference;
import com.lge.cameratest.PreferenceGroup;
import com.lge.cameratest.R;
import com.lge.cameratest.Util;
import com.lge.cameratest.ui.GLListView.OnItemSelectedListener;

public class BasicIndicator extends AbstractIndicator {

    private final ResourceTexture mIcon[];
    private final IconListPreference mPreference;
    protected int mIndex;
    private GLListView mPopupContent;
    private PreferenceAdapter mModel;
    private String mOverride;

    public BasicIndicator(Context context,
            PreferenceGroup group, IconListPreference preference) {
        super(context);
        mPreference = preference;
        mIcon = new ResourceTexture[preference.getLargeIconIds().length];
        mIndex = preference.findIndexOfValue(preference.getValue());
    }

    // Set the override and/or reload the value from preferences.
    private void updateContent(String override, boolean reloadValue) {
    	Log.i("park","+BasicIndicator, updateContent");
        if (!reloadValue && Util.equals(mOverride, override)) return;
        IconListPreference pref = mPreference;
        mOverride = override;
        int index = pref.findIndexOfValue(
                override == null ? pref.getValue() : override);
        if (mIndex != index) {
            mIndex = index;
            invalidate();
        }
    }

    @Override
    public void overrideSettings(String key, String settings) {
        IconListPreference pref = mPreference;
        if (!pref.getKey().equals(key)) return;
        updateContent(settings, false);
    }

    @Override
    public void reloadPreferences() {
        if (mModel != null) mModel.reload();
        updateContent(null, true);
    }

    @Override
    public GLView getPopupContent() {
        if (mPopupContent == null) {
            Context context = getGLRootView().getContext();
            mPopupContent = new GLListView(context);
            mPopupContent.setHighLight(new NinePatchTexture(
                    context, R.drawable.optionitem_highlight));
            mPopupContent.setScroller(new NinePatchTexture(
                    context, R.drawable.scrollbar_handle_vertical));
            mModel = new PreferenceAdapter(context, mPreference);
            mPopupContent.setOnItemSelectedListener(new MyListener(mModel));
            mPopupContent.setDataModel(mModel);
        }
        mModel.overrideSettings(mOverride);
        return mPopupContent;
    }

    protected void onPreferenceChanged(int newIndex) {
        if (newIndex == mIndex) return;
        mIndex = newIndex;
        invalidate();
    }

    private class MyListener implements OnItemSelectedListener {

        private final PreferenceAdapter mAdapter;

        public MyListener(PreferenceAdapter adapter) {
            mAdapter = adapter;
        }

        public void onItemSelected(GLView view, int position) {
            mAdapter.onItemSelected(view, position);
            onPreferenceChanged(position - 1);
        }
    }

    @Override
    protected ResourceTexture getIcon() {
        int index = mIndex;
        if (mIcon[index] == null) {
            Context context = getGLRootView().getContext();
            mIcon[index] = new ResourceTexture(
                    context, mPreference.getLargeIconIds()[index]);
        }
        return mIcon[index];
    }
}
