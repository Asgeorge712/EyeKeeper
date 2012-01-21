package com.paulgeorge.ek;

import java.util.List;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

// 014994AF0A017009

public class EyeKeeperActivity extends MapActivity {
	private boolean pingActive = true;
	private BroadcastReceiver receiver;
	public static final String ACTIVITY_ACTION = "com.paulgeorge.ek.EyeKeeperActivity.ACTION";
	public static final String MESSAGE_BODY = "com.paulgeorge.ek.EyeKeeperActivity.BODY";

	public static final String MESSAGE_PREAMBLE = "EyeKeeper-1.0.0.1: ";

	// Sendable Actions
	//public static final int INIT_ACTION = 0;
	//public static final int PING = 1;
	//public static final int START_TRACKING = 2;
	//public static final int STOP_TRACKING = 3;
	//public static final int GET_UPTIME = 4;
	//public static final int GET_STARTTIME = 5;

	// Receivable actions
	//public static final int PING_REPLY = 6;
	//public static final int LOCATION = 7;
	//public static final int STOP_REC_ACTION = 8;
	//public static final int UPTIME_REPLY = 9;
	//public static final int STARTTIME_REPLY = 10;

	//public static final int ERROR_REPLY = 99;
	
	private AppVars vars;

	private MapView mapView;
	private MapController mapController;

	/*******************************************************************
	 * 
	 * Called when the activity is first created.
	 * 
	 *******************************************************************/
	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		vars = AppVars.getInstance();
		
		if ( vars.getServerPhone() == null ) {
			TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			vars.setServerPhone( tMgr.getLine1Number() );
			Log.i("EyeKeeperActivity", "My phone number is: " + vars.getServerPhone() );
		}

