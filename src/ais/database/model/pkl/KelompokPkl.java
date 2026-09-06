package ais.database.model.pkl;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.KerjasamaAntarInstansi;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Pkl;
import ais.database.model.Sertifikat;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.VOPembelajaran;
import ais.database.model.asset.Lokasi;

/**
 * Entity <b>kelompok tempat penempatan PKL</b> — unit magang lapangan mahasiswa (atau siswa, lihat
 * bagian "Perluasan siswa sekolah" di bawah) peserta PKL — pada tabel {@code public.kelompok_pkl}.
 * Satu baris mewakili SATU kelompok: lokasi/instansi penempatan, rentang tanggal pelaksanaan, kuota
 * anggota, hingga sepuluh slot dosen pembimbing ({@link #getDosen_pembimbing1()} s.d.
 * {@link #getDosen_pembimbing10()}). Kelas ini adalah turunan
 * {@link ais.database.model.VOPembelajaran} ("Value Object Pembelajaran") — pola yang dipakai
 * bersama modul akademik lain (KKN, skripsi, magang, dsb) untuk merepresentasikan "wadah
 * pembelajaran" yang punya anggota, penilaian, dan agenda/pertemuan.
 *
 * <h3>Kedudukan dalam alur PKL</h3>
 * <p>Satu {@link Pkl} (program PKL) dipecah menjadi banyak {@code KelompokPkl}: mahasiswa yang
 * lolos seleksi pendaftaran (lihat {@link MahasiswaDaftarPkl}) ditempatkan ke satu kelompok,
 * direkam sebagai {@link MahasiswaDapatKelompokPkl} — relasi keanggotaan yang, TIDAK SEPERTI
 * kebanyakan relasi one-to-many di repo ini, <b>tidak disimpan sebagai baris tabel relasional
 * biasa</b> yang dapat dikueri langsung lewat {@code Restrictions.eq("kelompokPkl", this)},
 * melainkan lewat <b>pola berkas JSON per kelompok</b> (lihat bagian di bawah). Setelah
 * ditempatkan, agenda pertemuan lapangan dan penilaian akhir per mahasiswa dikelola lewat entity
 * {@code MahasiswaDapatKelompokPkl} itu sendiri (di luar paket ini), sedangkan bobot &amp; daftar
 * komponen penilaian yang dipakai diambil dari {@link PklPunyaKomponenPenilaianPkl} milik
 * {@link #getPkl()} kelompok ini.</p>
 *
 * <h3>Pola berkas-JSON untuk anggota kelompok (bukan tabel relasional)</h3>
 * <p>Alih-alih menyimpan daftar anggota sebagai baris-baris tabel yang bisa langsung dikueri lewat
 * Hibernate {@code Criteria} atas kolom foreign key, kelas ini menyimpan <b>peta id-mahasiswa
 * &rarr; lokasi-berkas-JSON-detail-anggota</b> pada SATU berkas JSON per kelompok, dikelola lewat
 * {@link #ambilLokasiDetailKelompokPkl()}/{@link #tulisLokasiDetailKelompokPkl(String)}. Setiap
 * mahasiswa yang "dapat" kelompok ini ({@link MahasiswaDapatKelompokPkl}) sebenarnya juga punya
 * berkas JSON detailnya SENDIRI (lewat {@code write()} miliknya sendiri, method
 * {@code getAbsolutePath()} dipanggil pada hasilnya), dan berkas indeks milik {@code KelompokPkl}
 * ini hanya menyimpan PETA id&rarr;path ke berkas-berkas detail tersebut — dua lapis penyimpanan
 * berbasis berkas, bukan satu tabel relasional dengan foreign key. Konsekuensinya sama seperti yang
 * dijelaskan pada javadoc kembaran {@link ais.database.model.kkn.KelompokKkn} (bagian "Pola
 * berkas-JSON"): penulisan bukan transaksional, pembacaan lebih mahal secara I/O dibanding SQL JOIN,
 * dan {@link #reInitMahasiswaDapatKelompokPkl(Session)} adalah satu-satunya jalur yang benar-benar
 * mengueri tabel {@code MahasiswaDapatKelompokPkl} langsung.</p>
 *
 * <h3>Parsing JSON — TIDAK ada pengaman legacy (divergensi dari kembaran KKN)</h3>
 * <p><b>Catatan konsistensi lintas modul (temuan audit dokumentasi ini):</b> kembaran kelas ini,
 * {@link ais.database.model.kkn.KelompokKkn}, memiliki method privat
 * {@code amanJadikanJSONObject(String)} yang memvalidasi teks sebelum di-parse sebagai JSON
 * (menangkal {@code JSONException} dari data legacy/rusak — string kosong, angka, atau format
 * non-JSON lain). Kelas {@code KelompokPkl} ini <b>TIDAK memiliki pembungkus pengaman setara</b>:
 * kelima method paralelnya ({@link #ambilLokasiDetailKelompokPkl()} tidak langsung, tapi
 * {@link #removeMahasiswaDapatKelompokPkl(Serializable)},
 * {@link #populateMahasiswaDapatKelompokPkl(MahasiswaDapatKelompokPkl)}, kedua overload
 * {@link #ambilMahasiswaDapatKelompokPkl}, dan {@link #ambilJumlahDetailperkuliahanLangsung()})
 * memanggil {@code new JSONObject(...)} secara LANGSUNG. Ini divergensi robustness yang genuinely
 * baru ditemukan selama audit dokumentasi ini (bukan bagian dari bug SKS/IPK atau kode mati
 * {@code reload...} yang sudah tercatat sebelumnya di memori proyek), dan sudah dilaporkan lewat
 * task terpisah untuk ditelaah/ditambal pada sesi lain — bukan bagian dari perubahan dokumentasi
 * ini. Lihat javadoc masing-masing method di bawah untuk rincian try/catch yang membungkusnya
 * (sebagian ADA perlindungan parsial lewat try/catch generik di sekitarnya, tapi TIDAK ada satu
 * titik terpusat yang menjamin "data legacy/rusak = dianggap kosong" seperti pola KKN).</p>
 *
 * <h3>Sepuluh slot dosen pembimbing, lima slot wewenang penilaian</h3>
 * <p>Kelompok ini punya sepuluh slot dosen pembimbing ({@link #getDosen_pembimbing1()} s.d.
 * {@link #getDosen_pembimbing10()}), tapi katalog {@link KomponenPenilaianPkl} hanya punya lima
 * flag wewenang penilaian granular ({@code dosen1}..{@code dosen5}) — lihat javadoc
 * {@code KomponenPenilaianPkl} untuk penjelasan kesenjangan 10-vs-5 ini.</p>
 *
 * <h3>Perluasan siswa sekolah (fitur PKL-spesifik, BUKAN divergensi salin-tempel)</h3>
 * <p>Berbeda dari {@code KelompokKkn}, kelas ini punya field TAMBAHAN {@link #getSekolah()} dan
 * {@link #getKerjasamaAntarInstansi()}, serta method {@link #untukSiswa()} dan
 * {@link #ambilSiswaDapatKelompokPkl(Session)} — perluasan yang DISENGAJA untuk mendukung PKL bagi
 * peserta didik SEKOLAH (bukan hanya mahasiswa), lihat javadoc masing-masing method/field untuk
 * detail. Ini BUKAN kesalahan salin-tempel yang luput disalin ke KKN; KKN memang tidak
 * membutuhkan konsep ini karena tidak melayani peserta didik sekolah.</p>
 *
 * <h3>Kembaran modul KKN</h3>
 * <p>Selain perluasan siswa sekolah dan divergensi parsing JSON di atas, struktur kelas ini nyaris
 * identik dengan {@link ais.database.model.kkn.KelompokKkn}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kelompok_pkl")
public class KelompokPkl extends VOPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 2413821571548439808L;
	/** Primary key baris kelompok ini. */
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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama tampilan pengubah terakhir. Nilai {@code null} atau blank diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama tampilan pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** Pengguna yang sedang mengunci/menyunting baris ini (mekanisme cegah edit-bentrok); {@code null} bila tidak sedang dikunci siapa pun. */
	private Tbmuser dikunci;

	/**
	 * @return pengguna ({@link Tbmuser}) yang sedang mengunci baris ini untuk pengeditan, atau
	 *         {@code null} bila tidak sedang dikunci. Referensi dicek lewat {@code check(dikunci)}
	 *         sebelum dikembalikan (proxy Hibernate basi diganti entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/** @param dikunci pengguna yang mengunci baris ini untuk pengeditan; {@code null} untuk membuka kunci. */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
	 * {@code UPDATE} dikirim ke basis data, memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu saat ini pada konstruksi objek. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; biasanya diset otomatis oleh
	 *                        {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini; diperbarui otomatis oleh
	 *         {@link #onUpdate()} setiap kali baris diperbarui di basis data.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@code id + "-" + nama_kelompok} — representasi teks ringkas kelompok ini, mis. "12-Kelompok PT Maju Jaya". */
	public String toString() {
		return id + "-" + nama_kelompok;
	}

	/** Cache in-memory nama tampilan kelompok; selalu ditimpa ulang dari {@link #getNama_kelompok()} setiap kali {@link #getNama()} dipanggil. */
	private String nama;

	/**
	 * @return nama tampilan kelompok ini. Method ini <b>bukan getter pasif</b>: setiap kali
	 *         dipanggil, ia MENIMPA field {@link #nama} dengan nilai {@link #getNama_kelompok()}
	 *         saat ini lalu mengembalikannya — sehingga {@link #setNama(String)} yang dipanggil
	 *         sebelumnya menjadi tidak berpengaruh permanen; nilai yang benar-benar dipakai untuk
	 *         menampilkan nama kelompok adalah {@link #nama_kelompok}, bukan {@link #nama}.
	 */
	public String getNama() {
		nama = getNama_kelompok();
		return nama;
	}

	/**
	 * @param nama nama tampilan sementara; disimpan ke field {@link #nama}, tapi akan DITIMPA
	 *             kembali oleh {@link #getNama()} pada pemanggilan berikutnya — lihat javadoc
	 *             {@link #getNama()} untuk penjelasan lengkap kenapa setter ini tidak
	 *             "menempel" secara permanen.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Nama kelompok yang sesungguhnya disimpan/dipakai (mis. "Kelompok PT Maju Jaya"); wajib diisi secara praktik meski tidak dipetakan {@code nullable=false} secara eksplisit di kelas ini. */
	private String nama_kelompok;
	/** Tanggal mulai pelaksanaan PKL kelompok ini. Diinisialisasi ke tanggal saat ini pada konstruksi objek (bukan {@code null}), lalu biasanya ditimpa oleh operator saat mengisi jadwal. */
	private Date tanggal_mulai = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal selesai pelaksanaan PKL kelompok ini. Diinisialisasi ke tanggal saat ini pada konstruksi objek, sama seperti {@link #tanggal_mulai}. */
	private Date tanggal_selesai = ais.ui.util.WaktuUtil.getDate();
	/** Program PKL induk kelompok ini. */
	private Pkl pkl;
	/** Alamat lokasi/instansi penempatan kelompok; boleh {@code null}. */
	private String alamat;

	/** Catatan/keterangan bebas untuk kelompok ini; boleh {@code null}. */
	private String keterangan;
	/** Dosen pembimbing slot ke-1. */
	private Dosen dosen_pembimbing1;
	/** Dosen pembimbing slot ke-2. */
	private Dosen dosen_pembimbing2;
	/** Dosen pembimbing slot ke-3. */
	private Dosen dosen_pembimbing3;
	/** Dosen pembimbing slot ke-4. */
	private Dosen dosen_pembimbing4;
	/** Dosen pembimbing slot ke-5. */
	private Dosen dosen_pembimbing5;
	/** Dosen pembimbing slot ke-6 (di luar jangkauan flag wewenang penilaian {@code dosen1}..{@code dosen5} milik {@link KomponenPenilaianPkl} — lihat javadoc kelas). */
	private Dosen dosen_pembimbing6;
	/** Dosen pembimbing slot ke-7. */
	private Dosen dosen_pembimbing7;
	/** Dosen pembimbing slot ke-8. */
	private Dosen dosen_pembimbing8;
	/** Dosen pembimbing slot ke-9. */
	private Dosen dosen_pembimbing9;
	/** Dosen pembimbing slot ke-10. */
	private Dosen dosen_pembimbing10;

	/** Menandai apakah mahasiswa boleh memilih sendiri kelompok ini (alih-alih ditempatkan panitia). Default {@code false} bila belum diisi. */
	private Boolean mahasiswaBisaMemilih;
	/** Kuota maksimum anggota kelompok ini. Default {@code 30} bila belum diisi. */
	private Integer kuota;
	/** Jenis/frekuensi pelaksanaan kelompok ini (mis. "Mingguan"). Default {@code "Mingguan"} bila belum diisi. */
	private String jenis;
	/** Menandai apakah penjadwalan kelompok ini boleh melewati tanggal merah/hari libur nasional. Default {@code true} bila belum diisi. */
	private Boolean lewatiTanggalMerahNasional;
	/** Sertifikat yang diterbitkan untuk kelompok ini (bila ada); boleh {@code null}. */
	private Sertifikat sertifikat;
	/** Kerja sama antar instansi yang menaungi penempatan kelompok ini (mis. MoU dengan perusahaan/lembaga mitra); boleh {@code null}. Field ini TIDAK ADA pada kembaran {@link ais.database.model.kkn.KelompokKkn} — perluasan PKL-spesifik. */
	private KerjasamaAntarInstansi kerjasamaAntarInstansi;

	/** Tanggal Surat Keputusan (SK) penempatan kelompok ini; boleh {@code null}. */
	private Date tglSk;
	/** Nomor Surat Keputusan (SK) penempatan kelompok ini; boleh {@code null}. */
	private String noSk;

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public KelompokPkl() {
	}

	/**
	 * @return primary key baris kelompok ini, di-generate basis data ({@code IDENTITY});
	 *         {@code null} sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris kelompok ini. Kolom dipetakan {@code insertable = false}
	 *           sehingga pengisian di sini tidak berpengaruh pada {@code INSERT}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @param nama_kelompok nama kelompok yang sesungguhnya disimpan/dipakai (mis. "Kelompok PT Maju Jaya"). */
	public void setNama_kelompok(String nama_kelompok) {
		this.nama_kelompok = nama_kelompok;
	}

	/** @return nama kelompok yang sesungguhnya disimpan/dipakai, apa adanya tanpa trimming/normalisasi. */
	public String getNama_kelompok() {
		return nama_kelompok;
	}

	/** @param tanggal_mulai tanggal mulai pelaksanaan PKL kelompok ini. */
	public void setTanggal_mulai(Date tanggal_mulai) {
		this.tanggal_mulai = tanggal_mulai;
	}

	/** @return tanggal mulai pelaksanaan PKL kelompok ini. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_mulai() {
		return tanggal_mulai;
	}

	/** @param tanggal_selesai tanggal selesai pelaksanaan PKL kelompok ini. */
	public void setTanggal_selesai(Date tanggal_selesai) {
		this.tanggal_selesai = tanggal_selesai;
	}

	/** @return tanggal selesai pelaksanaan PKL kelompok ini. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_selesai() {
		return tanggal_selesai;
	}

	/** @param alamat alamat lokasi/instansi penempatan kelompok. */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/** @return alamat lokasi/instansi penempatan kelompok, atau {@code null} bila belum diisi. */
	public String getAlamat() {
		return alamat;
	}

	/** @param keterangan catatan/keterangan bebas untuk kelompok ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return catatan/keterangan bebas kelompok ini, apa adanya tanpa normalisasi. */
	public String getKeterangan() {
		return keterangan;
	}

	/** @param dosen_pembimbing1 dosen pembimbing slot ke-1. */
	public void setDosen_pembimbing1(Dosen dosen_pembimbing1) {
		this.dosen_pembimbing1 = dosen_pembimbing1;
	}

	/** @return dosen pembimbing slot ke-1, dicek lewat {@code check(...)} sebelum dikembalikan (proxy Hibernate basi diganti entity segar bila perlu), atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing1", nullable = true)
	public Dosen getDosen_pembimbing1() {
		dosen_pembimbing1 = check(dosen_pembimbing1);
		return dosen_pembimbing1;
	}

	/** @param dosen_pembimbing2 dosen pembimbing slot ke-2. */
	public void setDosen_pembimbing2(Dosen dosen_pembimbing2) {
		this.dosen_pembimbing2 = dosen_pembimbing2;
	}

	/** @return dosen pembimbing slot ke-2, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing2", nullable = true)
	public Dosen getDosen_pembimbing2() {
		dosen_pembimbing2 = check(dosen_pembimbing2);
		return dosen_pembimbing2;
	}

	/** @return program {@link Pkl} induk kelompok ini, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila belum diisi (berbeda dari beberapa relasi "Punya" sepaket, kolom ini {@code nullable = true} di sini). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pkl", nullable = true)
	public Pkl getPkl() {
		pkl = check(pkl);
		return pkl;
	}

	/** @param pkl program PKL induk kelompok ini. */
	public void setPkl(Pkl pkl) {
		this.pkl = pkl;
	}

	/** @return dosen pembimbing slot ke-3, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing3", nullable = true)
	public Dosen getDosen_pembimbing3() {
		dosen_pembimbing3 = check(dosen_pembimbing3);
		return dosen_pembimbing3;
	}

	/** @param dosen_pembimbing3 dosen pembimbing slot ke-3. */
	public void setDosen_pembimbing3(Dosen dosen_pembimbing3) {
		this.dosen_pembimbing3 = dosen_pembimbing3;
	}

	/** @return dosen pembimbing slot ke-4, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing4", nullable = true)
	public Dosen getDosen_pembimbing4() {
		dosen_pembimbing4 = check(dosen_pembimbing4);
		return dosen_pembimbing4;
	}

	/** @param dosen_pembimbing4 dosen pembimbing slot ke-4. */
	public void setDosen_pembimbing4(Dosen dosen_pembimbing4) {
		this.dosen_pembimbing4 = dosen_pembimbing4;
	}

	/** @return dosen pembimbing slot ke-5, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing5", nullable = true)
	public Dosen getDosen_pembimbing5() {
		dosen_pembimbing5 = check(dosen_pembimbing5);
		return dosen_pembimbing5;
	}

	/** @param dosen_pembimbing5 dosen pembimbing slot ke-5. */
	public void setDosen_pembimbing5(Dosen dosen_pembimbing5) {
		this.dosen_pembimbing5 = dosen_pembimbing5;
	}

	/** @return {@code true} bila mahasiswa boleh memilih sendiri kelompok ini; default {@code false} bila field {@link #mahasiswaBisaMemilih} belum pernah diisi (getter ini TIDAK menuliskan default ke field, berbeda dari pola beberapa getter lain di kelas ini). */
	public Boolean getMahasiswaBisaMemilih() {
		return mahasiswaBisaMemilih == null ? false : mahasiswaBisaMemilih;
	}

	/** @param mahasiswaBisaMemilih {@code true} agar mahasiswa boleh memilih sendiri kelompok ini. */
	public void setMahasiswaBisaMemilih(Boolean mahasiswaBisaMemilih) {
		this.mahasiswaBisaMemilih = mahasiswaBisaMemilih;
	}

	/** @return kuota maksimum anggota kelompok ini; default {@code 30} bila field {@link #kuota} belum pernah diisi. */
	public Integer getKuota() {
		return kuota == null ? 30 : kuota;
	}

	/** @param kuota kuota maksimum anggota kelompok ini. */
	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

	/** @return jenis/frekuensi pelaksanaan kelompok ini; default {@code "Mingguan"} bila field {@link #jenis} belum pernah diisi. */
	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	/** @param jenis jenis/frekuensi pelaksanaan kelompok ini (mis. "Mingguan"). */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/** @return {@code true} bila penjadwalan kelompok ini boleh melewati tanggal merah/hari libur nasional; default {@code true} bila field {@link #lewatiTanggalMerahNasional} belum pernah diisi (fail-open). */
	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	/** @param lewatiTanggalMerahNasional {@code true} agar penjadwalan kelompok ini boleh melewati tanggal merah/hari libur nasional. */
	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	public String ambilLokasiDetailKelompokPkl() {

		File file = Common.getFileLocation(this, "detail_kelompokPkl_" + this.getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:319");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiDetailKelompokPkl(String data) {
		File file = Common.getFileLocation(this, "detail_kelompokPkl_" + this.getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:328");
			// TODO Auto-generated catch block

		}
	}

	public void removeMahasiswaDapatKelompokPkl(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailKelompokPkl());
			c.put(id.toString(), "");
			tulisLokasiDetailKelompokPkl(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:339");

		}
	}

	public void populateMahasiswaDapatKelompokPkl(MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl) {
		try {
			if (mahasiswaDapatKelompokPkl == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiDetailKelompokPkl());
			c.put(mahasiswaDapatKelompokPkl.getId().toString(), mahasiswaDapatKelompokPkl.write().getAbsolutePath());
			tulisLokasiDetailKelompokPkl(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:353");
		}
	}

	public Collection<MahasiswaDapatKelompokPkl> ambilMahasiswaDapatKelompokPkl(boolean refresh) {
		return ambilMahasiswaDapatKelompokPkl(null, null, null, refresh);
	}

	public void reInitMahasiswaDapatKelompokPkl(Collection<MahasiswaDapatKelompokPkl> detailperkuliahans) {
		tulisLokasiDetailKelompokPkl(new JSONObject().toString());
		for (MahasiswaDapatKelompokPkl detailperkuliahan : detailperkuliahans) {
			populateMahasiswaDapatKelompokPkl(detailperkuliahan);
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitMahasiswaDapatKelompokPkl(Session session) {
		List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = session
				.createCriteria(MahasiswaDapatKelompokPkl.class).add(Restrictions.eq("kelompokPkl", this))
				.addOrder(Order.asc("id")).list();
		tulisLokasiDetailKelompokPkl(new JSONObject().toString());
		for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
			populateMahasiswaDapatKelompokPkl(mahasiswaDapatKelompokPkl);
		}
		mahasiswaDapatKelompokPkls = null;
	}

	@SuppressWarnings("unchecked")
	public Collection<MahasiswaDapatKelompokPkl> ambilMahasiswaDapatKelompokPkl(String nim, String nama,
			String hanyaNama, boolean refresh) {
		try {
			if (!udah("dapat_kelompok") || refresh) {
				Session session = HibernateUtil.currentNativeSession();
				reInitMahasiswaDapatKelompokPkl(session);
				HibernateUtil.closeSession();
			}

			List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPklsTemp = new ArrayList<MahasiswaDapatKelompokPkl>();
			try {
				JSONObject c = new JSONObject(ambilLokasiDetailKelompokPkl());
				Iterator<String> keys = c.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					try {
						String s = c.getString(key);
						if (!s.trim().isEmpty()) {
							MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl = (MahasiswaDapatKelompokPkl) Common
									.convertToObject(new JSONObject(ais.common.BacaTulisUtil.baca(new File(s))),
											MahasiswaDapatKelompokPkl.class);
							if (mahasiswaDapatKelompokPkl != null) {
								mahasiswaDapatKelompokPkl.setKelompokPkl(this);
								mahasiswaDapatKelompokPklsTemp.add(mahasiswaDapatKelompokPkl);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:407");

					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:411");

			}

			Map<Long, MahasiswaDapatKelompokPkl> maps = new HashMap<Long, MahasiswaDapatKelompokPkl>();

			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPklsTemp) {
				if (mahasiswaDapatKelompokPkl != null && mahasiswaDapatKelompokPkl.getMahasiswa() != null
						&& mahasiswaDapatKelompokPkl.getMahasiswa().getId() != null) {

					if (hanyaNama != null && !hanyaNama.trim().isEmpty()) {
						if ((mahasiswaDapatKelompokPkl.getMahasiswa().getNim() != null && mahasiswaDapatKelompokPkl
								.getMahasiswa().getNim().toLowerCase().contains(hanyaNama.toLowerCase()))
								|| (mahasiswaDapatKelompokPkl.getMahasiswa().getNama() != null
										&& mahasiswaDapatKelompokPkl.getMahasiswa().getNama().toLowerCase()
												.contains(hanyaNama.toLowerCase()))) {
							maps.put(mahasiswaDapatKelompokPkl.getMahasiswa().getId(), mahasiswaDapatKelompokPkl);
						}
					}

					else if ((nim == null || nim.trim().isEmpty()
							|| (mahasiswaDapatKelompokPkl.getMahasiswa().getNim() != null && mahasiswaDapatKelompokPkl
									.getMahasiswa().getNim().toLowerCase().contains(nim.toLowerCase())))

							&& (nama == null || nama.trim().isEmpty()
									|| (mahasiswaDapatKelompokPkl.getMahasiswa().getNama() != null
											&& mahasiswaDapatKelompokPkl.getMahasiswa().getNama().toLowerCase()
													.contains(nama.toLowerCase())))) {

						maps.put(mahasiswaDapatKelompokPkl.getMahasiswa().getId(), mahasiswaDapatKelompokPkl);
					}
				}
			}

			mahasiswaDapatKelompokPklsTemp = null;
			return maps.values();
		} catch (Exception e) {
			return new ArrayList<MahasiswaDapatKelompokPkl>();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		int jumlah = 0;
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailKelompokPkl());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						jumlah++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:466");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/pkl/KelompokPkl.java:470");

		}

		return jumlah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	private String course;
	private String fileLocation;
	private String feeder;
	private Lokasi lokasi;
	private Double jarak;
	private Boolean urutkanotomatis;
	private Boolean aktif;

	// Scope SEKOLAH: bila terisi, kelompok PKL ini adalah PKL untuk SISWA pada sekolah tsb
	// (anggotanya SiswaDapatKelompokPkl). Bila null, kelompok PKL berlaku untuk MAHASISWA seperti
	// semula — sehingga engine PKL yang sama dipakai ulang tanpa menggandakan model.
	private ais.database.model.sekolah.Sekolah sekolah;

	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	@javax.persistence.Transient
	public String getOrCreateFileLocation() {
		if (fileLocation == null || !fileLocation.endsWith(getId() + ".json")
				|| java.nio.file.Files.notExists(java.nio.file.Paths.get(fileLocation))) {
			write();
		}
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public String getNoSk() {
		return noSk;
	}

	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}

	@Column(columnDefinition = "text")
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing6", nullable = true)
	public Dosen getDosen_pembimbing6() {
		dosen_pembimbing6 = check(dosen_pembimbing6);
		return dosen_pembimbing6;
	}

	public void setDosen_pembimbing6(Dosen dosen_pembimbing6) {
		this.dosen_pembimbing6 = dosen_pembimbing6;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing7", nullable = true)
	public Dosen getDosen_pembimbing7() {
		dosen_pembimbing7 = check(dosen_pembimbing7);
		return dosen_pembimbing7;
	}

	public void setDosen_pembimbing7(Dosen dosen_pembimbing7) {
		this.dosen_pembimbing7 = dosen_pembimbing7;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing8", nullable = true)
	public Dosen getDosen_pembimbing8() {
		dosen_pembimbing8 = check(dosen_pembimbing8);
		return dosen_pembimbing8;
	}

	public void setDosen_pembimbing8(Dosen dosen_pembimbing8) {
		this.dosen_pembimbing8 = dosen_pembimbing8;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing9", nullable = true)
	public Dosen getDosen_pembimbing9() {
		dosen_pembimbing9 = check(dosen_pembimbing9);
		return dosen_pembimbing9;
	}

	public void setDosen_pembimbing9(Dosen dosen_pembimbing9) {
		this.dosen_pembimbing9 = dosen_pembimbing9;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing10", nullable = true)
	public Dosen getDosen_pembimbing10() {
		dosen_pembimbing10 = check(dosen_pembimbing10);
		return dosen_pembimbing10;
	}

	public void setDosen_pembimbing10(Dosen dosen_pembimbing10) {
		this.dosen_pembimbing10 = dosen_pembimbing10;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kerjasama_antar_instansi", nullable = true)
	public KerjasamaAntarInstansi getKerjasamaAntarInstansi() {
		kerjasamaAntarInstansi = check(kerjasamaAntarInstansi);
		return kerjasamaAntarInstansi;
	}

	public void setKerjasamaAntarInstansi(KerjasamaAntarInstansi kerjasamaAntarInstansi) {
		this.kerjasamaAntarInstansi = kerjasamaAntarInstansi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	public Double getJarak() {
		return jarak == null ? 1.0 : jarak;
	}

	public void setJarak(Double jarak) {
		this.jarak = jarak;
	}

	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Sekolah pemilik kelompok PKL ini bila PKL diperuntukkan bagi SISWA. Bernilai {@code null}
	 * untuk PKL mahasiswa (perilaku lama tidak berubah). Dipakai untuk menyaring kelompok PKL per
	 * sekolah pada halaman "PKL Siswa" dan menandai bahwa anggotanya bertipe {@code SiswaDapatKelompokPkl}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public ais.database.model.sekolah.Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	public void setSekolah(ais.database.model.sekolah.Sekolah sekolah) {
		this.sekolah = sekolah;
	}

	/** {@code true} bila kelompok PKL ini diperuntukkan bagi siswa (punya scope sekolah). */
	public boolean untukSiswa() {
		return getSekolah() != null;
	}

	/**
	 * Mengambil seluruh anggota SISWA pada kelompok PKL ini secara langsung lewat kueri (bukan
	 * pola berkas-JSON yang dipakai anggota mahasiswa), diurutkan berdasarkan id. Bila {@code session}
	 * {@code null}, dipakai {@code HibernateUtil.currentSession()} (sesi thread-request yang dikelola
	 * kerangka kerja dan tidak ditutup di sini). Selalu mengembalikan daftar (tidak pernah
	 * {@code null}) sehingga aman langsung diiterasi.
	 *
	 * @param session sesi Hibernate aktif, atau {@code null} untuk memakai currentSession
	 * @return daftar {@link ais.database.model.SiswaDapatKelompokPkl} pada kelompok ini
	 */
	@SuppressWarnings("unchecked")
	public java.util.List<ais.database.model.SiswaDapatKelompokPkl> ambilSiswaDapatKelompokPkl(Session session) {
		try {
			if (this.getId() == null) {
				return new java.util.ArrayList<ais.database.model.SiswaDapatKelompokPkl>();
			}
			if (session == null) {
				session = HibernateUtil.currentSession();
			}
			return session.createCriteria(ais.database.model.SiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("kelompokPkl", this)).addOrder(Order.asc("id")).list();
		} catch (Exception e) {
			return new java.util.ArrayList<ais.database.model.SiswaDapatKelompokPkl>();
		}
	}
}
