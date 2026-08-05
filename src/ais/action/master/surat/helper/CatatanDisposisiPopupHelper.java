package ais.action.master.surat.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;

/**
 * <h1>CatatanDisposisiPopupHelper — popup TABEL "Catatan Disposisi"</h1>
 *
 * <p>Menggantikan tombol lama "Disposisi" (yang mencetak PDF format kertas via {@code cetakDisposisi})
 * menjadi <b>"Catatan Disposisi"</b>: sebuah pop-up modal berisi TABEL catatan disposisi per pengguna/
 * pejabat — kolom <b>Nomor/Tgl</b>, <b>Klasifikasi</b>, <b>Diperuntukkan/Diajukan</b>, dan
 * <b>Alur Persetujuan</b> (kartu status + catatan + waktu + kontrol tindak lanjut). Bukan format cetakan.</p>
 *
 * <p>Data baris diambil dengan query yang SAMA seperti {@code SuratKeluarAction.cetakDisposisi} /
 * {@code SuratMasukAction.cetakDisposisi} (daftar {@code ...Status} ber-{@code kodeUnik} untuk satu surat,
 * urut id) sehingga isinya persis sama dengan versi PDF lama. Kartu "Alur Persetujuan" memakai builder
 * HTML yang sudah ada di {@link DasboardSurat} ({@code buildAlurKeluarStatusHtmlV20} /
 * {@code buildAlurMasukStatusHtmlV20}), dan kontrol unggah/unduh tindak lanjut memakai
 * {@link LampiranLain#createDownloadUploadFileLain} seperti layar detail.</p>
 */
public final class CatatanDisposisiPopupHelper {

	private CatatanDisposisiPopupHelper() {
	}

	/** Satu baris tabel = satu langkah/pejabat disposisi. */
	private static final class Baris {
		String nomorTgl; // HTML
		String klasifikasi; // teks
		String jabatan; // teks (Diperuntukkan/Diajukan)
		String cardHtml; // HTML kartu Alur Persetujuan
		Long lampiranRef; // id status untuk kontrol tindak lanjut
		String lampiranJenis; // nama class status (jenis LampiranLain)
	}

