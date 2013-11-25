package com.fhws.RoboAR;

import java.util.Timer;
import java.util.TimerTask;

import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.QCAR.QCAR;
import com.qualcomm.QCARUnityPlayer.QCARPlayerActivity;
import com.qualcomm.QCARUnityPlayer.QCARUnityPlayer;

import de.thserv.robodroid.api.ApiEntry;

public class MainActivity extends QCARPlayerActivity {

	//Unity
	private QCARUnityPlayer mQCARView = null;
	private Timer mViewFinderTimer = null;

	//Controller
	private static TextView tvMotorLeft;
	private static TextView tvMotorRight;
	private CheckBox lights;
	private static CheckBox resetSliders;
	private SpeedSeekBar seekBarLeft;
	private SpeedSeekBar seekBarRight;
	private String mac;

	private final int LED_LEFT = 4;
	private final int LED_RIGHT = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mac = intent.getExtras().getString("mac");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mQCARView == null) {
			//search the QCAR view
			mViewFinderTimer = new Timer();
			mViewFinderTimer.scheduleAtFixedRate(new QCARViewFinderTask(), 1000, 1000);
		}

		//Deaktivieren der Motoren
		setMotor(0, 0, MotorPosition.LEFT);
		setMotor(0, 0, MotorPosition.RIGHT);

		//Linke und rechte Seekbar wieder in den Ausgangszustand setzen
//		seekBarLeft.setProgress(seekBarLeft.getMax()/2);
//		seekBarRight.setProgress(seekBarRight.getMax()/2);

		//Übertragung wieder Aktivieren
		ApiEntry.getInstance().changeToOnlineMode();
		ApiEntry.getInstance().setMacAdress(mac);
		ApiEntry.getInstance().open();
		ApiEntry.getInstance().startTransferArea();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mViewFinderTimer != null) {
			mViewFinderTimer.cancel();
			mViewFinderTimer = null;
		}

		ApiEntry.getInstance().stopTransferArea();
		ApiEntry.getInstance().close();
	}

	class QCARViewFinderTask extends TimerTask {
		public void run() {
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					if (!QCAR.isInitialized()) return; //wait for QCAR init
					if (mQCARView != null) return;//already found, no need to search                    
					//else search
					View rootView = MainActivity.this.findViewById(android.R.id.content);
					QCARUnityPlayer qcarView = findQCARView(rootView);
					//if QCAR view has been found, add some android view/widget on top
					if (qcarView != null) {
						ViewGroup qcarParentView = (ViewGroup)(qcarView.getParent());
						View myView = getLayoutInflater().inflate(R.layout.speed_control, null);

						initController(myView);

						Toast.makeText(getApplicationContext(), mac, Toast.LENGTH_LONG).show();

						qcarParentView.addView(myView, new LayoutParams
								(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
						mQCARView = qcarView;
					}
				}
			});
		}
		private QCARUnityPlayer findQCARView(View view) {
			if (view instanceof QCARUnityPlayer) {
				return (QCARUnityPlayer)view;
			}
			if (view instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup)view;
				for (int i = 0; i < vg.getChildCount(); ++i) {
					QCARUnityPlayer foundView = findQCARView(vg.getChildAt(i));
					if (foundView != null)
						return foundView;
				}
			}
			return null;
		}
	}

	private void initController(View view){
		resetSliders = (CheckBox) view.findViewById(R.id.cbSliders);
		
		
		//Initialisieren von Checkbox für LEDs und dem Listener
		lights = (CheckBox) view.findViewById(R.id.cbLightSwitch);
		lights.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if (isChecked)
				{
					//Aktivieren der Ausgänge M5, M6
					ApiEntry.getInstance().SetOutPwmValues(LED_LEFT, (short) 512);
					ApiEntry.getInstance().SetOutPwmValues(LED_RIGHT, (short) 512);
				}
				else
				{
					//Deaktivieren der Ausgänge M5, M6
					ApiEntry.getInstance().SetOutPwmValues(LED_LEFT, (short) 0);
					ApiEntry.getInstance().SetOutPwmValues(LED_RIGHT, (short) 0);
				}
			}
		});
		
		seekBarLeft=(SpeedSeekBar) view.findViewById(R.id.seekBarLeft);
		seekBarLeft.setPosition(MotorPosition.LEFT);


		//Initialisieren der rechten Seekbar + Festlegen der Position
		seekBarRight=(SpeedSeekBar) view.findViewById(R.id.seekBarRight);
		seekBarRight.setPosition(MotorPosition.RIGHT);
		
		//Linke und rechte Seekbar wieder in den Ausgangszustand setzen
		seekBarLeft.setProgress(seekBarLeft.getMax()/2);
		seekBarRight.setProgress(seekBarRight.getMax()/2);
	}

	/**
	 * Ändert Geschwindigkeit und Richtung eines der beiden Motoren
	 * @param motorSpeed die neue Geschwindigkeit des Motors (positiv = vorwärts, negativ = rückwärts)
	 * @param percentage die Prozente der maximalen Geschwindigkeit des Motors als Ganzzahl (z.B 90)
	 * @param isLeft Angabe, ob der linke oder der rechte Motor angesteuert werden soll
	 */
	public static void setMotor(int motorSpeed, int percentage, MotorPosition position) {

		//Wenn der rechte Motor
		if (position == MotorPosition.RIGHT)
		{
			//Setzen der Textausgabe
			//tvMotorLeft.setText( percentage + "%" );


			/*
			 * Setzen von Geschwindigkeit und Richtung des linken Motors
			 * Nach geschätzter Wahrscheinlichkeit sortiert
			 */
			if (motorSpeed > 0)
			{
				/*
				 * SetOutMotorValues(int motorId, duty_p, duty_m)
				 * MotorID ist der Anschluss am Controller
				 * duty_p: der positive PWM-Wert ( = Geschwindigkeit vorwärts)
				 * duty_m: der negative PWM-Wert ( = Geschwindigkeit rückwärts) 
				 */
				ApiEntry.getInstance().SetOutMotorValues(1, motorSpeed, 0);
			}
			//Anhalten
			else if (motorSpeed == 0)
			{
				ApiEntry.getInstance().SetOutMotorValues(1, 0, 0);
			}
			//Rückwärts fahren
			else
			{
				ApiEntry.getInstance().SetOutMotorValues(1, 0, Math.abs(motorSpeed));
			}
		}

		//Linker Motor
		else
		{

			//tvMotorRight.setText( percentage + "%" );

			/*
			 * Setzen von Geschwindigkeit und Richtung des rechten Motors
			 * Nach geschätzter Wahrscheinlichkeit sortiert
			 */
			if (motorSpeed > 0)
			{
				ApiEntry.getInstance().SetOutMotorValues(0, motorSpeed, 0);
			}
			else if (motorSpeed == 0)
			{
				ApiEntry.getInstance().SetOutMotorValues(0, 0, 0);
			}
			else
			{
				ApiEntry.getInstance().SetOutMotorValues(0, 0, Math.abs(motorSpeed));
			}
		}

	}
	
	public static boolean getResetBoxStatus()
	{
		return resetSliders.isChecked();
	}
}
