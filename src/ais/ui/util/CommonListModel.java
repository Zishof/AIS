package ais.ui.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class CommonListModel {

	@SuppressWarnings({ "rawtypes" })
	public static List generateTahun() {

		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		List<Integer> list = new ArrayList<Integer>();
		list.add(cal.get(Calendar.YEAR) - 2);
		list.add(cal.get(Calendar.YEAR) - 1);
		list.add(cal.get(Calendar.YEAR));
		list.add(cal.get(Calendar.YEAR) + 1);
		list.add(cal.get(Calendar.YEAR) + 2);
		return list;
	}

}
