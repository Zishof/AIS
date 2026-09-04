package ais.action.servlet.api;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;

/**
 * Kontrak status bersama untuk seluruh halaman posting eBisnis.
 *
 * <p>Daftar draf dan riwayat sengaja dikirim terpisah supaya klien lama tetap
 * hanya membaca {@code rincian}, sementara klien baru dapat menggabungkannya
 * dengan {@code rincianSudahDiposting}. Setiap baris tetap mempunyai penanda
 * eksplisit agar status tidak ditebak dari ada/tidaknya tombol.</p>
 */
public final class PostingStatusUtil {

	public static final String BELUM_SIAP = "BELUM_DIPOSTING_SIAP";
	public static final String BELUM_TERTAHAN = "BELUM_DIPOSTING_TERTAHAN";
	public static final String SUDAH = "SUDAH_DIPOSTING";

	private PostingStatusUtil() {
	}

	/** Batas aman API: paling sedikit 100 dan paling banyak 10.000 baris. */
	public static int batasRiwayat(JSONObject payload) {
		return batasRiwayat(payload == null ? 500 : payload.optInt("batasRiwayat", 500));
	}

	/** Normalisasi batas yang diteruskan dari adapter API. */
	public static int batasRiwayat(int nilai) {
		if (nilai < 100) {
			return 100;
		}
		return Math.min(nilai, 10000);
	}

	/** Lengkapi baris pratinjau dengan status yang dapat dirender tanpa tebakan. */
	public static JSONObject tandaiBelum(JSONObject baris, boolean siap) {
		try {
			baris.put("sudahDiposting", false);
			baris.put("statusPosting", siap ? BELUM_SIAP : BELUM_TERTAHAN);
			baris.put("statusLabel", siap
					? "Belum Diposting - Siap"
					: "Belum Diposting - Tertahan");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingStatusUtil.tandaiBelum");
		}
		return baris;
	}

	/** Bentuk ringkas satu dokumen yang sudah memiliki jurnal buku besar. */
	public static JSONObject sudah(long id, String referensi, double nilai, Date tanggalSumber,
			long postingHistoryId, String nomorJurnal, Date tanggalPosting, String keterangan) {
		JSONObject baris = new JSONObject();
		try {
			String ref = referensi == null || referensi.trim().isEmpty()
					? ("Dokumen #" + id) : referensi.trim();
			baris.put("id", id);
			baris.put("ref", ref);
			baris.put("referensi", ref);
			baris.put("nilai", nilai);
			baris.put("tanggal", tanggalSumber == null ? ""
					: Common.dateFormat3.get().format(tanggalSumber));
			baris.put("siap", false);
			baris.put("sudahDiposting", true);
			baris.put("statusPosting", SUDAH);
			baris.put("statusLabel", "Sudah Diposting");
			baris.put("postingHistoryId", postingHistoryId);
			baris.put("nomorJurnal", nomorJurnal == null ? "" : nomorJurnal);
			baris.put("tanggalPosting", tanggalPosting == null ? ""
					: Common.dateFormat3.get().format(tanggalPosting));
			baris.put("keterangan", keterangan == null ? "" : keterangan);
			baris.put("alasan", "");
			baris.put("debet", "");
			baris.put("kredit", "");
			baris.put("jurnal", new JSONArray());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingStatusUtil.sudah");
		}
		return baris;
	}
}
