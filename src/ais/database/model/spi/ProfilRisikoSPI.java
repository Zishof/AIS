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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

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

	public ProfilRisikoSPI() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@Column(name = "skor_materialitas", nullable = false)
	public Integer getSkorMaterialitas() {
		return skorMaterialitas == null ? 1 : skorMaterialitas;
	}

	public void setSkorMaterialitas(Integer skorMaterialitas) {
		this.skorMaterialitas = skorMaterialitas;
	}

	@Column(name = "skor_dampak_operasional", nullable = false)
	public Integer getSkorDampakOperasional() {
		return skorDampakOperasional == null ? 1 : skorDampakOperasional;
	}

	public void setSkorDampakOperasional(Integer skorDampakOperasional) {
		this.skorDampakOperasional = skorDampakOperasional;
	}

	@Column(name = "skor_kualitas_pengendalian", nullable = false)
	public Integer getSkorKualitasPengendalian() {
		return skorKualitasPengendalian == null ? 1 : skorKualitasPengendalian;
	}

	public void setSkorKualitasPengendalian(Integer skorKualitasPengendalian) {
		this.skorKualitasPengendalian = skorKualitasPengendalian;
	}

	@Column(name = "skor_temuan_sebelumnya", nullable = false)
	public Integer getSkorTemuanSebelumnya() {
		return skorTemuanSebelumnya == null ? 1 : skorTemuanSebelumnya;
	}

	public void setSkorTemuanSebelumnya(Integer skorTemuanSebelumnya) {
		this.skorTemuanSebelumnya = skorTemuanSebelumnya;
	}

	@Column(name = "skor_lama_tidak_diaudit", nullable = false)
	public Integer getSkorLamaTidakDiaudit() {
		return skorLamaTidakDiaudit == null ? 1 : skorLamaTidakDiaudit;
	}

	public void setSkorLamaTidakDiaudit(Integer skorLamaTidakDiaudit) {
		this.skorLamaTidakDiaudit = skorLamaTidakDiaudit;
	}

	@Column(name = "catatan", nullable = true, columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

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
