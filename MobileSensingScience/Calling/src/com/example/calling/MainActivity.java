package com.example.calling;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void process(View view) throws InterruptedException {

		double[] entr = new double[9];

		for (int j = 0; j <= 8; j++) {
			for (int i = 4; i <= 16; i = i * 2) {
				int fileSize = i * 1024 * 1024;
				startNewActivity("com.example.ziptest", fileSize + "", j + "");
				Thread.sleep((fileSize / 1024) * 10 + 10000);
			}
		}

		// for (int i = 40; i >= 5; i/=2) {
		//
		// int fileSize = i*1024*1024;
		//
		// startNewActivity("com.example.ziptest", fileSize+"");
		// //startNewActivity("com.example.sendtest", i+".gz");
		// try {
		// int delay =(i/100) + 5000;
		// Thread.sleep(delay);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
	}

	public void startNewActivity(String packageName, String data, String entropy) {
		Intent launchIntent = getPackageManager().getLaunchIntentForPackage(
				packageName);

		launchIntent.putExtra("data", data);
		launchIntent.putExtra("entropy", entropy);
		startActivityForResult(launchIntent, 0);
	}
}

/*
 * dead code
 * 
 * @Override protected void onActivityResult(int requestCode, int resultCode,
 * Intent data) { // super.onActivityResult(requestCode, resultCode, data); if
 * (resultCode == RESULT_OK) {
 * 
 * try { Log.i("MA", "Result" + data.getIntExtra("value", 0));
 * doIt(data.getIntExtra("value", 0)); } catch (Exception e) { Log.w("MA", "" +
 * e.toString()); } } else { Log.i("MA", "errror" + resultCode); } }
 */