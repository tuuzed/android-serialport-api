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

package com.tuuzed.androidx.serialport.sample;

import android.os.Bundle;
import android.widget.EditText;

import java.io.IOException;


public class ConsoleActivity extends SerialPortActivity {

	EditText mReception;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.console);

//		setTitle("Loopback test");
		mReception = findViewById(R.id.EditTextReception);

		EditText Emission = findViewById(R.id.EditTextEmission);
		Emission.setOnEditorActionListener((v, actionId, event) -> {
            int i;
            CharSequence t = v.getText();
            char[] text = new char[t.length()];
            for (i=0; i<t.length(); i++) {
                text[i] = t.charAt(i);
            }
            try {
                mOutputStream.write(new String(text).getBytes());
                mOutputStream.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        });
	}

	@Override
	protected void onDataReceived(final byte[] buffer, final int size) {
		runOnUiThread(() -> {
            if (mReception != null) {
                mReception.append(new String(buffer, 0, size));
            }
        });
	}
}
