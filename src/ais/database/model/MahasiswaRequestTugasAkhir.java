package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;

/**
 * Pengajuan judul &amp; pembimbing tugas akhir/skripsi oleh seorang mahasiswa — tahap yang terjadi
 * <b>SEBELUM</b> entity {@link Skripsi} resmi terbentuk.
 *
 * <p>Tabel {@code public.mahasiswa_request_tugas_akhir}. Satu baris = satu berkas pengajuan milik
 * satu {@link Mahasiswa} pada satu tahun akademik/semester. Isinya: sampai sepuluh alternatif judul
 * yang diusulkan mahasiswa, sampai enam slot dosen (pembimbing/penguji proposal), penilaian seminar
 * proposal, jadwal &amp; catatan seminar, serta tautan ke {@link Skripsi} bila pengajuan sudah naik
 * ke tahap sidang.</p>
 *
 * <h3>Posisi dalam alur tugas akhir</h3>
 * <ol>
 *   <li><b>Pengajuan</b> ({@link #REQUEST_STATUS}) — mahasiswa mengisi {@link #getJudul1()} sampai
 *   {@link #getJudul10()} sebanyak {@link #getJumlahJudul()} alternatif.</li>
 *   <li><b>Disetujui</b> ({@link #AKTIF_STATUS}) — prodi menetapkan judul terpilih
 *   ({@link #getJudul()}) dan dosen pembimbing ({@link #getDosen1()} dst.); saat itu
 *   {@link #getTanggalAwalBimbingan()}/{@link #getTanggalAkhirBimbingan()} terisi otomatis
 *   (rentang default 6 bulan).</li>
 *   <li><b>Seminar/Proses Bimbingan</b> ({@link #SEMINAR_STATUS}) — begitu ada
 *   {@link JadwalSeminarTugasAkhir} atau tanggal awal bimbingan, status naik sendiri; seminar
 *   proposal dinilai lewat {@link FormatNilaiProposalSkripsi} +
 *   {@link KomponenPenilaianProposalSkripsi} dan disimpan di kolom teks
 *   {@link #getDetailNilai()}.</li>
 *   <li><b>Sidang</b> ({@link #LULUS_STATUS}) — sebuah {@link Skripsi} dibuat dari pengajuan ini
 *   (lihat {@code AktifitasTugasAkhirHelper.initSidang}: judul, pembimbing 1-3, dan rentang
 *   bimbingan disalin dari sini, lalu {@code Skripsi.setMahasiswaRequestTugasAkhir(this)}).
 *   Setelah itu {@link #getStatus()} membaca {@code Skripsi.getTelahSidang()} untuk menentukan
 *   apakah statusnya {@code Sidang} atau turun lagi ke {@code Disetujui}/{@code Seminar}.</li>
 *   <li>Cabang lain: {@link #MENGULANG_STATUS} (wajib mengulang) dan {@link #GAGAL_STATUS}
 *   (ditolak).</li>
 * </ol>
 *
 * <h3>Tautan balik ke {@link Skripsi}: field {@code skr} — WAJIB DIPAHAMI</h3>
 * <p>Relasi antara pengajuan dan skripsi <b>disimpan dua arah dan tidak simetris</b>:</p>
 * <ul>
 *   <li>Arah <b>Skripsi &rarr; pengajuan</b> adalah relasi Hibernate biasa: kolom
 *   {@code skripsi.mahasiswa_request_tugas_akhir} ({@code @ManyToOne}).</li>
 *   <li>Arah <b>pengajuan &rarr; Skripsi</b> BUKAN foreign key. Id skripsi disimpan sebagai
 *   <b>teks</b> pada properti {@code skr}, yang dipetakan ke kolom yang bernama
 *   {@code filelocation} (lihat {@link #getSkr()} — nama kolomnya menyesatkan, isinya id skripsi,
 *   bukan lokasi berkas). {@link #ambilSkripsi()} memuat entity-nya lewat {@code ambilData(...)}
 *   dari cache/DB, bukan lewat join.</li>
 * </ul>
 * <p><b>Siapa yang mengisi {@code skr}?</b> Bukan kode pengajuan ini, melainkan getter di sisi
 * seberang: {@code Skripsi.getMahasiswaRequestTugasAkhir()} memanggil {@code setSkr(getId())} pada
 * instance ini setiap kali dipanggil. Jadi pembacaan yang tampak "hanya baca" di layar skripsi
 * <b>menulis</b> ke entity pengajuan — bila pengajuan masih terpasang pada session, Hibernate
 * menandainya kotor dan menerbitkan {@code UPDATE mahasiswa_request_tugas_akhir} pada flush
 * berikutnya. {@link #setSkr(String)} juga menulis salinan nilai itu ke berkas cache sisi aplikasi
 * lewat {@code put(nilai, "skr")}, dan {@link #getSkr()} MENGUTAMAKAN isi berkas tersebut di atas
 * nilai kolom database. Konsekuensi &amp; jebakan lengkapnya ditulis di
 * {@link #getSkr()}/{@link #setSkr(String)}.</p>
 *
 * <h3>Relasi utama</h3>
 * <ul>
 *   <li>{@link #getMahasiswa()} — pemilik pengajuan ({@code NOT NULL}).</li>
 *   <li>{@link #getDosen1()}..{@link #getDosen6()} — enam slot {@link Dosen}; makna tiap slot
 *   ditentukan label pada {@link FormatNilaiProposalSkripsi} ({@code getDosen1()}..
 *   {@code getDosen6()}), bukan hardcode.</li>
 *   <li>{@link #getFormatNilaiProposalSkripsi()} — master bobot/label penilaian seminar proposal;
 *   ikut menentukan slot dosen mana yang aktif.</li>
 *   <li>{@link #getJadwalSeminarTugasAkhir()} — jadwal seminar proposal.</li>
 *   <li>{@link #getDetailperkuliahan()} — baris KHS tempat nilai tugas akhir dikonversi.</li>
 *   <li>{@link #getTahapanPenyusunanTugasAkhir()} — tahapan/capaian pembelajaran yang sedang
 *   berjalan.</li>
 *   <li>Entity lain yang menunjuk ke sini: {@code Pertemuan} (baris bimbingan),
 *   {@code DataPunyaArtikel}, {@code ParameterTambahanPertemuan}, dan {@link Skripsi}.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Data pengajuan</b>: {@link #getJudul()}, {@link #getJudul1()}..{@link #getJudul10()},
 *   {@link #getJumlahJudul()}, {@link #getStatus()}, {@link #getSemester()},
 *   {@link #getTahunAkademik()}.</li>
 *   <li><b>Penetapan dosen</b>: {@link #getDosen1()}..{@link #getDosen6()},
 *   {@link #simpanDosen(Dosen, String)}, {@link #dataDosen(boolean)},
 *   {@link #checkMaksSksDosen(Dosen, String, String, Integer)}.</li>
 *   <li><b>Penilaian seminar proposal</b>: {@link #getDetailNilai()},
 *   {@link #populateDetailNilai(KomponenPenilaianProposalSkripsi, Dosen, Double, Boolean)},
 *   {@link #retreiveDetailNilai(KomponenPenilaianProposalSkripsi, Dosen)},
 *   {@link #retreiveDetailVerifikasiNilai(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi, Dosen)},
 *   {@link #hitungTotalNilai(Boolean, Dosen)}, {@link #cariNilaiDariDosen(Dosen, String, Boolean)},
 *   {@link #bersihkanNilaiKeDefault()}, {@link #refreshNilaiKeDefault(Dosen)},
 *   {@link #reloadProposalSkripsiPunyaKomponenPenilaianProposalSkripsi(Session, Dosen)},
 *   {@link #getNilaiDosen1()}..{@link #getNilaiDosen6()}, {@link #getTotalNilai()},
 *   {@link #getNilaiHuruf()}, {@link #getLulus()}.</li>
 *   <li><b>Seminar &amp; berita acara</b>: {@link #getTanggalSeminar()},
 *   {@link #getWaktuSeminar()}, {@link #getWaktuSampaiSeminar()}, {@link #getCatatanSeminar()},
 *   {@link #getCatatanDosen()}, {@link #getTanpaPerbaikan()}, {@link #getLokasiUjian()},
 *   {@link #getNoSk()}, {@link #getTglSk()}.</li>
 *   <li><b>Jembatan ke skripsi</b>: {@link #ambilSkripsi()}, {@link #getSkr()},
 *   {@link #setSkr(String)}.</li>
 *   <li><b>Kontrak {@link VOPembelajaran}/{@link VOPesertaPembelajaran}</b>:
 *   {@link #getCourse()}, {@link #getUrutkanotomatis()}, {@link #ambilVOPembelajaran()},
 *   {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang harus diketahui sebelum menyentuh kelas ini</h3>
 * <ol>
 *   <li><b>Banyak getter menulis ke state (lazy default), bukan sekadar membaca.</b>
 *   {@link #getSemester()}, {@link #getTahunAkademik()}, {@link #getStatus()},
 *   {@link #getJudul()}, {@link #getTanggalAwalBimbingan()}, {@link #getTanggalAkhirBimbingan()},
 *   {@link #getWaktuSeminar()}, {@link #getWaktuSampaiSeminar()}, {@link #getCatatanSeminar()},
 *   {@link #getTotalNilai()}, {@link #getNilaiHuruf()}, {@link #getLulus()} dan
 *   {@link #getSkr()} semuanya menugaskan nilai ke field-nya sendiri. Pada entity yang masih
 *   terpasang session, membaca berarti mengotori entity dan berujung {@code UPDATE} saat flush.
 *   Ini pola universal di seluruh model AIS, bukan kekhususan kelas ini.</li>
 *   <li><b>{@link #getDosen1()}..{@link #getDosen6()} bisa MENGHAPUS penetapan dosen.</b> Bila
 *   slot yang bersangkutan dinonaktifkan pada {@link FormatNilaiProposalSkripsi}, getter menyetel
 *   field dosen menjadi {@code null} — dan nilai {@code null} itu ikut tersimpan ke kolom
 *   {@code dosenN} pada flush berikutnya. Mengubah master format nilai karenanya dapat
 *   menghapus data dosen pengajuan lama secara diam-diam.</li>
 *   <li><b>Field bayangan (shadowing).</b> {@code id}, {@code oleh}, {@code olehId},
 *   {@code tanggal_dirubah}, {@code nama}, {@code keterangan} dan {@code disposisiSop}
 *   dideklarasikan ulang di kelas ini padahal induknya
 *   ({@link ais.database.model.GeneralValueObject} dan {@link VOPembelajaran}) sudah punya field
 *   dan accessor bernama sama. Karena Hibernate pada model ini memetakan lewat <i>property
 *   access</i> (anotasi di getter), pemetaan tetap benar; tapi setiap kode yang menyentuh field
 *   induk secara langsung (mis. lewat refleksi atau dari method induk yang tidak di-override)
 *   akan melihat salinan induk yang selamanya {@code null}. Pola ini konsisten ditemukan di
 *   seluruh entity AIS yang sudah didokumentasikan.</li>
 *   <li><b>{@code getDisposisiSop()}/{@code setDisposisiSop()} adalah duplikat verbatim</b> dari
 *   {@link VOPembelajaran}; kehadirannya di sini hanya menambah field bayangan, tidak mengubah
 *   perilaku.</li>
 *   <li><b>Kolom {@code detail_nilai} bukan relasi.</b> Rincian nilai seminar proposal disimpan
 *   sebagai satu string CSV bertingkat
 *   ({@code komponenId,dosenId,nilai,0,bobot,verifikasi} dipisah {@code ;}) — lihat
 *   {@link #getDetailNilai()}. Semua parsing dilakukan manual dan setiap baris cacat ditelan
 *   diam-diam.</li>
 * </ol>
 *
 * <p>Kontrak umum {@code id}/{@code equals}/{@code compareTo}/{@code check(...)}/{@code put(...)}/
 * {@code retreive(...)} diwarisi dari {@link ais.database.model.GeneralValueObject} — baca di sana,
 * jangan diduplikasi di sini.</p>
 *
 * @see Skripsi
 * @see Mahasiswa
 * @see FormatNilaiProposalSkripsi
 * @see JadwalSeminarTugasAkhir
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "mahasiswa_request_tugas_akhir")
public class MahasiswaRequestTugasAkhir extends VOPembelajaran implements VOPesertaPembelajaran {

	/** Status awal: judul sudah diusulkan mahasiswa, belum disetujui prodi. Nilai default field. */
	public static final String REQUEST_STATUS = "Pengajuan";
	/** Status: judul &amp; pembimbing sudah ditetapkan, bimbingan boleh berjalan. */
	public static final String AKTIF_STATUS = "Disetujui";
	/** Status: sudah punya jadwal seminar proposal atau sudah masuk masa bimbingan. */
	public static final String SEMINAR_STATUS = "Seminar/Proses Bimbingan";
	/**
	 * Status: sudah menghasilkan {@link Skripsi} yang telah disidangkan
	 * ({@code Skripsi.getTelahSidang() == 1}). Perhatikan teksnya "Sidang", bukan "Lulus" —
	 * nama konstanta dan nilainya tidak sejalan.
	 */
	public static final String LULUS_STATUS = "Sidang";
	/** Status: pengajuan harus diulang. */
	public static final String MENGULANG_STATUS = "Mengulang";
	/** Status akhir negatif: pengajuan ditolak. */
	public static final String GAGAL_STATUS = "Ditolak";
	/**
	 * Teks catatan berita acara seminar versi LAMA. Dipakai sebagai penanda migrasi-saat-baca oleh
	 * {@link #getCatatanSeminar()}: bila isi tersimpan persis sama dengan teks ini, catatan
	 * ditimpa {@link #CATATAN_SEMINAR_DEFAULT}.
	 */
	private static final String CATATAN_SEMINAR_DEFAULT_LAMA = "1. Lama Perbaikan\t: .. hari (Maks. 15 hari)\n"
			+ "2. Jika lebih dari 15 hari s/d 1 (satu) bulan dikenakan sanksi berupa ....\n"
			+ "3. Jika lebih dari 3 (tiga) bulan dari tanggal ujian maka hasil ujian dibatalkan dan wajib mengajukan judul dan pembimbing baru.";
	/**
	 * Teks catatan berita acara seminar versi BERLAKU. Diisikan otomatis oleh
	 * {@link #getCatatanSeminar()} bila catatan masih kosong atau masih memakai teks versi lama.
	 */
	private static final String CATATAN_SEMINAR_DEFAULT = "Batas waktu penyelesaian perbaikan naskah Tugas Akhir adalah maksimal 14 (empat belas) hari kerja, "
			+ "terhitung sejak hari ini tanggal ................ sampai dengan tanggal ................ Hari kerja dihitung dari Senin sampai Sabtu, "
			+ "tidak termasuk hari Minggu, libur nasional dan/atau cuti bersama.\n\n"
			+ "Apabila mahasiswa terlambat menyelesaikan perbaikan melewati batas waktu yang telah ditentukan di berita acara, "
			+ "maka nilai Tugas Akhir akan diturunkan satu tingkat.\n\n"
			+ "Apabila melewati 2 (dua) minggu dari batas waktu yang telah dituliskan di berita acara maka mahasiswa wajib "
			+ "mengikuti ujian ulang sesuai dengan ketentuan dari Akademik.";

	/**
	 * Versi serialisasi. Entity ini ikut terserialisasi (mis. ke sesi ZK/cluster), jadi jangan
	 * diubah kecuali memang berniat memutus kompatibilitas data lama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Primary key. <b>Field bayangan</b>: {@link ais.database.model.GeneralValueObject} sudah punya
	 * {@code id} privat sendiri; yang dipetakan Hibernate adalah field ini lewat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. <b>Field bayangan</b> atas {@code oleh} milik
	 * {@link ais.database.model.GeneralValueObject}.
	 */
	private String oleh;
	/**
	 * Id pengguna pengubah terakhir. <b>Field bayangan</b> atas {@code olehId} milik
	 * {@link ais.database.model.GeneralValueObject}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see ais.database.model.GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong <b>diabaikan diam-diam</b>
	 * agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan tanpa sesi login (batch,
	 * penjadwal). Salinan persis perilaku
	 * {@link ais.database.model.GeneralValueObject#setOlehId(String)}.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjaga yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA ({@code @PreUpdate}): menyegarkan stempel audit sebelum setiap
	 * {@code UPDATE} lewat {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Implementasi wajib dari method {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}. Dipanggil oleh provider JPA, tidak pernah
	 * dipanggil langsung dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir; diinisialisasi ke jam server aplikasi
	 * ({@code WaktuUtil.getDate()}), bukan jam database. <b>Field bayangan</b> atas
	 * {@code tanggal_dirubah} milik {@link ais.database.model.GeneralValueObject}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (dipetakan sebagai {@code TIMESTAMP},
	 * jam ikut tersimpan).
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@code id} dan {@code nama} <b>secara langsung</b> (bukan lewat getter),
	 * sehingga aman dipanggil dari proxy yang belum terinisiasi tanpa memicu query, tetapi juga
	 * bisa mengembalikan {@code "null-null"} pada object yang belum tersimpan.</p>
	 *
	 * @return gabungan id dan nama pengajuan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Pengguna yang sedang mengunci baris ini; {@code null} berarti tidak terkunci. */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang sedang mengunci baris ini (mekanisme kunci edit {@link VoKunci}).
	 * Nilai dilewatkan {@code check(...)} sehingga proxy lazy yang session-nya sudah mati akan
	 * dimuat ulang lebih dulu.
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak sedang dikunci
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Mengunci/membuka kunci baris ini.
	 *
	 * @param dikunci pengguna pengunci; {@code null} untuk melepas kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/** Mahasiswa pemilik pengajuan; kolom {@code mahasiswa}, {@code NOT NULL}. */
	private Mahasiswa mahasiswa;
	/** Semester tempuh mahasiswa saat pengajuan; diisi otomatis oleh {@link #getSemester()}. */
	private Integer semester;
	/** Tahun akademik pengajuan; diisi otomatis oleh {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/**
	 * Nama/label pengajuan (biasanya nama kegiatan atau nama mahasiswa). <b>Field bayangan</b> atas
	 * {@code nama} milik {@link ais.database.model.GeneralValueObject}.
	 */
	private String nama;
	/**
	 * Keterangan bebas. <b>Field bayangan</b> atas {@code keterangan} milik
	 * {@link ais.database.model.GeneralValueObject}.
	 */
	private String keterangan;
	/** Status alur pengajuan; salah satu konstanta {@code *_STATUS}. Default {@link #REQUEST_STATUS}. */
	private String status = REQUEST_STATUS;
	/** Banyaknya alternatif judul yang boleh/harus diisi mahasiswa; default 1. */
	private Integer jumlahJudul;
	/** Judul terpilih (hasil persetujuan). Lihat {@link #getJudul()}. */
	private String judul;
	/** Alternatif judul ke-1 yang diusulkan mahasiswa. */
	private String judul1;
	/** Alternatif judul ke-2. */
	private String judul2;
	/** Alternatif judul ke-3. */
	private String judul3;
	/** Alternatif judul ke-4. */
	private String judul4;
	/** Alternatif judul ke-5. */
	private String judul5;
	/** Alternatif judul ke-6. */
	private String judul6;
	/** Alternatif judul ke-7. */
	private String judul7;
	/** Alternatif judul ke-8. */
	private String judul8;
	/** Alternatif judul ke-9. */
	private String judul9;
	/** Alternatif judul ke-10. */
	private String judul10;
	/** Slot dosen ke-1; makna (pembimbing/penguji) ditentukan label {@link FormatNilaiProposalSkripsi}. */
	private Dosen dosen1;
	/** Slot dosen ke-2. */
	private Dosen dosen2;
	/** Slot dosen ke-3. */
	private Dosen dosen3;
	/** Slot dosen ke-4. */
	private Dosen dosen4;
	/** Slot dosen ke-5. */
	private Dosen dosen5;
	/** Slot dosen ke-6. */
	private Dosen dosen6;

	/** Penanda baris masih aktif/berlaku; default {@code true} bila belum diisi. */
	private Boolean aktif;

	/**
	 * Memeriksa apakah seorang dosen sudah melampaui kuota jumlah pengujian/bimbingan proposal pada
	 * satu tahun akademik + paritas semester, dan bila ya menampilkan peringatan modal ke pengguna.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Batas diambil dari konfigurasi
	 *   {@code maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester} (default {@code 50}).
	 *   <b>Perhatian</b>: {@code Common.getKonfigurasi(...)} akan MENULIS nilai default ke tabel
	 *   konfigurasi bila kuncinya belum ada.</li>
	 *   <li>Menghitung banyaknya baris {@code MahasiswaRequestTugasAkhir} yang memakai dosen ini di
	 *   salah satu dari enam slot {@code dosen1..dosen6}, pada {@code tahunAkademik} yang sama dan
	 *   pada semester dengan paritas ganjil/genap yang sama (dibandingkan lewat
	 *   {@code this_.semester % 2}).</li>
	 *   <li>Bila {@code batas < (tambahanMengajar + jumlah)}, sebuah {@link MyMessageboxConfig}
	 *   peringatan ditampilkan dan method mengembalikan {@code true}.</li>
	 * </ol>
	 *
	 * <h4>Catatan &amp; jebakan</h4>
	 * <ul>
	 *   <li>Parameter {@code tahunAkademik} atau {@code semester} yang {@code null} tidak berarti
	 *   "abaikan filter" melainkan {@code Restrictions.sqlRestriction("false")} — hasil hitungnya
	 *   pasti 0, sehingga pemeriksaan selalu lulus. Sengaja permisif.</li>
	 *   <li>Pemeriksaan {@code dosen == null} dilakukan dua kali; pengecekan kedua (saat membangun
	 *   {@code criterion}) sudah tidak mungkin tercapai karena method sudah {@code return false} di
	 *   awal.</li>
	 *   <li>Method ini menempel pada UI (menampilkan messagebox) dan mencetak ke
	 *   {@code System.out} — jangan dipakai dari proses batch/servlet tanpa desktop ZK.</li>
	 *   <li>Memakai {@code HibernateUtil.currentSession()} (session thread-local) dan
	 *   <b>tidak menutupnya</b>; pemanggil yang memegang session bertanggung jawab.</li>
	 *   <li>Kembaran fungsi ini ada di {@link Skripsi#checkMaksSksDosen(Dosen, String, String, Integer)}
	 *   dengan tabel berbeda.</li>
	 * </ul>
	 *
	 * <p>Dipanggil dari {@code MahasiswaRequestTugasAkhirAction} saat memvalidasi enam slot dosen
	 * sebelum pengajuan disimpan.</p>
	 *
	 * @param dosen             dosen yang diperiksa; {@code null} langsung dianggap lolos
	 * @param tahunAkademik     tahun akademik pembanding; {@code null} membuat hitungan menjadi 0
	 * @param semester          {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP} sebagai penentu
	 *                          paritas; {@code null} membuat hitungan menjadi 0
	 * @param tambahanMengajar  banyaknya penugasan baru yang hendak ditambahkan
	 * @return {@code true} bila kuota terlampaui (dan peringatan sudah ditampilkan), {@code false}
	 *         bila masih boleh
	 */
	public static boolean checkMaksSksDosen(Dosen dosen, String tahunAkademik, String semester,
			Integer tambahanMengajar) {
		if (dosen == null) {
			return false;
		}

		int maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester = 50;
		try {
			maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester = Integer
					.parseInt(Common.getKonfigurasi("maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester", "50")
							.getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaRequestTugasAkhir.java:161");

		}

		Session session = HibernateUtil.currentSession();

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("false")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));

		Number q = ((Number) session.createCriteria(MahasiswaRequestTugasAkhir.class)

				.add(criterion)
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("tahunAkademik", tahunAkademik))

				.add(semester == null ? Restrictions.sqlRestriction("false")
						: Restrictions.sqlRestriction(
								"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")))

				.setProjection(Projections.rowCount()).uniqueResult());

		int jumlahMengajar = q == null ? 0 : q.intValue();

		System.out.println("dosen => " + dosen + ", tahunAkademik => " + tahunAkademik + ", semester => " + semester
				+ ", maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester => "
				+ maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester + ", jumlahMengajar => " + jumlahMengajar);

		boolean hasil = maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester < (tambahanMengajar
				+ jumlahMengajar);

		if (hasil) {
			try {
				MyMessageboxConfig.show(
						"Dosen dengan nama " + dosen.getNama() + " telah menguji di tahun akademik " + tahunAkademik
								+ " semester " + semester + " sebanyak " + jumlahMengajar
								+ " pengujian. Anda tidak bisa menambah " + tambahanMengajar
								+ " pengujian lagi, karena maksimal jumlah pengujian yang diajar oleh dosen adalah "
								+ maksimal_penguji_sidang_dosen_mengajar_dalam_satu_semester,
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return hasil;
		}

		return false;
	}

	/** Nilai akhir seminar proposal dari dosen slot 1. Lihat {@link #getNilaiDosen1()}. */
	private Double nilaiDosen1;
	/** Nilai akhir seminar proposal dari dosen slot 2. */
	private Double nilaiDosen2;
	/** Nilai akhir seminar proposal dari dosen slot 3. */
	private Double nilaiDosen3;
	/** Nilai akhir seminar proposal dari dosen slot 4. */
	private Double nilaiDosen4;
	/** Nilai akhir seminar proposal dari dosen slot 5. */
	private Double nilaiDosen5;
	/** Nilai akhir seminar proposal dari dosen slot 6. */
	private Double nilaiDosen6;

	/** Master bobot &amp; label penilaian seminar proposal yang dipakai pengajuan ini. */
	private FormatNilaiProposalSkripsi formatNilaiProposalSkripsi;
	/** Tahapan/capaian penyusunan tugas akhir yang sedang berjalan. */
	private TahapanAtauCapaianPembelajaran tahapanPenyusunanTugasAkhir;

	/**
	 * Format nilai perkuliahan (bukan format nilai proposal). Terpetakan ke kolom
	 * {@code format_nilai}, namun perhitungan nilai di kelas ini seluruhnya memakai
	 * {@link #formatNilaiProposalSkripsi} — field ini hanya data pendamping.
	 */
	private FormatNilai formatNilai;

	/** Tanggal mulai masa bimbingan; diisi otomatis oleh {@link #getTanggalAwalBimbingan()}. */
	private Date tanggalAwalBimbingan;
	/** Tanggal akhir masa bimbingan; diisi otomatis (+6 bulan) oleh {@link #getTanggalAkhirBimbingan()}. */
	private Date tanggalAkhirBimbingan;
	/** Tanggal pelaksanaan seminar proposal. */
	private Date tanggalSeminar;
	/** Jam mulai seminar (teks {@code HH:mm}); diisi otomatis oleh {@link #getWaktuSeminar()}. */
	private String waktuSeminar;
	/** Jam selesai seminar (teks {@code HH:mm}); diisi otomatis oleh {@link #getWaktuSampaiSeminar()}. */
	private String waktuSampaiSeminar;
	/** Catatan/ketentuan perbaikan pada berita acara seminar. Lihat {@link #getCatatanSeminar()}. */
	private String catatanSeminar;
	/** Catatan per dosen dalam bentuk JSON. Lihat {@link #getCatatanDosen()}. */
	private String catatanDosen;
	/** Penanda hasil seminar tanpa perbaikan; default {@code false}. */
	private Boolean tanpaPerbaikan;

	/** Jadwal seminar proposal; keberadaannya menaikkan status ke {@link #SEMINAR_STATUS}. */
	private JadwalSeminarTugasAkhir jadwalSeminarTugasAkhir;
	/** Baris KHS tempat nilai tugas akhir dikonversi. */
	private Detailperkuliahan detailperkuliahan;
	/**
	 * Rincian nilai seminar proposal dalam satu string CSV bertingkat. Format satu entri:
	 * {@code komponenId,dosenId,nilai,0,bobot,verifikasi}, antar entri dipisah {@code ;}.
	 */
	private String detailNilai;
	/** Kelulusan seminar proposal; diturunkan dari {@link #getNilaiHuruf()} oleh {@link #getLulus()}. */
	private Boolean lulus;
	/** Bobot IP hasil konversi nilai; default 0.0 lewat getter. */
	private Double totalIP;
	/** Nilai huruf hasil konversi {@link #getTotalNilai()}; dihitung ulang tiap {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;
	/** Nilai angka akhir gabungan enam slot dosen; dihitung ulang tiap {@link #getTotalNilai()}. */
	private Double totalNilai = 0.0;
	/** Jenis penjadwalan pertemuan bimbingan; default {@code "Mingguan"}. */
	private String jenis;
	/** Penanda penjadwalan melewati tanggal merah nasional; default {@code true}. */
	private Boolean lewatiTanggalMerahNasional;

	/** Konstruktor tanpa argumen yang diperlukan Hibernate. Tidak mengisi apa pun. */
	public MahasiswaRequestTugasAkhir() {
	}

	/**
	 * Mengembalikan primary key baris pengajuan ini.
	 *
	 * @return id, atau {@code null} bila belum tersimpan
	 * @see ais.database.model.GeneralValueObject#getId()
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Dipakai Hibernate saat hidrasi; jangan dipanggil manual pada entity
	 * yang sudah terpasang session.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/label pengajuan, sudah di-{@code trim()}.
	 *
	 * @return nama tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/label pengajuan.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas apa adanya (tanpa {@code trim}).
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan semester tempuh mahasiswa untuk pengajuan ini.
	 *
	 * <p><b>Getter berefek samping</b>: bila field masih {@code null}, nilainya diisi dari
	 * {@code Mahasiswa.currentSemester()} (semester berjalan mahasiswa saat ini), atau {@code 0}
	 * bila mahasiswa juga belum terisi. Nilai hasil pengisian itu menempel pada entity dan akan
	 * ikut ter-{@code UPDATE} saat flush — jadi semester pengajuan lama bisa "berpindah" ke
	 * semester berjalan bila barisnya pernah dibaca tanpa nilai semester.</p>
	 *
	 * <p>Blok yang mengambil semester dari {@link #getDetailperkuliahan()} sengaja dinonaktifkan
	 * (dikomentari) di kode — jangan dihidupkan tanpa memeriksa dampaknya ke laporan.</p>
	 *
	 * @return semester tempuh; tidak pernah {@code null}
	 */
	@Column(name = "semester", nullable = true)
	public Integer getSemester() {
		if (semester == null && mahasiswa != null) {
			semester = mahasiswa.currentSemester();
		}
		if (semester == null) {
			semester = 0;
		}

		// if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan()
		// != null) {
		// semester = detailperkuliahan.getSemester();
		// }

		return semester;
	}

	/**
	 * Menyetel semester tempuh secara eksplisit (menonaktifkan pengisian otomatis
	 * {@link #getSemester()} selama nilainya bukan {@code null}).
	 *
	 * @param semester semester tempuh
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan jenis penjadwalan pertemuan bimbingan; {@code "Mingguan"} bila belum diisi.
	 * Tidak menyimpan default ke field (murni baca).
	 *
	 * @return jenis penjadwalan; tidak pernah {@code null}
	 */
	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	/**
	 * Menyetel jenis penjadwalan pertemuan bimbingan.
	 *
	 * @param jenis jenis penjadwalan (mis. {@code "Mingguan"})
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Menyatakan apakah penjadwalan pertemuan harus melompati tanggal merah nasional; {@code true}
	 * bila belum diisi. Tidak menyimpan default ke field.
	 *
	 * @return {@code true} bila tanggal merah dilewati
	 */
	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	/**
	 * Menyetel apakah penjadwalan melompati tanggal merah nasional.
	 *
	 * @param lewatiTanggalMerahNasional {@code true} untuk melewati tanggal merah
	 */
	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	/**
	 * Mengembalikan mahasiswa pemilik pengajuan ini (kolom {@code mahasiswa}, {@code NOT NULL}).
	 *
	 * <p>Nilai dilewatkan {@code check(...)} lebih dulu, sehingga proxy lazy yang session-nya sudah
	 * tertutup akan dimuat ulang dari cache/DB alih-alih melempar
	 * {@code LazyInitializationException}. Pakai getter ini — <b>jangan</b> field {@code mahasiswa}
	 * mentah — dari jalur yang mungkin berjalan di luar session asli (cetak laporan/transkrip
	 * lewat refleksi); lihat catatan di {@link #getNilaiHuruf()}.</p>
	 *
	 * @return mahasiswa pemilik pengajuan
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pemilik pengajuan.
	 *
	 * @param mahasiswa mahasiswa pemilik
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan tahun akademik pengajuan.
	 *
	 * <p><b>Getter berefek samping</b>: bila masih {@code null}, field diisi dengan
	 * {@code Common.getCurrentTahunAkademik()} (tahun akademik berjalan) dan nilai itu akan ikut
	 * tersimpan saat flush. Baris lama tanpa tahun akademik karenanya akan "menempel" ke tahun
	 * akademik saat pertama kali dibaca, bukan tahun aslinya.</p>
	 *
	 * @return tahun akademik; tidak pernah {@code null} bila konfigurasi tahun berjalan tersedia
	 */
	@Column(name = "tahun_akademik", nullable = true)
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}

		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik pengajuan secara eksplisit.
	 *
	 * @param tahunAkademik tahun akademik (format master AIS, mis. {@code "2025/2026"})
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan status alur pengajuan, sekaligus <b>menaikkan/menurunkannya sendiri</b>
	 * berdasarkan keadaan data terkait. Inilah mesin status utama entity ini.
	 *
	 * <h4>Aturan yang dijalankan, berurutan</h4>
	 * <ol>
	 *   <li>Status {@code null} dinormalkan ke {@link #REQUEST_STATUS}.</li>
	 *   <li>Bila sudah ada {@link JadwalSeminarTugasAkhir} <i>atau</i> tanggal awal bimbingan
	 *   jatuh setelah hari ini, dan status sekarang {@link #AKTIF_STATUS}/{@link #REQUEST_STATUS},
	 *   status dinaikkan ke {@link #SEMINAR_STATUS}.</li>
	 *   <li>Bila {@link #ambilSkripsi()} menemukan {@link Skripsi} dengan
	 *   {@code getTelahSidang() == 1}, status menjadi {@link #LULUS_STATUS} ({@code "Sidang"}).</li>
	 *   <li>Bila skripsi ada tetapi belum sidang ({@code telahSidang == 0}), status dikembalikan
	 *   ke {@link #SEMINAR_STATUS} (bila ada jadwal/tanggal bimbingan) atau diturunkan dari
	 *   {@link #SEMINAR_STATUS} kembali ke {@link #AKTIF_STATUS}.</li>
	 * </ol>
	 *
	 * <h4>Efek samping &amp; jebakan</h4>
	 * <ul>
	 *   <li>Menulis ke field {@code status}, {@code jadwalSeminarTugasAkhir}, dan — lewat
	 *   {@link #getTanggalAwalBimbingan()} — juga ke {@code tanggalAwalBimbingan}. Membaca status
	 *   pada entity terpasang session dapat menerbitkan {@code UPDATE}.</li>
	 *   <li>Memanggil {@link #ambilSkripsi()}, yang berarti bisa memicu pembacaan database
	 *   (cache-miss) di dalam sebuah getter.</li>
	 *   <li>Kondisi "sudah masuk masa bimbingan" diuji dengan
	 *   {@code getTanggalAwalBimbingan().after(sekarang)} — yaitu tanggal awal bimbingan masih di
	 *   MASA DEPAN. Secara harfiah ini berarti "bimbingan belum mulai", jadi pembacaan literalnya
	 *   berlawanan dengan maksud yang tersirat. Perilaku ini dibiarkan apa adanya karena sudah
	 *   dipakai konsisten oleh layar/laporan yang ada.</li>
	 *   <li>Seluruh galat pada blok skripsi ditelan (dicetak + dicatat ke audit); status tetap
	 *   dikembalikan.</li>
	 *   <li>Status {@link #MENGULANG_STATUS} dan {@link #GAGAL_STATUS} tidak pernah dihasilkan
	 *   otomatis di sini — hanya lewat {@link #setStatus(String)} dari layar.</li>
	 * </ul>
	 *
	 * @return status terkini; tidak pernah {@code null}
	 */
	public String getStatus() {
		if (status == null) {
			status = REQUEST_STATUS;
		}

		jadwalSeminarTugasAkhir = getJadwalSeminarTugasAkhir();
		if ((jadwalSeminarTugasAkhir != null
				|| (getTanggalAwalBimbingan() != null && getTanggalAwalBimbingan().after(WaktuUtil.getDate())))

				&& (status.equals(AKTIF_STATUS) || status.equals(REQUEST_STATUS))) {
			status = SEMINAR_STATUS;
		}

		try {
			Skripsi skripsi = ambilSkripsi();
			if (skripsi != null && skripsi.getTelahSidang().equals(1)) {
				status = LULUS_STATUS;
			} else if (skripsi != null && skripsi.getTelahSidang().equals(0)) {
				if ((jadwalSeminarTugasAkhir != null || (getTanggalAwalBimbingan() != null
						&& getTanggalAwalBimbingan().after(WaktuUtil.getDate())))) {
					status = SEMINAR_STATUS;
				} else if (status.equals(SEMINAR_STATUS)) {
					status = AKTIF_STATUS;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/MahasiswaRequestTugasAkhir.java:365");
		}

		return status;
	}

	/**
	 * Menyetel status alur pengajuan secara eksplisit (dipakai layar persetujuan/penolakan).
	 *
	 * <p>Nilai yang dipakai aplikasi hanyalah konstanta {@code *_STATUS} di kelas ini; tidak ada
	 * validasi, string bebas akan diterima dan membuat {@link #getStatus()} tidak pernah
	 * mengoreksinya.</p>
	 *
	 * @param status salah satu dari {@link #REQUEST_STATUS}, {@link #AKTIF_STATUS},
	 *               {@link #SEMINAR_STATUS}, {@link #MENGULANG_STATUS}, {@link #LULUS_STATUS},
	 *               {@link #GAGAL_STATUS}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan judul tugas akhir yang berlaku, sudah di-{@code trim()}.
	 *
	 * <p><b>Getter berefek samping</b>: bila pengajuan sudah melewati tahap persetujuan (status
	 * {@link #AKTIF_STATUS}, {@link #SEMINAR_STATUS}, {@link #MENGULANG_STATUS}, atau
	 * {@link #LULUS_STATUS}) tetapi kolom {@code judul} masih kosong, judul diisi otomatis dari
	 * alternatif pertama {@link #getJudul1()}. Ini menutup kasus data lama yang menyetujui
	 * pengajuan tanpa memilih judul secara eksplisit; nilainya menempel ke entity dan tersimpan
	 * saat flush.</p>
	 *
	 * <p>Memanggil {@link #getStatus()} tiga sampai empat kali, jadi seluruh efek samping mesin
	 * status ikut terpicu.</p>
	 *
	 * @return judul terpilih; {@code ""} bila belum ada, tidak pernah {@code null}
	 */
	@Column(name = "judul", columnDefinition = "text", nullable = true)
	public String getJudul() {
		if ((getStatus().equals(AKTIF_STATUS) || getStatus().equals(SEMINAR_STATUS)
				|| getStatus().equals(MENGULANG_STATUS) || getStatus().equals(LULUS_STATUS))
				&& (judul == null || judul.trim().isEmpty())) {
			judul = getJudul1();
		}
		return judul == null ? "" : judul.trim();
	}

	/**
	 * Menyetel judul tugas akhir terpilih.
	 *
	 * @param judul judul yang disetujui
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan dosen pada slot ke-1 (kolom {@code dosen1}).
	 *
	 * <h4>PERINGATAN — getter ini bisa MENGHAPUS penetapan dosen</h4>
	 * <p>Sebelum mengembalikan nilai, getter membaca {@link #getFormatNilaiProposalSkripsi()} dan,
	 * bila slot 1 dinyatakan tidak aktif di master format nilai
	 * ({@code FormatNilaiProposalSkripsi.getDosen1Aktif() == false}), <b>menyetel field
	 * {@code dosen1} menjadi {@code null}</b>. Karena ini field ter-map, {@code null} tersebut
	 * ikut ditulis ke kolom {@code dosen1} pada flush berikutnya. Artinya: menonaktifkan sebuah
	 * slot pada master {@link FormatNilaiProposalSkripsi} akan menghapus data dosen pada seluruh
	 * pengajuan lama yang memakainya, cukup dengan membacanya. Perilaku ini identik pada
	 * {@link #getDosen2()}..{@link #getDosen6()} dan sengaja tidak diubah — banyak layar bergantung
	 * pada slot nonaktif tampil kosong.</p>
	 *
	 * <p>Makna slot (pembimbing 1/2/3, penguji 1/2/3, dst.) tidak dihardcode: labelnya diambil dari
	 * {@code FormatNilaiProposalSkripsi.getDosen1()}. Lihat {@link #dataDosen(boolean)}.</p>
	 *
	 * @return dosen slot 1, atau {@code null} bila belum ditetapkan atau slotnya nonaktif
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen1", nullable = true)
	public Dosen getDosen1() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		if (formatNilaiProposalSkripsi != null && !formatNilaiProposalSkripsi.getDosen1Aktif()) {
			dosen1 = null;
		}

		dosen1 = check(dosen1);
		return dosen1;
	}

	/**
	 * Menetapkan dosen pada slot ke-1. Tanpa validasi kuota — pemanggil layar memakai
	 * {@link #checkMaksSksDosen(Dosen, String, String, Integer)} lebih dulu.
	 *
	 * @param dosen1 dosen slot 1; {@code null} untuk mengosongkan
	 */
	public void setDosen1(Dosen dosen1) {
		this.dosen1 = dosen1;
	}

	/**
	 * Mengembalikan dosen pada slot ke-2 (kolom {@code dosen2}). Perilaku dan peringatan efek
	 * samping sama persis dengan {@link #getDosen1()}: slot yang nonaktif di
	 * {@link FormatNilaiProposalSkripsi} akan dikosongkan dan {@code null}-nya ikut tersimpan.
	 *
	 * @return dosen slot 2, atau {@code null}
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen2", nullable = true)
	public Dosen getDosen2() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		if (formatNilaiProposalSkripsi != null && !formatNilaiProposalSkripsi.getDosen2Aktif()) {
			dosen2 = null;
		}

		dosen2 = check(dosen2);
		return dosen2;
	}

	/**
	 * Menetapkan dosen pada slot ke-2.
	 *
	 * @param dosen2 dosen slot 2; {@code null} untuk mengosongkan
	 */
	public void setDosen2(Dosen dosen2) {
		this.dosen2 = dosen2;
	}

	/**
	 * Mengembalikan dosen pada slot ke-3 (kolom {@code dosen3}). Perilaku dan efek samping sama
	 * dengan {@link #getDosen1()}.
	 *
	 * @return dosen slot 3, atau {@code null}
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen3", nullable = true)
	public Dosen getDosen3() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		if (formatNilaiProposalSkripsi != null && !formatNilaiProposalSkripsi.getDosen3Aktif()) {
			dosen3 = null;
		}

		dosen3 = check(dosen3);
		return dosen3;
	}

	/**
	 * Menetapkan dosen pada slot ke-3.
	 *
	 * @param dosen3 dosen slot 3; {@code null} untuk mengosongkan
	 */
	public void setDosen3(Dosen dosen3) {
		this.dosen3 = dosen3;
	}

	/**
	 * Mengembalikan dosen pada slot ke-4 (kolom {@code dosen4}). Perilaku dan efek samping sama
	 * dengan {@link #getDosen1()}.
	 *
	 * @return dosen slot 4, atau {@code null}
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen4", nullable = true)
	public Dosen getDosen4() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		if (formatNilaiProposalSkripsi != null && !formatNilaiProposalSkripsi.getDosen4Aktif()) {
			dosen4 = null;
		}

		dosen4 = check(dosen4);
		return dosen4;
	}

	/**
	 * Menetapkan dosen pada slot ke-4.
	 *
	 * @param dosen4 dosen slot 4; {@code null} untuk mengosongkan
	 */
	public void setDosen4(Dosen dosen4) {
		this.dosen4 = dosen4;
	}

	/**
	 * Mengembalikan dosen pada slot ke-5 (kolom {@code dosen5}). Perilaku dan efek samping sama
	 * dengan {@link #getDosen1()}.
	 *
	 * @return dosen slot 5, atau {@code null}
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen5", nullable = true)
	public Dosen getDosen5() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		if (formatNilaiProposalSkripsi != null && !formatNilaiProposalSkripsi.getDosen5Aktif()) {
			dosen5 = null;
		}

		dosen5 = check(dosen5);
		return dosen5;
	}

	/**
	 * Menetapkan dosen pada slot ke-5.
	 *
	 * @param dosen5 dosen slot 5; {@code null} untuk mengosongkan
	 */
	public void setDosen5(Dosen dosen5) {
		this.dosen5 = dosen5;
	}

	/**
	 * Mengembalikan dosen pada slot ke-6 (kolom {@code dosen6}). Perilaku dan efek samping sama
	 * dengan {@link #getDosen1()}.
	 *
	 * @return dosen slot 6, atau {@code null}
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen6", nullable = true)
	public Dosen getDosen6() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		if (formatNilaiProposalSkripsi != null && !formatNilaiProposalSkripsi.getDosen6Aktif()) {
			dosen6 = null;
		}

		dosen6 = check(dosen6);
		return dosen6;
	}

	/**
	 * Menetapkan dosen pada slot ke-6.
	 *
	 * @param dosen6 dosen slot 6; {@code null} untuk mengosongkan
	 */
	public void setDosen6(Dosen dosen6) {
		this.dosen6 = dosen6;
	}

	/**
	 * Mengembalikan jadwal seminar proposal yang tertaut ke pengajuan ini.
	 *
	 * <p>Keberadaan jadwal ini adalah salah satu pemicu kenaikan status ke
	 * {@link #SEMINAR_STATUS} di {@link #getStatus()}. Nilai dilewatkan {@code check(...)} agar
	 * aman dibaca di luar session asli.</p>
	 *
	 * @return jadwal seminar, atau {@code null} bila belum dijadwalkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_seminar_tugas_akhir", nullable = true)
	public JadwalSeminarTugasAkhir getJadwalSeminarTugasAkhir() {
		jadwalSeminarTugasAkhir = check(jadwalSeminarTugasAkhir);
		return jadwalSeminarTugasAkhir;
	}

	/**
	 * Menautkan jadwal seminar proposal ke pengajuan ini.
	 *
	 * @param jadwalSeminarTugasAkhir jadwal seminar; {@code null} untuk melepas
	 */
	public void setJadwalSeminarTugasAkhir(JadwalSeminarTugasAkhir jadwalSeminarTugasAkhir) {
		this.jadwalSeminarTugasAkhir = jadwalSeminarTugasAkhir;
	}

	/**
	 * Mengembalikan baris KHS ({@link Detailperkuliahan}) tempat nilai tugas akhir dikonversi.
	 *
	 * <p>Berbeda dari relasi lain di kelas ini, getter ini <b>tidak</b> memakai {@code check(...)}
	 * — proxy dikembalikan apa adanya. Karena itu memanggilnya di luar session asli bisa melempar
	 * {@code LazyInitializationException}; {@code @Fetch(FetchMode.SELECT)} tidak mengubah hal itu.
	 * {@link #getNilaiHuruf()} dan {@link #getTotalNilai()} membaca field {@code detailperkuliahan}
	 * ini di dalam blok {@code try} sehingga galat tersebut ditelan diam-diam.</p>
	 *
	 * @return baris KHS terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detailperkuliahan", nullable = true)
	public Detailperkuliahan getDetailperkuliahan() {
		return detailperkuliahan;
	}

	/**
	 * Menautkan baris KHS tempat nilai tugas akhir dikonversi.
	 *
	 * @param detailperkuliahan baris KHS; {@code null} untuk melepas
	 */
	public void setDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/**
	 * Mengembalikan tanggal mulai masa bimbingan.
	 *
	 * <p><b>Getter berefek samping</b>: bila masih {@code null} sementara status sudah
	 * {@link #AKTIF_STATUS} atau {@link #SEMINAR_STATUS}, tanggal diisi dengan {@code new Date()}
	 * (waktu JVM saat ini — perhatikan, di sini TIDAK memakai {@code WaktuUtil.getDate()} seperti
	 * bagian lain kelas ini). Nilai itu menempel dan tersimpan saat flush, sehingga pengajuan lama
	 * yang belum punya tanggal akan mendapat tanggal hari pertama kali dibaca.</p>
	 *
	 * <p><b>Risiko NPE</b>: field {@code status} dibaca mentah tanpa penjaga {@code null} (berbeda
	 * dari {@link #getTanggalAkhirBimbingan()} yang memeriksanya). Aman selama tidak ada yang
	 * memanggil {@code setStatus(null)}, karena nilai awal field adalah {@link #REQUEST_STATUS}.</p>
	 *
	 * @return tanggal awal bimbingan, atau {@code null} bila pengajuan belum disetujui
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalAwalBimbingan() {
		if (tanggalAwalBimbingan == null && (status.equalsIgnoreCase(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
				|| status.equalsIgnoreCase(MahasiswaRequestTugasAkhir.SEMINAR_STATUS))) {
			tanggalAwalBimbingan = new Date();
		}
		return tanggalAwalBimbingan;
	}

	/**
	 * Menyetel tanggal mulai masa bimbingan secara eksplisit.
	 *
	 * @param tanggalAwalBimbingan tanggal mulai bimbingan
	 */
	public void setTanggalAwalBimbingan(Date tanggalAwalBimbingan) {
		this.tanggalAwalBimbingan = tanggalAwalBimbingan;
	}

	/**
	 * Mengembalikan tanggal berakhirnya masa bimbingan.
	 *
	 * <p><b>Getter berefek samping</b>: bila tanggal awal sudah ada, tanggal akhir masih
	 * {@code null}, dan status {@link #AKTIF_STATUS}/{@link #SEMINAR_STATUS}, nilainya dihitung
	 * sebagai <b>tanggal awal + 6 bulan</b> memakai {@code WaktuUtil.getCalendar()}, lalu disimpan
	 * ke field (ikut ter-{@code UPDATE} saat flush). Enam bulan adalah masa berlaku bimbingan
	 * default dan tidak dapat dikonfigurasi.</p>
	 *
	 * <p>Karena memanggil {@link #getTanggalAwalBimbingan()}, efek samping pengisian tanggal awal
	 * juga ikut terpicu.</p>
	 *
	 * @return tanggal akhir bimbingan, atau {@code null} bila belum ada tanggal awal
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalAkhirBimbingan() {

		if (getTanggalAwalBimbingan() != null && tanggalAkhirBimbingan == null && status != null
				&& (status.equalsIgnoreCase(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
						|| status.equalsIgnoreCase(MahasiswaRequestTugasAkhir.SEMINAR_STATUS))) {
			Calendar calendar = WaktuUtil.getCalendar();
			calendar.setTime(getTanggalAwalBimbingan());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			tanggalAkhirBimbingan = calendar.getTime();
		}

		return tanggalAkhirBimbingan;
	}

	/**
	 * Menyetel tanggal berakhirnya masa bimbingan secara eksplisit (mis. perpanjangan).
	 *
	 * @param tanggalAkhirBimbingan tanggal akhir bimbingan
	 */
	public void setTanggalAkhirBimbingan(Date tanggalAkhirBimbingan) {

		this.tanggalAkhirBimbingan = tanggalAkhirBimbingan;
	}

	/**
	 * Mengembalikan master format penilaian seminar proposal yang dipakai pengajuan ini.
	 *
	 * <p>Master ini menentukan tiga hal sekaligus: (1) slot dosen mana yang aktif — lihat
	 * peringatan pada {@link #getDosen1()}, (2) label/kode tiap slot yang dipakai
	 * {@link #dataDosen(boolean)}, {@link #simpanDosen(Dosen, String)} dan
	 * {@link #cariNilaiDariDosen(Dosen, String, Boolean)}, serta (3) persentase bobot tiap slot
	 * yang dipakai {@link #getTotalNilai()}.</p>
	 *
	 * @return format nilai proposal, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_nilai_proposal_skripsi", nullable = true)
	public FormatNilaiProposalSkripsi getFormatNilaiProposalSkripsi() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		return formatNilaiProposalSkripsi;
	}

	/**
	 * Memilih master format penilaian seminar proposal untuk pengajuan ini.
	 *
	 * <p>Mengganti master ini mengubah makna semua slot dosen dan bobot nilai yang sudah tersimpan;
	 * slot yang menjadi nonaktif akan dikosongkan oleh getter dosen terkait.</p>
	 *
	 * @param formatNilaiProposalSkripsi master format nilai proposal
	 */
	public void setFormatNilaiProposalSkripsi(FormatNilaiProposalSkripsi formatNilaiProposalSkripsi) {
		this.formatNilaiProposalSkripsi = formatNilaiProposalSkripsi;
	}

	/**
	 * Menentukan kelulusan seminar proposal berdasarkan nilai huruf.
	 *
	 * <h4>Urutan penentuan</h4>
	 * <ol>
	 *   <li>{@link #getNilaiHuruf()} dipanggil lebih dulu (dengan seluruh efek sampingnya: nilai
	 *   angka dan nilai huruf dihitung ulang dan disimpan ke field).</li>
	 *   <li><b>Sumber kebenaran utama</b>: master konfigurasi nilai huruf
	 *   {@code ConstantValues.lulusDariNilaiHuruf(nilaiHuruf, mahasiswa)}. Bila master menjawab
	 *   (tidak {@code null}), jawabannya menimpa nilai {@code lulus} yang tersimpan — sengaja,
	 *   supaya data lama yang basi ikut terkoreksi saat dibaca.</li>
	 *   <li>Fallback heuristik lama (hanya bila master tidak menjawab dan {@code lulus} masih
	 *   {@code null}): nilai huruf yang kosong atau mengandung huruf {@code D}, {@code E}, atau
	 *   {@code T} dianggap tidak lulus; selain itu lulus.</li>
	 *   <li>Bila nilai huruf {@code null}, hasil dipaksa {@code false}.</li>
	 * </ol>
	 *
	 * <p><b>Jebakan heuristik fallback</b>: pemeriksaannya memakai {@code contains}, bukan
	 * kesamaan, sehingga nilai huruf apa pun yang memuat huruf D/E/T di posisi mana pun akan
	 * dianggap tidak lulus. Hanya berlaku pada data yang tidak dikenali master.</p>
	 *
	 * <p><b>Efek samping</b>: menulis field {@code nilaiHuruf}, {@code totalNilai}, dan
	 * {@code lulus}. Galat pada jalur master ditelan dan dicatat ke audit.</p>
	 *
	 * @return {@code true} bila dinyatakan lulus seminar proposal; tidak pernah {@code null}
	 */
	public Boolean getLulus() {
		nilaiHuruf = getNilaiHuruf();
		// Utamakan KONFIGURASI Nilai Huruf yang DIPEROLEH (permintaan): status lulus mengikuti master
		// Nilai Huruf (ConstantValues.lulusDariNilaiHuruf) & mengoreksi nilai tersimpan yang basi.
		try {
			if (nilaiHuruf != null && !nilaiHuruf.trim().isEmpty()) {
				Boolean cfgLulus = ais.common.ConstantValues.lulusDariNilaiHuruf(nilaiHuruf, getMahasiswa());
				if (cfgLulus != null) {
					if (lulus == null || !lulus.equals(cfgLulus)) {
						lulus = cfgLulus;
					}
					return lulus;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaRequestTugasAkhir.java:565");
		}
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.isEmpty() || nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}

		if (nilaiHuruf == null) {
			lulus = false;
		}

		return lulus;
	}

	/**
	 * Menyetel status kelulusan secara manual. Nilai ini bisa ditimpa kembali oleh
	 * {@link #getLulus()} bila master nilai huruf memberi jawaban berbeda.
	 *
	 * @param lulus status kelulusan
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Mengembalikan bobot IP hasil konversi nilai; {@code 0.0} bila belum diisi.
	 *
	 * @return bobot IP; tidak pernah {@code null}
	 */
	public Double getTotalIP() {
		return totalIP == null ? 0.0 : totalIP;
	}

	/**
	 * Menyetel bobot IP hasil konversi nilai.
	 *
	 * @param totalIP bobot IP
	 */
	public void setTotalIP(Double totalIP) {
		this.totalIP = totalIP;
	}

	/**
	 * Membangun ulang seluruh isi {@link #getDetailNilai()} untuk seorang dosen, dari daftar
	 * komponen penilaian yang berlaku pada {@link FormatNilaiProposalSkripsi} pengajuan ini.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>{@link #refreshNilaiKeDefault(Dosen)} dipanggil lebih dulu (mengisi rincian dari nilai
	 *   total lama bila rincian masih kosong).</li>
	 *   <li>Mengambil daftar {@link ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi} milik
	 *   format nilai ini yang: bukan turunan ({@code parent} kosong), berbobot &gt; 0,01, dan
	 *   status pertemuannya aktif; diurutkan menaik menurut id.</li>
	 *   <li>Untuk tiap komponen, nilai dan tanda verifikasi yang SUDAH ada dibaca lewat
	 *   {@link #retreiveDetailNilai(KomponenPenilaianProposalSkripsi, Dosen)} dan
	 *   {@link #retreiveDetailVerifikasiNilai(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi, Dosen)},
	 *   lalu ditulis ulang sebagai entri
	 *   {@code komponenId,dosenId,nilai,0,bobot,verifikasi}.</li>
	 * </ol>
	 *
	 * <h4>PERINGATAN — bersifat merusak</h4>
	 * <p>Field {@code detailNilai} <b>ditimpa seluruhnya</b> dengan hasil rakitan di atas. Entri
	 * milik dosen LAIN yang sebelumnya ada dalam string yang sama akan <b>hilang</b>, karena
	 * perulangan hanya menuliskan entri untuk {@code dosen} yang diberikan. Method ini karenanya
	 * hanya aman dipakai pada skenario penilaian satu dosen.</p>
	 *
	 * <p>Memakai field {@code formatNilaiProposalSkripsi} mentah (bukan getter), sehingga proxy
	 * lazy yang belum terinisiasi bisa menghasilkan kriteria yang tidak diharapkan. Galat per
	 * komponen ditelan lewat {@code Common.tampilErrorJikaAdmin}. Saat ini tidak ada pemanggil di
	 * luar kelas ini.</p>
	 *
	 * @param session session Hibernate aktif yang dipakai untuk query komponen; tidak ditutup
	 * @param dosen   dosen penilai yang entrinya dibangun ulang
	 */
	@SuppressWarnings("unchecked")
	public void reloadProposalSkripsiPunyaKomponenPenilaianProposalSkripsi(Session session, Dosen dosen) {

		refreshNilaiKeDefault(dosen);

		String formatbaru = "";
		List<ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi> proposalSkripsiPunyaKomponenPenilaianProposalSkripsis = session
				.createCriteria(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi.class)
				.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi))
				.add(Restrictions.isNull("parent")).add(Restrictions.gt("persen", 0.01))
				.createCriteria("statusPertemuan")
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).list();
		for (ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi proposalSkripsiPunyaKomponenPenilaianProposalSkripsi : proposalSkripsiPunyaKomponenPenilaianProposalSkripsis) {
			try {
				Double jumlah = retreiveDetailNilai(
						proposalSkripsiPunyaKomponenPenilaianProposalSkripsi.getKomponenPenilaianProposalSkripsi(),
						dosen);
				Boolean verivy = retreiveDetailVerifikasiNilai(proposalSkripsiPunyaKomponenPenilaianProposalSkripsi,
						dosen);
				String aformatBaru = proposalSkripsiPunyaKomponenPenilaianProposalSkripsi
						.getKomponenPenilaianProposalSkripsi().getId() + "," + dosen.getId() + "," + jumlah + ",0,"
						+ proposalSkripsiPunyaKomponenPenilaianProposalSkripsi.getKomponenPenilaianProposalSkripsi()
								.getBobot()
						+ "," + verivy;
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		detailNilai = formatbaru;
	}

	/**
	 * Pintasan {@link #hitungTotalNilai(Boolean, Dosen, List)} tanpa daftar komponen eksplisit
	 * (daftar diambil dari database bila penyaringan diminta).
	 *
	 * @param gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase {@code true}
	 *        untuk membuang entri nilai yang komponennya sudah tidak berlaku
	 * @param dosen dosen penilai yang nilainya dihitung
	 * @return nilai akhir tertimbang milik dosen tersebut
	 * @see #hitungTotalNilai(Boolean, Dosen, List)
	 */
	public Double hitungTotalNilai(Boolean gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase,
			Dosen dosen) {
		return hitungTotalNilai(gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen, null);
	}

	/**
	 * Menghitung nilai akhir seminar proposal dari seorang dosen sebagai rata-rata tertimbang atas
	 * seluruh entri komponen miliknya di {@link #getDetailNilai()}.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>{@link #refreshNilaiKeDefault(Dosen)} dipanggil (mengisi rincian dari nilai total lama
	 *   bila rincian masih kosong).</li>
	 *   <li>Bila {@code gunakan...DariDatabase} bernilai {@code true}, rincian dibersihkan dari
	 *   komponen yang sudah tidak berlaku: memakai {@code proposalSkripsiPunya...s} bila diberikan
	 *   ({@link #bersihkanNilaiKeDefault(List)}), atau menariknya dari database bila {@code null}
	 *   ({@link #bersihkanNilaiKeDefault()}).</li>
	 *   <li>Seluruh entri milik {@code dosen} dikumpulkan sebagai pasangan (nilai, bobot).
	 *   <b>Bobot dinormalkan</b>: hasil = &sum; nilai &times; (bobot / total bobot), sehingga
	 *   nilainya tetap pada skala 0-100 walau total bobot tidak persis 100.</li>
	 * </ol>
	 *
	 * <h4>Catatan</h4>
	 * <ul>
	 *   <li>Entri disimpan dalam {@code HashMap} berkunci id komponen, jadi <b>entri ganda untuk
	 *   komponen yang sama akan saling menimpa</b> (yang terakhir menang) dan bobotnya tetap
	 *   dihitung berulang pada {@code totalPersen} — kombinasi ini bisa menghasilkan nilai lebih
	 *   rendah dari seharusnya bila rincian mengandung duplikat.</li>
	 *   <li>Entri tanpa kolom bobot (panjang &lt; 5) diabaikan sepenuhnya.</li>
	 *   <li>Bila total bobot &le; 0,001 hasilnya {@code 0.0}.</li>
	 *   <li>Method ini murni menghitung — tidak menulis ke {@code nilaiDosenN}. Penyimpanannya
	 *   dilakukan {@link #cariNilaiDariDosen(Dosen, String, Boolean)}.</li>
	 *   <li>Setiap baris rincian yang gagal diurai ditelan lewat {@code Common.tampilErrorJikaAdmin}
	 *   dan dilewati.</li>
	 * </ul>
	 *
	 * <p>Dipanggil dari {@code PenilaianProposalSkripsiHelper}/{@code PenilaianSkripsiHelper} lewat
	 * {@link #cariNilaiDariDosen(Dosen, String, Boolean)}.</p>
	 *
	 * @param gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase {@code true}
	 *        untuk membersihkan entri komponen yang tidak berlaku sebelum menghitung
	 * @param dosen dosen penilai yang nilainya dihitung
	 * @param proposalSkripsiPunyaKomponenPenilaianProposalSkripsis daftar id komponen yang dianggap
	 *        berlaku; {@code null} berarti tarik dari database
	 * @return nilai akhir tertimbang; {@code 0.0} bila tidak ada entri yang cocok
	 */
	public Double hitungTotalNilai(Boolean gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase,
			Dosen dosen, List<Long> proposalSkripsiPunyaKomponenPenilaianProposalSkripsis) {

		refreshNilaiKeDefault(dosen);
		if (gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase) {
			if (proposalSkripsiPunyaKomponenPenilaianProposalSkripsis == null) {
				bersihkanNilaiKeDefault();
			} else {
				bersihkanNilaiKeDefault(proposalSkripsiPunyaKomponenPenilaianProposalSkripsis);
			}
		}

		Double total = 0.0;
		String str = getDetailNilai();
		Double totalPersen = 0.0;

		if (str != null && !str.trim().isEmpty()) {
			String[] s = StringUtils.split(str, ";");
			Map<Long, Object[]> nilais = new HashMap<Long, Object[]>();
			for (String ss : s) {
				try {
					String[] sss = StringUtils.split(ss, ",");
					Long dosenId = Long.parseLong(sss[1]);
					if (dosenId.equals(dosen.getId())) {
						Long idProposalSkripsiPunyaKomponenPenilaianProposalSkripsi = Long.parseLong(sss[0].trim());
						Double persen = sss.length > 4 ? Double.parseDouble(sss[4].trim()) : null;
						if (persen != null) {
							Double n = Double.parseDouble(sss[2].trim());
							nilais.put(idProposalSkripsiPunyaKomponenPenilaianProposalSkripsi,
									new Object[] { n, persen });

							totalPersen += persen;

						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (totalPersen > 0.001) {
				for (Long proposalSkripsiPunyaKomponenPenilaianProposalSkripsi : nilais.keySet()) {
					try {
						Double n = (Double) nilais.get(proposalSkripsiPunyaKomponenPenilaianProposalSkripsi)[0];
						Double persen = (Double) nilais.get(proposalSkripsiPunyaKomponenPenilaianProposalSkripsi)[1];
						total += (n * (persen / totalPersen));
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

		return total;
	}

	/**
	 * Menyimpan (menambah atau memperbarui) nilai satu komponen penilaian dari satu dosen ke dalam
	 * string rincian {@link #getDetailNilai()}.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Nilai di bawah 0,01 memaksa {@code verify} menjadi {@code false} — komponen yang belum
	 *   dinilai tidak boleh berstatus terverifikasi.</li>
	 *   <li>Seluruh entri lama ditelusuri; entri dengan pasangan (komponenId, dosenId) yang sama
	 *   ditimpa oleh entri baru, entri lain disalin apa adanya.</li>
	 *   <li>Bila pasangan itu belum ada, entri baru ditambahkan di akhir.</li>
	 * </ol>
	 *
	 * <p>Bentuk entri: {@code komponenId,dosenId,nilai,0,bobot,verifikasi} — kolom keempat selalu
	 * konstanta {@code 0} (tidak terpakai), kolom bobot diambil dari
	 * {@code KomponenPenilaianProposalSkripsi.getBobot()} sehingga bobot yang tersimpan adalah
	 * SNAPSHOT saat penilaian, bukan bobot master terkini.</p>
	 *
	 * <p><b>Efek samping</b>: menulis field {@code detailNilai} (kolom {@code detail_nilai}).
	 * Method tidak melakukan apa pun bila {@code komponenPenilaianProposalSkripsi} {@code null}.
	 * Baris rincian yang gagal diurai dilewati dan galatnya ditelan lewat
	 * {@code Common.tampilErrorJikaAdmin}. Nilai {@code dosen} {@code null} akan melempar
	 * {@code NullPointerException} — pemanggil wajib menyediakannya.</p>
	 *
	 * <p>Dipanggil dari {@code PenilaianProposalSkripsiHelper}/{@code PenilaianSkripsiHelper} saat
	 * dosen mengisi form penilaian seminar proposal.</p>
	 *
	 * @param komponenPenilaianProposalSkripsi komponen yang dinilai; {@code null} = tidak ada aksi
	 * @param dosen  dosen penilai
	 * @param jumlah nilai angka komponen
	 * @param verify tanda verifikasi; dipaksa {@code false} bila nilai &lt; 0,01
	 */
	public void populateDetailNilai(KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi, Dosen dosen,
			Double jumlah, Boolean verify) {
		if (jumlah != null && jumlah < 0.01) {
			verify = false;
		}
		if (komponenPenilaianProposalSkripsi != null) {
			String formatBaru = "";
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long dosenId = Long.parseLong(s[1]);
						if (komponenPenilaianProposalSkripsi.getId().equals(formatId)
								&& dosen.getId().equals(dosenId)) {
							aformatBaru = komponenPenilaianProposalSkripsi.getId() + "," + dosen.getId() + "," + jumlah
									+ ",0," + komponenPenilaianProposalSkripsi.getBobot() + "," + verify;
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = komponenPenilaianProposalSkripsi.getId() + "," + dosen.getId() + "," + jumlah
						+ ",0," + komponenPenilaianProposalSkripsi.getBobot() + "," + verify;
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatBaru;
		}

	}

	/**
	 * Membuang entri nilai yang komponennya sudah tidak berlaku, dengan daftar komponen berlaku
	 * <b>ditarik dari database</b>.
	 *
	 * <p>Daftar id {@link KomponenPenilaianProposalSkripsi} yang dianggap berlaku adalah komponen
	 * milik {@link FormatNilaiProposalSkripsi} pengajuan ini yang: jurusannya kosong atau sama
	 * dengan jurusan mahasiswa, fakultasnya kosong atau sama dengan fakultas jurusan tersebut, dan
	 * berstatus aktif (atau {@code aktif} masih {@code null}). Hasilnya diteruskan ke
	 * {@link #bersihkanNilaiKeDefault(List)}.</p>
	 *
	 * <p><b>Catatan</b>: memakai {@code HibernateUtil.currentSession()} (session thread-local,
	 * tidak ditutup di sini) dan membaca field {@code formatNilaiProposalSkripsi} mentah.
	 * {@code getMahasiswa().getJurusan()} diakses tanpa penjaga {@code null} — mahasiswa tanpa
	 * jurusan akan melempar {@code NullPointerException}.</p>
	 *
	 * @see #bersihkanNilaiKeDefault(List)
	 */
	@SuppressWarnings("unchecked")
	public void bersihkanNilaiKeDefault() {
		Session session = HibernateUtil.currentSession();
		List<Long> proposalSkripsiPunyaKomponenPenilaianProposalSkripsis = session
				.createCriteria(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi.class)
				.createAlias("komponenPenilaianProposalSkripsi", "komponenPenilaianProposalSkripsi")
				.setProjection(Projections.groupProperty("komponenPenilaianProposalSkripsi.id"))

				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.jurusan"),
						Restrictions.eq("komponenPenilaianProposalSkripsi.jurusan", getMahasiswa().getJurusan())))
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.fakultas"),
						Restrictions.eq("komponenPenilaianProposalSkripsi.fakultas",
								getMahasiswa().getJurusan().getFakultas())))

				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.aktif"),
						Restrictions.eq("komponenPenilaianProposalSkripsi.aktif", true)))
				.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi)).list();
		bersihkanNilaiKeDefault(proposalSkripsiPunyaKomponenPenilaianProposalSkripsis);
	}

	/**
	 * Menyaring string rincian {@link #getDetailNilai()} sehingga hanya menyisakan entri yang id
	 * komponennya ada pada daftar yang diberikan.
	 *
	 * <p><b>Efek samping</b>: menulis field {@code detailNilai}. Entri milik SEMUA dosen ikut
	 * tersaring, bukan hanya satu dosen. Bila rincian kosong, method tidak melakukan apa pun
	 * (field tidak dikosongkan).</p>
	 *
	 * <p><b>Jebakan</b>: berbeda dari method sejenis di kelas ini, pengurai di sini <b>tidak</b>
	 * dibungkus {@code try/catch}. Satu entri cacat (id komponen bukan angka, atau entri kosong)
	 * akan melempar {@code NumberFormatException} keluar dari method.</p>
	 *
	 * @param proposalSkripsiPunyaKomponenPenilaianProposalSkripsis daftar id komponen yang boleh
	 *        dipertahankan; {@code null} akan melempar {@code NullPointerException} bila rincian
	 *        tidak kosong
	 */
	public void bersihkanNilaiKeDefault(List<Long> proposalSkripsiPunyaKomponenPenilaianProposalSkripsis) {
		String formatbaru = "";

		if (detailNilai != null && !detailNilai.trim().isEmpty()) {
			String[] s = StringUtils.split(detailNilai, ";");
			for (String ss : s) {
				String[] sss = StringUtils.split(ss, ",");
				Long idProposalSkripsiPunyaKomponenPenilaianProposalSkripsi = Long.parseLong(sss[0].trim());
				if (proposalSkripsiPunyaKomponenPenilaianProposalSkripsis
						.contains(idProposalSkripsiPunyaKomponenPenilaianProposalSkripsi)) {
					formatbaru += formatbaru.isEmpty() ? ss : ";" + ss;
				}
			}

			detailNilai = formatbaru;
		}
	}

	/**
	 * Membangun rincian nilai per komponen dari nilai total lama — jalur migrasi data lawas.
	 *
	 * <p>Hanya bekerja bila {@code detailNilai} masih kosong SEKALIGUS {@code totalNilai} sudah
	 * lebih dari 1,0 — kondisi khas baris lama yang dulu hanya menyimpan satu nilai akhir tanpa
	 * rincian komponen. Dalam keadaan itu, seluruh komponen yang berlaku (kriteria jurusan,
	 * fakultas, dan aktif sama seperti {@link #bersihkanNilaiKeDefault()}) diberi <b>nilai yang
	 * sama</b>, yaitu {@code totalNilai}, dengan tanda verifikasi {@code false}.</p>
	 *
	 * <p><b>Efek samping</b>: menulis field {@code detailNilai}. Dipanggil di awal hampir semua
	 * operasi penilaian di kelas ini ({@link #hitungTotalNilai(Boolean, Dosen, List)},
	 * {@link #retreiveDetailNilai(KomponenPenilaianProposalSkripsi, Dosen)},
	 * {@link #retreiveDetailVerifikasiNilai(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi, Dosen)},
	 * {@link #reloadProposalSkripsiPunyaKomponenPenilaianProposalSkripsi(Session, Dosen)}), jadi
	 * migrasi terjadi diam-diam pada pemakaian pertama.</p>
	 *
	 * <p><b>Catatan</b>: karena semua komponen diberi nilai identik, hasil
	 * {@link #hitungTotalNilai(Boolean, Dosen, List)} atas rincian hasil migrasi ini akan sama
	 * dengan {@code totalNilai} semula berapa pun bobotnya — itulah maksudnya. Memakai
	 * {@code HibernateUtil.currentSession()} dan tidak menutupnya; {@code dosen} {@code null} akan
	 * melempar {@code NullPointerException}.</p>
	 *
	 * @param dosen dosen yang dicantumkan sebagai penilai pada entri hasil migrasi
	 */
	@SuppressWarnings("unchecked")
	public void refreshNilaiKeDefault(Dosen dosen) {
		if ((detailNilai == null || detailNilai.trim().isEmpty()) && totalNilai != null && totalNilai > 1.0) {
			String formatbaru = "";
			Session session = HibernateUtil.currentSession();
			List<ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi> proposalSkripsiPunyaKomponenPenilaianProposalSkripsis = session
					.createCriteria(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi.class)
					.createAlias("komponenPenilaianProposalSkripsi", "komponenPenilaianProposalSkripsi")

					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.jurusan"),
							Restrictions.eq("komponenPenilaianProposalSkripsi.jurusan", getMahasiswa().getJurusan())))
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.fakultas"),
							Restrictions.eq("komponenPenilaianProposalSkripsi.fakultas",
									getMahasiswa().getJurusan().getFakultas())))

					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.aktif"),
							Restrictions.eq("komponenPenilaianProposalSkripsi.aktif", true)))

					.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi)).list();

			for (ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi proposalSkripsiPunyaKomponenPenilaianProposalSkripsi : proposalSkripsiPunyaKomponenPenilaianProposalSkripsis) {

				String aformatBaru = proposalSkripsiPunyaKomponenPenilaianProposalSkripsi
						.getKomponenPenilaianProposalSkripsi().getId() + "," + dosen.getId() + "," + totalNilai + ",0,"
						+ proposalSkripsiPunyaKomponenPenilaianProposalSkripsi.getKomponenPenilaianProposalSkripsi()
								.getBobot()
						+ ",false";
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}
			proposalSkripsiPunyaKomponenPenilaianProposalSkripsis = null;
			detailNilai = formatbaru;
		}
	}

	/**
	 * Membaca nilai satu komponen penilaian dari seorang dosen di dalam rincian
	 * {@link #getDetailNilai()}.
	 *
	 * <p>Memanggil {@link #refreshNilaiKeDefault(Dosen)} lebih dulu, sehingga <b>bisa menulis</b>
	 * field {@code detailNilai} walau namanya terdengar seperti operasi baca saja.</p>
	 *
	 * <p>Entri dianggap sah bila punya minimal tiga kolom dan tiga kolom pertamanya tidak kosong;
	 * entri lain dilewati. Setiap galat penguraian ditelan dan dicatat ke audit.</p>
	 *
	 * @param formatIdSource komponen penilaian yang dicari; {@code null} atau tanpa id menghasilkan
	 *                       {@code 0.0}
	 * @param dosen          dosen penilai yang entrinya dicari
	 * @return nilai komponen tersebut, atau {@code 0.0} bila tidak ditemukan
	 */
	public Double retreiveDetailNilai(KomponenPenilaianProposalSkripsi formatIdSource, Dosen dosen) {

		refreshNilaiKeDefault(dosen);

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn == null ? new String[] {} : nn.split(",", -1);
					if (s.length < 3 || s[0].trim().length() == 0 || s[1].trim().length() == 0
							|| s[2].trim().length() == 0) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dosenId = Long.parseLong(s[1]);
					if (formatIdSource.getId().equals(formatId) && dosen.getId().equals(dosenId)) {
						return Double.parseDouble(s[2]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaRequestTugasAkhir.java:822");

				}
			}
		}

		return 0.0;
	}

	/**
	 * Membaca bendera verifikasi (kolom ke-6) satu komponen penilaian dari satu dosen pada rincian
	 * {@link #getDetailNilai()}. Nilai di bawah 0,01 selalu dilaporkan belum terverifikasi, apa pun
	 * isi kolom benderanya.
	 *
	 * <p>Strukturnya menyalin {@link #retreiveDetailNilai(KomponenPenilaianProposalSkripsi, Dosen)},
	 * termasuk pemanggilan {@link #refreshNilaiKeDefault(Dosen)} di awal (jadi method ini
	 * <b>bisa menulis</b> {@code detailNilai}). Entri dianggap sah bila punya minimal enam kolom.</p>
	 *
	 * <p><b>BUG yang ditemukan saat pendokumentasian (sengaja TIDAK diperbaiki, hanya dicatat):
	 * ruang id argumennya tidak cocok dengan isi rincian.</b> Kolom pertama {@code detailNilai}
	 * SELALU ditulis sebagai id {@link KomponenPenilaianProposalSkripsi} — lihat
	 * {@link #populateDetailNilai(KomponenPenilaianProposalSkripsi, Dosen, Double, Boolean)},
	 * {@link #refreshNilaiKeDefault(Dosen)}, dan
	 * {@link #reloadProposalSkripsiPunyaKomponenPenilaianProposalSkripsi(Session, Dosen)} yang
	 * semuanya memakai {@code getKomponenPenilaianProposalSkripsi().getId()}. Tetapi method ini
	 * membandingkannya dengan {@code formatIdSource.getId()}, yaitu id baris
	 * {@link ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi} — tabel penghubung dengan
	 * urutan id sendiri. Akibatnya bendera verifikasi hanya terbaca benar bila kedua id kebetulan
	 * bernilai sama; selebihnya method mengembalikan {@code false}. Ini bug yang identik dengan
	 * {@link Skripsi#retreiveDetailVerifikasiNilai(SkripsiPunyaKomponenPenilaianSkripsi, Dosen)}
	 * dan sepola dengan salah-pakai id anak versus id induk yang pernah ditemukan di
	 * {@link Pertemuan}. Dampaknya saat ini terbatas: satu-satunya pemanggil di dalam kelas ini,
	 * {@link #reloadProposalSkripsiPunyaKomponenPenilaianProposalSkripsi(Session, Dosen)}, juga
	 * sudah tidak dipanggil dari mana pun.</p>
	 *
	 * @param formatIdSource baris penghubung komponen-format nilai; lihat catatan bug di atas
	 * @param dosen          dosen pemberi nilai; tidak boleh {@code null}
	 * @return {@code true} bila nilai komponen itu sudah diverifikasi; {@code false} bila belum,
	 *         nilainya nol, atau entri tidak ditemukan
	 */
	public Boolean retreiveDetailVerifikasiNilai(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi formatIdSource,
			Dosen dosen) {

		refreshNilaiKeDefault(dosen);

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn == null ? new String[] {} : nn.split(",", -1);
					if (s.length < 6 || s[0].trim().length() == 0 || s[1].trim().length() == 0
							|| s[2].trim().length() == 0) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dosenId = Long.parseLong(s[1]);
					if (formatIdSource.getId().equals(formatId) && dosen.getId().equals(dosenId)) {
						if (Double.parseDouble(s[2]) < 0.01) {
							return false;
						}
						return Boolean.parseBoolean(s[5]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaRequestTugasAkhir.java:849");

				}
			}
		}

		return false;
	}

	/**
	 * Mengembalikan rincian nilai seminar proposal sebagai satu string CSV bertingkat.
	 *
	 * <p>Format: entri dipisah titik koma, tiap entri berisi enam kolom dipisah koma —
	 * {@code komponenId,dosenId,nilai,0,bobot,verifikasi}. Kolom keempat selalu {@code 0} (tidak
	 * terpakai). Satu string memuat entri dari SEMUA dosen penilai sekaligus. Data lawas bisa
	 * punya kolom lebih sedikit; semua pembaca di kelas ini menyaring entri pendek dan
	 * melewatinya.</p>
	 *
	 * @return rincian nilai; {@code ""} bila belum ada, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai == null ? "" : detailNilai.trim();
	}

	/**
	 * Menyetel rincian nilai mentah. Tanpa validasi format — pemakaian normal lewat
	 * {@link #populateDetailNilai(KomponenPenilaianProposalSkripsi, Dosen, Double, Boolean)}.
	 *
	 * @param detailNilai string rincian dalam format yang dijelaskan di {@link #getDetailNilai()}
	 */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/**
	 * Menghitung ulang dan mengembalikan nilai huruf hasil konversi {@link #getTotalNilai()}.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>{@link #getTotalNilai()} dipanggil lebih dulu (nilai angka dihitung ulang dari enam
	 *   slot dosen dan disimpan ke field).</li>
	 *   <li>Matakuliah pembanding diambil dari {@link #getDetailperkuliahan()}: dari perkuliahannya
	 *   bila ada, jika tidak dari matakuliah konversi.</li>
	 *   <li>Jenis skala nilai huruf diambil dari matakuliah tersebut, lalu <b>ditimpa</b> oleh
	 *   {@code FormatNilaiProposalSkripsi.getJenisNilaiHuruf()} bila master format nilai
	 *   menyediakannya — master format nilai proposal lebih tinggi prioritasnya.</li>
	 *   <li>{@code Common.getNilaiHuruf(...)} dipanggil dengan konteks tahun angkatan, jurusan,
	 *   fakultas, tahun akademik, ganjil/genap, dan kode matakuliah. Bila belum ada
	 *   {@link Detailperkuliahan}, konteks tahun akademik/semester diambil dari periode
	 *   <b>berjalan</b> ({@code Common.getCurrentTahunAkademik()},
	 *   {@code Common.isNowSemensterGanjil()}), bukan dari periode pengajuan — jadi konversi
	 *   pengajuan lama bisa memakai aturan skala yang berlaku hari ini.</li>
	 * </ol>
	 *
	 * <h4>Catatan penting</h4>
	 * <ul>
	 *   <li>Akses mahasiswa <b>wajib</b> lewat {@link #getMahasiswa()}, bukan field mentah — lihat
	 *   komentar di badan method: bila dipanggil belakangan lewat refleksi saat cetak
	 *   laporan/transkrip, proxy lazy {@code mahasiswa} bisa memicu
	 *   {@code LazyInitializationException}; {@code check(...)} di dalam getter memuat ulang
	 *   entity dari session baru.</li>
	 *   <li><b>Efek samping</b>: menulis field {@code totalNilai} dan {@code nilaiHuruf}.</li>
	 *   <li>Seluruh galat ditelan (dicatat ke audit); bila gagal, nilai huruf lama dikembalikan
	 *   apa adanya.</li>
	 * </ul>
	 *
	 * @return nilai huruf hasil konversi (sudah di-{@code trim}), atau {@code null} bila belum ada
	 */
	public String getNilaiHuruf() {

		totalNilai = getTotalNilai();
		try {
			Matakuliah matakuliah = detailperkuliahan == null ? null
					: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
							: detailperkuliahan.getMatakuliahKonversi();

			JenisNilaiHurufMatakuliah jenisNilaiHuruf = matakuliah == null ? null : matakuliah.getJenisNilaiHuruf();

			if (formatNilaiProposalSkripsi != null && formatNilaiProposalSkripsi.getJenisNilaiHuruf() != null) {
				jenisNilaiHuruf = formatNilaiProposalSkripsi.getJenisNilaiHuruf();
			}

			// PENTING: jangan akses field proxy `mahasiswa` secara langsung di sini.
			// Jika session Hibernate asli sudah tertutup (mis. dipanggil belakangan oleh
			// ManajemenProperty via reflection saat cetak laporan/transkrip), proxy lazy
			// `mahasiswa` bisa belum terinisiasi dan langsung memicu
			// org.hibernate.LazyInitializationException: could not initialize proxy - no Session.
			// getMahasiswa() sudah membungkus akses lewat check(mahasiswa) yang akan
			// menginisiasi ulang / reload entity dari session baru (openSession sendiri, ditutup
			// di finally) bila proxy terdeteksi belum ter-initialize dan session lama sudah mati.
			Mahasiswa mahasiswaAman = getMahasiswa();
			NilaiHuruf a = mahasiswaAman == null ? null
					: detailperkuliahan == null
					? Common.getNilaiHuruf(totalNilai, mahasiswaAman.getTahunangkatan(), mahasiswaAman.getJurusan(),
							mahasiswaAman.getJurusan().getFakultas(), Common.getCurrentTahunAkademik(),
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
							matakuliah == null ? "" : matakuliah.getKode(), jenisNilaiHuruf)
					: Common.getNilaiHuruf(totalNilai, mahasiswaAman.getTahunangkatan(), mahasiswaAman.getJurusan(),
							mahasiswaAman.getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
							detailperkuliahan.getPerkuliahan() == null ? null
									: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
							matakuliah == null ? "" : matakuliah.getKode(), jenisNilaiHuruf);
			nilaiHuruf = a == null ? "" : a.getNilaiHuruf();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaRequestTugasAkhir.java:892");
			// TODO: handle exception
		}

		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

	/**
	 * Menyetel nilai huruf secara manual. Akan ditimpa lagi pada pemanggilan
	 * {@link #getNilaiHuruf()} berikutnya bila konversi berhasil.
	 *
	 * @param nilaiHuruf nilai huruf
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Menghitung ulang dan mengembalikan nilai angka akhir seminar proposal sebagai jumlah
	 * tertimbang enam slot dosen.
	 *
	 * <h4>Rumus</h4>
	 * <p>{@code total = }&sum; {@code nilaiDosenN × prosentaseSlotN / 100}, dengan persentase
	 * diambil dari {@link FormatNilaiProposalSkripsi}:</p>
	 * <ul>
	 *   <li>{@code nilaiDosen1} &times; {@code getProsentasiNilaiPembimbing1()}</li>
	 *   <li>{@code nilaiDosen2} &times; {@code getProsentasiNilaiPembimbing2()}</li>
	 *   <li>{@code nilaiDosen3} &times; {@code getProsentasiNilaiPembimbing3()}</li>
	 *   <li>{@code nilaiDosen4} &times; {@code getProsentasiNilaiPenguji1()}</li>
	 *   <li>{@code nilaiDosen5} &times; {@code getProsentasiNilaiPenguji2()}</li>
	 *   <li>{@code nilaiDosen6} &times; {@code getProsentasiNilaiPenguji3()}</li>
	 * </ul>
	 * <p>Pemetaan slot ke kolom persentase di atas <b>konsisten</b> dengan penomoran master
	 * {@link FormatNilaiProposalSkripsi} (tidak ada pertukaran slot seperti yang ditemukan di
	 * {@link Skripsi}). Yang menyesatkan hanyalah <b>nama variabel lokal</b> di dalam badan method
	 * ({@code nilaiKetuaV}, {@code nilaiPembimbingV}, {@code nilaiPenguji1V}, ...) yang bergeser
	 * satu langkah dari makna slotnya — namanya salah, rumusnya benar.</p>
	 *
	 * <h4>Catatan &amp; jebakan</h4>
	 * <ul>
	 *   <li><b>Penjaga masuk hanya memeriksa slot 1 sampai 5</b>
	 *   ({@code getNilaiDosen1() > 0.1 || ... || getNilaiDosen5() > 0.1}). Bila HANYA slot 6 yang
	 *   terisi, perhitungan tidak pernah dijalankan dan nilai total lama dipertahankan. Dibiarkan
	 *   apa adanya karena slot 6 praktis tidak pernah dipakai sendirian.</li>
	 *   <li>{@code FormatNilaiProposalSkripsi.getProsentasiNilaiPenguji4()} ada di master tetapi
	 *   TIDAK dipakai di sini — hanya enam slot yang dihitung.</li>
	 *   <li>Nilai per slot dibaca lewat getter {@code getNilaiDosenN()} yang sudah null-safe;
	 *   membaca field {@code Double} mentah akan memicu {@code NullPointerException} saat unboxing
	 *   (lihat komentar di badan method — ini pernah terjadi).</li>
	 *   <li><b>Efek samping</b>: menulis field {@code formatNilaiProposalSkripsi} dan
	 *   {@code totalNilai}. Galat ditelan dan dicatat ke audit.</li>
	 * </ul>
	 *
	 * @return nilai angka akhir; {@code 0.0} bila belum ada nilai, tidak pernah {@code null}
	 */
	public Double getTotalNilai() {
		try {
			formatNilaiProposalSkripsi = getFormatNilaiProposalSkripsi();
			// PENTING: jangan akses field nilaiDosenX mentah di sini -- field Double ini boleh
			// null (belum dinilai salah satu penguji/pembimbing), dan unboxing langsung
			// (nilaiDosenX > 0.1) memicu NullPointerException. Pakai getter getNilaiDosenX()
			// yang sudah null-guard (kembalikan 0.0 bila belum diisi), formula/bobot TIDAK diubah.
			if (formatNilaiProposalSkripsi != null && (getNilaiDosen1() > 0.1 || getNilaiDosen2() > 0.1
					|| getNilaiDosen3() > 0.1 || getNilaiDosen4() > 0.1 || getNilaiDosen5() > 0.1)) {

				Double nilaiKetuaV = getNilaiDosen1() * (formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing1()) / 100.0;
				Double nilaiPembimbingV = getNilaiDosen2() * (formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing2())
						/ 100.0;
				Double nilaiPenguji1V = getNilaiDosen3() * (formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing3())
						/ 100.0;
				Double nilaiPenguji2V = getNilaiDosen4() * (formatNilaiProposalSkripsi.getProsentasiNilaiPenguji1()) / 100.0;

				Double nilaiPenguji3V = getNilaiDosen5() * (formatNilaiProposalSkripsi.getProsentasiNilaiPenguji2()) / 100.0;

				Double nilaiPenguji4V = getNilaiDosen6() * (formatNilaiProposalSkripsi.getProsentasiNilaiPenguji3()) / 100.0;

				Double jumlahTotal = nilaiKetuaV + nilaiPembimbingV + nilaiPenguji1V + nilaiPenguji2V + nilaiPenguji3V
						+ nilaiPenguji4V;
				// System.out.println("jumlahTotal => " + jumlahTotal + ",
				// totalNilai = " + totalNilai);
				if (totalNilai == null || !jumlahTotal.equals(totalNilai)) {
					totalNilai = jumlahTotal;

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaRequestTugasAkhir.java:929");
			// Common.tampilErrorJikaAdmin(e);
		}

		if (totalNilai == null) {
			totalNilai = 0.0;
		}
		return totalNilai;
	}

	/**
	 * Menyetel nilai angka akhir secara manual. Bisa dihitung ulang dan ditimpa oleh
	 * {@link #getTotalNilai()}.
	 *
	 * @param totalNilai nilai angka akhir
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Mengembalikan nilai akhir dari dosen slot 1; {@code 0.0} bila belum dinilai (null-safe,
	 * mencegah {@code NullPointerException} saat unboxing di {@link #getTotalNilai()}).
	 *
	 * @return nilai slot 1; tidak pernah {@code null}
	 */
	public Double getNilaiDosen1() {
		return nilaiDosen1 == null ? 0.0 : nilaiDosen1;
	}

	/**
	 * Menyimpan nilai akhir dari dosen slot 1. Biasanya dipanggil
	 * {@link #cariNilaiDariDosen(Dosen, String, Boolean)}.
	 *
	 * @param nilaiDosen1 nilai slot 1
	 */
	public void setNilaiDosen1(Double nilaiDosen1) {
		this.nilaiDosen1 = nilaiDosen1;
	}

	/**
	 * Mengembalikan nilai akhir dari dosen slot 2; {@code 0.0} bila belum dinilai.
	 *
	 * @return nilai slot 2; tidak pernah {@code null}
	 */
	public Double getNilaiDosen2() {
		return nilaiDosen2 == null ? 0.0 : nilaiDosen2;
	}

	/**
	 * Menyimpan nilai akhir dari dosen slot 2.
	 *
	 * @param nilaiDosen2 nilai slot 2
	 */
	public void setNilaiDosen2(Double nilaiDosen2) {
		this.nilaiDosen2 = nilaiDosen2;
	}

	/**
	 * Mengembalikan nilai akhir dari dosen slot 3; {@code 0.0} bila belum dinilai.
	 *
	 * @return nilai slot 3; tidak pernah {@code null}
	 */
	public Double getNilaiDosen3() {
		return nilaiDosen3 == null ? 0.0 : nilaiDosen3;
	}

	/**
	 * Menyimpan nilai akhir dari dosen slot 3.
	 *
	 * @param nilaiDosen3 nilai slot 3
	 */
	public void setNilaiDosen3(Double nilaiDosen3) {
		this.nilaiDosen3 = nilaiDosen3;
	}

	/**
	 * Mengembalikan nilai akhir dari dosen slot 4; {@code 0.0} bila belum dinilai.
	 *
	 * @return nilai slot 4; tidak pernah {@code null}
	 */
	public Double getNilaiDosen4() {
		return nilaiDosen4 == null ? 0.0 : nilaiDosen4;
	}

	/**
	 * Menyimpan nilai akhir dari dosen slot 4.
	 *
	 * @param nilaiDosen4 nilai slot 4
	 */
	public void setNilaiDosen4(Double nilaiDosen4) {
		this.nilaiDosen4 = nilaiDosen4;
	}

	/**
	 * Mengembalikan nilai akhir dari dosen slot 5; {@code 0.0} bila belum dinilai.
	 *
	 * @return nilai slot 5; tidak pernah {@code null}
	 */
	public Double getNilaiDosen5() {
		return nilaiDosen5 == null ? 0.0 : nilaiDosen5;
	}

	/**
	 * Menyimpan nilai akhir dari dosen slot 5.
	 *
	 * @param nilaiDosen5 nilai slot 5
	 */
	public void setNilaiDosen5(Double nilaiDosen5) {
		this.nilaiDosen5 = nilaiDosen5;
	}

	/**
	 * Mengembalikan nilai akhir dari dosen slot 6; {@code 0.0} bila belum dinilai.
	 *
	 * <p>Perhatikan: slot ini tidak ikut diperiksa oleh penjaga masuk {@link #getTotalNilai()} —
	 * lihat catatan jebakan di sana.</p>
	 *
	 * @return nilai slot 6; tidak pernah {@code null}
	 */
	public Double getNilaiDosen6() {
		return nilaiDosen6 == null ? 0.0 : nilaiDosen6;
	}

	/**
	 * Menyimpan nilai akhir dari dosen slot 6.
	 *
	 * @param nilaiDosen6 nilai slot 6
	 */
	public void setNilaiDosen6(Double nilaiDosen6) {
		this.nilaiDosen6 = nilaiDosen6;
	}

	/**
	 * Mengembalikan format nilai perkuliahan pendamping (kolom {@code format_nilai}).
	 *
	 * <p>Bukan master yang dipakai perhitungan nilai kelas ini — itu
	 * {@link #getFormatNilaiProposalSkripsi()}. Tidak memakai {@code check(...)}, jadi proxy
	 * dikembalikan apa adanya.</p>
	 *
	 * @return format nilai perkuliahan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_nilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	/**
	 * Menyetel format nilai perkuliahan pendamping.
	 *
	 * @param formatNilai format nilai perkuliahan
	 */
	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	/**
	 * Mengembalikan tahapan/capaian pembelajaran penyusunan tugas akhir yang sedang berjalan
	 * (mis. bab yang sedang dikerjakan). Nilai dilewatkan {@code check(...)}.
	 *
	 * @return tahapan penyusunan, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tahapan_penyusunan_tugas_akhir", nullable = true)
	public TahapanAtauCapaianPembelajaran getTahapanPenyusunanTugasAkhir() {
		tahapanPenyusunanTugasAkhir = check(tahapanPenyusunanTugasAkhir);
		return tahapanPenyusunanTugasAkhir;
	}

	/**
	 * Menyetel tahapan/capaian pembelajaran penyusunan tugas akhir.
	 *
	 * @param tahapanPenyusunanTugasAkhir tahapan yang sedang berjalan
	 */
	public void setTahapanPenyusunanTugasAkhir(TahapanAtauCapaianPembelajaran tahapanPenyusunanTugasAkhir) {
		this.tahapanPenyusunanTugasAkhir = tahapanPenyusunanTugasAkhir;
	}

	/**
	 * Mengembalikan alternatif judul ke-2 yang diusulkan mahasiswa, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 2; {@code ""} bila kosong, tidak pernah {@code null}
	 */
	@Column(name = "judul2", columnDefinition = "text", nullable = true)
	public String getJudul2() {
		return judul2 == null ? "" : judul2.trim();
	}

	/**
	 * Menyetel alternatif judul ke-2.
	 *
	 * @param judul2 judul alternatif 2
	 */
	public void setJudul2(String judul2) {
		this.judul2 = judul2;
	}

	/**
	 * Mengembalikan alternatif judul ke-3, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 3; {@code ""} bila kosong
	 */
	@Column(name = "judul3", columnDefinition = "text", nullable = true)
	public String getJudul3() {
		return judul3 == null ? "" : judul3.trim();
	}

	/**
	 * Menyetel alternatif judul ke-3.
	 *
	 * @param judul3 judul alternatif 3
	 */
	public void setJudul3(String judul3) {
		this.judul3 = judul3;
	}

	/**
	 * Mengembalikan alternatif judul ke-4, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 4; {@code ""} bila kosong
	 */
	@Column(name = "judul4", columnDefinition = "text", nullable = true)
	public String getJudul4() {
		return judul4 == null ? "" : judul4.trim();
	}

	/**
	 * Menyetel alternatif judul ke-4.
	 *
	 * @param judul4 judul alternatif 4
	 */
	public void setJudul4(String judul4) {
		this.judul4 = judul4;
	}

	/**
	 * Mengembalikan alternatif judul ke-5, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 5; {@code ""} bila kosong
	 */
	@Column(name = "judul5", columnDefinition = "text", nullable = true)
	public String getJudul5() {
		return judul5 == null ? "" : judul5.trim();
	}

	/**
	 * Menyetel alternatif judul ke-5.
	 *
	 * @param judul5 judul alternatif 5
	 */
	public void setJudul5(String judul5) {
		this.judul5 = judul5;
	}

	/**
	 * Mengembalikan alternatif judul ke-6, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 6; {@code ""} bila kosong
	 */
	@Column(name = "judul6", columnDefinition = "text", nullable = true)
	public String getJudul6() {
		return judul6 == null ? "" : judul6.trim();
	}

	/**
	 * Menyetel alternatif judul ke-6.
	 *
	 * @param judul6 judul alternatif 6
	 */
	public void setJudul6(String judul6) {
		this.judul6 = judul6;
	}

	/**
	 * Mengembalikan alternatif judul ke-1.
	 *
	 * <p><b>Berbeda dari saudara-saudaranya</b>: bila {@code judul1} kosong, yang dikembalikan
	 * adalah field {@code judul} (judul terpilih) — bukan {@code ""} — dan hasilnya TIDAK
	 * di-{@code trim} maupun dijamin non-{@code null}. Fallback ini menopang data lama yang hanya
	 * mengisi judul final tanpa daftar alternatif, sekaligus melengkapi jalur balik di
	 * {@link #getJudul()} yang mengisi judul final dari alternatif pertama.</p>
	 *
	 * @return judul alternatif 1, atau judul terpilih bila alternatif 1 kosong; bisa {@code null}
	 */
	@Column(name = "judul1", columnDefinition = "text", nullable = true)
	public String getJudul1() {
		return judul1 == null || judul1.trim().isEmpty() ? judul : judul1;
	}

	/**
	 * Menyetel alternatif judul ke-1.
	 *
	 * @param judul1 judul alternatif 1
	 */
	public void setJudul1(String judul1) {
		this.judul1 = judul1;
	}

	/**
	 * Mengembalikan banyaknya alternatif judul yang berlaku untuk pengajuan ini; {@code 1} bila
	 * belum diisi. Dipakai layar untuk menentukan berapa kotak judul yang ditampilkan.
	 *
	 * @return jumlah alternatif judul (1-10); tidak pernah {@code null}
	 */
	public Integer getJumlahJudul() {
		return jumlahJudul == null ? 1 : jumlahJudul;
	}

	/**
	 * Menyetel banyaknya alternatif judul yang berlaku. Tidak ada validasi batas atas 10 —
	 * kolom judul yang tersedia hanya sepuluh.
	 *
	 * @param jumlahJudul jumlah alternatif judul
	 */
	public void setJumlahJudul(Integer jumlahJudul) {
		this.jumlahJudul = jumlahJudul;
	}

	/**
	 * Mengembalikan alternatif judul ke-7, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 7; {@code ""} bila kosong
	 */
	@Column(name = "judul7", columnDefinition = "text", nullable = true)
	public String getJudul7() {
		return judul7 == null ? "" : judul7.trim();
	}

	/**
	 * Menyetel alternatif judul ke-7.
	 *
	 * @param judul7 judul alternatif 7
	 */
	public void setJudul7(String judul7) {
		this.judul7 = judul7;
	}

	/**
	 * Mengembalikan alternatif judul ke-8, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 8; {@code ""} bila kosong
	 */
	@Column(name = "judul8", columnDefinition = "text", nullable = true)
	public String getJudul8() {
		return judul8 == null ? "" : judul8.trim();
	}

	/**
	 * Menyetel alternatif judul ke-8.
	 *
	 * @param judul8 judul alternatif 8
	 */
	public void setJudul8(String judul8) {
		this.judul8 = judul8;
	}

	/**
	 * Mengembalikan alternatif judul ke-9, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 9; {@code ""} bila kosong
	 */
	@Column(name = "judul9", columnDefinition = "text", nullable = true)
	public String getJudul9() {
		return judul9 == null ? "" : judul9.trim();
	}

	/**
	 * Menyetel alternatif judul ke-9.
	 *
	 * @param judul9 judul alternatif 9
	 */
	public void setJudul9(String judul9) {
		this.judul9 = judul9;
	}

	/**
	 * Mengembalikan alternatif judul ke-10, sudah di-{@code trim()}.
	 *
	 * @return judul alternatif 10; {@code ""} bila kosong
	 */
	@Column(name = "judul10", columnDefinition = "text", nullable = true)
	public String getJudul10() {
		return judul10 == null ? "" : judul10.trim();
	}

	/**
	 * Menyetel alternatif judul ke-10.
	 *
	 * @param judul10 judul alternatif 10
	 */
	public void setJudul10(String judul10) {
		this.judul10 = judul10;
	}

	/**
	 * Implementasi {@link VOPesertaPembelajaran}: pengajuan ini <b>adalah</b> objek
	 * pembelajarannya sendiri, jadi mengembalikan {@code this}.
	 *
	 * <p>Pola yang sama dipakai {@link Skripsi}: satu bimbingan tugas akhir sekaligus menjadi
	 * "kelas" dan satu-satunya pesertanya, sehingga mesin pertemuan/absensi generik
	 * {@link VOPembelajaran} bisa dipakai tanpa entity kelas terpisah.</p>
	 *
	 * @return {@code this}
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return this;
	}

	/**
	 * Implementasi {@link VOPembelajaran}: selalu {@code 1}, karena satu pengajuan tugas akhir
	 * hanya menyangkut satu mahasiswa (satu baris KHS).
	 *
	 * @return selalu {@code 1}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/** Konfigurasi e-learning/materi dalam bentuk JSON. Lihat {@link #getCourse()}. */
	private String course;
	/** Daftar referensi/pustaka dalam bentuk JSON array. Lihat {@link #getReferensi()}. */
	private String referensi;
	/** Kode/identitas ekspor Feeder PDDikti. */
	private String feeder;
	/** Nomor SK penetapan pembimbing. */
	private String noSk;
	/** Lokasi/ruang pelaksanaan seminar proposal. */
	private String lokasiUjian;
	/** Tanggal SK penetapan pembimbing. */
	private Date tglSk;
	/**
	 * Id {@link Skripsi} yang lahir dari pengajuan ini, disimpan sebagai TEKS pada kolom bernama
	 * {@code filelocation}. Bukan foreign key. Lihat {@link #getSkr()} dan
	 * {@link #setSkr(String)}.
	 */
	private String skr;
	/** Penanda nilai disembunyikan dari mahasiswa; default {@code false}. */
	private Boolean sembunyikanNilaiKemahasiswa;
	/** Penanda pengurutan pertemuan otomatis; default {@code true}. */
	private Boolean urutkanotomatis;

	/**
	 * Implementasi {@link VOPembelajaran}: mengembalikan konfigurasi e-learning/materi sebagai
	 * teks JSON. Bila kolom kosong, dikembalikan objek JSON kosong ({@code "{}"}) supaya pemanggil
	 * bisa langsung mem-parsing tanpa penjaga {@code null}.
	 *
	 * @return teks JSON konfigurasi; tidak pernah {@code null} maupun kosong
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Implementasi {@link VOPembelajaran}: menyetel konfigurasi e-learning/materi sebagai teks
	 * JSON. Tanpa validasi bentuk JSON.
	 *
	 * @param course teks JSON konfigurasi
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Mengembalikan tanggal pelaksanaan seminar proposal apa adanya (tanpa pengisian otomatis,
	 * berbeda dari {@link #getWaktuSeminar()}).
	 *
	 * @return tanggal seminar, atau {@code null} bila belum dijadwalkan
	 */
	public Date getTanggalSeminar() {
		return tanggalSeminar;
	}

	/**
	 * Menyetel tanggal pelaksanaan seminar proposal.
	 *
	 * @param tanggalSeminar tanggal seminar
	 */
	public void setTanggalSeminar(Date tanggalSeminar) {
		this.tanggalSeminar = tanggalSeminar;
	}

	/**
	 * Mengembalikan jam mulai seminar proposal sebagai teks.
	 *
	 * <p><b>Getter berefek samping</b>: bila kosong, diisi dengan JAM SAAT INI
	 * ({@code Common.timeFormat} atas {@code WaktuUtil.getDate()}) dan nilai itu menempel pada
	 * entity — pengajuan lama yang belum punya jam seminar akan mendapat jam saat pertama kali
	 * layar/laporan membacanya, bukan jam seminar sesungguhnya.</p>
	 *
	 * @return jam mulai seminar; tidak pernah kosong
	 */
	public String getWaktuSeminar() {
		if (waktuSeminar == null || waktuSeminar.trim().isEmpty()) {
			waktuSeminar = Common.timeFormat.get().format(ais.ui.util.WaktuUtil.getDate());
		}
		return waktuSeminar;
	}

	/**
	 * Menyetel jam mulai seminar proposal.
	 *
	 * @param waktuSeminar jam mulai (teks, format {@code Common.timeFormat})
	 */
	public void setWaktuSeminar(String waktuSeminar) {
		this.waktuSeminar = waktuSeminar;
	}

	/**
	 * Mengembalikan jam selesai seminar proposal sebagai teks; berperilaku sama persis dengan
	 * {@link #getWaktuSeminar()}, termasuk pengisian otomatis dengan jam saat ini bila kosong.
	 *
	 * @return jam selesai seminar; tidak pernah kosong
	 * @see #getWaktuSeminar()
	 */
	public String getWaktuSampaiSeminar() {
		if (waktuSampaiSeminar == null || waktuSampaiSeminar.trim().isEmpty()) {
			waktuSampaiSeminar = Common.timeFormat.get().format(ais.ui.util.WaktuUtil.getDate());
		}
		return waktuSampaiSeminar;
	}

	/**
	 * Menyetel jam selesai seminar proposal.
	 *
	 * @param waktuSampaiSeminar jam selesai (teks)
	 */
	public void setWaktuSampaiSeminar(String waktuSampaiSeminar) {
		this.waktuSampaiSeminar = waktuSampaiSeminar;
	}

	/**
	 * Mengembalikan catatan/ketentuan perbaikan yang dicetak pada berita acara seminar.
	 *
	 * <p><b>Getter berefek samping (migrasi saat baca)</b>: bila catatan masih kosong ATAU isinya
	 * persis sama dengan teks default versi lama ({@link #CATATAN_SEMINAR_DEFAULT_LAMA}, setelah
	 * normalisasi {@code \r\n} &rarr; {@code \n} dan {@code trim}), field ditimpa dengan teks
	 * default versi berlaku {@link #CATATAN_SEMINAR_DEFAULT}. Dengan begitu berita acara lama ikut
	 * memakai ketentuan 14 hari kerja yang baru begitu dibuka kembali. Catatan yang sudah disunting
	 * manual tidak tersentuh.</p>
	 *
	 * <p>Perhatikan: normalisasi {@code \r\n} hanya dipakai untuk PEMBANDINGAN; nilai yang
	 * dikembalikan tetap isi field aslinya (atau default baru), tanpa {@code trim}.</p>
	 *
	 * @return catatan berita acara seminar; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCatatanSeminar() {
		String nilaiCatatan = catatanSeminar == null ? "" : catatanSeminar.replace("\r\n", "\n").trim();
		if (nilaiCatatan.isEmpty() || CATATAN_SEMINAR_DEFAULT_LAMA.equals(nilaiCatatan)) {
			catatanSeminar = CATATAN_SEMINAR_DEFAULT;
		}
		return catatanSeminar;
	}

	/**
	 * Menyetel catatan berita acara seminar. Menyimpan teks yang sama persis dengan
	 * {@link #CATATAN_SEMINAR_DEFAULT_LAMA} akan membuatnya ditimpa lagi saat dibaca.
	 *
	 * @param catatanSeminar catatan berita acara
	 */
	public void setCatatanSeminar(String catatanSeminar) {
		this.catatanSeminar = catatanSeminar;
	}

	/**
	 * Menyatakan apakah hasil seminar dinyatakan tanpa perbaikan; {@code false} bila belum diisi.
	 *
	 * @return {@code true} bila tanpa perbaikan; tidak pernah {@code null}
	 */
	public Boolean getTanpaPerbaikan() {
		return tanpaPerbaikan == null ? false : tanpaPerbaikan;
	}

	/**
	 * Menyetel penanda hasil seminar tanpa perbaikan.
	 *
	 * @param tanpaPerbaikan {@code true} bila tanpa perbaikan
	 */
	public void setTanpaPerbaikan(Boolean tanpaPerbaikan) {
		this.tanpaPerbaikan = tanpaPerbaikan;
	}

	/**
	 * Mengembalikan catatan per dosen sebagai teks JSON (objek berkunci id dosen).
	 *
	 * <p>Bila kolom kosong, dikembalikan objek JSON kosong ({@code "{}"}) supaya pemanggil bisa
	 * langsung mem-parsing. Pola yang sama dipakai {@link #getCourse()}.</p>
	 *
	 * @return teks JSON catatan dosen; tidak pernah {@code null} maupun kosong
	 */
	@Column(columnDefinition = "text")
	public String getCatatanDosen() {
		return catatanDosen == null || catatanDosen.trim().isEmpty() ? new JSONObject().toString()
				: catatanDosen.trim();
	}

	/**
	 * Menyetel catatan per dosen (teks JSON). Tanpa validasi bentuk JSON.
	 *
	 * @param catatanDosen teks JSON catatan dosen
	 */
	public void setCatatanDosen(String catatanDosen) {
		this.catatanDosen = catatanDosen;
	}

	/**
	 * Merangkum enam slot dosen menjadi daftar {@link CommonVO} siap tampil, lengkap dengan label
	 * dan kode peran dari {@link FormatNilaiProposalSkripsi}.
	 *
	 * <p>Tiap entri {@link CommonVO} berisi: id dosen sebagai teks ({@code ""} bila slot kosong),
	 * label peran ({@code FormatNilaiProposalSkripsi.getDosenN()}, mis. "Pembimbing 1"), objek
	 * {@link Dosen}-nya, dan kode peran ({@code getKodeN()}).</p>
	 *
	 * <p>Sebuah slot hanya masuk daftar bila slot itu <b>aktif</b> pada master format nilai
	 * ({@code getDosenNAktif()}) dan — bila {@code semua} bernilai {@code false} — dosennya sudah
	 * terisi. Bila {@link #getFormatNilaiProposalSkripsi()} {@code null}, daftar yang dikembalikan
	 * kosong.</p>
	 *
	 * <p><b>Efek samping</b>: memanggil {@link #getDosen1()}..{@link #getDosen6()}, jadi slot yang
	 * nonaktif ikut dikosongkan dari entity (lihat peringatan di {@link #getDosen1()}). Getter
	 * dosen dan getter format dipanggil berkali-kali per slot; tidak ada caching.</p>
	 *
	 * <p>Dipanggil dari layar penilaian dan ekspor Feeder
	 * ({@code PenilaianProposalSkripsiHelper}, {@code AktifitasSkripsiHelper},
	 * {@code EksporPesertaDosenBimbinganFeeder}, dan lain-lain).</p>
	 *
	 * @param semua {@code true} untuk menyertakan slot aktif yang dosennya masih kosong
	 *              (dipakai form pengisian); {@code false} untuk hanya slot yang sudah terisi
	 * @return daftar slot dosen; kosong bila format nilai belum dipilih, tidak pernah {@code null}
	 */
	public List<CommonVO> dataDosen(boolean semua) {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = this;
		if ((semua || mahasiswaRequestTugasAkhir.getDosen1() != null)
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1Aktif()) {
			commonVOs.add(new CommonVO(
					mahasiswaRequestTugasAkhir.getDosen1() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen1().getId().toString(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1(),
					mahasiswaRequestTugasAkhir.getDosen1(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKode1()));
		}
		if ((semua || mahasiswaRequestTugasAkhir.getDosen2() != null)
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2Aktif()) {
			commonVOs.add(new CommonVO(
					mahasiswaRequestTugasAkhir.getDosen2() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen2().getId().toString(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2(),
					mahasiswaRequestTugasAkhir.getDosen2(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKode2()));
		}
		if ((semua || mahasiswaRequestTugasAkhir.getDosen3() != null)
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3Aktif()) {
			commonVOs.add(new CommonVO(
					mahasiswaRequestTugasAkhir.getDosen3() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen3().getId().toString(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3(),
					mahasiswaRequestTugasAkhir.getDosen3(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKode3()));
		}
		if ((semua || mahasiswaRequestTugasAkhir.getDosen4() != null)
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4Aktif()) {
			commonVOs.add(new CommonVO(
					mahasiswaRequestTugasAkhir.getDosen4() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen4().getId().toString(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4(),
					mahasiswaRequestTugasAkhir.getDosen4(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKode4()));
		}
		if ((semua || mahasiswaRequestTugasAkhir.getDosen5() != null)
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5Aktif()) {
			commonVOs.add(new CommonVO(
					mahasiswaRequestTugasAkhir.getDosen5() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen5().getId().toString(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5(),
					mahasiswaRequestTugasAkhir.getDosen5(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKode5()));
		}
		if ((semua || mahasiswaRequestTugasAkhir.getDosen6() != null)
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
				&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6Aktif()) {
			commonVOs.add(new CommonVO(
					mahasiswaRequestTugasAkhir.getDosen6() == null ? ""
							: mahasiswaRequestTugasAkhir.getDosen6().getId().toString(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6(),
					mahasiswaRequestTugasAkhir.getDosen6(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKode6()));
		}
		return commonVOs;
	}

	/**
	 * Menghitung nilai akhir seorang dosen lalu <b>menyimpannya</b> ke slot {@code nilaiDosenN}
	 * yang sesuai dengan peran ({@code jenis}) dosen tersebut.
	 *
	 * <p>Peran dicocokkan sebagai teks dengan label slot pada
	 * {@link FormatNilaiProposalSkripsi} ({@code getDosen1()}..{@code getDosen6()}); slot pertama
	 * yang labelnya sama dengan {@code jenis} yang dipakai. Nilainya sendiri berasal dari
	 * {@link #hitungTotalNilai(Boolean, Dosen)}.</p>
	 *
	 * <p><b>Efek samping</b>: menulis {@code nilaiDosenN} (dan, lewat
	 * {@link #hitungTotalNilai(Boolean, Dosen)}, juga bisa menulis {@code detailNilai}). Perubahan
	 * ini tersimpan saat flush.</p>
	 *
	 * <p><b>Jebakan</b>: pencocokan dilakukan atas label yang bisa diubah pengguna di master
	 * format nilai. Mengubah label slot membuat pemanggil lama tidak lagi menemukan slotnya dan
	 * method mengembalikan {@code 0.0} tanpa menyimpan apa pun — tidak ada pesan galat.
	 * {@code jenis} yang {@code null} akan melempar {@code NullPointerException}.</p>
	 *
	 * <p>Dipanggil dari {@code PenilaianProposalSkripsiHelper} dan
	 * {@code PenilaianSkripsiHelper} setiap kali seorang dosen menyimpan penilaiannya.</p>
	 *
	 * @param dosen dosen penilai
	 * @param jenis label peran dosen sesuai {@link FormatNilaiProposalSkripsi}
	 * @param gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase diteruskan ke
	 *        {@link #hitungTotalNilai(Boolean, Dosen)}; {@code true} membersihkan komponen yang
	 *        sudah tidak berlaku lebih dulu
	 * @return nilai yang disimpan, atau {@code 0.0} bila peran tidak cocok dengan slot mana pun
	 */
	public Double cariNilaiDariDosen(Dosen dosen, String jenis,
			Boolean gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase) {
		Double nilaiPembimbing = 0.0;
		MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = this;
		if (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null) {
			if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1())) {
				mahasiswaRequestTugasAkhir.setNilaiDosen1(nilaiPembimbing = mahasiswaRequestTugasAkhir.hitungTotalNilai(
						gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen));
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2())) {
				mahasiswaRequestTugasAkhir.setNilaiDosen2(nilaiPembimbing = mahasiswaRequestTugasAkhir.hitungTotalNilai(
						gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen));
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3())) {
				mahasiswaRequestTugasAkhir.setNilaiDosen3(nilaiPembimbing = mahasiswaRequestTugasAkhir.hitungTotalNilai(
						gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen));
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4())) {
				mahasiswaRequestTugasAkhir.setNilaiDosen4(nilaiPembimbing = mahasiswaRequestTugasAkhir.hitungTotalNilai(
						gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen));
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5())) {
				mahasiswaRequestTugasAkhir.setNilaiDosen5(nilaiPembimbing = mahasiswaRequestTugasAkhir.hitungTotalNilai(
						gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen));
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6())) {
				mahasiswaRequestTugasAkhir.setNilaiDosen6(nilaiPembimbing = mahasiswaRequestTugasAkhir.hitungTotalNilai(
						gunakanProposalSkripsiPunyaKomponenPenilaianProposalSkripsiDariDatabase, dosen));
			}
		}
		return nilaiPembimbing;
	}

	/**
	 * Menempatkan seorang {@link Dosen} pada slot {@code dosenN} yang labelnya cocok dengan peran
	 * ({@code jenis}) pada {@link FormatNilaiProposalSkripsi}.
	 *
	 * <p>Pasangan penetapan dari {@link #cariNilaiDariDosen(Dosen, String, Boolean)}: yang satu
	 * mengisi orangnya, yang lain mengisi nilainya, keduanya memakai pencocokan label yang sama
	 * dan karenanya berbagi jebakan yang sama — label yang berubah membuat penetapan gagal
	 * diam-diam, dan {@code jenis} {@code null} melempar {@code NullPointerException}.</p>
	 *
	 * <p><b>Efek samping</b>: menulis field {@code dosenN}. Tidak memeriksa kuota dosen; layar
	 * pemanggil ({@code MahasiswaRequestTugasAkhirAction}, {@code SkripsiAction},
	 * {@code PenilaianSkripsiHelper}) memakai
	 * {@link #checkMaksSksDosen(Dosen, String, String, Integer)} lebih dulu.</p>
	 *
	 * @param dosen dosen yang ditetapkan; {@code null} mengosongkan slot yang cocok
	 * @param jenis label peran sesuai {@link FormatNilaiProposalSkripsi}
	 */
	public void simpanDosen(Dosen dosen, String jenis) {
		MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = this;
		if (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null) {
			if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1())) {
				mahasiswaRequestTugasAkhir.setDosen1(dosen);
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2())) {
				mahasiswaRequestTugasAkhir.setDosen2(dosen);
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3())) {
				mahasiswaRequestTugasAkhir.setDosen3(dosen);
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4())) {
				mahasiswaRequestTugasAkhir.setDosen4(dosen);
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5())) {
				mahasiswaRequestTugasAkhir.setDosen5(dosen);
			} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6())) {
				mahasiswaRequestTugasAkhir.setDosen6(dosen);
			}
		}
	}

	/**
	 * Mengembalikan daftar referensi/pustaka tugas akhir sebagai teks JSON <b>array</b>.
	 *
	 * <p>Bila kolom kosong, dikembalikan array JSON kosong ({@code "[]"}) — perhatikan bedanya
	 * dengan {@link #getCourse()}/{@link #getCatatanDosen()} yang memakai objek ({@code "{}"}).
	 * Daftar ini juga dibayangi/dipakai kembali oleh {@code Skripsi.getReferensi()}.</p>
	 *
	 * @return teks JSON array referensi; tidak pernah {@code null} maupun kosong
	 */
	@Column(columnDefinition = "text")
	public String getReferensi() {
		return referensi == null || referensi.trim().isEmpty() ? new JSONArray().toString() : referensi.trim();
	}

	/**
	 * Menyetel daftar referensi/pustaka (teks JSON array). Tanpa validasi bentuk JSON.
	 *
	 * @param referensi teks JSON array referensi
	 */
	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	/**
	 * Mengembalikan nomor SK penetapan pembimbing, sudah di-{@code trim()}.
	 *
	 * @return nomor SK; {@code ""} bila belum ada, tidak pernah {@code null}
	 */
	public String getNoSk() {
		return noSk == null ? "" : noSk.trim();
	}

	/**
	 * Menyetel nomor SK penetapan pembimbing.
	 *
	 * @param noSk nomor SK
	 */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/**
	 * Mengembalikan tanggal SK penetapan pembimbing (dipetakan sebagai {@code DATE}, tanpa jam).
	 *
	 * @return tanggal SK, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/**
	 * Menyetel tanggal SK penetapan pembimbing.
	 *
	 * @param tglSk tanggal SK
	 */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}

	/**
	 * Mengembalikan kode/identitas ekspor Feeder PDDikti untuk pengajuan ini.
	 *
	 * <p>Berbeda dari getter teks lain di kelas ini, nilai kosong dinormalkan menjadi {@code null}
	 * (bukan {@code ""}) — pemanggil di modul Feeder memakai {@code null} sebagai penanda "belum
	 * pernah diekspor".</p>
	 *
	 * @return kode Feeder, atau {@code null} bila belum diekspor
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menyetel kode/identitas ekspor Feeder PDDikti.
	 *
	 * @param feeder kode Feeder
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Memuat entity {@link Skripsi} yang lahir dari pengajuan ini, berdasarkan id yang tersimpan
	 * di {@link #getSkr()}.
	 *
	 * <p>Bukan relasi Hibernate: id dibaca sebagai teks, divalidasi harus berupa angka
	 * ({@code Common.isNumber}), lalu entity diambil lewat
	 * {@code DataUtil.ambilData(Skripsi.class, id, true)} — yaitu dari konstanta/cache MapDB, dan
	 * bila tidak ketemu, dari database lewat session tersendiri yang ditutup tuntas di
	 * {@code finally} oleh {@code DataUtil}.</p>
	 *
	 * <p>Konsekuensi praktis: pemanggilan ini bisa memicu SATU query database per pengajuan pada
	 * cache-miss. Karena {@link #getStatus()} memanggilnya, menampilkan daftar panjang pengajuan
	 * berarti sederet query terpisah (pola N+1) yang tak terlihat dari kode pemanggil.</p>
	 *
	 * <p>Seluruh galat ditelan (dicetak + dicatat ke audit) dan menghasilkan {@code null}.</p>
	 *
	 * @return skripsi terkait, atau {@code null} bila belum ada / id tidak valid / gagal dimuat
	 * @see #getSkr()
	 */
	public Skripsi ambilSkripsi() {
		try {
			Skripsi skripsi = (Skripsi) (getSkr() == null || !Common.isNumber(getSkr()) ? null
					: ambilData(Skripsi.class, getSkr(), true));
			return skripsi;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/MahasiswaRequestTugasAkhir.java:1360");
		}
		return null;
	}

	/**
	 * Mengembalikan id {@link Skripsi} yang lahir dari pengajuan ini, sebagai teks.
	 *
	 * <h4>Dua tempat penyimpanan, berkas menang atas kolom</h4>
	 * <p>Nilai ini punya dua salinan:</p>
	 * <ol>
	 *   <li><b>Kolom database</b> {@code filelocation} (nama kolomnya peninggalan pemakaian lama —
	 *   isinya id skripsi, bukan lokasi berkas), lewat field {@code skr} yang dipetakan getter
	 *   ini.</li>
	 *   <li><b>Berkas cache sisi aplikasi</b> yang ditulis
	 *   {@code GeneralValueObject.put(nilai, "skr")} dan dibaca {@code retreive("skr")}.</li>
	 * </ol>
	 * <p>Getter ini membaca berkas lebih dulu; bila berkas berisi sesuatu yang tidak kosong,
	 * <b>isinya menimpa field</b> dan itulah yang dikembalikan. Kolom database hanya menjadi
	 * cadangan ketika berkas kosong/hilang (mis. server baru, direktori cache dibersihkan, atau
	 * selama masa startup — {@code retreive} sengaja mengembalikan {@code ""} saat
	 * {@code AppStartupListener.isStartupInProgress()}).</p>
	 *
	 * <h4>Efek samping</h4>
	 * <p>Menulis field {@code skr} bila berkas berisi nilai yang berbeda; pada entity terpasang
	 * session ini bisa berujung {@code UPDATE}. Galat baca berkas ditelan (dicetak + dicatat ke
	 * audit) dan nilai field lama dikembalikan.</p>
	 *
	 * <h4>Jebakan yang tercatat</h4>
	 * <p>Karena berkas menang atas kolom, sebuah nilai lama yang tertinggal di berkas dapat
	 * "menghidupkan kembali" tautan skripsi yang sudah diubah di database. Perhatikan juga
	 * asimetri dengan {@link #setSkr(String)}: setter hanya menulis berkas bila nilainya berupa
	 * ANGKA, sedangkan field selalu ditimpa. Menyetel nilai non-angka karena itu mengubah kolom
	 * saja, lalu getter berikutnya akan mengembalikannya ke nilai angka lama dari berkas.</p>
	 *
	 * @return id skripsi sebagai teks, atau {@code null} bila pengajuan belum menghasilkan skripsi
	 * @see #setSkr(String)
	 * @see #ambilSkripsi()
	 * @see ais.database.model.GeneralValueObject#retreive(String)
	 */
	@Column(name = "filelocation")
	public String getSkr() {
		try {
			String s = retreive("skr");
			if (s != null && !s.isEmpty()) {
				skr = s;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/MahasiswaRequestTugasAkhir.java:1373");
		}
		return skr;
	}

	/**
	 * Menautkan pengajuan ini ke sebuah {@link Skripsi} dengan menyimpan id skripsi sebagai teks.
	 *
	 * <h4>SIAPA yang memanggil ini — inti mekanisme {@code skr}</h4>
	 * <p>Pemanggil utamanya BUKAN kode pengajuan, melainkan getter di sisi seberang:
	 * {@link Skripsi#getMahasiswaRequestTugasAkhir()} memanggil {@code setSkr(getId())} setiap kali
	 * ia mengembalikan pengajuan yang tertaut. Artinya membuka layar/laporan skripsi — operasi yang
	 * terasa "hanya membaca" — akan <b>menulis</b> ke entity pengajuan ini. Bila pengajuan masih
	 * terpasang pada session, Hibernate menandainya kotor dan menerbitkan
	 * {@code UPDATE mahasiswa_request_tugas_akhir} pada flush berikutnya, dan {@code put(...)} di
	 * dalam setter ini menulis satu berkas cache. Inilah alasan tautan balik pengajuan &rarr;
	 * skripsi umumnya terisi sendiri tanpa ada layar yang secara eksplisit mengisinya.</p>
	 *
	 * <h4>Apa yang ditulis</h4>
	 * <ul>
	 *   <li>Ke <b>berkas cache</b> lewat {@code put(skr, "skr")} — HANYA bila {@code skr} berupa
	 *   angka ({@code Common.isNumber}). Penulisan berkas dilewati selama startup aplikasi (lihat
	 *   {@link ais.database.model.GeneralValueObject#put(String, String)}).</li>
	 *   <li>Ke <b>field/kolom</b> {@code filelocation} — SELALU, apa pun isinya, termasuk
	 *   {@code null} dan teks bukan angka.</li>
	 * </ul>
	 *
	 * <h4>Konsekuensi asimetri di atas</h4>
	 * <p>Menyetel nilai bukan angka (atau {@code null}) tidak menghapus berkas cache lama, padahal
	 * {@link #getSkr()} mengutamakan berkas. Jadi tautan skripsi <b>tidak bisa dilepas</b> lewat
	 * setter ini selama berkas cache-nya masih ada — pembacaan berikutnya akan mengembalikan id
	 * lama. Perilaku ini dicatat apa adanya, tidak diperbaiki.</p>
	 *
	 * <p>Karena setter ini dipetakan Hibernate (getter-nya ber-{@code @Column}), Hibernate juga
	 * memanggilnya saat hidrasi entity; penjaga startup pada {@code put(...)} ada justru untuk
	 * mencegah ribuan tulis-berkas pada saat itu.</p>
	 *
	 * <p>Galat penulisan berkas ditelan (dicetak + dicatat ke audit); field tetap disetel.</p>
	 *
	 * @param skr id skripsi sebagai teks; nilai bukan angka hanya mengubah kolom, tidak berkas
	 * @see #getSkr()
	 * @see Skripsi#getMahasiswaRequestTugasAkhir()
	 * @see ais.database.model.GeneralValueObject#put(String, String)
	 */
	public void setSkr(String skr) {
		try {
			if (Common.isNumber(skr)) {
				put(skr, "skr");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/MahasiswaRequestTugasAkhir.java:1384");
		}
		this.skr = skr;
	}

	/**
	 * Mengembalikan lokasi/ruang pelaksanaan seminar proposal, sudah di-{@code trim()}.
	 *
	 * @return lokasi ujian; {@code ""} bila belum diisi, tidak pernah {@code null}
	 */
	public String getLokasiUjian() {
		return lokasiUjian == null ? "" : lokasiUjian.trim();
	}

	/**
	 * Menyetel lokasi/ruang pelaksanaan seminar proposal.
	 *
	 * @param lokasiUjian lokasi/ruang
	 */
	public void setLokasiUjian(String lokasiUjian) {
		this.lokasiUjian = lokasiUjian;
	}

	/**
	 * Menyatakan apakah nilai seminar proposal disembunyikan dari mahasiswa; {@code false} bila
	 * belum diisi.
	 *
	 * @return {@code true} bila nilai disembunyikan; tidak pernah {@code null}
	 */
	public Boolean getSembunyikanNilaiKemahasiswa() {
		return sembunyikanNilaiKemahasiswa == null ? false : sembunyikanNilaiKemahasiswa;
	}

	/**
	 * Menyetel penanda penyembunyian nilai dari mahasiswa.
	 *
	 * @param sembunyikanNilaiKemahasiswa {@code true} untuk menyembunyikan nilai
	 */
	public void setSembunyikanNilaiKemahasiswa(Boolean sembunyikanNilaiKemahasiswa) {
		this.sembunyikanNilaiKemahasiswa = sembunyikanNilaiKemahasiswa;
	}

	/**
	 * Implementasi {@link VOPembelajaran}: menyatakan apakah pertemuan bimbingan diurutkan
	 * otomatis; {@code true} bila belum diisi.
	 *
	 * @return {@code true} bila pengurutan otomatis aktif; tidak pernah {@code null}
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Implementasi {@link VOPembelajaran}: menyetel penanda pengurutan pertemuan otomatis.
	 *
	 * @param urutkanotomatis {@code true} untuk mengurutkan otomatis
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/**
	 * Disposisi SOP yang menaungi pengajuan ini. <b>Field bayangan</b>: {@link VOPembelajaran}
	 * sudah punya field {@code disposisiSop} beserta accessor identik — deklarasi ulang di sini
	 * hanya membuat salinan induk selamanya {@code null}.
	 */
	private DisposisiSop disposisiSop;

	/**
	 * Mengembalikan disposisi SOP yang menaungi pengajuan ini (kolom {@code disposisi_sop}).
	 *
	 * <p><b>Duplikat verbatim</b> dari {@code VOPembelajaran.getDisposisiSop()}; perilakunya sama
	 * persis (nilai dilewatkan {@code check(...)}). Yang berbeda hanyalah field yang dibaca —
	 * lihat catatan field bayangan di atas.</p>
	 *
	 * @return disposisi SOP, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP, dengan penjaga: nilai {@code null} atau yang belum punya id
	 * <b>diabaikan diam-diam</b> (method langsung {@code return}), sehingga disposisi yang sudah
	 * terpasang tidak bisa dilepas lewat setter ini.
	 *
	 * <p>Ekspresi ternary setelah penjaga itu sudah tidak pernah memilih cabang pertamanya —
	 * kondisi {@code disposisiSop == null || disposisiSop.getId() == null} mustahil bernilai
	 * {@code true} di titik tersebut. Sisa kode mati ini disalin apa adanya dari
	 * {@code VOPembelajaran.setDisposisiSop(DisposisiSop)}; dicatat, tidak diubah.</p>
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null} atau belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Menyatakan apakah baris pengajuan ini masih aktif/berlaku; {@code true} bila belum diisi.
	 *
	 * <p>Berbeda dari {@link #getStatus()}, penanda ini murni data (tanpa logika turunan) dan
	 * dipakai untuk menyembunyikan baris dari daftar tanpa menghapusnya.</p>
	 *
	 * @return {@code true} bila baris masih aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif/berlaku baris pengajuan ini.
	 *
	 * @param aktif {@code true} bila masih aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
