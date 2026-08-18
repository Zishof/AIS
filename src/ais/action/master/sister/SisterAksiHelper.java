package ais.action.master.sister;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.DataSisterApi;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>Aksi Bersama untuk Fitur SISTER (dapat dipakai ulang)</h2>
 *
 * <p>
 * Kumpulan aksi antarmuka SISTER yang dipakai bersama oleh halaman kelola {@code DataSisterAction} maupun
 * dasbor mandiri {@link DasbordSinkronisasiSister}, agar tidak ada duplikasi &amp; mudah dirawat:
 * </p>
 * <ol>
 *   <li>{@link #bukaDialogLogin(Component)} — dialog isi &amp; uji kredensial SISTER (username, password,
 *       id pengguna, alamat server) yang tersimpan sebagai konfigurasi.</li>
 *   <li>{@link #bukaDialogPilihDosen(Component)} — dialog memilih dosen (centang + saring nama) agar
 *       sinkronisasi data dosen/Tridharma dapat dijalankan <b>bertahap/per-batch</b>, bukan sekaligus.</li>
 * </ol>
 *
 * <p>
 * Kedua dialog dibuka sebagai {@link MyWindow} modal di bawah {@code root} (biasanya root halaman). Seluruh
 * pekerjaan berat (login/sinkron) didelegasikan ke {@link DataSisterApi} yang menjalankan thread latar aman.
 * Kompatibel Java 1.7 (tanpa lambda/stream/diamond, {@code try/catch} gaya 1.6).
 * </p>
 *
 * @author e-Campus
 */
public final class SisterAksiHelper {

	private SisterAksiHelper() {
	}

	// =====================================================================================
	// DIALOG LOGIN / UJI KREDENSIAL
	// =====================================================================================

	/**
	 * Membuka dialog untuk mengisi &amp; menguji kredensial SISTER. Nilai disimpan sebagai konfigurasi, lalu
	 * login diuji lewat {@link DataSisterApi#doLogin(String, String, String, String)}.
	 *
	 * @param root komponen induk untuk memasang jendela modal (mis. {@code page.getFirstRoot()}).
	 */
	public static void bukaDialogLogin(Component root) throws Exception {
		final MyWindow window = new MyWindow("Masukkan Akun SISTER", "none", true);
		window.setParent(root);
		window.setHeight("95%");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid gridDialog = new MyGrid();
		gridDialog.setWidth("100%");
		gridDialog.setParent(center);
		gridDialog.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(gridDialog);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridDialog);

		String username = Common.getKonfigurasi("sister_username", "knNcb8iOFtKOxY1N8mUfVY5mqArRyecX+RH+pLOndCE=")
				.getNilai();
		String password = Common
				.getKonfigurasi("sister_password", "MycV1kHjaHWJ97zYzg4YiReNBpIj40ZVnxrFXWkmi0zooQDExe6sJ6HLHVoX8BJN")
				.getNilai();
		String idPengguna = Common.getKonfigurasi("sister_id_pengguna", "acecd7e5-330a-48e8-98d0-12cd46500408")
				.getNilai();
		String strURL = Common.getKonfigurasi("sister_host_url", "https://sister-api.kemdikbud.go.id/ws.php/1.0")
				.getNilai();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Username"));
		final Textbox usernameBox = new Textbox(username);
		row.appendChild(usernameBox);
		usernameBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Password"));
		final Textbox passwordBox = new Textbox(password);
		row.appendChild(passwordBox);
		passwordBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("ID Pengguna"));
		final Textbox idPenggunaBox = new Textbox(idPengguna);
		row.appendChild(idPenggunaBox);
		idPenggunaBox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Host URL"));
		final Textbox hostUrlBox = new Textbox(strURL);
		row.appendChild(hostUrlBox);
		hostUrlBox.setWidth("90%");
		hostUrlBox.setRows(2);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Coba Login", "/img/save.gif");
		save.setTooltiptext("Simpan & uji kredensial");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();

				Konfigurasi kUser = Common.getKonfigurasi("sister_username",
						"knNcb8iOFtKOxY1N8mUfVY5mqArRyecX+RH+pLOndCE=");
				kUser.setNilai(usernameBox.getValue());
				Common.refreshUpdate(kUser);

				Konfigurasi kPass = Common.getKonfigurasi("sister_password",
						"MycV1kHjaHWJ97zYzg4YiReNBpIj40ZVnxrFXWkmi0zooQDExe6sJ6HLHVoX8BJN");
				kPass.setNilai(passwordBox.getValue());
				Common.refreshUpdate(kPass);

				Konfigurasi kId = Common.getKonfigurasi("sister_id_pengguna", "acecd7e5-330a-48e8-98d0-12cd46500408");
				kId.setNilai(idPenggunaBox.getValue());
				Common.refreshUpdate(kId);

				Konfigurasi kHost = Common.getKonfigurasi("sister_host_url",
						"https://sister-api.kemdikbud.go.id/ws.php/1.0");
				kHost.setNilai(hostUrlBox.getValue());
				Common.refreshUpdate(kHost);

				String hasil = DataSisterApi.doLogin(kUser.getNilai(), kPass.getNilai(), kId.getNilai(),
						kHost.getNilai() + "/authorize");

				if (DataSisterApi.token == null || DataSisterApi.token.isEmpty()) {
					MyMessageboxConfig.show("Login data SISTER GAGAL. Info rinci:\n\n" + hasil, "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				} else {
					MyMessageboxConfig.show("Login data SISTER BERHASIL. Info rinci:\n\n" + hasil, "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

	// =====================================================================================
	// DIALOG PEMILIHAN DOSEN (sinkron bertahap)
	// =====================================================================================

	/**
	 * Membuka dialog daftar dosen (dari referensi SDM tersinkron) dengan centang + penyaring nama, tombol
	 * "Pilih Semua (tampil)"/"Kosongkan", lalu "Sinkronkan Terpilih". Memungkinkan menyinkronkan hanya
	 * sebagian dosen sekali jalan.
	 */
	public static void bukaDialogPilihDosen(Component root) throws Exception {
		final List<String[]> dosens = DataSisterApi.ambilDaftarDosen();
		if (dosens == null || dosens.isEmpty()) {
			MyMessageboxConfig.show(
					"Belum ada data SDM/dosen. Jalankan \"Sinkronkan Data Referensi\" lebih dulu (agar referensi SDM terisi), lalu ulangi.",
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		final Set<String> terpilih = new HashSet<String>();

		final MyWindow window = new MyWindow("Pilih Dosen untuk Disinkronkan", "normal", true);
		window.setParent(root);
		window.setWidth("640px");
		window.setHeight("90%");
		window.setClosable(true);

		Borderlayout bl = new ais.ui.util.MyBorderlayout();
		bl.setParent(window);

		North north = new North();
		north.setParent(bl);
		Vbox atas = new Vbox();
		atas.setWidth("100%");
		atas.setStyle("padding:8px;");
		atas.setParent(north);
		atas.appendChild(new MyLabelConfig(
				"Centang dosen yang ingin disinkronkan (boleh sebagian, agar bisa bertahap). Ketik untuk menyaring nama."));
		Hbox barisAtas = new Hbox();
		barisAtas.setParent(atas);
		barisAtas.appendChild(new MyLabelConfig("Cari nama:"));
		final Textbox cari = new Textbox();
		cari.setWidth("240px");
		barisAtas.appendChild(cari);
		final Label infoLbl = new Label(ais.common.Common.getBahasaConfig("Terpilih: 0"));
		barisAtas.appendChild(infoLbl);

		Center center = new Center();
		center.setParent(bl);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Div scroll = new Div();
		scroll.setWidth("100%");
		scroll.setStyle("overflow:auto;height:100%;");
		scroll.setParent(center);
		MyGrid g = new MyGrid();
		g.setWidth("100%");
		g.setParent(scroll);
		Columns cols = new Columns();
		cols.setParent(g);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("42px");
		c1.setParent(cols);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setLabel("Nama Dosen");
		c2.setParent(cols);
		MyColumnConfig c3 = new MyColumnConfig();
		c3.setLabel("ID SDM");
		c3.setWidth("40%");
		c3.setParent(cols);
		final Rows rows = new Rows();
		rows.setParent(g);

		renderDosenRows(rows, dosens, "", terpilih, infoLbl);

		cari.addEventListener("onChanging", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String v = ((InputEvent) e).getValue();
				renderDosenRows(rows, dosens, v == null ? "" : v, terpilih, infoLbl);
			}
		});

		South south = new South();
		south.setParent(bl);
		ais.ui.util.ZkCompat.setFlex(south, true);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig pilihSemua = new MyToolbarbuttonConfig("Pilih Semua (tampil)",
				"/img/svg/check2-circle.svg");
		pilihSemua.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				String v = cari.getValue();
				String f = v == null ? "" : v.trim().toLowerCase();
				for (int i = 0; i < dosens.size(); i++) {
					String[] d = dosens.get(i);
					if (f.isEmpty() || d[1].toLowerCase().contains(f)) {
						terpilih.add(d[0]);
					}
				}
				renderDosenRows(rows, dosens, v == null ? "" : v, terpilih, infoLbl);
			}
		});
		pilihSemua.setParent(toolbar);

		MyToolbarbuttonConfig kosong = new MyToolbarbuttonConfig("Kosongkan", "/img/cancel.gif");
		kosong.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				terpilih.clear();
				renderDosenRows(rows, dosens, cari.getValue() == null ? "" : cari.getValue(), terpilih, infoLbl);
			}
		});
		kosong.setParent(toolbar);

		MyToolbarbuttonConfig sinkron = new MyToolbarbuttonConfig("Sinkronkan Terpilih", "/img/save.gif");
		sinkron.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (terpilih.isEmpty()) {
					MyMessageboxConfig.show("Belum ada dosen yang dicentang.", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				final List<String> pilihan = new ArrayList<String>(terpilih);
				window.detach();
				MyMessageboxConfig.show(
						"Sinkronkan " + pilihan.size()
								+ " dosen sekarang? Proses berjalan di latar belakang dan bisa lama.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) == MyMessageboxConfig.OK) {
									DataSisterApi.synDataDosen(pilihan);
								}
							}
						});
			}
		});
		sinkron.setParent(toolbar);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

	/** Menggambar ulang baris daftar dosen sesuai penyaring nama; mempertahankan centang lewat {@code terpilih}. */
	private static void renderDosenRows(final Rows rows, List<String[]> dosens, String filter,
			final Set<String> terpilih, final Label infoLbl) {
		Common.clear(rows);
		final int MAKS = 500;
		String f = filter == null ? "" : filter.trim().toLowerCase();
		int tampil = 0;
		for (int i = 0; i < dosens.size(); i++) {
			final String[] d = dosens.get(i);
			if (!f.isEmpty() && !d[1].toLowerCase().contains(f)) {
				continue;
			}
			if (tampil >= MAKS) {
				break;
			}
			tampil++;
			Row row = new Row();
			row.setParent(rows);
			final Checkbox cb = new Checkbox();
			cb.setChecked(terpilih.contains(d[0]));
			cb.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (cb.isChecked()) {
						terpilih.add(d[0]);
					} else {
						terpilih.remove(d[0]);
					}
					infoLbl.setValue("Terpilih: " + terpilih.size());
				}
			});
			row.appendChild(cb);
			row.appendChild(new Label(d[1]));
			row.appendChild(new Label(d[0]));
		}
		infoLbl.setValue("Terpilih: " + terpilih.size() + (tampil >= MAKS ? " (tampil " + MAKS + ", saring nama)" : ""));
	}
}
