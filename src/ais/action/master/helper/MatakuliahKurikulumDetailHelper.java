package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.RencanaTahunAkademikAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.KurikulumPunyaMatakuliahDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.KurikulumPunyaMatakuliahPunyaItem;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper composer untuk mengelola Rencana Pembelajaran Semester (RPS) tingkat KURIKULUM — daftar
 * {@link KurikulumPunyaMatakuliahDetail} (satu baris per pertemuan ke-1..N: topik, indikator,
 * metode/waktu pembelajaran, pengalaman belajar, tugas & penilaian, buku rujukan, status
 * pertemuan) milik satu {@link KurikulumPunyaMatakuliah} (matakuliah dalam konteks kurikulum
 * tertentu). Ini adalah TEMPLATE rencana pembelajaran yang kemudian dapat "disalin" menjadi baris
 * {@link Pertemuan} nyata pada satu {@link Perkuliahan} (kelas) tertentu lewat {@link #simpan}.
 *
 * <p>
 * Tampilan utama ({@link #display}) berupa tabbox: tab "Rencana Pembelajaran" (grid RPS ini,
 * ATAU — bila kurikulum memakai skema OBE/Outcome-Based Education pada tahun ajaran & ganjil-genap
 * yang berlaku, dicek lewat {@link ais.database.model.Kurikulum#apakahObe} — sebuah iframe
 * {@code /pages/master/rps_obe.zul} yang menggantikan grid biasa sepenuhnya), serta tab lazy-load
 * File/Buku Referensi/Buku Ajar/Artikel/Audio/Video (masing-masing menampilkan jumlah item pada
 * labelnya, dimuat hanya saat tab pertama kali diklik).
 * </p>
 *
 * <p>
 * <b>Pembuatan massal pertemuan RPS</b> ({@link #tampilTombolBuatKurikulumPunyaMatakuliahDetail}):
 * dialog "Buat Rencana Pembelajaran" menghasilkan N baris {@link KurikulumPunyaMatakuliahDetail}
 * sekaligus sesuai jumlah pertemuan yang diminta, secara otomatis menandai pertemuan di
 * pertengahan sebagai UTS dan pertemuan terakhir sebagai UAS (bila dicentang), dan opsional
 * menghapus seluruh rencana lama lebih dulu. Idempoten terhadap nomor urut yang sudah ada (tidak
 * menimpa baris yang sudah dibuat manual).
 * </p>
 *
 * <p>
 * <b>Penyalinan ke kelas nyata</b> ({@link #simpan(Perkuliahan, KurikulumPunyaMatakuliah, List,
 * Date, boolean)}, static): untuk setiap {@link KurikulumPunyaMatakuliahDetail}, dibuatkan (atau
 * dipakai ulang bila sudah ada) satu baris {@link Pertemuan} pada {@code perkuliahan}, dengan
 * tanggal dihitung mingguan (+7 hari) berturut-turut mulai dari {@code tgl} yang diberikan; bila
 * {@code lewatiTanggalMerahNasional} aktif, tanggal yang jatuh pada hari libur/tanggal merah
 * nasional dilompati ({@link Common#isHolidayMerahDanAtauHariLibur}). Lampiran (silabus/SAP milik
 * kurikulum, serta file/audio/video milik setiap detail RPS) turut disalin ke perkuliahan/
 * pertemuan hasil lewat dua overload {@link #copyLampiran} — SEBAGAI SALINAN baru (bukan referensi
 * bersama), ditandai {@code copyDari} ke sumber aslinya.
 * </p>
 *
 * <p>
 * Editing inline (topik, indikator, dsb.) pada grid RPS hanya diizinkan untuk pengguna yang BUKAN
 * mahasiswa/siswa/dosen (mis. admin); pengguna lain melihat versi read-only (label). Setiap baris
 * juga menampilkan ringkasan jumlah lampiran file/audio/video yang sudah tertaut lewat
 * {@link #createKeterangan}.
 * </p>
 *
 * <p>
 * <b>Pembagian tanggung jawab dengan {@link MatakuliahKurikulumHelper}.</b> Kedua kelas bekerja
 * pada dua tingkat yang berbeda dalam hierarki kurikulum dan tidak saling menggantikan:
 * </p>
 * <ul>
 * <li>{@link MatakuliahKurikulumHelper} mengelola tingkat <i>matakuliah</i>: grid
 * {@link KurikulumPunyaMatakuliah} milik satu {@link ais.database.model.Kurikulum} pada satu
 * semester (SKS, tahap, status, jumlah jadwal, aktif/nonaktif, integrasi Neo Feeder).</li>
 * <li>Kelas INI mengelola tingkat <i>pertemuan</i>: grid
 * {@link KurikulumPunyaMatakuliahDetail} milik SATU {@link KurikulumPunyaMatakuliah}, dan
 * dibuka sebagai baris detail dari grid milik kelas tersebut (lihat pemakaiannya di
 * {@code MatakuliahKurikulumHelper}).</li>
 * </ul>
 *
 * <p>
 * Keduanya kebetulan memiliki method bernama SAMA,
 * {@code tampilTombolBuatKurikulumPunyaMatakuliahDetail(Toolbar, EventListener)}, dengan tanda
 * tangan identik namun cakupan dan kemampuan yang berbeda &mdash; perbedaan ini penting agar
 * tidak tertukar:
 * </p>
 * <table border="1" summary="Perbandingan tombol pembuatan RPS pada kedua helper">
 * <tr><th></th><th>MatakuliahKurikulumHelper</th><th>kelas ini</th></tr>
 * <tr><td>Label tombol</td><td>"Generate Rencana Pembelajaran"</td>
 *     <td>"Buat Rencana Pembelajaran"</td></tr>
 * <tr><td>Cakupan</td><td>MASSAL &mdash; mengulang seluruh isi daftar
 *     {@code kurikulumPunyaMatakuliahs} hasil penyaringan grid, jadi banyak matakuliah
 *     sekaligus</td>
 *     <td>TUNGGAL &mdash; hanya {@link #kurikulumPunyaMatakuliah} yang sedang dibuka</td></tr>
 * <tr><td>Jumlah pertemuan</td><td>diambil dari nilai tersimpan tiap matakuliah
 *     ({@code getJumlahPertemuanPerkuliahanDefault()}); pengguna tidak dapat mengubahnya di
 *     dialog</td>
 *     <td>diketik pengguna pada dialog, dan nilainya DISIMPAN kembali ke matakuliah</td></tr>
 * <tr><td>Metadata matakuliah</td><td>tidak disentuh</td>
 *     <td>ikut disunting: deskripsi pembelajaran, capaian/kompetensi prodi, flag inti,
 *     institusional, dan bobot tugas &mdash; plus unggah lampiran Silabus</td></tr>
 * </table>
 *
 * <p>
 * Perilaku yang SAMA pada keduanya: pertemuan dengan {@code nomorUrut} yang sudah ada dilewati
 * (idempoten, tidak menimpa hasil suntingan manual); penanda UTS diletakkan pada
 * {@code i == jumlah / 2} memakai pembagian bilangan bulat &mdash; sehingga untuk 15 pertemuan
 * UTS jatuh di pertemuan ke-7, dan untuk jumlah pertemuan 1 tidak ada pertemuan yang ditandai
 * UTS sama sekali; serta opsi "Hapus pertamuan yang sebelumnya sudah ada" yang dijalankan
 * sebagai {@code delete} SQL native langsung ke tabel, melewati kaskade Hibernate maupun
 * pencatatan revisi Envers.
 * </p>
 *
 * <p>
 * <b>Penjagaan akses.</b> Seluruh penjagaan di kelas ini bersifat tampilan: privilese
 * {@link #add}/{@link #delete} dan pengecekan peran {@link #tbmuser} (bukan mahasiswa, bukan
 * siswa, bukan dosen) hanya menyembunyikan atau menonaktifkan komponen ZK. Method statis
 * {@link #simpan(Perkuliahan, KurikulumPunyaMatakuliah, java.util.List, Date, boolean)} dan
 * kedua {@link #copyLampiran} tidak memeriksa peran maupun privilese sama sekali &mdash;
 * keduanya memang dipanggil dari alur penjadwalan ({@code PenjadwalanHelper},
 * {@code PenjadwalanUtil}) yang menegakkan penjagaannya sendiri.
 * </p>
 */
public class MatakuliahKurikulumDetailHelper implements DataLoader {

	/**
	 * Grid baris RPS (satu baris per {@link KurikulumPunyaMatakuliahDetail} / per pertemuan).
	 * Dibuat di {@link #display} HANYA pada cabang non-OBE &mdash; bila kurikulum memakai skema OBE,
	 * grid digantikan iframe {@code rps_obe.zul} dan field ini tetap {@code null}, sehingga
	 * {@link #loadData(Object)} tidak boleh dipanggil pada mode itu.
	 */
	private MyGrid grid;
	/**
	 * Matakuliah-dalam-kurikulum yang RPS-nya sedang dikelola &mdash; induk dari seluruh baris
	 * {@link KurikulumPunyaMatakuliahDetail} di grid. Ditetapkan sekali di {@link #display} dan
	 * menjadi penyaring wajib pada {@link #loadData(Object)}, tombol pembuatan massal, ekspor,
	 * impor, serta penghitungan jumlah item pada label tiap tab.
	 */
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	/**
	 * Daftar baris RPS hasil {@link #loadData(Object)} terakhir. Selain menjadi model grid, daftar
	 * ini juga menjadi CAKUPAN KERJA dua operasi lain: tombol "Hapus Semua Rencana Pembelajaran"
	 * (menghapus persis isi daftar ini) dan {@link #simpan(Perkuliahan)} (menyalin persis isi
	 * daftar ini menjadi baris {@link Pertemuan}). Karena {@link #loadData(Object)} memuat seluruh
	 * detail milik {@link #kurikulumPunyaMatakuliah} tanpa paging basis data, isinya memang seluruh
	 * RPS matakuliah tersebut. Tetap {@code null} bila {@link #loadData(Object)} belum pernah
	 * dipanggil &mdash; termasuk pada mode OBE.
	 */
	private List<KurikulumPunyaMatakuliahDetail> kurikulumPunyaMatakuliahDetails = null;

	/**
	 * Sub-helper tab "Video", dibuat di konstruktor dengan mode (tambah=true, hapus=false).
	 * Isinya baru dirender saat tab Video diklik pertama kali (lazy-load) di {@link #display}.
	 */
	private VideoPertemuanHelper videoPertemuanHelper;
	/**
	 * Sub-helper tab "Audio", dibuat di konstruktor dengan mode (tambah=true, hapus=false).
	 * Isinya baru dirender saat tab Audio diklik pertama kali (lazy-load) di {@link #display}.
	 */
	private AudioPertemuanHelper audioPertemuanHelper;
	/**
	 * Sub-helper tab "File", dibuat di konstruktor tanpa konteks perkuliahan/pertemuan
	 * ({@code new FilePerkuliahanHelper(null, null)}); konteks kurikulum baru diberikan saat tab
	 * File diklik pertama kali (lazy-load) di {@link #display}.
	 */
	private FilePerkuliahanHelper filePerkuliahanHelper;

	/**
	 * Hak tambah pengguna ({@link CommonPrivilages#CREATE}), dibaca sekali di konstruktor. Dipakai
	 * bersama pengecekan peran ({@link #tbmuser} bukan mahasiswa/siswa/dosen) untuk menentukan
	 * tampil-tidaknya tombol "Buat Rencana Pembelajaran".
	 */
	private Boolean add = false;
	/**
	 * Hak hapus pengguna ({@link CommonPrivilages#DELETE}), dibaca sekali di konstruktor; mengatur
	 * tampil-tidaknya tombol hapus per baris dan tombol "Hapus Semua Rencana Pembelajaran".
	 */
	private Boolean delete = false;

	/**
	 * Pengguna yang sedang login, dibaca sekali saat instance dibuat (inisialisasi field, jadi
	 * sebelum badan konstruktor berjalan). Menjadi dasar penjagaan peran di seluruh kelas ini:
	 * hanya pengguna yang BUKAN mahasiswa, BUKAN siswa, dan BUKAN dosen yang mendapat field RPS
	 * yang bisa disunting inline, toolbar, dan tombol unggah lampiran &mdash; pengguna lain melihat
	 * versi label baca saja.
	 *
	 * <p>Penjagaan ini bersifat tampilan (menyembunyikan/menonaktifkan komponen ZK), sehingga
	 * jalur simpan yang dipanggilnya tidak memeriksa ulang peran.</p>
	 */
	private Tbmuser tbmuser = Common.getCurrentUser();
	/**
	 * Wilayah atas borderlayout tab RPS, berisi panel ringkasan (deskripsi pembelajaran,
	 * capaian/kompetensi, lampiran silabus, dan &mdash; bila {@link #perkuliahan} ada &mdash;
	 * tanggal mulai perkuliahan serta opsi lewati tanggal merah). Panel ini DIBANGUN ULANG dari nol
	 * setiap kali {@link #loadData(Object)} dipanggil, bukan hanya diperbarui isinya.
	 */
	private North north;
	/**
	 * Kelas/jadwal nyata yang menjadi konteks tampilan, boleh {@code null}. Bila {@code null},
	 * layar bersifat murni master data kurikulum: baris "Tgl. Mulai" dan checkbox lewati tanggal
	 * merah tidak dimunculkan, {@link #tanggalMulaiPerkuliahan} tetap {@code null}, dan
	 * {@link #simpan(Perkuliahan)} tidak punya tanggal awal untuk dipakai.
	 */
	private Perkuliahan perkuliahan;
	/**
	 * Datebox tanggal pertemuan pertama, dibuat di {@link #loadData(Object)} HANYA bila
	 * {@link #perkuliahan} tidak {@code null}. Sengaja {@code public} karena diisi dari luar oleh
	 * layar penjadwalan lewat {@link #setHariMulai(String)} agar tanggal default mengikuti hari
	 * jadwal kelas yang dipilih. Bersifat {@code readonly} bagi pengguna (dipilih lewat kalender),
	 * dan nilainya menjadi titik awal perhitungan tanggal mingguan pada
	 * {@link #simpan(Perkuliahan)}.
	 */
	public MyDatebox tanggalMulaiPerkuliahan;
	/**
	 * Checkbox "Lewati tanggal merah / hari libur", dibuat di {@link #loadData(Object)} hanya bila
	 * {@link #perkuliahan} tidak {@code null} dan nilainya diambil dari
	 * {@code perkuliahan.getLewatiTanggalMerahNasional()}. Selama masih {@code null} &mdash; yaitu
	 * pada konteks tanpa perkuliahan &mdash; {@link #simpan(Perkuliahan)} memperlakukannya sebagai
	 * TERCENTANG ({@code true}), bukan sebagai tidak tercentang.
	 */
	private MyCheckboxConfig lewatiTanggalMerahNasional = null;

	/**
	 * Menginisialisasi sub-helper tab lampiran (file/audio/video pertemuan) dan membaca flag
	 * privilese tambah/hapus untuk pengguna saat ini.
	 *
	 * <p>{@link #videoPertemuanHelper} dan {@link #audioPertemuanHelper} dibuat dengan mode
	 * (tambah=true, hapus=false), sedangkan {@link #filePerkuliahanHelper} dibuat tanpa konteks
	 * ({@code null, null}) &mdash; konteks kurikulumnya baru diberikan saat tab yang bersangkutan
	 * diklik pertama kali di {@link #display}. Ketiganya dibuat lebih awal walaupun mungkin tidak
	 * pernah dipakai (mis. pada mode OBE).</p>
	 *
	 * <p>Perhatikan bahwa {@link #tbmuser} TIDAK diisi di sini melainkan pada inisialisasi field,
	 * yang berjalan sebelum badan konstruktor ini.</p>
	 */
	public MatakuliahKurikulumDetailHelper() {
		filePerkuliahanHelper = new FilePerkuliahanHelper(null, null);
		videoPertemuanHelper = new VideoPertemuanHelper(true, false);
		audioPertemuanHelper = new AudioPertemuanHelper(true, false);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Menghitung dan mengisi {@link #tanggalMulaiPerkuliahan} dengan tanggal pertama yang jatuh
	 * pada hari {@code hari} (mis. "Senin") terhitung sejak tanggal mulai
	 * {@link RencanaTahunAkademik} yang sedang berlaku — dipakai agar tanggal mulai perkuliahan
	 * default mengikuti hari jadwal kelas yang dipilih.
	 *
	 * <p><b>Peringatan pemakaian.</b> Pencarian tanggal dilakukan dengan {@code while (true)} yang
	 * hanya berhenti ketika nama hari cocok PERSIS (perbandingan {@code equals}) dengan salah satu
	 * elemen {@code Common.haris}, yaitu {@code {"Minggu", "Senin", "Selasa", "Rabu", "Kamis",
	 * "Jum'at", "Sabtu"}}. Nilai di luar daftar itu &mdash; termasuk variasi ejaan yang lazim
	 * seperti {@code "Jumat"} tanpa apostrof, atau beda huruf besar/kecil &mdash; membuat loop
	 * berputar tanpa henti dan menggantung thread pemanggil. Pemanggil wajib mengoper nilai yang
	 * berasal dari {@code Common.haris} itu sendiri.</p>
	 *
	 * <p>Tidak melakukan apa pun bila {@link #tanggalMulaiPerkuliahan} belum dibuat (konteks tanpa
	 * {@link #perkuliahan}), {@code hari} {@code null}, atau tidak ada {@link RencanaTahunAkademik}
	 * berlaku yang punya tanggal mulai. Pada saat dokumentasi ini ditulis method ini belum
	 * dipanggil dari mana pun di dalam basis kode.</p>
	 *
	 * @param hari nama hari tujuan; HARUS salah satu nilai dari {@code Common.haris}
	 */
	public void setHariMulai(String hari) {
		if (tanggalMulaiPerkuliahan != null && hari != null) {
			RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
					.getCurrentRencanaTahunAkademik(ais.ui.util.WaktuUtil.getDate());
			if (rencanaTahunAkademik != null && rencanaTahunAkademik.getTanggalMulai() != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(rencanaTahunAkademik.getTanggalMulai());
				while (true) {

					int dayOfweek = calendar.get(Calendar.DAY_OF_WEEK);
					if (hari.equals(Common.haris[dayOfweek - 1])) {
						tanggalMulaiPerkuliahan.setValue(calendar.getTime());
						break;
					}

					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
				}
			}
		}
	}

	/**
	 * Varian instance: mengambil tanggal mulai dan flag lewati-tanggal-merah dari state form saat
	 * ini, lalu mendelegasikan ke
	 * {@link #simpan(Perkuliahan, KurikulumPunyaMatakuliah, List, Date, boolean)}.
	 *
	 * <p>Nilai yang dioper diambil dari state layar, bukan dibaca ulang dari basis data:
	 * {@link #kurikulumPunyaMatakuliah} dan {@link #kurikulumPunyaMatakuliahDetails} berasal dari
	 * {@link #loadData(Object)} terakhir, sehingga method ini tidak berbuat apa-apa bila data
	 * belum pernah dimuat (daftar masih {@code null}).</p>
	 *
	 * <p>Bila {@link #lewatiTanggalMerahNasional} masih {@code null} &mdash; keadaan normal ketika
	 * layar dibuka tanpa {@link #perkuliahan} sehingga checkbox-nya tidak pernah dibuat &mdash;
	 * nilai yang dioper adalah {@code true}, yakni seolah-olah pengguna MENCENTANGnya.</p>
	 *
	 * @param perkuliahan kelas nyata tujuan penyalinan RPS
	 */
	public void simpan(Perkuliahan perkuliahan) {
		Date tgl = tanggalMulaiPerkuliahan == null ? null : tanggalMulaiPerkuliahan.getValue();
		MatakuliahKurikulumDetailHelper.simpan(perkuliahan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetails,
				tgl, lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional.isChecked());
	}

	/**
	 * Menyalin template RPS {@code kurikulumPunyaMatakuliahDetails} menjadi baris
	 * {@link Pertemuan} nyata pada {@code perkuliahan}, dengan tanggal berturut-turut mingguan
	 * (+7 hari) mulai dari {@code tgl}. Untuk setiap detail, pertemuan yang sudah ada (dicari lebih
	 * dulu berdasarkan tautan {@code kurikulumPunyaMatakuliahDetail}, lalu berdasarkan tanggal yang
	 * sama pada perkuliahan yang sama) dipakai ulang; bila belum ada, dibuat baru dengan seluruh
	 * atribut disalin dari template (topik, indikator, metode, dsb.) beserta ruang/jam dari
	 * {@code perkuliahan}, lalu lampirannya turut disalin lewat
	 * {@link #copyLampiran(KurikulumPunyaMatakuliahDetail, Pertemuan)}. Silabus/SAP milik
	 * kurikulum juga disalin sekali ke {@code perkuliahan} lewat
	 * {@link #copyLampiran(KurikulumPunyaMatakuliah, Perkuliahan)}. Tidak melakukan apa pun bila
	 * {@code kurikulumPunyaMatakuliahDetails} kosong, {@code tgl} {@code null}, atau
	 * {@code perkuliahan} belum tersimpan.
	 *
	 * <p>
	 * <b>Pertemuan yang dipakai ulang tidak diperbarui.</b> Bila sebuah {@link Pertemuan} sudah
	 * ditemukan (lewat tautan {@code kurikulumPunyaMatakuliahDetail} atau lewat kecocokan
	 * tanggal), baris itu dibiarkan APA ADANYA: atribut RPS tidak disalin ulang, tanggalnya tidak
	 * diperbaiki, dan {@link #copyLampiran(KurikulumPunyaMatakuliahDetail, Pertemuan)} TIDAK
	 * dipanggil. Seluruh penyalinan isi hanya terjadi untuk pertemuan yang benar-benar baru
	 * dibuat. Konsekuensinya method ini aman dipanggil berulang (tidak menggandakan lampiran),
	 * tetapi juga tidak dapat dipakai untuk menyegarkan pertemuan yang sudah terlanjur dibuat
	 * dari template yang kemudian direvisi.
	 * </p>
	 *
	 * <p>
	 * <b>Pencocokan berdasarkan tanggal.</b> Cabang pencarian kedua hanya membandingkan
	 * {@code (perkuliahan, tanggal)} tanpa melihat nomor pertemuan, sehingga pertemuan yang sudah
	 * ada pada tanggal tersebut &mdash; apa pun asal-usulnya &mdash; akan dianggap sebagai
	 * pertemuan untuk baris RPS yang sedang diproses dan dilewati.
	 * </p>
	 *
	 * <p>
	 * <b>Tanggal pertama tidak diperiksa hari libur.</b> Pelompatan tanggal merah dijalankan
	 * SETELAH penambahan tujuh hari di akhir tiap iterasi, jadi hanya berlaku bagi pertemuan
	 * kedua dan seterusnya. Tanggal {@code tgl} yang diberikan dipakai apa adanya untuk pertemuan
	 * pertama walaupun jatuh pada tanggal merah.
	 * </p>
	 *
	 * <p>
	 * <b>Efek samping pada {@code perkuliahan}.</b> Sebelum loop berjalan, {@code perkuliahan}
	 * diperbarui dan disimpan: {@code tanggalMulaiPerkuliahan} diisi {@code tgl} dan
	 * {@code lewatiTanggalMerahNasional} diisi nilai parameter. Silabus/SAP milik kurikulum juga
	 * disalin lebih dulu lewat {@link #copyLampiran(KurikulumPunyaMatakuliah, Perkuliahan)}.
	 * </p>
	 *
	 * <p>
	 * <b>Penjagaan akses.</b> Method statis ini tidak memeriksa peran maupun privilese pengguna;
	 * penjagaannya berada di layar pemanggil (alur penjadwalan {@code PenjadwalanHelper} /
	 * {@code PenjadwalanUtil}).
	 * </p>
	 *
	 * @param perkuliahan                kelas nyata tujuan; diabaikan bila {@code null} atau belum
	 *                                   memiliki id
	 * @param kurikulumPunyaMatakuliah   matakuliah-dalam-kurikulum sumber silabus/SAP
	 * @param kurikulumPunyaMatakuliahDetails template RPS yang disalin; diabaikan bila kosong
	 * @param tgl                        tanggal pertemuan pertama; diabaikan bila {@code null}
	 * @param lewatiTanggalMerahNasional bila {@code true}, tanggal pertemuan KEDUA dan seterusnya
	 *                                   yang jatuh pada hari libur/tanggal merah nasional dilompati
	 *                                   ke minggu berikutnya
	 */
	public static void simpan(Perkuliahan perkuliahan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			List<KurikulumPunyaMatakuliahDetail> kurikulumPunyaMatakuliahDetails, Date tgl,
			boolean lewatiTanggalMerahNasional) {
		if (kurikulumPunyaMatakuliahDetails != null && !kurikulumPunyaMatakuliahDetails.isEmpty()) {

			if (tgl != null && perkuliahan != null && perkuliahan.getId() != null) {

				copyLampiran(kurikulumPunyaMatakuliah, perkuliahan);

				perkuliahan.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional);
				perkuliahan.setTanggalMulaiPerkuliahan(tgl);
				Common.refreshUpdate(perkuliahan);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tgl);
				Session session = HibernateUtil.currentSession();
				for (KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail : kurikulumPunyaMatakuliahDetails) {
					Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail",
									kurikulumPunyaMatakuliahDetail.getId()))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();

					if (pertemuan == null) {
						pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("tanggal", calendar.getTime()))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();
					}

					if (pertemuan == null) {
						pertemuan = new Pertemuan();
						pertemuan.setPerkuliahan(perkuliahan);
						pertemuan.setTopik(kurikulumPunyaMatakuliahDetail.getTopik());
						pertemuan.setIndikator(kurikulumPunyaMatakuliahDetail.getIndikator());
						pertemuan.setWaktupembelajaran(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran());
						pertemuan.setPengalamanBelajar(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar());
						pertemuan.setTugasDanPenilaian(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian());
						pertemuan.setBukuRujukan1(kurikulumPunyaMatakuliahDetail.getBukuRujukan1());
						pertemuan.setStatusPertemuan(kurikulumPunyaMatakuliahDetail.getStatusPertemuan());
						pertemuan.setPertemuanKe(kurikulumPunyaMatakuliahDetail.getNomorUrut());
						pertemuan.setMetodePembelajaran(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran());
						pertemuan.setTanggal(calendar.getTime());
						pertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetail.getId());
						pertemuan.setRuang(perkuliahan.getRuang());
						pertemuan.setWaktuMulai(perkuliahan.getWaktuMulai());
						pertemuan.setWaktuSelesai(perkuliahan.getWaktuSelesai());
						session.save(pertemuan);

						copyLampiran(kurikulumPunyaMatakuliahDetail, pertemuan);
					}

					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
					if (lewatiTanggalMerahNasional) {
						while (Common.isHolidayMerahDanAtauHariLibur(calendar.getTime())) {
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
						}
					}
					System.out.println("calendar = " + Common.dateFormat1.get().format(calendar.getTime()));
				}
			}
		}
	}

	/**
	 * Menyalin lampiran Silabus dan SAP milik {@code kurikulumPunyaMatakuliah} ke
	 * {@code perkuliahan}, HANYA bila {@code perkuliahan} belum memiliki lampiran jenis tersebut
	 * (idempoten — tidak menimpa lampiran yang sudah diunggah manual pada kelas). Setiap salinan
	 * ditandai {@code copyDari} ke {@link LampiranLain} sumber, dalam sesi streaming mandiri.
	 *
	 * <p>Sifat idempoten berasal dari pemeriksaan "sudah ada lampiran jenis ini pada
	 * perkuliahan?", bukan dari perbandingan isi &mdash; bila kelas sudah punya Silabus hasil
	 * unggah manual, versi kurikulum tidak akan menimpanya. Kedua jenis diperiksa terpisah,
	 * sehingga Silabus dapat tersalin sementara SAP tidak, atau sebaliknya.</p>
	 *
	 * <p>Tiap salinan disimpan dalam transaksi tersendiri pada sesi
	 * {@link StreamingHibernateUtil}. Bila terjadi galat, transaksi di-rollback dan galatnya
	 * dicatat, namun sesi streaming TIDAK ditutup pada jalur galat itu (penutupan hanya ada pada
	 * jalur sukses). Kegagalan tidak pernah dilempar ke pemanggil, jadi penyalinan yang gagal
	 * tidak menghentikan alur penjadwalan yang memanggilnya.</p>
	 *
	 * @param kurikulumPunyaMatakuliah sumber lampiran Silabus/SAP tingkat kurikulum
	 * @param perkuliahan              kelas nyata tujuan salinan
	 */
	public static void copyLampiran(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, Perkuliahan perkuliahan) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			LampiranLain lama = LampiranLain.ambil(perkuliahan.getId(), LampiranLain.SILABUS);

			if (lama == null) {
				LampiranLain c = LampiranLain.ambil(kurikulumPunyaMatakuliah.getId(),
						LampiranLain.SILABUS + KurikulumPunyaMatakuliah.class.getName());
				if (c != null) {
					LampiranLain copy = (LampiranLain) c.clone();
					copy.setRef(perkuliahan.getId());
					copy.setJenis(LampiranLain.SILABUS);
					copy.setCopyDari(c);
					session.getTransaction().begin();
					session.save(copy);
					session.getTransaction().commit();
				}
			}

			lama = LampiranLain.ambil(perkuliahan.getId(), LampiranLain.SAP);

			if (lama == null) {
				LampiranLain c = LampiranLain.ambil(kurikulumPunyaMatakuliah.getId(),
						LampiranLain.SAP + KurikulumPunyaMatakuliah.class.getName());
				if (c != null) {
					LampiranLain copy = (LampiranLain) c.clone();
					copy.setRef(perkuliahan.getId());
					copy.setJenis(LampiranLain.SAP);
					copy.setCopyDari(c);
					session.getTransaction().begin();
					session.save(copy);
					session.getTransaction().commit();
				}
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MatakuliahKurikulumDetailHelper.java:236");
		}

	}

	/**
	 * Menyalin SELURUH {@link PertemuanFileContent}, {@link VideoPertemuan}, dan
	 * {@link AudioPertemuan} yang tertaut ke {@code kurikulumPunyaMatakuliahDetail} (template RPS)
	 * menjadi salinan baru yang tertaut ke {@code pertemuan} (kelas nyata) — setiap salinan
	 * ditandai {@code copyDari} ke sumbernya dan referensi kurikulum-nya dikosongkan (murni milik
	 * pertemuan hasil salinan). Selalu menyalin ulang (tidak mengecek duplikasi) setiap kali
	 * dipanggil — dipanggil hanya saat {@link Pertemuan} baru pertama kali dibuat di
	 * {@link #simpan(Perkuliahan, KurikulumPunyaMatakuliah, List, Date, boolean)}.
	 *
	 * <p><b>Tidak idempoten.</b> Berbeda dari
	 * {@link #copyLampiran(KurikulumPunyaMatakuliah, Perkuliahan)} yang memeriksa keberadaan
	 * lampiran lebih dulu, method ini selalu membuat salinan baru untuk setiap berkas sumber
	 * tanpa memeriksa apakah salinan serupa sudah ada pada {@code pertemuan}. Memanggilnya dua
	 * kali untuk pasangan yang sama akan menggandakan seluruh lampiran. Keamanannya dalam praktik
	 * bergantung pada pemanggil tunggalnya, yang hanya memanggil saat {@link Pertemuan} baru
	 * pertama kali dibuat.</p>
	 *
	 * <p>Untuk berkas, yang disalin adalah METADATA beserta {@code lokasiFisik} yang sama &mdash;
	 * artinya kedua baris menunjuk berkas fisik yang sama, bukan menggandakan isinya di
	 * penyimpanan. Untuk video/audio, kolom {@code kurikulumPunyaMatakuliah} dan
	 * {@code kurikulumPunyaMatakuliahDetail} sengaja dikosongkan agar salinan menjadi milik
	 * pertemuan saja, sekaligus mencegah salinan itu ikut terhitung pada ringkasan
	 * {@link #createKeterangan} milik template.</p>
	 *
	 * <p>Sama seperti overload-nya, tiap salinan disimpan dalam transaksi tersendiri pada sesi
	 * {@link StreamingHibernateUtil}, galat di-rollback dan hanya dicatat (tidak dilempar), dan
	 * sesi streaming tidak ditutup pada jalur galat.</p>
	 *
	 * @param kurikulumPunyaMatakuliahDetail baris RPS sumber lampiran
	 * @param pertemuan                      pertemuan nyata tujuan salinan
	 */
	@SuppressWarnings("unchecked")
	public static void copyLampiran(KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail,
			Pertemuan pertemuan) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (PertemuanFileContent c : pertemuanFileContents) {
				PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
				pertemuanFileContent.setFoto(c.getFoto());
				pertemuanFileContent.setNama(c.getNama());
				pertemuanFileContent.setFileMimeType(c.getFileMimeType());
				pertemuanFileContent.setCopyDari(c);
				pertemuanFileContent.setLokasiFisik(c.getLokasiFisik());
				pertemuanFileContent.setKurikulumPunyaMatakuliah(null);
				pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(null);
				pertemuanFileContent.setPertemuan(pertemuan.getId());
				pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.getTransaction().begin();
				session.save(pertemuanFileContent);
				session.getTransaction().commit();
			}

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (VideoPertemuan c : videoPertemuans) {
				VideoPertemuan videoPertemuan = new VideoPertemuan();
				videoPertemuan.setFoto(c.getFoto());
				videoPertemuan.setNama(c.getNama());
				videoPertemuan.setJurusan(c.getJurusan());
				videoPertemuan.setKeterangan(c.getKeterangan());
				videoPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				videoPertemuan.setTahunAkademik(c.getTahunAkademik());
				videoPertemuan.setType(c.getType());
				videoPertemuan.setUkuran(c.getUkuran());

				videoPertemuan.setKurikulumPunyaMatakuliah(null);
				videoPertemuan.setKurikulumPunyaMatakuliahDetail(null);
				videoPertemuan.setPertemuan(pertemuan.getId());
				session.getTransaction().begin();
				session.save(videoPertemuan);
				session.getTransaction().commit();
			}

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.desc("id"))
					.add(Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId()))
					.list();
			for (AudioPertemuan c : audioPertemuans) {
				AudioPertemuan audioPertemuan = new AudioPertemuan();
				audioPertemuan.setFoto(c.getFoto());
				audioPertemuan.setNama(c.getNama());
				audioPertemuan.setJurusan(c.getJurusan());
				audioPertemuan.setKeterangan(c.getKeterangan());
				audioPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
				audioPertemuan.setTahunAkademik(c.getTahunAkademik());
				audioPertemuan.setType(c.getType());
				audioPertemuan.setUkuran(c.getUkuran());

				audioPertemuan.setKurikulumPunyaMatakuliah(null);
				audioPertemuan.setKurikulumPunyaMatakuliahDetail(null);
				audioPertemuan.setPertemuan(pertemuan.getId());
				session.getTransaction().begin();
				session.save(audioPertemuan);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/MatakuliahKurikulumDetailHelper.java:317");
		}

	}

	/**
	 * Menambahkan tombol "Buat Rencana Pembelajaran" ke {@code toolbar} (tampil hanya untuk
	 * pengguna berprivilese CREATE yang bukan mahasiswa/siswa/dosen) yang membuka dialog untuk
	 * membangkitkan N baris {@link KurikulumPunyaMatakuliahDetail} sekaligus — mengisi deskripsi
	 * pembelajaran, capaian/kompetensi, silabus, dan jumlah pertemuan; opsional menandai pertemuan
	 * tengah sebagai UTS dan pertemuan akhir sebagai UAS, serta opsional menghapus seluruh rencana
	 * lama lebih dulu. Baris dengan nomor urut yang sudah ada dilewati (tidak ditimpa).
	 *
	 * @param eventListener dipanggil (lewat timer default) setelah baris berhasil dibuat, untuk
	 *                      menyegarkan tampilan pemanggil
	 */
	public void tampilTombolBuatKurikulumPunyaMatakuliahDetail(Toolbar toolbar, final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Buat Rencana Pembelajaran", "/img/new.gif");
		button.setVisible(add && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null);
		button.addEventListener("onClick", new EventListener() {
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
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(kurikulumPunyaMatakuliah.getMatakuliah().getKode()
						+ " - " + kurikulumPunyaMatakuliah.getMatakuliah().getNama()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(kurikulumPunyaMatakuliah.getKurikulum().getNama()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(kurikulumPunyaMatakuliah.getSemester() + ""));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Pembelajaran *"));
				final MyTextbox deskripsiPembelajaran;
				row.appendChild(
						deskripsiPembelajaran = new MyTextbox(kurikulumPunyaMatakuliah.getDeskripsiPembelajaran()));
				deskripsiPembelajaran.setRows(3);
				deskripsiPembelajaran.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk matakuliah Sistem Multimedia : Tujuan utama dari mata kuliah ini adalah membekali mahasiswa dengan berbagai kemampuan dalam membangun sistem multimedia melalui pemahaman akan konsep dari sub-sistem penyusunnya........");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Capaian / Kompetensi *"));
				final MyTextbox kompetensi;
				row.appendChild(kompetensi = new MyTextbox(kurikulumPunyaMatakuliah.getCapaianPembelajaranProdi()));
				kompetensi.setRows(2);
				kompetensi.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk matakuliah Sistem Multimedia : Mahasiswa memiliki pemahaman mengenai konsep dasar multimedia dan komponen pembentuk sistem multimedia........");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				Hbox hbox1 = new Hbox();
				hbox1.setParent(hbox);
				LampiranLain.createDownloadUploadFileLain(hbox1, kurikulumPunyaMatakuliah.getId(),
						KurikulumPunyaMatakuliah.class.getName(), "Silabus", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, tbmuser != null && tbmuser.getMahasiswa() == null
								&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

				Common.initKeterangan(rows,
						"Berupa file silabus atau rencana pembelajaran kuliah, file ini tidak harus diupload, namun sangat dianjurkan diupload, sehingga semua mahasiswa yang mengikuti perkuliahan dapat melihat silabus atau rencana pembelajaran selama satu semester");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pertemuan"));
				final MyIntbox jumlahKurikulumPunyaMatakuliahDetail;
				row.appendChild(jumlahKurikulumPunyaMatakuliahDetail = new MyIntbox(
						kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault()));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig inti = new MyCheckboxConfig(
						"Inti menurut rujukan peer group / SK Mendiknas 045/2002 (ps. 3 ayat 2e)");
				row.appendChild(inti);
				inti.setChecked(kurikulumPunyaMatakuliah.getInti());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig institusional = new MyCheckboxConfig("Institusional");
				row.appendChild(institusional);
				institusional.setChecked(kurikulumPunyaMatakuliah.getInstitusional());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig terdapatTugas = new MyCheckboxConfig(
						"Bobot Tugas, mata kuliah yang dalam penentuan nilai akhirnya memberikan bobot pada tugas-tugas (praktikum/praktek, PR atau makalah) >= 20%.");
				row.appendChild(terdapatTugas);
				terdapatTugas.setChecked(kurikulumPunyaMatakuliah.getTerdapatTugas());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UTS"));
				final MyCheckboxConfig uts;
				row.appendChild(uts = new MyCheckboxConfig("Di pertengahan pertamuan merupakan jadwal UTS"));
				uts.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("UAS"));
				final MyCheckboxConfig uas;
				row.appendChild(uas = new MyCheckboxConfig("Di akhir pertamuan merupakan jadwal UAS"));
				uas.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Hapus pertamuan"));
				final MyCheckboxConfig hapus;
				row.appendChild(hapus = new MyCheckboxConfig("Hapus pertamuan yang sebelumnya sudah ada"));

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

						if (deskripsiPembelajaran.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show("Mohon maaf, deskripsi pembelajaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom deskripsi pembelajaran pada form yang tersedia; (2) pastikan deskripsi tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						if (kompetensi.getValue().trim().isEmpty()) {
							MyMessageboxConfig.show("Mohon maaf, capaian/kompetensi pembelajaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom capaian atau kompetensi pembelajaran; (2) pastikan kolom tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						if (jumlahKurikulumPunyaMatakuliahDetail.getValue() == null) {
							MyMessageboxConfig.show("Mohon maaf, jumlah pertemuan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom jumlah pertemuan dengan angka yang sesuai; (2) pastikan nilai tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();

						kurikulumPunyaMatakuliah.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
						kurikulumPunyaMatakuliah.setCapaianPembelajaranProdi(kompetensi.getValue());
						kurikulumPunyaMatakuliah
								.setJumlahPertemuanPerkuliahanDefault(jumlahKurikulumPunyaMatakuliahDetail.getValue());
						kurikulumPunyaMatakuliah.setInti(inti.isChecked());
						kurikulumPunyaMatakuliah.setInstitusional(institusional.isChecked());
						kurikulumPunyaMatakuliah.setTerdapatTugas(terdapatTugas.isChecked());
						Common.refreshUpdate(session, kurikulumPunyaMatakuliah);

						if (hapus.isChecked()) {
							session.createSQLQuery(
									"delete from kurikulum_punya_matakuliah_detail where kurikulum_punya_matakuliah="
											+ kurikulumPunyaMatakuliah.getId())
									.executeUpdate();
						}

						for (int i = 1; i <= jumlahKurikulumPunyaMatakuliahDetail.getValue(); i++) {
							KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) session
									.createCriteria(KurikulumPunyaMatakuliahDetail.class)
									.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
									.add(Restrictions.eq("nomorUrut", i)).setMaxResults(1).uniqueResult();
							if (kurikulumPunyaMatakuliahDetail == null) {
								kurikulumPunyaMatakuliahDetail = new KurikulumPunyaMatakuliahDetail();
								kurikulumPunyaMatakuliahDetail.setNomorUrut(i);
								kurikulumPunyaMatakuliahDetail.setStatusPertemuan(ConstantValues.TATAP_MUKA);

								if (uas.isChecked()) {
									if (i == jumlahKurikulumPunyaMatakuliahDetail.getValue()) {
										kurikulumPunyaMatakuliahDetail.setStatusPertemuan(ConstantValues.UAS);
										kurikulumPunyaMatakuliahDetail.setTopik("Pertemuan ke " + i + " : UAS");
										kurikulumPunyaMatakuliahDetail.setMetodePembelajaran("Mengerjakan soal UAS");
									}
								}

								if (uts.isChecked()) {
									if (i == (jumlahKurikulumPunyaMatakuliahDetail.getValue() / 2)) {
										kurikulumPunyaMatakuliahDetail.setStatusPertemuan(ConstantValues.UTS);
										kurikulumPunyaMatakuliahDetail.setTopik("Pertemuan ke " + i + " : UTS");
										kurikulumPunyaMatakuliahDetail.setMetodePembelajaran("Mengerjakan soal UTS");
									}
								}

								kurikulumPunyaMatakuliahDetail.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
								Common.refreshSaveOrUpdate(session, kurikulumPunyaMatakuliahDetail);
							}

						}
						window.detach();

						Common.createDefaultTimer(eventListener);
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);
	}

	/** Perender baris grid RPS: ringkasan jumlah lampiran (via {@link #createKeterangan}), field topik/indikator/waktu/pengalaman/tugas/rujukan/metode/status (inline-editable, autosave, hanya untuk pengguna non-mahasiswa/siswa/dosen; read-only berupa label untuk pengguna lain), dan tombol hapus. */
	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		/**
		 * Merender satu baris RPS. Kolom pertama berupa {@link ais.ui.util.MyDetail} yang selalu
		 * terbuka ({@code setOpen(true)}) dan berisi ringkasan jumlah lampiran File/Audio/Video hasil
		 * {@link MatakuliahKurikulumDetailHelper#createKeterangan}. Kolom berikutnya bergantung peran
		 * pengguna:
		 *
		 * <ul>
		 * <li>Pengguna yang BUKAN mahasiswa, BUKAN siswa, dan BUKAN dosen mendapat sembilan kontrol
		 * yang dapat disunting langsung: topik, indikator, waktu pembelajaran, pengalaman belajar,
		 * tugas &amp; penilaian, buku rujukan 1, buku rujukan 2, metode pembelajaran, dan combobox
		 * {@link StatusPertemuan}. Masing-masing punya listener {@code onChange} sendiri yang
		 * menyimpan HANYA field itu lewat {@link Common#refreshUpdate} &mdash; jadi simpan per-field,
		 * bukan simpan gabungan.</li>
		 * <li>Pengguna lain mendapat delapan {@link org.zkoss.zul.Label} baca saja.</li>
		 * </ul>
		 *
		 * <p><b>Asimetri jumlah kolom.</b> Cabang yang dapat disunting menghasilkan SEMBILAN komponen
		 * (buku rujukan 2 termasuk), sedangkan cabang baca saja hanya DELAPAN &mdash; buku rujukan 2
		 * tidak pernah ditampilkan kepada mahasiswa/siswa/dosen. Karena definisi kolom grid di
		 * {@link MatakuliahKurikulumDetailHelper#display} bersifat tetap, isi kolom pada kedua cabang
		 * tidak sejajar satu sama lain.</p>
		 *
		 * <p>Tombol hapus baris hanya tampil bila {@link MatakuliahKurikulumDetailHelper#delete}
		 * bernilai {@code true} DAN pengguna bukan mahasiswa/siswa/dosen, dan meminta konfirmasi lebih
		 * dulu. Kegagalan hapus karena relasi ditampilkan lewat
		 * {@link ais.common.PesanFormalHelper#tampilkanGagalException}.</p>
		 *
		 * @param row  baris grid ZK tujuan render
		 * @param data instance {@link KurikulumPunyaMatakuliahDetail} untuk baris ini
		 */
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail = (KurikulumPunyaMatakuliahDetail) data;

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Hbox hb = MatakuliahKurikulumDetailHelper.createKeterangan(kurikulumPunyaMatakuliahDetail,
					new DataLoader() {

						@Override
						public void loadData(Object value) {
							MatakuliahKurikulumDetailHelper.this.loadData(value);
						}
					});
			detail.appendChild(hb);

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null) {

				final Textbox topik = new Textbox(kurikulumPunyaMatakuliahDetail.getTopik());
				topik.setWidth("90%");
				topik.setRows(2);
				topik.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setTopik(topik.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				topik.setParent(row);

				final Textbox indikator = new Textbox(kurikulumPunyaMatakuliahDetail.getIndikator());
				indikator.setWidth("90%");
				indikator.setRows(2);
				indikator.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setIndikator(indikator.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				indikator.setParent(row);

				final Textbox waktupembelajaran = new Textbox(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran());
				waktupembelajaran.setWidth("90%");
				waktupembelajaran.setRows(2);
				waktupembelajaran.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setWaktupembelajaran(waktupembelajaran.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				waktupembelajaran.setParent(row);

				final Textbox pengalamanBelajar = new Textbox(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar());
				pengalamanBelajar.setWidth("90%");
				pengalamanBelajar.setRows(2);
				pengalamanBelajar.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setPengalamanBelajar(pengalamanBelajar.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				pengalamanBelajar.setParent(row);

				final Textbox tugasDanPenilaian = new Textbox(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian());
				tugasDanPenilaian.setWidth("90%");
				tugasDanPenilaian.setRows(2);
				tugasDanPenilaian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setTugasDanPenilaian(tugasDanPenilaian.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				tugasDanPenilaian.setParent(row);

				final Textbox bukuRujukan1 = new Textbox(kurikulumPunyaMatakuliahDetail.getBukuRujukan1());
				bukuRujukan1.setWidth("90%");
				bukuRujukan1.setRows(2);
				bukuRujukan1.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setBukuRujukan1(bukuRujukan1.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				bukuRujukan1.setParent(row);

				final Textbox bukuRujukan2 = new Textbox(kurikulumPunyaMatakuliahDetail.getBukuRujukan2());
				bukuRujukan2.setWidth("90%");
				bukuRujukan2.setRows(2);
				bukuRujukan2.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setBukuRujukan2(bukuRujukan2.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				bukuRujukan2.setParent(row);

				final Textbox metodePembelajaran = new Textbox(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran());
				metodePembelajaran.setWidth("90%");
				metodePembelajaran.setRows(2);
				metodePembelajaran.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						kurikulumPunyaMatakuliahDetail.setMetodePembelajaran(metodePembelajaran.getValue());
						Common.refreshUpdate(kurikulumPunyaMatakuliahDetail);
					}
				});
				metodePembelajaran.setParent(row);

				final Combobox combobox = new Combobox();
				Common.insertCombo(combobox, "nama", StatusPertemuan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(combobox, kurikulumPunyaMatakuliahDetail.getStatusPertemuan());
				combobox.setWidth("90%");
				combobox.setParent(row);
				combobox.setReadonly(true);

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kurikulumPunyaMatakuliahDetail
								.setStatusPertemuan((StatusPertemuan) combobox.getSelectedItem().getValue());
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (kurikulumPunyaMatakuliahDetail));
					}
				});
			} else {
				new Label(kurikulumPunyaMatakuliahDetail.getTopik()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getIndikator()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getWaktupembelajaran()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getPengalamanBelajar()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getTugasDanPenilaian()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getBukuRujukan1()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getMetodePembelajaran()).setParent(row);
				new Label(kurikulumPunyaMatakuliahDetail.getStatusPertemuan() == null ? ""
						: kurikulumPunyaMatakuliahDetail.getStatusPertemuan().getNama()).setParent(row);
			}

			Hbox toolbar = new Hbox();
			toolbar.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(kurikulumPunyaMatakuliahDetail);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Memuat ulang grid RPS dengan seluruh {@link KurikulumPunyaMatakuliahDetail} milik
	 * {@link #kurikulumPunyaMatakuliah}, dan membangun ulang panel ringkasan di {@link #north}
	 * (deskripsi pembelajaran, kompetensi, lampiran silabus) — bila {@link #perkuliahan} diberikan,
	 * panel ini juga menampilkan/mengedit tanggal mulai perkuliahan dan opsi lewati tanggal merah.
	 * Kontrak {@link DataLoader#loadData(Object)}; {@code value} tidak dipakai.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();

		kurikulumPunyaMatakuliahDetails = session.createCriteria(KurikulumPunyaMatakuliahDetail.class)
				.addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).list();
		ListModel strset = new SimpleListModel(kurikulumPunyaMatakuliahDetails);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

		Common.clear(north);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(north);

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi:"));
		row.appendChild(new MyLabelAgakKecil(kurikulumPunyaMatakuliah.getDeskripsiPembelajaran()));

		row.appendChild(new ais.ui.util.MyLabelConfig("Kompetensi:"));
		row.appendChild(new MyLabelAgakKecil(kurikulumPunyaMatakuliah.getCapaianPembelajaranProdi()));

		row = new MyFormRow();
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran:"));

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		Hbox hbox1 = new Hbox();
		hbox1.setParent(hbox);
		LampiranLain.createDownloadUploadFileLain(hbox1, kurikulumPunyaMatakuliah.getId(),
				KurikulumPunyaMatakuliah.class.getName(), "Silabus", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, tbmuser != null && tbmuser.getMahasiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);

		if (perkuliahan != null) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. Mulai :"));
			hbox = new Hbox();
			row.appendChild(hbox);
			hbox.appendChild(tanggalMulaiPerkuliahan = new MyDatebox(perkuliahan.getTanggalMulaiPerkuliahan()));
			tanggalMulaiPerkuliahan.setReadonly(true);
			lewatiTanggalMerahNasional = new MyCheckboxConfig("Lewati tanggal merah / hari libur");
			lewatiTanggalMerahNasional.setChecked(perkuliahan.getLewatiTanggalMerahNasional());
			hbox.appendChild(lewatiTanggalMerahNasional);
		} else {
			ais.ui.util.ZkCompat.setSpans(row, "1,3");
		}
	}

	/**
	 * Membangun seluruh tabbox RPS+lampiran ke dalam {@code component}. Lihat javadoc kelas untuk
	 * uraian lengkap perilaku (termasuk pencabangan ke iframe RPS-OBE bila kurikulum memakai skema
	 * OBE) dan tab-tab lazy-load.
	 *
	 * @param kurikulumPunyaMatakuliah matakuliah-dalam-kurikulum yang RPS-nya ditampilkan/dikelola
	 * @param perkuliahan              kelas nyata terkait (untuk fitur "Tgl. Mulai" & penyalinan
	 *                                 RPS ke pertemuan), boleh {@code null} bila konteksnya murni
	 *                                 master data kurikulum tanpa kelas spesifik
	 * @param component                kontainer ZK tujuan; isi sebelumnya dibersihkan lewat
	 *                                 {@link Common#clear}
	 */
	public void display(final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, final Perkuliahan perkuliahan,
			final Component component) {
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
		this.perkuliahan = perkuliahan;
		Common.clear(component);

		final Tabbox tabbox = new Tabbox();

		if (component instanceof Center) {
			tabbox.setParent(component);
		} else {
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 600px;");
			groupbox.setParent(component);
			tabbox.setParent(groupbox);
		}

		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tab = new Tab("Rencana Pembelajaran");
		tab.setParent(tabs);

		final Tab tabFile = new Tab("File");
		tabFile.setParent(tabs);

		final Tab tabReferensi = new Tab("Buku Referensi");
		tabReferensi.setParent(tabs);

		final Tab tabBukuAjar = new Tab("Buku Ajar");
		Session session = HibernateUtil.currentSession();
		int jumlahBukuAjar = ((Number) session.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("matakuliah", kurikulumPunyaMatakuliah.getMatakuliah())).uniqueResult())
				.intValue();
		tabBukuAjar.setLabel("Buku Diktat / Ajar " + (jumlahBukuAjar == 0 ? "" : "(" + jumlahBukuAjar + ")"));
		tabBukuAjar.setParent(tabs);

		final Tab tabArtikel = new Tab("Artikel");
		tabArtikel.setParent(tabs);

		final Tab tabAudio = new Tab("Audio");
		tabAudio.setParent(tabs);

		final Tab tabVideo = new Tab("Video");
		tabVideo.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		if (kurikulumPunyaMatakuliah.getKurikulum().apakahObe(perkuliahan == null ? null : perkuliahan.getTahunAjaran(),
				perkuliahan == null ? null : perkuliahan.getGanjilGenap())) {
			tabpanel.setHeight("1200px");

			org.zkoss.zul.Div wadahObe = new org.zkoss.zul.Div();
			wadahObe.setStyle("width:100%;height:100%;min-height:1100px;overflow:auto;");
			wadahObe.setParent(tabpanel);

			MyInclude iframe = new MyInclude("/pages/master/rps_obe.zul?kur=" + kurikulumPunyaMatakuliah.getId()
					+ (perkuliahan != null && perkuliahan.getId() != null ? "&perkuliahan=" + perkuliahan.getId()
							: ""));
			iframe.setWidth("100%");
			iframe.setParent(wadahObe);

		} else {

			MyPanel panel = new MyPanel();
			panel.setParent(tabpanel);
			panel.setWidth("100%");
			if (component instanceof Center) {
				panel.setHeight("100%");
			} else {
				panel.setHeight("1200px");
			}

			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Toolbar toolbar = new Toolbar();
			toolbar.setVisible(tbmuser != null && tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
			// toolbar.setHeight("25px");
			toolbar.setParent(panel);
			tampilTombolBuatKurikulumPunyaMatakuliahDetail(toolbar, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			String[] contents = new String[] { "id", "kurikulumPunyaMatakuliah", "nomorUrut", "indikator", "topik",
					"metodePembelajaran", "pengalamanBelajar", "waktupembelajaran", "tugasDanPenilaian", "bukuRujukan1",
					"statusPertemuan" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

				@Override
				public Criteria initCriteria(boolean order) {
					Session session = HibernateUtil.currentSession();

					return session.createCriteria(KurikulumPunyaMatakuliahDetail.class).addOrder(Order.asc("nomorUrut"))
							.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah));
				}
			}, contents);
			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

				@Override
				public void onSearchDefault(Event event) {
					loadData(null);
				}
			}, KurikulumPunyaMatakuliahDetail.class, contents);
			toolbar.appendChild(upload);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus Semua Rencana Pembelajaran",
					"/img/svg/trash.svg");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											KurikulumPunyaMatakuliahDetailDao kurikulumPunyaMatakuliahDetailDao = DaoFactory
													.getInstance().getKurikulumPunyaMatakuliahDetailDao();

											for (KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail : kurikulumPunyaMatakuliahDetails) {
												kurikulumPunyaMatakuliahDetailDao
														.delete(kurikulumPunyaMatakuliahDetail);
											}

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);

			north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("0%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kemampuan akhir pembelajaran");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kriteria, Indikator & Bobot penilaian");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu pembelajaran");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Pengalaman Belajar");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tugas Dan Penilaian");
			column.setWidth("15%");

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
			column.setLabel("Jenis");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("5%");

			loadData(null);
		}

		final Tabpanel filePerkuliahan = new ais.ui.util.MyTabpanel();
		filePerkuliahan.setParent(tabpanels);
		filePerkuliahan.setHeight("1250px");
		tabFile.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (filePerkuliahan.getChildren().size() == 0) {
					filePerkuliahanHelper.createFile(null, null, kurikulumPunyaMatakuliah, null, filePerkuliahan, null);
				}
			}
		});

		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		int jumlahReferensi = ((Number) session.createCriteria(KurikulumPunyaMatakuliahPunyaItem.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).uniqueResult()).intValue();
		tabReferensi.setLabel("Buku Referensi " + (jumlahReferensi == 0 ? "" : "(" + jumlahReferensi + ")"));

		tabpanelReferensi.setParent(tabpanels);
		tabReferensi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelReferensi.getChildren().size() == 0) {
					tabpanelReferensi.setHeight("1250px");
					KurikulumPunyaMatakuliahPunyaItemHelper kurikulumPunyaMatakuliahPunyaItemHelper = new KurikulumPunyaMatakuliahPunyaItemHelper();
					kurikulumPunyaMatakuliahPunyaItemHelper.display(kurikulumPunyaMatakuliah, tabpanelReferensi);
				}
			}
		});

		final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
		tabpanelBukuAjar.setParent(tabpanels);
		tabpanelBukuAjar.setHeight("450px");
		tabBukuAjar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelBukuAjar.getChildren().size() == 0) {

					BukuBahanAjarHelper bukuBahanAjarHelper = new BukuBahanAjarHelper();
					bukuBahanAjarHelper.display(kurikulumPunyaMatakuliah.getMatakuliah(), tabpanelBukuAjar, null);
				}
			}
		});

		final Tabpanel tabpanelArtikel = new ais.ui.util.MyTabpanel();
		int jumlahArtikel = ((Number) session.createCriteria(DataPunyaArtikel.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah)).uniqueResult()).intValue();
		tabArtikel.setLabel("Artikel " + (jumlahArtikel == 0 ? "" : "(" + jumlahArtikel + ")"));

		tabpanelArtikel.setParent(tabpanels);
		tabpanelArtikel.setHeight("1250px");
		tabArtikel.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelArtikel.getChildren().size() == 0) {
					DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
					dataPunyaArtikelHelper.display(null, null, null, null, null, null, kurikulumPunyaMatakuliah,
							tabpanelArtikel);
				}
			}
		});

		final Tabpanel audioPerkuliahan = new ais.ui.util.MyTabpanel();
		audioPerkuliahan.setParent(tabpanels);
		audioPerkuliahan.setHeight("1250px");
		tabAudio.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (audioPerkuliahan.getChildren().size() == 0) {
					audioPertemuanHelper.display(null, kurikulumPunyaMatakuliah, null, audioPerkuliahan, null);
				}
			}
		});

		final Tabpanel videoPerkuliahan = new ais.ui.util.MyTabpanel();
		videoPerkuliahan.setParent(tabpanels);
		videoPerkuliahan.setHeight("1250px");
		tabVideo.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (videoPerkuliahan.getChildren().size() == 0) {
					videoPertemuanHelper.display(null, kurikulumPunyaMatakuliah, null, videoPerkuliahan, null);
				}
			}
		});

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();
			int videoPertemuans = ((Number) session.createCriteria(VideoPertemuan.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId())).uniqueResult())
					.intValue();

			tabVideo.setLabel("Video" + (videoPertemuans == 0 ? "" : " (" + videoPertemuans + " video)"));

			int audioPertemuans = ((Number) session.createCriteria(AudioPertemuan.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId())).uniqueResult())
					.intValue();

			tabAudio.setLabel("Audio" + (audioPertemuans == 0 ? "" : " (" + audioPertemuans + " audio)"));

			int file = ((Number) session.createCriteria(PertemuanFileContent.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId())).uniqueResult())
					.intValue();

			tabFile.setLabel("File" + (file == 0 ? "" : " (" + file + " file)"));

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun ringkasan jumlah lampiran (File/Audio/Video) tertaut pada satu
	 * {@code kurikulumPunyaMatakuliahDetail} sebagai tiga tautan {@link A} sebaris — angka dicetak
	 * merah bila jumlahnya lebih dari nol. Bila {@code dataLoader} diberikan, setiap tautan dapat
	 * diklik untuk membuka {@link KurikulumPunyaMatakuliahHelper} pada tab terkait (0=File,
	 * 1=Audio, 2=Video). Query hitungan memakai SQL native pada sesi streaming mandiri agar ringan
	 * dipanggil berulang saat merender banyak baris grid.
	 */
	@SuppressWarnings("unchecked")
	public static Hbox createKeterangan(final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail,
			final DataLoader dataLoader) {

		Hbox hbox = new Hbox();

		try {

			Session streamSession = StreamingHibernateUtil.getInstance().currentSession();

			hbox = new Hbox();

			String sql = "select (select count(id) from audio_pertemuan where kurikulumpunyamatakuliahdetail = "
					+ kurikulumPunyaMatakuliahDetail.getId()
					+ ") as audio, (select count(id) from video_pertemuan where kurikulumpunyamatakuliahdetail = "
					+ kurikulumPunyaMatakuliahDetail.getId()
					+ ") as video, (select count(id) from pertemuan_file_content where kurikulumpunyamatakuliahdetail = "
					+ kurikulumPunyaMatakuliahDetail.getId() + ") as file";

			List<Object[]> objects = streamSession.createSQLQuery(sql).list();

			if (objects != null && objects.size() != 0) {
				Object[] numbers = objects.get(0);
				Number audio = (Number) numbers[0];
				Number video = (Number) numbers[1];
				Number file = (Number) numbers[2];

				A a = new A("File : " + file + ", ");
				a.setStyle("font-size:12px" + (file.intValue() > 0 ? ";color:red;" : ""));
				a.setHref("");
				a.setParent(hbox);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new KurikulumPunyaMatakuliahHelper().display(kurikulumPunyaMatakuliahDetail, dataLoader, 0);

						}
					});
				}

				a = new A("Audio : " + audio + ", ");
				a.setStyle("font-size:12px" + (audio.intValue() > 0 ? ";color:red;" : ""));
				a.setHref("");
				a.setParent(hbox);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new KurikulumPunyaMatakuliahHelper().display(kurikulumPunyaMatakuliahDetail, dataLoader, 1);

						}
					});
				}
				a = new A("Video : " + video + ".");
				a.setStyle("font-size:12px" + (video.intValue() > 0 ? ";color:red;" : ""));
				a.setHref("");
				a.setParent(hbox);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new KurikulumPunyaMatakuliahHelper().display(kurikulumPunyaMatakuliahDetail, dataLoader, 2);

						}
					});
				}

			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			StreamingHibernateUtil.getInstance().rollbackTransaction();
		}

		return hbox;
	}

}
