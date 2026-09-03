package ais.database.model.payroll;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.CommonVO;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Dokumen <b>pengajuan transaksi pegawai</b> — surat permohonan seorang pegawai atas sebuah
 * transaksi non-gaji (pinjaman, panjar, potongan sukarela, iuran, dan sejenisnya menurut katalog
 * {@link JenisPengajuanTransaksiPegawai}) yang, bila disetujui, akan dipotongkan dari gajinya
 * secara berangsur.
 *
 * <h3>Peran: induk dari jadwal angsuran</h3>
 * <p>Entity ini adalah <b>induk</b> dari {@link ais.database.model.payroll.TransaksiPegawai}.
 * Hubungannya satu-arah dan sangat kuat: satu dokumen pengajuan melahirkan sebanyak
 * {@link #getJumlahAngsur()} baris {@code TransaksiPegawai}, dan baris-baris itu <b>tidak menyimpan
 * angkanya sendiri</b>. Verifikasi langsung dari kode {@code TransaksiPegawai} memastikan tiga
 * kolom dokumen ini menjadi sumber hidup (live-read, bukan snapshot) bagi seluruh anaknya:</p>
 * <ul>
 *   <li>{@link #getNilaiTransaksi()} dan {@link #getJumlahAngsur()} → <code>TransaksiPegawai
 *       .getNilai()</code> menghitung <code>nilaiTransaksi / jumlahAngsur</code> pada
 *       <b>setiap pembacaan</b> lalu menuliskannya balik ke kolom nominal baris angsuran;</li>
 *   <li>{@link #getTanggalJatuhTempo()} → <code>TransaksiPegawai.getTanggal()</code> menghitung
 *       ulang jatuh tempo baris ke-<i>N</i> sebagai jatuh tempo dokumen digeser maju
 *       <code>(N - 1)</code> bulan, lalu menuliskannya balik — dan dari situ mengalir ke kolom
 *       bulan/tahun yang menentukan <b>bulan pemotongan gaji</b>;</li>
 *   <li>{@link #getPegawai()} dan {@code jenisPengajuanTransaksiPegawai.jenisTransaksiPegawai} →
 *       pegawai serta <b>akun jurnal</b> baris angsuran.</li>
 * </ul>
 * <p>Konsekuensinya penting dan tidak kasat mata: <b>menyunting nominal, jumlah angsuran, atau
 * tanggal jatuh tempo pada dokumen ini mengubah angsuran yang sudah terbit secara retroaktif</b>,
 * termasuk angsuran yang sudah diposting ke buku besar dan sudah dipotongkan pada slip gaji yang
 * terbayar. Jurnal lama tidak ikut berubah, sehingga tabel angsuran dan buku besar berpisah
 * diam-diam. Karena itu dokumen ini harus diperlakukan sebagai dokumen sumber yang <b>beku setelah
 * disetujui</b>, meskipun tidak ada satu pun penjaga di kode yang menegakkan hal itu.</p>
 *
 * <h3>Dua mekanisme persetujuan yang berjalan berdampingan</h3>
 * <p>Kelas ini turunan {@link DataSop}, jadi ia bisa dijalankan lewat mesin SOP (disposisi
 * berjenjang) melalui {@link #getDisposisiSop()}. Tetapi ada jalur kedua yang jauh lebih ringan:
 * kolom boolean {@link #getSetujui()} yang dikendalikan sebuah <b>checkbox "Setujui" langsung di
 * baris daftar</b> pada {@code PengajuanTransaksiPegawaiAction}. Keduanya saling meniadakan di
 * layar — renderer hanya memasang checkbox itu bila {@code getDisposisiSop() == null} — sehingga
 * dokumen yang tidak pernah dimasukkan ke alur SOP resmi disetujui hanya dengan satu klik. Ini
 * kerabat langsung dari pola bypass SOP yang sudah dilacak (<code>task_c9d4d09f</code>): dokumen
 * yang lahir di luar layar SOP tidak pernah memperoleh {@code disposisiSop}, dan justru karena itu
 * memperoleh jalur persetujuan satu-klik.</p>
 * <p><b>Gerbang persetujuannya berbasis data, bukan peran.</b> Verifikasi di
 * {@code PengajuanTransaksiPegawaiAction} menunjukkan checkbox itu dirender tanpa satu pun
 * pemanggilan {@code CommonPrivilages}; syaratnya murni relasi kepegawaian — penyetuju harus
 * pegawai lain (tidak boleh dirinya sendiri, ini penjaga yang <i>benar</i> dan patut dicatat) dan
 * harus salah satu dari: atasan langsung 1/2/3 pemohon, atau termasuk dalam daftar
 * {@code punyaBawahan} yang dirakit dari jabatan/pejabat. Tidak ada batas nominal, tidak ada
 * pemeriksaan tenant, dan daftar {@code punyaBawahan} itu sendiri dirakit dari query
 * {@code Pegawai} <b>tanpa penyaring satuan kerja</b>, sehingga seorang pejabat bisa menyetujui
 * pengajuan pegawai tenant lain bila hierarki jabatannya kebetulan menyentuh mereka.</p>
 *
 * <h3>Mekanisme perusak jadwal angsuran (<code>task_f2f37db5</code>) — dari sisi dokumen ini</h3>
 * <p>Rangkaiannya dapat ditelusuri utuh dari sisi pengajuan, dan pemicunya jauh lebih ringan
 * daripada yang tersirat dari nama methodnya:</p>
 * <ol>
 *   <li><code>PengajuanTransaksiPegawaiAction.populateTransaksi(Session, PengajuanTransaksiPegawai)</code>
 *       membuka diri dengan SQL mentah <code>delete from payroll.transaksi_pegawai where
 *       pengajuan_transaksi_pegawai = &lt;id&gt;</code> — <b>tanpa syarat apa pun</b>: tidak
 *       memeriksa apakah baris sudah punya {@code postingHistory} (sudah dijurnal), tidak memeriksa
 *       apakah sudah dipotongkan ke slip gaji, tidak memeriksa {@link #getAktif()}. Karena SQL
 *       mentah, penghapusan ini juga <b>luput dari Envers</b> ({@code @Audited} tidak berlaku untuk
 *       {@code createSQLQuery}), jadi tidak ada jejak audit apa pun.</li>
 *   <li>Sesudahnya, <b>hanya bila</b> {@link #getSetujui()} bernilai benar, baris angsuran
 *       dibangkitkan ulang dari nol sebanyak {@link #getJumlahAngsur()} dengan id baru dan status
 *       "belum diposting".</li>
 *   <li>Pemicunya <b>dua</b>, keduanya ringan. Pertama, <code>onSave()</code> memanggilnya pada
 *       <b>setiap penyimpanan biasa</b> layar pengajuan — mengoreksi salah ketik pada kolom
 *       keterangan pun cukup. Kedua — dan inilah yang paling berbahaya — listener
 *       <code>onCheck</code> pada checkbox "Setujui" memanggilnya <b>tanpa memandang arah
 *       perubahan</b>: baik saat dicentang maupun saat <b>dicabut centangnya</b>.</li>
 *   <li>Pada pencabutan centang, listener lebih dulu menjalankan {@link #setSetujui(Boolean)} ke
 *       {@code false}, {@link #setDisetujuiOleh(Tbmuser)} ke {@code null}, dan
 *       {@link #setSetujuiTanggal(Date)} ke {@code null}, lalu menyimpan. Ketika
 *       <code>populateTransaksi()</code> berjalan sesudahnya, cabang pembangkitan ulang
 *       <b>tidak dimasuki</b> — sehingga hasil bersihnya adalah <b>penghapusan total seluruh
 *       jadwal angsuran</b>, termasuk angsuran yang sudah diposting ke buku besar dan yang sudah
 *       dipotongkan dari gaji terbayar. Jurnal lama tetap tertinggal di
 *       <code>akunting.grup_transaksi</code> sambil menunjuk id angsuran yang sudah tidak ada.</li>
 *   <li>Satu klik itu tidak dapat dibatalkan dengan mencentang ulang: pencentangan kembali
 *       membangkitkan baris <b>baru</b> berstatus belum-diposting, sehingga potongan yang sama bisa
 *       dijurnal untuk kedua kalinya, sementara referensi jurnal lama tetap yatim.</li>
 *   <li>Asimetri tambahan: hanya cabang "disetujui" yang memanggil
 *       {@link DaftarPengajuanTransfer#simpanPengajuanTransaksiPegawai(PengajuanTransaksiPegawai)},
 *       dan helper itu berhenti lebih awal bila {@link #getDaftarPengajuanTransfer()} sudah terisi.
 *       Karena penghapusan pada langkah (1) tidak pernah melepas kolom
 *       {@code daftar_pengajuan_transfer} dokumen, pencentangan ulang <b>tidak</b> mendaftarkan
 *       kembali antrean transfernya — angsuran hidup lagi, antrean pembayarannya tidak.</li>
 * </ol>
 *
 * <h3>Kelompok anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; penomoran</b> — {@link #getId()}, {@link #getKode()},
 *       {@link #getIndex()}, {@link #getNama()}, {@link #getKeterangan()}.</li>
 *   <li><b>Waktu</b> — {@link #getWaktu()} (waktu pengajuan), {@link #getTahun()} dan
 *       {@link #getBulan()} (periode penomoran), {@link #getTanggalJatuhTempo()} (jangkar jadwal
 *       angsuran), {@link #getTanggal_dirubah()} (stempel audit).</li>
 *   <li><b>Nominal</b> — {@link #getNilaiTransaksi()}, {@link #getJumlahAngsur()}.</li>
 *   <li><b>Subjek &amp; klasifikasi</b> — {@link #getPegawai()},
 *       {@link #getJenisPengajuanTransaksiPegawai()}, {@link #getSatuanKerja()}.</li>
 *   <li><b>Persetujuan</b> — {@link #getSetujui()}, {@link #getDisetujuiOleh()},
 *       {@link #getSetujuiTanggal()}, {@link #getDiajukanOleh()}, {@link #getDisposisiSop()},
 *       {@link #getAktif()}.</li>
 *   <li><b>Isian dinamis</b> — {@link #getParameterTambahan()},
 *       {@link #getParameterTambahanInds()}, {@link #populateParameterTambahan(List)},
 *       {@link #ambilDataParameterTambahan()}.</li>
 *   <li><b>Hilir keuangan</b> — {@link #getDaftarPengajuanTransfer()}.</li>
 *   <li><b>Jejak audit ringan</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h3>Hal-hal yang tidak kasat mata</h3>
 * <p><b>Deklarasi ulang field bukan bug.</b> {@link ais.database.model.GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, dan Hibernate tidak
 * memetakan propertinya. Karena itu setiap entity turunannya <b>harus</b> mendeklarasikan ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}-nya sendiri. Ini keharusan
 * teknis, bukan duplikasi yang perlu dirapikan.</p>
 * <p><b>Getter yang menulis.</b> Kelas dipetakan property-access dengan {@code dynamicUpdate},
 * sehingga getter yang menugaskan nilai ke fieldnya sendiri ikut tersimpan pada flush berikutnya
 * tanpa ada yang memanggil setter. Lihat peringatan pada {@link #getNama()},
 * {@link #getSatuanKerja()}, {@link #getTahun()}, {@link #getBulan()}, {@link #getSetujui()}, dan
 * {@link #getAktif()}.</p>
 * <p><b>Isian dinamis (sudah diperbaiki).</b> {@link #populateParameterTambahan(List)} sempat membaca
 * atribut baris ZK dengan kunci dan tipe milik modul {@code PengajuanPegawai} yang tidak pernah
 * dipasang oleh listener modul ini, sehingga setiap penyimpanan mengosongkan seluruh jawaban
 * parameter tambahan dokumen ini secara diam-diam. Sudah diperbaiki menjadi kunci/tipe
 * {@code KelompokParameterTambahanPengajuanTransaksiPegawai} milik modul ini, dan method sekarang
 * fail-closed (mempertahankan nilai lama) bila tidak satu pun baris berhasil diproses. Dokumen yang
 * disimpan sebelum perbaikan ini kemungkinan sudah telanjur kosong; lihat catatan pemulihan pada
 * {@link #populateParameterTambahan(List)}.</p>
 * <p><b>Cakupan tenant.</b> Layar ZK menyaring {@code satuanKerja} dengan pola fail-open yang sudah
 * dikenal (daftar satuan kerja kosong menjadi <code>1=1</code>, dan dokumen ber-{@code satuanKerja}
 * null selalu ikut tampil); cabang "punya bawahan" bahkan melewati penyaring satuan kerja sama
 * sekali. Di sisi CRUD generik v2, {@code satuanKerja} <b>ada</b> pada whitelist
 * {@code GenericCrudAutoEntityAdapter.scopeBindings()} sehingga pembatasnya terpasang — namun
 * {@code pegawai} tidak (pola <code>task_7b6038ac</code>), sehingga di dalam satu satuan kerja
 * setiap pegawai dapat membaca nominal serta alasan pengajuan rekan kerjanya. Bila
 * {@code Tbmuser.getSatuanKerja()} bernilai null, {@code addScope()} berhenti tanpa memasang
 * pembatas apa pun dan dokumen seluruh tenant terbuka.</p>
 * <p><b>Penomoran.</b> Nomor agenda dokumen ini dibangkitkan
 * {@code PengajuanTransaksiPegawaiAction.getindex()} dengan <code>Projections.rowCount() + 1</code>
 * (menghitung baris, bukan mengambil nomor terbit tertinggi) tanpa penyaring tenant — instans
 * berikutnya dari pola nomor kembar yang dilacak <code>task_59118ff1</code>. Lihat
 * {@link #getIndex()}.</p>
 *
 * @see ais.database.model.payroll.TransaksiPegawai
 * @see JenisPengajuanTransaksiPegawai
 * @see DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "pengajuan_transaksi_pegawai")
public class PengajuanTransaksiPegawai extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; identity, dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyentuh baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyimpan baris ini (jejak audit ringan di luar Envers).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir.
	 *
	 * <p><b>Setter menolak pengosongan:</b> {@code null} maupun string kosong/spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan. Jejak audit tidak pernah bisa dihapus lewat
	 * setter ini, tetapi juga tidak bisa dikosongkan dengan sengaja bila memang diperlukan.</p>
	 *
	 * @param olehId id pengguna; kosong/{@code null} diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p><b>Setter menolak pengosongan</b> dengan aturan yang sama seperti
	 * {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengguna; kosong/{@code null} diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} yang memperbarui stempel waktu perubahan lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali Hibernate menerbitkan {@code UPDATE}
	 * untuk baris ini.
	 *
	 * <p>Tidak dipanggil manual; hanya provider JPA yang memanggilnya. Perhatikan bahwa kait ini
	 * <b>tidak</b> menyala untuk perubahan yang dilakukan lewat SQL mentah — termasuk penghapusan
	 * angsuran pada <code>populateTransaksi()</code> yang dijelaskan di dokumentasi kelas.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris yang sama (peninggalan
	 * penyuntingan otomatis); nilai awalnya waktu pembuatan objek.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()}; nilai awalnya waktu instansiasi objek, bukan
	 * {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas untuk log dan komponen pilihan.
	 *
	 * <p>Membaca field {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 * substitusi nama dari jenis pengajuan tidak berlaku di sini: dokumen yang namanya belum pernah
	 * diisi tampil sebagai <code>"12-null"</code>.</p>
	 *
	 * @return gabungan <code>id + "-" + nama</code>.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Alasan/judul pengajuan. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Tanggal dan jam pengajuan. Lihat {@link #getWaktu()}. */
	private Date waktu;

	/** Jatuh tempo angsuran pertama — jangkar seluruh jadwal. Lihat {@link #getTanggalJatuhTempo()}. */
	private Date tanggalJatuhTempo;
	/** Nominal total yang diajukan. Lihat {@link #getNilaiTransaksi()}. */
	private Double nilaiTransaksi;
	/** Banyaknya angsuran. Lihat {@link #getJumlahAngsur()}. */
	private Integer jumlahAngsur;

	/** Katalog jenis pengajuan; pembawa akun jurnal. Lihat {@link #getJenisPengajuanTransaksiPegawai()}. */
	private JenisPengajuanTransaksiPegawai jenisPengajuanPegawai;
	/** Pegawai pemohon. Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Satuan kerja (sumbu tenant). Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Isian dinamis versi terbaca-manusia. Lihat {@link #getParameterTambahan()}. */
	private String parameterTambahan;
	/** Isian dinamis versi berbasis id. Lihat {@link #getParameterTambahanInds()}. */
	private String parameterTambahanInds;
	/** Disposisi SOP bila dokumen dijalankan lewat alur SOP. Lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;
	/** Tahun periode penomoran. Lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Bulan periode penomoran. Lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Nomor agenda tercetak. Lihat {@link #getKode()}. */
	private String kode;
	/** Urutan numerik di balik {@link #getKode()}. Lihat {@link #getIndex()}. */
	private Long index;
	/** Waktu persetujuan. Lihat {@link #getSetujuiTanggal()}. */
	private Date setujuiTanggal;
	/** Pengguna pengaju. Lihat {@link #getDiajukanOleh()}. */
	private Tbmuser diajukanOleh;
	/** Pengguna penyetuju (nama field kurang satu huruf, kolomnya pun demikian). Lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujiOleh;
	/** Bendera persetujuan — gerbang pembangkitan angsuran. Lihat {@link #getSetujui()}. */
	private Boolean setujui;
	/** Bendera aktif/berlaku. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Baris antrean pembayaran hilir. Lihat {@link #getDaftarPengajuanTransfer()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;

	/**
	 * Constructor kosong yang diwajibkan Hibernate dan dipakai layar untuk membuat dokumen baru.
	 *
	 * <p>Tidak menyetel apa pun; nilai bawaan datang dari getter-getter bersubstitusi
	 * ({@link #getWaktu()}, {@link #getTahun()}, {@link #getBulan()},
	 * {@link #getTanggalJatuhTempo()}, {@link #getJumlahAngsur()}, {@link #getAktif()}).</p>
	 */
	public PengajuanTransaksiPegawai() {
	}

	/**
	 * Kunci utama dokumen.
	 *
	 * <p>Dipakai sebagai kunci pencarian {@link LampiranLain} untuk berkas lampiran isian dinamis,
	 * dan diselipkan langsung ke SQL penghapusan angsuran pada
	 * <code>PengajuanTransaksiPegawaiAction.populateTransaksi()</code>.</p>
	 *
	 * @return id, atau {@code null} untuk dokumen yang belum disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Hanya Hibernate yang seharusnya memanggilnya; kolomnya {@code insertable = false}.</p>
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nomor agenda dokumen sebagaimana tercetak.
	 *
	 * <p>Getter bersih (tidak menulis balik) yang menormalkan hasilnya: string kosong atau berisi
	 * spasi saja dilaporkan sebagai {@code null}, sisanya di-{@code trim()}. Karena itu pemeriksaan
	 * "kode belum ada" di layar cukup memeriksa {@code null}.</p>
	 * <p>Nilainya dibangkitkan {@code PengajuanTransaksiPegawaiAction.generateCode()} dari template
	 * {@link ais.database.model.surat.NomorSurat} milik {@link #getJenisPengajuanTransaksiPegawai()};
	 * bila jenis pengajuan tidak punya template nomor surat, hasilnya string kosong — yang lewat
	 * getter ini terbaca {@code null}, sehingga renderer daftar akan mencoba membangkitkan kode
	 * lagi pada <b>setiap render baris</b>.</p>
	 *
	 * @return nomor agenda, atau {@code null} bila kosong.
	 */
	@Column(name = "kode")
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/**
	 * Menyetel nomor agenda.
	 *
	 * <p>Disimpan apa adanya tanpa {@code trim()}; normalisasi terjadi saat dibaca.</p>
	 *
	 * @param kode nomor agenda.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Alasan/judul pengajuan — teks yang dipakai layar, laporan, dan keterangan jurnal angsuran.
	 *
	 * <p><b>Getter destruktif.</b> Bila {@code nama} masih {@code null} atau kosong sedangkan
	 * {@link #getJenisPengajuanTransaksiPegawai()} sudah terisi, field ditimpa dengan nama katalog
	 * jenis pengajuan dan — karena pemetaan property-access dengan {@code dynamicUpdate} —
	 * nilai itu ikut tersimpan pada flush berikutnya. Dokumen yang alasannya sengaja dikosongkan
	 * karena itu perlahan berubah menjadi berjudul nama jenisnya.</p>
	 * <p>Efek samping tambahan: pemanggilan {@link #getJenisPengajuanTransaksiPegawai()} ikut
	 * me-resolve relasi katalognya (lihat catatan {@code check()} di sana).</p>
	 * <p>Teks ini mengalir jauh: <code>TransaksiPegawai.getKeterangan()</code> menyusun keterangan
	 * baris angsuran (dan karenanya keterangan jurnal) dari
	 * <code>getNama() + " " + getKeterangan()</code>.</p>
	 *
	 * @return alasan pengajuan sudah di-{@code trim()}, atau {@code null} bila memang kosong dan
	 *         jenis pengajuannya pun belum dipilih.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if ((nama == null || nama.isEmpty()) && getJenisPengajuanTransaksiPegawai() != null) {
			nama = getJenisPengajuanTransaksiPegawai().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel alasan pengajuan.
	 *
	 * <p>Diisi dari kotak "Alasan Pengajuan" pada layar. Menyetelnya ke kosong tidak bertahan —
	 * lihat {@link #getNama()}.</p>
	 *
	 * @param nama alasan pengajuan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel urutan numerik penomoran.
	 *
	 * @param index nomor urut.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Urutan numerik di balik {@link #getKode()} — nomor ke berapa dokumen ini dalam periode
	 * penomorannya.
	 *
	 * <p>Getter murni (tidak ada substitusi maupun tulis-balik); {@code null} berarti dokumen belum
	 * pernah dinomori.</p>
	 * <p><b>Kuirk penomoran.</b> Nilainya diisi layar sebagai
	 * <code>getindex(jenis) + 1</code>, dan {@code getindex()} memakai
	 * <code>Projections.rowCount()</code> — yakni <b>menghitung banyaknya baris</b> yang cocok,
	 * bukan mengambil nomor terbit tertinggi. Begitu satu dokumen dihapus, nomor berikutnya
	 * mengulang nomor yang sudah terbit. Penghitungnya juga menyaring kolom {@code tahun}/
	 * {@code bulan} tersimpan, padahal kolom itu baru terisi ketika {@link #getTahun()}/
	 * {@link #getBulan()} pernah dibaca — baris lama yang kolomnya masih {@code null} tidak ikut
	 * terhitung, dan tidak ada penyaring tenant sama sekali sehingga pencacahnya global. Ini
	 * instans lanjutan dari pola nomor surat kembar yang dilacak <code>task_59118ff1</code>.</p>
	 *
	 * @return nomor urut, atau {@code null} bila belum dinomori.
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Keterangan bebas dokumen.
	 *
	 * <p>Getter bersubstitusi (bukan destruktif — field tidak ditulis balik): {@code null}
	 * dilaporkan sebagai string kosong, sehingga pemanggil tidak perlu memeriksa {@code null}.
	 * Sisi lainnya, "belum diisi" dan "diisi kosong" tidak dapat dibedakan lewat getter ini.</p>
	 * <p>Ikut menyusun keterangan jurnal angsuran lewat
	 * <code>TransaksiPegawai.getKeterangan()</code>. Kolom inilah yang dicari kotak pencarian
	 * "nama" pada layar daftar — bukan {@link #getNama()}.</p>
	 *
	 * @return keterangan; tidak pernah {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Tanggal dan jam pengajuan diajukan.
	 *
	 * <p>Getter bersubstitusi (field tidak ditulis balik): {@code null} dilaporkan sebagai waktu
	 * sekarang. Karena substitusinya tidak persisten, dokumen lama yang kolomnya {@code null} akan
	 * tampak "diajukan hari ini" setiap kali dibuka — dan karena nilai ini dipakai
	 * {@code NomorSurat.format()} saat membangkitkan nomor agenda, nomor yang terbit ikut memakai
	 * tanggal hari ini.</p>
	 * <p>Layar mengisinya sendiri untuk dokumen baru dengan jam dipatok 08:00:00.</p>
	 *
	 * @return waktu pengajuan; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyetel waktu pengajuan.
	 *
	 * @param waktu waktu pengajuan.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Katalog jenis pengajuan — penentu akun jurnal, template nomor surat, dan daftar isian dinamis
	 * yang harus diisi.
	 *
	 * <p><b>Getter destruktif ringan:</b> hasil {@code check()} ditugaskan kembali ke field. Dengan
	 * pemetaan property-access, referensi yang di-resolve {@code check()} (kanonikalisasi lewat
	 * peta identitas entity, atau pemuatan ulang bila proxy sudah tidak dapat diinisialisasi)
	 * menjadi nilai yang tersimpan. Ini juga berarti pembacaan getter ini dapat menerbitkan query
	 * meskipun relasinya {@code LAZY}.</p>
	 * <p><b>Jalur ke akun jurnal.</b> Rantainya <code>pengajuan → jenisPengajuanTransaksiPegawai →
	 * jenisTransaksiPegawai</code>, dan dibaca <b>hidup</b> oleh
	 * <code>TransaksiPegawai.getJenisTransaksiPegawai()</code>. Mengubah
	 * {@code jenisTransaksiPegawai} pada baris katalog karena itu <b>memindahkan akun buku besar
	 * angsuran yang sudah terbit</b>, termasuk yang sudah diposting.</p>
	 * <p>Nama field ({@code jenisPengajuanPegawai}) sengaja berbeda dari nama properti
	 * ({@code jenisPengajuanTransaksiPegawai}); yang mengikat pemetaan adalah nama getter/setter,
	 * dan kolomnya {@code jenis_pengajuan_transaksi_pegawai}.</p>
	 *
	 * @return jenis pengajuan, atau {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengajuan_transaksi_pegawai")
	public JenisPengajuanTransaksiPegawai getJenisPengajuanTransaksiPegawai() {
		jenisPengajuanPegawai = check(jenisPengajuanPegawai);
		return jenisPengajuanPegawai;
	}

	/**
	 * Menyetel jenis pengajuan.
	 *
	 * <p>Layar mengisinya dari parameter URL {@code jenis} (bila layar dibuka terikat pada satu
	 * jenis) atau dari combobox. Mengubahnya pada dokumen yang angsurannya sudah terbit memindahkan
	 * akun jurnal seluruh angsuran itu — lihat {@link #getJenisPengajuanTransaksiPegawai()}.</p>
	 *
	 * @param jenisPengajuanPegawai jenis pengajuan.
	 */
	public void setJenisPengajuanTransaksiPegawai(JenisPengajuanTransaksiPegawai jenisPengajuanPegawai) {
		this.jenisPengajuanPegawai = jenisPengajuanPegawai;
	}

	/**
	 * Jawaban isian dinamis dalam bentuk <b>berbasis id</b> — versi yang benar-benar dibaca kembali
	 * oleh layar dan laporan.
	 *
	 * <p>Formatnya satu baris per jawaban, dipisah newline, dengan medan dipisah
	 * <code>&lt;=&gt;</code>:</p>
	 * <pre>&lt;idKelompok&gt;-&gt;&lt;idParameter&gt; &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan</pre>
	 * <p>Medan pertama sengaja dibuat identik dengan kunci {@code jenis} yang dipakai
	 * {@link LampiranLain#resolveJenisParameterTambahan(Class, Long, String)}, sehingga pembaca
	 * dapat mencocokkan jawaban dengan berkas lampirannya.</p>
	 * <p>Getter bersubstitusi <b>destruktif ringan</b>: {@code null} diganti string kosong dan
	 * nilai itu ditugaskan ke field (ikut tersimpan pada flush berikutnya). Substitusi ini
	 * disengaja agar pemanggil dapat langsung memanggil {@code split("\n")}.</p>
	 * <p><b>Peringatan:</b> kolom ini dikosongkan pada setiap penyimpanan — lihat
	 * {@link #populateParameterTambahan(List)}.</p>
	 *
	 * @return teks jawaban berbasis id; tidak pernah {@code null}, sering kosong.
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menyetel teks jawaban isian dinamis berbasis id.
	 *
	 * <p>Satu-satunya pemanggil di luar Hibernate adalah
	 * {@link #populateParameterTambahan(List)}.</p>
	 *
	 * @param parameterTambahanInds teks jawaban berbasis id.
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mem-parse {@link #getParameterTambahan()} (versi terbaca-manusia) menjadi daftar
	 * {@link CommonVO} terurut, untuk dikonsumsi komponen tampilan/laporan generik.
	 *
	 * <p>Pemetaan medan per baris — perhatikan bahwa <b>indeks yang dibaca di sini tidak sama</b>
	 * dengan yang ditulis {@link #populateParameterTambahan(List)}, karena format tulisnya lebih
	 * panjang:</p>
	 * <ul>
	 *   <li>medan 0 → {@code name} (label lengkap <code>namaKelompok-&gt;labelInputan</code>), dan
	 *       potongan sebelum <code>-&gt;</code> juga disalin ke {@code name5} sebagai nama
	 *       kelompok;</li>
	 *   <li>medan 1 → {@code name1} (nilai jawaban);</li>
	 *   <li>medan 2 → {@code name2} (URL lampiran);</li>
	 *   <li>medan 3 → {@code nomorUrut} (dasar pengurutan);</li>
	 *   <li>medan 4 → {@code id} (id {@link ParameterTambahan});</li>
	 *   <li>medan 5 (id kelompok) dan medan 6 (keterangan) <b>tidak dibaca sama sekali</b>.</li>
	 * </ul>
	 * <p><b>Kasus tepi:</b></p>
	 * <ul>
	 *   <li>bila teks sumbernya kosong — keadaan normal untuk entity ini, lihat
	 *       {@link #populateParameterTambahan(List)} — <code>"".split("\n")</code> tetap
	 *       menghasilkan satu elemen kosong, sehingga method ini mengembalikan
	 *       <b>satu baris hantu</b> berlabel kosong, bukan daftar kosong;</li>
	 *   <li>medan 3 dan 4 yang tidak berupa angka ditelan {@code try/catch} dan jatuh ke nilai
	 *       bawaan {@code 1}/{@code 1L} — jadi baris rusak tetap masuk daftar dengan id yang
	 *       menyesatkan (bertumbukan dengan parameter ber-id 1);</li>
	 *   <li>medan yang tidak ada diganti string kosong, tidak melempar
	 *       {@code ArrayIndexOutOfBounds} — kecuali {@code param[0]}, yang aman karena
	 *       {@code split} selalu mengembalikan minimal satu elemen.</li>
	 * </ul>
	 * <p><b>Status pemakaian:</b> penyisiran seluruh repo tidak menemukan satu pun pemanggil method
	 * ini <i>untuk kelas ini</i>. Seluruh konsumen isian dinamis dokumen pengajuan transaksi
	 * pegawai (renderer daftar, listener parameter, dan
	 * {@code LaporanPengajuanTransaksiPegawai}) mem-parsing sendiri
	 * {@link #getParameterTambahanInds()}. Method ini adalah salinan dari keluarga
	 * {@code PengajuanPegawai}/{@code KegiatanSiswa} yang ikut terbawa tetapi tidak pernah
	 * tersambung di sini.</p>
	 *
	 * @return daftar {@link CommonVO} terurut menurut {@code nomorUrut}; tidak pernah {@code null},
	 *         tetapi minimal berisi satu baris hantu bila sumbernya kosong.
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/PengajuanTransaksiPegawai.java:219");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/PengajuanTransaksiPegawai.java:225");

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
	 * Memanen nilai seluruh baris isian dinamis yang sedang tampil di layar dan menuliskannya ke
	 * {@link #setParameterTambahan(String)} serta {@link #setParameterTambahanInds(String)}.
	 *
	 * <p>Dipanggil {@code ParameterTambahanPengajuanTransaksiPegawaiListener} pada dua momen:
	 * ketika pengguna mengubah salah satu isian (listener {@code isi}), dan sekali lagi dari
	 * {@code onSave()} tepat sebelum dokumen disimpan.</p>
	 *
	 * <p><b>✅ CACAT SALIN-TEMPEL YANG MENGHAPUS DATA — SUDAH DIPERBAIKI.</b>
	 * Method ini sempat membaca atribut baris ZK dengan kunci
	 * <code>"kelompokParameterTambahanPengajuanPegawai"</code> dan menampungnya dalam variabel
	 * bertipe {@code KelompokParameterTambahanPengajuanPegawai} — keduanya milik keluarga
	 * <b>{@code PengajuanPegawai}</b> (paket {@code ais.database.model}), bukan keluarga payroll
	 * ini, sedangkan listener modul ini memasang atributnya dengan kunci
	 * <code>"kelompokParameterTambahanPengajuanTransaksiPegawai"</code> dan objek bertipe
	 * {@link KelompokParameterTambahanPengajuanTransaksiPegawai}. Kedua kunci/tipe itu sekarang
	 * sudah disamakan dengan yang dipasang listener. Akibat sebelum perbaikan:</p>
	 * <ul>
	 *   <li>{@code row.getAttribute(...)} selalu mengembalikan {@code null};</li>
	 *   <li>penjaga <code>parameterTambahan != null &amp;&amp; kelompok... != null</code>
	 *       karenanya <b>tidak pernah benar</b> untuk satu baris pun;</li>
	 *   <li>kedua penampung teks tetap kosong sampai akhir perulangan, lalu ditulis ke entity —
	 *       sehingga <b>setiap penyimpanan mengosongkan seluruh jawaban isian dinamis</b> dokumen
	 *       ini (alasan rinci, nominal pendukung, tautan lampiran), diam-diam dan tanpa pesan
	 *       kesalahan apa pun;</li>
	 *   <li>ironisnya {@code validate()} pada listener yang sama memakai kunci dan tipe yang
	 *       <b>benar</b>, sehingga isian wajib tetap divalidasi dengan ketat sesaat sebelum
	 *       nilainya dibuang;</li>
	 *   <li>berkas lampirannya sendiri selamat (disimpan terpisah sebagai {@link LampiranLain} oleh
	 *       {@code onSave()} layar), sehingga hasil akhirnya adalah dokumen dengan lampiran yang
	 *       ada tetapi tanpa satu pun jawaban tercatat.</li>
	 * </ul>
	 * <p><b>Setelah perbaikan</b> method ini juga fail-closed: bila {@code parameterRows} tidak
	 * kosong tetapi tidak satu pun barisnya berhasil diproses (mis. karena listener gagal memasang
	 * atribut, atau setiap baris melempar exception), nilai {@link #getParameterTambahan()} dan
	 * {@link #getParameterTambahanInds()} yang lama <b>dipertahankan</b>, bukan ditimpa string
	 * kosong. Exception per baris kini juga dicatat ke {@code ais.common.ErrorAuditUtil} selain
	 * ditampilkan lewat {@code Common.tampilErrorJikaAdmin(e)}.</p>
	 * <p><b>Pemulihan data historis.</b> Dokumen yang disimpan sebelum perbaikan ini kemungkinan sudah
	 * telanjur memiliki {@code parameter_tambahan}/{@code parameter_tambahan_inds} kosong di tabel
	 * {@code payroll.pengajuan_transaksi_pegawai}. Entity ini {@code @Audited} (Envers), sehingga
	 * revisi lama pada tabel audit (skema {@code payroll_aud}, tabel
	 * {@code pengajuan_transaksi_pegawai_aud}) mungkin masih menyimpan teks sebelum dikosongkan —
	 * pemulihan itu belum dijalankan dan memerlukan akses/kredensial basis data untuk memverifikasi
	 * cakupan kerusakan dan menyusun skrip pemulihan dari revisi audit terakhir yang kolomnya masih
	 * terisi.</p>
	 * <p>Pembanding yang menegaskan ini salinan: {@code PengajuanPegawai.populateParameterTambahan()}
	 * memakai kunci yang sama persis, dan di sana listener pasangannya
	 * ({@code ParameterTambahanPengajuanPegawaiListener}) memang memasang kunci itu — jadi di
	 * modul asalnya method ini bekerja. Kandidat lain yang menyalin pola ini dan belum diperiksa
	 * untuk cacat serupa: {@code ais.database.model.employ.CutiDanIzin}, {@code CatatanAdministrasi},
	 * {@code CatatanMahasiswa}, {@code CatatanPegawai}, {@code IsiAngketParameterUmum},
	 * {@code KegiatanSiswa}.</p>
	 *
	 * <p><b>Format yang ditulis</b> adalah dua teks berbeda dari data yang sama:</p>
	 * <ul>
	 *   <li>versi terbaca-manusia ke {@link #setParameterTambahan(String)}:
	 *       <code>namaKelompok-&gt;label &lt;=&gt; nilai &lt;=&gt; url &lt;=&gt; nomorUrut
	 *       &lt;=&gt; idParameter &lt;=&gt; idKelompok &lt;=&gt; keterangan</code>;</li>
	 *   <li>versi berbasis id ke {@link #setParameterTambahanInds(String)}:
	 *       <code>idKelompok-&gt;idParameter &lt;=&gt; nilai &lt;=&gt; url &lt;=&gt;
	 *       keterangan</code>.</li>
	 * </ul>
	 * <p>URL lampiran hanya diisi untuk parameter yang {@code getHarusMenyertakanLampiran()}, dan
	 * diambil dari {@link LampiranLain#ambil(Long, String)} memakai kunci {@code jenis} hasil
	 * {@link LampiranLain#resolveJenisParameterTambahan(Class, Long, String)}. Untuk dokumen
	 * <b>baru</b> {@link #getId()} masih {@code null}, sehingga kunci lampirannya berbeda dari
	 * kunci yang dipakai setelah dokumen tersimpan.</p>
	 *
	 * <p><b>Kasus tepi lain:</b> pemanggilan dengan daftar {@code null} atau kosong
	 * <b>keluar lebih awal</b> tanpa menyentuh entity, dan sejak perbaikan ini nilai lama juga
	 * dipertahankan bila {@code parameterRows} tidak kosong tetapi tidak satu pun barisnya berhasil
	 * diproses (fail-closed). Kegagalan per baris masih ditampilkan lewat
	 * {@code Common.tampilErrorJikaAdmin(e)} (hanya terlihat pengguna admin) tetapi kini juga
	 * dicatat ke {@code ais.common.ErrorAuditUtil} sehingga tetap terlacak untuk pengguna biasa.</p>
	 *
	 * @param parameterRows baris-baris ZK hasil rakitan listener isian dinamis; {@code null} atau
	 *                      kosong menjadikan method ini no-op.
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		int baris = 0;
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanPengajuanTransaksiPegawai kelompokParameterTambahanPengajuanTransaksiPegawai = (KelompokParameterTambahanPengajuanTransaksiPegawai) row
						.getAttribute("kelompokParameterTambahanPengajuanTransaksiPegawai");
				if (parameterTambahan != null && kelompokParameterTambahanPengajuanTransaksiPegawai != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(PengajuanTransaksiPegawai.class, getId(),
							kelompokParameterTambahanPengajuanTransaksiPegawai.getId() + "->" + parameterTambahan.getId());

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
								ais.common.ErrorAuditUtil.record(e,
										"PengajuanTransaksiPegawai.populateParameterTambahan: gagal membuat link lampiran");
							}
						}

					}

					String s = kelompokParameterTambahanPengajuanTransaksiPegawai.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanPengajuanTransaksiPegawai.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPengajuanTransaksiPegawai.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
					baris++;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				ais.common.ErrorAuditUtil.record(e,
						"PengajuanTransaksiPegawai.populateParameterTambahan: gagal memproses satu baris isian dinamis");
			}
		}
		if (baris == 0) {
			// Tidak satu pun baris berhasil diproses padahal parameterRows tidak kosong:
			// pertahankan nilai lama (fail-closed) alih-alih menimpanya dengan string kosong.
			return;
		}
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Jawaban isian dinamis dalam bentuk <b>terbaca-manusia</b> (memuat nama kelompok dan label
	 * inputan, bukan hanya id).
	 *
	 * <p>Getter bersubstitusi destruktif ringan: {@code null} diganti string kosong dan ditugaskan
	 * kembali ke field.</p>
	 * <p>Format per barisnya diuraikan pada {@link #populateParameterTambahan(List)}; pembacaannya
	 * kembali menjadi objek dilakukan {@link #ambilDataParameterTambahan()}. Di dalam repo, versi
	 * inilah yang <b>tidak</b> dipakai layar mana pun untuk memuat ulang nilai — semua pembaca
	 * memilih {@link #getParameterTambahanInds()}; versi ini hanya berguna untuk laporan generik dan
	 * ekspor.</p>
	 * <p><b>Peringatan:</b> kolom ini dikosongkan pada setiap penyimpanan — lihat
	 * {@link #populateParameterTambahan(List)}.</p>
	 *
	 * @return teks jawaban terbaca-manusia; tidak pernah {@code null}, sering kosong.
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menyetel teks jawaban isian dinamis terbaca-manusia.
	 *
	 * @param parameterTambahan teks jawaban.
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Satuan kerja dokumen — sumbu tenant yang dipakai penyaringan daftar maupun CRUD generik v2.
	 *
	 * <p><b>Getter destruktif dengan penurunan nilai.</b> Bila {@link #getPegawai()} terisi dan
	 * pegawai itu punya satuan kerja, field {@code satuanKerja} <b>ditimpa</b> dengan satuan kerja
	 * pegawai; hanya bila tidak, nilai tersimpan dipertahankan (setelah lewat {@code check()}).
	 * Karena pemetaan property-access dengan {@code dynamicUpdate}, penimpaan itu ikut tersimpan
	 * pada flush berikutnya. Praktisnya: kolom satuan kerja dokumen ini bukan data yang berdiri
	 * sendiri, melainkan bayangan satuan kerja pegawainya yang menyegarkan diri setiap kali
	 * dokumen dibaca.</p>
	 * <p><b>Konsekuensi:</b></p>
	 * <ul>
	 *   <li>memindahkan seorang pegawai ke satuan kerja lain akan <b>memindahkan seluruh dokumen
	 *       pengajuan historisnya</b> ke tenant baru begitu dokumen-dokumen itu dibaca — mengubah
	 *       siapa yang berhak melihatnya, tanpa satu pun aksi pengguna yang tercatat;</li>
	 *   <li>nilai yang disetel {@code onSave()} dari kotak pilihan satuan kerja pada layar
	 *       <b>tidak bertahan</b> bila pegawainya punya satuan kerja sendiri — pilihan pengguna
	 *       diam-diam diabaikan;</li>
	 *   <li>dua sumbu pemeriksaan CRUD generik v2 karena itu membaca hal berbeda:
	 *       {@code applyScope()} menyaring kolom SQL tersimpan, sedangkan
	 *       {@code validateObjectScope()} membaca nilai getter ini (yang diturunkan dari pegawai).
	 *       Ini mekanisme yang sama dengan yang dilacak <code>task_c9d4d09f</code>, hanya di sini
	 *       arahnya kebetulan membuat validasi objek <i>lebih</i> ketat daripada penyaring
	 *       daftarnya — kecuali untuk dokumen yang {@link #getPegawai()}-nya {@code null}, yang
	 *       jatuh kembali ke nilai tersimpan.</li>
	 * </ul>
	 * <p><b>Cakupan fail-open.</b> Pada CRUD generik v2 pembatas hanya dipasang bila
	 * {@code Tbmuser.getSatuanKerja()} tidak {@code null}; bila null, {@code addScope()} berhenti
	 * dan dokumen <b>seluruh tenant</b> terbuka. Pada layar ZK, penyaringnya jatuh ke
	 * <code>1=1</code> ketika daftar satuan kerja pengguna kosong, selalu meng-OR-kan
	 * <code>satuanKerja is null</code>, dan dilewati sepenuhnya pada cabang "punya bawahan".</p>
	 *
	 * @return satuan kerja dokumen, atau {@code null} bila pegawai maupun kolomnya kosong.
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
	 * Menyetel satuan kerja dokumen.
	 *
	 * <p>Nilai yang disetel di sini tidak bertahan bila pegawainya punya satuan kerja sendiri —
	 * lihat {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja satuan kerja.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Disposisi SOP dokumen ini, bila dokumen dijalankan lewat mesin SOP berjenjang.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop}. Getter destruktif ringan
	 * ({@code check()} ditugaskan kembali).</p>
	 * <p>Keberadaannya menentukan <b>mekanisme persetujuan mana</b> yang berlaku: renderer daftar
	 * hanya memasang checkbox "Setujui" satu-klik ketika getter ini {@code null}. Dokumen yang
	 * pernah masuk alur SOP karenanya kehilangan jalur pintas itu — dan sebaliknya, dokumen yang
	 * dibuat di luar layar SOP tidak pernah memperoleh {@code disposisiSop} dan justru memperoleh
	 * jalur pintasnya (pola <code>task_c9d4d09f</code>).</p>
	 * <p>Ia juga menjadi sumber kebenaran {@link #getSetujui()} dan {@link #getAktif()} ketika
	 * terisi.</p>
	 *
	 * @return disposisi SOP, atau {@code null} bila dokumen tidak dijalankan lewat SOP.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP dokumen.
	 *
	 * <p><b>Setter dua lapis penolakan — mustahil melepaskan disposisi.</b> Penjaga pertama
	 * langsung keluar bila argumennya {@code null} atau ber-id {@code null}. Penjaga kedua
	 * mengulang syarat yang sama dalam bentuk ekspresi ternary; karena penjaga pertama sudah
	 * menyaring kasus itu, cabang "pertahankan nilai lama" pada ternary tersebut
	 * <b>tidak pernah tercapai</b> dan penugasannya selalu memakai argumen. Efek gabungannya:
	 * disposisi hanya bisa <i>diganti</i> dengan disposisi tersimpan lain, tidak pernah dilepas
	 * kembali menjadi {@code null} lewat jalur ini — sehingga dokumen yang sekali masuk alur SOP
	 * tidak dapat dikembalikan ke jalur persetujuan satu-klik dari kode.</p>
	 *
	 * @param disposisiSop disposisi SOP; {@code null} atau yang belum tersimpan diabaikan.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Tahun periode dokumen — dipakai penghitung nomor agenda untuk mereset urutan tiap tahun.
	 *
	 * <p><b>Getter destruktif.</b> {@code null} diganti tahun berjalan dan ditugaskan ke field,
	 * sehingga ikut tersimpan pada flush berikutnya. Nilainya karena itu bukan "tahun dokumen
	 * diajukan" melainkan "tahun saat kolom ini pertama kali dibaca" — untuk dokumen lama yang
	 * kolomnya masih {@code null}, membuka daftarnya hari ini akan menstempelkan tahun ini.</p>
	 * <p>Perhatikan tidak ada penyelarasan dengan {@link #getWaktu()}: kedua nilai bisa berbeda
	 * tahun tanpa ada yang memperbaikinya.</p>
	 *
	 * @return tahun periode; tidak pernah {@code null}.
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode dokumen.
	 *
	 * @param tahun tahun periode.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Bulan periode dokumen (1-12) — dipakai penghitung nomor agenda untuk mereset urutan tiap
	 * bulan.
	 *
	 * <p><b>Getter destruktif</b> dengan perilaku sama seperti {@link #getTahun()}: {@code null}
	 * diganti bulan berjalan (sudah dinormalkan dari basis-0 {@code Calendar} menjadi 1-12) lalu
	 * ditugaskan ke field.</p>
	 *
	 * @return bulan periode, 1-12; tidak pernah {@code null}.
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode dokumen.
	 *
	 * @param bulan bulan periode, 1-12.
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Pegawai pemohon — subjek dokumen dan sumber pegawai bagi seluruh baris angsurannya.
	 *
	 * <p>Getter destruktif ringan ({@code check()} ditugaskan kembali).</p>
	 * <p>Perannya berlapis: ia menurunkan {@link #getSatuanKerja()} (dan karenanya menentukan
	 * cakupan tenant dokumen), dibaca hidup oleh <code>TransaksiPegawai.getPegawai()</code>
	 * sehingga menentukan <b>gaji siapa</b> yang dipotong, dan menjadi dasar gerbang persetujuan
	 * (penyetuju harus atasan pegawai ini, dan tidak boleh pegawai ini sendiri).</p>
	 * <p>Layar mengunci kotak pemilihnya: untuk dokumen baru ia diisi dari pegawai pemilik sesi
	 * lalu di-{@code disable}, dan untuk dokumen tersimpan selalu di-{@code disable}. Penguncian
	 * itu murni sisi tampilan — jalur CRUD generik v2 tidak menirunya, dan {@code pegawai} tidak
	 * termasuk whitelist {@code scopeBindings()} (pola <code>task_7b6038ac</code>).</p>
	 * <p>Kolomnya nullable, dan beberapa pembaca (mis. renderer daftar) memanggil
	 * {@code getPegawai().getMycode()} tanpa pemeriksaan {@code null} — dokumen tanpa pegawai
	 * akan menggagalkan render barisnya.</p>
	 *
	 * @return pegawai pemohon, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai pemohon.
	 *
	 * <p>Mengubahnya pada dokumen yang angsurannya sudah terbit <b>memindahkan potongan gaji ke
	 * pegawai lain secara retroaktif</b>, karena baris angsuran menurunkan pegawainya hidup dari
	 * sini. Layar tidak mengizinkannya (kotak pemilih dikunci), tetapi tidak ada penjaga di
	 * lapisan model maupun CRUD generik.</p>
	 *
	 * @param pegawai pegawai pemohon.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Pengguna yang menyetujui dokumen ini.
	 *
	 * <p>Getter destruktif ringan ({@code check()} ditugaskan kembali) — berbeda dari beberapa
	 * getter penyetuju di modul akunting, getter ini <b>tidak</b> pernah menugaskan {@code null}
	 * dan karenanya tidak dapat mencabut persetujuan hanya dengan dibaca.</p>
	 * <p>Diisi listener checkbox "Setujui" dengan pengguna yang sedang login, dan
	 * <b>dikosongkan</b> oleh listener yang sama ketika centangnya dicabut — tanpa menyisakan jejak
	 * siapa yang pernah menyetujui. Jalur SOP tidak mengisi kolom ini sama sekali: dokumen yang
	 * disetujui lewat disposisi tetap menampilkan penyetuju kosong di daftar meskipun
	 * {@link #getSetujui()} bernilai benar.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum/tidak pernah disetujui lewat
	 *         checkbox.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);
		return disetujiOleh;
	}

	/**
	 * Menyetel pengguna penyetuju.
	 *
	 * <p>Menerima {@code null} — dipakai listener checkbox untuk menghapus jejak penyetuju saat
	 * centang dicabut.</p>
	 *
	 * @param disetujiOleh pengguna penyetuju; boleh {@code null}.
	 */
	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	/**
	 * Waktu persetujuan.
	 *
	 * <p>Getter murni, tanpa substitusi maupun tulis-balik.</p>
	 * <p>Seperti {@link #getDisetujuiOleh()}, hanya diisi jalur checkbox dan dikosongkan kembali
	 * ketika centangnya dicabut; jalur SOP tidak menyentuhnya.</p>
	 *
	 * @return waktu persetujuan, atau {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {
		return setujuiTanggal;
	}

	/**
	 * Menyetel waktu persetujuan.
	 *
	 * @param setujuiTanggal waktu persetujuan; boleh {@code null}.
	 */
	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	/**
	 * Bendera persetujuan — <b>satu-satunya gerbang</b> antara dokumen pengajuan dan jadwal
	 * angsuran yang benar-benar memotong gaji.
	 *
	 * <p><b>Getter destruktif.</b> Bila {@link #getDisposisiSop()} terisi, field {@code setujui}
	 * <b>ditimpa</b> dengan hasil <code>disposisiSetuju != null</code> pada setiap pembacaan; nilai
	 * itu ikut tersimpan pada flush berikutnya. Untuk dokumen ber-SOP, kolom ini karena itu bukan
	 * data melainkan cermin keadaan disposisi. Bila disposisinya {@code null}, nilai tersimpan
	 * dipakai apa adanya dan {@code null} dilaporkan sebagai {@code false}.</p>
	 * <p><b>Peran dalam <code>task_f2f37db5</code>.</b> Nilai getter inilah yang dibaca
	 * <code>PengajuanTransaksiPegawaiAction.populateTransaksi()</code> <i>setelah</i> ia menghapus
	 * seluruh baris angsuran: benar → bangkitkan ulang sebanyak {@link #getJumlahAngsur()} baris
	 * baru berstatus belum-diposting; salah → tidak membangkitkan apa pun, sehingga jadwal angsuran
	 * lenyap seluruhnya. Karena penghapusan mendahului pemeriksaan dan tidak bersyarat, satu klik
	 * pencabutan centang cukup untuk menghapus angsuran yang sudah diposting dan sudah
	 * dipotongkan.</p>
	 * <p>Perhatikan bahwa {@link #getAktif()} — yang menjadi {@code false} ketika disposisinya
	 * ditolak — <b>tidak pernah</b> ikut diperiksa di sana: dokumen yang sudah <i>ditolak</i> lewat
	 * SOP tetap membangkitkan ulang angsurannya selama bendera ini masih benar.</p>
	 * <p>Layar daftar menyaring bendera ini secara bawaan ke
	 * <code>setujui is null or setujui = false</code>, sehingga kotak centang "aktif" pada
	 * pencarian sebenarnya berarti "tampilkan yang belum disetujui saja".</p>
	 *
	 * @return {@code true} bila dokumen disetujui; tidak pernah {@code null}.
	 */
	public Boolean getSetujui() {

		if (getDisposisiSop() != null) {
			setujui = getDisposisiSop().getDisposisiSetuju() != null;
		}

		return setujui == null ? false : setujui;
	}

	/**
	 * Menyetel bendera persetujuan.
	 *
	 * <p>Dipanggil listener checkbox "Setujui" untuk kedua arah. Untuk dokumen ber-SOP nilai yang
	 * disetel di sini akan ditimpa lagi pada pembacaan berikutnya — lihat {@link #getSetujui()}.</p>
	 * <p><b>Peringatan:</b> setiap perubahan bendera ini yang diikuti pemanggilan
	 * <code>populateTransaksi()</code> membangun ulang seluruh jadwal angsuran dari nol. Jangan
	 * memanggil setter ini dari kode baru tanpa memahami akibatnya pada baris angsuran yang sudah
	 * diposting.</p>
	 *
	 * @param setujui bendera persetujuan.
	 */
	public void setSetujui(Boolean setujui) {
		this.setujui = setujui;
	}

	/**
	 * Pengguna yang mengajukan dokumen ini.
	 *
	 * <p>Getter destruktif ringan ({@code check()} ditugaskan kembali).</p>
	 * <p>Diisi {@code onSave()} <b>hanya pada penyimpanan pertama</b> dengan pengguna yang sedang
	 * login, jadi ia merekam pembuat dokumen dan tidak berubah lagi. Nilainya juga dipakai kriteria
	 * daftar cabang "punya bawahan" untuk menampilkan dokumen yang <i>diajukan oleh</i> bawahan,
	 * berdampingan dengan dokumen yang <i>atas nama</i> bawahan.</p>
	 * <p>Bersama pemeriksaan "penyetuju bukan pegawai pemohon" pada renderer, kolom inilah dasar
	 * pemisahan tugas pembuat/penyetuju yang tersedia di modul ini — perlu dicatat bahwa
	 * pemeriksaannya dilakukan terhadap {@link #getPegawai()}, bukan terhadap kolom ini, sehingga
	 * seorang atasan yang <i>mengajukan</i> dokumen atas nama bawahannya tetap boleh menyetujui
	 * dokumen yang ia buat sendiri.</p>
	 *
	 * @return pengguna pengaju, atau {@code null} untuk dokumen lama/impor.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Tbmuser getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);
		return diajukanOleh;
	}

	/**
	 * Menyetel pengguna pengaju.
	 *
	 * @param diajukanOleh pengguna pengaju.
	 */
	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/**
	 * Bendera aktif/berlaku dokumen — dipakai sebagai penanda "belum dibatalkan/ditolak".
	 *
	 * <p><b>Getter destruktif satu arah.</b> Ia menurunkan nilainya dari disposisi SOP dan
	 * menugaskannya ke field (ikut tersimpan pada flush berikutnya), tetapi <b>hanya ke arah
	 * {@code false}</b>:</p>
	 * <ol>
	 *   <li>disposisi ada dan disposisinya sendiri tidak aktif → {@code false};</li>
	 *   <li>disposisi ada, punya {@code disposisiEnd}, dan alur SOP di titik akhir itu ditandai
	 *       {@code getPenolakanAdaDiSini()} — artinya dokumen berhenti di simpul penolakan →
	 *       {@code false};</li>
	 *   <li>selain itu nilai tersimpan dipakai apa adanya, dengan {@code null} dilaporkan sebagai
	 *       {@code true}.</li>
	 * </ol>
	 * <p>Karena tidak ada cabang yang menugaskan {@code true}, dokumen yang sekali tercap
	 * {@code false} <b>tidak pernah pulih sendiri</b> meskipun disposisinya kemudian diaktifkan
	 * kembali; pemulihannya harus lewat {@link #setAktif(Boolean)}.</p>
	 * <p><b>Rantai pemanggilan bersarang:</b> method ini memanggil {@link #getDisposisiSop()} dan
	 * kemudian menelusuri {@code disposisiEnd → alurSop}, sehingga sebuah pembacaan bendera
	 * sederhana dapat menerbitkan beberapa query lazy sekaligus. Baris pertamanya juga menugaskan
	 * hasil {@code getDisposisiSop()} ke field {@code disposisiSop} secara langsung (bukan lewat
	 * setter), sehingga penjaga anti-pengosongan pada {@link #setDisposisiSop(DisposisiSop)}
	 * dilewati.</p>
	 * <p><b>Catatan integritas:</b> bendera ini <b>tidak diperiksa</b> oleh
	 * <code>populateTransaksi()</code> — lihat {@link #getSetujui()}.</p>
	 *
	 * @return {@code true} bila dokumen masih berlaku; tidak pernah {@code null}.
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
	 * Menyetel bendera aktif/berlaku.
	 *
	 * <p>Satu-satunya cara mengembalikan dokumen yang sudah tercap tidak aktif — lihat
	 * {@link #getAktif()}. Nilainya tetap dapat ditimpa lagi menjadi {@code false} pada pembacaan
	 * berikutnya selama disposisinya masih berada di simpul penolakan.</p>
	 *
	 * @param aktif bendera aktif.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Tanggal jatuh tempo angsuran <b>pertama</b> — jangkar seluruh jadwal angsuran dokumen ini.
	 *
	 * <p>Getter bersubstitusi (field tidak ditulis balik): {@code null} dilaporkan sebagai hari
	 * ini. Karena substitusinya tidak persisten, dokumen yang kolomnya {@code null} akan
	 * menghasilkan jadwal angsuran yang <b>berpindah setiap hari</b>.</p>
	 * <p><b>Sumber hidup jadwal angsuran.</b> <code>TransaksiPegawai.getTanggal()</code> menghitung
	 * jatuh tempo angsuran ke-<i>N</i> sebagai nilai ini digeser maju <code>(N - 1)</code> bulan,
	 * lalu menuliskannya balik ke baris angsuran. Dari sana ia mengalir ke kolom bulan/tahun
	 * angsuran yang menentukan <b>bulan pemotongan gaji</b>. Mengubah tanggal ini pada dokumen yang
	 * angsurannya sudah terbit karena itu <b>menggeser seluruh jadwal potongan secara retroaktif</b>,
	 * termasuk angsuran yang sudah dijurnal — sementara jurnalnya tetap di bulan lama.</p>
	 * <p>Penggeseran bulan di sisi anak memakai {@code Calendar} lenient tanpa normalisasi hari,
	 * sehingga jatuh tempo tanggal 29-31 dapat bergulir ke bulan berikutnya (lihat catatan pada
	 * <code>TransaksiPegawai.getTanggal()</code>).</p>
	 *
	 * @return tanggal jatuh tempo angsuran pertama; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalJatuhTempo() {
		return tanggalJatuhTempo == null ? WaktuUtil.getDate() : tanggalJatuhTempo;
	}

	/**
	 * Menyetel tanggal jatuh tempo angsuran pertama.
	 *
	 * <p>Diisi {@code onSave()} dari kotak tanggal pada layar, tanpa validasi apa pun (boleh masa
	 * lampau, boleh sebelum {@link #getWaktu()}). Perhatikan akibat retroaktifnya pada angsuran
	 * yang sudah terbit — lihat {@link #getTanggalJatuhTempo()}.</p>
	 *
	 * @param tanggalJatuhTempo tanggal jatuh tempo angsuran pertama.
	 */
	public void setTanggalJatuhTempo(Date tanggalJatuhTempo) {
		this.tanggalJatuhTempo = tanggalJatuhTempo;
	}

	/**
	 * Nominal <b>total</b> yang diajukan — bukan nominal per angsuran.
	 *
	 * <p>Getter bersubstitusi (field tidak ditulis balik): {@code null} dilaporkan sebagai
	 * {@code 0.0}, sehingga pembagian di sisi anak tidak pernah ber-NPE.</p>
	 * <p><b>Sumber hidup nominal angsuran.</b> <code>TransaksiPegawai.getNilai()</code> menghitung
	 * <code>nilaiTransaksi / jumlahAngsur</code> pada setiap pembacaan lalu menuliskannya balik.
	 * Konsekuensinya berlapis: koreksi nominal manual pada baris angsuran tidak pernah bertahan;
	 * mengubah nilai di sini <b>mengubah nominal angsuran yang sudah diposting</b> tanpa menyentuh
	 * jurnalnya; dan pembagiannya tidak dibulatkan sehingga sisa pembagian tersebar sebagai pecahan
	 * di setiap angsuran alih-alih diselesaikan di angsuran terakhir.</p>
	 * <p>Nilai <b>negatif</b> bermakna arah jurnal dibalik di sisi posting (uang masuk, bukan
	 * potongan) — tidak ada validasi tanda di sini maupun di layar.</p>
	 *
	 * @return nominal total; tidak pernah {@code null}.
	 */
	public Double getNilaiTransaksi() {
		return nilaiTransaksi == null ? 0.0 : nilaiTransaksi;
	}

	/**
	 * Menyetel nominal total pengajuan.
	 *
	 * <p>Diisi {@code onSave()} dari kotak nominal pada layar, tanpa batas atas, tanpa larangan
	 * nilai negatif, dan tanpa perbandingan dengan gaji pegawai. Perhatikan akibat retroaktifnya
	 * pada angsuran yang sudah terbit — lihat {@link #getNilaiTransaksi()}.</p>
	 *
	 * @param nilaiTransaksi nominal total.
	 */
	public void setNilaiTransaksi(Double nilaiTransaksi) {
		this.nilaiTransaksi = nilaiTransaksi;
	}

	/**
	 * Banyaknya angsuran — sekaligus <b>banyaknya baris {@code TransaksiPegawai} yang akan
	 * dibangkitkan</b>.
	 *
	 * <p>Getter bersubstitusi (field tidak ditulis balik): {@code null} dilaporkan sebagai
	 * {@code 1}, yang menjadikan pengajuan tanpa jumlah angsuran otomatis berperilaku sebagai
	 * pembayaran sekali lunas.</p>
	 * <p>Dipakai dua kali di jalur kritis: sebagai batas atas perulangan pembangkitan angsuran pada
	 * <code>populateTransaksi()</code>, dan sebagai pembagi pada
	 * <code>TransaksiPegawai.getNilai()</code>.</p>
	 * <p><b>Kasus tepi:</b> nilai {@code 0} yang <b>tersimpan eksplisit</b> tidak dijaga di mana
	 * pun. Perulangan pembangkit tidak akan membuat satu baris pun (batasnya
	 * <code>i &lt;= 0</code>), sementara pembagian di sisi anak menghasilkan {@code Infinity} atau
	 * {@code NaN} untuk baris yang sudah telanjur ada dari penyimpanan sebelumnya — dan nilai itu
	 * ditulis balik ke kolom nominal serta ikut dijurnal. Menyunting jumlah angsuran menjadi
	 * <b>lebih kecil</b> juga memusnahkan baris angsuran berlebih yang mungkin sudah diposting,
	 * karena pembangkitan selalu didahului penghapusan menyeluruh.</p>
	 *
	 * @return banyaknya angsuran; tidak pernah {@code null}, bawaan {@code 1}.
	 */
	public Integer getJumlahAngsur() {
		return jumlahAngsur == null ? 1 : jumlahAngsur;
	}

	/**
	 * Menyetel banyaknya angsuran.
	 *
	 * <p>Diisi {@code onSave()} dari kotak isian pada layar, tanpa batas bawah/atas. Perhatikan
	 * akibatnya pada angsuran yang sudah terbit — lihat {@link #getJumlahAngsur()}.</p>
	 *
	 * @param jumlahAngsur banyaknya angsuran.
	 */
	public void setJumlahAngsur(Integer jumlahAngsur) {
		this.jumlahAngsur = jumlahAngsur;
	}


	/**
	 * Baris antrean pembayaran (Daftar Pengajuan Transfer) yang mewakili dokumen ini di modul
	 * akunting.
	 *
	 * <p>Getter murni — tidak ada {@code check()} maupun tulis-balik, berbeda dari relasi lain di
	 * kelas ini. Karena itu ia dapat mengembalikan proxy yang belum terinisialisasi.</p>
	 * <p>Diisi sekali oleh
	 * {@link DaftarPengajuanTransfer#simpanPengajuanTransaksiPegawai(PengajuanTransaksiPegawai)},
	 * yang dipanggil dari <code>populateTransaksi()</code> <b>hanya pada cabang disetujui</b> dan
	 * <b>hanya bila kolom ini masih {@code null}</b>. Helper itu memakai
	 * {@link #getTanggalJatuhTempo()} sebagai tanggal acuan antrean, bukan
	 * {@link #getWaktu()}.</p>
	 * <p><b>Asimetri yang perlu diketahui:</b> penghapusan angsuran pada
	 * <code>populateTransaksi()</code> tidak pernah melepas kolom ini. Setelah centang "Setujui"
	 * dicabut lalu dipasang kembali, angsuran dibangkitkan ulang tetapi antrean transfernya
	 * <b>tidak</b> didaftarkan ulang — dokumen hidup lagi sebagai potongan gaji sambil menunjuk
	 * baris antrean lama yang sudah tidak selaras. Sebaliknya, sebuah dokumen yang antreannya
	 * sengaja dihapus dari sisi akunting akan didaftarkan ulang pada penyimpanan berikutnya.</p>
	 *
	 * @return baris antrean pembayaran, atau {@code null} bila dokumen belum pernah disetujui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menautkan dokumen ini ke baris antrean pembayaran.
	 *
	 * <p>Dipanggil {@link DaftarPengajuanTransfer#simpanPengajuanTransaksiPegawai(PengajuanTransaksiPegawai)}
	 * setelah baris antreannya tersimpan. Menyetelnya ke {@code null} akan membuat penyimpanan
	 * dokumen berikutnya mendaftarkan antrean baru.</p>
	 *
	 * @param daftarPengajuanTransfer baris antrean pembayaran; boleh {@code null}.
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}
}
