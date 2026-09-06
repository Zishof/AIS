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

import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.detail_penjemputan_antar_jemput}
 * pada modul layanan antar-jemput siswa/mahasiswa. Merepresentasikan satu baris
 * status penjemputan untuk satu peserta pada satu transaksi antar-jemput —
 * yaitu perjalanan status seorang peserta (siswa/mahasiswa/guru/dosen/pegawai)
 * sejak menunggu panggilan hingga diserahkan ke penjemput, lengkap dengan
 * waktu pemanggilan ({@code waktuDipanggil}), waktu keluar kelas
 * ({@code waktuKeluarKelas}), dan waktu serah terima ({@code waktuSerahTerima}).
 *
 * <p>
 * Berelasi many-to-one ke {@link TransaksiPenjemputanAntarJemput} (transaksi
 * antar-jemput yang menaunginya) dan {@link PesertaJadwalAntarJemput} (peserta
 * pada jadwal antar-jemput); dari peserta jadwal itu pula sejumlah field
 * ({@code nama}, {@link #getSiswa()}, {@link #getMahasiswa()}, {@link #getGuru()},
 * {@link #getDosen()}, {@link #getPegawai()}, {@link #getKelasSiswa()}) diwarisi
 * secara lazy bila belum diisi langsung pada baris ini. Diaudit lewat Hibernate
 * Envers ({@code @Audited}).
 * </p>
 *
 * <h2>Investigasi keamanan &mdash; mekanisme verifikasi penjemput (2026-09-06)</h2>
 * <p>
 * Entitas ini sendiri adalah POJO/entitas Hibernate murni: TIDAK ada logika verifikasi apa pun di
 * kelas ini. Mekanisme verifikasi sesungguhnya berada di dua pemakai luar paket
 * {@code ais.database.model.antarjemput} yang membuat/mengubah baris entitas ini — servlet kiosk
 * gerbang {@link ais.action.servlet.AntarJemput} (metode privat {@code verify}, dipanggil dari
 * endpoint {@code action=verify}) dan layar staf internal
 * {@code ais.action.master.antarjemput.AntarJemputAction} (metode privat
 * {@code updateDetailStatus}, dipanggil dari tombol "Serah" pada panel Detail Panggilan). Berikut
 * rangkuman lengkap alurnya, karena ini domain sensitif keselamatan anak:
 * </p>
 * <ol>
 * <li><b>Identifikasi kartu (kuat, berbasis sistem):</b> penjemput memindai kartu fisik
 * (QR/barcode via kamera {@code BarcodeDetector}, RFID reader model "keyboard wedge", atau input
 * manual nomor kartu) di kiosk gerbang. Nilai yang terbaca dicocokkan ke DB oleh
 * {@code AntarJemput.findKartu(session, nomor)} terhadap kolom {@code nomorKartu} ATAU
 * {@code barcode} milik {@link KartuPenjemputAntarJemput} — BUKAN pencatatan manual bebas oleh
 * petugas. Kartu yang tidak ditemukan, tidak aktif ({@link KartuPenjemputAntarJemput#getAktif()}),
 * atau sudah lewat masa berlaku ({@link KartuPenjemputAntarJemput#getBerlakuSampai()}) ditolak dan
 * tetap dicatat sebagai transaksi berstatus {@code DITOLAK} untuk audit (fail-closed, bukan
 * fail-open).</li>
 * <li><b>Pengikatan ke peserta yang sah (kuat, berbasis relasi FK):</b> kartu yang valid
 * dicocokkan ke {@link PesertaJadwalAntarJemput} yang AKTIF via relasi FK kartu ke
 * siswa/mahasiswa/guru/dosen/pegawai ({@code AntarJemput.findPeserta}) — bukan sekadar "kartu
 * apa saja boleh menjemput siapa saja". Bila tidak ada peserta aktif yang cocok dengan pemilik
 * kartu, transaksi ditolak ({@code DITOLAK}) dan baris {@link DetailPenjemputanAntarJemput} untuk
 * peserta itu TIDAK dibuat sama sekali — satu baris entitas ini hanya lahir setelah kartu
 * tervalidasi DAN cocok dengan peserta terdaftar sah.</li>
 * <li><b>Antrian panggilan (status {@link #MENUNGGU_PANGGILAN} &rarr; {@link #SUDAH_DIPANGGIL}
 * &rarr; {@link #KELUAR_KELAS}):</b> transisi ini dipicu oleh {@code AntarJemputAction} saat
 * petugas menekan tombol terkait di layar; hanya mencatat waktu ({@link #getWaktuDipanggil()},
 * {@link #getWaktuKeluarKelas()}) dan mengirim log soundbox, tanpa verifikasi identitas tambahan
 * (memang belum ada serah-terima fisik pada tahap ini).</li>
 * <li><b>Serah-terima final (status {@link #DISERAHKAN}) &mdash; TITIK LEMAH yang teridentifikasi:</b>
 * dipicu oleh tombol "Serah" di layar staf ({@code AntarJemputAction.updateDetailStatus},
 * dipanggil dengan status {@link #DISERAHKAN}). Metode ini HANYA men-set
 * {@link #getStatusPanggilan()} dan {@link #getWaktuSerahTerima()} — TIDAK melakukan pemindaian
 * ulang kartu, TIDAK mencocokkan ulang identitas penjemput yang secara fisik hadir saat itu
 * terhadap kartu yang di-scan di awal, dan TIDAK merekam field apa pun pada baris ini tentang
 * SIAPA (penjemput/petugas mana) yang melakukan klik "Serah". Verifikasi bahwa orang yang benar
 * secara fisik hadir bergantung SEPENUHNYA pada penilaian visual petugas gerbang saat itu (mis.
 * mencocokkan wajah/kartu fisik secara manual) — sistem tidak punya cara memverifikasi ulang
 * bahwa proses ini benar dilakukan dengan benar, dan tidak ada jejak audit granular per-detail
 * tentang penjemput mana yang hadir secara fisik di titik serah-terima ini (berbeda dari titik
 * (1)-(2) yang punya jejak sistem yang kuat). Dalam praktiknya risiko ini dibatasi oleh: (a) baris
 * {@link #DetailPenjemputanAntarJemput} hanya ada untuk peserta yang sudah lolos verifikasi kartu
 * di awal (titik 1-2), dan (b) tombol "Serah" hanya dapat diklik oleh staf yang punya akses ke
 * layar admin {@code AntarJemputAction} (bukan endpoint publik) — namun ini tetap berarti TIDAK
 * ada pengikatan teknis antara "siapa yang di-scan kartunya" dan "siapa yang benar-benar
 * menerima siswa" pada momen serah-terima itu sendiri.</li>
 * </ol>
 * <p>
 * <b>Kesimpulan verifikasi penjemput:</b> mekanisme identifikasi AWAL (pemindaian kartu +
 * pencocokan ke peserta terdaftar) KUAT dan berbasis sistem (bukan pencatatan manual murni,
 * fail-closed, teraudit). Namun mekanisme KONFIRMASI SERAH-TERIMA FISIK di akhir alur bersifat
 * MURNI MANUAL/PENGAMATAN VISUAL petugas tanpa jejak sistem granular — pola yang sama seperti
 * sistem antrian penjemputan fisik pada umumnya, tapi tetap merupakan celah rawan human
 * error/social engineering yang belum tertutup secara teknis (mis. petugas lengah, terburu-buru,
 * atau kartu dipinjamkan/dipalsukan visualnya). Ini bukan kerentanan yang benar-benar baru di luar
 * pola yang sudah dikenal (verifikasi berlapis kartu+visual adalah desain umum di sistem
 * shuttle/gerbang sekolah), sehingga TIDAK di-spawn sebagai task keamanan terpisah pada sesi ini —
 * lihat javadoc {@link ais.action.servlet.AntarJemput} untuk detail penambalan keamanan otentikasi
 * kiosk yang sudah dilakukan (secret gerbang, DITAMBAL 2026-09-01), yang merupakan lapisan
 * perlindungan berbeda (mencegah pihak luar memalsukan permintaan {@code verify}/{@code card}
 * lewat jaringan) dari isu serah-terima manual di atas.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "detail_penjemputan_antar_jemput")
public class DetailPenjemputanAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439817L;

	/** Status awal: peserta terdaftar pada penjemputan tapi belum dipanggil. */
	public static final String MENUNGGU_PANGGILAN = "MENUNGGU_PANGGILAN";
	/** Status: panggilan penjemputan sudah dikirim/diumumkan ke peserta. */
	public static final String SUDAH_DIPANGGIL = "SUDAH_DIPANGGIL";
	/** Status: peserta sudah keluar kelas menuju titik penjemputan. */
	public static final String KELUAR_KELAS = "KELUAR_KELAS";
	/** Status akhir: peserta sudah diserahterimakan ke penjemput. */
	public static final String DISERAHKAN = "DISERAHKAN";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private String teksPanggilan;
	private String perangkatTujuan;
	private String statusPanggilan;
	private Date waktuDipanggil;
	private Date waktuKeluarKelas;
	private Date waktuSerahTerima;

	private TransaksiPenjemputanAntarJemput transaksiPenjemputanAntarJemput;
	private PesertaJadwalAntarJemput pesertaJadwalAntarJemput;
	private Siswa siswa;
	private Mahasiswa mahasiswa;
	private Guru guru;
	private Dosen dosen;
	private Pegawai pegawai;
	private KelasSiswa kelasSiswa;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public DetailPenjemputanAntarJemput() {
	}

	/** @return ID unik baris detail (primary key, auto-increment via {@code IDENTITY}). */
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
	 * entitas modul antarjemput. Catatan: field audit standar ini mencatat siapa yang MENGUBAH
	 * baris (mis. staf yang mengklik tombol status), BUKAN siapa penjemput fisik yang hadir — lihat
	 * catatan keamanan pada javadoc kelas.
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

	/** @return kode singkat baris detail ini, di-trim; {@code null} bila belum diisi. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** Nama peserta; bila belum diisi langsung, diambil dari {@link #getPesertaJadwalAntarJemput()}. */
	@Column(name = "nama", length = 255)
	public String getNama() {
		if (nama != null) {
			return nama.trim();
		}
		if (getPesertaJadwalAntarJemput() != null) {
			return getPesertaJadwalAntarJemput().getNama();
		}
		return null;
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk baris detail ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return teks yang ditampilkan/diumumkan (soundbox/monitor) saat memanggil peserta ini. Bila
	 *         kosong, dibangkitkan otomatis dari {@link #getNama()} dengan template "Penjemputan
	 *         ananda {nama} sudah datang." — hasilnya ikut di-cache ke field
	 *         {@link #teksPanggilan} in-memory (pola fallback yang sama seperti
	 *         {@link JadwalAntarJemput#getNama()}). Template ini SELALU memakai kata "ananda"
	 *         walau pesertanya bukan siswa (mis. mahasiswa/guru/pegawai) — berbeda dari
	 *         {@code AntarJemput.resolveTeksPanggilan} (di servlet kiosk) yang menyesuaikan kata
	 *         sapaan per jenis peserta; fallback di getter ini adalah cadangan generik bila
	 *         pemanggil tidak lewat servlet tersebut.
	 */
	@Column(name = "teks_panggilan")
	public String getTeksPanggilan() {
		if (teksPanggilan == null && getNama() != null) {
			teksPanggilan = "Penjemputan ananda " + getNama() + " sudah datang.";
		}
		return teksPanggilan;
	}

	/** @param teksPanggilan lihat {@link #getTeksPanggilan()}. */
	public void setTeksPanggilan(String teksPanggilan) {
		this.teksPanggilan = teksPanggilan;
	}

	/** @return identitas/alamat perangkat tujuan pengumuman panggilan (mis. label ruang/kelas atau id perangkat soundbox) — lihat {@link LogNotifikasiAntarJemput#getPerangkatTujuan()} untuk log pengirimannya. */
	@Column(name = "perangkat_tujuan", length = 255)
	public String getPerangkatTujuan() {
		return perangkatTujuan;
	}

	/** @param perangkatTujuan lihat {@link #getPerangkatTujuan()}. */
	public void setPerangkatTujuan(String perangkatTujuan) {
		this.perangkatTujuan = perangkatTujuan;
	}

	/**
	 * @return status alur panggilan (lihat konstanta {@link #MENUNGGU_PANGGILAN} dkk.); default
	 *         {@link #MENUNGGU_PANGGILAN} bila belum diisi (tidak di-cache ke field). Transisi ke
	 *         {@link #DISERAHKAN} adalah konfirmasi serah-terima final — lihat catatan keamanan
	 *         pada javadoc kelas untuk batasan mekanisme verifikasinya (murni klik staf tanpa
	 *         pemindaian ulang kartu).
	 */
	@Column(name = "status_panggilan", length = 40)
	public String getStatusPanggilan() {
		return statusPanggilan == null ? MENUNGGU_PANGGILAN : statusPanggilan;
	}

	/** @param statusPanggilan lihat {@link #getStatusPanggilan()}; nilai valid: {@link #MENUNGGU_PANGGILAN}, {@link #SUDAH_DIPANGGIL}, {@link #KELUAR_KELAS}, {@link #DISERAHKAN}. Tidak divalidasi terhadap konstanta ini maupun urutan transisi oleh setter — pemanggil (lihat {@code AntarJemputAction.updateDetailStatus}) bertanggung jawab menjaga urutan alur. */
	public void setStatusPanggilan(String statusPanggilan) {
		this.statusPanggilan = statusPanggilan;
	}

	/** @return waktu peserta ini dipanggil (transisi ke {@link #SUDAH_DIPANGGIL}); {@code null} sebelum dipanggil. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuDipanggil() {
		return waktuDipanggil;
	}

	/** @param waktuDipanggil lihat {@link #getWaktuDipanggil()}. */
	public void setWaktuDipanggil(Date waktuDipanggil) {
		this.waktuDipanggil = waktuDipanggil;
	}

	/** @return waktu peserta ini tercatat keluar kelas menuju titik penjemputan (transisi ke {@link #KELUAR_KELAS}); {@code null} sebelum tahap itu. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuKeluarKelas() {
		return waktuKeluarKelas;
	}

	/** @param waktuKeluarKelas lihat {@link #getWaktuKeluarKelas()}. */
	public void setWaktuKeluarKelas(Date waktuKeluarKelas) {
		this.waktuKeluarKelas = waktuKeluarKelas;
	}

	/**
	 * @return waktu peserta ini diserahterimakan ke penjemput (transisi final ke
	 *         {@link #DISERAHKAN}); {@code null} sebelum tahap itu. PENTING: field ini hanya
	 *         mencatat KAPAN, bukan KEPADA SIAPA — tidak ada field pada baris ini yang merekam
	 *         identitas penjemput yang secara fisik menerima peserta pada momen ini (lihat catatan
	 *         keamanan pada javadoc kelas).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSerahTerima() {
		return waktuSerahTerima;
	}

	/** @param waktuSerahTerima lihat {@link #getWaktuSerahTerima()}. */
	public void setWaktuSerahTerima(Date waktuSerahTerima) {
		this.waktuSerahTerima = waktuSerahTerima;
	}

	/** @return transaksi penjemputan yang menaungi baris detail ini (relasi lazy) — satu transaksi lahir dari satu kali scan kartu, dan menaungi satu atau lebih baris detail (satu per peserta yang cocok, mis. untuk kartu yang mencakup beberapa saudara kandung). Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_penjemputan_antar_jemput")
	public TransaksiPenjemputanAntarJemput getTransaksiPenjemputanAntarJemput() {
		transaksiPenjemputanAntarJemput = check(transaksiPenjemputanAntarJemput);
		return transaksiPenjemputanAntarJemput;
	}

	/** @param transaksiPenjemputanAntarJemput lihat {@link #getTransaksiPenjemputanAntarJemput()}. */
	public void setTransaksiPenjemputanAntarJemput(
			TransaksiPenjemputanAntarJemput transaksiPenjemputanAntarJemput) {
		this.transaksiPenjemputanAntarJemput = transaksiPenjemputanAntarJemput;
	}

	/** @return peserta jadwal yang menjadi subjek baris detail ini (relasi lazy); sumber fallback untuk {@link #getNama()} dan field-field relasi jenis peserta di bawah. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_jadwal_antar_jemput")
	public PesertaJadwalAntarJemput getPesertaJadwalAntarJemput() {
		pesertaJadwalAntarJemput = check(pesertaJadwalAntarJemput);
		return pesertaJadwalAntarJemput;
	}

	/** @param pesertaJadwalAntarJemput lihat {@link #getPesertaJadwalAntarJemput()}. */
	public void setPesertaJadwalAntarJemput(PesertaJadwalAntarJemput pesertaJadwalAntarJemput) {
		this.pesertaJadwalAntarJemput = pesertaJadwalAntarJemput;
	}

	/**
	 * @return siswa subjek baris detail ini, bila jenis pesertanya siswa. Bila belum di-set
	 *         langsung, diisi otomatis dari {@link #getPesertaJadwalAntarJemput()} dan hasilnya
	 *         ikut di-cache ke field {@link #siswa} in-memory (pola fallback warisan yang sama
	 *         untuk kelima field jenis peserta di bawah — {@link #getMahasiswa()},
	 *         {@link #getGuru()}, {@link #getDosen()}, {@link #getPegawai()}, dan
	 *         {@link #getKelasSiswa()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		if (siswa == null && getPesertaJadwalAntarJemput() != null) {
			siswa = getPesertaJadwalAntarJemput().getSiswa();
		}
		siswa = check(siswa);
		return siswa;
	}

	/** @param siswa lihat {@link #getSiswa()}. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/** @return mahasiswa subjek baris detail ini, bila jenis pesertanya mahasiswa. Fallback diwarisi dari {@link #getPesertaJadwalAntarJemput()} — lihat javadoc {@link #getSiswa()} untuk pola lengkapnya. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		if (mahasiswa == null && getPesertaJadwalAntarJemput() != null) {
			mahasiswa = getPesertaJadwalAntarJemput().getMahasiswa();
		}
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa lihat {@link #getMahasiswa()}. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return guru subjek baris detail ini, bila jenis pesertanya guru. Fallback diwarisi dari {@link #getPesertaJadwalAntarJemput()} — lihat javadoc {@link #getSiswa()} untuk pola lengkapnya. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		if (guru == null && getPesertaJadwalAntarJemput() != null) {
			guru = getPesertaJadwalAntarJemput().getGuru();
		}
		guru = check(guru);
		return guru;
	}

	/** @param guru lihat {@link #getGuru()}. */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/** @return dosen subjek baris detail ini, bila jenis pesertanya dosen. Fallback diwarisi dari {@link #getPesertaJadwalAntarJemput()} — lihat javadoc {@link #getSiswa()} untuk pola lengkapnya. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		if (dosen == null && getPesertaJadwalAntarJemput() != null) {
			dosen = getPesertaJadwalAntarJemput().getDosen();
		}
		dosen = check(dosen);
		return dosen;
	}

	/** @param dosen lihat {@link #getDosen()}. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return pegawai/staf subjek baris detail ini, bila jenis pesertanya pegawai. Fallback diwarisi dari {@link #getPesertaJadwalAntarJemput()} — lihat javadoc {@link #getSiswa()} untuk pola lengkapnya. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		if (pegawai == null && getPesertaJadwalAntarJemput() != null) {
			pegawai = getPesertaJadwalAntarJemput().getPegawai();
		}
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai lihat {@link #getPegawai()}. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/** @return kelas siswa subjek baris detail ini. Fallback diwarisi dari {@link #getPesertaJadwalAntarJemput()} — lihat javadoc {@link #getSiswa()} untuk pola lengkapnya. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		if (kelasSiswa == null && getPesertaJadwalAntarJemput() != null) {
			kelasSiswa = getPesertaJadwalAntarJemput().getKelasSiswa();
		}
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	/** @param kelasSiswa lihat {@link #getKelasSiswa()}. */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}
}
