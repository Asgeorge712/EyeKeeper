/*****************************************
 * 
 * @author Paul George  
 * 
 *****************************************/
package com.paulgeorge.ek;

import java.util.Date;

public class AppVars {
	private static AppVars instance;
	private String clientPhone;
	private String serverPhone;
	private String clientNumberDefault = "7033808765";
	private Date serverStartDate;
	private long trackingInterval = 10;
	
	protected AppVars() {
		
	}
	
	public static AppVars getInstance() {
		
		if ( instance == null ) {
			instance = new AppVars();
		}
		return instance;
	}
	
	public boolean validServerPhone() {
		if ( serverPhone == null ) return false;
		if ( serverPhone.length() < 10 ) return false;
		return true;
	}
	
	public String getServerPhone() {
		return serverPhone;
	}

	public void setServerPhone( String serverPhone ) {
		this.serverPhone = serverPhone;
	}
	
	public String getClientPhone() {
		return clientPhone;
	}

	public void setClientPhone( String clientPhone ) {
		this.clientPhone = clientPhone;
	}

	public String getClientNumberDefault() {
		return clientNumberDefault;
	}

	public void setClientNumberDefault( String clientNumberDefault ) {
		this.clientNumberDefault = clientNumberDefault;
	}

	public Date getServerStartDate() {
		return serverStartDate;
	}

	public void setServerStartDate( Date serverStartDate ) {
		this.serverStartDate = serverStartDate;
	}

	public long getTrackingInterval() {
		return trackingInterval;
	}

	public void setTrackingInterval( long trackingInterval ) {
		this.trackingInterval = trackingInterval;
	}
	
}
