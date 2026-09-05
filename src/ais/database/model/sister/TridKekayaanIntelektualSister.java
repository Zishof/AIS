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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>trid/kekayaan_intelektual</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl).
 *
 * <p>Memetakan tabel fisik {@code sister_trid_kekayaan_intelektual}, terdaftar pada {@link SisterEntitasRegistry}
 * dengan kunci {@code "kekayaan_intelektual"}. Bentuk &amp; semantik field administratif (id/oleh/olehId/
 * tanggal_dirubah/kode/keterangan/aktif/idSdm) identik dengan {@link TridBahanAjarSister}, kelas rujukan klaster
 * {@code Trid*Sister} bagian 2 — lihat javadoc di sana untuk penjelasan lengkap pola tersebut.</p>
 *
 * <p><b>Catatan pola:</b> field spesifik-domain kelas ini ({@code kategoriKegiatan/judul/quartile/bidangKeilmuan/
 * jenisPublikasi/tanggal}) sama persis namanya dengan {@link TridPublikasiSister} (endpoint publikasi) minus
 * {@code asalData} — indikasi kuat kelas ini disalin dari {@code TridPublikasiSister} lalu disesuaikan endpoint,
 * bukan didesain ulang untuk semantik kekayaan intelektual (mis. {@code quartile}/{@code jenisPublikasi} adalah
 * konsep spesifik jurnal ilmiah yang kurang relevan untuk paten/hak cipta). Dibiarkan apa adanya karena mengikuti
 * struktur payload yang benar-benar dikirim oleh endpoint SISTER, bukan bug fungsional.</p>
 *
 * @see ais.database.model.sister.TridBahanAjarSister
 * @see ais.database.model.sister.TridPublikasiSister
 * @see ais.database.model.sister.SisterEntitasRegistry
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_kekayaan_intelektual")
public class TridKekayaanIntelektualSister extends GeneralValueObject {
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
	/** Kategori kegiatan kekayaan intelektual sesuai referensi SISTER. */
	private String kategoriKegiatan;
	/** Judul karya kekayaan intelektual (mis. paten, hak cipta). */
	private String judul;
	/** Kolom quartile, diwarisi apa adanya dari struktur field yang sama dengan {@link TridPublikasiSister#getQuartile()}; lihat catatan pola pada javadoc kelas ini soal kemungkinan kurang relevannya konsep ini untuk kekayaan intelektual. */
	private Integer quartile;
	/** Bidang keilmuan terkait karya. */
	private String bidangKeilmuan;
	/** Nama kolom mengikuti struktur {@link TridPublikasiSister#getJenisPublikasi()}; pada endpoint ini berisi jenis/kategori kekayaan intelektual sesuai payload SISTER. */
	private String jenisPublikasi;
	/** Tanggal terkait karya (mis. tanggal pendaftaran/terbit), teks mentah dari SISTER. */
	private String tanggal;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TridKekayaanIntelektualSister() {}
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
	/** @return {@link #kategoriKegiatan}. */
	@Column(name = "kategori_kegiatan", columnDefinition = "text") public String getKategoriKegiatan() { return kategoriKegiatan; }
	/** @param v nilai {@link #kategoriKegiatan} baru. */
	public void setKategoriKegiatan(String v) { this.kategoriKegiatan = v; }
	/** @return {@link #judul}. */
	@Column(name = "judul", columnDefinition = "text") public String getJudul() { return judul; }
	/** @param v nilai {@link #judul} baru. */
	public void setJudul(String v) { this.judul = v; }
	/** @return {@link #quartile}. */
	@Column(name = "quartile") public Integer getQuartile() { return quartile; }
	/** @param v nilai {@link #quartile} baru. */
	public void setQuartile(Integer v) { this.quartile = v; }
	/** @return {@link #bidangKeilmuan}. */
	@Column(name = "bidang_keilmuan", columnDefinition = "text") public String getBidangKeilmuan() { return bidangKeilmuan; }
	/** @param v nilai {@link #bidangKeilmuan} baru. */
	public void setBidangKeilmuan(String v) { this.bidangKeilmuan = v; }
	/** @return {@link #jenisPublikasi}. */
	@Column(name = "jenis_publikasi", columnDefinition = "text") public String getJenisPublikasi() { return jenisPublikasi; }
	/** @param v nilai {@link #jenisPublikasi} baru. */
	public void setJenisPublikasi(String v) { this.jenisPublikasi = v; }
	/** @return {@link #tanggal}. */
	@Column(name = "tanggal", columnDefinition = "text") public String getTanggal() { return tanggal; }
	/** @param v nilai {@link #tanggal} baru. */
	public void setTanggal(String v) { this.tanggal = v; }
	/** @return representasi ringkas {@code "<id>-<kode>"}; lihat {@link TridBahanAjarSister#toString()}. */
	@Override public String toString() { return id + "-" + kode; }
}
