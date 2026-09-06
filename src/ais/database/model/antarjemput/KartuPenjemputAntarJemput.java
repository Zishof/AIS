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
import ais.database.model.sekolah.Siswa;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.kartu_penjemput_antar_jemput}
 * pada modul layanan antar-jemput siswa/mahasiswa. Merepresentasikan satu
 * kartu identitas penjemput (mis. kartu ber-barcode yang dipegang orang tua/wali
 * atau petugas jemput) yang berlaku untuk satu peserta didik/mahasiswa/dosen/guru/
 * pegawai tertentu, lengkap dengan identitas penjemput ({@code namaPenjemput},
 * {@code hubungan}, {@code nomorIdentitas}, {@code nomorHp}), nomor & barcode
 * kartu, serta masa berlaku ({@code berlakuMulai}..{@code berlakuSampai}) dan
 * status aktif/nonaktif kartu.
 *
 * <p>
 * Berelasi many-to-one opsional ke {@link Siswa}, {@link Mahasiswa},
 * {@link Guru}, {@link Dosen}, dan {@link Pegawai} — kartu penjemput dapat
 * dikaitkan ke salah satu jenis peserta tersebut, tergantung jenjang layanan
 * antar-jemput. Diaudit lewat Hibernate Envers ({@code @Audited}).
 * </p>
 *
 * <h2>Investigasi keamanan &mdash; identitas penjemput (domain keselamatan anak, 2026-09-06)</h2>
 * <p>
 * Kartu ini BUKAN sekadar catatan administratif pasif — ia adalah kredensial fisik yang
 * dipindai (bukan diketik bebas oleh petugas) untuk MEMULAI proses penjemputan lewat servlet
 * kiosk gerbang {@link ais.action.servlet.AntarJemput}. Rincian lengkap mekanismenya:
 * </p>
 * <ul>
 * <li><b>Identitas kartu yang dipindai:</b> {@link #getNomorKartu()} (kode unik tercetak/tersimpan
 * di kartu) dan {@link #getBarcode()} (nilai QR/barcode yang dipindai kamera atau RFID) —
 * kombinasi keduanya dicari via {@code Restrictions.or} di
 * {@code AntarJemput.findKartu(session, nomor)} sehingga kartu bisa diidentifikasi lewat salah
 * satu dari dua nilai tersebut. Ini adalah verifikasi BERBASIS SISTEM (pencocokan ke baris unik
 * di DB), BUKAN pencatatan manual bebas oleh petugas gerbang — petugas/kiosk tidak "mengetik nama
 * penjemput", melainkan memindai kredensial yang lalu dicocokkan sistem.</li>
 * <li><b>Gerbang aktif/masa berlaku (fail-closed):</b> kartu yang ditemukan tetap ditolak
 * (transaksi dicatat {@code DITOLAK} untuk audit) bila {@link #getAktif()} bernilai
 * {@code false} atau {@link #getBerlakuSampai()} sudah lewat — lihat {@code AntarJemput.verify}.
 * Tidak ada bypass; kartu yang tidak lolos kedua cek ini tidak akan pernah memicu pemanggilan
 * peserta.</li>
 * <li><b>Pengikatan ke peserta yang sah:</b> kartu HARUS terhubung (via salah satu dari
 * {@link #getSiswa()}, {@link #getMahasiswa()}, {@link #getGuru()}, {@link #getDosen()},
 * {@link #getPegawai()}) ke peserta yang terdaftar aktif pada
 * {@link PesertaJadwalAntarJemput} — pengecekan ini dilakukan lewat relasi FK yang divalidasi
 * pada saat kartu dibuat (CRUD kartu, lihat di bawah), bukan diverifikasi ulang setiap transaksi
 * (transaksi hanya query "siapa peserta aktif dengan siswa/mahasiswa/dst yang SAMA dengan milik
 * kartu ini" — lihat javadoc {@link DetailPenjemputanAntarJemput}).</li>
 * <li><b>Tidak ada foto pada kartu:</b> entitas ini TIDAK punya field foto penjemput (mis. URL/
 * blob foto wajah) — identitas visual penjemput hanya berupa {@link #getNamaPenjemput()} (teks)
 * dan {@link #getHubungan()} (mis. "Ayah"/"Ibu"/"Wali"/nama penjemput pengganti), yang dicetak
 * di kartu QR (lihat {@code AntarJemput.renderCardPage}) untuk dicocokkan SECARA VISUAL oleh
 * petugas gerbang terhadap orang yang membawa kartu — ini adalah lapisan verifikasi MANUAL yang
 * melengkapi (bukan menggantikan) verifikasi sistem di atas. Tanpa foto, seseorang yang secara
 * fisik mirip/mengaku sebagai penjemput terdaftar dan membawa kartu fisik/tampilan QR yang sah
 * (mis. discan dari HP orang lain) berpotensi lolos pengecekan visual petugas — ini adalah celah
 * yang sudah melekat pada desain "kartu tanpa foto" itu sendiri, bukan bug baru di kode.</li>
 * <li><b>Manajemen data kartu (CRUD):</b> panel {@code antarjemput/panel_kartu} DIAKTIFKAN
 * (parameter {@code true}) di {@code GenericCrudAkademikOverrides.DINAIKKAN} untuk generic CRUD
 * v2, artinya staf dengan hak akses panel ini dapat menambah/mengubah kartu (termasuk
 * nomorKartu/barcode/relasi peserta) lewat layar generik, bukan hanya lewat layar khusus
 * {@code AntarJemputAction}. Siapa yang berhak mengakses panel generik ini diatur oleh
 * mekanisme hak akses/peran generic CRUD v2 di luar paket ini — TIDAK diaudit ulang pada sesi
 * dokumentasi ini karena di luar cakupan (fokus sesi ini adalah paket model {@code antarjemput}).
 * </li>
 * </ul>
 * <p>
 * <b>Kesimpulan mekanisme verifikasi penjemput:</b> KUAT untuk lapisan identifikasi
 * sistem (kartu unik yang wajib dipindai + wajib aktif + wajib belum kedaluwarsa + wajib terhubung
 * FK ke peserta terdaftar, fail-closed di setiap gerbang pengecekan) — ini BUKAN sekadar
 * pencatatan manual setelah kejadian. Namun verifikasi identitas FISIK penjemput di titik gerbang
 * itu sendiri bersifat gabungan sistem+manual: sistem memastikan "kartu ini sah dan terhubung ke
 * peserta X", sedangkan "orang yang membawa kartu ini benar-benar penjemput yang berhak" masih
 * bergantung pada kecocokan visual manual petugas terhadap {@link #getNamaPenjemput()}/
 * {@link #getHubungan()} tercetak (tanpa foto pembanding). Lihat juga javadoc
 * {@link DetailPenjemputanAntarJemput} untuk analisis titik serah-terima final (konfirmasi
 * "Serah") yang TIDAK melakukan pemindaian ulang kartu. Kedua temuan ini konsisten dengan pola
 * verifikasi berlapis kartu+visual yang umum di sistem shuttle/gerbang sekolah secara umum,
 * sehingga TIDAK di-spawn sebagai task keamanan baru pada sesi dokumentasi ini.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kartu_penjemput_antar_jemput")
public class KartuPenjemputAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439815L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private String namaPenjemput;
	private String hubungan;
	private String nomorIdentitas;
	private String nomorHp;
	private String nomorKartu;
	private String barcode;
	private Date berlakuMulai;
	private Date berlakuSampai;
	private Boolean aktif;

	private Siswa siswa;
	private Mahasiswa mahasiswa;
	private Guru guru;
	private Dosen dosen;
	private Pegawai pegawai;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public KartuPenjemputAntarJemput() {
	}

	/** @return ID unik baris kartu (primary key, auto-increment via {@code IDENTITY}). */
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

	/** Kode kartu; bila belum diisi langsung, jatuh ke {@link #getNomorKartu()}. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? getNomorKartu() : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** Nama tampilan kartu; bila belum diisi langsung, jatuh ke {@link #getNamaPenjemput()}. */
	@Column(name = "nama", length = 255)
	public String getNama() {
		return nama == null ? getNamaPenjemput() : nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk kartu ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return nama penjemput yang berhak memakai kartu ini, di-trim. Wajib diisi
	 *         ({@code nullable = false}) — ini adalah label identitas visual utama yang dicetak di
	 *         kartu QR (lihat {@code AntarJemput.renderCardPage}) dan dicocokkan secara MANUAL oleh
	 *         petugas gerbang terhadap orang yang membawa kartu (kartu ini tidak menyimpan foto —
	 *         lihat catatan keamanan pada javadoc kelas).
	 */
	@Column(name = "nama_penjemput", nullable = false, length = 255)
	public String getNamaPenjemput() {
		return namaPenjemput == null ? null : namaPenjemput.trim();
	}

	/** @param namaPenjemput lihat {@link #getNamaPenjemput()}. */
	public void setNamaPenjemput(String namaPenjemput) {
		this.namaPenjemput = namaPenjemput;
	}

	/** @return hubungan penjemput dengan peserta (mis. "Ayah"/"Ibu"/"Wali"/"Supir Pribadi"), format teks bebas; ikut dicetak di kartu QR sebagai bagian identitas visual yang dicocokkan petugas gerbang. */
	@Column(name = "hubungan", length = 80)
	public String getHubungan() {
		return hubungan;
	}

	/** @param hubungan lihat {@link #getHubungan()}. */
	public void setHubungan(String hubungan) {
		this.hubungan = hubungan;
	}

	/** @return nomor identitas penjemput (mis. KTP/SIM), data pribadi sensitif — tidak ada masking/pembatasan akses di level entitas ini (lihat catatan kepemilikan pada javadoc {@link LogNotifikasiAntarJemput}, prinsip yang sama berlaku di sini: filter siapa yang boleh membaca field ini ada di layar/aksi pemanggil, bukan di entitas). */
	@Column(name = "nomor_identitas", length = 80)
	public String getNomorIdentitas() {
		return nomorIdentitas;
	}

	/** @param nomorIdentitas lihat {@link #getNomorIdentitas()}. */
	public void setNomorIdentitas(String nomorIdentitas) {
		this.nomorIdentitas = nomorIdentitas;
	}

	/** @return nomor telepon/HP penjemput, dipakai untuk kontak/verifikasi tambahan di luar sistem bila diperlukan. */
	@Column(name = "nomor_hp", length = 40)
	public String getNomorHp() {
		return nomorHp;
	}

	/** @param nomorHp lihat {@link #getNomorHp()}. */
	public void setNomorHp(String nomorHp) {
		this.nomorHp = nomorHp;
	}

	/**
	 * @return nomor kartu unik, di-trim; wajib diisi ({@code nullable = false}). Bersama
	 *         {@link #getBarcode()}, inilah kredensial yang dipindai/dicocokkan ke DB oleh
	 *         {@code AntarJemput.findKartu} untuk memulai proses penjemputan — lihat catatan
	 *         keamanan pada javadoc kelas untuk rincian lengkap alur verifikasinya. Tidak ada
	 *         penegakan keunikan ({@code unique}) pada level anotasi kolom ini; keunikan (bila
	 *         ada) ditegakkan di level DB/aplikasi pemanggil, bukan di entitas ini.
	 */
	@Column(name = "nomor_kartu", nullable = false, length = 100)
	public String getNomorKartu() {
		return nomorKartu == null ? null : nomorKartu.trim();
	}

	/** @param nomorKartu lihat {@link #getNomorKartu()}. */
	public void setNomorKartu(String nomorKartu) {
		this.nomorKartu = nomorKartu;
	}

	/**
	 * @return nilai QR/barcode kartu ini (dipindai kamera {@code BarcodeDetector} di kiosk
	 *         gerbang, alternatif dari input manual {@link #getNomorKartu()}). Dicari bersama
	 *         {@link #getNomorKartu()} lewat {@code Restrictions.or} oleh
	 *         {@code AntarJemput.findKartu} — kartu dapat dikenali lewat salah satu dari kedua
	 *         nilai ini.
	 */
	@Column(name = "barcode", length = 255)
	public String getBarcode() {
		return barcode;
	}

	/** @param barcode lihat {@link #getBarcode()}. */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/** @return tanggal mulai berlakunya kartu ini (hanya komponen tanggal yang dipersist — lihat {@code @Temporal(DATE)}); tidak ditegakkan secara eksplisit oleh {@code AntarJemput.verify} (hanya {@link #getBerlakuSampai()} yang dicek terhadap waktu sekarang). */
	@Temporal(TemporalType.DATE)
	public Date getBerlakuMulai() {
		return berlakuMulai;
	}

	/** @param berlakuMulai lihat {@link #getBerlakuMulai()}. */
	public void setBerlakuMulai(Date berlakuMulai) {
		this.berlakuMulai = berlakuMulai;
	}

	/**
	 * @return tanggal berakhirnya masa berlaku kartu ini. Ditegakkan secara aktif: bila tanggal
	 *         ini sudah lewat waktu sekarang, {@code AntarJemput.verify} menolak transaksi
	 *         (dicatat {@code DITOLAK}) walau kartu ditemukan dan {@link #getAktif()} bernilai
	 *         {@code true} — lihat catatan keamanan pada javadoc kelas. {@code null} berarti
	 *         kartu tidak punya batas masa berlaku (berlaku selamanya selama {@link #getAktif()}
	 *         tetap {@code true}).
	 */
	@Temporal(TemporalType.DATE)
	public Date getBerlakuSampai() {
		return berlakuSampai;
	}

	/** @param berlakuSampai lihat {@link #getBerlakuSampai()}. */
	public void setBerlakuSampai(Date berlakuSampai) {
		this.berlakuSampai = berlakuSampai;
	}

	/**
	 * @return status aktif kartu; default {@code true} bila belum diisi (tidak di-cache ke
	 *         field). Ditegakkan secara aktif oleh {@code AntarJemput.verify}: kartu yang
	 *         ditemukan tapi {@code aktif == false} tetap ditolak (fail-closed) — lihat catatan
	 *         keamanan pada javadoc kelas. Menonaktifkan kartu (mis. saat kartu hilang/dicabut)
	 *         adalah mekanisme utama untuk mencabut hak akses penjemput tanpa menghapus riwayat
	 *         datanya.
	 */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return siswa pemilik kartu ini, bila jenjang layanannya sekolah; {@code null} bila kartu untuk jenjang lain. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/** @param siswa lihat {@link #getSiswa()}. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/** @return mahasiswa pemilik kartu ini, bila jenjang layanannya perguruan tinggi; {@code null} bila kartu untuk jenjang lain. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa lihat {@link #getMahasiswa()}. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return guru pemilik kartu ini, bila kartu untuk guru; {@code null} bila kartu untuk jenis lain. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/** @param guru lihat {@link #getGuru()}. */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/** @return dosen pemilik kartu ini, bila kartu untuk dosen; {@code null} bila kartu untuk jenis lain. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/** @param dosen lihat {@link #getDosen()}. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return pegawai/staf pemilik kartu ini, bila kartu untuk pegawai; {@code null} bila kartu untuk jenis lain. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai lihat {@link #getPegawai()}. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}
}
