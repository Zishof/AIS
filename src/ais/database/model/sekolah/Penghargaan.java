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
 * Entity MASTER butir penghargaan siswa pada modul sekolah — satu baris tabel
 * {@code sekolah.penghargaan} mewakili SATU jenis penghargaan/hadiah yang dapat diberikan kepada
 * seorang siswa (mis. "Piagam Juara Umum", "Beasiswa Prestasi Satu Semester", "Piala Bergilir"),
 * lengkap dengan BOBOT POIN-nya pada field {@link #getPoin()}.
 *
 * <p>Kelas ini adalah DAFTAR JENIS, BUKAN catatan pemberian penghargaan kepada siswa tertentu.
 * Catatan per siswa disimpan di entity transaksi terpisah
 * {@link ais.database.model.sekolah.ApresiasiSiswa} (tabel {@code sekolah.apresiasi_siswa}) lewat
 * tabel jembatan {@code sekolah.apresiasi_siswa_has_penghargaan}. Konsekuensinya: mengubah nama
 * atau bobot sebuah baris di sini berdampak RETROAKTIF ke seluruh catatan siswa yang sudah pernah
 * merujuknya, karena catatan itu menyimpan FK — bukan salinan nilai.</p>
 *
 * <h3>Posisi dalam rantai apresiasi (4 lapis)</h3>
 * Modul apresiasi sekolah tersusun berlapis, dan entity ini berada di lapis paling dasar
 * berdampingan dengan {@link ais.database.model.sekolah.Apresiasi}:
 * <ol>
 * <li><b>{@link ais.database.model.sekolah.Apresiasi}</b> (tabel {@code sekolah.apresiasi}) —
 * master BUTIR apresiasi + bobot {@code kredit}. Perannya SEJAJAR (bukan induk maupun turunan)
 * dengan kelas ini; keduanya berdiri sendiri.</li>
 * <li><b>{@code Penghargaan}</b> (kelas ini, tabel {@code sekolah.penghargaan}) — master JENIS
 * penghargaan/hadiah yang menyertai + bobot {@link #getPoin() poin}.</li>
 * <li><b>{@link ais.database.model.sekolah.ApresiasiDanPenghargaan}</b> (tabel
 * {@code sekolah.apresiasi_dan_penghargaan}) — PAKET bernama yang menggabungkan sekumpulan
 * {@code Apresiasi} dan sekumpulan {@code Penghargaan} lewat dua tabel penghubung
 * {@code @ManyToMany} ({@code apresiasi_dan_penghargaan_has_apresiasi} dan
 * {@code apresiasi_dan_penghargaan_has_penghargaan}). Paket inilah yang dipilih petugas saat
 * mencatat kejadian, bukan baris entity ini secara langsung. Perhatikan: meski namanya terdengar
 * seperti transaksi, lapis ini MASIH master.</li>
 * <li><b>{@link ais.database.model.sekolah.ApresiasiSiswa}</b> (tabel
 * {@code sekolah.apresiasi_siswa}) — barulah ini entity TRANSAKSI: siswa X menerima paket Y pada
 * waktu Z, dengan himpunan butir {@code Apresiasi}/{@code Penghargaan} yang benar-benar dicentang
 * untuk kejadian itu.</li>
 * </ol>
 * Struktur ini adalah CERMINAN PERSIS sisi tata tertib: {@code Pelanggaran} (kredit) +
 * {@code Hukuman} (poin) &rarr; {@code PelanggaranDanHukuman} &rarr; {@code PelanggaranSiswa}.
 * Padanan struktural kelas ini di sisi sana adalah {@code Hukuman} — sama-sama lapis dasar
 * pemegang kolom {@code poin}, dengan daftar field yang nyaris identik (lihat catatan
 * "Perbedaan dari {@code Hukuman}" di bawah).
 *
 * <p>Baris di sini <b>dirujuk dari dua tabel jembatan berbeda</b>, dan kedua relasi itu
 * dideklarasikan <b>hanya di sisi seberang</b> — kelas ini sama sekali tidak punya koleksi balik.
 * Menghapus satu baris penghargaan berarti memutus rujukan pada catatan apresiasi siswa yang sudah
 * terlanjur menyimpannya (riwayat prestasi historis), tanpa peringatan apa pun dari sisi entity
 * ini.</p>
 *
 * <h3>Kolom bisnis</h3>
 * <ul>
 * <li>{@code nama} — label jenis penghargaan; satu-satunya kolom {@code nullable=false} dan
 * satu-satunya yang divalidasi layar. Sekaligus menjadi kunci pengurutan efektif entity ini
 * (lihat {@link #getNama()}).</li>
 * <li>{@code poin} — bobot poin prestasi. Bukan sekadar metadata: nilai ini dijumlahkan menjadi
 * parameter {@code "point"} pada {@code LaporanApresiasiSiswa} dan muncul per kejadian di rapor.
 * Daftar pemakai lengkapnya ada pada {@link #getPoin()}.</li>
 * <li>{@code keterangan} — teks bebas penjelas.</li>
 * <li>{@code aktif} — penanda baris masih dipakai. <b>Berbeda dari padanannya di {@code Hukuman}
 * (yang nol pembaca), penanda ini di sini BENAR-BENAR menyaring</b> — lihat
 * {@link #getAktif()}.</li>
 * <li>{@code sekolah} / {@code yayasan} — cakupan kepemilikan data pada instalasi
 * multi-tenant.</li>
 * </ul>
 *
 * <h3>Siapa yang benar-benar membaca {@code poin}</h3>
 * Nilai {@link #getPoin()} TIDAK diagregasi di dalam entity ini; seluruh penjumlahan dilakukan
 * pemanggil. Ada empat kelas pembaca runtime, dan salah satunya RUSAK:
 * <ol>
 * <li>{@code ais.action.master.sekolah.PenghargaanAction} — kolom "Poin" pada grid master dan
 * isian {@code MyDoublebox} pada dialog Tambah/Ubah.</li>
 * <li>{@code ais.action.master.sekolah.ApresiasiSiswaAction.loadPenghargaan(...)} — label
 * checkbox pilihan penghargaan. Lihat catatan cacat label pada {@link #getPoin()}.</li>
 * <li>{@code ais.action.report.format1.sekolah.LaporanApresiasiSiswa} — dua tempat kembar (jalur
 * rekap periode dan jalur cetak satu nota) menjumlahkan {@code getPoin()} seluruh anggota
 * {@code apresiasiSiswa.getPenghargaans()} ke parameter laporan {@code "point"}, berdampingan
 * dengan {@code "kredit"} dari {@code Apresiasi.getKredit()}. <b>Jalur ini benar.</b></li>
 * <li>{@code ais.action.report.format1.sekolah.LaporanRaporSiswa.masukkanPoin(...)} — bobot per
 * penghargaan dan subtotal per kejadian pada rapor siswa. <b>Jalur ini mengandung cacat
 * akumulator, lihat bagian berikut.</b></li>
 * </ol>
 *
 * <h3>Cacat akumulator total poin di rapor siswa (TERVERIFIKASI)</h3>
 * Pada {@code LaporanRaporSiswa.masukkanPoin(...)} terdapat cacat salin-tempel yang membuat
 * <b>parameter total pada rapor selalu bernilai {@code 0.0}</b>. Alurnya:
 * <ul>
 * <li>Variabel lokal {@code totalPointPenghargaan} dideklarasikan {@code 0.0}, lalu di dalam
 * lingkaran butir yang di-{@code +=} justru variabel <b>{@code totalPointKegiatan}</b> — bukan
 * {@code totalPointPenghargaan}. Akibatnya parameter
 * {@code "totalPointPenghargaan_" + siswa.getId()} yang dikirim ke template laporan
 * <b>selalu {@code 0.0}</b>, berapa pun bobot penghargaan yang diterima siswa.</li>
 * <li>Cacat KEMBAR PERSIS ada pada blok sebelumnya di method yang sama untuk
 * {@code totalPointHukuman} (bobot dari {@code Hukuman.getPoin()}), sehingga
 * {@code "totalPointHukuman_..."} juga selalu {@code 0.0}. Ini menegaskan temuan batch 42 dan 47:
 * bug tersebut memang SEPASANG, satu di sisi disiplin dan satu di sisi apresiasi, di berkas yang
 * sama.</li>
 * <li><b>Nuansa yang mudah terlewat dan memperkuat temuan:</b> penjumlahan yang "salah alamat"
 * itu <b>bahkan tidak bocor</b> ke total kegiatan. Parameter
 * {@code "totalPointKegiatan_" + siswa.getId()} sudah di-{@code put} ke peta parameter
 * <i>sebelum</i> kedua blok tersebut berjalan, dan {@code Double} bersifat <i>immutable</i>
 * sehingga nilai yang terlanjur tersimpan tidak ikut berubah. Jadi kedua baris {@code +=} itu
 * adalah <b>kode mati sepenuhnya</b>: tidak menambah total penghargaan, tidak menambah total
 * hukuman, dan tidak pula menambah total kegiatan.</li>
 * <li><b>Yang TIDAK terdampak:</b> subtotal per kejadian ({@code map.put("poin", ...)}) memakai
 * akumulator lokal {@code pointPenghargaan} yang benar, dan bobot per butir
 * ({@code "poin_" + id}) juga benar. Jadi baris rincian pada rapor menampilkan angka yang tepat —
 * hanya baris TOTAL yang nol. Inilah sebabnya cacat ini bisa bertahan lama tanpa terdeteksi:
 * rapor tetap terlihat "berisi".</li>
 * <li>Tidak ditemukan berkas template ({@code .jrxml}) di dalam pohon sumber yang merujuk nama
 * parameter tersebut, sehingga dampak terlihat/tidaknya bergantung pada template yang diunggah
 * masing-masing instalasi lewat {@code JenisRaporSiswa}. Bila template memuat bidang itu,
 * pengguna melihat total 0 di samping rincian yang tidak nol.</li>
 * </ul>
 *
 * <h3>Perbedaan dari {@code Hukuman} (padanan strukturalnya)</h3>
 * <ul>
 * <li><b>TIDAK ada field {@code perguruanTinggi}</b> — sehingga tidak ada getter pengisi-otomatis
 * tenant terluar yang "mengklaim" baris berkolom {@code NULL} atas nama sesi pembaca, dan tenant
 * terluar tidak dapat dipakai sebagai sumbu penyaringan di modul ini. Sama seperti
 * {@link ais.database.model.sekolah.Apresiasi}.</li>
 * <li><b>TIDAK di-preload ke cache tingkat aplikasi</b> — {@code Hukuman.class} terdaftar pada
 * {@code ais.common.InitData.initClasses(...)}, sedangkan kelas ini tidak ada di daftar mana pun
 * di {@code InitData}. (Yang muncul di sana adalah {@code ais.database.model.KategoriPenghargaan},
 * entity yang sama sekali berbeda milik modul perguruan tinggi — jangan tertukar.) Isinya selalu
 * dibaca langsung dari basis data setiap layar dibuka; ini menghindarkan modul apresiasi dari
 * amplifier cache app-wide yang tercatat pada modul pelanggaran, dengan ongkos satu query
 * tambahan per pembukaan layar.</li>
 * <li><b>Kolom {@code aktif} benar-benar dipakai</b> — di {@code Hukuman} penanda ini murni
 * informatif, di sini ia menyaring daftar butir pada layar paket. Lihat {@link #getAktif()}.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, dua konstruktor.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 * {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 * <li><b>Isi butir</b> — {@link #getNama()}, {@link #getKeterangan()}, {@link #getPoin()},
 * {@link #getAktif()} beserta setter-nya.</li>
 * </ul>
 * Entity ini TIDAK memiliki satu pun method bisnis, query/finder statis, {@code equals}/
 * {@code hashCode}, maupun {@code compareTo()} sendiri. Seluruh isinya adalah getter/setter
 * properti dan satu callback JPA.
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 * <li><b>Komentar hbm2java di atas anotasi keliru</b> — teks aslinya berbunyi "JenisGuru generated
 * by hbm2java", sisa salin-tempel generator. Kelas ini tidak ada hubungannya dengan jenis guru.
 * Kekeliruan yang sama muncul di {@link ais.database.model.sekolah.Apresiasi},
 * {@link ais.database.model.sekolah.ApresiasiDanPenghargaan}, dan seluruh keluarga
 * pelanggaran.</li>
 * <li><b>{@code serialVersionUID} kembar lintas modul</b> — nilai {@code -7490758846785025664L}
 * dipakai ulang persis oleh {@code Apresiasi}, {@code ApresiasiDanPenghargaan}, dan keluarga
 * pelanggaran. Tidak berbahaya (serialisasi Java tetap memeriksa nama kelas), tetapi menegaskan
 * asal-usul salin-tempel berkas ini.</li>
 * <li><b>Field warisan yang dideklarasikan ULANG bukan bug</b> — {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} sudah ada di induk
 * {@link ais.database.model.GeneralValueObject}, namun induk itu BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — hanya POJO abstrak biasa. Hibernate TIDAK memetakan properti induk,
 * sehingga setiap entity turunan HARUS mendeklarasikan ulang keempatnya agar tersimpan. Ini
 * keharusan teknis; jangan "dirapikan".</li>
 * <li><b>Field induk yang tetap ada tapi selalu {@code null}</b> — {@code nama} dan
 * {@code keterangan} di kelas ini MEMBAYANGI (<i>shadow</i>) field bernama sama milik induk,
 * sedangkan {@code nomorUrut} dan {@code nim} milik induk tidak pernah dipetakan maupun diisi
 * untuk entity ini. Ini yang menentukan perilaku pengurutan di butir berikut.</li>
 * <li><b>Dua getter melakukan MUTASI saat dibaca</b> — {@link #getSekolah()} menulis balik hasil
 * resolusi proxy, dan {@link #getYayasan()} bahkan MENIMPA nilai kolomnya dengan yayasan turunan
 * dari sekolah. Karena entity beranotasi {@code dynamicUpdate} dan dipetakan dengan <i>property
 * access</i> ({@code @Id} ada di getter), pembacaan pada entity yang masih ter-<i>attach</i> ke
 * session dapat memicu {@code UPDATE} nyata plus revisi Envers palsu saat flush. Rinciannya ada
 * pada Javadoc masing-masing getter.</li>
 * <li><b>Pengurutan mewarisi {@code compareTo} induk &rarr; risiko penciutan senyap
 * {@code TreeSet}</b> — kunci urut induk adalah {@code nomorUrut} &rarr; {@code nim} &rarr;
 * {@code nama} &rarr; {@code keterangan}; karena dua kunci pertama selalu {@code null} di sini,
 * kunci efektifnya LANGSUNG jatuh ke {@code nama}. Dampaknya nyata dan sudah terverifikasi —
 * lihat {@link #getNama()}.</li>
 * <li><b>{@link #getKeterangan()} TIDAK membalik kontrak</b> — berbeda dari sejumlah entity lain
 * di repo ini yang getter {@code keterangan}-nya mengembalikan sesuatu selain isi kolomnya, di
 * sini kolom {@code keterangan} nyata ada dan dikembalikan apa adanya.</li>
 * </ul>
 *
 * <h3>Layar, impor/ekspor, dan audit</h3>
 * Layar CRUD-nya {@code ais.action.master.sekolah.PenghargaanAction} (366 baris), dengan kolom
 * ekspor/impor Excel {@code {"id", "nama", "poin", "sekolah", "keterangan", "aktif"}} — tombol
 * unggah hanya muncul bagi pengguna yang memegang hak {@code CREATE}, {@code UPDATE}, DAN
 * {@code DELETE} sekaligus. Layar itu memanggil {@code Common.doCheckSecurity()} pada
 * {@code doBeforeCompose} dan menggerbangi tombol Tambah/Ubah/Hapus dengan
 * {@code CommonPrivilages.checkPrevilages(...)}, jadi gerbang perannya utuh.
 *
 * <p><b>Catatan cakupan tenant.</b> {@code PenghargaanAction.initCriteria(...)} membangun
 * {@code Criteria} atas kelas ini <b>tanpa satu pun batasan cakupan otomatis</b>: satu-satunya
 * penyaring adalah kotak pencarian yang dipilih pengguna sendiri (nama, sekolah, yayasan), dan
 * bila dibiarkan kosong query jatuh ke {@code Restrictions.sqlRestriction("1=1")}. Artinya daftar
 * bawaan menampilkan katalog penghargaan SELURUH sekolah/yayasan pada instalasi multi-tenant.
 * Pola "nol filter tenant" ini identik dengan yang tercatat pada beberapa layar master lain;
 * <b>tingkat keparahannya rendah</b> karena isinya metadata katalog (bukan data pribadi siswa),
 * tetapi ia tetap membocorkan struktur pembinaan sekolah lain dan — dipadu hak
 * {@code UPDATE}/{@code DELETE} — memungkinkan penyuntingan katalog milik tenant lain.</p>
 *
 * <p>Baris master ini juga muncul sebagai daftar checkbox pada layar paket
 * {@code ApresiasiDanPenghargaanAction} dan pada dialog transaksi {@code ApresiasiSiswaAction}.
 * Entity beranotasi {@link Audited} sehingga setiap perubahan direkam Hibernate Envers ke tabel
 * bayangan.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.Apresiasi
 * @see ais.database.model.sekolah.ApresiasiDanPenghargaan
 * @see ais.database.model.sekolah.ApresiasiSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "penghargaan", schema = "sekolah")
public class Penghargaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini kebetulan IDENTIK dengan milik
	 * {@link ais.database.model.sekolah.Apresiasi},
	 * {@link ais.database.model.sekolah.ApresiasiDanPenghargaan}, serta seluruh keluarga
	 * pelanggaran/hukuman (sisa salin-tempel generator); jangan diubah karena instance entity ikut
	 * diserialisasi ke dalam state desktop ZK.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Kunci utama, dibangkitkan basis data ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila baris belum pernah diubah lewat interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Setter ini SENGAJA tidak simetris: nilai {@code null} atau string kosong/spasi DIABAIKAN
	 * diam-diam sehingga nilai lama tetap bertahan. Efeknya, jejak audit tidak pernah bisa
	 * dikosongkan kembali setelah terisi — termasuk saat Hibernate menghidrasi ulang baris yang
	 * kolomnya {@code NULL} ke instance yang sudah memegang nilai sebelumnya.</p>
	 *
	 * <p>Pemanggil normalnya adalah {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan kode layar.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini, dengan penjagaan "tolak nilai
	 * kosong" yang sama persis dengan {@link #setOlehId(String)} — nilai {@code null}/kosong
	 * diabaikan diam-diam sehingga jejak audit lama tidak terhapus.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
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
	 * @return nama pengguna, atau {@code null} bila baris belum pernah diubah lewat interceptor
	 *         audit
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * {@code UPDATE} baris ini dikirim ke basis data.
	 *
	 * <p>Efek samping: mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #oleh}/{@link #olehId} dari pengguna sesi aktif dan menyegarkan
	 * {@link #tanggal_dirubah}. TIDAK dipanggil pada {@code INSERT} (tidak ada
	 * {@code @PrePersist}), sehingga baris baru mengandalkan nilai awal field
	 * {@link #tanggal_dirubah} serta pengisian {@code oleh} dari jalur lain.</p>
	 *
	 * <p><b>Jangan dipanggil manual.</b> Method ini {@code protected} dan sepenuhnya milik
	 * lifecycle Hibernate/JPA. Perlu disadari pula bahwa satu klik centang "Aktif" pada grid
	 * master (lihat {@link #setAktif(Boolean)}) sudah cukup untuk memicu jalur ini dan menimpa
	 * jejak audit baris master.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server ({@code WaktuUtil.getDate()}) saat
	 * instance dibuat, sehingga baris yang baru di-{@code INSERT} pun sudah punya stempel waktu
	 * meski {@link #onUpdate()} belum pernah berjalan. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini menerima
	 * {@code null} apa adanya (tidak ada penjagaan). Umumnya dipanggil oleh
	 * {@code AuditTimestampInterceptor}, bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori, tetapi bisa {@code null} bila kolomnya kosong di basis data dan
	 *         nilainya di-{@code set} eksplisit
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik baris ini (cakupan tenant paling sempit). Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik baris ini. <b>Bukan</b> nilai independen: {@link #getYayasan()} selalu
	 * menurunkannya kembali dari {@link #sekolah} bila sekolah terisi.
	 */
	private Yayasan yayasan;
	/** Keterangan bebas penjelas jenis penghargaan. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Label jenis penghargaan; satu-satunya kolom wajib. Lihat {@link #getNama()}. */
	private String nama;
	/**
	 * Bobot poin prestasi untuk jenis penghargaan ini. Lihat {@link #getPoin()} untuk perilaku
	 * nilai bawaan dan daftar pemakainya.
	 */
	private Double poin;
	/** Penanda baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk membuat instance saat menghidrasi
	 * baris dari basis data. Dipakai juga oleh layar "Tambah Penghargaan"
	 * ({@code PenghargaanAction.onAdd}) untuk menyiapkan formulir kosong.
	 */
	public Penghargaan() {
	}

	/**
	 * Konstruktor ringkas berisi kolom wajib saja ({@code id} + {@code nama}), warisan template
	 * hbm2java.
	 *
	 * <p><b>Catatan:</b> tidak ditemukan pemanggil di dalam basis kode — kolom {@code id}
	 * dipetakan {@code insertable=false} dan dibangkitkan basis data, sehingga menyetelnya manual
	 * di konstruktor tidak berpengaruh pada {@code INSERT}.</p>
	 *
	 * @param id   kunci utama yang sudah diketahui
	 * @param nama label jenis penghargaan
	 */
	public Penghargaan(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable=false} dan dibangkitkan basis data
	 * ({@code IDENTITY}), jadi nilainya baru terisi setelah baris benar-benar tersimpan.
	 * {@code PenghargaanAction} memakai {@code getId() == null} sebagai pembeda mode "Tambah" vs
	 * "Ubah" pada judul dialog.</p>
	 *
	 * <p>Karena anotasi {@code @Id} melekat pada getter ini, seluruh pemetaan entity memakai
	 * <i>property access</i> — inilah yang membuat efek samping pada {@link #getSekolah()} dan
	 * {@link #getYayasan()} bisa ikut terbawa ke pernyataan {@code UPDATE} saat flush.</p>
	 *
	 * @return kunci utama, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara manual.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini, setelah proxy lazy-nya diselesaikan.
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@code check(sekolah)} milik
	 * {@link ais.database.model.GeneralValueObject} yang menyelesaikan proxy Hibernate yang
	 * mungkin sudah <i>detached</i> (memuat ulang lewat identifier bila perlu), sehingga pemanggil
	 * menerima entity asli dan tidak meledak dengan {@code LazyInitializationException}. Hasilnya
	 * DITULIS BALIK ke field, jadi resolusi hanya terjadi sekali per instance — tetapi itu juga
	 * berarti getter ini bukan pembacaan murni.</p>
	 *
	 * <p><b>Dipanggil dari.</b> Renderer grid {@code PenghargaanAction.PenghargaanRenderer} (kolom
	 * "Sekolah"), dialog Tambah/Ubah ({@code Common.pilihSekolah}), dan secara tidak langsung oleh
	 * {@link #getYayasan()}.</p>
	 *
	 * <p><b>Catatan cakupan.</b> Tidak satu pun pembaca runtime yang ditemukan menyaring daftar
	 * master ini berdasarkan sekolah secara OTOMATIS: pada {@code PenghargaanAction.initCriteria}
	 * penyaring sekolah/yayasan sepenuhnya berasal dari pilihan pengguna dan jatuh ke
	 * {@code 1=1} bila dibiarkan kosong, sedangkan daftar checkbox butir pada
	 * {@code ApresiasiDanPenghargaanAction} hanya menyaring {@link #getAktif() aktif} tanpa
	 * batasan tenant sama sekali. Field ini karenanya berfungsi sebagai LABEL kepemilikan, bukan
	 * sebagai kontrol akses.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris ini.
	 *
	 * <p><b>Penjagaan penting:</b> objek {@code Sekolah} yang belum tersimpan (ber-{@code id}
	 * {@code null}) DINORMALISASI MENJADI {@code null}, bukan disimpan apa adanya. Ini mencegah
	 * {@code CascadeType.PERSIST} tanpa sengaja membuat baris {@code Sekolah} baru hanya karena
	 * combobox mengembalikan objek kosong. Efek sampingnya: pemanggil yang mengira sudah menyetel
	 * sekolah bisa mendapati nilainya hilang tanpa pesan kesalahan.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa {@code id} disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini — <b>getter dengan efek samping (destruktif)</b>.
	 *
	 * <p><b>Cara kerja.</b> Bukan pembacaan murni: method ini lebih dulu memanggil
	 * {@link #getSekolah()}, dan bila sekolah terisi maka field {@link #yayasan} DITIMPA dengan
	 * {@code sekolah.getYayasan()}. Barulah hasilnya dilewatkan {@code check(...)} untuk resolusi
	 * proxy lazy.</p>
	 *
	 * <p><b>Konsekuensi yang mudah terlewat.</b> Karena pemetaan memakai <i>property access</i>
	 * (lihat {@link #getId()}) dengan {@code dynamicUpdate=true}, Hibernate membaca state entity
	 * lewat getter saat flush. Bila baris ini kebetulan berada dalam sesi aktif dan yayasan
	 * tersimpan berbeda dari yayasan milik sekolahnya, sekadar MEMBACA baris sudah cukup memicu
	 * {@code UPDATE} nyata plus revisi Envers palsu. Nilai yayasan yang disetel manual lewat
	 * {@link #setYayasan(Yayasan)} praktis tidak pernah bertahan selama {@code sekolah} terisi —
	 * kolom {@code yayasan_id} efektif hanya turunan (denormalisasi) dari {@code sekolah_id}. Pola
	 * getter destruktif yang sama dijumpai berulang di banyak subclass
	 * {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * @return yayasan pemilik (turunan dari sekolah bila sekolah terisi), atau {@code null}
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
	 * Menyetel yayasan pemilik baris ini, dengan normalisasi "objek tanpa {@code id} menjadi
	 * {@code null}" yang sama seperti {@link #setSekolah(Sekolah)}.
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini akan DITIMPA lagi oleh
	 * {@link #getYayasan()} bila {@link #sekolah} terisi — lihat penjelasan pada getter-nya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa {@code id} disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas penjelas jenis penghargaan.
	 *
	 * <p>Berbeda dengan sebagian subclass {@link ais.database.model.GeneralValueObject} yang
	 * membalik kontrak {@code keterangan} milik induk, di sini kolom {@code keterangan} benar-benar
	 * dipetakan ke basis data dan dikembalikan apa adanya: ditampilkan sebagai kolom grid pada
	 * {@code PenghargaanAction}, ikut kolom ekspor/impor Excel, dan diisi lewat {@code Textbox}
	 * pada dialog Tambah/Ubah.</p>
	 *
	 * @return keterangan jenis penghargaan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas penjelas jenis penghargaan.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}, tanpa validasi panjang
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label jenis penghargaan.
	 *
	 * <p><b>Peran ganda.</b> Selain sebagai label tampilan (grid master, checkbox pilihan pada
	 * layar paket dan layar transaksi, kolom "penghargaans" pada rapor, serta daftar nama pada
	 * {@code LaporanApresiasiSiswa}), nilai ini juga menjadi KUNCI PENGURUTAN EFEKTIF entity ini:
	 * {@link ais.database.model.GeneralValueObject} mengurutkan dengan {@code nomorUrut} &rarr;
	 * {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}, dan dua kunci pertama selalu
	 * {@code null} di sini.</p>
	 *
	 * <p><b>Risiko penciutan senyap {@code TreeSet} (TERVERIFIKASI, bukan teoretis).</b> Karena
	 * {@code compareTo} mengembalikan {@code 0} untuk dua baris penghargaan ber-{@code nama} sama
	 * meski {@code id}-nya berbeda, dan tidak ada batasan {@code unique} pada kolom {@code nama},
	 * dua butir berbeda dengan nama identik akan MENCIUT menjadi satu baris di layar. Ini terjadi
	 * di dua tempat yang membungkus koleksi butir ke dalam {@code new TreeSet<Penghargaan>(...)}
	 * sebelum merendernya:</p>
	 * <ul>
	 * <li>{@code ApresiasiDanPenghargaanAction} — kolom ringkasan isi paket pada grid master
	 * paket.</li>
	 * <li>{@code ApresiasiSiswaAction} — kolom ringkasan penghargaan yang diterima pada grid
	 * transaksi siswa.</li>
	 * </ul>
	 * <p>Perbedaan penting dari sekadar "tampilan kurang rapi": penjumlahan bobot di
	 * {@code LaporanApresiasiSiswa} mengiterasi koleksi ASLI ({@code HashSet} terkelola), bukan
	 * {@code TreeSet} tersebut. Jadi total poin pada laporan menghitung KEDUA butir sementara
	 * layar hanya menampilkan SATU — selisih yang tampak seperti salah hitung padahal berasal dari
	 * penciutan tampilan. Ini varian pola yang sama seperti yang berulang kali ditemukan di
	 * keluarga {@code KelompokParameterTambahan*} dan {@code JenisCatatanGuru}.</p>
	 *
	 * @return label jenis penghargaan; secara skema {@code NOT NULL}, tetapi baris warisan
	 *         teoretis bisa mengembalikan {@code null} sehingga pemanggil tetap perlu berjaga-jaga
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel label jenis penghargaan.
	 *
	 * <p>Validasi "harus diisi" dilakukan di lapis UI ({@code PenghargaanAction.onSave}), bukan di
	 * sini; setter ini menerima nilai apa pun termasuk {@code null} dan string kosong. Perlu
	 * diingat bahwa mengubah nama berdampak RETROAKTIF ke seluruh catatan
	 * {@link ais.database.model.sekolah.ApresiasiSiswa} yang merujuk baris ini, karena catatan itu
	 * menyimpan FK dan bukan salinan teks.</p>
	 *
	 * @param nama label jenis penghargaan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan penanda apakah jenis penghargaan ini masih aktif dipakai, dengan bawaan
	 * {@code true}.
	 *
	 * <p><b>Perilaku bawaan.</b> Bila field masih {@code null} (baris lama sebelum kolom ini ada,
	 * atau baris hasil impor Excel yang kolomnya kosong), method mengembalikan {@code true} — jadi
	 * "belum diisi" diperlakukan sebagai "aktif". Nilai {@code null} itu sendiri TIDAK ditulis
	 * balik ke field, sehingga getter ini aman dari efek samping <i>write-back</i> dan tidak
	 * memicu {@code UPDATE} tak terduga.</p>
	 *
	 * <p><b>Penanda ini BENAR-BENAR menyaring — berbeda dari padanannya di {@code Hukuman}.</b>
	 * {@code ApresiasiDanPenghargaanAction} membangun daftar checkbox butir penghargaan dengan
	 * {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))},
	 * sehingga butir yang dinonaktifkan benar-benar hilang dari pilihan saat menyusun paket baru.
	 * Perhatikan bahwa bentuk {@code isNull OR true} itulah yang membuat baris warisan
	 * ber-{@code NULL} tetap tampil, selaras dengan bawaan getter ini.</p>
	 *
	 * <p><b>Batas penyaringannya.</b> Penonaktifan TIDAK bersifat surut: dialog transaksi
	 * {@code ApresiasiSiswaAction.loadPenghargaan(...)} tidak mengambil dari basis data melainkan
	 * mengiterasi {@code apresiasiDanPenghargaan.getPenghargaans()} — isi paket yang sudah
	 * terlanjur tersimpan — TANPA memeriksa kolom ini. Jadi butir yang dinonaktifkan tetap muncul
	 * sebagai pilihan yang dapat dicentang pada setiap paket yang masih memuatnya, dan tetap ikut
	 * terjumlah di laporan. Menonaktifkan sebuah butir hanya mencegahnya MASUK paket baru, bukan
	 * mengeluarkannya dari paket lama. Layar master {@code PenghargaanAction} juga tidak menyaring
	 * kolom ini, sehingga baris nonaktif tetap terlihat dan tetap dapat diedit di sana.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} hanya bila eksplisit
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif/tidaknya jenis penghargaan ini.
	 *
	 * <p><b>Jalur pemanggilan yang perlu disadari.</b> Selain dari dialog Tambah/Ubah, setter ini
	 * dipanggil langsung dari listener {@code onCheck} checkbox "Aktif" pada grid master
	 * {@code PenghargaanAction}, yang segera menyimpan perubahannya lewat
	 * {@code Common.refreshSaveOrUpdate}. Artinya satu klik centang di daftar sudah merupakan
	 * penulisan basis data penuh — memicu {@link #onUpdate()}, menimpa jejak audit
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah}, dan membuat satu revisi Envers baru.</p>
	 *
	 * @param aktif {@code true} aktif, {@code false} nonaktif, {@code null} diperlakukan sebagai
	 *              aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan bobot poin prestasi untuk jenis penghargaan ini, dengan bawaan {@code 0.0}.
	 *
	 * <p><b>Perilaku bawaan.</b> Field {@code null} dikembalikan sebagai {@code 0.0} tanpa ditulis
	 * balik ke field — sehingga pemanggil aman melakukan aritmetika langsung
	 * ({@code point += penghargaan.getPoin()}) tanpa memeriksa {@code null}, dan getter ini tidak
	 * memicu {@code UPDATE} tak terduga. Seluruh pembaca yang ditemukan memang mengandalkan sifat
	 * ini dan tidak melakukan pemeriksaan {@code null} sendiri.</p>
	 *
	 * <p><b>Dipanggil dari (semua konsumen nyata bobot ini).</b></p>
	 * <ul>
	 * <li>{@code PenghargaanAction} — kolom "Poin" pada grid master (diformat lewat
	 * {@code Common.numberFormat}) dan isian {@code MyDoublebox} pada dialog Tambah/Ubah. Juga
	 * ikut dalam kolom ekspor/impor Excel.</li>
	 * <li>{@code ApresiasiSiswaAction.loadPenghargaan(...)} — label checkbox pilihan penghargaan
	 * ditambahi keterangan poin hanya bila nilainya {@code > 0.1}. Ambang ini sekaligus berfungsi
	 * sebagai penjagaan terhadap galat pembulatan {@code double}, tetapi juga berarti penghargaan
	 * berbobot kecil (mis. 0,05) tidak pernah menampilkan poinnya di layar meski tetap dijumlahkan
	 * di laporan. <b>Cacat label:</b> teks yang ditempelkan berbunyi "pengurangan poin : ..." —
	 * salin-tempel mentah dari {@code PelanggaranSiswaAction} tempat pengurangan memang tepat. Di
	 * konteks apresiasi arahnya TERBALIK: ini bobot prestasi yang ditambahkan, bukan dikurangkan.
	 * Murni kesalahan teks, tidak ada perhitungan yang terpengaruh.</li>
	 * <li>{@code LaporanApresiasiSiswa} — dua tempat kembar (jalur rekap periode dan jalur cetak
	 * satu nota) menjumlahkan bobot seluruh anggota {@code apresiasiSiswa.getPenghargaans()} ke
	 * parameter laporan {@code "point"}, berdampingan dengan {@code "kredit"} dari
	 * {@code Apresiasi.getKredit()}. <b>Jalur ini benar.</b></li>
	 * <li>{@code LaporanRaporSiswa.masukkanPoin(...)} — bobot per butir ({@code "poin_" + id}) dan
	 * subtotal per kejadian ({@code "poin"}) BENAR, tetapi parameter TOTAL
	 * {@code "totalPointPenghargaan_" + siswa.getId()} selalu {@code 0.0}. Rinciannya dijelaskan
	 * pada Javadoc kelas, bagian "Cacat akumulator total poin di rapor siswa".</li>
	 * </ul>
	 *
	 * @return bobot poin prestasi; {@code 0.0} bila belum pernah diisi
	 */
	public Double getPoin() {
		return poin == null ? 0.0 : poin;
	}

	/**
	 * Menyetel bobot poin prestasi untuk jenis penghargaan ini.
	 *
	 * <p>Tidak ada validasi rentang: nilai negatif maupun sangat besar diterima apa adanya dan
	 * akan ikut terjumlah di {@code LaporanApresiasiSiswa}. Nilai diambil langsung dari
	 * {@code MyDoublebox} pada {@code PenghargaanAction.onSave} tanpa pemeriksaan, sehingga isian
	 * kosong akan tersimpan sebagai {@code null} dan dibaca kembali sebagai {@code 0.0} oleh
	 * {@link #getPoin()}. Perlu diingat bahwa perubahan bobot berdampak RETROAKTIF ke seluruh
	 * laporan atas catatan siswa yang sudah merujuk baris ini.</p>
	 *
	 * @param poin bobot poin prestasi; {@code null} diperlakukan sebagai {@code 0.0} oleh
	 *             {@link #getPoin()}
	 */
	public void setPoin(Double poin) {
		this.poin = poin;
	}

}
