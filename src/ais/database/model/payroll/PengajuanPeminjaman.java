package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import org.json.JSONArray;

import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Dokumen <b>pengajuan peminjaman (pinjaman/kasbon) pegawai</b> — rancangan sebuah formulir di mana
 * seorang {@link Pegawai} meminta sejumlah uang ({@link #getNilai()}) kepada instansi, dengan
 * rincian bebas pada {@link #getFormula()}, yang kemudian melewati alur persetujuan SOP
 * ({@link DisposisiSop}) dan pada akhirnya dicap sudah dijurnal lewat
 * {@link PostingHistory}. Dipetakan ke tabel <code>payroll.pengajuan_peminjaman</code> dan
 * diaudit Envers ({@code @Audited}).
 *
 * <h2>PERINGATAN UTAMA — entity ini TIDUR/YATIM: nol pemakai di seluruh repositori</h2>
 * <p>Sebelum apa pun yang lain, satu fakta harus dinyatakan lebih dulu karena ia mengubah cara
 * membaca <i>seluruh</i> sisa berkas ini. Penelusuran menyeluruh atas <code>src/</code> dan
 * <code>webapp/</code> (Java, JSP, ZUL, XML, SQL) menemukan bahwa nama kelas
 * <code>PengajuanPeminjaman</code> hanya muncul di <b>tiga</b> tempat:</p>
 * <ol>
 *   <li>berkas ini sendiri;</li>
 *   <li><code>src/hibernate.cfg.xml</code> baris pemetaan — jadi tabelnya memang dibuat dan
 *       dikelola skema;</li>
 *   <li>{@link TransaksiPegawai} — hanya sebagai deklarasi kolom relasi
 *       <code>pengajuan_peminjaman</code> yang <b>tidak pernah diisi siapa pun</b>.</li>
 * </ol>
 * <p>Selebihnya <b>nol</b>: tidak ada <code>PengajuanPeminjamanAction</code>, tidak ada
 * <code>PostingPengajuanPeminjamanAction</code>, tidak ada <code>ApiHelper</code>, tidak ada
 * berkas <code>.zul</code> maupun <code>.jsp</code> New UI, tidak ada entri menu, tidak ada satu
 * pun query HQL/SQL yang menyebut kelas ini atau tabelnya. Konstruktornya tidak pernah dipanggil;
 * tidak ada satu pun <code>session.save()</code>/<code>saveOrUpdate()</code> atasnya. Dengan kata
 * lain <b>tabel <code>payroll.pengajuan_peminjaman</code> tidak pernah bertambah satu baris pun
 * dari aplikasi</b>. Ini adalah <i>entity tidur/yatim</i> — kategori yang sudah beberapa kali
 * ditemukan di repositori ini (mis. <code>PengumumanJadwalPelajaran</code>,
 * <code>TemplateTransaksi</code>, <code>TemplateGrupTransaksi</code>,
 * <code>DetailLaporanPertahun</code>) — yakni rancangan yang selesai ditulis sebagai model tetapi
 * layar dan mesin pendukungnya tidak pernah (atau tidak lagi) ada.</p>
 * <p><b>Koreksi terhadap catatan sebelumnya.</b> Dokumentasi {@link TransaksiPegawai} menyebut
 * bahwa jembatan <code>TransaksiPegawai.pengajuanPeminjaman</code> tidur "sementara
 * <code>PengajuanPeminjaman</code> sendiri hidup". Bagian pertama benar dan terkonfirmasi ulang di
 * sini; bagian kedua <b>tidak benar</b>. Bukan hanya jembatannya yang mati — <b>kedua ujungnya</b>
 * mati. Yang tidur bukan relasi pinjaman→angsuran saja, melainkan seluruh gagasan "dokumen
 * pinjaman pegawai berdiri sendiri" di modul payroll.</p>
 *
 * <h2>Lalu bagaimana pinjaman pegawai SEBENARNYA diangsur dan dilunasi?</h2>
 * <p>Ini pertanyaan yang penting karena menyangkut uang riil yang dipinjamkan kepada pegawai.
 * Jawabannya: lewat rantai yang <b>sama sekali tidak menyentuh kelas ini</b>. Ada dua mekanisme
 * hidup yang terverifikasi, dan keduanya lengkap tanpa {@code PengajuanPeminjaman}:</p>
 * <ol>
 *   <li><b>Pinjaman/kasbon pegawai yang dipotong dari gaji</b> — inilah pengganti fungsional
 *       kelas ini. Dokumen pengajuannya adalah {@link PengajuanTransaksiPegawai} (tabel
 *       <code>payroll.pengajuan_transaksi_pegawai</code>), yang punya persis kolom-kolom yang
 *       dibutuhkan sebuah pinjaman dan <b>tidak dimiliki</b> kelas ini:
 *       <code>nilaiTransaksi</code> (pokok), <code>jumlahAngsur</code> (berapa kali dicicil),
 *       <code>tanggalJatuhTempo</code>, katalog jenis
 *       <code>JenisPengajuanTransaksiPegawai</code>, kolom tenant <code>satuanKerja</code>, dan
 *       relasi <code>daftarPengajuanTransfer</code> untuk pencairannya. Alurnya:
 *       <ul>
 *         <li>pengajuan disetujui di <code>PengajuanTransaksiPegawaiAction</code>;</li>
 *         <li>persetujuan itu memanggil <code>populateTransaksi()</code>, yang membuat
 *             <code>jumlahAngsur</code> baris {@link TransaksiPegawai} sekaligus — satu baris per
 *             angsuran, dibedakan oleh <code>ke</code> (angsuran ke-1, ke-2, ...);</li>
 *         <li>pemanggilan yang sama mendaftarkan dokumen ke
 *             <code>DaftarPengajuanTransfer.simpanPengajuanTransaksiPegawai(...)</code> — itulah
 *             jalur <b>pencairan dana</b>-nya (batch transfer bank);</li>
 *         <li>tiap baris angsuran dipotong dari slip gaji dan dijurnal
 *             <code>PostingTransaksiPegawaiAction</code>.</li>
 *       </ul>
 *       Jadi "pinjaman → angsuran" memang ada dan hidup di modul payroll; hanya saja kepala
 *       rantainya {@link PengajuanTransaksiPegawai}, bukan kelas ini.</li>
 *   <li><b>Pinjaman berbunga lewat koperasi</b> — untuk pegawai yang berstatus anggota koperasi,
 *       pinjaman diproses di modul koperasi: dokumen <code>TransaksiKoperasi</code> dengan baris
 *       <code>TransaksiKoperasiDetail</code>, dan jadwal angsurannya dihitung
 *       <code>ais.action.master.koperasi.helper.PerhitunganPinjamanUtil</code> (metode bunga
 *       flat/menurun/anuitas mengikuti <code>ProdukKoperasi.getMetodeBunga()</code>). Perlu
 *       ditegaskan: <code>PerhitunganPinjamanUtil</code> ada di paket <b>koperasi</b>, tidak pernah
 *       memanggil kelas ini, dan sama sekali tidak berkaitan dengan modul payroll.</li>
 * </ol>
 * <p>Kesimpulannya, tidak ada "proses manual" tersembunyi yang mengangsur baris kelas ini: tidak
 * ada barisnya sama sekali untuk diangsur. Bila suatu hari fitur ini dihidupkan, ia harus dibangun
 * dari nol — dan sebaiknya dipertimbangkan untuk <b>tidak</b> dibangun sama sekali, karena
 * {@link PengajuanTransaksiPegawai} sudah menutup kebutuhan yang sama secara lebih lengkap.</p>
 *
 * <h2>Struktur data — apa yang ada dan apa yang justru tidak ada</h2>
 * <p>Kolom yang dimiliki: identitas ({@link #getId()}, {@link #getKodeUnik()}), pegawai peminjam
 * ({@link #getPegawai()}, satu-satunya kolom <code>nullable = false</code>), nominal
 * ({@link #getNilai()}), rincian bebas berformat JSON ({@link #getFormula()}), periode
 * ({@link #getBulan()}/{@link #getTahun()}), keterangan, tiga pasang jejak alur (dibuat/disetujui/
 * ditolak beserta tanggalnya), bendera {@link #getAktif()}, cap posting
 * ({@link #getPostingHistory()}), alur nomor surat ({@link #getNomorSuratAlurPengadaan()}), dan
 * jangkar SOP ({@link #getDisposisiSop()}).</p>
 * <p>Yang <b>tidak</b> ada justru lebih informatif, dan menjelaskan mengapa rancangan ini tidak
 * pernah bisa jadi dokumen pinjaman yang utuh:</p>
 * <ul>
 *   <li><b>Tidak ada jumlah angsuran</b> dan <b>tidak ada tenor/jatuh tempo</b>. Tanpa keduanya
 *       tidak ada cara menurunkan jadwal cicilan dari dokumen ini.</li>
 *   <li><b>Tidak ada kolom sisa pokok/pelunasan</b>, tidak ada bunga, tidak ada relasi ke baris
 *       angsuran mana pun (relasinya satu arah dari {@link TransaksiPegawai}, dan itu pun mati).</li>
 *   <li><b>Tidak ada satu pun kolom tenant</b> — tidak ada <code>satuanKerja</code>,
 *       <code>sekolah</code>, maupun <code>yayasan</code>. Bandingkan dengan
 *       {@link PengajuanTransaksiPegawai} yang punya <code>satuanKerja</code>. Konsekuensinya
 *       dibahas di bawah.</li>
 *   <li><b>Tidak ada katalog jenis</b> — sehingga tidak ada pasangan akun debet/kredit yang bisa
 *       dipakai mesin posting. Kolom {@link #getPostingHistory()} ada, tetapi tidak ada satu pun
 *       kode yang mengisinya; <code>CommonAkunting.saveTransaksi(...)</code> tidak punya
 *       <i>overload</i> untuk kelas ini, dan <code>GrupTransaksi</code> tidak punya kolom referensi
 *       ke kelas ini. <b>Dokumen ini secara struktural tidak sanggup menghasilkan jurnal.</b></li>
 * </ul>
 *
 * <h2>Pewarisan: <code>PengajuanPeminjaman extends DataSop extends GeneralValueObject</code></h2>
 * <p>{@link DataSop} adalah kelas abstrak tipis yang hanya mewajibkan pasangan
 * {@link #getDisposisiSop()}/{@link #setDisposisiSop(DisposisiSop)} — kontrak agar mesin SOP bisa
 * memperlakukan dokumen apa pun secara seragam. Di atasnya,
 * {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}: ia POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun
 * properti induknya</b>. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId},
 * {@code keterangan}, {@code tanggal_dirubah}, dan {@code aktif} di kelas ini <b>bukan bug dan
 * bukan duplikasi ceroboh</b> — itu keharusan teknis agar kolom-kolom tersebut benar-benar
 * ada di tabel. Lihat {@link ais.database.model.GeneralValueObject} untuk penjelasan lengkap pola
 * ini.</p>
 * <p><b>Satu akibat halus yang perlu diketahui:</b> {@link #getKodeUnik()} memanggil
 * {@code getKode()} yang <b>diwarisi</b> dari {@code GeneralValueObject} dan sengaja
 * <b>tidak</b> dideklarasikan ulang di sini. Karena properti induk tidak dipetakan,
 * {@code kode} tidak punya kolom, tidak pernah dibaca dari basis data, dan tidak pernah ditulis
 * pemanggil mana pun — jadi {@code getKode()} pada instance kelas ini <b>selalu mengembalikan
 * {@code null}</b>. Rincian dampaknya ada di Javadoc {@link #getKodeUnik()}.</p>
 *
 * <h2>Pemetaan property-access dan getter yang menulis balik</h2>
 * <p>Seluruh anotasi menempel pada <b>getter</b>, bukan field; ditambah
 * {@code dynamicInsert = true} dan {@code dynamicUpdate = true} pada tingkat kelas. Artinya setiap
 * kali Hibernate menyusun INSERT/UPDATE atau memeriksa <i>dirty state</i>, ia <b>memanggil getter
 * ini</b> — dan getter di kelas ini banyak yang menghitung nilai lalu <b>menuliskannya balik</b>
 * ke field. Tulis-balik itu karenanya ikut tersimpan ke basis data. Getter yang berperilaku
 * demikian:</p>
 * <ul>
 *   <li>{@link #getBulan()}, {@link #getTahun()} — mengisi periode dari tanggal pembuatan bila
 *       masih kosong (idempoten setelah terisi);</li>
 *   <li>{@link #getKodeUnik()} — <b>selalu</b> menyusun ulang, mengabaikan nilai tersimpan;</li>
 *   <li>{@link #getNomorSuratAlurPengadaan()} — mengisi nilai bawaan
 *       {@code NomorSuratAlurPengadaan.PEMINJAMAN_PEGAWAI} bila kosong;</li>
 *   <li>{@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 *       {@link #getTanggalPembuatan()} — menurunkan nilai dari {@link DisposisiSop};</li>
 *   <li>{@link #getAktif()} — memadamkan bendera aktif berdasarkan keadaan SOP.</li>
 * </ul>
 * <p><b>Dua di antaranya destruktif</b>, dan pada dokumen yang menyangkut uang itu perlu
 * ditekankan: {@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} dapat
 * <b>menghapus persetujuan yang sudah sah</b> — cukup dengan <i>dibaca</i> — bila jejak SOP
 * langkah "setuju" hilang, atau bila kolom {@code ditolakOleh} terisi. Karena property-access +
 * {@code dynamicUpdate}, penghapusan itu ikut <i>ter-flush</i> ke basis data. Pola yang sama sudah
 * ditemukan pada dokumen keuangan lain di repositori ini (mis.
 * <code>ProsesTransitori.getDisetujuiOleh()</code>) dan di sana konsekuensinya nyata: dana bisa
 * tersangkut karena status persetujuan lenyap tanpa jejak. Di sini konsekuensinya <b>laten</b>
 * semata karena tabelnya kosong — bukan karena kodenya lebih aman.</p>
 * <p>Kabar baiknya: <b>tidak ada</b> getter di kelas ini yang menyentuh {@link #getNilai()}.
 * Nominal pinjaman hanya bisa berubah lewat {@link #setNilai(Double)} yang eksplisit; tidak ada
 * pembulatan diam-diam, tidak ada penghitungan ulang dari rumus, tidak ada penimpaan retroaktif
 * seperti pada <code>TransaksiPegawai.getNilai()</code> atau
 * <code>PembayaranItemGajiPegawai.getNilai()</code>. Ini adalah verifikasi <i>negatif</i> yang
 * menenangkan untuk pertanyaan "adakah getter destruktif yang menyentuh nominal pinjaman?" —
 * jawabannya <b>tidak ada</b>.</p>
 *
 * <h2>Cakupan tenant: tidak fail-open bersyarat, melainkan tidak ada sumbu tenant sama sekali</h2>
 * <p>Entity ini tidak punya kolom tenant apa pun. Untuk lapisan CRUD generik v2 hal itu berarti
 * <code>GenericCrudAutoEntityAdapter.scopeBindings()</code> memanggil
 * <code>addScope(metadata, result, "yayasan"|"sekolah"|"program"|"fakultas"|"jurusan"|
 * "satuanKerja", ...)</code> untuk properti yang <b>tidak ada</b> pada metadata Hibernate kelas
 * ini; <code>addScope()</code> menelan kegagalan itu diam-diam, sehingga peta restriksi yang
 * dihasilkan <b>kosong</b> dan tidak satu pun <code>Restrictions.eq(...)</code> ditambahkan ke
 * kriteria. Ketiadaan kolom dibaca sebagai "tidak perlu disaring", bukan "tolak" — pola sistemik
 * yang sudah tercatat di repositori ini. Perhatikan pula bahwa <code>pegawai</code> — satu-satunya
 * relasi yang bisa membatasi baris ke seseorang — <b>tidak ada</b> di daftar putih
 * <code>scopeBindings()</code>, sehingga tidak dipakai sebagai penyaring sekalipun ia ada.</p>
 * <p>Permukaan yang benar-benar menjangkau kelas ini adalah <i>admin model browser</i>:
 * <code>GenericCrudAutoDefinitionFactory.listAdministrativeModels()</code> mengenumerasi
 * <b>seluruh</b> entity ber-metadata Hibernate yang turunan {@code GeneralValueObject} — jadi
 * termasuk kelas ini, terlepas dari catatan "default disabled" pada manifes
 * <code>webapp/WEB-INF/generic-crud/manifests/general_value_object_inventory.csv</code>.
 * Peringanannya: karena kelas ini tidak punya Action sumber,
 * <code>buildForClass()</code> menetapkan {@code mutable = false} sehingga definisi yang terbentuk
 * <b>READ_ONLY</b> — tambah/ubah/hapus dimatikan. Yang tersisa adalah <b>pembacaan dan ekspor</b>
 * seluruh baris lintas tenant tanpa penyaring apa pun. Untuk saat ini tidak berdampak karena
 * tabelnya kosong; ia menjadi masalah nyata pada hari fitur ini dihidupkan tanpa lebih dulu
 * menambahkan kolom tenant.</p>
 *
 * <h2>Siapa yang bisa menyetujui dan mencairkan? — verifikasi gerbang</h2>
 * <p>Untuk kelas ini jawabannya <b>tidak ada siapa pun</b>, karena tidak ada layar, tidak ada
 * REST, dan tidak ada tombol. Tidak ada permukaan tulis, jadi tidak ada gerbang yang bisa
 * ditembus; pola <i>broken access control</i> yang dikenal di modul uang lain — bypass persetujuan
 * lewat parameter URL, <code>bolehAksi()</code> yang fail-open pada peran null, tombol
 * "Realisasikan" tanpa pemeriksaan hak — <b>tidak berlaku</b> di sini karena tidak ada kodenya.
 * Ini verifikasi negatif, bukan bukti bahwa rancangannya aman.</p>
 * <p>Sebagai pembanding yang relevan untuk mekanisme yang benar-benar dipakai: gerbang persetujuan
 * di jalur hidup ({@code PengajuanTransaksiPegawaiAction}, kotak centang "Setujui") justru
 * termasuk yang <b>lebih ketat</b> daripada rata-rata modul uang di repositori ini — kotak centang
 * hanya dirender bila pengguna punya {@code Pegawai}, dokumen <b>bukan</b> milik dirinya sendiri
 * (larangan menyetujui pengajuan sendiri ditegakkan), dan pengguna adalah atasan langsung
 * ke-1/ke-2/ke-3 pegawai bersangkutan atau memiliki pegawai itu sebagai bawahan; selain itu hanya
 * label baca-saja yang ditampilkan. Catatan: cabang ini hanya berlaku ketika dokumen
 * <b>tidak</b> memakai SOP ({@code getDisposisiSop() == null}); bila memakai SOP, otorisasi
 * berpindah ke mesin disposisi.</p>
 *
 * <h2>Integritas: bisakah dicairkan atau diposting dua kali?</h2>
 * <p>Tidak — dan bukan karena ada penjaga, melainkan karena tidak ada satu pun jalur pencairan
 * maupun posting yang menyentuh kelas ini. Kolom {@link #getPostingHistory()} tidak pernah diisi,
 * dan tidak ada <code>Posting...Action</code> maupun tombol pembatalan posting untuk dokumen ini.
 * Yang perlu dicatat sebagai <b>risiko laten</b> bila fitur ini dihidupkan:</p>
 * <ul>
 *   <li>tidak ada kolom "sudah dicairkan"/"sudah lunas" sama sekali, sehingga idempotensi
 *       pencairan harus dirancang dari nol;</li>
 *   <li>{@link #getKodeUnik()} — satu-satunya kandidat kunci idempotensi — <b>cacat</b>: lihat
 *       Javadoc method tersebut; nilainya bahkan tidak stabil antara saat INSERT dan pembacaan
 *       berikutnya;</li>
 *   <li>tidak ada penjaga yang mencegah dua dokumen pinjaman aktif untuk pegawai yang sama pada
 *       periode yang sama.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 *   <li><b>Identitas &amp; kunci</b> — {@link #getId()}, {@link #getKodeUnik()},
 *       {@link #toString()}.</li>
 *   <li><b>Jejak audit ringan</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Isi dokumen</b> — {@link #getKeterangan()}, {@link #getNilai()},
 *       {@link #getFormula()}, {@link #getBulan()}, {@link #getTahun()}.</li>
 *   <li><b>Alur persetujuan/penolakan</b> — {@link #getDibuatOleh()},
 *       {@link #getTanggalPembuatan()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPersetujuan()}, {@link #getDitolakOleh()},
 *       {@link #getTanggalDitolak()}, {@link #getDisposisiSop()}, {@link #getAktif()}.</li>
 *   <li><b>Relasi lain</b> — {@link #getPegawai()}, {@link #getNomorSuratAlurPengadaan()},
 *       {@link #getPostingHistory()}.</li>
 * </ol>
 *
 * @see PengajuanTransaksiPegawai
 * @see TransaksiPegawai
 * @see DataSop
 * @see DisposisiSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "pengajuan_peminjaman")
public class PengajuanPeminjaman extends DataSop {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya <b>identik</b> dengan {@code serialVersionUID} milik
	 * {@link PengajuanTransaksiPegawai} — petunjuk kuat bahwa kelas ini lahir sebagai salinan
	 * berkas tersebut lalu dipangkas. Kesamaan itu tidak berbahaya (kedua kelas berbeda nama,
	 * jadi tidak pernah saling ditukar saat deserialisasi), tetapi berguna sebagai bukti
	 * arkeologis asal-usul kelas ini.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, IDENTITY. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> bila {@code olehId} {@code null} atau hanya
	 * berisi spasi, method langsung {@code return} tanpa mengubah apa pun dan tanpa melempar.
	 * Jejak audit lama karenanya dipertahankan, bukan ditimpa nilai hampa — tetapi juga berarti
	 * jejak ini <b>tidak bisa dikosongkan</b> lewat setter.</p>
	 *
	 * @param olehId id pengguna; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks dokumen — mengembalikan {@link #getKeterangan()} apa adanya.
	 *
	 * <p>Dipakai komponen ZK/laporan sebagai label baris. Perhatikan dua hal: (1) method ini
	 * membaca <b>field</b> {@code keterangan} langsung, bukan getter-nya; (2) karena
	 * {@code keterangan} boleh {@code null}, method ini <b>bisa mengembalikan {@code null}</b> —
	 * berbeda dari {@code GeneralValueObject.toString()} yang selalu menghasilkan {@link String}.
	 * Pemanggil yang merangkai hasilnya ke dalam teks akan melihat "null".</p>
	 *
	 * @return keterangan dokumen, mungkin {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan diam-diam</b>.</p>
	 *
	 * @param oleh nama pengguna; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} yang menyerahkan pengisian stempel audit kepada
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menjalankan UPDATE.
	 *
	 * <p>Interceptor itulah yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif. Method ini
	 * {@code protected} dan tidak boleh dipanggil manual; ia juga <b>tidak</b> berjalan pada INSERT
	 * (hanya UPDATE), sehingga baris yang baru dibuat mengandalkan nilai awal field
	 * {@code tanggal_dirubah}.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama dengan
	 * method ini — gaya salin-tempel yang dipakai seragam di seluruh entity repositori ini; jangan
	 * dirapikan tanpa alasan, karena skrip penyisip audit mencocokkan pola tersebut. Nilai awalnya
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu server saat objek dibuat), sehingga baris baru
	 * selalu punya stempel meski interceptor belum pernah jalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>menerima {@code null}</b>. Menyetelnya {@code null} punya efek samping tidak langsung:
	 * {@link #getTanggalPembuatan()} memakai nilai ini sebagai cadangan, sehingga
	 * {@link #getBulan()}/{@link #getTahun()} bisa melempar {@link NullPointerException} — lihat
	 * Javadoc kedua method itu.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} kecuali sengaja disetel demikian
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan bebas dokumen; juga menjadi hasil {@link #toString()}. */
	private String keterangan;

	/** Bulan periode dokumen (1-12), diturunkan bila kosong. Lihat {@link #getBulan()}. */
	private Integer bulan;

	/** Tahun periode dokumen, diturunkan bila kosong. Lihat {@link #getTahun()}. */
	private Integer tahun;

	/** Nominal pinjaman yang diajukan. Lihat {@link #getNilai()}. */
	private Double nilai;

	/** Rincian dokumen dalam bentuk teks JSON array. Lihat {@link #getFormula()}. */
	private String formula;

	/** Tanggal dokumen dibuat/diajukan; diturunkan dari SOP bila ada. Lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;

	/** Tanggal dokumen disetujui; diturunkan dari SOP bila ada. Lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;

	/** Pengguna pembuat dokumen; diturunkan dari SOP bila ada. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;

	/** Pengguna penyetuju dokumen; diturunkan dari SOP bila ada. Lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;

	/**
	 * Alur penomoran surat untuk dokumen ini; secara efektif selalu
	 * {@code NomorSuratAlurPengadaan.PEMINJAMAN_PEGAWAI}. Lihat {@link #getNomorSuratAlurPengadaan()}.
	 */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;

	/** Pegawai peminjam — satu-satunya relasi wajib ({@code nullable = false}). */
	private Pegawai pegawai;

	/** Jangkar alur SOP dokumen ini; sumber turunan bagi banyak getter di kelas ini. */
	private DisposisiSop disposisiSop;

	/**
	 * Cap posting jurnal. <b>Tidak pernah diisi</b> — tidak ada mesin posting untuk kelas ini.
	 * Lihat {@link #getPostingHistory()}.
	 */
	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun. Perlu dicatat bahwa konstruktor ini
	 * <b>tidak pernah dipanggil dari kode aplikasi</b> — tidak ada satu pun {@code new
	 * PengajuanPeminjaman()} di seluruh repositori (lihat catatan "entity tidur" pada Javadoc
	 * kelas); satu-satunya pemanggilnya secara teoretis adalah Hibernate saat memuat baris, dan
	 * tabelnya kosong.</p>
	 */
	public PengajuanPeminjaman() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan ditandai
	 * {@code insertable = false}, sehingga nilainya baru terisi <b>setelah</b> INSERT dijalankan.
	 * Hal itu penting untuk memahami cacat {@link #getKodeUnik()}.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya untuk Hibernate dan kode pemuatan ulang; jangan dipakai kode
	 * bisnis.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas dokumen — misalnya maksud/peruntukan pinjaman.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas dokumen. Tanpa validasi; {@code null} diterima apa adanya.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan bulan periode dokumen (1 = Januari ... 12 = Desember).
	 *
	 * <p><b>Menulis balik.</b> Bila {@code bulan} masih {@code null}, method menurunkannya dari
	 * {@link #getTanggalPembuatan()} lalu <b>menyimpannya ke field</b>. Karena kelas ini dipetakan
	 * property-access dengan {@code dynamicUpdate}, nilai turunan itu ikut tersimpan ke basis data
	 * pada flush berikutnya. Setelah terisi, method bersifat idempoten — nilai tersimpan tidak
	 * pernah dihitung ulang, jadi memindahkan tanggal pembuatan <b>tidak</b> menggeser periode
	 * dokumen lama.</p>
	 *
	 * <p><b>Kasus tepi.</b> {@code Calendar.setTime(null)} melempar
	 * {@link NullPointerException}. Itu bisa terjadi hanya bila {@link #getTanggalPembuatan()}
	 * mengembalikan {@code null}, yang pada gilirannya menuntut <i>kedua</i> {@code tanggalPembuatan}
	 * dan {@link #getTanggal_dirubah()} bernilai {@code null} — dan {@code tanggal_dirubah}
	 * diinisialisasi ke waktu server saat objek dibuat, jadi hanya
	 * {@link #setTanggal_dirubah(Date)} yang eksplisit dengan {@code null} yang dapat memicunya.</p>
	 *
	 * <p>Perhatikan {@code Calendar.MONTH} berbasis 0, karena itu ditambah 1 agar Januari = 1.
	 * Kalender diambil lewat {@code WaktuUtil.getCalendar()} sehingga memakai zona waktu server
	 * yang seragam se-aplikasi, bukan {@code Calendar.getInstance()} bawaan.</p>
	 *
	 * @return bulan periode 1-12
	 */
	public Integer getBulan() {
		if (bulan == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalPembuatan());
			bulan = calendar.get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode dokumen secara eksplisit.
	 *
	 * <p>Tanpa validasi rentang: nilai di luar 1-12 (bahkan negatif) diterima apa adanya. Menyetel
	 * {@code null} mengembalikan perilaku turunan otomatis pada pembacaan berikutnya.</p>
	 *
	 * @param bulan bulan periode; boleh {@code null}
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan tahun periode dokumen.
	 *
	 * <p>Perilakunya persis paralel dengan {@link #getBulan()}: menulis balik saat masih kosong,
	 * idempoten setelahnya, dan berbagi kasus tepi {@link NullPointerException} yang sama.</p>
	 *
	 * @return tahun periode (mis. 2026)
	 */
	public Integer getTahun() {
		if (tahun == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalPembuatan());
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode dokumen secara eksplisit. Tanpa validasi.
	 *
	 * @param tahun tahun periode; boleh {@code null}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan nominal pinjaman yang diajukan, dengan {@code null} disubstitusi menjadi
	 * {@code 0.0}.
	 *
	 * <p><b>Substitusi, bukan tulis-balik.</b> Ini perbedaan penting dan disengaja: method
	 * mengembalikan {@code 0.0} untuk pemanggil tetapi <b>tidak</b> menugaskan {@code 0.0} ke
	 * field, sehingga kolom di basis data tetap {@code NULL} dan tidak ada informasi yang hilang.
	 * Bandingkan dengan beberapa getter nominal lain di modul penggajian yang menormalkan nilai
	 * secara permanen dan retroaktif — <b>getter ini tidak destruktif</b>.</p>
	 *
	 * <p>Tidak ada pembulatan, tidak ada pembatasan tanda: nominal negatif diterima dan
	 * dikembalikan apa adanya. Tidak ada plafon/pagu pinjaman yang diperiksa di mana pun pada
	 * kelas ini.</p>
	 *
	 * @return nominal pinjaman; {@code 0.0} bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nominal pinjaman yang diajukan.
	 *
	 * <p>Satu-satunya jalan nominal berubah — tidak ada getter di kelas ini yang menyentuhnya.
	 * Tanpa validasi: {@code null}, nol, maupun nilai negatif diterima.</p>
	 *
	 * @param nilai nominal pinjaman; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Menyetel pengguna pembuat dokumen secara eksplisit.
	 *
	 * <p>Perlu diketahui bahwa nilai yang disetel di sini akan <b>ditimpa</b> oleh
	 * {@link #getDibuatOleh()} bila dokumen punya {@link DisposisiSop} dengan langkah awal yang
	 * lengkap — SOP selalu menang atas nilai manual.</p>
	 *
	 * @param dibuatOleh pengguna pembuat; boleh {@code null}
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen, dengan SOP sebagai sumber kebenaran.
	 *
	 * <p>Urutan kerjanya:</p>
	 * <ol>
	 *   <li>{@code check(dibuatOleh)} meresolusi proxy lazy yang mungkin sudah lepas dari sesi —
	 *       lihat {@link ais.database.model.GeneralValueObject#check(Object)};</li>
	 *   <li>bila {@link #getDisposisiSop()} punya langkah awal ({@code getDisposisiStart()}) yang
	 *       menyimpan pengaju ({@code getDiajukanOleh()}), nilai itu <b>menimpa</b> field
	 *       {@code dibuatOleh} — jadi jejak SOP dianggap lebih otoritatif daripada kolom manual;</li>
	 *   <li>seluruh langkah 2 dibungkus {@code try/catch} yang <b>sengaja menelan</b>
	 *       {@code LazyInitializationException} (dan exception lain), sekadar mencatatnya ke
	 *       {@code ErrorAuditUtil}. Alasannya ada pada komentar di dalam kode: {@code disposisiSop}
	 *       bisa berupa instance kanonik bersama yang proxy-nya terikat ke {@link org.hibernate.Session}
	 *       lain yang sudah tertutup, dan getter entity tidak boleh membuat pemanggilnya gagal.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field, sehingga nilai turunan dari SOP ikut
	 * tersimpan ke kolom {@code dibuat_oleh} pada flush berikutnya. <b>Tidak destruktif</b> —
	 * berbeda dari {@link #getDisetujuiOleh()}, method ini tidak pernah menugaskan {@code null};
	 * bila SOP tidak menyediakan data, nilai lama dipertahankan.</p>
	 *
	 * <p><b>Kegagalan yang tersembunyi:</b> karena exception ditelan, dokumen yang seharusnya
	 * mengambil pembuat dari SOP bisa diam-diam tetap memakai nilai lama tanpa tanda apa pun di
	 * layar — satu-satunya jejaknya ada di log {@code ErrorAuditUtil}.</p>
	 *
	 * @return pengguna pembuat dokumen, atau {@code null} bila tidak diketahui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PengajuanPeminjaman.java:getDibuatOleh-lazy");
		}

		return dibuatOleh;
	}

	/**
	 * Menyetel pengguna penyetuju dokumen secara eksplisit.
	 *
	 * <p>Sama seperti {@link #setDibuatOleh(Tbmuser)}, nilai ini dapat ditimpa —
	 * bahkan <b>dihapus</b> — oleh {@link #getDisetujuiOleh()}. Menyetel penyetuju lewat setter ini
	 * saja karenanya <b>bukan cara yang andal</b> untuk menyatakan sebuah pinjaman telah disetujui
	 * bila dokumen juga terhubung ke SOP.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju; boleh {@code null}
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui pinjaman — <b>getter destruktif</b>.
	 *
	 * <p>Sumber kebenarannya SOP, dengan tiga aturan berurutan:</p>
	 * <ol>
	 *   <li>bila {@link DisposisiSop} punya langkah "setuju" ({@code getDisposisiSetuju()}) berisi
	 *       pengaju, nilai itu <b>menimpa</b> field;</li>
	 *   <li>bila dokumen punya {@link DisposisiSop} <b>tetapi</b> langkah "setuju"-nya belum ada
	 *       atau belum berpengaju, field <b>ditugasi {@code null}</b> — persetujuan yang tercatat
	 *       sebelumnya dihapus;</li>
	 *   <li>terlepas dari SOP, bila {@link #getDitolakOleh()} terisi, field <b>ditugasi
	 *       {@code null}</b> — dokumen yang pernah ditolak tidak boleh tampak disetujui.</li>
	 * </ol>
	 *
	 * <p><b>Mengapa ini berbahaya (secara laten).</b> Aturan (2) dan (3) menugaskan {@code null}
	 * ke field yang dipetakan property-access pada kelas ber-{@code dynamicUpdate}. Artinya
	 * <i>membaca</i> dokumen ini di dalam sesi Hibernate yang aktif dapat <b>menghapus persetujuan
	 * yang sah dari basis data</b>, tanpa aksi pengguna, tanpa jejak selain revisi Envers. Pada
	 * dokumen yang menyangkut pencairan uang, pola ini sudah terbukti merugikan di modul lain
	 * repositori ini. Di sini dampaknya laten semata karena tidak ada baris dan tidak ada layar —
	 * bukan karena kodenya lebih aman. Siapa pun yang menghidupkan fitur ini <b>harus</b> lebih
	 * dulu memindahkan logika turunan ini keluar dari getter.</p>
	 *
	 * <p>Aturan (3) dievaluasi <b>di luar</b> blok {@code try/catch}, jadi ia tetap berlaku
	 * sekalipun resolusi SOP gagal. Blok {@code try/catch}-nya sendiri menelan
	 * {@code LazyInitializationException} dengan alasan yang sama seperti pada
	 * {@link #getDibuatOleh()}.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui, persetujuan SOP tidak
	 *         ditemukan, atau dokumen sudah ditolak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PengajuanPeminjaman.java:getDisetujuiOleh-lazy");
		}

		if (getDitolakOleh() != null) {
			disetujuiOleh = null;
		}

		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal persetujuan secara eksplisit.
	 *
	 * <p>Seperti {@link #setDisetujuiOleh(Tbmuser)}, nilainya dapat ditimpa atau dikosongkan oleh
	 * {@link #getTanggalPersetujuan()}.</p>
	 *
	 * @param tanggalPersetujuan tanggal persetujuan; boleh {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan pinjaman — <b>getter destruktif</b>, cerminan persis
	 * {@link #getDisetujuiOleh()}.
	 *
	 * <p>Aturannya identik: diambil dari {@code disposisiSop.getDisposisiSetuju().getWaktu()} bila
	 * tersedia; ditugasi {@code null} bila dokumen ber-SOP tetapi langkah "setuju"-nya belum ada
	 * atau belum berpengaju; dan ditugasi {@code null} bila {@link #getDitolakOleh()} terisi.
	 * Karena kedua getter dievaluasi terpisah oleh Hibernate saat flush, keduanya konsisten satu
	 * sama lain — pasangan "siapa yang menyetujui" dan "kapan" selalu hidup atau mati bersama.</p>
	 *
	 * <p>Berbeda dari getter turunan lain di kelas ini, method ini <b>tidak</b> memanggil
	 * {@code check(...)} lebih dulu karena {@link Date} bukan entity, jadi tidak ada proxy yang
	 * perlu diresolusi.</p>
	 *
	 * @return tanggal persetujuan, atau {@code null} bila belum/tidak lagi disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PengajuanPeminjaman.java:getTanggalPersetujuan-lazy");
		}

		if (getDitolakOleh() != null) {
			tanggalPersetujuan = null;
		}

		return tanggalPersetujuan;
	}

	/**
	 * Menyetel tanggal pembuatan/pengajuan dokumen secara eksplisit.
	 *
	 * @param tanggalPembuatan tanggal pembuatan; boleh {@code null}
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal dokumen dibuat/diajukan.
	 *
	 * <p>Dua lapis:</p>
	 * <ol>
	 *   <li><b>Tulis balik dari SOP</b> — bila langkah awal SOP ({@code getDisposisiStart()})
	 *       menyimpan pengaju, waktunya menimpa field {@code tanggalPembuatan} dan ikut tersimpan
	 *       pada flush. Sama seperti {@link #getDibuatOleh()}, <b>tidak destruktif</b>: tidak ada
	 *       cabang yang menugaskan {@code null}.</li>
	 *   <li><b>Substitusi saat kosong</b> — nilai kembalian (bukan field) jatuh ke
	 *       {@link #getTanggal_dirubah()} bila {@code tanggalPembuatan} masih {@code null}. Ini
	 *       substitusi murni: field tetap {@code null}, kolom di basis data tetap kosong. Dengan
	 *       demikian method praktis tidak pernah mengembalikan {@code null}, karena
	 *       {@code tanggal_dirubah} diinisialisasi ke waktu server saat objek dibuat.</li>
	 * </ol>
	 *
	 * <p><b>Dipanggil dari mana:</b> selain Hibernate, method ini dipakai {@link #getBulan()} dan
	 * {@link #getTahun()} sebagai basis penurunan periode — sehingga substitusi di atas menentukan
	 * periode dokumen yang belum pernah menyimpan tanggal pengajuan.</p>
	 *
	 * @return tanggal pembuatan dokumen, atau stempel perubahan terakhir sebagai penggantinya
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PengajuanPeminjaman.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? getTanggal_dirubah() : tanggalPembuatan;
	}

	/** Kunci unik turunan; selalu dihitung ulang. Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Tanggal dokumen ditolak. Lihat {@link #getTanggalDitolak()}. */
	private Date tanggalDitolak;

	/** Pengguna yang menolak dokumen. Lihat {@link #getDitolakOleh()}. */
	private Tbmuser ditolakOleh;

	/** Bendera aktif; dapat dipadamkan oleh keadaan SOP. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Mengembalikan kunci unik dokumen, <b>selalu disusun ulang</b> dari
	 * {@code getKode() + "" + ("_" + id)} atau {@code getKode() + "" + ("_" + disposisiSop.getId())}
	 * bila dokumen punya SOP.
	 *
	 * <p>Ini adalah kolom ber-{@code unique = true}, jadi ia berperan sebagai kunci idempotensi —
	 * pola yang sama dipakai dokumen keuangan lain di repositori ini. Namun implementasi di sini
	 * <b>cacat pada tiga titik sekaligus</b>, dan seluruhnya perlu diperbaiki sebelum entity ini
	 * dihidupkan:</p>
	 * <ol>
	 *   <li><b>{@code getKode()} selalu {@code null}.</b> Method itu diwarisi dari
	 *       {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
	 *       {@code @MappedSuperclass} — properti {@code kode} karenanya tidak punya kolom, tidak
	 *       pernah dimuat, dan tidak pernah disetel siapa pun untuk kelas ini. Prefiks kunci
	 *       selalu berupa teks harfiah <code>"null"</code>, bukan kode dokumen. Bandingkan dengan
	 *       {@link PengajuanTransaksiPegawai} yang mendeklarasikan ulang {@code kode}-nya sendiri
	 *       sehingga benar-benar terpetakan.</li>
	 *   <li><b>Nilainya berubah antara INSERT dan pembacaan berikutnya.</b> {@link #getId()}
	 *       dibangkitkan {@code IDENTITY}, jadi saat Hibernate menyusun pernyataan INSERT id masih
	 *       {@code null}. Untuk dokumen tanpa SOP, nilai yang benar-benar ditulis ke kolom adalah
	 *       <code>"null_null"</code>; barulah pembacaan berikutnya menghasilkan
	 *       <code>"null_&lt;id&gt;"</code>. Karena kolomnya {@code unique}, <b>baris kedua tanpa
	 *       SOP akan ditolak basis data</b> — dokumen tanpa SOP praktis hanya bisa dibuat satu
	 *       kali seumur instalasi.</li>
	 *   <li><b>Kunci tidak membedakan dokumen yang berbagi satu disposisi.</b> Bila dokumen punya
	 *       SOP, kunci disusun dari id {@link DisposisiSop}, bukan id dokumen. Dua
	 *       {@code PengajuanPeminjaman} yang menunjuk disposisi yang sama akan bertabrakan pada
	 *       indeks unik.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field pada setiap pemanggilan, sehingga nilai
	 * apa pun yang pernah disetel {@link #setKodeUnik(String)} tidak pernah bertahan.</p>
	 *
	 * @return kunci unik dokumen; secara praktis berbentuk <code>"null_&lt;id&gt;"</code> atau
	 *         <code>"null_&lt;idDisposisi&gt;"</code>
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik dokumen.
	 *
	 * <p>Praktis <b>tanpa efek</b>: {@link #getKodeUnik()} menghitung ulang nilainya pada setiap
	 * pembacaan, termasuk pembacaan yang dilakukan Hibernate saat flush. Setter ini hanya ada agar
	 * Hibernate dapat mengisi field saat memuat baris dari basis data.</p>
	 *
	 * @param kodeUnik kunci unik; akan ditimpa pada pembacaan berikutnya
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan jangkar alur SOP dokumen ini.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()}, yang memungkinkan mesin
	 * SOP memperlakukan dokumen apa pun secara seragam. {@code check(...)} meresolusi proxy lazy
	 * yang mungkin sudah lepas dari sesi.</p>
	 *
	 * <p>Method ini adalah <b>sumber tunggal</b> bagi seluruh logika turunan di kelas ini
	 * ({@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
	 * {@link #getTanggalPembuatan()}, {@link #getKodeUnik()}, {@link #getAktif()}) — sehingga
	 * satu pembacaan dokumen dapat memanggilnya berkali-kali. Tidak ada memoisasi selain cache
	 * internal {@code check(...)}.</p>
	 *
	 * <p>Perlu dicatat: mesin SOP diikat ke dokumen lewat implementasi {@code FormSop.ambil()} di
	 * kelas <i>Action</i> masing-masing. Karena kelas ini tidak punya Action, dokumen ini
	 * <b>tidak pernah benar-benar masuk ke mesin SOP</b> — seluruh logika turunan di atas tidak
	 * pernah dieksekusi dengan data nyata.</p>
	 *
	 * @return disposisi SOP dokumen, atau {@code null} bila dokumen tidak memakai alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel jangkar alur SOP dokumen ini — <b>setter dengan dua lapis penjaga</b>.
	 *
	 * <p>Lapis pertama (penjaga awal pada baris deklarasi): bila {@code disposisiSop} {@code null}
	 * atau belum punya id, method langsung {@code return} tanpa mengubah apa pun. Maksudnya
	 * mencegah disposisi yang belum tersimpan menggantikan disposisi yang sudah sah.</p>
	 *
	 * <p>Lapis kedua adalah ekspresi ternary di badan method. Karena lapis pertama sudah menjamin
	 * {@code disposisiSop != null && disposisiSop.getId() != null}, kondisi
	 * {@code (disposisiSop == null || disposisiSop.getId() == null)} pada ternary itu
	 * <b>selalu {@code false}</b>, sehingga ekspresi selalu bermuara pada
	 * {@code this.disposisiSop = disposisiSop}. Dengan kata lain lapis kedua adalah
	 * <b>sisa kode yang tidak pernah berpengaruh</b> — peninggalan versi sebelum penjaga awal
	 * ditambahkan. Dibiarkan apa adanya karena menghapusnya adalah perubahan logika.</p>
	 *
	 * <p><b>Konsekuensi:</b> disposisi <b>tidak dapat dilepas</b> lewat setter ini — memanggilnya
	 * dengan {@code null} tidak melakukan apa-apa. Melepas SOP dari dokumen hanya mungkin lewat
	 * SQL langsung.</p>
	 *
	 * @param disposisiSop disposisi SOP baru; {@code null} atau yang belum tersimpan diabaikan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan alur penomoran surat untuk dokumen ini, dengan nilai bawaan
	 * {@code NomorSuratAlurPengadaan.PEMINJAMAN_PEGAWAI} ("Pinjaman Pegawai", kode surat
	 * <code>015</code>).
	 *
	 * <p><b>Menulis balik.</b> Bila field masih {@code null}, method mengisinya dengan konstanta
	 * statis tersebut — sehingga kolom FK <code>nomor_surat_alur_pengadaan</code> selalu berakhir
	 * menunjuk baris katalog itu. Bila sudah terisi, hanya diresolusi lewat {@code check(...)}.</p>
	 *
	 * <p><b>Hal non-obvious yang penting:</b> {@code NomorSuratAlurPengadaan.PEMINJAMAN_PEGAWAI}
	 * adalah field <b>{@code static}</b> yang disemai sekali saat inisialisasi data
	 * ({@code session.createCriteria(...).eq("nama", PINJAMAN_PEGAWAI)}, dibuat bila belum ada).
	 * Ia karenanya <b>global se-JVM dan lintas tenant</b> — semua instalasi yang berbagi satu JVM
	 * memakai baris katalog yang sama. Selain itu, referensi objek yang di-<i>cache</i> di field
	 * statis itu bisa berasal dari {@link org.hibernate.Session} yang sudah lama tertutup;
	 * {@code check(...)} pada cabang <i>else</i> yang menanganinya, tetapi cabang bawaan (cabang
	 * <i>then</i>) <b>tidak</b> melewati {@code check(...)} — jadi objek statis itu diserahkan apa
	 * adanya ke pemanggil.</p>
	 *
	 * <p>Konstanta ini adalah <b>satu-satunya pemakai</b> {@code PEMINJAMAN_PEGAWAI} di seluruh
	 * repositori. Karena kelas ini tidak pernah dipakai, alur nomor surat "Pinjaman Pegawai" tetap
	 * disemai ke tabel katalog pada setiap instalasi tetapi tidak pernah menerbitkan satu nomor
	 * surat pun.</p>
	 *
	 * @return alur penomoran surat; praktis selalu baris katalog "Pinjaman Pegawai"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMINJAMAN_PEGAWAI;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Menyetel alur penomoran surat dokumen ini.
	 *
	 * <p>Menyetel {@code null} tidak menghapus nilai secara permanen: pembacaan berikutnya akan
	 * mengisinya kembali dengan konstanta bawaan.</p>
	 *
	 * @param nomorSuratAlurPengadaan alur penomoran surat baru; boleh {@code null}
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/**
	 * Mengembalikan pegawai peminjam — pemilik dokumen ini.
	 *
	 * <p>Satu-satunya relasi yang dideklarasikan {@code nullable = false}, jadi dokumen tanpa
	 * pegawai tidak bisa disimpan. {@code check(...)} meresolusi proxy lazy yang mungkin sudah
	 * lepas dari sesi; tidak ada logika turunan lain di sini.</p>
	 *
	 * <p><b>Catatan cakupan:</b> relasi ini adalah satu-satunya dimensi yang secara teoretis dapat
	 * membatasi baris ke seorang pengguna, tetapi properti {@code pegawai} <b>tidak</b> termasuk
	 * daftar putih {@code GenericCrudAutoEntityAdapter.scopeBindings()}, sehingga tidak pernah
	 * dipakai sebagai penyaring pada lapisan CRUD generik.</p>
	 *
	 * @return pegawai peminjam
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai peminjam. Tanpa validasi; {@code null} diterima meski kolomnya
	 * {@code nullable = false} (kegagalannya baru muncul saat flush).
	 *
	 * @param pegawai pegawai peminjam
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan pengguna yang menolak dokumen ini.
	 *
	 * <p>Getter sederhana — hanya meresolusi proxy lazy, tanpa turunan dari SOP. Namun nilainya
	 * punya <b>jangkauan luas</b>: keberadaannya memaksa {@link #getDisetujuiOleh()} dan
	 * {@link #getTanggalPersetujuan()} mengembalikan (dan menyimpan) {@code null}. Berbeda dari
	 * pasangan setuju yang selalu diturunkan ulang dari SOP, penolakan <b>hanya</b> bisa dicatat
	 * lewat {@link #setDitolakOleh(Tbmuser)}; mesin SOP tidak pernah mengisinya sendiri.</p>
	 *
	 * @return pengguna penolak, atau {@code null} bila dokumen belum ditolak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditolak_oleh", nullable = true)
	public Tbmuser getDitolakOleh() {
		ditolakOleh = check(ditolakOleh);
		return ditolakOleh;
	}

	/**
	 * Menyetel pengguna yang menolak dokumen ini.
	 *
	 * <p>Tanpa validasi dan <b>tanpa gerbang hak apa pun di tingkat model</b> — juga tanpa
	 * pemeriksaan bahwa dokumen belum disetujui, atau bahwa penolak bukan pemohon sendiri. Semua
	 * pemeriksaan semacam itu seharusnya berada di lapisan Action, yang untuk kelas ini
	 * tidak ada.</p>
	 *
	 * @param ditolakOleh pengguna penolak; boleh {@code null} untuk membatalkan penolakan
	 */
	public void setDitolakOleh(Tbmuser ditolakOleh) {
		this.ditolakOleh = ditolakOleh;
	}

	/**
	 * Mengembalikan tanggal dokumen ditolak. Getter murni tanpa turunan maupun efek samping.
	 *
	 * <p>Perhatikan asimetri dengan {@link #getTanggalPersetujuan()}: tanggal persetujuan
	 * diturunkan otomatis dari SOP dan dapat dihapus sendiri, sedangkan tanggal penolakan
	 * sepenuhnya manual dan tidak pernah disentuh logika lain. Akibatnya kombinasi
	 * "{@code ditolakOleh} terisi tetapi {@code tanggalDitolak} kosong" mungkin terjadi tanpa ada
	 * yang mencegah.</p>
	 *
	 * @return tanggal penolakan, atau {@code null} bila belum ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditolak")
	public Date getTanggalDitolak() {
		return tanggalDitolak;
	}

	/**
	 * Menyetel tanggal dokumen ditolak. Tanpa validasi.
	 *
	 * @param tanggalDitolak tanggal penolakan; boleh {@code null}
	 */
	public void setTanggalDitolak(Date tanggalDitolak) {
		this.tanggalDitolak = tanggalDitolak;
	}

	/**
	 * Mengembalikan bendera aktif dokumen — <b>getter yang menulis balik dan hanya bisa
	 * memadamkan</b>.
	 *
	 * <p>Alurnya:</p>
	 * <ol>
	 *   <li>{@code disposisiSop = getDisposisiSop()} — memanggil getter, sehingga proxy
	 *       teresolusi sekaligus field disegarkan;</li>
	 *   <li>bila disposisi ada dan {@code disposisiSop.getAktif()} bernilai {@code false},
	 *       field {@code aktif} ditugasi {@code false};</li>
	 *   <li>bila disposisi ada dan langkah akhirnya ({@code getDisposisiEnd()}) berada pada
	 *       {@code AlurSop} yang ditandai {@code getPenolakanAdaDiSini()}, field {@code aktif}
	 *       juga ditugasi {@code false} — dokumen yang berhenti di simpul penolakan dianggap
	 *       tidak aktif;</li>
	 *   <li>nilai kembalian mensubstitusi {@code null} menjadi {@code true} — dokumen yang belum
	 *       pernah menyetel bendera dianggap aktif.</li>
	 * </ol>
	 *
	 * <p><b>Satu arah saja.</b> Tidak ada cabang yang mengembalikan {@code aktif} menjadi
	 * {@code true}. Begitu SOP pernah tercatat non-aktif atau berakhir di simpul penolakan — walau
	 * kemudian dikoreksi — bendera dokumen sudah terlanjur tersimpan {@code false} dan hanya bisa
	 * dipulihkan lewat {@link #setAktif(Boolean)} atau SQL langsung. Ini juga berarti pembacaan
	 * biasa dapat mengubah keadaan tersimpan, pola yang sama dengan getter turunan lain di kelas
	 * ini.</p>
	 *
	 * <p><b>Kasus tepi:</b> substitusi {@code null → true} berlaku pada nilai kembalian, bukan
	 * pada field — jadi kolom di basis data tetap {@code NULL} sampai ada cabang pemadaman yang
	 * benar-benar menyala. Kueri SQL yang menyaring {@code aktif = true} akan <b>melewatkan</b>
	 * baris-baris {@code NULL} yang secara semantik aktif — jebakan yang berulang di banyak entity
	 * repositori ini.</p>
	 *
	 * <p>Properti {@code aktif} juga dibaca lapisan CRUD generik v2 sebagai penanda
	 * "penghapusan lunak" ({@code hasBooleanProperty(metadata, "aktif")}); untuk kelas ini
	 * kemampuan hapus tetap mati karena tidak ada Action sumber yang mendukungnya.</p>
	 *
	 * @return {@code true} bila dokumen dianggap aktif; {@code false} bila SOP-nya non-aktif atau
	 *         berakhir di simpul penolakan
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
	 * Menyetel bendera aktif dokumen secara eksplisit — satu-satunya cara menyalakannya kembali
	 * setelah {@link #getAktif()} memadamkannya.
	 *
	 * @param aktif bendera aktif; {@code null} berarti "belum ditentukan" dan dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan cap posting jurnal dokumen ini.
	 *
	 * <p>{@link PostingHistory} adalah entity "cap posting" murni — satu baris dipakai bersama
	 * oleh banyak dokumen dalam satu batch posting. Konvensi di seluruh modul keuangan repositori
	 * ini: {@code null} berarti dokumen belum dijurnal.</p>
	 *
	 * <p><b>Pada kelas ini kolom itu tidak pernah terisi.</b> Tidak ada
	 * {@code Posting...Action}, tidak ada <i>overload</i>
	 * {@code CommonAkunting.saveTransaksi(...)}, dan tidak ada kolom referensi pada
	 * {@code GrupTransaksi} yang menunjuk kelas ini — jadi tidak ada jalur yang dapat memanggil
	 * {@link #setPostingHistory(PostingHistory)}. Dokumen pinjaman ini secara struktural tidak
	 * dapat menghasilkan jurnal, dan karenanya juga tidak dapat terjurnal ganda.</p>
	 *
	 * <p>Perhatikan {@code @Fetch(FetchMode.SELECT)} tanpa {@code fetch = FetchType.LAZY} pada
	 * {@code @ManyToOne}: relasi ini <b>eager</b> dengan pengambilan lewat SELECT terpisah
	 * (bukan JOIN) — pola yang dipakai seragam untuk cap posting di repositori ini agar cap selalu
	 * tersedia tanpa memperbesar kueri utama.</p>
	 *
	 * @return cap posting, atau {@code null} bila dokumen belum dijurnal (selalu, dalam praktik)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel cap posting jurnal dokumen ini.
	 *
	 * <p>Nol pemanggil di seluruh repositori — lihat {@link #getPostingHistory()}.</p>
	 *
	 * @param postingHistory cap posting; {@code null} menandai dokumen belum/batal diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Nilai bawaan {@link #getFormula()}: teks JSON array kosong (<code>"[]"</code>).
	 *
	 * <p>Dideklarasikan {@code public static} <b>tanpa {@code final}</b> — mengikuti gaya yang
	 * sama di sejumlah entity lain repositori ini. Konsekuensinya konstanta ini
	 * <b>dapat ditimpa</b> oleh kode mana pun dan perubahannya berlaku se-JVM; tidak ada yang
	 * melakukannya saat ini, tetapi keadaan itu tidak dijamin kompilator.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan rincian dokumen dalam bentuk teks JSON array.
	 *
	 * <p>Kolom bertipe {@code text} ini mengikuti pola "rincian sebagai JSON" yang dipakai luas di
	 * repositori ini (mis. pada dokumen kas besar, kas kecil, dan pertanggungjawaban): tiap elemen
	 * array mewakili satu baris rincian, dan layar-layar terkait mem-<i>parse</i>-nya dengan
	 * {@code new JSONArray(...)} lalu merendernya sebagai grid. Menyimpan rincian sebagai teks —
	 * alih-alih tabel anak — berarti tidak ada integritas referensial, tidak ada indeks, dan tidak
	 * ada validasi skema atas isinya.</p>
	 *
	 * <p><b>Substitusi, bukan tulis-balik:</b> bila {@code formula} {@code null} atau kosong,
	 * method mengembalikan {@link #DEFAULT_FORMULA} (<code>"[]"</code>) tanpa mengubah field —
	 * sehingga pemanggil selalu menerima JSON yang sah untuk di-<i>parse</i> dan tidak perlu
	 * memeriksa {@code null}, sementara kolom di basis data tetap kosong.</p>
	 *
	 * <p>Untuk kelas ini tidak ada satu pun pemanggil: tidak ada layar yang membaca atau menulis
	 * rincian pinjaman. Bentuk elemen yang dimaksudkan perancangnya karenanya tidak dapat
	 * dipastikan dari kode.</p>
	 *
	 * @return teks JSON array rincian; <code>"[]"</code> bila belum diisi — tidak pernah
	 *         {@code null}
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menyetel rincian dokumen dalam bentuk teks JSON array.
	 *
	 * <p>Tanpa validasi: teks yang bukan JSON yang sah akan diterima dan baru meledak saat
	 * di-<i>parse</i> pemanggil. Menyetel {@code null} atau string kosong mengembalikan perilaku
	 * bawaan {@link #getFormula()}.</p>
	 *
	 * @param formula teks JSON array rincian; boleh {@code null}
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}
}
