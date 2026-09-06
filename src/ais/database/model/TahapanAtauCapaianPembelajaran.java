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

import org.hibernate.envers.Audited;

/**
 * Tahapan atau capaian pembelajaran — satu butir milestone dalam sebuah alur pembelajaran atau
 * bimbingan, dengan bobot persentase terhadap keseluruhan dan cakupan ke sebuah {@link Jenjang}.
 *
 * <p>Perhatikan bahwa entity ini <b>berbeda dari {@link CapaianJurusan}</b> meskipun namanya mirip:
 * {@link CapaianJurusan} adalah rumusan capaian lulusan per jurusan untuk keperluan kurikulum dan
 * akreditasi, sedangkan kelas ini adalah tahapan berbobot dalam sebuah proses — terutama bimbingan,
 * sebagaimana ditunjukkan nilai jatuh-tempo {@link #getJenis()}.</p>
 *
 * <h3>Bobot persentase tidak dijaga totalnya</h3>
 * <p>{@link #getProsentase()} menyimpan bobot tiap tahapan, tetapi tidak ada apa pun di kelas ini
 * yang memastikan jumlah bobot seluruh tahapan dalam satu alur mencapai 100 — atau tidak
 * melampauinya. Penjumlahan dan validasinya harus dilakukan di lapisan yang menyusun alur.</p>
 *
 * <h3>Kelas ini bebas dari pola getter-yang-menulis</h3>
 * <p>Ketiga getter berpenjaga-default di sini — {@link #getProsentase()}, {@link #getJenis()}, dan
 * {@link #getAktif()} — seluruhnya memakai ternary dan tidak menyentuh field. Bandingkan
 * {@code Asesor.getAktif()} dan {@code JenisCapaianJurusan.getFeeder()} yang menulis saat dibaca;
 * bentuk di kelas inilah yang benar untuk entity yang dianotasi {@code @Audited}.</p>
 *
 * @see CapaianJurusan
 * @see Jenjang
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tahapan_atau_capaian_pembelajaran")
public class TahapanAtauCapaianPembelajaran extends GeneralValueObject {

	/**
	 * Nilai {@link #getJenis()} untuk tahapan bimbingan — sekaligus nilai jatuh-tempo bila jenis belum
	 * diisi.
	 *
	 * <p><b>Nama konstanta ini memuat salah ketik: {@code TATAPAN} seharusnya {@code TAHAPAN}.</b>
	 * Isinya sendiri benar ({@code "Tahapan Bimbingan"}), sehingga data yang tersimpan tidak
	 * terpengaruh dan perilaku aplikasi sepenuhnya normal — yang keliru hanya nama pengenalnya di
	 * lapisan Java. Karena konstanta ini {@code public}, mengganti namanya adalah perubahan yang
	 * memutus kompilasi bagi setiap pemakainya; perbaiki bersamaan dengan penyisiran seluruh
	 * pemanggil, bukan sebagai penyuntingan sepele.</p>
	 *
	 * <p>Nilai jenis disimpan sebagai teks bebas dan konstanta ini adalah satu-satunya nilai yang
	 * dikenali kelas ini — tidak ada daftar tertutup, dan tidak ada konstanta pendamping untuk jenis
	 * lain. Lihat {@link #getJenis()}.</p>
	 */
	public static final String TATAPAN_BIMBINGAN = "Tahapan Bimbingan";

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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
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
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	 * field {@code tanggal_dirubah} — yaitu waktu objek Java dibentuk, bukan waktu penyimpanan. Nilai
	 * awal itu diambil dari {@code WaktuUtil.getDate()}, jam aplikasi, yang dapat berbeda dari jam
	 * basis data; bila keduanya tidak selaras, urutan kejadian yang tersusun dari kolom ini bisa
	 * keliru.</p>
	 *
	 * @see #getTanggal_dirubah()
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir. Lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Representasi teks berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga nomor internal ikut terlihat pengguna
	 * akhir. <b>Tidak menyebut jenis maupun jenjang</b>, sehingga tahapan bernama sama pada dua jenjang
	 * berbeda hanya dapat dibedakan lewat id-nya.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * ikut memangkas spasi. Pada baris yang belum tersimpan hasilnya diawali {@code "null-"}.</p>
	 *
	 * @return teks gabungan id dan nama tahapan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama tahapan atau capaian. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas mengenai tahapan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Bobot tahapan ini dalam persen. Lihat {@link #getProsentase()}. */
	private Double prosentase;
	/** Jenjang pendidikan yang memakai tahapan ini; boleh kosong. Lihat {@link #getJenjang()}. */
	private Jenjang jenjang;
	/** Jenis tahapan sebagai teks bebas. Lihat {@link #getJenis()}. */
	private String jenis;
	/** Penanda apakah tahapan ini masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public TahapanAtauCapaianPembelajaran() {
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
	 * Nama tahapan atau capaian pembelajaran, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan — nilai di basis data tetap membawa
	 * spasi apa adanya, sehingga kueri yang mencocokkan kolom {@code nama} secara langsung dapat gagal
	 * menemukan baris yang lewat getter ini terlihat cocok.</p>
	 *
	 * <p>Kolomnya {@code nullable = false} dengan panjang 255; keduanya tidak ditegakkan di Java.
	 * Tidak ada batasan keunikan, termasuk di dalam satu jenjang yang sama.</p>
	 *
	 * @return nama tahapan tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama tahapan atau capaian.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi dan tanpa pemeriksaan panjang.</p>
	 *
	 * @param nama nama tahapan; tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai tahapan ini.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi — berbeda dari {@link #getNama()}.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan tahapan.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Bobot tahapan ini terhadap keseluruhan alur, dinyatakan dalam persen; {@code null} dibaca
	 * sebagai {@code 0.0}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field, sehingga bebas dari
	 * penulisan tak diniatkan dan revisi Envers palsu.</p>
	 *
	 * <p>Nilai jatuh-tempo {@code 0.0} berarti <b>tahapan yang belum diberi bobot tidak menyumbang
	 * apa-apa</b>, bukan menyumbang bagian yang merata. Untuk alur yang bobotnya belum diisi
	 * seluruhnya, total akan menjadi 0 dan pembagian apa pun terhadapnya berisiko menghasilkan
	 * {@code NaN} atau {@code Infinity} — jaga pembaginya di sisi pemanggil.</p>
	 *
	 * <p>Tidak ada pembatasan rentang: nilai negatif maupun di atas 100 akan diterima, dan tidak ada
	 * yang memastikan jumlah bobot seluruh tahapan dalam satu alur sama dengan 100.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code prosentase} secara bawaan.</p>
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum diisi
	 */
	public Double getProsentase() {
		return prosentase == null ? 0.0 : prosentase;
	}

	/**
	 * Menyetel bobot tahapan ini dalam persen.
	 *
	 * <p>Menerima {@code null}, yang akan dibaca sebagai {@code 0.0}. Tidak memvalidasi rentang dan
	 * tidak memeriksa total bobot seluruh tahapan dalam alur yang sama.</p>
	 *
	 * @param prosentase bobot dalam persen; boleh {@code null}
	 * @see #getProsentase()
	 */
	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

	/**
	 * Jenjang pendidikan yang memakai tahapan ini.
	 *
	 * <p>Anotasi kolomnya tidak menyebut {@code nullable}, sehingga berlaku nilai bawaan
	 * {@code true} — <b>relasi ini opsional</b>. Tahapan tanpa jenjang akan hilang dari penyaringan
	 * per jenjang, bukan berlaku untuk semua jenjang; bila maksudnya "berlaku umum", penyaji harus
	 * menangani {@code null} secara khusus.</p>
	 *
	 * <p>Dimuat secara {@code LAZY} dan disalurkan lewat {@code check(...)} yang berusaha menyelesaikan
	 * proksi dari cache atau session, lalu <b>menugaskan hasilnya kembali ke field</b>. Pada entity
	 * yang terikat session, pertukaran instance itu dapat terbaca sebagai perubahan properti sehingga
	 * {@code UPDATE} diterbitkan dan Envers mencatat revisi yang tidak diminta — satu-satunya jalur
	 * penulisan-saat-membaca yang tersisa di kelas ini.</p>
	 *
	 * <p>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah jenjang.</p>
	 *
	 * @return jenjang pendidikan, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang")
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Menyetel jenjang pendidikan yang memakai tahapan ini.
	 *
	 * @param jenjang jenjang pendidikan; boleh {@code null}
	 * @see #getJenjang()
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Jenis tahapan; {@code null} dibaca sebagai {@link #TATAPAN_BIMBINGAN}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field.</p>
	 *
	 * <p>Disimpan sebagai <b>teks bebas</b>, bukan enum. Tidak ada daftar nilai yang sah, tidak ada
	 * pemangkasan spasi, dan tidak ada penyeragaman huruf besar-kecil, sehingga
	 * {@code "Tahapan Bimbingan"} dan {@code "tahapan bimbingan"} tersimpan sebagai dua nilai berbeda
	 * yang tidak akan cocok bila dibandingkan dengan {@code equals}. Bandingkan dengan
	 * {@code equalsIgnoreCase} pada nilai yang sudah dipangkas bila jenis ini dipakai untuk
	 * memutuskan sesuatu.</p>
	 *
	 * <p>Karena nilai jatuh-tempo diterapkan saat membaca, <b>"jenisnya memang tahapan bimbingan" dan
	 * "jenisnya belum diisi" tidak dapat dibedakan</b> lewat getter ini.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code jenis} secara bawaan.</p>
	 *
	 * @return jenis tahapan; {@link #TATAPAN_BIMBINGAN} bila belum diisi
	 */
	public String getJenis() {
		return jenis == null ? TATAPAN_BIMBINGAN : jenis;
	}

	/**
	 * Menyetel jenis tahapan.
	 *
	 * <p>Menerima teks apa pun tanpa pemeriksaan terhadap daftar nilai yang dikenali. Mengirim
	 * {@code null} mengembalikan properti ke nilai jatuh-tempo {@link #TATAPAN_BIMBINGAN} saat
	 * dibaca.</p>
	 *
	 * @param jenis jenis tahapan; tidak divalidasi
	 * @see #getJenis()
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Apakah tahapan ini masih dipakai; {@code null} dianggap dipakai.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field.</p>
	 *
	 * <p><b>Penanda ini dua arah dengan condong ke "aktif".</b> Hanya {@code false} yang benar-benar
	 * menonaktifkan; {@code null} maupun {@code true} sama-sama berarti dipakai. Mengosongkan kolom
	 * bukan cara menonaktifkan sebuah tahapan.</p>
	 *
	 * <p>Perhatikan bahwa menonaktifkan sebuah tahapan <b>tidak menyesuaikan bobot tahapan lain</b>:
	 * bila total bobot sebelumnya 100, mencabut satu tahapan membuat sisanya berjumlah kurang dari
	 * 100 tanpa peringatan apa pun.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code aktif} secara bawaan.</p>
	 *
	 * @return {@code true} bila masih dipakai; hanya {@code false} yang menonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda dipakai-tidaknya tahapan ini.
	 *
	 * <p>Kirim {@code false} untuk menonaktifkan. Mengirim {@code null} <b>tidak</b> menonaktifkan.
	 * Tidak menyesuaikan bobot tahapan lain dalam alur yang sama.</p>
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code true} atau {@code null} berarti dipakai
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
