package ais.database.model.crm;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Aktivitas/tindak lanjut terjadwal pada satu {@link CrmLead} — mis. telepon, meeting, email,
 * tugas follow-up. Bentuk mengikuti pola {@code ais.database.model.spmi.TindakLanjutTemuanSPMI}
 * (target date + status + PIC), dengan PIC memakai FK terstruktur ke {@link Tbmuser} (bukan teks
 * bebas) supaya bisa dipakai untuk daftar "aktivitas jatuh tempo/terlambat milik saya".
 *
 * <p>Tabel {@code public.crm_activity} dibuat otomatis oleh {@code hbm2ddl=update} saat restart.</p>
 *
 * <p><b>Catatan kepemilikan/visibilitas:</b> pembacaan daftar aktivitas pada
 * {@code ais.common.newui.ticket.NewUiTicketController} (mis. blok yang membentuk JSON
 * {@code aktivitas} dari {@code activityRows}) hanya menyaring berdasar {@code lead} yang sedang
 * dibuka (bukan berdasar {@link #picUser} atau kepemilikan penyimpan) — siapa pun yang berhak
 * membuka satu {@link CrmLead} otomatis melihat SELURUH aktivitas pada lead tersebut, terlepas dari
 * siapa PIC-nya. Pola ini konsisten dengan {@link CrmCatatan} pada lead yang sama (lihat javadoc
 * di sana) dan dengan ketiadaan pemisahan tenant/satuan kerja pada {@link CrmLead} itu sendiri.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_activity")
public class CrmActivity extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815007L;

	public static final String JENIS_TELEPON = "TELEPON";
	public static final String JENIS_MEETING = "MEETING";
	public static final String JENIS_EMAIL = "EMAIL";
	public static final String JENIS_TUGAS = "TUGAS";
	public static final String JENIS_LAINNYA = "LAINNYA";

	/** Peta jenis aktivitas (kode &rarr; label bahasa manusia) untuk pilihan combo pada UI. */
	public static final Map<String, String> JENIS_DATA = new LinkedHashMap<String, String>();
	static {
		JENIS_DATA.put(JENIS_TELEPON, "Telepon");
		JENIS_DATA.put(JENIS_MEETING, "Meeting");
		JENIS_DATA.put(JENIS_EMAIL, "Email");
		JENIS_DATA.put(JENIS_TUGAS, "Tugas");
		JENIS_DATA.put(JENIS_LAINNYA, "Lainnya");
	}

	/** Status penyelesaian aktivitas: belum dimulai/dikerjakan, atau sudah selesai. */
	public static final String STATUS_BELUM_DIMULAI = "BELUM_DIMULAI";
	public static final String STATUS_SELESAI = "SELESAI";

	/** Primary key baris {@code crm_activity}. */
	private Long id;
	/** Lead tempat aktivitas ini terjadi/dijadwalkan. */
	private CrmLead lead;
	/** Jenis aktivitas; lihat konstanta {@code JENIS_*} dan {@link #getJenis()} untuk default-nya. */
	private String jenis;
	/** Isi/deskripsi bebas aktivitas (mis. ringkasan hasil telepon). */
	private String catatan;
	/** Tanggal target aktivitas ini seharusnya dikerjakan/selesai. */
	private Date targetDate;
	/** Tanggal aktivitas benar-benar ditandai selesai. */
	private Date tanggalSelesai;
	/** Status penyelesaian; lihat konstanta {@code STATUS_*} dan {@link #getStatus()} untuk default-nya. */
	private String status;
	/** Pengguna penanggung jawab (PIC) aktivitas ini. */
	private Tbmuser picUser;
	/** Flag aktif/nonaktif (soft delete); lihat {@link #getAktif()} untuk default-nya. */
	private Boolean aktif;
	/** Nama pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui otomatis lewat {@link #onUpdate()} saat update. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public CrmActivity() {
	}

	/**
	 * Constructor pintas yang langsung mengaitkan aktivitas ini ke sebuah {@link CrmLead}.
	 *
	 * @param lead lead pemilik aktivitas ini
	 */
	public CrmActivity(CrmLead lead) {
		this.lead = lead;
	}

	/**
	 * Mengembalikan primary key baris {@code crm_activity}.
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan lead tempat aktivitas ini terjadi/dijadwalkan, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} agar proxy lazy yang sudah detached tidak meledak.
	 * Kolom database {@code nullable = false} — setiap aktivitas wajib terkait satu lead.
	 *
	 * @return lead pemilik aktivitas ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lead", nullable = false)
	public CrmLead getLead() {
		lead = check(lead);
		return lead;
	}

	/**
	 * Menyetel lead pemilik aktivitas ini.
	 *
	 * @param lead lead baru
	 */
	public void setLead(CrmLead lead) {
		this.lead = lead;
	}

	/**
	 * Mengembalikan jenis aktivitas ({@link #JENIS_TELEPON}/{@link #JENIS_MEETING}/
	 * {@link #JENIS_EMAIL}/{@link #JENIS_TUGAS}/{@link #JENIS_LAINNYA}). Nilai {@code null}/kosong
	 * dianggap {@link #JENIS_LAINNYA} — aktivitas baru tanpa jenis eksplisit selalu tampil sebagai
	 * "Lainnya", bukan mengembalikan nilai kosong ke UI.
	 *
	 * @return jenis aktivitas, tidak pernah {@code null}/kosong
	 */
	@Column(name = "jenis", nullable = true, length = 32)
	public String getJenis() {
		return jenis == null || jenis.trim().isEmpty() ? JENIS_LAINNYA : jenis;
	}

	/**
	 * Menyetel jenis aktivitas. Tanpa validasi terhadap konstanta {@code JENIS_*}; pemanggil
	 * bertanggung jawab memakai nilai yang terdaftar di {@link #JENIS_DATA} agar label tampil
	 * dengan benar.
	 *
	 * @param jenis jenis aktivitas baru
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan isi/deskripsi bebas aktivitas (mis. ringkasan hasil telepon/meeting).
	 *
	 * @return catatan aktivitas
	 */
	@Column(name = "catatan", nullable = true, columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/**
	 * Menyetel isi/deskripsi bebas aktivitas.
	 *
	 * @param catatan catatan baru
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Mengembalikan tanggal target aktivitas ini seharusnya dikerjakan/selesai. Dipakai bersama
	 * {@link #getStatus()} untuk menandai aktivitas "terlambat" pada UI (target sudah lewat tapi
	 * status belum {@link #STATUS_SELESAI}) — logika penanda terlambat itu sendiri berada di kode
	 * pemanggil ({@code NewUiTicketController}), bukan di entity ini.
	 *
	 * @return tanggal target, atau {@code null} bila belum ditetapkan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "target_date", nullable = true)
	public Date getTargetDate() {
		return targetDate;
	}

	/**
	 * Menyetel tanggal target aktivitas.
	 *
	 * @param targetDate tanggal target baru
	 */
	public void setTargetDate(Date targetDate) {
		this.targetDate = targetDate;
	}

	/**
	 * Mengembalikan tanggal aktivitas benar-benar ditandai selesai.
	 *
	 * @return tanggal selesai, atau {@code null} bila belum selesai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_selesai", nullable = true)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Menyetel tanggal aktivitas selesai. Tidak diisi otomatis oleh entity ini saat
	 * {@link #status} diubah menjadi {@link #STATUS_SELESAI} — pengisian keduanya bersamaan adalah
	 * tanggung jawab kode pemanggil (aksi "Tandai Selesai" pada UI).
	 *
	 * @param tanggalSelesai tanggal selesai baru
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengembalikan status penyelesaian aktivitas ({@link #STATUS_BELUM_DIMULAI}/
	 * {@link #STATUS_SELESAI}). Nilai {@code null}/kosong dianggap {@link #STATUS_BELUM_DIMULAI} —
	 * aktivitas baru tanpa status eksplisit selalu tampil belum dimulai.
	 *
	 * @return status penyelesaian, tidak pernah {@code null}/kosong
	 */
	@Column(name = "status", nullable = true, length = 32)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_BELUM_DIMULAI : status;
	}

	/**
	 * Menyetel status penyelesaian. Tanpa validasi terhadap konstanta {@code STATUS_*}.
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan pengguna penanggung jawab (PIC) aktivitas ini, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Memakai FK terstruktur ke {@link Tbmuser} (bukan
	 * teks bebas nama PIC) supaya bisa dipakai untuk daftar "aktivitas jatuh tempo/terlambat milik
	 * saya" berdasar identitas pengguna yang login.
	 *
	 * @return pengguna PIC, atau {@code null} bila belum ditugaskan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pic_user", nullable = true)
	public Tbmuser getPicUser() {
		picUser = check(picUser);
		return picUser;
	}

	/**
	 * Menyetel pengguna PIC aktivitas ini.
	 *
	 * @param picUser pengguna PIC baru
	 */
	public void setPicUser(Tbmuser picUser) {
		this.picUser = picUser;
	}

	/**
	 * Mengembalikan status aktif/nonaktif (soft delete). Nilai {@code null} dianggap {@code true}.
	 *
	 * @return {@code true} bila aktif (default), {@code false} bila dinonaktifkan
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah entity ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
	 * atau string kosong/spasi diabaikan diam-diam sehingga jejak audit yang sudah terisi tidak
	 * bisa terhapus tanpa sengaja (lihat {@link GeneralValueObject#setOleh(String)}).
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah entity ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOleh(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilainya akan ditimpa otomatis
	 * oleh {@link #onUpdate()} pada jalur update Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Hook siklus hidup Hibernate {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum statement {@code UPDATE} dieksekusi untuk baris ini, mendelegasikan pembaruan stempel
	 * waktu ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi teks ringkas untuk logging/debugging: {@code "<id>-<jenis>"}.
	 *
	 * @return representasi teks entity ini
	 */
	public String toString() {
		return id + "-" + getJenis();
	}
}
