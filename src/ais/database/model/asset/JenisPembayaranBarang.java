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
 * Entity master <b>Jenis Pembayaran Barang</b> (tabel {@code asset.jenis_pembayaran_barang}) —
 * katalog {@code kode/nama/keterangan/aktif} berisi metode pembayaran pengadaan aset (mis.
 * "Pembayaran Tunai", transfer, dll.), masing-masing dipasangkan opsional dengan
 * {@link #getAkun() akun kredit}. Live dipakai oleh {@code PembayaranPengadaanMasterAsset},
 * {@code PembayaranDpMasterAsset}, dan {@code PembayaranTerminMasterAsset} untuk menentukan
 * akun kredit jurnal saat posting (lihat {@code ais.action.master.asset.PostingPembayaranAction},
 * {@code PostingPembayaranDpAction}, {@code PostingPembayaranTerminAction}) — bukan hanya
 * katalog tampilan combo.
 *
 * <h2>Seed default {@link #reloadDefault()} vs cache statis {@link #PEMBAYARAN_TUNAI}</h2>
 * <p>Method statis ini dipanggil sekali saat startup aplikasi dari
 * {@code ais.common.InitData#reloadDefaults()}. Efek yang benar-benar dipakai hanyalah <b>efek
 * sampingnya</b>: memastikan minimal satu baris "Pembayaran Tunai" ada di tabel bila tabel masih
 * kosong. Nilai yang ditaruh ke field statis {@link #PEMBAYARAN_TUNAI} sendiri <b>tidak pernah
 * dibaca</b> di tempat lain di seluruh repo (berbeda dari pola serupa di
 * {@code JenisPajakPpn.PPN} atau {@code JenisPenerimaanBarang.DEFAULT_PENERIMAAN_BARANG_JASA}
 * yang memang dipakai sebagai nilai fallback lazy-default) — jangan berasumsi kode lain
 * bergantung pada cache statis ini tetap terisi.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see ais.database.model.asset.PembayaranPengadaanMasterAsset
 * @see ais.action.master.asset.JenisPembayaranBarangAction
 * @see ais.action.master.asset.PostingPembayaranAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_pembayaran_barang")
public class JenisPembayaranBarang extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_pembayaran_barang}. */
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
	 * Cache statis in-memory hasil {@link #reloadDefault()} terakhir kali dipanggil. Diisi ulang
	 * setiap kali {@code reloadDefault()} dijalankan (sekali di startup lewat
	 * {@code InitData#reloadDefaults()}); lihat catatan kelas — field ini <b>tidak dibaca</b>
	 * oleh kode lain di repo, jadi jangan mengandalkannya sebagai nilai default yang sudah tentu
	 * konsisten dengan database bila dibaca dari thread lain setelah startup.
	 */
	public static JenisPembayaranBarang PEMBAYARAN_TUNAI = null;

	/**
	 * Memastikan tabel {@code asset.jenis_pembayaran_barang} punya minimal satu baris seed
	 * ("Pembayaran Tunai", kode "001", aktif) dan mengisi {@link #PEMBAYARAN_TUNAI} dengan baris
	 * pertama (urut {@code id} asc) yang ditemukan atau baris seed yang baru dibuat.
	 *
	 * <p><b>Cara kerja:</b> membuka sesi Hibernate native lewat
	 * {@link ais.database.hibernate.HibernateUtil#currentNativeSession()}, mengambil satu baris
	 * pertama; bila tabel kosong, membuat dan menyimpan baris default dalam transaksi eksplisit
	 * ({@code begin()}/{@code commit()} manual, bukan lewat lapisan DAO), lalu menutup sesi lewat
	 * {@link ais.database.hibernate.HibernateUtil#closeSession()} di akhir — termasuk saat
	 * cabang seed tidak dieksekusi.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> sekali secara asinkron saat startup aplikasi dari
	 * {@code ais.common.InitData#reloadDefaults()}. Tidak dipanggil ulang secara otomatis
	 * setelah itu — nilai {@link #PEMBAYARAN_TUNAI} yang tersimpan bisa jadi kedaluwarsa
	 * relatif terhadap perubahan tabel berikutnya, tapi karena field statis itu tidak dibaca di
	 * mana pun, kekedaluwarsaannya tidak berdampak praktis saat ini.</p>
	 *
	 * <p><b>Tidak ada penanganan exception</b> — kegagalan Hibernate (mis. koneksi database
	 * putus) menyebar sebagai unchecked exception ke pemanggil ({@code Runnable} di executor
	 * {@code InitData}), yang berarti kegagalan seed satu entity bisa menghentikan sisa urutan
	 * {@code reloadDefaults()} bila tidak ditangkap di level pemanggil.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		PEMBAYARAN_TUNAI = (JenisPembayaranBarang) session.createCriteria(JenisPembayaranBarang.class).setMaxResults(1)
				.addOrder(Order.asc("id")).uniqueResult();
		if (PEMBAYARAN_TUNAI == null) {
			PEMBAYARAN_TUNAI = new JenisPembayaranBarang();
			PEMBAYARAN_TUNAI.setKode("001");
			PEMBAYARAN_TUNAI.setAktif(true);
			PEMBAYARAN_TUNAI.setNama("Pembayaran Tunai");
			PEMBAYARAN_TUNAI.setKeterangan("Pembayaran Tunai");
			session.getTransaction().begin();
			session.save(PEMBAYARAN_TUNAI);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/** Kode singkat metode pembayaran. */
	private String kode;
	/** Akun buku besar kredit yang dipakai saat posting pembayaran dengan metode ini. */
	private Akun akun;
	/** Nama metode pembayaran. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPembayaranBarang() {
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
	 * Mengembalikan kode singkat metode pembayaran. Berbeda dari kolom lain, {@code null}
	 * dinormalisasi menjadi string kosong (bukan {@code null}) agar aman dipakai langsung di
	 * tampilan tanpa null-check tambahan.
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat metode pembayaran. Tidak dianotasi {@code @Column} — kolom dipetakan
	 * lewat konvensi nama Hibernate default.
	 *
	 * @param kode kode metode pembayaran
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama metode pembayaran, di-trim untuk menghindari whitespace tak sengaja
	 * dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama metode pembayaran. Tidak melakukan trim di sisi setter — trimming terjadi
	 * hanya saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama metode pembayaran
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
	 * Mengembalikan status aktif metode pembayaran. {@code null} database ditafsirkan sebagai
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
	 * Mengembalikan akun buku besar kredit yang dipasangkan dengan metode pembayaran ini,
	 * meresolusi proxy lazy Hibernate lewat {@link GeneralValueObject#check(Object)} sebelum
	 * dikembalikan agar pemanggil tidak menerima proxy yang bisa meledak di luar sesi Hibernate.
	 * Dipakai langsung oleh mesin posting pembayaran ({@code PostingPembayaranAction} dan
	 * sejenisnya) sebagai akun kredit jurnal, dengan opsi override manual di layar posting.
	 *
	 * @return akun kredit yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Mengisi akun buku besar kredit.
	 *
	 * @param akun akun kredit, boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

}
