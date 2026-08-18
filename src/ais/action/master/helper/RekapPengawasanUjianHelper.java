package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.PertemuanPunyaUjian;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * <h3>RekapPengawasanUjianHelper — rekap pelanggaran pengawasan (anti-curang) per peserta</h3>
 *
 * <p>Pengawasan ujian (Mode Pengawasan / anti-curang di {@code ProsesUjianHelper}) kini
 * MEREKAM tiap pelanggaran (pindah tab, blur jendela/Alt-Tab, keluar fullscreen) ke
 * {@link HasilUjianMahasiswa#getJumlahPelanggaran()} + {@code getLogPelanggaran()}. Helper
 * ini menampilkan rekap tersebut dalam satu jendela grid untuk sebuah
 * {@link PertemuanPunyaUjian}: Nama peserta, Jumlah Pelanggaran, dan Log kejadian.</p>
 *
 * <p>Dipakai oleh tombol "Rekap" di {@code PertemuanPunyaUjianHelper} (mahasiswa) dan
 * {@code PertemuanPunyaUjianSiswaHelper} (siswa) — keduanya berbagi entitas
 * HasilUjianMahasiswa. Gaya Java 1.6/1.7.</p>
 */
public final class RekapPengawasanUjianHelper {

	private RekapPengawasanUjianHelper() {
	}

	/** Nama peserta: mahasiswa / siswa / calon mahasiswa, mana yang tersedia. */
	private static String namaPeserta(HasilUjianMahasiswa h) {
		try {
			if (h.getMahasiswa() != null) {
				String nm = h.getMahasiswa().getNama();
				return (nm == null ? "" : nm) + " (" + safe(h.getMahasiswa().getNim()) + ")";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapPengawasanUjianHelper.java:56");
		}
		try {
			if (h.getSiswa() != null) {
				return safe(h.getSiswa().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapPengawasanUjianHelper.java:62");
		}
		try {
			if (h.getBiodataCalonMahasiswa() != null) {
				return safe(h.getBiodataCalonMahasiswa().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapPengawasanUjianHelper.java:68");
		}
		return "-";
	}

	private static String safe(String v) {
		return v == null ? "" : v;
	}

	@SuppressWarnings("unchecked")
	public static void tampilkanRekap(PertemuanPunyaUjian pertemuanPunyaUjian) throws Exception {
		final MyWindow window = new MyWindow("Rekap Pengawasan Ujian", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setWidth("82%");
		window.setHeight("86%");

		MyBorderlayout borderlayout = new MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ZkCompat.setFlex(center, true);
		center.setStyle("overflow:auto;");
		center.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setVflex("1");
		grid.setStyle("background:#ffffff;");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);
		Column cNo = new Column("No.");
		cNo.setWidth("48px");
		cNo.setParent(columns);
		Column cNama = new Column("Nama Peserta");
		cNama.setWidth("28%");
		cNama.setParent(columns);
		Column cJml = new Column("Jumlah Pelanggaran");
		cJml.setWidth("130px");
		cJml.setAlign("center");
		cJml.setParent(columns);
		Column cStatus = new Column("Status Ujian");
		cStatus.setWidth("110px");
		cStatus.setParent(columns);
		Column cLog = new Column("Log Pelanggaran (waktu & jenis)");
		cLog.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Session s = HibernateUtil.currentSession();
		List<HasilUjianMahasiswa> list = s.createCriteria(HasilUjianMahasiswa.class)
				.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
				.addOrder(Order.desc("jumlahPelanggaran")).addOrder(Order.asc("id")).list();

		int no = 1;
		int totalPelanggar = 0;
		for (HasilUjianMahasiswa h : list) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);

			row.appendChild(new Label(String.valueOf(no++)));
			row.appendChild(new Label(namaPeserta(h)));

			int jp = h.getJumlahPelanggaran() == null ? 0 : h.getJumlahPelanggaran().intValue();
			if (jp > 0) {
				totalPelanggar++;
			}
			Label lblJp = new Label(String.valueOf(jp));
			lblJp.setStyle(jp > 0 ? "color:#b91c1c;font-weight:bold;" : "color:#16a34a;");
			row.appendChild(lblJp);

			row.appendChild(new Label(Boolean.TRUE.equals(h.getTelahIkutUjian()) ? "Sudah Ujian" : "Belum"));

			Label lblLog = new Label(h.getLogPelanggaran() == null || h.getLogPelanggaran().trim().isEmpty() ? "-"
					: h.getLogPelanggaran());
			lblLog.setMultiline(true);
			lblLog.setPre(true);
			lblLog.setStyle("font-size:11px;color:#475569;white-space:pre-wrap;");
			row.appendChild(lblLog);
		}

		if (list.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			Label kosong = new Label(ais.common.Common.getBahasaConfig("Belum ada data peserta untuk ujian ini."));
			kosong.setStyle("color:#94a3b8;");
			row.appendChild(kosong);
			row.appendChild(new Label(""));
			row.appendChild(new Label(""));
			row.appendChild(new Label(""));
			row.appendChild(new Label(""));
		}

		South south = new South();
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		Label info = new Label(
				"Total peserta: " + list.size() + "  |  Terdeteksi melanggar: " + totalPelanggar + "    ");
		info.setStyle("font-weight:bold;color:#0f172a;margin-right:12px;");
		info.setParent(toolbar);

		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.setTooltiptext("Tutup");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setParent(null);
			}
		});
		tutup.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}
}
