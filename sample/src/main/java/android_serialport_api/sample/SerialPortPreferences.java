/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android_serialport_api.sample;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import android_serialport_api.SerialPortFinder;


public class SerialPortPreferences extends PreferenceActivity {

    private Application mApplication;
    private SerialPortFinder mSerialPortFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (Application) getApplication();
        mSerialPortFinder = mApplication.mSerialPortFinder;

        addPreferencesFromResource(R.xml.serial_port_preferences);

        // Devices
        final ListPreference devices = (ListPreference) findPreference("DEVICE");
        String[] entriesAndValues = mSerialPortFinder.getAllDevicesPath();
        devices.setEntries(entriesAndValues);
        devices.setEntryValues(entriesAndValues);
        devices.setSummary(devices.getValue());
        devices.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        // Baud rates
        final ListPreference baudrates = (ListPreference) findPreference("BAUDRATE");
        baudrates.setSummary(baudrates.getValue());
        baudrates.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        // parity
        final ListPreference parity = (ListPreference) findPreference("PARITY");
        parity.setSummary(parity.getValue());
        parity.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        // data bits
        final ListPreference databits = (ListPreference) findPreference("DATABITS");
        databits.setSummary(databits.getValue());
        databits.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        // stop bits
        final ListPreference stopbits = (ListPreference) findPreference("STOPBITS");
        stopbits.setSummary(stopbits.getValue());
        stopbits.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        //
    }
}
