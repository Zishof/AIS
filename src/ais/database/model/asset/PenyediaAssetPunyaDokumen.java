package ais.database.model.asset;

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

import ais.database.model.GeneralValueObject;

/**
 * Entitas penghubung <b>pemenuhan dokumen legalitas oleh penyedia asset</b> (tabel
 * {@code asset.penyedia_asset_punya_dokumen}) — satu baris menyatakan bahwa seorang
 * {@link PenyediaAsset} telah menyerahkan berkas untuk satu jenis dokumen
 * {@link DokumenPenyediaAsset}, berikut status verifikasi dan tanggal akhir masa berlakunya.
 *
 * <h2>Bentuk relasi: many-to-many lewat entitas penghubung</h2>
 * <p>Kelas ini memikul dua relasi {@code ManyToOne} yang keduanya {@code nullable = false}:
 * {@link #getPenyediaAsset()} dan {@link #getDokumenPenyediaAsset()}. Gabungan keduanya
 * mewujudkan relasi <b>many-to-many</b> antara vendor dan katalog jenis dokumen — satu vendor
 * dapat memiliki banyak jenis dokumen, dan satu jenis dokumen dimiliki banyak vendor — dengan
 * atribut tambahan yang melekat pada pasangannya ({@link #getStatus()},
 * {@link #getKeterangan()}, {@link #getTanggalBerlakuSampai()}). Pola ini disebut
 * <i>association entity</i>, bukan {@code @ManyToMany} murni, justru karena ada atribut yang harus
 * disimpan pada perpotongannya.</p>
 * <p><b>Tidak ada batasan unik pada pasangan (vendor, jenis dokumen).</b> Baik anotasi entitas
 * maupun definisi tabel tidak menyatakan {@code UniqueConstraint} atas kedua kolom kunci asing
 * tersebut, sehingga secara teknis dua baris atau lebih untuk pasangan yang sama dapat tersimpan.
 * Seluruh kode pembaca — {@code PenyediaAssetAction}, {@code PenyediaAssetPunyaDokumenAction},
 * {@code PemesananPengadaanMasterAssetAction}, {@code PenerimaanPengadaanMasterAssetAction},
 * {@code PerjanjianKerjasamaMasterAssetAction}, dan ketiga layar pembayaran — mengambilnya dengan
 * pola yang sama, yaitu {@code createCriteria(...).add(eq("dokumenPenyediaAsset", …))
 * .add(eq("penyediaAsset", …)).setMaxResults(1).uniqueResult()}. Pembatasan
 * {@code setMaxResults(1)} membuat pemanggilan tersebut tidak melempar
 * {@code NonUniqueResultException} bila duplikat benar-benar ada, tetapi konsekuensinya duplikat
 * <b>tertutupi secara diam-diam</b>: hanya satu baris yang terlihat dan tersunting di layar,
 * sementara baris kembarannya tetap ada di basis data, tetap ikut terhitung pada statistik
 * {@code DasboardVendor} dan {@code DasboardAnalisisVendor}, serta dapat memuat status verifikasi
 * yang berbeda dari baris yang tampak. Baris mana yang terpilih bergantung pada urutan yang
 * dikembalikan basis data karena tidak ada pengurutan eksplisit.</p>
 *
 * <h2>Tidak ada validasi kelengkapan dokumen sebelum vendor dipakai</h2>
 * <p>Pembaca wajar menduga bahwa entitas ini menjadi dasar sebuah gerbang — bahwa vendor yang
 * belum melengkapi dokumen wajibnya, atau yang dokumennya belum diverifikasi/sudah kedaluwarsa,
 * tidak boleh dipakai pada transaksi pengadaan baru. <b>Gerbang seperti itu tidak ada.</b></p>
 * <p>Layar-layar pengadaan memang membaca entitas ini, tetapi semata untuk keperluan pencetakan.
 * Pola yang dipakai keenam layar tersebut identik: untuk setiap jenis dokumen di katalog, cari
 * baris pemenuhannya bagi vendor yang bersangkutan, lalu isikan
 * {@code parameters.put("dokumen." + namaJenisDokumen, temp == null ? "" : temp.getKeterangan())}.
 * Ketika baris tidak ditemukan, nilainya sekadar dikosongkan dan proses berjalan terus — tidak
 * ada peringatan, tidak ada penolakan. Vendor yang sama sekali belum menyerahkan berkas apa pun
 * tetap dapat dipilih di {@code AmbilDataPenyediaAssetBanbox} dan tetap dapat dipakai pada
 * pemesanan (PO), penerimaan, perjanjian kerja sama, maupun pembayaran DP, termin, dan
 * pengadaan.</p>
 * <p>Demikian pula {@link #getStatus()}: nilai {@link #VERIFIKASI} tidak membuka apa pun dan nilai
 * {@link #BELUM} tidak menutup apa pun di alur pengadaan. Satu-satunya konsekuensi perilaku dari
 * status terjadi di dalam layar vendor itu sendiri — {@code PenyediaAssetAction} menyembunyikan
 * tombol unggah berkas ketika status sudah {@link #VERIFIKASI}, sehingga berkas yang sudah
 * disahkan tidak dapat ditimpa begitu saja. Perlindungan itu bersifat tampilan dan dapat
 * dilepas kembali oleh pengguna berhak dengan mengubah status kembali ke {@link #BELUM} atau
 * {@link #REVISI} lewat combobox pada layar yang sama.</p>
 * <p>{@link #getTanggalBerlakuSampai()} pun demikian: tanggal kedaluwarsa disunting di layar
 * vendor dan dihitung oleh {@code DasboardVendor} (jumlah dokumen yang sudah kedaluwarsa dan yang
 * akan kedaluwarsa dalam 30 hari), namun tidak satu pun proses transaksi yang menolak vendor
 * karena dokumen legalnya sudah lewat masa berlaku. Kepatuhan dokumen di modul ini karena itu
 * bersifat <b>pemantauan</b>, bukan <b>penegakan</b>. Bila penegakan dibutuhkan, ia harus
 * ditambahkan di lapisan {@code Action} pengadaan atau pada alur SOP pendaftaran vendor;
 * mekanismenya sudah tersedia di basis kode ini pada modul SOP, yang memeriksa
 * {@code DokumenAlurSop.getWajib()} sebagai prasyarat langkah disposisi.</p>
 *
 * <h2>Berkas fisik disimpan terpisah</h2>
 * <p>Baris ini tidak memuat berkas. Unggahan dilekatkan lewat {@code LampiranLain}/
 * {@code FileFotoLain} yang dikunci pada {@link #getId()} baris ini beserta nama kelasnya
 * ({@code PenyediaAssetPunyaDokumen.class.getName()}). Dua akibat praktis: baris harus tersimpan
 * lebih dahulu sebelum berkas dapat dilekatkan padanya — layar vendor memang membangun objek
 * sementara yang belum ber-id untuk pasangan yang belum pernah diisi — dan menghapus baris ini
 * tidak otomatis menghapus lampirannya, sehingga berkas dapat menjadi yatim.</p>
 *
 * <h2>Struktur</h2>
 * <p>Kelas mewarisi {@link GeneralValueObject} (bukan {@code DataSop}: pemenuhan dokumen tidak
 * memiliki alur persetujuan sendiri, ia mengikuti alur vendor induknya), memakai kunci utama
 * {@code IDENTITY}, dianotasi {@link Audited} sehingga setiap perubahan status dan tanggal
 * berlakunya terekam pada tabel revisi Envers, serta memakai
 * {@code dynamicInsert}/{@code dynamicUpdate}. Bidang audit bayangan
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} wajib ada agar
 * {@code AuditTimestampInterceptor} dapat mengisinya lewat {@link #onUpdate()}.</p>
 *
 * @see PenyediaAsset
 * @see DokumenPenyediaAsset
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "penyedia_asset_punya_dokumen")
public class PenyediaAssetPunyaDokumen extends GeneralValueObject {

	/**
	 * Status awal: berkas sudah tercatat namun <b>belum diverifikasi</b> petugas.
	 *
	 * <p>Menjadi nilai balik bawaan {@link #getStatus()} ketika kolom status masih {@code null}
	 * atau kosong, sehingga baris lama hasil migrasi maupun baris sementara yang dibuat layar
	 * vendor selalu dibaca sebagai status ini.</p>
	 * <p><b>Konstanta ini adalah teks, bukan enum.</b> Nilainya dibandingkan dengan
	 * {@code equals}/{@code equalsIgnoreCase} terhadap isi kolom, sehingga mengubah teksnya akan
	 * memutus pengenalan seluruh baris lama yang sudah tersimpan dengan teks lama. Jangan
	 * mengubahnya tanpa migrasi data.</p>
	 */
	public static final String BELUM = "Belum Diverifikasi";
	/**
	 * Status penolakan: berkas sudah diperiksa namun <b>harus direvisi</b> vendor.
	 *
	 * <p>Dipakai {@code DasboardAnalisisVendor} sebagai salah satu ember penghitungan kepatuhan
	 * dokumen. Sama seperti {@link #BELUM}, status ini tidak menghalangi transaksi pengadaan mana
	 * pun — vendor dengan seluruh dokumennya berstatus "Harus Direvisi" tetap dapat dipakai.</p>
	 * <p>Peringatan mengenai perubahan teks pada {@link #BELUM} berlaku sama di sini.</p>
	 */
	public static final String REVISI = "Harus Direvisi";
	/**
	 * Status akhir: berkas sudah <b>terverifikasi</b> petugas.
	 *
	 * <p>Satu-satunya status yang memiliki konsekuensi perilaku, dan itu pun terbatas pada
	 * tampilan: {@code PenyediaAssetAction} menyembunyikan tombol unggah berkas ketika status
	 * bernilai ini ({@code !getStatus().equalsIgnoreCase(VERIFIKASI)}), sehingga berkas yang sudah
	 * disahkan tidak dapat ditimpa tanpa terlebih dahulu menurunkan statusnya kembali. Status ini
	 * <b>tidak</b> menjadi syarat bagi transaksi pengadaan.</p>
	 * <p>Peringatan mengenai perubahan teks pada {@link #BELUM} berlaku sama di sini.</p>
	 */
	public static final String VERIFIKASI = "Terverifikasi";
	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai warisan cetakan hbm2java yang dipakai bersama hampir seluruh entitas paket ini.
	 * Kesamaan nilai antar kelas tidak berbahaya karena verifikasi dilakukan per kelas, namun
	 * jangan mengubahnya tanpa alasan sebab objek entitas dapat ikut diserialisasi bersama
	 * sesi/desktop ZK.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Untuk baris pemenuhan dokumen, nilai ini praktis menjadi jejak "siapa yang terakhir
	 * mengubah status verifikasi" — pelengkap riwayat Envers yang lebih lengkap.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan. Bidang audit bayangan hanya boleh diisi
	 * {@code AuditTimestampInterceptor}, dan penulisan kosong dari jalur salin/klon objek tidak
	 * boleh menghapus jejak audit yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan agar jejak
	 * audit tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui.
	 *
	 * <p>Interceptor-lah yang mengisi {@link #oleh}, {@link #olehId}, dan
	 * {@link #getTanggal_dirubah()} dari konteks pengguna aktif. Method sengaja {@code protected}
	 * dan tidak boleh dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui interceptor audit. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor}, bukan oleh form.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Perhatikan bahwa ini adalah waktu perubahan <i>baris pemenuhan</i>, bukan tanggal
	 * penerbitan berkas maupun masa berlakunya — untuk yang terakhir gunakan
	 * {@link #getTanggalBerlakuSampai()}.</p>
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<keterangan>"}.
	 *
	 * <p>Berbeda dari kebanyakan entitas paket ini yang memakai {@code nama}, kelas ini memakai
	 * {@link #keterangan} karena tidak memiliki bidang nama sendiri — identitas bisnisnya melekat
	 * pada pasangan vendor dan jenis dokumen yang ditunjuknya. Akibatnya representasi ini kurang
	 * informatif untuk penelusuran log: dua baris milik vendor berbeda dapat menghasilkan teks yang
	 * sama bila keterangannya sama, dan bagian keterangan akan bernilai {@code "null"} untuk baris
	 * yang keterangannya belum diisi. Method membaca bidang secara langsung sehingga tidak
	 * memicu resolusi relasi malas — perilaku yang memang diinginkan agar {@code toString()} aman
	 * dipanggil di luar sesi Hibernate.</p>
	 *
	 * @return gabungan id dan keterangan; bagian id bernilai {@code "null"} bila baris belum
	 *         tersimpan
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	/** Status verifikasi berkas sebagai teks bebas; lihat {@link #getStatus()}. */
	private String status;
	/** Catatan bebas petugas atas berkas ini; nilainya yang dicetak ke parameter laporan pengadaan. */
	private String keterangan;
	/** Vendor pemilik berkas; kunci asing pertama pembentuk relasi many-to-many. */
	private PenyediaAsset penyediaAsset;
	/** Jenis dokumen yang dipenuhi; kunci asing kedua pembentuk relasi many-to-many. */
	private DokumenPenyediaAsset dokumenPenyediaAsset;
	/** Tanggal akhir masa berlaku dokumen (mis. SIUP/NPWP/sertifikat); null = tanpa masa berlaku. */
	private Date tanggalBerlakuSampai;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Dipakai pula oleh {@code PenyediaAssetAction} dan layar-layar pengadaan untuk membuat
	 * objek sementara (belum ber-id, belum tersimpan) mewakili pasangan vendor-dokumen yang belum
	 * pernah diisi, sehingga tabel verifikasi di layar tetap menampilkan seluruh jenis dokumen di
	 * katalog termasuk yang belum dipenuhi.</p>
	 */
	public PenyediaAssetPunyaDokumen() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilai ini juga menjadi kunci pelekatan berkas: {@code LampiranLain}/{@code FileFotoLain}
	 * mencari lampiran berdasarkan id ini beserta nama kelas entitas. Baris yang belum tersimpan
	 * ({@code null}) karena itu belum dapat memiliki lampiran.</p>
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Hanya untuk kebutuhan Hibernate dan penyalinan objek; jangan menyetel id secara manual
	 * pada baris yang akan disimpan sebagai data baru, terlebih karena id ini dipakai sebagai
	 * kunci pelekatan berkas lampiran.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas petugas atas berkas ini.
	 *
	 * <p>Kolomnya bertipe {@code text} sehingga panjangnya tidak dibatasi. Nilai ini bukan sekadar
	 * catatan internal: seluruh layar pengadaan mencetaknya ke parameter laporan dengan kunci
	 * {@code "dokumen." + nama jenis dokumen}, sehingga isinya dapat muncul pada berkas cetak PO,
	 * berita acara penerimaan, perjanjian kerja sama, dan bukti pembayaran. Jangan menuliskan
	 * catatan internal yang tidak layak dibaca pihak luar di sini.</p>
	 * <p>Berbeda dari beberapa getter lain di paket ini, method ini <b>tidak</b> menormalkan
	 * {@code null} menjadi string kosong; pemanggil layar pengadaan menanganinya sendiri dengan
	 * {@code temp == null ? "" : temp.getKeterangan()}, yang berjaga terhadap barisnya yang
	 * {@code null} namun tidak terhadap keterangannya yang {@code null}.</p>
	 *
	 * @return catatan bebas, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas petugas atas berkas ini.
	 *
	 * @param keterangan teks catatan; boleh {@code null}. Ingat bahwa nilainya dapat ikut tercetak
	 *                   pada laporan pengadaan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status verifikasi berkas, dengan {@code null} dan string kosong dinormalkan
	 * menjadi {@link #BELUM}.
	 *
	 * <p><b>Fail-safe ke arah aman.</b> Normalisasi ini berarti baris lama hasil migrasi, dan
	 * objek sementara yang dibuat layar vendor bagi pasangan vendor-dokumen yang belum pernah
	 * diisi, selalu terbaca sebagai "Belum Diverifikasi" — bukan sebagai terverifikasi. Pilihan
	 * bawaan ini benar arahnya: ketiadaan data tidak boleh dibaca sebagai persetujuan.</p>
	 *
	 * <p><b>Status disimpan sebagai teks bebas, bukan enum atau kunci asing.</b> Kolomnya
	 * bertipe string dan tidak ada batasan {@code CHECK} maupun tabel referensi yang membatasi
	 * nilainya pada ketiga konstanta {@link #BELUM}, {@link #REVISI}, dan {@link #VERIFIKASI}.
	 * Dalam pemakaian normal hal ini tidak menjadi masalah karena layar vendor hanya menawarkan
	 * ketiganya lewat combobox, namun perlu diwaspadai pada jalur impor data, penyuntingan
	 * langsung di basis data, atau kode baru yang menuliskan literal sendiri: salah ketik satu
	 * huruf akan menghasilkan status yang tidak dikenali siapa pun. Pembaca menanganinya dengan
	 * cara yang berbeda-beda — {@code PenyediaAssetAction} membandingkan dengan
	 * {@code equalsIgnoreCase(VERIFIKASI)} sehingga toleran terhadap perbedaan huruf besar-kecil,
	 * sedangkan {@code DasboardAnalisisVendor} memakai {@code equals} yang peka huruf dan
	 * memasukkan setiap nilai tak dikenal ke ember "belum diverifikasi". Akibatnya sebuah status
	 * yang salah ketik akan tampak sebagai "belum" pada dasbor sekaligus tetap menyembunyikan
	 * tombol unggah bila kebetulan hanya berbeda huruf besar-kecil.</p>
	 *
	 * <p><b>Konsekuensi perilaku.</b> Hanya ada satu, dan itu pun terbatas pada tampilan:
	 * {@code PenyediaAssetAction} menyembunyikan tombol unggah berkas ketika status sudah
	 * {@link #VERIFIKASI}. Status ini <b>tidak</b> menjadi syarat bagi transaksi pengadaan mana
	 * pun — lihat uraian pada Javadoc kelas. Perlindungan tersebut juga bukan kunci sekali-jalan:
	 * pengguna berhak dapat menurunkan status kembali ke {@link #BELUM} atau {@link #REVISI} lewat
	 * combobox pada layar yang sama, yang akan memunculkan kembali tombol unggah. Riwayat
	 * perubahan status terekam Envers karena entitas ini dianotasi {@code @Audited}, sehingga
	 * penurunan status seperti itu dapat ditelusuri setelah kejadian, walaupun tidak dicegah.</p>
	 *
	 * @return salah satu dari {@link #BELUM}, {@link #REVISI}, {@link #VERIFIKASI}, atau — bila
	 *         data ditulis dari luar layar vendor — teks lain apa adanya; tidak pernah
	 *         {@code null} maupun string kosong
	 * @see #BELUM
	 * @see #REVISI
	 * @see #VERIFIKASI
	 */
	public String getStatus() {
		return status == null || status.isEmpty() ? BELUM : status;
	}

	/**
	 * Menyetel status verifikasi berkas.
	 *
	 * <p>Sebaiknya selalu memakai salah satu konstanta {@link #BELUM}, {@link #REVISI}, atau
	 * {@link #VERIFIKASI}, bukan literal teks, agar tidak menghasilkan nilai yang tidak dikenali
	 * pembacanya. Menyetel {@code null} atau string kosong membuat {@link #getStatus()} membaca
	 * kembali {@link #BELUM}.</p>
	 *
	 * @param status status verifikasi; sebaiknya salah satu konstanta kelas ini
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan tanggal akhir masa berlaku berkas ini.
	 *
	 * <p>Dipakai untuk dokumen yang memang punya masa berlaku — SIUP, sertifikat badan usaha,
	 * surat keterangan domisili, dan sejenisnya. Nilai {@code null} berarti dokumen dianggap
	 * <b>tanpa masa berlaku</b>, bukan "kedaluwarsa" maupun "belum diketahui"; kedua makna
	 * terakhir tidak dapat dibedakan pada model ini.</p>
	 * <p><b>Pemantauan, bukan penegakan.</b> Nilai ini disunting pada layar vendor
	 * ({@code PenyediaAssetAction} dan {@code PenyediaAssetPunyaDokumenAction}) dan dibaca
	 * {@code DasboardVendor} untuk dua penghitung: jumlah dokumen yang sudah kedaluwarsa
	 * ({@code tanggalBerlakuSampai < :now}) dan jumlah yang akan kedaluwarsa dalam 30 hari ke
	 * depan. Di luar itu tidak ada pemakaian lain — tidak ada proses pengadaan yang menolak
	 * vendor karena dokumen legalnya sudah lewat masa berlaku, dan tidak ada mekanisme pengingat
	 * otomatis yang mengirim pemberitahuan. Perhatikan pula bahwa kedua penghitung dasbor
	 * menghitung <i>baris dokumen</i>, bukan <i>vendor</i>, sehingga satu vendor dengan lima
	 * dokumen kedaluwarsa menyumbang lima pada angka tersebut.</p>
	 * <p>Kolomnya bertipe {@code DATE} (tanpa komponen jam), sehingga perbandingan terhadap
	 * stempel waktu {@code now} pada dasbor memperlakukan hari kedaluwarsa itu sendiri sebagai
	 * sudah lewat begitu jam berjalan melewati tengah malam.</p>
	 *
	 * @return tanggal akhir masa berlaku, atau {@code null} bila dokumen tidak memiliki masa
	 *         berlaku
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_berlaku_sampai", nullable = true)
	public Date getTanggalBerlakuSampai() {
		return tanggalBerlakuSampai;
	}

	/**
	 * Menyetel tanggal akhir masa berlaku berkas ini.
	 *
	 * <p>Tidak ada validasi bahwa tanggalnya berada di masa depan, maupun bahwa ia lebih lambat
	 * dari tanggal penerbitan — menyimpan tanggal yang sudah lewat diperbolehkan dan akan langsung
	 * terhitung sebagai dokumen kedaluwarsa pada {@code DasboardVendor}.</p>
	 *
	 * @param tanggalBerlakuSampai tanggal akhir masa berlaku; {@code null} berarti tanpa masa
	 *                             berlaku
	 */
	public void setTanggalBerlakuSampai(Date tanggalBerlakuSampai) {
		this.tanggalBerlakuSampai = tanggalBerlakuSampai;
	}

	/**
	 * Mengembalikan vendor pemilik berkas ini, setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * <p>Pemanggilan {@code check(...)} milik {@link GeneralValueObject} menyelesaikan proksi
	 * Hibernate yang mungkin terikat pada sesi yang sudah tertutup, dengan menempuh cache
	 * {@code ConstantValues} dan sesi aktif secara berurutan. Bila keempat sumbernya gagal, ia
	 * mengembalikan argumen apa adanya sehingga getter ini tetap dapat mengembalikan proksi yang
	 * tidak dapat diinisialisasi — pemanggil di luar sesi karena itu sebaiknya membungkus
	 * pemakaiannya, sebagaimana dilakukan {@code DasboardAnalisisVendor} yang membungkus
	 * {@code doc.getPenyediaAsset()} di dalam {@code try/catch}.</p>
	 * <p>Perhatikan bahwa method ini <b>menulis kembali</b> hasil resolusi ke bidang instansnya
	 * ({@code penyediaAsset = check(penyediaAsset)}) — pola getter yang memutasi state yang
	 * berulang di seluruh basis kode ini. Efek sampingnya diperlukan agar resolusi tidak diulang
	 * pada setiap pembacaan, namun berarti getter ini tidak bebas efek samping dan tidak aman
	 * dipanggil bersamaan dari beberapa thread atas objek yang sama.</p>
	 *
	 * @return vendor pemilik berkas; secara skema tidak boleh {@code null}
	 *         ({@code nullable = false}), namun dapat {@code null} pada objek sementara yang
	 *         belum tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penyedia_asset", nullable = false)
	public PenyediaAsset getPenyediaAsset() {
		penyediaAsset = check(penyediaAsset);
		return penyediaAsset;
	}

	/**
	 * Menyetel vendor pemilik berkas ini.
	 *
	 * <p>Kolom kunci asingnya {@code nullable = false}, sehingga baris tanpa vendor akan ditolak
	 * basis data saat disimpan. Bersama {@link #setDokumenPenyediaAsset(DokumenPenyediaAsset)},
	 * pasangan ini membentuk identitas bisnis baris — namun tidak ada batasan unik yang
	 * menegakkannya, sehingga menyimpan dua baris dengan pasangan yang sama tidak akan
	 * dicegah.</p>
	 * <p>Kaskade {@code PERSIST}/{@code MERGE} pada relasi ini berarti menyimpan baris pemenuhan
	 * dapat ikut menyimpan perubahan pada objek vendor yang menempel padanya; berhati-hatilah
	 * memasang objek vendor yang sudah disunting di tempat lain.</p>
	 *
	 * @param penyediaAsset vendor pemilik berkas
	 */
	public void setPenyediaAsset(PenyediaAsset penyediaAsset) {
		this.penyediaAsset = penyediaAsset;
	}

	/**
	 * Mengembalikan jenis dokumen yang dipenuhi baris ini, setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * <p>Sama seperti {@link #getPenyediaAsset()}, method ini menulis kembali hasil resolusi ke
	 * bidang instansnya sehingga memiliki efek samping. Nilai balikannya dipakai layar vendor untuk
	 * menampilkan nama dokumen, sifat wajib/opsional, dan sebagai kunci parameter laporan pada
	 * layar-layar pengadaan.</p>
	 *
	 * @return jenis dokumen yang dipenuhi; secara skema tidak boleh {@code null}
	 *         ({@code nullable = false}), namun dapat {@code null} pada objek sementara yang belum
	 *         tersimpan
	 * @see DokumenPenyediaAsset#getWajib()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokumen_penyedia_asset", nullable = false)
	public DokumenPenyediaAsset getDokumenPenyediaAsset() {
		dokumenPenyediaAsset = check(dokumenPenyediaAsset);
		return dokumenPenyediaAsset;
	}

	/**
	 * Menyetel jenis dokumen yang dipenuhi baris ini.
	 *
	 * <p>Kolom kunci asingnya {@code nullable = false}. Perhatikan bahwa menghapus sebuah jenis
	 * dokumen dari katalog {@link DokumenPenyediaAsset} tanpa membersihkan baris pemenuhan yang
	 * menunjuknya akan meninggalkan rujukan menggantung; menonaktifkannya
	 * ({@code setAktif(false)}) tidak menghapus atau membatalkan baris pemenuhan yang sudah
	 * ada.</p>
	 *
	 * @param dokumenPenyediaAsset jenis dokumen yang dipenuhi
	 */
	public void setDokumenPenyediaAsset(DokumenPenyediaAsset dokumenPenyediaAsset) {
		this.dokumenPenyediaAsset = dokumenPenyediaAsset;
	}

}
