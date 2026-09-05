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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>bkd/penelitian</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_bkd_penelitian")
public class BkdPenelitianSister extends GeneralValueObject {
	/** Versi serialisasi. */
	private static final long serialVersionUID = 1L;
	/** ID baris lokal (surrogate key, auto increment; bukan id SISTER). */
	private Long id;
	/** Nama pengguna aplikasi yang melakukan perubahan terakhir (field audit shadow, diisi ulang oleh {@code onUpdate}). */
	private String oleh;
	/** ID pengguna aplikasi yang melakukan perubahan terakhir (field audit shadow). */
	private String olehId;
	/** Timestamp perubahan terakhir baris ini (field audit shadow); default saat objek dibuat, diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kode/id item kegiatan penelitian pada SISTER — kunci upsert sinkronisasi (bukan PK lokal). */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk item ini (cadangan lengkap agar tidak ada data yang hilang). */
	private String keterangan;
	/** Penanda baris aktif; {@code null} diperlakukan sebagai aktif, lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** id_sdm dosen pemilik butir beban penelitian ini (acuan ke {@code RefSdmSister}). */
	private String idSdm;
	/** id_smt semester butir beban penelitian ini dihitung. */
	private String idSmt;
	/** Nama dosen (salinan denormalisasi dari SISTER, untuk kemudahan query/laporan). */
	private String nmSdm;
	/** NIDN dosen (salinan denormalisasi dari SISTER, untuk kemudahan query/laporan). */
	private String nidn;
	/** Unsur/komponen kegiatan penelitian menurut kategori SISTER. */
	private String unsur;
	/** Judul/uraian kegiatan penelitian yang menjadi butir beban ini. */
	private String judulKeg;
	/** id kategori kegiatan penelitian pada SISTER. */
	private Integer idKatgiat;
	/** Nama kategori kegiatan penelitian (salinan denormalisasi dari SISTER). */
	private String nmKat;
	/** Beban SKS (satuan kredit semester) kegiatan penelitian ini yang diperhitungkan dalam BKD. */
	private Double bebanSks;
	/** Nilai/skor kegiatan penelitian ini sebagaimana dihitung SISTER. */
	private Double nilai;

	/** Konstruktor kosong (dibutuhkan Hibernate). */
	public BkdPenelitianSister() {}
	/** @return ID baris lokal (surrogate key). */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/** @param id ID baris lokal baru. */
	public void setId(Long id) { this.id = id; }
	/** @return ID pengguna aplikasi yang terakhir mengubah baris ini. */
	public String getOlehId() { return olehId; }
	/** Menyetel ID pengguna pengubah; abai (no-op) bila kosong/blank agar nilai lama tak tertimpa kosong. @param olehId ID pengguna baru. */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	/** @return nama pengguna aplikasi yang terakhir mengubah baris ini. */
	public String getOleh() { return oleh; }
	/** Menyetel nama pengguna pengubah; abai (no-op) bila kosong/blank agar nilai lama tak tertimpa kosong. @param oleh nama pengguna baru. */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** Callback JPA {@code @PreUpdate}: mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} untuk memperbarui {@code oleh}/{@code olehId}/{@code tanggal_dirubah}. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	/** @return timestamp perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param t timestamp perubahan baru. */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	/** @return kode/id item SISTER, di-trim; {@code null} bila kosong. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode kode/id item SISTER baru. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return salinan JSON mentah respons SISTER untuk item ini. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k JSON mentah baru. */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return status aktif; {@code true} bila belum pernah diisi ({@code null}). */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a status aktif baru. */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return id_sdm dosen pemilik butir beban penelitian ini. */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }
	/** @param v id_sdm baru. */
	public void setIdSdm(String v) { this.idSdm = v; }
	/** @return id_smt semester butir beban penelitian ini. */
	@Column(name = "id_smt", columnDefinition = "text") public String getIdSmt() { return idSmt; }
	/** @param v id_smt baru. */
	public void setIdSmt(String v) { this.idSmt = v; }
	/** @return nama dosen (salinan denormalisasi). */
	@Column(name = "nm_sdm", columnDefinition = "text") public String getNmSdm() { return nmSdm; }
	/** @param v nama dosen baru. */
	public void setNmSdm(String v) { this.nmSdm = v; }
	/** @return NIDN dosen (salinan denormalisasi dari SISTER). */
	@Column(name = "nidn", columnDefinition = "text") public String getNidn() { return nidn; }
	/** @param v NIDN baru. */
	public void setNidn(String v) { this.nidn = v; }
	/** @return unsur/komponen kegiatan penelitian. */
	@Column(name = "unsur", columnDefinition = "text") public String getUnsur() { return unsur; }
	/** @param v unsur baru. */
	public void setUnsur(String v) { this.unsur = v; }
	/** @return judul/uraian kegiatan penelitian. */
	@Column(name = "judul_keg", columnDefinition = "text") public String getJudulKeg() { return judulKeg; }
	/** @param v judul kegiatan baru. */
	public void setJudulKeg(String v) { this.judulKeg = v; }
	/** @return id kategori kegiatan penelitian pada SISTER. */
	@Column(name = "id_katgiat") public Integer getIdKatgiat() { return idKatgiat; }
	/** @param v id kategori kegiatan baru. */
	public void setIdKatgiat(Integer v) { this.idKatgiat = v; }
	/** @return nama kategori kegiatan penelitian. */
	@Column(name = "nm_kat", columnDefinition = "text") public String getNmKat() { return nmKat; }
	/** @param v nama kategori baru. */
	public void setNmKat(String v) { this.nmKat = v; }
	/** @return beban SKS kegiatan penelitian ini. */
	@Column(name = "beban_sks") public Double getBebanSks() { return bebanSks; }
	/** @param v beban SKS baru. */
	public void setBebanSks(Double v) { this.bebanSks = v; }
	/** @return nilai/skor kegiatan penelitian ini. */
	@Column(name = "nilai") public Double getNilai() { return nilai; }
	/** @param v nilai baru. */
	public void setNilai(Double v) { this.nilai = v; }
	/** @return representasi ringkas "id-kode" untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
