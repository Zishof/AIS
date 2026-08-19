package ais.action.master.helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Editor DAFTAR ATASAN mahasiswa/alumni (ZKoss). Menyimpan banyak atasan sebagai satu string JSON (array)
 * pada kolom {@code Mahasiswa.atasans}. Tiap atasan berisi: {@code nama, email, telp, alamat, peran,
 * masihAktif(boolean), tanggalMulai(yyyy-MM-dd), keterangan}.
 *
 * <p>Bersifat REUSABLE: dipakai di form edit Mahasiswa (tab Kelulusan) maupun kuesioner alumni versi ZK.
 * Cara pakai:</p>
 * <pre>
 *   AtasanMahasiswaHelper h = new AtasanMahasiswaHelper();
 *   h.render(container, mahasiswa.getAtasans());   // gambar tabel + tombol "Tambah Atasan"
 *   ...
 *   mahasiswa.setAtasans(h.serialize());           // saat SIMPAN
 * </pre>
 *
 * <p>Tidak menyentuh DB; hanya mengelola string JSON. Aman (defensif) terhadap JSON rusak.</p>
 */
public class AtasanMahasiswaHelper {

	/** Satu baris atasan (nilai sederhana; tanggal disimpan sebagai string yyyy-MM-dd). */
	public static class Atasan {
		public String nama = "";
		public String email = "";
		public String telp = "";
		public String alamat = "";
		public String peran = "";
		public boolean masihAktif = true;
		public String tanggalMulai = ""; // yyyy-MM-dd
		public String keterangan = "";
	}

