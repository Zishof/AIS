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
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Entity master <b>Jenis Pajak PPN</b> (tabel {@code asset.jenis_pajak_ppn}) — katalog tarif
 * <b>PPN (Pajak Pertambahan Nilai)</b>, satu dari dua master tarif pajak yang benar-benar
 * dipakai mesin akunting pengadaan aset (lihat {@code ais.database.model.akunting.Pajak}).
 * Pasangannya adalah {@link JenisPajakBarang} untuk <b>PPh</b> (pajak penghasilan/withholding
 * tax) — keduanya sengaja terpisah karena {@code Pajak} punya dua properti berbeda,
 * {@code getJenisPajakBarang()} untuk PPh <b>atau</b> {@code getJenisPajakPpn()} untuk PPN,
 * tidak pernah keduanya sekaligus pada baris yang sama.
 *
 * <h2>Beda dari {@code AkunPajak}: ini master tarif yang benar-benar hidup</h2>
 * <p>Paket {@code ais.database.model.akunting} punya entity lain bernama mirip,
 * {@code AkunPajak}, yang <b>bukan</b> master tarif — javadoc kelas itu sendiri menegaskan
 * bahwa master tarif sesungguhnya untuk dokumen {@code Pajak} adalah kelas ini dan
 * {@link JenisPajakBarang}. Field {@link #getPersen()} di kelas ini benar-benar dibaca oleh
 * {@code Pajak} untuk menghitung nominal PPN pada setoran POS.</p>
 *
 * <h2>Cache statis {@link #PPN}: dipakai luas sebagai default lazy</h2>
 * <p>Berbeda dari cache serupa di beberapa saudara paket ini (mis.
 * {@code JenisPembayaranBarang.PEMBAYARAN_TUNAI} yang tidak dibaca di luar kelasnya sendiri),
 * field statis {@link #PPN} di sini <b>aktif dipakai</b> sebagai nilai fallback saat entity
 * lain (mis. {@code SaldoAwalMasterAssetDetail}, {@code PerjanjianKerjasamaMasterAsset},
 * {@code PenerimaanPengadaanMasterAssetDetail}, {@code PemesananPengadaanMasterAssetDetail})
 * belum punya jenis pajak PPN eksplisit pada getter lazy-nya, dan oleh aksi akunting
 * ({@code PertangungjawabanAction}, {@code PertangungjawabanKasBesarAction}) saat mendeteksi
 * tarif 11% dari input pengguna. Karena field statis ini hanya diisi ulang sekali oleh
 * {@link #reloadDefault()} saat startup, nilainya bisa jadi tidak sinkron dengan perubahan
 * tabel {@code jenis_pajak_ppn} berikutnya sampai aplikasi di-restart.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see JenisPajakBarang
 * @see ais.database.model.akunting.Pajak
 * @see ais.database.model.akunting.AkunPajak
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_pajak_ppn")
public class JenisPajakPpn extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_pajak_ppn}. */
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
	 * Cache statis in-memory berisi baris tarif PPN aktif pertama (urut {@code id} asc), dipakai
	 * luas sebagai nilai fallback lazy-default oleh entity dan aksi akunting lain — lihat
	 * catatan kelas untuk daftar pemakai dan implikasi kedaluwarsanya.
	 */
	public static JenisPajakPpn PPN = null;

	/**
	 * Memastikan tabel {@code asset.jenis_pajak_ppn} punya minimal satu baris tarif PPN aktif
	 * dan mengisi {@link #PPN} dengannya.
	 *
	 * <p><b>Cara kerja:</b> membuka sesi Hibernate native lewat
	 * {@link ais.database.hibernate.HibernateUtil#currentNativeSession()}, mengambil satu baris
	 * pertama yang {@code aktif} bernilai {@code true} atau {@code NULL} (memakai
	 * {@link org.hibernate.criterion.Restrictions#or}), urut {@code id} asc. Bila tidak
	 * ditemukan, membuat baris seed "PPN" kode "001" tarif 11% dalam transaksi eksplisit
	 * ({@code begin()}/{@code commit()} manual). Sesi kemudian di-{@code disconnect()} dan
	 * {@code close()} secara eksplisit sebelum {@link ais.database.hibernate.HibernateUtil#closeSession()}
	 * dipanggil — pola penutupan ganda yang tidak dipakai method {@code reloadDefault()} lain
	 * di paket ini, kemungkinan warisan debugging koneksi yang tidak dibersihkan.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> sekali secara asinkron saat startup aplikasi dari
	 * {@code ais.common.InitData#reloadDefaults()}. Nilai statis {@link #PPN} yang dihasilkan
	 * lalu dipakai lintas modul sebagai default — lihat catatan kelas.</p>
	 *
	 * <p><b>Tidak ada penanganan exception</b> — kegagalan Hibernate menyebar sebagai unchecked
	 * exception ke pemanggil di executor {@code InitData}.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		PPN = (JenisPajakPpn) session.createCriteria(JenisPajakPpn.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setMaxResults(1).addOrder(Order.asc("id"))
				.uniqueResult();
		if (PPN == null) {
			PPN = new JenisPajakPpn();
			PPN.setKode("001");
			PPN.setAktif(true);
			PPN.setNama("PPN");
			PPN.setKeterangan("PPN");
			PPN.setPersen(11.0);
			session.getTransaction().begin();
			session.save(PPN);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	/** Kode singkat tarif PPN. */
	private String kode;
	/** Akun buku besar yang dipasangkan dengan tarif PPN ini. */
	private Akun akun;
	/** Nama tarif PPN. */
	private String nama;
	/** Persentase tarif PPN, mis. {@code 11.0} untuk 11%. */
	private Double persen;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPajakPpn() {
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
	 * Mengembalikan kode singkat tarif PPN. {@code null} dinormalisasi menjadi string kosong
	 * agar aman dipakai langsung di tampilan tanpa null-check tambahan.
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat tarif PPN. Tidak dianotasi {@code @Column} — kolom dipetakan lewat
	 * konvensi nama Hibernate default.
	 *
	 * @param kode kode tarif PPN
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama tarif PPN, di-trim untuk menghindari whitespace tak sengaja dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama tarif PPN. Tidak melakukan trim di sisi setter — trimming terjadi hanya saat
	 * dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama tarif PPN
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
	 * Mengembalikan status aktif tarif PPN. {@code null} database ditafsirkan sebagai aktif,
	 * sehingga baris lama yang belum pernah disentuh field ini akan selalu tampil aktif. Juga
	 * dipakai langsung sebagai kriteria filter di {@link #reloadDefault()}.
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
	 * Mengembalikan akun buku besar yang dipasangkan dengan tarif PPN ini, meresolusi proxy
	 * lazy Hibernate lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar
	 * pemanggil tidak menerima proxy yang bisa meledak di luar sesi Hibernate.
	 *
	 * @return akun yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Mengisi akun buku besar yang dipasangkan dengan tarif PPN ini.
	 *
	 * @param akun akun terkait, boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan persentase tarif PPN. {@code null} dinormalisasi menjadi {@code 0.0} agar
	 * aman dipakai langsung dalam perhitungan aritmetika tanpa null-check tambahan — nilai ini
	 * dibaca oleh {@code ais.database.model.akunting.Pajak} untuk menghitung nominal PPN.
	 *
	 * @return persentase tarif, tidak pernah {@code null} ({@code 0.0} bila belum diisi)
	 */
	public Double getPersen() {
		return persen == null ? 0.0 : persen;
	}

	/**
	 * Mengisi persentase tarif PPN.
	 *
	 * @param persen persentase tarif, mis. {@code 11.0} untuk 11%
	 */
	public void setPersen(Double persen) {
		this.persen = persen;
	}

}
