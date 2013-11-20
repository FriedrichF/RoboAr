package com.fhws.RoboAR;

import java.util.Timer;
import java.util.TimerTask;

import android.app.ActionBar.LayoutParams;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.qualcomm.QCAR.QCAR;
import com.qualcomm.QCARUnityPlayer.QCARPlayerActivity;
import com.qualcomm.QCARUnityPlayer.QCARUnityPlayer;

public class MainActivity extends QCARPlayerActivity {

	private QCARUnityPlayer mQCARView = null;
    private Timer mViewFinderTimer = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mViewFinderTimer != null) {
       mViewFinderTimer.cancel();
       mViewFinderTimer = null;
        }
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
            View myView = getLayoutInflater().inflate(R.layout.control_gui, null);

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
}
