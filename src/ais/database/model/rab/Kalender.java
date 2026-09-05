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
 * Master hari libur RAB generasi <b>baru</b> — satu baris per {@link #getTanggal() tanggal}
 * (kolom unik) menandai apakah tanggal itu {@link #getHariLibur() hari libur}, dikelola lewat
 * kerangka Generic CRUD v2 melalui
 * {@code ais.action.master.generic.v2.adapter.KalenderHariLiburGenericCrudAdapter} — komentar
 * pada adapter itu sendiri menyebut entity ini "master hari libur", dan secara eksplisit
 * menegaskan dirinya <b>terpisah dari {@code ais.action.master.rab.KalenderAction}</b>, yang
 * sesungguhnya mengelola entity {@code Acara} (agenda/acara, konsep berbeda sama sekali dari
 * kalender hari libur). Adapter tersebut menegakkan aturan bisnis di sisi Java sebelum simpan:
 * {@code nama} dan {@code tanggal} wajib diisi, serta tanggal tidak boleh duplikat (dicek lewat
 * query {@code Restrictions.eq("tanggal", ...)} selain constraint {@code unique = true} pada
 * kolomnya sendiri) — keduanya dilempar sebagai {@code GenericCrudException} berkode
 * {@code CALENDAR_NAME_REQUIRED}/{@code CALENDAR_DATE_REQUIRED}/{@code CALENDAR_DATE_EXISTS}.
 * Baris baru yang dibuat lewat adapter otomatis diberi {@code tanggal = new Date()} dan
 * {@link #hariLibur} dihitung dari status akhir pekan tanggal tersebut.
 *
 * <h2>{@link HariLibur} vs {@code Kalender} — DUA tabel master hari libur RAB yang paralel</h2>
 * <p>Modul RAB memiliki DUA entity berbeda yang sama-sama memodelkan "tanggal + status libur":
 * {@link HariLibur} ({@code rab.hari_libur}, jalur ZK lama lewat
 * {@code KalenderHariLiburAction}/{@code HariLiburKalenderModel}, memakai widget kalender
 * {@code org.zkoss.calendar}) dan kelas ini ({@code rab.kalender}, jalur baru berbasis Generic
 * CRUD v2). Kedua tabel TIDAK disinkronkan satu sama lain: menambah hari libur lewat satu jalur
 * tidak memunculkannya di jalur lain — ini duplikasi arsitektural antar dua generasi implementasi
 * fitur yang sama, bukan bug pada kelas ini secara individual. Pemanggil baru yang butuh daftar
 * hari libur RAB perlu memastikan lebih dulu tabel mana yang benar-benar dipakai sebagai sumber
 * kebenaran pada konteksnya.</p>
 *
 * <h2>Pola arsitektur khas AIS yang muncul di kelas ini</h2>
 * <ul>
 * <li>Field {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta method
 * {@link #onUpdate()} adalah <b>field audit bayangan</b> yang menduplikasi field privat bernama
 * sama di {@link GeneralValueObject} — KEHARUSAN TEKNIS (induk bukan {@code @Entity} sehingga
 * tidak bisa mewariskan pemetaan kolom JPA), bukan salin-tempel ceroboh. Deklarasi
 * {@code oleh}/{@code olehId} pada kelas ini dipadatkan menjadi satu baris fisik (peninggalan
 * generator) tetapi perilakunya identik dengan entity RAB lain di paket ini. {@code oleh}/
 * {@code olehId} diisi otomatis oleh adapter dari {@code Tbmuser} sesi aktif saat
 * {@code beforeSave(...)}, berbeda dari {@link HariLibur} yang tidak pernah diisi lewat jalur
 * itu.</li>
 * <li>{@link #getHariLibur()} adalah getter dengan <b>default terhitung dan efek samping mutasi
 * state</b>, pola yang sama seperti {@link Satuan#getAktif()} dan {@link HariLibur#getLibur()} —
 * lihat rincian perbedaan kecilnya pada Javadoc method tersebut.</li>
 * </ul>
 *
 * @see HariLibur
 * @see ais.action.master.generic.v2.adapter.KalenderHariLiburGenericCrudAdapter
 * @see ais.action.master.rab.KalenderAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "kalender")



public class Kalender extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya sama dengan {@link MetodePengadaan}/{@link Satuan}/
	 * {@link SatuanLokasi}/{@link HariLibur} (peninggalan hasil salin-tempel generator hbm2java);
	 * tidak masalah selama tidak ada dua kelas berbeda yang benar-benar diserialkan/
	 * dideserialkan saling tertukar sebagai satu sama lain.
	 */
	private static final long serialVersionUID = -8738027816264807168L;
	/** Primary key baris {@code rab.kalender}. Lihat {@link #getId()}. */
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
	 * jalur simpan yang kebetulan tidak membawa informasi pengguna. Dalam praktiknya field ini
	 * diisi otomatis oleh
	 * {@code KalenderHariLiburGenericCrudAdapter.beforeSave(...)} dari {@code Tbmuser} sesi aktif.
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

	/** Nama/judul baris kalender hari libur ini. Wajib diisi (ditegakkan adapter Generic CRUD). Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas untuk baris ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Tanggal baris kalender ini, kolom unik. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Penanda tanggal ini hari libur atau bukan, dengan default terhitung bila belum disetel. Lihat {@link #getHariLibur()}. */
	private Boolean hariLibur;

	/**
	 * Constructor default tanpa argumen, WAJIB ada agar Hibernate dapat menginstansiasi entity
	 * lewat refleksi saat memuat baris dari database, dan agar
	 * {@code KalenderHariLiburGenericCrudAdapter.createNew(...)} dapat membuat object kosong
	 * untuk form tambah-baru (yang kemudian langsung diisi {@code tanggal}/{@code hariLibur}
	 * default oleh adapter).
	 */
	public Kalender() {
	}

	/**
	 * Constructor pintas untuk langsung menyetel nama/judul baris kalender.
	 *
	 * @param nama nama/judul baris kalender
	 */
	public Kalender(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris {@code rab.kalender}.
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
	 * Mengembalikan nama/judul baris kalender, sudah di-{@code trim()} bila tidak {@code null}.
	 * Kolom wajib diisi ({@code nullable = false}) pada tabel, dan juga ditegakkan ulang di sisi
	 * aplikasi oleh {@code KalenderHariLiburGenericCrudAdapter.beforeSave(...)}
	 * ({@code CALENDAR_NAME_REQUIRED}).
	 *
	 * @return nama baris kalender yang sudah dipangkas spasi tepi, atau {@code null} bila field
	 *         mentahnya {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/judul baris kalender. Tanpa validasi maupun pemangkasan spasi di sisi setter —
	 * pemangkasan hanya terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama baris kalender baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * @return teks keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal baris kalender ini, dipetakan sebagai kolom {@code DATE} dan
	 * <b>unik</b> — satu tanggal hanya boleh punya satu baris {@code Kalender} (ditegakkan ganda:
	 * constraint {@code unique = true} pada kolom database, DAN pengecekan eksplisit lewat query
	 * di {@code KalenderHariLiburGenericCrudAdapter.beforeSave(...)} yang melempar
	 * {@code CALENDAR_DATE_EXISTS} sebelum constraint database sempat dilanggar — sehingga
	 * pengguna mendapat pesan error yang ramah alih-alih exception database mentah).
	 *
	 * @return tanggal baris kalender, atau {@code null} bila belum diisi (hanya berlaku sebelum
	 *         disimpan, karena kolomnya {@code nullable = false})
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal", nullable = false, unique = true)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel tanggal baris kalender. Tanpa validasi; keunikan baru ditegakkan oleh pemanggil
	 * (lihat {@link #getTanggal()}) saat disimpan.
	 *
	 * @param tanggal tanggal baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan status hari libur/tidaknya tanggal ini, dengan <b>default terhitung dan efek
	 * samping mutasi state</b> — pola yang sama seperti {@link Satuan#getAktif()} dan
	 * {@link HariLibur#getLibur()}.
	 *
	 * <p>Bila {@link #hariLibur} belum pernah disetel eksplisit ({@code null}) DAN
	 * {@link #tanggal} sudah terisi, method ini menghitung apakah hari-dalam-minggu tanggal
	 * tersebut jatuh pada Sabtu/Minggu (memakai perbandingan {@code Integer.equals(...)}, bukan
	 * {@code ==} seperti pada {@link HariLibur#getLibur()} — secara fungsional setara untuk
	 * kisaran nilai konstanta {@link Calendar}, tetapi gaya penulisannya berbeda), lalu
	 * <b>menyimpan hasilnya ke field {@link #hariLibur}</b> (bukan cuma mengembalikannya) —
	 * panggilan berikutnya pada object yang sama tidak menghitung ulang. Bila {@link #tanggal}
	 * masih {@code null}, tidak ada perhitungan yang dilakukan dan method mengembalikan
	 * {@link #hariLibur} apa adanya, yaitu {@code null} — meski kolomnya dipetakan
	 * {@code nullable = false}, sehingga getter ini SENDIRI bisa mengembalikan {@code null} pada
	 * object yang belum lengkap; hanya jalur simpan (lewat {@code beforeSave(...)} yang memaksa
	 * {@code isWeekend(...)} bila masih {@code null} sebelum tanggal pasti terisi) yang menjamin
	 * kolom tidak pernah kosong di database.</p>
	 *
	 * @return {@code true} bila Sabtu/Minggu (atau nilai yang sudah disetel eksplisit sebelumnya),
	 *         {@code false} bila hari kerja biasa dan sudah disetel eksplisit {@code false}, atau
	 *         {@code null} bila {@link #tanggal} belum terisi dan status belum pernah dihitung/
	 *         disetel
	 */
	@Column(name = "hari_libur", nullable = false)
	public Boolean getHariLibur() {
		if (hariLibur == null && tanggal != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			Integer week = calendar.get(Calendar.DAY_OF_WEEK);
			if (week.equals(Calendar.SUNDAY) || week.equals(Calendar.SATURDAY)) {
				hariLibur = true;
			} else {
				hariLibur = false;
			}

		}
		return hariLibur;
	}

	/**
	 * Menyetel status hari libur secara eksplisit. Setelah dipanggil dengan nilai non-{@code null},
	 * {@link #getHariLibur()} tidak akan pernah lagi menghitung ulang default dari
	 * {@link #tanggal} — inilah yang dipakai pengguna lewat layar Generic CRUD untuk menimpa hasil
	 * hitungan otomatis akhir-pekan (mis. menandai suatu hari kerja biasa sebagai cuti bersama).
	 *
	 * @param hariLibur status hari libur baru; {@code null} mengembalikan field ke keadaan
	 *                  "belum disetel" sehingga {@link #getHariLibur()} akan menghitung ulang
	 *                  defaultnya (bila {@link #tanggal} sudah terisi)
	 */
	public void setHariLibur(Boolean hariLibur) {
		this.hariLibur = hariLibur;
	}

}
