package ais.database.model.spi;

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * <h2>ProfilRisikoSPI &mdash; Penilaian Risiko Satu Unit Kerja pada Satu Tahun (Audit Universe)</h2>
 *
 * <p>
 * Kelas ini adalah jantung dari perencanaan audit BERBASIS RISIKO (<i>risk-based audit
 * planning</i>) yang menjadi ciri khas praktik audit internal modern (Standar Audit AAIPI,
 * IIA's International Professional Practices Framework/IPPF, maupun SPIP/PP 60 Tahun 2008).
 * Alih-alih mengaudit seluruh unit kerja secara merata dan sama rata setiap tahun (yang boros
 * sumber daya SPI dan tidak fokus pada area yang benar-benar berisiko), praktik terbaik
 * mengharuskan SPI menilai lebih dulu SEBERAPA BERISIKO tiap unit kerja, lalu memakai hasil
 * penilaian itu untuk memilih unit mana yang paling prioritas diaudit pada tahun berjalan
 * (dituangkan ke {@link RencanaAuditTahunanSPI}, dokumen yang di dunia audit disebut PKPT/Program
 * Kerja Pengawasan Tahunan). Satu baris di tabel ini merepresentasikan SATU unit kerja pada SATU
 * tahun penilaian &mdash; sehingga daftar keseluruhan baris pada satu tahun disebut "audit
 * universe": peta lengkap seluruh unit yang berpotensi diaudit beserta tingkat risikonya
 * masing-masing.
 * </p>
 *
 * <h3>Mengapa memakai {@link SatuanKerja}, bukan tabel unit baru</h3>
 * <p>
 * Kelas ini SENGAJA tidak membuat tabel "unit organisasi" baru khusus SPI, melainkan memakai
 * {@link SatuanKerja} (paket {@code ais.database.model.rab}) yang SUDAH menjadi representasi unit
 * kerja generik lintas-lembaga di aplikasi ini &mdash; dipakai luas oleh modul RAB/Anggaran dan
 * Pengadaan untuk merepresentasikan struktur organisasi berjenjang (fakultas, program studi, unit
 * kerja, ATAU satuan pendidikan/sekolah sekaligus, lewat relasi {@code parent} yang self-referencing
 * dan tautan ke {@code Yayasan}/{@code Pendaftar}). Karena {@link SatuanKerja} memang dirancang
 * untuk merentang di seluruh jenis lembaga (perguruan tinggi MAUPUN sekolah), memakainya di sini
 * secara otomatis memenuhi kebutuhan modul SPI untuk terintegrasi dengan eCampus DAN eSchool
 * sekaligus, TANPA perlu membuat/merawat pemetaan unit organisasi duplikat yang berisiko
 * berbeda/usang dari data organisasi resmi yang sudah dipakai modul keuangan.
 * </p>
 *
 * <h3>Lima komponen skor risiko (1&ndash;5 tiap komponen)</h3>
 * <p>
 * Mengikuti praktik umum penyusunan profil risiko audit, total risiko satu unit dihitung dari
 * beberapa faktor independen yang masing-masing dinilai 1 (paling rendah) sampai 5 (paling
 * tinggi), lalu dijumlahkan (lihat {@link #getTotalSkorRisiko()}):
 * </p>
 * <ul>
 *   <li>{@link #getSkorMaterialitas()} &mdash; seberapa besar nilai keuangan/anggaran yang
 *       dikelola unit ini; unit dengan anggaran besar berarti potensi kerugian bila terjadi
 *       masalah juga besar.</li>
 *   <li>{@link #getSkorDampakOperasional()} &mdash; seberapa besar dampak ke operasional lembaga
 *       secara keseluruhan bila unit ini bermasalah (mis. unit akademik inti vs unit penunjang).</li>
 *   <li>{@link #getSkorKualitasPengendalian()} &mdash; SEMAKIN LEMAH pengendalian internal unit
 *       ini (SDM kurang, SOP tidak berjalan, pemisahan tugas lemah), SEMAKIN TINGGI skornya
 *       &mdash; komponen ini merefleksikan elemen "kegiatan pengendalian" pada kerangka SPIP.</li>
 *   <li>{@link #getSkorTemuanSebelumnya()} &mdash; seberapa banyak/berat temuan audit pada
 *       pemeriksaan-pemeriksaan sebelumnya di unit ini; riwayat temuan berulang mengindikasikan
 *       risiko yang belum benar-benar tertangani.</li>
 *   <li>{@link #getSkorLamaTidakDiaudit()} &mdash; semakin lama sejak unit ini terakhir diaudit,
 *       semakin tinggi skornya, karena semakin besar kemungkinan kondisi telah berubah tanpa
 *       terpantau.</li>
 * </ul>
 * <p>
 * Total dan zona risiko ({@link #getTotalSkorRisiko()}, {@link #getZonaRisiko()}) SENGAJA TIDAK
 * disimpan sebagai kolom database tersendiri, melainkan selalu dihitung ulang secara langsung
 * (<i>live</i>) dari kelima komponen setiap kali dibaca. Ini mencegah kelas masalah "nilai rollup
 * basi" yang pernah terjadi di modul lain pada aplikasi ini (mis. HPP Kantin yang sempat memakai
 * harga beli produk jadi alih-alih hasil rollup resep) &mdash; dengan skor komponen sebagai
 * satu-satunya sumber kebenaran, total/zona TIDAK PERNAH bisa berbeda dari yang seharusnya hanya
 * karena lupa disinkronkan ulang setelah komponen diedit.
 * </p>
 *
 * <h3>Satu baris per unit per tahun</h3>
 * <p>
 * Field {@link #getTahun()} membuat penilaian risiko bersifat PERIODIK (biasanya disegarkan tiap
 * tahun sebelum penyusunan PKPT tahun berikutnya), bukan status permanen &mdash; risiko satu unit
 * bisa naik/turun dari tahun ke tahun seiring perubahan kondisi (pergantian pimpinan unit,
 * perubahan besaran anggaran, dst.), dan riwayat penilaian tahun-tahun sebelumnya tetap
 * tersimpan sebagai jejak historis untuk analisis tren.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "profil_risiko_spi")
public class ProfilRisikoSPI extends GeneralValueObject {

	/** Ambang batas total skor (5&ndash;25) untuk zona risiko "Tinggi". */
	public static final int AMBANG_ZONA_TINGGI = 20;
	/** Ambang batas total skor (5&ndash;25) untuk zona risiko "Sedang". */
	public static final int AMBANG_ZONA_SEDANG = 12;

	public static final String ZONA_TINGGI = "Tinggi";
	public static final String ZONA_SEDANG = "Sedang";
	public static final String ZONA_RENDAH = "Rendah";

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field SHADOW dari riwayat Envers
	 * ({@code @Audited} pada kelas ini) &mdash; KEHARUSAN TEKNIS untuk menampilkan "terakhir diubah
	 * oleh siapa" secara murah di layar daftar tanpa query terpisah ke tabel riwayat revisi.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan agar
	 * tidak menimpa jejak yang sudah tercatat.
	 *
	 * @param olehId ID pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan.
	 *
	 * @param oleh nama pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE, mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #getTanggal_dirubah()} secara otomatis, tanpa kode aplikasi perlu mengelolanya manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi manual waktu terakhir baris ini diubah; dalam praktiknya disegarkan otomatis lewat
	 * {@link #onUpdate()} pada tiap UPDATE.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil waktu terakhir baris ini diubah.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris profil risiko ini untuk log/debug, format
	 * {@code "<nama unit> - <tahun> (<zona risiko>)"}, mis. "Fakultas Teknik - 2026 (Tinggi)".
	 *
	 * @return string gabungan nama unit kerja, tahun penilaian, dan zona risiko hasil hitung
	 *         {@link #getZonaRisiko()}; "-" bila unit kerja atau namanya belum diisi.
	 */
	public String toString() {
		return (satuanKerja == null || satuanKerja.getNama() == null ? "-" : satuanKerja.getNama())
				+ " - " + tahun + " (" + getZonaRisiko() + ")";
	}

	private SatuanKerja satuanKerja;
	private Integer tahun;
	private Integer skorMaterialitas;
	private Integer skorDampakOperasional;
	private Integer skorKualitasPengendalian;
	private Integer skorTemuanSebelumnya;
	private Integer skorLamaTidakDiaudit;
	private String catatan;
	private Boolean aktif;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public ProfilRisikoSPI() {
	}

	/**
	 * ID primer baris ini, di-generate otomatis oleh database (strategi {@code IDENTITY}).
	 *
	 * @return ID unik baris ini, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris ini secara manual, terutama saat membangun objek referensi ringan untuk
	 * relasi {@code JoinColumn} tanpa memuat seluruh baris dari database.
	 *
	 * @param id ID baris yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Unit kerja yang dinilai risikonya pada baris ini &mdash; lihat javadoc kelas bagian
	 * "Mengapa memakai SatuanKerja" untuk alasan lengkap memakai entity unit organisasi generik
	 * ini alih-alih tabel unit baru khusus SPI. Relasi wajib ({@code nullable = false}).
	 *
	 * @return unit kerja yang dinilai pada baris profil risiko ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengaitkan baris penilaian risiko ini ke satu unit kerja.
	 *
	 * @param satuanKerja unit kerja baru yang dinilai.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Tahun penilaian risiko ini dilakukan &mdash; lihat javadoc kelas bagian "Satu baris per unit
	 * per tahun": penilaian bersifat periodik, sehingga satu unit kerja bisa punya banyak baris
	 * profil risiko untuk tahun-tahun berbeda sebagai jejak historis tren risikonya.
	 *
	 * @return tahun penilaian risiko.
	 */
	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Mengisi tahun penilaian risiko ini.
	 *
	 * @param tahun tahun baru.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Skor komponen "materialitas" (1&ndash;5) &mdash; seberapa besar nilai keuangan/anggaran yang
	 * dikelola unit ini, lihat javadoc kelas bagian "Lima komponen skor risiko". Default 1
	 * (terendah) bila belum diisi, KONSISTEN dengan keempat skor komponen lain di kelas ini agar
	 * unit yang belum dinilai sama sekali tidak keliru tampil di zona risiko tinggi.
	 *
	 * @return skor materialitas (1&ndash;5); 1 bila nilai tersimpan {@code null}.
	 */
	@Column(name = "skor_materialitas", nullable = false)
	public Integer getSkorMaterialitas() {
		return skorMaterialitas == null ? 1 : skorMaterialitas;
	}

	/**
	 * Mengisi skor komponen materialitas (idealnya 1&ndash;5, tidak divalidasi/dibatasi di level
	 * entity ini &mdash; validasi rentang sepenuhnya tanggung jawab layar input).
	 *
	 * @param skorMaterialitas skor materialitas baru.
	 */
	public void setSkorMaterialitas(Integer skorMaterialitas) {
		this.skorMaterialitas = skorMaterialitas;
	}

	/**
	 * Skor komponen "dampak operasional" (1&ndash;5) &mdash; seberapa besar dampak ke operasional
	 * lembaga secara keseluruhan bila unit ini bermasalah, lihat javadoc kelas. Default 1 bila
	 * belum diisi.
	 *
	 * @return skor dampak operasional (1&ndash;5); 1 bila nilai tersimpan {@code null}.
	 */
	@Column(name = "skor_dampak_operasional", nullable = false)
	public Integer getSkorDampakOperasional() {
		return skorDampakOperasional == null ? 1 : skorDampakOperasional;
	}

	/**
	 * Mengisi skor komponen dampak operasional (idealnya 1&ndash;5).
	 *
	 * @param skorDampakOperasional skor dampak operasional baru.
	 */
	public void setSkorDampakOperasional(Integer skorDampakOperasional) {
		this.skorDampakOperasional = skorDampakOperasional;
	}

	/**
	 * Skor komponen "kualitas pengendalian" (1&ndash;5) &mdash; PENTING: arah skala ini TERBALIK
	 * dari intuisi biasa, lihat javadoc kelas bagian "Lima komponen skor risiko": SEMAKIN LEMAH
	 * pengendalian internal unit ini (SDM kurang, SOP tidak berjalan, pemisahan tugas lemah),
	 * SEMAKIN TINGGI skornya (semakin berisiko) &mdash; bukan semakin tinggi kualitas pengendalian.
	 * Default 1 bila belum diisi.
	 *
	 * @return skor kualitas pengendalian (1&ndash;5, makin tinggi makin LEMAH pengendaliannya);
	 *         1 bila nilai tersimpan {@code null}.
	 */
	@Column(name = "skor_kualitas_pengendalian", nullable = false)
	public Integer getSkorKualitasPengendalian() {
		return skorKualitasPengendalian == null ? 1 : skorKualitasPengendalian;
	}

	/**
	 * Mengisi skor komponen kualitas pengendalian (idealnya 1&ndash;5, ingat arah skalanya
	 * terbalik &mdash; lihat {@link #getSkorKualitasPengendalian()}).
	 *
	 * @param skorKualitasPengendalian skor kualitas pengendalian baru.
	 */
	public void setSkorKualitasPengendalian(Integer skorKualitasPengendalian) {
		this.skorKualitasPengendalian = skorKualitasPengendalian;
	}

	/**
	 * Skor komponen "temuan sebelumnya" (1&ndash;5) &mdash; seberapa banyak/berat temuan audit
	 * pada pemeriksaan-pemeriksaan sebelumnya di unit ini, lihat javadoc kelas. CATATAN: skor ini
	 * TIDAK dihitung otomatis dari riwayat {@link TemuanAuditSPI} yang benar-benar tersimpan
	 * &mdash; nilainya diisi manual oleh staf SPI berdasarkan penilaian kualitatif terhadap
	 * riwayat temuan, bukan agregasi query otomatis. Default 1 bila belum diisi.
	 *
	 * @return skor temuan sebelumnya (1&ndash;5); 1 bila nilai tersimpan {@code null}.
	 */
	@Column(name = "skor_temuan_sebelumnya", nullable = false)
	public Integer getSkorTemuanSebelumnya() {
		return skorTemuanSebelumnya == null ? 1 : skorTemuanSebelumnya;
	}

	/**
	 * Mengisi skor komponen temuan sebelumnya (idealnya 1&ndash;5).
	 *
	 * @param skorTemuanSebelumnya skor temuan sebelumnya baru.
	 */
	public void setSkorTemuanSebelumnya(Integer skorTemuanSebelumnya) {
		this.skorTemuanSebelumnya = skorTemuanSebelumnya;
	}

	/**
	 * Skor komponen "lama tidak diaudit" (1&ndash;5) &mdash; semakin lama sejak unit ini terakhir
	 * diaudit, semakin tinggi skornya, lihat javadoc kelas. Sama seperti
	 * {@link #getSkorTemuanSebelumnya()}, skor ini diisi manual, TIDAK dihitung otomatis dari
	 * selisih tanggal terhadap riwayat {@link PenugasanAuditSPI} yang sudah pernah dilaksanakan di
	 * unit ini. Default 1 bila belum diisi.
	 *
	 * @return skor lama tidak diaudit (1&ndash;5); 1 bila nilai tersimpan {@code null}.
	 */
	@Column(name = "skor_lama_tidak_diaudit", nullable = false)
	public Integer getSkorLamaTidakDiaudit() {
		return skorLamaTidakDiaudit == null ? 1 : skorLamaTidakDiaudit;
	}

	/**
	 * Mengisi skor komponen lama tidak diaudit (idealnya 1&ndash;5).
	 *
	 * @param skorLamaTidakDiaudit skor lama tidak diaudit baru.
	 */
	public void setSkorLamaTidakDiaudit(Integer skorLamaTidakDiaudit) {
		this.skorLamaTidakDiaudit = skorLamaTidakDiaudit;
	}

	/**
	 * Catatan bebas tambahan mengenai penilaian risiko baris ini, mis. justifikasi kualitatif di
	 * balik skor-skor yang diberikan, atau perubahan signifikan sejak penilaian tahun sebelumnya.
	 *
	 * @return teks catatan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "catatan", nullable = true, columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/**
	 * Mengisi catatan bebas untuk baris penilaian risiko ini.
	 *
	 * @param catatan teks catatan baru.
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Status aktif/nonaktif baris ini; nilai {@code null} SENGAJA diperlakukan sebagai
	 * {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity "data
	 * master sederhana" di aplikasi ini.
	 *
	 * @return {@code true} bila baris profil risiko ini aktif (termasuk saat nilai tersimpan
	 *         {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif baris profil risiko ini. Menonaktifkan (bukan menghapus)
	 * menjaga integritas referensial baris {@link RencanaAuditTahunanSPI} yang sudah pernah
	 * mengacu ke sini lewat {@link RencanaAuditTahunanSPI#getProfilRisikoSPI()}.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Jumlah kelima komponen skor risiko, dihitung LANGSUNG dari nilai komponen saat ini
	 * (bukan nilai tersimpan) &mdash; lihat penjelasan "nilai rollup basi" di javadoc kelas.
	 * Rentang hasil: 5 (risiko paling rendah) sampai 25 (risiko paling tinggi).
	 */
	@Transient
	public int getTotalSkorRisiko() {
		return getSkorMaterialitas() + getSkorDampakOperasional() + getSkorKualitasPengendalian()
				+ getSkorTemuanSebelumnya() + getSkorLamaTidakDiaudit();
	}

	/**
	 * Klasifikasi zona risiko (Tinggi/Sedang/Rendah) berdasarkan {@link #getTotalSkorRisiko()},
	 * dipakai untuk memprioritaskan unit mana yang paling layak masuk PKPT tahun berjalan pada
	 * {@link RencanaAuditTahunanSPI} dan untuk pewarnaan lencana (badge) di tampilan tabel/dasbor.
	 */
	@Transient
	public String getZonaRisiko() {
		int total = getTotalSkorRisiko();
		if (total >= AMBANG_ZONA_TINGGI) return ZONA_TINGGI;
		if (total >= AMBANG_ZONA_SEDANG) return ZONA_SEDANG;
		return ZONA_RENDAH;
	}

}
