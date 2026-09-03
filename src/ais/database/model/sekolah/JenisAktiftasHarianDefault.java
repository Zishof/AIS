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
 * Master <b>Jenis Aktivitas Harian Default</b> — daftar baku butir aktivitas yang otomatis
 * ditawarkan saat pembina/guru membuka formulir <i>Aktivitas Harian Siswa</i> (buku penghubung),
 * dipetakan ke tabel {@code sekolah.jenis_aktiftas_harian_default}.
 *
 * <h3>Peran yang TERVERIFIKASI (bukan tebakan dari nama kelas)</h3>
 * Baris entity ini adalah satu <b>label butir aktivitas</b> (contoh bawaan: "Shalat Jamaah",
 * "Membaca Al-Quran", "Membantu Orang Tua", "Olahraga"). Perannya dipastikan dari lima sumber
 * independen di dalam repo:
 * <ol>
 * <li><b>Layar master</b> — {@code /pages/master/sekolah/jenis_aktiftas_harian_default.zul}
 * dikendalikan {@code ais.action.master.sekolah.JenisAktiftasHarianDefaultAction}; judul dialog
 * yang disetel {@code init(JenisAktiftasHarianDefault)} berbunyi <i>"Tambah Jenis Aktiftas Harian
 * Default"</i> / <i>"Ubah Jenis Aktiftas Harian Default"</i> dengan isian <i>"Nama *"</i>,
 * <i>"Yayasan *"</i>, <i>"Sekolah *"</i>, dan <i>"Keterangan"</i>.</li>
 * <li><b>Tempat layar itu digantung</b> — bukan menu tersendiri, melainkan tab <i>"Aktivitas
 * Default"</i> di dalam {@code aktiftas_harian_siswa.zul}; tab itu di-<i>include</i> secara
 * dinamis oleh {@code AktiftasHarianSiswaAction#onJenisAktiftasHarianDefault(Event)}.</li>
 * <li><b>Satu-satunya pembaca sungguhan</b> —
 * {@code AktiftasHarianSiswaAction#init(AktiftasHarianSiswa)}: ketika sebuah catatan harian
 * <b>baru</b> dibuat ({@code getId() == null}), seluruh baris aktif kelas ini dibaca terurut
 * {@code nomorUrut, nama} lalu dijadikan baris-baris isian kosong lewat {@code addRowAkt(nama,
 * "")}. Untuk catatan yang <b>sudah tersimpan</b>, kelas ini tidak dibaca sama sekali — isian
 * dipulihkan dari kolom JSON {@code AktiftasHarianSiswa.aktifitas}.</li>
 * <li><b>Penyemaian bawaan</b> — {@code AktiftasHarianSiswaAction#initDefaultMasterData()}
 * (lihat bagian "Auto-seed" di bawah).</li>
 * <li><b>Pasangan kembarnya</b> — {@code JenisMateriHarianDefault} (tabel
 * {@code sekolah.jenis_materi_harian_default}, tab <i>"Materi Default"</i>, bawaan "Tahfidz",
 * "Hadits", "Bahasa Arab", "Fiqih"). Kedua kelas itu salinan persis satu sama lain sampai ke
 * nilai {@code serialVersionUID}-nya, dibaca berdampingan di method yang sama, dan mengisi dua
 * blok isian berbeda pada formulir yang sama ({@code rowsAkt} vs {@code rowsMat}). Aktivitas =
 * "apa yang <i>dikerjakan</i> anak", materi = "apa yang <i>dipelajari</i> anak".</li>
 * </ol>
 *
 * <h3>Hubungan ke data transaksi bersifat SALINAN NILAI, bukan foreign key</h3>
 * Tidak ada satu pun kolom FK yang menunjuk ke tabel ini. Yang tersimpan pada
 * {@code AktiftasHarianSiswa} hanyalah <b>string nama</b> hasil salinan, di dalam kolom teks
 * {@code aktifitas} berformat JSON ({@code {"0":{"nama":"Shalat Jamaah","nilai":"..."}}}). Tiga
 * konsekuensi penting:
 * <ul>
 * <li><b>Mengubah nama di sini tidak menulis ulang riwayat.</b> Catatan harian lama tetap
 * menyimpan nama versi lama; laporan historis aman dari perubahan master.</li>
 * <li><b>Menghapus baris di sini juga aman</b> — tidak ada pelanggaran integritas referensial,
 * dan tidak ada catatan lama yang rusak. Efeknya hanya "butir itu berhenti ditawarkan pada
 * catatan baru".</li>
 * <li><b>Sebaliknya, tidak ada cara mengelompokkan/merekap catatan lama menurut butir master.</b>
 * Rekap apa pun terpaksa mencocokkan string, sehingga sekadar memperbaiki salah ketik pada
 * {@code nama} akan memecah satu butir menjadi dua kelompok berbeda di mata laporan.</li>
 * </ul>
 *
 * <h3>Auto-seed: 4 baris bawaan, GLOBAL, dibuat oleh pembuka layar pertama</h3>
 * {@code AktiftasHarianSiswaAction#initDefaultMasterData()} dipanggil dari {@code doAfterCompose},
 * jadi berjalan <b>setiap kali</b> layar Aktivitas Harian Siswa dibuka (dua query
 * {@code rowCount} per pembukaan). Bila jumlah baris tabel ini <b>tepat nol</b>, ia membuka
 * session dan transaksi Hibernate tersendiri lalu menyimpan empat baris:
 * {@code "Shalat Jamaah"}, {@code "Membaca Al-Quran"}, {@code "Membantu Orang Tua"},
 * {@code "Olahraga"} — hanya {@code nama} dan {@code aktif} yang diisi. Hal yang perlu disadari:
 * <ul>
 * <li>Baris hasil semai <b>tidak punya {@code sekolah} maupun {@code yayasan}</b> (keduanya
 * {@code null}), sehingga bersifat <b>global</b>: pembacanya memakai
 * {@code Restrictions.or(isNull("sekolah"), eq("sekolah", ...))} sehingga baris tanpa sekolah
 * muncul pada formulir <b>seluruh</b> sekolah dan yayasan di instalasi.</li>
 * <li>Isinya <b>terpaku pada konteks sekolah Islam</b> dan tidak dapat dikonfigurasi; instalasi
 * dengan konteks lain harus menghapus/mengganti keempatnya secara manual.</li>
 * <li>Ambangnya "nol baris untuk <b>seluruh</b> instalasi", bukan per sekolah. Sekolah yang
 * menghapus semua butirnya sendiri tidak akan pernah mendapat semai ulang selama sekolah lain
 * masih punya baris.</li>
 * <li>Penyimpanan memakai {@code session.save(...)} langsung, bukan
 * {@code Common.refreshSaveOrUpdate(...)}, sehingga cache entity aplikasi tidak disegarkan pada
 * momen penyemaian.</li>
 * </ul>
 *
 * <h3>Hak akses: DIWARISI dari menu induk, dan tidak lengkap</h3>
 * <ul>
 * <li><b>Tidak ada menu sendiri.</b> {@code MenuInitializer}/{@code MenuSnapshotData} hanya
 * mendaftarkan menu <i>"Aktifitas Harian Siswa"</i> ({@code aktiftas_harian_siswa.zul}); tidak
 * ada entri menu apa pun untuk {@code jenis_aktiftas_harian_default.zul}. Karena
 * {@code CommonPrivilages.checkPrevilages(int)} menyelesaikan haknya lewat
 * {@code Common.getCurrentMenu()} — yaitu menu halaman <i>terluar</i> — seluruh gerbang
 * CREATE/UPDATE/DELETE layar master ini sesungguhnya adalah hak menu <b>Aktifitas Harian
 * Siswa</b>. Ini instance lain dari pola <i>pewarisan hak lewat menu induk</i> yang sudah
 * tercatat pada audit repo (bandingkan {@code PaketPsb}, {@code KategoriItemPenilaianSiswa},
 * {@code SubMatapelajaran}). Akibat praktisnya: siapa pun yang perlu <i>mengisi</i> jurnal harian
 * (pembina/guru) otomatis juga boleh <i>mengubah katalog master global</i>-nya, termasuk baris
 * milik sekolah lain.</li>
 * <li><b>Isian nomor urut TIDAK bergerbang sama sekali.</b> Di
 * {@code JenisAktiftasHarianDefaultRenderer#render(Row, Object)}, checkbox "Aktif" benar
 * dinonaktifkan bila tak punya hak ({@code checkbox.setDisabled(!edit)}), tetapi {@code Intbox}
 * nomor urut di sebelahnya <b>tidak pernah</b> di-{@code setDisabled}/{@code setReadonly}.
 * Event {@code onChange}-nya langsung memanggil {@link #setNomorUrut(Integer)} lalu
 * {@code Common.refreshSaveOrUpdate(...)}. Pengguna berhak <b>BACA saja</b> dapat mengubah urutan
 * butir pada baris mana pun — termasuk baris global — dan perubahan itu langsung tersimpan serta
 * mencatat revisi Envers. Kelemahan yang sama ada di layar kembarnya
 * ({@code JenisMateriHarianDefaultAction}).</li>
 * <li><b>Penyaringan tenant gagal-terbuka.</b> {@code initCriteria(boolean)} hanya menambah
 * syarat sekolah/yayasan bila combo pencarian kebetulan terpilih; bila kosong dipakai
 * {@code Restrictions.sqlRestriction("1=1")}. Combo diisi
 * {@code Common.initYayasanDanSekolahDanSemua(...)} yang menelan exception dan tidak memilih apa
 * pun bila konteks sekolah tidak ketemu — pada kondisi itu grid menampilkan butir SELURUH sekolah
 * dan yayasan lengkap dengan tombol Ubah/Hapus. Keparahan <b>rendah–menengah</b>: isinya metadata
 * katalog (bukan data pribadi anak), tetapi permukaan <b>tulis</b> lintas tenant nyata dan
 * berdampak langsung pada formulir harian sekolah lain.</li>
 * <li><b>Impor massal ikut terbawa.</b> {@code Common.uploadData(this,
 * JenisAktiftasHarianDefault.class, contents)} dipasang dengan syarat CREATE, UPDATE, dan DELETE
 * sekaligus — tetapi ketiganya tetap hak menu induk, sehingga seorang pembina beroleh unggah
 * massal ke katalog global.</li>
 * <li>Sisi baiknya, {@code doBeforeCompose(...)} memanggil {@code Common.doCheckSecurity()}
 * sehingga <b>tidak ada</b> jalur anonim ke layar ini. Adaptor
 * {@code new/sekolah/services/jenis_aktiftas_harian_default_service.jsp} hanya kerangka hasil
 * generator ({@code generate_new_jsp_scaffold.py}) yang meneruskan ke {@code dispatcher.jsp}
 * tanpa akses data apa pun.</li>
 * </ul>
 *
 * <h3>Jebakan: menyimpan ulang baris bawaan MENGHILANGKANNYA dari sekolah lain</h3>
 * Baris hasil auto-seed bersifat global ({@code sekolah = null}), tetapi
 * {@code JenisAktiftasHarianDefaultAction#onSave(Event)} <b>mewajibkan</b> "Yayasan *" dan
 * "Sekolah *" terisi sebelum menyimpan. Jadi begitu seseorang membuka baris global — bahkan hanya
 * untuk memperbaiki salah ketik nama atau mengisi keterangan — lalu menekan Simpan, baris itu
 * <b>berubah menjadi milik satu sekolah</b> dan seketika lenyap dari formulir harian seluruh
 * sekolah lain. Tidak ada peringatan, tidak ada jalan kembali lewat layar (formulir tidak
 * menyediakan pilihan "tanpa sekolah"/global). Satu-satunya cara membuat baris global baru adalah
 * lewat auto-seed pada tabel kosong. Perlakukan ini sebagai jebakan operasional utama berkas ini.
 *
 * <h3>Hal non-obvious lain yang wajib diketahui sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Komentar generator lama di berkas ini SALAH dan sudah diganti.</b> Sebelum revisi ini,
 * Javadoc kelas berbunyi <i>"JenisGuru generated by hbm2java"</i>. Kelas ini sama sekali bukan
 * {@code JenisGuru}: string itu terbawa salin-tempel dari {@code sekolah.JenisGuru} — berkas
 * sumber template yang komentarnya memang benar — ke 15 berkas model lain, termasuk berkas ini
 * dan kembarannya {@code JenisMateriHarianDefault}. Jejak salin-tempel yang sama masih terlihat
 * pada {@code serialVersionUID} ketiganya yang identik ({@code -7490758846785025664L}), dan pada
 * atribut {@code title="Tambah Jenis Guru"} yang terbawa ke puluhan berkas {@code .zul} tak
 * berhubungan. Komentar diganti, bukan sekadar dihapus, agar jejak asal-usulnya tidak
 * hilang.</li>
 * <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * TIDAK memetakan properti apa pun miliknya. Maka {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan juga {@code nama}/{@code keterangan}/{@code nomorUrut}
 * <b>harus</b> dideklarasikan ulang di sini agar terpetakan. Ini KEHARUSAN TEKNIS, bukan
 * duplikasi yang boleh "dibersihkan".</li>
 * <li><b>Akibat sampingannya: field induk selamanya kosong.</b> Field lokal membayangi
 * ({@code shadow}) field bernama sama di induk, sehingga kode yang membaca field induk secara
 * langsung (bukan lewat getter) akan mendapat {@code null}. Saat ini aman karena seluruh method
 * induk — termasuk {@code toString()} dan {@code compareTo(GeneralValueObject)} — mengaksesnya
 * lewat getter yang di-override kelas ini.</li>
 * <li><b>Kontrak {@code getKeterangan()} DIBALIK</b> terhadap janji induknya. Lihat
 * {@link #getKeterangan()}.</li>
 * <li><b>{@code getYayasan()} menurunkan ulang nilainya setiap kali dibaca</b> — nilai yang
 * disetel {@link #setYayasan(Yayasan)} akan ditimpa selama {@code sekolah} terisi. Lihat
 * {@link #getYayasan()}.</li>
 * <li><b>{@code getAktif()} dan {@code getNomorUrut()} menormalkan {@code null}</b> menjadi
 * {@code true} dan {@code 1}. Karena pemetaan kelas ini memakai akses properti (anotasi
 * {@code @Id} berada pada getter), nilai hasil normalisasi itulah yang benar-benar ditulis ke
 * kolom saat flush. Lihat {@link #getAktif()} dan {@link #getNomorUrut()}.</li>
 * <li><b>{@code getNomorUrut()} yang tak pernah {@code null} melumpuhkan pengurutan alami.</b>
 * {@code GeneralValueObject#compareTo(GeneralValueObject)} memakai {@code nomorUrut} sebagai
 * kunci pertama dan hanya turun ke {@code nama} bila salah satu sisi {@code null} — di kelas ini
 * hal itu mustahil, jadi cabang {@code nama} <b>tidak pernah</b> tercapai. Semua baris dengan
 * nomor urut sama (yaitu seluruh baris bawaan, yang semuanya bernilai 1) dianggap setara.
 * Jangan pernah menaruh entity ini di {@code TreeSet}/{@code TreeMap}: baris-barisnya akan
 * menciut menjadi satu. Jalur yang ada sekarang aman karena semuanya memakai {@code List}
 * ({@code ConstantValues.simpleList} dan {@code SimpleListModel}) dengan urutan dari SQL
 * ({@code ORDER BY nomorUrut, nama}).</li>
 * <li><b>{@code nama} tidak unik.</b> Kolomnya {@code nullable = false} tetapi tanpa
 * {@code unique}; dua butir bernama sama (mis. satu global + satu milik sekolah) akan muncul dua
 * kali pada formulir harian sekolah tersebut.</li>
 * <li><b>{@code nomorUrut} dan {@code aktif} tidak punya {@code @Column}</b>, berbeda dari
 * {@code nama}/{@code keterangan}/{@code id}. Nama kolomnya karena itu diturunkan dari nama
 * properti apa adanya oleh penamaan bawaan JPA (tidak ada {@code naming_strategy} di
 * {@code hibernate.cfg.xml}) — bukan {@code nomor_urut} seperti konvensi tabel lain di repo ini.
 * Jangan menambahkan {@code @Column(name = "nomor_urut")} tanpa migrasi data: skema dikelola
 * {@code hbm2ddl.auto=update} sehingga perubahan itu akan membuat kolom BARU yang kosong dan
 * meninggalkan data lama tak terbaca.</li>
 * <li><b>Kolom {@code aktif} tidak punya isian di formulir</b>; statusnya hanya dapat diubah
 * lewat checkbox pada grid daftar. Kotak centang pencarian <i>"Tampilkan hanya yang aktif"</i>
 * pada {@code jenis_aktiftas_harian_default.zul} ({@code id="searchaktif"}) <b>tidak
 * berfungsi</b>: {@code JenisAktiftasHarianDefaultAction} tidak punya field bernama
 * {@code searchaktif} sehingga komponen itu tak pernah ter-<i>autowire</i>, dan
 * {@code initCriteria(boolean)} tidak pernah menyaring kolom {@code aktif}. Butir non-aktif tetap
 * tampil di grid master walau kotak itu tercentang. (Penyaringan {@code aktif} yang sungguh
 * berjalan hanya ada di sisi pembaca, yaitu pengisian formulir harian.) Gejala identik sudah
 * tercatat pada {@code JenisGuru}.</li>
 * <li><b>Isian "Yayasan *" pada formulir praktis mubazir</b> — {@code onSave(...)} mewajibkannya,
 * tetapi nilai pilihan akan ditimpa {@link #getYayasan()} saat flush selama "Sekolah *" (yang
 * juga wajib) terisi. Yayasan efektif selalu mengikuti sekolah.</li>
 * <li><b>Audit Envers aktif</b> ({@code @Audited}) — setiap perubahan, termasuk perubahan tak
 * disengaja akibat getter penulis-balik dan {@code Intbox} tanpa gerbang di atas, menghasilkan
 * revisi pada tabel audit.</li>
 * <li><b>{@code @org.hibernate.annotations.Entity} sudah usang</b> pada Hibernate modern; ia
 * dipertahankan di sini hanya untuk {@code dynamicInsert}/{@code dynamicUpdate} (hanya kolom yang
 * benar-benar berubah yang ikut di INSERT/UPDATE).</li>
 * <li><b>Salah eja "Aktiftas" bersifat mengikat.</b> Ejaan itu (bukan "Aktifitas"/"Aktivitas")
 * dipakai pada nama kelas, nama tabel, nama berkas {@code .zul}, dan nama Action. Label yang
 * dilihat pengguna justru memakai ejaan lain ("Aktivitas Default", "Aktifitas Harian Siswa").
 * Jangan "membetulkan" ejaan tanpa migrasi skema.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ol>
 * <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 * {@link #setOlehId(String)}, {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)},
 * dan kait {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}, {@link #setId(Long)}, dua konstruktor.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 * {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 * <li><b>Muatan data</b> — {@link #getNama()}, {@link #setNama(String)},
 * {@link #getKeterangan()}, {@link #setKeterangan(String)}, {@link #getNomorUrut()},
 * {@link #setNomorUrut(Integer)}, {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 * </ol>
 *
 * <p>Tidak ada satu pun method dengan logika bisnis, query statis, maupun helper perhitungan di
 * kelas ini — seluruh perilaku non-trivial terkonsentrasi pada empat getter yang menulis balik
 * atau menormalkan ({@link #getSekolah()}, {@link #getYayasan()}, {@link #getAktif()},
 * {@link #getNomorUrut()}) dan pada dua setter audit yang menolak nilai kosong secara diam-diam
 * ({@link #setOleh(String)}, {@link #setOlehId(String)}).</p>
 *
 * @see AktiftasHarianSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "jenis_aktiftas_harian_default", schema = "sekolah")
public class JenisAktiftasHarianDefault extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar baris yang tersimpan di sesi/cache lama tetap
	 * terbaca setelah kelas ini dikompilasi ulang; jangan diubah tanpa alasan kuat.
	 *
	 * <p>Nilai ini identik dengan milik {@code JenisGuru} dan {@code JenisMateriHarianDefault} —
	 * sisa jejak salin-tempel, bukan hubungan tipe. Tidak berbahaya karena ketiganya kelas
	 * berbeda, tetapi jangan dijadikan patokan bahwa mereka sekeluarga.</p>
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/**
	 * Kunci utama tabel {@code sekolah.jenis_aktiftas_harian_default}; dideklarasikan ulang karena
	 * induk tidak dipetakan Hibernate.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi otomatis oleh interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah, dengan <b>penolakan diam-diam</b>: nilai {@code null} atau
	 * yang hanya berisi spasi diabaikan sehingga jejak audit lama tetap dipertahankan.
	 *
	 * <p>Pemanggil utamanya {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan kode
	 * layar. Perilaku "tolak nilai kosong" itu disengaja agar operasi penyimpanan tanpa konteks
	 * pengguna (mis. penyemaian awal, tugas terjadwal) tidak menghapus catatan siapa yang terakhir
	 * benar-benar mengubah baris. Karena itu setter ini <b>tidak dapat</b> dipakai untuk
	 * mengosongkan kolom.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa pesan kesalahan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah, dengan penolakan diam-diam yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa pesan kesalahan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA yang dijalankan <b>tepat sebelum</b> setiap {@code UPDATE} baris ini, meneruskan
	 * instance ke {@code AuditTimestampInterceptor.ubah(Object)} agar {@code oleh},
	 * {@code olehId}, dan {@link #getTanggal_dirubah()} diperbarui dari konteks pengguna aktif.
	 *
	 * <p><b>Efek samping:</b> memutasi state instance ini. Tidak dipanggil pada {@code INSERT}
	 * pertama (tidak ada {@code @PrePersist} di kelas ini) sehingga baris baru mengandalkan nilai
	 * awal field {@code tanggal_dirubah} dan pengisian oleh interceptor Hibernate. Jangan
	 * dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diberi nilai awal waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tidak pernah tersimpan dengan
	 * kolom kosong, lalu diperbarui {@link #onUpdate()} pada setiap perubahan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; biasanya dipanggil interceptor
	 * audit, bukan kode layar.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik butir; {@code null} berarti butir berlaku GLOBAL untuk semua sekolah. */
	private Sekolah sekolah;
	/** Yayasan pemilik butir; nilainya selalu diturunkan ulang dari {@link #getSekolah()}. */
	private Yayasan yayasan;
	/** Keterangan bebas; hanya tampil sebagai kolom informasi pada grid master. */
	private String keterangan;
	/** Label butir aktivitas yang disalin apa adanya ke formulir harian. Wajib, tidak unik. */
	private String nama;
	/** Urutan tampil butir pada formulir harian; dinormalkan menjadi 1 bila {@code null}. */
	private Integer nomorUrut;
	/** Saklar tampil/tidak pada formulir harian; dinormalkan menjadi {@code true} bila {@code null}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat baris.
	 * Dipakai juga oleh layar master ({@code onAdd}) dan oleh penyemai bawaan
	 * {@code AktiftasHarianSiswaAction#initDefaultMasterData()}.
	 */
	public JenisAktiftasHarianDefault() {
	}

	/**
	 * Konstruktor pintas untuk membuat objek dengan identitas dan label sekaligus.
	 *
	 * <p><b>Catatan:</b> tidak ada satu pun pemanggil di dalam repo saat ini — konstruktor ini
	 * ikut terbawa dari template generator. Perhatikan juga bahwa parameternya bertipe primitif
	 * {@code long} sehingga tidak dapat dipakai untuk objek baru yang id-nya masih {@code null};
	 * pemakaiannya hanya masuk akal untuk objek referensi/detached yang id-nya sudah diketahui.</p>
	 *
	 * @param id   kunci utama baris yang sudah ada
	 * @param nama label butir aktivitas
	 */
	public JenisAktiftasHarianDefault(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolomnya {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code IDENTITY}); id berurutan dan mudah ditebak, jadi jangan pernah dijadikan penanda
	 * rahasia. Anotasi {@code @Id} yang menempel pada getter inilah yang membuat <b>seluruh</b>
	 * kelas dipetakan dengan akses properti — sebabnya semua normalisasi di getter lain ikut
	 * tertulis ke basis data.</p>
	 *
	 * @return kunci utama, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Tanpa validasi; umumnya hanya Hibernate yang memanggilnya.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik butir, setelah dikanonikalkan lewat
	 * {@code GeneralValueObject.check(Object)}.
	 *
	 * <p><b>Efek samping:</b> getter ini <b>menulis balik</b> field {@code sekolah} dengan hasil
	 * {@code check(...)} — mekanisme bersama seluruh entity AIS yang menukar proxy lazy/salinan
	 * usang dengan instance kanonik dari {@code EntityIdentityMap}/cache. Bukan getter murni;
	 * pemanggilannya dapat memicu inisialisasi lazy sehingga selalu jalankan di dalam session
	 * Hibernate yang masih terbuka.</p>
	 *
	 * <p>Nilai {@code null} bermakna khusus di sini: butir <b>global</b>, yang oleh
	 * {@code AktiftasHarianSiswaAction} disertakan pada formulir harian <i>seluruh</i> sekolah
	 * ({@code Restrictions.or(isNull("sekolah"), eq("sekolah", ...))}). Keempat butir bawaan hasil
	 * auto-seed berada dalam kondisi ini.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila butir berlaku global
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik butir, dengan normalisasi: objek {@code null} <b>maupun</b> objek
	 * yang id-nya masih {@code null} (mis. baris kosong pilihan combobox) sama-sama disimpan
	 * sebagai {@code null}, sehingga tidak ada referensi transient yang lolos ke Hibernate.
	 *
	 * <p>Konsekuensi yang perlu diingat: karena {@code null} berarti "global", memilih entri
	 * kosong pada combobox tidak menghasilkan kesalahan melainkan <b>mempromosikan</b> butir
	 * menjadi berlaku untuk semua sekolah. Layar master menutup jalur itu dengan mewajibkan
	 * "Sekolah *" terisi — yang justru menimbulkan jebakan sebaliknya (lihat catatan kelas).</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null} (butir global)
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik butir — <b>selalu diturunkan ulang dari sekolah</b>.
	 *
	 * <p><b>Efek samping (penting):</b> selama {@link #getSekolah()} tidak {@code null}, method ini
	 * menimpa field {@code yayasan} dengan {@code sekolah.getYayasan()}. Nilai apa pun yang
	 * sebelumnya disetel lewat {@link #setYayasan(Yayasan)} akan hilang — dan karena kelas ini
	 * dipetakan dengan akses properti, nilai hasil penimpaan itulah yang ditulis ke kolom
	 * {@code yayasan_id} saat flush. Yayasan karena itu <b>tidak dapat</b> berbeda dari yayasan
	 * sekolahnya, dan isian "Yayasan *" pada formulir master efektif hanya hiasan.</p>
	 *
	 * <p>Method ini juga memanggil {@code check(...)} pada hasilnya sehingga dapat memicu
	 * inisialisasi lazy dua tingkat (sekolah, lalu yayasan) — jangan dipanggil di luar session.</p>
	 *
	 * @return yayasan pemilik; {@code null} bila butir global atau bila sekolahnya belum punya
	 *         yayasan
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
	 * Menyetel yayasan pemilik butir, dengan normalisasi yang sama seperti
	 * {@link #setSekolah(Sekolah)} ({@code null} atau objek tanpa id menjadi {@code null}).
	 *
	 * <p><b>Nilai yang disetel di sini tidak bertahan</b> bila {@code sekolah} terisi:
	 * {@link #getYayasan()} akan menurunkannya ulang dari sekolah sebelum flush. Setter ini hanya
	 * benar-benar berpengaruh untuk butir global ({@code sekolah == null}) — sebuah kombinasi yang
	 * tidak dapat dibuat lewat layar master.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas butir ini, <b>apa adanya</b>.
	 *
	 * <p><b>Kontrak induk dibalik:</b> {@code GeneralValueObject.getKeterangan()} menjamin tidak
	 * pernah mengembalikan {@code null} (mengubahnya menjadi {@code ""}); override ini
	 * mengembalikan {@code null} bila kolomnya kosong. Konsekuensinya, kode yang memegang tipe
	 * induk dan mengandalkan jaminan "tidak pernah null" bisa gagal pada entity ini. Sejauh ini
	 * pemakaiannya aman: cabang {@code keterangan} pada
	 * {@code GeneralValueObject#compareTo(GeneralValueObject)} tidak pernah tercapai untuk kelas
	 * ini (lihat catatan {@link #getNomorUrut()}), dan renderer grid master hanya meneruskannya ke
	 * {@code Label} ZK.</p>
	 *
	 * @return keterangan butir, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas butir ini. Tanpa validasi maupun pemangkasan spasi; nilai
	 * {@code null} diterima.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label butir aktivitas.
	 *
	 * <p>Nilai inilah yang <b>disalin apa adanya</b> ke dalam JSON kolom
	 * {@code AktiftasHarianSiswa.aktifitas} saat sebuah catatan harian baru dibuat, dan yang
	 * dipakai {@code RevisiHelper.createNewRevisi(...)} sebagai judul riwayat revisi pada grid
	 * master.</p>
	 *
	 * @return label butir; {@code null} hanya mungkin untuk objek yang belum pernah disimpan
	 *         (kolomnya {@code nullable = false})
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel label butir aktivitas. Tanpa validasi di lapis entity — kewajiban isi dan
	 * pemangkasan spasi dilakukan {@code JenisAktiftasHarianDefaultAction#onSave(Event)}.
	 *
	 * <p>Tidak ada penjagaan keunikan: dua butir bernama sama dapat tersimpan dan akan tampil dua
	 * kali pada formulir harian bila keduanya terjangkau sekolah yang sama (mis. satu global dan
	 * satu milik sekolah tersebut).</p>
	 *
	 * @param nama label butir baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan saklar tampil butir, dengan <b>normalisasi {@code null} menjadi
	 * {@code true}</b>.
	 *
	 * <p><b>Efek samping tak kasat mata:</b> karena kelas ini dipetakan dengan akses properti,
	 * nilai {@code true} hasil normalisasi itulah yang ditulis Hibernate ke kolom saat flush.
	 * Sebuah baris yang kolom {@code aktif}-nya {@code NULL} di basis data akan "sembuh sendiri"
	 * menjadi {@code true} pada penyimpanan berikutnya. Artinya butir default bersifat
	 * <b>aktif kecuali dinyatakan sebaliknya</b> — tidak ada butir yang tersembunyi karena lupa
	 * diisi.</p>
	 *
	 * <p>Pembacanya yang benar-benar menegakkan saklar ini hanya satu:
	 * {@code AktiftasHarianSiswaAction#init(AktiftasHarianSiswa)} menambahkan
	 * {@code Restrictions.eq("aktif", true)} saat mengambil daftar butir untuk catatan baru.
	 * Perhatikan syarat {@code eq(true)} itu <b>tidak</b> cocok dengan baris yang kolomnya masih
	 * {@code NULL} di basis data, walaupun getter ini menganggapnya aktif — divergensi layar vs
	 * SQL yang perlu diingat bila ada baris lama hasil migrasi.</p>
	 *
	 * <p>Grid master menampilkannya sebagai checkbox "Aktif" yang benar dinonaktifkan bila
	 * pengguna tidak punya hak UPDATE.</p>
	 *
	 * @return {@code true} bila butir ditawarkan pada formulir harian; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar tampil butir. Tanpa validasi; {@code null} diterima tetapi akan terbaca
	 * sebagai {@code true} lewat {@link #getAktif()} dan tertulis demikian pada flush berikutnya.
	 *
	 * <p>Satu-satunya pemanggil dari layar adalah event {@code onCheck} checkbox "Aktif" pada grid
	 * master, yang langsung diikuti {@code Common.refreshSaveOrUpdate(...)} — jadi pencentangan
	 * tersimpan seketika tanpa tombol Simpan.</p>
	 *
	 * @param aktif saklar tampil baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan urutan tampil butir pada formulir harian, dengan <b>normalisasi {@code null}
	 * menjadi {@code 1}</b>.
	 *
	 * <p><b>Efek samping tak kasat mata:</b> sama seperti {@link #getAktif()}, nilai hasil
	 * normalisasi inilah yang ditulis ke kolom saat flush karena pemetaan memakai akses properti.
	 * Keempat butir bawaan hasil auto-seed karena itu semuanya berakhir dengan nomor urut 1, dan
	 * urutan tampilnya ditentukan kunci kedua {@code nama} di dalam SQL
	 * ({@code ORDER BY nomorUrut, nama}) — bukan urutan penulisan di kode penyemai.</p>
	 *
	 * <p><b>Dampak pada pengurutan alami (non-obvious):</b>
	 * {@code GeneralValueObject#compareTo(GeneralValueObject)} memakai {@code nomorUrut} sebagai
	 * kunci pertama dan baru turun ke {@code nim}/{@code nama}/{@code keterangan} bila salah satu
	 * sisi {@code null}. Karena getter ini tidak pernah {@code null}, cabang-cabang berikutnya
	 * <b>tidak pernah</b> tercapai untuk kelas ini: semua butir bernomor urut sama dianggap setara
	 * ({@code compareTo} mengembalikan 0). Menaruh entity ini di {@code TreeSet}/{@code TreeMap}
	 * akan menciutkan seluruh butir bernomor sama menjadi satu elemen. Jalur yang ada sekarang
	 * aman karena semuanya memakai {@code List}.</p>
	 *
	 * <p><b>Catatan hak akses:</b> {@code Intbox} nomor urut pada grid master tidak pernah
	 * dinonaktifkan, sehingga nilai ini dapat diubah dan langsung tersimpan oleh pengguna yang
	 * hanya punya hak BACA. Lihat uraian di Javadoc kelas.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel urutan tampil butir. Tanpa validasi — nilai negatif atau duplikat diterima; nilai
	 * {@code null} akan terbaca sebagai {@code 1} lewat {@link #getNomorUrut()}.
	 *
	 * <p>Satu-satunya pemanggil dari layar adalah event {@code onChange} pada {@code Intbox} grid
	 * master, yang langsung diikuti {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan
	 * tersimpan seketika tanpa tombol Simpan (dan tanpa pemeriksaan hak akses).</p>
	 *
	 * @param nomorUrut nomor urut tampil baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
