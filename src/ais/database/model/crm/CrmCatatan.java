package ais.database.model.crm;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

/**
 * Catatan / timeline pada satu {@link CrmLead} — riwayat komunikasi &amp; catatan internal tim
 * penjualan. Kloning pola {@code ais.database.model.ticket.TicketKomentar}.
 *
 * <p>Tabel {@code public.crm_catatan} dibuat otomatis oleh {@code hbm2ddl=update} saat restart.</p>
 *
 * <p><b>Verifikasi filter kepemilikan/visibilitas (dari kode).</b> Baik jalur baca lama
 * ({@code CrmPipelineHelper#muatCatatan(Component, CrmLead)}, kriteria
 * {@code Restrictions.eq("lead", lead)}) maupun jalur JSON baru
 * ({@code NewUiTicketController}, query {@code CrmCatatan} dengan kriteria yang sama) HANYA
 * menyaring berdasar {@link #lead} yang sedang dibuka — TIDAK ada filter tambahan berdasar
 * {@link #userId}/penulis catatan, tim penjualan, maupun satuan kerja. Konsekuensinya: setiap
 * pengguna yang berhak membuka satu {@link CrmLead} otomatis dapat membaca SELURUH catatan pada
 * lead tersebut, termasuk catatan yang ditulis anggota tim penjualan lain. Ini SAMA seperti
 * {@code TicketKomentar} pada satu {@code Ticket} (catatan bersifat "milik tim", bukan "milik
 * penulis") dan konsisten dengan ketiadaan pemisahan tenant/satuan kerja pada {@link CrmLead} itu
 * sendiri (lihat javadoc di sana) — bukan penyimpangan baru yang unik pada entity ini. Sepanjang
 * akses ke fitur CRM pada modul Ticketing memang dimaksudkan sebagai ruang kerja bersama tim
 * penjualan/marketing (bukan ruang privat per pengguna), perilaku ini sesuai desain; namun bila
 * institusi mengharapkan sebagian catatan bersifat rahasia antar anggota tim, TIDAK ADA mekanisme
 * di lapisan ini (field maupun query) yang mendukungnya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_catatan")
public class CrmCatatan extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815008L;

	/** Primary key baris {@code crm_catatan}. */
	private Long id;
	/** Lead tempat catatan ini ditulis. */
	private CrmLead lead;
	/** Isi bebas catatan/komunikasi. */
	private String isi;
	/** Id pengguna penulis catatan; teks bebas (bukan FK ke {@code Tbmuser}), diisi manual saat penyimpanan. */
	private String userId;
	/** Nama tampil penulis catatan pada saat penulisan; disalin sebagai teks, tidak mengikuti perubahan nama pengguna di kemudian hari. */
	private String nama;
	/** Tanggal catatan ditulis; default waktu instansiasi object. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Nama pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui otomatis lewat {@link #onUpdate()} saat update. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public CrmCatatan() {
	}

	/**
	 * Constructor pintas yang langsung mengaitkan catatan ini ke sebuah {@link CrmLead}.
	 *
	 * @param lead lead pemilik catatan ini
	 */
	public CrmCatatan(CrmLead lead) {
		this.lead = lead;
	}

	/**
	 * Mengembalikan primary key baris {@code crm_catatan}.
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
	 * Mengembalikan lead tempat catatan ini ditulis, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Kolom database mengizinkan {@code null} meski
	 * seluruh jalur pembuatan catatan yang ditemukan (constructor {@link #CrmCatatan(CrmLead)})
	 * selalu mengisinya.
	 *
	 * @return lead pemilik catatan ini, atau {@code null} bila tidak terkait lead mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lead", nullable = true)
	public CrmLead getLead() {
		lead = check(lead);
		return lead;
	}

	/**
	 * Menyetel lead pemilik catatan ini.
	 *
	 * @param lead lead baru
	 */
	public void setLead(CrmLead lead) {
		this.lead = lead;
	}

	/**
	 * Mengembalikan isi bebas catatan/komunikasi.
	 *
	 * @return isi catatan
	 */
	@Column(name = "isi", nullable = true, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/**
	 * Menyetel isi catatan.
	 *
	 * @param isi isi catatan baru
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Mengembalikan id pengguna penulis catatan. Disimpan sebagai teks bebas (kolom {@code String}),
	 * BUKAN foreign key ke {@code Tbmuser} — berbeda dari {@link CrmActivity#getPicUser()}/
	 * {@link CrmSalesTeamMember#getAnggota()} yang memakai relasi terstruktur. Konsekuensinya tidak
	 * ada constraint yang menjaga nilai ini tetap merujuk pengguna yang benar-benar ada, dan
	 * perubahan/penghapusan akun pengguna tidak memengaruhi catatan lama.
	 *
	 * @return id pengguna penulis, atau {@code null} bila tidak diisi
	 */
	@Column(name = "user_id", nullable = true, length = 128)
	public String getUserId() {
		return userId;
	}

	/**
	 * Menyetel id pengguna penulis catatan.
	 *
	 * @param userId id pengguna baru
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Mengembalikan nama tampil penulis catatan pada saat penulisan. Disalin sebagai teks statis
	 * (snapshot) saat catatan dibuat — bila nama pengguna berubah di kemudian hari (mis. ganti
	 * nama), catatan lama tetap menampilkan nama pada saat penulisan, tidak ikut berubah.
	 *
	 * @return nama penulis, atau {@code null} bila tidak diisi
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama tampil penulis catatan.
	 *
	 * @param nama nama penulis baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan tanggal catatan ditulis. Diinisialisasi ke waktu instansiasi object
	 * ({@code WaktuUtil.getDate()}), namun seluruh jalur pembuatan catatan yang ditemukan
	 * ({@code CrmPipelineHelper}/{@code NewUiTicketController}) tetap menyetelnya eksplisit lewat
	 * {@link #setTanggal(Date)} dengan {@code new Date()} saat penyimpanan.
	 *
	 * @return tanggal catatan ditulis
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel tanggal catatan ditulis.
	 *
	 * @param tanggal tanggal baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
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
	 * atau string kosong/spasi diabaikan diam-diam (lihat {@link GeneralValueObject#setOleh(String)}).
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
	 * Representasi teks ringkas untuk logging/debugging: {@code "<id>-<nama>"}, dengan nama
	 * dikosongkan (bukan tercetak {@code "null"}) bila {@link #nama} belum diisi.
	 *
	 * @return representasi teks entity ini
	 */
	public String toString() {
		return id + "-" + (nama == null ? "" : nama);
	}
}
