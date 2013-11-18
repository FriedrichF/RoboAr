package com.fhws.RoboAR;

import de.thserv.robodroid.api.*;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
//import android.widget.ImageView;
import android.widget.TextView;


public class Controller extends Activity {

	private static TextView tvMotorLeft;
	private static TextView tvMotorRight;
	private CheckBox lights;
	private static CheckBox resetSliders;
	private CustomSeekBar seekBarLeft;
	private CustomSeekBar seekBarRight;
	private String mac;
	
	private final int LED_LEFT = 4;
	private final int LED_RIGHT = 5;



	/**
	 * Initialisieren der GUI-Elemente, setzen der Listener, Abspeichern der übergebenen MAC-Adresse
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_gui);

		tvMotorLeft = (TextView) findViewById(R.id.tvMotorLeft);
		tvMotorRight = (TextView) findViewById(R.id.tvMotorRight);
		
		resetSliders = (CheckBox) findViewById(R.id.cbSliders);
		
		
		//Initialisieren von Checkbox für LEDs und dem Listener
		lights = (CheckBox) findViewById(R.id.cbLightSwitch);
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
		
		

		Intent intent = getIntent();
		mac = intent.getStringExtra("mac");

		
		seekBarLeft=(CustomSeekBar) findViewById(R.id.sbMotorLeft);
		seekBarLeft.setPosition(MotorPosition.LEFT);


		//Initialisieren der rechten Seekbar + Festlegen der Position
		seekBarRight=(CustomSeekBar) findViewById(R.id.sbMotorRight);
		seekBarRight.setPosition(MotorPosition.RIGHT);
		


	}
	
	/**
	 * Beenden der Übertragung
	 */
	public void onPause()
	{
		super.onPause();
		ApiEntry.getInstance().stopTransferArea();
		ApiEntry.getInstance().close();

	}

	
	/**
	 * Bringt die Activity wieder in den Startzustand (Motoren aus, Seekbars im Ausgangszustand, Übertragung aktivieren)
	 */
	public void onResume()
	{
		super.onResume();
		
		//Deaktivieren der Motoren
		setMotor(0, 0, MotorPosition.LEFT);
		setMotor(0, 0, MotorPosition.RIGHT);

		//Linke und rechte Seekbar wieder in den Ausgangszustand setzen
		seekBarLeft.setProgress(seekBarLeft.getMax()/2);
		seekBarRight.setProgress(seekBarRight.getMax()/2);

		//Übertragung wieder Aktivieren
		ApiEntry.getInstance().changeToOnlineMode();
		ApiEntry.getInstance().setMacAdress(mac);
		ApiEntry.getInstance().open();
		ApiEntry.getInstance().startTransferArea();
		

	}	

	
	
	/**
	 * Ändert Geschwindigkeit und Richtung eines der beiden Motoren
	 * @param motorSpeed die neue Geschwindigkeit des Motors (positiv = vorwärts, negativ = rückwärts)
	 * @param percentage die Prozente der maximalen Geschwindigkeit des Motors als Ganzzahl (z.B 90)
	 * @param isLeft Angabe, ob der linke oder der rechte Motor angesteuert werden soll
	 */
	public static void setMotor(int motorSpeed, int percentage, MotorPosition position) {
		
		//Wenn der linke Motor
		if (position == MotorPosition.LEFT)
		{
			//Setzen der Textausgabe
			tvMotorLeft.setText( percentage + "%" );

			
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
		
		//Rechter Motor
		else
		{
			
			tvMotorRight.setText( percentage + "%" );
			
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
