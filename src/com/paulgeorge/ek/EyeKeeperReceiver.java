package com.paulgeorge.ek;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class EyeKeeperReceiver extends BroadcastReceiver {
	EyeKeeperService service;

    /*****************************************************
    *
    *
    *
    ******************************************************/
	@Override
	public void onReceive(Context context, Intent intent) {

		if ( "android.intent.action.BOOT_COMPLETED".equals( intent.getAction() ) ) {
			//When boot is completed start the service.
			Intent serviceIntent = new Intent();
			serviceIntent.setAction("com.paulgeorge.ek.EyeKeeperService");
			context.startService(serviceIntent);
		}
		else if ( "android.provider.Telephony.SMS_RECEIVED".equals( intent.getAction() ) ) {
			//---get the SMS message passed in---
			Bundle bundle = intent.getExtras();

			SmsMessage[] msgs = null;

			if (bundle != null) {

				//---retrieve the SMS message received---
				Object[] pdus = (Object[]) bundle.get("pdus");

				msgs = new SmsMessage[pdus.length];

				for ( int i=0; i < msgs.length; i++ ) {
					msgs[i] = SmsMessage.createFromPdu( (byte[])pdus[i] );

					String body = msgs[i].getMessageBody().toString();
					Log.i("EyeKeeperReceiver", "Received body[" + i + "]: " + body);

					//**********************************
					//*******   Activity Message *******
					//**********************************
					if ( body != null && body.startsWith( EyeKeeperActivity.MESSAGE_PREAMBLE) ) {
						Log.i("EyeKeeperReceiver","Got an Activity message.");
						//Stop any other BroadcastReceivers from seeing this message:
						this.abortBroadcast();

				        //Send a broadcast to the Activity to act upon this message
						Intent activityIntent = new Intent();
						activityIntent.setAction( EyeKeeperActivity.ACTIVITY_ACTION );
						activityIntent.putExtra( EyeKeeperActivity.MESSAGE_BODY, (CharSequence) body );
				        context.sendBroadcast( activityIntent );
					}
					//**********************************
					//*********  Service Message *******
					//**********************************
					else if ( body != null && body.startsWith( EyeKeeperService.MESSAGE_PREAMBLE ) ) {
						Log.i("EyeKeeperReceiver","Got a Service message.");
						//Stop any other BroadcastReceivers from seeing this message:
						this.abortBroadcast();
						EyeKeeperService.getInstance( context ).processMessage(body);
					}
				}
			}
		}
	}
}