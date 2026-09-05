package ais.database.model.sirs;

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

import ais.database.model.GeneralValueObject;

/**
 * Master <b>tarif khusus</b> modul SIRS: satu paket harga alternatif yang berlaku bagi kombinasi
 * {@link Dokter}, {@link Asuransi}, {@link Komunitas}, dan/atau {@link Pasien} tertentu — misalnya
 * daftar tarif bagi peserta suatu asuransi, karyawan suatu perusahaan, atau seorang pasien VIP.
 * Entitas ini hanya kepala paketnya; isinya adalah daftar objek yang ditarifkan khusus, tersimpan
 * pada {@link TarifKhususPunyaTindakan}, {@link TarifKhususPunyaAlatMedis}, dan
 * {@link TarifKhususPunyaItem}, yang masing-masing lalu memiliki baris tarif per kelas perawatan
 * sendiri.
 *
 * <h3>Tarif khusus adalah pemilih baris tarif, bukan koreksi di akhir</h3>
 * Dugaan yang lazim — dan keliru — adalah bahwa tarif khusus merupakan <i>override</i> yang
 * mengoreksi harga setelah tarif dasar dihitung. Yang sebenarnya terjadi adalah kebalikannya:
 * tarif khusus diselesaikan <b>paling awal</b> dalam urutan penetapan harga.
 * {@code ais.action.master.sirs.util.CommonTarif#getTarif} dipanggil lebih dahulu untuk menentukan
 * apakah konteks transaksi (dokter yang menangani, asuransi pasien, komunitas pasien, dan pasien
 * itu sendiri) cocok dengan suatu tarif khusus. Hasilnya menentukan <i>baris tarif per kelas mana
 * yang diambil</i> — bukan mengubah angka yang sudah dihitung. Baris tarif standar dan baris tarif
 * khusus bahkan berbagi tabel yang sama ({@link BiayaTindakanPerKelas},
 * {@link BiayaAlatMedisPerKelas}) dan dibedakan hanya oleh pola pengisian kolom yang saling
 * meniadakan. Karena itu <b>tarif khusus menggantikan tarif dasar, tidak pernah bertumpuk di
 * atasnya</b>, dan tidak ada keadaan "keduanya berlaku lalu salah satu menang".
 *
 * <h3>Cara pemenang ditentukan bila beberapa tarif khusus cocok</h3>
 * Pertanyaan "mana yang menang" berlaku di antara sesama tarif khusus, dan dijawab
 * {@code CommonTarif.getTarif} dengan <b>strategi generalisasi bertingkat</b> — tiga belas kueri
 * berurutan yang dijalankan sampai salah satunya membuahkan hasil:
 * <ol>
 * <li>Mula-mula dicari kecocokan paling spesifik: keempat dimensi
 * (dokter, asuransi, komunitas, pasien) harus cocok persis dengan konteks transaksi — atau
 * bernilai {@code NULL} bila konteksnya memang kosong.</li>
 * <li>Bila nihil, syarat dilonggarkan selangkah demi selangkah: satu dimensi dikosongkan
 * (dipaksa {@code isNull}) dalam berbagai kombinasi, mulai dari melepas {@code pasien}, lalu
 * {@code komunitas}, lalu {@code asuransi}, lalu {@code dokter}, kemudian melepas dua dimensi
 * sekaligus, lalu tiga.</li>
 * <li>Langkah terakhir adalah tarif khusus yang keempat dimensinya kosong — yaitu paket tarif
 * alternatif yang berlaku untuk siapa saja.</li>
 * </ol>
 * Dengan demikian aturan yang lebih spesifik selalu mengalahkan yang lebih umum, tanpa perlu kolom
 * prioritas eksplisit. Di setiap langkah hanya satu baris diambil, diurutkan {@code mulai} menurun
 * — yakni yang masa berlakunya paling baru dimenangkan.
 * <p>
 * <b>Perhatian pada seri.</b> Karena {@link #getMulai()} ditandai {@link TemporalType#DATE} (hanya
 * tanggal, tanpa jam), dua tarif khusus dengan tingkat kespesifikan sama yang mulai berlaku pada
 * <i>hari</i> yang sama akan seri pada pengurutan itu, dan pemenangnya ditentukan urutan baris yang
 * dikembalikan basis data — tidak deterministik. Tidak ada indeks unik maupun validasi yang
 * mencegah dua tarif khusus tumpang tindih pada kombinasi dimensi yang sama, sehingga keadaan ini
 * dapat terjadi dan menghasilkan harga berbeda antar pembacaan.
 * </p>
 *
 * <h3>Masa berlaku dan status aktif — pola yang benar, berbeda dari {@link Diskon}</h3>
 * Berbeda dari {@link Diskon#getSampai()} dan {@link PajakMedis#getSampai()} yang diinisialisasi
 * {@code new Date()} sehingga langsung kedaluwarsa, {@link #sampai} di kelas ini <b>tidak
 * diinisialisasi</b> — nilainya {@code null}, yang oleh kueri resolusi diperlakukan sebagai
 * "berlaku tanpa batas akhir". Kedua kolom tanggal juga ditandai {@link TemporalType#DATE} sehingga
 * tidak membawa komponen jam yang bisa membuat perbandingan meleset dalam hari yang sama. Status
 * aktif pun ditangani secara longgar dan konsisten dengan maksud getternya:
 * {@code CommonTarif.getTarif} memakai {@code or(isNull(aktif), eq(aktif, true))}, sejalan dengan
 * {@link #getAktif()} yang menganggap {@code null} sebagai aktif. Inilah pola yang benar, dan
 * kelas ini dapat dijadikan acuan saat memperbaiki penanganan serupa pada {@link Diskon} dan
 * {@link PajakMedis} yang justru memakai {@code eq(aktif, true)} yang ketat.
 *
 * <h3>Status aktif hanya ada di tingkat paket</h3>
 * Bendera {@link #getAktif()} adalah <b>satu-satunya</b> saklar aktif yang benar-benar dibaca saat
 * resolusi. Ketiga entitas anak juga memiliki bendera {@code aktif} masing-masing
 * ({@link TarifKhususPunyaTindakan#getAktif()} dan seterusnya), tetapi bendera-bendera itu tidak
 * pernah dibaca oleh kode mana pun — bendera tidur. Konsekuensi praktisnya: menonaktifkan satu
 * tindakan atau satu item dari sebuah paket tarif khusus <b>tidak dapat dilakukan</b>; yang bisa
 * dimatikan hanyalah keseluruhan paket lewat entitas ini.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Bendera aktif dua-arah</b> — dapat dinyalakan dan dimatikan.</li>
 * <li><b>Getter destruktif</b> — keempat relasi dimensi dan {@link #getAktif()} menulis balik ke
 * field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — paket tarif khusus bersifat global lintas unit.</li>
 * <li>Kode resolusi {@code CommonTarif.getTarif} masih memuat pernyataan {@code System.out.println}
 * penanda langkah yang tercetak pada setiap resolusi tarif; sisa penelusuran yang belum
 * dibersihkan.</li>
 * </ul>
 *
 * @see TarifKhususPunyaTindakan daftar tindakan yang ditarifkan khusus dalam paket ini
 * @see TarifKhususPunyaAlatMedis daftar alat medis yang ditarifkan khusus dalam paket ini
 * @see TarifKhususPunyaItem daftar item medis yang ditarifkan khusus dalam paket ini
 * @see ais.action.master.sirs.util.CommonTarif#getTarif mesin resolusi generalisasi bertingkat
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "tarif_khusus")
public class TarifKhusus extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.tarif_khusus}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah paket tarif khusus ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks paket tarif khusus untuk komponen ZK, memakai field {@link #nama} langsung.
	 *
	 * @return nama paket tarif khusus; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah paket tarif khusus ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir paket tarif khusus ini. Jangan dikacaukan dengan
	 * {@link #getMulai()}/{@link #getSampai()} yang merupakan masa berlaku domain.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama paket tarif khusus, wajib diisi; dipakai sebagai label tampil. */
	private String nama;

	/** Status berlaku paket; tanpa nilai awal, tetapi {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/** Awal masa berlaku paket (tanggal saja); diinisialisasi ke hari pembuatan objek. */
	private Date mulai = new Date();

	/** Akhir masa berlaku paket (tanggal saja); {@code null} berarti tanpa batas akhir. */
	private Date sampai;

	/** Dimensi pembatas: paket hanya berlaku bila ditangani dokter ini. */
	private Dokter dokter;

	/** Dimensi pembatas: paket hanya berlaku bagi peserta asuransi ini. */
	private Asuransi asuransi;

	/** Dimensi pembatas: paket hanya berlaku bagi anggota komunitas ini. */
	private Komunitas komunitas;

	/** Dimensi pembatas: paket hanya berlaku bagi pasien ini (tarif personal). */
	private Pasien pasien;

	/** Keterangan bebas atas paket tarif khusus. */
	private String keterangan;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public TarifKhusus() {
	}

	/**
	 * Mengembalikan kunci utama paket tarif khusus.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama paket tarif khusus.
	 *
	 * @return nama paket (kolom wajib, maksimal 255 karakter)
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Mengisi nama paket tarif khusus. Tidak ada penjaga tabrakan nama maupun indeks unik, sehingga
	 * dua paket bernama sama akan tampak identik di daftar pilihan.
	 *
	 * @param nama nama paket tarif khusus
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas paket tarif khusus.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas paket tarif khusus.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan awal masa berlaku paket, disimpan sebagai tanggal saja
	 * ({@link TemporalType#DATE}) tanpa komponen jam.
	 *
	 * <p>
	 * Selain menjadi batas bawah masa berlaku lewat syarat {@code le(mulai, hari ini)}, kolom ini
	 * berperan sebagai <b>penentu pemenang</b> di antara tarif khusus yang sama-sama cocok pada
	 * tingkat kespesifikan yang sama: {@code CommonTarif.getTarif} mengurutkan {@code mulai} menurun
	 * lalu mengambil satu baris teratas, sehingga paket yang mulai berlaku paling belakangan menang.
	 * Karena kolom ini hanya menyimpan tanggal, dua paket yang mulai berlaku pada hari yang sama
	 * seri — dan pemenangnya menjadi bergantung pada urutan baris yang dikembalikan basis data,
	 * yakni tidak deterministik. Hindari menerbitkan dua paket tarif khusus dengan cakupan dimensi
	 * yang sama pada hari yang sama.
	 * </p>
	 *
	 * @return awal masa berlaku (tanggal saja), atau {@code null} — baris {@code NULL} tidak akan
	 *         pernah terpilih karena syarat {@code le} tidak mentoleransinya
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Mengisi awal masa berlaku paket.
	 *
	 * @param mulai awal masa berlaku
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan akhir masa berlaku paket, disimpan sebagai tanggal saja
	 * ({@link TemporalType#DATE}).
	 *
	 * <p>
	 * Berbeda dari {@link Diskon#getSampai()} dan {@link PajakMedis#getSampai()} yang field-nya
	 * diinisialisasi {@code new Date()} sehingga aturan baru langsung kedaluwarsa, field di sini
	 * sengaja <b>tidak diinisialisasi</b>. Nilai {@code null} yang dihasilkannya diperlakukan kueri
	 * resolusi sebagai "berlaku tanpa batas akhir" lewat
	 * {@code or(isNull(sampai), ge(sampai, hari ini))} — perilaku yang wajar bagi paket tarif yang
	 * umumnya memang tidak diberi tanggal berakhir. Inilah pola yang benar untuk masa berlaku di
	 * modul ini.
	 * </p>
	 *
	 * @return akhir masa berlaku, atau {@code null} yang berarti tanpa batas akhir
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi akhir masa berlaku paket. Kosongkan ({@code null}) bila paket berlaku tanpa batas
	 * waktu.
	 *
	 * @param sampai akhir masa berlaku, atau {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan dimensi pembatas dokter. <b>Getter destruktif</b> ({@code check(...)}).
	 * Nilai {@code null} berarti paket tidak dibatasi dokter tertentu — dan pada strategi
	 * generalisasi bertingkat, paket semacam itu baru dipertimbangkan setelah paket yang dokternya
	 * cocok persis tidak ditemukan.
	 *
	 * @return dokter yang dicakup, atau {@code null} bila tanpa pembatas dokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokter", nullable = true)
	public Dokter getDokter() {
		dokter = check(dokter);
		return dokter;
	}

	/**
	 * Menetapkan dimensi pembatas dokter.
	 *
	 * @param dokter dokter yang dicakup, atau {@code null}
	 */
	public void setDokter(Dokter dokter) {
		this.dokter = dokter;
	}

	/**
	 * Mengembalikan dimensi pembatas asuransi. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return asuransi yang dicakup, atau {@code null} bila tanpa pembatas asuransi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	/**
	 * Menetapkan dimensi pembatas asuransi.
	 *
	 * @param asuransi asuransi yang dicakup, atau {@code null}
	 */
	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	/**
	 * Mengembalikan dimensi pembatas komunitas. <b>Getter destruktif</b> ({@code check(...)}).
	 * Perhatikan bahwa kolom ini menyimpan satu komunitas, sedangkan seorang pasien dapat
	 * tergabung dalam beberapa komunitas — karena itu kueri resolusi memakai
	 * {@code Restrictions.in} atas himpunan komunitas pasien, bukan kesamaan tunggal.
	 *
	 * @return komunitas yang dicakup, atau {@code null} bila tanpa pembatas komunitas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "komunitas", nullable = true)
	public Komunitas getKomunitas() {
		komunitas = check(komunitas);
		return komunitas;
	}

	/**
	 * Menetapkan dimensi pembatas komunitas.
	 *
	 * @param komunitas komunitas yang dicakup, atau {@code null}
	 */
	public void setKomunitas(Komunitas komunitas) {
		this.komunitas = komunitas;
	}

	/**
	 * Mengembalikan dimensi pembatas pasien. <b>Getter destruktif</b> ({@code check(...)}). Inilah
	 * dimensi paling spesifik: paket yang pasiennya cocok persis mengalahkan seluruh paket lain,
	 * dan merupakan syarat pertama yang dilepas ketika kecocokan penuh tidak ditemukan.
	 *
	 * @return pasien yang dicakup, atau {@code null} bila tanpa pembatas pasien
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	/**
	 * Menetapkan dimensi pembatas pasien.
	 *
	 * @param pasien pasien yang dicakup, atau {@code null}
	 */
	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	/**
	 * Mengembalikan status berlaku paket, menormalkan {@code null} menjadi {@code true}.
	 * <b>Getter destruktif</b>. Bendera ini dua-arah dan — berbeda dari
	 * {@link Diskon#getAktif()}/{@link PajakMedis#getAktif()} — maksud "belum ditentukan berarti
	 * aktif" di sini <b>dihormati</b> oleh kueri resolusinya, yang memakai
	 * {@code or(isNull(aktif), eq(aktif, true))}.
	 *
	 * <p>
	 * Ini juga satu-satunya saklar aktif yang benar-benar berpengaruh pada susunan tarif khusus:
	 * bendera {@code aktif} pada ketiga entitas anak tidak pernah dibaca kode mana pun, sehingga
	 * menonaktifkan satu tindakan/item/alat dari sebuah paket tidak dapat dilakukan — yang bisa
	 * dimatikan hanyalah keseluruhan paket lewat setter di sini.
	 * </p>
	 *
	 * @return {@code true} bila paket berlaku, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyalakan atau mematikan paket tarif khusus. Mematikan paket membuat seluruh transaksi
	 * berikutnya jatuh kembali ke tarif standar; harga transaksi yang sudah tercatat tidak berubah.
	 *
	 * @param aktif {@code true} bila paket berlaku
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
