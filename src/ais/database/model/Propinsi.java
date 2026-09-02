package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * Master <b>provinsi</b> &mdash; tingkat kedua hierarki wilayah administratif klasik AIS
 * ({@link Negara} &rarr; {@code Propinsi} &rarr; {@link Kota}). Memetakan tabel
 * {@code public.propinsi} dan dikelola lewat layar
 * {@code /pages/master/propinsi.zul} ({@code ais.action.master.PropinsiAction}, turunan
 * {@code ais.action.master.generic.GenericCrudAction}).
 *
 * <p>Baris di sini dipakai sebagai <b>komponen alamat</b> di hampir semua modul: biodata
 * mahasiswa, calon mahasiswa (tiga relasi sekaligus: provinsi calon, provinsi sekolah asal,
 * provinsi orang tua), dosen, pegawai, orang tua, calon pegawai, calon siswa, penyedia asset,
 * penduduk (sisdes) dan pasien (sirs). Karena itu perubahan pada kelas ini beresiko luas
 * meskipun tabelnya kecil.</p>
 *
 * <h3>Siapa yang memakai entity ini</h3>
 *
 * <ul>
 *   <li><b>Relasi {@code @ManyToOne} langsung</b>: {@link Kota#getPropinsi()} (wajib,
 *   {@code nullable = false}), {@code BiodataMahasiswa}, {@code BiodataCalonMahasiswa}
 *   (3&times;), {@code BiodataDosen}, {@code Pegawai}, {@code OrangTua},
 *   {@code recruitment.CalonPegawai} (2&times;), {@code sekolah.CalonSiswa} (2&times;),
 *   {@code asset.PenyediaAsset}, {@code sisdes.Penduduk}, {@code sirs.Pasien}.</li>
 *   <li><b>Bukan relasi</b>: {@code PerguruanTinggi}, {@code sekolah.Sekolah} dan
 *   {@code sekolah.Yayasan} menyimpan provinsi sebagai <i>kolom teks bebas</i> bernama
 *   {@code propinsi}, tanpa foreign key ke tabel ini &mdash; jangan tertukar.</li>
 *   <li><b>Laporan &amp; ekspor</b>: EMIS ({@code LaporanFormatEMIS},
 *   {@code LaporanFormatEMISDosen}) mengekspor {@link #getKode()} + {@link #getNama()};
 *   dashboard {@code DashboardMahasiswaPropinsi} dan statistik
 *   {@code RekapPendaftarSpmbPropinsi} mengelompokkan mahasiswa/pendaftar per provinsi;
 *   beberapa payment gateway ({@code FasPay}, {@code Doku}) mengirim nama provinsi sebagai
 *   bagian alamat penagih.</li>
 * </ul>
 *
 * <h3>Dua hierarki wilayah yang berjalan berdampingan</h3>
 *
 * <p>AIS menyimpan wilayah administratif <b>dua kali</b>, dan entity ini berdiri di kaki
 * salah satunya:</p>
 *
 * <ol>
 *   <li><b>Hierarki klasik (tabel terpisah)</b>: {@link Negara} &rarr; {@code Propinsi} &rarr;
 *   {@link Kota}. Hierarki ini <b>berhenti di kota/kabupaten</b> &mdash; tidak ada entity
 *   {@code Kecamatan} untuk jalur umum ({@code ais.database.model.sirs.Kecamatan} khusus
 *   modul rumah sakit).</li>
 *   <li><b>Hierarki {@link Wilayah} (satu tabel self-reference)</b> bergaya feeder/PDDIKTI:
 *   satu tabel {@code public.wilayah} dengan kolom {@code level}
 *   (<i>1 = provinsi, 2 = kota/kabupaten, 3 = kecamatan</i>), {@code induk} (kode induk
 *   sebagai teks) dan {@code wilayahInduk} (relasi ke dirinya sendiri). Hanya di hierarki
 *   inilah kecamatan tersedia.</li>
 * </ol>
 *
 * <p>Kedua hierarki dijembatani secara <b>malas dan otomatis</b> oleh {@link #simpanWilayah()}
 * (lihat javadoc method itu) dan padanannya {@link Kota#simpanWilayah()}. Jembatan arah
 * sebaliknya &mdash; dari {@code Wilayah} hasil pilihan pengguna kembali ke baris
 * {@code Propinsi} &mdash; ada di
 * {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)}, yang mencocokkan
 * <b>nama</b> dengan jarak Levenshtein &lt; 2 dan <b>membuat baris {@code Propinsi} baru</b>
 * bila tidak ada yang cukup mirip. Konsekuensinya, tabel ini bisa bertambah isi tanpa ada
 * orang yang membuka layar masternya.</p>
 *
 * <p>Kuirk penamaan: helper pemilih {@code ais.action.master.helper.AmbilDataPropinsiBanbox}
 * <b>tidak</b> menampilkan entity ini &mdash; ia memuat {@link Wilayah} ber-{@code level "1"}.
 * Yang memuat kombo dari entity ini adalah {@code Common.insertCombo(..., Propinsi.class, ...)}
 * di {@code KotaAction}, {@code AmbilDataKecamatanBanbox}, {@code PendudukAction},
 * {@code PasienAction}, {@code RiwayatPendidikanDosenHelper} dan kerabatnya.</p>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Layar daftarnya MENULIS ke database saat sekadar di-render.</b>
 *   {@code PropinsiAction.PropinsiRenderer.render(...)} memanggil {@link #simpanWilayah()}
 *   untuk <i>setiap baris</i> yang tampil di grid. Membuka halaman master provinsi (atau
 *   membalik halaman paging-nya) dapat menyisipkan baris {@link Wilayah} baru dan
 *   meng-{@code UPDATE} kolom {@code wilayah} pada baris provinsi. Ini bukan efek samping
 *   yang lazim untuk sebuah renderer, dan membuat operasi "baca" tidak benar-benar read-only.</li>
 *   <li><b>{@link #getNegara()} tidak pernah mengembalikan {@code null}.</b> Bila kolomnya
 *   kosong, getter mengembalikan singleton statis {@code ConstantValues.INDONESIA}. Karena
 *   pemetaan kelas ini memakai <i>property access</i> (anotasi menempel pada getter), nilai
 *   pengganti itulah yang dibaca Hibernate saat dirty-check/flush &mdash; artinya baris
 *   ber-{@code negara} kosong (mis. hasil impor SQL langsung) akan <b>diam-diam tersimpan
 *   sebagai Indonesia</b> pada update berikutnya. Detail lain: {@code ConstantValues.INDONESIA}
 *   diisi sekali saat inisialisasi aplikasi ({@code InitDataHelper}) dan bisa berupa instance
 *   <i>detached</i> dari session lama.</li>
 *   <li><b>{@link #getNama()} memangkas spasi, {@link #toString()} tidak.</b> Getter
 *   mengembalikan {@code nama.trim()} tanpa menuliskannya balik ke field; karena property
 *   access, versi ter-<i>trim</i> itulah yang dipersistensikan &mdash; jadi nilai dengan spasi
 *   ekor akan "membersihkan diri" pada update berikutnya, sementara {@link #toString()} masih
 *   memakai field mentah.</li>
 *   <li><b>{@link #getAktif()} mengubah {@code null} menjadi {@code true}.</b> Sama seperti
 *   di atas, nilai pengganti ini ikut ter-flush. Query kombo di seluruh aplikasi memakai
 *   {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} sehingga konsisten dengan
 *   getter ini; namun layar master sendiri <b>tidak</b> menyaring {@code aktif}, jadi provinsi
 *   nonaktif tetap terlihat di sana.</li>
 *   <li><b>{@link #getKodeEpsbed()} praktis mati.</b> Kolom {@code kode_epsbed} punya
 *   field/getter/setter lengkap tetapi <b>tidak ada satu pun pembaca atau penulis</b> di
 *   seluruh codebase &mdash; bahkan ekspor EPSBED/EMIS memakai {@link #getKode()}, dan sisi
 *   feeder memakai {@code Wilayah.feeder}. Kolom ini juga tidak ditampilkan di layar master.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa
 *   &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate
 *   sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib
 *   mendeklarasikan sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 *   tidak ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat baris
 *   di-<i>update</i>, bukan saat pertama dibuat. Riwayat lengkap tetap tersedia lewat
 *   {@code @Audited} (Hibernate Envers).</li>
 *   <li><b>Komentar generator "Fakultas generated by hbm2java" salah nama</b> &mdash; sisa
 *   salin-tempel template hbm2java (Des 2009); tidak ada hubungannya dengan
 *   {@link Fakultas}.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   konstruktor {@link #Propinsi()} dan {@link #Propinsi(String)}.</li>
 *   <li><b>Atribut deskriptif</b>: {@link #getKode()}/{@link #setKode(String)},
 *   {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKodeEpsbed()}/{@link #setKodeEpsbed(String)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Relasi</b>: {@link #getNegara()}/{@link #setNegara(Negara)} (induk hierarki
 *   klasik) dan {@link #getWilayah()}/{@link #setWilayah(Wilayah)} (kembaran di hierarki
 *   {@code Wilayah}).</li>
 *   <li><b>Method bisnis</b>: {@link #simpanWilayah()} &mdash; satu-satunya method yang
 *   menulis ke database dari dalam kelas ini.</li>
 * </ul>
 *
 * <p>Tidak ada method query statis di kelas ini; seluruh pencarian/penyaringan dilakukan
 * pemanggil lewat {@code Criteria} masing-masing.</p>
 *
 * @see Negara
 * @see Kota
 * @see Wilayah
 * @see #simpanWilayah()
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "propinsi")
public class Propinsi extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 5021327183727922240L;
	/** Kunci utama baris provinsi (kolom {@code id}, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String olehId;

	/** @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi: nilai lama dipertahankan alih-alih ditimpa, sehingga jejak audit terakhir
	 * tidak hilang saat interceptor dipanggil tanpa konteks pengguna (mis. proses terjadwal
	 * atau penulisan dari {@link #simpanWilayah()} yang berjalan di luar sesi UI).
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi pembuat baris tidak tercatat di
	 * kolom-kolom ini (lihat javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan
	 * field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris: {@code "<id>-<nama>"}, mis. {@code "11-Jawa Timur"}.
	 *
	 * <p>Berbeda dari {@link #getNama()}, method ini membaca <b>field mentah</b> {@code nama}
	 * sehingga spasi di awal/akhir ikut tercetak. Untuk label UI, layar master menampilkan
	 * {@link #getNama()} secara eksplisit, bukan hasil method ini.</p>
	 *
	 * @return {@code id} diikuti tanda hubung dan nama provinsi apa adanya
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode provinsi (kolom {@code kode}); dipakai ekspor EMIS dan disalin ke {@link Wilayah#getKode()}. */
	private String kode;
	/** Nama provinsi (kolom {@code nama}, panjang 150); dipakai sebagai kunci pencocokan lintas hierarki. */
	private String nama;
	/** Negara induk (kolom {@code negara}, wajib); lihat kuirk fallback di {@link #getNegara()}. */
	private Negara negara;
	/** Kode EPSBED (kolom {@code kode_epsbed}); tidak dibaca/ditulis kode mana pun &mdash; lihat javadoc kelas. */
	private String kodeEpsbed;
	/** Kembaran baris ini di hierarki {@link Wilayah} ({@code level "1"}); diisi {@link #simpanWilayah()}. */
	private Wilayah wilayah;
	/** Penanda aktif (kolom {@code aktif}); {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;

	/**
	 * Menyinkronkan baris provinsi ini ke <b>hierarki {@link Wilayah}</b> secara malas
	 * (<i>lazy</i>): memastikan ada satu baris {@code Wilayah} ber-{@code level "1"} yang
	 * mewakili provinsi ini, lalu menautkannya balik ke kolom {@code wilayah}.
	 *
	 * <p><b>Mengapa perlu.</b> AIS menyimpan wilayah administratif di dua tempat (lihat
	 * javadoc kelas). Tabel {@code propinsi}/{@code kota} adalah master lama, sedangkan tabel
	 * {@code wilayah} adalah pohon bergaya feeder yang satu-satunya menampung kecamatan.
	 * Method ini adalah jembatan arah <i>propinsi &rarr; wilayah</i>; tanpa dipanggil, provinsi
	 * yang baru dibuat tidak akan pernah muncul di pemilih kecamatan/wilayah.</p>
	 *
	 * <p><b>Alur kerja</b> (semuanya di dalam {@code HibernateUtil.currentSession()}):</p>
	 * <ol>
	 *   <li>Bila {@link #getWilayah()} sudah terisi, kandidat langsung dipakai; bila belum,
	 *   dicari baris {@code Wilayah} dengan {@code negara = }{@link #getNegara()}{@code .getKode()},
	 *   {@code level = "1"} dan {@code nama ILIKE} nama provinsi ini
	 *   ({@code ConstantValues.simpleObject(...)}, maksimal 1 hasil).</li>
	 *   <li>Bila tetap tidak ketemu, dibuat baris {@code Wilayah} baru: {@code induk = "000000"},
	 *   {@code kode}/{@code nama} disalin dari provinsi, {@code negara} diisi kode negara,
	 *   {@code level = "1"}; lalu {@code session.save(...)} + {@code session.flush()}.</li>
	 *   <li>Bila kolom {@code wilayah} provinsi ini masih kosong, hasil di atas ditautkan lewat
	 *   {@link #setWilayah(Wilayah)} dan baris provinsi di-UPDATE lewat
	 *   {@code Common.refreshUpdate(session, propinsi)} (yang juga melakukan flush dan
	 *   memperbarui identity-map).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping: method ini MENULIS ke database</b> &mdash; sisip baris
	 * {@code wilayah} dan/atau update baris {@code propinsi}. Karena
	 * {@code PropinsiAction.PropinsiRenderer.render(...)} memanggilnya untuk setiap baris grid,
	 * sekadar <i>membuka</i> layar master provinsi sudah bisa memicu penulisan.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.action.master.PropinsiAction} (saat simpan, saat
	 * setiap baris grid dirender, dan pada setiap baris hasil unggah Excel),
	 * {@link Kota#simpanWilayah()} (selalu menyinkronkan provinsi induk lebih dulu supaya
	 * {@code wilayahInduk} kota bisa diisi), serta
	 * {@code ais.action.master.helper.AmbilDataKecamatanBanbox} saat pengguna menambah
	 * negara/provinsi/kota/kecamatan baru dari dialog pemilih.</p>
	 *
	 * <p><b>Kuirk dan keterbatasan yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li><b>Seluruh exception ditelan</b> &mdash; blok {@code catch} hanya mencetak stack
	 *   trace dan mencatatnya ke {@code ErrorAuditUtil}. Kegagalan sinkronisasi tidak pernah
	 *   sampai ke pemanggil, sehingga provinsi bisa saja tetap tanpa pasangan {@code Wilayah}
	 *   tanpa ada pesan kesalahan.</li>
	 *   <li><b>Pencocokan memakai NAMA, bukan kode.</b> Baris {@code Wilayah} lama akan
	 *   dipungut bila namanya sama (tidak peka besar-kecil huruf) walaupun kodenya berbeda;
	 *   sebaliknya provinsi yang namanya sedikit berbeda ejaannya akan memunculkan baris
	 *   {@code Wilayah} kembar.</li>
	 *   <li><b>Tidak ada penyaringan {@code aktif}</b> pada pencarian kandidat, sehingga baris
	 *   {@code Wilayah} nonaktif pun bisa terpilih.</li>
	 *   <li><b>{@code wilayahInduk} sengaja/tidak sengaja dibiarkan kosong.</b> Berbeda dari
	 *   {@link Kota#simpanWilayah()} yang mengisi {@code induk} <i>dan</i> {@code wilayahInduk},
	 *   di sini hanya {@code induk} teks ({@code "000000"}) yang diisi &mdash; wajar untuk
	 *   simpul akar, tapi berarti relasi objek ke atas memang tidak pernah ada di level 1.</li>
	 *   <li><b>Bergantung pada fallback {@link #getNegara()}.</b> Provinsi tanpa negara tetap
	 *   diproses memakai {@code ConstantValues.INDONESIA}, yang kodenya dipaksa {@code "ID"}
	 *   oleh {@link Negara#getKode()}.</li>
	 * </ul>
	 */
	public void simpanWilayah() {
		try {
			Propinsi propinsi = this;
			Session session = HibernateUtil.currentSession();
			Wilayah wilayah = propinsi.getWilayah();
			if (wilayah == null) {
				wilayah = (Wilayah) ConstantValues.simpleObject(session.createCriteria(Wilayah.class)
						.add(Restrictions.eq("negara", propinsi.getNegara().getKode()))
						.add(Restrictions.eq("level", "1")).add(Restrictions.ilike("nama", propinsi.getNama()))
						.setMaxResults(1), Wilayah.class);
			}

			if (wilayah == null) {
				wilayah = new Wilayah();
				wilayah.setInduk("000000");
				wilayah.setKode(propinsi.getKode());
				wilayah.setNama(propinsi.getNama());
				wilayah.setNegara(propinsi.getNegara().getKode());
				wilayah.setLevel("1");
				session.save(wilayah);
				session.flush();
			}

			if (propinsi.getWilayah() == null) {
				propinsi.setWilayah(wilayah);
				Common.refreshUpdate(session, propinsi);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Propinsi.java:115");
		}
	}

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate; seluruh field diisi lewat setter. */
	public Propinsi() {
	}

	/**
	 * Konstruktor pintasan yang hanya mengisi nama provinsi. Dipakai kode impor/pencocokan
	 * yang membuat provinsi baru dari sebuah nama saja &mdash; perhatikan bahwa {@code kode}
	 * dan {@code negara} dibiarkan kosong, sehingga baris hasilnya bergantung penuh pada
	 * fallback {@link #getNegara()} dan akan menghasilkan {@link Wilayah} tanpa kode bila
	 * {@link #simpanWilayah()} dipanggil sebelum kodenya diisi.
	 *
	 * @param nama nama provinsi
	 */
	public Propinsi(String nama) {
		this.nama = nama;
	}

	/**
	 * @return kunci utama baris provinsi, atau {@code null} bila baris belum pernah disimpan
	 *         (dipakai layar master untuk membedakan baris baru dari baris tersimpan)
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris; normalnya hanya diisi Hibernate */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama provinsi &mdash; atribut terpenting kelas ini karena dipakai sebagai <b>kunci
	 * pencocokan</b> di banyak tempat: pencarian baris {@link Wilayah} pasangan di
	 * {@link #simpanWilayah()}, pencocokan Levenshtein di
	 * {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)}, serta pemeriksaan
	 * duplikat di layar master.
	 *
	 * <p>Getter ini <b>memangkas spasi</b> di awal/akhir ({@code trim()}) tetapi
	 * <b>tidak</b> menuliskan hasilnya kembali ke field. Karena pemetaan memakai property
	 * access, nilai ter-<i>trim</i> itulah yang dibaca Hibernate saat flush &mdash; efeknya
	 * nilai dengan spasi ekor akan "membersihkan diri" pada UPDATE berikutnya.</p>
	 *
	 * @return nama provinsi tanpa spasi di awal/akhir, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama provinsi; disimpan apa adanya (pemangkasan spasi terjadi di {@link #getNama()}) */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @param negara negara induk provinsi ini; boleh {@code null} &mdash; lihat fallback di {@link #getNegara()} */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Negara induk provinsi ini (kolom {@code negara}, {@code nullable = false} di database).
	 * Relasi lazy, sehingga getter meresolusi proxy lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)} dan <b>menetapkan hasilnya kembali ke field</b>
	 * supaya pemanggil berikutnya tidak perlu meresolusi ulang; resolusi yang gagal bersifat
	 * senyap (proxy dikembalikan apa adanya).
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}:</b> bila field kosong, getter
	 * mengembalikan singleton statis {@code ConstantValues.INDONESIA} &mdash; instance
	 * {@link Negara} yang dimuat (atau dibuat) sekali saat inisialisasi aplikasi oleh
	 * {@code ais.common.InitDataHelper}, sehingga bisa berupa objek <i>detached</i> dari
	 * session lama. Nilai pengganti itu <b>tidak</b> ditugaskan ke field, tetapi karena
	 * pemetaan memakai property access, Hibernate tetap membacanya saat dirty-check/flush:
	 * baris provinsi yang kolom {@code negara}-nya kosong akan diam-diam ter-UPDATE menjadi
	 * Indonesia. Konsumen yang ingin tahu apakah negara benar-benar diisi tidak bisa
	 * mengandalkan getter ini.</p>
	 *
	 * @return negara induk, atau {@code ConstantValues.INDONESIA} bila kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara", nullable = false)
	public Negara getNegara() {
		negara = check(negara);
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/** @param kodeEpsbed kode EPSBED provinsi; tersimpan tetapi tidak pernah dibaca kode mana pun */
	public void setKodeEpsbed(String kodeEpsbed) {
		this.kodeEpsbed = kodeEpsbed;
	}

	/**
	 * Kode provinsi versi EPSBED (kolom {@code kode_epsbed}).
	 *
	 * <p><b>Peninggalan yang tidak terpakai.</b> Tidak ada satu pun pembaca maupun penulis
	 * properti ini di luar kelas ini: layar master tidak menampilkannya, ekspor EPSBED/EMIS
	 * memakai {@link #getKode()}, dan integrasi feeder memakai {@code Wilayah.feeder}. Kolomnya
	 * tetap dipertahankan karena {@code @Audited} dan data lama yang mungkin sudah terisi.</p>
	 *
	 * @return kode EPSBED apa adanya, atau {@code null}
	 */
	@Column(name = "kode_epsbed")
	public String getKodeEpsbed() {
		return kodeEpsbed;
	}

	/**
	 * Kode provinsi (kolom {@code kode}, tanpa anotasi {@code @Column} eksplisit sehingga
	 * memakai penamaan bawaan). Disalin ke {@link Wilayah#getKode()} oleh
	 * {@link #simpanWilayah()} dan diekspor apa adanya oleh laporan EMIS
	 * ({@code LaporanFormatEMIS}, {@code LaporanFormatEMISDosen}). Juga dipakai sebagai kunci
	 * pencocokan saat sinkronisasi PMB Arkatama dan sebagai kolom pencarian di layar master.
	 *
	 * <p>Tidak ada normalisasi apa pun di sini: nilai dikembalikan mentah, termasuk spasi.</p>
	 *
	 * @return kode provinsi, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/** @param kode kode provinsi; tidak divalidasi maupun dinormalisasi */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Kembaran baris ini di hierarki {@link Wilayah} (baris ber-{@code level "1"}), kolom
	 * {@code wilayah} yang bersifat {@code nullable}. Relasi lazy yang diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan <b>ditetapkan kembali ke field</b>;
	 * kegagalan resolusi bersifat senyap.
	 *
	 * <p>Berbeda dari {@link #getNegara()}, getter ini <b>tidak</b> punya nilai pengganti:
	 * {@code null} berarti provinsi ini belum pernah disinkronkan. Tautan diisi otomatis oleh
	 * {@link #simpanWilayah()}, dan dibaca {@link Kota#simpanWilayah()} untuk menentukan
	 * {@code wilayahInduk} baris kota ({@code level "2"}) &mdash; bila masih {@code null},
	 * kota yang dibuat akan berakhir dengan {@code induk} berupa string kosong.</p>
	 *
	 * @return baris {@code Wilayah} level 1 pasangan provinsi ini, atau {@code null} bila belum ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wilayah", nullable = true)
	public Wilayah getWilayah() {
		wilayah = check(wilayah);
		return wilayah;
	}

	/** @param wilayah baris {@code Wilayah} level 1 pasangan provinsi ini; normalnya hanya diisi {@link #simpanWilayah()} */
	public void setWilayah(Wilayah wilayah) {
		this.wilayah = wilayah;
	}

	/**
	 * Penanda apakah provinsi masih dipakai (kolom {@code aktif}).
	 *
	 * <p><b>{@code null} diperlakukan sebagai {@code true}</b> &mdash; data lama yang dibuat
	 * sebelum kolom ini ada tetap dianggap aktif. Seperti {@link #getNegara()}, nilai pengganti
	 * ini ikut terbaca Hibernate saat flush, sehingga kolom {@code NULL} akan menjadi
	 * {@code TRUE} pada UPDATE berikutnya. Semua kombo pemilih provinsi menyaring dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} &mdash; konsisten dengan
	 * getter ini &mdash; tetapi <b>layar master dan statistik pendaftar tidak menyaring
	 * {@code aktif} sama sekali</b>, jadi provinsi nonaktif tetap muncul di sana.</p>
	 *
	 * @return {@code true} bila aktif atau kolomnya kosong, {@code false} bila dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda aktif; disetel dari checkbox di grid layar master */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
