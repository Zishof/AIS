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
 * Entity master <b>Jenis Pajak Barang</b> (tabel {@code asset.jenis_pajak_barang}) — katalog
 * tarif <b>PPh (Pajak Penghasilan/withholding tax, mis. PPh 23)</b>, pasangan
 * {@link JenisPajakPpn} (PPN) sebagai dua master tarif yang benar-benar dibaca mesin akunting
 * lewat {@code ais.database.model.akunting.Pajak.getJenisPajakBarang()}. Berbeda dari
 * {@code AkunPajak} di paket {@code akunting} (yang javadoc-nya sendiri menegaskan <b>bukan</b>
 * master tarif yang dipakai), kelas ini benar-benar menggerakkan angka: {@link #getPersen()}
 * dipakai untuk menghitung nominal PPh, dan {@link #getAkun()} untuk akun tujuan setoran
 * langsung.
 *
 * <h2>{@link #getAkunDanaTitipan()}: jalur "dana titipan" yang aktif dipakai lintas mesin posting</h2>
 * <p>Berbeda dari {@link #getAkun()} (setoran langsung ke kas negara), akun ini dipakai saat
 * PPh <b>ditahan sebagai dana titipan</b> (escrow) alih-alih langsung disetor — dibaca luas
 * oleh {@code ProsesTransferAction}, {@code PostingProsesTransferAction}, dan
 * {@code PostingPertangungjawabanPajakAction} untuk menentukan akun kredit/debit jurnal saat
 * memproses transfer dan pertanggungjawaban pajak. Bila field ini kosong pada baris tarif yang
 * dipakai, mesin posting terkait jatuh ke jalur lain (bukan dana titipan) — perubahan nilai ini
 * langsung berdampak pada baris mana yang dianggap "dana titipan" oleh proses transfer.</p>
 *
 * <h2>{@link #reloadDefault()} dan cache {@link #PEMBAYARAN_TUNAI}: kode mati, tidak seperti saudaranya</h2>
 * <p>Sekilas method dan pola ini identik dengan {@code JenisPembayaranBarang.reloadDefault()}
 * dan {@code JenisPajakPpn.reloadDefault()}. Bedanya: penelusuran seluruh repo memastikan
 * {@code JenisPajakBarang.reloadDefault()} <b>tidak pernah dipanggil di mana pun</b>. Daftar
 * seed startup {@code ais.common.InitData#reloadDefaults()} memanggil
 * {@code JenisPembayaranBarang.reloadDefault()}, {@code JenisPekerjaanPenyedia.reloadDefault()},
 * {@code JenisPajakPpn.reloadDefault()}, {@code JenisPengapusanBarang.reloadDefault()}, dan
 * {@code JenisPenerimaanBarang.reloadDefault()} — tapi <b>bukan</b> milik kelas ini; kelas ini
 * hanya muncul di daftar {@code initClasses(...)} terpisah yang sekadar memanasi metadata
 * entity, bukan memanggil seed data. Akibatnya baris seed "PPH23" 1% tidak pernah dibuat
 * otomatis, dan field statis {@link #PEMBAYARAN_TUNAI} selalu {@code null} sepanjang siklus
 * hidup aplikasi kecuali sesuatu memanggilnya manual.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see JenisPajakPpn
 * @see ais.database.model.akunting.Pajak
 * @see ais.action.master.akunting.PostingProsesTransferAction
 * @see ais.action.master.akunting.PostingPertangungjawabanPajakAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_pajak_barang")
public class JenisPajakBarang extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_pajak_barang}. */
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
	 * Cache statis in-memory yang <b>seharusnya</b> diisi oleh {@link #reloadDefault()}. Karena
	 * method itu tidak pernah dipanggil di mana pun dalam repo (lihat catatan kelas), field ini
	 * tetap {@code null} sepanjang siklus hidup normal aplikasi — jangan mengasumsikan field ini
	 * pernah terisi.
	 */
	public static JenisPajakBarang PEMBAYARAN_TUNAI = null;

	/**
	 * Memastikan tabel {@code asset.jenis_pajak_barang} punya minimal satu baris seed ("PPH23",
	 * kode "001", tarif 1%) dan mengisi {@link #PEMBAYARAN_TUNAI} dengannya.
	 *
	 * <p><b>Cara kerja:</b> identik polanya dengan
	 * {@code JenisPembayaranBarang.reloadDefault()} — membuka sesi Hibernate native, mengambil
	 * baris pertama urut {@code id} asc, membuat baris seed dalam transaksi eksplisit bila
	 * kosong, lalu menutup sesi.</p>
	 *
	 * <p><b>KODE MATI:</b> method ini <b>tidak dipanggil di mana pun</b> dalam repo saat ini —
	 * berbeda dari {@code reloadDefault()} pada {@code JenisPembayaranBarang},
	 * {@code JenisPekerjaanPenyedia}, {@link JenisPajakPpn}, {@code JenisPengapusanBarang}, dan
	 * {@code JenisPenerimaanBarang} yang semuanya terdaftar di
	 * {@code ais.common.InitData#reloadDefaults()}. Baris seed "PPH23" tidak pernah dibuat
	 * otomatis oleh method ini; bila baris tarif PPh dibutuhkan, harus dibuat manual lewat
	 * layar CRUD {@code ais.action.master.asset.JenisPajakBarangAction}.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		PEMBAYARAN_TUNAI = (JenisPajakBarang) session.createCriteria(JenisPajakBarang.class).setMaxResults(1)
				.addOrder(Order.asc("id")).uniqueResult();
		if (PEMBAYARAN_TUNAI == null) {
			PEMBAYARAN_TUNAI = new JenisPajakBarang();
			PEMBAYARAN_TUNAI.setKode("001");
			PEMBAYARAN_TUNAI.setAktif(true);
			PEMBAYARAN_TUNAI.setNama("PPH23");
			PEMBAYARAN_TUNAI.setKeterangan("PPH23");
			PEMBAYARAN_TUNAI.setPersen(1.0);
			session.getTransaction().begin();
			session.save(PEMBAYARAN_TUNAI);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/** Kode singkat tarif PPh. */
	private String kode;
	/** Akun buku besar tujuan setoran langsung PPh. */
	private Akun akun;
	/** Akun buku besar dana titipan; dipakai saat PPh ditahan (escrow), bukan disetor langsung. */
	private Akun akunDanaTitipan;
	/** Nama tarif PPh. */
	private String nama;
	/** Persentase tarif PPh, mis. {@code 2.0} untuk 2%. */
	private Double persen;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPajakBarang() {
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
	 * Mengembalikan kode singkat tarif PPh. {@code null} dinormalisasi menjadi string kosong
	 * agar aman dipakai langsung di tampilan tanpa null-check tambahan.
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat tarif PPh. Tidak dianotasi {@code @Column} — kolom dipetakan lewat
	 * konvensi nama Hibernate default.
	 *
	 * @param kode kode tarif PPh
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama tarif PPh, di-trim untuk menghindari whitespace tak sengaja dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama tarif PPh. Tidak melakukan trim di sisi setter — trimming terjadi hanya saat
	 * dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama tarif PPh
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
	 * Mengembalikan status aktif tarif PPh. {@code null} database ditafsirkan sebagai aktif,
	 * sehingga baris lama yang belum pernah disentuh field ini akan selalu tampil aktif.
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
	 * Mengembalikan akun buku besar tujuan setoran langsung PPh, meresolusi proxy lazy
	 * Hibernate lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar
	 * pemanggil tidak menerima proxy yang bisa meledak di luar sesi Hibernate. Dibaca oleh
	 * {@code ais.database.model.akunting.Pajak} untuk menentukan akun jurnal PPh.
	 *
	 * @return akun setoran yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Mengisi akun buku besar tujuan setoran langsung PPh.
	 *
	 * @param akun akun setoran, boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan persentase tarif PPh. {@code null} dinormalisasi menjadi {@code 0.0} agar
	 * aman dipakai langsung dalam perhitungan aritmetika tanpa null-check tambahan — dibaca
	 * oleh {@code Pajak} sebagai tarif PPh yang berlaku saat ini untuk menghitung nominal.
	 *
	 * @return persentase tarif, tidak pernah {@code null} ({@code 0.0} bila belum diisi)
	 */
	public Double getPersen() {
		return persen == null ? 0.0 : persen;
	}

	/**
	 * Mengisi persentase tarif PPh.
	 *
	 * @param persen persentase tarif, mis. {@code 2.0} untuk 2%
	 */
	public void setPersen(Double persen) {
		this.persen = persen;
	}

	/**
	 * Mengembalikan akun buku besar dana titipan, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan. Dibaca luas oleh mesin
	 * posting transfer dan pertanggungjawaban pajak ({@code PostingProsesTransferAction},
	 * {@code PostingPertangungjawabanPajakAction}, {@code ProsesTransferAction}) untuk
	 * menentukan akun kredit/debit saat PPh ditahan sebagai dana titipan alih-alih disetor
	 * langsung; nilai {@code null} pada field ini membuat proses tersebut mengambil jalur
	 * non-dana-titipan.
	 *
	 * @return akun dana titipan yang sudah teresolusi, atau {@code null} bila tarif ini tidak
	 *         memakai skema dana titipan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_dana_titipan", nullable = true)
	public Akun getAkunDanaTitipan() {
		akunDanaTitipan = check(akunDanaTitipan);
		return akunDanaTitipan;
	}

	/**
	 * Mengisi akun buku besar dana titipan.
	 *
	 * @param akunDanaTitipan akun dana titipan; {@code null} berarti tarif ini tidak memakai
	 *                        skema dana titipan
	 */
	public void setAkunDanaTitipan(Akun akunDanaTitipan) {
		this.akunDanaTitipan = akunDanaTitipan;
	}

}
