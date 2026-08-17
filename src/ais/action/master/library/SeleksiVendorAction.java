package ais.action.master.library;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.library.helper.AmbilDataPenyediaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.library.Penyedia;
import ais.database.model.library.SeleksiVendor;
import ais.database.model.library.SeleksiVendorDetail;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * <h3>SeleksiVendorAction — Form SOP "Pemilihan Penilaian Vendor" (Pra-Pembelian)</h3>
 *
 * <p>Modul seleksi vendor dengan metode best-practice: beberapa vendor (I/II/III) dinilai
 * pada 9 kriteria (skor 1..5) dengan bobot per-kriteria, lalu di-ranking berdasarkan skor
 * tertimbang. Proses di-SOP-kan (disposisi) mengikuti pola {@code UangMukaAction}: field
 * boleh diubah selama disposisi masih aktif, terkunci saat sudah disetujui & selesai.</p>
 *
 * <p>Auto-registrasi sebagai {@code FormSop} lewat {@code InitDataHelper.reInitClass}
 * karena berada di paket {@code ais.action.master.*} dan memiliki konstruktor tanpa
 * argumen. Nomor pengajuan memakai {@code NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR_DATA}.</p>
 */
public class SeleksiVendorAction extends GenericAutowireComposer implements FormSop {

	private static final long serialVersionUID = 4124140285573733299L;

	public static final int JML_VENDOR = 3;
	public static final int JML_KRITERIA = 9;

	private static final String[] KRITERIA = { "Kesesuaian Harga", "Spesifikasi & Kualitas Penawaran",
			"Ketersediaan Stok / Kapasitas", "Kejelasan Penawaran", "Legalitas Vendor", "Pengalaman Vendor",
			"Responsif & Komunikatif", "Metode Pembayaran", "Reputasi" };

	private static final String[] PERTANYAAN = { "Apakah harga paling kompetitif dibanding vendor lain?",
			"Apakah spesifikasi barang/jasa sesuai kebutuhan?", "Apakah vendor mampu menyediakan sesuai jumlah & waktu?",
			"Apakah penawaran tertulis jelas, lengkap, dan detail?", "Apakah vendor memiliki dokumen usaha lengkap?",
			"Apakah vendor berpengalaman dalam bidang terkait?", "Seberapa cepat vendor merespons permintaan?",
			"Apakah syarat pembayaran fleksibel?", "Apakah vendor memiliki riwayat baik?" };

	private SeleksiVendor seleksiVendor;
	private DisposisiSop disposisiSop = null;
	private boolean persetujuan = false;
	private boolean viewOnly = false;
	private Tbmuser tbmuser;

	// Header controls
	private MyTextbox nama;          // Perihal
	private Label kode;
	private MyTextbox jenisPengadaan;
	private MyDatebox tanggal;
	private MyTextbox keterangan;    // Latar belakang

	// Per-vendor controls
	private final AmbilDataPenyediaBanbox[] penyedia = new AmbilDataPenyediaBanbox[JML_VENDOR];
	private final MyTextbox[] namaVendor = new MyTextbox[JML_VENDOR];
	private final MyTextbox[] alamatKontak = new MyTextbox[JML_VENDOR];
	private final MyTextbox[] jenisBarangJasa = new MyTextbox[JML_VENDOR];
	private final MyTextbox[] picVendor = new MyTextbox[JML_VENDOR];
	private final MyIntbox[][] nilai = new MyIntbox[JML_VENDOR][JML_KRITERIA];
	private final SeleksiVendorDetail[] detailRef = new SeleksiVendorDetail[JML_VENDOR];
	private final Label[] totalLabel = new Label[JML_VENDOR];
	private final Label[] rankLabel = new Label[JML_VENDOR];

	// Bobot + Ket per kriteria
	private final MyIntbox[] bobot = new MyIntbox[JML_KRITERIA];
	private final MyTextbox[] ket = new MyTextbox[JML_KRITERIA];

	// Section C
	private MyTextbox vendorPembanding1;
	private MyTextbox vendorPembanding2;
	private MyTextbox vendorPembanding3;
	private MyTextbox alasanDipilih;
	private final java.util.List<org.zkoss.zk.ui.Component> barisPembanding = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

