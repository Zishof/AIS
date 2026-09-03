package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

/**
 * Baris tarif satu <b>item biaya</b> di dalam satu <b>pengaturan biaya sekolah</b> — entity
 * penghubung (join entity dengan atribut) antara {@link PengaturanBiaya} dan
 * {@link ItemBiayaSekolah}.
 *
 * <p>Tabel: {@code sekolah.pengaturan_biaya_item_biaya}. Satu baris menjawab pertanyaan
 * <i>"untuk aturan tarif ini, item biaya X dipungut, dan berapa nominalnya?"</i> Baris ini
 * membawa empat angka rupiah: {@link #getDefaultBiaya() default}, {@link #getMinimalBiaya()
 * minimal}, {@link #getMaksimalBiaya() maksimal}, dan {@link #getDiskonBiaya() diskon lunas
 * sekali bayar}.</p>
 *
 * <h2>Status: HIDUP — inti mesin billing sekolah (terverifikasi)</h2>
 *
 * <p>Entity ini <b>bukan</b> peninggalan yatim. Ia berada tepat di tengah rantai penagihan
 * sekolah yang bahkan didokumentasikan sendiri oleh kode diagnostik
 * {@code AnalisisTagihanSekolahHelper} sebagai:</p>
 *
 * <pre>
 * PengaturanBiaya &rarr; PengaturanBiayaItemBiaya &rarr; NominalBiaya &rarr; Tagihan
 *                 &rarr; PembayaranSiswaDetail/PembayaranSiswa &rarr; layar pembayaran
 * </pre>
 *
 * <p>Pembaca/penulis nyata yang terverifikasi di kode:</p>
 * <ul>
 *   <li><b>{@code PengaturanBiayaAction}</b> — satu-satunya layar master penulis. Merender
 *       satu baris form per {@link ItemBiayaSekolah} aktif (checkbox + 4 kotak angka), lalu
 *       {@code onSave()} menyimpan baris yang dicentang dan <i>menghapus</i> baris yang tidak
 *       dicentang (lihat catatan hapus di bawah).</li>
 *   <li><b>{@code TagihanUtil.ambilNominalBiaya(...)}</b> dan
 *       {@code TagihanUtilCalonSiswa.ambilNominalBiaya(...)} — mesin pembangkit tagihan.
 *       Ketika seorang siswa/calon siswa belum punya {@link NominalBiaya} untuk kombinasi
 *       (pengaturan biaya + item biaya), record baru dibuat dengan
 *       {@code setNominal(getDefaultBiaya())} dan {@code setPengaturanBiayaItemBiaya(this)}.
 *       Jadi angka default di baris inilah yang menjadi nominal awal tagihan siswa.</li>
 *   <li><b>{@code NominalBiaya.getNominal()}</b> — getter <i>destruktif</i> yang membaca
 *       {@link #getMinimalBiaya()}/{@link #getMaksimalBiaya()} dari baris ini untuk menimpa
 *       field {@code nominal}-nya sendiri (lihat "Pola arsitektur" di bawah).</li>
 *   <li><b>{@code Tagihan.getNominal()}, {@code Tagihan.ambilNominal()},
 *       {@code Tagihan.getDiskon()}</b> — menentukan apakah tarif bersifat tetap (kunci) dan
 *       menambahkan {@link #getDiskonBiaya()} pada tagihan yang dibayar sekali lunas.</li>
 *   <li><b>{@code DetailTagihanSiswaHelper}, {@code DetailTagihanCalonSiswaHelper},
 *       {@code DetailTagihanItemBiayaHelper}</b> — layar rincian tagihan per siswa/calon
 *       siswa/item; memakai baris ini untuk memutuskan apakah nominal boleh diketik manual
 *       atau ditampilkan sebagai label baca-saja.</li>
 *   <li><b>{@code PembayaranSiswaAction}</b> dan <b>{@code PembayaranOnline}</b> — layar kasir
 *       dan kanal pembayaran daring; {@code PembayaranOnline} memakai
 *       {@link #getDiskonBiaya()} untuk menempelkan keterangan "Diskon dibayar lunas 1x" pada
 *       pilihan tagihan.</li>
 *   <li><b>{@code PengaturanBiaya.reInit()}</b> — membangun cache statis
 *       {@code Map<idPengaturan, List<idItemBiaya>>} dari seluruh baris tabel ini; cache ini
 *       dipakai untuk menjawab "item apa saja yang termasuk aturan tarif ini" tanpa query.
 *       Cache disegarkan otomatis setelah {@code PengaturanBiayaAction.onSave()}.</li>
 *   <li><b>{@code PengaturanBiaya.kirimTemplate(...)}</b> — merakit pesan notifikasi tagihan
 *       ke wali murid; setiap baris di sini menjadi satu butir rincian, dan
 *       <code>[kode item]</code> pada template diganti nominal tagihan terhitung.</li>
 *   <li><b>{@code InitData}</b> memuat kelas ini ke cache in-memory saat boot, dan
 *       <b>{@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}</b> melindunginya dari pembersihan cache
 *       berkala — konsisten dengan perannya sebagai data referensi panas.</li>
 * </ul>
 *
 * <h2>Peran dalam rantai penagihan</h2>
 *
 * <p>{@link PengaturanBiaya} menentukan <i>siapa</i> yang ditagih (kombinasi jenis biaya,
 * tahun ajaran, kelas, angkatan, penjurusan, status awal siswa, dan — sesuai temuan pada
 * modul asrama — keanggotaan asrama). Entity ini menentukan <i>apa</i> yang ditagih dan
 * <i>berapa</i>. {@link NominalBiaya} lalu menjadi materialisasi per siswa, dan
 * {@link Tagihan} menjadi baris tagihan per periode/angsuran.</p>
 *
 * <h2>Semantik empat angka rupiah</h2>
 * <ul>
 *   <li>{@link #getDefaultBiaya() defaultBiaya} — nominal awal yang disalin ke
 *       {@link NominalBiaya} saat record nominal siswa pertama kali dibuat. Setelah
 *       {@code NominalBiaya} ada, mengubah angka ini <b>tidak</b> menyusul mengubah nominal
 *       siswa yang sudah terbentuk.</li>
 *   <li>{@link #getMinimalBiaya() minimalBiaya} — lantai nominal. Dipakai sebagai batas bawah
 *       validasi input di layar rincian tagihan, dan dipakai {@code NominalBiaya.getNominal()}
 *       untuk menaikkan nominal yang berada di bawah lantai.</li>
 *   <li>{@link #getMaksimalBiaya() maksimalBiaya} — dipakai <b>bukan</b> sebagai plafon,
 *       melainkan sebagai penanda "tarif terkunci": bila {@code maksimal > 0.1} dan
 *       {@code maksimal == minimal}, nominal dianggap tetap, kotak isian diganti label
 *       baca-saja, dan {@code NominalBiaya.getNominal()} memaksa nominal = maksimal.</li>
 *   <li>{@link #getDiskonBiaya() diskonBiaya} — potongan khusus yang <b>hanya</b> berlaku
 *       bila {@code NominalBiaya.dibayarSebayak == 1} (lunas sekali bayar); ditambahkan ke
 *       diskon di {@code Tagihan.getDiskon()} dan ditandai di keterangan pilihan tagihan
 *       {@code PembayaranOnline}.</li>
 * </ul>
 *
 * <h2>Pola arsitektur berulang — hasil verifikasi</h2>
 * <ul>
 *   <li><b>Getter write-back {@code check(...)}</b>: ADA, dan merupakan bentuk yang
 *       <i>normal/aman</i>. {@link #getPengaturanBiaya()} dan {@link #getItemBiayaSekolah()}
 *       menugaskan ulang hasil {@code check(...)} ke field-nya — ini resolusi proxy lazy baku
 *       yang dijelaskan pada {@link ais.database.model.GeneralValueObject}, bukan mutasi data
 *       bisnis.</li>
 *   <li><b>Getter destruktif</b>: <b>TIDAK ADA</b> di file ini. Keempat getter {@code Double}
 *       hanya melakukan <i>null-coalescing</i> ({@code null} &rarr; {@code 0.0}) dan
 *       <b>tidak</b> menulis balik ke field, sehingga membaca entity ini tidak pernah
 *       mengotori Hibernate dirty-checking. Perilaku destruktif justru ada di tetangganya,
 *       {@code NominalBiaya.getNominal()}, yang menulis ulang field {@code nominal} memakai
 *       angka dari baris ini.</li>
 *   <li><b>{@code getKeterangan()} membalik kontrak</b>: <b>TIDAK BERLAKU</b> — entity ini
 *       tidak punya kolom {@code keterangan}/{@code nama}/{@code aktif} sama sekali.</li>
 *   <li><b>Fail-open cakupan tenant</b>: ADA, tetapi <i>di hulu</i>. Entity ini tidak punya
 *       kolom {@code sekolah}/{@code yayasan}; tenancy diwarisi dari
 *       {@link PengaturanBiaya}. Pada {@code PengaturanBiayaAction.initCriteria()}, filter
 *       sekolah/yayasan hanya dipasang bila combo pencarian <i>terpilih</i>; bila konteks
 *       sekolah aktif dan tautan sekolah/yayasan pada akun sama-sama kosong, combo tidak
 *       terisi dan kedua filter menjadi {@code sqlRestriction("1=1")} sehingga daftar aturan
 *       tarif (beserta seluruh baris tarif ini) terbuka lintas sekolah/yayasan. Ini varian
 *       yang sama dengan pola fail-open yang sudah tercatat di modul sekolah lain; kelasnya
 *       metadata tarif, bukan PII siswa.</li>
 * </ul>
 *
 * <h2>Hal non-obvious &amp; jebakan</h2>
 * <ul>
 *   <li><b>Penjaga {@code != null} yang tidak pernah gagal.</b> Puluhan pemanggil menulis
 *       {@code pbi.getMinimalBiaya() != null && pbi.getMaksimalBiaya() != null} — padahal
 *       getter di file ini <b>tidak pernah</b> mengembalikan {@code null}. Penjaga sesungguhnya
 *       adalah {@code maksimal > 0.1}. Konsekuensinya: "belum diisi" dan "sengaja diisi 0"
 *       tidak dapat dibedakan oleh pemanggil.</li>
 *   <li><b>Dua definisi "tarif terkunci" yang tidak konsisten.</b> {@code NominalBiaya},
 *       {@code DetailTagihanItemBiayaHelper}, {@code DetailTagihanSiswaHelper} dan
 *       {@code DetailTagihanCalonSiswaHelper} membandingkan
 *       {@code maksimal.intValue() == minimal.intValue()} (pembulatan ke bawah — 100.000,4
 *       dianggap sama dengan 100.000,9), sedangkan {@code Tagihan.getNominal()} dan
 *       {@code Tagihan.getDiskon()} memakai {@code maksimal.equals(minimal)} (kesamaan
 *       {@code Double} persis). Untuk tarif berpecahan, layar rincian bisa menganggap tarif
 *       terkunci sementara mesin tagihan tidak — atau sebaliknya.</li>
 *   <li><b>Hapus baris memakai SQL mentah.</b> Saat sebuah item dilepas centangnya,
 *       {@code PengaturanBiayaAction.onSave()} menjalankan
 *       {@code delete from sekolah.pengaturan_biaya_item_biaya where ...} lewat
 *       {@code createSQLQuery(...)} di dalam {@code try/catch} yang menelan kegagalan. Dua
 *       akibat: (a) penghapusan <b>tidak</b> tercatat oleh Envers meskipun kelas ini
 *       {@link Audited} — jejak audit kehilangan peristiwa hapus; (b) bila baris masih
 *       direferensikan {@link NominalBiaya} (kolom {@code pengaturan_biaya_item_biaya}),
 *       kegagalan constraint hanya masuk log dan pengguna tetap melihat "tersimpan" padahal
 *       item tidak benar-benar dilepas.</li>
 *   <li><b>Fitur "salin dari pengaturan lain" mati total.</b> Di
 *       {@code PengaturanBiayaAction} blok penyalinan hanya dimasuki ketika
 *       {@code pengaturanBiaya.getId() == null}, tetapi query di dalamnya dibungkus ternary
 *       {@code pengaturanBiaya.getId() == null ? null : ...} — sehingga sumber salinan
 *       <b>selalu</b> bernilai {@code null}. Akibatnya {@code copyDari} tidak pernah
 *       menyalin satu pun tarif, dan tidak ada item yang tercentang otomatis pada aturan
 *       tarif hasil salinan.</li>
 *   <li><b>Kotak "diskon" tidak mengikuti saklar "gunakan biaya default".</b> Di layar
 *       master, {@code defaultBiaya}/{@code minimalBiaya}/{@code maksimalBiaya} di-disable
 *       mengikuti checkbox item <i>dan</i> saklar {@code gunakanBiayaDefault}, sedangkan
 *       {@code diskonBiaya} hanya mengikuti checkbox item. Asimetri ini disengaja atau tidak,
 *       efeknya diskon tetap bisa diubah saat mode biaya default dimatikan.</li>
 *   <li><b>Tidak ada kolom {@code aktif}.</b> Tidak ada soft-delete: satu-satunya cara
 *       menonaktifkan tarif adalah menghapus barisnya, atau menonaktifkan
 *       {@link ItemBiayaSekolah} induknya (seluruh query di modul ini menyaring
 *       {@code itemBiayaSekolah.aktif}).</li>
 *   <li><b>Tidak ada jaminan keunikan pasangan.</b> Kode di seluruh modul memakai
 *       {@code setMaxResults(1)} saat mencari baris untuk pasangan (pengaturan biaya, item
 *       biaya), yang mengindikasikan duplikat dianggap mungkin terjadi; bila duplikat ada,
 *       baris mana yang menang bergantung pada urutan hasil query.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} beserta setter-nya dan kait {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} dan {@link #toString()}.</li>
 *   <li><b>Relasi</b> — {@link #getPengaturanBiaya()} (induk aturan tarif) dan
 *       {@link #getItemBiayaSekolah()} (item biaya yang ditarifkan).</li>
 *   <li><b>Angka tarif</b> — {@link #getDefaultBiaya()}, {@link #getMinimalBiaya()},
 *       {@link #getMaksimalBiaya()}, {@link #getDiskonBiaya()} beserta setter-nya.</li>
 * </ol>
 *
 * <p><b>Catatan pewarisan:</b> {@link ais.database.model.GeneralValueObject} adalah POJO
 * abstrak biasa — <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — sehingga
 * Hibernate tidak memetakan properti induknya. Deklarasi ulang {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru</b>,
 * melainkan keharusan teknis agar kolom-kolom tersebut ikut terpetakan.</p>
 *
 * @see PengaturanBiaya
 * @see ItemBiayaSekolah
 * @see NominalBiaya
 * @see Tagihan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pengaturan_biaya_item_biaya", schema = "sekolah")
public class PengaturanBiayaItemBiaya extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok agar instance yang tersimpan di sesi ZK atau
	 * cache tetap dapat dibaca setelah kelas ini disunting.
	 */
	private static final long serialVersionUID = 7096788954859657529L;
	/** Kunci utama tabel; lihat {@link #getId()}. */
	private Long id;
	/**
	 * Nama tampilan pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Id/username pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id/username pengguna terakhir yang mengubah baris tarif ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah melewati interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id/username pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b>
	 * — jejak audit lama sengaja dipertahankan agar tidak terhapus oleh proses batch atau
	 * pemanggil yang tidak membawa konteks pengguna.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong <b>diabaikan diam-diam</b> sehingga nilai lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna terakhir yang mengubah baris tarif ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna sesi aktif dan
	 * memperbarui {@link #setTanggal_dirubah(Date)}.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga properti audit pada instance ini. Dipanggil oleh
	 * penyedia JPA/Hibernate, bukan oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat instance dibuat
	 * (lewat {@code WaktuUtil.getDate()}, yang menghormati zona waktu konfigurasi aplikasi)
	 * sehingga baris baru tidak pernah memiliki kolom waktu kosong.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()};
	 * berbeda dari {@link #setOleh(String)}, setter ini <b>menerima</b> {@code null}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris tarif ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang baru
	 *         dibuat di memori, tetapi dapat {@code null} untuk baris lama di basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Item biaya yang ditarifkan oleh baris ini; lihat {@link #getItemBiayaSekolah()}. */
	private ItemBiayaSekolah itemBiayaSekolah;
	/** Aturan tarif induk yang memiliki baris ini; lihat {@link #getPengaturanBiaya()}. */
	private PengaturanBiaya pengaturanBiaya;
	/** Nominal awal yang disalin ke {@link NominalBiaya}; lihat {@link #getDefaultBiaya()}. */
	private Double defaultBiaya;
	/** Lantai nominal yang boleh diisi/ditagih; lihat {@link #getMinimalBiaya()}. */
	private Double minimalBiaya;
	/** Penanda tarif terkunci bila sama dengan minimal; lihat {@link #getMaksimalBiaya()}. */
	private Double maksimalBiaya;
	/** Potongan khusus lunas sekali bayar; lihat {@link #getDiskonBiaya()}. */
	private Double diskonBiaya;

	/**
	 * Representasi teks ringkas berbentuk {@code id-itemBiayaSekolah-pengaturanBiaya}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getItemBiayaSekolah()} dan
	 * {@link #getPengaturanBiaya()}, sehingga dapat memicu resolusi proxy lazi
	 * ({@code check(...)}) — termasuk kemungkinan query basis data — hanya untuk mencetak log.
	 * Hindari memanggilnya dari perulangan panas.</p>
	 *
	 * @return teks gabungan id, item biaya, dan aturan tarif induk
	 */
	public String toString() {
		return id + "-" + getItemBiayaSekolah() + "-" + getPengaturanBiaya();
	}

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung oleh {@code PengaturanBiayaAction} untuk membuat baris tarif
	 * baru; pemanggil wajib mengisi {@link #setPengaturanBiaya(PengaturanBiaya)} dan
	 * {@link #setItemBiayaSekolah(ItemBiayaSekolah)} karena kedua kolom FK-nya
	 * {@code nullable = false}.</p>
	 */
	public PengaturanBiayaItemBiaya() {
	}

	/**
	 * Mengembalikan kunci utama baris tarif ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false}: nilainya dibangkitkan basis data
	 * ({@link javax.persistence.GenerationType#IDENTITY}), sehingga id yang disetel manual
	 * sebelum {@code save} tidak akan ikut dikirim pada perintah {@code INSERT}. Di layar
	 * master, {@code id == null} juga dipakai sebagai penanda "baris ini belum pernah
	 * disimpan" untuk menentukan status centang awal.</p>
	 *
	 * @return id baris, atau {@code null} bila belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris tarif ini.
	 *
	 * <p>Dipakai Hibernate setelah {@code INSERT}, dan dipakai eksplisit dengan nilai
	 * {@code null} oleh alur penyalinan {@code PengaturanBiayaAction} untuk memaksa baris
	 * hasil salinan diperlakukan sebagai record baru.</p>
	 *
	 * @param id id baru; boleh {@code null}
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan aturan tarif induk ({@link PengaturanBiaya}) pemilik baris ini.
	 *
	 * <p>Relasi dimuat lazi, karena itu getter menugaskan ulang hasil {@code check(...)} ke
	 * field: pola resolusi proxy baku {@link ais.database.model.GeneralValueObject} yang
	 * memungkinkan entity tetap dapat dibaca meski sesi Hibernate asalnya sudah ditutup.
	 * Write-back ini <b>bukan</b> perubahan data bisnis.</p>
	 *
	 * <p>Semua konteks tenant (yayasan, sekolah, tahun ajaran, kelas, angkatan, penjurusan,
	 * status awal siswa, asrama) diwarisi dari object inilah — baris tarif tidak menyimpan
	 * kolom tenant sendiri.</p>
	 *
	 * @return aturan tarif induk; secara skema tidak pernah {@code null} untuk baris yang
	 *         sudah tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengaturan_biaya_id", nullable = false)
	public PengaturanBiaya getPengaturanBiaya() {
		pengaturanBiaya = check(pengaturanBiaya);
		return this.pengaturanBiaya;
	}

	/**
	 * Menyetel aturan tarif induk pemilik baris ini.
	 *
	 * <p>Dipanggil saat membuat baris baru di layar master dan sekali lagi tepat sebelum
	 * {@code saveOrUpdate} pada {@code onSave()} — penyetelan ulang itu penting karena aturan
	 * tarif baru baru memperoleh id setelah induknya disimpan.</p>
	 *
	 * @param pengaturanBiaya aturan tarif induk; wajib terisi sebelum baris disimpan
	 */
	public void setPengaturanBiaya(PengaturanBiaya pengaturanBiaya) {
		this.pengaturanBiaya = pengaturanBiaya;
	}

	/**
	 * Mengembalikan item biaya ({@link ItemBiayaSekolah}) yang ditarifkan baris ini.
	 *
	 * <p>Sama seperti {@link #getPengaturanBiaya()}, relasi lazi ini melewati
	 * {@code check(...)} dengan write-back ke field — resolusi proxy, bukan mutasi bisnis.</p>
	 *
	 * <p>Object inilah yang membawa kode dan nama item (dipakai template notifikasi
	 * {@code PengaturanBiaya.kirimTemplate}), penanda {@code aktif} (dipakai seluruh query
	 * modul untuk menyaring item mati), {@code parameterTambahan} (bila terisi, nominal
	 * dihitung dari skor siswa dan kotak isian nominal diganti label baca-saja), serta
	 * {@code bolehDiangsur}/{@code angsuranSeragam} yang dibaca mesin tagihan.</p>
	 *
	 * @return item biaya yang ditarifkan; secara skema tidak pernah {@code null} untuk baris
	 *         yang sudah tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_sekolah_id", nullable = false)
	public ItemBiayaSekolah getItemBiayaSekolah() {
		itemBiayaSekolah = check(itemBiayaSekolah);
		return itemBiayaSekolah;
	}

	/**
	 * Menyetel item biaya yang ditarifkan baris ini.
	 *
	 * @param itemBiayaSekolah item biaya; wajib terisi sebelum baris disimpan
	 */
	public void setItemBiayaSekolah(ItemBiayaSekolah itemBiayaSekolah) {
		this.itemBiayaSekolah = itemBiayaSekolah;
	}

	/**
	 * Mengembalikan nominal default item biaya ini pada aturan tarif induk.
	 *
	 * <p>Ini adalah angka yang disalin menjadi {@code NominalBiaya.nominal} ketika mesin
	 * tagihan ({@code TagihanUtil.doAmbilNominalBiaya},
	 * {@code TagihanUtilCalonSiswa.doAmbilNominalBiaya}, dan renderer
	 * {@code DetailTagihanItemBiayaHelper}) membuat record nominal untuk seorang siswa/calon
	 * siswa yang belum memilikinya. Setelah record nominal terbentuk, angka ini
	 * <b>tidak</b> lagi menyusul mengubah nominal siswa tersebut.</p>
	 *
	 * <p><b>Non-obvious:</b> getter melakukan null-coalescing — field {@code null}
	 * dikembalikan sebagai {@code 0.0}, sehingga pemanggil tidak dapat membedakan "belum
	 * diisi" dari "diisi nol", dan pemeriksaan {@code != null} pada hasil getter ini tidak
	 * pernah bernilai salah. Nilai field aslinya tidak diubah (getter tidak destruktif).</p>
	 *
	 * @return nominal default, atau {@code 0.0} bila belum diisi
	 */
	public Double getDefaultBiaya() {
		return defaultBiaya == null ? 0.0 : defaultBiaya;
	}

	/**
	 * Menyetel nominal default item biaya ini.
	 *
	 * <p>Dipanggil dari pendengar {@code onChange} kotak "Default" di
	 * {@code PengaturanBiayaAction} (hanya bila checkbox item dalam keadaan tercentang), dan
	 * dari alur penyalinan {@code copyDari}.</p>
	 *
	 * @param defaultBiaya nominal default baru; {@code null} berarti "belum diisi" dan akan
	 *                     dibaca sebagai {@code 0.0}
	 */
	public void setDefaultBiaya(Double defaultBiaya) {
		this.defaultBiaya = defaultBiaya;
	}

	/**
	 * Mengembalikan batas bawah (lantai) nominal untuk item biaya ini.
	 *
	 * <p>Dipakai di dua tempat berbeda:</p>
	 * <ul>
	 *   <li>Sebagai <b>validasi input</b> di {@code DetailTagihanSiswaHelper} dan
	 *       {@code DetailTagihanCalonSiswaHelper}: nominal yang diketik di bawah angka ini
	 *       ditolak dengan pesan bernominal.</li>
	 *   <li>Sebagai <b>koreksi otomatis</b> di {@code NominalBiaya.getNominal()} dan
	 *       {@code Tagihan.ambilNominal()}: nominal yang berada di bawah lantai dinaikkan
	 *       menjadi lantai — pada {@code NominalBiaya} koreksi itu ditulis balik ke field
	 *       sehingga ikut ter-flush ke basis data.</li>
	 * </ul>
	 *
	 * <p>Bersama {@link #getMaksimalBiaya()} juga menentukan status "tarif terkunci"
	 * (maksimal &gt; 0,1 dan maksimal sama dengan minimal).</p>
	 *
	 * <p><b>Non-obvious:</b> null-coalescing ke {@code 0.0} seperti
	 * {@link #getDefaultBiaya()}. Nilai {@code 0.0} inilah yang membuat perbandingan
	 * {@code getMinimalBiaya() > nominal} aman dari {@code NullPointerException} di pemanggil
	 * yang tidak memeriksa null lebih dulu.</p>
	 *
	 * @return lantai nominal, atau {@code 0.0} bila belum diisi
	 */
	public Double getMinimalBiaya() {
		return minimalBiaya == null ? 0.0 : minimalBiaya;
	}

	/**
	 * Menyetel batas bawah (lantai) nominal untuk item biaya ini.
	 *
	 * <p>Dipanggil dari pendengar {@code onChange} kotak "Minimal" di
	 * {@code PengaturanBiayaAction} (hanya bila checkbox item tercentang) dan dari alur
	 * penyalinan {@code copyDari}.</p>
	 *
	 * <p><b>Perhatian:</b> mengubah angka ini berdampak surut pada data yang sudah ada —
	 * {@code NominalBiaya.getNominal()} akan menaikkan nominal siswa lama yang berada di
	 * bawah lantai baru pada pembacaan berikutnya.</p>
	 *
	 * @param minimalBiaya lantai nominal baru; {@code null} dibaca sebagai {@code 0.0}
	 */
	public void setMinimalBiaya(Double minimalBiaya) {
		this.minimalBiaya = minimalBiaya;
	}

	/**
	 * Mengembalikan potongan khusus untuk pembayaran lunas sekali bayar.
	 *
	 * <p>Diskon ini <b>hanya</b> berlaku bila {@code NominalBiaya.dibayarSebayak == 1}:</p>
	 * <ul>
	 *   <li>{@code Tagihan.getDiskon()} menambahkannya ke diskon terhitung, tetapi hanya pada
	 *       cabang yang tidak memakai diskon manual dan hanya untuk tagihan sekali bayar.</li>
	 *   <li>{@code PembayaranOnline} memakainya untuk menempelkan keterangan
	 *       "Diskon dibayar lunas 1x" pada label pilihan tagihan; untuk tagihan yang dibayar
	 *       lebih dari sekali, nilainya dipaksa {@code 0.0}.</li>
	 * </ul>
	 *
	 * <p><b>Non-obvious:</b> null-coalescing ke {@code 0.0}; ambang efektif yang dipakai
	 * seluruh pemanggil adalah {@code > 0.1}, bukan {@code > 0}.</p>
	 *
	 * @return nominal potongan lunas sekali bayar, atau {@code 0.0} bila belum diisi
	 */
	public Double getDiskonBiaya() {
		return diskonBiaya == null ? 0.0 : diskonBiaya;
	}

	/**
	 * Menyetel potongan khusus untuk pembayaran lunas sekali bayar.
	 *
	 * <p>Dipanggil dari pendengar {@code onChange} kotak "Diskon" di
	 * {@code PengaturanBiayaAction} (hanya bila checkbox item tercentang) dan dari alur
	 * penyalinan {@code copyDari}. Berbeda dari tiga kotak angka lainnya, kotak diskon di
	 * layar master <b>tidak</b> ikut dinonaktifkan oleh saklar "gunakan biaya default".</p>
	 *
	 * @param diskonBiaya nominal potongan baru; {@code null} dibaca sebagai {@code 0.0}
	 */
	public void setDiskonBiaya(Double diskonBiaya) {
		this.diskonBiaya = diskonBiaya;
	}

	/**
	 * Mengembalikan angka "maksimal" item biaya ini.
	 *
	 * <p><b>Non-obvious — namanya menyesatkan.</b> Angka ini hampir tidak pernah dipakai
	 * sebagai plafon. Perannya yang sebenarnya adalah <b>penanda tarif terkunci</b>: bila
	 * {@code maksimal > 0.1} dan maksimal sama dengan {@link #getMinimalBiaya()}, maka</p>
	 * <ul>
	 *   <li>{@code NominalBiaya.getNominal()} memaksa {@code nominal = maksimal} (dan
	 *       menuliskannya balik ke field, sehingga nilai tersimpan ikut berubah);</li>
	 *   <li>{@code DetailTagihanItemBiayaHelper}, {@code DetailTagihanSiswaHelper}, dan
	 *       {@code DetailTagihanCalonSiswaHelper} mengganti kotak isian nominal dengan label
	 *       baca-saja sehingga petugas tidak dapat menawarnya;</li>
	 *   <li>{@code Tagihan.getNominal()} dan {@code Tagihan.getDiskon()} mengambil cabang
	 *       khusus yang memakai nominal apa adanya dari {@code NominalBiaya}.</li>
	 * </ul>
	 *
	 * <p><b>Jebakan konsistensi:</b> pemeriksaan "sama dengan minimal" tidak seragam —
	 * {@code NominalBiaya} dan ketiga helper rincian membandingkan {@code intValue()}
	 * (mengabaikan pecahan), sedangkan {@code Tagihan} memakai {@code equals()} pada
	 * {@code Double}. Untuk tarif berpecahan kedua sisi bisa mengambil keputusan berbeda.</p>
	 *
	 * <p>Null-coalescing ke {@code 0.0} seperti getter angka lainnya; nilai {@code 0.0}
	 * secara efektif berarti "tarif tidak dikunci".</p>
	 *
	 * @return angka maksimal/penanda kunci, atau {@code 0.0} bila belum diisi
	 */
	public Double getMaksimalBiaya() {
		return maksimalBiaya == null ? 0.0 : maksimalBiaya;
	}

	/**
	 * Menyetel angka "maksimal" (penanda tarif terkunci) item biaya ini.
	 *
	 * <p>Dipanggil dari pendengar {@code onChange} kotak "Maksimal" di
	 * {@code PengaturanBiayaAction} (hanya bila checkbox item tercentang) dan dari alur
	 * penyalinan {@code copyDari}.</p>
	 *
	 * <p><b>Perhatian:</b> menyamakan angka ini dengan {@link #getMinimalBiaya()} mengunci
	 * tarif secara surut untuk seluruh siswa pada aturan tarif induk — {@code NominalBiaya}
	 * yang sudah ada akan ditimpa pada pembacaan berikutnya, bukan hanya untuk tagihan
	 * baru.</p>
	 *
	 * @param maksimalBiaya angka maksimal baru; {@code null} dibaca sebagai {@code 0.0}
	 */
	public void setMaksimalBiaya(Double maksimalBiaya) {
		this.maksimalBiaya = maksimalBiaya;
	}

}
