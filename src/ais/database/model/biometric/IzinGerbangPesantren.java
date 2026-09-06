package ais.database.model.biometric;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

/**
 * Izin keluar/masuk pondok pesantren yang diverifikasi di gerbang lewat pemindaian biometrik —
 * satu baris mewakili satu pengajuan izin santri (atau pihak lain di lingkungan pesantren) untuk
 * keluar area dan kembali, dari pengajuan ({@code status = "DIAJUKAN"}) hingga diproses/disetujui
 * petugas dan direalisasikan (dicatat waktu benar-benar keluar/kembali). Berbeda dari
 * {@link BiometricCredential}/{@link BiometricEvent} yang generik lintas domain, kelas ini
 * spesifik untuk konteks keselamatan santri (siapa yang keluar pondok, kapan, untuk tujuan apa,
 * didampingi siapa) — relasi ke verifikasi biometrik aktual ada lewat {@link #getEventKeluarId()}/
 * {@link #getEventKembaliId()} yang merujuk baris {@link BiometricEvent} saat gerbang benar-benar
 * discan (memverifikasi bahwa santri yang lewat gerbang memang subjek izin ini, bukan orang lain).
 *
 * <p>Berbeda dari {@link BiometricCredential}/{@link BiometricEvent} (sengaja tidak memakai
 * Envers agar material sensitif tidak tersalin ke tabel audit), kelas ini MEMAKAI
 * {@code @Audited} — konsisten dengan sifatnya sebagai data alur kerja/persetujuan (bukan
 * template biometrik mentah), sehingga riwayat perubahan izin (siapa memproses, kapan disetujui)
 * perlu dilacak sama seperti entitas alur kerja AIS lainnya.</p>
 *
 * <p>Constraint unik {@code uk_izin_gerbang_mutation} pada
 * ({@code requester_user_id}, {@code client_mutation_id}) menjadikan
 * {@link #getClientMutationId()} kunci idempotensi: percobaan pengajuan ganda (mis. retry
 * jaringan) oleh pemohon yang sama dengan ID mutasi yang identik ditolak basis data.</p>
 */
@Entity
@Audited
@Table(schema = "public", name = "izin_gerbang_pesantren", uniqueConstraints = @UniqueConstraint(
		name = "uk_izin_gerbang_mutation", columnNames = { "requester_user_id", "client_mutation_id" }))
public class IzinGerbangPesantren implements Serializable {
	private static final long serialVersionUID = 1L;
	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** ID santri/subjek yang mengajukan izin keluar-masuk ini. */
	private String subjectUserId;
	/** ID pengguna yang MENGAJUKAN permohonan (bisa berbeda dari subjek, mis. wali/pendamping mengajukan atas nama santri). */
	private String requesterUserId;
	/** ID mutasi buatan klien untuk mencegah pengajuan ganda (idempotency key) — lihat catatan constraint unik pada javadoc kelas. */
	private String clientMutationId;
	/** Alasan permohonan izin keluar. */
	private String alasan;
	/** Tujuan kepergian (lokasi/keperluan) selama izin. */
	private String tujuan;
	/** Nama pendamping santri selama izin (bila ada), untuk keperluan keselamatan. */
	private String pendamping;
	/** Waktu rencana keluar pondok yang diajukan. */
	private Date rencanaKeluar;
	/** Waktu rencana kembali ke pondok yang diajukan. */
	private Date rencanaKembali;
	/** Status alur izin ini; default {@code "DIAJUKAN"} saat pengajuan pertama, berubah seiring diproses petugas. */
	private String status = "DIAJUKAN";
	/** Identitas petugas yang memproses (menyetujui/menolak) pengajuan ini. */
	private String diprosesOleh;
	/** Catatan petugas terkait keputusan/proses pengajuan ini. */
	private String catatanPetugas;
	/** Waktu pengajuan ini diproses (disetujui/ditolak) oleh petugas. */
	private Date diprosesPada;
	/** Waktu santri benar-benar melewati gerbang keluar (diselaraskan dari verifikasi biometrik, lihat {@link #eventKeluarId}). */
	private Date keluarPada;
	/** Waktu santri benar-benar melewati gerbang kembali (diselaraskan dari verifikasi biometrik, lihat {@link #eventKembaliId}). */
	private Date kembaliPada;
	/** ID baris {@link BiometricEvent} yang merekam verifikasi biometrik saat santri keluar gerbang. */
	private Long eventKeluarId;
	/** ID baris {@link BiometricEvent} yang merekam verifikasi biometrik saat santri kembali ke gerbang. */
	private Long eventKembaliId;
	/** Status aktif baris izin ini; {@code null} diperlakukan sebagai AKTIF (lihat {@link #getAktif()} — berbeda dari {@link BiometricCredential#getActive()} yang fail-closed, di sini default-aktif seperti konvensi umum entitas AIS lain). */
	private Boolean aktif = Boolean.TRUE;
	/** Waktu baris ini dibuat; default waktu instansiasi. */
	private Date dibuatPada = new Date();
	/** Waktu baris ini terakhir diubah; default waktu instansiasi (perlu diset ulang manual saat update, tidak ada callback otomatis seperti entitas AIS lain yang memakai {@code onUpdate()}). */
	private Date diubahPada = new Date();

