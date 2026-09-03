package ais.database.model.koperasi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

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
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.security.PasswordHashService;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.VOSiswa;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

/**
 * Entity <b>anggota koperasi</b> -- representasi satu individu (mahasiswa, siswa, dosen, guru,
 * pegawai, atau pengguna umum) yang terdaftar sebagai <i>member</i> pada sebuah unit usaha
 * {@link Koperasi}. Kelas ini adalah simpul <b>paling sentral</b> dari seluruh domain
 * koperasi/kantin/eBisnis pada aplikasi ini: hampir semua dokumen transaksional di paket
 * {@code ais.database.model.koperasi} menyimpan foreign key ke baris tabel
 * {@code koperasi.anggota_koperasi}.
 *
 * <h3>Kedudukan dalam model data</h3>
 * <p>Kelas ini meng-<i>extend</i> {@link ais.database.model.VOSiswa} (yang pada gilirannya
 * meng-extend {@code GeneralValueObject}), sehingga mewarisi kontrak "kelas les" dan helper
 * {@code check(...)} untuk resolusi proxy lazy Hibernate. Konsekuensi penting dari pewarisan ini
 * dijelaskan pada bagian <i>Getter yang tidak murni</i> di bawah.</p>
 *
 * <p>Entity ini berperan sebagai <b>jembatan identitas</b>: satu baris anggota boleh menunjuk ke
 * salah satu (atau beberapa) sumber identitas orang di modul lain --
 * {@link ais.database.model.Mahasiswa}, {@link ais.database.model.Dosen},
 * {@link ais.database.model.sekolah.Guru}, {@link ais.database.model.sekolah.Siswa},
 * {@link ais.database.model.sekolah.CalonSiswa}, {@link ais.database.model.Pegawai}, dan
 * {@link ais.database.model.Tbmuser}. Semua relasi tersebut {@code nullable}, dan urutan
 * prioritasnya menentukan bagaimana {@link #getNama()}, {@link #getKodeIdentitas()},
 * {@link #getJenisIdentitasAnggotaKoperasi()}, {@link #getTipeAnggotaKoperasi()}, dan
 * {@link #getSatuanKerja()} menurunkan nilainya secara otomatis.</p>
 *
 * <h3>Dua dimensi klasifikasi yang saling bebas</h3>
 * <ul>
 * <li>{@link JenisAnggotaKoperasi} -- dimensi <b>keanggotaan/paket</b> (mis. Biasa, Luar Biasa,
 * paket khusus dengan kewajiban belanja rutin). Menentukan prefix kode member dan aturan
 * {@code wajib_belanja_rutin}/{@code wajib_pin}.</li>
 * <li>{@link TipeAnggotaKoperasi} -- dimensi <b>peran/asal orang</b> (Mahasiswa, Dosen, Siswa,
 * Guru, Pegawai, Umum). Menentukan batas {@code maksimalBolehUtang} yang dipakai sebagai gerbang
 * checkout kantin.</li>
 * </ul>
 * <p>Keduanya independen: satu orang bertipe Mahasiswa dapat berjenis paket apa pun.</p>
 *
 * <h3>Tenant: dua tingkat ketidaklangsungan</h3>
 * <p>Berbeda dengan kebanyakan entity di paket ini, {@code AnggotaKoperasi} menyimpan
 * <b>dua</b> pengait tenant sekaligus:</p>
 * <ol>
 * <li>{@link #getKoperasi()} -- FK ke unit usaha koperasi. Ini adalah tenant "sebenarnya" untuk
 * domain koperasi, tetapi {@code nullable} dan tidak pernah dipaksa terisi oleh entity ini.</li>
 * <li>{@link #getSatuanKerja()} -- FK <b>langsung</b> ke {@link ais.database.model.rab.SatuanKerja}
 * (tenant akuntansi/anggaran). Berbeda dari {@link Koperasi} yang hanya bisa dicapai lewat dua
 * tingkat ketidaklangsungan, di sini satuan kerja tersedia langsung sebagai kolom, walau nilainya
 * <i>diturunkan</i> dari relasi pegawai/dosen/guru/tbmuser bila belum diisi.</li>
 * </ol>
 * <p><b>Peringatan operasional:</b> karena kedua kolom {@code nullable}, penyaringan data per
 * tenant sepenuhnya menjadi tanggung jawab lapisan pemanggil. Sejumlah jalur pencarian anggota
 * (pencarian member POS, daftar member desktop, serta lookup by-NIM/NIDN/NIP) diketahui membaca
 * seluruh tabel tanpa membatasi {@code koperasi} maupun {@code satuanKerja}. Jangan berasumsi
 * bahwa objek yang diterima dari sebuah pencarian pasti milik tenant yang sedang aktif -- lakukan
 * verifikasi ulang sebelum memakainya pada dokumen keuangan.</p>
 *
 * <h3>Saldo dan hutang: dihitung <i>on-the-fly</i>, bukan snapshot</h3>
 * <p>Entity ini <b>tidak menyimpan kolom saldo, deposit, hutang, maupun piutang</b>. Satu-satunya
 * angka finansial yang benar-benar tersimpan di sini adalah {@link #getLimitKredit()} (plafon,
 * bukan realisasi). Seluruh posisi keuangan anggota dihitung ulang setiap kali dibutuhkan dengan
 * meng-agregasi tabel transaksi:</p>
 * <ul>
 * <li><b>Saldo deposit/voucher</b> -- dijumlahkan dari {@code Deposit} dan {@code PencairanDiskon}
 * lalu dikurangi realisasi belanja pada {@code koperasi.pembelian_anggota_koperasi}, dengan hasil
 * akhir di-<i>clamp</i> agar tidak negatif.</li>
 * <li><b>Hutang berjalan</b> -- SUM slot cara bayar yang bertanda "masuk sebagai hutang" pada
 * pembelian, dikurangi SUM {@link PembayaranHutang}.</li>
 * <li><b>Simpanan/pinjaman (USPK)</b> -- ditelusuri baris per baris dari {@link TransaksiKoperasi}
 * dan {@link TransaksiKoperasiDetail} (angsuran pinjaman) per tipe produk.</li>
 * </ul>
 * <p>Pola ini konsisten dengan konvensi "dihitung on-the-fly" yang dipakai di domain lain pada
 * aplikasi ini. Implikasinya: angka saldo <b>selalu</b> mengikuti mutasi terbaru dan tidak bisa
 * "basi", tetapi biaya bacanya mahal (beberapa <i>round-trip</i> SQL per anggota per pemanggilan),
 * dan tidak ada satu pun kolom di entity ini yang boleh diperlakukan sebagai kas anggota.
 * {@link PenyesuaianSaldoAnggota} pun tidak menimpa saldo, melainkan mencatat selisih
 * sistem-vs-fisik sebagai bukti beku lalu menerbitkan satu baris {@code Deposit} koreksi.</p>
 *
 * <h3>Getter yang tidak murni (destruktif terhadap state)</h3>
 * <p>Sejumlah besar getter pada kelas ini <b>menulis balik ke field</b> saat dipanggil, dan karena
 * objeknya berada dalam <i>persistence context</i> Hibernate, perubahan tersebut ikut ter-flush ke
 * basis data pada akhir transaksi walaupun pemanggil hanya bermaksud membaca. Getter dengan
 * perilaku ini: {@link #getKode()}, {@link #getNama()}, {@link #getKodeIdentitas()},
 * {@link #getJenisAnggotaKoperasi()}, {@link #getJenisIdentitasAnggotaKoperasi()},
 * {@link #getTipeAnggotaKoperasi()}, {@link #getAktif()}, {@link #getTanggal()},
 * {@link #getSatuanKerja()}, {@link #getKelasLesDipilih()}, serta seluruh getter relasi yang
 * memanggil {@code check(...)}. Perlakukan pemanggilannya sebagai operasi yang berpotensi
 * mengubah data, bukan sekadar pembacaan.</p>
 *
 * <p>{@link #getKoperasi()} sengaja dikecualikan dari pola tersebut dan dibuat murni; alasannya
 * terdokumentasi pada getter-nya (lookup di dalam getter dapat memicu flush berulang). Fallback ke
 * koperasi aktif dipisahkan ke helper eksplisit {@link #ambilKoperasiAtauDefault()}.</p>
 *
 * <h3>Audit</h3>
 * <p>Kelas ditandai {@link org.hibernate.envers.Audited}, sehingga setiap revisi baris disalin ke
 * tabel {@code new_audit.anggota_koperasi__audit}. Field {@link #getOleh()}/{@link #getOlehId()}
 * dan {@link #getTanggal_dirubah()} adalah <b>bayangan audit</b> (<i>audit shadow</i>) -- sebuah
 * KEHARUSAN TEKNIS agar identitas pengubah ikut tersalin ke tabel revisi, bukan duplikasi data
 * yang keliru. Kolom {@code pin_hash}/{@code pin_salt}/{@code pin_iterations} sengaja
 * {@link org.hibernate.envers.NotAudited} agar material kredensial tidak berlipat ganda di tabel
 * revisi.</p>
 *
 * <h3>Generic CRUD v2</h3>
 * <p>Entity ini <b>tidak terdaftar</b> pada registry Generic CRUD v2, baik lewat pendaftaran
 * statis maupun auto-register dari JSP. Akses lewat jalur generik akan ditolak sebagai
 * "entity tidak terdaftar", dan seluruh operasi CRUD anggota berjalan melalui action/API khusus.
 * Perlu dicatat bahwa binding scope otomatis pada adapter generik mengenal kunci
 * {@code satuanKerja} tetapi <b>tidak mengenal kunci {@code koperasi}</b>; sehingga seandainya
 * entity ini kelak didaftarkan, tenant koperasi tidak akan ikut ter-scope secara otomatis dan
 * whitelist-nya harus ditulis eksplisit.</p>
 *
 * <h3>Entity yang merujuk balik ke sini</h3>
 * <p>Di dalam paket koperasi: {@link TransaksiKoperasi}, {@link PembelianAnggotaKoperasi},
 * {@link DraftPembelianAnggotaKoperasi}, {@link PembayaranAnggotaKoperasi},
 * {@link PembayaranHutang}, {@link PembatalanTransaksiKantin}, {@link ShuAnggota},
 * {@link PencairanDiskon}, {@link PenyesuaianSaldoAnggota},
 * {@link PengajuanLimitTransaksiMember}, {@link PenerimaanPiutangCustomer},
 * {@link PiutangCustomerDoc}, {@link HargaJualCustomer}, {@link SpjSalesNota},
 * {@link SalesOrderLapangan}, {@link KodePembayaranOnline}, {@link CalonAnggotaKoperasi}, dan
 * {@link CustomerInventoryProfile} (perluasan 1:1 untuk varian eBisnis Inventory &amp; Sales).
 * Di luar paket koperasi: {@code Deposit}, {@code VirtualAccountBank}, {@code Tbmuser},
 * {@code inventory.Pembelian}, {@code inventory.DraftPembelian}, dan
 * {@code inventory.ReturPenjualan}.</p>
 *
 * @see Koperasi
 * @see JenisAnggotaKoperasi
 * @see TipeAnggotaKoperasi
 * @see CustomerInventoryProfile
 * @see ais.database.model.VOSiswa
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "anggota_koperasi")
public class AnggotaKoperasi extends VOSiswa {

	/**
	 * Versi serialisasi Java. Nilai dikunci eksplisit agar objek anggota yang sudah tersimpan pada
	 * sesi HTTP atau cache terdistribusi tetap dapat dibaca setelah kelas ini dikompilasi ulang
	 * (mis. setelah penambahan Javadoc atau field baru).
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key {@code koperasi.anggota_koperasi.id}, IDENTITY yang dibangkitkan basis data. */
	private Long id;

	/**
	 * Bayangan audit: nama pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} pada {@code @PreUpdate}. Ini KEHARUSAN TEKNIS Envers --
	 * tanpa kolom ini, tabel revisi tidak memuat identitas pengubah -- bukan duplikasi data.
	 */
	private String oleh;

	/**
	 * Bayangan audit pendamping {@link #oleh}: id pengguna pengubah terakhir, disimpan terpisah agar
	 * jejak audit tetap dapat ditelusuri walau nama pengguna kelak berubah.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris anggota ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Setter ini <b>menolak nilai kosong secara diam-diam</b>:
	 * {@code null} maupun string yang hanya berisi spasi diabaikan sehingga nilai lama dipertahankan.
	 * Perilaku ini disengaja agar jejak audit yang sudah benar tidak terhapus oleh proses yang
	 * kebetulan menyalin objek tanpa membawa konteks pengguna.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan agar bayangan audit yang sudah terisi tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris anggota ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum baris anggota di-UPDATE. Mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} yang mengisi {@link #oleh}, {@link #olehId}, dan
	 * {@link #tanggal_dirubah} dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Perhatikan bahwa kait ini hanya berjalan pada UPDATE, bukan INSERT; nilai awal
	 * {@code tanggal_dirubah} karena itu diinisialisasi langsung pada deklarasi field.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Bayangan audit: stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek
	 * dibuat, lalu diperbarui otomatis oleh {@link #onUpdate()} pada setiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya tidak dipanggil manual -- nilainya diisi oleh
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris anggota ini, dengan presisi TIMESTAMP.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks anggota dalam format {@code "<kode> - <nama>"}, dipakai pada combo/lookup ZK
	 * dan pada log.
	 *
	 * <p><b>Awas -- operasi ini mahal dan tidak murni.</b> Method memanggil {@link #getNama()} yang
	 * pada gilirannya me-resolve <i>enam</i> relasi lazy (mahasiswa, dosen, pegawai, tbmuser, guru,
	 * siswa) lewat {@code check(...)}; masing-masing dapat memicu inisialisasi proxy atau bahkan
	 * membuka sesi Hibernate baru untuk memuat objek yang sudah <i>detached</i>. Selain itu
	 * {@link #getNama()} menulis balik ke field {@link #nama}. Jadi sekadar mencetak satu objek
	 * anggota ke log dapat menghasilkan rantai query dan menandai objek sebagai kotor. Hindari
	 * memanggilnya di dalam perulangan panjang.</p>
	 *
	 * @return gabungan kode member dan nama anggota
	 */
	public String toString() {
		String nama = getNama();
		return kode + " - " + nama;
	}

	/**
	 * Nomor identitas anggota pada institusi asal (NIM/NIDN/NUPTK/NIS/kode pegawai). Diturunkan
	 * otomatis oleh {@link #getKodeIdentitas()} bila salah satu relasi orang terisi.
	 */
	private String kodeIdentitas;

	/**
	 * Label bebas jenis identitas dalam bentuk teks. Berbeda dari relasi terstruktur
	 * {@link #jenisIdentitasAnggotaKoperasi}; field teks ini hanya deskriptif dan tidak dipakai
	 * sebagai kunci logika.
	 */
	private String jenisIdentitas;

	/**
	 * Kode member koperasi. Dipetakan sebagai kolom UNIQUE dan dipakai sebagai identitas yang
	 * dipindai barcode di kasir. Diisi otomatis oleh {@link #getKode()} bila masih kosong.
	 */
	private String kode;

	/**
	 * Nama anggota. Bersifat <i>cache</i> lokal: {@link #getNama()} selalu menimpanya dari relasi
	 * orang bila salah satu relasi terisi, sehingga nilai yang di-set manual hanya bertahan untuk
	 * anggota umum yang tidak terkait entity orang mana pun.
	 */
	private String nama;

	/** Alamat surat anggota; kolom bertipe {@code text} sehingga tidak dibatasi panjang. */
	private String alamat;

	/**
	 * Unit usaha koperasi tempat anggota terdaftar -- pengait tenant utama domain koperasi.
	 * {@code nullable}: banyak anggota historis dibuat sebelum kolom ini ada.
	 */
	private Koperasi koperasi;

	/** Nama pengguna untuk portal anggota, bila anggota diberi akses login mandiri. */
	private String userid;

	/** Kata sandi portal anggota. Disimpan apa adanya pada kolom; jangan tampilkan ke antarmuka. */
	private String pass;

	/**
	 * Tanggal kedaluwarsa keanggotaan. Bila terlewati, {@link #getAktif()} akan memaksa status
	 * menjadi tidak aktif -- lihat catatan perilaku destruktif pada getter tersebut.
	 */
	private Date tanggalKadaluarsa;

	/** Relasi ke data mahasiswa, bila anggota berasal dari sivitas perguruan tinggi. */
	private Mahasiswa mahasiswa;

	/** Relasi ke data dosen, bila anggota adalah tenaga pendidik perguruan tinggi. */
	private Dosen dosen;

	/** Relasi ke data guru sekolah. */
	private Guru guru;

	/** Relasi ke data siswa sekolah. */
	private Siswa siswa;

	/** Relasi ke data calon siswa (pendaftar yang belum menjadi siswa penuh). */
	private CalonSiswa calonSiswa;

	/** Relasi ke data pegawai/karyawan. */
	private Pegawai pegawai;

	/** Relasi ke akun pengguna aplikasi, bila anggota juga merupakan pengguna sistem. */
	private Tbmuser tbmuser;

	/**
	 * Satuan kerja (tenant akuntansi/anggaran) pemilik anggota. Tersedia sebagai kolom
	 * <b>langsung</b> di entity ini, namun nilainya diturunkan dari relasi orang oleh
	 * {@link #getSatuanKerja()} bila belum diisi.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Dimensi klasifikasi <b>keanggotaan/paket</b>. Menentukan prefix kode member serta aturan
	 * kewajiban belanja rutin dan kewajiban PIN.
	 */
	private JenisAnggotaKoperasi jenisAnggotaKoperasi;

	/** Label tipe anggota dalam bentuk teks bebas; padanan terstrukturnya {@link #tipeAnggotaKoperasi}. */
	private String tipe;

	/** Catatan bebas mengenai anggota. */
	private String keterangan;

	/** Nomor telepon tetap anggota. */
	private String telp;

	/** Nomor telepon seluler anggota sebagaimana dientri pengguna (belum dinormalisasi). */
	private String hp;

	/**
	 * Bentuk kanonik {@link #hp} (format {@code 62xxxxxxxxxx}) yang dipakai sebagai identitas unik
	 * member pada alur POS. Normalisasi dilakukan di lapisan layanan, bukan di entity ini.
	 */
	private String nomorHpNormalisasi;

	/** Alamat surel anggota; dipetakan ke kolom {@code email_nasabah}. */
	private String email;

	/**
	 * Dimensi klasifikasi <b>jenis dokumen identitas</b> (NIM/NIDN/NIS/NUPTK/KTP). Diturunkan
	 * otomatis dari relasi orang oleh {@link #getJenisIdentitasAnggotaKoperasi()}.
	 */
	private JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi;

	/**
	 * Dimensi klasifikasi <b>peran/asal orang</b> (Mahasiswa, Dosen, Siswa, Guru, Pegawai, Umum).
	 * Menentukan batas maksimal berutang yang dipakai sebagai gerbang checkout kantin.
	 */
	private TipeAnggotaKoperasi tipeAnggotaKoperasi;

	/**
	 * Penanda keanggotaan aktif; default {@code true}. Bersifat <b>dua arah</b>: dapat dimatikan
	 * manual oleh admin, dan juga dimatikan otomatis oleh {@link #getAktif()} ketika
	 * {@link #tanggalKadaluarsa} terlewati, maupun oleh penjadwal pelanggaran belanja rutin ketika
	 * {@link #jumlahPeringatan} melampaui batas.
	 */
	private Boolean aktif = true;

	/** Tanggal pendaftaran anggota; diinisialisasi ke tanggal server saat objek dibuat. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/** Pengguna yang mendaftarkan anggota ini. Berbeda dari {@link #oleh} yang mencatat pengubah terakhir. */
	private Tbmuser dibuatOleh;

	/** Berkas pengajuan calon anggota yang menjadi asal-usul baris anggota ini, bila ada. */
	private CalonAnggotaKoperasi calonAnggotaKoperasi;

	/** Tanggal anggota berhenti/keluar; {@code null} berarti masih tercatat sebagai anggota. */
	private Date tanggalBerhenti;

	/** Alasan anggota berhenti/keluar. */
	private String alasanBerhenti;

	/**
	 * Penanda anggota sebagai <b>pihak terkait</b> (pengurus/pengawas), yang memperketat Batas
	 * Maksimum Pemberian Pinjaman menjadi 10% Modal Sendiri alih-alih 15%.
	 */
	private Boolean pihakTerkait;

	/** Daftar id kelas les yang dipilih, disimpan sebagai deretan angka berpemisah koma. */
	private String kelasLesDipilih;

	/**
	 * Akumulasi pelanggaran target belanja rutin. Dinaikkan oleh penjadwal mingguan dan dibandingkan
	 * dengan {@code maksimal_pelanggaran} pada {@link JenisAnggotaKoperasi} untuk menghanguskan
	 * keanggotaan.
	 */
	private Integer jumlahPeringatan;

	/**
	 * Jumlah peringatan pelanggaran target belanja rutin yang sudah dikumpulkan anggota ini.
	 *
	 * <p><b>Null-safe:</b> getter mengembalikan {@code 0} bila kolom belum pernah diisi, sehingga
	 * pemanggil dapat langsung membandingkannya secara numerik tanpa memeriksa {@code null}.
	 * Perhatikan bahwa normalisasi ini <i>tidak</i> ditulis balik ke field -- getter ini termasuk
	 * yang murni, berbeda dari kebanyakan getter lain di kelas ini.</p>
	 *
	 * <p>Nilai dinaikkan oleh proses terjadwal, bukan oleh entity ini. Ambang penghangusan
	 * keanggotaan berasal dari {@link JenisAnggotaKoperasi}, sehingga satu anggota yang berpindah
	 * paket akan dinilai dengan ambang baru sementara akumulasi peringatannya tetap terbawa.</p>
	 *
	 * <p>Mengembalikan jumlah peringatan anggota, minimal {@code 0}.</p>
	 *
	 * <p>Catatan rancangan asli yang dipertahankan apa adanya:</p>
	 *
	 * 2. Scheduler / Cron Job (Logika Pengecekan)** Untuk memunculkan *alert* dan
	 * menonaktifkan member, Anda membutuhkan *Scheduler* (tugas yang berjalan
	 * otomatis di *background* server, misalnya setiap hari **Minggu jam 23:59
	 * malam**). Logika kerjanya kira-kira seperti ini:
	 * 
	 * 1. Ambil semua `JenisAnggotaKoperasi` yang `wajib_belanja_rutin = true`. 2.
	 * Ambil semua `AnggotaKoperasi` yang masuk dalam jenis tersebut dan statusnya
	 * masih `aktif = true`. 3. Hitung jumlah transaksi `Pembelian` anggota tersebut
	 * dari Senin s/d Sabtu minggu ini. 4. **Jika (Jumlah Belanja <
	 * `target_frekuensi_belanja`)**: Tambahkan `jumlah_peringatan` si Anggota + 1.
	 * Kirim Alert/Notifikasi "Anda belum memenuhi target belanja minggu ini." 5.
	 * **Jika (`jumlah_peringatan` >= `maksimal_pelanggaran`)**: Set `aktif = false`
	 * pada Anggota tersebut (Hangus).
	 */
	@Column(name = "jumlah_peringatan")
	public Integer getJumlahPeringatan() {
		return jumlahPeringatan == null ? 0 : jumlahPeringatan;
	}

	/**
	 * Mengisi akumulasi peringatan pelanggaran belanja rutin.
	 *
	 * @param jumlahPeringatan jumlah peringatan baru; {@code null} diperbolehkan dan akan dibaca
	 *                         sebagai {@code 0} oleh {@link #getJumlahPeringatan()}
	 */
	public void setJumlahPeringatan(Integer jumlahPeringatan) {
		this.jumlahPeringatan = jumlahPeringatan;
	}

	/**
	 * Plafon kredit anggota. Lihat {@link #getLimitKredit()} untuk penjelasan lengkap mengenai
	 * ruang lingkup pemakaiannya -- khususnya bahwa nilai ini bersifat informatif bagi pelaporan dan
	 * <b>bukan</b> gerbang penolakan transaksi di kasir.
	 *
	 * <p>Keterangan asli yang dipertahankan:</p>
	 *
	 * Plafon / batas kredit (piutang maksimum) anggota untuk belanja tempo di kantin/koperasi.
	 * Dipakai laporan "Limit & Sisa Kredit Pelanggan" (gaya Accurate). Kolom auto terbentuk saat
	 * RESTART (hbm2ddl=update). DIAUDIT (Envers) -- kolom tabel audit ditambah via InitIndex.java.
	 */
	private Double limitKredit = 0.0;

	/**
	 * Mengembalikan plafon kredit (batas piutang maksimum) anggota untuk belanja tempo, dalam satuan
	 * mata uang penuh.
	 *
	 * <p><b>Null-safe.</b> Nilai {@code null} pada kolom -- kondisi normal bagi seluruh anggota yang
	 * dibuat sebelum kolom {@code limit_kredit} ditambahkan -- dinormalkan menjadi {@code 0.0}.
	 * Normalisasi ini tidak ditulis balik ke field, sehingga getter bersifat murni. Perhatikan bahwa
	 * {@code 0.0} karena itu bermakna ganda: bisa berarti "belum pernah diatur" atau "sengaja
	 * diberi plafon nol". Kedua kondisi tersebut tidak dapat dibedakan lagi setelah normalisasi, dan
	 * pemanggil yang perlu membedakannya harus membaca kolom melalui jalur lain.</p>
	 *
	 * <h4>Siapa yang memakai nilai ini</h4>
	 * <p>Pemakaian nyata plafon ini <b>terbatas pada pelaporan dan pertukaran data master</b>:</p>
	 * <ul>
	 * <li>Laporan "Limit &amp; Sisa Kredit Pelanggan" bergaya Accurate. Sisa kredit di sana dihitung
	 * seluruhnya di dalam SQL sebagai {@code coalesce(limit_kredit,0) - coalesce(piutang,0)}, dengan
	 * komponen piutang di-agregasi dari sumber kasbon. Tidak ada method Java pendamping semacam
	 * {@code getSisaLimitKredit()} pada entity ini -- sisa kredit bukan properti anggota, melainkan
	 * hasil hitung laporan.</li>
	 * <li>Modul Sales &amp; Inventory, yang memperlakukan {@code AnggotaKoperasi} sebagai
	 * "customer" dan memetakan kolom ini ke konsep <i>plafon piutang</i> pada profil customer
	 * (bandingkan {@link CustomerInventoryProfile}).</li>
	 * </ul>
	 *
	 * <h4>Batas penting: bukan gerbang transaksi</h4>
	 * <p>Alur checkout kantin <b>tidak</b> membaca {@code limitKredit} sama sekali. Penolakan belanja
	 * tempo didasarkan pada batas {@code maksimalBolehUtang} milik {@link TipeAnggotaKoperasi},
	 * dibandingkan terhadap hutang berjalan yang dihitung on-the-fly. Akibatnya seorang anggota dapat
	 * melewati plafon kredit pribadinya tanpa ditolak sistem, selama batas tipe anggotanya belum
	 * terlampaui. Jangan menyimpulkan dari keberadaan field ini bahwa ada penegakan plafon per
	 * anggota; bila penegakan semacam itu dibutuhkan, ia harus ditambahkan secara eksplisit di
	 * lapisan layanan.</p>
	 *
	 * <p>Kolom terbentuk otomatis saat aplikasi dimulai ulang melalui {@code hbm2ddl=update}, dan
	 * kolom padanannya pada tabel revisi Envers ditambahkan lewat {@code InitIndex}.</p>
	 *
	 * @return plafon kredit anggota, {@code 0.0} bila belum diatur
	 */
	@Column(name = "limit_kredit")
	public Double getLimitKredit() {
		return limitKredit == null ? 0.0 : limitKredit;
	}

	/**
	 * Mengisi plafon kredit anggota.
	 *
	 * @param limitKredit plafon baru dalam satuan mata uang penuh; {@code null} akan dibaca sebagai
	 *                    {@code 0.0} oleh {@link #getLimitKredit()}
	 */
	public void setLimitKredit(Double limitKredit) {
		this.limitKredit = limitKredit;
	}

	/**
	 * PIN transaksi anggota (dientri pembeli di Layar Pelanggan / layar kedua POS saat
	 * {@link JenisAnggotaKoperasi#getWajibPin()} aktif). Verifikasi dilakukan
	 * SERVER-SIDE memakai PBKDF2+salt (plaintext lama hanya fallback migrasi) -- nilai PIN TIDAK PERNAH
	 * dikirim kembali ke browser. Tidak ada PIN bawaan bersama: setiap member wajib diatur
	 * secara eksplisit oleh admin. DIAUDIT (Envers) -- riwayat perubahan PIN anggota perlu terlacak;
	 * kolom tabel audit ditambah via InitIndex.java (ALTER new_audit.anggota_koperasi__audit).
	 */
	private String pin;
	private String pinHash;
	private String pinSalt;
	private Integer pinIterations;

	@Column(name = "pin")
	public String getPin() {
		return pin == null || pin.trim().length() == 0 ? null : pin.trim();
	}

	public void setPin(String pin) {
		this.pin = pin == null || pin.trim().length() == 0 ? null : pin.trim();
	}

	@NotAudited
	@Column(name = "pin_hash", length = 128)
	public String getPinHash() { return pinHash; }
	public void setPinHash(String pinHash) { this.pinHash = pinHash; }

	@NotAudited
	@Column(name = "pin_salt", length = 128)
	public String getPinSalt() { return pinSalt; }
	public void setPinSalt(String pinSalt) { this.pinSalt = pinSalt; }

	@NotAudited
	@Column(name = "pin_iterations")
	public Integer getPinIterations() { return pinIterations; }
	public void setPinIterations(Integer pinIterations) { this.pinIterations = pinIterations; }

	/** Menyimpan PIN baru sebagai PBKDF2+salt dan menghapus plaintext warisan. */
	@Transient
	public void aturPinAman(String nilai) {
		if (nilai == null || !nilai.matches("[0-9]{4,8}"))
			throw new IllegalArgumentException("PIN wajib terdiri dari 4 sampai 8 angka");
		String[] hashSalt = PasswordHashService.hash(nilai);
		pinHash = hashSalt[0]; pinSalt = hashSalt[1];
		pinIterations = Integer.valueOf(PasswordHashService.ITERASI); pin = null;
	}

	/** Verifikasi constant-time; plaintext lama hanya fallback selama masa migrasi. */
	@Transient
	public boolean verifikasiPin(String nilai) {
		if (pinHash != null && pinSalt != null)
			return PasswordHashService.verify(nilai, pinHash, pinSalt, pinIterations);
		return nilai != null && getPin() != null && getPin().equals(nilai);
	}

	@Transient
	public boolean getPinSudahDiatur() {
		return (pinHash != null && pinSalt != null) || getPin() != null;
	}

	/**
	 * Method untuk menghasilkan Kode Member Koperasi berdasarkan rumus standar.
	 * Menjalankan kueri langsung ke database menggunakan parameter Session.
	 *
	 * @param session       Sesi Hibernate yang sedang aktif untuk mengeksekusi
	 *                      kueri.
	 * @param tanggalDaftar Waktu pendaftaran (jika null, sistem menggunakan waktu
	 *                      saat ini).
	 * @return String Kode Member Koperasi yang sudah diformat.
	 */
	public String generateKodeMember(Session session, Date tanggalDaftar) {
		try {
			// 1. Dapatkan Prefix dan ID dari relasi JenisAnggotaKoperasi
			String prefix = "MEM"; // Prefix bawaan jika data jenis kosong
			Long idJenis = null;

			if (this.getJenisAnggotaKoperasi() != null) {
				idJenis = this.getJenisAnggotaKoperasi().getId();
				if (this.getJenisAnggotaKoperasi().getKode() != null
						&& !this.getJenisAnggotaKoperasi().getKode().trim().isEmpty()) {
					prefix = this.getJenisAnggotaKoperasi().getKode().trim().toUpperCase();
				}
			}

			// 2. Dapatkan komponen Waktu (Bulan & Tahun)
			Calendar cal = Calendar.getInstance();
			if (tanggalDaftar != null) {
				cal.setTime(tanggalDaftar);
			}

			int bulan = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH dimulai dari angka 0
			int tahun = cal.get(Calendar.YEAR);

			// Format bulan menjadi 2 digit (contoh: 02, 08, 10, dst)
			String strBulan = String.format("%02d", bulan);

			// 3. Kueri Hitung Urutan Keseluruhan (Global)
			long urutanGlobal = 1;
			try {
				Number countGlobal = (Number) session.createSQLQuery("SELECT COUNT(a.id) FROM koperasi.anggota_koperasi a")
						.uniqueResult();
				if (countGlobal != null) {
					urutanGlobal = countGlobal.longValue() + 1;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/koperasi/AnggotaKoperasi.java:240");
			}

			// 4. Kueri Hitung Urutan Per Jenis Anggota
			long urutanPerJenis = 1;
			if (idJenis != null) {
				try {
					// FIX QuerySyntaxException "koperasi.anggota_koperasi is not mapped": HQL wajib memakai
					// nama ENTITY Hibernate ("AnggotaKoperasi"), bukan nama tabel/skema fisik
					// ("koperasi.anggota_koperasi") seperti pada SQL native di atas -- query lama SELALU
					// gagal (tertangkap try/catch di bawah, urutanPerJenis diam-diam tetap 1), sehingga
					// nomor urut per jenis anggota koperasi tidak pernah benar-benar bertambah.
					Number countJenis = (Number) session.createQuery(
							"SELECT COUNT(a.id) FROM AnggotaKoperasi a WHERE a.jenisAnggotaKoperasi.id = :idJenis")
							.setParameter("idJenis", idJenis).uniqueResult();
					if (countJenis != null) {
						urutanPerJenis = countJenis.longValue() + 1;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/koperasi/AnggotaKoperasi.java:254");
				}
			} else {
				// Jika tidak ada jenis anggota yang dipilih, urutannya disamakan dengan urutan
				// global
				urutanPerJenis = urutanGlobal;
			}

			// 5. Tentukan Aturan Format
			// Jika prefix adalah MR atau MRS, maka menggunakan format Reguler (pakai bulan
			// dan tanda strip).
			// Selain itu, dianggap sebagai Member Khusus/Fakultas (tanpa bulan, garis
			// miring penuh).
			boolean isReguler = prefix.equals("MR") || prefix.equals("MRS");

			StringBuilder kodeBuilder = new StringBuilder();

			if (isReguler) {
				// Format Reguler: MR-1/02/2026/89
				kodeBuilder.append(prefix).append("-").append(urutanPerJenis).append("/").append(strBulan).append("/")
						.append(tahun).append("/").append(urutanGlobal);
			} else {
				// Format Khusus (Unit/Lembaga): FTSP/1/2026/5
				kodeBuilder.append(prefix).append("/").append(urutanPerJenis).append("/").append(tahun).append("/")
						.append(urutanGlobal);
			}

			return kodeBuilder.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/koperasi/AnggotaKoperasi.java:283");
			return null;
		}
	}

	public AnggotaKoperasi() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_anggota_koperasi", nullable = true)
	public JenisAnggotaKoperasi getJenisAnggotaKoperasi() {
		if (jenisAnggotaKoperasi == null) {
			jenisAnggotaKoperasi = ConstantValues.ANGGOTA_KOPERASI_REGULER;
		} else {
			jenisAnggotaKoperasi = check(jenisAnggotaKoperasi);
		}
		return jenisAnggotaKoperasi;
	}

	public void setJenisAnggotaKoperasi(JenisAnggotaKoperasi jenisAnggotaKoperasi) {
		this.jenisAnggotaKoperasi = jenisAnggotaKoperasi;
	}

	@Column(unique = true)
	public String getKode() {

		if (kode == null || kode.trim().isEmpty()) {
			if (getMahasiswa() != null) {
				kode = getMahasiswa().getNim();
			} else if (getDosen() != null && getDosen().getNidn() != null && !getDosen().getNidn().trim().isEmpty()) {
				kode = getDosen().getNidn();
			} else if (getSiswa() != null && getSiswa().getNomorInduk() != null
					&& !getSiswa().getNomorInduk().trim().isEmpty()) {
				kode = getSiswa().getNomorInduk();
			} else if (getGuru() != null && getGuru().getNuptk() != null && !getGuru().getNuptk().trim().isEmpty()) {
				kode = getGuru().getNuptk();
			} else if (kode == null) {
				kode = BarcodeCommon.generateCode();
			}
		}

		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String ambilNama() {
		return nama;
	}

	public String getNama() {
		getMahasiswa();
		getDosen();
		getPegawai();
		getTbmuser();
		getGuru();
		getSiswa();

		if (mahasiswa != null) {
			nama = mahasiswa.getNama();
		} else if (dosen != null) {
			nama = dosen.getNama();
		} else if (guru != null) {
			nama = guru.getNama();
		} else if (siswa != null) {
			nama = siswa.getNama();
		} else if (pegawai != null) {
			nama = pegawai.getNama();
		} else if (tbmuser != null && tbmuser.ambilNama() != null && !tbmuser.ambilNama().trim().isEmpty()) {
			nama = tbmuser.ambilNama();
		}
		return nama;
	}

	public String generateEmail() {
		return getNama().toLowerCase().replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", "")
				+ ThreadLocalRandom.current().nextLong(100, 999)
				+ Common.getKonfigurasi("alamat_email_default", "@eschool.id").getNilai().trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "alamat", nullable = true, columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		/*
		 * Getter entity harus murni dan tidak melakukan lookup database.
		 * Hibernate memanggil getter saat flush/autoflush untuk membaca nilai property.
		 * Jika getter melakukan query lagi, proses flush dapat berulang terus.
		 */
		return koperasi;
	}

	/**
	 * Helper eksplisit untuk kebutuhan UI/service yang memang ingin fallback ke
	 * koperasi aktif saat field koperasi belum terisi. Jangan dipakai oleh Hibernate
	 * mapping atau proses flush.
	 */
	public Koperasi ambilKoperasiAtauDefault() {
		Koperasi k = koperasi;
		if (k == null) {
			try {
				k = Common.getCurrentKoperasi();
			} catch (Exception e) {
				k = null;
			}
		}
		return k;
	}

	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi;
	}

	@Column(name = "kode_identitas", nullable = true)
	public String getKodeIdentitas() {
		getMahasiswa();
		getDosen();
		getPegawai();
		getTbmuser();
		getGuru();
		getSiswa();
		if (mahasiswa != null && mahasiswa.getNim() != null && !mahasiswa.getNim().trim().isEmpty()) {
			kodeIdentitas = mahasiswa.getNim();
		} else if (dosen != null && dosen.getNidn() != null && !dosen.getNidn().trim().isEmpty()) {
			kodeIdentitas = dosen.getNidn();
		} else if (guru != null && guru.getNuptk() != null && !guru.getNuptk().trim().isEmpty()) {
			kodeIdentitas = guru.getNuptk();
		} else if (siswa != null && siswa.getNomorInduk() != null && !siswa.getNomorInduk().trim().isEmpty()) {
			kodeIdentitas = siswa.getNomorInduk();
		} else if (siswa != null && siswa.getNomorIndukNasional() != null
				&& !siswa.getNomorIndukNasional().trim().isEmpty()) {
			kodeIdentitas = siswa.getNomorIndukNasional();
		} else if (pegawai != null && pegawai.getMycode() != null && !pegawai.getMycode().trim().isEmpty()) {
			kodeIdentitas = pegawai.getMycode();
		}

		return kodeIdentitas;
	}

	public void setKodeIdentitas(String kodeIdentitas) {
		this.kodeIdentitas = kodeIdentitas;
	}

	public String getTipe() {
		return tipe;
	}

	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	public String getJenisIdentitas() {
		return jenisIdentitas;
	}

	public void setJenisIdentitas(String jenisIdentitas) {
		this.jenisIdentitas = jenisIdentitas;
	}

	public String getTelp() {
		return telp;
	}

	public void setTelp(String telp) {
		this.telp = telp;
	}

	public String getHp() {
		return hp;
	}

	public void setHp(String hp) {
		this.hp = hp;
	}

	/** Nomor seluler kanonik (62xxxxxxxxxx) untuk identitas unik member POS. */
	@Column(name = "nomor_hp_normalisasi", length = 32)
	public String getNomorHpNormalisasi() {
		return nomorHpNormalisasi;
	}

	public void setNomorHpNormalisasi(String nomorHpNormalisasi) {
		this.nomorHpNormalisasi = nomorHpNormalisasi;
	}

	@Column(name = "email_nasabah")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_identitas_anggota_koperasi", nullable = true)
	public JenisIdentitasAnggotaKoperasi getJenisIdentitasAnggotaKoperasi() {

		if (getMahasiswa() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NIM;
		} else if (getDosen() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NIDN;
		} else if (getSiswa() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NIS;
		} else if (getGuru() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NUPTK;
		} else if (jenisIdentitasAnggotaKoperasi == null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.KTP;
		}

		jenisIdentitasAnggotaKoperasi = check(jenisIdentitasAnggotaKoperasi);
		return jenisIdentitasAnggotaKoperasi;
	}

	public void setJenisIdentitasAnggotaKoperasi(JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi) {
		this.jenisIdentitasAnggotaKoperasi = jenisIdentitasAnggotaKoperasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota_koperasi", nullable = true)
	public TipeAnggotaKoperasi getTipeAnggotaKoperasi() {
		if (getMahasiswa() != null) {
			tipeAnggotaKoperasi = ConstantValues.MAHASISWA;
		} else if (getDosen() != null) {
			tipeAnggotaKoperasi = ConstantValues.DOSEN;
		} else if (getSiswa() != null) {
			tipeAnggotaKoperasi = ConstantValues.SISWA;
		} else if (getGuru() != null) {
			tipeAnggotaKoperasi = ConstantValues.GURU;
		} else if (tipeAnggotaKoperasi == null) {
			tipeAnggotaKoperasi = ConstantValues.PEGAWAI;
		} else {
			tipeAnggotaKoperasi = check(tipeAnggotaKoperasi);
		}
		return tipeAnggotaKoperasi;
	}

	public void setTipeAnggotaKoperasi(TipeAnggotaKoperasi tipeAnggotaKoperasi) {
		this.tipeAnggotaKoperasi = tipeAnggotaKoperasi;
	}

	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		// TAMBAHAN: Jika tanggal_kadaluarsa diisi dan sudah melewati batas tanggal saat
		// ini
		if (tanggalKadaluarsa != null) {
			if (ais.ui.util.WaktuUtil.getDate().compareTo(tanggalKadaluarsa) >= 0) {
				aktif = false;
			}
		}
		return aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Tanggal anggota berhenti/keluar dari koperasi (untuk "Buku Anggota" pada template pembukuan).
	 * {@code null} berarti masih aktif sebagai anggota.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_berhenti")
	public Date getTanggalBerhenti() {
		return tanggalBerhenti;
	}

	public void setTanggalBerhenti(Date tanggalBerhenti) {
		this.tanggalBerhenti = tanggalBerhenti;
	}

	/** Alasan anggota berhenti/keluar (mis. meninggal, pindah, mengundurkan diri). */
	@Column(name = "alasan_berhenti", columnDefinition = "text")
	public String getAlasanBerhenti() {
		return alasanBerhenti;
	}

	public void setAlasanBerhenti(String alasanBerhenti) {
		this.alasanBerhenti = alasanBerhenti;
	}

	/**
	 * {@code true} bila anggota merupakan <b>pihak terkait</b> (mis. pengurus/pengawas). Dipakai untuk
	 * perhitungan Batas Maksimum Pemberian Pinjaman (BMPP): pihak terkait dibatasi 10% dari Modal
	 * Sendiri, sedangkan pihak tidak terkait 15%. Default {@code false}.
	 */
	@Column(name = "pihak_terkait")
	public Boolean getPihakTerkait() {
		return pihakTerkait == null ? false : pihakTerkait;
	}

	public void setPihakTerkait(Boolean pihakTerkait) {
		this.pihakTerkait = pihakTerkait;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getPegawai() != null && getPegawai().getSatuanKerja() != null) {
			satuanKerja = getPegawai().getSatuanKerja();
		} else if (getDosen() != null && getDosen().getJurusan() != null
				&& getDosen().getJurusan().getSatuanKerja() != null
				&& getDosen().getJurusan().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getJurusan().getSatuanKerja();
		}

		else if (getDosen() != null && getDosen().getFakultas() != null
				&& getDosen().getFakultas().getSatuanKerja() != null
				&& getDosen().getFakultas().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getFakultas().getSatuanKerja();
		}

		else if (getDosen() != null && getDosen().getPerguruanTinggi() != null
				&& getDosen().getPerguruanTinggi().getSatuanKerja() != null
				&& getDosen().getPerguruanTinggi().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getPerguruanTinggi().getSatuanKerja();
		}

		else if (getGuru() != null && getGuru().getSekolah() != null && getGuru().getSekolah().getSatuanKerja() != null
				&& getGuru().getSekolah().getGuruHarusPakaiSatuanKerja()) {
			satuanKerja = getGuru().getSekolah().getSatuanKerja();
		} else if (getTbmuser() != null && getTbmuser().getSatuanKerja() != null) {
			satuanKerja = getTbmuser().getSatuanKerja();
		} else if (satuanKerja == null) {
			guru = getGuru();
			dosen = getDosen();
			if (this.guru != null && guru.getSekolah() != null && guru.getSekolah().getSatuanKerja() != null) {
				try {
					this.satuanKerja = guru.getSekolah().getSatuanKerja();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/AnggotaKoperasi.java:727");
				}
			} else if (this.dosen != null && dosen.getPerguruanTinggi() != null
					&& dosen.getPerguruanTinggi().getSatuanKerja() != null) {
				try {
					this.satuanKerja = dosen.getPerguruanTinggi().getSatuanKerja();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/AnggotaKoperasi.java:733");
				}
			} else if (this.satuanKerja == null && this.id == null) {
				try {
					Tbmuser currentUser = Common.getCurrentUser();
					SatuanKerja satuanKerja = currentUser == null ? null : currentUser.ambilSatuanKerja();
					Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
					if (satuanKerja == null && currentPerpustakaan != null) {
						satuanKerja = currentPerpustakaan.getSatuanKerja();
					}
					this.satuanKerja = satuanKerja;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/AnggotaKoperasi.java:743");
				}
			}
		}
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	public String getUserid() {
		return userid == null || userid.trim().isEmpty() ? null : userid.trim();
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPass() {
		return pass == null || pass.trim().isEmpty() ? null : pass.trim();
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_anggota_koperasi", nullable = true)
	public CalonAnggotaKoperasi getCalonAnggotaKoperasi() {
		calonAnggotaKoperasi = check(calonAnggotaKoperasi);
		return calonAnggotaKoperasi;
	}

	public void setCalonAnggotaKoperasi(CalonAnggotaKoperasi calonAnggotaKoperasi) {
		this.calonAnggotaKoperasi = calonAnggotaKoperasi;
	}

	public String ambilKelasLesDipilih() {
		return kelasLesDipilih == null ? "" : kelasLesDipilih.trim();
	}

	public String getKelasLesDipilih() {
		if (getSiswa() != null) {
			kelasLesDipilih = getSiswa().getKelasLesDipilih();
		} else {
			kelasLesDipilih = (kelasLesDipilih == null || kelasLesDipilih.trim().equalsIgnoreCase(",") ? ""
					: "," + kelasLesDipilih.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
					.replaceAll(",,", ",");

			if (kelasLesDipilih.equals(",")) {
				kelasLesDipilih = "";
			} else if (kelasLesDipilih.equals(",,")) {
				kelasLesDipilih = "";
			} else if (kelasLesDipilih.equals(",,,")) {
				kelasLesDipilih = "";
			}
		}
		return kelasLesDipilih == null ? "" : kelasLesDipilih.trim();
	}

	public void setKelasLesDipilih(String kelasLesDipilih) {
		this.kelasLesDipilih = kelasLesDipilih;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_kadaluarsa")
	public Date getTanggalKadaluarsa() {
		return tanggalKadaluarsa;
	}

	public void setTanggalKadaluarsa(Date tanggalKadaluarsa) {
		this.tanggalKadaluarsa = tanggalKadaluarsa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

}
