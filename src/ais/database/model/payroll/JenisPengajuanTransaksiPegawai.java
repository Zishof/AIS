package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashMap;
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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.surat.NomorSurat;

/**
 * Katalog master <b>Jenis Pengajuan Transaksi Pegawai</b> &mdash; daftar kategori dokumen pengajuan
 * transaksi payroll pegawai (kasbon, pinjaman, potongan, dan sejenisnya) untuk modul
 * {@link PengajuanTransaksiPegawai}. Tabel <code>payroll.jenis_pengajuan_transaksi_pegawai</code>.
 * Satu baris di sini adalah satu "jenis" yang tampil di combobox layar pengajuan
 * ({@code ais.action.master.payroll.PengajuanTransaksiPegawaiAction}) dan sekaligus membawa <b>tiga
 * konfigurasi berbeda</b> yang berlaku untuk seluruh dokumen pengajuan yang memilihnya:</p>
 * <ol>
 *   <li><b>Atribusi akun jurnal</b> &mdash; {@link #getJenisTransaksiPegawai()}, rujukan ke katalog
 *       {@link ais.database.model.payroll.JenisTransaksiPegawai} yang sesungguhnya memegang pasangan
 *       akun debet/kredit;</li>
 *   <li><b>Skema penomoran agenda</b> &mdash; {@link #getNomorSurat()}, format nomor dokumen yang
 *       dipakai layar pengajuan untuk membangkitkan {@link PengajuanTransaksiPegawai#getKode()};</li>
 *   <li><b>Isian dinamis</b> &mdash; {@link #getKelompokParameterTambahanPengajuanTransaksiPegawais()},
 *       daftar seksi field kustom yang harus tampil pada formulir pengajuan berjenis ini.</li>
 * </ol>
 *
 * <h3>1. {@link #getJenisTransaksiPegawai()} TERVERIFIKASI bukan kosmetik</h3>
 * <p>Berbeda dari kolom <code>jenisTransaksi</code> pada {@link ais.database.model.payroll.JenisTransaksiPegawai}
 * (yang terkonfirmasi murni label dasbor), relasi {@code jenisTransaksiPegawai} pada kelas <b>ini</b>
 * adalah penentu akun jurnal yang <i>sesungguhnya</i> dipakai untuk hampir seluruh transaksi pegawai
 * yang lahir dari sebuah pengajuan. Rantainya dikonfirmasi dari
 * {@code TransaksiPegawai.getJenisTransaksiPegawai()} (getter destruktif):</p>
 * <pre>{@code
 * public JenisTransaksiPegawai getJenisTransaksiPegawai() {
 *     jenisTransaksiPegawai = check(jenisTransaksiPegawai);
 *     pengajuanTransaksiPegawai = getPengajuanTransaksiPegawai();
 *     if (pengajuanTransaksiPegawai != null && pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai() != null
 *             && pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai() != null) {
 *         jenisTransaksiPegawai = pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai()
 *                 .getJenisTransaksiPegawai();
 *     }
 *     return jenisTransaksiPegawai;
 * }
 * }</pre>
 * <p>Jadi setiap baris {@code TransaksiPegawai} yang lahir dari {@code populateTransaksi()} (lihat
 * dokumentasi {@link PengajuanTransaksiPegawai}) akan <b>menimpa</b> field lokalnya sendiri dengan
 * nilai yang diambil lewat rantai <code>pengajuan &rarr; jenisPengajuanTransaksiPegawai
 * &rarr; jenisTransaksiPegawai</code> setiap kali dibaca, selama field ini pada baris katalog terisi.
 * Mengubah {@link #setJenisTransaksiPegawai(JenisTransaksiPegawai)} pada baris katalog jenis
 * pengajuan karena itu <b>memindahkan akun buku besar</b> untuk seluruh angsuran &mdash; termasuk yang
 * sudah diposting &mdash; dari seluruh dokumen pengajuan yang memakai jenis ini, persis seperti efek
 * mengubah akun langsung pada {@link ais.database.model.payroll.JenisTransaksiPegawai} itu sendiri.</p>
 *
 * <h3>2. {@link #getKode()}: terhubung ke combobox, tapi tak pernah diisi</h3>
 * <p>Layar CRUD katalog ini ({@code ais.action.master.payroll.JenisPengajuanTransaksiPegawaiAction})
 * hanya menyediakan isian untuk {@code nama}, {@code jenisTransaksiPegawai}, {@code nomorSurat}, dan
 * {@code keterangan} &mdash; <b>tidak ada kotak isian untuk {@code kode}</b>, dan tak satu pun
 * pemanggil {@code setKode(...)} pada kelas ini ditemukan di seluruh repo (diverifikasi ulang lewat
 * pencarian menyeluruh). Namun kolom ini <b>ikut dipetakan</b> ke label combobox pemilih jenis pada
 * layar pengajuan:</p>
 * <pre>{@code
 * Common.insertCombo(jenisPengajuanTransaksiPegawai, new String[] { "nama", "kode" }, "keterangan",
 *         JenisPengajuanTransaksiPegawai.class, Restrictions.eq("aktif", true));
 * }</pre>
 * <p>{@code CommonComboInsertHelper} melewati properti yang nilainya kosong saat menyusun label, jadi
 * dalam praktiknya label combobox selalu jatuh ke {@code nama} saja &mdash; komponen {@code kode}
 * tersambung secara arsitektural tetapi tidak pernah berkontribusi apa pun karena tak ada jalur tulis
 * yang mengisinya. Kolom ini juga tidak masuk daftar kolom cetak/impor Excel milik layar CRUD-nya
 * sendiri. Ringkasnya: {@code kode} adalah field yang hidup (dipetakan, dibaca reflektif) tetapi
 * secara efektif tidur untuk katalog ini &mdash; berbeda dari {@link PengajuanTransaksiPegawai#getKode()}
 * pada dokumen pengajuannya sendiri, yang aktif dipakai sebagai nomor agenda.</p>
 *
 * <h3>3. Relasi {@code @ManyToMany} ke isian dinamis</h3>
 * <p>{@link #getKelompokParameterTambahanPengajuanTransaksiPegawais()} memetakan tabel penghubung
 * <code>payroll.jenis_pengajuan_transaksi_pegawai_has_parameter</code> ke
 * {@link KelompokParameterTambahanPengajuanTransaksiPegawai} (lihat javadoc kelas itu untuk rantai
 * 4 lapis lengkap sampai ke {@link ais.database.model.ParameterTambahan}). Pola ini identik dengan
 * pasangan sejenis pada modul lain yang sudah diverifikasi (Pengajuan/CatatanPegawai/CatatanMahasiswa/
 * Alumni/GajiPegawai): baris katalog jenis ini memilih <i>seksi mana</i> yang muncul pada formulir,
 * lewat dua konsumen terverifikasi &mdash;
 * {@code JenisPengajuanTransaksiPegawaiAction.initKelompokParameterTambahanPengajuanTransaksiPegawai()}
 * (grid checkbox konfigurasi di layar CRUD katalog ini) dan
 * {@code PengajuanTransaksiPegawaiAction} (perender seksi isian dinamis pada layar pengajuan dan pada
 * tampilan cetak/revisi dokumen).</p>
 * <p><b>Cache statis {@link #mapParameters}.</b> Getter relasi ini memakai peta statis
 * {@code Map<Long, Set<...>>} sebagai <i>hot cache</i> di luar siklus hidup Hibernate: sekali
 * {@link #setKelompokParameterTambahanPengajuanTransaksiPegawais(Set)} dipanggil untuk id tertentu,
 * nilai itu ditaruh ke peta statis dan getter berikutnya &mdash; untuk instance manapun dengan id yang
 * sama &mdash; akan mengembalikan isi peta itu, menimpa nilai field milik instance yang sedang dibaca.
 * Ini adalah <b>idiom yang berulang di &ge;16 kelas katalog sejenis</b> di seluruh repo (verifikasi
 * grep: {@code AlurSop}, {@code JenisCatatanSiswa}, {@code JenisCatatanKelasSiswa},
 * {@code JenisCatatanGuru}, {@code JenisGajiPegawai}, {@code JenisPerbaikanAsset},
 * {@code JenisTransaksiKoperasi}, {@code JenisPengajuanPegawai}, {@code JenisPengajuan},
 * {@code JenisPengaduan}, {@code JenisCatatanPegawai}, {@code JenisCatatanMahasiswa},
 * {@code JenisCatatanAdministrasi}, {@code JenisCutiDanIzin}, dan lainnya) &mdash; bukan penyimpangan
 * khusus kelas ini, melainkan pola arsitektur AIS yang mapan untuk relasi many-to-many pada katalog
 * jenis yang dimuat lewat {@link ais.common.InitData}. Konsekuensinya tetap perlu disadari: peta ini
 * berumur JVM (tidak pernah dibersihkan) dan dibagi <b>lintas seluruh sesi/pengguna</b> &mdash; sebuah
 * perubahan yang disimpan satu admin langsung terlihat oleh pembaca lain tanpa perlu query ulang,
 * tetapi juga berarti tidak ada isolasi per-transaksi Hibernate atas nilai yang disajikan getter ini.</p>
 *
 * <h3>4. Cakupan tenant dan permukaan akses</h3>
 * <p>Sama seperti {@link ais.database.model.payroll.JenisTransaksiPegawai}: <b>tidak ada sumbu
 * tenant</b> (tidak ada kolom {@code satuanKerja}/{@code sekolah}/{@code yayasan}) &mdash; satu katalog
 * global dipakai bersama seluruh instalasi. Tidak ditemukan pendaftaran halaman New UI
 * ({@code nuiServiceEntities}) untuk entity ini di seluruh berkas JSP repo (verifikasi negatif),
 * sehingga permukaan akses satu-satunya adalah layar ZK legacy
 * {@code JenisPengajuanTransaksiPegawaiAction}, yang menggerbangi tombol Tambah/Ubah/Hapus dan
 * checkbox "Aktif" pada grid dengan {@code CommonPrivilages.checkPrevilages(...)} &mdash; pola
 * "checkbox grid tanpa gerbang" ({@code task_0a06e418}) <b>tidak</b> berlaku di sini (verifikasi
 * negatif).</p>
 *
 * <h3>5. Siklus hidup data</h3>
 * <p>Kelas ini terdaftar di {@code InitData.initClasses(...)}, sehingga seluruh barisnya dimuat ke
 * cache memori JVM saat bootstrap dan dibaca lewat instance kanonik via
 * {@link GeneralValueObject#check(Object)}. Baris dinonaktifkan lewat {@link #getAktif()}, bukan
 * dihapus; seluruh combobox konsumen menyaring dengan
 * <code>Restrictions.or(isNull("aktif"), eq("aktif", true))</code>, sejalan dengan nilai bawaan
 * {@code true} pada getternya.</p>
 *
 * <h3>6. Pemetaan Hibernate dan warisan {@link GeneralValueObject}</h3>
 * <p>Entity dipetakan <i>property-access</i> (anotasi {@code @Id} berada pada {@link #getId()}).
 * {@link GeneralValueObject} bukan {@code @Entity} maupun {@code @MappedSuperclass}, sehingga field
 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib dideklarasikan ulang</b> di sini
 * agar ikut dipetakan &mdash; pengulangan ini keharusan teknis, bukan bug. Entity ber-{@code @Audited}
 * (Envers): setiap perubahan menghasilkan baris revisi di skema audit.</p>
 *
 * <h3>7. Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; penyajian:</b> {@link #getId()}, {@link #setId(Long)},
 *       {@link #toString()}.</li>
 *   <li><b>Atribut katalog:</b> {@link #getKode()}, {@link #setKode(String)}, {@link #getNama()},
 *       {@link #setNama(String)}, {@link #getKeterangan()}, {@link #setKeterangan(String)},
 *       {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 *   <li><b>Konfigurasi dokumen pengajuan:</b> {@link #getJenisTransaksiPegawai()}
 *       (akun jurnal, lihat butir 1), {@link #setJenisTransaksiPegawai(JenisTransaksiPegawai)},
 *       {@link #getNomorSurat()} (skema penomoran), {@link #setNomorSurat(NomorSurat)},
 *       {@link #getKelompokParameterTambahanPengajuanTransaksiPegawais()} (isian dinamis, lihat
 *       butir 3), {@link #setKelompokParameterTambahanPengajuanTransaksiPegawais(Set)}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, dan kait {@code onUpdate()}.</li>
 * </ul>
 *
 * @see PengajuanTransaksiPegawai
 * @see ais.database.model.payroll.JenisTransaksiPegawai
 * @see KelompokParameterTambahanPengajuanTransaksiPegawai
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "jenis_pengajuan_transaksi_pegawai")
public class JenisPengajuanTransaksiPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap peninggalan generator hbm2java 2010; jangan diubah agar
	 * object yang sudah terlanjur diserialkan (session HTTP, cache) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama IDENTITY; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyentuh baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang membuat/mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor}.</p>
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh interceptor.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun
	 * string kosong/spasi &mdash; nilai lama dipertahankan. Pola ini seragam untuk seluruh entity AIS
	 * dan disengaja agar stempel audit tidak terhapus oleh jalur simpan yang tidak menyertakan
	 * konteks pengguna.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan tanpa efek dan tanpa exception.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong <b>diabaikan
	 * diam-diam</b> sehingga stempel audit lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan tanpa efek.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang membuat/mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum pernyataan {@code UPDATE}
	 * dieksekusi, meneruskan entity ini ke {@code AuditTimestampInterceptor.ubah(this)} yang
	 * memperbarui {@link #getTanggal_dirubah()} serta stempel {@code oleh}/{@code olehId} dari
	 * konteks pengguna aktif. Tidak dipanggil pada {@code INSERT} &mdash; nilai awal
	 * {@code tanggal_dirubah} datang dari inisialisasi field pada deklarasi di baris yang sama.
	 * Jangan panggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi &mdash; pengisiannya diserahkan ke {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolom mengikuti nama properti apa adanya
	 * ({@code tanggal_dirubah}). Dipetakan {@code TemporalType.TIMESTAMP}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object baru di memori.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris katalog dalam bentuk <code>"id-nama"</code> (tanpa spasi di sekitar
	 * tanda hubung, berbeda dari {@link ais.database.model.payroll.JenisTransaksiPegawai#toString()}
	 * yang berformat <code>"kode - nama"</code>). Dipakai sebagai fallback label pada
	 * {@code RevisiHelper.createNewRevisi(...)} dan pemanggil generik lain yang membutuhkan
	 * representasi ringkas object ini; tidak diverifikasi ikut masuk ke teks jurnal permanen manapun
	 * (berbeda dari {@code JenisTransaksiPegawai.toString()}).
	 *
	 * @return gabungan id dan nama dipisah tanda hubung tanpa spasi; dapat memuat literal
	 *         {@code "null"} untuk baris yang belum tersimpan atau belum bernama.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat katalog; lihat {@link #getKode()} &mdash; dipetakan tapi tidak pernah diisi (butir 2). */
	private String kode;

	/** Nama katalog yang tampil di combobox pemilih jenis pengajuan; lihat {@link #getNama()}. */
	private String nama;
	/** Catatan bebas operator; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Bendera aktif/nonaktif katalog; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Skema penomoran agenda dokumen pengajuan berjenis ini; lihat {@link #getNomorSurat()}. */
	private NomorSurat nomorSurat;

	/**
	 * Cache statis JVM-wide berkunci id baris katalog, menyimpan hasil
	 * {@link #setKelompokParameterTambahanPengajuanTransaksiPegawais(Set)} paling akhir untuk id
	 * tersebut. Dibaca (dan ditulis balik ke field instance) oleh
	 * {@link #getKelompokParameterTambahanPengajuanTransaksiPegawais()} &mdash; lihat butir 3 pada
	 * javadoc kelas untuk penjelasan lengkap idiom ini dan daftar &ge;16 kelas sejenis yang
	 * memakainya. Peta ini tidak pernah dibersihkan/dihapus isinya sepanjang umur JVM, dan dibagi
	 * bersama lintas seluruh sesi/pengguna karena bersifat {@code static}.
	 */
	public static Map<Long, Set<KelompokParameterTambahanPengajuanTransaksiPegawai>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanPengajuanTransaksiPegawai>>();

	/**
	 * Isi field seumpama tanpa cache statis aktif; lihat
	 * {@link #getKelompokParameterTambahanPengajuanTransaksiPegawais()} untuk interaksinya dengan
	 * {@link #mapParameters}. Diinisialisasi {@link TreeSet} kosong (bukan {@code null}) agar aman
	 * diiterasi sebelum entity pernah dimuat/disimpan.
	 */
	private Set<KelompokParameterTambahanPengajuanTransaksiPegawai> kelompokParameterTambahanPengajuanPegawais = new TreeSet<KelompokParameterTambahanPengajuanTransaksiPegawai>();
	/** Katalog jenis transaksi &mdash; pembawa akun jurnal sesungguhnya; lihat {@link #getJenisTransaksiPegawai()}. */
	private JenisTransaksiPegawai jenisTransaksiPegawai;

	/**
	 * Mengembalikan daftar seksi field kustom ({@link KelompokParameterTambahanPengajuanTransaksiPegawai})
	 * yang aktif untuk jenis pengajuan ini &mdash; menentukan isian dinamis apa saja yang wajib
	 * dirender pada formulir {@link PengajuanTransaksiPegawai} berjenis ini.
	 *
	 * <h4>Pemetaan tabel penghubung</h4>
	 * <p>Relasi {@code @ManyToMany} lewat {@code @JoinTable} eksplisit ke
	 * <code>payroll.jenis_pengajuan_transaksi_pegawai_has_parameter</code>, dengan kolom join
	 * <code>jenis_catatan_administrasi</code> (sisi ini) dan <code>parameter</code> (sisi
	 * {@link KelompokParameterTambahanPengajuanTransaksiPegawai}). Nama kolom
	 * <code>jenis_catatan_administrasi</code> adalah jejak salin-tempel dari modul lain (bukan bug
	 * fungsional &mdash; Hibernate hanya memakai nama itu untuk dirinya sendiri), sejalan dengan
	 * catatan salin-tempel serupa yang sudah didokumentasikan di
	 * {@link KelompokParameterTambahanPengajuanTransaksiPegawai}. Diurutkan
	 * {@code nomorUrut asc, nama asc} lewat {@code @OrderBy}, dan {@code cascade = MERGE} sehingga
	 * menyimpan baris katalog ini ikut menggabungkan perubahan pada baris kelompok yang sudah
	 * ditempel &mdash; tidak mem-<i>persist</i> kelompok baru yang belum pernah disimpan.</p>
	 *
	 * <h4>Cache statis {@link #mapParameters} &mdash; getter destruktif ringan</h4>
	 * <p>Sebelum mengembalikan nilai, getter ini memeriksa peta statis {@link #mapParameters}: bila
	 * ada entri untuk {@link #getId() id} baris ini, entri itu <b>menimpa</b> field instance
	 * {@code kelompokParameterTambahanPengajuanPegawais} sebelum dikembalikan. Karena peta ini
	 * bersifat {@code static} (dibagi lintas seluruh instance kelas, sesi, dan pengguna JVM), dua
	 * instance Java yang berbeda untuk id katalog yang sama akan mengembalikan <i>Set</i> yang identik
	 * begitu salah satunya pernah di-set lewat
	 * {@link #setKelompokParameterTambahanPengajuanTransaksiPegawais(Set)} &mdash; termasuk lintas
	 * request/pengguna berbeda, tanpa perlu {@code session.refresh()}. Untuk baris yang belum pernah
	 * tersimpan ({@code id == null}) atau belum pernah lewat setter tersebut, nilai yang dikembalikan
	 * adalah field instance apa adanya (bawaan {@code TreeSet} kosong untuk object baru).</p>
	 * <p>Konsumen terverifikasi: {@code JenisPengajuanTransaksiPegawaiAction} (grid checkbox
	 * konfigurasi jenis) dan {@code PengajuanTransaksiPegawaiAction} (perender seksi isian dinamis
	 * pada tampilan cetak/revisi dokumen pengajuan) &mdash; keduanya memanggil
	 * {@code session.refresh(jenis)} sesaat sebelum membaca koleksi ini untuk memaksa sinkronisasi
	 * dengan database, namun getter tetap dapat menimpa hasil refresh itu dengan isi cache statis
	 * bila entrinya ada.</p>
	 *
	 * @return set kelompok parameter tambahan aktif; tidak pernah {@code null} (minimal {@link TreeSet}
	 *         kosong), diurutkan {@code nomorUrut, nama}.
	 * @see #mapParameters
	 * @see #setKelompokParameterTambahanPengajuanTransaksiPegawais(Set)
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanPengajuanTransaksiPegawai.class, cascade = {
			CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_pengajuan_transaksi_pegawai_has_parameter", schema = "payroll", joinColumns = @JoinColumn(name = "jenis_catatan_administrasi"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanPengajuanTransaksiPegawai> getKelompokParameterTambahanPengajuanTransaksiPegawais() {
		if (id != null) {
			Set<KelompokParameterTambahanPengajuanTransaksiPegawai> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanPengajuanPegawais = temp;
			}
		}
		return kelompokParameterTambahanPengajuanPegawais;
	}

	/**
	 * Menetapkan daftar seksi field kustom untuk jenis pengajuan ini, sekaligus memperbarui
	 * {@link #mapParameters}.
	 *
	 * <p>Bila {@link #getId() id} baris ini sudah terisi, nilai yang diberikan langsung ditulis ke
	 * peta statis {@link #mapParameters} dengan id ini sebagai kunci &mdash; sehingga perubahan
	 * <b>langsung terlihat</b> oleh getter manapun yang membaca id yang sama, di sesi/pengguna
	 * manapun, sebelum entity ini sendiri di-<i>flush</i> ke database. Untuk baris baru
	 * ({@code id == null}, mis. saat form "Tambah" belum disimpan), nilai hanya ditulis ke field
	 * instance dan belum masuk cache statis; entri baru akan ditambahkan pada pemanggilan berikutnya
	 * setelah id terisi (lihat alur {@code JenisPengajuanTransaksiPegawaiAction.onSave()}, yang
	 * memanggil setter ini <i>sebelum</i> {@code Common.refreshSaveOrUpdate(...)} menyimpan baris).</p>
	 *
	 * @param kelompokParameterTambahanPengajuanPegawais set kelompok parameter tambahan baru; tidak
	 *                                                    boleh {@code null} (dipakai langsung tanpa
	 *                                                    guard null).
	 * @see #getKelompokParameterTambahanPengajuanTransaksiPegawais()
	 */
	public void setKelompokParameterTambahanPengajuanTransaksiPegawais(
			Set<KelompokParameterTambahanPengajuanTransaksiPegawai> kelompokParameterTambahanPengajuanPegawais) {
		this.kelompokParameterTambahanPengajuanPegawais = kelompokParameterTambahanPengajuanPegawais;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanPengajuanPegawais);
		}
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Seluruh field dibiarkan {@code null}
	 * kecuali {@code tanggal_dirubah} (diisi pada deklarasinya) dan
	 * {@code kelompokParameterTambahanPengajuanPegawais} (diinisialisasi {@link TreeSet} kosong).
	 * Object hasil konstruktor ini akan tampak "aktif" bila dibaca lewat {@link #getAktif()} karena
	 * getter itu memberi nilai bawaan {@code true}.
	 */
	public JenisPengajuanTransaksiPegawai() {
	}

	/**
	 * Mengembalikan kunci utama baris katalog ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} (dibangkitkan database) dan {@code insertable = false}. Nilai
	 * {@code null} dipakai layar CRUD sebagai penanda "baris baru" dan oleh
	 * {@link #getKelompokParameterTambahanPengajuanTransaksiPegawais()}/
	 * {@link #setKelompokParameterTambahanPengajuanTransaksiPegawais(Set)} untuk memutuskan apakah
	 * cache statis {@link #mapParameters} boleh disentuh.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Dipakai Hibernate setelah {@code INSERT}; kode aplikasi umumnya tidak
	 * memanggilnya.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat katalog, sudah di-{@code trim()}.
	 *
	 * <p><b>Terverifikasi tidak pernah diisi.</b> Layar CRUD katalog ini
	 * ({@code JenisPengajuanTransaksiPegawaiAction}) tidak menyediakan kotak isian untuk kolom ini,
	 * dan tak satu pun pemanggil {@link #setKode(String)} pada kelas ini ditemukan di seluruh repo.
	 * Kolom ini tetap ikut dipetakan ke label combobox pemilih jenis pada layar pengajuan
	 * (<code>Common.insertCombo(..., new String[] { "nama", "kode" }, ...)</code>), tetapi karena
	 * nilainya selalu kosong, label yang tampil dalam praktiknya hanya berisi {@code nama}. Lihat
	 * butir 2 pada javadoc kelas untuk uraian lengkap.</p>
	 *
	 * @return kode katalog yang sudah dipangkas spasi; string kosong (bukan {@code null}) bila
	 *         kolomnya belum pernah diisi.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menetapkan kode singkat katalog. Tidak ada validasi di sini. Lihat {@link #getKode()} mengenai
	 * ketiadaan pemanggil setter ini di seluruh repo.
	 *
	 * @param kode kode katalog; boleh {@code null}.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama katalog jenis pengajuan, sudah di-{@code trim()}.
	 *
	 * <p>Kolom {@code NOT NULL}, dipetakan {@code columnDefinition = "text"} sehingga tidak terbatas
	 * 255 karakter (berbeda dari {@code JenisTransaksiPegawai.getNama()} yang dibatasi 255). Inilah
	 * satu-satunya komponen yang efektif membentuk label combobox pemilih jenis pada layar pengajuan
	 * (lihat {@link #getKode()}), dan menjadi fallback nama dokumen pada
	 * {@code PengajuanTransaksiPegawai.getNama()} ketika field lokal dokumen masih kosong.</p>
	 *
	 * @return nama katalog yang sudah dipangkas spasi, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama katalog. Wajib terisi (kolom {@code NOT NULL}); layar CRUD menolak simpan bila
	 * kosong.
	 *
	 * @param nama nama katalog.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas operator untuk baris katalog ini.
	 *
	 * <p>Murni deskriptif; ditampilkan sebagai kolom grid pada layar CRUD dan sebagai {@code deskripsi}
	 * (tooltip/deskripsi item) pada combobox pemilih jenis di layar pengajuan
	 * (<code>Common.insertCombo(..., "keterangan", ...)</code>). Tidak dibaca mesin posting maupun
	 * mesin isian dinamis.</p>
	 *
	 * @return keterangan; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas operator.
	 *
	 * @param keterangan keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif katalog, dengan <b>nilai bawaan {@code true}</b> bila kolomnya
	 * {@code null}.
	 *
	 * <p>Bawaan ini sejalan dengan cara seluruh konsumen menyaring katalog:
	 * {@code PengajuanTransaksiPegawaiAction} dan {@code JenisPengajuanTransaksiPegawaiAction} memakai
	 * <code>Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))</code> saat
	 * mengisi combobox dan menyaring grid pencarian. Nonaktif berarti jenis ini tidak lagi bisa
	 * dipilih untuk pengajuan baru; tidak menyembunyikan atau mengubah perilaku dokumen lama yang
	 * sudah memakainya. Tanpa {@code @Column}, sehingga kolomnya bernama {@code aktif} apa adanya.</p>
	 *
	 * @return {@code true} bila katalog aktif atau kolomnya belum pernah diisi; tidak pernah
	 *         {@code null}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif katalog.
	 *
	 * <p>Dipanggil dari <i>listener</i> {@code onCheck} pada checkbox "Aktif" di grid layar CRUD
	 * (langsung menyimpan lewat {@code Common.refreshSaveOrUpdate}, sehingga satu klik langsung
	 * berefek ke database tanpa tombol Simpan). Checkbox tersebut digerbangi
	 * {@code checkbox.setDisabled(!edit)} dengan {@code edit} berasal dari
	 * {@code CommonPrivilages.checkPrevilages(UPDATE)} &mdash; verifikasi negatif untuk pola
	 * "checkbox grid tanpa gerbang" ({@code task_0a06e418}).</p>
	 *
	 * @param aktif status aktif; {@code null} akan terbaca sebagai {@code true} oleh getternya.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan katalog {@link ais.database.model.payroll.JenisTransaksiPegawai} yang menentukan
	 * pasangan akun jurnal untuk seluruh transaksi pegawai yang lahir dari dokumen pengajuan berjenis
	 * ini.
	 *
	 * <p><b>TERVERIFIKASI bukan kosmetik.</b> Lihat butir 1 pada javadoc kelas: relasi ini dibaca
	 * hidup oleh {@code TransaksiPegawai.getJenisTransaksiPegawai()} setiap kali baris transaksi
	 * turunan pengajuan dibaca, dan hasilnya <b>menimpa</b> field lokal {@code TransaksiPegawai}
	 * tersebut. Mesin posting ({@code PostingTransaksiPegawaiAction}) kemudian mengambil
	 * {@code getAkunDebet()}/{@code getAkun()} dari hasil rantai ini. Mengubah field ini pada baris
	 * katalog jenis pengajuan karena itu memindahkan atribusi buku besar untuk seluruh angsuran yang
	 * belum diposting dari <b>seluruh</b> dokumen pengajuan yang memakai jenis ini &mdash; berlaku
	 * retroaktif tanpa jejak di dokumen manapun, sama seperti mengubah akun langsung pada
	 * {@link ais.database.model.payroll.JenisTransaksiPegawai} itu sendiri.</p>
	 * <p>Kolom {@code nullable = false} pada level anotasi, dan layar CRUD
	 * ({@code JenisPengajuanTransaksiPegawaiAction.onSave()}) menolak simpan bila belum dipilih.
	 * Relasi {@code @ManyToOne} LAZY dengan {@code cascade = {PERSIST, MERGE}}; penugasan kembali
	 * ({@code jenisTransaksiPegawai = check(jenisTransaksiPegawai)}) adalah resolusi proxy lazy
	 * standar {@link GeneralValueObject#check(Object)}, bukan penimpaan nilai bisnis.</p>
	 *
	 * @return katalog jenis transaksi pegawai; tidak boleh {@code null} pada baris yang tersimpan
	 *         benar, tetapi tidak divalidasi oleh constraint database (kolomnya sendiri
	 *         {@code nullable = false} pada anotasi Hibernate, bukan pada skema fisik yang
	 *         diverifikasi di sini).
	 * @see ais.database.model.payroll.JenisTransaksiPegawai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_transaksi_pegawai", nullable = false)
	public JenisTransaksiPegawai getJenisTransaksiPegawai() {
		jenisTransaksiPegawai = check(jenisTransaksiPegawai);
		return jenisTransaksiPegawai;
	}

	/**
	 * Menetapkan katalog jenis transaksi pegawai yang menentukan akun jurnal.
	 *
	 * <p><b>Efek retroaktif.</b> Berlaku peringatan yang sama dengan
	 * {@link ais.database.model.payroll.JenisTransaksiPegawai#setAkun(ais.database.model.akunting.Akun)}:
	 * karena nilainya dibaca hidup oleh rantai turunan {@code TransaksiPegawai}, mengganti field ini
	 * memindahkan atribusi buku besar seluruh angsuran yang belum diposting dari dokumen pengajuan
	 * manapun yang memakai jenis pengajuan ini. Dipanggil dari
	 * {@code JenisPengajuanTransaksiPegawaiAction.onSave()} dengan nilai dari combobox yang wajib
	 * dipilih (validasi menolak simpan bila kosong).</p>
	 *
	 * @param jenisTransaksiPegawai katalog jenis transaksi pegawai.
	 */
	public void setJenisTransaksiPegawai(JenisTransaksiPegawai jenisTransaksiPegawai) {
		this.jenisTransaksiPegawai = jenisTransaksiPegawai;
	}

	/**
	 * Mengembalikan skema penomoran agenda ({@link NomorSurat}) yang dipakai untuk membangkitkan
	 * nomor dokumen pengajuan berjenis ini.
	 *
	 * <p>Dibaca {@code PengajuanTransaksiPegawaiAction.generateCode(...)} dan
	 * {@code .getindex(...)} untuk membangkitkan {@code PengajuanTransaksiPegawai.getKode()} (nomor
	 * agenda tercetak) setiap kali dokumen pengajuan baru berjenis ini disimpan tanpa kode: format
	 * ({@code getContohFormat()}, dipakai juga pada grid layar CRUD katalog ini), sumber indeks urut
	 * ({@code getGunakanIndexUrut()}), dan aturan reset (tahunan/bulanan/tanggal tertentu) seluruhnya
	 * diambil dari object ini. Kolom {@code nullable = true} &mdash; bila kosong,
	 * {@code generateCode(...)} mengembalikan string kosong (dibungkus try/catch), sehingga dokumen
	 * pengajuan berjenis ini tidak pernah mendapat nomor agenda otomatis.</p>
	 * <p>Relasi {@code @ManyToOne} LAZY dengan {@code cascade = {PERSIST, MERGE}}; penugasan kembali
	 * hanyalah resolusi proxy lazy standar {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return skema penomoran, atau {@code null} bila belum dipilih.
	 * @see PengajuanTransaksiPegawai#getKode()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan skema penomoran agenda untuk jenis pengajuan ini.
	 *
	 * <p>Dipanggil {@code JenisPengajuanTransaksiPegawaiAction.onSave()} dari nilai
	 * {@code AmbilDataNomorSuratBanbox}, yang menolak simpan bila belum dipilih (kotak isian
	 * "Nomor Agenda *" bersifat wajib di layar, meskipun kolom database sendiri
	 * {@code nullable = true}).</p>
	 *
	 * @param nomorSurat skema penomoran; boleh {@code null} pada level database, dengan konsekuensi
	 *                   dokumen pengajuan berjenis ini tidak mendapat nomor agenda otomatis.
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

}
