package ais.ui.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Kumpulan utilitas statis untuk membangun daftar (list) nilai siap pakai sebagai sumber data
 * komponen pilihan ZK (mis. combobox tahun) di berbagai layar AIS. Saat ini hanya berisi satu
 * metode pembangkit daftar tahun di sekitar tahun berjalan.
 */
public class CommonListModel {

	/**
	 * Membangun daftar lima tahun berurutan berpusat pada tahun kalender saat ini (via
	 * {@link WaktuUtil#getCalendar()}, sehingga menghormati kemungkinan tanggal simulasi
	 * aplikasi): dua tahun sebelum, tahun berjalan, dan dua tahun sesudah. Umumnya dipakai
	 * sebagai sumber data combobox pemilihan tahun akademik/anggaran.
	 *
	 * @return daftar {@link Integer} berisi lima tahun berurutan, urut dari yang paling lampau
	 *         ke yang paling akan datang
	 */
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