	// ── SURAT KELUAR ────────────────────────────────────────────────────────────────────────
	@SuppressWarnings("unchecked")
	public static void showKeluar(AlurPersetujuanSuratKeluarStatus status, Tbmuser tbmuser, Component owner) {
		try {
			if (status == null || status.getSuratKeluar() == null) {
				return;
			}
			SuratKeluar surat = status.getSuratKeluar();
			Session session = HibernateUtil.currentSession();
			List<AlurPersetujuanSuratKeluarStatus> daftar = session
					.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.add(Restrictions.eq("suratKeluar", surat)).addOrder(Order.asc("id")).list();

			String nomor = surat.getKode();
			String klasifikasi = surat.getKlasifikasiSuratKeluar() == null ? ""
					: surat.getKlasifikasiSuratKeluar().getNama();

			List<Baris> baris = new ArrayList<Baris>();
			for (AlurPersetujuanSuratKeluarStatus s : daftar) {
				Baris b = new Baris();
				Date waktu = Boolean.TRUE.equals(s.getDitolak()) ? s.getWaktuDitolak()
						: (Boolean.TRUE.equals(s.getDisetujui()) ? s.getWaktuPersetujuan() : null);
				b.nomorTgl = nomorTglHtml(nomor, waktu != null ? waktu : surat.getTanggal());
				b.klasifikasi = klasifikasi;
				b.jabatan = s.getJenisJabatan() == null ? "" : s.getJenisJabatan().getNama();
				b.cardHtml = DasboardSurat.buildAlurKeluarStatusHtmlV20(s);
				b.lampiranRef = s.getId();
				b.lampiranJenis = AlurPersetujuanSuratKeluarStatus.class.getName();
				baris.add(b);
			}
			tampilTabel(owner, "Catatan Disposisi — " + nz(nomor), nz(surat.getKepada()), baris);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ── SURAT MASUK ─────────────────────────────────────────────────────────────────────────
	@SuppressWarnings("unchecked")
	public static void showMasuk(AlurPersetujuanSuratMasukStatus status, Tbmuser tbmuser, Component owner) {
		try {
			if (status == null || status.getSuratMasuk() == null) {
				return;
			}
			SuratMasuk surat = status.getSuratMasuk();
			Session session = HibernateUtil.currentSession();
			List<AlurPersetujuanSuratMasukStatus> daftar = session
					.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.add(Restrictions.eq("suratMasuk", surat)).addOrder(Order.asc("id")).list();

			String nomor = surat.getNoSurat() == null || surat.getNoSurat().trim().isEmpty() ? surat.getKode()
					: surat.getNoSurat();
			String klasifikasi = surat.getKlasifikasiSuratMasuk() == null ? ""
					: surat.getKlasifikasiSuratMasuk().getNama();

			List<Baris> baris = new ArrayList<Baris>();
			for (AlurPersetujuanSuratMasukStatus s : daftar) {
				Baris b = new Baris();
				Date waktu = Boolean.TRUE.equals(s.getDitolak()) ? s.getWaktuDitolak()
						: (Boolean.TRUE.equals(s.getDisetujui()) ? s.getWaktuPersetujuan() : null);
				b.nomorTgl = nomorTglHtml(nomor, waktu != null ? waktu : surat.getTanggalSurat());
				b.klasifikasi = klasifikasi;
				b.jabatan = s.getJenisJabatan() == null ? "" : s.getJenisJabatan().getNama();
				b.cardHtml = DasboardSurat.buildAlurMasukStatusHtmlV20(s);
				b.lampiranRef = s.getId();
				b.lampiranJenis = AlurPersetujuanSuratMasukStatus.class.getName();
				baris.add(b);
			}
			tampilTabel(owner, "Catatan Disposisi — " + nz(nomor), nz(surat.getAsal()), baris);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ── Render modal + tabel (generik) ──────────────────────────────────────────────────────
	private static void tampilTabel(Component owner, String judul, String diperuntukkanHeader, List<Baris> baris)
			throws Exception {
		boolean mobile = Common.isMobile();

		final Window window = new Window();
		window.setTitle(judul);
		window.setWidth(mobile ? "96%" : "88%");
		window.setHeight(mobile ? "88%" : "80%");
		window.setClosable(true);
		window.setSizable(true);
		window.setMaximizable(true);
		window.setBorder("normal");
		window.setContentStyle("overflow:auto;");
		window.setStyle("border-radius:14px; overflow:hidden;");
		if (owner != null && owner.getPage() != null) {
			window.setPage(owner.getPage());
		} else if (owner != null && owner.getParent() != null) {
			window.setParent(owner.getParent());
		} else {
			window.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}

		Vbox root = new Vbox();
		root.setWidth("100%");
		root.setStyle("padding:12px 14px; box-sizing:border-box; background:#f8fafc;");
		root.setParent(window);

		new MyHtml("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>"
				+ "Catatan disposisi per pejabat/pengguna. Diperuntukkan/Diajukan: <b>" + escHtml(diperuntukkanHeader)
				+ "</b></div>").setParent(root);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(15);
		grid.setParent(root);

		Columns columns = new Columns();
		columns.setParent(grid);
		kolom(columns, "Nomor / Tgl.", mobile ? "120px" : "170px");
		kolom(columns, "Klasifikasi", mobile ? "110px" : "170px");
		kolom(columns, "Diperuntukkan / Diajukan", mobile ? "120px" : "180px");
		kolom(columns, "Alur Persetujuan", null);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (baris.isEmpty()) {
			Row r = new Row();
			r.setParent(rows);
			MyHtml kosong = new MyHtml(
					"<div style='padding:14px;color:#94a3b8;font-size:12px;'>Belum ada data disposisi.</div>");
			kosong.setParent(r);
		}

		for (Baris b : baris) {
			Row r = new Row();
			r.setValign("top");
			r.setParent(rows);

			new MyHtml(b.nomorTgl).setParent(r);
			new MyHtml("<div style='font-size:11px;color:#334155;'>" + escHtml(b.klasifikasi) + "</div>").setParent(r);
			new MyHtml("<div style='font-size:11px;font-weight:700;color:#0f172a;'>" + escHtml(b.jabatan) + "</div>")
					.setParent(r);

			Vbox sel = new Vbox();
			sel.setWidth("100%");
			sel.setParent(r);
			new MyHtml(b.cardHtml).setParent(sel);
			try {
				if (b.lampiranRef != null) {
					Hbox hbox = new Hbox();
					hbox.setParent(sel);
					LampiranLain.createDownloadUploadFileLain(hbox, b.lampiranRef, b.lampiranJenis,
							"tindak lanjut Disposisi", false, null, null, false, false, false, false);
				}
			} catch (Exception eLamp) {
				Common.tampilErrorJikaAdmin(eLamp);
			}
		}

		try {
			window.doModal();
		} catch (InterruptedException eModal) { ais.common.ErrorAuditUtil.record(eModal, "auto-audit(empty-catch) src/ais/action/master/surat/helper/CatatanDisposisiPopupHelper.java:214");
		}
	}

	private static void kolom(Columns columns, String label, String lebar) {
		MyColumnConfig c = lebar == null ? new MyColumnConfig(label) : new MyColumnConfig(label, lebar);
		c.setParent(columns);
	}

	private static String nomorTglHtml(String nomor, Date tgl) {
		String t = "";
		try {
			if (tgl != null) {
				t = Common.dateFormat3.get().format(tgl);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CatatanDisposisiPopupHelper.nomorTglHtml");
		}
		return "<div style='font-size:11px;font-weight:800;color:#1e3a5f;'>" + escHtml(nz(nomor)) + "</div>"
				+ "<div style='font-size:10px;color:#64748b;margin-top:2px;'>" + escHtml(t) + "</div>";
	}

	private static String nz(String s) {
		return s == null ? "" : s.trim();
	}

	private static String escHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
