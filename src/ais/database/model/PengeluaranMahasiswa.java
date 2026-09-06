package ais.database.model;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.akunting.PostingHistory;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.pengeluaran_mahasiswa} — catatan
 * <b>pengeluaran/refund</b> yang dibayarkan KEPADA mahasiswa/siswa (atau calon mahasiswa/calon
 * siswa), kebalikan arah dari tagihan biasa ({@link Kegiatan} dkk. yang DITAGIHKAN KE
 * mahasiswa). Satu baris mencatat nominal ({@link #getNominal()}), jenis pembayaran ({@link
 * #getJenisPembayaran()}), jenis pengeluaran ({@link #getJenisPengeluaranMahasiswa()}), dan
 * waktu transaksi, dengan tautan opsional ke {@link PostingHistory} akunting.
 *
 * <p>Entity ini dipakai untuk DUA populasi berbeda dengan pola yang sama: mahasiswa/calon
 * mahasiswa (perguruan tinggi) DAN siswa/calon siswa (modul sekolah, {@link
 * ais.database.model.sekolah.Siswa}/{@link ais.database.model.sekolah.CalonSiswa}) — satu baris
 * secara skema bisa menautkan salah satu atau kombinasi keempatnya, dengan {@link #getMahasiswa()}
 * dan {@link #getSiswa()} masing-masing menurunkan mahasiswa/siswa aktif dari
 * calon-nya bila belum ditautkan langsung (lihat javadoc masing-masing getter).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pengeluaran_mahasiswa")
public class PengeluaranMahasiswa extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code pengeluaran_mahasiswa}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu "terakhir diubah"; diinisialisasi ke waktu sekarang saat instance dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code "<id>-<nama>"}.
	 *
	 * @return string ringkas identitas baris ini
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Mahasiswa aktif penerima pengeluaran ini (FK {@code mahasiswa}); lihat {@link #getMahasiswa()} untuk penurunan dari calon mahasiswa. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa (PMB) penerima pengeluaran ini (FK {@code calon_mahasiswa}). */
	private BiodataCalonMahasiswa calonMahasiswa;
	/** Jenis pembayaran (mis. tunai/transfer); default {@link JenisPembayaran#DEFAULT_JENIS_PEMBAYARAN} bila belum diisi. */
	private JenisPembayaran jenisPembayaran;
	/** Jenis pengeluaran (kategori); default {@link JenisPengeluaranMahasiswa#DEFAULT_JENIS_PENGELUARAN} bila belum diisi. */
	private JenisPengeluaranMahasiswa jenisPengeluaranMahasiswa;
	/** Nominal pengeluaran; default {@code 0.0} bila kosong. */
	private Double nominal;
	/** Waktu transaksi pengeluaran; lazy default ke waktu sekarang bila belum diisi, lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Nama/deskripsi baris pengeluaran; diturunkan otomatis dari mahasiswa/calon mahasiswa bila kosong, lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas baris pengeluaran ini. */
	private String keterangan;
	/** Tautan ke riwayat posting akunting terkait pengeluaran ini. */
	private PostingHistory postingHistory;
	/** Calon siswa (modul sekolah) penerima pengeluaran ini (FK {@code calon_siswa}). */
	private CalonSiswa calonSiswa;
	/** Siswa aktif (modul sekolah) penerima pengeluaran ini (FK {@code siswa}); lihat {@link #getSiswa()} untuk penurunan dari calon siswa. */
	private Siswa siswa;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public PengeluaranMahasiswa() {
	}

	/**
	 * @return primary key baris {@code pengeluaran_mahasiswa}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama/deskripsi baris pengeluaran ini.
	 *
	 * <p><b>Penurunan otomatis bila kosong:</b> bila field mentah {@link #nama} {@code null},
	 * getter ini membangun deskripsi dari mahasiswa terkait ({@code
	 * mahasiswa.toString() + "-" + getNominal()}), atau dari calon mahasiswa bila tidak ada
	 * mahasiswa ({@code calonMahasiswa.toString() + "-" + getNominal()}), atau string kosong
	 * bila keduanya tidak ada. Tidak seperti beberapa getter derivatif lain di cluster ini,
	 * nilai yang diturunkan TIDAK ditulis balik ke field {@link #nama} (murni dihitung sesaat).
	 * </p>
	 * <p><b>Efek samping:</b> memanggil {@code check()} pada {@link #mahasiswa}/{@link
	 * #calonMahasiswa} (resolusi proxy lazy, ditulis balik ke field).</p>
	 *
	 * @return nama/deskripsi efektif, di-{@code trim()} bila field mentah terisi; hasil turunan
	 *         (lihat di atas) bila field mentah {@code null}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		mahasiswa = check(mahasiswa);
		calonMahasiswa = check(calonMahasiswa);
		return this.nama == null
				? (mahasiswa == null ? ((calonMahasiswa == null ? "" : calonMahasiswa.toString() + "-" + getNominal()))
						: mahasiswa.toString() + "-" + getNominal())
				: this.nama.trim();
	}

	/**
	 * @param nama nama/deskripsi baru; {@code null} untuk memakai penurunan otomatis lewat
	 *             {@link #getNama()}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas baris ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk baris ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mahasiswa aktif penerima pengeluaran ini.
	 *
	 * <p><b>Penurunan dari calon mahasiswa:</b> bila {@link #getCalonMahasiswa()} tidak {@code
	 * null} dan calon mahasiswa itu sudah punya {@code mahasiswa} terkait (sudah diterima/jadi
	 * mahasiswa aktif), field {@link #mahasiswa} DITIMPA dengan mahasiswa hasil penurunan itu —
	 * menimpa apa pun yang ditautkan langsung sebelumnya di kolom {@code mahasiswa}.</p>
	 *
	 * @return mahasiswa aktif efektif (langsung, atau diturunkan dari calon mahasiswa); boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);

		if (getCalonMahasiswa() != null && calonMahasiswa.getMahasiswa() != null) {
			mahasiswa = calonMahasiswa.getMahasiswa();
		}

		return mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa penerima baru; bisa tetap ditimpa oleh hasil penurunan dari
	 *                  calon mahasiswa saat dibaca via {@link #getMahasiswa()}.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return nominal pengeluaran; {@code 0.0} bila belum diisi.
	 */
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	/**
	 * @param nominal nominal baru untuk baris pengeluaran ini.
	 */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * @return waktu transaksi pengeluaran; waktu sekarang (dihitung sesaat, TIDAK ditulis balik
	 *         ke field) bila belum diisi — berbeda dari beberapa getter lazy-default lain di
	 *         cluster ini yang menyimpan permanen nilai fallback-nya.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * @param waktu waktu transaksi baru.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Jenis pembayaran baris pengeluaran ini.
	 *
	 * <p><b>Default konstanta, bukan {@code null}-check biasa:</b> bila field mentah {@code
	 * null}, dikembalikan konstanta {@link JenisPembayaran#DEFAULT_JENIS_PEMBAYARAN} (BUKAN
	 * ditulis balik ke field {@link #jenisPembayaran}); bila field mentah terisi, diresolusi
	 * lewat {@code check()} seperti biasa.</p>
	 *
	 * @return jenis pembayaran efektif; tidak pernah {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pembayaran", nullable = true)
	public JenisPembayaran getJenisPembayaran() {
		if (jenisPembayaran == null) {
			jenisPembayaran = JenisPembayaran.DEFAULT_JENIS_PEMBAYARAN;
		} else {
			jenisPembayaran = check(jenisPembayaran);
		}

		return jenisPembayaran;
	}

	/**
	 * @param jenisPembayaran jenis pembayaran baru; {@code null} untuk memakai default
	 *                        {@link JenisPembayaran#DEFAULT_JENIS_PEMBAYARAN}.
	 */
	public void setJenisPembayaran(JenisPembayaran jenisPembayaran) {
		this.jenisPembayaran = jenisPembayaran;
	}

	/**
	 * Jenis pengeluaran (kategori) baris ini.
	 *
	 * <p>Pola sama seperti {@link #getJenisPembayaran()}: bila field mentah {@code null},
	 * dikembalikan konstanta {@link JenisPengeluaranMahasiswa#DEFAULT_JENIS_PENGELUARAN}.</p>
	 *
	 * @return jenis pengeluaran efektif; tidak pernah {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengeluaran_mahasiswa", nullable = true)
	public JenisPengeluaranMahasiswa getJenisPengeluaranMahasiswa() {
		if (jenisPengeluaranMahasiswa == null) {
			jenisPengeluaranMahasiswa = JenisPengeluaranMahasiswa.DEFAULT_JENIS_PENGELUARAN;
		} else {
			jenisPengeluaranMahasiswa = check(jenisPengeluaranMahasiswa);
		}
		return jenisPengeluaranMahasiswa;
	}

	/**
	 * @param jenisPengeluaranMahasiswa jenis pengeluaran baru; {@code null} untuk memakai
	 *                                  default {@link JenisPengeluaranMahasiswa#DEFAULT_JENIS_PENGELUARAN}.
	 */
	public void setJenisPengeluaranMahasiswa(JenisPengeluaranMahasiswa jenisPengeluaranMahasiswa) {
		this.jenisPengeluaranMahasiswa = jenisPengeluaranMahasiswa;
	}

	/**
	 * @return riwayat posting akunting terkait baris pengeluaran ini; boleh {@code null}. Tidak
	 *         memakai {@code check()} untuk resolusi proxy lazy (berbeda dari relasi lain di
	 *         kelas ini yang konsisten memakainya).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * @param postingHistory riwayat posting akunting baru; {@code null} untuk melepas tautan.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * @return calon mahasiswa (PMB) penerima pengeluaran ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getCalonMahasiswa() {
		calonMahasiswa = check(calonMahasiswa);
		return calonMahasiswa;
	}

	/**
	 * @param calonMahasiswa calon mahasiswa penerima baru; {@code null} untuk melepas tautan.
	 */
	public void setCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa) {
		this.calonMahasiswa = calonMahasiswa;
	}

	/**
	 * Siswa aktif (modul sekolah) penerima pengeluaran ini.
	 *
	 * <p><b>Penurunan dari calon siswa (pola sama seperti {@link #getMahasiswa()}):</b> bila
	 * {@link #getCalonSiswa()} tidak {@code null} dan calon siswa itu sudah punya {@code siswa}
	 * terkait, field {@link #siswa} DITIMPA dengan siswa hasil penurunan itu; hanya bila tidak
	 * ada penurunan yang berlaku, field mentah diresolusi lewat {@code check()} seperti biasa.
	 * </p>
	 *
	 * @return siswa aktif efektif (langsung, atau diturunkan dari calon siswa); boleh {@code
	 *         null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		calonSiswa = getCalonSiswa();
		if (calonSiswa != null && calonSiswa.getSiswa() != null) {
			siswa = calonSiswa.getSiswa();
		} else {
			siswa = check(siswa);
		}
		return siswa;
	}

	/**
	 * @param siswa siswa penerima baru; bisa tetap ditimpa oleh hasil penurunan dari calon siswa
	 *              saat dibaca via {@link #getSiswa()}.
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * @return calon siswa (modul sekolah) penerima pengeluaran ini (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * @param calonSiswa calon siswa penerima baru; {@code null} untuk melepas tautan.
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}
}
