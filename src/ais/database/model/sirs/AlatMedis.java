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
import ais.database.model.Ruang;

/**
 * Entitas katalog master alat medis pada schema {@code sirs} (tabel
 * {@code alat_medis}). MESKI namanya "AlatMedis", entitas ini TIDAK sebatas
 * mewakili alat kesehatan fisik seperti USG/ventilator — diverifikasi dari
 * kode, kelas ini juga dipakai untuk merepresentasikan RESOURCE YANG
 * DITAGIH BERDASARKAN WAKTU/PEMAKAIAN, termasuk sewa tempat tidur
 * ({@link #getTempatTidur()}), kamar ({@link #getKamar()}), dan ruangan
 * ({@link #getRuang()}) — mis. baris "AlatMedis" bisa berarti "Bed nomor
 * X di Kamar Y, Ruang Z" dengan tarif per jam/hari/pemakaian. Ini pola
 * "nama kelas menyesatkan" yang sama seperti sudah berulang kali ditemukan
 * di paket model AIS lain.
 *
 * <p>
 * Tarif dasarnya berbasis satuan waktu/pemakaian lewat konstanta
 * {@link #PER_JAM}, {@link #PER_HARI}, {@link #PER_KALI} (field
 * {@link #getPer()}). Terdapat DUA konsep "jenis" yang berbeda dan TIDAK
 * saling terkait langsung:
 * </p>
 * <ul>
 * <li>Field String {@link #getJenis()} dengan konstanta
 * {@link #JENIS_TEMPAT_TIDUR}/{@link #JENIS_UMUM} milik kelas ini
 * sendiri.</li>
 * <li>Relasi {@link #getJenisAlatMedis()} ke entitas terpisah
 * {@link JenisAlatMedis}, yang konstantanya sendiri
 * ({@link JenisAlatMedis#JENIS_TARIF_KAMAR_DAN_RUANGAN}/{@link JenisAlatMedis#JENIS_TARIF_BED})
 * merepresentasikan basis tarif kamar/ruangan vs tempat tidur — nama mirip
 * tapi berasal dari kelas dan kolom database yang berbeda
 * ({@code jenis} vs {@code jenis_alat_medis}).</li>
 * </ul>
 *
 * <p>
 * Sama seperti {@link ItemMedis}, katalog ini terpisah total dari
 * {@code inventory.Produk} (tidak ada import silang di kedua arah,
 * diverifikasi dari kode).
 * </p>
 *
 * <p>
 * PERHATIAN ARSITEKTUR: kelas ini memuat beberapa getter dengan EFEK
 * SAMPING TULIS-BALIK ke field instance — {@link #getNama()},
 * {@link #getKamar()}, {@link #getRuang()}, dan
 * {@link #getKeteranganLayanan()} semuanya menghitung ulang nilai field
 * setiap kali dipanggil dan menimpa field aslinya. Karena field-field ini
 * tidak ditandai {@code @Transient}, Hibernate (mode akses properti,
 * ditandai dari {@code @Id} di atas getter) akan membaca nilai HASIL
 * PERHITUNGAN ULANG tersebut saat dirty-checking/flush, sehingga nilai
 * yang tersimpan di database bisa berbeda dari yang terakhir di-set lewat
 * setter murni — lihat javadoc masing-masing getter untuk detail.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "alat_medis")
public class AlatMedis extends GeneralValueObject {

	/** Konstanta basis tarif: dihitung per jam pemakaian. */
	public static final String PER_JAM = "Per Jam";
	/** Konstanta basis tarif: dihitung per hari pemakaian. */
	public static final String PER_HARI = "Per Hari";
	/** Konstanta basis tarif: dihitung per satu kali pemakaian/pakai. */
	public static final String PER_KALI = "Per kali pakai";

	/**
	 * Konstanta nilai field {@link #getJenis()} yang menandakan baris ini
	 * merepresentasikan tempat tidur (bed). Field ini independen dari
	 * relasi {@link #getJenisAlatMedis()} — lihat javadoc kelas.
	 */
	public static final String JENIS_TEMPAT_TIDUR = "Tempat Tidur";
	/**
	 * Konstanta nilai field {@link #getJenis()} yang menandakan baris ini
	 * adalah alat medis umum (bukan tempat tidur/kamar/ruangan khusus).
	 * Ini juga nilai default {@link #getJenis()} bila belum pernah diisi.
	 */
	public static final String JENIS_UMUM = "Umum";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas alat medis ini untuk keperluan tampilan/log:
	 * nama (lihat efek samping di {@link #getNama()}) digabung dengan
	 * relasi {@link #getJenisAlatMedis()}.
	 *
	 * @return string {@code nama-jenisAlatMedis}.
	 */
	public String toString() {
		return nama + "-" + jenisAlatMedis;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private Boolean semuahargasama = true;
	private String per;

	private JenisAlatMedis jenisAlatMedis;

	private Boolean alatMedisLab = false;
	private Boolean alatMedisOperasi = false;
	private Boolean alatMedisRadiologi = false;
	private Boolean alatMedisVk = false;
	private Boolean alatMedisRenalUnit = false;
	private Boolean alatMedisGizi = false;
	private String keteranganLayanan;

	private Boolean aktif;

	private String jenis;

	private TempatTidur tempatTidur;
	private Kamar kamar;
	private Ruang ruang;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public AlatMedis() {
	}

	/**
	 * Primary key baris alat medis, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik alat medis ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID alat medis.
	 *
	 * @param id ID alat medis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama tampil alat medis ini.
	 *
	 * <p>
	 * GETTER DESTRUKTIF: bila baris ini terkait tempat tidur/kamar/ruang,
	 * method ini MENGHITUNG ULANG dan MENIMPA field {@link #nama} setiap
	 * kali dipanggil, mengabaikan nilai yang pernah diset lewat
	 * {@link #setNama(String)} — nama disusun dari rantai
	 * {@link #getTempatTidur()} &rarr; {@link #getKamar()} &rarr;
	 * {@link #getRuang()} (dalam urutan prioritas: tempat tidur paling
	 * spesifik, lalu kamar, lalu ruang) digabung {@link #getPer()}.
	 * Hanya bila KETIGA relasi tersebut {@code null} nilai {@link #nama}
	 * yang tersimpan apa adanya dikembalikan. Efek samping ini konsisten
	 * dengan pola getter destruktif berulang yang ditemukan di paket
	 * model AIS lain (KEHARUSAN TEKNIS agar nama selalu mengikuti
	 * perubahan nama tempat tidur/kamar/ruang terkait, bukan disalin
	 * statis saat pembuatan baris).
	 * </p>
	 *
	 * @return nama tampil alat medis, disusun otomatis dari relasi
	 *         tempat tidur/kamar/ruang bila ada.
	 */
	@Column(name = "nama", nullable = false, unique = true, length = 255)
	public String getNama() {
		if (getTempatTidur() != null) {
			nama = "Bed : " + getTempatTidur().getNama() + ", Kamar: " + getKamar().getNama() + ", Ruang: "
					+ getRuang().getNama() + " " + getPer();
		} else if (getKamar() != null) {
			nama = "Kamar: " + getKamar().getNama() + ", Ruang: " + getRuang().getNama() + " " + getPer();
		} else if (getRuang() != null) {
			nama = "Ruang: " + getRuang().getNama() + " " + getPer();
		}
		return this.nama;
	}

	/**
	 * Menetapkan nama alat medis secara eksplisit. Perlu dicatat bahwa
	 * nilai ini akan DITIMPA lagi oleh {@link #getNama()} pada panggilan
	 * berikutnya jika baris ini punya relasi tempat tidur/kamar/ruang —
	 * lihat javadoc getter.
	 *
	 * @param nama nama alat medis.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas alat medis ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas alat medis ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kode alat medis.
	 *
	 * @param kode kode alat medis.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil kode alat medis. Berbeda dari {@link ItemMedis#getKode()},
	 * kolom {@code kode} di sini bersifat {@code nullable = true} (tidak
	 * wajib diisi) dan TIDAK ditandai {@code unique} — jadi kode alat
	 * medis boleh kosong maupun duplikat, tidak dijamin sebagai pengenal
	 * bisnis unik.
	 *
	 * @return kode alat medis, atau {@code null} jika belum diisi.
	 */
	@Column(name = "kode", nullable = true)
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan apakah tarif alat medis ini sama untuk semua kelas
	 * perawatan.
	 *
	 * @param semuahargasama {@code true} jika tarif seragam lintas kelas
	 *                       perawatan.
	 */
	public void setSemuahargasama(Boolean semuahargasama) {
		this.semuahargasama = semuahargasama;
	}

	/**
	 * Mengambil flag "semua harga sama" alat medis ini — pola flag yang
	 * sama dipakai juga di {@link ItemMedis#getSemuahargasama()} dan
	 * entitas tarif lain di paket {@code sirs}.
	 *
	 * @return {@code true} jika tarif seragam lintas kelas perawatan.
	 */
	public Boolean getSemuahargasama() {
		return semuahargasama;
	}

	/**
	 * Mengambil flag apakah alat medis ini dipakai layanan Laboratorium.
	 * Flag SATU-ARAH: {@code null} dibaca dan ditulis-balik sebagai
	 * {@code false} lewat lazy-init di getter (bukan sekadar dikembalikan
	 * sebagai hasil hitung sementara).
	 *
	 * @return {@code true} jika dipakai layanan Lab; default
	 *         {@code false} bila belum pernah diset.
	 */
	public Boolean getAlatMedisLab() {
		if (alatMedisLab == null) {
			alatMedisLab = false;
		}
		return alatMedisLab;
	}

	/**
	 * Menetapkan flag pemakaian alat medis ini untuk layanan Laboratorium.
	 *
	 * @param alatMedisLab {@code true} jika dipakai layanan Lab.
	 */
	public void setAlatMedisLab(Boolean alatMedisLab) {
		this.alatMedisLab = alatMedisLab;
	}

	/**
	 * Mengambil flag apakah alat medis ini dipakai layanan Operasi. Sama
	 * seperti {@link #getAlatMedisLab()}, {@code null} otomatis
	 * ditulis-balik jadi {@code false}.
	 *
	 * @return {@code true} jika dipakai layanan Operasi; default
	 *         {@code false} bila belum pernah diset.
	 */
	public Boolean getAlatMedisOperasi() {
		if (alatMedisOperasi == null) {
			alatMedisOperasi = false;
		}
		return alatMedisOperasi;
	}

	/**
	 * Menetapkan flag pemakaian alat medis ini untuk layanan Operasi.
	 *
	 * @param alatMedisOperasi {@code true} jika dipakai layanan Operasi.
	 */
	public void setAlatMedisOperasi(Boolean alatMedisOperasi) {
		this.alatMedisOperasi = alatMedisOperasi;
	}

	/**
	 * Mengambil flag apakah alat medis ini dipakai layanan Radiologi.
	 * Sama seperti {@link #getAlatMedisLab()}, {@code null} otomatis
	 * ditulis-balik jadi {@code false}.
	 *
	 * @return {@code true} jika dipakai layanan Radiologi; default
	 *         {@code false} bila belum pernah diset.
	 */
	public Boolean getAlatMedisRadiologi() {
		if (alatMedisRadiologi == null) {
			alatMedisRadiologi = false;
		}
		return alatMedisRadiologi;
	}

	/**
	 * Menetapkan flag pemakaian alat medis ini untuk layanan Radiologi.
	 *
	 * @param alatMedisRadiologi {@code true} jika dipakai layanan
	 *                           Radiologi.
	 */
	public void setAlatMedisRadiologi(Boolean alatMedisRadiologi) {
		this.alatMedisRadiologi = alatMedisRadiologi;
	}

	/**
	 * Mengambil flag apakah alat medis ini dipakai layanan VK (kamar
	 * bersalin). Sama seperti {@link #getAlatMedisLab()}, {@code null}
	 * otomatis ditulis-balik jadi {@code false}.
	 *
	 * @return {@code true} jika dipakai layanan VK; default {@code false}
	 *         bila belum pernah diset.
	 */
	public Boolean getAlatMedisVk() {
		if (alatMedisVk == null) {
			alatMedisVk = false;
		}
		return alatMedisVk;
	}

	/**
	 * Menetapkan flag pemakaian alat medis ini untuk layanan VK.
	 *
	 * @param alatMedisVk {@code true} jika dipakai layanan VK.
	 */
	public void setAlatMedisVk(Boolean alatMedisVk) {
		this.alatMedisVk = alatMedisVk;
	}

	/**
	 * Mengambil flag apakah alat medis ini dipakai layanan Renal Unit
	 * (cuci darah/dialisis). Sama seperti {@link #getAlatMedisLab()},
	 * {@code null} otomatis ditulis-balik jadi {@code false}.
	 *
	 * @return {@code true} jika dipakai layanan Renal Unit; default
	 *         {@code false} bila belum pernah diset.
	 */
	public Boolean getAlatMedisRenalUnit() {
		if (alatMedisRenalUnit == null) {
			alatMedisRenalUnit = false;
		}
		return alatMedisRenalUnit;
	}

	/**
	 * Menetapkan flag pemakaian alat medis ini untuk layanan Renal Unit.
	 *
	 * @param alatMedisRenalUnit {@code true} jika dipakai layanan Renal
	 *                           Unit.
	 */
	public void setAlatMedisRenalUnit(Boolean alatMedisRenalUnit) {
		this.alatMedisRenalUnit = alatMedisRenalUnit;
	}

	/**
	 * Mengambil flag apakah alat medis ini dipakai layanan Gizi. Sama
	 * seperti {@link #getAlatMedisLab()}, {@code null} otomatis
	 * ditulis-balik jadi {@code false}.
	 *
	 * @return {@code true} jika dipakai layanan Gizi; default
	 *         {@code false} bila belum pernah diset.
	 */
	public Boolean getAlatMedisGizi() {
		if (alatMedisGizi == null) {
			alatMedisGizi = false;
		}
		return alatMedisGizi;
	}

	/**
	 * Menetapkan flag pemakaian alat medis ini untuk layanan Gizi.
	 *
	 * @param alatMedisGizi {@code true} jika dipakai layanan Gizi.
	 */
	public void setAlatMedisGizi(Boolean alatMedisGizi) {
		this.alatMedisGizi = alatMedisGizi;
	}

	/**
	 * Membangun ringkasan layanan yang memakai alat medis ini, dalam
	 * satu baris teks berformat {@code "Lab: x; Operasi: x; Radiologi: x;
	 * Gizi: x; Renal Unit: x; Vk: x; "}.
	 *
	 * <p>
	 * GETTER DESTRUKTIF/FIELD SHADOW TERHITUNG-ULANG: setiap panggilan
	 * MEMBANGUN ULANG string dari keenam flag layanan
	 * ({@link #getAlatMedisLab()}, {@link #getAlatMedisOperasi()}, dst.)
	 * dan MENIMPA field {@link #keteranganLayanan}, mengabaikan nilai
	 * apapun yang sebelumnya diset lewat
	 * {@link #setKeteranganLayanan(String)}. Karena field ini tidak
	 * ditandai {@code @Transient} maupun dipetakan lewat
	 * {@code @Column} eksplisit, Hibernate (mode akses properti) tetap
	 * akan membaca nilai hasil bentukan ulang ini via getter saat
	 * dirty-checking/flush — sehingga isi kolom {@code keteranganLayanan}
	 * di database mengikuti hasil komputasi terakhir, bukan input manual
	 * lewat setter.
	 * </p>
	 *
	 * @return ringkasan enam flag layanan dalam satu baris teks.
	 */
	public String getKeteranganLayanan() {
		keteranganLayanan = "";
		keteranganLayanan += "Lab: " + getAlatMedisLab() + "; ";
		keteranganLayanan += "Operasi: " + getAlatMedisOperasi() + "; ";
		keteranganLayanan += "Radiologi: " + getAlatMedisRadiologi() + "; ";
		keteranganLayanan += "Gizi: " + getAlatMedisGizi() + "; ";
		keteranganLayanan += "Renal Unit: " + getAlatMedisRenalUnit() + "; ";
		keteranganLayanan += "Vk: " + getAlatMedisVk() + "; ";
		return keteranganLayanan;
	}

	/**
	 * Menetapkan ringkasan layanan alat medis ini secara eksplisit.
	 * Nilai ini akan DITIMPA lagi oleh {@link #getKeteranganLayanan()}
	 * pada panggilan berikutnya — lihat javadoc getter.
	 *
	 * @param keteranganLayanan teks ringkasan layanan.
	 */
	public void setKeteranganLayanan(String keteranganLayanan) {
		this.keteranganLayanan = keteranganLayanan;
	}

	/**
	 * Mengambil basis tarif alat medis ini (per jam/hari/pemakaian).
	 * {@code null} otomatis dibaca sebagai {@link #PER_HARI} lewat
	 * lazy-init yang ditulis-balik ke field {@link #per}.
	 *
	 * @return basis tarif; salah satu dari {@link #PER_JAM},
	 *         {@link #PER_HARI}, {@link #PER_KALI}; default
	 *         {@link #PER_HARI} bila belum pernah diset.
	 */
	@Column(name = "per", nullable = false, length = 20)
	public String getPer() {
		if (per == null) {
			per = PER_HARI;
		}
		return per;
	}

	/**
	 * Menetapkan basis tarif alat medis ini.
	 *
	 * @param per salah satu dari {@link #PER_JAM}, {@link #PER_HARI},
	 *            {@link #PER_KALI}.
	 */
	public void setPer(String per) {
		this.per = per;
	}

	/**
	 * Mengambil relasi jenis alat medis ini (klasifikasi basis tarif
	 * kamar/ruangan vs tempat tidur, lihat {@link JenisAlatMedis}) —
	 * relasi OPSIONAL yang TIDAK sama dengan field String
	 * {@link #getJenis()} milik kelas ini sendiri; lihat javadoc kelas
	 * untuk perbedaan kedua konsep "jenis" ini.
	 *
	 * @return jenis alat medis, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_alat_medis", nullable = true)
	public JenisAlatMedis getJenisAlatMedis() {
		jenisAlatMedis = check(jenisAlatMedis);
		return jenisAlatMedis;
	}

	/**
	 * Menetapkan relasi jenis alat medis ini.
	 *
	 * @param jenisAlatMedis jenis alat medis, lihat {@link JenisAlatMedis}.
	 */
	public void setJenisAlatMedis(JenisAlatMedis jenisAlatMedis) {
		this.jenisAlatMedis = jenisAlatMedis;
	}

	/**
	 * Mengambil flag aktif/tidak-aktif alat medis ini. {@code null}
	 * otomatis dibaca sebagai {@code true} (aktif) lewat lazy-init yang
	 * ditulis-balik ke field {@link #aktif} — item lama yang belum
	 * pernah eksplisit diset akan otomatis dianggap aktif begitu getter
	 * ini dipanggil sekali.
	 *
	 * @return {@code true} jika alat medis aktif; default {@code true}
	 *         bila belum pernah diset.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan flag aktif/tidak-aktif alat medis ini.
	 *
	 * @param aktif {@code true} jika alat medis aktif dipakai/ditampilkan.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil jenis alat medis ini dalam bentuk field String milik
	 * kelas ini sendiri (BUKAN relasi {@link #getJenisAlatMedis()} —
	 * lihat javadoc kelas untuk perbedaannya). {@code null} otomatis
	 * dibaca dan ditulis-balik sebagai {@link #JENIS_UMUM}.
	 *
	 * @return salah satu dari {@link #JENIS_TEMPAT_TIDUR} atau
	 *         {@link #JENIS_UMUM}; default {@link #JENIS_UMUM} bila
	 *         belum pernah diset.
	 */
	public String getJenis() {
		if (jenis == null) {
			jenis = JENIS_UMUM;
		}
		return jenis;
	}

	/**
	 * Menetapkan jenis alat medis ini (field String, lihat
	 * {@link #getJenis()}).
	 *
	 * @param jenis salah satu dari {@link #JENIS_TEMPAT_TIDUR} atau
	 *              {@link #JENIS_UMUM}.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengambil relasi tempat tidur (bed) yang diwakili baris alat medis
	 * ini, bila baris ini merepresentasikan sewa tempat tidur — lihat
	 * javadoc kelas untuk konteks pola "AlatMedis sebagai resource
	 * bertarif" ini.
	 *
	 * @return tempat tidur terkait, atau {@code null} jika baris ini
	 *         bukan representasi tempat tidur.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tempat_tidur", nullable = true)
	public TempatTidur getTempatTidur() {
		tempatTidur = check(tempatTidur);
		return tempatTidur;
	}

	/**
	 * Menetapkan relasi tempat tidur untuk baris alat medis ini.
	 *
	 * @param tempatTidur tempat tidur terkait.
	 */
	public void setTempatTidur(TempatTidur tempatTidur) {
		this.tempatTidur = tempatTidur;
	}

	/**
	 * Mengambil relasi kamar yang diwakili/terkait baris alat medis ini.
	 *
	 * <p>
	 * GETTER DESTRUKTIF: jika baris ini punya {@link #getTempatTidur()}
	 * dengan kamar sendiri, kamar tempat tidur tersebut DIPAKSAKAN
	 * menimpa field {@link #kamar} (mengabaikan nilai yang mungkin sudah
	 * diset lewat {@link #setKamar(Kamar)}), agar kamar selalu konsisten
	 * mengikuti tempat tidurnya. Hanya bila tidak ada tempat tidur
	 * terkait, nilai {@link #kamar} yang tersimpan (lewat
	 * {@link #check(Object)}) yang dikembalikan.
	 * </p>
	 *
	 * @return kamar terkait; diturunkan dari tempat tidur bila ada,
	 *         atau nilai kamar yang diset langsung.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = true)
	public Kamar getKamar() {
		if (getTempatTidur() != null && getTempatTidur().getKamar() != null) {
			kamar = getTempatTidur().getKamar();
		} else {
			kamar = check(kamar);
		}
		return kamar;
	}

	/**
	 * Menetapkan relasi kamar untuk baris alat medis ini secara eksplisit.
	 * Nilai ini bisa ditimpa oleh {@link #getKamar()} bila baris ini
	 * punya tempat tidur dengan kamarnya sendiri — lihat javadoc getter.
	 *
	 * @param kamar kamar terkait.
	 */
	public void setKamar(Kamar kamar) {
		this.kamar = kamar;
	}

	/**
	 * Mengambil relasi ruang yang diwakili/terkait baris alat medis ini.
	 *
	 * <p>
	 * GETTER DESTRUKTIF, pola sama seperti {@link #getKamar()}: jika
	 * {@link #getKamar()} (hasil resolusi di atas) punya ruang sendiri,
	 * ruang tersebut DIPAKSAKAN menimpa field {@link #ruang}. Rantai
	 * turunan lengkapnya: tempat tidur &rarr; kamar &rarr; ruang, di mana
	 * level yang lebih spesifik selalu memenangkan level di atasnya.
	 * </p>
	 *
	 * @return ruang terkait; diturunkan dari kamar (yang mungkin sendiri
	 *         diturunkan dari tempat tidur) bila ada, atau nilai ruang
	 *         yang diset langsung.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		if (getKamar() != null && getKamar().getRuang() != null) {
			ruang = getKamar().getRuang();
		} else {
			ruang = check(ruang);
		}
		return ruang;
	}

	/**
	 * Menetapkan relasi ruang untuk baris alat medis ini secara eksplisit.
	 * Nilai ini bisa ditimpa oleh {@link #getRuang()} bila baris ini
	 * (via kamar/tempat tidur) punya ruang turunan — lihat javadoc
	 * getter.
	 *
	 * @param ruang ruang terkait.
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

}