	// Section D
	private Combobox rekomendasi;
	private MyIntbox rekomendasiNomor;
	private MyTextbox alasanUtama;

	public SeleksiVendorAction() {
		super();
		try {
			tbmuser = Common.getCurrentUser();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SeleksiVendorAction.ctor");
		}
	}

	public SeleksiVendorAction(boolean persetujuan) {
		this();
		this.persetujuan = persetujuan;
	}

	// ================= FormSop =================

	@Override
	public String istilah() throws Exception {
		return NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR; // "Pemilihan Penilaian Vendor"
	}

	@Override
	public DataSop ambil() throws Exception {
		return seleksiVendor;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return SeleksiVendor.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;

		seleksiVendor = (SeleksiVendor) generalValueObject;
		if (seleksiVendor == null) {
			seleksiVendor = new SeleksiVendor();
		}

		// view-only ketika sudah disetujui & selesai (identik pola UangMuka)
		viewOnly = false;
		if (seleksiVendor.getDisposisiSop() != null && seleksiVendor.getDisposisiSop().getDisposisiSetuju() != null
				&& seleksiVendor.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& seleksiVendor.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}
		final boolean edit = !viewOnly;

		// Muat detail (jika sudah tersimpan)
		List<SeleksiVendorDetail> details = new ArrayList<SeleksiVendorDetail>();
		if (seleksiVendor.getId() != null) {
			details = HibernateUtil.currentSession().createCriteria(SeleksiVendorDetail.class)
					.add(Restrictions.eq("seleksiVendor", seleksiVendor)).addOrder(Order.asc("urutan")).list();
		}
		for (int v = 0; v < JML_VENDOR; v++) {
			detailRef[v] = v < details.size() ? details.get(v) : new SeleksiVendorDetail();
			if (detailRef[v].getUrutan() == null || detailRef[v].getUrutan() == 0) {
				detailRef[v].setUrutan(v + 1);
			}
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setParent(columns);
		c1.setWidth("22%");
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		// ---- Header ----
		// Kode ditampilkan SEBELUM simpan: preview nomor berikutnya (tanpa menaikkan counter).
		String kodeTampil = seleksiVendor.getKode();
		if (kodeTampil == null) {
			try {
				kodeTampil = generateCode(false);
			} catch (Exception e) {
				kodeTampil = "(otomatis saat simpan)";
			}
		}
		kode = new Label(kodeTampil);
		rowKV(rows, "Kode Pengajuan", kode);

		nama = new MyTextbox();
		nama.setWidth("95%");
		nama.setValue(seleksiVendor.getNama() == null ? "" : seleksiVendor.getNama());
		rowKV(rows, "Perihal / Judul *", edit ? nama : new Label(nama.getValue()));

		jenisPengadaan = new MyTextbox();
		jenisPengadaan.setWidth("95%");
		jenisPengadaan.setValue(seleksiVendor.getJenisPengadaan() == null ? "" : seleksiVendor.getJenisPengadaan());
		rowKV(rows, "Jenis Barang/Jasa", edit ? jenisPengadaan : new Label(jenisPengadaan.getValue()));

		tanggal = new MyDatebox();
		tanggal.setWidth("160px");
		tanggal.setValue(seleksiVendor.getTanggal());
		rowKV(rows, "Tanggal", edit ? tanggal : new Label(Common.dateFormat.get().format(seleksiVendor.getTanggal())));

		keterangan = new MyTextbox();
		keterangan.setMultiline(true);
		keterangan.setRows(2);
		keterangan.setWidth("95%");
		keterangan.setValue(seleksiVendor.getKeterangan() == null ? "" : seleksiVendor.getKeterangan());
		rowKV(rows, "Latar Belakang", edit ? keterangan : new Label(keterangan.getValue()));

		// ---- Section A + B: Data Vendor & Penilaian (matriks full-width) ----
		Row rowMatrix = new Row();
		rowMatrix.setParent(rows);
		Cell cellMatrix = new Cell();
		cellMatrix.setColspan(2);
		cellMatrix.setParent(rowMatrix);
		buildMatriks(cellMatrix, edit);

		// Section C (Ringkasan Perbandingan / Vendor Pembanding) & Section D (Rekomendasi Pemilihan)
		// SENGAJA DIHILANGKAN dari form atas permintaan: field-nya tidak lagi ditampilkan/diisi.
		// Kolom entity dibiarkan (kompatibilitas data lama); controls tidak dibangun -> onSave tidak menyentuhnya.

		recomputeTotals();
		return grid;
	}

	/** Bangun matriks Data Vendor (Section A) + Penilaian 9 kriteria (Section B). */
	private void buildMatriks(Cell parent, final boolean edit) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("overflow-x:auto;");

		// PENTING: pakai Grid biasa, BUKAN MyGrid — MyGrid nested (setVisible(false)+Timer)
		// akan dipaksa tetap hidden oleh logika re-hide descendant grid induk (matriks jadi hilang).
		org.zkoss.zul.Grid g = new org.zkoss.zul.Grid();
		g.setSclass("dgrid");
		g.setWidth("100%");
		Columns cols = new Columns();
		cols.setParent(g);
		cols.setStyle("background:#eef2ff; font-weight:bold;");
		MyColumnConfig ck = new MyColumnConfig();
		ck.setParent(cols);
		ck.setWidth("28%");
		ck.setLabel("Kriteria / Data Vendor");
		MyColumnConfig cb = new MyColumnConfig();
		cb.setParent(cols);
		cb.setWidth("7%");
		cb.setLabel("Bobot%");
		for (int v = 0; v < JML_VENDOR; v++) {
			MyColumnConfig cv = new MyColumnConfig();
			cv.setParent(cols);
			cv.setLabel(new String[] { "Vendor I", "Vendor II", "Vendor III" }[v]);
		}
		MyColumnConfig cket = new MyColumnConfig();
		cket.setParent(cols);
		cket.setWidth("16%");
		cket.setLabel("Ket.");

		Rows rs = new Rows();
		rs.setParent(g);

		// --- Section A: Data Vendor ---
		barisSeksiMatriks(rs, "A. DATA VENDOR");
		// Penyedia picker
		Row rp = new Row();
		rp.setParent(rs);
		new Label("Penyedia (master)").setParent(rp);
		new Label("").setParent(rp);
		for (int v = 0; v < JML_VENDOR; v++) {
			final int vv = v;
			penyedia[v] = new AmbilDataPenyediaBanbox();
			penyedia[v].setWidth("95%");
			penyedia[v].setDisabled(!edit);
			penyedia[v].setAttribute("penyedia", detailRef[v].getPenyedia());
			penyedia[v].setValue(detailRef[v].getPenyedia() == null ? "" : detailRef[v].getPenyedia().getNama());
			penyedia[v].setEventListener(new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					Penyedia p = (Penyedia) penyedia[vv].getAttribute("penyedia");
					detailRef[vv].setPenyedia(p);
					if (p != null) {
						if (namaVendor[vv] != null && isBlank(namaVendor[vv].getValue())) {
							namaVendor[vv].setValue(p.getNama() == null ? "" : p.getNama());
						}
						if (picVendor[vv] != null && isBlank(picVendor[vv].getValue()) && p.getKontak() != null) {
							picVendor[vv].setValue(p.getKontak());
						}
					}
				}
			});
			penyedia[v].setParent(rp);
		}
		new Label("").setParent(rp);

		barisVendorTeks(rs, "Nama Vendor", edit, 0);
		barisVendorTeks(rs, "Alamat / Kontak", edit, 1);
		barisVendorTeks(rs, "Jenis Barang/Jasa", edit, 2);
		barisVendorTeks(rs, "PIC Vendor", edit, 3);

		// --- Section B: Alasan Pemilihan Vendor ---
		barisSeksiMatriks(rs, "B. ALASAN PEMILIHAN VENDOR (skor 1 - 5)");
		barisInfoMatriks(rs, "Isi berdasarkan informasi penawaran vendor sebelum digunakan.");
		for (int k = 0; k < JML_KRITERIA; k++) {
			final int kk = k;
			Row r = new Row();
			r.setValign("top");
			r.setParent(rs);
			Div lbl = new Div();
			lbl.setParent(r);
			Label nk = new Label((k + 1) + ". " + KRITERIA[k]);
			nk.setStyle("font-weight:bold;");
			nk.setParent(lbl);
			Label pk = new Label(PERTANYAAN[k]);
			pk.setStyle("font-size:11px; color:#555;");
			pk.setParent(lbl);

			bobot[k] = new MyIntbox();
			bobot[k].setWidth("45px");
			bobot[k].setValue(getBobot(seleksiVendor, k));
			if (edit) {
				bobot[k].addEventListener(Events.ON_CHANGE, new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						recomputeTotals();
					}
				});
				bobot[k].setParent(r);
			} else {
				new Label("" + getBobot(seleksiVendor, k)).setParent(r);
			}

			for (int v = 0; v < JML_VENDOR; v++) {
				nilai[v][k] = new MyIntbox();
				nilai[v][k].setWidth("50px");
				nilai[v][k].setValue(getNilai(detailRef[v], k));
				if (edit) {
					nilai[v][k].addEventListener(Events.ON_CHANGE, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							recomputeTotals();
						}
					});
					nilai[v][k].setParent(r);
				} else {
					Integer nn = getNilai(detailRef[v], k);
					new Label(nn == null ? "-" : "" + nn).setParent(r);
				}
			}

			ket[k] = new MyTextbox();
			ket[k].setWidth("95%");
			ket[k].setValue(getKet(seleksiVendor, k) == null ? "" : getKet(seleksiVendor, k));
			if (edit) {
				ket[k].setParent(r);
			} else {
				new Label(getKet(seleksiVendor, k) == null ? "" : getKet(seleksiVendor, k)).setParent(r);
			}
			// referensi kk agar tidak unused-warning kritikal
			if (kk < 0) {
				continue;
			}
		}

		// --- Baris total tertimbang + ranking (best practice) ---
		Row rTotal = new Row();
		rTotal.setParent(rs);
		rTotal.setStyle("background:#f0fdf4; font-weight:bold;");
		Label lt = new Label("SKOR TERTIMBANG (0-100)");
		lt.setParent(rTotal);
		new Label("").setParent(rTotal);
		for (int v = 0; v < JML_VENDOR; v++) {
			totalLabel[v] = new Label("0");
			totalLabel[v].setStyle("font-weight:bold;");
			totalLabel[v].setParent(rTotal);
		}
		new Label("").setParent(rTotal);

		Row rRank = new Row();
		rRank.setParent(rs);
		rRank.setStyle("background:#fefce8; font-weight:bold;");
		new Label("PERINGKAT").setParent(rRank);
		new Label("").setParent(rRank);
		for (int v = 0; v < JML_VENDOR; v++) {
			rankLabel[v] = new Label("-");
			rankLabel[v].setParent(rRank);
		}
		new Label("").setParent(rRank);
	}

	/** Baris teks per-vendor (nama/alamat/jenis/pic) berdasarkan indeks field 0..3. */
	private void barisVendorTeks(Rows rs, String label, boolean edit, int field) {
		Row r = new Row();
		r.setParent(rs);
		new Label(label).setParent(r);
		new Label("").setParent(r);
		for (int v = 0; v < JML_VENDOR; v++) {
			MyTextbox tb = new MyTextbox();
			tb.setWidth("95%");
			String val = "";
			switch (field) {
			case 0:
				val = detailRef[v].getNamaVendor() == null ? "" : detailRef[v].getNamaVendor();
				namaVendor[v] = tb;
				break;
			case 1:
				val = detailRef[v].getAlamatKontak() == null ? "" : detailRef[v].getAlamatKontak();
				alamatKontak[v] = tb;
				break;
			case 2:
				val = detailRef[v].getJenisBarangJasa() == null ? "" : detailRef[v].getJenisBarangJasa();
				jenisBarangJasa[v] = tb;
				break;
			default:
				val = detailRef[v].getPicVendor() == null ? "" : detailRef[v].getPicVendor();
				picVendor[v] = tb;
				break;
			}
			tb.setValue(val);
			if (edit) {
				tb.setParent(r);
			} else {
				new Label(val).setParent(r);
			}
		}
		new Label("").setParent(r);
	}

	private void barisSeksiMatriks(Rows rs, String judul) {
		Row r = new Row();
		r.setParent(rs);
		r.setStyle("background:#e0e7ff; font-weight:bold;");
		Label l = new Label(judul);
		l.setParent(r);
		for (int i = 0; i < JML_VENDOR + 3; i++) {
			new Label("").setParent(r);
		}
	}

	private void barisInfoMatriks(Rows rs, String info) {
		Row r = new Row();
		r.setParent(rs);
		r.setStyle("background:#f8fafc; color:#64748b; font-size:11px;");
		Label l = new Label(info);
		l.setParent(r);
		for (int i = 0; i < JML_VENDOR + 3; i++) {
			new Label("").setParent(r);
		}
	}

	/** Hitung ulang skor tertimbang & peringkat (dipanggil saat nilai/bobot berubah). */
	private void recomputeTotals() {
		if (totalLabel[0] == null) {
			return;
		}
		double[] skor = new double[JML_VENDOR];
		for (int v = 0; v < JML_VENDOR; v++) {
			double total = 0;
			for (int k = 0; k < JML_KRITERIA; k++) {
				int n = nilai[v][k] == null || nilai[v][k].getValue() == null ? 0 : nilai[v][k].getValue();
				int b = bobot[k] == null || bobot[k].getValue() == null ? 0 : bobot[k].getValue();
				total += n * b;
			}
			skor[v] = Math.round((total / 5.0) * 100.0) / 100.0;
			totalLabel[v].setValue("" + skor[v]);
		}
		// ranking sederhana (1 = tertinggi)
		for (int v = 0; v < JML_VENDOR; v++) {
			int rank = 1;
			boolean adaNilai = skor[v] > 0;
			for (int w = 0; w < JML_VENDOR; w++) {
				if (skor[w] > skor[v]) {
					rank++;
				}
			}
			rankLabel[v].setValue(adaNilai ? ("#" + rank) : "-");
		}
	}

	@Override
	public boolean onSave(Event event) throws Exception {
		if (isBlank(nama.getValue())) {
			MyMessageboxConfig.show("Perihal / Judul pengajuan belum diisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		boolean adaVendor = false;
		for (int v = 0; v < JML_VENDOR; v++) {
			if (penyedia[v].getAttribute("penyedia") != null || !isBlank(namaVendor[v].getValue())) {
				adaVendor = true;
				break;
			}
		}
		if (!adaVendor) {
			MyMessageboxConfig.show("Minimal satu vendor harus diisi (pilih Penyedia atau ketik Nama Vendor).",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (seleksiVendor.getId() != null) {
			seleksiVendor = (SeleksiVendor) session.load(SeleksiVendor.class, seleksiVendor.getId());
		}
		if (seleksiVendor.getDibuatOleh() == null) {
			seleksiVendor.setDibuatOleh(tbmuser);
			seleksiVendor.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			seleksiVendor.setDisposisiSop(disposisiSop);
		}

		seleksiVendor.setNama(nama.getValue());
		seleksiVendor.setJenisPengadaan(jenisPengadaan.getValue());
		seleksiVendor.setTanggal(tanggal.getValue() == null ? new Date() : tanggal.getValue());
		seleksiVendor.setKeterangan(keterangan.getValue());
		for (int k = 0; k < JML_KRITERIA; k++) {
			setBobot(seleksiVendor, k, bobot[k].getValue());
			setKet(seleksiVendor, k, ket[k].getValue());
		}
		// Section C (Vendor Pembanding / alasan) & D (Rekomendasi) dihilangkan dari form -> tidak diisi di sini.
		// Penilai = pengguna yang menindaklanjuti (mengisi skor)
		if (tbmuser != null) {
			seleksiVendor.setNamaPenilai(tbmuser.getUserNama());
			seleksiVendor.setJabatanPenilai(tbmuser.getPegawai() == null ? null : tbmuser.getPegawai().getJabatan());
			seleksiVendor.setTanggalPenilaian(new Date());
		}

		if (seleksiVendor.getId() != null) {
			session.update(seleksiVendor);
		} else {
			seleksiVendor.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			seleksiVendor.setKode(noAgenda);
			if (kode != null) {
				kode.setValue(noAgenda);
			}
			session.save(seleksiVendor);
		}
		session.flush();

		// Simpan detail per vendor
		for (int v = 0; v < JML_VENDOR; v++) {
			Penyedia p = (Penyedia) penyedia[v].getAttribute("penyedia");
			boolean kosong = p == null && isBlank(namaVendor[v].getValue());
			SeleksiVendorDetail d = detailRef[v];
			if (kosong) {
				if (d.getId() != null) {
					session.delete(d);
				}
				continue;
			}
			d.setSeleksiVendor(seleksiVendor);
			d.setPenyedia(p);
			d.setUrutan(v + 1);
			d.setNamaVendor(namaVendor[v].getValue());
			d.setAlamatKontak(alamatKontak[v].getValue());
			d.setJenisBarangJasa(jenisBarangJasa[v].getValue());
			d.setPicVendor(picVendor[v].getValue());
			for (int k = 0; k < JML_KRITERIA; k++) {
				setNilai(d, k, nilai[v][k].getValue());
			}
			session.saveOrUpdate(d);
		}
		session.flush();
		return true;
	}

	/**
	 * Cetak report via JasperReports (webapp/report/Seleksi_Vendor.jrxml). Parameter = field form
	 * (header + Section C/D/E) dan detail 9 kriteria dikirim sebagai {@code maps} (List&lt;Map&gt;).
	 * Engine {@code Report} otomatis meng-compile jrxml→jasper + inject logo/kop/barcode.
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		SeleksiVendor sv = (SeleksiVendor) generalValueObject;
		List<SeleksiVendorDetail> details = new ArrayList<SeleksiVendorDetail>();
		if (sv.getId() != null) {
			details = HibernateUtil.currentSession().createCriteria(SeleksiVendorDetail.class)
					.add(Restrictions.eq("seleksiVendor", sv)).addOrder(Order.asc("urutan")).list();
		}
		SeleksiVendorDetail[] d = new SeleksiVendorDetail[JML_VENDOR];
		for (int v = 0; v < JML_VENDOR; v++) {
			d[v] = v < details.size() ? details.get(v) : null;
		}

		java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
		parameters.put("kode", sv.getKode());
		parameters.put("perihal", sv.getNama());
		parameters.put("jenis_pengadaan", sv.getJenisPengadaan());
		parameters.put("tanggal", sv.getTanggal() == null ? null : Common.dateFormat.get().format(sv.getTanggal()));
		parameters.put("keterangan", sv.getKeterangan());

		double[] skor = new double[JML_VENDOR];
		for (int v = 0; v < JML_VENDOR; v++) {
			String nm = d[v] == null ? null
					: (d[v].getNamaVendor() != null && !d[v].getNamaVendor().trim().isEmpty() ? d[v].getNamaVendor()
							: (d[v].getPenyedia() == null ? null : d[v].getPenyedia().getNama()));
			parameters.put("nama_vendor_" + (v + 1), nm);
			parameters.put("alamat_vendor_" + (v + 1), d[v] == null ? null : d[v].getAlamatKontak());
			parameters.put("jenis_vendor_" + (v + 1), d[v] == null ? null : d[v].getJenisBarangJasa());
			parameters.put("pic_vendor_" + (v + 1), d[v] == null ? null : d[v].getPicVendor());
			skor[v] = d[v] == null || d[v].getSkorTertimbang() == null ? 0 : d[v].getSkorTertimbang();
			parameters.put("skor_" + (v + 1), d[v] == null ? "-" : "" + skor[v]);
		}
		for (int v = 0; v < JML_VENDOR; v++) {
			if (d[v] == null || skor[v] <= 0) {
				parameters.put("peringkat_" + (v + 1), "-");
				continue;
			}
			int rank = 1;
			for (int w = 0; w < JML_VENDOR; w++) {
				if (skor[w] > skor[v]) {
					rank++;
				}
			}
			parameters.put("peringkat_" + (v + 1), "#" + rank);
		}

		parameters.put("pembanding_1", sv.getVendorPembanding1());
		parameters.put("pembanding_2", sv.getVendorPembanding2());
		parameters.put("pembanding_3", sv.getVendorPembanding3());
		parameters.put("alasan_dipilih", sv.getAlasanDipilih());
		parameters.put("rekomendasi", sv.getRekomendasi());
		parameters.put("rekomendasi_nomor", sv.getRekomendasiNomor() == null ? null : "" + sv.getRekomendasiNomor());
		parameters.put("alasan_utama", sv.getAlasanUtama());
		parameters.put("nama_penilai", sv.getNamaPenilai());
		parameters.put("jabatan_penilai", sv.getJabatanPenilai());
		parameters.put("tanggal_penilaian", sv.getTanggalPenilaian() == null ? null
				: Common.dateFormat.get().format(sv.getTanggalPenilaian()));

		// Detail 9 kriteria (Section B) → List<Map>
		List<java.util.Map<String, Object>> maps = new ArrayList<java.util.Map<String, Object>>();
		for (int k = 0; k < JML_KRITERIA; k++) {
			java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
			m.put("no", k + 1);
			m.put("kriteria", KRITERIA[k]);
			m.put("pertanyaan", PERTANYAAN[k]);
			m.put("bobot", "" + (getBobot(sv, k) == null ? 0 : getBobot(sv, k)));
			for (int v = 0; v < JML_VENDOR; v++) {
				Integer nn = d[v] == null ? null : getNilai(d[v], k);
				m.put("nilai_" + (v + 1), nn == null ? "-" : "" + nn);
			}
			m.put("ket", getKet(sv, k));
			maps.add(m);
		}
		parameters.put("maps", maps);

		return ais.action.report.Report.generateFileReportSimple(ais.action.report.Report.PDF, parameters,
				"Seleksi_Vendor");
	}

	// ================= Nomor Surat =================

	private String generateCode(boolean tambah) {
		NomorSuratAlurPengadaan cfg = NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR_DATA;
		if (cfg == null || cfg.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}
		Long index = cfg.getNomorSurat().getGunakanIndexUrut() ? cfg.getNomorSurat().getNomorIndex()
				: getindex(cfg.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(cfg.getNomorSurat());
		}
		String noAgenda = cfg.getNomorSurat().format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(SeleksiVendor.class, noAgenda);
	}

	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(SeleksiVendor.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)
				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)
						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))
				.add(nomorSurat.getResetTiap() != null
						&& (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| nomorSurat.getResetTiap().before(sekarang))
										? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
										: Restrictions.sqlRestriction("true"))
				.setProjection(Projections.rowCount()).uniqueResult();
		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	// ================= util kecil =================

	private void rowKV(Rows rows, String label, org.zkoss.zk.ui.Component control) {
		rowKVret(rows, label, control);
	}

	private MyFormRow rowKVret(Rows rows, String label, org.zkoss.zk.ui.Component control) {
		MyFormRow r = new MyFormRow();
		r.setValign("top");
		r.setParent(rows);
		r.appendChild(new MyLabelConfig(label));
		r.appendChild(control);
		return r;
	}

	private void headerSeksi(Rows rows, String judul) {
		Row r = new Row();
		r.setParent(rows);
		r.setStyle("background:#e0e7ff; font-weight:bold;");
		Cell c = new Cell();
		c.setColspan(2);
		c.setParent(r);
		Label l = new Label(judul);
		l.setStyle("font-weight:bold;");
		l.setParent(c);
	}

	private MyTextbox teksNilai(String val) {
		MyTextbox tb = new MyTextbox();
		tb.setWidth("95%");
		tb.setValue(val == null ? "" : val);
		return tb;
	}

	private MyTextbox teksArea(String val) {
		MyTextbox tb = new MyTextbox();
		tb.setMultiline(true);
		tb.setRows(2);
		tb.setWidth("95%");
		tb.setValue(val == null ? "" : val);
		return tb;
	}

	private static String nilaiStr(MyTextbox tb) {
		return tb == null || tb.getValue() == null ? "" : tb.getValue();
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static String nvl(String s) {
		return s == null || s.trim().isEmpty() ? "-" : s;
	}

	private static String romawi(int n) {
		switch (n) {
		case 1:
			return "I";
		case 2:
			return "II";
		case 3:
			return "III";
		default:
			return "" + n;
		}
	}

	private static PdfPCell selHead(String s, Font f) {
		PdfPCell c = new PdfPCell(new Paragraph(s, f));
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		c.setBackgroundColor(new com.itextpdf.text.BaseColor(224, 231, 255));
		c.setPadding(3);
		return c;
	}

	private static PdfPCell selCell(String s, Font f) {
		PdfPCell c = new PdfPCell(new Paragraph(s == null ? "" : s, f));
		c.setPadding(3);
		return c;
	}

	// ---- mapping index <-> getter/setter ----

	private static Integer getBobot(SeleksiVendor h, int k) {
		switch (k) {
		case 0: return h.getBobotHarga();
		case 1: return h.getBobotSpesifikasi();
		case 2: return h.getBobotKetersediaan();
		case 3: return h.getBobotKejelasan();
		case 4: return h.getBobotLegalitas();
		case 5: return h.getBobotPengalaman();
		case 6: return h.getBobotResponsif();
		case 7: return h.getBobotPembayaran();
		default: return h.getBobotReputasi();
		}
	}

	private static void setBobot(SeleksiVendor h, int k, Integer v) {
		switch (k) {
		case 0: h.setBobotHarga(v); break;
		case 1: h.setBobotSpesifikasi(v); break;
		case 2: h.setBobotKetersediaan(v); break;
		case 3: h.setBobotKejelasan(v); break;
		case 4: h.setBobotLegalitas(v); break;
		case 5: h.setBobotPengalaman(v); break;
		case 6: h.setBobotResponsif(v); break;
		case 7: h.setBobotPembayaran(v); break;
		default: h.setBobotReputasi(v); break;
		}
	}

	private static String getKet(SeleksiVendor h, int k) {
		switch (k) {
		case 0: return h.getKetHarga();
		case 1: return h.getKetSpesifikasi();
		case 2: return h.getKetKetersediaan();
		case 3: return h.getKetKejelasan();
		case 4: return h.getKetLegalitas();
		case 5: return h.getKetPengalaman();
		case 6: return h.getKetResponsif();
		case 7: return h.getKetPembayaran();
		default: return h.getKetReputasi();
		}
	}

	private static void setKet(SeleksiVendor h, int k, String v) {
		switch (k) {
		case 0: h.setKetHarga(v); break;
		case 1: h.setKetSpesifikasi(v); break;
		case 2: h.setKetKetersediaan(v); break;
		case 3: h.setKetKejelasan(v); break;
		case 4: h.setKetLegalitas(v); break;
		case 5: h.setKetPengalaman(v); break;
		case 6: h.setKetResponsif(v); break;
		case 7: h.setKetPembayaran(v); break;
		default: h.setKetReputasi(v); break;
		}
	}

	private static Integer getNilai(SeleksiVendorDetail d, int k) {
		switch (k) {
		case 0: return d.getNilaiHarga();
		case 1: return d.getNilaiSpesifikasi();
		case 2: return d.getNilaiKetersediaan();
		case 3: return d.getNilaiKejelasan();
		case 4: return d.getNilaiLegalitas();
		case 5: return d.getNilaiPengalaman();
		case 6: return d.getNilaiResponsif();
		case 7: return d.getNilaiPembayaran();
		default: return d.getNilaiReputasi();
		}
	}

	private static void setNilai(SeleksiVendorDetail d, int k, Integer v) {
		switch (k) {
		case 0: d.setNilaiHarga(v); break;
		case 1: d.setNilaiSpesifikasi(v); break;
		case 2: d.setNilaiKetersediaan(v); break;
		case 3: d.setNilaiKejelasan(v); break;
		case 4: d.setNilaiLegalitas(v); break;
		case 5: d.setNilaiPengalaman(v); break;
		case 6: d.setNilaiResponsif(v); break;
		case 7: d.setNilaiPembayaran(v); break;
		default: d.setNilaiReputasi(v); break;
		}
	}

}
