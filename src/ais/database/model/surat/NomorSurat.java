package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Templat penomoran dokumen &mdash; <b>MESIN PENOMORAN PUSAT</b> seluruh aplikasi AIS.
 *
 * <h3>Peran di dalam sistem</h3>
 * Meski secara paket berada di {@code ais.database.model.surat} (modul persuratan), entity ini
 * BUKAN milik modul surat saja. Ia adalah satu-satunya tempat di mana "bagaimana bentuk sebuah
 * nomor dokumen" didefinisikan, dan dirujuk oleh puluhan modul lain lewat sebuah field FK
 * opsional bernama {@code nomorSurat} pada entity konfigurasi masing-masing modul, antara lain:
 * <ul>
 *   <li><b>Keuangan/akunting</b> &mdash; {@code NomorSuratAlurKeuangan} (Kas Besar, Kas Kecil,
 *       Uang Muka, Dana Talangan, Penggantian Kas Kecil, Pertanggungjawaban, Proses Transfer,
 *       Standing Instruction, Reimbursement) serta {@code JenisTransaksi} untuk nomor jurnal.</li>
 *   <li><b>Aset/pengadaan</b> &mdash; {@code NomorSuratAlurPengadaan} (permintaan, pemesanan,
 *       penerimaan, pembayaran DP/termin/pelunasan, perjanjian kerja sama, pemakaian, peminjaman,
 *       pengembalian, penghapusan, penyedia).</li>
 *   <li><b>Penggajian</b> &mdash; pembayaran gaji, pengajuan transaksi pegawai.</li>
 *   <li><b>Persuratan</b> &mdash; {@code KlasifikasiSuratKeluar.nomorSurat} dan
 *       {@code .nomorAgenda}, {@code KlasifikasiSuratMasuk.nomorSurat}.</li>
 *   <li><b>Lain-lain</b> &mdash; pengaduan, pengajuan mahasiswa/siswa/pegawai, KPI, koperasi,
 *       seleksi vendor, catatan administrasi, perbaikan aset.</li>
 * </ul>
 * Konsekuensi praktisnya: setiap perubahan perilaku pada {@link #format(Long, Date)} atau pada
 * mekanisme counter di kelas ini berdampak LINTAS MODUL, termasuk ke dokumen keuangan yang
 * nomornya dipakai sebagai referensi resmi. Perlakukan kelas ini sebagai kode berisiko tinggi.
 *
 * <h3>Model data: sepuluh slot kolom + sepuluh pemisah</h3>
 * Sebuah nomor dibentuk dari maksimum sepuluh "slot" berpasangan: {@code kolom1..kolom10} berisi
 * JENIS isi slot (salah satu konstanta {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS},
 * {@link #TANGGAL}, {@link #BULAN}, {@link #BULAN_ROMAWI}, {@link #TAHUN}) dan
 * {@code tanda1..tanda10} berisi teks pemisah yang ditempelkan SESUDAH slot tersebut. Untuk slot
 * bertipe {@link #KATA_STATIS} justru {@code tandaN}-lah yang menjadi isi teks statisnya (lihat
 * {@link #format(Long, Date, SatuanKerja)}). Rangkaian slot inilah yang membuat satu templat bisa
 * menghasilkan bentuk seperti {@code 001/KEU/IX/2026} maupun {@code KB-2026-09-0001} tanpa
 * perubahan kode.
 *
 * <h3>Dua mode penomoran yang SANGAT berbeda &mdash; wajib dipahami</h3>
 * Angka urut yang disuplai ke {@code format(...)} dapat berasal dari dua sumber yang dipilih
 * lewat flag {@link #getGunakanIndexUrut()}:
 * <ol>
 *   <li><b>Mode counter tersimpan</b> ({@code gunakanIndexUrut = true}). Angka diambil dari kolom
 *       {@code nomorIndex} milik baris templat ini, lalu dinaikkan. Sejak perbaikan TOCTOU,
 *       pengambilan+kenaikan dilakukan atomik oleh
 *       {@link #ambilLaluTambahIndexNomorSurat(NomorSurat)}. Sifatnya MONOTONIK: menghapus
 *       dokumen tidak membuat nomor mundur.</li>
 *   <li><b>Mode hitung baris</b> ({@code gunakanIndexUrut = false}, ini nilai default field).
 *       Angka dihitung oleh method {@code getindex(NomorSurat)} yang di-<i>copy-paste</i> ke
 *       hampir setiap Action pemakai (mis. {@code KasBesarAction.getindex},
 *       {@code CommonAkunting.getindex}) sebagai {@code rowCount(tabel dokumen) + 1} dengan
 *       filter periode. Sifatnya TIDAK monotonik &mdash; menghapus/menonaktifkan satu dokumen
 *       membuat hitungan turun sehingga nomor berikutnya mengulang angka yang sudah terbit.
 *       Inilah alasan keberadaan {@code ais.action.master.KodeUnikUtil.pastikanUnik(...)} sebagai
 *       jaring pengaman sufiks ("-2", "-3", ...).</li>
 * </ol>
 *
 * <h3>Reset periode HANYA berlaku pada mode hitung baris</h3>
 * Field {@link #getResetUrutanTiapTahun()} (default {@code true}),
 * {@link #getResetUrutanTiapBulan()} dan {@link #getResetTiap()} TIDAK dibaca sama sekali oleh
 * kelas ini. Ketiganya hanya dikonsumsi di dalam {@code getindex(...)} milik Action pemakai,
 * yaitu sebagai {@code Restrictions} tambahan pada query {@code rowCount} (mis.
 * {@code Restrictions.eq("tahun", tahun)}). Artinya: <b>bila templat memakai mode counter
 * tersimpan, "reset tiap tahun" yang tercentang di layar konfigurasi TIDAK akan pernah
 * mereset apa pun</b> &mdash; {@code nomorIndex} akan terus naik melewati pergantian tahun. Ini
 * perilaku nyata kode saat ini, bukan dugaan; lihat catatan pada {@link #getResetUrutanTiapTahun()}.
 *
 * <h3>Placeholder tekstual di luar sepuluh slot</h3>
 * Selain slot terstruktur, string hasil masih dilewatkan penggantian teks untuk token
 * {@code KODE_SATKER}, {@code UNIT}, {@code SATKER}, {@code INDUK_SATKER}, {@code HAK_AKSES}
 * (dan &mdash; dengan cacat yang didokumentasikan di {@link #format(Long, Date, SatuanKerja)}
 * &mdash; {@code KODE_JABATAN}). Token-token ini biasanya ditulis admin ke dalam
 * {@code tandaN} sebuah slot {@link #KATA_STATIS}.
 *
 * <h3>Catatan Hibernate/Envers</h3>
 * <ul>
 *   <li>Pemetaan berbasis <b>anotasi pada getter</b> (property access), sehingga getter yang
 *       memiliki efek samping ikut dijalankan Hibernate saat flush &mdash; persis inilah akar
 *       masalah yang ditangani penjaga {@link #SEDANG_HITUNG_CONTOH_FORMAT}.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif: hanya kolom yang benar-benar berubah
 *       yang ikut di-{@code UPDATE}, penting karena baris templat sering di-update hanya untuk
 *       menaikkan {@code nomorIndex}.</li>
 *   <li>{@link Audited} &mdash; seluruh perubahan templat direkam Envers, sehingga perubahan
 *       format nomor dapat ditelusuri.</li>
 *   <li>Field audit {@code oleh}/{@code olehId}/{@code tanggal_dirubah} ditulis ulang di kelas
 *       ini (bukan diwarisi) karena {@link GeneralValueObject} bukan {@code @Entity} sehingga
 *       propertinya tidak ikut terpetakan &mdash; ini KEHARUSAN TEKNIS, bukan duplikasi ceroboh.</li>
 * </ul>
 *
 * <h3>Pemeliharaan</h3>
 * CRUD templat ini ada di {@code ais.action.master.surat.NomorSuratAction} (ber-parameter
 * {@code tipe} untuk memisahkan daftar per modul), pemilihannya lewat
 * {@code ais.action.master.surat.helper.AmbilDataNomorSuratBanbox}.
 *
 * @see KelompokNomorSurat
 * @see ais.action.master.KodeUnikUtil
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "nomor_surat")
public class NomorSurat extends GeneralValueObject {

	/**
	 * Jenis slot "kosong": slot diabaikan seluruhnya oleh
	 * {@link #format(Long, Date, SatuanKerja)} &mdash; baik isi maupun {@code tandaN}-nya TIDAK
	 * ikut dirangkai. Ini nilai default seluruh {@code kolom1..kolom10}, sehingga templat yang
	 * baru dibuat menghasilkan string kosong sampai admin mengisi minimal satu slot.
	 */
	public static final String KOSONG = "Kosong";

	/**
	 * Jenis slot "nomor urut": slot diisi angka urut dokumen, di-<i>pad</i> nol di depan sebanyak
	 * {@link #getJumlahAngkaNolDiDepanNomorUrut()} digit. Nilai dasar angka berasal dari mode
	 * penomoran aktif (counter tersimpan atau hitung baris), lalu DITAMBAH offset awal dari
	 * {@link KelompokNomorSurat#getMulaiUrutanKe()} atau {@link #getMulaiUrutanKe()} sesuai flag
	 * {@link #getUrutBerdasarkanKelompok()}/{@link #getUrutBerdasarkanNomor()}.
	 */
	public static final String NOMOR_URUT = "Nomor Urut";

	/**
	 * Jenis slot "kata statis": slot tidak menghasilkan isi tersendiri; yang dirangkai justru
	 * nilai {@code tandaN} pasangannya, sehingga admin memakai {@code tandaN} untuk menuliskan
	 * teks tetap seperti {@code "/KEU/"} atau token substitusi seperti {@code KODE_SATKER}.
	 */
	public static final String KATA_STATIS = "Kata Statis";

	/**
	 * Jenis slot "tanggal": diisi tanggal (hari dalam bulan) dari argumen {@code tanggal} yang
	 * diberikan pemanggil, selalu dua digit dengan nol di depan (mis. {@code "07"}).
	 */
	public static final String TANGGAL = "Tanggal";

	/**
	 * Jenis slot "bulan": diisi bulan dalam angka dari argumen {@code tanggal}, selalu dua digit
	 * dengan nol di depan (mis. {@code "09"} untuk September).
	 */
	public static final String BULAN = "Bulan";

	/**
	 * Jenis slot "bulan romawi": diisi bulan dalam angka Romawi ({@code I}..{@code XII}) yang
	 * diambil dari tabel {@code Common.ROMAWI} &mdash; bentuk yang lazim dipakai pada penomoran
	 * surat dinas Indonesia, mis. {@code 001/KEU/IX/2026}.
	 */
	public static final String BULAN_ROMAWI = "Bulan Romawi";

	/**
	 * Jenis slot "tahun": diisi tahun empat digit dari argumen {@code tanggal}, tanpa padding
	 * tambahan.
	 */
	public static final String TAHUN = "Tahun";

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja dibiarkan sama dengan beberapa entity lain
	 * hasil generator hbm2java; jangan diubah agar sesi ZK lama yang masih memegang objek ini
	 * tidak gagal dideserialisasi setelah <i>redeploy</i>.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer baris templat, dibangkitkan basis data ({@code IDENTITY}). */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (field audit bayangan &mdash; lihat catatan
	 * pada Javadoc kelas mengenai {@link GeneralValueObject} yang bukan {@code @Entity}).
	 */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini (pasangan teknis dari {@link #oleh}). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris templat ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan penjagaan "hanya-maju".
	 *
	 * <p>Nilai {@code null} maupun string kosong/spasi DIABAIKAN diam-diam: field lama
	 * dipertahankan. Ini pola audit yang konsisten di seluruh entity AIS &mdash; tujuannya agar
	 * jalur penyimpanan yang tidak mengetahui identitas pengguna (impor massal, penjadwal latar,
	 * pemanggilan API tanpa sesi ZK) tidak menghapus jejak audit yang sudah benar. Konsekuensinya
	 * field ini TIDAK dapat dikosongkan kembali lewat setter.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila null atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks templat untuk dropdown/label di layar konfigurasi, berbentuk
	 * {@code "<id>-<nama>-<contoh nomor>"}.
	 *
	 * <p><b>Perhatian: method ini MENJALANKAN QUERY.</b> Bagian ketiga dihasilkan dengan memanggil
	 * {@link #format(Long, Date)} memakai {@link #getMulaiUrutanKe()} sebagai angka urut dan
	 * tanggal sekarang, sehingga ikut memanggil {@code Common.getSatuanKerja()} dan
	 * {@code Common.getCurrentPejabat(...)}. Akibatnya {@code toString()} pada entity ini jauh
	 * lebih mahal daripada {@code toString()} entity biasa dan TIDAK aman dipanggil di jalur yang
	 * sesi Hibernate-nya sedang ditutup (mis. logging di dalam blok {@code finally} penutupan
	 * sesi). Berbeda dengan {@link #getContohFormat()}, di sini TIDAK ada penjaga reentransi
	 * maupun {@code try/catch} &mdash; exception akan merambat ke pemanggil.</p>
	 *
	 * <p>Perhatikan pula bahwa angka yang dipakai adalah {@code mulaiUrutanKe}, bukan
	 * {@code nomorIndex}; jadi contoh yang muncul di sini menggambarkan "nomor pertama" templat,
	 * bukan nomor berikutnya yang akan terbit.</p>
	 *
	 * @return string ringkas berisi id, nama, dan contoh nomor hasil format
	 */
	public String toString() {
		return id + "-" + nama + "-" + format(getMulaiUrutanKe(), ais.ui.util.WaktuUtil.getDate());
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan penjagaan "hanya-maju" yang sama persis
	 * dengan {@link #setOlehId(String)}: nilai null/kosong diabaikan sehingga jejak audit lama
	 * tidak terhapus oleh jalur penyimpanan tanpa konteks pengguna.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila null atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris templat ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dipicu SEBELUM setiap {@code UPDATE} baris templat; mendelegasikan
	 * ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi {@link #tanggal_dirubah}
	 * beserta {@link #oleh}/{@link #olehId} dari sesi pengguna yang sedang berjalan.
	 *
	 * <p>Perhatikan bahwa deklarasi field {@code tanggal_dirubah} ditulis pada BARIS YANG SAMA
	 * dengan method ini (gaya penulisan hasil penyisipan otomatis di seluruh entity AIS). Field
	 * tersebut diinisialisasi ke waktu sekarang lewat {@code WaktuUtil.getDate()} &mdash; bukan
	 * {@code new Date()} &mdash; agar mengikuti zona waktu/penyetelan waktu aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir secara manual.
	 *
	 * <p>Umumnya TIDAK perlu dipanggil kode aplikasi: nilainya diisi otomatis oleh
	 * {@link #onUpdate()}. Setter ini ada terutama demi kebutuhan Hibernate saat memuat baris dan
	 * demi jalur impor data yang ingin mempertahankan stempel waktu asal.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris templat (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah null untuk objek yang baru dibuat di memori
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama templat yang ditampilkan admin saat memilih penomoran untuk sebuah modul
	 * (mis. "Nomor Kas Besar", "Nomor Surat Keluar Umum"). Kolom {@code nama} bersifat
	 * {@code NOT NULL} di basis data.
	 */
	private String nama;

	/**
	 * Contoh nomor hasil format, DIHITUNG ULANG setiap kali {@link #getContohFormat()} dipanggil
	 * dan sekaligus dipersistensikan sebagai kolom biasa. Sifat ganda inilah yang membuat
	 * getter-nya perlu penjaga reentransi {@link #SEDANG_HITUNG_CONTOH_FORMAT}.
	 */
	private String contohFormat;

	/**
	 * Preferensi "urutan direset tiap pergantian tahun" (default {@code true}).
	 *
	 * <p><b>Hanya dipatuhi pada mode hitung baris.</b> Nilainya dibaca oleh {@code getindex(...)}
	 * milik Action pemakai untuk menambahkan {@code Restrictions.eq("tahun", tahunSekarang)} pada
	 * query {@code rowCount}. Pada mode counter tersimpan ({@code gunakanIndexUrut = true}) field
	 * ini tidak berpengaruh sama sekali.</p>
	 */
	private Boolean resetUrutanTiapTahun = true;

	/**
	 * Preferensi "urutan direset tiap pergantian bulan" (default {@code false}). Sama seperti
	 * {@link #resetUrutanTiapTahun}, hanya dipatuhi pada mode hitung baris, di mana ia menambahkan
	 * filter {@code tahun = ... AND bulan = ...} pada query penghitung.
	 */
	private Boolean resetUrutanTiapBulan = false;

	/**
	 * Tanggal patokan reset manual: bila diisi dan tanggalnya sudah terlewat (atau sama dengan
	 * hari ini), {@code getindex(...)} hanya menghitung dokumen dengan tanggal pembuatan
	 * &ge; tanggal ini. Dipakai admin untuk "menyetel ulang" penomoran di tengah periode tanpa
	 * menghapus data lama. Kembali: hanya berlaku pada mode hitung baris.
	 */
	private Date resetTiap;

	/**
	 * Bila {@code true}, deret urut dibagi bersama dengan seluruh templat lain yang menunjuk
	 * {@link KelompokNomorSurat} yang sama, dan offset awal diambil dari
	 * {@link KelompokNomorSurat#getMulaiUrutanKe()}. Default {@code false}.
	 */
	private Boolean urutBerdasarkanKelompok = false;

	/**
	 * Bila {@code true}, deret urut dihitung khusus untuk templat INI saja (filter
	 * {@code nomorSurat = templat ini} pada query penghitung) dan offset awal diambil dari
	 * {@link #getMulaiUrutanKe()}. Nilai awal field adalah {@code false}, namun perhatikan bahwa
	 * getter-nya {@link #getUrutBerdasarkanNomor()} mengembalikan {@code true} untuk nilai
	 * {@code null} &mdash; asimetri yang didokumentasikan pada getter tersebut.
	 */
	private Boolean urutBerdasarkanNomor = false;

	/**
	 * Offset awal deret: angka yang DITAMBAHKAN ke index sebelum di-<i>pad</i> pada slot
	 * {@link #NOMOR_URUT}, dipakai bila {@link #getUrutBerdasarkanNomor()} aktif. Default
	 * {@code 1L}, artinya dokumen pertama tercetak sebagai urutan ke-2 bila index dimulai dari 1.
	 */
	private Long mulaiUrutanKe = 1L;

	/**
	 * Lebar tetap slot {@link #NOMOR_URUT} dalam jumlah digit (default {@code 3}, mis.
	 * {@code "007"}). Lihat peringatan pemotongan angka pada
	 * {@link #getJumlahAngkaNolDiDepanNomorUrut()} bila urutan melampaui lebar ini.
	 */
	private Integer jumlahAngkaNolDiDepanNomorUrut = 3;

	/** Jenis isi slot ke-1 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom1 = KOSONG;

	/** Teks pemisah sesudah slot ke-1, atau isi teks statisnya bila {@link #kolom1} = {@link #KATA_STATIS}. */
	private String tanda1 = "/";

	/** Jenis isi slot ke-2 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom2 = KOSONG;

	/** Teks pemisah sesudah slot ke-2, atau isi teks statisnya bila {@link #kolom2} = {@link #KATA_STATIS}. */
	private String tanda2 = "/";

	/** Jenis isi slot ke-3 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom3 = KOSONG;

	/** Teks pemisah sesudah slot ke-3, atau isi teks statisnya bila {@link #kolom3} = {@link #KATA_STATIS}. */
	private String tanda3 = "/";

	/** Jenis isi slot ke-4 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom4 = KOSONG;

	/** Teks pemisah sesudah slot ke-4, atau isi teks statisnya bila {@link #kolom4} = {@link #KATA_STATIS}. */
	private String tanda4 = "/";

	/** Jenis isi slot ke-5 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom5 = KOSONG;

	/** Teks pemisah sesudah slot ke-5, atau isi teks statisnya bila {@link #kolom5} = {@link #KATA_STATIS}. */
	private String tanda5 = "/";

	/** Jenis isi slot ke-6 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom6 = KOSONG;

	/** Teks pemisah sesudah slot ke-6, atau isi teks statisnya bila {@link #kolom6} = {@link #KATA_STATIS}. */
	private String tanda6 = "/";

	/** Jenis isi slot ke-7 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom7 = KOSONG;

	/** Teks pemisah sesudah slot ke-7, atau isi teks statisnya bila {@link #kolom7} = {@link #KATA_STATIS}. */
	private String tanda7 = "/";

	/** Jenis isi slot ke-8 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom8 = KOSONG;

	/** Teks pemisah sesudah slot ke-8, atau isi teks statisnya bila {@link #kolom8} = {@link #KATA_STATIS}. */
	private String tanda8 = "/";

	/** Jenis isi slot ke-9 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom9 = KOSONG;

	/** Teks pemisah sesudah slot ke-9, atau isi teks statisnya bila {@link #kolom9} = {@link #KATA_STATIS}. */
	private String tanda9 = "/";

	/** Jenis isi slot ke-10 (salah satu konstanta jenis slot); default {@link #KOSONG}. */
	private String kolom10 = KOSONG;

	/** Teks pemisah sesudah slot ke-10, atau isi teks statisnya bila {@link #kolom10} = {@link #KATA_STATIS}. */
	private String tanda10 = "/";

	/** Catatan bebas admin mengenai peruntukan templat ini; tidak ikut membentuk nomor. */
	private String keterangan;

	/**
	 * Jurusan pemilik templat (konteks perguruan tinggi), opsional. Disimpan sebagai FK namun
	 * TIDAK dipakai sebagai filter oleh mesin penomoran mana pun &mdash; lihat catatan cakupan
	 * tenant pada {@link #getJurusan()}.
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas pemilik templat (konteks perguruan tinggi), opsional. Sama seperti
	 * {@link #jurusan}, bersifat penanda saja pada praktik pemakaian saat ini.
	 */
	private Fakultas fakultas;

	/**
	 * Kelompok penomoran yang menaungi templat ini. Bila diisi DAN
	 * {@link #getUrutBerdasarkanKelompok()} aktif, deret urut dibagi bersama seluruh templat
	 * se-kelompok dan offset awalnya diambil dari kelompok, bukan dari {@link #mulaiUrutanKe}.
	 */
	private KelompokNomorSurat kelompokNomorSurat;

	/** Sekolah pemilik templat (konteks yayasan pendidikan dasar/menengah), opsional. */
	private Sekolah sekolah;

	/** Yayasan pemilik templat, opsional. */
	private Yayasan yayasan;

	/**
	 * Satuan kerja pemilik templat, opsional. PERHATIKAN: kode substitusi {@code KODE_SATKER}/
	 * {@code UNIT}/{@code SATKER} di dalam {@link #format(Long, Date, SatuanKerja)} TIDAK dibaca
	 * dari field ini, melainkan dari satuan kerja pengguna yang sedang login
	 * ({@code Common.getSatuanKerja()}) atau dari argumen eksplisit pemanggil.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Pemilih mode penomoran. {@code true} = mode counter tersimpan (pakai {@link #nomorIndex});
	 * {@code false} (default) = mode hitung baris (pakai {@code getindex(...)} milik Action).
	 * Lihat penjelasan lengkap perbedaan kedua mode pada Javadoc kelas.
	 */
	private Boolean gunakanIndexUrut = false;

	/**
	 * Counter urut tersimpan untuk mode counter. Nilainya adalah angka yang AKAN dipakai untuk
	 * dokumen berikutnya (bukan angka terakhir yang sudah terbit). Dinaikkan secara atomik oleh
	 * {@link #ambilLaluTambahIndexNomorSurat(NomorSurat)}.
	 */
	private Long nomorIndex = 1L;

	/**
	 * Penanda aktif/nonaktif templat. Lihat {@link #getAktif()}: tidak ditemukan pemakai yang
	 * benar-benar MEMFILTER berdasarkan flag ini, sehingga praktis bersifat dokumentatif.
	 */
	private Boolean aktif;

	/**
	 * Pengelompokan templat menurut modul asal, diisi otomatis dari parameter {@code tipe} pada
	 * URL layar {@code NomorSuratAction} (default {@code "surat"}). Dipakai layar tersebut untuk
	 * menyaring daftar templat agar admin modul aset tidak melihat templat modul keuangan, dengan
	 * kriteria {@code tipe IS NULL OR tipe = <tipe layar>} sehingga templat lama bertipe null
	 * tetap muncul di semua layar.
	 */
	private String tipe;

	/**
	 * Mengambil index yang akan DIPAKAI untuk mencetak nomor surat berikutnya, sekaligus
	 * menaikkan counter {@code nomorIndex} secara ATOMIK &mdash; menutup celah TOCTOU yang ada
	 * pada pola lama "baca {@code getNomorIndex()} lalu panggil {@link #tambahIndexNomorSurat}
	 * terpisah" (dua permintaan bersamaan bisa membaca angka yang sama sebelum salah satunya
	 * sempat menaikkan, sehingga dua dokumen tercetak dengan nomor identik).
	 *
	 * <p><b>Cara kerja.</b> Memakai {@link ais.database.hibernate.KunciEntityHelper#jalankanDenganKunci}
	 * &mdash; infrastruktur kunci yang sudah dipakai modul lain di codebase ini (mis. disposisi SOP,
	 * transfer saldo koperasi) dan memang didokumentasikan untuk kasus "generate nomor urut": antrian
	 * FIFO di aplikasi (lapis 1) LALU {@code SELECT ... FOR NO KEY UPDATE NOWAIT} di database (lapis 3,
	 * dengan retry+backoff bila baris sedang dikunci proses lain). Baris {@code nomor_surat} dikunci,
	 * nilai {@code nomorIndex} SAAT INI dibaca sebagai nilai yang akan dipakai (return value), lalu
	 * langsung ditulis {@code +1} dan di-{@code commit} dalam transaksi pendek yang sama &mdash; tidak
	 * ada jendela waktu antara "baca" dan "naikkan" yang bisa diselip pemanggil lain, baik dalam satu
	 * JVM maupun lintas node (berbeda dengan {@code synchronized} pada {@link #tambahIndexNomorSurat}
	 * yang hanya berlaku se-JVM).</p>
	 *
	 * <p><b>Hanya berlaku mode index urut.</b> Bila {@code nomorSurat} null, {@code getGunakanIndexUrut()}
	 * bernilai false, atau belum tersimpan ({@code getId() == null}), method ini TIDAK mengunci apa pun
	 * dan sekadar mengembalikan {@code getNomorIndex()} (mode non-index memakai {@code getindex()} milik
	 * Action masing-masing, tidak lewat sini).</p>
	 *
	 * <p><b>Gagal-aman.</b> Bila penguncian gagal (mis. baris terkunci proses lain melebihi batas retry,
	 * atau baris sudah terhapus), kegagalan dicatat ke {@link ais.common.ErrorAuditUtil} dan method
	 * JATUH KEMBALI ke perilaku lama (baca {@code nomorIndex} di memori, naikkan tanpa kunci) alih-alih
	 * melempar exception &mdash; konsisten dengan {@link #tambahIndexNomorSurat} yang juga tidak pernah
	 * menggagalkan penyimpanan dokumen keuangan hanya karena penomoran resminya bermasalah.</p>
	 *
	 * <p><b>Dipanggil dari.</b> {@code generateCode(true)} di seluruh Action/ApiHelper penomoran
	 * keuangan (Uang Muka, Kas Besar, Kas Kecil, dst.), menggantikan pola lama
	 * "{@code getNomorIndex()} lalu {@code tambahIndexNomorSurat} terpisah", TEPAT SEBELUM
	 * {@code format(index, tanggal)} dipanggil dengan index yang dikembalikan di sini.</p>
	 *
	 * @param nomorSurat template nomor surat yang indexnya hendak dikonsumsi
	 * @return index yang harus dipakai untuk mencetak nomor kali ini (BUKAN nilai sesudah increment)
	 */
	public static Long ambilLaluTambahIndexNomorSurat(final NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		if (!nomorSurat.getGunakanIndexUrut() || nomorSurat.getId() == null) {
			return nomorSurat.getNomorIndex() == null ? 1L : nomorSurat.getNomorIndex();
		}
		final Long[] konsumsiTerkunci = new Long[1];
		try {
			boolean adaRow = ais.database.hibernate.KunciEntityHelper.jalankanDenganKunci(NomorSurat.class,
					nomorSurat.getId(), new ais.database.hibernate.KunciEntityHelper.PekerjaanTransaksi() {
						/**
						 * Badan kerja yang dijalankan SETELAH baris templat berhasil dikunci di
						 * basis data, dan sebelum transaksi pendek pengunci di-{@code commit}.
						 *
						 * <p>{@code entityTerkunci} adalah instance {@link NomorSurat} yang baru
						 * dimuat ulang DI DALAM sesi terkunci &mdash; bukan objek {@code nomorSurat}
						 * milik pemanggil yang bisa saja sudah usang (detached/basi). Karena itu
						 * nilai {@code nomorIndex} yang dibaca di sini dijamin merupakan nilai
						 * ter-commit paling mutakhir, bukan nilai yang tersimpan di cache sesi
						 * pemanggil.</p>
						 *
						 * <p>Urutannya: baca index yang akan dikonsumsi, tulis {@code +1} pada
						 * entity terkunci, daftarkan lewat {@code session.update}, lalu simpan
						 * angka yang dikonsumsi ke array penampung satu elemen
						 * {@code konsumsiTerkunci}. Array dipakai (alih-alih variabel biasa) karena
						 * kelas anonim Java 1.7 hanya boleh menangkap variabel lokal
						 * {@code final} &mdash; array final tetap memungkinkan isinya diubah,
						 * sehingga nilai dapat dikembalikan ke method pembungkus.</p>
						 *
						 * @param session        sesi Hibernate milik transaksi pengunci
						 * @param entityTerkunci baris {@link NomorSurat} yang sudah terkunci
						 * @throws Exception bila pembaruan gagal; ditangani method pembungkus yang
						 *                   kemudian jatuh ke jalur gagal-aman
						 */
						public void kerjakan(Session session, Object entityTerkunci) throws Exception {
							NomorSurat fresh = (NomorSurat) entityTerkunci;
							Long konsumsi = fresh.getNomorIndex() == null ? 1L : fresh.getNomorIndex();
							fresh.setNomorIndex(konsumsi + 1L);
							session.update(fresh);
							konsumsiTerkunci[0] = konsumsi;
						}
					});
			if (adaRow && konsumsiTerkunci[0] != null) {
				nomorSurat.setNomorIndex(konsumsiTerkunci[0] + 1L);
				return konsumsiTerkunci[0];
			}
		} catch (Exception e) {
			e.printStackTrace();
			ais.common.ErrorAuditUtil.record(e, "NomorSurat.ambilLaluTambahIndexNomorSurat");
		}
		// Gagal-aman: baris hilang atau penguncian gagal — jangan blokir penyimpanan dokumen.
		Long konsumsi = nomorSurat.getNomorIndex() == null ? 1L : nomorSurat.getNomorIndex();
		nomorSurat.setNomorIndex(konsumsi + 1L);
		return konsumsi;
	}

	/**
	 * @deprecated Sejak perbaikan celah TOCTOU (lihat {@link #ambilLaluTambahIndexNomorSurat}),
	 * pemanggil BARU sebaiknya memakai {@link #ambilLaluTambahIndexNomorSurat} yang menggabungkan
	 * baca-index dan naik-index dalam satu operasi terkunci. Method ini dipertahankan agar
	 * pemanggil lama yang belum dipindahkan tetap berfungsi; ia sendiri tidak berubah.
	 */
	@Deprecated
	public synchronized static void tambahIndexNomorSurat(NomorSurat nomorSurat) {
		if (nomorSurat != null && nomorSurat.getGunakanIndexUrut()) {
			// Selalu pakai session DEDICATED (openSession) — TIDAK memakai currentNativeSession
			// bersama yang bisa sudah ditutup / koneksinya mati (c3p0 "closed Connection")
			// saat dipanggil dari rantai onSave Disposisi SOP / Pengadaan Asset → "Session is
			// closed!". Muat ulang entity by id di session ini agar bebas objek detached.
			Session session = null;
			org.hibernate.Transaction tx = null;
			try {
				session = HibernateUtil.openSession();
				tx = session.beginTransaction();

				NomorSurat fresh = nomorSurat.getId() == null ? null
						: (NomorSurat) session.get(NomorSurat.class, nomorSurat.getId());
				if (fresh != null) {
					Long idx = fresh.getNomorIndex() == null ? 1L : fresh.getNomorIndex();
					fresh.setNomorIndex(idx + 1L);
					session.update(fresh);
					// Sinkronkan ke objek pemanggil agar nilai terbaru ikut terlihat.
					nomorSurat.setNomorIndex(fresh.getNomorIndex());
				} else {
					Long idx = nomorSurat.getNomorIndex() == null ? 1L : nomorSurat.getNomorIndex();
					nomorSurat.setNomorIndex(idx + 1L);
				}

				tx.commit();
			} catch (Exception e) {
				if (tx != null && tx.isActive()) {
					try { tx.rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:178");}
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/surat/NomorSurat.java:180");
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:183");}
					try { session.disconnect(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:184");}
					try { if (session.isOpen()) session.close(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:185");}
				}
			}
		}
	}

	/**
	 * Membentuk string nomor dokumen dari templat ini, memakai satuan kerja pengguna yang SEDANG
	 * LOGIN sebagai sumber substitusi {@code KODE_SATKER}/{@code UNIT}/{@code SATKER}.
	 *
	 * <p>Ini adalah pintu masuk yang dipakai mayoritas pemanggil: seluruh {@code generateCode()}
	 * di Action keuangan/aset/penggajian, {@code CommonAkunting.generateNoJurnal}, serta
	 * {@link #getContohFormat()} dan {@link #toString()}. Method sekadar mengambil
	 * {@code Common.getSatuanKerja()} lalu mendelegasikan ke
	 * {@link #format(Long, Date, SatuanKerja)}; seluruh logika perangkaian slot, padding, dan
	 * substitusi token dijelaskan di sana.</p>
	 *
	 * <p><b>Ketergantungan pada konteks sesi &mdash; penting.</b> Karena satuan kerja dibaca dari
	 * sesi pengguna, nomor yang dihasilkan overload ini BERGANTUNG PADA SIAPA YANG MEMANGGIL. Dua
	 * pengguna dari satuan kerja berbeda yang memakai templat yang SAMA akan memperoleh nomor
	 * dengan segmen {@code KODE_SATKER} berbeda, sementara angka urutnya tetap berasal dari satu
	 * deret yang sama (kecuali templat memakai kelompok). Untuk pemanggilan dari jalur tanpa sesi
	 * &mdash; penjadwal latar, servlet API, proses impor &mdash; {@code Common.getSatuanKerja()}
	 * dapat mengembalikan {@code null} sehingga token satuan kerja TIDAK tergantikan dan tersisa
	 * apa adanya di dalam nomor. Jalur seperti itu sebaiknya memakai overload tiga-argumen dan
	 * menyuplai satuan kerja secara eksplisit.</p>
	 *
	 * <p><b>Biaya.</b> {@code Common.getSatuanKerja()} bukan operasi murni memori: ia dapat
	 * menelusuri konfigurasi perguruan tinggi lewat query Hibernate. Karena itu method ini tidak
	 * boleh dipanggil di dalam perulangan besar tanpa <i>caching</i>, dan &mdash; sebagaimana
	 * dijelaskan pada {@link #SEDANG_HITUNG_CONTOH_FORMAT} &mdash; tidak aman dipanggil dari
	 * dalam siklus flush Hibernate.</p>
	 *
	 * @param urutanke angka urut dasar dokumen (sebelum penambahan offset {@code mulaiUrutanKe});
	 *                 boleh {@code 0L} bila templat tidak memakai slot {@link #NOMOR_URUT}
	 * @param tanggal  tanggal acuan untuk slot {@link #TANGGAL}/{@link #BULAN}/
	 *                 {@link #BULAN_ROMAWI}/{@link #TAHUN}; TIDAK boleh null
	 * @return nomor dokumen yang sudah diformat; string kosong bila semua slot {@link #KOSONG}
	 * @see #format(Long, Date, SatuanKerja)
	 */
	public String format(Long urutanke, Date tanggal) {
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		return format(urutanke, tanggal, satuanKerja);
	}

	/**
	 * Membentuk string nomor dokumen dari templat ini &mdash; INTI mesin penomoran seluruh
	 * aplikasi. Seluruh nomor dokumen resmi AIS (kas besar, kas kecil, uang muka, pemesanan
	 * pengadaan, pembayaran gaji, surat masuk/keluar, dan puluhan lainnya) pada akhirnya melewati
	 * method ini.
	 *
	 * <h3>Tahap 1 &mdash; perangkaian sepuluh slot</h3>
	 * Kesepuluh pasangan {@code (kolomN, tandaN)} disalin ke sebuah array datar dan ditelusuri
	 * dua langkah sekali putaran. Untuk setiap slot, JENIS-nya ({@code data[i]}) menentukan isi,
	 * dan {@code data[i+1]} ditempelkan sesudahnya sebagai pemisah:
	 * <ul>
	 *   <li>{@link #KOSONG} &rarr; slot dilewati SELURUHNYA lewat {@code continue}, termasuk
	 *       pemisahnya. Inilah sebabnya templat baru (semua slot default {@link #KOSONG})
	 *       menghasilkan string kosong, bukan deretan garis miring.</li>
	 *   <li>{@link #NOMOR_URUT} &rarr; lihat tahap 2 di bawah.</li>
	 *   <li>{@link #KATA_STATIS} &rarr; TIDAK ada isi tersendiri; hanya {@code tandaN} yang
	 *       ditempelkan. Di sinilah admin menuliskan teks tetap ({@code "KEU"}, {@code "/"}) dan
	 *       token substitusi ({@code KODE_SATKER}, {@code HAK_AKSES}).</li>
	 *   <li>{@link #TANGGAL} &rarr; {@code Calendar.DATE} dipadkan menjadi dua digit.</li>
	 *   <li>{@link #BULAN_ROMAWI} &rarr; {@code Common.ROMAWI[bulan+1]}. Perhatikan pemeriksaan
	 *       ini dilakukan SEBELUM {@link #BULAN}; keduanya string berbeda sehingga tidak saling
	 *       menutupi.</li>
	 *   <li>{@link #BULAN} &rarr; {@code Calendar.MONTH + 1} dipadkan menjadi dua digit.</li>
	 *   <li>{@link #TAHUN} &rarr; {@code Calendar.YEAR} apa adanya (empat digit).</li>
	 * </ul>
	 * Jenis slot yang tidak dikenali (mis. data lama dengan ejaan berbeda) jatuh ke luar seluruh
	 * cabang sehingga slot itu diabaikan tanpa error &mdash; perilaku permisif yang disengaja agar
	 * satu templat rusak tidak menggagalkan penyimpanan dokumen.
	 *
	 * <h3>Tahap 2 &mdash; angka urut, offset, dan padding</h3>
	 * Angka yang dicetak BUKAN {@code urutanke} apa adanya, melainkan {@code urutanke} ditambah
	 * offset awal yang dipilih berjenjang: bila {@link #getUrutBerdasarkanKelompok()} aktif DAN
	 * {@link #getKelompokNomorSurat()} tidak null, offset diambil dari
	 * {@link KelompokNomorSurat#getMulaiUrutanKe()}; bila tidak, dan
	 * {@link #getUrutBerdasarkanNomor()} aktif, offset diambil dari {@link #getMulaiUrutanKe()};
	 * selain itu offset {@code 0L}. Hasil penjumlahan ditempelkan di belakang untaian 72 karakter
	 * nol, lalu dipotong dari kanan sepanjang {@link #getJumlahAngkaNolDiDepanNomorUrut()} digit.
	 *
	 * <p><b>Peringatan integritas &mdash; pemotongan saat urutan melampaui lebar.</b> Karena
	 * padding dilakukan dengan {@code substring(len - lebar)}, angka yang lebih panjang daripada
	 * lebar yang dikonfigurasi akan DIPOTONG dari kiri, bukan melebar. Dengan lebar default 3,
	 * urutan ke-1000 tercetak sebagai {@code "000"}, ke-1001 sebagai {@code "001"}, dan
	 * seterusnya &mdash; yaitu MENGULANG persis nomor yang sudah pernah terbit di awal periode.
	 * Bagi templat mode counter tersimpan yang tidak pernah direset (lihat Javadoc kelas), kondisi
	 * ini pasti tercapai begitu dokumen ke-1000 terbit. Satu-satunya penahan yang tersisa adalah
	 * {@code KodeUnikUtil.pastikanUnik(...)} yang menambahkan sufiks {@code "-2"} &mdash; itu pun
	 * hanya dipakai sebagian pemanggil, dan mengubah bentuk nomor resmi. Admin WAJIB menyetel
	 * {@link #setJumlahAngkaNolDiDepanNomorUrut(Integer)} cukup lebar untuk volume dokumen yang
	 * diperkirakan.</p>
	 *
	 * <p>Selain itu, lebar {@code 0} menghasilkan slot nomor kosong, dan lebar NEGATIF membuat
	 * {@code substring} melempar {@code StringIndexOutOfBoundsException} yang merambat ke
	 * pemanggil &mdash; tidak ada validasi rentang di sini maupun di setter-nya.</p>
	 *
	 * <h3>Tahap 3 &mdash; substitusi token tekstual</h3>
	 * String hasil tahap 1&ndash;2 kemudian dilewatkan tiga blok penggantian teks yang
	 * masing-masing dibungkus {@code try/catch} sendiri (kegagalan satu blok tidak membatalkan
	 * blok lain maupun keseluruhan nomor; exception dicatat ke {@code ErrorAuditUtil}):
	 * <ol>
	 *   <li><b>Satuan kerja.</b> Bila argumen {@code satuanKerja} null, method mencoba mengisinya
	 *       dari {@code Common.getSatuanKerja()}. Bila akhirnya tersedia,
	 *       {@code KODE_SATKER} diganti kodenya, sedangkan {@code UNIT} dan {@code SATKER}
	 *       diganti NAMANYA (bukan kode). Bila satuan kerja punya induk, {@code INDUK_SATKER}
	 *       diganti nama induknya. Seluruh penggantian memakai
	 *       {@code StringUtils.replaceIgnoreCase} sehingga penulisan token tidak sensitif huruf
	 *       besar/kecil.</li>
	 *   <li><b>Kode jabatan.</b> Blok ini dijaga oleh {@code hasil.contains("KODE_JABATAN")},
	 *       menelusuri seluruh jabatan pengguna berjalan lewat
	 *       {@code Common.getCurrentPejabat(false)}. <b>CACAT YANG DIKETAHUI:</b> di dalam
	 *       perulangan, yang diganti adalah token {@code "KODE_SATKER"}, BUKAN
	 *       {@code "KODE_JABATAN"}. Akibatnya (a) token {@code KODE_JABATAN} tidak pernah
	 *       tergantikan dan tercetak apa adanya sebagai teks harfiah di dalam nomor dokumen; dan
	 *       (b) bila blok satuan kerja sebelumnya GAGAL mengganti {@code KODE_SATKER} (satuan
	 *       kerja null), sisa token {@code KODE_SATKER} justru terisi kode jabatan &mdash; nilai
	 *       dari dimensi yang sama sekali berbeda. Karena perulangan tidak {@code break}, jabatan
	 *       TERAKHIR yang berkode tidak kosonglah yang menang. Perilaku ini dibiarkan apa adanya
	 *       di sini (dokumentasi tidak mengubah kode); perbaikannya berdampak pada bentuk nomor
	 *       yang sudah terbit sehingga perlu keputusan fungsional tersendiri.</li>
	 *   <li><b>Hak akses.</b> Bila string mengandung {@code HAK_AKSES}, token diganti kode role
	 *       pengguna berjalan ({@code Tbmuser.hakAkses().getKode()}). Bila tidak ada pengguna
	 *       (jalur latar/API), token tersisa apa adanya.</li>
	 * </ol>
	 *
	 * <h3>Sifat method</h3>
	 * Method ini MURNI dalam arti tidak mengubah keadaan entity dan tidak menaikkan counter apa
	 * pun &mdash; menaikkan urutan adalah tanggung jawab
	 * {@link #ambilLaluTambahIndexNomorSurat(NomorSurat)} atau {@code getindex(...)} milik
	 * pemanggil. Namun ia TIDAK bebas efek eksternal: ia dapat menjalankan query lewat
	 * {@code Common.getSatuanKerja()}/{@code Common.getCurrentPejabat(...)}/
	 * {@code Common.getCurrentUser()}, sehingga tidak aman dipanggil dari dalam siklus flush
	 * Hibernate (lihat {@link #SEDANG_HITUNG_CONTOH_FORMAT}).
	 *
	 * @param urutanke    angka urut dasar dokumen sebelum penambahan offset; boleh {@code 0L}
	 * @param tanggal     tanggal acuan slot kalender; TIDAK boleh null ({@code Calendar.setTime}
	 *                    akan melempar {@code NullPointerException})
	 * @param satuanKerja satuan kerja untuk substitusi token; bila null, diambil dari sesi
	 *                    pengguna berjalan
	 * @return nomor dokumen yang sudah diformat, atau string kosong bila seluruh slot
	 *         {@link #KOSONG}; tidak pernah null
	 */
	public String format(Long urutanke, Date tanggal, SatuanKerja satuanKerja) {
		String hasil = "";

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);

		String[] data = new String[] { kolom1, tanda1, kolom2, tanda2, kolom3, tanda3, kolom4, tanda4, kolom5, tanda5,
				kolom6, tanda6, kolom7, tanda7, kolom8, tanda8, kolom9, tanda9, kolom10, tanda10, };

		for (int i = 0; i < data.length; i += 2) {
			if (data[i].equals(KOSONG)) {
				continue;
			} else if (data[i].equals(NOMOR_URUT)) {
				String nomor = "000000000000000000000000000000000000000000000000000000000000000000000000"
						+ (urutanke + ((getUrutBerdasarkanKelompok() && getKelompokNomorSurat() != null)
								? getKelompokNomorSurat().getMulaiUrutanKe()
								: (getUrutBerdasarkanNomor() ? getMulaiUrutanKe() : 0L)));
				nomor = nomor.substring(nomor.length() - getJumlahAngkaNolDiDepanNomorUrut());
				hasil += nomor + data[i + 1];
			} else if (data[i].equals(KATA_STATIS)) {
				hasil += data[i + 1];
			} else if (data[i].equals(TANGGAL)) {
				String tgl = "000" + (calendar.get(Calendar.DATE));
				tgl = tgl.substring(tgl.length() - 2);
				hasil += tgl + data[i + 1];
			} else if (data[i].equals(BULAN_ROMAWI)) {
				hasil += Common.ROMAWI[calendar.get(Calendar.MONTH) + 1] + data[i + 1];
			} else if (data[i].equals(BULAN)) {
				String bln = "000" + (calendar.get(Calendar.MONTH) + 1);
				bln = bln.substring(bln.length() - 2);
				hasil += bln + data[i + 1];
			} else if (data[i].equals(TAHUN)) {
				hasil += (calendar.get(Calendar.YEAR)) + data[i + 1];
			}
		}

		try {

			if (satuanKerja == null) {
				satuanKerja = Common.getSatuanKerja();
			}

			if (satuanKerja != null) {
				hasil = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(hasil, "KODE_SATKER",
						satuanKerja.getKode());
				hasil = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(hasil, "UNIT", satuanKerja.getNama());
				hasil = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(hasil, "SATKER", satuanKerja.getNama());
			}

			if (satuanKerja != null && satuanKerja.getParent() != null) {
				hasil = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(hasil, "INDUK_SATKER",
						satuanKerja.getParent().getNama());
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:250");
			// TODO: handle exception
		}

		try {
			if (hasil.contains("KODE_JABATAN")) {
				List<Pejabat> jabatans = Common.getCurrentPejabat(false);
				for (Pejabat pejabat : jabatans) {
					if (pejabat != null && pejabat.getJenisJabatan() != null
							&& pejabat.getJenisJabatan().getKode() != null
							&& !pejabat.getJenisJabatan().getKode().trim().isEmpty()) {
						hasil = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(hasil, "KODE_SATKER",
								pejabat.getJenisJabatan().getKode());
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:266");
			// TODO: handle exception
		}

		try {
			if (hasil.contains("HAK_AKSES")) {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null) {
					Tbmrole tbmrole = tbmuser.hakAkses();
					if (tbmrole != null) {
						hasil = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(hasil, "HAK_AKSES",
								tbmrole.getKode());
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/surat/NomorSurat.java:281");
			// TODO: handle exception
		}

//		System.out.println("No -> " + hasil);

		return hasil;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membentuk instance saat
	 * memuat baris dari basis data, dan dipakai layar CRUD {@code NomorSuratAction} untuk
	 * membuat templat baru. Seluruh nilai default (sepuluh slot {@link #KOSONG}, pemisah
	 * {@code "/"}, lebar tiga digit, reset tahunan aktif, mode hitung baris) berasal dari
	 * inisialisasi field, bukan dari konstruktor ini.
	 */
	public NomorSurat() {
	}

	/**
	 * Mengembalikan kunci primer baris templat.
	 *
	 * <p>Selain sebagai identitas, nilai ini menentukan apakah penguncian baris dapat dilakukan:
	 * {@link #ambilLaluTambahIndexNomorSurat(NomorSurat)} melewati seluruh mekanisme kunci bila
	 * {@code id} masih null (templat belum tersimpan).</p>
	 *
	 * @return id templat, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer. Umumnya hanya dipanggil Hibernate; pengisian manual dipakai jalur
	 * impor massal yang mempertahankan id asal.
	 *
	 * @param id kunci primer templat
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama templat, sudah dibersihkan spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan di GETTER, bukan di setter &mdash; artinya nilai yang tersimpan di
	 * memori maupun di basis data bisa saja masih mengandung spasi tepi, dan nilai yang dilihat
	 * pemanggil berbeda dari nilai mentahnya. Konsekuensi praktis: pencarian/pembandingan yang
	 * dilakukan langsung di sisi SQL (mis. {@code Restrictions.eq("nama", ...)} pada validasi
	 * duplikat) TIDAK ikut terpangkas dan bisa meleset untuk data yang terlanjur berspasi.</p>
	 *
	 * @return nama templat tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama templat. Tidak ada normalisasi maupun validasi di sini; kewajiban isi dan
	 * pemeriksaan duplikat ditegakkan di layar {@code NomorSuratAction}, sedangkan basis data
	 * hanya menjamin kolom tidak null.
	 *
	 * @param nama nama templat
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas admin mengenai templat ini (apa adanya, tanpa pemangkasan).
	 * Tidak ikut membentuk nomor dokumen.
	 *
	 * @return keterangan templat, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas admin mengenai templat ini.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-1 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda1()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-1; secara default {@link #KOSONG}
	 */
	public String getKolom1() {
		return kolom1;
	}

	/**
	 * Menetapkan JENIS isi slot ke-1. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom1 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom1(String kolom1) {
		this.kolom1 = kolom1;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-1. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom1()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-1; secara default {@code "/"}
	 */
	public String getTanda1() {
		return tanda1;
	}

	/**
	 * Menetapkan teks pendamping slot ke-1 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda1 teks pemisah atau isi statis slot ke-1
	 */
	public void setTanda1(String tanda1) {
		this.tanda1 = tanda1;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-2 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda2()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-2; secara default {@link #KOSONG}
	 */
	public String getKolom2() {
		return kolom2;
	}

	/**
	 * Menetapkan JENIS isi slot ke-2. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom2 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom2(String kolom2) {
		this.kolom2 = kolom2;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-2. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom2()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-2; secara default {@code "/"}
	 */
	public String getTanda2() {
		return tanda2;
	}

	/**
	 * Menetapkan teks pendamping slot ke-2 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda2 teks pemisah atau isi statis slot ke-2
	 */
	public void setTanda2(String tanda2) {
		this.tanda2 = tanda2;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-3 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda3()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-3; secara default {@link #KOSONG}
	 */
	public String getKolom3() {
		return kolom3;
	}

	/**
	 * Menetapkan JENIS isi slot ke-3. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom3 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom3(String kolom3) {
		this.kolom3 = kolom3;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-3. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom3()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-3; secara default {@code "/"}
	 */
	public String getTanda3() {
		return tanda3;
	}

	/**
	 * Menetapkan teks pendamping slot ke-3 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda3 teks pemisah atau isi statis slot ke-3
	 */
	public void setTanda3(String tanda3) {
		this.tanda3 = tanda3;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-4 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda4()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-4; secara default {@link #KOSONG}
	 */
	public String getKolom4() {
		return kolom4;
	}

	/**
	 * Menetapkan JENIS isi slot ke-4. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom4 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom4(String kolom4) {
		this.kolom4 = kolom4;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-4. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom4()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-4; secara default {@code "/"}
	 */
	public String getTanda4() {
		return tanda4;
	}

	/**
	 * Menetapkan teks pendamping slot ke-4 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda4 teks pemisah atau isi statis slot ke-4
	 */
	public void setTanda4(String tanda4) {
		this.tanda4 = tanda4;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-5 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda5()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-5; secara default {@link #KOSONG}
	 */
	public String getKolom5() {
		return kolom5;
	}

	/**
	 * Menetapkan JENIS isi slot ke-5. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom5 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom5(String kolom5) {
		this.kolom5 = kolom5;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-5. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom5()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-5; secara default {@code "/"}
	 */
	public String getTanda5() {
		return tanda5;
	}

	/**
	 * Menetapkan teks pendamping slot ke-5 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda5 teks pemisah atau isi statis slot ke-5
	 */
	public void setTanda5(String tanda5) {
		this.tanda5 = tanda5;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-6 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda6()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-6; secara default {@link #KOSONG}
	 */
	public String getKolom6() {
		return kolom6;
	}

	/**
	 * Menetapkan JENIS isi slot ke-6. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom6 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom6(String kolom6) {
		this.kolom6 = kolom6;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-6. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom6()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-6; secara default {@code "/"}
	 */
	public String getTanda6() {
		return tanda6;
	}

	/**
	 * Menetapkan teks pendamping slot ke-6 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda6 teks pemisah atau isi statis slot ke-6
	 */
	public void setTanda6(String tanda6) {
		this.tanda6 = tanda6;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-7 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda7()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-7; secara default {@link #KOSONG}
	 */
	public String getKolom7() {
		return kolom7;
	}

	/**
	 * Menetapkan JENIS isi slot ke-7. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom7 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom7(String kolom7) {
		this.kolom7 = kolom7;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-7. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom7()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-7; secara default {@code "/"}
	 */
	public String getTanda7() {
		return tanda7;
	}

	/**
	 * Menetapkan teks pendamping slot ke-7 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda7 teks pemisah atau isi statis slot ke-7
	 */
	public void setTanda7(String tanda7) {
		this.tanda7 = tanda7;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-8 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda8()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-8; secara default {@link #KOSONG}
	 */
	public String getKolom8() {
		return kolom8;
	}

	/**
	 * Menetapkan JENIS isi slot ke-8. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom8 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom8(String kolom8) {
		this.kolom8 = kolom8;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-8. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom8()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-8; secara default {@code "/"}
	 */
	public String getTanda8() {
		return tanda8;
	}

	/**
	 * Menetapkan teks pendamping slot ke-8 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda8 teks pemisah atau isi statis slot ke-8
	 */
	public void setTanda8(String tanda8) {
		this.tanda8 = tanda8;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-9 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda9()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-9; secara default {@link #KOSONG}
	 */
	public String getKolom9() {
		return kolom9;
	}

	/**
	 * Menetapkan JENIS isi slot ke-9. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom9 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom9(String kolom9) {
		this.kolom9 = kolom9;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-9. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom9()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-9; secara default {@code "/"}
	 */
	public String getTanda9() {
		return tanda9;
	}

	/**
	 * Menetapkan teks pendamping slot ke-9 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda9 teks pemisah atau isi statis slot ke-9
	 */
	public void setTanda9(String tanda9) {
		this.tanda9 = tanda9;
	}

	/**
	 * Mengembalikan JENIS isi slot ke-10 dari templat nomor: salah satu konstanta
	 * {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TANGGAL},
	 * {@link #BULAN}, {@link #BULAN_ROMAWI}, atau {@link #TAHUN}. Dibaca
	 * {@link #format(Long, Date, SatuanKerja)} bersama pasangannya {@link #getTanda10()}.
	 * Nilai yang tidak dikenali membuat slot ini dilewati tanpa error.
	 *
	 * @return jenis isi slot ke-10; secara default {@link #KOSONG}
	 */
	public String getKolom10() {
		return kolom10;
	}

	/**
	 * Menetapkan JENIS isi slot ke-10. Tidak ada validasi bahwa nilai merupakan salah satu
	 * konstanta jenis slot yang dikenal &mdash; nilai asing diterima dan berakibat slot
	 * diabaikan saat pemformatan.
	 *
	 * @param kolom10 jenis isi slot, sebaiknya salah satu konstanta jenis slot kelas ini
	 */
	public void setKolom10(String kolom10) {
		this.kolom10 = kolom10;
	}

	/**
	 * Mengembalikan teks pendamping slot ke-10. Perannya BERGANTUNG pada jenis slot: bila
	 * {@link #getKolom10()} bernilai {@link #KATA_STATIS}, nilai inilah yang menjadi ISI slot
	 * (tempat admin menuliskan teks tetap atau token substitusi seperti {@code KODE_SATKER});
	 * untuk jenis slot lain, nilai ini ditempelkan SESUDAH isi slot sebagai pemisah. Bila
	 * jenis slot {@link #KOSONG}, nilai ini tidak ikut dirangkai sama sekali.
	 *
	 * @return teks pemisah/isi statis slot ke-10; secara default {@code "/"}
	 */
	public String getTanda10() {
		return tanda10;
	}

	/**
	 * Menetapkan teks pendamping slot ke-10 (pemisah, atau isi statis bila jenis slot
	 * {@link #KATA_STATIS}). Nilai disimpan apa adanya: spasi tepi TIDAK dipangkas dan akan
	 * ikut tercetak di dalam nomor dokumen.
	 *
	 * @param tanda10 teks pemisah atau isi statis slot ke-10
	 */
	public void setTanda10(String tanda10) {
		this.tanda10 = tanda10;
	}

	/**
	 * Mengembalikan offset awal deret milik templat ini, dengan penormalan ke {@code 1L} bila
	 * nilainya null.
	 *
	 * <p><b>Getter ini MENULIS field.</b> Ketika {@code mulaiUrutanKe} null, getter tidak sekadar
	 * mengembalikan {@code 1L} melainkan juga MENUGASKAN {@code 1L} ke field. Pada entity terkelola
	 * Hibernate, penugasan semacam ini membuat baris ikut ter-<i>dirty</i> dan tertulis ke basis
	 * data pada flush berikutnya walau pemanggil hanya bermaksud membaca. Pola "getter destruktif"
	 * ini berulang di banyak entity AIS; di sini dampaknya jinak (nilai yang ditulis sama dengan
	 * default yang memang diinginkan), namun perlu diketahui saat menelusuri mengapa sebuah baris
	 * templat ikut ter-update tanpa perubahan dari layar.</p>
	 *
	 * <p>Angka ini DITAMBAHKAN ke index dokumen saat slot {@link #NOMOR_URUT} dicetak, dan hanya
	 * bila {@link #getUrutBerdasarkanNomor()} aktif sementara jalur kelompok tidak dipilih. Karena
	 * ia bersifat penambah &mdash; bukan "mulai dari" secara harfiah &mdash; nilai default
	 * {@code 1L} menggeser seluruh deret satu angka ke atas.</p>
	 *
	 * @return offset awal deret; tidak pernah null
	 */
	public Long getMulaiUrutanKe() {
		if (mulaiUrutanKe == null) {
			mulaiUrutanKe = 1L;
		}
		return mulaiUrutanKe;
	}

	/**
	 * Menetapkan offset awal deret templat ini.
	 *
	 * <p>Mengubah nilai ini menggeser SELURUH nomor yang akan terbit berikutnya, sehingga dapat
	 * menabrak nomor yang sudah pernah dipakai bila digeser mundur. Tidak ada validasi terhadap
	 * nilai negatif maupun terhadap nomor yang telah terbit.</p>
	 *
	 * @param mulaiUrutanKe offset awal deret; null akan dinormalkan menjadi {@code 1L} saat dibaca
	 */
	public void setMulaiUrutanKe(Long mulaiUrutanKe) {
		this.mulaiUrutanKe = mulaiUrutanKe;
	}

	/**
	 * Mengembalikan preferensi "reset urutan tiap tahun", dengan default {@code true} bila null.
	 *
	 * <p><b>Getter destruktif:</b> sama seperti {@link #getMulaiUrutanKe()}, nilai null tidak hanya
	 * digantikan pada nilai balik tetapi juga DITULIS ke field.</p>
	 *
	 * <p><b>Cakupan keberlakuan &mdash; penting.</b> Kelas ini sendiri TIDAK PERNAH membaca flag
	 * ini. Satu-satunya pemakainya adalah method {@code getindex(NomorSurat)} yang disalin ke
	 * hampir setiap Action pemakai ({@code KasBesarAction}, {@code KasKecilAction},
	 * {@code UangMukaAction}, {@code CommonAkunting}, Action aset, dan seterusnya), di mana ia
	 * menambahkan {@code Restrictions.eq("tahun", tahunSekarang)} pada query penghitung baris.
	 * Karena {@code getindex(...)} HANYA dipanggil pada mode hitung baris, templat yang memakai
	 * mode counter tersimpan ({@link #getGunakanIndexUrut()} = true) sama sekali tidak terpengaruh:
	 * {@code nomorIndex} akan terus naik melewati pergantian tahun meski kotak centang "reset tiap
	 * tahun" tampak aktif di layar konfigurasi. Ini menyesatkan secara operasional dan patut
	 * diperhatikan saat menelusuri keluhan "nomor tidak kembali ke 1 di bulan Januari".</p>
	 *
	 * <p>Perhatikan pula bahwa filter tahun dievaluasi terhadap kolom {@code tahun} pada tabel
	 * DOKUMEN, bukan pada templat ini; ketepatan reset karena itu bergantung pada benarnya kolom
	 * {@code tahun}/{@code bulan} di entity dokumen yang bersangkutan.</p>
	 *
	 * @return {@code true} bila urutan direset tiap tahun (pada mode hitung baris); tidak pernah null
	 */
	public Boolean getResetUrutanTiapTahun() {
		if (resetUrutanTiapTahun == null) {
			resetUrutanTiapTahun = true;
		}
		return resetUrutanTiapTahun;
	}

	/**
	 * Menetapkan preferensi reset urutan tahunan. Ingat bahwa flag ini hanya berpengaruh pada mode
	 * hitung baris; lihat {@link #getResetUrutanTiapTahun()}.
	 *
	 * @param resetUrutanTiapTahun {@code true} untuk mereset deret tiap pergantian tahun
	 */
	public void setResetUrutanTiapTahun(Boolean resetUrutanTiapTahun) {
		this.resetUrutanTiapTahun = resetUrutanTiapTahun;
	}

	/**
	 * Penjaga reentransi PER-THREAD untuk {@link #getContohFormat()}.
	 *
	 * <p><b>Akar masalah.</b> {@code contohFormat} adalah properti yang IKUT DIBACA Hibernate
	 * saat flush ({@code AbstractEntityTuplizer.getPropertyValues}), padahal getter-nya
	 * menjalankan query: {@code format(...)} -&gt; {@code Common.getSatuanKerja()} -&gt;
	 * {@code PerguruanTinggiUtil.getPerguruanTinggi()} -&gt; {@code createCriteria}. Query itu
	 * memicu AUTO-FLUSH, dan auto-flush membaca properti lagi -&gt; getContohFormat terpanggil
	 * berulang berlapis-lapis. Bila flush terjadi saat commit/Envers, sesi sudah dalam proses
	 * penutupan sehingga muncul {@code SessionException: Session is closed!}.</p>
	 *
	 * <p><b>Perbaikan.</b> Saat penjaga aktif (artinya kita sedang berada di dalam perhitungan
	 * atau di dalam flush yang dipicu perhitungan itu), kembalikan nilai tersimpan tanpa query.
	 * Perilaku pemanggilan normal dari layar/laporan TIDAK berubah: perhitungan tetap jalan.</p>
	 */
	private static final ThreadLocal<Boolean> SEDANG_HITUNG_CONTOH_FORMAT = new ThreadLocal<Boolean>();

	/**
	 * Mengembalikan contoh nomor hasil templat ini, DIHITUNG ULANG setiap pemanggilan.
	 *
	 * <p><b>Getter destruktif &amp; ber-efek samping.</b> Method ini bukan pembaca pasif: ia
	 * memanggil {@link #format(Long, Date)} (yang dapat menjalankan query), lalu MENUGASKAN hasilnya
	 * ke field {@code contohFormat} yang juga merupakan kolom terpetakan. Konsekuensinya setiap
	 * pembacaan berpotensi membuat baris ter-<i>dirty</i> dan tertulis ke basis data.</p>
	 *
	 * <p><b>Angka yang dipakai.</b> Pada mode counter tersimpan, contoh dibentuk dari
	 * {@link #getNomorIndex()} &mdash; yakni nomor yang AKAN terbit berikutnya, sehingga contoh di
	 * layar benar-benar mewakili nomor berikutnya. Pada mode hitung baris dipakai {@code 0L},
	 * karena angka sesungguhnya baru diketahui saat query penghitung dijalankan Action pemakai;
	 * contoh yang muncul karenanya hanya menggambarkan BENTUK, bukan angka sebenarnya.</p>
	 *
	 * <p><b>Penjaga reentransi.</b> Bila penjaga {@code SEDANG_HITUNG_CONTOH_FORMAT} sedang aktif
	 * pada thread ini, method langsung mengembalikan nilai tersimpan tanpa menghitung ulang &mdash;
	 * memutus rantai rekursi "hitung &rarr; query &rarr; auto-flush &rarr; baca properti &rarr;
	 * hitung" yang dijelaskan pada Javadoc field penjaga tersebut. Penjaga dibersihkan pada blok
	 * {@code finally} dengan {@code remove()} (bukan {@code set(false)}) agar {@code ThreadLocal}
	 * tidak menahan entri pada thread pool ZK/Tomcat.</p>
	 *
	 * <p><b>Gagal-aman.</b> Seluruh perhitungan dibungkus penangkap {@link Throwable} &mdash;
	 * bukan sekadar {@code Exception} &mdash; agar kegagalan menghitung contoh TIDAK pernah
	 * menggagalkan flush/commit dokumen yang sedang disimpan; nilai contoh terakhir dipakai apa
	 * adanya. Pencatatan ke {@code ErrorAuditUtil} disaring
	 * {@link #merupakanSesiTertutup(Throwable)}: kegagalan "session is closed" adalah kondisi WAJAR
	 * pada jalur flush dan sengaja tidak dicatat supaya Error Log tidak dibanjiri ratusan entri
	 * identik; penyebab lain tetap dicatat. Pencatatan itu sendiri masih dibungkus
	 * {@code try/catch} kosong agar kegagalan mencatat pun tidak merambat.</p>
	 *
	 * @return contoh nomor hasil format; dapat berupa nilai tersimpan sebelumnya bila perhitungan
	 *         gagal atau penjaga reentransi sedang aktif
	 */
	public String getContohFormat() {
		if (Boolean.TRUE.equals(SEDANG_HITUNG_CONTOH_FORMAT.get())) {
			return contohFormat;
		}
		SEDANG_HITUNG_CONTOH_FORMAT.set(Boolean.TRUE);
		try {
			contohFormat = getGunakanIndexUrut() ? format(getNomorIndex(), ais.ui.util.WaktuUtil.getDate())
					: format(0L, ais.ui.util.WaktuUtil.getDate());
		} catch (Throwable t) {
			/* Sesi sudah tertutup / DB tak tersedia (mis. dibaca saat commit di thread latar):
			 * JANGAN gagalkan flush & commit hanya karena contoh format tak bisa dihitung ulang.
			 * Nilai contoh terakhir yang tersimpan dipakai apa adanya. */
			/* Sesi yang sudah/sedang ditutup saat commit-Envers adalah kondisi WAJAR pada jalur
			 * ini (getter dibaca ulang oleh flush), bukan kesalahan yang perlu ditindaklanjuti.
			 * Mencatatnya membanjiri Error Log dgn ratusan entri "Session is closed!" yang
			 * identik. Catat HANYA penyebab lain supaya masalah nyata tetap terlihat. */
			try {
				if (!merupakanSesiTertutup(t)) {
					ais.common.ErrorAuditUtil.record(t, "NomorSurat.getContohFormat");
				}
			} catch (Throwable abaikan) {
			}
		} finally {
			SEDANG_HITUNG_CONTOH_FORMAT.remove();
		}
		return contohFormat;
	}

	/**
	 * Apakah kegagalan berasal dari sesi/koneksi Hibernate yang sudah ditutup? Dipakai untuk
	 * membedakan kondisi wajar (getter dibaca ulang saat flush/commit di thread latar)
	 * dari kesalahan nyata yang perlu dicatat ke Error Log.
	 */
	private static boolean merupakanSesiTertutup(Throwable t) {
		Throwable c = t;
		int penjaga = 0;
		while (c != null && penjaga < 30) {
			String pesan = c.getMessage() == null ? "" : c.getMessage().toLowerCase();
			if (c instanceof org.hibernate.SessionException || pesan.indexOf("session is closed") >= 0
					|| pesan.indexOf("has been closed") >= 0 || pesan.indexOf("already closed") >= 0
					|| pesan.indexOf("connection is closed") >= 0) {
				return true;
			}
			c = c.getCause();
			penjaga++;
		}
		return false;
	}

	/**
	 * Menetapkan contoh nomor secara langsung. Nilai yang disetel bersifat sementara: pemanggilan
	 * {@link #getContohFormat()} berikutnya akan menghitung ulang dan menimpanya, kecuali
	 * perhitungan gagal atau penjaga reentransi sedang aktif.
	 *
	 * @param contohFormat contoh nomor hasil format
	 */
	public void setContohFormat(String contohFormat) {
		this.contohFormat = contohFormat;
	}

	/**
	 * Mengembalikan lebar tetap slot {@link #NOMOR_URUT} dalam jumlah digit, dengan default
	 * {@code 3} bila null (getter destruktif: nilai default juga ditulis ke field).
	 *
	 * <p><b>Konsekuensi integritas.</b> Nilai ini bukan sekadar lebar tampilan minimum melainkan
	 * lebar TETAP: {@link #format(Long, Date, SatuanKerja)} memotong angka dari kiri bila urutan
	 * melampauinya. Dengan nilai 3, dokumen ke-1000 dan seterusnya akan mengulang nomor yang sudah
	 * terbit. Setel cukup lebar untuk perkiraan volume dokumen sepanjang satu periode reset &mdash;
	 * dan untuk templat mode counter tersimpan yang tidak pernah direset, sepanjang umur templat.</p>
	 *
	 * @return jumlah digit slot nomor urut; tidak pernah null
	 */
	public Integer getJumlahAngkaNolDiDepanNomorUrut() {
		if (jumlahAngkaNolDiDepanNomorUrut == null) {
			jumlahAngkaNolDiDepanNomorUrut = 3;
		}
		return jumlahAngkaNolDiDepanNomorUrut;
	}

	/**
	 * Menetapkan lebar tetap slot {@link #NOMOR_URUT}.
	 *
	 * <p>TIDAK ada validasi rentang di sini. Nilai {@code 0} menghasilkan slot nomor kosong, dan
	 * nilai NEGATIF membuat {@code substring} di dalam {@link #format(Long, Date, SatuanKerja)}
	 * melempar {@code StringIndexOutOfBoundsException} saat nomor dibentuk. Nilai yang sangat besar
	 * (di atas panjang untaian nol pembantu, yakni 72 ditambah panjang angka) juga menyebabkan
	 * exception yang sama.</p>
	 *
	 * @param jumlahAngkaNolDiDepanNomorUrut lebar slot nomor urut dalam digit; sebaiknya bernilai
	 *                                       positif dan wajar
	 */
	public void setJumlahAngkaNolDiDepanNomorUrut(Integer jumlahAngkaNolDiDepanNomorUrut) {
		this.jumlahAngkaNolDiDepanNomorUrut = jumlahAngkaNolDiDepanNomorUrut;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	/**
	 * Mengembalikan jurusan pemilik templat.
	 *
	 * <p>Melewatkan nilai melalui {@code check(...)} milik {@link GeneralValueObject} &mdash; helper
	 * bersama yang menetralkan proxy Hibernate yang tak lagi dapat diinisialisasi (sesi sudah
	 * ditutup) menjadi {@code null} alih-alih melempar {@code LazyInitializationException}. Karena
	 * hasil {@code check} ditugaskan kembali ke field, getter ini termasuk pola getter destruktif
	 * yang lazim di entity AIS.</p>
	 *
	 * <p><b>Catatan cakupan.</b> Field ini murni penanda kepemilikan pada layar konfigurasi; tidak
	 * ditemukan jalur penomoran mana pun yang MEMFILTER templat berdasarkan jurusan pengguna, jadi
	 * jangan mengandalkannya sebagai pembatas tenant.</p>
	 *
	 * @return jurusan pemilik, atau {@code null} bila tidak diisi atau proxy tak dapat dimuat
	 */
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan jurusan pemilik templat.
	 *
	 * @param jurusan jurusan pemilik; boleh null
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	/**
	 * Mengembalikan fakultas pemilik templat, dengan penetralan proxy lewat {@code check(...)}
	 * seperti pada {@link #getJurusan()}. Sama seperti jurusan, bersifat penanda dan bukan
	 * pembatas tenant pada jalur penomoran.
	 *
	 * @return fakultas pemilik, atau {@code null}
	 */
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan fakultas pemilik templat.
	 *
	 * @param fakultas fakultas pemilik; boleh null
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_nomor_surat", nullable = true)
	/**
	 * Mengembalikan kelompok penomoran yang menaungi templat ini, dengan penetralan proxy lewat
	 * {@code check(...)}.
	 *
	 * <p>Nilai ini menjadi bermakna hanya bila {@link #getUrutBerdasarkanKelompok()} aktif. Dalam
	 * kondisi tersebut ia berperan pada DUA tempat yang berbeda:</p>
	 * <ol>
	 *   <li>Di {@link #format(Long, Date, SatuanKerja)}, offset awal deret diambil dari
	 *       {@link KelompokNomorSurat#getMulaiUrutanKe()} &mdash; menggantikan
	 *       {@link #getMulaiUrutanKe()} milik templat sendiri.</li>
	 *   <li>Di {@code getindex(...)} milik Action pemakai, query penghitung difilter
	 *       {@code nomorSurat.kelompokNomorSurat = kelompok ini} alih-alih
	 *       {@code nomorSurat = templat ini}, sehingga SELURUH templat se-kelompok berbagi satu
	 *       deret bersama. Inilah cara sebuah unit/departemen memakai beberapa bentuk nomor yang
	 *       berbeda namun tetap berurutan dalam satu buku agenda.</li>
	 * </ol>
	 *
	 * @return kelompok penomoran, atau {@code null} bila templat berdiri sendiri
	 */
	public KelompokNomorSurat getKelompokNomorSurat() {
		kelompokNomorSurat = check(kelompokNomorSurat);
		return kelompokNomorSurat;
	}

	/**
	 * Menetapkan kelompok penomoran penaung templat ini. Ingat bahwa penetapan saja tidak cukup:
	 * agar deret benar-benar dibagi bersama, {@link #setUrutBerdasarkanKelompok(Boolean)} harus
	 * diaktifkan.
	 *
	 * @param kelompokNomorSurat kelompok penomoran; boleh null
	 */
	public void setKelompokNomorSurat(KelompokNomorSurat kelompokNomorSurat) {
		this.kelompokNomorSurat = kelompokNomorSurat;
	}

	/**
	 * Menyatakan apakah deret urut dibagi bersama seluruh templat se-{@link KelompokNomorSurat}.
	 *
	 * <p>Berbeda dengan {@link #getMulaiUrutanKe()}, getter ini TIDAK destruktif: nilai null
	 * dipetakan ke {@code false} pada nilai balik saja, field dibiarkan apa adanya.</p>
	 *
	 * <p>Flag ini menang atas {@link #getUrutBerdasarkanNomor()} pada tempat yang memakainya, namun
	 * HANYA bila {@link #getKelompokNomorSurat()} juga terisi; bila kelompok kosong, jalur kelompok
	 * diabaikan dan sistem jatuh ke perilaku per-templat.</p>
	 *
	 * @return {@code true} bila deret mengikuti kelompok; tidak pernah null
	 */
	public Boolean getUrutBerdasarkanKelompok() {
		return urutBerdasarkanKelompok == null ? false : urutBerdasarkanKelompok;
	}

	/**
	 * Menetapkan apakah deret urut dibagi bersama se-kelompok.
	 *
	 * @param urutBerdasarkanKelompok {@code true} untuk berbagi deret dengan kelompok
	 */
	public void setUrutBerdasarkanKelompok(Boolean urutBerdasarkanKelompok) {
		this.urutBerdasarkanKelompok = urutBerdasarkanKelompok;
	}

	/**
	 * Menyatakan apakah deret urut dihitung khusus untuk templat INI saja.
	 *
	 * <p><b>Asimetri default yang perlu diketahui.</b> Field {@code urutBerdasarkanNomor}
	 * diinisialisasi {@code false} untuk objek baru, tetapi getter ini memetakan null ke
	 * {@code true}. Artinya baris LAMA di basis data yang kolomnya masih {@code NULL} akan
	 * berperilaku "urut per templat", sedangkan templat yang baru dibuat lewat layar berperilaku
	 * sebaliknya sampai admin mencentangnya. Kedua kelompok data karena itu dapat menghasilkan
	 * deret yang berbeda meski konfigurasi terlihat sama di layar.</p>
	 *
	 * <p>Pada {@code getindex(...)} milik Action pemakai, flag ini dievaluasi PERTAMA: bila aktif,
	 * query penghitung difilter {@code nomorSurat = templat ini} dan jalur kelompok tidak pernah
	 * dipertimbangkan. Bila tidak aktif dan kelompok juga tidak terpakai, query berjalan TANPA
	 * filter templat sama sekali &mdash; menghitung seluruh baris tabel dokumen pada periode
	 * berjalan, sehingga seluruh templat modul itu berbagi satu deret global.</p>
	 *
	 * @return {@code true} bila deret dihitung per templat; tidak pernah null
	 */
	public Boolean getUrutBerdasarkanNomor() {
		return urutBerdasarkanNomor == null ? true : urutBerdasarkanNomor;
	}

	/**
	 * Menetapkan apakah deret urut dihitung khusus untuk templat ini.
	 *
	 * @param urutBerdasarkanNomor {@code true} untuk deret per templat
	 */
	public void setUrutBerdasarkanNomor(Boolean urutBerdasarkanNomor) {
		this.urutBerdasarkanNomor = urutBerdasarkanNomor;
	}

	/**
	 * Menyatakan apakah urutan direset tiap pergantian bulan (default {@code false} bila null;
	 * getter tidak destruktif).
	 *
	 * <p>Seperti {@link #getResetUrutanTiapTahun()}, flag ini hanya dipatuhi pada mode hitung baris
	 * dan diterapkan di {@code getindex(...)} milik Action pemakai sebagai filter gabungan
	 * {@code tahun = ... AND bulan = ...}. Perhatikan bahwa filter tahun dan filter bulan
	 * ditambahkan sebagai DUA kriteria terpisah yang saling menumpuk, sehingga mengaktifkan reset
	 * bulanan sementara reset tahunan juga aktif tidak menimbulkan konflik &mdash; keduanya
	 * menyaring tahun yang sama.</p>
	 *
	 * @return {@code true} bila urutan direset tiap bulan (pada mode hitung baris); tidak pernah null
	 */
	public Boolean getResetUrutanTiapBulan() {
		return resetUrutanTiapBulan == null ? false : resetUrutanTiapBulan;
	}

	/**
	 * Menetapkan preferensi reset urutan bulanan; hanya berpengaruh pada mode hitung baris.
	 *
	 * @param resetUrutanTiapBulan {@code true} untuk mereset deret tiap pergantian bulan
	 */
	public void setResetUrutanTiapBulan(Boolean resetUrutanTiapBulan) {
		this.resetUrutanTiapBulan = resetUrutanTiapBulan;
	}

	@Temporal(TemporalType.DATE)
	/**
	 * Mengembalikan tanggal patokan reset manual deret (presisi {@code DATE}, tanpa jam).
	 *
	 * <p>Dipakai {@code getindex(...)} milik Action pemakai: bila tanggal ini terisi DAN sudah
	 * terlewat atau sama dengan hari ini (dibandingkan lewat pemformatan ke pola tanggal, sehingga
	 * komponen jam tidak mengganggu), query penghitung hanya mencacah dokumen dengan tanggal
	 * pembuatan &ge; tanggal ini. Efeknya deret "dimulai ulang" pada tanggal tersebut tanpa perlu
	 * menghapus dokumen lama. Bila tanggal masih di masa depan, filter tidak diterapkan sehingga
	 * penomoran berjalan seperti biasa sampai tanggal itu tiba.</p>
	 *
	 * <p>Sama seperti kedua flag reset lainnya, tidak berpengaruh pada mode counter tersimpan.</p>
	 *
	 * @return tanggal patokan reset, atau {@code null} bila tidak dipakai
	 */
	public Date getResetTiap() {
		return resetTiap;
	}

	/**
	 * Menetapkan tanggal patokan reset manual deret.
	 *
	 * <p>Menggeser tanggal ini MUNDUR memperbesar cakupan hitungan (nomor melompat naik), sedangkan
	 * menggesernya MAJU mengecilkan cakupan sehingga nomor dapat mundur ke angka yang sudah pernah
	 * terbit. Tidak ada penjagaan terhadap hal itu di lapisan entity.</p>
	 *
	 * @param resetTiap tanggal patokan reset; null untuk menonaktifkan
	 */
	public void setResetTiap(Date resetTiap) {
		this.resetTiap = resetTiap;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	/**
	 * Mengembalikan satuan kerja pemilik templat, dengan penetralan proxy lewat {@code check(...)}.
	 *
	 * <p><b>Jangan tertukar.</b> Nilai ini TIDAK dipakai sebagai sumber substitusi token
	 * {@code KODE_SATKER}/{@code UNIT}/{@code SATKER} di {@link #format(Long, Date, SatuanKerja)};
	 * substitusi di sana memakai satuan kerja pengguna yang sedang login atau argumen eksplisit
	 * pemanggil. Field ini juga tidak dipakai memfilter templat mana yang boleh dipilih pengguna
	 * suatu satuan kerja &mdash; pembatasan semacam itu, bila diinginkan, harus ditegakkan di
	 * lapisan layar/Action.</p>
	 *
	 * @return satuan kerja pemilik, atau {@code null}
	 */
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik templat.
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh null
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	/**
	 * Mengembalikan sekolah pemilik templat, dengan penetralan proxy lewat {@code check(...)}.
	 *
	 * @return sekolah pemilik, atau {@code null}
	 */
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik templat, dengan penormalan objek "kosong".
	 *
	 * <p>Objek yang null ATAU yang id-nya masih null disimpan sebagai {@code null}. Normalisasi ini
	 * mencegah masalah khas jalur ZK: komponen pemilih kerap menyerahkan instance kosong hasil
	 * {@code new Sekolah()} ketika pengguna tidak memilih apa pun, dan menyimpan objek transient
	 * seperti itu akan memicu Hibernate mencoba menyisipkan baris sekolah baru melalui
	 * {@code CascadeType.PERSIST}. Pola yang sama diterapkan pada {@link #setYayasan(Yayasan)},
	 * namun TIDAK pada {@link #setJurusan(Jurusan)}, {@link #setFakultas(Fakultas)},
	 * {@link #setSatuanKerja(SatuanKerja)}, maupun
	 * {@link #setKelompokNomorSurat(KelompokNomorSurat)} &mdash; ketidakseragaman yang perlu
	 * diingat saat menelusuri kemunculan baris master kosong.</p>
	 *
	 * @param sekolah sekolah pemilik; objek null atau ber-id null disimpan sebagai null
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	/**
	 * Mengembalikan yayasan pemilik templat, dengan penetralan proxy lewat {@code check(...)}.
	 *
	 * @return yayasan pemilik, atau {@code null}
	 */
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik templat, dengan penormalan objek "kosong" yang sama persis dengan
	 * {@link #setSekolah(Sekolah)}: objek null atau yang id-nya masih null disimpan sebagai
	 * {@code null} agar tidak memicu penyisipan baris yayasan baru lewat cascade.
	 *
	 * @param yayasan yayasan pemilik; objek null atau ber-id null disimpan sebagai null
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Menyatakan mode penomoran yang dipakai templat ini &mdash; SAKELAR PALING MENENTUKAN pada
	 * entity ini.
	 *
	 * <p>{@code true} berarti mode counter tersimpan: angka urut diambil dan dinaikkan pada kolom
	 * {@link #getNomorIndex()} lewat {@link #ambilLaluTambahIndexNomorSurat(NomorSurat)}. Deret
	 * bersifat monotonik (menghapus dokumen tidak membuat nomor mundur), tetapi ketiga preferensi
	 * reset periode TIDAK berlaku sama sekali.</p>
	 *
	 * <p>{@code false} (nilai default dan hasil pemetaan null) berarti mode hitung baris: angka
	 * urut dihitung Action pemakai sebagai {@code rowCount(dokumen) + 1} dengan filter periode.
	 * Preferensi reset berlaku, tetapi deret TIDAK monotonik &mdash; penghapusan atau
	 * penonaktifan dokumen membuat hitungan turun dan nomor berikutnya mengulang angka lama.</p>
	 *
	 * <p>Karena itu memindahkan sebuah templat yang sudah berjalan dari satu mode ke mode lain
	 * bukan perubahan kosmetik: {@code nomorIndex} tidak otomatis disinkronkan dengan jumlah
	 * dokumen yang sudah terbit, sehingga peralihan dapat langsung menghasilkan nomor ganda atau
	 * lompatan besar.</p>
	 *
	 * @return {@code true} bila memakai counter tersimpan; tidak pernah null
	 */
	public Boolean getGunakanIndexUrut() {
		return gunakanIndexUrut == null ? false : gunakanIndexUrut;
	}

	/**
	 * Menetapkan mode penomoran templat. Lihat {@link #getGunakanIndexUrut()} untuk konsekuensi
	 * peralihan mode pada templat yang sudah dipakai.
	 *
	 * @param gunakanIndexUrut {@code true} untuk mode counter tersimpan
	 */
	public void setGunakanIndexUrut(Boolean gunakanIndexUrut) {
		this.gunakanIndexUrut = gunakanIndexUrut;
	}

	/**
	 * Mengembalikan angka urut BERIKUTNYA yang akan dipakai pada mode counter tersimpan (default
	 * {@code 1L} bila null; getter tidak destruktif &mdash; field dibiarkan apa adanya).
	 *
	 * <p>Semantiknya adalah "nomor yang akan datang", bukan "nomor terakhir yang terbit":
	 * {@link #ambilLaluTambahIndexNomorSurat(NomorSurat)} mengembalikan nilai ini APA ADANYA
	 * sebagai angka yang dipakai, lalu menyimpan {@code nilai + 1}. Pembacaan langsung getter ini
	 * di luar mekanisme tersebut &mdash; pola lama "baca lalu naikkan terpisah" yang masih dipakai
	 * sejumlah pemanggil melalui {@link #tambahIndexNomorSurat(NomorSurat)} &mdash; TIDAK aman
	 * terhadap permintaan bersamaan.</p>
	 *
	 * <p>Nilai ini tidak pernah direset otomatis oleh mekanisme apa pun di dalam kelas ini; reset
	 * hanya dapat dilakukan admin secara manual lewat layar konfigurasi.</p>
	 *
	 * @return angka urut berikutnya; tidak pernah null
	 */
	public Long getNomorIndex() {
		return nomorIndex == null ? 1L : nomorIndex;
	}

	/**
	 * Menetapkan angka urut berikutnya pada mode counter tersimpan.
	 *
	 * <p>Dipanggil dari jalur penomoran terkunci maupun dari layar konfigurasi. Menurunkan nilainya
	 * secara manual akan membuat nomor yang sudah terbit diterbitkan ulang; tidak ada penjagaan
	 * terhadap hal itu di lapisan entity.</p>
	 *
	 * @param nomorIndex angka urut berikutnya
	 */
	public void setNomorIndex(Long nomorIndex) {
		this.nomorIndex = nomorIndex;
	}

	/**
	 * Menyatakan status aktif templat (default {@code true} bila null; getter tidak destruktif).
	 *
	 * <p><b>Flag tidur.</b> Penelusuran pemakaian tidak menemukan satu pun jalur penomoran atau
	 * layar pemilih templat yang MEMFILTER berdasarkan flag ini &mdash; templat yang ditandai tidak
	 * aktif tetap dapat dipilih dan tetap menerbitkan nomor. Perlakukan nilainya sebagai catatan
	 * administratif belaka sampai ada penegakan yang benar-benar ditambahkan. Pola "flag aktif yang
	 * tidak ditegakkan" ini berulang di sejumlah entity master AIS.</p>
	 *
	 * @return {@code true} bila templat ditandai aktif; tidak pernah null
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif templat; lihat catatan pada {@link #getAktif()} bahwa nilai ini belum
	 * ditegakkan di jalur mana pun.
	 *
	 * @param aktif status aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan pengelompokan templat menurut modul asal (apa adanya, tanpa pemangkasan).
	 *
	 * <p>Nilainya diisi otomatis {@code NomorSuratAction} dari parameter {@code tipe} pada URL layar
	 * (default {@code "surat"} bila parameter tidak diberikan), lalu dipakai layar yang sama untuk
	 * menyaring daftar templat dengan kriteria {@code tipe IS NULL OR tipe = <tipe layar>}. Klausa
	 * {@code IS NULL} membuat templat warisan yang belum bertipe tetap terlihat di SEMUA layar
	 * &mdash; disengaja demi kompatibilitas data lama, tetapi berarti pemisahan antarmodul di sini
	 * bersifat kosmetik dan bukan kontrol akses. Nilai {@code tipe} tidak berpengaruh sama sekali
	 * pada bentuk nomor yang dihasilkan.</p>
	 *
	 * @return tipe/pengelompokan modul templat, atau {@code null} untuk data warisan
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Menetapkan pengelompokan templat menurut modul asal. Umumnya tidak dipanggil kode aplikasi
	 * selain {@code NomorSuratAction} yang mengisinya dari parameter layar.
	 *
	 * @param tipe tipe/pengelompokan modul
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

}
