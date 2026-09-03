package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.LogPembayaran;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.asset.AssetDetail;
import ais.database.model.payroll.PembayaranItemGajiPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.database.model.rab.Mitra;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;

/**
 * Kepala (<i>header</i>) cetakan bukti jurnal — kembaran struktural
 * {@link ais.database.model.akunting.GrupTransaksi} yang dimaksudkan sebagai pola siap pakai
 * untuk jurnal berulang, tetapi <b>tidak pernah selesai dihubungkan ke aplikasi</b>.
 *
 * <h2>Peran yang dirancang</h2>
 *
 * <p>Mesin akuntansi AIS memakai pasangan induk&ndash;anak: satu
 * {@link ais.database.model.akunting.GrupTransaksi} (kepala bukti jurnal, tabel
 * <code>akunting.grup_transaksi</code>) memayungi banyak
 * {@link ais.database.model.akunting.Transaksi} (baris debet/kredit, tabel
 * <code>akunting.transaksi</code>). Kelas ini beserta pasangannya
 * {@link ais.database.model.akunting.TemplateTransaksi} adalah salinan pasangan tersebut ke
 * tabel terpisah (<code>akunting.template_grup_transaksi</code> dan
 * <code>akunting.template_transaksi</code>): sebuah bukti jurnal yang disimpan sebagai
 * <i>pola</i>, agar transaksi yang berulang tiap periode tinggal disalin menjadi jurnal
 * sungguhan alih-alih diketik ulang.</p>
 *
 * <p>Sebagai header, kelas ini memuat dimensi yang berlaku untuk <b>seluruh</b> bukti:
 * {@link #getKode() nomor bukti}, {@link #getTanggalTransaksi() tanggal},
 * {@link #getJenisTransaksi() jenis/nomor-seri jurnal} dan
 * {@link #getJenisJurnal() klasifikasi jurnal}, {@link #getKeterangan() keterangan},
 * {@link #getTotalDebet()}/{@link #getTotalKredit() total sisi debet dan kredit},
 * {@link #getMitra() mitra}, {@link #getPegawai() pegawai},
 * {@link #getTbmuser() pembuat}, {@link #getDisetujui() penyetuju},
 * {@link #getPostingHistory() cap posting}, blok data cetak bukti
 * ({@link #getKeperluan()}, {@link #getKepada()}, {@link #getAlamat()},
 * {@link #getKeteranganRekening()}, {@link #getNomorTagihan()},
 * {@link #getTanggalTagihan()}), serta tujuh kolom rujukan dokumen sumber.</p>
 *
 * <h2>Status sesungguhnya: entity TIDUR / YATIM (terverifikasi)</h2>
 *
 * <p><b>Tidak ada satu pun pemanggil kelas ini di seluruh repositori.</b> Penelusuran
 * menyeluruh (peka maupun tak-peka huruf besar/kecil) atas <code>.java</code>,
 * <code>.zul</code>, <code>.jsp</code>, <code>.xml</code>, <code>.sql</code>,
 * <code>.js</code>, dan <code>.csv</code> di <code>src/main/src</code> serta
 * <code>src/main/webapp</code> hanya menemukan lima hal:</p>
 *
 * <ul>
 *   <li>berkas ini sendiri;</li>
 *   <li>rujukan <i>Javadoc</i> saja (bukan kode) di
 *       {@link ais.database.model.akunting.TemplateTransaksi} dan
 *       {@link ais.database.model.akunting.JenisTransaksi};</li>
 *   <li>satu deklarasi field <code>private TemplateGrupTransaksi grupTransaksi;</code> di
 *       {@link ais.database.model.akunting.TemplateTransaksi} — yaitu pasangannya yang juga
 *       tidur, sehingga bukan bukti pemakaian;</li>
 *   <li>satu baris <code>&lt;mapping class=&hellip;&gt;</code> di
 *       <code>hibernate.cfg.xml</code>;</li>
 *   <li>satu baris inventaris di manifes <code>generic-crud</code>
 *       (<code>WEB-INF/generic-crud/manifests/general_value_object_inventory.csv</code> dan
 *       <code>.json</code>), berstatus <code>REVIEW_REQUIRED</code> dengan alasan "nama
 *       mengindikasikan data transaksi/log/internal" dan kolom
 *       <code>existing_actions</code> <b>kosong</b> — jadi belum diaktifkan sebagai layar
 *       CRUD generik.</li>
 * </ul>
 *
 * <p>Konsekuensinya: <b>tidak ada Action, Helper, DAO, servlet REST, layar ZK/JSP, HQL,
 * maupun Criteria yang menulis atau membaca tabel <code>template_grup_transaksi</code></b>.
 * Tidak ada pula method "bangkitkan jurnal dari template" di kelas ini maupun di
 * {@link ais.database.model.akunting.TemplateTransaksi}. Tabelnya tetap dibuat (karena
 * terdaftar di <code>hibernate.cfg.xml</code>) tetapi secara praktis selalu kosong. Jadi
 * <b>kedua</b> anggota pasangan template ini mati, bukan hanya baris detailnya — status
 * yang wajib diverifikasi terpisah dan di sini terkonfirmasi. Pola ini sama dengan entity
 * tidur lain yang sudah terdokumentasi di proyek ini, mis.
 * <code>ais.database.model.sekolah.PengumumanJadwalPelajaran</code>.</p>
 *
 * <p><b>Relasi ke pasangan detailnya bersifat SATU ARAH.</b> Kelas ini <i>tidak</i> memiliki
 * koleksi <code>&#64;OneToMany</code> balik ke
 * {@link ais.database.model.akunting.TemplateTransaksi}; nama kelas itu bahkan tidak
 * disebut sama sekali di berkas ini. Satu-satunya jalan navigasi adalah baris &rarr; header,
 * lewat <code>TemplateTransaksi.getTemplateGrupTransaksi()</code>. Bandingkan dengan
 * {@link ais.database.model.akunting.GrupTransaksi} yang juga tidak memelihara koleksi anak
 * — di sana baris jurnal selalu diambil dengan Criteria terpisah.</p>
 *
 * <p><b>Fitur "Template Jurnal"/"Jurnal Berulang" yang benar-benar hidup adalah entity
 * lain</b>, yaitu {@link ais.database.model.akunting.TemplateJurnalPenyesuaian} (amortisasi,
 * akrual, penyisihan) yang berdiri sendiri, jauh lebih ramping, dan punya penjaga
 * anti-posting-ganda per periode. Pasangan <code>TemplateGrupTransaksi</code> +
 * <code>TemplateTransaksi</code> adalah peninggalan rancangan lama tahun 2010 (lihat komentar
 * <code>hbm2java</code> di kepala berkas, bertanggal 16 April 2010) yang ditinggalkan.</p>
 *
 * <h2>Klon salin-tempel keempat</h2>
 *
 * <p><code>serialVersionUID</code> kelas ini <b>identik</b>
 * (<code>2463821577548439808L</code>) dengan
 * {@link ais.database.model.akunting.GrupTransaksi},
 * {@link ais.database.model.akunting.Transaksi}, dan
 * {@link ais.database.model.akunting.TemplateTransaksi} — bukti keempatnya lahir dari satu
 * cetakan yang disalin berulang. Perbedaan penting terhadap
 * {@link ais.database.model.akunting.GrupTransaksi}, yang seluruhnya sudah diverifikasi dari
 * kode:</p>
 *
 * <ol>
 *   <li><b>Kolom rujukan dokumen hanya tujuh, bukan 41.</b> Header jurnal sungguhan
 *       menyimpan 41 relasi ke dokumen sumber (pembayaran siswa, penggajian, aset, koperasi,
 *       pajak, kas kecil, &hellip;). Kelas ini hanya membawa tujuh yang tertua:
 *       {@link #getCicilanPembayaran()}, {@link #getPembayaranSiswaDetail()},
 *       {@link #getDepositSiswa()}, {@link #getPembayaranItemGajiPegawai()},
 *       {@link #getTransaksiPegawai()}, {@link #getLogPembayaran()}, dan
 *       {@link #getAssetDetail()} — potret modul yang ada pada 2010, membeku sejak itu.</li>
 *   <li><b>Tidak ada mekanisme penguncian &amp; anti-duplikat.</b>
 *       {@link ais.database.model.akunting.GrupTransaksi} memiliki <code>kodeUnik</code>
 *       (kunci idempotensi), <code>ref</code> (penanda dokumen sumber), dan
 *       <code>closing</code> (pengait ke periode tutup buku yang mengunci jurnal). Ketiganya
 *       <b>tidak ada</b> di sini. Bila entity ini kelak dihidupkan tanpa menambahkannya, tidak
 *       ada satu pun penjaga posting-ganda.</li>
 *   <li><b>Tidak ada dimensi <code>workspace</code>.</b> Header jurnal sungguhan punya
 *       <code>workspace</code> (anggaran) <i>dan</i> <code>satuanKerja</code>; kelas ini hanya
 *       punya {@link #getSatuanKerja()}. Menariknya, di keluarga template dimensi
 *       <i>workspace</i> justru menempel di tingkat <b>baris</b>
 *       (<code>TemplateTransaksi.getDefaultWorkspace()</code>) dan hilang dari headernya.</li>
 *   <li><b>Tidak ada method bisnis kecuali satu.</b> Header jurnal sungguhan memuat mesin
 *       penyusun jurnal, keluarga <code>tampilkanJurnal(&hellip;)</code>, dan
 *       <code>ambilUnik()</code>. Di sini yang tersisa hanya
 *       {@link #populateDeskripsi()} — dan justru method itulah yang tidak selesai
 *       disesuaikan (lihat di bawah).</li>
 * </ol>
 *
 * <h2>Hal-hal non-obvious &amp; kuirk</h2>
 *
 * <ul>
 *   <li><b>{@link #populateDeskripsi()} menanyakan tabel yang SALAH.</b> Method itu
 *       menjalankan Criteria atas {@link ais.database.model.akunting.Transaksi} (baris jurnal
 *       <b>sungguhan</b>) dengan <code>Restrictions.eq("grupTransaksi", this)</code>, padahal
 *       <code>this</code> bertipe <code>TemplateGrupTransaksi</code>. Yang seharusnya
 *       ditanyakan adalah {@link ais.database.model.akunting.TemplateTransaksi} dengan nama
 *       properti <code>templateGrupTransaksi</code>. Ini sisa salin-tempel dari
 *       {@code GrupTransaksi.populateDeskripsi()} yang tidak ikut diubah. Rincian akibatnya
 *       ada pada Javadoc method tersebut — cukup dicatat di sini bahwa dampaknya
 *       <b>menyilang ke data jurnal resmi</b>, bukan sekadar hasil kosong.</li>
 *   <li><b>Getter destruktif (menulis balik ke field).</b> Karena
 *       <code>dynamicUpdate = true</code> dan entity berada dalam sesi Hibernate hidup,
 *       penulisan balik berikut bisa ikut ter-<i>flush</i> menjadi <code>UPDATE</code> hanya
 *       karena baris dibaca: {@link #getTotalDebet()} dan {@link #getTotalKredit()}
 *       (menormalkan <code>null</code> menjadi <code>0.0</code>), serta
 *       {@link #getSatuanKerja()} dan {@link #getPembayaranItemGajiPegawai()} (menimpa field
 *       dengan hasil <code>check(&hellip;)</code>, pe-resolve proxy lazy dari kelas induk).
 *       {@link #populateDeskripsi()} bahkan tidak menunggu <i>flush</i> — ia memanggil
 *       {@code Common.refreshUpdate} yang melakukan <code>saveOrUpdate</code> + flush
 *       langsung.</li>
 *   <li><b>Dua getter menormalkan tanpa menulis balik.</b> {@link #getKepada()} dan
 *       {@link #getNomorTagihan()} mengembalikan <code>""</code> untuk <code>null</code> dan
 *       memangkas spasi, tetapi <b>tidak</b> menyentuh field — jadi keduanya aman dibaca dari
 *       <i>renderer</i>. Perhatikan bahwa nilai yang tersimpan di DB tetap bisa
 *       <code>null</code>/berspasi; penyaring atau perbandingan yang memakai field langsung
 *       (HQL/Criteria) akan melihat nilai mentah, bukan hasil normalisasi ini.</li>
 *   <li><b>Setter audit menolak nilai kosong secara diam-diam.</b> {@link #setOleh(String)}
 *       dan {@link #setOlehId(String)} langsung <code>return</code> untuk
 *       <code>null</code>/string kosong, sehingga jejak audit tidak dapat dikosongkan dan
 *       pemanggil tidak diberi umpan balik apa pun.</li>
 *   <li><b>{@link #toString()} dapat mengembalikan <code>null</code>.</b> Method
 *       mengembalikan field <code>kode</code> apa adanya, tanpa normalisasi. Kolomnya memang
 *       <code>nullable = false</code>, tetapi objek yang belum tersimpan tetap membawa
 *       <code>null</code>, sehingga pemanggil yang langsung merantai operasi String atas
 *       hasilnya (pola lazim di <i>renderer</i> ZK) akan gagal. Bandingkan dengan
 *       {@link #getKepada()} yang justru menormalkan.</li>
 *   <li><b>Delapan properti tidak beranotasi <code>&#64;Column</code>.</b>
 *       {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 *       {@link #getKepada()}, {@link #getAlamat()}, {@link #getKeteranganRekening()},
 *       {@link #getNomorTagihan()}, dan {@link #getTanggalTagihan()} jatuh ke strategi
 *       penamaan {@code ais.database.hibernate.MyNamingStrategy}, turunan
 *       {@code DefaultNamingStrategy} yang memakai <b>nama properti apa adanya</b> sebagai
 *       nama kolom. Akibatnya skema tabel ini bercampur: kolom yang dianotasi memakai
 *       <i>snake_case</i> (<code>total_debet</code>, <code>jenis_jurnal</code>), sedangkan
 *       yang tidak dianotasi memakai <i>camelCase</i> (<code>keteranganRekening</code>,
 *       <code>nomorTagihan</code>, <code>tanggalTagihan</code>, <code>olehId</code>).</li>
 *   <li><b>Cakupan tenant: tidak ada sama sekali.</b> Entity ini tidak memiliki kolom
 *       <code>sekolah</code> maupun <code>yayasan</code>. Satu-satunya pembatas ruang lingkup
 *       adalah {@link #getSatuanKerja()}, yang <b>tidak pernah disetel dari mana pun</b> di
 *       repositori — jadi de facto selalu <code>null</code>. Selama entity ini tidur hal itu
 *       tidak berdampak; begitu dihidupkan, ia akan menjadi katalog global lintas tenant
 *       kecuali penyaringnya ditambahkan lebih dahulu (pola <i>fail-open</i> cakupan tenant
 *       yang berulang di modul lain).</li>
 *   <li><b>Persetujuan tanpa gerbang.</b> {@link #getDisetujui()} adalah relasi
 *       <code>&#64;ManyToOne</code> ke {@link ais.database.model.Tbmuser} — jadi "disetujui"
 *       di sini berarti <i>siapa</i> yang menyetujui, bukan bendera boolean. Tidak ada satu
 *       pun kode yang menyetel maupun membacanya, sehingga saat ini tidak ada siapa pun yang
 *       "bisa mengubah" nilai itu. Bila entity ini dihidupkan, perhatikan bahwa <b>tidak ada
 *       apa pun di tingkat entity yang mencegah pembuat template menuliskan dirinya sendiri
 *       sebagai penyetuju</b> — gerbang wajib dibuat di lapisan Action/Helper, karena pola
 *       persetujuan-diri-sendiri sudah berulang kali terkonfirmasi di modul finansial AIS.</li>
 *   <li><b>Tidak ada gerbang hak akses.</b> Karena tidak ada Action/Helper, tidak ada pula
 *       <code>checkPrevilages()</code> yang menjaga siapa boleh membuat/mengubah/menyetujui
 *       template. Ini penting bila fitur ini kelak dihidupkan: template jurnal adalah sasaran
 *       empuk untuk menyuntikkan jurnal palsu <i>berulang</i>, jadi layar pembuatnya wajib
 *       memakai menu terdaftar sendiri — <b>bukan</b> disisipkan sebagai tab di bawah menu
 *       induk, karena mekanisme <code>checkPrevilages()</code> membaca atribut sesi
 *       <code>currentMenu</code> yang tidak di-resolve ulang untuk halaman ter-<i>include</i>,
 *       sehingga hak menu induk otomatis diwarisi tab anaknya (pola yang sudah tercatat
 *       puluhan kali di proyek ini). Jalur kebangkitan yang paling mungkin adalah mesin
 *       <code>generic-crud</code>, yang sudah mendaftarkan entity ini di manifesnya; status
 *       <code>REVIEW_REQUIRED</code> di manifes itu sebaiknya dipertahankan sampai gerbang
 *       haknya ada.</li>
 *   <li><b>Riwayat Envers ikut aktif.</b> <code>&#64;Audited</code> membuat setiap versi baris
 *       digandakan ke tabel <code>template_grup_transaksi_aud</code> — termasuk revisi yang
 *       lahir dari getter destruktif di atas, bukan dari tindakan pengguna.</li>
 *   <li><b>Komentar kepala kelas asli menyesatkan.</b> Blok
 *       <code>Bank generated by hbm2java</code> yang digantikan Javadoc ini adalah sisa
 *       pembangkitan otomatis: kelas ini tidak ada hubungannya dengan entity
 *       <code>Bank</code>. Komentar identik muncul di beberapa entity akunting lain.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #onUpdate()}, {@link #getTanggal_dirubah()},
 *       {@link #toString()}, {@link #getTbmuser()}.</li>
 *   <li><b>Identitas bukti:</b> {@link #getKode()}, {@link #getParentCode()},
 *       {@link #getTanggalTransaksi()}, {@link #getKeterangan()},
 *       {@link #getJenisTransaksi()}, {@link #getJenisJurnal()}.</li>
 *   <li><b>Nominal ringkas:</b> {@link #getTotalDebet()}, {@link #getTotalKredit()}.</li>
 *   <li><b>Data cetak bukti / kuitansi:</b> {@link #getKeperluan()}, {@link #getKepada()},
 *       {@link #getAlamat()}, {@link #getKeteranganRekening()}, {@link #getNomorTagihan()},
 *       {@link #getTanggalTagihan()}, {@link #getMitra()}, {@link #getPegawai()}.</li>
 *   <li><b>Status alur kerja:</b> {@link #getDisetujui()}, {@link #getPostingHistory()}.</li>
 *   <li><b>Rujukan dokumen sumber (tujuh kolom):</b> {@link #getCicilanPembayaran()},
 *       {@link #getPembayaranSiswaDetail()}, {@link #getDepositSiswa()},
 *       {@link #getPembayaranItemGajiPegawai()}, {@link #getTransaksiPegawai()},
 *       {@link #getLogPembayaran()}, {@link #getAssetDetail()}.</li>
 *   <li><b>Dimensi organisasi:</b> {@link #getSatuanKerja()}.</li>
 *   <li><b>Penyajian:</b> {@link #getDeskripsi()} dan {@link #populateDeskripsi()} —
 *       satu-satunya method dengan logika nyata di kelas ini.</li>
 * </ol>
 *
 * <p>Kelas ini mewarisi {@link ais.database.model.GeneralValueObject}. Perlu diingat bahwa
 * kelas induk tersebut <b>bukan</b> <code>&#64;Entity</code> maupun
 * <code>&#64;MappedSuperclass</code> — ia POJO abstrak biasa, sehingga Hibernate tidak
 * memetakan propertinya. Field yang dideklarasikan ulang di sini (mis.
 * <code>oleh</code>/<code>olehId</code>/<code>tanggal_dirubah</code>) karena itu <b>bukan bug
 * duplikasi</b>, melainkan keharusan teknis agar kolomnya benar-benar terpetakan. Fasilitas
 * yang benar-benar dipakai dari induk adalah <code>check(&hellip;)</code>, pe-resolve proxy
 * lazy yang dipanggil {@link #getSatuanKerja()} dan
 * {@link #getPembayaranItemGajiPegawai()}.</p>
 *
 * @see ais.database.model.akunting.TemplateTransaksi
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.TemplateJurnalPenyesuaian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "template_grup_transaksi")

public class TemplateGrupTransaksi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p><b>Catatan verifikasi:</b> nilainya <b>identik</b> dengan
	 * {@link ais.database.model.akunting.GrupTransaksi},
	 * {@link ais.database.model.akunting.Transaksi}, dan
	 * {@link ais.database.model.akunting.TemplateTransaksi} — keempat kelas lahir dari satu
	 * cetakan salin-tempel dan nilai ini ikut tersalin, bukan dihitung ulang per kelas.
	 * Nilai yang sama pada kelas berbeda tidak menimbulkan masalah bagi Java (identitas
	 * kelas ditentukan nama kelas, bukan angka ini), tetapi merupakan sidik jari duplikasi
	 * yang jelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (<code>IDENTITY</code>), lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi otomatis, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi otomatis, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} pada setiap <code>UPDATE</code>; tidak
	 * pernah disetel manual dari kode aplikasi.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau <code>null</code> bila baris belum pernah
	 *         diperbarui
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk penting:</b> nilai <code>null</code> maupun string kosong/berisi spasi
	 * saja <b>diabaikan diam-diam</b> — method langsung <code>return</code> tanpa mengubah
	 * apa pun dan tanpa memberi umpan balik. Akibatnya jejak audit tidak dapat dikosongkan
	 * kembali setelah pernah terisi. Pola satu baris yang sama dipakai di seluruh keluarga
	 * entity akunting.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila <code>null</code> atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris ini untuk komponen UI (mis. <code>Combobox</code>/
	 * <code>Listbox</code> ZK) dan untuk pesan log.
	 *
	 * <p><b>Peringatan:</b> mengembalikan field {@link #getKode() kode} <b>apa adanya</b>,
	 * sehingga <b>dapat mengembalikan <code>null</code></b> untuk objek baru yang kodenya
	 * belum diisi. Pemanggil yang sekadar merangkai string (mis.
	 * <code>"Bukti " + obj</code>) akan mendapat teks <code>"null"</code>, sedangkan
	 * pemanggil yang langsung merantai operasi String atas hasilnya (mis.
	 * <code>obj.toString().length()</code>) akan melempar
	 * {@code NullPointerException}. Berbeda dengan
	 * {@link #getKepada()}/{@link #getNomorTagihan()}, di sini tidak ada normalisasi
	 * <code>null</code> menjadi <code>""</code>.</p>
	 *
	 * @return nomor bukti template, bisa <code>null</code>
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk penting:</b> sama seperti {@link #setOlehId(String)}, nilai
	 * <code>null</code> maupun string kosong <b>diabaikan diam-diam</b>.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila <code>null</code> atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()}; tidak pernah disetel manual dari kode
	 * aplikasi.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau <code>null</code> bila belum pernah
	 *         diperbarui
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum setiap <code>UPDATE</code> baris ini.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)},
	 * yang mengisi {@link #getTanggal_dirubah()}, {@link #getOleh()}, dan
	 * {@link #getOlehId()} dari konteks pengguna yang sedang aktif. Interceptor itu
	 * melewatkan pembaruan yang dinilai tidak membawa perubahan bisnis
	 * (<code>AuditTrailHelper.peekUpdateDecision</code>), sehingga jejak audit tidak ikut
	 * bergerak untuk <code>UPDATE</code> kosong.</p>
	 *
	 * <p><b>Efek samping:</b> memodifikasi tiga properti entity ini pada saat
	 * <i>flush</i>.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> oleh penyedia JPA/Hibernate, bukan oleh kode aplikasi.
	 * Karena entity ini tidur, praktis tidak pernah menyala.</p>
	 *
	 * <p><b>Catatan gaya:</b> baris ini juga mendeklarasikan field
	 * <code>tanggal_dirubah</code> pada baris fisik yang sama — bentuk sisipan otomatis yang
	 * dipakai seragam di seluruh entity AIS; jangan dirapikan karena akan mengubah
	 * <i>diff</i> di puluhan berkas.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi — nilainya ditulis
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai <code>TIMESTAMP</code>. Tanpa <code>&#64;Column</code>, sehingga
	 * nama kolom mengikuti nama properti apa adanya
	 * ({@code MyNamingStrategy} &rarr; <code>tanggal_dirubah</code>). Nilai awal field diisi
	 * {@code WaktuUtil.getDate()} saat objek dibuat, jadi tidak pernah <code>null</code>
	 * untuk objek baru.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor/kode bukti template; lihat {@link #getKode()}. */
	private String kode;
	/** Tanggal bukti template, diinisialisasi ke waktu sekarang; lihat {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi = ais.ui.util.WaktuUtil.getDate();
	/** Pengguna pembuat template; lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Pegawai yang terkait bukti; lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Keterangan bebas tingkat header; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Total sisi debet seluruh baris template; lihat {@link #getTotalDebet()}. */
	private Double totalDebet;
	/** Total sisi kredit seluruh baris template; lihat {@link #getTotalKredit()}. */
	private Double totalKredit;
	/** Klasifikasi jurnal (umum/penerimaan/pengeluaran); lihat {@link #getJenisJurnal()}. */
	private String jenisJurnal;
	/** Kode bukti induk untuk penjurnalan berjenjang; lihat {@link #getParentCode()}. */
	private String parentCode;

	/** Uraian keperluan pada cetakan bukti; lihat {@link #getKeperluan()}. */
	private String keperluan;
	/** Nama pihak penerima pada cetakan bukti; lihat {@link #getKepada()}. */
	private String kepada;
	/** Alamat pihak penerima pada cetakan bukti; lihat {@link #getAlamat()}. */
	private String alamat;
	/** Keterangan rekening tujuan pada cetakan bukti; lihat {@link #getKeteranganRekening()}. */
	private String keteranganRekening;
	/** Nomor tagihan/faktur rujukan; lihat {@link #getNomorTagihan()}. */
	private String nomorTagihan;
	/** Tanggal tagihan/faktur rujukan; lihat {@link #getTanggalTagihan()}. */
	private Date tanggalTagihan;

	/** Mitra/rekanan lawan transaksi; lihat {@link #getMitra()}. */
	private Mitra mitra;
	/** Jenis (nomor seri) jurnal; lihat {@link #getJenisTransaksi()}. */
	private JenisTransaksi jenisTransaksi;
	/** Pengguna yang menyetujui template; lihat {@link #getDisetujui()}. */
	private Tbmuser disetujui;
	/** Cap batch posting; lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	// reference
	/*
	 * Tujuh kolom rujukan dokumen sumber di bawah ini adalah potret modul AIS pada 2010.
	 * Bandingkan dengan GrupTransaksi (header jurnal sungguhan) yang kini membawa 41 kolom
	 * serupa. Pola pemakaiannya: paling banyak satu di antaranya terisi, menandai dokumen
	 * operasional yang menurunkan bukti ini. Karena entity ini tidur, tidak ada satu pun
	 * yang pernah diisi.
	 */
	/** Rujukan ke cicilan pembayaran; lihat {@link #getCicilanPembayaran()}. */
	private CicilanPembayaran cicilanPembayaran;
	/** Rujukan ke detail pembayaran siswa; lihat {@link #getPembayaranSiswaDetail()}. */
	private PembayaranSiswaDetail pembayaranSiswaDetail;
	/** Rujukan ke deposit siswa; lihat {@link #getDepositSiswa()}. */
	private DepositSiswa depositSiswa;
	/** Rujukan ke pembayaran item gaji pegawai; lihat {@link #getPembayaranItemGajiPegawai()}. */
	private PembayaranItemGajiPegawai pembayaranItemGajiPegawai;
	/** Rujukan ke transaksi pegawai; lihat {@link #getTransaksiPegawai()}. */
	private TransaksiPegawai transaksiPegawai;
	/** Rujukan ke log pembayaran; lihat {@link #getLogPembayaran()}. */
	private LogPembayaran logPembayaran;
	/** Rujukan ke detail aset; lihat {@link #getAssetDetail()}. */
	private AssetDetail assetDetail;

	/** Cuplikan HTML tabel jurnal hasil {@link #populateDeskripsi()}; lihat {@link #getDeskripsi()}. */
	private String deskripsi;
	/** Satuan kerja pemilik bukti; lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Tidak melakukan apa pun selain membiarkan penginisialisasi field berjalan:
	 * <code>tanggalTransaksi</code> dan <code>tanggal_dirubah</code> terisi waktu sekarang
	 * ({@code WaktuUtil.getDate()}), sisanya <code>null</code>.</p>
	 */
	public TemplateGrupTransaksi() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data (<code>IDENTITY</code>) dan karena itu
	 * <code>insertable = false</code>. Bernilai <code>null</code> untuk objek yang belum
	 * pernah disimpan — kondisi yang dipakai {@link #populateDeskripsi()} sebagai penjaga
	 * masuk.</p>
	 *
	 * <p>Karena <code>&#64;Id</code> dipasang pada <i>getter</i>, Hibernate memakai
	 * <b>property access</b> untuk seluruh kelas ini: setiap getter di berkas ini ikut
	 * dipanggil saat <i>load</i>, <i>flush</i>, dan pemeriksaan <i>dirty</i>. Itulah sebabnya
	 * getter yang menulis balik ke field (lihat Javadoc kelas) bisa berubah menjadi
	 * <code>UPDATE</code> sungguhan.</p>
	 *
	 * @return kunci utama, atau <code>null</code> bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate setelah <code>INSERT</code>. Menyetelnya manual
	 * atas objek yang sudah persisten akan membingungkan sesi Hibernate.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor/kode bukti template.
	 *
	 * <p>Kolom <code>kode</code>, <code>nullable = false</code>, panjang 255. Ini juga nilai
	 * yang dikembalikan {@link #toString()}. Berbeda dengan
	 * {@link ais.database.model.akunting.GrupTransaksi}, di sini <b>tidak ada</b>
	 * <code>kodeUnik</code> maupun batasan unik pada kolom ini — jadi tidak ada penjaga
	 * duplikasi kode template di tingkat entity.</p>
	 *
	 * @return nomor bukti template, bisa <code>null</code> untuk objek baru
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel nomor/kode bukti template.
	 *
	 * @param kode nomor bukti template
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas tingkat header.
	 *
	 * <p>Berbeda dari keterangan per baris pada
	 * {@link ais.database.model.akunting.TemplateTransaksi}, yang dirangkai
	 * {@link #populateDeskripsi()} ke kolom "Keterangan" tabel HTML.</p>
	 *
	 * @return keterangan header, bisa <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas tingkat header.
	 *
	 * @param keterangan keterangan header
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tanggal bukti template.
	 *
	 * @param tanggalTransaksi tanggal bukti
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan tanggal bukti template.
	 *
	 * <p>Dipetakan sebagai <code>TIMESTAMP</code> pada kolom <code>tanggal_transaksi</code>
	 * yang <code>nullable = false</code>. Field diinisialisasi ke
	 * {@code WaktuUtil.getDate()} saat objek dibuat, jadi objek baru tidak pernah membawa
	 * <code>null</code>.</p>
	 *
	 * <p>Pada keluarga jurnal sungguhan, tanggal header inilah yang menang atas tanggal
	 * baris; <code>TemplateTransaksi.getTanggalTransaksi()</code> pun menimpa dirinya dari
	 * nilai header ini. Karena kedua entity tidur, perilaku itu tidak pernah terjadi.</p>
	 *
	 * @return tanggal bukti template
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi", nullable = false)
	public Date getTanggalTransaksi() {
		return tanggalTransaksi;
	}

	/**
	 * Menyetel pengguna pembuat template.
	 *
	 * @param tbmuser pengguna pembuat
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan pengguna pembuat template.
	 *
	 * <p>Relasi <code>&#64;ManyToOne</code> ke {@link ais.database.model.Tbmuser} dengan
	 * <i>cascade</i> <code>PERSIST</code>+<code>MERGE</code> dan
	 * <code>FetchMode.SELECT</code>. Perhatikan bahwa <i>cascade</i> ini berarti menyimpan
	 * template dapat ikut menyimpan/menggabungkan baris pengguna — perilaku yang seragam
	 * pada seluruh relasi di berkas ini dan berisiko bila objek pengguna yang dipasang
	 * berasal dari sesi lain.</p>
	 *
	 * <p>Tidak ada <code>check(&hellip;)</code> di sini (berbeda dari
	 * {@link #getSatuanKerja()}), sehingga getter ini tidak destruktif.</p>
	 *
	 * @return pengguna pembuat, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		return tbmuser;
	}

	/**
	 * Menyetel total sisi debet.
	 *
	 * <p>Nilai ini <b>tidak</b> dihitung ulang dari baris template mana pun — tidak ada kode
	 * yang menjumlahkan {@link ais.database.model.akunting.TemplateTransaksi} ke sini. Bila
	 * fitur ini dihidupkan, penjumlahan dan pemeriksaan keseimbangan Dr = Cr harus
	 * ditambahkan sendiri.</p>
	 *
	 * @param totalDebet total sisi debet
	 */
	public void setTotalDebet(Double totalDebet) {
		this.totalDebet = totalDebet;
	}

	/**
	 * Mengembalikan total sisi debet seluruh baris template.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila field bernilai <code>null</code>,
	 * method <b>menulis</b> <code>0.0</code> ke field sebelum mengembalikannya. Karena
	 * Hibernate memakai <i>property access</i> dan kelas ini <code>dynamicUpdate</code>,
	 * sekadar membaca baris di dalam sesi hidup dapat menghasilkan <code>UPDATE</code> yang
	 * mengubah <code>NULL</code> menjadi <code>0</code> di basis data — sekaligus
	 * menerbitkan revisi Envers baru. Pola identik ada pada {@link #getTotalKredit()} dan
	 * pada {@code Transaksi.getDebet()}/{@code getKredit()}.</p>
	 *
	 * <p>Perbedaan <code>NULL</code> vs <code>0</code> hilang setelah pembacaan pertama,
	 * sehingga "belum diisi" tidak lagi bisa dibedakan dari "nol".</p>
	 *
	 * @return total sisi debet, tidak pernah <code>null</code>
	 */
	@Column(name = "total_debet", nullable = true)
	public Double getTotalDebet() {
		if (totalDebet == null) {
			totalDebet = 0.0;
		}
		return totalDebet;
	}

	/**
	 * Menyetel total sisi kredit.
	 *
	 * <p>Seperti {@link #setTotalDebet(Double)}, nilai ini tidak pernah dihitung ulang dari
	 * baris template.</p>
	 *
	 * @param totalKredit total sisi kredit
	 */
	public void setTotalKredit(Double totalKredit) {
		this.totalKredit = totalKredit;
	}

	/**
	 * Mengembalikan total sisi kredit seluruh baris template.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> identik dengan {@link #getTotalDebet()} —
	 * <code>null</code> ditulis balik menjadi <code>0.0</code>, yang dapat ter-<i>flush</i>
	 * menjadi <code>UPDATE</code> hanya karena baris dibaca.</p>
	 *
	 * @return total sisi kredit, tidak pernah <code>null</code>
	 */
	@Column(name = "total_kredit", nullable = true)
	public Double getTotalKredit() {
		if (totalKredit == null) {
			totalKredit = 0.0;
		}
		return totalKredit;
	}

	/**
	 * Menyetel klasifikasi jurnal.
	 *
	 * @param jenisJurnal klasifikasi jurnal (mis. jurnal umum, penerimaan, pengeluaran)
	 */
	public void setJenisJurnal(String jenisJurnal) {
		this.jenisJurnal = jenisJurnal;
	}

	/**
	 * Mengembalikan klasifikasi jurnal bukti ini.
	 *
	 * <p>Kolom string bebas <code>jenis_jurnal</code>. Sumbu klasifikasi ini <b>berbeda</b>
	 * dari {@link #getJenisTransaksi()}: yang ini menyatakan jenis buku (umum/penerimaan/
	 * pengeluaran), sedangkan {@link #getJenisTransaksi()} adalah katalog kode/nomor-seri
	 * jurnal yang didefinisikan operator. Tidak ada konstanta atau <i>enum</i> pembatas di
	 * kelas ini — nilai sah hanya dipahami lewat konvensi di modul jurnal sungguhan.</p>
	 *
	 * @return klasifikasi jurnal, bisa <code>null</code>
	 */
	@Column(name = "jenis_jurnal", nullable = true)
	public String getJenisJurnal() {
		return jenisJurnal;
	}

	/**
	 * Menyetel kode bukti induk.
	 *
	 * @param parentCode kode bukti induk
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * Mengembalikan kode bukti induk untuk penjurnalan berjenjang.
	 *
	 * <p>Kolom string bebas — <b>bukan</b> relasi <code>&#64;ManyToOne</code> ke baris lain
	 * pada tabel ini, sehingga tidak ada integritas referensial: nilai bisa menunjuk kode
	 * yang tidak ada. Tidak ada kode di repositori yang menelusuri hierarki ini.</p>
	 *
	 * @return kode bukti induk, bisa <code>null</code>
	 */
	@Column(name = "parent_code", nullable = true)
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * Mengembalikan pegawai yang terkait dengan bukti ini.
	 *
	 * <p>Relasi <code>&#64;ManyToOne</code> ke {@link ais.database.model.Pegawai}
	 * (<i>eager</i>, <code>FetchMode.SELECT</code>). Pada jurnal sungguhan kolom ini
	 * dipakai untuk bukti yang bersumber dari penggajian/panjar pegawai. Tidak destruktif —
	 * tidak ada <code>check(&hellip;)</code>.</p>
	 *
	 * @return pegawai terkait, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		return pegawai;
	}

	/**
	 * Menyetel pegawai yang terkait dengan bukti ini.
	 *
	 * @param pegawai pegawai terkait
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan uraian keperluan yang dicetak pada bukti.
	 *
	 * <p>Kolom <code>keperluan</code> bertipe <code>text</code> (bebas panjang). Nilainya
	 * dipakai layar cetak kuitansi pada modul jurnal sungguhan; di sini tidak ada
	 * pemakainya.</p>
	 *
	 * @return uraian keperluan, bisa <code>null</code>
	 */
	@Column(name = "keperluan", columnDefinition = "text", nullable = true)
	public String getKeperluan() {
		return keperluan;
	}

	/**
	 * Menyetel uraian keperluan yang dicetak pada bukti.
	 *
	 * @param keperluan uraian keperluan
	 */
	public void setKeperluan(String keperluan) {
		this.keperluan = keperluan;
	}

	/**
	 * Mengembalikan nama pihak penerima pada cetakan bukti.
	 *
	 * <p><b>Normalisasi non-destruktif:</b> <code>null</code> dikembalikan sebagai
	 * <code>""</code> dan spasi tepi dipangkas, tetapi field <b>tidak</b> ditulis balik —
	 * jadi getter ini aman dipanggil dari <i>renderer</i> dan tidak menerbitkan
	 * <code>UPDATE</code>. Bandingkan dengan {@link #getTotalDebet()} yang justru
	 * destruktif.</p>
	 *
	 * <p><b>Konsekuensi:</b> nilai di basis data tetap bisa <code>null</code> atau
	 * berspasi; HQL/Criteria yang menyaring kolom ini melihat nilai mentah, bukan hasil
	 * normalisasi. Properti ini tidak beranotasi <code>&#64;Column</code>, sehingga nama
	 * kolomnya mengikuti nama properti apa adanya: <code>kepada</code>.</p>
	 *
	 * @return nama pihak penerima, tidak pernah <code>null</code>
	 */
	public String getKepada() {
		return kepada == null ? "" : kepada.trim();
	}

	/**
	 * Menyetel nama pihak penerima pada cetakan bukti.
	 *
	 * <p>Menyimpan nilai apa adanya — pemangkasan hanya terjadi saat dibaca lewat
	 * {@link #getKepada()}.</p>
	 *
	 * @param kepada nama pihak penerima
	 */
	public void setKepada(String kepada) {
		this.kepada = kepada;
	}

	/**
	 * Mengembalikan alamat pihak penerima pada cetakan bukti.
	 *
	 * <p>Tanpa normalisasi apa pun (berbeda dari {@link #getKepada()}), dan tanpa
	 * <code>&#64;Column</code> sehingga nama kolomnya <code>alamat</code>.</p>
	 *
	 * @return alamat pihak penerima, bisa <code>null</code>
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menyetel alamat pihak penerima pada cetakan bukti.
	 *
	 * @param alamat alamat pihak penerima
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan keterangan rekening tujuan yang dicetak pada bukti.
	 *
	 * <p>Kolom teks bebas (nomor rekening, nama bank, atas nama) — <b>bukan</b> relasi ke
	 * entity bank/rekening mana pun, jadi tidak ada validasi format maupun integritas.
	 * Tanpa <code>&#64;Column</code>, sehingga nama kolom mengikuti nama properti apa adanya
	 * dalam <i>camelCase</i>: <code>keteranganRekening</code>.</p>
	 *
	 * @return keterangan rekening tujuan, bisa <code>null</code>
	 */
	public String getKeteranganRekening() {
		return keteranganRekening;
	}

	/**
	 * Menyetel keterangan rekening tujuan yang dicetak pada bukti.
	 *
	 * @param keteranganRekening keterangan rekening tujuan
	 */
	public void setKeteranganRekening(String keteranganRekening) {
		this.keteranganRekening = keteranganRekening;
	}

	/**
	 * Mengembalikan nomor tagihan/faktur yang dirujuk bukti ini.
	 *
	 * <p><b>Normalisasi non-destruktif:</b> seperti {@link #getKepada()}, <code>null</code>
	 * menjadi <code>""</code> dan spasi tepi dipangkas tanpa menulis balik ke field.</p>
	 *
	 * <p>Ini kolom string bebas, <b>bukan</b> relasi ke entity tagihan — jadi tidak ada
	 * jaminan nomor yang ditulis benar-benar ada. Tanpa <code>&#64;Column</code>, nama
	 * kolomnya <code>nomorTagihan</code>.</p>
	 *
	 * @return nomor tagihan, tidak pernah <code>null</code>
	 */
	public String getNomorTagihan() {
		return nomorTagihan == null ? "" : nomorTagihan.trim();
	}

	/**
	 * Menyetel nomor tagihan/faktur yang dirujuk bukti ini.
	 *
	 * @param nomorTagihan nomor tagihan
	 */
	public void setNomorTagihan(String nomorTagihan) {
		this.nomorTagihan = nomorTagihan;
	}

	/**
	 * Mengembalikan tanggal tagihan/faktur yang dirujuk bukti ini.
	 *
	 * <p>Dipetakan sebagai <code>DATE</code> (tanpa komponen jam), berbeda dari
	 * {@link #getTanggalTransaksi()} yang <code>TIMESTAMP</code>. Tanpa
	 * <code>&#64;Column</code>, sehingga nama kolomnya <code>tanggalTagihan</code>.</p>
	 *
	 * @return tanggal tagihan, bisa <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihan() {
		return tanggalTagihan;
	}

	/**
	 * Menyetel tanggal tagihan/faktur yang dirujuk bukti ini.
	 *
	 * @param tanggalTagihan tanggal tagihan
	 */
	public void setTanggalTagihan(Date tanggalTagihan) {
		this.tanggalTagihan = tanggalTagihan;
	}

	/**
	 * Mengembalikan mitra/rekanan lawan transaksi.
	 *
	 * <p>Relasi <code>&#64;ManyToOne</code> ke {@link ais.database.model.rab.Mitra}
	 * (modul RAB/pengadaan). Pada jurnal sungguhan kolom ini menandai vendor atau pihak
	 * ketiga pada bukti pengeluaran. Tidak destruktif.</p>
	 *
	 * @return mitra lawan transaksi, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mitra", nullable = true)
	public Mitra getMitra() {
		return mitra;
	}

	/**
	 * Menyetel mitra/rekanan lawan transaksi.
	 *
	 * @param mitra mitra lawan transaksi
	 */
	public void setMitra(Mitra mitra) {
		this.mitra = mitra;
	}

	/**
	 * Mengembalikan jenis (nomor seri) jurnal bukti ini.
	 *
	 * <p>Relasi <code>&#64;ManyToOne</code> ke
	 * {@link ais.database.model.akunting.JenisTransaksi} — katalog kode/nomor-seri jurnal
	 * yang didefinisikan operator (PB, JBI, JCO, dan seterusnya), <b>bukan</b> sumbu
	 * "umum/penerimaan/pengeluaran" (itu {@link #getJenisJurnal()}).</p>
	 *
	 * <p>Pada keluarga jurnal sungguhan, baris mewarisi jenis dari headernya; di keluarga
	 * template pewarisan itu ditulis di sisi baris
	 * (<code>TemplateTransaksi.getJenisTransaksi()</code>) dan tidak pernah berjalan karena
	 * kedua entity tidur.</p>
	 *
	 * @return jenis jurnal, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_transaksi", nullable = true)
	public JenisTransaksi getJenisTransaksi() {
		return jenisTransaksi;
	}

	/**
	 * Menyetel jenis (nomor seri) jurnal bukti ini.
	 *
	 * @param jenisTransaksi jenis jurnal
	 */
	public void setJenisTransaksi(JenisTransaksi jenisTransaksi) {
		this.jenisTransaksi = jenisTransaksi;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui template ini.
	 *
	 * <p><b>Perhatikan tipenya:</b> ini relasi <code>&#64;ManyToOne</code> ke
	 * {@link ais.database.model.Tbmuser}, bukan bendera <code>boolean</code> — "disetujui"
	 * di sini berarti <i>siapa</i> penyetujunya; <code>null</code> berarti belum
	 * disetujui.</p>
	 *
	 * <p><b>Status verifikasi:</b> tidak ada satu pun kode di repositori yang menyetel
	 * maupun membaca properti ini, jadi kolom <code>disetujui</code> selalu
	 * <code>NULL</code>. Tidak ada pula alur kerja, batas nominal, atau larangan menyetujui
	 * pengajuan sendiri yang ditegakkan di tingkat entity. Bila fitur ini dihidupkan,
	 * gerbang persetujuan <b>wajib</b> dibuat di lapisan Action/Helper — pola
	 * persetujuan-diri-sendiri sudah berulang kali terkonfirmasi di modul finansial AIS,
	 * dan template jurnal jauh lebih berbahaya karena efeknya berulang.</p>
	 *
	 * @return pengguna penyetuju, atau <code>null</code> bila belum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui", nullable = true)
	public Tbmuser getDisetujui() {
		return disetujui;
	}

	/**
	 * Menyetel pengguna yang menyetujui template ini.
	 *
	 * <p>Tidak melakukan pemeriksaan hak apa pun — lihat catatan pada
	 * {@link #getDisetujui()}.</p>
	 *
	 * @param disetujui pengguna penyetuju
	 */
	public void setDisetujui(Tbmuser disetujui) {
		this.disetujui = disetujui;
	}

	/**
	 * Mengembalikan cap batch posting bukti ini.
	 *
	 * <p>Relasi <code>&#64;ManyToOne</code> ke
	 * {@link ais.database.model.akunting.PostingHistory} — satu baris cap dipakai bersama
	 * oleh banyak dokumen dalam satu batch posting. Keberadaan cap (bukan bendera boolean)
	 * itulah yang berarti "sudah diposting" pada modul jurnal sungguhan.</p>
	 *
	 * <p>Karena template bukanlah jurnal, kolom ini sebenarnya tidak bermakna di sini —
	 * ia ikut tersalin dari {@link ais.database.model.akunting.GrupTransaksi} dan tidak
	 * pernah diisi.</p>
	 *
	 * @return cap batch posting, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel cap batch posting bukti ini.
	 *
	 * @param postingHistory cap batch posting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan rujukan ke cicilan pembayaran yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber (lihat Javadoc kelas). Tidak pernah
	 * diisi karena entity ini tidur.</p>
	 *
	 * @return cicilan pembayaran sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "cicilan_pembayaran", nullable = true)
	public CicilanPembayaran getCicilanPembayaran() {
		return cicilanPembayaran;
	}

	/**
	 * Menyetel rujukan ke cicilan pembayaran yang menurunkan bukti ini.
	 *
	 * @param cicilanPembayaran cicilan pembayaran sumber
	 */
	public void setCicilanPembayaran(CicilanPembayaran cicilanPembayaran) {
		this.cicilanPembayaran = cicilanPembayaran;
	}

	/**
	 * Mengembalikan cuplikan HTML tabel jurnal yang tersimpan di kolom
	 * <code>deskripsi</code> (tipe <code>text</code>).
	 *
	 * <p>Isinya dirangkai dan <b>dipersistenkan</b> oleh {@link #populateDeskripsi()}; getter
	 * ini sendiri hanya membaca field.</p>
	 *
	 * <p><b>Catatan keamanan (relevan bila entity dihidupkan):</b> nilai kolom ini adalah
	 * HTML mentah yang dibangun dengan perangkaian string tanpa <i>escaping</i>, sehingga
	 * layar mana pun yang menampilkannya sebagai HTML akan mewarisi risiko XSS tersimpan
	 * dari keterangan baris jurnal. Lihat {@link #populateDeskripsi()}.</p>
	 *
	 * @return cuplikan HTML tabel jurnal, atau <code>null</code> bila belum pernah
	 *         dibangkitkan
	 */
	@Column(name = "deskripsi", columnDefinition = "text", nullable = true)
	public String getDeskripsi() {
		return deskripsi;
	}

	/**
	 * Menyetel cuplikan HTML tabel jurnal.
	 *
	 * <p>Dipanggil {@link #populateDeskripsi()} di akhir perangkaian (pemanggilan yang
	 * sebenarnya mubazir karena field sudah ditulis langsung sebelumnya).</p>
	 *
	 * @param deskripsi cuplikan HTML tabel jurnal
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Merangkai tabel HTML berisi baris debet/kredit bukti ini, menyimpannya ke
	 * {@link #getDeskripsi()}, lalu <b>mempersistenkannya ke basis data</b>.
	 *
	 * <h3>Alur</h3>
	 * <ol>
	 *   <li>Berhenti tanpa efek bila {@link #getId()} masih <code>null</code> (baris belum
	 *       tersimpan).</li>
	 *   <li>Mengambil sesi ZK/Hibernate aktif lewat
	 *       {@code HibernateUtil.currentSession()} dan memanggil
	 *       <code>session.refresh(this)</code> — memuat ulang seluruh properti dari basis
	 *       data, membuang perubahan yang belum di-<i>flush</i> pada objek ini.</li>
	 *   <li>Menjalankan Criteria untuk mengambil daftar baris, lalu merangkainya menjadi
	 *       tabel HTML empat kolom (Akun, Keterangan, Debet, Kredit) dengan nominal
	 *       diformat {@code Common.numberFormat} (locale Indonesia, <i>ThreadLocal</i>).</li>
	 *   <li>Menulis hasilnya ke field <code>deskripsi</code>, memanggil
	 *       {@link #setDeskripsi(String)} (mubazir, field sudah terisi), lalu
	 *       {@code Common.refreshUpdate(session, this)} yang melakukan
	 *       <code>saveOrUpdate</code> <b>plus flush</b> — jadi kolom <code>deskripsi</code>
	 *       benar-benar tertulis ke basis data dan revisi Envers baru diterbitkan.</li>
	 * </ol>
	 *
	 * <h3>BUG TERVERIFIKASI: menanyakan tabel yang salah</h3>
	 *
	 * <p>Criteria yang dijalankan adalah</p>
	 * <pre>session.createCriteria(Transaksi.class).add(Restrictions.eq("grupTransaksi", this))</pre>
	 * <p>yaitu atas {@link ais.database.model.akunting.Transaksi} — <b>baris jurnal
	 * sungguhan</b> — bukan atas {@link ais.database.model.akunting.TemplateTransaksi}.
	 * Nama properti yang benar untuk keluarga template pun berbeda
	 * (<code>templateGrupTransaksi</code>, bukan <code>grupTransaksi</code>). Ini sisa
	 * salin-tempel dari {@code GrupTransaksi.populateDeskripsi()} yang tidak ikut
	 * disesuaikan saat kelas template dibuat.</p>
	 *
	 * <p><b>Akibat bila method ini pernah dipanggil:</b> Hibernate mengikat sisi kanan
	 * <code>Restrictions.eq</code> pada relasi <code>&#64;ManyToOne</code> dengan
	 * <i>identifier</i> objek yang diberikan. Yang terkirim ke basis data karena itu adalah
	 * <code>WHERE transaksi.grup_transaksi = &lt;id baris template ini&gt;</code> — sebuah
	 * <b>tabrakan id lintas tabel</b>. Hasilnya bukan daftar kosong, melainkan baris jurnal
	 * resmi milik <code>grup_transaksi</code> yang kebetulan ber-id sama. Dua konsekuensi
	 * yang lebih serius menyusul:</p>
	 * <ul>
	 *   <li>tabel HTML yang tersimpan di {@link #getDeskripsi()} akan <b>membocorkan isi
	 *       jurnal resmi milik dokumen lain</b> — termasuk lintas tenant, karena entity ini
	 *       tidak punya kolom sekolah/yayasan sama sekali;</li>
	 *   <li>perangkaian memanggil <code>transaksi.getAkun()</code> atas baris-baris
	 *       <b>resmi</b> tersebut, dan getter itu bersifat <b>destruktif</b>: bila baris
	 *       memiliki <code>akunOver</code>, nilainya menimpa kolom <code>akun</code> secara
	 *       permanen (lihat {@link ais.database.model.akunting.Transaksi#getAkun()}). Jadi
	 *       sekadar "menampilkan deskripsi template" dapat <b>memindahkan atribusi akun buku
	 *       besar</b> pada jurnal resmi.</li>
	 * </ul>
	 *
	 * <p><b>Status saat ini: laten, bukan aktif.</b> Penelusuran menyeluruh menunjukkan
	 * method ini <b>tidak pernah dipanggil dari mana pun</b> — satu-satunya varian
	 * <code>populateDeskripsi*</code> yang benar-benar dipakai layar adalah
	 * {@code GrupTransaksi.populateDeskripsiLengkap()} (dipanggil dari
	 * {@code GrupTransaksiAction}, {@code TransaksiJurnalPenerimaanAction}, dan
	 * {@code TransaksiJurnalPengeluaranAction}). Bug di atas karena itu belum pernah
	 * menyala; ia wajib diperbaiki <b>sebelum</b> entity ini dihidupkan.</p>
	 *
	 * <h3>Risiko lain</h3>
	 * <ul>
	 *   <li><b>XSS tersimpan.</b> <code>transaksi.getKeterangan()</code> beserta kode dan
	 *       nama akun dirangkai ke HTML <b>tanpa <i>escaping</i></b>. Keterangan jurnal
	 *       berasal dari masukan pengguna, sehingga cuplikan yang tersimpan dapat memuat
	 *       markup aktif.</li>
	 *   <li><b>{@code NullPointerException}.</b> <code>transaksi.getAkun()</code> dapat
	 *       mengembalikan <code>null</code> (kolomnya opsional), dan hasilnya langsung
	 *       di-<code>getKode()</code> tanpa penjaga.</li>
	 *   <li><b>Mahal dan tidak aman untuk <i>renderer</i>.</b>
	 *       <code>session.refresh(this)</code> menerbitkan <code>SELECT</code> tambahan per
	 *       pemanggilan dan membuang perubahan yang belum di-<i>flush</i>; varian
	 *       {@code GrupTransaksi.populateDeskripsiLengkap()} sengaja tidak memakainya justru
	 *       karena alasan ini. Jangan memanggil method ini dari dalam <i>renderer</i> daftar
	 *       berisi banyak baris.</li>
	 *   <li><b>Tidak ada pemeriksaan keseimbangan.</b> Berbeda dari varian pada header
	 *       jurnal sungguhan yang mengembalikan penanda "TIDAK BALANCE", method ini
	 *       bertipe <code>void</code> dan tidak pernah membandingkan total debet dengan
	 *       total kredit.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping ringkas:</b> menerbitkan <code>SELECT</code> (refresh + Criteria),
	 * menulis field <code>deskripsi</code>, melakukan <code>saveOrUpdate</code> + flush atas
	 * entity ini, menerbitkan revisi Envers, dan berpotensi menerbitkan <code>UPDATE</code>
	 * atas baris {@link ais.database.model.akunting.Transaksi} lain lewat getter
	 * destruktifnya.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> tidak pernah — nol pemanggil di seluruh repositori
	 * (terverifikasi).</p>
	 */
	@SuppressWarnings("unchecked")
	public void populateDeskripsi() {
		if (getId() != null) {
			Session session = HibernateUtil.currentSession();
			session.refresh(this);
			List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
					.add(Restrictions.eq("grupTransaksi", this)).list();
			deskripsi = "<table style='width:100%;'>" + "<thead>";
			deskripsi += "<tr>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Akun</th>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Keterangan</th>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Debet</th>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Kredit</th>";
			deskripsi += "</tr>" + "</thead>" + "<tbody>";
			for (Transaksi transaksi : transaksis) {
				deskripsi += "<tr>";
				deskripsi += "<td style='border:solid;border-width: thin;'>" + transaksi.getAkun().getKode() + " - "
						+ transaksi.getAkun().getNama() + "</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' >" + transaksi.getKeterangan() + "</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
						+ Common.numberFormat.get().format(transaksi.getDebet()) + "</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
						+ Common.numberFormat.get().format(transaksi.getKredit()) + "</td>";
				deskripsi += "</tr>";
			}
			deskripsi += "</tbody></table>";
			setDeskripsi(deskripsi);
			Common.refreshUpdate(session, this);
		}
	}

	/**
	 * Mengembalikan satuan kerja pemilik bukti ini.
	 *
	 * <p>Relasi <code>&#64;ManyToOne</code> <code>LAZY</code> ke
	 * {@link ais.database.model.rab.SatuanKerja}.</p>
	 *
	 * <p><b>Efek samping (getter destruktif ringan):</b> field ditimpa dengan hasil
	 * <code>check(satuanKerja)</code>, helper dari
	 * {@link ais.database.model.GeneralValueObject} yang menukar proxy Hibernate yang belum
	 * terinisialisasi dengan instance sungguhan (memakai cache bila tersedia). Penulisan
	 * balik ini menghindari {@code LazyInitializationException} di luar sesi, tetapi berarti
	 * pembacaan properti dapat menyentuh basis data dan mengubah keadaan objek.</p>
	 *
	 * <p><b>Cakupan tenant:</b> ini satu-satunya pembatas ruang lingkup pada entity ini
	 * (tidak ada kolom sekolah maupun yayasan), dan <b>tidak pernah disetel dari mana pun</b>
	 * di repositori — de facto selalu <code>null</code>. Lihat catatan <i>fail-open</i>
	 * cakupan tenant pada Javadoc kelas.</p>
	 *
	 * @return satuan kerja pemilik, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik bukti ini.
	 *
	 * <p>Tidak ada pemanggil di repositori — lihat catatan pada {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja satuan kerja pemilik
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan rujukan ke pembayaran item gaji pegawai yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber, relasi <code>LAZY</code>.</p>
	 *
	 * <p><b>Efek samping (getter destruktif ringan):</b> seperti {@link #getSatuanKerja()},
	 * field ditimpa dengan hasil <code>check(&hellip;)</code>. Perhatikan tidak-konsistennya
	 * berkas ini: hanya dua dari sembilan relasi yang memakai <code>check(&hellip;)</code>,
	 * yaitu getter ini dan {@link #getSatuanKerja()} — tepat dua relasi yang ditandai
	 * <code>FetchType.LAZY</code>. Sisanya <i>eager</i> sehingga tidak memerlukannya.</p>
	 *
	 * @return pembayaran item gaji pegawai sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembayaran_item_gaji_pegawai", nullable = true)
	public PembayaranItemGajiPegawai getPembayaranItemGajiPegawai() {
		pembayaranItemGajiPegawai = check(pembayaranItemGajiPegawai);
		return pembayaranItemGajiPegawai;
	}

	/**
	 * Menyetel rujukan ke pembayaran item gaji pegawai yang menurunkan bukti ini.
	 *
	 * @param pembayaranItemGajiPegawai pembayaran item gaji pegawai sumber
	 */
	public void setPembayaranItemGajiPegawai(PembayaranItemGajiPegawai pembayaranItemGajiPegawai) {
		this.pembayaranItemGajiPegawai = pembayaranItemGajiPegawai;
	}

	/**
	 * Mengembalikan rujukan ke transaksi pegawai yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber. Tidak destruktif.</p>
	 *
	 * @return transaksi pegawai sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_pegawai", nullable = true)
	public TransaksiPegawai getTransaksiPegawai() {
		return transaksiPegawai;
	}

	/**
	 * Menyetel rujukan ke transaksi pegawai yang menurunkan bukti ini.
	 *
	 * @param transaksiPegawai transaksi pegawai sumber
	 */
	public void setTransaksiPegawai(TransaksiPegawai transaksiPegawai) {
		this.transaksiPegawai = transaksiPegawai;
	}

	/**
	 * Mengembalikan rujukan ke log pembayaran yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber. Tidak destruktif.</p>
	 *
	 * @return log pembayaran sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "log_pembayaran", nullable = true)
	public LogPembayaran getLogPembayaran() {
		return logPembayaran;
	}

	/**
	 * Menyetel rujukan ke log pembayaran yang menurunkan bukti ini.
	 *
	 * @param logPembayaran log pembayaran sumber
	 */
	public void setLogPembayaran(LogPembayaran logPembayaran) {
		this.logPembayaran = logPembayaran;
	}

	/**
	 * Mengembalikan rujukan ke detail pembayaran siswa yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber. Tidak destruktif.</p>
	 *
	 * @return detail pembayaran siswa sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_siswa_detail", nullable = true)
	public PembayaranSiswaDetail getPembayaranSiswaDetail() {
		return pembayaranSiswaDetail;
	}

	/**
	 * Menyetel rujukan ke detail pembayaran siswa yang menurunkan bukti ini.
	 *
	 * @param pembayaranSiswaDetail detail pembayaran siswa sumber
	 */
	public void setPembayaranSiswaDetail(PembayaranSiswaDetail pembayaranSiswaDetail) {
		this.pembayaranSiswaDetail = pembayaranSiswaDetail;
	}

	/**
	 * Mengembalikan rujukan ke deposit siswa yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber. Tidak destruktif.</p>
	 *
	 * @return deposit siswa sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "deposit_siswa", nullable = true)
	public DepositSiswa getDepositSiswa() {
		return depositSiswa;
	}

	/**
	 * Menyetel rujukan ke deposit siswa yang menurunkan bukti ini.
	 *
	 * @param depositSiswa deposit siswa sumber
	 */
	public void setDepositSiswa(DepositSiswa depositSiswa) {
		this.depositSiswa = depositSiswa;
	}

	/**
	 * Mengembalikan rujukan ke detail aset yang menurunkan bukti ini.
	 *
	 * <p>Satu dari tujuh kolom rujukan dokumen sumber (modul aset). Tidak destruktif.</p>
	 *
	 * @return detail aset sumber, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset_detail", nullable = true)
	public AssetDetail getAssetDetail() {
		return assetDetail;
	}

	/**
	 * Menyetel rujukan ke detail aset yang menurunkan bukti ini.
	 *
	 * @param assetDetail detail aset sumber
	 */
	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

}
