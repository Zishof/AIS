package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.sekolah.Siswa;

/**
 * Entity <b>pengajuan izin / sakit untuk tidak menghadiri satu pertemuan</b> (tabel
 * {@code public.pengajuan_izin_tidak_masuk_perkuliahan}).
 *
 * <p>Satu baris = satu permohonan ketidakhadiran seorang peserta didik pada <b>satu</b>
 * {@link Pertemuan}. Bukan izin per-semester atau per-matakuliah: cakupannya selalu satu sesi
 * tatap muka/daring. Peserta didiknya bisa {@link Mahasiswa} (jenjang perguruan tinggi) atau
 * {@link ais.database.model.sekolah.Siswa} (jenjang sekolah) &mdash; keduanya adalah kolom
 * terpisah dan hanya salah satu yang terisi; lihat bagian <i>Dua jenjang dalam satu tabel</i>
 * di bawah.</p>
 *
 * <h2>Peran dalam alur izin tidak masuk perkuliahan</h2>
 * <ol>
 *   <li><b>Pengajuan.</b> Baris dibuat dari layar absensi pertemuan &mdash; dialog ZK di
 *       {@code ais.action.master.helper.AbsensiHelper} (perguruan tinggi),
 *       {@code ais.action.master.sekolah.helper.AbsensiSiswaHelper} (sekolah), atau formulir
 *       web baru {@code webapp/WEB-INF/baru/modul/elearning/pengajuan_izin_atau_sakit.jsp}.
 *       Yang wajib diisi: peserta, {@link #getStatusabsensi() jenis pengajuan} (dibatasi ke
 *       {@code ConstantValues.IZIN} dan {@code ConstantValues.SAKIT} saja), dan
 *       {@link #getKeterangan() alasan}. Surat/berkas pendukung disimpan terpisah sebagai
 *       {@code ais.database.model.file.LampiranLain} berjenis
 *       {@code LampiranLain.IZIN_TIDAK_MASUK} dengan {@code ref} = {@link #getId() id} baris
 *       ini.</li>
 *   <li><b>Notifikasi.</b> Sesudah baris tersimpan, jalur ZK memanggil
 *       {@code ais.common.CommonEmail#infoAdaIzinAbsensi(PengajuanIzinTidakMasukPerkuliahan)}
 *       yang mengirim surel asinkron ke dosen/guru dan pengelola pertemuan. Jalur JSP generik
 *       <b>tidak</b> mengirim notifikasi apa pun.</li>
 *   <li><b>Persetujuan.</b> Penilai mencentang checkbox &quot;Setujui&quot; pada grid daftar
 *       izin, yang menyetel {@link #setDiizinkan(Boolean)} menjadi {@code true}.</li>
 *   <li><b>Dampak ke presensi.</b> Persetujuan bukan sekadar penanda administratif: pada saat
 *       checkbox diklik, pemanggilnya langsung menulis status kehadiran peserta pada
 *       {@link Pertemuan} lewat {@link Pertemuan#populate}, memakai
 *       {@link #getStatusabsensi()} dan {@link #getKeterangan()} baris ini. Jadi menyetujui
 *       satu pengajuan izin <b>mengubah rekam presensi</b> mahasiswa/siswa untuk pertemuan
 *       tersebut menjadi Izin atau Sakit.</li>
 * </ol>
 *
 * <h2>Efek status &quot;sudah disetujui&quot;</h2>
 * <p>Nilai {@code true} pada {@link #getDiizinkan()} berfungsi sebagai <b>kunci baris</b> di
 * seluruh layar:</p>
 * <ul>
 *   <li>Dialog pengajuan ulang di {@code AbsensiHelper} menolak menimpa baris yang sudah
 *       disetujui (&quot;telah disetujui, sehingga tidak bisa diubah&quot;).</li>
 *   <li>Tombol Hapus disembunyikan pada baris yang sudah disetujui, baik di grid ZK maupun di
 *       kartu JSP {@code _daftar_peserta_absen_izin_dan_sakit.jsp}.</li>
 *   <li>Pada {@code AbsensiGrupPertemuanHelper}, kombo status kehadiran peserta diganti label
 *       statis sehingga status hasil izin tidak bisa lagi disunting bebas dari grid absensi.</li>
 * </ul>
 * <p><b>Penting:</b> seluruh penjagaan di atas adalah penjagaan <i>tampilan</i>. Lihat bagian
 * <i>Catatan otorisasi</i>.</p>
 *
 * <h2>Dua jenjang dalam satu tabel</h2>
 * <p>{@link #getMahasiswa()} dan {@link #getSiswa()} adalah dua kolom FK terpisah yang
 * nullable; baris perguruan tinggi mengisi {@code mahasiswa} dan membiarkan {@code siswa}
 * kosong, dan sebaliknya. Formulir JSP memilih salah satu secara otomatis dengan memeriksa
 * apakah {@link Pertemuan#ambilVOPembelajaran()} berisi daftar siswa atau daftar mahasiswa.
 * Konsekuensinya: kode pembaca <b>wajib</b> memeriksa {@code null} pada sisi yang tidak
 * dipakai &mdash; beberapa jalur lama (termasuk {@link #toString()} dan renderer ZK jenjang
 * perguruan tinggi) menganggap {@code mahasiswa} selalu ada.</p>
 *
 * <h2>Pembaca dan penulis lain</h2>
 * <ul>
 *   <li>{@link Pertemuan} menyimpan indeks id pengajuan izin miliknya dalam berkas JSON
 *       (lihat {@link Pertemuan#ambilPengajuanIzinTidakMasukPerkuliahanTotal()},
 *       {@link Pertemuan#ambilJumlahPengajuanIzinTidakMasukPerkuliahan()}). Indeks itu
 *       dipelihara oleh {@code ais.database.hibernate.AuditListener} yang memanggil
 *       {@link Pertemuan#populatePengajuanIzinTidakMasukPerkuliahan(Long)} pada
 *       insert/update dan {@link Pertemuan#removePengajuanIzinTidakMasukPerkuliahan(java.io.Serializable)}
 *       pada delete. Karena itu, menulis baris ini lewat jalur apa pun ikut menyentuh cache
 *       {@link Pertemuan}.</li>
 *   <li>{@code ais.common.DataUtil} mendaftarkan kelas ini dalam {@code CLASS_IZINKAN},
 *       artinya instance-nya boleh disimpan pada cache MapDB dan diambil lagi lewat
 *       {@link GeneralValueObject#ambilData(Class, String, boolean)}.</li>
 *   <li>{@link VOPembelajaran} membaca daftar izin per pertemuan saat menyusun rekap
 *       pembelajaran.</li>
 *   <li>{@code ais.database.model.Statuskehadiran_old} memiliki FK ke entity ini &mdash;
 *       peninggalan skema presensi lama yang sudah digantikan mekanisme JSON pada
 *       {@link Pertemuan}.</li>
 * </ul>
 *
 * <h2>Catatan otorisasi (didokumentasikan apa adanya, bukan rekomendasi perubahan)</h2>
 * <ul>
 *   <li><b>Persetujuan dijaga hanya di sisi tampilan.</b> Ketiga helper ZK dan JSP
 *       {@code pengajuan_izin_atau_sakit.jsp} sama-sama memutuskan siapa yang boleh menyetujui
 *       dengan {@link Pertemuan#bolehUbahAbsenSaja(Tbmuser)}; bila hasilnya {@code false},
 *       kolom &quot;Persetujuan&quot; sekadar <i>tidak ditampilkan</i>. Formulir JSP menyimpan
 *       lewat endpoint reflektif generik {@code /Data?datasearch=} &rarr;
 *       {@code ElearningApiUtil.simpanDataRinci} &rarr; {@code prosesSimpan} &rarr;
 *       {@code simpanProperty}, yang menyetel properti apa pun yang dikirim klien dan
 *       <b>tidak</b> memiliki gerbang otorisasi untuk kelas ini (gerbangnya hanya ada untuk dua
 *       kelas master e-Kantin). Hal yang sama berlaku pada penghapusan lewat
 *       {@code prosesHapus}.</li>
 *   <li><b>Penilai tidak dibatasi lingkup.</b> {@link Pertemuan#bolehUbahAbsenSaja(Tbmuser)}
 *       hanya menguji bahwa akun bukan mahasiswa/siswa/calon (atau merupakan asisten absen
 *       perkuliahan terkait). Tidak ada pemeriksaan bahwa penilai benar-benar pengampu
 *       pertemuan tersebut.</li>
 *   <li><b>Jejak persetujuan minim.</b> Persetujuan hanya berupa satu {@code boolean}: tidak
 *       ada kolom penyetuju, tanggal persetujuan, status &quot;ditolak&quot;, maupun alasan
 *       penolakan. Yang tersedia hanyalah {@link #getOleh()}/{@link #getOlehId()} (pengubah
 *       terakhir, dan hanya terisi pada {@code UPDATE} karena tidak ada {@code @PrePersist})
 *       serta riwayat Hibernate Envers dari {@link Audited}.</li>
 * </ul>
 *
 * <h2>Mengapa {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * dideklarasikan ulang</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti yang dideklarasikan di sana. Mendeklarasikan ulang keempat anggota audit tersebut di
 * setiap entity adalah <b>keharusan teknis</b> agar kolomnya benar-benar terpetakan, bukan
 * duplikasi yang perlu dirapikan.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Audit/infrastruktur:</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><b>Konteks pertemuan:</b> {@link #getPertemuan()}.</li>
 *   <li><b>Pengaju:</b> {@link #getMahasiswa()}, {@link #getSiswa()}.</li>
 *   <li><b>Isi pengajuan:</b> {@link #getStatusabsensi()}, {@link #getKeterangan()}.</li>
 *   <li><b>Hasil penilaian:</b> {@link #getDiizinkan()}.</li>
 * </ul>
 * <p>Tidak ada method bisnis, query statis, maupun validasi di kelas ini; seluruh aturan
 * (batas jenis absensi, kelayakan pengaju, kewenangan penyetuju) berada di helper/JSP
 * pemanggil.</p>
 *
 * <p>Komentar generator asli di atas kelas ini berbunyi &quot;Bank generated by hbm2java&quot;
 * &mdash; sisa salin-tempel dari template entity {@code Bank} dan <b>tidak</b> menggambarkan isi
 * kelas ini.</p>
 *
 * @see Pertemuan
 * @see Statusabsensi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengajuan_izin_tidak_masuk_perkuliahan")

public class PengajuanIzinTidakMasukPerkuliahan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris, kolom {@code id} (identity/serial pada PostgreSQL). Lihat
	 * {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama pengguna yang terakhir mengubah baris ini, kolom {@code oleh}. Diisi otomatis oleh
	 * {@link #onUpdate()}; tetap kosong untuk baris yang belum pernah di-{@code UPDATE}. Lihat
	 * {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Identitas (user id) pengguna yang terakhir mengubah baris ini, kolom {@code oleh_id}.
	 * Pendamping {@link #oleh}, diisi pada jalur yang sama. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Kembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return user id pengubah terakhir, atau {@code null} bila baris belum pernah
	 *         di-{@code UPDATE}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setel identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> bila {@code olehId} adalah {@code null}
	 * atau hanya berisi spasi, method langsung kembali tanpa mengubah apa pun &mdash; nilai
	 * lama dipertahankan. Ini disengaja agar jejak audit yang sudah terisi tidak terhapus oleh
	 * jalur simpan yang kebetulan tidak membawa identitas pengguna (mis. tugas terjadwal atau
	 * impor). Konsekuensinya, kolom ini tidak dapat dikosongkan kembali lewat setter.</p>
	 *
	 * @param olehId user id pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Setel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan</b>
	 * sehingga nilai audit yang sudah ada tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Kembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila baris belum pernah
	 *         di-{@code UPDATE}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan pengisian jejak audit ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, yang menyetel
	 * {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)} dari pengguna yang sedang login. Dipanggil oleh penyedia
	 * persistensi &mdash; <b>tidak pernah</b> dipanggil manual. Pada entity ini pemicunya
	 * praktis selalu satu hal: penilai mencentang/melepas checkbox persetujuan, atau pengaju
	 * menyunting keterangan pengajuan yang belum disetujui.</p>
	 *
	 * <p><b>Tidak ada pasangan {@code @PrePersist}</b>, sehingga {@code INSERT} tidak mencatat
	 * pembuat baris; siapa yang mengajukan izin hanya bisa ditelusuri lewat riwayat Envers
	 * ({@link Audited}). Konsekuensinya dibahas pada Javadoc kelas.</p>
	 *
	 * <p><b>Field {@code tanggal_dirubah}.</b> Dideklarasikan pada baris yang sama dan
	 * diinisialisasi ke waktu sekarang lewat {@code ais.ui.util.WaktuUtil.getDate()} ketika
	 * object dibentuk di memori, sehingga baris baru tetap memiliki stempel waktu meski hook di
	 * atas belum pernah berjalan. Lihat {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setel cap waktu perubahan terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini menerima
	 * {@code null} apa adanya.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Kembalikan cap waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * presisi {@code TIMESTAMP}).
	 *
	 * <p>Nilainya tidak pernah {@code null} untuk object yang baru dibentuk di memori karena
	 * field-nya diinisialisasi saat deklarasi; untuk baris yang dimuat dari basis data, nilai
	 * kolomlah yang dipakai.</p>
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code "<id>-<mahasiswa>"}.
	 *
	 * <p>Dipakai untuk keperluan log/diagnostik (mis. baris {@code System.out.println} di
	 * {@code AbsensiHelper} setelah pengajuan disimpan), bukan untuk ditampilkan ke pengguna
	 * akhir.</p>
	 *
	 * <p><b>Dua kuirk yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li><b>Menulis balik ke field.</b> Baris pertama menyalin hasil
	 *       {@link #getMahasiswa()} ke field {@code mahasiswa}. Karena {@link #getMahasiswa()}
	 *       sendiri sudah melakukan penulisan balik tersebut, penugasan di sini mubazir; efek
	 *       nyatanya adalah proxy lazi {@code mahasiswa} ikut ter-resolusi hanya karena object
	 *       ini di-{@code toString()} (mis. oleh debugger atau log).</li>
	 *   <li><b>Baris jenjang sekolah.</b> Untuk baris yang mengisi {@link #getSiswa()} dan
	 *       bukan {@code mahasiswa}, keluarannya berakhir sebagai {@code "<id>-null"} karena
	 *       {@link #getSiswa()} tidak pernah ikut dilihat.</li>
	 * </ul>
	 *
	 * @return gabungan id dan mahasiswa pengaju
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		return id + "-" + mahasiswa;
	}

	/**
	 * Pertemuan yang ketidakhadirannya diajukan, kolom FK {@code pertemuan}. Lihat
	 * {@link #getPertemuan()}.
	 */
	private Pertemuan pertemuan;
	/**
	 * Mahasiswa pengaju (jenjang perguruan tinggi), kolom FK {@code mahasiswa}; kosong pada
	 * baris jenjang sekolah. Lihat {@link #getMahasiswa()}.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Siswa pengaju (jenjang sekolah), kolom FK {@code siswa}; kosong pada baris jenjang
	 * perguruan tinggi. Lihat {@link #getSiswa()}.
	 */
	private Siswa siswa;
	/**
	 * Jenis ketidakhadiran yang diajukan (Izin atau Sakit), kolom FK {@code statusabsensi}.
	 * Lihat {@link #getStatusabsensi()}.
	 */
	private Statusabsensi statusabsensi;
	/**
	 * Penanda bahwa pengajuan sudah disetujui. {@code null} untuk baris lama diperlakukan sama
	 * dengan {@code false} oleh {@link #getDiizinkan()}.
	 */
	private Boolean diizinkan;
	/**
	 * Alasan izin yang ditulis pengaju, kolom {@code keterangan}. Ikut disalin ke rekam
	 * presensi saat pengajuan disetujui. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk membentuk instance saat
	 * memuat baris, dan dipakai kode pemanggil untuk membuat pengajuan baru sebelum
	 * seluruh field disetel lewat setter.
	 */
	public PengajuanIzinTidakMasukPerkuliahan() {
	}

	/**
	 * Kembalikan kunci utama baris ini.
	 *
	 * <p>Selain sebagai identitas baris, nilai ini dipakai sebagai {@code ref} pada
	 * {@code ais.database.model.file.LampiranLain} berjenis
	 * {@code LampiranLain.IZIN_TIDAK_MASUK} untuk menautkan surat/berkas pendukung, dan
	 * sebagai kunci pada indeks JSON pengajuan izin milik {@link Pertemuan}.</p>
	 *
	 * <p>Kolomnya dipetakan {@code insertable = false} dengan strategi
	 * {@link javax.persistence.GenerationType#IDENTITY}, jadi nilainya baru terisi setelah
	 * baris benar-benar tersimpan.</p>
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setel kunci utama baris ini. Umumnya hanya dipanggil oleh Hibernate; pemanggil biasa
	 * tidak perlu menyetelnya sendiri.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kembalikan alasan izin yang ditulis pengaju.
	 *
	 * <p>Wajib diisi oleh kedua jalur pengajuan (dialog ZK memvalidasinya dan menolak teks
	 * kosong; formulir JSP mencantumkannya pada {@code paramRequired}). Nilai ini bukan sekadar
	 * catatan: ketika pengajuan disetujui, teksnya ikut disalin menjadi keterangan pada rekam
	 * presensi pertemuan lewat {@link Pertemuan#populate}, dan ikut dikirim pada surel
	 * notifikasi {@code CommonEmail#infoAdaIzinAbsensi}.</p>
	 *
	 * @return alasan izin, atau {@code null} untuk baris lama yang tidak mengisinya
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setel alasan izin.
	 *
	 * @param keterangan alasan izin yang ditulis pengaju
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kembalikan pertemuan yang ketidakhadirannya diajukan.
	 *
	 * <p>Berbeda dari relasi lain di kelas ini, getter ini <b>tidak</b> memanggil
	 * {@code check(...)}: relasinya dipetakan tanpa {@link FetchType#LAZY} dan dengan
	 * {@link FetchMode#SELECT}, sehingga Hibernate sudah memuat entity sesungguhnya. Karena
	 * itu getter ini murni pembacaan &mdash; tidak menulis balik ke field, tidak menyentuh
	 * sesi Hibernate, dan tidak destruktif.</p>
	 *
	 * <p>Nilainya dipakai oleh {@code AuditListener} untuk memutakhirkan indeks JSON pengajuan
	 * izin pada {@link Pertemuan}, oleh renderer persetujuan untuk menulis rekam presensi, dan
	 * oleh {@code CommonEmail#infoAdaIzinAbsensi} untuk menyusun konteks akademik pada surel.
	 * Kolomnya nullable dan data lama memang bisa kehilangan tautannya &mdash;
	 * {@code CommonEmail} secara eksplisit menangani kasus {@code null} tersebut dengan
	 * melewati notifikasi.</p>
	 *
	 * @return pertemuan terkait, atau {@code null} pada data lama yang tidak tertaut
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertemuan", nullable = true)
	public Pertemuan getPertemuan() {
		return pertemuan;
	}

	/**
	 * Setel pertemuan yang ketidakhadirannya diajukan.
	 *
	 * @param pertemuan pertemuan terkait
	 */
	public void setPertemuan(Pertemuan pertemuan) {
		this.pertemuan = pertemuan;
	}

	/**
	 * Kembalikan mahasiswa pengaju (jenjang perguruan tinggi).
	 *
	 * <p><b>Menulis balik ke field.</b> Getter ini memanggil
	 * {@link GeneralValueObject#check(Object)} lalu <b>menugaskan hasilnya kembali</b> ke
	 * field {@code mahasiswa}. Perilaku itu adalah resolusi proxy lazi standar di repo ini:
	 * {@code check(...)} berusaha menukar proxy Hibernate yang mungkin sudah terlepas dari
	 * sesi dengan instance nyata (dari sesi aktif, cache, atau pembacaan ulang), dan bila
	 * semua sumber gagal ia mengembalikan argumennya apa adanya. Yang perlu diketahui: karena
	 * hasilnya ditulis ke field, <i>membaca</i> properti ini mengubah keadaan object.
	 * {@code check(...)} tidak menutup sesi Hibernate dan tidak menghapus data &mdash; getter
	 * ini tidak destruktif.</p>
	 *
	 * <p>Kolomnya nullable karena baris jenjang sekolah memakai {@link #getSiswa()} sebagai
	 * gantinya. Beberapa pemanggil jenjang perguruan tinggi (renderer ZK, {@link #toString()})
	 * meng-dereference hasil ini tanpa penjagaan {@code null}.</p>
	 *
	 * @return mahasiswa pengaju, atau {@code null} bila baris ini milik jenjang sekolah
	 * @see #getSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Setel mahasiswa pengaju.
	 *
	 * @param mahasiswa mahasiswa pengaju
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Kembalikan jenis ketidakhadiran yang diajukan.
	 *
	 * <p>Kedua jalur pengajuan membatasi pilihannya ke dua nilai saja: {@code ConstantValues.IZIN}
	 * dan {@code ConstantValues.SAKIT} (formulir JSP membatasinya lewat klausa
	 * {@code where id in (...)} pada dropdown). Pembatasan itu berada di sisi pemanggil, bukan
	 * di entity ini, sehingga kolomnya secara teknis dapat menampung status absensi lain.</p>
	 *
	 * <p>Nilai ini menentukan status presensi yang akan ditulis ke {@link Pertemuan} pada saat
	 * pengajuan disetujui.</p>
	 *
	 * <p><b>Menulis balik ke field</b> lewat {@link GeneralValueObject#check(Object)}, sama
	 * seperti {@link #getMahasiswa()}: bukan getter destruktif dan tidak menutup sesi, tetapi
	 * membaca properti ini memutakhirkan field-nya.</p>
	 *
	 * @return status absensi yang diajukan (Izin/Sakit), atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "statusabsensi", nullable = true)
	public Statusabsensi getStatusabsensi() {
		statusabsensi = check(statusabsensi);
		return statusabsensi;
	}

	/**
	 * Setel jenis ketidakhadiran yang diajukan.
	 *
	 * @param statusabsensi status absensi yang diajukan (biasanya Izin atau Sakit)
	 */
	public void setStatusabsensi(Statusabsensi statusabsensi) {
		this.statusabsensi = statusabsensi;
	}

	/**
	 * Kembalikan status persetujuan pengajuan ini.
	 *
	 * <p><b>Menormalkan {@code null} menjadi {@code false}</b> sehingga pemanggil dapat
	 * meng-unbox hasilnya langsung ke {@code boolean} tanpa risiko
	 * {@link NullPointerException} &mdash; hal ini penting karena baris lama dapat memiliki
	 * kolom kosong. Normalisasi hanya terjadi pada nilai yang dikembalikan; field
	 * {@link #diizinkan} sendiri <b>tidak</b> ikut ditulisi, jadi getter ini murni pembacaan.</p>
	 *
	 * <p>Konsekuensi bisnis nilai {@code true} dijelaskan pada Javadoc kelas: baris terkunci
	 * dari penyuntingan/penghapusan lewat layar, dan rekam presensi peserta pada pertemuan
	 * terkait sudah ditulis sesuai {@link #getStatusabsensi()}.</p>
	 *
	 * <p>Perhatikan bahwa properti ini <b>tidak dianotasi</b> {@link Column}; pemetaannya
	 * mengikuti aturan bawaan Hibernate (kolom {@code diizinkan}).</p>
	 *
	 * @return {@code true} bila pengajuan sudah disetujui, {@code false} bila belum atau
	 *         kolomnya kosong
	 */
	public Boolean getDiizinkan() {
		return diizinkan == null ? false : diizinkan;
	}

	/**
	 * Setel status persetujuan pengajuan ini.
	 *
	 * <p>Dipanggil dari event {@code onClick} checkbox &quot;Setujui&quot; pada grid daftar
	 * izin di {@code AbsensiHelper}, {@code AbsensiGrupPertemuanHelper}, dan
	 * {@code AbsensiSiswaHelper}, serta dari kolom &quot;Persetujuan&quot; formulir
	 * {@code pengajuan_izin_atau_sakit.jsp}. Pada jalur ZK, pemanggil langsung menyimpan baris
	 * ini <i>dan</i> menulis status kehadiran peserta ke {@link Pertemuan} lewat
	 * {@link Pertemuan#populate} &mdash; efek samping itu ada di pemanggil, bukan di setter
	 * ini.</p>
	 *
	 * <p>Setter ini sendiri tidak memeriksa kewenangan apa pun; penjagaan siapa yang boleh
	 * menyetujui sepenuhnya berada di lapisan tampilan (lihat Javadoc kelas).</p>
	 *
	 * @param diizinkan {@code true} untuk menyetujui, {@code false} untuk membatalkan
	 *                  persetujuan
	 */
	public void setDiizinkan(Boolean diizinkan) {
		this.diizinkan = diizinkan;
	}

	/**
	 * Kembalikan siswa pengaju (jenjang sekolah).
	 *
	 * <p>Padanan {@link #getMahasiswa()} untuk jenjang sekolah; hanya salah satu dari keduanya
	 * yang terisi pada satu baris. Dibaca oleh
	 * {@code ais.action.master.sekolah.helper.AbsensiSiswaHelper} dan oleh formulir JSP ketika
	 * pertemuan terdeteksi berisi daftar siswa.</p>
	 *
	 * <p><b>Menulis balik ke field</b> lewat {@link GeneralValueObject#check(Object)}, sama
	 * seperti {@link #getMahasiswa()}: bukan getter destruktif, tidak menutup sesi Hibernate,
	 * tetapi membaca properti ini memutakhirkan field-nya.</p>
	 *
	 * @return siswa pengaju, atau {@code null} bila baris ini milik jenjang perguruan tinggi
	 * @see #getMahasiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Setel siswa pengaju.
	 *
	 * @param siswa siswa pengaju
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

}
