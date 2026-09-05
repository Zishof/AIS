package ais.database.model.recruitment;

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



import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.ujian_pegawai}: definisi/master "sesi ujian
 * seleksi" pada modul rekrutmen calon pegawai ({@code ais.database.model.recruitment}). Satu baris
 * mewakili satu ujian (mis. "Ujian Tahap 1 Gelombang Agustus") yang terikat ke satu {@link
 * GelombangPendaftaranPegawai} lewat {@link #getGelombangPendaftaranPegawai()}, dengan lokasi
 * pelaksanaan, teks informasi sebelum/sesudah pembayaran, serta hingga sepuluh tanggal pelaksanaan
 * ({@link #getTanggalUjian1()}..{@link #getTanggalUjian10()}) yang jumlah efektifnya dikendalikan
 * oleh {@link #getJumlahHariUjian()}.
 *
 * <p><b>PENTING — bukan entity skor/jawaban.</b> Kelas ini <b>tidak memiliki field nilai, skor,
 * jawaban, maupun status kelulusan</b>. Ia murni metadata jadwal & logistik ujian (nama, lokasi,
 * tanggal, teks pengumuman); pertanyaan apakah "skor ujian dihitung server-side atau dipercaya
 * dari klien" — pola kerentanan yang ditemukan pada kuis online {@code
 * kursus.JawabanPercobaanKuisKursus} (lihat {@code task_bee6756e}) — <b>tidak berlaku untuk
 * entity ini</b> karena tidak ada mekanisme kuis/jawaban/penilaian otomatis di modul rekrutmen
 * ini sama sekali. Pencarian menyeluruh terhadap {@code ais.action.master.recruitment} dan seluruh
 * paket {@code ais.database.model.recruitment} untuk kata kunci "skor"/"nilai"/"jawaban"/"hasil
 * ujian" tidak menemukan satu pun jalur kode yang menghitung atau menyimpan hasil ujian pegawai;
 * satu-satunya field bernama "skor" di modul ini adalah {@link
 * KelompokPendaftaranPegawai#getSkorSampai()}, sebuah ambang batas kelompok yang juga tidak dipakai
 * oleh alur kode manapun saat dokumentasi ini ditulis. Kesimpulannya, penilaian/kelulusan ujian
 * pegawai — bila memang diproses di sistem ini — dilakukan sepenuhnya manual/offline di luar
 * jangkauan entity JPA pada paket ini; tidak ditemukan input dari klien yang langsung menentukan
 * skor atau status lulus/tidak lulus pelamar.</p>
 *
 * <p><b>Relasi:</b> {@link #getGelombangPendaftaranPegawai()} adalah {@code @ManyToOne} opsional
 * ({@code nullable = true}) ke {@link GelombangPendaftaranPegawai} dengan {@code FetchMode.SELECT}
 * (query terpisah saat diakses, bukan join). Entity anak yang mereferensikan ujian ini (sisi
 * pemilik FK, tidak dideklarasikan di kelas ini): {@link JadwalUjianPegawai} (jadwal pelaksanaan
 * konkret, lewat kolom {@code ujian_pegawai}, wajib diisi di sana) dan {@link RuangPegawai}
 * (penempatan ruang, opsional).</p>
 *
 * <p><b>Pola sepuluh tanggal bercabang ({@link #getTanggalUjian1()}..{@link
 * #getTanggalUjian10()}):</b> setiap getter tanggal ke-N (N &ge; 2) memiliki efek samping
 * menuliskan {@code null} ke field in-memory bila {@link #getJumlahHariUjian()} kurang dari N —
 * bukan getter murni. Efeknya, tanggal hari ke-N yang sebelumnya tersimpan di database bisa
 * "hilang" dari sudut pandang objek in-memory begitu {@code jumlahHariUjian} diturunkan, meski
 * baris di database belum tentu ikut ter-null-kan sampai entity benar-benar di-flush ulang. Ini
 * pola sepuluh-kolom-tetap (bukan koleksi/tabel anak) yang sama dengan pola serupa di entity lain
 * pada codebase AIS untuk kasus "jumlah hari bervariasi tapi disimpan sebagai kolom tetap" — desain
 * ini membatasi ujian maksimal 10 hari pelaksanaan tanpa migrasi skema.</p>
 *
 * <p>Diaudit oleh Hibernate Envers ({@code @Audited}); setiap INSERT/UPDATE/DELETE tercatat ke
 * tabel revisi historis terpisah.</p>
 *
 * @see GelombangPendaftaranPegawai
 * @see JadwalUjianPegawai
 * @see RuangPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ujian_pegawai")



public class UjianPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deployment.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Primary key baris ini pada tabel {@code ujian_pegawai}, dihasilkan otomatis oleh database
	 * ({@code IDENTITY}). Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir membuat/mengubah baris ini. Lihat {@link #getOleh()}/
	 * {@link #setOleh(String)}.
	 */
	private String oleh;
	/**
	 * ID pengguna yang terakhir membuat/mengubah baris ini, pasangan dari {@link #oleh}. Lihat
	 * {@link #getOlehId()}/{@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna audit terakhir.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID pengguna audit. Menolak (no-op) nilai {@code null}/kosong-whitespace agar jejak
	 * "olehId" tidak pernah tertimpa kosong — pola audit-shadow-field yang berulang di seluruh
	 * entity AIS.
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)}: nilai
	 * {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna audit terakhir.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan timestamp audit ke {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum UPDATE
	 * dijalankan. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset timestamp perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah timestamp baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entity ini untuk keperluan tampilan/log (mis. pada dropdown pemilihan
	 * ujian di UI ZK) — hanya nama ujian, tanpa ID maupun detail lain.
	 *
	 * @return nilai {@link #nama} apa adanya (bisa {@code null} bila belum diisi).
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Nama ujian, ditampilkan ke publik/admin. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Lokasi pelaksanaan ujian (teks bebas). Lihat {@link #getLokasi()}.
	 */
	private String lokasi;
	/**
	 * Jumlah hari efektif pelaksanaan ujian (1–10), mengendalikan getter {@link
	 * #getTanggalUjian2()}..{@link #getTanggalUjian10()} yang mengembalikan {@code null} bila
	 * indeksnya melebihi nilai ini. Lihat {@link #getJumlahHariUjian()} — default 1.
	 */
	private Integer jumlahHariUjian = 1;
	/**
	 * Tanggal pelaksanaan ujian hari ke-1. Selalu aktif berapa pun {@link #jumlahHariUjian} (tidak
	 * ada guard seperti hari ke-2 dan seterusnya). Lihat {@link #getTanggalUjian1()}.
	 */
	private Date tanggalUjian1;
	/**
	 * Tanggal pelaksanaan ujian hari ke-2; hanya relevan bila {@link #jumlahHariUjian} &ge; 2.
	 * Lihat {@link #getTanggalUjian2()}.
	 */
	private Date tanggalUjian2;
	/**
	 * Tanggal pelaksanaan ujian hari ke-3; hanya relevan bila {@link #jumlahHariUjian} &ge; 3.
	 * Lihat {@link #getTanggalUjian3()}.
	 */
	private Date tanggalUjian3;
	/**
	 * Tanggal pelaksanaan ujian hari ke-4; hanya relevan bila {@link #jumlahHariUjian} &ge; 4.
	 * Lihat {@link #getTanggalUjian4()}.
	 */
	private Date tanggalUjian4;
	/**
	 * Tanggal pelaksanaan ujian hari ke-5; hanya relevan bila {@link #jumlahHariUjian} &ge; 5.
	 * Lihat {@link #getTanggalUjian5()}.
	 */
	private Date tanggalUjian5;
	/**
	 * Tanggal pelaksanaan ujian hari ke-6; hanya relevan bila {@link #jumlahHariUjian} &ge; 6.
	 * Lihat {@link #getTanggalUjian6()}.
	 */
	private Date tanggalUjian6;
	/**
	 * Tanggal pelaksanaan ujian hari ke-7; hanya relevan bila {@link #jumlahHariUjian} &ge; 7.
	 * Lihat {@link #getTanggalUjian7()}.
	 */
	private Date tanggalUjian7;
	/**
	 * Tanggal pelaksanaan ujian hari ke-8; hanya relevan bila {@link #jumlahHariUjian} &ge; 8.
	 * Lihat {@link #getTanggalUjian8()}.
	 */
	private Date tanggalUjian8;
	/**
	 * Tanggal pelaksanaan ujian hari ke-9; hanya relevan bila {@link #jumlahHariUjian} &ge; 9.
	 * Lihat {@link #getTanggalUjian9()}.
	 */
	private Date tanggalUjian9;
	/**
	 * Tanggal pelaksanaan ujian hari ke-10 (maksimum yang didukung skema kolom tetap ini). Lihat
	 * {@link #getTanggalUjian10()}.
	 */
	private Date tanggalUjian10;

	/**
	 * Gelombang pendaftaran yang menaungi ujian ini. Lihat {@link
	 * #getGelombangPendaftaranPegawai()}.
	 */
	private GelombangPendaftaranPegawai gelombangPendaftaranPegawai;
	/**
	 * Penanda apakah jadwal ujian ditampilkan pada kartu ujian yang dicetak/diunduh peserta. Lihat
	 * {@link #getTampilkanJadwalUjianDiKartuUjian()} — default {@code true}.
	 */
	private Boolean tampilkanJadwalUjianDiKartuUjian;
	/**
	 * Teks keterangan umum ujian ini, tanpa default (kolom tidak dipetakan {@code @Column}
	 * eksplisit dan tidak ada placeholder pada getter). Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;
	/**
	 * Teks header yang ditampilkan sebelum peserta mengisi data (mis. konfirmasi kebenaran data
	 * diri). Lihat {@link #getKeteranganHeader()}.
	 */
	private String keteranganHeader;
	/**
	 * Teks keterangan yang ditampilkan setelah peserta melakukan pembayaran (mis. daftar yang
	 * harus dibawa saat ujian). Lihat {@link #getKeteranganSetelahBayar()}.
	 */
	private String keteranganSetelahBayar;
	/**
	 * Teks header yang menyertai {@link #keteranganSetelahBayar}. Lihat {@link
	 * #getKeteranganSetelahBayarHeader()}.
	 */
	private String keteranganSetelahBayarHeader;

	/**
	 * Konstruktor kosong yang disyaratkan Hibernate/JPA untuk instansiasi lewat refleksi. Field
	 * lain (nama, lokasi, tanggal ujian, gelombang) harus diisi terpisah lewat setter.
	 */
	public UjianPegawai() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID ujian, atau {@code null} untuk instance transient.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengeset {@link #id}.
	 *
	 * @param id nilai baru untuk {@link #id}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama ujian, dengan whitespace di kedua ujung dipangkas ({@link String#trim()}).
	 *
	 * @return nama ujian yang sudah di-trim, atau {@code null} bila field belum diisi (trim tidak
	 * dipanggil pada nilai {@code null}).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset {@link #nama}.
	 *
	 * @param nama nilai baru untuk {@link #nama}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil teks keterangan umum ujian ini. <b>Efek samping ganda:</b> bila field {@link
	 * #keterangan} masih {@code null}, ditulis menjadi string kosong terlebih dahulu; lalu bila
	 * hasilnya (setelah trim) tetap kosong, ditulis ulang menjadi placeholder {@code "Informasi
	 * penerimaan pegawai baru tulis disini"}. Kedua langkah ini memodifikasi field in-memory
	 * sebelum nilai dikembalikan — bukan getter murni; placeholder yang dihasilkan berpotensi ikut
	 * tersimpan ke database bila entity kemudian di-flush, berbeda dengan pola placeholder
	 * "hitung ulang tanpa menulis field" yang dipakai getter teks lowongan di {@link
	 * GelombangPendaftaranPegawai}.
	 *
	 * @return teks keterangan (setelah dinormalisasi); tidak pernah {@code null} atau kosong
	 * setelah pemanggilan pertama.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}

		if (keterangan.trim().isEmpty()) {
			keterangan = "Informasi penerimaan pegawai baru tulis disini";
		}

		return this.keterangan;
	}

	/**
	 * Mengeset {@link #keterangan}.
	 *
	 * @param keterangan nilai baru untuk {@link #keterangan}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) jumlah hari efektif pelaksanaan ujian. <b>Efek
	 * samping:</b> menulis {@code 1} ke field bila masih {@code null}. Nilai ini menentukan berapa
	 * banyak dari sepuluh getter {@code getTanggalUjianN()} yang mengembalikan tanggal tersimpan
	 * vs. {@code null} — lihat catatan pola di Javadoc kelas.
	 *
	 * @return jumlah hari ujian; default {@code 1} bila belum pernah diset. Tidak ada validasi
	 * batas atas eksplisit di getter/setter (nilai &gt; 10 secara teknis bisa diset, tapi hanya
	 * hari ke-1 s/d ke-10 yang punya kolom tanggal, jadi hari di atas 10 tidak berpengaruh nyata
	 * pada tanggal manapun).
	 */
	public Integer getJumlahHariUjian() {
		if (jumlahHariUjian == null) {
			jumlahHariUjian = 1;
		}
		return jumlahHariUjian;
	}

	/**
	 * Mengeset {@link #jumlahHariUjian}.
	 *
	 * @param jumlahHariUjian nilai baru untuk {@link #jumlahHariUjian}.
	 */
	public void setJumlahHariUjian(Integer jumlahHariUjian) {
		this.jumlahHariUjian = jumlahHariUjian;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-1. Tidak ada guard {@link
	 * #getJumlahHariUjian()} di sini — hari pertama selalu dianggap berlaku selama ujian
	 * dilaksanakan minimal satu hari.
	 *
	 * @return tanggal hari pertama, atau {@code null} bila belum diisi.
	 */
	public Date getTanggalUjian1() {
		return tanggalUjian1;
	}

	/**
	 * Mengeset {@link #tanggalUjian1}.
	 *
	 * @param tanggalUjian1 nilai baru untuk {@link #tanggalUjian1}.
	 */
	public void setTanggalUjian1(Date tanggalUjian1) {
		this.tanggalUjian1 = tanggalUjian1;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-2. <b>Efek samping:</b> bila {@link
	 * #getJumlahHariUjian()} kurang dari 2, field {@link #tanggalUjian2} ditulis menjadi
	 * {@code null} sebelum dikembalikan — bukan getter murni. Nilai yang sebelumnya tersimpan di
	 * database untuk kolom ini bisa jadi masih ada sampai entity di-flush ulang; getter ini hanya
	 * "menyembunyikan" nilai dari sudut pandang in-memory saat jumlah hari diturunkan.
	 *
	 * @return tanggal hari ke-2, atau {@code null} bila {@link #getJumlahHariUjian()} &lt; 2 atau
	 * memang belum diisi.
	 */
	public Date getTanggalUjian2() {
		if (getJumlahHariUjian() < 2) {
			tanggalUjian2 = null;
		}
		return tanggalUjian2;
	}

	/**
	 * Mengeset {@link #tanggalUjian2}.
	 *
	 * @param tanggalUjian2 nilai baru untuk {@link #tanggalUjian2}.
	 */
	public void setTanggalUjian2(Date tanggalUjian2) {
		this.tanggalUjian2 = tanggalUjian2;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-3. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 3.
	 *
	 * @return tanggal hari ke-3, atau {@code null} bila jumlah hari &lt; 3 atau belum diisi.
	 */
	public Date getTanggalUjian3() {
		if (getJumlahHariUjian() < 3) {
			tanggalUjian3 = null;
		}
		return tanggalUjian3;
	}

	/**
	 * Mengeset {@link #tanggalUjian3}.
	 *
	 * @param tanggalUjian3 nilai baru untuk {@link #tanggalUjian3}.
	 */
	public void setTanggalUjian3(Date tanggalUjian3) {
		this.tanggalUjian3 = tanggalUjian3;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-4. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 4.
	 *
	 * @return tanggal hari ke-4, atau {@code null} bila jumlah hari &lt; 4 atau belum diisi.
	 */
	public Date getTanggalUjian4() {
		if (getJumlahHariUjian() < 4) {
			tanggalUjian4 = null;
		}
		return tanggalUjian4;
	}

	/**
	 * Mengeset {@link #tanggalUjian4}.
	 *
	 * @param tanggalUjian4 nilai baru untuk {@link #tanggalUjian4}.
	 */
	public void setTanggalUjian4(Date tanggalUjian4) {
		this.tanggalUjian4 = tanggalUjian4;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-5. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 5.
	 *
	 * @return tanggal hari ke-5, atau {@code null} bila jumlah hari &lt; 5 atau belum diisi.
	 */
	public Date getTanggalUjian5() {
		if (getJumlahHariUjian() < 5) {
			tanggalUjian5 = null;
		}
		return tanggalUjian5;
	}

	/**
	 * Mengeset {@link #tanggalUjian5}.
	 *
	 * @param tanggalUjian5 nilai baru untuk {@link #tanggalUjian5}.
	 */
	public void setTanggalUjian5(Date tanggalUjian5) {
		this.tanggalUjian5 = tanggalUjian5;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-6. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 6.
	 *
	 * @return tanggal hari ke-6, atau {@code null} bila jumlah hari &lt; 6 atau belum diisi.
	 */
	public Date getTanggalUjian6() {
		if (getJumlahHariUjian() < 6) {
			tanggalUjian6 = null;
		}
		return tanggalUjian6;
	}

	/**
	 * Mengeset {@link #tanggalUjian6}.
	 *
	 * @param tanggalUjian6 nilai baru untuk {@link #tanggalUjian6}.
	 */
	public void setTanggalUjian6(Date tanggalUjian6) {
		this.tanggalUjian6 = tanggalUjian6;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-7. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 7.
	 *
	 * @return tanggal hari ke-7, atau {@code null} bila jumlah hari &lt; 7 atau belum diisi.
	 */
	public Date getTanggalUjian7() {
		if (getJumlahHariUjian() < 7) {
			tanggalUjian7 = null;
		}
		return tanggalUjian7;
	}

	/**
	 * Mengeset {@link #tanggalUjian7}.
	 *
	 * @param tanggalUjian7 nilai baru untuk {@link #tanggalUjian7}.
	 */
	public void setTanggalUjian7(Date tanggalUjian7) {
		this.tanggalUjian7 = tanggalUjian7;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-8. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 8.
	 *
	 * @return tanggal hari ke-8, atau {@code null} bila jumlah hari &lt; 8 atau belum diisi.
	 */
	public Date getTanggalUjian8() {
		if (getJumlahHariUjian() < 8) {
			tanggalUjian8 = null;
		}
		return tanggalUjian8;
	}

	/**
	 * Mengeset {@link #tanggalUjian8}.
	 *
	 * @param tanggalUjian8 nilai baru untuk {@link #tanggalUjian8}.
	 */
	public void setTanggalUjian8(Date tanggalUjian8) {
		this.tanggalUjian8 = tanggalUjian8;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-9. Pola sama seperti {@link
	 * #getTanggalUjian2()}: field ditulis {@code null} bila {@link #getJumlahHariUjian()} &lt; 9.
	 *
	 * @return tanggal hari ke-9, atau {@code null} bila jumlah hari &lt; 9 atau belum diisi.
	 */
	public Date getTanggalUjian9() {
		if (getJumlahHariUjian() < 9) {
			tanggalUjian9 = null;
		}
		return tanggalUjian9;
	}

	/**
	 * Mengeset {@link #tanggalUjian9}.
	 *
	 * @param tanggalUjian9 nilai baru untuk {@link #tanggalUjian9}.
	 */
	public void setTanggalUjian9(Date tanggalUjian9) {
		this.tanggalUjian9 = tanggalUjian9;
	}

	/**
	 * Mengambil tanggal pelaksanaan ujian hari ke-10 (hari terakhir yang didukung skema kolom
	 * tetap ini). Pola sama seperti {@link #getTanggalUjian2()}: field ditulis {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 10.
	 *
	 * @return tanggal hari ke-10, atau {@code null} bila jumlah hari &lt; 10 atau belum diisi.
	 */
	public Date getTanggalUjian10() {
		if (getJumlahHariUjian() < 10) {
			tanggalUjian10 = null;
		}
		return tanggalUjian10;
	}

	/**
	 * Mengeset {@link #tanggalUjian10}.
	 *
	 * @param tanggalUjian10 nilai baru untuk {@link #tanggalUjian10}.
	 */
	public void setTanggalUjian10(Date tanggalUjian10) {
		this.tanggalUjian10 = tanggalUjian10;
	}

	/**
	 * Mengambil gelombang pendaftaran yang menaungi ujian ini. Relasi {@code @ManyToOne} opsional
	 * dengan {@code FetchMode.SELECT} (dimuat lewat query terpisah, bukan join, saat pertama
	 * diakses) — berbeda dari pola lazy-proxy-resolution ({@link
	 * ais.database.model.GeneralValueObject#check(Object)}) yang dipakai relasi lain di modul ini;
	 * di sini tidak ada pemanggilan {@code check(...)}, jadi proxy Hibernate dikembalikan apa
	 * adanya tanpa resolusi tambahan.
	 *
	 * @return {@link GelombangPendaftaranPegawai} pemilik ujian ini, atau {@code null} bila ujian
	 * tidak terikat gelombang manapun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gelombang_pendaftaran_pegawai", nullable = true)
	public GelombangPendaftaranPegawai getGelombangPendaftaranPegawai() {
		return gelombangPendaftaranPegawai;
	}

	/**
	 * Mengeset {@link #gelombangPendaftaranPegawai}.
	 *
	 * @param gelombangPendaftaranPegawai nilai baru untuk {@link #gelombangPendaftaranPegawai}.
	 */
	public void setGelombangPendaftaranPegawai(GelombangPendaftaranPegawai gelombangPendaftaranPegawai) {
		this.gelombangPendaftaranPegawai = gelombangPendaftaranPegawai;
	}

	/**
	 * Mengambil lokasi pelaksanaan ujian (teks bebas, tanpa pemetaan {@code @Column} eksplisit).
	 *
	 * @return teks lokasi, atau {@code null} bila belum diisi.
	 */
	public String getLokasi() {
		return lokasi;
	}

	/**
	 * Mengeset {@link #lokasi}.
	 *
	 * @param lokasi nilai baru untuk {@link #lokasi}.
	 */
	public void setLokasi(String lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) penanda apakah jadwal ujian ditampilkan pada kartu
	 * ujian. <b>Efek samping:</b> menulis {@code true} ke field bila masih {@code null}.
	 *
	 * @return {@code true} sebagai default bila belum pernah diset, atau nilai eksplisit yang
	 * tersimpan.
	 */
	public Boolean getTampilkanJadwalUjianDiKartuUjian() {
		if (tampilkanJadwalUjianDiKartuUjian == null) {
			tampilkanJadwalUjianDiKartuUjian = true;
		}
		return tampilkanJadwalUjianDiKartuUjian;
	}

	/**
	 * Mengeset {@link #tampilkanJadwalUjianDiKartuUjian}.
	 *
	 * @param tampilkanJadwalUjianDiKartuUjian nilai baru untuk {@link #tampilkanJadwalUjianDiKartuUjian}.
	 */
	public void setTampilkanJadwalUjianDiKartuUjian(Boolean tampilkanJadwalUjianDiKartuUjian) {
		this.tampilkanJadwalUjianDiKartuUjian = tampilkanJadwalUjianDiKartuUjian;
	}

	/**
	 * Mengambil teks keterangan yang ditampilkan setelah peserta melakukan pembayaran. <b>Efek
	 * samping ganda</b> seperti {@link #getKeterangan()}: {@code null} dinormalisasi ke string
	 * kosong, lalu string kosong (setelah trim) diisi placeholder {@code "1. Alat Tuilis\n2. Papan
	 * Ujian"} (perhatikan salah ketik "Tuilis" yang ikut tersalin apa adanya dari source asli —
	 * bukan kesalahan transkripsi dokumentasi ini).
	 *
	 * @return teks keterangan setelah bayar (setelah dinormalisasi); tidak pernah {@code null}
	 * atau kosong setelah pemanggilan pertama.
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganSetelahBayar() {
		if (keteranganSetelahBayar == null) {
			keteranganSetelahBayar = "";
		}
		if (keteranganSetelahBayar.trim().isEmpty()) {
			keteranganSetelahBayar = "1. Alat Tuilis\n2. Papan Ujian";
		}

		return keteranganSetelahBayar;
	}

	/**
	 * Mengeset {@link #keteranganSetelahBayar}.
	 *
	 * @param keteranganSetelahBayar nilai baru untuk {@link #keteranganSetelahBayar}.
	 */
	public void setKeteranganSetelahBayar(String keteranganSetelahBayar) {
		this.keteranganSetelahBayar = keteranganSetelahBayar;
	}

	/**
	 * Mengambil teks header yang menyertai keterangan sebelum peserta mengisi data. <b>Efek
	 * samping:</b> bila field masih {@code null}, ditulis placeholder {@code "Pastikan bahwa data
	 * dibawah ini adalah benar data diri anda."} sebelum dikembalikan.
	 *
	 * @return teks header keterangan; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganHeader() {
		if (keteranganHeader == null) {
			keteranganHeader = "Pastikan bahwa data dibawah ini adalah benar data diri anda.";
		}
		return keteranganHeader;
	}

	/**
	 * Mengeset {@link #keteranganHeader}.
	 *
	 * @param keteranganHeader nilai baru untuk {@link #keteranganHeader}.
	 */
	public void setKeteranganHeader(String keteranganHeader) {
		this.keteranganHeader = keteranganHeader;
	}

	/**
	 * Mengambil teks header yang menyertai {@link #getKeteranganSetelahBayar()}. <b>Efek
	 * samping:</b> sama seperti {@link #getKeteranganHeader()}, menulis placeholder identik
	 * ({@code "Pastikan bahwa data dibawah ini adalah benar data diri anda."}) ke field bila masih
	 * {@code null} — kedua header ini punya teks default yang persis sama meski konteks
	 * tampilannya berbeda (sebelum vs. setelah bayar).
	 *
	 * @return teks header keterangan setelah bayar; tidak pernah {@code null} setelah pemanggilan
	 * pertama.
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganSetelahBayarHeader() {
		if (keteranganSetelahBayarHeader == null) {
			keteranganSetelahBayarHeader = "Pastikan bahwa data dibawah ini adalah benar data diri anda.";
		}
		return keteranganSetelahBayarHeader;
	}

	/**
	 * Mengeset {@link #keteranganSetelahBayarHeader}.
	 *
	 * @param keteranganSetelahBayarHeader nilai baru untuk {@link #keteranganSetelahBayarHeader}.
	 */
	public void setKeteranganSetelahBayarHeader(String keteranganSetelahBayarHeader) {
		this.keteranganSetelahBayarHeader = keteranganSetelahBayarHeader;
	}

}
