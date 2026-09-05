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
 * <b>Tarif alat medis per kelas perawatan</b>: satu baris menyatakan berapa biaya pemakaian satu
 * {@link AlatMedis} (alat kesehatan, mis. ventilator, inkubator, monitor pasien) pada satu
 * {@link KelasPerawatan}. Entitas ini adalah kembaran struktural
 * {@link BiayaTindakanPerKelas} untuk sumbu alat medis, dengan pola dan keterbatasan yang persis
 * sama; rincian pembentuk angkanya tersimpan sebagai baris-baris {@link Biaya} yang menunjuk balik
 * ke sini.
 *
 * <h3>Satu tabel untuk dua peran: tarif standar dan tarif khusus</h3>
 * Sama seperti pada tindakan, tarif standar dan <b>tarif khusus</b> ({@link TarifKhusus} — tarif
 * bagi peserta asuransi, komunitas, dokter, atau pasien tertentu) berbagi tabel yang sama,
 * {@code sirs.biaya_alat_medis_per_kelas}, dan dibedakan hanya oleh pola pengisian dua kolom yang
 * saling meniadakan:
 * <table border="1" summary="Pola pengisian kolom penentu peran baris">
 * <tr><th>Peran baris</th><th>{@code alat_medis}</th><th>{@code tarif_khusus_punya_alat_medis}</th></tr>
 * <tr><td>Tarif standar</td><td>terisi</td><td>{@code NULL}</td></tr>
 * <tr><td>Tarif khusus</td><td>{@code NULL}</td><td>terisi</td></tr>
 * </table>
 * Pada baris tarif khusus, alat yang dimaksud ditelusuri lewat
 * {@link TarifKhususPunyaAlatMedis#getAlatMedis()}, bukan dari kolom {@code alat_medis} yang
 * sengaja dikosongkan oleh {@code ais.action.master.sirs.util.CommonTarifAlatMedis}. Dengan
 * demikian tarif khusus <b>menggantikan</b> baris tarif dasar, bukan menimpanya: tidak pernah ada
 * dua baris yang sama-sama berlaku lalu harus diperebutkan. Pemilihan tarif khusus mana yang
 * berlaku dilakukan lebih dahulu oleh {@code CommonTarif.getTarif} dengan strategi generalisasi
 * bertingkat, dari kecocokan dokter/asuransi/komunitas/pasien yang paling spesifik hingga tarif
 * umum tanpa dimensi apa pun.
 *
 * <h3>Pembuatan baris otomatis (get-or-create)</h3>
 * {@code CommonTarifAlatMedis.getBiayaAlatMedisPerKelas} memakai strategi "ambil atau buat" yang
 * identik dengan padanan tindakannya: bila kombinasi alat/kelas/tarif-khusus belum memiliki baris,
 * baris baru langsung dibuat dan disimpan — berbiaya 0 untuk tarif standar, atau disalin dari
 * tarif standar pada kelas yang sama untuk tarif khusus. Akibatnya membuka layar transaksi dapat
 * menciptakan baris tarif baru, tarif yang belum dikonfigurasi berperilaku sebagai tarif nol tanpa
 * peringatan, dan — karena tidak ada indeks unik atas kombinasi (alat medis, kelas, tarif khusus) —
 * dua sesi paralel dapat menghasilkan baris kembar yang salah satunya menjadi baris siluman.
 *
 * <h3>Dua saklar perilaku</h3>
 * {@link #getPembagianBiayaDalamPersen()} menentukan arah turunan rincian {@link Biaya} (persen
 * atau rupiah — lihat {@link Biaya#getJumlah()} dan {@link Biaya#getPersen()}), sedangkan
 * {@link #getHargaBisaDirubahSaatTransaksi()} mengizinkan kasir menyunting harga saat transaksi,
 * dan hanya berpengaruh bila dipasangkan dengan mode persen. Keduanya bendera dua-arah yang benar
 * dengan getter yang menormalkan {@code null} menjadi {@code false}, sehingga baris lama yang
 * kolomnya masih {@code NULL} diperlakukan sebagai mode rupiah dengan harga terkunci.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — seluruh relasi dan kedua saklar menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — tarif bersifat global lintas unit.</li>
 * <li><b>Tanpa masa berlaku</b> — tidak ada kolom {@code mulai}/{@code sampai}, sehingga
 * penyuntingan angka tarif berlaku surut dan riwayatnya hanya tertelusur lewat Hibernate
 * Envers.</li>
 * <li>Berbeda dari {@link BiayaTindakanPerKelas#getKeterangan()} yang menormalkan {@code null}
 * menjadi string kosong, {@link #getKeterangan()} di sini tidak menormalkan apa pun — perbedaan
 * kecil antara dua kelas kembar yang perlu diketahui pemanggil yang menangani keduanya secara
 * seragam.</li>
 * </ul>
 *
 * @see Biaya rincian komponen pembentuk angka tarif ini
 * @see TarifKhususPunyaAlatMedis penanda bahwa baris ini adalah tarif khusus, bukan tarif standar
 * @see BiayaTindakanPerKelas kembaran entitas ini untuk tindakan/layanan
 * @see ais.action.master.sirs.util.CommonTarifAlatMedis resolusi dan pembuatan otomatis baris tarif
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "biaya_alat_medis_per_kelas")
public class BiayaAlatMedisPerKelas extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.biaya_alat_medis_per_kelas}, dibangkitkan basis data (IDENTITY). */
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
	 * Representasi teks baris tarif untuk komponen ZK. Metode ini memanggil {@link #getAlatMedis()}
	 * dan {@link #getKelasPerawatan()} lebih dahulu <b>semata-mata untuk efek sampingnya</b> —
	 * memaksa materialisasi proxy malas — lalu merangkai label dari field, bukan dari nilai kembalian
	 * kedua pemanggilan itu. Konsekuensinya {@code toString()} bukan operasi ringan: ia dapat
	 * memicu dua kueri basis data dan mengubah state objek.
	 *
	 * @return label baris tarif berisi alat medis, kelas perawatan, dan biaya
	 */
	public String toString() {
		getAlatMedis();
		getKelasPerawatan();
		return alatMedis + "- kelasPerawatan: " + kelasPerawatan + "- biaya: " + biaya;
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
	 * Mengembalikan cap waktu perubahan terakhir baris tarif ini. Karena entitas ini tidak memiliki
	 * masa berlaku, nilai inilah satu-satunya penanda kapan angka tarif terakhir disentuh.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan bebas atas baris tarif. */
	private String keterangan;

	/** Alat medis yang ditarifkan; terisi hanya pada baris tarif standar. */
	private AlatMedis alatMedis;

	/** Kelas perawatan yang menjadi sumbu pembeda tarif. */
	private KelasPerawatan kelasPerawatan;

	/** Penanda tarif khusus; terisi hanya pada baris tarif khusus, menggantikan {@link #alatMedis}. */
	private TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis;

	/** Nilai tarif dalam rupiah; hasil penjumlahan rincian {@link Biaya} yang menunjuk baris ini. */
	private Double biaya = 0.0;

	/** Saklar arah turunan rincian biaya: persen (benar) atau rupiah (salah). */
	private Boolean pembagianBiayaDalamPersen = false;

	/** Saklar izin penyuntingan harga saat transaksi berlangsung. */
	private Boolean hargaBisaDirubahSaatTransaksi = false;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public BiayaAlatMedisPerKelas() {
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
	 * Mengembalikan keterangan bebas baris tarif. Berbeda dari
	 * {@link BiayaTindakanPerKelas#getKeterangan()}, getter ini <b>tidak</b> menormalkan
	 * {@code null} menjadi string kosong, sehingga dapat mengembalikan {@code null}.
	 *
	 * @return keterangan, dapat {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
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
	 * Mengembalikan alat medis yang ditarifkan baris ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Bernilai {@code null} pada baris tarif khusus — pada baris semacam itu
	 * alatnya harus dibaca lewat {@link #getTarifKhususPunyaAlatMedis()}.
	 *
	 * @return alat medis yang ditarifkan, atau {@code null} bila baris ini tarif khusus
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alat_medis", nullable = true)
	public AlatMedis getAlatMedis() {
		alatMedis = check(alatMedis);
		return alatMedis;
	}

	/**
	 * Menetapkan alat medis yang ditarifkan. Pada baris tarif khusus, nilai ini justru <b>harus</b>
	 * dikosongkan ({@code null}) agar baris tidak terambil sebagai tarif standar oleh kueri resolusi
	 * yang menyaring dengan {@code isNull("tarifKhususPunyaAlatMedis")}.
	 *
	 * @param alatMedis alat medis yang ditarifkan, atau {@code null} untuk baris tarif khusus
	 */
	public void setAlatMedis(AlatMedis alatMedis) {
		this.alatMedis = alatMedis;
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
	 * {@code 0.0}. <b>Getter destruktif</b>.
	 *
	 * <p>
	 * Pada alur normal nilai ini bukan angka yang diketik operator secara langsung, melainkan hasil
	 * penjumlahan seluruh baris {@link Biaya} yang menunjuk baris tarif ini — penjumlahan yang
	 * dilakukan {@code CommonPendaftaranUtil} lalu ditulis balik lewat {@link #setBiaya(Double)}.
	 * Nilai ini pula yang menjadi basis {@link Biaya#getJumlahTotal()} selama baris biaya masih
	 * berupa cetakan tarif. Karena baris tarif yang dibuat otomatis berbiaya {@code 0.0}, tarif yang
	 * belum dikonfigurasi tidak dapat dibedakan dari tarif yang sengaja digratiskan, dan tidak ada
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
	 * turunan seluruh rincian {@link Biaya} milik baris tarif ini.
	 *
	 * @param pembagianBiayaDalamPersen {@code true} untuk mode persen, {@code false} untuk mode rupiah
	 */
	public void setPembagianBiayaDalamPersen(Boolean pembagianBiayaDalamPersen) {
		this.pembagianBiayaDalamPersen = pembagianBiayaDalamPersen;
	}

	/**
	 * Mengembalikan saklar izin penyuntingan harga saat transaksi, menormalkan {@code null} menjadi
	 * {@code false}. <b>Getter destruktif</b>. Saklar ini baru berpengaruh bila dipasangkan dengan
	 * mode persen.
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

	/**
	 * Mengembalikan penanda tarif khusus baris ini. <b>Getter destruktif</b> ({@code check(...)}).
	 * Terisinya nilai ini menandai bahwa baris adalah tarif khusus dan bahwa {@link #getAlatMedis()}
	 * pada baris ini pasti {@code null}.
	 *
	 * @return penanda tarif khusus, atau {@code null} bila baris ini tarif standar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tarif_khusus_punya_alat_medis", nullable = true)
	public TarifKhususPunyaAlatMedis getTarifKhususPunyaAlatMedis() {
		tarifKhususPunyaAlatMedis = check(tarifKhususPunyaAlatMedis);
		return tarifKhususPunyaAlatMedis;
	}

	/**
	 * Menjadikan baris ini tarif khusus dengan menautkannya ke penanda tarif khusus. Harus
	 * dipasangkan dengan {@code setAlatMedis(null)} agar pola pembeda peran tetap konsisten; tidak
	 * ada penjaga di lapisan model yang memaksanya.
	 *
	 * @param tarifKhususPunyaAlatMedis penanda tarif khusus, atau {@code null} untuk tarif standar
	 */
	public void setTarifKhususPunyaAlatMedis(TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis) {
		this.tarifKhususPunyaAlatMedis = tarifKhususPunyaAlatMedis;
	}
}
