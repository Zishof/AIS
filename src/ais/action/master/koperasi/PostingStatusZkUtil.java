package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Combobox;

/** Filter status bersama untuk seluruh halaman posting versi ZK. */
final class PostingStatusZkUtil {

	static final String SEMUA = "SEMUA";
	static final String SUDAH = "SUDAH_DIPOSTING";
	static final String BELUM = "BELUM_DIPOSTING";

	private PostingStatusZkUtil() {
	}

	static Combobox buatFilter(EventListener listener) {
		Combobox cb = new Combobox();
		cb.setReadonly(true);
		cb.setWidth("210px");
		tambah(cb, "Semua", SEMUA);
		tambah(cb, "Telah Diposting", SUDAH);
		tambah(cb, "Belum Diposting", BELUM);
		cb.setSelectedIndex(0);
		if (listener != null) {
			cb.addEventListener("onSelect", listener);
		}
		return cb;
	}

	private static void tambah(Combobox cb, String label, String value) {
		Comboitem item = new Comboitem(label);
		item.setValue(value);
		cb.appendChild(item);
	}

	static String nilai(Combobox cb) {
		if (cb == null || cb.getSelectedItem() == null
				|| cb.getSelectedItem().getValue() == null) {
			return SEMUA;
		}
		return cb.getSelectedItem().getValue().toString();
	}

	static List<JSONObject> gabungkan(List<JSONObject> draf, JSONArray riwayat,
			String filter) {
		List<JSONObject> keluar = new ArrayList<JSONObject>();
		for (int i = 0; draf != null && i < draf.size(); i++) {
			JSONObject baris = draf.get(i);
			if (cocok(baris, filter)) {
				keluar.add(baris);
			}
		}
		for (int i = 0; riwayat != null && i < riwayat.length(); i++) {
			JSONObject baris = riwayat.optJSONObject(i);
			if (baris != null && cocok(baris, filter)) {
				keluar.add(baris);
			}
		}
		return keluar;
	}

	private static boolean cocok(JSONObject baris, String filter) {
		boolean sudah = baris.optBoolean("sudahDiposting", false);
		if (SUDAH.equals(filter)) {
			return sudah;
		}
		if (BELUM.equals(filter)) {
			return !sudah;
		}
		return true;
	}
}
