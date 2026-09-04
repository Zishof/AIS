package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * Jejak dispensing resep: pemeriksaan kedua (double-check) dan konseling
 * pasien (IR-05 modernisasi UI/UX Apotik).
 *
 * <p><b>Mengapa tabel baru?</b> {@code Resep} adalah entity milik modul SIRS
 * yang dipakai jalur rumah sakit dan sudah {@code @Audited}; menambah kolom di
 * sana menuntut ALTER tabel audit lama (gotcha Envers). Tabel BARU dibuat
 * otomatis oleh {@code hbm2ddl=update} berikut tabel auditnya — tanpa migrasi
 * manual sama sekali.</p>
 *
 * <p><b>Aturan keselamatan yang ditegakkan server</b> (lihat
 * {@code ApotikDispensingHelper}): pemeriksa kedua WAJIB akun yang berbeda
 * dari penyiap; satu resep hanya boleh punya satu catatan per jenis yang
 * aktif. Catatan bersifat append-only — pembatalan dilakukan dengan
 * menonaktifkan baris, bukan menghapusnya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_dispensing_log")
public class ApotikDispensingLog extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Pemeriksaan kedua sebelum obat diserahkan. */
	public static final String JENIS_DOUBLE_CHECK = "DOUBLE_CHECK";
	/** Konseling/penjelasan pemakaian obat kepada pasien. */
	public static final String JENIS_KONSELING = "KONSELING";

	private Long id;
	private Resep resep;
	private String jenis;

	/** Akun yang MENYIAPKAN obat (pembanding untuk aturan pemeriksa kedua). */
	private String penyiapUserId;

	/** Akun yang melakukan pemeriksaan/konseling ini. */
	private String pelakuUserId;
	private String pelakuNama;
	private String catatan;
	private Boolean aktif;
	private Date waktu;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (jenis == null ? "" : jenis);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resep", nullable = false)
	public Resep getResep() { resep = check(resep); return resep; }
	public void setResep(Resep resep) { this.resep = resep; }

	@Column(name = "jenis", nullable = false, length = 24)
	public String getJenis() { return jenis; }
	public void setJenis(String jenis) { this.jenis = jenis; }

	@Column(name = "penyiap_user_id", length = 60)
	public String getPenyiapUserId() { return penyiapUserId; }
	public void setPenyiapUserId(String penyiapUserId) { this.penyiapUserId = penyiapUserId; }

	@Column(name = "pelaku_user_id", nullable = false, length = 60)
	public String getPelakuUserId() { return pelakuUserId; }
	public void setPelakuUserId(String pelakuUserId) { this.pelakuUserId = pelakuUserId; }

	@Column(name = "pelaku_nama", length = 160)
	public String getPelakuNama() { return pelakuNama; }
	public void setPelakuNama(String pelakuNama) { this.pelakuNama = pelakuNama; }

	@Column(name = "catatan", columnDefinition = "text")
	public String getCatatan() { return catatan; }
	public void setCatatan(String catatan) { this.catatan = catatan; }

	@Column(name = "aktif")
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
