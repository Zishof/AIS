package ais.database.model.asset;

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

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Entity master <b>Jenis Penghapusan Barang</b> (tabel {@code asset.jenis_penghapusan_barang},
 * nama Java {@code JenisPengapusanBarang} — perhatikan beda ejaan "Pengapusan" vs
 * "Penghapusan" di nama tabel) — katalog alasan penghapusan aset (mis. "Barang Hilang", rusak,
 * dijual, dihibahkan), masing-masing dipasangkan dengan <b>sepasang akun write-off</b>:
 * {@link #getDebet()} dan {@link #getKredit()}. Dipilih pada
 * {@code PenghapusanMasterAsset} lewat layar CRUD
 * {@code ais.action.master.asset.JenisPengapusanBarangAction}.
 *
 * <h2>Pasangan {@link #getDebet()}/{@link #getKredit()}: terhubung mesin akunting, tapi lewat satu jalur sempit</h2>
 * <p>Pasangan akun ini benar-benar dipakai untuk menjurnal write-off nilai buku aset —
 * tapi <b>hanya</b> lewat {@code PenghapusanMasterAssetAction#postingSemua(...)}, API batch
 * posting untuk dasbor "Draft Jurnal POS" (dok 61 butir D), yang membaca
 * {@code getJenisPengapusanBarang().getDebet()}/{@code getKredit()} untuk menjurnal Dr/Cr
 * senilai total harga perolehan dokumen. Komentar pada method itu sendiri mencatat bahwa
 * pasangan akun ini "sudah lama tersedia dan dirawat di layar masternya" sebelum
 * {@code postingSemua} ditulis untuk benar-benar memakainya — jalur posting per-dokumen biasa
 * di layar UI tidak dikonfirmasi memakai pasangan ini secara terpisah. <b>Batasan yang
 * disengaja:</b> satu pasang akun per jenis penghapusan berarti pelepasan akumulasi penyusutan
 * dan pengakuan rugi tidak dipecah otomatis oleh jalur ini; dokumen ber-jenis tanpa akun
 * lengkap dilewati oleh {@code postingSemua} dan tetap terhitung draf.</p>
 *
 * <h2>Cache statis {@link #PEMBAYARAN_TUNAI}: nama menyesatkan, tidak dibaca di luar kelas</h2>
 * <p>Nama field ini adalah salin-tempel dari pola {@code JenisPembayaranBarang.PEMBAYARAN_TUNAI}
 * — <b>tidak ada hubungannya</b> dengan pembayaran tunai; isinya baris penghapusan pertama
 * ("Barang Hilang" sebagai seed). Method {@link #reloadDefault()} dipanggil dari
 * {@code InitData#reloadDefaults()} saat startup untuk efek sampingnya (memastikan baris seed
 * ada), tapi field statis ini sendiri <b>tidak dibaca</b> di tempat lain dalam repo — sama
 * seperti pola {@code JenisPembayaranBarang.PEMBAYARAN_TUNAI}, bukan seperti
 * {@code JenisPajakPpn.PPN} yang memang dipakai luas sebagai fallback.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see ais.database.model.asset.PenghapusanMasterAsset
 * @see ais.action.master.asset.JenisPengapusanBarangAction
 * @see ais.action.master.asset.PenghapusanMasterAssetAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_penghapusan_barang")
public class JenisPengapusanBarang extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_penghapusan_barang}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Guard di awal method membuat setter ini diam-diam mengabaikan
	 * nilai {@code null}/blank — tidak menghapus nilai lama, berbeda dari setter lain di kelas
	 * ini yang selalu menimpa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)} membuat
	 * nilai {@code null}/blank diabaikan, bukan menghapus nilai lama.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu
	 * serta identitas pengguna aktif. Dipicu otomatis oleh Hibernate lewat
	 * {@link javax.persistence.PreUpdate}, tidak dipanggil manual di tempat lain.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat ini pada konstruksi
	 * objek, lalu ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null} karena field diinisialisasi
	 *         saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug dan tampilan combobox generik: {@code id} diikuti
	 * {@link #nama}.
	 *
	 * @return string berformat {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statis in-memory hasil {@link #reloadDefault()} terakhir kali dipanggil. Meski
	 * namanya mengingatkan pembayaran tunai (salin-tempel dari
	 * {@code JenisPembayaranBarang.PEMBAYARAN_TUNAI}), isinya baris seed penghapusan pertama
	 * ("Barang Hilang"). Tidak dibaca oleh kode lain di repo — lihat catatan kelas.
	 */
	public static JenisPengapusanBarang PEMBAYARAN_TUNAI = null;

	/**
	 * Memastikan tabel {@code asset.jenis_penghapusan_barang} punya minimal satu baris seed
	 * ("Barang Hilang", kode "001", aktif) dan mengisi {@link #PEMBAYARAN_TUNAI} dengannya.
	 *
	 * <p><b>Cara kerja:</b> identik polanya dengan {@code JenisPembayaranBarang.reloadDefault()}
	 * — membuka sesi Hibernate native, mengambil baris pertama urut {@code id} asc, membuat
	 * baris seed dalam transaksi eksplisit bila kosong, lalu menutup sesi.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> sekali secara asinkron saat startup aplikasi dari
	 * {@code ais.common.InitData#reloadDefaults()}. Efek yang benar-benar dipakai hanyalah efek
	 * sampingnya (baris seed di database); nilai {@link #PEMBAYARAN_TUNAI} sendiri tidak dibaca
	 * di tempat lain — lihat catatan kelas.</p>
	 *
	 * <p><b>Tidak ada penanganan exception</b> — kegagalan Hibernate menyebar sebagai unchecked
	 * exception ke pemanggil.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		PEMBAYARAN_TUNAI = (JenisPengapusanBarang) session.createCriteria(JenisPengapusanBarang.class).setMaxResults(1)
				.addOrder(Order.asc("id")).uniqueResult();
		if (PEMBAYARAN_TUNAI == null) {
			PEMBAYARAN_TUNAI = new JenisPengapusanBarang();
			PEMBAYARAN_TUNAI.setKode("001");
			PEMBAYARAN_TUNAI.setAktif(true);
			PEMBAYARAN_TUNAI.setNama("Barang Hilang");
			PEMBAYARAN_TUNAI.setKeterangan("Barang Hilang");
			session.getTransaction().begin();
			session.save(PEMBAYARAN_TUNAI);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/** Kode singkat jenis penghapusan. */
	private String kode;
	/** Akun buku besar sisi debet write-off (biasanya beban/rugi penghapusan aset). */
	private Akun debet;
	/** Akun buku besar sisi kredit write-off (biasanya akun aset yang dihapuskan). */
	private Akun kredit;
	/** Nama jenis/alasan penghapusan (mis. "Barang Hilang", "Rusak Berat", "Dijual"). */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPengapusanBarang() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id, atau {@code null} untuk instance baru yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat jenis penghapusan. {@code null} dinormalisasi menjadi string
	 * kosong agar aman dipakai langsung di tampilan tanpa null-check tambahan.
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat jenis penghapusan. Tidak dianotasi {@code @Column} — kolom dipetakan
	 * lewat konvensi nama Hibernate default.
	 *
	 * @param kode kode jenis penghapusan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis/alasan penghapusan, di-trim untuk menghindari whitespace tak
	 * sengaja dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis/alasan penghapusan. Tidak melakukan trim di sisi setter — trimming
	 * terjadi hanya saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis/alasan penghapusan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif jenis penghapusan. {@code null} database ditafsirkan sebagai
	 * aktif, sehingga baris lama yang belum pernah disentuh field ini akan selalu tampil aktif.
	 *
	 * @return {@code true} bila aktif atau belum pernah di-set; {@code false} hanya bila
	 *         eksplisit pernah di-set nonaktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif. Tidak ada normalisasi di setter — nilai {@code null} yang di-set di
	 * sini tetap tersimpan {@code null} dan akan dibaca sebagai aktif oleh {@link #getAktif()}.
	 *
	 * @param aktif status aktif baru; {@code null} diperbolehkan dan berarti "aktif" saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan akun buku besar sisi debet write-off, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar pemanggil tidak
	 * menerima proxy yang bisa meledak di luar sesi Hibernate. Dibaca oleh
	 * {@code PenghapusanMasterAssetAction#postingSemua(...)} sebagai sisi debet jurnal
	 * write-off — lihat catatan kelas untuk cakupan pemakaiannya yang sempit (hanya jalur API
	 * batch posting).
	 *
	 * @return akun debet yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "debet", nullable = true)
	public Akun getDebet() {
		debet = check(debet);
		return debet;
	}

	/**
	 * Mengisi akun buku besar sisi debet write-off.
	 *
	 * @param debet akun debet, boleh {@code null}
	 */
	public void setDebet(Akun debet) {
		this.debet = debet;
	}

	/**
	 * Mengembalikan akun buku besar sisi kredit write-off, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan. Dibaca oleh
	 * {@code PenghapusanMasterAssetAction#postingSemua(...)} sebagai sisi kredit jurnal
	 * write-off — lihat catatan kelas.
	 *
	 * @return akun kredit yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kredit", nullable = true)
	public Akun getKredit() {
		kredit = check(kredit);
		return kredit;
	}

	/**
	 * Mengisi akun buku besar sisi kredit write-off.
	 *
	 * @param kredit akun kredit, boleh {@code null}
	 */
	public void setKredit(Akun kredit) {
		this.kredit = kredit;
	}

}
