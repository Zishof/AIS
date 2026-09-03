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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;
import ais.database.model.Sertifikat;

/**
 * Entity <b>INDUK kegiatan kesiswaan</b> — satu baris {@code sekolah.kegiatan_kesiswaan} adalah satu
 * <i>kegiatan konkret</i> yang diikuti siswa: lomba, kepanitiaan, ekstrakurikuler, seminar, bakti
 * sosial, dan sejenisnya. Baris ini menyimpan identitas kegiatan (nama, nama Inggris, kode, tempat,
 * URL), rentang tanggalnya, cakupan penyelenggara (yayasan/sekolah), dua guru pembina, nomor dan
 * tanggal SK, sertifikat yang akan dicetak untuk pesertanya, serta <b>status alur pengajuan</b>
 * ({@link #BELUM_DIPROSES} &rarr; {@link #SEDANG_DIPROSES} &rarr; {@link #DISETUJUI}/{@link #DITOLAK}).
 *
 * <p><b>Peserta kegiatan TIDAK disimpan di sini.</b> Daftar siswa yang mengikuti kegiatan ada pada
 * entity anak {@link ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa} (tabel
 * {@code sekolah.kegiatan_kesiswaan_punya_siswa}), satu baris per (kegiatan, siswa). Baris peserta
 * itulah yang menyimpan peran/capaian, skala, tanggal keikutsertaan, dan flag
 * {@code persetujuan} per siswa. Hubungan itu satu arah: FK {@code kegiatan_kesiswaan} berada di
 * sisi anak, kelas ini <b>tidak</b> memiliki koleksi peserta — semua pembacaan daftar peserta
 * dilakukan lewat query {@code Restrictions.eq("kegiatanKesiswaan", ...)} di sisi anak.</p>
 *
 * <h2>Posisi dalam rantai kegiatan kesiswaan</h2>
 * <ol>
 *   <li>{@link JenisKelompokKegiatanKesiswaan} — tingkat 1 katalog (Utama/Penunjang).</li>
 *   <li>{@link KelompokKegiatanKesiswaan} — tingkat 2, di layar disebut <i>"Aspek Kegiatan"</i>.
 *   Punya flag {@code aktif} dan {@code bisaDipilihSiswa} yang membatasi combobox saat siswa
 *   sendiri yang mengajukan kegiatan.</li>
 *   <li>{@link DetailKelompokKegiatanKesiswaan} — tingkat 3, di layar disebut <i>"Rincian Aspek
 *   Kegiatan"</i>. Baris inilah yang memiliki koleksi many-to-many
 *   {@code jabatanKegiatanKesiswaans} dan {@code skalaKegiatanKesiswaans}, sehingga pilihan
 *   Jabatan/Skala pada kegiatan <b>dibatasi oleh rincian aspek yang dipilih</b>, bukan oleh seluruh
 *   isi katalog.</li>
 *   <li><b>Kelas ini</b> — kegiatan konkret; menyimpan <i>nilai bawaan</i> jabatan, skala, dan
 *   tanggal yang akan diwarisi baris peserta bila peserta tidak mengisinya sendiri.</li>
 *   <li>{@link ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa} — baris peserta.</li>
 *   <li>{@link NilaiKegiatanKesiswaan} — rubrik nilai yang dihitung dari keikutsertaan.</li>
 * </ol>
 *
 * <h2>Dua jalur pembuatan baris (verifikasi kode, bukan dugaan)</h2>
 * <ul>
 *   <li><b>Jalur petugas.</b> {@code ais.action.master.sekolah.KegiatanKesiswaanAction#onAdd(Event)}
 *   dari layar {@code /pages/master/sekolah/kegiatan_kesiswaan.zul}. Tombol "Tambah" disembunyikan
 *   untuk akun guru dan akun siswa, sehingga hanya akun tata usaha/admin yang melihatnya.</li>
 *   <li><b>Jalur siswa.</b> Tombol <i>"Ajukan Kegiatan Baru"</i> pada
 *   {@code ais.action.master.helper.SiswaPunyaKegiatanKesiswaanHelper} memanggil method
 *   <b>statis</b> {@code KegiatanKesiswaanAction.onAddExternal(...)} dengan
 *   {@code diajukanOleh} sudah diisi siswa yang bersangkutan. Karena statis, jalur ini
 *   menginstansiasi action lewat {@code new} sehingga {@code doBeforeCompose()} dan
 *   {@code doAfterCompose()} <b>tidak pernah jalan</b> — lihat catatan keamanan di bawah.</li>
 * </ul>
 *
 * <p>Ketika kegiatan disimpan lewat {@code onSave()} dan {@code diajukanOleh} terisi, action
 * otomatis membuatkan satu baris peserta
 * {@link ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa} untuk siswa pengaju itu (bila
 * belum ada), serta menyalin lampiran kegiatan menjadi lampiran peserta.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><i>Konstanta status</i> — {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
 *   {@link #DISETUJUI}, {@link #DITOLAK}.</li>
 *   <li><i>Jejak audit ringan</i> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><i>Identitas</i> — {@link #getId()}/{@link #setId(Long)},
 *   {@link #getKode()}/{@link #setKode(String)}, {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getNamaEn()}/{@link #setNamaEn(String)}, {@link #toString()}.</li>
 *   <li><i>Klasifikasi katalog</i> — {@link #getKelompokKegiatanKesiswaan()},
 *   {@link #getDetailKelompokKegiatanKesiswaan()}, {@link #getJabatanKegiatanKesiswaan()},
 *   {@link #getSkalaKegiatanKesiswaan()}.</li>
 *   <li><i>Waktu &amp; periode akademik</i> — {@link #getMulai()}, {@link #getSampai()},
 *   {@link #getTahunAkademik()}, {@link #getJenisSemester()}, {@link #getTahun()}.</li>
 *   <li><i>Cakupan tenant</i> — {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *   {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 *   <li><i>Orang yang terkait</i> — {@link #getGuruPembina1()}, {@link #getGuruPembina2()},
 *   {@link #getDiajukanOleh()}.</li>
 *   <li><i>Alur pengajuan</i> — {@link #getStatus()}/{@link #setStatus(String)}.</li>
 *   <li><i>Atribut deskriptif &amp; administratif</i> — {@link #getTempat()},
 *   {@link #getKeterangan()}, {@link #getUrl()}, {@link #getNoSk()}, {@link #getTglSk()},
 *   {@link #getSertifikat()}, {@link #getBolehDipilih()}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h2>
 * <ol>
 *   <li><b>Empat getter menulis balik ke field (write-back).</b> {@link #getKode()},
 *   {@link #getTahun()}, {@link #getTahunAkademik()}, dan {@link #getJenisSemester()} mengisi
 *   field-nya sendiri ketika masih kosong. Karena Hibernate memakai <i>property access</i>
 *   (anotasi berada pada getter) dan entity dipetakan {@code dynamicUpdate = true}, nilai hasil
 *   tulis-balik itu ikut ter-{@code flush} ke database begitu baris tersentuh di dalam session yang
 *   masih terbuka — termasuk pada layar yang hanya <i>membaca</i>. Berbeda dari getter destruktif
 *   yang ditemukan di entity lain, keempat getter di sini <b>hanya mengisi nilai yang kosong</b>
 *   dan tidak pernah menimpa nilai yang sudah ada, sehingga tidak menghapus data. Efeknya tetap
 *   nyata: kegiatan lama yang tahun akademiknya sengaja dibiarkan NULL akan mendapat tahun akademik
 *   <b>saat baris dibuka</b>, bukan tahun kegiatan sesungguhnya.</li>
 *   <li><b>{@code sekolah}/{@code yayasan} NULL berarti "Semua", bukan "belum diisi".</b>
 *   {@link #setSekolah(Sekolah)} dan {@link #setYayasan(Yayasan)} <b>membuang</b> objek yang
 *   id-nya {@code null} menjadi {@code null} tanpa memberi tanda apa pun. Renderer layar
 *   menampilkan NULL sebagai teks "Semua", dan filter pencarian memakai
 *   {@code CommonSearchFilterHelper.eqSelectedWithId} yang tidak menambah pembatas ketika pilihan
 *   "Semua" aktif — jadi baris ber-{@code sekolah} NULL terlihat oleh seluruh instalasi.</li>
 *   <li><b>{@code nama} dideklarasikan {@code unique = true} tanpa cakupan tenant.</b> Keunikan
 *   berlaku untuk SATU instalasi, bukan per sekolah/yayasan. Dua sekolah dalam satu instalasi tidak
 *   bisa memakai nama kegiatan yang persis sama (mis. "Class Meeting"). Batasan ini hanya
 *   ditegakkan bila skema dibuat oleh Hibernate; pada instalasi yang skemanya dikelola manual,
 *   perilaku bergantung pada indeks yang benar-benar ada di database.</li>
 *   <li><b>{@link #getNama()} memangkas spasi, {@link #setNama(String)} tidak.</b> Nilai mentah
 *   berspasi tetap tersimpan di kolom; hanya pembacaan lewat getter yang bersih. Query yang
 *   membandingkan langsung ke kolom (native SQL, {@code Restrictions.eq}) bisa gagal cocok padahal
 *   di layar terlihat sama.</li>
 *   <li><b>Konstanta status di kelas ini kembar persis dengan milik {@link PrestasiSiswa}</b>
 *   (empat-empatnya bernilai string yang identik). {@code KegiatanKesiswaanAction} membandingkan
 *   status kegiatan ini terhadap {@code PrestasiSiswa.DISETUJUI}, bukan {@link #DISETUJUI}. Kode
 *   itu bekerja <b>hanya karena nilai stringnya kebetulan sama</b>; mengubah salah satu konstanta
 *   akan memutus perbandingan di layar tanpa error kompilasi.</li>
 *   <li><b>{@link #getBolehDipilih()} default {@code true} (fail-open).</b> Kegiatan yang kolomnya
 *   masih NULL — yaitu semua kegiatan yang dibuat sebelum kolom ini ada — dianggap boleh dipilih
 *   siswa lain.</li>
 *   <li><b>{@code tahun} adalah kolom yang dipetakan, bukan properti hitung.</b> Nilainya diturunkan
 *   dari {@link #getTahunAkademik()} di dalam getter dan ikut tersimpan. Kegagalan parsing ditelan
 *   diam-diam (hanya dicatat {@code ErrorAuditUtil}), sehingga kolom bisa tertinggal pada nilai
 *   lama tanpa tanda apa pun.</li>
 *   <li><b>Entity ber-{@link Audited} (Hibernate Envers).</b> Riwayat perubahan ditampilkan lewat
 *   {@code RevisiHelper.createNewRevisi(KegiatanKesiswaan.class, ...)} pada kolom nama di grid.
 *   Semua jalur tulis yang ada memakai {@code Common.refreshUpdate}/{@code refreshSaveOrUpdate}
 *   per baris, jadi — tidak seperti beberapa modul lain — riwayat audit di sini <b>tidak</b>
 *   dilewati oleh operasi massal.</li>
 *   <li><b>Lampiran tidak disimpan di kelas ini.</b> Berkas kegiatan disimpan sebagai
 *   {@code ais.database.model.file.LampiranLain} dengan {@code jenis} =
 *   {@code KegiatanKesiswaan.class.getName()} dan {@code ref} = {@link #getId()}. Karena kunci itu
 *   hanya sepasang string+angka, mengganti id baris berarti memutus lampirannya.</li>
 * </ol>
 *
 * <h2>HASIL VERIFIKASI otorisasi Action pengelola (batch 64)</h2>
 * <p>Pertanyaan yang diaudit: apakah {@code KegiatanKesiswaanAction} — Action yang mengelola CRUD
 * <i>kegiatan itu sendiri</i>, bukan panel peserta yang sudah diaudit terpisah — bergerbang benar.
 * Jawabannya <b>TIDAK</b>. Temuan berikut diverifikasi langsung dari kode Action-nya:</p>
 * <ul>
 *   <li><b>Nol {@code checkPrevilages} aktif di seluruh Action.</b> Satu-satunya kemunculan
 *   {@code CommonPrivilages.checkPrevilages} berada di dalam <b>komentar</b>. Gerbang yang tersisa
 *   hanyalah pembedaan <i>peran</i> ({@code tbmuser.ambilGuru() == null &amp;&amp;
 *   tbmuser.getSiswa() == null}), yang memisahkan guru/siswa dari petugas — tetapi <b>tidak</b>
 *   memisahkan petugas ber-hak-baca dari petugas ber-hak-CRUD.</li>
 *   <li><b>Tombol "Ubah Data" dan "Hapus Data" pada setiap baris grid tidak bergerbang sama
 *   sekali.</b> Baris {@code button.setVisible(edit)} dan {@code button.setVisible(delete)} ada,
 *   tetapi <b>dikomentari</b>, dan variabel {@code edit}/{@code delete} beserta perhitungannya juga
 *   dikomentari. Akibatnya siapa pun yang bisa membuka layar ini bisa mengubah dan menghapus
 *   kegiatan milik sekolah mana pun di instalasi.</li>
 *   <li><b>Tombol "Setujui Semua" melakukan persetujuan massal lintas cakupan.</b> Tombol memanggil
 *   {@code initCriteria(true)} — kriteria yang <i>sama</i> dengan grid, yang secara bawaan tidak
 *   memfilter sekolah/yayasan ("Semua") — lalu untuk setiap kegiatan yang statusnya bukan
 *   {@link #DITOLAK}: menyetel {@link #setStatus(String)} menjadi {@link #DISETUJUI}, dan
 *   menyetel {@code persetujuan = true} untuk <b>SELURUH</b> baris
 *   {@link ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa} milik kegiatan itu. Ini
 *   mengonfirmasi dari sisi entity induk apa yang terlihat dari sisi entity peserta: satu klik
 *   dapat meresmikan seluruh peserta seluruh kegiatan di seluruh instalasi. Gerbangnya hanya
 *   (a) flag konfigurasi {@code aktifkan_tombol_setujui_semua_kegiatan_siswa} — terdaftar dengan
 *   default <b>AKTIF</b> di {@code KonfigurasiNewAction} — dan (b) syarat
 *   {@code tbmuser.getSiswa() == null &amp;&amp; tbmuser.getSiswa() == null}, yaitu <b>kondisi yang
 *   sama diulang dua kali</b> (kemungkinan besar salah salin dari {@code ambilGuru()}), sehingga
 *   akun guru pun lolos.</li>
 *   <li><b>Combobox Status per baris menulis langsung ke database.</b> Mengubah pilihan memicu
 *   {@code Common.refreshUpdate(kegiatanKesiswaan)} seketika, tanpa tombol Simpan dan tanpa
 *   konfirmasi. Gerbangnya hanya {@code tbmuser.getSiswa() == null}, jadi setiap akun non-siswa —
 *   termasuk guru dan petugas ber-hak-baca — bisa menyetujui atau menolak kegiatan.</li>
 *   <li><b>SQL injection pada pencarian {@code KegiatanKesiswaanAction.initCriteria}.</b> Nilai
 *   kotak "Nama Siswa" ({@code searchnamamhs}) dan "NIS Siswa" ({@code searchnim}) disambung
 *   <b>mentah</b> ke dalam string yang diserahkan ke {@code Restrictions.sqlRestriction(...)},
 *   di dalam sub-query {@code this_.id in (select ... from
 *   sekolah.kegiatan_kesiswaan_punya_siswa ...)}. Bentuknya persis sama dengan pola yang sudah
 *   dikenal pada layar organisasi siswa, dan kali ini berada pada layar master kegiatan itu
 *   sendiri. Sub-query kedua ({@code searchguru}) menyambung id numerik dari picker, sehingga jauh
 *   lebih sulit disalahgunakan.</li>
 *   <li><b>Ekspor "Download Persetujuan Siswa" tidak mewarisi gerbang.</b> Tombolnya dipasang lewat
 *   {@code Common.appendKeToolbar(...)} yang tidak menyalin {@code isVisible()} dari tombol
 *   jangkarnya, sehingga tetap terlihat walau tombol "Tambah" disembunyikan. Isinya adalah data
 *   peserta (id, siswa, kegiatan, jabatan, skala, tanggal, keterangan, persetujuan).</li>
 *   <li><b>{@code onAddExternal} melewati pemeriksaan sesi.</b> Method statis itu membuat
 *   {@code new KegiatanKesiswaanAction()} secara manual, sehingga {@code doBeforeCompose()} —
 *   satu-satunya tempat {@code Common.doCheckSecurity()} dipanggil — tidak pernah dieksekusi.
 *   Pemanggilnya, tombol "Ajukan Kegiatan Baru" di
 *   {@code SiswaPunyaKegiatanKesiswaanHelper}, hanya bergerbang {@code tbmuser != null}.</li>
 *   <li><b>Picker "Ikut Kegiatan" tidak memfilter tenant.</b>
 *   {@code AmbilDataKegiatanForKegiatanKesiswaanHelper#initCriteria} menyaring
 *   {@link #getBolehDipilih()}, flag kelompok, dan {@code status = }{@link #DISETUJUI} — tetapi
 *   <b>tanpa satu pun pembatas sekolah/yayasan</b>. Seorang siswa melihat, dan bisa mendaftarkan
 *   diri ke, kegiatan sekolah mana pun di instalasi.</li>
 *   <li><b>Pewarisan hak lewat menu.</b> {@code MenuSnapshotData} mendaftarkan
 *   {@code /pages/master/sekolah/kegiatan_kesiswaan.zul} pada <b>tiga</b> entri menu berbeda
 *   ("Kegiatan Kesiswaan" ×2 dan "Kesiswaan"), dan {@code DashboardKegiatanKesiswaanAdmin} juga
 *   menyisipkan layar yang sama sebagai tab dashboard. Hak atas salah satu pintu itu cukup untuk
 *   membuka layar beserta seluruh tombol tak-bergerbang di atas.</li>
 * </ul>
 * <p><b>Catatan kuirk (bukan kerentanan).</b> (a) Pada kriteria ekspor, baris
 * {@code Guru dsn = (Guru) (searchguru != null ? null : searchguru.getAttribute("guru"));}
 * memiliki ternary <b>terbalik</b> — {@code dsn} selalu {@code null}, sehingga filter guru pembina
 * pada file ekspor tidak pernah berlaku. (b) Method {@code onUploadData(Event)} ada di Action,
 * tetapi tidak ada komponen di {@code kegiatan_kesiswaan.zul} yang meneruskan {@code onUpload} ke
 * sana, jadi jalur itu tidak terjangkau dari layar; pemetaan kolomnya pun tidak sejajar dengan
 * urutan kolom ekspor. (c) ZUL mendeklarasikan empat {@code tabpanel} tetapi hanya tiga
 * {@code tab}, sehingga {@code formTab} beserta handler {@code onForm(Event)} tak pernah muncul.
 * </p>
 *
 * <p><b>Catatan pewarisan.</b> Kelas ini {@code extends} {@link GeneralValueObject}, sebuah POJO
 * abstrak biasa — <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}. Hibernate tidak
 * memetakan properti kelas induk, sehingga {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini agar tersimpan. Pengulangan itu
 * keharusan teknis, bukan duplikasi yang perlu "dirapikan". Method {@code check(...)} yang dipakai
 * seluruh getter relasi juga berasal dari kelas induk: ia me-resolusi proxy lazy dari cache atau
 * session baru sehingga relasi tetap terbaca setelah session ditutup.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa
 * @see NilaiKegiatanKesiswaan
 * @see DetailKelompokKegiatanKesiswaan
 * @see KelompokKegiatanKesiswaan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kegiatan_kesiswaan")
public class KegiatanKesiswaan extends GeneralValueObject {

	/**
	 * Status awal kegiatan yang baru diajukan dan belum disentuh petugas. Nilai inilah yang
	 * dikembalikan {@link #getStatus()} ketika kolom {@code status} masih NULL, sehingga kegiatan
	 * lama yang dibuat sebelum kolom ini ada pun tampil sebagai "Belum diproses". Nilai stringnya
	 * identik dengan {@code PrestasiSiswa.BELUM_DIPROSES}.
	 */
	public static final String BELUM_DIPROSES = "Belum diproses";
	/**
	 * Status antara: pengajuan sedang ditelaah petugas kesiswaan. Hanya disetel manual lewat
	 * combobox Status pada grid; tidak ada proses otomatis yang memindahkan kegiatan ke status ini.
	 */
	public static final String SEDANG_DIPROSES = "Sedang diproses";
	/**
	 * Status kegiatan yang <b>disetujui</b>. Nilai ini adalah gerbang fungsional terpenting entity:
	 * hanya kegiatan berstatus ini yang muncul di picker "Ikut Kegiatan" untuk siswa, dan hanya
	 * pada status ini tombol kirim-ke-feeder serta panel tambahan pada baris grid ditampilkan.
	 * Tombol "Setujui Semua" menyetel status seluruh kegiatan hasil pencarian menjadi nilai ini
	 * sekaligus menyetel {@code persetujuan = true} pada seluruh baris pesertanya.
	 */
	public static final String DISETUJUI = "Disetujui";
	/**
	 * Status kegiatan yang <b>ditolak</b>. Satu-satunya status yang dikecualikan oleh tombol
	 * "Setujui Semua" (kegiatan ber-status ini tidak ikut disetujui massal).
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer {@code sekolah.kegiatan_kesiswaan.id}; dideklarasikan ulang karena kelas induk tidak dipetakan Hibernate. */
	private Long id;
	/** Id pengguna terakhir yang menyimpan baris ini (jejak audit ringan); lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Identitas tambahan pengubah (mis. NIP/NIM) yang diisi interceptor audit; lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan identitas tambahan pengubah terakhir baris ini.
	 *
	 * @return nilai kolom {@code oleh_id}, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas tambahan pengubah terakhir.
	 *
	 * <p><b>Efek samping penting:</b> nilai {@code null}, kosong, atau hanya berisi spasi
	 * <b>diabaikan diam-diam</b> — field tetap memegang nilai lama. Ini disengaja agar jejak audit
	 * tidak terhapus oleh pemanggil yang meneruskan nilai kosong (mis. proses batch tanpa konteks
	 * pengguna). Konsekuensinya, field ini <b>tidak bisa dikosongkan kembali</b> lewat setter.</p>
	 *
	 * @param olehId identitas pengubah; diabaikan bila kosong/blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel id pengguna yang menyimpan baris ini.
	 *
	 * <p><b>Efek samping penting:</b> sama seperti {@link #setOlehId(String)}, nilai kosong/blank
	 * diabaikan diam-diam sehingga jejak "siapa terakhir menyentuh" tidak bisa dihapus lewat
	 * setter.</p>
	 *
	 * @param oleh id pengguna (biasanya {@code Tbmuser.getUserId()}); diabaikan bila kosong/blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nilai kolom {@code oleh}, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menstempel jejak audit sesaat sebelum baris di-{@code
	 * UPDATE}. Delegasi ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@code oleh}/{@code olehId} dari konteks pengguna
	 * yang aktif.
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Karena hanya terpasang pada
	 * {@code @PreUpdate} (tanpa {@code @PrePersist}), stempel awal saat baris dibuat berasal dari
	 * inisialisasi field {@code tanggal_dirubah} yang dideklarasikan pada baris yang sama dengan
	 * method ini.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya tidak dipanggil langsung oleh kode layar: {@link #onUpdate()} sudah mengisinya
	 * otomatis lewat interceptor audit. Setter ini disediakan untuk kebutuhan migrasi/impor yang
	 * perlu mempertahankan stempel waktu asli.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu ({@code TIMESTAMP}); tidak pernah {@code null} pada objek baru karena
	 *         field-nya diinisialisasi {@code WaktuUtil.getDate()} saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perhatian:</b> method ini membaca field {@code nama} <b>langsung</b>, bukan lewat
	 * {@link #getNama()}, sehingga spasi di ujung nama <b>tidak</b> dipangkas di sini — berbeda dari
	 * teks yang tampil di grid. Dipakai antara lain sebagai label combobox dan pada pesan log/proses
	 * unggah.</p>
	 *
	 * @return gabungan id dan nama kegiatan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas kegiatan (5 digit) yang diturunkan dari id bila kosong; lihat {@link #getKode()}. */
	private String kode;

	/** Nama kegiatan; dipetakan {@code unique = true} tanpa cakupan tenant. Lihat {@link #getNama()}. */
	private String nama;
	/** Nama kegiatan dalam bahasa Inggris, untuk sertifikat/laporan dwibahasa. */
	private String namaEn;
	/** Tempat atau alamat penyelenggaraan kegiatan; wajib diisi oleh validasi layar. */
	private String tempat;
	/** Catatan bebas tentang kegiatan; tampil sebagai kolom terakhir grid. */
	private String keterangan;

	/** Tingkat 2 katalog ("Aspek Kegiatan"); wajib, lihat {@link #getKelompokKegiatanKesiswaan()}. */
	private KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan;
	/** Tingkat 3 katalog ("Rincian Aspek Kegiatan"); wajib, dan pembatas pilihan jabatan/skala. */
	private DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan;
	/** Peran/capaian bawaan bagi peserta kegiatan ini; opsional di database, wajib di layar. */
	private JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan;
	/** Skala/tingkat penyelenggaraan bawaan; opsional di database, wajib di layar. */
	private SkalaKegiatanKesiswaan skalaKegiatanKesiswaan;

	/** Tanggal mulai kegiatan; diwarisi baris peserta yang tidak mengisi tanggalnya sendiri. */
	private Date mulai;
	/** Tanggal selesai kegiatan; diwarisi baris peserta yang tidak mengisi tanggalnya sendiri. */
	private Date sampai;

	/** Status alur pengajuan; lihat konstanta {@link #BELUM_DIPROSES} s.d. {@link #DITOLAK}. */
	private String status;
	/** Guru pembina utama; dipilih lewat bandbox pencarian guru tanpa batas sekolah. */
	private Guru guruPembina1;
	/** Guru pembina kedua (opsional). */
	private Guru guruPembina2;
	/** Siswa pengaju bila kegiatan diajukan dari sisi siswa; {@code null} berarti dibuat petugas. */
	private Siswa diajukanOleh;
	/** Sekolah pemilik kegiatan; {@code null} berarti "Semua" — lihat {@link #setSekolah(Sekolah)}. */
	private Sekolah sekolah;
	/** Yayasan pemilik kegiatan; {@code null} berarti "Semua" — lihat {@link #setYayasan(Yayasan)}. */
	private Yayasan yayasan;
	/** Tautan publikasi/dokumentasi kegiatan; dipetakan sebagai kolom {@code text}. */
	private String url;
	/** Semester penyelenggaraan ("Ganjil"/"Genap"); lihat {@link #getJenisSemester()}. */
	private String jenisSemester;
	/** Tahun ajaran dalam format {@code "2025/2026"}; lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Tahun awal yang diturunkan dari {@code tahunAkademik}; kolom dipetakan, lihat {@link #getTahun()}. */
	private Integer tahun;

	/** Template sertifikat yang dicetak untuk peserta kegiatan ini; {@code null} = tanpa sertifikat. */
	private Sertifikat sertifikat;
	/** Boleh-tidaknya kegiatan ini dipilih siswa lain; default {@code true} bila NULL. */
	private Boolean bolehDipilih;
	/** Tanggal SK penyelenggaraan kegiatan (opsional, administratif). */
	private Date tglSk;
	/** Nomor SK penyelenggaraan kegiatan (opsional, administratif). */
	private String noSk;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Dipakai juga oleh layar saat menekan
	 * "Tambah"/"Ajukan Kegiatan Baru" dan oleh proses unggah Excel saat kegiatan bernama sama belum
	 * ditemukan. Semua field dibiarkan {@code null} kecuali {@code tanggal_dirubah}, yang sudah
	 * berisi waktu saat objek dibuat.
	 */
	public KegiatanKesiswaan() {
	}

	/**
	 * Mengembalikan kunci primer baris.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dihasilkan database
	 * ({@code IDENTITY}/sequence). Id ini juga dipakai sebagai {@code ref} lampiran
	 * {@code LampiranLain} dan sebagai dasar {@link #getKode()}.</p>
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
	 * Menyetel kunci primer. Hanya dipakai Hibernate dan kode migrasi; jangan diubah pada baris yang
	 * sudah tersimpan karena lampiran {@code LampiranLain} terikat pada nilai lama.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kegiatan <b>dengan spasi ujung dipangkas</b>.
	 *
	 * <p>Pemangkasan hanya terjadi pada pembacaan; {@link #setNama(String)} menyimpan nilai apa
	 * adanya, sehingga kolom di database bisa tetap berspasi. Query yang membandingkan langsung ke
	 * kolom (native SQL atau {@code Restrictions.eq}) memakai nilai mentah dan bisa tidak cocok
	 * dengan yang terlihat di layar.</p>
	 *
	 * <p>Kolom {@code nama} dipetakan {@code nullable = false} dan {@code unique = true}. Keunikan
	 * itu berlaku <b>seluruh instalasi</b>, bukan per sekolah — dua sekolah tidak dapat memakai nama
	 * kegiatan yang persis sama.</p>
	 *
	 * @return nama kegiatan yang sudah di-trim, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kegiatan <b>tanpa memangkas spasi</b>.
	 *
	 * <p>Dipanggil dari {@code KegiatanKesiswaanAction#onSave(Event)} (nilai kotak "Nama Kegiatan",
	 * divalidasi tidak boleh kosong) dan dari proses unggah Excel, yang memakai nama sheet sebagai
	 * nama kegiatan.</p>
	 *
	 * @param nama nama kegiatan; wajib diisi menurut pemetaan kolom
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas tentang kegiatan.
	 *
	 * @return isi kolom {@code keterangan}, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang kegiatan.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kelompok ("Aspek Kegiatan") tempat kegiatan ini bernaung — tingkat 2 katalog.
	 *
	 * <p><b>Non-obvious:</b> getter memanggil {@code check(...)} milik {@link GeneralValueObject}
	 * untuk me-resolusi proxy lazy, lalu <b>menulis balik</b> hasilnya ke field. Penulisan itu hanya
	 * mengganti proxy dengan objek yang sudah terinisialisasi (nilai logisnya sama), bukan mengubah
	 * data; tanpa itu, relasi ini akan melempar {@code LazyInitializationException} pada layar yang
	 * merender baris setelah session ditutup.</p>
	 *
	 * <p>Relasi dipetakan {@code nullable = false}: setiap kegiatan wajib punya kelompok. Renderer
	 * grid memanggil {@code getKelompokKegiatanKesiswaan().getNama()} tanpa penjagaan null, jadi
	 * baris dengan FK rusak akan menggagalkan render seluruh halaman.</p>
	 *
	 * @return kelompok kegiatan kesiswaan yang sudah ter-resolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kesiswaan", nullable = false)
	public KelompokKegiatanKesiswaan getKelompokKegiatanKesiswaan() {
		kelompokKegiatanKesiswaan = check(kelompokKegiatanKesiswaan);
		return kelompokKegiatanKesiswaan;
	}

	/**
	 * Menyetel kelompok ("Aspek Kegiatan") kegiatan ini.
	 *
	 * <p>Dipanggil dari {@code onSave(Event)} dengan nilai combobox "Aspek Kegiatan", yang isinya
	 * sudah disaring: hanya kelompok ber-{@code aktif} true/NULL, dan — bila yang login adalah
	 * siswa — hanya yang ber-{@code bisaDipilihSiswa} true/NULL.</p>
	 *
	 * @param kelompokKegiatanKesiswaan kelompok tujuan; tidak boleh {@code null} menurut pemetaan
	 */
	public void setKelompokKegiatanKesiswaan(KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan) {
		this.kelompokKegiatanKesiswaan = kelompokKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan tanggal mulai kegiatan.
	 *
	 * <p>Nilai ini adalah <b>bawaan</b> bagi baris peserta: {@code KegiatanKesiswaanPunyaSiswa}
	 * yang tanggal mulainya kosong akan mengembalikan tanggal ini lewat getter-nya (tanpa
	 * menuliskannya ke kolom peserta).</p>
	 *
	 * @return tanggal mulai ({@code DATE}, tanpa komponen jam), boleh {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Menyetel tanggal mulai kegiatan.
	 *
	 * @param mulai tanggal mulai; boleh {@code null} (layar tidak mewajibkannya)
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal selesai kegiatan.
	 *
	 * <p>Seperti {@link #getMulai()}, nilai ini menjadi bawaan bagi baris peserta yang tidak mengisi
	 * tanggal selesainya sendiri.</p>
	 *
	 * @return tanggal selesai ({@code DATE}), boleh {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Menyetel tanggal selesai kegiatan.
	 *
	 * @param sampai tanggal selesai; boleh {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan kode ringkas kegiatan, <b>membuatnya lebih dulu bila masih kosong</b>.
	 *
	 * <p><b>Getter dengan write-back.</b> Bila baris sudah punya {@link #getId()} tetapi
	 * {@code kode} masih kosong/blank, method membentuk kode dari id: id di-<i>pad</i> dengan nol di
	 * depan lalu diambil <b>5 karakter terakhir</b>, sehingga id {@code 42} menjadi
	 * {@code "00042"}. Nilai itu ditulis ke field, dan karena Hibernate memakai property access
	 * dengan {@code dynamicUpdate}, ia ikut tersimpan begitu baris tersentuh dalam session yang
	 * masih terbuka — termasuk saat layar hanya membaca. Nilai yang sudah ada <b>tidak pernah</b>
	 * ditimpa.</p>
	 *
	 * <p><b>Batas yang perlu diketahui:</b> karena hanya 5 karakter terakhir yang dipakai, instalasi
	 * dengan id kegiatan melewati 99.999 akan menghasilkan kode yang <b>berulang</b> (id
	 * {@code 100042} juga menjadi {@code "00042"}). Kode bukan kolom unik, jadi tabrakan itu tidak
	 * ditolak database. Kolom kode juga dipakai sebagai kunci pencocokan pada proses unggah Excel
	 * (nama sheet dicocokkan {@code ilike} ke {@code kode}), sehingga tabrakan kode berarti unggahan
	 * bisa menempel pada kegiatan yang salah.</p>
	 *
	 * @return kode 5 karakter, atau {@code null} bila baris belum pernah disimpan (id masih null)
	 *         dan kode belum diisi manual
	 */
	public String getKode() {
		if (id != null && (kode == null || kode.trim().isEmpty())) {
			String k = "0000000000" + id;
			kode = k.substring(k.length() - 5);
		}
		return kode;
	}

	/**
	 * Menyetel kode kegiatan secara eksplisit, menonaktifkan pembentukan otomatis di
	 * {@link #getKode()} (yang hanya mengisi bila kode kosong).
	 *
	 * <p>Dipanggil dari proses unggah Excel, yang menyimpan nama sheet sebagai kode kegiatan baru.
	 * Tidak ada validasi keunikan.</p>
	 *
	 * @param kode kode kegiatan; boleh {@code null} untuk mengaktifkan kembali pembentukan otomatis
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan rincian kelompok ("Rincian Aspek Kegiatan") — tingkat 3 katalog.
	 *
	 * <p><b>Penting:</b> baris inilah yang menentukan <i>daftar pilihan</i> Jabatan dan Skala pada
	 * layar. Saat combobox rincian aspek berubah, layar mengambil koleksi
	 * {@code getJabatanKegiatanKesiswaans()} dan {@code getSkalaKegiatanKesiswaans()} milik rincian
	 * terpilih, mengurutkannya, dan hanya itulah yang boleh dipilih. Jadi mengganti rincian aspek
	 * bisa membuat jabatan/skala yang sudah tersimpan tidak lagi tersedia.</p>
	 *
	 * <p>Seperti getter relasi lain, memanggil {@code check(...)} dan menulis balik hasil resolusi
	 * proxy ke field. Relasi dipetakan {@code nullable = false} dan renderer grid memanggil
	 * {@code .getNama()} tanpa penjagaan null.</p>
	 *
	 * @return rincian kelompok kegiatan yang sudah ter-resolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_kelompok_kegiatan_kesiswaan", nullable = false)
	public DetailKelompokKegiatanKesiswaan getDetailKelompokKegiatanKesiswaan() {
		detailKelompokKegiatanKesiswaan = check(detailKelompokKegiatanKesiswaan);
		return detailKelompokKegiatanKesiswaan;
	}

	/**
	 * Menyetel rincian kelompok kegiatan.
	 *
	 * <p>Dipanggil dari {@code onSave(Event)}; combobox sumbernya hanya berisi rincian milik
	 * kelompok yang dipilih di baris atasnya, ber-{@code aktif} true/NULL, dan (untuk akun siswa)
	 * ber-{@code bisaDipilihSiswa} true/NULL.</p>
	 *
	 * @param detailKelompokKegiatanKesiswaan rincian kelompok tujuan; tidak boleh {@code null}
	 */
	public void setDetailKelompokKegiatanKesiswaan(DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan) {
		this.detailKelompokKegiatanKesiswaan = detailKelompokKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan status alur pengajuan kegiatan, dengan {@link #BELUM_DIPROSES} sebagai nilai
	 * pengganti bila kolom masih NULL.
	 *
	 * <p>Berbeda dari {@link #getKode()}, penggantian di sini <b>tidak</b> ditulis balik ke field —
	 * kolom di database tetap NULL. Akibatnya query yang menyaring langsung ke kolom (mis. filter
	 * "Status = Belum diproses" pada layar, yang memakai {@code Restrictions.eq("status", ...)})
	 * <b>tidak menemukan</b> baris ber-status NULL, walaupun di grid baris itu tampil "Belum
	 * diproses". Filter status karena itu tidak pernah bisa memunculkan kegiatan lama yang kolom
	 * statusnya belum pernah diisi.</p>
	 *
	 * <p>Nilai balik dibandingkan di banyak tempat terhadap {@link #DISETUJUI} — dan, pada renderer
	 * grid, terhadap {@code PrestasiSiswa.DISETUJUI} yang nilainya kebetulan identik.</p>
	 *
	 * @return salah satu dari empat konstanta status; tidak pernah {@code null}
	 */
	public String getStatus() {
		return status == null ? BELUM_DIPROSES : status;
	}

	/**
	 * Menyetel status alur pengajuan kegiatan.
	 *
	 * <p><b>Dari mana dipanggil:</b> (a) combobox Status pada setiap baris grid — perubahannya
	 * langsung di-{@code refreshUpdate} ke database tanpa tombol Simpan; (b) tombol
	 * <i>"Setujui Semua"</i>, yang menyetel {@link #DISETUJUI} untuk seluruh kegiatan hasil
	 * pencarian yang belum berstatus {@link #DITOLAK}, lalu ikut menyetel {@code persetujuan = true}
	 * pada seluruh baris {@code KegiatanKesiswaanPunyaSiswa} milik tiap kegiatan tersebut.</p>
	 *
	 * <p><b>Efek berantai bila diisi {@link #DISETUJUI}:</b> kegiatan mulai muncul di picker
	 * "Ikut Kegiatan" milik siswa, dan panel/tombol khusus kegiatan disetujui (kirim ke feeder,
	 * cetak sertifikat peserta) menjadi terlihat.</p>
	 *
	 * <p>Tidak ada validasi bahwa nilai yang masuk merupakan salah satu dari empat konstanta;
	 * string sembarang akan tersimpan apa adanya dan membuat semua perbandingan status gagal.</p>
	 *
	 * @param status salah satu dari {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
	 *               {@link #DISETUJUI}, {@link #DITOLAK}; {@code null} berarti kembali dianggap
	 *               "Belum diproses" oleh {@link #getStatus()}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan siswa yang mengajukan kegiatan ini, atau {@code null} bila kegiatan dibuat
	 * petugas.
	 *
	 * <p>Renderer grid menampilkan {@code null} sebagai teks <b>"Admin"</b>. Kolom ini pula yang
	 * memicu pembuatan otomatis satu baris peserta pada {@code onSave(Event)}: bila terisi, action
	 * mencari/membuat {@code KegiatanKesiswaanPunyaSiswa} untuk pasangan (kegiatan, siswa
	 * pengaju).</p>
	 *
	 * <p><b>Perhatikan:</b> {@code onSave(Event)} menyetel ulang kolom ini dari
	 * {@code tbmuser.getSiswa()} setiap kali kegiatan disimpan. Karena akun petugas tidak punya
	 * relasi siswa, menyimpan ulang kegiatan yang diajukan siswa <b>dari layar petugas</b> akan
	 * mengosongkan kolom ini — jejak "siapa yang mengajukan" hilang, dan baris peserta otomatis
	 * tidak lagi dibuat ulang.</p>
	 *
	 * @return siswa pengaju yang sudah ter-resolusi, atau {@code null} bila diajukan petugas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Siswa getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);
		return diajukanOleh;
	}

	/**
	 * Menyetel siswa pengaju kegiatan.
	 *
	 * <p>Diisi oleh tombol "Ajukan Kegiatan Baru" pada tab Kegiatan di biodata siswa (sebelum
	 * dialog tambah dibuka), dan disetel ulang dari akun yang login pada setiap
	 * {@code onSave(Event)} — lihat peringatan di {@link #getDiajukanOleh()}.</p>
	 *
	 * @param diajukanOleh siswa pengaju; {@code null} berarti "Admin"
	 */
	public void setDiajukanOleh(Siswa diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/**
	 * Mengembalikan sekolah pemilik kegiatan.
	 *
	 * <p><b>{@code null} berarti "Semua", bukan "belum diisi".</b> Renderer grid menampilkannya
	 * sebagai teks "Semua", dan filter pencarian tidak menambahkan pembatas apa pun untuk baris
	 * seperti itu — sehingga kegiatan tanpa sekolah terlihat oleh seluruh sekolah dalam instalasi.
	 * Beberapa jalur pembuatan (mis. unggah Excel yang membuat kegiatan baru dari nama sheet) tidak
	 * pernah mengisi kolom ini, jadi kegiatan hasil unggahan otomatis bersifat global.</p>
	 *
	 * @return sekolah pemilik yang sudah ter-resolusi, atau {@code null} yang berarti "Semua"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel sekolah pemilik kegiatan.
	 *
	 * <p><b>Efek samping penting:</b> objek yang {@code null} <i>atau yang id-nya {@code null}</i>
	 * disimpan sebagai {@code null}. Pola ini menyaring item combobox "Semua" (yang bernilai objek
	 * kosong tanpa id), tetapi juga membuang objek {@code Sekolah} yang belum tersimpan tanpa
	 * memberi tanda apa pun kepada pemanggil. Hasilnya kegiatan menjadi <b>berlaku untuk seluruh
	 * instalasi</b> — perilaku fail-open yang tidak terlihat di layar kecuali lewat teks "Semua" di
	 * grid.</p>
	 *
	 * <p>Pada dialog tambah/ubah, combobox sekolah <i>di-disable</i> bila akun yang login sudah
	 * terikat pada satu sekolah, sehingga jalur UI normal mengisi kolom ini dengan benar; jalur
	 * non-UI (unggah, API, migrasi) tidak punya penjagaan serupa.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id berarti "Semua"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik kegiatan.
	 *
	 * <p>Sama seperti {@link #getSekolah()}, {@code null} berarti "Semua" dan tidak dibatasi filter
	 * pencarian mana pun.</p>
	 *
	 * @return yayasan pemilik yang sudah ter-resolusi, atau {@code null} yang berarti "Semua"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menyetel yayasan pemilik kegiatan, dengan perilaku pembuangan yang sama seperti
	 * {@link #setSekolah(Sekolah)}: objek tanpa id disimpan sebagai {@code null} ("Semua").
	 *
	 * <p>Pada dialog tambah, bila kegiatan belum punya yayasan dan akun yang login terikat pada satu
	 * yayasan, nilai itu diisikan otomatis sebelum combobox dirender.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id berarti "Semua"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan tempat/alamat penyelenggaraan kegiatan.
	 *
	 * @return isi kolom {@code tempat}; boleh {@code null} pada data lama meskipun layar
	 *         mewajibkannya
	 */
	public String getTempat() {
		return tempat;
	}

	/**
	 * Menyetel tempat/alamat penyelenggaraan kegiatan.
	 *
	 * <p>Divalidasi tidak boleh kosong oleh {@code onSave(Event)} (label layar "Tempat / Alamat
	 * Kegiatan *"); validasi itu hanya berlaku di layar, bukan di pemetaan kolom.</p>
	 *
	 * @param tempat tempat atau alamat kegiatan
	 */
	public void setTempat(String tempat) {
		this.tempat = tempat;
	}

	/**
	 * Mengembalikan tautan publikasi/dokumentasi kegiatan.
	 *
	 * <p>Dipetakan sebagai kolom {@code text} sehingga tidak terbatas 255 karakter. Isinya
	 * dimasukkan pengguna sebagai teks bebas dan tidak divalidasi sebagai URL.</p>
	 *
	 * @return tautan kegiatan; boleh {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url;
	}

	/**
	 * Menyetel tautan publikasi/dokumentasi kegiatan.
	 *
	 * @param url tautan; tidak divalidasi formatnya
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Mengembalikan guru pembina utama kegiatan.
	 *
	 * <p><b>Catatan nama kolom:</b> FK-nya bernama {@code guru_pmbina1} — ejaan yang hilang huruf
	 * "e". Ejaan itu harus dipertahankan; memperbaikinya berarti migrasi skema.</p>
	 *
	 * <p>Renderer grid menggabungkan nama pembina 1 dan 2 dalam satu sel. Pembina dipilih lewat
	 * bandbox {@code AmbilDataGuruBanbox}, yang tidak dibatasi sekolah kegiatan — guru dari sekolah
	 * lain dalam instalasi yang sama bisa terpilih.</p>
	 *
	 * @return guru pembina utama yang sudah ter-resolusi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_pmbina1", nullable = true)
	public Guru getGuruPembina1() {
		guruPembina1 = check(guruPembina1);
		return guruPembina1;
	}

	/**
	 * Menyetel guru pembina utama.
	 *
	 * <p>Diisi {@code onSave(Event)} dari atribut {@code "guru"} bandbox pembina I. Bila pengguna
	 * mengetik nama tanpa memilih dari daftar, atribut itu kosong dan kolom menjadi {@code null}
	 * tanpa peringatan.</p>
	 *
	 * @param guruPembina1 guru pembina utama; boleh {@code null}
	 */
	public void setGuruPembina1(Guru guruPembina1) {
		this.guruPembina1 = guruPembina1;
	}

	/**
	 * Mengembalikan guru pembina kedua kegiatan.
	 *
	 * <p>FK-nya bernama {@code guru_pmbina2}, dengan salah ketik yang sama seperti
	 * {@link #getGuruPembina1()}.</p>
	 *
	 * @return guru pembina kedua yang sudah ter-resolusi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_pmbina2", nullable = true)
	public Guru getGuruPembina2() {
		guruPembina2 = check(guruPembina2);
		return guruPembina2;
	}

	/**
	 * Menyetel guru pembina kedua.
	 *
	 * @param guruPembina2 guru pembina kedua; boleh {@code null}
	 */
	public void setGuruPembina2(Guru guruPembina2) {
		this.guruPembina2 = guruPembina2;
	}

	/**
	 * Mengembalikan skala/tingkat penyelenggaraan bawaan kegiatan (mis. "Sekolah", "Kabupaten",
	 * "Nasional").
	 *
	 * <p>Nilai ini diwarisi baris peserta yang tidak mengisi skalanya sendiri. Pilihan yang tersedia
	 * di layar dibatasi koleksi {@code skalaKegiatanKesiswaans} milik
	 * {@link #getDetailKelompokKegiatanKesiswaan()}, bukan seluruh isi katalog
	 * {@link SkalaKegiatanKesiswaan}.</p>
	 *
	 * @return skala kegiatan yang sudah ter-resolusi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skala_kegiatan_kesiswaan", nullable = true)
	public SkalaKegiatanKesiswaan getSkalaKegiatanKesiswaan() {
		skalaKegiatanKesiswaan = check(skalaKegiatanKesiswaan);
		return skalaKegiatanKesiswaan;
	}

	/**
	 * Menyetel skala/tingkat penyelenggaraan bawaan kegiatan.
	 *
	 * <p>Divalidasi wajib oleh {@code onSave(Event)} (label layar "Skala *"), meskipun kolomnya
	 * dipetakan {@code nullable = true} sehingga data lama/hasil unggah bisa kosong.</p>
	 *
	 * @param skalaKegiatanKesiswaan baris katalog skala; boleh {@code null} di tingkat pemetaan
	 */
	public void setSkalaKegiatanKesiswaan(SkalaKegiatanKesiswaan skalaKegiatanKesiswaan) {
		this.skalaKegiatanKesiswaan = skalaKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan jabatan/peran bawaan kegiatan.
	 *
	 * <p>Katalog {@link JabatanKegiatanKesiswaan} mencampur peran kepanitiaan, format lomba, dan
	 * capaian juara dalam satu kolom (mis. "Peserta", "Panitia", "Juara I"). Nilai di sini menjadi
	 * bawaan bagi peserta yang tidak mengisi perannya sendiri, dan pilihan yang tersedia dibatasi
	 * koleksi {@code jabatanKegiatanKesiswaans} milik
	 * {@link #getDetailKelompokKegiatanKesiswaan()}.</p>
	 *
	 * @return jabatan kegiatan yang sudah ter-resolusi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_kegiatan_kesiswaan", nullable = true)
	public JabatanKegiatanKesiswaan getJabatanKegiatanKesiswaan() {
		jabatanKegiatanKesiswaan = check(jabatanKegiatanKesiswaan);
		return jabatanKegiatanKesiswaan;
	}

	/**
	 * Menyetel jabatan/peran bawaan kegiatan.
	 *
	 * <p>Divalidasi wajib oleh {@code onSave(Event)} (label layar "Jabatan/Status *"), meskipun
	 * kolomnya dipetakan {@code nullable = true}.</p>
	 *
	 * @param jabatanKegiatanKesiswaan baris katalog jabatan; boleh {@code null} di tingkat pemetaan
	 */
	public void setJabatanKegiatanKesiswaan(JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan) {
		this.jabatanKegiatanKesiswaan = jabatanKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan tahun awal kegiatan, <b>menurunkannya lebih dulu dari {@code tahunAkademik}</b>
	 * bila tahun akademiknya terisi.
	 *
	 * <p><b>Getter dengan write-back, dan bukan properti hitung.</b> Kolom {@code tahun} benar-benar
	 * dipetakan (tidak ada {@code @Transient}), jadi nilai yang dihitung di sini ikut tersimpan.
	 * Perhitungannya: ambil bagian sebelum tanda "/" dari {@code tahunAkademik} ("2025/2026" &rarr;
	 * {@code 2025}) lalu di-parse menjadi {@code Integer}.</p>
	 *
	 * <p><b>Berbeda dari getter write-back lain di kelas ini, method ini MENIMPA nilai yang sudah
	 * ada</b> — syaratnya hanya {@code tahunAkademik != null}, bukan {@code tahun == null}. Jadi
	 * nilai {@code tahun} yang pernah disetel manual lewat {@link #setTahun(Integer)} akan
	 * dikembalikan ke turunan {@code tahunAkademik} pada pembacaan berikutnya. Bila parsing gagal
	 * (format tahun akademik tidak baku), exception ditelan diam-diam — hanya dicatat ke
	 * {@code ErrorAuditUtil} — dan field tetap memegang nilai sebelumnya, sehingga kegagalan itu
	 * tidak terlihat di layar mana pun.</p>
	 *
	 * @return tahun awal kegiatan, atau {@code null} bila tahun akademik kosong dan tahun belum
	 *         pernah diisi
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KegiatanKesiswaan.java:305");

			}
		}
		return tahun;
	}

	/**
	 * Menyetel tahun awal kegiatan secara eksplisit.
	 *
	 * <p><b>Nilai yang diset di sini tidak bertahan</b> bila {@code tahunAkademik} terisi: panggilan
	 * {@link #getTahun()} berikutnya akan menghitung ulang dan menimpanya. Tidak ada layar yang
	 * memanggil setter ini; ia praktis hanya dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param tahun tahun awal kegiatan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun ajaran kegiatan dalam format {@code "2025/2026"}, <b>mengisinya dengan
	 * tahun ajaran berjalan bila masih kosong</b>.
	 *
	 * <p><b>Getter dengan write-back.</b> Bila field {@code null}, method memanggil
	 * {@code Common.getCurrentTahunAkademik()} dan menuliskan hasilnya ke field. Karena property
	 * access + {@code dynamicUpdate}, nilai itu ikut ter-{@code flush} begitu baris tersentuh dalam
	 * session yang masih terbuka. Konsekuensi nyata: <b>kegiatan lama yang tahun ajarannya NULL
	 * akan tercatat sebagai kegiatan tahun ajaran berjalan sekadar karena barisnya dibuka</b> —
	 * bukan tahun kegiatan yang sebenarnya. Nilai yang sudah ada tidak pernah ditimpa.</p>
	 *
	 * <p>Nilai ini juga menjadi sumber {@link #getTahun()} dan tampil di grid sebagai teks
	 * {@code "<tahunAkademik>/<jenisSemester>"}.</p>
	 *
	 * @return tahun ajaran; tidak pernah {@code null} setelah pemanggilan pertama, kecuali
	 *         {@code Common.getCurrentTahunAkademik()} sendiri mengembalikan {@code null}
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun ajaran kegiatan.
	 *
	 * <p>Diisi {@code onSave(Event)} dari combobox "Tahun Ajaran *" yang divalidasi wajib. Mengubah
	 * nilai ini otomatis mengubah {@link #getTahun()} pada pembacaan berikutnya.</p>
	 *
	 * @param tahunAkademik tahun ajaran dalam format {@code "awal/akhir"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester penyelenggaraan kegiatan, <b>mengisinya dari kalender berjalan bila
	 * masih kosong</b>.
	 *
	 * <p><b>Getter dengan write-back.</b> Bila field {@code null}, method menentukan semester dari
	 * {@code Common.isNowSemensterGanjil()} — menghasilkan {@link Perkuliahan#GANJIL} atau
	 * {@link Perkuliahan#GENAP} — lalu menuliskannya ke field, yang kemudian ikut tersimpan.
	 * Seperti {@link #getTahunAkademik()}, ini berarti kegiatan lama bisa "mewarisi" semester
	 * <b>saat baris dibuka</b>, bukan semester kegiatan sesungguhnya. Nilai yang sudah ada tidak
	 * ditimpa.</p>
	 *
	 * <p>Konstanta semester dipinjam dari modul perguruan tinggi ({@link Perkuliahan}); tidak ada
	 * konstanta khusus sekolah untuk ini.</p>
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return jenisSemester;
	}

	/**
	 * Menyetel semester penyelenggaraan kegiatan.
	 *
	 * <p>Diisi {@code onSave(Event)} dari combobox "Semester *" yang divalidasi wajib. Tidak ada
	 * validasi bahwa nilainya salah satu konstanta {@link Perkuliahan}.</p>
	 *
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan template sertifikat yang dipakai untuk mencetak sertifikat peserta kegiatan
	 * ini.
	 *
	 * <p>Dipakai {@code SertifikatAction.cetakSertifikat(KegiatanKesiswaanPunyaSiswa)}: pencetakan
	 * sertifikat per peserta hanya berjalan bila kegiatan induknya punya sertifikat. Jadi kolom di
	 * entity <i>induk</i> inilah yang menentukan apakah tombol cetak sertifikat pada baris peserta
	 * menghasilkan sesuatu. Renderer grid menampilkan {@code null} sebagai "-".</p>
	 *
	 * @return template sertifikat yang sudah ter-resolusi, atau {@code null} bila kegiatan tidak
	 *         bersertifikat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	/**
	 * Menyetel template sertifikat kegiatan.
	 *
	 * <p>Combobox sumbernya di-disable untuk akun siswa, sehingga siswa yang mengajukan kegiatan
	 * tidak dapat memilih sertifikat sendiri.</p>
	 *
	 * @param sertifikat template sertifikat; {@code null} berarti "== Tanpa Sertifikat =="
	 */
	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	/**
	 * Mengembalikan apakah kegiatan ini boleh dipilih siswa lain, dengan <b>default {@code true}
	 * bila kolom masih NULL</b>.
	 *
	 * <p><b>Fail-open:</b> semua kegiatan yang dibuat sebelum kolom ini ada — dan semua kegiatan
	 * yang dibuat lewat jalur non-UI seperti unggah Excel — dianggap boleh dipilih. Penggantian
	 * default ini <i>tidak</i> ditulis balik ke field, sehingga query yang menyaring langsung ke
	 * kolom harus menuliskan sendiri bentuk {@code isNull(...) OR eq(..., true)} — dan itulah yang
	 * dilakukan picker "Ikut Kegiatan" ({@code AmbilDataKegiatanForKegiatanKesiswaanHelper}).</p>
	 *
	 * <p>Nilai ini adalah salah satu dari tiga syarat agar kegiatan muncul di picker siswa; dua
	 * lainnya adalah flag kelompoknya ({@code aktif}, {@code bisaDipilihSiswa}) dan status
	 * {@link #DISETUJUI}.</p>
	 *
	 * @return {@code true} bila kegiatan boleh dipilih siswa lain; tidak pernah {@code null}
	 */
	public Boolean getBolehDipilih() {
		return bolehDipilih == null ? true : bolehDipilih;
	}

	/**
	 * Menyetel apakah kegiatan boleh dipilih siswa lain.
	 *
	 * <p>Diisi {@code onSave(Event)} dari checkbox "Kegiatan ini bisa dipilih oleh siswa lainnya".
	 * Checkbox itu tidak bergerbang hak apa pun di luar visibilitas dialog tambah/ubah.</p>
	 *
	 * @param bolehDipilih {@code true} bila boleh dipilih; {@code null} diperlakukan sebagai
	 *                     {@code true} oleh {@link #getBolehDipilih()}
	 */
	public void setBolehDipilih(Boolean bolehDipilih) {
		this.bolehDipilih = bolehDipilih;
	}

	/**
	 * Mengembalikan nama kegiatan dalam bahasa Inggris.
	 *
	 * <p>Dipetakan ke kolom {@code namaen} (huruf kecil semua, tanpa pemisah) bertipe {@code text}.
	 * Ditampilkan di grid tepat di bawah nama Indonesia, sehingga baris tanpa nama Inggris
	 * menyisakan sel kosong.</p>
	 *
	 * @return nama kegiatan versi Inggris; boleh {@code null}
	 */
	@Column(name = "namaen", columnDefinition = "text")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menyetel nama kegiatan dalam bahasa Inggris.
	 *
	 * @param namaEn nama versi Inggris; boleh {@code null}
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan nomor SK penyelenggaraan kegiatan.
	 *
	 * <p>Kolom administratif murni: diisi dan dibaca hanya oleh dialog tambah/ubah. Tidak ada
	 * generator nomor SK untuk entity ini (berbeda dari SK penugasan mengajar), tidak ada validasi
	 * format, dan tidak ada pemeriksaan keunikan.</p>
	 *
	 * @return nomor SK; boleh {@code null}
	 */
	public String getNoSk() {
		return noSk;
	}

	/**
	 * Menyetel nomor SK penyelenggaraan kegiatan.
	 *
	 * @param noSk nomor SK sebagai teks bebas
	 */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/**
	 * Mengembalikan tanggal SK penyelenggaraan kegiatan.
	 *
	 * <p>Tidak divalidasi terhadap {@link #getMulai()}/{@link #getSampai()}: tanggal SK boleh berada
	 * setelah kegiatan selesai.</p>
	 *
	 * @return tanggal SK ({@code DATE}); boleh {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/**
	 * Menyetel tanggal SK penyelenggaraan kegiatan.
	 *
	 * @param tglSk tanggal SK; boleh {@code null}
	 */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}
}
