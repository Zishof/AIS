package ais.database.model.kkn;

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
import ais.database.model.Kkn;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Sertifikat;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.VOPembelajaran;
import ais.database.model.asset.Lokasi;

/**
 * Entity <b>kelompok/posko KKN</b> — unit penempatan lapangan mahasiswa peserta KKN — pada tabel
 * {@code public.kelompok_kkn}. Satu baris mewakili SATU kelompok: lokasi penempatan, rentang
 * tanggal pelaksanaan, kuota anggota, hingga sepuluh slot dosen pembimbing
 * ({@link #getDosen_pembimbing1()} s.d. {@link #getDosen_pembimbing10()}). Kelas ini adalah
 * turunan {@link ais.database.model.VOPembelajaran} ("Value Object Pembelajaran") — pola yang
 * dipakai bersama modul akademik lain (skripsi, PKL, magang, dsb) untuk merepresentasikan "wadah
 * pembelajaran" yang punya anggota, penilaian, dan agenda/pertemuan.
 *
 * <h3>Kedudukan dalam alur KKN</h3>
 * <p>Satu {@link Kkn} (gelaran/periode KKN) dipecah menjadi banyak {@code KelompokKkn}: mahasiswa
 * yang lolos seleksi pendaftaran (lihat {@link MahasiswaDaftarKkn}) ditempatkan ke satu kelompok,
 * direkam sebagai {@link MahasiswaDapatKelompokKkn} — relasi keanggotaan yang, TIDAK SEPERTI
 * kebanyakan relasi one-to-many di repo ini, <b>tidak disimpan sebagai baris tabel relasional
 * biasa</b> yang dapat dikueri langsung lewat {@code Restrictions.eq("kelompokKkn", this)},
 * melainkan lewat <b>pola berkas JSON per kelompok</b> (lihat bagian di bawah). Setelah
 * ditempatkan, agenda pertemuan lapangan dan penilaian akhir per mahasiswa dikelola lewat entity
 * {@code MahasiswaDapatKelompokKkn} itu sendiri (di luar paket ini), sedangkan bobot &amp; daftar
 * komponen penilaian yang dipakai diambil dari {@link KknPunyaKomponenPenilaianKkn} milik
 * {@link #getKkn()} kelompok ini.</p>
 *
 * <h3>Pola berkas-JSON untuk anggota kelompok (bukan tabel relasional)</h3>
 * <p>Alih-alih menyimpan daftar anggota sebagai baris-baris tabel yang bisa langsung dikueri lewat
 * Hibernate {@code Criteria} atas kolom foreign key, kelas ini menyimpan <b>peta id-mahasiswa
 * &rarr; lokasi-berkas-JSON-detail-anggota</b> pada SATU berkas JSON per kelompok, dikelola lewat
 * {@link #ambilLokasiDetailKelompokKkn()}/{@link #tulisLokasiDetailKelompokKkn(String)}. Setiap
 * mahasiswa yang "dapat" kelompok ini ({@link MahasiswaDapatKelompokKkn}) sebenarnya juga punya
 * berkas JSON detailnya SENDIRI (lewat {@code write()} miliknya sendiri, method
 * {@code getAbsolutePath()} dipanggil pada hasilnya), dan berkas indeks milik {@code KelompokKkn}
 * ini hanya menyimpan PETA id&rarr;path ke berkas-berkas detail tersebut — dua lapis penyimpanan
 * berbasis berkas, bukan satu tabel relasional dengan foreign key. Konsekuensinya:
 * <ul>
 * <li>Menambah/menghapus anggota kelompok = menulis ulang satu berkas JSON indeks (operasi
 * <b>bukan transaksional</b> terhadap basis data — race condition antar dua request bersamaan yang
 * memodifikasi kelompok yang sama berpotensi saling menimpa perubahan satu sama lain, karena
 * seluruh peta ditulis ulang dari awal, bukan di-patch per entri).</li>
 * <li>Membaca anggota memerlukan MEMBACA BERKAS (bukan kueri SQL sederhana), lalu untuk setiap
 * entri memuat berkas detail terpisah dan mem-parsing-nya lagi menjadi objek
 * {@link MahasiswaDapatKelompokKkn} lewat {@link Common#convertToObject(JSONObject, Class)} — jauh
 * lebih mahal secara I/O dibanding satu kueri SQL dengan JOIN, tapi dipilih kemungkinan karena pola
 * ini sudah lama dipakai di seluruh repo untuk entity serupa (lihat {@code VOMahasiswa}) sebelum
 * era ORM penuh.</li>
 * <li>{@link #reInitMahasiswaDapatKelompokKkn(Session)} adalah SATU-SATUNYA jalur yang benar-benar
 * mengueri tabel {@code MahasiswaDapatKelompokKkn} langsung lewat Hibernate {@code Criteria}
 * (dengan {@code Restrictions.eq("kelompokKkn", this)}) untuk MEMBANGUN ULANG berkas indeks JSON
 * dari basis data — dipakai sebagai mekanisme "refresh dari sumber kebenaran" bila berkas indeks
 * dicurigai basi/tidak sinkron (lihat parameter {@code refresh} pada
 * {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)}).</li>
 * </ul></p>
 *
 * <h3>Parsing JSON yang tahan data legacy/rusak</h3>
 * <p>Seluruh method di kelas ini yang membaca berkas indeks JSON memakai
 * {@link #amanJadikanJSONObject(String)} (bukan {@code new JSONObject(String)} langsung) — lihat
 * javadoc method tersebut untuk rincian kenapa ini perlu. <b>Catatan konsistensi lintas modul:</b>
 * kembaran kelas ini, {@link ais.database.model.pkl.KelompokPkl}, TIDAK memiliki pembungkus
 * pengaman setara dan memanggil {@code new JSONObject(...)} langsung di lima method paralelnya —
 * lihat javadoc kelas {@code KelompokPkl} untuk rincian divergensi ini (ditemukan &amp; dilaporkan
 * lewat task terpisah, TIDAK ditambal sebagai bagian dari sesi dokumentasi ini).</p>
 *
 * <h3>Sepuluh slot dosen pembimbing, lima slot wewenang penilaian</h3>
 * <p>Kelompok ini punya sepuluh slot dosen pembimbing ({@link #getDosen_pembimbing1()} s.d.
 * {@link #getDosen_pembimbing10()}), tapi katalog {@link KomponenPenilaianKkn} hanya punya lima
 * flag wewenang penilaian granular ({@code dosen1}..{@code dosen5}) — lihat javadoc
 * {@code KomponenPenilaianKkn} untuk penjelasan kesenjangan 10-vs-5 ini.</p>
 *
 * <h3>Kembaran modul PKL</h3>
 * <p>Struktur kelas ini nyaris identik dengan {@link ais.database.model.pkl.KelompokPkl} (kelas
 * PKL bahkan punya field TAMBAHAN {@code sekolah}/{@code kerjasamaAntarInstansi} untuk mendukung
 * jalur peserta SISWA sekolah — perluasan yang disengaja, bukan penyimpangan salin-tempel), KECUALI
 * satu divergensi robustness nyata pada parsing JSON yang dijelaskan di atas.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kelompok_kkn")
public class KelompokKkn extends VOPembelajaran {

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

	/** @return {@code id + "-" + nama_kelompok} — representasi teks ringkas kelompok ini, mis. "12-Kelompok Desa Sukamaju". */
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

	/** Nama kelompok yang sesungguhnya disimpan/dipakai (mis. "Kelompok Desa Sukamaju"); wajib diisi secara praktik meski tidak dipetakan {@code nullable=false} secara eksplisit di kelas ini. */
	private String nama_kelompok;
	/** Tanggal mulai pelaksanaan KKN kelompok ini. Diinisialisasi ke tanggal saat ini pada konstruksi objek (bukan {@code null}), lalu biasanya ditimpa oleh operator saat mengisi jadwal. */
	private Date tanggal_mulai = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal selesai pelaksanaan KKN kelompok ini. Diinisialisasi ke tanggal saat ini pada konstruksi objek, sama seperti {@link #tanggal_mulai}. */
	private Date tanggal_selesai = ais.ui.util.WaktuUtil.getDate();
	/** Gelaran KKN induk kelompok ini. */
	private Kkn kkn;
	/** Alamat lokasi penempatan kelompok; boleh {@code null}. */
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
	/** Dosen pembimbing slot ke-6 (di luar jangkauan flag wewenang penilaian {@code dosen1}..{@code dosen5} milik {@link KomponenPenilaianKkn} — lihat javadoc kelas). */
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
	/** Nomor Surat Keputusan (SK) penempatan kelompok ini; boleh {@code null}. */
	private String noSk;
	/** Tanggal Surat Keputusan (SK) penempatan kelompok ini; boleh {@code null}. */
	private Date tglSk;

	/** Data feeder/integrasi sistem eksternal (teks bebas, biasanya JSON), dipetakan sebagai kolom {@code text}; boleh {@code null}/kosong. */
	private String feeder;

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public KelompokKkn() {
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

	/** @param nama_kelompok nama kelompok yang sesungguhnya disimpan/dipakai (mis. "Kelompok Desa Sukamaju"). */
	public void setNama_kelompok(String nama_kelompok) {
		this.nama_kelompok = nama_kelompok;
	}

	/** @return nama kelompok yang sesungguhnya disimpan/dipakai, apa adanya tanpa trimming/normalisasi. */
	public String getNama_kelompok() {
		return nama_kelompok;
	}

	/** @param tanggal_mulai tanggal mulai pelaksanaan KKN kelompok ini. */
	public void setTanggal_mulai(Date tanggal_mulai) {
		this.tanggal_mulai = tanggal_mulai;
	}

	/** @return tanggal mulai pelaksanaan KKN kelompok ini. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_mulai() {
		return tanggal_mulai;
	}

	/** @param tanggal_selesai tanggal selesai pelaksanaan KKN kelompok ini. */
	public void setTanggal_selesai(Date tanggal_selesai) {
		this.tanggal_selesai = tanggal_selesai;
	}

	/** @return tanggal selesai pelaksanaan KKN kelompok ini. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_selesai() {
		return tanggal_selesai;
	}

	/** @param alamat alamat lokasi penempatan kelompok. */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/** @return alamat lokasi penempatan kelompok, atau {@code null} bila belum diisi. */
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

	/** @return gelaran {@link Kkn} induk kelompok ini, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila belum diisi (berbeda dari beberapa relasi "Punya" sepaket, kolom ini {@code nullable = true} di sini). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kkn", nullable = true)
	public Kkn getKkn() {
		kkn = check(kkn);
		return kkn;
	}

	/** @param kkn gelaran KKN induk kelompok ini. */
	public void setKkn(Kkn kkn) {
		this.kkn = kkn;
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

	/**
	 * Parsing JSON yang aman terhadap data legacy/rusak. Kolom penyimpanan lokasi
	 * detail KKN semestinya selalu berisi teks JSON (mis. "{...}"), tapi data lama
	 * bisa saja berisi string kosong, angka, atau nilai non-JSON lain sehingga
	 * {@link JSONObject#JSONObject(String)} melempar {@code JSONException}
	 * ("A JSONObject text must begin with '{'"). Validasi dulu SEBELUM parse:
	 * kalau bukan JSON objek yang valid, anggap saja "tidak ada data" (JSONObject
	 * kosong) alih-alih membiarkan exception menjalar ke pemanggil (mis. layar
	 * detail aktifitas KKN dosen).
	 *
	 * <p><b>Detail alur validasi (untuk pembaca yang memelihara method ini):</b> tiga
	 * kondisi diperiksa berurutan sebelum benar-benar mencoba parse: (1) {@code data == null}
	 * &rarr; langsung kembalikan objek kosong tanpa menyentuh {@code trim()} (menghindari
	 * {@code NullPointerException}); (2) setelah di-{@code trim()}, string kosong &rarr; juga
	 * dianggap "tidak ada data"; (3) karakter pertama string yang sudah di-trim BUKAN {@code '{'}
	 * &rarr; ditolak lebih awal SEBELUM mencoba parse sama sekali — ini menangkap kasus paling
	 * umum data legacy: angka mentah, string bebas, atau JSON array ({@code "[...]"}, yang juga
	 * ditolak karena karakter pertamanya {@code '['} bukan {@code '{'}). Baru setelah lolos ketiga
	 * pemeriksaan itu, method mencoba {@code new JSONObject(trimmed)} di dalam blok
	 * {@code try}/{@code catch} sebagai <b>jaring pengaman lapis kedua</b> — menangkap kasus yang
	 * lolos pemeriksaan format dasar (diawali {@code '{'}) tapi tetap bukan JSON valid (mis.
	 * {@code "{tidak valid"} tanpa penutup, atau sintaks JSON yang rusak di tengah). Pada lapis
	 * kedua ini, exception-nya DIREKAM lewat {@link ais.common.ErrorAuditUtil#record(Throwable, String)}
	 * (bukan dibuang diam-diam total) sehingga tetap ada jejak audit bahwa ada data yang gagal
	 * di-parse, walau perilaku fungsionalnya sama: mengembalikan {@link JSONObject} kosong ke
	 * pemanggil, bukan melempar ulang.</p>
	 *
	 * <p><b>Kenapa ini penting secara khusus untuk kelas ini:</b> {@link #ambilLokasiDetailKelompokKkn()}
	 * membaca isi berkas indeks JSON dari disk lewat {@link ais.common.BacaTulisUtil#baca(File)},
	 * dan berkas itu bisa saja hilang isinya (truncated akibat proses tulis yang terputus,
	 * mis. server mati mendadak di tengah {@link #tulisLokasiDetailKelompokKkn(String)}), berisi
	 * whitespace sisa migrasi lama, atau — pada instalasi yang sudah lama berjalan sejak sebelum
	 * era ORM penuh — memuat format penyimpanan lama yang tidak lagi kompatibel. Tanpa method
	 * pembungkus ini, SETIAP pemanggilan {@code new JSONObject(...)} langsung atas data semacam itu
	 * akan melempar {@code JSONException} yang tidak tertangani ke lapisan UI (mis. layar detail
	 * aktifitas KKN dosen di {@code ais.action.master.helper.AktifitasKknHelper}), membuat seluruh
	 * halaman gagal dimuat hanya karena SATU baris indeks lama yang rusak — padahal kelompok KKN
	 * bersangkutan sepenuhnya valid dan seharusnya tetap bisa diakses (hanya kehilangan daftar
	 * anggota lama yang gagal dibaca).</p>
	 *
	 * <p><b>Divergensi lintas modul (dicatat sebagai temuan terpisah, TIDAK ditambal di sini):</b>
	 * kembaran kelas ini, {@link ais.database.model.pkl.KelompokPkl}, TIDAK memiliki method
	 * pembungkus setara — kelima method paralelnya memanggil {@code new JSONObject(...)} secara
	 * LANGSUNG tanpa validasi format maupun jaring pengaman ini. Ini divergensi robustness yang
	 * genuinely baru ditemukan selama audit dokumentasi ini (bukan bagian dari bug SKS/IPK atau
	 * kode mati {@code reload...} yang sudah tercatat sebelumnya di memori proyek), dan sudah
	 * dilaporkan lewat task terpisah untuk ditambal pada sesi lain — bukan bagian dari perubahan
	 * dokumentasi ini.</p>
	 *
	 * @param data teks mentah yang diharapkan berupa JSON objek; boleh {@code null}, kosong, atau
	 *             format apa pun (tidak divalidasi tipenya oleh pemanggil).
	 * @return {@link JSONObject} hasil parse {@code data}, atau {@link JSONObject} KOSONG (bukan
	 *         {@code null}) bila {@code data} bukan JSON objek yang valid dengan alasan apa pun.
	 *         Method ini TIDAK PERNAH melempar exception ke pemanggil.
	 */
	private static JSONObject amanJadikanJSONObject(String data) {
		if (data == null) {
			return new JSONObject();
		}
		String trimmed = data.trim();
		if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
			return new JSONObject();
		}
		try {
			return new JSONObject(trimmed);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(JSON legacy tidak valid, dilewati) src/ais/database/model/kkn/KelompokKkn.java:amanJadikanJSONObject");
			return new JSONObject();
		}
	}

	/**
	 * Membaca isi berkas indeks JSON anggota kelompok ini dari disk — berkas yang memetakan
	 * id-mahasiswa (dari {@link MahasiswaDapatKelompokKkn#getId()}) ke path absolut berkas detail
	 * anggota tersebut (lihat javadoc kelas, bagian "Pola berkas-JSON untuk anggota kelompok").
	 * Lokasi berkas ditentukan lewat {@link Common#getFileLocation(Object, String)} dengan nama
	 * kunci {@code "detail_kelompokKkn_" + getId()} — sehingga setiap kelompok (dibedakan oleh id
	 * primary key-nya) punya SATU berkas indeks unik miliknya sendiri di lokasi penyimpanan yang
	 * dikelola {@code Common}.
	 *
	 * <p>Method ini AMAN dipanggil bahkan bila berkas belum pernah ada (kelompok baru yang belum
	 * pernah punya anggota): baik kegagalan I/O (berkas tidak ditemukan, masalah permission, dsb —
	 * ditangkap lewat blok {@code try}/{@code catch} yang membungkus SELURUH badan method, termasuk
	 * baris {@code Common.getFileLocation(...)} itu sendiri) maupun isi berkas yang kosong/blank
	 * akan menghasilkan hasil yang SAMA: {@link VOMahasiswa#dataJSON} (representasi JSON kosong
	 * standar yang dipakai bersama seluruh entity turunan {@code VOPembelajaran}/{@code VOMahasiswa}
	 * di repo ini), bukan {@code null} atau exception. Perhatikan bahwa exception yang tertangkap
	 * di sini HANYA direkam lewat {@link ais.common.ErrorAuditUtil#record(Throwable, String)} —
	 * blok catch ini adalah salah satu dari beberapa "empty-catch" auto-audit di kelas ini yang
	 * menandakan penanganan error minimal/pasif, bukan penanganan yang membedakan jenis kegagalan.</p>
	 *
	 * @return isi berkas indeks JSON (teks mentah, belum di-parse) bila berkas ada dan tidak kosong;
	 *         {@link VOMahasiswa#dataJSON} pada seluruh kasus lain (berkas tidak ada, gagal dibaca,
	 *         atau isinya kosong/blank).
	 */
	public String ambilLokasiDetailKelompokKkn() {

		File file = Common.getFileLocation(this, "detail_kelompokKkn_" + this.getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:317");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menulis ulang SELURUH isi berkas indeks JSON anggota kelompok ini ke disk, di lokasi yang
	 * sama seperti yang dibaca {@link #ambilLokasiDetailKelompokKkn()} (kunci
	 * {@code "detail_kelompokKkn_" + getId()} lewat {@link Common#getFileLocation(Object, String)}).
	 * Ini adalah operasi TIMPA PENUH (bukan patch/append) — pemanggil bertanggung jawab membangun
	 * ulang seluruh peta id&rarr;path yang ingin dipertahankan sebelum memanggil method ini (lihat
	 * pemanggil-pemanggilnya: {@link #removeMahasiswaDapatKelompokKkn(Serializable)} dan
	 * {@link #populateMahasiswaDapatKelompokKkn(MahasiswaDapatKelompokKkn)} keduanya membaca dulu
	 * peta lengkap lewat {@link #ambilLokasiDetailKelompokKkn()}, memodifikasi SATU entri, baru
	 * menulis ulang seluruh peta hasil modifikasi).
	 *
	 * <p>Kegagalan I/O (mis. disk penuh, permission ditolak) DITELAN diam-diam — hanya direkam
	 * lewat {@link ais.common.ErrorAuditUtil#record(Throwable, String)}, TIDAK dilempar ulang ke
	 * pemanggil. Konsekuensinya: kode pemanggil yang memanggil method ini TIDAK PUNYA cara untuk
	 * tahu bahwa penulisan gagal — operasi "hapus anggota" atau "tambah anggota" akan TAMPAK
	 * berhasil dari sudut pandang pemanggil (tidak ada exception yang perlu ditangani), padahal
	 * perubahan sebenarnya tidak tersimpan ke disk. Ini konsisten dengan pola "empty-catch"
	 * auto-audit yang berulang di seluruh kelas ini, bukan kasus terisolasi.</p>
	 *
	 * @param data teks JSON lengkap (hasil {@code JSONObject.toString()}) yang menggantikan
	 *             SELURUH isi berkas indeks sebelumnya.
	 */
	public void tulisLokasiDetailKelompokKkn(String data) {
		File file = Common.getFileLocation(this, "detail_kelompokKkn_" + this.getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:326");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus satu entri anggota dari berkas indeks JSON kelompok ini, TANPA menghapus berkas
	 * detail anggota itu sendiri dari disk. Secara mekanis: baca peta lengkap lewat
	 * {@link #ambilLokasiDetailKelompokKkn()} (dibungkus {@link #amanJadikanJSONObject(String)}),
	 * timpa nilai entri berkunci {@code id.toString()} menjadi string kosong {@code ""} (BUKAN
	 * menghapus kunci itu dari peta — lihat catatan di bawah), lalu tulis ulang seluruh peta lewat
	 * {@link #tulisLokasiDetailKelompokKkn(String)}.
	 *
	 * <p><b>Catatan penting soal semantik "hapus":</b> method {@code org.json.JSONObject} yang
	 * dipakai di sini adalah {@code put(key, "")} — MENGISI kunci dengan string kosong, bukan
	 * {@code remove(key)} yang benar-benar membuang kunci dari peta. Konsekuensinya, entri untuk
	 * {@code id} tersebut TETAP ADA di berkas JSON (dengan nilai kosong) alih-alih hilang
	 * seluruhnya. Ini konsisten dengan cara {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)}
	 * membaca peta: ia secara eksplisit melewati (skip) entri dengan {@code s.trim().isEmpty()}
	 * (lihat javadoc method tersebut) — sehingga entri "kosong" ini secara efektif diperlakukan
	 * sebagai "tidak ada", meski secara harfiah kuncinya tidak dibuang dari JSON. Berkas detail
	 * milik anggota yang dihapus (di path yang SEBELUMNYA tersimpan pada entri ini) juga TIDAK
	 * dihapus oleh method ini — hanya referensinya dari indeks kelompok yang dikosongkan, bukan
	 * berkas fisiknya (potensi berkas yatim/orphan yang menumpuk seiring waktu bila anggota sering
	 * dihapus-tambah).</p>
	 *
	 * <p>Kegagalan apa pun (baik saat membaca, memodifikasi, maupun menulis ulang) ditangkap oleh
	 * satu blok {@code try}/{@code catch} yang membungkus seluruh badan method dan hanya direkam
	 * lewat audit, konsisten dengan pola "empty-catch" di seluruh kelas ini.</p>
	 *
	 * @param id id mahasiswa ({@code MahasiswaDapatKelompokKkn.getId()}) yang entrinya hendak
	 *           dikosongkan dari indeks; dikonversi ke {@code String} lewat {@code id.toString()}
	 *           sebagai kunci JSON, sehingga TIDAK BOLEH {@code null} (akan melempar
	 *           {@code NullPointerException} yang tertangkap oleh blok {@code catch} method ini
	 *           sendiri, bukan diteruskan ke pemanggil).
	 */
	public void removeMahasiswaDapatKelompokKkn(Serializable id) {
		try {
			JSONObject c = amanJadikanJSONObject(ambilLokasiDetailKelompokKkn());
			c.put(id.toString(), "");
			tulisLokasiDetailKelompokKkn(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:337");

		}
	}

	/**
	 * Mendaftarkan/memperbarui SATU entri anggota pada berkas indeks JSON kelompok ini: menulis
	 * path absolut berkas detail milik {@code mahasiswaDapatKelompokKkn} (didapat lewat
	 * {@code mahasiswaDapatKelompokKkn.write().getAbsolutePath()} — method {@code write()} pada
	 * {@link MahasiswaDapatKelompokKkn} sendiri yang bertanggung jawab menyimpan berkas detailnya
	 * ke disk dan mengembalikan lokasinya) ke dalam peta indeks, berkunci id mahasiswa tersebut.
	 * Ini adalah kebalikan operasional dari {@link #removeMahasiswaDapatKelompokKkn(Serializable)}:
	 * bila method itu mengosongkan entri, method ini MENGISI/MEMPERBARUI entri dengan path berkas
	 * detail yang valid.
	 *
	 * <p>Alur: baca peta lengkap lewat {@link #ambilLokasiDetailKelompokKkn()} (dibungkus
	 * {@link #amanJadikanJSONObject(String)}), timpa/tambahkan entri berkunci
	 * {@code mahasiswaDapatKelompokKkn.getId().toString()}, tulis ulang seluruh peta lewat
	 * {@link #tulisLokasiDetailKelompokKkn(String)}. Bila {@code mahasiswaDapatKelompokKkn} bernilai
	 * {@code null}, method ini early-return TANPA melakukan apa pun (tidak mengubah berkas indeks
	 * sama sekali) — bukan melempar {@code NullPointerException}. Kegagalan lain (mis.
	 * {@code getId()} mengembalikan {@code null} sehingga {@code .toString()} melempar exception,
	 * atau kegagalan I/O saat {@code write()}/menulis indeks) ditangkap oleh satu blok
	 * {@code try}/{@code catch} yang membungkus seluruh badan method dan hanya direkam lewat audit
	 * — konsisten dengan pola "empty-catch" di seluruh kelas ini; pemanggil TIDAK diberi tahu bila
	 * pendaftaran anggota ini sebenarnya gagal.</p>
	 *
	 * @param mahasiswaDapatKelompokKkn entity keanggotaan yang hendak didaftarkan/diperbarui pada
	 *                                  indeks kelompok ini; bila {@code null}, method ini tidak
	 *                                  melakukan apa pun.
	 */
	public void populateMahasiswaDapatKelompokKkn(MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn) {
		try {
			if (mahasiswaDapatKelompokKkn == null) {
				return;
			}

			JSONObject c = amanJadikanJSONObject(ambilLokasiDetailKelompokKkn());
			c.put(mahasiswaDapatKelompokKkn.getId().toString(), mahasiswaDapatKelompokKkn.write().getAbsolutePath());
			tulisLokasiDetailKelompokKkn(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:351");
		}
	}

	/**
	 * @param refresh {@code true} untuk memaksa {@link #reInitMahasiswaDapatKelompokKkn(Session)}
	 *                membangun ulang indeks JSON dari basis data sebelum membaca anggota
	 *                (lihat javadoc {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)}
	 *                untuk penjelasan lengkap parameter ini); {@code false} untuk memakai berkas
	 *                indeks yang sudah ada apa adanya (kecuali flag {@code "dapat_kelompok"} belum
	 *                pernah di-set — lihat method tersebut).
	 * @return seluruh anggota kelompok ini tanpa filter nama/NIM — delegasi langsung ke
	 *         {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)} dengan
	 *         {@code nim}, {@code nama}, {@code hanyaNama} semuanya {@code null}.
	 */
	public Collection<MahasiswaDapatKelompokKkn> ambilMahasiswaDapatKelompokKkn(boolean refresh) {
		return ambilMahasiswaDapatKelompokKkn(null, null, null, refresh);
	}

	/**
	 * Membangun ulang berkas indeks JSON kelompok ini dari KOLEKSI anggota yang SUDAH DIMILIKI
	 * pemanggil di memori (BUKAN dari basis data — bandingkan dengan overload
	 * {@link #reInitMahasiswaDapatKelompokKkn(Session)} yang mengueri basis data terlebih dahulu).
	 * Mengosongkan seluruh berkas indeks (menulis {@code new JSONObject().toString()}, yakni
	 * {@code "{}"}) lalu memanggil {@link #populateMahasiswaDapatKelompokKkn(MahasiswaDapatKelompokKkn)}
	 * satu per satu untuk setiap elemen {@code detailperkuliahans} — sehingga hasil akhirnya adalah
	 * indeks yang PERSIS mencerminkan koleksi yang diberikan, tidak digabung dengan indeks lama.
	 *
	 * <p>Nama parameter {@code detailperkuliahans} adalah sisa penamaan dari pola salin-tempel
	 * lintas modul akademik (istilah "detail perkuliahan" umum dipakai di entity
	 * {@code VOPembelajaran} turunan lain seperti perkuliahan reguler) — TIDAK mencerminkan
	 * terminologi KKN yang sesungguhnya (semestinya "anggota kelompok KKN"), tapi tidak diubah di
	 * sini agar tidak menyimpang dari kode sumber asli yang didokumentasikan.</p>
	 *
	 * @param detailperkuliahans koleksi anggota yang akan MENGGANTIKAN seluruh isi indeks kelompok
	 *                           ini; iterasi memakai for-each biasa sehingga {@code null} pada
	 *                           parameter ini akan melempar {@code NullPointerException} (TIDAK
	 *                           ditangkap oleh method ini sendiri, berbeda dari kebanyakan method
	 *                           lain di kelas ini yang membungkus badan method dengan
	 *                           {@code try}/{@code catch}).
	 */
	public void reInitMahasiswaDapatKelompokKkn(Collection<MahasiswaDapatKelompokKkn> detailperkuliahans) {
		tulisLokasiDetailKelompokKkn(new JSONObject().toString());
		for (MahasiswaDapatKelompokKkn detailperkuliahan : detailperkuliahans) {
			populateMahasiswaDapatKelompokKkn(detailperkuliahan);
		}
	}

	/**
	 * Membangun ulang berkas indeks JSON kelompok ini langsung dari BASIS DATA lewat Hibernate
	 * {@code Criteria} — SATU-SATUNYA method di kelas ini yang benar-benar mengueri tabel
	 * {@code MahasiswaDapatKelompokKkn} lewat {@code Restrictions.eq("kelompokKkn", this)}, bukan
	 * membaca peta id&rarr;path dari berkas JSON yang sudah ada. Dipakai sebagai mekanisme
	 * "refresh dari sumber kebenaran" ketika berkas indeks JSON dicurigai basi/tidak sinkron
	 * dengan basis data (lihat pemanggilnya di
	 * {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)}, dipicu oleh flag
	 * {@code refresh} atau ketika flag udah("dapat_kelompok") belum pernah di-set).
	 *
	 * <p>Alur: (1) kueri seluruh {@link MahasiswaDapatKelompokKkn} yang menunjuk kelompok ini,
	 * diurutkan menaik berdasarkan {@code id}; (2) kosongkan berkas indeks (tulis {@code "{}"});
	 * (3) untuk setiap hasil kueri, panggil
	 * {@link #populateMahasiswaDapatKelompokKkn(MahasiswaDapatKelompokKkn)} — yang, perhatikan,
	 * MEMBACA ULANG dan MENULIS ULANG SELURUH berkas indeks pada SETIAP iterasi (bukan
	 * dikumpulkan dulu baru ditulis sekali) — sehingga kompleksitas I/O method ini adalah O(n) kali
	 * baca-tulis berkas untuk n anggota, bukan satu kali tulis batch. Untuk kelompok dengan banyak
	 * anggota, ini berarti banyak operasi baca/tulis berkas kecil berturut-turut; (4) variabel lokal
	 * {@code mahasiswaDapatKelompokKkns} secara eksplisit di-set {@code null} di akhir method
	 * (pola pembersihan referensi manual yang umum di kelas-kelas lama repo ini, kemungkinan
	 * peninggalan kebiasaan mengelola memori era JVM lama — secara praktik tidak diperlukan karena
	 * variabel lokal otomatis keluar dari scope setelah method selesai).</p>
	 *
	 * <p>Method ini TIDAK membungkus badannya dengan {@code try}/{@code catch} sendiri — berbeda
	 * dari kebanyakan method lain di kelas ini. Bila query Hibernate gagal (mis. sesi sudah
	 * tertutup, masalah koneksi basis data), exception akan MENJALAR ke pemanggil. Pemanggil
	 * satu-satunya di kelas ini, {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)},
	 * MEMBUNGKUS pemanggilan ini di dalam blok {@code try} miliknya sendiri sehingga exception
	 * tetap tertangani di lapisan atas, bukan menjalar ke UI.</p>
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk mengueri
	 *                {@link MahasiswaDapatKelompokKkn}; pemanggil bertanggung jawab atas siklus
	 *                hidup sesi ini (method ini tidak menutupnya).
	 */
	@SuppressWarnings("unchecked")
	public void reInitMahasiswaDapatKelompokKkn(Session session) {
		List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = session
				.createCriteria(MahasiswaDapatKelompokKkn.class).add(Restrictions.eq("kelompokKkn", this))
				.addOrder(Order.asc("id")).list();
		tulisLokasiDetailKelompokKkn(new JSONObject().toString());
		for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
			populateMahasiswaDapatKelompokKkn(mahasiswaDapatKelompokKkn);
		}
		mahasiswaDapatKelompokKkns = null;
	}

	/**
	 * Method inti pengambilan anggota kelompok ini, dengan filter opsional berdasarkan NIM/nama,
	 * dan kontrol eksplisit apakah indeks JSON harus dibangun ulang dari basis data terlebih
	 * dahulu. Ini adalah method paling substansial di kelas ini dan menjadi titik konvergensi
	 * seluruh mekanisme yang dijelaskan pada javadoc kelas (pola berkas-JSON, parsing aman lewat
	 * {@link #amanJadikanJSONObject(String)}, dan refresh-dari-basis-data lewat
	 * {@link #reInitMahasiswaDapatKelompokKkn(Session)}).
	 *
	 * <p><b>Tahap 1 — keputusan refresh:</b> bila {@code !udah("dapat_kelompok")} (flag penanda
	 * dari {@code VOPembelajaran}/framework yang menandakan proses ini "belum pernah dilakukan"
	 * untuk instance objek ini pada request/siklus saat ini) ATAU parameter {@code refresh} bernilai
	 * {@code true}, method membuka sesi Hibernate baru lewat
	 * {@link HibernateUtil#currentNativeSession()}, memanggil
	 * {@link #reInitMahasiswaDapatKelompokKkn(Session)} untuk membangun ulang berkas indeks JSON
	 * langsung dari basis data, lalu SEGERA menutup sesi lewat {@link HibernateUtil#closeSession()}.
	 * Ini berarti: pemanggilan PERTAMA ke method ini untuk suatu instance {@code KelompokKkn}
	 * (ketika flag {@code "dapat_kelompok"} belum pernah di-set) SELALU memicu kueri basis data
	 * penuh, terlepas dari nilai {@code refresh} — parameter {@code refresh} hanya relevan untuk
	 * memaksa refresh pada pemanggilan BERIKUTNYA setelah yang pertama.</p>
	 *
	 * <p><b>Tahap 2 — baca &amp; parse indeks JSON:</b> baca peta id&rarr;path lewat
	 * {@link #ambilLokasiDetailKelompokKkn()} (sudah pasti berisi data terbaru bila Tahap 1
	 * memicu refresh), parse lewat {@link #amanJadikanJSONObject(String)}, lalu ITERASI setiap
	 * kunci (id mahasiswa) dalam peta: entri dengan nilai kosong/blank (lihat catatan di
	 * {@link #removeMahasiswaDapatKelompokKkn(Serializable)} soal semantik "hapus" yang sebenarnya
	 * mengosongkan, bukan membuang kunci) DILEWATI (skip); entri lain dibaca berkas detailnya lewat
	 * {@code ais.common.BacaTulisUtil.baca(new File(s))}, di-parse (dibungkus
	 * {@link #amanJadikanJSONObject(String)} JUGA — perlindungan berlapis yang sama diterapkan
	 * pada berkas DETAIL, bukan hanya berkas indeks), lalu dikonversi menjadi objek
	 * {@link MahasiswaDapatKelompokKkn} lewat {@link Common#convertToObject(JSONObject, Class)}.
	 * Setiap kegagalan PER-ENTRI (mis. satu berkas detail hilang/rusak) ditangkap oleh blok
	 * {@code try}/{@code catch} INDIVIDUAL di dalam loop — sehingga SATU entri yang gagal TIDAK
	 * menggagalkan pembacaan entri-entri lain, konsisten dengan filosofi keseluruhan kelas ini:
	 * kegagalan parsial tidak boleh menjatuhkan seluruh operasi.</p>
	 *
	 * <p><b>Tahap 3 — deduplikasi &amp; filter:</b> hasil Tahap 2 (list sementara, boleh memuat
	 * duplikat bila ada anomali data) dikonversi menjadi {@link Map} berkunci
	 * {@code mahasiswa.getId()} — efek sampingnya adalah DEDUPLIKASI otomatis per mahasiswa (bila
	 * ada dua entri berbeda yang somehow merujuk mahasiswa yang sama, hanya satu yang bertahan,
	 * yakni yang terakhir diproses). Filter diterapkan saat memasukkan ke map, dengan PRIORITAS:
	 * bila {@code hanyaNama} diisi (tidak null/blank), filter HANYA memakai {@code hanyaNama}
	 * dicocokkan (case-insensitive, {@code contains}) terhadap NIM ATAU nama mahasiswa —
	 * mengabaikan parameter {@code nim}/{@code nama} sepenuhnya pada cabang ini. Bila
	 * {@code hanyaNama} kosong/null, barulah {@code nim} dan {@code nama} dipakai sebagai filter
	 * gabungan (AND): keduanya kosong/null berarti tidak ada filter (semua lolos untuk kriteria
	 * itu), sedangkan yang terisi harus cocok (case-insensitive, {@code contains}) pada field
	 * masing-masing.</p>
	 *
	 * <p><b>Penanganan error keseluruhan:</b> SELURUH badan method (tahap 1-3) dibungkus SATU blok
	 * {@code try}/{@code catch} terluar — bila terjadi kegagalan yang TIDAK tertangkap oleh
	 * try/catch internal (mis. Tahap 1 gagal membuka sesi Hibernate), method mengembalikan
	 * {@code ArrayList} KOSONG, BUKAN melempar exception maupun mengembalikan {@code null}. Ini
	 * membuat method ini aman dipanggil dari lapisan UI tanpa perlu try/catch tambahan di sisi
	 * pemanggil, dengan konsekuensi: kegagalan besar (mis. basis data tidak terjangkau) akan
	 * TAMPAK sebagai "kelompok tanpa anggota" ke pengguna, bukan pesan error yang eksplisit.</p>
	 *
	 * @param nim NIM (case-insensitive, dicocokkan sebagian/{@code contains}) untuk memfilter
	 *            anggota; diabaikan bila {@code hanyaNama} diisi, dan diabaikan (tidak memfilter)
	 *            bila {@code null}/blank.
	 * @param nama nama mahasiswa (case-insensitive, {@code contains}) untuk memfilter anggota;
	 *             sama seperti {@code nim}, diabaikan bila {@code hanyaNama} diisi.
	 * @param hanyaNama bila diisi (tidak null/blank), MENGGANTIKAN filter {@code nim}/{@code nama}
	 *                  sepenuhnya: anggota lolos bila {@code hanyaNama} cocok (case-insensitive,
	 *                  {@code contains}) pada NIM ATAU nama mahasiswa.
	 * @param refresh {@code true} untuk memaksa membangun ulang indeks JSON dari basis data
	 *                sebelum membaca anggota, meski flag {@code udah("dapat_kelompok")} sudah
	 *                pernah di-set sebelumnya (lihat penjelasan Tahap 1).
	 * @return koleksi anggota kelompok ini yang lolos filter (deduplikasi per mahasiswa); TIDAK
	 *         PERNAH {@code null} — {@code ArrayList} kosong pada kegagalan apa pun.
	 */
	@SuppressWarnings("unchecked")
	public Collection<MahasiswaDapatKelompokKkn> ambilMahasiswaDapatKelompokKkn(String nim, String nama,
			String hanyaNama, boolean refresh) {

		try {
			if (!udah("dapat_kelompok") || refresh) {
				Session session = HibernateUtil.currentNativeSession();
				reInitMahasiswaDapatKelompokKkn(session);
				HibernateUtil.closeSession();
			}

			List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKknsTemp = new ArrayList<MahasiswaDapatKelompokKkn>();
			try {
				JSONObject c = amanJadikanJSONObject(ambilLokasiDetailKelompokKkn());
				Iterator<String> keys = c.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					try {
						String s = c.getString(key);
						if (!s.trim().isEmpty()) {
							MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn = (MahasiswaDapatKelompokKkn) Common
									.convertToObject(amanJadikanJSONObject(ais.common.BacaTulisUtil.baca(new File(s))),
											MahasiswaDapatKelompokKkn.class);
							if (mahasiswaDapatKelompokKkn != null) {
								mahasiswaDapatKelompokKkn.setKelompokKkn(this);
								mahasiswaDapatKelompokKknsTemp.add(mahasiswaDapatKelompokKkn);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:406");

					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:410");

			}

			Map<Long, MahasiswaDapatKelompokKkn> maps = new HashMap<Long, MahasiswaDapatKelompokKkn>();

			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKknsTemp) {
				if (mahasiswaDapatKelompokKkn != null && mahasiswaDapatKelompokKkn.getMahasiswa() != null
						&& mahasiswaDapatKelompokKkn.getMahasiswa().getId() != null) {

					if (hanyaNama != null && !hanyaNama.trim().isEmpty()) {
						if ((mahasiswaDapatKelompokKkn.getMahasiswa().getNim() != null && mahasiswaDapatKelompokKkn
								.getMahasiswa().getNim().toLowerCase().contains(hanyaNama.toLowerCase()))
								|| (mahasiswaDapatKelompokKkn.getMahasiswa().getNama() != null && mahasiswaDapatKelompokKkn
										.getMahasiswa().getNama().toLowerCase().contains(hanyaNama.toLowerCase()))) {
							maps.put(mahasiswaDapatKelompokKkn.getMahasiswa().getId(), mahasiswaDapatKelompokKkn);
						}
					}

					else if ((nim == null || nim.trim().isEmpty()
							|| (mahasiswaDapatKelompokKkn.getMahasiswa().getNim() != null && mahasiswaDapatKelompokKkn
									.getMahasiswa().getNim().toLowerCase().contains(nim.toLowerCase())))

							&& (nama == null || nama.trim().isEmpty()
									|| (mahasiswaDapatKelompokKkn.getMahasiswa().getNama() != null
											&& mahasiswaDapatKelompokKkn.getMahasiswa().getNama().toLowerCase()
													.contains(nama.toLowerCase())))) {

						maps.put(mahasiswaDapatKelompokKkn.getMahasiswa().getId(), mahasiswaDapatKelompokKkn);
					}
				}
			}

			mahasiswaDapatKelompokKknsTemp = null;
			return maps.values();
		}catch (Exception e) {
			return new ArrayList<MahasiswaDapatKelompokKkn>();
		}
	}

	/**
	 * Menghitung jumlah anggota kelompok ini langsung dari berkas indeks JSON, TANPA memuat/
	 * mem-parsing berkas detail masing-masing anggota (berbeda dari
	 * {@link #ambilMahasiswaDapatKelompokKkn(String, String, String, boolean)} yang memuat detail
	 * penuh setiap anggota) — sehingga jauh lebih murah untuk sekadar menampilkan "jumlah anggota"
	 * pada daftar kelompok tanpa perlu detailnya. Mengimplementasikan method abstrak
	 * {@code ambilJumlahDetailperkuliahanLangsung()} yang dideklarasikan pada superclass
	 * {@link ais.database.model.VOPembelajaran} — nama method ini juga sisa penamaan generik lintas
	 * modul akademik (lihat catatan serupa pada javadoc
	 * {@link #reInitMahasiswaDapatKelompokKkn(Collection)}).
	 *
	 * <p>Alur: parse berkas indeks lewat {@link #amanJadikanJSONObject(String)}, lalu iterasi
	 * setiap kunci dan tambahkan {@code jumlah} HANYA bila nilainya tidak kosong/blank — konsisten
	 * dengan semantik "entri kosong = dianggap terhapus" yang dijelaskan pada javadoc
	 * {@link #removeMahasiswaDapatKelompokKkn(Serializable)}. Kegagalan membaca nilai suatu kunci
	 * (blok {@code try}/{@code catch} dalam loop) hanya membuat entri tersebut TIDAK dihitung
	 * (tidak menaikkan {@code jumlah}), bukan menggagalkan seluruh perhitungan; kegagalan yang lebih
	 * luas (mis. gagal membaca berkas indeks sama sekali, blok {@code try}/{@code catch} terluar)
	 * membuat method ini mengembalikan {@code jumlah} apa adanya pada titik kegagalan (biasanya
	 * {@code 0} bila kegagalan terjadi sebelum loop sempat berjalan) — TIDAK PERNAH melempar
	 * exception ke pemanggil.</p>
	 *
	 * @return jumlah entri ber-nilai tidak kosong pada berkas indeks JSON kelompok ini; {@code 0}
	 *         bila berkas tidak ada/kosong/gagal dibaca.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		int jumlah = 0;
		try {
			JSONObject c = amanJadikanJSONObject(ambilLokasiDetailKelompokKkn());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						jumlah++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:464");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:468");

		}

		return jumlah;
	}

	/** @return sertifikat yang diterbitkan untuk kelompok ini, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila belum ada sertifikat. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	/** @param sertifikat sertifikat yang diterbitkan untuk kelompok ini. */
	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	/** @return nomor Surat Keputusan (SK) penempatan kelompok ini, atau {@code null} bila belum diisi. */
	public String getNoSk() {
		return noSk;
	}

	/** @param noSk nomor Surat Keputusan (SK) penempatan kelompok ini. */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/** @return tanggal Surat Keputusan (SK) penempatan kelompok ini, atau {@code null} bila belum diisi. */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/** @param tglSk tanggal Surat Keputusan (SK) penempatan kelompok ini. */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}

	/** Data course/kurikulum kelompok ini dalam format JSON teks; dipetakan {@code columnDefinition = "text"}. Lihat {@link #getCourse()} untuk perilaku default. */
	private String course;
	/** Path absolut berkas JSON representasi lengkap objek ini di disk (bukan berkas indeks anggota); diisi/dipakai oleh {@link #getOrCreateFileLocation()} dan {@code write()} milik superclass. */
	private String fileLocation;
	/** Lokasi geografis ({@link Lokasi}) penempatan kelompok ini; boleh {@code null}. */
	private Lokasi lokasi;
	/** Jarak (satuan tidak dinormalisasi secara eksplisit di kelas ini, mengikuti konvensi pemanggil) lokasi kelompok dari suatu titik acuan. Default {@code 1.0} bila belum diisi. */
	private Double jarak;
	/** Menandai apakah anggota kelompok diurutkan otomatis oleh sistem. Default {@code true} bila belum diisi. */
	private Boolean urutkanotomatis;
	/** Menandai apakah kelompok ini masih berlaku/ditampilkan. Default {@code true} bila belum diisi. */
	private Boolean aktif;

	/**
	 * @return data course/kurikulum kelompok ini sebagai teks JSON; bila field {@link #course}
	 *         belum pernah diisi atau kosong/blank, method ini mengembalikan representasi JSON
	 *         objek KOSONG ({@code new JSONObject().toString()}, yakni {@code "{}"}) — bukan
	 *         {@code null} — sehingga pemanggil yang langsung mem-parsing hasilnya sebagai JSON
	 *         tidak perlu menangani kasus {@code null} secara terpisah. Mengimplementasikan method
	 *         abstrak dari superclass {@link ais.database.model.VOPembelajaran}.
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/** @param course data course/kurikulum kelompok ini sebagai teks JSON. */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/** @return path absolut berkas JSON representasi lengkap objek ini di disk, TANPA memicu penulisan bila belum ada (getter pasif — bandingkan dengan {@link #getOrCreateFileLocation()}); {@code null} bila belum pernah ditulis. */
	public String getFileLocation() {
		return fileLocation;
	}

	/**
	 * @return path absolut berkas JSON representasi lengkap objek ini di disk, MENJAMIN berkas
	 *         tersebut ada dan valid sebelum mengembalikan path-nya. Method ini <b>bukan getter
	 *         pasif</b> (ditandai {@code @Transient} — bukan kolom basis data): ia memicu
	 *         penulisan ulang lewat {@code write()} (method superclass) bila salah satu dari tiga
	 *         kondisi berikut terpenuhi: (1) {@link #fileLocation} masih {@code null}; (2) path
	 *         yang tersimpan TIDAK diakhiri {@code getId() + ".json"} (mis. id berubah setelah
	 *         entity di-persist pertama kali, sehingga path lama sudah tidak relevan); atau (3)
	 *         berkas pada path tersebut TERNYATA TIDAK ADA di disk (mis. terhapus manual/oleh
	 *         proses lain di luar kendali aplikasi). Pemanggil yang butuh JAMINAN berkas selalu
	 *         valid (bukan sekadar membaca apa adanya) harus memakai method ini, bukan
	 *         {@link #getFileLocation()}.
	 */
	@javax.persistence.Transient
	public String getOrCreateFileLocation() {
		if (fileLocation == null || !fileLocation.endsWith(getId() + ".json")
				|| java.nio.file.Files.notExists(java.nio.file.Paths.get(fileLocation))) {
			write();
		}
		return fileLocation;
	}

	/** @param fileLocation path absolut berkas JSON representasi lengkap objek ini di disk. */
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	/** @return data feeder/integrasi sistem eksternal, di-trim; {@code null} bila field {@link #feeder} belum pernah diisi atau isinya kosong/blank setelah trim (BUKAN string kosong seperti beberapa field lain di kelas ini). */
	@Column(columnDefinition = "text")
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/** @param feeder data feeder/integrasi sistem eksternal (teks bebas, biasanya JSON). */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/** @return dosen pembimbing slot ke-6, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. Di luar jangkauan flag wewenang penilaian granular {@code dosen1}..{@code dosen5} milik {@link KomponenPenilaianKkn} — lihat javadoc kelas. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing6", nullable = true)
	public Dosen getDosen_pembimbing6() {
		dosen_pembimbing6 = check(dosen_pembimbing6);
		return dosen_pembimbing6;
	}

	/** @param dosen_pembimbing6 dosen pembimbing slot ke-6. */
	public void setDosen_pembimbing6(Dosen dosen_pembimbing6) {
		this.dosen_pembimbing6 = dosen_pembimbing6;
	}

	/** @return dosen pembimbing slot ke-7, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing7", nullable = true)
	public Dosen getDosen_pembimbing7() {
		dosen_pembimbing7 = check(dosen_pembimbing7);
		return dosen_pembimbing7;
	}

	/** @param dosen_pembimbing7 dosen pembimbing slot ke-7. */
	public void setDosen_pembimbing7(Dosen dosen_pembimbing7) {
		this.dosen_pembimbing7 = dosen_pembimbing7;
	}

	/** @return dosen pembimbing slot ke-8, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing8", nullable = true)
	public Dosen getDosen_pembimbing8() {
		dosen_pembimbing8 = check(dosen_pembimbing8);
		return dosen_pembimbing8;
	}

	/** @param dosen_pembimbing8 dosen pembimbing slot ke-8. */
	public void setDosen_pembimbing8(Dosen dosen_pembimbing8) {
		this.dosen_pembimbing8 = dosen_pembimbing8;
	}

	/** @return dosen pembimbing slot ke-9, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing9", nullable = true)
	public Dosen getDosen_pembimbing9() {
		dosen_pembimbing9 = check(dosen_pembimbing9);
		return dosen_pembimbing9;
	}

	/** @param dosen_pembimbing9 dosen pembimbing slot ke-9. */
	public void setDosen_pembimbing9(Dosen dosen_pembimbing9) {
		this.dosen_pembimbing9 = dosen_pembimbing9;
	}

	/** @return dosen pembimbing slot ke-10, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila slot belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pembimbing10", nullable = true)
	public Dosen getDosen_pembimbing10() {
		dosen_pembimbing10 = check(dosen_pembimbing10);
		return dosen_pembimbing10;
	}

	/** @param dosen_pembimbing10 dosen pembimbing slot ke-10. */
	public void setDosen_pembimbing10(Dosen dosen_pembimbing10) {
		this.dosen_pembimbing10 = dosen_pembimbing10;
	}

	/** @return lokasi geografis penempatan kelompok ini, dicek lewat {@code check(...)} sebelum dikembalikan, atau {@code null} bila belum diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/** @param lokasi lokasi geografis penempatan kelompok ini. */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/** @return jarak lokasi kelompok dari titik acuan; default {@code 1.0} bila field {@link #jarak} belum pernah diisi. */
	public Double getJarak() {
		return jarak == null ? 1.0 : jarak;
	}

	/** @param jarak jarak lokasi kelompok dari titik acuan. */
	public void setJarak(Double jarak) {
		this.jarak = jarak;
	}

	/**
	 * @return {@code true} bila anggota kelompok ini diurutkan otomatis oleh sistem; default
	 *         {@code true} bila field {@link #urutkanotomatis} belum pernah diisi (fail-open).
	 *         Mengimplementasikan method abstrak dari superclass
	 *         {@link ais.database.model.VOPembelajaran}.
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/** @param urutkanotomatis {@code true} agar anggota kelompok ini diurutkan otomatis oleh sistem. */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/** @return {@code true} bila kelompok ini masih berlaku/ditampilkan; default {@code true} bila field {@link #aktif} belum pernah diisi (fail-open). */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} agar kelompok ini tetap berlaku/ditampilkan, {@code false} untuk menonaktifkannya tanpa menghapus baris. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
