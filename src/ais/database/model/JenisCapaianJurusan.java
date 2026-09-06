package ais.database.model;

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

/**
 * Jenis capaian pembelajaran jurusan — penggolongan bagi baris {@link CapaianJurusan}, misalnya
 * pembeda antara capaian sikap, pengetahuan, dan keterampilan.
 *
 * <p>Entity master kecil: nama, padanan bahasa Inggris, keterangan, nomor urut tampilan, dan kode
 * pemetaan ke Feeder. Rujukan dari {@link CapaianJurusan#getJenisCapaianJurusan()} bersifat opsional,
 * sehingga capaian dapat berdiri tanpa jenis.</p>
 *
 * <h3>Tanpa penanda aktif — berbeda dari entity yang digolongkannya</h3>
 * <p>Kelas ini <b>tidak memiliki kolom {@code aktif}</b>, padahal {@link CapaianJurusan} yang
 * digolongkannya memilikinya. Jenis yang tidak lagi dipakai karenanya hanya dapat dihapus, dan
 * menghapusnya akan memutus rujukan dari capaian yang pernah memakainya.</p>
 *
 * <h3>Dua getter berpenjaga-default dengan perilaku berbeda</h3>
 * <p>{@link #getNomorUrut()} memakai ternary dan tidak menyentuh field, sedangkan
 * {@link #getFeeder()} <b>menulis ke field saat dibaca</b>. Keduanya berdampingan di kelas yang sama;
 * yang pertama adalah bentuk yang benar. Lihat uraian pada masing-masing method.</p>
 *
 * @see CapaianJurusan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_capaian_jurusan")

public class JenisCapaianJurusan extends GeneralValueObject {

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
	 * Representasi teks berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga nomor internal ikut terlihat pengguna
	 * akhir. Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga
	 * tidak ikut memangkas spasi. Pada baris yang belum tersimpan hasilnya diawali {@code "null-"}.</p>
	 *
	 * @return teks gabungan id dan nama jenis capaian
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama jenis capaian. Lihat {@link #getNama()}. */
	private String nama;
	/** Padanan nama dalam bahasa Inggris. Lihat {@link #getNamaEn()}. */
	private String namaEn;
	/** Keterangan bebas mengenai jenis capaian ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nomor urut tampilan. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Kode padanan jenis capaian ini pada Feeder. Lihat {@link #getFeeder()}. */
	private Long feeder;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public JenisCapaianJurusan() {
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
	 * Nama jenis capaian, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan — nilai di basis data tetap membawa
	 * spasi apa adanya, sehingga kueri yang mencocokkan kolom {@code nama} secara langsung dapat gagal
	 * menemukan baris yang lewat getter ini terlihat cocok.</p>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false} dengan panjang 255; keduanya tidak ditegakkan di
	 * Java. Tidak ada batasan keunikan pada nama.</p>
	 *
	 * @return nama jenis capaian tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis capaian.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi dan tanpa pemeriksaan panjang.</p>
	 *
	 * @param nama nama jenis capaian; tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai jenis capaian ini.
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
	 * Menyetel keterangan jenis capaian.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode padanan jenis capaian ini pada Feeder (pangkalan data pendidikan tinggi), dengan nilai
	 * jatuh-tempo {@code 1}.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca.</b> Bila {@code feeder} masih {@code null}, ia
	 * menetapkannya menjadi {@code 1L} lalu mengembalikannya. Pada entity yang terikat session
	 * Hibernate hal itu menjadikan objek kotor: {@code UPDATE} diterbitkan pada flush berikutnya dan
	 * — karena kelas ini {@code @Audited} — Envers mencatat revisi baru untuk perubahan yang tidak
	 * pernah diminta siapa pun. Sekadar menampilkan daftar jenis capaian di layar sudah cukup untuk
	 * menulis ulang seluruh baris yang kolom feeder-nya masih kosong.</p>
	 *
	 * <p>Akibat lain: setelah pembacaan pertama, "kode Feeder-nya memang 1" dan "kode Feeder-nya belum
	 * pernah diisi" tidak lagi dapat dibedakan.</p>
	 *
	 * <p>Bandingkan dengan {@link #getNomorUrut()} pada kelas yang sama, yang memberi nilai
	 * jatuh-tempo memakai ternary tanpa menyentuh field. Itulah bentuk yang benar; getter ini
	 * sebaiknya diseragamkan ke sana. Pola yang persis sama ada pada
	 * {@code JenisEvaluasi.getFeeder()}.</p>
	 *
	 * @return kode Feeder; {@code 1} bila belum pernah diisi
	 */
	public Long getFeeder() {
		if (feeder == null) {
			feeder = 1L;
		}
		return feeder;
	}

	/**
	 * Menyetel kode padanan Feeder untuk jenis capaian ini.
	 *
	 * <p>Menerima {@code null}, tetapi {@link #getFeeder()} akan segera menggantinya dengan {@code 1}
	 * pada pembacaan berikutnya — mengosongkan nilai ini tidak bertahan lama.</p>
	 *
	 * @param feeder kode Feeder; tidak divalidasi terhadap daftar kode yang sah
	 * @see #getFeeder()
	 */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * Nomor urut tampilan jenis capaian ini; {@code null} dibaca sebagai {@code 0}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary dan tidak menyentuh field, berbeda dengan
	 * {@link #getFeeder()} pada kelas yang sama. Inilah bentuk penjaga nilai jatuh-tempo yang benar
	 * untuk entity yang dianotasi {@code @Audited}.</p>
	 *
	 * <p><b>Tidak ada batasan keunikan pada nomor urut.</b> Beberapa jenis capaian dapat memakai nomor
	 * yang sama, dan karena nilai jatuh-tempo {@code 0} berlaku untuk semua baris yang belum diberi
	 * nomor, baris-baris itu akan berkerumun di urutan yang sama. Urutan di antara mereka lalu
	 * ditentukan oleh pengurut sekunder — atau, bila tidak ada, oleh urutan baris yang kebetulan
	 * terbaca basis data, yang tidak dijamin tetap antar pemanggilan.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code nomorUrut} secara bawaan.</p>
	 *
	 * @return nomor urut tampilan; {@code 0} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampilan jenis capaian ini.
	 *
	 * <p>Menerima {@code null}, yang akan dibaca sebagai {@code 0} oleh {@link #getNomorUrut()}.
	 * Tidak memeriksa tabrakan dengan nomor urut jenis capaian lain.</p>
	 *
	 * @param nomorUrut nomor urut tampilan; boleh {@code null}
	 * @see #getNomorUrut()
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Padanan nama jenis capaian dalam bahasa Inggris, dipakai pada dokumen berbahasa Inggris.
	 *
	 * <p>Bersifat pilihan dan sering kosong. <b>Dikembalikan apa adanya tanpa pemangkasan spasi</b>,
	 * berbeda dari {@link #getNama()} yang memangkasnya — ketidakseragaman antara dua getter nama pada
	 * kelas yang sama. Perhatikan bahwa {@link CapaianJurusan} yang digolongkan entity ini justru
	 * memangkas <i>kedua</i> nama, sehingga perlakuan spasi berbeda antara jenis dan capaiannya.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code namaEn} secara bawaan — perhatikan bahwa {@link CapaianJurusan} memetakan properti
	 * senama ke kolom {@code nama_en}. Kedua tabel karenanya memakai gaya penamaan kolom yang berbeda
	 * untuk hal yang sama.</p>
	 *
	 * @return nama dalam bahasa Inggris, atau {@code null} bila belum diisi
	 */
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menyetel padanan nama dalam bahasa Inggris.
	 *
	 * @param namaEn nama dalam bahasa Inggris; boleh {@code null}, tidak divalidasi
	 * @see #getNamaEn()
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}
}
