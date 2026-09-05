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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>bkd/laporan_akhir_bkd</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_bkd_laporan_akhir_bkd")
public class BkdLaporanAkhirBkdSister extends GeneralValueObject {
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
	/** Kode/id laporan akhir BKD pada SISTER — kunci upsert sinkronisasi (bukan PK lokal). */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk laporan ini (cadangan lengkap agar tidak ada data yang hilang). */
	private String keterangan;
	/** Penanda baris aktif; {@code null} diperlakukan sebagai aktif, lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** id_sdm dosen pemilik laporan akhir BKD ini (acuan ke {@code RefSdmSister}). */
	private String idSdm;
	/** id_smt semester laporan akhir BKD ini dihitung/dinilai. */
	private String idSmt;
	/** id registrasi PTK (Pendidik dan Tenaga Kependidikan) dosen bersangkutan pada laporan ini. */
	private String idRegPtk;
	/** SKS kinerja unsur pengajaran yang tercapai (realisasi) pada laporan ini. */
	private String sksKinerjaAjar;
	/** SKS unsur pengajaran yang melebihi ambang kinerja normal pada laporan ini. */
	private String sksLebihAjar;
	/** SKS kinerja unsur pendidikan (didik) yang tercapai (realisasi) pada laporan ini. */
	private String sksKinerjaDidik;
	/** SKS unsur pendidikan (didik) yang melebihi ambang kinerja normal pada laporan ini. */
	private String sksLebihDidik;
	/** SKS kinerja unsur penelitian (lit) yang tercapai (realisasi) pada laporan ini. */
	private String sksKinerjaLit;
	/** SKS unsur penelitian (lit) yang melebihi ambang kinerja normal pada laporan ini. */
	private String sksLebihLit;
	/** SKS kinerja unsur pengabdian masyarakat (Pengmas) yang tercapai (realisasi) pada laporan ini. */
	private String sksKinerjaPengmas;
	/** SKS unsur pengabdian masyarakat (Pengmas) yang melebihi ambang kinerja normal pada laporan ini. */
	private String sksLebihPengmas;
	/** SKS kinerja unsur penunjang tridharma yang tercapai (realisasi) pada laporan ini. */
	private String sksKinerjaPenunjang;
	/** SKS unsur penunjang tridharma yang melebihi ambang kinerja normal pada laporan ini. */
	private String sksLebihTunjang;
	/** Total SKS kinerja seluruh unsur (ajar+didik+lit+pengmas+penunjang) yang tercapai pada laporan ini. */
	private String sksKinerja;
	/** Total SKS seluruh unsur yang melebihi ambang kinerja normal pada laporan ini. */
	private String sksLebih;
	/** Status pemenuhan kewajiban BKD dosen ini pada semester bersangkutan (mis. terpenuhi/tidak). */
	private String statKewajiban;
	/** Status tugas dosen ini pada semester bersangkutan (mis. tugas belajar, penugasan lain). */
	private String statTugas;
	/** Status belajar dosen ini (mis. sedang studi lanjut) pada semester bersangkutan. */
	private String statBelajar;
	/** id jabatan fungsional dosen ini pada saat laporan dibuat. */
	private String idJabfung;
	/** Simpulan/penilaian akhir asesor atas laporan BKD dosen ini. */
	private String simpulanAsesor;

