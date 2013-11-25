package com.fhws.RoboAR;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SearchDevice extends Activity {

	private final int BT_REQUEST_CODE = 1;
	private final String MAC_STRING = "mac";

	private ListView lvFoundDevices;
	private Button btScan;
	private ProgressBar progressbar;

	private BluetoothAdapter btAdapter;
	private List<String> adapterList;
	private ArrayAdapter<String> lvAdapter;


	/**
	 * Beim Start der App werden
	 * a) alle GUI-Elemente und deren zugehörige Komponenten inkl. Listener initialisiert
	 * b) überprüft, ob Bluetooth verfügbar ist (verbaut und eingeschalten; wenn aus, Anfrage zum einschalten
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initGUI();

		initBT();

	}


	/**
	 * Die Liste der gefundenen Devices wird geleert und neu nach verfügbaren Devices gesucht
	 * Falls Bluetooth zwischendurch deaktiviert wurde, wird es wieder eingeschalten
	 */
	public void onResume()
	{
		super.onResume();

		adapterList.clear();
		lvAdapter.notifyDataSetChanged();

		if ( btAdapter.isEnabled() == true)
		{
			getAvailableDevices();
		}
		else
		{
			enableBT();
			getAvailableDevices();
		}

	}

	public void onDestroy()
	{
		super.onDestroy();
		
		//Entfernen des BroadcastReceivers für Bluetooth-Devices
		unregisterReceiver(receiver);
	}

	/**
	 * Initialisiert die Komponenten des Geraeteauswahlfensters und fuegt die Listener von Button und ListView hinzu
	 */
	private void initGUI() {

		//Scan-Button. Bei Betätigung wird die Discovery erneut angestoßen
		btScan = (Button) findViewById(R.id.bScanAgain);
		btScan.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				/*
				 * Discovery darf nur erneut angestoßen, wenn sie gerade NICHT läuft
				 * Die Liste der Devices muss geleert werden, damit keine doppelten Nennungen vorkommen
				 */
				if (btAdapter.isDiscovering() == true)
				{
					btAdapter.cancelDiscovery();
				}
				adapterList.clear();
				lvAdapter.notifyDataSetChanged();
				btAdapter.startDiscovery();

			}
		});


		lvFoundDevices = (ListView) findViewById(R.id.lvFoundBtDevices);
		lvFoundDevices.setOnItemClickListener(
				new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						
						//Buttons deaktivieren und ProgressBar starten
						btScan.setEnabled(false);
						progressbar = (ProgressBar) findViewById(R.id.StartUnityProgress);
						progressbar.setVisibility(View.VISIBLE);
						lvFoundDevices.setVisibility(View.INVISIBLE);

						//Da sich der Nutzer ein Device ausgesucht hat, kann die Suche nach anderen Geräten beendet werden
						btAdapter.cancelDiscovery();
						
						//Entfernen des BroadcastReceivers für Bluetooth-Devices
						unregisterReceiver(receiver);

						/*
						 * Bestimmen der MAC-Adresse des gewaehlten Devices
						 * in arg1 befindet sich der String des gewählten Items aus der ListView
						 * Dieser muss allerdings noch explizit auf String gecastet werden
						 */
						String deviceText = ((TextView) arg1).getText().toString();
						String mac = deviceText.substring(deviceText.indexOf("\n")+1);

						//Anlegen des Intents zum Aufruf der Controller-Activity
						Intent intent = new Intent(getApplicationContext(), MainActivity.class);
						intent.putExtra(MAC_STRING, mac);

						
						startActivity(intent);

					}
				} 
				);

		adapterList = new ArrayList<String>();
		lvAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, adapterList);
		lvFoundDevices.setAdapter(lvAdapter);
	}


	/**
	 * Überprüft, ob ein BluetooModul vorhanden ist und initialisiert es
	 */
	private void initBT()
	{
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		//Test, ob Bluetooth Modul im System vorhanden ist
		if (btAdapter == null)
		{
			Toast.makeText(this, R.string.btNotFound, Toast.LENGTH_LONG).show();;
			finish();
		}


	}

	/**
	 * Aktiviert das Bluetooth-Modul, falls es deaktiviert ist
	 */
	private void enableBT()
	{
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, BT_REQUEST_CODE);
	}


	/**
	 * Startet die Suche nach BluetoothDevices in der Nähe
	 */
	private void getAvailableDevices()
	{
		btAdapter.startDiscovery();

		// Anlegen des BroadcastReceivers mit dem IntentFilter, dass nur beim Fund neuer BluetoothDevices etwas getan werden soll
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(receiver, filter); // Don't forget to unregister during onDestroy
	}

	//BroadcastReceiver für ACTION_FOUND anlegen
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {

				//Das BluetoothDevice aus dem Intent herausziehen
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				//Device soll der Liste nur hinzugefügt werden, wenn es sich um einen möglichen ROBO TX Controller handelt
				if (device.getName().toLowerCase().contains("robo"))
				{
				// Name und Adresse dem ArrayAdapter hinzufügen
				adapterList.add(device.getName() + "\n" + device.getAddress());
				lvAdapter.notifyDataSetChanged();
				}
			}
		}
	};
	

	/**
	 * Wird ausgeführt, wenn das System auf einen gesendeten Intent eine Rückmeldung gibt
	 * Wenn ein RESULT_CANCELED zurückkommt, konnte Bluetooth nicht aktiviert werden und die App soll beendet werden
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == BT_REQUEST_CODE && resultCode == RESULT_CANCELED)
		{
			Toast.makeText(this, R.string.btActivationError, Toast.LENGTH_LONG).show();
			finish();
		}
	}


}