package ais.database.model.inventory;

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
 * <h2>SetoranTenant — Setoran &amp; Bagi Hasil Tenant/Stan (Toko) untuk Toko/Kantin.</h2>
 *
 * <p>
 * Entity BARU untuk mencatat kewajiban dan setoran tiap <b>tenant/stan</b> (direpresentasikan oleh
 * {@link Toko}) secara periodik, sehingga tersedia laporan katalog §3.8: <i>Bagi Hasil Tenant</i>,
 * <i>Setoran Tenant</i>, dan <i>Tunggakan Tenant</i>. Sebelumnya {@code Toko} hanya menyimpan
 * nama/keterangan sehingga aspek komersial (komisi, sewa, setoran) belum bisa dilaporkan. Dengan
 * entity ini + pendaftaran di {@code hibernate.cfg.xml}, tabel {@code koperasi.setoran_tenant}
 * otomatis dibuat (hbm2ddl=update).
 * </p>
 *
 * <h3>Perhitungan</h3>
 * <ul>
 *   <li><b>Bagi hasil (komisi)</b> = {@code omzet} × {@code persenBagiHasil}% (disimpan pada
 *       {@code nilaiBagiHasil}; boleh diisi manual bila skema bukan persentase).</li>
 *   <li><b>Total kewajiban</b> = {@code nilaiBagiHasil} + {@code sewa} + {@code biayaLayanan}.</li>
 *   <li><b>Sisa/tunggakan</b> = total kewajiban − {@code setoran}. Bila &le; 0 dianggap LUNAS.</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek: field ber-@Column memakai nama eksplisit, field
 * numerik/tanggal tanpa @Column ter-<i>fold</i> menjadi huruf kecil tanpa underscore
 * (mis. {@code persenBagiHasil}→{@code persenbagihasil}, {@code nilaiBagiHasil}→{@code nilaibagihasil},
 * {@code biayaLayanan}→{@code biayalayanan}). Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul tenant)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "setoran_tenant")
public class SetoranTenant extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Kewajiban tenant sudah terpenuhi. */
	public static final String STATUS_LUNAS = "LUNAS";
	/** Masih ada kekurangan setoran (tunggakan). */
	public static final String STATUS_KURANG = "KURANG";

	private Long id;
	private Toko toko;
	private Date tanggal;
	private String periode;
	private Double omzet;
	private Double persenBagiHasil;
	private Double nilaiBagiHasil;
	private Double sewa;
	private Double biayaLayanan;
	private Double setoran;
	private String status;
	private String keterangan;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SetoranTenant() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	public String getPeriode() {
		return periode;
	}

	public void setPeriode(String periode) {
		this.periode = periode;
	}

	public Double getOmzet() {
		return omzet == null ? 0.0 : omzet;
	}

	public void setOmzet(Double omzet) {
		this.omzet = omzet;
	}

	public Double getPersenBagiHasil() {
		return persenBagiHasil == null ? 0.0 : persenBagiHasil;
	}

	public void setPersenBagiHasil(Double persenBagiHasil) {
		this.persenBagiHasil = persenBagiHasil;
	}

	public Double getNilaiBagiHasil() {
		return nilaiBagiHasil == null ? 0.0 : nilaiBagiHasil;
	}

	public void setNilaiBagiHasil(Double nilaiBagiHasil) {
		this.nilaiBagiHasil = nilaiBagiHasil;
	}

	public Double getSewa() {
		return sewa == null ? 0.0 : sewa;
	}

	public void setSewa(Double sewa) {
		this.sewa = sewa;
	}

	public Double getBiayaLayanan() {
		return biayaLayanan == null ? 0.0 : biayaLayanan;
	}

	public void setBiayaLayanan(Double biayaLayanan) {
		this.biayaLayanan = biayaLayanan;
	}

	public Double getSetoran() {
		return setoran == null ? 0.0 : setoran;
	}

	public void setSetoran(Double setoran) {
		this.setoran = setoran;
	}

	public String getStatus() {
		return status == null ? STATUS_KURANG : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
