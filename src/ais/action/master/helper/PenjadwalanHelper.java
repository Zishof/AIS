package ais.action.master.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.RpsObeAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.generic.AmbilDataTemplatePembelajaran;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PertemuanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.DataPunyaItem;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.PerkuliahanPunyaItem;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Skripsi;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.VOPembelajaran;
import ais.database.model.Wisuda;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelEdit;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Mesin CRUD + orkestrasi UI untuk "Agenda Pertemuan" (RPS/jadwal per-minggu) sebuah perkuliahan, dan —
 * lewat parameter kembar yang berulang di hampir semua method statis di kelas ini — untuk jenis agenda
 * sejenis lain yang juga memiliki daftar {@link ais.database.model.Pertemuan}: kelompok KKN
 * ({@link ais.database.model.kkn.KelompokKkn}), kelompok PKL ({@link ais.database.model.pkl.KelompokPkl}),
 * bimbingan tugas akhir ({@link ais.database.model.MahasiswaRequestTugasAkhir}), bimbingan/sidang skripsi
 * ({@link ais.database.model.Skripsi}), bimbingan KRS ({@link ais.database.model.KrsMahasiswa}), formulir
 * kegiatan ({@link ais.database.model.FormulirKegiatan}), dan wisuda ({@link ais.database.model.Wisuda}).
 * Hampir setiap method publik menerima kedelapan tipe pemilik itu sekaligus sebagai parameter (lazimnya
 * hanya satu yang tidak {@code null}), sebagai cara sederhana memakai ulang query/aksi yang sama untuk
 * banyak domain sekaligus, tanpa interface/superclass bersama.
 *
 * <p><b>Entity/tabel utama.</b> {@link ais.database.model.Pertemuan} adalah baris "agenda" itu sendiri
 * (topik, indikator, tanggal, jam mulai/selesai, status pertemuan, dsb — lihat array {@code contents[]}
 * pada {@link #display(Perkuliahan, Component)} untuk daftar kolom yang diekspor/diimpor Excel). Untuk
 * matakuliah berkurikulum OBE, format penilaian (CPMK, komponen penilaian, rubrik, pemetaan soal UTS/UAS,
 * teknik per-CPMK) hidup di {@link ais.database.model.KurikulumPunyaMatakuliah} — lihat
 * {@link #tampilTombolAmbil} (menyalin seluruh field itu dari perkuliahan/agenda lain) dan
 * {@link #buatSatuPertemuan} (membuka editor rincian OBE lewat {@code RpsObeAction} bila kurikulum
 * memakai OBE). Lampiran/isi satu pertemuan tersebar di {@link ais.database.model.file.LampiranLain}
 * (RPS/SAP/absen manual/soal UTS-UAS, dsb, per {@code jenis}), {@link ais.database.model.file.PertemuanFileContent},
 * {@link ais.database.model.streaming.VideoPertemuan}/{@link ais.database.model.streaming.AudioPertemuan}
 * (di session Hibernate terpisah lewat {@link ais.database.hibernate.StreamingHibernateUtil}),
 * {@link ais.database.model.PertemuanPunyaUjian}, {@link ais.database.model.TugasKelompok}, dan
 * {@code ais.database.model.TugasPertemuan} (tugas individu) — semuanya anak dari satu {@code Pertemuan}.</p>
 *
 * <p><b>Alur UI (ZK).</b> {@link #display(Perkuliahan, Component)} merender toolbar aksi (buat seluruh
 * pertemuan sekaligus per interval — {@link #tampilTombolBuatPertemuan}/{@link #buatPertemuan}; tambah
 * satu pertemuan atau rincian OBE — {@link #buatSatuPertemuan}; ambil/salin agenda dari perkuliahan lain
 * — {@link #tampilTombolAmbil}; atur ulang tanggal massal — {@link #tampilTombolAturUlangWaktu}/
 * {@link #prosesTampilTombolAturUlangWaktu}; download Excel — {@link #tampilTombolDownload}; upload;
 * hapus semua — {@link #tampilTombolHapus}; hapus pertemuan tidak terpakai; refresh; filter "hanya yg
 * aktif"; toggle urut manual) di atas sebuah {@code MyGrid} yang dirender baris-per-baris oleh
 * {@link PertemuanRenderer} (satu kartu per pertemuan, lihat javadoc kelas tersebut).
 * {@link #display(Perkuliahan, DataLoader)} adalah varian jendela modal: bila kurikulum matakuliah
 * memakai OBE ({@code Kurikulum.apakahObe}), isi jendela digantikan iframe halaman terpisah
 * {@code /pages/master/rps_obe.zul}; bila tidak, ia mendelegasikan ke
 * {@link #display(Perkuliahan, Component)} di dalam borderlayout yang sama.</p>
 *
 * <p><b>Kuirk/hal non-obvious yang perlu diperhatikan pemanggil atau pemelihara berikutnya:</b></p>
 * <ul>
 *   <li>Urutan pertemuan punya dua mode: default OTOMATIS, di mana nomor "Pertemuan ke-" dihitung ulang
 *   setiap grid dimuat berdasarkan TANGGAL pertemuan (lihat {@code VOPembelajaran#reInitPertemuan}) —
 *   sehingga mengubah/menyisipkan tanggal bisa menggeser semua nomor sesudahnya (mis. UTS "tadinya
 *   pertemuan 8 jadi 6"). Tombol Naik/Turun ({@link #pindahkanUrutanPertemuan}) memaksa mode ke MANUAL
 *   secara permanen begitu dipakai, agar urutan hasil susunan manual tidak lagi ditimpa pengurutan
 *   tanggal.</li>
 *   <li>{@link #tampilTombolAmbil} ("Ambil (copy) dari agenda sebelumnya / lain") — fitur salin format
 *   penilaian/OBE dan seluruh isi pertemuan dari perkuliahan/kelompok lain — sempat dibatasi hanya untuk
 *   ADMINISTRATOR (r75196, 07-07-2026), lalu di-revert 20-08-2026 karena dosen jadi tidak bisa lagi
 *   menarik agenda semester sebelumnya sendiri; hak akses sekarang murni ditentukan kondisi di sisi
 *   pemanggil {@code display(...)}, bukan gerbang di dalam method ini. Peserta, kehadiran, dan nilai
 *   per-peserta SENGAJA tidak ikut disalin — hanya struktur agenda dan format penilaiannya.</li>
 *   <li>Tombol "Hapus pertemuan tidak terpakai (&gt;N)" pada {@link #display(Perkuliahan, Component)}
 *   sengaja menghitung dan menampilkan daftar konkret nomor pertemuan yang akan dihapus PERMANEN sebelum
 *   dialog konfirmasi (bukan sekadar angka generik), karena batas N ({@code getJumlahMaksimalPertemuan()})
 *   bisa berubah diam-diam mengikuti setting kurikulum matakuliah — lihat komentar "KE-FIX" pada badan
 *   method tersebut.</li>
 *   <li>Field {@code mahasiswas} dideklarasikan tapi baris deklarasinya dikomentari — dead code
 *   peninggalan, tidak dipakai di mana pun pada kelas ini.</li>
 *   <li>{@code copyLampiranPertemuan} punya dua overload dengan tanggung jawab berbeda: yang
 *   {@code void} tiga-parameter menyalin SATU jenis lampiran saja
 *   ({@link #copyLampiranPertemuan(Pertemuan, Pertemuan, String)}); yang dua-parameter menyalin SEMUA
 *   jenis konten pertemuan (materi, file/video/audio, ujian, tugas kelompok, tugas mandiri) sekaligus dan
 *   mengembalikan ringkasan {@link HasilSalinPertemuan} berikut daftar kendala per item
 *   ({@link #copyLampiranPertemuan(Pertemuan, Pertemuan)}) — dipakai bersama oleh
 *   {@link #tampilTombolAmbil} untuk membangun laporan hasil salin agenda yang diunduh sebagai .txt.</li>
 * </ul>
 * <p><b>Efek samping.</b> Sebagian besar method di kelas ini langsung membaca/menulis basis data lewat
 * Hibernate ({@code Session.createCriteria}, {@code Common.refreshUpdate/refreshSaveOrUpdate/refreshDelete},
 * SQL native untuk hapus massal seperti pada {@link #hapusPertemuanBesertaTugas}) sekaligus memanipulasi
 * komponen ZK secara langsung (membangun {@code Window} modal, grid, listener {@code onClick}/
 * {@code onChange}). Tidak ada lapisan service terpisah — validasi boleh-hapus, logika penjadwalan, dan
 * salin-agenda semuanya berada di kelas ini; pemanggil baru sebaiknya memakai method statis yang sudah
 * ada, bukan menduplikasi query/validasi ini di action lain.</p>
 */
public class PenjadwalanHelper {

	private Perkuliahan perkuliahan;
	private MyGrid grid;
	// private List<Mahasiswa> mahasiswas;

	private MyCheckboxConfig hanyaYangAktif;
	private MyCheckboxConfig urutkanManual;

	/**
	 * {@code RowRenderer} grid agenda: menerjemahkan satu id {@link Pertemuan} (nilai baris dari
	 * {@code ListModel} milik grid, di-resolve lewat {@code GeneralValueObject.ambilData}) menjadi satu
	 * baris kartu pertemuan pada grid yang dibangun {@link PenjadwalanHelper#display(Perkuliahan, Component)}.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link PenjadwalanHelper}
	 * induk — dependensinya ({@code perkuliahan}, {@code eventListener} untuk memberi tahu refresh ke
	 * pemanggil) diberikan lewat konstruktor.</p>
	 * <p>Per baris, {@link #render(Row, Object)} menyusun: tombol video conference
	 * ({@code DashboardTimelinePertemuan.createVideoConrefrence}), tombol absen
	 * ({@code AbsensiHelper.createTombolAbsen}), tombol agenda kalender mingguan
	 * ({@code CalendarPerkuliahanMingguIniComposer}), keterangan aktifitas + scan foto
	 * ({@code AktifitasPerkuliahanHelper.createKeteranganData}); kolom "Urutan" (nomor pertemuan, berupa
	 * {@code Intbox} yang bisa diketik langsung bila mode manual, atau label saja bila mode otomatis) plus
	 * tombol Naik/Turun yang memanggil {@link PenjadwalanHelper#pindahkanUrutanPertemuan}; field-field
	 * yang bisa diedit inline langsung ke {@code Pertemuan} (topik, indikator, waktu pembelajaran,
	 * pengalaman belajar, tugas dan penilaian, dua buku rujukan, metode pembelajaran) — tiap perubahan
	 * langsung memanggil {@code Common.refreshUpdate}; combobox status pertemuan; checkbox aktif; tanggal
	 * dan jam mulai/selesai (dapat diedit bila pengguna bukan mahasiswa dan bukan dosen yang dikunci
	 * tanggalnya, dengan tombol hapus per baris yang melalui {@link PenjadwalanHelper#checkBolehHapus}
	 * lebih dulu); serta indikator "sesuai" (checkbox untuk pengelola, ikon untuk peran lain).</p>
	 * <p><b>Efek samping:</b> hampir setiap listener pada komponen yang dibangun langsung melakukan
	 * simpan/update/hapus ke basis data via Hibernate (lewat {@code Common.refreshUpdate}/
	 * {@code refreshSaveOrUpdate} atau {@code PertemuanDao.delete}) dan memanggil {@code perkuliahan.belum()}
	 * untuk membersihkan cache pertemuan sebelum memicu {@code eventListener} agar grid dimuat ulang.
	 * Harus dijalankan pada event thread ZK dengan konteks pengguna/session aktif.</p>
	 *
	 * @see PenjadwalanHelper
	 */
	public static class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();
		private Long pertId = -1L;
		private Perkuliahan perkuliahan;
		private EventListener eventListener;

		private Integer perteKe = 0;

		/**
		 * @param perkuliahan   perkuliahan pemilik pertemuan-pertemuan yang akan dirender
		 * @param eventListener dipanggil setiap kali suatu aksi pada baris (edit tanggal, hapus,
		 *                      naik/turun urutan, absen, dst.) mengubah data, agar pemanggil memuat
		 *                      ulang grid
		 */
		public PertemuanRenderer(Perkuliahan perkuliahan, EventListener eventListener) {
			this.perkuliahan = perkuliahan;
			this.eventListener = eventListener;
		}

		/**
		 * Merender satu baris grid untuk id {@link Pertemuan} yang diberikan sebagai {@code arg1}
		 * (nilai model diresolusi ke entity via {@code GeneralValueObject.ambilData}). Baris
		 * disembunyikan ({@code setVisible(false)}) bila data kosong/null atau id-nya tidak valid.
		 * Lihat javadoc {@link PertemuanRenderer} untuk rincian komponen yang dibangun.
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 nilai model baris — biasanya id {@link Pertemuan} dalam bentuk {@code String}
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			if (arg1 == null) {
				arg0.setVisible(false);
				return;
			}
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, arg1.toString(),
					true);

			if (pertemuan == null || pertemuan.getId() == null) {
				arg0.setVisible(false);
				return;
			}

			arg0.setAttribute("myValue", pertemuan);

			perteKe++;

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			if (pertId != null && pertId.equals(pertemuan.getId())) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {
						Clients.scrollIntoView(arg0);
					}
				});
			}

			MyGroupboxStyled tools = new MyGroupboxStyled();
			tools.setWidth("100%");
			tools.setParent(detail);
			tools.setStyleLangsung(
					"text-align:center;border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;");

			Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false, new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {
					perkuliahan.belum();
					pertId = pertemuan.getId();
					eventListener.onEvent(a);
				}
			});

			Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, new DataLoader() {

				@Override
				public void loadData(Object value) {
					perkuliahan.belum();
					pertId = pertemuan.getId();
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:195");
					}

				}
			});

			MyToolbarbutton a = new MyToolbarbutton("fa-calendar-o", "Agenda");

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {

						@Override
						public void onEvent(Event a) throws Exception {
							perkuliahan.belum();
							pertId = pertemuan.getId();
							try {
								eventListener.onEvent(null);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:218");
							}
						}
					});
				}
			});

			AktifitasPerkuliahanHelper.createKeteranganData(pertemuan, tbmuser, tbmuser.getMahasiswa(),
					tbmuser.getBiodataCalonMahasiswa(), new DataLoader() {

						@Override
						public void loadData(Object value) {
							perkuliahan.belum();
							pertId = pertemuan.getId();
							try {
								eventListener.onEvent(null);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:236");
							}
						}
					}, true, false, aa, a, bb, DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan))
					.setParent(tools);

			/*
			 * Kolom "Urutan": nomor pertemuan + tombol Naikkan/Turunkan.
			 *
			 * Latar belakang keluhan ("tadinya pertemuan 3 jadi 1", "UTS tadinya pertemuan 8
			 * jadi 6"): secara default sebuah perkuliahan memakai mode urut OTOMATIS
			 * (urutkanotomatis = true), di mana nomor "Pertemuan ke-" dihitung ulang setiap
			 * data dimuat berdasarkan TANGGAL pertemuan (lihat VOPembelajaran.reInitPertemuan).
			 * Jadi begitu tanggal sebuah pertemuan diubah/disisipkan, nomor urutnya ikut
			 * bergeser. Tombol Naikkan/Turunkan di bawah memberi dosen/guru/admin kendali
			 * penuh: sekali ditekan, mode dipaksa MANUAL dan urutan dikunci sehingga tidak
			 * lagi ditimpa pengurutan tanggal (lihat PenjadwalanHelper.pindahkanUrutanPertemuan).
			 */
			Vbox selUrutan = new Vbox();
			selUrutan.setParent(arg0);
			selUrutan.setWidth("100%");

			VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
			if (pembelajaran != null && !pembelajaran.getUrutkanotomatis()) {
				final Intbox pertemuanManual = new Intbox(pertemuan.getPertemuanManual());
				pertemuanManual.setParent(selUrutan);
				pertemuanManual.setWidth("90%");
				pertemuanManual.setStyle("font-size:16px;font-weight: bolder;");
				pertemuanManual.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setPertemuanManual(pertemuanManual.getValue());
						/* Samakan pertemuanKe agar pengurutan grid (ORDER BY pertemuan_ke) ikut. */
						pertemuan.setPertemuanKe(pertemuanManual.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});
			} else {

				pertemuan.setPertemuanKe(perteKe);

				new MyLabelBolder(pertemuan.getPertemuanKe() + "").setParent(selUrutan);
			}

			/*
			 * Tombol Naikkan/Turunkan hanya untuk pengelola jadwal (bukan
			 * mahasiswa/siswa/calon), dan dosen hanya bila diizinkan mengubah jadwal
			 * perkuliahan — sama dengan syarat boleh mengubah tanggal di bawah.
			 */
			boolean bolehUbahUrutan = tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null
					&& (tbmuser.ambilDosen() == null
							|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()));

			if (bolehUbahUrutan) {
				final MyToolbarbuttonConfig tombolNaik = new MyToolbarbuttonConfig("Naik");
				tombolNaik.setTooltiptext("Naikkan urutan pertemuan ini (pindah ke atas)");
				tombolNaik.setStyle("font-size:10px;color:#1d4ed8;padding:1px 4px;");
				tombolNaik.setParent(selUrutan);
				tombolNaik.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event ev) throws Exception {
						if (PenjadwalanHelper.pindahkanUrutanPertemuan(perkuliahan, pertemuan, true)) {
							pertId = pertemuan.getId();
							eventListener.onEvent(ev);
						}
					}
				});

				final MyToolbarbuttonConfig tombolTurun = new MyToolbarbuttonConfig("Turun");
				tombolTurun.setTooltiptext("Turunkan urutan pertemuan ini (pindah ke bawah)");
				tombolTurun.setStyle("font-size:10px;color:#1d4ed8;padding:1px 4px;");
				tombolTurun.setParent(selUrutan);
				tombolTurun.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event ev) throws Exception {
						if (PenjadwalanHelper.pindahkanUrutanPertemuan(perkuliahan, pertemuan, false)) {
							pertId = pertemuan.getId();
							eventListener.onEvent(ev);
						}
					}
				});
			}

			final MyLabelEdit topik = new MyLabelEdit(pertemuan.getTopik());
			topik.setWidth("90%");
			topik.setRows(4);
			topik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setTopik(topik.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			topik.setParent(arg0);

			final MyLabelEdit indikator = new MyLabelEdit(pertemuan.getIndikator());
			indikator.setWidth("90%");
			indikator.setRows(4);
			indikator.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setIndikator(indikator.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			indikator.setParent(arg0);

			final MyLabelEdit waktupembelajaran = new MyLabelEdit(pertemuan.getWaktupembelajaran());
			waktupembelajaran.setWidth("90%");
			waktupembelajaran.setRows(4);
			waktupembelajaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setWaktupembelajaran(waktupembelajaran.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			waktupembelajaran.setParent(arg0);

			final MyLabelEdit pengalamanBelajar = new MyLabelEdit(pertemuan.getPengalamanBelajar());
			pengalamanBelajar.setWidth("90%");
			pengalamanBelajar.setRows(4);
			pengalamanBelajar.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setPengalamanBelajar(pengalamanBelajar.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			pengalamanBelajar.setParent(arg0);

			final MyLabelEdit tugasDanPenilaian = new MyLabelEdit(pertemuan.getTugasDanPenilaian());
			tugasDanPenilaian.setWidth("90%");
			tugasDanPenilaian.setRows(4);
			tugasDanPenilaian.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setTugasDanPenilaian(tugasDanPenilaian.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			tugasDanPenilaian.setParent(arg0);

			final MyLabelEdit bukuRujukan1 = new MyLabelEdit(pertemuan.getBukuRujukan1());
			bukuRujukan1.setWidth("90%");
			bukuRujukan1.setRows(4);
			bukuRujukan1.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setBukuRujukan1(bukuRujukan1.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			bukuRujukan1.setParent(arg0);

			final MyLabelEdit bukuRujukan2 = new MyLabelEdit(pertemuan.getBukuRujukan2());
			bukuRujukan2.setWidth("90%");
			bukuRujukan2.setRows(4);
			bukuRujukan2.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setBukuRujukan2(bukuRujukan2.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			bukuRujukan2.setParent(arg0);

			final MyLabelEdit metodePembelajaran = new MyLabelEdit(pertemuan.getMetodePembelajaran());
			metodePembelajaran.setWidth("90%");
			metodePembelajaran.setRows(4);
			metodePembelajaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			metodePembelajaran.setParent(arg0);

			final Combobox combobox = new Combobox();
			Common.insertCombo(combobox, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(combobox, pertemuan.getStatusPertemuan());
			combobox.setWidth("90%");
			combobox.setParent(arg0);
			combobox.setReadonly(true);

			combobox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					/*
					 * ZK tetap dapat mengirim onChange ketika nilai ComboBox dikosongkan dari
					 * sisi klien, walaupun komponennya readonly. Jangan dereference item null;
					 * kembalikan pilihan lama dan abaikan event yang tidak membawa pilihan sah.
					 */
					Comboitem itemTerpilih = combobox.getSelectedItem();
					if (itemTerpilih == null || !(itemTerpilih.getValue() instanceof StatusPertemuan)) {
						Common.selectComboItem(combobox, pertemuan.getStatusPertemuan());
						return;
					}
					pertemuan.setStatusPertemuan((StatusPertemuan) itemTerpilih.getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(pertemuan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pertemuan);
				}
			});

			final MyDatebox mulai = new MyDatebox();
			mulai.setValue(pertemuan.getTanggal());

			if (tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
					|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);

				mulai.setWidth("90%");
				mulai.setParent(vbox);
				mulai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setTanggal(mulai.getValue());
						pertemuan.setTanggalEdit(mulai.getValue());
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (pertemuan));
					}
				});

				final Timebox waktuMulai;
				final Timebox waktuSelesai;
				waktuMulai = new ais.ui.util.MyTimebox();
				waktuSelesai = new ais.ui.util.MyTimebox();
				waktuMulai.setFormat(Common.timeFormat.get().toPattern());
				waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

				try {
					waktuMulai.setValue(
							pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanHelper.java:500");

				}
				try {
					waktuSelesai.setValue(
							pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanHelper.java:507");

				}

				EventListener updateLocal = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuMulai.getValue()));
						pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuSelesai.getValue()));

						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (pertemuan));
					}
				};

				waktuMulai.setCols(1);
				waktuSelesai.setCols(1);

				waktuMulai.addEventListener("onChange", updateLocal);
				waktuSelesai.addEventListener("onChange", updateLocal);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				waktuMulai.setParent(hbox);
				waktuSelesai.setParent(hbox);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (!PenjadwalanHelper.checkBolehHapus(pertemuan)) {
							return;
						}

						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												PertemuanDao pertemuanDao = DaoFactory.getInstance().getPertemuanDao();
												// pertemuanDao.beginTransaction();

												pertemuanDao.delete((pertemuan));

												perkuliahan.belum();
												pertId = pertemuan.getId();
												Common.createDefaultTimer(eventListener);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("penghapusan data pertemuan",
														e,
														new String[] {
																"Periksa apakah pertemuan ini masih memiliki data terkait (materi, tugas, ujian, diskusi, atau absensi) yang harus dihapus/dilepaskan terlebih dahulu.",
																"Pastikan tidak ada mahasiswa atau dosen lain yang sedang membuka/menggunakan data pertemuan ini.",
																"Coba ulangi proses penghapusan beberapa saat lagi.",
																"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
														});
											}

										}

									}
								});

					}
				});
				button.setParent(toolbar);
				toolbar.setParent(arg0);

			} else {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(pertemuan.getTanggal() == null ? "" : Common.dateFormat1.get().format(pertemuan.getTanggal()))
						.setParent(vbox);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);

				new MyLabelKecil(pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai()).setParent(hbox);
				new MyLabelKecil(pertemuan.getWaktuSelesai() == null ? "" : "s.d " + pertemuan.getWaktuSelesai())
						.setParent(hbox);

				new Label().setParent(arg0);
			}

			if (tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getDosen() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
				final MyCheckboxConfig sesuai = new MyCheckboxConfig("");
				sesuai.setChecked(pertemuan.getSesuai());
				sesuai.setParent(arg0);
				sesuai.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setAktif(sesuai.isChecked());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});
			} else {
				new Image(pertemuan.getSesuai() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg")
						.setParent(arg0);
			}

			pertemuan.getTanggal();
			arg0.setAttribute("topik", topik);
			arg0.setAttribute("indikator", indikator);
			arg0.setAttribute("waktupembelajaran", waktupembelajaran);
			arg0.setAttribute("pengalamanBelajar", pengalamanBelajar);
			arg0.setAttribute("tugasDanPenilaian", tugasDanPenilaian);
			arg0.setAttribute("buku", bukuRujukan1);
			arg0.setAttribute("ref", bukuRujukan2);
			arg0.setAttribute("metodePembelajaran", metodePembelajaran);
			arg0.setAttribute("statuspertemuan", combobox);
			arg0.setAttribute("date", mulai);
		}

	}

	/**
	 * Memindahkan urutan sebuah pertemuan satu langkah ke atas (naik) atau ke bawah
	 * (turun) sesuai permintaan dosen/guru/admin.
	 *
	 * <p><b>Latar belakang.</b> Secara default sebuah {@link Perkuliahan} memakai mode
	 * urut OTOMATIS ({@code urutkanotomatis = true}). Pada mode ini nomor "Pertemuan ke-"
	 * dihitung ulang setiap kali data dimuat berdasarkan <i>tanggal</i> pertemuan
	 * (lihat {@link ais.database.model.VOPembelajaran#reInitPertemuan}). Akibatnya, ketika
	 * tanggal sebuah pertemuan diubah (mis. UTS dimajukan, atau pertemuan pengganti
	 * disisipkan dengan tanggal lebih awal), nomor urutnya ikut bergeser — inilah keluhan
	 * "tadinya pertemuan 3 jadi 1" dan "UTS tadinya pertemuan 8 jadi 6".</p>
	 *
	 * <p><b>Yang dilakukan metode ini.</b>
	 * <ol>
	 *   <li>Memuat seluruh pertemuan AKTIF milik perkuliahan dalam urutan tampil saat ini
	 *       (sama dengan grid: by {@code pertemuan_ke} bila manual, atau by tanggal bila
	 *       otomatis).</li>
	 *   <li>Menukar posisi pertemuan yang dipilih dengan tetangganya (atas/bawah).</li>
	 *   <li>Memaksa mode ke MANUAL ({@code urutkanotomatis = false}) supaya urutan baru
	 *       tidak lagi ditimpa oleh pengurutan tanggal.</li>
	 *   <li>Menomori ulang 1..N. Baik {@code pertemuanManual} maupun {@code pertemuanKe}
	 *       diset sama agar konsisten dengan query yang memakai {@code ORDER BY pertemuan_ke}.</li>
	 *   <li>Mengosongkan cache pertemuan ({@code perkuliahan.belum()}) agar render berikutnya
	 *       membaca urutan terbaru.</li>
	 * </ol>
	 * </p>
	 *
	 * @param perkuliahan perkuliahan pemilik pertemuan
	 * @param pertemuan   pertemuan yang akan dipindah
	 * @param naik        {@code true} = pindah ke atas (nomor mengecil), {@code false} = ke bawah
	 * @return {@code true} bila urutan benar-benar berubah; {@code false} bila sudah berada
	 *         di tepi (paling atas/bawah) atau data tidak valid sehingga grid tak perlu dimuat ulang
	 */
	@SuppressWarnings("unchecked")
	public static boolean pindahkanUrutanPertemuan(final Perkuliahan perkuliahan, final Pertemuan pertemuan,
			final boolean naik) {
		if (perkuliahan == null || pertemuan == null || pertemuan.getId() == null) {
			return false;
		}
		try {
			Session session = HibernateUtil.currentSession();

			/* Ambil seluruh pertemuan aktif dalam urutan tampil saat ini (identik dengan grid). */
			List<Pertemuan> daftar = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("perkuliahan", perkuliahan))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.addOrder(Order.asc("id"))
					.list();

			if (daftar == null || daftar.size() < 2) {
				return false;
			}

			/* Cari posisi pertemuan yang dipindah berdasarkan id (instance bisa berbeda dari cache). */
			int idx = -1;
			for (int i = 0; i < daftar.size(); i++) {
				if (daftar.get(i) != null && pertemuan.getId().equals(daftar.get(i).getId())) {
					idx = i;
					break;
				}
			}
			if (idx < 0) {
				return false;
			}

			int target = naik ? idx - 1 : idx + 1;
			if (target < 0 || target >= daftar.size()) {
				return false; // sudah di paling atas / paling bawah → tidak ada yang berubah
			}

			/* Tukar dua pertemuan bertetangga. */
			Pertemuan tmp = daftar.get(idx);
			daftar.set(idx, daftar.get(target));
			daftar.set(target, tmp);

			/* Paksa mode MANUAL agar urutan baru tidak ditimpa pengurutan tanggal. */
			if (perkuliahan.getUrutkanotomatis() == null || perkuliahan.getUrutkanotomatis()) {
				perkuliahan.setUrutkanotomatis(false);
				Common.refreshUpdate(session, perkuliahan);
			}

			/* Nomori ulang 1..N; set pertemuanManual & pertemuanKe sama-sama agar konsisten. */
			for (int i = 0; i < daftar.size(); i++) {
				Pertemuan p = daftar.get(i);
				if (p == null) {
					continue;
				}
				int nomor = i + 1;
				p.setPertemuanManual(nomor);
				p.setPertemuanKe(nomor);
				Common.refreshUpdate(session, p);
			}

			perkuliahan.belum(); // invalidasi cache lokasi pertemuan
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	/**
	 * Sama seperti {@link #checkBolehHapus(Pertemuan, boolean)} dengan {@code warning=true}, yaitu
	 * menampilkan pesan peringatan ZK bila pertemuan tidak boleh dihapus.
	 *
	 * @param pertemuan pertemuan yang akan divalidasi
	 * @return {@code true} bila pertemuan boleh dihapus
	 */
	public static boolean checkBolehHapus(Pertemuan pertemuan) throws Exception {
		return checkBolehHapus(pertemuan, true);
	}

	/**
	 * Memvalidasi apakah satu {@link Pertemuan} boleh dihapus, berdasarkan ada/tidaknya data terkait
	 * yang akan ikut hilang: absensi kehadiran ("M"/masuk pada {@code hitungStatus()}), materi
	 * ({@code PertemuanFileContent}), audio, video, ujian ({@code PertemuanPunyaUjian}), diskusi, tugas
	 * yang sudah dikumpulkan mahasiswa, dan judul tugas yang sudah diisi. Setiap kondisi yang gagal
	 * langsung menghentikan pengecekan (return {@code false}) tanpa memeriksa kondisi berikutnya.
	 *
	 * @param pertemuan pertemuan yang akan divalidasi
	 * @param warning   bila {@code true}, tampilkan {@link MyMessageboxConfig} berisi alasan spesifik
	 *                  saat pertemuan tidak boleh dihapus
	 * @return {@code true} bila pertemuan tidak memiliki data terkait sehingga aman dihapus
	 */
	public static boolean checkBolehHapus(Pertemuan pertemuan, boolean warning) throws Exception {

		Map<String, Integer> statuses = pertemuan.hitungStatus();
		int masuk = pertemuan == null || statuses.get("M") == null ? 0 : statuses.get("M");
		if (masuk > 0) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah terdapat data absensi kehadiran, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (pertemuan.ambilJumlahPertemuanFileContent() > 0) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah memiliki data materi pertemuan, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (pertemuan.ambilJumlahAudioPertemuan() > 0) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah memiliki data audio, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (pertemuan.ambilJumlahVideoPertemuan() > 0) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah memiliki data video, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (pertemuan.ambilJumlahPertemuanPunyaUjian() > 0) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah memiliki data ujian, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (pertemuan.ambilJumlahPertemuanPunyaDiskusi() > 0) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah memiliki data diskusi, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (pertemuan.ambilJumlahTugasFileContent() > 0) {
			if (warning)
				MyMessageboxConfig.show("Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \""
						+ pertemuan.getTopik()
						+ "\" telah memiliki data tugas, dimana mahasiswa telah mengumulkan tugas teserbut, Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (!pertemuan.getJudultugas().trim().isEmpty()) {
			if (warning)
				MyMessageboxConfig.show(
						"Pertemuan ke " + pertemuan.getPertemuanKe() + " dengan pembahasan \"" + pertemuan.getTopik()
								+ "\" telah memiliki tugas dengan judul \"" + pertemuan.getJudultugas()
								+ "\", Anda tidak bisa menghapus pertemuan ini",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		return true;
	}

	/**
	 * Memvalidasi apakah SELURUH pertemuan milik satu pemilik agenda (perkuliahan/KKN/PKL/tugas akhir/
	 * skripsi/KRS/formulir kegiatan/wisuda — tepat satu parameter yang tidak {@code null} dipakai untuk
	 * mengambil daftar pertemuannya lewat {@code ambilPertemuanList()}) boleh dihapus sekaligus, dengan
	 * memeriksa tiap pertemuan lewat {@link #checkBolehHapus(Pertemuan)} (tanpa peringatan per-item; ia
	 * sendiri yang menampilkan pesan begitu menemukan satu pertemuan yang tidak boleh dihapus). Dipakai
	 * sebagai gerbang sebelum {@link #tampilTombolHapus} (hapus semua pertemuan) maupun sebelum
	 * regenerasi penuh agenda (hapus-lalu-buat-ulang) pada {@link #tampilTombolBuatPertemuan}.
	 *
	 * @return {@code true} bila semua pertemuan pemilik boleh dihapus, atau daftar pertemuannya kosong;
	 *         {@code false} begitu ditemukan satu pertemuan yang tidak boleh dihapus (berhenti di situ)
	 */
	public static boolean bolehHapus(final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn,
			final KelompokPkl kelompokPkl, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final Skripsi skripsi, final KrsMahasiswa krsMahasiswa, final FormulirKegiatan formulirKegiatan,
			final Wisuda wisuda) throws Exception {
		try {
			List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
			if (perkuliahan != null) {
				pertemuans = perkuliahan.ambilPertemuanList();
			} else if (kelompokKkn != null) {
				pertemuans = kelompokKkn.ambilPertemuanList();
			} else if (kelompokPkl != null) {
				pertemuans = kelompokPkl.ambilPertemuanList();
			} else if (mahasiswaRequestTugasAkhir != null) {
				pertemuans = mahasiswaRequestTugasAkhir.ambilPertemuanList();
			} else if (skripsi != null) {
				pertemuans = skripsi.ambilPertemuanList();
			} else if (krsMahasiswa != null) {
				pertemuans = krsMahasiswa.ambilPertemuanList();
			} else if (formulirKegiatan != null) {
				pertemuans = formulirKegiatan.ambilPertemuanList();
			} else if (wisuda != null) {
				pertemuans = wisuda.ambilPertemuanList();
			}

			for (Pertemuan pertemuan : pertemuans) {
				if (!PenjadwalanHelper.checkBolehHapus(pertemuan)) {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:842");
		}
		return true;
	}

	/**
	 * Menambahkan tombol "Download" (cetak/ekspor Excel) ke {@code toolbar} untuk daftar pertemuan milik
	 * satu pemilik agenda (hanya satu dari kedelapan parameter pemilik yang perlu diisi). Kolom dasar
	 * ({@code contents}) diperkaya dengan kolom tambahan per baris lewat {@code dataAdding}: ID, topik,
	 * tanggal, jam mulai/selesai, dan jenis pertemuan, ditambah kolom dinamis dari
	 * {@code pertemuan.ambilDataParameterTambahan()} — nilai yang punya URL dijadikan hyperlink berwarna
	 * biru bergaris bawah pada sel Excel yang dihasilkan (via POI XSSF).
	 *
	 * @param toolbar     toolbar ZK tempat tombol ditambahkan
	 * @param contents    nama-nama field {@link Pertemuan} yang diekspor sebagai kolom dasar
	 * @param perkuliahan pemilik agenda (perkuliahan), atau {@code null} bila pemilik jenis lain dipakai
	 */
	public static void tampilTombolDownload(Toolbar toolbar, String[] contents, final Perkuliahan perkuliahan,
			final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Skripsi skripsi,
			final KrsMahasiswa krsMahasiswa, final FormulirKegiatan formulirKegiatan, final Wisuda wisuda) {
		List<String> columnHeadersAddingTambahan = new ArrayList<String>();
		columnHeadersAddingTambahan.add("ID");
		columnHeadersAddingTambahan.add("Topik/Materi/Pembahasan");
		columnHeadersAddingTambahan.add("Tanggal");
		columnHeadersAddingTambahan.add("Waktu Mulai");
		columnHeadersAddingTambahan.add("Waktu Selesai");
		columnHeadersAddingTambahan.add("Jenis");

		List<String> columnHeadersAdding = null;
		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				try {

					Object[] objects = (Object[]) arg0.getData();
					Pertemuan pertemuan = (Pertemuan) objects[0];

					XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
					XSSFRow rowTambahan = (XSSFRow) objects[4];
					XSSFRow rowheadTambahan = (XSSFRow) objects[5];
					XSSFFont hlink_font = workbook.createFont();
					hlink_font.setUnderline(XSSFFont.U_SINGLE);
					hlink_font.setColor(new XSSFColor(Color.BLUE));

					final XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
					hlink_style.setFont(hlink_font);

					if (rowTambahan != null) {
						rowTambahan.createCell(0).setCellValue(pertemuan.getId());
						rowTambahan.createCell(1).setCellValue(pertemuan.getTopik());
						rowTambahan.createCell(2).setCellValue(Common.dateFormat6.get().format(pertemuan.getTanggal()));
						rowTambahan.createCell(3).setCellValue(pertemuan.getWaktuMulai());
						rowTambahan.createCell(4).setCellValue(pertemuan.getWaktuSelesai());
						rowTambahan.createCell(5).setCellValue(
								pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());

						int j = 0;

						for (CommonVO commonVO : pertemuan.ambilDataParameterTambahan()) {
							int indexCol = j + 5;
							j++;

							String lbl = commonVO.getName();
							String url = commonVO.getName2();
							String val = commonVO.getName1();

							if (rowheadTambahan != null) {
								XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
								if (hssfCell == null) {
									rowheadTambahan.createCell(indexCol).setCellValue(lbl);
								}
							}

							XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
							cellTambahan.setCellValue(val);
							if (url != null && !url.trim().isEmpty()) {
								cellTambahan.setCellStyle(hlink_style);
								XSSFHyperlink link = workbook.getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
								link.setAddress(url);
								cellTambahan.setHyperlink(link);
							}
						}

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:920");
				}
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Pertemuan.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();

				boolean urut = true;
				if (perkuliahan != null) {
					urut = perkuliahan.getUrutkanotomatis();
				} else if (kelompokKkn != null) {
					urut = kelompokKkn.getUrutkanotomatis();
				} else if (kelompokPkl != null) {
					urut = kelompokPkl.getUrutkanotomatis();
				} else if (mahasiswaRequestTugasAkhir != null) {
					urut = mahasiswaRequestTugasAkhir.getUrutkanotomatis();
				} else if (skripsi != null) {
					urut = skripsi.getUrutkanotomatis();
				} else if (krsMahasiswa != null) {
					urut = krsMahasiswa.getUrutkanotomatis();
				} else if (formulirKegiatan != null) {
					urut = formulirKegiatan.getUrutkanotomatis();
				} else if (wisuda != null) {
					urut = wisuda.getUrutkanotomatis();
				}

				return session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(!urut ? Order.asc("pertemuanKe") : Order.asc("tanggal")).addOrder(Order.asc("id"))

						.add(perkuliahan != null ? Restrictions.eq("perkuliahan", perkuliahan)
								: Restrictions.sqlRestriction("true"))

						.add(kelompokKkn != null ? Restrictions.eq("kelompokKkn", kelompokKkn)
								: Restrictions.sqlRestriction("true"))

						.add(kelompokPkl != null ? Restrictions.eq("kelompokPkl", kelompokPkl)
								: Restrictions.sqlRestriction("true"))

						.add(mahasiswaRequestTugasAkhir != null
								? Restrictions.eq("mahasiswaRequestTugasAkhir", mahasiswaRequestTugasAkhir)
								: Restrictions.sqlRestriction("true"))

						.add(skripsi != null ? Restrictions.eq("skripsi", skripsi)
								: Restrictions.sqlRestriction("true"))

						.add(krsMahasiswa != null ? Restrictions.eq("krsMahasiswa", krsMahasiswa)
								: Restrictions.sqlRestriction("true"))

						.add(formulirKegiatan != null ? Restrictions.eq("formulirKegiatan", formulirKegiatan)
								: Restrictions.sqlRestriction("true"))

						.add(wisuda != null ? Restrictions.eq("wisuda", wisuda) : Restrictions.sqlRestriction("true"))

				;
			}
		}, "Download", "/img/print.png", columnHeadersAdding, dataAdding, true, columnHeadersAddingTambahan, contents);
		toolbar.appendChild(cetakToolbarbutton);
	}

	/**
	 * Menambahkan tombol "Hapus" ke {@code toolbar} yang, setelah lolos validasi
	 * {@link #bolehHapus} dan konfirmasi pengguna, menghapus PERMANEN seluruh pertemuan (beserta
	 * {@code tugas_pertemuan} terkait) milik satu pemilik agenda lewat
	 * {@link #hapusPertemuanBesertaTugas} (SQL native, bukan lewat Hibernate delete per-baris).
	 *
	 * @param toolbar       toolbar ZK tempat tombol ditambahkan
	 * @param perkuliahan   pemilik agenda (perkuliahan), atau {@code null} bila pemilik jenis lain dipakai
	 * @param eventListener dipanggil setelah penghapusan berhasil agar pemanggil memuat ulang tampilan
	 */
	public static void tampilTombolHapus(Toolbar toolbar, final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn,
			final KelompokPkl kelompokPkl, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final Skripsi skripsi, final KrsMahasiswa krsMahasiswa, final FormulirKegiatan formulirKegiatan,
			final Wisuda wisuda, final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (!bolehHapus(perkuliahan, kelompokKkn, kelompokPkl, mahasiswaRequestTugasAkhir, skripsi,
						krsMahasiswa, formulirKegiatan, wisuda)) {
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua pertemuan ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										Session session = HibernateUtil.currentSession();

										if (perkuliahan != null) {
											perkuliahan.belum();
											hapusPertemuanBesertaTugas(session, "perkuliahan", perkuliahan.getId());
										} else if (kelompokKkn != null) {
											hapusPertemuanBesertaTugas(session, "kelompok_kkn", kelompokKkn.getId());
										} else if (kelompokPkl != null) {
											hapusPertemuanBesertaTugas(session, "kelompok_pkl", kelompokPkl.getId());
										} else if (formulirKegiatan != null) {
											hapusPertemuanBesertaTugas(session, "formulir_kegiatan", formulirKegiatan.getId());
										} else if (mahasiswaRequestTugasAkhir != null) {
											hapusPertemuanBesertaTugas(session, "mahasiswa_request_tugas_akhir",
													mahasiswaRequestTugasAkhir.getId());
										} else if (krsMahasiswa != null) {
											hapusPertemuanBesertaTugas(session, "krs_mahasiswa", krsMahasiswa.getId());
										} else if (skripsi != null) {
											hapusPertemuanBesertaTugas(session, "skripsi", skripsi.getId());
										} else if (wisuda != null) {
											hapusPertemuanBesertaTugas(session, "wisuda", wisuda.getId());
										}

										eventListener.onEvent(event);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});

			}
		});
		button.setParent(toolbar);
	}

	/**
	 * Menghapus PERMANEN, lewat SQL native (bukan Hibernate/dao), semua baris {@code tugas_pertemuan}
	 * lalu semua baris {@code pertemuan} milik satu pemilik, tanpa melalui validasi
	 * {@link #checkBolehHapus} — pemanggil ({@link #tampilTombolHapus}) bertanggung jawab memvalidasi
	 * lebih dulu. Dipakai untuk hapus massal karena jauh lebih cepat daripada hapus per-entity Hibernate.
	 *
	 * @param session      session Hibernate aktif untuk menjalankan SQL native
	 * @param kolomPemilik nama kolom FK pemilik pada tabel {@code pertemuan} (mis. "perkuliahan",
	 *                     "kelompok_kkn", "skripsi", dst.)
	 * @param idPemilik    id baris pemilik pada kolom tersebut
	 */
	private static void hapusPertemuanBesertaTugas(Session session, String kolomPemilik, Long idPemilik) {
		String kondisi = kolomPemilik + "=:idPemilik";
		session.createSQLQuery("delete from tugas_pertemuan where pertemuan in "
				+ "(select id from pertemuan where " + kondisi + ")").setLong("idPemilik", idPemilik).executeUpdate();
		session.createSQLQuery("delete from pertemuan where " + kondisi).setLong("idPemilik", idPemilik)
				.executeUpdate();
	}

	/**
	 * Membuka jendela modal "Atur Tanggal &amp; Interval Pertemuan": form untuk mengatur ulang secara
	 * MASSAL tanggal, jam, dan interval (harian/tgl ganjil-genap/2-6 harian/mingguan/2-4 mingguan/bulanan)
	 * seluruh pertemuan milik satu pemilik agenda, dengan panel pratinjau daftar tanggal yang diperbarui
	 * langsung (live, lewat listener {@code refreshPratinjau} yang memanggil
	 * {@link #daftarPertemuanPratinjau}) setiap field tanggal/jenis/lewati-libur berubah.
	 *
	 * <p>Saat "Simpan" ditekan: field jadwal (jam mulai/selesai, lewati tanggal merah, tanggal mulai,
	 * jenis interval, boleh menentukan tanggal mulai sendiri) disimpan ke entity pemilik; kemudian setiap
	 * id pertemuan pada {@code pemilik.ambilPertemuan()} dimuat satu-per-satu dan tanggal/jamnya ditimpa
	 * berurutan sesuai kalender yang dihitung ulang dari tanggal mulai baru (melompati tanggal merah bila
	 * dicentang, via {@code Common.tanggalMerahAja}/{@code Common.curreDate}), lalu di-commit per baris
	 * dalam transaksi terpisah dan session ditutup setelah tiap iterasi.</p>
	 *
	 * @param perkuliahan   pemilik agenda (perkuliahan), atau {@code null} bila pemilik jenis lain dipakai
	 * @param eventListener dipanggil setelah proses simpan selesai agar pemanggil memuat ulang tampilan
	 */
	public static void prosesTampilTombolAturUlangWaktu(final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn,
			final KelompokPkl kelompokPkl, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final Skripsi skripsi, final KrsMahasiswa krsMahasiswa, final FormulirKegiatan formulirKegiatan,
			final Wisuda wisuda, final EventListener eventListener) throws Exception {
		final Window window = new Window();
		window.setTitle("Atur Tanggal & Interval Pertemuan");
		window.setBorder("normal");
		window.setClosable(true);
		window.setHeight("600px");
		window.setWidth("92%");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		center.setAutoscroll(true);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		final MyDatebox tanggalMulai;
		row.appendChild(
				tanggalMulai = new MyDatebox(perkuliahan == null ? null : perkuliahan.getTanggalMulaiPerkuliahan()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig lewatiTanggalMerahNasional = new MyCheckboxConfig("Lewati tanggal merah / hari libur");
		lewatiTanggalMerahNasional.setChecked(perkuliahan == null ? true : perkuliahan.getLewatiTanggalMerahNasional());
		row.appendChild(lewatiTanggalMerahNasional);

		Date dateMulai = null;
		Date dateSelesai = null;
		try {
			if (perkuliahan != null)
				if ((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) != null
						&& !(perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()).equals(""))
					dateMulai = Common.timeFormat2.get()
							.parse((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (perkuliahan != null)
				if ((perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()) != null
						&& !(perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()).equals(""))
					dateSelesai = Common.timeFormat2.get()
							.parse((perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		final MyFormRow rowWaktu = new MyFormRow();
		rowWaktu.setStyle("border:0px;background: transparent;");
		rowWaktu.setParent(rows);
		rowWaktu.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
		Hbox hbox = new Hbox();
		rowWaktu.appendChild(hbox);
		final MyTimebox waktuMulai;
		hbox.appendChild(waktuMulai = new MyTimebox(dateMulai));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		final MyTimebox waktuSelesai;
		hbox.appendChild(waktuSelesai = new MyTimebox(dateSelesai));

		final MyCheckboxConfig bolehMenentukanTanggalMulaiPerkuliahan = new MyCheckboxConfig(
				"Bedakan Tanggal Mulai Perkuliahan");
		bolehMenentukanTanggalMulaiPerkuliahan
				.setChecked(perkuliahan == null ? false : perkuliahan.getBolehMenentukanTanggalMulaiPerkuliahan());

		if (perkuliahan != null
				&& ((perkuliahan.getMasaPerkuliahan() != null && perkuliahan.getMasaPerkuliahan().getMulai() != null
						&& perkuliahan.getMasaPerkuliahan().getTanggalMulaiHarusSesuaiJadwal())
						|| perkuliahan.getAwalPerkuliahan() != null)) {
			if (tanggalMulai.getValue() != null) {
				if (tanggalMulai != null) tanggalMulai.setDisabled(!perkuliahan.getBolehMenentukanTanggalMulaiPerkuliahan());
			}
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));

			row.appendChild(bolehMenentukanTanggalMulaiPerkuliahan);
			bolehMenentukanTanggalMulaiPerkuliahan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perkuliahan.setBolehMenentukanTanggalMulaiPerkuliahan(
							bolehMenentukanTanggalMulaiPerkuliahan.isChecked());
					Common.refreshUpdate(perkuliahan);
					if (tanggalMulai.getValue() != null) {
						if (tanggalMulai != null) tanggalMulai.setDisabled(!perkuliahan.getBolehMenentukanTanggalMulaiPerkuliahan());
					}
				}
			});

		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		final Radiogroup jenis;
		row.appendChild(jenis = new Radiogroup());

		jenis.appendChild(new MyRadioConfig("Harian"));
		jenis.appendChild(new MyRadioConfig("Tgl Ganjil"));
		jenis.appendChild(new MyRadioConfig("Tgl Genap"));
		jenis.appendChild(new MyRadioConfig("2 Harian"));
		jenis.appendChild(new MyRadioConfig("3 Harian"));
		jenis.appendChild(new MyRadioConfig("4 Harian"));
		jenis.appendChild(new MyRadioConfig("5 Harian"));
		jenis.appendChild(new MyRadioConfig("6 Harian"));
		MyRadioConfig minggu;
		jenis.appendChild(minggu = new MyRadioConfig("Mingguan"));
		minggu.setChecked(true);
		jenis.appendChild(new MyRadioConfig("2 Mingguan"));
		jenis.appendChild(new MyRadioConfig("3 Mingguan"));
		jenis.appendChild(new MyRadioConfig("4 Mingguan"));
		jenis.appendChild(new MyRadioConfig("Bulanan"));

		@SuppressWarnings("unchecked")
		List<MyRadioConfig> myRadioConfigs = jenis.getChildren();
		for (MyRadioConfig s : myRadioConfigs) {
			if (perkuliahan != null) {
				if (s.getLabel().equalsIgnoreCase(perkuliahan.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (mahasiswaRequestTugasAkhir != null) {
				if (s.getLabel().equalsIgnoreCase(mahasiswaRequestTugasAkhir.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (skripsi != null) {
				if (s.getLabel().equalsIgnoreCase(skripsi.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (krsMahasiswa != null) {
				if (s.getLabel().equalsIgnoreCase(krsMahasiswa.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (kelompokPkl != null) {
				if (s.getLabel().equalsIgnoreCase(kelompokPkl.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (formulirKegiatan != null) {
				if (s.getLabel().equalsIgnoreCase(formulirKegiatan.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (wisuda != null) {
				if (s.getLabel().equalsIgnoreCase(wisuda.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
			if (kelompokKkn != null) {
				if (s.getLabel().equalsIgnoreCase(kelompokKkn.getJenis())) {
					s.setChecked(true);
					break;
				}
			}
		}

		if (mahasiswaRequestTugasAkhir != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
			lewatiTanggalMerahNasional.setChecked(mahasiswaRequestTugasAkhir.getLewatiTanggalMerahNasional());
		}
		if (skripsi != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(skripsi.getTanggalSidang());
			lewatiTanggalMerahNasional.setChecked(skripsi.getLewatiTanggalMerahNasional());
		}
		if (krsMahasiswa != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(krsMahasiswa.getTanggalAwalBimbingan());
			lewatiTanggalMerahNasional.setChecked(krsMahasiswa.getLewatiTanggalMerahNasional());
		}
		if (kelompokPkl != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(kelompokPkl.getTanggal_mulai());
			lewatiTanggalMerahNasional.setChecked(kelompokPkl.getLewatiTanggalMerahNasional());
		}
		if (formulirKegiatan != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(formulirKegiatan.getMulai());
			lewatiTanggalMerahNasional.setChecked(formulirKegiatan.getLewatiTanggalMerahNasional());
		}
		if (wisuda != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(wisuda.getTanggal());
			lewatiTanggalMerahNasional.setChecked(wisuda.getLewatiTanggalMerahNasional());
		}
		if (kelompokKkn != null) {
			if (tanggalMulai != null) tanggalMulai.setValue(kelompokKkn.getTanggal_mulai());
			lewatiTanggalMerahNasional.setChecked(kelompokKkn.getLewatiTanggalMerahNasional());
		}

		// --- Pratinjau daftar tanggal (live) ---
		MyFormRow rowPratinjau = new MyFormRow();
		rowPratinjau.setValign("top");
		rowPratinjau.setParent(rows);
		rowPratinjau.appendChild(new ais.ui.util.MyLabelConfig("Pratinjau Tanggal"));
		final org.zkoss.zul.Div pratinjauBox = new org.zkoss.zul.Div();
		pratinjauBox.setStyle("max-height:300px;overflow:auto;border:1px solid #e2e8f0;border-radius:8px;"
				+ "padding:6px 10px;background:#f8fafc;");
		rowPratinjau.appendChild(pratinjauBox);

		final EventListener refreshPratinjau = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				try {
					Common.clear(pratinjauBox);
					if (tanggalMulai.getValue() == null || jenis.getSelectedItem() == null) {
						pratinjauBox.appendChild(new ais.ui.util.MyHtml(
								"<div style='color:#94a3b8;'>Pilih Tanggal Mulai &amp; Jenis interval untuk melihat pratinjau.</div>"));
						return;
					}
					java.util.List<Pertemuan> daftar = daftarPertemuanPratinjau(perkuliahan, kelompokKkn, kelompokPkl,
							formulirKegiatan, wisuda, skripsi, mahasiswaRequestTugasAkhir, krsMahasiswa);
					if (daftar == null || daftar.isEmpty()) {
						pratinjauBox.appendChild(new ais.ui.util.MyHtml(
								"<div style='color:#94a3b8;'>Belum ada pertemuan untuk dijadwalkan.</div>"));
						return;
					}
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, dd-MM-yyyy",
							new java.util.Locale("id"));
					java.util.Calendar cal = java.util.Calendar.getInstance();
					cal.setTime(tanggalMulai.getValue());
					boolean skip = lewatiTanggalMerahNasional.isChecked();
					StringBuilder sb = new StringBuilder();
					sb.append("<div style='font-size:11px;color:#334155;margin-bottom:4px;'>")
							.append(daftar.size()).append(" pertemuan &middot; ")
							.append(ais.ui.util.DashboardUiKit.esc(jenis.getSelectedItem().getLabel()))
							.append(skip ? " &middot; lewati hari libur" : " &middot; TANPA lewati libur").append("</div>");
					int no = 0;
					for (Pertemuan p : daftar) {
						no++;
						if (skip) {
							cal = Common.tanggalMerahAja(jenis, cal);
						}
						java.util.Date d = cal.getTime();
						boolean libur = false;
						try {
							libur = Common.isHolidayMerahDanAtauHariLibur(d);
						} catch (Exception e) {
							libur = false;
						}
						int ke = p.getPertemuanKe() == null ? no : p.getPertemuanKe();
						String topik = p.getTopik() == null ? "" : p.getTopik().trim();
						if (topik.length() > 40) {
							topik = topik.substring(0, 40) + "…";
						}
						sb.append("<div style='padding:3px 0;border-bottom:1px dashed #e2e8f0;'>")
								.append("<b>Pertemuan ke-").append(ke).append("</b> : ")
								.append("<span style='color:").append(libur ? "#dc2626" : "#0f172a").append(";'>")
								.append(sdf.format(d)).append(libur ? " (libur)" : "").append("</span>");
						if (topik.length() > 0) {
							sb.append(" <span style='color:#64748b;font-size:11px;'>— ")
									.append(ais.ui.util.DashboardUiKit.esc(topik)).append("</span>");
						}
						sb.append("</div>");
						cal = Common.curreDate(jenis, cal);
					}
					pratinjauBox.appendChild(new ais.ui.util.MyHtml(sb.toString()));
				} catch (Exception e) {
					pratinjauBox.appendChild(new ais.ui.util.MyHtml(
							"<div style='color:#dc2626;'>Gagal membuat pratinjau: "
									+ ais.ui.util.DashboardUiKit.esc(pesanError(e)) + "</div>"));
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenjadwalanHelper.pratinjauTanggal");
				}
			}
		};
		tanggalMulai.addEventListener("onChange", refreshPratinjau);
		jenis.addEventListener("onCheck", refreshPratinjau);
		lewatiTanggalMerahNasional.addEventListener("onCheck", refreshPratinjau);
		try {
			refreshPratinjau.onEvent(null);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenjadwalanHelper.pratinjau-init");
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (tanggalMulai.getValue() == null) {
					MyMessageboxConfig.show("Tanggal Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();

				if (perkuliahan != null) {

					if (waktuMulai.getValue() != null) {
						perkuliahan.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuMulai.getValue()));
					}

					if (waktuSelesai.getValue() != null) {
						perkuliahan.setWaktuSelesai(!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuSelesai.getValue()));
					}
					perkuliahan.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					perkuliahan.setTanggalMulaiPerkuliahan(tanggalMulai.getValue());
					perkuliahan.setJenis(jenis.getSelectedItem().getLabel());
					perkuliahan.setBolehMenentukanTanggalMulaiPerkuliahan(
							bolehMenentukanTanggalMulaiPerkuliahan.isChecked());
					Common.refreshUpdate(session, perkuliahan);
				}
				if (mahasiswaRequestTugasAkhir != null) {
					mahasiswaRequestTugasAkhir.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					mahasiswaRequestTugasAkhir.setTanggalAwalBimbingan(tanggalMulai.getValue());
					mahasiswaRequestTugasAkhir.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, mahasiswaRequestTugasAkhir);
				}
				if (skripsi != null) {
					skripsi.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					skripsi.setTanggalSidang(tanggalMulai.getValue());
					skripsi.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, skripsi);
				}
				if (krsMahasiswa != null) {
					krsMahasiswa.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					krsMahasiswa.setTanggalAwalBimbingan(tanggalMulai.getValue());
					krsMahasiswa.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, skripsi);
				}
				if (kelompokPkl != null) {
					kelompokPkl.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					kelompokPkl.setTanggal_mulai(tanggalMulai.getValue());
					kelompokPkl.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, kelompokPkl);
				}
				if (formulirKegiatan != null) {
					formulirKegiatan.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					formulirKegiatan.setMulai(tanggalMulai.getValue());
					formulirKegiatan.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, formulirKegiatan);
				}
				if (wisuda != null) {
					wisuda.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					wisuda.setTanggal(tanggalMulai.getValue());
					wisuda.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, wisuda);
				}
				if (kelompokKkn != null) {
					kelompokKkn.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					kelompokKkn.setTanggal_mulai(tanggalMulai.getValue());
					kelompokKkn.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(session, kelompokKkn);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Date curr = tanggalMulai.getValue();
						System.out.println("Ubah currDate -> " + curr);

						Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
						myCalendar.setTime(curr);

						TreeMap<String, Long> pertemuans = new TreeMap<String, Long>();
						if (perkuliahan != null) {
							pertemuans = perkuliahan.ambilPertemuan();
							perkuliahan.belum();
						}
						if (kelompokKkn != null) {
							pertemuans = kelompokKkn.ambilPertemuan();
							kelompokKkn.belum();
						}
						if (kelompokPkl != null) {
							pertemuans = kelompokPkl.ambilPertemuan();
							kelompokPkl.belum();
						}
						if (formulirKegiatan != null) {
							pertemuans = formulirKegiatan.ambilPertemuan();
							formulirKegiatan.belum();
						}
						if (wisuda != null) {
							pertemuans = wisuda.ambilPertemuan();
							wisuda.belum();
						}
						if (skripsi != null) {
							pertemuans = skripsi.ambilPertemuan();
							skripsi.belum();
						}
						if (mahasiswaRequestTugasAkhir != null) {
							pertemuans = mahasiswaRequestTugasAkhir.ambilPertemuan();
							mahasiswaRequestTugasAkhir.belum();
						}
						if (krsMahasiswa != null) {
							pertemuans = krsMahasiswa.ambilPertemuan();
							krsMahasiswa.belum();
						}
						System.out.println("Ubah pertemuans -> " + pertemuans.size());

						for (Long pertemuanId : pertemuans.values()) {

							try {
								Session session = HibernateUtil.currentNativeSession();

								Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
										.add(Restrictions.idEq(pertemuanId)).uniqueResult();
								if (pertemuan != null) {
									System.out.println("Ubah pertemuan -> " + pertemuan.toString());
									if (lewatiTanggalMerahNasional.isChecked()) {

										myCalendar = Common.tanggalMerahAja(jenis, myCalendar);

									}
									Date currDate = myCalendar.getTime();
									System.out.println(
											"currDate pertemuan -> " + Common.dateFormat6.get().format(currDate));
									pertemuan.setTanggalEdit(currDate);
									pertemuan.setTanggal(currDate);

									pertemuan.setMulai(currDate);
									pertemuan.setSelesai(null);

									if (waktuMulai.getValue() != null) {
										pertemuan.setWaktuMulai(
												!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
														: Common.timeFormat2.get().format(waktuMulai.getValue()));
									}
									if (waktuSelesai.getValue() != null) {
										pertemuan.setWaktuSelesai(
												!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
														: Common.timeFormat2.get().format(waktuSelesai.getValue()));
									}

									session.getTransaction().begin();
									Common.refreshUpdate(session, pertemuan);
									session.getTransaction().commit();

									GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
									myCalendar = Common.curreDate(jenis, myCalendar);
								}
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:1466");
							}
							HibernateUtil.closeSession();

						}

						window.detach();

						Common.createDefaultTimer(eventListener);
					}
				});
			}
		});
		save.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Mengambil daftar pertemuan AKTIF milik satu pemilik agenda, terurut {@code pertemuanKe}→
	 * {@code tanggal}→{@code id}, khusus untuk dirender di panel pratinjau tanggal pada
	 * {@link #prosesTampilTombolAturUlangWaktu} (bukan untuk tampilan grid utama). Gagal dengan tenang:
	 * mengembalikan list kosong dan mencatat error ke {@code ErrorAuditUtil} bila query gagal.
	 */
	@SuppressWarnings("unchecked")
	private static java.util.List<Pertemuan> daftarPertemuanPratinjau(Perkuliahan perkuliahan, KelompokKkn kelompokKkn,
			KelompokPkl kelompokPkl, FormulirKegiatan formulirKegiatan, Wisuda wisuda, Skripsi skripsi,
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, KrsMahasiswa krsMahasiswa) {
		try {
			Session session = HibernateUtil.currentSession();
			return session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(perkuliahan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan", perkuliahan))
					.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("kelompokKkn", kelompokKkn))
					.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("kelompokPkl", kelompokPkl))
					.add(formulirKegiatan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("formulirKegiatan", formulirKegiatan))
					.add(wisuda == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("wisuda", wisuda))
					.add(skripsi == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("skripsi", skripsi))
					.add(mahasiswaRequestTugasAkhir == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswaRequestTugasAkhir", mahasiswaRequestTugasAkhir))
					.add(krsMahasiswa == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("krsMahasiswa", krsMahasiswa))
					.addOrder(Order.asc("pertemuanKe")).addOrder(Order.asc("tanggal")).addOrder(Order.asc("id"))
					.list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenjadwalanHelper.daftarPertemuanPratinjau");
			return new java.util.ArrayList<Pertemuan>();
		}
	}

	/**
	 * Menambahkan tombol "Ubah Tanggal Agenda" ke {@code toolbar} yang, saat diklik, membuka jendela
	 * pengaturan ulang tanggal massal lewat {@link #prosesTampilTombolAturUlangWaktu}.
	 *
	 * @return tombol yang baru dibuat dan sudah ditambahkan ke {@code toolbar}
	 */
	public static MyToolbarbuttonConfig tampilTombolAturUlangWaktu(Component toolbar, final Perkuliahan perkuliahan,
			final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Skripsi skripsi,
			final KrsMahasiswa krsMahasiswa, final FormulirKegiatan formulirKegiatan, final Wisuda wisuda,
			final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah Tanggal Agenda", "/img/svg/edit-box-line.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				prosesTampilTombolAturUlangWaktu(perkuliahan, kelompokKkn, kelompokPkl, mahasiswaRequestTugasAkhir,
						skripsi, krsMahasiswa, formulirKegiatan, wisuda, eventListener);

			}
		});
		button.setParent(toolbar);
		return button;
	}

	/**
	 * Menambahkan tombol "Buat Pertemuan" ke {@code toolbar} khusus untuk {@link Perkuliahan} (satu-
	 * satunya varian penjadwalan di kelas ini yang tidak menerima ke-7 tipe pemilik lain), yang saat
	 * diklik membuka jendela form RPS lengkap: pendahuluan, deskripsi pembelajaran, capaian/kompetensi,
	 * lampiran RPS/SAP/absen manual/soal UTS-UAS/lampiran lain sesuai konfigurasi yang aktif, tanggal
	 * mulai perkuliahan, jam mulai/selesai, jenis interval (harian s.d. bulanan), opsi lewati tanggal
	 * merah, opsi "jumlah rencana pertemuan mengikuti kurikulum" (bila dicentang,
	 * {@code jumlahMaksimalPertemuan} disimpan {@code null} agar selalu dibaca dari kurikulum), batas
	 * minimal persen kehadiran, pengaturan absen online (dosen/mahasiswa, toleransi menit sebelum/sesudah
	 * jadwal), penanda UTS di pertengahan dan UAS di akhir, serta opsi menghapus pertemuan lama sebelum
	 * membuat yang baru (divalidasi dulu lewat {@link #bolehHapus}).
	 *
	 * <p>Saat "Simpan": entity {@code perkuliahan} diperbarui, RPS/lampiran kurikulum disalin ulang
	 * ({@code MatakuliahKurikulumDetailHelper.copyLampiran}), lalu {@link #buatPertemuan} dipanggil
	 * berulang untuk setiap nomor 1..{@code jumlahMaksimalPertemuan}, memajukan kalender sesuai interval
	 * dan melompati tanggal merah bila diminta.</p>
	 *
	 * @param eventListener dipanggil setelah seluruh pertemuan selesai dibuat
	 * @return listener {@code onClick} tombol tersebut (dikembalikan agar bisa dipicu ulang programatis)
	 */
	public static EventListener tampilTombolBuatPertemuan(Component toolbar, final Perkuliahan perkuliahan,
			final EventListener eventListener) {
		EventListener buatPertemuan;
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Buat Pertemuan", "/img/new.gif");
		button.addEventListener("onClick", buatPertemuan = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Window window = new Window();
				window.setHeight("95%");
				window.setWidth("90%");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Perkuliahan"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(perkuliahan.info()));

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Pendahuluan"));
				final MyCkEditor pendahuluan;
				row.appendChild(pendahuluan = new MyCkEditor());
				pendahuluan.setValue(perkuliahan.getPendahuluan());
				pendahuluan.setHeight("200px");
				pendahuluan.setWidth("90%");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Pembelajaran"));
				final MyTextbox deskripsiPembelajaran;
				row.appendChild(deskripsiPembelajaran = new MyTextbox(perkuliahan.getDeskripsiPembelajaran()));
				deskripsiPembelajaran.setRows(3);
				deskripsiPembelajaran.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk matakuliah Sistem Multimedia : Tujuan utama dari mata kuliah ini adalah membekali mahasiswa dengan berbagai kemampuan dalam membangun sistem multimedia melalui pemahaman akan konsep dari sub-sistem penyusunnya........");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Capaian / Kompetensi"));
				final MyTextbox kompetensi;
				row.appendChild(kompetensi = new MyTextbox(perkuliahan.getCapaianPembelajaranProdi()));
				kompetensi.setRows(2);
				kompetensi.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk matakuliah Sistem Multimedia : Mahasiswa memiliki pemahaman mengenai konsep dasar multimedia dan komponen pembentuk sistem multimedia........");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
				Hbox hbox1;
				if (Common.bolehKonfigurasi("tampilkan_rps")) {
					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));

					hbox1 = new Hbox();
					hbox1.setParent(row);
					LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), LampiranLain.SILABUS, "RPS",
							false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true, null, false, false);
				}

				if (Common.bolehKonfigurasi("tampilkan_sap")) {
					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));

					hbox1 = new Hbox();
					hbox1.setParent(row);
					LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), LampiranLain.SAP, "SAP",
							false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true, null, false, false);
				}

				if (Common.bolehKonfigurasi("tampilkan_absen_manual")) {
					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));

					hbox1 = new Hbox();
					hbox1.setParent(row);
					LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), "Absen Manual",
							"Absen Manual", false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true, null, false, false);
				}
				if (Common.bolehKonfigurasi("tampilkan_soal_uts")) {

					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));

					hbox1 = new Hbox();
					hbox1.setParent(row);
					LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), "Soal UTS", "Soal UTS", false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true, null, false, false);

				}

				if (Common.bolehKonfigurasi("tampilkan_soal_uas")) {

					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));

					hbox1 = new Hbox();
					hbox1.setParent(row);
					LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), "Soal UAS", "Soal UAS", false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, null, false, false, false, true, null, false, false);
				}

				for (String t : AktifitasPerkuliahanHelper.lampiranLain) {

					if (Common.bolehKonfigurasi("tampilkan_" + t, Konfigurasi.TIDAK_AKTIF)) {
						row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig(""));

						hbox1 = new Hbox();
						hbox1.setParent(row);
						LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), t, t, false,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, null, false, false, false, true, null, false, false);
					}
				}

				String tampilkan_lampiran_lain_di_agenda = Common
						.getKonfigurasi("tampilkan_lampiran_lain_di_agenda", "").getNilai();
				if (tampilkan_lampiran_lain_di_agenda != null && !tampilkan_lampiran_lain_di_agenda.trim().isEmpty()) {
					for (String s : tampilkan_lampiran_lain_di_agenda.split(",")) {
						row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig(""));

						hbox1 = new Hbox();
						hbox1.setParent(row);
						LampiranLain.createDownloadUploadFileLain(hbox1, perkuliahan.getId(), s, s, false,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, null, false, false, false, true, null, false, false);
					}
				}

