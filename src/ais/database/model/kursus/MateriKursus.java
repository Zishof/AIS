package ais.database.model.kursus;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Ujian;

/**
 * Satu item materi/lecture (baris kurikulum) di dalam sebuah {@link SeksiKursus}, yaitu satu
 * unit konten yang dipelajari peserta secara berurutan sesuai {@link #getUrutan()}. Setiap
 * materi punya {@link #getTipeKonten() tipe konten} yang menentukan bagaimana ia "diselesaikan"
 * oleh peserta -- lihat konstanta {@link #VIDEO}, {@link #ARTIKEL}, {@link #QUIZ}, {@link #TUGAS}:
 * <ul>
 * <li>{@link #VIDEO} -- progres ditandai lewat pelaporan posisi putar (heartbeat) dari pemutar
 * video di sisi klien, direkam di {@link ProgressMateriKursus#getDurasiDitonton()}/
 * {@link ProgressMateriKursus#getPersentase()}.</li>
 * <li>{@link #ARTIKEL} -- tidak ada metrik otomatis; peserta menandai sendiri sudah membaca.</li>
 * <li>{@link #QUIZ} -- materi bertaut ke satu {@link Ujian} (field {@link #getUjian()}); riwayat
 * pengerjaannya disimpan di {@link PercobaanKuisKursus} + {@link JawabanPercobaanKuisKursus},
 * BUKAN soal Quiz itu sendiri yang di-reuse dari {@link ais.database.model.BankSoal}.</li>
 * <li>{@link #TUGAS} -- peserta mengunggah berkas/tautan lewat {@link PengumpulanTugasKursus}.</li>
 * </ul>
 * <p>
 * <b>Catatan keamanan (dicek terhadap pemanggil nyata di
 * webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp):</b> entity ini sendiri (layer model)
 * tidak melakukan validasi apa pun atas isi field -- semua setter menerima nilai apa adanya.
 * Otorisasi "hanya instruktur pemilik {@link ais.database.model.kursus.ProdukKursus} boleh
 * mengubah kurikulum" ditegakkan di action aksi=simpan_materi/hapus_materi pada JSP tersebut
 * (bandingkan {@link SeksiKursus#getProdukKursus()}.getInstruktur() dengan peserta yang login),
 * bukan di sini. Perubahan pada gerbang otorisasi itu (mis. jika ada jalur pemanggil baru yang
 * lupa memverifikasi kepemilikan) TIDAK akan tertangkap oleh entity ini.
 * <p>
 * Video/lampiran fisik (mis. berkas video materi) disimpan lewat
 * {@link ais.database.model.file.LampiranLain} (ref = id materi ini, jenis = "Video Materi
 * Kursus"), BUKAN kolom di kelas ini -- pola yang sama dipakai thumbnail
 * {@link ais.database.model.kursus.ProdukKursus}. Ini berbeda dari
 * {@link PengumpulanTugasKursus}, yang menyimpan nama berkas/tautan tugas langsung sebagai
 * kolom sendiri ({@code namaFile}/{@code link}), TANPA lewat {@code LampiranLain}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "materi_kursus")
public class MateriKursus extends GeneralValueObject {

	/** Tipe konten materi berupa video yang ditonton peserta; nilai default {@link #getTipeKonten()} bila kolom kosong. */
	public final static String VIDEO = "Video";
	/** Tipe konten materi berupa artikel/bacaan teks (tanpa pelacakan progres otomatis selain tanda "selesai" manual). */
	public final static String ARTIKEL = "Artikel";
	/** Tipe konten materi berupa kuis interaktif; membutuhkan {@link #getUjian()} sebagai wadah bank soalnya. */
	public final static String QUIZ = "Quiz";
	/** Tipe konten materi berupa tugas yang harus dikumpulkan peserta lewat {@link PengumpulanTugasKursus}. */
	public final static String TUGAS = "Tugas";

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil identitas (username/id) pengguna yang terakhir membuat/mengubah baris ini.
	 * Field audit "shadow" -- diisi lewat {@link ais.database.hibernate.AuditTimestampInterceptor},
	 * bukan bagian dari data bisnis materi kursus.
	 *
	 * @return id pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe)
	 * supaya baris audit lama tidak tertimpa nilai kosong secara tidak sengaja.
	 *
	 * @param olehId id pengguna; diabaikan jika {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe),
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan jika {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate sesaat sebelum UPDATE
	 * dieksekusi, memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Method audit
	 * "shadow" -- keharusan teknis infrastruktur audit, bukan logika bisnis materi kursus.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi tanggal terakhir baris ini diubah. Biasanya diisi otomatis lewat {@link #onUpdate()},
	 * bukan dipanggil manual dari kode bisnis.
	 *
	 * @param tanggal_dirubah tanggal/waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil tanggal terakhir baris ini diubah. Default diinisialisasi ke waktu saat objek
	 * dibuat ({@link ais.ui.util.WaktuUtil#getDate()}), lalu diperbarui otomatis oleh
	 * {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return tanggal/waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug/dropdown: {@code id + "-" + nama}.
	 *
	 * @return string gabungan id dan nama materi.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private SeksiKursus seksiKursus;
	private String tipeKonten;
	private Integer durasiMenit;
	private Integer urutan;
	private Boolean preview;
	private Boolean aktif;
	private Ujian ujian;
	private Integer batasWaktuMenit;
	private Integer batasPercobaan;
	private Boolean acakSoal;
	private Boolean acakJawaban;
	private Integer jumlahSoalDitampilkan;

	/** Konstruktor default -- dibutuhkan Hibernate untuk instansiasi lewat reflection. */
	public MateriKursus() {
	}

	/**
	 * Mengambil id unik (primary key) baris materi ini, di-generate DB lewat
	 * strategi {@code IDENTITY}.
	 *
	 * @return id materi, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id materi. Dipanggil Hibernate saat memuat entity dari DB; kolom
	 * {@code insertable = false} berarti nilai INSERT selalu berasal dari sequence/identity DB,
	 * bukan dari nilai yang di-set manual di sini.
	 *
	 * @param id id materi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode materi (belum dipetakan lewat {@code @Column} eksplisit -- mengandalkan
	 * penamaan properti standar Hibernate). Nilai selalu di-trim, dan {@code null} dikembalikan
	 * sebagai string kosong supaya aman dipakai langsung di tampilan/pencarian.
	 *
	 * @return kode materi ter-trim, atau string kosong jika belum diisi.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode materi. Tidak seperti field lain, tidak ada logika auto-generate di sini
	 * (bandingkan {@code getKode()} pada {@link ProgressMateriKursus}/{@link PercobaanKuisKursus}
	 * yang auto-generate lewat {@link ais.common.BarcodeCommon#generateCode()} bila kosong).
	 *
	 * @param kode kode materi.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama/judul materi, selalu di-trim. Kolom {@code NOT NULL} di DB, tapi getter ini
	 * tetap bisa mengembalikan {@code null} untuk entity baru yang belum diisi.
	 *
	 * @return nama materi ter-trim, atau {@code null} jika belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama/judul materi. Tidak ada validasi kosong/blank di layer entity ini -- validasi
	 * "judul harus diisi" ditegakkan di pemanggil (mis. aksi simpan_materi pada
	 * {@code _kursus_service.jsp}), bukan di setter ini.
	 *
	 * @param nama nama materi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi bebas untuk materi ini (kolom {@code text}, boleh panjang).
	 *
	 * @return keterangan materi, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/deskripsi bebas. Lihat {@link #getKeterangan()} untuk detail perilaku getter.
	 *
	 * @param keterangan nilai baru untuk keterangan/deskripsi bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil {@link SeksiKursus} (section kurikulum) tempat materi ini berada.
	 * Lazy-loaded lewat {@code ManyToOne}; dibungkus {@link #check(Object)} untuk menangani
	 * proxy Hibernate yang sudah ter-detach/kosong.
	 *
	 * @return seksi induk materi ini; kolom {@code NOT NULL} di DB sehingga seharusnya tidak
	 *         pernah {@code null} untuk baris yang sudah tersimpan valid.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "seksi_kursus", nullable = false)
	public SeksiKursus getSeksiKursus() {
		seksiKursus = check(seksiKursus);
		return seksiKursus;
	}

	/**
	 * Mengisi seksi induk materi ini. Tidak ada verifikasi bahwa seksi yang di-set benar-benar
	 * milik {@link ais.database.model.kursus.ProdukKursus} yang sama dengan instruktur yang
	 * memanggil -- pengecekan kepemilikan itu tanggung jawab pemanggil (lihat catatan keamanan
	 * di Javadoc kelas).
	 *
	 * @param seksiKursus seksi induk baru.
	 */
	public void setSeksiKursus(SeksiKursus seksiKursus) {
		this.seksiKursus = seksiKursus;
	}

	/**
	 * Mengambil tipe konten materi ({@link #VIDEO}/{@link #ARTIKEL}/{@link #QUIZ}/{@link #TUGAS}).
	 * Kolom bebas nilai apa pun (bukan enum DB), sehingga nilai di luar keempat konstanta di atas
	 * tetap tersimpan apa adanya -- kode pemanggil yang mencocokkan lewat {@code equals()} dengan
	 * konstanta tersebut (bukan enum-switch) akan menganggapnya sebagai tipe tak dikenal.
	 * Default ke {@link #VIDEO} bila kolom kosong/null, BUKAN {@code null} -- supaya kode
	 * pemanggil lama yang belum mengenal tipe konten tetap memperlakukan materi sebagai video.
	 *
	 * @return tipe konten materi, tidak pernah {@code null}/kosong (fallback {@link #VIDEO}).
	 */
	@Column(name = "tipe_konten", nullable = true, length = 50)
	public String getTipeKonten() {
		return tipeKonten == null || tipeKonten.isEmpty() ? VIDEO : tipeKonten;
	}

	/**
	 * Mengisi tipe konten materi. Lihat {@link #getTipeKonten()} untuk detail perilaku getter.
	 *
	 * @param tipeKonten nilai baru untuk tipe konten materi.
	 */
	public void setTipeKonten(String tipeKonten) {
		this.tipeKonten = tipeKonten;
	}

	/**
	 * Mengambil estimasi durasi materi dalam menit (dipakai video untuk menghitung persentase
	 * tonton di {@link ProgressMateriKursus#getPersentase()}, dan kuis untuk menghitung batas
	 * atas detik posisi putar yang dipercaya dari heartbeat klien).
	 *
	 * @return durasi dalam menit, default 0 bila belum diisi (bukan {@code null}).
	 */
	public Integer getDurasiMenit() {
		return durasiMenit == null ? 0 : durasiMenit;
	}

	/**
	 * Mengisi estimasi durasi dalam menit. Lihat {@link #getDurasiMenit()} untuk detail perilaku getter.
	 *
	 * @param durasiMenit nilai baru untuk estimasi durasi dalam menit.
	 */
	public void setDurasiMenit(Integer durasiMenit) {
		this.durasiMenit = durasiMenit;
	}

	/**
	 * Mengambil urutan tampil materi ini di dalam seksinya (ascending, dipakai saat query
	 * kurikulum -- lihat pemanggil di {@code _kursus_service.jsp} yang selalu
	 * {@code addOrder(Order.asc("urutan"))}).
	 *
	 * @return urutan tampil, default 0 bila belum diisi.
	 */
	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	/**
	 * Mengisi urutan tampil. Lihat {@link #getUrutan()} untuk detail perilaku getter.
	 *
	 * @param urutan nilai baru untuk urutan tampil.
	 */
	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	/**
	 * Mengambil penanda apakah materi ini bisa diakses gratis sebagai pratinjau (preview) tanpa
	 * perlu membeli/enroll kursus terlebih dahulu.
	 *
	 * @return {@code true} jika bisa dipratinjau; default {@code false} bila belum diisi.
	 */
	public Boolean getPreview() {
		return preview == null ? false : preview;
	}

	/**
	 * Mengisi penanda bisa dipratinjau gratis. Lihat {@link #getPreview()} untuk detail perilaku getter.
	 *
	 * @param preview nilai baru untuk penanda bisa dipratinjau gratis.
	 */
	public void setPreview(Boolean preview) {
		this.preview = preview;
	}

	/**
	 * Mengambil penanda aktif/nonaktif materi ini (soft-disable, materi tidak dihapus dari DB
	 * tapi disembunyikan dari kurikulum yang ditampilkan ke peserta). Default {@code true} bila
	 * kolom belum diisi -- pola flag aktif "fail-open by default" yang umum di codebase ini:
	 * baris lama tanpa nilai eksplisit dianggap tetap aktif.
	 *
	 * @return {@code true} jika aktif; default {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi penanda aktif/nonaktif. Lihat {@link #getAktif()} untuk detail perilaku getter.
	 *
	 * @param aktif nilai baru untuk penanda aktif/nonaktif.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/*
	 * Kolom BARU pada tabel LAMA yang sudah ber-@Audited. Sesuai keputusan yang
	 * disepakati, kolom ini SENGAJA murni diserahkan ke Hibernate hbm2ddl=update
	 * (tanpa helper self-heal manual seperti KursusSchemaFix) -- konsekuensinya
	 * tabel new_audit.materi_kursus__audit TIDAK otomatis mendapat kolom ini,
	 * dan proses simpan MateriKursus BISA GAGAL sampai kolom berikut ditambah
	 * manual satu kali di database:
	 *   ALTER TABLE public.materi_kursus ADD COLUMN ujian bigint;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN batas_waktu_menit integer;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN batas_percobaan integer;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN acak_soal boolean;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN acak_jawaban boolean;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN jumlah_soal_ditampilkan integer;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN ujian bigint;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN batas_waktu_menit integer;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN batas_percobaan integer;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN acak_soal boolean;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN acak_jawaban boolean;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN jumlah_soal_ditampilkan integer;
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian", nullable = true)
	public Ujian getUjian() {
		ujian = check(ujian);
		return ujian;
	}

	/**
	 * Mengaitkan materi ini (harus bertipe {@link #QUIZ} agar bermakna) dengan sebuah
	 * {@link Ujian} yang menampung bank soalnya. Bila belum ada, dibuatkan otomatis sekali oleh
	 * pemanggil (lihat {@code ambilAtauBuatUjianMateri()} di {@code _kursus_service.jsp}) --
	 * entity ini sendiri tidak membuat {@code Ujian} baru secara implisit.
	 *
	 * @param ujian wadah bank soal untuk materi bertipe kuis ini.
	 */
	public void setUjian(Ujian ujian) {
		this.ujian = ujian;
	}

	/**
	 * Mengambil batas waktu pengerjaan kuis dalam menit. Hanya relevan untuk materi bertipe
	 * {@link #QUIZ}; {@code null} berarti tidak ada batas waktu.
	 *
	 * @return batas waktu kuis dalam menit, bisa {@code null}.
	 */
	public Integer getBatasWaktuMenit() {
		return batasWaktuMenit;
	}

	/**
	 * Mengisi batas waktu pengerjaan kuis dalam menit. Lihat {@link #getBatasWaktuMenit()} untuk detail perilaku getter.
	 *
	 * @param batasWaktuMenit nilai baru untuk batas waktu pengerjaan kuis dalam menit.
	 */
	public void setBatasWaktuMenit(Integer batasWaktuMenit) {
		this.batasWaktuMenit = batasWaktuMenit;
	}

	/**
	 * Mengambil batas maksimal jumlah percobaan (attempt) kuis yang boleh dilakukan satu peserta
	 * untuk materi ini. {@code null} berarti tidak dibatasi -- lihat pengecekan
	 * {@code jumlahSudah >= m.getBatasPercobaan()} di aksi mulai_kuis pada
	 * {@code _kursus_service.jsp}.
	 *
	 * @return batas jumlah percobaan kuis, bisa {@code null} (tidak dibatasi).
	 */
	public Integer getBatasPercobaan() {
		return batasPercobaan;
	}

	/**
	 * Mengisi batas maksimal jumlah percobaan kuis. Lihat {@link #getBatasPercobaan()} untuk detail perilaku getter.
	 *
	 * @param batasPercobaan nilai baru untuk batas maksimal jumlah percobaan kuis.
	 */
	public void setBatasPercobaan(Integer batasPercobaan) {
		this.batasPercobaan = batasPercobaan;
	}

	/**
	 * Mengambil penanda apakah urutan soal diacak setiap kali peserta memulai percobaan kuis
	 * baru. Hanya relevan untuk materi bertipe {@link #QUIZ}.
	 *
	 * @return {@code true} jika urutan soal diacak; default {@code false} bila belum diisi.
	 */
	public Boolean getAcakSoal() {
		return acakSoal == null ? false : acakSoal;
	}

	/**
	 * Mengisi penanda urutan soal diacak. Lihat {@link #getAcakSoal()} untuk detail perilaku getter.
	 *
	 * @param acakSoal nilai baru untuk penanda urutan soal diacak.
	 */
	public void setAcakSoal(Boolean acakSoal) {
		this.acakSoal = acakSoal;
	}

	/**
	 * Mengambil penanda apakah urutan pilihan jawaban (untuk soal pilihan ganda) diacak saat
	 * ditampilkan ke peserta.
	 *
	 * @return {@code true} jika urutan jawaban diacak; default {@code false} bila belum diisi.
	 */
	public Boolean getAcakJawaban() {
		return acakJawaban == null ? false : acakJawaban;
	}

	/**
	 * Mengisi penanda urutan pilihan jawaban diacak. Lihat {@link #getAcakJawaban()} untuk detail perilaku getter.
	 *
	 * @param acakJawaban nilai baru untuk penanda urutan pilihan jawaban diacak.
	 */
	public void setAcakJawaban(Boolean acakJawaban) {
		this.acakJawaban = acakJawaban;
	}

	/**
	 * Mengambil jumlah soal yang ditampilkan ke peserta per percobaan kuis, sebagai subset dari
	 * keseluruhan bank soal milik {@link #getUjian()} (lihat pemotongan
	 * {@code daftarSoal.subList(0, jumlahSoalDitampilkan)} di aksi mulai_kuis pada
	 * {@code _kursus_service.jsp}). {@code null} berarti seluruh soal di bank ditampilkan.
	 *
	 * @return jumlah soal yang ditampilkan per percobaan, bisa {@code null} (semua soal).
	 */
	public Integer getJumlahSoalDitampilkan() {
		return jumlahSoalDitampilkan;
	}

	/**
	 * Mengisi jumlah soal yang ditampilkan per percobaan. Lihat {@link #getJumlahSoalDitampilkan()} untuk detail perilaku getter.
	 *
	 * @param jumlahSoalDitampilkan nilai baru untuk jumlah soal yang ditampilkan per percobaan.
	 */
	public void setJumlahSoalDitampilkan(Integer jumlahSoalDitampilkan) {
		this.jumlahSoalDitampilkan = jumlahSoalDitampilkan;
	}

}
