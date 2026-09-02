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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>permohonan/pengajuan yang diajukan oleh (atau atas nama) seorang mahasiswa</b>,
 * dipetakan ke tabel {@code public.pengajuan_mahasiswa}.
 *
 * <h2>Ini entity GENERIK, bukan entity satu jenis pengajuan tertentu</h2>
 *
 * <p>Hasil verifikasi dari kode (bukan asumsi dari nama kelas): tidak ada satu pun konstanta,
 * enum, atau percabangan {@code if} di kelas ini yang mengunci jenis permohonan tertentu
 * (mis. cuti, pindah, beasiswa, atau surat keterangan). Jenis permohonan sepenuhnya ditentukan
 * oleh <b>data master</b> lewat relasi {@link #getJenisPengajuan()} ke {@link JenisPengajuan} —
 * sebuah tabel master berisi {@code nama}, {@code keterangan}, {@code aktif}, konfigurasi
 * penomoran surat ({@code NomorSurat}), dan daftar
 * {@link KelompokParameterTambahanPengajuan} yang menentukan field tambahan apa saja yang harus
 * diisi pemohon. Artinya operator dapat menambah jenis permohonan baru tanpa menyentuh kode:
 * cukup menambah baris {@code JenisPengajuan} beserta kelompok parameternya.</p>
 *
 * <p>Satu-satunya jejak "jenis tertentu" yang ada di kelas ini adalah flag boolean
 * {@link #getSemesterPendek()} (permohonan Semester Pendek/SP) — itu pun hanya penanda tambahan,
 * bukan diskriminator: baris SP tetap punya {@code jenisPengajuan} sendiri, dan tampilan SP
 * dikendalikan konfigurasi aplikasi {@code terdapat_pengajuan_mahasiswa_sp} (lihat
 * {@code KonfigurasiNewAction}), bukan oleh entity ini.</p>
 *
 * <h2>Padanan di jenjang sekolah</h2>
 *
 * <p>Kelas ini punya kembaran hampir identik untuk jenjang sekolah:
 * {@link ais.database.model.sekolah.PengajuanSiswa}. Keduanya dilayani oleh listener yang sama,
 * {@code ais.action.master.helper.ParameterTambahanPengajuanListener}, yang memilih cabang
 * berdasarkan field mana yang tidak {@code null}. Bila memperbaiki perilaku di sini, periksa
 * apakah perbaikan yang sama juga dibutuhkan di sana.</p>
 *
 * <h2>Rantai pewarisan &amp; pengulangan field yang DISENGAJA</h2>
 *
 * <p>{@code PengajuanMahasiswa} → {@link DataSop} → {@link GeneralValueObject}.
 * {@link DataSop} hanya menambahkan kontrak abstrak {@code getDisposisiSop()}/
 * {@code setDisposisiSop(...)} sehingga seluruh dokumen ber-alur SOP dapat diperlakukan seragam
 * oleh mesin disposisi.</p>
 *
 * <p><b>PENTING — jangan "dibersihkan":</b> {@link GeneralValueObject} <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO abstrak biasa. Hibernate
 * karena itu <b>tidak</b> memetakan properti milik induk. Deklarasi ulang {@link #id},
 * {@link #oleh}, {@link #olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi
 * yang keliru</b>, melainkan keharusan teknis agar kolom-kolom tersebut ikut terpetakan.
 * Menghapusnya akan membuat kolom audit dasar hilang dari tabel. Lihat
 * {@link GeneralValueObject} untuk penjelasan lengkap pola ini.</p>
 *
 * <h2>Relasi utama</h2>
 *
 * <ul>
 *   <li>{@link #getMahasiswa()} → {@link Mahasiswa}, pemohon (kolom {@code mahasiswa},
 *       {@code nullable = true} — jadi baris tanpa mahasiswa secara teknis mungkin ada).</li>
 *   <li>{@link #getJenisPengajuan()} → {@link JenisPengajuan}, jenis permohonan (kolom
 *       {@code jenis_pengajuan}, {@code nullable = false}).</li>
 *   <li>{@link #getDisposisiSop()} → {@link DisposisiSop}, simpul alur persetujuan berjalan.</li>
 *   <li>{@link #getDisetujuiOleh()} → {@link Tbmuser}, penyetuju — <b>diturunkan</b> dari
 *       disposisi, lihat catatan di method-nya.</li>
 *   <li>Lampiran tidak dipetakan sebagai relasi Hibernate, melainkan dicari lewat
 *       {@link LampiranLain#ambil(Long, String)} dengan kunci
 *       {@code "<kelompokId>-&gt;<parameterId>"}.</li>
 * </ul>
 *
 * <h2>Status persetujuan: JANGAN andalkan satu flag</h2>
 *
 * <p>Ada tiga sumber informasi status yang berbeda dan mudah tertukar:</p>
 * <ol>
 *   <li>{@link #getPersetujuan()} — flag boolean yang <b>diisi operator dari form</b>
 *       ("butuh persetujuan"/"disetujui" tergantung layar); defaultnya {@code true}.</li>
 *   <li>{@link #getDisposisiSop()} — status <b>sebenarnya</b> menurut mesin alur SOP.
 *       {@link #getDisetujuiOleh()} dan {@link #getSetujuiTanggal()} <b>menimpa</b> nilai
 *       kolomnya sendiri dari disposisi ini setiap kali dipanggil.</li>
 *   <li>{@link #getAktif()} — {@code false} bila disposisi tidak aktif <i>atau</i> alur berhenti
 *       di simpul penolakan; pola ini identik dengan entity akunting ber-SOP lain (mis.
 *       {@code KasKecil}, {@code DanaTalangan}).</li>
 * </ol>
 *
 * <h2>Penomoran agenda/surat</h2>
 *
 * <p>Empat properti bekerja sama: {@link #getKode()} (nomor agenda hasil format
 * {@code NomorSurat}), {@link #getIndex()} (nomor urut mentah), serta {@link #getTahun()} dan
 * {@link #getBulan()} yang <b>bukan sekadar informasi</b> melainkan kolom filter yang dipakai
 * {@code PengajuanMahasiswaAction.getindex(...)} untuk mereset urutan tiap tahun/bulan. Entity
 * ini tidak membangkitkan nomor sendiri; seluruh logikanya ada di action.</p>
 *
 * <h2>Parameter tambahan dinamis</h2>
 *
 * <p>Field isian tambahan per jenis pengajuan tidak dipetakan sebagai kolom, melainkan
 * disimpan sebagai <b>dua kolom teks</b> berisi baris-baris yang dipisah {@code "\n"} dan
 * kolom-kolom yang dipisah {@code "&lt;=&gt;"}:</p>
 * <ul>
 *   <li>{@link #getParameterTambahan()} — versi <i>manusiawi</i>, 7 kolom:
 *       {@code namaKelompok->labelInputan <=> nilai <=> urlLampiran <=> nomorUrut <=>
 *       parameterId <=> kelompokId <=> keterangan}.</li>
 *   <li>{@link #getParameterTambahanInds()} — versi <i>berbasis id</i>, 4 kolom:
 *       {@code kelompokId->parameterId <=> nilai <=> urlLampiran <=> keterangan}. Dipakai saat
 *       membangun ulang form supaya isian lama terpasang kembali ke komponen yang tepat.</li>
 * </ul>
 * <p>Keduanya ditulis sekaligus oleh {@link #populateParameterTambahan(List)} dan dibaca
 * kembali oleh {@link #ambilDataParameterTambahan()}. Pola dua kolom kembar ini dipakai luas di
 * repo (mis. {@code CatatanMahasiswa}, {@code PerbaikanAsset}, {@code CutiDanIzin}).</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ol>
 *   <li><b>Jejak audit dasar</b> (deklarasi ulang wajib): {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}.</li>
 *   <li><b>Identitas permohonan</b>: {@link #getJenisPengajuan()}, {@link #getMahasiswa()},
 *       {@link #getKeterangan()}, {@link #getKode()}, {@link #getIndex()}.</li>
 *   <li><b>Konteks akademik</b>: {@link #getTahunAkademik()}, {@link #getGanjilGenap()},
 *       {@link #getSemester()}, {@link #getTahap()}, {@link #getSemesterPendek()}.</li>
 *   <li><b>Waktu</b>: {@link #getTanggal()}, {@link #getTanggalSelesai()},
 *       {@link #getWaktuMulai()}, {@link #getWaktuSelesai()}, {@link #getTahun()},
 *       {@link #getBulan()}, {@link #toTglDanWaktu()}.</li>
 *   <li><b>Persetujuan/SOP</b>: {@link #getPersetujuan()}, {@link #getDisposisiSop()},
 *       {@link #getDisetujuiOleh()}, {@link #getSetujuiTanggal()}, {@link #getAktif()}.</li>
 *   <li><b>Parameter tambahan</b>: {@link #getParameterTambahan()},
 *       {@link #getParameterTambahanInds()}, {@link #ambilDataParameterTambahan()},
 *       {@link #populateParameterTambahan(List)}.</li>
 * </ol>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 *
 * <ul>
 *   <li><b>Getter kelas ini bukan getter polos.</b> Karena {@code @Id} dipasang di getter,
 *       Hibernate memakai <i>property access</i>: apa pun yang ditulis sebuah getter ke field
 *       akan ikut ter-flush ke database bila entity masih ter-attach. Getter yang menulis:
 *       {@link #getMahasiswa()}, {@link #getJenisPengajuan()}, {@link #getDisposisiSop()}
 *       (menulis balik hasil resolusi proxy), {@link #getSemester()}, {@link #getPersetujuan()},
 *       {@link #getTanggal()}, {@link #getWaktuMulai()}, {@link #getParameterTambahan()},
 *       {@link #getParameterTambahanInds()}, {@link #getDisetujuiOleh()},
 *       {@link #getSetujuiTanggal()}, {@link #getTahun()}, {@link #getBulan()},
 *       {@link #getAktif()}. Membaca daftar pengajuan di layar <b>bisa mengubah isi tabel</b>.
 *       {@link #getTahunAkademik()} adalah satu-satunya pengecualian: ia mengembalikan nilai
 *       bawaan tanpa menyimpannya.</li>
 *   <li><b>Tidak ada getter di kelas ini yang membuka atau menutup session Hibernate sendiri</b>
 *       (kelas ini tidak menyebut {@code Session} maupun {@code HibernateUtil} sama sekali).
 *       Pembukaan/penutupan session tersembunyi terjadi di dalam
 *       {@link GeneralValueObject#check(Object)} yang dipanggil getter relasi — lihat dokumentasi
 *       method tersebut untuk biaya dan jebakannya.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif, sehingga hanya kolom yang benar-benar
 *       berubah yang masuk ke SQL — ini memperkecil, tapi tidak menghilangkan, dampak efek
 *       samping getter di atas.</li>
 *   <li>{@code @Audited} (Hibernate Envers) menyalin setiap perubahan ke tabel bayangan
 *       {@code new_audit.pengajuan_mahasiswa__audit}. Kolom {@code keterangan} pada kedua tabel
 *       dipaksa bertipe {@code text} oleh {@code ais.common.DatabaseTextColumnSchemaFix
 *       #initPengajuanMahasiswa()} saat aplikasi start.</li>
 * </ul>
 *
 * @see JenisPengajuan
 * @see Mahasiswa
 * @see DisposisiSop
 * @see ais.database.model.sekolah.PengajuanSiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengajuan_mahasiswa")
public class PengajuanMahasiswa extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan dengan beberapa entity lain hasil
	 * generate Hibernate Tools (mis. {@link JenisPengajuan}); jangan diubah agar object yang
	 * sudah ter-serialisasi di session ZK lama tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, kolom {@code id}, dibangkitkan database ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (jejak audit sederhana). */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini (jejak audit sederhana). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (guard di awal method), sehingga nilai lama tidak bisa dikosongkan lewat setter ini.
	 * Ini pola seragam di seluruh entity repo agar interceptor audit tidak menghapus jejak
	 * yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum
	 * {@code UPDATE} dijalankan, dan mendelegasikan pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Jangan dipanggil manual dari kode aplikasi. Perhatikan bahwa deklarasi field
	 * {@code tanggal_dirubah} sengaja ditempel pada baris yang sama oleh perkakas audit
	 * otomatis repo ini — nilai awalnya {@link WaktuUtil#getDate()} sehingga baris baru pun
	 * sudah punya stempel waktu sebelum sempat di-{@code update}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pengisian manual hanya dilakukan
	 * saat migrasi/impor data.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk object baru
	 *         karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini, dipakai antara lain sebagai label komponen ZK.
	 *
	 * <p><b>Kuirk:</b> mengembalikan {@link #keterangan} apa adanya — jadi bisa
	 * mengembalikan {@code null} bila keterangan belum diisi (kolomnya {@code nullable}).
	 * Pemanggil yang merangkai string sebaiknya tidak menganggap hasilnya selalu non-null.</p>
	 *
	 * @return isi keterangan permohonan, mungkin {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Membentuk kunci pengurutan gabungan tanggal + jam mulai + jam selesai + id, dalam bentuk
	 * satu string yang bisa dibandingkan secara leksikografis.
	 *
	 * <p>Formatnya adalah sambungan tanpa pemisah dari: tanggal {@code yyyyMMdd} (atau
	 * {@code "00000000"} bila {@link #getTanggal()} {@code null}), {@link #getWaktuMulai()}
	 * (atau {@code "00.00"}), {@link #getWaktuSelesai()} (atau {@code "00.00"}), lalu
	 * {@link #getId()} (atau {@code "0"}).</p>
	 *
	 * <p><b>Catatan hasil penelusuran:</b> pola ini disalin dari {@code Pertemuan#toTglDanWaktu()}
	 * yang di sana menjadi dasar {@code Comparator}. Di kelas ini method tersebut
	 * <b>tidak dipanggil dari mana pun</b> dan {@code PengajuanMahasiswa} juga tidak
	 * mengimplementasikan {@code Comparable}, sehingga praktis merupakan API yang belum terpakai.
	 * Selain itu, karena id disambung tanpa padding, urutan leksikografis akan salah begitu
	 * panjang digit id berbeda (mis. {@code "...9"} &gt; {@code "...10"}). Perbaiki bila suatu
	 * saat method ini benar-benar dipakai untuk mengurutkan.</p>
	 *
	 * @return kunci pengurutan gabungan; tidak pernah {@code null}
	 */
	public String toTglDanWaktu() {
		String tgl = getTanggal() == null ? "00000000" : Common.dateFormat8.get().format(getTanggal());
		tgl += getWaktuMulai() == null ? "00.00" : getWaktuMulai();
		tgl += getWaktuSelesai() == null ? "00.00" : getWaktuSelesai();
		tgl += getId() == null ? "0" : getId().toString();
		return tgl;
	}

	/** Nomor agenda/surat hasil format {@code NomorSurat} milik {@link JenisPengajuan}. */
	private String kode;

	/** Mahasiswa pemohon. */
	private Mahasiswa mahasiswa;
	/** Semester mahasiswa saat mengajukan; dihitung ulang oleh {@link #getSemester()}. */
	private Integer semester;
	/** Tahap/termin permohonan, bebas dipakai per jenis pengajuan. */
	private Integer tahap;
	/** Uraian/alasan permohonan (kolom {@code text}). */
	private String keterangan;
	/** Tahun akademik, format {@code "2025/2026"}. */
	private String tahunAkademik;
	/** Penanda semester ganjil/genap; nilainya mengikuti konstanta {@code Perkuliahan}. */
	private String ganjilGenap;
	/** Flag persetujuan manual dari form (default {@code true}). */
	private Boolean persetujuan;
	/** Penanda bahwa permohonan berkaitan dengan Semester Pendek (SP). */
	private Boolean semesterPendek;
	/** Tanggal permohonan; diinisialisasi ke hari ini saat object dibuat. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal berakhir permohonan (mis. akhir masa cuti/izin). */
	private Date tanggalSelesai;
	/** Jenis permohonan — inilah yang menentukan makna baris ini. */
	private JenisPengajuan jenisPengajuan;
	/** Jam selesai, string bebas format {@code HH.mm}. */
	private String waktuSelesai;
	/** Jam mulai, string bebas format {@code HH.mm}. */
	private String waktuMulai;
	/** Simpul alur SOP yang sedang/terakhir memproses permohonan ini. */
	private DisposisiSop disposisiSop;
	/** Isian parameter tambahan versi manusiawi (7 kolom per baris). */
	private String parameterTambahan;
	/** Isian parameter tambahan versi berbasis id (4 kolom per baris). */
	private String parameterTambahanInds;
	/** Waktu persetujuan; <b>diturunkan ulang</b> dari SOP oleh {@link #getSetujuiTanggal()}. */
	private Date setujuiTanggal;
	/** Penyetuju; <b>diturunkan ulang</b> dari SOP oleh {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujiOleh;
	/** Nomor urut mentah untuk penomoran agenda. */
	private Long index;
	/** Tahun acuan penomoran; diisi otomatis oleh {@link #getTahun()}. */
	private Integer tahun;
	/** Bulan acuan penomoran (1-12); diisi otomatis oleh {@link #getBulan()}. */
	private Integer bulan;
	/** Status aktif; dapat dipaksa {@code false} oleh {@link #getAktif()} berdasarkan SOP. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Perhatikan bahwa beberapa field sudah berisi nilai bawaan lewat inisialisasi field
	 * ({@link #tanggal} dan {@code tanggal_dirubah} diisi {@link WaktuUtil#getDate()}), jadi
	 * object baru tidak sepenuhnya kosong.</p>
	 */
	public PengajuanMahasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolomnya {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}/sequence), jadi {@code null} berarti "belum pernah disimpan" — kondisi
	 * itu dipakai sebagai penanda "record baru" oleh {@link #getWaktuMulai()} dan oleh
	 * {@code PengajuanMahasiswaAction}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Hanya dipakai Hibernate dan kode migrasi/impor.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nomor urut mentah penomoran agenda.
	 *
	 * <p>Diisi oleh {@code PengajuanMahasiswaAction#onSave()} dari hasil
	 * {@code getindex(JenisPengajuan)}. Nilai {@code null} berarti baris lama yang dibuat
	 * sebelum fitur penomoran ada — action akan membangkitkan nomornya saat baris itu disimpan
	 * ulang.</p>
	 *
	 * @param index nomor urut mentah
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut mentah penomoran agenda.
	 *
	 * @return nomor urut, atau {@code null} bila baris belum pernah diberi nomor
	 * @see #getKode()
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan uraian/alasan permohonan.
	 *
	 * <p>Kolomnya dipaksa bertipe {@code text} (bukan {@code varchar(255)}) oleh
	 * {@code ais.common.DatabaseTextColumnSchemaFix#initPengajuanMahasiswa()} saat aplikasi
	 * start, karena alasan permohonan memang boleh panjang.</p>
	 *
	 * @return uraian permohonan, mungkin {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan uraian/alasan permohonan.
	 *
	 * @param keterangan uraian permohonan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan mahasiswa pemohon.
	 *
	 * @param mahasiswa mahasiswa pemohon
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan mahasiswa pemohon, dengan resolusi proxy lazy lebih dulu.
	 *
	 * <p><b>Efek samping:</b> hasil {@link GeneralValueObject#check(Object)} <b>ditugaskan
	 * kembali</b> ke field {@link #mahasiswa}. Itu disengaja (pola standar seluruh entity repo
	 * ini) supaya proxy yang sudah teresolusi tidak diresolusi ulang pada pemanggilan
	 * berikutnya. Karena pemetaan memakai <i>property access</i>, penggantian object di sini
	 * juga menjadi bagian dari state yang diperiksa Hibernate saat flush.</p>
	 *
	 * <p>Relasinya {@code LAZY} dengan cascade {@code PERSIST}/{@code MERGE} pada kolom
	 * {@code mahasiswa} yang {@code nullable}.</p>
	 *
	 * @return mahasiswa pemohon, mungkin {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan semester mahasiswa saat mengajukan.
	 *
	 * <p>Nilai yang diset di sini mudah tertimpa: {@link #getSemester()} menghitung ulang
	 * setiap kali dipanggil selama data pendukungnya lengkap.</p>
	 *
	 * @param semester nomor semester
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan semester mahasiswa saat permohonan diajukan — <b>dihitung ulang</b>, bukan
	 * sekadar dibaca dari kolom.
	 *
	 * <p><b>Cara kerja.</b> Bila {@link #mahasiswa}, {@link #ganjilGenap}, dan
	 * {@link #tahunAkademik} ketiganya terisi, method mengambil tahun awal dari tahun akademik
	 * (bagian sebelum {@code "/"}), lalu memanggil
	 * {@code Common.getSemester(tahunAngkatan, ganjilGenap, pindahKeKampusIniMasukSemester,
	 * tahun, semesterMulai)} dan <b>menimpa field {@link #semester}</b> dengan hasilnya. Bila
	 * salah satu data pendukung kosong, nilai kolom yang tersimpan dikembalikan apa adanya.</p>
	 *
	 * <p><b>Efek samping:</b> karena field ditimpa dan pemetaan memakai property access,
	 * membaca semester pada entity yang masih ter-attach dapat menyebabkan {@code UPDATE}
	 * kolom {@code semester} saat flush.</p>
	 *
	 * <p><b>Jebakan yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li>Method mengakses field {@link #mahasiswa} <b>langsung</b>, bukan lewat
	 *       {@link #getMahasiswa()}, sehingga proxy lazy tidak diresolusi lebih dulu. Pada
	 *       entity yang sudah <i>detached</i> hal ini bisa memicu
	 *       {@code LazyInitializationException}.</li>
	 *   <li>{@code Integer.parseInt(...)} tidak dibungkus {@code try}: tahun akademik yang
	 *       tidak berformat {@code "yyyy/yyyy"} akan melempar
	 *       {@code NumberFormatException}/{@code ArrayIndexOutOfBoundsException} dari dalam
	 *       sebuah getter.</li>
	 * </ul>
	 *
	 * @return nomor semester (hasil hitung ulang bila memungkinkan), mungkin {@code null}
	 * @see Common#getSemester(Integer, String, Integer, Integer, String)
	 */
	public Integer getSemester() {
		if (mahasiswa != null && ganjilGenap != null && tahunAkademik != null) {
			final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String ta = tahunAkademik;
			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			semester = Common.getSemester(tahunAngkatanMhs, ganjilGenap, mahasiswa.getPindahKeKampusIniMasukSemester(),
					tahun, mahasiswa.getSemesterMulai());
		}

		return semester;
	}

	/**
	 * Mengembalikan tahun akademik permohonan, dengan nilai bawaan tahun akademik berjalan.
	 *
	 * <p><b>Perhatikan perbedaan dengan getter lain di kelas ini:</b> bila kolomnya {@code null},
	 * method mengembalikan {@code Common.getCurrentTahunAkademik()} <b>tanpa menuliskannya
	 * kembali</b> ke field. Jadi nilai bawaan ini hanya tampil di layar dan tidak ikut tersimpan
	 * — berbeda dari {@link #getTahun()}, {@link #getBulan()}, atau {@link #getPersetujuan()}
	 * yang menulis balik. Konsekuensinya kolom {@code tahun_akademik} bisa tetap {@code null} di
	 * database walaupun layar selalu menampilkan sebuah nilai, dan {@link #getSemester()}
	 * (yang membaca field mentah) tidak ikut memanfaatkan nilai bawaan ini.</p>
	 *
	 * @return tahun akademik format {@code "2025/2026"}; tidak pernah {@code null} selama
	 *         konfigurasi tahun akademik berjalan terisi
	 */
	@Column(name = "tahun_akademik", nullable = true)
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik permohonan.
	 *
	 * @param tahunAkademik tahun akademik format {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan penanda semester ganjil/genap permohonan.
	 *
	 * <p>Nilainya dipakai {@link #getSemester()} sebagai salah satu masukan perhitungan.</p>
	 *
	 * @return penanda ganjil/genap, mungkin {@code null}
	 */
	@Column(name = "ganjil_genap", nullable = true)
	public String getGanjilGenap() {
		return ganjilGenap;
	}

	/**
	 * Menetapkan penanda semester ganjil/genap permohonan.
	 *
	 * @param ganjilGenap penanda ganjil/genap
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * Mengembalikan flag persetujuan manual, dengan nilai bawaan {@code true}.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code null}, method <b>menulis</b>
	 * {@code true} ke field, sehingga baris lama yang belum punya nilai akan ikut ter-{@code
	 * update} menjadi {@code true} begitu dibaca oleh entity yang ter-attach.</p>
	 *
	 * <p><b>Jangan tertukar:</b> flag ini adalah isian operator dari form (lihat
	 * {@code PengajuanMahasiswaAction#onSave()}), <b>bukan</b> status persetujuan menurut alur
	 * SOP. Status yang mengikat ada di {@link #getDisposisiSop()} dan turunannya
	 * ({@link #getDisetujuiOleh()}, {@link #getSetujuiTanggal()}, {@link #getAktif()}).</p>
	 *
	 * @return {@code true}/{@code false}; tidak pernah {@code null}
	 */
	public Boolean getPersetujuan() {
		if (persetujuan == null) {
			persetujuan = true;
		}
		return persetujuan;
	}

	/**
	 * Menetapkan flag persetujuan manual.
	 *
	 * @param persetujuan flag persetujuan
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Mengembalikan tanggal permohonan (presisi {@code DATE}).
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, field diisi <b>dan disimpan</b>
	 * dengan {@link #getTanggal_dirubah()} sebagai pengganti. Untuk object baru hal ini nyaris
	 * tak pernah terjadi karena {@link #tanggal} sudah diinisialisasi hari ini saat konstruksi;
	 * jalur ini terutama menyentuh baris lama hasil impor yang tanggalnya kosong.</p>
	 *
	 * <p>Kolom {@code tanggal} juga dipakai sebagai filter oleh
	 * {@code PengajuanMahasiswaAction#getindex(...)} untuk aturan "reset urutan tiap ...", dan
	 * oleh laporan {@code LaporanPengajuan}.</p>
	 *
	 * @return tanggal permohonan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = getTanggal_dirubah();
		}
		return tanggal;
	}

	/**
	 * Menetapkan tanggal permohonan.
	 *
	 * @param tanggal tanggal permohonan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tahap/termin permohonan.
	 *
	 * <p>Maknanya bergantung jenis pengajuan (mis. tahap ke berapa sebuah permohonan
	 * bertingkat); entity ini tidak memberi arti khusus pada angkanya.</p>
	 *
	 * @return nomor tahap, mungkin {@code null}
	 */
	public Integer getTahap() {
		return tahap;
	}

	/**
	 * Menetapkan tahap/termin permohonan.
	 *
	 * @param tahap nomor tahap
	 */
	public void setTahap(Integer tahap) {
		this.tahap = tahap;
	}

	/**
	 * Mengembalikan penanda permohonan berkaitan Semester Pendek (SP), dengan bawaan
	 * {@code false}.
	 *
	 * <p>Berbeda dengan {@link #getPersetujuan()}, nilai bawaan di sini <b>tidak</b> ditulis
	 * balik ke field — hanya dikembalikan. Kemunculan kolom SP di layar dikendalikan
	 * konfigurasi aplikasi {@code terdapat_pengajuan_mahasiswa_sp}.</p>
	 *
	 * @return {@code true} bila permohonan terkait SP; tidak pernah {@code null}
	 */
	public Boolean getSemesterPendek() {
		return semesterPendek == null ? false : semesterPendek;
	}

	/**
	 * Menetapkan penanda permohonan Semester Pendek.
	 *
	 * @param semesterPendek {@code true} bila permohonan terkait SP
	 */
	public void setSemesterPendek(Boolean semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/**
	 * Mengembalikan jenis permohonan, dengan resolusi proxy lazy lebih dulu.
	 *
	 * <p>Inilah relasi yang menentukan <b>makna</b> baris ini: nama jenis, aturan penomoran
	 * surat, dan daftar kelompok parameter tambahan yang harus diisi semuanya berasal dari
	 * {@link JenisPengajuan}. Kolom {@code jenis_pengajuan} bersifat {@code nullable = false}.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@link GeneralValueObject#check(Object)} ditulis balik ke
	 * field {@link #jenisPengajuan} (pola standar repo, sengaja).</p>
	 *
	 * @return jenis permohonan
	 * @see JenisPengajuan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengajuan", nullable = false)
	public JenisPengajuan getJenisPengajuan() {
		jenisPengajuan = check(jenisPengajuan);
		return jenisPengajuan;
	}

	/**
	 * Menetapkan jenis permohonan.
	 *
	 * @param jenisPengajuan jenis permohonan
	 */
	public void setJenisPengajuan(JenisPengajuan jenisPengajuan) {
		this.jenisPengajuan = jenisPengajuan;
	}

	/**
	 * Mengembalikan tanggal berakhirnya permohonan (presisi {@code DATE}).
	 *
	 * <p>Dipakai jenis pengajuan yang punya rentang waktu (mis. cuti/izin). Getter polos —
	 * tidak ada nilai bawaan dan tidak ada efek samping.</p>
	 *
	 * @return tanggal selesai, mungkin {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Menetapkan tanggal berakhirnya permohonan.
	 *
	 * @param tanggalSelesai tanggal selesai
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Menetapkan jam mulai, dinormalisasi: string kosong/spasi disimpan sebagai {@code null},
	 * selain itu dipangkas spasinya.
	 *
	 * @param waktuMulai jam mulai (mis. {@code "08.00"}); kosong dianggap {@code null}
	 */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Mengembalikan jam mulai permohonan.
	 *
	 * <p><b>Efek samping khusus record baru:</b> bila {@link #getId()} masih {@code null}
	 * (baris belum pernah tersimpan) dan jam mulai belum diisi, field <b>diisi dengan jam
	 * sekarang</b> ({@code Common.timeFormat2} atas {@link WaktuUtil#getDate()}). Jadi sekadar
	 * menampilkan form pengajuan baru sudah menetapkan jam mulai; setelah baris punya id,
	 * pengisian otomatis ini tidak berlaku lagi.</p>
	 *
	 * @return jam mulai yang sudah dipangkas, atau {@code null} bila kosong
	 */
	@Column(name = "waktu_mulai", length = 20)
	public String getWaktuMulai() {
		if (getId() == null && (waktuMulai == null || waktuMulai.trim().equals(""))) {
			waktuMulai = Common.timeFormat2.get().format(WaktuUtil.getDate());
		}
		return waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Menetapkan jam selesai, dinormalisasi sama seperti {@link #setWaktuMulai(String)}.
	 *
	 * @param waktuSelesai jam selesai; kosong dianggap {@code null}
	 */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Mengembalikan jam selesai permohonan.
	 *
	 * <p>Tidak seperti {@link #getWaktuMulai()}, getter ini <b>tidak</b> mengisi nilai bawaan —
	 * hanya menormalkan string kosong menjadi {@code null}.</p>
	 *
	 * @return jam selesai yang sudah dipangkas, atau {@code null} bila kosong
	 */
	@Column(name = "waktu_selesai", length = 20)
	public String getWaktuSelesai() {

		return waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Mengembalikan isian parameter tambahan dalam bentuk <b>berbasis id</b>.
	 *
	 * <p>Formatnya satu baris per parameter, dipisah {@code "\n"}, dengan empat kolom yang
	 * dipisah {@code "&lt;=&gt;"}:</p>
	 * <pre>
	 * kelompokId-&gt;parameterId &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
	 * </pre>
	 *
	 * <p>Bentuk ini dipakai saat form dibangun ulang, supaya nilai lama bisa dipasang kembali
	 * ke komponen yang tepat berdasarkan id — pola yang sama dipakai banyak entity lain
	 * (mis. {@code CatatanMahasiswa}, {@code PerbaikanAsset}, {@code CutiDanIzin}).</p>
	 *
	 * <p><b>Efek samping:</b> nilai {@code null} dinormalkan menjadi string kosong dan
	 * <b>ditulis balik</b> ke field, sehingga baris lama bisa ter-{@code update} dari
	 * {@code NULL} menjadi {@code ''} hanya karena dibaca.</p>
	 *
	 * @return isian berbasis id; tidak pernah {@code null} (minimal string kosong)
	 * @see #getParameterTambahan()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menetapkan isian parameter tambahan bentuk berbasis id.
	 *
	 * <p>Umumnya tidak dipanggil langsung; {@link #populateParameterTambahan(List)} yang
	 * mengisinya bersamaan dengan {@link #setParameterTambahan(String)}. Bila diisi manual,
	 * pastikan kedua kolom tetap konsisten satu sama lain.</p>
	 *
	 * @param parameterTambahanInds isian berbasis id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} (bentuk manusiawi) menjadi daftar
	 * {@link CommonVO} siap tampil/rekap, terurut.
	 *
	 * <p><b>Pemetaan kolom → properti {@code CommonVO}:</b></p>
	 * <table border="1" summary="pemetaan kolom parameter tambahan ke CommonVO">
	 *   <tr><th>Indeks kolom</th><th>Isi</th><th>Disimpan ke</th></tr>
	 *   <tr><td>0</td><td>{@code namaKelompok->labelInputan}</td><td>{@code name}</td></tr>
	 *   <tr><td>1</td><td>nilai isian</td><td>{@code name1}</td></tr>
	 *   <tr><td>2</td><td>URL lampiran</td><td>{@code name2}</td></tr>
	 *   <tr><td>3</td><td>nomor urut</td><td>{@code nomorUrut}</td></tr>
	 *   <tr><td>4</td><td>id {@link ParameterTambahan}</td><td>{@code id}</td></tr>
	 * </table>
	 * <p>Selain itu {@code name5} diisi bagian sebelum {@code "->"} dari kolom 0, yaitu nama
	 * kelompok parameter. Nomor urut dan id dibungkus {@code try/catch} sehingga data rusak
	 * jatuh ke nilai bawaan {@code 1} alih-alih melempar exception.</p>
	 *
	 * <p><b>Pengurutan.</b> {@code Collections.sort} memakai {@code CommonVO#compareTo}: karena
	 * {@code name5} di sini hampir selalu terisi, urutannya memakai perbandingan
	 * <b>string</b> {@code "namaKelompok nomorUrut"}. Efeknya nomor urut dibandingkan secara
	 * leksikografis, sehingga {@code 10} muncul sebelum {@code 2} bila satu kelompok punya
	 * lebih dari sembilan parameter.</p>
	 *
	 * <p><b>Kuirk yang perlu diantisipasi pemanggil.</b></p>
	 * <ul>
	 *   <li>Bila belum ada isian sama sekali, {@link #getParameterTambahan()} mengembalikan
	 *       string kosong dan {@code "".split("\n")} menghasilkan array berisi satu elemen
	 *       kosong. Method ini karena itu mengembalikan <b>satu {@code CommonVO} kosong</b>,
	 *       bukan list kosong. Pemanggil yang menghitung jumlah parameter harus menyaringnya
	 *       sendiri.</li>
	 *   <li>Kolom 5 (id kelompok) dan kolom 6 (keterangan) yang ditulis
	 *       {@link #populateParameterTambahan(List)} <b>tidak diurai</b> di sini; keduanya hanya
	 *       tersedia lewat {@link #getParameterTambahanInds()}.</li>
	 * </ul>
	 *
	 * @return daftar parameter tambahan terurut; tidak pernah {@code null}, tetapi bisa berisi
	 *         satu elemen kosong seperti dijelaskan di atas
	 * @see #populateParameterTambahan(List)
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengajuanMahasiswa.java:304");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PengajuanMahasiswa.java:310");

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
	 * Memanen nilai seluruh baris form parameter tambahan yang sedang tampil di layar ZK, lalu
	 * <b>menulis ulang kedua kolom teks</b> {@link #setParameterTambahanInds(String)} dan
	 * {@link #setParameterTambahan(String)}.
	 *
	 * <p><b>Dipanggil dari mana.</b> Dari
	 * {@code ais.action.master.helper.ParameterTambahanPengajuanListener#onSave(PengajuanMahasiswa)},
	 * yang sendiri dipanggil {@code PengajuanMahasiswaAction#onSave()} tepat sebelum entity
	 * disimpan. Baris-baris {@link Row} yang dioper sudah dibangun listener tersebut dan
	 * membawa dua atribut penting: {@code "parameterTambahan"} ({@link ParameterTambahan}) dan
	 * {@code "kelompokParameterTambahanPengajuan"}
	 * ({@link KelompokParameterTambahanPengajuan}); baris tanpa keduanya dilewati.</p>
	 *
	 * <p><b>Yang dikerjakan per baris.</b></p>
	 * <ol>
	 *   <li>Menyusun kunci {@code jenis = "<kelompokId>-&gt;<parameterId>"}.</li>
	 *   <li>Mengambil nilai isian lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)}
	 *       (menangani semua tipe komponen: textbox, combobox, datebox, dan seterusnya).</li>
	 *   <li>Bila parameter mewajibkan lampiran, mencari berkasnya lewat
	 *       {@link LampiranLain#ambil(Long, String)} dengan {@code ref = getId()} dan kunci
	 *       {@code jenis}, lalu menyimpan {@code createLinkUri()}-nya sebagai URL.</li>
	 *   <li>Merangkai satu baris untuk masing-masing kolom teks (7 kolom untuk bentuk
	 *       manusiawi, 4 kolom untuk bentuk berbasis id).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping.</b> Kedua kolom ditimpa total — isian lama yang barisnya tidak lagi
	 * tampil di form akan hilang. Ini disengaja: form selalu merender ulang seluruh parameter
	 * milik jenis pengajuan yang dipilih.</p>
	 *
	 * <p><b>Jebakan yang perlu diketahui.</b></p>
	 * <ul>
	 *   <li>Pada pengajuan <b>baru</b> {@link #getId()} masih {@code null}, sehingga pencarian
	 *       lampiran memakai {@code ref} {@code null} dan URL lampiran umumnya keluar kosong
	 *       pada penyimpanan pertama; URL baru terisi pada penyuntingan berikutnya.</li>
	 *   <li>Setiap baris dibungkus {@code try/catch} yang hanya memanggil
	 *       {@code Common.tampilErrorJikaAdmin(e)}. Bagi pengguna non-admin kegagalan satu baris
	 *       <b>senyap</b> dan nilai parameter tersebut hilang tanpa peringatan.</li>
	 *   <li>Variabel lokal {@code parameterTambahanInds} sengaja menutupi (shadow) field dengan
	 *       nama sama; penulisan ke field baru terjadi lewat setter di akhir method.</li>
	 * </ul>
	 *
	 * @param parameterRows baris-baris form ZK yang membawa komponen isian; {@code null} atau
	 *                      kosong membuat method tidak melakukan apa-apa (kolom lama dibiarkan
	 *                      utuh)
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
				KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan = (KelompokParameterTambahanPengajuan) row
						.getAttribute("kelompokParameterTambahanPengajuan");
				if (parameterTambahan != null && kelompokParameterTambahanPengajuan != null) {
					String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();

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

					String s = kelompokParameterTambahanPengajuan.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanPengajuan.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + (keterangan == null ? "" : keterangan.getValue().trim());
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
	 * Mengembalikan isian parameter tambahan dalam bentuk <b>manusiawi</b> (siap dicetak/rekap).
	 *
	 * <p>Formatnya satu baris per parameter, dipisah {@code "\n"}, dengan tujuh kolom yang
	 * dipisah {@code "&lt;=&gt;"}:</p>
	 * <pre>
	 * namaKelompok-&gt;labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut
	 *   &lt;=&gt; parameterId &lt;=&gt; kelompokId &lt;=&gt; keterangan
	 * </pre>
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getParameterTambahanInds()}, nilai
	 * {@code null} dinormalkan menjadi string kosong dan ditulis balik ke field.</p>
	 *
	 * @return isian bentuk manusiawi; tidak pernah {@code null} (minimal string kosong)
	 * @see #ambilDataParameterTambahan()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menetapkan isian parameter tambahan bentuk manusiawi.
	 *
	 * <p>Umumnya diisi {@link #populateParameterTambahan(List)}; bila diisi manual, jaga
	 * konsistensinya dengan {@link #setParameterTambahanInds(String)}.</p>
	 *
	 * @param parameterTambahan isian bentuk manusiawi
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan simpul alur SOP yang sedang/terakhir memproses permohonan ini, dengan
	 * resolusi proxy lazy lebih dulu.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()} sehingga permohonan ini
	 * dapat diproses mesin disposisi bersama dokumen ber-SOP lainnya. Dari relasi inilah
	 * {@link #getDisetujuiOleh()}, {@link #getSetujuiTanggal()}, dan {@link #getAktif()}
	 * menurunkan nilainya.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@link GeneralValueObject#check(Object)} ditulis balik ke
	 * field {@link #disposisiSop}.</p>
	 *
	 * @return simpul disposisi SOP, mungkin {@code null} bila permohonan belum masuk alur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan simpul alur SOP permohonan ini.
	 *
	 * <p><b>Guard yang penting:</b> argumen {@code null} atau disposisi yang belum punya id
	 * <b>diabaikan diam-diam</b>. Artinya tautan disposisi yang sudah ada tidak dapat dilepas
	 * lewat setter ini — disengaja, supaya penyimpanan ulang form tidak sampai memutus jejak
	 * alur persetujuan yang sedang berjalan.</p>
	 *
	 * <p><b>Catatan pemeliharaan:</b> ekspresi ternary di badan method merupakan sisa versi
	 * lama dan kini merupakan kode mati — setelah guard di atas, kondisi
	 * {@code (disposisiSop == null || disposisiSop.getId() == null)} tidak mungkin bernilai
	 * benar, sehingga cabang yang mempertahankan nilai lama tidak pernah tereksekusi dan
	 * penugasan selalu memakai argumen. Dibiarkan apa adanya di sini karena tugas dokumentasi
	 * tidak boleh mengubah logika.</p>
	 *
	 * @param disposisiSop simpul disposisi baru; diabaikan bila {@code null} atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui permohonan — <b>diturunkan ulang dari alur SOP</b>,
	 * bukan sekadar dibaca dari kolom {@code disetuji_oleh}.
	 *
	 * <p><b>Cara kerja.</b> Setelah resolusi proxy lazy, method memeriksa
	 * {@link #getDisposisiSop()}:</p>
	 * <ul>
	 *   <li>bila disposisi punya simpul "setuju" ({@code getDisposisiSetuju()}) yang sudah ada
	 *       pengajunya, nilai field <b>ditimpa</b> dengan pengaju simpul tersebut;</li>
	 *   <li>bila disposisi ada tetapi simpul setuju belum ada / belum ada pengajunya, field
	 *       <b>ditimpa {@code null}</b>.</li>
	 * </ul>
	 * <p>Bila {@link #getDisposisiSop()} sendiri {@code null}, nilai kolom dibiarkan apa
	 * adanya.</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari.</b> Karena pemetaan memakai property access,
	 * penimpaan di atas ikut ter-flush. Pada permohonan yang sudah masuk alur SOP tetapi belum
	 * disetujui, sekadar menampilkan datanya akan <b>mengosongkan</b> kolom
	 * {@code disetuji_oleh} di database — termasuk bila kolom itu pernah diisi manual atau
	 * berasal dari data lama sebelum alur SOP dipakai. Nilai yang tampil karena itu selalu
	 * mengikuti SOP, dan {@link #setDisetujuiOleh(Tbmuser)} tidak akan bertahan bila permohonan
	 * punya disposisi.</p>
	 *
	 * @return pengguna penyetuju menurut alur SOP, atau {@code null} bila belum disetujui
	 * @see #getSetujuiTanggal()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujiOleh = null;
		}

		return disetujiOleh;
	}

	/**
	 * Menetapkan pengguna penyetuju.
	 *
	 * <p>Perhatikan bahwa nilai ini akan ditimpa {@link #getDisetujuiOleh()} begitu permohonan
	 * punya {@link DisposisiSop}; setter ini praktis hanya berpengaruh pada baris tanpa
	 * disposisi (mis. data hasil impor).</p>
	 *
	 * @param disetujiOleh pengguna penyetuju
	 */
	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	/**
	 * Mengembalikan waktu persetujuan — <b>diturunkan ulang dari alur SOP</b> dengan pola yang
	 * persis sama seperti {@link #getDisetujuiOleh()}.
	 *
	 * <p>Bila simpul "setuju" pada {@link #getDisposisiSop()} sudah ada pengajunya, field
	 * ditimpa dengan {@code getWaktu()} simpul tersebut; bila disposisi ada tetapi belum
	 * disetujui, field ditimpa {@code null}. Bila tidak ada disposisi sama sekali, nilai kolom
	 * dibiarkan.</p>
	 *
	 * <p><b>Efek samping identik:</b> penimpaan ikut ter-flush, sehingga membaca data bisa
	 * mengosongkan kolom {@code setujui_tanggal} pada permohonan yang belum disetujui. Waktu
	 * yang dikembalikan adalah waktu <b>disposisi persetujuan</b>, bukan waktu perubahan baris
	 * ini ({@link #getTanggal_dirubah()}).</p>
	 *
	 * @return waktu persetujuan menurut alur SOP, atau {@code null} bila belum disetujui
	 * @see #getDisetujuiOleh()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			setujuiTanggal = getDisposisiSop().getDisposisiSetuju().getWaktu();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			setujuiTanggal = null;
		}

		return setujuiTanggal;
	}

	/**
	 * Menetapkan waktu persetujuan.
	 *
	 * <p>Seperti {@link #setDisetujuiOleh(Tbmuser)}, nilainya akan ditimpa
	 * {@link #getSetujuiTanggal()} pada permohonan yang punya disposisi SOP.</p>
	 *
	 * @param setujuiTanggal waktu persetujuan
	 */
	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	/**
	 * Mengembalikan nomor agenda/surat permohonan.
	 *
	 * <p>Nilainya dibangkitkan {@code PengajuanMahasiswaAction} dari konfigurasi
	 * {@code NomorSurat} milik {@link JenisPengajuan} (lihat {@code generateCode(...)}), bukan
	 * oleh entity ini. Getter polos tanpa nilai bawaan — permohonan lama yang dibuat sebelum
	 * fitur penomoran ada bisa saja {@code null}, dan action akan mengisinya saat baris itu
	 * disimpan ulang.</p>
	 *
	 * @return nomor agenda, mungkin {@code null}
	 * @see #getIndex()
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan nomor agenda/surat permohonan.
	 *
	 * @param kode nomor agenda hasil format {@code NomorSurat}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan tahun acuan penomoran, dengan nilai bawaan tahun berjalan.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, field diisi tahun sekarang
	 * ({@link WaktuUtil#getCalendar()}) <b>dan ikut tersimpan</b> saat flush. Untuk baris lama
	 * hasil impor, ini berarti tahun yang tercatat adalah tahun saat data itu pertama kali
	 * dibaca, bukan tahun permohonannya.</p>
	 *
	 * <p>Kolom ini bukan sekadar informasi: {@code PengajuanMahasiswaAction#getindex(...)}
	 * memfilternya untuk aturan "reset urutan tiap tahun".</p>
	 *
	 * @return tahun acuan penomoran; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun acuan penomoran.
	 *
	 * @param tahun tahun acuan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan bulan acuan penomoran (1-12), dengan nilai bawaan bulan berjalan.
	 *
	 * <p><b>Efek samping</b> dan konsekuensinya sama seperti {@link #getTahun()}: bila kolomnya
	 * {@code null}, field diisi bulan sekarang dan ikut tersimpan. Perhatikan penambahan
	 * {@code + 1} — {@link Calendar#MONTH} berbasis nol, sedangkan kolom ini menyimpan
	 * 1 = Januari.</p>
	 *
	 * <p>Dipakai {@code PengajuanMahasiswaAction#getindex(...)} untuk aturan "reset urutan tiap
	 * bulan".</p>
	 *
	 * @return bulan acuan penomoran, 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan bulan acuan penomoran.
	 *
	 * @param bulan bulan acuan, 1-12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan status aktif permohonan, dengan <b>koreksi otomatis berdasarkan alur SOP</b>.
	 *
	 * <p><b>Cara kerja.</b> Field {@link #aktif} dipaksa {@code false} bila salah satu dari dua
	 * kondisi berikut terpenuhi:</p>
	 * <ol>
	 *   <li>{@link #getDisposisiSop()} ada tetapi disposisinya sendiri sudah tidak aktif;</li>
	 *   <li>alur berhenti di simpul akhir yang ditandai sebagai titik penolakan
	 *       ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}) — dengan kata lain
	 *       permohonan <b>ditolak</b>.</li>
	 * </ol>
	 * <p>Bila kolomnya masih {@code null}, hasilnya dianggap {@code true}. Pola persis ini
	 * dipakai berulang di entity ber-SOP lain (mis. {@code KasKecil}, {@code KasBesar},
	 * {@code DanaTalangan}, {@code PenggantianKasKecil}, {@code DaftarPengajuanTransfer}), jadi
	 * perubahan perilaku di sini kemungkinan besar juga relevan di sana.</p>
	 *
	 * <p><b>Efek samping &amp; sifat satu arah.</b> Penimpaan menulis ke field yang dipetakan,
	 * sehingga membaca daftar permohonan dapat menyimpan {@code aktif = false} ke database.
	 * Perhatikan bahwa method ini <b>tidak pernah mengembalikan nilai ke {@code true}</b>: bila
	 * disposisi kemudian diaktifkan lagi atau penolakan dibatalkan, kolom {@code aktif} tetap
	 * {@code false} sampai ada yang memanggil {@link #setAktif(Boolean)} secara eksplisit.
	 * Baris pertama ({@code disposisiSop = getDisposisiSop();}) hanyalah penugasan ulang hasil
	 * resolusi proxy ke field yang sama — tidak mengubah perilaku.</p>
	 *
	 * @return {@code true} bila permohonan masih berjalan/berlaku; {@code false} bila
	 *         disposisinya nonaktif atau permohonan ditolak. Tidak pernah {@code null}
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
	 * Menetapkan status aktif permohonan.
	 *
	 * <p>Satu-satunya cara mengembalikan status menjadi {@code true} setelah
	 * {@link #getAktif()} memaksanya {@code false}.</p>
	 *
	 * @param aktif status aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
