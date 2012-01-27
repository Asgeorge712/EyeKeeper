package com.paulgeorge.ek;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.SmsManager;
import android.util.Log;

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

	private LocationManager locationManager;
	private MyLocationListener locListener;
	private Location currentLocation;
	private boolean listeningForLocationUpdates = false;

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
			setCurrentLocation(locListener.getBestCurrentLocation());
			sendCurrentLocationToServer(getCurrentLocation());
			Log.i("EyeKeeperService", "Timer task sent current location to: " + AppVars.getInstance().getServerPhone());
		}
	};


	/*****************************************************
	 * Constructor
	 * 
	 ******************************************************/
	protected EyeKeeperService(Context context) {
		this.ctx = context;
		AppVars.getInstance().setServerStartDate(new Date());
		Log.i("EyeKeeperService", "Service constructor is running.");
	}


	/*****************************************************
	 * 
	 * 
	 ******************************************************/
	public static EyeKeeperService getInstance( Context ctx ) {
		if ( instance == null ) {
			Log.i("EyeKeeperService", "EyeKeeper Service instance is null, starting new instance...");
			instance = new EyeKeeperService(ctx);
		}
		return instance;
	}


	/*****************************************************
	 * 
	 * 
	 *****************************************************/
	private void initLocationManager() {
		// Define a listener that responds to location updates
		try {
			if ( !listeningForLocationUpdates ) {
				locListener = new MyLocationListener();

				// Register the listener with the Location Manager to receive
				// location updates
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
				listeningForLocationUpdates = true;
			}
		}
		catch (Exception e) {
			Log.e("EyeKeeperService", "Exception occurred during initLocationManager: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/*****************************************************
	 * 
	 *
	 *****************************************************/
	private void destroyLocationManager() {
		// Remove the listener you previously added
		if ( listeningForLocationUpdates ) {
			locationManager.removeUpdates(locListener);
			locListener = null;
			listeningForLocationUpdates = false;
		}
		else {
			Log.e("EyeKeeperService", "Request to destroy Location Manager when none is set up.");
		}

	}


	/************************************************
	 * 
	 * @param msg
	 * @return
	 ************************************************/
	private String extractCommand( String msg ) {
		Log.i("EyeKeeperService", "extractCommand received message: " + msg);
		if ( msg == null || msg.length() == 0 ) {
			Log.w("EyeKeeperService", "extractCommand received a blank msg string.");
			return null;
		}
		int firstSpace = msg.indexOf(" ");

		if ( firstSpace <= 0 ) {
			return null;
		}

		String cmd = msg.substring(firstSpace + 1, firstSpace + 2);
		Log.d("EyeKeeperService", "substring =  " + cmd);

		if ( cmd == null || cmd.length() == 0 )
			return null;
		return cmd;
	}


	/**********************************************************
	 * 
	 * 
	 * @param msg
	 * @return
	 *****************************************************/
	private HashMap<String, String> getParams( String msg ) {
		HashMap<String, String> params = new HashMap<String, String>();

		ArrayList<String> nameValues = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(msg, " ");

		// Skip the preamble
		if ( tok.hasMoreTokens() )
			tok.nextToken();

		// Skip the command.
		if ( tok.hasMoreTokens() )
			tok.nextToken();

		while (tok.hasMoreTokens()) {
			String nextToken = (String) tok.nextToken();
			nameValues.add(nextToken);
		}

		if ( nameValues.size() > 0 ) {
			params = parseNameValues(nameValues);
		}

		return params;
	}


	/*****************************************************
	 * Service has received a BroadcastIntent from EyeKeeperReceiver
	 * 
	 ******************************************************/
	public void processMessage( String msg ) {
		AppVars vars = AppVars.getInstance();
		Log.i("EyeKeeperService", "Processing Message, body is: " + msg);

		// A message will have: A preamble, A command, and Zero or more command parameters
		String command = extractCommand(msg);

		Log.i("EyeKeeperService", "Command received: " + Command.lookup(Integer.parseInt(command)) );

		// Look at the command and decide what to do....
		if ( command == null || command.length() == 0 ) {
			Log.e("EyeKeeperService", "No Command was in the text message!");
			return;
		}

		// Convert Command to an int
		int command_int;
		try {
			command_int = Integer.parseInt(command);
		}
		catch (Exception e) {
			Log.e("EyeKeeperService", "Command: " + command + " could not be turned into an int.");
			command_int = -1;
		}

		HashMap<String, String> params = getParams(msg);

		// ************************************
		if ( params.containsKey("REPLY") ) {
			vars.setServerPhone(params.get("REPLY"));
		}

		// ***********************************************
		if ( !vars.validServerPhone() ) {
			Log.e("EyeKeeperService", "Server Ph Num is blank! Initialization ERROR!!");
			return;
		}

		// ***********************************************
		Command cmd = Command.lookup(command_int);

		if ( cmd == null ) {
			Log.e("EyeKeeperService", "Command received is null, doing nothing...");
			return;
		}

		switch (cmd) {
			case PING:
				sendPingReplyToServer();
				break;
				
			case SERVICE_STARTED:
				Log.i("EyeKeeperService","Service Started.");
				break;
				
			case SERVICE_STOPPED:
				Log.i("EyeKeeperService","Service Stopped.");
				break;
				
			case START_TRACKING:
				Log.i("Service", "Received request to start tracking thread.");
				if ( !timerRunning ) {
					Log.i("Service", "Timer is not running, so yay, kick off thread");
					initLocationManager();
					startTimer(params);
				}
				else {
					Log.i("Service", "Received request to start the Timer thread, but it seems to be already running.");
				}
				break;

			case STOP_TRACKING:
				if ( timerRunning ) {
					stopTimer("Stop Request");
					destroyLocationManager();
				}
				else {
					Log.w("EyeKeeperService", "Received request to stop location timer, but it doesn't appear to be running.");
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
		Date startDate = AppVars.getInstance().getServerStartDate();
		long diff = 0;
		if ( startDate != null ) {
			diff = currDate.getTime() - startDate.getTime();
		}

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
	private void sendCurrentLocationToServer( Location loc ) {
		String lat = "" + loc.getLatitude();
		String lng = "" + loc.getLongitude();

		String message = EyeKeeperActivity.MESSAGE_PREAMBLE + " " + Command.LOCATION.value() + " " + lat + " " + lng;
		Log.i("SERVICE", "Sending message to server: " + message);
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
		if ( timer != null ) {
			timer.cancel();
			timer = null;
		}
		timer = new Timer("EyeKeeperService");
		String timerIntervalString = (String) params.get("INTERVAL");
		int interval = 0;

		try {
			interval = Integer.parseInt(timerIntervalString);
			Log.i("Service", "Timer interval set at: " + interval + " Seconds.");
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


	/*****************************************************
    *
    *
    ******************************************************/
	public Location setCurrentLocation() {
		return this.currentLocation;
	}


	/*****************************************************
    *
    *
    ******************************************************/
	public void setCurrentLocation( Location currentLocation ) {
		this.currentLocation = currentLocation;
	}

}