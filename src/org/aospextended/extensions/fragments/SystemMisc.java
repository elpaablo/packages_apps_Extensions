/*
 * Copyright (C) 2017 AICP
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

package org.aospextended.extensions.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.SystemProperties;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.Secure;

import java.net.InetAddress;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import org.aospextended.extensions.utils.Util;

public class SystemMisc extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String PREF_SYSTEM_APP_REMOVER = "system_app_remover";
    private static final String PREF_ADBLOCK = "persist.aicp.hosts_block";
    private static final String KERNEL_PROFILE_KEY = "sys.kernel.profile";
    private static final int PROFILE_DEFAULT = 2; //balanced
    private static final int PROFILE_NONE = 0; //disabled
    private static final String PREF_PROFILE = "kernel_profile";
    
    private SwitchPreference mKernelProfilesPreference;
    private Handler mHandler = new Handler();
    private SharedPreferences pref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_misc);

        Preference systemAppRemover = findPreference(PREF_SYSTEM_APP_REMOVER);
        Util.requireRoot(getActivity(), systemAppRemover);

        findPreference(PREF_ADBLOCK).setOnPreferenceChangeListener(this);
        
        int profile = SystemProperties.getInt(KERNEL_PROFILE_KEY, PROFILE_DEFAULT);
        mKernelProfilesPreference = (SwitchPreference) findPreference(KERNEL_PROFILE_KEY);
        mKernelProfilesPreference.setChecked(profile > 0);
        mKernelProfilesPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EXTENSIONS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        
        if (preference == mKernelProfilesPreference) {
            pref = getActivity().getPreferences(Context.MODE_PRIVATE);
            boolean value = (Boolean) objValue;
            mKernelProfilesPreference.setChecked(value);
            
            if (value){ //enable kernel profiles and restore saved profile
                 SystemProperties.set(KERNEL_PROFILE_KEY, "" + 
                         pref.getInt(PREF_PROFILE, PROFILE_DEFAULT));
            }
            else{ //disable kernel profiles and save current profile
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(PREF_PROFILE, SystemProperties.getInt(
                        KERNEL_PROFILE_KEY, PROFILE_DEFAULT));
                editor.apply();
                SystemProperties.set(KERNEL_PROFILE_KEY, "" + PROFILE_NONE); 
            }
            return true;
        } 
        else if (PREF_ADBLOCK.equals(preference.getKey())) {
            // Flush the java VM DNS cache to re-read the hosts file.
            // Delay to ensure the value is persisted before we refresh
            mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InetAddress.clearDnsCache();
                    }
            }, 1000);
            return true;
        } 
        return false;
    }
}
