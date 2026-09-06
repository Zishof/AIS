package ais.database.model;

// Generated Apr 5, 2010 1:13:29 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Sesi interview (wawancara) massal calon mahasiswa — <b>entity induk penjadwalan</b> pada modul
 * PMB. Satu baris mewakili satu "ruang" atau gelombang wawancara: siapa pewawancaranya, kapan
 * dimulai dan berakhir, berapa kapasitasnya, serta lewat kanal apa pertemuannya digelar bila daring.
 * Tabel: {@code public.interview_calon_mahasiswa}.
 *
 * <h3>Hubungan dengan peserta</h3>
 * <p>Peserta individual dicatat di {@link InterviewPunyaCalonMahasiswa}, yang menunjuk balik ke
 * entity ini. Hubungannya bersifat <i>jadwal induk dengan override per peserta</i>:
 * {@code InterviewPunyaCalonMahasiswa.getMulai()} dan {@code getSampai()} memakai jadwal peserta
 * bila diisi, dan <b>jatuh kembali ke {@link #getMulai()}/{@link #getSampai()} milik sesi ini</b>
 * bila tidak.</p>
 * <p><b>Catatan penting soal fallback itu:</b> {@link #getMulai()} dan {@link #getSampai()} di kelas
 * ini <i>tidak pernah</i> mengembalikan {@code null} — bila kolomnya kosong keduanya mengembalikan
 * {@link WaktuUtil#getDate()}, yaitu <i>waktu saat method dipanggil</i>. Jadi sesi yang jadwalnya
 * belum diisi tidak tampak sebagai "belum dijadwalkan", melainkan sebagai jendela selebar nol yang
 * selalu bergeser mengikuti jam sekarang, dan nilainya berubah setiap kali dibaca. Kode yang
 * membandingkan {@code getMulai()} dengan {@code getSampai()} pada sesi seperti itu akan mendapat
 * hasil yang tidak stabil.</p>
 *
 * <h3>Kanal pertemuan daring</h3>
 * <p>{@link #getOnlineMenggunakan()} memilih satu dari delapan mode
 * ({@link #TIDAK_AKTIF}, {@link #JITSI}, {@link #GOOGLE_MEET}, {@link #ZOOM}, {@link #BBB},
 * {@link #SKYPE}, {@link #WA}, {@link #LAIN}). Tiap mode punya sumber tautan sendiri —
 * {@link #getZoomLink()}, {@link #getBbbLink()}, {@link #getSkypeLink()}, {@link #getWaLink()},
 * {@link #getLainLink()}, tautan Google Calendar dari cache {@code hangoutLink}, atau tautan Jitsi
 * yang dibangkitkan on-the-fly oleh {@link #generateJitsiLink()}. Tombol pemicunya dirakit oleh
 * {@link #createVideoConrefrence(InterviewCalonMahasiswa, Component, boolean, boolean,
 * EventListener)}.</p>
 *
 * <h3>Pola arsitektur yang perlu diwaspadai di kelas ini</h3>
 * <ul>
 *   <li><b>Getter yang mengubah state.</b> {@link #getOnlineMenggunakan()},
 *       {@link #getGelombangPendaftaranLain()}, kelima getter tautan, dan {@link #getFakultas()}
 *       semuanya <i>menulis balik</i> ke field-nya. Pada objek yang dikelola Hibernate, sekadar
 *       membaca properti tersebut dapat menandai entity kotor sehingga memicu {@code UPDATE} dan
 *       revisi Envers palsu. Ini instance dari pola getter-mutasi-field yang tersebar luas pada
 *       model AIS.</li>
 *   <li><b>Setter satu arah.</b> {@link #setOleh(String)} dan {@link #setOlehId(String)} mengabaikan
 *       argumen {@code null}/kosong secara senyap, sehingga jejak pelaku yang sudah tercatat tidak
 *       bisa dikosongkan lewat jalur normal.</li>
 *   <li><b>Konstanta mode yang tidak {@code final}.</b> Lihat catatan pada {@link #TIDAK_AKTIF}.</li>
 *   <li><b>Tautan disisipkan ke JavaScript tanpa escaping.</b> Lihat catatan pada
 *       {@link #createVideoConrefrence(InterviewCalonMahasiswa, Component, boolean, boolean,
 *       EventListener)}.</li>
 *   <li><b>Tanpa penyaring tenant/kepemilikan.</b> Entity ini tidak punya kolom satuan kerja; ruang
 *       lingkupnya hanya bisa dipersempit lewat {@link #getJurusan()}/{@link #getFakultas()} dan
 *       {@link #getGelombangPendaftaranLain()}, dan itu pun tidak ditegakkan di lapisan model.</li>
 * </ul>
 *
 * <p>Padanan kelas ini untuk jenjang sekolah adalah {@code ais.database.model.sekolah.InterviewCalonSiswa}.</p>
 *
 * @see InterviewPunyaCalonMahasiswa
 * @see GelombangPendaftaran
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "interview_calon_mahasiswa")
public class InterviewCalonMahasiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entity ini.
	 */
	private static final long serialVersionUID = -7550466125892447098L;

	/** Kunci utama sesi interview (kolom {@code id}, IDENTITY). */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini — bagian jejak audit ringan yang ditempelkan
	 * pada entity, terpisah dari revisi Envers.
	 */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini, pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah tercatat
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mencatat id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Setter satu arah:</b> argumen {@code null} maupun string kosong diabaikan secara senyap,
	 * sehingga id pelaku yang sudah tercatat <b>tidak dapat dikosongkan</b> lewat setter ini.
	 * Perilaku itu memang melindungi jejak audit dari penimpaan yang tidak disengaja, tetapi juga
	 * berarti pemanggil tidak bisa membedakan penolakan dari keberhasilan.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mencatat nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, ini <b>setter satu arah</b>: nilai {@code null}
	 * atau kosong diabaikan senyap dan tidak menghapus nama yang sudah tersimpan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah tercatat
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait lifecycle JPA yang dijalankan tepat sebelum {@code UPDATE}; mendelegasikan pembaruan
	 * stempel waktu ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Perhatikan bahwa deklarasi field {@code tanggal_dirubah} ditulis pada baris yang sama
	 * dengan method ini (hasil penyuntingan otomatis), sehingga mudah terlewat saat membaca sekilas.
	 * Nilai awalnya adalah waktu objek dibuat di memori, bukan {@code null}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir secara manual.
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa oleh {@link #onUpdate()} pada operasi
	 * {@code UPDATE} berikutnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu; untuk objek baru berisi waktu objek dibuat di memori, bukan
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat sesi interview dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@link #nama} mentah (bukan lewat {@link #getNama()}), sehingga aman dipanggil
	 * pada objek yang belum sepenuhnya terinisialisasi dan tidak ikut memicu {@code trim()}.</p>
	 *
	 * @return gabungan id dan nama sesi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama sesi/ruang interview (kolom {@code nama}, wajib, maks. 150 karakter). */
	private String nama;

	/**
	 * Tahun akademik penyelenggaraan sesi. Bila kosong, {@link #getTahunAkademik()} mengganti dengan
	 * tahun akademik berjalan dari konfigurasi.
	 */
	private String tahunAkademik;

	/**
	 * Muatan JSON hasil sinkronisasi ke Google Calendar (kolom {@code text}). Di dalamnya antara
	 * lain tersimpan {@code hangoutLink} yang dipakai mode {@link #GOOGLE_MEET}.
	 */
	private String calendarEvent;

	/** Tautan rapat Zoom untuk mode {@link #ZOOM}. */
	private String zoomLink;

	/** Tautan rapat Big Blue Button untuk mode {@link #BBB}. */
	private String bbbLink;

	/**
	 * Kanal pertemuan daring yang dipakai sesi ini, salah satu dari konstanta
	 * {@link #TIDAK_AKTIF} sampai {@link #LAIN}.
	 */
	private Integer onlineMenggunakan;

	/** Tautan panggilan Skype untuk mode {@link #SKYPE}. */
	private String skypeLink;

	/** Tautan grup WhatsApp untuk mode {@link #WA}. */
	private String waLink;

	/** Tautan bebas untuk mode {@link #LAIN}, dipakai bila kanalnya di luar daftar baku. */
	private String lainLink;

	/**
	 * Pegawai yang bertugas sebagai pewawancara pada sesi ini (kolom {@code pegawai}, wajib).
	 */
	private Pegawai pegawai;

	/**
	 * Waktu mulai sesi. Perhatikan bahwa {@link #getMulai()} mengganti nilai {@code null} dengan
	 * waktu sekarang, sehingga kolom kosong tidak terlihat kosong dari luar.
	 */
	private Date mulai;

	/**
	 * Waktu berakhir sesi, dengan perilaku pengganti {@code null} yang sama seperti {@link #mulai}.
	 */
	private Date sampai;

	/**
	 * Penanda sesi masih berlaku. Bernilai baku {@code true} bila kolomnya {@code null} — lihat
	 * {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Penanda bahwa peserta sesi ini wajib mengikuti ujian, baku {@code false} bila kolomnya
	 * {@code null}.
	 */
	private Boolean hrsUjian;

	/** Catatan bebas mengenai sesi (kolom {@code text}). */
	private String keterangan;

	/** Daya tampung peserta sesi; baku {@code 3000} bila kolomnya {@code null}. */
	private Integer kapasitasRuangan;

	/**
	 * Daftar id {@link GelombangPendaftaran} yang boleh mengikuti sesi ini, disimpan sebagai
	 * <b>string id dipisah koma</b> dan bukan sebagai relasi tabel penghubung.
	 *
	 * @see #ambilGelombangPendaftaran()
	 */
	private String gelombangPendaftaranLain;

	/** Jurusan yang menaungi sesi ini (opsional). */
	private Jurusan jurusan;

	/**
	 * Fakultas yang menaungi sesi ini (opsional). Nilainya diturunkan dari {@link #jurusan} bila
	 * jurusan terisi — lihat {@link #getFakultas()}.
	 */
	private Fakultas fakultas;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate. Seluruh field dibiarkan pada nilai
	 * awalnya; nilai baku baru muncul saat getter masing-masing dipanggil.
	 */
	public InterviewCalonMahasiswa() {
	}

	/**
	 * Kunci utama sesi interview.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan ditandai {@code insertable = false}, jadi
	 * nilai yang disetel manual lewat {@link #setId(Long)} tidak akan ikut pada {@code INSERT}.</p>
	 *
	 * @return id sesi, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama sesi. Umumnya hanya dipakai Hibernate atau kode yang menyusun objek
	 * detached; jangan dipakai untuk memindahkan identitas baris.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama sesi/ruang interview tanpa spasi di kedua ujungnya.
	 *
	 * <p>Nilai ini ikut membentuk nama ruang Jitsi pada {@link #generateJitsiLink()}, sehingga
	 * mengubah nama sesi akan <b>mengubah tautan ruang daringnya</b> untuk mode {@link #JITSI}.</p>
	 *
	 * @return nama sesi yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama sesi/ruang interview. Nilai disimpan apa adanya (trim baru terjadi saat
	 * dibaca) dan tidak divalidasi terhadap batas 150 karakter di lapisan model.
	 *
	 * @param nama nama sesi
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan tahun akademik sesi, dengan <b>tahun akademik berjalan sebagai pengganti</b>
	 * bila kolomnya belum diisi.
	 *
	 * <p>Penggantian ini bersifat dinamis dan tidak disimpan: sesi lama yang kolom tahun
	 * akademiknya kosong akan terus tampak sebagai milik tahun akademik <i>saat ini</i>, sehingga
	 * bisa ikut tersaring pada periode yang bukan periodenya. Bedakan dengan nilai yang benar-benar
	 * tersimpan bila keperluannya audit atau pelaporan historis.</p>
	 *
	 * <p>Berbeda dari getter lain di kelas ini, method ini <i>tidak</i> menulis balik ke field —
	 * penggantian hanya berlaku pada nilai kembalian.</p>
	 *
	 * @return tahun akademik tersimpan, atau tahun akademik berjalan bila kosong
	 */
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik sesi.
	 *
	 * @param tahunAkademik kode tahun akademik; {@code null} mengembalikan perilaku pengganti pada
	 *                      {@link #getTahunAkademik()}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan muatan JSON hasil sinkronisasi sesi ini ke Google Calendar.
	 *
	 * <p>Bila kolomnya kosong, yang dikembalikan adalah <b>objek JSON kosong</b>
	 * ({@code "{}"}) alih-alih {@code null}, supaya pemanggil dapat langsung mem-parsing hasilnya
	 * tanpa penjagaan tambahan. Konsekuensinya, pemanggil tidak bisa membedakan sesi yang belum
	 * pernah disinkronkan dari sesi yang muatannya memang kosong.</p>
	 *
	 * @return string JSON; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCalendarEvent() {
		return calendarEvent == null || calendarEvent.trim().isEmpty() ? new JSONObject().toString() : calendarEvent;
	}

	/**
	 * Menyimpan muatan JSON hasil sinkronisasi Google Calendar. Isinya tidak divalidasi sebagai JSON
	 * di sini.
	 *
	 * @param calendarEvent string JSON hasil sinkronisasi
	 */
	public void setCalendarEvent(String calendarEvent) {
		this.calendarEvent = calendarEvent;
	}

	/**
	 * Mode {@link #getOnlineMenggunakan()}: sesi tidak digelar daring. Tombol pertemuan daring
	 * disembunyikan.
	 *
	 * <p><b>Peringatan — konstanta di keluarga ini tidak {@code final}.</b> Kedelapan penanda mode
	 * dideklarasikan {@code public static Integer} tanpa {@code final}, sehingga kode mana pun dalam
	 * JVM dapat menugasinya ulang dan mengubah arti seluruh perbandingan mode di aplikasi. Tipenya
	 * pun {@code Integer} (objek) dan bukan {@code int}, jadi perbandingan <b>harus</b> memakai
	 * {@code equals()} seperti yang dilakukan
	 * {@link #createVideoConrefrence(InterviewCalonMahasiswa, Component, boolean, boolean,
	 * EventListener)} — memakai {@code ==} akan tampak benar untuk nilai kecil karena cache
	 * {@code Integer}, lalu diam-diam salah bila nilainya pernah di-boxing ulang.</p>
	 */
	public static Integer TIDAK_AKTIF = 0;

	/**
	 * Mode: pertemuan lewat Jitsi Meet. Tautannya <b>dibangkitkan</b> oleh
	 * {@link #generateJitsiLink()}, bukan disimpan di kolom.
	 */
	public static Integer JITSI = 1;

	/**
	 * Mode: pertemuan lewat Google Meet. Tautannya diambil dari {@code hangoutLink} pada cache
	 * berkas, yang baru terisi setelah sesi disinkronkan ke Google Calendar.
	 */
	public static Integer GOOGLE_MEET = 2;

	/** Mode: pertemuan lewat Zoom, memakai {@link #getZoomLink()}. */
	public static Integer ZOOM = 3;

	/** Mode: pertemuan lewat Big Blue Button, memakai {@link #getBbbLink()}. */
	public static Integer BBB = 4;

	/** Mode: pertemuan lewat Skype, memakai {@link #getSkypeLink()}. */
	public static Integer SKYPE = 5;

	/** Mode: pertemuan lewat grup WhatsApp, memakai {@link #getWaLink()}. */
	public static Integer WA = 6;

	/** Mode: kanal lain di luar daftar baku, memakai {@link #getLainLink()}. */
	public static Integer LAIN = 7;

	/**
	 * Mengembalikan kanal pertemuan daring yang dipakai sesi ini.
	 *
	 * <p><b>Getter ini mengubah state:</b> bila kolomnya {@code null}, field diisi
	 * {@link #TIDAK_AKTIF} dan penugasan itu bertahan pada objek — pada entity yang dikelola
	 * Hibernate hal ini dapat menandainya kotor dan memicu {@code UPDATE} beserta revisi audit
	 * palsu meski tidak ada perubahan berarti.</p>
	 *
	 * @return salah satu konstanta {@link #TIDAK_AKTIF} sampai {@link #LAIN}; tidak pernah
	 *         {@code null}
	 */
	public Integer getOnlineMenggunakan() {
		if (onlineMenggunakan == null) {
			onlineMenggunakan = TIDAK_AKTIF;
		}
		return onlineMenggunakan;
	}

	/**
	 * Menetapkan kanal pertemuan daring sesi.
	 *
	 * <p>Nilai tidak divalidasi terhadap rentang konstanta yang dikenal; angka di luar
	 * {@code 0..7} membuat seluruh cabang di
	 * {@link #createVideoConrefrence(InterviewCalonMahasiswa, Component, boolean, boolean,
	 * EventListener)} meleset sehingga tombol pertemuan disembunyikan.</p>
	 *
	 * @param onlineMenggunakan salah satu konstanta {@link #TIDAK_AKTIF} sampai {@link #LAIN}
	 */
	public void setOnlineMenggunakan(Integer onlineMenggunakan) {
		this.onlineMenggunakan = onlineMenggunakan;
	}

	/**
	 * Mengembalikan tautan rapat Zoom sesi ini, dengan cadangan dari cache berkas dan pembersihan
	 * teks di sekitarnya.
	 *
	 * <p>Alurnya sama untuk kelima getter tautan di kelas ini ({@link #getZoomLink()},
	 * {@link #getBbbLink()}, {@link #getSkypeLink()}, {@link #getWaLink()},
	 * {@link #getLainLink()}):</p>
	 * <ol>
	 *   <li>Bila kolom kosong, nilai diambil dari cache berkas lewat {@code retreive("zoomLink")}
	 *       — mekanisme cadangan {@link GeneralValueObject} yang diisi oleh setter pasangannya.</li>
	 *   <li>Bila nilainya <b>mengandung spasi</b>, isinya dianggap kalimat bercampur URL dan
	 *       {@code Common.getUrls()} dipakai untuk memungut URL pertama saja; sisanya dibuang.</li>
	 *   <li>Hasil akhir di-trim, dan string kosong dinormalkan menjadi {@code null}.</li>
	 * </ol>
	 *
	 * <p><b>Getter ini mengubah state.</b> Kedua langkah pertama menulis balik ke field, sehingga
	 * membaca properti ini dapat menandai entity kotor dan memicu {@code UPDATE} beserta revisi
	 * audit palsu. Langkah kedua bahkan bersifat <i>merusak</i>: teks asli yang ditulis pengguna
	 * digantikan permanen oleh potongan URL pertama begitu objek disimpan. Kegagalan pada langkah
	 * itu ditelan (hanya dicatat ke audit galat), jadi pemanggil tidak pernah tahu bila ekstraksi
	 * URL gagal.</p>
	 *
	 * @return tautan Zoom yang sudah dibersihkan, atau {@code null} bila tidak ada
	 * @see #setZoomLink(String)
	 */
	@Column(columnDefinition = "text")
	public String getZoomLink() {
		if (zoomLink == null || zoomLink.trim().isEmpty()) {
			zoomLink = this.retreive("zoomLink");
		}

		try {
			if (zoomLink != null && !zoomLink.trim().isEmpty() && zoomLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(zoomLink.trim());
				zoomLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:192");
		}

		return zoomLink == null || zoomLink.trim().isEmpty() ? null : zoomLink.trim();
	}

	/**
	 * Menetapkan tautan rapat Zoom sekaligus <b>mencerminkannya ke cache berkas</b> lewat
	 * {@code put(zoomLink, "zoomLink")}, supaya {@link #getZoomLink()} punya cadangan bila kolom
	 * basis data kelak kosong.
	 *
	 * <p>Pencerminan hanya terjadi untuk nilai non-kosong. Menyetel {@code null} atau string kosong
	 * <b>mengosongkan kolom tetapi tidak menghapus cache</b>, sehingga {@link #getZoomLink()} akan
	 * memunculkan kembali tautan lama dari cache — tautan Zoom yang "sudah dihapus" bisa hidup lagi.
	 * Perilaku ini berlaku sama pada {@link #setBbbLink(String)}, {@link #setSkypeLink(String)}, dan
	 * {@link #setWaLink(String)}.</p>
	 *
	 * @param zoomLink tautan rapat Zoom; {@code null}/kosong mengosongkan kolom tanpa menyentuh
	 *                 cache
	 */
	public void setZoomLink(String zoomLink) {
		if (zoomLink != null && !zoomLink.trim().isEmpty()) {
			this.put(zoomLink, "zoomLink");
		}
		this.zoomLink = zoomLink;
	}

	/**
	 * Mengembalikan tautan rapat Big Blue Button sesi ini.
	 *
	 * <p>Alur, efek samping, dan peringatannya identik dengan {@link #getZoomLink()} — termasuk
	 * cadangan dari cache berkas (kunci {@code "bbbLink"}), pemungutan URL pertama bila nilainya
	 * mengandung spasi, penulisan balik ke field, dan penelanan galat.</p>
	 *
	 * @return tautan Big Blue Button yang sudah dibersihkan, atau {@code null} bila tidak ada
	 * @see #getZoomLink()
	 * @see #setBbbLink(String)
	 */
	@Column(columnDefinition = "text")
	public String getBbbLink() {
		if (bbbLink == null || bbbLink.trim().isEmpty()) {
			bbbLink = this.retreive("bbbLink");
		}

		try {
			if (bbbLink != null && !bbbLink.trim().isEmpty() && bbbLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(bbbLink.trim());
				bbbLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:217");
		}

		return bbbLink == null || bbbLink.trim().isEmpty() ? null : bbbLink.trim();
	}

	/**
	 * Menetapkan tautan rapat Big Blue Button sekaligus mencerminkannya ke cache berkas (kunci
	 * {@code "bbbLink"}).
	 *
	 * @param bbbLink tautan Big Blue Button; {@code null}/kosong mengosongkan kolom tanpa menghapus
	 *                cache
	 * @see #setZoomLink(String)
	 */
	public void setBbbLink(String bbbLink) {
		if (bbbLink != null && !bbbLink.trim().isEmpty()) {
			this.put(bbbLink, "bbbLink");
		}
		this.bbbLink = bbbLink;
	}

	/**
	 * Mengembalikan tautan panggilan Skype sesi ini.
	 *
	 * <p>Alur, efek samping, dan peringatannya identik dengan {@link #getZoomLink()}; kunci cache
	 * berkasnya {@code "skypeLink"}.</p>
	 *
	 * @return tautan Skype yang sudah dibersihkan, atau {@code null} bila tidak ada
	 * @see #getZoomLink()
	 * @see #setSkypeLink(String)
	 */
	@Column(columnDefinition = "text")
	public String getSkypeLink() {
		if (skypeLink == null || skypeLink.trim().isEmpty()) {
			skypeLink = this.retreive("skypeLink");
		}

		try {
			if (skypeLink != null && !skypeLink.trim().isEmpty() && skypeLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(skypeLink.trim());
				skypeLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:242");
		}

		return skypeLink == null || skypeLink.trim().isEmpty() ? null : skypeLink.trim();
	}

	/**
	 * Menetapkan tautan panggilan Skype sekaligus mencerminkannya ke cache berkas (kunci
	 * {@code "skypeLink"}).
	 *
	 * @param skypeLink tautan Skype; {@code null}/kosong mengosongkan kolom tanpa menghapus cache
	 * @see #setZoomLink(String)
	 */
	public void setSkypeLink(String skypeLink) {
		if (skypeLink != null && !skypeLink.trim().isEmpty()) {
			this.put(skypeLink, "skypeLink");
		}
		this.skypeLink = skypeLink;
	}

	/**
	 * Mengembalikan tautan bebas untuk mode {@link #LAIN}.
	 *
	 * <p>Alurnya mengikuti {@link #getZoomLink()}, tetapi <b>dua hal berbeda dan patut
	 * diperhatikan</b>:</p>
	 * <ol>
	 *   <li><b>Kunci cache-nya tidak seragam.</b> Getter ini membaca cadangan dari kunci
	 *       {@code "link_online"}, bukan {@code "lainLink"} seperti pola saudara-saudaranya.</li>
	 *   <li><b>Pasangannya tidak pernah mengisi cache itu.</b> Berbeda dari keempat setter tautan
	 *       lain, {@link #setLainLink(String)} <i>tidak</i> memanggil {@code put(...)}. Akibatnya
	 *       cadangan {@code "link_online"} hanya bisa terisi oleh penulis lain di luar kelas ini;
	 *       lewat jalur setter biasa, cabang cadangan pada getter ini praktis tidak pernah
	 *       menghasilkan nilai.</li>
	 * </ol>
	 * <p>Perlakukan ketidakseragaman ini sebagai fakta yang ada, bukan sebagai pola yang layak
	 * ditiru saat menambahkan kanal baru.</p>
	 *
	 * @return tautan bebas yang sudah dibersihkan, atau {@code null} bila tidak ada
	 * @see #getZoomLink()
	 * @see #setLainLink(String)
	 */
	@Column(columnDefinition = "text")
	public String getLainLink() {
		if (lainLink == null || lainLink.trim().isEmpty()) {
			lainLink = this.retreive("link_online");
		}

		try {
			if (lainLink != null && !lainLink.trim().isEmpty() && lainLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(lainLink.trim());
				lainLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:267");
		}

		return lainLink == null || lainLink.trim().isEmpty() ? null : lainLink.trim();
	}

	/**
	 * Menetapkan tautan bebas untuk mode {@link #LAIN}.
	 *
	 * <p><b>Tidak mencerminkan nilai ke cache berkas</b> — berbeda dari {@link #setZoomLink(String)},
	 * {@link #setBbbLink(String)}, {@link #setSkypeLink(String)}, dan {@link #setWaLink(String)}
	 * yang semuanya memanggil {@code put(...)}. Lihat penjelasan lengkapnya pada
	 * {@link #getLainLink()}.</p>
	 *
	 * @param lainLink tautan bebas; disimpan apa adanya termasuk {@code null}
	 */
	public void setLainLink(String lainLink) {
		this.lainLink = lainLink;
	}

	/**
	 * Membangkitkan URL ruang Jitsi Meet untuk sesi ini — mode {@link #JITSI} tidak menyimpan tautan
	 * di kolom mana pun, melainkan menurunkannya dari identitas sesi setiap kali dibutuhkan.
	 *
	 * <p>Nama ruang disusun dari {@code "GEL_" + getNama() + "_" + getId()}, diberi awalan nama
	 * konteks aplikasi (dari {@code request.getContextPath()}) yang sudah di-URL-encode, lalu
	 * dinormalkan: semua karakter selain huruf/angka/spasi diganti garis bawah, huruf dijadikan
	 * kecil, spasi dirapatkan, dan garis bawah ganda dimampatkan (tiga kali berturut-turut — jadi
	 * rentetan garis bawah yang sangat panjang bisa tersisa). Hasilnya ditempelkan di belakang
	 * alamat server dari konfigurasi {@code alamat_server_video_conference}, yang bila belum ada
	 * akan memakai {@code https://meet.jit.si}.</p>
	 *
	 * <p><b>Catatan konfigurasi:</b> {@code Common.getKonfigurasi(...)} pada basis kode ini menulis
	 * nilai baku ke basis data bila kuncinya belum ada, jadi pemanggilan pertama dapat menciptakan
	 * baris konfigurasi baru.</p>
	 *
	 * <p><b>Catatan keamanan:</b> nama ruang sepenuhnya dapat diprediksi dari nama sesi dan id-nya,
	 * dan Jitsi publik tidak memerlukan otentikasi untuk masuk ke sebuah ruang. Siapa pun yang dapat
	 * menebak atau memperoleh kombinasi itu bisa bergabung ke wawancara. Kendali aksesnya harus
	 * berasal dari sisi server Jitsi (mis. ruang berkata sandi atau server sendiri lewat konfigurasi
	 * di atas), bukan dari kerahasiaan URL.</p>
	 *
	 * <p>Method ini membaca {@code HttpServletRequest} dari eksekusi ZK yang sedang berjalan dan
	 * jatuh kembali ke {@link RequestContext} bila dipanggil di luar konteks ZK; bila keduanya tidak
	 * tersedia, pemanggilan berakhir dengan {@code NullPointerException}.</p>
	 *
	 * @return URL lengkap ruang Jitsi untuk sesi ini
	 * @throws Exception bila pengodean URL gagal atau tidak ada {@code HttpServletRequest} yang
	 *                   dapat dipakai
	 */
	public String generateJitsiLink() throws Exception {
		InterviewCalonMahasiswa interviewCalonMahasiswa = this;
		String id = "GEL_" + interviewCalonMahasiswa.getNama() + "_" + interviewCalonMahasiswa.getId();

		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}
		String kodeStream = (URLEncoder
				.encode(org.apache.commons.lang3.StringUtils.replace(request.getContextPath(), "/", ""), "UTF-8") + "_")
				+ id;
		try {
			String[] words = kodeStream.replaceAll("[^a-zA-Z0-9 ]", "_").toLowerCase().split("\\s+");
			kodeStream = "";
			for (String w : words) {
				kodeStream += kodeStream.isEmpty() ? w : "_" + w;
			}

			kodeStream = kodeStream.replaceAll("__", "_");
			kodeStream = kodeStream.replaceAll("__", "_");
			kodeStream = kodeStream.replaceAll("__", "_");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:304");
		}
		String server = Common.getKonfigurasi("alamat_server_video_conference", "https://meet.jit.si").getNilai() + "/"
				+ kodeStream;
		return server;
	}

	/**
	 * Mengembalikan tautan grup WhatsApp sesi ini.
	 *
	 * <p>Alur, efek samping, dan peringatannya identik dengan {@link #getZoomLink()}; kunci cache
	 * berkasnya {@code "waLink"}.</p>
	 *
	 * @return tautan grup WhatsApp yang sudah dibersihkan, atau {@code null} bila tidak ada
	 * @see #getZoomLink()
	 * @see #setWaLink(String)
	 */
	@Column(columnDefinition = "text")
	public String getWaLink() {
		if (waLink == null || waLink.trim().isEmpty()) {
			waLink = this.retreive("waLink");
		}

		try {
			if (waLink != null && !waLink.trim().isEmpty() && waLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(waLink.trim());
				waLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:323");
		}

		return waLink == null || waLink.trim().isEmpty() ? null : waLink.trim();
	}

	/**
	 * Menetapkan tautan grup WhatsApp sekaligus mencerminkannya ke cache berkas (kunci
	 * {@code "waLink"}).
	 *
	 * @param waLink tautan undangan grup WhatsApp; {@code null}/kosong mengosongkan kolom tanpa
	 *               menghapus cache
	 * @see #setZoomLink(String)
	 */
	public void setWaLink(String waLink) {
		if (waLink != null && !waLink.trim().isEmpty()) {
			this.put(waLink, "waLink");
		}
		this.waLink = waLink;
	}

	public static Button createVideoConrefrence(final InterviewCalonMahasiswa interviewCalonMahasiswa, Component hbox,
			boolean vertical, final EventListener eventListener) throws Exception {
		return createVideoConrefrence(interviewCalonMahasiswa, hbox, vertical, false, eventListener);
	}

	public static Button createVideoConrefrence(final InterviewCalonMahasiswa interviewCalonMahasiswa, Component hbox,
			boolean vertical, boolean button, final EventListener eventListener) throws Exception {

		Button toolbarbutton = button ? new MyButtonConfig("Online", "/img/svg/user-group.svg")
				: new MyToolbarbuttonConfig("Online", "/img/svg/user-group.svg");

		if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.JITSI)) {

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			int jumlah = 0;
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
					jumlah++;
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:360");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}
			if (jumlah > 0) {
				toolbarbutton.setImage("/img/online-red-icon.png");
			}
			toolbarbutton.setParent(hbox);

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					interviewCalonMahasiswa.masukkanData("online");

					((Button) a.getTarget()).setImage("/img/online-red-icon.png");

					String server = interviewCalonMahasiswa.generateJitsiLink();
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");
					}

					if (eventListener != null) {
						eventListener.onEvent(null);
					}
				}
			});

		} else if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.GOOGLE_MEET)) {
			toolbarbutton.setImage("/img/meet-google.png");
			toolbarbutton.setStyle("font-size:9px");
			if (hbox != null)
				toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:415");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {

					String linkcalendar = interviewCalonMahasiswa.retreive("hangoutLink");
					if (linkcalendar == null || linkcalendar.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk tatap muka online menggunakan Google Meet, harap singkronkan dulu ke Google Calendar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					interviewCalonMahasiswa.masukkanData("online");

					String server = linkcalendar + "?hs=122&ijlm=1588886137268";
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {

						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}

			});
		} else if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.ZOOM)) {
			toolbarbutton.setImage("/img/zoom.png");
			toolbarbutton.setStyle("font-size:9px");
			if (hbox != null)
				toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:467");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {

					String server = interviewCalonMahasiswa.getZoomLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk tatap muka online menggunakan zoom, harap link zoom dimasukkan secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					interviewCalonMahasiswa.masukkanData("online");

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}

			});
		} else if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.BBB)) {
			toolbarbutton.setImage("/img/bbb.png");
			toolbarbutton.setStyle("font-size:9px");
			if (hbox != null)
				toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:517");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {

					String server = interviewCalonMahasiswa.getBbbLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk tatap muka online menggunakan Big Blue Button, harap link Big Blue Button dimasukkan secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					interviewCalonMahasiswa.masukkanData("online");

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}

			});
		} else if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.SKYPE)) {
			toolbarbutton.setImage("/img/Skype-icon.png");
			toolbarbutton.setStyle("font-size:9px");
			if (hbox != null)
				toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:567");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {

					String server = interviewCalonMahasiswa.getSkypeLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk tatap muka online menggunakan Skype, harap link Skype dimasukkan secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					interviewCalonMahasiswa.masukkanData("online");

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

					}
				}

			});
		} else if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.WA)) {
			toolbarbutton.setImage("/img/svg/whats.svg");
			toolbarbutton.setStyle("font-size:9px");
			if (hbox != null)
				toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:617");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {

					String server = interviewCalonMahasiswa.getWaLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk tatap muka online menggunakan Grup Whatsapp, harap link Grup Whatsapp dimasukkan secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					interviewCalonMahasiswa.masukkanData("online");

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Grup Whatsapp', w: 1200, h: 600});");

					}
				}

			});
		} else if (interviewCalonMahasiswa.getOnlineMenggunakan().equals(InterviewCalonMahasiswa.LAIN)) {
			toolbarbutton.setImage("/img/online-red-icon.png");
			toolbarbutton.setStyle("font-size:9px");
			if (hbox != null)
				toolbarbutton.setParent(hbox);
			if (vertical) {
				toolbarbutton.setOrient("vertical");
			}

			TreeMap<String, String> d = interviewCalonMahasiswa.ambilData("online", null, "", null, null, null);

			String onl = "";
			for (String user : d.keySet()) {
				try {
					String jam = d.get(user);
					String[] u = user.split("-");
					onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/InterviewCalonMahasiswa.java:667");
				}
			}
			if (!onl.isEmpty()) {
				toolbarbutton.setTooltiptext(onl);
			}

			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {

					String server = interviewCalonMahasiswa.getLainLink();
					if (server == null || server.trim().isEmpty()) {
						MyMessageboxConfig.show("Untuk tatap muka online, link belum dimasukkan secara benar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					interviewCalonMahasiswa.masukkanData("online");

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
					} else {
						Clients.evalJavaScript(
								"popupCenter({url: '" + server + "', title: 'Grup Whatsapp', w: 1200, h: 600});");

					}
				}

			});
		} else {
			toolbarbutton.setVisible(false);
		}
		return toolbarbutton;

	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		return mulai == null ? WaktuUtil.getDate() : mulai;
	}

	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getSampai() {
		return sampai == null ? WaktuUtil.getDate() : sampai;
	}

	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	public Integer getKapasitasRuangan() {
		return kapasitasRuangan == null ? 3000 : kapasitasRuangan;
	}

	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	public String getGelombangPendaftaranLain() {
		if (gelombangPendaftaranLain == null) {
			gelombangPendaftaranLain = "";
		}
		return gelombangPendaftaranLain;
	}

	public List<GelombangPendaftaran> ambilGelombangPendaftaran() {

		List<GelombangPendaftaran> gelombangPendaftarans = new ArrayList<GelombangPendaftaran>();

		for (String kode : StringUtils.split(getGelombangPendaftaranLain(), ",")) {
			if (!kode.trim().isEmpty()) {
				GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) ConstantValues
						.simpleObject(HibernateUtil.currentSession().createCriteria(GelombangPendaftaran.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("id",
										!Common.isNumber(kode.trim()) ? -1L : Long.parseLong(kode.trim())))
								.setMaxResults(1), GelombangPendaftaran.class);
				if (gelombangPendaftaran != null && !gelombangPendaftarans.contains(gelombangPendaftaran)) {
					gelombangPendaftarans.add(gelombangPendaftaran);
				}
			}
		}

		Collections.sort(gelombangPendaftarans);
		return gelombangPendaftarans;
	}

	public List<Long> ambilGelombangPendaftaranId() {

		List<Long> gelombangPendaftarans = new ArrayList<Long>();

		for (String kode : StringUtils.split(getGelombangPendaftaranLain(), ",")) {
			if (!kode.trim().isEmpty()) {
				try {
					Long id = Long.parseLong(kode.trim());
					if (!gelombangPendaftarans.contains(id)) {
						gelombangPendaftarans.add(id);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/InterviewCalonMahasiswa.java:799");
					// TODO: handle exception
				}
			}
		}

		Collections.sort(gelombangPendaftarans);
		return gelombangPendaftarans;
	}

	public void setGelombangPendaftaranLain(String gelombangPendaftaranLain) {
		this.gelombangPendaftaranLain = gelombangPendaftaranLain;
	}

	public Boolean getHrsUjian() {
		return hrsUjian == null ? false : hrsUjian;
	}

	public void setHrsUjian(Boolean hrsUjian) {
		this.hrsUjian = hrsUjian;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getJurusan() != null) {
			fakultas = getJurusan().getFakultas();
		}
		return fakultas;
	}

	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}
}
