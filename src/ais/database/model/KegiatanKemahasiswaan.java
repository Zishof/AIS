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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;

/**
 * Entity <b>kegiatan kemahasiswaan</b> &mdash; tabel {@code public.kegiatan_kemahasiswaan}.
 *
 * <p>Satu baris = satu <b>wadah kegiatan/organisasi mahasiswa</b> yang terdaftar di kampus:
 * kepanitiaan, seminar, UKM, lomba internal, kegiatan pengabdian, dan sejenisnya. Yang disimpan di
 * sini adalah <i>identitas kegiatannya</i> (nama, aspek, skala, jabatan, pembina, rentang tanggal,
 * SK, sertifikat), <b>bukan</b> daftar pesertanya. Karena itu class ini berperan sebagai
 * <b>master/katalog kegiatan</b>, bukan tabel transaksi.</p>
 *
 * <p><b>Nama class ini TIDAK menyesatkan</b> (berbeda dari beberapa entity tetangga di paket ini,
 * mis. {@link PenghargaanMahasiswa} yang modul UI-nya bernama "Karya"). Layar yang mengelolanya
 * memang bernama sama: entri menu {@code NewUiLayarLainnyaController} &rarr; tab Kemahasiswaan
 * berlabel <b>"Kegiatan Mahasiswa"</b> menuju {@code /pages/master/kegiatan_kemahasiswaan.zul},
 * judul jendela tambah/ubah {@code "Tambah/Ubah Kegiatan Kemahasiswaan"}, dan action pengelolanya
 * {@code ais.action.master.KegiatanKemahasiswaanAction}.</p>
 *
 * <h2>Siapa yang menunjuk entity ini</h2>
 *
 * <p>Hanya <b>dua</b> entity di seluruh {@code ais.database.model} yang punya properti bertipe
 * {@code KegiatanKemahasiswaan}, keduanya lewat kolom FK bernama sama
 * ({@code kegiatan_kemahasiswaan}):</p>
 *
 * <ul>
 *   <li><b>{@link KegiatanKemahasiswaanPunyaMahasiswa}</b> ({@code nullable = false}) &mdash;
 *       tabel <b>kepesertaan</b>. Di sanalah tersimpan pasangan (kegiatan, mahasiswa) beserta
 *       {@code jabatanKegiatanKemahasiswaan}, {@code skalaKegiatanKemahasiswaan}, {@code mulai},
 *       {@code sampai}, dan {@code persetujuan} <i>per peserta</i>. Ini penting untuk memahami
 *       class ini: jabatan/skala yang ada di sini adalah nilai <b>tingkat kegiatan</b>, dan bisa
 *       berbeda dari jabatan/skala yang dicatat untuk tiap peserta.</li>
 *   <li><b>{@link FormulirKegiatan}</b> ({@code nullable = true}) &mdash; formulir/kepanitiaan
 *       yang secara opsional menempel pada sebuah kegiatan.</li>
 * </ul>
 *
 * <p>Perhatikan bahwa {@code NilaiKegiatanKemahasiswaan} &mdash; meski namanya mirip &mdash;
 * <b>tidak</b> menunjuk entity ini sama sekali; ia adalah tabel rubrik/bobot yang menautkan
 * {@link DetailKelompokKegiatanKemahasiswaan} &times; {@link JabatanKegiatanKemahasiswaan}
 * &times; {@link SkalaKegiatanKemahasiswaan} ke sebuah nilai.</p>
 *
 * <h2>Alur persetujuan</h2>
 *
 * <p>{@link #getStatus()} adalah simpul alur persetujuan bertingkat, dan status {@link #DISETUJUI}
 * membuka tiga hal di hilir (diverifikasi di action/helper terkait): (1) kegiatan baru muncul
 * sebagai pilihan bagi mahasiswa lain; (2) tombol pengiriman ke Neo Feeder baru tampil;
 * (3) kegiatan baru bisa dipilih dari layar Formulir Kegiatan. Persetujuan tingkat kedua ada di
 * {@link KegiatanKemahasiswaanPunyaMahasiswa#getPersetujuan()} (per peserta), yang mengunci baris
 * peserta dan memunculkan tombol cetak sertifikat.</p>
 *
 * <p>Combobox pengubah status hanya dirender untuk pengguna <b>non-mahasiswa</b>; mahasiswa
 * melihatnya sebagai label, dan pejabat fakultas/prodi dibuat baca-saja untuk kegiatan di luar
 * unitnya. Perubahan status pada combobox itu langsung disimpan ({@code Common.refreshUpdate})
 * tanpa tombol simpan terpisah.</p>
 *
 * <h2>PERINGATAN NAMA: tidak ada hubungannya dengan {@link Kegiatan}/{@link DetailKegiatan}</h2>
 *
 * <p>Ini <b>bukan</b> spesialisasi, subclass, sub-tabel, atau varian dari {@link Kegiatan}. Kedua
 * nama itu memakai kata "kegiatan" untuk dua domain yang sama sekali berbeda. Bukti konkret dari
 * kode, bukan dari nama:</p>
 *
 * <ul>
 *   <li><b>Tabel berbeda dan tidak berelasi.</b> Class ini {@code @Table(name =
 *       "kegiatan_kemahasiswaan")}; {@link Kegiatan} {@code @Table(name = "kegiatan")};
 *       {@link DetailKegiatan} {@code @Table(name = "detail_kegiatan")}. Tidak ada satu pun
 *       {@code @JoinColumn} di sini yang menunjuk {@code kegiatan}/{@code detail_kegiatan}, dan
 *       {@link Kegiatan}/{@link DetailKegiatan} tidak punya properti bertipe class ini.</li>
 *   <li><b>Domain berbeda: keuangan vs kemahasiswaan.</b> {@link Kegiatan} adalah wadah
 *       <b>tagihan</b> milik seorang mahasiswa untuk satu semester + satu {@link JenisKegiatan}
 *       (registrasi, daftar ulang, wisuda). Field-nya berbicara uang: {@code amount},
 *       {@code denda}, {@code pengurangan}, {@code lunas}, {@code amountTerhutang},
 *       {@code tanggalBayarAwal}, {@code jadwalPembayaran}. {@link DetailKegiatan} adalah
 *       <b>baris tagihan</b> di dalam wadah itu ({@code biaya}, {@code diskon},
 *       {@code detailBiaya}, {@code itemBiaya}, {@code postingHistory}). Class <i>ini</i> tidak
 *       punya satu pun properti nominal, denda, cicilan, atau posting jurnal.</li>
 *   <li><b>Master pendukung berbeda.</b> {@link Kegiatan} bergantung pada {@link JenisKegiatan}
 *       (tabel {@code jenis_kegiatan}); class ini bergantung pada
 *       {@link KelompokKegiatanKemahasiswaan}, {@link DetailKelompokKegiatanKemahasiswaan},
 *       {@link JabatanKegiatanKemahasiswaan}, {@link SkalaKegiatanKemahasiswaan}, dan
 *       {@link JenisAktfitasMahasiswa} &mdash; himpunan master yang tidak dipakai sama sekali oleh
 *       rantai billing.</li>
 *   <li><b>Ukuran/bentuk berbeda jauh.</b> Class ini 411 baris/31 properti tanpa satu pun method
 *       query statis; {@link Kegiatan} 2.125 baris dan {@link DetailKegiatan} 2.096 baris, penuh
 *       method bisnis penghitungan tagihan dan akses {@code HibernateUtil}/{@code Session}.</li>
 * </ul>
 *
 * <p>Kerabat sebenarnya dari class ini adalah <b>{@link KegiatanKedosenan}</b> (tabel
 * {@code kegiatan_kedosenan}) dan {@code ais.database.model.sekolah.KegiatanKesiswaan} &mdash;
 * tiga varian dengan bentuk hampir identik untuk tiga populasi berbeda (mahasiswa / dosen /
 * siswa). Bedanya yang paling terlihat: di sini {@code diajukanOleh} bertipe {@link Mahasiswa},
 * di {@link KegiatanKedosenan} bertipe {@link Dosen}; class ini punya tambahan
 * {@link #getDosenPembina1() dua dosen pembina}, {@link #getJenisAktfitasMahasiswa()} (jembatan
 * Neo Feeder/PDDIKTI), {@link #getFeeder()}, {@link #getNoSk()}/{@link #getTglSk()}, dan
 * {@link #getTempat()}, yang tidak ada di sisi kedosenan.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Kontrak audit warisan</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 *       {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Identitas kegiatan</b> &mdash; {@link #getKode()}, {@link #getNama()},
 *       {@link #getNamaEn()}, {@link #getKeterangan()}, {@link #getTempat()},
 *       {@link #getUrl()}.</li>
 *   <li><b>Klasifikasi (relasi master)</b> &mdash; {@link #getKelompokKegiatanKemahasiswaan()},
 *       {@link #getDetailKelompokKegiatanKemahasiswaan()},
 *       {@link #getJabatanKegiatanKemahasiswaan()}, {@link #getSkalaKegiatanKemahasiswaan()},
 *       {@link #getJenisAktfitasMahasiswa()}.</li>
 *   <li><b>Periode</b> &mdash; {@link #getMulai()}, {@link #getSampai()},
 *       {@link #getTahunAkademik()}, {@link #getJenisSemester()}, {@link #getTahun()}.</li>
 *   <li><b>Pengusul &amp; unit</b> &mdash; {@link #getDiajukanOleh()}, {@link #getJurusan()},
 *       {@link #getFakultas()}, {@link #getDosenPembina1()}, {@link #getDosenPembina2()}.</li>
 *   <li><b>Legalitas &amp; keluaran</b> &mdash; {@link #getNoSk()}, {@link #getTglSk()},
 *       {@link #getSertifikat()}.</li>
 *   <li><b>Kendali tampil &amp; integrasi</b> &mdash; {@link #getStatus()},
 *       {@link #getBolehDipilih()}, {@link #getFeeder()}.</li>
 * </ol>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 *
 * <ul>
 *   <li><b>Akses properti, bukan field.</b> {@code @Id} dipasang pada {@link #getId()}, sehingga
 *       Hibernate memakai <i>property access</i>: <b>getter di bawah ini benar-benar dipanggil
 *       Hibernate</b> saat insert, update, dan dirty-checking. Logika apa pun di dalam getter
 *       ikut menentukan isi baris di database &mdash; ini sumber semua kejutan di daftar
 *       berikutnya.</li>
 *   <li><b>Lima getter menulis balik ke field terpetakan.</b> {@link #getKode()},
 *       {@link #getTahun()}, {@link #getTahunAkademik()}, {@link #getJenisSemester()}, dan
 *       (secara tidak langsung, lewat {@code check()}) semua getter relasi. Dengan
 *       {@code dynamicUpdate = true}, sekadar <i>membaca</i> object managed bisa menghasilkan
 *       {@code UPDATE} tanpa aksi simpan dari pengguna. Rinciannya ada di Javadoc masing-masing
 *       getter.</li>
 *   <li><b>{@link #getJenisAktfitasMahasiswa()} mengembalikan sesuatu yang tidak ada di
 *       field.</b> Bila field null, getter mengembalikan konstanta global
 *       {@code ConstantValues.KEGIATAN_KEMAHASISWAAN}. Karena Hibernate membaca lewat getter,
 *       nilai default itu bisa ikut tersimpan sebagai FK di baris ini walau pengguna tidak pernah
 *       memilihnya.</li>
 *   <li><b>{@code nama} unik secara global.</b> {@code @Column(name = "nama", nullable = false,
 *       length = 255, unique = true)} berlaku untuk seluruh tabel &mdash; tidak dibatasi per
 *       tahun akademik, per fakultas, atau per kelompok. Akibatnya nama kegiatan berulang tahunan
 *       ("Seminar Nasional", "Ospek") hanya boleh muncul <b>sekali</b> di seluruh riwayat, dan
 *       pemotongan 255 karakter di {@link #setNama(String)} bisa membuat dua nama panjang yang
 *       berbeda bertabrakan pada constraint unik itu.</li>
 *   <li><b>{@code GeneralValueObject} bukan {@code @MappedSuperclass}.</b> Induknya
 *       ({@link ais.database.model.GeneralValueObject}) adalah POJO abstrak biasa tanpa anotasi
 *       JPA sama sekali, jadi Hibernate tidak memetakan properti induk. Deklarasi ulang
 *       {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di class ini
 *       <b>bukan duplikasi ceroboh</b>, melainkan keharusan teknis; menghapusnya akan menghapus
 *       kolom-kolom itu dari pemetaan. Yang diwarisi dari induk adalah <i>perilaku</i>-nya, di
 *       atas semua {@code check(...)}.</li>
 *   <li><b>Nama kolom FK dosen pembina salah eja.</b> {@code @JoinColumn(name = "dosen_pmbina1")}
 *       dan {@code "dosen_pmbina2"} &mdash; "pmbina", bukan "pembina". Salah eja ini ada di
 *       skema database, jadi <b>jangan "dirapikan"</b> tanpa migrasi DDL.</li>
 *   <li><b>Penamaan kolom default.</b> Properti tanpa {@code @Column} ({@code mulai},
 *       {@code sampai}, {@code tempat}, {@code status}, {@code kode}, {@code tahunAkademik},
 *       {@code jenisSemester}, {@code tahun}, {@code bolehDipilih}, {@code noSk},
 *       {@code tglSk}) memakai {@code ais.database.hibernate.MyNamingStrategy}, turunan
 *       {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya (PostgreSQL
 *       melipatnya jadi huruf kecil). Itulah sebabnya {@code namaEn} perlu {@code @Column(name =
 *       "namaen")} eksplisit.</li>
 *   <li><b>{@code @Audited}.</b> Hibernate Envers merekam setiap revisi ke tabel bayangan
 *       {@code kegiatan_kemahasiswaan_AUD}. Perubahan yang lahir dari efek samping getter di atas
 *       juga ikut terekam sebagai revisi &mdash; berguna untuk forensik, tapi juga berarti tabel
 *       audit bisa membengkak oleh perubahan yang tidak pernah diminta pengguna.</li>
 *   <li><b>Tidak ada akses database langsung di class ini.</b> Tidak ada {@code import}
 *       {@code Session}, {@code HibernateUtil}, {@code Criteria}, atau {@code Restrictions}, dan
 *       tidak ada satu pun method query statis. Semua sentuhan ke database terjadi <i>secara
 *       tidak langsung</i> lewat {@code check(...)} milik induk (yang bisa membuka dan menutup
 *       session sendiri) dan lewat {@link Common} (cache tahun akademik).</li>
 *   <li><b>{@code serialVersionUID} kembar.</b> Nilai {@code 2463821577548439808L} dipakai
 *       bersama oleh 315 class di paket ini akibat salin-tempel. Tidak berbahaya (nilai ini
 *       dievaluasi per class), tapi jangan dijadikan petunjuk kekerabatan antar-entity.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see KegiatanKedosenan
 * @see FormulirKegiatan
 * @see KelompokKegiatanKemahasiswaan
 * @see DetailKelompokKegiatanKemahasiswaan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kegiatan_kemahasiswaan")

public class KegiatanKemahasiswaan extends GeneralValueObject {

	/**
	 * Status awal sebuah usulan kegiatan: sudah tercatat, belum disentuh verifikator.
	 *
	 * <p>Ini sekaligus nilai <b>default saat dibaca</b>: {@link #getStatus()} mengembalikan
	 * konstanta ini bila kolom {@code status} masih {@code null} (baris lama yang dibuat sebelum
	 * alur persetujuan ada). Perhatikan bahwa nilai yang disimpan adalah <b>teks bahasa
	 * Indonesia apa adanya</b>, bukan kode/enum, sehingga perbandingan status di seluruh aplikasi
	 * bersifat string-sensitif &mdash; mengubah ejaan konstanta ini akan memutus pencocokan
	 * terhadap baris-baris lama di database.</p>
	 */
	public static final String BELUM_DIPROSES = "Belum diproses";

	/**
	 * Status antara: usulan kegiatan sedang ditelaah/diverifikasi.
	 *
	 * @see #BELUM_DIPROSES catatan tentang penyimpanan status sebagai teks
	 */
	public static final String SEDANG_DIPROSES = "Sedang diproses";

	/**
	 * Status akhir positif: kegiatan disetujui dan boleh dijalankan/diakui.
	 *
	 * @see #BELUM_DIPROSES catatan tentang penyimpanan status sebagai teks
	 */
	public static final String DISETUJUI = "Disetujui";

	/**
	 * Status akhir negatif: usulan kegiatan ditolak.
	 *
	 * @see #BELUM_DIPROSES catatan tentang penyimpanan status sebagai teks
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, kolom {@code id} (IDENTITY/serial PostgreSQL). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama tampil pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh form.
	 */
	private String oleh;

	/**
	 * Identitas (login/NIP/NIM) pengguna yang terakhir mengubah baris ini. Pasangan teknis dari
	 * {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir, dengan <b>penjagaan anti-timpa</b>.
	 *
	 * <p>Nilai {@code null} atau string kosong/whitespace <b>diabaikan diam-diam</b> (method
	 * langsung {@code return} tanpa menyentuh field). Ini disengaja: jejak audit yang sudah ada
	 * tidak boleh terhapus oleh proses yang kebetulan memanggil setter dengan nilai kosong
	 * &mdash; misalnya saat entity di-{@code merge} dari object hasil binding form yang tidak
	 * memuat kolom audit. Konsekuensinya, <b>setter ini tidak bisa dipakai untuk mengosongkan
	 * kolom</b>.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini di-{@code
	 * UPDATE}, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan
	 * {@code @PrePersist}: pada baris baru, stempel waktu berasal dari inisialisasi field
	 * {@link #tanggal_dirubah} ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor
	 * berjalan.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan
	 * entity paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu
	 * konflik di banyak sesi paralel.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya <b>tidak dipanggil dari form</b>; pengisian normal dilakukan
	 * {@link #onUpdate()}. Pemanggilan manual berguna hanya pada migrasi/impor data yang ingin
	 * mempertahankan stempel waktu asal.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (presisi
	 * {@link TemporalType#TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya diinisialisasi {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen ZK (Combobox/Listbox) sebagai label default ketika object ini
	 * ditampilkan tanpa renderer khusus, dan muncul juga di log.</p>
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field</b> {@link #nama} secara langsung, bukan
	 * lewat {@link #getNama()}, sehingga hasilnya <b>tidak di-{@code trim}</b> dan bisa berbeda
	 * dari yang ditampilkan getter. Pada baris baru yang belum di-{@code save}, {@code id} masih
	 * {@code null} sehingga hasilnya berbentuk {@code "null-..."}.</p>
	 *
	 * @return gabungan id dan nama kegiatan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode ringkas kegiatan (5 digit). Dihitung otomatis dari {@link #id} bila kosong &mdash;
	 * lihat {@link #getKode()}.
	 */
	private String kode;

	/**
	 * Nama kegiatan. Kolom {@code nama} bersifat <b>UNIK secara global</b> dan
	 * {@code nullable = false}, maksimal 255 karakter.
	 */
	private String nama;

	/** Nama kegiatan dalam bahasa Inggris (kolom {@code namaen}, tipe {@code text}). */
	private String namaEn;

	/** Lokasi penyelenggaraan kegiatan (teks bebas, bukan relasi ke master ruang/gedung). */
	private String tempat;

	/** Keterangan/deskripsi bebas, kolom bertipe {@code text} (tanpa batas panjang praktis). */
	private String keterangan;

	/** Kelompok kegiatan (master tingkat 1), wajib. Lihat {@link #getKelompokKegiatanKemahasiswaan()}. */
	private KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan;

	/** Rincian kelompok kegiatan (master tingkat 2), wajib. Lihat {@link #getDetailKelompokKegiatanKemahasiswaan()}. */
	private DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan;

	/** Jabatan yang diampu dalam kegiatan (Ketua, Sekretaris, Anggota, ...). Opsional. */
	private JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan;

	/** Skala/cakupan kegiatan (lokal, wilayah, nasional, internasional, ...). Opsional. */
	private SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan;

	/** Tanggal mulai kegiatan (presisi hari, {@link TemporalType#DATE}). */
	private Date mulai;

	/** Tanggal selesai kegiatan (presisi hari, {@link TemporalType#DATE}). */
	private Date sampai;

	/**
	 * Status alur persetujuan usulan kegiatan. Nilainya salah satu dari {@link #BELUM_DIPROSES},
	 * {@link #SEDANG_DIPROSES}, {@link #DISETUJUI}, atau {@link #DITOLAK} &mdash; disimpan sebagai
	 * teks, tanpa constraint di sisi database.
	 */
	private String status;

	/** Dosen pembina utama. Dipetakan ke kolom salah eja {@code dosen_pmbina1}. */
	private Dosen dosenPembina1;

	/** Dosen pembina pendamping. Dipetakan ke kolom salah eja {@code dosen_pmbina2}. */
	private Dosen dosenPembina2;

	/** Mahasiswa pengusul kegiatan. Opsional: kegiatan yang dibuat operator bisa tanpa pengusul. */
	private Mahasiswa diajukanOleh;

	/** Program studi/jurusan penyelenggara. Opsional (kegiatan tingkat universitas bisa kosong). */
	private Jurusan jurusan;

	/** Fakultas penyelenggara. Opsional, dengan alasan yang sama seperti {@link #jurusan}. */
	private Fakultas fakultas;

	/** Tautan (URL) dokumentasi/publikasi kegiatan, kolom bertipe {@code text}. */
	private String url;

	/**
	 * Jenis semester penyelenggaraan ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}).
	 * Terisi otomatis dari periode berjalan bila dibiarkan kosong &mdash; lihat
	 * {@link #getJenisSemester()}.
	 */
	private String jenisSemester;

	/**
	 * Tahun akademik penyelenggaraan, format {@code "2025/2026"}. Terisi otomatis dari periode
	 * berjalan bila dibiarkan kosong &mdash; lihat {@link #getTahunAkademik()}.
	 */
	private String tahunAkademik;

	/**
	 * Tahun (angka) penyelenggaraan. <b>Bukan field mandiri</b> dalam praktiknya: selalu ditimpa
	 * dari potongan pertama {@link #tahunAkademik} setiap kali {@link #getTahun()} dipanggil.
	 */
	private Integer tahun;

	/**
	 * Template sertifikat yang dipakai untuk kegiatan ini. Opsional; dipakai saat mencetak
	 * sertifikat peserta.
	 */
	private Sertifikat sertifikat;

	/**
	 * Penanda apakah kegiatan ini masih boleh dipilih di form/dropdown. Bernilai {@code true}
	 * bila {@code null} &mdash; lihat {@link #getBolehDipilih()}.
	 */
	private Boolean bolehDipilih;

	/**
	 * Jenis aktivitas mahasiswa menurut referensi Neo Feeder/PDDIKTI
	 * ({@code id_jns_akt_mhs}). Bila kosong, {@link #getJenisAktfitasMahasiswa()} memakai
	 * konstanta global {@code ConstantValues.KEGIATAN_KEMAHASISWAAN}.
	 */
	private JenisAktfitasMahasiswa jenisAktfitasMahasiswa;

	/**
	 * Identitas/penanda baris ini di Neo Feeder (PDDIKTI), kolom bertipe {@code text}. Disimpan
	 * sebagai {@link String}, berbeda dari {@code JenisAktfitasMahasiswa#getFeeder()} yang
	 * bertipe {@link Long}.
	 */
	private String feeder;

	/** Tanggal Surat Keputusan yang melandasi kegiatan ({@link TemporalType#DATE}). */
	private Date tglSk;

	/** Nomor Surat Keputusan yang melandasi kegiatan. */
	private String noSk;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi entity.
	 *
	 * <p>Tidak menetapkan nilai apa pun secara eksplisit; satu-satunya inisialisasi yang benar-benar
	 * terjadi di sini adalah inisialisasi field {@link #tanggal_dirubah} =
	 * {@code WaktuUtil.getDate()}. Nilai bawaan untuk {@code status}, {@code bolehDipilih},
	 * {@code tahunAkademik}, {@code jenisSemester}, dan {@code jenisAktfitasMahasiswa}
	 * <b>tidak</b> diisi di sini, melainkan dihitung belakangan oleh getter masing-masing.</p>
	 */
	public KegiatanKemahasiswaan() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} memakai strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * (sequence/serial PostgreSQL) dan ditandai {@code insertable = false}, sehingga nilainya
	 * <b>tidak pernah dikirim</b> pada {@code INSERT} &mdash; database yang menentukannya, lalu
	 * Hibernate membacanya kembali. Karena {@code @Id} dipasang pada getter ini, seluruh entity
	 * memakai <i>property access</i>.</p>
	 *
	 * @return id baris; {@code null} selama object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama secara manual.
	 *
	 * <p>Praktis hanya dipakai oleh kode yang membangun object rujukan ringan (mis. untuk
	 * {@code Restrictions.eq("...", obj)}) atau oleh proses impor. Menetapkan id pada object baru
	 * lalu menyimpannya tidak akan memaksa id itu terpakai: strategi {@code IDENTITY} tetap
	 * meminta nilai dari database.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kegiatan, sudah di-{@code trim}.
	 *
	 * <p><b>Pemangkasan terjadi saat baca, bukan saat simpan.</b> Field disimpan apa adanya oleh
	 * {@link #setNama(String)}; getter inilah yang membuang spasi tepi. Karena Hibernate memakai
	 * property access, nilai <i>yang sudah di-trim</i> itulah yang ikut dibandingkan saat
	 * dirty-checking &mdash; jadi baris lama yang di database punya spasi tepi akan terlihat
	 * "berubah" dan bisa memicu {@code UPDATE} pada flush pertama, walau tidak ada yang
	 * menyuntingnya.</p>
	 *
	 * <p>Kolom {@code nama} <b>unik secara global</b> ({@code unique = true}) dan wajib
	 * ({@code nullable = false}); lihat catatan di Javadoc class tentang konsekuensinya untuk
	 * kegiatan yang berulang tiap tahun.</p>
	 *
	 * <p>Di layar berlabel <b>"Nama Kegiatan *"</b> dan menjadi salah satu kolom penyaring di
	 * daftar; nilainya dikirim ke Neo Feeder sebagai {@code judul}.</p>
	 *
	 * @return nama kegiatan tanpa spasi tepi, atau {@code null} bila field kosong
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama kegiatan, <b>memotong paksa</b> ke 255 karakter pertama bila lebih panjang.
	 *
	 * <p>Pemotongan ini adalah perbaikan lapangan (lihat komentar {@code KE-FIX} di dalam method):
	 * kolom database {@code varchar(255)}, sedangkan {@code Textbox} ZK di form tidak membatasi
	 * panjang masukan, sehingga simpan gagal dengan {@code DataException "value too long for type
	 * character varying(255)"}. Pemotongan dipilih supaya penyimpanan tetap berjalan tanpa
	 * mengubah skema.</p>
	 *
	 * <p><b>Efek samping yang perlu disadari:</b> pemotongan berlangsung <i>diam-diam</i> &mdash;
	 * pengguna tidak diberi tahu bahwa namanya dipendekkan. Dipadukan dengan constraint
	 * {@code unique} pada kolom yang sama, dua nama panjang berbeda yang 255 karakter pertamanya
	 * sama akan berakhir sebagai pelanggaran keunikan, bukan sebagai dua baris.</p>
	 *
	 * @param nama nama kegiatan; dipotong ke 255 karakter bila lebih panjang. Nilai {@code null}
	 *             diteruskan apa adanya (dan akan ditolak database karena kolom
	 *             {@code nullable = false})
	 */
	public void setNama(String nama) {
		// KE-FIX (DataException "value too long for type character varying(255)"): kolom DB
		// nama varchar(255), tapi field UI Textbox bebas tanpa batas panjang. Potong aman di
		// sini (bukan mengubah skema) supaya simpan tetap jalan.
		if (nama != null && nama.length() > 255) {
			nama = nama.substring(0, 255);
		}
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi kegiatan apa adanya (tanpa {@code trim}).
	 *
	 * <p>Kolom dipetakan {@code columnDefinition = "text"} sehingga tidak ada batas panjang
	 * praktis &mdash; berbeda dari {@link #getNama()} yang dibatasi 255 karakter.</p>
	 *
	 * @return keterangan kegiatan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/deskripsi kegiatan.
	 *
	 * @param keterangan teks bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kelompok kegiatan (master tingkat 1) setelah <b>meresolusi proxy lazy</b>.
	 *
	 * <p>Mengikuti pola relasi standar paket ini: hasil {@code check(...)} <b>ditugaskan kembali
	 * ke field</b> sebelum dikembalikan, sehingga proxy yang mati (session sudah tertutup)
	 * diganti object hidup dan pemanggilan berikutnya tidak perlu bekerja lagi. Efek sampingnya:
	 * getter ini <b>menulis ke field terpetakan</b>, dan bila object ini {@code managed},
	 * pergantian referensi itu bisa terlihat sebagai perubahan pada dirty-checking. {@code
	 * check(...)} sendiri dapat membuka dan menutup session Hibernate-nya sendiri sebagai upaya
	 * penyelamat terakhir.</p>
	 *
	 * <p>Relasi ini {@code nullable = false} di sisi database: setiap kegiatan wajib punya
	 * kelompok. Pasangannya, {@link #getDetailKelompokKegiatanKemahasiswaan()}, adalah rincian
	 * tingkat kedua dari kelompok yang sama.</p>
	 *
	 * <p><b>Beda istilah kode vs layar:</b> di UI properti ini berlabel <b>"Aspek Kegiatan"</b>
	 * (kolom grid "Aspek Kegiatan", label form "Aspek Kegiatan *"), bukan "Kelompok". Saat
	 * menelusuri laporan bug pengguna, "aspek" = {@code kelompokKegiatanKemahasiswaan}.</p>
	 *
	 * <p>Kelompok juga ikut menentukan apakah kegiatan boleh dipilih mahasiswa: penyaring
	 * pemilihan mensyaratkan {@code kelompok.bisaDipilihMahasiswa == true} dan
	 * {@code kelompok.aktif == true} &mdash; lihat {@link #getBolehDipilih()}.</p>
	 *
	 * @return kelompok kegiatan, sudah terinisialisasi bila memungkinkan
	 * @see ais.database.model.GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kemahasiswaan", nullable = false)
	public KelompokKegiatanKemahasiswaan getKelompokKegiatanKemahasiswaan() {
		kelompokKegiatanKemahasiswaan = check(kelompokKegiatanKemahasiswaan);
		return kelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengisi kelompok kegiatan (wajib terisi sebelum disimpan).
	 *
	 * @param kelompokKegiatanKemahasiswaan kelompok kegiatan
	 */
	public void setKelompokKegiatanKemahasiswaan(KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan) {
		this.kelompokKegiatanKemahasiswaan = kelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan tanggal mulai kegiatan.
	 *
	 * @return tanggal mulai (presisi hari), atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Mengisi tanggal mulai kegiatan.
	 *
	 * <p>Tidak ada validasi bahwa {@code mulai} mendahului {@link #getSampai()}; urutan tanggal
	 * (bila diperiksa sama sekali) menjadi tanggung jawab lapisan action/form.</p>
	 *
	 * @param mulai tanggal mulai
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal selesai kegiatan.
	 *
	 * @return tanggal selesai (presisi hari), atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi tanggal selesai kegiatan.
	 *
	 * @param sampai tanggal selesai
	 * @see #setMulai(Date) catatan tentang tidak adanya validasi urutan tanggal
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan kode ringkas kegiatan, <b>menghitung dan menyimpannya sendiri bila masih
	 * kosong</b>.
	 *
	 * <p>Bila {@link #id} sudah ada tetapi {@code kode} masih {@code null}/kosong, method
	 * membentuk kode dari id: id di-prefiks {@code "0000000000"} lalu diambil <b>5 karakter
	 * terakhir</b> &mdash; id {@code 7} menjadi {@code "00007"}, id {@code 12345} menjadi
	 * {@code "12345"}. Hasilnya <b>ditulis ke field</b> {@code kode} yang terpetakan ke kolom
	 * {@code kode}, jadi ini salah satu getter yang bisa mengubah isi database hanya karena
	 * dibaca (dengan {@code dynamicUpdate = true}, flush berikutnya akan mengirim
	 * {@code UPDATE}).</p>
	 *
	 * <p><b>Batas yang tersembunyi:</b> pemotongan 5 karakter terakhir berarti kode <b>berputar
	 * setelah id melewati 99999</b> &mdash; id {@code 100007} juga menghasilkan {@code "00007"}.
	 * Kode ini karena itu tidak boleh diperlakukan sebagai pengenal unik; yang unik adalah
	 * {@link #getId()} dan (secara constraint) {@link #getNama()}.</p>
	 *
	 * <p>Kode hanya dihitung untuk baris yang <i>sudah</i> punya id, sehingga object baru yang
	 * belum disimpan selalu mengembalikan {@code null} di sini.</p>
	 *
	 * @return kode 5 digit, kode yang sudah diisi manual, atau {@code null} bila baris belum
	 *         punya id
	 */
	public String getKode() {
		if (id != null && (kode == null || kode.trim().isEmpty())) {
			String k = "0000000000" + id;
			kode = k.substring(k.length() - 5);
		}
		return kode;
	}

	/**
	 * Mengisi kode kegiatan secara manual.
	 *
	 * <p>Nilai yang diisi di sini <b>mengalahkan</b> perhitungan otomatis di {@link #getKode()}:
	 * selama kode tidak kosong, getter tidak akan menghitung ulang.</p>
	 *
	 * @param kode kode kegiatan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan rincian kelompok kegiatan (master tingkat 2) setelah meresolusi proxy lazy.
	 *
	 * <p>Berpasangan dengan {@link #getKelompokKegiatanKemahasiswaan()}: master ini adalah anak
	 * dari kelompok tersebut ({@link DetailKelompokKegiatanKemahasiswaan} menyimpan referensi ke
	 * {@code KelompokKegiatanKemahasiswaan} induknya, serta koleksi jabatan dan skala yang boleh
	 * dipilih). Perlu diperhatikan: <b>tidak ada penjagaan konsistensi</b> di class ini yang
	 * memastikan detail yang dipilih benar-benar milik kelompok yang dipilih &mdash; itu urusan
	 * form/action.</p>
	 *
	 * <p>Sama seperti relasi lain, hasil {@code check(...)} ditulis balik ke field; lihat
	 * {@link #getKelompokKegiatanKemahasiswaan()} untuk penjelasan efek sampingnya.</p>
	 *
	 * <p><b>Beda istilah kode vs layar:</b> di UI properti ini berlabel <b>"Rincian Aspek
	 * Kegiatan"</b> (kolom grid "Aspek Rinci"), bukan "Detail Kelompok".</p>
	 *
	 * @return rincian kelompok kegiatan, sudah terinisialisasi bila memungkinkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_kelompok_kegiatan_kemahasiswaan", nullable = false)
	public DetailKelompokKegiatanKemahasiswaan getDetailKelompokKegiatanKemahasiswaan() {
		detailKelompokKegiatanKemahasiswaan = check(detailKelompokKegiatanKemahasiswaan);
		return detailKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengisi rincian kelompok kegiatan (wajib terisi sebelum disimpan).
	 *
	 * @param detailKelompokKegiatanKemahasiswaan rincian kelompok kegiatan
	 */
	public void setDetailKelompokKegiatanKemahasiswaan(
			DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan) {
		this.detailKelompokKegiatanKemahasiswaan = detailKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan status alur persetujuan kegiatan, dengan bawaan {@link #BELUM_DIPROSES}.
	 *
	 * <p>Bila kolom {@code status} masih {@code null} &mdash; kasus khas baris lama yang dibuat
	 * sebelum alur persetujuan diperkenalkan &mdash; getter mengembalikan
	 * {@link #BELUM_DIPROSES}. <b>Nilai bawaan itu TIDAK ditulis balik ke field</b>, berbeda dari
	 * {@link #getTahunAkademik()}/{@link #getJenisSemester()}. Konsekuensinya berbeda arah pula:
	 * karena Hibernate membaca lewat getter (property access), baris yang di database bernilai
	 * {@code NULL} tetap akan dibandingkan sebagai {@code "Belum diproses"} saat dirty-checking,
	 * sehingga flush pertama dapat menuliskan teks itu ke kolom yang tadinya {@code NULL}.</p>
	 *
	 * <p><b>Apa yang dibuka oleh {@link #DISETUJUI}</b> (diverifikasi di action/helper terkait,
	 * bukan di class ini): kegiatan baru muncul di daftar pilih mahasiswa, tombol kirim ke Neo
	 * Feeder baru tampil, dan kegiatan baru bisa dirujuk dari layar Formulir Kegiatan. Ada pula
	 * aksi "setujui semua" massal yang menaikkan status seluruh kegiatan terfilter ke
	 * {@link #DISETUJUI} kecuali yang berstatus {@link #DITOLAK}.</p>
	 *
	 * <p><b>Kuirk lintas class:</b> beberapa perbandingan status kegiatan di
	 * {@code KegiatanKemahasiswaanAction} memakai konstanta {@code PrestasiMahasiswa.DISETUJUI}
	 * alih-alih {@link #DISETUJUI}. Nilai stringnya kebetulan sama sehingga tidak pernah
	 * ketahuan, tetapi kopling itu salah kelas: mengubah teks {@link #DISETUJUI} di sini
	 * <b>tidak</b> akan mengubah perbandingan tersebut.</p>
	 *
	 * @return salah satu dari {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
	 *         {@link #DISETUJUI}, {@link #DITOLAK}, atau nilai lain yang pernah ditulis
	 *         pemanggil; tidak pernah {@code null}
	 */
	public String getStatus() {
		return status == null ? BELUM_DIPROSES : status;
	}

	/**
	 * Mengisi status alur persetujuan.
	 *
	 * <p>Tidak ada validasi bahwa nilainya termasuk salah satu dari empat konstanta status class
	 * ini &mdash; kolomnya {@code varchar} biasa tanpa constraint. Selalu pakai konstanta
	 * ({@link #DISETUJUI} dst.), jangan literal string, supaya pencocokan di layar daftar tidak
	 * meleset.</p>
	 *
	 * @param status status baru; sebaiknya salah satu konstanta status class ini
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan mahasiswa pengusul kegiatan setelah meresolusi proxy lazy.
	 *
	 * <p>Relasi ini {@code nullable = true}: kegiatan yang didaftarkan langsung oleh operator
	 * kemahasiswaan tidak punya pengusul. Karena itu <b>jangan</b> menjadikan kolom ini
	 * satu-satunya penentu kepemilikan data saat menyaring daftar per mahasiswa &mdash; baris
	 * dengan {@code diajukanOleh} kosong akan hilang dari saringan semacam itu.</p>
	 *
	 * <p><b>Ini pengusul, BUKAN daftar peserta.</b> Keanggotaan kegiatan disimpan di
	 * {@link KegiatanKemahasiswaanPunyaMahasiswa}; seorang mahasiswa bisa menjadi peserta tanpa
	 * pernah menjadi pengusul, dan sebaliknya. Laporan yang merekap "kegiatan yang diikuti
	 * mahasiswa X" berangkat dari tabel kepesertaan itu, bukan dari kolom ini.</p>
	 *
	 * <p>Hasil {@code check(...)} ditulis balik ke field; lihat
	 * {@link #getKelompokKegiatanKemahasiswaan()}.</p>
	 *
	 * @return mahasiswa pengusul, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Mahasiswa getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);
		return diajukanOleh;
	}

	/**
	 * Mengisi mahasiswa pengusul kegiatan.
	 *
	 * @param diajukanOleh mahasiswa pengusul; boleh {@code null}
	 */
	public void setDiajukanOleh(Mahasiswa diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/**
	 * Mengembalikan program studi/jurusan penyelenggara setelah meresolusi proxy lazy.
	 *
	 * <p>Opsional: kegiatan tingkat universitas boleh tidak terikat jurusan. Tidak ada penjagaan
	 * bahwa jurusan ini konsisten dengan {@link #getFakultas()} maupun dengan jurusan
	 * {@link #getDiajukanOleh()}.</p>
	 *
	 * <p>Hasil {@code check(...)} ditulis balik ke field; lihat
	 * {@link #getKelompokKegiatanKemahasiswaan()}.</p>
	 *
	 * @return jurusan penyelenggara, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengisi program studi/jurusan penyelenggara.
	 *
	 * @param jurusan jurusan penyelenggara; boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas penyelenggara setelah meresolusi proxy lazy.
	 *
	 * <p>Disimpan terpisah dari {@link #getJurusan()} (bukan diturunkan dari jurusan), sehingga
	 * kedua kolom bisa saja tidak konsisten satu sama lain bila diisi lewat impor.</p>
	 *
	 * <p>Hasil {@code check(...)} ditulis balik ke field; lihat
	 * {@link #getKelompokKegiatanKemahasiswaan()}.</p>
	 *
	 * @return fakultas penyelenggara, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengisi fakultas penyelenggara.
	 *
	 * @param fakultas fakultas penyelenggara; boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan lokasi penyelenggaraan kegiatan.
	 *
	 * <p>Teks bebas, <b>bukan</b> relasi ke master ruang/gedung &mdash; jadi tidak bisa dipakai
	 * untuk mendeteksi bentrok pemakaian ruangan. Di layar berlabel <b>"Tempat / Alamat Kegiatan
	 * *"</b> (ditandai wajib di form, meski kolomnya sendiri {@code nullable} di database), dan
	 * nilainya dikirim ke Neo Feeder sebagai {@code lokasi}.</p>
	 *
	 * @return nama tempat, atau {@code null}
	 */
	public String getTempat() {
		return tempat;
	}

	/**
	 * Mengisi lokasi penyelenggaraan kegiatan.
	 *
	 * @param tempat nama tempat (teks bebas)
	 */
	public void setTempat(String tempat) {
		this.tempat = tempat;
	}

	/**
	 * Mengembalikan tautan dokumentasi/publikasi kegiatan.
	 *
	 * <p>Kolom bertipe {@code text} dan isinya <b>tidak divalidasi maupun dibersihkan</b> di sini.
	 * Bila nilai ini dirender sebagai atribut {@code href} di layar, penyaringan skema URL
	 * (mis. menolak {@code javascript:}) harus dilakukan di lapisan tampilan &mdash; class ini
	 * tidak melakukannya.</p>
	 *
	 * @return URL kegiatan, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url;
	}

	/**
	 * Mengisi tautan dokumentasi/publikasi kegiatan.
	 *
	 * @param url tautan; tidak divalidasi
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Mengembalikan dosen pembina utama setelah meresolusi proxy lazy.
	 *
	 * <p><b>Perhatikan nama kolomnya:</b> {@code dosen_pmbina1} &mdash; salah eja ("pmbina")
	 * yang sudah terlanjur ada di skema database. Jangan diperbaiki di sini tanpa migrasi
	 * DDL yang menyertainya.</p>
	 *
	 * <p>Hasil {@code check(...)} ditulis balik ke field; lihat
	 * {@link #getKelompokKegiatanKemahasiswaan()}.</p>
	 *
	 * @return dosen pembina utama, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pmbina1", nullable = true)
	public Dosen getDosenPembina1() {
		dosenPembina1 = check(dosenPembina1);
		return dosenPembina1;
	}

	/**
	 * Mengisi dosen pembina utama.
	 *
	 * @param dosenPembina1 dosen pembina utama; boleh {@code null}
	 */
	public void setDosenPembina1(Dosen dosenPembina1) {
		this.dosenPembina1 = dosenPembina1;
	}

	/**
	 * Mengembalikan dosen pembina pendamping setelah meresolusi proxy lazy.
	 *
	 * <p>Kolomnya {@code dosen_pmbina2} &mdash; salah eja yang sama seperti pada
	 * {@link #getDosenPembina1()}. Tidak ada penjagaan bahwa pembina 1 dan 2 berbeda orang.</p>
	 *
	 * @return dosen pembina pendamping, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pmbina2", nullable = true)
	public Dosen getDosenPembina2() {
		dosenPembina2 = check(dosenPembina2);
		return dosenPembina2;
	}

	/**
	 * Mengisi dosen pembina pendamping.
	 *
	 * @param dosenPembina2 dosen pembina pendamping; boleh {@code null}
	 */
	public void setDosenPembina2(Dosen dosenPembina2) {
		this.dosenPembina2 = dosenPembina2;
	}

	/**
	 * Mengembalikan skala/cakupan kegiatan setelah meresolusi proxy lazy.
	 *
	 * <p>Master {@link SkalaKegiatanKemahasiswaan} berisi tingkatan seperti lokal/wilayah/
	 * nasional/internasional. Perhatikan bahwa pilihan skala yang "sah" sebenarnya dibatasi oleh
	 * koleksi {@code skalaKegiatanKemahasiswaans} milik
	 * {@link DetailKelompokKegiatanKemahasiswaan}, tetapi <b>pembatasan itu tidak ditegakkan di
	 * class ini</b> &mdash; hanya di form.</p>
	 *
	 * @return skala kegiatan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skala_kegiatan_kemahasiswaan", nullable = true)
	public SkalaKegiatanKemahasiswaan getSkalaKegiatanKemahasiswaan() {
		skalaKegiatanKemahasiswaan = check(skalaKegiatanKemahasiswaan);
		return skalaKegiatanKemahasiswaan;
	}

	/**
	 * Mengisi skala/cakupan kegiatan.
	 *
	 * @param skalaKegiatanKemahasiswaan skala kegiatan; boleh {@code null}
	 */
	public void setSkalaKegiatanKemahasiswaan(SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan) {
		this.skalaKegiatanKemahasiswaan = skalaKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan jabatan yang diampu dalam kegiatan, setelah meresolusi proxy lazy.
	 *
	 * <p>Sama seperti skala, daftar jabatan yang sah sesungguhnya bersumber dari koleksi
	 * {@code jabatanKegiatanKemahasiswaans} milik {@link DetailKelompokKegiatanKemahasiswaan};
	 * class ini tidak menegakkannya.</p>
	 *
	 * <p><b>Catatan pemodelan:</b> jabatan di sini melekat pada <i>kegiatan</i>, bukan pada
	 * pasangan (kegiatan, mahasiswa) &mdash; satu baris kegiatan hanya menyimpan satu jabatan.
	 * Jabatan <i>per peserta</i> punya kolomnya sendiri di
	 * {@link KegiatanKemahasiswaanPunyaMahasiswa}, dan nilainya bisa berbeda dari yang tercatat
	 * di sini. Rubrik penilaian ({@code NilaiKegiatanKemahasiswaan}) memakai kombinasi rincian
	 * aspek &times; jabatan &times; skala, jadi ketidakcocokan antara dua tingkat itu berdampak
	 * pada rekap nilai.</p>
	 *
	 * @return jabatan dalam kegiatan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_kegiatan_kemahasiswaan", nullable = true)
	public JabatanKegiatanKemahasiswaan getJabatanKegiatanKemahasiswaan() {
		jabatanKegiatanKemahasiswaan = check(jabatanKegiatanKemahasiswaan);
		return jabatanKegiatanKemahasiswaan;
	}

	/**
	 * Mengisi jabatan yang diampu dalam kegiatan.
	 *
	 * @param jabatanKegiatanKemahasiswaan jabatan; boleh {@code null}
	 */
	public void setJabatanKegiatanKemahasiswaan(JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan) {
		this.jabatanKegiatanKemahasiswaan = jabatanKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan tahun penyelenggaraan, <b>selalu menghitung ulang dari tahun akademik</b>.
	 *
	 * <p>Bila field {@link #tahunAkademik} tidak {@code null}, method mengambil potongan sebelum
	 * {@code "/"} dan mem-{@code parse}-nya sebagai {@link Integer}, lalu <b>menimpa field</b>
	 * {@link #tahun}. Untuk {@code "2025/2026"} hasilnya {@code 2025}. Nilai apa pun yang
	 * sebelumnya diisi lewat {@link #setTahun(Integer)} akan <b>hilang</b> begitu getter ini
	 * dipanggil &mdash; jadi {@code tahun} praktis adalah kolom turunan, bukan masukan.</p>
	 *
	 * <p><b>Efek samping DB:</b> field {@code tahun} terpetakan ke kolom {@code tahun}, dan
	 * penulisan di sini dapat memicu {@code UPDATE} pada flush berikutnya untuk object yang
	 * {@code managed} ({@code dynamicUpdate = true}). Ini juga bisa "memperbaiki" diam-diam
	 * baris-baris lama yang kolom tahunnya tidak cocok dengan tahun akademiknya.</p>
	 *
	 * <p><b>Titik buta:</b> pemeriksaan memakai <i>field</i> {@code tahunAkademik} secara
	 * langsung, bukan {@link #getTahunAkademik()}. Bila tahun akademik masih {@code null},
	 * pengisian otomatis periode berjalan <b>tidak</b> ikut terpicu di sini, dan method
	 * mengembalikan nilai {@code tahun} apa adanya. Urutan pemanggilan karena itu berpengaruh:
	 * memanggil {@link #getTahunAkademik()} lebih dulu memberi hasil yang berbeda.</p>
	 *
	 * <p>Kegagalan {@code parse} (format tahun akademik tidak baku) ditangkap dan dicatat ke
	 * {@code ErrorAuditUtil}, lalu diabaikan &mdash; {@code tahun} tetap pada nilai lamanya.
	 * Penanda {@code auto-audit(empty-catch)} pada blok {@code catch} berasal dari inisiatif
	 * audit blok catch kosong, terpisah dari inisiatif Javadoc ini; nomor baris di dalam pesannya
	 * sudah tidak lagi cocok dengan posisi sebenarnya.</p>
	 *
	 * @return tahun penyelenggaraan, atau {@code null} bila belum pernah bisa ditentukan
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KegiatanKemahasiswaan.java:307");

			}
		}
		return tahun;
	}

	/**
	 * Mengisi tahun penyelenggaraan.
	 *
	 * <p><b>Nilainya tidak awet.</b> {@link #getTahun()} akan menimpanya dari
	 * {@link #tahunAkademik} pada pembacaan berikutnya, kecuali tahun akademik memang
	 * {@code null}. Untuk mengubah tahun secara efektif, ubah tahun akademiknya.</p>
	 *
	 * @param tahun tahun penyelenggaraan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun akademik penyelenggaraan, <b>mengisinya dengan periode berjalan bila
	 * masih kosong</b>.
	 *
	 * <p>Bila field {@code null}, method memanggil {@link Common#getCurrentTahunAkademik()} dan
	 * <b>menulis hasilnya ke field</b>. Karena field ini terpetakan (kolom {@code tahunAkademik},
	 * penamaan default {@code MyNamingStrategy}), sekadar membaca getter ini pada entity
	 * {@code managed} bisa menghasilkan {@code UPDATE} ke database tanpa aksi simpan dari
	 * pengguna &mdash; termasuk pada baris lama yang memang sengaja dibiarkan tanpa tahun
	 * akademik.</p>
	 *
	 * <p><b>Yang diisikan adalah periode SAAT DIBACA, bukan saat kegiatan berlangsung.</b> Untuk
	 * data historis yang diimpor, ini berarti kegiatan lama bisa mendapat cap tahun akademik
	 * berjalan. Bila periode kegiatan yang benar sudah diketahui, isi eksplisit lewat
	 * {@link #setTahunAkademik(String)} sebelum getter ini pernah dipanggil.</p>
	 *
	 * <p>Nilai ini juga menjadi bahan {@code id_semester} yang dikirim ke Neo Feeder (tahun
	 * akademik digabung dengan {@code 1}/{@code 2} sesuai {@link #getJenisSemester()}), dan
	 * menjadi sumber tunggal {@link #getTahun()}.</p>
	 *
	 * @return tahun akademik format {@code "2025/2026"}; tidak {@code null} kecuali
	 *         {@link Common#getCurrentTahunAkademik()} sendiri mengembalikan {@code null}
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik penyelenggaraan.
	 *
	 * <p>Mengisi ini secara eksplisit adalah satu-satunya cara mencegah pengisian otomatis
	 * periode berjalan di {@link #getTahunAkademik()}. Nilai ini juga menjadi sumber
	 * {@link #getTahun()}, jadi format {@code "YYYY/YYYY"} harus dipertahankan.</p>
	 *
	 * @param tahunAkademik tahun akademik, format {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester penyelenggaraan, <b>mengisinya dari periode berjalan bila
	 * masih kosong</b>.
	 *
	 * <p>Bila field {@code null}, method menanyakan {@link Common#isNowSemensterGanjil()}
	 * (ejaan aslinya memang demikian di {@code Common}) dan menulis {@link Perkuliahan#GANJIL}
	 * atau {@link Perkuliahan#GENAP} <b>ke field</b>. Efek sampingnya identik dengan
	 * {@link #getTahunAkademik()}: pembacaan pada entity {@code managed} bisa berbuntut
	 * {@code UPDATE}, dan nilai yang terisi adalah semester <i>saat dibaca</i>, bukan semester
	 * kegiatan berlangsung.</p>
	 *
	 * <p>Nilai yang dipakai adalah teks {@code "Ganjil"}/{@code "Genap"} dari konstanta
	 * {@link Perkuliahan}, bukan angka &mdash; sama seperti seluruh modul akademik.</p>
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
	 * Mengisi jenis semester penyelenggaraan.
	 *
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}; mengisi
	 *                      eksplisit mencegah penebakan otomatis di {@link #getJenisSemester()}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan template sertifikat kegiatan setelah meresolusi proxy lazy.
	 *
	 * <p>Menunjuk master {@link Sertifikat} (tabel {@code sertifikat}) yang dipakai saat mencetak
	 * sertifikat bagi peserta kegiatan ini &mdash; tombol cetaknya baru muncul setelah peserta
	 * bersangkutan disetujui di {@link KegiatanKemahasiswaanPunyaMahasiswa}. Di layar,
	 * combobox-nya menyediakan pilihan kosong bertuliskan {@code "== Tanpa Sertifikat =="}.
	 * Relasi yang sama juga dimiliki {@link FormulirKegiatan}, sehingga sertifikat dapat
	 * ditetapkan di tingkat kegiatan maupun di tingkat formulir &mdash; class ini tidak
	 * menentukan mana yang menang.</p>
	 *
	 * <p>Hasil {@code check(...)} ditulis balik ke field; lihat
	 * {@link #getKelompokKegiatanKemahasiswaan()}.</p>
	 *
	 * @return template sertifikat, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	/**
	 * Mengisi template sertifikat kegiatan.
	 *
	 * @param sertifikat template sertifikat; boleh {@code null}
	 */
	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	/**
	 * Mengembalikan penanda apakah kegiatan ini masih boleh dipilih, dengan bawaan {@code true}.
	 *
	 * <p>Di layar, ini adalah centang <b>"Kegiatan ini bisa dipilih oleh mahasiswa lainnya"</b>:
	 * penandanya menentukan apakah mahasiswa <i>lain</i> boleh mendaftarkan diri ke kegiatan yang
	 * sudah ada. Bawaan {@code true} memastikan baris lama yang kolomnya masih {@code NULL} tetap
	 * ikut terpilih &mdash; jadi penyembunyian bersifat <i>opt-in</i>, harus disetel
	 * {@code false} secara sengaja.</p>
	 *
	 * <p><b>Bukan satu-satunya syarat.</b> Penyaring daftar pilih kegiatan bagi mahasiswa
	 * menggabungkan <b>empat</b> syarat sekaligus: {@code bolehDipilih IS NULL OR bolehDipilih =
	 * true}, <b>dan</b> {@code kelompokKegiatanKemahasiswaan.bisaDipilihMahasiswa = true},
	 * <b>dan</b> {@code kelompokKegiatanKemahasiswaan.aktif = true}, <b>dan</b>
	 * {@code status = }{@link #DISETUJUI}. Mematikan salah satu saja sudah cukup untuk
	 * menyembunyikan kegiatan &mdash; sumber kebingungan yang sering muncul saat pengguna
	 * melaporkan "kegiatan tidak muncul padahal centangnya menyala".</p>
	 *
	 * <p>Nilai bawaan <b>tidak ditulis balik ke field</b> (tidak seperti
	 * {@link #getTahunAkademik()}), namun karena Hibernate membaca lewat getter, baris yang di
	 * database {@code NULL} bisa tetap terkirim sebagai {@code true} pada flush berikutnya.</p>
	 *
	 * <p><b>Bukan kontrol keamanan.</b> Nilai ini menyaring pilihan di lapisan tampilan; ia tidak
	 * membatasi siapa yang boleh membaca atau mengubah baris ini.</p>
	 *
	 * @return {@code true} bila kegiatan boleh dipilih (termasuk saat kolom masih {@code null});
	 *         tidak pernah {@code null}
	 */
	public Boolean getBolehDipilih() {
		return bolehDipilih == null ? true : bolehDipilih;
	}

	/**
	 * Mengisi penanda boleh-dipilih.
	 *
	 * @param bolehDipilih {@code false} untuk menyembunyikan kegiatan dari daftar pilihan tanpa
	 *                     menghapus datanya
	 */
	public void setBolehDipilih(Boolean bolehDipilih) {
		this.bolehDipilih = bolehDipilih;
	}

	/**
	 * Mengembalikan nama kegiatan dalam bahasa Inggris.
	 *
	 * <p>Dipetakan eksplisit ke kolom {@code namaen} (huruf kecil semua) karena
	 * {@code MyNamingStrategy} tidak mengubah camelCase menjadi {@code snake_case} sementara
	 * PostgreSQL melipat pengenal tanpa kutip menjadi huruf kecil. Berbeda dari
	 * {@link #getNama()}, kolom ini bertipe {@code text}, tidak wajib, dan <b>tidak unik</b>.</p>
	 *
	 * @return nama kegiatan dalam bahasa Inggris, atau {@code null}
	 */
	@Column(name = "namaen", columnDefinition = "text")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Mengisi nama kegiatan dalam bahasa Inggris.
	 *
	 * <p>Tidak ada pemotongan panjang di sini (kolomnya {@code text}), berbeda dari
	 * {@link #setNama(String)}.</p>
	 *
	 * @param namaEn nama dalam bahasa Inggris
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan jenis aktivitas mahasiswa menurut referensi Neo Feeder/PDDIKTI, dengan
	 * <b>fallback ke konstanta global</b>.
	 *
	 * <p>Alurnya dua tahap dan keduanya penting:</p>
	 * <ol>
	 *   <li>{@code check(...)} meresolusi proxy lazy dan <b>menulis hasilnya ke field</b>, sama
	 *       seperti relasi lain di class ini.</li>
	 *   <li>Bila setelah itu field masih {@code null}, yang dikembalikan adalah
	 *       {@code ConstantValues.KEGIATAN_KEMAHASISWAAN} &mdash; <b>tanpa</b> ditulis ke
	 *       field.</li>
	 * </ol>
	 *
	 * <p><b>Inilah satu-satunya getter di class ini yang bisa mengembalikan object yang tidak
	 * sama dengan isi field.</b> Karena Hibernate memakai property access, nilai yang dibaca saat
	 * {@code INSERT}/flush adalah nilai <i>hasil getter</i>: baris yang jenis aktivitasnya
	 * dibiarkan kosong oleh pengguna tetap dapat tersimpan dengan FK menunjuk jenis bawaan
	 * tersebut. Ditambah {@code cascade = PERSIST, MERGE}, object konstanta itu ikut terbawa ke
	 * dalam session yang sedang menyimpan &mdash; sesuatu yang perlu diingat karena konstanta itu
	 * berumur panjang (static) dan berasal dari session lain.</p>
	 *
	 * <p>{@code ConstantValues.KEGIATAN_KEMAHASISWAAN} sendiri diisi oleh
	 * {@code ais.common.InitDataHelper} saat sinkronisasi referensi Neo Feeder, yaitu baris
	 * {@link JenisAktfitasMahasiswa} yang namanya persis {@code "Aktivitas kemahasiswaan"}. Bila
	 * sinkronisasi itu belum pernah berjalan (atau nama referensinya berubah di Feeder),
	 * konstantanya masih {@code null} dan getter ini mengembalikan {@code null} &mdash; jadi
	 * pemanggil tetap harus siap menerima {@code null}.</p>
	 *
	 * <p>Di layar, properti ini berlabel <b>"Jenis / Kampus Merdeka"</b> dan dibuat
	 * <i>disabled</i> bila yang login adalah mahasiswa. Nilai yang dikirim ke Feeder berasal dari
	 * {@code JenisAktfitasMahasiswa#getFeeder()}; bila kosong, pengirim memakai {@code "10"}
	 * sebagai jenis aktivitas bawaan.</p>
	 *
	 * <p>Pola identik terdapat di {@code FormulirKegiatan#getJenisAktfitasMahasiswa()}.</p>
	 *
	 * @return jenis aktivitas mahasiswa; jenis bawaan "Aktivitas kemahasiswaan" bila belum
	 *         dipilih; {@code null} bila referensi Feeder belum pernah disinkronkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_aktfitas_mahasiswa", nullable = true)
	public JenisAktfitasMahasiswa getJenisAktfitasMahasiswa() {
		jenisAktfitasMahasiswa = check(jenisAktfitasMahasiswa);
		return (JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa == null ? ConstantValues.KEGIATAN_KEMAHASISWAAN
				: jenisAktfitasMahasiswa);
	}

	/**
	 * Mengisi jenis aktivitas mahasiswa (referensi Neo Feeder/PDDIKTI).
	 *
	 * <p>Mengisi {@code null} di sini <b>tidak</b> berarti getter akan mengembalikan
	 * {@code null} &mdash; lihat mekanisme fallback di
	 * {@link #getJenisAktfitasMahasiswa()}.</p>
	 *
	 * @param jenisAktfitasMahasiswa jenis aktivitas mahasiswa
	 */
	public void setJenisAktfitasMahasiswa(JenisAktfitasMahasiswa jenisAktfitasMahasiswa) {
		this.jenisAktfitasMahasiswa = jenisAktfitasMahasiswa;
	}

	/**
	 * Mengembalikan penanda Neo Feeder (PDDIKTI) untuk baris ini, <b>dinormalkan</b>.
	 *
	 * <p>String kosong atau yang hanya berisi spasi diperlakukan sama dengan "belum ada" dan
	 * dikembalikan sebagai {@code null}; selain itu nilainya di-{@code trim}. Normalisasi ini
	 * terjadi <b>saat baca saja</b> &mdash; {@link #setFeeder(String)} menyimpan apa adanya.</p>
	 *
	 * <p><b>ISINYA BUKAN SATU ID, MELAINKAN DOKUMEN JSON.</b> Ini hal paling mudah salah paham
	 * dari properti ini. {@code FeederExporter} memperlakukan nilai di sini sebagai
	 * {@code JSONObject} berbentuk peta <b>{@code id jurusan &rarr; id_aktivitas di Feeder}</b>,
	 * mis. {@code {"12":"a1b2-...","15":"c3d4-..."}}. Alasannya: satu kegiatan kemahasiswaan bisa
	 * melibatkan mahasiswa dari beberapa program studi, sedangkan Neo Feeder mencatat aktivitas
	 * <i>per program studi</i> &mdash; jadi satu baris di sini dapat berpadanan dengan
	 * <b>banyak</b> aktivitas di Feeder. Alur pengirimannya: untuk tiap jurusan, id aktivitas
	 * dicari di peta ini; ada &rarr; {@code UpdateAktivitasMahasiswa}, belum ada &rarr;
	 * {@code InsertAktivitasMahasiswa}, lalu id hasilnya dimasukkan ke peta dan disimpan kembali
	 * lewat {@link #setFeeder(String)} + {@code Common.refreshUpdate(...)}.</p>
	 *
	 * <p>Karena itu <b>jangan</b> membandingkan nilai ini dengan sebuah id, dan jangan
	 * menimpanya dengan skalar &mdash; itu akan menghapus jejak sinkronisasi seluruh jurusan
	 * lain. Pemeriksaan yang aman hanyalah "sudah pernah tersinkron atau belum"
	 * ({@code getFeeder() != null}); selebihnya harus lewat parsing JSON.</p>
	 *
	 * <p><b>Konsekuensi normalisasi yang mudah terlewat:</b> karena Hibernate membaca lewat
	 * getter, baris lama yang di database berisi string kosong akan terbaca sebagai {@code null}
	 * dan, pada flush berikutnya, dapat ter-{@code UPDATE} menjadi {@code NULL} betulan.</p>
	 *
	 * <p>Perhatikan bedanya dengan {@code JenisAktfitasMahasiswa#getFeeder()} yang bertipe
	 * {@link Long} dan memang berisi satu id referensi: di sini penandanya {@link String} berisi
	 * JSON, dan kolomnya bertipe {@code text}.</p>
	 *
	 * @return dokumen JSON pemetaan jurusan&rarr;id aktivitas Feeder yang sudah di-{@code trim},
	 *         atau {@code null} bila kegiatan ini belum pernah dikirim ke Feeder
	 */
	@Column(columnDefinition = "text")
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Mengisi penanda Neo Feeder (PDDIKTI) untuk baris ini.
	 *
	 * <p>Disimpan apa adanya, tanpa {@code trim} maupun konversi kosong-jadi-{@code null};
	 * normalisasi baru terjadi di {@link #getFeeder()}. Pemanggil yang sah praktis hanya
	 * {@code FeederExporter}, yang mengisinya dengan hasil {@code JSONObject#toString()}.</p>
	 *
	 * @param feeder dokumen JSON pemetaan jurusan&rarr;id aktivitas Feeder; lihat
	 *               {@link #getFeeder()} sebelum menimpanya dengan nilai lain
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan nomor Surat Keputusan yang melandasi kegiatan.
	 *
	 * <p>Berpasangan dengan {@link #getTglSk()}; keduanya opsional dan tidak divalidasi
	 * berpasangan (boleh ada nomor tanpa tanggal, dan sebaliknya). Di layar berlabel <b>"Nomor SK
	 * Kegiatan"</b>, dan nilainya ikut dikirim ke Neo Feeder sebagai {@code sk_tugas}
	 * (pasangannya {@code tanggal_sk_tugas} dari {@link #getTglSk()}).</p>
	 *
	 * <p><b>Jangan tertukar</b> dengan "SK Dosen Pembina I/II" di form yang sama: dua field itu
	 * bukan properti entity ini melainkan <i>unggahan berkas</i> {@code LampiranLain} yang
	 * dikunci pada nama class + id baris ini. SK yang tersimpan di kolom sini adalah SK
	 * <b>kegiatan</b>, bukan SK pembina.</p>
	 *
	 * @return nomor SK kegiatan, atau {@code null}
	 */
	public String getNoSk() {
		return noSk;
	}

	/**
	 * Mengisi nomor Surat Keputusan.
	 *
	 * @param noSk nomor SK
	 */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/**
	 * Mengembalikan tanggal Surat Keputusan yang melandasi kegiatan.
	 *
	 * @return tanggal SK (presisi hari), atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/**
	 * Mengisi tanggal Surat Keputusan.
	 *
	 * @param tglSk tanggal SK
	 */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}
}
