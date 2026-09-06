package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.common.Common;
import ais.database.model.akunting.Akun;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.jenis_diskon_mahasiswa} — <b>master/definisi
 * jenis diskon</b> mahasiswa: nama, besaran ({@link #getDiskon()}), apakah besaran itu persen
 * atau nominal tetap ({@link #getBerupaPersen()}), akun akunting tujuan, sampai lima slot item
 * biaya sasaran, rentang tanggal/semester berlaku, serta filter Fakultas (Institusi) / Jurusan
 * (Prodi) / Program / Status Awal Mahasiswa yang menyaring mahasiswa mana yang berhak.
 *
 * <p>Dibedakan dari {@link DiskonMahasiswa}, yang merupakan <b>baris pemberian</b> — penautan
 * eksplisit satu mahasiswa/calon mahasiswa ke satu instance jenis diskon ini. Banyak field pada
 * {@code DiskonMahasiswa} (aktif, semesterMulai/Sampai, itemBiaya..itemBiaya5) mewarisi nilainya
 * dari sini bila field lokalnya kosong.</p>
 *
 * <p>Ada dua jalur penerapan diskon yang dilayani entity ini:</p>
 * <ol>
 * <li><b>Diskon tertaut</b> — via {@link DiskonMahasiswa}, atau via slot bawaan pada {@code
 * Mahasiswa.getKelompokMahasiswa()} / {@code CalonMahasiswa.getJenisSeleksi()} (lihat pemanggil
 * {@link #cocokUntukKegiatan}), yakni diskon yang secara eksplisit ditugaskan ke kelompok atau
 * gelombang pendaftaran/seleksi mahasiswa tersebut.</li>
 * <li><b>Promo global</b> ("Berlaku Untuk Semua Mahasiswa", {@link #getBerlakuUntukSemuaMahasiswa()})
 * — ditambahkan 19-08-2026 sebagai jalur FALLBACK TERAKHIR lewat {@link #cariPromoGlobal} /
 * {@link #cocokUntukTagihanGlobal}, dipakai bila tidak ada satu pun diskon tertaut yang berlaku
 * pada baris tagihan. Sebelum perbaikan itu, mencentang flag ini di form tidak pernah membuat
 * diskon benar-benar dipotong dari tagihan.</li>
 * </ol>
 *
 * <p>Dipakai bersama mesin billing pusat {@code Kegiatan.java}/{@code DetailBiaya.java}/{@code
 * DetailKegiatan.java} yang sudah didokumentasikan lengkap pada batch sebelumnya — lihat method
 * {@link DetailKegiatan#hitungDiskon(Double)} untuk urutan prioritas lengkap dan catatan
 * mengenai potongan yang secara umum <b>tidak dibatasi (di-cap)</b> ke nominal baris tagihan
 * (kecuali pada jalur promo global, yang secara eksplisit dibatasi oleh pemanggilnya).</p>
 *
 * @see DiskonMahasiswa
 * @see DetailKegiatan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_diskon_mahasiswa")
public class JenisDiskonMahasiswa extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code jenis_diskon_mahasiswa}, kolom {@code id} (identity, auto-generate). */
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
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
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
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * @return string ringkas {@code id-nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat jenis diskon (opsional, untuk tampilan/pencarian ringkas), kolom implisit {@code kode}. */
	private String kode;

	/** Nama jenis diskon, kolom {@code nama} (wajib diisi, maks. 255 karakter). */
	private String nama;
	/** Keterangan bebas jenis diskon, kolom {@code keterangan}. */
	private String keterangan;
	/** Akun akunting tujuan pencatatan diskon ini (FK {@code akun}), opsional. */
	private Akun akun;
	/** Item biaya sasaran diskon slot ke-1 (FK {@code item_biaya}), opsional pada level jenis diskon. */
	private ItemBiaya itemBiaya;
	/** Item biaya sasaran diskon slot ke-2 (FK {@code item_biaya_2}), opsional. */
	private ItemBiaya itemBiaya2;
	/** Item biaya sasaran diskon slot ke-3 (FK {@code item_biaya_3}), opsional. */
	private ItemBiaya itemBiaya3;
	/** Item biaya sasaran diskon slot ke-4 (FK {@code item_biaya_4}), opsional. */
	private ItemBiaya itemBiaya4;
	/** Item biaya sasaran diskon slot ke-5 (FK {@code item_biaya_5}), opsional. */
	private ItemBiaya itemBiaya5;
	/** Besaran diskon mentah — persen (0-100) bila {@link #berupaPersen} {@code true}, atau nominal rupiah tetap bila tidak. Lihat {@link #getDiskon()}. */
	private Double diskon;
	/** Penanda arti {@link #diskon}: {@code true} = persen, {@code false}/{@code null} (default) = nominal tetap. */
	private Boolean berupaPersen;
	/** Flag aktif jenis diskon ini, kolom implisit {@code aktif}; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;
	/** Batas bawah semester berlakunya jenis diskon ini, kolom {@code semester_mulai}; {@code null} = tanpa batas bawah. */
	private Integer semesterMulai;
	/** Batas atas semester berlakunya jenis diskon ini, kolom {@code semester_sampai}; {@code null} = tanpa batas atas. */
	private Integer semesterSampai;
	/** Tanggal mulai berlaku (inklusif, awal hari), kolom {@code tanggal_mulai_berlaku}; {@code null} = tanpa batas mulai. */
	private Date tanggalMulaiBerlaku;
	/** Tanggal sampai berlaku (inklusif, akhir hari), kolom {@code tanggal_sampai_berlaku}; {@code null} = tanpa batas akhir. */
	private Date tanggalSampaiBerlaku;
	/** Flag promo global "Berlaku Untuk Semua Mahasiswa", kolom {@code berlaku_untuk_semua_mahasiswa}; lihat javadoc kelas mengenai jalur fallback promo global. */
	private Boolean berlakuUntukSemuaMahasiswa;
	/** Filter Fakultas (Institusi): bila diisi, diskon hanya berlaku untuk mahasiswa/calon mahasiswa di fakultas ini. */
	private Fakultas fakultas;
	/** Filter Jurusan (Prodi): bila diisi, diskon hanya berlaku untuk mahasiswa/calon mahasiswa di jurusan ini. */
	private Jurusan jurusan;
	/** Filter Program (mis. Reguler/Karyawan), kolom {@code program}; dibandingkan case-insensitive. */
	private String program;
	/** Filter Status Awal Mahasiswa (mis. Baru-A/Pindahan): bila diisi, diskon hanya berlaku untuk status awal ini. */
	private StatusAwalMahasiswa statusAwalMahasiswa;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public JenisDiskonMahasiswa() {
	}

	/**
	 * @return primary key baris {@code jenis_diskon_mahasiswa}; {@code null} sebelum baris
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
	 * @return kode singkat jenis diskon, di-{@code trim()}; string kosong (bukan {@code null})
	 *         bila belum diisi.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * @param kode kode singkat baru untuk jenis diskon ini.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama jenis diskon, di-{@code trim()}; {@code null} bila field mentah {@code null}
	 *         (meski kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama baru untuk jenis diskon ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas jenis diskon ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk jenis diskon ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif jenis diskon ini; {@code true} sebagai default bila field mentah
	 *         {@code null} (belum pernah diset eksplisit ke tidak-aktif).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif baru; {@code false} untuk menonaktifkan jenis diskon ini secara
	 *              keseluruhan (berdampak ke semua {@link DiskonMahasiswa} yang menautkannya,
	 *              lihat {@link DiskonMahasiswa#getAktif()}).
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return akun akunting tujuan pencatatan diskon ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * @param akun akun akunting tujuan baru; {@code null} untuk melepas tautan.
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Besaran diskon mentah, arti angkanya bergantung pada {@link #getBerupaPersen()}: persen
	 * (0-100) bila {@code true}, atau nominal rupiah tetap bila {@code false}. Nilai ini
	 * <b>tidak dibatasi/divalidasi</b> di sini — lihat javadoc kelas mengenai potongan yang
	 * umumnya tidak di-cap ke nominal baris tagihan.
	 *
	 * @return besaran diskon; {@code 0.0} bila belum diisi.
	 */
	public Double getDiskon() {
		return diskon == null ? 0.0 : diskon;
	}

	/**
	 * @param diskon besaran diskon baru (persen atau nominal, tergantung {@link #getBerupaPersen()}).
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * @return {@code true} bila {@link #getDiskon()} harus dibaca sebagai persentase (0-100),
	 *         {@code false} bila nominal rupiah tetap; default {@code true} bila belum diisi.
	 */
	public Boolean getBerupaPersen() {
		return berupaPersen == null ? true : berupaPersen;
	}

	/**
	 * @param berupaPersen {@code true} untuk menandai {@link #getDiskon()} sebagai persen,
	 *                     {@code false} untuk nominal tetap.
	 */
	public void setBerupaPersen(Boolean berupaPersen) {
		this.berupaPersen = berupaPersen;
	}

	/**
	 * @return item biaya sasaran diskon slot ke-1 pada level jenis diskon (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return itemBiaya;
	}

	/**
	 * @param itemBiaya item biaya slot ke-1 baru; {@code null} untuk melepas tautan.
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * @return batas bawah semester berlakunya jenis diskon ini; {@code null} berarti tanpa
	 *         batas bawah (tidak seperti {@link DiskonMahasiswa#getSemesterMulai()} yang
	 *         mem-fallback ke {@code 0}, di sini {@code null} dikembalikan apa adanya).
	 */
	@Column(name = "semester_mulai")
	public Integer getSemesterMulai() {
		return semesterMulai;
	}

	/**
	 * @param semesterMulai batas bawah semester baru; {@code null} untuk menghapus batas bawah.
	 */
	public void setSemesterMulai(Integer semesterMulai) {
		this.semesterMulai = semesterMulai;
	}

	/**
	 * @return batas atas semester berlakunya jenis diskon ini; {@code null} berarti tanpa batas
	 *         atas (tidak seperti {@link DiskonMahasiswa#getSemesterSampai()} yang mem-fallback
	 *         ke 1/8, di sini {@code null} dikembalikan apa adanya).
	 */
	@Column(name = "semester_sampai")
	public Integer getSemesterSampai() {
		return semesterSampai;
	}

	/**
	 * @param semesterSampai batas atas semester baru; {@code null} untuk menghapus batas atas.
	 */
	public void setSemesterSampai(Integer semesterSampai) {
		this.semesterSampai = semesterSampai;
	}

	/**
	 * @return tanggal mulai berlaku jenis diskon ini (dibandingkan inklusif dari awal hari oleh
	 *         {@link #cocokTanggalBerlaku}); {@code null} berarti tanpa batas mulai.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_mulai_berlaku")
	public Date getTanggalMulaiBerlaku() {
		return tanggalMulaiBerlaku;
	}

	/**
	 * @param tanggalMulaiBerlaku tanggal mulai berlaku baru; {@code null} untuk menghapus batas mulai.
	 */
	public void setTanggalMulaiBerlaku(Date tanggalMulaiBerlaku) {
		this.tanggalMulaiBerlaku = tanggalMulaiBerlaku;
	}

	/**
	 * @return tanggal sampai berlaku jenis diskon ini (dibandingkan inklusif sampai akhir hari
	 *         oleh {@link #cocokTanggalBerlaku}); {@code null} berarti tanpa batas akhir.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_sampai_berlaku")
	public Date getTanggalSampaiBerlaku() {
		return tanggalSampaiBerlaku;
	}

	/**
	 * @param tanggalSampaiBerlaku tanggal sampai berlaku baru; {@code null} untuk menghapus batas akhir.
	 */
	public void setTanggalSampaiBerlaku(Date tanggalSampaiBerlaku) {
		this.tanggalSampaiBerlaku = tanggalSampaiBerlaku;
	}

	/**
	 * Flag promo global "Berlaku Untuk Semua Mahasiswa" — lihat javadoc kelas untuk penjelasan
	 * lengkap jalur fallback yang diaktifkan flag ini sejak perbaikan 19-08-2026.
	 *
	 * @return {@code true} bila jenis diskon ini merupakan kandidat promo global; default
	 *         {@code false} bila belum diisi.
	 */
	@Column(name = "berlaku_untuk_semua_mahasiswa")
	public Boolean getBerlakuUntukSemuaMahasiswa() {
		return berlakuUntukSemuaMahasiswa == null ? false : berlakuUntukSemuaMahasiswa;
	}

	/**
	 * @param berlakuUntukSemuaMahasiswa {@code true} untuk menjadikan jenis diskon ini kandidat
	 *                                    promo global.
	 */
	public void setBerlakuUntukSemuaMahasiswa(Boolean berlakuUntukSemuaMahasiswa) {
		this.berlakuUntukSemuaMahasiswa = berlakuUntukSemuaMahasiswa;
	}

	/**
	 * @return filter Fakultas (Institusi) jenis diskon ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} berarti tidak difilter berdasarkan fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param fakultas filter fakultas baru; {@code null} untuk menghapus filter.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return filter Jurusan (Prodi) jenis diskon ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} berarti tidak difilter berdasarkan jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param jurusan filter jurusan baru; {@code null} untuk menghapus filter.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return filter Program jenis diskon ini, di-{@code trim()}; {@code null} berarti tidak
	 *         difilter berdasarkan program. Dibandingkan case-insensitive oleh {@link
	 *         #cocokUntukKegiatan} dan {@link #cocokUntukTagihanGlobal}.
	 */
	@Column(name = "program", length = 50)
	public String getProgram() {
		return program == null ? null : program.trim();
	}

	/**
	 * @param program filter program baru; {@code null} untuk menghapus filter.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * @return filter Status Awal Mahasiswa jenis diskon ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} berarti tidak difilter berdasarkan status awal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		return statusAwalMahasiswa;
	}

	/**
	 * @param statusAwalMahasiswa filter status awal baru; {@code null} untuk menghapus filter.
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Menguji apakah jenis diskon ini berlaku untuk satu baris tagihan ({@code kegiatan} +
	 * {@code detailBiaya}) pada jalur <b>diskon tertaut</b> — dipanggil dari tiga tempat berbeda
	 * di {@code DetailKegiatan}/{@code Kegiatan}: slot bawaan {@code
	 * KelompokMahasiswa.getJenisDiskonMahasiswa()}, slot bawaan {@code
	 * CalonMahasiswa/Mahasiswa.getJenisSeleksi().getJenisDiskonMahasiswa()}, dan tautan per-orang
	 * {@link DiskonMahasiswa#getJenisDiskonMahasiswa()} (lewat {@code DetailKegiatan.diskonCocok}).
	 * Perhatikan bahwa penyaringan <b>rentang semester</b> (semesterMulai/semesterSampai) TIDAK
	 * dilakukan di method ini — pemanggil melakukannya sendiri sebelum atau sesudah memanggil
	 * method ini (lihat catatan di {@code DetailKegiatan.getDiskonMahasiswaData()} dan
	 * {@code DetailKegiatan.hitungDiskon(Double)}).
	 *
	 * <h4>Alur pemeriksaan dan sebuah asimetri penting</h4>
	 * <p>Langkah pertama selalu {@link #cocokTanggalBerlaku(Kegiatan)} — bila tanggal kegiatan
	 * di luar rentang {@code tanggalMulaiBerlaku}..{@code tanggalSampaiBerlaku}, method langsung
	 * mengembalikan {@code false} tanpa memeriksa apa pun lagi.</p>
	 * <p>Langkah berikutnya adalah percabangan yang menentukan <b>apakah filter Fakultas/
	 * Jurusan/Program/Status Awal di bawahnya diperiksa sama sekali</b>: {@code if
	 * (!getBerlakuUntukSemuaMahasiswa()) { return true; }}. Artinya, untuk jenis diskon yang
	 * <b>bukan</b> promo global — yakni yang ditautkan secara eksplisit lewat {@link
	 * DiskonMahasiswa}, {@code KelompokMahasiswa}, atau {@code JenisSeleksi} — keempat filter itu
	 * (Fakultas, Jurusan, Program, Status Awal) yang tersimpan pada jenis diskon <b>sama sekali
	 * tidak ditegakkan</b> lewat method ini: begitu tanggal cocok, hasilnya otomatis {@code true}.
	 * Filter-filter itu baru benar-benar berfungsi untuk jenis diskon yang memang berflag {@link
	 * #getBerlakuUntukSemuaMahasiswa()} (lewat cabang di bawahnya, dan lebih lengkap lagi lewat
	 * {@link #cocokUntukTagihanGlobal}). Perilaku ini tampaknya disengaja — penautan eksplisit
	 * (assignment ke kelompok/gelombang/perorangan) dianggap sudah cukup menyaring populasi
	 * penerima secara implisit di sisi pemanggil — tetapi berarti mengisi Fakultas/Jurusan/
	 * Program/Status Awal pada sebuah jenis diskon yang <b>tidak</b> dicentang "Berlaku Untuk
	 * Semua Mahasiswa" adalah pengaturan yang efektif tidak berpengaruh sama sekali lewat jalur
	 * ini; operator formulir bisa salah kira filter tersebut berlaku universal.</p>
	 * <p>Untuk jenis diskon berflag promo global, method berlanjut: bila {@code kegiatan} atau
	 * {@code kegiatan.getMahasiswa()} {@code null} (mis. baris kegiatan calon mahasiswa PMB),
	 * method langsung mengembalikan {@code true} — filter Fakultas/Jurusan/dst tidak diperiksa
	 * untuk populasi calon mahasiswa lewat method ini (berbeda dengan {@link
	 * #cocokUntukTagihanGlobal}, yang secara eksplisit menelusuri {@code
	 * kegiatan.getCalonMahasiswa()} untuk acuan jurusan/program/status awal). Baru untuk
	 * mahasiswa aktif, method mengambil {@link HistoryStatusMahasiswa} kegiatan (via {@link
	 * #ambilHistoryStatusMahasiswa}) sebagai acuan program/status-awal yang lebih akurat
	 * (mengikuti riwayat KRS semester bersangkutan, bukan status mahasiswa saat ini), lalu
	 * menguji Jurusan, Fakultas (diturunkan dari jurusan acuan atau {@code detailBiaya} sebagai
	 * fallback), Program (case-insensitive), dan Status Awal Mahasiswa satu per satu — begitu
	 * satu filter yang diisi tidak cocok, method mengembalikan {@code false}.</p>
	 *
	 * <p><b>Efek samping:</b> memicu lazy-load lewat {@link #getJurusan()}/{@link #getFakultas()}/
	 * {@link #getStatusAwalMahasiswa()}; {@link #ambilHistoryStatusMahasiswa} melakukan sinkronisasi
	 * KRS ({@code Common.singkronkanKrsMahasiswa}) yang berpotensi menulis ke database.</p>
	 *
	 * @param kegiatan    kegiatan/tagihan yang sedang dihitung; boleh {@code null} (diperlakukan
	 *                    sebagai tanggal hari ini pada pengecekan tanggal, dan sebagai "cocok"
	 *                    pada percabangan promo global)
	 * @param detailBiaya detail biaya baris tagihan, dipakai sebagai fallback acuan jurusan/fakultas
	 *                    saat kegiatan sendiri tidak menyediakannya; boleh {@code null}
	 * @return {@code true} bila jenis diskon ini dianggap berlaku untuk baris tagihan tersebut
	 * @see #cocokTanggalBerlaku(Kegiatan)
	 * @see #cocokUntukTagihanGlobal(Kegiatan, DetailBiaya)
	 */
	public boolean cocokUntukKegiatan(Kegiatan kegiatan, DetailBiaya detailBiaya) {
		if (!cocokTanggalBerlaku(kegiatan)) {
			return false;
		}
		if (!getBerlakuUntukSemuaMahasiswa()) {
			return true;
		}
		if (kegiatan == null || kegiatan.getMahasiswa() == null) {
			return true;
		}

		HistoryStatusMahasiswa historyStatusMahasiswa = ambilHistoryStatusMahasiswa(kegiatan);
		Jurusan jurusanAcuan = kegiatan.getJurusan();
		if (jurusanAcuan == null && detailBiaya != null) {
			jurusanAcuan = detailBiaya.getJurusan();
		}
		if (getJurusan() != null && (jurusanAcuan == null || jurusanAcuan.getId() == null
				|| !getJurusan().getId().equals(jurusanAcuan.getId()))) {
			return false;
		}
		if (getFakultas() != null) {
			Fakultas fakultasAcuan = jurusanAcuan == null ? null : jurusanAcuan.getFakultas();
			if (fakultasAcuan == null && detailBiaya != null) {
				fakultasAcuan = detailBiaya.getFakultas();
			}
			if (fakultasAcuan == null || fakultasAcuan.getId() == null
					|| !getFakultas().getId().equals(fakultasAcuan.getId())) {
				return false;
			}
		}
		if (getProgram() != null && !getProgram().trim().isEmpty()) {
			String programAcuan = historyStatusMahasiswa == null ? kegiatan.getProgram() : historyStatusMahasiswa.getProgram();
			if (programAcuan == null || !getProgram().trim().equalsIgnoreCase(programAcuan.trim())) {
				return false;
			}
		}
		if (getStatusAwalMahasiswa() != null) {
			StatusAwalMahasiswa statusAwalAcuan = historyStatusMahasiswa == null ? kegiatan.getMahasiswa().getStatusAwalMahasiswa()
					: historyStatusMahasiswa.getStatusAwalMahasiswa();
			if (statusAwalAcuan == null || statusAwalAcuan.getId() == null
					|| !getStatusAwalMahasiswa().getId().equals(statusAwalAcuan.getId())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Menguji rentang tanggal berlaku ({@link #getTanggalMulaiBerlaku()}..{@link
	 * #getTanggalSampaiBerlaku()}) terhadap tanggal acuan satu kegiatan/tagihan.
	 *
	 * <p>Tanggal acuan diambil dari {@code kegiatan.getTanggal()}; bila {@code kegiatan} atau
	 * tanggalnya {@code null}, dipakai waktu sekarang ({@link ais.ui.util.WaktuUtil#getDate()}).
	 * Batas bawah dibandingkan inklusif dari awal hari ({@link #awalHari}, 00:00:00.000) dan
	 * batas atas inklusif sampai akhir hari ({@link #akhirHari}, 23:59:59.999), sehingga tanggal
	 * berlaku bersifat "sepanjang hari itu", bukan sekadar titik waktu penyimpanannya. Batas yang
	 * {@code null} berarti tanpa batas ke arah tersebut.</p>
	 *
	 * @param kegiatan kegiatan/tagihan acuan; boleh {@code null}
	 * @return {@code true} bila tanggal acuan berada dalam rentang berlaku (atau kedua batas
	 *         {@code null})
	 */
	private boolean cocokTanggalBerlaku(Kegiatan kegiatan) {
		Date tanggalAcuan = kegiatan == null || kegiatan.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate()
				: kegiatan.getTanggal();
		if (getTanggalMulaiBerlaku() != null && tanggalAcuan.before(awalHari(getTanggalMulaiBerlaku()))) {
			return false;
		}
		if (getTanggalSampaiBerlaku() != null && tanggalAcuan.after(akhirHari(getTanggalSampaiBerlaku()))) {
			return false;
		}
		return true;
	}

	/**
	 * @param tanggal tanggal acuan
	 * @return salinan {@code tanggal} dengan jam/menit/detik/milidetik dinolkan (00:00:00.000),
	 *         yakni awal hari yang sama.
	 */
	private Date awalHari(Date tanggal) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(tanggal);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * @param tanggal tanggal acuan
	 * @return salinan {@code tanggal} dengan jam/menit/detik/milidetik diset ke akhir hari
	 *         (23:59:59.999), yakni akhir hari yang sama.
	 */
	private Date akhirHari(Date tanggal) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(tanggal);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	// ════════════════════════════════════════════════════════════════════════════════
	// PROMO GLOBAL — "Berlaku Untuk Semua Mahasiswa" (perbaikan 19-08-2026)
	// ════════════════════════════════════════════════════════════════════════════════
	// SEBELUMNYA: centang "Berlaku Untuk Semua Mahasiswa" TIDAK pernah menjadi jalur
	// penerapan diskon — mesin tagihan hanya menerapkan diskon yang DITAUTKAN lewat
	// Gelombang Pendaftaran / Jenis Seleksi / assignment per-orang, sehingga promo
	// global (mis. "Promo Kemerdekaan" berlaku 17–31 Agustus untuk semua) tidak pernah
	// memotong tagihan. Sekarang jenis diskon ber-flag ini dicari sebagai FALLBACK
	// TERAKHIR (hanya bila tidak ada diskon lain yang berlaku), dengan seluruh filter
	// yang tampil di form dihormati: rentang tanggal, Fakultas (Institusi), Jurusan
	// (Prodi), Program, Status Awal, batas semester, dan daftar item biaya.

	/** Cache daftar promo global (jarang berubah) agar grid tagihan tidak query berulang. */
	private static volatile List<JenisDiskonMahasiswa> cachePromoGlobal = null;
	/** Stempel waktu (epoch millis) saat {@link #cachePromoGlobal} terakhir diisi ulang dari database. */
	private static volatile long cachePromoGlobalWaktu = 0L;
	/** Masa hidup cache promo global dalam milidetik (60 detik) sebelum dianggap kedaluwarsa. */
	private static final long TTL_CACHE_PROMO_GLOBAL_MS = 60000L;

	/**
	 * Mengosongkan cache statis {@link #cachePromoGlobal} secara paksa, tanpa menunggu TTL
	 * habis. Perlu dipanggil setelah sebuah {@link JenisDiskonMahasiswa} disimpan/diubah (mis.
	 * flag {@code berlakuUntukSemuaMahasiswa} atau {@code aktif}-nya berubah) agar mesin tagihan
	 * langsung melihat perubahan itu, bukan menunggu sampai {@link #TTL_CACHE_PROMO_GLOBAL_MS}
	 * berlalu. Cache bersifat statis per-JVM (bukan per-session/per-user), sehingga panggilan
	 * ini memengaruhi seluruh pengguna aplikasi yang berjalan pada instance yang sama.
	 */
	public static void bersihkanCachePromoGlobal() {
		cachePromoGlobal = null;
		cachePromoGlobalWaktu = 0L;
	}

	/**
	 * Mengambil daftar seluruh jenis diskon berflag promo global ({@code
	 * berlakuUntukSemuaMahasiswa = true}) yang aktif (kolom {@code aktif} {@code null} atau
	 * {@code true}), diurutkan berdasar {@code id}. Hasil di-cache secara statis (lihat {@link
	 * #cachePromoGlobal}) selama {@link #TTL_CACHE_PROMO_GLOBAL_MS} (60 detik) agar mesin
	 * tagihan yang menghitung banyak baris tidak melakukan query berulang untuk data yang jarang
	 * berubah.
	 *
	 * <p><b>Fail-closed pada error:</b> bila query gagal (mis. masalah koneksi database),
	 * exception dicatat ke {@link ais.common.ErrorAuditUtil} dan method mengembalikan (serta
	 * meng-cache) daftar kosong — promo global dianggap tidak ada untuk sisa periode TTL,
	 * bukan melempar exception ke pemanggil.</p>
	 *
	 * @return daftar promo global aktif, urut {@code id} ascending; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private static List<JenisDiskonMahasiswa> ambilDaftarPromoGlobal() {
		List<JenisDiskonMahasiswa> cache = cachePromoGlobal;
		long sekarang = System.currentTimeMillis();
		if (cache != null && (sekarang - cachePromoGlobalWaktu) < TTL_CACHE_PROMO_GLOBAL_MS) {
			return cache;
		}
		List<JenisDiskonMahasiswa> hasil = new ArrayList<JenisDiskonMahasiswa>();
		org.hibernate.Session session = null;
		try {
			session = ais.database.hibernate.HibernateUtil.openSession();
			hasil = session.createCriteria(JenisDiskonMahasiswa.class)
					.add(org.hibernate.criterion.Restrictions.eq("berlakuUntukSemuaMahasiswa", Boolean.TRUE))
					.add(org.hibernate.criterion.Restrictions.or(
							org.hibernate.criterion.Restrictions.isNull("aktif"),
							org.hibernate.criterion.Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(org.hibernate.criterion.Order.asc("id")).list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "JenisDiskonMahasiswa.ambilDaftarPromoGlobal");
			hasil = new ArrayList<JenisDiskonMahasiswa>();
		} finally {
			if (session != null) {
				ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
			}
		}
		cachePromoGlobal = hasil;
		cachePromoGlobalWaktu = sekarang;
		return hasil;
	}

	/**
	 * Mencari, dari seluruh promo global aktif ({@link #ambilDaftarPromoGlobal()}), satu jenis
	 * diskon yang berlaku untuk baris tagihan {@code kegiatan}/{@code detailBiaya} dan
	 * memberikan potongan paling menguntungkan mahasiswa.
	 *
	 * <p>Dipanggil sebagai <b>fallback terakhir</b> oleh {@code DetailKegiatan.cariJenisDiskonMahasiswa()}
	 * dan {@code DetailKegiatan.hitungDiskon(Double)} (juga oleh {@code Kegiatan.java}), hanya
	 * setelah dipastikan tidak ada diskon tertaut (kelompok mahasiswa, jenis seleksi, atau
	 * tautan per-orang {@link DiskonMahasiswa}) yang berlaku pada baris itu.</p>
	 *
	 * <h4>Validasi awal dan iterasi</h4>
	 * <p>Bila {@code kegiatan}, {@code detailBiaya}, {@code detailBiaya.getItemBiaya()}, atau
	 * ID-nya {@code null}, method langsung mengembalikan {@code null} tanpa memeriksa satu pun
	 * promo — baris tagihan tanpa item biaya yang jelas tidak bisa dicocokkan dengan daftar
	 * item biaya promo mana pun. Selebihnya, method mengiterasi seluruh promo dari cache,
	 * melewati (skip) promo dengan {@link #getDiskon()} {@code null} atau {@code <= 0.0} (diskon
	 * nol/negatif tidak berguna dipilih), lalu menguji kecocokan lewat {@link
	 * #cocokUntukTagihanGlobal(Kegiatan, DetailBiaya)}.</p>
	 *
	 * <h4>Pemilihan "paling menguntungkan" dan hubungannya dengan pembatasan nominal</h4>
	 * <p>Untuk setiap promo yang cocok, potongan dihitung sebagai {@code dasar * (diskon / 100)}
	 * bila {@link #getBerupaPersen()} {@code true}, atau {@code diskon} apa adanya (nominal
	 * tetap) bila {@code false} — {@code dasar} adalah parameter {@code jumlah} (nominal tagihan
	 * sebelum diskon). Promo dengan potongan terbesar yang ditemukan sejauh ini disimpan sebagai
	 * kandidat terbaik ({@code potonganTerbaik}/{@code terbaik}); method mengembalikan <b>objek
	 * jenis diskonnya</b>, bukan nilai potongannya.</p>
	 * <p><b>Catatan verifikasi rumus (dibandingkan dengan bug diskon-melebihi-nominal yang
	 * ditemukan pada mesin billing {@code Kegiatan}/{@code DetailKegiatan}, promo global bukan
	 * per-orang):</b> perhitungan {@code potongan} di sini <b>tidak dibatasi (di-cap)</b> ke
	 * {@code dasar} — sebuah promo dengan {@code berupaPersen = false} dan {@code diskon} lebih
	 * besar dari {@code dasar} akan tetap dianggap "menguntungkan" dan bisa terpilih sebagai
	 * {@code terbaik} meski potongannya melebihi nominal baris. Namun pada seluruh titik
	 * pemanggilan yang ada saat ini ({@code DetailKegiatan}), pemanggil <b>menghitung ulang
	 * potongan secara independen</b> dari objek yang dikembalikan method ini dan secara eksplisit
	 * mengklemnya ({@code if (potongan > jumlahDiskon) potongan = jumlahDiskon;}) sebelum
	 * dipakai sebagai potongan final. Karena itu, sejauh pemanggilnya tetap disiplin melakukan
	 * clamp tersebut, celah ini tidak (belum) tereksploitasi lewat jalur promo global — berbeda
	 * dengan jalur diskon tertaut kelompok mahasiswa/jenis seleksi pada {@code
	 * DetailKegiatan.hitungDiskon}, yang memang tidak punya clamp sama sekali. Diperlakukan
	 * sebagai <b>perluasan</b> temuan bug kalkulasi diskon yang sudah tercatat sebelumnya
	 * (bukan celah baru yang independen), karena mekanismenya (perhitungan tanpa cap yang
	 * capping-nya didelegasikan ke pemanggil) identik.</p>
	 *
	 * @param kegiatan    kegiatan/tagihan yang sedang dihitung
	 * @param detailBiaya detail biaya baris tagihan (menentukan item biaya & prodi acuan)
	 * @param jumlah      nominal tagihan sebelum diskon (untuk membandingkan persen vs nominal)
	 * @return jenis diskon promo global terbaik yang berlaku, atau {@code null} bila tidak ada
	 * @see #cocokUntukTagihanGlobal(Kegiatan, DetailBiaya)
	 */
	public static JenisDiskonMahasiswa cariPromoGlobal(Kegiatan kegiatan, DetailBiaya detailBiaya, Double jumlah) {
		if (kegiatan == null || detailBiaya == null || detailBiaya.getItemBiaya() == null
				|| detailBiaya.getItemBiaya().getId() == null) {
			return null;
		}
		JenisDiskonMahasiswa terbaik = null;
		double potonganTerbaik = 0.0;
		double dasar = jumlah == null ? 0.0 : jumlah.doubleValue();
		for (JenisDiskonMahasiswa promo : ambilDaftarPromoGlobal()) {
			try {
				if (promo == null || promo.getDiskon() == null || promo.getDiskon() <= 0.0) {
					continue;
				}
				if (!promo.cocokUntukTagihanGlobal(kegiatan, detailBiaya)) {
					continue;
				}
				double potongan = promo.getBerupaPersen() ? (dasar * (promo.getDiskon() / 100.0)) : promo.getDiskon();
				if (potongan > potonganTerbaik) {
					potonganTerbaik = potongan;
					terbaik = promo;
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "JenisDiskonMahasiswa.cariPromoGlobal id="
						+ (promo == null ? null : promo.getId()));
			}
		}
		return terbaik;
	}

	/**
	 * Kecocokan promo global untuk satu baris tagihan: item biaya, rentang tanggal, batas
	 * semester, serta filter Fakultas (Institusi) / Jurusan (Prodi) / Program / Status Awal.
	 * Berlaku untuk mahasiswa aktif MAUPUN calon mahasiswa (PMB).
	 *
	 * <p>Ini adalah pemeriksaan kecocokan promo global yang <b>paling lengkap</b> di kelas ini —
	 * berbeda dengan {@link #cocokUntukKegiatan}, method ini SELALU menegakkan seluruh enam
	 * filter secara berurutan (nomor mengikuti komentar pada kode):</p>
	 * <ol>
	 * <li><b>Item biaya</b> — baris tagihan ditolak kalau {@code detailBiaya.getItemBiaya()}
	 * tidak termasuk salah satu dari lima slot Default Item Biaya I..V jenis diskon ini
	 * ({@link #ambilItemBiayaIds()}). Ini gerbang wajib pertama: promo tanpa item biaya sama
	 * sekali ({@code ambilItemBiayaIds()} kosong) tidak pernah cocok untuk baris apa pun.</li>
	 * <li><b>Rentang tanggal</b> — didelegasikan ke {@link #cocokTanggalBerlaku(Kegiatan)}.</li>
	 * <li><b>Batas semester</b> — hanya diperiksa bila {@code kegiatan.getSemster()} tidak
	 * {@code null}; dibandingkan langsung terhadap {@link #getSemesterMulai()}/{@link
	 * #getSemesterSampai()} milik jenis diskon (yang bisa {@code null} berarti tanpa batas).</li>
	 * <li><b>Jurusan (Prodi) / Fakultas (Institusi)</b> — acuan jurusan dicari berturutan dari
	 * {@code kegiatan.getJurusan()}, {@code detailBiaya.getJurusan()}, {@code
	 * kegiatan.getMahasiswa().getJurusan()}, lalu {@code
	 * kegiatan.getCalonMahasiswa().ambilJurusan()} (prodi kelulusan bila sudah ada, atau pilihan
	 * prodi pertama saat pendaftaran); fakultas acuan diturunkan dari jurusan itu atau, bila
	 * tidak ada, dari {@code detailBiaya.getFakultas()}.</li>
	 * <li><b>Program</b> — acuan diambil dari {@code kegiatan.getProgram()}, lalu calon
	 * mahasiswa, lalu mahasiswa aktif (fallback berjenjang); dibandingkan case-insensitive.</li>
	 * <li><b>Status Awal Mahasiswa</b> — acuan diambil dari calon mahasiswa dahulu, baru
	 * mahasiswa aktif.</li>
	 * </ol>
	 * <p>Untuk setiap filter yang diisi ({@code getJurusan()}/{@code getFakultas()}/{@code
	 * getProgram()}/{@code getStatusAwalMahasiswa()} tidak {@code null}), kegagalan mencocokkan
	 * ATAU acuan yang tidak ditemukan sama sekali membuat method mengembalikan {@code false}
	 * (fail-closed per filter). Method ini dipakai baik oleh {@link #cariPromoGlobal} maupun
	 * langsung sebagai gerbang kedua {@link #cocokUntukKegiatan} untuk jenis diskon berflag
	 * promo global.</p>
	 *
	 * @param kegiatan    kegiatan/tagihan yang diuji
	 * @param detailBiaya detail biaya baris tagihan
	 * @return {@code true} bila seluruh filter yang diisi pada jenis diskon ini terpenuhi
	 * @see #cariPromoGlobal(Kegiatan, DetailBiaya, Double)
	 */
	public boolean cocokUntukTagihanGlobal(Kegiatan kegiatan, DetailBiaya detailBiaya) {
		if (kegiatan == null || detailBiaya == null || detailBiaya.getItemBiaya() == null) {
			return false;
		}
		// (1) Item biaya harus termasuk daftar Default Item Biaya I..V pada jenis diskon.
		List<Long> itemIds = ambilItemBiayaIds();
		if (itemIds == null || itemIds.isEmpty() || !itemIds.contains(detailBiaya.getItemBiaya().getId())) {
			return false;
		}
		// (2) Rentang tanggal berlaku promo.
		if (!cocokTanggalBerlaku(kegiatan)) {
			return false;
		}
		// (3) Batas semester (bila diisi).
		Integer semester = kegiatan.getSemster();
		if (semester != null) {
			if (getSemesterMulai() != null && getSemesterMulai() > semester) {
				return false;
			}
			if (getSemesterSampai() != null && getSemesterSampai() < semester) {
				return false;
			}
		}
		// (4) Filter Jurusan (Prodi) / Fakultas (Institusi) — acuan diambil dari kegiatan,
		// detail biaya, lalu identitas mahasiswa/calon mahasiswa.
		Jurusan jurusanAcuan = kegiatan.getJurusan();
		if (jurusanAcuan == null) {
			jurusanAcuan = detailBiaya.getJurusan();
		}
		if (jurusanAcuan == null && kegiatan.getMahasiswa() != null) {
			jurusanAcuan = kegiatan.getMahasiswa().getJurusan();
		}
		if (jurusanAcuan == null && kegiatan.getCalonMahasiswa() != null) {
			// ambilJurusan(): prodi kelulusan bila sudah ada, jika belum pakai pilihan prodi ke-1.
			jurusanAcuan = kegiatan.getCalonMahasiswa().ambilJurusan();
		}
		if (getJurusan() != null && (jurusanAcuan == null || jurusanAcuan.getId() == null
				|| !getJurusan().getId().equals(jurusanAcuan.getId()))) {
			return false;
		}
		if (getFakultas() != null) {
			Fakultas fakultasAcuan = jurusanAcuan == null ? null : jurusanAcuan.getFakultas();
			if (fakultasAcuan == null) {
				fakultasAcuan = detailBiaya.getFakultas();
			}
			if (fakultasAcuan == null || fakultasAcuan.getId() == null
					|| !getFakultas().getId().equals(fakultasAcuan.getId())) {
				return false;
			}
		}
		// (5) Filter Program.
		if (getProgram() != null && !getProgram().trim().isEmpty()) {
			String programAcuan = kegiatan.getProgram();
			if ((programAcuan == null || programAcuan.trim().isEmpty()) && kegiatan.getCalonMahasiswa() != null) {
				programAcuan = kegiatan.getCalonMahasiswa().getProgram();
			}
			if ((programAcuan == null || programAcuan.trim().isEmpty()) && kegiatan.getMahasiswa() != null) {
				programAcuan = kegiatan.getMahasiswa().getProgram();
			}
			if (programAcuan == null || !getProgram().trim().equalsIgnoreCase(programAcuan.trim())) {
				return false;
			}
		}
		// (6) Filter Status Awal (mis. Baru-A / Pindahan).
		if (getStatusAwalMahasiswa() != null) {
			StatusAwalMahasiswa statusAcuan = null;
			if (kegiatan.getCalonMahasiswa() != null) {
				statusAcuan = kegiatan.getCalonMahasiswa().getStatusAwalMahasiswa();
			}
			if (statusAcuan == null && kegiatan.getMahasiswa() != null) {
				statusAcuan = kegiatan.getMahasiswa().getStatusAwalMahasiswa();
			}
			if (statusAcuan == null || statusAcuan.getId() == null
					|| !getStatusAwalMahasiswa().getId().equals(statusAcuan.getId())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Versi publik pengecekan rentang tanggal berlaku — dipakai mesin tagihan untuk rute
	 * Gelombang Pendaftaran / Jenis Seleksi milik CALON mahasiswa yang sebelumnya sama
	 * sekali tidak memeriksa tanggal (promo berbatas waktu tidak pernah berhenti sendiri).
	 *
	 * <p>Sekadar delegasi tipis ke {@link #cocokTanggalBerlaku(Kegiatan)} (yang bersifat
	 * {@code private}) supaya pemanggil di luar kelas ini (mis. {@code DetailKegiatan}/{@code
	 * Kegiatan}) bisa memakai logika rentang-tanggal yang sama persis tanpa menduplikasinya.</p>
	 *
	 * @param kegiatan kegiatan/tagihan acuan; boleh {@code null}
	 * @return {@code true} bila tanggal kegiatan berada dalam rentang {@link
	 *         #getTanggalMulaiBerlaku()}..{@link #getTanggalSampaiBerlaku()} jenis diskon ini
	 * @see #cocokTanggalBerlaku(Kegiatan)
	 */
	public boolean cocokTanggalBerlakuUntuk(Kegiatan kegiatan) {
		return cocokTanggalBerlaku(kegiatan);
	}

	/**
	 * Mengambil {@link HistoryStatusMahasiswa} (riwayat status per-semester) mahasiswa pada
	 * {@code kegiatan}, dipakai sebagai acuan Program/Status Awal yang lebih akurat pada {@link
	 * #cocokUntukKegiatan} — status mahasiswa bisa berubah antar semester (mis. pindah program),
	 * sehingga acuan "saat ini" pada entity {@code Mahasiswa} belum tentu mencerminkan status
	 * pada semester baris tagihan yang sedang dihitung.
	 *
	 * <p><b>Efek samping:</b> memanggil {@code Common.singkronkanKrsMahasiswa(...)}, yang bisa
	 * membuat/menyinkronkan baris {@link KrsMahasiswa} — berpotensi menulis ke database sekadar
	 * dari pemanggilan pengecekan kecocokan diskon. Exception ditelan dan dicatat ke {@link
	 * ais.common.ErrorAuditUtil}; kegagalan membuat method mengembalikan {@code null} (fallback
	 * ke acuan status "saat ini" di {@link #cocokUntukKegiatan}), bukan melempar exception.</p>
	 *
	 * @param kegiatan kegiatan yang menyediakan mahasiswa dan semester acuan
	 * @return riwayat status mahasiswa pada semester kegiatan, atau {@code null} bila gagal
	 */
	private HistoryStatusMahasiswa ambilHistoryStatusMahasiswa(Kegiatan kegiatan) {
		try {
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(kegiatan.getMahasiswa(), kegiatan.getSemster(),
					null, null, false);
			return HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa, false);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"JenisDiskonMahasiswa.ambilHistoryStatusMahasiswa");
			return null;
		}
	}

	/**
	 * @return item biaya sasaran diskon slot ke-2 pada level jenis diskon (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_2", nullable = true)
	public ItemBiaya getItemBiaya2() {
		itemBiaya2 = check(itemBiaya2);
		return itemBiaya2;
	}

	/**
	 * @param itemBiaya2 item biaya slot ke-2 baru; {@code null} untuk melepas tautan.
	 */
	public void setItemBiaya2(ItemBiaya itemBiaya2) {
		this.itemBiaya2 = itemBiaya2;
	}

	/**
	 * @return item biaya sasaran diskon slot ke-3 pada level jenis diskon (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_3", nullable = true)
	public ItemBiaya getItemBiaya3() {
		itemBiaya3 = check(itemBiaya3);
		return itemBiaya3;
	}

	/**
	 * @param itemBiaya3 item biaya slot ke-3 baru; {@code null} untuk melepas tautan.
	 */
	public void setItemBiaya3(ItemBiaya itemBiaya3) {
		this.itemBiaya3 = itemBiaya3;
	}

	/**
	 * @return item biaya sasaran diskon slot ke-4 pada level jenis diskon (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_4", nullable = true)
	public ItemBiaya getItemBiaya4() {
		itemBiaya4 = check(itemBiaya4);
		return itemBiaya4;
	}

	/**
	 * @param itemBiaya4 item biaya slot ke-4 baru; {@code null} untuk melepas tautan.
	 */
	public void setItemBiaya4(ItemBiaya itemBiaya4) {
		this.itemBiaya4 = itemBiaya4;
	}

	/**
	 * @return item biaya sasaran diskon slot ke-5 pada level jenis diskon (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_5", nullable = true)
	public ItemBiaya getItemBiaya5() {
		itemBiaya5 = check(itemBiaya5);
		return itemBiaya5;
	}

	/**
	 * @param itemBiaya5 item biaya slot ke-5 baru; {@code null} untuk melepas tautan.
	 */
	public void setItemBiaya5(ItemBiaya itemBiaya5) {
		this.itemBiaya5 = itemBiaya5;
	}

	/**
	 * Mengumpulkan semua item biaya slot 1&ndash;5 pada level jenis diskon ini yang tidak
	 * {@code null} ke dalam satu daftar.
	 *
	 * @return daftar item biaya yang tercakup oleh jenis diskon ini, urut slot 1 s.d. 5; tidak
	 *         pernah {@code null}, boleh kosong.
	 */
	public List<ItemBiaya> ambilItemBiayas() {
		List<ItemBiaya> itemBiayas = new ArrayList<ItemBiaya>();
		if (getItemBiaya() != null) {
			itemBiayas.add(itemBiaya);
		}
		if (getItemBiaya2() != null) {
			itemBiayas.add(itemBiaya2);
		}
		if (getItemBiaya3() != null) {
			itemBiayas.add(itemBiaya3);
		}
		if (getItemBiaya4() != null) {
			itemBiayas.add(itemBiaya4);
		}
		if (getItemBiaya5() != null) {
			itemBiayas.add(itemBiaya5);
		}
		return itemBiayas;
	}

	/**
	 * Sama seperti {@link #ambilItemBiayas()} tetapi mengembalikan ID-nya saja. Dipakai oleh
	 * {@link #cocokUntukTagihanGlobal} untuk menguji keanggotaan item biaya baris tagihan pada
	 * daftar Default Item Biaya I..V jenis diskon ini.
	 *
	 * @return daftar ID item biaya slot 1 s.d. 5 yang tidak {@code null}; tidak pernah {@code
	 *         null}, boleh kosong.
	 */
	public List<Long> ambilItemBiayaIds() {
		List<Long> itemBiayas = new ArrayList<Long>();
		if (getItemBiaya() != null) {
			itemBiayas.add(itemBiaya.getId());
		}
		if (getItemBiaya2() != null) {
			itemBiayas.add(itemBiaya2.getId());
		}
		if (getItemBiaya3() != null) {
			itemBiayas.add(itemBiaya3.getId());
		}
		if (getItemBiaya4() != null) {
			itemBiayas.add(itemBiaya4.getId());
		}
		if (getItemBiaya5() != null) {
			itemBiayas.add(itemBiaya5.getId());
		}
		return itemBiayas;
	}

}
