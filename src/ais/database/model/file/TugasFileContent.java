package ais.database.model.file;

// Generated May 15, 2010 10:07:50 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
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
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Berkas hasil unggahan/pengumpulan atas sebuah {@code Tugas} (materi pekerjaan) -- tabel
 * {@code tugas_file_content}, subclass {@link FileFotoLain}. Berbeda dari kebanyakan subclass
 * {@link FileFotoLain} lain, kelas ini TIDAK punya kolom acuan pemilik tunggal: {@code
 * RELASI_MAP} pada {@link FileFotoLain} mendaftarkan kelas ini dengan nama field {@code "id"},
 * sehingga {@link #ambilRef()} sengaja mengembalikan primary key baris ini sendiri ({@link
 * #getId()}), BUKAN salah satu dari {@link #getMahasiswa()}/{@link #getSiswa()}/{@link
 * #getCalonSiswa()}/{@link #getBiodataCalonMahasiswa()}/{@link #getPertemuan()} -- konsisten
 * dengan {@code PertemuanFileContent}, {@code AudioPertemuan}, dan {@code VideoPertemuan} yang
 * didaftarkan pada golongan yang sama. Akibatnya penyaringan berbasis {@code jenis} pada {@link
 * FileFotoLain#ambil} dimatikan untuk golongan ini, dan penghapusan non-{@code usingId} tidak
 * melakukan apa pun (lihat Javadoc {@code RELASI_MAP} dan {@code SOFT_DELETE_ID} pada
 * {@link FileFotoLain}).
 *
 * <p><b>Empat jenis pemilik sekaligus, tidak saling eksklusif secara skema.</b> Satu baris
 * membawa LIMA kolom acuan pemilik yang independen: {@link #getPertemuan()} (id {@code Tugas}
 * atau entity lain, lihat {@link #getClassFrom()}), {@link #getMahasiswa()}, {@link
 * #getSiswa()}, {@link #getCalonSiswa()}, dan {@link #getBiodataCalonMahasiswa()}. Skema tidak
 * memaksakan tepat satu di antaranya terisi -- pemanggil (mis. {@code
 * ais.action.master.helper.generic.AmbilDataTugasFileContent}) bertanggung jawab mengisi field
 * yang relevan sesuai konteks (mahasiswa kuliah, siswa sekolah, calon siswa PSB, atau calon
 * mahasiswa PMB) dan MENGISI sentinel negatif ({@code -Common.randLong()}) pada kolom yang tidak
 * relevan agar tidak tertinggal {@code null}/nilai baris lain dari objek yang didaur ulang.
 * Salah isi FK pada kolom yang salah (menyalin nilai dari objek pemilik yang keliru) akan
 * membuat berkas tertaut ke baris siswa/mahasiswa yang SALAH -- kelas model ini sendiri tidak
 * memvalidasi konsistensi kelima kolom tersebut; validasi sepenuhnya ada di pemanggil.</p>
 *
 * <p><b>{@link #getClassFrom()}</b> menyimpan nama kelas Java pemilik konseptual baris ini
 * (default {@code Pertemuan.class}, tetapi konstruktor {@link #TugasFileContent(String)}
 * mengizinkan kelas lain, mis. {@code Tugas.class}), dipakai murni sebagai metadata/penanda
 * konteks -- TIDAK memengaruhi query atau validasi FK di atas.</p>
 *
 * <p><b>Google Drive sebagai sumber alternatif.</b> {@link #getGdrive()}/{@link
 * #getGdriveUsername()} TIDAK dipetakan sebagai kolom JPA; nilainya disimpan lewat cache berkas
 * per-instance {@link ais.database.model.GeneralValueObject#put(String, String) put}/{@link
 * ais.database.model.GeneralValueObject#retreive(String) retreive} milik {@code
 * GeneralValueObject} induk. Selama {@link #getGdrive()} terisi, {@link #getFoto()} sengaja
 * mengembalikan {@code null} sebagai pertanda berkas asli harus diambil dari Google Drive.</p>
 *
 * <p><b>Baris "copy".</b> {@link #getCopyDari()} adalah asosiasi opsional ke baris
 * {@code TugasFileContent} lain; ketika terisi, {@link #getNama()} dan {@link #getFoto()}
 * membaca nilainya dari baris sumber tersebut -- pola berbagi satu berkas fisik di antara banyak
 * baris tanpa menduplikasi blob, sama seperti subclass {@link FileFoto} lain.</p>
 *
 * @see FileFotoLain
 * @see PertemuanFileContent
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "tugas_file_content")
public class TugasFileContent extends FileFotoLain {
	/**
	 * Path/lokasi penyimpanan lokal baris ini. Field ini MENIMPA (shadow) field privat sejenis di
	 * {@link FileFoto}: tidak diberi anotasi JPA ({@code @Column}), jadi bukan kolom ter-mapping --
	 * getter/setter di sini hanya menyediakan state in-memory milik baris.
	 */
	private String lokasiSimpan;

	/** @return {@link #lokasiSimpan}, path penyimpanan lokal baris ini (bukan kolom database). */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/** @param lokasiSimpan path penyimpanan lokal baru untuk baris ini. */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 8396956558947881138L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOlehId() {
		return olehId;
	}

	/** Menetapkan id pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Menetapkan nama pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai timestamp perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir baris ini; lihat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Membandingkan urutan tampil baris ini terhadap baris {@link GeneralValueObject} lain,
	 * mencoba beberapa kriteria berurutan sampai salah satu cocok (fallback chain):
	 * <ol>
	 *   <li>{@link #getUploadDate()} milik kedua baris, HANYA bila {@code arg0} juga instance
	 *       {@code TugasFileContent} (dibandingkan sebagai {@code TugasFileContent}, bukan lewat
	 *       method {@code getUploadDate()} generik {@link GeneralValueObject} yang mungkin punya
	 *       makna berbeda pada subclass lain);</li>
	 *   <li>{@code getNomorUrut()} kedua baris (method {@link GeneralValueObject}, dipanggil
	 *       lewat {@code this} secara implisit dan {@code arg0} secara eksplisit);</li>
	 *   <li>{@code getNim()} kedua baris;</li>
	 *   <li>{@link #getNama()}/{@code getNama()} kedua baris;</li>
	 *   <li>{@link #getKeterangan()}/{@code getKeterangan()} kedua baris.</li>
	 * </ol>
	 * <p>Kegagalan apa pun (mis. {@code NullPointerException} dari pemanggilan berantai, atau
	 * {@code ClassCastException}) ditangkap dan dianggap "setara" ({@code 0}) -- kontrak {@link
	 * Comparable} yang longgar, cocok untuk pengurutan tampilan (bukan untuk struktur data yang
	 * menuntut total order konsisten seperti {@code TreeSet}).</p>
	 *
	 * @param arg0 baris pembanding.
	 * @return negatif/nol/positif mengikuti kriteria pertama yang cocok, atau {@code 0} bila
	 *         tidak ada kriteria yang cocok atau terjadi kegagalan.
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (getUploadDate() != null && (arg0 instanceof TugasFileContent)
					&& ((TugasFileContent) arg0).getUploadDate() != null) {
				return getUploadDate().compareTo(((TugasFileContent) arg0).getUploadDate());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/TugasFileContent.java:116");

		}

		return 0;
	}

	/** @return gabungan {@link #realFile} dan {@link #fileMimeType} dipisah garis bawah; bisa memuat literal {@code "null"} bila salah satu belum diisi. */
	public String toString() {
		return realFile + "_" + fileMimeType;
	}

	private Long pertemuan;
	private Long mahasiswa;
	private Long biodataCalonMahasiswa;
	private Long siswa;
	private Long calonSiswa;
	private Blob foto;
	private String realFile;
	private String fileMimeType;
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();
	private Double nilai = 0.0;
	private String keterangan;
	private String namaTemp;
	private TugasFileContent copyDari;
	private String classFrom;
	private String link;
	private String kodeUpload;

	/** @return {@link #jenis} apa adanya (bisa {@code null} bila {@link #getJenis()} belum pernah dipanggil sebelumnya). */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	private String jenis;

	/** Nilai default {@link #getJenis()}/{@link #jenis}: penanda jenis lampiran untuk baris berkas tugas. */
	public static String DEFAULT_JENIS = "tugas file";

	/** @return keterangan berkas ini, atau string kosong (bukan {@code null}) bila belum diisi. */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/** @param keterangan keterangan berkas ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menghapus prefiks {@code "<NIS>_<nama siswa>"} dari {@link #getNama()} bila ada, untuk
	 * menampilkan nama berkas asli tanpa penanda kepemilikan yang ditambahkan {@link
	 * #ubahRealNameSesuaiDenganNIM(Siswa)}. CATATAN: method ini memakai {@link
	 * Siswa#getNomorInduk() siswa.getNomorInduk()} (NIS) untuk membentuk prefiks yang dicari,
	 * SEDANGKAN {@link #ubahRealNameSesuaiDenganNIM(Siswa)} membentuk prefiks yang ditulis dari
	 * {@link Siswa#getNim() siswa.getNim()} (alias NISN, kolom berbeda) -- akibatnya bila nama
	 * berkas ditulis lewat {@code ubahRealNameSesuaiDenganNIM(Siswa)}, method ini TIDAK PERNAH
	 * berhasil mencocokkan dan menghapus prefiksnya (semua percobaan {@code replaceAll} tidak
	 * match), sehingga nama yang dikembalikan tetap memuat prefiks NISN yang seharusnya
	 * disembunyikan. Prefiks dihapus di tiga variasi posisi (diapit {@code _}, di akhir, di awal)
	 * sebelum jatuh ke variasi tanpa apit sama sekali.
	 *
	 * @param siswa siswa pemilik konseptual berkas ini; method tidak mengubah apa pun bila
	 *              {@code null} atau {@link Siswa#getNomorInduk()}-nya {@code null}.
	 * @return nama berkas tanpa prefiks NIS bila cocok ditemukan, atau {@link #getNama()} apa
	 *         adanya bila tidak cocok atau terjadi kegagalan (dicatat lewat {@code
	 *         Common.tampilErrorJikaAdmin}).
	 */
	public String ambilRealNameSesuaiDenganNIM(Siswa siswa) {
		try {
			if (siswa != null && siswa.getNomorInduk() != null) {
				String nama = siswa.getNomorInduk().trim() + "_" + siswa.getNama();

				String n = getNama();
				String n1 = n.contains(nama) ? n.replaceAll("_" + nama + "_", "") : n;
				String n2 = n1.contains(nama) ? n1.replaceAll("_" + nama, "") : n1;
				String n3 = n2.contains(nama) ? n2.replaceAll(nama + "_", "") : n2;
				String n4 = n3.contains(nama) ? n3.replaceAll(nama, "") : n3;
				return n4;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return getNama();
	}

	/**
	 * Menghapus prefiks {@code "<NIM>_<nama mahasiswa>"} dari {@link #getNama()} bila ada, untuk
	 * menampilkan nama berkas asli tanpa penanda kepemilikan yang ditambahkan {@link
	 * #ubahRealNameSesuaiDenganNIM(Mahasiswa)}. Prefiks dihapus di tiga variasi posisi (diapit
	 * {@code _}, di akhir, di awal) sebelum jatuh ke variasi tanpa apit sama sekali.
	 *
	 * @param mahasiswa mahasiswa pemilik konseptual berkas ini; method tidak mengubah apa pun
	 *                  bila {@code null} atau {@code mahasiswa.getNim()}-nya {@code null}.
	 * @return nama berkas tanpa prefiks NIM bila cocok ditemukan, atau {@link #getNama()} apa
	 *         adanya bila tidak cocok atau terjadi kegagalan (dicatat lewat {@code
	 *         Common.tampilErrorJikaAdmin}).
	 */
	public String ambilRealNameSesuaiDenganNIM(Mahasiswa mahasiswa) {
		try {
			if (mahasiswa != null && mahasiswa.getNim() != null) {
				String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama();

				String n = getNama();
				String n1 = n.contains(nama) ? n.replaceAll("_" + nama + "_", "") : n;
				String n2 = n1.contains(nama) ? n1.replaceAll("_" + nama, "") : n1;
				String n3 = n2.contains(nama) ? n2.replaceAll(nama + "_", "") : n2;
				String n4 = n3.contains(nama) ? n3.replaceAll(nama, "") : n3;
				return n4;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return getNama();
	}

	/**
	 * Menghapus prefiks {@code "<no registrasi>_<nama>"} dari {@link #getNama()} bila ada, untuk
	 * menampilkan nama berkas asli tanpa penanda kepemilikan yang ditambahkan {@link
	 * #ubahRealNameSesuaiDenganNIM(BiodataCalonMahasiswa)}. Prefiks dihapus di tiga variasi
	 * posisi (diapit {@code _}, di akhir, di awal) sebelum jatuh ke variasi tanpa apit sama
	 * sekali.
	 *
	 * @param mahasiswa calon mahasiswa (PMB) pemilik konseptual berkas ini; method tidak
	 *                  mengubah apa pun bila {@code null} atau nomor registrasinya {@code null}.
	 * @return nama berkas tanpa prefiks nomor registrasi bila cocok ditemukan, atau {@link
	 *         #getNama()} apa adanya bila tidak cocok atau terjadi kegagalan (dicatat lewat
	 *         {@code Common.tampilErrorJikaAdmin}).
	 */
	public String ambilRealNameSesuaiDenganNIM(BiodataCalonMahasiswa mahasiswa) {
		try {
			if (mahasiswa != null && mahasiswa.getNoRegistrasi() != null) {
				String nama = mahasiswa.getNoRegistrasi().trim() + "_" + mahasiswa.getNama();
				String n = getNama();
				String n1 = n.contains(nama) ? n.replaceAll("_" + nama + "_", "") : n;
				String n2 = n1.contains(nama) ? n1.replaceAll("_" + nama, "") : n1;
				String n3 = n2.contains(nama) ? n2.replaceAll(nama + "_", "") : n2;
				String n4 = n3.contains(nama) ? n3.replaceAll(nama, "") : n3;
				return n4;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return getNama();
	}

	/**
	 * Menambahkan prefiks {@code "<NIM>_<nama mahasiswa>_"} ke {@link #getNama()} dan
	 * menyimpannya lewat {@link #setNama(String)}, HANYA bila nama saat ini belum berawalan NIM
	 * tersebut (dibandingkan case-insensitive) -- mencegah penambahan prefiks berulang pada
	 * pemanggilan berkali-kali. Pasangan penulis untuk {@link
	 * #ambilRealNameSesuaiDenganNIM(Mahasiswa)}.
	 *
	 * @param mahasiswa mahasiswa pemilik konseptual berkas ini; method tidak mengubah apa pun
	 *                  bila {@code null}, {@code mahasiswa.getNim()}-nya {@code null}, atau nama
	 *                  sudah berawalan NIM tersebut.
	 */
	public void ubahRealNameSesuaiDenganNIM(Mahasiswa mahasiswa) {
		try {
			if (mahasiswa != null && mahasiswa.getNim() != null
					&& !getNama().trim().toLowerCase().startsWith(mahasiswa.getNim().trim())) {
				String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_" + getNama();
				setNama(nama);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menambahkan prefiks {@code "<no registrasi>_<nama>_"} ke {@link #getNama()} dan
	 * menyimpannya lewat {@link #setNama(String)}, HANYA bila nama saat ini belum berawalan
	 * nomor registrasi tersebut (dibandingkan case-insensitive) -- mencegah penambahan prefiks
	 * berulang pada pemanggilan berkali-kali. Pasangan penulis untuk {@link
	 * #ambilRealNameSesuaiDenganNIM(BiodataCalonMahasiswa)}.
	 *
	 * @param mahasiswa calon mahasiswa (PMB) pemilik konseptual berkas ini; method tidak
	 *                  mengubah apa pun bila {@code null}, nomor registrasinya {@code null},
	 *                  atau nama sudah berawalan nomor registrasi tersebut.
	 */
	public void ubahRealNameSesuaiDenganNIM(BiodataCalonMahasiswa mahasiswa) {
		try {
			if (mahasiswa != null && mahasiswa.getNoRegistrasi() != null
					&& !getNama().trim().toLowerCase().startsWith(mahasiswa.getNoRegistrasi().trim())) {
				String nama = mahasiswa.getNoRegistrasi().trim() + "_" + mahasiswa.getNama() + "_" + getNama();
				setNama(nama);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menambahkan prefiks {@code "<NISN>_<nama siswa>_"} ke {@link #getNama()} dan
	 * menyimpannya lewat {@link #setNama(String)}, HANYA bila nama saat ini belum berawalan
	 * NISN tersebut (dibandingkan case-insensitive) -- mencegah penambahan prefiks berulang pada
	 * pemanggilan berkali-kali.
	 *
	 * <p><b>CATATAN inkonsistensi identifier.</b> Method ini memakai {@link Siswa#getNim()
	 * siswa.getNim()} (alias kolom {@code nomor_induk_nasional}/NISN) untuk membentuk prefiks
	 * yang DITULIS, sedangkan {@link #ambilRealNameSesuaiDenganNIM(Siswa)} (pasangan
	 * "pembaca"-nya) mencari prefiks berdasarkan {@link Siswa#getNomorInduk()} (kolom
	 * {@code nomor_induk}/NIS) -- dua kolom berbeda pada entity {@code Siswa} yang sama. Prefiks
	 * yang ditulis method ini TIDAK PERNAH berhasil dihapus oleh {@code
	 * ambilRealNameSesuaiDenganNIM(Siswa)}; keduanya perlu disamakan (memakai kolom identifier
	 * yang sama) agar pasangan tulis/baca ini bekerja sebagaimana mestinya.</p>
	 *
	 * @param siswa siswa pemilik konseptual berkas ini; method tidak mengubah apa pun bila
	 *              {@code null}, {@link Siswa#getNim()}-nya {@code null}, atau nama sudah
	 *              berawalan NISN tersebut.
	 */
	public void ubahRealNameSesuaiDenganNIM(Siswa siswa) {
		try {
			if (siswa != null && siswa.getNim() != null
					&& !getNama().trim().toLowerCase().startsWith(siswa.getNim().trim())) {
				String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_" + getNama();
				setNama(nama);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menambahkan prefiks {@code "<nomor induk>_<nama>_"} ke {@link #getNama()} dan
	 * menyimpannya lewat {@link #setNama(String)}, HANYA bila nama saat ini belum berawalan
	 * nomor induk tersebut (dibandingkan case-insensitive) -- mencegah penambahan prefiks
	 * berulang pada pemanggilan berkali-kali. Berbeda dari {@link
	 * #ubahRealNameSesuaiDenganNIM(Siswa)}, method ini TIDAK punya pasangan
	 * {@code ambilRealNameSesuaiDenganNIM(CalonSiswa)} pada kelas ini.
	 *
	 * @param siswa calon siswa (PSB) pemilik konseptual berkas ini; method tidak mengubah apa
	 *              pun bila {@code null}, nomor induknya {@code null}, atau nama sudah berawalan
	 *              nomor induk tersebut.
	 */
	public void ubahRealNameSesuaiDenganNIM(CalonSiswa siswa) {
		try {
			if (siswa != null && siswa.getNomorInduk() != null
					&& !getNama().trim().toLowerCase().startsWith(siswa.getNomorInduk().trim())) {
				String nama = siswa.getNomorInduk().trim() + "_" + siswa.getNama() + "_" + getNama();
				setNama(nama);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Konstruktor default (dipakai Hibernate); menyetel {@link #classFrom} ke {@code Pertemuan.class.getName()}. */
	public TugasFileContent() {
		this.classFrom = Pertemuan.class.getName();
	}

	/** @param classFrom nama kelas Java pemilik konseptual baris ini (lihat {@link #getClassFrom()}). */
	public TugasFileContent(String classFrom) {
		this.classFrom = classFrom;
	}

	/** @return primary key baris ini; kolom identity, tidak pernah di-{@code INSERT} manual. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return id acuan pemilik konseptual (biasanya {@code Tugas} atau {@link Pertemuan}, lihat
	 *         {@link #getClassFrom()}). CATATAN: berbeda dari {@link #ambilRef()} (yang
	 *         mengembalikan {@link #getId()} sendiri sesuai golongan {@code RELASI_MAP}
	 *         {@code "id"} pada {@link FileFotoLain}), kolom ini TETAP ada dan diisi pemanggil
	 *         (mis. {@code AmbilDataTugasFileContent}) untuk mencatat konteks pengumpulan tugas
	 *         -- hanya tidak dipakai sebagai kunci pencarian lampiran generik {@link
	 *         FileFotoLain#ambil}.
	 */
	@Column(name = "pertemuan")
	public Long getPertemuan() {
		return this.pertemuan;
	}

	/** @param pertemuan id acuan pemilik konseptual baris ini. */
	public void setPertemuan(Long pertemuan) {
		this.pertemuan = pertemuan;
	}

	/** @param fileMimeType mime-type berkas ini. */
	public void setFileMimeType(String fileMimeType) {
		this.fileMimeType = fileMimeType;
	}

	/**
	 * @return mime-type berkas ini, atau {@code null} bila belum diisi. Bila {@link #copyDari}
	 *         terisi, nilainya disegarkan lebih dulu dari mime-type baris sumber.
	 */
	@Column(name = "file_mime_tipe", length = 255)
	public String getFileMimeType() {
		if (copyDari != null) {
			fileMimeType = copyDari.fileMimeType;
		}
		return fileMimeType;
	}

	/** @param uploadDate waktu unggah berkas ini. */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/**
	 * @return waktu unggah berkas ini. Berbeda dari kebanyakan subclass {@link FileFoto} lain
	 *         (yang menginisialisasi field waktu ke "sekarang" sekali di deklarasi field), getter
	 *         ini mengembalikan {@link WaktuUtil#getDate()} SEGAR setiap kali dipanggil apabila
	 *         {@link #uploadDate} masih {@code null} -- nilai yang dikembalikan bisa berbeda
	 *         antar-panggilan sampai field-nya benar-benar diisi lewat {@link
	 *         #setUploadDate(Date)} atau dipersist.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate == null ? WaktuUtil.getDate() : uploadDate;
	}

	/** @param realFile nama berkas asli (disimpan ke kolom {@code real_file}). */
	public void setNama(String realFile) {
		this.realFile = realFile;
	}

	/**
	 * @return nama berkas ini, dipetakan ke kolom {@code real_file}. Bila {@link #copyDari}
	 *         terisi, nilainya disegarkan dari {@code copyDari.getNama()} (rekursif, ikut memicu
	 *         default milik baris sumber). Jika tidak, dan {@link #getLink()} terisi (baris
	 *         berupa tautan, bukan berkas biner) sementara nama masih kosong, default-nya
	 *         {@code "berupa_link.txt"}; selain itu (berkas biner biasa) default-nya
	 *         {@code "file___<epoch millis saat ini>"}. Ketiga default DITULISKAN BALIK ke field
	 *         {@link #realFile} sebagai efek samping getter.
	 */
	@Column(name = "real_file", length = 255)
	public String getNama() {
		if (copyDari != null) {
			realFile = copyDari.getNama();
		} else if (getLink() != null && !getLink().trim().isEmpty()) {
			if (realFile == null || realFile.trim().isEmpty()) {
				realFile = "berupa_link.txt";
			}
		} else {
			if (realFile == null || realFile.trim().isEmpty()) {
				realFile = "file___" + (ais.ui.util.WaktuUtil.getDate().getTime());
			}
		}
		return realFile;
	}

	/** @param foto isi biner berkas ini. */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * @return {@code null} bila {@link #getGdrive()} terisi (berkas asli ada di Google Drive, bukan
	 *         di kolom ini); jika tidak, blob milik {@link #copyDari} bila terisi, atau blob baris
	 *         ini sendiri. Dipetakan ke kolom {@code filecontent} dan tidak diaudit
	 *         ({@code @NotAudited}) karena isi biner tidak perlu dilacak riwayatnya oleh Envers.
	 */
	@NotAudited
	@Column(name = "filecontent")
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/** @param mahasiswa id {@code Mahasiswa} pemilik berkas ini, atau sentinel bila tidak relevan (lihat Javadoc kelas). */
	public void setMahasiswa(Long mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return id {@code Mahasiswa} pemilik berkas ini, atau {@code null}/sentinel bila tidak relevan (lihat Javadoc kelas). */
	@Column(name = "mahasiswa")
	public Long getMahasiswa() {
		return mahasiswa;
	}

	/** @param nilai nilai/skor yang diberikan atas berkas ini. */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/** @return nilai/skor yang diberikan atas berkas ini, atau {@code 0.0} bila belum dinilai. */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/** @return id {@code BiodataCalonMahasiswa} (PMB) pemilik berkas ini, atau {@code null}/sentinel bila tidak relevan (lihat Javadoc kelas). */
	public Long getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa id {@code BiodataCalonMahasiswa} (PMB) pemilik berkas ini, atau sentinel bila tidak relevan. */
	public void setBiodataCalonMahasiswa(Long biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/** @return id {@code CalonSiswa} (PSB) pemilik berkas ini, atau {@code null}/sentinel bila tidak relevan (lihat Javadoc kelas). */
	public Long getCalonSiswa() {
		return calonSiswa;
	}

	/** @param calonSiswa id {@code CalonSiswa} (PSB) pemilik berkas ini, atau sentinel bila tidak relevan. */
	public void setCalonSiswa(Long calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/** @return id {@code Siswa} pemilik berkas ini, atau {@code null}/sentinel bila tidak relevan (lihat Javadoc kelas). */
	public Long getSiswa() {
		return siswa;
	}

	/** @param siswa id {@code Siswa} pemilik berkas ini, atau sentinel bila tidak relevan. */
	public void setSiswa(Long siswa) {
		this.siswa = siswa;
	}

	/**
	 * @return baris {@code TugasFileContent} sumber bila baris ini adalah "copy" yang berbagi
	 *         berkas fisik dengan baris lain; {@code null} bila baris ini berdiri sendiri.
	 *         {@code NotFoundAction.IGNORE} membuat asosiasi yang menunjuk baris yang sudah
	 *         terhapus diperlakukan sebagai {@code null}, bukan melempar exception.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public TugasFileContent getCopyDari() {
		return copyDari;
	}

	/** @param copyDari baris sumber untuk berbagi berkas fisik (lihat {@link #getCopyDari()}). */
	public void setCopyDari(TugasFileContent copyDari) {
		this.copyDari = copyDari;
	}

	/**
	 * @return tautan materi eksternal (di-trim), atau {@code null} bila kosong. Bila {@link
	 *         #copyDari} terisi, nilainya disegarkan lebih dulu dari tautan baris sumber.
	 */
	@Column(columnDefinition = "text", nullable = true)
	public String getLink() {
		if (copyDari != null) {
			link = copyDari.link;
		}
		return link == null || link.trim().isEmpty() ? null : link.trim();
	}

	/** @param link tautan materi eksternal. */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * @return nama kelas Java pemilik konseptual baris ini, murni metadata/penanda konteks
	 *         (lihat Javadoc kelas). Bila kolom kosong/belum diisi, getter ini menuliskan balik
	 *         default {@code Pertemuan.class.getName()} ke field in-memory sebelum
	 *         mengembalikannya.
	 */
	@Column(name = "class_from", nullable = true)
	public String getClassFrom() {
		if (classFrom == null || classFrom.trim().isEmpty()) {
			classFrom = Pertemuan.class.getName();
		}
		return classFrom;
	}

	/** @param classFrom nama kelas Java pemilik konseptual baris ini. */
	public void setClassFrom(String classFrom) {
		this.classFrom = classFrom;
	}

	private String gdrive;
	private String gdriveUsername;

	/**
	 * @return URL Google Drive tempat berkas sesungguhnya disimpan, atau {@code null} bila berkas
	 *         disimpan sebagai blob biasa. Bukan kolom JPA -- nilainya dibaca dari cache berkas
	 *         per-instance {@link ais.database.model.GeneralValueObject#retreive(String)
	 *         retreive("gdrive")} milik induk, dengan penyegaran lebih dulu dari {@link #copyDari}
	 *         bila terisi.
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Menetapkan URL Google Drive berkas ini. Nilai tidak kosong ditulis ke cache berkas
	 * per-instance lewat {@link ais.database.model.GeneralValueObject#put(String, String)
	 * put(gdrive, "gdrive")} milik induk -- BUKAN ke kolom database.
	 *
	 * @param gdrive URL Google Drive baru; {@code null}/kosong tidak ditulis ke cache (hanya
	 *               mengubah field in-memory).
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * @return nama pengguna akun Google Drive terkait, disegarkan lebih dulu dari {@link
	 *         #copyDari} bila terisi. Bukan kolom JPA -- murni field in-memory.
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/** @param gdriveUsername nama pengguna akun Google Drive terkait. */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/** @return {@link #getLink()}, tautan materi eksternal ini. */
	@Override
	public String ambilLink() {
		return getLink();
	}

	/**
	 * @return {@link #getId()}, primary key baris ini sendiri -- BUKAN salah satu dari {@link
	 *         #getMahasiswa()}/{@link #getSiswa()}/{@link #getCalonSiswa()}/{@link
	 *         #getBiodataCalonMahasiswa()}/{@link #getPertemuan()}. Sengaja demikian:
	 *         {@code RELASI_MAP} pada {@link FileFotoLain} mendaftarkan kelas ini dengan nama
	 *         field {@code "id"} (golongan ketiga: entity tanpa kolom acuan pemilik tunggal,
	 *         {@code ref} dicocokkan langsung ke primary key), konsisten dengan {@code
	 *         PertemuanFileContent}/{@code AudioPertemuan}/{@code VideoPertemuan}. Lihat Javadoc
	 *         {@code RELASI_MAP} pada {@link FileFotoLain} untuk akibat golongan ini terhadap
	 *         penyaringan {@code jenis} dan perilaku penghapusan.
	 */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return id;
	}

	/** @return kelas runtime baris ini (selalu {@code TugasFileContent.class} kecuali lewat proxy Hibernate). */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		// TODO Auto-generated method stub
		return getClass();
	}

	/**
	 * @return selalu {@link #DEFAULT_JENIS} ({@code "tugas file"}). Getter ini MENULISKAN balik
	 *         nilai tersebut ke field {@link #jenis} sebagai efek samping setiap dipanggil,
	 *         menimpa apa pun yang mungkin sebelumnya disetel lewat {@link #setJenis(String)} --
	 *         konsisten dengan golongan {@code RELASI_MAP} {@code "id"} pada {@link
	 *         FileFotoLain} yang mematikan penyaringan berbasis {@code jenis} untuk kelas ini,
	 *         sehingga nilai {@code jenis} yang berbeda-beda tidak relevan.
	 */
	@Override
	@Column(name = "jenis", length = 20)
	public String getJenis() {
		// TODO Auto-generated method stub
		jenis = DEFAULT_JENIS;
		return jenis;
	}

	/** @param jenis nilai jenis lampiran; akan ditimpa {@link #DEFAULT_JENIS} pada pemanggilan {@link #getJenis()} berikutnya. */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	private String url;

	/**
	 * @return URL akses berkas ini, dihitung malas (lazy) sekali lewat {@code createLinkUri()}
	 *         milik {@link FileFotoLain} lalu di-cache pada field {@link #url}. Kolom
	 *         {@code @Transient} -- tidak pernah dipersist, dihitung ulang setiap objek baru
	 *         dimuat. Kegagalan penghitungan dicatat lewat {@code ErrorAuditUtil} dan
	 *         menghasilkan {@code null} alih-alih exception ke pemanggil.
	 */
	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/TugasFileContent.java:487");
		}
		return url;
	}

	/** @param url URL akses berkas ini; menimpa cache lazy pada {@link #getUrl()}. */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Membentuk label identitas pemilik berkas ini untuk tampilan, memuat ulang entity {@link
	 * Mahasiswa}/{@link Siswa} lewat {@code ConstantValues.ambil(...)} berdasarkan {@link
	 * #getMahasiswa()}/{@link #getSiswa()}, lalu menuliskan hasilnya ke field {@link #namaTemp}
	 * sebagai efek samping (bukan murni fungsi baca).
	 *
	 * <p>Mahasiswa diprioritaskan: bila {@link #getMahasiswa()} mengarah ke baris yang valid,
	 * labelnya {@code "<NIM>-<nama>"} dan cabang siswa TIDAK diperiksa sama sekali (meski
	 * {@link #getSiswa()} kebetulan juga terisi). Untuk siswa, nomor identitas yang dipakai
	 * adalah {@link Siswa#getNomorInduk() NIS} bila tidak kosong, jatuh ke {@code
	 * getNomorIndukNasional()} (NISN) bila NIS kosong. Kolom {@code @Transient} -- tidak pernah
	 * dipersist. Kegagalan apa pun (mis. entity sudah terhapus) dicatat lewat
	 * {@code ErrorAuditUtil} dan menghasilkan nilai {@link #namaTemp} sebelumnya (bisa
	 * {@code null}) alih-alih exception ke pemanggil.
	 *
	 * @return label {@code "<identitas>-<nama>"} pemilik berkas ini, atau {@code null} bila
	 *         belum pernah berhasil dihitung.
	 */
	@Transient
	public String getNamaTemp() {

		try {
			Mahasiswa mahasiswa = (Mahasiswa) (getMahasiswa() == null ? null
					: ConstantValues.ambil(Mahasiswa.class.getName(), getMahasiswa()));
			Siswa siswa = (Siswa) (getSiswa() == null ? null : ConstantValues.ambil(Siswa.class.getName(), getSiswa()));

			if (mahasiswa != null) {
				namaTemp = mahasiswa.getNim() + "-" + mahasiswa.getNama();
			} else if (siswa != null) {
				namaTemp = (siswa.getNomorInduk().isEmpty() ? siswa.getNomorIndukNasional() : siswa.getNomorInduk())
						+ "-" + siswa.getNama();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/TugasFileContent.java:511");
		}

		return namaTemp;
	}

	/** @param namaTemp label identitas pemilik berkas ini; menimpa cache lazy pada {@link #getNamaTemp()}. */
	public void setNamaTemp(String namaTemp) {
		this.namaTemp = namaTemp;
	}

	/** @return kode pengelompokan sesi upload (dipakai untuk menautkan beberapa berkas yang diunggah dalam satu aksi), atau {@code null}. */
	public String getKodeUpload() {
		return kodeUpload;
	}

	/** @param kodeUpload kode pengelompokan sesi upload. */
	public void setKodeUpload(String kodeUpload) {
		this.kodeUpload = kodeUpload;
	}
}
