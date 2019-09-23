package com.docusign.example.worker;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class DatePretty {
	
	/**
	 * Prints pretty the current day and time
	 * @return string of date and time
	 */
	public static String date() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		return formatter.format(date)+" ";	
	}
}
