package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Kelompok penomoran surat: wadah yang memungkinkan BEBERAPA templat
 * {@link NomorSurat} berbagi SATU deret nomor yang sama, sekaligus menyimpan
 * daftar pengguna/role yang diarahkan memakai kelompok tersebut.
 *
 * <h3>Mengapa kelompok diperlukan</h3>
 * Secara default setiap templat {@link NomorSurat} punya deret urutnya sendiri. Namun praktik
 * persuratan dan keuangan sering menuntut sebaliknya: satu unit kerja memakai beberapa BENTUK
 * nomor yang berbeda (mis. nomor untuk surat keputusan, surat tugas, dan nota dinas) tetapi
 * semuanya harus berurutan di dalam SATU buku agenda. Kelompok inilah yang menyatukan mereka.
 *
 * <h3>Dua peran yang berbeda &mdash; keduanya bergantung pada satu flag</h3>
 * Kelompok baru berpengaruh bila templat yang menunjuknya juga mengaktifkan
 * {@link NomorSurat#getUrutBerdasarkanKelompok()}. Bila flag itu tidak aktif, kolom kelompok
 * hanya menjadi label yang tidak berdampak apa pun pada nomor yang terbit. Ketika aktif,
 * kelompok berperan pada dua tempat yang terpisah:
 * <ol>
 *   <li><b>Offset awal deret.</b> Di {@link NomorSurat#format(Long, java.util.Date,
 *       ais.database.model.rab.SatuanKerja)}, angka yang dicetak pada slot
 *       {@link NomorSurat#NOMOR_URUT} ditambah {@link #getMulaiUrutanKe()} milik kelompok ini
 *       &mdash; MENGGANTIKAN {@code mulaiUrutanKe} milik templat itu sendiri.</li>
 *   <li><b>Cakupan penghitungan.</b> Di method {@code getindex(NomorSurat)} yang disalin ke
 *       hampir setiap Action pemakai, query penghitung difilter
 *       {@code nomorSurat.kelompokNomorSurat = kelompok ini} alih-alih {@code nomorSurat =
 *       templat ini}, sehingga seluruh dokumen yang templatnya se-kelompok ikut tercacah dalam
 *       satu deret.</li>
 * </ol>
 * Perhatikan bahwa peran kedua HANYA berlaku pada mode hitung baris. Untuk templat yang memakai
 * mode counter tersimpan ({@link NomorSurat#getGunakanIndexUrut()} = true), counter berada di
 * kolom {@code nomorIndex} MILIK MASING-MASING TEMPLAT, sehingga templat se-kelompok TIDAK
 * benar-benar berbagi deret &mdash; hanya offset awalnya yang seragam. Kombinasi "urut
 * berdasarkan kelompok" dengan "gunakan index urut" karena itu tidak menghasilkan penomoran
 * bersama seperti yang mungkin diharapkan admin.
 *
 * <h3>Daftar pengguna dan grup pengguna</h3>
 * Field {@link #getUserid()} dan {@link #getGrupUserid()} menyimpan daftar id pengguna dan id
 * role yang dipisah titik koma. Keduanya dibaca
 * {@code ais.action.master.surat.KelompokNomorSuratAction.checkKelompok(Combobox)} untuk
 * MEMILIH OTOMATIS dan MENGUNCI dropdown kelompok pada layar surat bila pengguna berjalan
 * tercantum di salah satu daftar. Sifatnya kemudahan antarmuka, BUKAN kontrol akses: penguncian
 * dilakukan dengan menonaktifkan komponen di layar, dan tidak ada pemeriksaan padanan di sisi
 * penyimpanan yang menolak dokumen bila kelompok yang terkirim bukan kelompok yang diizinkan.
 * Jangan memperlakukan kedua field ini sebagai pembatas wewenang.
 *
 * <h3>Catatan Hibernate/Envers</h3>
 * Pemetaan berbasis anotasi pada getter, {@code dynamicInsert}/{@code dynamicUpdate} aktif, dan
 * seluruh perubahan direkam Envers lewat {@link Audited}. Field audit {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah} ditulis ulang di kelas ini karena {@link GeneralValueObject} bukan
 * {@code @Entity} sehingga propertinya tidak ikut terpetakan &mdash; keharusan teknis, bukan
 * duplikasi ceroboh.
 *
 * <h3>Pemeliharaan</h3>
 * CRUD-nya ada di {@code ais.action.master.surat.KelompokNomorSuratAction} (dengan validasi nama
 * wajib dan tidak duplikat di lapisan layar; basis data hanya menjamin kolom nama tidak null).
 *
 * @see NomorSurat
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "kelompok_nomor_surat")
public class KelompokNomorSurat extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja sama dengan sejumlah entity lain hasil
	 * generator hbm2java; jangan diubah agar sesi ZK lama yang masih memegang objek ini tidak
	 * gagal dideserialisasi setelah <i>redeploy</i>.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer baris kelompok, dibangkitkan basis data ({@code IDENTITY}). */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (field audit bayangan &mdash; lihat catatan
	 * pada Javadoc kelas mengenai {@link GeneralValueObject} yang bukan {@code @Entity}).
	 */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini (pasangan teknis dari {@link #oleh}). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris kelompok ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan penjagaan "hanya-maju": nilai {@code null}
	 * maupun string kosong/spasi DIABAIKAN sehingga jejak audit lama dipertahankan.
	 *
	 * <p>Pola ini konsisten di seluruh entity AIS. Tujuannya agar jalur penyimpanan yang tidak
	 * mengetahui identitas pengguna &mdash; impor massal lewat tombol unggah data, penjadwal
	 * latar, pemanggilan tanpa sesi ZK &mdash; tidak menghapus jejak audit yang sudah benar.
	 * Konsekuensinya field ini TIDAK dapat dikosongkan kembali lewat setter.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila null atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan penjagaan "hanya-maju" yang sama persis
	 * dengan {@link #setOlehId(String)}: nilai null/kosong diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila null atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris kelompok ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dipicu SEBELUM setiap {@code UPDATE} baris kelompok;
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getTanggal_dirubah()} beserta {@link #getOleh()}/{@link #getOlehId()} dari sesi
	 * pengguna yang sedang berjalan.
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} ditulis pada BARIS YANG SAMA dengan method ini
	 * (gaya penulisan hasil penyisipan otomatis di seluruh entity AIS). Field tersebut
	 * diinisialisasi ke waktu sekarang lewat {@code WaktuUtil.getDate()} &mdash; bukan
	 * {@code new Date()} &mdash; agar mengikuti zona waktu/penyetelan waktu aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir secara manual.
	 *
	 * <p>Umumnya TIDAK perlu dipanggil kode aplikasi: nilainya diisi otomatis oleh
	 * {@link #onUpdate()}. Setter ini ada terutama demi kebutuhan Hibernate saat memuat baris dan
	 * demi jalur impor data yang ingin mempertahankan stempel waktu asal.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris kelompok (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah null untuk objek yang baru dibuat di memori
	 */
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks kelompok untuk dropdown dan label, berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Berbeda dengan {@link NomorSurat#toString()} yang ikut menghitung contoh nomor dan
	 * karenanya menjalankan query, method ini murni membaca dua field di memori sehingga murah
	 * dan aman dipanggil dari jalur mana pun, termasuk saat sesi Hibernate sudah ditutup.</p>
	 *
	 * <p>Perhatikan bahwa {@code nama} dibaca dari FIELD langsung, bukan lewat
	 * {@link #getNama()}, sehingga spasi tepi (bila ada di data) ikut tampil di sini padahal
	 * tidak tampil pada pembacaan lewat getter.</p>
	 *
	 * @return string ringkas berisi id dan nama kelompok
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama kelompok yang ditampilkan admin pada dropdown pemilih (mis. "Agenda Keuangan Pusat").
	 * Kolom {@code nama} bersifat {@code NOT NULL} di basis data; kewajiban isi dan larangan
	 * duplikat ditegakkan di layar {@code KelompokNomorSuratAction}, bukan oleh constraint.
	 */
	private String nama;

	/**
	 * Daftar id pengguna yang diarahkan memakai kelompok ini, dipisah titik koma. Dibaca
	 * {@code KelompokNomorSuratAction.checkKelompok} untuk memilih otomatis dan mengunci dropdown
	 * kelompok di layar surat. Kemudahan antarmuka, bukan kontrol akses &mdash; lihat Javadoc
	 * kelas.
	 */
	private String userid;

	/**
	 * Daftar id role (grup pengguna) yang diarahkan memakai kelompok ini, dipisah titik koma.
	 * Berperan sama dengan {@link #userid} dan dievaluasi lebih dulu oleh {@code checkKelompok}.
	 */
	private String grupUserid;

	/** Catatan bebas admin mengenai peruntukan kelompok ini; tidak berpengaruh pada penomoran. */
	private String keterangan;

	/**
	 * Offset awal deret milik kelompok: angka yang DITAMBAHKAN ke index dokumen saat slot
	 * {@link NomorSurat#NOMOR_URUT} dicetak, menggantikan {@code mulaiUrutanKe} milik templat
	 * bila {@link NomorSurat#getUrutBerdasarkanKelompok()} aktif. Default {@code 1L}.
	 */
	private Long mulaiUrutanKe = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membentuk instance saat
	 * memuat baris, dan dipakai layar CRUD {@code KelompokNomorSuratAction} untuk membuat
	 * kelompok baru. Satu-satunya nilai default yang berlaku ({@code mulaiUrutanKe = 1L})
	 * berasal dari inisialisasi field, bukan dari konstruktor ini.
	 */
	public KelompokNomorSurat() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	/**
	 * Mengembalikan kunci primer baris kelompok.
	 *
	 * @return id kelompok, atau {@code null} bila objek belum pernah disimpan
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer. Umumnya hanya dipanggil Hibernate; pengisian manual dipakai jalur
	 * unggah/impor massal yang mempertahankan id asal.
	 *
	 * @param id kunci primer kelompok
	 */
	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "nama", nullable = false, length = 255)
	/**
	 * Mengembalikan nama kelompok, sudah dibersihkan spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan di GETTER, bukan di setter &mdash; nilai yang tersimpan di basis
	 * data bisa saja masih mengandung spasi tepi. Konsekuensinya pemeriksaan duplikat yang
	 * dilakukan langsung di sisi SQL oleh layar CRUD tidak ikut terpangkas dan bisa meleset untuk
	 * data yang terlanjur berspasi; demikian pula {@link #toString()} yang membaca field mentah.</p>
	 *
	 * @return nama kelompok tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama kelompok. Tidak ada normalisasi maupun validasi di sini.
	 *
	 * @param nama nama kelompok
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true)
	/**
	 * Mengembalikan catatan bebas admin mengenai kelompok ini (apa adanya, tanpa pemangkasan).
	 *
	 * @return keterangan kelompok, atau {@code null} bila kosong
	 */
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas admin mengenai kelompok ini.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan daftar id pengguna yang diarahkan memakai kelompok ini, dipisah titik koma,
	 * sudah dipangkas spasi tepinya dan dinormalkan menjadi string KOSONG (bukan {@code null})
	 * bila belum diisi.
	 *
	 * <p>Penormalan ke string kosong itu penting bagi pemanggilnya: {@code checkKelompok} langsung
	 * memanggil {@code .split(";")} pada nilai balik tanpa pemeriksaan null. Untuk nilai kosong,
	 * {@code split} menghasilkan larik berisi satu elemen string kosong, yang kemudian dibandingkan
	 * dengan id pengguna berjalan dan tidak akan cocok selama id pengguna tidak kosong &mdash;
	 * jadi kelompok tanpa daftar pengguna aman: ia tidak terpilih otomatis untuk siapa pun.</p>
	 *
	 * <p>Perhatikan pemangkasan hanya berlaku pada ujung SELURUH string, bukan pada tiap elemen;
	 * pemangkasan per elemen dilakukan pemanggil saat membandingkan. Pembandingan bersifat tidak
	 * sensitif huruf besar/kecil.</p>
	 *
	 * @return daftar id pengguna dipisah titik koma; tidak pernah null, minimal string kosong
	 */
	public String getUserid() {
		return userid == null ? "" : userid.trim();
	}

	/**
	 * Menetapkan daftar id pengguna yang diarahkan memakai kelompok ini.
	 *
	 * <p>Nilai disimpan APA ADANYA &mdash; tidak dipangkas, tidak divalidasi formatnya, dan tidak
	 * diperiksa apakah id yang dicantumkan benar-benar ada. Konvensi pemisah titik koma sepenuhnya
	 * bergantung pada disiplin admin yang mengisi.</p>
	 *
	 * @param userid daftar id pengguna dipisah titik koma; boleh null
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * Mengembalikan offset awal deret milik kelompok, dengan penormalan ke {@code 1L} bila null.
	 *
	 * <p><b>Getter ini MENULIS field.</b> Ketika nilainya null, getter tidak sekadar mengembalikan
	 * {@code 1L} melainkan juga MENUGASKAN {@code 1L} ke field. Pada entity terkelola Hibernate,
	 * penugasan semacam ini membuat baris ikut ter-<i>dirty</i> dan tertulis pada flush berikutnya
	 * walau pemanggil hanya bermaksud membaca. Pola getter destruktif yang berulang di banyak
	 * entity AIS; di sini dampaknya jinak karena nilai yang ditulis sama dengan default yang
	 * memang diinginkan.</p>
	 *
	 * <p>Angka ini dipakai {@link NomorSurat#format(Long, java.util.Date,
	 * ais.database.model.rab.SatuanKerja)} sebagai penambah index dokumen, dan hanya bila templat
	 * yang bersangkutan mengaktifkan {@link NomorSurat#getUrutBerdasarkanKelompok()} DAN benar
	 * menunjuk kelompok ini. Karena bersifat penambah &mdash; bukan "mulai dari" secara harfiah
	 * &mdash; nilai default {@code 1L} menggeser seluruh deret satu angka ke atas.</p>
	 *
	 * @return offset awal deret kelompok; tidak pernah null
	 */
	public Long getMulaiUrutanKe() {
		if (mulaiUrutanKe == null) {
			mulaiUrutanKe = 1L;
		}
		return mulaiUrutanKe;
	}

	/**
	 * Menetapkan offset awal deret kelompok.
	 *
	 * <p>Perubahan di sini berdampak pada SELURUH templat {@link NomorSurat} yang menunjuk
	 * kelompok ini dan mengaktifkan urut-berdasarkan-kelompok &mdash; bukan hanya satu templat.
	 * Menggesernya mundur dapat membuat nomor yang sudah terbit diterbitkan ulang. Tidak ada
	 * validasi nilai negatif maupun pemeriksaan terhadap nomor yang telah terbit.</p>
	 *
	 * @param mulaiUrutanKe offset awal deret; null akan dinormalkan menjadi {@code 1L} saat dibaca
	 */
	public void setMulaiUrutanKe(Long mulaiUrutanKe) {
		this.mulaiUrutanKe = mulaiUrutanKe;
	}

	/**
	 * Mengembalikan daftar id role (grup pengguna) yang diarahkan memakai kelompok ini, dipisah
	 * titik koma, sudah dipangkas spasi tepinya dan dinormalkan menjadi string KOSONG bila belum
	 * diisi &mdash; dengan alasan yang sama seperti dijelaskan pada {@link #getUserid()}.
	 *
	 * <p>Di dalam {@code KelompokNomorSuratAction.checkKelompok}, daftar role dievaluasi LEBIH
	 * DULU daripada daftar pengguna, namun keduanya berada di dalam perulangan yang menelusuri
	 * seluruh item dropdown tanpa berhenti setelah kecocokan pertama: {@code break} di dalamnya
	 * hanya keluar dari perulangan elemen, bukan dari perulangan item. Akibatnya, bila seorang
	 * pengguna tercantum pada LEBIH DARI SATU kelompok, kelompok yang akhirnya terpilih adalah
	 * yang cocok TERAKHIR menurut urutan item dropdown &mdash; dan dropdown tetap terkunci.
	 * Hindari mencantumkan satu pengguna/role di beberapa kelompok sekaligus.</p>
	 *
	 * @return daftar id role dipisah titik koma; tidak pernah null, minimal string kosong
	 */
	public String getGrupUserid() {
		return grupUserid == null ? "" : grupUserid.trim();
	}

	/**
	 * Menetapkan daftar id role (grup pengguna) yang diarahkan memakai kelompok ini. Sama seperti
	 * {@link #setUserid(String)}, nilai disimpan apa adanya tanpa pemangkasan maupun validasi
	 * bahwa role yang dicantumkan benar-benar ada.
	 *
	 * @param grupUserid daftar id role dipisah titik koma; boleh null
	 */
	public void setGrupUserid(String grupUserid) {
		this.grupUserid = grupUserid;
	}
}
