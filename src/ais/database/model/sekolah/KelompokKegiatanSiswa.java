package ais.database.model.sekolah;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Katalog <b>"Jenis Kegiatan"</b> siswa &mdash; master satu tingkat (tanpa induk/anak) yang
 * memasok pilihan jenis kegiatan sekaligus <b>bobot poin</b> yang dipakai perhitungan rapor.
 * Dipetakan ke tabel {@code sekolah.kelompok_kegiatan_siswa}.
 *
 * <h2>Peran dalam sistem (terverifikasi dari kode)</h2>
 *
 * <p>Entity ini adalah <b>satu-satunya katalog</b> yang dirujuk
 * {@link ais.database.model.sekolah.KegiatanSiswa} lewat relasi {@code ManyToOne}
 * {@code KegiatanSiswa.kelompokKegiatanSiswa} (kolom FK {@code kelompok_kegiatan_siswa},
 * {@code nullable = false}). Setiap baris kegiatan siswa <b>wajib</b> menunjuk tepat satu baris
 * di sini. Meski namanya berawalan "Kelompok", <b>tidak ada hierarki</b>: kelas ini tidak punya
 * relasi ke dirinya sendiri, dan {@link ais.database.model.sekolah.KegiatanSiswa} tidak punya
 * katalog lapis kedua. Di layar, kombobox yang menunjuk katalog ini berlabel
 * <i>"Jenis Kegiatan"</i> ({@code KegiatanSiswaAction}), sedangkan layar masternya sendiri
 * berlabel <i>"Kelompok Kegiatan Siswa"</i> &mdash; dua nama untuk entity yang sama.
 *
 * <p>Katalog ini dipakai oleh <b>dua</b> konsumen struktural yang berbeda:</p>
 * <ul>
 *   <li><b>Baris kegiatan</b> &mdash; {@link ais.database.model.sekolah.KegiatanSiswa} memilih
 *   satu jenis kegiatan; {@link #getPoin()} baris katalog itulah yang masuk ke rapor
 *   (lihat "Mekanisme poin" di bawah).</li>
 *   <li><b>Definisi formulir dinamis</b> &mdash;
 *   {@link ais.database.model.sekolah.ParameterTambahanKegiatanSiswa} memasangkan katalog ini
 *   ({@code kelompokKegiatanSiswa}) dengan {@link ais.database.model.ParameterTambahan}, sehingga
 *   satu baris di sini juga berperan sebagai <i>heading/seksi</i> pada formulir tambahan yang
 *   dirakit {@code ParameterTambahanKegiatanSiswaListener}. Perakit itu menyaring dengan
 *   {@code kelompokKegiatanSiswa.aktif = true}, jadi menonaktifkan satu baris di sini juga
 *   <b>menyembunyikan seluruh seksi form tambahannya</b>.</li>
 * </ul>
 *
 * <p><b>Bukan kerabat</b> {@code KelompokKegiatanKesiswaan}/{@code DetailKelompokKegiatanKesiswaan}
 * (keluarga {@link ais.database.model.sekolah.KegiatanKesiswaan}, batch 64). Nama, tabel, dan
 * pemakainya terpisah total; kemiripan nama field murni warisan salin-tempel template hbm2java.</p>
 *
 * <h2>Mekanisme poin (VERIFIKASI PENTING)</h2>
 *
 * <p>{@link #getPoin()} adalah bobot yang <b>dibaca langsung dari katalog saat rapor dicetak</b>,
 * bukan angka yang disalin ke baris kegiatan. Jalur persisnya ada di
 * {@code ais.action.report.format1.sekolah.LaporanRaporSiswa.masukkanPoin(...)}: untuk setiap
 * {@link ais.database.model.sekolah.KegiatanSiswa} milik siswa yang bersangkutan, laporan
 * mengisi {@code map.put("poin", kegiatan.getKelompokKegiatanSiswa().getPoin())} lalu
 * mengakumulasi {@code totalPointKegiatan += kegiatan.getKelompokKegiatanSiswa().getPoin()}.
 * Hasilnya diserahkan ke JRXML sebagai parameter {@code "kegiatanSiswas_" + idSiswa} (rincian
 * baris) dan {@code "totalPointKegiatan_" + idSiswa} (total). Poin pelanggaran/apresiasi dihitung
 * di method yang sama tetapi dari master lain ({@code Hukuman}, {@code ApresiasiSiswa}).</p>
 *
 * <p>Konsekuensi yang <b>harus disadari operator</b>:</p>
 * <ul>
 *   <li><b>Tidak ada snapshot.</b> Nilai poin tidak pernah dibekukan ke baris
 *   {@link ais.database.model.sekolah.KegiatanSiswa}. Mengubah {@link #setPoin(Double)} hari ini
 *   <b>mengubah retroaktif</b> total poin pada setiap rapor tahun-tahun sebelumnya yang dicetak
 *   ulang &mdash; termasuk rapor yang sudah pernah dibagikan dengan angka lama.</li>
 *   <li><b>Tanpa validasi rentang.</b> {@link #setPoin(Double)} menerima nilai apa pun. Layar
 *   memakai {@code MyDoublebox} <b>tanpa constraint</b> dan {@code KelompokKegiatanSiswaAction.onSave}
 *   hanya memvalidasi nama (wajib isi + unik), <b>bukan</b> poin. Jadi poin
 *   <b>boleh negatif</b>, boleh nol, dan tidak punya batas atas; nilai negatif akan
 *   <i>mengurangi</i> {@code totalPointKegiatan} pada rapor. Tidak ada tanda di UI bahwa itu
 *   disengaja atau salah ketik.</li>
 *   <li><b>{@code null} dibaca sebagai 0.0</b> oleh {@link #getPoin()} sehingga baris lama yang
 *   belum diisi tidak memicu {@code NullPointerException} saat penjumlahan di laporan.</li>
 * </ul>
 *
 * <h2>Cakupan tenant: FAIL-OPEN (katalog de facto global)</h2>
 *
 * <p>Entity <i>punya</i> kolom {@code sekolah_id} dan {@code yayasan_id}
 * ({@link #getSekolah()}/{@link #getYayasan()}), tetapi <b>tidak ada satu pun jalur tulis yang
 * mengisinya</b>: {@code KelompokKegiatanSiswaAction.onSave(Event)} hanya menyetel
 * {@code nama}, {@code poin}, dan {@code keterangan}; {@link #checkCreateDefault()} dan seeder
 * di {@code doAfterCompose} juga tidak menyetel keduanya. Akibatnya seluruh baris berakhir dengan
 * {@code sekolah_id}/{@code yayasan_id} bernilai {@code NULL}.</p>
 *
 * <p>Di sisi baca, <b>tidak satu pun</b> query yang menyaring tenant:
 * {@code KelompokKegiatanSiswaAction.initCriteria(boolean)} (daftar + hitung paging),
 * {@code Common.insertCombo(..., KelompokKegiatanSiswa.class, ...)} di
 * {@code KegiatanSiswaAction} dan {@code ParameterTambahanKegiatanSiswaAction}, serta
 * {@code DashboardRekapKegiatanSiswaData} semuanya membuat {@code Criteria} polos. Gabungan
 * kedua sisi berarti katalog ini <b>dipakai bersama seluruh instalasi</b>: satu sekolah bisa
 * melihat, mengubah poin, menonaktifkan, dan menghapus jenis kegiatan yang dipakai sekolah lain
 * &mdash; dan karena poin dibaca live saat cetak, suntingan itu langsung memengaruhi rapor
 * sekolah lain. Juga: pemeriksaan keunikan nama
 * ({@code KelompokKegiatanSiswaAction.checkNamaKelompokKegiatanSiswa()}) bersifat global, jadi
 * dua sekolah tidak bisa memakai nama jenis kegiatan yang sama.</p>
 *
 * <p><b>Ironi arah sebaliknya di New UI.</b> Karena {@code sekolah}/{@code yayasan} termasuk 12
 * nama properti yang di-whitelist {@code GenericCrudAutoEntityAdapter.scopeBindings()}, jalur
 * Generic CRUD v2 justru menambahkan {@code Restrictions.eq("sekolah", <sekolah pengguna>)}.
 * Karena semua baris ber-{@code sekolah_id} {@code NULL}, daftar New UI
 * ({@code new/sekolah/services/kelompok_kegiatan_siswa_service.jsp}) akan <b>kosong</b> untuk
 * pengguna non-admin, sementara layar ZK lama menampilkan semuanya. Perbedaan ini fungsional,
 * bukan keamanan, tetapi mudah dikira "data hilang".</p>
 *
 * <h2>Pewarisan hak lewat menu induk</h2>
 *
 * <p>{@code kelompok_kegiatan_siswa.zul} <b>tidak terdaftar di menu mana pun</b>; berkas itu
 * disisipkan sebagai salah satu {@code tabpanel} di dalam
 * {@code /WEB-INF/z/x/y/pages/master/sekolah/kegiatan_siswa.zul}. Karena
 * {@code CommonPrivilages.checkPrevilages(...)} di {@code KelompokKegiatanSiswaAction} menilai
 * hak terhadap menu yang <i>sedang</i> dibuka, seluruh CRUD katalog ini &mdash; termasuk
 * mengubah poin yang memengaruhi rapor &mdash; <b>mewarisi hak menu induk "Kegiatan Siswa"</b>.
 * Pola yang sama berlaku untuk tab tetangganya ({@code parameter_tambahan_kegiatan_siswa.zul},
 * {@code pembina_siswa.zul}, {@code parameter_tambahan.zul}). Ini instance lanjutan dari pola
 * berulang yang sudah dicatat pada batch-batch sebelumnya.</p>
 *
 * <h2>Auto-seed dan baris bawaan</h2>
 *
 * <p>Ada <b>dua</b> mekanisme penyemaian yang saling lepas dan menghasilkan baris berbeda:</p>
 * <ol>
 *   <li>{@code KelompokKegiatanSiswaAction.doAfterCompose(Component)} &mdash; bila
 *   {@code rowCount} tabel (global, tanpa tapis tenant) bernilai 0, membuat satu baris bernama
 *   {@code "Kegiatan Siswa"}. Baris ini <b>tidak</b> ber-{@code defaultData}. Perhatikan pula
 *   {@code angket.setKode("001.000")} pada seeder itu: {@code kode} adalah properti
 *   {@link ais.database.model.GeneralValueObject} yang <b>tidak dideklarasikan ulang</b> di sini,
 *   sedangkan kelas dasar itu bukan {@code @Entity}/{@code @MappedSuperclass}, sehingga nilainya
 *   <b>tidak pernah tersimpan</b> ke basis data &mdash; setelan itu senyap tak berefek.</li>
 *   <li>{@link #checkCreateDefault()} &mdash; dipanggil
 *   {@code ParameterTambahanKegiatanSiswaAction.doAfterCompose(Component)}, membuat baris
 *   {@code "Form Tambahan"} dengan {@link #setDefaultData(Boolean) defaultData} = {@code true}
 *   bila belum ada. Baris ber-{@code defaultData} <b>tidak bisa dihapus</b> lewat layar (tombol
 *   hapus disembunyikan renderer).</li>
 * </ol>
 *
 * <h2>Catatan pemetaan Hibernate</h2>
 *
 * <p>Kelas mewarisi {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} &mdash; POJO abstrak biasa. Hibernate karena
 * itu <b>tidak memetakan</b> properti kelas induk. Pengulangan deklarasi {@code id},
 * {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah},
 * {@code sekolah}, {@code yayasan}, dan {@code aktif} di kelas ini <b>bukan bug melainkan
 * keharusan teknis</b>: hanya yang dideklarasikan ulang di sini yang benar-benar punya kolom.
 * Kebalikannya juga berlaku dan terlihat pada kasus {@code kode} di atas.</p>
 *
 * <p>Anotasi kolom hanya dipasang pada {@link #getId()}, {@link #getNama()},
 * {@link #getKeterangan()}, {@link #getTanggal_dirubah()}, {@link #getSekolah()}, dan
 * {@link #getYayasan()}. Properti {@code defaultData}, {@code aktif}, {@code nomorUrut},
 * {@code bisaDipilihSiswa}, dan {@code poin} <b>tanpa {@code @Column}</b> sehingga memakai nama
 * kolom bawaan Hibernate. Kelas beranotasi {@code @Audited} (Envers), jadi setiap perubahan
 * &mdash; termasuk perubahan {@code poin} &mdash; terekam di tabel revisi dan dapat ditelusuri
 * lewat {@code RevisiHelper} pada kolom pertama grid.</p>
 *
 * <p>{@code dynamicInsert}/{@code dynamicUpdate} aktif: hanya kolom yang benar-benar berubah
 * yang ikut dalam pernyataan SQL. Ini relevan karena beberapa getter di bawah <b>menulis balik
 * ke field</b> (lihat bagian berikut) dan karenanya dapat memicu {@code UPDATE} tak terduga bila
 * objek sedang <i>attached</i> pada session.</p>
 *
 * <h2>Getter yang menulis balik / merusak</h2>
 * <ul>
 *   <li>{@link #getDefaultData()}, {@link #getAktif()}, {@link #getNomorUrut()} &mdash; menulis
 *   nilai bawaan ({@code false}/{@code true}/{@code 1}) ke field bila masih {@code null}. Membaca
 *   saja bisa membuat objek menjadi <i>dirty</i>.</li>
 *   <li>{@link #getSekolah()} &mdash; menulis balik hasil {@code check(...)} (resolusi proxy
 *   lazy) ke field.</li>
 *   <li>{@link #getYayasan()} &mdash; <b>paling destruktif</b>: menimpa {@code yayasan} dengan
 *   {@code sekolah.getYayasan()} setiap kali {@code sekolah} tidak {@code null}, dan juga
 *   menulis ulang field {@code sekolah}. Nilai yayasan yang disetel eksplisit lewat
 *   {@link #setYayasan(Yayasan)} akan hilang pada pembacaan pertama. Efek ini tidak terasa pada
 *   data saat ini hanya karena {@code sekolah} selalu {@code null} (lihat bagian cakupan
 *   tenant).</li>
 *   <li>{@link #checkCreateDefault()} &mdash; bukan sekadar pemeriksaan: <b>menyimpan dan
 *   commit sendiri</b>, lalu <b>menutup session</b>. Rinciannya di Javadoc method.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; label</b>: {@link #getId()}/{@link #setId(Long)},
 *   {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()}.</li>
 *   <li><b>Bobot rapor</b>: {@link #getPoin()}/{@link #setPoin(Double)}.</li>
 *   <li><b>Kendali tampil &amp; urutan</b>: {@link #getAktif()}/{@link #setAktif(Boolean)},
 *   {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)},
 *   {@link #getBisaDipilihSiswa()}/{@link #setBisaDipilihSiswa(Boolean)} (tidak terpakai),
 *   {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Cakupan institusi</b>: {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *   {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Baris bawaan</b>: {@link #getDefaultData()}/{@link #setDefaultData(Boolean)},
 *   {@link #checkCreateDefault()}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious lain</h2>
 * <ul>
 *   <li>{@link #toString()} <b>tidak</b> memakai format {@code "kode - nama"} milik
 *   {@link ais.database.model.GeneralValueObject}, melainkan {@code id + "-" + nama}. Format itu
 *   ikut masuk ke nama turunan baris kegiatan
 *   ({@code KegiatanSiswa.getNama()} merangkai {@code siswa + "_" + kelompokKegiatanSiswa + "_" + waktu}),
 *   jadi <b>mengganti {@link #setNama(String) nama} di sini mengubah label baris kegiatan</b>
 *   pada penyimpanan berikutnya.</li>
 *   <li>{@link #getBisaDipilihSiswa()} <b>tidak dipakai sama sekali</b> pada keluarga entity ini
 *   &mdash; tidak ada layar yang menampilkannya dan tidak ada query yang menyaringnya. Seluruh
 *   pemakaian nama tersebut di repo berasal dari {@code KelompokKegiatanKesiswaan}/
 *   {@code DetailKelompokKegiatanKesiswaan}. Perlakukan sebagai kolom vestigial.</li>
 *   <li><b>Bug salin-tempel yang nyata:</b> tombol "Upload Data" pada layar ini dibangun dengan
 *   {@code Common.uploadData(this, Hukuman.class, contents)} &mdash; kelas target impor massal
 *   adalah {@link ais.database.model.sekolah.Hukuman}, <b>bukan</b> kelas ini, padahal kolom
 *   {@code {"id","nama","poin","keterangan","aktif"}} kebetulan cocok di kedua entity sehingga
 *   impor berhasil diam-diam ke tabel yang salah. Tombol "Cetak" di sebelahnya benar (memakai
 *   {@code initCriteria} milik layar ini). Jejaknya bahkan ikut ke scaffold New UI, yang
 *   mendaftarkan {@code nuiServiceEntities = {"KelompokKegiatanSiswa", "Hukuman"}}.</li>
 *   <li>Kolom FK di {@link ais.database.model.sekolah.KegiatanSiswa} bernama
 *   {@code kelompok_kegiatan_siswa} <b>tanpa</b> akhiran {@code _id}, berbeda dari konvensi
 *   {@code sekolah_id}/{@code yayasan_id} pada kelas ini.</li>
 * </ul>
 *
 * @see ais.database.model.sekolah.KegiatanSiswa
 * @see ais.database.model.sekolah.ParameterTambahanKegiatanSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kelompok_kegiatan_siswa")
public class KelompokKegiatanSiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key {@code sekolah.kelompok_kegiatan_siswa.id}. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna penyimpan, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Tidak bisa mengosongkan.</b> Nilai {@code null} atau string kosong/spasi diabaikan
	 * diam-diam (method langsung {@code return}), sehingga jejak audit yang sudah terisi tidak
	 * dapat dihapus lewat setter. Ini pola standar seluruh entity AIS.</p>
	 *
	 * @param olehId id pengguna; {@code null}/blank diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/blank diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/blank diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna penyimpan, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback {@code @PreUpdate} JPA: menyerahkan baris ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum
	 * {@code UPDATE} dieksekusi, sehingga {@link #getTanggal_dirubah()}, {@link #getOleh()}, dan
	 * {@link #getOlehId()} terisi dari konteks pengguna aktif tanpa perlu diisi pemanggil.
	 *
	 * <p>Tidak ada padanan {@code @PrePersist} dan tidak ada kolom "tanggal dibuat": nilai awal
	 * {@code tanggal_dirubah} datang dari inisialisasi field
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) yang ditulis pada baris yang sama dengan
	 * deklarasi method ini &mdash; bentuk padat hasil codemod, bukan kesengajaan gaya.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris katalog dengan format {@code id + "-" + nama}.
	 *
	 * <p><b>Sengaja berbeda</b> dari {@link ais.database.model.GeneralValueObject#toString()} yang
	 * berformat {@code "kode - nama"}; {@code kode} memang tidak dipetakan untuk entity ini.</p>
	 *
	 * <p><b>Efek tak langsung:</b> {@code KegiatanSiswa.getNama()} merangkai nama turunannya dari
	 * {@code siswa + "_" + kelompokKegiatanSiswa + "_" + waktu}, yang berarti hasil method ini
	 * ikut tersimpan sebagai bagian label baris kegiatan. Mengganti {@link #setNama(String)}
	 * mengubah label baris kegiatan pada penyimpanan berikutnya, tetapi <b>tidak</b> memperbarui
	 * baris lama yang sudah tersimpan.</p>
	 *
	 * @return gabungan id dan nama katalog
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama jenis kegiatan; wajib isi dan unik global. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas yang tampil di grid dan tooltip kombobox. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Penanda baris bawaan sistem yang tidak boleh dihapus. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;

	/** Penanda aktif; menyembunyikan katalog dari kombobox dan form tambahan. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Urutan tampil pada dasbor rekap. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/** Sekolah pemilik; pada praktiknya selalu {@code null}. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Yayasan pemilik; diturunkan dari {@code sekolah}. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Kolom vestigial, tidak dipakai kode mana pun. Lihat {@link #getBisaDipilihSiswa()}. */
	private Boolean bisaDipilihSiswa;

	/** Bobot poin yang masuk ke perhitungan rapor. Lihat {@link #getPoin()}. */
	private Double poin;

	/**
	 * Memastikan tersedianya baris katalog bawaan {@code "Form Tambahan"}
	 * ({@link #getDefaultData()} = {@code true}), lalu mengembalikannya.
	 *
	 * <p><b>Bukan pemeriksaan murni.</b> Meski namanya berawalan {@code check}, method ini
	 * <b>menulis ke basis data</b>: bila belum ada baris ber-{@code defaultData} = {@code true},
	 * ia membuat satu ({@code nama} dan {@code keterangan} sama-sama {@code "Form Tambahan"}),
	 * membuka transaksi sendiri ({@code session.getTransaction().begin()}), {@code save}, lalu
	 * {@code commit}. Kolom {@code sekolah}/{@code yayasan} <b>tidak</b> diisi, sejalan dengan
	 * sifat katalog yang global (lihat Javadoc kelas).</p>
	 *
	 * <p><b>Efek samping berat pada session.</b> Method memakai
	 * {@link ais.database.hibernate.HibernateUtil#currentNativeSession()} lalu memanggil
	 * {@code HibernateUtil.closeSession()} sebelum {@code return} &mdash; menutup session
	 * {@code ThreadLocal} milik thread saat itu. Karena
	 * {@code HibernateUtil.currentSession()} pada ZK 9/10 jatuh ke {@code ThreadLocal} yang
	 * <b>sama</b>, panggilan dari konteks request ZK (yaitu
	 * {@code ParameterTambahanKegiatanSiswaAction.doAfterCompose(Component)}, satu-satunya
	 * pemanggil) menutup session yang masih dipakai sisa layar; session berikutnya dibuka ulang
	 * secara lazy sehingga objek yang sudah dipegang menjadi <i>detached</i>. Ini bertentangan
	 * dengan pengingat eksplisit pada {@code currentNativeSession()} ("JANGAN dipakai di konteks
	 * request ZK"). Jangan jadikan pola ini contoh untuk kode baru.</p>
	 *
	 * <p><b>Cakupan tenant fail-open:</b> pencarian baris bawaan tidak memfilter
	 * sekolah/yayasan &mdash; hanya ada <b>satu</b> baris "Form Tambahan" untuk seluruh
	 * instalasi. {@code setMaxResults(1)} dipasang tanpa {@code addOrder}, jadi bila terlanjur
	 * ada lebih dari satu baris ber-{@code defaultData}, baris mana yang terpilih tidak
	 * deterministik.</p>
	 *
	 * @return baris katalog bawaan yang sudah ada atau yang baru saja dibuat; tidak pernah
	 *         {@code null} pada jalur normal
	 */
	public static KelompokKegiatanSiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokKegiatanSiswa kelompokParameterTambahanSiswa = (KelompokKegiatanSiswa) session
				.createCriteria(KelompokKegiatanSiswa.class).add(Restrictions.eq("defaultData", true)).setMaxResults(1)
				.uniqueResult();
		if (kelompokParameterTambahanSiswa == null) {
			kelompokParameterTambahanSiswa = new KelompokKegiatanSiswa();
			kelompokParameterTambahanSiswa.setDefaultData(true);
			kelompokParameterTambahanSiswa.setNama("Form Tambahan");
			kelompokParameterTambahanSiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanSiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanSiswa;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA dan dipakai jalur "Tambah" pada layar
	 * ({@code KelompokKegiatanSiswaAction.onAdd(Event)}) serta {@link #checkCreateDefault()}.
	 *
	 * <p>Tidak menyetel apa pun; nilai bawaan {@code aktif}, {@code nomorUrut},
	 * {@code defaultData}, dan {@code poin} baru muncul saat getter masing-masing dipanggil.</p>
	 */
	public KelompokKegiatanSiswa() {
	}

	/**
	 * Mengembalikan primary key baris katalog.
	 *
	 * <p>Nilai ini menjadi bagian {@link #toString()} dan dipakai sebagai potongan pertama kunci
	 * parameter tambahan berformat {@code "<idKelompok>-><idParameter>"} yang dibentuk
	 * {@code ParameterTambahanKegiatanSiswaListener}.</p>
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
	 * Menyetel primary key. Hanya untuk Hibernate dan kode uji; jangan dipanggil kode aplikasi.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis kegiatan, sudah di-{@code trim()}.
	 *
	 * <p>Nilai ini yang tampil sebagai label kombobox "Jenis Kegiatan"
	 * ({@code KegiatanSiswaAction}), judul seksi form tambahan
	 * ({@code ParameterTambahanKegiatanSiswaListener}), heading baris dasbor
	 * ({@code DashboardRekapKegiatanSiswaData}), dan kolom {@code "kelompok"} pada rincian poin
	 * rapor ({@code LaporanRaporSiswa.masukkanPoin}).</p>
	 *
	 * <p><b>Perhatian:</b> {@code trim()} hanya terjadi saat membaca; nilai mentah di basis data
	 * tetap apa adanya. Akibatnya pemeriksaan keunikan di layar (yang membandingkan hasil
	 * {@code trim()} masukan dengan kolom mentah) bisa meleset bila ada baris lama yang menyimpan
	 * spasi di tepi.</p>
	 *
	 * @return nama jenis kegiatan tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis kegiatan.
	 *
	 * <p>Dipanggil {@code KelompokKegiatanSiswaAction.onSave(Event)} setelah dua validasi layar:
	 * nama wajib diisi, dan nama belum dipakai baris lain
	 * ({@code checkNamaKelompokKegiatanSiswa()}, pengecekan <b>global lintas tenant</b> dan
	 * tanpa normalisasi huruf besar/kecil). Setter sendiri tidak memvalidasi apa pun dan tidak
	 * melakukan {@code trim()}.</p>
	 *
	 * <p>Mengubah nama juga mengubah label baris {@link ais.database.model.sekolah.KegiatanSiswa}
	 * yang disimpan setelahnya &mdash; lihat {@link #toString()}.</p>
	 *
	 * @param nama nama jenis kegiatan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis kegiatan.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; diisi dari {@code Textbox} tiga baris pada
	 * dialog "Tambah/Ubah Kelompok".
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris bawaan sistem", dengan {@code false} sebagai nilai efektif
	 * bila kolom masih {@code null}.
	 *
	 * <p><b>Menulis balik ke field:</b> nilai bawaan {@code false} disimpan ke field, sehingga
	 * sekadar membaca dapat membuat objek yang sedang <i>attached</i> menjadi <i>dirty</i> dan
	 * memicu {@code UPDATE} pada {@code flush} berikutnya.</p>
	 *
	 * <p>Dipakai renderer layar untuk menyembunyikan tombol hapus
	 * ({@code button.setVisible(delete && !kelompokKegiatanSiswa.getDefaultData())}), dan oleh
	 * {@link #checkCreateDefault()} sebagai kriteria pencarian baris "Form Tambahan".</p>
	 *
	 * @return {@code true} bila baris bawaan yang tidak boleh dihapus, selain itu {@code false}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda baris bawaan sistem.
	 *
	 * <p>Hanya dipanggil {@link #checkCreateDefault()}; <b>tidak ada kendali UI</b> untuk
	 * menyetel atau membatalkannya, sehingga sekali sebuah baris ditandai bawaan, baris itu
	 * praktis permanen dari sudut pandang operator.</p>
	 *
	 * @param defaultData {@code true} untuk menandai sebagai baris bawaan
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan status aktif, dengan {@code true} sebagai nilai efektif bila kolom masih
	 * {@code null} (baris lama otomatis dianggap aktif).
	 *
	 * <p><b>Menulis balik ke field</b> seperti {@link #getDefaultData()}.</p>
	 *
	 * <p><b>Cakupan pengaruh flag ini tidak seragam:</b></p>
	 * <ul>
	 *   <li>Menyaring kombobox pencarian "Jenis Kegiatan" di
	 *   {@code KegiatanSiswaAction} ({@code Restrictions.eq("aktif", true)}), kombobox pencarian
	 *   {@code ParameterTambahanKegiatanSiswaAction}, daftar dasbor
	 *   {@code DashboardRekapKegiatanSiswaData}, dan perakitan form tambahan
	 *   ({@code kelompokKegiatanSiswa.aktif = true}).</li>
	 *   <li><b>Tidak</b> menyaring kombobox pada dialog simpan {@code KegiatanSiswaAction}
	 *   ({@code Common.insertCombo} tanpa kriteria), sehingga jenis kegiatan nonaktif masih bisa
	 *   dipilih saat menambah/mengubah baris kegiatan.</li>
	 *   <li><b>Tidak</b> menyaring perhitungan rapor: {@code LaporanRaporSiswa.masukkanPoin}
	 *   menjumlahkan poin dari baris kegiatan yang ada, tanpa memeriksa {@code aktif}.
	 *   Menonaktifkan jenis kegiatan <b>tidak</b> mengeluarkan poinnya dari rapor.</li>
	 * </ul>
	 *
	 * @return {@code true} bila katalog masih boleh dipakai pada jalur yang menyaringnya
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Dipanggil langsung dari checkbox "Aktif" pada grid layar, yang <b>langsung menyimpan</b>
	 * ({@code Common.refreshSaveOrUpdate}) begitu dicentang &mdash; tanpa dialog konfirmasi dan
	 * tanpa tombol simpan terpisah.</p>
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil, dengan {@code 1} sebagai nilai efektif bila kolom masih
	 * {@code null}.
	 *
	 * <p><b>Menulis balik ke field</b> seperti {@link #getAktif()}. Ternary pada baris
	 * {@code return} bersifat <b>mati</b>: cabang {@code null} sudah dihilangkan blok {@code if}
	 * di atasnya, jadi kondisi itu tidak pernah bernilai benar. Dibiarkan apa adanya karena
	 * hanya redundan, bukan salah.</p>
	 *
	 * <p>Karena nilai bawaannya sama untuk semua baris, urutan hasil
	 * {@link #compareTo(GeneralValueObject)} pada katalog yang belum pernah diatur akan seri
	 * seluruhnya &mdash; {@code Collections.sort} bersifat stabil sehingga urutan hasil query
	 * yang dipertahankan.</p>
	 *
	 * @return nomor urut tampil, minimal {@code 1}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil.
	 *
	 * <p>Dipanggil dari {@code Intbox} kolom "Nomor Urut" pada grid layar yang <b>langsung
	 * menyimpan</b> pada event {@code onChange}, tanpa validasi rentang dan tanpa pemeriksaan
	 * duplikasi antar baris.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dibaca sebagai {@code 1}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua baris untuk keperluan pengurutan tampilan.
	 *
	 * <p>Aturan: bila lawan bandingnya juga {@code KelompokKegiatanSiswa}, urutan ditentukan
	 * {@link #getNomorUrut()}. Bila bukan, method jatuh ke perbandingan {@link #getNama()}, lalu
	 * {@link #getKeterangan()}, dan akhirnya {@code 0}.</p>
	 *
	 * <p><b>Efek samping:</b> cabang pertama memanggil {@link #getNomorUrut()} pada <i>kedua</i>
	 * objek, sehingga pengurutan dapat menulis nilai bawaan {@code 1} ke baris yang belum diatur
	 * dan menjadikannya <i>dirty</i>.</p>
	 *
	 * <p><b>Catatan kontrak:</b> hasilnya tidak konsisten dengan
	 * {@link ais.database.model.GeneralValueObject#equals(Object)} (yang berbasis primary key) dan
	 * tidak simetris terhadap tipe lain. Aman untuk {@code Collections.sort} pada daftar homogen
	 * &mdash; satu-satunya pemakaian nyata ada di {@code DashboardRekapKegiatanSiswaData} &mdash;
	 * tetapi jangan dipakai sebagai pembanding pada {@code TreeSet}/{@code TreeMap} campuran.</p>
	 *
	 * <p>Blok {@code catch} pada cabang kedua menelan kegagalan apa pun (misalnya
	 * {@code LazyInitializationException} saat mengakses lawan banding) dan hanya mencatatnya ke
	 * {@code ais.common.ErrorAuditUtil}; pembanding lalu mengembalikan {@code 0} sehingga urutan
	 * pasangan itu dianggap seri.</p>
	 *
	 * @param arg0 objek pembanding; boleh entity jenis lain
	 * @return bilangan negatif, nol, atau positif sesuai urutan relatif objek ini terhadap
	 *         {@code arg0}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokKegiatanSiswa) {
			KelompokKegiatanSiswa s = (KelompokKegiatanSiswa) arg0;
			return getNomorUrut().compareTo(s.getNomorUrut());
		} else {
			try {
				if (getNama() != null && arg0.getNama() != null) {
					return getNama().compareTo(arg0.getNama());
				} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
					return getKeterangan().compareTo(arg0.getKeterangan());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelompokKegiatanSiswa.java:185");

			}

			return 0;
		}
	}

	/**
	 * Mengembalikan sekolah pemilik katalog, setelah proxy lazy diresolusi
	 * {@link ais.database.model.GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Menulis balik ke field</b> ({@code sekolah = check(sekolah)}) &mdash; getter ini
	 * mengubah state objek.</p>
	 *
	 * <p><b>Fail-open cakupan tenant:</b> pada praktiknya nilai ini <b>selalu {@code null}</b>
	 * karena tidak ada satu pun jalur tulis yang memanggil {@link #setSekolah(Sekolah)} untuk
	 * entity ini, sementara semua query pembacanya juga tidak menyaring sekolah. Katalog karena
	 * itu berperilaku global lintas sekolah/yayasan; rinciannya di Javadoc kelas.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} (kondisi normal pada data saat ini)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik katalog.
	 *
	 * <p>Objek {@code Sekolah} yang belum tersimpan ({@code getId() == null}) diperlakukan sebagai
	 * {@code null} agar {@code CascadeType.PERSIST} tidak ikut menyimpan entity setengah jadi.</p>
	 *
	 * <p><b>Tidak ada pemanggil di seluruh repo</b> &mdash; layar
	 * {@code KelompokKegiatanSiswaAction.onSave(Event)} maupun {@link #checkCreateDefault()}
	 * tidak menyetelnya. Lihat catatan fail-open pada Javadoc kelas.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik katalog.
	 *
	 * <p><b>Getter destruktif.</b> Method ini bukan pembaca murni:</p>
	 * <ol>
	 *   <li>memanggil {@link #getSekolah()} (yang menulis balik field {@code sekolah});</li>
	 *   <li>bila {@code sekolah} tidak {@code null}, <b>menimpa</b> field {@code yayasan} dengan
	 *   {@code sekolah.getYayasan()} &mdash; nilai yang sebelumnya disetel eksplisit lewat
	 *   {@link #setYayasan(Yayasan)} hilang tanpa peringatan;</li>
	 *   <li>menulis balik hasil {@code check(...)} ke field {@code yayasan}.</li>
	 * </ol>
	 *
	 * <p>Dengan {@code dynamicUpdate} aktif, urutan itu bisa menghasilkan {@code UPDATE} kolom
	 * {@code yayasan_id} yang tidak diminta pemanggil mana pun. Pada data saat ini efeknya tidak
	 * terlihat semata-mata karena {@code sekolah} selalu {@code null}; begitu ada baris yang
	 * ber-{@code sekolah_id}, yayasan baris itu terkunci mengikuti yayasan sekolahnya.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila ada), atau {@code null}
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
	 * Menyetel yayasan pemilik katalog.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}, objek tanpa id diperlakukan {@code null}.</p>
	 *
	 * <p><b>Nilai yang disetel di sini tidak tahan lama</b> bila {@code sekolah} terisi:
	 * {@link #getYayasan()} akan menimpanya pada pembacaan berikutnya. Tidak ada pemanggil di
	 * seluruh repo.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan flag "boleh dipilih siswa", dengan {@code true} sebagai nilai efektif bila
	 * kolom masih {@code null}.
	 *
	 * <p><b>Kolom vestigial &mdash; tidak dipakai kode mana pun.</b> Tidak ada layar yang
	 * menampilkan atau menyetelnya untuk entity ini, dan tidak ada query yang menyaringnya.
	 * Seluruh pemakaian nama {@code bisaDipilihSiswa} di repo berasal dari keluarga entity yang
	 * berbeda ({@code KelompokKegiatanKesiswaan} dan {@code DetailKelompokKegiatanKesiswaan},
	 * yang memang menyaring pilihan siswa dengan flag ini). Peninggalan salin-tempel; jangan
	 * diandalkan sebagai kendali akses siswa pada katalog ini.</p>
	 *
	 * @return {@code true} bila katalog boleh dipilih siswa (nilai bawaan)
	 */
	public Boolean getBisaDipilihSiswa() {
		return bisaDipilihSiswa == null ? true : bisaDipilihSiswa;
	}

	/**
	 * Menyetel flag "boleh dipilih siswa". Tanpa pemanggil; lihat
	 * {@link #getBisaDipilihSiswa()}.
	 *
	 * @param bisaDipilihSiswa nilai flag baru
	 */
	public void setBisaDipilihSiswa(Boolean bisaDipilihSiswa) {
		this.bisaDipilihSiswa = bisaDipilihSiswa;
	}

	/**
	 * Mengembalikan bobot poin jenis kegiatan ini, dengan {@code 0.0} sebagai nilai efektif bila
	 * kolom masih {@code null} (berbeda dari getter lain, nilai bawaan <b>tidak</b> ditulis balik
	 * ke field).
	 *
	 * <p><b>Inilah angka yang masuk ke rapor.</b> Jalurnya:
	 * {@code LaporanRaporSiswa.masukkanPoin(...)} melintasi setiap
	 * {@link ais.database.model.sekolah.KegiatanSiswa} milik siswa, mengambil
	 * {@code kegiatan.getKelompokKegiatanSiswa().getPoin()} untuk kolom rincian {@code "poin"},
	 * dan menjumlahkannya ke {@code totalPointKegiatan}. Keduanya diserahkan ke JRXML lewat
	 * parameter {@code "kegiatanSiswas_" + idSiswa} dan {@code "totalPointKegiatan_" + idSiswa}.
	 * Nilai juga ditampilkan sebagai label "Poin" (baca-saja) pada dialog Kegiatan Siswa dan
	 * sebagai kolom "Poin" pada grid layar master.</p>
	 *
	 * <p><b>Karakteristik yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li>Dibaca <b>live</b> saat rapor dirender &mdash; tidak ada penyalinan/snapshot ke baris
	 *   kegiatan, sehingga menyunting poin mengubah hasil cetak ulang rapor lama secara
	 *   retroaktif.</li>
	 *   <li>Bobot melekat pada <b>jenis</b> kegiatan, bukan pada baris kegiatan: setiap kegiatan
	 *   dengan jenis yang sama selalu bernilai sama; tidak ada mekanisme poin per-baris.</li>
	 *   <li>{@link #getAktif()} tidak diperiksa jalur rapor, jadi menonaktifkan jenis kegiatan
	 *   tidak mengeluarkan poinnya dari total.</li>
	 * </ul>
	 *
	 * @return bobot poin; {@code 0.0} bila belum diisi. Bisa bernilai <b>negatif</b> &mdash;
	 *         lihat {@link #setPoin(Double)}
	 */
	public Double getPoin() {
		return poin == null ? 0.0 : poin;
	}

	/**
	 * Menyetel bobot poin jenis kegiatan.
	 *
	 * <p><b>Tanpa validasi apa pun</b>, dan tidak ada lapis validasi di atasnya:
	 * {@code KelompokKegiatanSiswaAction.onSave(Event)} hanya memeriksa nama (wajib isi dan unik)
	 * lalu langsung memanggil {@code setPoin(poin.getValue())}, sedangkan komponen masukan
	 * {@code ais.ui.util.MyDoublebox} hanya mengatur format tampilan
	 * ({@code "#,##0.####"}) tanpa {@code constraint} sama sekali. Konsekuensinya:</p>
	 * <ul>
	 *   <li><b>Nilai negatif diterima</b> dan akan <i>mengurangi</i> {@code totalPointKegiatan}
	 *   pada rapor &mdash; berguna bila memang dimaksudkan sebagai kegiatan bernilai minus,
	 *   tetapi tidak dapat dibedakan dari salah ketik.</li>
	 *   <li><b>Tidak ada batas atas/bawah</b> maupun pembatasan jumlah desimal pada data yang
	 *   tersimpan (format empat desimal hanya memengaruhi tampilan).</li>
	 *   <li>Kotak kosong menghasilkan {@code null}, yang dibaca {@link #getPoin()} sebagai
	 *   {@code 0.0}.</li>
	 * </ul>
	 *
	 * <p>Karena entity beranotasi {@code @Audited}, setiap perubahan poin terekam Envers dan bisa
	 * ditelusuri lewat tautan revisi pada kolom pertama grid layar &mdash; satu-satunya jejak
	 * yang tersedia untuk menjelaskan mengapa total poin sebuah rapor berubah.</p>
	 *
	 * @param poin bobot poin baru; boleh {@code null} (dibaca sebagai {@code 0.0}) dan boleh
	 *             negatif
	 */
	public void setPoin(Double poin) {
		this.poin = poin;
	}
}
