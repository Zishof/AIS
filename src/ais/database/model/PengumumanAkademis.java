package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity <b>Pengumuman Akademik</b> — tabel {@code public.pengumuman_akademis}, ber-{@code @Audited}
 * (Hibernate Envers, seluruh perubahan direkam ke tabel {@code _AUD}).
 *
 * <p>Satu baris = satu pengumuman/informasi yang diterbitkan pengelola dan ditampilkan pada Papan
 * Pengumuman AIS (halaman utama/Beranda, tab portal, dan halaman publik). Meski namanya
 * "akademis", entity ini sebenarnya adalah <b>kanal informasi serba-guna</b> milik seluruh sistem:
 * ia dipakai untuk pengumuman mahasiswa, siswa, dosen, guru, alumni, calon mahasiswa/siswa,
 * pegawai, vendor, lowongan karir, sampai halaman statis (profil kampus) yang isinya cuma HTML.</p>
 *
 * <h3>Kelompok fungsi</h3>
 * <ol>
 *   <li><b>Isi pengumuman</b> — {@link #getJudul()}/{@link #getCatatan()} beserta padanan bahasa
 *   Inggrisnya {@link #getJudulEn()}/{@link #getCatatanEn()}. Isi berupa HTML bebas yang disaring
 *   ringan oleh {@code filterTidakBoleh*} milik induk.</li>
 *   <li><b>Penargetan audiens</b> — {@link #getDiperuntukkan()} (salah satu konstanta
 *   {@code UNTUK_*}), disempitkan lagi oleh konteks organisasi
 *   ({@link #getPerguruanTinggi()}, {@link #getFakultas()}, {@link #getJurusan()},
 *   {@link #getProgram()}, {@link #getYayasan()}, {@link #getSekolah()},
 *   {@link #getTahunAjaran()}) dan oleh daftar putih eksplisit
 *   ({@link #getHanyaUntuk()}, {@link #getHanyaUntukAngkatan()},
 *   {@link #getHanyaUntukUsername()}).</li>
 *   <li><b>Masa berlaku &amp; keaktifan</b> — {@link #getTanggal()}, {@link #getSampai()},
 *   {@link #getAktif()}, {@link #getTetapTampilkanPengumumanMeskipunSudahKelewat()}.</li>
 *   <li><b>Distribusi/broadcast e-mail &amp; notifikasi</b> — sekawanan flag
 *   {@code broadcast*} yang dibaca {@code ais.action.master.helper.BroadcastHelper}.</li>
 *   <li><b>Interaksi pembaca</b> — komentar/diskusi ({@link #getBolehDiberiKomentar()},
 *   {@link #getKomentarDitutup()}, {@code *DiskusiPengumumanAkademis*}) dan polling
 *   ({@link #getIsiPolling()}, {@link #getJawabanPolling()}).</li>
 *   <li><b>Presentasi/tata letak</b> — galeri gambar ({@link #galeries},
 *   {@link #getTinggiGaleri()}, {@link #getSlideWaktu()}), tombol navigasi antar pengumuman
 *   ({@link #getInduk()}, {@link #getPosisiTombol()}, {@link #getLabelTombol()}), serta
 *   penempatan ({@link #getLangsungTampilBeranda()}, {@link #getLangsungMunculDiTab()},
 *   {@link #getTampilkanProfile()}).</li>
 *   <li><b>Integrasi kalender/konferensi</b> — {@link #getAdaVideoConference()},
 *   {@link #getAdaVideoConferenceGoogleMeet()}, {@link #getCalendarEvent()},
 *   {@link #ambilOrganizer()}, {@link #ambilAttendee()}; dikonsumsi
 *   {@code ais.common.calendar.CalendarUtil}.</li>
 *   <li><b>Papan lepas (embed komponen ZK)</b> — {@link #getKlassData()}.</li>
 * </ol>
 *
 * <h3>Mekanisme distribusi</h3>
 * <p>Ada DUA jalur yang sama sekali terpisah dan perlu dibedakan:</p>
 * <ul>
 *   <li><b>Tarik (pull)</b> — pembaca membuka Papan Pengumuman dan
 *   {@code TampilanPengumumanAkademisAction} menyusun Criteria yang menyaring baris berdasarkan
 *   {@code diperuntukkan}, konteks organisasi, {@code aktif}, rentang tanggal, dan pencocokan
 *   {@code ilike ",<kunci>,"} pada {@code hanyaUntuk}/{@code hanyaUntukAngkatan}/
 *   {@code hanyaUntukUsername}. Bentuk berkoma-pengapit inilah alasan getter ketiga field itu
 *   melakukan normalisasi (lihat {@link #getHanyaUntuk()}).</li>
 *   <li><b>Dorong (push)</b> — {@code BroadcastHelper.broadcastEmail(PengumumanAkademis)}
 *   membaca flag {@code broadcast*}, mengumpulkan alamat e-mail + userId penerima lewat query
 *   Criteria per kelompok (Siswa, Mahasiswa aktif, mahasiswa cuti, alumni, calon mahasiswa,
 *   Dosen, Guru, Admin) — masing-masing tetap dipersempit oleh fakultas/jurusan/program/
 *   sekolah/yayasan pengumuman — lalu mengirim e-mail berisi judul, isi, dan seluruh komentar
 *   melalui {@code MailSender}. Jalur ini dijalankan di timer ZK, bukan sinkron saat simpan.</li>
 * </ul>
 *
 * <h3>Hal-hal non-obvious (WAJIB dibaca sebelum mengubah)</h3>
 * <ul>
 *   <li><b>Banyak getter di sini MENULIS ke field</b>, tidak sekadar membaca. Selain pola
 *   {@code check(...)} standar milik induk, ada getter yang menormalkan/menimpa nilai:
 *   {@link #getJudul()}, {@link #getCatatan()}, {@link #getIsiPolling()},
 *   {@link #getJawabanPolling()} (filter XSS), {@link #getTahunAjaran()} (mengisi tahun akademik
 *   berjalan), {@link #getJudulEn()}/{@link #getCatatanEn()} (menyalin versi Indonesia),
 *   {@link #getFakultas()}/{@link #getYayasan()} (menimpa dari relasi lain),
 *   {@link #getPerguruanTinggi()} (mengisi PT default), dan yang paling berbahaya
 *   {@link #getHanyaUntuk()}/{@link #getHanyaUntukAngkatan()}/{@link #getHanyaUntukUsername()}
 *   yang bisa <b>MENGOSONGKAN daftar putih</b>. Karena entity ini kerap masih attached ke session
 *   Hibernate, perubahan itu dapat ikut ter-flush ke database hanya karena pengumuman dibaca.</li>
 *   <li><b>Dua getter menutup session Hibernate</b> — {@link #ambilOrganizer()} dan
 *   {@link #ambilAttendee()} memanggil {@code HibernateUtil.closeSession()}. Memanggilnya di
 *   tengah unit-of-work lain akan memutus session yang sedang dipakai pemanggil.</li>
 *   <li><b>Daftar komentar TIDAK disimpan sebagai relasi Hibernate</b>. Indeksnya berupa berkas
 *   JSON di luar database (lihat {@link #ambilLokasiDiskusiPengumumanAkademis()}), disegarkan
 *   lewat penanda {@code udah("diskusi")}. Berkas ini adalah cache yang bisa basi/hilang; baris
 *   {@code DiskusiPengumumanAkademis} tetap sumber kebenaran di DB.</li>
 *   <li><b>{@link #galeries} adalah cache statis JVM-wide</b> tanpa sinkronisasi dan tanpa batas
 *   ukuran — lihat catatannya.</li>
 *   <li><b>{@link #toString()} mengembalikan {@code catatan}</b> (isi HTML lengkap), bukan judul.
 *   Jangan pakai untuk label UI atau log ringkas.</li>
 * </ul>
 *
 * <p>Kontrak umum entity — {@code id}, {@code equals}/{@code hashCode}, {@code compareTo},
 * {@code check()}, {@code udah()}/{@code belum()}, filter {@code filterTidakBoleh*}, serta field
 * audit {@code oleh}/{@code tanggal_dirubah} — dijelaskan lengkap di kelas induk. Baca ke sana
 * sebelum menyimpulkan perilaku apa pun di kelas ini.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.action.master.PengumumanAkademisAction
 * @see ais.action.master.TampilanPengumumanAkademisAction
 * @see ais.action.master.helper.BroadcastHelper
 * @see ais.database.model.DiskusiPengumumanAkademis
 * @see ais.database.model.KategoriPengumuman
 * @see ais.database.model.PengumumanPerkuliahan
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pengumuman_akademis")
public class PengumumanAkademis extends GeneralValueObject {

	/**
	 * Audiens "Untuk Umum" — pengumuman publik, tampil ke semua orang termasuk pengunjung yang
	 * belum login. Ini juga nilai default bila {@code diperuntukkan} kosong (lihat
	 * {@link #getDiperuntukkan()}).
	 *
	 * <p><b>Perhatian:</b> nilai konstanta ini adalah teks yang tersimpan apa adanya di kolom
	 * {@code diperuntukkan} dan dibandingkan dengan {@code equals}/{@code equalsIgnoreCase} di
	 * banyak tempat. Mengubah ejaannya akan "mematikan" seluruh baris lama di produksi.</p>
	 */
	public final static String UNTUK_UMUM = "Untuk Umum";

	/**
	 * Audiens mahasiswa (perguruan tinggi). Salah satu dari empat nilai yang mengaktifkan daftar
	 * putih {@link #getHanyaUntuk()} (berisi NIM) dan {@link #getHanyaUntukAngkatan()}.
	 */
	public final static String UNTUK_MAHASISWA = "Untuk Mahasiswa";

	/** Audiens siswa (jalur sekolah/yayasan, bukan perguruan tinggi). */
	public final static String UNTUK_SISWA = "Untuk Siswa";

	/** Audiens guru (jalur sekolah/yayasan). */
	public final static String UNTUK_GURU = "Untuk Guru";

	/**
	 * Audiens alumni. Termasuk salah satu dari empat nilai yang mengaktifkan daftar putih
	 * {@link #getHanyaUntuk()}/{@link #getHanyaUntukAngkatan()}.
	 */
	public final static String UNTUK_ALUMNI = "Untuk Alumni";

	/** Audiens dosen. */
	public final static String UNTUK_DOSEN = "Untuk Dosen";

	/** Audiens pegawai/tenaga kependidikan. */
	public final static String UNTUK_PEGAWAI = "Untuk Pegawai";

	/**
	 * Audiens calon mahasiswa (pendaftar PMB). Pengumuman bertanda ini biasanya tampil di portal
	 * pendaftaran, bukan di portal mahasiswa aktif.
	 */
	public final static String UNTUK_CALON_MAHASISWA = "Untuk Calon Mahasiswa";

	/** Audiens calon siswa (PPDB sekolah). */
	public final static String UNTUK_CALON_SISWA = "Untuk Calon Siswa";

	/** Audiens penyedia/vendor (modul pengadaan aset). */
	public final static String UNTUK_VENDOR = "Untuk Penyedia/Vendor";

	/** Audiens pelamar kerja / kanal informasi lowongan (modul rekrutmen). */
	public final static String UNTUK_KARIR = "Untuk Karir";

	/**
	 * Audiens peserta kegiatan/pelatihan. Termasuk empat nilai yang mengaktifkan daftar putih
	 * {@link #getHanyaUntuk()}/{@link #getHanyaUntukAngkatan()}.
	 */
	public final static String UNTUK_PESERTA = "Untuk Peserta";

	/** Audiens pengguna modul perpustakaan. */
	public final static String UNTUK_PERPUSTAKAAN = "Untuk Perpustakaan";

	/**
	 * Audiens admin/operator (pengguna {@code Tbmuser} back-office).
	 *
	 * <p>Satu-satunya nilai yang mengaktifkan {@link #getHanyaUntukUsername()}. Pada mode ini
	 * {@link #getHanyaUntuk()} <b>berganti makna</b>: isinya dicocokkan terhadap
	 * {@code Tbmrole.roleId} (daftar peran), bukan NIM mahasiswa — lihat filter di
	 * {@code TampilanPengumumanAkademisAction}.</p>
	 */
	public final static String UNTUK_ADMIN = "Untuk Admin";

	/**
	 * Cache statis galeri gambar per pengumuman: {@code id pengumuman -> (id lampiran ->
	 * LampiranLain)}.
	 *
	 * <p>Diisi {@link #reloadGaleries(PengumumanAkademis)} dan dibaca langsung (tanpa getter) oleh
	 * {@code PengumumanAkademisAction.tampilPengumuman(...)} dengan pola "kalau key belum ada,
	 * reload dulu". Isi map inilah yang dirender menjadi satu {@code <img>} atau, bila lebih dari
	 * satu berkas, menjadi slideshow.</p>
	 *
	 * <p><b>Kuirk yang perlu diingat:</b> ini {@code HashMap} statis biasa —
	 * <b>tidak thread-safe</b>, <b>tidak pernah dibersihkan</b> (tumbuh sepanjang umur JVM sesuai
	 * jumlah pengumuman yang pernah dibuka), dan <b>tidak otomatis basi</b>. Setelah galeri
	 * diubah, pemanggil WAJIB memanggil {@link #reloadGaleries(PengumumanAkademis)} sendiri
	 * (dilakukan di jalur simpan galeri pada {@code PengumumanAkademisAction}), kalau tidak gambar
	 * lama akan terus tampil.</p>
	 */
	public static Map<Long, Map<Long, LampiranLain>> galeries = new HashMap<Long, Map<Long, LampiranLain>>();

	/**
	 * Memuat ulang cache galeri gambar {@link #galeries} untuk satu pengumuman.
	 *
	 * <p><b>Cara kerja.</b> Membuka session <i>streaming</i> tersendiri
	 * ({@code StreamingHibernateUtil}) — sengaja terpisah dari session aplikasi agar pembacaan
	 * lampiran (yang bisa besar) tidak mengotori unit-of-work yang sedang berjalan — lalu
	 * mengambil semua {@link LampiranLain} dengan {@code ref = id pengumuman} dan {@code jenis}
	 * berawalan {@code "Galery_Pengumuman_"} (case-insensitive), diurutkan menaik menurut id.
	 * Entri lama untuk pengumuman ini <b>dikosongkan lebih dulu</b> ({@code data.clear()}) sebelum
	 * hasil baru dimasukkan, sehingga berkas yang sudah dihapus ikut hilang dari cache.</p>
	 *
	 * <p><b>Efek samping.</b> Mengubah map statis {@link #galeries} (dampaknya JVM-wide, terlihat
	 * oleh semua sesi pengguna) dan menutup session streaming lewat
	 * {@code StreamingHibernateUtil.closeSession()} pada jalur sukses.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Dari {@code PengumumanAkademisAction} — saat merender pengumuman
	 * bila key-nya belum ada di cache, dan setelah pengguna menambah/menghapus berkas galeri.</p>
	 *
	 * <p><b>Penanganan error.</b> Kegagalan apa pun memicu
	 * {@code StreamingHibernateUtil.rollbackTransaction()} dan dicatat ke audit error; method
	 * tidak melempar exception, sehingga pemanggil bisa mendapati cache tetap kosong/basi tanpa
	 * pemberitahuan. Perhatikan pula bahwa pada jalur gagal session streaming <b>tidak</b>
	 * ditutup di {@code finally}.</p>
	 *
	 * @param pengumumanAkademis pengumuman yang galerinya dimuat ulang; harus sudah ber-id
	 *                           (id dipakai sebagai key cache sekaligus nilai {@code ref})
	 */
	@SuppressWarnings("unchecked")
	public static void reloadGaleries(PengumumanAkademis pengumumanAkademis) {
		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("ref", pengumumanAkademis.getId()))
					.add(Restrictions.ilike("jenis", "Galery_Pengumuman_", MatchMode.START)).list();

			Map<Long, LampiranLain> data = galeries.get(pengumumanAkademis.getId());
			if (data == null) {
				data = new HashMap<Long, LampiranLain>();
				galeries.put(pengumumanAkademis.getId(), data);
			}
			data.clear();
			for (LampiranLain lampiran : lampiranLains) {
				data.put(lampiran.getId(), lampiran);
			}

			StreamingHibernateUtil.getInstance().closeSession();
			lampiranLains = null;
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/PengumumanAkademis.java:97");
		}
	}

	/**
	 * Versi serialisasi Java. Jangan diubah tanpa alasan — entity ini ikut diserialisasi ke cache
	 * MapDB/identity map.
	 */
	private static final long serialVersionUID = 2463822571548439808L;

	/** Kunci utama tabel, {@code IDENTITY} (sequence PostgreSQL). */
	private Long id;

	/**
	 * Callback JPA sebelum baris di-{@code UPDATE}: mendelegasikan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Catatan: hanya ada
	 * {@code @PreUpdate} — tidak ada {@code @PrePersist} — sehingga baris yang baru dibuat
	 * mengandalkan nilai default field dan pengisian manual di layer action.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir (bagian dari trio field audit bayangan
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah}).
	 *
	 * <p><b>Pola berulang di seluruh entity AIS:</b> field ini <b>membayangi</b> field bernama
	 * sama di {@link GeneralValueObject}. Nilai awalnya diambil dari jam server aplikasi
	 * ({@code WaktuUtil.getDate()}), bukan jam database, sehingga object baru selalu punya isi.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir (tanpa validasi).
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 * @see GeneralValueObject#setTanggal_dirubah(Date)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir; dipetakan sebagai {@code TIMESTAMP}
	 * sehingga jam ikut tersimpan.
	 *
	 * @return waktu perubahan terakhir milik instance ini
	 * @see GeneralValueObject#getTanggal_dirubah()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity.
	 *
	 * <p><b>Awas:</b> yang dikembalikan adalah field {@code catatan} MENTAH — yaitu <b>seluruh isi
	 * HTML pengumuman</b>, bukan judul dan bukan ringkasan. Nilainya juga bisa {@code null}
	 * (berbeda dengan {@link #getCatatan()} yang menormalkan ke string kosong). Karena itu jangan
	 * memakai method ini untuk label combobox, judul dialog, atau baris log — pakai
	 * {@link #getJudul()}.</p>
	 *
	 * @return isi pengumuman apa adanya, bisa {@code null}
	 */
	public String toString() {
		return catatan;
	}

	/** Kode audiens; salah satu konstanta {@code UNTUK_*}. Lihat {@link #getDiperuntukkan()}. */
	private String diperuntukkan;

	/** Judul pengumuman (kolom {@code judul}, {@code text}, wajib isi). */
	private String judul;

	/** Judul versi bahasa Inggris (kolom {@code judul_en}); kosong berarti ikut versi Indonesia. */
	private String judulEn;

	/** Isi pengumuman berupa HTML bebas (kolom {@code catatan}, {@code text}, wajib isi). */
	private String catatan;

	/** Isi versi bahasa Inggris (kolom {@code catatan_en}); kosong berarti ikut versi Indonesia. */
	private String catatanEn;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini — field audit bayangan (membayangi field
	 * bernama sama di {@link GeneralValueObject}).
	 */
	private String oleh;

	/**
	 * Identitas (userId/NIM) pengguna terakhir yang mengubah baris ini — pasangan {@link #oleh}
	 * dalam trio field audit bayangan.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna pengubah terakhir.
	 *
	 * @return userId/NIM pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Setter defensif</b> (pola sama seperti {@link #setOleh(String)}): nilai {@code null},
	 * kosong, atau hanya spasi <b>diabaikan diam-diam</b> sehingga jejak audit lama tidak
	 * tertimpa. Konsekuensinya jejak audit tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId userId/NIM pengubah; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Daftar putih penerima, dipisah koma. Berisi NIM untuk audiens mahasiswa/alumni/peserta, dan
	 * {@code roleId} untuk audiens admin. Lihat {@link #getHanyaUntuk()}.
	 */
	private String hanyaUntuk;

	/** Daftar putih tahun angkatan (dipisah koma). Lihat {@link #getHanyaUntukAngkatan()}. */
	private String hanyaUntukAngkatan;

	/**
	 * Daftar putih username {@code Tbmuser} (dipisah koma), hanya berlaku pada audiens
	 * {@link #UNTUK_ADMIN}. Lihat {@link #getHanyaUntukUsername()}.
	 */
	private String hanyaUntukUsername;

	/** Bila true, pengumuman tetap tampil walau {@code sampai} sudah lewat. Default: true. */
	private Boolean tetapTampilkanPengumumanMeskipunSudahKelewat;

	/** Menandai pengumuman memuat tautan video conference internal. */
	private Boolean adaVideoConference;

	/** Menandai pengumuman memuat tautan Google Meet (dibuat lewat integrasi Google Calendar). */
	private Boolean adaVideoConferenceGoogleMeet;

	/** Tanggal mulai berlaku/terbit pengumuman (kolom {@code tanggal}, tipe DATE). */
	private Date tanggal;

	/** Tanggal akhir berlaku pengumuman (kolom {@code sampai}, tipe DATE). */
	private Date sampai;

	/** Pembatas audiens per fakultas; {@code null} = semua fakultas. */
	private Fakultas fakultas;

	/** Pembatas audiens per program studi/jurusan; {@code null} = semua jurusan. */
	private Jurusan jurusan;

	/** Pembatas audiens per perguruan tinggi (instalasi multi-PT); {@code null} = semua PT. */
	private PerguruanTinggi perguruanTinggi;

	/** Pembatas audiens per program (S1/S2/Reguler/dsb) — disimpan sebagai NAMA program, bukan relasi. */
	private String program;

	/** Tahun akademik pengumuman; kosong akan diisi tahun berjalan oleh {@link #getTahunAjaran()}. */
	private String tahunAjaran;

	/** Saklar aktif/nonaktif. Baris nonaktif disaring keluar dari Papan Pengumuman. Default: true. */
	private Boolean aktif;

	/** Flag broadcast e-mail ke seluruh mahasiswa aktif. Lihat {@code BroadcastHelper.broadcastEmail}. */
	private Boolean broadcastKeMahasiswaAktif;

	/** Flag broadcast e-mail ke siswa (jalur sekolah/yayasan). */
	private Boolean broadcastKeSiswaAktif;

	/** Flag broadcast e-mail ke guru (jalur sekolah/yayasan). */
	private Boolean broadcastKeGuru;

	/** Flag broadcast e-mail ke alumni (mahasiswa dengan {@code statusKeluar.id = 1}). */
	private Boolean broadcastKeMahasiswaAlumni;

	/** Flag broadcast e-mail ke mahasiswa yang cutinya disetujui pada semester berjalan. */
	private Boolean broadcastKeMahasiswaCuti;

	/** Flag broadcast e-mail ke dosen. */
	private Boolean broadcastKeDosen;

	/** Flag broadcast e-mail ke pengguna back-office non-dosen (admin/operator). */
	private Boolean broadcastAdmin;

	/** Flag broadcast e-mail ke calon mahasiswa pada tahun akademik pengumuman. */
	private Boolean broadcastCalonMahasiswa;

	/** Mengizinkan pembaca menuliskan komentar/diskusi. Default: true. */
	private Boolean bolehDiberiKomentar;

	/** Menampilkan daftar pengumuman lain di bawah pengumuman ini. */
	private Boolean tampilkanPengumumanLain;

	/** Menandai pengumuman ini sebagai halaman "Profil" yang boleh ditampilkan di area publik. */
	private Boolean tampilkanProfile;

	/**
	 * Daftar {@code Tbmuser.userId} penanggung jawab/korespondensi, dipisah koma. Dipakai sebagai
	 * penerima notifikasi komentar dan sebagai <i>organizer</i> event kalender.
	 */
	private String korespondensi;

	/** Definisi pertanyaan polling dalam bentuk JSON array (kolom {@code text}). */
	private String isiPolling;

	/** Rekapitulasi jawaban polling dalam bentuk JSON object (kolom {@code text}). */
	private String jawabanPolling;

	/**
	 * Nama kelas Java (FQCN) komponen ZK yang disisipkan ke badan pengumuman. Lihat
	 * {@link #getKlassData()} — field ini memicu {@code Class.forName(...).newInstance()}.
	 */
	private String klassData;

	/** Kategori pengumuman; menentukan urutan tampil (lewat {@code kategoriPengumuman.nomorUrut}). */
	private KategoriPengumuman kategoriPengumuman;

	/** Pembatas audiens per sekolah (jalur yayasan); {@code null} = semua sekolah. */
	private Sekolah sekolah;

	/** Pembatas audiens per yayasan; {@code null} = semua yayasan. */
	private Yayasan yayasan;

	/** Jeda pergantian slide galeri dalam DETIK. Lihat {@link #getSlideWaktu()}. */
	private Integer slideWaktu;

	/** Potongan CSS tinggi galeri untuk tampilan desktop (disisipkan mentah ke atribut {@code style}). */
	private String tinggiGaleri;

	/** Potongan CSS tinggi galeri untuk tampilan mobile. */
	private String tinggiGaleriMobile;

	/** Cuplikan JSON hasil sinkronisasi event Google Calendar. Lihat {@link #getCalendarEvent()}. */
	private String calendarEvent;

	/** Menutup kolom komentar walau {@code bolehDiberiKomentar} true. */
	private Boolean komentarDitutup;

	/** Mengizinkan pengunggahan lampiran pada komentar. */
	private Boolean izinkanUploadLampiranDiKomentar;

	/** Mengizinkan pengunggahan lampiran ke penyimpanan "Grive". Default: true. */
	private Boolean izinkanUploadLampiranDiGrive;

	/** Menandakan isi galeri dirender sebagai HTML, bukan sebagai daftar gambar. */
	private Boolean galeryBerupaHtml;

	/** Menampilkan pengumuman ini sebagai tab tersendiri begitu portal dibuka. */
	private Boolean langsungMunculDiTab;

	// Bila true: isi pengumuman ini LANGSUNG tampil penuh di Papan Pengumuman halaman utama
	// (Beranda) tanpa pengguna perlu mengklik judul dulu.
	/**
	 * Bila true, isi pengumuman langsung tampil penuh di Beranda tanpa perlu diklik lebih dulu.
	 * Lihat {@link #getLangsungTampilBeranda()}.
	 */
	private Boolean langsungTampilBeranda;

	/**
	 * Pengumuman induk. Pengumuman ber-induk dirender sebagai TOMBOL di atas/bawah isi induknya,
	 * bukan sebagai baris terpisah. Lihat {@link #getInduk()} dan {@link #getPosisiTombol()}.
	 */
	private PengumumanAkademis induk;

	/** Nilai {@code posisiTombol}: tombol anak dirender DI ATAS isi pengumuman induk (default). */
	public static final String ATAS = "atas";

	/** Nilai {@code posisiTombol}: tombol anak dirender DI BAWAH isi pengumuman induk. */
	public static final String BAWAH = "bawah";

	/** Nilai {@code posisiTombol}: kanan. Disediakan konstantanya, jarang/tidak dipakai perender. */
	public static final String KANAN = "kanan";

	/** Nilai {@code posisiTombol}: kiri. Disediakan konstantanya, jarang/tidak dipakai perender. */
	public static final String KIRI = "kiri";

	/** Posisi tombol pengumuman anak relatif terhadap induknya; salah satu {@link #ATAS}/{@link #BAWAH}. */
	private String posisiTombol;

	/** Teks tombol yang mewakili pengumuman ini bila ia menjadi anak dari pengumuman lain. */
	private String labelTombol;

	/** Nomor urut manual; dipakai {@code compareTo} milik induk untuk mengurutkan daftar pengumuman. */
	private Integer nomorUrut;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Tidak mengisi apa pun: seluruh nilai default (aktif, boleh dikomentari, tanggal, tahun
	 * ajaran, dan sebagainya) ditentukan secara <i>lazy</i> di masing-masing getter, bukan di
	 * sini.</p>
	 */
	public PengumumanAkademis() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id pengumuman, atau {@code null} bila entity masih transient
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul pengumuman setelah disaring filter kata terlarang.
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code filterTidakBoleh(judul)} <b>ditugaskan kembali
	 * ke field</b>. Karena filter versi penuh ini akan meng-<i>uppercase</i> SELURUH teks begitu
	 * satu token terlarang ditemukan (mis. {@code SCRIPT}, {@code FUNCTION}, {@code <BODY}), judul
	 * yang tercemar akan berubah menjadi huruf kapital semua secara permanen bila entity ini
	 * kemudian ter-flush ke database. Filter tidak pernah mengembalikan {@code null}, sehingga
	 * method ini juga tidak.</p>
	 *
	 * @return judul yang sudah difilter, tidak pernah {@code null}
	 * @see GeneralValueObject#filterTidakBoleh(String)
	 */
	@Column(name = "judul", nullable = false, columnDefinition = "text")
	public String getJudul() {
		judul = filterTidakBoleh(judul);
		return this.judul;
	}

	/**
	 * Menyetel judul pengumuman apa adanya (penyaringan baru terjadi saat dibaca).
	 *
	 * @param judul judul pengumuman
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan isi pengumuman (HTML) setelah disaring filter ringan.
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code filterTidakBolehSederhana(catatan)} ditugaskan
	 * kembali ke field. Berbeda dengan {@link #getJudul()}, versi "sederhana" ini hanya menetralkan
	 * kata {@code script} menjadi {@code __S__} dan <b>tidak</b> mengubah kapitalisasi — pilihan
	 * yang tepat karena isi pengumuman adalah HTML yang harus tetap terbaca. Perlu diingat filter
	 * ini bukan sanitizer HTML penuh: atribut event ({@code onerror=}, {@code onclick=}) dan
	 * atribut {@code style} lolos begitu saja, sedangkan isi ini dirender mentah ke halaman.</p>
	 *
	 * @return isi pengumuman; string kosong bila belum diisi (tidak pernah {@code null})
	 * @see GeneralValueObject#filterTidakBolehSederhana(String)
	 */
	@Column(name = "catatan", nullable = false, columnDefinition = "text")
	public String getCatatan() {
		catatan = filterTidakBolehSederhana(catatan);
		return this.catatan == null ? "" : this.catatan;
	}

	/**
	 * Menyetel isi pengumuman apa adanya (penyaringan baru terjadi saat dibaca).
	 *
	 * @param catatan isi pengumuman berupa HTML
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Setter defensif:</b> nilai {@code null}/kosong diabaikan diam-diam agar jejak audit
	 * lama tidak hilang tertimpa proses yang tidak tahu siapa penggunanya.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah pengumuman ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel tanggal mulai berlaku/terbit pengumuman.
	 *
	 * @param tanggal tanggal terbit; boleh {@code null} (getter akan memakai tanggal hari ini)
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tanggal terbit pengumuman.
	 *
	 * <p>Bila field masih {@code null}, dikembalikan tanggal hari ini menurut jam server aplikasi
	 * ({@code WaktuUtil.getDate()}). Berbeda dengan getter lain di kelas ini, nilai pengganti itu
	 * <b>tidak</b> ditulis balik ke field — jadi baris tetap tersimpan dengan {@code tanggal}
	 * NULL, dan setiap pembacaan berikutnya menghasilkan tanggal yang berbeda-beda.</p>
	 *
	 * @return tanggal terbit; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel pembatas jurusan/program studi.
	 *
	 * @param jurusan jurusan penerima; {@code null} berarti tidak dibatasi jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan jurusan pembatas audiens, setelah relasi lazy diresolusikan
	 * {@code check(...)}.
	 *
	 * @return jurusan pembatas, atau {@code null} bila pengumuman berlaku untuk semua jurusan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel pembatas fakultas.
	 *
	 * @param fakultas fakultas penerima; {@code null} berarti tidak dibatasi fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas pembatas audiens.
	 *
	 * <p><b>Getter yang menulis dan menimpa:</b> selain memanggil {@code check(...)}, method ini
	 * <b>menimpa</b> field {@code fakultas} dengan {@code getJurusan().getFakultas()} setiap kali
	 * jurusan terisi. Artinya jurusan selalu menang atas fakultas: nilai fakultas yang disetel
	 * manual akan hilang begitu pengumuman juga punya jurusan, dan perubahan itu bisa ikut
	 * ter-flush ke database hanya karena pengumuman dibaca. Konsistensinya memang masuk akal
	 * (fakultas mesti induk dari jurusan), tetapi efeknya tidak kasat mata di layer pemanggil.</p>
	 *
	 * @return fakultas pembatas, atau {@code null} bila berlaku untuk semua fakultas
	 * @see #getJurusan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getJurusan() != null) {
			fakultas = getJurusan().getFakultas();
		}
		return fakultas;
	}

	/**
	 * Menyetel pembatas yayasan.
	 *
	 * <p><b>Setter yang menormalkan:</b> yayasan tanpa id (object baru/kosong dari combobox)
	 * disimpan sebagai {@code null}, bukan sebagai entity transient. Ini mencegah Hibernate
	 * mencoba meng-{@code PERSIST} yayasan kosong lewat cascade.</p>
	 *
	 * @param yayasan yayasan penerima; {@code null} atau tanpa id dianggap "tidak dibatasi"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan yayasan pembatas audiens.
	 *
	 * <p><b>Getter yang menulis dan menimpa</b> — kembaran {@link #getFakultas()} untuk jalur
	 * sekolah: bila {@link #getSekolah()} terisi, field {@code yayasan} ditimpa dengan yayasan
	 * milik sekolah tersebut. Sekolah selalu menang atas yayasan.</p>
	 *
	 * @return yayasan pembatas, atau {@code null} bila berlaku untuk semua yayasan
	 * @see #getSekolah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Mengembalikan sekolah pembatas audiens, setelah relasi lazy diresolusikan {@code check(...)}.
	 *
	 * @return sekolah pembatas, atau {@code null} bila berlaku untuk semua sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel pembatas sekolah.
	 *
	 * <p><b>Setter yang menormalkan</b> (sama seperti {@link #setYayasan(Yayasan)}): sekolah tanpa
	 * id disimpan sebagai {@code null}.</p>
	 *
	 * @param sekolah sekolah penerima; {@code null} atau tanpa id dianggap "tidak dibatasi"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Menyetel tanggal akhir berlaku pengumuman.
	 *
	 * @param sampai tanggal akhir; boleh {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tanggal akhir berlaku pengumuman.
	 *
	 * <p>Bila belum diisi, dikembalikan tanggal hari ini (tanpa ditulis balik ke field, sama
	 * seperti {@link #getTanggal()}). Perlu dicatat bahwa penyaringan masa berlaku di
	 * {@code TampilanPengumumanAkademisAction} menggabungkan {@code tanggal <= hari ini}
	 * dengan {@code sampai >= hari ini} memakai <b>OR</b>, bukan AND, sehingga dalam praktiknya
	 * rentang tanggal hampir tidak pernah menyembunyikan pengumuman — penyembunyian nyatanya
	 * bergantung pada {@link #getAktif()} dan
	 * {@link #getTetapTampilkanPengumumanMeskipunSudahKelewat()}.</p>
	 *
	 * @return tanggal akhir berlaku; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "sampai")
	public Date getSampai() {
		return sampai == null ? ais.ui.util.WaktuUtil.getDate() : sampai;
	}

	/**
	 * Menyetel tahun akademik pengumuman.
	 *
	 * @param tahunAjaran tahun akademik, format mengikuti {@code Common.getCurrentTahunAkademik()}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan tahun akademik pengumuman.
	 *
	 * <p><b>Getter yang menulis:</b> bila kosong/{@code null}, field diisi dengan tahun akademik
	 * berjalan ({@code Common.getCurrentTahunAkademik()}) dan nilai itu <b>bertahan di object</b>,
	 * sehingga bisa ikut tersimpan saat entity di-flush. Efek nyatanya: pengumuman lama yang
	 * kolomnya masih NULL bisa "berpindah" ke tahun akademik berjalan hanya karena dibuka.
	 * Nilai ini dipakai antara lain untuk menyaring calon mahasiswa penerima broadcast.</p>
	 *
	 * @return tahun akademik; tidak pernah kosong
	 */
	@Column(name = "tahun_ajaran", length = 20, nullable = true)
	public String getTahunAjaran() {
		if (tahunAjaran == null || tahunAjaran.isEmpty()) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return tahunAjaran;
	}

	/**
	 * Mengembalikan kode audiens pengumuman.
	 *
	 * <p><b>Getter yang menulis:</b> nilai {@code null} diisi {@link #UNTUK_UMUM} dan disimpan ke
	 * field. Nilai inilah yang menentukan cabang perilaku {@link #getHanyaUntuk()},
	 * {@link #getHanyaUntukAngkatan()}, dan {@link #getHanyaUntukUsername()}, jadi hati-hati:
	 * mengubah audiens dapat mengosongkan daftar putih (lihat catatan pada method-method
	 * tersebut).</p>
	 *
	 * @return salah satu konstanta {@code UNTUK_*}; tidak pernah {@code null}
	 */
	public String getDiperuntukkan() {
		if (diperuntukkan == null) {
			diperuntukkan = UNTUK_UMUM;
		}
		return diperuntukkan;
	}

	/**
	 * Menyetel kode audiens pengumuman.
	 *
	 * @param diperuntukkan salah satu konstanta {@code UNTUK_*}; nilai di luar daftar tidak
	 *                      divalidasi, tetapi akan membuat seluruh daftar putih dikosongkan saat
	 *                      dibaca
	 */
	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}

	/**
	 * Mengembalikan status aktif pengumuman.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dianggap aktif dan ditulis balik sebagai
	 * {@code true}. Pengumuman nonaktif disaring keluar dari Papan Pengumuman.</p>
	 *
	 * @return {@code true} bila pengumuman aktif; default {@code true}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif pengumuman.
	 *
	 * @param aktif {@code true} untuk menampilkan, {@code false} untuk menyembunyikan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menyatakan apakah pembaca boleh menuliskan komentar/diskusi.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code true} — komentar
	 * TERBUKA secara default. Untuk menutup diskusi tanpa mencabut izin, tersedia
	 * {@link #getKomentarDitutup()}.</p>
	 *
	 * @return {@code true} bila komentar diizinkan; default {@code true}
	 */
	public Boolean getBolehDiberiKomentar() {
		if (bolehDiberiKomentar == null) {
			bolehDiberiKomentar = true;
		}
		return bolehDiberiKomentar;
	}

	/**
	 * Menyetel izin berkomentar.
	 *
	 * @param bolehDiberiKomentar {@code true} bila pembaca boleh berkomentar
	 */
	public void setBolehDiberiKomentar(Boolean bolehDiberiKomentar) {
		this.bolehDiberiKomentar = bolehDiberiKomentar;
	}

	/**
	 * Mengembalikan daftar {@code Tbmuser.userId} penanggung jawab pengumuman (dipisah koma).
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi string kosong dan disimpan
	 * ke field. Nilai ini dipakai di dua tempat: {@code BroadcastHelper} mengirim notifikasi
	 * setiap ada komentar baru ke pengguna-pengguna ini, dan {@link #ambilOrganizer()}
	 * menerjemahkannya menjadi daftar alamat e-mail <i>organizer</i> event Google Calendar.</p>
	 *
	 * @return daftar userId dipisah koma; string kosong bila tidak ada penanggung jawab
	 */
	public String getKorespondensi() {
		if (korespondensi == null) {
			korespondensi = "";
		}
		return korespondensi;
	}

	/**
	 * Menyetel daftar userId penanggung jawab/korespondensi.
	 *
	 * @param korespondensi daftar {@code Tbmuser.userId} dipisah koma
	 */
	public void setKorespondensi(String korespondensi) {
		this.korespondensi = korespondensi;
	}

	/**
	 * Menyatakan apakah pengumuman ini dibroadcast lewat e-mail ke seluruh mahasiswa aktif.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code false} (broadcast
	 * TIDAK aktif secara default). Bila true, {@code BroadcastHelper.broadcastEmail} menarik
	 * pasangan (e-mail, NIM) seluruh mahasiswa yang aktif dan belum keluar, tetap dipersempit oleh
	 * fakultas/jurusan/program pengumuman.</p>
	 *
	 * @return {@code true} bila broadcast ke mahasiswa aktif dinyalakan; default {@code false}
	 * @see ais.action.master.helper.BroadcastHelper
	 */
	public Boolean getBroadcastKeMahasiswaAktif() {
		if (broadcastKeMahasiswaAktif == null) {
			broadcastKeMahasiswaAktif = false;
		}
		return broadcastKeMahasiswaAktif;
	}

	/**
	 * Menyetel flag broadcast ke mahasiswa aktif.
	 *
	 * @param broadcastKeMahasiswaAktif {@code true} untuk mengirim e-mail ke mahasiswa aktif
	 */
	public void setBroadcastKeMahasiswaAktif(Boolean broadcastKeMahasiswaAktif) {
		this.broadcastKeMahasiswaAktif = broadcastKeMahasiswaAktif;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke dosen.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code false}. Penerimanya
	 * adalah {@code Tbmuser} yang punya relasi {@code dosen}, dipersempit fakultas/jurusan
	 * pengumuman.</p>
	 *
	 * @return {@code true} bila broadcast ke dosen dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastKeDosen() {
		if (broadcastKeDosen == null) {
			broadcastKeDosen = false;
		}
		return broadcastKeDosen;
	}

	/**
	 * Menyetel flag broadcast ke dosen.
	 *
	 * @param broadcastKeDosen {@code true} untuk mengirim e-mail ke dosen
	 */
	public void setBroadcastKeDosen(Boolean broadcastKeDosen) {
		this.broadcastKeDosen = broadcastKeDosen;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke pengguna back-office (admin/operator).
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code false}. Penerimanya
	 * adalah {@code Tbmuser} aktif yang relasi {@code dosen}-nya NULL, dipersempit oleh
	 * program/fakultas/jurusan pengumuman.</p>
	 *
	 * @return {@code true} bila broadcast ke admin dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastAdmin() {
		if (broadcastAdmin == null) {
			broadcastAdmin = false;
		}
		return broadcastAdmin;
	}

	/**
	 * Menyetel flag broadcast ke admin/operator.
	 *
	 * @param broadcastAdmin {@code true} untuk mengirim e-mail ke pengguna back-office
	 */
	public void setBroadcastAdmin(Boolean broadcastAdmin) {
		this.broadcastAdmin = broadcastAdmin;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke calon mahasiswa.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code false}. Penerimanya
	 * adalah {@code BiodataCalonMahasiswa} aktif pada {@link #getTahunAjaran()} pengumuman,
	 * dipersempit oleh prodi/fakultas/program. Karena tahun akademik diambil dari getter yang
	 * mengisi sendiri nilainya, pengumuman lama bisa tiba-tiba menyasar angkatan pendaftar
	 * berjalan.</p>
	 *
	 * @return {@code true} bila broadcast ke calon mahasiswa dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastCalonMahasiswa() {
		if (broadcastCalonMahasiswa == null) {
			broadcastCalonMahasiswa = false;
		}
		return broadcastCalonMahasiswa;
	}

	/**
	 * Menyetel flag broadcast ke calon mahasiswa.
	 *
	 * @param broadcastCalonMahasiswa {@code true} untuk mengirim e-mail ke calon mahasiswa
	 */
	public void setBroadcastCalonMahasiswa(Boolean broadcastCalonMahasiswa) {
		this.broadcastCalonMahasiswa = broadcastCalonMahasiswa;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke alumni.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code false}. "Alumni"
	 * didefinisikan {@code BroadcastHelper} sebagai mahasiswa dengan {@code statusKeluar.id = 1}
	 * — id itu di-<i>hardcode</i>, jadi instalasi yang memakai kode status keluar berbeda tidak
	 * akan mendapat penerima apa pun.</p>
	 *
	 * @return {@code true} bila broadcast ke alumni dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastKeMahasiswaAlumni() {
		if (broadcastKeMahasiswaAlumni == null) {
			broadcastKeMahasiswaAlumni = false;
		}
		return broadcastKeMahasiswaAlumni;
	}

	/**
	 * Menyetel flag broadcast ke alumni.
	 *
	 * @param broadcastKeMahasiswaAlumni {@code true} untuk mengirim e-mail ke alumni
	 */
	public void setBroadcastKeMahasiswaAlumni(Boolean broadcastKeMahasiswaAlumni) {
		this.broadcastKeMahasiswaAlumni = broadcastKeMahasiswaAlumni;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke mahasiswa yang sedang cuti.
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi {@code false}. Penerimanya
	 * diambil dari {@code PendaftaranCutiMahasiswa} yang sudah disetujui pada semester
	 * ganjil/genap dan tahun akademik BERJALAN — bukan tahun akademik pengumuman.</p>
	 *
	 * @return {@code true} bila broadcast ke mahasiswa cuti dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastKeMahasiswaCuti() {
		if (broadcastKeMahasiswaCuti == null) {
			broadcastKeMahasiswaCuti = false;
		}
		return broadcastKeMahasiswaCuti;
	}

	/**
	 * Menyetel flag broadcast ke mahasiswa cuti.
	 *
	 * @param broadcastKeMahasiswaCuti {@code true} untuk mengirim e-mail ke mahasiswa cuti
	 */
	public void setBroadcastKeMahasiswaCuti(Boolean broadcastKeMahasiswaCuti) {
		this.broadcastKeMahasiswaCuti = broadcastKeMahasiswaCuti;
	}

	/**
	 * Mengembalikan daftar putih penerima pengumuman, sudah dinormalkan ke bentuk berkoma-pengapit.
	 *
	 * <h4>Apa isinya</h4>
	 * <p>Daftar kunci penerima dipisah koma. Maknanya BERGANTUNG audiens: untuk
	 * {@link #UNTUK_MAHASISWA}/{@link #UNTUK_ALUMNI}/{@link #UNTUK_PESERTA} isinya <b>NIM</b>
	 * mahasiswa, sedangkan untuk {@link #UNTUK_ADMIN} isinya <b>{@code Tbmrole.roleId}</b>
	 * (daftar peran, bukan orang). Penyaringnya di
	 * {@code TampilanPengumumanAkademisAction} berupa {@code ilike "%,<kunci>,%"} — itulah sebabnya
	 * nilai perlu diapit koma di kedua ujungnya, jika tidak kunci pertama dan terakhir tak akan
	 * pernah cocok.</p>
	 *
	 * <h4>Normalisasi yang dilakukan</h4>
	 * <p>Nilai diubah menjadi {@code ",a,b,c,"}, lalu koma ganda dirapikan dengan tiga kali
	 * {@code replaceAll(",,", ",")} berturut-turut (bukan loop sampai konvergen — deretan koma
	 * yang sangat panjang bisa tersisa), dan bentuk yang hanya berisi koma dipulangkan menjadi
	 * string kosong.</p>
	 *
	 * <h4>Efek samping yang berbahaya</h4>
	 * <p><b>Getter ini menulis ke field, dan pada audiens selain keempat nilai di atas ia
	 * MENGOSONGKAN daftar putih</b> ({@code hanyaUntuk = ""}). Jadi: mengubah audiens sebuah
	 * pengumuman — misalnya dari "Untuk Mahasiswa" menjadi "Untuk Umum" — lalu menyimpannya akan
	 * <b>MENGHAPUS permanen daftar NIM</b> yang sudah susah payah diisi, tanpa peringatan apa pun.
	 * Bahkan sekadar merender pengumuman dengan entity yang masih attached bisa memicu
	 * penghapusan itu saat session di-flush.</p>
	 *
	 * @return daftar kunci berkoma-pengapit yang sudah di-{@code trim}; string kosong bila tidak
	 *         ada pembatasan atau audiensnya tidak mendukung daftar putih
	 */
	@Column(columnDefinition = "text")
	public String getHanyaUntuk() {

		if (getDiperuntukkan().equals(PengumumanAkademis.UNTUK_ALUMNI)
				|| getDiperuntukkan().equals(PengumumanAkademis.UNTUK_MAHASISWA)
				|| getDiperuntukkan().equals(PengumumanAkademis.UNTUK_PESERTA)
				|| getDiperuntukkan().equals(PengumumanAkademis.UNTUK_ADMIN)) {
			hanyaUntuk = (hanyaUntuk == null || hanyaUntuk.trim().equalsIgnoreCase(",") ? ""
					: "," + hanyaUntuk.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

			if (hanyaUntuk.equals(",")) {
				hanyaUntuk = "";
			} else if (hanyaUntuk.equals(",,")) {
				hanyaUntuk = "";
			} else if (hanyaUntuk.equals(",,,")) {
				hanyaUntuk = "";
			}
		} else {
			hanyaUntuk = "";
		}

		return hanyaUntuk.trim();
	}

	/**
	 * Menyetel daftar putih penerima apa adanya (normalisasi baru terjadi saat dibaca).
	 *
	 * @param hanyaUntuk daftar NIM (atau {@code roleId} untuk audiens admin) dipisah koma
	 * @see #getHanyaUntuk()
	 */
	public void setHanyaUntuk(String hanyaUntuk) {
		this.hanyaUntuk = hanyaUntuk;
	}

	/**
	 * Menyatakan apakah pengumuman tetap ditampilkan meski masa berlakunya sudah lewat.
	 *
	 * <p>{@code null} diperlakukan sebagai {@code true} — <b>default-nya "tetap tampil"</b> —
	 * tanpa ditulis balik ke field. Bersama klausa rentang tanggal yang memakai OR (lihat
	 * {@link #getSampai()}), inilah alasan pengumuman lama umumnya tidak pernah hilang sendiri
	 * dan harus dinonaktifkan manual lewat {@link #setAktif(Boolean)}.</p>
	 *
	 * @return {@code true} bila pengumuman kedaluwarsa tetap ditampilkan; default {@code true}
	 */
	public Boolean getTetapTampilkanPengumumanMeskipunSudahKelewat() {
		return tetapTampilkanPengumumanMeskipunSudahKelewat == null ? true
				: tetapTampilkanPengumumanMeskipunSudahKelewat;
	}

	/**
	 * Menyetel perilaku tampil setelah masa berlaku lewat.
	 *
	 * @param tetapTampilkanPengumumanMeskipunSudahKelewat {@code false} agar pengumuman menghilang
	 *                                                     setelah tanggal {@code sampai}
	 */
	public void setTetapTampilkanPengumumanMeskipunSudahKelewat(Boolean tetapTampilkanPengumumanMeskipunSudahKelewat) {
		this.tetapTampilkanPengumumanMeskipunSudahKelewat = tetapTampilkanPengumumanMeskipunSudahKelewat;
	}

	/**
	 * Mengembalikan nama program pembatas audiens (mis. "Reguler", "S1").
	 *
	 * <p>Disimpan sebagai teks NAMA program, bukan relasi ke entity {@code Program}. Perhatikan
	 * bahwa di data produksi pengumuman umum kerap menyimpan string KOSONG, bukan {@code null} —
	 * penyaring di {@code TampilanPengumumanAkademisAction} sudah diperbaiki agar memperlakukan
	 * keduanya sama.</p>
	 *
	 * @return nama program, string kosong, atau {@code null}; ketiganya berarti "berlaku umum"
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel nama program pembatas audiens.
	 *
	 * @param program nama program; {@code null}/kosong berarti tidak dibatasi program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan daftar putih tahun angkatan, dinormalkan ke bentuk berkoma-pengapit.
	 *
	 * <p>Perilakunya identik dengan {@link #getHanyaUntuk()} — termasuk <b>efek samping
	 * mengosongkan field</b> ketika audiens bukan mahasiswa/alumni/peserta/admin — hanya saja
	 * kunci yang dicocokkan adalah {@code Mahasiswa.tahunangkatan}. Berbeda sedikit dari saudaranya,
	 * baris {@code return} di sini masih memeriksa {@code null} walau pada titik itu field
	 * dipastikan sudah tidak {@code null} (sisa penulisan defensif).</p>
	 *
	 * @return daftar angkatan berkoma-pengapit; string kosong bila tidak ada pembatasan
	 * @see #getHanyaUntuk()
	 */
	@Column(columnDefinition = "text")
	public String getHanyaUntukAngkatan() {
		if (getDiperuntukkan().equals(PengumumanAkademis.UNTUK_ALUMNI)
				|| getDiperuntukkan().equals(PengumumanAkademis.UNTUK_MAHASISWA)
				|| getDiperuntukkan().equals(PengumumanAkademis.UNTUK_PESERTA)
				|| getDiperuntukkan().equals(PengumumanAkademis.UNTUK_ADMIN)) {
			hanyaUntukAngkatan = (hanyaUntukAngkatan == null || hanyaUntukAngkatan.trim().equalsIgnoreCase(",") ? ""
					: "," + hanyaUntukAngkatan.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
					.replaceAll(",,", ",");

			if (hanyaUntukAngkatan.equals(",")) {
				hanyaUntukAngkatan = "";
			} else if (hanyaUntukAngkatan.equals(",,")) {
				hanyaUntukAngkatan = "";
			} else if (hanyaUntukAngkatan.equals(",,,")) {
				hanyaUntukAngkatan = "";
			}
		} else {
			hanyaUntukAngkatan = "";
		}
		return hanyaUntukAngkatan == null ? "" : hanyaUntukAngkatan.trim();
	}

	/**
	 * Menyetel daftar putih tahun angkatan apa adanya.
	 *
	 * @param hanyaUntukAngkatan daftar tahun angkatan dipisah koma
	 * @see #getHanyaUntukAngkatan()
	 */
	public void setHanyaUntukAngkatan(String hanyaUntukAngkatan) {
		this.hanyaUntukAngkatan = hanyaUntukAngkatan;
	}

	/**
	 * Mengembalikan kategori pengumuman, setelah relasi lazy diresolusikan {@code check(...)}.
	 *
	 * <p>Kategori menentukan urutan tampil di Papan Pengumuman: daftar diurutkan menaik menurut
	 * {@code kategoriPengumuman.nomorUrut}, lalu menurun menurut {@code tanggal}.</p>
	 *
	 * @return kategori pengumuman, atau {@code null} bila belum dikategorikan
	 * @see ais.database.model.KategoriPengumuman
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_pengumuman", nullable = true)
	public KategoriPengumuman getKategoriPengumuman() {
		kategoriPengumuman = check(kategoriPengumuman);
		return kategoriPengumuman;
	}

	/**
	 * Menyetel kategori pengumuman.
	 *
	 * @param kategoriPengumuman kategori; boleh {@code null}
	 */
	public void setKategoriPengumuman(KategoriPengumuman kategoriPengumuman) {
		this.kategoriPengumuman = kategoriPengumuman;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke siswa (jalur sekolah/yayasan).
	 *
	 * <p>Berbeda dengan saudara-saudaranya di atas, getter ini <b>tidak</b> menulis balik ke
	 * field: {@code null} hanya dipetakan menjadi {@code false} pada nilai kembalian. Penerimanya
	 * adalah {@code Siswa} bernama dan bersekolah, dipersempit sekolah/yayasan pengumuman; yang
	 * dikumpulkan adalah {@code alamatEmail} dan {@code nomorIndukNasional}.</p>
	 *
	 * @return {@code true} bila broadcast ke siswa dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastKeSiswaAktif() {
		return broadcastKeSiswaAktif == null ? false : broadcastKeSiswaAktif;
	}

	/**
	 * Menyetel flag broadcast ke siswa.
	 *
	 * @param broadcastKeSiswaAktif {@code true} untuk mengirim e-mail ke siswa
	 */
	public void setBroadcastKeSiswaAktif(Boolean broadcastKeSiswaAktif) {
		this.broadcastKeSiswaAktif = broadcastKeSiswaAktif;
	}

	/**
	 * Menyatakan apakah pengumuman dibroadcast ke guru.
	 *
	 * <p>Tidak menulis balik ke field. Penerimanya adalah {@code Tbmuser} aktif yang punya relasi
	 * {@code guru}, dipersempit sekolah/yayasan pengumuman.</p>
	 *
	 * @return {@code true} bila broadcast ke guru dinyalakan; default {@code false}
	 */
	public Boolean getBroadcastKeGuru() {
		return broadcastKeGuru == null ? false : broadcastKeGuru;
	}

	/**
	 * Menyetel flag broadcast ke guru.
	 *
	 * @param broadcastKeGuru {@code true} untuk mengirim e-mail ke guru
	 */
	public void setBroadcastKeGuru(Boolean broadcastKeGuru) {
		this.broadcastKeGuru = broadcastKeGuru;
	}

	/**
	 * Mengembalikan jeda pergantian slide galeri dalam DETIK.
	 *
	 * <p>Nilai {@code null} maupun angka {@code <= 0} dipulangkan sebagai {@code 3} (tanpa ditulis
	 * balik ke field), sehingga slideshow tidak pernah mendapat interval nol yang akan membuat
	 * {@code setTimeout} berputar tanpa jeda. Perender mengalikannya dengan 1000 untuk memperoleh
	 * milidetik.</p>
	 *
	 * @return jeda slide dalam detik; minimal 3
	 */
	public Integer getSlideWaktu() {
		return slideWaktu == null || slideWaktu.intValue() <= 0 ? 3 : slideWaktu;
	}

	/**
	 * Menyetel jeda pergantian slide galeri.
	 *
	 * @param slideWaktu jeda dalam detik; nilai {@code <= 0} akan diabaikan saat dibaca
	 */
	public void setSlideWaktu(Integer slideWaktu) {
		this.slideWaktu = slideWaktu;
	}

	/**
	 * Menyatakan apakah pengumuman menyertakan tautan video conference internal.
	 *
	 * <p>Bila flag ini atau {@link #getAdaVideoConferenceGoogleMeet()} bernilai true, halaman
	 * tampilan pengumuman menambahkan blok/tombol konferensi.</p>
	 *
	 * @return {@code true} bila video conference internal diaktifkan; default {@code false}
	 */
	public Boolean getAdaVideoConference() {
		return adaVideoConference == null ? false : adaVideoConference;
	}

	/**
	 * Menyetel flag video conference internal.
	 *
	 * @param adaVideoConference {@code true} bila pengumuman memuat konferensi internal
	 */
	public void setAdaVideoConference(Boolean adaVideoConference) {
		this.adaVideoConference = adaVideoConference;
	}

	/**
	 * Mengembalikan daftar putih username {@code Tbmuser}, dinormalkan ke bentuk berkoma-pengapit.
	 *
	 * <p>Berbeda dari {@link #getHanyaUntuk()}, daftar ini hanya hidup pada audiens
	 * {@link #UNTUK_ADMIN}; untuk audiens lain field <b>dikosongkan</b> (efek samping yang sama
	 * berbahayanya — mengganti audiens akan menghapus daftar username). Nilainya dipakai
	 * penyaring Papan Pengumuman untuk mencocokkan {@code Tbmuser.userId} — dan, pada beberapa
	 * cabang, NIM mahasiswa — serta oleh {@link #ambilAttendee()} untuk menyusun daftar peserta
	 * event kalender.</p>
	 *
	 * @return daftar username berkoma-pengapit; string kosong bila tidak ada pembatasan
	 * @see #getHanyaUntuk()
	 */
	@Column(columnDefinition = "text")
	public String getHanyaUntukUsername() {
		if (getDiperuntukkan().equals(PengumumanAkademis.UNTUK_ADMIN)) {
			hanyaUntukUsername = (hanyaUntukUsername == null || hanyaUntukUsername.trim().equalsIgnoreCase(",") ? ""
					: "," + hanyaUntukUsername.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
					.replaceAll(",,", ",");

			if (hanyaUntukUsername.equals(",")) {
				hanyaUntukUsername = "";
			} else if (hanyaUntukUsername.equals(",,")) {
				hanyaUntukUsername = "";
			} else if (hanyaUntukUsername.equals(",,,")) {
				hanyaUntukUsername = "";
			}
		} else {
			hanyaUntukUsername = "";
		}
		return hanyaUntukUsername == null ? "" : hanyaUntukUsername.trim();
	}

	/**
	 * Menyetel daftar putih username apa adanya.
	 *
	 * @param hanyaUntukUsername daftar {@code Tbmuser.userId} dipisah koma
	 * @see #getHanyaUntukUsername()
	 */
	public void setHanyaUntukUsername(String hanyaUntukUsername) {
		this.hanyaUntukUsername = hanyaUntukUsername;
	}

	/**
	 * Mengembalikan cuplikan JSON event Google Calendar yang terkait pengumuman ini.
	 *
	 * <p>Isinya adalah hasil {@code Event.toPrettyString()} dari Google Calendar API, disimpan
	 * kembali oleh {@code CalendarUtil} setelah event dibuat/diperbarui (termasuk tautan
	 * {@code htmlLink} dan {@code hangoutLink}). Bila kosong dikembalikan {@code "{}"} agar
	 * pemanggil selalu bisa langsung memakainya sebagai {@code JSONObject} tanpa cek null. Nilai
	 * pengganti tidak ditulis balik ke field.</p>
	 *
	 * @return JSON event kalender, atau {@code "{}"} bila belum pernah disinkronkan
	 * @see ais.common.calendar.CalendarUtil
	 */
	@Column(columnDefinition = "text")
	public String getCalendarEvent() {
		return calendarEvent == null || calendarEvent.trim().isEmpty() ? new JSONObject().toString() : calendarEvent;
	}

	/**
	 * Menyetel cuplikan JSON event Google Calendar.
	 *
	 * @param calendarEvent representasi JSON event; biasanya diisi {@code CalendarUtil}
	 */
	public void setCalendarEvent(String calendarEvent) {
		this.calendarEvent = calendarEvent;
	}

	/**
	 * Menyatakan apakah pengumuman menyertakan Google Meet.
	 *
	 * <p>Bila true, jalur simpan pengumuman memicu pembuatan event Google Calendar berikut
	 * tautan Hangout/Meet-nya lewat {@code CalendarUtil}; hasilnya disimpan ke
	 * {@link #getCalendarEvent()}.</p>
	 *
	 * @return {@code true} bila Google Meet diaktifkan; default {@code false}
	 */
	public Boolean getAdaVideoConferenceGoogleMeet() {
		return adaVideoConferenceGoogleMeet == null ? false : adaVideoConferenceGoogleMeet;
	}

	/**
	 * Menyetel flag Google Meet.
	 *
	 * @param adaVideoConferenceGoogleMeet {@code true} untuk membuat event + tautan Google Meet
	 */
	public void setAdaVideoConferenceGoogleMeet(Boolean adaVideoConferenceGoogleMeet) {
		this.adaVideoConferenceGoogleMeet = adaVideoConferenceGoogleMeet;
	}

	/**
	 * Menyusun daftar alamat e-mail <b>penyelenggara</b> (organizer) event kalender pengumuman ini.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Bila {@link #getKorespondensi()} tidak kosong, daftar userId-nya dipecah per koma dan
	 *   dicari di {@code Tbmuser} yang aktif (atau {@code aktif} NULL), mengambil kolom
	 *   {@code email} yang tidak null dan tidak kosong lewat {@code groupProperty}.</li>
	 *   <li>Setiap nilai e-mail masih dipecah lagi per koma (satu pengguna boleh punya beberapa
	 *   alamat) dan divalidasi {@code Common.isValidEmailAddress}.</li>
	 *   <li>Terakhir, alamat pada konfigurasi {@code alamat_email_monitoring} SELALU ditambahkan
	 *   — bahkan ketika tidak ada korespondensi sama sekali. Jadi method ini bisa mengembalikan
	 *   daftar tidak kosong walau pengumuman tak punya penanggung jawab.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping penting:</b> method ini memanggil
	 * {@code HibernateUtil.currentNativeSession()} lalu {@code HibernateUtil.closeSession()} —
	 * artinya ia <b>MENUTUP session Hibernate milik thread</b>. Memanggilnya di tengah unit-of-work
	 * lain akan memutus session yang sedang dipakai pemanggil. Penutupan juga tidak berada di blok
	 * {@code finally}, sehingga exception di tengah query meninggalkan session terbuka.</p>
	 *
	 * <p><b>Hasilnya adalah {@code List}, bukan {@code Set}</b> (berbeda dengan
	 * {@link #ambilAttendee()}), sehingga alamat yang sama bisa muncul lebih dari sekali; pemanggil
	 * di {@code CalendarUtil} memakainya untuk menandai atribut {@code organizer} pada tiap
	 * peserta.</p>
	 *
	 * @return daftar alamat e-mail penyelenggara; bisa kosong, tidak pernah {@code null}
	 * @see #ambilAttendee()
	 * @see ais.common.calendar.CalendarUtil
	 */
	@SuppressWarnings("unchecked")
	public List<String> ambilOrganizer() {
		List<String> emails = new ArrayList<String>();

		if (!getKorespondensi().isEmpty()) {

			Session session = HibernateUtil.currentNativeSession();
			List<String> d = session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("userId", getKorespondensi().split(",")))
					.setProjection(Projections.groupProperty("email")).add(Restrictions.isNotNull("email"))
					.add(Restrictions.ne("email", "")).list();
			HibernateUtil.closeSession();
			for (String email : d) {
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}

		String email = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
		for (String e : email.split(",")) {
			if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
				emails.add(e.trim());
			}
		}
		return emails;
	}

	/**
	 * Menyusun himpunan alamat e-mail <b>peserta</b> (attendee) event kalender pengumuman ini.
	 *
	 * <h4>Cara kerja</h4>
	 * <p>Dua sumber digabung ke dalam satu {@code HashSet} (duplikat otomatis hilang):</p>
	 * <ol>
	 *   <li>{@link #getHanyaUntukUsername()} → dicocokkan ke {@code Tbmuser.userId} yang aktif,
	 *   diambil kolom {@code email}.</li>
	 *   <li>{@link #getHanyaUntuk()} → dicocokkan ke {@code Mahasiswa.nim} yang aktif, diambil
	 *   kolom {@code email}.</li>
	 * </ol>
	 * <p>Tiap nilai masih dipecah per koma dan divalidasi {@code Common.isValidEmailAddress}.</p>
	 *
	 * <p><b>Konsekuensi dari sumber datanya:</b> karena kedua daftar putih itu dikosongkan sendiri
	 * oleh getter-nya ketika audiens bukan mahasiswa/alumni/peserta/admin (lihat
	 * {@link #getHanyaUntuk()}), pengumuman "Untuk Umum" praktis <b>tidak pernah</b> punya peserta
	 * kalender — undangan hanya berisi organizer. Ini bukan bug di method ini, melainkan turunan
	 * langsung dari perilaku getter daftar putih.</p>
	 *
	 * <p><b>Efek samping penting:</b> sama seperti {@link #ambilOrganizer()}, method ini memanggil
	 * {@code HibernateUtil.closeSession()} — bahkan DUA kali bila kedua daftar terisi — sehingga
	 * <b>menutup session Hibernate milik thread</b> pemanggil.</p>
	 *
	 * @return himpunan alamat e-mail peserta; bisa kosong, tidak pernah {@code null}
	 * @see #ambilOrganizer()
	 */
	@SuppressWarnings("unchecked")
	public Set<String> ambilAttendee() {
		Set<String> emails = new HashSet<String>();

		if (!getHanyaUntukUsername().isEmpty()) {

			Session session = HibernateUtil.currentNativeSession();
			List<String> d = session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("userId", getHanyaUntukUsername().split(",")))
					.setProjection(Projections.groupProperty("email")).add(Restrictions.isNotNull("email"))
					.add(Restrictions.ne("email", "")).list();
			HibernateUtil.closeSession();
			for (String email : d) {
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}

		if (!getHanyaUntuk().isEmpty()) {

			Session session = HibernateUtil.currentNativeSession();
			List<String> d = session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("nim", getHanyaUntuk().split(",")))
					.setProjection(Projections.groupProperty("email")).add(Restrictions.isNotNull("email"))
					.add(Restrictions.ne("email", "")).list();
			HibernateUtil.closeSession();
			for (String email : d) {
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}

		return emails;
	}

	/**
	 * Mengembalikan definisi polling pengumuman sebagai teks JSON array.
	 *
	 * <p>Strukturnya berupa array pertanyaan/pilihan yang di-parse pemanggil dengan
	 * {@code new JSONArray(...)}. Bila belum diisi dikembalikan {@code "[]"} sehingga pemanggil
	 * tidak perlu memeriksa null.</p>
	 *
	 * <p><b>Getter yang menulis:</b> isi disaring {@code filterTidakBoleh(...)} dan hasilnya
	 * ditugaskan kembali ke field. Karena filter versi penuh meng-<i>uppercase</i> seluruh teks
	 * bila menemukan token terlarang, JSON polling yang tercemar bisa berubah menjadi huruf besar
	 * semua — dan karena nama kunci JSON bersifat <i>case-sensitive</i>, itu berpotensi merusak
	 * pembacaan polling. Nilai pengganti {@code "[]"} sendiri tidak ditulis balik.</p>
	 *
	 * @return JSON array definisi polling; minimal {@code "[]"}
	 * @see #getJawabanPolling()
	 */
	@Column(columnDefinition = "text")
	public String getIsiPolling() {
		isiPolling = filterTidakBoleh(isiPolling);
		return isiPolling == null || isiPolling.trim().isEmpty() ? new JSONArray().toString() : isiPolling.trim();
	}

	/**
	 * Menyetel definisi polling.
	 *
	 * @param isiPolling teks JSON array definisi polling
	 */
	public void setIsiPolling(String isiPolling) {
		this.isiPolling = isiPolling;
	}

	/**
	 * Mengembalikan rekapitulasi jawaban polling sebagai teks JSON object.
	 *
	 * <p>Dipakai {@code PengumumanAkademisAction} dengan pola baca-ubah-tulis: JSON diambil,
	 * jawaban pemilih ditambahkan, lalu disimpan kembali lewat
	 * {@link #setJawabanPolling(String)}. Bila belum ada jawaban dikembalikan {@code "{}"}.</p>
	 *
	 * <p><b>Getter yang menulis</b> — berlaku catatan filter yang sama dengan
	 * {@link #getIsiPolling()}. Perlu diingat pula bahwa pola baca-ubah-tulis di atas tidak
	 * terkunci: dua pemilih yang menekan tombol bersamaan bisa saling menimpa jawaban.</p>
	 *
	 * @return JSON object jawaban polling; minimal {@code "{}"}
	 */
	@Column(columnDefinition = "text")
	public String getJawabanPolling() {
		jawabanPolling = filterTidakBoleh(jawabanPolling);
		return jawabanPolling == null || jawabanPolling.trim().isEmpty() ? new JSONObject().toString()
				: jawabanPolling.trim();
	}

	/**
	 * Menyetel rekapitulasi jawaban polling.
	 *
	 * @param jawabanPolling teks JSON object jawaban polling
	 */
	public void setJawabanPolling(String jawabanPolling) {
		this.jawabanPolling = jawabanPolling;
	}

	/**
	 * Menyatakan apakah kolom komentar sudah ditutup.
	 *
	 * <p>Berbeda peran dengan {@link #getBolehDiberiKomentar()}: izin komentar menentukan apakah
	 * fitur diskusi ada sama sekali, sedangkan flag ini menutup penambahan komentar baru tanpa
	 * menyembunyikan komentar yang sudah ada.</p>
	 *
	 * @return {@code true} bila komentar ditutup; default {@code false}
	 */
	public Boolean getKomentarDitutup() {
		return komentarDitutup == null ? false : komentarDitutup;
	}

	/**
	 * Menyetel status penutupan kolom komentar.
	 *
	 * @param komentarDitutup {@code true} untuk menghentikan komentar baru
	 */
	public void setKomentarDitutup(Boolean komentarDitutup) {
		this.komentarDitutup = komentarDitutup;
	}

	/**
	 * Menyatakan apakah pembaca boleh melampirkan berkas pada komentarnya.
	 *
	 * @return {@code true} bila lampiran pada komentar diizinkan; default {@code false}
	 */
	public Boolean getIzinkanUploadLampiranDiKomentar() {
		return izinkanUploadLampiranDiKomentar == null ? false : izinkanUploadLampiranDiKomentar;
	}

	/**
	 * Menyetel izin melampirkan berkas pada komentar.
	 *
	 * @param izinkanUploadLampiranDiKomentar {@code true} untuk mengizinkan lampiran komentar
	 */
	public void setIzinkanUploadLampiranDiKomentar(Boolean izinkanUploadLampiranDiKomentar) {
		this.izinkanUploadLampiranDiKomentar = izinkanUploadLampiranDiKomentar;
	}

	/**
	 * Menyatakan apakah pengunggahan lampiran ke penyimpanan "Grive" diizinkan.
	 *
	 * <p>Perhatikan default-nya <b>{@code true}</b> (berlawanan dengan
	 * {@link #getIzinkanUploadLampiranDiKomentar()} yang default {@code false}): pengumuman lama
	 * yang kolomnya masih NULL otomatis mengizinkan fitur ini.</p>
	 *
	 * @return {@code true} bila unggah ke Grive diizinkan; default {@code true}
	 */
	public Boolean getIzinkanUploadLampiranDiGrive() {
		return izinkanUploadLampiranDiGrive == null ? true : izinkanUploadLampiranDiGrive;
	}

	/**
	 * Menyetel izin pengunggahan lampiran ke Grive.
	 *
	 * @param izinkanUploadLampiranDiGrive {@code true} untuk mengizinkan
	 */
	public void setIzinkanUploadLampiranDiGrive(Boolean izinkanUploadLampiranDiGrive) {
		this.izinkanUploadLampiranDiGrive = izinkanUploadLampiranDiGrive;
	}

	/**
	 * Membaca indeks komentar pengumuman ini dari berkas cache di luar database.
	 *
	 * <p><b>Kenapa ada.</b> Daftar komentar TIDAK dipetakan sebagai koleksi Hibernate. Sebagai
	 * gantinya, id-id {@code DiskusiPengumumanAkademis} milik pengumuman ini disimpan sebagai
	 * kunci pada sebuah berkas JSON bernama
	 * {@code pengumumanAkademis_punya_diskusi_<id>} di lokasi kerja
	 * ({@code Common.getFileLocation}). Ini menghindari query relasi mahal setiap kali papan
	 * pengumuman dirender.</p>
	 *
	 * <p><b>Bentuk isi.</b> {@code JSONObject} dengan kunci = id komentar dan nilai = id komentar
	 * yang sama (nilai KOSONG berarti komentar sudah dihapus — lihat
	 * {@link #removeDiskusiPengumumanAkademis(Serializable)}).</p>
	 *
	 * <p><b>Kegagalan &amp; jebakan.</b> Berkas hilang/rusak/tidak terbaca menghasilkan
	 * {@code VOMahasiswa.dataJSON} yaitu {@code "{}"}; exception hanya dicatat ke audit. Berbeda
	 * dari method sejenis di {@code VOMahasiswa}, di sini <b>tidak ada penjagaan id null</b>:
	 * memanggilnya pada entity yang belum tersimpan akan melempar {@code NullPointerException}
	 * pada {@code getId().toString()} sebelum masuk blok {@code try}.</p>
	 *
	 * @return teks JSON indeks komentar; minimal {@code "{}"}
	 * @see #tulisLokasiDiskusiPengumumanAkademis(String)
	 */
	public String ambilLokasiDiskusiPengumumanAkademis() {
		File file = Common.getFileLocation(this, "pengumumanAkademis_punya_diskusi_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:719");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas indeks komentar pengumuman ini.
	 *
	 * <p>Menulis {@code data} apa adanya ke berkas
	 * {@code pengumumanAkademis_punya_diskusi_<id>}; isi lama hilang. Kegagalan penulisan hanya
	 * dicatat ke audit dan <b>tidak dilaporkan ke pemanggil</b>, sehingga indeks bisa gagal
	 * tersimpan tanpa ada yang tahu — komentar tetap ada di database tetapi tidak muncul sampai
	 * indeks dibangun ulang oleh {@link #reInitDiskusiPengumumanAkademis(Session)}.</p>
	 *
	 * @param data teks JSON indeks komentar yang akan ditulis
	 */
	public void tulisLokasiDiskusiPengumumanAkademis(String data) {
		File file = Common.getFileLocation(this, "pengumumanAkademis_punya_diskusi_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:728");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks komentar pengumuman ini.
	 *
	 * <p>Dipakai sebagai langkah pertama pembangunan ulang indeks
	 * ({@link #reInitDiskusiPengumumanAkademis(Session)}). Data komentar di database tidak
	 * tersentuh — yang dihapus hanya cache-nya.</p>
	 */
	public void bersihkanLokasiDiskusiPengumumanAkademis() {
		File file = Common.getFileLocation(this, "pengumumanAkademis_punya_diskusi_" + getId().toString());
		BacaTulisUtil.doHapus(file, "pengumumanAkademis_punya_diskusi");

	}

	/**
	 * Membangun ulang indeks komentar dari database.
	 *
	 * <p><b>Alur.</b> Mengambil seluruh id {@code DiskusiPengumumanAkademis} yang menunjuk ke
	 * pengumuman ini (diurutkan menaik), menghapus berkas indeks lama, menulis indeks kosong
	 * {@code "{}"}, lalu memasukkan id satu per satu lewat
	 * {@link #populateDiskusiPengumumanAkademis(Long, boolean)}.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Hanya dari {@link #ambilJumlahDiskusiPengumumanAkademis(Mahasiswa,
	 * Dosen)} dan {@link #ambilDiskusiPengumumanAkademisTotal(boolean, Long, boolean)}, ketika
	 * penanda berkas {@code udah("diskusi")} belum terpasang — jadi normalnya sekali per
	 * pengumuman per daur hidup penanda.</p>
	 *
	 * <p><b>Biaya.</b> Setiap id memicu satu siklus baca-berkas + tulis-berkas penuh, sehingga
	 * pembangunan ulang pada pengumuman dengan ratusan komentar berarti ratusan operasi I/O.</p>
	 *
	 * @param session session Hibernate yang dipakai untuk query id komentar; harus sudah terbuka
	 */
	@SuppressWarnings("unchecked")
	private void reInitDiskusiPengumumanAkademis(Session session) {
		List<Long> diskusiPengumumanAkademiss = session.createCriteria(DiskusiPengumumanAkademis.class)
				.setProjection(Projections.property("id")).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pengumumanAkademis", this)).list();
		bersihkanLokasiDiskusiPengumumanAkademis();
		tulisLokasiDiskusiPengumumanAkademis(new JSONObject().toString());
		for (Long diskusiPengumumanAkademis : diskusiPengumumanAkademiss) {
			populateDiskusiPengumumanAkademis(diskusiPengumumanAkademis, true);
		}
		diskusiPengumumanAkademiss = null;
	}

	/**
	 * Menandai sebuah komentar sebagai "dihapus" pada indeks berkas.
	 *
	 * <p><b>Perhatikan: kunci TIDAK dibuang dari JSON</b>, melainkan nilainya diganti string
	 * kosong. Pembaca indeks ({@link #ambilJumlahDiskusiPengumumanAkademis(Mahasiswa, Dosen)} dan
	 * {@link #ambilDiskusiPengumumanAkademisTotal(boolean, Long, boolean)}) memang melewati entri
	 * bernilai kosong, jadi efeknya benar; konsekuensinya berkas indeks terus tumbuh berisi
	 * nisan-nisan id lama sampai dibangun ulang.</p>
	 *
	 * <p>Kegagalan (indeks rusak, berkas tak bisa ditulis) hanya dicatat ke audit; pemanggil tidak
	 * pernah tahu bahwa penghapusan gagal.</p>
	 *
	 * @param id id komentar yang ditandai terhapus; dipakai lewat {@code toString()}
	 */
	public void removeDiskusiPengumumanAkademis(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiDiskusiPengumumanAkademis());
			c.put(id.toString(), "");
			tulisLokasiDiskusiPengumumanAkademis(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:758");

		}
	}

	/**
	 * Mendaftarkan sebuah komentar ke indeks berkas pengumuman ini.
	 *
	 * <p>Membaca indeks, menambahkan pasangan {@code "<id>" : "<id>"}, lalu menulisnya kembali.
	 * Bila id-nya sudah ada, isinya sekadar ditimpa dengan nilai sama — itulah cara sebuah
	 * komentar yang sebelumnya ditandai terhapus bisa "hidup" lagi.</p>
	 *
	 * <p><b>Parameter {@code tulisUlang} tidak dipakai sama sekali</b> di badan method (sisa
	 * rancangan lama); memanggil dengan {@code true} maupun {@code false} berperilaku identik.
	 * Argumen {@code null} langsung diabaikan.</p>
	 *
	 * @param diskusiPengumumanAkademis id komentar yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang                TIDAK BERPENGARUH — parameter mati
	 */
	public void populateDiskusiPengumumanAkademis(Long diskusiPengumumanAkademis, boolean tulisUlang) {
		try {
			if (diskusiPengumumanAkademis == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiDiskusiPengumumanAkademis());
			c.put(diskusiPengumumanAkademis.toString(), diskusiPengumumanAkademis.toString());
			tulisLokasiDiskusiPengumumanAkademis(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:772");
		}
	}

	/**
	 * Memeriksa apakah pengumuman ini punya komentar.
	 *
	 * <p>Implementasinya memanggil
	 * {@link #ambilDiskusiPengumumanAkademisTotal(boolean, Long, boolean)} dengan
	 * {@code semua = true}, jadi biayanya <b>bukan</b> sekadar {@code count}: seluruh id komentar
	 * dimuat (dan bila perlu indeks dibangun ulang lebih dulu) hanya untuk mengetahui apakah
	 * jumlahnya lebih dari nol.</p>
	 *
	 * @return {@code true} bila ada minimal satu komentar berisi
	 */
	public boolean punyaDiskusi() {
		TreeSet<Long> diskusiPengumumanAkademissa = ambilDiskusiPengumumanAkademisTotal(false, null, true);
		int ada = diskusiPengumumanAkademissa.size();
		diskusiPengumumanAkademissa = null;
		return ada > 0;
	}

	/**
	 * Menghitung seluruh komentar pengumuman ini tanpa penyaringan penulis.
	 *
	 * @return jumlah komentar
	 * @see #ambilJumlahDiskusiPengumumanAkademis(Mahasiswa, Dosen)
	 */
	public int ambilJumlahDiskusiPengumumanAkademis() {
		return ambilJumlahDiskusiPengumumanAkademis(null, null);
	}

	/**
	 * Menghitung komentar pengumuman ini, opsional hanya milik seorang mahasiswa atau dosen.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Bila penanda {@code udah("diskusi")} belum terpasang, indeks berkas dibangun ulang
	 *   lebih dulu lewat {@link #reInitDiskusiPengumumanAkademis(Session)}. Ingat semantik
	 *   {@code udah()} yang bergaya <i>test-and-set</i>: pemanggilan pertama mengembalikan
	 *   {@code false} SEKALIGUS memasang penandanya.</li>
	 *   <li>Indeks JSON ditelusuri. Entri bernilai kosong (komentar terhapus) dilewati.</li>
	 *   <li>Bila {@code mahasiswa} atau {@code dosen} diberikan, tiap id di-<i>resolve</i> lewat
	 *   cache {@code ambilData(DiskusiPengumumanAkademis.class, key)} lalu penulisnya dibandingkan.
	 *   Bila TIDAK diberikan keduanya, jumlah dinaikkan tanpa membaca komentarnya sama sekali —
	 *   jalur cepat.</li>
	 *   <li>Id yang tidak ada di cache dikumpulkan ke {@code idsBelumAda}, lalu diambil sekaligus
	 *   dari database dalam satu query {@code in(...)}, dimasukkan ke cache
	 *   ({@code masukkanData}), dan ikut dihitung.</li>
	 * </ol>
	 *
	 * <h4>Efek samping</h4>
	 * <p>Selain menulis berkas indeks (lewat pembangunan ulang) dan mengisi cache entity, tiap
	 * komentar yang tersentuh di-{@code setPengumumanAkademis(this)} — object komentar di cache
	 * ikut diubah agar relasi baliknya menunjuk instance ini.</p>
	 *
	 * <h4>Kuirk</h4>
	 * <p>Pada jalur "id belum ada di cache", kenaikan jumlah HANYA terjadi bila
	 * {@code mahasiswa} atau {@code dosen} diberikan — cabang tanpa penyaring tidak punya
	 * {@code else}. Artinya komentar yang belum masuk cache <b>tidak ikut terhitung</b> pada
	 * pemanggilan {@link #ambilJumlahDiskusiPengumumanAkademis()} tanpa argumen, sehingga
	 * angkanya bisa lebih kecil dari kenyataan sampai cache terisi. Perhatikan juga seluruh
	 * kegagalan parsing ditelan blok catch kosong berlapis.</p>
	 *
	 * @param mahasiswa bila tidak {@code null}, hanya komentar milik mahasiswa ini yang dihitung
	 * @param dosen     bila tidak {@code null} (dan {@code mahasiswa} {@code null}), hanya
	 *                  komentar milik dosen ini yang dihitung
	 * @return jumlah komentar yang cocok
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahDiskusiPengumumanAkademis(Mahasiswa mahasiswa, Dosen dosen) {

		if (!udah("diskusi")) {
			reInitDiskusiPengumumanAkademis(HibernateUtil.currentSession());
		}

		int jumlah = 0;
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiDiskusiPengumumanAkademis());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						if (mahasiswa != null) {
							GeneralValueObject generalValueObject = ambilData(DiskusiPengumumanAkademis.class, key);
							if (generalValueObject != null) {
								DiskusiPengumumanAkademis diskusiPengumumanAkademis = (DiskusiPengumumanAkademis) generalValueObject;
								diskusiPengumumanAkademis.setPengumumanAkademis(this);
								if (diskusiPengumumanAkademis.getMahasiswa() != null
										&& diskusiPengumumanAkademis.getMahasiswa().getId().equals(mahasiswa.getId())) {
									jumlah++;
								}
							} else {
								idsBelumAda.add(Long.parseLong(key));

							}
						} else if (dosen != null) {
							GeneralValueObject generalValueObject = ambilData(DiskusiPengumumanAkademis.class, key);
							if (generalValueObject != null) {
								DiskusiPengumumanAkademis diskusiPengumumanAkademis = (DiskusiPengumumanAkademis) generalValueObject;
								diskusiPengumumanAkademis.setPengumumanAkademis(this);
								if (diskusiPengumumanAkademis.getDosen() != null
										&& diskusiPengumumanAkademis.getDosen().getId().equals(dosen.getId())) {
									jumlah++;
								}
							} else {
								idsBelumAda.add(Long.parseLong(key));
							}
						} else {
							jumlah++;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:834");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:838");

		}

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda DiskusiPengumumanAkademis -> " +
			// idsBelumAda);
			List<DiskusiPengumumanAkademis> diskusiPengumumanAkademiss = HibernateUtil.currentSession()
					.createCriteria(DiskusiPengumumanAkademis.class).add(Restrictions.in("id", idsBelumAda)).list();
			for (DiskusiPengumumanAkademis diskusiPengumumanAkademis : diskusiPengumumanAkademiss) {
				masukkanData(DiskusiPengumumanAkademis.class, diskusiPengumumanAkademis);
				diskusiPengumumanAkademis.setPengumumanAkademis(this);

				if (mahasiswa != null) {
					if (diskusiPengumumanAkademis.getMahasiswa() != null
							&& diskusiPengumumanAkademis.getMahasiswa().getId().equals(mahasiswa.getId())) {
						jumlah++;
					}
				} else if (dosen != null) {
					if (diskusiPengumumanAkademis.getDosen() != null
							&& diskusiPengumumanAkademis.getDosen().getId().equals(dosen.getId())) {
						jumlah++;
					}
				}
			}
		}

		return jumlah;
	}

	/**
	 * Mengumpulkan id seluruh komentar pengumuman ini, terurut dan tersaring menurut induk balasan.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Membangun ulang indeks berkas bila penanda {@code udah("diskusi")} belum ada.</li>
	 *   <li>Mengumpulkan kunci indeks yang nilainya tidak kosong (yang kosong = komentar
	 *   terhapus).</li>
	 *   <li>Memuat komentar secara borongan lewat
	 *   {@code ambilDataBanyak(DiskusiPengumumanAkademis.class, ids)} — sekali jalan, bukan satu
	 *   query per id.</li>
	 *   <li>Komentar dengan {@code catatan} kosong dibuang; sisanya di-{@code setPengumumanAkademis(this)}
	 *   lalu disaring menurut {@code parent}.</li>
	 * </ol>
	 *
	 * <h4>Arti parameter penyaring</h4>
	 * <ul>
	 *   <li>{@code semua = true} → semua komentar diambil, {@code parent} diabaikan.</li>
	 *   <li>{@code semua = false} dan {@code parent = null} → hanya komentar tingkat atas (yang
	 *   tidak membalas komentar lain).</li>
	 *   <li>{@code semua = false} dan {@code parent} terisi → hanya balasan langsung komentar
	 *   ber-id tersebut.</li>
	 * </ul>
	 *
	 * @param urutkan {@code true} untuk urutan id menaik (komentar terlama dulu); {@code false}
	 *                untuk menurun (terbaru dulu) — inilah default tampilan papan
	 * @param parent  id komentar induk; bermakna hanya bila {@code semua} {@code false}
	 * @param semua   {@code true} untuk mengabaikan penyaringan induk
	 * @return himpunan terurut id komentar; kosong bila tidak ada yang cocok
	 */
	@SuppressWarnings("unchecked")
	public TreeSet<Long> ambilDiskusiPengumumanAkademisTotal(boolean urutkan, Long parent, boolean semua) {

		if (!udah("diskusi")) {
			reInitDiskusiPengumumanAkademis(HibernateUtil.currentSession());
		}

		List<String> idsBelumAda = new ArrayList<String>();
		try {
			JSONObject c = new JSONObject(ambilLokasiDiskusiPengumumanAkademis());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						idsBelumAda.add(key);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:886");

				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/PengumumanAkademis.java:891");
		}

		TreeSet<Long> diskusiPengumumanAkademissa = urutkan ? new TreeSet<Long>()
				: new TreeSet<Long>(Collections.reverseOrder());
		List<DiskusiPengumumanAkademis> diskusiPengumumanAkademiss = ambilDataBanyak(DiskusiPengumumanAkademis.class,
				idsBelumAda);
		for (DiskusiPengumumanAkademis diskusiPengumumanAkademis : diskusiPengumumanAkademiss) {
			if (diskusiPengumumanAkademis != null && !diskusiPengumumanAkademis.getCatatan().isEmpty()) {
				diskusiPengumumanAkademis.setPengumumanAkademis(this);
				if (semua || (parent == null && diskusiPengumumanAkademis.getParent() == null)
						|| (parent != null && diskusiPengumumanAkademis.getParent() != null
								&& diskusiPengumumanAkademis.getParent().getId() != null
								&& diskusiPengumumanAkademis.getParent().getId().equals(parent))) {
					diskusiPengumumanAkademissa.add(diskusiPengumumanAkademis.getId());
				}
			}
		}
		diskusiPengumumanAkademiss = null;
		return diskusiPengumumanAkademissa;
	}

	/**
	 * Mengambil satu halaman id komentar dari himpunan yang sudah dikumpulkan sebelumnya.
	 *
	 * <p>Dipakai untuk paginasi tampilan diskusi: pemanggil lebih dulu memperoleh himpunan lengkap
	 * lewat {@link #ambilDiskusiPengumumanAkademisTotal(boolean, Long, boolean)}, lalu meminta
	 * potongan {@code [mulai, mulai + banyak)} dari komentar yang membalas {@code parent}
	 * tertentu.</p>
	 *
	 * <p><b>Catat baik-baik kondisi penyaringnya</b>: ekspresi
	 * {@code a != null && (parent == null && ...) || (parent != null && ...)} mengikuti presedensi
	 * Java {@code (A && B) || C}, sehingga cabang kedua TIDAK ikut memeriksa {@code
	 * diskusiPengumumanAkademis != null}. Dalam praktiknya aman karena akses ke {@code getParent()}
	 * pada object null akan melempar {@code NullPointerException} yang langsung ditelan blok catch
	 * per-iterasi, tetapi bentuk ekspresinya memang tidak seperti yang dimaksudkan.</p>
	 *
	 * <p>Nomor urut ({@code index}) hanya dinaikkan untuk komentar yang lolos saring, jadi
	 * paginasinya relatif terhadap hasil saring, bukan terhadap himpunan masukan.</p>
	 *
	 * @param parent                       komentar induk; {@code null} berarti komentar tingkat atas
	 * @param diskusiPengumumanAkademissa  himpunan id komentar hasil
	 *                                     {@link #ambilDiskusiPengumumanAkademisTotal(boolean, Long, boolean)}
	 * @param mulai                        indeks awal (berbasis 0) potongan yang diminta
	 * @param banyak                       jumlah maksimum id yang dikembalikan
	 * @return daftar id komentar pada potongan yang diminta, dalam urutan himpunan masukan
	 */
	public List<Long> ambilDiskusiPengumumanAkademis(DiskusiPengumumanAkademis parent,
			TreeSet<Long> diskusiPengumumanAkademissa, int mulai, int banyak) {

		int index = 0;
		List<Long> diskusiPengumumanAkademiss = new ArrayList<Long>();
		for (Long diskusiPengumumanAkademisId : diskusiPengumumanAkademissa) {
			try {
				DiskusiPengumumanAkademis diskusiPengumumanAkademis = (DiskusiPengumumanAkademis) GeneralValueObject
						.ambilData(DiskusiPengumumanAkademis.class, diskusiPengumumanAkademisId.toString());
				if (diskusiPengumumanAkademis != null
						&& (parent == null && diskusiPengumumanAkademis.getParent() == null)
						|| (parent != null && diskusiPengumumanAkademis.getParent() != null
								&& parent.getId().equals(diskusiPengumumanAkademis.getParent().getId()))) {
					if (index >= mulai && index < (mulai + banyak)) {
						diskusiPengumumanAkademiss.add(diskusiPengumumanAkademis.getId());
					}
					index++;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:931");
				// TODO: handle exception
			}
		}
		return diskusiPengumumanAkademiss;
	}

	/**
	 * Mengembalikan perguruan tinggi pemilik pengumuman.
	 *
	 * <p><b>Getter yang menulis:</b> setelah {@code check(...)}, bila masih {@code null} field
	 * diisi PT default instalasi lewat
	 * {@code PerguruanTinggiUtil.getPerguruanTinggi()}. Konsekuensinya pengumuman yang sengaja
	 * dibuat lintas-PT (kolom NULL = tampil di semua PT) bisa <b>terikat permanen</b> ke satu PT
	 * begitu dibaca lalu di-flush — perilaku yang perlu diwaspadai pada instalasi multi-PT.</p>
	 *
	 * <p>Kegagalan pengambilan PT default ditelan blok catch (dicatat ke audit), sehingga hasilnya
	 * tetap bisa {@code null}.</p>
	 *
	 * @return perguruan tinggi pemilik, atau {@code null} bila PT default pun tak bisa ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengumumanAkademis.java:946");
		}
		return perguruanTinggi;
	}

	/**
	 * Menyetel perguruan tinggi pemilik pengumuman.
	 *
	 * @param perguruanTinggi PT pemilik; {@code null} berarti berlaku lintas-PT (tetapi lihat
	 *                        peringatan pada {@link #getPerguruanTinggi()})
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * Menyatakan apakah daftar pengumuman lain ikut ditampilkan di bawah pengumuman ini.
	 *
	 * @return {@code true} bila daftar pengumuman lain ditampilkan; default {@code false}
	 */
	public Boolean getTampilkanPengumumanLain() {
		return tampilkanPengumumanLain == null ? false : tampilkanPengumumanLain;
	}

	/**
	 * Menyetel penampilan daftar pengumuman lain.
	 *
	 * @param tampilkanPengumumanLain {@code true} untuk menampilkan
	 */
	public void setTampilkanPengumumanLain(Boolean tampilkanPengumumanLain) {
		this.tampilkanPengumumanLain = tampilkanPengumumanLain;
	}

	/**
	 * Menyatakan apakah galeri pengumuman ini berupa HTML, bukan daftar gambar.
	 *
	 * @return {@code true} bila galeri dirender sebagai HTML; default {@code false}
	 */
	public Boolean getGaleryBerupaHtml() {
		return galeryBerupaHtml == null ? false : galeryBerupaHtml;
	}

	/**
	 * Menyetel mode galeri HTML.
	 *
	 * @param galeryBerupaHtml {@code true} untuk merender galeri sebagai HTML
	 */
	public void setGaleryBerupaHtml(Boolean galeryBerupaHtml) {
		this.galeryBerupaHtml = galeryBerupaHtml;
	}

	/**
	 * Mengembalikan potongan CSS tinggi galeri untuk tampilan desktop.
	 *
	 * <p>Isinya <b>disisipkan mentah</b> ke dalam atribut {@code style} elemen {@code <img>} /
	 * {@code <div class="slideshow-container">} oleh perender, jadi nilainya harus berupa
	 * deklarasi CSS lengkap (mis. {@code "height:300px;"}), bukan sekadar angka. String kosong
	 * berarti "biarkan default".</p>
	 *
	 * @return potongan CSS; string kosong bila tidak diatur (tidak pernah {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getTinggiGaleri() {
		return tinggiGaleri == null ? "" : tinggiGaleri;
	}

	/**
	 * Menyetel potongan CSS tinggi galeri untuk desktop.
	 *
	 * @param tinggiGaleri deklarasi CSS, mis. {@code "height:300px;"}
	 */
	public void setTinggiGaleri(String tinggiGaleri) {
		this.tinggiGaleri = tinggiGaleri;
	}

	/**
	 * Mengembalikan potongan CSS tinggi galeri untuk tampilan mobile.
	 *
	 * <p>Perender memilih nilai ini menggantikan {@link #getTinggiGaleri()} ketika
	 * {@code Common.isMobile()} bernilai true.</p>
	 *
	 * @return potongan CSS; string kosong bila tidak diatur (tidak pernah {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getTinggiGaleriMobile() {
		return tinggiGaleriMobile == null ? "" : tinggiGaleriMobile;
	}

	/**
	 * Menyetel potongan CSS tinggi galeri untuk mobile.
	 *
	 * @param tinggiGaleriMobile deklarasi CSS untuk tampilan mobile
	 */
	public void setTinggiGaleriMobile(String tinggiGaleriMobile) {
		this.tinggiGaleriMobile = tinggiGaleriMobile;
	}

	/**
	 * Menyatakan apakah pengumuman ini berperan sebagai halaman "Profil".
	 *
	 * <p>Dipakai {@code ProfileAction}/{@code MainAction2} sebagai penyaring: pengumuman yang
	 * flag-nya {@code false} tidak akan dipilih untuk mengisi halaman profil publik.</p>
	 *
	 * @return {@code true} bila pengumuman boleh tampil sebagai halaman profil; default
	 *         {@code false}
	 */
	public Boolean getTampilkanProfile() {
		return tampilkanProfile == null ? false : tampilkanProfile;
	}

	/**
	 * Menyetel peran halaman profil.
	 *
	 * @param tampilkanProfile {@code true} bila pengumuman ini menjadi halaman profil
	 */
	public void setTampilkanProfile(Boolean tampilkanProfile) {
		this.tampilkanProfile = tampilkanProfile;
	}

	/**
	 * Mengembalikan judul versi bahasa Inggris.
	 *
	 * <p><b>Getter yang menulis:</b> bila kolom {@code judul_en} kosong, field diisi hasil
	 * {@link #getJudul()} (versi Indonesia yang sudah difilter) dan nilai itu bertahan di object —
	 * sehingga dapat ikut TERSIMPAN ke kolom {@code judul_en} saat entity di-flush. Dampaknya
	 * kolom bahasa Inggris pelan-pelan terisi teks Indonesia, dan sejak saat itu perubahan judul
	 * Indonesia tidak lagi tercermin di versi Inggris.</p>
	 *
	 * @return judul bahasa Inggris; jatuh kembali ke judul Indonesia bila belum diterjemahkan
	 */
	@Column(name = "judul_en", nullable = true, columnDefinition = "text")
	public String getJudulEn() {
		if (judulEn == null || judulEn.trim().isEmpty()) {
			judulEn = getJudul();
		}
		return judulEn;
	}

	/**
	 * Menyetel judul versi bahasa Inggris.
	 *
	 * @param judulEn judul terjemahan; kosong berarti ikut versi Indonesia
	 */
	public void setJudulEn(String judulEn) {
		this.judulEn = judulEn;
	}

	/**
	 * Mengembalikan isi pengumuman versi bahasa Inggris.
	 *
	 * <p><b>Getter yang menulis</b> — berlaku catatan yang sama persis dengan
	 * {@link #getJudulEn()}: bila kosong, isi Indonesia disalin ke field {@code catatanEn} dan
	 * berpotensi ikut tersimpan ke database.</p>
	 *
	 * @return isi bahasa Inggris; jatuh kembali ke isi Indonesia bila belum diterjemahkan
	 */
	@Column(name = "catatan_en", nullable = true, columnDefinition = "text")
	public String getCatatanEn() {
		if (catatanEn == null || catatanEn.trim().isEmpty()) {
			catatanEn = getCatatan();
		}
		return catatanEn;
	}

	/**
	 * Menyetel isi pengumuman versi bahasa Inggris.
	 *
	 * @param catatanEn isi terjemahan; kosong berarti ikut versi Indonesia
	 */
	public void setCatatanEn(String catatanEn) {
		this.catatanEn = catatanEn;
	}

	/**
	 * Mengembalikan pengumuman induk (relasi mandiri/self-reference).
	 *
	 * <p>Pengumuman yang punya induk <b>tidak dirender sebagai baris pengumuman tersendiri</b>,
	 * melainkan sebagai TOMBOL di dalam tampilan induknya —
	 * {@code PengumumanAkademisAction.tampilPengumuman} menelusuri seluruh pengumuman di
	 * {@code ConstantValues}, mengelompokkan anak menurut {@link #getPosisiTombol()} ({@code atas}
	 * / {@code bawah}), mengurutkannya dengan {@code compareTo} (yaitu menurut
	 * {@link #getNomorUrut()}), lalu membuat toolbar button berlabel {@link #getLabelTombol()}
	 * berikut ikon dari lampiran ber-jenis {@code Icon Pengumuman}.</p>
	 *
	 * <p>Struktur ini efektif hanya satu tingkat: perender tidak menelusuri cucu. Tidak ada
	 * penjagaan terhadap induk melingkar.</p>
	 *
	 * @return pengumuman induk, atau {@code null} bila pengumuman ini berdiri sendiri
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "induk", nullable = true)
	public PengumumanAkademis getInduk() {
		induk = check(induk);
		return induk;
	}

	/**
	 * Menyetel pengumuman induk.
	 *
	 * @param induk pengumuman induk; {@code null} membuat pengumuman ini berdiri sendiri
	 */
	public void setInduk(PengumumanAkademis induk) {
		this.induk = induk;
	}

	/**
	 * Mengembalikan posisi tombol pengumuman ini di dalam tampilan induknya.
	 *
	 * <p>Nilai kosong/{@code null} dipulangkan sebagai {@link #ATAS} (tanpa ditulis balik ke
	 * field). Perender hanya mengenali {@link #ATAS} dan {@link #BAWAH}; konstanta {@link #KANAN}
	 * dan {@link #KIRI} tersedia tetapi tidak diperiksa di jalur tampil pengumuman, sehingga
	 * pengumuman anak bernilai "kanan"/"kiri" tidak akan muncul sebagai tombol di mana pun.</p>
	 *
	 * @return {@link #ATAS} atau {@link #BAWAH} (atau nilai lain yang tersimpan); default
	 *         {@link #ATAS}
	 */
	public String getPosisiTombol() {
		return posisiTombol == null || posisiTombol.trim().isEmpty() ? ATAS : posisiTombol;
	}

	/**
	 * Menyetel posisi tombol pengumuman anak.
	 *
	 * @param posisiTombol {@link #ATAS} atau {@link #BAWAH}
	 */
	public void setPosisiTombol(String posisiTombol) {
		this.posisiTombol = posisiTombol;
	}

	/**
	 * Mengembalikan teks tombol pengumuman anak apa adanya.
	 *
	 * <p>Tidak dinormalkan: nilainya bisa {@code null}, dan tombol dengan label {@code null} akan
	 * tampil tanpa teks (hanya ikon, atau kosong sama sekali bila ikon juga tidak ada).</p>
	 *
	 * @return teks tombol, bisa {@code null}
	 */
	public String getLabelTombol() {
		return labelTombol;
	}

	/**
	 * Menyetel teks tombol pengumuman anak.
	 *
	 * @param labelTombol teks tombol
	 */
	public void setLabelTombol(String labelTombol) {
		this.labelTombol = labelTombol;
	}

	/**
	 * Mengembalikan nomor urut manual pengumuman.
	 *
	 * <p>{@code null} dipulangkan sebagai {@code 0}. Nilai ini dipakai {@code compareTo} milik
	 * {@link GeneralValueObject} — jadi ia menentukan urutan setiap kali sekumpulan pengumuman
	 * disortir di memori (mis. daftar tombol anak). Urutan daftar utama Papan Pengumuman sendiri
	 * ditentukan query: {@code kategoriPengumuman.nomorUrut} menaik, lalu {@code tanggal}
	 * menurun.</p>
	 *
	 * @return nomor urut; {@code 0} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut manual pengumuman.
	 *
	 * @param nomorUrut nomor urut; makin kecil makin awal
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Menyatakan apakah pengumuman langsung dibuka sebagai tab tersendiri saat portal dimuat.
	 *
	 * @return {@code true} bila pengumuman langsung muncul di tab; default {@code false}
	 */
	public Boolean getLangsungMunculDiTab() {
		return langsungMunculDiTab == null ? false : langsungMunculDiTab;
	}

	/**
	 * Menyetel perilaku "langsung muncul di tab".
	 *
	 * @param langsungMunculDiTab {@code true} untuk membuka pengumuman sebagai tab
	 */
	public void setLangsungMunculDiTab(Boolean langsungMunculDiTab) {
		this.langsungMunculDiTab = langsungMunculDiTab;
	}

	/**
	 * Menyatakan apakah isi pengumuman langsung tampil penuh di Beranda.
	 *
	 * <p>Bila {@code false} (default), pembaca hanya melihat judul dan harus mengkliknya untuk
	 * membuka isi.</p>
	 *
	 * @return {@code true} bila isi langsung terbentang di halaman utama; default {@code false}
	 */
	@Column(name = "langsung_tampil_beranda")
	public Boolean getLangsungTampilBeranda() {
		return langsungTampilBeranda == null ? false : langsungTampilBeranda;
	}

	/**
	 * Menyetel perilaku "langsung tampil di Beranda".
	 *
	 * @param langsungTampilBeranda {@code true} agar isi terbentang tanpa perlu diklik
	 */
	public void setLangsungTampilBeranda(Boolean langsungTampilBeranda) {
		this.langsungTampilBeranda = langsungTampilBeranda;
	}

	/**
	 * Mengembalikan nama kelas Java (FQCN) komponen ZK yang disisipkan ke badan pengumuman.
	 *
	 * <p><b>Untuk apa.</b> Mekanisme "papan lepas": alih-alih menulis HTML, pengelola dapat
	 * menempelkan satu komponen ZK penuh (dasbor, portal layout, dsb.) ke dalam pengumuman.
	 * {@code PengumumanAkademisAction} melakukan
	 * {@code Class.forName(getKlassData()).newInstance()}, lalu — hanya bila hasilnya berupa
	 * {@code Window} atau {@code MyPortallayout} — mengatur lebar/tinggi komponen dan
	 * menempelkannya ke baris tampilan.</p>
	 *
	 * <p><b>Nilai kembalian sengaja {@code null}-able:</b> string kosong dinormalkan menjadi
	 * {@code null} agar pemanggil cukup memeriksa {@code != null} sebelum melakukan refleksi.
	 * Normalisasi ini tidak ditulis balik ke field.</p>
	 *
	 * <p><b>Catatan pemeliharaan.</b> Karena isi kolom ini menjadi nama kelas yang di-instansiasi
	 * lewat refleksi, salah ketik hanya terlihat sebagai halaman kosong (exception ditangani di
	 * pemanggil), dan siapa pun yang bisa menyunting pengumuman dapat menentukan kelas apa yang
	 * di-instansiasi di server. Batasi hak sunting pengumuman seketat hak konfigurasi sistem.</p>
	 *
	 * @return FQCN komponen ZK, atau {@code null} bila pengumuman tidak menyisipkan komponen
	 */
	@Column(name = "klass_data", nullable = true, columnDefinition = "text")
	public String getKlassData() {
		return klassData == null || klassData.trim().isEmpty() ? null : klassData.trim();
	}

	/**
	 * Menyetel nama kelas komponen ZK yang disisipkan.
	 *
	 * @param klassData FQCN kelas ZK; kosong berarti tidak ada komponen sisipan
	 * @see #getKlassData()
	 */
	public void setKlassData(String klassData) {
		this.klassData = klassData;
	}
}
