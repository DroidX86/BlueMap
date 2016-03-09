package com.mcproject.rounak.mcapp;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Non-default file template :)
 */
public class PrefFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* layout is in the xml */
        addPreferencesFromResource(R.xml.pref_layout);
    }
}
