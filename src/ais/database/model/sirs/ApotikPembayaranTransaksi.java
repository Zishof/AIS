package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
import ais.database.model.koperasi.CaraPembayaranKoperasi;

/**
 * Metode pembayaran yang dipakai satu transaksi apotik (IR-07).
 *
 * <p><b>Mengapa tabel terpisah, bukan kolom baru di {@code TransaksiMedis}?</b>
 * {@code TransaksiMedis} dipakai bersama modul rumah sakit dan sudah
 * {@code @Audited}; menambah kolom di sana menuntut ALTER pada tabel audit
 * lama (gotcha Envers) dan menyentuh jalur yang tidak sedang dimodernisasi.
 * Tabel BARU dibuat otomatis oleh {@code hbm2ddl=update} berikut tabel
 * auditnya, sehingga tidak ada migrasi manual sama sekali.</p>
 *
 * <p>Satu transaksi boleh punya lebih dari satu baris; sejak IR-11 hal itu
 * benar-benar dipakai untuk pembayaran terpisah (mis. sebagian tunai,
 * sebagian QRIS). Penjumlahan nominal divalidasi pemanggil ({@code bayar}),
 * bukan entity ini.</p>
 *
 * <p><b>PERHATIAN untuk perubahan berikutnya.</b> Kalimat di atas tentang
 * "tidak ada migrasi manual" hanya berlaku saat tabel ini BELUM ada di
 * produksi. Sejak ia terbentuk, {@code hbm2ddl=update} menambah kolom baru ke
 * tabel utama tetapi TIDAK ke tabel {@code __audit}-nya, sehingga INSERT audit
 * gagal dan seluruh transaksi ter-rollback. Kolom {@code tunai}/{@code kembalian}
 * (IR-11) karena itu disertai
 * {@code webapp/sql/migrasi_apotik_ir11_pembayaran.sql} yang WAJIB dijalankan
 * sebelum restart.</p>
 *
 * <h3>Bentuk penjagaan penjumlahan yang dimaksud di atas</h3>
 *
 * <p>Karena entity ini tidak dapat memeriksa apa pun tentang saudara-saudaranya,
 * ada gunanya menyebut di sini apa yang sebenarnya dijaga pemanggil, supaya
 * jalur pembuatan baru tidak melewatkannya. Pada cabang pembayaran terpisah,
 * {@code ApotikApiHelper.bayar} menjumlahkan seluruh nominal yang dikirim dan
 * MENOLAK transaksi bila selisihnya terhadap total melebihi setengah rupiah.
 * Tanpa pagar itu, penjualan dapat terbukukan penuh sementara uang yang masuk
 * kurang, dan selisihnya baru muncul saat tutup kas — pada saat mana ia sudah
 * berupa satu angka gabungan yang tidak dapat ditelusuri kembali ke transaksi
 * mana pun.</p>
 *
 * <p>Pada cabang metode tunggal, pagar itu tidak diperlukan karena nominalnya
 * tidak datang dari klien sama sekali: helper mengisinya dengan total transaksi.
 * Yang perlu diketahui adalah cabang ketiga — bila permintaan tidak menyertakan
 * metode pembayaran apa pun, transaksi tetap tersimpan dan TIDAK ada satu pun
 * baris pembayaran yang lahir. Transaksi semacam itu sah menurut sistem tetapi
 * tak berjejak uang; {@link ApotikSesiKas} menampilkannya terpisah sebagai
 * "penjualan tanpa metode" justru agar ia tidak tersamar sebagai selisih kas.</p>
 *
 * @see ApotikSesiKas rekonsiliasi laci kasir yang menjumlahkan baris-baris ini
 * @see ApotikPbfPembayaran padanan sisi utang: pembayaran kepada distributor obat
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_pembayaran_transaksi")
public class ApotikPembayaranTransaksi extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Transaksi penjualan yang dibayar. Wajib. */
	private TransaksiMedis transaksi;

	/** Master cara pembayaran; boleh kosong pada data lama. */
	private CaraPembayaranKoperasi caraBayar;

	/** Salinan nama metode saat transaksi -- master boleh berubah/dinonaktifkan. */
	private String namaCaraBayar;

	/** Nominal yang dibukukan lewat metode ini. */
	private Double nominal;

	/**
	 * Uang tunai yang DITERIMA kasir dan kembalian yang diberikan (IR-11).
	 *
	 * <p>Keduanya hanya bermakna untuk metode yang memberi kembalian. Sebelum
	 * kolom ini ada, angka tersebut hanya hidup di layar kasir dan hilang
	 * begitu transaksi selesai -- akibatnya rekonsiliasi laci tidak dapat
	 * membedakan "kembalian belum diberikan" dari "uang kurang". Nominal tetap
	 * jumlah yang DIBUKUKAN; tunai/kembalian adalah catatan penyerahan uang.</p>
	 */
	private Double tunai;

	/** Kembalian yang diserahkan kepada pembayar; pasangan {@link #tunai}. */
	private Double kembalian;

	/** Nomor referensi kanal (nomor approval EDC/QRIS) bila ada. */
	private String referensi;

	/** Waktu pembayaran; dasar penjaringan periode sesi kas. */
	private Date waktu;

	/** Nama tampil pelaku pencatatan (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku pencatatan (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Praktis tidak pernah berjalan: baris pembayaran ditulis sekali di dalam
	 * transaksi penjualan dan tidak pernah disunting sesudahnya. Dipertahankan
	 * demi keseragaman, supaya perubahan yang seharusnya tidak terjadi tetap
	 * meninggalkan jejak waktu bila toh terjadi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Representasi teks: kunci baris dan nama metode, dipisah tanda hubung.
	 *
	 * <p>Membaca field langsung, bukan lewat getter, sehingga aman dipanggil
	 * pada objek yang sudah lepas dari sesi Hibernate. Bagian yang kosong
	 * diganti string kosong supaya hasilnya tidak pernah memuat kata "null".</p>
	 *
	 * @return teks ringkas untuk log dan layar
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (namaCaraBayar == null ? "" : namaCaraBayar);
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * <p>{@code insertable = false}: nilai apa pun di objek Java tidak ikut dalam
	 * INSERT.</p>
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Transaksi penjualan yang dibayar baris ini.
	 *
	 * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
	 * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
	 * sesinya menjadi {@code null}, mencegah {@code LazyInitializationException}
	 * ketika objek dibaca di luar sesi — misalnya saat diserialkan ke JSON.
	 * Karena itu memanggil getter ini dapat mengubah keadaan objek, dan dua
	 * panggilan berturut-turut tidak dijamin sama bila di antaranya sesi
	 * ditutup.</p>
	 *
	 * <p>{@code nullable = false} — pembayaran yang tidak menunjuk transaksi
	 * tidak dapat dijumlahkan ke mana pun dan akan mengambang di luar seluruh
	 * rekonsiliasi.</p>
	 *
	 * @return transaksi yang dibayar, atau {@code null} bila proksinya lepas
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi", nullable = false)
	public TransaksiMedis getTransaksi() { transaksi = check(transaksi); return transaksi; }

	/**
	 * Menetapkan transaksi yang dibayar.
	 *
	 * @param transaksi transaksi penjualan; wajib terisi sebelum disimpan
	 */
	public void setTransaksi(TransaksiMedis transaksi) { this.transaksi = transaksi; }

	/**
	 * Master cara pembayaran yang dipakai.
	 *
	 * <p>Berlaku catatan getter destruktif yang sama seperti
	 * {@link #getTransaksi()}.</p>
	 *
	 * <p>Sengaja {@code nullable = true}, berbeda dari
	 * {@link ApotikPbfPembayaran#getCaraBayar()} yang mewajibkannya. Kelonggaran
	 * itu berpasangan dengan {@link #getNamaCaraBayar()}: relasi boleh putus
	 * tanpa membuat baris kehilangan makna, karena namanya sudah disalin. Untuk
	 * catatan penerimaan uang yang harus tetap terbaca bertahun-tahun kemudian,
	 * itu pilihan yang tepat — master boleh dibersihkan tanpa mengaburkan
	 * riwayat.</p>
	 *
	 * <p>Perhatikan bahwa {@link ApotikSesiKas} membedakan tunai dari non-tunai
	 * dengan menelusuri master ini, bukan salinan namanya. Baris yang relasinya
	 * putus karena itu tidak akan terhitung sebagai tunai maupun non-tunai pada
	 * rekonsiliasi laci — ia muncul sebagai selisih antara penjualan berjalan
	 * dan jumlah kedua kelompok.</p>
	 *
	 * @return master cara pembayaran, atau {@code null}
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_bayar", nullable = true)
	public CaraPembayaranKoperasi getCaraBayar() { caraBayar = check(caraBayar); return caraBayar; }

	/**
	 * Menetapkan master cara pembayaran.
	 *
	 * @param caraBayar cara pembayaran, boleh {@code null}
	 */
	public void setCaraBayar(CaraPembayaranKoperasi caraBayar) { this.caraBayar = caraBayar; }

	/**
	 * Salinan nama metode pada saat transaksi terjadi.
	 *
	 * <p>Bentuk snapshot yang disengaja: master boleh berubah nama, boleh
	 * dinonaktifkan, boleh dihapus, dan struk yang dicetak ulang tahun depan
	 * harus tetap menyebut metode yang benar-benar dipakai hari itu. Menyalin
	 * nama membuat riwayat tidak ikut berubah ketika master berubah.</p>
	 *
	 * <p>Karena itu salinan ini TIDAK boleh "diperbaiki" agar mengikuti master —
	 * perbedaan antara keduanya bukan ketidakcocokan data melainkan justru
	 * informasi: ia menyatakan bahwa master sudah berubah sejak transaksi
	 * tersebut.</p>
	 *
	 * @return nama metode saat transaksi, atau {@code null}
	 */
	@Column(name = "nama_cara_bayar", length = 120)
	public String getNamaCaraBayar() { return namaCaraBayar; }

	/**
	 * Menetapkan salinan nama metode.
	 *
	 * @param namaCaraBayar nama metode saat transaksi
	 */
	public void setNamaCaraBayar(String namaCaraBayar) { this.namaCaraBayar = namaCaraBayar; }

	/**
	 * Nominal yang DIBUKUKAN lewat metode ini.
	 *
	 * <p>Mengembalikan {@code 0} bila kosong, sehingga penjumlahan tidak perlu
	 * berjaga terhadap {@code null}. Inilah angka yang mengikat — yang
	 * dijumlahkan {@link ApotikSesiKas} saat tutup kas dan yang wajib berjumlah
	 * sama dengan total transaksi menurut pagar di {@code bayar}. Bandingkan
	 * dengan {@link #getTunai()}, yang merupakan catatan penyerahan uang dan
	 * bukan angka pembukuan.</p>
	 *
	 * <p>Entity tidak menolak nilai negatif maupun nol; seluruh penjagaan
	 * kewajaran ada di pemanggil.</p>
	 *
	 * @return nominal yang dibukukan; {@code 0} bila kosong
	 */
	@Column(name = "nominal", nullable = false)
	public Double getNominal() { return nominal == null ? Double.valueOf(0) : nominal; }

	/**
	 * Menetapkan nominal yang dibukukan.
	 *
	 * @param nominal nominal
	 */
	public void setNominal(Double nominal) { this.nominal = nominal; }

	/**
	 * Uang tunai yang diterima kasir dari pembayar.
	 *
	 * <p>Mengembalikan {@code null} apa adanya — sengaja TIDAK diganti nol,
	 * berbeda dari {@link #getNominal()}. Perbedaan itu bermakna: {@code null}
	 * di sini berarti "tidak dicatat" (metode non-tunai, atau transaksi sebelum
	 * IR-11), sedangkan nol berarti "dicatat, dan jumlahnya nol". Mengganti
	 * kosong dengan nol akan menghapus perbedaan itu dan membuat transaksi lama
	 * tampak seperti kasir menerima uang nol rupiah.</p>
	 *
	 * <p>Bersama {@link #getKembalian()}, kolom ini adalah catatan penyerahan
	 * uang, BUKAN angka pembukuan. Yang masuk buku tetap
	 * {@link #getNominal()}.</p>
	 *
	 * <p><b>Tidak ada pemeriksaan kesesuaian.</b> Nilai tunai dan kembalian
	 * datang dari klien apa adanya; tidak ada satu pun tempat yang memeriksa
	 * bahwa {@code tunai - nominal} sama dengan {@code kembalian}, atau bahkan
	 * bahwa tunai tidak kurang dari nominal. Ketiganya karena itu dapat
	 * tersimpan dalam keadaan yang tidak mungkin terjadi secara fisik, dan
	 * rekonsiliasi laci tidak akan menangkapnya karena
	 * {@link ApotikSesiKas} menjumlahkan nominal, bukan tunai. Siapa pun yang
	 * kelak membangun laporan atas ketiga angka ini perlu memeriksanya
	 * sendiri.</p>
	 *
	 * @return tunai yang diterima, atau {@code null} bila tidak dicatat
	 */
	@Column(name = "tunai")
	public Double getTunai() { return tunai; }

	/**
	 * Menetapkan uang tunai yang diterima kasir.
	 *
	 * @param tunai tunai diterima, boleh {@code null}
	 */
	public void setTunai(Double tunai) { this.tunai = tunai; }

	/**
	 * Kembalian yang diserahkan kepada pembayar.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #getTunai()}: {@code null}
	 * berarti tidak dicatat, bukan nol, dan tidak ada pemeriksaan kesesuaian
	 * terhadap tunai maupun nominal.</p>
	 *
	 * @return kembalian yang diberikan, atau {@code null} bila tidak dicatat
	 */
	@Column(name = "kembalian")
	public Double getKembalian() { return kembalian; }

	/**
	 * Menetapkan kembalian yang diserahkan.
	 *
	 * @param kembalian kembalian, boleh {@code null}
	 */
	public void setKembalian(Double kembalian) { this.kembalian = kembalian; }

	/**
	 * Nomor referensi kanal (mis. nomor approval EDC / QRIS) bila ada.
	 *
	 * <p>Inilah kolom yang dijelaskan keterangan tersebut; sebelum revisi ini
	 * keterangan itu tertulis di atas {@code getTunai()} dan karena itu
	 * menyesatkan pembacanya. Isinya dipakai untuk mencocokkan penerimaan
	 * non-tunai apotek dengan laporan penyelenggara pembayaran ketika keduanya
	 * tidak sama. Teks bebas, tidak dijaga unik, dan tidak divalidasi
	 * bentuknya.</p>
	 *
	 * @return nomor referensi kanal, atau {@code null}
	 */
	@Column(name = "referensi", length = 160)
	public String getReferensi() { return referensi; }

	/**
	 * Menetapkan nomor referensi kanal.
	 *
	 * @param referensi nomor referensi
	 */
	public void setReferensi(String referensi) { this.referensi = referensi; }

	/**
	 * Waktu pembayaran.
	 *
	 * <p>Dasar penjaringan periode pada rekonsiliasi kas: {@link ApotikSesiKas}
	 * menjumlahkan pembayaran antara waktu sesi dibuka dan waktu ia ditutup.
	 * Karena baris ini ditulis di dalam transaksi penjualan yang sama, waktunya
	 * praktis selalu sama dengan waktu penjualan.</p>
	 *
	 * @return waktu pembayaran
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }

	/**
	 * Menetapkan waktu pembayaran.
	 *
	 * @param waktu waktu pembayaran
	 */
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	/**
	 * Nama tampil pelaku pencatatan (bayangan audit).
	 *
	 * <p>Pada baris pembayaran, ini adalah kasir yang menerima uangnya —
	 * jawaban atas pertanyaan yang paling sering muncul ketika laci tidak
	 * cocok.</p>
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	public String getOleh() { return oleh; }

	/**
	 * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
	 *
	 * <p>Menolak {@code null} dan teks berisi spasi saja secara diam. Bentuk ini
	 * seragam di basis kode dan merupakan keharusan teknis, bukan kelalaian.
	 * Kolom {@code oleh}/{@code oleh_id} adalah bayangan audit yang menempel
	 * pada barisnya sendiri, dan entity di sini melewati jalur-jalur yang
	 * menyalin seluruh properti tanpa memilah mana yang bermakna — pengikatan
	 * formulir, pemetaan dari JSON, penyalinan objek. Satu penyalinan lugu
	 * dengan string kosong sudah cukup untuk menghapus nama kasir yang benar,
	 * dan baris itu tidak menyimpan nilai sebelumnya di mana pun.</p>
	 *
	 * <p>Untuk catatan penerimaan uang, pertanyaan "siapa yang menerima" adalah
	 * pertanyaan pertama saat selisih kas ditemukan. Harga dari penjagaan ini:
	 * nilai tidak dapat dikosongkan kembali lewat setter — harga yang benar
	 * untuk kolom yang hanya boleh bertambah jelas.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }

	/**
	 * Identitas akun pelaku pencatatan (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	public String getOlehId() { return olehId; }

	/**
	 * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Praktis selalu sama dengan waktu pembuatan baris, karena baris
	 * pembayaran tidak pernah disunting.</p>
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
