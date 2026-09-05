package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Sesi kas (shift) kasir apotek — IR-06.
 *
 * <p><b>Mengapa TIDAK memakai ulang {@code sesi_kas_*} POS umum.</b> Laporan
 * tutup kas POS umum ({@code ais.action.master.koperasi.helper.SesiKasUtil})
 * menghitung uang dari {@code koperasi.pembelian_anggota_koperasi}. Penjualan
 * apotek tidak pernah ditulis ke sana: jejaknya di
 * {@code sirs.detail_transaksi_pasien} (kode transaksi {@code AJ}) dan
 * pembayarannya di {@code sirs.apotik_pembayaran_transaksi}. Memakainya apa
 * adanya akan melaporkan penjualan tunai apotek sebesar NOL dan memunculkan
 * selisih kas sebesar seluruh penerimaan hari itu — angka yang salah, bukan
 * sekadar kurang lengkap.</p>
 *
 * <p>Tabel BARU sehingga {@code hbm2ddl=update} membuatnya berikut tabel
 * auditnya; tidak ada migrasi manual untuk entity ini.</p>
 *
 * <p><b>Angka dihitung server.</b> {@link #getTotalTunaiSistem()} dan
 * {@link #getSelisih()} diisi oleh {@code ApotikSesiKasHelper} dari data
 * pembayaran, BUKAN dari nilai kiriman klien. Yang boleh datang dari kasir
 * hanyalah modal awal dan hasil hitungan fisik laci; kalau angka sistem pun
 * boleh dikirim klien, rekonsiliasi berhenti menjadi pemeriksaan.</p>
 *
 * <h3>Selisih dicatat, tidak menahan</h3>
 *
 * <p>{@code ApotikSesiKasHelper.tutup} menghitung selisih lalu menyimpannya
 * bersama sesi. Ia TIDAK menolak penutupan betapa pun besar selisihnya, dan
 * tidak menuntut keterangan ketika selisihnya besar. Sesi berselisih jutaan
 * rupiah ditutup dengan cara yang persis sama dengan sesi yang pas.</p>
 *
 * <p>Sikap itu dapat dibela: menahan penutupan tidak membuat uang yang hilang
 * kembali, dan kasir yang tidak dapat menutup laci akan tetap pulang — yang
 * berubah hanyalah bahwa hitungannya tidak pernah tercatat. Catatan selisih
 * yang jujur lebih berguna daripada penutupan yang dipaksa cocok. Yang perlu
 * diketahui pembaca kode adalah bahwa tindak lanjutnya sepenuhnya di luar
 * sistem: tidak ada peringatan, tidak ada persetujuan penyelia, dan tidak ada
 * satu pun tempat yang membaca {@link #getSelisih()} kembali untuk
 * menindaklanjutinya. Ia hanya tersimpan.</p>
 *
 * <h3>Cakupan perhitungan: tunai/non-tunai per kasir, ledger penjualan per waktu</h3>
 *
 * <p>Riwayat: sampai dengan revisi ini, {@code ApotikSesiKasHelper.penerimaan}
 * menjumlahkan baris {@link ApotikPembayaranTransaksi} hanya bersyarat
 * {@code b.waktu} berada di antara waktu sesi dibuka dan waktu ia ditutup —
 * TANPA menyaring siapa yang menerima uangnya, padahal baris pembayaran itu
 * sudah membawa identitas kasirnya pada kolom {@code oleh}/{@code oleh_id}.
 * Pada apotek dengan dua kasir yang sesinya bertumpang tindih — keadaan yang
 * justru dibayangkan oleh aturan "satu kasir hanya boleh punya satu sesi
 * BUKA", sebab aturan itu membatasi per kasir dan bukan per apotek — cara lama
 * itu salah bagi keduanya: masing-masing sesi menghitung SELURUH penerimaan
 * tunai apotek pada rentang waktunya sendiri, termasuk uang yang masuk ke laci
 * sebelah, sehingga keduanya tampak memegang uang jauh lebih banyak daripada
 * isi lacinya dan mencatat selisih kurang yang besar tanpa ada uang yang
 * benar-benar hilang.</p>
 *
 * <p><b>Sekarang:</b> {@code penerimaan} menyaring {@code b.oleh_id} terhadap
 * {@link #getUserId()} sesi yang sedang dihitung. Baris milik kasir lain
 * dikecualikan sepenuhnya; baris lama yang {@code oleh_id}-nya kosong (sebelum
 * kolom itu terisi) tidak diam-diam dibuang maupun digabungkan ke sesi ini,
 * melainkan dilaporkan terpisah oleh {@code ApotikSesiKasHelper} sebagai
 * {@code tunaiTanpaKasir}/{@code nonTunaiTanpaKasir} pada respons JSON —
 * TIDAK ikut {@link #getTotalTunaiSistem()}/{@link #getSelisih()}, sehingga
 * baris semacam itu perlu ditelusuri manual bila jumlahnya berarti.</p>
 *
 * <p><b>Ledger penjualan ('AJ') TETAP bercakupan waktu, bukan per kasir.</b>
 * {@code sirs.detail_transaksi_pasien} juga diisi jalur pendaftaran rumah
 * sakit ({@code CommonPendaftaranUtil}) untuk dispensing yang dibebankan ke
 * tagihan pasien, di luar sesi kas apotek mana pun — menyaring kolom pelaku di
 * sana akan diam-diam membuang baris sah. Konsekuensinya:
 * {@code penjualanBerjalan} pada respons {@code ApotikSesiKasHelper} tetap
 * bercakupan seluruh apotek (bahkan seluruh rumah sakit untuk kode 'AJ'),
 * sedangkan tunai/non-tunai di atasnya kini per kasir — sehingga
 * {@code penjualanTanpaMetode} (selisih keduanya) pada apotek berkasir banyak
 * HANYA indikatif, bukan angka yang bisa dipertanggungjawabkan ke satu
 * kasir.</p>
 *
 * @see ApotikPembayaranTransaksi baris pembayaran yang dijumlahkan sesi ini
 * @see ApotikDeliveryOrder#getBiayaKirim() ongkos kirim yang TIDAK ikut dalam rekonsiliasi ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_sesi_kas")
public class ApotikSesiKas extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/**
	 * Status: sesi sedang berjalan, laci belum dihitung.
	 *
	 * <p>Sekaligus nilai yang dikembalikan {@link #getStatus()} bila kolomnya
	 * kosong. Bawaan itu perlu diperhatikan arahnya: ia condong menganggap sesi
	 * masih TERBUKA. Untuk sesi kas arah tersebut yang tepat — sesi yang keliru
	 * dianggap terbuka akan menghalangi kasir membuka sesi baru dan segera
	 * ketahuan, sedangkan sesi yang keliru dianggap tertutup akan lolos dari
	 * rekonsiliasi tanpa ada yang menyadarinya.</p>
	 *
	 * <p>Perhatikan bahwa {@code sesiAktif} mencari dengan
	 * {@code Restrictions.eq("status", STATUS_BUKA)} atas KOLOM, bukan lewat
	 * getter. Baris yang kolom statusnya benar-benar NULL karena itu tidak akan
	 * ditemukan sebagai sesi aktif, meskipun getter menyebutnya BUKA — ia
	 * menjadi sesi yatim yang tidak dapat ditutup lewat aplikasi. Keadaan itu
	 * tidak dapat lahir dari jalur yang ada, sebab {@code buka} selalu mengisi
	 * status secara eksplisit.</p>
	 */
	public static final String STATUS_BUKA = "BUKA";

	/** Status: sesi sudah ditutup dan laci sudah dihitung; tidak dapat ditutup ulang. */
	public static final String STATUS_TUTUP = "TUTUP";

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Akun kasir pemegang sesi. */
	private String userId;

	/** Nama tampil kasir, disalin saat sesi dibuka. */
	private String namaKasir;

	/** {@link #STATUS_BUKA} atau {@link #STATUS_TUTUP}. */
	private String status;

	/** Waktu sesi dibuka; batas bawah rentang perhitungan penerimaan. */
	private Date waktuBuka;

	/** Waktu sesi ditutup; batas atas rentang perhitungan penerimaan. */
	private Date waktuTutup;

	/** Uang tunai di laci saat sesi dibuka. */
	private Double modalAwal;

	/** Hasil hitungan fisik laci saat tutup — satu-satunya angka uang dari kasir. */
	private Double uangFisik;

	/** Penerimaan TUNAI menurut catatan pembayaran, dihitung server saat tutup. */
	private Double totalTunaiSistem;

	/** Penerimaan non-tunai pada periode yang sama (tidak masuk laci). */
	private Double totalNonTunaiSistem;

	/** uangFisik - (modalAwal + totalTunaiSistem). Negatif berarti kurang. */
	private Double selisih;

	/** Catatan bebas; bertambah, tidak menimpa, saat sesi ditutup. */
	private String keterangan;

	/** Nama tampil pelaku pencatatan (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku pencatatan (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Berjalan tepat sekali dalam daur hidup normal sebuah sesi, yaitu ketika
	 * sesi ditutup — satu-satunya UPDATE yang menyentuh baris ini.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Representasi teks: kunci baris dan status, dipisah tanda hubung.
	 *
	 * <p>Membaca field langsung, bukan lewat getter, sehingga aman dipanggil
	 * pada objek yang sudah lepas dari sesi Hibernate. Bagian kosong diganti
	 * string kosong supaya hasilnya tidak pernah memuat kata "null" — perhatikan
	 * bahwa karena field yang dibaca, status yang kosong tampil sebagai kosong
	 * di sini, bukan sebagai BUKA seperti pada {@link #getStatus()}.</p>
	 *
	 * @return teks ringkas untuk log dan layar
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (status == null ? "" : status);
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Akun kasir pemegang sesi.
	 *
	 * <p>Diisi dari sesi yang sedang berjalan ({@code tbmuser.getUserId()}),
	 * tidak pernah dari payload — sehingga seorang kasir tidak dapat membuka
	 * atau menutup sesi atas nama orang lain. Kolom ini pula yang dipakai
	 * {@code sesiAktif} untuk menegakkan aturan "satu kasir, satu sesi
	 * terbuka".</p>
	 *
	 * <p>Penegakan aturan itu tetap berbentuk baca-lalu-tulis di level baris:
	 * {@code buka} memeriksa keberadaan sesi terbuka lebih dulu, baru kemudian
	 * memulai transaksi dan menyimpan. Sejak perbaikan lomba-tulis, jeda itu
	 * diserialkan per kasir lewat {@code pg_try_advisory_lock} bertitik-kunci
	 * {@code ApotikSesiKasHelper.kunciBukaSesiKas(userId)} (idiom yang sama
	 * dengan {@code PengadaanPosApiHelper.kunciSinkronBast}): kunci ini
	 * dipegang PostgreSQL sendiri sehingga berlaku lintas seluruh koneksi ke
	 * basis data yang sama, bukan hanya dalam satu proses JVM — permintaan
	 * buka kedua dari akun yang sama yang datang SELAGI yang pertama masih
	 * diproses akan ditolak dengan pesan "sedang diproses", bukan ikut
	 * menyimpan sesi kedua. Tidak ada batasan unik pada tabel yang menutup
	 * jeda ini; penjagaannya murni advisory lock di atas.</p>
	 *
	 * @return id akun kasir, atau {@code null}
	 */
	@Column(name = "user_id", length = 60)
	public String getUserId() { return userId; }

	/**
	 * Menetapkan akun kasir pemegang sesi.
	 *
	 * @param userId id akun kasir
	 */
	public void setUserId(String userId) { this.userId = userId; }

	/**
	 * Nama tampil kasir, disalin saat sesi dibuka.
	 *
	 * <p>Snapshot yang disengaja, seperti
	 * {@link ApotikDispensingLog#getPelakuNama()}: nama pegawai boleh berubah
	 * dan akun boleh dinonaktifkan, sedangkan riwayat tutup kas harus tetap
	 * menyebut nama yang berlaku pada hari itu. Yang mengikat tetap
	 * {@link #getUserId()}.</p>
	 *
	 * @return nama kasir saat sesi dibuka, atau {@code null}
	 */
	@Column(name = "nama_kasir", length = 120)
	public String getNamaKasir() { return namaKasir; }

	/**
	 * Menetapkan nama tampil kasir.
	 *
	 * @param namaKasir nama kasir
	 */
	public void setNamaKasir(String namaKasir) { this.namaKasir = namaKasir; }

	/**
	 * Status sesi; {@link #STATUS_BUKA} atau {@link #STATUS_TUTUP}.
	 *
	 * <p>Mengembalikan {@link #STATUS_BUKA} bila kolom kosong — lihat penjelasan
	 * arah bawaan dan perbedaan getter-versus-kolom pada
	 * {@link #STATUS_BUKA}.</p>
	 *
	 * <p>Perpindahan status hanya satu arah dalam praktik: {@code tutup} bekerja
	 * atas sesi yang ditemukan {@code sesiAktif}, yang menyaring status BUKA,
	 * sehingga sesi yang sudah TUTUP tidak akan pernah ditemukan lagi untuk
	 * ditutup ulang. Itulah bentuk penegakan aturan "sesi yang sudah tutup tidak
	 * dapat ditutup ulang" — bukan pemeriksaan status yang ditulis eksplisit,
	 * melainkan akibat dari cara sesi dicari.</p>
	 *
	 * @return status sesi; {@link #STATUS_BUKA} bila kolom kosong
	 */
	@Column(name = "status", length = 12, nullable = false)
	public String getStatus() { return status == null ? STATUS_BUKA : status; }

	/**
	 * Menetapkan status sesi.
	 *
	 * @param status {@link #STATUS_BUKA} atau {@link #STATUS_TUTUP}
	 */
	public void setStatus(String status) { this.status = status; }

	/**
	 * Waktu sesi dibuka — batas bawah rentang perhitungan penerimaan.
	 *
	 * <p>Bukan sekadar keterangan: seluruh angka sistem pada sesi ini dihitung
	 * dari pembayaran yang waktunya berada di antara nilai ini dan
	 * {@link #getWaktuTutup()}. Menggeser waktu buka berarti menggeser uang
	 * siapa yang dianggap masuk ke laci ini.</p>
	 *
	 * @return waktu sesi dibuka
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_buka", nullable = false)
	public Date getWaktuBuka() { return waktuBuka; }

	/**
	 * Menetapkan waktu sesi dibuka.
	 *
	 * @param waktuBuka waktu buka
	 */
	public void setWaktuBuka(Date waktuBuka) { this.waktuBuka = waktuBuka; }

	/**
	 * Waktu sesi ditutup — batas atas rentang perhitungan penerimaan.
	 *
	 * <p>Kosong selama sesi masih berjalan. Pada layar status berjalan,
	 * pemanggil memakai waktu sekarang sebagai gantinya, sehingga angka yang
	 * ditampilkan selalu "sampai detik ini".</p>
	 *
	 * @return waktu sesi ditutup, atau {@code null} bila masih terbuka
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_tutup")
	public Date getWaktuTutup() { return waktuTutup; }

	/**
	 * Menetapkan waktu sesi ditutup.
	 *
	 * @param waktuTutup waktu tutup
	 */
	public void setWaktuTutup(Date waktuTutup) { this.waktuTutup = waktuTutup; }

	/**
	 * Uang tunai yang sudah ada di laci saat sesi dibuka.
	 *
	 * <p>Mengembalikan {@code 0} bila kosong, karena ia selalu masuk
	 * perhitungan: kas seharusnya adalah modal awal ditambah penerimaan tunai.
	 * Angka ini datang dari kasir dan itu memang benar — hanya kasir yang tahu
	 * berapa uang kembalian yang ia siapkan di awal giliran. Yang ditolak
	 * pemanggil hanyalah nilai negatif.</p>
	 *
	 * @return modal awal; {@code 0} bila kosong
	 */
	@Column(name = "modal_awal")
	public Double getModalAwal() { return modalAwal == null ? Double.valueOf(0) : modalAwal; }

	/**
	 * Menetapkan modal awal laci.
	 *
	 * @param modalAwal modal awal
	 */
	public void setModalAwal(Double modalAwal) { this.modalAwal = modalAwal; }

	/**
	 * Hasil hitungan fisik laci saat tutup — satu-satunya angka uang dari kasir.
	 *
	 * <p>Mengembalikan {@code null} apa adanya, sengaja TIDAK diganti nol.
	 * Perbedaan itu menentukan: {@code null} berarti sesi belum ditutup dan laci
	 * belum dihitung, sedangkan nol berarti laci dihitung dan isinya benar-benar
	 * kosong. Mengganti kosong dengan nol akan membuat setiap sesi yang masih
	 * berjalan tampak seperti laci yang habis.</p>
	 *
	 * <p>Inilah satu-satunya angka uang yang boleh datang dari kasir saat
	 * penutupan, dan itu memang seharusnya: rekonsiliasi adalah membandingkan
	 * apa yang dihitung orang dengan apa yang dicatat sistem. Kalau angka sistem
	 * pun boleh dikirim, tidak ada lagi yang dibandingkan.</p>
	 *
	 * @return uang fisik hasil hitungan, atau {@code null} bila belum ditutup
	 */
	@Column(name = "uang_fisik")
	public Double getUangFisik() { return uangFisik; }

	/**
	 * Menetapkan hasil hitungan fisik laci.
	 *
	 * @param uangFisik uang fisik, boleh {@code null}
	 */
	public void setUangFisik(Double uangFisik) { this.uangFisik = uangFisik; }

	/**
	 * Penerimaan TUNAI menurut catatan pembayaran, dihitung server saat tutup.
	 *
	 * <p>Mengembalikan {@code null} bila belum dihitung — berlaku pertimbangan
	 * yang sama dengan {@link #getUangFisik()}.</p>
	 *
	 * <p><b>Apa yang dihitung sebagai "tunai".</b> Penggolongannya mengikuti
	 * penanda {@code ada_kembalian} pada master cara pembayaran, dengan cadangan
	 * berupa nama yang mengandung kata "tunai". Aturan itu sengaja disalin dari
	 * laporan shift POS umum supaya definisi "uang yang masuk laci" tidak
	 * dikarang ulang di dua tempat. Harganya: cara pembayaran baru yang lupa
	 * ditandai dan namanya tidak memuat kata "tunai" akan digolongkan non-tunai,
	 * sehingga uangnya tidak masuk kas seharusnya dan muncul sebagai selisih
	 * lebih di laci.</p>
	 *
	 * <p><b>Disaring per kasir.</b> Lihat penjelasan lengkap pada dokumentasi
	 * class: hanya baris {@link ApotikPembayaranTransaksi} yang
	 * {@code oleh_id}-nya cocok dengan {@link #getUserId()} sesi ini yang
	 * dijumlahkan. Baris lama yang {@code oleh_id}-nya kosong TIDAK ikut di
	 * sini — lihat {@code tunaiTanpaKasir} pada respons {@code ApotikSesiKasHelper}.</p>
	 *
	 * @return penerimaan tunai kasir ini menurut sistem, atau {@code null} bila belum dihitung
	 */
	@Column(name = "total_tunai_sistem")
	public Double getTotalTunaiSistem() { return totalTunaiSistem; }

	/**
	 * Menetapkan penerimaan tunai menurut sistem.
	 *
	 * <p>Hanya dipanggil {@code ApotikSesiKasHelper.tutup} dengan angka yang ia
	 * hitung sendiri dari basis data. Mengisinya dari nilai kiriman klien akan
	 * membuat rekonsiliasi membandingkan dua angka yang keduanya berasal dari
	 * pihak yang sama, dan dengan begitu berhenti menjadi pemeriksaan.</p>
	 *
	 * @param totalTunaiSistem penerimaan tunai
	 */
	public void setTotalTunaiSistem(Double totalTunaiSistem) { this.totalTunaiSistem = totalTunaiSistem; }

	/**
	 * Penerimaan non-tunai pada periode yang sama (tidak masuk laci).
	 *
	 * <p>Tidak ikut dalam perhitungan selisih — uang kartu dan QRIS tidak berada
	 * di laci, sehingga membandingkannya dengan hitungan fisik tidak berarti apa
	 * pun. Kolom ini disimpan untuk melengkapi gambaran giliran: berapa yang
	 * masuk lewat kanal lain, dan karena itu berapa bagian penjualan yang
	 * memang tidak seharusnya ada di tangan kasir.</p>
	 *
	 * <p>Berlaku pula catatan cakupan per kasir yang sama seperti
	 * {@link #getTotalTunaiSistem()}.</p>
	 *
	 * @return penerimaan non-tunai kasir ini, atau {@code null} bila belum dihitung
	 */
	@Column(name = "total_non_tunai_sistem")
	public Double getTotalNonTunaiSistem() { return totalNonTunaiSistem; }

	/**
	 * Menetapkan penerimaan non-tunai menurut sistem.
	 *
	 * @param totalNonTunaiSistem penerimaan non-tunai
	 */
	public void setTotalNonTunaiSistem(Double totalNonTunaiSistem) { this.totalNonTunaiSistem = totalNonTunaiSistem; }

	/**
	 * Selisih kas: {@code uangFisik - (modalAwal + totalTunaiSistem)}.
	 *
	 * <p>Negatif berarti uang di laci KURANG dari yang seharusnya; positif
	 * berarti lebih. Keduanya sama-sama perlu diperhatikan — kelebihan uang
	 * biasanya berarti ada penjualan yang tidak tercatat, bukan keberuntungan.
	 * Mengembalikan {@code null} bila sesi belum ditutup.</p>
	 *
	 * <p><b>Tersimpan, tidak menahan, tidak ditindaklanjuti sistem.</b> Nilai
	 * berapa pun tidak menghalangi penutupan sesi, tidak menuntut keterangan,
	 * dan tidak dibaca kembali oleh apa pun. Tidak ada peringatan yang terkirim
	 * dan tidak ada persetujuan penyelia yang diminta. Untuk memakainya,
	 * seseorang harus membuka daftar riwayat sesi dan melihatnya sendiri.</p>
	 *
	 * <p><b>Sudah disaring per kasir, TETAPI abaikan baris tanpa pelaku
	 * tercatat.</b> Sebagaimana dijelaskan pada dokumentasi class,
	 * {@link #getTotalTunaiSistem()} kini hanya menjumlahkan pembayaran yang
	 * {@code oleh_id}-nya cocok dengan kasir ini, sehingga selisih pada apotek
	 * berkasir banyak tidak lagi tercampur uang kasir lain. Yang TIDAK ikut
	 * diperiksa di sini adalah pembayaran lama yang {@code oleh_id}-nya kosong
	 * dan karenanya tidak masuk ke kasir mana pun — jumlahnya ada di
	 * {@code tunaiTanpaKasir} pada respons {@code ApotikSesiKasHelper}, bukan
	 * di selisih ini. Selisih yang tampak pas boleh saja menyembunyikan baris
	 * semacam itu; periksa {@code tunaiTanpaKasir} juga sebelum menyimpulkan
	 * laci benar-benar cocok.</p>
	 *
	 * @return selisih kas, atau {@code null} bila sesi belum ditutup
	 */
	@Column(name = "selisih")
	public Double getSelisih() { return selisih; }

	/**
	 * Menetapkan selisih kas.
	 *
	 * <p>Hanya dipanggil {@code ApotikSesiKasHelper.tutup} dengan hasil
	 * perhitungannya sendiri; tidak pernah dari nilai kiriman klien.</p>
	 *
	 * @param selisih selisih kas
	 */
	public void setSelisih(Double selisih) { this.selisih = selisih; }

	/**
	 * Catatan bebas tentang sesi.
	 *
	 * <p>Bentuk penulisannya perlu diketahui: saat sesi ditutup, keterangan baru
	 * tidak MENIMPA yang lama melainkan disambung di belakangnya dengan pemisah
	 * garis tegak. Pilihan itu tepat — keterangan yang ditulis saat sesi dibuka
	 * (misalnya "modal dari brankas") dan keterangan yang ditulis saat ditutup
	 * (misalnya alasan selisih) sama-sama perlu tersimpan, dan yang belakangan
	 * tidak boleh menghapus yang terdahulu.</p>
	 *
	 * <p>Inilah satu-satunya tempat di seluruh entity yang dapat menjelaskan
	 * MENGAPA selisih terjadi. Karena tidak ada satu pun aturan yang menuntutnya
	 * terisi ketika selisihnya besar, keterangan itu ada sepenuhnya karena
	 * kesediaan kasir menuliskannya.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }

	/**
	 * Menetapkan catatan sesi.
	 *
	 * <p>Menetapkan apa adanya — penyambungan keterangan lama dan baru
	 * dikerjakan pemanggil sebelum memanggil setter ini, bukan di sini.</p>
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Nama tampil pelaku pencatatan (bayangan audit).
	 *
	 * <p>Bertumpang tindih dengan {@link #getUserId()} karena pemanggil mengisi
	 * keduanya dari sesi yang sama saat sesi kas dibuka. Perbedaannya bermakna:
	 * {@code userId} adalah PEMILIK sesi kas, sedangkan {@code oleh} adalah
	 * pelaku perubahan baris. Keduanya akan berbeda pada hari sebuah jalur
	 * penyeliaan menutup sesi milik orang lain — jalur yang belum ada
	 * sekarang.</p>
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	@Column(name = "oleh", length = 60)
	public String getOleh() { return oleh; }

	/**
	 * Menetapkan nama pelaku pencatatan.
	 *
	 * <p>Menetapkan apa adanya, termasuk nilai kosong — berbeda dari entity
	 * tetangga seperti {@link ApotikPembayaranTransaksi#setOleh(String)} yang
	 * menolaknya. Untuk sesi kas, jejak pelaku yang lebih kuat tetap tersimpan
	 * Envers di {@code new_audit.apotik_sesi_kas__audit}, dan pemilik sesi
	 * sendiri tercatat terpisah pada {@link #getUserId()} yang tidak dapat
	 * terhapus oleh penyalinan properti.</p>
	 *
	 * @param oleh nama pelaku
	 */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/**
	 * Identitas akun pelaku pencatatan (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id", length = 60)
	public String getOlehId() { return olehId; }

	/**
	 * Menetapkan id akun pelaku pencatatan.
	 *
	 * <p>Berlaku catatan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku
	 */
	public void setOlehId(String olehId) { this.olehId = olehId; }

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Bergerak tepat sekali dalam daur hidup normal sesi, yaitu saat sesi
	 * ditutup — sehingga ia praktis merupakan salinan
	 * {@link #getWaktuTutup()}.</p>
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
