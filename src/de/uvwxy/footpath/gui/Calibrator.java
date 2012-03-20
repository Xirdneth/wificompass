package de.uvwxy.footpath.gui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import at.fhstp.wificompass.R;
import at.fhstp.wificompass.userlocation.StepDetectionProvider;
import de.uvwxy.footpath.core.StepDetection;
import de.uvwxy.footpath.core.StepTrigger;

/**
 * This Activity is used to calibrate the parameters concerning step detection
 * 
 * @author Paul Smith
 * 
 */
public class Calibrator extends Activity implements StepTrigger {
	

	private StepDetection stepDetection;

	PaintBoxHistory svHistory;

	// GUI
	TextView tvPeak = null;

	TextView tvFilter = null;

	TextView tvTimeout = null;

	SeekBar sbPeak = null;

	SeekBar sbFilter = null;

	SeekBar sbTimeout = null;

	float peak; // threshold for step detection

	float a; // value for low pass filter

	int step_timeout_ms; // distance in ms between each step

	OnSeekBarChangeListener sbListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
			if (arg0.equals(sbPeak)) {
				peak = sbPeak.getProgress() / 10.0f;
				stepDetection.setPeak(peak);
				tvPeak.setText("Set peak value: (" + peak + ")");
			} else if (arg0.equals(sbFilter)) {
				a = sbFilter.getProgress() / 100.0f;
				stepDetection.setA(a);
				tvFilter.setText("Set filter value: (" + a + ")");
			} else if (arg0.equals(sbTimeout)) {
				step_timeout_ms = sbTimeout.getProgress();
				stepDetection.setStep_timeout_ms(step_timeout_ms);
				tvTimeout.setText("Set step timeout: (" + step_timeout_ms + ")");
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar arg0) {
		}

	};

	private void loadSettings() {
		a = getSharedPreferences(StepDetectionProvider.CALIB_DATA, 0).getFloat("a", 0.5f);
		peak = getSharedPreferences(StepDetectionProvider.CALIB_DATA, 0).getFloat("peak", 0.5f);
		step_timeout_ms = getSharedPreferences(StepDetectionProvider.CALIB_DATA, 0).getInt("timeout", 666);

		// Update GUI elements
		sbPeak.setProgress((int) (peak * 10));
		sbFilter.setProgress((int) (a * 100));
		sbTimeout.setProgress(step_timeout_ms);

		tvPeak.setText("Set peak value: (" + peak + ")");
		tvFilter.setText("Set filter value: (" + a + ")");
		tvTimeout.setText("Set step timeout: (" + step_timeout_ms + ")");
	}

	private void saveSettings() {
		// Save current values to settings
		SharedPreferences settings = getSharedPreferences(StepDetectionProvider.CALIB_DATA, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("a", a);
		editor.putFloat("peak", peak);
		editor.putInt("timeout", step_timeout_ms);
		// Apply changes
		editor.commit();
	}

	@Override
	public void dataHookAcc(long nowMs, double x, double y, double z) {
	}

	@Override
	public void dataHookComp(long nowMs, double x, double y, double z) {
	}

	@Override
	public void timedDataHook(long nowMs, double[] acc, double[] comp) {
		svHistory.addTriple(nowMs, acc);
	}

	@Override
	public void trigger(long nowMs, double compDir) {
		svHistory.addStepTS(nowMs);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initUI();

	}

	protected void initUI() {
		setContentView(R.layout.calibrator);

		tvPeak = (TextView) findViewById(R.id.calibrator_tvPeak);
		tvFilter = (TextView) findViewById(R.id.calibrator_tvFilter);
		tvTimeout = (TextView) findViewById(R.id.calibrator_tvTimeout);

		sbPeak = (SeekBar) findViewById(R.id.calibrator_sbPeak);
		sbFilter = (SeekBar) findViewById(R.id.calibrator_sbFilter);
		sbTimeout = (SeekBar) findViewById(R.id.calibrator_sbTimeout);

		// Load settings after creation of GUI-elements, to set their values
		loadSettings();
		stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);
		// Add OnSeekBarChangeListener after creation of step detection, because object is used
		sbPeak.setOnSeekBarChangeListener(sbListener);
		sbFilter.setOnSeekBarChangeListener(sbListener);
		sbTimeout.setOnSeekBarChangeListener(sbListener);

		LinearLayout linLayout = (LinearLayout) findViewById(R.id.calibrator_LinearLayout01); // get pointer to layout
		SurfaceView svOld = (SurfaceView) findViewById(R.id.calibrator_svHistory); // get SurfaceView defined in xml
		LayoutParams lpHistory = svOld.getLayoutParams(); // get its layout params

		long samples_per_second = 1000 / stepDetection.INTERVAL_MS;
		int history_in_seconds = 4;
		int samples_per_history = (int) (history_in_seconds * samples_per_second);

		// create PaintBox (-24.0 to 24.0, 100 entries)
		svHistory = new PaintBoxHistory(this, 48.0, samples_per_history, history_in_seconds);

		linLayout.removeView(svOld); // and remove surface view from layout
		linLayout.addView(svHistory, lpHistory); // add surface view clone to layout
	}

	@Override
	public void onPause() {
		super.onPause();
		saveSettings();
		stepDetection.unload();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		loadSettings();
		stepDetection.load();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		initUI();
	}

}