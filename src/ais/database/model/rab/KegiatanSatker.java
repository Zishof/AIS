package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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
 * Entitas master <b>Kegiatan Satker</b> pada modul RAB/perencanaan anggaran, dipetakan ke tabel
 * {@code rab.kegiatan_satker}. Secara struktur entitas ini adalah master paling sederhana di seluruh
 * paket {@code ais.database.model.rab}: hanya membawa identitas ({@code id}), sepasang label bebas
 * ({@code nama} dan {@code keterangan}), plus tiga field jejak audit bayangan yang diwarisi pola
 * repo ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}). Tidak ada satu pun relasi
 * {@code @ManyToOne}/{@code @OneToMany}, tidak ada kolom tanggal periode, tidak ada nilai anggaran,
 * dan tidak ada flag aktif.
 *
 * <h2>PERINGATAN PENTING #1 — nama "Kegiatan" TIDAK berarti berkerabat dengan Kegiatan lain</h2>
 * <p>Basis kode AIS memuat banyak entitas berawalan "Kegiatan" yang secara konsep <b>sama sekali
 * berbeda</b> dan tidak saling berelasi. Verifikasi atas kode sumber menunjukkan
 * {@code KegiatanSatker} <b>TIDAK</b> memiliki foreign key, kolom bersama, superclass bersama
 * (selain {@link GeneralValueObject} yang diwarisi hampir seluruh entitas AIS), maupun pemakaian
 * bersama dengan salah satu dari:</p>
 * <ul>
 *   <li>{@code ais.database.model.Kegiatan} — entitas kegiatan pada modul billing/tagihan;</li>
 *   <li>{@code ais.database.model.DetailKegiatan} / {@code ais.database.model.KegiatanTemporary} —
 *   turunan/penyangga pada modul billing yang sama;</li>
 *   <li>{@code ais.database.model.KegiatanKedosenan} dan
 *   {@code ais.database.model.KegiatanKemahasiswaan} — aktivitas dosen/mahasiswa;</li>
 *   <li>{@code ais.database.model.sekolah.KegiatanKesiswaan} dan
 *   {@code ais.database.model.sekolah.KegiatanSiswa} — kegiatan kesiswaan pada modul sekolah;</li>
 *   <li>{@code ais.database.model.lkp.KegiatanTugasJabatan} — kegiatan tugas jabatan pada modul LKP
 *   (kinerja pegawai).</li>
 * </ul>
 * <p>Kemiripan nama murni kebetulan penamaan domain berbahasa Indonesia. Jangan pernah menyambung,
 * memigrasi, atau menyamakan data {@code rab.kegiatan_satker} dengan tabel mana pun di atas.</p>
 *
 * <h2>PERINGATAN PENTING #2 — entitas tidur (dormant), belum terpakai</h2>
 * <p>Penelusuran menyeluruh atas basis kode menemukan bahwa entitas ini <b>tidak dipakai oleh satu
 * pun alur aplikasi</b>. Referensi yang ada hanyalah rangkaian sambungan infrastruktur standar:</p>
 * <ul>
 *   <li>pendaftaran mapping pada {@code hibernate.cfg.xml}
 *   ({@code <mapping class="ais.database.model.rab.KegiatanSatker"/>});</li>
 *   <li>kontrak DAO {@code ais.database.dao.rab.KegiatanSatkerDao} dan implementasinya
 *   {@code KegiatanSatkerDaoImpl} — keduanya kosong, seluruh CRUD diwarisi dari
 *   {@code GenericHibernateDao};</li>
 *   <li>method pabrik {@code DaoFactory#getKegiatanSatkerDao()} dan realisasinya di
 *   {@code HibernateDaoFactory}.</li>
 * </ul>
 * <p>Tidak ada kelas {@code Action} ZK, tidak ada {@code Helper}, tidak ada {@code TreeModel},
 * tidak ada berkas ZUL, tidak ada entri menu, dan tidak ada entitas lain yang memegang referensi
 * ke tipe ini. Artinya {@code getKegiatanSatkerDao()} tidak pernah dipanggil dari jalur pengguna
 * mana pun, sehingga tabel {@code rab.kegiatan_satker} praktis selalu kosong pada instalasi normal.
 * Inventaris Generic CRUD ({@code WEB-INF/generic-crud/manifests/general_value_object_inventory.csv})
 * mencatatnya sebagai kandidat berstatus {@code ELIGIBLE_METADATA_FIRST} namun tetap
 * <i>default disabled</i> sampai verifikasi Hibernate/menu/scope dilakukan — konsisten dengan
 * temuan di atas.</p>
 * <p>Konsekuensi praktis: jangan menjadikan entitas ini acuan bentuk data yang "sudah terbukti
 * jalan". Bila kelak dihidupkan, seluruh perilaku (validasi, penomoran kode, pembatasan tenant)
 * masih harus dirancang dari nol.</p>
 *
 * <h2>PERINGATAN PENTING #3 — tidak ada kolom tenant meski namanya "Satker"</h2>
 * <p>Meski dinamai {@code KegiatanSatker} dan dipetakan ke tabel {@code kegiatan_satker}, entitas
 * ini <b>tidak memiliki field maupun kolom {@code satuan_kerja}</b>. Bandingkan dengan tetangga
 * satu paket seperti {@link Indikator}, {@link Sasaran}, {@link Proyek}, {@link Tor}, dan
 * {@link RencanaDanRealisasiOutputKegiatan} yang semuanya membawa relasi
 * {@code @ManyToOne SatuanKerja} dan karenanya bisa disaring per tenant. Dengan bentuk sekarang,
 * seluruh baris {@code rab.kegiatan_satker} bersifat global: siapa pun yang kelak diberi layar CRUD
 * atas entitas ini akan melihat data seluruh satuan kerja. Ini adalah instansi pola berulang
 * "filter tenant lemah/hilang" yang sudah tercatat pada inisiatif dokumentasi ini. Bila entitas
 * dihidupkan, penambahan relasi {@code SatuanKerja} beserta penyaringan pada query pencarian adalah
 * prasyarat, bukan opsi.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}.</p>
 *
 * <h2>Catatan gaya kode</h2>
 * <p>Berkas asli memampatkan deklarasi field {@code oleh}/{@code olehId} beserta accessor-nya ke
 * dalam satu baris, dan menempelkan deklarasi field {@code tanggal_dirubah} di belakang method
 * {@link #onUpdate()}. Pemampatan tersebut merupakan artefak penyuntingan massal lintas repo, bukan
 * keputusan desain; pada berkas ini deklarasi dipisah baris agar setiap anggota dapat diberi
 * dokumentasi, tanpa mengubah semantik apa pun.</p>
 *
 * @see ais.database.dao.rab.KegiatanSatkerDao
 * @see GeneralValueObject
 * @see SatuanKerja
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "kegiatan_satker")



