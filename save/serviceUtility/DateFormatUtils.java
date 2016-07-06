/*
**  Copyright (c) 2016, Cascade Computer Consulting.
**
**  Permission to use, copy, modify, and/or distribute this software for any
**  purpose with or without fee is hereby granted, provided that the above
**  copyright notice and this permission notice appear in all copies.
**
**  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
**  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
**  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
**  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
**  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
**  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
**  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package com.ccc.tools.app.serviceUtility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for formatting dates and times.
 */
public final class DateFormatUtils {

	private static final String utcFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private DateFormatUtils() {
		// prevent construction
	}

	/**
	 * Format a Date into a String, according to the format specified by RFC 3339, and conforming to the ISO 8601 complete date and time extended format in UTC.
	 * @param date The date to be formatted. Must not be null.
	 * @return String representing the formatted Date.
	 */
	public static String formatUTC(Date date) {
	    return org.apache.commons.lang3.time.DateFormatUtils.formatUTC(date, utcFormat);
	}
	
	/**
	 * Format a millisecond UTC time into a String, according to the format specified by RFC 3339, and conforming to the ISO 8601 complete date and time extended format in UTC.
	 * @param date The millisecond UTC time to be formatted. Must not be null.
	 * @return String representing the formatted Date.
	 */
	public static String formatUTC(long date) {
	    return org.apache.commons.lang3.time.DateFormatUtils.formatUTC(date, utcFormat);
	}
	
	/**
	 * Parse a Date from a String, according to the format specified by RFC 3339, and conforming to the ISO 8601 complete date and time extended format in UTC.
	 * @param date The string representing date to be formatted. Assumes the same format as produced by @link{formatUTC}. Must not be null.
	 * @return Date The parsed Date.
	 * @exception ParseException if the string is in an unrecognizable format.
	 */
	public static Date parseUTC(String date) throws ParseException {
		// Since SimpleDateFormat is not thread-safe, we instantiate a new one for each conversion.
		SimpleDateFormat df = new SimpleDateFormat(utcFormat);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.parse(date);
	}

	/**
	 * Retrieve the standard UTC Format as specified by RFC 3339.
	 * @return The standard UTC Format as specified by RFC 3339.
	 */
	public static String getUTCFormat() {
		return utcFormat;
	}
}
