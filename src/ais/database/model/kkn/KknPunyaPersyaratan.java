package ais.database.model.kkn;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.Kkn;
import ais.database.model.GeneralValueObject;



/**
 * Entity relasi <b>many-to-one ganda</b> yang menghubungkan satu gelaran {@link Kkn} dengan satu
 * baris katalog {@link PersyaratanKkn} (mis. "Fotokopi KTM", "Minimal SKS 100") pada tabel
 * {@code public.kkn_punya_persyaratan}. Baris pada tabel ini adalah cara modul KKN menentukan
 * <b>syarat pendaftaran apa saja yang berlaku untuk gelaran KKN tertentu</b> — katalog
 * {@code PersyaratanKkn} sendiri bersifat global/dipakai bersama antar gelaran, sedangkan tabel
 * penghubung ini yang mengikatnya ke satu {@code Kkn}. Jawaban/pemenuhan tiap syarat oleh
 * mahasiswa disimpan terpisah di {@link MahasiswaKknPersyaratan}, bukan di sini.
 *
 * <p><b>Kembaran modul PKL, dengan satu divergensi nyata:</b> struktur kelas ini nyaris identik
 * dengan {@link ais.database.model.pkl.PklPunyaPersyaratan} (selain penggantian nama Kkn&rarr;Pkl),
 * <b>KECUALI</b> pada {@link #getNama()}: versi KKN ini jatuh balik ke
 * {@code persyaratanKkn.getNama()} saat field {@code nama} sendiri masih {@code null} (lihat
 * javadoc {@link #getNama()}), sedangkan versi PKL-nya <b>tidak</b> — ia mengembalikan {@code null}
 * polos tanpa fallback. Pola fallback-ke-master-data yang sama juga dipakai konsisten oleh
 * {@link KknPunyaKomponenPenilaianKkn#getNama()} dan {@code PklPunyaKomponenPenilaianPkl#getNama()}
 * (keduanya PUNYA fallback), sehingga versi KKN di sinilah yang mengikuti pola mayoritas repo dan
 * versi PKL yang tampak sebagai penyimpangan salin-tempel yang luput.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kkn_punya_persyaratan")



public class KknPunyaPersyaratan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris relasi ini (bukan primary key {@link Kkn} maupun {@link PersyaratanKkn}). */
	private Long id;
	/** Nama/username pengubah terakhir; diisi lewat {@link #setOleh(String)} oleh lapisan audit. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi lewat {@link #setOlehId(String)} oleh lapisan audit. */
	private String olehId;

	/**
	 * @return id pengguna (bukan nama tampilan) yang terakhir mengubah baris ini, atau {@code null}
	 *         bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/blank
	 * <b>diabaikan diam-diam</b> (early return) — nilai lama yang sudah tersimpan tetap
	 * dipertahankan, bukan ditimpa jadi kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama tampilan pengubah terakhir. Nilai {@code null} atau blank diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama tampilan pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
	 * {@code UPDATE} dikirim ke basis data, memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; biasanya diset otomatis oleh
	 *                        {@link #onUpdate()}, jarang dipanggil manual.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini. Diinisialisasi ke waktu saat ini pada
	 *         konstruksi objek (in-memory), lalu diperbarui otomatis oleh {@link #onUpdate()}
	 *         setiap kali baris diperbarui di basis data.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama()} — representasi teks ringkas baris relasi ini. */
	public String toString() {
		return nama;
	}

	/** Nama tampilan baris relasi; boleh {@code null} — lihat {@link #getNama()} untuk fallback-nya. */
	private String nama;
	/** Catatan/keterangan bebas untuk baris relasi ini; boleh {@code null}. */
	private String keterangan;
	/** Gelaran KKN yang memberlakukan syarat ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Kkn kkn;
	/** Syarat katalog yang diberlakukan oleh gelaran KKN di atas. Wajib diisi. */
	private PersyaratanKkn persyaratanKkn;

	/**
	 * @return gelaran {@link Kkn} pemilik baris relasi ini. <b>Tidak</b> memakai pembungkus
	 *         {@code check(...)} sebelum dikembalikan (berbeda dari pola
	 *         {@link KknPunyaKomponenPenilaianKkn#getKkn()} yang memakainya) — pemanggil yang
	 *         mengakses proxy ini di luar sesi Hibernate yang masih terbuka berisiko
	 *         {@code LazyInitializationException}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kkn", nullable = false)
	public Kkn getKkn() {
		return kkn;
	}

	/** @param kkn gelaran KKN yang memberlakukan syarat ini. */
	public void setKkn(Kkn kkn) {
		this.kkn = kkn;
	}

	/**
	 * @return syarat katalog ({@link PersyaratanKkn}) yang diikat oleh baris relasi ini ke gelaran
	 *         KKN pada {@link #getKkn()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "persyaratan_kkn", nullable = false)
	public PersyaratanKkn getPersyaratanKkn() {
		return persyaratanKkn;
	}

	/** @param persyaratanKkn syarat katalog yang diikat ke gelaran KKN ini. */
	public void setPersyaratanKkn(PersyaratanKkn persyaratanKkn) {
		this.persyaratanKkn = persyaratanKkn;
	}

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public KknPunyaPersyaratan() {
	}

	/**
	 * @return primary key baris relasi ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris relasi ini. Kolom dipetakan {@code insertable = false} sehingga
	 *           pengisian di sini tidak berpengaruh pada {@code INSERT}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama tampilan baris relasi ini. Bila field {@link #nama} belum pernah diisi eksplisit
	 *         (masih {@code null}), method ini <b>jatuh balik (fallback) ke nama syarat katalog
	 *         terkait</b> lewat {@code persyaratanKkn.getNama()} — sehingga tampilan daftar tetap
	 *         punya label bermakna walau baris relasi belum pernah diberi nama sendiri. Lihat
	 *         catatan divergensi pada javadoc kelas: kembaran PKL-nya tidak melakukan fallback ini.
	 * @throws NullPointerException bila field {@link #persyaratanKkn} masih {@code null} (seharusnya
	 *         tidak terjadi karena kolomnya {@code NOT NULL}, tapi entity yang belum pernah dikaitkan
	 *         syaratnya akan melempar exception ini, bukan mengembalikan {@code null} dengan aman).
	 *         Perhatikan juga bahwa akses lapangan di sini memakai field {@code persyaratanKkn}
	 *         mentah, bukan {@link #getPersyaratanKkn()} — sehingga tidak melewati proxy-check yang
	 *         dipakai getter publiknya.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? persyaratanKkn.getNama() : this.nama.trim();
	}

	/** @param nama nama tampilan eksplisit baris relasi; boleh dibiarkan {@code null} untuk memakai fallback. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan/keterangan bebas baris relasi ini, apa adanya tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk baris relasi ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
