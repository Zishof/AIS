package ais.action.master.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h2>Pembayaran Online Mahasiswa — layar hub eksperimental "cara baru".</h2>
 *
 * <p>
 * Klon struktural {@link ais.action.master.sekolah.helper.PembayaranOnline} (sisi
 * siswa) untuk Mahasiswa/{@link BiodataCalonMahasiswa}: pemilih mahasiswa/calon di
 * North, grid tagihan checkable + riwayat cicilan di Center (dua kolom via
 * {@link MyPortallayout}), toolbar Refresh/Cetak/Surat Tagihan/Wizard/Bayar
 * Tunai/History/Restore. <b>SEPENUHNYA ADDITIVE</b> — tidak mengubah satu baris pun
 * di {@code DaftarUlangMahasiswaBaruAction}/{@code DaftarUlangMahasiswaLamaAction};
 * dibuka lewat tombol terpisah sebagai alternatif eksperimental (permintaan user:
 * "coba bayar via cara baru").
 * </p>
 *
 * <h3>Sumber data (REUSE, bukan reinvent)</h3>
 * <ul>
 *   <li>Template biaya: {@link PembayaranUtilHelper#getDetailBiayaMahasiswa} /
 *       {@link PembayaranUtilHelper#getDetailBiayaCalonMahasiswa}.</li>
 *   <li>Header Kegiatan: {@link KegiatanHelper#checkKegiatanMahasiswa} /
 *       {@link KegiatanHelper#checkKegiatanCalonMahasiswa}.</li>
 *   <li>Riwayat cicilan: {@link KegiatanPersistenceHelper#ambilCicilan}.</li>
 * </ul>
 *
 * <h3>Cakupan Fase 1 (keputusan skop, lihat komentar di masing-masing tombol)</h3>
 * <p>
 * <b>Wizard Pembayaran</b> (12 kanal gateway) hanya tersedia utk Mahasiswa —
 * {@link WizardPembayaranMhsHelper} saat ini murni terikat ke {@code Mahasiswa} di
 * ~2000 baris kode yang baru selesai diperbaiki; me-retrofit dukungan calon
 * mahasiswa ke sana berisiko terlalu besar utk layar eksperimental ini. <b>Bayar
 * Tunai/Manual</b> tersedia utk KEDUA jenis pengguna — dibangun mandiri di sini,
 * mengadaptasi pola {@code WizardPembayaranMhsHelper.bayarTunai} yang sudah terbukti
 * (satu {@link Kegiatan} + satu {@link CicilanPembayaran} per item terpilih, satu
 * transaksi), TANPA menyentuh berkas Wizard sama sekali. Tombol kanal bank
 * langsung (BNI/BRI/BSI/VA, dst.) SENGAJA belum dibangun di sini — akan jadi
 * follow-up terpisah bila diperlukan, agar tidak menduplikasi ~25 method dispatch
 * gateway yang sudah ada &amp; teruji di dalam Wizard.
 * </p>
 *
 * <p>Kompatibilitas: Java 1.7 / ZK 5 CE — tanpa lambda/stream.</p>
 */
public class PembayaranOnlineMahasiswa extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	// ============================================================ FIELDS (autowire dari zul)
	/**
	 * Window utama, diisi ZK secara otomatis dari {@code id="window"} pada
	 * {@code pembayaran_online_mahasiswa.zul} ketika komposer ini dipakai lewat entry point
	 * {@code doAfterCompose}, atau dibuat manual oleh {@link #onViewExternal} ketika dipakai
	 * sebagai window modal berdiri sendiri.
	 */
	private MyWindow window;

	// ============================================================ FIELDS (dibangun manual)
	/** Bandbox pemilih Mahasiswa pada North; disembunyikan bila {@link #calonMahasiswa} terisi (lihat {@link #updateVisibilitasPemilihStudent()}). */
	private AmbilDataMahasiswaBanbox mahasiswaBanbox;
	/** Bandbox pemilih Calon Mahasiswa (daftar ulang baru) pada North; disembunyikan bila {@link #mahasiswa} terisi. */
	private AmbilDataCalonMahasiswaDaftarUlangBaruBanbox calonBanbox;
	/** Grup label+bandbox "Mahasiswa" pada North; visibilitasnya diatur oleh {@link #updateVisibilitasPemilihStudent()}. */
	private Hbox groupMahasiswa;
	/** Grup label+bandbox "Calon Mahasiswa" pada North; visibilitasnya diatur oleh {@link #updateVisibilitasPemilihStudent()}. */
	private Hbox groupCalon;
	/** Kombo jenis pembayaran ({@link JenisKegiatan}), diisi oleh {@code Common#initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa}; perubahan memicu {@link #reload(boolean)}. */
	private Combobox jenisKegiatanCombo;
	/** Input semester, diisi awal oleh {@link #isiSemesterDefault()}; perubahan memicu {@link #reload(boolean)}. */
	private Intbox semesterBox;

	/** Mahasiswa yang sedang ditampilkan/dikelola tagihannya; saling eksklusif dengan {@link #calonMahasiswa}. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa (daftar ulang) yang sedang ditampilkan/dikelola tagihannya; saling eksklusif dengan {@link #mahasiswa}. */
	private BiodataCalonMahasiswa calonMahasiswa;
	/** {@link Kegiatan} (header transaksi pembayaran) aktif untuk kombinasi mahasiswa/calon+jenis+semester saat ini, dimuat/dibuat oleh {@link #reload(boolean)}/{@link #simpanPembayaranTunai}. */
	private Kegiatan kegiatanAktif;

	/** Kolom kiri portal (60%): daftar tagihan checkable, dirender oleh {@link #renderGridTagihan()}. */
	private Component colKiri;
	/** Kolom kanan portal (40%): riwayat cicilan, dirender oleh {@link #renderRiwayat(boolean)}. */
	private Component colKanan;
	/** Label total nominal yang akan dibayar (jumlah baris terpilih), diperbarui oleh {@link #renderTotal()}. */
	private MyLabelBold totalLabel;
	/** Label terbilang (dalam kata) dari {@link #totalLabel}, diperbarui oleh {@link #renderTotal()}. */
	private MyLabelBoldAja terbilangLabel;

	/** Daftar baris tagihan checkable yang sedang ditampilkan di grid; dibangun ulang oleh {@link #susunBarisDariTemplate}. */
	private List<BarisTagihan> barisList = new ArrayList<BarisTagihan>();

	/** Baris tagihan checkable di grid — satu per DetailBiaya/PengaturanPembayaranBulanan. */
	private static final class BarisTagihan {
		/** Template rincian biaya (item + bayarKe) sumber baris ini; {@code null} bila baris berasal murni dari {@link #ppb}. */
		DetailBiaya detailBiaya;
		/** Pengaturan pembayaran bulanan sumber baris ini (mode cicilan bulanan); {@code null} bila baris berasal dari {@link #detailBiaya} biasa. */
		PengaturanPembayaranBulanan ppb;
		/** Nama item biaya untuk ditampilkan (dari {@link #detailBiaya}'s {@link ItemBiaya}). */
		String namaItem;
		/** Label bulan (mis. "Bulan 3") untuk baris bermode {@link #ppb}; {@code null} untuk baris biaya biasa. */
		String namaBulan;
		/** Nominal tagihan penuh baris ini (hasil {@code Kegiatan#ambilJumlahTagihan}). */
		double nominal;
		/** Total yang sudah dibayar untuk baris ini, dihitung langsung dari {@link CicilanPembayaran} tersimpan (bukan cache Kegiatan). */
		double sudahDibayar;
		/** Sisa kekurangan ({@code max(0, nominal - sudahDibayar)}); dipakai sebagai batas atas nominal yang boleh dibayar. */
		double kekurangan;
		/** Status centang awal baris (default tercentang bila masih ada kekurangan &gt; 0.1). */
		boolean dipilih;
		/** Komponen input nominal yang akan dibayar untuk baris ini pada grid; nilainya dibaca saat {@link #bayarTunaiManual()}/{@link #simpanPembayaranTunai}. */
		MyDoublebox nominalBayarBox;
		/** Komponen checkbox "Pilih" baris ini pada grid. */
		MyCheckboxConfig checkboxConfig;
	}

	// ============================================================ ENTRY POINT (URL lama, opsional)
	/**
	 * Entry point ZK otomatis saat {@code pembayaran_online_mahasiswa.zul} (
	 * {@code apply="ais.action.master.helper.PembayaranOnlineMahasiswa"}) dimuat. Tanpa
	 * parameter {@code mahasiswa}/{@code calon_mahasiswa}, jatuh ke mahasiswa/calon mahasiswa
	 * milik {@link Tbmuser} yang sedang login (self-service) — tidak berubah dari semula.
	 *
	 * <p><b>GERBANG KEAMANAN (fail-closed):</b> parameter URL {@code mahasiswa}/
	 * {@code calon_mahasiswa} HANYA diterima bila id-nya sama persis dengan
	 * {@code tbmuser.getMahasiswa()}/{@code tbmuser.getBiodataCalonMahasiswa()} milik pengguna
	 * yang sedang login — id itu dibandingkan langsung ke objek milik sesi login, TANPA query
	 * database berdasarkan id dari URL. Tidak ada pengecualian untuk staf/admin: tidak ada satu
	 * pun tempat di codebase ini yang membentuk tautan ke halaman ini dengan parameter tersebut
	 * (termasuk kelon struktural {@link ais.action.master.sekolah.helper.PembayaranOnline} yang
	 * punya lubang identik) — satu-satunya jalur staf yang didukung/didokumentasikan untuk
	 * membuka data mahasiswa/calon LAIN adalah {@link #onViewExternal}, yang sudah menggerbangi
	 * dirinya sendiri dengan {@link Common#doCheckSecurity()} sebelum memanggil method ini.
	 * Ketidakcocokan diaudit ({@link ais.common.ErrorAuditUtil}) dan ditolak dengan pesan —
	 * TIDAK diam-diam di-fallback ke data milik sendiri, agar percobaan akses tak sah tidak
	 * tersamarkan sebagai penggunaan normal.</p>
	 */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null) {
			tampilAksesDitolak("Sesi login tidak valid. Silakan login kembali.");
			return;
		}

		String pMahasiswa = ExecutionsCtrl.getCurrent().getParameter("mahasiswa");
		String pCalon = ExecutionsCtrl.getCurrent().getParameter("calon_mahasiswa");

		if (pMahasiswa != null || pCalon != null) {
			boolean cocokMahasiswa = pMahasiswa != null && pCalon == null && tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId() != null
					&& tbmuser.getMahasiswa().getId().toString().equals(pMahasiswa.trim());
			boolean cocokCalon = pCalon != null && pMahasiswa == null && tbmuser.getBiodataCalonMahasiswa() != null
					&& tbmuser.getBiodataCalonMahasiswa().getId() != null
					&& tbmuser.getBiodataCalonMahasiswa().getId().toString().equals(pCalon.trim());
			if (!cocokMahasiswa && !cocokCalon) {
				ais.common.ErrorAuditUtil.record(
						new SecurityException("Percobaan akses id mahasiswa/calon mahasiswa lain via parameter URL: "
								+ "mahasiswa=" + pMahasiswa + " calon_mahasiswa=" + pCalon + " oleh userId="
								+ tbmuser.getId() + " userNama=" + tbmuser.getUserNama()),
						"SECURITY PembayaranOnlineMahasiswa.doAfterCompose - akses lintas kepemilikan via parameter URL ditolak");
				tampilAksesDitolak("Anda tidak berhak mengakses data pembayaran mahasiswa/calon mahasiswa lain "
						+ "lewat tautan ini. Staf/admin: gunakan tombol \"Coba Cara Baru (Eksperimental)\" pada "
						+ "layar Informasi Pembayaran Mahasiswa.");
				return;
			}
			// id dari URL sudah diverifikasi sama dengan milik tbmuser sendiri — pakai objek
			// dari sesi login (tepercaya), BUKAN hasil query ulang berdasarkan id URL.
			if (pMahasiswa != null) {
				mahasiswa = tbmuser.getMahasiswa();
			} else {
				calonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
			}
		} else if (tbmuser.getMahasiswa() != null) {
			mahasiswa = tbmuser.getMahasiswa();
		} else if (tbmuser.getBiodataCalonMahasiswa() != null) {
			calonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
		}

		buildUI();
	}

	/**
	 * Bersihkan {@link #window} lalu tampilkan satu pesan penolakan akses (dipakai
	 * {@link #doAfterCompose} saat sesi tidak valid atau id URL tidak cocok dengan
	 * kepemilikan pengguna login). Tidak memuat/membangun bagian UI lain apa pun, sehingga
	 * tombol "Bayar Tunai/Manual" dkk. tidak pernah dirender untuk permintaan yang ditolak.
	 */
	private void tampilAksesDitolak(String pesan) {
		Common.clear(window);
		new ais.ui.util.MyLabelConfig(pesan).setParent(window);
	}

	/**
	 * Pintu masuk utama (pola {@code onViewExternal} yang sudah dipakai
	 * {@code InformasiPembayaranMahasiswaAction}): buka layar ini sebagai
	 * {@link MyWindow} langsung, tanpa zul/iframe/URL. Boleh isi salah satu
	 * (mahasiswa ATAU calonMahasiswa) atau keduanya null (admin memilih manual
	 * dari bandbox North setelah jendela terbuka).
	 */
	public static PembayaranOnlineMahasiswa onViewExternal(Mahasiswa mahasiswaTarget,
			BiodataCalonMahasiswa calonMahasiswaTarget) throws Exception {
		Common.doCheckSecurity();
		final PembayaranOnlineMahasiswa action = new PembayaranOnlineMahasiswa();
		action.mahasiswa = mahasiswaTarget;
		action.calonMahasiswa = calonMahasiswaTarget;
		action.window = new MyWindow("Pembayaran Online Mahasiswa (Coba Cara Baru)", "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(action.window);
		action.window.setHeight("92%");
		action.window.setWidth(Common.isMobile() ? "97%" : "90%");
		action.buildUI();
		action.window.setVisible(true);
		action.window.onModal();
		return action;
	}

	// ============================================================ BANGUN UI
	/**
	 * Membangun seluruh UI layar: North berisi baris pemilih (bandbox Mahasiswa/Calon Mahasiswa,
	 * kombo jenis pembayaran, input semester) dan toolbar aksi (Refresh, Cetak Bukti Pembayaran,
	 * Surat Tagihan, Wizard Pembayaran — khusus Mahasiswa, History, Restore); Center berisi
	 * portal layout dua kolom ({@link #colKiri}/{@link #colKanan}) untuk grid tagihan dan
	 * riwayat. Diakhiri memuat data awal lewat {@link #reload(boolean)}.
	 */
	private void buildUI() throws Exception {
		Common.clear(window);

		Borderlayout bl = new ais.ui.util.MyBorderlayout();
		bl.setWidth("100%");
		bl.setHeight("100%");
		bl.setParent(window);

		North north = new North();
		north.setBorder("none");
		north.setStyle("padding:8px;");
		north.setParent(bl);

		Vbox northBox = new Vbox();
		northBox.setWidth("100%");
		northBox.setParent(north);

		Hbox pickerRow = new Hbox();
		pickerRow.setWidth("100%");
		pickerRow.setStyle("flex-wrap:wrap;align-items:center;gap:8px;");
		pickerRow.setParent(northBox);

		groupMahasiswa = new Hbox();
		groupMahasiswa.setStyle("align-items:center;gap:8px;");
		groupMahasiswa.setParent(pickerRow);
		groupMahasiswa.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa :"));
		mahasiswaBanbox = new AmbilDataMahasiswaBanbox();
		mahasiswaBanbox.setParent(groupMahasiswa);
		if (mahasiswa != null) {
			mahasiswaBanbox.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			mahasiswaBanbox.setAttribute("mahasiswa", mahasiswa);
		}
		mahasiswaBanbox.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object v = mahasiswaBanbox.getAttribute("mahasiswa");
				if (v instanceof Mahasiswa) {
					mahasiswa = (Mahasiswa) v;
					calonMahasiswa = null;
					if (calonBanbox != null) {
						calonBanbox.setValue("");
					}
					updateVisibilitasPemilihStudent();
					isiSemesterDefault();
					reload(false);
				}
			}
		});

		groupCalon = new Hbox();
		groupCalon.setStyle("align-items:center;gap:8px;");
		groupCalon.setParent(pickerRow);
		groupCalon.appendChild(new ais.ui.util.MyLabelConfig("Calon Mahasiswa :"));
		calonBanbox = new AmbilDataCalonMahasiswaDaftarUlangBaruBanbox();
		calonBanbox.setParent(groupCalon);
		if (calonMahasiswa != null) {
			calonBanbox.setValue(calonMahasiswa.getNoRegistrasi() + " - " + calonMahasiswa.getNama());
			calonBanbox.setAttribute("calonMahasiswa", calonMahasiswa);
		}
		calonBanbox.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object v = calonBanbox.getAttribute("calonMahasiswa");
				if (v instanceof BiodataCalonMahasiswa) {
					calonMahasiswa = (BiodataCalonMahasiswa) v;
					mahasiswa = null;
					if (mahasiswaBanbox != null) {
						mahasiswaBanbox.setValue("");
					}
					updateVisibilitasPemilihStudent();
					isiSemesterDefault();
					reload(false);
				}
			}
		});
		updateVisibilitasPemilihStudent();

		pickerRow.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran :"));
		jenisKegiatanCombo = new Combobox();
		jenisKegiatanCombo.setReadonly(true);
		jenisKegiatanCombo.setWidth("220px");
		Common.initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(jenisKegiatanCombo);
		jenisKegiatanCombo.setParent(pickerRow);
		jenisKegiatanCombo.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(false);
			}
		});

		pickerRow.appendChild(new ais.ui.util.MyLabelConfig("Semester :"));
		semesterBox = new Intbox();
		semesterBox.setCols(3);
		semesterBox.setParent(pickerRow);
		semesterBox.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(false);
			}
		});
		isiSemesterDefault();

		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setParent(northBox);

		MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh-cw.svg");
		btnRefresh.setParent(toolbar);
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(true);
			}
		});

		MyToolbarbuttonConfig btnCetak = new MyToolbarbuttonConfig("Cetak Bukti Pembayaran", "/img/print.png");
		btnCetak.setParent(toolbar);
		btnCetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kegiatanAktif == null || kegiatanAktif.getId() == null) {
					peringatanBelumAdaData();
					return;
				}
				if (mahasiswa != null) {
					CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatanAktif, false);
				} else {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatanAktif, false);
				}
			}
		});

		MyToolbarbuttonConfig btnSurat = new MyToolbarbuttonConfig("Surat Tagihan", "/img/svg/money-bills.svg");
		btnSurat.setParent(toolbar);
		btnSurat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					JenisKegiatan jk = jenisKegiatanTerpilih();
					if (jk == null) {
						peringatanBelumAdaData();
						return;
					}
					if (mahasiswa != null) {
						CommonReportHelper.prosesSuratTagihan(mahasiswa, hitungTahunAkademikMahasiswa(semesterBox.getValue()),
								semesterBox.getValue(), kegiatanAktif == null ? null : kegiatanAktif.getJadwalPembayaran());
					} else if (calonMahasiswa != null) {
						CommonReportHelper.prosesSuratTagihan(calonMahasiswa, jk, kegiatanAktif,
								semesterBox.getValue(), kegiatanAktif == null ? null : kegiatanAktif.getJadwalPembayaran());
					} else {
						peringatanBelumAdaData();
					}
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit PembayaranOnlineMahasiswa surat tagihan");
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		// WIZARD PEMBAYARAN: hanya utk Mahasiswa (lihat javadoc class ini — keputusan skop
		// Fase 1). Utk calon mahasiswa, tombol disembunyikan; gunakan "Bayar Tunai/Manual".
		final MyToolbarbuttonConfig btnWizard = new MyToolbarbuttonConfig("Wizard Pembayaran",
				"/img/svg/payments.svg");
		btnWizard.setParent(toolbar);
		btnWizard.setVisible(mahasiswa != null);
		btnWizard.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (mahasiswa == null) {
					return;
				}
				WizardPembayaranMhsHelper.buka(mahasiswa, new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						reload(true);
					}
				});
			}
		});

		MyToolbarbuttonConfig btnHistory = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		btnHistory.setParent(toolbar);
		btnHistory.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				bukaRevisi(false);
			}
		});

		MyToolbarbuttonConfig btnRestore = new MyToolbarbuttonConfig("Restore", "/img/refresh.gif");
		btnRestore.setParent(toolbar);
		btnRestore.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				bukaRevisi(true);
			}
		});

		Center center = new Center();
		center.setBorder("none");
		center.setStyle("overflow:auto;");
		center.setParent(bl);

		MyPortallayout portal = new MyPortallayout();
		portal.setWidth("100%");
		portal.setParent(center);

		MyPortalchildren kiri = new MyPortalchildren();
		kiri.setWidth("60%");
		kiri.setParent(portal);
		colKiri = kiri;

		MyPortalchildren kanan = new MyPortalchildren();
		kanan.setWidth("40%");
		kanan.setParent(portal);
		colKanan = kanan;

		reload(false);
	}

	/**
	 * Sembunyikan pemilih (label+bandbox) sisi yang TIDAK aktif — bila Mahasiswa sudah
	 * terisi, "Calon Mahasiswa" disembunyikan (bukan sekadar dibiarkan kosong), begitu pula
	 * sebaliknya. Bila keduanya masih kosong (admin belum memilih), tampilkan dua-duanya.
	 */
	private void updateVisibilitasPemilihStudent() {
		if (groupMahasiswa != null) {
			groupMahasiswa.setVisible(calonMahasiswa == null);
		}
		if (groupCalon != null) {
			groupCalon.setVisible(mahasiswa == null);
		}
	}

	/**
	 * Mengisi {@link #semesterBox} dengan nilai default: semester berjalan hasil hitung
	 * {@code Common#getSemester} untuk {@link #mahasiswa} (berdasar tahun angkatan/status
	 * ganjil-genap saat ini), atau {@code 1} untuk {@link #calonMahasiswa}/bila terjadi
	 * kegagalan penghitungan.
	 */
	private void isiSemesterDefault() {
		try {
			if (mahasiswa != null) {
				Boolean ganjil = Common.isNowSemensterGanjil();
				Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
						Boolean.TRUE.equals(ganjil) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
				semesterBox.setValue(smt == null ? 1 : smt);
			} else if (calonMahasiswa != null) {
				semesterBox.setValue(1);
			}
		} catch (Exception e) {
			semesterBox.setValue(1);
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PembayaranOnlineMahasiswa.isiSemesterDefault");
		}
	}

	/** @return {@link JenisKegiatan} yang sedang terpilih pada {@link #jenisKegiatanCombo}, atau {@code null} bila belum ada yang dipilih */
	private JenisKegiatan jenisKegiatanTerpilih() {
		return jenisKegiatanCombo.getSelectedItem() == null ? null
				: (JenisKegiatan) jenisKegiatanCombo.getSelectedItem().getValue();
	}

	/**
	 * Hitung tahun akademik (mis. "2025/2026") mahasiswa utk semester tertentu.
	 * {@code Mahasiswa} (beda dari {@link BiodataCalonMahasiswa}) TIDAK punya field
	 * {@code tahunAkademik} langsung — dihitung dari angkatan+semester, pola sama
	 * dgn {@code KegiatanProsesHeper} ({@code String Ta = tahun + "/" + (tahun+1)}).
	 */
	private String hitungTahunAkademikMahasiswa(Integer smt) {
		try {
			Integer tahunAkademikMulai = Common.getTahunAkademik(smt, mahasiswa.getTahunangkatan(),
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			return tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		} catch (Exception eTa) {
			return ais.action.ws.util.CommonUtil.generateTahunAkademik();
		}
	}

	/** Menampilkan pesan peringatan generik "belum ada data tagihan" (dipakai beberapa tombol toolbar saat {@link #kegiatanAktif}/jenis pembayaran belum siap). */
	private void peringatanBelumAdaData() throws Exception {
		MyMessageboxConfig.show(
				"Mohon maaf, belum ada data tagihan untuk kombinasi mahasiswa/jenis pembayaran/semester ini. Langkah yang dapat dilakukan: (1) pilih mahasiswa atau calon mahasiswa terlebih dahulu; (2) pastikan jenis pembayaran dan semester sudah sesuai; (3) tekan Refresh.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
	}

	/**
	 * Membuka {@link RevisiCicilanPembayaranHelper} (riwayat revisi/audit trail) untuk
	 * {@link #kegiatanAktif}; menampilkan peringatan bila belum ada {@link #kegiatanAktif}.
	 *
	 * @param fokusRestore bila {@code true}, langsung buka tab "seluruh data" (mode restore, dipakai tombol Restore); {@code false} untuk tombol History biasa
	 */
	private void bukaRevisi(final boolean fokusRestore) {
		try {
			if (kegiatanAktif == null || kegiatanAktif.getId() == null) {
				peringatanBelumAdaData();
				return;
			}
			RevisiCicilanPembayaranHelper revisiHelper = new RevisiCicilanPembayaranHelper(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					reload(true);
				}
			}, kegiatanAktif);
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
			revisiHelper.setVisible(true);
			if (fokusRestore) {
				revisiHelper.bukaTabSeluruhData();
			}
			revisiHelper.onModal();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PembayaranOnlineMahasiswa.bukaRevisi");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ============================================================ MUAT DATA
	/**
	 * Memuat ulang seluruh data tagihan+riwayat untuk kombinasi {@link #mahasiswa}/
	 * {@link #calonMahasiswa} + jenis pembayaran + semester saat ini: mengambil/membuat
	 * {@link #kegiatanAktif} (via {@code KegiatanHelper#checkKegiatanMahasiswa}/
	 * {@code checkKegiatanCalonMahasiswa}), template rincian biaya (via
	 * {@code PembayaranUtilHelper#getDetailBiayaMahasiswa}/{@code getDetailBiayaCalonMahasiswa} —
	 * dengan permintaan ulang mode {@code "-1"} bila template mengandung
	 * {@link PengaturanPembayaranBulanan} agar seluruh bulan ikut termuat), lalu membangun
	 * {@link #barisList} ({@link #susunBarisDariTemplate}) dan merender ulang grid tagihan
	 * ({@link #renderGridTagihan()}) serta riwayat ({@link #renderRiwayat(boolean)}). Bila
	 * mahasiswa/calon/jenis/semester belum lengkap, cukup menampilkan pesan kosong.
	 *
	 * @param refresh bila {@code true}, paksa segarkan cache (Kegiatan, template biaya, riwayat cicilan) alih-alih memakai cache
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void reload(boolean refresh) {
		try {
			Common.clear(colKiri);
			Common.clear(colKanan);
			barisList = new ArrayList<BarisTagihan>();
			kegiatanAktif = null;

			JenisKegiatan jk = jenisKegiatanTerpilih();
			Integer smt = semesterBox.getValue();
			if (jk == null || smt == null || (mahasiswa == null && calonMahasiswa == null)) {
				tampilKosong(colKiri, "Pilih mahasiswa/calon mahasiswa, jenis pembayaran, dan semester terlebih dahulu.");
				return;
			}

			java.util.Collection detailBiayas;
			Session session = HibernateUtil.currentSession();
			if (calonMahasiswa != null) {
				kegiatanAktif = KegiatanHelper.checkKegiatanCalonMahasiswa(jk, calonMahasiswa, smt,
						calonMahasiswa.getTahunAkademik(), refresh, false, null, session);
				ais.database.model.Jurusan jurusanCalon = calonMahasiswa.getProdiLulus();
				if (jurusanCalon == null || jurusanCalon.getId() == null) {
					jurusanCalon = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
							: calonMahasiswa.getProdi1();
				}
				detailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMahasiswa, jk, jurusanCalon,
						smt, refresh);
			} else {
				String ta = hitungTahunAkademikMahasiswa(smt);
				kegiatanAktif = KegiatanHelper.checkKegiatanMahasiswa(jk, mahasiswa, smt, ta, refresh, false, null,
						session);
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jk, refresh);
			}

			boolean adaBulanan = false;
			if (detailBiayas != null) {
				for (Object o : detailBiayas) {
					if (o instanceof PengaturanPembayaranBulanan) {
						adaBulanan = true;
						break;
					}
				}
			}
			if (adaBulanan && mahasiswa != null) {
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jk, "-1", refresh);
			}

			susunBarisDariTemplate(detailBiayas, smt);
			renderGridTagihan();
			renderRiwayat(refresh);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PembayaranOnlineMahasiswa.reload");
			tampilKosong(colKiri, "Gagal memuat data: " + e.getMessage());
		}
	}

	/**
	 * Susun {@link #barisList} dari template biaya + peta "sudah dibayar" yang
	 * dihitung LANGSUNG dari cicilan (bukan cache Kegiatan) — memakai kunci
	 * item+bayarKe dan fallback item+bulan (paritas perbaikan hari ini di
	 * WizardPembayaranMhsHelper utk kasus "Dibayar Rp 0 padahal lunas") agar layar
	 * baru ini kebal sejak awal terhadap bug yang sama.
	 */
	private void susunBarisDariTemplate(java.util.Collection detailBiayas, Integer smt) {
		List<CicilanPembayaran> cicilans = new ArrayList<CicilanPembayaran>();
		try {
			if (kegiatanAktif != null && kegiatanAktif.getId() != null) {
				cicilans = KegiatanPersistenceHelper.ambilCicilan(kegiatanAktif, true);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PembayaranOnlineMahasiswa.susunBaris ambilCicilan");
		}

		Map<Long, Double> dibayarPerPpb = new HashMap<Long, Double>();
		Map<String, Double> dibayarPerItemBayarKe = new HashMap<String, Double>();
		Map<String, Double> dibayarPerItemBulan = new HashMap<String, Double>();
		for (CicilanPembayaran cp : cicilans) {
			if (cp == null || cp.getNilai() == null) {
				continue;
			}
			if (cp.getPengaturanPembayaranBulanan() != null && cp.getPengaturanPembayaranBulanan().getId() != null) {
				Long idPpb = cp.getPengaturanPembayaranBulanan().getId();
				Double lama = dibayarPerPpb.get(idPpb);
				dibayarPerPpb.put(idPpb, (lama == null ? 0 : lama) + cp.getNilai());
				try {
					PengaturanPembayaranBulanan ppbCp = cp.getPengaturanPembayaranBulanan();
					if (ppbCp.getRealBulan() != null && cp.getItemBiaya() != null && cp.getItemBiaya().getId() != null) {
						String kunciBulan = cp.getItemBiaya().getId() + "_bln_" + ppbCp.getRealBulan();
						Double lamaBulan = dibayarPerItemBulan.get(kunciBulan);
						dibayarPerItemBulan.put(kunciBulan, (lamaBulan == null ? 0 : lamaBulan) + cp.getNilai());
					}
				} catch (Exception eBulan) {
					ais.common.ErrorAuditUtil.record(eBulan, "auto-audit(empty-catch) PembayaranOnlineMahasiswa susun kunci item+bulan");
				}
			}
			if (cp.getItemBiaya() != null && cp.getItemBiaya().getId() != null) {
				String key = cp.getItemBiaya().getId() + "_" + cp.getBayarKe();
				Double lama = dibayarPerItemBayarKe.get(key);
				dibayarPerItemBayarKe.put(key, (lama == null ? 0 : lama) + cp.getNilai());
			}
		}

		java.util.Collection<ais.database.model.DetailKegiatan> detailKegiatans = (kegiatanAktif == null
				|| kegiatanAktif.getId() == null) ? null : kegiatanAktif.ambilDetailKegiatan(true);

		Set<String> kunciSudah = new HashSet<String>();
		if (detailBiayas != null) {
			for (Object o : detailBiayas) {
				BarisTagihan baris = new BarisTagihan();
				if (o instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) o;
					baris.ppb = ppb;
					baris.detailBiaya = ppb.getDetailBiaya();
					if (ppb.getId() != null && !kunciSudah.add("ppb:" + ppb.getId())) {
						continue;
					}
					baris.namaBulan = "Bulan " + ppb.getNamaBulan();
					Double j = kegiatanAktif == null ? null
							: mahasiswa != null
									? Kegiatan.ambilJumlahTagihan(kegiatanAktif, detailKegiatans, mahasiswa, smt, ppb)
									: Kegiatan.ambilJumlahTagihan(kegiatanAktif, detailKegiatans, smt, ppb);
					baris.nominal = j == null ? (ppb.getNominal() == null ? 0.0 : ppb.getNominal()) : j;
					Double sudah = ppb.getId() == null ? null : dibayarPerPpb.get(ppb.getId());
					if ((sudah == null || sudah.doubleValue() <= 0.0) && ppb.getRealBulan() != null
							&& baris.detailBiaya != null && baris.detailBiaya.getItemBiaya() != null
							&& baris.detailBiaya.getItemBiaya().getId() != null) {
						sudah = dibayarPerItemBulan
								.get(baris.detailBiaya.getItemBiaya().getId() + "_bln_" + ppb.getRealBulan());
					}
					baris.sudahDibayar = sudah == null ? 0.0 : sudah.doubleValue();
				} else if (o instanceof DetailBiaya) {
					DetailBiaya db = (DetailBiaya) o;
					baris.detailBiaya = db;
					String kunciDb = "item:"
							+ (db.getItemBiaya() == null || db.getItemBiaya().getId() == null ? "?"
									: db.getItemBiaya().getId().toString())
							+ "|bayarKe:" + (db.getBayarKe() == null ? "1" : db.getBayarKe().toString());
					if (!kunciSudah.add(kunciDb)) {
						continue;
					}
					Double j = kegiatanAktif == null ? null : Kegiatan.ambilJumlahTagihan(kegiatanAktif, db);
					baris.nominal = j == null ? 0.0 : j.doubleValue();
					String key = (db.getItemBiaya() == null || db.getItemBiaya().getId() == null ? "?"
							: db.getItemBiaya().getId().toString()) + "_" + (db.getBayarKe() == null ? 1 : db.getBayarKe());
					Double sudah = dibayarPerItemBayarKe.get(key);
					baris.sudahDibayar = sudah == null ? 0.0 : sudah.doubleValue();
				} else {
					continue;
				}
				baris.namaItem = baris.detailBiaya == null || baris.detailBiaya.getItemBiaya() == null ? "-"
						: baris.detailBiaya.getItemBiaya().getNama();
				baris.kekurangan = Math.max(0.0, baris.nominal - baris.sudahDibayar);
				baris.dipilih = baris.kekurangan > 0.1;
				barisList.add(baris);
			}
		}
	}

	/** Membersihkan {@code parent} lalu menampilkan satu label pesan (dipakai untuk kondisi "belum ada data"). */
	private void tampilKosong(Component parent, String pesan) {
		Common.clear(parent);
		new ais.ui.util.MyLabelConfig(pesan).setParent(parent);
	}

	/**
	 * Judul seksi bergaya (bukan {@code Caption}/{@code MyCaptionStyled}): {@code Caption} adalah
	 * komponen ZK yang HANYA boleh jadi anak Groupbox/Panel/Window — dipasang di {@code Div} biasa
	 * (mis. {@link MyPortalchildren}) akan meledak runtime "Wrong parent" (lolos compile, gagal jalan).
	 */
	private void judulSeksi(Component parent, String teks) {
		org.zkoss.zul.Div judul = new org.zkoss.zul.Div();
		judul.setStyle("font-size:12px;font-weight:bolder;color:black;border:1px solid black;"
				+ "padding:5px;border-radius:5px 15px;display:inline-block;margin-bottom:8px;");
		judul.setParent(parent);
		new ais.ui.util.MyLabelConfig(teks).setParent(judul);
	}

	/**
	 * Merender grid tagihan di {@link #colKiri} dari {@link #barisList}: satu baris per
	 * {@link BarisTagihan} dengan checkbox pilih, label item/tagihan/sudah dibayar/kekurangan,
	 * dan input nominal yang akan dibayar (di-clamp ke rentang {@code [0, kekurangan]} pada
	 * {@code onChange}). Diakhiri tombol "Bayar Tunai / Manual" ({@link #bayarTunaiManual()})
	 * dan pembaruan {@link #renderTotal()}. Menampilkan pesan kosong bila {@link #barisList} kosong.
	 */
	private void renderGridTagihan() {
		Common.clear(colKiri);
		judulSeksi(colKiri, "Daftar Tagihan");

		if (barisList.isEmpty()) {
			tampilKosong(colKiri, "Belum ada tagihan untuk kombinasi ini. Tekan Refresh untuk memuat ulang, atau periksa pengaturan billing-nya.");
			renderTotal();
			return;
		}

		MyGrid grid = new MyGrid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(colKiri);

		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Pilih").setParent(columns);
		new MyColumnConfig("Item Biaya").setParent(columns);
		new MyColumnConfig("Tagihan").setParent(columns);
		new MyColumnConfig("Sudah Dibayar").setParent(columns);
		new MyColumnConfig("Kekurangan").setParent(columns);
		new MyColumnConfig("Nominal Dibayar").setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (final BarisTagihan baris : barisList) {
			Row row = new Row();
			row.setParent(rows);

			final MyCheckboxConfig chk = new MyCheckboxConfig("");
			chk.setChecked(baris.dipilih);
			chk.setParent(row);
			baris.checkboxConfig = chk;
			chk.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					baris.dipilih = chk.isChecked();
					renderTotal();
				}
			});

			new ais.ui.util.MyLabelConfig(
					baris.namaItem + (baris.namaBulan == null ? "" : " - " + baris.namaBulan)).setParent(row);
			new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(baris.nominal)).setParent(row);
			new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(baris.sudahDibayar)).setParent(row);
			new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(baris.kekurangan)).setParent(row);

			final MyDoublebox nominalBayar = new MyDoublebox(baris.kekurangan);
			nominalBayar.setWidth("90%");
			nominalBayar.setParent(row);
			baris.nominalBayarBox = nominalBayar;
			nominalBayar.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					double v = nominalBayar.getValue() == null ? 0.0 : nominalBayar.getValue();
					if (v > baris.kekurangan) {
						v = baris.kekurangan;
						nominalBayar.setValue(v);
					}
					if (v < 0) {
						v = 0;
						nominalBayar.setValue(v);
					}
					renderTotal();
				}
			});
		}

		renderTotal();

		Hbox aksiRow = new Hbox();
		aksiRow.setWidth("100%");
		aksiRow.setStyle("margin-top:10px;gap:8px;");
		aksiRow.setParent(colKiri);
		MyToolbarbuttonConfig btnBayarTunai = new MyToolbarbuttonConfig("Bayar Tunai / Manual",
				"/img/svg/payments.svg");
		btnBayarTunai.setParent(aksiRow);
		btnBayarTunai.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				bayarTunaiManual();
			}
		});
	}

	/**
	 * Menghitung ulang total nominal dari seluruh baris {@link #barisList} yang tercentang
	 * (menjumlahkan nilai {@link BarisTagihan#nominalBayarBox} masing-masing), lalu memperbarui
	 * {@link #totalLabel} dan {@link #terbilangLabel} (terbilang dalam kata via
	 * {@link IndonesianNumberToWords}); membuat kedua label bila belum ada/sudah terlepas dari
	 * {@link #colKiri}.
	 */
	private void renderTotal() {
		double total = 0.0;
		for (BarisTagihan baris : barisList) {
			if (baris.dipilih && baris.nominalBayarBox != null) {
				Double v = baris.nominalBayarBox.getValue();
				total += v == null ? 0.0 : v.doubleValue();
			}
		}
		if (totalLabel == null || totalLabel.getParent() == null) {
			totalLabel = new MyLabelBold("");
			totalLabel.setParent(colKiri);
		}
		if (terbilangLabel == null || terbilangLabel.getParent() == null) {
			terbilangLabel = new MyLabelBoldAja("");
			terbilangLabel.setParent(colKiri);
		}
		totalLabel.setValue("Total yang akan dibayar: " + Common.numberFormat.get().format(total));
		try {
			terbilangLabel.setValue(Common.kapitalAwalKata(
					IndonesianNumberToWords.convert((long) total) + " rupiah"));
		} catch (Exception e) {
			terbilangLabel.setValue("");
		}
	}

	/**
	 * Merender grid riwayat pembayaran ({@link CicilanPembayaran}) di {@link #colKanan} untuk
	 * {@link #kegiatanAktif} (via {@code KegiatanPersistenceHelper#ambilCicilan}): waktu, item
	 * biaya, nilai, dan validator per baris cicilan. Menampilkan pesan kosong bila
	 * {@link #kegiatanAktif} belum ada atau belum punya cicilan.
	 *
	 * @param refresh bila {@code true}, paksa segarkan cache daftar cicilan
	 */
	private void renderRiwayat(boolean refresh) {
		Common.clear(colKanan);
		judulSeksi(colKanan, "Riwayat Pembayaran");
		if (kegiatanAktif == null || kegiatanAktif.getId() == null) {
			tampilKosong(colKanan, "Belum ada riwayat.");
			return;
		}
		List<CicilanPembayaran> cicilans = KegiatanPersistenceHelper.ambilCicilan(kegiatanAktif, refresh);
		if (cicilans == null || cicilans.isEmpty()) {
			tampilKosong(colKanan, "Belum ada riwayat pembayaran.");
			return;
		}
		MyGrid grid = new MyGrid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(colKanan);
		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Waktu").setParent(columns);
		new MyColumnConfig("Item").setParent(columns);
		new MyColumnConfig("Nilai").setParent(columns);
		new MyColumnConfig("Validator").setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);
		for (CicilanPembayaran cp : cicilans) {
			Row row = new Row();
			row.setParent(rows);
			new ais.ui.util.MyLabelConfig(
					cp.getTanggal() == null ? "" : Common.dateFormat3.get().format(cp.getTanggal())).setParent(row);
			new ais.ui.util.MyLabelConfig(cp.getItemBiaya() == null ? "" : cp.getItemBiaya().getNama())
					.setParent(row);
			new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(cp.getNilai() == null ? 0.0 : cp.getNilai()))
					.setParent(row);
			new ais.ui.util.MyLabelConfig(cp.getValidator()).setParent(row);
		}
	}

	// ============================================================ BAYAR TUNAI / MANUAL
	/**
	 * Simpan pembayaran tunai/manual: satu {@link Kegiatan} (get-or-create, dipakai
	 * ulang bila sudah ada) + satu {@link CicilanPembayaran} per baris terpilih,
	 * dalam satu transaksi. Diadaptasi LANGSUNG dari pola
	 * {@code WizardPembayaranMhsHelper.bayarTunai} yang sudah terbukti (lihat
	 * javadoc class ini) — berlaku utk Mahasiswa MAUPUN {@link BiodataCalonMahasiswa},
	 * tanpa menyentuh berkas Wizard sama sekali.
	 */
	private void bayarTunaiManual() throws Exception {
		List<BarisTagihan> dipilih = new ArrayList<BarisTagihan>();
		double total = 0.0;
		for (BarisTagihan baris : barisList) {
			if (baris.dipilih && baris.nominalBayarBox != null) {
				Double v = baris.nominalBayarBox.getValue();
				double nilai = v == null ? 0.0 : v.doubleValue();
				if (nilai > 0.01) {
					total += nilai;
					dipilih.add(baris);
				}
			}
		}
		if (dipilih.isEmpty()) {
			MyMessageboxConfig.show("Pilih minimal satu tagihan dengan nominal lebih dari 0 untuk dibayar.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		final double totalFinal = total;
		final List<BarisTagihan> dipilihFinal = dipilih;
		MyMessageboxConfig.show(
				"Simpan pembayaran tunai/manual sebesar " + Common.numberFormat.get().format(totalFinal)
						+ " untuk " + dipilihFinal.size() + " item terpilih?",
				"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
							simpanPembayaranTunai(dipilihFinal, totalFinal);
						}
					}
				});
	}

	/**
	 * Menyimpan transaksi pembayaran tunai/manual dalam satu sesi+transaksi Hibernate baru:
	 * mengambil-atau-membuat satu {@link Kegiatan} (get-or-create via
	 * {@code Mahasiswa#ambilKegiatansRefresh}/{@code BiodataCalonMahasiswa#ambilKegiatansRefresh}),
	 * menambah {@code amount}-nya, dan menyimpan satu {@link CicilanPembayaran} bertipe
	 * {@link ConstantValues#TUNAI} per baris terpilih dengan nominal dari
	 * {@link BarisTagihan#nominalBayarBox} saat ini. Validator dicatat dari nama pengguna login
	 * saat ini. Menjalankan ulang {@code PembayaranUtil#updateTunggakan} setelahnya (kegagalan
	 * di sini diaudit tapi tidak membatalkan transaksi utama). Rollback otomatis dan pesan
	 * kegagalan ditampilkan bila terjadi exception; sukses memicu {@link #reload(boolean)}.
	 *
	 * <p><b>Catatan integritas (defense-in-depth):</b> nilai tiap baris di-clamp ULANG ke
	 * rentang {@code [0, kekurangan]} tepat di titik simpan ini ({@code totalTersimpan}/
	 * {@code nilaiTersimpanList}, dihitung dari {@link BarisTagihan#kekurangan} server-side)
	 * sebelum dipakai untuk {@code amount}/{@code amountTerhutang} {@link Kegiatan} maupun
	 * {@link CicilanPembayaran#setNilai}. Parameter {@code total} dari pemanggil hanya dipakai
	 * untuk teks konfirmasi di {@link #bayarTunaiManual()} dan TIDAK dipercaya di sini — clamp
	 * pada listener {@code onChange} komponen (lihat {@link #renderGridTagihan()}) tetap ada
	 * sebagai lapis UX, tapi bukan satu-satunya penjaga integritas nominal lagi.</p>
	 *
	 * @param dipilih baris tagihan terpilih dengan nominal &gt; 0 yang akan disimpan sebagai cicilan
	 * @param total   perkiraan jumlah nominal {@code dipilih} dari sisi client (advisory saja, lihat catatan integritas di atas)
	 */
	private void simpanPembayaranTunai(List<BarisTagihan> dipilih, double total) throws Exception {
		boolean sukses = false;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.beginTransaction();

			JenisKegiatan jk = jenisKegiatanTerpilih();
			Integer smt = semesterBox.getValue();
			String validator = Common.getCurrentUser() == null ? "-" : Common.getCurrentUser().getUserNama();

			double totalKekurangan = 0.0;
			double totalTersimpan = 0.0;
			List<Double> nilaiTersimpanList = new ArrayList<Double>();
			for (BarisTagihan baris : dipilih) {
				totalKekurangan += baris.kekurangan;
				Double v = baris.nominalBayarBox == null ? null : baris.nominalBayarBox.getValue();
				double nilai = v == null ? 0.0 : v.doubleValue();
				if (nilai > baris.kekurangan) {
					nilai = baris.kekurangan;
				}
				if (nilai < 0.0) {
					nilai = 0.0;
				}
				nilaiTersimpanList.add(nilai);
				totalTersimpan += nilai;
			}

			Kegiatan kegiatan;
			if (calonMahasiswa != null) {
				kegiatan = calonMahasiswa.ambilKegiatansRefresh(smt, jk, true);
			} else {
				kegiatan = mahasiswa.ambilKegiatansRefresh(smt, jk, true);
			}

			boolean kegiatanBaru = kegiatan == null || kegiatan.getId() == null;
			if (kegiatanBaru) {
				kegiatan = new Kegiatan();
				kegiatan.setJenisKegiatan(jk);
				if (calonMahasiswa != null) {
					kegiatan.setCalonMahasiswa(calonMahasiswa);
				} else {
					kegiatan.setMahasiswa(mahasiswa);
					kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
				}
				kegiatan.setSemster(smt);
				kegiatan.setTahunAkademik(
						calonMahasiswa != null ? calonMahasiswa.getTahunAkademik() : hitungTahunAkademikMahasiswa(smt));
				kegiatan.setKeterangan("Pembayaran Online Mahasiswa (Coba Cara Baru) - Tunai");
				kegiatan.setAmount(totalTersimpan);
			} else {
				double amountLama = kegiatan.getAmount() == null ? 0.0 : kegiatan.getAmount();
				kegiatan.setAmount(amountLama + totalTersimpan);
			}
			kegiatan.setTanggal(WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(validator);
			kegiatan.setAmountTerhutang(Math.max(0.0, totalKekurangan - totalTersimpan));
			Common.refreshSaveOrUpdate(session, kegiatan);

			int ke = 1;
			for (int i = 0; i < dipilih.size(); i++) {
				BarisTagihan baris = dipilih.get(i);
				double nilai = nilaiTersimpanList.get(i).doubleValue();
				if (nilai <= 0.01) {
					continue;
				}
				CicilanPembayaran cicilan = new CicilanPembayaran(baris.detailBiaya);
				cicilan.setKegiatan(kegiatan);
				if (baris.detailBiaya != null) {
					cicilan.setItemBiaya(baris.detailBiaya.getItemBiaya());
				}
				if (baris.ppb != null) {
					cicilan.setPengaturanPembayaranBulanan(baris.ppb);
				}
				cicilan.setNilai(nilai);
				cicilan.setNilaiAsli(nilai);
				cicilan.setJenisPembayaran(ConstantValues.TUNAI);
				cicilan.setTanggal(WaktuUtil.getDate());
				cicilan.setKe(ke++);
				cicilan.setKeterangan("Pembayaran Online Mahasiswa - Tunai");
				cicilan.setValidator(validator);
				session.save(cicilan);
			}

			try {
				ais.action.ws.util.PembayaranUtil.getInstance().updateTunggakan(kegiatan, session);
			} catch (Exception eTunggakan) {
				ais.common.ErrorAuditUtil.record(eTunggakan,
						"auto-audit(empty-catch) PembayaranOnlineMahasiswa.simpanPembayaranTunai updateTunggakan");
			}

			session.getTransaction().commit();
			sukses = true;
			kegiatanAktif = kegiatan;
		} catch (Exception ex) {
			if (session != null) {
				try {
					session.getTransaction().rollback();
				} catch (Exception exRollback) {
					ais.common.ErrorAuditUtil.record(exRollback,
							"auto-audit(empty-catch) PembayaranOnlineMahasiswa.simpanPembayaranTunai rollback");
				}
			}
			ais.common.ErrorAuditUtil.record(ex, "auto-audit PembayaranOnlineMahasiswa.simpanPembayaranTunai");
			Common.tampilErrorJikaAdmin(ex);
			MyMessageboxConfig.show("Gagal menyimpan pembayaran: " + ex.getMessage(), "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		if (sukses) {
			MyMessageboxConfig.show("Pembayaran berhasil disimpan.", "Berhasil", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			reload(true);
		}
	}
}
