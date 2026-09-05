package ais.database.model.asset;

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

import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;

/**
 * Daftar RUJUKAN INDEPENDEN calon pemegang/penanggung jawab aset -- BUKAN snapshot yang
 * diturunkan otomatis dari transaksi {@link PemakaianMasterAsset} atau
 * {@link PeminjamanMasterAsset}.
 *
 * <h3>Master data, bukan turunan transaksi</h3>
 *
 * <p>Baris di sini adalah data yang berdiri sendiri: nama, keterangan, serta relasi opsional ke
 * struktur akademik ({@link Jurusan}, {@link Fakultas}, {@link PerguruanTinggi}). Kelas ini TIDAK
 * memiliki referensi balik ke {@code PemakaianMasterAsset} atau {@code PeminjamanMasterAsset} --
 * arah relasinya SEBALIKNYA: {@link PemakaianMasterAsset#getPemilikAsset()} menunjuk KE sini
 * lewat kolom {@code pemilik_asset}. Artinya baris {@code PemilikAsset} harus sudah ada
 * (didaftarkan terlebih dulu sebagai master data) sebelum dapat dipilih sebagai pemegang pada
 * dokumen pemakaian; kelas ini sendiri tidak pernah dibuat atau diperbarui otomatis akibat
 * transaksi pemakaian/peminjaman manapun.</p>
 *
 * <p>Konsekuensinya: "siapa pemegang aset SAAT INI" tidak dapat dibaca langsung dari kelas ini,
 * melainkan harus ditelusuri dari dokumen {@link PemakaianMasterAsset} TERBARU yang menunjuk ke
 * {@code PemilikAsset} tertentu -- kelas ini hanya menyediakan daftar KANDIDAT pemegang, bukan
 * riwayat/status kepemilikan.</p>
 *
 * @see PemakaianMasterAsset#getPemilikAsset() satu-satunya arah relasi yang menunjuk ke kelas ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pemilik_asset")

public class PemilikAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, identik di seluruh berkas entitas hbm2java sepaket (lihat
	 * catatan yang sama di {@link PemakaianMasterAsset#serialVersionUID}).
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (strategi IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini (jejak audit ringan). */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return id pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank agar proses batch
	 * tanpa pengguna aktif tidak menimpa jejak audit yang sudah tercatat.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank (lihat
	 * {@link #setOlehId(String)}).
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mencatat waktu perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Field
	 * {@link #tanggal_dirubah} adalah field AUDIT SHADOW -- inisialisasi
	 * {@code = WaktuUtil.getDate()} saat objek dibuat adalah KEHARUSAN TEKNIS, bukan bug.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; biasanya diisi otomatis lewat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas berupa {@code id-nama}, dipakai untuk log dan tampilan debug. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama calon pemegang/penanggung jawab aset. */
	private String nama;

	/** Catatan/keterangan bebas terkait rujukan pemegang aset ini. */
	private String keterangan;

	/** Jurusan terkait, bila pemegang aset diasosiasikan dengan struktur akademik jurusan tertentu. */
	private Jurusan jurusan;

	/** Fakultas terkait, bila pemegang aset diasosiasikan dengan struktur akademik fakultas tertentu. */
	private Fakultas fakultas;

	/** Perguruan tinggi terkait, bila pemegang aset diasosiasikan dengan institusi tertentu. */
	private PerguruanTinggi perguruanTinggi;

	/** Penanda status aktif rujukan ini; default {@code true} bila belum diisi, lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public PemilikAsset() {
	}

	/** @return kunci utama baris ini, atau {@code null} bila belum persisten. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; kolom {@code insertable=false} sehingga hanya relevan setelah baris ada. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama calon pemegang aset setelah di-trim, atau {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama calon pemegang/penanggung jawab aset; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan/keterangan bebas rujukan ini, atau {@code null} bila tidak ada. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas rujukan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return jurusan terkait, atau {@code null} bila tidak diasosiasikan dengan jurusan tertentu. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/** @param jurusan jurusan terkait rujukan pemegang aset ini. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/** @return fakultas terkait, atau {@code null} bila tidak diasosiasikan dengan fakultas tertentu. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/** @param fakultas fakultas terkait rujukan pemegang aset ini. */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return perguruan tinggi terkait, atau {@code null} bila belum ditetapkan ATAU bila baris
	 *         {@link PerguruanTinggi} yang tertaut belum memiliki id (belum persisten) --
	 *         guard-clause ekstra ini mencegah getter mengembalikan referensi ke entitas
	 *         transient yang bisa memicu error saat dipakai lebih lanjut (mis. serialisasi JSON).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
	}

	/** @param perguruanTinggi perguruan tinggi terkait rujukan pemegang aset ini. */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/** @return {@code true} (default, bila belum diisi) atau nilai tersimpan status aktif rujukan pemegang aset ini. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif rujukan pemegang aset ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
