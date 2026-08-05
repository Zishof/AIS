package ais.action.master.sop.helper;

import java.util.Iterator;

import org.json.JSONObject;

import ais.database.model.sop.DisposisiSop;

/**
 * <h3>SopKodeUtil — penjaga konsistensi kode pengajuan SOP</h3>
 *
 * <p><b>Untuk apa:</b> memastikan kolom {@code kode} pada {@link DisposisiSop} SELALU sama dengan
 * kode milik objek (class transaksi) yang disimpan di JSON properti — baik di
 * {@code DisposisiSop.properti} maupun {@code DisposisiAlurSop.properti}. Dengan begitu kode
 * pengajuan (mis. {@code 0091/BAST/YTB/FAS/VI/2026}) PASTI tampil di semua tampilan modul SOP
 * (dasbor, grid workflow, kartu langkah) karena semuanya membaca {@code DisposisiSop.getKode()}
 * (dan {@code DisposisiAlurSop.getKode()} yang meneruskan ke {@code disposisiSop.getKode()}).</p>
 *
 * <p><b>Cara pakai:</b> panggil {@link #sinkronkanKode(DisposisiSop)} TEPAT setelah
 * {@code disposisiSop.setProperti(...)} dan sebelum penyimpanan (save/update/refreshUpdate) di
 * setiap alur yang membangun properti. Bersifat idempoten &amp; aman: hanya menulis kode bila
 * ditemukan nilai non-kosong di properti dan berbeda dari kode yang tersimpan.</p>
 *
 * <p><b>Pemeliharaan:</b> kompatibel Java 1.6/1.7 (tanpa lambda/stream/diamond). Memakai
 * {@code org.json.JSONObject} yang sudah dipakai modul SOP.</p>
 */
public final class SopKodeUtil {

	/** Field yang dipindai sebagai kode/nomor pengajuan (urut prioritas). */
	private static final String[] FIELD_KODE = new String[] { "kode", "nomor", "nomorSurat", "noSurat", "no_surat",
			"kodeTransaksi" };

	private SopKodeUtil() {
	}

	/**
	 * Pindai properti JSON dan kembalikan kode/nomor pertama yang tidak kosong. Memeriksa field di
	 * LEVEL ATAS lalu di SETIAP objek transaksi bersarang (struktur properti berbeda antar jenis SOP).
	 *
	 * @return kode pengajuan, atau {@code null} bila tidak ditemukan.
	 */
	public static String ambilKodeDariProperti(String propertiJson) {
		try {
			if (propertiJson == null || propertiJson.trim().isEmpty()) {
				return null;
			}
			JSONObject root = new JSONObject(propertiJson);
			String k = cariDiObjek(root);
			if (k != null) {
				return k;
			}
			Iterator<String> keys = root.keys();
			while (keys.hasNext()) {
				Object val = root.opt(keys.next());
				if (val instanceof JSONObject) {
					k = cariDiObjek((JSONObject) val);
					if (k != null) {
						return k;
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/SopKodeUtil.java:62");
			// properti tidak valid → tidak ada kode yang bisa diambil
		}
		return null;
	}

	private static String cariDiObjek(JSONObject obj) {
		for (int i = 0; i < FIELD_KODE.length; i++) {
			String f = FIELD_KODE[i];
			if (obj.has(f) && !obj.isNull(f)) {
				String k = obj.optString(f, "");
				if (k != null && !k.trim().isEmpty()) {
					return k.trim();
				}
			}
		}
		return null;
	}

	/**
	 * Sinkronkan {@code disposisiSop.kode} dengan kode pada propertinya (fallback: properti langkah
	 * START). Hanya menulis bila ditemukan kode non-kosong dan nilainya berbeda — sehingga aman
	 * dipanggil berulang dan tidak pernah mengosongkan kode yang sudah ada.
	 *
	 * @return kode hasil sinkron (atau kode lama bila tak ada perubahan).
	 */
	public static String sinkronkanKode(DisposisiSop disposisiSop) {
		try {
			if (disposisiSop == null) {
				return null;
			}
			String kode = ambilKodeDariProperti(disposisiSop.getProperti());
			if ((kode == null || kode.trim().isEmpty()) && disposisiSop.getDisposisiStart() != null) {
				kode = ambilKodeDariProperti(disposisiSop.getDisposisiStart().getProperti());
			}
			if (kode != null && !kode.trim().isEmpty()) {
				String sekarang = disposisiSop.getKode();
				if (sekarang == null || !kode.equals(sekarang.trim())) {
					disposisiSop.setKode(kode);
				}
				return kode;
			}
			return disposisiSop.getKode();
		} catch (Exception e) {
			return disposisiSop == null ? null : disposisiSop.getKode();
		}
	}
}
