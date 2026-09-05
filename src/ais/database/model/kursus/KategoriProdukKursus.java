package ais.database.model.kursus;

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
 * Master <b>kategori produk kursus</b> (mis. "IT &amp; Software", "Bisnis", "Bahasa") yang dipakai
 * {@link ProdukKursus#getKategoriProdukKursus()} untuk mengelompokkan produk kursus pada
 * katalog/landing page. Kelas ini murni daftar referensi tanpa relasi keluar; setiap baris berdiri
 * sendiri dengan nama, keterangan promosi, dan ikon tampilan.
 *
 * <h3>{@link #reloadDefault()} — penyemaian delapan kategori baku</h3>
 * <p>Method statis ini memuat delapan kategori baku (IT &amp; Software, Bisnis, Marketing,
 * Kesehatan &amp; Fitnes, Bahasa, Fotografi, Gaya Hidup, Produktifitas) lengkap dengan teks promosi
 * masing-masing, tetapi <b>hanya bila tabel ini masih kosong</b> ({@code count == 0}) — bukan
 * upsert per baris. Konsekuensinya: penyemaian ini hanya efektif sekali, pada instalasi baru yang
 * belum pernah memiliki baris kategori apa pun; setelah satu kategori (apa pun) pernah tersimpan,
 * pemanggilan {@link #reloadDefault()} berikutnya tidak berbuat apa-apa walau tujuh dari delapan
 * kategori baku itu kebetulan belum ada. Method ini membuka {@code Session} native Hibernate
 * sendiri lewat {@code HibernateUtil.currentNativeSession()} dan menutupnya secara eksplisit di
 * akhir (disconnect + close), <b>di luar</b> pola {@code check()}/getter standar kelas dasar.</p>
 *
 * <h3>Getter destruktif dengan cache satu-kali di {@link #getIcon()}</h3>
 * <p>{@link #getIcon()} memetakan nama kategori ke nama kelas CSS ikon (font {@code pe-7s-*}) lewat
 * perbandingan {@code equalsIgnoreCase} terhadap kedelapan nama baku, HANYA bila field {@link #icon}
 * masih {@code null} — begitu terisi (baik dari basis data maupun dari pemetaan otomatis ini),
 * hasilnya ditulis balik ke field dan tidak dihitung ulang pada pemanggilan berikutnya untuk
 * instance yang sama. Kategori dengan nama di luar delapan nilai baku (mis. kategori kustom yang
 * dibuat operator sendiri lewat form, bukan lewat {@link #reloadDefault()}) tidak pernah mendapat
 * ikon otomatis dan mengembalikan {@code null} kecuali diisi manual lewat {@link #setIcon(String)}.</p>
 *
 * @see ProdukKursus produk kursus yang dikelompokkan oleh kategori ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kategori_produk_kursus")
public class KategoriProdukKursus extends GeneralValueObject {

	/**
	 * Menyemai delapan kategori produk kursus baku (IT &amp; Software, Bisnis, Marketing, Kesehatan
	 * &amp; Fitnes, Bahasa, Fotografi, Gaya Hidup, Produktifitas) lengkap dengan teks promosi
	 * masing-masing sebagai {@link #getKeterangan()}, <b>hanya bila tabel {@code kategori_produk_kursus}
	 * masih benar-benar kosong</b>. Method membuka session native Hibernate sendiri, memeriksa
	 * jumlah baris lewat {@code Projections.rowCount()}, dan — bila kosong — melakukan transaksi
	 * simpan per kategori (masing-masing dicek dulu dengan {@code Restrictions.eq("nama", ...)}
	 * sebelum disisipkan, walau di titik ini tabel dijamin kosong sehingga pengecekan itu tidak
	 * pernah menemukan apa pun). Sesudah penyemaian (atau bila tabel sudah tidak kosong sejak awal),
	 * session ditutup secara eksplisit (disconnect lalu close) dan
	 * {@code HibernateUtil.closeSession()} dipanggil.
	 *
	 * <p><b>Bukan idempoten penuh:</b> pemanggilan ini hanya berpengaruh SEKALI, pada saat tabel
	 * benar-benar kosong. Bila kemudian salah satu dari delapan kategori dihapus (atau kategori
	 * kustom lain sempat dibuat lebih dulu), pemanggilan ulang {@link #reloadDefault()} tidak akan
	 * mengembalikan kategori yang hilang tersebut karena syarat {@code count == 0} sudah tidak
	 * terpenuhi. Cocok dipanggil sekali saat inisialisasi aplikasi (mis. dari
	 * {@code AppStartupListener}), bukan sebagai mekanisme perbaikan data yang hilang.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();

		int count = ((Number) session.createCriteria(KategoriProdukKursus.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {

			for (String keta : new String[] {
					"IT & Software;Perkembangan dunia Teknologi yang semakin maju kami menghadirkan berbagai kursus untuk memenuhi kebutuhan anda dalam mengembangkan diri.",
					"Bisnis;Berbisnis akan berjalan apabila pengelolaan dilakukan dengan baik, kami hadirkan pembelajaran berbisnis yang kompeten untuk menunjang kebutuhan.",
					"Marketing;Perkembangan dunia usaha semakin maju, dengan itu kami hadirkan berbagai materi pembelajaran marketing untuk UMKM yang mudah dan tepat.",
					"Kesehatan & Fitnes;Kesehatan terkadang tidak jadi perhatian anda bukan? sibuk kerja menjadikan tidak banyak ilmu tentang kesehatan padahal itu sangat penting.",
					"Bahasa;Kursus secara langsung terkendala biaya dan waktu? kami solusikan anda untuk belajar berbagai bahasa bisa dimana saja dengan mudah dan hemat.",
					"Fotografi;Fotografi hobi yang mungkin tidak anda sadari! kami akan memberikan kurus dengan berbagai pembelanjaran yang mudah untuk pemula hingga mahir.",
					"Gaya Hidup;Bagaimana gaya hidup yang biasa, menjadi luar biasa, kami memberikan kurus untuk anda dari kebiasaan sehari hari menjadi hal yang menguntungkan.",
					"Produktifitas;Pekerja kantoran terdang dituntuk untuk bisa bebagai software, kami hadirkan solusi kurus online dari mulai MS. Office hingga ERP yang umum digunakan." }) {

				String[] k = keta.split(";");
				String ket = k[0];
				String ket1 = k[1];

				KategoriProdukKursus data = (KategoriProdukKursus) session.createCriteria(KategoriProdukKursus.class)
						.add(Restrictions.eq("nama", ket)).setMaxResults(1).uniqueResult();

				if (data == null) {
					data = new KategoriProdukKursus();
					data.setNama(ket);
					data.setKeterangan(ket1);
					session.getTransaction().begin();
					session.save(data);
					session.getTransaction().commit();
				}
			}
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code kategori_produk_kursus}, dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah kategori ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan tanpa identitas pengguna.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah kategori ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object memakai
	 * {@code WaktuUtil.getDate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas kategori: {@code "id-nama"}.
	 *
	 * @return gabungan id dan nama kategori
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas kategori; lihat {@link #getKode()} untuk nilai cadangan berbasis id. */
	private String kode;

	/** Nama kategori (kolom wajib, maksimal 255 karakter; mis. "IT & Software", "Bisnis"). */
	private String nama;
	/** Keterangan/teks promosi kategori (kolom bertipe {@code text}). */
	private String keterangan;
	/** Nama kelas CSS ikon (font {@code pe-7s-*}) kategori; lihat {@link #getIcon()} untuk pemetaan otomatisnya. */
	private String icon;
	/** Status aktif/nonaktif kategori; {@code null} dianggap aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public KategoriProdukKursus() {
	}

	/**
	 * Mengembalikan primary key kategori.
	 *
	 * @return primary key, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya diisi otomatis oleh Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas kategori. Bila field {@link #kode} kosong, nilai cadangannya
	 * BUKAN string kosong seperti pola umum kelas sejenis di klaster ini, melainkan
	 * {@code "00" + getId()} — kode turunan dari primary key (mis. id {@code 7} menghasilkan kode
	 * {@code "007"}) — atau string kosong bila {@link #getId()} juga {@code null} (object belum
	 * tersimpan).
	 *
	 * @return kode kategori (tersimpan atau turunan dari id), tidak pernah {@code null}
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? (getId() == null ? "" : "00" + getId()) : kode.trim();
	}

	/**
	 * Mengisi kode ringkas kategori.
	 *
	 * @param kode kode kategori baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama kategori, dipangkas spasi tepi.
	 *
	 * @return nama kategori (dipangkas), atau {@code null} bila belum pernah diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama kategori.
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/teks promosi kategori. Getter murni-baca, tanpa normalisasi.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/teks promosi kategori.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/nonaktif kategori, menormalkan {@code null} menjadi {@code true}.
	 *
	 * @return {@code true} bila kategori aktif, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyalakan atau mematikan kategori.
	 *
	 * @param aktif {@code true} bila kategori aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama kelas CSS ikon (font {@code pe-7s-*}) kategori ini. <b>Getter destruktif
	 * dengan cache satu-kali</b>: bila field {@link #icon} masih {@code null} DAN {@link #getNama()}
	 * terisi, method mencocokkan nama kategori (case-insensitive) terhadap delapan nama baku yang
	 * disemai {@link #reloadDefault()}, menulis hasil pemetaan ke field {@link #icon}, lalu
	 * mengembalikannya. Pemanggilan berikutnya pada instance yang sama langsung mengembalikan nilai
	 * ter-cache tanpa mengulang pencocokan. Kategori kustom (nama di luar delapan nilai baku) tidak
	 * pernah cocok dengan cabang mana pun sehingga {@link #icon} tetap {@code null} — method
	 * mengembalikan {@code null} untuk kategori semacam itu kecuali diisi manual lewat
	 * {@link #setIcon(String)}.
	 *
	 * @return nama kelas CSS ikon, atau {@code null} bila nama kategori kosong atau tidak cocok
	 *         dengan salah satu dari delapan nama baku
	 */
	public String getIcon() {

		if (getNama() != null && icon == null) {
			if (getNama().equalsIgnoreCase("IT & Software")) {
				icon = "pe-7s-pendrive";
			} else if (getNama().equalsIgnoreCase("Bisnis")) {
				icon = "pe-7s-display1";
			} else if (getNama().equalsIgnoreCase("Marketing")) {
				icon = "pe-7s-speaker";
			} else if (getNama().equalsIgnoreCase("Kesehatan & Fitnes")) {
				icon = "pe-7s-gym";
			} else if (getNama().equalsIgnoreCase("Bahasa")) {
				icon = "pe-7s-notebook";
			} else if (getNama().equalsIgnoreCase("Fotografi")) {
				icon = "pe-7s-camera";
			} else if (getNama().equalsIgnoreCase("Gaya Hidup")) {
				icon = "pe-7s-bicycle";
			} else if (getNama().equalsIgnoreCase("Produktifitas")) {
				icon = "pe-7s-mouse";
			}
		}

		return icon;
	}

	/**
	 * Mengisi nama kelas CSS ikon kategori secara manual. Mengisi field ini secara langsung juga
	 * menonaktifkan pemetaan otomatis {@link #getIcon()} untuk instance yang sama (karena syarat
	 * {@code icon == null} tidak lagi terpenuhi).
	 *
	 * @param icon nama kelas CSS ikon baru
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

}
