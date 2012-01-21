package com.paulgeorge.ek;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

/*******************************************************************
 * 
 * 
 * 
 *******************************************************************/
public class EyeKeeperService {

	public static final String MESSAGE_PREAMBLE = "EyeKeeper-2.0.0.1: ";
	private Timer timer;
	private Context ctx;
	private boolean timerRunning = false;

	private static EyeKeeperService instance;

	/*******************************************************
	 * Timer task which runs every interval set up by timer.
	 * 
	 ********************************************************/
	private TimerTask updateTask = new TimerTask() {
		@Override
		public void run() {
			// Calculate Location and send text message to server number
			Log.i("EyeKeeperService", "Timer kicking off.. looking for location...");
			sendCurrentLocationToServer();
			Log.i("EyeKeeperService", "Timer task sent current location to: " + AppVars.getInstance().getServerPhone());
		}
	};


	/*****************************************************
	 * Constructor
	 * 
	 ******************************************************/
	protected EyeKeeperService(Context context) {
		this.ctx = context;
		Log.i("EyeKeeperService", "Service constructor is done running.");
	}


	/*****************************************************
	 * 
	 * 
	 ******************************************************/
	public static EyeKeeperService getInstance( Context ctx ) {
		if ( instance == null ) {
			instance = new EyeKeeperService(ctx);
		}
		return instance;
	}