	private final List<Atasan> daftar = new ArrayList<Atasan>();
	private Rows rows; // body tabel daftar atasan
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	/** Menggambar editor (tabel + tombol Tambah) ke {@code parent}, memuat data awal dari {@code jsonAwal}. */
	public void render(Component parent, String jsonAwal) {
		muatDariJson(jsonAwal);

		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setParent(parent);

		box.appendChild(new MyLabelConfig("Daftar Atasan / Pengguna Lulusan"));

		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(box);

		Columns columns = new Columns();
		columns.setParent(grid);
		String[] judul = { "Nama", "Peran", "Email", "Telp.", "Aktif", "" };
		String[] lebar = { "22%", "18%", "22%", "14%", "8%", "16%" };
		for (int i = 0; i < judul.length; i++) {
			MyColumnConfig col = new MyColumnConfig();
			col.setLabel(judul[i]);
			col.setWidth(lebar[i]);
			col.setParent(columns);
		}

		rows = new Rows();
		rows.setParent(grid);

		MyToolbarbuttonConfig btnTambah = new MyToolbarbuttonConfig("Tambah Atasan", "/img/svg/plus-circle.svg");
		btnTambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaPopup(-1);
			}
		});
		box.appendChild(btnTambah);

		refreshTabel();
	}

	/** Serialisasi daftar atasan menjadi JSON array (String). {@code null} bila kosong. */
	public String serialize() {
		try {
			if (daftar.isEmpty()) {
				return null;
			}
			org.json.JSONArray arr = new org.json.JSONArray();
			for (Atasan a : daftar) {
				org.json.JSONObject o = new org.json.JSONObject();
				o.put("nama", a.nama == null ? "" : a.nama);
				o.put("email", a.email == null ? "" : a.email);
				o.put("telp", a.telp == null ? "" : a.telp);
				o.put("alamat", a.alamat == null ? "" : a.alamat);
				o.put("peran", a.peran == null ? "" : a.peran);
				o.put("masihAktif", a.masihAktif);
				o.put("tanggalMulai", a.tanggalMulai == null ? "" : a.tanggalMulai);
				o.put("keterangan", a.keterangan == null ? "" : a.keterangan);
				arr.put(o);
			}
			return arr.toString();
		} catch (Exception e) {
			return null;
		}
	}

	private void muatDariJson(String json) {
		daftar.clear();
		daftar.addAll(parseList(json));
	}

	/**
	 * Mem-parse string JSON array atasan menjadi {@code List<Atasan>} (static, reusable — mis. untuk broadcast
	 * email ke atasan). Mengembalikan list kosong bila null/rusak (aman).
	 */
	public static List<Atasan> parseList(String json) {
		List<Atasan> hasil = new ArrayList<Atasan>();
		if (json == null || json.trim().isEmpty()) {
			return hasil;
		}
		try {
			org.json.JSONArray arr = new org.json.JSONArray(json.trim());
			for (int i = 0; i < arr.length(); i++) {
				org.json.JSONObject o = arr.optJSONObject(i);
				if (o == null) {
					continue;
				}
				Atasan a = new Atasan();
				a.nama = o.optString("nama", "");
				a.email = o.optString("email", "");
				a.telp = o.optString("telp", "");
				a.alamat = o.optString("alamat", "");
				a.peran = o.optString("peran", "");
				a.masihAktif = o.optBoolean("masihAktif", true);
				a.tanggalMulai = o.optString("tanggalMulai", "");
				a.keterangan = o.optString("keterangan", "");
				hasil.add(a);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AtasanMahasiswaHelper.java:162");
			// JSON rusak -> list kosong (aman)
		}
		return hasil;
	}

	private void refreshTabel() {
		if (rows == null) {
			return;
		}
		Common.clear(rows);
		for (int i = 0; i < daftar.size(); i++) {
			final int idx = i;
			final Atasan a = daftar.get(i);
			Row row = new Row();
			row.setValign("middle");
			row.setParent(rows);

			row.appendChild(new Label(a.nama));
			row.appendChild(new Label(a.peran));
			row.appendChild(new Label(a.email));
			row.appendChild(new Label(a.telp));
			row.appendChild(new Label(a.masihAktif ? "Ya" : "Tidak"));

			Hbox aksi = new Hbox();
			MyToolbarbuttonConfig btnUbah = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit.svg");
			btnUbah.setTooltiptext("Ubah atasan ini");
			btnUbah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					bukaPopup(idx);
				}
			});
			aksi.appendChild(btnUbah);

			MyToolbarbuttonConfig btnHapus = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			btnHapus.setTooltiptext("Hapus atasan ini");
			btnHapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (idx >= 0 && idx < daftar.size()) {
						daftar.remove(idx);
						refreshTabel();
					}
				}
			});
			aksi.appendChild(btnHapus);
			row.appendChild(aksi);
		}
	}

	/** Buka popup form tambah/ubah. {@code editIndex < 0} berarti TAMBAH baru. */
	private void bukaPopup(final int editIndex) throws Exception {
		final Atasan a = (editIndex >= 0 && editIndex < daftar.size()) ? daftar.get(editIndex) : new Atasan();

		final MyWindow win = new MyWindow();
		win.setTitle(editIndex < 0 ? "Tambah Atasan" : "Ubah Atasan");
		win.setBorder("normal");
		win.setClosable(true);
		win.setWidth("540px");

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;");
		isi.setParent(win);

		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(isi);
		Columns cols = new Columns();
		cols.setParent(grid);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("35%");
		c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(cols);
		Rows formRows = new Rows();
		formRows.setParent(grid);

		final MyTextbox tNama = tambahBarisTeks(formRows, "Nama Atasan", a.nama);
		final MyTextbox tEmail = tambahBarisTeks(formRows, "Email Atasan", a.email);
		final MyTextbox tTelp = tambahBarisTeks(formRows, "Telp. Atasan", a.telp);
		final MyTextbox tAlamat = tambahBarisTeks(formRows, "Alamat Atasan", a.alamat);
		final MyTextbox tPeran = tambahBarisTeks(formRows, "Peran Atasan Sebagai", a.peran);

		ais.ui.util.MyFormRow rAktif = new ais.ui.util.MyFormRow();
		rAktif.setParent(formRows);
		rAktif.appendChild(new MyLabelConfig("Saat ini masih aktif"));
		final Checkbox cAktif = new Checkbox();
		cAktif.setChecked(a.masihAktif);
		rAktif.appendChild(cAktif);

		ais.ui.util.MyFormRow rTgl = new ais.ui.util.MyFormRow();
		rTgl.setParent(formRows);
		rTgl.appendChild(new MyLabelConfig("Tanggal Mulai Jadi Atasan"));
		final MyDatebox dMulai = new MyDatebox();
		dMulai.setWidth("90%");
		try {
			if (a.tanggalMulai != null && !a.tanggalMulai.trim().isEmpty()) {
				dMulai.setValue(sdf.parse(a.tanggalMulai.trim()));
			}
		} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/AtasanMahasiswaHelper.java:263");
		}
		rTgl.appendChild(dMulai);

		ais.ui.util.MyFormRow rKet = new ais.ui.util.MyFormRow();
		rKet.setValign("top");
		rKet.setParent(formRows);
		rKet.appendChild(new MyLabelConfig("Keterangan Tambahan"));
		final Textbox tKet = new Textbox(a.keterangan == null ? "" : a.keterangan);
		tKet.setMultiline(true);
		tKet.setRows(3);
		tKet.setWidth("90%");
		rKet.appendChild(tKet);

		Hbox tombol = new Hbox();
		tombol.setStyle("margin-top:8px;");
		tombol.setParent(isi);

		MyToolbarbuttonConfig btnSimpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		btnSimpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				a.nama = tNama.getValue() == null ? "" : tNama.getValue().trim();
				a.email = tEmail.getValue() == null ? "" : tEmail.getValue().trim();
				a.telp = tTelp.getValue() == null ? "" : tTelp.getValue().trim();
				a.alamat = tAlamat.getValue() == null ? "" : tAlamat.getValue().trim();
				a.peran = tPeran.getValue() == null ? "" : tPeran.getValue().trim();
				a.masihAktif = cAktif.isChecked();
				Date d = dMulai.getValue();
				a.tanggalMulai = d == null ? "" : sdf.format(d);
				a.keterangan = tKet.getValue() == null ? "" : tKet.getValue().trim();

				if (a.nama.isEmpty()) {
					ais.ui.util.MyMessageboxConfig.show("Nama Atasan wajib diisi.", "Peringatan",
							ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
					return;
				}
				if (editIndex < 0) {
					daftar.add(a);
				}
				refreshTabel();
				win.detach();
			}
		});
		tombol.appendChild(btnSimpan);

		MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		btnBatal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});
		tombol.appendChild(btnBatal);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);
		win.onModal();
	}

	private MyTextbox tambahBarisTeks(Rows formRows, String label, String nilai) {
		ais.ui.util.MyFormRow r = new ais.ui.util.MyFormRow();
		r.setParent(formRows);
		r.appendChild(new MyLabelConfig(label));
		MyTextbox tb = new MyTextbox(nilai == null ? "" : nilai);
		tb.setWidth("90%");
		r.appendChild(tb);
		return tb;
	}
}
