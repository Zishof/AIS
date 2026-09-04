package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.json.JSONObject;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>pengajuan pegawai</b> — satu baris tabel {@code public.pengajuan_pegawai}, yaitu satu
 * permohonan yang diajukan seorang pegawai untuk suatu rentang tanggal dan menunggu persetujuan.
 *
 * <h2>Nama class vs nama modul</h2>
 *
 * <p>Nama class-nya generik, tetapi modulnya spesifik. Di menu aplikasi entri yang mengarah ke
 * {@code /pages/master/pengajuan_pegawai.zul} berjudul <b>"Pengajuan Lembur &amp; Masuk Hari Libur
 * Pegawai"</b> (grup induk "Pengajuan", bersebelahan dengan "Pengajuan Izin &amp; Cuti Pegawai"
 * yang ditangani modul lain, {@code CutiDanIzinAction}). Judul jendela tambah/ubah dan istilah
 * internal controller-nya tetap memakai nama lama "Pengajuan Pegawai". Jadi: <i>izin/cuti</i>
 * bukan urusan class ini; yang dilayani di sini adalah lembur, masuk hari libur, penugasan, dan
 * jenis-jenis pengajuan lain yang didaftarkan di master {@link JenisPengajuanPegawai}.</p>
 *
 * <h2>Posisi dalam hierarki</h2>
 *
 * <p>{@code PengajuanPegawai} &rarr; {@link ais.database.model.sop.DataSop} &rarr;
 * {@link ais.database.model.GeneralValueObject}. {@code DataSop} adalah POJO abstrak yang hanya
 * mewajibkan pasangan {@code get/setDisposisiSop} sehingga entity ini bisa dipasang ke mesin alur
 * SOP.</p>
 *
 * <p><b>Penting — bukan bug:</b> {@link ais.database.model.GeneralValueObject} <b>tidak</b>
 * dianotasi {@code @Entity} maupun {@code @MappedSuperclass}; ia POJO abstrak biasa dan Hibernate
 * <b>tidak</b> memetakan properti apa pun miliknya. Karena itu field jejak audit
 * {@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}, dan {@link #id} <b>harus</b>
 * dideklarasikan ulang di setiap entity turunan seperti class ini. Pengulangan itu keharusan
 * teknis, jangan "dirapikan" dengan memindahkannya ke induk — kolomnya akan hilang dari peta
 * Hibernate.</p>
 *
 * <h2>Relasi</h2>
 *
 * <ul>
 * <li>{@link #getJenisPengajuanPegawai()} &rarr; {@link JenisPengajuanPegawai} — master jenis
 * pengajuan. Master inilah yang menentukan hilirnya: flag {@code masukPresensi},
 * {@code masukLembur}, {@code dapatKonsumsi}, format {@code NomorSurat} untuk penomoran, dan
 * daftar {@link KelompokParameterTambahanPengajuanPegawai} yang membentuk isian dinamis.</li>
 * <li>{@link #getPegawai()} &rarr; {@link Pegawai} — pegawai yang pengajuannya dibuat (subjek).</li>
 * <li>{@link #getSatuanKerja()} &rarr; {@link SatuanKerja} — <b>selalu diturunkan</b> dari satuan
 * kerja pegawai bila ada, lihat catatan di getter-nya.</li>
 * <li>{@link #getSatuanKerjaPengaju()} &rarr; {@link SatuanKerja} — unit yang mengajukan; boleh
 * berbeda dari satuan kerja pegawai (mis. unit lain yang meminta bantuan tenaga), fallback ke
 * {@link #getSatuanKerja()} bila kosong.</li>
 * <li>{@link #getDisposisiSop()} &rarr; {@link DisposisiSop} — instance alur SOP yang menangani
 * pengajuan ini, bila jenisnya memang dialirkan lewat SOP.</li>
 * <li>{@link #getDiajukanOleh()} dan {@link #getDisetujuiOleh()} &rarr; {@link Tbmuser} — akun
 * pengaju dan akun penyetuju (yang terakhir hanya terisi pada mode persetujuan manual).</li>
 * </ul>
 *
 * <h2>Dua mode persetujuan yang saling eksklusif</h2>
 *
 * <p>Ini bagian paling tidak obvious dari class ini. Ada dua jalur persetujuan dan yang menentukan
 * jalur mana yang berlaku adalah ada-tidaknya {@link #getDisposisiSop()}:</p>
 *
 * <ol>
 * <li><b>Manual oleh atasan</b> — bila {@code disposisiSop} kosong. Layar daftar merender kotak
 * centang "Setujui" hanya untuk pengguna yang merupakan atasan langsung/atasan jabatan pegawai
 * bersangkutan dan bukan pegawai itu sendiri. Mencentangnya mengisi {@link #setSetujui(Boolean)},
 * {@link #setSetujuiTanggal(Date)}, dan {@link #setDisetujuiOleh(Tbmuser)}.</li>
 * <li><b>Alur SOP</b> — bila {@code disposisiSop} terisi. Sejak saat itu {@link #getSetujui()} dan
 * {@link #getAktif()} <b>berhenti menjadi kolom biasa dan menjadi nilai turunan</b> dari disposisi:
 * disetujui bila langkah "setuju" sudah tercapai, dan tidak aktif bila disposisinya dimatikan atau
 * berakhir di langkah penolakan. Kotak centang manual tidak dirender lagi.</li>
 * </ol>
 *
 * <p>Konsekuensinya untuk pembaca kode: <b>jangan</b> membaca kolom {@code setujui}/{@code aktif}
 * dari database lalu menganggapnya kebenaran akhir tanpa melewati getter di class ini — kecuali
 * memang sengaja, seperti yang dilakukan modul payroll yang menyaring lewat kriteria SQL
 * {@code setujui = true}. Nilai kolomnya baru sinkron setelah getter dipanggil pada instance yang
 * masih managed (lihat "Getter yang menulis" di bawah).</p>
 *
 * <h2>Isian dinamis (parameter tambahan)</h2>
 *
 * <p>Selain kolom tetap, tiap jenis pengajuan bisa punya sekumpulan isian yang didefinisikan admin
 * lewat {@link ParameterTambahan} dan {@link KelompokParameterTambahanPengajuanPegawai}. Jawabannya
 * tidak disimpan sebagai tabel anak melainkan diserialkan ke <b>dua kolom {@code text}</b> yang
 * ditulis bersamaan oleh {@link #populateParameterTambahan(List)}:</p>
 *
 * <ul>
 * <li>{@link #getParameterTambahan()} — versi <b>berlabel</b>, 7 ruas per baris, untuk ditampilkan
 * apa adanya (dibongkar {@link #ambilDataParameterTambahan()});</li>
 * <li>{@link #getParameterTambahanInds()} — versi <b>ber-ID</b>, 4 ruas per baris, untuk mengisi
 * ulang formulir dan mencocokkan nilai per parameter. Justru versi inilah yang dipakai jalur
 * render layar, render grid, dan cetak laporan — masing-masing mem-parse-nya sendiri secara inline
 * (logika bongkar terduplikasi di beberapa tempat, bukan lewat satu method bersama).</li>
 * </ul>
 *
 * <p>Format kedua string dijelaskan rinci pada {@link #populateParameterTambahan(List)}.</p>
 *
 * <h2>Keterangan harian</h2>
 *
 * <p>Bila rentang pengajuan lebih dari satu hari ({@link #getJumlahHari()} &gt; 1), layar meminta
 * keterangan kegiatan <b>per hari</b> dan menyimpannya sebagai objek JSON di
 * {@link #getKeteranganBanyak()} dengan kunci indeks hari ({@code "0"}, {@code "1"}, ...). Untuk
 * pengajuan satu hari yang dipakai tetap {@link #getKeterangan()} biasa; kunci {@code "0"}
 * disinkronkan dua arah dengan kolom keterangan itu.</p>
 *
 * <h2>Penomoran</h2>
 *
 * <p>{@link #getKode()} adalah "No. Agenda" yang tampil ke pengguna; ia dibentuk lapisan UI dari
 * {@link #getIndex()} (nomor urut internal per jenis pengajuan) memakai format {@code NomorSurat}
 * milik jenis pengajuan, dengan aturan reset per tahun/bulan/tanggal. {@link #getTahun()} dan
 * {@link #getBulan()} ada semata-mata untuk menopang aturan reset itu — tidak ada input UI
 * untuknya. <b>Kuirk operasional:</b> renderer daftar melakukan backfill kode untuk baris yang
 * kodenya masih kosong dan menyimpannya, sehingga sekadar membuka layar daftar dapat menulis ke
 * database dan menaikkan penghitung nomor surat.</p>
 *
 * <h2>Konsumen hilir</h2>
 *
 * <p>Pengajuan yang sudah disetujui menjadi masukan modul kepegawaian/payroll: proses absensi
 * (bila jenisnya {@code masukPresensi}), laporan lembur ({@code masukLembur}), laporan konsumsi
 * dan konsumsi penugasan ({@code dapatKonsumsi}), rekap kehadiran, serta dasbor kehadiran. Semua
 * menyaring dengan {@code setujui = true}.</p>
 *
 * <h2>Pengelompokan anggota class ini</h2>
 *
 * <ol>
 * <li><b>Jejak audit</b> (deklarasi ulang wajib): {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}.</li>
 * <li><b>Identitas &amp; deskripsi</b>: {@link #getKode()}, {@link #getIndex()},
 * {@link #getNama()}, {@link #getKeterangan()}, {@link #getKeteranganBanyak()},
 * {@link #toString()}.</li>
 * <li><b>Rentang waktu</b>: {@link #getWaktu()}, {@link #getWaktuSampai()},
 * {@link #getJumlahHari()}, {@link #getTahun()}, {@link #getBulan()}.</li>
 * <li><b>Relasi</b>: jenis, pegawai, dua satuan kerja, disposisi SOP, dua {@link Tbmuser}.</li>
 * <li><b>Status</b>: {@link #getSetujui()}, {@link #getSetujuiTanggal()}, {@link #getAktif()}.</li>
 * <li><b>Isian dinamis</b>: {@link #getParameterTambahan()}, {@link #getParameterTambahanInds()},
 * {@link #populateParameterTambahan(List)}, {@link #ambilDataParameterTambahan()}.</li>
 * </ol>
 *
 * <h2>Getter yang menulis — daftar hasil verifikasi atas file ini</h2>
 *
 * <p>Class ini padat "getter yang tidak sekadar membaca". Karena entity dianotasi
 * {@code dynamicUpdate = true}, membaca getter-getter berikut pada instance yang masih terikat
 * session dapat menghasilkan {@code UPDATE} saat flush <b>tanpa ada aksi simpan dari
 * pengguna</b>:</p>
 *
 * <ul>
 * <li>Menulis field terpetakan dari nilai turunan: {@link #getNama()} (menyalin nama jenis bila
 * kosong), {@link #getTahun()}, {@link #getBulan()} (mengisi periode berjalan bila null),
 * {@link #getSatuanKerja()} (<b>selalu</b> menimpa dari satuan kerja pegawai bila ada),
 * {@link #getSatuanKerjaPengaju()} (mengisi dari satuan kerja bila null),
 * {@link #getJumlahHari()} (<b>selalu</b> menghitung ulang), {@link #getSetujui()} dan
 * {@link #getAktif()} (menurunkan status dari disposisi), serta
 * {@link #getParameterTambahan()}/{@link #getParameterTambahanInds()} yang menormalkan
 * {@code null} menjadi string kosong.</li>
 * <li>Menulis balik referensi relasi hasil {@code check()}: {@link #getJenisPengajuanPegawai()},
 * {@link #getPegawai()}, {@link #getSatuanKerja()}, {@link #getDisposisiSop()},
 * {@link #getDiajukanOleh()}, {@link #getDisetujuiOleh()} — enam relasi, semuanya. Pola dan
 * biayanya diuraikan lengkap di {@link ais.database.model.GeneralValueObject#check(Object)}.</li>
 * <li><b>Tidak</b> menulis apa pun (nilai default hanya dikembalikan, tidak disimpan):
 * {@link #getKode()}, {@link #getKeterangan()}, {@link #getWaktu()}, {@link #getWaktuSampai()},
 * {@link #getKeteranganBanyak()}, {@link #getIndex()}, {@link #getSetujuiTanggal()}, dan getter
 * jejak audit.</li>
 * <li><b>Getter destruktif: tidak ada.</b> Tidak ada getter di file ini yang mengosongkan atau
 * menghapus data setelah dibaca.</li>
 * <li><b>Getter yang menutup sesi Hibernate: tidak ada.</b> File ini tidak menyentuh
 * {@code Session}, {@code Criteria}, atau {@code HibernateUtil} sama sekali. Sesi tetap bisa
 * dibuka-tutup secara <i>tidak langsung</i> lewat {@code check()} (yang membuka sesi sendiri saat
 * harus memuat ulang proxy) dan lewat {@link LampiranLain#ambil(Long, String)} di dalam
 * {@link #populateParameterTambahan(List)}.</li>
 * <li><b>Flag satu arah:</b> {@link #getAktif()} hanya pernah menurunkan nilai menjadi
 * {@code false} — tidak ada cabang yang mengembalikannya ke {@code true}; nilai {@code null}
 * dibaca sebagai {@code true}. Sebaliknya {@link #getSetujui()} <b>dua arah</b>: ia menyalin
 * ulang status disposisi apa adanya, jadi bisa kembali {@code false} bila langkah setuju dibatalkan
 * di alur SOP.</li>
 * <li><b>Setter yang menolak pengosongan:</b> {@link #setOleh(String)},
 * {@link #setOlehId(String)}, dan {@link #setDisposisiSop(DisposisiSop)} mengabaikan argumen
 * kosong/null, sehingga nilai yang sudah terisi tidak bisa dihapus lewat setter.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sop.DataSop
 * @see JenisPengajuanPegawai
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pengajuan_pegawai")
public class PengajuanPegawai extends DataSop {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya persis sama dengan milik {@link JenisPengajuanPegawai} — sisa penggandaan berkas
	 * saat entity ini dibuat. Tidak berbahaya (serialVersionUID hanya dibandingkan antar versi
	 * class yang sama), tetapi jangan dijadikan penanda identitas class.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama; dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String oleh;

	/** ID/username pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String olehId;

	/**
	 * @return ID pengguna terakhir yang mengubah baris ini; boleh {@code null} untuk baris lama
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Menolak pengosongan:</b> argumen {@code null} atau hanya spasi diabaikan diam-diam,
	 * sehingga jejak audit yang sudah terisi tidak bisa dihapus lewat setter ini.</p>
	 *
	 * @param olehId ID/username pengguna; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Menolak pengosongan</b>, sama seperti {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini; boleh {@code null} untuk baris lama
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum {@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)}/{@link #setOlehId(String)}
	 * dari pengguna sesi berjalan. Dipanggil Hibernate, bukan oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir; sudah diinisialisasi ke "sekarang" saat objek dibuat sehingga baris
	 * baru tetap punya nilai walau {@link #onUpdate()} belum pernah jalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<nama>"}, dipakai combobox dan log.
	 *
	 * <p><b>Kuirk:</b> keduanya dibaca dari <b>field</b>, bukan lewat {@link #getNama()}. Jadi
	 * selama {@link #getNama()} belum pernah dipanggil, bagian nama bisa tampil {@code null}
	 * meski jenis pengajuannya sudah terisi (getter itulah yang menyalin nama dari jenis).</p>
	 *
	 * @return teks {@code id-nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Judul pengajuan; bila dibiarkan kosong akan disalin dari nama jenis pengajuan. */
	private String nama;

	/** Keterangan/alasan pengajuan untuk kasus satu hari. */
	private String keterangan;

	/** Keterangan kegiatan per hari dalam bentuk objek JSON, untuk pengajuan lebih dari satu hari. */
	private String keteranganBanyak;

	/** Awal rentang pengajuan. */
	private Date waktu;

	/** Akhir rentang pengajuan (inklusif). */
	private Date waktuSampai;

	/** Master jenis pengajuan yang menentukan perilaku hilir dan isian dinamisnya. */
	private JenisPengajuanPegawai jenisPengajuanPegawai;

	/** Pegawai yang menjadi subjek pengajuan. */
	private Pegawai pegawai;

	/** Satuan kerja pegawai; nilainya diturunkan, bukan diisi pengguna. */
	private SatuanKerja satuanKerja;

	/** Satuan kerja yang mengajukan; boleh berbeda dari {@link #satuanKerja}. */
	private SatuanKerja satuanKerjaPengaju;

	/** Isian dinamis versi berlabel (7 ruas per baris). */
	private String parameterTambahan;

	/** Isian dinamis versi ber-ID (4 ruas per baris). */
	private String parameterTambahanInds;

	/** Instance alur SOP yang menangani pengajuan ini, bila jenisnya memakai SOP. */
	private DisposisiSop disposisiSop;

	/** Tahun pembuatan; penopang aturan reset penomoran, tidak ada input UI-nya. */
	private Integer tahun;

	/** Bulan pembuatan (1-12); penopang aturan reset penomoran, tidak ada input UI-nya. */
	private Integer bulan;

	/** Jumlah hari rentang pengajuan; nilai turunan yang selalu dihitung ulang. */
	private Integer jumlahHari;

	/** "No. Agenda" hasil format nomor surat. */
	private String kode;

	/** Nomor urut internal per jenis pengajuan, bahan pembentuk {@link #kode}. */
	private Long index;

	/** Tanggal persetujuan manual oleh atasan. */
	private Date setujuiTanggal;

	/** Akun pengguna yang mengajukan. */
	private Tbmuser diajukanOleh;

	/**
	 * Akun pengguna yang menyetujui.
	 *
	 * <p><b>Perhatikan ejaan:</b> nama field-nya {@code disetujiOleh} (kurang huruf "u"), tetapi
	 * nama properti Hibernate mengikuti getter/setter, yaitu {@code disetujuiOleh}; kolom
	 * databasenya sendiri {@code disetuji_oleh}. Ketiganya sengaja dibiarkan agar tidak memecah
	 * kueri dan skema yang sudah ada.</p>
	 */
	private Tbmuser disetujiOleh;

	/** Status disetujui; menjadi nilai turunan begitu {@link #disposisiSop} terisi. */
	private Boolean setujui;

	/** Status aktif; menjadi nilai turunan begitu {@link #disposisiSop} terisi. */
	private Boolean aktif;

	/** Konstruktor kosong yang dibutuhkan Hibernate dan lapisan UI. */
	public PengajuanPegawai() {
	}

	/**
	 * @return kunci utama baris; {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id kunci utama; normalnya diisi Hibernate dari sequence identity
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * "No. Agenda" pengajuan, hasil format {@code NomorSurat} milik jenis pengajuan atas
	 * {@link #getIndex()}.
	 *
	 * <p>Dinormalkan saat dibaca: string kosong/hanya spasi dilaporkan sebagai {@code null} agar
	 * lapisan UI bisa membedakan "belum bernomor" dari "bernomor". Normalisasi ini <b>tidak</b>
	 * ditulis balik ke field.</p>
	 *
	 * @return kode ter-trim, atau {@code null} bila belum bernomor
	 */
	@Column(name = "kode")
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/**
	 * @param kode nomor agenda hasil pembentukan di lapisan UI
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Judul pengajuan.
	 *
	 * <p><b>Getter yang menulis:</b> bila nama masih kosong dan jenis pengajuan sudah terisi, nama
	 * <b>disalin dari nama jenis pengajuan dan disimpan ke field terpetakan</b>. Pada instance yang
	 * masih managed, hal itu bisa ikut ter-flush sebagai {@code UPDATE} tanpa aksi simpan pengguna.
	 * Efek samping tambahan: pemanggilan {@link #getJenisPengajuanPegawai()} di dalamnya juga
	 * meresolusi proxy lazy lewat {@code check()}.</p>
	 *
	 * @return judul ter-trim, atau {@code null} bila kosong dan jenis pengajuan belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if ((nama == null || nama.isEmpty()) && getJenisPengajuanPegawai() != null) {
			nama = getJenisPengajuanPegawai().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama judul pengajuan; boleh dibiarkan kosong agar diisi dari nama jenis pengajuan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @param index nomor urut internal; dihitung lapisan UI dari jumlah baris sejenis dengan aturan
	 *         reset per tahun/bulan/tanggal sesuai konfigurasi nomor surat
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * @return nomor urut internal per jenis pengajuan, bahan pembentuk {@link #getKode()}; boleh
	 *         {@code null} untuk baris yang belum pernah dinomori
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Keterangan/alasan pengajuan.
	 *
	 * <p>Untuk pengajuan lebih dari satu hari, keterangan per hari disimpan terpisah di
	 * {@link #getKeteranganBanyak()}; nilai di sini disinkronkan dengan keterangan hari pertama
	 * (kunci JSON {@code "0"}).</p>
	 *
	 * @return keterangan, atau string kosong bila belum diisi — tidak pernah {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * @param keterangan alasan/uraian pengajuan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Awal rentang pengajuan.
	 *
	 * <p>Bila belum diisi, yang dikembalikan adalah waktu "sekarang" — nilai itu <b>tidak</b>
	 * disimpan ke field, jadi memanggil getter ini berulang pada objek baru menghasilkan stempel
	 * yang berbeda-beda. Formulir tambah menetapkan sendiri jam awal harinya.</p>
	 *
	 * @return awal rentang; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * @param waktu awal rentang pengajuan
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Master jenis pengajuan (lembur, masuk hari libur, penugasan, dan seterusnya).
	 *
	 * <p>Jenis inilah yang menentukan flag hilir ({@code masukPresensi}, {@code masukLembur},
	 * {@code dapatKonsumsi}), format penomoran surat, dan daftar kelompok parameter tambahan yang
	 * dirender di formulir.</p>
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code check()} ditugaskan kembali ke field — proxy lazy
	 * diresolusi lewat cache atau, bila perlu, lewat sesi Hibernate baru.</p>
	 *
	 * @return jenis pengajuan yang sudah teresolusi, atau {@code null} bila memang belum dipilih
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengajuan_pegawai")
	public JenisPengajuanPegawai getJenisPengajuanPegawai() {
		jenisPengajuanPegawai = check(jenisPengajuanPegawai);
		return jenisPengajuanPegawai;
	}

	/**
	 * @param jenisPengajuanPegawai master jenis pengajuan
	 */
	public void setJenisPengajuanPegawai(JenisPengajuanPegawai jenisPengajuanPegawai) {
		this.jenisPengajuanPegawai = jenisPengajuanPegawai;
	}

	/**
	 * Isian dinamis versi <b>ber-ID</b> — bentuk yang dipakai untuk memuat ulang formulir dan
	 * mencocokkan nilai per parameter.
	 *
	 * <p>Satu baris per parameter, dipisah {@code "\n"}, tiap baris terdiri atas empat ruas yang
	 * dipisah <code>&lt;=&gt;</code>:
	 * {@code idKelompok->idParameter <=> nilai <=> urlLampiran <=> keterangan}. Kunci
	 * {@code "idKelompok->idParameter"} itulah yang dicari jalur render layar, render grid, dan
	 * cetak laporan.</p>
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi string kosong dan disimpan ke
	 * field terpetakan.</p>
	 *
	 * @return string terenkode; tidak pernah {@code null}
	 * @see #populateParameterTambahan(List)
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menyetel isian dinamis versi ber-ID secara mentah.
	 *
	 * <p>Umumnya tidak dipanggil langsung — pakai {@link #populateParameterTambahan(List)} yang
	 * menyusun format terenkodenya dengan benar dan menjaga kedua kolom tetap sejalan.</p>
	 *
	 * @param parameterTambahanInds string terenkode versi ber-ID
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Membongkar {@link #getParameterTambahan()} (versi <b>berlabel</b>) menjadi daftar
	 * {@link CommonVO} siap tampil.
	 *
	 * <p>Kebalikan parsial dari {@link #populateParameterTambahan(List)}. Tiap baris dipecah pada
	 * <code>&lt;=&gt;</code> dan dipetakan:</p>
	 * <ul>
	 * <li>ruas ke-0 &rarr; {@code name} (label gabungan {@code namaKelompok->labelInputan}),</li>
	 * <li>ruas ke-1 &rarr; {@code name1} (nilai isian),</li>
	 * <li>ruas ke-2 &rarr; {@code name2} (URL lampiran),</li>
	 * <li>ruas ke-3 &rarr; {@code nomorUrut} (default 1 bila gagal di-parse),</li>
	 * <li>ruas ke-4 &rarr; {@code id} (ID {@link ParameterTambahan}, default 1 bila gagal),</li>
	 * <li>potongan pertama label sebelum {@code "->"} &rarr; {@code name5} (nama kelompok).</li>
	 * </ul>
	 *
	 * <p><b>Asimetri:</b> {@link #populateParameterTambahan(List)} menulis <b>tujuh</b> ruas,
	 * method ini hanya membaca lima yang pertama — {@code idKelompok} dan {@code keterangan}
	 * diabaikan.</p>
	 *
	 * <p><b>Kuirk urutan.</b> Karena {@code name5} <b>diisi</b> di sini, {@code CommonVO.compareTo}
	 * mengambil cabang perbandingan <i>string</i> {@code "namaKelompok nomorUrut"} — bukan cabang
	 * numerik yang dipakai entity lain yang membiarkan {@code name5} kosong (mis.
	 * {@code BiodataMahasiswa}). Akibatnya pengurutan di dalam satu kelompok bersifat leksikografis:
	 * nomor urut 10 muncul sebelum 2. Kelompoknya sendiri terurut menurut nama, bukan menurut
	 * nomor urut kelompok.</p>
	 *
	 * <p><b>Kuirk daftar hampa.</b> Pada string kosong, {@code split} tetap menghasilkan satu
	 * elemen kosong sehingga method mengembalikan satu {@link CommonVO} kosong (label dan nilai
	 * kosong, {@code id} = 1), bukan daftar kosong. Pemanggil yang menampilkan hasilnya langsung
	 * perlu menyaring baris tanpa label.</p>
	 *
	 * <p>Kegagalan {@code parseInt}/{@code parseLong} ditelan (tercatat lewat {@code ErrorAuditUtil})
	 * dan nilai default dipakai, jadi satu ruas rusak tidak membatalkan seluruh baris.</p>
	 *
	 * <p><b>Pemanggil:</b> bukan layar pengajuan itu sendiri, melainkan laporan konsumsi
	 * ({@code LaporanDapatKonsumsi}, {@code LaporanKonsumsiPenugasan}). Jalur render layar, grid,
	 * dan cetak justru mem-parse {@link #getParameterTambahanInds()} secara inline sendiri-sendiri
	 * — logika bongkar itu terduplikasi beberapa kali di luar class ini.</p>
	 *
	 * @return daftar {@link CommonVO} terurut; tidak pernah {@code null}, tetapi bisa berisi satu
	 *         elemen hampa untuk isian yang kosong
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengajuanPegawai.java:218");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengajuanPegawai.java:224");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Memanen nilai isian dinamis dari baris-baris formulir ZK dan menyerialkannya ke <b>dua</b>
	 * kolom {@code text}: {@link #setParameterTambahan(String)} (versi berlabel) dan
	 * {@link #setParameterTambahanInds(String)} (versi ber-ID).
	 *
	 * <p>Dipanggil dari {@code ais.action.master.helper.ParameterTambahanPengajuanPegawaiListener}
	 * di dua kesempatan: (1) saat <b>simpan formulir</b>, tepat sebelum entity disimpan controller,
	 * dan (2) setiap kali salah satu <b>input parameter berubah</b> saat formulir masih terbuka,
	 * sehingga entity selalu ter-update inkremental. Tiap {@link Row} membawa atribut
	 * {@code "parameterTambahan"} ({@link ParameterTambahan}),
	 * {@code "kelompokParameterTambahanPengajuanPegawai"}
	 * ({@link KelompokParameterTambahanPengajuanPegawai}), dan opsional {@code "keterangan"}
	 * (sebuah {@code Textbox}).</p>
	 *
	 * <p>Untuk tiap baris yang lengkap:</p>
	 * <ol>
	 * <li>nilai isian diambil lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)} yang
	 * tahu cara membaca komponen ZK sesuai tipe inputan;</li>
	 * <li>bila parameter mewajibkan lampiran, berkasnya dicari dengan
	 * {@code LampiranLain.ambil(getId(), idKelompok + "->" + idParameter)} — <b>kunci lampiran
	 * adalah ID pengajuan ini</b> — lalu URL unduhnya disertakan;</li>
	 * <li>dua baris dirangkai dan digabung dengan pemisah {@code "\n"}:
	 * <ul>
	 * <li>versi berlabel, <b>7 ruas</b>: {@code namaKelompok->labelInputan <=> nilai <=>
	 * urlLampiran <=> nomorUrut <=> idParameter <=> idKelompok <=> keterangan};</li>
	 * <li>versi ber-ID, <b>4 ruas</b>: {@code idKelompok->idParameter <=> nilai <=> urlLampiran
	 * <=> keterangan}.</li>
	 * </ul>
	 * </li>
	 * </ol>
	 *
	 * <p><b>Perilaku MENIMPA.</b> Kedua kolom ditulis ulang seluruhnya, bukan ditambahkan. Memanggil
	 * method ini dengan daftar baris yang tidak lengkap akan MENGHAPUS isian yang tidak ikut
	 * ditampilkan. Karena itu ada pengaman di awal: daftar {@code null} atau kosong menyebabkan
	 * method langsung kembali tanpa menyentuh apa pun.</p>
	 *
	 * <p><b>Efek samping lain:</b> {@code LampiranLain.ambil(...)} melakukan akses database
	 * (membuka sesinya sendiri) untuk tiap parameter yang mewajibkan lampiran, jadi biaya method
	 * ini naik seiring jumlah parameter berlampiran. Kegagalan per baris ditelan lewat
	 * {@code Common.tampilErrorJikaAdmin} sehingga satu baris rusak tidak membatalkan sisanya —
	 * tetapi baris itu juga <b>hilang</b> dari hasil karena perilaku menimpa di atas.</p>
	 *
	 * @param parameterRows daftar baris formulir ZK; bila {@code null} atau kosong method langsung
	 *         kembali tanpa mengubah apa pun (isian lama tetap aman)
	 * @see #ambilDataParameterTambahan()
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai = (KelompokParameterTambahanPengajuanPegawai) row
						.getAttribute("kelompokParameterTambahanPengajuanPegawai");
				if (parameterTambahan != null && kelompokParameterTambahanPengajuanPegawai != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(PengajuanPegawai.class, getId(),
							kelompokParameterTambahanPengajuanPegawai.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanPengajuanPegawai.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanPengajuanPegawai.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPengajuanPegawai.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Isian dinamis versi <b>berlabel</b> — bentuk siap tampil/cetak.
	 *
	 * <p>Satu baris per parameter, dipisah {@code "\n"}, tiap baris tujuh ruas yang dipisah
	 * <code>&lt;=&gt;</code>; formatnya diuraikan di {@link #populateParameterTambahan(List)}.
	 * Karena menyimpan label (bukan ID), isi kolom ini adalah <b>potret saat disimpan</b> — mengubah
	 * nama kelompok atau label parameter di master tidak mengubah baris pengajuan yang sudah ada.
	 * Kolom ini juga masuk daftar kolom ekspor/impor massal di layar pengajuan.</p>
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} dinormalkan menjadi string kosong dan disimpan ke
	 * field terpetakan.</p>
	 *
	 * @return string terenkode; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menyetel isian dinamis versi berlabel secara mentah.
	 *
	 * <p>Umumnya tidak dipanggil langsung — pakai {@link #populateParameterTambahan(List)}.</p>
	 *
	 * @param parameterTambahan string terenkode versi berlabel
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Satuan kerja tempat pegawai bertugas.
	 *
	 * <p><b>Nilai turunan, bukan isian.</b> Bila {@link #getPegawai()} terisi dan pegawai itu punya
	 * satuan kerja, field ini <b>selalu ditimpa</b> dengan satuan kerja pegawai — apa pun yang
	 * pernah disetel lewat {@link #setSatuanKerja(SatuanKerja)}. Hanya bila pegawai belum terisi
	 * (atau pegawainya tanpa satuan kerja) nilai yang tersimpan dipertahankan dan sekadar
	 * diresolusi lewat {@code check()}.</p>
	 *
	 * <p><b>Getter yang menulis</b> pada kedua cabang, dan pada cabang pertama juga memicu resolusi
	 * proxy {@link #getPegawai()}. Untuk unit <i>pengaju</i> yang boleh berbeda, pakai
	 * {@link #getSatuanKerjaPengaju()}.</p>
	 *
	 * @return satuan kerja pegawai, atau nilai tersimpan yang sudah teresolusi; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja")
	public SatuanKerja getSatuanKerja() {
		if (getPegawai() != null && pegawai.getSatuanKerja() != null) {
			satuanKerja = pegawai.getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja.
	 *
	 * <p>Perhatikan bahwa nilai ini akan <b>ditimpa</b> oleh {@link #getSatuanKerja()} begitu
	 * pegawai terisi dan punya satuan kerja.</p>
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Instance alur SOP yang menangani pengajuan ini.
	 *
	 * <p>Terisi bila jenis pengajuan dialirkan lewat mesin SOP; mesin itulah yang menautkannya
	 * (lewat kontrak {@link ais.database.model.sop.DataSop}). Begitu terisi, {@link #getSetujui()}
	 * dan {@link #getAktif()} berubah menjadi nilai turunan dari disposisi dan kotak centang
	 * persetujuan manual tidak dirender lagi.</p>
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code check()} ditugaskan kembali ke field.</p>
	 *
	 * @return disposisi SOP yang sudah teresolusi, atau {@code null} bila pengajuan ini memakai
	 *         jalur persetujuan manual
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menautkan pengajuan ini ke sebuah disposisi SOP.
	 *
	 * <p><b>Sekali terpasang tidak bisa dilepas:</b> argumen {@code null} atau disposisi yang belum
	 * punya ID diabaikan diam-diam, sehingga tautan yang sudah ada tidak dapat dihapus lewat setter
	 * ini. Ini disengaja — melepas disposisi akan membuat status persetujuan pengajuan kembali ke
	 * mode manual dan memutus jejak alur.</p>
	 *
	 * <p><b>Kuirk:</b> ekspresi ternary di badan method sudah tidak pernah bercabang. Penjaga di
	 * baris pertama sudah memastikan {@code disposisiSop != null} dan ber-ID, sehingga syarat
	 * ternary selalu {@code false} dan penugasan selalu memakai argumen baru. Sisa kode lama yang
	 * tidak berpengaruh; dicatat, tidak diubah.</p>
	 *
	 * @param disposisiSop disposisi SOP yang menangani pengajuan ini; diabaikan bila {@code null}
	 *         atau belum tersimpan (ID masih {@code null})
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
	 * Tahun pembuatan pengajuan.
	 *
	 * <p>Tidak ada input UI untuk properti ini; satu-satunya konsumennya adalah pembentukan nomor
	 * urut/nomor agenda, yang bisa dikonfigurasi agar penomorannya di-reset tiap tahun.</p>
	 *
	 * <p><b>Getter yang menulis:</b> bila masih {@code null}, tahun berjalan diisikan ke field
	 * terpetakan. Karena pengisian terjadi saat getter pertama kali dipanggil — bukan saat baris
	 * dibuat — nilai yang tersimpan adalah tahun saat pembacaan pertama, yang pada baris lama yang
	 * kolomnya kosong bisa berbeda dari tahun pengajuannya sendiri.</p>
	 *
	 * @return tahun (empat digit); tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * @param tahun tahun pembuatan (empat digit)
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Bulan pembuatan pengajuan, <b>1-12</b> (sudah ditambah satu dari
	 * {@link Calendar#MONTH} yang berbasis nol).
	 *
	 * <p>Sama seperti {@link #getTahun()}: tanpa input UI, hanya menopang aturan reset penomoran
	 * per bulan, dan <b>getter ini menulis</b> nilai berjalan ke field terpetakan bila masih
	 * {@code null}.</p>
	 *
	 * @return bulan 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * @param bulan bulan pembuatan, 1-12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Pegawai yang menjadi subjek pengajuan.
	 *
	 * <p>Di layar pengajuan, field ini dipaksa ke pegawai milik pengguna yang sedang login dan
	 * dinonaktifkan, sehingga secara praktis pengguna hanya bisa mengajukan untuk dirinya sendiri.
	 * Pegawai ini juga menentukan {@link #getSatuanKerja()} dan menjadi acuan pengecekan
	 * atasan pada jalur persetujuan manual.</p>
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code check()} ditugaskan kembali ke field.</p>
	 *
	 * @return pegawai yang sudah teresolusi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		return pegawai;
	}

	/**
	 * @param pegawai pegawai subjek pengajuan
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Akun pengguna yang menyetujui pengajuan pada jalur persetujuan <b>manual</b>.
	 *
	 * <p>Diisi bersamaan dengan {@link #setSetujuiTanggal(Date)} saat atasan mencentang kotak
	 * "Setujui", dan tetap kosong pada pengajuan yang mengalir lewat SOP (di sana jejak penyetuju
	 * ada di disposisinya, bukan di sini).</p>
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code check()} ditugaskan kembali ke field. Perhatikan
	 * bahwa nama field-nya {@code disetujiOleh} sementara nama properti/method-nya
	 * {@code disetujuiOleh}.</p>
	 *
	 * @return akun penyetuju yang sudah teresolusi, atau {@code null} bila belum disetujui manual
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);
		return disetujiOleh;
	}

	/**
	 * @param disetujiOleh akun pengguna yang menyetujui; di-null-kan saat persetujuan dibatalkan
	 */
	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	/**
	 * Tanggal persetujuan pada jalur manual.
	 *
	 * <p>Diisi waktu "sekarang" saat kotak centang "Setujui" dicentang dan di-null-kan saat
	 * dibatalkan. Tidak punya nilai default — {@code null} berarti belum pernah disetujui manual.</p>
	 *
	 * @return tanggal persetujuan, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {
		return setujuiTanggal;
	}

	/**
	 * @param setujuiTanggal tanggal persetujuan; {@code null} untuk membatalkan
	 */
	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	/**
	 * Status disetujui — <b>kolom biasa pada jalur manual, nilai turunan pada jalur SOP</b>.
	 *
	 * <p>Bila {@link #getDisposisiSop()} terisi, status dihitung ulang setiap kali getter dipanggil:
	 * disetujui bila disposisi sudah mencapai langkah "setuju". Bila tidak ada disposisi, nilai
	 * kolomnya dipakai apa adanya, dengan {@code null} dibaca sebagai belum disetujui.</p>
	 *
	 * <p><b>Getter yang menulis, dua arah.</b> Hasil penurunan disimpan ke field terpetakan dan
	 * bisa bergerak ke <b>kedua</b> arah — berbeda dari {@link #getAktif()} yang hanya bisa
	 * menurun. Karena itu membaca getter ini pada instance managed adalah mekanisme yang membuat
	 * kolom {@code setujui} akhirnya sinkron dengan alur SOP; modul payroll yang menyaring lewat
	 * SQL {@code setujui = true} bergantung pada kolom itu, bukan pada getter ini.</p>
	 *
	 * <p>Efek samping: memanggil {@link #getDisposisiSop()} di dalamnya ikut meresolusi proxy lazy
	 * disposisi.</p>
	 *
	 * @return {@code true} bila pengajuan sudah disetujui; tidak pernah {@code null}
	 */
	public Boolean getSetujui() {
		if (getDisposisiSop() != null) {
			setujui = getDisposisiSop().getDisposisiSetuju() != null;
		}
		return setujui == null ? false : setujui;
	}

	/**
	 * Menyetel status disetujui secara manual.
	 *
	 * <p>Dipakai kotak centang "Setujui" di layar daftar, yang hanya dirender untuk atasan langsung
	 * atau atasan jabatan pegawai bersangkutan dan tidak untuk pegawai itu sendiri. Pada pengajuan
	 * yang punya disposisi SOP, nilai yang disetel di sini akan ditimpa kembali oleh
	 * {@link #getSetujui()}.</p>
	 *
	 * @param setujui status persetujuan
	 */
	public void setSetujui(Boolean setujui) {
		this.setujui = setujui;
	}

	/**
	 * Akun pengguna yang membuat pengajuan.
	 *
	 * <p>Berbeda dari {@link #getPegawai()}: yang ini akun aplikasi (pembuat baris), yang itu
	 * pegawai subjek pengajuan. Pada praktiknya keduanya merujuk orang yang sama karena layar
	 * mengunci field pegawai ke pegawai milik pengguna login.</p>
	 *
	 * <p><b>Getter yang menulis:</b> hasil {@code check()} ditugaskan kembali ke field.</p>
	 *
	 * @return akun pengaju yang sudah teresolusi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Tbmuser getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);
		return diajukanOleh;
	}

	/**
	 * @param diajukanOleh akun pengguna pembuat pengajuan
	 */
	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/**
	 * Status aktif pengajuan — dipakai layar daftar untuk menyembunyikan pengajuan yang batal atau
	 * ditolak.
	 *
	 * <p>Nilainya diturunkan dari disposisi SOP. Pengajuan dinyatakan tidak aktif bila salah satu
	 * terpenuhi:</p>
	 * <ol>
	 * <li>disposisinya sendiri sudah tidak aktif; atau</li>
	 * <li>langkah akhir disposisi berada pada alur yang ditandai sebagai langkah
	 * <b>penolakan</b>.</li>
	 * </ol>
	 *
	 * <p><b>Flag satu arah.</b> Tidak ada cabang yang mengembalikan nilai ke {@code true}: sekali
	 * getter ini menyimpan {@code false} ke field terpetakan, status itu bertahan walau kondisi
	 * disposisinya kemudian berubah. Nilai {@code null} (belum pernah dievaluasi) dibaca sebagai
	 * aktif — itulah sebabnya filter "hanya yang aktif" di layar daftar harus mencocokkan
	 * <i>kolom bernilai null ATAU true</i>, bukan hanya {@code true}.</p>
	 *
	 * <p><b>Getter yang menulis</b> baik ke {@link #aktif} maupun ke field {@link #disposisiSop}
	 * (baris pertama menugaskan ulang hasil {@link #getDisposisiSop()}). Tidak ada satu pun kode
	 * aplikasi yang memanggil {@link #setAktif(Boolean)} — status ini sepenuhnya turunan.</p>
	 *
	 * @return {@code true} bila pengajuan masih berlaku; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Disediakan demi kelengkapan properti Hibernate; tidak ada kode aplikasi yang memanggilnya,
	 * dan nilainya akan ditimpa {@link #getAktif()} bila disposisi menyatakan sebaliknya.</p>
	 *
	 * @param aktif status aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Satuan kerja yang <b>mengajukan</b> — boleh berbeda dari satuan kerja pegawai, misalnya saat
	 * sebuah unit meminta bantuan tenaga dari unit lain.
	 *
	 * <p>Di formulir ini adalah field tersendiri yang diisi otomatis dari satuan kerja pegawai saat
	 * pegawai dipilih pada pengajuan baru, lalu boleh diubah pengguna.</p>
	 *
	 * <p><b>Getter yang menulis:</b> bila masih kosong, diisi dari {@link #getSatuanKerja()} dan
	 * disimpan ke field terpetakan — sehingga baris lama yang kolomnya kosong akan "menempel" ke
	 * satuan kerja pegawai saat pertama dibaca. Berbeda dari relasi lain di class ini, getter ini
	 * <b>tidak</b> memanggil {@code check()} secara langsung; resolusi proxy hanya terjadi lewat
	 * {@link #getSatuanKerja()} pada cabang pengisian, jadi nilai yang sudah tersimpan dikembalikan
	 * apa adanya dan bisa saja masih berupa proxy lazy.</p>
	 *
	 * @return satuan kerja pengaju; boleh {@code null} bila satuan kerja pegawai juga kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja_pengaju")
	public SatuanKerja getSatuanKerjaPengaju() {
		if (satuanKerjaPengaju == null) {
			satuanKerjaPengaju = getSatuanKerja();
		}
		satuanKerjaPengaju = check(satuanKerjaPengaju);
		return satuanKerjaPengaju;
	}

	/**
	 * @param satuanKerjaPengaju satuan kerja yang mengajukan
	 */
	public void setSatuanKerjaPengaju(SatuanKerja satuanKerjaPengaju) {
		this.satuanKerjaPengaju = satuanKerjaPengaju;
	}

	/**
	 * Akhir rentang pengajuan (inklusif).
	 *
	 * <p>Bila belum diisi, yang dikembalikan adalah <b>sehari setelah</b> {@link #getWaktu()};
	 * nilai default itu tidak disimpan ke field. Perhatikan bahwa jam/menit pada nilai default
	 * mengikuti waktu saat pemanggilan, bukan jam {@link #getWaktu()}.</p>
	 *
	 * @return akhir rentang; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSampai() {
		return waktuSampai == null ? WaktuUtil.besok(getWaktu()) : waktuSampai;
	}

	/**
	 * @param waktuSampai akhir rentang pengajuan
	 */
	public void setWaktuSampai(Date waktuSampai) {
		this.waktuSampai = waktuSampai;
	}

	/**
	 * Jumlah hari yang dicakup pengajuan.
	 *
	 * <p><b>Nilai turunan yang selalu dihitung ulang</b> dari selisih {@link #getWaktu()} dan
	 * {@link #getWaktuSampai()}, ditambah satu agar hari awal ikut terhitung. Nilai apa pun yang
	 * disetel lewat {@link #setJumlahHari(Integer)} akan tertimpa pada pembacaan berikutnya.</p>
	 *
	 * <p>Dipakai layar untuk menentukan bentuk isian keterangan: lebih dari satu hari berarti
	 * formulir meminta keterangan kegiatan per hari yang disimpan di
	 * {@link #getKeteranganBanyak()}, satu hari berarti cukup {@link #getKeterangan()}.</p>
	 *
	 * <p><b>Kuirk penghitungan.</b> Selisihnya dihitung atas <i>stempel waktu</i>, bukan atas
	 * tanggal kalender. Dua stempel pada hari yang sama tetapi beda jam sudah menghasilkan selisih
	 * satu, sehingga jumlah hari terbaca 2. Karena {@link #getWaktuSampai()} secara bawaan bernilai
	 * "sehari setelah waktu mulai", pengajuan baru yang belum disunting pun sudah dianggap 2
	 * hari.</p>
	 *
	 * <p><b>Getter yang menulis</b> ke field terpetakan setiap kali dipanggil (kedua tanggal selalu
	 * ada nilainya karena masing-masing punya default), jadi pada instance managed pemanggilan
	 * getter ini bisa memicu {@code UPDATE}.</p>
	 *
	 * @return jumlah hari rentang; boleh {@code null} hanya secara teoretis, tidak pernah dalam
	 *         praktik karena kedua tanggal selalu bernilai
	 */
	public Integer getJumlahHari() {
		if (getWaktu() != null && getWaktuSampai() != null) {
			jumlahHari = Common.getBetweenTwoDates(getWaktu(), getWaktuSampai()) + 1;
		}
		return jumlahHari;
	}

	/**
	 * Menyetel jumlah hari.
	 *
	 * <p>Praktis tidak berguna: {@link #getJumlahHari()} selalu menghitung ulang dan menimpa nilai
	 * ini. Ada semata-mata agar properti Hibernate lengkap.</p>
	 *
	 * @param jumlahHari jumlah hari
	 */
	public void setJumlahHari(Integer jumlahHari) {
		this.jumlahHari = jumlahHari;
	}

	/**
	 * Nilai bawaan untuk {@link #getKeteranganBanyak()}, yaitu teks objek JSON kosong
	 * ({@code "{}"}).
	 *
	 * <p>Berbeda dari konstanta senama di entity akunting ({@code KasBesar}, {@code KasKecil},
	 * {@code Pertangungjawaban}) yang berisi <b>array</b> JSON kosong {@code "[]"} — di sini
	 * strukturnya objek karena kuncinya adalah indeks hari, bukan urutan larik.</p>
	 *
	 * <p><b>Kuirk:</b> dideklarasikan {@code public static} <b>tanpa</b> {@code final}, jadi secara
	 * teknis bisa ditimpa dari mana saja dan mengubah default seluruh JVM. Dibiarkan seperti aslinya
	 * agar konsisten dengan konstanta serupa di entity lain.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONObject().toString();

	/**
	 * Keterangan kegiatan <b>per hari</b> dalam bentuk teks objek JSON.
	 *
	 * <p>Kuncinya adalah indeks hari sebagai string ({@code "0"} untuk hari pertama, {@code "1"}
	 * untuk hari kedua, dan seterusnya) dan nilainya keterangan kegiatan hari itu. Formulir hanya
	 * menampilkan isian ini bila {@link #getJumlahHari()} lebih dari satu, dan mewajibkan setiap
	 * hari terisi. Kunci {@code "0"} disinkronkan dua arah dengan {@link #getKeterangan()}.</p>
	 *
	 * <p>Nilai kosong/hanya spasi dilaporkan sebagai {@link #DEFAULT_FORMULA} ({@code "{}"})
	 * sehingga pemanggil selalu bisa langsung membangun {@code JSONObject} tanpa memeriksa
	 * {@code null}. Normalisasi ini <b>tidak</b> ditulis balik ke field.</p>
	 *
	 * @return teks objek JSON keterangan harian; tidak pernah {@code null} maupun kosong
	 */
	@Column(name = "keterangan_banyak", nullable = true, columnDefinition = "text")
	public String getKeteranganBanyak() {
		return keteranganBanyak == null || keteranganBanyak.trim().isEmpty() ? DEFAULT_FORMULA : keteranganBanyak;
	}

	/**
	 * @param keteranganBanyak teks objek JSON keterangan harian; disusun lapisan UI dari kotak
	 *         "Keterangan kegiatan hari ke-N"
	 */
	public void setKeteranganBanyak(String keteranganBanyak) {
		this.keteranganBanyak = keteranganBanyak;
	}
}
