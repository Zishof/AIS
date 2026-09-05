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

import ais.database.model.GeneralValueObject;

/**
 * Antrean penyiapan obat untuk konsol petugas dan layar publik Instalasi Farmasi.
 *
 * <p>Entity ini sengaja tidak memakai Envers: perubahan operasional tetap membawa
 * waktu dan pelaku pada barisnya, sedangkan tabel audit legacy tidak boleh menjadi
 * prasyarat agar layar pasien dapat digunakan setelah deploy pertama.</p>
 *
 * <h3>Kedudukan dalam alur apotek</h3>
 *
 * <p>Baris di sini adalah <em>papan kerja</em>, bukan dokumen keuangan maupun rekam
 * medis. Satu baris mewakili satu berkas resep yang sedang berjalan di meja
 * penyiapan: pasien sudah menyerahkan resep, petugas menyiapkan obatnya, dan
 * layar di ruang tunggu menampilkan sampai di mana antreannya. Karena itu
 * seluruh isinya berumur pendek — {@code ApotikApiHelper.antreanFarmasiList}
 * hanya membaca baris dengan {@link #getTanggalDibuat()} pada hari berjalan, dan
 * baris kemarin tidak pernah muncul lagi tanpa dihapus. Yang bertahan sebagai
 * bukti transaksi adalah {@code TransaksiMedis}/{@code TransaksiMedisDetail} dan
 * register {@link ApotikNarkotikaLog}, bukan antrean ini.</p>
 *
 * <p>Konsekuensi desain itu: entity ini boleh diubah berkali-kali (status naik
 * dari MENUNGGU ke SELESAI) dan boleh dihapus lewat aksi
 * {@code apotik_antrean_farmasi_hapus}. Tidak ada satu pun angka rupiah, stok,
 * atau kuantitas obat yang disimpan di sini — {@link #getDaftarObat()} hanya
 * salinan teks untuk ditampilkan. Jadi menghapus baris antrean TIDAK merusak
 * pembukuan, stok, maupun jejak penyerahan obat. Itulah alasan aksi hapus
 * disediakan sama sekali, padahal entity apotek lain di paket ini seragam
 * append-only.</p>
 *
 * <h3>Dua pembaca dengan tingkat kerahasiaan berbeda</h3>
 *
 * <p>Tabel ini dibaca oleh dua layar sekaligus, dan keduanya tidak boleh melihat
 * hal yang sama. Konsol petugas di balik meja berhak melihat
 * {@link #getNamaPasien()} dan {@link #getNomorRekamMedis()} apa adanya, karena
 * petugas memang harus memanggil orang yang benar dan mencocokkan berkasnya.
 * Layar publik yang menghadap ruang tunggu TIDAK boleh: nama lengkap dan nomor
 * rekam medis yang terpampang di layar besar dapat dibaca siapa saja yang lewat,
 * termasuk orang yang tidak berkepentingan, dan itu membocorkan siapa sedang
 * berobat apa. Pemisahan ini tidak dikerjakan oleh entity: pemanggil
 * mengirim {@code untuk_layar=true} dan helper menyamarkan kedua field lewat
 * {@code samarkanNama}/{@code samarkanRm} sebelum menaruhnya di JSON. Entity
 * hanya menyimpan bentuk aslinya.</p>
 *
 * <p><b>Batas jujur tentang perlindungan itu.</b> Karena penyamaran dikerjakan
 * pemanggil dan bukan entity, setiap jalur baca BARU yang dibuat kemudian atas
 * tabel ini akan mengembalikan nama dan nomor rekam medis dalam bentuk terang
 * kecuali penulisnya ingat menyamarkannya sendiri. Bentuk aman default TIDAK
 * ada di lapisan ini. Siapa pun yang menambah endpoint, laporan, atau ekspor di
 * atas {@code sirs.antrean_farmasi} harus memutuskan sendiri apakah pembacanya
 * berhak melihat identitas, dan wajib memakai kembali fungsi penyamaran yang
 * sudah ada alih-alih menulis versinya sendiri.</p>
 *
 * <h3>Sumbu apotek: {@link #getTokoId()}</h3>
 *
 * <p>Berbeda dari mayoritas entity paket {@code sirs} yang sama sekali tidak
 * punya sumbu pemisah antar unit, entity ini PUNYA — {@link #getTokoId()} —
 * dan seluruh jalur bacanya benar-benar memfilter dengan kolom itu. Yang perlu
 * dipahami pembaca kode: nilai pemisah tersebut ditentukan oleh pemanggil, bukan
 * oleh entity, dan kekuatan pemisahan itu persis sekuat cara pemanggil
 * menurunkannya. Entity tidak dapat menolak nilai yang salah karena ia tidak
 * tahu siapa yang sedang meminta.</p>
 *
 * @see ApotikItemProfile profil farmasi per item yang menentukan golongan obat
 * @see ApotikDispensingLog jejak pemeriksaan kedua dan konseling atas resep
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "sirs", name = "antrean_farmasi")
public class AntreanFarmasi extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/**
	 * Jenis antrean: obat jadi — diambil langsung dari rak, tanpa peracikan.
	 *
	 * <p>Ketiga konstanta jenis dipakai dua arah. Ke arah masuk,
	 * {@code ApotikApiHelper.jenisAntreanValid} menolak nilai apa pun di luar
	 * ketiganya sehingga kolom {@code jenis} tidak dapat diisi teks bebas dari
	 * klien. Ke arah keluar, konsol petugas memakainya untuk memisahkan papan
	 * kerja: berkas racikan menuntut waktu dan meja yang berbeda dari obat jadi,
	 * sehingga mencampurnya dalam satu daftar membuat estimasi tunggu yang
	 * ditampilkan ke pasien menjadi salah.</p>
	 */
	public static final String JENIS_JADI = "JADI";

	/** Jenis antrean: racikan — obat harus diracik lebih dulu (puyer, kapsul, sirup). */
	public static final String JENIS_RACIKAN = "RACIKAN";

	/** Jenis antrean: campuran — satu berkas memuat obat jadi sekaligus racikan. */
	public static final String JENIS_CAMPURAN = "CAMPURAN";

	/**
	 * Status: berkas diterima, belum ada yang mengerjakan.
	 *
	 * <p>Keempat konstanta status membentuk urutan wajar MENUNGGU &rarr;
	 * DISIAPKAN &rarr; SIAP &rarr; SELESAI, tetapi urutan itu TIDAK ditegakkan di
	 * mana pun. {@code ApotikApiHelper.statusAntreanValid} hanya memeriksa bahwa
	 * nilai yang dikirim termasuk salah satu dari keempatnya; ia tidak memeriksa
	 * status sebelumnya. Artinya satu baris bisa melompat langsung dari MENUNGGU
	 * ke SELESAI, dan bisa pula mundur dari SIAP kembali ke MENUNGGU.</p>
	 *
	 * <p>Kelonggaran itu disengaja dan tidak berbahaya di sini. Antrean adalah
	 * alat bantu koordinasi, bukan bukti bahwa obat sudah diperiksa atau
	 * diserahkan; kalau petugas salah menekan tombol, ia harus bisa
	 * mengembalikannya tanpa membuat berkas baru. Jejak yang benar-benar
	 * dipertaruhkan — pemeriksaan kedua sebelum obat diserahkan — hidup di
	 * {@link ApotikDispensingLog}, yang justru menegakkan aturan pemisahan
	 * pelaku dan tidak boleh dihapus. Jangan pernah memakai status SELESAI di
	 * tabel ini sebagai bukti bahwa obat sudah diperiksa atau diserahkan.</p>
	 */
	public static final String STATUS_MENUNGGU = "MENUNGGU";

	/** Status: sedang disiapkan/diracik petugas. */
	public static final String STATUS_DISIAPKAN = "DISIAPKAN";

	/** Status: obat siap, pasien boleh dipanggil ke loket. */
	public static final String STATUS_SIAP = "SIAP";

	/** Status: obat sudah diserahkan; baris berhenti tampil di layar publik. */
	public static final String STATUS_SELESAI = "SELESAI";

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Apotek/toko pemilik antrean — sumbu pemisah satu-satunya entity ini. */
	private Long tokoId;

	/** Resep sumber bila antrean dibuat dari resep terdaftar; longgar, bukan FK. */
	private Long resepId;

	/** Nomor panggil yang diucapkan/ditampilkan (mis. "A-014"). */
	private String kodeAntrean;

	/** Nomor rekam medis pasien; identitas, disamarkan sebelum tampil di layar publik. */
	private String nomorRekamMedis;

	/** Nama pasien; identitas, disamarkan sebelum tampil di layar publik. */
	private String namaPasien;

	/** Salah satu dari {@link #JENIS_JADI}/{@link #JENIS_RACIKAN}/{@link #JENIS_CAMPURAN}. */
	private String jenis;

	/** Salah satu dari keempat konstanta STATUS_*. */
	private String status;

	/** Loket penyerahan bila apotek memakai lebih dari satu jendela. */
	private String loket;

	/** Salinan JSON nama/jumlah obat untuk ditampilkan; bukan sumber stok. */
	private String daftarObat;

	/** Catatan yang memang boleh terbaca publik (mis. "menunggu konfirmasi dokter"). */
	private String catatanPublik;

	/** Nomor urut harian; menentukan urutan tampil papan antrean. */
	private Integer urutan;

	/** Waktu baris dibuat; sekaligus penentu "hari ini" pada daftar antrean. */
	private Date tanggalDibuat = new Date();

	/** Stempel ubah terakhir; diisi ulang interseptor audit pada setiap update. */
	private Date tanggal_dirubah = new Date();

	/** Nama tampil pelaku perubahan terakhir (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku perubahan terakhir (bayangan audit). */
	private String olehId;

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * <p>{@code insertable = false} menegaskan kolom tidak pernah ikut dalam
	 * INSERT: nilai apa pun yang tersisa di objek Java diabaikan dan basis data
	 * yang menentukan. Ini mencegah klien mendikte kunci baris.</p>
	 *
	 * @return kunci baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }

	/**
	 * Menetapkan kunci baris.
	 *
	 * <p>Hanya untuk Hibernate saat memuat baris yang sudah ada; kode aplikasi
	 * tidak pernah perlu memanggilnya.</p>
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Apotek/toko pemilik antrean ini — sumbu pemisah satu-satunya entity ini.
	 *
	 * <p>Seluruh jalur baca dan tulis antrean menyaring dengan nilai ini:
	 * {@code antreanFarmasiList} menambahkan {@code Restrictions.eq("tokoId",
	 * tokoId)} pada kriterianya, sementara {@code antreanFarmasiSimpan},
	 * {@code antreanFarmasiStatus}, dan {@code antreanFarmasiHapus} memuat baris
	 * berdasarkan id lalu MENOLAK bila {@code tokoId} baris tidak sama dengan
	 * toko aktif pemanggil. Pemeriksaan terakhir itu penting dan sudah benar
	 * bentuknya: memuat-lalu-bandingkan menutup celah "tebak id" yang akan
	 * terbuka bila baris langsung diubah lewat query ber-id saja.</p>
	 *
	 * <p><b>Dari mana nilai pembanding itu datang.</b> Pemanggil menurunkannya
	 * lewat {@code ApotikApiHelper.tokoId(user, request)}, yang lebih dulu
	 * mencoba {@code user.getPedagang().getToko().getId()} — turunan sesi, tidak
	 * dapat dipalsukan klien. Namun bila rantai itu putus di titik mana pun
	 * (pengguna tanpa {@code Pedagang}, {@code Pedagang} tanpa {@code Toko}, atau
	 * pengecualian saat menelusurinya), metode itu JATUH KEMBALI membaca
	 * {@code toko_id} dari payload permintaan. Sejak titik itu, "toko aktif"
	 * bukan lagi fakta tentang siapa yang login melainkan angka yang dikirim
	 * pemanggil, dan seluruh penyaringan di paragraf sebelumnya menyaring
	 * memakai nilai yang dipilih sendiri oleh peminta. Pengguna modul rumah sakit
	 * pada umumnya memang tidak terikat ke {@code Toko} mana pun, sehingga
	 * cabang cadangan itu bukan kasus langka.</p>
	 *
	 * <p>Karena baris antrean memuat {@link #getNamaPasien()} dan
	 * {@link #getNomorRekamMedis()}, dan karena {@code antreanFarmasiList}
	 * mengembalikan keduanya tanpa penyamaran ketika {@code untuk_layar} tidak
	 * disetel, akibat dari cabang cadangan itu bersifat kerahasiaan pasien, bukan
	 * sekadar kerapian data. Catatan ini ditulis di sini supaya siapa pun yang
	 * menambah jalur baca baru tahu bahwa nilai yang ia pakai untuk menyaring
	 * belum tentu berasal dari sesi.</p>
	 *
	 * @return id toko/apotek pemilik baris, atau {@code null} pada data lama
	 */
	@Column(name = "toko_id", nullable = false)
	public Long getTokoId() { return tokoId; }

	/**
	 * Menetapkan apotek/toko pemilik antrean.
	 *
	 * <p>Diisi sekali saat baris dibuat dan tidak pernah diubah sesudahnya —
	 * memindahkan antrean antar apotek tidak punya makna operasional.</p>
	 *
	 * @param tokoId id toko/apotek pemilik baris
	 */
	public void setTokoId(Long tokoId) { this.tokoId = tokoId; }

	/**
	 * Id resep sumber bila antrean dibangun dari resep terdaftar.
	 *
	 * <p>Sengaja {@code Long} biasa, BUKAN relasi {@code @ManyToOne} ke
	 * {@link Resep}. Alasannya operasional: antrean sering dibuat lebih dulu
	 * daripada resepnya terdaftar rapi — pasien menyerahkan kertas resep, petugas
	 * langsung memasukkannya ke papan supaya nomor tunggu terbit, dan pencatatan
	 * resep formalnya menyusul. Relasi keras akan memaksa resep ada lebih dulu
	 * dan menahan penerbitan nomor antrean karena alasan administratif.</p>
	 *
	 * <p>Harga dari keputusan itu: tidak ada jaminan referensial. Nilai di sini
	 * boleh menunjuk resep yang sudah dihapus, dan pembaca WAJIB memperlakukan
	 * hasil pencariannya sebagai mungkin-kosong. Jangan menaikkannya menjadi FK
	 * tanpa lebih dulu membersihkan baris yatim yang sudah terlanjur ada.</p>
	 *
	 * @return id resep sumber, atau {@code null} bila antrean dibuat manual
	 */
	@Column(name = "resep_id")
	public Long getResepId() { return resepId; }

	/**
	 * Menetapkan id resep sumber.
	 *
	 * @param resepId id resep, boleh {@code null}
	 */
	public void setResepId(Long resepId) { this.resepId = resepId; }

	/**
	 * Nomor panggil yang diucapkan petugas dan ditampilkan di layar (mis. "A-014").
	 *
	 * <p>Ini satu-satunya penanda antrean yang boleh dibaca publik: ia menyebut
	 * giliran tanpa menyebut orang. Karena itu nomor panggil TIDAK boleh
	 * diturunkan dari nomor rekam medis atau tanggal lahir — pola semacam itu
	 * mengubah papan antrean menjadi jalur bocor identitas. Pemanggil membiarkan
	 * kode diisi klien lalu meng-upper-case-kannya; kalau kosong, ia membangkitkan
	 * kode sendiri dari nomor urut harian.</p>
	 *
	 * @return kode antrean, tidak boleh {@code null} pada baris tersimpan
	 */
	@Column(name = "kode_antrean", nullable = false, length = 30)
	public String getKodeAntrean() { return kodeAntrean; }

	/**
	 * Menetapkan nomor panggil antrean.
	 *
	 * @param kodeAntrean kode antrean
	 */
	public void setKodeAntrean(String kodeAntrean) { this.kodeAntrean = kodeAntrean; }

	/**
	 * Nomor rekam medis pasien — identitas, disamarkan sebelum tampil publik.
	 *
	 * <p>Nilai yang tersimpan selalu bentuk aslinya. Penyamaran untuk layar ruang
	 * tunggu dikerjakan pemanggil ({@code samarkanRm}), bukan getter ini, karena
	 * konsol petugas memang harus melihat nomor utuh untuk mencocokkan berkas.
	 * Getter yang menyamarkan akan merusak kegunaan konsol dan — lebih buruk —
	 * membuat nilai yang dibaca berbeda dari yang disimpan, pola yang sudah
	 * berulang kali menyulitkan penelusuran di basis kode ini.</p>
	 *
	 * <p>Konsekuensinya, setiap pembaca baru bertanggung jawab sendiri memutuskan
	 * apakah pembacanya berhak melihat nomor ini.</p>
	 *
	 * @return nomor rekam medis apa adanya, atau {@code null}
	 */
	@Column(name = "nomor_rekam_medis", length = 80)
	public String getNomorRekamMedis() { return nomorRekamMedis; }

	/**
	 * Menetapkan nomor rekam medis pasien.
	 *
	 * @param nomorRekamMedis nomor rekam medis
	 */
	public void setNomorRekamMedis(String nomorRekamMedis) { this.nomorRekamMedis = nomorRekamMedis; }

	/**
	 * Nama pasien — identitas, disamarkan sebelum tampil publik.
	 *
	 * <p>Berlaku pertimbangan yang sama dengan {@link #getNomorRekamMedis()}:
	 * disimpan utuh, disamarkan oleh pemanggil hanya untuk layar publik.</p>
	 *
	 * @return nama pasien apa adanya
	 */
	@Column(name = "nama_pasien", nullable = false, length = 160)
	public String getNamaPasien() { return namaPasien; }

	/**
	 * Menetapkan nama pasien.
	 *
	 * @param namaPasien nama pasien
	 */
	public void setNamaPasien(String namaPasien) { this.namaPasien = namaPasien; }

	/**
	 * Jenis pekerjaan penyiapan; salah satu konstanta {@code JENIS_*}.
	 *
	 * @return jenis antrean
	 */
	@Column(name = "jenis", nullable = false, length = 20)
	public String getJenis() { return jenis; }

	/**
	 * Menetapkan jenis pekerjaan penyiapan.
	 *
	 * <p>Tidak memvalidasi; pemanggil sudah menolak nilai di luar daftar lewat
	 * {@code jenisAntreanValid} sebelum menyentuh entity.</p>
	 *
	 * @param jenis salah satu konstanta {@code JENIS_*}
	 */
	public void setJenis(String jenis) { this.jenis = jenis; }

	/**
	 * Tahap penyiapan saat ini; salah satu konstanta {@code STATUS_*}.
	 *
	 * <p>Bukan bukti bahwa obat sudah diperiksa atau diserahkan — lihat
	 * penjelasan pada {@link #STATUS_MENUNGGU}.</p>
	 *
	 * @return status antrean
	 */
	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() { return status; }

	/**
	 * Menetapkan tahap penyiapan.
	 *
	 * <p>Tidak memeriksa urutan perpindahan; itu memang dibiarkan longgar agar
	 * salah tekan dapat dikoreksi.</p>
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) { this.status = status; }

	/**
	 * Loket penyerahan bila apotek memakai lebih dari satu jendela.
	 *
	 * @return nama/nomor loket, atau {@code null} bila apotek berloket tunggal
	 */
	@Column(name = "loket", length = 40)
	public String getLoket() { return loket; }

	/**
	 * Menetapkan loket penyerahan.
	 *
	 * @param loket nama/nomor loket
	 */
	public void setLoket(String loket) { this.loket = loket; }

	/**
	 * Salinan teks daftar obat untuk ditampilkan di konsol — BUKAN sumber stok.
	 *
	 * <p>Disimpan sebagai JSON array dalam kolom {@code text} dan dibaca kembali
	 * apa adanya oleh pemanggil; bila isinya bukan JSON yang sah, pemanggil
	 * menggantinya dengan array kosong alih-alih menggagalkan seluruh daftar
	 * antrean. Sikap memaafkan itu tepat untuk kolom tampilan.</p>
	 *
	 * <p><b>Jangan menjadikannya sumber kebenaran.</b> Kuantitas yang benar-benar
	 * mengurangi stok hidup di {@link ApotikBatchKonsumsi} dan
	 * {@code TransaksiMedisDetail}; kuantitas obat terkendali yang wajib
	 * dilaporkan hidup di {@link ApotikNarkotikaLog}. Kolom ini tidak pernah
	 * direkonsiliasi dengan ketiganya, dan mengubahnya tidak mengubah apa pun
	 * selain yang terbaca di layar. Laporan apa pun yang menjumlahkan angka dari
	 * sini akan menghasilkan bilangan yang tampak masuk akal namun tidak
	 * berdasar.</p>
	 *
	 * @return JSON array daftar obat sebagai teks, atau {@code null}
	 */
	@Column(name = "daftar_obat", columnDefinition = "text")
	public String getDaftarObat() { return daftarObat; }

	/**
	 * Menetapkan salinan teks daftar obat.
	 *
	 * @param daftarObat JSON array sebagai teks
	 */
	public void setDaftarObat(String daftarObat) { this.daftarObat = daftarObat; }

	/**
	 * Catatan yang memang layak terbaca publik.
	 *
	 * <p>Namanya menyatakan kontraknya: isi kolom ini ikut dikirim ke layar ruang
	 * tunggu TANPA penyamaran apa pun, berbeda dari nama pasien dan nomor rekam
	 * medis. Karena itu jangan pernah menaruh diagnosis, nama obat terkendali,
	 * atau keterangan klinis di sini. Yang pantas: "menunggu konfirmasi dokter",
	 * "obat sedang diracik", "silakan ke kasir dulu".</p>
	 *
	 * @return catatan publik, atau {@code null}
	 */
	@Column(name = "catatan_publik", length = 240)
	public String getCatatanPublik() { return catatanPublik; }

	/**
	 * Menetapkan catatan yang boleh terbaca publik.
	 *
	 * @param catatanPublik catatan singkat non-klinis
	 */
	public void setCatatanPublik(String catatanPublik) { this.catatanPublik = catatanPublik; }

	/**
	 * Nomor urut harian; menentukan urutan tampil papan antrean.
	 *
	 * <p>Pemanggil mengisinya dengan {@code max(urutan) + 1} atas baris toko yang
	 * sama pada hari berjalan. Perhitungan itu berjalan di luar kunci baris,
	 * sehingga dua pendaftaran yang benar-benar bersamaan dapat memperoleh nomor
	 * urut kembar. Tidak ada batasan unik di basis data yang mencegahnya, dan itu
	 * dibiarkan: akibat terburuknya adalah dua berkas tampil berdampingan dalam
	 * urutan yang ditentukan {@code id} sebagai pengurut kedua — mengganggu, tapi
	 * tidak merusak data apa pun. Bandingkan dengan {@link ApotikPbfDokumen} yang
	 * kodenya memang dijaga batasan unik karena kembar di sana berarti dokumen
	 * utang ganda.</p>
	 *
	 * @return nomor urut harian, atau {@code null}
	 */
	@Column(name = "urutan")
	public Integer getUrutan() { return urutan; }

	/**
	 * Menetapkan nomor urut harian.
	 *
	 * @param urutan nomor urut
	 */
	public void setUrutan(Integer urutan) { this.urutan = urutan; }

	/**
	 * Waktu baris dibuat; sekaligus penentu keanggotaan "hari ini".
	 *
	 * <p>Bukan sekadar stempel informatif. {@code antreanFarmasiList} menyaring
	 * dengan {@code tanggalDibuat >= awal hari} dan {@code < awal besok}, jadi
	 * mengubah nilai ini berarti memindahkan baris keluar-masuk papan antrean
	 * hari berjalan. Nilai awalnya {@code new Date()} pada saat objek dibuat di
	 * memori — bukan saat baris ditulis — sehingga objek yang dibangun lalu
	 * ditahan lama sebelum disimpan akan membawa waktu pembuatan objeknya.
	 * Untuk antrean yang selalu disimpan seketika, selisih itu tidak berarti.</p>
	 *
	 * @return waktu pembuatan baris
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dibuat", nullable = false)
	public Date getTanggalDibuat() { return tanggalDibuat; }

	/**
	 * Menetapkan waktu pembuatan baris.
	 *
	 * @param tanggalDibuat waktu pembuatan
	 */
	public void setTanggalDibuat(Date tanggalDibuat) { this.tanggalDibuat = tanggalDibuat; }

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan stempel {@link #getTanggal_dirubah()}.
	 *
	 * <p>Dilakukan lewat {@code AuditTimestampInterceptor.ubah(this)} supaya
	 * seluruh entity memakai satu sumber waktu yang sama. Kait ini TIDAK berjalan
	 * pada INSERT — nilai awal field-lah yang berlaku di sana.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	/**
	 * Stempel perubahan terakhir.
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/**
	 * Nama tampil pelaku perubahan terakhir (bayangan audit).
	 *
	 * @return nama pelaku, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() { return oleh; }

	/**
	 * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
	 *
	 * <p>Setter ini menolak {@code null} dan teks yang hanya berisi spasi dengan
	 * cara diam: nilai lama dipertahankan dan tidak ada tanda apa pun bahwa
	 * penetapan tidak jadi dilakukan. Bentuk ini seragam di seluruh basis kode
	 * dan merupakan KEHARUSAN TEKNIS, bukan kelalaian.</p>
	 *
	 * <p>Alasannya begini. Kolom {@code oleh}/{@code oleh_id} adalah bayangan
	 * audit: jawaban atas "siapa yang terakhir menyentuh baris ini" yang menempel
	 * pada barisnya sendiri, terpisah dari Envers. Entity ini melewati banyak
	 * jalur — formulir ZK, API JSON, penyalinan objek, pemuatan ulang Hibernate —
	 * dan sebagian di antaranya menyalin seluruh properti tanpa tahu mana yang
	 * bermakna, termasuk memanggil setter ini dengan string kosong dari kolom
	 * yang tidak diisi. Kalau setter menurut apa adanya, satu penyalinan lugu
	 * sudah cukup untuk menghapus nama pelaku yang benar dan menggantinya dengan
	 * ruang kosong. Yang hilang bukan kenyamanan: pertanyaan "siapa mengubah ini"
	 * kehilangan jawabannya secara permanen, karena tidak ada yang menyimpan
	 * nilai sebelumnya di baris itu.</p>
	 *
	 * <p>Maka pilihannya adalah antara kehilangan jejak akibat penulisan kosong
	 * yang tidak disengaja, atau menolak penulisan kosong sepenuhnya. Yang kedua
	 * dipilih. Harganya: jejak ini tidak dapat DIKOSONGKAN lagi lewat setter —
	 * satu-satunya cara adalah UPDATE langsung ke basis data. Untuk kolom yang
	 * memang hanya boleh bertambah jelas, itu harga yang benar.</p>
	 *
	 * <p><b>Yang perlu diketahui pembaca baru:</b> jangan "memperbaiki" setter ini
	 * menjadi penetapan lugas karena tampak seperti anomali. Ia sengaja begini,
	 * dan mengubahnya akan menghapus jejak pelaku di seluruh modul sekaligus,
	 * bukan hanya di entity ini.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }

	/**
	 * Identitas akun pelaku perubahan terakhir (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id")
	public String getOlehId() { return olehId; }

	/**
	 * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }
}
