package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.TugasKelompok;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Master <b>Jenis Penilaian</b> &mdash; <i>profil</i> atau <i>skema</i> penilaian tingkat
 * <b>PALING ATAS</b> pada modul Sekolah. Satu baris di sini adalah sebuah nama skema (misalnya
 * "Kurikulum Merdeka", "KTSP", "Penilaian Harian") milik satu {@link Sekolah}, dan seluruh susunan
 * kolom nilai yang muncul pada formulir input nilai, rapor, rekap, serta API mobile diturunkan dari
 * baris ini.
 *
 * <h2>Pemetaan</h2>
 *
 * <ul>
 *   <li>Tabel <b>{@code sekolah.jenis_penilaian}</b>; terdaftar di {@code hibernate.cfg.xml}
 *       sebagai {@code <mapping class="ais.database.model.sekolah.JenisPenilaian"/>} (pemetaan
 *       murni anotasi, tidak ada berkas {@code .hbm.xml} pendamping).</li>
 *   <li>{@code @Audited} (Envers) dan {@code dynamicInsert}/{@code dynamicUpdate}.</li>
 *   <li><b>Akses properti</b> (anotasi diletakkan pada <i>getter</i>), sehingga setiap getter yang
 *       menulis balik ke field ikut menentukan nilai yang benar-benar disimpan ke basis data
 *       &mdash; lihat {@link #getYayasan()} dan {@link #getNama()}.</li>
 *   <li>Kolom yang benar-benar ada: {@code id}, {@code sekolah_id} ({@code nullable = false}),
 *       {@code yayasan_id}, {@code jenis} ({@code nullable = false}), {@code aktif},
 *       {@code oleh}, {@code olehId}, {@code tanggal_dirubah}. <b>Tidak ada</b> kolom
 *       {@code keterangan} maupun {@code nomorUrut} (lihat "Bug keterangan" di bawah).</li>
 * </ul>
 *
 * <h2>Posisi dalam rantai penilaian siswa (terverifikasi dari kode)</h2>
 *
 * <p>Kelas ini adalah <b>PUNCAK</b> rantai konfigurasi penilaian. Arah setiap anak panah di bawah
 * diverifikasi dari nama {@code @JoinColumn} pada entity penghubungnya, bukan diasumsikan dari
 * nama kelas:</p>
 *
 * <pre>
 *   <b>JenisPenilaian &mdash; KELAS INI</b>          (master puncak; sekolah.jenis_penilaian)
 *        &darr;  {@link DetailJenisPenilaian}                  (jenis_penilaian_id, grup_penilaian_siswa_id)
 *   {@link GrupPenilaian}                             (pemilik formula, jenisNilaiHuruf, khususTingkat/Semester)
 *        &darr;  {@link DetailGrupPenilaian}                   (grup_penilaian_id, grup_kategori_item_penilaian_siswa)
 *   {@link GrupKategoriItemPenilaianSiswa}            (master; kode/nama/formula/khususTingkat)
 *        &darr;  {@link DetailGrupKategoriItemPenilaianSiswa}  (grup_kategori_item_penilaian_siswa, kategori_item_penilaian_siswa)
 *   {@link KategoriItemPenilaianSiswa}                (master)
 *        &darr;  (FK langsung, bukan tabel penghubung)
 *   {@link JenisItemPenilaianSiswa}                   (butir nilai terkecil; kode-nya dipakai formula)
 * </pre>
 *
 * <p>Karena berada di puncak, dampak perubahan satu baris di sini <b>berlipat ke bawah</b>:
 * melepas satu {@link GrupPenilaian} dari sebuah Jenis Penilaian menghilangkan seluruh kolom nilai
 * turunan grup itu dari <b>semua</b> mata pelajaran yang memakai jenis penilaian tersebut,
 * sekaligus di formulir input nilai, rapor, rekap, dan API.</p>
 *
 * <h2>Siapa yang menunjuk baris ini</h2>
 *
 * <p>Hanya ada dua FK masuk, keduanya {@code @ManyToOne} LAZY ke kolom {@code jenis_penilaian_id}:</p>
 *
 * <ol>
 *   <li>{@link Matapelajaran#getJenisPenilaian()} &mdash; skema <b>dasar</b> per mata pelajaran.</li>
 *   <li>{@link KurikulumSekolah#getJenisPenilaian()} &mdash; <b>penimpa</b> (<i>override</i>) di
 *       tingkat kurikulum; {@code null} berarti "ikuti jenis penilaian matapelajaran" (persis
 *       seperti label combobox di layar master kurikulum).</li>
 * </ol>
 *
 * <p>Aturan penggabungan keduanya <b>disalin-tempel di sekitar 25 titik</b> dengan bentuk yang
 * selalu sama:</p>
 *
 * <pre>
 * JenisPenilaian jenisPenilaian = kpm.getMatapelajaran().getJenisPenilaian();
 * if (kpm != null &amp;&amp; kpm.getKurikulumSekolah() != null
 *         &amp;&amp; kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
 *     jenisPenilaian = kpm.getKurikulumSekolah().getJenisPenilaian();
 * }
 * </pre>
 *
 * <p>Pemakainya antara lain {@code ais.action.report.format1.sekolah.LaporanRekapTotalNilai},
 * {@code ais.action.report.format1.sekolah.LaporanRaporSiswa},
 * {@code ais.action.master.sekolah.PenilaianSiswaAction},
 * {@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper} dan
 * {@code DetailPenilaianLesSiswaHelper}, {@code PertemuanPunyaUjianSiswaHelper},
 * {@code TampilStudiSiswaHelper}, {@code ais.action.master.helper.TugasMandiriHelper} dan
 * {@code TugasKelompokHelper}, serta {@code ais.action.servlet.api.NilaiSiswaApi} dan
 * {@code ElearningApiUtil}. Bentuk kanoniknya sebenarnya sudah tersedia di
 * {@code ais.common.CommonUiFactoryHelper#getDetailJenisPenilaians(JadwalPelajaran)} &mdash;
 * <b>tetapi helper itu rusak permanen</b> (memakai {@code createAlias("jenisItemPenilaianSiswa", ...)}
 * padahal {@link DetailJenisPenilaian} tidak punya properti bernama itu; {@code QueryException}
 * ditelan {@code catch} sehingga hasilnya selalu list kosong). Untunglah tidak ada satu pun
 * pemanggilnya, jadi kode itu mati &mdash; tetapi jangan dipakai sebelum diperbaiki.</p>
 *
 * <h2>Tidak ada koleksi balik (verifikasi eksplisit)</h2>
 *
 * <p>Kelas ini <b>tidak memiliki</b> {@code @OneToMany} apa pun &mdash; tidak ada field/koleksi
 * {@code detailJenisPenilaians}, {@code grupPenilaians}, {@code matapelajarans}, dan seterusnya.
 * Seluruh navigasi turun ke {@link DetailJenisPenilaian} dilakukan dengan {@code Criteria}
 * eksplisit di sisi pemanggil, selalu berbentuk
 * {@code eq("jenisPenilaian", jenisPenilaian)} + filter {@code aktif} toleran-NULL
 * ({@code isNull("aktif") OR eq("aktif", true)}) + biasanya
 * {@code setProjection(Projections.groupProperty("grupPenilaian.id"))}. Konsekuensi praktis: tidak
 * ada {@code cascade}, tidak ada {@code orphanRemoval}, dan tidak ada mekanisme apa pun di kelas
 * ini yang bisa melindungi atau memulihkan baris detail bila layar master menghapusnya secara
 * logis (lihat "Bom waktu" di bawah).</p>
 *
 * <h2>Layar master, menu, dan pewarisan hak akses</h2>
 *
 * <p>Satu-satunya layar pengelola adalah {@code /pages/master/sekolah/jenis_penilaian.zul} yang
 * dikendalikan {@code ais.action.master.sekolah.JenisPenilaianAction}, terdaftar di
 * {@code ais.common.MenuInitializer} sebagai menu <b>id 881229 "Jenis Penilaian"</b>
 * (induk 570007, modul 5700).</p>
 *
 * <p><b>Temuan pewarisan hak menu &mdash; diverifikasi ulang dari sisi berkas ini.</b> Berkas ZUL
 * layar ini berisi <b>8 tab</b>: satu tab milik entity ini sendiri, ditambah <b>7 tab yang
 * menyuntikkan halaman lain</b> lewat {@code MyInclude} pada handler {@code onGrupPenilaian},
 * {@code onGrupKategori}, {@code onKategoriPenilaian}, {@code onItemPenilaian},
 * {@code onNilaiHuruf}, {@code onJenisNilaiHuruf}, dan {@code onKonstanta}:</p>
 *
 * <ol>
 *   <li>{@code /pages/master/sekolah/grup_penilaian.zul}</li>
 *   <li>{@code /pages/master/sekolah/grup_kategori_item_penilaian_siswa.zul}</li>
 *   <li>{@code /pages/master/sekolah/kategori_item_penilaian_siswa.zul}</li>
 *   <li>{@code /pages/master/sekolah/jenis_item_penilaian_siswa.zul}</li>
 *   <li>{@code /pages/master/sekolah/nilai_huruf_sekolah.zul}</li>
 *   <li>{@code /pages/master/sekolah/jenis_nilai_huruf.zul}</li>
 *   <li><b>{@code /pages/master/konstanta.zul}</b> &mdash; perhatikan: <b>bukan</b> halaman modul
 *       sekolah.</li>
 * </ol>
 *
 * <p>Mekanismenya: {@code CommonPrivilages.checkPrevilages(kode)} menyelesaikan hak akses terhadap
 * {@code Common.getCurrentMenu()}, yaitu <b>menu yang sedang dibuka</b> &mdash; bukan halaman ZUL
 * yang sedang di-{@code include}. Halaman yang disuntikkan sebagai tab karena itu dinilai memakai
 * hak akses menu 881229. Butir ke-7 adalah yang paling luas akibatnya: {@code konstanta.zul}
 * dikendalikan {@code ais.action.master.KonstantaAction} atas entity
 * {@code ais.database.model.Konstanta} yang dipetakan ke <b>{@code public.konstanta}</b> dan
 * <b>sama sekali tidak punya kolom {@code sekolah}/{@code yayasan}</b> (diperiksa langsung pada
 * kelas entity-nya). Dengan kata lain: <b>hak ubah katalog penilaian SATU sekolah dengan
 * sendirinya memberi hak CRUD atas tabel konstanta GLOBAL seluruh instalasi.</b> Ini eskalasi hak
 * terluas yang tercatat pada pola "pewarisan hak lewat menu induk".</p>
 *
 * <h2>Bom waktu: seluruh pemetaan bisa lenyap dalam satu klik Simpan</h2>
 *
 * <p>Diverifikasi ulang dari sisi entity ini. {@code JenisPenilaianAction#onSave(Event)} menyimpan
 * baris {@code JenisPenilaian} lalu <b>mematikan SELURUH</b> baris {@link DetailJenisPenilaian}
 * yang menunjuk baris ini ({@code setAktif(false)} satu per satu, masing-masing di-{@code flush()}),
 * kemudian <b>menghidupkan kembali hanya</b> yang ada di peta
 * {@code selectedJenisItemPenilaianSiswa}. Peta itu dibuat <b>kosong</b> di {@code init(...)} dan
 * baru diisi oleh listener {@code ubahJenisPenialain}, yang selain dipasang pada {@code onChange}
 * kombo Yayasan/Sekolah juga dijadwalkan sekali lewat {@code Common.createDefaultTimer(...)}
 * &rarr; {@code CommonTimerHelper} &rarr; ZK {@code Timer} <b>50&nbsp;ms</b>.</p>
 *
 * <p>Bila tombol <i>Simpan</i> sempat ditekan sebelum timer 50&nbsp;ms itu berjalan &mdash; atau
 * bila listener gagal di tengah jalan, misalnya {@code session.refresh(jenisPenilaian)} melempar
 * &mdash; maka langkah "matikan semua" tetap dijalankan sedangkan langkah "hidupkan yang
 * tercentang" mengiterasi peta kosong (peta tidak pernah {@code null}, sehingga penjaga
 * {@code != null} di {@code onSave} tidak menolong). <b>Seluruh pemetaan Jenis Penilaian &rarr;
 * Grup Penilaian hilang sekaligus, permanen, tanpa pesan kesalahan apa pun.</b> Karena entity ini
 * adalah puncak rantai, yang lenyap bukan satu kolom nilai melainkan <b>seluruh isi rapor</b>
 * untuk setiap mata pelajaran yang memakai jenis penilaian tersebut.</p>
 *
 * <p>Dari sisi berkas ini tidak ada satu pun penangkal: tidak ada koleksi ber-{@code cascade},
 * tidak ada {@code @PreUpdate}/{@code @PreRemove} yang memvalidasi, dan tidak ada penanda versi.
 * Satu-satunya jejak yang tersisa adalah riwayat Envers pada {@code DetailJenisPenilaian} &mdash;
 * yang justru sangat berisik karena setiap penyimpanan menghasilkan hingga 2&times;N revisi.</p>
 *
 * <h2>Bug: kolom "Keterangan" pada layar ini tidak pernah tersimpan</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan</b> properti apa pun
 * miliknya. Kelas ini mendeklarasikan ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} justru karena keharusan teknis itu (bukan bug &mdash; lihat
 * {@link GeneralValueObject}), <b>tetapi tidak mendeklarasikan ulang {@code keterangan}</b>.</p>
 *
 * <p>Akibatnya {@code keterangan} pada entity ini hanyalah field POJO yang hidup selama satu
 * request: {@code JenisPenilaianAction#onSave} memanggil
 * {@code jenisPenilaian.setKeterangan(keterangan.getValue())}, nilainya tidak pernah sampai ke
 * INSERT/UPDATE, dan renderer grid membacanya kembali dari objek yang baru dimuat sehingga
 * <b>kolom "Keterangan" di layar Jenis Penilaian selalu kosong</b>. Hal yang sama berlaku untuk
 * ekspor/impor generik: {@code Common.cetakData}/{@code Common.uploadData} di layar ini memakai
 * daftar properti {@code {"id", "jenis", "sekolah", "keterangan"}}, sehingga kolom keterangan pada
 * berkas Excel selalu kosong saat diekspor dan diam-diam diabaikan saat diunggah. Tidak ada NPE
 * karena {@link GeneralValueObject#getKeterangan()} mengembalikan {@code ""} (bukan {@code null})
 * &mdash; normalisasi itulah yang menutupi masalahnya. Pola identik pernah dicatat pada
 * {@code StatusAwalSiswa}.</p>
 *
 * <h2>Hasil pemeriksaan pola berulang lain</h2>
 *
 * <ul>
 *   <li><b>Getter yang menulis balik (<i>write-back</i>) &mdash; ADA, dua buah.</b>
 *       {@link #getNama()} menimpa field {@code nama} dengan {@code jenis} setiap kali dipanggil
 *       (sehingga {@link #setNama(String)} praktis tak berpengaruh), dan {@link #getYayasan()}
 *       menimpa field {@code yayasan} dengan {@code sekolah.getYayasan()} (sehingga
 *       {@link #setYayasan(Yayasan)} praktis tak berpengaruh selama {@code sekolah} terisi).
 *       Keduanya <b>tidak destruktif ke baris lain</b> &mdash; berbeda dengan varian
 *       {@code GrupChecklistPenilaianGuru} yang menulis balik FK ke baris sembarang &mdash; tetapi
 *       karena Hibernate memakai akses properti, nilai hasil penimpaan itulah yang benar-benar
 *       ditulis ke kolom {@code yayasan_id}.</li>
 *   <li><b>Penciutan {@code TreeSet}/{@code compareTo} &mdash; TIDAK ADA.</b> Kelas ini tidak
 *       meng-override {@code getNomorUrut()}, tidak ada {@code TreeSet<JenisPenilaian>} atau
 *       {@code Collections.sort} atas daftar entity ini di seluruh repo, dan
 *       {@code ConstantValues.simpleList(...)} menyaring duplikat dengan
 *       {@code ArrayList.contains(...)} yang memakai {@link GeneralValueObject#equals(Object)}
 *       (berbasis {@code id}), bukan {@code compareTo}. Catatan untuk masa depan: bila suatu saat
 *       daftar entity ini dimasukkan ke koleksi terurut, penciutan <b>akan</b> terjadi &mdash;
 *       {@code compareTo} induk jatuh ke kunci ketiga ({@code nama}), dan {@code nama} di sini
 *       adalah {@code jenis}, yang lazim sama persis antar sekolah ("Kurikulum Merdeka" dsb.).</li>
 *   <li><b>SQL injection lewat nama katalog &rarr; alias kolom native SQL &mdash; TIDAK ADA.</b>
 *       Tidak ada dasbor native SQL yang menyisipkan {@code getJenis()} ke dalam string query;
 *       seluruh pembaca memakai {@code Criteria} berparameter.</li>
 *   <li><b>Fail-open cakupan tenant pada jalur baca &mdash; ADA (observasi mentah).</b>
 *       {@code JenisPenilaianAction#initCriteria(boolean)} tidak memasang <i>baseline</i> filter
 *       tenant apa pun di sisi server: filter sekolah/yayasan hanya terpasang bila combo pencarian
 *       punya item terpilih, selain itu jatuh ke {@code Restrictions.sqlRestriction("1=1")}.
 *       {@code Common.initYayasanDanSekolahDanSemua(...)} memang memilih dan me-{@code disable}
 *       combo untuk pengguna non-admin, tetapi itu penegakan di sisi komponen, bukan di query
 *       &mdash; untuk akun tanpa keterikatan sekolah/yayasan (pola yang sudah tercatat pada
 *       {@code AsramaSiswa}/{@code PembinaSiswa}) daftar merosot menjadi seluruh instalasi.
 *       Demikian pula {@code onSave} menerima sekolah/yayasan apa pun yang terkirim dari combo
 *       tanpa memverifikasi ulang kepemilikannya.</li>
 *   <li><b>Kolom {@code aktif} tidak pernah ditulis eksplisit oleh {@code onSave} &mdash;
 *       observasi mentah.</b> {@code JenisPenilaianAction#onSave} tidak pernah memanggil
 *       {@link #setAktif(Boolean)}; nilai baru hanya diset lewat checkbox "Aktif" di renderer grid.
 *       Untuk baris baru, {@link #getAktif()} mengembalikan {@code true} saat {@code aktif} masih
 *       {@code null}. Karena pemetaan memakai akses properti, nilai hasil <i>coalesce</i> itulah
 *       yang dibaca Hibernate saat INSERT. Dicatat sebagai observasi &mdash; nilai kolom yang
 *       sebenarnya di basis data perlu diverifikasi empiris sebelum dijadikan dasar perbaikan.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Identitas &amp; jejak audit</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 *       {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Cakupan tenant</b> &mdash; {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Isi bisnis</b> &mdash; {@link #getJenis()}/{@link #setJenis(String)},
 *       {@link #getNama()}/{@link #setNama(String)}, {@link #getAktif()}/{@link #setAktif(Boolean)},
 *       {@link #toString()}.</li>
 *   <li><b>Konstruktor</b> &mdash; {@link #JenisPenilaian()},
 *       {@link #JenisPenilaian(long, Sekolah, String)}.</li>
 *   <li><b>Utilitas UI/laporan (kode mati)</b> &mdash;
 *       {@link #hitungNilaiBerdasarkanDetailGrupPenilaian(JadwalPelajaran, DetailGrupPenilaian)}.</li>
 * </ol>
 *
 * <h2>Catatan historis</h2>
 *
 * <p>Javadoc kelas ini sebelumnya hanya berisi satu baris penanda generator,
 * <i>"JenisPenilaian generated by hbm2java"</i>, sepadan dengan komentar generator pada baris
 * kedua berkas ({@code // Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final}) yang
 * sengaja dipertahankan. Isi berkas sudah lama menyimpang dari hasil generator &mdash; getter
 * yang menulis balik, penjagaan {@code null} pada setter, kait {@code @PreUpdate}, dan method
 * statis {@link #hitungNilaiBerdasarkanDetailGrupPenilaian(JadwalPelajaran, DetailGrupPenilaian)}
 * semuanya ditambahkan manual &mdash; jadi berkas ini <b>tidak boleh</b> di-generate ulang.</p>
 *
 * @see GeneralValueObject
 * @see DetailJenisPenilaian
 * @see GrupPenilaian
 * @see Matapelajaran
 * @see KurikulumSekolah
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "jenis_penilaian", schema = "sekolah")
public class JenisPenilaian extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi. Instance entity ikut diserialkan bersama state desktop ZK, jadi
	 * nilai ini harus tetap sama selama bentuk field tidak berubah.
	 */
	private static final long serialVersionUID = -8817799955174105108L;

	/** Kunci utama, kolom {@code id}; di-generate basis data ({@code IDENTITY}). */
	private Long id;

	/**
	 * Nama tampilan pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private String oleh;

	/**
	 * Identitas (login id) pengguna terakhir yang mengubah baris ini; pasangan teknis dari
	 * {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * @return login id pengubah terakhir, atau {@code null} bila baris belum pernah diubah lewat
	 *         jalur yang mengisi jejak audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah, dengan <b>penjagaan</b>: nilai {@code null} atau yang
	 * hanya berisi spasi <b>diabaikan</b> (method langsung {@code return} tanpa menyentuh field),
	 * sehingga jejak audit lama tidak pernah terhapus oleh penulisan kosong.
	 *
	 * @param olehId login id pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pengguna pengubah, dengan penjagaan yang sama seperti
	 * {@link #setOlehId(String)}: {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: dipanggil Hibernate tepat sebelum UPDATE dan meneruskan entity
	 * ke {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, yang mengisi
	 * {@link #oleh}/{@link #olehId} dari pengguna aktif dan menyegarkan
	 * {@link #tanggal_dirubah}.
	 *
	 * <p><b>Catatan bentuk:</b> deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris
	 * yang sama dengan method ini (bentuk warisan generator). Nilai awalnya
	 * {@code ais.ui.util.WaktuUtil.getDate()}, sehingga baris baru sudah bertanggal sejak objek
	 * dibuat &mdash; bukan sejak disimpan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; dipanggil terutama oleh
	 * {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai
	 * {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang dibuat lewat
	 *         konstruktor karena field-nya sudah diinisialisasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik skema penilaian ini; kolom {@code sekolah_id}, wajib. */
	private Sekolah sekolah;

	/** Nama skema penilaian; kolom {@code jenis}, wajib. Satu-satunya isi bisnis kelas ini. */
	private String jenis;

	/** Penanda aktif; kolom {@code aktif}. {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/**
	 * Yayasan pemilik; kolom {@code yayasan_id}. Nilainya <b>turunan</b> dari
	 * {@link #sekolah} &mdash; lihat {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Cermin baca-saja dari {@link #jenis}. Bukan kolom tersendiri: dipetakan ke kolom
	 * <b>{@code jenis} yang sama</b> dengan {@code insertable = false, updatable = false}.
	 */
	private String nama;

	/**
	 * Mengembalikan nama entity untuk keperluan generik &mdash; <b>selalu sama dengan
	 * {@link #getJenis()}</b>.
	 *
	 * <p><b>Dua hal non-obvious:</b></p>
	 * <ol>
	 *   <li><b>Pemetaan ganda kolom.</b> Anotasi {@code @Column(name = "jenis")} di sini menunjuk
	 *       kolom yang sama dengan {@link #getJenis()}, tetapi ditandai
	 *       {@code insertable = false, updatable = false} sehingga hanya arah baca yang dipetakan.
	 *       Gunanya: memberi properti bernama {@code nama} yang bisa dipakai HQL/Criteria dan
	 *       pengurutan ZK &mdash; kolom grid pada {@code jenis_penilaian.zul} memakai
	 *       {@code sort="auto(nama)"}. {@code nullable = false} pada anotasi ini hanya relevan
	 *       untuk DDL, bukan validasi runtime.</li>
	 *   <li><b>Getter menulis balik.</b> Method ini <b>menimpa</b> field {@code nama} pada setiap
	 *       pemanggilan, sehingga apa pun yang pernah disetel lewat {@link #setNama(String)} akan
	 *       hilang pada pembacaan berikutnya. Ini juga membuat kunci urut ketiga
	 *       {@link GeneralValueObject#compareTo(GeneralValueObject)} selalu berisi teks
	 *       {@code jenis}, dan {@code toString()} induk (bila dipakai) konsisten dengan
	 *       {@link #toString()} kelas ini.</li>
	 * </ol>
	 *
	 * @return nama skema penilaian (isi kolom {@code jenis}); tidak pernah {@code null} pada baris
	 *         tersimpan karena kolomnya {@code nullable = false}
	 */
	@Column(name = "jenis", nullable = false, insertable = false, updatable = false)
	public String getNama() {
		nama = getJenis();
		return nama;
	}

	/**
	 * Menyetel field {@code nama}. <b>Praktis tidak berguna</b>: nilainya tidak pernah disimpan
	 * (properti ini {@code insertable = false, updatable = false}) dan akan segera ditimpa oleh
	 * {@link #getNama()} pada pembacaan berikutnya. Tetap disediakan agar pasangan getter/setter
	 * lengkap, yang dibutuhkan Hibernate (akses properti) dan utilitas reflektif seperti
	 * {@code Common.uploadData}.
	 *
	 * @param nama diabaikan secara efektif
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Representasi teks entity: nama skema penilaian apa adanya.
	 *
	 * <p>Membaca <b>field</b> {@code jenis} secara langsung, bukan lewat {@link #getJenis()}.
	 * Untuk properti sederhana (bukan asosiasi) perbedaannya tidak berarti, tetapi perhatikan pada
	 * <i>proxy</i> Hibernate yang belum ter-inisialisasi hasilnya bisa {@code null} sementara
	 * getter akan memicu pemuatan.</p>
	 *
	 * @return isi kolom {@code jenis}, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return jenis;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA dan dipakai layar master saat menekan
	 * tombol Tambah ({@code JenisPenilaianAction#onAdd(Event)} memanggil
	 * {@code init(new JenisPenilaian())}).
	 */
	public JenisPenilaian() {
	}

	/**
	 * Konstruktor lengkap warisan generator hbm2java. <b>Tidak dipakai di mana pun</b> pada kode
	 * aplikasi (satu-satunya pembuatan instance adalah {@link #JenisPenilaian()} di
	 * {@code JenisPenilaianAction#onAdd}).
	 *
	 * <p>Dua kuirk yang perlu diketahui bila suatu saat dipakai:</p>
	 * <ul>
	 *   <li>Parameter {@code id} bertipe {@code long} primitif dan disetel langsung, padahal kunci
	 *       utama di-generate {@code IDENTITY} &mdash; menyimpan objek hasil konstruktor ini
	 *       sebagai baris baru bukan alur yang didukung.</li>
	 *   <li>{@code sekolah} melewati penjagaan yang sama dengan {@link #setSekolah(Sekolah)}:
	 *       objek {@code Sekolah} yang belum punya {@code id} (mis. hasil {@code new}) diperlakukan
	 *       sebagai {@code null} agar {@code cascade PERSIST} tidak menyisipkan sekolah hantu.</li>
	 * </ul>
	 *
	 * @param id      nilai kunci utama yang dipaksakan
	 * @param sekolah sekolah pemilik; objek tanpa {@code id} disimpan sebagai {@code null}
	 * @param jenis   nama skema penilaian
	 */
	public JenisPenilaian(long id, Sekolah sekolah, String jenis) {
		this.id = id;
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} ditandai {@code insertable = false} karena nilainya di-generate basis
	 * data ({@code IDENTITY}). Nilai inilah yang dipakai seluruh pembaca rantai penilaian sebagai
	 * kunci pencarian {@link DetailJenisPenilaian} ({@code eq("jenisPenilaian", ...)}), dan yang
	 * dipakai {@link GeneralValueObject#equals(Object)} untuk kesamaan entity.</p>
	 *
	 * @return kunci utama; {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Tanpa validasi; disediakan untuk Hibernate dan utilitas reflektif.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik skema penilaian ini &mdash; satu-satunya penentu cakupan
	 * tenant entity ini.
	 *
	 * <p>Relasi {@code @ManyToOne} LAZY ke kolom {@code sekolah_id} yang {@code nullable = false},
	 * dengan {@code cascade = {PERSIST, MERGE}} (tanpa {@code REMOVE}, sehingga menghapus jenis
	 * penilaian tidak pernah menyentuh baris sekolah). Sebelum dikembalikan, proxy dilewatkan ke
	 * {@link GeneralValueObject#check(Object)} yang memulihkan/menetralkan referensi tak valid,
	 * sehingga pemanggil tidak menerima {@code LazyInitializationException} untuk kasus umum.</p>
	 *
	 * <p>Perhatikan bahwa {@link DetailJenisPenilaian} dan seluruh simpul rantai di bawahnya
	 * <b>mewarisi</b> cakupan tenant dari sini &mdash; mereka tidak punya kolom sekolah sendiri
	 * (kecuali {@link GrupPenilaian}, yang punya kolom tenant terpisah sehingga satu baris
	 * penghubung bisa menjembatani dua tenant berbeda).</p>
	 *
	 * @return sekolah pemilik; {@code null} hanya pada objek yang belum tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan penjagaan: objek {@code Sekolah} yang belum punya
	 * {@code id} (mis. hasil {@code new Sekolah()} dari combobox kosong) disimpan sebagai
	 * {@code null}, agar {@code cascade PERSIST} tidak menyisipkan baris sekolah hantu ke
	 * {@code sekolah.sekolah}.
	 *
	 * <p>Perhatikan efek sampingnya pada {@link #getYayasan()}: karena yayasan diturunkan dari
	 * sekolah, mengganti sekolah otomatis mengganti yayasan yang tersimpan pada penyimpanan
	 * berikutnya.</p>
	 *
	 * @param sekolah sekolah pemilik; objek tanpa {@code id} diperlakukan {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik &mdash; <b>nilai turunan, bukan nilai yang disimpan
	 * pemanggil</b>.
	 *
	 * <p>Method ini <b>menulis balik dua field</b> setiap kali dipanggil: {@code sekolah} diisi
	 * ulang dari {@link #getSekolah()}, lalu &mdash; bila sekolah ada &mdash; {@code yayasan}
	 * <b>ditimpa</b> dengan {@code sekolah.getYayasan()}. Konsekuensinya:</p>
	 *
	 * <ul>
	 *   <li>{@link #setYayasan(Yayasan)} praktis tidak berpengaruh selama {@code sekolah} terisi;
	 *       nilai yang disetel akan tertimpa pada pembacaan berikutnya.</li>
	 *   <li>Karena pemetaan memakai akses properti, <b>nilai turunan itulah yang benar-benar
	 *       ditulis ke kolom {@code yayasan_id}</b> saat INSERT/UPDATE. Kolom {@code yayasan_id}
	 *       karena itu selalu konsisten dengan yayasan milik sekolah &mdash; dan akan ikut berubah
	 *       diam-diam bila sekolah dipindahkan ke yayasan lain, pada penyimpanan berikutnya.</li>
	 *   <li>Layar master sudah menyesuaikan diri dengan perilaku ini: combobox "Yayasan" pada
	 *       jendela Tambah/Ubah disetel {@code setReadonly(true)} dan hanya mengikuti pilihan
	 *       sekolah.</li>
	 * </ul>
	 *
	 * <p>Berbeda dengan {@code sekolah_id}, kolom {@code yayasan_id} <b>boleh {@code null}</b>
	 * &mdash; nilainya akan {@code null} bila sekolah pemiliknya sendiri belum berinduk yayasan.
	 * Filter pencarian pada layar master menyaring kolom ini secara terpisah, jadi baris ber-yayasan
	 * {@code null} bisa tidak muncul saat pengguna memilih sebuah yayasan.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah), atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik, dengan penjagaan objek tanpa {@code id} sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * <p><b>Efeknya sangat terbatas</b>: {@link #getYayasan()} akan menimpa nilai ini dengan
	 * yayasan milik sekolah pada pembacaan berikutnya, sehingga setter ini hanya berarti untuk
	 * objek yang {@code sekolah}-nya masih {@code null}. Dipanggil oleh
	 * {@code JenisPenilaianAction#onSave} dengan nilai dari combobox, tetapi hasilnya tetap
	 * ditentukan sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; objek tanpa {@code id} diperlakukan {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nama skema penilaian &mdash; satu-satunya isi bisnis entity ini.
	 *
	 * <p>Kolom {@code jenis}, {@code nullable = false}. Nilainya adalah teks bebas yang diketik
	 * pengguna pada isian "Nama Jenis Penilaian *" (mis. "Kurikulum Merdeka", "KTSP"); tidak ada
	 * daftar nilai terbatas, tidak ada kode standar (Feeder/PDDikti), dan <b>tidak ada unique
	 * constraint</b> &mdash; dua sekolah (bahkan satu sekolah) boleh punya nama yang sama.</p>
	 *
	 * <p>Nilai ini juga dipakai sebagai label pada seluruh combobox pemilih jenis penilaian
	 * ({@link #toString()}), sebagai kunci urut alami ({@link #getNama()}), dan sebagai judul
	 * riwayat revisi Envers di renderer grid
	 * ({@code RevisiHelper.createNewRevisi(JenisPenilaian.class, obj, obj.getJenis())}).</p>
	 *
	 * @return nama skema penilaian; tidak pernah {@code null} pada baris tersimpan
	 */
	@Column(name = "jenis", nullable = false)
	public String getJenis() {
		return this.jenis;
	}

	/**
	 * Menyetel nama skema penilaian. Tanpa validasi di sini &mdash; pengecekan "tidak boleh
	 * kosong" dilakukan {@code JenisPenilaianAction#onSave(Event)} sebelum setter ini dipanggil.
	 *
	 * @param jenis nama skema penilaian
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan status aktif, dengan <b>normalisasi {@code null} menjadi {@code true}</b>.
	 *
	 * <p>Konvensi ini seragam di seluruh modul: baris lama yang kolom {@code aktif}-nya masih
	 * {@code NULL} dianggap aktif, dan seluruh query pembaca memakai filter toleran-NULL
	 * {@code isNull("aktif") OR eq("aktif", true)} agar konsisten dengan getter ini.</p>
	 *
	 * <p><b>Catatan mekanisme (observasi, perlu verifikasi empiris):</b> karena pemetaan memakai
	 * akses properti, nilai yang dibaca Hibernate saat INSERT adalah hasil normalisasi ini &mdash;
	 * artinya baris yang dibuat lewat {@code onSave()} normal semestinya tersimpan
	 * {@code aktif = true} walaupun {@code onSave()} tidak pernah memanggil
	 * {@link #setAktif(Boolean)}. Baris yang masuk lewat SQL mentah/migrasi tetap bisa
	 * {@code NULL} di basis data; itu tidak menjadi masalah selama pembacanya memakai filter
	 * toleran-NULL di atas.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif (<i>soft delete</i>).
	 *
	 * <p>Satu-satunya pemanggil di layar master adalah checkbox "Aktif" pada renderer grid, yang
	 * langsung memanggil {@code Common.refreshSaveOrUpdate(jenisPenilaian)} setelahnya &mdash;
	 * perubahan tersimpan seketika tanpa konfirmasi. Menonaktifkan sebuah Jenis Penilaian
	 * <b>tidak</b> menyentuh baris {@link DetailJenisPenilaian} di bawahnya (tidak ada
	 * {@code cascade}), tetapi menyembunyikannya dari seluruh combobox pemilih; mata pelajaran dan
	 * kurikulum yang terlanjur menunjuk baris ini <b>tetap</b> menunjuknya karena tidak ada
	 * pembersihan FK.</p>
	 *
	 * @param aktif status aktif baru; {@code null} akan dibaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Membangun berkas Excel rekapitulasi komponen nilai satu {@link JadwalPelajaran} untuk satu
	 * {@link DetailGrupPenilaian}, menampilkannya dalam jendela {@code Spreadsheet}, dan
	 * menyediakan tombol unduh.
	 *
	 * <p><b>PENTING &mdash; kode mati.</b> Method ini {@code public static}, tetapi
	 * <b>tidak ada satu pun pemanggilnya</b> di seluruh repositori (diverifikasi dengan pencarian
	 * menyeluruh; satu-satunya penyebutan lain adalah rujukan di Javadoc
	 * {@link KurikulumPunyaJenisNilai}). Method ini juga satu-satunya alasan kelas entity ini
	 * meng-import puluhan kelas ZK/POI &mdash; sebuah pelanggaran lapisan (logika UI di dalam kelas
	 * model) yang tidak menghasilkan apa pun saat ini. Uraian di bawah karena itu bersifat
	 * "bila suatu saat dihidupkan".</p>
	 *
	 * <h3>Alur</h3>
	 * <ol>
	 *   <li>Menolak diam-diam bila salah satu argumen {@code null}; menampilkan peringatan dan
	 *       berhenti bila {@code jadwalPelajaran.getDikunci() != null} (penilaian sudah dikunci).</li>
	 *   <li>Membuat berkas kosong {@code <webapp>/tmp/cetak_data_<yyMMddHHmmss>.xlsx}.</li>
	 *   <li>Memasang ZK {@link Timer} <b>200&nbsp;ms berulang</b> pada akar halaman sebagai
	 *       mekanisme <i>polling</i>: timer membaca teks sebuah {@link Label} yang dipakai sebagai
	 *       kanal komunikasi lintas-thread. Teks kosong berarti "berkas selesai" &rarr; buka
	 *       jendela {@code Spreadsheet}; teks {@code "-"} berarti "batal" &rarr; lepaskan timer.</li>
	 *   <li>Menjalankan {@code new Thread(...)} yang mengumpulkan data lewat dua sesi Hibernate
	 *       ({@code HibernateUtil.currentNativeSession()} dan
	 *       {@code StreamingHibernateUtil}), menulis {@code XSSFWorkbook}, lalu mengosongkan teks
	 *       label sebagai tanda selesai.</li>
	 * </ol>
	 *
	 * <h3>Isi berkas</h3>
	 * <p>Baris judul: {@code No.}, {@code NIM}, {@code Nama Siswa}, lalu satu kolom untuk setiap
	 * komponen nilai yang terikat pada {@code detailGrupPenilaian} &mdash; {@link PertemuanPunyaUjian}
	 * (ujian per pertemuan), {@link Pertemuan} (tugas mandiri, kolom {@code judultugas}), dan
	 * {@link TugasKelompok} &mdash; masing-masing diberi keterangan {@code (bobot:...)}. Dua kolom
	 * terakhir adalah {@code Total} (jumlah nilai mentah) dan {@code Nilai Akhir} (jumlah nilai
	 * setelah dibobot terhadap {@code totalPersen}). Setiap baris berikutnya adalah satu
	 * {@link KelasSiswaPunyaSiswa} yang aktif pada jadwal tersebut.</p>
	 *
	 * <h3>Kuirk dan bom waktu yang tercatat</h3>
	 * <ul>
	 *   <li><b>Tombol "Masukkan nilai ke ..." tidak melakukan apa-apa.</b> Badan
	 *       {@code EventListener}-nya <b>dikomentari 100%</b>. Tombol itu pun hanya muncul bila ada
	 *       baris {@link KurikulumPunyaJenisNilai} yang cocok &mdash; relasi yang sudah tercatat
	 *       sebagai yatim. Akibatnya {@link Map} {@code nilais} yang dikumpulkan sepanjang proses
	 *       tidak pernah dibaca siapa pun; ia hanya menahan memori sampai jendela ditutup.</li>
	 *   <li><b>Timer bisa berputar selamanya.</b> Cabang "batal" menunggu label bernilai
	 *       {@code "-"}, padahal <b>tidak ada kode yang pernah menuliskan {@code "-"}</b>. Bila
	 *       thread latar gagal sebelum sempat mengosongkan label, atau bila blok pembuatan jendela
	 *       melempar (ditangkap {@code catch} yang hanya memanggil {@code Clients.clearBusy()}
	 *       tanpa {@code timer.detach()}), timer 200&nbsp;ms tetap menembak selamanya selama
	 *       halaman terbuka.</li>
	 *   <li><b>Thread latar menyentuh komponen ZK.</b> {@code label.setValue(...)} dan
	 *       {@code intbox.setValue(...)} dipanggil dari thread biasa tanpa
	 *       {@code Executions.activate(...)}. Pola ini "berfungsi" hanya karena komponen tidak
	 *       terpasang ke desktop, dan tidak pernah aman untuk diperluas.</li>
	 *   <li><b>Pembagian dengan nol.</b> {@code totalPersen} adalah jumlah seluruh bobot komponen;
	 *       bila tidak ada komponen berbobot, seluruh pembagian {@code nilai / totalPersen}
	 *       menghasilkan {@code Infinity}/{@code NaN} yang langsung ditulis ke sel Excel.</li>
	 *   <li><b>Ukuran pratinjau dipatok.</b> {@code colSize} dibuat {@code new Intbox(10)} dan
	 *       <b>tidak pernah diperbarui</b> dengan jumlah kolom sesungguhnya, sehingga
	 *       {@code spreadsheet.setMaxcolumns(...)} selalu 11 &mdash; kolom komponen ke-8 dan
	 *       seterusnya tidak terlihat pada pratinjau (berkas unduhannya tetap utuh).</li>
	 *   <li><b>Kesalahan per siswa ditelan.</b> Exception di dalam perulangan siswa hanya
	 *       dilaporkan lewat {@code Common.tampilErrorJikaAdmin(e)}; bagi pengguna non-admin baris
	 *       siswa itu keluar kosong tanpa penjelasan.</li>
	 * </ul>
	 *
	 * <h3>&#9888; Catatan privasi &mdash; berkas ekspor ditulis ke direktori publik</h3>
	 * <p>Berkas dibuat di {@code Sessions.getCurrent().getWebApp().getRealPath("/tmp/...")}, yaitu
	 * di <b>dalam</b> direktori webapp, dan pratinjaunya diakses lewat URL relatif
	 * {@code "../../tmp/" + file.getName()} sebagai sumber daya statis biasa. Berkas itu berisi
	 * NIM, nama, dan seluruh nilai siswa satu kelas; namanya hanya bergantung pada cap waktu
	 * {@code yyMMddHHmmss} (format {@code Common.datetimeFormat2s}, presisi detik, tanpa komponen
	 * acak), dan <b>tidak pernah dihapus</b>. Pada konfigurasi repo saat ini
	 * {@code WEB-INF/web.xml} hanya memasang {@code security-constraint} untuk {@code *.sh},
	 * {@code *.sql}, dan {@code *.bak}, sedangkan {@code applicationContext-security.xml} menutup
	 * dengan {@code <intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>} &mdash;
	 * sehingga berkas semacam ini pada dasarnya dapat diambil tanpa otentikasi bila namanya
	 * ditebak. Untuk method <i>ini</i> risikonya <b>laten</b> (kode mati), tetapi pola
	 * {@code getRealPath("/tmp/...")} yang sama dipakai di <b>ratusan</b> titik lain yang hidup;
	 * lihat catatan audit menyeluruh.</p>
	 *
	 * @param jadwalPelajaran    jadwal pelajaran yang nilainya direkap; {@code null} =&gt; method
	 *                           tidak melakukan apa pun. Harus belum dikunci
	 *                           ({@code getDikunci() == null}), jika tidak hanya muncul peringatan.
	 * @param detailGrupPenilaian simpul {@link DetailGrupPenilaian} yang menjadi penyaring komponen
	 *                           nilai (ujian/tugas mandiri/tugas kelompok yang menunjuknya);
	 *                           {@code null} =&gt; method tidak melakukan apa pun
	 * @throws Exception bila pembuatan berkas sementara gagal ({@code IOException} dari
	 *                   {@code createNewFile()}) atau {@code URLEncoder.encode} gagal; kegagalan di
	 *                   dalam thread latar <b>tidak</b> merambat lewat method ini
	 * @see DetailGrupPenilaian
	 * @see KurikulumPunyaJenisNilai
	 */
	@SuppressWarnings("unchecked")
	public static void hitungNilaiBerdasarkanDetailGrupPenilaian(final JadwalPelajaran jadwalPelajaran,
			final DetailGrupPenilaian detailGrupPenilaian) throws Exception {
		if (detailGrupPenilaian != null && jadwalPelajaran != null) {

			if (jadwalPelajaran.getDikunci() != null) {
				MyMessageboxConfig.show("Penilaian untuk jadwalPelajaran ini telah terkunci", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			final Map<KelasSiswaPunyaSiswa, Double> nilais = new HashMap<KelasSiswaPunyaSiswa, Double>();

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
			final Intbox intbox = new Intbox(10);
			final Intbox colSize = new Intbox(10);
			Clients.showBusy(label.getValue());

			final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
					+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
					+ ".xlsx");
			final File file;
			(file = new File(filename)).createNewFile();

			final Timer timer = new Timer(200);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {

						Clients.showBusy(label.getValue());
						// System.out.println("label " + label.getValue());

						if (label.getValue().trim().equalsIgnoreCase("-")) {
							Clients.clearBusy();
							timer.detach();
						} else if (label.getValue().isEmpty()) {

							Center center = new Center();
							final MyWindow window = new MyWindow("Cetak Data", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("97%");
							window.setWidth("90%");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);

							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setParent(borderlayout);

							System.out.println("loading file " + file.getAbsolutePath());
							Common.clear(center);
							Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
							Common.clear(center);
							spreadsheet.setParent(center);
							spreadsheet.setWidth("100%");
							spreadsheet.setHeight("100%");
							spreadsheet.setSrc("../../tmp/" + file.getName());

							spreadsheet.setMaxrows(intbox.getValue() + 1);
							spreadsheet.setMaxcolumns(colSize.getValue() + 1);

							South south = new South();
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							// toolbar.setHeight("25px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batalkan", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							cancel.setParent(toolbar);

							MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
							print.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									try {
										Filedownload.save(new FileInputStream(file),
												"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
												file.getName());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JenisPenilaian.java:287");

									}
								}
							});
							print.setParent(toolbar);

							if (jadwalPelajaran.getKurikulumPunyaMatapelajaran() != null) {
								final KurikulumPunyaJenisNilai kurikulumPunyaJenisNilai = (KurikulumPunyaJenisNilai) HibernateUtil
										.currentSession().createCriteria(KurikulumPunyaJenisNilai.class)
										.add(Restrictions.eq("kurikulumPunyaMatapelajaran",
												jadwalPelajaran.getKurikulumPunyaMatapelajaran()))
										.add(Restrictions.eq("detailGrupPenilaian", detailGrupPenilaian))
										.setMaxResults(1).uniqueResult();

								if (kurikulumPunyaJenisNilai != null) {
									MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
											"Masukkan nilai ke \"" + detailGrupPenilaian.getNama() + "\"",
											"/img/svg/check2.svg");
									proses.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
//											for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : nilais.keySet()) {
//												Double nilaiSemua = nilais.get(kelasSiswaPunyaSiswa);
//												Double jumlah = nilaiSemua == null ? 0.0 : nilaiSemua.doubleValue();
//												kelasSiswaPunyaSiswa.populateDetailNilai(detailGrupPenilaian,
//														jadwalPelajaran.getMatapelajaran(), jumlah.toString(), true,
//														jadwalPelajaran.getSemester());
//												Common.refreshUpdate(kelasSiswaPunyaSiswa);
//											}

										}
									});
									proses.setParent(toolbar);
								}
							}

							window.setVisible(true);
							window.onModal();

							Clients.clearBusy();
							timer.detach();
						}

					} catch (Exception e) {
						Clients.clearBusy();
					}

				}
			});
			timer.start();

			try {

				Clients.showBusy(label.getValue());

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						try {

							Session session = HibernateUtil.currentNativeSession();
							Session sessionStreaming = StreamingHibernateUtil.getInstance().currentSession();
							List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = session
									.createCriteria(KelasSiswaPunyaSiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();

							intbox.setValue(kelasSiswaPunyaSiswas.size());

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("Nilai"));

							sheet.setDefaultColumnWidth(20);

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue(Common.getBahasaConfig("No."));
							rowhead.createCell(1).setCellValue(Common.getBahasaConfig("NIM"));
							rowhead.createCell(2).setCellValue(Common.getBahasaConfig("Nama Siswa"));

							List<PertemuanPunyaUjian> pertemuanPunyaUjians = session
									.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "pertemuan")
									.addOrder(Order.asc("nama"))
									.add(Restrictions.eq("pertemuan.jadwalPelajaran", jadwalPelajaran))
									.add(Restrictions.eq("detailGrupPenilaian", detailGrupPenilaian)).list();
							int index = 3;
							Double totalPersen = 0.0;
							for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
								totalPersen += pertemuanPunyaUjian.getProsentase();
								rowhead.createCell(index).setCellValue(pertemuanPunyaUjian.getNama() + "(bobot:"
										+ Common.numberFormat.get().format(pertemuanPunyaUjian.getProsentase()) + ")");
								index++;
							}

							List<Pertemuan> pertemuanTugas = session.createCriteria(Pertemuan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.addOrder(Order.asc("judultugas"))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.add(Restrictions.eq("detailGrupPenilaian", detailGrupPenilaian)).list();
							for (Pertemuan tugas : pertemuanTugas) {
								totalPersen += tugas.getProsentase();
								rowhead.createCell(index).setCellValue(tugas.getJudultugas() + "(bobot:"
										+ Common.numberFormat.get().format(tugas.getProsentase()) + ")");
								index++;
							}

							List<TugasKelompok> tugasKelompoks = session.createCriteria(TugasKelompok.class)
									.addOrder(Order.asc("judul"))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.add(Restrictions.eq("detailGrupPenilaian", detailGrupPenilaian)).list();
							for (TugasKelompok tugasKelompok : tugasKelompoks) {
								totalPersen += tugasKelompok.getProsentase();
								rowhead.createCell(index).setCellValue(tugasKelompok.getJudul() + "(bobot:"
										+ Common.numberFormat.get().format(tugasKelompok.getProsentase()) + ")");
								index++;
							}

							rowhead.createCell(index).setCellValue("Total");
							rowhead.createCell(index + 1).setCellValue("Nilai Akhir");
							int rowIndex = 0;
							for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
								try {
									rowIndex++;
									System.out.println("kelasSiswaPunyaSiswa = " + kelasSiswaPunyaSiswa);
									if (kelasSiswaPunyaSiswa == null) {
										continue;
									}
									label.setValue(
											"Sedang memproses data "
													+ kelasSiswaPunyaSiswa.toString() + " (" + Common.numberFormat.get()
															.format(rowIndex * 100.0 / kelasSiswaPunyaSiswas.size())
													+ " %)");

									XSSFRow row = sheet.createRow(rowIndex);
									row.createCell(0).setCellValue(rowIndex);
									row.createCell(1).setCellValue(kelasSiswaPunyaSiswa.getSiswa().getNim());
									row.createCell(2).setCellValue(kelasSiswaPunyaSiswa.getSiswa().getNama());

									Double nilaiTotal = 0.0;
									Double nilaiSemua = 0.0;
									index = 3;
									for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {

										Number ujianSiswa = (Number) session.createCriteria(HasilUjianMahasiswa.class)
												.setMaxResults(1).setProjection(Projections.property("nilai"))
												.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
												.add(Restrictions.eq("siswa", kelasSiswaPunyaSiswa.getSiswa()))
												.uniqueResult();
										if (ujianSiswa != null) {
											Double nilai = (ujianSiswa.doubleValue()
													* pertemuanPunyaUjian.getProsentase()) / totalPersen;
											nilaiSemua += nilai;
											nilaiTotal += ujianSiswa.doubleValue();
											row.createCell(index).setCellValue(ujianSiswa.doubleValue());
										} else {
											row.createCell(index).setCellValue(0.0);
										}
										index++;
									}

									for (Pertemuan tugas : pertemuanTugas) {
										Number nilaiTugas = (Number) sessionStreaming
												.createCriteria(TugasFileContent.class).setMaxResults(1)
												.setProjection(Projections.property("nilai"))
												.add(Restrictions.eq("pertemuan", tugas.getId()))
												.add(Restrictions.eq("siswa", kelasSiswaPunyaSiswa.getSiswa().getId()))
												.uniqueResult();
										if (nilaiTugas != null) {
											Double nilai = (nilaiTugas.doubleValue() * tugas.getProsentase())
													/ totalPersen;
											nilaiSemua += nilai;
											nilaiTotal += nilaiTugas.doubleValue();
											row.createCell(index).setCellValue(nilaiTugas.doubleValue());
										} else {
											row.createCell(index).setCellValue(0.0);
										}
										index++;
									}

									for (TugasKelompok tugasKelompok : tugasKelompoks) {
										Number nilaiTugas = (Number) session
												.createCriteria(NamaTugasKelompokPunyaMahasiswa.class).setMaxResults(1)
												.setProjection(Projections.property("nilai"))
												.createAlias("namaTugasKelompok", "namaTugasKelompok")
												.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugasKelompok))
												.add(Restrictions.eq("siswa", kelasSiswaPunyaSiswa.getSiswa()))
												.uniqueResult();
										if (nilaiTugas != null) {
											Double nilai = (nilaiTugas.doubleValue() * tugasKelompok.getProsentase())
													/ totalPersen;
											nilaiSemua += nilai;
											nilaiTotal += nilaiTugas.doubleValue();
											row.createCell(index).setCellValue(nilaiTugas.doubleValue());
										} else {
											row.createCell(index).setCellValue(0.0);
										}
										index++;
									}

									row.createCell(index).setCellValue(nilaiTotal);
									row.createCell(index + 1).setCellValue(nilaiSemua);

									nilais.put(kelasSiswaPunyaSiswa, nilaiSemua);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}

							try {
								FileOutputStream fileOut = new FileOutputStream(filename);
								workbook.write(fileOut);
								fileOut.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}
							System.out.println("Your excel file has been generated! ");
							kelasSiswaPunyaSiswas.clear();
							kelasSiswaPunyaSiswas = null;
							label.setValue("");
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							label.setValue("");
						}

						HibernateUtil.closeSession();
						StreamingHibernateUtil.getInstance().closeSession();

											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		}
	}
}
