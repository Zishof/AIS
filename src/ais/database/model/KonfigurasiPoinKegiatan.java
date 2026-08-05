package ais.database.model;

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

/**
 * Aturan poin/voucher yang diberikan untuk kehadiran satu {@link FormulirKegiatan} -- fitur "Voucher
 * Pegawai" (bonus poin belanja koperasi dari kehadiran Kajian &amp; Disiplin Kehadiran).
 *
 * <p><b>Relasi 1-ke-1 dengan {@link FormulirKegiatan}.</b> Tiap kategori kegiatan yang diberi poin
 * (mis. "Kajian Tafsir Al-Quran", "Kajian Hadist", "Kajian Al-Hikam", dan satu baris sintetis
 * "Disiplin Kehadiran" yang TIDAK punya sesi {@link Pertemuan} sungguhan) punya SATU baris konfigurasi
 * di sini. Dipisah dari {@code FormulirKegiatan} sendiri (bukan kolom tambahan di situ) supaya entitas
 * kegiatan generik tetap bersih dari konsep "poin/voucher" yang spesifik fitur ini.</p>
 *
 * <p><b>{@link #poinOffline}/{@link #poinOnline}</b> -- nilai poin (Rupiah) untuk kehadiran offline vs
 * online pada kegiatan yang punya sesi {@code Pertemuan} sungguhan (Kajian). Untuk baris sintetis
 * "Disiplin Kehadiran" (tidak ada mode online/offline, murni evaluasi absensi bulanan), HANYA
 * {@link #poinOffline} yang dipakai sebagai nilai poinnya -- {@link #poinOnline} dibiarkan kosong/0,
 * bukan menambah kolom ketiga yang ambigu.</p>
 *
 * <p><b>{@link #durasiBerlakuHari}</b> -- masa berlaku voucher (dalam hari) sejak tanggal poin
 * diterbitkan, sebelum dihanguskan bila belum sempat dibelanjakan. Dipakai untuk mengisi
 * {@link Deposit#getTanggalExpired()} saat voucher diterbitkan (tanggal terbit + durasi ini) --
 * kosong/null berarti voucher TIDAK PERNAH kedaluwarsa.</p>
 *
 * @see Deposit#getKonfigurasiPoinKegiatan()
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "konfigurasi_poin_kegiatan")
public class KonfigurasiPoinKegiatan extends GeneralValueObject {

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
		formulirKegiatan = getFormulirKegiatan();
		return id + "-" + formulirKegiatan;
	}

	private FormulirKegiatan formulirKegiatan;
	private Double poinOffline;
	private Double poinOnline;
	private Integer durasiBerlakuHari;
	private Boolean aktif;
	private String keterangan;

	public KonfigurasiPoinKegiatan() {
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
	@JoinColumn(name = "formulir_kegiatan", nullable = false)
	public FormulirKegiatan getFormulirKegiatan() {
		formulirKegiatan = check(formulirKegiatan);
		return formulirKegiatan;
	}

	public void setFormulirKegiatan(FormulirKegiatan formulirKegiatan) {
		this.formulirKegiatan = formulirKegiatan;
	}

	@Column(name = "poin_offline", nullable = true)
	public Double getPoinOffline() {
		return poinOffline == null ? 0.0 : poinOffline;
	}

	public void setPoinOffline(Double poinOffline) {
		this.poinOffline = poinOffline;
	}

	@Column(name = "poin_online", nullable = true)
	public Double getPoinOnline() {
		return poinOnline == null ? 0.0 : poinOnline;
	}

	public void setPoinOnline(Double poinOnline) {
		this.poinOnline = poinOnline;
	}

	/** @return jumlah hari masa berlaku voucher sebelum hangus; {@code null} = tidak pernah kedaluwarsa. */
	@Column(name = "durasi_berlaku_hari", nullable = true)
	public Integer getDurasiBerlakuHari() {
		return durasiBerlakuHari;
	}

	public void setDurasiBerlakuHari(Integer durasiBerlakuHari) {
		this.durasiBerlakuHari = durasiBerlakuHari;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
