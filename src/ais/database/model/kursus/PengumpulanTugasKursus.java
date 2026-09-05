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

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Pengumpulan tugas oleh peserta untuk satu {@link MateriKursus} bertipe
 * {@link MateriKursus#TUGAS}. Satu baris per (enrollment, materi) -- dicari lewat pasangan
 * {@link #getPesertaPunyaProdukKursus()} + {@link #getMateriKursus()} lalu di-upsert, sehingga
 * setiap kali peserta unggah ulang berkasnya (aksi {@code upload_tugas} pada
 * webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp), baris yang sama diperbarui (bukan
 * ditambah baris baru) -- berkas/nilai/catatan sebelumnya TERTIMPA tanpa riwayat versi.
 * <p>
 * <b>Relasi dengan paket {@code ais.database.model.file}:</b> BERBEDA dari
 * {@link MateriKursus} (video materi) dan {@code ProdukKursus} (thumbnail), yang menyimpan
 * berkasnya lewat {@link ais.database.model.file.LampiranLain} (tabel lampiran generik, relasi
 * longgar lewat pasangan ref+jenis), kelas ini menyimpan referensi berkas tugas SECARA
 * INDEPENDEN sebagai kolom sendiri: {@link #getNamaFile()} (nama file asli, sudah
 * di-sanitasi pemanggil) dan {@link #getLink()} (URL statis hasil unggah ke disk lewat
 * {@code simpanFileKeDisk()} pada JSP tersebut, bukan FK ke tabel {@code LampiranLain}
 * ataupun ke entity lain di paket {@code file}). Konsekuensinya: mekanisme apa pun yang berlaku
 * untuk {@code LampiranLain} (mis. resolusi jenis-namespace, self-heal, dsb -- lihat riwayat
 * perbaikan {@code ais-fix-jenis-namespace-lampiran} pada memori proyek) TIDAK otomatis
 * berlaku untuk berkas tugas kursus ini.
 * <p>
 * Alur completion: mengunggah tugas otomatis menandai {@link MateriKursus} terkait selesai di
 * {@link ProgressMateriKursus} (tanpa syarat nilai) -- lihat catatan integritas lengkap di
 * Javadoc kelas {@link ProgressMateriKursus} mengenai jalur-jalur penanda "selesai".
 * <p>
 * <b>Catatan fitur belum lengkap (dicek pada _kursus_service.jsp saat berkas ini
 * didokumentasikan):</b> kolom {@link #getNilai()}/{@link #getCatatanPenilaian()} dan konstanta
 * {@link #DINILAI} sudah disiapkan di entity ini, TAPI tidak ada aksi/endpoint di
 * {@code _kursus_service.jsp} yang benar-benar mengubah status ke {@link #DINILAI} atau mengisi
 * {@link #getNilai()} -- berbeda dari alur penilaian esai kuis yang sudah lengkap lewat aksi
 * {@code nilai_jawaban_esai} (lihat {@link JawabanPercobaanKuisKursus}). Fitur "instruktur
 * menilai tugas peserta" tampaknya baru direncanakan (field siap) namun belum diimplementasikan
 * jalur API-nya -- field/konstanta ini "tidur" sampai endpoint tersebut dibuat.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pengumpulan_tugas_kursus")
public class PengumpulanTugasKursus extends GeneralValueObject {

	/** Status awal setelah peserta mengunggah tugas, sebelum dinilai instruktur. */
	public final static String DIKUMPULKAN = "Dikumpulkan";
	/**
	 * Status setelah instruktur memberi nilai ({@link #getNilai()}) untuk tugas ini. Disiapkan
	 * di entity ini tapi (pada saat berkas ini didokumentasikan) belum ada aksi di
	 * {@code _kursus_service.jsp} yang benar-benar men-set status ke nilai ini -- lihat catatan
	 * fitur belum lengkap di Javadoc kelas.
	 */
	public final static String DINILAI = "Dinilai";

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir baris ini. Field audit "shadow" -- diisi
	 * lewat {@link ais.database.hibernate.AuditTimestampInterceptor}, bukan bagian dari data
	 * bisnis pengumpulan tugas.
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
	 * Representasi ringkas untuk log/debug: {@code kode + " - " + nama}.
	 *
	 * @return string gabungan kode dan nama pengumpulan tugas.
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private MateriKursus materiKursus;
	private String namaFile;
	private String link;
	private Date waktuKumpul;
	private String status;
	private Double nilai;
	private String catatanPenilaian;

	/** Konstruktor default -- dibutuhkan Hibernate untuk instansiasi lewat reflection. */
	public PengumpulanTugasKursus() {
	}

	/**
	 * Mengambil id unik (primary key) baris pengumpulan ini, di-generate DB lewat
	 * strategi {@code IDENTITY}.
	 *
	 * @return id pengumpulan, atau {@code null} untuk entity yang belum pernah disimpan.
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
	 * Mengambil keterangan bebas untuk pengumpulan ini (jarang dipakai -- pemanggil di
	 * {@code _kursus_service.jsp} tidak pernah mengisi kolom ini).
	 *
	 * @return keterangan pengumpulan, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kode unik baris pengumpulan ini, auto-generate sekali via
	 * {@link ais.common.BarcodeCommon#generateCode()} pada panggilan pertama jika belum ada.
	 * Dijaga {@code unique} di DB.
	 *
	 * @return kode unik pengumpulan, tidak pernah {@code null} setelah getter ini dipanggil.
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama tampilan pengumpulan, dibentuk on-the-fly dari nama peserta digabung nama
	 * materi -- BUKAN kolom murni tersimpan; ditimpa ulang setiap getter ini dipanggil bila
	 * kedua relasi berhasil di-load. Pola sama dengan {@link ProgressMateriKursus#getNama()}.
	 *
	 * @return nama gabungan "nama peserta - nama materi", atau nilai {@link #nama} apa adanya
	 *         jika relasi belum bisa di-resolve.
	 */
	public String getNama() {
		if (getMateriKursus() != null && getPesertaPunyaProdukKursus() != null
				&& getPesertaPunyaProdukKursus().getPesertaKursus() != null) {
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - " + materiKursus.getNama();
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil enrollment ({@link PesertaPunyaProdukKursus}) pemilik pengumpulan ini --
	 * bersama {@link #getMateriKursus()} membentuk kunci logis "satu baris per (enrollment,
	 * materi)".
	 *
	 * @return enrollment pemilik pengumpulan; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = false)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		pesertaPunyaProdukKursus = check(pesertaPunyaProdukKursus);
		return pesertaPunyaProdukKursus;
	}

	/**
	 * Mengisi enrollment pemilik pengumpulan ini. Tidak ada verifikasi kepemilikan di setter ini
	 * -- ditegakkan oleh pemanggil (aksi upload_tugas) sebelum entity di-upsert.
	 *
	 * @param pesertaPunyaProdukKursus enrollment pemilik pengumpulan.
	 */
	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	/**
	 * Mengambil {@link MateriKursus} bertipe {@link MateriKursus#TUGAS} yang dikumpulkan pada
	 * baris ini.
	 *
	 * @return materi tugas terkait; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "materi_kursus", nullable = false)
	public MateriKursus getMateriKursus() {
		materiKursus = check(materiKursus);
		return materiKursus;
	}

	public void setMateriKursus(MateriKursus materiKursus) {
		this.materiKursus = materiKursus;
	}

	/**
	 * Mengambil nama berkas asli yang diunggah peserta (sudah disanitasi pemanggil lewat
	 * {@code cleanFilename()} sebelum di-set -- karakter di luar alfanumerik/titik/underscore/
	 * strip diganti underscore), dipakai untuk ditampilkan ke pengguna. BUKAN nama file fisik di
	 * disk (lihat {@link #getLink()} untuk lokasi berkas sebenarnya).
	 *
	 * @return nama berkas asli (tersanitasi), bisa {@code null} jika belum ada berkas diunggah.
	 */
	public String getNamaFile() {
		return namaFile;
	}

	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

	/**
	 * Mengambil URL statis ke berkas tugas yang tersimpan di disk (hasil
	 * {@code simpanFileKeDisk("kursus_tugas", ...)} pada pemanggil), BUKAN FK ke
	 * {@link ais.database.model.file.LampiranLain} -- lihat catatan relasi dengan paket
	 * {@code file} di Javadoc kelas.
	 *
	 * @return URL berkas tugas, bisa {@code null} jika belum ada berkas diunggah.
	 */
	@Column(name = "link", nullable = true, columnDefinition = "text")
	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Mengambil waktu pengumpulan (unggah) tugas ini. Berbeda dari kebanyakan field tanggal
	 * lain di paket ini, getter ini TIDAK mengembalikan {@code null} untuk entity yang belum
	 * diisi -- fallback ke {@code new Date()} (waktu saat getter dipanggil), sama seperti pola
	 * {@link PercobaanKuisKursus#getWaktuMulai()}. Pemanggil selalu meng-set nilai eksplisit
	 * lewat {@link #setWaktuKumpul(Date)} sebelum membaca kembali via getter dalam alur normal.
	 *
	 * @return waktu pengumpulan, atau waktu saat ini jika belum pernah di-set.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuKumpul() {
		return waktuKumpul == null ? new Date() : waktuKumpul;
	}

	public void setWaktuKumpul(Date waktuKumpul) {
		this.waktuKumpul = waktuKumpul;
	}

	/**
	 * Mengambil status pengumpulan ({@link #DIKUMPULKAN}/{@link #DINILAI}). Default ke
	 * {@link #DIKUMPULKAN} bila kolom kosong/null. Lihat catatan fitur belum lengkap di Javadoc
	 * kelas -- status {@link #DINILAI} disiapkan tapi belum ada jalur API yang men-set-nya.
	 *
	 * @return status pengumpulan, tidak pernah {@code null}/kosong (fallback {@link #DIKUMPULKAN}).
	 */
	public String getStatus() {
		return status == null || status.isEmpty() ? DIKUMPULKAN : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil nilai/skor tugas yang diberikan instruktur. Lihat catatan fitur belum lengkap
	 * di Javadoc kelas -- pada saat berkas ini didokumentasikan, tidak ada jalur API yang
	 * mengisi field ini, sehingga selalu {@code null} dalam praktiknya kecuali diisi manual
	 * lewat mekanisme lain (mis. CRUD generik).
	 *
	 * @return nilai tugas, bisa {@code null} jika belum dinilai.
	 */
	public Double getNilai() {
		return nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengambil catatan/feedback tertulis instruktur untuk tugas ini. Sama seperti
	 * {@link #getNilai()}, belum ada jalur API yang mengisinya pada saat berkas ini
	 * didokumentasikan.
	 *
	 * @return catatan penilaian, bisa {@code null}.
	 */
	@Column(name = "catatan_penilaian", nullable = true, columnDefinition = "text")
	public String getCatatanPenilaian() {
		return catatanPenilaian;
	}

	public void setCatatanPenilaian(String catatanPenilaian) {
		this.catatanPenilaian = catatanPenilaian;
	}

}
