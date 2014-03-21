package restaurant;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.Getter;
import au.com.bytecode.opencsv.CSVReader;

public class Restaurant {

	/*
	 * Restaurant hours CSV file has format:
	 *   line: name, hours
	 *   hours: entry |                    (one or more entries, / delimited)
	 *          entry / entry / ...
	 *   entry: day hour-hour |            (currently only parsing these four patterns)
	 *          day-day hour-hour |
	 *          day,day-day hour-hour |
	 *          day-day,day hour-hour
	 *   day: Mon | Tue | Wed | Thu | Fri | Sat | Sun  (ordered -- Mon is first, Sun is last)
	 *   hour: [12|1|2|...|11]:[0..59] [am|pm]
	 * 
	 * Read each line from file. If date specified matches any of the corresponding hours, 
	 * add that restaurant to the list of open restaurants.  Return list.
	 */
	public List<String> find_open_restaurants(final String csv_filename, final Date date)
			throws IOException {

		@Cleanup final InputStream is = new FileInputStream(csv_filename);
		@Cleanup final CSVReader reader = new CSVReader(new InputStreamReader(is), ',', '"');

		final List<String> openRestaurants = new ArrayList<String>();		
		String[] nextLine;

		while ((nextLine = reader.readNext()) != null) {
			if (nextLine != null) {
				for(final String entry : nextLine[1].split("/")) {
					if (isOpen(date, entry.trim())) {
						openRestaurants.add(nextLine[0]);
						break;
					}
				}
			}
		}
		return openRestaurants;
	}