public class KegiatanSatker extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi. Jangan mengandalkan nilai
	 * ini sebagai identitas kelas.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada
	 * {@code rab.kegiatan_satker.id}. Bernilai {@code null} selama objek belum pernah disimpan.
	 */
	private Long id;

	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Diisi lewat
	 * {@link #setOleh(String)} oleh lapisan interceptor/penyimpanan, bukan oleh pengguna.
	 */
	private String oleh;

	/**
	 * Field audit bayangan: identitas (id pengguna) terakhir yang mengubah baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh lapisan interceptor/penyimpanan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Setter ini <b>menolak diam-diam</b> nilai
	 * {@code null} maupun string kosong/spasi: pada kasus tersebut nilai lama dipertahankan. Pola
	 * "setter menolak nilai kosong" ini dipakai seragam di seluruh entitas AIS agar jejak audit yang
	 * sudah terisi tidak terhapus oleh proses penyalinan objek atau pengikatan form yang mengirim
	 * nilai kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum operasi {@code UPDATE}. Method ini
	 * mendelegasikan pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, sehingga
	 * {@link #tanggal_dirubah} selalu mencerminkan saat penyimpanan terakhir tanpa perlu diisi oleh
	 * pemanggil. Tidak boleh dipanggil langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Field audit bayangan: stempel waktu perubahan terakhir. Diinisialisasi ke waktu sekarang
	 * (lewat {@code ais.ui.util.WaktuUtil#getDate()}, bukan {@code new Date()}, agar mengikuti zona
	 * waktu/penyesuaian waktu aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya relevan pada skenario impor/migrasi data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom
	 * {@code TIMESTAMP}.
	 *
	 * @return waktu penyimpanan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama/uraian kegiatan satuan kerja. Dipetakan ke kolom {@code nama} bertipe
	 * {@code varchar(255)} dan dinyatakan {@code NOT NULL} pada tingkat skema.
	 */
	private String nama;

	/**
	 * Keterangan bebas atas kegiatan. Tidak diberi anotasi {@code @Column} eksplisit sehingga
	 * memakai pemetaan bawaan Hibernate (kolom {@code keterangan}, panjang bawaan 255).
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai lapisan CRUD generik saat membuat baris baru.
	 */
	public KegiatanSatker() {
	}

	/**
	 * Konstruktor pintas yang langsung mengisi {@link #nama}. Disediakan oleh generator hbm2java
	 * untuk kolom {@code NOT NULL}; pada basis kode saat ini tidak ada pemanggil karena entitas
	 * masih berstatus tidur (lihat catatan pada dokumentasi kelas).
	 *
	 * @param nama nama/uraian kegiatan.
	 */
	public KegiatanSatker(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya diisi Hibernate setelah {@code INSERT}; pengisian manual
	 * hanya relevan pada skenario impor/migrasi.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kegiatan dengan spasi di ujung sudah dipangkas. Pemangkasan dilakukan pada
	 * getter (bukan setter), sehingga nilai yang tersimpan di memori bisa saja masih mengandung
	 * spasi sementara nilai yang ditulis Hibernate ke basis data sudah terpangkas — perilaku ini
	 * konsisten dengan seluruh entitas hasil hbm2java di repo.
	 *
	 * @return nama kegiatan yang sudah dipangkas, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kegiatan apa adanya, tanpa pemangkasan maupun validasi panjang. Kolom target
	 * dinyatakan {@code NOT NULL} dan dibatasi 255 karakter, sehingga nilai {@code null} atau lebih
	 * panjang dari 255 karakter baru akan gagal saat penyimpanan (bukan saat pemanggilan setter).
	 *
	 * @param nama nama/uraian kegiatan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas atas kegiatan.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas atas kegiatan.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
