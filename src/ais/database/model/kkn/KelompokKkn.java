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

	public String ambilLokasiDetailKelompokKkn() {

		File file = Common.getFileLocation(this, "detail_kelompokKkn_" + this.getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:317");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiDetailKelompokKkn(String data) {
		File file = Common.getFileLocation(this, "detail_kelompokKkn_" + this.getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:326");
			// TODO Auto-generated catch block

		}
	}

	public void removeMahasiswaDapatKelompokKkn(Serializable id) {
		try {
			JSONObject c = amanJadikanJSONObject(ambilLokasiDetailKelompokKkn());
			c.put(id.toString(), "");
			tulisLokasiDetailKelompokKkn(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kkn/KelompokKkn.java:337");

		}
	}

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

	public Collection<MahasiswaDapatKelompokKkn> ambilMahasiswaDapatKelompokKkn(boolean refresh) {
		return ambilMahasiswaDapatKelompokKkn(null, null, null, refresh);
	}

	public void reInitMahasiswaDapatKelompokKkn(Collection<MahasiswaDapatKelompokKkn> detailperkuliahans) {
		tulisLokasiDetailKelompokKkn(new JSONObject().toString());
		for (MahasiswaDapatKelompokKkn detailperkuliahan : detailperkuliahans) {
			populateMahasiswaDapatKelompokKkn(detailperkuliahan);
		}
	}

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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
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

	private String course;
	private String fileLocation;
	private Lokasi lokasi;
	private Double jarak;
	private Boolean urutkanotomatis;
	private Boolean aktif;

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
}
