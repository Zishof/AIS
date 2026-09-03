package ais.database.model.payroll;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Katalog master <b>Jenis Transaksi Pegawai</b> &mdash; daftar kategori transaksi kepegawaian
 * <i>di luar</i> komponen slip gaji (tunjangan jabatan, honor mengajar, honor pengawas ujian,
 * asuransi, biaya perjalanan dinas, potongan, angsuran, dan sejenisnya). Satu baris tabel
 * <code>payroll.jenis_transaksi_pegawai</code> menyimpan tiga hal sekaligus:</p>
 * <ol>
 *   <li><b>Identitas kategori</b> &mdash; {@link #getKode() kode} dan {@link #getNama() nama} yang
 *       dipakai seluruh layar transaksi pegawai sebagai isi combobox dan label baris;</li>
 *   <li><b>Pasangan akun jurnal</b> &mdash; {@link #getAkunDebet()} dan {@link #getAkun()}, dua
 *       kaki jurnal <i>double-entry</i> yang dipakai mesin posting saat transaksi pegawai
 *       dibukukan ke buku besar;</li>
 *   <li><b>Nama variabel rumus</b> &mdash; {@link #getFormula()}, penghubung katalog ini ke mesin
 *       rumus komponen gaji {@code ItemGaji}.</li>
 * </ol>
 *
 * <h3>1. Posisi dalam rantai penggajian (terverifikasi)</h3>
 * <p>Kelas ini adalah sisi <i>many-to-one</i> dari
 * {@link ais.database.model.payroll.TransaksiPegawai}: setiap baris transaksi pegawai menunjuk
 * tepat satu baris katalog ini lewat properti <code>jenisTransaksiPegawai</code>. Katalog inilah
 * &mdash; bukan baris transaksinya &mdash; yang memegang akun jurnal, sehingga <b>mengubah satu
 * baris katalog memindahkan atribusi akun untuk SELURUH transaksi pegawai yang memakainya</b>,
 * termasuk yang belum diposting. Rantai kedua berjalan lewat
 * {@code JenisPengajuanTransaksiPegawai.jenisTransaksiPegawai}: katalog pengajuan transaksi
 * pegawai menunjuk kembali ke katalog ini, dan {@code TransaksiPegawai.getJenisTransaksiPegawai()}
 * akan <i>menurunkan</i> jenisnya dari pengajuan bila field lokalnya kosong.</p>
 * <p>Konsumen yang terverifikasi ada di lima tempat:</p>
 * <ul>
 *   <li>{@code ais.action.master.payroll.JenisTransaksiPegawaiAction} &mdash; layar CRUD katalog
 *       ini sendiri (ZK legacy, {@code /WEB-INF/z/x/y/pages/master/payroll/jenis_transaksi_pegawai.zul});</li>
 *   <li>{@code TransaksiPegawaiAction} dan {@code PostingTransaksiPegawaiAction} &mdash; combobox
 *       pemilih jenis, label baris, pratinjau jurnal, dan mesin posting;</li>
 *   <li>{@code CommonAkunting.saveTransaksi(TransaksiPegawai, ...)} &mdash; penerbit jurnal
 *       sesungguhnya;</li>
 *   <li>{@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai(...)} &mdash; mesin rumus gaji;</li>
 *   <li>{@code DasborAnalisisPenggajian} dan {@code DasborPenggajianDetailHelper} &mdash; dua panel
 *       dasbor penggajian;</li>
 *   <li>{@code ais.database.model.akunting.DaftarPengajuanTransfer} &mdash; surat perintah transfer
 *       (lihat butir 3 di bawah).</li>
 * </ul>
 *
 * <h3>2. Bagaimana kedua akun dipakai mesin posting (terverifikasi)</h3>
 * <p>{@code PostingTransaksiPegawaiAction} mengambil <b>keduanya sekaligus</b> lalu memanggil
 * mesin posting:</p>
 * <pre>{@code
 * Akun akunDebet  = transaksiPegawai.getJenisTransaksiPegawai().getAkunDebet();
 * Akun akunKredit = transaksiPegawai.getJenisTransaksiPegawai().getAkun();
 * if (akunDebet != null && akunKredit != null) {
 *     CommonAkunting.saveTransaksi(transaksiPegawai, akunDebet, postingHistory,
 *             transaksiPegawai.getNilai() < 0.0, tbmuser.ambilSatuanKerja(), session);
 * }
 * }</pre>
 * <p>Di dalam {@code saveTransaksi}, {@code akunDebet} dipakai apa adanya untuk kaki pertama dan
 * <code>transaksiPegawai.getJenisTransaksiPegawai().getAkun()</code> dibaca lagi untuk kaki kedua.
 * Tiga konsekuensi yang perlu dipahami sebelum menyentuh kelas ini:</p>
 * <ul>
 *   <li><b>Akun dibaca hidup, bukan snapshot.</b> Tidak ada satu pun kolom di
 *       {@code TransaksiPegawai} yang menyalin id akun. Jadi berbeda dari
 *       {@code JenisReimbursement} (yang di-<i>snapshot</i> saat klaim disimpan), memindahkan
 *       {@link #getAkun()}/{@link #getAkunDebet()} di sini <b>langsung berlaku untuk semua
 *       transaksi yang belum diposting</b>, tanpa jejak di dokumen manapun.</li>
 *   <li><b>Kedua akun wajib terisi atau dokumen dilewati diam-diam.</b> Bila salah satu
 *       {@code null}, cabang {@code if} di atas tidak pernah dimasuki: layar hanya menuliskan
 *       label "Transaksi tidak valid", dan tidak ada exception maupun log. Potongan/tunjangannya
 *       tetap memengaruhi gaji tetapi tidak pernah masuk buku besar. Layar CRUD katalog ini
 *       memang mewajibkan keduanya ({@code onSave()} menolak bila salah satu kosong), tetapi
 *       kolomnya sendiri <code>nullable = true</code> dan jalur tulis lain (impor Excel, Generic
 *       CRUD v2) tidak menegakkan syarat itu.</li>
 *   <li><b>Arah jurnal tidak diambil dari katalog ini.</b> Parameter
 *       <code>apakahUangMasuk</code> disusun dari <code>transaksiPegawai.getNilai() &lt; 0.0</code>
 *       &mdash; semata dari <b>tanda nominal</b>. Lihat butir berikutnya.</li>
 * </ul>
 *
 * <h3>3. Kolom <code>jenisTransaksi</code>: KONFIRMASI ULANG bahwa ia kosmetik</h3>
 * <p>Dokumentasi {@link ais.database.model.payroll.TransaksiPegawai} mencatat bahwa kolom
 * <code>jenisTransaksi</code> pada katalog ini "hanya label dasbor &mdash; mesin posting semata
 * melihat tanda <code>nilai</code>". Klaim itu <b>diperiksa ulang dari kode dan TERKONFIRMASI</b>.
 * Seluruh pembacanya, tanpa kecuali:</p>
 * <ol>
 *   <li>{@code JenisTransaksiPegawaiAction} baris grid &mdash;
 *       <code>getJenisTransaksi().equals(1) ? "Debet" : "Kredit"</code> (label murni);</li>
 *   <li>{@code JenisTransaksiPegawaiAction.init()} &mdash; memilih radio pada form (kosmetik);</li>
 *   <li>{@code DasborAnalisisPenggajian.queryTransaksi()} &mdash; memisah total Debet/Kredit dan
 *       tren bulanan;</li>
 *   <li>{@code DasborPenggajianDetailHelper.panelTunjanganHonor()} &mdash; melewati baris yang
 *       dianggap potongan.</li>
 * </ol>
 * <p>{@code CommonAkunting.saveTransaksi}, {@code PostingTransaksiPegawaiAction} (baik jalur
 * tombol per-baris, jalur "posting semua", maupun jalur pembatalan), dan
 * {@code ItemGajiPegawaiTreeModel} <b>tidak pernah membacanya sama sekali</b>. Jadi sebuah baris
 * katalog boleh bertuliskan "Kredit (potongan)" namun terjurnal sebagai penambah, semata karena
 * nominal transaksinya kebetulan positif.</p>
 * <p><b>Kuirk lanjutan yang ditemukan saat verifikasi ini &mdash; sandi Kredit tidak pernah
 * cocok.</b> Form ZK menulis nilai radio <code>1</code> untuk "Debet" dan <code>-1</code> untuk
 * "Kredit":</p>
 * <pre>{@code
 * radio.setLabel("Debet");  radio.setAttribute("value", 1);
 * radio.setLabel("Kredit"); radio.setAttribute("value", -1);
 * }</pre>
 * <p>Sementara <b>kedua dasbor</b> menguji sandi <code>2</code>
 * (<code>jenis != 2</code> / <code>intValue() == 2</code>). Karena satu-satunya penulis dengan
 * kosakata tetap adalah form ZK di atas &mdash; kolom ini juga <b>tidak ikut</b> dalam daftar
 * kolom impor/cetak Excel layar ini (<code>{"id","kode","nama","keterangan","formula","akun",
 * "akunDebet","aktif"}</code>) &mdash; nilai <code>2</code> praktis tidak pernah tersimpan.
 * Akibatnya baris berjenis Kredit dinilai Debet oleh kedua dasbor: total dan tren "Kredit"
 * cenderung nol, dan panel "Tunjangan &amp; Honor" ikut menjumlahkan potongan sebagai tunjangan.
 * Ini memperkuat sekaligus mempertajam catatan lama: kolom ini bukan hanya tidak dipakai mesin
 * posting, satu-satunya dua pemakainya pun tidak berfungsi sebagaimana dimaksud. (Nilai
 * <code>2</code> secara teori masih bisa masuk lewat form Generic CRUD v2 New UI, yang menurunkan
 * daftar isian langsung dari metadata Hibernate sehingga menampilkan kolom ini sebagai isian
 * bilangan bebas.)</p>
 *
 * <h3>4. {@link #getAkun()} merangkap rekening bank sumber transfer</h3>
 * <p>Temuan yang tidak terlihat dari paket ini sendiri: pada
 * {@code ais.database.model.akunting.DaftarPengajuanTransfer}, akun kredit katalog ini dipakai
 * untuk mengisi <b>identitas rekening sumber</b> surat perintah transfer &mdash;
 * {@code ambilBankSumber()} membaca <code>...getJenisTransaksiPegawai().getAkun().getBank()</code>,
 * {@code ambilAtasNamaSumber()} membaca <code>.getAtasNama()</code>, dan
 * {@code ambilNoRekSumber()} membaca <code>.getNoRek()</code>, seluruhnya lewat rantai
 * {@code pengajuanTransaksiPegawai &rarr; jenisPengajuanTransaksiPegawai &rarr; jenisTransaksiPegawai}.
 * Jadi kolom yang di layar hanya berlabel "Akun Kredit *" sesungguhnya <b>menentukan dari
 * rekening bank mana uang ditarik</b> pada dokumen transfer. Setiap perubahan pada
 * {@link #setAkun(Akun)} perlu dinilai dari dua sisi: dampak jurnal dan dampak instruksi
 * pembayaran.</p>
 *
 * <h3>5. Pemetaan Hibernate dan warisan {@link GeneralValueObject}</h3>
 * <p>Entity dipetakan <b>property-access</b> (anotasi {@code @Id} berada pada
 * {@link #getId()}), sehingga Hibernate membaca nilai lewat <i>getter</i> dan menulis lewat
 * <i>setter</i> &mdash; bukan lewat field. Itu penting karena tiga getter di kelas ini
 * mengembalikan nilai yang sudah diolah ({@link #getFormula()}, {@link #getJenisTransaksi()},
 * {@link #getAktif()}): nilai yang tersimpan ke database adalah nilai <b>hasil olahan</b>, bukan
 * apa yang diberikan pemanggil ke setter. Karena {@code dynamicInsert}/{@code dynamicUpdate}
 * aktif, hanya kolom yang benar-benar berubah yang ikut dalam pernyataan SQL.</p>
 * <p>Properti tanpa {@code @Column} ({@link #getKode()}, {@link #getJenisTransaksi()},
 * {@link #getAktif()}, {@code tanggal_dirubah}) jatuh ke strategi penamaan
 * {@code ais.database.hibernate.MyNamingStrategy}, turunan
 * {@code org.hibernate.cfg.DefaultNamingStrategy}, yang memakai <b>nama properti apa adanya</b>
 * sebagai nama kolom (tanpa konversi ke <i>snake_case</i>).</p>
 * <p><b>Tentang field yang dideklarasikan ulang.</b> {@link GeneralValueObject} <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa yang menyediakan
 * infrastruktur bersama (resolusi proxy lazy {@code check()}, cache, helper session). Hibernate
 * <b>tidak</b> memetakan properti milik induk itu. Karena itu setiap entity turunan <b>wajib</b>
 * mendeklarasikan ulang field yang ingin dipetakan (di sini: {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}). Pengulangan itu <b>keharusan teknis, bukan bug</b>, dan menghapusnya
 * akan menghilangkan kolom-kolom tersebut dari tabel.</p>
 * <p>Entity ber-{@code @Audited} (Envers): setiap perubahan menghasilkan baris revisi di skema
 * audit. Perlu diingat konsekuensi yang sudah didokumentasikan di modul lain &mdash; skema audit
 * yang ketinggalan kolom membuat INSERT audit gagal sehingga penyimpanan entity ikut
 * <i>rollback</i>.</p>
 *
 * <h3>6. Siklus hidup data</h3>
 * <ul>
 *   <li><b>Pra-muat startup.</b> {@code InitData} mendaftarkan kelas ini ke
 *       {@code initClasses(...)}, yang lewat {@code InitDataHelper.doInitData()} memuat seluruh
 *       barisnya ke cache memori JVM saat bootstrap. Bersama {@link GeneralValueObject#check(Object)}
 *       dan {@code EntityIdentityMap}, ini berarti pembaca umumnya menerima instance <i>kanonik</i>
 *       yang sama untuk id yang sama.</li>
 *   <li><b>Kebal pembersihan data.</b> {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} memuat kelas ini,
 *       sehingga isi katalog tidak ikut terhapus oleh rutin pembersihan/reset data.</li>
 *   <li><b>Tanpa penghapusan keras di layar.</b> Baris dinonaktifkan lewat kolom {@link #getAktif()};
 *       seluruh combobox konsumen menyaring dengan
 *       <code>Restrictions.or(isNull("aktif"), eq("aktif", true))</code> &mdash; kebetulan persis
 *       sejalan dengan nilai bawaan {@code true} pada getternya.</li>
 * </ul>
 *
 * <h3>7. Cakupan tenant dan permukaan akses (hasil verifikasi)</h3>
 * <ul>
 *   <li><b>Tidak ada sumbu tenant sama sekali.</b> Entity ini tidak punya {@code sekolah},
 *       {@code yayasan}, {@code satuanKerja}, maupun kolom pemilik institusi lain. Ini bukan
 *       fail-open bersyarat melainkan <i>ketiadaan sumbu</i> &mdash; pola yang sama dengan
 *       {@code Closing} dan {@code ProsesTransferStandingInstruction} di paket akunting: satu
 *       katalog global dipakai bersama seluruh instalasi. Konsekuensi langsung: pemeriksaan
 *       duplikat {@code checkKodeJenisTransaksiPegawai()}/{@code checkNamaJenisTransaksiPegawai()}
 *       pada layar CRUD juga berlaku global, sehingga dua tenant tidak dapat memakai kode yang
 *       sama untuk kategori yang berbeda.</li>
 *   <li><b>Generic CRUD v2 &mdash; terjangkau, tanpa pembatas.</b> Halaman New UI
 *       {@code /WEB-INF/new/payroll/services/jenis_transaksi_pegawai_service.jsp} mendeklarasikan
 *       {@code nuiServiceEntities = {"JenisTransaksiPegawai"}} dan meneruskannya ke dispatcher
 *       scaffold, yang memanggil {@code GenericCrudDefinitionRegistry.tryAutoRegister("payroll",
 *       "jenis_transaksi_pegawai", ...)}. Karena {@code GenericCrudAutoEntityAdapter.scopeBindings()}
 *       hanya mengenal 12 nama properti tetap dan tak satu pun dimiliki kelas ini, peta pengikatan
 *       <b>kosong</b>: {@code applyScope()} tidak menambahkan restriksi apa pun, dan
 *       {@code validateObjectScope()} menelusuri peta kosong yang sama sehingga jalur tulis pun
 *       tidak diperiksa.</li>
 *   <li><b>Koreksi arah perbaikan untuk {@code task_7b6038ac}.</b> Menambahkan {@code pegawai} ke
 *       whitelist <b>tidak</b> menutup celah di sini &mdash; entity ini tidak punya properti
 *       {@code pegawai} sama sekali. Mekanismenya identik dengan {@code ItemGaji}: pengikatan
 *       kosong karena nol properti relasi ada di whitelist manapun. Perbaikan yang benar adalah
 *       memperlakukan pengikatan kosong sebagai <b>penolakan</b>, bukan restriksi-nol.</li>
 *   <li><b>{@code task_66986071} &mdash; verifikasi NEGATIF.</b> Tidak ada satu pun
 *       {@code *ApiHelper} yang menyentuh kelas ini; katalog ini tidak punya permukaan REST
 *       sendiri, sehingga pola fail-open {@code bolehAksi()} tidak menjangkaunya.</li>
 *   <li><b>Pola "checkbox grid tanpa gerbang" ({@code task_0a06e418}) &mdash; verifikasi
 *       NEGATIF.</b> Checkbox "Aktif" pada grid layar ini <b>digerbangi</b>
 *       ({@code checkbox.setDisabled(!edit)}, dengan {@code edit} berasal dari
 *       {@code CommonPrivilages.checkPrevilages(UPDATE)}), berbeda dari {@code JenisKasBesar} dan
 *       {@code CaraPembayaranTransfer}. Tombol Tambah/Ubah/Hapus dan tombol unggah Excel juga
 *       digerbangi. Yang tersisa adalah kelemahan sistemik yang sudah dilacak terpisah: nilai
 *       {@code edit}/{@code delete} bergantung pada atribut sesi {@code currentMenu}
 *       ({@code task_9f520b16}).</li>
 * </ul>
 *
 * <h3>8. Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; penyajian:</b> {@link #getId()}, {@link #setId(Long)},
 *       {@link #toString()}.</li>
 *   <li><b>Atribut katalog:</b> {@link #getKode()}, {@link #setKode(String)}, {@link #getNama()},
 *       {@link #setNama(String)}, {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Akun jurnal:</b> {@link #getAkun()}, {@link #setAkun(Akun)}, {@link #getAkunDebet()},
 *       {@link #setAkunDebet(Akun)} &mdash; kedua getter meresolusi proxy lazy.</li>
 *   <li><b>Getter berperilaku (bukan getter polos):</b> {@link #getFormula()} (menormalkan spasi),
 *       {@link #getJenisTransaksi()} (bawaan 1), {@link #getAktif()} (bawaan {@code true}).</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, dan kait {@code onUpdate()}.</li>
 * </ul>
 *
 * <p><b>Tidak ada method bisnis sesungguhnya di kelas ini.</b> Seluruh logika yang memakainya
 * (posting jurnal, evaluasi rumus, agregasi dasbor, pembentukan surat transfer) tinggal di kelas
 * lain. Kelas ini murni pembawa data + tiga getter berperilaku ringan.</p>
 *
 * @see ais.database.model.payroll.TransaksiPegawai
 * @see ais.database.model.payroll.JenisPengajuanTransaksiPegawai
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "jenis_transaksi_pegawai")
public class JenisTransaksiPegawai extends GeneralValueObject {

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
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} dan, pada
	 * jalur New UI, juga oleh {@code GenericCrudAutoEntityAdapter.applyContextDefaults()} yang
	 * menyalin {@code user.getUserId()} ke properti bernama {@code olehId} bila masih kosong.</p>
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh interceptor
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null}
	 * maupun string kosong/spasi &mdash; nilai lama dipertahankan. Jadi kolom ini tidak dapat
	 * dikosongkan kembali lewat setter; sekali terisi ia hanya bisa ditimpa nilai lain yang tidak
	 * kosong. Pola ini seragam untuk seluruh entity AIS dan disengaja agar stempel audit tidak
	 * terhapus oleh jalur simpan yang tidak menyertakan konteks pengguna.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan tanpa efek dan tanpa exception
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris katalog dalam bentuk <code>"kode - nama"</code>.
	 *
	 * <p>Bukan sekadar bantuan <i>debug</i>: bentuk ini benar-benar tampil ke pengguna dan ikut
	 * masuk ke data resmi. {@code TransaksiPegawaiAction} dan {@code PostingTransaksiPegawaiAction}
	 * memakainya sebagai label kolom "Jenis" pada grid, dan
	 * {@code CommonAkunting.saveTransaksi(TransaksiPegawai, ...)} menyusun <b>keterangan jurnal</b>
	 * yang tersimpan permanen di {@code GrupTransaksi}/{@code Transaksi} dengan potongan
	 * <code>"jenis (" + jenisTransaksiPegawai.toString() + ")"</code>. Mengubah format method ini
	 * mengubah teks buku besar untuk jurnal yang terbit sesudahnya.</p>
	 *
	 * <p>Membaca field {@code kode} dan {@code nama} <b>langsung</b>, bukan lewat getter; keduanya
	 * boleh {@code null} sehingga hasilnya bisa berupa <code>"null - null"</code> untuk baris yang
	 * belum tersimpan.</p>
	 *
	 * @return gabungan kode dan nama dipisah " - "
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong <b>diabaikan
	 * diam-diam</b> sehingga stempel audit lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang membuat/mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor}; pada jalur New UI juga oleh
	 * {@code GenericCrudAutoEntityAdapter.applyContextDefaults()} (properti {@code oleh}).</p>
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> pernyataan
	 * {@code UPDATE} dieksekusi, dan meneruskan entity ini ke
	 * {@code AuditTimestampInterceptor.ubah(this)} yang memperbarui {@link #getTanggal_dirubah()}
	 * serta stempel {@code oleh}/{@code olehId} dari konteks pengguna aktif.
	 *
	 * <p>Tidak dipanggil pada {@code INSERT} &mdash; nilai awal {@code tanggal_dirubah} datang dari
	 * inisialisasi field pada deklarasi yang berada di baris yang sama
	 * ({@code = ais.ui.util.WaktuUtil.getDate()}). {@code WaktuUtil} dipakai, bukan
	 * {@code new Date()}, agar seluruh aplikasi memakai satu sumber waktu yang bisa digeser untuk
	 * keperluan uji/backdate.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state entity ini di dalam siklus <i>flush</i> Hibernate.
	 * Jangan panggil manual.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi &mdash; pengisiannya diserahkan ke
	 * {@link #onUpdate()}. Menetapkannya manual akan menimpa stempel audit.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolom mengikuti nama properti apa adanya
	 * ({@code tanggal_dirubah}) sesuai {@code MyNamingStrategy}. Dipetakan
	 * {@code TemporalType.TIMESTAMP} (tanggal + jam).</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat di
	 *         memori karena field-nya diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode singkat katalog, sekaligus nama variabel per-jenis di mesin rumus; lihat {@link #getKode()}. */
	private String kode;
	/** Nama katalog yang tampil di combobox dan dasbor; lihat {@link #getNama()}. */
	private String nama;
	/** Nama variabel rumus gabungan lintas jenis; lihat {@link #getFormula()}. */
	private String formula;
	/** Catatan bebas operator; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Sandi arah (1 = Debet, -1 = Kredit menurut form ZK); lihat {@link #getJenisTransaksi()}. */
	private Integer jenisTransaksi;
	/** Akun kaki KREDIT jurnal, merangkap rekening sumber transfer; lihat {@link #getAkun()}. */
	private Akun akun;
	/** Akun kaki DEBET jurnal; lihat {@link #getAkunDebet()}. */
	private Akun akunDebet;
	/** Bendera aktif/nonaktif katalog; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan dipakai
	 * {@code GenericCrudAutoEntityAdapter.createNew()} lewat refleksi.
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah diisi pada
	 * deklarasinya. Perhatikan bahwa object hasil konstruktor ini akan tampak sebagai jenis "Debet"
	 * yang "aktif" bila dibaca lewat {@link #getJenisTransaksi()}/{@link #getAktif()}, karena
	 * kedua getter itu memberi nilai bawaan.</p>
	 */
	public JenisTransaksiPegawai() {
	}

	/**
	 * Mengembalikan kunci utama baris katalog ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} (dibangkitkan database) dan {@code insertable = false},
	 * sehingga nilai tidak boleh ditetapkan sendiri saat menyimpan baris baru. Nilai {@code null}
	 * dipakai layar CRUD sebagai penanda "baris baru" ({@code init()} memilih judul jendela
	 * Tambah/Ubah berdasarkan hal ini) dan oleh kedua pemeriksa duplikat untuk memutuskan apakah
	 * baris ini sendiri perlu dikecualikan dari hitungan.</p>
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
	 * Menetapkan kunci utama. Dipakai Hibernate setelah {@code INSERT}; kode aplikasi umumnya tidak
	 * memanggilnya.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama katalog, mis. "Tunjangan Jabatan" atau "Honor Mengajar".
	 *
	 * <p>Kolom {@code nama} bersifat {@code NOT NULL} sepanjang 255 karakter. Nilai ini bukan
	 * sekadar label: {@code DasborPenggajianDetailHelper.cocokKategori()} <b>mencocokkan
	 * substring</b> nama ini terhadap 11 kata kunci tetap ("jabatan", "lembur", "asuransi",
	 * "perjalanan dinas", "mengajar", "koreksi", "pengawas", dan seterusnya) untuk menentukan di
	 * kartu dasbor mana nominalnya dijumlahkan; yang tidak cocok masuk keranjang "lain-lain".
	 * Mengganti nama sebuah katalog karena itu dapat memindahkan angka antar kartu dasbor tanpa
	 * ada perubahan data transaksi apa pun.</p>
	 *
	 * <p>Keunikan nama ditegakkan hanya di lapisan layar
	 * ({@code JenisTransaksiPegawaiAction.checkNamaJenisTransaksiPegawai()}), bukan oleh
	 * <i>constraint</i> database, dan berlaku global lintas tenant.</p>
	 *
	 * @return nama katalog
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama katalog. Wajib terisi (kolom {@code NOT NULL}); layar CRUD menolak simpan
	 * bila kosong.
	 *
	 * @param nama nama katalog
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas operator untuk baris katalog ini.
	 *
	 * <p>Murni deskriptif &mdash; hanya ditampilkan sebagai kolom grid pada layar CRUD dan ikut
	 * dalam daftar kolom cetak/impor Excel. Tidak dibaca mesin posting, mesin rumus, maupun
	 * dasbor.</p>
	 *
	 * @return keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas operator.
	 *
	 * @param keterangan keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode singkat katalog (mis. "TJB", "HMJ").
	 *
	 * <p>Punya <b>tiga</b> peran terverifikasi:</p>
	 * <ol>
	 *   <li>bagian pertama {@link #toString()}, sehingga ikut masuk ke keterangan jurnal permanen;</li>
	 *   <li>kolom pertama grid dan kunci pencarian pada seluruh combobox konsumen
	 *       ({@code Common.insertCombo(..., "nama", "kode", JenisTransaksiPegawai.class, ...)});</li>
	 *   <li><b>nama variabel di mesin rumus gaji</b> &mdash;
	 *       {@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai()} menjumlahkan seluruh
	 *       {@code TransaksiPegawai} milik pegawai yang bersangkutan per jenis, lalu memasukkan
	 *       hasilnya ke peta variabel dengan <b>kode ini sebagai kuncinya</b>. Rumus komponen gaji
	 *       {@code ItemGaji} kemudian dapat menyebut kode tersebut sebagai variabel. Konsekuensinya:
	 *       <b>mengganti kode di sini diam-diam melumpuhkan setiap rumus gaji yang menyebutnya</b>
	 *       &mdash; variabel yang tidak lagi ada tidak menimbulkan error yang terlihat, komponennya
	 *       hanya berhenti bernilai.</li>
	 * </ol>
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya {@code kode} apa adanya. Keunikan hanya
	 * ditegakkan {@code JenisTransaksiPegawaiAction.checkKodeJenisTransaksiPegawai()} (global,
	 * lintas tenant), bukan oleh <i>constraint</i> database; jalur impor Excel dan Generic CRUD v2
	 * tidak melewati pemeriksaan itu.</p>
	 *
	 * @return kode katalog; boleh {@code null} untuk baris yang belum tersimpan
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan kode singkat katalog.
	 *
	 * <p>Layar CRUD memanggil ini dengan nilai yang sudah di-{@code trim()} dan menolak simpan bila
	 * kosong. Baca peringatan pada {@link #getKode()} mengenai dampak penggantian kode terhadap
	 * rumus gaji sebelum mengubah nilai ini pada baris yang sudah dipakai.</p>
	 *
	 * @param kode kode katalog
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan akun <b>kaki KREDIT</b> jurnal untuk transaksi pegawai berjenis ini &mdash; di
	 * layar CRUD berlabel "Akun Kredit *".
	 *
	 * <p><b>Dua peran, satu kolom.</b> Selain menjadi kaki kredit jurnal
	 * ({@code CommonAkunting.saveTransaksi} menugaskannya ke {@code transaksiAkunKredit}), akun ini
	 * juga menjadi <b>rekening sumber</b> surat perintah transfer: {@code DaftarPengajuanTransfer}
	 * membaca {@code getAkun().getBank()}, {@code .getAtasNama()}, dan {@code .getNoRek()} lewat
	 * rantai pengajuan transaksi pegawai. Lihat butir 4 pada Javadoc kelas.</p>
	 *
	 * <p><b>Penugasan kembali ke field ({@code akun = check(akun)}) adalah pola standar entity AIS,
	 * bukan getter destruktif.</b> {@link GeneralValueObject#check(Object)} hanya <i>meresolusi
	 * proxy lazy</i> menjadi object yang bisa dipakai (lewat {@code EntityIdentityMap}, cache,
	 * session aktif, atau <i>reload</i>) dan mengembalikan instance kanonik untuk pasangan
	 * kelas+id yang sama. Penugasan kembali diperlukan justru karena hasilnya bisa berupa instance
	 * lain. Berbeda tajam dari {@code Transaksi.getAkun()} di paket akunting, yang menimpa akun
	 * dengan nilai field <i>lain</i> ({@code akunOver}) sehingga benar-benar memindahkan atribusi
	 * buku besar hanya dengan dibaca &mdash; <b>di sini tidak ada penimpaan semacam itu</b>
	 * (verifikasi negatif). {@code check()} juga tidak pernah melempar exception dan tidak pernah
	 * mengembalikan {@code null} untuk argumen non-null.</p>
	 *
	 * <p>Relasi {@code @ManyToOne} LAZY dengan {@code cascade = {PERSIST, MERGE}}: menyimpan baris
	 * katalog ikut menyimpan/menggabungkan object {@link Akun} yang menempel padanya. Kolom
	 * {@code akun} {@code nullable = true} &mdash; bila kosong, seluruh transaksi pegawai berjenis
	 * ini <b>tidak akan pernah terjurnal</b> dan dilewati diam-diam oleh mesin posting.</p>
	 *
	 * @return akun kaki kredit, atau {@code null} bila belum dipilih
	 * @see #getAkunDebet()
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun kaki KREDIT jurnal.
	 *
	 * <p><b>Efek retroaktif yang perlu disadari:</b> tidak ada dokumen hilir yang menyalin id akun
	 * ini. Karena mesin posting membacanya hidup saat posting dijalankan, mengganti akun di sini
	 * memindahkan atribusi buku besar untuk <b>seluruh transaksi pegawai berjenis ini yang belum
	 * diposting</b> &mdash; dan sekaligus memindahkan rekening bank sumber pada surat perintah
	 * transfer yang dicetak sesudahnya. Jurnal yang sudah terbit tidak ikut berubah.</p>
	 *
	 * <p>Dipanggil dari {@code JenisTransaksiPegawaiAction.onSave()} (nilai diambil dari
	 * {@code AmbilDataAkunBanbox}, yang menolak simpan bila kosong) dan &mdash; lewat metadata
	 * Hibernate, bukan setter ini &mdash; dari {@code GenericCrudAutoEntityAdapter} pada jalur New
	 * UI, yang menerima id akun mentah dari klien tanpa pembatas cakupan.</p>
	 *
	 * @param akun akun kaki kredit; boleh {@code null}, dengan konsekuensi transaksi berjenis ini
	 *             tidak akan terjurnal
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan akun <b>kaki DEBET</b> jurnal untuk transaksi pegawai berjenis ini &mdash; di
	 * layar CRUD berlabel "Akun Debet *".
	 *
	 * <p>Dibaca {@code PostingTransaksiPegawaiAction} bersama {@link #getAkun()} lalu diteruskan
	 * sebagai parameter {@code akunDebet} ke {@code CommonAkunting.saveTransaksi}. Berbeda dari
	 * {@link #getAkun()}, akun ini <b>tidak</b> punya peran kedua &mdash; ia semata kaki jurnal;
	 * {@code DaftarPengajuanTransfer} tidak pernah membacanya.</p>
	 *
	 * <p>Sama seperti {@link #getAkun()}, {@code akunDebet = check(akunDebet)} hanyalah resolusi
	 * proxy lazy standar, bukan penimpaan nilai. Relasi LAZY, {@code cascade = {PERSIST, MERGE}},
	 * kolom {@code akun_debet} {@code nullable = true} dengan konsekuensi yang sama: transaksi
	 * dilewati diam-diam oleh mesin posting bila salah satu kaki kosong.</p>
	 *
	 * <p><b>Catatan penamaan yang mudah menyesatkan:</b> pasangan getter ini <i>tidak</i> simetris
	 * &mdash; kaki debet bernama {@code akunDebet} sedangkan kaki kredit bernama {@code akun}
	 * (tanpa akhiran). Salah membaca pasangan ini akan membalik seluruh jurnal transaksi
	 * pegawai.</p>
	 *
	 * @return akun kaki debet, atau {@code null} bila belum dipilih
	 * @see #getAkun()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_debet", nullable = true)
	public Akun getAkunDebet() {
		akunDebet = check(akunDebet);
		return akunDebet;
	}

	/**
	 * Menetapkan akun kaki DEBET jurnal.
	 *
	 * <p>Berlaku peringatan retroaktif yang sama dengan {@link #setAkun(Akun)}: nilainya dibaca
	 * hidup oleh mesin posting, sehingga perubahan di sini memindahkan atribusi buku besar untuk
	 * seluruh transaksi berjenis ini yang belum diposting.</p>
	 *
	 * @param akunDebet akun kaki debet; boleh {@code null}, dengan konsekuensi transaksi berjenis
	 *                  ini tidak akan terjurnal
	 */
	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

	/**
	 * Mengembalikan sandi arah transaksi, dengan <b>nilai bawaan 1</b> bila kolomnya {@code null}.
	 *
	 * <h4>Kosakata nilai (tidak konsisten &mdash; baca sebelum memakai)</h4>
	 * <ul>
	 *   <li>Form ZK menulis <code>1</code> untuk radio "Debet" dan <code>-1</code> untuk radio
	 *       "Kredit";</li>
	 *   <li>label grid layar CRUD menguji <code>equals(1) ? "Debet" : "Kredit"</code> &mdash;
	 *       cocok dengan kedua nilai di atas;</li>
	 *   <li>kedua dasbor penggajian menguji sandi <code>2</code> untuk Kredit
	 *       ({@code DasborAnalisisPenggajian}: <code>jenis != 2</code> berarti Debet;
	 *       {@code DasborPenggajianDetailHelper}: <code>intValue() == 2</code> berarti dilewati).</li>
	 * </ul>
	 * <p>Karena penulis dengan kosakata tetap hanya form ZK &mdash; dan kolom ini tidak termasuk
	 * daftar kolom impor/cetak Excel layar ini &mdash; sandi <code>2</code> praktis tidak pernah
	 * tersimpan, sehingga <b>kedua dasbor menggolongkan baris Kredit sebagai Debet</b>. Lihat
	 * butir 3 pada Javadoc kelas.</p>
	 *
	 * <h4>Kolom ini TIDAK memengaruhi jurnal</h4>
	 * <p>Diverifikasi ulang dari kode: {@code CommonAkunting.saveTransaksi},
	 * {@code PostingTransaksiPegawaiAction} (seluruh jalurnya), dan
	 * {@code ItemGajiPegawaiTreeModel} tidak pernah membaca properti ini. Arah jurnal ditentukan
	 * semata oleh tanda nominal: {@code apakahUangMasuk = transaksiPegawai.getNilai() < 0.0}.
	 * Sebuah baris karenanya bisa berjenis "Kredit" di katalog namun terjurnal sebagai penambah,
	 * tanpa satu pun penjaga yang menyelaraskan keduanya.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> tanpa {@code @Column}, sehingga kolomnya bernama
	 * {@code jenisTransaksi} apa adanya sesuai {@code MyNamingStrategy}. Karena entity dipetakan
	 * <i>property-access</i>, nilai bawaan 1 di sini ikut tertulis ke database saat {@code INSERT}
	 * baris yang jenisnya tidak ditetapkan. Perhatikan pula bahwa getter ini <b>tidak pernah
	 * mengembalikan {@code null}</b>, sehingga pemeriksaan
	 * <code>j.getJenisTransaksi() != null</code> di {@code DasborPenggajianDetailHelper} selalu
	 * bernilai benar (pemeriksaan mati). Kedua dasbor sendiri melewati getter ini &mdash; keduanya
	 * membaca kolomnya langsung lewat proyeksi Criteria, lalu menerapkan bawaan 1 sendiri di
	 * Java.</p>
	 *
	 * @return sandi arah; tidak pernah {@code null} (1 bila kolomnya kosong)
	 */
	public Integer getJenisTransaksi() {
		return jenisTransaksi == null ? 1 : jenisTransaksi;
	}

	/**
	 * Menetapkan sandi arah transaksi.
	 *
	 * <p>Satu-satunya pemanggil di seluruh repo adalah
	 * {@code JenisTransaksiPegawaiAction.onSave()}, yang mengambil nilai dari atribut radio yang
	 * terpilih &mdash; {@code 1} (Debet), {@code -1} (Kredit), atau {@code null} bila tidak ada
	 * yang terpilih. Baca peringatan kosakata pada {@link #getJenisTransaksi()}: nilai
	 * <code>-1</code> yang ditulis di sini tidak dikenali oleh kedua dasbor yang membacanya.</p>
	 *
	 * @param jenisTransaksi sandi arah; {@code null} akan terbaca sebagai 1 (Debet) oleh getternya
	 */
	public void setJenisTransaksi(Integer jenisTransaksi) {
		this.jenisTransaksi = jenisTransaksi;
	}

	/**
	 * Mengembalikan nama variabel rumus untuk jenis transaksi ini, <b>sudah dinormalkan</b>.
	 *
	 * <h4>Getter berperilaku, bukan getter polos</h4>
	 * <p>Nilai yang dikembalikan bukan isi field apa adanya: {@code null} menjadi string kosong,
	 * lalu spasi di ujung dipangkas dan <b>setiap spasi di tengah diganti garis bawah</b>
	 * (<code>"Tunjangan Tetap"</code> &rarr; <code>"Tunjangan_Tetap"</code>). Normalisasi ini
	 * memang perlu: hasilnya dipakai sebagai <i>identifier</i> variabel di mesin rumus, yang tidak
	 * menerima spasi.</p>
	 * <p>Karena entity dipetakan <i>property-access</i>, <b>nilai hasil normalisasi inilah yang
	 * tersimpan ke database</b> &mdash; Hibernate membaca kolom lewat getter ini, bukan lewat
	 * field. Jadi apa pun yang diketik operator, yang tercatat adalah versi bergaris bawah.
	 * Karena nilai hasil normalisasi bersifat idempoten (menormalkan yang sudah normal tidak
	 * mengubah apa-apa), tidak ada efek "tulis-balik" berulang saat entity dimuat lalu di-flush.</p>
	 *
	 * <h4>Peran di mesin rumus (terverifikasi)</h4>
	 * <p>{@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai()} membangun <b>dua</b> peta
	 * variabel dari transaksi pegawai satu orang untuk satu periode:</p>
	 * <ul>
	 *   <li><code>hashMapTransaksi</code>, berkunci {@link #getKode()} &mdash; satu variabel per
	 *       jenis;</li>
	 *   <li><code>hashMapFormulaTransaksi</code>, berkunci nilai method ini &mdash; hanya diisi
	 *       bila hasilnya <b>tidak kosong</b>, dan nominal beberapa jenis yang memakai formula
	 *       sama <b>dijumlahkan menjadi satu variabel</b>.</li>
	 * </ul>
	 * <p>Jadi kolom ini adalah mekanisme <i>pengelompokan</i>: beberapa jenis transaksi berbeda
	 * dapat dijumlahkan sebagai satu variabel rumus dengan cara diberi nilai formula yang sama.
	 * Mengosongkannya berarti jenis tersebut hanya tersedia lewat kodenya sendiri.</p>
	 *
	 * <p>Kolom dipetakan {@code columnDefinition = "text"} sehingga tidak terbatas 255 karakter.</p>
	 *
	 * @return nama variabel yang sudah dinormalkan; <b>tidak pernah {@code null}</b> (string kosong
	 *         bila belum diisi), sehingga aman dipanggil {@code .isEmpty()} langsung seperti yang
	 *         dilakukan {@code ItemGajiPegawaiTreeModel} dan {@code TransaksiPegawaiAction}
	 */
	@Column(columnDefinition = "text", name = "formula")
	public String getFormula() {
		return formula == null ? "" : formula.trim().replaceAll(" ", "_");
	}

	/**
	 * Menetapkan nama variabel rumus mentah, apa adanya.
	 *
	 * <p>Tidak ada validasi di sini &mdash; normalisasi baru terjadi saat dibaca kembali lewat
	 * {@link #getFormula()}. Dipanggil {@code JenisTransaksiPegawaiAction.onSave()} dengan isi
	 * kotak teks "Kode Formula" tanpa {@code trim()}.</p>
	 *
	 * @param formula nama variabel rumus; boleh {@code null} atau kosong (berarti jenis ini tidak
	 *                ikut dalam variabel rumus gabungan)
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	
	/**
	 * Mengembalikan status aktif katalog, dengan <b>nilai bawaan {@code true}</b> bila kolomnya
	 * {@code null}.
	 *
	 * <p>Bawaan ini sengaja sejalan dengan cara seluruh konsumen menyaring katalog:
	 * {@code TransaksiPegawaiAction}, {@code PostingTransaksiPegawaiAction}, dan
	 * {@code JenisPengajuanTransaksiPegawaiAction} memakai persis
	 * <code>Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))</code>
	 * saat mengisi comboboxnya &mdash; jadi baris lama yang kolomnya belum pernah terisi tetap
	 * muncul, baik lewat SQL maupun lewat getter ini. Kesesuaian semacam ini termasuk jarang di
	 * repo ini dan patut dipertahankan bila kolomnya disentuh.</p>
	 *
	 * <p>Nonaktif berarti jenis tersebut tidak lagi bisa <b>dipilih</b> untuk transaksi baru; ia
	 * <b>tidak</b> menghapus atau menyembunyikan transaksi lama yang sudah memakainya, dan tidak
	 * memengaruhi posting maupun mesin rumus.</p>
	 *
	 * <p>Keberadaan properti inilah yang membuat {@code GenericCrudAutoEntityAdapter} menganggap
	 * entity ini mendukung <i>soft delete</i>: {@code delete()} pada jalur New UI tidak menghapus
	 * baris melainkan menugaskan {@code aktif = false}.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga kolomnya bernama {@code aktif} apa adanya.</p>
	 *
	 * @return {@code true} bila katalog aktif atau kolomnya belum pernah diisi; tidak pernah
	 *         {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif katalog.
	 *
	 * <p>Dipanggil dari dua tempat: <i>listener</i> {@code onCheck} pada checkbox "Aktif" di grid
	 * layar CRUD (yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate}, sehingga satu
	 * klik langsung berefek ke database tanpa tombol Simpan) dan jalur <i>soft delete</i> Generic
	 * CRUD v2. Checkbox tersebut digerbangi {@code checkbox.setDisabled(!edit)} dengan {@code edit}
	 * berasal dari {@code CommonPrivilages.checkPrevilages(UPDATE)} &mdash; berbeda dari pola
	 * "checkbox grid tanpa gerbang" yang ditemukan pada beberapa master keuangan.</p>
	 *
	 * @param aktif status aktif; {@code null} akan terbaca sebagai {@code true} oleh getternya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
