package com.iswitch.iswitch;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);	
		
		TextView promptTextView = (TextView)findViewById(R.id.about);  
		  promptTextView.setMovementMethod(LinkMovementMethod.getInstance());
		  
		Button sbackButton = (Button) findViewById(R.id.sback);
		sbackButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {		
				finish();
			}

		});
	}
}
