package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>Jenis Pekerjaan Penyedia</b> (tabel {@code asset.jenis_pekerjaan_penyedia}) —
 * katalog {@code kode/nama/keterangan/aktif} berisi bidang usaha/jenis pekerjaan penyedia
 * (vendor), mis. "Manufaktur/Produksi", "Jasa Teknologi Informasi (TI)". Entity {@code PenyediaAsset}
 * (master data vendor di paket ini) punya <b>lima</b> field bertipe kelas ini sekaligus —
 * {@code jenisPekerjaanPenyedia1} s.d. {@code jenisPekerjaanPenyedia5} — sehingga satu penyedia
 * bisa ditandai dengan sampai lima bidang usaha berbeda secara bersamaan.
 *
 * <h2>{@link #reloadDefault()}: seed 13 kategori baku, bukan satu baris tunggal</h2>
 * <p>Berbeda dari pola {@code reloadDefault()} di sebagian besar saudara paket ini yang hanya
 * membuat <b>satu</b> baris seed dan menaruhnya ke field statis, method ini men-seed <b>13
 * kategori bidang usaha</b> yang sudah ditulis-tangan (distribusi/penjualan, manufaktur, TI,
 * pemasaran, SDM, logistik, pemeliharaan, konsultasi non-TI, keamanan, desain kreatif,
 * konstruksi, katering, event) satu per satu — dan <b>tidak</b> menyimpan hasilnya ke field
 * statis mana pun (kelas ini tidak punya field cache seperti {@code JenisPajakPpn.PPN}). Setiap
 * kategori dicek keberadaannya lewat {@code nama} sebelum disisipkan, dan seluruh proses seed
 * hanya berjalan sama sekali bila tabel benar-benar kosong ({@code count == 0}) — bukan per
 * baris yang hilang, jadi menghapus satu baris seed secara manual tidak membuatnya dibuat ulang
 * otomatis pada startup berikutnya.</p>
 *
 * <h2>Penanganan error yang menelan exception</h2>
 * <p>Seluruh badan method dibungkus {@code try/catch(Exception)} yang mencetak stack trace dan
 * merekamnya lewat {@link ais.common.ErrorAuditUtil#record(Throwable, String)}, lalu
 * membiarkan eksekusi lanjut normal (exception tidak dilempar ulang). Artinya kegagalan seed
 * (mis. tabel terkunci, koneksi database putus di tengah loop 13 kategori) tidak menghentikan
 * urutan {@code InitData#reloadDefaults()} secara keseluruhan, tapi juga tidak memberi sinyal
 * ke pemanggil bahwa sebagian kategori mungkin gagal disisipkan.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see ais.database.model.asset.PenyediaAsset
 * @see ais.action.master.asset.JenisPekerjaanPenyediaAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_pekerjaan_penyedia")
public class JenisPekerjaanPenyedia extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_pekerjaan_penyedia}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String olehId;

	/**
	 * Memastikan tabel {@code asset.jenis_pekerjaan_penyedia} punya 13 baris kategori bidang
	 * usaha baku (nama + keterangan tertulis-tangan di source, lihat catatan kelas) bila tabel
	 * saat ini benar-benar kosong.
	 *
	 * <p><b>Cara kerja:</b> menghitung jumlah baris lewat {@code Projections.rowCount()}; hanya
	 * bila hasilnya nol, melakukan loop atas 13 pasangan {@code "nama;keterangan"} yang
	 * di-{@code split(";")}, mengecek keberadaan tiap nama lewat query terpisah, lalu menyimpan
	 * baris baru dalam transaksi eksplisit per-kategori ({@code begin()}/{@code commit()}
	 * manual) bila belum ada. Seluruh badan method dibungkus {@code try/catch/finally}:
	 * exception apa pun ditangkap, dicetak, dan direkam lewat
	 * {@link ais.common.ErrorAuditUtil#record(Throwable, String)} tanpa dilempar ulang, dan
	 * sesi selalu ditutup di blok {@code finally} lewat
	 * {@link ais.database.hibernate.HibernateUtil#closeSession()} — berbeda dari
	 * {@code reloadDefault()} lain di paket ini yang tidak membungkus badannya dengan
	 * try/catch.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> sekali secara asinkron saat startup aplikasi dari
	 * {@code ais.common.InitData#reloadDefaults()}. Tidak ada field statis yang diisi hasil
	 * seed ini — tidak seperti {@code JenisPajakPpn.PPN} atau
	 * {@code JenisPenerimaanBarang.DEFAULT_PENERIMAAN_BARANG_JASA} — jadi konsumen yang butuh
	 * baris kategori harus query tabel langsung, bukan membaca cache in-memory kelas ini.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		try {

		int count = ((Number) session.createCriteria(JenisPekerjaanPenyedia.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {

			for (String keta : new String[] {
					"Distribusi dan Penjualan;Menyediakan dan mendistribusikan barang atau produk jadi kepada perusahaan atau konsumen.",
					"Manufaktur/Produksi;Membuat atau memproduksi barang sesuai dengan spesifikasi atau pesanan.",
					"Jasa Teknologi Informasi (TI);Menyediakan layanan terkait sistem, perangkat lunak, jaringan, dan dukungan TI.",
					"Jasa Pemasaran dan Periklanan;Membantu perusahaan dalam strategi pemasaran, promosi, dan komunikasi.",
					"Jasa Sumber Daya Manusia (SDM);Menyediakan layanan terkait perekrutan, pelatihan, dan pengelolaan SDM.",
					"Jasa Logistik dan Pengiriman;Mengelola pergerakan barang dari satu titik ke titik lain, termasuk penyimpanan.",
					"Jasa Pemeliharaan dan Perbaikan;Melakukan perawatan dan perbaikan aset fisik perusahaan.",
					"Jasa Konsultasi (Non-TI);Memberikan saran dan bimbingan ahli dalam berbagai bidang spesifik.",
					"Jasa Keamanan;Menyediakan layanan pengamanan untuk properti, data, atau personel.",
					"Jasa Desain dan Kreatif;Menyediakan layanan yang berfokus pada estetika dan kreasi visual.",
					"Jasa Konstruksi dan Renovasi;Melakukan pembangunan atau perbaikan struktur dan infrastruktur.",
					"Jasa Katering dan Makanan/Minuman;Menyediakan layanan makanan dan minuman untuk acara atau operasional.",
					"Jasa Event dan Hiburan;Mengelola dan menyediakan kebutuhan untuk berbagai acara." }) {

				String[] k = keta.split(";");
				String ket = k[0];
				String ket1 = k[1];

				JenisPekerjaanPenyedia data = (JenisPekerjaanPenyedia) session
						.createCriteria(JenisPekerjaanPenyedia.class).add(Restrictions.eq("nama", ket)).setMaxResults(1)
						.uniqueResult();

				if (data == null) {
					data = new JenisPekerjaanPenyedia();
					data.setNama(ket);
					data.setKeterangan(ket1);
					session.getTransaction().begin();
					session.save(data);
					session.getTransaction().commit();
				}
			}
		}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/asset/JenisPekerjaanPenyedia.java:88");
		} finally {
			HibernateUtil.closeSession();
		}
	}

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

	/** Kode singkat jenis pekerjaan penyedia. */
	private String kode;
	/** Nama bidang usaha/jenis pekerjaan penyedia. */
	private String nama;
	/** Keterangan; dipetakan sebagai kolom {@code text} (bukan {@code varchar}), boleh panjang. */
	private String keterangan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPekerjaanPenyedia() {
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
	 * Mengembalikan kode singkat jenis pekerjaan. Berbeda dari kebanyakan getter {@code kode}
	 * di paket ini yang fallback ke string kosong, getter ini fallback ke
	 * {@code "0000" + id} saat kode belum diisi atau kosong — sehingga selalu menghasilkan
	 * identitas tampil yang unik per baris walau kolom {@code kode} sendiri kosong.
	 *
	 * @return kode ter-trim bila terisi; {@code "0000<id>"} bila kosong/belum diisi
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "0000" + getId() : kode.trim();
	}

	/**
	 * Mengisi kode singkat jenis pekerjaan. Tidak dianotasi {@code @Column} — kolom dipetakan
	 * lewat konvensi nama Hibernate default.
	 *
	 * @param kode kode jenis pekerjaan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama bidang usaha/jenis pekerjaan, di-trim untuk menghindari whitespace tak
	 * sengaja dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama bidang usaha/jenis pekerjaan. Tidak melakukan trim di sisi setter —
	 * trimming terjadi hanya saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama bidang usaha/jenis pekerjaan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bidang usaha. Kolom dipetakan sebagai {@code text} (bukan
	 * {@code varchar}) sehingga tidak dibatasi panjang seperti kebanyakan field
	 * {@code keterangan} lain di paket ini — cocok untuk deskripsi bidang usaha yang panjang
	 * seperti pada 13 baris seed di {@link #reloadDefault()}.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bidang usaha.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif jenis pekerjaan. {@code null} database ditafsirkan sebagai
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

}
