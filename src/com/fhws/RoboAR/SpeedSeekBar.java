package com.fhws.RoboAR;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class SpeedSeekBar extends SeekBar {
	
	MotorPosition position;
	
	int localProgress;
	int median = getMax()/2;
	int baseValue = 512-median;
	int percentage = 0;

	public SpeedSeekBar(Context context) {
		super(context);
	}
	
	public SpeedSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SpeedSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			localProgress = (int) (getMax() * event.getX() / getWidth());
			
			if (localProgress > 320)
				localProgress = 320;
			if (localProgress < 0)
				localProgress = 0;
			
			setProgress(localProgress);
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			percentage = getPercentage();
			MainActivity.setMotor(getMotorSpeed(percentage), percentage, position);
			break;
		case MotionEvent.ACTION_UP: //Listener anstatt koppelung
			
			if (MainActivity.getResetBoxStatus())
			
			{
				setProgress(0);
				setProgress(160);
				MainActivity.setMotor(0, 0, position);
			}
			

		case MotionEvent.ACTION_CANCEL:
			break;
		}

		return true;
	}
	
	


	public void setPosition (MotorPosition position)
	{
		this.position = position;
	}
	
	public int getPercentage()
	{
		//Percentage ist eine Ganzzahl (z.B. percentage = 90 entspricht 90%)
		return 10*Math.round((float)10*(localProgress-median)/median); 
	}
	
	public void setPercentage(int percent)
	{
		this.percentage = percent;
	}
	
	public void setProgress(int progress)
	{
		this.localProgress = progress;
		super.setProgress(progress);
	}
	
	public int getLocalProgress()
	{
		return localProgress;
	}

	public int getMotorSpeed(int percent)
	{		
		if (percent > 0)
		{
			//Positiver Rückgabewert --> Vorwärtsfahren in Controller.setMotor(...)
			return baseValue + (int)(percent*median/100);
		}
		else if (percent < 0)
		{
			//Negativer Rückgabewert --> Rückwärtsfahren in Controller.setMotor(...)
			return baseValue * (-1) + (int)(percent*median/100);
		}
		else
		{
			return 0;
		}
	}
	
}
