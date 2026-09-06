package ais.database.model.ticket;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * <h3>Ticket — entitas utama modul Ticketing Management</h3>
 *
 * <p>Tiket mencatat monitoring pengembangan &amp; implementasi sistem: progress develop, kendala,
 * permintaan modul baru, dan interaksi (pesan) antara developer↔pengguna maupun pengguna↔pengguna.
 * Bersifat GENERAL — pengaju bisa pegawai/dosen/guru/siswa/mahasiswa/calon/orang tua/vendor/umum
 * (disimpan via {@link #pengajuUserId}/{@link #pengajuTipe}, plus email/HP untuk pengaju eksternal).</p>
 *
 * <p><b>Workflow.</b> {@code extends DataSop} → tiket diajukan lewat SOP/disposisi yang sudah
 * berjalan; {@link #disposisiSop} menautkan tiket ke alur persetujuannya.</p>
 *
 * <p><b>Scoping.</b> {@link #satuanKerja} + {@link #hakAksesTarget} (CSV roleId) dipakai membatasi
 * visibilitas tiket sesuai posisi pengguna; admin/developer melihat semua.</p>
 *
 * <p>Tabel {@code public.ticket} dibuat otomatis oleh {@code hbm2ddl=update} saat restart. Nama
 * kolom snake_case, semua FK nullable agar aman. Di-audit Envers untuk riwayat/recovery.</p>
 *
 * <p><b>Relasi entity lain di paket ini.</b> Satu {@link Ticket} dapat memiliki banyak
 * {@link TicketKomentar} (relasi anak, FK {@code ticket} pada {@code TicketKomentar}) sebagai thread
 * pesan/komentar, dan mereferensikan satu {@link TicketKategori} (FK {@code ticket_kategori}) sebagai
 * lookup pengelompokan. Enforcement siapa-boleh-lihat/kelola tiket TIDAK dilakukan di kelas entity
 * ini — dilakukan di layer action/controller (mis. {@code TicketingAction.scopedCriteria()} dan
 * pengecekan {@code bolehKelola()} pada {@code NewUiTicketController}) yang membaca field scoping
 * ({@link #satuanKerja}, {@link #hakAksesTarget}, {@link #ditugaskanKeUserId}) di kelas ini.</p>
 *
 * <p><b>Field bayangan audit.</b> {@link #aktif}, {@link #oleh}, {@link #olehId}, dan
 * {@link #tanggal_dirubah} adalah field lokal yang meniru pola kolom audit standar
 * {@code GeneralValueObject} (kelas ini sendiri tidak meng-extend {@code GeneralValueObject},
 * melainkan {@code DataSop}) — konsisten dengan pola arsitektur berulang di seluruh basis kode ini,
 * bukan duplikasi yang tidak disengaja.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ticket")
public class Ticket extends DataSop {

	private static final long serialVersionUID = 3120250724002L;

	/** Status: tiket baru dibuat oleh pengaju, belum ditinjau siapa pun. Nilai default {@link #getStatus()}. */
	public static final String STATUS_BARU = "Baru";
	/** Status: tiket sudah ditinjau/divalidasi oleh pengelola, belum mulai dikerjakan. */
	public static final String STATUS_DITINJAU = "Ditinjau";
	/** Status: tiket sedang aktif dikerjakan/diproses oleh pihak yang ditugaskan. */
	public static final String STATUS_DIPROSES = "Diproses";
	/** Status: pengelola menunggu balasan/klarifikasi lebih lanjut dari pengaju. */
	public static final String STATUS_MENUNGGU_RESPON = "Menunggu Respon";
	/** Status: pengerjaan sudah selesai; lihat {@link #getTanggalSelesai()} untuk waktu penyelesaian. */
	public static final String STATUS_SELESAI = "Selesai";
	/** Status: tiket ditutup secara final, tidak ada aktivitas lanjutan yang diharapkan. */
	public static final String STATUS_DITUTUP = "Ditutup";
	/** Status: tiket ditolak (mis. bukan kendala valid atau di luar cakupan penanganan). */
	public static final String STATUS_DITOLAK = "Ditolak";

	/** Tipe: laporan kendala teknis/bug pada sistem yang sudah berjalan. */
	public static final String TIPE_KENDALA = "Kendala / Bug";
	/** Tipe: permintaan pembuatan modul atau fitur baru. */
	public static final String TIPE_PERMINTAAN = "Permintaan Modul Baru";
	/** Tipe: laporan progress pengembangan modul yang sedang dikerjakan developer. */
	public static final String TIPE_PROGRESS = "Progress Development";
	/** Tipe: pertanyaan umum atau interaksi non-teknis antar pengguna. */
	public static final String TIPE_INTERAKSI = "Pertanyaan / Interaksi";
	/** Tipe: kategori lain di luar keempat pilihan baku di atas. */
	public static final String TIPE_LAINNYA = "Lainnya";

	/** Prioritas rendah — dapat ditangani belakangan. */
	public static final String PRIORITAS_RENDAH = "Rendah";
	/** Prioritas sedang — nilai default yang dipakai {@link #getPrioritas()} bila belum diisi. */
	public static final String PRIORITAS_SEDANG = "Sedang";
	/** Prioritas tinggi — perlu penanganan lebih cepat dari biasanya. */
	public static final String PRIORITAS_TINGGI = "Tinggi";
	/** Prioritas kritis — perlu penanganan segera, mengganggu operasional. */
	public static final String PRIORITAS_KRITIS = "Kritis";

	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** Nomor tiket yang dibaca manusia (mis. untuk ditampilkan di PDF/notifikasi); boleh kosong. */
	private String nomorTiket;
	/** Judul singkat tiket; disyaratkan tidak kosong. */
	private String judul;
	/** Deskripsi/isi lengkap laporan atau permintaan pada tiket. */
	private String deskripsi;
	/** Kategori lookup tiket (lihat {@link TicketKategori}); opsional. */
	private TicketKategori ticketKategori;
	/** Tipe tiket, salah satu konstanta {@code TIPE_*} (bebas teks, tidak divalidasi enum di level entity). */
	private String tipe;
	/** Prioritas tiket, salah satu konstanta {@code PRIORITAS_*}; kosong dianggap {@link #PRIORITAS_SEDANG}. */
	private String prioritas;
	/** Status siklus hidup tiket, salah satu konstanta {@code STATUS_*}; kosong dianggap {@link #STATUS_BARU}. */
	private String status;
	/** Nama modul/aplikasi terkait tiket (bebas teks, mis. untuk pengelompokan pada dasbor). */
	private String modul;
	/** Persentase progress pengerjaan (0-100 secara konvensi); {@code null} dianggap 0. */
	private Integer progress;

	// Pengaju (general lintas jenis pengguna)
	/** ID pengguna pengaju tiket, lintas jenis akun (pegawai/dosen/siswa/dll — lihat {@link #pengajuTipe}). */
	private String pengajuUserId;
	/** Nama pengaju yang disimpan mandiri (snapshot), tidak bergantung lookup akun saat tampil. */
	private String pengajuNama;
	/** Jenis/tipe akun pengaju (mis. pegawai, dosen, siswa, mahasiswa, calon, orang tua, vendor, umum). */
	private String pengajuTipe;
	/** Alamat email pengaju; dipakai terutama untuk pengaju eksternal tanpa akun sistem. */
	private String pengajuEmail;
	/** Nomor HP pengaju; dipakai terutama untuk pengaju eksternal tanpa akun sistem. */
	private String pengajuHp;

	// Scoping posisi pengguna
	/** Satuan kerja terkait tiket, dipakai membatasi visibilitas tiket sesuai posisi pengguna. */
	private SatuanKerja satuanKerja;
	/** Daftar roleId (CSV) yang berhak mengakses tiket ini di luar penugasan langsung; opsional. */
	private String hakAksesTarget;

	// Penanganan
	/** ID pengguna (developer/pengelola) yang ditugaskan menangani tiket ini. */
	private String ditugaskanKeUserId;
	/** Nama pengguna yang ditugaskan, disimpan sebagai snapshot untuk tampilan cepat. */
	private String ditugaskanKeNama;

	// Workflow SOP
	/** Disposisi SOP yang menaungi alur persetujuan/penugasan tiket ini. */
	private DisposisiSop disposisiSop;

	// Waktu
	/** Waktu tiket dibuat; default saat instansiasi objek, sebelum benar-benar disimpan. */
	private Date tanggalDibuat = ais.ui.util.WaktuUtil.getDate();
	/** Target waktu penyelesaian tiket (SLA); opsional. */
	private Date tanggalTarget;
	/** Waktu tiket benar-benar selesai dikerjakan; diisi saat status berubah ke {@link #STATUS_SELESAI}. */
	private Date tanggalSelesai;

	/** Penanda aktif/nonaktif tiket (soft-delete/hide); {@code null} dianggap aktif ({@code true}). */
	private Boolean aktif;
	/** Nama pengguna terakhir yang mengubah baris ini (field audit bayangan, lihat javadoc kelas). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (field audit bayangan, lihat javadoc kelas). */
	private String olehId;
	/** Waktu perubahan terakhir; diperbarui otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate). */
	public Ticket() {
	}

	/**
	 * Mengambil ID baris (primary key).
	 *
	 * @return ID tiket, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Mengatur ID baris. Biasanya tidak dipanggil manual karena kolom {@code id} auto-increment
	 * dan {@code insertable = false}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nomor tiket yang dibaca manusia.
	 *
	 * @return nomor tiket, boleh {@code null}.
	 */
	@Column(name = "nomor_tiket", nullable = true, length = 64)
	public String getNomorTiket() {
		return nomorTiket;
	}

	/**
	 * Mengatur nomor tiket yang dibaca manusia.
	 *
	 * @param nomorTiket nomor tiket baru.
	 */
	public void setNomorTiket(String nomorTiket) {
		this.nomorTiket = nomorTiket;
	}

	/**
	 * Mengambil judul singkat tiket.
	 *
	 * @return judul tiket.
	 */
	@Column(name = "judul", nullable = false, columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	/**
	 * Mengatur judul singkat tiket.
	 *
	 * @param judul judul baru.
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Alias agar kompatibel dengan {@code getNama()} milik {@code GeneralValueObject} (dipakai
	 * combo/label umum). Tidak dipetakan JPA (bukan properti persisten baru) — hanya mengembalikan
	 * nilai {@link #judul} yang sudah ada.
	 *
	 * @return nilai {@link #getJudul()}.
	 */
	public String getNama() {
		return judul;
	}

	/**
	 * Mengambil deskripsi/isi lengkap tiket.
	 *
	 * @return deskripsi tiket, boleh {@code null}.
	 */
	@Column(name = "deskripsi", nullable = true, columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi;
	}

	/**
	 * Mengatur deskripsi/isi lengkap tiket.
	 *
	 * @param deskripsi deskripsi baru.
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengambil kategori lookup tiket.
	 *
	 * @return {@link TicketKategori} terkait, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "ticket_kategori", nullable = true)
	public TicketKategori getTicketKategori() {
		return ticketKategori;
	}

	/**
	 * Mengatur kategori lookup tiket.
	 *
	 * @param ticketKategori kategori baru.
	 */
	public void setTicketKategori(TicketKategori ticketKategori) {
		this.ticketKategori = ticketKategori;
	}

	/**
	 * Mengambil tipe tiket (salah satu konstanta {@code TIPE_*}, bebas teks di level entity).
	 *
	 * @return tipe tiket, boleh {@code null}.
	 */
	@Column(name = "tipe", nullable = true, length = 64)
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengatur tipe tiket.
	 *
	 * @param tipe tipe baru.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengambil prioritas tiket. Tidak menulis balik ke field {@link #prioritas} — hanya
	 * mengembalikan nilai default {@link #PRIORITAS_SEDANG} secara sementara bila field kosong
	 * atau berisi spasi saja, tanpa mempersist perubahan apa pun.
	 *
	 * @return prioritas tiket, tidak pernah {@code null}/kosong.
	 */
	@Column(name = "prioritas", nullable = true, length = 32)
	public String getPrioritas() {
		return prioritas == null || prioritas.trim().isEmpty() ? PRIORITAS_SEDANG : prioritas;
	}

	/**
	 * Mengatur prioritas tiket.
	 *
	 * @param prioritas prioritas baru, idealnya salah satu konstanta {@code PRIORITAS_*}.
	 */
	public void setPrioritas(String prioritas) {
		this.prioritas = prioritas;
	}

	/**
	 * Mengambil status siklus hidup tiket. Tidak menulis balik ke field {@link #status} — hanya
	 * mengembalikan nilai default {@link #STATUS_BARU} secara sementara bila field kosong atau
	 * berisi spasi saja, tanpa mempersist perubahan apa pun.
	 *
	 * @return status tiket, tidak pernah {@code null}/kosong.
	 */
	@Column(name = "status", nullable = true, length = 32)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_BARU : status;
	}

	/**
	 * Mengatur status siklus hidup tiket.
	 *
	 * @param status status baru, idealnya salah satu konstanta {@code STATUS_*}.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil nama modul/aplikasi terkait tiket.
	 *
	 * @return nama modul, boleh {@code null}.
	 */
	@Column(name = "modul", nullable = true, length = 255)
	public String getModul() {
		return modul;
	}

	/**
	 * Mengatur nama modul/aplikasi terkait tiket.
	 *
	 * @param modul nama modul baru.
	 */
	public void setModul(String modul) {
		this.modul = modul;
	}

	/**
	 * Mengambil persentase progress pengerjaan. Tidak menulis balik ke field {@link #progress} —
	 * hanya mengembalikan 0 secara sementara bila field {@code null}.
	 *
	 * @return progress (0-100 secara konvensi), tidak pernah {@code null}.
	 */
	@Column(name = "progress", nullable = true)
	public Integer getProgress() {
		return progress == null ? 0 : progress;
	}

	/**
	 * Mengatur persentase progress pengerjaan.
	 *
	 * @param progress progress baru.
	 */
	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	/**
	 * Mengambil ID pengguna pengaju tiket.
	 *
	 * @return ID pengaju, boleh {@code null}.
	 */
	@Column(name = "pengaju_user_id", nullable = true, length = 128)
	public String getPengajuUserId() {
		return pengajuUserId;
	}

	/**
	 * Mengatur ID pengguna pengaju tiket.
	 *
	 * @param pengajuUserId ID pengaju baru.
	 */
	public void setPengajuUserId(String pengajuUserId) {
		this.pengajuUserId = pengajuUserId;
	}

	/**
	 * Mengambil nama pengaju (snapshot, tidak bergantung lookup akun).
	 *
	 * @return nama pengaju, boleh {@code null}.
	 */
	@Column(name = "pengaju_nama", nullable = true, length = 255)
	public String getPengajuNama() {
		return pengajuNama;
	}

	/**
	 * Mengatur nama pengaju.
	 *
	 * @param pengajuNama nama pengaju baru.
	 */
	public void setPengajuNama(String pengajuNama) {
		this.pengajuNama = pengajuNama;
	}

	/**
	 * Mengambil jenis/tipe akun pengaju (mis. pegawai, dosen, siswa, mahasiswa, calon, orang tua,
	 * vendor, umum).
	 *
	 * @return tipe pengaju, boleh {@code null}.
	 */
	@Column(name = "pengaju_tipe", nullable = true, length = 64)
	public String getPengajuTipe() {
		return pengajuTipe;
	}

	/**
	 * Mengatur jenis/tipe akun pengaju.
	 *
	 * @param pengajuTipe tipe pengaju baru.
	 */
	public void setPengajuTipe(String pengajuTipe) {
		this.pengajuTipe = pengajuTipe;
	}

	/**
	 * Mengambil alamat email pengaju (terutama untuk pengaju eksternal).
	 *
	 * @return email pengaju, boleh {@code null}.
	 */
	@Column(name = "pengaju_email", nullable = true, length = 255)
	public String getPengajuEmail() {
		return pengajuEmail;
	}

	/**
	 * Mengatur alamat email pengaju.
	 *
	 * @param pengajuEmail email pengaju baru.
	 */
	public void setPengajuEmail(String pengajuEmail) {
		this.pengajuEmail = pengajuEmail;
	}

	/**
	 * Mengambil nomor HP pengaju (terutama untuk pengaju eksternal).
	 *
	 * @return nomor HP pengaju, boleh {@code null}.
	 */
	@Column(name = "pengaju_hp", nullable = true, length = 64)
	public String getPengajuHp() {
		return pengajuHp;
	}

	/**
	 * Mengatur nomor HP pengaju.
	 *
	 * @param pengajuHp nomor HP pengaju baru.
	 */
	public void setPengajuHp(String pengajuHp) {
		this.pengajuHp = pengajuHp;
	}

	/**
	 * Mengambil satuan kerja terkait tiket, dipakai membatasi visibilitas tiket sesuai posisi
	 * pengguna pada layer action/controller (lihat javadoc kelas — enforcement tidak dilakukan
	 * di kelas ini).
	 *
	 * @return {@link SatuanKerja} terkait, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		return satuanKerja;
	}

	/**
	 * Mengatur satuan kerja terkait tiket.
	 *
	 * @param satuanKerja satuan kerja baru.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengambil daftar roleId (CSV) yang berhak mengakses tiket ini di luar penugasan langsung.
	 * Format dan parsing CSV ini ditangani di layer action, bukan di kelas entity.
	 *
	 * @return string CSV roleId, boleh {@code null}.
	 */
	@Column(name = "hak_akses_target", nullable = true, columnDefinition = "text")
	public String getHakAksesTarget() {
		return hakAksesTarget;
	}

	/**
	 * Mengatur daftar roleId (CSV) yang berhak mengakses tiket ini.
	 *
	 * @param hakAksesTarget string CSV roleId baru.
	 */
	public void setHakAksesTarget(String hakAksesTarget) {
		this.hakAksesTarget = hakAksesTarget;
	}

	/**
	 * Mengambil ID pengguna (developer/pengelola) yang ditugaskan menangani tiket ini.
	 *
	 * @return ID pengguna yang ditugaskan, boleh {@code null} bila belum ada penugasan.
	 */
	@Column(name = "ditugaskan_ke_user_id", nullable = true, length = 128)
	public String getDitugaskanKeUserId() {
		return ditugaskanKeUserId;
	}

	/**
	 * Mengatur ID pengguna yang ditugaskan menangani tiket ini.
	 *
	 * @param ditugaskanKeUserId ID pengguna baru.
	 */
	public void setDitugaskanKeUserId(String ditugaskanKeUserId) {
		this.ditugaskanKeUserId = ditugaskanKeUserId;
	}

	/**
	 * Mengambil nama pengguna yang ditugaskan (snapshot untuk tampilan cepat).
	 *
	 * @return nama pengguna yang ditugaskan, boleh {@code null}.
	 */
	@Column(name = "ditugaskan_ke_nama", nullable = true, length = 255)
	public String getDitugaskanKeNama() {
		return ditugaskanKeNama;
	}

	/**
	 * Mengatur nama pengguna yang ditugaskan.
	 *
	 * @param ditugaskanKeNama nama pengguna baru.
	 */
	public void setDitugaskanKeNama(String ditugaskanKeNama) {
		this.ditugaskanKeNama = ditugaskanKeNama;
	}

	/**
	 * Mengambil disposisi SOP yang menaungi alur persetujuan/penugasan tiket ini.
	 *
	 * @return {@link DisposisiSop} terkait, boleh {@code null}.
	 */
	@Override
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		return disposisiSop;
	}

	/**
	 * Mengatur disposisi SOP yang menaungi tiket ini.
	 *
	 * @param disposisiSop disposisi SOP baru.
	 */
	@Override
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		this.disposisiSop = disposisiSop;
	}

	/**
	 * Mengambil waktu tiket dibuat.
	 *
	 * @return tanggal dibuat.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dibuat", nullable = true)
	public Date getTanggalDibuat() {
		return tanggalDibuat;
	}

	/**
	 * Mengatur waktu tiket dibuat.
	 *
	 * @param tanggalDibuat tanggal dibuat baru.
	 */
	public void setTanggalDibuat(Date tanggalDibuat) {
		this.tanggalDibuat = tanggalDibuat;
	}

	/**
	 * Mengambil target waktu penyelesaian tiket (SLA).
	 *
	 * @return tanggal target, boleh {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_target", nullable = true)
	public Date getTanggalTarget() {
		return tanggalTarget;
	}

	/**
	 * Mengatur target waktu penyelesaian tiket.
	 *
	 * @param tanggalTarget tanggal target baru.
	 */
	public void setTanggalTarget(Date tanggalTarget) {
		this.tanggalTarget = tanggalTarget;
	}

	/**
	 * Mengambil waktu tiket benar-benar selesai dikerjakan.
	 *
	 * @return tanggal selesai, boleh {@code null} bila belum selesai.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_selesai", nullable = true)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Mengatur waktu tiket benar-benar selesai dikerjakan.
	 *
	 * @param tanggalSelesai tanggal selesai baru.
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengambil status aktif/nonaktif tiket. Tidak menulis balik ke field {@link #aktif} — hanya
	 * mengembalikan {@code true} secara sementara bila field {@code null} (default aktif).
	 *
	 * @return {@code true} bila tiket dianggap aktif.
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengatur status aktif/nonaktif tiket.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, boleh {@code null} bila belum pernah diubah.
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengatur nama pengguna terakhir yang mengubah baris ini. Nilai kosong/blank diabaikan
	 * (tidak menimpa nilai lama) agar riwayat "oleh" tidak hilang akibat pemanggilan dengan nilai
	 * kosong secara tidak sengaja.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna pengubah, boleh {@code null} bila belum pernah diubah.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengatur ID pengguna terakhir yang mengubah baris ini. Nilai kosong/blank diabaikan (tidak
	 * menimpa nilai lama), sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengambil waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengatur waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah tanggal perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Callback JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate sebelum setiap
	 * {@code UPDATE}, mendelegasikan pencatatan {@link #oleh}/{@link #olehId}/
	 * {@link #tanggal_dirubah} ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi string ringkas tiket, dipakai untuk debugging/log.
	 *
	 * @return string berformat {@code "<id>-<judul>"}.
	 */
	public String toString() {
		return id + "-" + judul;
	}
}
