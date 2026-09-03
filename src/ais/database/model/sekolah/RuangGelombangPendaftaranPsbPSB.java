package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity relasi <b>alokasi ruang ujian PSB</b>: satu baris menyatakan bahwa seorang calon siswa
 * ({@link CalonSiswa}) ditempatkan pada satu ruang ujian penerimaan siswa baru
 * ({@link RuangPSB}). Dipetakan ke tabel {@code sekolah.ruang_gelombang_psb} dan diaudit penuh
 * Hibernate Envers ({@link Audited}), sehingga setiap perpindahan ruang tersimpan permanen di
 * tabel revisi dan dapat ditelusuri lewat tombol "Revisi" pada grid pengelola.
 *
 * <h2>Nama kelas menyesatkan — TIDAK ada relasi ke gelombang</h2>
 * <p>Meski bernama {@code RuangGelombangPendaftaranPsbPSB}, entity ini <b>tidak punya field
 * {@code gelombangPendaftaranPsb} sama sekali</b>. Yang tersimpan hanyalah pasangan
 * (ruang, calon siswa). Gelombang pendaftaran hanya bisa dijangkau secara transitif — lewat
 * {@code getRuangPSB().getGelombangPendaftaranPsb()} atau
 * {@code getCalonSiswa().getGelombangPendaftaranPsb()} — dan kedua jalur itu <b>bisa berbeda</b>
 * karena tidak ada satu pun invarian di kode yang memaksa keduanya cocok saat penulisan manual.
 * Bacalah nama kelas ini sebagai "penghuni ruang PSB", bukan "ruang per gelombang".</p>
 *
 * <h2>Peran dalam alur PSB (terverifikasi dari kode pemanggil)</h2>
 * <p>Baris di tabel ini adalah <b>bukti bahwa seorang calon siswa memakai satu kursi</b> pada
 * ruang tertentu — dipakai untuk menghitung isi ruang, mencetak absensi/berita acara/kartu
 * ujian, dan menurunkan jadwal ujian yang boleh diikuti calon siswa. Rantai penggunaannya:</p>
 * <ol>
 *   <li><b>Penulisan otomatis (jalur normal).</b> Saat nomor ujian calon siswa dibangkitkan,
 *       {@code ais.common.CommonPSB.dapatkanRuangUjian(CalonSiswa)} memilih {@link RuangPSB}
 *       ber-id terkecil yang masih {@code penuh = 0} pada gelombang calon siswa, lalu menulis
 *       (atau <b>menimpa</b>, lihat di bawah) baris di tabel ini. Jalur kembarnya ada di
 *       {@code ais.action.master.sekolah.psb.noujian.DefaultNoUjianGeneratorPsb} dan
 *       {@code ais.action.master.sekolah.CalonSiswaAction}.</li>
 *   <li><b>Penulisan lewat REST/mobile.</b>
 *       {@code ais.action.servlet.api.ElearningApiUtil} memanggil jalur yang sama ketika
 *       konfigurasi {@code setelah_daftar_psb_langsung_generate_nomor_ujian} aktif; bila alokasi
 *       gagal (semua ruang penuh), pendaftaran ditolak dengan pesan "Kuota / ruangan penerimaan
 *       calon siswa telah penuh".</li>
 *   <li><b>Penulisan manual oleh panitia.</b> Panel detail
 *       {@code ais.action.master.psb.RuangPsbCalonSiswaDetailAction} (tersemat di setiap baris
 *       grid {@code RuangPSBAction}) menyediakan tombol "Ambil Data Calon Siswa Manual" yang
 *       menyisipkan/memindahkan calon siswa terpilih ke ruang tersebut secara massal.</li>
 *   <li><b>Pembacaan.</b> Jumlah baris per ruang menjadi angka "isi" pada grid
 *       ({@code RuangPSBAction.cekRuanganIsi}) dan pemicu penandaan {@code penuh}; isinya juga
 *       diambil {@code CommonReportPsb}, album foto PSB, laporan JasperReports
 *       ({@code Coverspsbi}, {@code BeritaAcaraUjianPSB}, {@code AbsensiPSB_day1},
 *       {@code ValidasiPSB}, {@code KartuUjianSpsbMandiri}, {@code Keterangan_Lulus},
 *       {@code KartuBayarPsbMandiri}), layar
 *       {@code ais.action.master.sekolah.psb.TampilanUjianCalonSiswa}, serta beberapa halaman
 *       portal PPDB ({@code WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp},
 *       {@code _cetak_kartu_ujian.jsp}, {@code _cetak_kartu_pendaftaran.jsp},
 *       {@code _sukses_login.jsp}).</li>
 * </ol>
 *
 * <h2>Kunci unik {@code kode_unik}: maksimal SATU alokasi per calon siswa, selamanya</h2>
 * <p>Non-obvious dan berdampak besar: {@link #getKodeUnik()} <b>tidak pernah memakai ruang</b>.
 * Nilainya selalu dihitung ulang menjadi {@code "<idCalonSiswa>_"} dan kolomnya dideklarasikan
 * {@code unique = true}. Efeknya tabel ini bukan relasi many-to-many biasa melainkan
 * <b>pemetaan 1:1 dari calon siswa ke ruang</b>, ditegakkan di tingkat basis data untuk
 * <i>seluruh instalasi</i> — lintas ruang, lintas gelombang, lintas tahun ajaran, dan lintas
 * sekolah/yayasan.</p>
 * <p>Konsekuensi yang perlu diketahui pemanggil:</p>
 * <ul>
 *   <li>Seluruh kode pembaca konsisten dengan itu: setiap pencarian memakai
 *       {@code Restrictions.eq("calonSiswa", ...)} + {@code setMaxResults(1)}, tidak pernah
 *       mengharapkan lebih dari satu baris.</li>
 *   <li>Menempatkan calon siswa ke ruang baru <b>bukan penambahan melainkan pemindahan</b>:
 *       semua penulis (mesin alokasi, panel detail, tombol "Perbaiki Urutan Nomor Ujian")
 *       mencari baris lama lalu memanggil {@link #setRuangPSB(RuangPSB)} pada baris yang sama.
 *       Calon siswa otomatis <b>lenyap dari ruang sebelumnya</b> tanpa notifikasi.</li>
 *   <li>Calon siswa yang mendaftar ulang di gelombang berikutnya (atau peserta ujian ulang)
 *       tidak bisa punya riwayat dua alokasi; jejaknya hanya tersisa di tabel revisi Envers.</li>
 * </ul>
 * <p>Bandingkan dengan kembarannya di modul perguruan tinggi,
 * {@code ais.database.model.RuangPaketPMB} (tabel {@code public.ruang_paket_pmb}): versi PMB
 * masih memeriksa kecocokan paket antara ruang dan calon mahasiswa dan mengosongkan
 * {@code kodeUnik} bila tidak cocok. Versi sekolah ini <b>membuang pemeriksaan tersebut</b> dan
 * selalu menulis {@code "<id>_"} tanpa syarat, sehingga tidak ada rem apa pun bila sebuah
 * alokasi diarahkan ke ruang milik gelombang/sekolah yang salah.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #setId(Long)},
 *       {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi (inti entity):</b> {@link #getRuangPSB()}, {@link #setRuangPSB(RuangPSB)},
 *       {@link #getCalonSiswa()}, {@link #setCalonSiswa(CalonSiswa)} — keduanya {@code LAZY}
 *       dan diresolusi lewat {@code check()} milik {@link GeneralValueObject}.</li>
 *   <li><b>Kunci alternatif turunan:</b> {@link #getKodeUnik()},
 *       {@link #setKodeUnik(String)}.</li>
 *   <li><b>Utilitas:</b> {@link #toString()}, konstruktor
 *       {@link #RuangGelombangPendaftaranPsbPSB()}.</li>
 * </ul>
 * <p>Tidak ada method query statis, tidak ada method bisnis (perhitungan kuota seluruhnya
 * berada di Action/helper pemanggil), tidak ada {@code compareTo}/{@code equals}/{@code hashCode}
 * (identitas memakai bawaan {@link Object}), dan tidak ada koleksi anak.</p>
 *
 * <h2>Pola berulang yang diverifikasi pada berkas ini</h2>
 * <ul>
 *   <li><b>Getter write-back/destruktif — ADA, tiga buah.</b> {@link #getRuangPSB()} dan
 *       {@link #getCalonSiswa()} menimpa field-nya dengan hasil {@code check()};
 *       {@link #getKodeUnik()} menimpa {@link #kodeUnik} dengan nilai turunan. Karena Hibernate
 *       memakai akses <b>properti</b> pada entity ini (lihat catatan pemetaan di bawah), nilai
 *       hasil timpaan itulah yang ikut ter-flush ke basis data. Untuk {@link #getKodeUnik()}
 *       nilainya deterministik, jadi timpaan hanya menghasilkan satu {@code UPDATE} tambahan
 *       (dan satu revisi Envers) pada baris lama yang kolom {@code kode_unik}-nya belum
 *       terformat.</li>
 *   <li><b>Penciutan {@code TreeSet} — TIDAK ADA.</b> Seluruh pemanggil memuat entity ini ke
 *       {@code List} atau mengambilnya sebagai {@code uniqueResult()}, tidak pernah ke
 *       {@code Set} berurut; risiko baris "hilang" akibat pembanding tidak unik tidak berlaku.</li>
 *   <li><b>{@code getKeterangan()} membalik kontrak / {@code compareTo()} dipangkas — TIDAK
 *       ADA</b> (kedua method tersebut tidak ada di kelas ini).</li>
 *   <li><b>Cakupan tenant sekolah/yayasan — TIDAK ADA FILTER SAMA SEKALI.</b> Sama seperti
 *       {@link RuangPSB}, entity ini tidak punya kolom {@code sekolah}/{@code yayasan}, dan
 *       tidak satu pun pemanggilnya memasang syarat tenant. Batas tenant hanya bisa ditarik
 *       transitif lewat {@code ruangPSB.gelombangPendaftaranPsb.sekolah} — dan tidak pernah
 *       ditarik.</li>
 * </ul>
 *
 * <h2>CATATAN KEAMANAN — panel pengelola tanpa gerbang privilese</h2>
 * <p>Layar yang memutasi entity ini adalah {@code RuangPsbCalonSiswaDetailAction}
 * (paket {@code ais.action.master.psb}), yang <b>tidak memanggil
 * {@code CommonPrivilages.checkPrevilages(...)} satu kali pun</b> (kelas induknya,
 * {@code ais.ui.util.MyDetail}, juga tidak). Panel ini di-<i>instansiasi tanpa syarat</i> pada
 * setiap baris grid oleh {@code RuangPSBAction.RuangPSBRenderer.render()}, sedangkan
 * {@code RuangPSBAction} sendiri hanya menuntut hak {@code READ} untuk membuka layar dan
 * mengatur {@code setVisible(...)} berbasis {@code CREATE}/{@code UPDATE}/{@code DELETE} hanya
 * untuk tombol-tombol miliknya sendiri. Artinya pemegang hak <b>BACA saja</b> tetap mendapatkan
 * di dalam panel detail:</p>
 * <ul>
 *   <li>tombol <b>"Ambil Data Calon Siswa Manual"</b> — penulisan massal ke tabel ini; dan
 *       karena {@code kode_unik} memaksa satu alokasi per calon siswa, aksi ini
 *       <b>memindahkan</b> calon siswa keluar dari ruang/gelombang lamanya, bukan menambah;</li>
 *   <li>dialog pemilihannya, {@code ais.action.master.helper.generic.AmbilDataCalonSiswaBanyak},
 *       yang juga <b>nol {@code checkPrevilages}</b> dan <b>nol filter tenant</b> — kriterianya
 *       hanya {@code gelombangPendaftaranPsb is not null} ditambah filter opsional tahun
 *       ajaran/nama/no. registrasi/no. ujian, sehingga daftar pilihannya memuat calon siswa
 *       <b>seluruh sekolah dan yayasan</b> dalam satu instalasi. Ini mekanisme sekerabat dengan
 *       {@code AmbilDataSiswaBanyak} yang sudah tercatat pada audit "surat sakti";</li>
 *   <li>tombol <b>"Hapus"</b> per baris (lihat catatan bug di bawah — saat ini mati total); dan</li>
 *   <li>tombol <b>"Cetak"</b> ({@code Common.cetakData(...)}) yang mengekspor calon siswa ruang
 *       tersebut memakai daftar kolom {@code CalonSiswaAction.contents} — <b>lebih dari 120
 *       kolom</b>, termasuk {@code nik}, {@code kk}, {@code nikAyah}/{@code nikIbu}/
 *       {@code nikWali}, {@code penghasilanAyah}/{@code Ibu}/{@code Wali}, sembilan nomor HP
 *       dan tiga nomor WhatsApp orang tua/wali, {@code riwayatPenyakit}, {@code golonganDarah},
 *       {@code kebutuhanKhusus}, {@code noKip}, hingga {@code koordinat} (titik GPS rumah).
 *       Ini kelas paparan yang sama dengan ekspor PII berkolom-banyak yang sudah dieskalasi
 *       untuk data siswa, kini pada data <b>calon</b> siswa.</li>
 * </ul>
 * <p>Pada sisi induknya, tombol <b>"Perbaiki Urutan Nomor Ujian di Ruang Ujian"</b> di
 * {@code RuangPSBAction} juga dibuat <b>tanpa {@code setVisible(...)} berbasis privilese</b>:
 * satu klik memindahkan <i>seluruh</i> calon siswa satu gelombang ke ruang pertama hasil filter
 * lalu membagi ulang penghuninya antar ruang. Ini instansi tambahan dari pola "tombol mutasi
 * massal tanpa gerbang" pada keluarga layar PSB. Javadoc ini hanya mendokumentasikan keadaan
 * kode; tidak ada perilaku yang diubah.</p>
 *
 * <h2>CATATAN BUG — tombol "Hapus" menyasar tabel modul lain dan mati total</h2>
 * <p>Handler tombol "Hapus" pada {@code RuangPsbCalonSiswaDetailAction} menjalankan SQL mentah
 * {@code "delete from ruang_paket_pmb where calon_siswa=" + id}. Tabel {@code ruang_paket_pmb}
 * adalah tabel modul <b>PMB perguruan tinggi</b> ({@code public.ruang_paket_pmb}, entity
 * {@code ais.database.model.RuangPaketPMB}), bukan {@code sekolah.ruang_gelombang_psb}; kolom
 * FK-nya pun bernama {@code calon_mahasiswa}, bukan {@code calon_siswa}. Jelas hasil salin-tempel
 * dari {@code ais.action.master.pmb.RuangPmbCalonMahasiswaDetailAction} yang hanya nama kolomnya
 * ikut diganti, tabelnya tidak. Akibat saat ini: pernyataan selalu gagal, tertangkap
 * {@code catch}, dan pengguna diberi pesan yang menyesatkan ("Data ini tidak dapat dihapus ..,
 * karena berelasi dengan data lainnya") — <b>tidak ada baris yang pernah terhapus</b>.</p>
 * <p><b>Perhatian "bom waktu" saat memperbaiki:</b> perbaikan yang benar adalah mengganti nama
 * <i>tabel</i> menjadi {@code sekolah.ruang_gelombang_psb}. Perbaikan yang keliru — menyelaraskan
 * nama <i>kolom</i> menjadi {@code calon_mahasiswa} agar "cocok dengan tabelnya" — akan membuat
 * tombol di layar sekolah menghapus alokasi ruang ujian <b>calon mahasiswa</b> yang kebetulan
 * ber-id sama, yaitu perusakan data lintas modul yang senyap. Nilai yang disambung ke SQL adalah
 * {@code Long} hasil {@code getId()}, jadi tidak ada celah injeksi SQL di titik ini.</p>
 *
 * <h2>Jalur PPDB pra-otentikasi yang membaca tabel ini</h2>
 * <p>{@code WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp} membaca alokasi ruang
 * berdasarkan parameter permintaan {@code id} mentah <b>tanpa pemeriksaan sesi apa pun</b>, lalu
 * mengembalikan jadwal ujian beserta nilai yang sudah diperoleh calon siswa bersangkutan;
 * {@code _cetak_kartu_ujian.jsp} berpola sama ({@code request.getParameter("id")}, sesi Hibernate
 * dibuka sendiri, tanpa gerbang). Keduanya berada di direktori yang dapat dijangkau lewat
 * dispatcher {@code hanya_tampil_jsp=true&p=ppdb&s=...} yang sudah tercatat sebagai cacat
 * struktural pada audit berjalan — pengamatan ini memperkuat temuan tersebut, bukan temuan
 * kategori baru.</p>
 *
 * <h2>Catatan pemetaan Hibernate</h2>
 * <p>{@link Id} berada pada {@link #getId()}, sehingga mode akses seluruh entity adalah
 * <b>PROPERTY</b>: Hibernate membaca anotasi pada <i>getter</i> dan mengabaikan anotasi pada
 * setter maupun field. Kedua relasi memakai {@code cascade = {PERSIST, MERGE}} — menyimpan baris
 * alokasi ikut mem-persist/merge {@link RuangPSB} dan {@link CalonSiswa} yang ditunjuk, tetapi
 * <b>tidak</b> ikut menghapusnya (tidak ada {@code REMOVE}/{@code orphanRemoval}).</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, resolusi proxy lazy, dan kontrak audit dimiliki
 * {@link ais.database.model.GeneralValueObject}. Kelas ini hanya memuat state pasangan
 * ruang–calon siswa; persistence, transaksi, dan otorisasi tetap tanggung jawab Action/service
 * pemanggil — jangan menaruh query di model.</p>
 *
 * <p><b>Catatan pengulangan field induk:</b> {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@link #tanggal_dirubah} sengaja <b>dideklarasikan ulang</b> di sini. Ini bukan duplikasi yang
 * keliru: {@link ais.database.model.GeneralValueObject} bukan {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Tanpa deklarasi ulang ini, kolom {@code id}, {@code oleh},
 * {@code oleh_id}, dan {@code tanggal_dirubah} tidak akan ada pemetaannya.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see RuangPSB
 * @see CalonSiswa
 * @see GelombangPendaftaranPsb
 * @see ais.database.model.RuangPaketPMB
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "ruang_gelombang_psb")
public class RuangGelombangPendaftaranPsbPSB extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja <b>sama persis</b> dengan milik
	 * {@code ais.database.model.RuangPaketPMB} — sisa penyalinan berkas antar modul
	 * (sekolah &harr; perguruan tinggi). Karena kedua kelas berbeda nama, kesamaan ini tidak
	 * menimbulkan konflik deserialisasi; dibiarkan apa adanya agar kompatibilitas biner
	 * terhadap sesi/cache lama tetap terjaga.
	 */
	private static final long serialVersionUID = -8522391894818139048L;

	/**
	 * Kunci utama baris alokasi, dibangkitkan basis data ({@link javax.persistence.GenerationType#IDENTITY}).
	 * Dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;

	/**
	 * Id pengguna yang terakhir mengubah baris ini; pasangan teknis dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris alokasi ini.
	 *
	 * @return id pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau kosong (setelah
	 * {@code trim()}) <b>ditolak diam-diam</b>: method langsung kembali tanpa mengubah state dan
	 * tanpa melempar pengecualian. Pola ini mempertahankan jejak audit terakhir agar tidak
	 * terhapus oleh proses yang tidak membawa konteks pengguna (mis. impor atau tugas
	 * terjadwal), tetapi juga berarti pemanggil <b>tidak dapat mengosongkan</b> kolom ini.
	 *
	 * @param olehId id pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penolakan diam-diam yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong diabaikan begitu saja.
	 *
	 * @param oleh nama pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris alokasi ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: dijalankan Hibernate tepat sebelum
	 * pernyataan {@code UPDATE} baris ini dikirim ke basis data. Isinya mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang memperbarui
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif. Merupakan implementasi method {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}, sehingga setiap entity turunan wajib
	 * menyediakannya.
	 *
	 * <p><b>Efek samping:</b> mengubah state instance sesaat sebelum flush. Tidak pernah
	 * dipanggil langsung oleh kode aplikasi. Perlu diingat bahwa {@link #getKodeUnik()} dapat
	 * memicu {@code UPDATE} — dan karenanya callback ini — bahkan pada alur yang hanya membaca
	 * baris (lihat catatan getter destruktif pada Javadoc kelas).</p>
	 *
	 * <p><b>Catatan gaya:</b> deklarasi ini dan field {@link #tanggal_dirubah} berada pada satu
	 * baris sumber yang sama (sisa pembangkitan otomatis lintas berkas model). Field tersebut
	 * menyimpan stempel waktu perubahan terakhir dan diinisialisasi ke waktu server saat objek
	 * dibuat lewat {@code ais.ui.util.WaktuUtil.getDate()}; dibiarkan apa adanya agar diff
	 * terhadap berkas sejenis tetap dapat dibandingkan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual. Berbeda dari
	 * {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini <b>tidak menyaring</b>
	 * nilai {@code null} — memanggilnya dengan {@code null} akan mengosongkan kolom
	 * {@code tanggal_dirubah}. Pada alur normal nilai ini ditulis {@link #onUpdate()}, bukan
	 * oleh pemanggil.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris alokasi ini, dipetakan sebagai
	 * {@link TemporalType#TIMESTAMP} (tanggal beserta jam).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris alokasi, yaitu <b>nama calon siswa</b> penghuni kursi (bukan nama
	 * ruangnya). Dipakai ZK sebagai label bawaan ketika instance ini muncul di combobox/listbox
	 * dan oleh {@code RevisiHelper} sebagai judul dialog revisi.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getCalonSiswa()} lebih dulu, sehingga ikut
	 * memicu resolusi {@code check()} dan menimpa field {@link #calonSiswa}. Aman terhadap
	 * relasi kosong: bila calon siswa belum diisi, mengembalikan string kosong, bukan
	 * melempar {@link NullPointerException}.</p>
	 *
	 * @return nama calon siswa yang menempati ruang ini, atau {@code ""} bila relasi
	 *         {@code calonSiswa} belum terisi
	 */
	public String toString() {
		calonSiswa = getCalonSiswa();
		return calonSiswa == null ? "" : calonSiswa.getNama();
	}

	/**
	 * Ruang ujian PSB tempat calon siswa ditempatkan; relasi {@code LAZY} ke kolom
	 * {@code ruang_psb}.
	 */
	private RuangPSB ruangPSB;

	/**
	 * Calon siswa yang menempati kursi; relasi {@code LAZY} ke kolom {@code calon_siswa}.
	 */
	private CalonSiswa calonSiswa;

	/**
	 * Kunci alternatif unik berformat {@code "<idCalonSiswa>_"}. Nilainya selalu dihitung ulang
	 * oleh {@link #getKodeUnik()} dan menegakkan aturan "satu calon siswa hanya boleh punya satu
	 * baris alokasi di seluruh instalasi" (lihat Javadoc kelas).
	 */
	private String kodeUnik;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi lewat refleksi.
	 * Juga dipakai langsung oleh kode aplikasi ({@code CommonPSB.dapatkanRuangUjian},
	 * {@code RuangPsbCalonSiswaDetailAction}, {@code RuangPSBAction}) untuk membuat alokasi baru
	 * sebelum {@link #setCalonSiswa(CalonSiswa)} dan {@link #setRuangPSB(RuangPSB)} dipanggil.
	 * Tidak melakukan inisialisasi apa pun selain default field (kecuali
	 * {@link #tanggal_dirubah} yang diisi waktu server di deklarasinya).
	 */
	public RuangGelombangPendaftaranPsbPSB() {
	}

	/**
	 * Mengembalikan kunci utama baris alokasi ini. Kehadiran anotasi {@link Id} pada getter
	 * inilah yang membuat mode akses seluruh entity menjadi <b>PROPERTY</b>.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@link javax.persistence.GenerationType#IDENTITY}); id berurutan dan mudah ditebak,
	 * sehingga pemanggil yang menerima id dari luar wajib memeriksa kepemilikan sendiri —
	 * model ini tidak melakukannya.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris alokasi. Pada alur normal hanya dipanggil Hibernate setelah
	 * {@code INSERT}; menyetelnya manual pada objek yang sudah tersimpan akan membuat Hibernate
	 * memperlakukannya sebagai baris lain.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan ruang ujian PSB tempat calon siswa ini ditempatkan.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> nilai dilewatkan {@code check()} milik
	 * {@link GeneralValueObject} lebih dulu — yang meresolusi proxy {@code LAZY} dari cache
	 * in-memory bila memungkinkan — lalu hasilnya <b>ditulis balik</b> ke field
	 * {@link #ruangPSB}. Karena mode akses entity ini PROPERTY, nilai hasil resolusi itulah
	 * yang dipakai Hibernate saat pemeriksaan kotor/flush.</p>
	 *
	 * <p>Relasi memakai {@code cascade = {PERSIST, MERGE}}: menyimpan baris alokasi ikut
	 * mem-persist/merge ruang yang ditunjuk, tetapi tidak pernah menghapusnya.</p>
	 *
	 * @return ruang ujian PSB terkait, atau {@code null} bila belum dialokasikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_psb")
	public RuangPSB getRuangPSB() {
		ruangPSB = check(ruangPSB);
		return ruangPSB;
	}

	/**
	 * Menyetel ruang ujian PSB tempat calon siswa ditempatkan.
	 *
	 * <p><b>Ini adalah operasi pemindahan, bukan penambahan.</b> Karena kolom
	 * {@code kode_unik} memaksa satu baris alokasi per calon siswa (lihat
	 * {@link #getKodeUnik()}), seluruh penulis — {@code CommonPSB.dapatkanRuangUjian(...)},
	 * tombol "Ambil Data Calon Siswa Manual" pada
	 * {@code ais.action.master.psb.RuangPsbCalonSiswaDetailAction}, dan tombol "Perbaiki Urutan
	 * Nomor Ujian di Ruang Ujian" pada {@code ais.action.master.sekolah.RuangPSBAction} —
	 * memuat baris lama calon siswa yang sama lalu memanggil setter ini. Efeknya calon siswa
	 * <b>lenyap dari ruang sebelumnya</b> tanpa pemberitahuan; jejaknya hanya tersisa di tabel
	 * revisi Envers. Tidak ada validasi bahwa ruang baru berada pada gelombang, sekolah, atau
	 * yayasan yang sama dengan calon siswa.</p>
	 *
	 * @param ruangPSB ruang ujian PSB tujuan; {@code null} melepaskan alokasi
	 */
	public void setRuangPSB(RuangPSB ruangPSB) {
		this.ruangPSB = ruangPSB;
	}

	/**
	 * Mengembalikan calon siswa yang menempati kursi pada ruang ini.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> sama seperti {@link #getRuangPSB()} — nilai
	 * dilewatkan {@code check()} untuk meresolusi proxy {@code LAZY}, lalu hasilnya ditulis
	 * balik ke field {@link #calonSiswa}.</p>
	 *
	 * <p>Dipanggil dari renderer grid panel detail, mesin cetak absensi/berita acara/album foto
	 * ({@code RuangPSBAction.getDataAlbumPSBAdmin}, {@code CommonReportPsb}), serta
	 * {@link #toString()}. Relasi memakai {@code cascade = {PERSIST, MERGE}} — menghapus baris
	 * alokasi <b>tidak</b> menghapus calon siswanya.</p>
	 *
	 * @return calon siswa penghuni kursi, atau {@code null} bila baris belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa")
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel calon siswa yang menempati kursi pada ruang ini. Menentukan pula nilai
	 * {@link #getKodeUnik()}, sehingga mengubahnya pada baris yang sudah tersimpan dapat
	 * memicu pelanggaran batasan unik {@code kode_unik} bila calon siswa tujuan sudah punya
	 * alokasi lain.
	 *
	 * @param calonSiswa calon siswa penghuni kursi; {@code null} mengosongkan relasi
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan kunci alternatif unik baris alokasi ini, dipetakan ke kolom
	 * {@code kode_unik} dengan batasan {@code unique = true}.
	 *
	 * <p><b>Nilainya dihitung ulang, bukan dibaca apa adanya (getter destruktif).</b> Bila
	 * {@link #calonSiswa} sudah terisi dan sudah punya id, field {@link #kodeUnik} ditimpa
	 * menjadi {@code "<idCalonSiswa>_"}; bila belum, nilai yang tersimpan dikembalikan apa
	 * adanya. Karena mode akses entity ini PROPERTY, nilai hasil timpaan itulah yang ikut
	 * ter-flush — jadi membaca objek lama yang kolom {@code kode_unik}-nya belum berformat akan
	 * menghasilkan satu {@code UPDATE} tambahan beserta satu revisi Envers. Setelah itu nilainya
	 * stabil (deterministik), sehingga tidak berulang setiap render.</p>
	 *
	 * <p><b>Implikasi struktural penting:</b> karena rumusnya <b>tidak menyertakan ruang</b>,
	 * batasan unik pada kolom ini menjadikan tabel {@code sekolah.ruang_gelombang_psb} pemetaan
	 * 1:1 calon siswa &rarr; ruang untuk <i>seluruh instalasi</i> — lintas ruang, gelombang,
	 * tahun ajaran, sekolah, dan yayasan. Seorang calon siswa tidak pernah bisa punya dua baris
	 * alokasi sekaligus; menempatkannya di ruang lain selalu berarti memindahkan baris yang
	 * sudah ada.</p>
	 *
	 * <p><b>Catatan implementasi:</b> berbeda dari {@link #toString()} yang memakai
	 * {@link #getCalonSiswa()}, method ini membaca field {@link #calonSiswa} <b>langsung</b>
	 * sehingga melewati {@code check()}. Aman untuk proxy Hibernate (memanggil {@code getId()}
	 * pada proxy tidak memicu inisialisasi), tetapi berarti kode unik tidak terhitung bila
	 * field-nya belum terisi walau kolom {@code calon_siswa} di basis data berisi nilai.
	 * Bandingkan dengan {@code ais.database.model.RuangPaketPMB.getKodeUnik()} yang memanggil
	 * kedua getter relasi dan masih memeriksa kecocokan paket; pemeriksaan itu <b>tidak ada</b>
	 * di versi sekolah ini.</p>
	 *
	 * @return kode unik berformat {@code "<idCalonSiswa>_"}, atau nilai tersimpan sebelumnya
	 *         (mungkin {@code null}) bila relasi calon siswa belum terisi/belum ber-id
	 */
	@Column(name = "kode_unik", unique = true)
	public String getKodeUnik() {
		if (calonSiswa != null && calonSiswa.getId() != null) {
			kodeUnik = calonSiswa.getId() + "_";
		}
		return kodeUnik;
	}

	/**
	 * Menyetel kode unik baris alokasi. Praktis hanya dipakai Hibernate saat memuat baris dari
	 * basis data: nilai apa pun yang disetel pemanggil akan <b>ditimpa</b> oleh
	 * {@link #getKodeUnik()} begitu relasi {@link #calonSiswa} terisi.
	 *
	 * @param kodeUnik kode unik baris alokasi
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
