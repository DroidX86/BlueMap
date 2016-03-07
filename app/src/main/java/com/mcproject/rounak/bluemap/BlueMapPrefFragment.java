package com.mcproject.rounak.bluemap;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by rounak on 7/3/16.
 */
public class BlueMapPrefFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_layout);
    }
}
