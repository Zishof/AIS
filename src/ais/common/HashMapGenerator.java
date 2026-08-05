package ais.common;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ais.action.master.surat.util.SuratUtil;

public class HashMapGenerator {

	/**
	 * CLASS BARU (Inner Class): NullSafeConcurrentHashMap
	 * Membungkus ConcurrentHashMap agar kebal terhadap NPE saat menerima null key / value.
	 */
	public static class NullSafeConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
		private static final long serialVersionUID = 1L;

		/**
		 * Override method put() bawaan.
		 * Jika ada kode lama yang mencoba memasukkan key=null atau value=null,
		 * proses ini akan diabaikan secara diam-diam.
		 */
		@Override
		public V put(K key, V value) {
			if (key == null || value == null) {
				// Di JasperReports, jika parameter tidak ditemukan di Map, nilainya otomatis null.
				// Jadi, mengabaikan proses put() di sini akan memberikan hasil akhir yang sama
				// nyamannya dengan menaruh null di HashMap biasa, namun 100% aman dari NPE.
				return null; 
			}
			return super.put(key, value);
		}

		/**
		 * Override method putAll() agar saat menggabungkan map lama,
		 * data yang mengandung null otomatis tersaring/dilewati.
		 */
		@Override
		public void putAll(Map<? extends K, ? extends V> m) {
			if (m == null) return;
			for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
				if (e.getKey() != null && e.getValue() != null) {
					super.put(e.getKey(), e.getValue());
				}
			}
		}
	}

	/**
	 * Private helper method untuk membuat Map baru. 
	 * Sekarang menggunakan custom map yang Thread-Safe sekaligus Null-Safe.
	 */
	@SuppressWarnings({ "rawtypes" })
	private static Map createNewMap() {
		// Menggunakan class custom yang kita buat di atas
		Map map = new NullSafeConcurrentHashMap();

		try {
			SuratUtil.initDefaultKop(map);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/HashMapGenerator.java:61");
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Serializable> getRandStringSerializable() {
		return (Map<String, Serializable>) createNewMap();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Serializable> getRandStringSerializable(Map<String, Serializable> parameters) {
		Map<String, Serializable> map = (Map<String, Serializable>) createNewMap();

		if (parameters != null) {
			// Karena kita sudah meng-override putAll di dalam class NullSafeConcurrentHashMap,
			// kita tidak perlu lagi melakukan looping manual di sini. Kodenya kembali bersih!
			map.putAll(parameters);
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getRandStringObject() {
		return (Map<String, Object>) createNewMap();
	}

	@SuppressWarnings("rawtypes")
	public static Map getRand() {
		return createNewMap();
	}

}