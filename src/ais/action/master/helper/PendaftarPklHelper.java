package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.pkl.PklUntukMahasiswaAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pkl;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPklMahasiswa;
import ais.database.model.pkl.MahasiswaDaftarPkl;
import ais.database.model.pkl.MahasiswaPklPersyaratan;
import ais.database.model.pkl.PersyaratanPkl;
import ais.database.model.pkl.PklPunyaPersyaratan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer untuk mengelola daftar mahasiswa pendaftar seleksi satu {@link Pkl} (Praktik
 * Kerja Lapangan): menampilkan {@link MahasiswaDaftarPkl} berpaging dengan info akademik terkini
 * (SKS/SKSK, IP/IPK disinkronkan lewat {@link Common#singkronkanKrsMahasiswa}), skor seleksi total,
 * dan status terima/tolak yang dapat diubah langsung (hanya bila {@link #approve} true). Mendukung
 * pencarian mahasiswa baru lewat {@link AmbilDataMahasiswaSeleksiPklHelper}, cetak berbagai laporan
 * PDF (kartu pendaftar per mahasiswa, daftar pendaftar, daftar diterima, rekap penerima), serta
 * ekspor/impor massal data persyaratan dan status penerimaan lewat Excel (.xlsx).
 *
 * <p>
 * <b>Ekspor Excel</b> ({@link #cetakDataCustomButton}): dijalankan di thread terpisah dengan
 * indikator progres (busy overlay + label berjalan, dipoll lewat {@link org.zkoss.zul.Timer}),
 * menghasilkan satu baris per mahasiswa dengan kolom dasar (ID, NIM, nama, jurusan, fakultas,
 * diterima, skor) diikuti satu kolom per {@link PersyaratanPkl} milik {@link Pkl} ini — tipe kolom
 * mengikuti {@link PersyaratanPkl#getTipeDataInputan()} (teks/angka/tanggal/ya-tidak/pilihan
 * custom), dengan hyperlink ke berkas lampiran bila persyaratan tersebut mewajibkan lampiran.
 * Baris {@link MahasiswaPklPersyaratan} yang belum ada dibuat otomatis (kosong) saat proses
 * berjalan. Hasil akhir ditampilkan dalam pratinjau spreadsheet sebelum diunduh.
 *
 * <p>
 * <b>Impor Excel</b> (tombol "Upload" pada {@link #displayPrasyaratPkl}): membaca kembali file
 * .xlsx berformat sama (kolom ID di 0, NIM di 1, status diterima di 5), mencocokkan baris lewat id
 * (bila ada) atau membuat baris {@link MahasiswaDaftarPkl} baru berdasarkan NIM, lalu memperbarui
 * status terima/tolak — dijalankan juga di thread terpisah dengan indikator progres serupa.
 *
 * <p>
 * Tombol "Hitung Skor" menjumlahkan skor dari seluruh jawaban {@link MahasiswaPklPersyaratan}
 * bertipe {@link PersyaratanPkl#PILIHAN_CUSTOM} (format nilai {@code "label:skor"}, bagian skor
 * di-parse dari segmen setelah titik dua) untuk setiap mahasiswa hasil filter saat ini, lalu
 * menuliskannya ke {@link MahasiswaDaftarPkl#setTotalSkor}.
 *
 * <h3>Posisi dalam alur PKL</h3>
 * <p>
 * Kelas ini adalah layar <b>pendaftaran/seleksi</b>, satu tahap sebelum pembagian kelompok:
 * <ol>
 *   <li>mahasiswa (atau petugas) mendaftar lewat
 *   {@code ais.action.master.pkl.PklUntukMahasiswaAction} — di sanalah kolom
 *   {@code memenuhiSyarat} dihitung dan disimpan;</li>
 *   <li>panitia menyeleksi di layar ini ({@code SeleksiPenerimaPklAction} &rarr; helper ini),
 *   menetapkan {@code totalSkor} dan {@code terima};</li>
 *   <li>mahasiswa yang diterima dibagi ke kelompok lewat {@link KelompokPklHelper} dan
 *   {@link ais.database.model.MahasiswaDapatKelompokPkl}.</li>
 * </ol>
 * Pengecualian per-mahasiswa (mahasiswa yang dibebaskan dari sebagian syarat) dikelola terpisah
 * lewat {@link PengecualianPklMahasiswaHelper}.
 *
 * <h3>Catatan otorisasi</h3>
 * <p>
 * Hak {@code APPROVE} dari menu diteruskan pemanggil sebagai parameter {@code approve} dan dipakai
 * untuk menonaktifkan checkbox "Terima" pada grid. Tiga jalur lain yang juga mengubah data
 * penerimaan — tombol "Upload" (impor Excel menulis kolom {@code terima}), tombol hapus per baris,
 * dan tombol "Hitung Skor" (menulis {@code totalSkor}) — tidak bergantung pada field {@code approve}
 * ini; masing-masing memeriksa ulang haknya sendiri secara independen tepat sebelum menulis data,
 * langsung lewat {@code CommonPrivilages.checkPrevilages(...)} di dalam listener-nya: "Upload"
 * menuntut {@code APPROVE} (menulis {@code terima} setara keputusan penerimaan), tombol hapus
 * menuntut {@code DELETE}, dan "Hitung Skor" menuntut {@code UPDATE}. Karena pemeriksaan ini dibaca
 * langsung dari hak pengguna saat listener dijalankan (bukan dari status tampil/nonaktif komponen
 * UI), event ZK yang terlanjur dipicu ke komponen yang seharusnya tersembunyi/nonaktif tetap
 * ditolak. Kelas ini juga tidak melakukan pemeriksaan cakupan satuan kerja/tenant atas {@link #pkl}
 * yang diterimanya — lihat catatan pada field {@link #pkl}.
 *
 * <h3>Hubungan dengan kembarannya di modul KKN</h3>
 * <p>
 * Kelas ini adalah salinan {@link PendaftarKknHelper} untuk modul PKL. Keduanya sejajar baris demi
 * baris pada hampir seluruh isinya. Sebelumnya terdapat tiga perbedaan nyata yang bukan sekadar
 * penggantian nama entitas — ketiganya membuat sisi PKL lebih lemah. Ketiganya kini sudah
 * ditambal:
 * <ul>
 *   <li><b>(Sudah ditambal.)</b> {@link PendaftarPklRenderer} sebelumnya membuat label kolom
 *   "Memenuhi Syarat" tetapi tidak pernah mengisinya, sehingga kolom tersebut selalu kosong di
 *   layar ini. Kini label diisi dari {@code getMemenuhiSyarat()}, sejajar dengan kembarannya;</li>
 *   <li><b>(Sudah ditambal.)</b> pencarian {@link MahasiswaPklPersyaratan} saat ekspor sebelumnya
 *   memanggil {@code uniqueResult()} tanpa {@code addOrder(desc(id))} + {@code setMaxResults(1)},
 *   sehingga data rangkap memicu pengecualian. Kini pembatas yang sama dipasang, sejajar dengan
 *   kembarannya;</li>
 *   <li><b>(Sudah ditambal.)</b> tombol "Rekap" sebelumnya menunjuk nama laporan
 *   {@code penerima-pkl} (dengan tanda hubung) yang tidak ada di direktori laporan manapun. Kini
 *   menunjuk {@code penerima_pkl} (dengan garis bawah, mengikuti konvensi kembarannya
 *   {@code penerima_kkn}) — berkas {@code webapp/report/penerima_pkl.jrxml} baru dibuat dengan
 *   mem-port {@code penerima_kkn.jrxml} ke tabel {@code mahasiswa_daftar_pkl}/{@code pkl} (kolom
 *   dan struktur parameter {@code id_pkl} identik, hanya nama tabel dan judul yang berbeda);
 *   laporan {@code penerima_kelompok_pkl} yang sudah ada <b>tidak</b> bisa dipakai sebagai
 *   pengganti karena parameternya (juga bernama {@code id_pkl}) sebenarnya adalah ID
 *   {@code kelompok_pkl}, bukan ID {@link Pkl}, dan query-nya mensyaratkan mahasiswa sudah dibagi
 *   ke kelompok — data yang belum ada pada tahap seleksi ini.</li>
 * </ul>
 * <p>
 * Perbedaan lain bersifat kosmetik dan tidak mengubah perilaku: kelas ini memakai
 * {@code ais.common.HashMapGenerator.getRand()} pada tiga penampung parameter laporan sedangkan
 * kembarannya memakai {@code new HashMap}.
 *
 * <h3>Catatan: syarat akademik tidak dievaluasi di kelas ini</h3>
 * <p>
 * Ambang SKS/IPK PKL — termasuk pasangan syarat kedua yang dikendalikan sakelar "Aktifkan Syarat
 * Lain" pada {@link Pkl} — <b>tidak</b> dievaluasi di sini. Kelas ini hanya berurusan dengan kolom
 * {@code memenuhiSyarat} yang sudah tersimpan (kini ditampilkan apa adanya di kolom "Memenuhi
 * Syarat", lihat di atas — bukan dihitung ulang).
 * Jebakan konfigurasi pada pasangan syarat kedua terdokumentasi pada {@link Pkl} dan berlaku bagi
 * jalur yang menghitung {@code memenuhiSyarat}, bukan bagi kelas ini.
 *
 * @see PendaftarKknHelper
 * @see PengecualianPklMahasiswaHelper
 * @see KelompokPklHelper
 * @see AmbilDataMahasiswaSeleksiPklHelper
 */
public class PendaftarPklHelper implements DataLoader, DataCriteria {

	/**
	 * Grid utama berisi baris {@link MahasiswaDaftarPkl} hasil {@link #initCriteria(boolean)}.
	 * Dibuat sekali di {@link #displayPrasyaratPkl}, lalu di-render ulang setiap
	 * {@link #loadData(Object)} memakai {@link PendaftarPklRenderer}.
	 */
	private MyGrid grid;

	/**
	 * PKL yang sedang dikelola. Menjadi filter wajib pada seluruh query kelas ini
	 * ({@code Restrictions.eq("pkl", pkl)}) sekaligus penentu daftar {@link PersyaratanPkl} yang
	 * dipakai sebagai kolom dinamis ekspor Excel.
	 *
	 * <p>
	 * Objek ini diterima apa adanya dari pemanggil
	 * ({@code ais.action.master.pkl.SeleksiPenerimaPklAction}); kelas ini <b>tidak</b> memeriksa
	 * ulang kepemilikan/cakupan satuan kerja atas PKL tersebut — pembatasan cakupan sepenuhnya
	 * menjadi tanggung jawab Action pemanggil.
	 * </p>
	 */
	private Pkl pkl;

	/**
	 * Kotak isian kata kunci pencarian pendaftar. Nilainya dicocokkan sekaligus ke NIM
	 * <b>atau</b> nama mahasiswa (LIKE {@code MatchMode.ANYWHERE}, case-insensitive) pada
	 * {@link #initCriteria(boolean)}; kosong berarti tanpa penyaringan.
	 */
	private Textbox nim;

	/** Combobox filter fakultas. Bila tidak ada pilihan aktif, kriteria hanya mensyaratkan {@code jurusan.fakultas} tidak null (mahasiswa tanpa fakultas ikut tersaring keluar). */
	private Combobox fakultas;

	/** Combobox filter jurusan/program studi. Bila tidak ada pilihan aktif, kriteria hanya mensyaratkan mahasiswa punya jurusan. */
	private Combobox jurusan;

	/** Kontrol paging 50 baris per halaman untuk {@link #grid}; halaman aktifnya dibaca ulang setiap {@link #loadData(Object)}. */
	private Paging paging;

	/**
	 * Penanda apakah pengguna saat ini boleh mengubah status penerimaan langsung dari grid.
	 * Diisi pemanggil dari hak {@code CommonPrivilages.APPROVE}.
	 *
	 * <p>
	 * <b>Cakupan:</b> flag ini hanya menonaktifkan checkbox "Terima" pada
	 * {@link PendaftarPklRenderer}; nilainya dibaca sekali di {@link #displayPrasyaratPkl} sehingga
	 * hanya layak dipakai sebagai kendali tampilan (UI-state), bukan sebagai gerbang keamanan.
	 * Tombol "Upload" (impor Excel), tombol hapus per baris, dan tombol "Hitung Skor" TIDAK membaca
	 * field ini — masing-masing memeriksa ulang haknya sendiri secara independen langsung lewat
	 * {@code CommonPrivilages.checkPrevilages(...)} di dalam listener-nya ({@code APPROVE},
	 * {@code DELETE}, dan {@code UPDATE}) tepat sebelum menulis data, dan menolak dengan pesan
	 * {@link MyMessageboxConfig} bila gagal.
	 * </p>
	 */
	private boolean approve;

	/** Filter tahun angkatan mahasiswa; kosong ({@code null}) berarti semua angkatan. */
	private Intbox angkatan;

	/** Checkbox "Belum diterima": bila dicentang, kriteria dipersempit ke {@code terima = 0} ({@link MahasiswaDaftarPkl#BELUM_DIPROSES}) saja. Perubahan centangnya langsung memicu {@link #loadData(Object)}. */
	private MyCheckboxConfig hanyaYgBelumDiterima;

	/**
	 * Perender satu baris grid pendaftar PKL. Setiap baris mengisi sembilan kolom yang dipasang
	 * {@link PendaftarPklHelper#displayPrasyaratPkl}: NIM, nama, jurusan, SKS/SKSK, IP/IPK, skor
	 * seleksi, "Memenuhi Syarat", checkbox penerimaan, dan kelompok tombol cetak
	 * kartu/ubah/hapus.
	 *
	 * <p>
	 * Nilai SKS dan IP/IPK tidak diambil dari kolom tersimpan, melainkan dihitung ulang setiap
	 * render lewat {@link Common#singkronkanKrsMahasiswa} untuk semester yang diturunkan dari
	 * tahun akademik dan semester {@link Pkl} terkait — sehingga angkanya selalu mencerminkan KRS
	 * terkini, bukan kondisi saat mahasiswa mendaftar.
	 * </p>
	 */
	class PendaftarPklRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris pendaftar PKL ke dalam {@code row}.
		 *
		 * <p>
		 * Beberapa perilaku yang perlu diperhatikan:
		 * </p>
		 * <ul>
		 *   <li><b>Kolom "Memenuhi Syarat" (sudah ditambal).</b> Label untuk kolom ketujuh dibuat dan
		 *   dipasang ke baris, lalu diisi dari kolom tersimpan
		 *   {@link MahasiswaDaftarPkl#getMemenuhiSyarat()} — sejajar dengan kembarannya
		 *   {@code PendaftarKknHelper.PendaftarKknRenderer} yang pada titik yang sama memanggil
		 *   {@code setValue(mahasiswaDaftarKkn.getMemenuhiSyarat() ? "Ya" : "Tidak")}. Kolom ini
		 *   ditulis oleh {@code ais.action.master.pkl.PklUntukMahasiswaAction#daftar}; nilainya
		 *   ditampilkan apa adanya di sini, tidak dihitung ulang.</li>
		 *   <li><b>Penghitungan semester berbiaya.</b> Semester akademik mahasiswa dihitung dari
		 *   tahun angkatan, semester/tahun akademik PKL, dan riwayat pindah kampus, lalu dipakai
		 *   memanggil {@link Common#singkronkanKrsMahasiswa} — pemanggilan ini dapat menulis ke
		 *   basis data (sinkronisasi KRS), jadi render grid tidak sepenuhnya bebas efek
		 *   samping.</li>
		 *   <li><b>Checkbox "Terima"</b> langsung menyimpan perubahan ke basis data pada tiap klik
		 *   ({@link Common#refreshUpdate}) tanpa dialog konfirmasi maupun jejak audit; ia hanya
		 *   dinonaktifkan (bukan disembunyikan) bila {@link PendaftarPklHelper#approve} bernilai
		 *   {@code false}.</li>
		 *   <li><b>Tombol hapus</b> menghapus baris pendaftaran secara permanen; sebelum menghapus,
		 *   listener-nya memeriksa ulang hak {@code CommonPrivilages.DELETE} pengguna saat ini
		 *   (independen dari {@link PendaftarPklHelper#approve}) dan membatalkan dengan pesan
		 *   peringatan bila gagal. Kegagalan karena relasi tetap ditangani dengan pesan kesalahan
		 *   terpisah.</li>
		 * </ul>
		 *
		 * @param row  baris ZK tujuan; sel ditambahkan berurutan sebagai anak {@code row}
		 * @param data elemen model, harus berupa {@link MahasiswaDaftarPkl}
		 * @throws Exception bila render sel atau pemanggilan sinkronisasi KRS gagal
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MahasiswaDaftarPkl mahasiswaDaftarPkl = (MahasiswaDaftarPkl) data;

			final Mahasiswa mahasiswa = mahasiswaDaftarPkl.getMahasiswa();

			new Label(mahasiswa.getNim()).setParent(row);
			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String semesterMulai = mahasiswaDaftarPkl.getPkl().getSemester();
			String ta = mahasiswaDaftarPkl.getPkl().getTahunAkademik();
			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			Integer semester = Common.getSemester(tahunAngkatanMhs, semesterMulai,
					mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
			new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil()) + " / "
					+ Common.numberFormat.get().format(krsMahasiswa.getSksk())).setParent(row);
			new Label(Common.numberFormat.get().format(krsMahasiswa.getIps()) + " / "
					+ Common.numberFormat.get().format(krsMahasiswa.getIpk())).setParent(row);

			new Label(Common.numberFormat.get().format(mahasiswaDaftarPkl.getTotalSkor())).setParent(row);

			final Label labelmemenuhiSyarat = new Label();
			labelmemenuhiSyarat.setParent(row);
			labelmemenuhiSyarat.setValue(mahasiswaDaftarPkl.getMemenuhiSyarat() ? "Ya" : "Tidak");

			final MyCheckboxConfig labelTelahTerpenuhi = new MyCheckboxConfig("Terima");
			labelTelahTerpenuhi.setDisabled(!approve);
			labelTelahTerpenuhi.setParent(row);
			labelTelahTerpenuhi.setChecked(mahasiswaDaftarPkl.getTerima().equals(MahasiswaDaftarPkl.DITERIMA));

			labelTelahTerpenuhi.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswaDaftarPkl.setTerima(labelTelahTerpenuhi.isChecked() ? MahasiswaDaftarPkl.DITERIMA
							: MahasiswaDaftarPkl.BELUM_DIPROSES);
					Common.refreshUpdate(mahasiswaDaftarPkl);
				}
			});

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("", "/img/print.png");
			cetak.setOrient("vertical");
			cetak.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("id_mahasiswa", mahasiswa.getId());
					parameters.put("id_pkl", pkl.getId());
					mahasiswa.putPhoto(parameters);
					Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_pkl",
							ais.ui.util.WaktuUtil.getDate());
				}
			});
			cetak.setParent(toolbar);

			final MyToolbarbuttonConfig buttonEdit = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			buttonEdit.setTooltiptext("Ubah Data");
			buttonEdit.setParent(toolbar);
			buttonEdit.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PklUntukMahasiswaAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
						}
					}, mahasiswaDaftarPkl.getPkl(), mahasiswa);
				}

			});

			final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			buttonDelete.setTooltiptext("Hapus Data");
			buttonDelete.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										// Gerbang diperiksa ulang di sini (server-side): tombol ini sebelumnya
										// dipasang tanpa memeriksa hak sama sekali; event onClick dapat dipicu
										// ke komponen ini di luar jalur render normal.
										if (!CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE)) {
											MyMessageboxConfig.show(
													"Mohon maaf, Anda tidak memiliki hak untuk menghapus data pendaftaran ini.",
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}
										try {

											Common.refreshDelete(HibernateUtil.currentSession(), mahasiswaDaftarPkl);

											loadData(null);
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
			buttonDelete.setParent(toolbar);

		}

	}

	/**
	 * Membangun kriteria Hibernate {@link MahasiswaDaftarPkl} untuk PKL yang sedang ditampilkan,
	 * memfilter sesuai status penerimaan, angkatan, NIM/nama, dan jurusan/fakultas yang dipilih
	 * pada toolbar.
	 *
	 * <p>
	 * Struktur kriteria: kriteria akar dibatasi {@code pkl = }{@link #pkl} dan (opsional)
	 * {@code terima = 0}, lalu berpindah ke sub-kriteria {@code mahasiswa} tempat filter
	 * angkatan/NIM/nama/jurusan dipasang, dan terakhir membuka sub-kriteria {@code jurusan}
	 * secara {@code LEFT_JOIN} untuk filter fakultas. Karena {@code addOrder} dipanggil setelah
	 * berpindah ke sub-kriteria {@code mahasiswa}, pengurutan berlaku atas kolom mahasiswa
	 * ({@code tahunangkatan}, {@code nim}), bukan kolom pendaftaran.
	 *
	 * <p>
	 * Cabang "tanpa filter" diwujudkan dengan {@code Restrictions.sqlRestriction("true")} —
	 * predikat yang selalu benar — sehingga rantai {@code add(...)} tetap seragam. Perlu dicatat
	 * bahwa cabang default filter jurusan/fakultas bukan "tanpa syarat" melainkan
	 * {@code isNotNull}: mahasiswa yang belum punya jurusan (atau jurusannya belum punya
	 * fakultas) tidak akan pernah muncul di grid ini meski baris pendaftarannya ada.
	 *
	 * <p>
	 * Kriteria ini <b>tidak</b> menambahkan pembatasan cakupan satuan kerja/tenant apa pun di
	 * luar penyaringan per-{@link #pkl}; lihat catatan pada field {@link #pkl}.
	 *
	 * @param order bila {@code true}, menambahkan pengurutan tahun angkatan menurun lalu NIM menaik
	 * @return kriteria siap dieksekusi (belum dibatasi jumlah baris)
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MahasiswaDaftarPkl.class).add(Restrictions.eq("pkl", pkl))
				.add(hanyaYgBelumDiterima.isChecked() ? Restrictions.eq("terima", 0)
						: Restrictions.sqlRestriction("true"))
				.createCriteria("mahasiswa")

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", angkatan.getValue()))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("jurusan")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("fakultas")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));

		return criteria;
	}

	/**
	 * Memuat ulang halaman aktif grid pendaftar: menghitung ulang total baris untuk
	 * {@link #paging}, mengambil maksimum {@code Common.ROWS_COUNT_ON_PAGE_50} baris pada offset
	 * halaman aktif, lalu memasang model beserta {@link PendaftarPklRenderer} yang baru.
	 *
	 * <p>
	 * Implementasi kontrak {@link DataLoader#loadData(Object)} sehingga instance ini dapat
	 * diserahkan sebagai callback penyegaran kepada {@link AmbilDataMahasiswaSeleksiPklHelper}
	 * (lihat {@link #getDataloader()}). {@link #initCriteria(boolean)} sengaja dipanggil dua kali
	 * — sekali untuk pencacahan tanpa pengurutan, sekali untuk pengambilan data dengan pengurutan
	 * — karena satu objek {@link Criteria} tidak dapat dipakai ulang setelah diberi proyeksi
	 * pencacahan.
	 *
	 * @param value tidak dipakai; ada hanya untuk memenuhi tanda tangan {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.initPaging50(initCriteria(false), paging);

		List<MahasiswaDaftarPkl> mahasiswaDaftarPkl = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswaDaftarPkl);
		grid.setRowRenderer(new PendaftarPklRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menyediakan instance ini sebagai {@link DataLoader} untuk diserahkan ke dialog pemilihan
	 * mahasiswa ({@link AmbilDataMahasiswaSeleksiPklHelper}), agar dialog tersebut dapat memicu
	 * {@link #loadData(Object)} dan menyegarkan grid setelah pendaftar baru ditambahkan.
	 *
	 * <p>
	 * Method pembungkus ini diperlukan karena {@code this} di dalam kelas anonim
	 * {@code EventListener} merujuk ke listener, bukan ke helper.
	 *
	 * @return objek helper ini sendiri
	 */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun tombol toolbar yang, saat diklik, mengekspor seluruh {@link MahasiswaDaftarPkl}
	 * hasil {@code dataCriteria.initCriteria(true)} (hingga 1.048.576 baris) ke berkas Excel
	 * sementara di folder {@code /tmp}, dijalankan di thread terpisah dengan indikator progres.
	 * Setelah selesai, berkas ditampilkan dalam jendela pratinjau spreadsheet dengan tombol unduh.
	 *
	 * <p>
	 * <b>Struktur kolom.</b> Tujuh kolom tetap di depan — ID, NIM, Nama, Jurusan, Fakultas,
	 * Diterima, Skor — diikuti satu kolom per {@link PersyaratanPkl} (diurutkan menurut nama lalu
	 * label inputan). Isi kolom persyaratan mengikuti
	 * {@link PersyaratanPkl#getTipeDataInputan()}: teks/teks-angka dan pilihan custom memakai
	 * {@code nilaiString}, tanggal memakai {@code nilaiTanggal}, angka memakai {@code nilaiNumber},
	 * dan ya/tidak memakai {@code nilaiBoolean}. Tipe yang tidak dikenali menuliskan nama
	 * persyaratannya sendiri sebagai isi sel, bukan nilai jawaban mahasiswa.
	 *
	 * <p>
	 * <b>Efek samping penulisan data.</b> Selama ekspor, setiap kombinasi mahasiswa-persyaratan
	 * yang belum punya baris {@link MahasiswaPklPersyaratan} akan <i>dibuat</i> (kosong) dan
	 * disimpan. Jadi tombol "Download" bukan operasi baca-saja: menekannya dapat menambah baris
	 * jawaban kosong ke basis data.
	 *
	 * <p>
	 * <b>Perbedaan dari kembarannya di modul KKN (sudah ditambal).</b> Pencarian baris
	 * {@link MahasiswaPklPersyaratan} di sini sebelumnya memanggil {@code uniqueResult()} tanpa
	 * {@code addOrder(desc(id))} maupun {@code setMaxResults(1)}, sedangkan
	 * {@code PendaftarKknHelper#cetakDataCustomButton} sudah memasang kedua pembatas tersebut.
	 * Bila pernah tercipta lebih dari satu baris jawaban untuk kombinasi mahasiswa-pkl-persyaratan
	 * yang sama, pemanggilan tanpa pembatas itu melempar {@code NonUniqueResultException} yang
	 * ditangkap per-sel sehingga sel bersangkutan kosong tanpa peringatan ke pengguna. Kini kedua
	 * pembatas yang sama dipasang di sini, sejajar dengan kembarannya.
	 *
	 * <p>
	 * <b>Model konkurensi.</b> Pembuatan berkas berjalan di {@link Thread} terpisah, sementara
	 * thread ZK memantau kemajuannya lewat {@link org.zkoss.zul.Timer} 200&nbsp;ms yang membaca
	 * teks sebuah {@link Label} sebagai kanal status: teks kosong berarti selesai (buka
	 * pratinjau), teks {@code "-"} berarti gagal. Konsekuensinya, thread latar memakai
	 * {@link HibernateUtil#currentSession()} di luar konteks permintaan ZK dan
	 * {@link StreamingHibernateUtil} untuk pembacaan lampiran; kegagalan per-sel ditelan dan
	 * hanya ditampilkan bila pengguna berstatus admin.
	 *
	 * @param dataCriteria sumber kriteria data yang akan diekspor (biasanya {@code this})
	 * @param buttonLabel  label tombol toolbar
	 * @param buttonImage  ikon tombol toolbar
	 * @return tombol toolbar siap ditambahkan ke {@link Toolbar} pemanggil
	 */
	@SuppressWarnings("unchecked")
	public MyToolbarbuttonConfig cetakDataCustomButton(final DataCriteria dataCriteria, String buttonLabel,
			String buttonImage) {

		Session session = HibernateUtil.currentSession();
		final List<PersyaratanPkl> persyaratanPkls = session.createCriteria(PklPunyaPersyaratan.class)
				.createAlias("persyaratanPkl", "persyaratanPkl").add(Restrictions.eq("pkl", pkl))
				.setProjection(Projections.property("persyaratanPkl")).addOrder(Order.asc("persyaratanPkl.nama"))
				.addOrder(Order.asc("persyaratanPkl.labelInputan")).list();

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());
								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(persyaratanPkls.size() + 7);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PendaftarPklHelper.java:362");

										}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {

							try {
								Object d = dataCriteria == null ? null : dataCriteria.initCriteria(true);
								@SuppressWarnings("rawtypes")
								List<MahasiswaDaftarPkl> data = (d != null && d instanceof Criteria)
										? ((Criteria) d).setMaxResults(1048576).list()
										: (List) d;
								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();

								XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
								lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// lockedNumericStyle.setLocked(true);

								XSSFCellStyle hlink_style = workbook.createCellStyle();
								XSSFFont hlink_font = workbook.createFont();
								hlink_font.setUnderline(XSSFFont.U_SINGLE);
								hlink_font.setColor(new XSSFColor(Color.BLUE));
								hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								hlink_style.setFont(hlink_font);

								XSSFCellStyle notLocked = workbook.createCellStyle();
								notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// notLocked.setLocked(false);

								XSSFSheet sheet = workbook.createSheet("CETAK DATA");
								// sheet.protectSheet("passwordrahasia");
								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);

								rowhead.createCell(0).setCellValue("ID");

								rowhead.createCell(1).setCellValue("NIM");
								rowhead.createCell(2).setCellValue("Nama");
								rowhead.createCell(3).setCellValue("Jurusan");
								rowhead.createCell(4).setCellValue("Fakultas");
								rowhead.createCell(5).setCellValue("Diterima");
								rowhead.createCell(6).setCellValue("Skor");

								for (int i = 7; i < persyaratanPkls.size() + 7; i++) {
									PersyaratanPkl persyaratanPkl = persyaratanPkls.get(i - 7);
									if (persyaratanPkl.getLabelInputan() == null
											|| persyaratanPkl.getLabelInputan().trim().isEmpty()) {
										rowhead.createCell(i).setCellValue(persyaratanPkl.getNama());
									} else {
										rowhead.createCell(i).setCellValue(persyaratanPkl.getLabelInputan());

									}
								}

								for (MahasiswaDaftarPkl o : data) {

									try {
										rowIndex++;
										if (o == null) {
											continue;
										}
										Mahasiswa mahasiswa = o.getMahasiswa();
										label.setValue("Sedang memproses data " + o.toString() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size()) + " %)");

										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(o.getId());

										cell = row.createCell(1);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getNim());

										cell = row.createCell(2);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getNama());

										cell = row.createCell(3);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getJurusan().getNama());

										cell = row.createCell(4);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getJurusan().getFakultas().getNama());

										cell = row.createCell(5);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getTerima().equals(1));

										cell = row.createCell(6);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(o.getTotalSkor());

										Session session = HibernateUtil.currentSession();
										for (int i = 7; i < persyaratanPkls.size() + 7; i++) {

											try {
												PersyaratanPkl persyaratanPkl = persyaratanPkls.get(i - 7);
												MahasiswaPklPersyaratan mahasiswaPklPersyaratan = (MahasiswaPklPersyaratan) session
														.createCriteria(MahasiswaPklPersyaratan.class)
														.add(Restrictions.eq("mahasiswa", mahasiswa))
														.add(Restrictions.eq("pkl", pkl))
														.addOrder(Order.desc("id")).setMaxResults(1)
														.add(Restrictions.eq("persyaratanPkl", persyaratanPkl))
														.uniqueResult();
												if (mahasiswaPklPersyaratan == null) {
													mahasiswaPklPersyaratan = new MahasiswaPklPersyaratan();
													mahasiswaPklPersyaratan.setMahasiswa(mahasiswa);
													mahasiswaPklPersyaratan.setPkl(pkl);
													mahasiswaPklPersyaratan.setPersyaratanPkl(persyaratanPkl);
													session.save(mahasiswaPklPersyaratan);
												}

												if (persyaratanPkl.getTipeDataInputan().equals(PersyaratanPkl.TEXT)
														|| persyaratanPkl.getTipeDataInputan()
																.equals(PersyaratanPkl.TEXT_ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaPklPersyaratan.getNilaiString() != null) {
														cell.setCellValue(mahasiswaPklPersyaratan.getNilaiString());
													}
												} else if (persyaratanPkl.getTipeDataInputan()
														.equals(PersyaratanPkl.TANGGAL)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													cell.setCellValue(
															mahasiswaPklPersyaratan.getNilaiTanggal() == null ? ""
																	: Common.dateFormat1.get().format(
																			mahasiswaPklPersyaratan.getNilaiTanggal()));

												} else if (persyaratanPkl.getTipeDataInputan()
														.equals(PersyaratanPkl.ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaPklPersyaratan.getNilaiNumber() != null) {
														cell.setCellValue(mahasiswaPklPersyaratan.getNilaiNumber());
													}

												} else if (persyaratanPkl.getTipeDataInputan()
														.equals(PersyaratanPkl.PILIHAN_YA_TIDAK)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaPklPersyaratan.getNilaiBoolean() != null) {
														cell.setCellValue(mahasiswaPklPersyaratan.getNilaiBoolean());
													}

												} else if (persyaratanPkl.getTipeDataInputan()
														.equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaPklPersyaratan.getNilaiString() != null) {
														cell.setCellValue(mahasiswaPklPersyaratan.getNilaiString());
													}

												} else {
													cell = row.createCell(i);
													if (persyaratanPkl.getLabelInputan() == null
															|| persyaratanPkl.getLabelInputan().trim().isEmpty()) {
														cell.setCellValue(persyaratanPkl.getNama());
													} else {
														cell.setCellValue(persyaratanPkl.getLabelInputan());

													}
												}

												if (persyaratanPkl.getHarusMenyertakanLampiran()) {
													cell.setCellStyle(hlink_style);
													try {
														Session streamingSession = StreamingHibernateUtil.getInstance()
																.currentSession();

														int jumlah = ((Number) streamingSession
																.createCriteria(LampiranPklMahasiswa.class)
																.setProjection(Projections.rowCount())
																.add(Restrictions.eq("persyaratanPkl",
																		mahasiswaPklPersyaratan.getId()))
																.setMaxResults(1).uniqueResult()).intValue();

														Long ids = (Long) (streamingSession
																.createCriteria(LampiranPklMahasiswa.class)
																.setProjection(Projections.property("id"))
																.add(Restrictions.eq("persyaratanPkl",
																		mahasiswaPklPersyaratan.getId()))
																.setMaxResults(1).uniqueResult());

														String url = CommonMedia.getFile(ids,
																LampiranPklMahasiswa.class.getName());

														if (jumlah > 0) {
															XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
															link.setAddress(url);
															cell.setHyperlink(link);
														}

													} catch (Exception e) {
														StreamingHibernateUtil.getInstance().rollbackTransaction();
													}

													StreamingHibernateUtil.getInstance().closeSession();
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println(
										"Your excel file has been generated! " );
								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
								label.setValue("-");
							}

						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mencetak/mengekspor data pendaftar PKL ke Excel",
							e, new String[] {
									"Muat ulang (refresh) halaman ini lalu coba cetak data kembali.",
									"Periksa apakah jumlah data yang akan diekspor tidak terlalu besar.",
									"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		return toolbarbutton;
	}

	/**
	 * Membangun panel lengkap pengelolaan pendaftar PKL ke dalam {@code component}, untuk
	 * {@code pkl} yang diberikan, lalu memuat data awal.
	 *
	 * <p>
	 * Isi toolbar yang dipasang, berurutan: kotak cari NIM/nama, combobox fakultas dan jurusan,
	 * isian angkatan, checkbox "Belum diterima", tombol "Cari", tombol "Pengecualian"
	 * ({@link PengecualianPklMahasiswaHelper} — hanya tampil bagi pengguna non-mahasiswa dan bila
	 * konfigurasi {@code tampilkan_pengecualian_pkl_mahasiswa_di_seleksi} aktif), tiga tombol
	 * cetak PDF, tombol "Hitung Skor", tombol "Baru"
	 * ({@link AmbilDataMahasiswaSeleksiPklHelper}), tombol "Download"
	 * ({@link #cetakDataCustomButton}), dan tombol "Upload".
	 *
	 * <p>
	 * <b>Tombol cetak dan berkas laporannya.</b> "Pendaftar" mencetak {@code pendaftar_pkl} dan
	 * "Penerima" mencetak {@code pendaftar_pkl_diterima} — keduanya tersedia di direktori
	 * laporan. Tombol <b>"Rekap" (sudah ditambal)</b> sebelumnya menunjuk nama laporan
	 * {@code penerima-pkl} (dengan tanda hubung) yang tidak ada di direktori laporan manapun,
	 * sehingga selalu gagal saat laporan hendak dihasilkan walaupun pemeriksaan "ada penerima" di
	 * atasnya lolos. Kini menunjuk {@code penerima_pkl} (dengan garis bawah), berkas
	 * {@code webapp/report/penerima_pkl.jrxml} baru yang di-port dari {@code penerima_kkn.jrxml}
	 * kembarannya ke tabel {@code mahasiswa_daftar_pkl}/{@code pkl}.
	 *
	 * <p>
	 * <b>"Hitung Skor"</b> menghitung ulang {@link MahasiswaDaftarPkl#setTotalSkor} untuk seluruh
	 * baris hasil filter saat ini. Skor dijumlahkan dari jawaban bertipe
	 * {@link PersyaratanPkl#PILIHAN_CUSTOM} yang nilainya berformat {@code "label:skor"}; segmen
	 * setelah titik dua di-parse sebagai bilangan bulat, dan jawaban yang tidak berformat demikian
	 * dihitung nol tanpa peringatan ke pengguna. Karena berbasis {@link #initCriteria(boolean)},
	 * tombol ini hanya menghitung ulang baris yang <i>sedang lolos filter</i>, bukan seluruh
	 * pendaftar PKL ini. Listener {@code onClick}-nya memeriksa ulang hak
	 * {@code CommonPrivilages.UPDATE} sebelum penghitungan dijalankan, independen dari parameter
	 * {@code approve}.
	 *
	 * <p>
	 * <b>"Upload" (impor Excel)</b> membaca kembali berkas berformat sama dengan hasil ekspor:
	 * kolom 0 = ID baris, kolom 1 = NIM, kolom 5 = status diterima. Baris dicocokkan lewat ID bila
	 * ada; bila tidak, baris {@link MahasiswaDaftarPkl} <i>baru</i> dibuat berdasarkan NIM lalu
	 * {@code terima} diisi dari kolom 5. Perlu diperhatikan:
	 * <ul>
	 *   <li>jalur ini melewati {@code PklUntukMahasiswaAction#daftar}, sehingga
	 *   {@code memenuhiSyarat} tidak pernah diisi dan pemeriksaan kuota/persyaratan akademik tidak
	 *   dijalankan;</li>
	 *   <li>listener {@code onUpload}-nya memeriksa ulang hak {@code CommonPrivilages.APPROVE}
	 *   (menulis {@code terima} setara keputusan penerimaan) tepat sebelum berkas diproses, dan
	 *   menolak dengan pesan peringatan bila gagal — pemeriksaan ini independen dari parameter
	 *   {@code approve} maupun status tampil/nonaktif checkbox "Terima" pada grid;</li>
	 *   <li>setiap baris di-commit satu per satu di thread terpisah — kegagalan di tengah berkas
	 *   meninggalkan sebagian data sudah tersimpan.</li>
	 * </ul>
	 *
	 * @param pkl       kegiatan PKL yang daftar pendaftarnya dikelola
	 * @param component kontainer ZK tujuan; isi sebelumnya dibersihkan lewat {@link Common#clear}
	 * @param window    jendela pemanggil, diteruskan ke {@link AmbilDataMahasiswaSeleksiPklHelper}
	 *                  saat menambah pendaftar baru
	 * @param approve   izinkan pengguna mengubah status terima/tolak langsung dari grid; lihat
	 *                  catatan cakupan pada field {@link #approve}
	 */
	public void displayPrasyaratPkl(final Pkl pkl, final Component component, final MyWindow window, boolean approve) {
		this.pkl = pkl;
		this.approve = approve;
		Common.clear(component);

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mendaftar pkl"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setWidth("");
		nim.setWidth("70px");
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		toolbar.appendChild(fakultas);
		fakultas.setWidth("70px");

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		toolbar.appendChild(jurusan);
		jurusan.setWidth("70px");

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setWidth("50px");

		toolbar.appendChild(hanyaYgBelumDiterima = new MyCheckboxConfig("Belum diterima"));
		hanyaYgBelumDiterima.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		Tbmuser tbmuser = Common.getCurrentUser();

		MyToolbarbuttonConfig pengecualian = new MyToolbarbuttonConfig("Pengecualian", "/img/svg/edit-box-line.svg");
		toolbar.appendChild(pengecualian);
		pengecualian.setVisible(tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_pengecualian_pkl_mahasiswa_di_seleksi"));
		pengecualian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PengecualianPklMahasiswaHelper pengecualianPklMahasiswaHelper = new PengecualianPklMahasiswaHelper(pkl);
				pengecualianPklMahasiswaHelper.display();
			}
		});

		button = new MyToolbarbuttonConfig("Pendaftar", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			final Map parameters = ais.common.HashMapGenerator.getRand();

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPendaftar = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarPkl.class).add(Restrictions.eq("pkl", pkl))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				if (countPendaftar == 0) {
					MyMessageboxConfig.show("Tidak Ada Pendaftar", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_pkl", pkl.getId());
				// parameters
				// .put("jurusan", fakultas.getSelectedItem().getValue());
				// parameters.put("fakultas", fakultas.getSelectedItem()
				// .getValue());
				Report.generatePDFReport(Report.PDF, parameters, "pendaftar_pkl", ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Penerima", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			final Map parameters = ais.common.HashMapGenerator.getRand();

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPenerima = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarPkl.class).add(Restrictions.eq("pkl", pkl))
						.add(Restrictions.eq("terima", 1)).setProjection(Projections.rowCount()).uniqueResult())
								.intValue();

				if (countPenerima == 0) {
					MyMessageboxConfig.show("Tidak Ada Mahasiswa yang Diterima di Pkl Ini", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_pkl", pkl.getId());
				Report.generatePDFReport(Report.PDF, parameters, "pendaftar_pkl_diterima",
						ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			final HashMap<String, Long> parameters = new HashMap<String, Long>();

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPenerima = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarPkl.class).add(Restrictions.eq("pkl", pkl))
						.add(Restrictions.eq("terima", 1)).setProjection(Projections.rowCount()).uniqueResult())
								.intValue();

				if (countPenerima == 0) {
					MyMessageboxConfig.show("Tidak Ada Mahasiswa yang Diterima di Pkl Ini", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_pkl", pkl.getId());
				Report.generatePDFReport(Report.PDF, parameters, "penerima_pkl", ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hitung Skor", "/img/excel.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// Gerbang diperiksa di sini (server-side): tombol ini sebelumnya dipasang tanpa
				// memeriksa hak sama sekali, sehingga siapa pun yang membuka layar ini dapat
				// menimpa totalSkor seluruh baris yang lolos filter.
				if (!CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE)) {
					MyMessageboxConfig.show(
							"Mohon maaf, Anda tidak memiliki hak untuk menghitung ulang skor seleksi ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MahasiswaDaftarPkl> mahasiswaDaftarPkls = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();
						for (MahasiswaDaftarPkl mahasiswaDaftarPkl : mahasiswaDaftarPkls) {

							List<MahasiswaPklPersyaratan> mahasiswaPklPersyaratans = session
									.createCriteria(MahasiswaPklPersyaratan.class)
									.add(Restrictions.eq("mahasiswa", mahasiswaDaftarPkl.getMahasiswa()))
									.add(Restrictions.eq("pkl", mahasiswaDaftarPkl.getPkl()))
									.createAlias("persyaratanPkl", "persyaratanPkl").add(Restrictions
											.eq("persyaratanPkl.tipeDataInputan", PersyaratanPkl.PILIHAN_CUSTOM))
									.list();
							Integer totalSkor = 0;
							for (MahasiswaPklPersyaratan mahasiswaPklPersyaratan : mahasiswaPklPersyaratans) {
								String val = mahasiswaPklPersyaratan.getNilaiString() == null ? ""
										: mahasiswaPklPersyaratan.getNilaiString().trim();
								String[] kol = StringUtils.split(val, ":");
								Integer skor = 0;
								try {
									skor = Integer.parseInt(kol[1].trim());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PendaftarPklHelper.java:823");

								}
								totalSkor += skor;
							}
							mahasiswaDaftarPkl.setTotalSkor(totalSkor);

							Common.refreshSaveOrUpdate(session, mahasiswaDaftarPkl);
						}
						loadData(null);
					}
				});
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Baru", "/img/new.gif");
		button.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				try {
					new AmbilDataMahasiswaSeleksiPklHelper().display(pkl, getDataloader(), window);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PendaftarPklHelper membuka dialog tambah pendaftar PKL");
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Form tambah pendaftar PKL belum dapat dibuka. Silakan coba kembali.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton(this, "Download", "/img/excel.png");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// Gerbang diperiksa di sini (server-side), sebelum berkas apa pun disimpan/diproses:
				// tombol ini sebelumnya dipasang tanpa memeriksa hak sama sekali, sehingga pengguna
				// tanpa hak APPROVE (checkbox "Terima" tampak nonaktif baginya) tetap dapat menulis
				// kolom terima secara massal lewat unggahan Excel.
				if (!CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE)) {
					MyMessageboxConfig.show(
							"Mohon maaf, Anda tidak memiliki hak untuk menetapkan status penerimaan lewat upload data.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										Session session = HibernateUtil.currentNativeSession();
										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												String nim = Common.getSheetContentAsString(sheet, 1, i);
												Boolean diterima = Common.getSheetContentAsBoolean(sheet, 5, i);
												if (nim == null || nim.trim().isEmpty() || diterima == null) {
													continue;
												}

												Mahasiswa mahasiswa = ConstantValues.ambilByNim(nim);
												if (mahasiswa == null) {
													continue;
												}

												MahasiswaDaftarPkl biodataCalonMahasiswa = (MahasiswaDaftarPkl) (id == null
														? null
														: session.createCriteria(MahasiswaDaftarPkl.class)
																.add(Restrictions.idEq(id)).uniqueResult());
												if (biodataCalonMahasiswa == null) {
													biodataCalonMahasiswa = new MahasiswaDaftarPkl();
													biodataCalonMahasiswa.setNama(nim);
													biodataCalonMahasiswa.setMahasiswa(mahasiswa);
													biodataCalonMahasiswa.setPkl(pkl);
													biodataCalonMahasiswa.setTanggalDaftar(new Date());
												}

												biodataCalonMahasiswa.setTerima(diterima ? 1 : 0);

												session.getTransaction().begin();
												session.saveOrUpdate(biodataCalonMahasiswa);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + biodataCalonMahasiswa.getNama()
														+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount)
														+ " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/PendaftarPklHelper.java:973");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS/SKSK");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("IP/IPk");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skor");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Memenuhi Syarat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Terima/Tidak");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah/Hapus");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	/**
	 * Mengubah status terima/tolak pendaftaran PKL seorang mahasiswa (baris
	 * {@link MahasiswaDaftarPkl} terbaru untuk kombinasi mahasiswa-pkl tersebut) dan menampilkan
	 * pesan konfirmasi informatif sesuai hasilnya.
	 *
	 * <p>
	 * <b>Tidak ada pemanggil di dalam basis kode saat ini</b> — method ini tersedia sebagai API
	 * bantu bagi layar lain, dan sengaja dipertahankan apa adanya.
	 *
	 * <p>
	 * Perilaku yang perlu diketahui bila hendak dipakai: method ini <i>tidak</i> memeriksa
	 * {@link #approve} maupun hak apa pun, dan hasil query tidak diperiksa {@code null} sehingga
	 * mahasiswa yang belum pernah mendaftar PKL ini menyebabkan {@link NullPointerException},
	 * bukan pesan kesalahan yang ramah. Parameter {@code pkl} bersifat lokal dan tidak mengubah
	 * field {@link #pkl}.
	 *
	 * @param mahasiswa mahasiswa yang statusnya diubah
	 * @param pkl       PKL terkait
	 * @param checked   {@code true} untuk menerima, {@code false} untuk menolak
	 * @throws Exception bila penyimpanan gagal
	 */
	public void terimaPkl(Mahasiswa mahasiswa, Pkl pkl, boolean checked) throws Exception {
		Session session = HibernateUtil.currentSession();
		MahasiswaDaftarPkl mahasiswaDiterimaPklIni = (MahasiswaDaftarPkl) session
				.createCriteria(MahasiswaDaftarPkl.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("pkl", pkl)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

		if (checked) {

			mahasiswaDiterimaPklIni.setTerima(1);
			Common.refreshUpdate(session, mahasiswaDiterimaPklIni);
			MyMessageboxConfig.show("Mahasiswa " + mahasiswa.getNama() + " / " + mahasiswa.getNim()
					+ " diterima untuk pkl " + pkl.getNama(), "INFORMASI", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;

		}

		if (!checked) {
			mahasiswaDiterimaPklIni.setTerima(0);
			Common.refreshUpdate(session, mahasiswaDiterimaPklIni);
			MyMessageboxConfig.show("Mahasiswa " + mahasiswa.getNama() + " / " + mahasiswa.getNim()
					+ " ditolak untuk pkl " + pkl.getNama(), "INFORMASI", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

	}

}
