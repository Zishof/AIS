package ais.action.master.rab.helper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dialog ZKoss "Member Satuan Kerja": memilih member mana saja yang tergolong
 * dalam satu satuan kerja.
 *
 * <p>Seluruh datanya diambil dan disimpan lewat
 * {@code ais.action.servlet.api.SatuanKerjaKantinHelper} -- kelas yang SAMA
 * dipakai POS Desktop/Android dan halaman JSP. Dengan begitu aturan
 * penugasannya (member tanpa akun tetap bisa dipilih; akun ikut disamakan bila
 * ada) tidak mungkin berbeda antar kanal, dan tidak ada query kedua yang perlu
 * dijaga tetap sinkron.</p>
 */
public final class SatuanKerjaMemberZkDialog {

	private SatuanKerjaMemberZkDialog() {
	}

	/** Buka dialog untuk satu satuan kerja. */
	public static void buka(final SatuanKerja satuanKerja) throws Exception {
		if (satuanKerja == null || satuanKerja.getId() == null) {
			MyMessageboxConfig.show("Simpan satuan kerja ini dulu sebelum memilih member.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final Window win = new Window("Member — " + nvl(satuanKerja.getNama()), "normal", true);
		win.setWidth("620px");
		win.setHeight("560px");
		win.setClosable(true);
		win.setSizable(true);
		win.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());

		final Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setParent(win);

		new Label("Cari kode/nama member (tekan Enter):").setParent(isi);
		final Textbox cari = new Textbox();
		cari.setWidth("100%");
		cari.setTooltiptext("Ketik kode atau nama member, lalu tekan Enter");
		cari.setParent(isi);

		final Label ringkas = new Label("");
		ringkas.setParent(isi);

		final Div wadah = new Div();
		wadah.setWidth("100%");
		wadah.setHeight("380px");
		wadah.setStyle("overflow:auto;border:1px solid #e2e8f0;padding:6px");
		wadah.setParent(isi);

		// Pilihan disimpan TERPISAH dari daftar yang tampil: mencari ulang tidak
		// boleh menghapus centang yang belum sempat disimpan.
		final java.util.Map<String, Boolean> terpilih = new java.util.LinkedHashMap<String, Boolean>();

		final EventListener muat = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				wadah.getChildren().clear();
				JSONObject req = new JSONObject();
				req.put("satuan_kerja_id", String.valueOf(satuanKerja.getId()));
				req.put("cari", cari.getValue() == null ? "" : cari.getValue().trim());
				JSONObject hasil = new JSONObject();
				ais.action.servlet.api.SatuanKerjaKantinHelper.satuanKerjaAnggotaList(req, hasil);
				JSONArray data = hasil.optJSONArray("data");
				if (data == null || data.length() == 0) {
					new Label("Tidak ada member yang cocok.").setParent(wadah);
					perbarui(ringkas, terpilih);
					return;
				}
				for (int i = 0; i < data.length(); i++) {
					JSONObject m = data.getJSONObject(i);
					final String id = String.valueOf(m.opt("id"));
					if (!terpilih.containsKey(id) && m.optBoolean("terpilih", false)) {
						terpilih.put(id, Boolean.TRUE);
					}
					Hbox baris = new Hbox();
					baris.setWidth("100%");
					baris.setStyle("padding:3px 0;border-bottom:1px solid #f1f5f9");
					final Checkbox cb = new Checkbox();
					cb.setChecked(Boolean.TRUE.equals(terpilih.get(id)));
					cb.addEventListener(Events.ON_CHECK, new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							terpilih.put(id, Boolean.valueOf(cb.isChecked()));
							perbarui(ringkas, terpilih);
						}
					});
					cb.setParent(baris);

					StringBuilder ket = new StringBuilder();
					if (nvl(m.optString("kode", "")).length() > 0) {
						ket.append(m.optString("kode"));
					}
					if (!m.optBoolean("punyaAkun", false)) {
						if (ket.length() > 0) {
							ket.append("  ·  ");
						}
						ket.append("tanpa akun login");
					}
					String skLain = nvl(m.optString("satuanKerjaNama", ""));
					if (skLain.length() > 0 && !skLain.equals(nvl(satuanKerja.getNama()))) {
						if (ket.length() > 0) {
							ket.append("  ·  ");
						}
						ket.append("kini di: ").append(skLain);
					}
					Vbox teks = new Vbox();
					teks.setSpacing("0");
					new Label(nvl(m.optString("nama", ""))).setParent(teks);
					Label sub = new Label(ket.toString());
					sub.setStyle("font-size:11px;color:#64748b");
					sub.setParent(teks);
					teks.setParent(baris);
					baris.setParent(wadah);
				}
				perbarui(ringkas, terpilih);
			}
		};

		cari.addEventListener(Events.ON_CHANGE, muat);

		Label catatan = new Label(
				"Member yang dicentang menjadi anggota satuan kerja ini; yang dilepas centangnya dikeluarkan.");
		catatan.setStyle("font-size:11px;font-style:italic;color:#64748b");
		catatan.setParent(isi);

		Hbox tombol = new Hbox();
		tombol.setStyle("padding-top:8px");
		tombol.setParent(isi);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONArray ids = new JSONArray();
				for (java.util.Map.Entry<String, Boolean> e : terpilih.entrySet()) {
					if (Boolean.TRUE.equals(e.getValue())) {
						ids.put(e.getKey());
					}
				}
				JSONObject req = new JSONObject();
				req.put("satuan_kerja_id", String.valueOf(satuanKerja.getId()));
				req.put("anggota_id", ids);
				JSONObject hasil = new JSONObject();
				ais.action.servlet.api.SatuanKerjaKantinHelper.satuanKerjaAnggotaSimpan(req, hasil);
				win.detach();
				MyMessageboxConfig.show(hasil.optString("description", "Tersimpan."), "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		});
		simpan.setParent(tombol);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
			}
		});
		batal.setParent(tombol);

		muat.onEvent(null);
		win.doModal();
	}

	private static void perbarui(Label ringkas, java.util.Map<String, Boolean> terpilih) {
		int n = 0;
		for (Boolean v : terpilih.values()) {
			if (Boolean.TRUE.equals(v)) {
				n++;
			}
		}
		ringkas.setValue(n + " member dipilih");
	}

	private static String nvl(String v) {
		return v == null ? "" : v.trim();
	}
}
