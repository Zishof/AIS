package ais.common;

import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KonfigurasiPromptHelper {

	private KonfigurasiPromptHelper() {
	}

	private static boolean bolehUbahKonfigurasi() {
		Tbmuser user = Common.getCurrentUser();
		return user != null && user.getMahasiswa() == null && user.ambilDosen() == null;
	}

	public static void tampilkanPeringatanDenganOpsiUbah(final String pesan, final String judul,
			final String namaKonfigurasi, final String defaultNilai) {
		if (!bolehUbahKonfigurasi() || namaKonfigurasi == null || namaKonfigurasi.trim().isEmpty()) {
			try {
				MyMessageboxConfig.show(pesan, judul, MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			return;
		}

		try {
			MyMessageboxConfig.show(pesan + "\n\nApakah ingin mengubah konfigurasi ini sekarang?",
					judul, MyMessageboxConfig.YES | MyMessageboxConfig.NO, MyMessageboxConfig.QUESTION,
					new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							int pilihan = Integer.parseInt(event.getData().toString());
							if (pilihan == MyMessageboxConfig.YES) {
								tampilkanFormUbahKonfigurasi(namaKonfigurasi, defaultNilai);
							}
						}
					});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void tampilkanFormUbahKonfigurasi(final String namaKonfigurasi, final String defaultNilai)
			throws Exception {
		final Konfigurasi konfigurasi = Common.getKonfigurasi(namaKonfigurasi, defaultNilai);
		final MyWindow window = new MyWindow();
		window.setTitle("Ubah Konfigurasi");
		window.setWidth("560px");
		window.setHeight("310px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jenis Konfigurasi"));
		row.appendChild(new Label(konfigurasi.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nilai Konfigurasi"));
		final Textbox nilai = new Textbox(konfigurasi.getNilai());
		nilai.setWidth("95%");
		row.appendChild(nilai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Keterangan"));
		final Textbox keterangan = new Textbox(konfigurasi.getKeterangan() == null ? "" : konfigurasi.getKeterangan());
		keterangan.setRows(4);
		keterangan.setWidth("95%");
		row.appendChild(keterangan);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (nilai.getValue() == null || nilai.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nilai konfigurasi belum diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				Konfigurasi target = konfigurasi.getId() == null ? konfigurasi
						: (Konfigurasi) session.load(Konfigurasi.class, konfigurasi.getId());
				target.setNama(namaKonfigurasi);
				target.setNilai(nilai.getValue().trim());
				target.setKeterangan(keterangan.getValue());
				Common.refreshSaveOrUpdate(session, target);
				session.flush();

				try {
					Map<String, Konfigurasi> cache = MemoryDbUtil.getKonfigurasi();
					if (cache != null) {
						cache.put(target.getNama(), target);
					}
				} catch (Throwable t) {
					MemoryDbUtil.resetLocalReferences();
				}

				window.detach();
				MyMessageboxConfig.show("Konfigurasi berhasil disimpan", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		simpan.setParent(toolbar);

		window.onModal();
	}
}
