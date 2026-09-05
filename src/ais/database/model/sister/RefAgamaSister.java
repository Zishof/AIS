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
 * Entitas hasil sinkronisasi SISTER untuk endpoint referensi <b>{@code referensi/agama}</b> (daftar kode
 * agama/kepercayaan yang dipakai SISTER Kemdikbud/Kemdikbudristek pada profil dosen dan tenaga kependidikan).
 * Kelas ini adalah <b>referensi pola</b> bagi seluruh klaster {@code Ref*Sister} (37 kelas serupa di paket
 * {@code ais.database.model.sister}): 32 kelas murni memakai struktur identik dengan kelas ini —
 * {@link RefBidangStudiSister}, {@link RefBidangUsahaSister}, {@link RefDudiSister},
 * {@link RefGolonganPangkatSister}, {@link RefIkatanKerjaSister}, {@link RefJabatanFungsionalSister},
 * {@link RefJabatanNegaraSister}, {@link RefJabatanTugasTambahanSister}, {@link RefJenisBahanAjarSister},
 * {@link RefJenisBeasiswaSister}, {@link RefJenisDiklatSister}, {@link RefJenisDokumenSister},
 * {@link RefJenisKeluarSister}, {@link RefJenisKepanitiaanSister}, {@link RefJenisKesejahteraanSister},
 * {@link RefJenisPekerjaanSister}, {@link RefJenisPenghargaanSister}, {@link RefJenisPublikasiSister},
 * {@link RefJenisTesSister}, {@link RefJenisTunjanganSister}, {@link RefJenjangPendidikanSister},
 * {@link RefKategoriCapaianLuaranSister}, {@link RefKelompokBidangSister}, {@link RefMediaPublikasiSister},
 * {@link RefNegaraSister}, {@link RefPerguruanTinggiSister}, {@link RefSemesterSister},
 * {@link RefSkimKegiatanSister}, {@link RefStatusKepegawaianSister}, {@link RefSumberGajiSister},
 * {@link RefTingkatPenghargaanSister} — ditambah 3 varian dengan satu kolom tambahan
 * ({@link RefGelarAkademikSister}, {@link RefUnitKerjaSister}, {@link RefWilayahSister}) dan 2 varian data
 * individual (bukan kode referensi murni): {@link RefMahasiswaPddiktiSister} dan {@link RefSdmSister}.
 * <p>
 * <b>Bukan entitas yatim.</b> Berbeda dari 20+ entitas dorman yang ditemukan sepanjang inisiatif Javadoc ini,
 * klaster {@code Ref*Sister} AKTIF dipakai oleh mesin sinkronisasi nyata:
 * {@link ais.common.DataSisterApi#synDataSister()} menyusun daftar endpoint {@code referensi/*} lewat
 * {@code DataSisterApi.daftarEndpoint(...)}, menariknya dari API eksternal SISTER Kemdikbud (HTTP GET, respons
 * JSON array), lalu meng-upsert tiap baris ke tabel terstruktur yang dipetakan lewat
 * {@link SisterEntitasRegistry#kelas(String)} (peta {@code "referensi/agama" -> RefAgamaSister.class}, dst — 88
 * entri total, dihasilkan dari {@code api_spec.yaml}). Pemetaan JSON-ke-entitas TIDAK di-hardcode per kelas:
 * dilakukan oleh <b>mesin refleksi generik</b> di {@code DataSisterApi} (metode privat {@code pemetaRefleksi},
 * {@code isiEntitasRefleksi}, {@code panggilSetter}) yang mengiterasi tiap key JSON, mengonversi
 * {@code snake_case -> PascalCase} (mis. {@code bidang_studi -> BidangStudi}), lalu memanggil setter
 * {@code setXxx} yang cocok via {@code java.lang.reflect}, meng-coerce tipe (String/Integer/Long/Double/Boolean)
 * sesuai tipe parameter setter. Karena itu, penambahan field baru pada respons SISTER TIDAK memerlukan
 * perubahan kode mesin sinkronisasi — cukup tambah kolom+getter/setter pada entitas ini.
 * <p>
 * <b>Struktur kolom (identik di seluruh klaster 32-kelas murni):</b> {@link #id} adalah PK lokal (identity DB,
 * BUKAN id item SISTER); {@link #kode} menyimpan id item SISTER dari key JSON {@code "id"} dan menjadi
 * <b>kunci upsert</b> (baris existing dicari berdasarkan {@code kode}, bukan {@link #id}); {@link #keterangan}
 * adalah salinan JSON mentah utuh baris ini (arsip/cadangan, dipakai bila field terstruktur tak mencukupi);
 * {@link #aktif} mengikuti status aktif dari SISTER (getter memperlakukan {@code null} sebagai {@code true} —
 * pola default-aktif yang berulang di banyak entitas AIS); {@link #nama} adalah label tampilan. Field
 * {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah <b>field audit shadow</b> (KEHARUSAN TEKNIS,
 * bukan bug — pola berulang dicatat sepanjang inisiatif ini): setter {@link #setOleh}/{@link #setOlehId}
 * SENGAJA mengabaikan nilai null/kosong agar identitas pengubah terakhir tidak tertimpa oleh pemanggil yang
 * tak menyertakan info aktor (mis. thread sinkronisasi latar yang berjalan tanpa sesi pengguna interaktif).
 * <p>
 * Kelas ini {@code @Audited} (Hibernate Envers) — setiap INSERT/UPDATE dicatat otomatis ke tabel bayangan
 * {@code sister_ref_agama_AUD} (dibuat otomatis oleh {@code hbm2ddl}, tak perlu migrasi manual). Skema tabel:
 * {@code public.sister_ref_agama}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_agama")
public class RefAgamaSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	/** PK lokal (identity DB), BUKAN kode SISTER — lihat {@link #kode}. */
	private Long id;
	/** Nama aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOleh}). */
	private String oleh;
	/** ID aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOlehId}). */
	private String olehId;
	/** Timestamp perubahan terakhir; diinisialisasi saat objek dibuat, dimutakhirkan oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kode/id item SISTER (field JSON {@code "id"}) — kunci upsert sinkronisasi. */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk baris ini. */
	private String keterangan;
	/** Flag aktif dari SISTER; {@code null} diperlakukan aktif oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Nama tampilan item referensi (field JSON {@code "nama"}). */
	private String nama;

	/** Constructor default (kontrak JPA/Hibernate — instansiasi via refleksi). */
	public RefAgamaSister() {}
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
	/** @return nama tampilan item. */
	@Column(name = "nama", columnDefinition = "text") public String getNama() { return nama; }
	/** @param v nama tampilan baru. */
	public void setNama(String v) { this.nama = v; }
	/** @return representasi ringkas {@code id-kode} untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