//				Common.initKeterangan(rows,
//						"Berupa file silabus atau rencana pembelajaran kuliah, file ini tidak harus diupload, namun sangat dianjurkan diupload, sehingga semua mahasiswa yang mengikuti perkuliahan dapat melihat silabus atau rencana pembelajaran selama satu semester");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Perkuliahan *"));
				final MyDatebox tanggalMulai;
				row.appendChild(tanggalMulai = new MyDatebox(perkuliahan.getTanggalMulaiPerkuliahan()));

				Common.initKeterangan(rows, "Berupa tanggal pertemuan awal perkuliahan");

				final MyCheckboxConfig bolehMenentukanTanggalMulaiPerkuliahan = new MyCheckboxConfig(
						"Bedakan Tanggal Mulai Perkuliahan");
				bolehMenentukanTanggalMulaiPerkuliahan.setChecked(
						perkuliahan == null ? false : perkuliahan.getBolehMenentukanTanggalMulaiPerkuliahan());

				if ((perkuliahan.getMasaPerkuliahan() != null && perkuliahan.getMasaPerkuliahan().getMulai() != null
						&& perkuliahan.getMasaPerkuliahan().getTanggalMulaiHarusSesuaiJadwal())
						|| perkuliahan.getAwalPerkuliahan() != null) {

					if (tanggalMulai.getValue() != null) {
						if (tanggalMulai != null) tanggalMulai.setDisabled(!perkuliahan.getBolehMenentukanTanggalMulaiPerkuliahan());
					}
					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));

					row.appendChild(bolehMenentukanTanggalMulaiPerkuliahan);
					bolehMenentukanTanggalMulaiPerkuliahan.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							perkuliahan.setBolehMenentukanTanggalMulaiPerkuliahan(
									bolehMenentukanTanggalMulaiPerkuliahan.isChecked());
							Common.refreshUpdate(perkuliahan);
							if (tanggalMulai.getValue() != null) {
								if (tanggalMulai != null) tanggalMulai.setDisabled(!perkuliahan.getBolehMenentukanTanggalMulaiPerkuliahan());
							}
						}
					});
				}

				Date dateMulai = null;
				Date dateSelesai = null;
				try {
					if (perkuliahan != null)
						if ((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) != null
								&& !(perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()).equals(""))
							dateMulai = Common.timeFormat2.get()
									.parse((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				try {
					if (perkuliahan != null)
						if ((perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()) != null
								&& !(perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai())
										.equals(""))
							dateSelesai = Common.timeFormat2.get().parse(
									(perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				final MyFormRow rowWaktu = new MyFormRow();
				rowWaktu.setStyle("border:0px;background: transparent;");
				rowWaktu.setParent(rows);
				rowWaktu.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
				Hbox hbox = new Hbox();
				rowWaktu.appendChild(hbox);
				final MyTimebox waktuMulai;
				hbox.appendChild(waktuMulai = new MyTimebox(dateMulai));
				hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
				final MyTimebox waktuSelesai;
				hbox.appendChild(waktuSelesai = new MyTimebox(dateSelesai));

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig lewatiTanggalMerahNasional = new MyCheckboxConfig(
						"Lewati tanggal merah / hari libur");
				lewatiTanggalMerahNasional.setChecked(perkuliahan.getLewatiTanggalMerahNasional());
				row.appendChild(lewatiTanggalMerahNasional);

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
				final Radiogroup jenis;
				row.appendChild(jenis = new Radiogroup());

				jenis.appendChild(new MyRadioConfig("Harian"));
				jenis.appendChild(new MyRadioConfig("Tgl Ganjil"));
				jenis.appendChild(new MyRadioConfig("Tgl Genap"));
				jenis.appendChild(new MyRadioConfig("2 Harian"));
				jenis.appendChild(new MyRadioConfig("3 Harian"));
				jenis.appendChild(new MyRadioConfig("4 Harian"));
				jenis.appendChild(new MyRadioConfig("5 Harian"));
				jenis.appendChild(new MyRadioConfig("6 Harian"));
				MyRadioConfig minggu;
				jenis.appendChild(minggu = new MyRadioConfig("Mingguan"));
				minggu.setChecked(true);
				jenis.appendChild(new MyRadioConfig("2 Mingguan"));
				jenis.appendChild(new MyRadioConfig("3 Mingguan"));
				jenis.appendChild(new MyRadioConfig("4 Mingguan"));
				jenis.appendChild(new MyRadioConfig("Bulanan"));

				@SuppressWarnings("unchecked")
				List<MyRadioConfig> myRadioConfigs = jenis.getChildren();
				for (MyRadioConfig s : myRadioConfigs) {
					if (s.getLabel().equalsIgnoreCase(perkuliahan.getJenis())) {
						s.setChecked(true);
						break;
					}
				}

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig());
				final MyCheckboxConfig jumlahRencanaPertemuanMengikutiKurikulum;
				row.appendChild(jumlahRencanaPertemuanMengikutiKurikulum = new MyCheckboxConfig(
						"Jumlah Rencana Pertemuan Mengikuti Kurikulum"));
				jumlahRencanaPertemuanMengikutiKurikulum
						.setChecked(perkuliahan.getJumlahRencanaPertemuanMengikutiKurikulum());

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pertemuan"));
				final MyIntbox jumlahMaksimalPertemuan;
				row.appendChild(jumlahMaksimalPertemuan = new MyIntbox(perkuliahan.getJumlahMaksimalPertemuan()));
				jumlahMaksimalPertemuan.setDisabled(jumlahRencanaPertemuanMengikutiKurikulum.isChecked());

				jumlahRencanaPertemuanMengikutiKurikulum.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						jumlahMaksimalPertemuan.setDisabled(jumlahRencanaPertemuanMengikutiKurikulum.isChecked());
					}
				});

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Batas Minimal Persen Kehadiran"));
				final MyDoublebox persenKehadiranDinilai0;
				row.appendChild(persenKehadiranDinilai0 = new MyDoublebox(perkuliahan.getPersenKehadiranDinilai0()));

				Common.initKeterangan(rows, "Isikan nilai 0 jika tidak ada batasan kehadiran mahasiswa");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig());
				final MyCheckboxConfig dosenBolehAbsenMenggunakanFoto;
				row.appendChild(
						dosenBolehAbsenMenggunakanFoto = new MyCheckboxConfig("Dosen Diizinkan / Boleh Absen Online"));
				dosenBolehAbsenMenggunakanFoto.setChecked(perkuliahan.getDosenBolehAbsenMenggunakanFoto());

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig());
				final MyCheckboxConfig mahasiswaBolehAbsenMenggunakanFoto;
				row.appendChild(mahasiswaBolehAbsenMenggunakanFoto = new MyCheckboxConfig(
						"Mahasiswa Diizinkan / Boleh Absen Online"));
				mahasiswaBolehAbsenMenggunakanFoto.setChecked(perkuliahan.getMahasiswaBolehAbsenMenggunakanFoto());

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig());
				final MyCheckboxConfig bolehAbsenWaktuIkutiPerkuliahan;
				row.appendChild(bolehAbsenWaktuIkutiPerkuliahan = new MyCheckboxConfig(
						"Batas waktu toleransi absen harus mengikuti perkuliahan"));
				bolehAbsenWaktuIkutiPerkuliahan.setChecked(perkuliahan.getBolehAbsenWaktuIkutiPerkuliahan());

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Menit toleransi absensi online sebelum perkuliahan"));
				final MyIntbox bolehAbsenSebelumWaktuMulaiDalamMenit;
				row.appendChild(bolehAbsenSebelumWaktuMulaiDalamMenit = new MyIntbox(
						perkuliahan.getBolehAbsenSebelumWaktuMulaiDalamMenit()));
				bolehAbsenSebelumWaktuMulaiDalamMenit.setDisabled(!bolehAbsenWaktuIkutiPerkuliahan.isChecked());

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Menit toleransi absensi online setelah perkuliahan"));
				final MyIntbox bolehAbsenSetelahWaktuMulaiDalamMenit;
				row.appendChild(bolehAbsenSetelahWaktuMulaiDalamMenit = new MyIntbox(
						perkuliahan.getBolehAbsenSetelahWaktuMulaiDalamMenit()));
				bolehAbsenSetelahWaktuMulaiDalamMenit.setDisabled(!bolehAbsenWaktuIkutiPerkuliahan.isChecked());

				bolehAbsenWaktuIkutiPerkuliahan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bolehAbsenSebelumWaktuMulaiDalamMenit.setDisabled(!bolehAbsenWaktuIkutiPerkuliahan.isChecked());
						bolehAbsenSetelahWaktuMulaiDalamMenit.setDisabled(!bolehAbsenWaktuIkutiPerkuliahan.isChecked());
					}
				});

				row = new MyFormRow();
				row.setVisible(Common.getKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF)
						.getNilai().equals(Konfigurasi.TIDAK_AKTIF));
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Batas hari boleh melakukan presensi kehadiran"));
				final MyIntbox batasWaktuBolehAbsenKehadiran;
				row.appendChild(
						batasWaktuBolehAbsenKehadiran = new MyIntbox(perkuliahan.getBatasWaktuBolehAbsenKehadiran()));

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UTS"));
				final MyCheckboxConfig uts;
				row.appendChild(uts = new MyCheckboxConfig("Di pertengahan pertemuan merupakan jadwal UTS"));
				uts.setChecked(true);

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UAS"));
				final MyCheckboxConfig uas;
				row.appendChild(uas = new MyCheckboxConfig("Di akhir pertemuan merupakan jadwal UAS"));
				uas.setChecked(true);

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Hapus Pertemuan"));
				final MyCheckboxConfig hapus;
				row.appendChild(hapus = new MyCheckboxConfig("Hapus pertemuan yang sebelumnya sudah ada"));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (hapus.isChecked()) {
							if (!bolehHapus(perkuliahan, null, null, null, null, null, null, null)) {
								return;
							}
						}

						if (tanggalMulai.getValue() == null) {
							MyMessageboxConfig.show("Tanggal Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						perkuliahan.belum();

						Session session = HibernateUtil.currentSession();
						session.refresh(perkuliahan);
						perkuliahan.setBolehMenentukanTanggalMulaiPerkuliahan(
								bolehMenentukanTanggalMulaiPerkuliahan.isChecked());
						perkuliahan.setJumlahRencanaPertemuanMengikutiKurikulum(
								jumlahRencanaPertemuanMengikutiKurikulum.isChecked());
						// Bila "ikuti kurikulum" dicentang, simpan null agar getJumlahMaksimalPertemuan()
						// selalu membaca dari kurikulum; jika tidak, gunakan nilai yang diisi manual.
						if (jumlahRencanaPertemuanMengikutiKurikulum.isChecked()) {
							perkuliahan.setJumlahMaksimalPertemuan(null);
						} else {
							perkuliahan.setJumlahMaksimalPertemuan(jumlahMaksimalPertemuan.getValue());
						}
						perkuliahan.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
						perkuliahan.setTanggalMulaiPerkuliahan(tanggalMulai.getValue());
						perkuliahan.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
						perkuliahan.setPendahuluan(pendahuluan.getValue());
						perkuliahan.setCapaianPembelajaranProdi(kompetensi.getValue());
						perkuliahan.setJenis(jenis.getSelectedItem().getLabel());
						perkuliahan.setPersenKehadiranDinilai0(persenKehadiranDinilai0.getValue());

						perkuliahan
								.setMahasiswaBolehAbsenMenggunakanFoto(mahasiswaBolehAbsenMenggunakanFoto.isChecked());
						perkuliahan.setDosenBolehAbsenMenggunakanFoto(dosenBolehAbsenMenggunakanFoto.isChecked());

						perkuliahan.setBolehAbsenWaktuIkutiPerkuliahan(bolehAbsenWaktuIkutiPerkuliahan.isChecked());

						perkuliahan.setBolehAbsenSebelumWaktuMulaiDalamMenit(
								bolehAbsenSebelumWaktuMulaiDalamMenit.getValue());
						perkuliahan.setBolehAbsenSetelahWaktuMulaiDalamMenit(
								bolehAbsenSetelahWaktuMulaiDalamMenit.getValue());

						perkuliahan.setBatasWaktuBolehAbsenKehadiran(batasWaktuBolehAbsenKehadiran.getValue());

						if (waktuMulai.getValue() != null) {
							perkuliahan.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
									: Common.timeFormat2.get().format(waktuMulai.getValue()));
						}

						if (waktuSelesai.getValue() != null) {
							perkuliahan
									.setWaktuSelesai(!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
											: Common.timeFormat2.get().format(waktuSelesai.getValue()));
						}

						Common.refreshUpdate(session, perkuliahan);

						if (hapus.isChecked()) {
							session.createSQLQuery("delete from pertemuan where perkuliahan=" + perkuliahan.getId())
									.executeUpdate();
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan
										.getKurikulumPunyaMatakuliah();
								if (kurikulumPunyaMatakuliah != null) {
									MatakuliahKurikulumDetailHelper.copyLampiran(kurikulumPunyaMatakuliah, perkuliahan);
								}

								Date curr = tanggalMulai.getValue();
								System.out.println("Ubah currDate -> " + curr);

								Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
								myCalendar.setTime(curr);

								for (int i = 1; i <= perkuliahan.getJumlahMaksimalPertemuan(); i++) {

									if (lewatiTanggalMerahNasional.isChecked()) {
										myCalendar = Common.tanggalMerahAja(jenis, myCalendar);
									}
									Date currDate = myCalendar.getTime();
									System.out.println(
											"currDate pertemuan -> " + Common.dateFormat6.get().format(currDate));
									PenjadwalanHelper.buatPertemuan(perkuliahan, kurikulumPunyaMatakuliah, i, currDate,
											uts, uas, waktuMulai, waktuSelesai);

									myCalendar = Common.curreDate(jenis, myCalendar);
								}
								window.detach();

								Common.createDefaultTimer(eventListener);
							}
						});

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);
		return buatPertemuan;
	}

	/**
	 * Membuat (atau, bila sudah ada pertemuan aktif pada tanggal yang sama, mengembalikan yang sudah ada
	 * tanpa membuat duplikat) satu {@link Pertemuan} untuk nomor urut ke-{@code i} pada tanggal
	 * {@code currDate}, dipanggil berulang oleh {@link #tampilTombolBuatPertemuan} saat men-generate
	 * seluruh agenda satu semester. Topik/status default: "Tatap Muka" untuk pertemuan biasa; bila
	 * {@code i} sama dengan pertemuan terakhir dan {@code uas} dicentang, status/topik/metode diisi UAS;
	 * lihat lanjutan method (di luar cuplikan ini) untuk penanda UTS di pertengahan. Bila tersedia,
	 * topik/indikator/dsb diambil dari {@link KurikulumPunyaMatakuliahDetail} nomor urut ke-{@code i}
	 * pada {@code kurikulumPunyaMatakuliah} milik matakuliah tersebut.
	 *
	 * @param i           nomor urut pertemuan yang akan dibuat (1..jumlah maksimal pertemuan)
	 * @param currDate    tanggal pertemuan tersebut
	 * @param uts         checkbox "UTS di pertengahan pertemuan"
	 * @param uas         checkbox "UAS di akhir pertemuan"
	 * @param waktuMulai  komponen jam mulai perkuliahan (dipakai sebagai jam default pertemuan baru)
	 * @param waktuSelesai komponen jam selesai perkuliahan (dipakai sebagai jam default pertemuan baru)
	 * @return pertemuan yang baru dibuat, atau pertemuan aktif yang sudah ada pada tanggal tersebut;
	 *         {@code null} bila terjadi kegagalan (dicatat ke error audit, tidak dilempar ke pemanggil)
	 */
	public static Pertemuan buatPertemuan(Perkuliahan perkuliahan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			Integer i, Date currDate, MyCheckboxConfig uts, MyCheckboxConfig uas, Timebox waktuMulai,
			Timebox waktuSelesai) {
		Pertemuan pertemuan = null;
		try {
			Session session = HibernateUtil.currentNativeSession();

			pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.eq("tanggal", currDate))
					.setMaxResults(1).uniqueResult();
			if (pertemuan == null) {

				KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail = null;
				if (kurikulumPunyaMatakuliah != null) {
					kurikulumPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) session
							.createCriteria(KurikulumPunyaMatakuliahDetail.class)
							.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
							.add(Restrictions.eq("nomorUrut", i)).setMaxResults(1).uniqueResult();
				}

				pertemuan = new Pertemuan();
				pertemuan.setPertemuanKe(i);

				pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);

				if (uas.isChecked()) {
					if (i == perkuliahan.getJumlahMaksimalPertemuan()) {
						pertemuan.setStatusPertemuan(ConstantValues.UAS);
						pertemuan.setTopik("Pertemuan ke " + i + " : UAS");
						pertemuan.setMetodePembelajaran("Mengerjakan soal UAS");
					}
				}

				if (uts.isChecked()) {
					if (i == (perkuliahan.getJumlahMaksimalPertemuan() / 2)) {
						pertemuan.setStatusPertemuan(ConstantValues.UTS);
						pertemuan.setTopik("Pertemuan ke " + i + " : UTS");
						pertemuan.setMetodePembelajaran("Mengerjakan soal UTS");
					}
				}

				pertemuan.setTanggal(currDate);
				pertemuan.setPerkuliahan(perkuliahan);
				pertemuan.setRuang(perkuliahan.getRuang());
				pertemuan.setWaktuMulai(perkuliahan.getWaktuMulai());
				pertemuan.setWaktuSelesai(perkuliahan.getWaktuSelesai());

				pertemuan.setTanggal(currDate);
				pertemuan.setMulai(currDate);
				pertemuan.setSelesai(null);
				pertemuan.setWaktuMulai(perkuliahan.getWaktuMulai());
				pertemuan.setWaktuSelesai(perkuliahan.getWaktuSelesai());

				if (kurikulumPunyaMatakuliahDetail != null) {
					pertemuan.setTopik(kurikulumPunyaMatakuliahDetail.getTopik());
					pertemuan.setIndikator(kurikulumPunyaMatakuliahDetail.getIndikator());
					pertemuan.setWaktupembelajaran(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran());
					pertemuan.setPengalamanBelajar(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar());
					pertemuan.setTugasDanPenilaian(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian());
					pertemuan.setBukuRujukan1(kurikulumPunyaMatakuliahDetail.getBukuRujukan1());
					pertemuan.setStatusPertemuan(kurikulumPunyaMatakuliahDetail.getStatusPertemuan());
					pertemuan.setPertemuanKe(kurikulumPunyaMatakuliahDetail.getNomorUrut());
					pertemuan.setMetodePembelajaran(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran());
					pertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetail.getId());
					pertemuan.setRuang(perkuliahan.getRuang());
					pertemuan.setWaktuMulai(perkuliahan.getWaktuMulai());
					pertemuan.setWaktuSelesai(perkuliahan.getWaktuSelesai());
				}

				if (waktuMulai != null && waktuMulai.getValue() != null) {
					pertemuan.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
							: Common.timeFormat2.get().format(waktuMulai.getValue()));
				}
				if (waktuSelesai != null && waktuSelesai.getValue() != null) {
					pertemuan.setWaktuSelesai(!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
							: Common.timeFormat2.get().format(waktuSelesai.getValue()));
				}

				session.getTransaction().begin();
				session.save(pertemuan);
				session.getTransaction().commit();
				if (kurikulumPunyaMatakuliahDetail != null) {
					MatakuliahKurikulumDetailHelper.copyLampiran(kurikulumPunyaMatakuliahDetail, pertemuan);
				}
			}

			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenjadwalanHelper.java:2188");
		}

		HibernateUtil.closeSession();

		return pertemuan;
	}

	/**
	 * Menambahkan tombol "Ambil (copy) dari agenda sebelumnya / lain" ke {@code toolbar} — fitur salin
	 * agenda pembelajaran (dan, untuk perkuliahan, seluruh format penilaian OBE) dari perkuliahan/kelompok
	 * lain (mis. semester sebelumnya) ke pemilik agenda saat ini. Membuka
	 * {@link ais.action.master.helper.generic.AmbilDataTemplatePembelajaran} untuk memilih satu atau
	 * banyak sumber, lalu untuk tiap sumber terpilih:
	 * <ol>
	 *   <li>Bila sumber &amp; tujuan sama-sama {@link Perkuliahan}: menyalin deskripsi pembelajaran,
	 *   pendahuluan, dan capaian pembelajaran prodi ke {@code perkuliahan}; menyalin capaian/profil
	 *   lulusan dan bahan kajian ke {@link Matakuliah} tujuan; lalu bila kedua sisi memiliki
	 *   {@link KurikulumPunyaMatakuliah}, menyalin SELURUH struktur/format OBE-nya — minimal
	 *   ketercapaian, {@code nilaiMenggunakanCpmk}, bobot CPL, pemetaan soal UTS/UAS, komponen penilaian,
	 *   teknik per-CPMK, rubrik penilaian, deskripsi pembelajaran, capaian pembelajaran prodi, jumlah
	 *   pertemuan default, dan metadata RPS lain (koordinator, pengembang RPS, tanggal penyusunan, dsb).
	 *   Nilai dan peserta SENGAJA tidak disalin — keduanya tersimpan di kelas/detail penilaian, bukan
	 *   di {@code KurikulumPunyaMatakuliah}.</li>
	 *   <li>Untuk setiap {@link Pertemuan} aktif milik sumber yang belum pernah disalin ke tujuan
	 *   (dideteksi lewat kolom {@code copyDariPertemuan}, agar tombol ini aman diklik berkali-kali tanpa
	 *   membuat duplikat), membuat {@code Pertemuan} baru dengan field RPS disalin (indikator, waktu
	 *   pembelajaran, pengalaman belajar, tugas dan penilaian, topik, ruang, jam, buku rujukan, dsb),
	 *   lalu menyalin lampiran/isi pertemuan lewat {@link #copyLampiranPertemuan(Pertemuan, Pertemuan)}.</li>
	 *   <li>Menyalin relasi item pendukung ({@link PerkuliahanPunyaItem} untuk perkuliahan, atau
	 *   {@link DataPunyaItem} untuk KKN/PKL/tugas akhir/skripsi) yang belum ada di tujuan.</li>
	 * </ol>
	 * Setelah selesai, menyusun dan mengunduh laporan ringkas berformat .txt (jumlah pertemuan
	 * dibuat/dilewati per kategori konten, serta daftar kendala per item bila ada), menampilkan ringkasan
	 * lewat {@link MyMessageboxConfig}, lalu langsung membuka
	 * {@link #prosesTampilTombolAturUlangWaktu} agar pengguna mengatur tanggal mulai &amp; interval
	 * pertemuan yang baru disalin.
	 *
	 * <p><b>Catatan riwayat:</b> tombol ini sempat digerbang khusus ADMINISTRATOR (r75196, 07-07-2026)
	 * sehingga dosen tidak bisa lagi menarik agenda semester sebelumnya sendiri; gerbang itu di-revert
	 * 20-08-2026 atas permintaan pemilik produk — hak akses sekarang murni ditentukan kondisi pada
	 * pemanggil {@code display(...)}.</p>
	 *
	 * @param dataLoader dipanggil setelah proses salin (dan pengaturan ulang tanggal) selesai, agar
	 *                   pemanggil memuat ulang tampilan
	 */
	public static void tampilTombolAmbil(Component toolbar, final Perkuliahan perkuliahan,
			final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Skripsi skripsi,
			final FormulirKegiatan formulirKegiatan, final Wisuda wisuda, final DataLoader dataLoader) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil (copy) dari agenda sebelumnya / lain",
				"/img/svg/edit-copy.svg");
		/* REVERT 20-08-2026 atas permintaan pemilik: r75196 (07-07-2026) menambahkan
		 * button.setVisible(Common.getApakahAdmin()) sehingga tombol ini hanya tampil bagi
		 * ADMINISTRATOR. Gerbang itu menimpa kendali lama pada pemanggil, yaitu kombinasi
		 * tbmuser.getMahasiswa() == null dan perkuliahan.getDosenBisaMerubahTanggalPerkuliahan(),
		 * sehingga dosen tidak pernah lagi bisa menarik agenda semester sebelumnya. Gerbang
		 * admin dihapus; hak akses kembali ditentukan oleh kondisi di masing-masing pemanggil. */
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataTemplatePembelajaran window = new AmbilDataTemplatePembelajaran(perkuliahan, kelompokKkn,
						kelompokPkl, mahasiswaRequestTugasAkhir, skripsi, formulirKegiatan, wisuda);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("97%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<GeneralValueObject> templatePembelajarans = (List<GeneralValueObject>) arg0.getData();

						if (templatePembelajarans != null) {
							Session session = HibernateUtil.currentSession();

							// --- Laporan hasil salin agenda (diunduh .txt setelah selesai) ---
							StringBuilder laporan = new StringBuilder();
							laporan.append("========================================\n");
							laporan.append("   LAPORAN SALIN AGENDA PEMBELAJARAN\n");
							laporan.append("========================================\n");
							laporan.append("Waktu proses : ")
									.append(Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate())).append("\n");
							if (perkuliahan != null) {
								laporan.append("Perkuliahan  : ID ").append(perkuliahan.getId()).append("\n");
							}
							laporan.append("Catatan      : Peserta (mahasiswa/siswa/dosen/guru), kehadiran, dan nilai "
									+ "per-peserta TIDAK ikut disalin.\n");
							laporan.append("----------------------------------------\n\n");
							int totPertemuan = 0;
							int totDilewati = 0;
							int totMateri = 0;
							int totFile = 0;
							int totVideo = 0;
							int totAudio = 0;
							int totUjian = 0;
							int totTgsKel = 0;
							int totTgsMandiri = 0;
							int totKendala = 0;

							for (GeneralValueObject templatePembelajaran : templatePembelajarans) {
								Perkuliahan copyPerkuliahan = null;
								KelompokKkn copyKelompokKkn = null;
								KelompokPkl copyKelompokPkl = null;
								MahasiswaRequestTugasAkhir copyMahasiswaRequestTugasAkhir = null;
								Skripsi copySkripsi = null;
								FormulirKegiatan copyFormulirKegiatan = null;
								Wisuda copyWisuda = null;
								if (perkuliahan != null && templatePembelajaran instanceof Perkuliahan) {
									copyPerkuliahan = (Perkuliahan) templatePembelajaran;
									perkuliahan.setDeskripsiPembelajaran(copyPerkuliahan.getDeskripsiPembelajaran());
									perkuliahan.setPendahuluan(copyPerkuliahan.getPendahuluan());
									perkuliahan
											.setCapaianPembelajaranProdi(copyPerkuliahan.getCapaianPembelajaranProdi());

									Common.refreshSaveOrUpdate(session, perkuliahan);

									Matakuliah matakuliahlama = copyPerkuliahan.getMatakuliah();
									Matakuliah matakuliahBaru = perkuliahan.getMatakuliah();

									matakuliahBaru.setCapaianLulusan(matakuliahlama.getCapaianLulusan());
									matakuliahBaru.setProfilLulusan(matakuliahlama.getProfilLulusan());
									matakuliahBaru.setCapaianPembelajaranLulusan(
											matakuliahlama.getCapaianPembelajaranLulusan());
									matakuliahBaru.setBahanKajian(matakuliahlama.getBahanKajian());
									Common.refreshSaveOrUpdate(session, matakuliahBaru);

									KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahLama = copyPerkuliahan
											.getKurikulumPunyaMatakuliah();
									KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahBaru = perkuliahan
											.getKurikulumPunyaMatakuliah();

									if (kurikulumPunyaMatakuliahLama != null && kurikulumPunyaMatakuliahBaru != null) {
										// Salin SELURUH struktur/format OBE. Nilai dan peserta tetap tidak
										// disalin; keduanya tersimpan pada kelas/detail penilaian, bukan KPM.
										kurikulumPunyaMatakuliahBaru.setMinimalKetercapaian(
												kurikulumPunyaMatakuliahLama.getMinimalKetercapaian());
										kurikulumPunyaMatakuliahBaru.setNilaiMenggunakanCpmk(
												kurikulumPunyaMatakuliahLama.getNilaiMenggunakanCpmk());
										kurikulumPunyaMatakuliahBaru.setCplBobot(kurikulumPunyaMatakuliahLama.getCplBobot());
										kurikulumPunyaMatakuliahBaru.setPemetaanSoalUts(
												kurikulumPunyaMatakuliahLama.getPemetaanSoalUts());
										kurikulumPunyaMatakuliahBaru.setPemetaanSoalUas(
												kurikulumPunyaMatakuliahLama.getPemetaanSoalUas());
										kurikulumPunyaMatakuliahBaru.setKomponenPenilaian(
												kurikulumPunyaMatakuliahLama.getKomponenPenilaian());
										kurikulumPunyaMatakuliahBaru.setTeknikPerCpmk(
												kurikulumPunyaMatakuliahLama.getTeknikPerCpmk());
										kurikulumPunyaMatakuliahBaru.setRubrikPenilaian(
												kurikulumPunyaMatakuliahLama.getRubrikPenilaian());
										kurikulumPunyaMatakuliahBaru.setDeskripsiPembelajaran(
												kurikulumPunyaMatakuliahLama.getDeskripsiPembelajaran());
										kurikulumPunyaMatakuliahBaru.setCapaianPembelajaranProdi(
												kurikulumPunyaMatakuliahLama.getCapaianPembelajaranProdi());
										kurikulumPunyaMatakuliahBaru.setJumlahPertemuanPerkuliahanDefault(
												kurikulumPunyaMatakuliahLama.getJumlahPertemuanPerkuliahanDefault());
										kurikulumPunyaMatakuliahBaru.setTerdapatTugas(
												kurikulumPunyaMatakuliahLama.getTerdapatTugas());
										kurikulumPunyaMatakuliahBaru
												.setCatatan(kurikulumPunyaMatakuliahLama.getCatatan());
										kurikulumPunyaMatakuliahBaru
												.setRincian(kurikulumPunyaMatakuliahLama.getRincian());
										kurikulumPunyaMatakuliahBaru
												.setMkPrasyarat(kurikulumPunyaMatakuliahLama.getMkPrasyarat());
										kurikulumPunyaMatakuliahBaru
												.setMitraPengembang(kurikulumPunyaMatakuliahLama.getMitraPengembang());
										kurikulumPunyaMatakuliahBaru.setDosen(kurikulumPunyaMatakuliahLama.getDosen());
										kurikulumPunyaMatakuliahBaru.setPustakaPendukung(
												kurikulumPunyaMatakuliahLama.getPustakaPendukung());
										kurikulumPunyaMatakuliahBaru
												.setPustaka(kurikulumPunyaMatakuliahLama.getPustaka());
										kurikulumPunyaMatakuliahBaru
												.setKoordinator(kurikulumPunyaMatakuliahLama.getKoordinator());
										kurikulumPunyaMatakuliahBaru
												.setPengembangRps(kurikulumPunyaMatakuliahLama.getPengembangRps());
										kurikulumPunyaMatakuliahBaru.setTanggalPenyusunan(
												kurikulumPunyaMatakuliahLama.getTanggalPenyusunan());

										Common.refreshSaveOrUpdate(session, kurikulumPunyaMatakuliahBaru);
									}
								}

								if (templatePembelajaran instanceof Perkuliahan) {
									copyPerkuliahan = (Perkuliahan) templatePembelajaran;
								}

								if (templatePembelajaran instanceof KelompokKkn) {
									copyKelompokKkn = (KelompokKkn) templatePembelajaran;
								}

								if (templatePembelajaran instanceof KelompokPkl) {
									copyKelompokPkl = (KelompokPkl) templatePembelajaran;
								}

								if (templatePembelajaran instanceof FormulirKegiatan) {
									copyFormulirKegiatan = (FormulirKegiatan) templatePembelajaran;
								}

								if (templatePembelajaran instanceof Wisuda) {
									copyWisuda = (Wisuda) templatePembelajaran;
								}

								if (templatePembelajaran instanceof MahasiswaRequestTugasAkhir) {
									copyMahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) templatePembelajaran;
								}

								if (templatePembelajaran instanceof Skripsi) {
									copySkripsi = (Skripsi) templatePembelajaran;
								}

								List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))

										.add(copyPerkuliahan == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("perkuliahan", copyPerkuliahan))

										.add(copyKelompokKkn == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("kelompokKkn", copyKelompokKkn))

										.add(copyKelompokPkl == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("kelompokPkl", copyKelompokPkl))

										.add(copyFormulirKegiatan == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("formulirKegiatan", copyFormulirKegiatan))

										.add(copyWisuda == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("wisuda", copyWisuda))

										.add(copySkripsi == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("skripsi", copySkripsi))

										.add(mahasiswaRequestTugasAkhir == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("mahasiswaRequestTugasAkhir",
														mahasiswaRequestTugasAkhir))

										.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list();
								for (Pertemuan pertemuan : pertemuans) {
									Pertemuan pertemuanBaru = (Pertemuan) session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("copyDariPertemuan", pertemuan.getId()))

											.add(perkuliahan == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("perkuliahan", perkuliahan))

											.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("kelompokPkl", kelompokPkl))

											.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("kelompokKkn", kelompokKkn))

											.add(formulirKegiatan == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("formulirKegiatan", formulirKegiatan))

											.add(wisuda == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("wisuda", wisuda))

											.add(skripsi == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("skripsi", skripsi))

											.add(mahasiswaRequestTugasAkhir == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("mahasiswaRequestTugasAkhir",
															mahasiswaRequestTugasAkhir))

											.setMaxResults(1).uniqueResult();
									if (pertemuanBaru == null) {
										pertemuanBaru = new Pertemuan();
										pertemuanBaru.setIndikator(pertemuan.getIndikator());
										pertemuanBaru.setWaktupembelajaran(pertemuan.getWaktupembelajaran());
										pertemuanBaru.setPengalamanBelajar(pertemuan.getPengalamanBelajar());
										pertemuanBaru.setTugasDanPenilaian(pertemuan.getTugasDanPenilaian());
										pertemuanBaru.setMandiri(pertemuan.getMandiri());
										pertemuanBaru.setCopyDariPertemuan(pertemuan.getId());
										pertemuanBaru.setStatusPertemuan(pertemuan.getStatusPertemuan());
										pertemuanBaru.setTanggal(pertemuan.getTanggal());
										pertemuanBaru.setPerkuliahan(perkuliahan);
										pertemuanBaru.setKelompokKkn(kelompokKkn);
										pertemuanBaru.setKelompokPkl(kelompokPkl);
										pertemuanBaru.setFormulirKegiatan(formulirKegiatan);
										pertemuanBaru.setWisuda(wisuda);
										pertemuanBaru.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
										pertemuanBaru.setSkripsi(skripsi);
										pertemuanBaru.setTopik(pertemuan.getTopik());
										pertemuanBaru.setRuang(pertemuan.getRuang());
										pertemuanBaru.setWaktuMulai(pertemuan.getWaktuMulai());
										pertemuanBaru.setWaktuSelesai(pertemuan.getWaktuSelesai());
										pertemuanBaru.setBukuRujukan1(pertemuan.getBukuRujukan1());
										pertemuanBaru.setBukuRujukan2(pertemuan.getBukuRujukan2());
										pertemuanBaru.setCatatan(pertemuan.getCatatan());
										pertemuanBaru.setDosenTamu(pertemuan.getDosenTamu());
										pertemuanBaru.setDosenTamu2(pertemuan.getDosenTamu2());
										pertemuanBaru.setIsitugas(pertemuan.getIsitugas());
										pertemuanBaru.setMetodePembelajaran(pertemuan.getMetodePembelajaran());
										pertemuanBaru.setMulai(pertemuan.getMulai());
										pertemuanBaru.setSelesai(pertemuan.getSelesai());
										pertemuanBaru.setPertemuanKe(pertemuan.getPertemuanKe());

										try {
											Common.refreshSaveOrUpdate(session, pertemuanBaru);

											HasilSalinPertemuan hsl = copyLampiranPertemuan(pertemuan, pertemuanBaru);
											totPertemuan++;
											totMateri += hsl.materi;
											totFile += hsl.file;
											totVideo += hsl.video;
											totAudio += hsl.audio;
											totUjian += hsl.ujian;
											totTgsKel += hsl.tugasKelompok;
											totTgsMandiri += hsl.tugasMandiri;
											laporan.append("Pertemuan ke-").append(pertemuan.getPertemuanKe())
													.append(" (agenda lama #").append(pertemuan.getId())
													.append(" → baru #").append(pertemuanBaru.getId()).append(")\n");
											laporan.append("   materi=").append(hsl.materi).append(", file=")
													.append(hsl.file).append(", video=").append(hsl.video)
													.append(", audio=").append(hsl.audio).append(", ujian=")
													.append(hsl.ujian).append(", tugas kelompok=").append(hsl.tugasKelompok)
													.append(", tugas mandiri=").append(hsl.tugasMandiri).append("\n");
											for (String k : hsl.kendala) {
												laporan.append("   [KENDALA] ").append(k).append("\n");
												totKendala++;
											}
											laporan.append("\n");
										} catch (Exception ePert) {
											totKendala++;
											laporan.append("Pertemuan ke-").append(pertemuan.getPertemuanKe())
													.append(" (agenda lama #").append(pertemuan.getId())
													.append("): GAGAL dibuat — ").append(pesanError(ePert))
													.append("\n\n");
											ais.common.ErrorAuditUtil.record(ePert, "salin-agenda buat-pertemuan");
										}

									} else {
										totDilewati++;
										laporan.append("Pertemuan ke-").append(pertemuan.getPertemuanKe())
												.append(" (agenda lama #").append(pertemuan.getId())
												.append("): sudah pernah disalin — DILEWATI.\n\n");
									}

								}

								if (copyPerkuliahan != null) {
									List<PerkuliahanPunyaItem> perkuliahanPunyaItems = session
											.createCriteria(PerkuliahanPunyaItem.class).addOrder(Order.asc("id"))

											.add(copyPerkuliahan == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("perkuliahan", copyPerkuliahan))

											.list();
									for (PerkuliahanPunyaItem c : perkuliahanPunyaItems) {
										PerkuliahanPunyaItem perkuliahanPunyaItem = (PerkuliahanPunyaItem) session
												.createCriteria(PerkuliahanPunyaItem.class)
												.add(Restrictions.eq("item", c.getItem()))
												.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1)
												.uniqueResult();
										if (perkuliahanPunyaItem == null) {
											perkuliahanPunyaItem = new PerkuliahanPunyaItem();
											perkuliahanPunyaItem.setItem(c.getItem());
											perkuliahanPunyaItem.setPerkuliahan(perkuliahan);
											Common.refreshSaveOrUpdate(session, perkuliahanPunyaItem);
										}

									}
								} else if (copyMahasiswaRequestTugasAkhir != null
										|| copyKelompokKkn != null && copyKelompokPkl != null || copySkripsi != null) {
									List<DataPunyaItem> perkuliahanPunyaItems = session
											.createCriteria(DataPunyaItem.class).addOrder(Order.asc("id"))

											.add(copyMahasiswaRequestTugasAkhir == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("mahasiswaRequestTugasAkhir",
															copyMahasiswaRequestTugasAkhir))
											.add(copyKelompokKkn == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("kelompokKkn", copyKelompokKkn))
											.add(copyKelompokPkl == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("kelompokPkl", copyKelompokPkl))
											.add(copySkripsi == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("skripsi", copySkripsi))

											.list();
									for (DataPunyaItem c : perkuliahanPunyaItems) {
										DataPunyaItem perkuliahanPunyaItem = (DataPunyaItem) session
												.createCriteria(DataPunyaItem.class)
												.add(Restrictions.eq("item", c.getItem()))

												.add(mahasiswaRequestTugasAkhir == null
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("mahasiswaRequestTugasAkhir",
																mahasiswaRequestTugasAkhir))
												.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
														: Restrictions.eq("kelompokKkn", kelompokKkn))
												.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
														: Restrictions.eq("kelompokPkl", kelompokPkl))
												.add(skripsi == null ? Restrictions.sqlRestriction("true")
														: Restrictions.eq("skripsi", skripsi))

												.setMaxResults(1).uniqueResult();
										if (perkuliahanPunyaItem == null) {
											perkuliahanPunyaItem = new DataPunyaItem();
											perkuliahanPunyaItem.setItem(c.getItem());
											perkuliahanPunyaItem.setKelompokKkn(kelompokKkn);
											perkuliahanPunyaItem.setKelompokPkl(kelompokPkl);
											perkuliahanPunyaItem
													.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
											perkuliahanPunyaItem.setSkripsi(skripsi);
											Common.refreshSaveOrUpdate(session, perkuliahanPunyaItem);
										}

									}
								}

							}

							// --- Ringkasan + unduh laporan .txt ---
							laporan.append("========================================\n");
							laporan.append("   RINGKASAN\n");
							laporan.append("========================================\n");
							laporan.append("Pertemuan baru dibuat : ").append(totPertemuan).append("\n");
							laporan.append("Pertemuan dilewati    : ").append(totDilewati)
									.append(" (sudah pernah disalin)\n");
							laporan.append("Materi/catatan         : ").append(totMateri).append("\n");
							laporan.append("File materi            : ").append(totFile).append("\n");
							laporan.append("Video materi           : ").append(totVideo).append("\n");
							laporan.append("Audio materi           : ").append(totAudio).append("\n");
							laporan.append("Ujian                  : ").append(totUjian).append("\n");
							laporan.append("Tugas kelompok         : ").append(totTgsKel).append("\n");
							laporan.append("Tugas mandiri          : ").append(totTgsMandiri).append("\n");
							laporan.append("Total kendala          : ").append(totKendala).append("\n");
							if (totKendala == 0) {
								laporan.append("\nSemua item berhasil disalin tanpa kendala.\n");
							} else {
								laporan.append("\nTerdapat ").append(totKendala)
										.append(" kendala — lihat rincian [KENDALA] per pertemuan di atas.\n");
							}

							try {
								String namaFile = "laporan_salin_agenda_"
										+ Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()) + ".txt";
								org.zkoss.zul.Filedownload.save(laporan.toString().getBytes("UTF-8"), "text/plain",
										namaFile);
							} catch (Exception eDl) {
								ais.common.ErrorAuditUtil.record(eDl, "salin-agenda unduh-laporan");
							}

							try {
								MyMessageboxConfig.show(
										"Salin agenda selesai: " + totPertemuan + " pertemuan dibuat"
												+ (totDilewati > 0 ? ", " + totDilewati + " dilewati" : "")
												+ (totKendala > 0 ? ". Terdapat " + totKendala
														+ " kendala — lihat berkas laporan yang terunduh."
														: " tanpa kendala.")
												+ "\n\nBerikutnya: atur tanggal mulai & interval pertemuan.",
										"Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							} catch (Exception eMsg) {
								ais.common.ErrorAuditUtil.record(eMsg, "salin-agenda ringkasan");
							}

							try {
								KrsMahasiswa krsMahasiswa = null;
								prosesTampilTombolAturUlangWaktu(perkuliahan, kelompokKkn, kelompokPkl,
										mahasiswaRequestTugasAkhir, skripsi, krsMahasiswa, formulirKegiatan, wisuda,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (dataLoader != null) {
													dataLoader.loadData(null);
												}
											}
										});
							} catch (Exception e) {
								if (dataLoader != null) {
									dataLoader.loadData(null);
								}
							}

						}

					}
				});

				window.onModal();
			}

		});
		button.setParent(toolbar);
	}

	/**
	 * Menyalin SATU jenis {@link ais.database.model.file.LampiranLain} (mis. RPS, SAP, catatan
	 * perkuliahan, dsb — lihat konstanta pada {@code LampiranLain}) dari {@code pertemuan} ke
	 * {@code pertemuanBaru}, memakai session {@code StreamingHibernateUtil} terpisah (lampiran memakai
	 * penyimpanan streaming/large-object). Tidak melakukan apa pun bila sumber tidak punya lampiran jenis
	 * tersebut, atau tujuan sudah punya (idempotent — aman dipanggil ulang). Kegagalan di-rollback dan
	 * dicatat ke error audit, tidak dilempar ke pemanggil.
	 *
	 * @param pertemuan    pertemuan sumber
	 * @param pertemuanBaru pertemuan tujuan
	 * @param jenis        jenis lampiran yang disalin
	 */
	public static void copyLampiranPertemuan(Pertemuan pertemuan, Pertemuan pertemuanBaru, String jenis) {
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().openSession();
			session.beginTransaction();
			LampiranLain lama = (LampiranLain) session.createCriteria(LampiranLain.class)
					.add(Restrictions.eq("ref", pertemuan.getId())).add(Restrictions.eq("jenis", jenis))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			LampiranLain baru = (LampiranLain) session.createCriteria(LampiranLain.class)
					.add(Restrictions.eq("ref", pertemuanBaru.getId())).add(Restrictions.eq("jenis", jenis))
					.setMaxResults(1).uniqueResult();
			if (lama != null && baru == null) {
				baru = new LampiranLain();
				baru.setJenis(jenis);
				baru.setRef(pertemuanBaru.getId());
				baru.setCopyDari(lama);
				baru.setGdrive(lama.getGdrive());
				session.save(baru);
			}
			session.getTransaction().commit();
		} finally {
			// Session milik operasi ini saja; kegagalan tetap diteruskan ke laporan salin.
			HibernateUtil.closeSessionQuietly(session);
		}
	}
	/**
	 * Hasil salin lampiran/isi satu pertemuan: jumlah tiap jenis yang berhasil disalin + daftar
	 * kendala rinci (jika ada). Dipakai untuk laporan hasil salin agenda.
	 */
	public static class HasilSalinPertemuan {
		public int materi;
		public int file;
		public int video;
		public int audio;
		public int ujian;
		public int tugasKelompok;
		public int tugasMandiri;
		public final java.util.List<String> kendala = new java.util.ArrayList<String>();
	}

	/** Pesan error rinci (kelas + pesan + baris pertama stack) untuk laporan. */
	public static String pesanError(Throwable e) {
		if (e == null) {
			return "(tidak diketahui)";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(e.getClass().getSimpleName());
		if (e.getMessage() != null) {
			sb.append(": ").append(e.getMessage());
		}
		try {
			if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
				sb.append(" @ ").append(e.getStackTrace()[0].toString());
			}
			if (e.getCause() != null && e.getCause() != e) {
				sb.append(" | penyebab: ").append(e.getCause().getClass().getSimpleName());
				if (e.getCause().getMessage() != null) {
					sb.append(": ").append(e.getCause().getMessage());
				}
			}
		} catch (Exception ignore) {
			ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PenjadwalanHelper.pesanError");
		}
		return sb.toString();
	}

	/**
	 * Menyalin SELURUH isi/lampiran satu {@link Pertemuan} ke pertemuan baru hasil salin agenda, dipakai
	 * oleh {@link #tampilTombolAmbil}. Tiap kategori dibungkus try/catch independen sehingga kegagalan
	 * satu kategori (atau satu item di dalamnya) tidak menggagalkan kategori lain — kendalanya dicatat ke
	 * {@link HasilSalinPertemuan#kendala} dan ke error audit:
	 * <ol>
	 *   <li>Materi/catatan pembelajaran ({@code LampiranLain.CATATAN_PERKULIAHAN}) via
	 *   {@link #copyLampiranPertemuan(Pertemuan, Pertemuan, String)}.</li>
	 *   <li>File, video, dan audio materi ({@link PertemuanFileContent}, {@link VideoPertemuan},
	 *   {@link AudioPertemuan}) — disalin dalam SATU transaksi streaming (bukan per-item) karena kolom
	 *   large-object PostgreSQL tak boleh dibaca/ditulis dalam autocommit; kegagalan satu item membatalkan
	 *   seluruh transaksi kategori ini (jumlah direset ke 0).</li>
	 *   <li>Ujian ({@link PertemuanPunyaUjian}) milik pertemuan sumber, disalin apa adanya ke pertemuan
	 *   baru.</li>
	 *   <li>Tugas kelompok ({@link TugasKelompok}) dan tugas mandiri/individu
	 *   ({@code ais.database.model.TugasPertemuan}) milik pertemuan sumber — masing-masing di-dedup
	 *   berdasarkan nama/judul agar tidak dobel bila dipanggil ulang; field per-mahasiswa (nilai,
	 *   keterangan, status pengumpulan) SENGAJA tidak disalin karena peserta tidak ikut disalin.</li>
	 * </ol>
	 *
	 * @param pertemuan    pertemuan sumber
	 * @param pertemuanBaru pertemuan tujuan yang isinya akan dilengkapi
	 * @return ringkasan jumlah item per kategori yang berhasil disalin plus daftar kendala rinci
	 */
	@SuppressWarnings("unchecked")
	public static HasilSalinPertemuan copyLampiranPertemuan(Pertemuan pertemuan, Pertemuan pertemuanBaru) {
		final HasilSalinPertemuan hasil = new HasilSalinPertemuan();

		// 1) Materi / catatan pembelajaran (LampiranLain CATATAN_PERKULIAHAN).
		try {
			LampiranLain sebelum = LampiranLain.ambil(pertemuanBaru.getId(), LampiranLain.CATATAN_PERKULIAHAN);
			copyLampiranPertemuan(pertemuan, pertemuanBaru, LampiranLain.CATATAN_PERKULIAHAN);
			LampiranLain sesudah = LampiranLain.ambil(pertemuanBaru.getId(), LampiranLain.CATATAN_PERKULIAHAN);
			if (sebelum == null && sesudah != null) {
				hasil.materi++;
			}
		} catch (Exception e) {
			hasil.kendala.add("Materi/catatan pembelajaran: " + pesanError(e));
			ais.common.ErrorAuditUtil.record(e, "salin-agenda materi p" + pertemuanBaru.getId());
		}

		// 2) File / Video / Audio materi (transaksi per item).
		Session lampiranSession = null;
		try {
			lampiranSession = StreamingHibernateUtil.getInstance().openSession();
			Session session = lampiranSession;

			List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (PertemuanFileContent c : pertemuanFileContents) {
				Session itemSession = null;
				try {
					itemSession = StreamingHibernateUtil.getInstance().openSession();
					itemSession.beginTransaction();
					c = (PertemuanFileContent) itemSession.get(PertemuanFileContent.class, c.getId());
					PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
					pertemuanFileContent.setFoto(c.getFoto());
					pertemuanFileContent.setNama(c.getNama());
					pertemuanFileContent.setFileMimeType(c.getFileMimeType());
					pertemuanFileContent.setCopyDari(c);
					pertemuanFileContent.setGdrive(c.getGdrive());
					pertemuanFileContent.setLokasiFisik(c.getLokasiFisik());
					pertemuanFileContent.setPertemuan(pertemuanBaru.getId());
					pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
					itemSession.save(pertemuanFileContent);
					itemSession.getTransaction().commit();
					hasil.file++;
				} catch (Exception e) {
					hasil.kendala.add("File materi \"" + c.getNama() + "\": "
							+ pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda file p" + pertemuanBaru.getId());
				} finally {
					HibernateUtil.closeSessionQuietly(itemSession);
				}
			}

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (VideoPertemuan c : videoPertemuans) {
				Session itemSession = null;
				try {
					itemSession = StreamingHibernateUtil.getInstance().openSession();
					itemSession.beginTransaction();
					c = (VideoPertemuan) itemSession.get(VideoPertemuan.class, c.getId());
					VideoPertemuan videoPertemuan = new VideoPertemuan();
					videoPertemuan.setFoto(c.getFoto());
					videoPertemuan.setNama(c.getNama());
					videoPertemuan.setJurusan(c.getJurusan());
					videoPertemuan.setKeterangan(c.getKeterangan());
					videoPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
					videoPertemuan.setTahunAkademik(c.getTahunAkademik());
					videoPertemuan.setType(c.getType());
					videoPertemuan.setUkuran(c.getUkuran());
					videoPertemuan.setCopyDari(c);
					videoPertemuan.setGdrive(c.getGdrive());
					videoPertemuan.setPertemuan(pertemuanBaru.getId());
					itemSession.save(videoPertemuan);
					itemSession.getTransaction().commit();
					hasil.video++;
				} catch (Exception e) {
					hasil.kendala.add("Video materi \"" + c.getNama() + "\": "
							+ pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda video p" + pertemuanBaru.getId());
				} finally {
					HibernateUtil.closeSessionQuietly(itemSession);
				}
			}

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (AudioPertemuan c : audioPertemuans) {
				Session itemSession = null;
				try {
					itemSession = StreamingHibernateUtil.getInstance().openSession();
					itemSession.beginTransaction();
					c = (AudioPertemuan) itemSession.get(AudioPertemuan.class, c.getId());
					AudioPertemuan audioPertemuan = new AudioPertemuan();
					audioPertemuan.setFoto(c.getFoto());
					audioPertemuan.setNama(c.getNama());
					audioPertemuan.setJurusan(c.getJurusan());
					audioPertemuan.setKeterangan(c.getKeterangan());
					audioPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
					audioPertemuan.setTahunAkademik(c.getTahunAkademik());
					audioPertemuan.setType(c.getType());
					audioPertemuan.setUkuran(c.getUkuran());
					audioPertemuan.setCopyDari(c);
					audioPertemuan.setGdrive(c.getGdrive());
					audioPertemuan.setPertemuan(pertemuanBaru.getId());
					itemSession.save(audioPertemuan);
					itemSession.getTransaction().commit();
					hasil.audio++;
				} catch (Exception e) {
					hasil.kendala.add("Audio materi \"" + c.getNama() + "\": "
							+ pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda audio p" + pertemuanBaru.getId());
				} finally {
					HibernateUtil.closeSessionQuietly(itemSession);
				}
			}

		} catch (Exception e1) {
			hasil.kendala.add(
					"Lampiran file/video/audio (umum): " + pesanError(e1));
			ais.common.ErrorAuditUtil.record(e1, "salin-agenda lampiran-streaming p" + pertemuanBaru.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(lampiranSession);
		}

		Session session = HibernateUtil.currentSession();

		// 3) Ujian (PertemuanPunyaUjian) — pertemuan yang sama.
		try {
			List<PertemuanPunyaUjian> pertemuanPunyaUjians = session.createCriteria(PertemuanPunyaUjian.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("pertemuan", pertemuan)).list();
			for (PertemuanPunyaUjian punyaUjian : pertemuanPunyaUjians) {
				try {
					PertemuanPunyaUjian pertemuanPunyaUjian = new PertemuanPunyaUjian();
					pertemuanPunyaUjian.setDibatasiWaktu(punyaUjian.getDibatasiWaktu());
					pertemuanPunyaUjian.setFormatNilai(punyaUjian.getFormatNilai());
					pertemuanPunyaUjian.setJmlDitampilkan(punyaUjian.getJmlDitampilkan());
					pertemuanPunyaUjian.setKeterangan(punyaUjian.getKeterangan());
					pertemuanPunyaUjian.setLama(punyaUjian.getLama());
					pertemuanPunyaUjian.setMulaiUjian(punyaUjian.getMulaiUjian());
					pertemuanPunyaUjian.setNama(punyaUjian.getNama());
					pertemuanPunyaUjian.setPertemuan(pertemuanBaru);
					pertemuanPunyaUjian.setSampaiUjian(punyaUjian.getSampaiUjian());
					pertemuanPunyaUjian.setUjian(punyaUjian.getUjian());
					Common.refreshSaveOrUpdate(session, pertemuanPunyaUjian);
					hasil.ujian++;
				} catch (Exception e) {
					hasil.kendala.add("Ujian \"" + punyaUjian.getNama() + "\": " + pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda ujian p" + pertemuanBaru.getId());
				}
			}
		} catch (Exception e) {
			hasil.kendala.add("Ujian (umum): " + pesanError(e));
			ais.common.ErrorAuditUtil.record(e, "salin-agenda ujian-umum p" + pertemuanBaru.getId());
		}

		if (pertemuan != null) {

			// 4a) Tugas kelompok — pertemuan yang sama (dedup by nama).
			try {
				List<TugasKelompok> tugasKelompoks = session.createCriteria(TugasKelompok.class)
						.add(Restrictions.eq("pertemuan", pertemuan.getId())).addOrder(Order.asc("id")).list();
				for (TugasKelompok c : tugasKelompoks) {
					try {
						TugasKelompok tugasKelompok = (TugasKelompok) session.createCriteria(TugasKelompok.class)
								.add(Restrictions.eq("nama", c.getNama()))
								.add(Restrictions.eq("pertemuan", pertemuanBaru.getId())).setMaxResults(1).uniqueResult();
						if (tugasKelompok == null) {
							tugasKelompok = new TugasKelompok();
							tugasKelompok.setPertemuan(pertemuanBaru.getId());
							tugasKelompok.setNama(c.getNama());
							tugasKelompok.setPerkuliahan(pertemuanBaru.getPerkuliahan());
							tugasKelompok.setKelompokKkn(pertemuanBaru.getKelompokKkn());
							tugasKelompok.setKelompokPkl(pertemuanBaru.getKelompokPkl());
							Common.refreshSaveOrUpdate(session, tugasKelompok);
							hasil.tugasKelompok++;
						}
					} catch (Exception e) {
						hasil.kendala.add("Tugas kelompok \"" + c.getNama() + "\": " + pesanError(e));
						ais.common.ErrorAuditUtil.record(e, "salin-agenda tugas-kelompok p" + pertemuanBaru.getId());
					}
				}
			} catch (Exception e) {
				hasil.kendala.add("Tugas kelompok (umum): " + pesanError(e));
				ais.common.ErrorAuditUtil.record(e, "salin-agenda tugas-kelompok-umum p" + pertemuanBaru.getId());
			}

			// 4b) Tugas mandiri / individu (TugasPertemuan) — pertemuan yang sama (dedup by judul).
			// Field per-mahasiswa (nilai/keterangan/mhs) SENGAJA tidak disalin (peserta tak ikut).
			try {
				List<ais.database.model.TugasPertemuan> tugasMandiris = session
						.createCriteria(ais.database.model.TugasPertemuan.class)
						.add(Restrictions.eq("pertemuan", pertemuan.getId())).addOrder(Order.asc("id")).list();
				for (ais.database.model.TugasPertemuan c : tugasMandiris) {
					try {
						String judul = c.getJudultugas();
						if (judul == null || judul.trim().isEmpty()) {
							continue;
						}
						ais.database.model.TugasPertemuan ada = (ais.database.model.TugasPertemuan) session
								.createCriteria(ais.database.model.TugasPertemuan.class)
								.add(Restrictions.eq("judultugas", judul))
								.add(Restrictions.eq("pertemuan", pertemuanBaru.getId())).setMaxResults(1).uniqueResult();
						if (ada == null) {
							ais.database.model.TugasPertemuan baru = new ais.database.model.TugasPertemuan();
							baru.setJudultugas(c.getJudultugas());
							baru.setIsitugas(c.getIsitugas());
							baru.setMulai(c.getMulai());
							baru.setSelesai(c.getSelesai());
							baru.setFormatNilai(c.getFormatNilai());
							baru.setFormatNilais(c.getFormatNilais());
							baru.setProsentase(c.getProsentase());
							baru.setSyaratMengumpulkanTugas(c.getSyaratMengumpulkanTugas());
							baru.setSyaratAkses(c.getSyaratAkses());
							baru.setJenisItemPenilaianSiswa(c.getJenisItemPenilaianSiswa());
							baru.setGrupPenilaian(c.getGrupPenilaian());
							baru.setGrupKategoriItemPenilaianSiswa(c.getGrupKategoriItemPenilaianSiswa());
							baru.setAktif(c.getAktif() == null ? Boolean.TRUE : c.getAktif());
							baru.setPertemuan(pertemuanBaru.getId());
							Common.refreshSaveOrUpdate(session, baru);
							hasil.tugasMandiri++;
						}
					} catch (Exception e) {
						hasil.kendala.add("Tugas mandiri \"" + c.getJudultugas() + "\": " + pesanError(e));
						ais.common.ErrorAuditUtil.record(e, "salin-agenda tugas-mandiri p" + pertemuanBaru.getId());
					}
				}
			} catch (Exception e) {
				hasil.kendala.add("Tugas mandiri (umum): " + pesanError(e));
				ais.common.ErrorAuditUtil.record(e, "salin-agenda tugas-mandiri-umum p" + pertemuanBaru.getId());
			}
		}

		return hasil;
	}

	/**
	 * Sama seperti {@link #buatSatuPertemuan(Perkuliahan, Tbmuser, EventListener, StatusPertemuan)}
	 * dengan {@code statusPertemuan=null} (jenis pertemuan dipilih bebas oleh pengguna, default Tatap
	 * Muka).
	 */
	public static MyToolbarbuttonConfig buatSatuPertemuan(Perkuliahan perkuliahan, Tbmuser tbmuser,
			EventListener eventListenerData) {
		return buatSatuPertemuan(perkuliahan, tbmuser, eventListenerData, null);
	}

	/**
	 * Membangun tombol "Tambah Satu Pertemuan" (untuk perkuliahan non-OBE) atau, bila kurikulum
	 * matakuliah memakai OBE ({@code Kurikulum.apakahObe}), tombol "Tambah Rincian OBE" yang membuka
	 * editor rincian mingguan lewat {@code RpsObeAction.editRinci}/{@code reloadRinci} (populate dari
	 * {@code kurikulumPunyaMatakuliah.getRincian()}, menghitung ulang jumlah pertemuan yang perlu
	 * disiapkan dari {@code sampaiMingguKe} tiap sub-CPMK, lalu me-refresh cache pertemuan lewat
	 * {@code RpsObeAction.refreshPertemuan} sebelum menampilkan jendela "Rincian Kurikulum OBE"). Tombol
	 * OBE disembunyikan bila {@code kurikulumPunyaMatakuliah} kosong atau sudah dikunci; kedua varian
	 * tombol disembunyikan untuk siswa/mahasiswa, dan untuk guru yang perkuliahannya tidak mengizinkan
	 * dosen mengubah tanggal.
	 *
	 * <p>Pada varian non-OBE, jendela "Pilih Tanggal Pertemuan" meminta tanggal (readonly, default hari
	 * ini), jam mulai/selesai (default dari jam perkuliahan), jenis pertemuan ({@code statusPertemuan}
	 * bila diberikan — dikunci lewat {@code Common.freezeGanti} agar tidak bisa diganti — atau Tatap Muka
	 * sebagai default yang bisa diubah), kemampuan/kompetensi, bahan kajian, metode pembelajaran,
	 * referensi, dan catatan; menyimpan satu {@link Pertemuan} baru lalu memanggil
	 * {@code perkuliahan.reInitPertemuan} agar cache urutan pertemuan konsisten.</p>
	 *
	 * @param statusPertemuan jenis pertemuan yang dipaksakan (mis. UTS/UAS) pada varian non-OBE, atau
	 *                        {@code null} untuk membiarkan pengguna memilih bebas (default Tatap Muka)
	 * @return tombol toolbar yang sudah dilengkapi listener {@code onClick}, belum ditambahkan ke parent
	 *         mana pun — pemanggil bertanggung jawab memanggil {@code setParent(toolbar)}
	 */
	public static MyToolbarbuttonConfig buatSatuPertemuan(final Perkuliahan perkuliahan, final Tbmuser tbmuser,
			final EventListener eventListenerData, final StatusPertemuan statusPertemuan) {

		if (perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.ambilKurikulumPunyaMatakuliah();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Rincian OBE", "/img/svg/addthis.svg");

			if (kurikulumPunyaMatakuliah == null || kurikulumPunyaMatakuliah.getDikunci() != null) {
				button.setVisible(false);
			}

			if (tbmuser.getSiswa() != null || tbmuser.getMahasiswa() != null || (tbmuser.ambilGuru() != null
					&& perkuliahan != null && !perkuliahan.getDosenBisaMerubahTanggalPerkuliahan())) {
				button.setVisible(false);
			}
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final JSONObject jsonArraykurikulumPunyaMatakuliah = new JSONObject(
							kurikulumPunyaMatakuliah.getRincian());

					RpsObeAction.editRinci(new JSONObject(), null, kurikulumPunyaMatakuliah,
							jsonArraykurikulumPunyaMatakuliah, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Common.createDefaultTimer(new EventListener() {

										@SuppressWarnings("rawtypes")
										@Override
										public void onEvent(Event arg0) throws Exception {

											int banyak = 0;
											TreeMap<Integer, Map> maps = kurikulumPunyaMatakuliah
													.populateRinci(jsonArraykurikulumPunyaMatakuliah);
											for (Map map : maps.values()) {
												JSONObject jsonObject = (JSONObject) map.get("jsonObject");
												JSONObject subCpmk = (JSONObject) map.get("subCpmk");
												CapaianPembelajaranLulusan capaianPembelajaranLulusanData = (CapaianPembelajaranLulusan) map
														.get("capaianPembelajaranLulusanData");
												if (subCpmk != null && capaianPembelajaranLulusanData != null) {
													int sampaiMingguKe = jsonObject.getInt("sampaiMingguKe");
													if (banyak < sampaiMingguKe) {
														banyak = sampaiMingguKe;
													}
												}
											}
											Map<Integer, Pertemuan> pertemuansData = new HashMap<Integer, Pertemuan>();
											RpsObeAction.refreshPertemuan(banyak, pertemuansData, perkuliahan);

											final MyWindow window = new MyWindow("Rincian Kurikulum OBE", "none", true);
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											window.setHeight("95%");
											window.setWidth("95%");

											Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
											borderlayout.setParent(window);

											Center a = new Center();
											a.setParent(borderlayout);

											RpsObeAction.reloadRinci(true, kurikulumPunyaMatakuliah,
													jsonArraykurikulumPunyaMatakuliah, perkuliahan, a,
													kurikulumPunyaMatakuliah.getDikunci() == null,
													kurikulumPunyaMatakuliah.getDikunci() == null, pertemuansData,
													false);

											South south = new South();
											ais.ui.util.ZkCompat.setFlex(south, true);
											south.setParent(borderlayout);

											Toolbar toolbar = new Toolbar();
											// toolbar.setHeight("25px");
											toolbar.setParent(south);
											MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
													"/img/cancel.gif");
											cancel.setTooltiptext("Tutup");
											cancel.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {
													eventListenerData.onEvent(new Event("", null, null));
													window.detach();
												}
											});
											cancel.setParent(toolbar);

											window.onModal();

										}
									});

								}
							});
				}
			});

			return button;
		} else {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Satu Pertemuan", "/img/new.gif");
			if (tbmuser.getSiswa() != null || tbmuser.getMahasiswa() != null || (tbmuser.ambilGuru() != null
					&& perkuliahan != null && !perkuliahan.getDosenBisaMerubahTanggalPerkuliahan())) {
				button.setVisible(false);
			}
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					final MyWindow window = new MyWindow("Pilih Tanggal Pertemuan", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("95%");
					window.setWidth("600px");
					final Combobox tahunAkademik = new Combobox();
					Common.generateTahunAjaran(tahunAkademik);

					final Combobox tahunAkademikSampai = new Combobox();
					Common.generateTahunAjaran(tahunAkademikSampai);

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("30%");
					column.setParent(columns);
					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pertemuan"));
					final MyDatebox tanggalPertemuan;
					row.appendChild(tanggalPertemuan = new MyDatebox(WaktuUtil.getDate()));
					tanggalPertemuan.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Pertemuan"));

					final Timebox waktuMulai;
					final Timebox waktuSelesai;
					waktuMulai = new ais.ui.util.MyTimebox();
					waktuSelesai = new ais.ui.util.MyTimebox();
					waktuMulai.setFormat(Common.timeFormat.get().toPattern());
					waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

					try {
						waktuMulai.setValue(
								perkuliahan.getWaktuMulai() == null || perkuliahan.getWaktuMulai().trim().isEmpty()
										? null
										: Common.timeFormat2.get().parse(perkuliahan.getWaktuMulai()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanHelper.java:2839");

					}
					try {
						waktuSelesai.setValue(
								perkuliahan.getWaktuSelesai() == null || perkuliahan.getWaktuSelesai().trim().isEmpty()
										? null
										: Common.timeFormat2.get().parse(perkuliahan.getWaktuSelesai()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanHelper.java:2847");

					}

					waktuMulai.setCols(1);
					waktuSelesai.setCols(1);

					Hbox hbox = new Hbox();
					hbox.setParent(row);
					waktuMulai.setParent(hbox);
					waktuSelesai.setParent(hbox);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pertemuan"));
					final Combobox combobox = new Combobox();
					Common.insertCombo(combobox, "nama", StatusPertemuan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					Common.selectComboItem(combobox,
							statusPertemuan == null ? ConstantValues.TATAP_MUKA : statusPertemuan);
					combobox.setWidth("95%");
					combobox.setParent(row);
					combobox.setReadonly(true);

					if (statusPertemuan != null) {
						Common.freezeGanti(combobox);
					}

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan / kompetensi yang ingin dicapai"));
					final MyTextbox topik;
					row.appendChild(topik = new MyTextbox());
					topik.setWidth("95%");
					topik.setRows(5);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Kajian"));
					final MyTextbox bukuRujukan1;
					row.appendChild(bukuRujukan1 = new MyTextbox());
					bukuRujukan1.setWidth("95%");
					bukuRujukan1.setRows(4);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Metode Pembelajaran"));
					final MyTextbox metodePembelajaran;
					row.appendChild(metodePembelajaran = new MyTextbox());
					metodePembelajaran.setWidth("95%");
					metodePembelajaran.setRows(2);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Referensi"));
					final MyTextbox bukuRujukan2;
					row.appendChild(bukuRujukan2 = new MyTextbox());
					bukuRujukan2.setWidth("95%");
					bukuRujukan2.setRows(5);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
					final MyTextbox catatan;
					row.appendChild(catatan = new MyTextbox());
					catatan.setWidth("95%");
					catatan.setRows(5);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
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
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Tambahkan Pertemuan", "/img/save.gif");
					save.setTooltiptext("Proses");
					save.addEventListener("onClick", new EventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {

							window.detach();

							final Pertemuan pertemuan = new Pertemuan();
							pertemuan.setCatatan(catatan.getValue());
							pertemuan.setBukuRujukan1(bukuRujukan1.getValue());
							pertemuan.setBukuRujukan2(bukuRujukan2.getValue());
							pertemuan.setStatusPertemuan(statusPertemuan != null ? statusPertemuan
									: (combobox.getSelectedItem() != null
											? (StatusPertemuan) combobox.getSelectedItem().getValue()
											: null));
							pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
							pertemuan.setTanggal(tanggalPertemuan.getValue());
							pertemuan.setPerkuliahan(perkuliahan);
							pertemuan.setRuang(perkuliahan.getRuang());
							pertemuan.setTopik(topik.getValue());
							pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
									: Common.timeFormat2.get().format(waktuMulai.getValue()));
							pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
									: Common.timeFormat2.get().format(waktuSelesai.getValue()));

							pertemuan.setPerkuliahan(perkuliahan);

							Session session = HibernateUtil.currentSession();

							session.save(pertemuan);
							session.flush();
							perkuliahan.belum();

							List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
									.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe")
											: Order.asc("tanggal"))
									.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
									.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
							perkuliahan.reInitPertemuan(pertemuansTemp, session);
							pertemuansTemp.clear();
							pertemuansTemp = null;
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									eventListenerData.onEvent(new Event("", null, pertemuan));
								}
							});

						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			});

			return button;
		}
	}

	/**
	 * Titik masuk utama tampilan "Agenda Pertemuan" untuk satu {@link Perkuliahan}: membersihkan
	 * {@code component}, lalu merender borderlayout berisi toolbar aksi (Buat Pertemuan/Tambah Rincian
	 * OBE, Ambil dari agenda lain, Ubah Tanggal Agenda, Download, Upload, Hapus, "Hapus pertemuan tidak
	 * terpakai (&gt;N)", Refresh, filter "hanya yg aktif", toggle "Urutkan Manual" bila dikonfigurasi
	 * aktif) di {@code North} dan grid pertemuan (kolom Urutan, Kemampuan akhir pembelajaran,
	 * Kriteria/Indikator/Bobot, Waktu, Pengalaman Belajar, Tugas dan Penilaian, Bahan Kajian, Referensi,
	 * Metode Pembelajaran, Jenis Pert., Aktif, Tanggal/Waktu, kolom aksi) di {@code Center}, dirender
	 * baris-per-baris oleh {@link PertemuanRenderer}. Tombol Buat/Ambil/Atur-Ulang/Hapus hanya tampil
	 * bagi pengguna yang bukan mahasiswa dan (bukan dosen, atau dosennya diizinkan mengubah tanggal
	 * perkuliahan). Memanggil {@link #onSearchDefault} di akhir untuk memuat data pertama kali.
	 *
	 * <p>State {@code this.perkuliahan}, {@code this.grid}, {@code this.hanyaYangAktif}, dan
	 * {@code this.urutkanManual} disimpan sebagai field instance agar bisa diakses ulang oleh
	 * {@link #onSearchDefault} saat listener toolbar dipicu.</p>
	 *
	 * @param perkuliahan perkuliahan yang agendanya ditampilkan
	 * @param component   komponen ZK induk tempat tampilan dirender (isinya dibersihkan lebih dulu)
	 */
	public void display(final Perkuliahan perkuliahan, Component component) {
		Common.clear(component);
		this.perkuliahan = perkuliahan;

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(component);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (perkuliahan != null && tbmuser.getMahasiswa() == null) {

			if (tbmuser.ambilDosen() == null || perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()) {
				PenjadwalanHelper.tampilTombolBuatPertemuan(toolbar, perkuliahan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perkuliahan.belum();
						onSearchDefault(arg0);
					}
				});

			}
		}

		MyToolbarbuttonConfig button = PenjadwalanHelper.buatSatuPertemuan(perkuliahan, tbmuser, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		button.setParent(toolbar);

		if (tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
				|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {
			// PenjadwalanHelper.tampilTombol(toolbar, perkuliahan, null, null,
			// null, null);
			PenjadwalanHelper.tampilTombolAmbil(toolbar, perkuliahan, null, null, null, null, null, null,
					new DataLoader() {

						@Override
						public void loadData(Object value) {
							perkuliahan.belum();
							onSearchDefault(null);
						}
					});
			PenjadwalanHelper.tampilTombolAturUlangWaktu(toolbar, perkuliahan, null, null, null, null, null, null, null,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							perkuliahan.belum();
							onSearchDefault(arg0);
						}
					});
		}

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "dosenTamu",
				"dosenTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };

		PenjadwalanHelper.tampilTombolDownload(toolbar, contents, perkuliahan, null, null, null, null, null, null,
				null);

		if (tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
				|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {

			Criterion idCrit = null;
			HashMap<String, Object> nilai = null;
			if (perkuliahan != null) {
				idCrit = Restrictions.eq("perkuliahan", perkuliahan);
				nilai = new HashMap<String, Object>();
				nilai.put("perkuliahan", perkuliahan);
			}

			MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

				@Override
				public void onSearchDefault(Event event) {
					perkuliahan.belum();
					PenjadwalanHelper.this.onSearchDefault(null);
				}
			}, Pertemuan.class, null, idCrit, nilai, contents);
			toolbar.appendChild(upload);

			PenjadwalanHelper.tampilTombolHapus(toolbar, perkuliahan, null, null, null, null, null, null, null,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							perkuliahan.belum();
							onSearchDefault(arg0);
						}
					});
		}

		if (tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
				|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig(
					"Hapus pertemuan tidak terpakai (>" + perkuliahan.getJumlahMaksimalPertemuan() + ")",
					"/img/Check-icon.png");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (perkuliahan.ambilJumlahPertemuan() <= perkuliahan.getJumlahMaksimalPertemuan()) {
						MyMessageboxConfig.show(
								"Jumlah pertemuan tidak lebih dari " + perkuliahan.getJumlahMaksimalPertemuan(),
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					// KE-FIX (pengaman): sebelumnya idsBoleh (daftar pertemuan yang akan DIHAPUS
					// PERMANEN) baru dihitung SETELAH pengguna menekan OK pada dialog konfirmasi
					// yang generik ("...lebih dari N yang tidak terpakai?") -- pengguna menyetujui
					// tanpa tahu persis pertemuan ke berapa saja & berapa banyak yang akan hilang.
					// "N" (getJumlahMaksimalPertemuan()) sendiri BISA berubah diam-diam mengikuti
					// setting kurikulum mata kuliah (lihat javadoc method itu), sehingga tombol ini
					// bisa menghapus pertemuan yang sebenarnya sudah digenerate sah di awal semester
					// hanya karena kurikulumnya belakangan diedit. Hitung dulu daftar konkretnya DI
					// SINI supaya bisa ditampilkan ke pengguna sebelum konfirmasi, dan beri
					// peringatan ekstra + arahan ke "Recovery Data" saat jumlahnya besar (indikasi
					// kuat perubahan setting yang tak disengaja, bukan sekadar baris duplikat/nyasar).
					Session sessionCek = HibernateUtil.currentSession();
					List<Pertemuan> pertemuansCek = sessionCek.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
							.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
					perkuliahan.reInitPertemuan(pertemuansCek, sessionCek);

					final List<Long> idsBolehHapus = new ArrayList<Long>();
					final List<Integer> pertemuanKeAkanDihapus = new ArrayList<Integer>();
					for (Pertemuan pertemuan : pertemuansCek) {
						if (pertemuan.getPertemuanKe() > perkuliahan.getJumlahMaksimalPertemuan()) {
							if (PenjadwalanHelper.checkBolehHapus(pertemuan, false)) {
								idsBolehHapus.add(pertemuan.getId());
								pertemuanKeAkanDihapus.add(pertemuan.getPertemuanKe());
							}
						}
					}

					if (idsBolehHapus.isEmpty()) {
						MyMessageboxConfig.show(
								"Tidak ada pertemuan yang tidak terpakai (>" + perkuliahan.getJumlahMaksimalPertemuan()
										+ ") yang dapat dihapus (pertemuan yang sudah punya kehadiran/materi/tugas/nilai tidak akan dihapus).",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					StringBuilder daftarPertemuan = new StringBuilder();
					for (int idx = 0; idx < pertemuanKeAkanDihapus.size(); idx++) {
						if (idx > 0) daftarPertemuan.append(", ");
						daftarPertemuan.append(pertemuanKeAkanDihapus.get(idx));
					}

					String pesanKonfirmasi = "Akan menghapus PERMANEN " + idsBolehHapus.size()
							+ " pertemuan yang tidak terpakai: pertemuan ke-" + daftarPertemuan
							+ ". Tindakan ini tidak dapat dibatalkan (kecuali lewat fitur Recovery Data). ";
					if (idsBolehHapus.size() > 2) {
						pesanKonfirmasi += "PERINGATAN: jumlah yang akan dihapus cukup banyak -- pastikan dulu "
								+ "batas jumlah pertemuan (" + perkuliahan.getJumlahMaksimalPertemuan()
								+ ") pada mata kuliah ini memang SENGAJA diubah (bukan akibat perubahan setting "
								+ "kurikulum yang tidak disadari), sebelum melanjutkan. ";
					}
					pesanKonfirmasi += "Lanjutkan?";

					MyMessageboxConfig.show(pesanKonfirmasi,
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();

											List<Pertemuan> pertemuansHapus = idsBolehHapus.isEmpty()
													? new ArrayList<Pertemuan>()
													: session.createCriteria(Pertemuan.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.in("id", idsBolehHapus)).list();
											for (Pertemuan pertemuan : pertemuansHapus) {
												Common.refreshDelete(session, pertemuan);
											}

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Session session = HibernateUtil.currentSession();
													List<Pertemuan> pertemuansTemp = session
															.createCriteria(Pertemuan.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.addOrder(!perkuliahan.getUrutkanotomatis()
																	? Order.asc("pertemuanKe")
																	: Order.asc("tanggal"))
															.add(Restrictions.isNotNull("tanggal"))
															.addOrder(Order.asc("id"))
															.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
													perkuliahan.reInitPertemuan(pertemuansTemp, session);

													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.belum();
				onSearchDefault(arg0);
			}
		});

		hanyaYangAktif = new MyCheckboxConfig("hanya yg aktif");
		hanyaYangAktif.setChecked(true);
		hanyaYangAktif.setParent(toolbar);
		hanyaYangAktif.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.belum();
				onSearchDefault(arg0);
			}
		});

		urutkanManual = new MyCheckboxConfig("Urutkan Manual");
		urutkanManual.setChecked(!perkuliahan.getUrutkanotomatis());

		if (Common.bolehKonfigurasi("tampilkan_urutkan_manual_di_agenda_pertemuan")) {
			urutkanManual.setParent(toolbar);
		}
		urutkanManual.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				perkuliahan.setUrutkanotomatis(!urutkanManual.isChecked());
				Common.refreshUpdate(perkuliahan);
				perkuliahan.belum();
				onSearchDefault(arg0);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Urutan");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kemampuan akhir pembelajaran");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kriteria,Indikator&Bobot penilaian");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengalaman Belajar");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tugas Dan Penilaian");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bahan Kajian");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Referensi");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Metode Pembelajaran");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pert.");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal/Waktu");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setImage("/img/svg/check2-circle.svg");
		column.setHoverImage("/img/svg/check-circled-outline.svg");
		column.setWidth("3%");

		onSearchDefault(null);

	}

	/**
	 * Varian jendela modal dari tampilan agenda: membuka {@link Window} berjudul info perkuliahan yang
	 * berisi, di {@code Center}, salah satu dari dua tampilan tergantung apakah kurikulum matakuliah
	 * perkuliahan ini memakai OBE ({@code Kurikulum.apakahObe(tahunAjaran, ganjilGenap)}):
	 * <ul>
	 *   <li>Bukan OBE (atau kurikulum {@code null}): mendelegasikan ke
	 *   {@link #display(Perkuliahan, Component)} — grid agenda biasa.</li>
	 *   <li>OBE: menampilkan iframe {@code /pages/master/rps_obe.zul} (halaman RPS-OBE terpisah,
	 *   tinggi tetap 12000px) yang membawa parameter {@code kur} (id {@link KurikulumPunyaMatakuliah})
	 *   dan {@code perkuliahan}; bila {@code kurikulumPunyaMatakuliah} belum ada, menampilkan pesan
	 *   "Kurikulum belum diisi secara benar".</li>
	 * </ul>
	 * Tombol "Selesai" di {@code South} membersihkan cache ({@code perkuliahan.belum()}), memanggil
	 * {@code dataLoader.loadData(null)}, lalu menutup jendela.
	 *
	 * @param perkuliahan perkuliahan yang agendanya ditampilkan
	 * @param dataLoader  dipanggil saat jendela ditutup, agar pemanggil memuat ulang tampilan induk
	 */
	public void display(final Perkuliahan perkuliahan, final DataLoader dataLoader) {
		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(perkuliahan.info());
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setWidth("98%");
		window.setHeight("98%");

		Borderlayout borderlayoutUtama = new ais.ui.util.MyBorderlayout();
		borderlayoutUtama.setParent(window);
		borderlayoutUtama.setWidth("100%");
		borderlayoutUtama.setHeight("100%");

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);
		centerUtama.setAutoscroll(true);

		if (perkuliahan != null && (perkuliahan.getKurikulum() == null
				|| !perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap()))) {
			display(perkuliahan, centerUtama);
		} else {

			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.ambilKurikulumPunyaMatakuliah();

			if (kurikulumPunyaMatakuliah != null) {
				MyInclude iframe = new MyInclude("/pages/master/rps_obe.zul?kur=" + kurikulumPunyaMatakuliah.getId()
						+ "&perkuliahan=" + perkuliahan.getId());
				iframe.setHeight("12000px");
				iframe.setParent(centerUtama);
			} else {
				new MyLabelBoldMerah("Kurikulum belum diisi secara benar").setParent(centerUtama);
			}
		}

		South south = new South();
		south.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				perkuliahan.belum();
				dataLoader.loadData(null);
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memuat ulang daftar id pertemuan untuk {@code grid} sesuai state checkbox {@code hanyaYangAktif},
	 * lalu memasang ulang {@link PertemuanRenderer} dan model-nya. Bila filter "hanya yg aktif" TIDAK
	 * dicentang, query langsung mengambil semua id pertemuan (aktif maupun tidak) terurut sesuai mode
	 * urut perkuliahan. Bila dicentang, memakai jalur cache milik {@link Perkuliahan}: kalau cache belum
	 * ada ({@code !perkuliahan.udah()}), query ulang pertemuan aktif lalu panggil
	 * {@code perkuliahan.reInitPertemuan(...)} untuk membangun ulang cache (termasuk penomoran ulang bila
	 * mode urut otomatis) sebelum membaca id dari {@code perkuliahan.ambilPertemuan(0, 1000, false)}.
	 *
	 * @param event event pemicu (tidak dipakai isinya, hanya diteruskan lewat listener); boleh
	 *              {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		if (!hanyaYangAktif.isChecked()) {
			List<Long> pertemuanss = session.createCriteria(Pertemuan.class).setProjection(Projections.property("id"))
					.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

			ListModel strset = new SimpleListModel(pertemuanss);
			grid.setRowRenderer(new PertemuanRenderer(perkuliahan, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			}));
			grid.setModelCheckMobile(strset);
		} else {

			final List<Long> pertemuanss;
			if (perkuliahan.udah()) {
				Object[] a = perkuliahan.ambilPertemuan(0, 1000, false);
				pertemuanss = (List<Long>) a[0];
			} else {
				List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
						.add(hanyaYangAktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.sqlRestriction("true"))
						.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
						.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
						.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
				perkuliahan.reInitPertemuan(pertemuansTemp, session);
				pertemuansTemp.clear();
				pertemuansTemp = null;

				Object[] a = perkuliahan.ambilPertemuan(0, 1000, false);
				pertemuanss = (List<Long>) a[0];
			}

			ListModel strset = new SimpleListModel(pertemuanss);
			grid.setRowRenderer(new PertemuanRenderer(perkuliahan, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			}));
			grid.setModelCheckMobile(strset);
		}

	}

}
