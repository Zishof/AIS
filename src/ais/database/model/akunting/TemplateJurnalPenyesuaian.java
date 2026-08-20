package ais.database.model.akunting;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Template jurnal penyesuaian berkala (amortisasi, akrual, penyisihan).
 *
 * <p><b>Celah yang ditutup.</b> Penyusutan aset sudah punya prosesnya sendiri, tetapi penyesuaian
 * berkala lain &mdash; amortisasi biaya dibayar di muka, akrual beban yang belum ditagih, dan
 * penyisihan piutang tak tertagih &mdash; belum ada prosesnya sama sekali; satu-satunya jalan
 * adalah mengetik Jurnal Umum manual tiap bulan, yang mudah terlewat dan mudah salah akun.</p>
 *
 * <p><b>Cara pakai.</b> Sekali saja definisikan: nama, akun debet, akun kredit, nilai per periode,
 * dan frekuensinya. Tiap periode tinggal dilihat drafnya lalu diposting; satu template hanya bisa
 * diposting SEKALI untuk periode yang sama (penanda periode disimpan pada keterangan jurnalnya),
 * sehingga menjalankan ulang tidak menggandakan beban.</p>
 *
 * <p>Tabelnya dibuat otomatis oleh Hibernate.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "template_jurnal_penyesuaian")
public class TemplateJurnalPenyesuaian extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Sebulan sekali (default). */
	public static final String BULANAN = "BULANAN";
	/** Setahun sekali. */
	public static final String TAHUNAN = "TAHUNAN";

	private Long id;
	private String nama;
	private Akun akunDebet;
	private Akun akunKredit;
	private Double nilai;
	private String frekuensi;
	private Boolean aktif;
	private String keterangan;
	private ais.database.model.rab.SatuanKerja satuanKerja;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TemplateJurnalPenyesuaian() {
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

	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_debet", nullable = true)
	public Akun getAkunDebet() {
		return akunDebet;
	}

	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_kredit", nullable = true)
	public Akun getAkunKredit() {
		return akunKredit;
	}

	public void setAkunKredit(Akun akunKredit) {
		this.akunKredit = akunKredit;
	}

	@Column(name = "nilai", nullable = true)
	public Double getNilai() {
		return nilai == null ? Double.valueOf(0) : nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	@Column(name = "frekuensi", nullable = true, length = 20)
	public String getFrekuensi() {
		return frekuensi == null || frekuensi.trim().isEmpty() ? BULANAN : frekuensi.trim().toUpperCase();
	}

	public void setFrekuensi(String frekuensi) {
		this.frekuensi = frekuensi;
	}

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public ais.database.model.rab.SatuanKerja getSatuanKerja() {
		return satuanKerja;
	}

	public void setSatuanKerja(ais.database.model.rab.SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	@Column(name = "olehid", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
