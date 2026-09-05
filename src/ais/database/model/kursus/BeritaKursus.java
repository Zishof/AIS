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
import ais.ui.util.WaktuUtil;

/**
 * Berita/pengumuman terkait modul kursus (LMS), ditampilkan di landing page publik
 * ({@code landing_page.jsp}) dan/atau dashboard kursus. Strukturnya mirip
 * {@code ais.database.model.library.InformasiPerpustakaan}/{@code ItemPunyaTerbit}: judul
 * ({@link #getNama()}), ringkasan ({@link #getKeterangan()}), isi lengkap ({@link #getIsi()}),
 * penerbit ({@link #getPublikasiOleh()}), waktu publikasi ({@link #getWaktuPublikasi()}), dan
 * flag tampil {@link #getAktif()}.
 *
 * <h3>{@link #getAktif()} di kelas ini NORMAL, bukan instance ke-3 pola terbalik</h3>
 * <p>
 * Pola bug yang sudah tercatat dua kali di {@code InformasiPerpustakaan}/{@code ItemPunyaTerbit}
 * adalah getter yang MENGHITUNG ULANG kondisi aktif dari field lain (mis. rentang tanggal) dengan
 * logika kondisional yang justru terbalik dari maksudnya. {@link #getAktif()} DI SINI TIDAK
 * melakukan itu -- ia murni default nullable sederhana ({@code aktif == null ? true : aktif}):
 * mengembalikan {@code true} hanya bila field belum pernah diisi, dan mengembalikan nilai
 * {@link #aktif} apa adanya bila sudah diisi. Ini konsisten dengan makna namanya (persis pola yang
 * sama dengan {@code UlasanKursus#getAktif()} pada klaster paket ini) -- DICATAT di sini karena
 * diminta diperiksa, tapi BUKAN instance baru dari pola terbalik tersebut.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "berita_kursus")
public class BeritaKursus extends GeneralValueObject {

	/**
	 * Seed data awal: jika tabel {@code berita_kursus} masih kosong ({@code count == 0}), menyisipkan
	 * 4 berita contoh (judul;ringkasan dipisah {@code ";"}) dalam satu transaksi per baris. Setiap
	 * baris dicek dulu dengan {@code Restrictions.eq("nama", ket)} sebelum {@code save()} agar
	 * pemanggilan ulang tidak membuat duplikat -- namun karena guard luar sudah membatasi ke
	 * {@code count == 0}, cek per-baris ini hanya relevan bila seeding gagal di tengah jalan pada
	 * percobaan sebelumnya. Menutup {@link HibernateUtil} session di akhir lewat
	 * {@code HibernateUtil.closeSession()}. Dipanggil saat inisialisasi modul (pola sama dengan
	 * {@code reloadDefault()} pada entity lain di codebase ini).
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();

		int count = ((Number) session.createCriteria(BeritaKursus.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {

			for (String keta : new String[] {
					"Bekraf Dukung Developer Lokal Kembangkan Game Kearifan Lokal;Ekonomi kreatif di Indonesia itu adalah mengangkat kearifan lokal dibungkus dengan kemasan kekinian.",
					"Raup Puluhan Juta Jualan Souvenir Militer;Semakin banyak pengusaha souvenir, maka akan semakin ramai pasarannya, banyak orang akan datang Untuk mencari.",
					"[Semarang] Diponegoro Accelerator;Diponegoro Accelerator merupakan sebuah gerakan yang diprakarsai oleh Inbis KKIB Undip dan didukung oleh Universitas Diponegoro.",
					"[Bali] XBlockchain Summit;The XBlockchain Summit will bring together those leading the status quo and its upheaval in the spirit of fostering positive developments." }) {

				String[] k = keta.split(";");
				String ket = k[0];
				String ket1 = k[1];

				BeritaKursus data = (BeritaKursus) session.createCriteria(BeritaKursus.class)
						.add(Restrictions.eq("nama", ket)).setMaxResults(1).uniqueResult();

				if (data == null) {
					data = new BeritaKursus();
					data.setNama(ket);
					data.setKeterangan(ket1);
					data.setIsi(ket1);
					session.getTransaction().begin();
					session.save(data);
					session.getTransaction().commit();
				}
			}
		}

		HibernateUtil.closeSession();
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;

	/** Field audit shadow: nama pengubah terakhir, ditulis oleh {@code AuditTimestampInterceptor}. */
	private String oleh;

	/** Field audit shadow: id pengubah terakhir, ditulis oleh {@code AuditTimestampInterceptor}. */
	private String olehId;

	/** @return id pengguna yang terakhir mengubah baris ini, diisi otomatis oleh interceptor. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengubah terakhir. Nilai kosong/hanya-spasi diabaikan (dead assignment) agar
	 * baris audit sebelumnya tidak tertimpa nilai kosong saat interceptor dipanggil ulang.
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengubah terakhir. Nilai kosong/hanya-spasi diabaikan (dead assignment),
	 * simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini, diisi otomatis oleh interceptor. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} (dan field audit terkait)
	 * lewat {@code AuditTimestampInterceptor} setiap kali baris ini di-UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir untuk ditimpa langsung (jarang dipanggil manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas "{@link #getId() id}-{@link #getNama() nama}". */
	public String toString() {
		return id + "-" + nama;
	}

	/** Judul berita; lihat {@link #getNama()} untuk perilaku trim otomatis. */
	private String nama;
	private String keterangan;

	/** Nama/label penerbit berita; lihat {@link #getPublikasiOleh()}. */
	private String publikasiOleh;
	private Date waktuPublikasi;

	/** Isi lengkap berita (HTML/teks bebas); lihat {@link #getIsi()}. */
	private String isi;

	/** Flag tampil/sembunyi; default {@code true} bila belum diisi -- lihat {@link #getAktif()}. */
	private Boolean aktif;

	public BeritaKursus() {
	}

	/** @return id baris, auto-generated (identity) oleh database. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; normalnya tidak diisi manual karena kolom bertanda {@code insertable = false}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return judul berita, di-trim (whitespace di awal/akhir dibuang) atau {@code null} bila belum diisi. Wajib diisi (kolom {@code nullable = false}). */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama judul berita mentah (belum di-trim; trim dilakukan oleh {@link #getNama()} saat dibaca). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return ringkasan/keterangan singkat berita, boleh kosong. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan ringkasan/keterangan singkat berita. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@code true} bila berita ditampilkan ke publik, {@code false} bila disembunyikan.
	 *         Default {@code true} bila field belum pernah diisi. Pola nullable-default sederhana
	 *         yang NORMAL/konsisten dengan namanya -- BUKAN instance ketiga dari bug getter terbalik
	 *         yang sebelumnya ditemukan di {@code InformasiPerpustakaan}/{@code ItemPunyaTerbit}
	 *         (lihat javadoc kelas untuk detail perbandingannya).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} untuk tampil publik, {@code false} untuk disembunyikan. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return isi lengkap berita (HTML/teks bebas), boleh kosong/null. */
	@Column(name = "isi", nullable = true, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/** @param isi isi lengkap berita. */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/** @return nama/label penerbit berita, boleh kosong/null. */
	public String getPublikasiOleh() {
		return publikasiOleh;
	}

	/** @param publikasiOleh nama/label penerbit berita. */
	public void setPublikasiOleh(String publikasiOleh) {
		this.publikasiOleh = publikasiOleh;
	}

	/** @return waktu publikasi; default "sekarang" ({@link WaktuUtil#getDate()}, bukan tersimpan) bila field belum pernah diisi. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuPublikasi() {
		return waktuPublikasi == null ? WaktuUtil.getDate() : waktuPublikasi;
	}

	/** @param waktuPublikasi waktu publikasi berita. */
	public void setWaktuPublikasi(Date waktuPublikasi) {
		this.waktuPublikasi = waktuPublikasi;
	}

}
