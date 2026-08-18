package ais.ui.util;

import java.text.DecimalFormat;

import org.json.JSONObject;

public class Test {

	public static void main(String[] argv) throws Exception {

		JSONObject name = new JSONObject();
		name.put("test", 22000000000000.0);
		
		name.put("test1", 3.74);
		name.put("test1", 0.4);
		JSONObject name1 = new JSONObject();
		
		
		
		name1.put("tets lagi", name);

		MyJSONObject MyJSONObject = new MyJSONObject(name1);

		String pattern = "###########################";
		DecimalFormat decimalFormat = new DecimalFormat(pattern);
		decimalFormat.setMaximumFractionDigits(6);
		decimalFormat.setMinimumIntegerDigits(1);

		String sss = decimalFormat.format(22000000000000.0);
		System.out.println("sss -> " + sss + " " + MyJSONObject.toString());

		sss = decimalFormat.format(0.4);
		System.out.println("sss -> " + sss + " " + MyJSONObject.toString());
		
		
		 sss = decimalFormat.format(3.74);
		System.out.println("sss -> " + sss + " " + MyJSONObject.toString());
		
//		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) - 1);
//		DateFormat dateFormat = new SimpleDateFormat("YYYY-ww", new Locale("in", "ID"));
//		System.out.println(dateFormat.format(calendar.getTime()));
//		System.out.println("10.00".replaceAll("\\.", ":"));
//		System.out.println(DriveScopes.DRIVE_FILE);
//		System.out.println(CalendarScopes.CALENDAR);
//
//		String d = URLDecoder.decode("%3D%3D", "UTF-8");
//		System.out.println("d -> " + d);
	}
}
