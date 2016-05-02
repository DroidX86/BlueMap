package com.mcproject.rounak.mcapp;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

/**
 * Non-default file template :)
 */
public class SettingsActivity extends PreferenceActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* The activity loads a single fragment of type PrefFragment */
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment sett = new PrefFragment();
        ft.replace(android.R.id.content, sett);
        ft.commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        /* Exit on back pressed */
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode,ev);
    }
}
