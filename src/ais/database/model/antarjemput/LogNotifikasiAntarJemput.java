package ais.database.model.antarjemput;

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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk log pengiriman notifikasi layanan antar-jemput siswa — dipetakan ke
 * tabel {@code public.log_notifikasi_antar_jemput} (modul {@code antarjemput}). Mencatat satu
 * upaya pengiriman notifikasi (mis. ke perangkat soundbox/WA/aplikasi orang tua) terkait satu
 * kejadian penjemputan/pengantaran ({@link #detailPenjemputanAntarJemput}), termasuk kanal
 * pengiriman, isi pesan, status, jumlah percobaan, dan waktu kirim/diterima — dipakai untuk audit
 * &amp; troubleshooting pengiriman notifikasi (retry, delivery tracking).
 *
 * <h2>Nilai default</h2>
 * <p>
 * {@link #getKanal()} default {@code "SOUNDBOX"} (kanal notifikasi paling umum di layanan
 * antar-jemput AIS — perangkat pengeras suara di titik jemput); {@link #getStatus()} default
 * {@code "ANTRI"}; {@link #getPercobaan()} default {@code 0}; {@link #getNama()} fallback ke
 * {@link #getKanal()} bila belum diisi.
 * </p>
 *
 * <h2>Catatan keamanan &mdash; kepemilikan/scoping (diinvestigasi 2026-09-06)</h2>
 * <p>
 * Entitas ini TIDAK menyimpan referensi langsung ke siswa/mahasiswa/orang tua — identitas peserta
 * hanya dapat ditelusuri secara tidak langsung lewat rantai
 * {@link #getDetailPenjemputanAntarJemput()} &rarr;
 * {@link DetailPenjemputanAntarJemput#getPesertaJadwalAntarJemput()} &rarr;
 * {@link PesertaJadwalAntarJemput#getSiswa()}/{@code getMahasiswa()}/dst. Entitas itu sendiri
 * (dan model lain di paket ini) tidak menerapkan filter kepemilikan apa pun — bila field ini
 * (kanal, pesan, perangkatTujuan) dibaca oleh suatu layar/API, layar/API ITULAH yang bertanggung
 * jawab menyaring baris sesuai siapa yang berhak melihatnya, bukan entitas.
 * </p>
 * <p>
 * Hasil penelusuran pemakai kelas ini pada sesi dokumentasi ini (grep seluruh
 * {@code ais.action}/{@code ais.database}): hanya tiga pemakai ditemukan — servlet kiosk gerbang
 * {@link ais.action.servlet.AntarJemput} (endpoint {@code verify}, menulis log, tidak membaca/
 * menampilkannya balik ke pemanggil), serta dua layar staf internal
 * {@code ais.action.master.antarjemput.AntarJemputAction} (panel "Log Notifikasi", tabel CRUD
 * generik dinonaktifkan untuk tambah baris — lihat {@code GenericCrudAkademikOverrides},
 * {@code DITAHAN.put("antarjemput/panel_log", ...)}) dan
 * {@code ais.action.master.antarjemput.DasboardAntarJemput} (dasbor ringkasan hitungan status,
 * bukan menampilkan isi pesan per baris). TIDAK ditemukan endpoint/portal orang tua/siswa yang
 * membaca tabel ini secara langsung pada saat investigasi ini dilakukan — dengan kata lain,
 * skenario "orang tua siswa A melihat notifikasi siswa B" yang dikhawatirkan belum punya jalur
 * eksploitasi yang teridentifikasi di codebase saat ini KARENA belum ada portal orang tua yang
 * membaca tabel ini sama sekali (bukan karena ada filter kepemilikan yang terbukti benar). Bila di
 * kemudian hari dibuat portal orang tua/wali yang menampilkan riwayat notifikasi, WAJIB menambahkan
 * filter eksplisit di layar/aksi tersebut (mis. {@code Restrictions.eq} ke siswa milik orang tua
 * yang login lewat rantai relasi di atas) — jangan mengasumsikan entitas ini sudah aman secara
 * default.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "log_notifikasi_antar_jemput")
public class LogNotifikasiAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439818L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	/** Nama log; fallback ke {@link #getKanal()} bila belum diisi — lihat {@link #getNama()}. */
	private String nama;
	private String keterangan;
	/** Kanal pengiriman notifikasi (mis. SOUNDBOX, WA, dsb); default {@code "SOUNDBOX"} — lihat {@link #getKanal()}. */
	private String kanal;
	/** Identitas/alamat perangkat tujuan notifikasi (mis. id perangkat soundbox atau nomor tujuan). */
	private String perangkatTujuan;
	/** Isi pesan notifikasi yang dikirim. */
	private String pesan;
	/** Status pengiriman (mis. ANTRI/TERKIRIM/GAGAL); default {@code "ANTRI"} — lihat {@link #getStatus()}. */
	private String status;
	/** Jumlah percobaan pengiriman yang sudah dilakukan; default {@code 0}. */
	private Integer percobaan;
	/** Waktu notifikasi dikirim dari sistem. */
	private Date waktuKirim;
	/** Waktu notifikasi tercatat diterima/di-ack oleh perangkat tujuan. */
	private Date waktuDiterima;

	/** Kejadian penjemputan/pengantaran yang menjadi konteks/pemicu notifikasi ini. */
	private DetailPenjemputanAntarJemput detailPenjemputanAntarJemput;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public LogNotifikasiAntarJemput() {
	}

	/** @return ID unik baris log (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return ID pengguna (username) yang terakhir mengubah baris ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul antarjemput.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE, memperbarui {@link #tanggal_dirubah} (dan field audit terkait) lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return timestamp terakhir baris ini diubah; diisi otomatis saat objek dibuat dan diperbarui via {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode singkat log ini, di-trim; {@code null} bila belum diisi. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama tampilan log; bila belum diisi manual, fallback (tanpa di-cache ke field) ke {@link #getKanal()}. */
	@Column(name = "nama", length = 255)
	public String getNama() {
		return nama == null ? getKanal() : nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk log ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return kanal pengiriman notifikasi; default {@code "SOUNDBOX"} bila belum di-set. */
	@Column(name = "kanal", length = 40)
	public String getKanal() {
		return kanal == null ? "SOUNDBOX" : kanal;
	}

	/** @param kanal lihat {@link #getKanal()}. */
	public void setKanal(String kanal) {
		this.kanal = kanal;
	}

	/**
	 * @return identitas/alamat perangkat tujuan notifikasi (mis. id perangkat soundbox atau nomor
	 *         tujuan WA). Data yang berpotensi sensitif (nomor kontak) bila kanal bukan SOUNDBOX —
	 *         lihat catatan kepemilikan/scoping pada javadoc kelas; tidak ada pembatasan akses di
	 *         level entitas.
	 */
	@Column(name = "perangkat_tujuan", length = 255)
	public String getPerangkatTujuan() {
		return perangkatTujuan;
	}

	/** @param perangkatTujuan lihat {@link #getPerangkatTujuan()}. */
	public void setPerangkatTujuan(String perangkatTujuan) {
		this.perangkatTujuan = perangkatTujuan;
	}

	/** @return isi pesan notifikasi yang dikirim (mis. teks panggilan peserta — lihat {@link DetailPenjemputanAntarJemput#getTeksPanggilan()}). */
	@Column(name = "pesan")
	public String getPesan() {
		return pesan;
	}

	/** @param pesan lihat {@link #getPesan()}. */
	public void setPesan(String pesan) {
		this.pesan = pesan;
	}

	/** @return status pengiriman (mis. ANTRI/TERKIRIM/GAGAL); default {@code "ANTRI"} bila belum di-set (tidak di-cache ke field). */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null ? "ANTRI" : status;
	}

	/** @param status lihat {@link #getStatus()}. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return jumlah percobaan pengiriman yang sudah dilakukan; default {@code 0} bila belum di-set. */
	public Integer getPercobaan() {
		return percobaan == null ? 0 : percobaan;
	}

	/** @param percobaan lihat {@link #getPercobaan()}. */
	public void setPercobaan(Integer percobaan) {
		this.percobaan = percobaan;
	}

	/** @return waktu notifikasi dikirim dari sistem. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuKirim() {
		return waktuKirim;
	}

	/** @param waktuKirim lihat {@link #getWaktuKirim()}. */
	public void setWaktuKirim(Date waktuKirim) {
		this.waktuKirim = waktuKirim;
	}

	/** @return waktu notifikasi tercatat diterima/di-ack oleh perangkat tujuan; {@code null} selama belum ada konfirmasi penerimaan. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuDiterima() {
		return waktuDiterima;
	}

	/** @param waktuDiterima lihat {@link #getWaktuDiterima()}. */
	public void setWaktuDiterima(Date waktuDiterima) {
		this.waktuDiterima = waktuDiterima;
	}

	/**
	 * @return kejadian penjemputan/pengantaran yang menjadi konteks/pemicu notifikasi ini (relasi
	 *         lazy); satu-satunya jalur untuk menelusuri identitas peserta terkait log ini — lihat
	 *         catatan kepemilikan/scoping pada javadoc kelas. Dilewatkan {@code check()} agar
	 *         proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_penjemputan_antar_jemput")
	public DetailPenjemputanAntarJemput getDetailPenjemputanAntarJemput() {
		detailPenjemputanAntarJemput = check(detailPenjemputanAntarJemput);
		return detailPenjemputanAntarJemput;
	}

	/** @param detailPenjemputanAntarJemput lihat {@link #getDetailPenjemputanAntarJemput()}. */
	public void setDetailPenjemputanAntarJemput(DetailPenjemputanAntarJemput detailPenjemputanAntarJemput) {
		this.detailPenjemputanAntarJemput = detailPenjemputanAntarJemput;
	}
}
