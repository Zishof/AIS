package ais.database.model.obe;

// Indikator Kinerja (IK) / Performance Indicator (PI) untuk tiap CPL.

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
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;

/**
 * <h3>IndikatorKinerja — Indikator Kinerja (IK) / Performance Indicator (PI)</h3>
 *
 * <p>Penjabaran terukur dari sebuah CPL: pernyataan spesifik yang dipakai untuk menilai
 * apakah CPL tercapai. Tiap CPL ({@link CapaianLulusan}) dapat memiliki beberapa IK/PI.
 * Mengikuti pola entitas OBE lain (extends {@link GeneralValueObject}, ber-audit Envers,
 * tabel di skema public).</p>
 *
 * <p><b>DDL (hbm2ddl = none → WAJIB manual) — public.indikator_kinerja &amp;
 * new_audit.indikator_kinerja__audit:</b></p>
 * <pre>
 * CREATE TABLE public.indikator_kinerja (
 *   id              bigserial PRIMARY KEY,
 *   oleh            varchar(255),
 *   oleh_id         varchar(255),
 *   tanggal_dirubah timestamp,
 *   kode            varchar(255),
 *   nama            text,
 *   keterangan      text,
 *   jurusan         bigint,
 *   perguruan_tinggi bigint,
 *   capaian_lulusan bigint,
 *   nomor_urut      integer,
 *   bobot           double precision,
 *   aktif           boolean
 * );
 * </pre>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "indikator_kinerja")
public class IndikatorKinerja extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439810L;

	private Long id;
	private String oleh;
	private String olehId;

	/** @return ID pengguna (username) yang terakhir mengubah baris IK ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul OBE.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang terakhir mengubah baris IK ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE untuk
	 * mengisi {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} dari sesi pengguna aktif
	 * via {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Timestamp terakhir baris IK ini diubah; default diisi saat objek dibuat, diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return timestamp terakhir baris IK ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code "<id>-<nama>"}, dipakai untuk tampilan log/debug. */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private Jurusan jurusan;
	private PerguruanTinggi perguruanTinggi;
	private CapaianLulusan capaianLulusan;
	private String nama;
	private String keterangan;
	private Integer nomorUrut;
	private Double bobot;
	private Boolean aktif;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public IndikatorKinerja() {
	}

	/** @return ID unik baris IK (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode singkat IK (mis. "IK1.1"), di-trim; string kosong bila belum diisi. */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return rumusan/nama Indikator Kinerja (di-trim); {@code null} bila belum diisi. Wajib diisi ({@code nullable = false}). */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan IK (opsional); tidak di-trim, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif IK. Flag satu-arah: {@code null} (baris lama/belum pernah diisi)
	 *         dianggap aktif secara default agar data lama tidak tiba-tiba hilang dari daftar
	 *         IK aktif yang dipakai penilaian ketercapaian CPL.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return nomor urut tampilan IK ini di dalam daftar IK milik satu CPL ({@link #getCapaianLulusan()}); boleh {@code null}. */
	@Column(name = "nomor_urut")
	public Integer getNomorUrut() {
		return nomorUrut;
	}

	/** @param nomorUrut lihat {@link #getNomorUrut()}. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return bobot/kontribusi IK ini terhadap penilaian ketercapaian CPL induknya
	 *         ({@link #getCapaianLulusan()}). Nilai {@code null} (belum diisi) dinormalkan
	 *         menjadi {@code 0.0}, sama seperti pola
	 *         {@link CapaianPembelajaranLulusan#getBobot()}.
	 */
	public Double getBobot() {
		return bobot == null ? 0.0 : bobot;
	}

	/** @param bobot lihat {@link #getBobot()}. */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/**
	 * @return program studi pemilik IK ini, lazy-loaded, di-null-safe-kan via
	 *         {@link GeneralValueObject#check(Object)}. Berbeda dari getter {@code getJurusan()}
	 *         milik {@link CapaianLulusan}/{@link CapaianPembelajaranLulusan}/{@link BahanKajian},
	 *         getter ini TIDAK menimpa nilai dengan jurusan hasil penelusuran relasi lain
	 *         (mis. dari {@link #getCapaianLulusan()}) — nilai field {@link #jurusan} milik
	 *         baris ini sendiri selalu jadi sumber kebenaran.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param jurusan lihat {@link #getJurusan()}. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return perguruan tinggi pemilik IK ini. Lazy-loaded, di-null-safe-kan via
	 *         {@link GeneralValueObject#check(Object)}; bila hasilnya masih {@code null}
	 *         (baris lama tanpa kolom ini terisi), jatuh ke PT milik sesi pengguna saat ini
	 *         via {@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()}
	 *         (exception ditangkap diam-diam, direkam ke {@link ais.common.ErrorAuditUtil}).
	 *         Berbeda dari {@link CapaianLulusan#getPerguruanTinggi()}, getter ini TIDAK
	 *         ditimpa lagi oleh PT hasil penelusuran Jurusan → Fakultas → PerguruanTinggi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/obe/IndikatorKinerja.java:203");
		}
		return perguruanTinggi;
	}

	/** @param perguruanTinggi lihat {@link #getPerguruanTinggi()}. */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * CPL yang dijabarkan oleh indikator ini.
	 *
	 * <p>Ini relasi many-to-one inti kelas {@code IndikatorKinerja}: satu IK selalu menempel
	 * ke tepat satu {@link CapaianLulusan} (CPL), lazy-loaded dan di-null-safe-kan via
	 * {@link GeneralValueObject#check(Object)}. Berbeda dari rantai
	 * {@link CapaianLulusan}&nbsp;&rarr;&nbsp;{@link CapaianPembelajaranLulusan} (CPMK) yang
	 * disimpan sebagai CSV ID many-to-many, relasi IK-ke-CPL ini adalah FK asli — satu IK
	 * hanya bisa menjabarkan satu CPL, tapi satu CPL boleh punya banyak IK (di-order via
	 * {@link #getNomorUrut()}).</p>
	 *
	 * @return CPL induk indikator ini, atau {@code null} bila belum diisi/relasi terputus.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "capaian_lulusan")
	public CapaianLulusan getCapaianLulusan() {
		capaianLulusan = check(capaianLulusan);
		return capaianLulusan;
	}

	/** @param capaianLulusan lihat {@link #getCapaianLulusan()}. */
	public void setCapaianLulusan(CapaianLulusan capaianLulusan) {
		this.capaianLulusan = capaianLulusan;
	}
}
