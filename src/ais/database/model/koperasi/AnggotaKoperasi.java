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
	 * PIN transaksi anggota dalam bentuk teks terbuka. Field ini adalah <b>sisa warisan</b> yang
	 * dipertahankan semata-mata sebagai jalur migrasi: sejak pengamanan PIN diberlakukan, penyimpanan
	 * yang sah adalah trio {@link #pinHash}/{@link #pinSalt}/{@link #pinIterations}, dan
	 * {@link #aturPinAman(String)} selalu mengosongkan field ini setiap kali PIN diatur ulang.
	 * Baris anggota yang masih memiliki nilai di sini berarti PIN-nya belum pernah disetel ulang
	 * setelah migrasi.
	 *
	 * <p>Keterangan asli yang dipertahankan:</p>
	 *
	 * PIN transaksi anggota (dientri pembeli di Layar Pelanggan / layar kedua POS saat
	 * {@link JenisAnggotaKoperasi#getWajibPin()} aktif). Verifikasi dilakukan
	 * SERVER-SIDE memakai PBKDF2+salt (plaintext lama hanya fallback migrasi) -- nilai PIN TIDAK PERNAH
	 * dikirim kembali ke browser. Tidak ada PIN bawaan bersama: setiap member wajib diatur
	 * secara eksplisit oleh admin. DIAUDIT (Envers) -- riwayat perubahan PIN anggota perlu terlacak;
	 * kolom tabel audit ditambah via InitIndex.java (ALTER new_audit.anggota_koperasi__audit).
	 */
	private String pin;

	/**
	 * Digest PBKDF2 dari PIN anggota. Sengaja ditandai {@code @NotAudited} agar material kredensial
	 * tidak berlipat ganda di tabel revisi Envers.
	 */
	private String pinHash;

	/**
	 * Garam (<i>salt</i>) acak per anggota yang dipakai bersama {@link #pinHash}. Nilai unik per baris
	 * inilah yang membuat dua anggota berPIN sama menghasilkan digest berbeda, sehingga tabel
	 * pelangi tidak dapat dipakai.
	 */
	private String pinSalt;

	/**
	 * Jumlah iterasi PBKDF2 yang dipakai saat digest dibuat. Disimpan per baris agar biaya kerja dapat
	 * dinaikkan di kemudian hari tanpa membatalkan PIN yang sudah terlanjur tersimpan dengan iterasi
	 * lama.
	 */
	private Integer pinIterations;

	/**
	 * Mengembalikan PIN teks terbuka warisan, sudah dipangkas spasi, atau {@code null} bila kosong.
	 *
	 * <p>Hanya relevan bagi anggota yang belum bermigrasi ke penyimpanan ber-hash. Jangan memakai
	 * getter ini untuk verifikasi -- pakai {@link #verifikasiPin(String)} yang menangani kedua bentuk
	 * penyimpanan -- dan jangan pernah mengirim nilainya ke antarmuka.</p>
	 *
	 * @return PIN teks terbuka warisan, atau {@code null} bila tidak ada
	 */
	@Column(name = "pin")
	public String getPin() {
		return pin == null || pin.trim().length() == 0 ? null : pin.trim();
	}

	/**
	 * Mengisi PIN teks terbuka warisan, dinormalkan menjadi {@code null} bila kosong.
	 *
	 * <p>Disediakan untuk pemetaan Hibernate dan pembersihan data lama. Untuk menetapkan PIN baru
	 * gunakan {@link #aturPinAman(String)} yang langsung menyimpannya dalam bentuk ber-hash.</p>
	 *
	 * @param pin PIN teks terbuka; string kosong diperlakukan sebagai {@code null}
	 */
	public void setPin(String pin) {
		this.pin = pin == null || pin.trim().length() == 0 ? null : pin.trim();
	}

	/**
	 * Mengembalikan digest PBKDF2 dari PIN anggota.
	 *
	 * @return digest PIN, atau {@code null} bila PIN belum pernah disetel secara aman
	 */
	@NotAudited
	@Column(name = "pin_hash", length = 128)
	public String getPinHash() { return pinHash; }
	/**
	 * Mengisi digest PBKDF2 PIN. Untuk pemetaan Hibernate; gunakan {@link #aturPinAman(String)} pada
	 * alur aplikasi agar digest, garam, dan jumlah iterasi selalu ditulis sebagai satu kesatuan.
	 *
	 * @param pinHash digest PIN
	 */
	public void setPinHash(String pinHash) { this.pinHash = pinHash; }

	/**
	 * Mengembalikan garam acak yang menyertai {@link #getPinHash()}.
	 *
	 * @return garam PIN, atau {@code null} bila PIN belum pernah disetel secara aman
	 */
	@NotAudited
	@Column(name = "pin_salt", length = 128)
	public String getPinSalt() { return pinSalt; }
	/**
	 * Mengisi garam PIN. Untuk pemetaan Hibernate; lihat {@link #aturPinAman(String)}.
	 *
	 * @param pinSalt garam acak per anggota
	 */
	public void setPinSalt(String pinSalt) { this.pinSalt = pinSalt; }

	/**
	 * Mengembalikan jumlah iterasi PBKDF2 yang dipakai saat digest PIN dibuat.
	 *
	 * @return jumlah iterasi, atau {@code null} bagi baris yang belum bermigrasi
	 */
	@NotAudited
	@Column(name = "pin_iterations")
	public Integer getPinIterations() { return pinIterations; }
	/**
	 * Mengisi jumlah iterasi PBKDF2. Untuk pemetaan Hibernate; lihat {@link #aturPinAman(String)}.
	 *
	 * @param pinIterations jumlah iterasi yang dipakai saat digest dibuat
	 */
	public void setPinIterations(Integer pinIterations) { this.pinIterations = pinIterations; }

	/**
	 * Menetapkan PIN transaksi baru bagi anggota ini, menyimpannya sebagai digest PBKDF2 bergaram, dan
	 * menghapus jejak PIN teks terbuka warisan.
	 *
	 * <h4>Apa yang dikerjakan</h4>
	 * <ol>
	 * <li><b>Validasi bentuk.</b> Nilai wajib cocok dengan pola {@code [0-9]{4,8}} -- empat sampai
	 * delapan angka, tanpa huruf, tanda baca, maupun spasi. Nilai {@code null} atau yang tidak cocok
	 * ditolak dengan {@link IllegalArgumentException}. Validasi dilakukan di sini, pada entity, agar
	 * tidak ada jalur pemanggil mana pun (layar admin, API biometrik, maupun impor massal) yang bisa
	 * menyelundupkan PIN berbentuk aneh dengan melewatkan pemeriksaan sisi antarmuka.</li>
	 * <li><b>Pembangkitan digest.</b> {@code PasswordHashService.hash} menghasilkan pasangan
	 * digest dan garam acak baru. Garam dibuat ulang setiap kali PIN diatur, sehingga menetapkan PIN
	 * yang sama dua kali tetap menghasilkan digest yang berbeda.</li>
	 * <li><b>Pencatatan biaya kerja.</b> Jumlah iterasi yang berlaku disimpan ke
	 * {@link #pinIterations}, sehingga verifikasi di masa depan tahu berapa iterasi yang harus
	 * diulang. Ini yang memungkinkan biaya kerja dinaikkan secara bertahap tanpa membatalkan PIN
	 * lama.</li>
	 * <li><b>Penghapusan warisan.</b> {@link #pin} di-{@code null}-kan. Inilah langkah yang membuat
	 * populasi baris berPIN teks terbuka menyusut secara alami seiring anggota menyetel ulang
	 * PIN-nya.</li>
	 * </ol>
	 *
	 * <h4>Sifat operasi</h4>
	 * <p>Method ditandai {@code @Transient} sehingga bukan properti persistensi, namun ia
	 * <b>mengubah empat field yang dipetakan</b>. Bila objek anggota sedang terikat pada
	 * <i>persistence context</i>, perubahan tersebut akan ter-flush ke basis data pada akhir
	 * transaksi tanpa perlu pemanggilan {@code save}/{@code update} eksplisit. Karena kolom digest
	 * ditandai {@code @NotAudited}, penggantian PIN tidak menorehkan material kredensial ke tabel
	 * revisi Envers -- yang tercatat di sana hanyalah bahwa baris tersebut berubah.</p>
	 *
	 * <h4>Siapa yang boleh memanggil</h4>
	 * <p>Penetapan PIN adalah tindakan istimewa dan seluruh jalur pemanggilnya digerbangi hak akses:
	 * penggantian PIN massal dari layar kantin diperiksa terhadap izin CRUD anggota, penetapan lewat
	 * API biometrik diperiksa terhadap izin kelola PIN member, dan impor massal berjalan lewat utilitas
	 * tersendiri. Tidak ada PIN bawaan bersama: setiap anggota wajib diatur secara eksplisit, sehingga
	 * anggota yang belum pernah disetel tidak dapat bertransaksi pada alur yang mewajibkan PIN.</p>
	 *
	 * <h4>Batasan yang perlu disadari</h4>
	 * <p>Pola yang diizinkan hanya angka dengan panjang minimum empat, sehingga ruang tebak terkecil
	 * adalah sepuluh ribu kemungkinan. Kekuatan sesungguhnya karena itu tidak ditentukan oleh method
	 * ini, melainkan oleh ada tidaknya pembatasan laju percobaan pada sisi verifikasi. Lihat catatan
	 * pada {@link #verifikasiPin(String)}.</p>
	 *
	 * @param nilai PIN baru, empat sampai delapan angka
	 * @throws IllegalArgumentException bila {@code nilai} {@code null} atau tidak berbentuk empat
	 *                                  sampai delapan angka
	 * @see #verifikasiPin(String)
	 * @see #getPinSudahDiatur()
	 */
	@Transient
	public void aturPinAman(String nilai) {
		if (nilai == null || !nilai.matches("[0-9]{4,8}"))
			throw new IllegalArgumentException("PIN wajib terdiri dari 4 sampai 8 angka");
		String[] hashSalt = PasswordHashService.hash(nilai);
		pinHash = hashSalt[0]; pinSalt = hashSalt[1];
		pinIterations = Integer.valueOf(PasswordHashService.ITERASI); pin = null;
	}

	/**
	 * Memeriksa apakah PIN yang dimasukkan pembeli cocok dengan PIN anggota ini.
	 *
	 * <h4>Dua bentuk penyimpanan</h4>
	 * <p>Method memilih jalur pembandingan berdasarkan bentuk data yang tersimpan:</p>
	 * <ul>
	 * <li><b>Jalur utama.</b> Bila {@link #pinHash} dan {@link #pinSalt} sama-sama terisi,
	 * pembandingan diserahkan ke {@code PasswordHashService.verify} yang menghitung ulang digest
	 * PBKDF2 dari nilai masukan memakai garam dan jumlah iterasi milik baris ini, lalu
	 * membandingkannya dengan waktu tetap (<i>constant time</i>). Waktu tetap penting agar lamanya
	 * pemeriksaan tidak membocorkan berapa banyak digit awal yang sudah benar.</li>
	 * <li><b>Jalur warisan.</b> Bila digest belum ada, method jatuh ke pembandingan teks terbuka
	 * terhadap {@link #getPin()}. Ini semata-mata untuk anggota yang belum bermigrasi, dan jalur ini
	 * akan mati dengan sendirinya begitu PIN yang bersangkutan disetel ulang lewat
	 * {@link #aturPinAman(String)}.</li>
	 * </ul>
	 * <p>Bila anggota belum punya PIN dalam bentuk apa pun, kedua jalur mengembalikan
	 * {@code false} -- tidak ada masukan yang bisa dianggap cocok.</p>
	 *
	 * <h4>Verifikasi berjalan di server</h4>
	 * <p>Nilai PIN, baik digest maupun teks terbukanya, tidak pernah dikirimkan ke peramban.
	 * Antarmuka hanya menerima jawaban boolean, dan untuk keperluan tampilan tersedia
	 * {@link #getPinSudahDiatur()} yang juga hanya mengembalikan boolean. Dengan demikian
	 * pembandingan tidak dapat dipindahkan ke sisi klien.</p>
	 *
	 * <h4>Peringatan: tidak ada pembatasan laju di sekitar method ini</h4>
	 * <p>Method ini murni sebuah pembanding -- ia <b>tidak</b> menghitung percobaan gagal, tidak
	 * menunda jawaban, dan tidak mengunci akun. Pembatasan laju, penundaan bertingkat, atau
	 * penguncian sementara harus disediakan oleh pemanggil. Perlu diketahui bahwa jalur pemanggil
	 * yang ada saat ini (endpoint verifikasi PIN pada POS) juga belum menyediakannya; percobaan
	 * gagal hanya dicatat sebagai jejak audit, dan pencatatan tersebut secara sengaja tidak mengunci
	 * kode transaksi. Digabung dengan panjang PIN minimum empat angka, artinya ruang tebak sepuluh
	 * ribu kemungkinan dapat disapu habis oleh pemanggil endpoint yang gigih. Siapa pun yang
	 * menambahkan pemanggil baru wajib memasang pembatasan laju sendiri, dan sebaiknya mengangkat
	 * kekurangan ini pada jalur yang sudah ada.</p>
	 *
	 * @param nilai PIN yang dimasukkan pembeli
	 * @return {@code true} bila cocok; {@code false} bila tidak cocok atau anggota belum berPIN
	 * @see #aturPinAman(String)
	 */
	@Transient
	public boolean verifikasiPin(String nilai) {
		if (pinHash != null && pinSalt != null)
			return PasswordHashService.verify(nilai, pinHash, pinSalt, pinIterations);
		return nilai != null && getPin() != null && getPin().equals(nilai);
	}

	/**
	 * Menyatakan apakah anggota ini sudah memiliki PIN, dalam bentuk ber-hash maupun teks terbuka
	 * warisan.
	 *
	 * <p>Disediakan khusus agar antarmuka dapat menampilkan status "PIN sudah/belum diatur" tanpa
	 * perlu menyentuh nilai PIN itu sendiri. Method ini adalah satu-satunya informasi tentang PIN
	 * yang boleh mengalir ke peramban.</p>
	 *
	 * @return {@code true} bila PIN sudah pernah diatur
	 */
	@Transient
	public boolean getPinSudahDiatur() {
		return (pinHash != null && pinSalt != null) || getPin() != null;
	}

	/**
	 * Menyusun kode member koperasi untuk anggota ini berdasarkan jenis keanggotaan, bulan/tahun
	 * pendaftaran, dan dua nomor urut yang dihitung langsung dari basis data.
	 *
	 * <h4>Bahan penyusun</h4>
	 * <ul>
	 * <li><b>Prefix</b> diambil dari kode {@link JenisAnggotaKoperasi} anggota, dijadikan huruf
	 * besar. Bila jenis keanggotaan belum diisi atau kodenya kosong, dipakai prefix bawaan
	 * {@code "MEM"}.</li>
	 * <li><b>Bulan dan tahun</b> diambil dari {@code tanggalDaftar}; bila argumen tersebut
	 * {@code null}, dipakai waktu saat ini. Bulan diformat dua digit.</li>
	 * <li><b>Nomor urut global</b> -- jumlah seluruh baris pada {@code koperasi.anggota_koperasi}
	 * ditambah satu, tanpa memandang tenant maupun jenis keanggotaan.</li>
	 * <li><b>Nomor urut per jenis</b> -- jumlah anggota dengan jenis keanggotaan yang sama ditambah
	 * satu. Bila anggota belum punya jenis keanggotaan, nomor ini disamakan dengan nomor urut
	 * global.</li>
	 * </ul>
	 *
	 * <h4>Dua bentuk keluaran</h4>
	 * <p>Bentuk kode ditentukan oleh prefix. Prefix {@code "MR"} dan {@code "MRS"} dianggap
	 * keanggotaan reguler dan memakai bentuk yang menyertakan bulan, misalnya
	 * {@code MR-1/02/2026/89}. Prefix lain dianggap keanggotaan khusus unit/lembaga dan memakai
	 * bentuk tanpa bulan, misalnya {@code FTSP/1/2026/5}. Perhatikan bahwa penentuan ini berdasarkan
	 * pencocokan teks pada kode jenis, bukan pada penanda tersendiri; mengganti kode sebuah jenis
	 * keanggotaan karena itu diam-diam mengubah bentuk kode member yang diterbitkan setelahnya.</p>
	 *
	 * <h4>Nomor urut diambil dengan COUNT, bukan dengan penghitung terkunci</h4>
	 * <p>Kedua nomor urut diperoleh lewat {@code COUNT(*) + 1} tanpa penguncian baris, tanpa tabel
	 * penghitung, dan tanpa pemeriksaan duplikat sesudahnya. Padahal {@link #getKode()} dipetakan
	 * sebagai kolom UNIQUE. Konsekuensinya, dua pendaftaran anggota yang berjalan bersamaan akan
	 * membaca COUNT yang sama, menyusun kode yang sama persis, dan salah satunya gagal dengan
	 * pelanggaran batasan keunikan pada saat commit. Seluruh pemanggil yang ada -- pendaftaran member
	 * dari POS, penyimpanan anggota dari layar kantin, serta pembuatan otomatis saat login member --
	 * berjalan di dalam transaksi tetapi tidak satu pun memasang kunci, memeriksa duplikat, atau
	 * mencoba ulang. Pemanggil baru sebaiknya membungkus pemanggilan ini dengan pemeriksaan duplikat
	 * dan percobaan ulang, atau menyerahkan penomoran pada mekanisme yang benar-benar serial.</p>
	 *
	 * <p>Perlu disadari pula bahwa COUNT global mencakup <b>seluruh tenant</b>. Nomor urut terakhir
	 * pada kode member karena itu bukan nomor anggota ke sekian pada koperasi yang bersangkutan,
	 * melainkan nomor pada tabel secara keseluruhan; ia akan melompat-lompat bila ada lebih dari satu
	 * unit usaha dalam satu basis data, dan akan mundur bila ada baris anggota yang dihapus.</p>
	 *
	 * <h4>Kegagalan ditelan secara senyap</h4>
	 * <p>Ketiga blok penangkap kesalahan di dalam method hanya mencetak jejak tumpukan dan
	 * merekamnya ke audit kesalahan, lalu membiarkan eksekusi berlanjut dengan nilai bawaan. Bila
	 * kueri hitung gagal, nomor urut diam-diam bertahan di angka satu dan kode tetap diterbitkan
	 * seolah-olah baik-baik saja. Riwayat kelas ini memuat contoh nyata akibatnya: kueri urutan per
	 * jenis pernah ditulis memakai nama tabel fisik pada HQL sehingga selalu melempar kesalahan
	 * penguraian, tertangkap diam-diam, dan membuat nomor urut per jenis <i>selamanya</i> bernilai
	 * satu tanpa pernah terdeteksi -- perbaikannya beserta penjelasannya masih tertinggal sebagai
	 * komentar di dalam badan method. Kegagalan pada lapisan terluar mengembalikan {@code null},
	 * yang bila tidak diperiksa pemanggil akan tersimpan sebagai kode member kosong.</p>
	 *
	 * <p>Method sengaja menerima {@link Session} sebagai argumen alih-alih membuka sesi sendiri,
	 * supaya penghitungan ikut melihat perubahan yang belum ter-commit pada transaksi pemanggil.</p>
	 *
	 * <p>Keterangan asli yang dipertahankan:</p>
	 *
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

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Objek yang baru dibuat sudah memiliki {@link #tanggal} dan {@link #tanggal_dirubah} berisi
	 * waktu server, serta {@link #aktif} bernilai {@code true}; sisanya kosong dan harus diisi
	 * pemanggil.</p>
	 */
	public AnggotaKoperasi() {
	}

	/**
	 * Mengembalikan primary key anggota.
	 *
	 * <p>Kolom ditandai {@code insertable = false} karena nilainya dibangkitkan basis data melalui
	 * IDENTITY; nilainya baru terisi setelah baris benar-benar tersimpan. Kode yang membutuhkan id
	 * sebagai bagian dari alur penyimpanan harus melakukan flush terlebih dahulu.</p>
	 *
	 * @return primary key, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key anggota. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas mengenai anggota.
	 *
	 * @return catatan bebas, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas mengenai anggota.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan data mahasiswa yang menjadi identitas anggota ini, bila ada.
	 *
	 * <p><b>Tidak murni.</b> Getter memanggil {@code check(...)} dan menulis balik hasilnya ke field,
	 * sehingga dapat memicu inisialisasi proxy lazy atau bahkan membuka sesi Hibernate baru untuk
	 * memuat objek yang sudah lepas dari sesi. Kegagalan resolusi tidak dilaporkan -- pada kasus
	 * terburuk nilai yang sama dikembalikan apa adanya.</p>
	 *
	 * @return data mahasiswa, atau {@code null} bila anggota bukan mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan data mahasiswa sebagai identitas anggota. Mengubah nilai ini ikut mengubah hasil
	 * {@link #getNama()}, {@link #getKodeIdentitas()}, {@link #getTipeAnggotaKoperasi()}, dan
	 * {@link #getJenisIdentitasAnggotaKoperasi()}.
	 *
	 * @param mahasiswa data mahasiswa, boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan data dosen yang menjadi identitas anggota ini, bila ada. Sama seperti getter
	 * relasi lain di kelas ini, pemanggilannya dapat memicu pemuatan dari basis data dan menulis
	 * balik ke field.
	 *
	 * @return data dosen, atau {@code null} bila anggota bukan dosen
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan data dosen sebagai identitas anggota. Ikut memengaruhi nama, kode identitas, tipe
	 * anggota, dan -- lewat jurusan/fakultas/perguruan tinggi -- juga {@link #getSatuanKerja()}.
	 *
	 * @param dosen data dosen, boleh {@code null}
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan data pegawai yang menjadi identitas anggota ini, bila ada. Pemanggilannya dapat
	 * memicu pemuatan dari basis data dan menulis balik ke field.
	 *
	 * @return data pegawai, atau {@code null} bila anggota bukan pegawai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan data pegawai sebagai identitas anggota. Relasi ini memiliki prioritas tertinggi
	 * dalam penurunan {@link #getSatuanKerja()}.
	 *
	 * @param pegawai data pegawai, boleh {@code null}
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan akun pengguna aplikasi yang tertaut pada anggota ini, bila ada. Pemanggilannya
	 * dapat memicu pemuatan dari basis data dan menulis balik ke field.
	 *
	 * @return akun pengguna, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menautkan akun pengguna aplikasi pada anggota ini. Dipakai sebagai sumber nama dan satuan kerja
	 * dengan prioritas paling rendah, yaitu hanya bila relasi orang lain tidak tersedia.
	 *
	 * @param tbmuser akun pengguna, boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan jenis keanggotaan (dimensi paket) anggota ini.
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null} dalam kondisi normal.</b> Bila kolom masih kosong,
	 * getter memasang konstanta jenis anggota reguler bawaan aplikasi dan menulisnya ke field --
	 * sehingga sekadar membaca properti ini pada baris lama dapat menghasilkan UPDATE yang mengisi
	 * kolom {@code jenis_anggota_koperasi} pada akhir transaksi. Perilaku ini menyeragamkan data
	 * historis, tetapi berarti pembacaan bukan operasi bebas efek samping.</p>
	 *
	 * <p>Perlu dicatat bahwa konstanta bawaan tersebut adalah objek statis tingkat JVM yang di-seed
	 * saat aplikasi dimulai. Bila proses seed gagal, nilai yang dipasang di sini bisa berupa
	 * {@code null} tanpa melewati {@code check(...)}, dan cabang ini tidak menyediakan penjagaan
	 * tambahan.</p>
	 *
	 * @return jenis keanggotaan, jatuh ke jenis reguler bawaan bila belum diisi
	 */
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

	/**
	 * Menetapkan jenis keanggotaan (dimensi paket). Nilai ini menentukan prefix kode member yang
	 * diterbitkan {@link #generateKodeMember(Session, Date)} serta aturan kewajiban belanja rutin dan
	 * kewajiban PIN.
	 *
	 * @param jenisAnggotaKoperasi jenis keanggotaan, boleh {@code null}
	 */
	public void setJenisAnggotaKoperasi(JenisAnggotaKoperasi jenisAnggotaKoperasi) {
		this.jenisAnggotaKoperasi = jenisAnggotaKoperasi;
	}

	/**
	 * Mengembalikan kode member anggota, membangkitkannya lebih dahulu bila masih kosong.
	 *
	 * <h4>Urutan penurunan</h4>
	 * <p>Bila {@link #kode} kosong, getter mencoba memungut nomor identitas resmi anggota dengan
	 * urutan prioritas: NIM mahasiswa, lalu NIDN dosen, lalu nomor induk siswa, lalu NUPTK guru.
	 * Bila tidak satu pun tersedia, dibangkitkan barcode acak. Perhatikan bahwa cabang mahasiswa
	 * tidak memeriksa apakah NIM-nya kosong, berbeda dari tiga cabang lainnya; anggota bertaut
	 * mahasiswa tanpa NIM karena itu dapat memperoleh kode kosong alih-alih jatuh ke cabang
	 * berikutnya.</p>
	 *
	 * <p>Kode member yang dihasilkan di sini berbeda dari kode terformat yang disusun
	 * {@link #generateKodeMember(Session, Date)}. Yang ini memakai identitas institusional yang sudah
	 * ada supaya kartu mahasiswa atau kartu siswa dapat langsung dipindai di kasir; yang itu menyusun
	 * kode bernomor urut bagi anggota yang tidak membawa identitas semacam itu.</p>
	 *
	 * <h4>Efek samping</h4>
	 * <p><b>Getter ini destruktif.</b> Nilai yang dibangkitkan ditulis ke field, dan karena kolomnya
	 * dipetakan UNIQUE, pembacaan pada baris berkode kosong dapat menghasilkan UPDATE yang menetapkan
	 * kode permanen bagi anggota tersebut. Selain itu getter memanggil enam getter relasi sehingga
	 * ikut membawa seluruh biaya dan efek samping {@code check(...)}. Karena penetapan berlangsung
	 * tanpa pemeriksaan tabrakan, dua baris berkode kosong yang dibaca bersamaan berpeluang menerima
	 * nilai yang sama bila identitas sumbernya kebetulan sama.</p>
	 *
	 * @return kode member; tidak pernah kosong kecuali sumber identitasnya sendiri kosong
	 */
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

	/**
	 * Menetapkan kode member secara eksplisit, melewati seluruh penurunan otomatis
	 * {@link #getKode()}. Karena kolomnya UNIQUE, pemanggil bertanggung jawab memastikan nilainya
	 * belum dipakai anggota lain.
	 *
	 * @param kode kode member
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nilai field nama <b>apa adanya</b>, tanpa menurunkannya dari relasi orang.
	 *
	 * <p>Ini adalah padanan murni dari {@link #getNama()} dan sengaja diberi awalan {@code ambil}
	 * alih-alih {@code get} supaya tidak diperlakukan Hibernate sebagai properti. Gunakan method ini
	 * bila yang dibutuhkan sekadar nilai tersimpan -- misalnya di dalam log, pembanding, atau
	 * perulangan panjang -- untuk menghindari rantai resolusi enam relasi dan penulisan balik yang
	 * dilakukan {@link #getNama()}.</p>
	 *
	 * @return nama tersimpan, mungkin {@code null} bila belum pernah diturunkan
	 */
	public String ambilNama() {
		return nama;
	}

	/**
	 * Mengembalikan nama anggota, menurunkannya lebih dahulu dari relasi orang yang tertaut.
	 *
	 * <h4>Urutan prioritas</h4>
	 * <p>Method memaksa resolusi enam relasi sekaligus -- mahasiswa, dosen, pegawai, akun pengguna,
	 * guru, dan siswa -- lalu mengambil nama dari relasi pertama yang terisi menurut urutan:
	 * mahasiswa, dosen, guru, siswa, pegawai, dan terakhir akun pengguna. Nama dari akun pengguna
	 * hanya dipakai bila benar-benar tidak kosong, sedangkan lima cabang sebelumnya tidak memeriksa
	 * kekosongan nama sumbernya. Akibatnya, anggota yang tertaut ke sebuah entity orang bernama
	 * kosong akan menghasilkan nama kosong, bukan jatuh ke sumber berikutnya.</p>
	 *
	 * <p>Bila tidak satu pun relasi terisi -- kasus anggota umum -- nilai field dipertahankan, yaitu
	 * nama yang dientri manual lewat {@link #setNama(String)}. Ini satu-satunya keadaan di mana nama
	 * yang di-set manual bertahan; begitu sebuah relasi orang ditautkan, nama manual akan tertimpa
	 * pada pembacaan berikutnya.</p>
	 *
	 * <h4>Efek samping dan biaya</h4>
	 * <p><b>Getter ini mahal dan destruktif.</b> Enam pemanggilan {@code check(...)} masing-masing
	 * dapat menginisialisasi proxy lazy atau membuka sesi Hibernate baru untuk memuat objek yang
	 * sudah lepas dari sesi, dan hasil penurunan ditulis balik ke field {@link #nama} sehingga ikut
	 * ter-flush ke basis data. Karena {@link #toString()} memanggil method ini, sekadar mencetak satu
	 * objek anggota ke log sudah cukup untuk memicu seluruh rantai tersebut. Pada penyusunan laporan
	 * atau perulangan panjang, gunakan {@link #ambilNama()} atau ambil nama lewat proyeksi kueri.</p>
	 *
	 * @return nama anggota hasil penurunan, atau nama tersimpan bila tidak ada relasi orang
	 */
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

	/**
	 * Membangkitkan alamat surel semu bagi anggota yang tidak memiliki surel sendiri.
	 *
	 * <p>Alamat disusun dari nama anggota yang dijadikan huruf kecil, dibuang seluruh karakter selain
	 * huruf dan angka, dibuang spasinya, lalu ditambah tiga angka acak dan domain bawaan yang dibaca
	 * dari konfigurasi aplikasi.</p>
	 *
	 * <p><b>Tidak menjamin keunikan.</b> Pembeda satu-satunya adalah tiga digit acak, sehingga ruang
	 * nilainya hanya sekitar sembilan ratus kemungkinan per nama; dua anggota bernama sama berpeluang
	 * cukup besar memperoleh alamat yang sama. Method juga tidak memeriksa apakah alamat hasilnya
	 * sudah terpakai, dan tidak menyimpannya -- pemanggil sendiri yang harus meneruskannya ke
	 * {@link #setEmail(String)} bila memang ingin dipakai.</p>
	 *
	 * <p>Perlu diperhatikan bahwa pembacaan domain bawaan berjalan lewat mekanisme konfigurasi yang
	 * menuliskan nilai bawaan ke basis data bila kunci tersebut belum ada.</p>
	 *
	 * <p>Method memanggil {@link #getNama()} sehingga membawa seluruh biaya dan efek sampingnya, dan
	 * akan gagal dengan {@link NullPointerException} bila nama anggota belum dapat diturunkan.</p>
	 *
	 * @return alamat surel semu hasil bangkitan
	 */
	public String generateEmail() {
		return getNama().toLowerCase().replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", "")
				+ ThreadLocalRandom.current().nextLong(100, 999)
				+ Common.getKonfigurasi("alamat_email_default", "@eschool.id").getNilai().trim();
	}

	/**
	 * Menetapkan nama anggota. Nilai ini hanya bertahan bagi anggota umum yang tidak tertaut relasi
	 * orang mana pun; bila ada relasi yang terisi, {@link #getNama()} akan menimpanya pada pembacaan
	 * berikutnya.
	 *
	 * @param nama nama anggota
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan alamat surat anggota. Kolom bertipe {@code text} sehingga panjangnya tidak
	 * dibatasi.
	 *
	 * @return alamat, atau {@code null}
	 */
	@Column(name = "alamat", nullable = true, columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengisi alamat surat anggota.
	 *
	 * @param alamat alamat surat
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan unit usaha koperasi tempat anggota terdaftar -- pengait tenant utama domain
	 * koperasi.
	 *
	 * <p><b>Getter ini sengaja dibuat murni</b> dan merupakan pengecualian di antara getter relasi
	 * kelas ini: ia tidak memanggil {@code check(...)}, tidak melakukan pencarian, dan tidak menulis
	 * balik apa pun. Alasannya terurai pada komentar di dalam badan method -- Hibernate memanggil
	 * getter saat flush untuk membaca nilai properti, dan getter yang melakukan kueri dapat membuat
	 * proses flush berulang tanpa henti. Kebutuhan fallback ke koperasi aktif dipisahkan ke
	 * {@link #ambilKoperasiAtauDefault()}.</p>
	 *
	 * <p>Karena kolomnya {@code nullable} dan tidak pernah diisi paksa, jangan berasumsi hasilnya
	 * selalu ada; banyak baris anggota historis dibuat sebelum kolom ini diperkenalkan.</p>
	 *
	 * @return unit usaha koperasi, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		/*
		 * LAZY_GETTER_CHECK_EXCEPTION: getter property-access ini wajib tetap murni saat flush.
		 * Getter entity harus murni dan tidak melakukan lookup database.
		 * Hibernate memanggil getter saat flush/autoflush untuk membaca nilai property.
		 * Jika getter melakukan query lagi, proses flush dapat berulang terus.
		 */
		return koperasi;
	}

	/**
	 * Mengembalikan koperasi anggota, atau koperasi yang sedang aktif pada sesi bila field-nya
	 * kosong.
	 *
	 * <p>Ini adalah pasangan "tidak murni" dari {@link #getKoperasi()}. Diberi awalan {@code ambil}
	 * agar Hibernate tidak memperlakukannya sebagai properti, sehingga pencarian koperasi aktif tidak
	 * pernah ikut berjalan saat flush. Kegagalan pencarian ditelan dan menghasilkan {@code null},
	 * bukan melempar kesalahan.</p>
	 *
	 * <p>Perlu diperhatikan bahwa fallback ini bergantung pada konteks sesi pengguna yang sedang
	 * berjalan. Pada proses latar belakang atau penjadwal yang tidak memiliki sesi, hasilnya akan
	 * {@code null}. Method juga hanya <i>mengembalikan</i> nilai fallback -- ia tidak menetapkannya ke
	 * field, sehingga tidak menimbulkan efek samping penyimpanan.</p>
	 *
	 * <p>Keterangan asli yang dipertahankan:</p>
	 *
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

	/**
	 * Menetapkan unit usaha koperasi pemilik anggota.
	 *
	 * @param koperasi unit usaha koperasi, boleh {@code null}
	 */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi;
	}

	/**
	 * Mengembalikan nomor identitas anggota pada institusi asalnya, menurunkannya lebih dahulu dari
	 * relasi orang yang tertaut.
	 *
	 * <h4>Urutan prioritas</h4>
	 * <p>Setelah memaksa resolusi enam relasi, method memungut nomor identitas menurut urutan: NIM
	 * mahasiswa, NIDN dosen, NUPTK guru, nomor induk siswa, nomor induk nasional siswa, lalu kode
	 * pegawai. Berbeda dari {@link #getNama()}, setiap cabang di sini memeriksa kekosongan sumbernya,
	 * sehingga sumber yang kosong benar-benar dilewati dan penurunan berlanjut ke kandidat
	 * berikutnya.</p>
	 *
	 * <p>Bila tidak satu pun sumber tersedia, nilai field dipertahankan apa adanya -- termasuk
	 * {@code null} bagi anggota umum yang identitasnya belum pernah dientri.</p>
	 *
	 * <h4>Hubungan dengan kode member</h4>
	 * <p>Nilai ini kerap sama dengan {@link #getKode()} karena keduanya memungut sumber yang mirip,
	 * tetapi keduanya tidak identik dan tidak saling menjaga konsistensi. Kode member dipetakan
	 * UNIQUE dan dipakai sebagai identitas kasir, sedangkan kode identitas bersifat deskriptif dan
	 * tidak dijaga keunikannya. Jangan memakai kode identitas sebagai kunci pencarian anggota.</p>
	 *
	 * <h4>Efek samping</h4>
	 * <p><b>Getter ini destruktif</b> dengan cara yang sama seperti {@link #getNama()}: enam
	 * pemanggilan {@code check(...)} berpotensi memicu pemuatan dari basis data, dan hasil penurunan
	 * ditulis balik ke field sehingga ikut ter-flush.</p>
	 *
	 * @return nomor identitas institusional, atau {@code null} bila tidak ada sumbernya
	 */
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

	/**
	 * Menetapkan nomor identitas institusional secara manual. Nilai ini akan tertimpa oleh
	 * {@link #getKodeIdentitas()} bila anggota tertaut relasi orang yang membawa nomor identitas.
	 *
	 * @param kodeIdentitas nomor identitas institusional
	 */
	public void setKodeIdentitas(String kodeIdentitas) {
		this.kodeIdentitas = kodeIdentitas;
	}

	/**
	 * Mengembalikan label tipe anggota dalam bentuk teks bebas.
	 *
	 * <p>Berbeda dari {@link #getTipeAnggotaKoperasi()} yang merupakan relasi terstruktur dan menjadi
	 * dasar batas berutang, field teks ini hanya deskriptif dan tidak dipakai sebagai kunci logika
	 * mana pun. Keduanya tidak saling menjaga konsistensi.</p>
	 *
	 * @return label tipe anggota, atau {@code null}
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengisi label tipe anggota berbentuk teks bebas.
	 *
	 * @param tipe label tipe anggota
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengembalikan label jenis identitas dalam bentuk teks bebas.
	 *
	 * <p>Padanan deskriptif dari relasi terstruktur {@link #getJenisIdentitasAnggotaKoperasi()}.
	 * Tidak dipakai sebagai kunci logika dan tidak dijaga konsisten dengan relasi tersebut.</p>
	 *
	 * @return label jenis identitas, atau {@code null}
	 */
	public String getJenisIdentitas() {
		return jenisIdentitas;
	}

	/**
	 * Mengisi label jenis identitas berbentuk teks bebas.
	 *
	 * @param jenisIdentitas label jenis identitas
	 */
	public void setJenisIdentitas(String jenisIdentitas) {
		this.jenisIdentitas = jenisIdentitas;
	}

	/**
	 * Mengembalikan nomor telepon tetap anggota.
	 *
	 * @return nomor telepon tetap, atau {@code null}
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Mengisi nomor telepon tetap anggota.
	 *
	 * @param telp nomor telepon tetap
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan nomor seluler anggota sebagaimana dientri pengguna.
	 *
	 * <p>Nilainya belum dinormalisasi dan boleh berisi spasi, tanda hubung, atau awalan {@code 0}
	 * maupun {@code +62}. Untuk pencocokan dan pencarian gunakan
	 * {@link #getNomorHpNormalisasi()}.</p>
	 *
	 * @return nomor seluler mentah, atau {@code null}
	 */
	public String getHp() {
		return hp;
	}

	/**
	 * Mengisi nomor seluler anggota dalam bentuk mentah. Setter ini <b>tidak</b> ikut memperbarui
	 * {@link #nomorHpNormalisasi}; normalisasi dikerjakan lapisan layanan, sehingga mengubah nomor
	 * lewat method ini saja dapat membuat kedua kolom tidak sinkron.
	 *
	 * @param hp nomor seluler mentah
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Mengembalikan bentuk kanonik nomor seluler anggota, yaitu format {@code 62xxxxxxxxxx} tanpa
	 * pemisah.
	 *
	 * <p>Kolom inilah yang dipakai sebagai identitas unik member pada alur POS -- pembeli cukup
	 * menyebutkan nomor teleponnya untuk dikenali. Karena itu nilai di sini harus benar-benar
	 * kanonik; pencarian member tidak akan menemukan anggota yang nomornya masih tersimpan dalam
	 * bentuk mentah.</p>
	 *
	 * <p>Normalisasi dilakukan di lapisan layanan, bukan oleh entity ini, dan tidak ada penjagaan
	 * yang memaksa kolom ini selaras dengan {@link #getHp()}. Keduanya dapat menyimpang bila nomor
	 * diubah lewat jalur yang melewatkan langkah normalisasi.</p>
	 *
	 * @return nomor seluler kanonik, atau {@code null} bila belum dinormalisasi
	 */
	@Column(name = "nomor_hp_normalisasi", length = 32)
	public String getNomorHpNormalisasi() {
		return nomorHpNormalisasi;
	}

	/**
	 * Mengisi bentuk kanonik nomor seluler. Pemanggil bertanggung jawab memastikan nilainya benar
	 * benar sudah dinormalisasi -- entity ini tidak memverifikasinya.
	 *
	 * @param nomorHpNormalisasi nomor seluler dalam format {@code 62xxxxxxxxxx}
	 */
	public void setNomorHpNormalisasi(String nomorHpNormalisasi) {
		this.nomorHpNormalisasi = nomorHpNormalisasi;
	}

	/**
	 * Mengembalikan alamat surel anggota. Dipetakan ke kolom {@code email_nasabah} -- penamaan yang
	 * tertinggal dari masa ketika entity ini juga melayani konteks nasabah.
	 *
	 * @return alamat surel, atau {@code null}
	 */
	@Column(name = "email_nasabah")
	public String getEmail() {
		return email;
	}

	/**
	 * Mengisi alamat surel anggota. Untuk anggota tanpa surel sendiri, {@link #generateEmail()} dapat
	 * membangkitkan alamat semu yang nilainya harus diteruskan ke method ini secara eksplisit.
	 *
	 * @param email alamat surel
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan jenis dokumen identitas anggota, menurunkannya dari relasi orang yang tertaut.
	 *
	 * <p>Penurunan mengikuti urutan: mahasiswa menghasilkan NIM, dosen menghasilkan NIDN, siswa
	 * menghasilkan NIS, dan guru menghasilkan NUPTK. Bila tidak satu pun relasi terisi dan field
	 * masih kosong, dipasang KTP sebagai jenis bawaan bagi anggota umum.</p>
	 *
	 * <p><b>Getter ini destruktif dan menimpa nilai manual.</b> Empat cabang pertama berjalan tanpa
	 * memeriksa apakah field sudah terisi, sehingga jenis identitas yang dipilih admin secara manual
	 * akan selalu tertimpa selama anggota tertaut ke salah satu entity orang tersebut. Hasil
	 * penurunan ditulis balik ke field dan ikut ter-flush ke basis data pada akhir transaksi.</p>
	 *
	 * <p>Nilai bawaan yang dipasang berasal dari konstanta statis tingkat JVM yang di-seed saat
	 * aplikasi dimulai; bila proses seed gagal, yang terpasang bisa berupa {@code null}. Berbeda dari
	 * {@link #getTipeAnggotaKoperasi()}, di sini {@code check(...)} dipanggil pada seluruh jalur --
	 * termasuk setelah konstanta dipasang -- sehingga hasilnya konsisten melewati resolusi proxy.</p>
	 *
	 * @return jenis dokumen identitas anggota
	 */
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

	/**
	 * Menetapkan jenis dokumen identitas anggota. Nilai ini hanya bertahan bagi anggota umum; bagi
	 * anggota yang tertaut mahasiswa, dosen, siswa, atau guru ia akan tertimpa pada pembacaan
	 * berikutnya oleh {@link #getJenisIdentitasAnggotaKoperasi()}.
	 *
	 * @param jenisIdentitasAnggotaKoperasi jenis dokumen identitas, boleh {@code null}
	 */
	public void setJenisIdentitasAnggotaKoperasi(JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi) {
		this.jenisIdentitasAnggotaKoperasi = jenisIdentitasAnggotaKoperasi;
	}

	/**
	 * Mengembalikan tipe anggota -- dimensi klasifikasi peran/asal orang -- dengan menurunkannya dari
	 * relasi orang yang tertaut.
	 *
	 * <h4>Mengapa nilai ini penting</h4>
	 * <p>Tipe anggota bukan sekadar label tampilan. Batas {@code maksimalBolehUtang} milik
	 * {@link TipeAnggotaKoperasi} adalah gerbang yang benar-benar dipakai alur checkout kantin untuk
	 * menolak belanja tempo, dibandingkan terhadap hutang berjalan yang dihitung on-the-fly dari
	 * tabel pembelian dan pembayaran hutang. Karena {@link #getLimitKredit()} tidak dipakai sebagai
	 * gerbang, nilai inilah satu-satunya pembatas utang yang ditegakkan sistem.</p>
	 *
	 * <h4>Urutan penurunan</h4>
	 * <p>Mahasiswa menghasilkan tipe Mahasiswa, dosen menghasilkan Dosen, siswa menghasilkan Siswa,
	 * guru menghasilkan Guru; bila tidak satu pun terisi dan field masih kosong, dipasang Pegawai
	 * sebagai tipe bawaan. Perhatikan bahwa tipe bawaan bagi anggota tanpa relasi orang adalah
	 * Pegawai, bukan Umum -- anggota luar yang didaftarkan lewat POS karena itu mewarisi batas
	 * berutang milik tipe Pegawai kecuali tipenya ditetapkan eksplisit.</p>
	 *
	 * <h4>Efek samping dan satu kejanggalan</h4>
	 * <p><b>Getter ini destruktif dan menimpa nilai manual:</b> empat cabang pertama tidak memeriksa
	 * apakah field sudah terisi, sehingga tipe yang dipilih admin akan selalu tertimpa selama anggota
	 * tertaut ke entity orang yang bersangkutan. Hasilnya ditulis balik ke field dan ikut ter-flush.
	 * Konsekuensi finansialnya nyata: mengubah keterkaitan seorang anggota ke data mahasiswa atau
	 * pegawai diam-diam mengubah pula plafon utangnya di kasir.</p>
	 *
	 * <p>Berbeda dari {@link #getJenisIdentitasAnggotaKoperasi()}, di sini {@code check(...)} hanya
	 * dipanggil pada cabang terakhir, yaitu ketika field sudah terisi dan tidak ada relasi orang yang
	 * cocok. Nilai yang berasal dari konstanta bawaan maupun dari keempat cabang penurunan
	 * dikembalikan tanpa melewati resolusi proxy, sehingga pemanggil dapat menerima proxy yang belum
	 * terinisialisasi pada jalur-jalur tersebut.</p>
	 *
	 * @return tipe anggota, jatuh ke tipe Pegawai bawaan bila tidak dapat diturunkan
	 */
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

	/**
	 * Menetapkan tipe anggota. Karena tipe menentukan batas berutang yang ditegakkan di kasir,
	 * perubahan nilai ini berdampak langsung pada plafon utang anggota. Nilai yang ditetapkan di sini
	 * akan tertimpa pada pembacaan berikutnya bila anggota tertaut mahasiswa, dosen, siswa, atau
	 * guru.
	 *
	 * @param tipeAnggotaKoperasi tipe anggota, boleh {@code null}
	 */
	public void setTipeAnggotaKoperasi(TipeAnggotaKoperasi tipeAnggotaKoperasi) {
		this.tipeAnggotaKoperasi = tipeAnggotaKoperasi;
	}

	/**
	 * Mengembalikan status keaktifan anggota, sekaligus menegakkan kedaluwarsa keanggotaan.
	 *
	 * <h4>Dua hal yang dikerjakan</h4>
	 * <ol>
	 * <li><b>Normalisasi.</b> Bila kolom masih {@code null} -- kondisi baris lama yang dibuat sebelum
	 * kolom ini ada -- nilainya dijadikan {@code true}. Anggota tanpa status eksplisit karena itu
	 * dianggap aktif.</li>
	 * <li><b>Penegakan kedaluwarsa.</b> Bila {@link #getTanggalKadaluarsa()} terisi dan tanggal
	 * server sudah mencapai atau melewatinya, status dipaksa menjadi {@code false}.</li>
	 * </ol>
	 *
	 * <h4>Penanda ini bekerja satu arah dalam penegakan otomatisnya</h4>
	 * <p>Kedua penyesuaian di atas <b>ditulis balik ke field</b>, sehingga pembacaan status pada
	 * anggota yang baru kedaluwarsa menghasilkan UPDATE yang mematikan keanggotaannya secara
	 * permanen di basis data. Yang perlu disadari: penegakan ini hanya berjalan ke arah menonaktifkan.
	 * Bila tanggal kedaluwarsa kemudian diperpanjang atau dikosongkan, method ini <b>tidak</b>
	 * menghidupkan kembali status yang terlanjur {@code false}; pengaktifan ulang harus dilakukan
	 * eksplisit lewat {@link #setAktif(Boolean)}. Memperpanjang masa berlaku saja karena itu tidak
	 * cukup untuk memulihkan keanggotaan.</p>
	 *
	 * <p>Perbandingan tanggal dilakukan pada nilai penuh termasuk komponen waktu, dan bersifat
	 * inklusif -- anggota sudah dianggap tidak aktif tepat pada saat batas tercapai.</p>
	 *
	 * <p>Selain jalur kedaluwarsa ini, status keaktifan juga dimatikan dari luar oleh proses
	 * terjadwal pelanggaran belanja rutin ketika {@link #getJumlahPeringatan()} melampaui ambang
	 * milik {@link JenisAnggotaKoperasi}. Jadi terdapat tiga sumber yang dapat menonaktifkan seorang
	 * anggota -- admin, kedaluwarsa, dan penjadwal -- sementara hanya admin yang dapat
	 * mengaktifkannya kembali.</p>
	 *
	 * @return {@code true} bila anggota masih aktif
	 */
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

	/**
	 * Menetapkan status keaktifan anggota. Ini satu-satunya jalur untuk mengaktifkan kembali anggota
	 * yang sudah dinonaktifkan, baik oleh kedaluwarsa maupun oleh penjadwal pelanggaran belanja.
	 *
	 * <p>Perlu diperhatikan bahwa mengaktifkan kembali anggota yang tanggal kedaluwarsanya sudah
	 * lewat tidak akan bertahan: pembacaan {@link #getAktif()} berikutnya akan langsung
	 * menonaktifkannya lagi. Perpanjang atau kosongkan dahulu
	 * {@link #setTanggalKadaluarsa(Date)}.</p>
	 *
	 * @param aktif status keaktifan; {@code null} akan dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal pendaftaran anggota.
	 *
	 * <p><b>Tidak pernah {@code null}.</b> Bila kolom kosong, getter memasang tanggal server saat ini
	 * dan menuliskannya ke field, sehingga pembacaan pada baris lama dapat menghasilkan UPDATE yang
	 * menetapkan tanggal pendaftaran hari ini -- bukan tanggal pendaftaran yang sebenarnya. Untuk
	 * data historis yang tanggal pendaftarannya hilang, isi kolom secara eksplisit sebelum baris
	 * tersebut sempat terbaca.</p>
	 *
	 * @return tanggal pendaftaran anggota
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * Menetapkan tanggal pendaftaran anggota.
	 *
	 * @param tanggal tanggal pendaftaran
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tanggal anggota berhenti atau keluar dari koperasi.
	 *
	 * <p>Kolom ini adalah penanda riwayat, <b>bukan</b> penanda status: mengisinya tidak dengan
	 * sendirinya menonaktifkan anggota. Status keaktifan tetap dipegang {@link #getAktif()}, dan
	 * keduanya dapat menyimpang -- seorang anggota bisa memiliki tanggal berhenti sekaligus tetap
	 * berstatus aktif, atau sebaliknya. Alur yang memberhentikan anggota harus mengisi kedua hal
	 * tersebut.</p>
	 *
	 * <p>Keterangan asli yang dipertahankan:</p>
	 *
	 * Tanggal anggota berhenti/keluar dari koperasi (untuk "Buku Anggota" pada template pembukuan).
	 * {@code null} berarti masih aktif sebagai anggota.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_berhenti")
	public Date getTanggalBerhenti() {
		return tanggalBerhenti;
	}

	/**
	 * Menetapkan tanggal anggota berhenti/keluar. Ingat bahwa ini tidak menonaktifkan anggota --
	 * panggil juga {@link #setAktif(Boolean)} bila keanggotaan memang harus berakhir.
	 *
	 * @param tanggalBerhenti tanggal berhenti, {@code null} berarti masih tercatat sebagai anggota
	 */
	public void setTanggalBerhenti(Date tanggalBerhenti) {
		this.tanggalBerhenti = tanggalBerhenti;
	}

	/**
	 * Mengembalikan alasan anggota berhenti atau keluar, misalnya meninggal, pindah, atau
	 * mengundurkan diri.
	 *
	 * <p>Teks bebas tanpa daftar nilai baku, sehingga tidak dapat dipakai sebagai dasar
	 * pengelompokan yang andal pada laporan keanggotaan. Kolom bertipe {@code text} sehingga
	 * panjangnya tidak dibatasi.</p>
	 *
	 * @return alasan berhenti, atau {@code null}
	 */
	@Column(name = "alasan_berhenti", columnDefinition = "text")
	public String getAlasanBerhenti() {
		return alasanBerhenti;
	}

	/**
	 * Mengisi alasan anggota berhenti/keluar.
	 *
	 * @param alasanBerhenti alasan berhenti dalam teks bebas
	 */
	public void setAlasanBerhenti(String alasanBerhenti) {
		this.alasanBerhenti = alasanBerhenti;
	}

	/**
	 * Menyatakan apakah anggota tergolong <b>pihak terkait</b> koperasi, yaitu orang yang punya
	 * hubungan istimewa dengan pengurus seperti pengurus atau pengawas itu sendiri.
	 *
	 * <p>Penanda ini memperketat Batas Maksimum Pemberian Pinjaman: pihak terkait dibatasi sepuluh
	 * persen dari Modal Sendiri, sedangkan pihak tidak terkait lima belas persen. Karena itu
	 * pengisiannya bukan sekadar administratif -- ia langsung memperkecil pagu pinjaman yang boleh
	 * diterima anggota, dan salah mengisinya berarti melonggarkan batas kehati-hatian yang justru
	 * ditujukan bagi orang dalam.</p>
	 *
	 * <p><b>Null-safe:</b> kolom yang belum diisi dibaca sebagai {@code false}, artinya anggota
	 * dianggap bukan pihak terkait. Nilai bawaan ini bersifat longgar, sehingga pengurus yang belum
	 * ditandai akan dinilai dengan pagu yang lebih besar. Normalisasi tidak ditulis balik ke field,
	 * sehingga getter ini murni. Perhatikan bahwa penandaan di sini tidak diturunkan otomatis dari
	 * {@link PengurusKoperasi} -- keduanya adalah pencatatan terpisah dan harus dijaga selaras secara
	 * manual.</p>
	 *
	 * <p>Keterangan asli yang dipertahankan:</p>
	 *
	 * {@code true} bila anggota merupakan <b>pihak terkait</b> (mis. pengurus/pengawas). Dipakai untuk
	 * perhitungan Batas Maksimum Pemberian Pinjaman (BMPP): pihak terkait dibatasi 10% dari Modal
	 * Sendiri, sedangkan pihak tidak terkait 15%. Default {@code false}.
	 */
	@Column(name = "pihak_terkait")
	public Boolean getPihakTerkait() {
		return pihakTerkait == null ? false : pihakTerkait;
	}

	/**
	 * Menetapkan penanda pihak terkait. Perubahan nilai ini mengubah pagu Batas Maksimum Pemberian
	 * Pinjaman anggota dari lima belas persen menjadi sepuluh persen Modal Sendiri, atau sebaliknya.
	 *
	 * @param pihakTerkait {@code true} bila anggota adalah pihak terkait; {@code null} dibaca sebagai
	 *                     {@code false}
	 */
	public void setPihakTerkait(Boolean pihakTerkait) {
		this.pihakTerkait = pihakTerkait;
	}

	/**
	 * Mengembalikan pengguna yang mendaftarkan anggota ini.
	 *
	 * <p>Berbeda dari bayangan audit {@link #getOleh()} yang mencatat pengubah <i>terakhir</i>, relasi
	 * ini mencatat pembuat dan tidak berubah sepanjang umur baris. Pemanggilannya dapat memicu
	 * pemuatan dari basis data lewat {@code check(...)}.</p>
	 *
	 * @return pengguna pembuat, atau {@code null} bila tidak tercatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna yang mendaftarkan anggota ini.
	 *
	 * @param dibuatOleh pengguna pembuat, boleh {@code null}
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan data guru yang menjadi identitas anggota ini, bila ada. Pemanggilannya dapat
	 * memicu pemuatan dari basis data dan menulis balik ke field.
	 *
	 * @return data guru, atau {@code null} bila anggota bukan guru
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan data guru sebagai identitas anggota. Ikut memengaruhi nama, kode identitas, jenis
	 * identitas, tipe anggota, dan -- lewat sekolah -- juga {@link #getSatuanKerja()}.
	 *
	 * @param guru data guru, boleh {@code null}
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan data siswa yang menjadi identitas anggota ini, bila ada. Pemanggilannya dapat
	 * memicu pemuatan dari basis data dan menulis balik ke field.
	 *
	 * @return data siswa, atau {@code null} bila anggota bukan siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan data siswa sebagai identitas anggota. Selain memengaruhi nama dan identitas, relasi
	 * ini juga mengambil alih {@link #getKelasLesDipilih()} dari data siswa.
	 *
	 * @param siswa data siswa, boleh {@code null}
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan satuan kerja (tenant akuntansi/anggaran) pemilik anggota, menurunkannya dari
	 * rantai relasi orang bila belum ditetapkan.
	 *
	 * <h4>Tenant langsung, berbeda dari koperasi</h4>
	 * <p>Berbeda dari {@link #getKoperasi()} yang hanya dapat dicapai lewat dua tingkat
	 * ketidaklangsungan, satuan kerja tersedia sebagai kolom langsung pada entity ini. Keduanya
	 * adalah sumbu tenant yang berbeda dan tidak saling menyiratkan: satuan kerja dipakai modul
	 * akuntansi dan anggaran, sedangkan koperasi dipakai domain usaha koperasi.</p>
	 *
	 * <h4>Urutan penurunan</h4>
	 * <p>Kandidat diperiksa berurutan dan yang pertama cocok dipakai:</p>
	 * <ol>
	 * <li>Satuan kerja milik pegawai.</li>
	 * <li>Satuan kerja jurusan dosen, tetapi hanya bila jurusan tersebut menyatakan dosennya memang
	 * harus memakai satuan kerja.</li>
	 * <li>Satuan kerja fakultas dosen, dengan syarat penanda serupa pada fakultas.</li>
	 * <li>Satuan kerja perguruan tinggi dosen, dengan syarat penanda serupa.</li>
	 * <li>Satuan kerja sekolah guru, dengan syarat penanda serupa pada sekolah.</li>
	 * <li>Satuan kerja akun pengguna yang tertaut.</li>
	 * </ol>
	 * <p>Bila semuanya gagal dan field masih kosong, dijalankan rangkaian cadangan: satuan kerja
	 * sekolah guru atau perguruan tinggi dosen diambil <b>tanpa</b> memeriksa penanda "harus memakai
	 * satuan kerja" -- longgar dibandingkan jalur utama di atas. Sebagai upaya terakhir, dan hanya
	 * untuk baris yang belum pernah tersimpan, satuan kerja diambil dari pengguna yang sedang login
	 * atau dari perpustakaan aktif.</p>
	 *
	 * <h4>Peringatan pemakaian</h4>
	 * <p><b>Getter ini destruktif, mahal, dan bergantung pada konteks sesi.</b> Hasil penurunan
	 * ditulis balik ke field sehingga ikut ter-flush; penelusuran menembus beberapa tingkat relasi
	 * lazy sehingga dapat menerbitkan banyak kueri; dan cabang terakhir membaca pengguna yang sedang
	 * login. Konsekuensinya, tenant sebuah anggota baru dapat ditentukan oleh siapa yang kebetulan
	 * membuka layar tersebut. Pada proses latar belakang yang tidak punya sesi pengguna, cabang itu
	 * menghasilkan {@code null} dan anggota berakhir tanpa satuan kerja. Untuk data yang tenant-nya
	 * harus pasti, tetapkan nilainya eksplisit lewat {@link #setSatuanKerja(SatuanKerja)}.</p>
	 *
	 * <p>Seluruh kesalahan pada rangkaian cadangan ditelan dan hanya direkam ke audit kesalahan,
	 * sehingga kegagalan penurunan tidak terlihat oleh pemanggil dan berakhir sebagai {@code null}
	 * yang senyap. Perhatikan pula bahwa method ini dapat mengembalikan {@code null} pada banyak
	 * jalur, sementara sejumlah pemanggil di modul lain mengasumsikan tenant selalu ada.</p>
	 *
	 * @return satuan kerja pemilik anggota, atau {@code null} bila tidak dapat ditentukan
	 */
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
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik anggota secara eksplisit. Nilai yang ditetapkan di sini tetap
	 * dapat tertimpa oleh {@link #getSatuanKerja()} bila anggota tertaut pegawai, dosen, guru, atau
	 * akun pengguna yang membawa satuan kerja sendiri.
	 *
	 * @param satuanKerja satuan kerja pemilik, boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan nama pengguna portal anggota, sudah dipangkas spasi, atau {@code null} bila
	 * kosong.
	 *
	 * <p>Hanya relevan bagi anggota yang diberi akses login mandiri. Ini berbeda dari relasi
	 * {@link #getTbmuser()} yang menautkan anggota ke akun pengguna aplikasi.</p>
	 *
	 * @return nama pengguna portal, atau {@code null}
	 */
	public String getUserid() {
		return userid == null || userid.trim().isEmpty() ? null : userid.trim();
	}

	/**
	 * Mengisi nama pengguna portal anggota. Berbeda dari getter-nya, setter ini menyimpan nilai apa
	 * adanya tanpa memangkas spasi.
	 *
	 * @param userid nama pengguna portal
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * Mengembalikan kata sandi portal anggota, sudah dipangkas spasi, atau {@code null} bila kosong.
	 *
	 * <p><b>Berbeda dari PIN, kata sandi ini tidak disimpan dalam bentuk ber-hash</b> -- nilainya
	 * tersimpan apa adanya pada kolom dan dikembalikan utuh oleh getter ini. Jangan menampilkannya di
	 * antarmuka, menuliskannya ke log, atau menyertakannya dalam muatan yang dikirim ke peramban.
	 * Bandingkan dengan {@link #verifikasiPin(String)} yang memperlihatkan cara penyimpanan
	 * kredensial yang seharusnya.</p>
	 *
	 * @return kata sandi portal, atau {@code null}
	 */
	public String getPass() {
		return pass == null || pass.trim().isEmpty() ? null : pass.trim();
	}

	/**
	 * Mengisi kata sandi portal anggota. Nilai disimpan apa adanya tanpa hashing maupun pemangkasan
	 * spasi.
	 *
	 * @param pass kata sandi portal
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Mengembalikan berkas pengajuan calon anggota yang menjadi asal-usul baris anggota ini.
	 *
	 * <p>Terisi bagi anggota yang lahir dari alur pendaftaran resmi, dan {@code null} bagi anggota
	 * yang dibuat langsung oleh admin atau otomatis saat transaksi POS. Berguna untuk menelusuri
	 * kembali dokumen persetujuan keanggotaan. Pemanggilannya dapat memicu pemuatan dari basis
	 * data.</p>
	 *
	 * @return berkas pengajuan calon anggota, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_anggota_koperasi", nullable = true)
	public CalonAnggotaKoperasi getCalonAnggotaKoperasi() {
		calonAnggotaKoperasi = check(calonAnggotaKoperasi);
		return calonAnggotaKoperasi;
	}

	/**
	 * Menautkan berkas pengajuan calon anggota sebagai asal-usul baris anggota ini.
	 *
	 * @param calonAnggotaKoperasi berkas pengajuan calon anggota, boleh {@code null}
	 */
	public void setCalonAnggotaKoperasi(CalonAnggotaKoperasi calonAnggotaKoperasi) {
		this.calonAnggotaKoperasi = calonAnggotaKoperasi;
	}

	/**
	 * Mengembalikan daftar kelas les pilihan <b>apa adanya</b> dari field, tanpa mengambil alih dari
	 * data siswa dan tanpa membersihkan koma berlebih.
	 *
	 * <p>Padanan murni dari {@link #getKelasLesDipilih()}, diberi awalan {@code ambil} agar tidak
	 * diperlakukan Hibernate sebagai properti. Nilai {@code null} dinormalkan menjadi string kosong
	 * sehingga aman langsung dipakai.</p>
	 *
	 * @return daftar id kelas les berpemisah koma, atau string kosong
	 */
	public String ambilKelasLesDipilih() {
		return kelasLesDipilih == null ? "" : kelasLesDipilih.trim();
	}

	/**
	 * Mengembalikan daftar kelas les yang dipilih, dalam bentuk deretan id berpemisah koma.
	 *
	 * <p>Method ini melaksanakan kontrak abstrak milik {@link ais.database.model.VOSiswa}, yang
	 * memakainya untuk mengurai daftar id dan memuat objek kelas les terkait.</p>
	 *
	 * <h4>Dua perilaku yang berbeda</h4>
	 * <p>Bila anggota tertaut data siswa, daftar diambil alih sepenuhnya dari siswa tersebut --
	 * nilai yang tersimpan pada anggota diabaikan dan tertimpa. Bila tidak, nilai field dirapikan
	 * lebih dahulu: dibungkus koma di kedua ujung lalu koma ganda dipadatkan, dengan pemeriksaan
	 * tambahan untuk membuang sisa string yang hanya berisi koma.</p>
	 *
	 * <p>Perlu dicatat bahwa pemadatan koma ganda dilakukan dengan tiga kali penggantian berurutan,
	 * bukan dengan pengulangan sampai tuntas, sehingga deretan koma yang sangat panjang tidak
	 * dijamin bersih seluruhnya. Demikian pula pemeriksaan sisa hanya menangani sampai tiga koma
	 * berturut-turut.</p>
	 *
	 * <p><b>Getter ini destruktif:</b> hasil perapian maupun hasil pengambilalihan dari siswa
	 * ditulis balik ke field, sehingga pembacaan dapat menghasilkan UPDATE. Gunakan
	 * {@link #ambilKelasLesDipilih()} bila hanya butuh nilai tersimpan.</p>
	 *
	 * @return daftar id kelas les berpemisah koma, atau string kosong bila tidak ada
	 */
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

	/**
	 * Menetapkan daftar kelas les pilihan sebagai deretan id berpemisah koma. Nilai ini akan
	 * tertimpa oleh {@link #getKelasLesDipilih()} bila anggota tertaut data siswa.
	 *
	 * @param kelasLesDipilih daftar id kelas les berpemisah koma
	 */
	public void setKelasLesDipilih(String kelasLesDipilih) {
		this.kelasLesDipilih = kelasLesDipilih;
	}

	/**
	 * Mengembalikan tanggal kedaluwarsa keanggotaan.
	 *
	 * <p>{@code null} berarti keanggotaan tidak berbatas waktu. Bila terisi dan sudah terlewati,
	 * {@link #getAktif()} akan menonaktifkan anggota secara permanen pada pembacaan berikutnya --
	 * lihat catatan satu arah pada getter tersebut sebelum mengubah nilai ini.</p>
	 *
	 * @return tanggal kedaluwarsa, atau {@code null} bila tidak berbatas waktu
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_kadaluarsa")
	public Date getTanggalKadaluarsa() {
		return tanggalKadaluarsa;
	}

	/**
	 * Menetapkan tanggal kedaluwarsa keanggotaan.
	 *
	 * <p>Memperpanjang atau mengosongkan tanggal ini <b>tidak</b> mengaktifkan kembali anggota yang
	 * sudah terlanjur dinonaktifkan karena kedaluwarsa; panggil juga {@link #setAktif(Boolean)}.</p>
	 *
	 * @param tanggalKadaluarsa tanggal kedaluwarsa, {@code null} berarti tidak berbatas waktu
	 */
	public void setTanggalKadaluarsa(Date tanggalKadaluarsa) {
		this.tanggalKadaluarsa = tanggalKadaluarsa;
	}

	/**
	 * Mengembalikan data calon siswa yang tertaut pada anggota ini, bila ada.
	 *
	 * <p>Dipakai bagi pendaftar yang sudah menjadi anggota koperasi sebelum berstatus siswa penuh.
	 * Berbeda dari relasi orang lainnya, relasi ini <b>tidak</b> ikut dibaca oleh
	 * {@link #getNama()}, {@link #getKodeIdentitas()}, {@link #getTipeAnggotaKoperasi()}, maupun
	 * {@link #getJenisIdentitasAnggotaKoperasi()}. Anggota yang hanya tertaut calon siswa karena itu
	 * tetap memakai nama yang dientri manual dan jatuh ke tipe serta jenis identitas bawaan.</p>
	 *
	 * @return data calon siswa, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menautkan data calon siswa pada anggota ini.
	 *
	 * @param calonSiswa data calon siswa, boleh {@code null}
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

}
