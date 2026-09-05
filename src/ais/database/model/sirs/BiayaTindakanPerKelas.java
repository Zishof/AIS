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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <b>Tarif tindakan medis per kelas perawatan</b>: satu baris menyatakan berapa biaya satu
 * {@link Tindakan} pada satu {@link KelasPerawatan} (VIP, Kelas I, Kelas II, Kelas III, dan
 * seterusnya). Inilah lapisan kedua pada susunan penetapan harga jasa/tindakan medis; rincian
 * pembentuk angkanya tersimpan sebagai baris-baris {@link Biaya} yang menunjuk balik ke sini.
 *
 * <h3>Satu tabel untuk dua peran: tarif standar dan tarif khusus</h3>
 * Hal terpenting yang perlu dipahami tentang entitas ini adalah bahwa tarif standar dan
 * <b>tarif khusus</b> ({@link TarifKhusus}, mis. tarif untuk pasien perusahaan tertentu, peserta
 * asuransi tertentu, dokter tertentu, atau pasien tertentu) disimpan pada <b>tabel yang sama</b>,
 * {@code sirs.biaya_tindakan_per_kelas}, dan dibedakan semata-mata oleh pola pengisian dua kolom
 * yang saling meniadakan:
 * <table border="1" summary="Pola pengisian kolom penentu peran baris">
 * <tr><th>Peran baris</th><th>{@code tindakan}</th><th>{@code tarif_khusus_punya_tindakan}</th></tr>
 * <tr><td>Tarif standar</td><td>terisi</td><td>{@code NULL}</td></tr>
 * <tr><td>Tarif khusus</td><td>{@code NULL}</td><td>terisi</td></tr>
 * </table>
 * Pada baris tarif khusus, tindakan yang dimaksud tidak dibaca dari kolom {@code tindakan}
 * melainkan ditelusuri lewat {@link TarifKhususPunyaTindakan#getTindakan()}. Konsekuensi
 * pentingnya: <b>tarif khusus menggantikan baris tarif dasar, bukan menimpanya</b>. Tidak pernah
 * ada keadaan "tarif dasar dan tarif khusus sama-sama berlaku lalu salah satu menang" — resolusi
 * di {@code ais.action.master.sirs.util.CommonTarifTindakan} memilih terlebih dahulu apakah
 * konteks pasien/dokter/asuransi/komunitas cocok dengan suatu tarif khusus, lalu mengambil baris
 * yang sesuai peran itu saja. Pertanyaan "mana yang menang kalau tumpang tindih" karena itu tidak
 * berlaku di lapisan ini; ia berlaku di lapisan pemilihan tarif khusus, dan dijawab
 * {@code CommonTarif.getTarif} dengan strategi generalisasi bertingkat dari kecocokan paling
 * spesifik ke paling umum.
 *
 * <h3>Pembuatan baris otomatis (get-or-create) dan implikasinya</h3>
 * {@code CommonTarifTindakan.getBiayaTindakanPerKelas} memakai strategi "ambil atau buat": bila
 * kombinasi tindakan/kelas/tarif-khusus belum punya baris, baris baru <b>langsung dibuat dan
 * disimpan</b>, dengan biaya awal 0 untuk tarif standar, atau disalin dari tarif standar pada
 * kelas yang sama untuk tarif khusus. Efek sampingnya:
 * <ul>
 * <li>Sekadar membuka layar transaksi dapat menciptakan baris tarif baru di basis data.</li>
 * <li>Tarif yang belum pernah dikonfigurasi berperilaku sebagai tarif nol, bukan sebagai
 * kesalahan — layanan tetap dapat ditagihkan seharga Rp 0 tanpa peringatan.</li>
 * <li>Tidak ada indeks unik atas kombinasi (tindakan, kelas, tarif khusus), sehingga dua sesi
 * paralel yang membuka tindakan yang sama dapat membuat dua baris tarif kembar; kueri pengambil
 * memakai {@code setMaxResults(1)} sehingga salah satunya menjadi baris siluman yang tidak pernah
 * terpakai namun tetap tersimpan.</li>
 * </ul>
 *
 * <h3>Dua saklar perilaku</h3>
 * <ul>
 * <li>{@link #getPembagianBiayaDalamPersen()} menentukan arah turunan pada rincian {@link Biaya}:
 * bila benar, operator mengisi porsi persen tiap komponen dan nilai rupiahnya diturunkan; bila
 * salah, sebaliknya. Lihat {@link Biaya#getJumlah()} dan {@link Biaya#getPersen()}.</li>
 * <li>{@link #getHargaBisaDirubahSaatTransaksi()} mengizinkan kasir menyunting harga saat
 * transaksi berlangsung. Saklar ini hanya berpengaruh bila dipasangkan dengan mode persen —
 * lihat pemakaiannya di {@code TransaksiItemDetailHelper} yang menyalakan
 * {@code menggunakanAmountCustom} hanya ketika <i>kedua</i> saklar bernilai benar.</li>
 * </ul>
 * Keduanya adalah bendera dua-arah yang benar (dapat dinyalakan dan dimatikan lewat setter), dengan
 * getter yang menormalkan {@code null} menjadi {@code false} — artinya baris lama yang kolomnya
 * masih {@code NULL} diperlakukan sebagai mode rupiah dan harga terkunci.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — {@link #getTindakan()}, {@link #getKelasPerawatan()},
 * {@link #getTarifKhususPunyaTindakan()}, {@link #getBiaya()}, {@link #getKeterangan()}, dan kedua
 * getter saklar semuanya menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — tarif bersifat global lintas unit.</li>
 * <li>{@code biaya}, {@code pembagianBiayaDalamPersen}, dan {@code hargaBisaDirubahSaatTransaksi}
 * tidak diberi {@code @Column}, sehingga dipetakan ke kolom bernama sesuai nama properti.</li>
 * </ul>
 *
 * @see Biaya rincian komponen pembentuk angka tarif ini
 * @see TarifKhususPunyaTindakan penanda bahwa baris ini adalah tarif khusus, bukan tarif standar
 * @see BiayaAlatMedisPerKelas padanan entitas ini untuk alat medis
 * @see ais.action.master.sirs.util.CommonTarifTindakan resolusi dan pembuatan otomatis baris tarif
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "biaya_tindakan_per_kelas")
public class BiayaTindakanPerKelas extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.biaya_tindakan_per_kelas}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris tarif ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks baris tarif untuk komponen ZK. Metode ini memaksa materialisasi proxy malas
	 * {@link #tindakan} dan {@link #kelasPerawatan} lebih dahulu dengan memanggil {@code check(...)}
	 * secara langsung, baru kemudian merangkai label dari field — sebuah cara memastikan label tidak
	 * menampilkan representasi proxy. Karena itu {@code toString()} di sini bukan operasi ringan:
	 * ia dapat memicu dua kueri basis data dan mengubah state objek.
	 *
	 * @return label baris tarif berisi tindakan, kelas perawatan, biaya, dan keterangan
	 */
	public String toString() {
		tindakan = check(tindakan);
		kelasPerawatan = check(kelasPerawatan);

		return "tindakan = " + tindakan + ", kelasPerawatan = " + kelasPerawatan + ", biaya = " + biaya + " --- "
				+ getKeterangan();
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris tarif ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris tarif ini. Nilai ini adalah satu-satunya
	 * penanda waktu pada entitas ini: <b>tarif tidak memiliki masa berlaku</b> (tidak ada kolom
	 * {@code mulai}/{@code sampai}), sehingga menyunting angka tarif berlaku surut bagi seluruh
	 * pembacaan berikutnya dan riwayat tarif hanya dapat ditelusuri lewat Hibernate Envers.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan bebas atas baris tarif. */
	private String keterangan;

	/** Tindakan yang ditarifkan; terisi hanya pada baris tarif standar. */
	private Tindakan tindakan;

	/** Penanda tarif khusus; terisi hanya pada baris tarif khusus, menggantikan {@link #tindakan}. */
	private TarifKhususPunyaTindakan tarifKhususPunyaTindakan;

	/** Kelas perawatan yang menjadi sumbu pembeda tarif. */
	private KelasPerawatan kelasPerawatan;

	/** Nilai tarif dalam rupiah; hasil penjumlahan rincian {@link Biaya} yang menunjuk baris ini. */
	private Double biaya = 0.0;

	/** Saklar arah turunan rincian biaya: persen (benar) atau rupiah (salah). */
	private Boolean pembagianBiayaDalamPersen = false;

	/** Saklar izin penyuntingan harga saat transaksi berlangsung. */
	private Boolean hargaBisaDirubahSaatTransaksi = false;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public BiayaTindakanPerKelas() {
	}

	/**
	 * Mengembalikan kunci utama baris tarif.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris tarif, menormalkan {@code null} menjadi string kosong.
	 * <b>Getter destruktif</b>: normalisasi ditulis balik ke field.
	 *
	 * @return keterangan, tidak pernah {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas baris tarif.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tindakan yang ditarifkan baris ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Bernilai {@code null} pada baris tarif khusus — pada baris semacam itu
	 * tindakannya harus dibaca lewat {@link #getTarifKhususPunyaTindakan()}.
	 *
	 * @return tindakan yang ditarifkan, atau {@code null} bila baris ini tarif khusus
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tindakan", nullable = true)
	public Tindakan getTindakan() {
		tindakan = check(tindakan);
		return tindakan;
	}

	/**
	 * Menetapkan tindakan yang ditarifkan. Pada baris tarif khusus, nilai ini justru <b>harus</b>
	 * dikosongkan ({@code null}) agar baris tidak terambil sebagai tarif standar oleh kueri
	 * resolusi yang menyaring dengan {@code isNull("tarifKhususPunyaTindakan")}.
	 *
	 * @param tindakan tindakan yang ditarifkan, atau {@code null} untuk baris tarif khusus
	 */
	public void setTindakan(Tindakan tindakan) {
		this.tindakan = tindakan;
	}

	/**
	 * Mengembalikan kelas perawatan yang menjadi sumbu pembeda tarif ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Kolomnya {@code nullable}, sehingga baris tarif tanpa kelas dapat
	 * tersimpan dan tidak akan pernah cocok dengan kueri resolusi mana pun.
	 *
	 * @return kelas perawatan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = true)
	public KelasPerawatan getKelasPerawatan() {
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

	/**
	 * Menetapkan kelas perawatan yang menjadi sumbu pembeda tarif ini.
	 *
	 * @param kelasPerawatan kelas perawatan
	 */
	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	/**
	 * Mengembalikan nilai tarif dalam rupiah, menormalkan {@code null} maupun {@code NaN} menjadi
	 * {@code 0.0}. <b>Getter destruktif</b>: normalisasi ditulis balik ke field.
	 *
	 * <p>
	 * Nilai ini bukan angka yang diketik operator secara langsung pada alur normal, melainkan hasil
	 * penjumlahan seluruh baris {@link Biaya} yang menunjuk baris tarif ini — penjumlahan itu
	 * dilakukan {@code CommonPendaftaranUtil} yang lalu memanggil {@link #setBiaya(Double)}.
	 * Nilai ini pula yang menjadi basis {@link Biaya#getJumlahTotal()} selama baris biaya masih
	 * berupa cetakan tarif.
	 * </p>
	 *
	 * <p>
	 * Karena baris tarif yang baru dibuat otomatis berbiaya {@code 0.0}, tarif yang belum
	 * dikonfigurasi tidak dapat dibedakan dari tarif yang memang sengaja digratiskan. Tidak ada
	 * penjaga yang menolak nilai negatif.
	 * </p>
	 *
	 * @return nilai tarif dalam rupiah, tidak pernah {@code null} maupun {@code NaN}
	 */
	public Double getBiaya() {

		if (biaya == null || Double.isNaN(biaya)) {
			biaya = 0.0;
		}
		return biaya;
	}

	/**
	 * Mengisi nilai tarif dalam rupiah. Pada alur normal dipanggil oleh kode penjumlah rincian
	 * biaya, bukan langsung oleh form.
	 *
	 * @param biaya nilai tarif dalam rupiah
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * Mengembalikan penanda tarif khusus baris ini. <b>Getter destruktif</b> ({@code check(...)}).
	 * Terisinya nilai ini menandai bahwa baris adalah tarif khusus dan bahwa {@link #getTindakan()}
	 * pada baris ini pasti {@code null}.
	 *
	 * @return penanda tarif khusus, atau {@code null} bila baris ini tarif standar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tarif_khusus_punya_tindakan", nullable = true)
	public TarifKhususPunyaTindakan getTarifKhususPunyaTindakan() {
		tarifKhususPunyaTindakan = check(tarifKhususPunyaTindakan);
		return tarifKhususPunyaTindakan;
	}

	/**
	 * Menjadikan baris ini tarif khusus dengan menautkannya ke penanda tarif khusus. Harus
	 * dipasangkan dengan {@code setTindakan(null)} agar pola pembeda peran tetap konsisten; tidak
	 * ada penjaga di lapisan model yang memaksanya.
	 *
	 * @param tarifKhususPunyaTindakan penanda tarif khusus, atau {@code null} untuk tarif standar
	 */
	public void setTarifKhususPunyaTindakan(TarifKhususPunyaTindakan tarifKhususPunyaTindakan) {
		this.tarifKhususPunyaTindakan = tarifKhususPunyaTindakan;
	}

	/**
	 * Mengembalikan saklar arah turunan rincian biaya, menormalkan {@code null} menjadi
	 * {@code false}. <b>Getter destruktif</b>. Bernilai benar berarti operator mengisi porsi persen
	 * tiap komponen dan nilai rupiahnya diturunkan ({@link Biaya#getJumlah()}); bernilai salah
	 * berarti sebaliknya ({@link Biaya#getPersen()}).
	 *
	 * @return {@code true} bila rincian biaya dikonfigurasi dalam persen, tidak pernah {@code null}
	 */
	public Boolean getPembagianBiayaDalamPersen() {
		if (pembagianBiayaDalamPersen == null) {
			pembagianBiayaDalamPersen = false;
		}
		return pembagianBiayaDalamPersen;
	}

	/**
	 * Menyalakan atau mematikan mode pembagian biaya dalam persen. Mengubah saklar ini membalik arah
	 * turunan seluruh rincian {@link Biaya} milik baris tarif ini, sehingga angka yang sebelumnya
	 * merupakan masukan operator berubah menjadi nilai turunan dan sebaliknya.
	 *
	 * @param pembagianBiayaDalamPersen {@code true} untuk mode persen, {@code false} untuk mode rupiah
	 */
	public void setPembagianBiayaDalamPersen(Boolean pembagianBiayaDalamPersen) {
		this.pembagianBiayaDalamPersen = pembagianBiayaDalamPersen;
	}

	/**
	 * Mengembalikan saklar izin penyuntingan harga saat transaksi, menormalkan {@code null} menjadi
	 * {@code false}. <b>Getter destruktif</b>. Saklar ini baru berpengaruh bila dipasangkan dengan
	 * mode persen — kode transaksi menyalakan isian harga bebas hanya ketika kedua saklar bernilai
	 * benar.
	 *
	 * @return {@code true} bila harga boleh diubah saat transaksi, tidak pernah {@code null}
	 */
	public Boolean getHargaBisaDirubahSaatTransaksi() {
		if (hargaBisaDirubahSaatTransaksi == null) {
			hargaBisaDirubahSaatTransaksi = false;
		}
		return hargaBisaDirubahSaatTransaksi;
	}

	/**
	 * Menyalakan atau mematikan izin penyuntingan harga saat transaksi.
	 *
	 * @param hargaBisaDirubahSaatTransaksi {@code true} bila kasir boleh menyunting harga
	 */
	public void setHargaBisaDirubahSaatTransaksi(Boolean hargaBisaDirubahSaatTransaksi) {
		this.hargaBisaDirubahSaatTransaksi = hargaBisaDirubahSaatTransaksi;
	}

}
