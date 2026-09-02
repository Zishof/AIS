package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity <b>master ujian</b> (tabel {@code public.ujian}) — definisi sebuah ujian/kuis online
 * beserta identitas, kepemilikan akademik, aturan koreksi, syarat akses, dan tata tertibnya.
 *
 * <h3>Kedudukan dalam model ujian online AIS</h3>
 * <p>Ujian online AIS dipecah menjadi tiga lapis yang sering tertukar saat membaca kode:</p>
 * <ol>
 *   <li><b>{@code Ujian}</b> (kelas ini) — <i>cetakan</i>/master ujian: nama, kode, jenis soal,
 *   nilai lulus, level, tata tertib, sertifikat, syarat kelayakan, dan pemilik akademiknya. Satu
 *   {@code Ujian} bisa dipakai berulang kali di banyak kelas/pertemuan.</li>
 *   <li><b>{@link PertemuanPunyaUjian}</b> — <i>penjadwalan</i> master ini pada satu
 *   {@link Pertemuan} tertentu: tanggal mulai/selesai, durasi ({@code lama}), acak atau tidak
 *   ({@code random}), jumlah soal ditampilkan, format nilai, sampai seluruh setelan anti-curang.
 *   <b>Jadwal dan durasi TIDAK ada di kelas ini</b> — jangan mencarinya di sini.</li>
 *   <li><b>{@link HasilUjianMahasiswa}</b> — pengerjaan satu peserta pada satu penjadwalan.</li>
 * </ol>
 *
 * <h3>Kumpulan soal: relasi lewat {@link UjianPunyaSoal}, bukan koleksi terpetakan</h3>
 * <p>Hubungan ke {@link BankSoal} bersifat many-to-many yang diwujudkan lewat entity penghubung
 * {@link UjianPunyaSoal} ({@code ujian} + {@code bankSoal} + {@code nomorUrut}). Perhatikan bahwa
 * kelas ini <b>tidak punya properti koleksi {@code Set<UjianPunyaSoal>} yang dipetakan
 * Hibernate</b>. Sebagai gantinya dipakai <b>indeks JSON berbasis berkas</b>:</p>
 * <ul>
 *   <li>Berkas indeks per-ujian diambil lewat
 *   {@code Common.getFileLocation(this, "ujian_punya_soal_" + getId())}.</li>
 *   <li>Isinya sebuah {@code JSONObject} datar yang <b>hanya menyimpan ID</b>
 *   {@link UjianPunyaSoal} — bukan isi soal: kunci = ID, nilai = ID yang sama. Nilai string
 *   <b>kosong</b> berarti <i>nisan</i> (tombstone) dari baris yang sudah dihapus, lihat
 *   {@link #removeUjianPunyaSoal(Serializable)}.</li>
 *   <li>Isi sebenarnya selalu diambil ulang dari database lewat
 *   {@code GeneralValueObject.ambilData(UjianPunyaSoal.class, id)}, jadi indeks ini murni daftar
 *   kunci untuk menghindari query koleksi berulang saat ujian berjalan.</li>
 *   <li>Sinkronisasi otomatis dilakukan {@link ais.database.hibernate.AuditListener}: setiap
 *   {@code UjianPunyaSoal} yang tersimpan memanggil {@link #populateUjianPunyaSoal}, dan setiap
 *   yang terhapus memanggil {@link #removeUjianPunyaSoal(Serializable)}.</li>
 *   <li>Pembangunan ulang total dilakukan {@link #reInitUjianPunyaSoal(Session)}, dipicu otomatis
 *   pada pemanggilan pertama lewat penanda {@code udah("ujian")}
 *   ({@link GeneralValueObject#udah(String)}).</li>
 * </ul>
 *
 * <h3>Dua domain akademik dalam satu tabel</h3>
 * <p>AIS melayani perguruan tinggi dan sekolah dari satu basis kode, sehingga entity ini punya
 * dua set relasi kepemilikan yang saling melengkapi (umumnya hanya satu set yang terisi):</p>
 * <ul>
 *   <li><b>Perguruan tinggi</b>: {@link #getFakultas()}, {@link #getJurusan()},
 *   {@link #getDosen()}, {@link #getMatakuliah()}.</li>
 *   <li><b>Sekolah</b>: {@link #getYayasan()}, {@link #getSekolah()}, {@link #getGuru()},
 *   {@link #getMatapelajaran()}.</li>
 * </ul>
 * <p>Relasi-relasi ini dipakai sebagai penyaring pada bandbox pemilihan ujian (lihat
 * {@code ais.action.master.helper.generic.AmbilDataUjianBanyak}), bukan sebagai penegak hak akses.
 * Penyaring tambahan {@link #getDiperuntukkan()} membatasi ujian pada jenis konteks pemakainya.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Bayangan field audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}. Lihat catatan arsitektur
 *   di bawah.</li>
 *   <li><b>Identitas &amp; deskripsi</b> — {@link #getKode()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #toString()}, {@link #getLevel()},
 *   {@link #getAktif()}.</li>
 *   <li><b>Aturan penilaian</b> — {@link #getJenis()}, {@link #getJenisKoreksi()},
 *   {@link #getNilaiLulus()}, {@link #getSertifikat()},
 *   {@link #getTampilanHurufDiPilihanJawaban()}.</li>
 *   <li><b>Relasi kepemilikan akademik</b> — delapan getter/setter pada daftar di atas, plus
 *   {@link #getDiperuntukkan()}.</li>
 *   <li><b>Gerbang kelayakan</b> — {@link #getSyaratUjian()} (aturan terstruktur, dievaluasi
 *   {@code SyaratUjianAction.checkSyaratSyaratUjian(...)}) dan {@link #getSyaratAkses()}
 *   (prasyarat materi/tugas dalam bentuk JSON bebas).</li>
 *   <li><b>Teks tata tertib</b> — {@link #getTatatertibUjian()} beserta nilai bawaannya.</li>
 *   <li><b>Indeks soal berbasis berkas</b> — {@link #ambilLokasiUjianPunyaSoal()},
 *   {@link #tulisLokasiUjianPunyaSoal(String)}, {@link #bersihkanLokasiUjianPunyaSoal()},
 *   {@link #reInitUjianPunyaSoal(Session)}, {@link #populateUjianPunyaSoal},
 *   {@link #removeUjianPunyaSoal(Serializable)}.</li>
 *   <li><b>Query soal untuk UI/mesin ujian</b> — tiga kelebihan-beban
 *   {@code ambilUjianPunyaSoal(...)} dan {@link #ambilBankSoal(PertemuanPunyaUjian, boolean)},
 *   ditopang {@link #closeNativeSessionQuietly(Session)}.</li>
 * </ol>
 *
 * <h3>Catatan arsitektur: field audit dideklarasikan ulang di sini</h3>
 * <p>Kelas ini {@code extends} {@link GeneralValueObject}, tetapi induk tersebut <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * sama sekali tidak memetakan propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity, termasuk di
 * sini. Ini keharusan teknis arsitektur, bukan kelalaian. Kontrak umum method warisan
 * ({@code check}, {@code udah}, {@code ambilData}, {@code filterTidakBolehSederhana}, dan
 * kawan-kawan) didokumentasikan lengkap di {@link GeneralValueObject}.</p>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Pemetaan berbasis properti.</b> Karena {@code @Id} dipasang pada getter
 *   {@link #getId()}, Hibernate membaca SELURUH getter tanpa {@code @Transient} sebagai kolom —
 *   termasuk getter yang tidak beranotasi sama sekali seperti {@link #getLevel()},
 *   {@link #getJenis()}, atau {@link #getDiperuntukkan()}. Konsekuensinya besar: <b>getter yang
 *   mengubah field-nya sendiri akan ikut tersimpan ke database</b> pada flush berikutnya lewat
 *   dirty checking, walau pemanggil hanya bermaksud "membaca".</li>
 *   <li><b>Getter yang menulis balik ke field.</b> Sudah diverifikasi ada empat:
 *   {@link #getJenis()}, {@link #getJenisKoreksi()}, {@link #getLevel()}, dan
 *   {@link #getTatatertibUjian()}. Yang paling berdampak adalah {@code getTatatertibUjian()},
 *   karena ia menulis balik hasil penyaringan XSS ke field — perubahan teks bersifat permanen.
 *   Sebagai pembanding, {@link #getNilaiLulus()}, {@link #getAktif()},
 *   {@link #getTampilanHurufDiPilihanJawaban()}, {@link #getNama()}, dan
 *   {@link #getSyaratAkses()} hanya mengembalikan nilai bawaan tanpa menulis balik.</li>
 *   <li><b>Getter relasi memanggil {@code check()}.</b> {@link #getJurusan()},
 *   {@link #getFakultas()}, {@link #getDosen()}, {@link #getSyaratUjian()},
 *   {@link #getYayasan()}, dan {@link #getSekolah()} — semuanya relasi {@code FetchType.LAZY} —
 *   menugaskan ulang hasil {@link GeneralValueObject#check(Object)} ke field. Ini penyeragaman
 *   instance/anti-{@code LazyInitializationException}, bukan perubahan data.</li>
 *   <li><b>Satu-satunya method yang menutup sesi Hibernate</b> adalah
 *   {@link #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)} lewat
 *   {@link #closeNativeSessionQuietly(Session)}, dan hanya pada jalur pembangunan ulang indeks.
 *   Tidak ada getter properti yang menutup sesi.</li>
 * </ul>
 *
 * <p>Entity ini {@code @Audited} (Envers, tabel riwayat {@code ujian_AUD}) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} sehingga SQL hanya memuat kolom yang benar-benar
 * berubah.</p>
 *
 * @see PertemuanPunyaUjian
 * @see UjianPunyaSoal
 * @see BankSoal
 * @see SyaratUjian
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ujian")

//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="ujian")
public class Ujian extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi. Nilainya kebetulan sama dengan milik
	 * {@link PertemuanPunyaUjian} (sisa salin-tempel saat kelas ini dibuat); tidak berdampak
	 * karena {@code serialVersionUID} hanya dibandingkan antar-versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code public.ujian.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna yang mengubah baris ini.
	 *
	 * <p><b>Perhatikan:</b> masukan {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * — nilai lama dipertahankan. Jejak audit sengaja dibuat tidak bisa dikosongkan lewat setter
	 * ini.</p>
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan kosong/{@code null} diabaikan diam-diam
	 * sehingga nilai audit lama tidak bisa terhapus.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} dijalankan,
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} agar {@code oleh}, {@code olehId},
	 * dan {@code tanggal_dirubah} terisi dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Tidak untuk dipanggil langsung dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui
	 * {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (bayangan field audit).
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Wakil teks ujian untuk komponen UI (label, combobox, bandbox): mengembalikan {@link #nama}
	 * apa adanya, atau string kosong bila nama belum diisi — jadi tidak pernah {@code null}.
	 *
	 * <p>Berbeda dengan {@link #getNama()}, method ini membaca field secara langsung sehingga
	 * <b>tidak</b> memangkas spasi di tepi.</p>
	 *
	 * @return nama ujian, atau {@code ""} bila belum ada
	 */
	public String toString() {
		return nama == null ? "" : nama;
	}

	/** Kode pendek ujian yang diketik operator; lihat {@link #getKode()}. */
	private String kode;

	/**
	 * Mengembalikan kode pendek ujian.
	 *
	 * <p>Kolom bebas tanpa keunikan yang ditegakkan basis data; dipakai sebagai penanda manual
	 * pada daftar dan laporan.</p>
	 *
	 * @return kode ujian, atau {@code null} bila tidak diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan kode pendek ujian.
	 *
	 * @param kode kode ujian; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** Nama ujian yang tampil ke pengguna; kolom {@code nama}, wajib isi. */
	private String nama;
	/** Teks HTML tata tertib yang ditampilkan sebelum/ sesudah ujian; lihat {@link #getTatatertibUjian()}. */
	private String tatatertibUjian;
	/** Keterangan bebas; kolom {@code keterangan}. */
	private String keterangan;
	/**
	 * Jenis soal ({@link BankSoal#PILIHAN_GANDA} / {@link BankSoal#ESAY}).
	 *
	 * <p><b>Nilai turunan.</b> Meski punya setter sendiri, isinya selalu ditulis ulang oleh
	 * {@link #getJenis()} berdasarkan {@link #jenisKoreksi}.</p>
	 */
	private String jenis = BankSoal.PILIHAN_GANDA;
	/** Tingkat kesulitan/label level ({@code "Semua Level"} atau {@code "Level 1".."Level 10"}). */
	private String level;
	/** Ambang nilai kelulusan ujian; bawaan {@code 50.0} bila kosong. */
	private Double nilaiLulus;
	/** Fakultas pemilik (domain perguruan tinggi); relasi lazy. */
	private Fakultas fakultas;
	/** Jurusan/program studi pemilik (domain perguruan tinggi); relasi lazy. */
	private Jurusan jurusan;
	/** Templat sertifikat yang diterbitkan bagi peserta lulus; boleh {@code null}. */
	private Sertifikat sertifikat;

	/** Dosen penyusun/penanggung jawab (domain perguruan tinggi). */
	private Dosen dosen;
	/** Matakuliah terkait (domain perguruan tinggi). */
	private Matakuliah matakuliah;

	/** Yayasan pemilik (domain sekolah); relasi lazy. */
	private Yayasan yayasan;
	/** Sekolah pemilik (domain sekolah); relasi lazy. */
	private Sekolah sekolah;

	/** Guru penyusun/penanggung jawab (domain sekolah). */
	private Guru guru;
	/** Mata pelajaran terkait (domain sekolah). */
	private Matapelajaran matapelajaran;

	/** Penanda ujian masih dipakai; bawaan {@code true} bila kosong. */
	private Boolean aktif;
	/** Menampilkan huruf A/B/C/D di depan pilihan jawaban; bawaan {@code true} bila kosong. */
	private Boolean tampilanHurufDiPilihanJawaban;
	/** Aturan kelayakan terstruktur yang harus dipenuhi peserta; lihat {@link SyaratUjian}. */
	private SyaratUjian syaratUjian;
	/** Prasyarat materi/tugas dalam bentuk JSON; lihat {@link #getSyaratAkses()}. */
	private String syaratAkses;
	/** Cara koreksi hasil; lihat {@link #getJenisKoreksi()}. */
	private String jenisKoreksi;

	/**
	 * Nama sederhana kelas konteks pemakai ujian ini; lihat {@link #getDiperuntukkan()}.
	 */
	private String diperuntukkan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Field {@link #jenis} sudah terisi {@link BankSoal#PILIHAN_GANDA} dan
	 * {@link #tanggal_dirubah} terisi waktu server dari inisialisasi field.</p>
	 */
	public Ujian() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} bertipe identity (dibangkitkan basis data) dan {@code insertable = false}
	 * — jadi nilai yang di-{@code set} manual tidak akan ikut pada {@code INSERT}. Selama ID masih
	 * {@code null}, seluruh method indeks soal berbasis berkas berperilaku no-op karena nama
	 * berkasnya ikut memuat ID.</p>
	 *
	 * @return ID ujian, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id ID ujian
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama ujian dalam bentuk sudah dipangkas spasi tepinya.
	 *
	 * <p>Pemangkasan dilakukan pada nilai kembalian saja — field {@link #nama} tidak diubah,
	 * sehingga tidak ada efek samping penyimpanan. Bandingkan dengan {@link #toString()} yang
	 * mengembalikan nilai mentah.</p>
	 *
	 * @return nama ujian tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama ujian.
	 *
	 * @param nama nama ujian (kolom wajib isi pada basis data, maksimal 255 karakter)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang ujian.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang ujian.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan jenis soal secara langsung.
	 *
	 * <p><b>Praktis tidak berpengaruh.</b> {@link #getJenis()} selalu menghitung ulang nilai
	 * {@link #jenis} dari {@link #getJenisKoreksi()}, sehingga apa pun yang dipasang di sini akan
	 * tertimpa pada pembacaan berikutnya (termasuk pembacaan yang dilakukan Hibernate saat flush).
	 * Untuk benar-benar mengubah jenis soal, pakai
	 * {@link #setJenisKoreksi(String)} — itulah yang dilakukan formulir
	 * {@code ais.action.master.UjianAction}.</p>
	 *
	 * @param jenis {@link BankSoal#PILIHAN_GANDA} atau {@link BankSoal#ESAY}
	 * @see #setJenisKoreksi(String)
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan jenis soal ujian, <b>diturunkan</b> dari cara koreksinya.
	 *
	 * <p>Aturannya kaku dan satu arah: koreksi otomatis berarti soal pilihan ganda, selain itu
	 * esai. Jadi jenis soal bukan data yang berdiri sendiri melainkan bayangan dari
	 * {@link #getJenisKoreksi()}:</p>
	 * <ul>
	 *   <li>{@link PenjelasanBankSoal#KOREKSI_OTOMATIS} &rarr; {@link BankSoal#PILIHAN_GANDA}</li>
	 *   <li>selain itu &rarr; {@link BankSoal#ESAY}</li>
	 * </ul>
	 *
	 * <p><b>Efek samping (getter yang menulis balik):</b> hasil perhitungan ditugaskan ke field
	 * {@link #jenis}. Karena kolom {@code jenis} ikut dipetakan Hibernate lewat akses properti,
	 * nilai baru ini akan tersimpan ke basis data pada flush berikutnya bila entity dalam keadaan
	 * <i>managed</i> — sekadar memanggil getter ini bisa memicu {@code UPDATE}.</p>
	 *
	 * <p>Baris {@code return jenis == null ? PILIHAN_GANDA : jenis} di akhir adalah sisa kode
	 * lama: percabangan di atasnya sudah menjamin {@link #jenis} tidak pernah {@code null}, jadi
	 * cabang bawaannya tak pernah tercapai. Dibiarkan apa adanya.</p>
	 *
	 * @return {@link BankSoal#PILIHAN_GANDA} atau {@link BankSoal#ESAY}, tidak pernah {@code null}
	 * @see #getJenisKoreksi()
	 */
	public String getJenis() {

		if (getJenisKoreksi().equals(PenjelasanBankSoal.KOREKSI_OTOMATIS)) {
			jenis = BankSoal.PILIHAN_GANDA;
		} else {
			jenis = BankSoal.ESAY;
		}

		return jenis == null ? BankSoal.PILIHAN_GANDA : jenis;
	}

	/**
	 * Mengembalikan cara koreksi hasil ujian — inilah sumber kebenaran yang sesungguhnya, karena
	 * {@link #getJenis()} diturunkan darinya.
	 *
	 * <p>Nilainya salah satu dari {@link PenjelasanBankSoal#KOREKSI_OTOMATIS} (sistem menilai
	 * sendiri, cocok untuk pilihan ganda) atau {@link PenjelasanBankSoal#KOREKSI_MANUAL} (dosen/
	 * guru menilai jawaban esai satu per satu lewat {@code HasilUjianMahasiswaHelper}).</p>
	 *
	 * <p><b>Migrasi data lama + efek samping (getter yang menulis balik):</b> bila
	 * {@link #jenisKoreksi} masih {@code null} sementara {@link #jenis} sudah terisi — kondisi khas
	 * baris lama yang dibuat sebelum kolom ini ada — nilainya <b>disimpulkan dari jenis soal lalu
	 * ditugaskan ke field</b>. Karena kolom ini dipetakan Hibernate, penyimpulan tersebut ikut
	 * tersimpan pada flush berikutnya, sehingga pembacaan pertama pada data lama sekaligus
	 * memutakhirkannya.</p>
	 *
	 * <p>Bila kedua field kosong, nilai kembaliannya {@link PenjelasanBankSoal#KOREKSI_OTOMATIS}
	 * <i>tanpa</i> ditulis ke field.</p>
	 *
	 * @return cara koreksi hasil ujian, tidak pernah {@code null}
	 * @see #getJenis()
	 */
	public String getJenisKoreksi() {
		if (jenisKoreksi == null && jenis != null) {
			if (jenis.equals(BankSoal.PILIHAN_GANDA)) {
				jenisKoreksi = PenjelasanBankSoal.KOREKSI_OTOMATIS;
			} else {
				jenisKoreksi = PenjelasanBankSoal.KOREKSI_MANUAL;
			}
		}
		return jenisKoreksi == null ? PenjelasanBankSoal.KOREKSI_OTOMATIS : jenisKoreksi;
	}

	/**
	 * Menetapkan cara koreksi hasil ujian; ini pintu masuk yang benar untuk mengubah perilaku
	 * penilaian sekaligus jenis soal.
	 *
	 * @param jenisKoreksi {@link PenjelasanBankSoal#KOREKSI_OTOMATIS} atau
	 *                     {@link PenjelasanBankSoal#KOREKSI_MANUAL}
	 * @see #getJenisKoreksi()
	 */
	public void setJenisKoreksi(String jenisKoreksi) {
		this.jenisKoreksi = jenisKoreksi;
	}

	/**
	 * Menetapkan jurusan/program studi pemilik ujian.
	 *
	 * @param jurusan jurusan pemilik; boleh {@code null} (ujian lintas jurusan)
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan jurusan/program studi pemilik ujian.
	 *
	 * <p>Relasi lazy, karena itu proxy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditugaskan kembali ke field — pola
	 * getter relasi standar seluruh entity AIS untuk menghindari
	 * {@code LazyInitializationException} pada objek yang sudah <i>detached</i>.</p>
	 *
	 * @return jurusan pemilik, atau {@code null} bila ujian tidak terikat jurusan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan fakultas pemilik ujian.
	 *
	 * @param fakultas fakultas pemilik; boleh {@code null} (ujian lintas fakultas)
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas pemilik ujian.
	 *
	 * <p>Relasi lazy dengan pola resolusi proxy yang sama seperti {@link #getJurusan()}.</p>
	 *
	 * @return fakultas pemilik, atau {@code null} bila ujian tidak terikat fakultas
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan dosen penyusun/penanggung jawab ujian.
	 *
	 * @param dosen dosen pemilik; boleh {@code null}
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan dosen penyusun/penanggung jawab ujian (domain perguruan tinggi).
	 *
	 * <p>Relasi lazy dengan pola resolusi proxy yang sama seperti {@link #getJurusan()}. Padanan
	 * pada domain sekolah adalah {@link #getGuru()}.</p>
	 *
	 * @return dosen pemilik, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan matakuliah terkait ujian.
	 *
	 * @param matakuliah matakuliah terkait; boleh {@code null}
	 */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/**
	 * Mengembalikan matakuliah terkait ujian (domain perguruan tinggi).
	 *
	 * <p>Berbeda dari {@link #getJurusan()}/{@link #getFakultas()}/{@link #getDosen()}, relasi ini
	 * tidak diberi {@code fetch = FetchType.LAZY} sehingga bersifat eager (bawaan
	 * {@code @ManyToOne}) dan dimuat lewat {@code SELECT} terpisah
	 * ({@link FetchMode#SELECT}). Karena itu getter ini <b>tidak</b> memanggil
	 * {@link GeneralValueObject#check(Object)} — tidak ada proxy yang perlu diresolusi.</p>
	 *
	 * @return matakuliah terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matakuliah", nullable = true)
	public Matakuliah getMatakuliah() {
		return matakuliah;
	}

	/**
	 * Mengembalikan label tingkat/level ujian.
	 *
	 * <p>Nilai yang dikenal formulir {@code UjianAction} adalah {@code "Semua Level"} dan
	 * {@code "Level 1"} sampai {@code "Level 10"}. Label ini murni penanda pengelompokan yang
	 * ditampilkan di daftar dan bandbox pemilihan ujian — tidak ada logika yang menegakkannya.</p>
	 *
	 * <p><b>Efek samping (getter yang menulis balik):</b> bila field {@link #level} kosong atau
	 * berisi spasi saja, nilai {@code "Semua Level"} <b>ditulis ke field</b>, bukan sekadar
	 * dikembalikan. Karena kolom ini dipetakan Hibernate lewat akses properti, nilai bawaan
	 * tersebut ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @return label level ujian, tidak pernah {@code null} maupun kosong
	 */
	public String getLevel() {
		if (level == null || level.trim().isEmpty()) {
			level = "Semua Level";
		}
		return level;
	}

	/**
	 * Menetapkan label tingkat/level ujian.
	 *
	 * @param level label level, mis. {@code "Level 3"}; boleh {@code null} (akan dinormalkan
	 *              menjadi {@code "Semua Level"} pada pembacaan berikutnya)
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	/**
	 * Mengembalikan ambang nilai kelulusan ujian.
	 *
	 * <p>Dipakai mesin penilaian untuk memutuskan lulus/tidak lulus, termasuk sebagai syarat
	 * penerbitan {@link #getSertifikat()}. Bila belum diisi, nilai bawaan {@code 50.0}
	 * dikembalikan <b>tanpa</b> ditulis ke field — jadi getter ini bebas efek samping.</p>
	 *
	 * @return ambang kelulusan, tidak pernah {@code null}
	 */
	public Double getNilaiLulus() {
		return nilaiLulus == null ? 50.0 : nilaiLulus;
	}

	/**
	 * Menetapkan ambang nilai kelulusan ujian.
	 *
	 * @param nilaiLulus ambang kelulusan; {@code null} berarti pakai bawaan {@code 50.0}
	 */
	public void setNilaiLulus(Double nilaiLulus) {
		this.nilaiLulus = nilaiLulus;
	}

	/**
	 * Mengembalikan templat sertifikat yang diterbitkan bagi peserta yang lulus ujian ini.
	 *
	 * <p>Relasi eager ({@code @ManyToOne} tanpa {@code FetchType.LAZY}) dengan {@code SELECT}
	 * terpisah, karena itu tidak perlu {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return templat sertifikat, atau {@code null} bila ujian tidak menerbitkan sertifikat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		return sertifikat;
	}

	/**
	 * Menetapkan templat sertifikat bagi peserta yang lulus.
	 *
	 * @param sertifikat templat sertifikat; boleh {@code null}
	 */
	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	/**
	 * Mengembalikan status keaktifan ujian.
	 *
	 * <p>Ujian tidak aktif disembunyikan dari daftar pemilihan, tetapi penjadwalan
	 * ({@link PertemuanPunyaUjian}) yang sudah terlanjur dibuat tidak otomatis dibatalkan. Bila
	 * belum diisi, bawaan {@code true} dikembalikan tanpa ditulis ke field.</p>
	 *
	 * @return {@code true} bila ujian masih dipakai
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status keaktifan ujian.
	 *
	 * @param aktif {@code true} bila ujian masih dipakai; {@code null} berarti pakai bawaan
	 *              {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan guru penyusun/penanggung jawab ujian (domain sekolah).
	 *
	 * <p>Padanan {@link #getDosen()} pada domain perguruan tinggi. Relasi eager dengan
	 * {@code SELECT} terpisah.</p>
	 *
	 * @return guru pemilik, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		return guru;
	}

	/**
	 * Menetapkan guru penyusun/penanggung jawab ujian.
	 *
	 * @param guru guru pemilik; boleh {@code null}
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan mata pelajaran terkait ujian (domain sekolah).
	 *
	 * <p>Padanan {@link #getMatakuliah()} pada domain perguruan tinggi. Relasi eager dengan
	 * {@code SELECT} terpisah.</p>
	 *
	 * @return mata pelajaran terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matapelajaran", nullable = true)
	public Matapelajaran getMatapelajaran() {
		return matapelajaran;
	}

	/**
	 * Menetapkan mata pelajaran terkait ujian.
	 *
	 * @param matapelajaran mata pelajaran terkait; boleh {@code null}
	 */
	public void setMatapelajaran(Matapelajaran matapelajaran) {
		this.matapelajaran = matapelajaran;
	}

	/**
	 * Menutup sesi Hibernate <i>native</i> secara bertahap dan tanpa pernah melempar exception.
	 *
	 * <p>Ini pasangan wajib dari {@link HibernateUtil#currentNativeSession()} — sesi hasil method
	 * tersebut terikat pada {@code ThreadLocal} dan <b>harus</b> ditutup pemanggilnya, berbeda
	 * dengan {@code currentSession()} milik konteks request ZK yang justru TIDAK boleh ditutup
	 * manual.</p>
	 *
	 * <p>Empat langkah dijalankan berurutan, masing-masing dibungkus {@code try/catch} sendiri
	 * sehingga kegagalan satu langkah tidak menghalangi langkah berikutnya:</p>
	 * <ol>
	 *   <li>{@code clear()} — melepas seluruh entity dari cache tingkat satu;</li>
	 *   <li>{@code disconnect()} — mengembalikan koneksi JDBC ke pool;</li>
	 *   <li>{@code close()} — menutup sesi;</li>
	 *   <li>{@link HibernateUtil#closeSession()} — melepas rujukan {@code ThreadLocal} agar
	 *   pemanggilan {@code currentNativeSession()} berikutnya membuka sesi baru yang bersih.</li>
	 * </ol>
	 *
	 * <p>Setiap kegagalan dicatat ke audit error lewat penanda {@code auto-audit(empty-catch)},
	 * bukan ditelan diam-diam.</p>
	 *
	 * <p><b>Peringatan bagi pemanggil di masa depan:</b> karena sesi yang ditutup adalah sesi
	 * milik thread, setiap rujukan {@code Session} lain yang dipegang pemanggil pada thread yang
	 * sama menjadi tidak valid setelah method ini berjalan. Di kelas ini method hanya dipakai pada
	 * satu tempat: blok {@code finally} di
	 * {@link #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)}.</p>
	 *
	 * @param session sesi yang akan ditutup; {@code null} diabaikan
	 * @see HibernateUtil#currentNativeSession()
	 */
	private static void closeNativeSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:314");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:318");
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:322");
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:326");
		}
	}

	/**
	 * Mengembalikan daftar ID {@link BankSoal} yang menjadi isi ujian ini.
	 *
	 * <p>Alurnya dua tahap: ambil dulu daftar ID {@link UjianPunyaSoal} lewat
	 * {@link #ambilUjianPunyaSoal(PertemuanPunyaUjian, boolean)}, lalu untuk setiap ID muat entity
	 * penghubungnya dengan {@code GeneralValueObject.ambilData(...)} dan petik ID bank soalnya.
	 * Entry yang tidak bisa dimuat, atau yang bank soalnya {@code null}/tanpa ID, dilewati
	 * diam-diam — jadi daftar keluaran bisa lebih pendek dari daftar masukan tanpa peringatan.</p>
	 *
	 * <p>Urutan hasil mengikuti urutan {@code UjianPunyaSoal} dari method di atas (terurut
	 * {@code nomorUrut}/perbandingan alami bila ujian tidak diacak).</p>
	 *
	 * <p>Dipanggil dari {@code DetailUjianHelper} saat menyusun pratinjau soal, serta dari
	 * {@code PertemuanPunyaUjianHelper} dan {@code PertemuanPunyaUjianSiswaHelper} saat menyalin
	 * atau menghitung soal sebuah penjadwalan.</p>
	 *
	 * @param pertemuanPunyaUjian penjadwalan yang menjadi konteks pembacaan; menentukan apakah
	 *                            urutan soal diacak. Boleh {@code null} (diperlakukan sebagai
	 *                            "acak"/tanpa pengurutan tambahan)
	 * @param refresh             {@code true} memaksa indeks soal dibangun ulang dari basis data
	 *                            sebelum dibaca
	 * @return daftar ID bank soal; kosong (bukan {@code null}) bila ujian belum punya soal
	 * @see #ambilUjianPunyaSoal(PertemuanPunyaUjian, boolean)
	 */
	public List<Long> ambilBankSoal(PertemuanPunyaUjian pertemuanPunyaUjian, boolean refresh) {
		List<Long> ujianPunyaSoalsa = ambilUjianPunyaSoal(pertemuanPunyaUjian, refresh);
		List<Long> bankSoals = new ArrayList<Long>();
		if (ujianPunyaSoalsa == null || ujianPunyaSoalsa.isEmpty()) {
			return bankSoals;
		}
		for (Long ujianPunyaSoalid : ujianPunyaSoalsa) {
			if (ujianPunyaSoalid == null) {
				continue;
			}
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					ujianPunyaSoalid.toString());
			if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null
					&& ujianPunyaSoal.getBankSoal().getId() != null) {
				bankSoals.add(ujianPunyaSoal.getBankSoal().getId());
			}
		}
		ujianPunyaSoalsa = null;
		return bankSoals;
	}

	/**
	 * Versi <b>berhalaman</b> dari pembacaan daftar soal, untuk komponen grid/listbox ZK yang
	 * memakai model paging.
	 *
	 * <p>Seluruh daftar ID diambil lebih dulu lewat
	 * {@link #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)}, lalu dipotong di memori
	 * pada rentang {@code [mulai, mulai + banyak)}. Tidak ada {@code LIMIT}/{@code OFFSET} di sisi
	 * basis data — pemotongan murni terjadi di Java, jadi biaya pembacaan tidak berkurang seiring
	 * mengecilnya halaman.</p>
	 *
	 * <p>Rentang di luar jangkauan aman: bila {@code mulai} melebihi jumlah data, daftar yang
	 * dikembalikan kosong dan totalnya tetap benar.</p>
	 *
	 * <p>Dipanggil antara lain oleh {@code DetailUjianHelper} (termasuk saat ekspor seluruh soal ke
	 * Excel dengan {@code banyak = 10000}), {@code HasilUjianMahasiswaHelper}, dan
	 * {@code ProsesUjianHelper}.</p>
	 *
	 * @param refresh             {@code true} memaksa indeks soal dibangun ulang lebih dulu
	 * @param pertemuanPunyaUjian penjadwalan konteks; menentukan pengurutan. Boleh {@code null}
	 * @param cari                kata kunci pencarian pada teks soal; kosong/{@code null} berarti
	 *                            tanpa penyaringan
	 * @param mulai               indeks awal (berbasis 0) potongan yang diambil
	 * @param banyak              jumlah maksimum data dalam potongan
	 * @return larik dua elemen: indeks 0 berisi {@code List<Long>} ID {@link UjianPunyaSoal} pada
	 *         halaman yang diminta, indeks 1 berisi {@code Integer} jumlah TOTAL data sebelum
	 *         dipotong (untuk menghitung jumlah halaman)
	 * @see #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)
	 */
	public Object[] ambilUjianPunyaSoal(boolean refresh, PertemuanPunyaUjian pertemuanPunyaUjian, String cari,
			int mulai, int banyak) {
		List<Long> ujianPunyaSoalsa = ambilUjianPunyaSoal(cari, pertemuanPunyaUjian, refresh);
		if (ujianPunyaSoalsa == null) {
			ujianPunyaSoalsa = new ArrayList<Long>();
		}

		List<Long> diambil = new ArrayList<Long>();
		int index = 0;
		for (Long ujianPunyaSoal : ujianPunyaSoalsa) {
			if (index >= mulai && index < (mulai + banyak)) {
				diambil.add(ujianPunyaSoal);
			}
			index++;
		}
		int total = ujianPunyaSoalsa.size();
		ujianPunyaSoalsa = null;
		return new Object[] { diambil, total };
	}

	/**
	 * Pintasan pembacaan seluruh ID {@link UjianPunyaSoal} milik ujian ini tanpa penyaringan kata
	 * kunci.
	 *
	 * <p>Ini bentuk yang paling sering dipakai mesin ujian ({@code ProsesUjianHelper}) dan API
	 * e-learning ({@code ElearningPertemuanApi}) saat menyiapkan soal untuk peserta.</p>
	 *
	 * @param pertemuanPunyaUjian penjadwalan konteks; menentukan pengurutan. Boleh {@code null}
	 * @param refresh             {@code true} memaksa indeks soal dibangun ulang lebih dulu
	 * @return daftar ID {@link UjianPunyaSoal}
	 * @see #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)
	 */
	public List<Long> ambilUjianPunyaSoal(PertemuanPunyaUjian pertemuanPunyaUjian, boolean refresh) {
		return ambilUjianPunyaSoal("", pertemuanPunyaUjian, refresh);
	}

	/**
	 * Mengembalikan aturan kelayakan terstruktur yang harus dipenuhi peserta sebelum boleh
	 * mengikuti ujian ini.
	 *
	 * <p>Aturan disimpan sebagai entity {@link SyaratUjian} dan dievaluasi di luar kelas ini oleh
	 * {@code ais.action.master.SyaratUjianAction.checkSyaratSyaratUjian(...)}. Berbeda dengan
	 * {@link #getSyaratAkses()} yang berisi prasyarat materi/tugas dalam JSON bebas, syarat ini
	 * bersifat administratif (mis. status pembayaran).</p>
	 *
	 * <p>Relasi lazy dengan pola resolusi proxy yang sama seperti {@link #getJurusan()}.</p>
	 *
	 * @return aturan kelayakan, atau {@code null} bila ujian tidak bersyarat
	 * @see SyaratUjian
	 * @see #getSyaratAkses()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "syarat_ujian", nullable = true)
	public SyaratUjian getSyaratUjian() {
		syaratUjian = check(syaratUjian);
		return syaratUjian;
	}

	/**
	 * Menetapkan aturan kelayakan peserta.
	 *
	 * @param syaratUjian aturan kelayakan; boleh {@code null} (ujian tanpa syarat)
	 */
	public void setSyaratUjian(SyaratUjian syaratUjian) {
		this.syaratUjian = syaratUjian;
	}

	/**
	 * Mengembalikan teks HTML tata tertib yang ditampilkan kepada peserta sebelum dan sesudah
	 * ujian.
	 *
	 * <p>Ditampilkan mentah sebagai HTML oleh {@code ProsesUjianHelper} (komponen
	 * {@code MyHtml}), termasuk versi yang ukuran fontnya dibesarkan dari {@code 9px} menjadi
	 * {@code 16px} pada halaman pembuka ujian.</p>
	 *
	 * <p><b>Dua efek samping (getter yang menulis balik ke field):</b></p>
	 * <ol>
	 *   <li><b>Pengisian bawaan.</b> Bila field kosong, teks tata tertib bawaan (peringatan soal
	 *   koneksi internet, larangan menutup/me-refresh browser, dan pengingat sisa waktu)
	 *   <b>ditulis ke field</b>. Karena kolom {@code tata_tertib_ujian} dipetakan Hibernate,
	 *   sekadar menampilkan halaman ujian dapat membuat teks bawaan ini tersimpan permanen ke
	 *   basis data.</li>
	 *   <li><b>Penyaringan XSS yang merusak data.</b> Nilai selalu dilewatkan
	 *   {@link GeneralValueObject#filterTidakBolehSederhana(String)} dan <b>hasilnya ditugaskan
	 *   kembali ke field</b>. Filter itu mengganti setiap kemunculan kata {@code script} (tak
	 *   peduli besar-kecil huruf, termasuk yang menyatu dengan kata lain) menjadi {@code __S__}.
	 *   Akibatnya tata tertib yang sah namun memuat kata tersebut — mis. kalimat berbahasa Inggris
	 *   atau atribut {@code style} yang mengandung potongan huruf itu — akan <b>rusak permanen</b>
	 *   pada flush berikutnya, dan kerusakan bertambah setiap kali disimpan ulang. Perilaku ini
	 *   dicatat apa adanya; jangan diperbaiki tanpa keputusan terpisah.</li>
	 * </ol>
	 *
	 * @return teks HTML tata tertib, tidak pernah {@code null} maupun kosong
	 * @see GeneralValueObject#filterTidakBolehSederhana(String)
	 */
	@Column(name = "tata_tertib_ujian", columnDefinition = "text")
	public String getTatatertibUjian() {
		tatatertibUjian = tatatertibUjian == null || tatatertibUjian.trim().isEmpty()
				? "<div style='font-size:9px;color:blue'><b>Perhatian:</b><ol><li>Untuk memastikan bahwa Ujian ini berjalan dengan lancar, pastikan koneksi internet Anda terhubung secara baik.</li><li>Jangan menutup browser atau me-refresh browser yang menampilkan Ujian ini. Jika browser Anda tidak sengaja ter-refresh atau tertutup, maka Anda tidak bisa mengulang ujian ini, kecuali dengan seizin petugas ujian.</li><li>Pastikan Anda juga memperhatikan batas / sisa waktu Ujian, untuk memastikan bahwa Anda masih memiliki sisa waktu mengerjakan soal.</li></ol><br>Terima Kasih dan Selamat Mengerjakan</div>"
				: tatatertibUjian;

		tatatertibUjian = filterTidakBolehSederhana(tatatertibUjian);

		return tatatertibUjian;
	}

	/**
	 * Menetapkan teks HTML tata tertib ujian.
	 *
	 * <p>Nilai disimpan apa adanya di sini; penyaringan baru terjadi saat dibaca lewat
	 * {@link #getTatatertibUjian()}.</p>
	 *
	 * @param tatatertibUjian teks HTML tata tertib; {@code null}/kosong berarti pakai teks bawaan
	 */
	public void setTatatertibUjian(String tatatertibUjian) {
		this.tatatertibUjian = tatatertibUjian;
	}

	/**
	 * Membaca isi berkas indeks soal milik ujian ini.
	 *
	 * <p>Berkasnya di-resolve lewat
	 * {@code Common.getFileLocation(this, "ujian_punya_soal_" + getId())}, jadi setiap ujian punya
	 * berkas sendiri. Isinya berupa {@code JSONObject} datar berisi ID {@link UjianPunyaSoal}
	 * (lihat penjelasan indeks pada Javadoc kelas).</p>
	 *
	 * <p>Method ini <b>tidak pernah gagal</b>: bila ujian belum punya ID, berkasnya belum ada,
	 * isinya kosong, atau pembacaan melempar exception, yang dikembalikan adalah
	 * {@code VOMahasiswa.dataJSON} — string JSON kosong {@code "{}"} yang dipakai bersama sebagai
	 * nilai netral di seluruh model. Kegagalan pembacaan dicatat ke audit error.</p>
	 *
	 * @return string JSON indeks soal, tidak pernah {@code null}
	 * @see #tulisLokasiUjianPunyaSoal(String)
	 */
	public String ambilLokasiUjianPunyaSoal() {
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "ujian_punya_soal_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:410");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa isi berkas indeks soal milik ujian ini.
	 *
	 * <p>Menulis seluruh berkas sekaligus (bukan menambahkan), sehingga pemanggil bertanggung jawab
	 * membaca dulu lewat {@link #ambilLokasiUjianPunyaSoal()}, mengubah, lalu menulis balik — pola
	 * yang dipakai {@link #populateUjianPunyaSoal} dan
	 * {@link #removeUjianPunyaSoal(Serializable)}.</p>
	 *
	 * <p>Tidak melakukan apa pun bila ujian belum punya ID. Kegagalan penulisan dicatat ke audit
	 * error dan tidak dilempar ke pemanggil — artinya indeks bisa gagal tersimpan tanpa pemanggil
	 * mengetahuinya; jalur pemulihannya adalah pembangunan ulang lewat
	 * {@link #reInitUjianPunyaSoal(Session)}.</p>
	 *
	 * @param data string JSON yang akan ditulis sebagai isi berkas indeks
	 * @see #ambilLokasiUjianPunyaSoal()
	 */
	public void tulisLokasiUjianPunyaSoal(String data) {
		if (getId() == null) {
			return;
		}
		File file = Common.getFileLocation(this, "ujian_punya_soal_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:422");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks soal milik ujian ini sehingga isinya dibangun ulang dari basis data
	 * pada pembacaan berikutnya.
	 *
	 * <p>Tidak melakukan apa pun bila ujian belum punya ID. Penghapusan dilakukan
	 * {@code BacaTulisUtil.doHapus(file, "ujian_punya_soal")} yang aman dipanggil walau berkasnya
	 * memang tidak ada.</p>
	 *
	 * <p>Dipakai sebagai invalidasi cache, mis. oleh
	 * {@code ais.action.master.helper.RevisiUjianHelper} setelah memulihkan versi lama sebuah
	 * ujian dari riwayat Envers — data soal di basis data berubah, jadi indeks berkas harus
	 * dibuang.</p>
	 *
	 * @see #reInitUjianPunyaSoal(Session)
	 */
	public void bersihkanLokasiUjianPunyaSoal() {
		if (getId() == null) {
			return;
		}
		File file = Common.getFileLocation(this, "ujian_punya_soal_" + getId().toString());
		BacaTulisUtil.doHapus(file, "ujian_punya_soal");

	}

	/**
	 * Membangun ulang indeks soal dari basis data — sumber kebenaran indeks berkas.
	 *
	 * <p>Langkahnya: query seluruh {@link UjianPunyaSoal} yang {@code ujian}-nya adalah objek ini
	 * (terurut menaik berdasarkan {@code id}), hapus berkas indeks lama, tulis berkas indeks kosong
	 * {@code "{}"}, lalu daftarkan kembali setiap baris lewat {@link #populateUjianPunyaSoal}.</p>
	 *
	 * <p><b>Sesi tidak ditutup di sini.</b> {@code session} adalah milik pemanggil dan tetap
	 * terbuka setelah method selesai — pemanggil yang bertanggung jawab menutupnya. Lihat
	 * {@link #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)} yang membuka sesi
	 * <i>native</i> sendiri dan menutupnya di {@code finally}, atau
	 * {@code AmbilDataUjianBanyak}/{@code VOPembelajaran} yang meneruskan sesi mereka sendiri.</p>
	 *
	 * <p><b>Jendela indeks kosong.</b> Antara penulisan {@code "{}"} dan selesainya perulangan,
	 * pembaca lain pada saat yang sama akan melihat ujian ini seolah tidak punya soal. Tidak ada
	 * penguncian; untuk ujian dengan banyak soal jendela ini bisa terasa.</p>
	 *
	 * @param session sesi Hibernate milik pemanggil, dipakai untuk query {@link UjianPunyaSoal};
	 *                harus terbuka dan tidak boleh {@code null}
	 * @see #populateUjianPunyaSoal(UjianPunyaSoal, boolean)
	 * @see #bersihkanLokasiUjianPunyaSoal()
	 */
	@SuppressWarnings("unchecked")
	public void reInitUjianPunyaSoal(Session session) {
		List<UjianPunyaSoal> ujianPunyaSoals = ConstantValues.simpleList(session.createCriteria(UjianPunyaSoal.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("ujian", this)), UjianPunyaSoal.class);
		bersihkanLokasiUjianPunyaSoal();
		tulisLokasiUjianPunyaSoal(new JSONObject().toString());
		for (UjianPunyaSoal ujianPunyaSoal : ujianPunyaSoals) {
			populateUjianPunyaSoal(ujianPunyaSoal, true);
		}
		ujianPunyaSoals.clear();
		ujianPunyaSoals = null;
	}

	/**
	 * Mencoret satu soal dari indeks berkas setelah barisnya dihapus dari basis data.
	 *
	 * <p><b>Kuncinya tidak dibuang, hanya dikosongkan nilainya.</b> Entry ditulis ulang menjadi
	 * {@code "<id>": ""} — inilah "nisan" yang disebut pada Javadoc kelas. Pembaca indeks
	 * ({@link #ambilUjianPunyaSoal(String, PertemuanPunyaUjian, boolean)}) melewati entry bernilai
	 * kosong, jadi hasilnya benar; konsekuensinya berkas indeks <b>tumbuh terus</b> dan tidak
	 * pernah menyusut sampai dibangun ulang lewat {@link #reInitUjianPunyaSoal(Session)}.</p>
	 *
	 * <p>Dipanggil otomatis oleh {@link ais.database.hibernate.AuditListener} pada peristiwa
	 * penghapusan sebuah {@link UjianPunyaSoal}, bukan dari kode aplikasi biasa. Seluruh badan
	 * method dibungkus {@code try/catch}: kegagalan dicatat ke audit error dan indeks dibiarkan
	 * apa adanya.</p>
	 *
	 * @param id ID {@link UjianPunyaSoal} yang barisnya sudah dihapus
	 * @see #populateUjianPunyaSoal(UjianPunyaSoal, boolean)
	 */
	public void removeUjianPunyaSoal(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiUjianPunyaSoal());
			c.put(id.toString(), "");
			tulisLokasiUjianPunyaSoal(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:455");

		}
	}

	/**
	 * Mendaftarkan satu soal ke indeks berkas.
	 *
	 * <p>Membaca indeks saat ini, menambahkan entry {@code "<id>": "<id>"}, lalu menulis balik
	 * seluruh berkas. Karena kunci dan nilainya sama, memanggil ulang untuk ID yang sama bersifat
	 * idempoten — sekaligus menjadi cara "menghidupkan kembali" nisan yang dibuat
	 * {@link #removeUjianPunyaSoal(Serializable)}.</p>
	 *
	 * <p>Dipanggil dari dua tempat: {@link ais.database.hibernate.AuditListener} setiap kali sebuah
	 * {@link UjianPunyaSoal} tersimpan, dan {@link #reInitUjianPunyaSoal(Session)} saat membangun
	 * ulang indeks.</p>
	 *
	 * <p>Masukan {@code null} diabaikan. Seluruh badan method dibungkus {@code try/catch};
	 * kegagalan (termasuk {@code NullPointerException} bila {@code ujianPunyaSoal} belum punya ID)
	 * dicatat ke audit error dan tidak dilempar ke pemanggil.</p>
	 *
	 * @param ujianPunyaSoal baris penghubung yang akan didaftarkan; {@code null} diabaikan
	 * @param tulisUlang     <b>tidak dipakai</b> — parameter ini tidak dirujuk sama sekali di badan
	 *                       method (penulisan berkas selalu dilakukan). Dipertahankan demi
	 *                       kompatibilitas pemanggil yang sudah ada; keduanya kebetulan selalu
	 *                       mengirim {@code true}
	 * @see #removeUjianPunyaSoal(Serializable)
	 */
	public void populateUjianPunyaSoal(UjianPunyaSoal ujianPunyaSoal, boolean tulisUlang) {
		try {
			if (ujianPunyaSoal == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiUjianPunyaSoal());
			c.put(ujianPunyaSoal.getId().toString(), ujianPunyaSoal.getId().toString());
			tulisLokasiUjianPunyaSoal(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:469");
		}
	}

	/**
	 * Method inti pembacaan daftar soal ujian — semua kelebihan-beban {@code ambilUjianPunyaSoal}
	 * dan {@link #ambilBankSoal(PertemuanPunyaUjian, boolean)} bermuara ke sini.
	 *
	 * <h4>Tahap 1 — pembangunan ulang indeks bila perlu</h4>
	 * <p>Bila {@code refresh} bernilai {@code true} <b>atau</b> penanda
	 * {@link GeneralValueObject#udah(String) udah("ujian")} belum pernah dipasang, sebuah sesi
	 * Hibernate <i>native</i> dibuka lewat {@link HibernateUtil#currentNativeSession()},
	 * {@link #reInitUjianPunyaSoal(Session)} dijalankan, lalu sesi ditutup di {@code finally} lewat
	 * {@link #closeNativeSessionQuietly(Session)}. Perhatikan sifat <i>test-and-set</i> dari
	 * {@code udah(...)}: pemanggilan pertama seumur objek ini selalu membangun ulang indeks,
	 * pemanggilan berikutnya tidak. Kegagalan tahap ini dicatat ke audit lalu dibiarkan — pembacaan
	 * tetap dilanjutkan dengan indeks yang ada.</p>
	 *
	 * <h4>Tahap 2 — pembacaan indeks</h4>
	 * <p>Indeks JSON dibaca, dan setiap kunci bernilai tidak-kosong dimuat sebagai
	 * {@link UjianPunyaSoal} lewat {@code ambilData(...)}. Entry yang tidak bisa dimuat, atau yang
	 * {@code bankSoal}-nya {@code null}, dilewati. Bila {@code cari} diisi, hanya soal yang teks
	 * {@code BankSoal.getSoal()}-nya <b>memuat</b> kata kunci (peka besar-kecil huruf, pencocokan
	 * substring biasa) yang lolos.</p>
	 *
	 * <p>Sebagai efek samping pemuatan, setiap {@link UjianPunyaSoal} yang lolos di-{@code
	 * setUjian(this)} agar rujukan baliknya menunjuk instance ini — mencegah pemuatan ulang proxy
	 * di sisi pemanggil.</p>
	 *
	 * <h4>Tahap 3 — pengurutan</h4>
	 * <p>Di sinilah satu-satunya perbedaan nyata antara dua cabang besar method ini:</p>
	 * <ul>
	 *   <li><b>{@code pertemuanPunyaUjian != null} dan tidak diacak</b> — objek
	 *   {@link UjianPunyaSoal} dikumpulkan lebih dulu, diurutkan
	 *   {@code Collections.sort(...)} memakai urutan alaminya (nomor urut soal), baru ID-nya
	 *   dipetik. Ini jalur untuk ujian dengan urutan soal tetap.</li>
	 *   <li><b>selain itu</b> ({@code pertemuanPunyaUjian} {@code null} atau ujian diacak) — ID
	 *   dikumpulkan langsung apa adanya dalam urutan kunci JSON, yang <b>tidak terjamin</b>.
	 *   Pengacakan sesungguhnya dilakukan pemanggil ({@code ProsesUjianHelper}), bukan di sini.</li>
	 * </ul>
	 * <p>Kedua cabang berbagi badan perulangan yang praktis identik (duplikasi kode yang disengaja
	 * pada versi ini; dicatat, tidak diubah).</p>
	 *
	 * @param cari                kata kunci pencocokan substring pada teks soal;
	 *                            {@code null}/kosong berarti tanpa penyaringan
	 * @param pertemuanPunyaUjian penjadwalan konteks — hanya dipakai untuk membaca
	 *                            {@code getRandom()}. Boleh {@code null}
	 * @param refresh             {@code true} memaksa indeks dibangun ulang dari basis data lebih
	 *                            dulu
	 * @return daftar ID {@link UjianPunyaSoal}; kosong (bukan {@code null}) bila tidak ada yang
	 *         cocok
	 * @see #reInitUjianPunyaSoal(Session)
	 * @see GeneralValueObject#udah(String)
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilUjianPunyaSoal(String cari, PertemuanPunyaUjian pertemuanPunyaUjian, boolean refresh) {
		if (refresh || !udah("ujian")) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				reInitUjianPunyaSoal(session);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Ujian.java:481");
			} finally {
				closeNativeSessionQuietly(session);
			}
		}

		if (pertemuanPunyaUjian != null && !pertemuanPunyaUjian.getRandom()) {
			List<UjianPunyaSoal> ujianPunyaSoals = new ArrayList<UjianPunyaSoal>();
			try {
				JSONObject c = new JSONObject(ambilLokasiUjianPunyaSoal());
				Iterator<String> keys = c.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					try {
						String s = c.getString(key);
						if (s != null && !s.trim().isEmpty()) {

							GeneralValueObject generalValueObject = ambilData(UjianPunyaSoal.class, key);
							if (generalValueObject != null) {
								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) generalValueObject;
								if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
									if (cari == null || cari.trim().isEmpty()
											|| ujianPunyaSoal.getBankSoal().getSoal().contains(cari)) {
										ujianPunyaSoal.setUjian(this);
										ujianPunyaSoals.add(ujianPunyaSoal);
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Ujian.java:511");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Ujian.java:515");
			}
			Collections.sort(ujianPunyaSoals);
			List<Long> ujianPunyaSoalsa = new ArrayList<Long>();
			for (UjianPunyaSoal ujianPunyaSoal : ujianPunyaSoals) {
				ujianPunyaSoalsa.add(ujianPunyaSoal.getId());
			}
			ujianPunyaSoals = null;
			return ujianPunyaSoalsa;
		} else {

			List<Long> ujianPunyaSoalsa = new ArrayList<Long>();
			try {
				JSONObject c = new JSONObject(ambilLokasiUjianPunyaSoal());
				Iterator<String> keys = c.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					try {
						String s = c.getString(key);
						if (s != null && !s.trim().isEmpty()) {

							GeneralValueObject generalValueObject = ambilData(UjianPunyaSoal.class, key);
							if (generalValueObject != null) {
								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) generalValueObject;
								if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
									if (cari == null || cari.trim().isEmpty()
											|| ujianPunyaSoal.getBankSoal().getSoal().contains(cari)) {
										ujianPunyaSoal.setUjian(this);
										ujianPunyaSoalsa.add(ujianPunyaSoal.getId());
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Ujian.java:549");
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ujian.java:552");
//				e.printStackTrace();
			}
			return ujianPunyaSoalsa;
		}
	}

	/**
	 * String JSON kosong {@code "{}"} yang dipakai sebagai nilai bawaan
	 * {@link #getSyaratAkses()}. Dihitung sekali saat kelas dimuat.
	 */
	private static String JSON = new JSONObject().toString();

	/**
	 * Mengembalikan prasyarat akses ujian dalam bentuk JSON bebas.
	 *
	 * <p>Isinya peta {@code "<idMateri>_<NamaKelasSederhana>": "<namaKelasLengkap>_<id>"} yang
	 * menyatakan materi/tugas/ujian lain yang harus diselesaikan peserta sebelum ujian ini boleh
	 * dibuka. Peta disusun lewat komponen pemilih di {@code ais.database.model.Tugas} (lihat
	 * {@code ProfileUtil.tampilkanMateri(...)}): mencentang sebuah materi menambahkan entry,
	 * membatalkan centang menghapusnya, lalu hasilnya disimpan kembali lewat
	 * {@link #setSyaratAkses(String)}.</p>
	 *
	 * <p>Berbeda dengan {@link #getSyaratUjian()} yang berisi aturan administratif terstruktur,
	 * yang ini prasyarat pembelajaran dan strukturnya tidak dipetakan ke entity mana pun.</p>
	 *
	 * <p>Getter ini <b>tidak</b> menulis balik ke field: nilai bawaan {@code "{}"} hanya
	 * dikembalikan, sehingga aman dipanggil berulang kali dari perulangan UI.</p>
	 *
	 * @return string JSON prasyarat akses, tidak pernah {@code null} maupun kosong
	 * @see #getSyaratUjian()
	 */
	@Column(columnDefinition = "text")
	public String getSyaratAkses() {
		return syaratAkses == null || syaratAkses.trim().isEmpty() ? JSON : syaratAkses;
	}

	/**
	 * Menetapkan prasyarat akses ujian.
	 *
	 * <p>Tidak ada validasi bentuk JSON di sini — pemanggil bertanggung jawab mengirim string yang
	 * bisa diurai {@code JSONObject}, karena pembacanya di {@code Tugas} akan gagal bila tidak.</p>
	 *
	 * @param syaratAkses string JSON prasyarat; {@code null}/kosong berarti pakai bawaan
	 *                    {@code "{}"}
	 */
	public void setSyaratAkses(String syaratAkses) {
		this.syaratAkses = syaratAkses;
	}

	/**
	 * Menyatakan apakah pilihan jawaban ditampilkan dengan penanda huruf (A, B, C, D) di depannya.
	 *
	 * <p>Setelan tampilan murni untuk soal pilihan ganda. Bila belum diisi, bawaan {@code true}
	 * dikembalikan tanpa ditulis ke field.</p>
	 *
	 * @return {@code true} bila penanda huruf ditampilkan
	 */
	public Boolean getTampilanHurufDiPilihanJawaban() {
		return tampilanHurufDiPilihanJawaban == null ? true : tampilanHurufDiPilihanJawaban;
	}

	/**
	 * Menetapkan penampilan penanda huruf pada pilihan jawaban.
	 *
	 * @param tampilanHurufDiPilihanJawaban {@code true} untuk menampilkan penanda huruf;
	 *                                      {@code null} berarti pakai bawaan {@code true}
	 */
	public void setTampilanHurufDiPilihanJawaban(Boolean tampilanHurufDiPilihanJawaban) {
		this.tampilanHurufDiPilihanJawaban = tampilanHurufDiPilihanJawaban;
	}

	/**
	 * Mengembalikan yayasan pemilik ujian (domain sekolah).
	 *
	 * <p>Relasi lazy dengan pola resolusi proxy yang sama seperti {@link #getJurusan()}. Bersama
	 * {@link #getSekolah()} inilah pembatas ruang lingkup ujian pada instalasi multi-sekolah.</p>
	 *
	 * @return yayasan pemilik, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik ujian.
	 *
	 * <p><b>Penyaringan objek belum tersimpan:</b> objek {@link Yayasan} yang ID-nya masih
	 * {@code null} (mis. hasil pilihan combobox yang belum di-{@code persist}) diperlakukan sebagai
	 * {@code null}. Ini mencegah cascade {@code PERSIST} menyimpan yayasan baru secara tak sengaja
	 * saat ujian disimpan.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa ID membuat relasi dikosongkan
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan sekolah pemilik ujian (domain sekolah).
	 *
	 * <p>Relasi lazy dengan pola resolusi proxy yang sama seperti {@link #getJurusan()}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik ujian.
	 *
	 * <p>Sama seperti {@link #setYayasan(Yayasan)}, objek {@link Sekolah} yang belum punya ID
	 * diperlakukan sebagai {@code null} agar tidak ikut ter-{@code persist} lewat cascade.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa ID membuat relasi dikosongkan
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan penanda jenis konteks yang boleh memakai ujian ini.
	 *
	 * <p>Isinya <b>nama sederhana kelas</b> konteks pemilik pertemuan, hasil
	 * {@code Pertemuan.untuk()} — mis. {@code "Perkuliahan"}, {@code "JadwalPelajaran"},
	 * {@code "JadwalUjianPMB"}, {@code "Skripsi"}, {@code "KelompokKkn"}, dan seterusnya. Nilai
	 * {@code null} berarti ujian bersifat umum dan boleh dipakai konteks mana pun.</p>
	 *
	 * <p>Dipakai sebagai penyaring pada bandbox pemilihan ujian
	 * ({@code AmbilDataUjianBanyak}) dengan aturan
	 * {@code diperuntukkan IS NULL OR diperuntukkan = ?}, jadi ujian umum selalu ikut muncul di
	 * setiap konteks. Pengisiannya terjadi di dua tempat: formulir {@code UjianAction} (dari
	 * konteks pemanggil) dan {@code HasilUjianHelper} yang <b>menambal data lama</b> — ujian yang
	 * nilainya masih {@code null} dan sudah punya pertemuan akan diisi dari
	 * {@code pertemuan.untuk()}.</p>
	 *
	 * <p>Ini penyaring kenyamanan pada UI, <b>bukan penegak hak akses</b>.</p>
	 *
	 * @return nama sederhana kelas konteks pemakai, atau {@code null} untuk ujian umum
	 */
	public String getDiperuntukkan() {
		return diperuntukkan;
	}

	/**
	 * Menetapkan penanda jenis konteks pemakai ujian.
	 *
	 * @param diperuntukkan nama sederhana kelas konteks (mis. {@code "Perkuliahan"});
	 *                      {@code null} berarti ujian umum
	 * @see #getDiperuntukkan()
	 */
	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}
}
