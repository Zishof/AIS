package ais.database.model;

// Tabel penampung (staging) sinkronisasi Neo Feeder PDDikti.

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

/**
 * <h3>NeoFeederSync — tabel penampung (staging) sinkronisasi Neo Feeder PDDikti</h3>
 *
 * <p>Tabel umum (general) yang menampung jejak sinkronisasi SEMUA entitas/tabel Neo
 * Feeder (Dosen, Mahasiswa, Matakuliah, Kurikulum, Nilai, dll) dalam satu tempat.
 * Tiap baris mewakili satu aksi/endpoint Neo Feeder (mis. {@code GetListDosen}) dan
 * menyimpan payload mentah permintaan &amp; tanggapan dalam format JSON pada kolom
 * {@code json_request} dan {@code json_response}, plus status apakah data sudah
 * tersinkron dengan data lokal.</p>
 *
 * <p><b>Cara pakai.</b> Setiap kali panel Neo Feeder di-<i>Refresh</i> (ambil dari
 * feeder) maupun saat <i>Kirim/Ambil</i> data ke/dari feeder, baris untuk aksi
 * tersebut di-<i>upsert</i> (lihat {@code NeoFeederSyncHelper}) sehingga
 * {@code json_request}, {@code json_response}, {@code status}, jumlah data, dan
 * {@code tanggal_sinkron} selalu mencerminkan operasi terakhir. Karena di-upsert per
 * aksi, tabel ini berperan sebagai <i>snapshot</i> status sinkronisasi terkini untuk
 * tiap entitas Neo Feeder.</p>
 *
 * <p><b>Status</b> ({@code status}) memakai konstanta {@code STATUS_*}: Belum
 * Tersingkron (default awal), Proses, Tersingkron, Terkirim ke Feeder, dan Error.
 * <b>Arah</b> ({@code arah}) memakai {@code ARAH_*}: Ambil dari Feeder / Kirim ke
 * Feeder.</p>
 *
 * <p><b>DDL (hbm2ddl = none, jadi WAJIB manual):</b></p>
 * <pre>
 * CREATE TABLE public.neo_feeder_sync (
 *   id              bigserial PRIMARY KEY,
 *   oleh            varchar(255),
 *   oleh_id         varchar(255),
 *   tanggal_dirubah timestamp,
 *   nama            text,
 *   keterangan      text,
 *   entitas         varchar(255),
 *   aksi            varchar(255),
 *   arah            varchar(64),
 *   id_lokal        varchar(255),
 *   id_feeder       varchar(255),
 *   json_request    text,
 *   json_response   text,
 *   status          varchar(64),
 *   jumlah_data     integer,
 *   jumlah_feeder   integer,
 *   host            varchar(255),
 *   tanggal_sinkron timestamp,
 *   class_reference varchar(255),
 *   id_reference    bigint
 * );
 * -- tabel audit Envers
 * CREATE TABLE new_audit.neo_feeder_sync__audit (
 *   id              bigint  NOT NULL,
 *   rev             integer NOT NULL,
 *   revtype         smallint,
 *   oleh            varchar(255),
 *   oleh_id         varchar(255),
 *   tanggal_dirubah timestamp,
 *   nama            text,
 *   keterangan      text,
 *   entitas         varchar(255),
 *   aksi            varchar(255),
 *   arah            varchar(64),
 *   id_lokal        varchar(255),
 *   id_feeder       varchar(255),
 *   json_request    text,
 *   json_response   text,
 *   status          varchar(64),
 *   jumlah_data     integer,
 *   jumlah_feeder   integer,
 *   host            varchar(255),
 *   tanggal_sinkron timestamp,
 *   class_reference varchar(255),
 *   id_reference    bigint,
 *   PRIMARY KEY (id, rev)
 * );
 * </pre>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "neo_feeder_sync")
public class NeoFeederSync extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439809L;

	// ── Konstanta status ──────────────────────────────────────────────────
	/** Status awal/default: baris belum tersinkron dengan data lokal. */
	public static final String STATUS_BELUM = "Belum Tersingkron";
	/** Status sementara: operasi sinkronisasi sedang berjalan. */
	public static final String STATUS_PROSES = "Proses";
	/** Status akhir sukses: data sudah tersinkron dengan data lokal. */
	public static final String STATUS_TERSINGKRON = "Tersingkron";
	/** Status akhir sukses untuk arah {@link #ARAH_KIRIM}: data sudah terkirim ke Neo Feeder. */
	public static final String STATUS_TERKIRIM = "Terkirim ke Feeder";
	/** Status akhir gagal: terjadi error saat operasi sinkronisasi. */
	public static final String STATUS_ERROR = "Error";

	// ── Konstanta arah ────────────────────────────────────────────────────
	/** Arah operasi: mengambil/menarik data dari Neo Feeder ke sistem lokal. */
	public static final String ARAH_AMBIL = "Ambil dari Feeder";
	/** Arah operasi: mengirim data lokal ke Neo Feeder. */
	public static final String ARAH_KIRIM = "Kirim ke Feeder";

	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa
	 * nilai lama) — write-guard satu-arah, konsisten dengan pola arsip lain.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa
	 * nilai lama) — write-guard satu-arah, konsisten dengan pola arsip lain.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah}
	 * setiap kali baris ini di-update. Field ini adalah kebutuhan teknis, bukan bug.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{id}-{aksi}-{status}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + aksi + "-" + status;
	}

	private String nama;
	private String keterangan;
	private String entitas;
	private String aksi;
	private String arah;
	private String idLokal;
	private String idFeeder;
	private String jsonRequest;
	private String jsonResponse;
	private String status;
	private Integer jumlahData;
	private Integer jumlahFeeder;
	private String host;
	private Date tanggalSinkron;
	private String classReference;
	private Long idReference;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public NeoFeederSync() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama/label ringkas baris ini, di-trim saat dibaca; null bila belum diisi.
	 */
	@Column(name = "nama", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama/label ringkas baris ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas terkait baris sinkronisasi ini (mis. pesan error).
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan bebas terkait baris sinkronisasi ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return nama entitas/tabel Neo Feeder, mis. "Dosen", "Mahasiswa", "Matakuliah".
	 */
	@Column(name = "entitas", length = 255)
	public String getEntitas() {
		return entitas;
	}

	/**
	 * @param entitas nama entitas/tabel Neo Feeder.
	 */
	public void setEntitas(String entitas) {
		this.entitas = entitas;
	}

	/**
	 * @return nama aksi/endpoint Neo Feeder, mis. "GetListDosen". Kunci upsert per baris.
	 */
	@Column(name = "aksi", length = 255)
	public String getAksi() {
		return aksi;
	}

	/**
	 * @param aksi nama aksi/endpoint Neo Feeder (kunci upsert per baris).
	 */
	public void setAksi(String aksi) {
		this.aksi = aksi;
	}

	/**
	 * @return arah operasi: {@link #ARAH_AMBIL} / {@link #ARAH_KIRIM}.
	 */
	@Column(name = "arah", length = 64)
	public String getArah() {
		return arah;
	}

	/**
	 * @param arah arah operasi, lihat konstanta {@code ARAH_*}.
	 */
	public void setArah(String arah) {
		this.arah = arah;
	}

	/**
	 * @return id record lokal (opsional, untuk operasi per-record).
	 */
	@Column(name = "id_lokal", length = 255)
	public String getIdLokal() {
		return idLokal;
	}

	/**
	 * @param idLokal id record lokal (opsional, untuk operasi per-record).
	 */
	public void setIdLokal(String idLokal) {
		this.idLokal = idLokal;
	}

	/**
	 * @return id record di Neo Feeder (opsional, untuk operasi per-record).
	 */
	@Column(name = "id_feeder", length = 255)
	public String getIdFeeder() {
		return idFeeder;
	}

	/**
	 * @param idFeeder id record di Neo Feeder (opsional, untuk operasi per-record).
	 */
	public void setIdFeeder(String idFeeder) {
		this.idFeeder = idFeeder;
	}

	/**
	 * @return payload permintaan (request) ke Neo Feeder dalam JSON. Token disamarkan
	 *         oleh pemanggil sebelum disimpan (lihat javadoc parameter {@code jsonRequest}
	 *         pada {@code NeoFeederSyncHelper.catat(...)}).
	 */
	@Column(name = "json_request", columnDefinition = "text")
	public String getJsonRequest() {
		return jsonRequest;
	}

	/**
	 * @param jsonRequest payload permintaan (request) ke Neo Feeder dalam JSON.
	 */
	public void setJsonRequest(String jsonRequest) {
		this.jsonRequest = jsonRequest;
	}

	/**
	 * @return payload tanggapan (response) dari Neo Feeder dalam JSON.
	 */
	@Column(name = "json_response", columnDefinition = "text")
	public String getJsonResponse() {
		return jsonResponse;
	}

	/**
	 * @param jsonResponse payload tanggapan (response) dari Neo Feeder dalam JSON.
	 */
	public void setJsonResponse(String jsonResponse) {
		this.jsonResponse = jsonResponse;
	}

	/**
	 * @return status sinkronisasi: lihat konstanta {@code STATUS_*}.
	 */
	@Column(name = "status", length = 64)
	public String getStatus() {
		return status;
	}

	/**
	 * @param status status sinkronisasi, lihat konstanta {@code STATUS_*}.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return jumlah record pada tanggapan terakhir.
	 */
	@Column(name = "jumlah_data")
	public Integer getJumlahData() {
		return jumlahData;
	}

	/**
	 * @param jumlahData jumlah record pada tanggapan terakhir.
	 */
	public void setJumlahData(Integer jumlahData) {
		this.jumlahData = jumlahData;
	}

	/**
	 * @return jumlah total record di Neo Feeder (hasil GetCount), bila ada.
	 */
	@Column(name = "jumlah_feeder")
	public Integer getJumlahFeeder() {
		return jumlahFeeder;
	}

	/**
	 * @param jumlahFeeder jumlah total record di Neo Feeder (hasil GetCount).
	 */
	public void setJumlahFeeder(Integer jumlahFeeder) {
		this.jumlahFeeder = jumlahFeeder;
	}

	/**
	 * @return host Neo Feeder yang dipakai (untuk multi-tenant / audit).
	 */
	@Column(name = "host", length = 255)
	public String getHost() {
		return host;
	}

	/**
	 * @param host host Neo Feeder yang dipakai (untuk multi-tenant / audit).
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return waktu sinkronisasi terakhir untuk baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_sinkron")
	public Date getTanggalSinkron() {
		return tanggalSinkron;
	}

	/**
	 * @param tanggalSinkron waktu sinkronisasi terakhir untuk baris ini.
	 */
	public void setTanggalSinkron(Date tanggalSinkron) {
		this.tanggalSinkron = tanggalSinkron;
	}

	/**
	 * @return nama class entitas lokal eCampus yang menjadi TUJUAN sinkronisasi data ini
	 *         (mis. {@code ais.database.model.Dosen}). Dipakai untuk memetakan/menyelaraskan
	 *         data Neo Feeder ke data lokal.
	 */
	@Column(name = "class_reference", length = 255)
	public String getClassReference() {
		return classReference;
	}

	/**
	 * @param classReference nama class entitas lokal eCampus tujuan sinkronisasi.
	 */
	public void setClassReference(String classReference) {
		this.classReference = classReference;
	}

	/**
	 * @return id (PK) record pada {@link #getClassReference()} yang menjadi tujuan
	 *         sinkronisasi (untuk pemetaan per-record). Null untuk baris agregat per-aksi.
	 */
	@Column(name = "id_reference")
	public Long getIdReference() {
		return idReference;
	}

	/**
	 * @param idReference id (PK) record tujuan sinkronisasi pada {@link #getClassReference()}.
	 */
	public void setIdReference(Long idReference) {
		this.idReference = idReference;
	}

}
