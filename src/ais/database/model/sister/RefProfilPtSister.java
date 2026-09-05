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
 * Entitas hasil sinkronisasi SISTER untuk endpoint referensi <b>{@code referensi/profil_pt}</b> (profil
 * perguruan tinggi: alamat, kontak, dasar hukum pendirian/izin operasional, dan status kelembagaan/kepemilikan).
 * <p>
 * Berbeda dari klaster {@code Ref*Sister} kode-referensi murni (lihat {@link RefAgamaSister} sebagai kelas
 * rujukan pola — mis. {@link RefNegaraSister}, {@link RefPerguruanTinggiSister} yang hanya menyimpan pasangan
 * kode+nama), kelas ini menyimpan <b>data profil institusi</b> — strukturnya lebih dekat dengan entitas data
 * individual seperti {@code RefMahasiswaPddiktiSister}/{@code RefSdmSister} — dengan field administratif umum
 * ({@link #id}, {@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}, {@link #kode}, {@link #keterangan},
 * {@link #aktif}) identik strukturnya dengan {@link RefAgamaSister} (lihat kelas tsb untuk penjelasan lengkap
 * pola field audit shadow dan flag aktif satu-arah), ditambah ~24 field spesifik profil perguruan tinggi.
 * Kelas ini TIDAK punya field {@code nama} generik seperti klaster kode-referensi murni — perannya digantikan
 * oleh {@link #namaPerguruanTinggi}.
 * <p>
 * <b>Bukan entitas yatim — AKTIF disinkronkan.</b> Dikonfirmasi lewat dua titik: {@link SisterEntitasRegistry}
 * memetakan {@code "referensi/profil_pt" -> RefProfilPtSister.class}, dan {@code DataSisterApi.daftarEndpoint()}
 * menambahkan {@code "referensi/profil_pt"} (tanpa parameter query) ke daftar endpoint yang ditarik tiap
 * sinkronisasi. Tanpa parameter ini berbeda maknanya dari {@code "referensi/perguruan_tinggi"} (juga tanpa
 * parameter, tapi mengembalikan daftar SEMUA PT nasional): pola endpoint tanpa-parameter yang mengembalikan
 * data terikat token API institusi pemanggil sudah terlihat di endpoint sejenis (mis. {@code referensi/sdm}),
 * sehingga tabel ini secara praktik kemungkinan hanya berisi profil PT pemilik instalasi AIS ini, bukan daftar
 * PT nasional — namun ini belum diverifikasi lewat isi tabel produksi (di luar jangkauan audit kode statis).
 * Endpoint ini juga dipetakan ke kategori radar chart dasbor sinkronisasi "Wilayah &amp; Institusi"
 * ({@code DataSisterAction.kategoriTabel}, {@code DasborSisterUiHelper}) bersama {@code perguruan_tinggi},
 * {@code wilayah}, {@code unit_kerja}, {@code dudi}, dan {@code bidang_usaha}.
 * <p>
 * Kelas ini {@code @Audited} (Hibernate Envers) — setiap INSERT/UPDATE dicatat otomatis ke tabel bayangan
 * {@code sister_ref_profil_pt_AUD} (dibuat otomatis oleh {@code hbm2ddl}, tak perlu migrasi manual). Skema
 * tabel: {@code public.sister_ref_profil_pt}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_profil_pt")
public class RefProfilPtSister extends GeneralValueObject {
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
	/** Kode/NPSN perguruan tinggi menurut SISTER (field JSON {@code "kode_perguruan_tinggi"}). */
	private String kodePerguruanTinggi;
	/** Nama resmi perguruan tinggi (field JSON {@code "nama_perguruan_tinggi"}). */
	private String namaPerguruanTinggi;
	/** Nomor telepon institusi. */
	private String telepon;
	/** Nomor faksimile institusi. */
	private String faximile;
	/** Alamat surel resmi institusi. */
	private String email;
	/** Alamat situs web institusi. */
	private String website;
	/** Nama jalan pada alamat institusi. */
	private String jalan;
	/** Nama dusun pada alamat institusi. */
	private String dusun;
	/** Nomor RT (rukun tetangga) pada alamat institusi. */
	private Integer rt;
	/** Nomor RW (rukun warga) pada alamat institusi. */
	private Integer rw;
	/** Nama kelurahan/desa pada alamat institusi. */
	private String kelurahan;
	/** Kode pos alamat institusi. */
	private String kodePos;
	/** Id wilayah (kode referensi SISTER) tempat institusi berada — lihat klaster {@code RefWilayahSister}. */
	private String idWilayah;
	/** Nama wilayah tempat institusi berada, salinan tampilan dari {@link #idWilayah}. */
	private String namaWilayah;
	/** Koordinat garis lintang lokasi institusi. */
	private Double lintang;
	/** Koordinat garis bujur lokasi institusi. */
	private Double bujur;
	/** Nomor SK (surat keputusan) pendirian perguruan tinggi. */
	private String skPendirian;
	/** Tanggal SK pendirian, sebagaimana dikirim SISTER (String mentah, bukan {@link Date} — lihat {@link #getTanggalSkPendirian()}). */
	private String tanggalSkPendirian;
	/** Id status kepemilikan institusi (kode referensi SISTER, mis. negeri/swasta) — lihat klaster status milik SISTER. */
	private String idStatusMilik;
	/** Nama status kepemilikan institusi, salinan tampilan dari {@link #idStatusMilik}. */
	private String namaStatusMilik;
	/** Status kelembagaan perguruan tinggi saat ini (mis. aktif/tutup/bergabung, sesuai kode SISTER). */
	private String statusPerguruanTinggi;
	/** Nomor SK izin operasional perguruan tinggi. */
	private String skIzinOperasional;
	/** Tanggal SK izin operasional, sebagaimana dikirim SISTER (String mentah, bukan {@link Date}). */
	private String tanggalIzinOperasional;

	/** Constructor default (kontrak JPA/Hibernate — instansiasi via refleksi). */
	public RefProfilPtSister() {}
	/** @return PK lokal (identity DB), bukan kode SISTER. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/** @param id PK lokal; normalnya diisi Hibernate saat insert, bukan diset manual. */
	public void setId(Long id) { this.id = id; }
	/** @return id aktor pengubah terakhir. */
	public String getOlehId() { return olehId; }
	/**
	 * Menetapkan id aktor pengubah. Mengabaikan diam-diam nilai null/kosong (nilai lama dipertahankan) —
	 * pola guard field audit shadow yang berulang di seluruh klaster entitas SISTER (lihat {@link RefAgamaSister}), BUKAN bug.
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
	/** @return kode/NPSN perguruan tinggi. */
	@Column(name = "kode_perguruan_tinggi", columnDefinition = "text") public String getKodePerguruanTinggi() { return kodePerguruanTinggi; }
	/** @param v kode/NPSN perguruan tinggi baru. */
	public void setKodePerguruanTinggi(String v) { this.kodePerguruanTinggi = v; }
	/** @return nama resmi perguruan tinggi. */
	@Column(name = "nama_perguruan_tinggi", columnDefinition = "text") public String getNamaPerguruanTinggi() { return namaPerguruanTinggi; }
	/** @param v nama resmi perguruan tinggi baru. */
	public void setNamaPerguruanTinggi(String v) { this.namaPerguruanTinggi = v; }
	/** @return nomor telepon institusi. */
	@Column(name = "telepon", columnDefinition = "text") public String getTelepon() { return telepon; }
	/** @param v nomor telepon institusi baru. */
	public void setTelepon(String v) { this.telepon = v; }
	/** @return nomor faksimile institusi. */
	@Column(name = "faximile", columnDefinition = "text") public String getFaximile() { return faximile; }
	/** @param v nomor faksimile institusi baru. */
	public void setFaximile(String v) { this.faximile = v; }
	/** @return alamat surel resmi institusi. */
	@Column(name = "email", columnDefinition = "text") public String getEmail() { return email; }
	/** @param v alamat surel resmi institusi baru. */
	public void setEmail(String v) { this.email = v; }
	/** @return alamat situs web institusi. */
	@Column(name = "website", columnDefinition = "text") public String getWebsite() { return website; }
	/** @param v alamat situs web institusi baru. */
	public void setWebsite(String v) { this.website = v; }
	/** @return nama jalan pada alamat institusi. */
	@Column(name = "jalan", columnDefinition = "text") public String getJalan() { return jalan; }
	/** @param v nama jalan baru. */
	public void setJalan(String v) { this.jalan = v; }
	/** @return nama dusun pada alamat institusi. */
	@Column(name = "dusun", columnDefinition = "text") public String getDusun() { return dusun; }
	/** @param v nama dusun baru. */
	public void setDusun(String v) { this.dusun = v; }
	/** @return nomor RT pada alamat institusi. */
	@Column(name = "rt") public Integer getRt() { return rt; }
	/** @param v nomor RT baru. */
	public void setRt(Integer v) { this.rt = v; }
	/** @return nomor RW pada alamat institusi. */
	@Column(name = "rw") public Integer getRw() { return rw; }
	/** @param v nomor RW baru. */
	public void setRw(Integer v) { this.rw = v; }
	/** @return nama kelurahan/desa pada alamat institusi. */
	@Column(name = "kelurahan", columnDefinition = "text") public String getKelurahan() { return kelurahan; }
	/** @param v nama kelurahan/desa baru. */
	public void setKelurahan(String v) { this.kelurahan = v; }
	/** @return kode pos alamat institusi. */
	@Column(name = "kode_pos", columnDefinition = "text") public String getKodePos() { return kodePos; }
	/** @param v kode pos baru. */
	public void setKodePos(String v) { this.kodePos = v; }
	/** @return id wilayah (kode referensi SISTER) tempat institusi berada. */
	@Column(name = "id_wilayah", columnDefinition = "text") public String getIdWilayah() { return idWilayah; }
	/** @param v id wilayah baru. */
	public void setIdWilayah(String v) { this.idWilayah = v; }
	/** @return nama wilayah tempat institusi berada. */
	@Column(name = "nama_wilayah", columnDefinition = "text") public String getNamaWilayah() { return namaWilayah; }
	/** @param v nama wilayah baru. */
	public void setNamaWilayah(String v) { this.namaWilayah = v; }
	/** @return koordinat garis lintang lokasi institusi. */
	@Column(name = "lintang") public Double getLintang() { return lintang; }
	/** @param v koordinat garis lintang baru. */
	public void setLintang(Double v) { this.lintang = v; }
	/** @return koordinat garis bujur lokasi institusi. */
	@Column(name = "bujur") public Double getBujur() { return bujur; }
	/** @param v koordinat garis bujur baru. */
	public void setBujur(Double v) { this.bujur = v; }
	/** @return nomor SK pendirian perguruan tinggi. */
	@Column(name = "sk_pendirian", columnDefinition = "text") public String getSkPendirian() { return skPendirian; }
	/** @param v nomor SK pendirian baru. */
	public void setSkPendirian(String v) { this.skPendirian = v; }
	/** @return tanggal SK pendirian (String mentah sebagaimana dikirim SISTER, bukan {@link Date}). */
	@Column(name = "tanggal_sk_pendirian", columnDefinition = "text") public String getTanggalSkPendirian() { return tanggalSkPendirian; }
	/** @param v tanggal SK pendirian baru (String mentah). */
	public void setTanggalSkPendirian(String v) { this.tanggalSkPendirian = v; }
	/** @return id status kepemilikan institusi (kode referensi SISTER). */
	@Column(name = "id_status_milik", columnDefinition = "text") public String getIdStatusMilik() { return idStatusMilik; }
	/** @param v id status kepemilikan baru. */
	public void setIdStatusMilik(String v) { this.idStatusMilik = v; }
	/** @return nama status kepemilikan institusi. */
	@Column(name = "nama_status_milik", columnDefinition = "text") public String getNamaStatusMilik() { return namaStatusMilik; }
	/** @param v nama status kepemilikan baru. */
	public void setNamaStatusMilik(String v) { this.namaStatusMilik = v; }
	/** @return status kelembagaan perguruan tinggi saat ini. */
	@Column(name = "status_perguruan_tinggi", columnDefinition = "text") public String getStatusPerguruanTinggi() { return statusPerguruanTinggi; }
	/** @param v status kelembagaan baru. */
	public void setStatusPerguruanTinggi(String v) { this.statusPerguruanTinggi = v; }
	/** @return nomor SK izin operasional perguruan tinggi. */
	@Column(name = "sk_izin_operasional", columnDefinition = "text") public String getSkIzinOperasional() { return skIzinOperasional; }
	/** @param v nomor SK izin operasional baru. */
	public void setSkIzinOperasional(String v) { this.skIzinOperasional = v; }
	/** @return tanggal SK izin operasional (String mentah sebagaimana dikirim SISTER, bukan {@link Date}). */
	@Column(name = "tanggal_izin_operasional", columnDefinition = "text") public String getTanggalIzinOperasional() { return tanggalIzinOperasional; }
	/** @param v tanggal SK izin operasional baru (String mentah). */
	public void setTanggalIzinOperasional(String v) { this.tanggalIzinOperasional = v; }
	/** @return representasi ringkas {@code id-kode} untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