	/*****************************************************
	 * Service has received a BroadcastIntent from EyeKeeperReceiver
	 * 
	 ******************************************************/
	public void processMessage( String msg ) {
		AppVars vars = AppVars.getInstance();
		Log.i("EyeKeeperService", "Processing Message, body is: " + msg);

		// A message will have:
		// A preamble,
		// A command, and
		// Zero or more command parameters
		StringTokenizer tok = new StringTokenizer(msg, " ");

		String command = "";
		int command_int = -1;

		ArrayList<String> nameValues = new ArrayList<String>();

		HashMap<String, String> params = new HashMap<String, String>();

		// Skip the preamble
		if ( tok.hasMoreTokens() ) tok.nextToken();
		
		//Grab the next token, which should be command.
		if ( tok.hasMoreTokens() ) command = (String) tok.nextToken();
		
		//Convert Command to an int
		try {
			command_int = Integer.parseInt(command);
		}
		catch (Exception e) {
			Log.e("EyeKeeperService", "Command: " + command + " could not be turned into an int.");
		}

		Log.i("EyeKeeperService", "Command Received: " + command);

		while (tok.hasMoreTokens()) {
			String nextToken = (String) tok.nextToken();
			Log.i("EyeKeeperService", "Found another token in message:" + nextToken);
			nameValues.add(nextToken);
		}

		if ( nameValues.size() > 0 ) {
			Log.i("EyeKeeperService", "Parsing the extra tokens.");
			params = parseNameValues(nameValues);
		}

		if ( params.containsKey("REPLY") ) {
			Log.i("EyeKeeperServer", "Using params to set Server reply Phone number.");
			vars.setServerPhone(params.get("REPLY"));
		}

		// Look at the command and decide what to do....
		if ( command.equals("") ) {
			Log.e("EyeKeeperService", "No Command was in the text message!");
			return;
		}
		else if ( !vars.validServerPhone() ) {
			Log.e("EyeKeeperService", "Server Ph Num is blank! Initialization ERROR!!");
			return;
		}
		Command cmd = Command.lookup(command_int);

		switch (cmd) {
			case PING:
				sendPingReplyToServer();
				break;
			case START_TRACKING:
				if ( !timerRunning ) { // Start Timer
					startTimer(params);
				}
				break;
			case STOP_TRACKING:
				if ( timerRunning ) { // Stop Timer
					stopTimer("Stop Request");
				}
				break;
			case GET_UPTIME:
				sendUptimeReplyToServer();
				break;
			case GET_STARTTIME:
				sendStarttimeReplyToServer();
				break;
			default:
				Log.e("EyeKeeperService", "UNKNOWN COMMAND: " + msg);
		}
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private void sendStarttimeReplyToServer() {
		String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.STARTTIME_REPLY.value() + " "
				+ AppVars.getInstance().getServerStartDate().getTime();
		sendServerSMS(message);
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private void sendUptimeReplyToServer() {
		Date currDate = new Date();
		long diff = currDate.getTime() - AppVars.getInstance().getServerStartDate().getTime();

		String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.UPTIME_REPLY.value() + " " + diff;
		sendServerSMS(message);
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private void sendPingReplyToServer() {
		Location loc = getCurrentLocation();
		String lat = "" + loc.getLatitude();
		String lng = "" + loc.getLongitude();

		String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.PING_REPLY.value() + " " + lat + " " + lng;
		sendServerSMS(message);
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private void sendCurrentLocationToServer() {
		Location loc = getCurrentLocation();
		String lat = "" + loc.getLatitude();
		String lng = "" + loc.getLongitude();

		String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.LOCATION.value() + " " + lat + " " + lng;
		sendServerSMS(message);
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private void sendBackErrorMessage( String msg ) {
		String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.ERROR_REPLY.value() + " " + msg;
		sendServerSMS(message);
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private Location getCurrentLocation() {
		LocationManager locationManager;
		String context = Context.LOCATION_SERVICE;

		locationManager = (LocationManager) ctx.getSystemService(context);

		Criteria crta = new Criteria();
		crta.setAccuracy(Criteria.ACCURACY_FINE);
		crta.setAltitudeRequired(false);
		crta.setBearingRequired(false);
		crta.setCostAllowed(true);
		crta.setPowerRequirement(Criteria.POWER_LOW);

		String provider = locationManager.getBestProvider(crta, true);
		// String provider = LocationManager.GPS_PROVIDER;

		Location bestLoc = locationManager.getLastKnownLocation(provider);
		return bestLoc;
	}


	/*****************************************************
    *
    *
    ******************************************************/
	public void startTimer( HashMap<String, String> params ) {
		timer = new Timer("EyeKeeperService");
		String timerIntervalString = (String) params.get("INTERVAL");
		int interval = 0;

		try {
			interval = Integer.parseInt(timerIntervalString);
		}
		catch (Exception e) {
			Log.e("EyeKeeperService", "Start Timer: Could not parse INTERVAL parameter" + timerIntervalString + "! Timer not started!!");
			sendBackErrorMessage("Start Timer: Could not parse INTERVAL parameter: " + timerIntervalString + ". Timer not started!!");
			return;
		}

		int interval_in_millis = 1000 * interval;
		timer.schedule(updateTask, 1000, interval_in_millis);
		timerRunning = true;
		Log.i("EyeKeeperService", "Timer has been started! Will run every " + interval + " seconds.");
	}


	/*****************************************************
    *
    *
    ******************************************************/
	public void stopTimer( String source ) {
		if ( timer != null ) {
			timer.cancel();
			timer = null;
		}
		timerRunning = false;
		if ( "Stop Request".equals(source) ) {
			String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.STOP_REC_ACTION.value() + " Timer Stopped.";
			sendServerSMS(message);
		}
	}


	/*****************************************************
    *
    *
    ******************************************************/
	public void sendServerSMS( String message ) {
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(AppVars.getInstance().getServerPhone(), null, message, null, null);
	}


	/*****************************************************
    *
    *
    ******************************************************/
	private HashMap<String, String> parseNameValues( ArrayList<String> nameValues ) {
		HashMap<String, String> newParms = new HashMap<String, String>();

		for (int i = 0; i < nameValues.size(); i++) {
			String nameValue = (String) nameValues.get(i);
			Log.i("Parser", "Parsing :" + nameValue);
			String delims = "[:]";
			String[] tokens = nameValue.split(delims);

			if ( tokens.length != 2 ) {
				Log.e("EyeKeeperService", "Error Parsing name:value. Looking at: [" + nameValue + "]");
			}
			else {
				newParms.put(tokens[0].toUpperCase(), tokens[1]);
			}
		}
		return newParms;
	}

}