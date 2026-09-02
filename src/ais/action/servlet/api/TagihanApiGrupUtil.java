package ais.action.servlet.api;

import org.json.JSONObject;
import org.json.JSONException;

import ais.database.model.sekolah.GrupItemBiayaSekolah;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Tagihan;

/** Menjaga kontrak pengelompokan tagihan konsisten pada seluruh API sekolah. */
public final class TagihanApiGrupUtil {
	private TagihanApiGrupUtil() { }

	public static void putGrup(JSONObject json, Tagihan tagihan) {
		if (json == null || tagihan == null) return;
		try {
		GrupItemBiayaSekolah grup = tagihan.getItemBiayaSekolah() == null ? null
				: tagihan.getItemBiayaSekolah().getGrupItemBiayaSekolah();
		if (grup != null && grup.getId() != null && grup.getAktif()) {
			json.put("grup_id", grup.getId());
			json.put("grup_key", "item:" + grup.getId());
			json.put("grup_kode", grup.getKode());
			json.put("grup_nama", grup.getNama());
			json.put("grup_ta", grup.getLabelTampilan());
			json.put("grup_item_biaya_id", grup.getId());
			json.put("grup_item_biaya_aktif", true);
			return;
		}
		PengaturanBiaya pengaturan = tagihan.getPengaturanBiaya();
		if (pengaturan != null) {
			json.put("grup_id", pengaturan.getId());
			json.put("grup_key", "pengaturan:" + pengaturan.getId());
			json.put("grup_kode", pengaturan.getJenisBiayaSekolah() == null ? ""
					: pengaturan.getJenisBiayaSekolah().getKode());
			json.put("grup_nama", pengaturan.toString());
			json.put("grup_ta", pengaturan.getNama() + "-" + pengaturan.getTahunAjaran());
		}
		json.put("grup_item_biaya_aktif", false);
		} catch (JSONException e) {
			ais.common.ErrorAuditUtil.record(e, "TagihanApiGrupUtil.putGrup");
		}
	}
}
