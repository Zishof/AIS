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
import ais.ui.util.MyToolbarbuttonConfig;

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

		// Versi teks-polos (untuk cetak/unduh PDF — tanpa markup HTML)
		String pNomor; // nomor surat
		String pTgl; // tanggal terformat
		String pStatus; // Disetujui / Ditolak / Menunggu Persetujuan
		String pCatatan; // isi catatan/keterangan disposisi
		String pWaktu; // waktu status terformat
	}

	private static String statusLabel(Boolean disetujui, Boolean ditolak) {
		if (Boolean.TRUE.equals(ditolak)) {
			return "Ditolak";
		}
		if (Boolean.TRUE.equals(disetujui)) {
			return "Disetujui";
		}
		return "Menunggu Persetujuan";
	}

	private static String fmtTgl(Date tgl) {
		try {
			return tgl == null ? "" : Common.dateFormat3.get().format(tgl);
		} catch (Exception e) {
			return "";
		}
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
				b.pNomor = nz(nomor);
				b.pTgl = fmtTgl(waktu != null ? waktu : surat.getTanggal());
				b.pStatus = statusLabel(s.getDisetujui(), s.getDitolak());
				b.pCatatan = nz(s.getKeterangan());
				b.pWaktu = fmtTgl(waktu);
				baris.add(b);
			}

			// Listener "Cetak Disposisi" (format resmi lama) — SuratKeluarAction.cetakDisposisi(params, status, tbmuser).
			final AlurPersetujuanSuratKeluarStatus statusFinal = status;
			final SuratKeluar suratFinal = surat;
			final Tbmuser tbmuserFinal = tbmuser;
			org.zkoss.zk.ui.event.EventListener cetakDisposisi = new org.zkoss.zk.ui.event.EventListener() {
				@SuppressWarnings("rawtypes")
				public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
					java.util.Map parameters = ais.action.master.surat.util.SuratUtil.ubahIsiSuratKeluar(suratFinal, null);
					ais.action.master.surat.SuratKeluarAction.cetakDisposisi(parameters, statusFinal, tbmuserFinal);
				}
			};

			tampilTabel(owner, "Catatan Disposisi — " + nz(nomor), nz(surat.getKepada()), nz(nomor), baris,
					cetakDisposisi);
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
				String jabatan = s.getJenisJabatan() == null ? "" : s.getJenisJabatan().getNama();
				String penerima = ais.action.master.surat.SuratMasukAction.namaPenerimaDisposisiMasuk(s);
				b.jabatan = jabatan + (penerima.isEmpty() ? "" : (jabatan.isEmpty() ? penerima : " — " + penerima));
				b.cardHtml = DasboardSurat.buildAlurMasukStatusHtmlV20(s);
				b.lampiranRef = s.getId();
				b.lampiranJenis = AlurPersetujuanSuratMasukStatus.class.getName();
				b.pNomor = nz(nomor);
				b.pTgl = fmtTgl(waktu != null ? waktu : surat.getTanggalSurat());
				b.pStatus = statusLabel(s.getDisetujui(), s.getDitolak());
				b.pCatatan = nz(s.getKeterangan());
				b.pWaktu = fmtTgl(waktu);
				baris.add(b);
			}

			// Listener "Cetak Disposisi" (format resmi lama) — SuratMasukAction.cetakDisposisi(status, tbmuser).
			final AlurPersetujuanSuratMasukStatus statusFinal = status;
			final Tbmuser tbmuserFinal = tbmuser;
			org.zkoss.zk.ui.event.EventListener cetakDisposisi = new org.zkoss.zk.ui.event.EventListener() {
				public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
					ais.action.master.surat.SuratMasukAction.cetakDisposisi(statusFinal, tbmuserFinal);
				}
			};

			tampilTabel(owner, "Catatan Disposisi — " + nz(nomor), nz(surat.getAsal()), nz(nomor), baris, cetakDisposisi);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ── Render modal + tabel (generik) ──────────────────────────────────────────────────────
	private static void tampilTabel(Component owner, final String judul, final String diperuntukkanHeader,
			final String nomorFile, final List<Baris> baris,
			final org.zkoss.zk.ui.event.EventListener cetakDisposisiListener) throws Exception {
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

		// Bilah aksi: tombol Cetak Disposisi (format resmi, seperti di SuratKeluarAction) + Cetak PDF (rata kanan)
		Hbox bar = new Hbox();
		bar.setWidth("100%");
		bar.setPack("end");
		bar.setStyle("margin-bottom:6px;");
		bar.setParent(root);

		// "Cetak Disposisi" — mencetak PDF disposisi format resmi via SuratKeluarAction/SuratMasukAction.cetakDisposisi
		// (sama seperti tombol lama "Disposisi"). Hanya tampil bila listener tersedia (status & tbmuser diketahui).
		if (cetakDisposisiListener != null) {
			MyToolbarbuttonConfig btnDisposisi = new MyToolbarbuttonConfig("Cetak Disposisi", "/img/print.png");
			btnDisposisi.setTooltiptext("Cetak lembar disposisi (format resmi) ke PDF");
			btnDisposisi.setParent(bar);
			btnDisposisi.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK,
					new org.zkoss.zk.ui.event.EventListener() {
						public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
							try {
								cetakDisposisiListener.onEvent(event);
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
							}
						}
					});
		}

		MyToolbarbuttonConfig btnPdf = new MyToolbarbuttonConfig("Cetak PDF", "/img/print.png");
		btnPdf.setTooltiptext("Unduh catatan disposisi ke format PDF");
		btnPdf.setParent(bar);
		btnPdf.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new org.zkoss.zk.ui.event.EventListener() {
			public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
				try {
					cetakPdf(judul, diperuntukkanHeader, nomorFile, baris);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				}
			}
		});

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

	// ── Cetak / unduh PDF (iText 5) — mirror tabel popup, 4 kolom ─────────────────────────────
	private static void cetakPdf(String judul, String diperuntukkanHeader, String nomorFile, List<Baris> baris)
			throws Exception {
		com.itextpdf.text.Document document = new com.itextpdf.text.Document(
				com.itextpdf.text.PageSize.A4.rotate(), 28, 28, 28, 28);
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
		document.open();

		com.itextpdf.text.Font fJudul = com.itextpdf.text.FontFactory.getFont(
				com.itextpdf.text.FontFactory.HELVETICA_BOLD, 13, new com.itextpdf.text.BaseColor(15, 58, 95));
		com.itextpdf.text.Font fSub = com.itextpdf.text.FontFactory.getFont(
				com.itextpdf.text.FontFactory.HELVETICA, 9, new com.itextpdf.text.BaseColor(80, 90, 105));
		com.itextpdf.text.Font fHead = com.itextpdf.text.FontFactory.getFont(
				com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9, com.itextpdf.text.BaseColor.WHITE);
		com.itextpdf.text.Font fSel = com.itextpdf.text.FontFactory.getFont(
				com.itextpdf.text.FontFactory.HELVETICA, 9, new com.itextpdf.text.BaseColor(20, 30, 45));
		com.itextpdf.text.Font fStatus = com.itextpdf.text.FontFactory.getFont(
				com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9, new com.itextpdf.text.BaseColor(20, 30, 45));
		com.itextpdf.text.Font fWaktu = com.itextpdf.text.FontFactory.getFont(
				com.itextpdf.text.FontFactory.HELVETICA, 8, new com.itextpdf.text.BaseColor(110, 120, 135));

		com.itextpdf.text.Paragraph pJudul = new com.itextpdf.text.Paragraph(nz(judul), fJudul);
		document.add(pJudul);
		com.itextpdf.text.Paragraph pSub = new com.itextpdf.text.Paragraph(
				"Catatan disposisi per pejabat/pengguna. Diperuntukkan/Diajukan: " + nz(diperuntukkanHeader), fSub);
		pSub.setSpacingAfter(8f);
		document.add(pSub);

		com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(4);
		table.setWidthPercentage(100f);
		table.setWidths(new float[] { 20f, 16f, 18f, 46f });
		table.setHeaderRows(1);

		com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(30, 58, 95);
		String[] judulKolom = { "Nomor / Tgl.", "Klasifikasi", "Diperuntukkan / Diajukan", "Alur Persetujuan" };
		for (int i = 0; i < judulKolom.length; i++) {
			com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell(
					new com.itextpdf.text.Phrase(judulKolom[i], fHead));
			c.setBackgroundColor(headerBg);
			c.setPadding(6f);
			c.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
			table.addCell(c);
		}

		com.itextpdf.text.BaseColor garis = new com.itextpdf.text.BaseColor(210, 218, 226);
		if (baris == null || baris.isEmpty()) {
			com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell(
					new com.itextpdf.text.Phrase("Belum ada data disposisi.", fSel));
			c.setColspan(4);
			c.setPadding(10f);
			table.addCell(c);
		} else {
			int idx = 0;
			for (Baris b : baris) {
				com.itextpdf.text.BaseColor selBg = (idx % 2 == 0) ? com.itextpdf.text.BaseColor.WHITE
						: new com.itextpdf.text.BaseColor(246, 249, 252);
				idx++;

				// Kol 1: Nomor / Tgl
				com.itextpdf.text.Phrase ph1 = new com.itextpdf.text.Phrase();
				ph1.add(new com.itextpdf.text.Chunk(nz(b.pNomor) + "\n", fStatus));
				if (!nz(b.pTgl).isEmpty()) {
					ph1.add(new com.itextpdf.text.Chunk(nz(b.pTgl), fWaktu));
				}
				table.addCell(selCell(ph1, selBg, garis));

				// Kol 2: Klasifikasi
				table.addCell(selCell(new com.itextpdf.text.Phrase(nz(b.klasifikasi), fSel), selBg, garis));

				// Kol 3: Diperuntukkan / Diajukan
				table.addCell(selCell(new com.itextpdf.text.Phrase(nz(b.jabatan), fStatus), selBg, garis));

				// Kol 4: Alur Persetujuan (jabatan + status + catatan + waktu)
				com.itextpdf.text.Phrase ph4 = new com.itextpdf.text.Phrase();
				if (!nz(b.jabatan).isEmpty()) {
					ph4.add(new com.itextpdf.text.Chunk(nz(b.jabatan) + "\n", fStatus));
				}
				ph4.add(new com.itextpdf.text.Chunk(nz(b.pStatus), fStatus));
				if (!nz(b.pCatatan).isEmpty()) {
					ph4.add(new com.itextpdf.text.Chunk("\n" + nz(b.pCatatan), fSel));
				}
				if (!nz(b.pWaktu).isEmpty()) {
					ph4.add(new com.itextpdf.text.Chunk("\n" + nz(b.pWaktu), fWaktu));
				}
				table.addCell(selCell(ph4, selBg, garis));
			}
		}

		document.add(table);
		document.close();

		String namaFile = "Catatan_Disposisi_" + nz(nomorFile).replaceAll("[^A-Za-z0-9_-]+", "_");
		if (namaFile.endsWith("_")) {
			namaFile = namaFile.substring(0, namaFile.length() - 1);
		}
		org.zkoss.zul.Filedownload.save(baos.toByteArray(), "application/pdf", namaFile + ".pdf");
	}

	private static com.itextpdf.text.pdf.PdfPCell selCell(com.itextpdf.text.Phrase ph,
			com.itextpdf.text.BaseColor bg, com.itextpdf.text.BaseColor garis) {
		com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell(ph);
		c.setPadding(6f);
		c.setBackgroundColor(bg);
		c.setBorderColor(garis);
		c.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_TOP);
		return c;
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
