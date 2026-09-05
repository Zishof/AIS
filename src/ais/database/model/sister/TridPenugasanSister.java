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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>trid/penugasan</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl).
 *
 * <p>Memetakan tabel fisik {@code sister_trid_penugasan}, terdaftar pada {@link SisterEntitasRegistry}
 * dengan kunci {@code "penugasan"}. Bentuk &amp; semantik field administratif (id/oleh/olehId/
 * tanggal_dirubah/kode/keterangan/aktif/idSdm) identik dengan {@link TridBahanAjarSister}, kelas rujukan klaster
 * {@code Trid*Sister} bagian 2 — lihat javadoc di sana untuk penjelasan lengkap pola tersebut. Field spesifik-domain
 * didokumentasikan di bawah.</p>
 *
 * @see ais.database.model.sister.TridBahanAjarSister
 * @see ais.database.model.sister.SisterEntitasRegistry
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_penugasan")
public class TridPenugasanSister extends GeneralValueObject {
	/** Versi serialisasi tetap; lihat {@link TridBahanAjarSister#serialVersionUID}. */
	private static final long serialVersionUID = 1L;
	/** Primary key auto-increment; lihat {@link TridBahanAjarSister#getId()}. */
	private Long id;
	/** Pemicu simpan (nama pengguna); lihat {@link TridBahanAjarSister#getOleh()}. */
	private String oleh;
	/** Pemicu simpan (id pengguna); lihat {@link TridBahanAjarSister#getOlehId()}. */
	private String olehId;
	/** Timestamp perubahan terakhir; lihat {@link TridBahanAjarSister#getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Id item sisi SISTER (kunci upsert); lihat {@link TridBahanAjarSister#getKode()}. */
	private String kode;
	/** Payload JSON mentah dari SISTER; lihat {@link TridBahanAjarSister#getKeterangan()}. */
	private String keterangan;
	/** Penanda aktif/nonaktif; lihat {@link TridBahanAjarSister#getAktif()}. */
	private Boolean aktif;
	/** Id SDM (dosen) pemilik item; lihat {@link TridBahanAjarSister#getIdSdm()}. */
	private String idSdm;
	/** Status kepegawaian dosen (mis. PNS, tetap, kontrak) sesuai referensi SISTER. */
	private String statusKepegawaian;
	/** Jenis ikatan kerja dosen sesuai referensi SISTER. */
	private String ikatanKerja;
	/** Unit kerja penugasan dosen. */
	private String unitKerja;
	/** Jenjang pendidikan terakhir dosen pada saat penugasan. */
	private String jenjangPendidikan;
	/** Nama perguruan tinggi tempat penugasan. */
	private String perguruanTinggi;
	/** Tanggal mulai penugasan, teks mentah dari SISTER. */
	private String tanggalMulai;
	/** Tanggal keluar/berakhirnya penugasan, teks mentah dari SISTER (kosong bila masih aktif). */
	private String tanggalKeluar;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TridPenugasanSister() {}
	/** @return {@link #id}. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/** @param id nilai {@link #id} baru. */
	public void setId(Long id) { this.id = id; }
	/** @return {@link #olehId}. */
	public String getOlehId() { return olehId; }
	/** Lihat {@link TridBahanAjarSister#setOlehId(String)} (no-op saat null/kosong). @param olehId id pengguna pemicu simpan. */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	/** @return {@link #oleh}. */
	public String getOleh() { return oleh; }
	/** Lihat {@link TridBahanAjarSister#setOleh(String)} (no-op saat null/kosong). @param oleh nama pengguna pemicu simpan. */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** Lihat {@link TridBahanAjarSister#onUpdate()}. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	/** @return {@link #tanggal_dirubah}. */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param t nilai {@link #tanggal_dirubah} baru. */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	/** @return {@link #kode} setelah dinormalkan; lihat {@link TridBahanAjarSister#getKode()}. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode nilai {@link #kode} baru. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return {@link #keterangan}. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k nilai {@link #keterangan} baru. */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return {@link #aktif}, default {@code true} bila null; lihat {@link TridBahanAjarSister#getAktif()}. */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a nilai {@link #aktif} baru. */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return {@link #idSdm}. */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }
	/** @param v nilai {@link #idSdm} baru. */
	public void setIdSdm(String v) { this.idSdm = v; }
	/** @return {@link #statusKepegawaian}. */
	@Column(name = "status_kepegawaian", columnDefinition = "text") public String getStatusKepegawaian() { return statusKepegawaian; }
	/** @param v nilai {@link #statusKepegawaian} baru. */
	public void setStatusKepegawaian(String v) { this.statusKepegawaian = v; }
	/** @return {@link #ikatanKerja}. */
	@Column(name = "ikatan_kerja", columnDefinition = "text") public String getIkatanKerja() { return ikatanKerja; }
	/** @param v nilai {@link #ikatanKerja} baru. */
	public void setIkatanKerja(String v) { this.ikatanKerja = v; }
	/** @return {@link #unitKerja}. */
	@Column(name = "unit_kerja", columnDefinition = "text") public String getUnitKerja() { return unitKerja; }
	/** @param v nilai {@link #unitKerja} baru. */
	public void setUnitKerja(String v) { this.unitKerja = v; }
	/** @return {@link #jenjangPendidikan}. */
	@Column(name = "jenjang_pendidikan", columnDefinition = "text") public String getJenjangPendidikan() { return jenjangPendidikan; }
	/** @param v nilai {@link #jenjangPendidikan} baru. */
	public void setJenjangPendidikan(String v) { this.jenjangPendidikan = v; }
	/** @return {@link #perguruanTinggi}. */
	@Column(name = "perguruan_tinggi", columnDefinition = "text") public String getPerguruanTinggi() { return perguruanTinggi; }
	/** @param v nilai {@link #perguruanTinggi} baru. */
	public void setPerguruanTinggi(String v) { this.perguruanTinggi = v; }
	/** @return {@link #tanggalMulai}. */
	@Column(name = "tanggal_mulai", columnDefinition = "text") public String getTanggalMulai() { return tanggalMulai; }
	/** @param v nilai {@link #tanggalMulai} baru. */
	public void setTanggalMulai(String v) { this.tanggalMulai = v; }
	/** @return {@link #tanggalKeluar}. */
	@Column(name = "tanggal_keluar", columnDefinition = "text") public String getTanggalKeluar() { return tanggalKeluar; }
	/** @param v nilai {@link #tanggalKeluar} baru. */
	public void setTanggalKeluar(String v) { this.tanggalKeluar = v; }
	/** @return representasi ringkas {@code "<id>-<kode>"}; lihat {@link TridBahanAjarSister#toString()}. */
	@Override public String toString() { return id + "-" + kode; }
}
