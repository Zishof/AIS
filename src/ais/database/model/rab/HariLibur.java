package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Master hari libur RAB — satu baris per {@link #getTanggal() tanggal} (kolom unik), menandai
 * apakah tanggal itu {@link #getLibur() libur} beserta nama/keterangan peristiwanya (mis. hari
 * libur nasional/cuti bersama yang relevan bagi kalender kerja RAB). Ditampilkan dan diedit
 * lewat widget kalender ZK {@code org.zkoss.calendar} pada layar
 * {@code ais.action.master.rab.KalenderHariLiburAction}, dibungkus sebagai event kalender oleh
 * {@code ais.action.master.rab.helper.HariLiburKalenderEvent} (menentukan warna tampil: merah
 * untuk hari libur, biru untuk event non-libur) dan dipersiskan ke/dari database oleh
 * {@code ais.action.master.rab.helper.HariLiburKalenderModel} (save/delete/update langsung lewat
 * {@code Session} Hibernate saat event kalender ditambah/dihapus/diubah pengguna).
 *
 * <p><b>Entity ini TIDAK berelasi dengan {@code employ.LiburNasional} maupun
 * {@code payroll.LiburRutin}</b> — dua entity itu adalah master hari libur khusus modul
 * kepegawaian/absensi (dipakai antara lain oleh {@code Common.isHolidayMerahDanAtauHariLibur(...)}
 * untuk perhitungan cuti/lembur pegawai) dan berada di tabel/skema yang berbeda sama sekali.
 * {@code HariLibur} di paket ini murni untuk kalender kerja RAB.</p>
 *
 * <h2>{@code HariLibur} vs {@link Kalender} — DUA tabel master hari libur RAB yang paralel</h2>
 * <p>Modul RAB ternyata memiliki DUA entity berbeda yang sama-sama memodelkan "tanggal + status
 * libur": kelas ini ({@code rab.hari_libur}, dipakai jalur ZK lama lewat
 * {@code KalenderHariLiburAction}/{@code HariLiburKalenderModel} di atas) dan {@link Kalender}
 * ({@code rab.kalender}, dipakai jalur baru {@code KalenderHariLiburGenericCrudAdapter} berbasis
 * Generic CRUD v2 — komentar pada adapter itu sendiri menyebutnya "master hari libur"). Kedua
 * tabel TIDAK disinkronkan satu sama lain: menambah hari libur lewat satu jalur tidak
 * memunculkannya di jalur lain. Ini bukan bug pada kelas ini secara individual, melainkan
 * duplikasi arsitektural antar dua generasi implementasi fitur yang sama — pemanggil baru yang
 * butuh daftar hari libur RAB perlu memastikan lebih dulu jalur mana (lama atau baru) yang benar-
 * benar dipakai sebagai sumber kebenaran pada konteksnya, karena kedua tabel bisa berisi data
 * yang berbeda untuk tanggal yang sama.</p>
 *
 * <h2>Pola arsitektur khas AIS yang muncul di kelas ini</h2>
 * <ul>
 * <li>Field {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta method
 * {@link #onUpdate()} adalah <b>field audit bayangan</b> yang menduplikasi field privat bernama
 * sama di {@link GeneralValueObject} — KEHARUSAN TEKNIS (induk bukan {@code @Entity} sehingga
 * tidak bisa mewariskan pemetaan kolom JPA), bukan salin-tempel ceroboh. Deklarasi
 * {@code oleh}/{@code olehId} pada kelas ini dipadatkan menjadi satu baris fisik (peninggalan
 * generator) tetapi perilakunya identik dengan entity RAB lain di paket ini.</li>
 * <li>{@link #getLibur()} adalah getter dengan <b>default terhitung dan efek samping mutasi
 * state</b>, pola yang sama seperti {@link Satuan#getAktif()}: lihat penjelasan lengkap pada
 * Javadoc method tersebut.</li>
 * </ul>
 *
 * @see Kalender
 * @see ais.action.master.rab.KalenderHariLiburAction
 * @see ais.action.master.rab.helper.HariLiburKalenderEvent
 * @see ais.action.master.rab.helper.HariLiburKalenderModel
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "hari_libur")
public class HariLibur extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya sama dengan {@link MetodePengadaan}/{@link Satuan}/
	 * {@link SatuanLokasi} (peninggalan hasil salin-tempel generator hbm2java); tidak masalah
	 * selama tidak ada dua kelas berbeda yang benar-benar diserialkan/dideserialkan saling
	 * tertukar sebagai satu sama lain.
	 */
	private static final long serialVersionUID = -8738027816264807168L;
	/** Primary key baris {@code rab.hari_libur}. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (field audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (field audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;
	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> agar jejak audit yang sudah terisi tidak bisa terhapus oleh
	 * jalur simpan yang kebetulan tidak membawa informasi pengguna (mis. save langsung dari
	 * {@code HariLiburKalenderModel} tanpa konteks pengguna).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
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
	 * Hook {@code @PreUpdate} yang mengimplementasikan kontrak abstrak
	 * {@link GeneralValueObject#onUpdate()}: dipanggil JPA tepat sebelum UPDATE dieksekusi, dan
	 * menyerahkan penyegaran {@link #tanggal_dirubah} ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Karena kait ini hanya
	 * menempel pada {@code @PreUpdate} (bukan {@code @PrePersist}), jejak waktu pada operasi INSERT
	 * pertama bergantung sepenuhnya pada nilai awal field di bawah ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Cap waktu perubahan terakhir (field audit bayangan; lihat catatan kelas). Diinisialisasi ke
	 * waktu server saat object dibuat lewat {@code WaktuUtil.getDate()} sehingga baris baru tidak
	 * pernah membawa nilai {@code null}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Tanpa validasi — berbeda dari {@link #setOleh(String)}/
	 * {@link #setOlehId(String)}, {@code null} akan benar-benar tersimpan.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return cap waktu perubahan terakhir; praktis tidak pernah {@code null} untuk object yang
	 *         dibuat lewat konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Judul hari libur/event, dijadikan {@code title} widget kalender oleh
	 * {@code HariLiburKalenderEvent}. Diinisialisasi ke string kosong (bukan {@code null}) —
	 * berbeda dari kebanyakan entity katalog RAB lain di paket ini yang membiarkan field teksnya
	 * {@code null} sampai disetel. Lihat {@link #getNama()}.
	 */
	private String nama = "";
	/**
	 * Keterangan/isi event, dijadikan {@code content} widget kalender oleh
	 * {@code HariLiburKalenderEvent}. Sama seperti {@link #nama}, diinisialisasi ke string kosong,
	 * bukan {@code null}. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan = "";
	/** Tanggal hari libur/event ini, kolom unik. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Penanda tanggal ini libur atau bukan, dengan default terhitung bila belum disetel. Lihat {@link #getLibur()}. */
	private Boolean libur;

	/**
	 * Constructor default tanpa argumen, WAJIB ada agar Hibernate dapat menginstansiasi entity
	 * lewat refleksi saat memuat baris dari database, dan agar layar kalender dapat membuat
	 * object kosong untuk event baru.
	 */
	public HariLibur() {
	}

	/**
	 * Constructor pintas untuk langsung menyetel nama/judul hari libur.
	 *
	 * @param nama nama/judul hari libur
	 */
	public HariLibur(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris {@code rab.hari_libur}.
	 *
	 * @return primary key, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/judul hari libur, sudah di-{@code trim()} bila tidak {@code null}.
	 * Berbeda dari kebanyakan entity katalog RAB lain, kolom ini {@code nullable = true} — nama
	 * boleh kosong di database (meski field in-memory diinisialisasi string kosong, bukan
	 * {@code null}).
	 *
	 * @return nama hari libur yang sudah dipangkas spasi tepi, atau {@code null} bila field
	 *         mentahnya {@code null}
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/judul hari libur. Tanpa validasi maupun pemangkasan spasi di sisi setter —
	 * pemangkasan hanya terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama hari libur baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/isi event hari libur ini.
	 *
	 * @return teks keterangan, tidak pernah {@code null} untuk object baru (diinisialisasi string
	 *         kosong), tetapi bisa menjadi {@code null} bila disetel eksplisit lewat
	 *         {@link #setKeterangan(String)}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan/isi event. Tanpa validasi.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal hari libur/event ini, dipetakan sebagai kolom {@code DATE} dan
	 * <b>unik</b> — satu tanggal hanya boleh punya satu baris {@code HariLibur} (constraint
	 * database, bukan sekadar konvensi aplikasi).
	 *
	 * @return tanggal hari libur, atau {@code null} bila belum diisi (hanya berlaku sebelum
	 *         disimpan, karena kolomnya {@code nullable = false})
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal", unique = true, nullable = false)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel tanggal hari libur/event. Tanpa validasi; keunikan baru ditegakkan oleh constraint
	 * database saat disimpan.
	 *
	 * @param tanggal tanggal hari libur baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan status libur/tidaknya tanggal ini, dengan <b>default terhitung dan efek
	 * samping mutasi state</b> — pola yang sama seperti {@link Satuan#getAktif()}.
	 *
	 * <p>Bila {@link #libur} belum pernah disetel eksplisit ({@code null}) DAN {@link #tanggal}
	 * sudah terisi, method ini menghitung apakah hari-dalam-minggu tanggal tersebut jatuh pada
	 * Sabtu/Minggu, lalu <b>menyimpan hasilnya ke field {@link #libur}</b> (bukan cuma
	 * mengembalikannya) — artinya panggilan berikutnya pada object yang sama tidak menghitung
	 * ulang, dan {@code equals()}/serialisasi selanjutnya akan membawa nilai yang sudah "menetap"
	 * ini walau tidak pernah ada pemanggilan {@link #setLibur(Boolean)} eksplisit. Bila
	 * {@link #tanggal} masih {@code null} (mis. object baru yang belum diisi), tidak ada
	 * perhitungan yang dilakukan dan method mengembalikan {@link #libur} apa adanya — yaitu
	 * {@code null}, BUKAN {@code false}. Perhatikan ini berbeda dari {@link Satuan#getAktif()}
	 * yang punya fallback akhir {@code true}; getter ini tidak punya fallback non-null sama
	 * sekali di jalur tersebut.</p>
	 *
	 * @return {@code true} bila Sabtu/Minggu (atau nilai yang sudah disetel eksplisit sebelumnya),
	 *         {@code false} bila hari kerja biasa dan sudah disetel eksplisit {@code false}, atau
	 *         {@code null} bila {@link #tanggal} belum terisi dan status belum pernah dihitung/
	 *         disetel
	 */
	public Boolean getLibur() {
		if (tanggal != null && libur == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			libur = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
					|| calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
		}
		return libur;
	}

	/**
	 * Menyetel status libur secara eksplisit. Setelah dipanggil dengan nilai non-{@code null},
	 * {@link #getLibur()} tidak akan pernah lagi menghitung ulang default dari {@link #tanggal} —
	 * inilah yang dipakai {@code KalenderHariLiburAction} untuk menimpa hasil hitungan otomatis
	 * saat pengguna secara eksplisit menandai/membatalkan status libur suatu tanggal lewat UI.
	 *
	 * @param libur status libur baru; {@code null} mengembalikan field ke keadaan "belum disetel"
	 *              sehingga {@link #getLibur()} akan menghitung ulang defaultnya (bila
	 *              {@link #tanggal} sudah terisi)
	 */
	public void setLibur(Boolean libur) {
		this.libur = libur;
	}

}
