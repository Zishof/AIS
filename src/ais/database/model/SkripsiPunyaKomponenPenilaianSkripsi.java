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
 * Tabel penghubung <b>format nilai skripsi &harr; komponen penilaian skripsi</b>: satu baris
 * {@code skripsi_punya_komponen_penilaian_skripsi} menyatakan "butir penilaian X ikut dipakai pada
 * format sidang Y". Isinya persis dua kolom yang berarti — {@link #getFormatNilaiSkripsi()} dan
 * {@link #getKomponenPenilaianSkripsi()} — plus sekumpulan kolom pelengkap yang, seperti diuraikan
 * di bawah, tidak pernah dibaca siapa pun.
 *
 * <h3>Namanya menyesatkan: entity ini TIDAK menyimpan nilai mahasiswa mana pun</h3>
 * <p>Awalan {@code Skripsi...} pada nama class dan nama tabel membuatnya tampak seperti data
 * transaksional per mahasiswa (baris nilai per komponen per dosen penguji). <b>Bukan.</b> Tidak ada
 * satu pun relasi ke {@link Skripsi}, ke {@link Mahasiswa}, maupun ke {@link Dosen} di sini, dan
 * tidak ada kolom bertipe angka nilai. Baris ini murni <i>master</i>: bagian dari konfigurasi
 * {@link FormatNilaiSkripsi}, sama sekali tidak bergantung pada mahasiswa. Prefiks
 * {@code Skripsi} adalah sisa penamaan lama — pemakai kode di {@code FormatNilaiSkripsiAction}
 * bahkan menamai variabel lokalnya {@code formatNilaiSkripsiPunyaKomponenPenilaianSkripsis}, yang
 * jauh lebih tepat menggambarkan isinya.</p>
 *
 * <p>Nilai skripsi yang sesungguhnya tidak disimpan sebagai baris tabel sama sekali, melainkan
 * sebagai <b>satu string CSV</b> pada kolom {@code Skripsi.detail_nilai} dengan format
 * {@code idKomponen,idDosen,nilai,0,bobot,sudahVerifikasi} yang dipisahkan titik koma (lihat
 * {@link Skripsi#retreiveDetailNilai(KomponenPenilaianSkripsi, Dosen)} dan
 * {@code Skripsi.populateDetailNilai(...)}). Jadi
 * tabel ini hanya menjawab pertanyaan "komponen apa saja yang harus dirender sebagai baris entri
 * nilai", bukan "berapa nilainya".</p>
 *
 * <h3>Siapa yang menulis</h3>
 * <p>Satu-satunya penulis adalah {@code FormatNilaiSkripsiAction.simpan()}, dan caranya kasar:
 * seluruh baris milik format yang sedang disunting <b>dihapus lewat SQL mentah</b>
 * ({@code delete from skripsi_punya_komponen_penilaian_skripsi where format_nilai_skripsi=<id>})
 * lalu daftar centang di layar disimpan ulang satu per satu sebagai baris baru. Konsekuensinya:</p>
 * <ul>
 * <li><b>Id baris berubah total setiap kali format disimpan</b>, walau isinya sama persis. Jangan
 * pernah menyimpan id baris ini sebagai referensi jangka panjang di tempat lain — lihat bug yang
 * dicatat pada {@link Skripsi#retreiveDetailVerifikasiNilai(SkripsiPunyaKomponenPenilaianSkripsi, Dosen)}
 * yang justru melakukan hal itu.</li>
 * <li><b>{@code @Audited} (Hibernate Envers) hanya merekam separuh cerita.</b> Envers menangkap
 * {@code INSERT} yang lewat session Hibernate, tetapi {@code DELETE} SQL mentah di atas melewati
 * Envers sepenuhnya. Tabel revisi karena itu berisi tumpukan penambahan tanpa penghapusan
 * pasangannya, dan tidak dapat dipakai merekonstruksi susunan komponen pada tanggal tertentu.</li>
 * <li>Hanya {@link #setKomponenPenilaianSkripsi(KomponenPenilaianSkripsi)},
 * {@link #setNama(String)} dan {@link #setFormatNilaiSkripsi(FormatNilaiSkripsi)} yang dipanggil
 * saat menyimpan; seluruh property lain di class ini tidak pernah diisi.</li>
 * </ul>
 *
 * <h3>Siapa yang membaca</h3>
 * <ol>
 * <li>{@code FormatNilaiSkripsiAction} — memuat daftar komponen yang sudah tercentang agar layar
 * master format menampilkan keadaan tersimpan.</li>
 * <li>{@code PenilaianSkripsiHelper.populateKomponen(String)} — menentukan baris komponen yang
 * muncul di window entri nilai untuk satu peran dosen. Perhatikan: penyaringan "komponen ini boleh
 * dinilai oleh slot dosen ke-berapa" <b>tidak</b> dilakukan lewat kolom di class ini, melainkan
 * lewat {@code KomponenPenilaianSkripsi.dosen1}..{@code dosen7} (lihat bagian verifikasi di bawah).
 * Baris tabel ini hanya dipakai sebagai penyaring keanggotaan format.</li>
 * <li>{@link Skripsi#refreshNilaiKeDefault(Dosen)} dan
 * {@link Skripsi#bersihkanNilaiKeDefault()} — membangun/membersihkan CSV {@code detailNilai}
 * berdasarkan komponen yang berlaku.</li>
 * <li>{@code Skripsi.reloadSkripsiPunyaKomponenPenilaianSkripsi(Session, Dosen)} — <b>kode mati
 * yang sekaligus rusak</b>: kriterianya menyebut property {@code parent}, {@code persen}, dan
 * {@code statusPertemuan} yang <b>tidak ada</b> di class ini (tampaknya hasil salin-tempel dari
 * entity komponen/pertemuan), sehingga andai method itu pernah dipanggil, Hibernate dipastikan
 * melempar {@code QueryException: could not resolve property}. Method tersebut memang tidak
 * dipanggil dari mana pun di pohon sumber saat ini.</li>
 * </ol>
 * <p>Seluruh pembacaan di atas hanya memakai {@code formatNilaiSkripsi} dan
 * {@code komponenPenilaianSkripsi}. Tidak satu pun membaca {@link #getNama()},
 * {@link #getKeterangan()}, atau keenam bendera {@code getProsentasiNilai*()}.</p>
 *
 * <h3>Verifikasi "bug slot dosen 1/2 tertukar" — hasilnya di sini</h3>
 * <p>Pada {@link FormatNilaiSkripsi} (sisi master) dan {@link Skripsi} (sisi data per mahasiswa)
 * telah dicatat penamaan yang tergeser: bobot/nilai slot dosen pertama (label default
 * "Pembimbing I") disimpan di kolom bernama {@code ...ketua_sidang}, dan slot kedua ("Pembimbing
 * II") di kolom bernama {@code ...pembimbing}. Pertanyaannya: apakah pergeseran itu ikut menular ke
 * class ini? <b>Kosakata namanya menular, tetapi dampaknya nol.</b> Rinciannya:</p>
 * <ul>
 * <li><b>Menular secara nama.</b> Keenam bendera di sini bernama
 * {@code prosentasiNilaiKetuaSidang}, {@code prosentasiNilaiPembimbing}, dan
 * {@code prosentasiNilaiPenguji1}..{@code 4} — persis kosakata yang tergeser itu, termasuk pasangan
 * "ketua sidang untuk slot 1 / pembimbing untuk slot 2". Bila kelak seseorang menghidupkan kolom
 * ini, ia akan mewarisi kebingungan yang sama.</li>
 * <li><b>Dampaknya nol karena kolomnya benar-benar mati.</b> Penelusuran seluruh pohon sumber
 * menunjukkan keenam getter/setter tersebut <b>tidak pernah dipanggil dari mana pun</b> — semua
 * pemanggilan {@code getProsentasiNilai*()} di aplikasi mengenai object
 * {@link FormatNilaiSkripsi}/{@link FormatNilaiProposalSkripsi}, bukan object class ini. Kolomnya
 * juga tidak pernah diisi saat menyimpan, jadi isinya selalu {@code NULL} di database dan getter-nya
 * selalu mengembalikan {@code true} bawaan.</li>
 * <li><b>Tipenya pun sudah berbeda arti.</b> Di {@link FormatNilaiSkripsi} property senama bertipe
 * {@code Double} (bobot persen); di sini bertipe {@code Boolean}. Jadi meski namanya sama, ini
 * jelas dimaksudkan sebagai bendera "komponen ini boleh dinilai oleh slot dosen X", bukan bobot —
 * nama {@code prosentasi...} sudah salah sejak awal terlepas dari soal slot tertukar.</li>
 * <li><b>Jumlah slotnya tidak lengkap.</b> Hanya ada 6 bendera untuk 8 slot dosen yang dikenal
 * {@link FormatNilaiSkripsi}: tidak ada padanan untuk {@code dosen21} (Pembimbing III) maupun
 * {@code dosen7} (Penguji V). Bukti tambahan bahwa rancangan ini ditinggalkan setengah jalan.</li>
 * <li><b>Mekanisme penggantinya justru bersih.</b> Penyaringan per slot dosen yang benar-benar
 * berjalan ada di {@link KomponenPenilaianSkripsi}, dengan kolom {@code dosen1}..{@code dosen7} yang
 * dinomori lurus sesuai slot — tanpa jejak pergeseran nama sama sekali. Kolom itulah yang dipakai
 * {@code PenilaianSkripsiHelper.populateKomponen(String)}.</li>
 * </ul>
 * <p><b>Kesimpulan verifikasi:</b> bug slot-swap dosen/nilai <b>tidak bermanifestasi</b> di class
 * ini. Yang ada hanyalah warisan penamaan pada enam kolom mati; tidak ada nilai, bobot, maupun
 * keputusan tampilan yang bisa salah karenanya. (Catatan terpisah: {@code populateKomponen} tetap
 * tidak mengenali slot {@code dosen21}, tetapi itu cacat pemetaan label di helper, bukan cacat
 * class ini — sudah dicatat pada Javadoc {@link FormatNilaiSkripsi}.)</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Relasi inti (satu-satunya bagian yang hidup)</b> — {@link #getFormatNilaiSkripsi()},
 * {@link #getKomponenPenilaianSkripsi()} dan setter-nya.</li>
 * <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #toString()}, {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * <li><b>Label deskriptif</b> — {@link #getNama()}, {@link #getKeterangan()}; keduanya hanya
 * disimpan, tidak pernah dibaca.</li>
 * <li><b>Bendera kebolehan per slot dosen (mati)</b> — enam pasang
 * {@code getProsentasiNilai*()}/{@code setProsentasiNilai*()}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious lain</h3>
 * <ul>
 * <li><b>Field audit di-shadow, dan itu KEHARUSAN teknis.</b> {@code id}, {@code oleh},
 * {@code olehId} dan {@code tanggal_dirubah} dideklarasikan ulang di sini padahal
 * {@link GeneralValueObject} punya field bernama sama. Induknya bukan {@code @Entity} maupun
 * {@code @MappedSuperclass}, sehingga Hibernate tidak memetakan property induk sama sekali —
 * deklarasi ulang inilah yang membuat kolom audit tersimpan. Bukan bug. Efek sampingnya:
 * {@code super.getOleh()} selalu {@code null}.</li>
 * <li><b>{@link #getNama()} bisa melempar {@code NullPointerException}</b> bila kolom {@code nama}
 * kosong <i>dan</i> {@code komponenPenilaianSkripsi} belum diisi — lihat catatan pada method
 * tersebut.</li>
 * <li><b>{@link #getKeterangan()} membalik kontrak base class</b> dengan mengembalikan {@code null}
 * apa adanya, sementara {@link GeneralValueObject#getKeterangan()} berjanji mengembalikan
 * {@code ""}. Ini instance ke sekian dari pola yang sudah dikenal di entity turunan
 * {@code hbm2java}; berdampak pada {@code compareTo} yang mengandalkan {@code keterangan} sebagai
 * pembanding cadangan.</li>
 * <li><b>Keenam getter bendera null-safe ke {@code true}</b>, bukan {@code false}. Karena kolomnya
 * tidak pernah diisi, semuanya selalu {@code true} — konsisten dengan penyaring pembaca di tempat
 * lain yang memang berpola "null atau true dianggap boleh".</li>
 * <li><b>Kedua relasi memakai {@code cascade = PERSIST, MERGE}</b> sehingga menyimpan baris
 * penghubung ini ikut menyimpan master yang ditunjuknya. {@code komponenPenilaianSkripsi} di-fetch
 * {@code SELECT} (query terpisah, tidak {@code JOIN}), sedangkan {@code formatNilaiSkripsi}
 * {@code LAZY} dan getter-nya memanggil {@code check(...)} yang dapat membuka session Hibernate
 * sendiri bila object sudah <i>detached</i>.</li>
 * <li><b>Tidak ada {@code UNIQUE} pada pasangan (format, komponen).</b> Yang mencegah duplikat
 * hanyalah pola hapus-lalu-tulis-ulang di {@code simpan()}; penulisan lewat jalur lain (impor, SQL
 * manual) bisa menghasilkan komponen ganda yang akan terhitung dua kali oleh
 * {@link Skripsi#refreshNilaiKeDefault(Dosen)} — pembaca lain memakai
 * {@code Projections.groupProperty} sehingga kebal terhadap hal ini.</li>
 * </ul>
 *
 * <p>Komentar generator di atas paket menyebut {@code hbm2java} 16 Apr 2010; Javadoc bawaan yang
 * dulu tertulis di sini berbunyi "Bank generated by hbm2java" — salin-tempel dari entity
 * {@code Bank}, tidak ada hubungannya dengan isi file.</p>
 *
 * @see FormatNilaiSkripsi
 * @see KomponenPenilaianSkripsi
 * @see Skripsi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "skripsi_punya_komponen_penilaian_skripsi")

public class SkripsiPunyaKomponenPenilaianSkripsi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Dibangkitkan sekali dan sengaja tidak pernah diubah agar sesi
	 * ZK yang sudah ter-serialize dari versi aplikasi sebelumnya tetap dapat dibaca meski daftar
	 * field entity bertambah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama. Dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; inilah field yang benar-benar dipetakan Hibernate.
	 *
	 * <p><b>Berumur pendek:</b> {@code FormatNilaiSkripsiAction.simpan()} menghapus lalu menulis
	 * ulang seluruh baris milik satu format pada setiap penyimpanan, jadi id yang sama tidak
	 * bertahan antar penyuntingan. Lihat Javadoc class.</p>
	 */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; field audit yang di-shadow dari induk. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; field audit yang di-shadow dari induk. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini apa adanya.
	 *
	 * <p>Dalam praktiknya hampir selalu {@code null}: baris hanya dibuat oleh
	 * {@code FormatNilaiSkripsiAction.simpan()} lewat {@code session.save(...)} tanpa pernah
	 * memanggil {@link #setOlehId(String)}.</p>
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir. Masukan {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> sehingga nilai lama dipertahankan — perilaku sengaja agar jalur
	 * simpan yang tidak membawa identitas pengguna (batch, impor, penjadwal) tidak menghapus jejak
	 * audit yang sudah ada.
	 *
	 * @param olehId id pengguna penyimpan; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir. Sama seperti {@link #setOlehId(String)}, masukan
	 * kosong/{@code null} diabaikan agar nilai audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna penyimpan; diabaikan bila {@code null} atau kosong
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini apa adanya, tanpa normalisasi.
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dipanggil tepat sebelum setiap {@code UPDATE} baris ini, meneruskan
	 * ke {@code AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #getTanggal_dirubah()} dan identitas pengubah secara terpusat.
	 *
	 * <p>Implementasi wajib dari satu-satunya method {@code abstract} pada
	 * {@link GeneralValueObject}. Jangan dipanggil manual — Hibernate yang memanggilnya. Untuk
	 * entity ini kait tersebut praktis tidak pernah aktif: baris tidak pernah di-{@code UPDATE},
	 * hanya dihapus massal lalu ditulis ulang.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Field audit yang di-shadow dari induk; nilai awalnya diisi <b>saat
	 * object dibuat di JVM</b> (bukan saat baris disimpan) lewat {@code WaktuUtil.getDate()},
	 * sehingga baris yang dibaca dari database pun sempat memegang waktu "sekarang" sebelum
	 * Hibernate menimpanya dengan nilai kolom.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Berbeda dengan {@link #setOleh(String)}, method ini
	 * <b>tidak</b> menolak {@code null} — memanggilnya dengan {@code null} benar-benar mengosongkan
	 * kolomnya.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * <p>Karena baris ini selalu dibuat baru (bukan diperbarui), nilainya efektif menjadi <i>waktu
	 * pembuatan</i> — yaitu waktu terakhir kali format nilai induknya disimpan lewat layar
	 * master.</p>
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} karena field punya nilai
	 *         awal saat object dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, berupa isi mentah field {@code nama} saja.
	 *
	 * <p><b>Bukan sekadar tidak informatif, tapi juga rawan:</b> field dibaca <b>langsung</b>, bukan
	 * lewat {@link #getNama()} yang punya cadangan, sehingga baris yang kolom {@code nama}-nya
	 * kosong menghasilkan {@code null} — dan kerangka kerja yang merangkai hasil {@code toString()}
	 * ke dalam pesan (log, {@code Comboitem}, pesan galat) akan mencetak literal {@code "null"}.
	 * Untuk menampilkan baris ini ke pengguna pakailah {@link #getNama()}.</p>
	 *
	 * @return isi kolom {@code nama} apa adanya; dapat {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Salinan nama komponen penilaian pada saat baris dibuat (denormalisasi). Diisi
	 * {@code FormatNilaiSkripsiAction.simpan()} dari {@code komponenPenilaianSkripsi.getNama()};
	 * tidak pernah dibaca kembali oleh kode aplikasi mana pun, dan tidak ikut berubah bila nama
	 * komponen aslinya kemudian disunting.
	 */
	private String nama;
	/** Keterangan bebas. Tidak ada satu pun jalur di aplikasi yang mengisi maupun membacanya. */
	private String keterangan;
	/** Format nilai skripsi (master sidang) yang memiliki baris penghubung ini; wajib, LAZY. */
	private FormatNilaiSkripsi formatNilaiSkripsi;
	/** Butir penilaian yang ditautkan ke format di atas; wajib, di-fetch lewat query terpisah. */
	private KomponenPenilaianSkripsi komponenPenilaianSkripsi;

	// ---------------------------------------------------------------------------------------
	// BENDERA "KOMPONEN INI BOLEH DINILAI OLEH SLOT DOSEN X" — SELURUHNYA MATI.
	// Tidak pernah diisi saat menyimpan dan tidak pernah dibaca di mana pun; isinya selalu NULL di
	// database sehingga getter-nya selalu mengembalikan true. Penyaringan per slot dosen yang
	// benar-benar berjalan memakai KomponenPenilaianSkripsi.dosen1..dosen7. Nama "prosentasi..."
	// menyesatkan (tipenya Boolean, bukan bobot persen) dan mewarisi kosakata slot yang tergeser
	// dari FormatNilaiSkripsi. Baca bagian verifikasi pada Javadoc class sebelum menghidupkannya.
	// ---------------------------------------------------------------------------------------
	/** Bendera untuk slot {@code dosen1} (label default "Pembimbing I"); mati. */
	private Boolean prosentasiNilaiKetuaSidang;
	/** Bendera untuk slot {@code dosen2} (label default "Pembimbing II"); mati. */
	private Boolean prosentasiNilaiPembimbing;
	/** Bendera untuk slot {@code dosen3} (Penguji I); mati. */
	private Boolean prosentasiNilaiPenguji1;
	/** Bendera untuk slot {@code dosen4} (Penguji II); mati. */
	private Boolean prosentasiNilaiPenguji2;
	/** Bendera untuk slot {@code dosen5} (Penguji III); mati. */
	private Boolean prosentasiNilaiPenguji3;
	/** Bendera untuk slot {@code dosen6} (Penguji IV); mati. Tidak ada padanan untuk dosen21/dosen7. */
	private Boolean prosentasiNilaiPenguji4;

	/**
	 * Mengembalikan butir penilaian yang ditautkan baris ini ke formatnya — separuh dari pasangan
	 * yang menjadi seluruh alasan tabel ini ada.
	 *
	 * <p>Inilah object yang dibaca semua pemakai: {@code PenilaianSkripsiHelper.populateKomponen}
	 * dan {@code FormatNilaiSkripsiAction} mengambilnya lewat
	 * {@code Projections.groupProperty("komponenPenilaianSkripsi")} sehingga menerima langsung
	 * daftar {@link KomponenPenilaianSkripsi} tanpa menyentuh property lain di class ini, sementara
	 * {@link Skripsi#refreshNilaiKeDefault(Dosen)} memanggil getter ini untuk mengambil
	 * {@code getId()} dan {@code getBobot()} saat menyusun CSV {@code detailNilai}.</p>
	 *
	 * <p>Kolomnya {@code NOT NULL}. {@code CascadeType.PERSIST}/{@code MERGE} berarti menyimpan
	 * baris penghubung ikut menyimpan komponennya; {@code FetchMode.SELECT} berarti komponen dimuat
	 * lewat query terpisah, bukan {@code JOIN} — pada daftar panjang ini berpotensi N+1 query, tetapi
	 * dua pemakai utamanya sudah menghindarinya lewat {@code createAlias}.</p>
	 *
	 * @return butir penilaian yang ditautkan; tidak pernah {@code null} untuk baris yang tersimpan
	 *         sah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "komponen_penilaian_skripsi", nullable = false)
	public KomponenPenilaianSkripsi getKomponenPenilaianSkripsi() {
		return komponenPenilaianSkripsi;
	}

	/**
	 * Menyetel butir penilaian yang ditautkan. Dipanggil {@code FormatNilaiSkripsiAction.simpan()}
	 * untuk setiap komponen yang tercentang di layar master format.
	 *
	 * <p>Tidak ada validasi: {@code null} diterima apa adanya dan baru gagal saat {@code INSERT}
	 * karena kolomnya {@code NOT NULL}. Juga tidak ada pemeriksaan duplikat — lihat catatan tentang
	 * ketiadaan {@code UNIQUE} pada Javadoc class.</p>
	 *
	 * @param komponenPenilaianSkripsi butir penilaian yang ditautkan ke format
	 */
	public void setKomponenPenilaianSkripsi(KomponenPenilaianSkripsi komponenPenilaianSkripsi) {
		this.komponenPenilaianSkripsi = komponenPenilaianSkripsi;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA sekaligus dipakai
	 * {@code FormatNilaiSkripsiAction.simpan()} untuk membuat baris penghubung baru.
	 *
	 * <p>Tidak menyetel apa pun: satu-satunya nilai awal berasal dari inisialisasi field
	 * {@link #tanggal_dirubah} (waktu sekarang). Object hasil konstruktor ini belum sah untuk
	 * disimpan sampai {@link #setFormatNilaiSkripsi(FormatNilaiSkripsi)} dan
	 * {@link #setKomponenPenilaianSkripsi(KomponenPenilaianSkripsi)} diisi, karena kedua kolomnya
	 * {@code NOT NULL}.</p>
	 */
	public SkripsiPunyaKomponenPenilaianSkripsi() {
	}

	/**
	 * Mengembalikan kunci utama baris ini, dipakai kontrak {@code equals}/{@code compareTo} di
	 * {@link GeneralValueObject} sebagai pembanding kesamaan.
	 *
	 * <p>Kolomnya {@code insertable = false} karena diisi identity/sekuens database, jadi bernilai
	 * {@code null} sampai baris benar-benar tersimpan.</p>
	 *
	 * <p><b>Jangan menyimpan nilai ini sebagai referensi.</b> Id baris di tabel ini berganti setiap
	 * kali format nilai induknya disimpan ulang (hapus massal + tulis ulang). Salah satu bug yang
	 * sudah tercatat —
	 * {@link Skripsi#retreiveDetailVerifikasiNilai(SkripsiPunyaKomponenPenilaianSkripsi, Dosen)} —
	 * justru membandingkan id ini dengan id {@link KomponenPenilaianSkripsi} yang tersimpan di CSV
	 * {@code detailNilai}, dua ruang id yang sama sekali berbeda.</p>
	 *
	 * @return id baris, atau {@code null} untuk object yang belum pernah di-{@code persist}
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipanggil Hibernate; kode aplikasi tidak pernah memakainya.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama baris ini: isi kolom {@code nama} yang sudah di-{@code trim}, atau — bila
	 * kolom itu {@code null} — nama {@link KomponenPenilaianSkripsi} yang ditautkan sebagai
	 * cadangan.
	 *
	 * <p><b>Cadangan itu tidak null-safe.</b> Bila {@code nama} {@code null} <i>dan</i>
	 * {@code komponenPenilaianSkripsi} juga belum diisi (mis. object baru hasil konstruktor, atau
	 * baris rusak hasil impor manual), method ini melempar {@code NullPointerException}. Dicatat apa
	 * adanya, tidak diperbaiki. Risiko nyatanya kecil karena jalur simpan satu-satunya selalu
	 * mengisi keduanya.</p>
	 *
	 * <p>Perlu diketahui bahwa nilai kolom {@code nama} adalah <b>salinan beku</b> nama komponen
	 * pada saat baris dibuat: menyunting nama komponen di layar masternya tidak memperbaruinya.
	 * Karena getter ini mendahulukan salinan tersebut, nama yang dikembalikan bisa berbeda dari
	 * {@code getKomponenPenilaianSkripsi().getNama()} yang terkini. Tidak berdampak pada apa pun
	 * saat ini — tidak ada pemanggil aktif; layar master merender nama komponen langsung dari
	 * {@link KomponenPenilaianSkripsi}.</p>
	 *
	 * @return nama komponen untuk baris ini
	 * @throws NullPointerException bila {@code nama} dan {@code komponenPenilaianSkripsi} sama-sama
	 *                              {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? komponenPenilaianSkripsi.getNama() : this.nama.trim();
	}

	/**
	 * Menyetel nama baris apa adanya, tanpa {@code trim} dan tanpa penolakan nilai kosong. Dipanggil
	 * {@code FormatNilaiSkripsiAction.simpan()} dengan nama komponen yang sedang ditautkan.
	 *
	 * @param nama salinan nama komponen; {@code null} membuat {@link #getNama()} memakai cadangan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini apa adanya.
	 *
	 * <p><b>Membalik kontrak base class</b>: {@link GeneralValueObject#getKeterangan()} berjanji
	 * mengembalikan {@code ""} untuk kolom kosong, sedangkan override ini meneruskan {@code null}.
	 * Karena kolomnya tidak pernah diisi jalur mana pun, praktis method ini <b>selalu</b>
	 * mengembalikan {@code null} — pemanggil yang mengandalkan kontrak induk (mis. rantai
	 * {@code compareTo} yang memakai {@code keterangan} sebagai pembanding cadangan) harus
	 * memeriksanya sendiri. Pola yang sama sudah ditemukan pada beberapa entity turunan
	 * {@code hbm2java} lain.</p>
	 *
	 * @return keterangan bebas; praktis selalu {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas apa adanya. Tidak ada pemanggil di seluruh pohon sumber.
	 *
	 * @param keterangan keterangan bebas; {@code null} diterima
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan format nilai skripsi (master sidang) yang memiliki baris penghubung ini —
	 * separuh lainnya dari pasangan yang menjadi alasan tabel ini ada.
	 *
	 * <p>Dipakai seluruh pembaca sebagai <b>penyaring</b>:
	 * {@code Restrictions.eq("formatNilaiSkripsi", ...)} muncul di
	 * {@code PenilaianSkripsiHelper.populateKomponen}, {@code FormatNilaiSkripsiAction},
	 * {@link Skripsi#refreshNilaiKeDefault(Dosen)} dan
	 * {@link Skripsi#bersihkanNilaiKeDefault()} — semuanya bertanya "komponen apa saja yang terpasang
	 * pada format ini".</p>
	 *
	 * <p><b>Punya efek samping.</b> Getter memanggil {@code check(...)} dan <b>menulis balik</b>
	 * hasilnya ke field: pada object yang sudah <i>detached</i>, {@code check(...)} dapat membuka
	 * session Hibernate sendiri untuk memuat ulang proxy — lihat
	 * {@link GeneralValueObject#check(Object)}. Karena Hibernate membandingkan snapshot field,
	 * penggantian proxy dengan instance nyata juga berpotensi membuat entity dianggap kotor.</p>
	 *
	 * <p>Relasinya {@code LAZY} dan {@code NOT NULL}, dengan {@code CascadeType.PERSIST}/{@code MERGE}
	 * sehingga menyimpan baris penghubung ikut menyimpan formatnya.</p>
	 *
	 * @return format nilai skripsi pemilik baris ini; tidak pernah {@code null} untuk baris yang
	 *         tersimpan sah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_nilai_skripsi", nullable = false)
	public FormatNilaiSkripsi getFormatNilaiSkripsi() {
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		return formatNilaiSkripsi;
	}

	/**
	 * Menyetel format nilai skripsi pemilik baris ini. Dipanggil
	 * {@code FormatNilaiSkripsiAction.simpan()} setelah format induknya sendiri tersimpan (sehingga
	 * sudah punya id).
	 *
	 * @param formatNilaiSkripsi format nilai skripsi pemilik; {@code null} diterima apa adanya dan
	 *                           baru gagal saat {@code INSERT}
	 */
	public void setFormatNilaiSkripsi(FormatNilaiSkripsi formatNilaiSkripsi) {
		this.formatNilaiSkripsi = formatNilaiSkripsi;
	}

	/**
	 * Bendera kebolehan menilai untuk slot dosen pertama ({@code dosen1}, label default "Pembimbing
	 * I") — null-safe ke {@code true}.
	 *
	 * <p><b>Mati total: tidak ada pemanggil.</b> Kolomnya tidak pernah diisi jalur simpan mana pun,
	 * sehingga method ini selalu mengembalikan {@code true}. Namanya mewarisi kosakata yang tergeser
	 * dari {@link FormatNilaiSkripsi} (kolom "ketua sidang" sebenarnya milik slot pertama yang
	 * berlabel "Pembimbing I") <i>dan</i> memakai kata "prosentasi" untuk sesuatu yang bertipe
	 * {@code Boolean}, bukan bobot persen. Fungsi yang seharusnya dijalankannya kini ditangani
	 * {@code KomponenPenilaianSkripsi.getDosen1()} yang penomorannya lurus. Baca bagian verifikasi
	 * pada Javadoc class sebelum menghidupkan kolom ini.</p>
	 *
	 * @return selalu {@code true} dalam praktik ({@code true} bila field {@code null})
	 * @see KomponenPenilaianSkripsi
	 */
	public Boolean getProsentasiNilaiKetuaSidang() {
		return prosentasiNilaiKetuaSidang == null ? true : prosentasiNilaiKetuaSidang;
	}

	/**
	 * Menyetel bendera kebolehan menilai untuk slot {@code dosen1}. Tidak ada pemanggil di seluruh
	 * pohon sumber; lihat {@link #getProsentasiNilaiKetuaSidang()}.
	 *
	 * @param prosentasiNilaiKetuaSidang bendera kebolehan; {@code null} diterima dan dibaca sebagai
	 *                                   {@code true}
	 */
	public void setProsentasiNilaiKetuaSidang(Boolean prosentasiNilaiKetuaSidang) {
		this.prosentasiNilaiKetuaSidang = prosentasiNilaiKetuaSidang;
	}

	/**
	 * Bendera kebolehan menilai untuk slot dosen kedua ({@code dosen2}, label default "Pembimbing
	 * II") — null-safe ke {@code true}. Mati total, sama seperti
	 * {@link #getProsentasiNilaiKetuaSidang()}.
	 *
	 * @return selalu {@code true} dalam praktik
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Boolean getProsentasiNilaiPembimbing() {
		return prosentasiNilaiPembimbing == null ? true : prosentasiNilaiPembimbing;
	}

	/**
	 * Menyetel bendera kebolehan menilai untuk slot {@code dosen2}. Tidak ada pemanggil.
	 *
	 * @param prosentasiNilaiPembimbing bendera kebolehan; {@code null} diterima
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public void setProsentasiNilaiPembimbing(Boolean prosentasiNilaiPembimbing) {
		this.prosentasiNilaiPembimbing = prosentasiNilaiPembimbing;
	}

	/**
	 * Bendera kebolehan menilai untuk slot dosen ketiga ({@code dosen3}, label default "Penguji I")
	 * — null-safe ke {@code true}. Mati total.
	 *
	 * @return selalu {@code true} dalam praktik
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Boolean getProsentasiNilaiPenguji1() {
		return prosentasiNilaiPenguji1 == null ? true : prosentasiNilaiPenguji1;
	}

	/**
	 * Menyetel bendera kebolehan menilai untuk slot {@code dosen3}. Tidak ada pemanggil.
	 *
	 * @param prosentasiNilaiPenguji1 bendera kebolehan; {@code null} diterima
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public void setProsentasiNilaiPenguji1(Boolean prosentasiNilaiPenguji1) {
		this.prosentasiNilaiPenguji1 = prosentasiNilaiPenguji1;
	}

	/**
	 * Bendera kebolehan menilai untuk slot dosen keempat ({@code dosen4}, label default "Penguji
	 * II") — null-safe ke {@code true}. Mati total.
	 *
	 * @return selalu {@code true} dalam praktik
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Boolean getProsentasiNilaiPenguji2() {
		return prosentasiNilaiPenguji2 == null ? true : prosentasiNilaiPenguji2;
	}

	/**
	 * Menyetel bendera kebolehan menilai untuk slot {@code dosen4}. Tidak ada pemanggil.
	 *
	 * @param prosentasiNilaiPenguji2 bendera kebolehan; {@code null} diterima
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public void setProsentasiNilaiPenguji2(Boolean prosentasiNilaiPenguji2) {
		this.prosentasiNilaiPenguji2 = prosentasiNilaiPenguji2;
	}

	/**
	 * Bendera kebolehan menilai untuk slot dosen kelima ({@code dosen5}, label default "Penguji
	 * III") — null-safe ke {@code true}. Mati total.
	 *
	 * @return selalu {@code true} dalam praktik
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Boolean getProsentasiNilaiPenguji3() {
		return prosentasiNilaiPenguji3 == null ? true : prosentasiNilaiPenguji3;
	}

	/**
	 * Menyetel bendera kebolehan menilai untuk slot {@code dosen5}. Tidak ada pemanggil.
	 *
	 * @param prosentasiNilaiPenguji3 bendera kebolehan; {@code null} diterima
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public void setProsentasiNilaiPenguji3(Boolean prosentasiNilaiPenguji3) {
		this.prosentasiNilaiPenguji3 = prosentasiNilaiPenguji3;
	}

	/**
	 * Bendera kebolehan menilai untuk slot dosen keenam ({@code dosen6}, label default "Penguji IV")
	 * — null-safe ke {@code true}. Mati total, dan sekaligus <b>bendera terakhir yang ada</b>: tidak
	 * pernah dibuatkan padanan untuk slot {@code dosen21} (Pembimbing III) maupun {@code dosen7}
	 * (Penguji V) yang dikenal {@link FormatNilaiSkripsi}, sehingga rancangan ini memang tidak
	 * pernah selesai.
	 *
	 * @return selalu {@code true} dalam praktik
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Boolean getProsentasiNilaiPenguji4() {
		return prosentasiNilaiPenguji4 == null ? true : prosentasiNilaiPenguji4;
	}

	/**
	 * Menyetel bendera kebolehan menilai untuk slot {@code dosen6}. Tidak ada pemanggil.
	 *
	 * @param prosentasiNilaiPenguji4 bendera kebolehan; {@code null} diterima
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public void setProsentasiNilaiPenguji4(Boolean prosentasiNilaiPenguji4) {
		this.prosentasiNilaiPenguji4 = prosentasiNilaiPenguji4;
	}

}
