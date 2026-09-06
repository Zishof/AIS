package ais.database.model;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Baris kepesertaan seorang peserta didik pada satu pertemuan kelas — penghubung antara
 * {@link KelasPertemuan} (pertemuan perkuliahan) atau {@link KelasSiswaPunyaSiswa} (jalur
 * persekolahan) dengan orang yang hadir di dalamnya.
 *
 * <h3>Dua domain dalam satu tabel, dengan kewajiban yang timpang</h3>
 * <p>Entity ini melayani dua domain sekaligus: perguruan tinggi lewat {@link #getMahasiswa()} dan
 * {@link #getKelasPertemuan()}, serta persekolahan lewat {@link #getSiswa()} dan
 * {@link #getKelasSiswaPunyaSiswa()}. <b>Namun hanya {@link #getMahasiswa()} yang dipetakan
 * {@code nullable = false}</b> — seluruh kolom lainnya, termasuk kedua kolom jalur persekolahan dan
 * bahkan {@link #getKelasPertemuan()} sendiri, boleh kosong.</p>
 * <p>Akibatnya baris untuk seorang siswa tetap <b>wajib menyebut seorang mahasiswa</b>, yang pada
 * instalasi persekolahan murni tidak masuk akal. Sebaliknya, sebuah baris dapat tersimpan dengan
 * mahasiswa terisi tetapi tanpa satu pun pertemuan — kepesertaan yang tidak menunjuk apa pun.
 * Kewajiban yang timpang ini kemungkinan besar sisa dari masa ketika entity ini hanya melayani
 * perguruan tinggi; jangan menyimpulkan dari pemetaan bahwa jalur persekolahan bersifat
 * tambahan.</p>
 *
 * <h3>Ketidakseragaman pemuatan relasi</h3>
 * <p>{@link #getMahasiswa()} dan {@link #getSiswa()} dimuat {@code LAZY} dan dilindungi
 * {@code check(...)}, sedangkan {@link #getKelasPertemuan()}, {@link #getDetailperkuliahan()}, dan
 * {@link #getKelasSiswaPunyaSiswa()} dimuat {@code EAGER} tanpa pelindung. Objek yang dipindahkan
 * keluar dari session karenanya berperilaku berbeda tergantung properti mana yang dibaca.</p>
 *
 * @see KelasPertemuan
 * @see KelasSiswaPunyaSiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelas_pertemuan")

public class DetailKelasPertemuan extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = 8612385827123829867L;
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
	 * Representasi teks berbentuk {@code "<kelasPertemuan>_<mahasiswa>"}.
	 *
	 * <p><b>Method ini mengubah keadaan objek.</b> Sebelum menyusun teksnya, ia menugaskan kembali
	 * hasil {@link #getMahasiswa()} dan {@link #getKelasPertemuan()} ke field yang bersangkutan.
	 * Karena {@link #getMahasiswa()} menyalurkan nilainya lewat {@code check(...)} yang dapat
	 * mengembalikan instance Java yang berbeda untuk baris yang sama, pemanggilan {@code toString()}
	 * pada entity yang terikat session dapat menjadikan objek itu kotor — sehingga {@code UPDATE}
	 * diterbitkan dan, karena kelas ini {@code @Audited}, Envers mencatat revisi baru. Menuliskan
	 * objek ini ke dalam pesan log sudah cukup untuk memicunya.</p>
	 * <p>Sebuah {@code toString()} semestinya tidak pernah mengubah apa pun. Bila kelak dirapikan,
	 * cukup panggil kedua getter tanpa menugaskan hasilnya kembali — perilaku tampilannya tidak akan
	 * berubah.</p>
	 *
	 * <p>Sisi baiknya, karena memakai getter dan bukan field, method ini memperoleh perlindungan
	 * {@code check(...)} sehingga relatif aman dipanggil pada objek yang lepas dari session. Tanpa
	 * penjagaan {@code null}: relasi yang kosong memunculkan kata {@code "null"} secara harfiah.</p>
	 *
	 * <p>Tidak menyebut {@link #getSiswa()} sama sekali, sehingga baris jalur persekolahan tidak dapat
	 * dikenali dari teks ini.</p>
	 *
	 * @return teks gabungan pertemuan kelas dan mahasiswa
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		kelasPertemuan = getKelasPertemuan();
		return (kelasPertemuan) + "_" + mahasiswa;
	}

	/** Mahasiswa peserta pertemuan; wajib, bahkan pada baris jalur persekolahan. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Siswa peserta pertemuan pada jalur persekolahan; boleh kosong. Lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Pertemuan kelas perkuliahan yang diikuti; boleh kosong. Lihat {@link #getKelasPertemuan()}. */
	private KelasPertemuan kelasPertemuan;
	/** Baris pengambilan mata kuliah yang mendasari kepesertaan ini; boleh kosong. Lihat {@link #getDetailperkuliahan()}. */
	private Detailperkuliahan detailperkuliahan;
	/** Kepesertaan kelas pada jalur persekolahan; boleh kosong. Lihat {@link #getKelasSiswaPunyaSiswa()}. */
	private KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public DetailKelasPertemuan() {
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
	 * Mahasiswa peserta pertemuan ini.
	 *
	 * <p><b>Satu-satunya relasi wajib pada kelas ini</b> ({@code nullable = false}) — termasuk untuk
	 * baris jalur persekolahan, yang seharusnya cukup menyebut {@link #getSiswa()}. Lihat uraian pada
	 * Javadoc kelas.</p>
	 *
	 * <p>Ini pula pembatas kepemilikan sesungguhnya pada entity ini, dan kelas ini tidak menyaring apa
	 * pun berdasarkan nilainya: pemeriksaan bahwa pengguna berhak melihat kehadiran mahasiswa tertentu
	 * harus dilakukan di lapisan action.</p>
	 *
	 * <p>Dimuat {@code LAZY} dan disalurkan lewat {@code check(...)} yang hasilnya <b>ditugaskan
	 * kembali ke field</b>; pada entity terikat session hal itu dapat menerbitkan {@code UPDATE} dan
	 * revisi Envers yang tidak diminta. Perhatikan bahwa method ini menugaskan hasil {@code check} ke
	 * field lalu mengembalikan {@code this.mahasiswa} — bukan hasil {@code check} secara langsung —
	 * sehingga keduanya harus selalu bernilai sama.</p>
	 *
	 * <p>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah mahasiswa; pada tabel sebesar data
	 * mahasiswa, memunculkan baris bayangan secara tidak sengaja berakibat jauh.</p>
	 *
	 * @return mahasiswa peserta; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return this.mahasiswa;
	}

	/**
	 * Menyetel mahasiswa peserta pertemuan ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib; penolakan baru terjadi di
	 * basis data. Tidak memeriksa bahwa mahasiswa ini memang terdaftar pada kelas yang bersangkutan.</p>
	 *
	 * @param mahasiswa mahasiswa peserta
	 * @see #getMahasiswa()
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Pertemuan kelas perkuliahan yang diikuti.
	 *
	 * <p><b>Boleh kosong</b> ({@code nullable = true}) meskipun inilah sisi yang memberi makna pada
	 * baris kehadiran — sebuah baris dapat menyebut mahasiswa tanpa menyebut pertemuan mana pun.
	 * Ketimpangan dengan {@link #getMahasiswa()} yang wajib diuraikan pada Javadoc kelas.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah dan <b>tanpa</b> perlindungan
	 * {@code check(...)} — berbeda dari {@link #getMahasiswa()} dan {@link #getSiswa()} di kelas yang
	 * sama. Membacanya di dalam perulangan atas daftar kehadiran menghasilkan pola N+1.</p>
	 *
	 * @return pertemuan kelas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kelas_pertemuan", nullable = true)
	public KelasPertemuan getKelasPertemuan() {
		return this.kelasPertemuan;
	}

	/**
	 * Menyetel pertemuan kelas perkuliahan yang diikuti.
	 *
	 * <p>Tidak memeriksa apakah baris kehadiran untuk mahasiswa dan pertemuan yang sama sudah ada —
	 * tidak ada batasan keunikan pada pasangan itu, sehingga kehadiran ganda dapat tercatat dan
	 * terhitung dua kali dalam rekapitulasi.</p>
	 *
	 * @param kelasPertemuan pertemuan kelas; boleh {@code null}
	 * @see #getKelasPertemuan()
	 */
	public void setKelasPertemuan(KelasPertemuan kelasPertemuan) {
		this.kelasPertemuan = kelasPertemuan;
	}

	/**
	 * Baris pengambilan mata kuliah (KRS) yang mendasari kepesertaan ini.
	 *
	 * <p>Opsional. Menghubungkan kehadiran dengan rencana studi mahasiswa, sehingga kehadiran dapat
	 * ditelusuri kembali ke mata kuliah yang diambilnya. Bila kosong, kehadiran berdiri sendiri tanpa
	 * kaitan ke KRS — dan tidak ada yang memeriksa bahwa mahasiswa pada baris ini sama dengan
	 * mahasiswa pada baris KRS yang ditunjuk.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah, tanpa perlindungan {@code check(...)}.</p>
	 *
	 * @return baris pengambilan mata kuliah, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detailperkuliahan", nullable = true)
	public Detailperkuliahan getDetailperkuliahan() {
		return detailperkuliahan;
	}

	/**
	 * Menyetel baris pengambilan mata kuliah yang mendasari kepesertaan ini.
	 *
	 * <p>Tidak memeriksa bahwa mahasiswa pada baris KRS itu sama dengan {@link #getMahasiswa()}.</p>
	 *
	 * @param detailperkuliahan baris pengambilan mata kuliah; boleh {@code null}
	 * @see #getDetailperkuliahan()
	 */
	public void setDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/**
	 * Siswa peserta pertemuan pada jalur persekolahan.
	 *
	 * <p>Opsional, dan berpasangan dengan {@link #getKelasSiswaPunyaSiswa()}. Perhatikan bahwa
	 * mengisinya <b>tidak membebaskan baris ini dari kewajiban mengisi {@link #getMahasiswa()}</b> —
	 * lihat uraian pada Javadoc kelas.</p>
	 *
	 * <p>Dimuat {@code LAZY} dan disalurkan lewat {@code check(...)} yang hasilnya ditugaskan kembali
	 * ke field, sama seperti {@link #getMahasiswa()}.</p>
	 *
	 * @return siswa peserta, atau {@code null} pada baris jalur perkuliahan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa peserta pertemuan pada jalur persekolahan.
	 *
	 * <p>Tidak saling mengecualikan dengan {@link #setMahasiswa(Mahasiswa)}: keduanya dapat terisi
	 * sekaligus, dan tidak ada yang memeriksa bahwa keduanya merujuk orang yang sama.</p>
	 *
	 * @param siswa siswa peserta; boleh {@code null}
	 * @see #getSiswa()
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Kepesertaan kelas pada jalur persekolahan — padanan {@link #getDetailperkuliahan()} untuk
	 * domain sekolah.
	 *
	 * <p>Opsional; dimuat {@code EAGER} lewat {@code SELECT} terpisah, tanpa perlindungan
	 * {@code check(...)}. Tidak ada yang memeriksa bahwa siswa pada baris kepesertaan itu sama dengan
	 * {@link #getSiswa()}.</p>
	 *
	 * @return kepesertaan kelas jalur persekolahan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kelas_siswa_punya_siswa", nullable = true)
	public KelasSiswaPunyaSiswa getKelasSiswaPunyaSiswa() {
		return kelasSiswaPunyaSiswa;
	}

	/**
	 * Menyetel kepesertaan kelas pada jalur persekolahan.
	 *
	 * @param kelasSiswaPunyaSiswa kepesertaan kelas; boleh {@code null}
	 * @see #getKelasSiswaPunyaSiswa()
	 */
	public void setKelasSiswaPunyaSiswa(KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa) {
		this.kelasSiswaPunyaSiswa = kelasSiswaPunyaSiswa;
	}

}
