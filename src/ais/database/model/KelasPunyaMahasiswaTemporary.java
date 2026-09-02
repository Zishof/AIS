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

/**
 * <b>Tabel antrean (staging/inbox) penempatan mahasiswa ke kelas paralel akademik.</b> Satu baris
 * menyatakan "mahasiswa {@code M} hendak ditempatkan pada kelas {@code K}", untuk kemudian
 * <i>dikonsumsi sekali</i> oleh proses batch
 * {@code ais.action.master.helper.util.JamPerkuliahanSyncrhonizerProcessor#procesKelas()} dan
 * ditandai selesai lewat {@link #getUdah() udah}. Memetakan tabel {@code public.kelas_punya_mahasiswa}
 * (perhatikan: nama tabel <b>tidak</b> memuat kata "temporary"; hanya nama kelas Java yang memuatnya).
 *
 * <h3>Kenapa namanya "Temporary" &mdash; dan apa artinya secara teknis</h3>
 *
 * <p>Yang bersifat sementara adalah <b>barisnya</b>, bukan entity-nya. Entity ini sendiri permanen,
 * terpetakan penuh, dan bahkan ikut di-audit Envers ({@link Audited}). Namun setiap baris hanyalah
 * <i>perintah kerja sekali pakai</i>:</p>
 *
 * <ol>
 *   <li>Baris masuk dengan {@link #getUdah() udah} bernilai {@code null} (belum diproses).</li>
 *   <li>{@code procesKelas()} mengambil semua baris ber-{@code udah} {@code null}, menyalin
 *   penempatan kelas ke properti <b>teks</b> {@code Mahasiswa.kelas}, menyimpan mahasiswanya, lalu
 *   menyetel {@code udah = true} pada baris ini.</li>
 *   <li>Sejak saat itu baris menjadi inert &mdash; tetap ada sebagai jejak, tetapi tidak pernah
 *   dibaca lagi oleh proses mana pun.</li>
 * </ol>
 *
 * <p>Jadi entity ini adalah <b>jembatan satu arah</b> menuju sumber kebenaran yang sebenarnya, yaitu
 * kolom teks {@code Mahasiswa.kelas}. Ia bukan tabel penghubung yang menyimpan keanggotaan kelas
 * secara berkelanjutan.</p>
 *
 * <h3>Verifikasi: FK sungguhan, tetapi BUKAN sumber kebenaran keanggotaan</h3>
 *
 * <p>{@link Kelas} mencatat entity ini sebagai salah satu dari lima pemilik <i>foreign key</i>
 * sungguhan ke tabel {@code kelas}, dan itu benar: {@link #getKelas()} dan {@link #getMahasiswa()}
 * keduanya {@code @ManyToOne} dengan {@code nullable = false}. Ini satu-satunya tempat di seluruh
 * codebase yang menyatakan pasangan kelas&harr;mahasiswa sebagai relasi objek sungguhan.</p>
 *
 * <p><b>Namun relasi itu tidak dipakai sebagai keanggotaan.</b> Seluruh layar, laporan dan query
 * keanggotaan kelas tetap memakai pencocokan teks {@code Mahasiswa.kelas} seperti dijelaskan pada
 * javadoc {@link Kelas} (mis. {@code Restrictions.ilike("kelas", kelas.getNama(), MatchMode.EXACT)}).
 * Entity ini hanya menjadi jalur <i>masuk</i>-nya. Konsekuensi praktis: menghapus atau mengubah baris
 * di sini tidak memindahkan mahasiswa ke kelas lain, dan sebaliknya memindahkan mahasiswa lewat layar
 * kelas tidak memperbarui baris di sini.</p>
 *
 * <h3>Jangan tertukar dengan {@code KelasPunyaMahasiswaHelper}</h3>
 *
 * <p>{@code ais.action.master.helper.KelasPunyaMahasiswaHelper} (layar "Mahasiswa pada Kelas" di
 * dalam {@code ais.action.master.KelasAction}) bernama nyaris sama tetapi <b>sama sekali tidak
 * memakai entity ini</b> &mdash; layar itu bekerja langsung di atas kolom teks {@code Mahasiswa.kelas}
 * (termasuk fitur "Bersihkan", "Singkronkan" dan upload Excel-nya). Kemiripan nama adalah warisan
 * penamaan tabel, bukan tanda keterkaitan kode.</p>
 *
 * <h3>Asal-usul: salinan struktur keluarga {@code *Punya*}</h3>
 *
 * <p>Struktur kelas ini identik dengan {@link AsramaPunyaMahasiswa} &mdash; sampai ke nilai
 * {@code serialVersionUID} yang sama persis &mdash; dengan dua perbedaan saja: relasi {@code asrama}
 * diganti {@link #getKelas() kelas}, dan ditambahkan penanda {@link #getUdah() udah}. Field
 * {@link #getDiubahDari() diubahDari}, {@link #getUnique_id() unique_id} dan
 * {@link #getTbmuser() tbmuser} adalah bawaan keluarga tabel penghubung {@code *Punya*}
 * ({@link AsramaPunyaMahasiswa}, {@link KegiatanKemahasiswaanPunyaMahasiswa},
 * {@link OrganisasiIntraKampusPunyaMahasiswa}, dan seterusnya), bukan kebutuhan proses migrasi di
 * sini. Karena itu ketiganya <b>tidak pernah diisi</b> oleh kode mana pun untuk entity ini.</p>
 *
 * <h3>Siapa yang membuat barisnya?</h3>
 *
 * <p>Tidak ada satu pun kode Java di repo ini yang melakukan {@code new
 * KelasPunyaMahasiswaTemporary()} atau menyimpannya. Konstruktor {@link
 * #KelasPunyaMahasiswaTemporary()} hanya dipakai Hibernate saat memuat baris. Artinya baris hanya
 * bisa lahir dari luar aplikasi: skrip SQL, impor/migrasi basis data, atau alat administrasi
 * langsung. Kode aplikasi hanya berperan sebagai <i>konsumen</i>.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 *
 * <ul>
 *   <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Muatan penempatan (inti entity)</b>: {@link #getKelas()}, {@link #getMahasiswa()}.</li>
 *   <li><b>Kendali proses batch</b>: {@link #getUdah()}.</li>
 *   <li><b>Kunci alami turunan</b>: {@link #getUnique_id()}.</li>
 *   <li><b>Bawaan keluarga {@code *Punya*} yang tidak terpakai di sini</b>:
 *   {@link #getDiubahDari()}, {@link #getTbmuser()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #getUnique_id()} adalah getter yang MENULIS.</b> Ia menghitung ulang
 *   {@code <idMahasiswa>_<idKelas>} lalu <b>menimpa</b> field {@code unique_id} setiap kali dipanggil.
 *   Karena pemetaan memakai <i>property access</i> (anotasi menempel di getter), nilai hasil hitung
 *   itulah yang dibaca Hibernate saat menyusun snapshot dan saat INSERT/UPDATE &mdash; jadi getter ini
 *   ikut menentukan isi database, bukan sekadar isi memori. Kolomnya {@code unique = true}, sehingga
 *   basis data secara efektif menolak dua baris antrean untuk pasangan mahasiswa&harr;kelas yang sama.
 *   Nilai yang disetel manual lewat {@link #setUnique_id(String)} akan hilang ditimpa begitu kedua
 *   relasi terisi.</li>
 *   <li><b>{@link #getTbmuser()} melakukan de-proxy, bukan penghapusan.</b> Getter memanggil
 *   {@code GeneralValueObject.check(...)} lalu menulis balik hasilnya ke field. Ini resolusi
 *   proxy/cache identitas yang aman &mdash; <b>bukan</b> getter destruktif seperti
 *   {@code Komentar.getTbmuser()}. Tidak ada satu pun getter di kelas ini yang menutup sesi Hibernate,
 *   dan tidak ada yang menghapus data.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan sendiri
 *   kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Konsekuensi butir 3 yang berdampak nyata: properti induk yang TIDAK dideklarasikan ulang
 *   selalu bernilai {@code null}.</b> Kelas ini tidak mendeklarasikan ulang {@code nama}, sehingga
 *   {@code getNama()} warisan {@link GeneralValueObject#getNama()} tidak pernah terisi dari basis data
 *   dan selalu mengembalikan {@code null} untuk baris yang dimuat Hibernate. Ini penting karena
 *   {@code procesKelas()} justru memakai {@code getNama()} sebagai sumber nama kelas
 *   ({@code mahasiswa.setKelas(kelasPunyaMahasiswaTemporary.getNama())}) &mdash; lihat catatan bug di
 *   bawah. Nilai kelas yang benar tersedia lewat {@code getKelas().getNama()}.</li>
 *   <li><b>{@link #getUdah()} bersifat tri-state, dan hanya {@code null} yang berarti "antre".</b>
 *   Query pemilih baris di {@code procesKelas()} memakai {@code Restrictions.isNull("udah")} saja,
 *   berbeda dari proses saudaranya ({@code processMigrasiEkivalen}) yang memakai
 *   {@code or(isNull, eq(false))}. Baris yang secara eksplisit disetel {@code false} karena itu
 *   <b>tidak akan pernah</b> ikut diproses &mdash; untuk mengantrekan ulang sebuah baris, nilainya
 *   harus dikembalikan ke {@code null}, bukan {@code false}.</li>
 *   <li><b>{@link #toString()} membaca field, bukan getter.</b> Ia merangkai {@code kelas} dan
 *   {@code mahasiswa} apa adanya sehingga bisa menghasilkan teks yang memuat {@code "null"} untuk
 *   object yang belum diisi, dan tidak melewati {@code check(...)}.</li>
 *   <li><b>Nama kolom untuk properti tanpa {@code @Column}.</b> {@code diubahDari} dan {@code udah}
 *   tidak punya {@code @Column}, sehingga nama kolomnya mengikuti nama properti apa adanya
 *   ({@code diubahdari}, {@code udah} di PostgreSQL) lewat
 *   {@code ais.database.hibernate.MyNamingStrategy}.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak ada
 *   {@code @PrePersist}: kolom {@code oleh}/{@code olehId} baru terisi saat baris di-UPDATE (yakni
 *   ketika {@code procesKelas()} menandainya selesai), bukan saat baris dibuat. Karena baris memang
 *   dibuat dari luar aplikasi, nilai {@code oleh} yang akhirnya tersimpan adalah identitas pemroses
 *   batch, bukan identitas pengaju penempatan.</li>
 * </ol>
 *
 * <h3>Catatan bug yang teramati (dilaporkan apa adanya, tidak diperbaiki di sini)</h3>
 *
 * <p>Pada {@code JamPerkuliahanSyncrhonizerProcessor#procesKelas()} baris penerapan berbunyi
 * {@code mahasiswa.setKelas(kelasPunyaMahasiswaTemporary.getNama())}. Sesuai butir 4 di atas,
 * {@code getNama()} pada entity ini <b>selalu {@code null}</b> karena properti {@code nama} tidak
 * terpetakan. Akibatnya proses batch justru <b>mengosongkan</b> {@code Mahasiswa.kelas} alih-alih
 * mengisinya, lalu menandai barisnya {@code udah = true} sehingga penempatan itu tidak pernah bisa
 * diulang tanpa intervensi manual. Nilai yang dimaksud semestinya {@code getKelas().getNama()}.
 * Bentuk kesalahan yang setara juga terlihat pada proses saudaranya {@code procesDosenPa()}, yang
 * memakai {@code setDosen(dosenPembimbingAkademikTemporary.getId())} &mdash; id baris antrean itu
 * sendiri &mdash; alih-alih id dosennya.</p>
 *
 * <h3>Kontrol akses</h3>
 *
 * <p>Tidak ada layar ZK yang mengelola entity ini secara langsung. Satu-satunya permukaan yang
 * menyebutkannya adalah scaffold "new UI" {@code /WEB-INF/new/helper/services/util/
 * jam_perkuliahan_syncrhonizer_processor_service.jsp}, yang hanya mendaftarkan nama method/entity
 * sebagai metadata dan berjalan di belakang {@code dispatcher.jsp} &mdash; dispatcher itu menolak
 * permintaan tanpa {@code mytbmuser} di sesi (HTTP 401) dan memeriksa {@code NewUiRouteGuard
 * .isActionAuthorized} (HTTP 403). Untuk entity ini jalur tersebut merupakan contoh <b>positif</b>:
 * bergerbang login sekaligus otorisasi aksi.</p>
 *
 * @see Kelas
 * @see Mahasiswa
 * @see AsramaPunyaMahasiswa
 * @see DosenPembimbingAkademikTemporary
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelas_punya_mahasiswa")

public class KelasPunyaMahasiswaTemporary extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama persis dengan {@link AsramaPunyaMahasiswa} karena kelas
	 * ini disalin dari sana; kesamaan itu tidak berdampak fungsional (serialisasi selalu menyertakan
	 * nama kelas), tetapi menjadi petunjuk asal-usul kode.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.kelas_punya_mahasiswa}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris; diisi {@link #onUpdate()}. */
	private String olehId;

	/** @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi: nilai lama dipertahankan alih-alih ditimpa, sehingga jejak audit terakhir tidak
	 * hilang saat interceptor dipanggil tanpa konteks pengguna &mdash; kasus yang relevan di sini,
	 * karena baris entity ini justru diperbarui oleh proses batch/thread latar.
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
	 * di-UPDATE. Untuk entity ini pemicu UPDATE yang realistis hanyalah penandaan
	 * {@code udah = true} oleh {@code procesKelas()}. Tidak ada padanan {@code @PrePersist}, jadi
	 * pembuat baris tidak pernah tercatat (lihat javadoc kelas). Pada baris deklarasi yang sama juga
	 * dideklarasikan field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat object
	 * dibuat ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga object baru tetap punya stempel waktu
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
	 * Representasi teks baris antrean: {@code "<kelas> - <mahasiswa>"}. Membaca <b>field</b>
	 * {@code kelas}/{@code mahasiswa} langsung, bukan lewat {@link #getKelas()}/{@link #getMahasiswa()},
	 * sehingga tidak melewati resolusi proxy dan dapat menghasilkan teks yang memuat {@code "null"}
	 * untuk object yang belum diisi (mis. hasil {@code new KelasPunyaMahasiswaTemporary()}).
	 * Menimpa {@code GeneralValueObject.toString()} yang biasanya merangkai kode dan nama &mdash;
	 * bentuk bawaan itu tidak berguna di sini karena kedua properti tersebut tidak terpetakan.
	 *
	 * @return teks gabungan kelas dan mahasiswa; tidak pernah {@code null}
	 */
	public String toString() {
		return kelas + " - " + mahasiswa;
	}

	/** Kelas paralel tujuan penempatan; FK {@code kelas} (wajib). Lihat {@link #getKelas()}. */
	private Kelas kelas;
	/** Mahasiswa yang ditempatkan; FK {@code mahasiswa} (wajib). Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Penanda layar asal perubahan, bawaan keluarga {@code *Punya*}; tidak pernah diisi untuk entity ini. */
	private String diubahDari;

	/** Kunci alami turunan {@code <idMahasiswa>_<idKelas>}; dihitung ulang oleh {@link #getUnique_id()}. */
	private String unique_id;

	/** Akun pengguna terkait, bawaan keluarga {@code *Punya*}; tidak pernah diisi untuk entity ini. */
	private Tbmuser tbmuser;
	/** Penanda baris sudah diproses batch; {@code null} = masih antre. Lihat {@link #getUdah()}. */
	private Boolean udah;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate untuk membentuk instance saat memuat baris.
	 * Tidak ada kode aplikasi yang memanggilnya secara langsung &mdash; baris antrean dibuat dari
	 * luar aplikasi (lihat javadoc kelas).
	 */
	public KelasPunyaMahasiswaTemporary() {
	}

	/**
	 * @return kunci utama baris antrean ini (kolom {@code id}, IDENTITY, {@code insertable = false}
	 *         sehingga nilainya selalu ditentukan basis data)
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris; normalnya hanya disetel Hibernate setelah INSERT */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kelas paralel tujuan penempatan &mdash; <b>foreign key sungguhan</b> ke {@link Kelas} (kolom
	 * {@code kelas}, {@code nullable = false}), berbeda dari keanggotaan kelas pada umumnya yang
	 * disimpan sebagai teks di {@code Mahasiswa.kelas}.
	 *
	 * <p>Dimuat dengan {@code FetchMode.SELECT} (query terpisah, bukan {@code JOIN} pada query induk)
	 * dan meng-<i>cascade</i> {@code PERSIST}/{@code MERGE}, sehingga menyimpan baris antrean ini
	 * dapat ikut menyimpan/menggabungkan object {@link Kelas} yang menempel padanya. Nama kelas yang
	 * benar untuk disalin ke {@code Mahasiswa.kelas} berasal dari {@code getKelas().getNama()}
	 * &mdash; lihat catatan bug pada javadoc kelas.</p>
	 *
	 * @return kelas tujuan; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kelas", nullable = false)
	public Kelas getKelas() {
		return kelas;
	}

	/** @param kelas kelas paralel tujuan penempatan; wajib terisi sebelum baris disimpan */
	public void setKelas(Kelas kelas) {
		this.kelas = kelas;
	}

	/**
	 * Mahasiswa yang akan ditempatkan &mdash; foreign key ke {@link Mahasiswa} (kolom
	 * {@code mahasiswa}, {@code nullable = false}), dimuat {@code FetchMode.SELECT} dengan cascade
	 * {@code PERSIST}/{@code MERGE}. Inilah object yang diambil {@code procesKelas()} untuk kemudian
	 * diubah kolom {@code kelas}-nya dan disimpan.
	 *
	 * @return mahasiswa yang ditempatkan; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa yang akan ditempatkan; wajib terisi sebelum baris disimpan */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Kunci alami turunan berbentuk {@code "<idMahasiswa>_<idKelas>"}, dipetakan ke kolom
	 * {@code unique_id} dengan batasan {@code unique = true} sehingga basis data menolak dua baris
	 * antrean untuk pasangan mahasiswa&harr;kelas yang sama.
	 *
	 * <p><b>Getter ini MENULIS.</b> Setiap pemanggilan menghitung ulang nilai dari kedua relasi dan
	 * <b>menimpa</b> field {@code unique_id}. Karena pemetaan memakai <i>property access</i>, nilai
	 * hasil hitung itulah yang dibaca Hibernate saat menyusun snapshot dan saat INSERT/UPDATE &mdash;
	 * dengan kata lain nilai apa pun yang disetel lewat {@link #setUnique_id(String)} akan hilang
	 * tertimpa begitu {@link #getKelas()} dan {@link #getMahasiswa()} sama-sama terisi. Bila salah
	 * satu relasi masih {@code null}, nilai lama dipertahankan apa adanya (bisa {@code null}), dan
	 * INSERT akan menulis {@code null} &mdash; yang tetap lolos karena kolom {@code unique} tanpa
	 * {@code not null} mengizinkan banyak baris {@code null} di PostgreSQL.</p>
	 *
	 * <p>Perhatikan pula bahwa nilai ini memakai <i>id</i> mahasiswa dan kelas, bukan nama; mengganti
	 * nama kelas karena itu tidak mengubah kunci ini.</p>
	 *
	 * @return kunci gabungan {@code "<idMahasiswa>_<idKelas>"}, atau nilai lama/{@code null} bila
	 *         salah satu relasi belum terisi
	 */
	@Column(unique = true)
	public String getUnique_id() {
		if (mahasiswa != null && kelas != null) {
			unique_id = mahasiswa.getId() + "_" + kelas.getId();
		}
		return unique_id;
	}

	/**
	 * Menyetel kunci alami secara manual. Jarang berguna: nilainya akan ditimpa oleh
	 * {@link #getUnique_id()} segera setelah kedua relasi terisi.
	 *
	 * @param unique_id kunci gabungan yang ingin dipaksakan
	 */
	public void setUnique_id(String unique_id) {
		this.unique_id = unique_id;
	}

	/**
	 * Akun pengguna yang menempel pada baris (kolom {@code tbmuser}, boleh {@code null}), bawaan
	 * struktur keluarga {@code *Punya*}. Tidak ada kode yang mengisinya untuk entity ini, sehingga
	 * praktisnya selalu {@code null}.
	 *
	 * <p>Getter memanggil {@code GeneralValueObject.check(...)} lalu <b>menulis balik</b> hasilnya ke
	 * field. Ini semata-mata resolusi proxy lazy / pencocokan cache identitas entity agar pemanggil
	 * tidak menerima proxy yang gagal di-inisialisasi &mdash; <b>bukan</b> operasi destruktif dan
	 * <b>tidak</b> menutup sesi Hibernate. {@code check(...)} tidak pernah melempar exception dan
	 * mengembalikan argumen apa adanya bila resolusi gagal.</p>
	 *
	 * @return akun pengguna terkait yang sudah teresolusi, atau {@code null}
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/** @param tbmuser akun pengguna yang ingin ditautkan ke baris ini; boleh {@code null} */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Penanda layar/aksi asal perubahan (kolom {@code diubahdari}), bawaan keluarga tabel penghubung
	 * {@code *Punya*} yang di tempat lain diisi dengan {@code XxxAction.class.getSimpleName()} agar
	 * terlihat dari sisi mana relasi dua arah tersebut disunting. Untuk entity ini tidak ada satu pun
	 * pemanggil {@link #setDiubahDari(String)}, jadi nilainya selalu {@code null}.
	 *
	 * @return nama layar asal perubahan, praktisnya selalu {@code null}
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/** @param diubahDari nama sederhana kelas Action asal perubahan; tidak dipakai untuk entity ini */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * Penanda apakah baris antrean ini sudah diproses batch (kolom {@code udah}).
	 *
	 * <p><b>Tri-state, dan hanya {@code null} yang berarti "antre".</b>
	 * {@code JamPerkuliahanSyncrhonizerProcessor#procesKelas()} memilih kandidat dengan
	 * {@code Restrictions.isNull("udah")} saja, lalu menyetel {@code true} setelah penempatan
	 * diterapkan. Nilai {@code false} yang eksplisit karena itu <b>tidak pernah</b> terjaring &mdash;
	 * berbeda dari proses saudaranya {@code processMigrasiEkivalen()} yang memakai
	 * {@code or(isNull, eq(false))}. Untuk mengantrekan ulang sebuah baris, nilainya harus
	 * dikembalikan ke {@code null}.</p>
	 *
	 * @return {@code true} bila sudah diproses; {@code null} bila masih antre; {@code false} berarti
	 *         baris tidak akan pernah diproses
	 */
	public Boolean getUdah() {
		return udah;
	}

	/**
	 * Menyetel penanda selesai. Dipanggil {@code procesKelas()} dengan {@code true} setelah kelas
	 * disalin ke {@code Mahasiswa.kelas} dan mahasiswanya tersimpan; penyimpanan setelahnya memicu
	 * {@link #onUpdate()} sehingga kolom audit ikut terisi identitas pemroses batch.
	 *
	 * @param udah {@code true} untuk menandai selesai; {@code null} untuk mengantrekan ulang
	 *             (nilai {@code false} membuat baris tak pernah terjaring, lihat {@link #getUdah()})
	 */
	public void setUdah(Boolean udah) {
		this.udah = udah;
	}

}
