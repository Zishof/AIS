package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas <b>racikan obat</b> (compounded prescription) pada modul SIRS/apotik: satu "resep racik"
 * yaitu formula obat yang dicampur/diracik apoteker dari beberapa {@link ItemMedis} menjadi satu
 * sediaan (puyer, kapsul, sirup racikan, salep campur, dan sebagainya). Baris komponen penyusunnya
 * disimpan terpisah pada {@link RacikanDetail} (relasi satu-ke-banyak yang <i>tidak</i> dipetakan
 * sebagai koleksi di kelas ini — lihat catatan "arah relasi" di bawah).
 *
 * <h3>Hubungan Racikan dengan Resep — dua konsep berbeda, bukan sub-tipe</h3>
 * Perlu ditegaskan karena mudah tertukar: {@link Resep} dan {@code Racikan} adalah dua entitas
 * yang <b>berdiri sendiri</b>, masing-masing dengan tabel sendiri
 * ({@code sirs.resep} dan {@code sirs.racikan}), bukan hubungan induk-anak atau
 * "satu jenis baris di dalam Resep". Yang menjembatani keduanya adalah {@link ResepDetail}:
 * <ul>
 * <li>{@link ResepDetail} adalah baris resep. Ia memiliki dua FK alternatif —
 * {@link ResepDetail#getItem()} (obat jadi, diresepkan apa adanya) dan
 * {@link ResepDetail#getRacikan()} (obat yang harus diracik). Jadi satu lembar {@link Resep}
 * dapat memuat campuran baris obat jadi dan baris racikan; racikan bukan jenis resep, melainkan
 * <b>isi salah satu baris</b> resep.</li>
 * <li>Sebaliknya, kelas ini juga menyimpan FK balik {@link #getResepDetail()} ke baris resep yang
 * memakainya. Jadi pasangan Racikan &harr; ResepDetail dihubungkan oleh <b>dua kolom FK terpisah</b>
 * ({@code sirs.racikan.resep_detail} dan {@code sirs.resep_detail.racikan}) yang keduanya
 * dapat ditulis secara independen — tidak ada {@code mappedBy} yang menjadikan salah satunya
 * turunan yang lain. Konsekuensinya, kedua kolom itu bisa saling bertentangan (mis. racikan A
 * menunjuk baris resep 1, sementara baris resep 1 menunjuk racikan B) tanpa ada penjaga apa pun
 * di lapisan model. Kode pemanggil wajib menulis kedua sisi secara konsisten.</li>
 * </ul>
 *
 * <h3>Tiga konteks pemakaian (tiga FK opsional yang saling eksklusif secara logis)</h3>
 * Satu baris {@code Racikan} dipakai ulang untuk tiga situasi berbeda, dibedakan oleh FK mana yang
 * terisi — tetapi ketiganya {@code nullable} dan tidak ada <i>check constraint</i> yang memaksa
 * hanya satu terisi:
 * <ol>
 * <li>{@link #getResepDetail()} terisi &rarr; racikan berasal dari lembar resep dokter (belum tentu
 * sudah ditransaksikan/dilayani apotik).</li>
 * <li>{@link #getTransaksiDetail()} terisi &rarr; racikan yang benar-benar dilayani dan ditagihkan
 * pada satu baris {@link TransaksiMedisDetail}.</li>
 * <li>{@link #getTransaksiReturDetail()} terisi &rarr; racikan pada transaksi retur/pengembalian
 * obat.</li>
 * </ol>
 * Karena FK ini bebas, satu baris racikan yang sama secara teknis dapat menunjuk resep, transaksi,
 * dan retur sekaligus. Dalam praktiknya alur kerja aplikasi (lihat
 * {@code ais.action.master.sirs.detail.ResepRacikanDetailAction},
 * {@code TransaksiRacikanDetailAction}, dan {@code TransaksiReturRacikanDetailAction}) membuat
 * baris {@code Racikan} <b>baru</b> untuk tiap konteks alih-alih memakai ulang satu baris, sehingga
 * pertentangan itu tidak muncul pada jalur normal.
 *
 * <h3>Variasi racikan ({@link #getVariasiDari()})</h3>
 * Relasi rujuk-diri opsional yang memungkinkan sebuah racikan dinyatakan sebagai turunan/variasi
 * dari formula racikan lain (mis. formula dasar puyer batuk anak, lalu variasi dengan dosis
 * berbeda). Relasi ini <b>tidak dibatasi kedalamannya dan tidak dijaga terhadap siklus</b>: tidak
 * ada validasi yang mencegah A menunjuk B sementara B menunjuk A, atau racikan menunjuk dirinya
 * sendiri. Penelusuran rantai variasi oleh kode pemanggil karena itu harus dibatasi sendiri agar
 * tidak berputar tanpa henti.
 *
 * <h3>Pola arsitektur yang berlaku di kelas ini</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — {@link #getJenisRacikan()} dan {@link #getVariasiDari()} memanggil
 * {@code check(...)} milik {@link GeneralValueObject}, yang menulis balik hasilnya ke field.
 * Getter di kelas ini karena itu bukan operasi murni-baca: ia dapat memicu materialisasi proxy
 * Hibernate dan mengubah state objek. Ini disengaja (agar proxy malas tidak bocor ke lapisan UI),
 * bukan cacat.</li>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} beserta hook {@link #onUpdate()} adalah keharusan teknis
 * infrastruktur audit ({@code ais.database.hibernate.AuditTimestampInterceptor} dan Hibernate
 * Envers lewat {@link Audited}), bukan data domain. Setter {@code oleh}/{@code olehId} sengaja
 * mengabaikan nilai kosong agar identitas pengubah terakhir tidak terhapus oleh binding form
 * yang mengirim string kosong.</li>
 * <li><b>Tanpa sumbu tenant</b> — seperti seluruh modul {@code sirs}, entitas ini tidak memiliki
 * kolom satuan kerja/tenant, sehingga isolasi data antar unit tidak dapat ditegakkan di lapisan
 * model. Ini keterbatasan modul yang sudah tercatat terpisah, bukan cacat khusus kelas ini.</li>
 * </ul>
 *
 * @see RacikanDetail komponen/bahan penyusun racikan beserta harga, diskon, dan pajaknya
 * @see JenisRacikan klasifikasi bentuk sediaan racikan
 * @see Resep lembar resep; {@link ResepDetail} baris resep yang dapat merujuk racikan ini
 * @see ais.common.CommonSirs#hitungDiskonRacikan perhitungan diskon agregat seluruh komponen racikan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "racikan")
public class Racikan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini sengaja sama di hampir seluruh entitas modul {@code sirs}
	 * karena semuanya diturunkan dari templat pembangkit yang sama; jangan dijadikan penanda
	 * identitas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.racikan}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas (login/NIP) pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir. Nilai {@code null} maupun string kosong/spasi
	 * <b>diabaikan</b> (metode langsung kembali tanpa menulis) agar jejak audit yang sudah ada tidak
	 * terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan pendamping {@link #olehId}. */
	private String oleh;

	/**
	 * Representasi teks racikan untuk ditampilkan di komponen ZK (combobox, grid, laporan).
	 * Memakai {@link #kode} langsung dari field, bukan lewat getter.
	 *
	 * @return kode racikan; dapat {@code null} bila kode belum diisi
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Nilai kosong/spasi diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan waktu/pelaku perubahan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui. Dideklarasikan
	 * satu baris bersama field {@link #tanggal_dirubah} mengikuti gaya pembangkit kode modul ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir. Normalnya diisi otomatis oleh interceptor audit lewat
	 * {@link #onUpdate()}; pengisian manual hanya untuk keperluan migrasi/perbaikan data.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode racikan, wajib diisi; dipakai sebagai label tampil lewat {@link #toString()}. */
	private String kode;

	/** Nama/judul formula racikan (kolom bertipe {@code text}, panjang bebas). */
	private String nama;

	/** Keterangan bebas, mis. aturan pakai atau catatan peracikan. */
	private String keterangan;

	/** Klasifikasi bentuk sediaan racikan (puyer, kapsul, sirup, dan sebagainya). */
	private JenisRacikan jenisRacikan;

	// Optional
	/** Racikan induk bila baris ini merupakan variasi dari formula lain; relasi rujuk-diri opsional. */
	private Racikan variasiDari;

	/** Baris transaksi medis yang menagihkan racikan ini, bila racikan sudah dilayani dan ditagihkan. */
	private TransaksiMedisDetail transaksiDetail;

	/** Baris transaksi retur bila racikan ini dikembalikan/dibatalkan. */
	private TransaksiReturDetail transaksiReturDetail;

	/** Baris resep yang memesan racikan ini, bila racikan berasal dari lembar resep dokter. */
	private ResepDetail resepDetail;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate untuk instansiasi entitas. */
	public Racikan() {
	}

	/**
	 * Mengembalikan kunci utama baris racikan.
	 *
	 * @return id baris, atau {@code null} bila entitas belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Umumnya hanya dipakai kerangka kerja persistensi atau untuk mengosongkan
	 * id ({@code null}) saat menyalin entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode racikan.
	 *
	 * @return kode racikan (kolom wajib, maksimal 255 karakter)
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi kode racikan. Tidak ada penjaga keunikan di lapisan model maupun indeks unik pada
	 * kolomnya, sehingga duplikasi kode dapat terjadi bila kode dibangkitkan di luar helper
	 * penomoran standar.
	 *
	 * @param kode kode racikan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas racikan.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas racikan.
	 *
	 * @param keterangan catatan peracikan/aturan pakai
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi nama/judul formula racikan.
	 *
	 * @param nama nama formula racikan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan nama/judul formula racikan.
	 *
	 * @return nama racikan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "nama", columnDefinition = "text", nullable = true)
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi klasifikasi bentuk sediaan racikan.
	 *
	 * @param jenisRacikan jenis racikan, boleh {@code null}
	 */
	public void setJenisRacikan(JenisRacikan jenisRacikan) {
		this.jenisRacikan = jenisRacikan;
	}

	/**
	 * Mengembalikan klasifikasi bentuk sediaan racikan. <b>Getter destruktif</b>: memanggil
	 * {@code check(...)} yang memaksa materialisasi proxy malas Hibernate dan menulis balik hasilnya
	 * ke field, sehingga pemanggilan getter ini mengubah state objek.
	 *
	 * @return jenis racikan, atau {@code null} bila tidak diisi/proxy tidak dapat dimaterialisasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_racikan", nullable = true)
	public JenisRacikan getJenisRacikan() {
		jenisRacikan = check(jenisRacikan);
		return jenisRacikan;
	}

	/**
	 * Menetapkan racikan induk yang menjadi asal variasi baris ini. Tidak ada validasi siklus:
	 * menetapkan objek ini sendiri atau membentuk rantai melingkar tidak ditolak di sini.
	 *
	 * @param variasiDari racikan induk, atau {@code null} bila racikan ini bukan variasi
	 */
	public void setVariasiDari(Racikan variasiDari) {
		this.variasiDari = variasiDari;
	}

	/**
	 * Mengembalikan racikan induk yang menjadi asal variasi baris ini. <b>Getter destruktif</b>
	 * (lihat {@link #getJenisRacikan()}). Penelusuran rantai variasi harus dibatasi sendiri oleh
	 * pemanggil karena siklus tidak dicegah.
	 *
	 * @return racikan induk, atau {@code null} bila racikan ini bukan variasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "variasi_dari", nullable = true)
	public Racikan getVariasiDari() {
		variasiDari = check(variasiDari);
		return variasiDari;
	}

	/**
	 * Menautkan racikan ini ke baris transaksi medis yang menagihkannya.
	 *
	 * @param transaksiDetail baris transaksi medis, atau {@code null}
	 */
	public void setTransaksiDetail(TransaksiMedisDetail transaksiDetail) {
		this.transaksiDetail = transaksiDetail;
	}

	/**
	 * Mengembalikan baris transaksi medis yang menagihkan racikan ini. Berbeda dari
	 * {@link #getJenisRacikan()}, getter ini tidak memanggil {@code check(...)} sehingga tetap
	 * murni-baca; sebagai gantinya relasi diambil dengan {@link FetchMode#SELECT}.
	 *
	 * @return baris transaksi medis, atau {@code null} bila racikan belum/tidak ditagihkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_detail", nullable = true)
	public TransaksiMedisDetail getTransaksiDetail() {
		return transaksiDetail;
	}

	/**
	 * Menautkan racikan ini ke baris transaksi retur.
	 *
	 * @param transaksiReturDetail baris transaksi retur, atau {@code null}
	 */
	public void setTransaksiReturDetail(TransaksiReturDetail transaksiReturDetail) {
		this.transaksiReturDetail = transaksiReturDetail;
	}

	/**
	 * Mengembalikan baris transaksi retur tempat racikan ini dikembalikan.
	 *
	 * @return baris transaksi retur, atau {@code null} bila racikan tidak diretur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_retur_detail", nullable = true)
	public TransaksiReturDetail getTransaksiReturDetail() {
		return transaksiReturDetail;
	}

	/**
	 * Menautkan racikan ini ke baris resep yang memesannya. Perhatikan bahwa {@link ResepDetail}
	 * juga menyimpan FK {@code racikan} ke arah sebaliknya dan kedua kolom itu independen — kedua
	 * sisi harus ditulis bersamaan agar tidak saling bertentangan.
	 *
	 * @param resepDetail baris resep, atau {@code null}
	 */
	public void setResepDetail(ResepDetail resepDetail) {
		this.resepDetail = resepDetail;
	}

	/**
	 * Mengembalikan baris resep yang memesan racikan ini.
	 *
	 * @return baris resep, atau {@code null} bila racikan tidak berasal dari lembar resep
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "resep_detail", nullable = true)
	public ResepDetail getResepDetail() {
		return resepDetail;
	}

}
