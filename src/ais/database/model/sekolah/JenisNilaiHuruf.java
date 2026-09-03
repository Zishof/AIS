package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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
 * Master <b>Jenis Nilai Huruf</b> pada modul sekolah — label yang <i>memisahkan beberapa skala
 * konversi angka&rarr;huruf yang berjalan berdampingan</i> dalam satu sekolah. Dipetakan ke tabel
 * {@code sekolah.jenis_nilai_huruf}.
 *
 * <h3>KOREKSI PENTING: entity ini BUKAN tabel konversi nilai</h3>
 * Namanya sangat mudah disalahpahami. Kelas ini <b>tidak</b> menyimpan rentang angka maupun huruf
 * hasilnya — tidak ada {@code nilaiMinimal}, {@code nilaiMaksimal}, {@code huruf}, maupun
 * {@code bobot} di sini. Seluruh isinya hanya {@code nama}, {@code keterangan}, saklar
 * {@code aktif}, dan sepasang relasi cakupan {@link Sekolah}/{@link Yayasan}.
 *
 * <p>Baris konversi yang sesungguhnya (<i>90–100 = A</i>, <i>80–89 = B</i>, dan seterusnya) hidup
 * di {@link NilaiHurufSekolah} (tabel {@code sekolah.nilai_huruf_sekolah}) dengan kolom
 * {@code mulai}, {@code sampai}, {@code nilai_huruf}, {@code lulus}, {@code tahun_angkatan},
 * {@code ta}, {@code tahun_akademik} dan {@code semester}. Entity <b>ini</b> hanyalah kolom FK
 * {@code jenis_nilai_huruf} pada tabel tersebut — yaitu <b>dimensi tambahan</b> yang memungkinkan
 * satu sekolah memiliki lebih dari satu skala huruf yang aktif bersamaan (misalnya satu skala untuk
 * ranah Pengetahuan/Keterampilan dan skala lain untuk ranah Sikap, atau skala khusus untuk
 * kegiatan les). Tanpa entity ini, sebuah sekolah hanya bisa punya SATU tabel konversi per
 * angkatan/tahun-akademik.
 *
 * <p>Bahasa layarnya konsisten dengan pembacaan itu: dialog pengelolanya berjudul <i>"Tambah Jenis
 * Nilai Huruf"</i>/<i>"Ubah Jenis Nilai Huruf"</i> dengan isian <i>"Nama Jenis Nilai Huruf *"</i>,
 * <i>"Yayasan *"</i>, <i>"Sekolah *"</i> dan <i>"Keterangan"</i>
 * ({@code ais.action.master.sekolah.JenisNilaiHurufAction#init}); pada kedua layar pemakainya
 * label kombonya adalah <i>"Jenis Nilai Huruf"</i> dengan entri kosong
 * <i>"=Tanpa Jenis Nilai Huruf="</i>.
 *
 * <h3>Dua pemakai TERVERIFIKASI (seluruh repo)</h3>
 * Hanya dua entity yang menunjuk ke kelas ini, keduanya lewat kolom FK bernama sama
 * {@code jenis_nilai_huruf}:
 * <ol>
 * <li>{@link NilaiHurufSekolah#getJenisNilaiHuruf()} — menandai <i>milik skala mana</i> sebuah
 * baris rentang. Diisi dari layar <i>Nilai Huruf</i>
 * ({@code ais.action.master.sekolah.NilaiHurufSekolahAction}).</li>
 * <li>{@link GrupPenilaian#getJenisNilaiHuruf()} — menandai <i>skala mana yang dipakai</i> saat
 * grup penilaian tersebut dikonversi menjadi huruf di rapor. Diisi dari layar <i>Grup
 * Penilaian</i> ({@code ais.action.master.sekolah.GrupPenilaianAction}).</li>
 * </ol>
 * Kedua sisi itulah yang dipertemukan saat pencetakan rapor: {@code LaporanRaporSiswa} memanggil
 * pencari huruf dengan {@code grupPenilaian.getJenisNilaiHuruf()} sebagai kunci, sehingga hanya
 * baris {@link NilaiHurufSekolah} dengan jenis yang sama yang boleh dipakai.
 *
 * <p><b>Perhatikan homonim.</b> Ada kelas lain bernama sama di paket PT,
 * {@code ais.database.model.JenisNilaiHurufMatakuliah} (skema {@code public}), dan kolom
 * {@code jenis_nilai_huruf} juga muncul pada {@code ais.database.model.NilaiHuruf},
 * {@code Matakuliah}, {@code FormatNilaiSkripsi} dan {@code FormatNilaiProposalSkripsi}. Semua itu
 * jalur perguruan tinggi dan <b>tidak</b> berhubungan dengan kelas ini; pencarian teks polos
 * "JenisNilaiHuruf" di repo akan didominasi berkas-berkas PT tersebut.
 *
 * <h3>Bagaimana kunci ini benar-benar dipakai saat runtime</h3>
 * Titik tunggal pemakaiannya adalah
 * {@code NilaiHurufSekolah.getNilaiHurufSekolah(nilai, tahunAngkatan, sekolah, yayasan,
 * tahunAkademik, semester, jenisNilaiHuruf)} — inilah "cari huruf dari angka" yang sesungguhnya.
 * Perilaku yang perlu diketahui pemegang entity ini:
 * <ul>
 * <li>Method itu <b>tidak melakukan query</b>. Ia menyapu cache statis <i>app-wide</i>
 * {@code ConstantValues.nilaiHurufSekolahs} yang dimuat sekali saat startup
 * ({@code InitDataHelper}) dan dimuat ulang hanya oleh
 * {@code ConstantValues.realoadNilaiHurufSekolah(...)} setelah penyimpanan di layar
 * <i>Nilai Huruf</i>. Cache diurutkan {@code tahunAngkatan desc, ta desc, mulai desc}.</li>
 * <li>Pencocokan jenis bersifat <b>berpasangan ketat</b>:
 * {@code (baris.jenis == null && parameter == null) || (keduanya != null && id sama)}. Artinya
 * baris rentang tanpa jenis <b>tidak pernah</b> melayani grup penilaian yang menyebut sebuah
 * jenis, dan sebaliknya. Salah mengisi salah satu sisi membuat konversi gagal total, bukan
 * jatuh ke skala default.</li>
 * <li>Batas rentang <b>inklusif di kedua ujung</b> ({@code nilai >= mulai && nilai <= sampai}).
 * Bila admin membuat rentang bertumpang tindih, yang menang adalah baris pertama pada urutan
 * cache — yaitu yang {@code mulai}-nya paling besar.</li>
 * <li>Bila tidak ada satu pun yang cocok, method mengembalikan {@code null} (huruf kosong di
 * rapor), bukan melempar eksepsi.</li>
 * </ul>
 *
 * <p><b>Konsekuensi yang mudah terlewat:</b> saklar {@link #getAktif()} milik entity ini
 * <b>tidak pernah dibaca</b> pada jalur konversi di atas — penyaringan {@code aktif} hanya terjadi
 * saat mengisi kombo di layar master. Menonaktifkan sebuah Jenis Nilai Huruf karena itu
 * <b>tidak</b> menghentikan pemakaiannya: rapor tetap dikonversi memakai skala tersebut selama
 * masih ada {@link GrupPenilaian} yang menunjuk ke sana. Pola "saklar aktif tak dibaca runtime"
 * ini sama dengan yang tercatat pada {@link KategoriItemPenilaianSiswa}.
 *
 * <h3>BUG: kolom {@code aktif} tidak pernah ditulis layar master (instance ke-4)</h3>
 * {@code JenisNilaiHurufAction#onSave(Event)} hanya menulis {@code nama}, {@code sekolah},
 * {@code yayasan} dan {@code keterangan} — <b>tidak pernah</b> memanggil {@link #setAktif(Boolean)}.
 * Karena kelas ini memakai {@code dynamicInsert = true}, kolom {@code aktif} dihilangkan dari
 * INSERT dan baris baru tersimpan dengan nilai <b>NULL</b>. Rangkaian akibatnya:
 * <ol>
 * <li>Di grid master baris itu <b>tampak tercentang "Aktif"</b>, karena {@link #getAktif()}
 * memetakan {@code null} &rarr; {@code true} — admin tidak punya alasan menyentuhnya.</li>
 * <li>Kedua layar pemakainya menyaring dengan SQL, bukan dengan getter:
 * {@code Common.insertComboDanSemua(..., JenisNilaiHuruf.class, "=Tanpa Jenis Nilai Huruf=",
 * Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)))}. Predikat
 * {@code aktif = true} bernilai UNKNOWN untuk NULL, sehingga barisnya <b>tersaring keluar</b>.</li>
 * <li>Hasil akhir: Jenis Nilai Huruf yang baru dibuat <b>tidak pernah muncul</b> di kombo
 * <i>Nilai Huruf</i> maupun <i>Grup Penilaian</i> — skala baru tak bisa dipakai sama sekali.
 * Satu-satunya penawarnya adalah menekan checkbox <i>Aktif</i> di grid master <b>dua kali</b>
 * (matikan lalu hidupkan) agar {@code false} kemudian {@code true} benar-benar tertulis, atau
 * memakai jalur unggah massal Excel yang memang menyertakan kolom {@code aktif}.</li>
 * </ol>
 * Ini instance ke-4 pola yang sama setelah {@code JenisCatatanSiswa}, {@code JenisNilaiSiswa} dan
 * keluarga {@code JenisLaporanJadwalSekolah}/{@code JenisSKGuru}.
 *
 * <h3>Layar pengelola dan konsekuensi hak akses</h3>
 * Layar CRUD-nya {@code /pages/master/sekolah/jenis_nilai_huruf.zul}
 * ({@code JenisNilaiHurufAction}). Layar itu <b>tidak terdaftar sebagai menu mandiri</b>:
 * satu-satunya penyisipannya di seluruh repo adalah tab <i>Jenis Nilai Huruf</i> di dalam layar
 * Jenis Penilaian ({@code JenisPenilaianAction#onJenisNilaiHuruf(Event)} menyisipkannya lewat
 * {@code MyInclude}). Yang punya entri menu hanyalah induknya,
 * {@code /pages/master/sekolah/jenis_penilaian.zul} (id 881229 pada
 * {@code ais.common.MenuSnapshotData}/{@code MenuInitializer}). Karena
 * {@code CommonPrivilages.checkPrevilages(...)} selalu mengacu ke {@code Common.getCurrentMenu()},
 * seluruh hak CREATE/UPDATE/DELETE yang ditegakkan di sini sesungguhnya adalah hak pada menu
 * <b>Jenis Penilaian</b>. Ini mekanisme <i>pewarisan hak lewat menu induk</i> yang sama dengan yang
 * tercatat pada {@link PaketPsb} dan {@link KategoriItemPenilaianSiswa}.
 *
 * <p>Sisi positifnya perlu dicatat jujur: gerbang di dalam {@code JenisNilaiHurufAction} sendiri
 * <b>ditulis dengan benar</b> — {@code add.setVisible(checkPrevilages(CREATE))}, checkbox
 * <i>Aktif</i> per baris {@code setDisabled(!edit)}, tombol Ubah/Hapus lewat
 * {@code Common.copyEditDeleteButtons(edit, delete, ...)}, dan tombol unggah massal bahkan
 * digerbangi paling ketat ({@code add.isVisible() && edit && delete}). Tidak ada
 * {@code edit}/{@code delete} yang di-<i>hardcode</i>. Masalahnya murni pada <i>menu mana</i> yang
 * ditanyakan gerbang tersebut.
 *
 * <h3>Cakupan tenant (sekolah/yayasan)</h3>
 * {@code JenisNilaiHurufAction#initCriteria(boolean)} hanya menambahkan pembatas sekolah/yayasan
 * bila combo pencarian kebetulan terisi, dan memakai {@code Restrictions.sqlRestriction("1=1")}
 * bila kosong — sedangkan combo itu diisi {@code Common.initYayasanDanSekolahDanSemua(...)} yang
 * menyediakan pilihan "=Semua=". Ini varian <i>fail-open</i> yang sudah dikenal proyek ini.
 * Keparahannya <b>rendah</b>: isi tabel murni metadata katalog (nama skala + keterangan), bukan
 * data pribadi; namun tombol Ubah/Hapus per baris tetap ikut tampil untuk baris sekolah lain bila
 * hak UPDATE/DELETE dimiliki.
 *
 * <p><b>Asimetri "jenis global" yang perlu diketahui.</b> Relasi {@link #getSekolah()} boleh
 * {@code null} di tingkat skema, dan {@link NilaiHurufSekolah} memang punya cabang khusus untuk
 * baris rentang bersekolah-null. Namun kedua kombo pemakainya menyaring dengan
 * {@code Restrictions.eq("sekolah", s)} <b>tanpa</b> {@code isNull("sekolah") OR …}. Akibatnya
 * sebuah Jenis Nilai Huruf global tidak akan pernah bisa dipilih dari layar mana pun, dan selama
 * combo <i>Sekolah</i> pada formulir pemakai belum terisi, {@code s} bernilai {@code null}
 * sehingga kombo jenis hanya berisi entri kosong "=Tanpa Jenis Nilai Huruf=".
 *
 * <h3>Kuirk kecil lain yang sudah diverifikasi</h3>
 * <ul>
 * <li>Berkas {@code jenis_nilai_huruf.zul} memasang checkbox <i>"Tampilkan hanya yang aktif"</i>
 * (id {@code searchaktif}, tercentang secara default). Identifier itu <b>tidak pernah
 * dideklarasikan</b> di {@code JenisNilaiHurufAction} dan {@code initCriteria(boolean)} tidak
 * pernah membatasi kolom {@code aktif} — kendali itu <b>mati total</b>, daftar master selalu
 * menampilkan baris nonaktif juga.</li>
 * <li>{@code ais.common.InitData} mendaftarkan {@code JenisNilaiHuruf.class} <b>dua kali</b> pada
 * daftar pramuat {@code initClasses(...)}. Tidak berbahaya — {@code InitDataHelper#doInitData}
 * dijaga cache {@code MemoryDbUtil.getDataClass()} sehingga pemanggilan kedua langsung
 * dilewati — hanya redundansi.</li>
 * <li>Komentar generator di kelas ini semula berbunyi <i>"JenisGuru generated by hbm2java"</i>.
 * Itu artefak salin-tempel dari {@link JenisGuru} (sumber aslinya sudah diverifikasi pada batch
 * sebelumnya) dan sudah diperbaiki di sini.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()} beserta pasangan setter-nya dan kait {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}/{@link #getYayasan()} beserta setter-nya.</li>
 * <li><b>Isi katalog</b> — {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getAktif()}.</li>
 * </ul>
 * Tidak ada method bisnis, tidak ada pencari statis, dan tidak ada koleksi anak pada kelas ini.
 *
 * <h3>Catatan pewarisan</h3>
 * {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun miliknya.
 * Karena itu {@code id}, {@code oleh}, {@code olehId} dan {@code tanggal_dirubah} <b>harus</b>
 * dideklarasikan ulang di setiap entity turunan. Pengulangan tersebut <b>bukan bug</b>, melainkan
 * keharusan teknis. Yang tetap diwarisi adalah perilaku statis/utilitas seperti
 * {@link GeneralValueObject#check(Object)}.
 *
 * @see NilaiHurufSekolah
 * @see GrupPenilaian
 * @see KategoriItemPenilaianSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "jenis_nilai_huruf", schema = "sekolah")
public class JenisNilaiHuruf extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan agar instance yang sudah tersimpan di session ZK
	 * atau cache tetap dapat dibaca setelah kelas ini diubah (penambahan Javadoc tidak
	 * mengubahnya).
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/**
	 * Kunci utama baris {@code sekolah.jenis_nilai_huruf}. Dideklarasikan ulang karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate — lihat catatan pewarisan pada Javadoc
	 * kelas.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar.
	 */
	private String oleh;
	/**
	 * Identitas (login id) pengguna terakhir yang mengubah baris ini, pendamping {@link #oleh} dan
	 * juga diisi otomatis oleh interceptor audit.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * @return login id pengubah terakhir, atau {@code null} bila baris belum pernah tersentuh
	 *         interceptor audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>mengabaikan diam-diam</b> nilai {@code null} maupun
	 * string kosong/berisi spasi saja — dalam kasus itu ia langsung {@code return} tanpa mengubah
	 * apa pun. Tujuannya menjaga jejak audit lama tidak terhapus oleh pemanggil yang tidak punya
	 * konteks pengguna. Konsekuensinya nilai audit <b>tidak dapat dikosongkan</b> lewat setter ini.
	 *
	 * @param olehId login id pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong <b>diabaikan diam-diam</b> sehingga jejak audit sebelumnya tetap utuh.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang menyegarkan stempel waktu audit tepat sebelum Hibernate
	 * mengeksekusi UPDATE atas baris ini.
	 *
	 * <p>Seluruh pekerjaannya didelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna aktif.
	 * <b>Jangan panggil manual</b> — pemanggilnya adalah provider JPA.
	 *
	 * <p><b>Catatan pembacaan kode:</b> baris ini juga memuat deklarasi field
	 * {@code tanggal_dirubah} (stempel waktu perubahan terakhir, diinisialisasi
	 * {@code ais.ui.util.WaktuUtil.getDate()} sehingga entity baru sudah bertanggal sejak
	 * dibuat). Keduanya sengaja ditulis pada satu baris fisik oleh penulis aslinya; formatnya
	 * dipertahankan apa adanya agar diff tetap minimal.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Normalnya diisi otomatis lewat {@link #onUpdate()}; pemanggilan manual hanya wajar pada
	 * jalur migrasi/impor data yang sengaja mempertahankan waktu asli.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat lewat
	 *         konstruktor kelas ini karena field-nya diinisialisasi saat deklarasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Sekolah pemilik skala ini (kolom {@code sekolah_id}). Boleh {@code null} di tingkat skema,
	 * tetapi baris global praktis tak terpakai — lihat catatan asimetri pada Javadoc kelas.
	 */
	private Sekolah sekolah;
	/**
	 * Yayasan penaung (kolom {@code yayasan_id}). Nilainya selalu diturunkan ulang dari
	 * {@link #sekolah} setiap kali {@link #getYayasan()} dipanggil.
	 */
	private Yayasan yayasan;
	/** Catatan bebas milik admin; murni deskriptif, tidak pernah dipakai logika apa pun. */
	private String keterangan;
	/** Nama skala yang tampil di grid master dan di kedua kombo pemakainya. Wajib diisi. */
	private String nama;

	/**
	 * Saklar aktif. Perhatikan dua hal yang tercatat lengkap pada Javadoc kelas: (1) layar master
	 * <b>tidak pernah menulis</b> kolom ini saat menyimpan sehingga baris baru bernilai
	 * {@code null}, dan (2) kolomnya hanya disaring saat mengisi kombo — jalur konversi nilai di
	 * {@link NilaiHurufSekolah} tidak pernah membacanya.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA dan dipakai layar master saat menekan
	 * tombol <i>Tambah</i> ({@code JenisNilaiHurufAction#onAdd(Event)}).
	 */
	public JenisNilaiHuruf() {
	}

	/**
	 * Konstruktor peninggalan generator hbm2java yang mengisi kunci utama dan nama sekaligus.
	 *
	 * <p><b>Tidak ada satu pun pemanggil di seluruh repo</b> (satu-satunya pembuatan instance
	 * adalah {@code new JenisNilaiHuruf()} pada layar master). Disimpan apa adanya demi
	 * kompatibilitas; hindari memakainya karena menetapkan {@code id} secara manual pada entity
	 * ber-{@code @GeneratedValue(IDENTITY)} akan membuat Hibernate memperlakukan instance sebagai
	 * <i>detached</i>.
	 *
	 * @param id   kunci utama yang ingin ditetapkan.
	 * @param nama nama skala nilai huruf.
	 */
	public JenisNilaiHuruf(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolomnya {@code insertable = false} karena diisi sekuens/identity basis data. Nilai
	 * {@code null} berarti entity belum pernah tersimpan — layar master memakai tepat pemeriksaan
	 * ini untuk memilih judul dialog <i>"Tambah"</i> atau <i>"Ubah"</i>.
	 *
	 * @return kunci utama, atau {@code null} bila entity masih transien.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama.
	 *
	 * @param id kunci utama baru; normalnya hanya diisi Hibernate.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik skala ini.
	 *
	 * <p><b>Non-obvious — getter ini menulis ke field-nya sendiri.</b> Sebelum mengembalikan nilai,
	 * ia menjalankan {@code sekolah = check(sekolah)}, yaitu resolver proksi/identity-map milik
	 * {@link GeneralValueObject#check(Object)}. Efeknya proksi malas ditukar dengan instance
	 * kanonik agar semua pemegang referensi melihat perubahan skalar yang sama. Ini pola yang
	 * dipakai seragam di seluruh entity repo ini dan <b>bukan</b> mutasi domain, tetapi tetap
	 * berarti method baca ini tidak sepenuhnya bebas efek samping.
	 *
	 * @return sekolah pemilik, atau {@code null} untuk baris global.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik skala ini.
	 *
	 * <p><b>Non-obvious:</b> instance yang belum tersimpan (ber-{@code id} {@code null}) diubah
	 * menjadi {@code null} secara diam-diam. Penjaga ini mencegah Hibernate ikut menyimpan objek
	 * {@link Sekolah} setengah jadi lewat {@code CascadeType.PERSIST}, tetapi juga berarti
	 * penetapan gagal tanpa pesan apa pun bila pemanggil mengirim entity transien.
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau entity transien akan tersimpan sebagai
	 *                {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan penaung skala ini.
	 *
	 * <p><b>Non-obvious — getter destruktif (write-back).</b> Method ini <b>tidak</b> sekadar
	 * membaca: ia memanggil {@link #getSekolah()} lalu, bila sekolahnya ada, <b>menimpa</b> field
	 * {@link #yayasan} dengan {@code sekolah.getYayasan()} sebelum menjalankan
	 * {@link GeneralValueObject#check(Object)}. Akibatnya:
	 * <ul>
	 * <li>Yayasan yang ditetapkan manual lewat {@link #setYayasan(Yayasan)} akan <b>dibuang</b>
	 * pada pembacaan pertama bila berbeda dengan yayasan sekolahnya — sekolah selalu menang.</li>
	 * <li>Sekadar merender baris di grid dapat mengubah state entity terkelola, sehingga
	 * berpotensi menghasilkan UPDATE dan revisi Envers tambahan pada flush berikutnya.</li>
	 * <li>Untuk baris global ({@code sekolah == null}), nilai {@link #yayasan} dipertahankan apa
	 * adanya.</li>
	 * </ul>
	 * Pola ini sengaja dipertahankan agar kolom {@code yayasan_id} selalu konsisten dengan
	 * {@code sekolah_id}; jangan "diperbaiki" tanpa memeriksa seluruh pemakainya.
	 *
	 * @return yayasan penaung, atau {@code null} bila tidak dapat diturunkan maupun ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menetapkan yayasan penaung.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setSekolah(Sekolah)}, entity transien
	 * ({@code id} {@code null}) diubah menjadi {@code null} diam-diam. Selain itu perlu diingat
	 * bahwa nilai yang ditetapkan di sini <b>tidak bertahan</b> begitu {@link #getYayasan()}
	 * dipanggil sementara {@link #sekolah} terisi — lihat catatan pada getter tersebut.
	 *
	 * @param yayasan yayasan penaung; {@code null} atau entity transien tersimpan sebagai
	 *                {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan catatan bebas milik baris ini.
	 *
	 * <p>Berbeda dengan beberapa entity lain di repo ini, {@code getKeterangan()} di sini
	 * <b>tidak</b> membalik kontraknya (tidak menghitung, tidak merangkai teks, tidak menulis
	 * field lain) — murni pengembali field. Isinya juga ikut ditampilkan sebagai deskripsi item
	 * pada kedua kombo pemakainya.
	 *
	 * @return catatan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas.
	 *
	 * @param keterangan teks catatan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama skala nilai huruf.
	 *
	 * <p>Nama inilah yang tampil di kolom <i>"Nama Jenis Nilai Huruf"</i> grid master, menjadi
	 * label pada kombo layar <i>Nilai Huruf</i> dan <i>Grup Penilaian</i>, dan dipakai
	 * {@code RevisiHelper.createNewRevisi(...)} sebagai judul riwayat revisi Envers. Kolomnya
	 * {@code nullable = false} dan layar master menolak menyimpan bila kosong.
	 *
	 * @return nama skala.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama skala nilai huruf.
	 *
	 * @param nama nama baru; layar master sudah memvalidasi agar tidak kosong sebelum memanggil
	 *             method ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif skala ini, dengan {@code null} diperlakukan sebagai
	 * <b>aktif</b>.
	 *
	 * <p><b>Non-obvious dan penting.</b> Pemetaan {@code null} &rarr; {@code true} di sini adalah
	 * sumber bug yang diuraikan pada Javadoc kelas: karena layar master tidak pernah menulis kolom
	 * {@code aktif} saat menyimpan, baris baru bernilai NULL, sehingga checkbox di grid tampak
	 * <b>tercentang</b> lewat method ini — padahal kedua kombo pemakainya menyaring di sisi SQL
	 * dengan {@code Restrictions.eq("aktif", true)} yang <b>tidak</b> mencocokkan NULL. Getter ini
	 * karena itu dapat memberi kesan aktif untuk baris yang di praktiknya tak bisa dipilih di
	 * layar mana pun.
	 *
	 * <p>Perhatikan pula bahwa method ini <b>tidak dipanggil sama sekali</b> pada jalur konversi
	 * angka&rarr;huruf ({@code NilaiHurufSekolah.getNilaiHurufSekolah(...)}) — menonaktifkan sebuah
	 * jenis tidak menghentikan pemakaiannya di rapor.
	 *
	 * @return {@code true} bila aktif atau bila kolomnya NULL; {@code false} hanya bila memang
	 *         pernah ditulis {@code false}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif skala ini.
	 *
	 * <p>Satu-satunya pemanggil di layar ZK adalah listener {@code onCheck} checkbox <i>Aktif</i>
	 * pada grid master, yang langsung menyusulinya dengan
	 * {@code Common.refreshSaveOrUpdate(jenisNilaiHuruf)} sehingga perubahan tersimpan seketika
	 * tanpa tombol Simpan. Checkbox itu benar digerbangi {@code setDisabled(!edit)}.
	 * {@code onSave(Event)} pada layar yang sama <b>tidak</b> memanggil method ini — lihat uraian
	 * bug pada Javadoc kelas.
	 *
	 * @param aktif status baru; {@code null} akan dibaca kembali sebagai aktif oleh
	 *              {@link #getAktif()} tetapi <b>tidak</b> cocok dengan penyaring SQL kombo
	 *              pemakainya.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
