package restaurant;

import static org.junit.Assert.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.Test;

public class RestaurantTest {

	/*
	 * The shortFile used in these tests contains:
	 *   "Kushi Tsuru","Mon, Wed-Sun 11:00 am - 9 pm"
	 *   "Osakaya Restaurant","Mon-Thu, Sun 11:30 am - 9 pm  / Fri-Sat 11:30 am - 9:30 pm"
	 *   "The Stinking Rose","Mon-Thu 12:00 pm - 11 pm  / Sun 12:00 pm - 10 pm"
	 *   
	 * The longFile is similar, but with 51 lines of data in it.
	 */

	final String shortFile = "/tmp/rest_hours_short.csv";
	final String longFile = "/tmp/rest_hours.csv";
	final Restaurant r = new Restaurant();
	final SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy h:m a", Locale.ENGLISH);

	// Test a Sunday time. Starts with all restaurants closed, ramps up to all open, and ramps
	// back down again to all closed
	@Test
	public void testSunday() throws ParseException, IOException {
		assertEquals(new String[]{ },
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 10:59 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru"},
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 11:00 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru"},
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 11:29 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant" },
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 11:30 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant" },
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 11:59 am")).toArray());		
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant", "The Stinking Rose"},
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 12:00 pm")).toArray());	
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant", "The Stinking Rose"},
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 8:59 pm")).toArray());
		assertEquals(new String[]{ "The Stinking Rose"},
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 9:00 pm")).toArray());
		assertEquals(new String[]{ "The Stinking Rose"},
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 9:59 pm")).toArray());
		assertEquals(new String[]{ },
				r.find_open_restaurants(shortFile, sdf.parse("March 16, 2014 10:00 pm")).toArray());
	}

	// Test a Saturday time.  Similar to Sunday, but with one of the restaurants never open.
	@Test
	public void testSaturday() throws ParseException, IOException {
		assertEquals(new String[]{ },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 10:59 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru"},
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 11:00 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru"},
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 11:29 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant" },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 11:30 am")).toArray());
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant" },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 11:59 am")).toArray());		
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant" },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 12:00 pm")).toArray());	
		assertEquals(new String[]{ "Kushi Tsuru", "Osakaya Restaurant" },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 8:59 pm")).toArray());
		assertEquals(new String[]{ "Osakaya Restaurant"},
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 9:00 pm")).toArray());
		assertEquals(new String[]{ },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 9:59 pm")).toArray());
		assertEquals(new String[]{ },
				r.find_open_restaurants(shortFile, sdf.parse("March 15, 2014 10:00 pm")).toArray());
	}

	// Test on full file (Saturday 10am)
	@Test
	public void testLong() throws IOException, ParseException {

		final String[] expected =  {
				"Canton Seafood & Dim Sum Restaurant", "All Season Restaurant", "Herbivore",
				"Penang Garden", "Blu Restaurant", "Sabella & La Torre", "Tong Palace", 
				"India Garden Restaurant", "Santorini's Mediterranean Cuisine" };
		assertEquals(expected,
				r.find_open_restaurants(longFile, sdf.parse("March 15, 2014 10:00 am")).toArray());
	}

	// Late night test (Fri night, around midnight).  Make sure time wraps around correctly
	// from Saturday morning to Friday night. 
	@Test
	public void testLate() throws IOException, ParseException {

		final String[] expected1 = {
				"The Cheesecake Factory", "Sudachi", "Hanuri", "Bamboo Restaurant", "Burger Bar",
				"Naan 'N' Curry", "Viva Pizza Restaurant", "Thai Stick Restaurant",
				"Sabella & La Torre", "Marrakech Moroccan Restaurant" };
		assertEquals(expected1,
				r.find_open_restaurants(longFile, sdf.parse("March 14, 2014 11:59 pm")).toArray());

		final String[] expected2 = {
				"The Cheesecake Factory", "Sudachi", "Naan 'N' Curry", "Thai Stick Restaurant",
				"Sabella & La Torre", "Marrakech Moroccan Restaurant" };
		assertEquals(expected2,
				r.find_open_restaurants(longFile, sdf.parse("March 15, 2014 12:00 am")).toArray());
		assertEquals(expected2,
				r.find_open_restaurants(longFile, sdf.parse("March 15, 2014 12:01 am")).toArray());
	}

	// Late night test (Sunday night, around midnight).  Make sure time wraps around 
	// correctly from morning to night, and day wraps around correctly from Monday to Sunday.
	@Test
	public void testLateWrap() throws IOException, ParseException {

		final String[] expected1 = {
				"Hanuri", "Bamboo Restaurant", "Naan 'N' Curry", "Viva Pizza Restaurant", 
				"Thai Stick Restaurant", "Marrakech Moroccan Restaurant" };
		assertEquals(expected1,
				r.find_open_restaurants(longFile, sdf.parse("March 16, 2014 11:59 pm")).toArray());


		final String[] expected2 = {
				"Naan 'N' Curry", "Thai Stick Restaurant", "Marrakech Moroccan Restaurant" };
		assertEquals(expected2,
				r.find_open_restaurants(longFile, sdf.parse("March 17, 2014 12:00 am")).toArray());
		assertEquals(expected2,
				r.find_open_restaurants(longFile, sdf.parse("March 17, 2014 12:01 am")).toArray());
	}

	// Rod's time -- Sunday 1am (late Sat. night)
	@Test
	public void testRodsTime() throws IOException, ParseException {
		final String[] expected1 = {
				"Sudachi", "Naan 'N' Curry", "Thai Stick Restaurant", "Marrakech Moroccan Restaurant" };
		assertEquals(expected1,
				r.find_open_restaurants(longFile, sdf.parse("March 16, 2014 12:59 am")).toArray());
		final String[] expected2 = {
				"Sudachi", "Naan 'N' Curry", "Marrakech Moroccan Restaurant" };
		assertEquals(expected2,
				r.find_open_restaurants(longFile, sdf.parse("March 16, 2014 1:00 am")).toArray());
	}
	
}
