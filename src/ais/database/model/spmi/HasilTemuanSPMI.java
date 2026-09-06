package ais.database.model.spmi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.CascadeType;
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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.hasil_temuan_spmi} pada
 * modul SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi.
 * Merepresentasikan satu <b>temuan audit mutu internal (AMI)</b> — hasil
 * pemeriksaan konkret atas satu {@link SkenarioSPMI} (skenario/langkah bukti)
 * dalam konteks satu sesi evaluasi ({@link HasilSPMI}), yakni fase
 * <i>Evaluasi</i> pada siklus PPEPP (Penetapan-Pelaksanaan-Evaluasi-
 * Pengendalian-Peningkatan).
 *
 * <p><b>Posisi dalam hierarki PPEPP:</b> ...{@link SkenarioSPMI} (skenario yang
 * diperiksa) + {@link HasilSPMI} (sesi/header evaluasi tempat temuan ini
 * dicatat) &rarr; <b>{@code HasilTemuanSPMI}</b> &rarr;
 * {@link TindakLanjutTemuanSPMI} (rencana/aksi perbaikan atas temuan ini,
 * fase <i>Pengendalian</i> PPEPP). Satu temuan dapat memiliki nol, satu, atau
 * banyak tindak lanjut (relasi anak tidak dipetakan langsung di sini; kueri
 * {@code TindakLanjutTemuanSPMI} dengan {@code hasilTemuanSPMI = ini}).</p>
 *
 * <p>Setiap temuan mencatat: {@link #getStatus() status ketidaksesuaian}
 * (Observasi/Ketidaksesuaian Mayor/Minor/Sesuai/Melebihi Standar — lihat
 * {@link #statusData}), bukti yang disampaikan auditee
 * ({@link #getBuktiAuditee()}) beserta kesiapannya ({@link #getStatusKesiapanBukti()}),
 * catatan auditee ({@link #getCatatanAuditee()}), dan rekomendasi auditor
 * ({@link #getRekomendasi()}). Kolom evaluasi tambahan ini (bukti, status
 * kesiapan bukti, catatan, rekomendasi) ditandai {@link NotAudited} — sengaja
 * dikecualikan dari histori Hibernate Envers karena ditambahkan belakangan
 * untuk menyesuaikan instrumen AMI 2026 (lihat log SVN r77047) dan bukan
 * bagian dari skema audit historis awal entitas ini.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "hasil_temuan_spmi")
public class HasilTemuanSPMI extends GeneralValueObject {

	/** Label tampilan untuk status {@link #O1} — pemeriksaan bersifat observasi/catatan, bukan ketidaksesuaian. */
	public static final String O = "Observasi";
	/** Kode singkat status "Observasi", dipakai sebagai key {@link #statusData} dan nilai kolom {@code status}. */
	public static final String O1 = "o";

	/** Label tampilan untuk status {@link #KTS_MYR1} — ketidaksesuaian berat yang mempengaruhi pencapaian standar secara signifikan. */
	public static final String KTS_MYR = "Ketidaksesuaian Mayor";
	/** Kode singkat status "Ketidaksesuaian Mayor", dipakai sebagai key {@link #statusData} dan nilai kolom {@code status}. */
	public static final String KTS_MYR1 = "KTS MYR";

	/** Label tampilan untuk status {@link #KTS_MNR1} — ketidaksesuaian ringan/administratif. */
	public static final String KTS_MNR = "Ketidaksesuaian Minor";
	/** Kode singkat status "Ketidaksesuaian Minor", dipakai sebagai key {@link #statusData} dan nilai kolom {@code status}. */
	public static final String KTS_MNR1 = "KTS MNR";

	/** Label tampilan untuk status {@link #S1} — butir/indikator terpenuhi sesuai standar. */
	public static final String S = "Sesuai";
	/** Kode singkat status "Sesuai", dipakai sebagai key {@link #statusData} dan nilai kolom {@code status}. */
	public static final String S1 = "S";

	/** Label tampilan untuk status {@link #LS1} — pencapaian melampaui standar minimum yang ditetapkan. */
	public static final String LS = "Melebihi Standar";
	/** Kode singkat status "Melebihi Standar", dipakai sebagai key {@link #statusData} dan nilai kolom {@code status}. */
	public static final String LS1 = "LS";

	/** Nilai {@link #getStatusKesiapanBukti()}: bukti yang diminta sudah tersedia lengkap dari auditee. */
	public static final String BUKTI_TERSEDIA = "Tersedia";
	/** Nilai {@link #getStatusKesiapanBukti()}: bukti baru tersedia sebagian. */
	public static final String BUKTI_SEBAGIAN = "Sebagian";
	/** Nilai {@link #getStatusKesiapanBukti()}: bukti belum disiapkan sama sekali oleh auditee. */
	public static final String BUKTI_BELUM_TERSEDIA = "Belum Tersedia";

	/**
	 * Peta kode singkat (key, mis. {@link #S1}) ke label tampilan (value, mis.
	 * {@link #S}) untuk status ketidaksesuaian temuan, diurutkan alfabetis
	 * ({@link TreeMap}). Dipakai untuk mengisi dropdown/combobox dan
	 * menerjemahkan kode tersimpan ({@code status}) menjadi label yang
	 * dipahami pengguna saat ditampilkan.
	 */
	public static final Map<String, String> statusData = new TreeMap<String, String>();
	/**
	 * Peta status kesiapan bukti (Tersedia/Sebagian/Belum Tersedia) — key dan
	 * value sengaja sama persis untuk masing-masing entri, dipakai untuk
	 * mengisi dropdown/combobox pada form entri temuan.
	 */
	public static final Map<String, String> statusKesiapanBuktiData = new TreeMap<String, String>();

	static {
		statusData.put(O1, O);
		statusData.put(KTS_MYR1, KTS_MYR);
		statusData.put(KTS_MNR1, KTS_MNR);
		statusData.put(S1, S);
		statusData.put(LS1, LS);

		statusKesiapanBuktiData.put(BUKTI_TERSEDIA, BUKTI_TERSEDIA);
		statusKesiapanBuktiData.put(BUKTI_SEBAGIAN, BUKTI_SEBAGIAN);
		statusKesiapanBuktiData.put(BUKTI_BELUM_TERSEDIA, BUKTI_BELUM_TERSEDIA);
	}

	/**
	 * Nomor versi serialisasi tetap untuk kontrak {@link java.io.Serializable}.
	 * Nilai literal ini disalin dari template hbm2java bersama entitas SPMI
	 * lain di paket ini (bukan dihitung ulang per kelas).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return nilai mentah kolom audit shadow {@code olehId} (identitas
	 *         pengguna yang terakhir menyimpan/mengubah baris ini), atau
	 *         {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna (kolom audit shadow {@code olehId}). Setter
	 * ini sengaja mengabaikan nilai {@code null} atau kosong (guard di baris
	 * pertama) — kebutuhan teknis (bukan bug): nilai yang sudah tercatat oleh
	 * interceptor audit tidak boleh tertimpa oleh panggilan berikutnya yang
	 * membawa nilai kosong/null.
	 *
	 * @param olehId identitas pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna (kolom audit shadow {@code oleh}), dengan guard
	 * yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat pada kolom audit shadow {@code oleh},
	 *         atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence
	 * sesaat sebelum baris ini di-{@code UPDATE}, mendelegasikan pencatatan
	 * timestamp perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 * Bukan API publik — tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp perubahan terakhir; biasanya diisi
	 *                        otomatis oleh {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir kali baris ini diubah, diinisialisasi ke
	 *         waktu saat objek dibuat dan diperbarui otomatis oleh
	 *         {@link #onUpdate()} saat baris diperbarui di database.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code id + "-" + nama}, dipakai
	 *         untuk log/debug dan tampilan singkat, bukan identitas bisnis.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private SkenarioSPMI skenarioSPMI;
	private HasilSPMI hasilSPMI;
	private String nama;
	private String status;
	private String keterangan;
	private String buktiAuditee;
	private String statusKesiapanBukti;
	private String catatanAuditee;
	private String rekomendasi;
	private Boolean aktif;

	/** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
	public HasilTemuanSPMI() {
	}

	/**
	 * Membuat temuan baru yang langsung terkait ke sebuah skenario pemeriksaan,
	 * tanpa mengaitkan sesi evaluasi ({@link HasilSPMI}) induknya. Dipakai bila
	 * konteks sesi evaluasi belum/tidak relevan pada titik pembuatan objek.
	 *
	 * @param skenarioSPMI skenario ({@link SkenarioSPMI}) yang diperiksa
	 */
	public HasilTemuanSPMI(SkenarioSPMI skenarioSPMI) {
		this.skenarioSPMI = skenarioSPMI;
	}

	/**
	 * Membuat temuan baru sekaligus mengaitkannya ke skenario yang diperiksa
	 * dan sesi evaluasi ({@link HasilSPMI}) tempat temuan ini dicatat.
	 *
	 * @param skenarioSPMI skenario ({@link SkenarioSPMI}) yang diperiksa
	 * @param hasilSPMI    sesi evaluasi ({@link HasilSPMI}) induk temuan ini
	 */
	public HasilTemuanSPMI(SkenarioSPMI skenarioSPMI, HasilSPMI hasilSPMI) {
		this.skenarioSPMI = skenarioSPMI;
		this.hasilSPMI = hasilSPMI;
	}

	/**
	 * @return primary key baris ini. Kolom {@code id} bertipe {@code IDENTITY}
	 *         (auto-increment oleh database) dan ditandai {@code insertable = false}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; jarang dipanggil manual karena {@code id} adalah IDENTITY. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama/uraian singkat temuan ini, di-{@code trim()} terlebih
	 *         dahulu; dikembalikan sebagai string kosong (bukan {@code null})
	 *         bila belum diisi — dipakai sebagai judul baris temuan pada
	 *         popup {@link ais.action.master.spmi.TindakLanjutSPMIAction}.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? "" : this.nama.trim();
	}

	/** @param nama nama/uraian singkat temuan; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan bagi temuan ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan; opsional. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Bukti atau tautan dokumen yang disampaikan auditee untuk indikator ini. */
	@NotAudited
	@Column(name = "bukti_auditee", nullable = true, columnDefinition = "text")
	public String getBuktiAuditee() {
		return buktiAuditee;
	}

	/** @param buktiAuditee bukti atau tautan dokumen dari auditee; opsional, lihat {@link #getBuktiAuditee()}. */
	public void setBuktiAuditee(String buktiAuditee) {
		this.buktiAuditee = buktiAuditee;
	}

	/** Status kesiapan bukti AMI: Tersedia, Sebagian, atau Belum Tersedia. */
	@NotAudited
	@Column(name = "status_kesiapan_bukti", nullable = true, length = 30)
	public String getStatusKesiapanBukti() {
		return statusKesiapanBukti == null || statusKesiapanBukti.trim().isEmpty()
				? null : statusKesiapanBukti.trim();
	}

	/**
	 * @param statusKesiapanBukti status kesiapan bukti; opsional, salah satu
	 *                            dari {@link #BUKTI_TERSEDIA}, {@link #BUKTI_SEBAGIAN},
	 *                            atau {@link #BUKTI_BELUM_TERSEDIA} — lihat
	 *                            {@link #getStatusKesiapanBukti()}.
	 */
	public void setStatusKesiapanBukti(String statusKesiapanBukti) {
		this.statusKesiapanBukti = statusKesiapanBukti;
	}

	/** @return catatan tambahan dari auditee terkait temuan ini; boleh {@code null}. */
	@NotAudited
	@Column(name = "catatan_auditee", nullable = true, columnDefinition = "text")
	public String getCatatanAuditee() {
		return catatanAuditee;
	}

	/** @param catatanAuditee catatan tambahan dari auditee; opsional. */
	public void setCatatanAuditee(String catatanAuditee) {
		this.catatanAuditee = catatanAuditee;
	}

	/** @return rekomendasi auditor atas temuan ini; boleh {@code null}. */
	@NotAudited
	@Column(name = "rekomendasi", nullable = true, columnDefinition = "text")
	public String getRekomendasi() {
		return rekomendasi;
	}

	/** @param rekomendasi rekomendasi auditor; opsional. */
	public void setRekomendasi(String rekomendasi) {
		this.rekomendasi = rekomendasi;
	}

	/** Pemetaan kompatibel ke skor biner instrumen AMI 2026. */
	@Transient
	public Integer getSkorAmi() {
		String nilai = getStatus();
		if (S1.equals(nilai) || LS1.equals(nilai)) return Integer.valueOf(1);
		if (O1.equals(nilai) || KTS_MYR1.equals(nilai) || KTS_MNR1.equals(nilai)) return Integer.valueOf(0);
		return null;
	}

	/**
	 * @return {@code true} bila temuan ini masih aktif/berlaku, {@code false}
	 *         bila dinonaktifkan (soft delete). Default {@code true} bila
	 *         kolom belum pernah diisi — pola flag aktif "default aman" yang
	 *         konsisten dengan entitas SPMI lain di paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif temuan ini; lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@link SkenarioSPMI} yang diperiksa untuk menghasilkan temuan
	 *         ini. Getter memanggil {@link #check(Object)} warisan dari
	 *         {@link GeneralValueObject} untuk menangani kemungkinan proxy
	 *         Hibernate yang stale/terputus dari session. Kolom
	 *         {@code skenario_spmi} wajib diisi ({@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skenario_spmi", nullable = false)
	public SkenarioSPMI getSkenarioSPMI() {
		skenarioSPMI = check(skenarioSPMI);
		return skenarioSPMI;
	}

	/**
	 * Menyimpan skenario yang diperiksa. Setter ini menolak nilai yang
	 * {@code null} atau belum ter-{@code persist} (belum punya {@code id}) —
	 * mencegah relasi menunjuk ke instance transient yang gagal disimpan.
	 *
	 * @param skenarioSPMI skenario yang diperiksa; diabaikan bila null atau belum punya id
	 */
	public void setSkenarioSPMI(SkenarioSPMI skenarioSPMI) {
		if (skenarioSPMI != null && skenarioSPMI.getId() != null) {
			this.skenarioSPMI = skenarioSPMI;
		}
	}

	/**
	 * @return kode status ketidaksesuaian temuan ini (salah satu dari
	 *         {@link #O1}, {@link #KTS_MYR1}, {@link #KTS_MNR1}, {@link #S1},
	 *         {@link #LS1} — lihat peta label {@link #statusData}),
	 *         di-{@code trim()}; {@code null} bila belum diisi atau kosong.
	 */
	@Column(name = "status")
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? null : status.trim();
	}

	/** @param status kode status ketidaksesuaian; lihat {@link #getStatus()}. */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return {@link HasilSPMI} — sesi/header evaluasi tempat temuan ini
	 *         dicatat. Getter memanggil {@link #check(Object)} warisan dari
	 *         {@link GeneralValueObject} untuk menangani kemungkinan proxy
	 *         Hibernate yang stale/terputus dari session. Kolom
	 *         {@code hasil_spmi} wajib diisi ({@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "hasil_spmi", nullable = false)
	public HasilSPMI getHasilSPMI() {
		hasilSPMI = check(hasilSPMI);
		return hasilSPMI;
	}

	/**
	 * Menyimpan sesi evaluasi induk. Setter ini menolak nilai yang
	 * {@code null} atau belum ter-{@code persist} (belum punya {@code id}) —
	 * mencegah relasi menunjuk ke instance transient yang gagal disimpan.
	 *
	 * @param hasilSPMI sesi evaluasi induk; diabaikan bila null atau belum punya id
	 */
	public void setHasilSPMI(HasilSPMI hasilSPMI) {
		if (hasilSPMI != null && hasilSPMI.getId() != null) {
			this.hasilSPMI = hasilSPMI;
		}
	}

}
