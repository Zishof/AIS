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

	public IndikatorKinerja() {
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

	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "nomor_urut")
	public Integer getNomorUrut() {
		return nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	public Double getBobot() {
		return bobot == null ? 0.0 : bobot;
	}

	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

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

	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/** CPL yang dijabarkan oleh indikator ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "capaian_lulusan")
	public CapaianLulusan getCapaianLulusan() {
		capaianLulusan = check(capaianLulusan);
		return capaianLulusan;
	}

	public void setCapaianLulusan(CapaianLulusan capaianLulusan) {
		this.capaianLulusan = capaianLulusan;
	}
}
