package com.paulgeorge.ek;
  
import java.util.TreeMap;

enum Command {
	// Sendable Actions
	INIT_ACTION(0), PING(1), START_TRACKING(2), STOP_TRACKING(3), GET_UPTIME(4), GET_STARTTIME(5),

	// Receivable actions
	PING_REPLY(6), LOCATION(7), STOP_REC_ACTION(8), UPTIME_REPLY(9), STARTTIME_REPLY(10),

	// Errors
	ERROR_REPLY(99),
	
	//Messages
	SERVICE_STARTED(100), SERVICE_STOPPED(101);

	private int _value;


	Command(int value) {
		_value = value;
	}


	public int value() {
		return _value;
	}

	private static TreeMap<Integer, Command> _map;
	static {
		_map = new TreeMap<Integer, Command>();
		for (Command num : Command.values()) {
			_map.put(new Integer(num.value()), num);
		}
	}


	public static Command lookup( int value ) {
		return _map.get(new Integer(value));
	}
}