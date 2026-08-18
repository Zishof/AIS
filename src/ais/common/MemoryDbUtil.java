package ais.common;

import java.util.Map;

import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.Tagihan;

public class MemoryDbUtil {

	private static Map<String, Konfigurasi> mapkonfigurasi = null;

	public static synchronized void resetLocalReferences() {
		mapkonfigurasi = null;
		udahDataClass = null;
		bahasaIndonesias = null;
		bahasaEnglishs = null;
		bahasaArabs = null;
		bahasaMandarins = null;
		dataKey = null;
		allTagihan = null;
		allTagihanSudah = null;
		tanpaSpasi = null;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Konfigurasi> getKonfigurasi() {
		if (mapkonfigurasi == null) {
			mapkonfigurasi = (Map<String, Konfigurasi>) MemoryCacheUtil.get("mapkonfigurasi");
		}
		return mapkonfigurasi;
	}

	private static Map<String, String> udahDataClass = null;
	private static Map<String, String> bahasaIndonesias = null;
	private static Map<String, String> bahasaEnglishs = null;
	private static Map<String, String> bahasaArabs = null;
	private static Map<String, String> bahasaMandarins = null;
	private static Map<String, String> dataKey = null;

	private static Map<String, Tagihan> allTagihan = null;
	private static Map<Long, Boolean> allTagihanSudah = null;
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> getDataClass() {
		if (udahDataClass == null) {
			udahDataClass = (Map<String, String>) MemoryCacheUtil.get("udahDataClass");
		}
		return udahDataClass;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Tagihan> getAllTagihan() {
		if (allTagihan == null) {
			allTagihan = (Map<String, Tagihan>) MemoryCacheUtil.get("allTagihan");
		}
		return allTagihan;
	}

	@SuppressWarnings("unchecked")
	public static Map<Long, Boolean> getAllTagihanSudah() {
		if (allTagihanSudah == null) {
			allTagihanSudah = (Map<Long, Boolean>) MemoryCacheUtil.get("allTagihanSudah");
		}
		return allTagihanSudah;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaIndonesias() {
		if (bahasaIndonesias == null) {
			bahasaIndonesias = (Map<String, String>) MemoryCacheUtil.get("bahasaIndonesias");
		}
		return bahasaIndonesias;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaEnglishs() {
		if (bahasaEnglishs == null) {
			bahasaEnglishs = (Map<String, String>) MemoryCacheUtil.get("bahasaEnglishs");
		}
		return bahasaEnglishs;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaArabs() {
		if (bahasaArabs == null) {
			bahasaArabs = (Map<String, String>) MemoryCacheUtil.get("bahasaArabs");
		}
		return bahasaArabs;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getBahasaMandarins() {
		if (bahasaMandarins == null) {
			bahasaMandarins = (Map<String, String>) MemoryCacheUtil.get("bahasaMandarins");
		}
		return bahasaMandarins;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getDataKey() {
		if (dataKey == null) {
			dataKey = (Map<String, String>) MemoryCacheUtil.get("data_temp");
		}
		return dataKey;
	}

	private static Boolean tanpaSpasi = null;

	public static boolean apakahTanpaSpasi() {
		if (tanpaSpasi == null) {
			tanpaSpasi = Common.bolehKonfigurasi("matakuliah_tanpa_spasi");
		}
		return tanpaSpasi;
	}
}