	/** @return ID baris (primary key). */
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param value ID baris (primary key) yang akan diset. */
	public void setId(Long value) { id = value; }
	/** @return ID santri/subjek yang mengajukan izin ini. */
	@Column(name = "subject_user_id", nullable = false, length = 255)
	public String getSubjectUserId() { return subjectUserId; }
	/** @param value ID subjek yang akan diset. */
	public void setSubjectUserId(String value) { subjectUserId = value; }
	/** @return ID pengguna yang mengajukan permohonan. */
	@Column(name = "requester_user_id", nullable = false, length = 255)
	public String getRequesterUserId() { return requesterUserId; }
	/** @param value ID pemohon yang akan diset. */
	public void setRequesterUserId(String value) { requesterUserId = value; }
	/** @return ID mutasi buatan klien (kunci idempotensi). */
	@Column(name = "client_mutation_id", nullable = false, length = 150)
	public String getClientMutationId() { return clientMutationId; }
	/** @param value ID mutasi klien yang akan diset. */
	public void setClientMutationId(String value) { clientMutationId = value; }
	/** @return alasan permohonan izin keluar. */
	@Column(name = "alasan", nullable = false, length = 500)
	public String getAlasan() { return alasan; }
	/** @param value alasan yang akan diset. */
	public void setAlasan(String value) { alasan = value; }
	/** @return tujuan kepergian selama izin. */
	@Column(name = "tujuan", nullable = false, length = 500)
	public String getTujuan() { return tujuan; }
	/** @param value tujuan yang akan diset. */
	public void setTujuan(String value) { tujuan = value; }
	/** @return nama pendamping santri selama izin, bila ada. */
	@Column(name = "pendamping", length = 255)
	public String getPendamping() { return pendamping; }
	/** @param value nama pendamping yang akan diset. */
	public void setPendamping(String value) { pendamping = value; }
	/** @return waktu rencana keluar pondok yang diajukan. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "rencana_keluar", nullable = false)
	public Date getRencanaKeluar() { return rencanaKeluar; }
	/** @param value waktu rencana keluar yang akan diset. */
	public void setRencanaKeluar(Date value) { rencanaKeluar = value; }
	/** @return waktu rencana kembali ke pondok yang diajukan. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "rencana_kembali", nullable = false)
	public Date getRencanaKembali() { return rencanaKembali; }
	/** @param value waktu rencana kembali yang akan diset. */
	public void setRencanaKembali(Date value) { rencanaKembali = value; }
	/** @return status alur izin ini (mis. "DIAJUKAN", status disetujui/ditolak sesuai alur kerja). */
	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status; }
	/** @param value status yang akan diset. */
	public void setStatus(String value) { status = value; }
	/** @return identitas petugas yang memproses pengajuan ini. */
	@Column(name = "diproses_oleh", length = 255)
	public String getDiprosesOleh() { return diprosesOleh; }
	/** @param value identitas petugas pemroses yang akan diset. */
	public void setDiprosesOleh(String value) { diprosesOleh = value; }
	/** @return catatan petugas terkait keputusan/proses pengajuan ini. */
	@Column(name = "catatan_petugas", length = 1000)
	public String getCatatanPetugas() { return catatanPetugas; }
	/** @param value catatan petugas yang akan diset. */
	public void setCatatanPetugas(String value) { catatanPetugas = value; }
	/** @return waktu pengajuan ini diproses oleh petugas. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "diproses_pada")
	public Date getDiprosesPada() { return diprosesPada; }
	/** @param value waktu diproses yang akan diset. */
	public void setDiprosesPada(Date value) { diprosesPada = value; }
	/** @return waktu santri benar-benar melewati gerbang keluar. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "keluar_pada")
	public Date getKeluarPada() { return keluarPada; }
	/** @param value waktu keluar gerbang yang akan diset. */
	public void setKeluarPada(Date value) { keluarPada = value; }
	/** @return waktu santri benar-benar melewati gerbang kembali. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "kembali_pada")
	public Date getKembaliPada() { return kembaliPada; }
	/** @param value waktu kembali gerbang yang akan diset. */
	public void setKembaliPada(Date value) { kembaliPada = value; }
	/** @return ID baris {@link BiometricEvent} yang merekam verifikasi biometrik saat keluar gerbang. */
	@Column(name = "event_keluar_id")
	public Long getEventKeluarId() { return eventKeluarId; }
	/** @param value ID event verifikasi keluar yang akan diset. */
	public void setEventKeluarId(Long value) { eventKeluarId = value; }
	/** @return ID baris {@link BiometricEvent} yang merekam verifikasi biometrik saat kembali ke gerbang. */
	@Column(name = "event_kembali_id")
	public Long getEventKembaliId() { return eventKembaliId; }
	/** @param value ID event verifikasi kembali yang akan diset. */
	public void setEventKembaliId(Long value) { eventKembaliId = value; }
	/** @return status aktif baris izin ini; {@code null} diperlakukan sebagai AKTIF ({@code true}). */
	@Column(name = "aktif", nullable = false)
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	/** @param value status aktif yang akan diset. */
	public void setAktif(Boolean value) { aktif = value; }
	/** @return waktu baris ini dibuat. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "dibuat_pada", nullable = false)
	public Date getDibuatPada() { return dibuatPada; }
	/** @param value waktu pembuatan yang akan diset. */
	public void setDibuatPada(Date value) { dibuatPada = value; }
	/** @return waktu baris ini terakhir diubah. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "diubah_pada", nullable = false)
	public Date getDiubahPada() { return diubahPada; }
	/** @param value waktu perubahan yang akan diset (harus diset manual oleh pemanggil, tidak ada callback {@code @PreUpdate} otomatis). */
	public void setDiubahPada(Date value) { diubahPada = value; }
}
