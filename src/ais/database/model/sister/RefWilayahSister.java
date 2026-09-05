package ais.database.model.sister;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * Entitas hasil sinkronisasi SISTER untuk endpoint referensi <b>{@code referensi/wilayah}</b> (daftar wilayah
 * administratif berjenjang — provinsi/kabupaten-kota/kecamatan/kelurahan — ditarik 4x per level via parameter
 * {@code id_level_wilayah=0..3} pada {@code DataSisterApi.daftarEndpoint}, seluruhnya diupsert ke SATU tabel
 * ini). Struktur kolom inti, mekanisme upsert refleksi generik, pola field audit shadow, dan default
 * aktif=true — identik dengan kelas pola {@link RefAgamaSister}; lihat javadoc di sana untuk penjelasan
 * lengkap mesin sinkronisasi. Berbeda dari 32 kelas {@code Ref*Sister} murni, kelas ini punya SATU kolom
 * tambahan: {@link #idIndukWilayah} (field JSON {@code "id_induk_wilayah"}) yang membentuk pohon wilayah
 * (wilayah level bawah menunjuk ke {@link #kode} wilayah level atasnya). {@link #kode}=id item SISTER (kunci
 * upsert); {@link #keterangan}=JSON mentah baris ini. Dipetakan di {@link SisterEntitasRegistry} pada key
 * {@code "referensi/wilayah"}. {@code @Audited} (tabel bayangan {@code sister_ref_wilayah_AUD} otomatis via
 * hbm2ddl).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_wilayah")
public class RefWilayahSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	/** PK lokal (identity DB), BUKAN kode SISTER — lihat {@link #kode}. */
	private Long id;
	/** Nama aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOleh}). */
	private String oleh;
	/** ID aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOlehId}). */
	private String olehId;
	/** Timestamp perubahan terakhir; diinisialisasi saat objek dibuat, dimutakhirkan oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kode/id item SISTER (field JSON {@code "id"}) — kunci upsert sinkronisasi; juga jadi acuan {@link #idIndukWilayah} wilayah level di bawahnya. */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk baris ini. */
	private String keterangan;
	/** Flag aktif dari SISTER; {@code null} diperlakukan aktif oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Nama wilayah (field JSON {@code "nama"}). */
	private String nama;
	/** ID wilayah induk/parent pada hierarki (field JSON {@code "id_induk_wilayah"}, merujuk {@link #kode} baris level atasnya) — kolom tambahan kelas ini, tak ada di 32 kelas {@code Ref*Sister} murni. */
	private String idIndukWilayah;

	/** Constructor default (kontrak JPA/Hibernate — instansiasi via refleksi). */
	public RefWilayahSister() {}
	/** @return PK lokal (identity DB), bukan kode SISTER. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/** @param id PK lokal; normalnya diisi Hibernate saat insert, bukan diset manual. */
	public void setId(Long id) { this.id = id; }
	/** @return id aktor pengubah terakhir. */
	public String getOlehId() { return olehId; }
	/**
	 * Menetapkan id aktor pengubah. Mengabaikan diam-diam nilai null/kosong (nilai lama dipertahankan) —
	 * pola guard field audit shadow yang berulang di seluruh klaster entitas SISTER, BUKAN bug.
	 * @param olehId id aktor baru; diabaikan bila null/blank.
	 */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	/** @return nama aktor pengubah terakhir. */
	public String getOleh() { return oleh; }
	/**
	 * Menetapkan nama aktor pengubah. Mengabaikan diam-diam nilai null/kosong, sama seperti {@link #setOlehId}.
	 * @param oleh nama aktor baru; diabaikan bila null/blank.
	 */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** Callback {@code @PreUpdate}: mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah} otomatis pada setiap UPDATE. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	/** @return timestamp perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param t timestamp baru (biasanya diisi otomatis via {@link #onUpdate()}, bukan manual). */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	/** @return kode SISTER, di-trim; string kosong dinormalisasi menjadi {@code null}. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode kode/id item SISTER baru (kunci upsert); TIDAK di-trim di setter, hanya di getter. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return JSON mentah baris ini. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k JSON mentah baru (biasanya {@code JSONObject.toString()} dari respons SISTER). */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return status aktif; {@code null} tersimpan diperlakukan sebagai {@code true} (default aktif). */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a status aktif baru dari SISTER. */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return nama wilayah. */
	@Column(name = "nama", columnDefinition = "text") public String getNama() { return nama; }
	/** @param v nama wilayah baru. */
	public void setNama(String v) { this.nama = v; }
	/** @return id wilayah induk/parent pada hierarki. */
	@Column(name = "id_induk_wilayah", columnDefinition = "text") public String getIdIndukWilayah() { return idIndukWilayah; }
	/** @param v id wilayah induk/parent baru. */
	public void setIdIndukWilayah(String v) { this.idIndukWilayah = v; }
	/** @return representasi ringkas {@code id-kode} untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
