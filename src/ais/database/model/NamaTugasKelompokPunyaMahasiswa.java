package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.sekolah.Siswa;

/**
 * Keanggotaan seorang peserta didik pada sebuah kelompok tugas — penghubung antara
 * {@link NamaTugasKelompok} dan {@link Mahasiswa} (atau {@link Siswa} pada jalur persekolahan),
 * beserta nilai yang diperolehnya.
 *
 * <h3>Setiap sisi penghubung boleh kosong</h3>
 * <p>Ketiga relasinya — {@link #getNamaTugasKelompok()}, {@link #getMahasiswa()}, dan
 * {@link #getSiswa()} — seluruhnya dipetakan {@code nullable = true}. Sebuah baris keanggotaan
 * karenanya dapat tersimpan <b>tanpa menyebut kelompok mana pun dan tanpa menyebut orang mana pun</b>:
 * baris yang sepenuhnya yatim, tetapi tetap terhitung oleh kode yang mencacah tabel ini — termasuk
 * kode yang menghitung jumlah anggota sebuah kelompok. Untuk entity yang menentukan siapa memperoleh
 * nilai kelompok, kelonggaran itu perlu ditutup di lapisan action yang menyimpan.</p>
 *
 * <p>{@link #getMahasiswa()} dan {@link #getSiswa()} juga tidak saling mengecualikan: keduanya dapat
 * terisi sekaligus, dan tidak ada yang memeriksa bahwa keduanya merujuk orang yang sama.</p>
 *
 * <h3>Kuota kelompok tidak ditegakkan</h3>
 * <p>{@link NamaTugasKelompok#getKuota()} menyatakan batas jumlah anggota, tetapi tidak ada apa pun di
 * kelas ini maupun di basis data yang membatasi berapa banyak baris keanggotaan dapat menunjuk satu
 * kelompok. Tidak ada pula batasan keunikan pada pasangan kelompok dan orang, sehingga seseorang dapat
 * terdaftar dua kali di kelompok yang sama dan terhitung dua kali.</p>
 *
 * @see NamaTugasKelompok
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nama_tugas_kelompok_punya_mahasiswa")
public class NamaTugasKelompokPunyaMahasiswa extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Bagian dari trio jejak audit ringan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang
	 * ditempelkan ke hampir seluruh entity paket ini. Jejak ini terpisah dari — dan jauh lebih miskin
	 * daripada — riwayat Envers yang dihasilkan anotasi {@code @Audited} pada kelas ini: Envers
	 * menyimpan setiap revisi, sedangkan trio ini hanya menyimpan pengubah terakhir.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}. Karena kelas ini memakai pemetaan berbasis
	 * properti, Hibernate tetap memperlakukannya sebagai properti yang dipersistensi dengan nama
	 * kolom bawaan. Jangan mengganti nama getter tanpa memeriksa nama kolom yang sebenarnya ada di
	 * basis data.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah lewat
	 *         jalur yang mengisinya
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir — <b>tetapi menolak nilai kosong secara diam-diam</b>.
	 *
	 * <p>Bila argumennya {@code null} atau hanya berisi spasi, method langsung selesai tanpa mengubah
	 * apa pun dan tanpa melempar. Akibatnya jejak audit ini bersifat <b>satu arah</b>: nilainya dapat
	 * ditimpa oleh id lain, tetapi <b>tidak pernah dapat dikosongkan kembali</b>. Sekali terisi, ia
	 * bertahan selamanya kecuali diganti dengan id yang lain.</p>
	 *
	 * <p>Dua akibat yang perlu diketahui pemanggil. Pertama, kode yang bermaksud membersihkan jejak —
	 * misalnya saat menganonimkan data atau menyalin baris sebagai cetakan baru — akan gagal tanpa
	 * pesan; baris salinan tetap membawa id pengubah dari baris asalnya. Kedua, karena penolakan itu
	 * senyap, pemanggil tidak dapat membedakan "berhasil disetel" dari "diabaikan"; periksa lewat
	 * {@link #getOlehId()} bila hasilnya penting.</p>
	 *
	 * <p>Pola yang sama dipakai {@link #setOleh(String)} dan berulang di hampir seluruh entity paket
	 * ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir — menolak nilai kosong secara diam-diam.
	 *
	 * <p>Berperilaku persis seperti {@link #setOlehId(String)}: {@code null} atau string kosong
	 * diabaikan tanpa pesan, sehingga jejak ini hanya dapat ditimpa dan tidak pernah dikosongkan.
	 * Lihat uraian lengkapnya di sana.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Pasangan {@link #getOlehId()} yang menyimpan nama, bukan id. Keduanya diisi terpisah dan
	 * <b>tidak ada yang menjamin keduanya menunjuk orang yang sama</b> — bila satu jalur hanya
	 * mengisi salah satunya, yang lain tetap membawa nilai lama. Untuk penelusuran yang andal, id
	 * lebih dapat dipercaya karena nama pengguna dapat berubah.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang memperbarui stempel waktu perubahan tepat sebelum baris disimpan.
	 *
	 * <p>Dijalankan Hibernate pada peristiwa {@code @PreUpdate} dan mendelegasikan pekerjaannya ke
	 * {@code AuditTimestampInterceptor.ubah(this)}. Karena kaitnya hanya {@code @PreUpdate} dan bukan
	 * {@code @PrePersist}, stempel waktu pada baris yang <b>baru dibuat</b> berasal dari nilai awal
	 * field — yaitu waktu objek Java dibentuk, bukan waktu penyimpanan. Untuk objek yang dibentuk
	 * lalu baru disimpan jauh kemudian, selisihnya nyata.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> deklarasi field {@code tanggal_dirubah} berbagi baris yang sama
	 * dengan method ini. Ini hasil penyisipan otomatis, bukan kesengajaan gaya. Field itu adalah
	 * stempel waktu perubahan terakhir dan nilai awalnya diambil dari {@code WaktuUtil.getDate()} —
	 * jam aplikasi, yang dapat berbeda dari jam basis data. Bila kedua jam itu tidak selaras, urutan
	 * kejadian yang tersusun dari kolom ini bisa keliru.</p>
	 *
	 * @see #getTanggal_dirubah()
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara langsung.
	 *
	 * <p>Berbeda dengan {@link #setOleh(String)} dan {@link #setOlehId(String)}, setter ini menerima
	 * {@code null} tanpa penolakan — jejak waktu <b>dapat</b> dikosongkan, sedangkan jejak pelakunya
	 * tidak. Ketimpangan itu berarti sebuah baris dapat berakhir dengan "siapa" yang terisi dan
	 * "kapan" yang kosong.</p>
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa oleh {@link #onUpdate()} pada penyimpanan berikutnya,
	 * jadi menyetelnya secara manual hanya bermakna untuk impor data historis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini, dengan ketelitian sampai detik.
	 *
	 * <p>Diperbarui otomatis oleh {@link #onUpdate()} pada setiap pembaruan. Mengembalikan objek
	 * {@link Date} yang dapat diubah — pemanggil yang memanggil {@code setTime(...)} pada hasilnya
	 * ikut mengubah keadaan entity ini. Salin dulu bila nilainya akan dimanipulasi.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibentuk karena
	 *         field-nya diberi nilai awal
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berbentuk {@code "<mahasiswa>-<namaTugasKelompok>"}.
	 *
	 * <p><b>Method ini mengubah keadaan objek.</b> Sebelum menyusun teksnya, ia menugaskan kembali
	 * hasil {@link #getNamaTugasKelompok()} dan {@link #getMahasiswa()} ke field yang bersangkutan.
	 * Karena {@link #getMahasiswa()} menyalurkan nilainya lewat {@code check(...)} yang dapat
	 * mengembalikan instance Java yang berbeda untuk baris yang sama, memanggil {@code toString()} pada
	 * entity yang terikat session dapat menjadikan objek itu kotor — {@code UPDATE} diterbitkan dan,
	 * karena kelas ini {@code @Audited}, Envers mencatat revisi baru. Menuliskan objek ini ke dalam
	 * pesan log sudah cukup untuk memicunya.</p>
	 * <p>Sebuah {@code toString()} semestinya tidak pernah mengubah apa pun; bila dirapikan, cukup
	 * panggil kedua getter tanpa menugaskan hasilnya kembali. Pola yang sama ada pada
	 * {@code DetailKelasPertemuan.toString()}.</p>
	 *
	 * <p>Tidak menyebut {@link #getSiswa()}, sehingga baris jalur persekolahan tampil dengan bagian
	 * pertama berbunyi {@code "null"}. Tanpa penjagaan {@code null} pada relasi mana pun.</p>
	 *
	 * @return teks gabungan mahasiswa dan nama kelompok
	 */
	public String toString() {
		namaTugasKelompok = getNamaTugasKelompok();
		mahasiswa = getMahasiswa();
		return mahasiswa + "-" + namaTugasKelompok;
	}

	/** Kode keanggotaan; dibangkitkan otomatis saat dibaca bila kosong. Lihat {@link #getKode()}. */
	private String kode;

	/** Mahasiswa anggota kelompok; boleh kosong. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Siswa anggota kelompok pada jalur persekolahan; boleh kosong. Lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Kelompok yang diikuti; boleh kosong. Lihat {@link #getNamaTugasKelompok()}. */
	private NamaTugasKelompok namaTugasKelompok;
	/** Keterangan bebas mengenai keanggotaan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nilai yang diperoleh anggota ini. Lihat {@link #getNilai()}. */
	private Double nilai;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public NamaTugasKelompokPunyaMahasiswa() {
	}

	/**
	 * Kunci utama baris ini, dibangkitkan basis data dengan strategi {@code IDENTITY}.
	 *
	 * <p>Bernilai {@code null} sampai entity benar-benar tersimpan. Karena strategi {@code IDENTITY}
	 * memerlukan penyisipan nyata untuk memperoleh nomor, Hibernate tidak dapat menunda
	 * {@code save(...)} pada entity ini sebagaimana yang dilakukannya untuk strategi berbasis
	 * urutan.</p>
	 *
	 * <p>Angka ini hanya unik di dalam tabelnya sendiri. Id yang sama muncul kembali di tabel lain
	 * untuk baris yang sama sekali berbeda, jadi jangan pernah membandingkan id lintas entity atau
	 * memakainya sebagai pengenal tunggal pada peta gabungan.</p>
	 *
	 * @return kunci utama, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara langsung.
	 *
	 * <p>Disediakan untuk Hibernate dan untuk alur impor data yang memuat objek lepas. <b>Jangan
	 * memanggilnya pada entity yang sedang terikat session</b>: mengubah pengenal objek yang dikelola
	 * membingungkan cache tingkat pertama dan dapat berujung pada pembaruan baris yang salah.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Keterangan bebas mengenai keanggotaan ini — misalnya peran anggota dalam kelompok.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan keanggotaan.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel kelompok yang diikuti anggota ini.
	 *
	 * <p>Tidak memeriksa kuota kelompok tujuan dan tidak memeriksa apakah orang yang sama sudah
	 * terdaftar di sana — lihat catatan pada Javadoc kelas.</p>
	 *
	 * @param namaTugasKelompok kelompok tujuan; boleh {@code null}
	 * @see #getNamaTugasKelompok()
	 */
	public void setNamaTugasKelompok(NamaTugasKelompok namaTugasKelompok) {
		this.namaTugasKelompok = namaTugasKelompok;
	}

	/**
	 * Kelompok tugas yang diikuti anggota ini.
	 *
	 * <p><b>Opsional</b> ({@code nullable = true}) meskipun tanpa kelompok baris ini kehilangan
	 * maknanya — lihat catatan baris yatim pada Javadoc kelas.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah dan <b>tanpa</b> perlindungan
	 * {@code check(...)}, berbeda dari {@link #getMahasiswa()} dan {@link #getSiswa()} di kelas yang
	 * sama. Membacanya di dalam perulangan atas daftar anggota menghasilkan pola N+1.</p>
	 *
	 * @return kelompok yang diikuti, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "nama_tugas_kelompok", nullable = true)
	public NamaTugasKelompok getNamaTugasKelompok() {
		return namaTugasKelompok;
	}

	/**
	 * Mahasiswa anggota kelompok ini.
	 *
	 * <p>Opsional, dan berpasangan dengan {@link #getSiswa()} untuk jalur persekolahan. Keduanya boleh
	 * kosong sekaligus — lihat catatan pada Javadoc kelas.</p>
	 *
	 * <p>Ini pembatas kepemilikan sesungguhnya pada entity ini, dan kelas ini tidak menyaring apa pun
	 * berdasarkan nilainya: pemeriksaan bahwa pengguna berhak melihat atau mengubah keanggotaan dan
	 * nilai seseorang harus dilakukan di lapisan action.</p>
	 *
	 * <p>Dimuat {@code LAZY} dan disalurkan lewat {@code check(...)} yang hasilnya <b>ditugaskan
	 * kembali ke field</b>; pada entity terikat session hal itu dapat menerbitkan {@code UPDATE} dan
	 * revisi Envers yang tidak diminta. Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah
	 * mahasiswa.</p>
	 *
	 * @return mahasiswa anggota, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa anggota kelompok ini.
	 *
	 * <p>Tidak saling mengecualikan dengan {@link #setSiswa(Siswa)}, dan tidak memeriksa apakah
	 * mahasiswa ini sudah terdaftar di kelompok yang sama.</p>
	 *
	 * @param mahasiswa mahasiswa anggota; boleh {@code null}
	 * @see #getMahasiswa()
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Kode pengenal keanggotaan, dibangkitkan otomatis pada pembacaan pertama.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca, dan nilai yang ditulisnya baru setiap kali.</b>
	 * Bila {@code kode} masih {@code null}, ia memanggil pembangkit kode batang lalu menyimpan hasilnya
	 * ke field. Berbeda dari penjaga nilai bawaan biasa, nilai yang dihasilkan berbeda pada setiap
	 * pemanggilan — sekadar membaca properti ini menciptakan data baru.</p>
	 *
	 * <p>Karena kelas ini dipetakan lewat akses properti, Hibernate memanggil getter ini saat
	 * menyimpan; pada entity terikat session, membacanya juga menjadikan objek kotor sehingga
	 * {@code UPDATE} diterbitkan dan Envers mencatat revisi yang tidak diminta. Tidak ada batasan
	 * keunikan pada kolom kode, dan pembangkit tidak memeriksa tabrakan dengan kode yang sudah ada.</p>
	 *
	 * <p>Pola yang identik ada pada {@code NamaTugasKelompok.getKode()}; keduanya sebaiknya diubah
	 * bersamaan bila kelak dirapikan, dengan membangkitkan kode sekali di jalur pembuatan baris.</p>
	 *
	 * @return kode keanggotaan; kode yang baru dibangkitkan bila sebelumnya kosong
	 */
	public String getKode() {
		if (kode == null) {
			kode = Common.getGeneratedBarCode();
		}
		return kode;
	}

	/**
	 * Menyetel kode pengenal keanggotaan.
	 *
	 * <p>Mengisinya secara eksplisit sebelum baris pernah dibaca adalah cara menghindari pembangkitan
	 * otomatis pada {@link #getKode()}. Tidak memeriksa keunikan.</p>
	 *
	 * @param kode kode keanggotaan; tidak divalidasi
	 * @see #getKode()
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nilai yang diperoleh anggota ini atas tugas kelompoknya; {@code null} dibaca sebagai
	 * {@code 0.0}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field, berbeda dari
	 * {@link #getKode()} pada kelas yang sama.</p>
	 *
	 * <p><b>"Belum dinilai" dan "dinilai nol" tidak dapat dibedakan lewat getter ini.</b> Nilai
	 * jatuh-tempo {@code 0.0} berarti anggota yang belum dinilai tampil sebagai memperoleh nol —
	 * perbedaan yang berarti pada data yang masuk ke perhitungan nilai akhir. Bila perbedaan itu
	 * dibutuhkan, periksa field-nya lewat jalur lain atau andalkan
	 * {@link #getKeterangan()}.</p>
	 *
	 * <p>Tidak ada pembatasan rentang: nilai negatif maupun di atas skala maksimum akan diterima.
	 * Kelas ini juga tidak menurunkan nilai anggota dari nilai kelompok — keduanya berdiri sendiri,
	 * sehingga anggota satu kelompok dapat memiliki nilai berbeda-beda tanpa ada yang menyelaraskan.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code nilai} secara bawaan.</p>
	 *
	 * @return nilai yang diperoleh; {@code 0.0} bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nilai yang diperoleh anggota ini.
	 *
	 * <p>Menerima {@code null}, yang dibaca sebagai {@code 0.0} — jadi mengosongkan nilai tidak
	 * mengembalikan keadaan "belum dinilai" dari sudut pandang pembaca. Tidak memvalidasi rentang.</p>
	 *
	 * @param nilai nilai yang diperoleh; boleh {@code null}
	 * @see #getNilai()
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Siswa anggota kelompok pada jalur persekolahan — padanan {@link #getMahasiswa()} untuk domain
	 * sekolah.
	 *
	 * <p>Opsional; dimuat {@code LAZY} dan disalurkan lewat {@code check(...)} yang hasilnya ditugaskan
	 * kembali ke field, sama seperti {@link #getMahasiswa()}.</p>
	 *
	 * <p>Perhatikan bahwa {@link #toString()} tidak menyebut properti ini, sehingga baris jalur
	 * persekolahan tidak dapat dikenali dari layar daftar maupun log.</p>
	 *
	 * @return siswa anggota, atau {@code null} pada baris jalur perkuliahan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa anggota kelompok pada jalur persekolahan.
	 *
	 * <p>Tidak saling mengecualikan dengan {@link #setMahasiswa(Mahasiswa)}: keduanya dapat terisi
	 * sekaligus, dan tidak ada yang memeriksa bahwa keduanya merujuk orang yang sama.</p>
	 *
	 * @param siswa siswa anggota; boleh {@code null}
	 * @see #getSiswa()
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

}
