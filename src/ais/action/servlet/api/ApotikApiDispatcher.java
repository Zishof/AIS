package ais.action.servlet.api;

import org.json.JSONObject;

import ais.database.model.Tbmuser;

/**
 * <h3>Dispatcher aksi {@code apotik_*} -- varian "POS Apotik" (FASE A).</h3>
 *
 * <p>Dipanggil dari {@code ApiEBisnis.prosesAksiTambahan} SETELAH dispatcher {@code si_}
 * (Inventory &amp; Sales) menolak aksinya -- pola dan kontrak return SAMA: {@code true} = sudah
 * ditangani (termasuk ditangani-dengan-error), {@code false} = bukan milik dispatcher ini.
 * Gerbang menu per-aksi sudah dijalankan LEBIH DULU oleh {@code PosApi.bolehAksesActionKantin}
 * (kunci {@code apotik_*} default FALSE -- fail-closed); di sini tinggal rute + normalisasi.</p>
 */
public final class ApotikApiDispatcher {

	private ApotikApiDispatcher() {
	}

	public static boolean dispatch(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
			throws Exception {
		if (action == null || !action.startsWith("apotik_")) {
			return false;
		}
		if ("apotik_item_cari".equals(action)) {
			ApotikApiHelper.itemCari(payload, hasil);
		} else if ("apotik_item_batch".equals(action)) {
			ApotikApiHelper.itemBatch(payload, hasil);
		} else if ("apotik_resep_list".equals(action)) {
			ApotikApiHelper.resepList(payload, hasil);
		} else if ("apotik_resep_detail".equals(action)) {
			ApotikApiHelper.resepDetail(payload, hasil);
		} else if ("apotik_item_profil_simpan".equals(action)) {
			ApotikApiHelper.itemProfilSimpan(tbmuser, payload, hasil);
		} else if ("apotik_bayar".equals(action)) {
			ApotikApiHelper.bayar(tbmuser, payload, hasil);
		} else if ("apotik_terima_barang".equals(action)) {
			ApotikPersediaanHelper.terimaBarang(tbmuser, payload, hasil);
		} else if ("apotik_opname_simpan".equals(action)) {
			ApotikPersediaanHelper.opnameSimpan(tbmuser, payload, hasil);
		} else if ("apotik_retur_simpan".equals(action)) {
			ApotikPersediaanHelper.returSimpan(tbmuser, payload, hasil);
		} else if ("apotik_batch_monitor".equals(action)) {
			ApotikPersediaanHelper.batchMonitor(payload, hasil);
		} else {
			hasil.put("status", "error");
			hasil.put("message", "Aksi POS Apotik belum tersedia di server ini: " + action);
			return true;
		}
		normalisasi(hasil);
		return true;
	}

	/** Seragamkan konvensi "00"/"91" ke status:"success"/"error" (paritas dispatcher si_). */
	private static void normalisasi(JSONObject hasil) throws Exception {
		String status = hasil.optString("status", "");
		if ("00".equals(status)) {
			hasil.put("status", "success");
		} else if (!"success".equals(status) && !"error".equals(status)) {
			hasil.put("statusAsli", status);
			hasil.put("status", "error");
			if (!hasil.has("message")) {
				hasil.put("message", hasil.optString("description", "Permintaan tidak dapat diproses."));
			}
		}
	}
}
