package ais.database.model;

// Generated Apr 14, 2010 10:44:59 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 * Baris antrean penugasan dosen Pembimbing Akademik (PA): pasangan {@link Mahasiswa} dan
 * {@link Dosen} yang menunggu diterapkan ke data mahasiswa oleh proses batch.
 *
 * <h3>Bukan kelas tidur, dan bukan sekadar tabel sementara</h3>
 * <p>Meskipun namanya berakhiran {@code Temporary}, kelas ini terdaftar di
 * {@code hibernate.cfg.xml} dan dipakai secara nyata. Tabel yang dipetakannya pun bernama
 * {@code dosen_pembimbing_akademik} — tanpa akhiran apa pun — dan tidak ada entity lain di basis
 * kode ini yang memetakan tabel tersebut. Jadi "temporary" merujuk pada peran barisnya sebagai
 * antrean, bukan pada tabelnya.</p>
 *
 * <h3>Alur antrean</h3>
 * <ol>
 *   <li>Baris dibuat dengan {@link #getUdah()} bernilai {@code null} — belum diproses.</li>
 *   <li>{@code JamPerkuliahanSyncrhonizerProcessor#procesDosenPa()} memindai baris yang
 *       {@code udah}-nya {@code null}, menerapkan penugasan ke {@code Mahasiswa}, lalu menandai
 *       barisnya {@code udah = true}.</li>
 *   <li>Baris yang sudah ditandai tidak akan diproses ulang.</li>
 * </ol>
 *
 * <h3>Bug pada proses batch yang membaca entity ini</h3>
 * <p><b>Dicatat apa adanya, tidak diperbaiki di sini.</b> Baris penerapan pada
 * {@code procesDosenPa()} berbunyi:</p>
 * <pre>mahasiswa.setDosen(dosenPembimbingAkademikTemporary.getId());</pre>
 * <p>{@code Mahasiswa#setDosen(Long)} mengharapkan <b>id dosen PA</b>. Yang dikirim justru
 * {@code getId()} milik baris antrean ini sendiri — id dari tabel yang berbeda, dengan urutan nomor
 * yang berdiri sendiri. Nilai yang semestinya dipakai adalah {@code getDosen().getId()}. Akibatnya
 * setiap mahasiswa yang diproses memperoleh id pembimbing akademik yang keliru: menunjuk dosen lain
 * yang kebetulan ber-id sama, atau tidak menunjuk dosen mana pun. Karena barisnya langsung ditandai
 * {@code udah = true} setelah itu, penugasan tidak akan pernah diulang tanpa campur tangan manual.
 * Ini bentuk yang sama dengan salah-pakai id anak versus id induk yang berulang di basis kode ini.</p>
 * <p>Proses saudaranya, {@code procesKelas()} atas {@link KelasPunyaMahasiswaTemporary}, membawa
 * cacat yang setara dan sudah dicatat pada Javadoc kelas itu.</p>
 *
 * @see Mahasiswa
 * @see Dosen
 * @see KelasPunyaMahasiswaTemporary
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "dosen_pembimbing_akademik")

public class DosenPembimbingAkademikTemporary extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = -6061173783232357531L;
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
	 * Representasi teks berupa mahasiswa saja, digabung dengan string kosong agar {@code null} aman.
	 *
	 * <p>Bentuk {@code mahasiswa + ""} membuat mahasiswa yang belum diisi menghasilkan teks
	 * {@code "null"} alih-alih melempar — penjagaan yang berlaku untuk {@code null}, tetapi bukan
	 * untuk kegagalan pemuatan lazy: memanggil {@code toString()} milik {@link Mahasiswa} di luar
	 * session aktif tetap dapat melempar {@code LazyInitializationException}.</p>
	 *
	 * <p><b>Tidak menyebut dosen sama sekali</b>, padahal itulah inti baris ini. Dua baris antrean
	 * yang menugaskan dua dosen berbeda kepada mahasiswa yang sama akan tampil identik di layar dan
	 * di log — tepat pada entity yang bugnya (lihat Javadoc kelas) membuat dosen yang tersimpan
	 * menjadi keliru, sehingga jejak yang paling dibutuhkan untuk menelusurinya justru tidak ada.</p>
	 *
	 * <p>Isinya juga membawa identitas mahasiswa ke dalam keluaran log proses batch.</p>
	 *
	 * @return teks representasi mahasiswa, atau {@code "null"}
	 */
	public String toString() {
		return mahasiswa + "";
	}

	/** Dosen yang akan ditetapkan sebagai pembimbing akademik; wajib. Lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Mahasiswa yang akan menerima penugasan; wajib. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;

	/** Kunci gabungan turunan {@code "<idMahasiswa>_<idDosen>"}. Lihat {@link #getUnique_id()}. */
	private String unique_id;
	/** Penanda bahwa baris antrean ini sudah diproses. Lihat {@link #getUdah()}. */
	private Boolean udah;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public DosenPembimbingAkademikTemporary() {
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
	 * Dosen yang akan ditetapkan sebagai pembimbing akademik.
	 *
	 * <p>Relasi wajib — kolom {@code dosen} dinyatakan {@code nullable = false}. <b>Inilah nilai yang
	 * seharusnya dipakai proses batch:</b> {@code getDosen().getId()}, bukan {@link #getId()} milik
	 * baris antrean ini. Lihat catatan bug pada Javadoc kelas.</p>
	 *
	 * <p>Dimuat lewat {@code SELECT} terpisah, sehingga membacanya di dalam perulangan atas antrean
	 * yang panjang menghasilkan pola N+1 — dan proses batch memang memuat baris satu per satu. Riam
	 * {@code PERSIST} dan {@code MERGE} berlaku ke arah dosen.</p>
	 *
	 * @return dosen calon pembimbing akademik; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return this.dosen;
	}

	/**
	 * Menyetel dosen yang akan ditetapkan sebagai pembimbing akademik.
	 *
	 * <p>Tidak memeriksa kuota bimbingan dosen tersebut dan tidak memeriksa apakah pasangan yang sama
	 * sudah pernah diantrekan — yang terakhir itu dijaga indeks unik pada {@link #getUnique_id()},
	 * bukan oleh setter ini.</p>
	 *
	 * @param dosen dosen calon pembimbing akademik
	 * @see #getDosen()
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mahasiswa yang akan menerima penugasan pembimbing akademik.
	 *
	 * <p>Relasi wajib — kolom {@code mahasiswa} dinyatakan {@code nullable = false}. Dimuat lewat
	 * {@code SELECT} terpisah; riam {@code PERSIST} dan {@code MERGE} berlaku ke arah mahasiswa,
	 * sehingga menyimpan baris antrean yang membawa objek mahasiswa baru akan ikut menyimpan
	 * mahasiswa itu. Pada tabel sebesar data mahasiswa, membuat baris bayangan secara tidak sengaja
	 * berakibat jauh — selalu tautkan objek mahasiswa yang sudah dimuat dari basis data.</p>
	 *
	 * <p>Proses batch memanggil {@code setDosen(...)} pada objek yang dikembalikan properti ini lalu
	 * menyimpannya, jadi objek ini bukan sekadar acuan baca.</p>
	 *
	 * @return mahasiswa penerima penugasan; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return this.mahasiswa;
	}

	/**
	 * Menyetel mahasiswa yang akan menerima penugasan.
	 *
	 * @param mahasiswa mahasiswa penerima penugasan
	 * @see #getMahasiswa()
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Kunci gabungan {@code "<idMahasiswa>_<idDosen>"} yang menjaga agar satu pasangan mahasiswa dan
	 * dosen hanya dapat diantrekan sekali.
	 *
	 * <p>Kolomnya dinyatakan {@code unique = true}, jadi penjagaannya nyata dan ditegakkan basis data
	 * — bukan sekadar kesepakatan di lapisan Java. Ini bentuk penjagaan duplikasi yang benar untuk
	 * sebuah antrean.</p>
	 *
	 * <h3>Getter yang menghitung dan menulis saat dibaca</h3>
	 * <p>Nilai tidak disimpan lewat setter melainkan <b>diturunkan di dalam getter ini</b>: bila
	 * {@code mahasiswa} dan {@code dosen} sama-sama terisi, field {@code unique_id} ditulis ulang dari
	 * keduanya sebelum dikembalikan. Karena kelas ini dipetakan lewat akses properti, Hibernate
	 * memanggil getter ini saat menyimpan — sehingga nilai turunanlah yang benar-benar masuk ke basis
	 * data. Itu memang yang diinginkan, tetapi berarti setiap pembacaan properti ini pada entity yang
	 * terikat session dapat menjadikan objek kotor dan menerbitkan {@code UPDATE} beserta revisi
	 * Envers yang tidak diminta.</p>
	 *
	 * <p><b>Dua keadaan yang perlu diwaspadai:</b></p>
	 * <ul>
	 *   <li>Bila salah satu relasi belum terisi, penurunan dilewati dan nilai <b>lama</b> pada field
	 *       dikembalikan apa adanya. Baris yang relasinya diubah menjadi kosong akan tetap membawa
	 *       kunci gabungan yang menunjuk pasangan sebelumnya.</li>
	 *   <li>Field dibaca <b>secara langsung</b>, bukan lewat {@link #getMahasiswa()} dan
	 *       {@link #getDosen()}. Untuk relasi yang dimuat {@code EAGER} seperti di kelas ini hal itu
	 *       aman, tetapi mengubah salah satu relasi menjadi {@code LAZY} kelak akan membuat penurunan
	 *       ini melihat proksi yang belum tentu terinisialisasi.</li>
	 * </ul>
	 *
	 * @return kunci gabungan {@code "<idMahasiswa>_<idDosen>"}, atau nilai lama bila salah satu relasi
	 *         belum terisi
	 */
	@Column(unique = true)
	public String getUnique_id() {
		if (mahasiswa != null && dosen != null) {
			unique_id = mahasiswa.getId() + "_" + dosen.getId();
		}
		return unique_id;
	}

	/**
	 * Menyetel kunci gabungan secara langsung.
	 *
	 * <p><b>Nilai yang disetel di sini bersifat sementara.</b> Begitu {@link #getUnique_id()} dibaca
	 * dengan kedua relasi terisi, nilainya ditimpa oleh hasil penurunan. Setter ini praktis hanya
	 * berguna bagi Hibernate saat memuat baris dari basis data.</p>
	 *
	 * @param unique_id kunci gabungan; akan ditimpa oleh nilai turunan
	 * @see #getUnique_id()
	 */
	public void setUnique_id(String unique_id) {
		this.unique_id = unique_id;
	}

	/**
	 * Penanda bahwa baris antrean ini sudah diproses.
	 *
	 * <p>Bertipe {@code Boolean} dengan <b>tiga keadaan yang bermakna</b>, dan hanya dua di antaranya
	 * yang dipakai:</p>
	 * <ul>
	 *   <li>{@code null} — belum diproses. Inilah yang dicari proses batch; penyaringnya berbunyi
	 *       {@code Restrictions.isNull("udah")}.</li>
	 *   <li>{@code true} — sudah diproses, tidak akan disentuh lagi.</li>
	 *   <li>{@code false} — <b>tidak pernah ditulis oleh kode mana pun</b>, dan karena penyaring
	 *       memakai {@code isNull} dan bukan {@code isFalse}, baris bernilai {@code false} akan
	 *       <b>diabaikan selamanya</b>: dianggap belum selesai oleh manusia yang membacanya, tetapi
	 *       tidak pernah terambil oleh proses batch. Untuk mengantrekan ulang sebuah baris, kosongkan
	 *       penanda ini menjadi {@code null}, jangan menyetelnya {@code false}.</li>
	 * </ul>
	 *
	 * <p>Getter ini murni — tidak seperti {@link #getUnique_id()}, ia tidak menulis apa pun dan tidak
	 * memberi nilai jatuh-tempo. Ketiadaan nilai jatuh-tempo itu justru diperlukan di sini, karena
	 * {@code null} adalah keadaan yang bermakna.</p>
	 *
	 * <p>Tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama {@code udah} secara
	 * bawaan.</p>
	 *
	 * @return {@code true} bila sudah diproses; {@code null} bila masih mengantre
	 */
	public Boolean getUdah() {
		return udah;
	}

	/**
	 * Menyetel penanda sudah-diproses.
	 *
	 * <p>Dipanggil proses batch dengan {@code true} setelah penugasan diterapkan. Untuk mengantrekan
	 * ulang sebuah baris, kirim {@code null} — bukan {@code false}; lihat uraian tiga keadaan pada
	 * {@link #getUdah()}.</p>
	 *
	 * <p>Penandaan dilakukan di transaksi yang <b>terpisah</b> dari penerapan penugasan ke
	 * {@link Mahasiswa}. Bila proses berhenti di antara keduanya, penugasan sudah tersimpan sementara
	 * barisnya masih mengantre, sehingga penerapan akan diulang pada jalannya berikutnya. Untuk
	 * penugasan yang bersifat menimpa, pengulangan itu tidak berbahaya.</p>
	 *
	 * @param udah {@code true} bila sudah diproses; {@code null} untuk mengantrekan ulang
	 * @see #getUdah()
	 */
	public void setUdah(Boolean udah) {
		this.udah = udah;
	}

}