	/*
	 * Return true if date occurs in days/hours specified by entry:
	 *   - convert date from Data to internal representation DayTime
	 *   - parse day portion of string, convert to DayInterval list (handles multiple intervals)
	 *   - parse hour portion of string, convert to TimeInterval (assumes one interval)
	 */
	private boolean isOpen(final Date date, final String entry) {

		final Matcher matcher = Pattern.compile("\\d").matcher(entry);
		matcher.find();
		final int firstDigit = matcher.start(); 
		final String days = entry.substring(0, firstDigit).trim();
		final String times = entry.substring(firstDigit, entry.length()).trim();

		final List<DayInterval> dis = parseDays(days);
		final TimeInterval ti = parseTimes(times);

		// iterate through all day/time combinations, return true as soon as any match
		for (final DayInterval di : dis) {
			final DayTimeIntervals dti = new DayTimeIntervals(di, ti);
			if (isOpen(dateToDayTime(date), dti)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Internal representation of days:
	 *   Each week has 7 days, 0..6.  0 for Mon, 1 for Tue, ..., 6 for Sun.
	 *   NOTES:
	 *     - This code assumes CSV file always uses this order.  e.g. day intervals will always
	 *       consist of the first day < last day (Mon-Sun is ok, Sun-Mon is invalid)
	 *     - Java Calendar encodes days of the week differently: 1 for Sun, 2 for Mon, ... 7 for
	 *       Sat.  This code translates from this to internal representation when needed.
	 *       
	 * Internal representation of times:
	 *   Each day has 1440 minutes, 0-1439.  0 for midnight, 60 for 1am, 120 for 2am, etc.  The
	 *   final value 1439 corresponds to 11:59pm.
	 */
	@AllArgsConstructor
	private enum DOW {
		Mon(0), Tue(1), Wed(2), Thu(3), Fri(4), Sat(5), Sun(6);
		@Getter private int val;
	}

	@Data
	@AllArgsConstructor
	private class DayTime {
		private Integer day;
		private Integer time;
	}

	@Data
	@AllArgsConstructor
	private class DayTimeIntervals {
		private DayInterval day;
		private TimeInterval time;
	}

	// convert Java date to internal representation
	private DayTime dateToDayTime(final Date date) {

		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		final int dow = cal.get(Calendar.DAY_OF_WEEK);
		final int hour = cal.get(Calendar.HOUR_OF_DAY);
		final int minute = cal.get(Calendar.MINUTE);

		// convert day from Java encoding to internal representation encoding and compute minutes
		return new DayTime((dow + 5) % 7, hour * 60 + minute);
	}

	// 	check if day/time dt is in corresponding day/time interval dti
	private boolean isOpen(final DayTime dt, final DayTimeIntervals dti) {

		// check original day/time values
		Integer day = dt.getDay();
		final Integer fromDay = dti.getDay().getFrom();
		final Integer toDay = dti.getDay().getTo();

		Integer time = dt.getTime();
		final Integer fromTime = dti.getTime().getFrom();
		final Integer toTime = dti.getTime().getTo();

		if (fromDay <= day && day <= toDay
				&& fromTime <= time && time < toTime) {
			return true;
		}

		// handle wrap-around -- convert day/time to previous day + day's worth of minutes
		day = (day + 6) % 7; 
		time += 1440;
		return (fromDay <= day && day <= toDay 
				&& fromTime <= time && time < toTime);
	}

	/*
	 * Days in CSV file are specified by either a single day (Mon) or a range (Sat-Sun).  As
	 * well, there can be comma separated list of these (Mon,Sat-Sun).  Parsing these strings
	 * and converting to the internal representation makes these assumptions:
	 *   - Currently code only parses the four specific patterns encountered in the CSV file:
	 *     i.e. "Mon", "Sat-Sun", "Mon,Sat-Sun" and "Mon-Sat,Sun".  This is much simpler
	 *     than implementing a more general parser.  A count verifies every pattern is
	 *     matched exactly one time (currently prints out error message, could throw exception).
	 *   - For uniformity, single days are also represented as an interval, e.g. Sun -> Sun-Sun
	 */
	@Data
	@AllArgsConstructor
	private class DayInterval {
		private Integer from;
		private Integer to;
	}

	private List<DayInterval> parseDays(final String days) {
		int count = 0;
		final List<DayInterval> dayList = new ArrayList<DayInterval>();

		final Matcher oneDay = Pattern.compile("^(\\w+)$").matcher(days);
		final Matcher range = Pattern.compile("^(\\w+)-(\\w+)$").matcher(days);
		final Matcher rangeDay = Pattern.compile("^(\\w+)-(\\w+), (\\w+)$").matcher(days);
		final Matcher dayRange = Pattern.compile("^(\\w+), (\\w+)-(\\w+)$").matcher(days);

		if (oneDay.find()) {
			final String day = oneDay.group(1);
			dayList.add(new DayInterval(DOW.valueOf(day).getVal(), DOW.valueOf(day).getVal()));
			count++;
		}	
		if (range.find()) {
			final String from = range.group(1);
			final String to = range.group(2);
			dayList.add(new DayInterval(DOW.valueOf(from).getVal(), DOW.valueOf(to).getVal()));
			count++;
		}	
		if (rangeDay.find()) {
			final String from = rangeDay.group(1);
			final String to = rangeDay.group(2);
			final String day = rangeDay.group(3);
			dayList.add(new DayInterval(DOW.valueOf(from).getVal(), DOW.valueOf(to).getVal()));
			dayList.add(new DayInterval(DOW.valueOf(day).getVal(), DOW.valueOf(day).getVal()));
			count++;
		}
		if (dayRange.find()) {
			final String day = dayRange.group(1);
			final String from = dayRange.group(2);
			final String to = dayRange.group(3);
			dayList.add(new DayInterval(DOW.valueOf(day).getVal(), DOW.valueOf(day).getVal()));
			dayList.add(new DayInterval(DOW.valueOf(from).getVal(), DOW.valueOf(to).getVal()));
			count++;
		} 

		// verify everything matches if/when restaurant file changes
		if (count != 1) {
			System.out.println("ERROR: " + days);
		}

		return dayList;
	}

	/*
	 * All time strings look something like this: "11:30 am - 9 pm" -- the only variation is
	 * that the minutes (:30) for either time is optional.  Converting to the internal 
	 * representation (0..1439) consists of parsing out the individual component of each time
	 * and performing the required math to handle the hours, minutes, and am/pm.  Although
	 * the CSV file currently only includes times on the hour or half hour (9pm, 9:30pm), this
	 * code does handle any minute value (e.g. 9:33).
	 */
	@Data
	@AllArgsConstructor
	private class TimeInterval {
		private Integer from;
		private Integer to;
	}

	private TimeInterval parseTimes(final String times) {

		final Matcher time = 
				Pattern.compile("^([\\d:]+) (\\w+) - ([\\d:]+) (\\w+)$").matcher(times);
		if(time.find()) {
			final String from = time.group(1);
			final Matcher fromMatch = Pattern.compile("^([\\d]+):?(.*)").matcher(from);
			fromMatch.find();
			final String fromHour = fromMatch.group(1);
			final String fromMin= fromMatch.group(2);
			final String fromAmPm = time.group(2);
			final String to = time.group(3);
			final Matcher toMatch = Pattern.compile("^([\\d]+):?(.*)").matcher(to);
			toMatch.find();
			final String toHour = toMatch.group(1);
			final String toMin= toMatch.group(2);
			final String toAmPm = time.group(4);
			return makeTimeInterval(fromHour, fromMin, fromAmPm, toHour, toMin, toAmPm);
		} else {
			System.out.format("NOT FOUND %s\n", times);
			return null;
		}
	}

	private TimeInterval makeTimeInterval(
			final String fromHour, final String fromMin, final String fromAmPm,
			final String toHour, final String toMin, final String toAmPm) {

		Integer fH = Integer.parseInt(fromHour);
		if (fH == 12) fH -= 12;
		if (fromAmPm.equals("pm")) fH += 12;
		final Integer fM = fromMin.isEmpty() ? 0 : Integer.parseInt(fromMin);

		Integer tH = Integer.parseInt(toHour);
		if (tH == 12) tH -= 12;
		if (toAmPm.equals("pm")) tH += 12;
		final Integer tM = toMin.isEmpty() ? 0 : Integer.parseInt(toMin);

		final Integer fromMinutes = fH * 60 + fM;
		Integer toMinutes = tH * 60 + tM;
		// If interval is negative, assume it wraps into next day.  Therefore, add a
		// day's worth of minutes to toMinutes value.
		if (toMinutes < fromMinutes) {
			toMinutes += 1440;
		}
		return new TimeInterval(fromMinutes, toMinutes);
	}

}
