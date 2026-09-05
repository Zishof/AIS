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

import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.GeneralValueObject;

/**
 * Jawaban peserta untuk satu soal ({@link ais.database.model.BankSoal}, di-reuse dari sistem
 * CBT akademik) dalam satu {@link PercobaanKuisKursus}. Satu baris per (percobaan, soal), semua
 * dibuat sekaligus (skor 0, belum ada pilihan) saat percobaan dimulai (aksi {@code mulai_kuis}),
 * lalu diisi satu per satu lewat aksi {@code jawab_soal_kuis} setiap peserta menjawab satu soal.
 * <p>
 * Dua jenis soal diperlakukan berbeda (dibedakan lewat
 * {@link ais.database.model.PenjelasanBankSoal#KOREKSI_OTOMATIS} pada
 * {@code BankSoal.getJenisKoreksi()}):
 * <ul>
 * <li><b>Pilihan Ganda/Benar-Salah (koreksi otomatis)</b> -- jawaban berupa pilihan
 * {@link #getBankSoalDetailDipilih()}; {@link #getSkor()} dan {@link #getBenar()} langsung
 * dihitung SERVER-SIDE saat itu juga dari {@code BankSoalDetail.getSkor()}/{@code getBetul()}
 * milik detail yang dipilih, dan {@link #getSudahDinilai()} langsung {@code true}.</li>
 * <li><b>Esai (koreksi manual)</b> -- jawaban berupa {@link #getJawabanEsai()} teks bebas;
 * {@link #getSkor()} tetap 0.0 dan {@link #getSudahDinilai()} tetap {@code false} sampai
 * instruktur menilainya lewat aksi {@code nilai_jawaban_esai} (mengisi
 * {@link #getSkor()}/{@link #getCatatanPenilaian()} secara manual, dari input instruktur).</li>
 * </ul>
 * <p>
 * <b>VERIFIKASI INTEGRITAS SKOR (dicek terhadap pemanggil nyata di
 * webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp, aksi {@code jawab_soal_kuis}):</b>
 * skor TIDAK dipercaya mentah-mentah dari klien -- klien hanya mengirim
 * {@code bankSoalDetailId} (id pilihan jawaban yang dipilih peserta), dan server yang
 * menghitung {@code skor = detail.getSkor()} serta {@code benar = detail.getBetul()} dari baris
 * {@link ais.database.model.BankSoalDetail} yang diambil ulang dari DB berdasar id tersebut --
 * klien TIDAK bisa mengirim angka skor sendiri secara langsung. NAMUN, pemanggil TIDAK
 * memverifikasi bahwa {@code BankSoalDetail} yang dipilih benar-benar merupakan salah satu
 * pilihan milik {@link #getBankSoal()} soal yang sedang dijawab (tidak ada pengecekan
 * {@code detail.getBankSoal().getId().equals(soal.getId())}) -- lihat baris
 * {@code jawab_soal_kuis} sekitar {@code j.setBankSoalDetailDipilih(detail); j.setSkor(detail
 * == null ? 0.0 : detail.getSkor()); j.setBenar(detail != null && detail.getBetul());} pada JSP
 * tersebut. Karena {@code BankSoalDetail} adalah tabel bersama seluruh sistem CBT akademik
 * (bukan diberi ruang lingkup per soal/per kuis), seorang peserta yang mengetahui/menebak id
 * {@code BankSoalDetail} milik soal LAIN yang kebetulan {@code betul = true} (dari kuis lain,
 * bahkan dari modul akademik yang tidak berhubungan) dapat mengirim id tersebut sebagai
 * {@code bankSoalDetailId} untuk soal apa pun yang sedang dikerjakannya, dan akan tercatat
 * benar + mendapat skor penuh soal itu TANPA benar-benar menjawab dengan pilihan yang sah. Ini
 * kerentanan integritas nilai/sertifikasi (bukan "skor dipercaya dari klien" secara literal,
 * melainkan "identitas objek skor yang dipercaya dari klien tanpa verifikasi kepemilikan
 * terhadap konteks soal") yang genuinely dieksploitasi lewat jalur pemanggil nyata di atas.
 * TEMUAN INI DILAPORKAN sebagai task terpisah untuk perbaikan di {@code _kursus_service.jsp}
 * (di luar cakupan berkas model ini).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jawaban_percobaan_kuis_kursus")
public class JawabanPercobaanKuisKursus extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir baris ini. Field audit "shadow" -- diisi
	 * lewat {@link ais.database.hibernate.AuditTimestampInterceptor}, bukan bagian dari data
	 * bisnis jawaban.
	 *
	 * @return id pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe).
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
	 * Mengisi nama pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe).
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
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} otomatis sesaat
	 * sebelum UPDATE dieksekusi. Method audit "shadow", keharusan teknis infrastruktur.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi tanggal terakhir baris ini diubah. Biasanya diisi otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah tanggal/waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil tanggal terakhir baris ini diubah.
	 *
	 * @return tanggal/waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code kode + " - " + nama}. Karena kelas ini tidak
	 * auto-generate {@link #kode}/{@link #nama} (berbeda dari {@link PercobaanKuisKursus} dkk),
	 * hasilnya sering berupa {@code " - null"} untuk baris yang baru dibuat dan belum diisi
	 * manual.
	 *
	 * @return string gabungan kode dan nama jawaban.
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private PercobaanKuisKursus percobaanKuisKursus;
	private BankSoal bankSoal;
	private BankSoalDetail bankSoalDetailDipilih;
	private String jawabanEsai;
	private Double skor;
	private Boolean benar;
	private Integer urutanTampil;
	private Boolean sudahDinilai;
	private String catatanPenilaian;

	/** Konstruktor default -- dibutuhkan Hibernate untuk instansiasi lewat reflection. */
	public JawabanPercobaanKuisKursus() {
	}

	/**
	 * Mengambil id unik (primary key) baris jawaban ini, di-generate DB lewat
	 * strategi {@code IDENTITY}.
	 *
	 * @return id jawaban, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas untuk jawaban ini (jarang dipakai -- pemanggil di
	 * {@code _kursus_service.jsp} tidak pernah mengisi kolom ini).
	 *
	 * @return keterangan jawaban, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kode baris ini. Tidak seperti {@link PercobaanKuisKursus#getKode()}, TIDAK ada
	 * auto-generate di sini -- nilai {@code null} dikembalikan sebagai string kosong ter-trim,
	 * bukan diisi otomatis lewat {@code BarcodeCommon}.
	 *
	 * @return kode ter-trim, atau string kosong jika belum diisi.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama baris ini. Tidak ada logika derivasi otomatis di sini (berbeda dari
	 * {@link ProgressMateriKursus#getNama()}/{@link PercobaanKuisKursus#getNama()}) -- murni
	 * mengembalikan field apa adanya.
	 *
	 * @return nama jawaban, bisa {@code null}.
	 */
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil {@link PercobaanKuisKursus} tempat jawaban ini menjadi bagiannya.
	 *
	 * @return percobaan induk; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "percobaan_kuis_kursus", nullable = false)
	public PercobaanKuisKursus getPercobaanKuisKursus() {
		percobaanKuisKursus = check(percobaanKuisKursus);
		return percobaanKuisKursus;
	}

	public void setPercobaanKuisKursus(PercobaanKuisKursus percobaanKuisKursus) {
		this.percobaanKuisKursus = percobaanKuisKursus;
	}

	/**
	 * Mengambil {@link ais.database.model.BankSoal} (soal) yang dijawab pada baris ini,
	 * di-reuse langsung dari sistem CBT akademik.
	 *
	 * @return soal yang dijawab; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_soal", nullable = false)
	public BankSoal getBankSoal() {
		bankSoal = check(bankSoal);
		return bankSoal;
	}

	public void setBankSoal(BankSoal bankSoal) {
		this.bankSoal = bankSoal;
	}

	/**
	 * Mengambil {@link ais.database.model.BankSoalDetail} (pilihan jawaban) yang dipilih
	 * peserta, untuk soal Pilihan Ganda/Benar-Salah dengan koreksi otomatis. {@code null} untuk
	 * soal Esai (yang jawabannya disimpan di {@link #getJawabanEsai()}), atau untuk soal pilihan
	 * ganda yang belum dijawab sama sekali.
	 * <p>
	 * <b>CATATAN KEAMANAN:</b> field FK ini TIDAK divalidasi oleh entity ini (maupun -- pada
	 * saat berkas ini didokumentasikan -- oleh pemanggilnya) untuk memastikan detail yang
	 * dipilih benar-benar milik {@link #getBankSoal()} soal ini. Lihat catatan integritas
	 * lengkap di Javadoc kelas.
	 *
	 * @return pilihan jawaban yang dipilih peserta, atau {@code null} jika belum dijawab / soal
	 *         berjenis Esai.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_soal_detail_dipilih", nullable = true)
	public BankSoalDetail getBankSoalDetailDipilih() {
		bankSoalDetailDipilih = check(bankSoalDetailDipilih);
		return bankSoalDetailDipilih;
	}

	public void setBankSoalDetailDipilih(BankSoalDetail bankSoalDetailDipilih) {
		this.bankSoalDetailDipilih = bankSoalDetailDipilih;
	}

	/**
	 * Mengambil teks jawaban esai bebas yang ditulis peserta, untuk soal berjenis koreksi
	 * manual. Tetap {@code null}/tidak dipakai untuk soal Pilihan Ganda/Benar-Salah (yang
	 * jawabannya lewat {@link #getBankSoalDetailDipilih()}).
	 *
	 * @return teks jawaban esai, bisa {@code null}.
	 */
	@Column(name = "jawaban_esai", nullable = true, columnDefinition = "text")
	public String getJawabanEsai() {
		return jawabanEsai;
	}

	public void setJawabanEsai(String jawabanEsai) {
		this.jawabanEsai = jawabanEsai;
	}

	/**
	 * Mengambil skor yang diperoleh dari jawaban ini. Untuk soal koreksi otomatis, diisi
	 * server-side dari {@code BankSoalDetail.getSkor()} saat peserta menjawab (lihat catatan
	 * integritas skor di Javadoc kelas mengenai ketiadaan verifikasi kepemilikan detail
	 * terhadap soal). Untuk soal esai, tetap 0.0 sampai instruktur menilai manual lewat aksi
	 * {@code nilai_jawaban_esai} (yang MEMANG mempercayai angka skor dari input instruktur --
	 * ini penilaian manusia yang disengaja, bukan celah keamanan, tapi otorisasi pemanggilnya
	 * WAJIB memverifikasi identitas instruktur pemilik materi terlebih dahulu, yang sudah
	 * dilakukan lewat {@code pemilikMateriKuis()} pada JSP terkait).
	 *
	 * @return skor jawaban, default 0.0 bila belum dinilai/dijawab.
	 */
	public Double getSkor() {
		return skor == null ? 0.0 : skor;
	}

	public void setSkor(Double skor) {
		this.skor = skor;
	}

	/**
	 * Mengambil penanda benar/salah untuk jawaban pilihan ganda/benar-salah. Diisi server-side
	 * dari {@code BankSoalDetail.getBetul()} saat menjawab (lihat catatan integritas di Javadoc
	 * kelas). Selalu {@code null} untuk soal esai (kebenarannya tidak biner, dinilai lewat
	 * {@link #getSkor()} manual instruktur).
	 *
	 * @return {@code true}/{@code false} untuk soal koreksi otomatis, atau {@code null} untuk
	 *         soal esai / belum dijawab.
	 */
	public Boolean getBenar() {
		return benar;
	}

	public void setBenar(Boolean benar) {
		this.benar = benar;
	}

	/**
	 * Mengambil urutan tampil soal ini di dalam percobaannya (ditentukan sekali saat percobaan
	 * dimulai, termasuk hasil acak bila {@link MateriKursus#getAcakSoal()} aktif -- urutan ini
	 * lalu tetap sama sepanjang percobaan berlangsung, tidak diacak ulang tiap kali dimuat).
	 *
	 * @return urutan tampil soal, default 0 bila belum diisi.
	 */
	public Integer getUrutanTampil() {
		return urutanTampil == null ? 0 : urutanTampil;
	}

	public void setUrutanTampil(Integer urutanTampil) {
		this.urutanTampil = urutanTampil;
	}

	/**
	 * Mengambil penanda apakah jawaban ini sudah final dinilai. {@code true} otomatis untuk
	 * soal koreksi otomatis begitu dijawab; tetap {@code false} untuk soal esai sampai
	 * instruktur menilai manual. Field ini yang membedakan jawaban esai yang "masih menunggu
	 * koreksi" dari yang "sudah dikoreksi dengan skor 0" (keduanya punya {@link #getSkor()}
	 * bernilai 0.0 sebelum dinilai, tapi hanya salah satu yang benar-benar final).
	 *
	 * @return {@code true} jika sudah final dinilai; default {@code false} bila belum diisi.
	 */
	public Boolean getSudahDinilai() {
		return sudahDinilai == null ? false : sudahDinilai;
	}

	public void setSudahDinilai(Boolean sudahDinilai) {
		this.sudahDinilai = sudahDinilai;
	}

	/**
	 * Mengambil catatan/feedback tertulis instruktur saat menilai jawaban esai secara manual.
	 *
	 * @return catatan penilaian, bisa {@code null} jika belum dinilai atau instruktur tidak
	 *         menuliskan catatan.
	 */
	@Column(name = "catatan_penilaian", nullable = true, columnDefinition = "text")
	public String getCatatanPenilaian() {
		return catatanPenilaian;
	}

	public void setCatatanPenilaian(String catatanPenilaian) {
		this.catatanPenilaian = catatanPenilaian;
	}

}
