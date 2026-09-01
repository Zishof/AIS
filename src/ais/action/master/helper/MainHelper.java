package ais.action.master.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.maintenance.TbmuserAction;
import ais.action.master.BiodataDosenAction;
import ais.action.master.BiodataMahasiswaAction;
import ais.action.master.BiodataPegawaiAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.action.master.sekolah.GuruAction;
import ais.action.master.sekolah.SiswaAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.AngketUtil;
import ais.common.BarcodeCommon;
import ais.common.ChecklistPenilaianHelper;
import ais.common.ChecklistPenilaianGuruHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.ConstantValues;
import ais.common.PustakaUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailLogLogin;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Menu;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Orkestrator utama shell aplikasi AIS setelah login — <b>BUKAN</b> helper kecil berdiri sendiri,
 * melainkan class yang dipanggil langsung dari titik bootstrap layar utama
 * ({@code ais.action.maintenance.MainAction}/{@code MainAction2}, controller ZK di belakang
 * {@code main.zul}) dan dipakai bersama oleh puluhan class lain lintas modul (dikonfirmasi lewat
 * pencarian referensi {@code grep -rl "MainHelper" --include=*.java}: ±32 file, mencakup
 * {@code ais.common.Common}, {@code ais.common.CommonMenu}, {@code ais.common.SessionCounter},
 * {@code ais.action.servlet.api.AngketKewajibanApi}, {@code MainMenuHelper}/{@code
 * MainTreeMenuHelper}/{@code MainBaruMenuHelper}, serta berbagai {@code *Action} yang menampilkan QR
 * pairing aplikasi mobile). Peran nyatanya ada empat kelompok terpisah, semuanya statis (class ini
 * tidak pernah di-instansiasi):
 *
 * <ol>
 * <li><b>Gerbang pasca-login ({@link #initMain}).</b> Dipanggil sekali setiap kali {@code /main}
 * dimuat. Menyiapkan locale &amp; {@code Common.ROOT}, menolak sesi "alumni" yang menyamar, lalu
 * bercabang menurut jenis akun ({@link ais.database.model.Dosen}/{@link Mahasiswa}/{@link
 * ais.database.model.sekolah.Siswa}/{@link ais.database.model.sekolah.Guru}/admin-pegawai) untuk
 * memvalidasi kelengkapan email/biodata (dikontrol per konfigurasi, mis.
 * {@code apakah_mahasiswa_harus_melengkapi_biodata_nya}) dan syarat khusus role (mis. mahasiswa
 * tidak dapat login bila {@link JenisKegiatan#apakahBoleh} menolak, atau masih ada pinjaman
 * perpustakaan yang menahan lewat {@link PustakaUtil#checkPeminjamaBuku}). Setelah lolos per-role,
 * SELALU memaksa pengecekan jadwal kuesioner ("angket") wajib lewat {@code prosesAngketSaatLogin} —
 * tiga jalur berurutan dan terisolasi try/catch masing-masing (dosen/guru dari jadwal umum, grup
 * kuesioner umum, kuesioner umum polos); begitu satu jenis angket aktif &amp; belum lengkap
 * ditemukan, modal ZUL wajib ditampilkan dan {@code initMain} mengembalikan {@code false} (akses
 * ditahan) — user diarahkan ulang ke {@code /main} setelah mengisi sehingga jenis angket berikutnya
 * yang belum lengkap ikut tertangkap pada percobaan berikutnya. Servlet API
 * {@link ais.action.servlet.api.AngketKewajibanApi} sengaja meniru ulang logika tiga jalur yang sama
 * di luar konteks web (lihat javadoc-nya) — bila urutan/ketentuan gerbang di sini berubah, servlet
 * itu harus disinkronkan manual.</li>
 * <li><b>Bantuan &amp; logout ({@link #onBantuan}, {@link #onKatalogBantuan}, {@link #onKeluar}).</b>
 * Mencatat {@link DetailLogLogin} best-effort (pola native-session yang konsisten di seluruh file:
 * transaksi kecil, rollback di catch, tiga langkah cleanup {@code disconnect()}/{@code close()}/
 * {@code HibernateUtil.closeSession()} masing-masing dibungkus try-catch terpisah), lalu membuka
 * manual PDF/tautan Google Drive spesifik role ({@code onBantuan}) atau katalog panduan halaman
 * ({@code onKatalogBantuan}, didelegasikan ke {@code BantuanHelper}). {@code onKeluar} menampilkan
 * konfirmasi keluar; bila cookie "ingat saya" terdeteksi, alur logout menyertakan penghapusan cookie
 * di sisi JavaScript ({@link #hapusCookieDiPeramban}) DAN redirect penuh ke servlet
 * {@code /clear-cookie} ({@link #goBersihkanCookie}) — bukan header {@code Set-Cookie} pada respons
 * AJAX ZK, karena terbukti tidak selalu dihormati peramban (lihat komentar historis 21-08-2026 di
 * badan kode).</li>
 * <li><b>Utilitas pohon menu bersama ({@link #hasChild}, {@link #parents}, field {@link #logins}).</b>
 * {@code hasChild}/{@code logins} dipakai identik oleh {@code MainMenuHelper}, {@code
 * MainTreeMenuHelper}, {@code MainBaruMenuHelper}, dan {@code CommonMenu} untuk membangun menu
 * bertingkat dari {@link Menu} (kunci hierarki {@code root}/{@code child}, sama seperti
 * {@link MenuTreeModel}) serta menghindari pencatatan {@link DetailLogLogin} "Login" berulang per
 * sesi (map {@code logins} di-{@code remove} oleh {@link ais.common.SessionCounter} saat sesi
 * berakhir). {@code parents} secara struktural berisi lima blok for-loop yang IDENTIK berurutan
 * (bukan lima langkah berbeda) — tampak seperti upaya menaiki beberapa level pohon menu tanpa
 * rekursi/loop eksplisit; perilaku dipertahankan apa adanya karena berada di luar cakupan
 * dokumentasi.</li>
 * <li><b>QR pairing aplikasi mobile &amp; edit biodata ({@link #onDapatkanKode}, {@link
 * #onUbahBiodata}).</b> {@code onDapatkanKode} memanggil API eksternal lewat SUBPROSES OS
 * ({@code ProcessBuilder} menjalankan {@code curl}, bukan HTTP client Java) untuk meminta kode
 * pairing, lalu menghasilkan gambar QR ({@link BarcodeCommon#generateCRCode}) dan dialog unduhan
 * Android/iOS/desktop; dipanggil dari banyak {@code *Action} biodata (mahasiswa/dosen/pegawai/siswa/
 * pengumuman) dengan {@link Tbmuser} yang berbeda-beda. {@code onUbahBiodata} adalah dispatcher
 * tunggal yang memilih window/Action edit biodata yang benar berdasarkan jenis akun
 * {@link Tbmuser} (calon mahasiswa &rarr; {@code CetakRegistrasiAction}, mahasiswa/dosen &rarr; ZUL
 * biodata dalam {@link MyWindow} modal, siswa/guru &rarr; {@code SiswaAction}/{@code GuruAction},
 * pegawai &rarr; {@code BiodataPegawaiAction}, fallback &rarr; {@code TbmuserAction}) — dipakai baik
 * dari gerbang login (paksa lengkapi biodata) maupun tombol "Ubah Biodata" manual di shell aplikasi.</li>
 * </ol>
 *
 * <p><b>Kesimpulan untuk pemelihara:</b> class ini BUKAN kandidat "helper tipis" yang aman diubah
 * sepihak — perubahan pada urutan gerbang login/angket, kontrak {@code hasChild}/{@code logins},
 * atau alur logout cookie berdampak langsung ke titik masuk aplikasi (semua role) dan ke beberapa
 * class lain yang menyalin/mereplikasi sebagian perilakunya. Method domain baru sebaiknya
 * ditambahkan sebagai method statis baru di sini (mengikuti pola yang sudah ada) daripada
 * menduplikasi logika gerbang di {@code Action} pemanggil.</p>
 */
public class MainHelper {
	/**
	 * Halaman unduhan publik untuk APK Android dan installer Windows. URL latest
	 * menjaga dialog selalu mengarah ke rilis terbaru tanpa membuka repository
	 * sumber AIS yang bersifat privat.
	 */
	private static final String PUBLIC_APPLICATION_RELEASE =
			"https://github.com/Zishof/ecampus-eschool-releases/releases/latest";

	/**
	 * Cache in-memory (map ter-sinkronisasi, dibagikan seluruh JVM/webapp — bukan per-session) yang
	 * memetakan {@code LogLogin.id} ke {@code DetailLogLogin.id} dari entri "Login" pertama sesi
	 * tersebut. Dipakai bersama oleh {@link #onBantuan}/{@link #onKatalogBantuan}/{@code
	 * MainMenuHelper}/{@code MainTreeMenuHelper} untuk menghindari pencatatan baris "Login" berulang
	 * setiap kali menu dimuat ulang dalam satu sesi login yang sama. Entrinya DIHAPUS oleh
	 * {@link ais.common.SessionCounter} saat sesi ZK berakhir (timeout/logout) — bila tidak, map ini
	 * akan terus tumbuh selama aplikasi berjalan.
	 */
	public static Map<Long, Long> logins = Collections.synchronizedMap(new HashMap<Long, Long>());

	/**
	 * Gerbang tunggal yang dijalankan setiap kali layar utama ({@code /main}) selesai dimuat untuk
	 * user yang login — lihat penjelasan lengkap alurnya di Javadoc class. Ringkas: menolak sesi
	 * alumni yang menyamar, menyiapkan locale/konteks aplikasi, memvalidasi kelengkapan data sesuai
	 * jenis akun ({@code prosesLogin*}), lalu memaksa pengisian kuesioner wajib yang masih aktif
	 * ({@code prosesAngketSaatLogin}).
	 *
	 * @param session sesi ZK aktif; dipakai untuk membaca/menulis atribut locale, flag mobile, dsb.
	 * @param execution eksekusi ZK aktif; dipakai untuk {@link Common#ROOT} (context path)
	 * @param page halaman ZK yang baru selesai di-render; dipakai untuk menyisipkan
	 *            {@link CheckForParentScript} pengaman anti-iframe-hijack
	 * @param tbmuser user yang sedang login; {@code null} langsung memicu {@link Common#goLogoff()}
	 * @return {@code true} bila semua gerbang lolos dan halaman utama boleh dipakai; {@code false}
	 *         bila akses ditahan (redirect logoff atau modal wajib sedang ditampilkan) — pemanggil
	 *         WAJIB menghentikan proses render lebih lanjut saat menerima {@code false}
	 */
	public static boolean initMain(final Session session, Execution execution, Page page, final Tbmuser tbmuser)
			throws Exception {
		if (session.getAttribute("digunakanUntukPenggunaAlumni") != null) {
			session.removeAttribute("digunakanUntukPenggunaAlumni");
			Common.goLogoff();
			return false;
		}

		appendCheckForParentScript(page);

		LogLogin login = (LogLogin) session.getAttribute("login");
		boolean mobileAndroid = login != null && login.getLinkProfile() != null
				&& login.getLinkProfile().equalsIgnoreCase("Login via mobile");

		session.setAttribute("mobileAndroid", mobileAndroid);
		session.setAttribute(org.zkoss.web.Attributes.PREFERRED_LOCALE, new java.util.Locale("in", "ID"));

		Common.ROOT = execution.getContextPath();

		if (tbmuser == null) {
			Common.goLogoff();
			return false;
		}

		session.setAttribute("current_lang", tbmuser.getBahasa() == null ? Tbmuser.INDONESIA : tbmuser.getBahasa());

		try {
			if (Common.rencanaTahunAkademiks == null || Common.rencanaTahunAkademiks.isEmpty()) {
				Common.reloadRencanaTahunAkademik(HibernateUtil.currentSession());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (tbmuser.ambilDosen() != null) {
			if (!prosesLoginDosen(tbmuser)) {
				return false;
			}
		} else if (tbmuser.getMahasiswa() != null) {
			if (!prosesLoginMahasiswa(tbmuser)) {
				return false;
			}
		} else if (tbmuser.getSiswa() != null) {
			if (!prosesLoginSiswa(tbmuser)) {
				return false;
			}
		} else if (tbmuser.ambilGuru() != null) {
			if (!prosesLoginGuru(tbmuser)) {
				return false;
			}
		} else if (isPenggunaAdminAtauPegawai(tbmuser)) {
			if (!prosesLoginAdmin(tbmuser)) {
				return false;
			}
		}

		// WAJIB — SELALU query jadwal pengisian kuesioner saat login untuk SEMUA role
		// (mahasiswa -> angket dosen, siswa -> angket guru, semua -> grup & umum). Bila ADA
		// jadwal angket AKTIF (window tanggal mulai..sampai mencakup hari ini) yang BELUM
		// lengkap, popup wajib LANGSUNG muncul & menahan akses sampai diisi. Aman bila tidak
		// ada jadwal: prosesAngketSaatLogin bersifat self-gating (tak menampilkan apa-apa).
		//
		// CATATAN: jadwal kini menjadi SATU-SATUNYA pemicu — tidak lagi bergantung pada
		// konfigurasi on/off 'angket_saat_login_diaktifkan' yang, bila ter-set TIDAK AKTIF,
		// dapat membungkam gerbang ini secara diam-diam walau jadwal sudah dibuat. Untuk
		// menonaktifkan per-periode, kosongkan/lewati window tanggal jadwal (mulai/sampai).
		if (!prosesAngketSaatLogin(tbmuser)) {
			return false;
		}

		try {
			Common.initLaguage();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:168");
			// TODO: handle exception
		}

		return true;
	}



	/**
	 * Validasi khusus akun dosen saat login: memaksa pengisian angket "checklist penilaian oleh
	 * dosen" bila dikonfigurasi aktif untuk semester ganjil/genap berjalan maupun periode semester
	 * pendek ({@link #cekAngketWajibDosen}), lalu (opsional per konfigurasi) memvalidasi kelengkapan
	 * email dan biodata dosen lewat {@link BiodataDosenAction}.
	 *
	 * @param tbmuser user login; dosen diambil dari {@code tbmuser.ambilDosen()}
	 * @return {@code true} bila dosen null/tanpa id (tidak relevan, lolos) atau semua validasi
	 *         terpenuhi; {@code false} bila salah satu gerbang menahan akses
	 */
	private static boolean prosesLoginDosen(final Tbmuser tbmuser) throws Exception {
		final ais.database.model.Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		if (dosen == null || dosen.getId() == null) {
			return true;
		}

		String tahunAkademik = Common.getCurrentTahunAkademik();
		String semesterAktif = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

		if (!cekAngketWajibDosen(tbmuser, dosen, semesterAktif, tahunAkademik)) {
			return false;
		}
		if (!cekAngketWajibDosen(tbmuser, dosen, Perkuliahan.SP, tahunAkademik)) {
			return false;
		}

		if (isKonfigurasiAktif("dosen_harus_melengkapi_email_jika_belum_diisi", Konfigurasi.AKTIF)) {
			if (!BiodataDosenAction.checkEmailDosen(dosen)) {
				return false;
			}
		}

		if (isKonfigurasiAktif("apakah_dosen_harus_melengkapi_biodata_nya", Konfigurasi.TIDAK_AKTIF)) {
			if (!BiodataDosenAction.checkBiodataDosen(dosen)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Mengecek konfigurasi {@code checklist_penilaian_oleh_dosen} (berskop fakultas/jurusan dosen)
	 * untuk satu {@code semester}/{@code tahunAkademik}; bila aktif, delegasikan penentuan
	 * lengkap/belumnya penilaian ke {@link Common#displayPenilaianAngket}. Kegagalan pengambilan
	 * konfigurasi/tampilan DITELAN (dicatat lewat {@link Common#tampilErrorJikaAdmin}) dan dianggap
	 * lolos, supaya gangguan konfigurasi tidak mengunci dosen keluar dari aplikasi.
	 *
	 * @return {@code false} hanya bila konfigurasi aktif DAN angket dosen ditampilkan sebagai
	 *         gerbang wajib; {@code true} pada semua kondisi lain
	 */
	private static boolean cekAngketWajibDosen(Tbmuser tbmuser, ais.database.model.Dosen dosen, String semester,
			String tahunAkademik) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("checklist_penilaian_oleh_dosen", tahunAkademik, semester,
					null, tbmuser == null ? null : tbmuser.ambilFakultas(), tbmuser == null ? null : tbmuser.ambilJurusan(),
					Konfigurasi.TIDAK_AKTIF);
			if (konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai())) {
				return Common.displayPenilaianAngket(dosen, semester, tahunAkademik);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return true;
	}

	/**
	 * Validasi khusus akun mahasiswa saat login. Alur: hitung semester berjalan mahasiswa
	 * ({@link Common#getSemester}), lalu {@link JenisKegiatan#apakahBoleh} memutuskan boleh/tidaknya
	 * login berdasarkan status kegiatan akademik (hasil peringatan ditampilkan dan MENAHAN akses bila
	 * tidak kosong) &rarr; validasi email (opsional per konfigurasi) &rarr;
	 * {@link Common#reloadNilaiCurrentNilai} menyegarkan cache nilai berjalan mahasiswa &rarr;
	 * {@link PustakaUtil#checkPeminjamaBuku} menahan akses bila ada pinjaman perpustakaan
	 * bermasalah &rarr; validasi biodata (opsional per konfigurasi) &rarr; validasi
	 * {@link ParameterTambahanMahasiswaListener} (field kustom biodata tambahan) &rarr; terakhir
	 * {@link AngketUtil#checkAngket(Mahasiswa, int)} (tidak menahan akses, hanya memicu tampilan
	 * angket non-wajib bila relevan).
	 *
	 * @param tbmuser user login; mahasiswa diambil dari {@code tbmuser.getMahasiswa()}
	 * @return {@code true} bila mahasiswa null/tanpa id (tidak relevan) atau semua gerbang
	 *         terpenuhi; {@code false} bila salah satu gerbang menahan akses
	 */
	private static boolean prosesLoginMahasiswa(final Tbmuser tbmuser) throws Exception {
		final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return true;
		}

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		final int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		List<String> warning = new ArrayList<String>();
		JenisKegiatan.apakahBoleh(mahasiswa, currentSmt, warning);
		if (!warning.isEmpty()) {
			StringBuilder sbWarning = new StringBuilder();
			for (String d : warning) {
				if (d == null || d.trim().isEmpty()) {
					continue;
				}
				if (sbWarning.length() > 0) {
					sbWarning.append("\n\n\n");
				}
				sbWarning.append(d);
			}
			if (sbWarning.length() > 0) {
				MyMessageboxConfig.show(sbWarning.toString(), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		if (isKonfigurasiAktif("mahasiswa_harus_melengkapi_email_jika_belum_diisi", Konfigurasi.AKTIF)) {
			if (!BiodataMahasiswaAction.checkEmailMahasiswa(mahasiswa)) {
				return false;
			}
		}

		Common.reloadNilaiCurrentNilai(mahasiswa, false);
		if (!PustakaUtil.checkPeminjamaBuku(mahasiswa)) {
			return false;
		}

		if (isKonfigurasiAktif("apakah_mahasiswa_harus_melengkapi_biodata_nya", Konfigurasi.TIDAK_AKTIF)) {
			if (!BiodataMahasiswaAction.checkBiodataMahasiswa(mahasiswa)) {
				return false;
			}
		}

		try {
			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
			if (!ParameterTambahanMahasiswaListener.validate(biodataMahasiswa, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					BiodataMahasiswaAction.displayBiodataWindow();
				}
			}, true)) {
				return false;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		AngketUtil.checkAngket(mahasiswa, currentSmt);
		return true;
	}

	/**
	 * Validasi khusus akun siswa saat login: paksa pengisian angket "penilaian guru oleh siswa" bila
	 * dikonfigurasi aktif ({@link #cekAngketGuruOlehSiswa}), lalu picu (non-menahan)
	 * {@link AngketUtil#checkAngket(Siswa)} untuk angket siswa lain yang relevan.
	 *
	 * @param tbmuser user login; siswa diambil dari {@code tbmuser.getSiswa()}
	 * @return {@code true} bila siswa null/tanpa id atau gerbang wajib terpenuhi; {@code false} bila
	 *         angket guru wajib sedang ditampilkan
	 */
	private static boolean prosesLoginSiswa(final Tbmuser tbmuser) throws Exception {
		final Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
		if (siswa == null || siswa.getId() == null) {
			return true;
		}

		if (!cekAngketGuruOlehSiswa(siswa)) {
			return false;
		}
		AngketUtil.checkAngket(siswa);
		return true;
	}

	/**
	 * Mengecek konfigurasi {@code checklist_penilaian_guru_oleh_siswa} (default TIDAK AKTIF); bila
	 * aktif, delegasikan penentuan lengkap/belumnya penilaian guru untuk semester berjalan ke
	 * {@link Common#displayPenilaianAngketGuru}. Kegagalan ditelan dan dianggap lolos, sama seperti
	 * {@link #cekAngketWajibDosen}.
	 *
	 * @return {@code false} hanya bila konfigurasi aktif DAN angket guru ditampilkan sebagai
	 *         gerbang wajib; {@code true} pada semua kondisi lain (termasuk siswa {@code null})
	 */
	private static boolean cekAngketGuruOlehSiswa(Siswa siswa) {
		if (siswa == null || siswa.getId() == null) {
			return true;
		}
		try {
			if (isKonfigurasiAktif("checklist_penilaian_guru_oleh_siswa", Konfigurasi.TIDAK_AKTIF)) {
				String semester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
				return Common.displayPenilaianAngketGuru(siswa, semester, Common.getCurrentTahunAkademik());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return true;
	}

	/**
	 * Validasi khusus akun guru saat login: bila email belum diisi/tidak valid dan konfigurasi
	 * {@code guru_harus_melengkapi_email_jika_belum_diisi} aktif, tampilkan pesan wajib yang membuka
	 * dialog ubah biodata saat ditutup ({@link #onUbahBiodata}) dan tahan akses; bila konfigurasi
	 * {@code apakah_guru_harus_melengkapi_biodata_nya} aktif, langsung buka dialog ubah biodata dan
	 * tahan akses tanpa syarat tambahan.
	 *
	 * @param tbmuser user login; guru diambil dari {@code tbmuser.ambilGuru()}
	 * @return {@code true} bila guru null/tanpa id atau kedua gerbang tidak aktif/terpenuhi;
	 *         {@code false} bila salah satu gerbang menahan akses
	 */
	private static boolean prosesLoginGuru(final Tbmuser tbmuser) throws Exception {
		Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
		if (guru == null || guru.getId() == null) {
			return true;
		}

		if (isKonfigurasiAktif("guru_harus_melengkapi_email_jika_belum_diisi", Konfigurasi.TIDAK_AKTIF)) {
			String email = tbmuser == null ? null : tbmuser.getEmail();
			if (email == null || email.trim().isEmpty() || !Common.isValidEmailAddress(email.trim())) {
				MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, alamat surel (email) wajib diisi dengan format yang benar (contoh: nama@domain.com). Langkah yang dapat dilakukan: (1) lengkapi alamat surel Anda pada kolom yang tersedia; (2) pastikan penulisan formatnya sudah benar; (3) kemudian simpan kembali.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onUbahBiodata(tbmuser, null);
							}
						});
				return false;
			}
		}

		if (isKonfigurasiAktif("apakah_guru_harus_melengkapi_biodata_nya", Konfigurasi.TIDAK_AKTIF)) {
			onUbahBiodata(tbmuser, null);
			return false;
		}
		return true;
	}

	/**
	 * User dianggap "admin/pegawai murni" bila TIDAK terhubung ke entity akademik/kesiswaan manapun
	 * (dosen, mahasiswa, siswa, guru, calon mahasiswa, calon siswa) — dipakai sebagai kondisi
	 * fallback di {@link #initMain} untuk memutuskan apakah {@link #prosesLoginAdmin} perlu
	 * dijalankan.
	 *
	 * @return {@code true} bila {@code tbmuser} tidak null dan tidak terkait entity akademik manapun
	 */
	private static boolean isPenggunaAdminAtauPegawai(Tbmuser tbmuser) {
		return tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null;
	}

	/**
	 * Validasi khusus akun admin/pegawai saat login: bila email belum diisi/tidak valid dan
	 * konfigurasi {@code admin_harus_melengkapi_email_jika_belum_diisi} aktif (default AKTIF),
	 * tampilkan pesan wajib yang membuka dialog ubah biodata saat ditutup dan tahan akses.
	 *
	 * @return {@code true} bila email valid atau konfigurasi tidak aktif; {@code false} bila
	 *         pesan wajib sedang ditampilkan
	 */
	private static boolean prosesLoginAdmin(final Tbmuser tbmuser) throws Exception {
		if (isKonfigurasiAktif("admin_harus_melengkapi_email_jika_belum_diisi", Konfigurasi.AKTIF)) {
			if (tbmuser == null || tbmuser.getEmail() == null || tbmuser.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(tbmuser.getEmail().trim())) {
				MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, alamat surel (email) wajib diisi dengan format yang benar (contoh: nama@domain.com). Langkah yang dapat dilakukan: (1) lengkapi alamat surel Anda pada kolom yang tersedia; (2) pastikan penulisan formatnya sudah benar; (3) kemudian simpan kembali.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onUbahBiodata(tbmuser, null);
							}
						});
				return false;
			}
		}
		return true;
	}

	/**
	 * Utilitas baca-cepat: mengembalikan {@code true} bila nilai {@link Konfigurasi} bernama
	 * {@code nama} sama dengan {@link Konfigurasi#AKTIF}. Kegagalan pengambilan konfigurasi ditelan
	 * dan fallback ke {@code nilaiDefault} (dibandingkan dengan {@code AKTIF}) supaya masalah
	 * transien pada tabel konfigurasi tidak ikut menahan/melonggarkan gerbang secara tidak sengaja.
	 */
	private static boolean isKonfigurasiAktif(String nama, String nilaiDefault) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(nama, nilaiDefault);
			return konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return Konfigurasi.AKTIF.equals(nilaiDefault);
		}
	}

	/**
	 * Gerbang angket saat login: query jadwal pengisian kuesioner ({@code JadwalChecklistPenilaianUmum})
	 * lalu WAJIBKAN pengisian bila ada yang aktif &amp; belum lengkap. Tiga jenis diperiksa berurutan
	 * dan masing-masing DIISOLASI dengan try/catch sendiri, sehingga kegagalan/koneksi bermasalah pada
	 * satu jenis TIDAK menggagalkan pengecekan jenis lain (mis. bila angket dosen error, angket grup &amp;
	 * umum tetap diperiksa):
	 * <ol>
	 *   <li><b>Dosen (mahasiswa) / Guru (siswa)</b> dari Jadwal Angket Umum — {@code prosesAngketDosenGuruDariJadwalUmum}.</li>
	 *   <li><b>Grup kuesioner umum</b> — {@code prosesAngketSaatLoginGrup}.</li>
	 *   <li><b>Umum</b> — {@code prosesAngketSaatLoginUmum}.</li>
	 * </ol>
	 * Masing-masing jalur me-query jadwal dengan window tanggal ({@code mulai <= hari ini <= sampai})
	 * + peruntukan role, lalu memeriksa kelengkapan; bila belum lengkap, menampilkan ZUL spesifik
	 * sebagai modal wajib dan mengembalikan {@code true} di sini (login ditahan). Hanya SATU jenis
	 * ditampilkan per login; setelah diisi &amp; redirect ke {@code /main}, {@code initMain} berjalan
	 * lagi dan jenis berikutnya yang belum lengkap muncul — memaksa penyelesaian SEMUA angket.
	 *
	 * @param tbmuser user yang login
	 * @return {@code false} bila sebuah angket wajib ditampilkan (akses ditahan); {@code true} bila
	 *         tidak ada jadwal aktif yang belum lengkap (login boleh lanjut)
	 */
	private static boolean prosesAngketSaatLogin(final Tbmuser tbmuser) {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return true;
		}

		System.out.println("[AngketLogin] cek jadwal kuesioner utk user=" + tbmuser.getUserId()
				+ ", mahasiswa=" + (tbmuser.getMahasiswa() != null) + ", siswa=" + (tbmuser.getSiswa() != null));

		// 1) Angket dosen (mahasiswa) / guru (siswa) dari Jadwal Angket Umum — diisolasi.
		try {
			if (prosesAngketDosenGuruDariJadwalUmum(tbmuser)) {
				System.out.println("[AngketLogin] -> tampil angket DOSEN/GURU (wajib) utk user=" + tbmuser.getUserId());
				return false;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// 2) Angket grup kuesioner umum — diisolasi.
		try {
			if (prosesAngketSaatLoginGrup(tbmuser)) {
				System.out.println("[AngketLogin] -> tampil angket GRUP (wajib) utk user=" + tbmuser.getUserId());
				return false;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// 3) Angket umum — diisolasi.
		try {
			if (prosesAngketSaatLoginUmum(tbmuser)) {
				System.out.println("[AngketLogin] -> tampil angket UMUM (wajib) utk user=" + tbmuser.getUserId());
				return false;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		System.out.println("[AngketLogin] tidak ada jadwal kuesioner aktif yang belum lengkap utk user="
				+ tbmuser.getUserId() + " -> login lanjut");
		return true;
	}

	/**
	 * Cabang pertama gerbang angket ({@link #prosesAngketSaatLogin}): gabungkan jadwal angket umum
	 * grup+non-grup ({@link #gabungkanJadwalAngketUmum}), lalu untuk tiap periode tahun
	 * akademik/semester cek apakah ada jadwal angket DOSEN (untuk mahasiswa) atau GURU (untuk siswa)
	 * yang masih aktif; bila ada, tampilkan gerbang wajibnya ({@link
	 * #tampilkanAngketDosenMahasiswaDariJadwalUmum} / {@link #tampilkanAngketGuruSiswaDariJadwalUmum}).
	 *
	 * @return {@code true} begitu satu gerbang angket dosen/guru ditampilkan (akses ditahan);
	 *         {@code false} bila tidak ada jadwal relevan yang aktif
	 */
	private static boolean prosesAngketDosenGuruDariJadwalUmum(final Tbmuser tbmuser) {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return false;
		}

		List<Object[]> datas = gabungkanJadwalAngketUmum(tbmuser);
		if (datas == null || datas.isEmpty()) {
			return false;
		}

		for (Object[] obj : datas) {
			final String tahunAkademik = getStringData(obj, 0);
			final String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}

			if (tbmuser.getMahasiswa() != null && ChecklistPenilaianHelper
					.adaJadwalAngketDosenDariJadwalUmum(tahunAkademik, semester, tbmuser).booleanValue()) {
				if (tampilkanAngketDosenMahasiswaDariJadwalUmum(tbmuser, tahunAkademik, semester)) {
					return true;
				}
			}

			if (tbmuser.getSiswa() != null && ChecklistPenilaianHelper
					.adaJadwalAngketGuruDariJadwalUmum(tahunAkademik, semester, tbmuser).booleanValue()) {
				if (tampilkanAngketGuruSiswaDariJadwalUmum(tbmuser, tahunAkademik, semester)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Menggabungkan jadwal angket umum grup ({@link ChecklistPenilaianHelper#getJadwalChecklistUmumGrup})
	 * dan non-grup ({@link ChecklistPenilaianHelper#getJadwalChecklistUmum}) menjadi satu daftar
	 * {@code Object[]{tahunAkademik, semester, ...}}, dengan deduplikasi berdasarkan kombinasi
	 * tahun akademik + semester (jadwal grup diprioritaskan karena ditambahkan lebih dulu).
	 *
	 * @return daftar baris jadwal unik; tidak pernah {@code null}
	 */
	private static List<Object[]> gabungkanJadwalAngketUmum(Tbmuser tbmuser) {
		List<Object[]> rows = new ArrayList<Object[]>();
		Set<String> keySet = new java.util.HashSet<String>();
		tambahJadwalAngket(rows, keySet, ChecklistPenilaianHelper.getJadwalChecklistUmumGrup(tbmuser));
		tambahJadwalAngket(rows, keySet, ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser));
		return rows;
	}

	/**
	 * Menambahkan baris dari {@code source} ke {@code rows}, melewati baris tanpa tahun
	 * akademik/semester valid dan baris yang kombinasi tahun akademik+semester-nya sudah ada di
	 * {@code keySet} (dedup helper untuk {@link #gabungkanJadwalAngketUmum}).
	 */
	private static void tambahJadwalAngket(List<Object[]> rows, Set<String> keySet, List<Object[]> source) {
		if (source == null || source.isEmpty()) {
			return;
		}
		for (Object[] obj : source) {
			String tahunAkademik = getStringData(obj, 0);
			String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}
			String key = tahunAkademik + "|" + semester;
			if (!keySet.contains(key)) {
				keySet.add(key);
				rows.add(obj);
			}
		}
	}

	/**
	 * Menampilkan angket <b>"Penilaian Dosen oleh Mahasiswa"</b> saat login bila jadwal umum yang
	 * aktif memuat Grup Angket dosen dan mahasiswa <b>belum</b> menyelesaikannya.
	 *
	 * <p><b>Pola.</b> Menyamai angket umum ({@link #tampilkanAngketSaatLogin}): bila belum lengkap,
	 * buka halaman <code>/common/checklist_penilaian_dosen_oleh_mhs.zul</code> (di-<i>apply</i> oleh
	 * {@code ChecklistPenilaianDosenOlehMhsAction} yang memuat sendiri mahasiswa dari user login,
	 * dengan grup terbuka mengikuti jadwal angket aktif) sebagai modal wajib yang menahan akses
	 * sampai diisi, lalu redirect ke <code>/main</code>.</p>
	 *
	 * <p><b>Gerbang kelengkapan.</b> Diperiksa via
	 * {@link AngketUtil#checkStatusChecklist(Mahasiswa, int, Integer)} ({@code true} = masih ada
	 * dosen/perkuliahan yang belum dinilai) pada semester berjalan mahasiswa untuk periode
	 * GANJIL/GENAP jadwal. Menggantikan pemanggilan refleksi lama yang <b>selalu gagal</b>
	 * (<code>Common.displayPenilaianAngket(Mahasiswa,...)</code> tidak pernah ada) sehingga ZUL
	 * spesifik tidak pernah tampil.</p>
	 *
	 * @param tbmuser       user yang login (harus punya mahasiswa)
	 * @param tahunAkademik tahun akademik jadwal angket aktif
	 * @param semester      periode jadwal (GANJIL/GENAP)
	 * @return {@code true} bila angket ditampilkan sebagai gerbang (akses ditahan); {@code false}
	 *         bila sudah lengkap atau tidak dapat ditentukan
	 */
	private static boolean tampilkanAngketDosenMahasiswaDariJadwalUmum(Tbmuser tbmuser, String tahunAkademik,
			String semester) {
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return false;
		}
		try {
			int currentSmt = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			if (Boolean.TRUE.equals(AngketUtil.checkStatusChecklist(mahasiswa, currentSmt, null))) {
				tampilkanAngketSaatLoginPesan("/common/checklist_penilaian_dosen_oleh_mhs.zul",
						"Penilaian angket dosen oleh mahasiswa untuk tahun akademik " + tahunAkademik
								+ " dan semester " + semester
								+ " sebagian atau semuanya belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket Dosen berikut.\n\nKlik tombol OK untuk lanjut, kemudian buka tiap grup lalu Klik tombol \"Lakukan Penilaian\".");
				return true;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	/**
	 * Menampilkan angket <b>"Penilaian Guru oleh Siswa"</b> saat login bila jadwal umum yang aktif
	 * memuat Grup Angket guru dan siswa <b>belum</b> menyelesaikannya.
	 *
	 * <p><b>Pola.</b> Menyamai angket umum/dosen: bila belum lengkap, buka halaman
	 * <code>/pages/master/sekolah/checklist_penilaian_guru_oleh_siswa.zul</code> (di-<i>apply</i>
	 * oleh {@code ChecklistPenilaianGuruOlehMhsAction} yang memuat sendiri siswa dari user login)
	 * sebagai modal wajib yang menahan akses sampai diisi, lalu redirect ke <code>/main</code>.</p>
	 *
	 * <p><b>Gerbang kelengkapan.</b> Diperiksa via
	 * {@link ChecklistPenilaianGuruHelper#checkStatusChecklistGuru(Siswa, String, String)}
	 * ({@code true} = masih ada guru/pelajaran yang belum dinilai). Sebelumnya guru ditampilkan
	 * lewat {@code Common.displayPenilaianAngketGuru} (komponen {@code AngketGuruWindow} modal); kini
	 * memakai ZUL spesifik sesuai permintaan agar konsisten dengan angket umum &amp; dosen.</p>
	 *
	 * @param tbmuser       user yang login (harus punya siswa)
	 * @param tahunAkademik tahun akademik jadwal angket aktif
	 * @param semester      periode jadwal (GANJIL/GENAP)
	 * @return {@code true} bila angket ditampilkan sebagai gerbang (akses ditahan); {@code false}
	 *         bila sudah lengkap atau tidak dapat ditentukan
	 */
	private static boolean tampilkanAngketGuruSiswaDariJadwalUmum(Tbmuser tbmuser, String tahunAkademik,
			String semester) {
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
		if (siswa == null || siswa.getId() == null) {
			return false;
		}
		try {
			if (ChecklistPenilaianGuruHelper.checkStatusChecklistGuru(siswa, semester, tahunAkademik)) {
				tampilkanAngketSaatLoginPesan("/pages/master/sekolah/checklist_penilaian_guru_oleh_siswa.zul",
						"Penilaian angket guru oleh siswa untuk tahun akademik " + tahunAkademik + " dan semester "
								+ semester
								+ " sebagian atau semuanya belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket Guru berikut.\n\nKlik tombol OK untuk lanjut, kemudian buka tiap grup lalu Klik tombol \"Lakukan Penilaian\".");
				return true;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	/**
	 * Cabang kedua gerbang angket: untuk tiap jadwal grup kuesioner umum yang aktif
	 * ({@link ChecklistPenilaianHelper#getJadwalChecklistUmumGrup}), cek kelengkapan lewat
	 * {@link ChecklistPenilaianHelper#checkStatusChecklistUmumGrup}; bila belum lengkap, tampilkan
	 * {@code /common/checklist_penilaian_umum_grup.zul} sebagai gerbang wajib.
	 *
	 * @return {@code true} begitu satu gerbang grup ditampilkan; {@code false} bila tidak ada
	 */
	private static boolean prosesAngketSaatLoginGrup(final Tbmuser tbmuser) {
		List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmumGrup(tbmuser);
		if (datas == null || datas.isEmpty()) {
			return false;
		}

		for (Object[] obj : datas) {
			final String tahunAkademik = getStringData(obj, 0);
			final String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}

			if (ChecklistPenilaianHelper.checkStatusChecklistUmumGrup(tahunAkademik, semester, tbmuser)) {
				tampilkanAngketSaatLogin("/common/checklist_penilaian_umum_grup.zul", tahunAkademik, semester);
				return true;
			}
		}

		return false;
	}

	/**
	 * Cabang ketiga (terakhir) gerbang angket: padanan {@link #prosesAngketSaatLoginGrup} untuk
	 * jadwal kuesioner umum non-grup ({@link ChecklistPenilaianHelper#getJadwalChecklistUmum} /
	 * {@link ChecklistPenilaianHelper#checkStatusChecklistUmum}); bila belum lengkap, tampilkan
	 * {@code /common/checklist_penilaian_umum.zul} sebagai gerbang wajib.
	 *
	 * @return {@code true} begitu satu gerbang umum ditampilkan; {@code false} bila tidak ada
	 */
	private static boolean prosesAngketSaatLoginUmum(final Tbmuser tbmuser) {
		List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser);
		if (datas == null || datas.isEmpty()) {
			return false;
		}

		for (Object[] obj : datas) {
			final String tahunAkademik = getStringData(obj, 0);
			final String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}

			if (ChecklistPenilaianHelper.checkStatusChecklistUmum(tahunAkademik, semester, tbmuser)) {
				tampilkanAngketSaatLogin("/common/checklist_penilaian_umum.zul", tahunAkademik, semester);
				return true;
			}
		}

		return false;
	}

	/** Ambil elemen {@code index} dari {@code data} sebagai {@link String} ter-trim, atau {@code null} bila indeks di luar batas/nilainya kosong. */
	private static String getStringData(Object[] data, int index) {
		if (data == null || data.length <= index || data[index] == null) {
			return null;
		}
		String value = data[index].toString();
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	/**
	 * Bangun pesan standar angket umum ("Penilaian angket umum ... belum Anda lakukan") lalu
	 * delegasikan ke {@link #tampilkanAngketSaatLoginPesan} untuk membuka {@code url} sebagai modal
	 * wajib.
	 */
	private static void tampilkanAngketSaatLogin(final String url, final String tahunAkademik, final String semester) {
		tampilkanAngketSaatLoginPesan(url,
				"Penilaian angket umum sebagian atau semuanya untuk tahun akademik " + tahunAkademik + " dan semester "
						+ semester
						+ " belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket umum berikut.\n\nKlik tombol OK untuk lanjut, setelah itu Klik tombol \"Lakukan Penilaian\"");
	}

	/**
	 * Rutin generik penampil angket wajib saat login: membuka {@code url} sebagai modal (95%×95%)
	 * yang menahan akses aplikasi, dengan pesan {@code pesan} yang disesuaikan per jenis angket
	 * (umum / dosen oleh mahasiswa / guru oleh siswa). Saat modal ditutup, koneksi konfirmasi-tutup
	 * dibersihkan lalu pengguna diarahkan ulang ke {@code /main} agar gerbang dievaluasi kembali.
	 *
	 * <p>Dipakai bersama oleh {@link #tampilkanAngketSaatLogin} (umum),
	 * {@link #tampilkanAngketDosenMahasiswaDariJadwalUmum} (dosen) dan
	 * {@link #tampilkanAngketGuruSiswaDariJadwalUmum} (guru) — mesin tampil identik, hanya URL ZUL
	 * &amp; teks pesan yang berbeda.</p>
	 *
	 * @param url   path ZUL yang dibuka (mis. {@code /common/checklist_penilaian_umum.zul})
	 * @param pesan teks informasi yang ditampilkan setelah modal dibuka
	 */
	private static void tampilkanAngketSaatLoginPesan(final String url, final String pesan) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow(url, true, "95%", "95%", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.confirmClose(null);
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Executions.sendRedirect("/main");
							}
						});
					}
				}, "Angket Penilaian");

				MyMessageboxConfig.show(pesan, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
							}
						});
			}
		});
	}


	/**
	 * Menyisipkan komponen {@link CheckForParentScript} (script anti clickjacking/anti-iframe-asing)
	 * sekali ke root pertama halaman yang bukan komponen {@code <style>}/{@code <script>}, dan hanya
	 * bila belum ada instance-nya ({@link #alreadyHasCheckForParentScript}) — dipanggil di awal
	 * {@link #initMain} setiap kali {@code /main} dimuat. Kegagalan (mis. halaman belum punya root)
	 * ditelan lewat {@link Common#tampilErrorJikaAdmin}, tidak pernah menggagalkan {@code initMain}.
	 */
	@SuppressWarnings("rawtypes")
	private static void appendCheckForParentScript(Page page) {
		if (page == null) {
			return;
		}

		try {
			Component root = null;

			Collection roots = page.getRoots();
			if (roots != null) {
				for (Object obj : roots) {
					if (!(obj instanceof Component)) {
						continue;
					}

					Component candidate = (Component) obj;
					if (isStyleOrScriptComponent(candidate)) {
						continue;
					}

					root = candidate;
					break;
				}
			}

			if (root == null) {
				Component firstRoot = page.getFirstRoot();
				if (firstRoot != null && !isStyleOrScriptComponent(firstRoot)) {
					root = firstRoot;
				}
			}

			if (root != null && !alreadyHasCheckForParentScript(root)) {
				root.appendChild(new CheckForParentScript());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** {@code true} bila nama class {@code component} mengandung penanda komponen {@code Style}/{@code Script} ZK — dipakai {@link #appendCheckForParentScript} agar tidak memilih root non-visual sebagai tempat sisipan. */
	private static boolean isStyleOrScriptComponent(Component component) {
		if (component == null || component.getClass() == null) {
			return false;
		}

		String className = component.getClass().getName();
		return className != null
				&& (className.indexOf(".Style") >= 0
						|| className.indexOf(".Script") >= 0
						|| className.indexOf("org.zkoss.zul.Style") >= 0
						|| className.indexOf("org.zkoss.zul.Script") >= 0);
	}

	/** {@code true} bila salah satu anak langsung {@code root} sudah berupa {@link CheckForParentScript} — mencegah {@link #appendCheckForParentScript} menyisipkan duplikat pada pemuatan {@code /main} berulang. */
	private static boolean alreadyHasCheckForParentScript(Component root) {
		if (root == null || root.getChildren() == null) {
			return false;
		}

		for (Object child : root.getChildren()) {
			if (child instanceof CheckForParentScript) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Membuka <b>manual pengguna</b> (bukan katalog — lihat {@link #onKatalogBantuan} untuk daftar
	 * panduan per halaman) yang sesuai jenis akun. Mencatat {@link DetailLogLogin} "Bantuan"
	 * best-effort (kegagalan transaksi di-rollback &amp; native session ditutup di {@code finally},
	 * TIDAK menghentikan proses), lalu:
	 * <ol>
	 * <li>Bila ada {@link LampiranLain} bernama {@code nama_usermanual_<roleId>} yang tersimpan:
	 * tampilkan lewat Google Drive viewer bila ada tautan GDrive, atau via
	 * {@link Common#displayWindow}/redirect tab baru bila berupa berkas biasa.</li>
	 * <li>Bila tidak ada: tentukan nama berkas PDF dari {@link Konfigurasi} khusus role (keuangan,
	 * mahasiswa, dosen, atau {@code nama_usermanual_<roleId>} generik), fallback ke
	 * {@code nama_usermanual} umum, lalu unduh langsung dari folder {@code /help} lewat
	 * {@link Filedownload#save}.</li>
	 * </ol>
	 *
	 * @param login sesi login aktif untuk pencatatan; {@code null} melewati pencatatan tapi tetap
	 *            membuka manual
	 */
	public static void onBantuan(LogLogin login) throws Exception {
		if (login == null) {
			return;
		}
		DetailLogLogin detailLogLogin = new DetailLogLogin();
		detailLogLogin.setKeterangan("Bantuan");
		detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
		detailLogLogin.setLogLogin(login);
		// Pencatatan DetailLogLogin best-effort: kegagalan (mis. sequence
		// detail_log_login_id_seq belum ada) WAJIB di-rollback + tutup native session
		// di finally agar tx aborted tidak meracuni operasi berikutnya di method ini.
		org.hibernate.Session session1 = null;
		try {
			session1 = HibernateUtil.currentNativeSession();
			session1.getTransaction().begin();
			session1.save(detailLogLogin);
			session1.getTransaction().commit();

			Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:780");
			try {
				if (session1 != null && session1.getTransaction() != null
						&& session1.getTransaction().isActive()) {
					session1.getTransaction().rollback();
				}
			} catch (Exception eRoll) {
				eRoll.printStackTrace(); ais.common.ErrorAuditUtil.record(eRoll, "auto-audit src/ais/action/master/helper/MainHelper.java:787");
			}
		} finally {
			try {
				if (session1 != null) {
					session1.disconnect();
				}
			} catch (Exception eDis) { ais.common.ErrorAuditUtil.record(eDis, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:794");
				// abaikan
			}
			try {
				if (session1 != null) {
					session1.close();
				}
			} catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:801");
				// abaikan
			}
			try {
				HibernateUtil.closeSession();
			} catch (Exception eCs) { ais.common.ErrorAuditUtil.record(eCs, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:806");
				// abaikan
			}
		}

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		String req = request.getRequestURL().toString();
		req = req.replaceAll("http://", "");
		req = req.split(":")[0];
		req = req.split("/")[0];

		Tbmuser tbmuser = Common.getCurrentUser();

		LampiranLain lampiranLain = LampiranLain.ambil(1L,
				"nama_usermanual_" + tbmuser.hakAkses().getRoleId().toLowerCase());
		if (lampiranLain != null && lampiranLain.getId() != null) {
			if (lampiranLain.getGdrive() != null) {
				lampiranLain.tampilGDrive(null);
			} else {

				String link = lampiranLain == null ? null
						: (lampiranLain.getLink() == null || lampiranLain.getLink().isEmpty() ? null
								: lampiranLain.getLink());

				if (lampiranLain != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
					link = lampiranLain.createLinkUri();
					if (link != null) {
						// link = link.replaceAll("download=false", "download=true");
					}
				}

				if (lampiranLain != null && link != null && !link.trim().isEmpty()) {

					if (lampiranLain.bisaPreview()) {
						Common.displayWindow(lampiranLain.merupakanGambar(), link, true, "95%", "95%", true,
								lampiranLain);
					} else {

						ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
					}
				} else {
					MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu, berkas yang Anda akses tidak dapat ditemukan pada sistem. Langkah yang dapat dilakukan: (1) pastikan berkas masih tersedia dan belum dihapus; (2) muat ulang halaman lalu coba akses kembali; (3) apabila kendala masih berlanjut, hubungi Admin atau bagian terkait untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			}
		} else {

			Konfigurasi konfigurasi = null;

			if (tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses().getRoleId().toLowerCase().contains("keu")) {
				konfigurasi = Common.getKonfigurasi("nama_usermanual_keu",
						"User_Manual__Keuagan_Sistem_Informasi_Akademik.pdf");
			} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				konfigurasi = Common.getKonfigurasi("nama_usermanual_mahasiswa",
						"Presentasi_eCampus_Modul mahasiswa.pdf");
			} else if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				konfigurasi = Common.getKonfigurasi("nama_usermanual_dosen", "Presentasi_eCampus_Modul dosen.pdf");
			} else {
				konfigurasi = Common.getKonfigurasi("nama_usermanual_" + tbmuser.hakAkses().getRoleId().toLowerCase(),
						"");
			}

			if (konfigurasi == null || konfigurasi.getNilai().trim().isEmpty()) {
				konfigurasi = Common.getKonfigurasi("nama_usermanual", "User_Manual__Sistem_Informasi_Akademik.pdf");
			}

			Filedownload.save(
					new File(Sessions.getCurrent().getWebApp().getRealPath("/help/" + konfigurasi.getNilai())),
					"application/pdf");
		}
	}

	/**
	 * Buka <b>Katalog Bantuan</b>: daftar seluruh panduan halaman yang telah
	 * disiapkan, disaring menurut hak akses pengguna (hanya panduan untuk halaman
	 * yang menu-nya dimiliki peran pengguna), lengkap dengan pencarian judul/isi.
	 * Pencatatan akses bersifat best-effort dengan pola native-session yang sama
	 * seperti {@link #onBantuan(LogLogin)} agar transaksi gagal tidak meracuni
	 * operasi berikutnya.
	 */
	public static void onKatalogBantuan(LogLogin login) throws Exception {
		if (login != null) {
			DetailLogLogin detailLogLogin = new DetailLogLogin();
			detailLogLogin.setKeterangan("Bantuan");
			detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
			detailLogLogin.setLogLogin(login);
			org.hibernate.Session session1 = null;
			try {
				session1 = HibernateUtil.currentNativeSession();
				session1.getTransaction().begin();
				session1.save(detailLogLogin);
				session1.getTransaction().commit();
				Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:903");
				try {
					if (session1 != null && session1.getTransaction() != null
							&& session1.getTransaction().isActive()) {
						session1.getTransaction().rollback();
					}
				} catch (Exception eRoll) {
					eRoll.printStackTrace(); ais.common.ErrorAuditUtil.record(eRoll, "auto-audit src/ais/action/master/helper/MainHelper.java:910");
				}
			} finally {
				try {
					if (session1 != null) {
						session1.disconnect();
					}
				} catch (Exception eDis) { ais.common.ErrorAuditUtil.record(eDis, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:917");
					// abaikan
				}
				try {
					if (session1 != null) {
						session1.close();
					}
				} catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:924");
					// abaikan
				}
				try {
					HibernateUtil.closeSession();
				} catch (Exception eCs) { ais.common.ErrorAuditUtil.record(eCs, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:929");
					// abaikan
				}
			}
		}

		BantuanHelper.tampilkanKatalog(null);
	}

	/**
	 * Mulai alur keluar aplikasi: catat {@link DetailLogLogin} "Keluar Aplikasi"
	 * ({@link #simpanLogKeluar}), lalu bila cookie "ingat saya" terdeteksi
	 * ({@link #getRememberedUsername}) tampilkan dialog konfirmasi dengan opsi tambahan "Lupakan akun
	 * saya" ({@link #tampilkanKonfirmasiKeluarDenganRememberMe}); bila tidak, tampilkan konfirmasi
	 * OK/Batal standar yang langsung memanggil {@link #logoutSekarang} bila disetujui.
	 *
	 * @param login sesi login aktif yang akan dicatat keluar
	 */
	public static void onKeluar(LogLogin login) throws Exception {
		simpanLogKeluar(login);

		String username = getRememberedUsername();
		if (username != null && !username.trim().isEmpty()) {
			tampilkanKonfirmasiKeluarDenganRememberMe();
			return;
		}

		MyMessageboxConfig.show(
		"Apakah Bapak/Ibu yakin ingin keluar dari aplikasi? Mohon pastikan seluruh pekerjaan telah tersimpan sebelum keluar. Silakan pilih OK untuk keluar atau Batal untuk tetap berada di dalam aplikasi.",
		"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
		new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Object data = event.getData();
						if (data != null) {
							int i = Integer.parseInt(data.toString());
							if (i == MyMessageboxConfig.CANCEL) {
								return;
							}
						}
						logoutSekarang();
					}
				});
	}

	/**
	 * Menyimpan satu baris {@link DetailLogLogin} berketerangan "Keluar Aplikasi" lewat transaksi
	 * native-session mandiri (rollback pada gagal, cleanup {@code disconnect}/{@code close}/
	 * {@code HibernateUtil.closeSession()} di {@code finally}) — pola pencatatan yang sama dipakai di
	 * seluruh file ini untuk event "Login"/"Bantuan"/"Keluar Aplikasi".
	 */
	private static void simpanLogKeluar(LogLogin login) {
		DetailLogLogin detailLogLogin = new DetailLogLogin();
		detailLogLogin.setKeterangan("Keluar Aplikasi");
		detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
		detailLogLogin.setLogLogin(login);

		org.hibernate.Session session1 = null;
		org.hibernate.Transaction tx = null;
		try {
			session1 = HibernateUtil.currentNativeSession();
			tx = session1.getTransaction();
			tx.begin();
			session1.save(detailLogLogin);
			tx.commit();
			Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/MainHelper.java:985");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:988");
		} finally {
			if (session1 != null) {
				try {
					if (session1.isOpen()) {
						session1.disconnect();
						session1.close();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:997");
				}
			}
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1003");
			}
		}
	}

	/**
	 * Baca cookie {@code userinfo} (ditulis {@code login2.jsp} untuk fitur "ingat saya") dari
	 * request HTTP asli, ter-decode URL. Selalu {@code null} bila
	 * {@link ConstantValues#aktifkanRememeberMe} nonaktif secara global.
	 *
	 * @return username tersimpan, atau {@code null} bila fitur nonaktif/cookie tidak ada/gagal baca
	 */
	private static String getRememberedUsername() {
		if (!ConstantValues.aktifkanRememeberMe) {
			return null;
		}
		try {
			HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
			Cookie[] cookies = request == null ? null : request.getCookies();
			if (cookies == null) {
				return null;
			}
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie != null && "userinfo".equals(cookie.getName())) {
					return java.net.URLDecoder.decode(cookie.getValue(), "UTF-8");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1025");
		}
		return null;
	}

	/**
	 * Dialog konfirmasi keluar khusus saat cookie "ingat saya" aktif: menambahkan checkbox
	 * "Lupakan akun saya di browser ini" di samping tombol Keluar/Batal. Bila dicentang saat
	 * "Keluar" ditekan, alur menghapus cookie di sisi JS ({@link #hapusCookieDiPeramban}) dan sisi
	 * server ({@link #hapusCookieRememberMe}) lalu redirect penuh ke {@code /clear-cookie}
	 * ({@link #goBersihkanCookie}) — bukan pola logout biasa — karena header {@code Set-Cookie} pada
	 * respons AJAX ZK terbukti tidak selalu dihormati peramban (lihat catatan historis di badan
	 * method pemanggil tombol "Keluar"). Bila tidak dicentang, langsung {@link #logoutSekarang}.
	 */
	private static void tampilkanKonfirmasiKeluarDenganRememberMe() throws Exception {
		final MyWindow myWindow = new MyWindow("Pertanyaan", "none", true);
		myWindow.setHeight("215px");
		myWindow.setWidth("470px");
		myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Row row = Common.tampilanScroll1(myWindow);
		row.appendChild(new MyLabelBoldAja("Apakah ingin keluar aplikasi ?"));

		MyFormRow row1 = new MyFormRow();
		row1.setParent(row.getParent());

		final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Lupakan akun saya di browser ini");
		row1.appendChild(checkboxConfig);

		MyFormRow row2 = new MyFormRow();
		row2.setAlign("center");
		row2.setHeight("44px");
		row2.setParent(row.getParent());

		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:8px 10px;overflow:visible;text-align:center;");
		toolbar.setParent(row2);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				myWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Keluar", "/img/Apps-session-logout-icon.png");
		save.setTooltiptext("Keluar");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final boolean lupakanAkun = checkboxConfig.isChecked();
				try {
					myWindow.detach();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:1075");
				}
				/* FIX 21-08-2026 -- CENTANG "LUPAKAN AKUN" TIDAK BERPENGARUH.
				 *
				 * Sebelumnya centang ini memanggil hapusCookieRememberMe(), yang menaruh header
				 * Set-Cookie pada respons AU (AJAX) milik ZK. Header itu tidak dapat diandalkan:
				 * kepindahan halaman TIDAK terjadi pada respons yang sama, melainkan lewat
				 * Executions.sendRedirect("/logoff") yang dijadwalkan timer pada permintaan AU
				 * BERIKUTNYA. Akibatnya cookie "userinfo" selamat, dan login2.jsp tetap
				 * mengenali browser lalu masuk otomatis.
				 *
				 * Kini alurnya disamakan dengan dialog keluar versi JSP: diarahkan ke
				 * /clear-cookie, sebuah permintaan HTTP penuh yang header Set-Cookie-nya pasti
				 * dihormati peramban, dan servlet itu sendiri yang meneruskan ke /logoff. */
				if (lupakanAkun) {
					hapusCookieRememberMe();
					hapusCookieDiPeramban();
					goBersihkanCookie();
				} else {
					logoutSekarang();
				}
			}
		});
		save.setParent(toolbar);

		myWindow.onModal();
	}

	/**
	 * Menghapus (via {@link #expireCookie}, {@code Set-Cookie} pada respons servlet) seluruh cookie
	 * "ingat saya" ({@link #isCookieRememberMe}) yang ditemukan pada request, baik di path root
	 * {@code "/"} maupun context path aplikasi bila berbeda. Sisi server saja TIDAK CUKUP diandalkan
	 * pada respons AJAX ZK — lihat {@link #hapusCookieDiPeramban} untuk penghapusan sisi JavaScript
	 * yang dijalankan berdampingan.
	 */
	private static void hapusCookieRememberMe() {
		try {
			HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
			HttpServletResponse response = (HttpServletResponse) Executions.getCurrent().getNativeResponse();
			if (request == null || response == null) {
				return;
			}
			Cookie[] cookies = request.getCookies();
			if (cookies == null) {
				return;
			}
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie != null && isCookieRememberMe(cookie.getName())) {
					expireCookie(response, cookie.getName(), "/");
					String contextPath = request.getContextPath();
					if (contextPath != null && contextPath.trim().length() > 0 && !"/".equals(contextPath)) {
						expireCookie(response, cookie.getName(), contextPath);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1107");
		}
	}

	/** {@code true} bila {@code name} (case-insensitive) adalah salah satu nama cookie "ingat saya" ({@code userinfo}, {@code userid}, {@code rememberme}, {@code remember_me}). */
	private static boolean isCookieRememberMe(String name) {
		if (name == null) {
			return false;
		}
		String n = name.trim().toLowerCase();
		// "userid" ikut ditulis login2.jsp berdampingan dengan "userinfo"; tanpa ini sisa
		// nama pengguna masih tertinggal di peramban meski sesi sudah dilupakan.
		return "userinfo".equals(n) || "userid".equals(n) || "rememberme".equals(n)
				|| "remember_me".equals(n);
	}

	/** Menulis cookie {@code name} kosong ber-{@code maxAge=0} pada {@code path} tertentu ke {@code response} — teknik standar servlet untuk menghapus cookie di peramban. */
	private static void expireCookie(HttpServletResponse response, String name, String path) {
		try {
			Cookie cookie = new Cookie(name, "");
			cookie.setMaxAge(0);
			cookie.setPath(path == null || path.trim().length() == 0 ? "/" : path);
			response.addCookie(cookie);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1126");
		}
	}

	/**
	 * Keluar aplikasi SEKALIGUS melupakan akun: arahkan peramban ke {@code /clear-cookie}.
	 *
	 * <p>Servlet tersebut menghapus seluruh cookie, meng-invalidate sesi, lalu meneruskan ke
	 * {@code /logoff}. Pengalihan sengaja dilakukan sebagai permintaan HTTP penuh -- bukan
	 * menaruh Set-Cookie pada respons AU ZK -- agar peramban benar-benar menghapus cookie
	 * "userinfo" yang dipakai login2.jsp untuk mengenali pengguna.</p>
	 */
	/**
	 * Hapus cookie ingat-akun LANGSUNG DI PERAMBAN lewat JavaScript.
	 *
	 * <p><b>Kenapa perlu, padahal sudah ada penghapusan di sisi server.</b> Cookie
	 * {@code userinfo} dan {@code userid} DIBUAT oleh JavaScript di login2.jsp
	 * ({@code document.cookie = 'userinfo=...; path=/'}). Menghapusnya lewat JavaScript dengan
	 * path yang sama karena itu simetris dan pasti berhasil, tanpa bergantung pada apakah
	 * header {@code Set-Cookie} sempat terkirim dan dihormati -- hal yang tidak dapat
	 * diandalkan pada respons AU (AJAX) milik ZK.</p>
	 *
	 * <p>Dijalankan lebih dulu, sebelum pengalihan ke {@code /clear-cookie}, sehingga cookie
	 * sudah lenyap bahkan bila pengalihan itu gagal. Path context juga disertakan karena
	 * cookie lama mungkin pernah ditulis dengan path tersebut.</p>
	 */
	private static void hapusCookieDiPeramban() {
		try {
			String contextPath = "";
			try {
				HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
				if (request != null && request.getContextPath() != null) {
					contextPath = request.getContextPath().trim();
				}
			} catch (Exception e) {
				contextPath = "";
			}
			String[] nama = { "userinfo", "userid", "rememberme", "remember_me" };
			StringBuilder js = new StringBuilder();
			js.append("(function(){var kadaluarsa='=; expires=Thu, 01 Jan 1970 00:00:00 GMT';");
			js.append("var jalur=['/'");
			if (contextPath.length() > 0 && !"/".equals(contextPath)) {
				js.append(",'").append(contextPath).append("'");
			}
			js.append("];");
			js.append("var nama=[");
			for (int i = 0; i < nama.length; i++) {
				if (i > 0) {
					js.append(",");
				}
				js.append("'").append(nama[i]).append("'");
			}
			js.append("];");
			js.append("for(var i=0;i<nama.length;i++){for(var j=0;j<jalur.length;j++){");
			js.append("document.cookie=nama[i]+kadaluarsa+'; path='+jalur[j];}}");
			js.append("})();");
			org.zkoss.zk.ui.util.Clients.evalJavaScript(js.toString());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "MainHelper.hapusCookieDiPeramban");
		}
	}

	/**
	 * Redirect penuh (bukan AJAX) ke servlet {@code /clear-cookie}, yang menghapus seluruh cookie
	 * sisi server, meng-invalidate sesi, lalu meneruskan ke {@code /logoff}. Dipilih ketimbang
	 * {@link #logoutSekarang} biasa KHUSUS saat "Lupakan akun saya" dicentang, karena permintaan HTTP
	 * penuh menjamin header {@code Set-Cookie} dihormati peramban (berbeda dari respons AJAX ZK).
	 * Membersihkan atribut sesi {@code usersTemp} lebih dulu; bila timer redirect gagal dijadwalkan,
	 * fallback ke {@link #logoutSekarang}.
	 */
	private static void goBersihkanCookie() {
		try {
			try {
				Executions.getCurrent().getSession().removeAttribute("usersTemp");
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "MainHelper.goBersihkanCookie:usersTemp");
			}
			if (ExecutionsCtrl.getCurrent() == null) {
				return;
			}
			org.zkoss.zk.ui.util.Clients.confirmClose(null);
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (ExecutionsCtrl.getCurrent() != null) {
						ExecutionsCtrl.getCurrent().sendRedirect("/clear-cookie");
					}
				}
			});
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "MainHelper.goBersihkanCookie");
			logoutSekarang();
		}
	}

	/** Jalur logout "biasa" (tanpa lupakan cookie): bersihkan atribut sesi {@code usersTemp} lalu delegasikan ke {@link Common#goLogoff()}. */
	private static void logoutSekarang() {
		try {
			Executions.getCurrent().getSession().removeAttribute("usersTemp");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MainHelper.java:1133");
		}
		Common.goLogoff();
	}

	/**
	 * Menentukan apakah {@code root} punya minimal satu anak di {@code menus} — dipakai luas oleh
	 * {@code MainMenuHelper}, {@code MainTreeMenuHelper}, {@code MainBaruMenuHelper} untuk memutuskan
	 * apakah satu {@link Menu} harus dirender sebagai folder yang dapat dibuka (submenu) atau sebagai
	 * item daun yang langsung membuka halaman. Sama seperti {@link MenuTreeModel}, hierarki dikunci
	 * lewat pasangan kolom {@code root}/{@code child} pada {@link Menu}, BUKAN {@code id}/parentId.
	 *
	 * @param root nilai {@code child} milik menu yang diperiksa (menjadi {@code root} anak-anaknya)
	 * @param menus daftar seluruh {@link Menu} yang tersedia (role/session-scoped) untuk dicari
	 * @return {@code true} bila ada minimal satu {@link Menu} di {@code menus} dengan
	 *         {@code getRoot().equals(root)}
	 */
	public static Boolean hasChild(Long root, List<Menu> menus) {
		for (Menu menu : menus) {
			if (menu.getRoot().equals(root)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Menelusuri rantai induk (chain of {@code root}/{@code child}) dari {@code child} ke atas,
	 * dipakai {@code ais.common.CommonMenu} untuk menandai/expand jalur menu aktif di tree. Diawali
	 * dengan {@code menuD.getId() -> child}, lalu mencoba mengikuti relasi induk berulang.
	 *
	 * <p><b>Kuirk struktural:</b> badan method berisi LIMA blok {@code for} yang identik persis
	 * berturut-turut (bukan lima langkah logika berbeda) — tampaknya dimaksudkan sebagai
	 * "naik beberapa level tanpa rekursi/while eksplisit", membatasi kedalaman penelusuran ke
	 * maksimum sekitar 5 tingkat. Perilaku asli dipertahankan apa adanya (di luar cakupan
	 * dokumentasi untuk diperbaiki di sini).</p>
	 *
	 * @param child nilai {@code child} node awal (biasanya menu yang sedang aktif/dipilih)
	 * @param menuD {@link Menu} pemilik {@code child} awal, dipetakan sebagai entri pertama hasil
	 * @param menus seluruh {@link Menu} yang tersedia untuk pencarian induk
	 * @return map {@code Menu.id -> root induknya} untuk setiap node yang berhasil ditelusuri
	 *         (termasuk entri awal {@code menuD.getId() -> child})
	 */
	public static HashMap<Long, Long> parents(Long child, Menu menuD, Collection<Menu> menus) {

		HashMap<Long, Long> parents = new HashMap<Long, Long>();
		parents.put(menuD.getId(), child);
		for (Menu menu : menus) {
			if (menu.getChild().equals(child)) {
				parents.put(menu.getId(), menu.getRoot());
			}
		}

		for (Menu menu : menus) {
			if (parents.values().contains(menu.getChild())) {
				parents.put(menu.getId(), menu.getRoot());
			}
		}

		for (Menu menu : menus) {
			if (parents.values().contains(menu.getChild())) {
				parents.put(menu.getId(), menu.getRoot());
			}
		}

		for (Menu menu : menus) {
			if (parents.values().contains(menu.getChild())) {
				parents.put(menu.getId(), menu.getRoot());
			}
		}

		for (Menu menu : menus) {
			if (parents.values().contains(menu.getChild())) {
				parents.put(menu.getId(), menu.getRoot());
			}
		}

		return parents;
	}

	/** Overload praktis: memakai {@link Common#getCurrentUser()} sebagai {@link Tbmuser}, lihat {@link #onDapatkanKode(Tbmuser, Component, boolean)}. */
	public static void onDapatkanKode(Component window, boolean tampilSelesai) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		onDapatkanKode(tbmuser, window, tampilSelesai);
	}

	/**
	 * Menampilkan dialog <b>pairing aplikasi mobile/desktop</b> berisi kode/QR sekali-pakai untuk
	 * login otomatis di APK Android, iOS, atau installer desktop. Dijadwalkan lewat
	 * {@link Common#createDefaultTimer} (bukan langsung), lalu di dalam callback timer:
	 * <ol>
	 * <li>Cek {@code window.getPage() == null} — bila window sudah ditutup pengguna sebelum timer
	 * fire, batalkan (mencegah NPE {@code addMoved}/{@code setParent}).</li>
	 * <li>Kumpulkan branding (nama, motto, alamat, telepon, email, logo/banner/background) dari
	 * {@link PerguruanTinggi} ATAU {@link Sekolah}/{@link Yayasan} milik user (skema
	 * multi-tenant: sekolah &gt; yayasan &gt; default kampus).</li>
	 * <li>Panggil API eksternal ({@code Konfigurasi.ambil_kode_url}, default
	 * {@code https://dev.ecampus.id/ecampus/Api}) via <b>SUBPROSES OS</b> (
	 * {@code new ProcessBuilder("curl", "-d", jsonPayload, ...)}, BUKAN HTTP client Java) untuk
	 * meminta kode pairing; payload JSON memuat username (userId + host), link callback {@code /Api},
	 * dan seluruh branding di atas.</li>
	 * <li>Bila respons bukan JSON valid, batalkan diam-diam (log ke stderr).</li>
	 * <li>Generate gambar QR dari kode via {@link BarcodeCommon#generateCRCode} ke folder laporan,
	 * lalu render dialog berisi tautan unduhan Android/iOS (dari {@link Konfigurasi}, dengan varian
	 * khusus sekolah) DAN tautan {@link #PUBLIC_APPLICATION_RELEASE} untuk APK/installer desktop
	 * terbaru.</li>
	 * </ol>
	 *
	 * @param tbmuser user yang meminta kode; menentukan branding (sekolah/yayasan/kampus) dan
	 *            username yang dikirim ke API
	 * @param window komponen tempat dialog di-attach; bila instance {@link org.zkoss.zul.Window}
	 *            dibungkus layout border lengkap, bila bukan (mis. panel biasa) kontennya langsung
	 *            ditempel
	 * @param tampilSelesai {@code true} untuk menambahkan tombol "Selesai" (dipakai saat dialog
	 *            berdiri sendiri sebagai window modal, bukan tab di dalam halaman lain)
	 */
	public static void onDapatkanKode(final Tbmuser tbmuser, final Component window, final boolean tampilSelesai)
			throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// FIX Error D (NPE addMoved/setParent): callback Timer (lewat CommonTimerHelper)
				// bisa akhirnya fire SETELAH window/dialog target ini sudah ditutup pengguna --
				// window sudah lepas dari halaman (getPage()==null). vbox.setParent(window) di
				// bawah akan NPE pada AbstractComponent.addMoved kalau dipaksa jalan. window==null
				// tetap flow normal (dibiarkan lanjut, lihat "window instanceof Window" di bawah).
				if (window != null && window.getPage() == null) {
					return;
				}

				Mahasiswa mahasiswa = tbmuser != null && tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa() : null;

				PerguruanTinggi perguruanTinggi = mahasiswa != null && mahasiswa.getJurusan() != null
						&& mahasiswa.getJurusan().getFakultas() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
								? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
								: PerguruanTinggiUtil.getPerguruanTinggi();

				String strURL = Common.getKonfigurasi("ambil_kode_url", "https://dev.ecampus.id/ecampus/Api")
						.getNilai();

				String username = tbmuser.getUserId() + ";" + Common.getRequestHostWithProtocol();
				String link = Common.getRequestHostWithProtocol() + "/Api";
				if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
					link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai()
							+ "/Api";
					username = tbmuser.getUserId() + ";"
							+ Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
				}

				HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();

				String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
				String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
				String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
				String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");

				String judul = perguruanTinggi == null ? "" : perguruanTinggi.getNama();
				String Motto = perguruanTinggi == null || perguruanTinggi.getMotto() == null
						|| perguruanTinggi.getMotto().trim().isEmpty() ? "eCampus Information System"
								: perguruanTinggi.getMotto();
				String Alamat1 = perguruanTinggi == null ? "" : perguruanTinggi.getAlamat1();
				String Telepon = perguruanTinggi.getTelepon();
				String Email = perguruanTinggi.getEmail();

				Siswa siswa = tbmuser != null && tbmuser.getSiswa() != null ? tbmuser.getSiswa() : null;

				Sekolah sekolah = siswa != null && siswa.getSekolah() != null ? siswa.getSekolah()
						: SekolahUtil.getSekolah(request);
				Yayasan yayasan = tbmuser != null && tbmuser.getYayasan() != null ? tbmuser.getYayasan()
						: SekolahUtil.getYayasan(request);
				if (sekolah != null && sekolah.getId() != null) {
					judul = sekolah.getNama();

					if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) {
						Motto = sekolah.getMotto();
					}

					if (sekolah.getAlamat() != null && !sekolah.getAlamat().trim().isEmpty()) {
						Alamat1 = sekolah.getAlamat();
					}

					if (sekolah.getTelp() != null && !sekolah.getTelp().trim().isEmpty()) {
						Telepon = sekolah.getTelp();
					}

					if (sekolah.getEmail() != null && !sekolah.getEmail().trim().isEmpty()) {
						Email = sekolah.getEmail();
					}

					logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
							"logo_sekolah_");

				} else if (yayasan != null && yayasan.getId() != null) {
					judul = yayasan.getNama();

					if (yayasan.getMotto() != null && !yayasan.getMotto().trim().isEmpty()) {
						Motto = yayasan.getMotto();
					}

					if (yayasan.getAlamat() != null && !yayasan.getAlamat().trim().isEmpty()) {
						Alamat1 = yayasan.getAlamat();
					}

					if (yayasan.getTelp() != null && !yayasan.getTelp().trim().isEmpty()) {
						Telepon = yayasan.getTelp();
					}

					if (yayasan.getEmail() != null && !yayasan.getEmail().trim().isEmpty()) {
						Email = yayasan.getEmail();
					}

					logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia("logo_yayasan_");
				}

				String hasil = "";
				try {

					JSONObject postData = new JSONObject();
					postData.put("username", username);
					postData.put("link", link);
					postData.put("nama_pt", judul);
					postData.put("login_bg_pt", background_login_PerguruanTinggi);
					postData.put("bg_pt", background_PerguruanTinggi);
					postData.put("logo_pt", logo_PerguruanTinggi);
					postData.put("banner_pt", banner_PerguruanTinggi);

					postData.put("motto_pt", Motto);
					postData.put("alamat_pt", Alamat1);
					postData.put("telp_pt", Telepon);
					postData.put("email_pt", Email);
					postData.put("action", "code");

					System.out.println("linkPost -> " + strURL);
					System.out.println("postData -> " + postData);

					String[] command = { "curl", "-d", postData.toString(), "-H", "Content-Type: application/json",
							strURL };

					ProcessBuilder process = new ProcessBuilder(command);
					Process p;
					p = process.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					StringBuilder builder = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
					hasil = builder.toString();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1323");
				}

				System.out.println(hasil);

				if (hasil == null || hasil.trim().isEmpty() || !hasil.trim().startsWith("{")) {
					System.err.println("[MainHelper] Respons curl bukan JSON valid: " + hasil);
					return;
				}
				JSONObject jsonObject = new JSONObject(hasil);

				String linkAndroid = Common.getKonfigurasi("default_linkAndroid",
						"https://play.google.com/store/apps/details?id=com.ecampus.zishof").getNilai();

				if (sekolah != null && sekolah.getId() != null) {
					linkAndroid = Common.getKonfigurasi("default_linkAndroid_sekolah",
							"https://play.google.com/store/apps/details?id=com.eschool.zishof").getNilai();
				}

				linkAndroid = Common.getKonfigurasi(
						"linkAndroid" + (sekolah != null && sekolah.getId() != null ? "_s_" + sekolah.getId() : ""),
						linkAndroid).getNilai();

				if (linkAndroid.endsWith("id.zishof.ecampusweb")) {
					if (sekolah != null && sekolah.getId() != null) {
						linkAndroid = "https://play.google.com/store/apps/details?id=com.eschool.zishof";
					} else {
						linkAndroid = "https://play.google.com/store/apps/details?id=com.ecampus.zishof";
					}
				}

				String linkIphone = Common
						.getKonfigurasi("default_linkIphone", "https://apps.apple.com/id/app/ecampus/id6503487876?l=id")
						.getNilai();

				if (sekolah != null && sekolah.getId() != null) {
					linkIphone = Common.getKonfigurasi("default_linkIphone_sekolah",
							"https://apps.apple.com/us/app/eschool/id6503661156?l=id").getNilai();
				}

				linkIphone = Common.getKonfigurasi(
						"linkIphone" + (sekolah != null && sekolah.getId() != null ? "_s_" + sekolah.getId() : ""),
						linkIphone).getNilai();

				Vbox vbox = new Vbox();
				Borderlayout borderlayoutencarian = new Borderlayout();
				if (window instanceof Window) {
					borderlayoutencarian.setParent(window);
					Center center = new Center();
					center.setParent(borderlayoutencarian);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Grid gridcari = new Grid();
					gridcari.setWidth("100%");
					gridcari.setWidth("100%");
					gridcari.setHeight("100%");
					gridcari.setParent(center);

					Rows rowscari = new Rows();
					rowscari.setParent(gridcari);

					MyFormRow rowcari = new MyFormRow();
					rowcari.setParent(rowscari);
					rowcari.appendChild(vbox);
				} else {
					vbox.setParent(window);
				}

				vbox.setWidth("100%");
				vbox.setHeight("100%");

				vbox.setAlign("center");
				vbox.setPack("center");

				Label aa;
				vbox.appendChild(aa = new Label(ais.common.Common.getBahasaConfig("INFO : FITUR INI SEDANG DALAM TAHAP UJI COBA")));
				aa.setStyle("font-size:12px;font-weight: bolder;color:red;");
				aa.setVisible(false);

				String _qrCode = jsonObject.optString("code", "");
				File myfilebarcode = new File(
						Common.ambilREAL_PATH_REPORT() + "/crcode_" + _qrCode + ".png");

				BarcodeCommon.generateCRCode(_qrCode, myfilebarcode);

				vbox.appendChild(aa = new Label(ais.common.Common.getBahasaConfig("SCAN QRCODE BERIKUT UNTUK MULAI MENGGUNAKAN APLIKASI :")));
				aa.setStyle("font-size:11px;font-weight: bolder;");

				vbox.appendChild(new MyLabelAgakKecil(
						"* Jika aplikasi belum ter-install di smartphone Anda, untuk android dapat di-download pada link berikut:"));
				Toolbarbutton a = new ais.ui.util.MyToolbarbuttonConfig(
						Common.getBahasa("Download versi android di Google Play"), "/img/game.png");
				a.setStyle("font-size:9px;color: blue;");
				a.setTarget("_blank");
				a.setHref(linkAndroid);
				vbox.appendChild(a);

				vbox.appendChild(
						new MyLabelAgakKecil("* Untuk versi iphone / IOS dapat di-download pada link berikut:"));
				a = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Download versi iphone di Apps Store"),
						"/img/apps.png");
				a.setStyle("font-size:9px;color: blue;");
				a.setTarget("_blank");
				a.setHref(linkIphone);
				vbox.appendChild(a);

				// Satukan akses aplikasi mobile dan desktop di dialog ini. Tombol yang
				// sebelumnya berada di header dihapus agar header tetap ringkas dan
				// pengguna hanya mempunyai satu pintu unduhan yang konsisten.
				vbox.appendChild(new MyLabelAgakKecil(
						"* Untuk APK Android UAT dan installer desktop Windows terbaru:"));
				a = new ais.ui.util.MyToolbarbuttonConfig(
						Common.getBahasa("Download aplikasi Android dan Desktop"),
						"/img/svg/desktop-light.svg");
				a.setStyle("font-size:9px;color: blue;");
				a.setTarget("_blank");
				a.setHref(PUBLIC_APPLICATION_RELEASE);
				a.setTooltiptext("Buka halaman unduhan publik eCampus dan eSchool");
				vbox.appendChild(a);

				vbox.appendChild(aa = new Label("ATAU MASUKKAN KODE INSTALL : " + _qrCode));
				aa.setStyle("font-size:16px;font-weight: bolder;");

				Image img;
				vbox.appendChild(
						img = new Image(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode.getName()));
				img.setWidth("70%");

				if (tampilSelesai && window instanceof Window) {
					South southPencarian = new South();
					ais.ui.util.ZkCompat.setFlex(southPencarian, true);
					southPencarian.setParent(borderlayoutencarian);

					Toolbar toolbarPencarian = new Toolbar();
					toolbarPencarian.setHeight("25px");
					toolbarPencarian.setParent(southPencarian);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbarPencarian);
				}
			}
		});

	}

	/**
	 * Dispatcher tunggal "Ubah Biodata": memilih window/Action edit biodata yang tepat berdasarkan
	 * jenis akun {@code tbmuser}, dicoba berurutan (percabangan pertama yang cocok yang dipakai):
	 * <ol>
	 * <li>Calon mahasiswa &rarr; {@link CetakRegistrasiAction#onEdit} (form registrasi PMB).</li>
	 * <li>Mahasiswa &rarr; buka {@code /pages/master/biodata_mahasiswa.zul} dalam {@link MyWindow}
	 * modal 99%&times;99%, dengan listener lokal {@code FotoEventListener} yang menutup window dan
	 * menyegarkan {@code foto} (bila diberikan) setelah selesai.</li>
	 * <li>Siswa &rarr; {@link SiswaAction#onAddExternal}.</li>
	 * <li>Guru aktif &rarr; {@link GuruAction#onAddExternal}.</li>
	 * <li>Dosen aktif &rarr; window modal serupa mahasiswa, memuat
	 * {@code /pages/master/biodata_dosen.zul}.</li>
	 * <li>Pegawai &rarr; {@link BiodataPegawaiAction} sebagai popup modal, dengan callback yang
	 * memperbarui {@code tbmuser.setPegawai(...)} dan atribut sesi {@code usersTemp} setelah
	 * simpan.</li>
	 * <li>Fallback (user umum tanpa entity di atas) &rarr; {@link TbmuserAction#onAddExternal}.</li>
	 * </ol>
	 * Dipanggil baik dari gerbang login ({@code prosesLogin*} saat biodata wajib dilengkapi) maupun
	 * dari tombol "Ubah Biodata" manual di shell aplikasi ({@code MainAction}/{@code MainAction2}).
	 * Sebagian besar cabang me-refresh entity dari session Hibernate aktif sebelum membuka form
	 * (mis. {@code HibernateUtil.currentSession().refresh(...)}) agar data yang diedit tidak stale.
	 *
	 * @param tbmuser user yang biodatanya akan diubah
	 * @param foto komponen {@link Image} foto profil yang akan disegarkan setelah biodata disimpan;
	 *            boleh {@code null} bila pemanggil tidak menampilkan foto (mis. dipanggil dari
	 *            gerbang login sebelum shell utama tampil)
	 */
	public static void onUbahBiodata(final Tbmuser tbmuser, final Image foto) throws Exception {

		if (tbmuser.getBiodataCalonMahasiswa() != null && tbmuser.getBiodataCalonMahasiswa().getId() != null) {
			BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
			HibernateUtil.currentSession().refresh(biodataCalonMahasiswa);
			CetakRegistrasiAction.onEdit(biodataCalonMahasiswa, new DataSearchDefault() {

				@Override
				public void onSearchDefault(Event event) {
					if (foto != null) {
						// Common.loadFoto(tbmuser, foto);
						try {
							foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1475");
						}
					}
				}
			});

		} else if (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null) {

			final MyWindow window = new MyWindow();
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("99%");
			window.setWidth("99%");

			MyInclude include = new MyInclude("/pages/master/biodata_mahasiswa.zul");
			include.setParent(window);
			include.setWidth("100%");
			include.setHeight("100%");

			/**
			 * Event listener lokal milik {@link MainHelper}. Kelas ini menangani event untuk komponen induk dan meneruskan
			 * pekerjaan domain ke method/service yang sudah tersedia.
			 *
			 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MainHelper} dan dapat mengakses state kelas
			 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
			 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
			 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
			 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
			 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
			 * renderer/listener ini.</p>
			 *
			 * @see MainHelper
			 */
			class FotoEventListener implements EventListener, Serializable {

				/**
				 * 
				 */
				private static final long serialVersionUID = -5200278841874774302L;

				@Override
				public void onEvent(Event arg0) throws Exception {
					window.detach();
					if (foto != null) {
						// Common.loadFoto(tbmuser, foto);
						foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
					}
				}

			}

			FotoEventListener fotoEventListener = new FotoEventListener();
			Sessions.getCurrent().setAttribute("fotoEventListener", fotoEventListener);

			window.onModal();
		} else if (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null) {
			Siswa siswa = tbmuser.getSiswa();
			HibernateUtil.currentSession().refresh(siswa);

			SiswaAction.onAddExternal(null, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

				}
			}, siswa);

		} else if (tbmuser.getGuru() != null && tbmuser.getGuru().getAktif() && tbmuser.getGuru().getId() != null) {
			Guru guru = tbmuser.getGuru();
			HibernateUtil.currentSession().refresh(guru);
			GuruAction.onAddExternal(null, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

				}
			}, guru, true);

		} else if (tbmuser.getDosen() != null && tbmuser.getDosen().getAktif() && tbmuser.getDosen().getId() != null) {
			final MyWindow window = new MyWindow();
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("99%");
			window.setWidth("99%");

			MyInclude include = new MyInclude("/pages/master/biodata_dosen.zul");
			include.setParent(window);
			include.setWidth("100%");
			include.setHeight("100%");

			/**
			 * Listener lokal yang menutup dialog biodata dosen dan menyegarkan foto pengguna setelah penyuntingan.
			 * Instance menangkap window, foto, dan pengguna milik alur {@link MainHelper}; gunakan hanya pada desktop
			 * yang membuatnya dan jangan dibagikan lintas session.
			 *
			 * @see MainHelper
			 */
			class FotoEventListener implements EventListener, Serializable {

				/**
				 * 
				 */
				private static final long serialVersionUID = -5200278841874774302L;

				@Override
				public void onEvent(Event arg0) throws Exception {
					window.detach();
					if (foto != null) {
						// Common.loadFoto(tbmuser, foto);
						foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
					}

				}

			}

			FotoEventListener fotoEventListener = new FotoEventListener();
			Sessions.getCurrent().setAttribute("fotoEventListener", fotoEventListener);

			window.onModal();
		} else if (tbmuser.getPegawai() != null && tbmuser.getPegawai().getId() != null) {

			final BiodataPegawaiAction biodataPegawaiAction = new BiodataPegawaiAction(tbmuser.ambilPegawai());
			biodataPegawaiAction.setCommonOnSearchdefault(new CommonOnSearchdefault() {

				@Override
				public void onSearchDefault(Event event) {
					biodataPegawaiAction.detach();
					tbmuser.setPegawai((Pegawai) event.getData());
					Sessions.getCurrent().setAttribute("usersTemp", tbmuser);
					if (foto != null) {
						// Common.loadFoto(tbmuser, foto);
						try {
							foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/MainHelper.java:1590");
						}
					}
				}
			});
			biodataPegawaiAction.setHeight("95%");
			biodataPegawaiAction.setWidth("90%");
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(biodataPegawaiAction);
			biodataPegawaiAction.setVisible(true);
			biodataPegawaiAction.onModal();

		} else if (tbmuser != null && tbmuser.getUserId() != null) {

			TbmuserAction.onAddExternal(null, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (foto != null) {
						// Common.loadFoto(tbmuser, foto);
						foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
					}

				}
			}, tbmuser);
		}

	}

}
