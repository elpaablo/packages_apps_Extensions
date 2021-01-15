/*
 * Copyright (C) 2017 AospExtended ROM Project
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

package org.aospextended.extensions;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.internal.util.omni.OmniSwitchConstants;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.aospextended.AEXUtils;
import com.android.internal.util.aospextended.NavUtils;
import com.android.internal.util.hwkeys.ActionUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.lang.Integer;

import org.aospextended.extensions.preference.SystemSettingSwitchPreference;


public class NavbarSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String KEY_NAVIGATION_BAR_ENABLED = "force_show_navbar";
    private static final String KEY_LAYOUT_SETTINGS = "layout_settings";
    private static final String KEY_NAVIGATION_BAR_ARROWS = "navigation_bar_menu_arrow_keys";
    private static final String NAVIGATION_BAR_RECENTS_STYLE = "navbar_recents_style";
    private static final String KEY_KILLAPP_LONGPRESS_BACK = "kill_app_longpress_back";
    
    private static final String KEY_BACK_LONG_PRESS_ACTION = "back_key_long_press";
    private static final String KEY_BACK_DOUBLE_TAP_ACTION = "back_key_double_tap";
    private static final String KEY_HOME_LONG_PRESS_ACTION = "home_key_long_press";
    private static final String KEY_HOME_DOUBLE_TAP_ACTION = "home_key_double_tap";
    private static final String KEY_APP_SWITCH_LONG_PRESS = "app_switch_key_long_press";
    private static final String KEY_APP_SWITCH_DOUBLE_TAP = "app_switch_key_double_tap";
    private static final String KEY_MENU_LONG_PRESS_ACTION = "menu_key_long_press";
    private static final String KEY_MENU_DOUBLE_TAP_ACTION = "menu_key_double_tap";
    private static final String KEY_ASSIST_LONG_PRESS_ACTION = "assist_key_long_press";
    private static final String KEY_ASSIST_DOUBLE_TAP_ACTION = "assist_key_double_tap";

    private static final String KEY_BACK_LONG_PRESS_CUSTOM_APP = "back_key_long_press_custom_app";
    private static final String KEY_BACK_DOUBLE_TAP_CUSTOM_APP = "back_key_double_tap_custom_app";
    private static final String KEY_HOME_LONG_PRESS_CUSTOM_APP = "home_key_long_press_custom_app";
    private static final String KEY_HOME_DOUBLE_TAP_CUSTOM_APP = "home_key_double_tap_custom_app";
    private static final String KEY_APP_SWITCH_LONG_PRESS_CUSTOM_APP = "app_switch_key_long_press_custom_app";
    private static final String KEY_APP_SWITCH_DOUBLE_TAP_CUSTOM_APP = "app_switch_key_double_tap_custom_app";

    private static final String KEY_CATEGORY_HOME          = "home_key";
    private static final String KEY_CATEGORY_BACK          = "back_key";
    private static final String KEY_CATEGORY_MENU          = "menu_key";
    private static final String KEY_CATEGORY_ASSIST        = "assist_key";
    private static final String KEY_CATEGORY_APP_SWITCH    = "app_switch_key";
    
    private static final String CUSTOM_APP_ACTION_INDEX = "17";

    private ListPreference mBackLongPress;
    private ListPreference mBackDoubleTap;
    private ListPreference mHomeLongPress;
    private ListPreference mHomeDoubleTap;
    private ListPreference mAppSwitchLongPress;
    private ListPreference mAppSwitchDoubleTap;
    private ListPreference mMenuLongPress;
    private ListPreference mMenuDoubleTap;
    private ListPreference mAssistLongPress;
    private ListPreference mAssistDoubleTap;

    private Preference mAppSwitchLongPressCustomApp;
    private Preference mAppSwitchDoubleTapCustomApp;
    private Preference mBackLongPressCustomApp;
    private Preference mBackDoubleTapCustomApp;
    private Preference mGestureSystemNavigation;
    private Preference mHomeLongPressCustomApp;
    private Preference mHomeDoubleTapCustomApp;

    private PreferenceCategory mHomeCategory;
    private PreferenceCategory mBackCategory;
    private PreferenceCategory mMenuCategory;
    private PreferenceCategory mAssistCategory;
    private PreferenceCategory mAppSwitchCategory;
    private PreferenceCategory mLayoutCategory;

    private int deviceKeys;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    
    private Preference mLayoutSettings;
    private SwitchPreference mNavigationBar;
    private SystemSettingSwitchPreference mNavigationArrows;
    private ListPreference mNavbarRecentsStyle;

    private boolean mIsNavSwitchingMode = false;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.navbar_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefSet = getPreferenceScreen();

        final boolean needsNavbar = ActionUtils.hasNavbarByDefault(getActivity());
        final boolean hwkeysSupported = ActionUtils.isHWKeysSupported(getActivity());

        final boolean defaultToNavigationBar = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        final boolean navigationBarEnabled = Settings.System.getIntForUser(
                resolver, Settings.System.FORCE_SHOW_NAVBAR,
                defaultToNavigationBar ? 1 : 0, UserHandle.USER_CURRENT) != 0;
                
        // bits for hardware keys present on device
        deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        final boolean navigationMode3Button = !NavUtils.isGestureAvailable(getContext()) ||
                      (!NavUtils.isSwipeUpEnabled(getContext()) &&
                       !NavUtils.isEdgeToEdgeEnabled(getContext()));
        boolean hasHome = (deviceKeys & KEY_MASK_HOME) != 0 || (navigationBarEnabled && navigationMode3Button);
        boolean hasMenu = (deviceKeys & KEY_MASK_MENU) != 0;
        boolean hasBack = (deviceKeys & KEY_MASK_BACK) != 0 || (navigationBarEnabled && navigationMode3Button);
        boolean hasAssist = (deviceKeys & KEY_MASK_ASSIST) != 0;
        boolean hasAppSwitch = (deviceKeys & KEY_MASK_APP_SWITCH) != 0 || (navigationBarEnabled && navigationMode3Button);        
                

        mNavigationBar = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR_ENABLED);
        mNavigationBar.setChecked(navigationBarEnabled);
        mNavigationBar.setOnPreferenceChangeListener(this);

        mLayoutSettings = (Preference) findPreference(KEY_LAYOUT_SETTINGS);

        mNavigationArrows = (SystemSettingSwitchPreference) findPreference(KEY_NAVIGATION_BAR_ARROWS);
        
     
        SwitchPreference longPressBackToKill = (SwitchPreference) findPreference(KEY_KILLAPP_LONGPRESS_BACK);

        if (needsNavbar || !hwkeysSupported) {
           if (longPressBackToKill != null) longPressBackToKill.getParent().removePreference(longPressBackToKill);
        }

        mHomeCategory =
                (PreferenceCategory) prefSet.findPreference(KEY_CATEGORY_HOME);
        mBackCategory =
                (PreferenceCategory) prefSet.findPreference(KEY_CATEGORY_BACK);
        mMenuCategory =
                (PreferenceCategory) prefSet.findPreference(KEY_CATEGORY_MENU);
        mAssistCategory =
                (PreferenceCategory) prefSet.findPreference(KEY_CATEGORY_ASSIST);
        mAppSwitchCategory =
                (PreferenceCategory) prefSet.findPreference(KEY_CATEGORY_APP_SWITCH);

        mBackLongPressCustomApp = (Preference) findPreference(KEY_BACK_LONG_PRESS_CUSTOM_APP);
        mBackDoubleTapCustomApp = (Preference) findPreference(KEY_BACK_DOUBLE_TAP_CUSTOM_APP);
        mHomeLongPressCustomApp = (Preference) findPreference(KEY_HOME_LONG_PRESS_CUSTOM_APP);
        mHomeDoubleTapCustomApp = (Preference) findPreference(KEY_HOME_DOUBLE_TAP_CUSTOM_APP);
        mAppSwitchLongPressCustomApp = (Preference) findPreference(KEY_APP_SWITCH_LONG_PRESS_CUSTOM_APP);
        mAppSwitchDoubleTapCustomApp = (Preference) findPreference(KEY_APP_SWITCH_DOUBLE_TAP_CUSTOM_APP);

        mBackLongPress = (ListPreference) findPreference(KEY_BACK_LONG_PRESS_ACTION);
        int backlongpress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_BACK_LONG_PRESS_ACTION, 0, UserHandle.USER_CURRENT);
        mBackLongPress.setValue(String.valueOf(backlongpress));
        mBackLongPress.setSummary(mBackLongPress.getEntry());
        mBackLongPress.setOnPreferenceChangeListener(this);

        mBackDoubleTap = (ListPreference) findPreference(KEY_BACK_DOUBLE_TAP_ACTION);
        int backdoubletap = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_BACK_DOUBLE_TAP_ACTION, 0, UserHandle.USER_CURRENT);
        mBackDoubleTap.setValue(String.valueOf(backdoubletap));
        mBackDoubleTap.setSummary(mBackDoubleTap.getEntry());
        mBackDoubleTap.setOnPreferenceChangeListener(this);

        mHomeLongPress = (ListPreference) findPreference(KEY_HOME_LONG_PRESS_ACTION);
        int homelongpress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_HOME_LONG_PRESS_ACTION, 0, UserHandle.USER_CURRENT);
        mHomeLongPress.setValue(String.valueOf(homelongpress));
        mHomeLongPress.setSummary(mHomeLongPress.getEntry());
        mHomeLongPress.setOnPreferenceChangeListener(this);

        mHomeDoubleTap = (ListPreference) findPreference(KEY_HOME_DOUBLE_TAP_ACTION);
        int homedoubletap = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_HOME_DOUBLE_TAP_ACTION, 0, UserHandle.USER_CURRENT);
        mHomeDoubleTap.setValue(String.valueOf(homedoubletap));
        mHomeDoubleTap.setSummary(mHomeDoubleTap.getEntry());
        mHomeDoubleTap.setOnPreferenceChangeListener(this);

        mAppSwitchLongPress = (ListPreference) findPreference(KEY_APP_SWITCH_LONG_PRESS);
        int appswitchlongpress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, 0, UserHandle.USER_CURRENT);
        mAppSwitchLongPress.setValue(String.valueOf(appswitchlongpress));
        mAppSwitchLongPress.setSummary(mAppSwitchLongPress.getEntry());
        mAppSwitchLongPress.setOnPreferenceChangeListener(this);

        mAppSwitchDoubleTap = (ListPreference) findPreference(KEY_APP_SWITCH_DOUBLE_TAP);
        int appswitchdoubletap = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION, 0, UserHandle.USER_CURRENT);
        mAppSwitchDoubleTap.setValue(String.valueOf(appswitchdoubletap));
        mAppSwitchDoubleTap.setSummary(mAppSwitchDoubleTap.getEntry());
        mAppSwitchDoubleTap.setOnPreferenceChangeListener(this);

        mMenuLongPress = (ListPreference) findPreference(KEY_MENU_LONG_PRESS_ACTION);
        int menulongpress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_MENU_LONG_PRESS_ACTION, 0, UserHandle.USER_CURRENT);
        mMenuLongPress.setValue(String.valueOf(menulongpress));
        mMenuLongPress.setSummary(mMenuLongPress.getEntry());
        mMenuLongPress.setOnPreferenceChangeListener(this);

        mMenuDoubleTap = (ListPreference) findPreference(KEY_MENU_DOUBLE_TAP_ACTION);
        int menudoubletap = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_MENU_DOUBLE_TAP_ACTION, 0, UserHandle.USER_CURRENT);
        mMenuDoubleTap.setValue(String.valueOf(menudoubletap));
        mMenuDoubleTap.setSummary(mMenuDoubleTap.getEntry());
        mMenuDoubleTap.setOnPreferenceChangeListener(this);

        mAssistLongPress = (ListPreference) findPreference(KEY_ASSIST_LONG_PRESS_ACTION);
        int assistlongpress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, 0, UserHandle.USER_CURRENT);
        mAssistLongPress.setValue(String.valueOf(assistlongpress));
        mAssistLongPress.setSummary(mAssistLongPress.getEntry());
        mAssistLongPress.setOnPreferenceChangeListener(this);

        mAssistDoubleTap = (ListPreference) findPreference(KEY_ASSIST_DOUBLE_TAP_ACTION);
        int assistdoubletap = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION, 0, UserHandle.USER_CURRENT);
        mAssistDoubleTap.setValue(String.valueOf(assistdoubletap));
        mAssistDoubleTap.setSummary(mAssistDoubleTap.getEntry());
        mAssistDoubleTap.setOnPreferenceChangeListener(this);

        if (!hasMenu && mMenuCategory != null) {
            prefSet.removePreference(mMenuCategory);
        }

        if (!hasAssist && mAssistCategory != null) {
            prefSet.removePreference(mAssistCategory);
        }

        customAppCheck();
        
        boolean visible = mBackLongPress.getEntryValues()[backlongpress].equals(CUSTOM_APP_ACTION_INDEX);
        mBackLongPressCustomApp.setVisible(visible);
        if (visible) {
        
        }
        
        visible = mBackDoubleTap.getEntryValues()[backdoubletap].equals(CUSTOM_APP_ACTION_INDEX);
        mBackDoubleTapCustomApp.setVisible(visible);
        
        visible = mHomeLongPress.getEntryValues()[homelongpress].equals(CUSTOM_APP_ACTION_INDEX);
        mHomeLongPressCustomApp.setVisible(visible);
        
        visible = mHomeDoubleTap.getEntryValues()[homedoubletap].equals(CUSTOM_APP_ACTION_INDEX);
        mHomeDoubleTapCustomApp.setVisible(visible);
        
        visible = mAppSwitchLongPress.getEntryValues()[appswitchlongpress].equals(CUSTOM_APP_ACTION_INDEX);
        mAppSwitchLongPressCustomApp.setVisible(visible);
        
        mAppSwitchDoubleTap.getEntryValues()[appswitchdoubletap].equals(CUSTOM_APP_ACTION_INDEX);
        mAppSwitchDoubleTapCustomApp.setVisible(visible);


        /* OMNI Switch  */
        mHandler = new Handler();
        mNavbarRecentsStyle = (ListPreference) findPreference(NAVIGATION_BAR_RECENTS_STYLE);
        int recentsStyle = Settings.System.getInt(resolver,
                Settings.System.OMNI_NAVIGATION_BAR_RECENTS, 0);

        mNavbarRecentsStyle.setValue(Integer.toString(recentsStyle));
        mNavbarRecentsStyle.setSummary(mNavbarRecentsStyle.getEntry());
        mNavbarRecentsStyle.setOnPreferenceChangeListener(this);

    }
    
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EXTENSIONS;
    }

    @Override
    public void onResume() {
        super.onResume();
        customAppCheck();
    }

    @Override
    public void onPause() {
        super.onPause();
        customAppCheck();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNavigationBar) {
            boolean value = (Boolean) objValue;
            if (mIsNavSwitchingMode) {
                return false;
            }
            mIsNavSwitchingMode = true;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FORCE_SHOW_NAVBAR, value ? 1 : 0);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsNavSwitchingMode = false;
                }
            }, 1500);
            return true;
        } else if (preference == mNavbarRecentsStyle) {
            int value = Integer.valueOf((String) objValue);
            if (value == 1) {
                if (!isOmniSwitchInstalled()){
                    doOmniSwitchUnavail();
                } else if (!OmniSwitchConstants.isOmniSwitchRunning(getActivity())) {
                    doOmniSwitchConfig();
                }
            }
            int index = mNavbarRecentsStyle.findIndexOfValue((String) objValue);
            mNavbarRecentsStyle.setSummary(mNavbarRecentsStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.OMNI_NAVIGATION_BAR_RECENTS, value);
            return true;
        } else if (preference == mBackLongPress) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mBackLongPress.findIndexOfValue((String) objValue);
            mBackLongPress.setSummary(
                    mBackLongPress.getEntries()[index]);
            customAppCheck();
            mBackLongPressCustomApp.setVisible(mBackLongPress.getEntryValues()
                    [index].equals(CUSTOM_APP_ACTION_INDEX));
            return true;
        } else if (preference == mBackDoubleTap) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_BACK_DOUBLE_TAP_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mBackDoubleTap.findIndexOfValue((String) objValue);
            mBackDoubleTap.setSummary(
                    mBackDoubleTap.getEntries()[index]);
            mBackDoubleTapCustomApp.setVisible(mBackDoubleTap.getEntryValues()
                    [index].equals(CUSTOM_APP_ACTION_INDEX));
            return true;
        } else if (preference == mHomeLongPress) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mHomeLongPress.findIndexOfValue((String) objValue);
            mHomeLongPress.setSummary(
                    mHomeLongPress.getEntries()[index]);
            mHomeLongPressCustomApp.setVisible(mHomeLongPress.getEntryValues()
                    [index].equals(CUSTOM_APP_ACTION_INDEX));
            return true;
        } else if (preference == mHomeDoubleTap) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mHomeDoubleTap.findIndexOfValue((String) objValue);
            mHomeDoubleTap.setSummary(
                    mHomeDoubleTap.getEntries()[index]);
            mHomeDoubleTapCustomApp.setVisible(mHomeDoubleTap.getEntryValues()
                    [index].equals(CUSTOM_APP_ACTION_INDEX));
            return true;
        } else if (preference == mAppSwitchLongPress) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mAppSwitchLongPress.findIndexOfValue((String) objValue);
            mAppSwitchLongPress.setSummary(
                    mAppSwitchLongPress.getEntries()[index]);
            mAppSwitchLongPressCustomApp.setVisible(mAppSwitchLongPress.getEntryValues()
                    [index].equals(CUSTOM_APP_ACTION_INDEX));
            return true;
        } else if (preference == mAppSwitchDoubleTap) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mAppSwitchDoubleTap.findIndexOfValue((String) objValue);
            mAppSwitchDoubleTap.setSummary(
                    mAppSwitchDoubleTap.getEntries()[index]);
            mAppSwitchDoubleTapCustomApp.setVisible(mAppSwitchDoubleTap.getEntryValues()
                    [index].equals(CUSTOM_APP_ACTION_INDEX));
            return true;
        } else if (preference == mMenuLongPress) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mMenuLongPress.findIndexOfValue((String) objValue);
            mMenuLongPress.setSummary(
                    mMenuLongPress.getEntries()[index]);
            return true;
        } else if (preference == mMenuDoubleTap) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_MENU_DOUBLE_TAP_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mMenuDoubleTap.findIndexOfValue((String) objValue);
            mMenuDoubleTap.setSummary(
                    mMenuDoubleTap.getEntries()[index]);
            return true;
        } else if (preference == mAssistLongPress) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mAssistLongPress.findIndexOfValue((String) objValue);
            mAssistLongPress.setSummary(
                    mAssistLongPress.getEntries()[index]);
            return true;
        } else if (preference == mAssistDoubleTap) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION, value,
                    UserHandle.USER_CURRENT);
            int index = mAssistDoubleTap.findIndexOfValue((String) objValue);
            mAssistDoubleTap.setSummary(
                    mAssistDoubleTap.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void customAppCheck() {
        mBackLongPressCustomApp.setSummary(Settings.System.getStringForUser(getActivity().getContentResolver(),
                String.valueOf(Settings.System.KEY_BACK_LONG_PRESS_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        mBackDoubleTapCustomApp.setSummary(Settings.System.getStringForUser(getActivity().getContentResolver(),
                String.valueOf(Settings.System.KEY_BACK_DOUBLE_TAP_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        mHomeLongPressCustomApp.setSummary(Settings.System.getStringForUser(getActivity().getContentResolver(),
                String.valueOf(Settings.System.KEY_HOME_LONG_PRESS_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        mHomeDoubleTapCustomApp.setSummary(Settings.System.getStringForUser(getActivity().getContentResolver(),
                String.valueOf(Settings.System.KEY_HOME_DOUBLE_TAP_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        mAppSwitchLongPressCustomApp.setSummary(Settings.System.getStringForUser(getActivity().getContentResolver(),
                String.valueOf(Settings.System.KEY_APP_SWITCH_LONG_PRESS_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        mAppSwitchDoubleTapCustomApp.setSummary(Settings.System.getStringForUser(getActivity().getContentResolver(),
                String.valueOf(Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
    }
    
    private void checkForOmniSwitchRecents() {
        if (!isOmniSwitchInstalled()){
            doOmniSwitchUnavail();
        } else if (!OmniSwitchConstants.isOmniSwitchRunning(getActivity())) {
            doOmniSwitchConfig();
        }
    }

    private void doOmniSwitchConfig() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.omniswitch_title);
        alertDialogBuilder.setMessage(R.string.omniswitch_dialog_running_new)
            .setPositiveButton(R.string.omniswitch_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    startActivity(OmniSwitchConstants.INTENT_LAUNCH_APP);
                }
            });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void doOmniSwitchUnavail() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.omniswitch_title);
        alertDialogBuilder.setMessage(R.string.omniswitch_dialog_unavail);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private boolean isOmniSwitchInstalled() {
        return AEXUtils.isPackageInstalled(getActivity(), OmniSwitchConstants.APP_PACKAGE_NAME);
    }


}