	/** Konstruktor kosong (dibutuhkan Hibernate). */
	public BkdLaporanAkhirBkdSister() {}
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
	/** @return kode/id laporan pada SISTER, di-trim; {@code null} bila kosong. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode kode/id laporan baru. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return salinan JSON mentah respons SISTER untuk laporan ini. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k JSON mentah baru. */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return status aktif; {@code true} bila belum pernah diisi ({@code null}). */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a status aktif baru. */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return id_sdm dosen pemilik laporan akhir BKD ini. */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }
	/** @param v id_sdm baru. */
	public void setIdSdm(String v) { this.idSdm = v; }
	/** @return id_smt semester laporan akhir BKD ini. */
	@Column(name = "id_smt", columnDefinition = "text") public String getIdSmt() { return idSmt; }
	/** @param v id_smt baru. */
	public void setIdSmt(String v) { this.idSmt = v; }
	/** @return id registrasi PTK dosen bersangkutan. */
	@Column(name = "id_reg_ptk", columnDefinition = "text") public String getIdRegPtk() { return idRegPtk; }
	/** @param v id registrasi PTK baru. */
	public void setIdRegPtk(String v) { this.idRegPtk = v; }
	/** @return SKS kinerja unsur pengajaran yang tercapai. */
	@Column(name = "sks_kinerja_ajar", columnDefinition = "text") public String getSksKinerjaAjar() { return sksKinerjaAjar; }
	/** @param v SKS kinerja unsur pengajaran baru. */
	public void setSksKinerjaAjar(String v) { this.sksKinerjaAjar = v; }
	/** @return SKS unsur pengajaran yang melebihi ambang kinerja normal. */
	@Column(name = "sks_lebih_ajar", columnDefinition = "text") public String getSksLebihAjar() { return sksLebihAjar; }
	/** @param v SKS lebih unsur pengajaran baru. */
	public void setSksLebihAjar(String v) { this.sksLebihAjar = v; }
	/** @return SKS kinerja unsur pendidikan (didik) yang tercapai. */
	@Column(name = "sks_kinerja_didik", columnDefinition = "text") public String getSksKinerjaDidik() { return sksKinerjaDidik; }
	/** @param v SKS kinerja unsur didik baru. */
	public void setSksKinerjaDidik(String v) { this.sksKinerjaDidik = v; }
	/** @return SKS unsur pendidikan (didik) yang melebihi ambang kinerja normal. */
	@Column(name = "sks_lebih_didik", columnDefinition = "text") public String getSksLebihDidik() { return sksLebihDidik; }
	/** @param v SKS lebih unsur didik baru. */
	public void setSksLebihDidik(String v) { this.sksLebihDidik = v; }
	/** @return SKS kinerja unsur penelitian (lit) yang tercapai. */
	@Column(name = "sks_kinerja_lit", columnDefinition = "text") public String getSksKinerjaLit() { return sksKinerjaLit; }
	/** @param v SKS kinerja unsur lit baru. */
	public void setSksKinerjaLit(String v) { this.sksKinerjaLit = v; }
	/** @return SKS unsur penelitian (lit) yang melebihi ambang kinerja normal. */
	@Column(name = "sks_lebih_lit", columnDefinition = "text") public String getSksLebihLit() { return sksLebihLit; }
	/** @param v SKS lebih unsur lit baru. */
	public void setSksLebihLit(String v) { this.sksLebihLit = v; }
	/** @return SKS kinerja unsur pengabdian masyarakat (Pengmas) yang tercapai. */
	@Column(name = "sks_kinerja_pengmas", columnDefinition = "text") public String getSksKinerjaPengmas() { return sksKinerjaPengmas; }
	/** @param v SKS kinerja unsur Pengmas baru. */
	public void setSksKinerjaPengmas(String v) { this.sksKinerjaPengmas = v; }
	/** @return SKS unsur pengabdian masyarakat (Pengmas) yang melebihi ambang kinerja normal. */
	@Column(name = "sks_lebih_pengmas", columnDefinition = "text") public String getSksLebihPengmas() { return sksLebihPengmas; }
	/** @param v SKS lebih unsur Pengmas baru. */
	public void setSksLebihPengmas(String v) { this.sksLebihPengmas = v; }
	/** @return SKS kinerja unsur penunjang tridharma yang tercapai. */
	@Column(name = "sks_kinerja_penunjang", columnDefinition = "text") public String getSksKinerjaPenunjang() { return sksKinerjaPenunjang; }
	/** @param v SKS kinerja unsur penunjang baru. */
	public void setSksKinerjaPenunjang(String v) { this.sksKinerjaPenunjang = v; }
	/** @return SKS unsur penunjang tridharma yang melebihi ambang kinerja normal. */
	@Column(name = "sks_lebih_tunjang", columnDefinition = "text") public String getSksLebihTunjang() { return sksLebihTunjang; }
	/** @param v SKS lebih unsur penunjang baru. */
	public void setSksLebihTunjang(String v) { this.sksLebihTunjang = v; }
	/** @return total SKS kinerja seluruh unsur yang tercapai. */
	@Column(name = "sks_kinerja", columnDefinition = "text") public String getSksKinerja() { return sksKinerja; }
	/** @param v total SKS kinerja baru. */
	public void setSksKinerja(String v) { this.sksKinerja = v; }
	/** @return total SKS seluruh unsur yang melebihi ambang kinerja normal. */
	@Column(name = "sks_lebih", columnDefinition = "text") public String getSksLebih() { return sksLebih; }
	/** @param v total SKS lebih baru. */
	public void setSksLebih(String v) { this.sksLebih = v; }
	/** @return status pemenuhan kewajiban BKD dosen ini. */
	@Column(name = "stat_kewajiban", columnDefinition = "text") public String getStatKewajiban() { return statKewajiban; }
	/** @param v status kewajiban baru. */
	public void setStatKewajiban(String v) { this.statKewajiban = v; }
	/** @return status tugas dosen ini pada semester bersangkutan. */
	@Column(name = "stat_tugas", columnDefinition = "text") public String getStatTugas() { return statTugas; }
	/** @param v status tugas baru. */
	public void setStatTugas(String v) { this.statTugas = v; }
	/** @return status belajar dosen ini pada semester bersangkutan. */
	@Column(name = "stat_belajar", columnDefinition = "text") public String getStatBelajar() { return statBelajar; }
	/** @param v status belajar baru. */
	public void setStatBelajar(String v) { this.statBelajar = v; }
	/** @return id jabatan fungsional dosen ini. */
	@Column(name = "id_jabfung", columnDefinition = "text") public String getIdJabfung() { return idJabfung; }
	/** @param v id jabatan fungsional baru. */
	public void setIdJabfung(String v) { this.idJabfung = v; }
	/** @return simpulan/penilaian akhir asesor atas laporan BKD ini. */
	@Column(name = "simpulan_asesor", columnDefinition = "text") public String getSimpulanAsesor() { return simpulanAsesor; }
	/** @param v simpulan asesor baru. */
	public void setSimpulanAsesor(String v) { this.simpulanAsesor = v; }
	/** @return representasi ringkas "id-kode" untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
