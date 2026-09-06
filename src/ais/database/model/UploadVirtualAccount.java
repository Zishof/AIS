package ais.database.model;

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

/**
 * Entity <b>batch upload rekonsiliasi virtual account</b> (tabel {@code public.upload_virtual_account})
 * — satu baris mewakili satu SESI/BATCH pengunggahan file mutasi rekening virtual account dari bank
 * ({@link #getBank()}), untuk direkonsiliasi terhadap tagihan {@link #getKegiatan()} pada
 * mahasiswa/calon mahasiswa berdasarkan {@link #getJenisUpload()} (NIM, No. Registrasi, atau No.
 * Ujian). Isi baris per baris hasil parsing file diproses terpisah lewat
 * {@link ais.database.model.file.UploadVirtualAccountFileContent} (sudah didokumentasikan pada batch
 * sebelumnya), sedangkan {@link VirtualAccountBank} (yang tercatat rentan command injection pada
 * task_b0a90191) adalah entity KONFIGURASI bank virtual account per kegiatan — berbeda peran dari
 * entity ini yang murni mencatat riwayat/status satu batch upload.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "upload_virtual_account")

public class UploadVirtualAccount extends GeneralValueObject {

	/** Penanda {@link #getBank()}: Bank BRI. */
	public static String BRI = "bri";
	/** Penanda {@link #getBank()}: Bank NTT. */
	public static String NTT = "bank ntt";
	/** Penanda {@link #getBank()}: Bank BCA. */
	public static String BCA = "bca";

	/** Penanda {@link #getJenisUpload()}: rekonsiliasi pembayaran mahasiswa aktif berdasarkan NIM. */
	public static String JENIS_UPLOAD_NIM = "Pembayaran mahasiswa berdasarkan nim";
	/** Penanda {@link #getJenisUpload()}: rekonsiliasi pembayaran calon mahasiswa berdasarkan No. Registrasi. */
	public static String JENIS_UPLOAD_NO_REG = "Pembayaran calon mahasiswa berdasarkan No. Reg.";
	/** Penanda {@link #getJenisUpload()}: rekonsiliasi pembayaran calon mahasiswa berdasarkan No. Ujian. */
	public static String JENIS_UPLOAD_NO_UJIAN = "Pembayaran calon mahasiswa berdasarkan No. Ujian";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String tipe;
	private String bank;
	private String tahunAkademik;
	private String ganjilGenap;
	private String keterangan;
	private Kegiatan kegiatan;
	private String peringatan;
	private String jenisUpload;
	private Integer terupload;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public UploadVirtualAccount() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama/label batch upload ini, di-trim saat dibaca.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama/label batch upload ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan/catatan bebas tentang batch upload ini.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan/catatan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return tipe/kategori batch upload ini.
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * @param tipe tipe/kategori batch upload ini.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * @return pesan peringatan hasil proses upload (mis. baris gagal diparsing/tidak cocok).
	 */
	@Column(columnDefinition = "text")
	public String getPeringatan() {
		return peringatan;
	}

	/**
	 * @param peringatan pesan peringatan hasil proses upload.
	 */
	public void setPeringatan(String peringatan) {
		this.peringatan = peringatan;
	}

	/**
	 * @return kegiatan (jenis tagihan) yang direkonsiliasi oleh batch upload ini.
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kegiatan")
	public Kegiatan getKegiatan() {
		return kegiatan;
	}

	/**
	 * @param kegiatan kegiatan (jenis tagihan) yang direkonsiliasi oleh batch upload ini.
	 */
	public void setKegiatan(Kegiatan kegiatan) {
		this.kegiatan = kegiatan;
	}

	/**
	 * @return kode bank sumber file (salah satu {@link #BRI}, {@link #NTT}, {@link #BCA}, atau nilai
	 *         lain); default {@code "-"} bila belum diisi (bukan {@code null}).
	 */
	public String getBank() {
		if (bank == null) {
			bank = "-";
		}
		return bank;
	}

	/**
	 * @param bank kode bank sumber file.
	 */
	public void setBank(String bank) {
		this.bank = bank;
	}

	/**
	 * @return tahun akademik yang menjadi target rekonsiliasi batch ini. Catatan: logika default
	 *         (mengisi otomatis dari {@code Common.getCurrentTahunAkademik()}) DIKOMENTARI nonaktif
	 *         pada kode sumber — sengaja dibiarkan apa adanya, sehingga field ini {@code null} bila
	 *         memang belum pernah diisi.
	 */
	public String getTahunAkademik() {
		// if (tahunAkademik == null) {
		// tahunAkademik = Common.getCurrentTahunAkademik();
		// }
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik tahun akademik yang menjadi target rekonsiliasi batch ini.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return penanda ganjil/genap semester target rekonsiliasi batch ini. Catatan: logika default
	 *         (menghitung dari semester berjalan) DIKOMENTARI nonaktif pada kode sumber — sengaja
	 *         dibiarkan apa adanya.
	 */
	public String getGanjilGenap() {
		// if (ganjilGenap == null) {
		// ganjilGenap = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL
		// : Perkuliahan.GENAP;
		// }
		return ganjilGenap;
	}

	/**
	 * @param ganjilGenap penanda ganjil/genap semester target rekonsiliasi batch ini.
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * @return kode batch upload ini, di-trim saat dibaca (efek samping: menormalisasi field
	 *         {@link #kode} bila sudah pernah diisi — getter-mutasi ringan).
	 */
	public String getKode() {
		if (kode != null) {
			kode = kode.trim();
		}
		return kode;
	}

	/**
	 * @param kode kode batch upload ini.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return jenis pencocokan rekonsiliasi batch ini — salah satu {@link #JENIS_UPLOAD_NIM},
	 *         {@link #JENIS_UPLOAD_NO_REG}, {@link #JENIS_UPLOAD_NO_UJIAN}.
	 */
	public String getJenisUpload() {
		return jenisUpload;
	}

	/**
	 * @param jenisUpload jenis pencocokan rekonsiliasi batch ini.
	 */
	public void setJenisUpload(String jenisUpload) {
		this.jenisUpload = jenisUpload;
	}

	/**
	 * @return jumlah baris yang berhasil direkonsiliasi/diunggah pada batch ini; default {@code 0}
	 *         bila belum diisi.
	 */
	public Integer getTerupload() {
		if (terupload == null) {
			terupload = 0;
		}
		return terupload;
	}

	/**
	 * @param terupload jumlah baris yang berhasil direkonsiliasi/diunggah pada batch ini.
	 */
	public void setTerupload(Integer terupload) {
		this.terupload = terupload;
	}

}
