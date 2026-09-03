package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Katalog/master <b>waktu shift</b> — mis. "Shift Pagi 08:00 (8 jam)", "Shift Malam 22:00 (10 jam)" —
 * dipetakan ke tabel <code>payroll.waktu_shift</code>. Berbeda dari {@link JenisShiftPegawai} (header
 * definisi shift lengkap dengan flag rotasi, lokasi geofencing, toleransi telat/pulang cepat, dsb.),
 * entity ini adalah katalog jam yang jauh lebih sederhana: hanya {@link #getKode()}, {@link #getNama()},
 * {@link #getMulai()} (jam mulai), dan {@link #getJam()} (durasi dalam jam) — jam selesai
 * ({@link #getSampai()}) SELALU DIHITUNG, tidak pernah dibaca langsung dari kolomnya (lihat javadoc
 * getter tersebut).
 *
 * <h3>Peran dalam rantai shift kerja pegawai (diverifikasi dari kode)</h3>
 * <p>Entity ini <b>tidak</b> berelasi FK langsung ke {@code Pegawai} atau ke {@code JenisShiftPegawai}.
 * Satu-satunya pemakainya di rantai shift adalah {@link DetailJenisShiftPegawai}, lewat field
 * {@code DetailJenisShiftPegawai.waktuShift} ({@code @ManyToOne}, javadoc kelas itu menyebutnya "sumber
 * jam mulai/sampai kanonik opsional"): bila baris detail shift memasang {@code waktuShift}, getter
 * {@code DetailJenisShiftPegawai.getMulai()}/{@code getSampai()} menimpa field lokal mereka dengan hasil
 * {@link #getMulai()}/{@link #getSampai()} milik instance ini pada SETIAP pemanggilan — {@code WaktuShift}
 * dengan demikian berfungsi sebagai daftar preset jam yang bisa dipakai ulang lintas banyak baris detail
 * shift, alih-alih setiap baris detail mengetik ulang jam mulai/sampai secara manual. Di luar kelas
 * {@code DetailJenisShiftPegawai}, entity ini dikelola lewat layar administratif
 * {@code ais.action.master.payroll.WaktuShiftAction} (CRUD katalog waktu shift) — tidak ada jalur lain
 * (query, FK, atau field) yang menghubungkannya ke {@code StatuskehadiranKaryawanHarian} atau ke
 * transaksi/potongan gaji secara langsung; pengaruhnya ke absensi/gaji selalu tidak langsung, lewat
 * {@code DetailJenisShiftPegawai} yang menyalin jamnya.</p>
 *
 * <h3>Peringatan representasi waktu: {@link #getSampai()} adalah kolom "semu" (selalu dihitung ulang)</h3>
 * <p>Berbeda dengan pola getter destruktif biasa (yang hanya mengisi fallback ke field bila field masih
 * {@code null}), {@link #getSampai()} <b>TANPA SYARAT</b> menghitung ulang dan MENIMPA field
 * {@link #sampai} dari {@link #getMulai()} + {@link #getJam()} jam setiap kali dipanggil — nilai apa pun
 * yang di-set lewat {@link #setSampai(Date)} atau yang tersimpan di kolom {@code sampai} pada baris
 * database TIDAK PERNAH benar-benar dibaca kembali oleh getter ini. Karena entity JPA/Hibernate ini
 * memakai anotasi pada level getter (property access), Hibernate memanggil getter — bukan membaca field
 * secara langsung — saat menentukan nilai kolom untuk INSERT/UPDATE maupun saat dirty-checking; akibatnya
 * nilai yang benar-benar disimpan ke kolom {@code sampai} di database SELALU merupakan hasil turunan dari
 * {@code mulai + jam}, bukan nilai yang mungkin pernah diisi manual. Kolom {@code sampai} pada tabel
 * {@code payroll.waktu_shift} secara praktis adalah data turunan/cache yang selalu disinkronkan ulang,
 * bukan sumber kebenaran independen — mirip pola "kalkulasi waktu rawan representasi" yang juga ditemukan
 * pada {@code DetailJenisShiftPegawai.getJarakMulai()} (baris kode terpisah, sedang diperbaiki sesi lain),
 * meskipun di sini bentuknya berbeda: bukan bug representasi angka, melainkan getter yang membuat field
 * turunannya tidak pernah benar-benar independen dari field sumbernya.</p>
 *
 * <p><b>Catatan lintas-hari (overnight):</b> {@link #getSampai()} memakai {@code Calendar.set(SECOND, ...)}
 * untuk menambahkan durasi dalam detik ke {@link #getMulai()} — mekanisme ini menangani overflow
 * menit/jam/HARI dengan benar (Calendar API menormalisasi otomatis), sehingga shift yang melewati tengah
 * malam (mis. mulai 22:00, durasi 10 jam) menghasilkan {@code sampai} pada tanggal berikutnya dengan jam
 * yang benar (08:00). Namun karena kolom ini dipetakan {@code @Temporal(TemporalType.TIME)} (hanya
 * komponen jam yang dipersistensikan, komponen tanggal dibuang oleh Hibernate), informasi "lintas hari"
 * ini HILANG saat disimpan ke database — konsumen di sisi lain ({@code DetailJenisShiftPegawai.getJumlah()}
 * dan {@code getJumlahSecond()}) memakai algoritma normalisasi tanggal + deteksi {@code end.before(start)}
 * yang TERPISAH dan tidak identik dengan logika di sini untuk merekonstruksi durasi lintas-hari dari
 * pasangan jam TIME murni. Kedua kelas ini secara independen "menebak ulang" apakah suatu shift melewati
 * tengah malam berdasarkan representasi jam semata — konsisten satu sama lain untuk kasus wajar, tetapi
 * tidak ada satu sumber kebenaran tunggal untuk keputusan "shift ini overnight atau tidak".</p>
 *
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "waktu_shift")
public class WaktuShift extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} antar build. Nilai ini
	 * warisan generator hbm2java dan sengaja tidak diubah kecuali struktur field berubah tak-kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris waktu shift ini, di-generate database (identity) — lihat {@link #getId()}. */
	private Long id;

	/** Nama/username user yang terakhir membuat atau mengubah baris ini (field audit shadow). */
	private String oleh;

	/** ID user yang terakhir membuat atau mengubah baris ini (field audit shadow), pasangan {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan ID user yang terakhir mengubah baris ini.
	 *
	 * @return ID user audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID user audit ({@link #olehId}). Guard: nilai {@code null} atau string kosong/hanya-spasi
	 * diabaikan diam-diam agar baris audit yang sudah ada tidak tertimpa kosong oleh pemanggil yang lupa
	 * menyertakan identitas user (mis. proses batch/background). Pola identik dengan setter audit pada
	 * {@link DetailJenisShiftPegawai#setOlehId(String)} dan {@link JenisShiftPegawai#setOlehId(String)}.
	 *
	 * @param olehId ID user yang melakukan perubahan; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama/username user audit ({@link #oleh}). Guard sama seperti {@link #setOlehId(String)}:
	 * nilai {@code null} atau kosong/hanya-spasi diabaikan diam-diam supaya nilai audit lama tidak
	 * tertimpa kosong.
	 *
	 * @param oleh username user yang melakukan perubahan; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan username user yang terakhir mengubah baris ini.
	 *
	 * @return username user audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus-hidup JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate tepat sebelum statement
	 * {@code UPDATE} dikirim ke database untuk entity ini. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi
	 * {@link #tanggal_dirubah} dengan waktu saat ini, memastikan jejak "kapan terakhir diubah" selalu
	 * konsisten tanpa bergantung pada pemanggil yang mengeset manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Timestamp perubahan terakhir baris ini (field audit shadow). Diinisialisasi ke waktu saat object
	 * dibuat di JVM dan ditimpa otomatis oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Biasanya tidak perlu dipanggil langsung karena
	 * {@link #onUpdate()} sudah mengelolanya otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini.
	 *
	 * @return timestamp audit, dipetakan sebagai {@code TIMESTAMP}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris waktu shift ini untuk log/debug/tampilan combo-box, berformat
	 * {@code "<id>-<nama>"}. Berbeda dari kebanyakan entity lain di package ini, method ini mengakses
	 * field {@code id}/{@code nama} secara langsung (bukan lewat getter), sehingga tidak memicu efek
	 * samping apa pun — murni operasi baca.
	 *
	 * @return string deskriptif baris waktu shift, tidak pernah {@code null} secara struktur (meski
	 *         {@code nama} sendiri boleh {@code null} dan akan tampil sebagai literal "null" dalam
	 *         konkatenasi string)
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat/identifier waktu shift ini untuk pencarian cepat; lihat {@link #getKode()} untuk normalisasi. */
	private String kode;

	/**
	 * Jam mulai shift, dipetakan sebagai kolom {@code TIME} (komponen tanggal diabaikan Hibernate).
	 * Bila {@code null}, {@link #getMulai()} men-default-kannya ke pukul 08:00:00 dan MENULIS BALIK
	 * default itu ke field ini.
	 */
	private Date mulai;

	/**
	 * Jam selesai shift — <b>tidak pernah benar-benar dipakai sebagai sumber kebenaran</b>: getter
	 * {@link #getSampai()} SELALU menghitung ulang dan menimpa field ini dari {@link #mulai} +
	 * {@link #jam} setiap kali dipanggil (lihat javadoc kelas dan javadoc getter). Field ini pada
	 * praktiknya hanya berfungsi sebagai cache hasil kalkulasi terakhir.
	 */
	private Date sampai;

	/** Durasi shift dalam jam (desimal, boleh pecahan mis. 7.5); default efektif 8.0 — lihat {@link #getJam()}. */
	private Double jam;

	/** Nama tampilan waktu shift ini, mis. "Shift Pagi". Wajib diisi (kolom {@code nullable = false}). */
	private String nama;

	/** Keterangan bebas (opsional) untuk waktu shift ini. */
	private String keterangan;

	/** Flag status aktif/nonaktif waktu shift ini; default efektif {@code true} — lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dibutuhkan oleh Hibernate untuk instansiasi entity lewat
	 * reflection saat hydrating hasil query, serta dipakai kode aplikasi (mis.
	 * {@code ais.action.master.payroll.WaktuShiftAction#onAdd(Event)}) saat membuat baris baru sebelum
	 * field-nya diisi pengguna.
	 */
	public WaktuShift() {
	}

	/**
	 * Mengembalikan primary key baris waktu shift ini.
	 *
	 * @return ID baris, {@code null} untuk instance yang belum dipersistensikan; kolom identity
	 *         database ({@code insertable = false}) sehingga tidak boleh diisi manual saat insert
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris. Karena kolom database bersifat {@code insertable = false} (identity), setter ini
	 * pada praktiknya hanya relevan untuk keperluan Hibernate hydration/testing, bukan untuk menetapkan ID
	 * baru secara manual sebelum insert.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat waktu shift ini, dinormalisasi (di-trim) saat dibaca.
	 *
	 * <p><b>Catatan:</b> berbeda dari kebanyakan getter string di codebase AIS yang mengembalikan
	 * {@code null} bila field belum diisi, getter ini mengembalikan string kosong ({@code ""}) — baik
	 * untuk field {@code null} maupun field yang sudah berisi string kosong — sehingga pemanggil tidak
	 * perlu null-check terpisah, tetapi juga tidak bisa membedakan "kode belum pernah diisi" dari "kode
	 * sengaja diisi string kosong".</p>
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (fallback ke {@code ""})
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ?  "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat waktu shift ini. Nilai disimpan apa adanya (tanpa trim) — normalisasi hanya
	 * terjadi saat dibaca lewat {@link #getKode()}.
	 *
	 * @param kode kode baru, boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama tampilan waktu shift ini, di-trim saat dibaca.
	 *
	 * @return nama ter-trim, atau {@code null} bila field {@code nama} belum pernah diisi (berbeda dari
	 *         {@link #getKode()} yang fallback ke string kosong, getter ini tetap mengembalikan
	 *         {@code null} apa adanya)
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama tampilan waktu shift ini. Kolom {@code nama} bersifat wajib (not-null) di database,
	 * jadi baris tanpa nama akan gagal disimpan.
	 *
	 * @param nama nama baru; disimpan apa adanya, di-trim hanya saat dibaca lewat {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk waktu shift ini.
	 *
	 * @return teks keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk waktu shift ini.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/nonaktif waktu shift ini, dengan fallback ke {@code true} (aktif) bila
	 * field belum pernah diset — baris baru dianggap aktif secara default. Berbeda dari
	 * {@link #getMulai()} pada kelas ini dan {@code JenisShiftPegawai.getJumlahShift()} di entity lain,
	 * getter ini TIDAK menulis balik nilai fallback ke field {@link #aktif}.
	 *
	 * @return {@code true} bila aktif, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif/nonaktif waktu shift ini.
	 *
	 * @param aktif status baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan jam mulai shift ini, dipetakan sebagai kolom {@code TIME} (hanya komponen jam yang
	 * dipersistensikan, komponen tanggal diabaikan Hibernate).
	 *
	 * <p><b>Efek samping (getter destruktif, fallback tertulis):</b> bila field {@link #mulai} masih
	 * {@code null} (baris baru yang belum pernah diisi penggunanya), method ini MEMBANGUN nilai default
	 * pukul 08:00:00 (berbasis kalender "sekarang" dari {@code WaktuUtil.getCalendar()}, hanya komponen
	 * jam/menit/detik yang di-set eksplisit — komponen tanggal ikut kalender saat itu) dan MENULIS BALIK
	 * hasilnya ke field {@link #mulai}, sehingga pemanggilan berikutnya tidak lagi mengenali baris ini
	 * sebagai "belum diisi". Pola ini identik dengan {@code JenisShiftPegawai.getJumlahShift()}: fallback
	 * bukan sekadar nilai kembalian sesaat, melainkan menjadi nilai permanen field sejak pemanggilan
	 * pertama dalam sesi Hibernate yang sama.</p>
	 *
	 * @return jam mulai shift, tidak pernah {@code null} (fallback 08:00:00 bila belum pernah diisi)
	 */
	@Temporal(TemporalType.TIME)
	public Date getMulai() {
		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 8);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			mulai = calendar.getTime();
		}
		return mulai;
	}

	/**
	 * Mengisi jam mulai shift secara langsung. Nilai ini akan bertahan (tidak ditimpa) selama tidak
	 * {@code null} — berbeda dengan {@link DetailJenisShiftPegawai#setMulai(Date)} yang bisa ditimpa
	 * balik oleh sumber kanonik lain, setter di sini murni menyimpan nilai yang diberikan.
	 *
	 * @param mulai jam mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Menghitung ulang dan mengembalikan jam selesai shift ini dari {@link #getMulai()} +
	 * {@link #getJam()} jam, dipetakan sebagai kolom {@code TIME}.
	 *
	 * <p><b>Efek samping (getter destruktif TANPA SYARAT — bukan sekadar fallback).</b> Berbeda dari
	 * kebanyakan getter destruktif lain di codebase AIS yang hanya menimpa field ketika field tersebut
	 * masih {@code null} atau ketika suatu kondisi terpenuhi, method ini MENGHITUNG ULANG DAN MENIMPA
	 * {@link #sampai} SETIAP KALI dipanggil, tanpa syarat apa pun — nilai yang di-set lewat
	 * {@link #setSampai(Date)}, maupun nilai yang dimuat dari kolom {@code sampai} di database saat
	 * hydration, TIDAK PERNAH benar-benar dikembalikan oleh getter ini. Karena entity ini memakai
	 * anotasi Hibernate pada level getter (property access), Hibernate memanggil getter ini — bukan
	 * membaca field secara langsung — untuk menentukan nilai yang ditulis ke kolom {@code sampai} saat
	 * INSERT/UPDATE. Akibatnya kolom {@code sampai} pada tabel {@code payroll.waktu_shift} SELALU berisi
	 * hasil turunan {@code mulai + jam} pada saat baris terakhir di-flush — kolom ini praktis adalah
	 * data cache/turunan, bukan sumber kebenaran independen. Lihat javadoc kelas untuk penjelasan lebih
	 * lengkap dan perbandingannya dengan penanganan overnight di {@code DetailJenisShiftPegawai}.</p>
	 *
	 * <p><b>Algoritma.</b> Durasi {@link #getJam()} (jam desimal) dikonversi ke detik dengan pembulatan
	 * ke bilangan bulat terdekat ({@code (int) (0.5 + jam * 3600)} — idiom pembulatan manual, ekuivalen
	 * {@code Math.round} untuk nilai non-negatif), lalu ditambahkan ke komponen {@code SECOND} kalender
	 * yang berbasis {@link #getMulai()} lewat {@code Calendar.set(SECOND, current + seconds)}. Karena
	 * {@code Calendar} menormalisasi overflow field secara otomatis (detik berlebih naik ke menit, menit
	 * ke jam, jam ke hari), penambahan durasi besar (mis. shift 10 jam dari mulai 22:00) menghasilkan
	 * tanggal berikutnya dengan jam yang benar — TIDAK ada bug off-by-one pada aritmetika ini sendiri.
	 * Namun karena kolom dipetakan {@code @Temporal(TIME)}, komponen tanggal hasil normalisasi tersebut
	 * DIBUANG saat persist — lihat javadoc kelas untuk implikasinya pada deteksi shift lintas-hari di
	 * kelas lain.</p>
	 *
	 * @return jam selesai shift, hasil hitungan {@link #getMulai()} + {@link #getJam()} jam, tidak
	 *         pernah {@code null}
	 */
	@Temporal(TemporalType.TIME)
	public Date getSampai() {
		int seconds = (int) (0.5 + (getJam() * 60 * 60));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getMulai());
		calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + seconds);
		sampai = calendar.getTime();
		return sampai;
	}

	/**
	 * Mengisi jam selesai shift secara manual. <b>Peringatan:</b> nilai ini TIDAK PERNAH benar-benar
	 * dipertahankan — {@link #getSampai()} akan menghitung ulang dan menimpanya tanpa syarat pada
	 * pemanggilan berikutnya (lihat javadoc getter), termasuk saat Hibernate memanggil getter ini sendiri
	 * untuk menentukan nilai yang di-flush ke database. Setter ini pada praktiknya tidak berpengaruh
	 * apa pun terhadap nilai yang akhirnya tersimpan.
	 *
	 * @param sampai jam selesai yang diinginkan; akan selalu ditimpa oleh {@link #getSampai()}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan durasi shift dalam jam (desimal), dengan fallback ke 8.0 bila field belum pernah
	 * diset. Berbeda dari {@link #getMulai()}, getter ini TIDAK menulis balik nilai fallback ke field
	 * {@link #jam} — nilai {@code null} pada field tetap dibiarkan {@code null} di memori/DB, hanya
	 * nilai kembaliannya yang disamarkan jadi 8.0. Nilai ini dipakai langsung oleh {@link #getSampai()}
	 * untuk menghitung jam selesai.
	 *
	 * @return durasi shift dalam jam, tidak pernah {@code null}
	 */
	public Double getJam() {
		return jam == null ? 8.0 : jam;
	}

	/**
	 * Mengisi durasi shift dalam jam (desimal). Nilai ini dipakai oleh {@link #getSampai()} pada
	 * pemanggilan berikutnya untuk menghitung ulang jam selesai — lihat javadoc getter tersebut.
	 *
	 * @param jam durasi baru dalam jam
	 */
	public void setJam(Double jam) {
		this.jam = jam;
	}

}
