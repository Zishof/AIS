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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>trid/sertifikasi_profesi</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl).
 *
 * <p>Memetakan tabel fisik {@code sister_trid_sertifikasi_profesi}, terdaftar pada {@link SisterEntitasRegistry}
 * dengan kunci {@code "sertifikasi_profesi"}. Bentuk &amp; semantik field administratif (id/oleh/olehId/
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
@Table(schema = "public", name = "sister_trid_sertifikasi_profesi")
public class TridSertifikasiProfesiSister extends GeneralValueObject {
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
	/** Jenis sertifikasi profesi sesuai referensi SISTER. */
	private String jenisSertifikasi;
	/** Bidang studi/keahlian terkait sertifikasi. */
	private String bidangStudi;
	/** Nomor SK sertifikasi. */
	private String skSertifikasi;
	/** Id lembaga penyelenggara sertifikasi pada referensi SISTER (bukan FK terkelola Hibernate). */
	private Integer idLembagaSertifikasi;
	/** Nama lembaga penyelenggara sertifikasi. */
	private String namaLembagaSertifikasi;
	/** Tanggal mulai berlaku sertifikasi, teks mentah dari SISTER. */
	private String terhitungMulaiTanggal;
	/** Tanggal berakhir berlaku sertifikasi, teks mentah dari SISTER. */
	private String terhitungSampaiTanggal;
	/** Nomor registrasi sertifikasi. */
	private String nomorRegistrasi;
	/** Tahun perolehan sertifikasi. */
	private Integer tahunSertifikasi;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TridSertifikasiProfesiSister() {}
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
	/** @return {@link #jenisSertifikasi}. */
	@Column(name = "jenis_sertifikasi", columnDefinition = "text") public String getJenisSertifikasi() { return jenisSertifikasi; }
	/** @param v nilai {@link #jenisSertifikasi} baru. */
	public void setJenisSertifikasi(String v) { this.jenisSertifikasi = v; }
	/** @return {@link #bidangStudi}. */
	@Column(name = "bidang_studi", columnDefinition = "text") public String getBidangStudi() { return bidangStudi; }
	/** @param v nilai {@link #bidangStudi} baru. */
	public void setBidangStudi(String v) { this.bidangStudi = v; }
	/** @return {@link #skSertifikasi}. */
	@Column(name = "sk_sertifikasi", columnDefinition = "text") public String getSkSertifikasi() { return skSertifikasi; }
	/** @param v nilai {@link #skSertifikasi} baru. */
	public void setSkSertifikasi(String v) { this.skSertifikasi = v; }
	/** @return {@link #idLembagaSertifikasi}. */
	@Column(name = "id_lembaga_sertifikasi") public Integer getIdLembagaSertifikasi() { return idLembagaSertifikasi; }
	/** @param v nilai {@link #idLembagaSertifikasi} baru. */
	public void setIdLembagaSertifikasi(Integer v) { this.idLembagaSertifikasi = v; }
	/** @return {@link #namaLembagaSertifikasi}. */
	@Column(name = "nama_lembaga_sertifikasi", columnDefinition = "text") public String getNamaLembagaSertifikasi() { return namaLembagaSertifikasi; }
	/** @param v nilai {@link #namaLembagaSertifikasi} baru. */
	public void setNamaLembagaSertifikasi(String v) { this.namaLembagaSertifikasi = v; }
	/** @return {@link #terhitungMulaiTanggal}. */
	@Column(name = "terhitung_mulai_tanggal", columnDefinition = "text") public String getTerhitungMulaiTanggal() { return terhitungMulaiTanggal; }
	/** @param v nilai {@link #terhitungMulaiTanggal} baru. */
	public void setTerhitungMulaiTanggal(String v) { this.terhitungMulaiTanggal = v; }
	/** @return {@link #terhitungSampaiTanggal}. */
	@Column(name = "terhitung_sampai_tanggal", columnDefinition = "text") public String getTerhitungSampaiTanggal() { return terhitungSampaiTanggal; }
	/** @param v nilai {@link #terhitungSampaiTanggal} baru. */
	public void setTerhitungSampaiTanggal(String v) { this.terhitungSampaiTanggal = v; }
	/** @return {@link #nomorRegistrasi}. */
	@Column(name = "nomor_registrasi", columnDefinition = "text") public String getNomorRegistrasi() { return nomorRegistrasi; }
	/** @param v nilai {@link #nomorRegistrasi} baru. */
	public void setNomorRegistrasi(String v) { this.nomorRegistrasi = v; }
	/** @return {@link #tahunSertifikasi}. */
	@Column(name = "tahun_sertifikasi") public Integer getTahunSertifikasi() { return tahunSertifikasi; }
	/** @param v nilai {@link #tahunSertifikasi} baru. */
	public void setTahunSertifikasi(Integer v) { this.tahunSertifikasi = v; }
	/** @return representasi ringkas {@code "<id>-<kode>"}; lihat {@link TridBahanAjarSister#toString()}. */
	@Override public String toString() { return id + "-" + kode; }
}