		Log.i("onCreate", "onCreate has started!!!!!!!!!!!");
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive( Context context, Intent intent ) {
				Log.i("EyeKeeperActivity", "I received a new broadcast event!");
				String body = intent.getExtras().getString(MESSAGE_BODY); // Intent.EXTRA_TEXT
				Log.i("EyeKeeperActivity", "Body is: " + body);

				if ( body != null ) {
					processReceivedMessage(body);
				}
			}
		};

		if ( vars.getClientPhone() == null ) {
			Log.i("onCreate", "ClientNumber is NUUULLLL!!!!");
			getClientPhoneNumber();
		}

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapController = mapView.getController(); 
		mapController.setZoom(15);
		
		/*
		 * 
		 * double lat = 38.737305; double lng = -77.560111;
		 *  GeoPoint point = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
		 * setClientLocation(point, "Target Location!", "Not moving.");
		 */
	}


	/****************************************************
	 * Valid Messages from Client 1 lat lng loc|lat|lng uptime|long
	 * starttime|long
	 * 
	 * 
	 * 
	 *****************************************************/
	private void processReceivedMessage( String body ) {
		StringTokenizer tok = new StringTokenizer(body, " ");
		if ( !tok.hasMoreTokens() )
			return;
		@SuppressWarnings("unused")
		String preamble = tok.nextToken();
		String action = tok.nextToken();
		int action_int = Integer.parseInt(action);
		Log.i("processReceivedMessage", "Action: " + action_int);
		Command cmd = Command.lookup(action_int);
		
		switch (cmd) {
			case PING_REPLY:
				pingActive = true;
				Log.i("EyeKeeperActivity", "Processing a PING reply Action");
				// Update Map
				String latStr = tok.nextToken();
				String lngStr = tok.nextToken();
				Log.i("EyeKeeperActivity","Lat received: " + latStr);
				Log.i("EyeKeeperActivity","Lng received: " + lngStr);
				

				double latD = Double.parseDouble(latStr) * 1E6;
				double lngD = Double.parseDouble(lngStr) * 1E6;

				int lat = (int) latD;
				int lng = (int) lngD;
				
				Log.i("EyeKeeperActivity"," Lat int is: " + lat);
				Log.i("EyeKeeperActivity"," Lng int is: " + lng);

				GeoPoint point = new GeoPoint(lat, lng);
				
				Log.i("EyeKeeperActivity","GreoPoint is: " + point.toString() );

				setClientLocation(point, "Ping Reply", "New Location");

				// Post info to text area
				addToMessageArea("Client position has been updated.");
				invalidateOptionsMenu();
				break;

			case STOP_REC_ACTION:
				Log.i("EyeKeeperActivity", "Client has received my Stop Request.");
				// Post info to text area

				break;

			case LOCATION:
				Log.i("EyeKeeperActivity", "Processing a LOC Action");
				// Update Map

				// Post info to text area

				break;

			case UPTIME_REPLY:
				Log.i("EyeKeeperActivity", "Processing a UPTIME Action");
				// Post info to text area

				break;

			case STARTTIME_REPLY:
				Log.i("EyeKeeperActivity", "Processing a STARTTIME Action");
				// Post info to text area

				break;

			default:
				Log.i("EyeKeeperActivity", "Received an UNKNOWN Action: " + action);
				// Post a note to text area (fyi).
		}

	}


	/********************************************************************
	 * 
	 * 
	 * @return
	 ********************************************************************/
	private void getClientPhoneNumber() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Connect to a Client");
		alert.setMessage("Enter a Phone Number");
		
		String ph = "";
		if ( vars.getClientPhone() == null ) {
			ph = vars.getClientNumberDefault();
		}
		else {
			ph = vars.getClientPhone();
		}

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_PHONE);
		input.setText( ph );
		input.selectAll();
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick( DialogInterface dialog, int whichButton ) {
				vars.setClientPhone( input.getText().toString() );
				Log.i("getClientPhoneNumer", "onClick has set clientNumber to:" + vars.getClientPhone());
				addToMessageArea("Client Number set to: " + vars.getClientPhone());
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick( DialogInterface dialog, int whichButton ) {
				// Canceled.
			}
		});

		alert.show();
	}


	/***********************************************************
	 * 
	 * 
	 ***********************************************************/
	private void addToMessageArea( String message ) {
		TextView messages = (TextView) findViewById(R.id.messageArea);
		if ( messages == null ) {
			Log.i("addToMessageArea", "Messages area is null!!!");
			return;
		}
		String currentMessages = (String) messages.getText();
		currentMessages += "\n" + message;

		messages.setText(currentMessages);
	}


	/***********************************************************
	 * 
	 * 
	 ***********************************************************/
	@Override
	public void onResume() {
		super.onResume();
		Log.i("onResume", "onResume has been called!!!");
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTIVITY_ACTION);
		registerReceiver(receiver, filter);
	}


	/***********************************************************
	 * 
	 * 
	 ***********************************************************/
	@Override
	public void onPause() {
		super.onPause();
		Log.i("onPause", "onPause has been called!!!");
		unregisterReceiver(receiver);
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	private void setClientLocation( GeoPoint point, String noteHead, String noteMsg ) {
		List<Overlay> mapOverlays = mapView.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.arrow_64);

		EyeKeeperItemizedOverlay io = new EyeKeeperItemizedOverlay(drawable, this);
		OverlayItem overlayitem = new OverlayItem(point, noteHead, noteMsg);

		io.addOverlay(overlayitem);
		mapOverlays.add(io);
		mapController.animateTo(point);

	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch (item.getItemId()) {
			case R.id.ping:
				popMessage("Sending ping, a reply may take a few seconds. No other pings can be sent until a reply is received.");
				sendClientSystemSMS(Command.PING.value());
				pingActive = false;
				invalidateOptionsMenu();
				addToMessageArea("Ping Command Sent");
				return true;

			case R.id.tracking:
				if ( item.isChecked() ) {
					item.setChecked(false);
					popMessage("Stopping client tracking.");
					sendClientSystemSMS(Command.STOP_TRACKING.value());
					addToMessageArea("Stop Tracking Command Sent");
				}
				else {
					item.setCheckable(true);
					item.setChecked(true);
					popMessage("Starting up client tracking, it may take some time to start receiving data.");
					sendClientSystemSMS(Command.START_TRACKING.value());
					addToMessageArea("Start Tracking Command Sent");
				}
				return true;

			case R.id.where_r_u:
				popMessage("Sending Text: " + getString(R.string.where_r_u_going));
				sendClientSMS(R.string.where_r_u_going);
				return true;

			case R.id.come_home:
				popMessage("Sending Text: " + getString(R.string.come_home));
				sendClientSMS(R.string.come_home);
				return true;

			case R.id.dinner:
				popMessage("Sending Text: " + getString(R.string.dinner));
				sendClientSMS(R.string.dinner);
				return true;

			case R.id.call_me:
				popMessage("Sending Text: " + getString(R.string.call_me));
				sendClientSMS(R.string.call_me);
				return true;
			case R.id.resetNumber:
				getClientPhoneNumber();
				return true;
			case R.id.help:
				Log.i("onOptionsItemSelected", "Help was selected");
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	private void popMessage( String message ) {
		int duration = Toast.LENGTH_LONG;
		Context context = getApplicationContext();

		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	@Override
	public boolean onPrepareOptionsMenu( Menu menu ) {
		if ( !pingActive )
			menu.findItem(R.id.ping).setEnabled(false);
		return true;
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	public void sendClientSMS( int message_id ) {
		SmsManager sms = SmsManager.getDefault();
		String msg = getString(message_id);
		sms.sendTextMessage(vars.getClientPhone() , null, msg, null, null);
		addToMessageArea("Text Sent: " + msg);
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	public void sendClientSystemSMS( int message_id ) {
		SmsManager sms = SmsManager.getDefault();
		String msg = EyeKeeperService.MESSAGE_PREAMBLE + message_id;

		msg += " reply:" + vars.getServerPhone();
		sms.sendTextMessage(vars.getClientPhone() , null, msg, null, null);
	}


	/*******************************************************************
	 * 
	 * 
	 * 
	 *******************************************************************/
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}