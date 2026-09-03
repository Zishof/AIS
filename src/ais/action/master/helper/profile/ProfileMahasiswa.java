package ais.action.master.helper.profile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Box;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.PengumumanAkademisAction;
import ais.action.master.SkripsiAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.MahasiswaPunyaKegiatanKemahasiswaanHelper;
import ais.action.master.helper.MahasiswaPunyaOrganisasiIntraKampusHelper;
import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.action.report.format1.akademik.LaporanRekamanNilai;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>ProfileMahasiswa — Panel Profil Lengkap Mahasiswa</h3>
 *
 * <p><b>Untuk apa:</b> Menyajikan dashboard profil satu halaman bagi mahasiswa yang mencakup
 * semua aspek perjalanan studi: ringkasan studi (tren IPK/IPS, spider web, daftar matakuliah),
 * perkuliahan aktif per semester (dengan navigasi antar-semester, KRS, nilai, kehadiran),
 * KKN, PKL, bimbingan akademik (PA), bimbingan TA/skripsi, sidang, kegiatan kemahasiswaan,
 * organisasi, prestasi, dan karya.</p>
 *
 * <p><b>Cara kerja:</b> Konstruktor menerima entitas {@link Mahasiswa}. Method
 * {@link #init(Component, int, Integer, boolean)} dipanggil dari controller profil untuk
 * membangun seluruh panel ZK. Navigasi semester diimplementasikan sebagai tombol "Smt
 * sebelumnya/selanjutnya" yang memanggil ulang {@code init} dengan {@code smt} berbeda.</p>
 *
 * <p><b>Seksi yang ditampilkan (berurutan):</b>
 * <ol>
 *   <li>Toolbar: Biodata, Transkrip, IPK (Rekaman Nilai), Rekap Semua KRS</li>
 *   <li>Kartu identitas mahasiswa (foto, nama, NIM, program studi)</li>
 *   <li>Panel Perkuliahan semester aktif + navigasi semester + info KRS/KHS</li>
 *   <li>Sub-panel initDashboard: Ringkasan Studi + Tren Akademik + Spider Web +
 *       Daftar Matakuliah (paging 10)</li>
 *   <li>Jadwal perkuliahan dengan paging + search + tombol Refresh/KRS/Nilai/Kehadiran</li>
 *   <li>Materi/Tugas/Ujian per kelas (jika {@code tampilMateri=true})</li>
 *   <li>Panel Info Pembayaran (link ke informasi_pembayaran_mahasiswa.zul)</li>
 *   <li>Panel KKN (jika konfigurasi aktif), PKL</li>
 *   <li>Panel Pembimbing Akademik (foto + data dosen PA)</li>
 *   <li>Panel FormulirKegiatan per jenis + satu group tanpa jenis, tombol Ajukan</li>
 *   <li>Panel Bimbingan TA, Sidang TA/Skripsi</li>
 *   <li>Panel Kegiatan Kemahasiswaan, Organisasi, Prestasi, Karya</li>
 * </ol>
 * </p>
 *
 * <p><b>Semester pendek:</b> Parameter {@code semesterPendek} memungkinkan tampilan
 * KRS semester pendek yang terpisah dari semester reguler. Sinkronisasi KRS dilakukan
 * via {@code ProfileUtil.singkronkanKrsMahasiswa(mhs, smt, null, semPendek)}.</p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread karena memodifikasi
 * pohon komponen ZK. Query database di {@link #initDashboard} membuka sesi tersendiri.</p>
 *
 * <p><b>Pemeliharaan:</b> Java 1.7 dan ZKoss 5.5. Method {@code init} menggunakan
 * {@code @SuppressWarnings} untuk API ZK dan Hibernate lama. Penambahan seksi baru
 * harus mengikuti pola Group + MyRowStyled yang sudah ada.</p>
 */
public class ProfileMahasiswa {

	private Mahasiswa mahasiswa;
	private KrsMahasiswa krsMahasiswa;

	/**
	 * Membuat instance panel profil mahasiswa dengan entitas mahasiswa yang ditentukan.
	 *
	 * <p>Menyimpan referensi entitas {@link Mahasiswa} untuk diakses selama {@link #init}.
	 * Semua query database dilakukan di {@link #init} dan {@link #initDashboard}.</p>
	 *
	 * @param mahasiswa entitas mahasiswa yang profilnya akan ditampilkan; boleh null
	 *                  (beberapa seksi mungkin kosong, tapi tidak menyebabkan exception)
	 * @throws Exception diwarisi dari {@code super()}
	 */
	public ProfileMahasiswa(Mahasiswa mahasiswa) throws Exception {
		super();
		this.mahasiswa = mahasiswa;
	}

	private int jumlahDataDalamSatuHalamanElearning = 3;
	private int pagingPerkuliahan = 1;
	private int pagingFormulirKegiatan = 1;

	String tahunAkademik = null;
	String jenisSemester = null;
	String hr = null;
	String keyword = "";
	boolean merupakanPraPerkuliahan = false;
	Integer ekstrakurikuler = null;
	boolean merupakanRemedial = false;

	private List<Perkuliahan> dataDiambil = null;

	/**
	 * Membangun seluruh panel profil mahasiswa ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Titik masuk utama untuk merender profil mahasiswa secara lengkap.
	 * Parameter {@code smt} menentukan semester mana yang ditampilkan sebagai aktif.</p>
	 *
	 * <p><b>Urutan render utama:</b>
	 * <ol>
	 *   <li>Mempersiapkan Grid ZK 2-kolom via {@link ProfileUiHelper#prepareContentParent}.</li>
	 *   <li>Toolbar: Biodata ({@code CommonReportHelper.onCetakBiodataMahasiswa}),
	 *       Transkrip ({@code LaporanTranskipAkademik}), IPK ({@code LaporanRekamanNilai}),
	 *       Rekap ({@code PenilaianUtil.downloadSemuaKRS}).</li>
	 *   <li>Checkbox tampilMateri (hanya jika {@code tampilMateri=true}).</li>
	 *   <li>Sinkronisasi KRS mahasiswa: menentukan semester yang aktif untuk render.</li>
	 *   <li>Group header "Perkuliahan {ta}/{sem}" dengan navigasi prev/next semester
	 *       dan checkbox Semester Pendek (SP).</li>
	 *   <li>HTML keterangan pengambilan KRS + catatan KRS/KHS jika ada.</li>
	 *   <li>Panel Masa Studi ({@link #buatInfoMasaStudi(String)}).</li>
	 *   <li>Toolbar aksi: Refresh, KRS, KRS Paket, Nilai, Kehadiran, Aktifitas +
	 *       Cetak KRS, UTS, UAS, KHS.</li>
	 *   <li>Sub-panel {@link #initDashboard(Component, Mahasiswa, DataLoader, boolean)}:
	 *       Ringkasan Studi + Tren + Radar + Daftar Matakuliah.</li>
	 *   <li>Grid jadwal perkuliahan dengan paging + search textbox + event listener;
	 *       jika {@code tampilMateri=true}, panel materi/tugas/ujian per kelas.</li>
	 *   <li>Info pembayaran, KKN, PKL, PA, FormulirKegiatan, Bimbingan TA, Sidang TA.</li>
	 *   <li>Kegiatan, Organisasi, Prestasi, Karya Kemahasiswaan.</li>
	 * </ol>
	 * </p>
	 *
	 * @param parent        komponen ZK tujuan; jika null method langsung return
	 * @param smt           nomor semester yang ditampilkan sebagai "semester aktif"
	 *                      (digunakan untuk navigasi prev/next)
	 * @param semesterPendek Integer semester pendek yang aktif; null jika tidak ada
	 * @param tampilMateri  {@code true} untuk menampilkan materi, tugas, dan ujian
	 *                      per kelas di bawah jadwal perkuliahan
	 * @throws Exception bila terjadi error fatal dalam penyusunan komponen ZK
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void init(final Component parent, final int smt, final Integer semesterPendek, final boolean tampilMateri)
			throws Exception {

		if (parent == null) {
			return;
		}
		String waktu = ProfileUiHelper.waktuSapaan();

		Component contentParent = ProfileUiHelper.prepareContentParent(parent);

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("background: #f8f9fa; border: none; box-shadow: 0 4px 6px rgba(0,0,0,0.04); border-radius: 8px;");
		grid.setParent(contentParent);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("80px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (parent instanceof LayoutRegion) {
			((LayoutRegion) parent).setTitle("Hai, Selamat " + waktu);
			((LayoutRegion) parent).setCollapsible(true);
			((LayoutRegion) parent).setSplittable(true);
		} else {
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Profil Mahasiswa", "Identitas, perkuliahan, nilai, tugas, ujian, dan kegiatan akademik mahasiswa tersaji dalam satu halaman yang mudah dibaca.");
		}

		/* Kartu identitas satu baris: foto + sapaan + nama + kontak */
		Component fotoMhs = null;
		try {
			fotoMhs = CommonMedia.tampilkanGambarKecil(mahasiswa);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileMahasiswa.java:248");
		}
		org.zkoss.zul.Div infoKartu = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, fotoMhs, "Hai, Selamat " + waktu);
		MyLabelBoldAja lblNama = new MyLabelBoldAja(mahasiswa.getNama());
		lblNama.setStyle("font-size: 14px; color: #0056b3;");
		infoKartu.appendChild(lblNama);
		infoKartu.appendChild(new MyLabelBoldAja(mahasiswa.getNim()));
		mahasiswa.tampilkanHp(infoKartu);
		mahasiswa.tampilkanEmail(infoKartu);

		Row row;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Box hbox = new Hbox();
		hbox.setSpacing("10px");
		hbox.setStyle("padding: 10px 5px;");
		hbox.setSclass("ais-btn-group");
		hbox.setParent(row);

		// Tombol BAYAR (Wizard Pembayaran) — 1 klik bagi mahasiswa untuk membayar
		// tagihan langkah demi langkah. Paling depan & menonjol (hijau).
		if (mahasiswa != null && mahasiswa.getId() != null
				&& Common.bolehKonfigurasi("tampilkan_tombol_bayar_wizard_di_profile")
				&& ais.action.master.helper.WizardPembayaranMhsHelper.aktif()) {
			MyToolbarbuttonConfig btnBayarWizard = new MyToolbarbuttonConfig("Bayar", "/img/Finance-Invoice-icon.png");
			btnBayarWizard.setStyle("font-weight:800;border:0;border-radius:8px;padding:6px 16px;cursor:pointer;"
					+ "color:#ffffff;background:linear-gradient(135deg,#16a34a,#15803d);"
					+ "box-shadow:0 4px 12px rgba(22,163,74,.3);");
			btnBayarWizard.setTooltiptext("Buka Wizard Pembayaran — bayar tagihan langkah demi langkah");
			btnBayarWizard.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ais.action.master.helper.WizardPembayaranMhsHelper.buka(mahasiswa, null);
				}
			});
			btnBayarWizard.setParent(hbox);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Biodata", "/img/online-icon_access.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				CommonReportHelper.onCetakBiodataMahasiswa(biodataMahasiswa);
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Transkrip", "/img/Document-Text-icon.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_transkrip_di_profile"));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, smt)) {
					return;
				}
				LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik();
				laporanTranskipAkademik.setTitle("Transkrip");
				laporanTranskipAkademik.setClosable(true);
				laporanTranskipAkademik.setBorder("none");
				laporanTranskipAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanTranskipAkademik.setHeight("95%");
				laporanTranskipAkademik.setWidth("90%");
				laporanTranskipAkademik.onModal();
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("IPK", "/img/Diploma-Certificate-icon.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, smt)) {
					return;
				}
				LaporanRekamanNilai laporanTranskipAkademik = new LaporanRekamanNilai();
				laporanTranskipAkademik.setTitle("IPK");
				laporanTranskipAkademik.setClosable(true);
				laporanTranskipAkademik.setBorder("none");
				laporanTranskipAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanTranskipAkademik.setHeight("95%");
				laporanTranskipAkademik.setWidth("90%");
				laporanTranskipAkademik.onModal();
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Rekap", "/img/invoice-icon_surat.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, smt)) {
					return;
				}
				PenilaianUtil.downloadSemuaKRS(mahasiswa);
			}
		});
		button.setParent(hbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyCheckboxConfig materiPerkuliahan = new MyCheckboxConfig("Tampilkan juga materi, tugas, dan ujian");
		materiPerkuliahan.setChecked(tampilMateri);
		materiPerkuliahan.setStyle("font-size:11px; font-weight: bold; padding-left: 5px; color: #495057;");
		materiPerkuliahan.setParent(row);
		materiPerkuliahan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				init(parent, smt, semesterPendek, materiPerkuliahan.isChecked());
			}
		});

		krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, semesterPendek);
		Group group2 = new ais.ui.util.MyGroupConfig(
				"Perkuliahan " + krsMahasiswa.getTahunAkademik() + "/" + krsMahasiswa.getSemester());
		group2.setStyle(" font-weight: bold; color: #495057; font-size: 14px; padding: 10px;");
		group2.setParent(rows);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		hbox = new Hbox();
		hbox.setStyle("padding: 5px;");
		hbox.setParent(row);

		MyToolbarbuttonConfig nav = new MyToolbarbuttonConfig("Smt sebelumnya", "/img/back-2-icon.png");
		nav.setVisible(smt > 1);
		nav.setStyle("font-weight: bold;");
		hbox.appendChild(nav);
		nav.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				pagingFormulirKegiatan = 1;
				init(parent, smt - 1, semesterPendek, tampilMateri);
			}
		});

		nav = new MyToolbarbuttonConfig("Smt selanjutnya", "/img/next-2-icon.png");
		nav.setStyle("font-weight: bold;");
		hbox.appendChild(nav);
		nav.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				pagingFormulirKegiatan = 1;
				init(parent, smt + 1, semesterPendek, tampilMateri);
			}
		});

		MyCheckboxConfig sp = new MyCheckboxConfig("SP");
		sp.setChecked(semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK));
		sp.setStyle("font-weight: bold; padding-left: 10px;");
		hbox.appendChild(sp);
		sp.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyCheckboxConfig sp = (MyCheckboxConfig) arg0.getTarget();
				init(parent, smt, sp.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null, tampilMateri);
			}
		});


		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Vbox ringkasanKrs = new Vbox();
		ringkasanKrs.setWidth("100%");
		row.appendChild(ringkasanKrs);
		new ais.ui.util.MyHtml("<b>Perkuliahan:</b>").setParent(ringkasanKrs);
		Html keteranganKrs = new ais.ui.util.MyHtml("");
		ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(
				keteranganKrs, mahasiswa, krsMahasiswa, false);
		keteranganKrs.setParent(ringkasanKrs);

		// Penjelasan otomatis saat status Nonaktif memakai analyzer kanonik yang sama dengan
		// layar pembayaran. Analyzer membedakan tagihan belum dibayar, tagihan belum terbentuk,
		// KRS/SKS kosong, NIM pindahan, dan history yang belum sinkron; jangan mengembalikan
		// pemeriksaan lokal berbasis Kegiatan.getPersentaseLunas() karena rekap itu asynchronous.
		try {
			ais.database.model.HistoryStatusMahasiswa statusSaatIni = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(krsMahasiswa, false);
			boolean statusAktif = statusSaatIni != null && statusSaatIni.getStatusMahasiswa() != null
					&& ais.common.ConstantValues.AKTIF != null && ais.common.ConstantValues.AKTIF.getId() != null
					&& ais.common.ConstantValues.AKTIF.getId().equals(statusSaatIni.getStatusMahasiswa().getId());

			if (!statusAktif) {
				ais.action.master.helper.HistoryStatusMahasiswaUtil.AnalisisStatusMahasiswa analisisStatus =
						ais.action.master.helper.HistoryStatusMahasiswaUtil.analisisStatus(krsMahasiswa,
								statusSaatIni, statusSaatIni == null ? null : statusSaatIni.getStatusMahasiswa());
				String alasanNonaktif = analisisStatus.getRingkasan();
				if (alasanNonaktif != null && !alasanNonaktif.trim().isEmpty()) {
					String pesanNonaktif = "<div style=\"padding:10px;background-color:#fef9c3;border-radius:8px;"
							+ "border:1px solid #fde68a;color:#854d0e;margin-top:5px;line-height:1.5;\">"
							+ "<b>&#9888; Status Anda saat ini Nonaktif</b><br>"
							+ "Penyebab yang terdeteksi: <b>" + ProfileUiHelper.esc(alasanNonaktif) + "</b>.<br>"
							+ "Setelah data penyebab diperbaiki, tekan tombol \"Refresh Status &amp; Tagihan\" "
							+ "untuk menghitung kembali status dari data terbaru.</div>";
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.appendChild(new ais.ui.util.MyHtml(pesanNonaktif));
				}
			}
		} catch (Exception eJelaskan) {
			ais.common.ErrorAuditUtil.record(eJelaskan, "auto-audit(empty-catch) ProfileMahasiswa penjelasan nonaktif");
		}

		// Tombol refresh mandiri utk mahasiswa (permintaan user 2026-08-06, kasus ICHLAS
		// NUR A'MAL/UIN Bukittinggi, status tetap Nonaktif walau sudah bayar): status
		// Aktif/Non-Aktif & tagihan Kegiatan HANYA dihitung ulang saat ada aksi refresh=true
		// (Proses Tagihan batch / tombol admin) -- mahasiswa sendiri TIDAK punya cara memicu
		// ini, jadi harus menunggu admin. Tombol ini menghitung ulang Kegiatan (jenis yg jadi
		// syarat keaktifan saja, hitungUlang=true TAPI rst=false -- TIDAK memicu hapus-buat-ulang
		// DetailKegiatan yg destruktif) lalu status (refresh=true, bypass cache), sehingga
		// mahasiswa bisa mandiri tanpa perlu admin sinkron satu-satu.
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hboxRefreshStatusTagihan = new Hbox();
		hboxRefreshStatusTagihan.setStyle("padding: 5px;");
		hboxRefreshStatusTagihan.setParent(row);
		MyToolbarbuttonConfig btnRefreshStatusTagihan = new MyToolbarbuttonConfig("Refresh Status & Tagihan",
				"/img/Button-Refresh-icon.png");
		btnRefreshStatusTagihan.setStyle("font-weight: bold; color:#1d4ed8;");
		btnRefreshStatusTagihan.setTooltiptext(
				"Hitung ulang status keaktifan & tagihan Anda saat ini berdasarkan data pembayaran terbaru.");
		hboxRefreshStatusTagihan.appendChild(btnRefreshStatusTagihan);
		btnRefreshStatusTagihan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							if (ais.common.CommonHelperClass.jenisKegiatansUntukSyaratAktif == null) {
								try {
									ais.common.CommonHelperClass.reloadJenisKegiatans();
								} catch (Exception eReload) {
									ais.common.ErrorAuditUtil.record(eReload,
											"auto-audit(empty-catch) ProfileMahasiswa refresh mandiri: reloadJenisKegiatans");
								}
							}
							Session session = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								if (ais.common.CommonHelperClass.jenisKegiatansUntukSyaratAktif != null) {
									List<ais.database.model.Kegiatan> kegiatanSyaratAktif = mahasiswa.ambilKegiatans(
											smt, ais.common.CommonHelperClass.jenisKegiatansUntukSyaratAktif, true);
									if (kegiatanSyaratAktif != null) {
									for (ais.database.model.Kegiatan keg : kegiatanSyaratAktif) {
										try {
											if (!ais.action.master.helper.HistoryStatusMahasiswaUtil
													.kegiatanSyaratAktifBerlaku(keg, smt)) {
												continue;
											}
											ais.action.master.helper.KegiatanHelper.checkKegiatanMahasiswa(keg,
													keg.getJenisKegiatan(), mahasiswa, smt,
													krsMahasiswa.getTahunAkademik(), true, keg.getJadwalPembayaran(),
													false, false, null, session);
										} catch (Exception eKeg) {
											ais.common.ErrorAuditUtil.record(eKeg,
													"auto-audit(empty-catch) ProfileMahasiswa refresh mandiri: checkKegiatanMahasiswa");
										}
									}
									}
								}
							} finally {
								if (session != null) {
									try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ProfileMahasiswa refresh mandiri: clear"); }
									try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ProfileMahasiswa refresh mandiri: disconnect"); }
									try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ProfileMahasiswa refresh mandiri: close"); }
								}
							}
							ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa, true);
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e,
									"auto-audit ProfileMahasiswa refresh mandiri status+tagihan");
						}
						pagingPerkuliahan = 1;
						pagingFormulirKegiatan = 1;
						init(parent, smt, semesterPendek, tampilMateri);
					}
				});
			}
		});

		String catatanKrs = bersihkanCatatan(krsMahasiswa == null ? null : krsMahasiswa.getCatatan());
		String catatanKhs = bersihkanCatatan(krsMahasiswa == null ? null : krsMahasiswa.getCatatanKhs());
		if (catatanKrs.length() > 0 || catatanKhs.length() > 0) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			StringBuffer catatanHtml = new StringBuffer();
			catatanHtml.append("Catatan:<br><div style=\"padding:10px;background-color:#fff8e1;border-radius:8px;border:1px solid #fde68a;color:#854d0e;margin-top:5px;line-height:1.5;\">");
			if (catatanKrs.length() > 0) {
				catatanHtml.append(ProfileUiHelper.esc(catatanKrs));
			}
			if (catatanKrs.length() > 0 && catatanKhs.length() > 0) {
				catatanHtml.append("<br/>");
			}
			if (catatanKhs.length() > 0) {
				catatanHtml.append(ProfileUiHelper.esc(catatanKhs));
			}
			catatanHtml.append("</div>");
			row.appendChild(new ais.ui.util.MyHtml(catatanHtml.toString()));
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		// Panel "Masa Studi" (ambilMasaStudi + format) di-cache per-mahasiswa — TTL 20 mnt.
		row.appendChild(new Html(ProfileCacheUtil.htmlPerUser("MahasiswaMasaStudi", "MAHASISWA",
				(mahasiswa == null ? null : mahasiswa.getId()), new ProfileCacheUtil.Pembuat() {
					@Override
					public String buat() throws Exception {
						return buatInfoMasaStudi(mahasiswa.ambilMasaStudi());
					}
				})));

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		hbox = new Hbox();
		hbox.setStyle("padding: 5px;");
		hbox.setSclass("ais-btn-group");
		hbox.setParent(row);

		MyToolbarbuttonConfig toolbarbuttonLihat = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		toolbarbuttonLihat.setStyle("font-weight: bold;");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				pagingFormulirKegiatan = 1;

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = null;
						try {
							mahasiswa.reInit();
							session = HibernateUtil.getSessionFactory().openSession();
							mahasiswa.reInitSkripsi(session);
							mahasiswa.reInitBimbingan(session);
							mahasiswa.reInitKkn(session);
							mahasiswa.reInitPkl(session);
							mahasiswa.reInitKrs(session);
							mahasiswa.reInitFormulirKegiatanPeserta(session);
							mahasiswa.reInitKonsultasi(session);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileMahasiswa.java:492");
						} finally {
							if (session != null) {
								try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileMahasiswa.java:495");}
								try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileMahasiswa.java:496");}
								try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileMahasiswa.java:497");}
							}
						}
						init(parent, smt, semesterPendek, tampilMateri);
					}
				});
			}
		});

		toolbarbuttonLihat = new MyToolbarbuttonConfig("KRS", "/img/Document-Text-icon.png");
		toolbarbuttonLihat.setStyle("font-weight: bold;");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_krs_di_profile"));
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/krs.zul?pass=true", false, "95%", "95%", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, smt, semesterPendek, tampilMateri);
					}
				}, "KRS Mahasiswa");
			}
		});

		toolbarbuttonLihat = new MyToolbarbuttonConfig("KRS Paket", "/img/Document-Text-icon.png");
		toolbarbuttonLihat.setStyle("font-weight: bold;");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat
				.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_krs_paket_di_profile", Konfigurasi.TIDAK_AKTIF));
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/krs_paket.zul?pass=true", false, "95%", "95%", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, smt, semesterPendek, tampilMateri);
					}
				}, "KRS Paket Mahasiswa");
			}
		});

		toolbarbuttonLihat = new MyToolbarbuttonConfig("Nilai", "/img/options-icon.png");
		toolbarbuttonLihat.setStyle("font-weight: bold;");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, smt)) {
					return;
				}
				Common.displayWindow("/pages/master/nilai_mahasiswa.zul?pass=true", false, "95%", "95%",
						new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								init(parent, smt, semesterPendek, tampilMateri);
							}
						}, "Nilai Mahasiswa");
			}
		});

		toolbarbuttonLihat = new MyToolbarbuttonConfig("Kehadiran", "/img/vendor.png");
		toolbarbuttonLihat.setStyle("font-weight: bold;");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/absensi_mahasiswa.zul?pass=true", false, "95%", "95%",
						new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								init(parent, smt, semesterPendek, tampilMateri);
							}
						}, "Kehadiran Mahasiswa");
			}
		});

		toolbarbuttonLihat = new MyToolbarbuttonConfig("Aktfitas", "/img/Apps-Calendar-Metro-icon.png");
		toolbarbuttonLihat.setStyle("font-weight: bold;");
		hbox.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/pertemuan.zul?pass=true", false, "95%", "95%", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, smt, semesterPendek, tampilMateri);
					}
				}, "Aktiftas Perkuliahan");
			}
		});

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		hbox = new Hbox();
		hbox.setStyle("padding: 5px;");
		hbox.setSclass("ais-btn-group");
		hbox.setParent(row);

		button = new MyToolbarbuttonConfig("Cetak KRS", "/img/print.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakKRS(mahasiswa, krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
						krsMahasiswa.getSemesterPendek(), false);
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Cetak UTS", "/img/print.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		// Default TIDAK_AKTIF: tombol "Cetak UTS" HANYA tampil bila konfig
		// tampilkan_tombol_cetak_kartu_uts di-set "aktif". Bila belum di-set / tidak aktif -> disembunyikan.
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uts", Konfigurasi.TIDAK_AKTIF));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakUTS(mahasiswa, krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
						krsMahasiswa.getTahunAkademik(), krsMahasiswa.getSemesterPendek(), false, false);
			}
		});
		if(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uts", Konfigurasi.TIDAK_AKTIF)){
			button.setParent(hbox);
		}

		button = new MyToolbarbuttonConfig("Cetak UAS", "/img/print.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		// Default TIDAK_AKTIF: tombol "Cetak UAS" HANYA tampil bila konfig
		// tampilkan_tombol_cetak_kartu_uas di-set "aktif". Bila belum di-set / tidak aktif -> disembunyikan.
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uas", Konfigurasi.TIDAK_AKTIF));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakUAS(mahasiswa, krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
						krsMahasiswa.getTahunAkademik(), krsMahasiswa.getSemesterPendek(), false, false);
			}
		});
		if(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uas", Konfigurasi.TIDAK_AKTIF)){
			button.setParent(hbox);
		}

		button = new MyToolbarbuttonConfig("Cetak KHS", "/img/print.png");
		button.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		button.setParent(hbox);
		button.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, smt)) {
					return;
				}
				CommonReportHelper.cetakNilai(mahasiswa, krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
						krsMahasiswa.getSemesterPendek(), false, krsMahasiswa.getTahunAkademik());
			}
		});


		Row dashRow = new MyRowStyled();
		ais.ui.util.ZkCompat.setSpans(dashRow, "2");
		dashRow.setParent(rows);

		final Vbox dashboardContainer = new Vbox();
		dashboardContainer.setWidth("100%");
		dashboardContainer.setParent(dashRow);

		initDashboard(dashboardContainer, mahasiswa, null, false);

		Group groupJadwal = new ais.ui.util.MyGroupConfig(
				"Jadwal " + krsMahasiswa.getTahunAkademik() + "/" + krsMahasiswa.getSemester());
		groupJadwal.setStyle(" font-weight: bold; color: #495057; font-size: 14px; padding: 10px;");
		rows.appendChild(groupJadwal);
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Jadwal Perkuliahan", "Daftar kelas semester terpilih membantu mahasiswa membuka kelas, materi, tugas, dan ujian bila fitur tersedia.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox cariHbox = new Hbox();
		cariHbox.setParent(row);
		cariHbox.appendChild(new Space());
		cariHbox.appendChild(new Space());
		final Textbox cariPerkuliahan = new Textbox();
		cariPerkuliahan.setStyle("border-radius: 15px; border: 1px solid #ced4da; padding: 3px 10px;");
		cariPerkuliahan.setCols(20);
		cariPerkuliahan.setParent(cariHbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Row rowTugas = new MyRowStyled();
		Row rowUjian = new MyRowStyled();
		Row rowMateri = new MyRowStyled();

		final Rows rowsPerkuliahan = (Rows) Common.tampilanScroll1(row).getParent();
		final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
		final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
		final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

		final Textbox cariTugas = new Textbox();
		final Textbox cariUjian = new Textbox();
		final Textbox cariMateri = new Textbox();

		final EventListener rowEventPerkuliahan = new EventListener() {
			public EventListener getThis() {
				return this;
			}
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(krsMahasiswa.getTahunAkademik(),
						semesterPendek != null ? Perkuliahan.SP
								: (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL),
						hr, cariPerkuliahan.getValue() != null ? cariPerkuliahan.getValue().trim() : "", "", merupakanPraPerkuliahan, ekstrakurikuler, true,
						merupakanRemedial, false,
						TampilanELearningAction.PERKULIAHAN,
						(pagingPerkuliahan - 1) * jumlahDataDalamSatuHalamanElearning,
						jumlahDataDalamSatuHalamanElearning, false);

				List<Perkuliahan> perkuliahans = (List<Perkuliahan>) objects[0];
				if (tampilMateri && cariPerkuliahan.getValue() != null && cariPerkuliahan.getValue().trim().isEmpty()) {
					dataDiambil = (List<Perkuliahan>) objects[2];
				}

				Integer size = (Integer) objects[1];
				if (perkuliahans == null || perkuliahans.isEmpty()) {
					Row row = new MyRowStyled();
					row.setParent(rowsPerkuliahan);
					Label a;
					row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Tidak ada jadwal perkuliahan")));
					a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 10px;");
				} else {
					for (final Perkuliahan perkuliahan : perkuliahans) {
						if(perkuliahan == null) continue;
						Row row = new MyRowStyled();
						row.setParent(rowsPerkuliahan);
						Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(perkuliahan.infoSimple() + " "
								+ perkuliahan.getTahunAjaran() + "/" + perkuliahan.getGanjilGenap(),
								"/img/Healthcare-Groups-icon.png");
						toolbarbuttonData.setStyle("color: #0056b3; font-weight: 500;");
						row.appendChild(toolbarbuttonData);
						toolbarbuttonData.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								TampilanELearningAction.prosess(perkuliahan, true);
							}
						});
					}
				}
				perkuliahans = null;

				if (size > (pagingPerkuliahan * jumlahDataDalamSatuHalamanElearning)) {
					Row row = new MyRowStyled();
					rowsPerkuliahan.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
							"Tampilkan perkuliahan selanjutnya.. ("
									+ (size - (pagingPerkuliahan * jumlahDataDalamSatuHalamanElearning)) + " data)",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px; font-weight: bold; color: #28a745;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							arg0.getTarget().getParent().setVisible(false);
							try {
								pagingPerkuliahan++;
								getThis().onEvent(null);
							} catch (Exception e) {
								MyMessageboxConfig.show("Tidak ada perkuliahan selanjutnya yang bisa ditampilkan",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}
			}
		};

		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				Common.clear(rowsPerkuliahan);
				rowEventPerkuliahan.onEvent(arg0);
			}
		});
		button.setParent(cariHbox);

		cariPerkuliahan.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				Common.clear(rowsPerkuliahan);
				rowEventPerkuliahan.onEvent(arg0);
			}
		});

		EventListener reloadMateri = null;

		if (tampilMateri) {
			reloadMateri = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 3);
					rowsUjian.setAttribute("ditampilkan", 3);
					rowsMateri.setAttribute("ditampilkan", 3);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					TreeMap<String, Long> pertemuans = new TreeMap<String, Long>();
					if(dataDiambil != null) {
						for (Perkuliahan perkuliahan : dataDiambil) {
							if(perkuliahan != null) pertemuans.putAll(perkuliahan.ambilPertemuan());
						}
					}
					ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
							pertemuans, false, true, null, null, null, null, null);
				}
			};

			rows.appendChild(new ais.ui.util.MyGroupConfig(
					"Tugas " + krsMahasiswa.getTahunAkademik() + "/" + krsMahasiswa.getSemester()));

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			Hbox cariHboxTugas = new Hbox();
			cariHboxTugas.setParent(row);
			cariHboxTugas.appendChild(new Space());
			cariHboxTugas.appendChild(new Space());

			cariTugas.setStyle("border-radius: 15px; border: 1px solid #ced4da; padding: 3px 10px;");
			cariTugas.setCols(20);
			cariTugas.setParent(cariHboxTugas);
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", reloadMateri);
			button.setParent(cariHboxTugas);
			cariTugas.addEventListener("onOK", reloadMateri);

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rows.appendChild(new ais.ui.util.MyGroupConfig(
					"Ujian " + krsMahasiswa.getTahunAkademik() + "/" + krsMahasiswa.getSemester()));

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			Hbox cariHboxUjian = new Hbox();
			cariHboxUjian.setParent(row);
			cariHboxUjian.appendChild(new Space());
			cariHboxUjian.appendChild(new Space());

			cariUjian.setStyle("border-radius: 15px; border: 1px solid #ced4da; padding: 3px 10px;");
			cariUjian.setCols(20);
			cariUjian.setParent(cariHboxUjian);
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", reloadMateri);
			button.setParent(cariHboxUjian);
			cariUjian.addEventListener("onOK", reloadMateri);

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rows.appendChild(new ais.ui.util.MyGroupConfig(
					"Materi " + krsMahasiswa.getTahunAkademik() + "/" + krsMahasiswa.getSemester()));

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			Hbox cariHboxMateri = new Hbox();
			cariHboxMateri.setParent(row);
			cariHboxMateri.appendChild(new Space());
			cariHboxMateri.appendChild(new Space());

			cariMateri.setStyle("border-radius: 15px; border: 1px solid #ced4da; padding: 3px 10px;");
			cariMateri.setCols(20);
			cariMateri.setParent(cariHboxMateri);
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", reloadMateri);
			button.setParent(cariHboxMateri);
			cariMateri.addEventListener("onOK", reloadMateri);

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");
		}

		rowEventPerkuliahan.onEvent(null);

		if (reloadMateri != null) {
			reloadMateri.onEvent(null);
		}

		boolean mobile = Common.isMobile();
		if (mobile && !PengumumanAkademisAction.isKehadiranHomeDitampilkan()) {
			Tbmuser tbmuser = Common.getCurrentUser();
			String pengumuman = PengumumanAkademisAction.tampilkanKehadiranDosen(tbmuser, mobile);
			if (pengumuman != null && !pengumuman.isEmpty()) {
				row = new MyRowStyled();
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(new Html(pengumuman));
			}
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblInfoBayar = new MyLabelStyled("Info Pembayaran smt " + krsMahasiswa.getSemester());
		lblInfoBayar.setStyle("font-weight: bold; color: #495057; font-size: 12px;");
		row.appendChild(lblInfoBayar);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox hbox2D = new Hbox();
		hbox2D.setParent(row);

		toolbarbuttonLihat = new MyToolbarbuttonConfig("Informasi Tagihan dan Pembayaran",
				"/img/Status-dialog-information-icon.png");
		toolbarbuttonLihat.setStyle("color: #0056b3; font-weight: bold; padding: 5px;");
		hbox2D.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
			// PERBAIKAN (data mahasiswa tertukar saat "Lihat Tagihan"): dibuka LANGSUNG sebagai
			// method static (pola sama dgn SetingBiayaAction.onAddExternal), bukan lagi via
			// IFRAME + URL query-string -- menghilangkan celah ID salah ter-embed di URL.
			// Perilaku "refresh panel ini setelah popup ditutup" dipertahankan lewat callback
			// onClose (overload ke-4 InformasiPembayaranMahasiswaAction.onViewExternal).
			ais.action.master.InformasiPembayaranMahasiswaAction.onViewExternal(mahasiswa, null, null,
				new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, smt, semesterPendek, tampilMateri);
					}
				});
			}
		});

		Toolbarbutton toolbarbuttonData;

		if (Common.bolehKonfigurasi("tampilkan_kkn_di_dashboard_samping")) {

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			
			MyLabelStyled lblKkn = new MyLabelStyled(Common.getBahasaConfig("KKN"));
			lblKkn.setStyle("font-weight: bold; color: #495057;");
			row.appendChild(lblKkn);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword != null ? keyword.trim() : "",
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.KKN, 0, 100, true);
			List<KelompokKkn> kelompokKkns = (List<KelompokKkn>) objects[0];

			if (kelompokKkns == null || kelompokKkns.isEmpty()) {
				row = new MyRowStyled();
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				Label a;
				row.appendChild(a = new Label("Anda belum mengikuti " + Common.getBahasaConfig("KKN")));
				a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");
			} else {
				row = new MyRowStyled();
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				Rows rowsLocal = (Rows) Common.tampilanScroll1(row).getParent();

				for (final KelompokKkn kelompokKkn : kelompokKkns) {
					// KE-6: kelompokKkn.getKkn() bisa null (data KKN tak lengkap) -> NPE saat
					// getKkn().getTahunAkademik()/getSemester(). Lewati baris yang Kkn-nya null.
					if(kelompokKkn == null || kelompokKkn.getKkn() == null) continue;
					row = new MyRowStyled();
					row.setParent(rowsLocal);
					toolbarbuttonData = new MyToolbarbuttonConfig(kelompokKkn.getNama() + " ("
							+ kelompokKkn.getKkn().getTahunAkademik() + "/" + kelompokKkn.getKkn().getSemester() + ")",
							"/img/Healthcare-Groups-icon.png");
					toolbarbuttonData.setStyle("color: #0056b3;");
					row.appendChild(toolbarbuttonData);
					toolbarbuttonData.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanELearningAction.prosess(kelompokKkn, true);
						}
					});

					if (tampilMateri) {
						ProfileUtil.tampilkanMateri(rowsLocal, kelompokKkn.ambilPertemuan());
					}
				}
				kelompokKkns = null;
			}
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblPkl = new MyLabelStyled(Common.getBahasaConfig("PKL"));
		lblPkl.setStyle("font-weight: bold; color: #495057;");
		row.appendChild(lblPkl);

		Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword != null ? keyword.trim() : "", "",
				merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false, TampilanELearningAction.PKL,
				0, 100, false);
		List<KelompokPkl> kelompokPkls = (List<KelompokPkl>) objects[0];

		if (kelompokPkls == null || kelompokPkls.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label("Anda belum mengikuti " + Common.getBahasaConfig("PKL")));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");
		} else {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Rows rowsLocal = (Rows) Common.tampilanScroll1(row).getParent();

			for (final KelompokPkl kelompokPkl : kelompokPkls) {
				if(kelompokPkl == null) continue;
				row = new MyRowStyled();
				row.setParent(rowsLocal);
				toolbarbuttonData = new MyToolbarbuttonConfig(kelompokPkl.getNama() + " ("
						+ kelompokPkl.getPkl().getTahunAkademik() + "/" + kelompokPkl.getPkl().getSemester() + ")",
						"/img/options-icon.png");
				toolbarbuttonData.setStyle("color: #0056b3;");
				row.appendChild(toolbarbuttonData);
				toolbarbuttonData.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanELearningAction.prosess(kelompokPkl, true);
					}
				});
				if (tampilMateri) {
					ProfileUtil.tampilkanMateri(rowsLocal, kelompokPkl.ambilPertemuan());
				}
			}
			kelompokPkls = null;
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblPA = new MyLabelStyled(Common.getBahasaConfig("Pembimbing Akademik"));
		lblPA.setStyle("font-weight: bold; color: #495057; padding-top: 10px;");
		row.appendChild(lblPA);

		if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			
			MyLabelBoldAja lblDosPem = new MyLabelBoldAja("Dosen Pembimbing:");
			lblDosPem.setStyle("color: #6c757d; font-size: 11px;");
			row.appendChild(lblDosPem);

			row = new MyRowStyled();
			row.setParent(rows);
			row.setStyle("padding: 10px; background: #ffffff; border: 1px solid #e9ecef; border-radius: 5px; box-shadow: 0 1px 2px rgba(0,0,0,0.05);");
			
			try {
				CommonMedia.tampilkanGambarKecil(krsMahasiswa.getDosenPa()).setParent(row);
			} catch (Exception e) {
				new MyLabelKecil().setParent(row);
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileMahasiswa.java:1081");
			}
			Vbox vbox = new Vbox();
			vbox.setParent(row);

			vbox.appendChild(new MyLabelBoldAja(krsMahasiswa.getDosenPa().getNidn()));
			vbox.appendChild(new MyLabelBoldAja(krsMahasiswa.getDosenPa().getNama()));

			krsMahasiswa.getDosenPa().tampilkanHp(vbox);
			krsMahasiswa.getDosenPa().tampilkanEmail(vbox);
		}

		if (tampilMateri) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Rows rowsLocal = (Rows) Common.tampilanScroll1(row).getParent();
			ProfileUtil.tampilkanMateri(rowsLocal, krsMahasiswa.ambilPertemuan());
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		toolbarbuttonLihat = new MyToolbarbuttonConfig("Lihat KRS dan Konsultasi", "/img/laptop.png");
		toolbarbuttonLihat.setStyle("font-weight: bold; color: #0056b3; padding-top: 10px;");
		row.appendChild(toolbarbuttonLihat);
		toolbarbuttonLihat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				TampilanELearningAction.prosess(krsMahasiswa, true);
			}
		});

		List<JenisFormulirKegiatan> jenisFormulirKegiatans = new ArrayList<JenisFormulirKegiatan>();
		int adakosong = 0;
		Session sessionFormulir = null;
		try {
			sessionFormulir = HibernateUtil.getSessionFactory().openSession();
			jenisFormulirKegiatans = ConstantValues.simpleList(sessionFormulir.createCriteria(FormulirKegiatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("jenisFormulirKegiatan"))
					.setProjection(Projections.groupProperty("jenisFormulirKegiatan.id"))
					.addOrder(Order.asc("jenisFormulirKegiatan.id")), JenisFormulirKegiatan.class, false);
			adakosong = ((Number) sessionFormulir.createCriteria(FormulirKegiatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNull("jenisFormulirKegiatan")).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileMahasiswa.java:1130");
		} finally {
			ProfileUiHelper.closeOpenSession(sessionFormulir);
		}

		for (final JenisFormulirKegiatan jenisFormulirKegiatan : jenisFormulirKegiatans) {
			if (jenisFormulirKegiatan == null) continue;
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			
			MyLabelStyled lblKegiatan = new MyLabelStyled(jenisFormulirKegiatan.getNama());
			lblKegiatan.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
			row.appendChild(lblKegiatan);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			cariHbox = new Hbox();
			cariHbox.setParent(row);
			cariHbox.appendChild(new Space());
			cariHbox.appendChild(new Space());
			final Textbox cariFormulirKegiatan = new Textbox();
			cariFormulirKegiatan.setStyle("border-radius: 15px; border: 1px solid #ced4da; padding: 3px 10px;");
			cariFormulirKegiatan.setCols(20);
			cariFormulirKegiatan.setParent(cariHbox);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			final Rows rowsFormulirKegiatan = (Rows) Common.tampilanScroll1(row).getParent();

			final EventListener rowEventFormulirKegiatan = new EventListener() {
				public EventListener getThis() {
					return this;
				}
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr,
							keyword != null ? keyword.trim() : "", "", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial,
							false,
							TampilanELearningAction.KEGIATAN,
							(pagingFormulirKegiatan - 1) * jumlahDataDalamSatuHalamanElearning,
							jumlahDataDalamSatuHalamanElearning, true, jenisFormulirKegiatan);
					List<FormulirKegiatan> formulirKegiatans = (List<FormulirKegiatan>) objects[0];

					if (formulirKegiatans == null || formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						row.setParent(rowsFormulirKegiatan);
						Label a;
						row.appendChild(a = new Label("Tidak ada jadwal Kegiatan / Seminar"));
						a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");
					} else {
						for (final FormulirKegiatan formulirKegiatan : formulirKegiatans) {
							if(formulirKegiatan == null) continue;
							Row row = new MyRowStyled();
							row.setParent(rowsFormulirKegiatan);
							Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(formulirKegiatan.infoSimple()
									+ " " + formulirKegiatan.getTahunAkademik() + "/" + formulirKegiatan.getSemester(),
									"/img/vendor.png");
							toolbarbuttonData.setStyle("color: #0056b3;");
							row.appendChild(toolbarbuttonData);
							toolbarbuttonData.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									TampilanELearningAction.prosess(formulirKegiatan, true);
								}
							});

							if (tampilMateri) {
								ProfileUtil.tampilkanMateri(rowsFormulirKegiatan, formulirKegiatan.ambilPertemuan());
							}
						}
					}

					if (formulirKegiatans != null && !formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						rowsFormulirKegiatan.appendChild(row);

						Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan data selanjutnya..",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:11px; font-weight: bold; color: #28a745;");
						row.appendChild(a);
						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								arg0.getTarget().getParent().setVisible(false);
								try {
									pagingFormulirKegiatan++;
									getThis().onEvent(null);
								} catch (Exception e) {
									MyMessageboxConfig.show(
											"Tidak ada " + jenisFormulirKegiatan.getNama()
													+ " selanjutnya yang bisa ditampilkan",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}
						});
					}
					formulirKegiatans = null;
				}
			};

			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});
			button.setParent(cariHbox);

			cariFormulirKegiatan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});

			rowEventFormulirKegiatan.onEvent(null);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ajukan Baru",
					"/img/Document-Write-icon.png");
			toolbarbutton.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus(
							jenisFormulirKegiatan);
					laporan.setTitle("Pengajuan " + jenisFormulirKegiatan.getNama());
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporan.onModal();

					laporan.addEventListener("onClose", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, smt, semesterPendek, tampilMateri);
						}
					});
				}
			});
		}

		if (adakosong > 0) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			
			MyLabelStyled lblKgtSem = new MyLabelStyled(Common.getBahasaConfig("Kegiatan / Seminar"));
			lblKgtSem.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
			row.appendChild(lblKgtSem);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			cariHbox = new Hbox();
			cariHbox.setParent(row);
			cariHbox.appendChild(new Space());
			cariHbox.appendChild(new Space());
			final Textbox cariFormulirKegiatan = new Textbox();
			cariFormulirKegiatan.setStyle("border-radius: 15px; border: 1px solid #ced4da; padding: 3px 10px;");
			cariFormulirKegiatan.setCols(20);
			cariFormulirKegiatan.setParent(cariHbox);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			final Rows rowsFormulirKegiatan = (Rows) Common.tampilanScroll1(row).getParent();

			final EventListener rowEventFormulirKegiatan = new EventListener() {
				public EventListener getThis() {
					return this;
				}
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr,
							keyword != null ? keyword.trim() : "", "", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial,
							false,
							TampilanELearningAction.KEGIATAN,
							(pagingFormulirKegiatan - 1) * jumlahDataDalamSatuHalamanElearning,
							jumlahDataDalamSatuHalamanElearning, true);
					List<FormulirKegiatan> formulirKegiatans = (List<FormulirKegiatan>) objects[0];

					if (formulirKegiatans == null || formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						row.setParent(rowsFormulirKegiatan);
						Label a;
						row.appendChild(a = new Label("Tidak ada jadwal Kegiatan / Seminar"));
						a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");
					} else {
						for (final FormulirKegiatan formulirKegiatan : formulirKegiatans) {
							if(formulirKegiatan == null) continue;
							Row row = new MyRowStyled();
							row.setParent(rowsFormulirKegiatan);
							Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(formulirKegiatan.infoSimple()
									+ " " + formulirKegiatan.getTahunAkademik() + "/" + formulirKegiatan.getSemester(),
									"/img/vendor.png");
							toolbarbuttonData.setStyle("color: #0056b3;");
							row.appendChild(toolbarbuttonData);
							toolbarbuttonData.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									TampilanELearningAction.prosess(formulirKegiatan, true);
								}
							});

							if (tampilMateri) {
								ProfileUtil.tampilkanMateri(rowsFormulirKegiatan, formulirKegiatan.ambilPertemuan());
							}
						}
					}

					if (formulirKegiatans != null && !formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						rowsFormulirKegiatan.appendChild(row);

						Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan kegiatan / seminar selanjutnya..",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:11px; font-weight: bold; color: #28a745;");
						row.appendChild(a);
						a.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								arg0.getTarget().getParent().setVisible(false);
								try {
									pagingFormulirKegiatan++;
									getThis().onEvent(null);
								} catch (Exception e) {
									MyMessageboxConfig.show(
											"Tidak ada kegiatan / seminar selanjutnya yang bisa ditampilkan",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}
						});
					}
					formulirKegiatans = null;
				}
			};

			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});
			button.setParent(cariHbox);

			cariFormulirKegiatan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});

			rowEventFormulirKegiatan.onEvent(null);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ajukan Baru",
					"/img/Document-Write-icon.png");
			toolbarbutton.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanPendidikanLingkunganKampus laporan = new LaporanPendidikanLingkunganKampus();
					laporan.setTitle("Pengajuan Aktifitas / Kegiatan Mahasiswa");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporan.onModal();

					laporan.addEventListener("onClose", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, smt, semesterPendek, tampilMateri);
						}
					});
				}
			});
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblBimbingan = new MyLabelStyled("Bimbingan TA/Skripsi");
		lblBimbingan.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
		row.appendChild(lblBimbingan);

		objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword != null ? keyword.trim() : "", "",
				merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
				TampilanELearningAction.BIMBINGAN, 0, 100, false);
		List<MahasiswaRequestTugasAkhir> bimbingan = (List<MahasiswaRequestTugasAkhir>) objects[0];

		if (bimbingan == null || bimbingan.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label("Anda belum mengajukan bimbingan TA/Skripsi"));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");
		} else {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Rows rowsLocal = (Rows) Common.tampilanScroll1(row).getParent();

			for (final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : bimbingan) {
				if(mahasiswaRequestTugasAkhir == null) continue;
				row = new MyRowStyled();
				row.setParent(rowsLocal);

				toolbarbuttonData = new MyToolbarbuttonConfig(
						(mahasiswaRequestTugasAkhir.getJudul() == null || mahasiswaRequestTugasAkhir.getJudul().isEmpty() ? mahasiswaRequestTugasAkhir.getJudul1()
								: mahasiswaRequestTugasAkhir.getJudul()) + " ("
								+ mahasiswaRequestTugasAkhir.getTahunAkademik() + "/"
								+ mahasiswaRequestTugasAkhir.getSemester() + ")",
						"/img/Document-Write-icon.png");
				toolbarbuttonData.setStyle("color: #0056b3;");
				row.appendChild(toolbarbuttonData);

				row = new MyRowStyled();
				row.setParent(rowsLocal);

				Hbox hboxData = new Hbox();
				row.appendChild(hboxData);
				hboxData.appendChild(new Space());
				hboxData.appendChild(new Space());

				hboxData.appendChild(new Image(mahasiswaRequestTugasAkhir.getStatus() != null && (mahasiswaRequestTugasAkhir.getStatus()
						.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
						|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
						|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
						|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS))
								? "/img/svg/check2-circle.svg"
								: "/img/cancel.gif"));
				
				MyLabelKecil lblStatus = new MyLabelKecil(mahasiswaRequestTugasAkhir.getStatus());
				lblStatus.setStyle("padding-left: 5px; color: #28a745; font-weight: bold;");
				hboxData.appendChild(lblStatus);

				toolbarbuttonData.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanELearningAction.prosess(mahasiswaRequestTugasAkhir, true);
					}
				});

				if (tampilMateri) {
					ProfileUtil.tampilkanMateri(rowsLocal, mahasiswaRequestTugasAkhir.ambilPertemuan());
				}
			}
		}
		bimbingan = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Document-Write-icon.png");
		toolbarbutton.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = new MahasiswaRequestTugasAkhir();
				mahasiswaRequestTugasAkhir.setMahasiswa(mahasiswa);
				MahasiswaRequestTugasAkhirAction.onAddExternal(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) arg0
								.getData();
						Mahasiswa mahasiswa = mahasiswaRequestTugasAkhir.getMahasiswa();
						MyMessageboxConfig.show(
								"Mahasiswa dengan NIM " + mahasiswa.getNim() + " nama " + mahasiswa.getNama()
										+ " telah berhasil mengajukan dengan judul:\n\n"
										+ (mahasiswaRequestTugasAkhir.getJudul() == null || mahasiswaRequestTugasAkhir.getJudul().isEmpty()
												? mahasiswaRequestTugasAkhir.getJudul1()
												: mahasiswaRequestTugasAkhir.getJudul()),
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										init(parent, smt, semesterPendek, tampilMateri);
									}
								});
					}
				}, mahasiswaRequestTugasAkhir);
			}
		});

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblSidang = new MyLabelStyled("Sidang TA/Skripsi");
		lblSidang.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
		row.appendChild(lblSidang);

		objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword != null ? keyword.trim() : "", "",
				merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
				TampilanELearningAction.SKRIPSI, 0, 100, false);
		List<Skripsi> skripsis = (List<Skripsi>) objects[0];

		if (skripsis == null || skripsis.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label("Anda belum mengajukan sidang TA/Skripsi"));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");
		} else {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Rows rowsLocal = (Rows) Common.tampilanScroll1(row).getParent();

			for (final Skripsi skripsi : skripsis) {
				if(skripsi == null) continue;
				row = new MyRowStyled();
				row.setParent(rowsLocal);

				toolbarbuttonData = new MyToolbarbuttonConfig(
						skripsi.getJudul() + " (" + skripsi.getTahunAkademik() + "/" + skripsi.getSemester() + ")",
						"/img/certificate-icon.png");
				toolbarbuttonData.setStyle("color: #0056b3;");
				row.appendChild(toolbarbuttonData);

				row = new MyRowStyled();
				row.setParent(rowsLocal);

				Hbox hboxData = new Hbox();
				row.appendChild(hboxData);
				hboxData.appendChild(new Space());
				hboxData.appendChild(new Space());

				hboxData.appendChild(
						new Image(skripsi.getSetujuiSidang() != null && skripsi.getSetujuiSidang() ? "/img/svg/check2-circle.svg" : "/img/cancel.gif"));
				
				MyLabelAgakKecil lblAcc = new MyLabelAgakKecil(skripsi.getSetujuiSidang() != null && skripsi.getSetujuiSidang() ? "Disetujui" : "Belum Disetujui");
				lblAcc.setStyle("padding-left: 5px; color: " + (skripsi.getSetujuiSidang() != null && skripsi.getSetujuiSidang() ? "#28a745" : "#dc3545") + "; font-weight: bold;");
				hboxData.appendChild(lblAcc);

				toolbarbuttonData.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanELearningAction.prosess(skripsi, true);
					}
				});

				if (tampilMateri) {
					ProfileUtil.tampilkanMateri(rowsLocal, skripsi.ambilPertemuan());
				}
			}
		}
		skripsis = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		toolbarbutton = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Document-Write-icon.png");
		toolbarbutton.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Skripsi skripsi = new Skripsi();
				skripsi.setMahasiswa(mahasiswa);
				SkripsiAction.onAddExternal(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Skripsi skripsi = (Skripsi) arg0.getData();
						Mahasiswa mahasiswa = skripsi.getMahasiswa();
						MyMessageboxConfig.show(
								"Mahasiswa dengan NIM " + mahasiswa.getNim() + " nama " + mahasiswa.getNama()
										+ " telah berhasil mengajukan sidang dengan judul:\n\n" + skripsi.getJudul(),
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										init(parent, smt, semesterPendek, tampilMateri);
									}
								});
					}
				}, skripsi, mahasiswa);
			}
		});

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblKgtnMhs = new MyLabelStyled("Kegiatan Kemahasiswaan");
		lblKgtnMhs.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
		row.appendChild(lblKgtnMhs);

		List<Long> kegiatanKemahasiswaans = mahasiswa.ambilKegiatanKemahasiswaanPunyaMahasiswa();
		if (kegiatanKemahasiswaans == null || kegiatanKemahasiswaans.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan kegiatan kemahasiswaan")));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			toolbarbuttonData.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.addEventListener(Events.ON_CLOSE, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, smt, semesterPendek, tampilMateri);
						}
					});
					laporan.setTitle("Kegiatan Kemahasiswaan");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					MahasiswaPunyaKegiatanKemahasiswaanHelper detailperkuliahanHelper = new MahasiswaPunyaKegiatanKemahasiswaanHelper();
					detailperkuliahanHelper.display(mahasiswa, laporan);

					laporan.onModal();
				}
			});
		} else {
			for (final Long id : kegiatanKemahasiswaans) {
				if(id == null) continue;
				KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) GeneralValueObject
						.ambilData(KegiatanKemahasiswaanPunyaMahasiswa.class, id.toString());
				if (kegiatanKemahasiswaanPunyaMahasiswa != null && kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan() != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(
							kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama() + " ("
									+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getTahunAkademik()
									+ "/"
									+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getJenisSemester()
									+ ")",
							"/img/Resume-icon.png");
					toolbarbuttonData.setStyle("color: #0056b3;");
					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());

					boolean setuju = kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() != null && kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan();
					hboxData.appendChild(new Image(setuju ? "/img/svg/check2-circle.svg" : "/img/cancel.gif"));
					
					MyLabelKecil lblSetuju = new MyLabelKecil(setuju ? "Disetujui" : "Belum Disetujui");
					lblSetuju.setStyle("padding-left: 5px; color: " + (setuju ? "#28a745" : "#dc3545") + "; font-weight: bold;");
					hboxData.appendChild(lblSetuju);

					toolbarbuttonData.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) GeneralValueObject
									.ambilData(KegiatanKemahasiswaanPunyaMahasiswa.class, id.toString());
							MyWindow laporan = new MyWindow();
							laporan.addEventListener(Events.ON_CLOSE, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, smt, semesterPendek, tampilMateri);
								}
							});
							laporan.setTitle("Kegiatan Kemahasiswaan");
							laporan.setClosable(true);
							laporan.setHeight("95%");
							laporan.setWidth("90%");
							laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							MahasiswaPunyaKegiatanKemahasiswaanHelper detailperkuliahanHelper = new MahasiswaPunyaKegiatanKemahasiswaanHelper();
							detailperkuliahanHelper.display(mahasiswa, laporan, kegiatanKemahasiswaanPunyaMahasiswa);

							laporan.onModal();
						}
					});
				}
			}
		}
		kegiatanKemahasiswaans = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblOrmawa = new MyLabelStyled("Organisasi Mahasiswa");
		lblOrmawa.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
		row.appendChild(lblOrmawa);

		List<Long> organisasiMahasiswa = mahasiswa.ambilOrganisasiIntraKampusPunyaMahasiswa();
		if (organisasiMahasiswa == null || organisasiMahasiswa.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan organisasi mahasiswa")));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			toolbarbuttonData.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.addEventListener(Events.ON_CLOSE, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, smt, semesterPendek, tampilMateri);
						}
					});
					laporan.setTitle("Organisasi Mahasiswa");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					MahasiswaPunyaOrganisasiIntraKampusHelper detailperkuliahanHelper = new MahasiswaPunyaOrganisasiIntraKampusHelper();
					detailperkuliahanHelper.display(mahasiswa, laporan);

					laporan.onModal();
				}
			});
		} else {
			for (final Long id : organisasiMahasiswa) {
				if(id == null) continue;
				OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) GeneralValueObject
						.ambilData(OrganisasiIntraKampusPunyaMahasiswa.class, id.toString());
				if (organisasiIntraKampusPunyaMahasiswa != null && organisasiIntraKampusPunyaMahasiswa.getOrganisasiIntraKampus() != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(
							organisasiIntraKampusPunyaMahasiswa.getOrganisasiIntraKampus().getNama(),
							"/img/Resume-icon.png");
					toolbarbuttonData.setStyle("color: #0056b3;");
					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());
					
					boolean setujuOrmawa = organisasiIntraKampusPunyaMahasiswa.getPersetujuan() != null && organisasiIntraKampusPunyaMahasiswa.getPersetujuan();
					hboxData.appendChild(new Image(setujuOrmawa ? "/img/svg/check2-circle.svg" : "/img/cancel.gif"));
					
					MyLabelKecil lblSetujuOrmawa = new MyLabelKecil(setujuOrmawa ? "Disetujui" : "Belum Disetujui");
					lblSetujuOrmawa.setStyle("padding-left: 5px; color: " + (setujuOrmawa ? "#28a745" : "#dc3545") + "; font-weight: bold;");
					hboxData.appendChild(lblSetujuOrmawa);

					toolbarbuttonData.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) GeneralValueObject
									.ambilData(OrganisasiIntraKampusPunyaMahasiswa.class, id.toString());

							MyWindow laporan = new MyWindow();
							laporan.addEventListener(Events.ON_CLOSE, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, smt, semesterPendek, tampilMateri);
								}
							});
							laporan.setTitle("Organisasi Mahasiswa");
							laporan.setClosable(true);
							laporan.setHeight("95%");
							laporan.setWidth("90%");
							laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							MahasiswaPunyaOrganisasiIntraKampusHelper detailperkuliahanHelper = new MahasiswaPunyaOrganisasiIntraKampusHelper();
							detailperkuliahanHelper.display(mahasiswa, laporan, organisasiIntraKampusPunyaMahasiswa);

							laporan.onModal();
						}
					});
				}
			}
		}
		organisasiMahasiswa = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblPrestasi = new MyLabelStyled("Prestasi Mahasiswa");
		lblPrestasi.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
		row.appendChild(lblPrestasi);

		List<Long> prestasiMahasiswas = mahasiswa.ambilPrestasiMahasiswa();
		if (prestasiMahasiswas == null || prestasiMahasiswas.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan prestasi mahasiswa")));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			toolbarbuttonData.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					String url = "/pages/master/prestasi_mahasiswa.zul?mahasiswa=" + mahasiswa.getId();
					Common.displayWindow(url, false, "95%", "95%", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, smt, semesterPendek, tampilMateri);
						}
					}, "Prestasi Mahasiswa");
				}
			});
		} else {
			for (final Long id : prestasiMahasiswas) {
				if(id == null) continue;
				PrestasiMahasiswa prestasiMahasiswa = (PrestasiMahasiswa) GeneralValueObject
						.ambilData(PrestasiMahasiswa.class, id.toString());
				if (prestasiMahasiswa != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(prestasiMahasiswa.getNama(), "/img/Resume-icon.png");
					toolbarbuttonData.setStyle("color: #0056b3;");
					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());

					boolean setujuPrestasi = prestasiMahasiswa.getStatus() != null && prestasiMahasiswa.getStatus().equals(PrestasiMahasiswa.DISETUJUI);
					hboxData.appendChild(new Image(setujuPrestasi ? "/img/svg/check2-circle.svg" : "/img/cancel.gif"));
					
					MyLabelKecil lblSetujuPrestasi = new MyLabelKecil(prestasiMahasiswa.getStatus());
					lblSetujuPrestasi.setStyle("padding-left: 5px; color: " + (setujuPrestasi ? "#28a745" : "#dc3545") + "; font-weight: bold;");
					hboxData.appendChild(lblSetujuPrestasi);

					toolbarbuttonData.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String url = "/pages/master/prestasi_mahasiswa.zul?mahasiswa=" + mahasiswa.getId()
									+ "&prestasi=" + id;
							Common.displayWindow(url, false, "95%", "95%", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, smt, semesterPendek, tampilMateri);
								}
							}, "Prestasi Mahasiswa");
						}
					});
				}
			}
		}
		prestasiMahasiswas = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		
		MyLabelStyled lblKarya = new MyLabelStyled("Karya Mahasiswa");
		lblKarya.setStyle("font-weight: bold; color: #495057; margin-top: 10px; display: inline-block;");
		row.appendChild(lblKarya);

		List<Long> penghargaanMahasiswas = mahasiswa.ambilPenghargaanMahasiswa();
		if (penghargaanMahasiswas == null || penghargaanMahasiswas.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan karya mahasiswa")));
			a.setStyle("font-size:11px;font-weight: bolder;color:#dc3545; padding: 5px;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			toolbarbuttonData.setStyle("font-weight: bold; border: 1px solid #dee2e6; border-radius: 4px; padding: 4px 10px; background: white;");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					String url = "/pages/master/penghargaan_mahasiswa.zul?mahasiswa=" + mahasiswa.getId();
					Common.displayWindow(url, false, "95%", "95%", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, smt, semesterPendek, tampilMateri);
						}
					}, "Penghargaan Mahasiswa");
				}
			});

		} else {
			for (final Long id : penghargaanMahasiswas) {
				if(id == null) continue;
				PenghargaanMahasiswa penghargaanMahasiswa = (PenghargaanMahasiswa) GeneralValueObject
						.ambilData(PenghargaanMahasiswa.class, id.toString());
				if (penghargaanMahasiswa != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(penghargaanMahasiswa.getNama(),
							"/img/Resume-icon.png");
					toolbarbuttonData.setStyle("color: #0056b3;");
					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());

					boolean setujuKarya = penghargaanMahasiswa.getStatus() != null && penghargaanMahasiswa.getStatus().equals(PenghargaanMahasiswa.DISETUJUI);
					hboxData.appendChild(
							new Image(setujuKarya ? "/img/svg/check2-circle.svg" : "/img/cancel.gif"));
							
					MyLabelKecil lblSetujuKarya = new MyLabelKecil(penghargaanMahasiswa.getStatus());
					lblSetujuKarya.setStyle("padding-left: 5px; color: " + (setujuKarya ? "#28a745" : "#dc3545") + "; font-weight: bold;");
					hboxData.appendChild(lblSetujuKarya);

					toolbarbuttonData.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String url = "/pages/master/penghargaan_mahasiswa.zul?mahasiswa=" + mahasiswa.getId()
									+ "&penghargaan=" + id;
							Common.displayWindow(url, false, "95%", "95%", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, smt, semesterPendek, tampilMateri);
								}
							}, "Karya Mahasiswa");
						}
					});
				}
			}
		}
		penghargaanMahasiswas = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
	}
	

	/**
	 * Membangun sub-panel "Ringkasan Studi" mahasiswa: statistik, tren, radar, daftar matakuliah.
	 *
	 * <p><b>Tujuan:</b> Menyajikan ringkasan komprehensif perjalanan studi mahasiswa
	 * sejak semester pertama hingga saat ini dalam satu panel visual. Dipanggil dari
	 * {@link #init(Component, int, Integer, boolean)} untuk mengisi container khusus.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuka sesi Hibernate mandiri untuk mengambil semua {@code Detailperkuliahan}
	 *       mahasiswa diurutkan berdasarkan semester dan id.</li>
	 *   <li>Iterasi koleksi: menghitung totalValid ({@code saringBerdasarNilaiDan0}),
	 *       totalLulus, totalKonversi, dan membangun map per-semester ({@code mapSemesterData}).</li>
	 *   <li>Per-semester: sinkronisasi KRS untuk mendapatkan IPS/IPK, lalu mengisi
	 *       {@code trendData} (list map dengan kunci sks/sksk/ips/ipk/smt).</li>
	 *   <li>Menampilkan 7 kartu stat Ringkasan Studi (Matakuliah, SKS, Lulus,
	 *       Konversi, IPK, IPS, SKS Kum) via {@link ProfileUiHelper#statsWrap}.</li>
	 *   <li>Menampilkan panel Tren Akademik (batang per semester) via
	 *       {@link ProfileUiHelper#buildTrendBars(List)}.</li>
	 *   <li>Menampilkan panel Spider Web 4-sumbu (IPK, IPS, SKS, Valid) via
	 *       {@link ProfileUiHelper#buildRadar(double, double, double, double, double)}.</li>
	 *   <li>Membangun Grid paging-10 Daftar Matakuliah dengan kolom Kode, Nama,
	 *       SKS, Nilai, Semester (dari {@code detailData}).</li>
	 *   <li>Tombol "Refresh Ringkasan Studi" memanggil {@code loadData} ulang.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter {@code keDatabase}:</b> Jika {@code true}, data diambil ulang dari
	 * database (dipakai saat Refresh). Jika {@code false}, menggunakan data yang sudah ada
	 * di memori (pertama kali render).</p>
	 *
	 * <p><b>Penanganan error:</b> Jika mahasiswa null, menampilkan placeholder teks.
	 * Sesi Hibernate ditutup di blok finally. Exception per-seksi tidak menghentikan
	 * render seksi lainnya.</p>
	 *
	 * @param parent       komponen ZK tujuan sub-panel; jika null method langsung return
	 * @param mahasiswa    entitas mahasiswa yang datanya akan dirangkum; boleh null
	 * @param dataLoader   tidak digunakan saat ini (dipertahankan untuk ekstensi masa depan);
	 *                     boleh null
	 * @param keDatabase   {@code true} untuk memaksa reload dari database; {@code false}
	 *                     untuk render pertama kali
	 */
	private void initDashboard(final Component parent, final Mahasiswa mahasiswa, final DataLoader dataLoader,
			boolean keDatabase) {
		if (parent == null) {
			return;
		}
		Component contentParent = ProfileUiHelper.prepareContentParent(parent);

		if (mahasiswa == null) {
			contentParent.appendChild(new Html(ProfileUiHelper.panel("Ringkasan Studi",
					"Data mahasiswa belum tersedia.",
					"<div style=\"padding:14px;color:#64748b;font-size:11px;\">Data mahasiswa belum tersedia.</div>")));
			return;
		}

		Vbox container = new Vbox();
		container.setWidth("100%");
		container.setStyle("background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:10px;box-sizing:border-box;");
		container.setParent(contentParent);


		Map<String, Map<String, Object>> mapSemesterData = new TreeMap<String, Map<String, Object>>();
		List<Map<String, Object>> detailData = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> trendData = new ArrayList<Map<String, Object>>();

		int totalValid = 0;
		int totalTidakValid = 0;
		int totalLulus = 0;
		int totalTidakLulus = 0;
		int totalKonversi = 0;
		double sksKumulatifAkhir = 0.0;
		double ipsTerakhir = 0.0;
		double ipkTerakhir = 0.0;

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			if (keDatabase) {
				mahasiswa.reInitDetailperkuliahan(session);
			}

			List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.asc("semester"))
					.addOrder(Order.asc("id")).list();

			List<Long> allDetailIds = new ArrayList<Long>();
			if (detailperkuliahans != null) {
				for (Detailperkuliahan detail : detailperkuliahans) {
					if (detail != null && detail.getId() != null) {
						allDetailIds.add(detail.getId());
					}
				}
			}
			try {
				List<Long> idsDariMahasiswa = mahasiswa.ambilDetailperkuliahan();
				if (idsDariMahasiswa != null && !idsDariMahasiswa.isEmpty()) {
					allDetailIds = idsDariMahasiswa;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Collection<Long> detailValid = mahasiswa.saringBerdasarNilaiDan0(allDetailIds);
			Set<Long> validIds = new HashSet<Long>();
			if (detailValid != null) {
				validIds.addAll(detailValid);
			}

			// Alasan mengapa sebuah mata kuliah "Tidak Valid" (map id -> penjelasan)
			Map<Long, String> alasanTidakValid = new HashMap<Long, String>();
			try {
				Map<Long, String> m = mahasiswa.alasanTidakValidDetail(allDetailIds);
				if (m != null) {
					alasanTidakValid = m;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (detailperkuliahans != null) {
				for (Detailperkuliahan dp : detailperkuliahans) {
					if (dp == null) {
						continue;
					}

					Matakuliah matakuliah = dp.getPerkuliahan() != null ? dp.getPerkuliahan().getMatakuliah()
							: dp.getMatakuliahKonversi();
					if (matakuliah == null) {
						continue;
					}

					try {
						Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah, mahasiswa.getNim(), true);
						if (matakuliahs != null && matakuliahs.length > 0 && matakuliahs[0] != null) {
							matakuliah = matakuliahs[0];
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					boolean valid = dp.getId() != null && validIds.contains(dp.getId());
					if (valid) {
						totalValid++;
					} else {
						totalTidakValid++;
					}
					if (dp.getLulus() != null && dp.getLulus().booleanValue()) {
						totalLulus++;
					} else {
						totalTidakLulus++;
					}
					if (dp.getMatakuliahKonversi() != null) {
						totalKonversi++;
					}

					String idSmtKey = dp.getIdSmt();
					Integer semesterAsli = dp.getSemester();
					if (idSmtKey != null && semesterAsli != null && semesterAsli.intValue() > 0
							&& !mapSemesterData.containsKey(idSmtKey)) {
						Map<String, Object> paramData = new HashMap<String, Object>();
						paramData.put("semesterAsli", semesterAsli);
						paramData.put("tahap", dp.getTahap());
						paramData.put("semesterPendek",
								dp.getPerkuliahan() == null ? null : dp.getPerkuliahan().getStatusSemesterPendek());
						mapSemesterData.put(idSmtKey, paramData);
					}

					Map<String, Object> dt = new HashMap<String, Object>();
					dt.put("kode", matakuliah.getKode());
					dt.put("nama", matakuliah.getNama());
					dt.put("smt", dp.getSemester());
					dt.put("sks", matakuliah.getSks());
					dt.put("nilai_angka", dp.getTotalNilai());
					dt.put("nilai_ip", dp.getTotalIP());
					dt.put("huruf", dp.getNilaiHuruf());
					dt.put("valid", valid ? "Valid" : "Tidak Valid");
					if (!valid && dp.getId() != null) {
						String alasan = alasanTidakValid.get(dp.getId());
						if (alasan != null && alasan.length() > 0) {
							dt.put("alasan", alasan);
						}
					}
					detailData.add(dt);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			ProfileUiHelper.closeOpenSession(session);
		}

		for (Map.Entry<String, Map<String, Object>> entry : mapSemesterData.entrySet()) {
			Map<String, Object> params = entry.getValue();
			Integer semesterAsli = (Integer) params.get("semesterAsli");
			Integer tahap = (Integer) params.get("tahap");
			Integer semesterPendekData = (Integer) params.get("semesterPendek");
			KrsMahasiswa krsLocal = null;
			try {
				krsLocal = Common.singkronkanKrsMahasiswa(mahasiswa, semesterAsli, tahap, semesterPendekData,
						keDatabase);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			if (krsLocal == null) {
				continue;
			}

			if (krsLocal.getMahasiswa() != null && krsLocal.getMahasiswa().getStatusKeluar() != null
					&& krsLocal.getMahasiswa().getSemesterLulus() != null && krsLocal.getSemester() != null
					&& krsLocal.getSemester().intValue() > krsLocal.getMahasiswa().getSemesterLulus().intValue()) {
				continue;
			}

			int sksSmt = krsLocal.getSksYangDiambil() == null ? 0 : krsLocal.getSksYangDiambil().intValue();
			int sksKum = krsLocal.getSksk() == null ? 0 : krsLocal.getSksk().intValue();
			double ips = krsLocal.getIps() == null ? 0.0 : krsLocal.getIps().doubleValue();
			double ipk = krsLocal.getIpk() == null ? 0.0 : krsLocal.getIpk().doubleValue();

			sksKumulatifAkhir = sksKum;
			ipsTerakhir = ips;
			ipkTerakhir = ipk;

			Map<String, Object> tr = new HashMap<String, Object>();
			tr.put("idSmt", entry.getKey());
			tr.put("smt", krsLocal.getSemester());
			tr.put("sks", Integer.valueOf(sksSmt));
			tr.put("sksk", Integer.valueOf(sksKum));
			tr.put("ips", Double.valueOf(ips));
			tr.put("ipk", Double.valueOf(ipk));
			trendData.add(tr);
		}

		StringBuffer summary = new StringBuffer();
		summary.append("<div class=\"ais-profile-grid\">");
		summary.append(ProfileUiHelper.stat("SKS Kumulatif", ProfileUiHelper.fmt(Double.valueOf(sksKumulatifAkhir)),
				"Total SKS yang sudah terkumpul sampai semester terakhir."));
		summary.append(ProfileUiHelper.stat("IPK Terkini", ProfileUiHelper.fmt(Double.valueOf(ipkTerakhir)),
				"Nilai rata-rata kumulatif semua semester yang sudah dihitung."));
		summary.append(ProfileUiHelper.stat("IPS Terakhir", ProfileUiHelper.fmt(Double.valueOf(ipsTerakhir)),
				"Nilai rata-rata semester terakhir yang tampil dalam data KRS/KHS."));
		summary.append(ProfileUiHelper.stat("Mata Kuliah", String.valueOf(detailData.size()),
				"Jumlah mata kuliah yang pernah diambil atau dikonversi."));
		summary.append(ProfileUiHelper.stat("Valid / Tidak Valid", totalValid + " / " + totalTidakValid,
				"Membantu melihat nilai yang sudah masuk perhitungan akademik."));
		summary.append(ProfileUiHelper.stat("Lulus / Belum", totalLulus + " / " + totalTidakLulus,
				"Membantu melihat capaian kelulusan mata kuliah."));
		summary.append(ProfileUiHelper.stat("Konversi", String.valueOf(totalKonversi),
				"Jumlah mata kuliah yang berasal dari proses konversi."));
		summary.append("</div>");
		container.appendChild(new Html(ProfileUiHelper.panel("Ringkasan Studi",
				"Angka penting studi ditampilkan ringkas agar mudah dibaca.",
				summary.toString())));

		container.appendChild(new Html(ProfileUiHelper.panel("Perkembangan Akademik per Semester",
				"Naik-turunnya jumlah SKS, nilai IPS, dan IPK kamu dari semester ke semester.",
				ProfileUiHelper.buildTrendBars(trendData))));

		container.appendChild(new Html(ProfileUiHelper.panel("Keseimbangan Kekuatan Akademik",
				"Seberapa seimbang capaian belajarmu: dilihat dari IPK, IPS terakhir, total SKS, dan kelengkapan nilai.",
				ProfileUiHelper.buildRadar(ipkTerakhir, ipsTerakhir, sksKumulatifAkhir, totalValid,
						totalValid + totalTidakValid))));

		Groupbox gbGrid = new Groupbox();
		gbGrid.setMold("3d");
		gbGrid.setWidth("100%");
		gbGrid.setStyle("border:none;background:#fff;border-radius:14px;box-shadow:0 8px 24px rgba(15,23,42,.06);margin-top:8px;overflow:hidden;");
		gbGrid.setParent(container);
		org.zkoss.zul.Caption capGrid = new org.zkoss.zul.Caption("Daftar Mata Kuliah");
		capGrid.setStyle("font-weight:bold;font-size:12px;color:#0f172a;padding:10px 12px;");
		gbGrid.appendChild(capGrid);
		gbGrid.appendChild(new Html("<div style=\"padding:0 12px 8px 12px;color:#64748b;font-size:11px;line-height:1.45;\">Daftar mata kuliah, SKS, nilai, dan status validitas ditampilkan agar mudah diperiksa.</div>"));

		Grid gridTrend = new Grid();
		gridTrend.setMold("paging");
		gridTrend.setPageSize(10);
		gridTrend.setWidth("100%");
		gridTrend.setStyle("border:none;");
		gridTrend.setParent(gbGrid);

		Columns colsTrend = new Columns();
		colsTrend.setParent(gridTrend);
		MyColumnConfig colNama = new MyColumnConfig("Mata Kuliah");
		colNama.setWidth("48%");
		colNama.setParent(colsTrend);
		MyColumnConfig colSmtSks = new MyColumnConfig("Smt/SKS");
		colSmtSks.setWidth("17%");
		colSmtSks.setParent(colsTrend);
		MyColumnConfig colNilai = new MyColumnConfig("Nilai");
		colNilai.setWidth("18%");
		colNilai.setParent(colsTrend);
		MyColumnConfig colStatus = new MyColumnConfig("Status");
		colStatus.setWidth("17%");
		colStatus.setParent(colsTrend);

		Rows rowsTrend = new Rows();
		rowsTrend.setParent(gridTrend);
		for (Map<String, Object> dt : detailData) {
			Row rowTr = new MyRowStyled();
			rowTr.setStyle("border-bottom:1px solid #e5e7eb;");
			rowTr.setParent(rowsTrend);
			String kode = ProfileUiHelper.text(dt.get("kode"));
			String nama = ProfileUiHelper.text(dt.get("nama"));
			String valid = ProfileUiHelper.text(dt.get("valid"));
			String warnaStatus = "Valid".equals(valid) ? "#059669" : "#dc2626";
			StringBuffer mk = new StringBuffer();
			mk.append("<div style=\"line-height:1.35;\">");
			mk.append("<div style=\"font-weight:700;color:#0f172a;font-size:11px;\">").append(ProfileUiHelper.esc(nama)).append("</div>");
			if (kode.length() > 0) {
				mk.append("<div style=\"display:inline-block;margin-top:3px;padding:1px 6px;border-radius:999px;background:#f1f5f9;color:#475569;font-size:9px;font-weight:700;\">")
						.append(ProfileUiHelper.esc(kode)).append("</div>");
			}
			mk.append("</div>");
			new Html(mk.toString()).setParent(rowTr);
			new Html("<div style=\"font-size:10.5px;line-height:1.45;color:#334155;\"><b>Smt</b> "
					+ ProfileUiHelper.esc(dt.get("smt")) + "<br/><b>SKS</b> " + ProfileUiHelper.esc(dt.get("sks"))
					+ "</div>").setParent(rowTr);
			String nilai = ProfileUiHelper.fmt(dt.get("nilai_angka")) + "<br/>" + ProfileUiHelper.fmt(dt.get("nilai_ip"))
					+ " / " + ProfileUiHelper.esc(dt.get("huruf"));
			new Html("<div style=\"font-size:10.5px;line-height:1.45;color:#0f172a;font-weight:600;\">" + nilai + "</div>").setParent(rowTr);
			String alasan = ProfileUiHelper.text(dt.get("alasan"));
			StringBuffer stat = new StringBuffer();
			stat.append("<span style=\"display:inline-block;padding:3px 7px;border-radius:999px;background:")
					.append("Valid".equals(valid) ? "#ecfdf5" : "#fef2f2").append(";color:").append(warnaStatus)
					.append(";font-size:10px;font-weight:800;\">").append(ProfileUiHelper.esc(valid)).append("</span>");
			if (!"Valid".equals(valid) && alasan.length() > 0) {
				stat.append("<div title=\"").append(ProfileUiHelper.esc(alasan))
						.append("\" style=\"margin-top:4px;font-size:9.5px;line-height:1.35;color:#b91c1c;")
						.append("background:#fef2f2;border:1px solid #fecaca;border-radius:6px;padding:4px 6px;\">")
						.append("<b>Alasan:</b> ").append(ProfileUiHelper.esc(alasan)).append("</div>");
			}
			new Html(stat.toString()).setParent(rowTr);
		}

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("background:transparent;border:none;padding:10px 0 0 0;");
		toolbar.setParent(container);

		MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Refresh Ringkasan Studi", "/img/Button-Refresh-icon.png");
		btnRefresh.setAttribute("janganDisabled", true);
		btnRefresh.setStyle("font-weight:bold;color:#1d4ed8;background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:6px 12px;");
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ProfileUiHelper.showLoading(parent, "Memuat ulang Ringkasan Studi",
						"Sistem sedang mengambil ulang nilai, SKS, dan riwayat semester.");
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						initDashboard(parent, mahasiswa, dataLoader, true);
						if (dataLoader != null) {
							dataLoader.loadData(event);
						}
					}
				});
			}
		});
		btnRefresh.setParent(toolbar);
	}



	/**
	 * Membersihkan string catatan dari nilai null, whitespace, dan string literal "null".
	 *
	 * <p><b>Tujuan:</b> Data catatan KRS/KHS di database kadang berisi string {@code "null"}
	 * (bukan null Java) atau string kosong yang perlu diabaikan agar tidak ditampilkan
	 * sebagai teks pada halaman profil.</p>
	 *
	 * <p><b>Cara kerja:</b> Jika nilai null → ""; trim; jika setelah trim kosong atau
	 * sama dengan "null" (case-insensitive) → ""; sinon kembalikan nilai yang sudah di-trim.</p>
	 *
	 * @param value string catatan; boleh null
	 * @return string bersih siap ditampilkan, atau {@code ""} jika tidak ada konten bermakna
	 */
	private String bersihkanCatatan(String value) {
		if (value == null) {
			return "";
		}
		String s = value.trim();
		if (s.length() == 0 || "null".equalsIgnoreCase(s)) {
			return "";
		}
		return s;
	}

	/**
	 * Membangun HTML kartu "Masa Studi" dari string mentah masa studi mahasiswa.
	 *
	 * <p><b>Tujuan:</b> Menyajikan informasi masa studi mahasiswa dalam bentuk kartu
	 * dua stat (Sudah Ditempuh dan Batas Studi) + badge status. Digunakan untuk
	 * membantu mahasiswa memantau sisa waktu studi sebelum habis masa berlaku program.</p>
	 *
	 * <p><b>Format input {@code raw}:</b> String dari {@code mahasiswa.ambilMasaStudi()}
	 * yang dapat mengandung teks "batas waktu studi" sebagai pembatas antara waktu
	 * yang sudah ditempuh dan batas maksimal. Contoh:
	 * {@code "4 tahun 2 bulan, batas waktu studi 8 tahun"}</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mencari indeks "batas waktu studi" (case-insensitive) dalam string.</li>
	 *   <li>Jika ditemukan: memisah string menjadi {@code masa} (sebelum) dan
	 *       {@code batas} (sesudah).</li>
	 *   <li>Membersihkan prefix "Masa studi:" dari {@code masa}.</li>
	 *   <li>Menentukan badge dan teks status berdasarkan ada/tidaknya batas waktu.</li>
	 *   <li>Membangun HTML dua kartu stat via {@link ProfileUiHelper#stat} dan
	 *       badge status, dibungkus dalam {@link ProfileUiHelper#panel}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param raw string masa studi mentah dari entitas mahasiswa; boleh null/kosong
	 * @return string HTML kartu "Masa Studi" yang siap dirender sebagai {@code Html} ZK
	 */
	private String buatInfoMasaStudi(String raw) {
		String info = raw == null ? "" : raw.trim();
		String masa = info;
		String batas = "";
		String status = "Perjalanan studi masih berjalan.";
		String badge = "Masa Studi";
		String lower = info.toLowerCase();
		int idx = lower.indexOf("batas waktu studi");
		if (idx >= 0) {
			masa = info.substring(0, idx).trim();
			batas = info.substring(idx).trim();
		}
		masa = masa.replace("Masa studi", "").replace("masa studi", "").replace(":", "").trim();
		if (masa.length() == 0) {
			masa = "Belum ada keterangan masa studi.";
		}
		if (batas.length() == 0 && info.length() > 0 && !info.equals(masa)) {
			batas = info;
		}
		if (lower.contains("batas waktu studi")) {
			status = "Gunakan informasi ini untuk memantau sisa waktu studi.";
			badge = "Perlu Diperhatikan";
		}

		StringBuffer body = new StringBuffer();
		body.append("<div class=\"ais-profile-grid\">");
		body.append(ProfileUiHelper.stat("Sudah Ditempuh", masa,
				"Waktu belajar yang sudah berjalan sampai hari ini."));
		body.append(ProfileUiHelper.stat("Batas Studi", batas.length() == 0 ? "-" : batas,
				"Batas waktu studi yang berlaku pada program ini."));
		body.append("</div>");
		body.append("<div style=\"padding:0 12px 12px 12px;\"><span class=\"ais-profile-badge\">");
		body.append(ProfileUiHelper.esc(badge));
		body.append("</span><div style=\"margin-top:8px;font-size:11px;color:#475569;line-height:1.55;\">");
		body.append(ProfileUiHelper.esc(status));
		body.append("</div></div>");

		return ProfileUiHelper.panel("Masa Studi",
				"Lama studi dan batas waktu studi ditampilkan agar lebih mudah dipantau.", body.toString());
	}

}
