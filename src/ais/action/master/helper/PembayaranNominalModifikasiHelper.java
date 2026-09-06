package ais.action.master.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;

/**
 * Helper terpusat untuk menghitung <b>nominal tagihan mahasiswa</b> ketika item biaya yang
 * ditagih bukan harga tetap, melainkan harga satuan yang harus <b>dikalikan sesuatu</b>
 * (jumlah SKS yang diambil, jumlah matakuliah ber-UTS, jumlah matakuliah remedial, ada/tidaknya
 * konversi, tunggakan semester lalu, dan seterusnya).
 *
 * <h2>Apa yang sebenarnya dimaksud &quot;modifikasi nominal&quot;</h2>
 *
 * <p>Nama kelas ini mudah disalahpahami. Kelas ini <b>bukan</b> pintu bagi operator untuk
 * mengetikkan nominal tagihan seorang mahasiswa sesuka hati; kelas ini tidak menerima satu pun
 * nominal dari parameter pemanggil dan tidak punya jalur "isi angka bebas". Yang dilakukannya
 * adalah <b>menurunkan</b> nominal secara deterministik dari tiga sumber yang seluruhnya berupa
 * data master atau data akademik:</p>
 *
 * <ol>
 *   <li><b>Harga dasar.</b> {@link DetailBiaya#getNilaiBiaya()} pada jalur tagihan biasa, atau
 *   {@link PengaturanPembayaranBulanan#getNominal()} pada jalur tagihan bulanan/cicilan.</li>
 *   <li><b>Skema perkalian.</b> {@link ItemBiaya#getPenghitungan()} &mdash; sebuah string yang
 *   dipilih operator dari combobox {@code ItemBiaya.PENGHITUNGAN_MAP}. String inilah yang
 *   memilih cabang rumus di kedua method besar kelas ini.</li>
 *   <li><b>Fakta akademik mahasiswa.</b> Isi KRS-nya ({@link Detailperkuliahan} /
 *   {@link Perkuliahan} / {@link Matakuliah}), status lulus, matakuliah konversi, dan tunggakan
 *   semester sebelumnya &mdash; semuanya dibaca lewat {@link Mahasiswa} dan
 *   {@link KrsDetailHelper}.</li>
 * </ol>
 *
 * <p>Konsekuensi penting untuk penilaian risiko: <b>permukaan otorisasi yang sesungguhnya bukan
 * di kelas ini</b>, melainkan (a) di layar master {@code ItemBiaya} (siapa boleh mengubah
 * {@code penghitungan} dan harga satuan), (b) di layar {@code DetailSettingBiaya} /
 * {@code PengaturanPembayaranBulanan} (siapa boleh mengubah harga per prodi/per bulan), dan
 * (c) di layar KRS (siapa boleh mengubah matakuliah yang diambil mahasiswa, karena mengubah KRS
 * berarti mengubah tagihan). Kelas ini sendiri <b>tidak memeriksa hak akses sama sekali</b> dan
 * memang tidak seharusnya &mdash; ia dipanggil dari dalam render tabel tagihan dan dari getter
 * entity, tempat pemeriksaan hak akses sudah (seharusnya) dilakukan lebih dulu. Lihat bagian
 * &quot;Batasan&quot; di bawah untuk hal yang justru <i>tidak</i> aman.</p>
 *
 * <h2>Dua jalur, dua method</h2>
 *
 * <table border="1">
 *   <caption>Perbandingan kedua jalur</caption>
 *   <tr><th></th><th>{@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}</th>
 *       <th>{@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)}</th></tr>
 *   <tr><td>Untuk</td><td>tagihan biasa (satu baris rincian biaya per semester)</td>
 *       <td>tagihan bulanan/cicilan (satu baris per bulan)</td></tr>
 *   <tr><td>Harga dasar</td><td>{@code DetailBiaya.nilaiBiaya}</td>
 *       <td>{@code PengaturanPembayaranBulanan.nominal}</td></tr>
 *   <tr><td>Hasil ditulis ke</td><td>{@code DetailBiaya.nilaiBiayaBaru} (efek samping)</td>
 *       <td>nilai kembalian {@code Double}</td></tr>
 *   <tr><td>Rincian perhitungan ditulis ke</td><td>{@code DetailBiaya.keterangan} ({@code @Transient})</td>
 *       <td>{@code PengaturanPembayaranBulanan.keterangan} (<b>kolom terpetakan</b>)</td></tr>
 *   <tr><td>Penyaring tahapan</td><td>tidak ada (selalu {@code null})</td>
 *       <td>{@link PengaturanPembayaranBulanan#hitungTahap(Mahasiswa, Integer)}</td></tr>
 *   <tr><td>Gerbang tambahan</td><td>tidak ada</td>
 *       <td>{@link PengaturanPembayaranBulanan#getDikalikanDenganKondisiKhusus()} harus menyala</td></tr>
 * </table>
 *
 * <h2>Aturan utama (invarian yang dituju)</h2>
 * <ul>
 *   <li>{@link ItemBiaya#TIDAK_ADA_PENGHITUNGAN} &mdash; atau {@code penghitungan}
 *   {@code null}/kosong &mdash; selalu memakai nominal asli tanpa perkalian apa pun.</li>
 *   <li>{@link DetailBiaya} non-bulanan memakai {@code nilaiBiaya} sebagai harga dasar.</li>
 *   <li>{@link PengaturanPembayaranBulanan} memakai nominal bulanan sebagai harga dasar.</li>
 *   <li><b>Hasil {@code 0} adalah nilai sah, bukan nilai kosong.</b> Mahasiswa yang tidak
 *   mengambil satu SKS pun memang harus ditagih {@code 0} untuk item berbasis SKS; hasil itu
 *   tidak boleh dipaksa kembali ke nominal awal. Aturan ini yang membedakan kelas ini dari
 *   implementasi lama yang menganggap {@code 0} sebagai "belum dihitung".</li>
 * </ul>
 *
 * <h2>Rantai pemanggil</h2>
 * <ul>
 *   <li>{@link DetailBiaya#updateKeterangan(Mahasiswa, Integer)} &rarr;
 *   {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}. Dipicu antara lain oleh
 *   {@code Kegiatan.ambilJumlahTagihan(...)} saat mendapati {@code nilaiBiayaBaru} masih
 *   {@code null}.</li>
 *   <li>{@link PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)} &rarr;
 *   {@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)}.</li>
 *   <li>{@code PembayaranUtilHelper} dan {@code DetailPembayaranMahasiswaRenderer} memanggil
 *   bentuk statisnya langsung saat menyusun tabel tagihan di layar.</li>
 *   <li>{@code KegiatanPersistenceHelper} memakai {@link #isTanpaPenghitungan(ItemBiaya)} untuk
 *   memutuskan apakah sebuah item perlu dihitung ulang sama sekali.</li>
 * </ul>
 *
 * <h2>Batasan dan jebakan yang perlu diketahui</h2>
 * <ul>
 *   <li><b>Rantai {@code if/else-if} tanpa {@code else} penutup.</b> Kedua method besar memilih
 *   cabang dengan membandingkan {@code penghitungan} satu per satu. Bila tidak ada satu pun
 *   cabang yang cocok, <b>tidak ada nilai yang ditulis</b>: {@code updateKeterangan} meninggalkan
 *   {@code nilaiBiayaBaru} apa adanya (mungkin {@code null}, mungkin sisa perhitungan
 *   sebelumnya), dan {@code ambilNominalModifikasi} mengembalikan nominal dasar tanpa perkalian.
 *   Ini berarti menambah konstanta baru ke {@code ItemBiaya.PENGHITUNGAN_MAP} tanpa menambah
 *   cabang di sini menghasilkan <b>kegagalan diam</b>, bukan galat.</li>
 *   <li><b>[DIPERBAIKI] {@link ItemBiaya#DIKALI_JUMLAH_SKS_UAS_REMDIAL} sempat kehilangan
 *   cabangnya di {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}.</b> Blok yang
 *   seharusnya menanganinya dulu menguji {@link ItemBiaya#DIKALI_JUMLAH_SKS_UTS_REMEDIAL} untuk
 *   kedua kalinya (dua blok kembar persis; yang kedua tak pernah terjangkau), sehingga skema UAS
 *   remedial ini gagal diam di jalur tagihan biasa walau sudah benar di
 *   {@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)}. Sudah
 *   ditambal dengan mengganti konstanta dan saringan blok kedua. Lihat catatan rinci pada method
 *   tersebut.</li>
 *   <li><b>{@code ambilNominalModifikasi} menulis ke kolom terpetakan.</b> Meski namanya
 *   diawali {@code ambil} (getter-like), method itu memanggil
 *   {@link PengaturanPembayaranBulanan#setKeterangan(String)} pada entity yang umumnya masih
 *   dikelola sesi Hibernate. Pada instance terkelola, dirty-checking akan menerbitkan
 *   {@code UPDATE} beserta revisi Envers walau tidak ada yang "diubah" secara semantik. Ini
 *   perwujudan pola getter-mutasi-field yang sudah terdokumentasi luas di
 *   {@code ais/database/model/}, dan alasan {@code KegiatanHelper} menandai entity ini read-only
 *   saat hitung ulang massal.</li>
 *   <li><b>Tidak ada jejak audit perubahan nominal.</b> Kelas ini tidak menulis
 *   {@code posting_history}, tidak memanggil {@code Common.catatLog}, dan tidak menyimpan nominal
 *   sebelum/sesudah. Jejak yang ada hanyalah (a) revisi Envers pada tabel master yang diubah
 *   operator ({@code item_biaya}, {@code detail_biaya}, {@code pengaturan_pembayaran_bulanan}),
 *   dan (b) string {@code keterangan} yang merekam rumusnya dalam bentuk teks bebas
 *   (mis. {@code "SPP (500.000) x 20 SKS, sbb : Kalkulus:3sks, ..."}). Karena itu, untuk
 *   merekonstruksi "mengapa tagihan mahasiswa X berubah" seseorang harus menggabungkan riwayat
 *   Envers master biaya dengan riwayat KRS &mdash; kelas ini sendiri tidak mencatat apa pun.</li>
 *   <li><b>Biaya query berat, tanpa cache.</b> Setiap cabang memuat ulang seluruh
 *   {@link Detailperkuliahan} mahasiswa satu per satu lewat
 *   {@link GeneralValueObject#ambilData(Class, String)} (N+1 query). Untuk satu baris tagihan
 *   ini sudah mahal; dipanggil dari renderer tabel untuk ratusan mahasiswa, biayanya berlipat.
 *   Pemanggil massal wajib menyiapkan sesi/cache-nya sendiri.</li>
 *   <li><b>Aritmetika {@code double} untuk uang.</b> Seluruh perkalian memakai {@code double}
 *   tanpa pembulatan eksplisit, sehingga hasil seperti {@code 1.0000000000000002} mungkin muncul
 *   dan baru dibulatkan (atau tidak) di lapisan tampilan/posting.</li>
 * </ul>
 *
 * @see ItemBiaya#getPenghitungan()
 * @see DetailBiaya#updateKeterangan(Mahasiswa, Integer)
 * @see PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)
 */
public final class PembayaranNominalModifikasiHelper {

	/**
	 * Konstruktor privat: kelas ini murni kumpulan method statis dan tidak boleh diinstansiasi.
	 *
	 * <p>Dipasangkan dengan modifier {@code final} pada kelasnya, ini menutup dua jalur
	 * penyalahgunaan sekaligus: membuat objeknya dan mewarisinya untuk menimpa rumus biaya.</p>
	 */
	private PembayaranNominalModifikasiHelper() {
	}

	/**
	 * Menormalkan {@code Double} yang mungkin {@code null} menjadi {@code 0.0}.
	 *
	 * <p>Dipakai di jalur "pulang cepat" {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}
	 * agar {@code nilaiBiayaBaru} selalu terisi angka ketika perhitungan memang tidak berlaku,
	 * sehingga pemanggil tidak perlu membedakan "belum dihitung" dari "tidak perlu dihitung".</p>
	 *
	 * <p><b>Perhatikan asimetrinya:</b> normalisasi ini hanya dipakai pada jalur pulang cepat.
	 * Di dalam rantai perhitungan, harga dasar diambil langsung lewat
	 * {@code detailBiaya.getNilaiBiaya()} tanpa {@code safeDouble}, sehingga item biaya berskema
	 * perkalian yang harganya {@code null} akan melempar {@code NullPointerException} saat
	 * di-unbox pada perkalian &mdash; bukan menghasilkan {@code 0}.</p>
	 *
	 * @param value nilai yang mungkin {@code null}
	 * @return {@code value} apa adanya, atau {@code 0.0} bila {@code value} {@code null};
	 *         tidak pernah {@code null}
	 */
	private static Double safeDouble(Double value) {
		return value == null ? Double.valueOf(0.0) : value;
	}

	/**
	 * Menentukan apakah sebuah {@link ItemBiaya} <b>tidak</b> memakai skema perkalian apa pun,
	 * sehingga nominalnya dipakai apa adanya.
	 *
	 * <p>Tiga keadaan diperlakukan sama sebagai "tanpa penghitungan":</p>
	 * <ul>
	 *   <li>{@code itemBiaya} sendiri {@code null} &mdash; rincian biaya yatim, tidak ada skema
	 *   yang bisa dibaca;</li>
	 *   <li>{@code penghitungan} {@code null} atau kosong setelah {@code trim()} &mdash; data
	 *   lama dari sebelum kolom ini ada, atau operator belum memilih apa pun;</li>
	 *   <li>{@code penghitungan} persis {@link ItemBiaya#TIDAK_ADA_PENGHITUNGAN} &mdash; operator
	 *   memilih "Tidak ada penghitungan" secara sadar.</li>
	 * </ul>
	 *
	 * <p><b>Sikap fail-safe:</b> saat ragu, method ini mengembalikan {@code true}, yaitu
	 * "pakai nominal asli". Itu pilihan yang benar untuk keuangan &mdash; data yang tidak
	 * lengkap tidak boleh menghasilkan tagihan yang dikalikan angka acak. Bandingkan dengan
	 * kebalikannya (default "hitung"), yang akan mengubah harga tetap menjadi
	 * {@code 0} setiap kali skema tidak terbaca.</p>
	 *
	 * <p><b>Perbandingan memakai konstanta, bukan kunci combobox.</b> Yang dibandingkan adalah
	 * <i>label</i> ({@code "Tidak ada penghitungan"}), bukan kunci numeriknya di
	 * {@code ItemBiaya.PENGHITUNGAN_MAP}. Artinya nilai kolom {@code penghitungan} di basis data
	 * memang menyimpan labelnya; mengganti teks label di kemudian hari akan memutus pencocokan
	 * ini untuk seluruh baris lama.</p>
	 *
	 * <p><b>Dipanggil dari:</b> kedua method besar kelas ini sebagai gerbang paling awal, dan
	 * dari {@code KegiatanPersistenceHelper} untuk memutuskan apakah sebuah item biaya perlu
	 * dihitung ulang sama sekali sebelum menyusun tagihan.</p>
	 *
	 * @param itemBiaya item biaya yang diperiksa; boleh {@code null}
	 * @return {@code true} bila nominal harus dipakai apa adanya tanpa perkalian
	 */
	public static boolean isTanpaPenghitungan(ItemBiaya itemBiaya) {
		if (itemBiaya == null) {
			return true;
		}
		String penghitungan = itemBiaya.getPenghitungan();
		return penghitungan == null || penghitungan.trim().length() == 0
				|| ItemBiaya.TIDAK_ADA_PENGHITUNGAN.equals(penghitungan);
	}

	/**
	 * Menghitung ulang <b>nominal dan teks rincian</b> satu baris {@link DetailBiaya} untuk
	 * seorang mahasiswa pada semester tertentu &mdash; jalur tagihan biasa (non-bulanan).
	 *
	 * <p>Meski namanya menyebut &quot;keterangan&quot;, <b>inilah pintu masuk perhitungan
	 * nominal</b> untuk seluruh item biaya berskema perkalian. Hasil perkalian ditulis ke
	 * {@link DetailBiaya#setNilaiBiayaBaru(Double)}; teks penjelasnya (mis.
	 * {@code "Biaya SKS (50.000) x 20 SKS, sbb : Kalkulus:3sks, ..."}) ditulis ke
	 * {@link DetailBiaya#setKeterangan(String)}. Keduanya {@code @Transient}, jadi method ini
	 * <b>tidak</b> menyentuh tabel {@code detail_biaya}; efeknya hanya di memori dan hilang saat
	 * objeknya dibuang.</p>
	 *
	 * <h4>Empat gerbang pulang cepat</h4>
	 * <p>Sebelum masuk rantai rumus, method ini pulang lebih awal pada empat keadaan. Tiga di
	 * antaranya mengisi {@code nilaiBiayaBaru} dengan harga dasar yang sudah dinormalkan
	 * {@link #safeDouble(Double)} &mdash; sehingga pemanggil selalu mendapat angka, bukan
	 * {@code null}:</p>
	 * <ol>
	 *   <li>{@code detailBiaya} {@code null} &rarr; tidak ada apa pun yang bisa ditulis, langsung
	 *   {@code return} <b>tanpa</b> mengisi apa-apa;</li>
	 *   <li>{@code itemBiaya} {@code null} (rincian biaya yatim) &rarr; harga dasar apa adanya;</li>
	 *   <li>{@link #isTanpaPenghitungan(ItemBiaya)} bernilai {@code true} &rarr; harga dasar apa
	 *   adanya &mdash; ini jalur mayoritas item biaya berharga tetap;</li>
	 *   <li>{@code mahasiswa}/{@code mahasiswa.getId()}/{@code semester} {@code null} &rarr; harga
	 *   dasar apa adanya, karena tanpa konteks mahasiswa tidak ada yang bisa dikalikan.</li>
	 * </ol>
	 *
	 * <h4>Rantai rumus</h4>
	 * <p>Sesudah gerbang itu, method memilih <b>satu</b> cabang dengan membandingkan
	 * {@link ItemBiaya#getPenghitungan()} secara berurutan. Cabang pertama justru bukan
	 * berdasarkan {@code penghitungan}, melainkan berdasarkan
	 * {@code itemBiaya.getTerhubungKeNilaiTambahan()}; sisanya per skema. Ringkasannya:</p>
	 * <ul>
	 *   <li><b>Nilai dari parameter tambahan biodata</b> (cabang pertama, dipilih bila
	 *   {@code terhubungKeNilaiTambahan} menyala dan {@code parameterTambahan} terisi). Nominal
	 *   <b>tidak dikalikan apa pun</b>: helper membaca
	 *   {@code BiodataMahasiswa.parameterTambahanInds} milik mahasiswa (baris terbaru menurut
	 *   {@code id}), mem-parsing formatnya ({@code "label->idParameter<=>nilai"} per baris),
	 *   dan bila {@code idParameter} cocok dengan {@code itemBiaya.parameterTambahan.id} serta
	 *   nilainya angka, nilai itu <b>langsung menjadi nominal tagihan</b>. Lihat catatan
	 *   integritas di bawah.</li>
	 *   <li><b>Berbasis SKS KRS:</b> {@link ItemBiaya#DIKALI_JUMLAH_SKS_MAHASISWA} (seluruh SKS
	 *   yang diambil), {@link ItemBiaya#DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG} (hanya
	 *   {@code Perkuliahan.semester < semester} tagihan), dan
	 *   {@link ItemBiaya#DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG} (hanya yang semesternya
	 *   sama).</li>
	 *   <li><b>Berbasis SKS komponen matakuliah:</b> {@code ..._MK_PRAKTEK},
	 *   {@code ..._MK_PRAKTEK_SP}, {@code ..._MK_SKS_DISKUSI_TEORI},
	 *   {@code ..._MK_SKS_DISKUSI_TEORI_SP}, {@code ..._MK_SKS_SIMULASI}. Semuanya menjumlahkan
	 *   dari KRS reguler <i>lalu ditambah</i> matakuliah konversi.</li>
	 *   <li><b>Berbasis komponen ujian:</b> {@code DIKALI_JUMLAH_MK_UTS}/{@code _UAS} (jumlah
	 *   matakuliah), {@code ..._SP} (varian semester pendek), dan padanan berbasis SKS
	 *   {@code DIKALI_JUMLAH_SKS_UTS}/{@code _UAS}/{@code _UTS_SP}/{@code _UAS_SP}/
	 *   {@code _UTS_REMEDIAL}.</li>
	 *   <li><b>Berbasis remedial:</b> {@code DIKALI_JUMLAH_MATAKULIAH_REMEDIAL} dan empat
	 *   varian bobot SKS-nya ({@code _1_SKS} sampai {@code _4_SKS}).</li>
	 *   <li><b>Berbasis konversi:</b> {@code DIKALI_JUMLAH_SKS_MK_KONVERSI},
	 *   {@code DIKALI_JUMLAH_MK_KONVERSI}, {@code DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI}.</li>
	 *   <li><b>Bersyarat 0/1:</b> {@code DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA},
	 *   {@code DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU},
	 *   {@code DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA},
	 *   {@code DIKALI_SATU_JIKA_AMBIL_MK_SP}. Nominalnya {@code harga} atau {@code 0}.</li>
	 *   <li><b>Tunggakan:</b> {@link ItemBiaya#HITUNG_TUNGGAKAN_SMT_LALU} &mdash; satu-satunya
	 *   cabang yang juga mengisi {@link DetailBiaya#setTunggakanLalu(Double)}.</li>
	 * </ul>
	 *
	 * <h4>Catatan integritas finansial</h4>
	 * <ul>
	 *   <li><b>Tidak ada {@code else} penutup.</b> Bila {@code penghitungan} tidak cocok dengan
	 *   satu pun cabang, method selesai <b>tanpa menyentuh {@code nilaiBiayaBaru}</b>. Nilainya
	 *   tetap seperti sebelum pemanggilan &mdash; {@code null} pada objek baru, atau sisa
	 *   perhitungan lama pada objek yang dipakai ulang. Pemanggil seperti
	 *   {@code Kegiatan.ambilJumlahTagihan(...)} yang memakai "{@code nilaiBiayaBaru} masih
	 *   {@code null}" sebagai penanda "belum dihitung" karena itu akan memanggil method ini
	 *   berulang kali tanpa pernah mendapat hasil.</li>
	 *   <li><b>[DIPERBAIKI] {@link ItemBiaya#DIKALI_JUMLAH_SKS_UAS_REMDIAL} sempat tidak punya
	 *   cabang di sini.</b> Blok yang seharusnya menanganinya dulu menguji
	 *   {@link ItemBiaya#DIKALI_JUMLAH_SKS_UTS_REMEDIAL} untuk <b>kedua kalinya</b> &mdash; dua
	 *   blok kembar persis berturut-turut, sehingga blok kedua tak pernah terjangkau
	 *   ({@code else if} pertama sudah menangkap semua kasusnya). Akibatnya item biaya "dikali
	 *   jumlah SKS matakuliah remedial yang ada uas-nya" jatuh ke keadaan "tidak ada cabang yang
	 *   cocok" di atas: nominalnya tidak pernah dikalikan pada tagihan biasa, walau jalur bulanan
	 *   ({@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)})
	 *   sudah menanganinya dengan benar. Blok kedua kini menguji
	 *   {@link ItemBiaya#DIKALI_JUMLAH_SKS_UAS_REMDIAL} dengan saringan {@code getTerdapatUas()},
	 *   mencerminkan cabang yang sesuai di {@code ambilNominalModifikasi}.</li>
	 *   <li><b>Cabang parameter tambahan menjadikan nominal tagihan sebagai data biodata.</b>
	 *   Nilai yang dipakai berasal dari string {@code parameterTambahanInds} pada
	 *   {@link BiodataMahasiswa}, dibaca apa adanya dan langsung menjadi nominal &mdash; tanpa
	 *   batas atas, tanpa batas bawah, dan tanpa validasi selain "harus berupa angka". Siapa pun
	 *   yang berwenang menyunting biodata mahasiswa (termasuk jalur pengisian formulir tambahan
	 *   pada pendaftaran/daftar ulang) karena itu efektif berwenang menetapkan nominal item biaya
	 *   tersebut. Perubahannya tercatat sebagai revisi Envers pada {@code biodata_mahasiswa},
	 *   bukan sebagai perubahan tagihan &mdash; jadi pada laporan keuangan perubahan itu tampak
	 *   sebagai tagihan yang "berubah sendiri".</li>
	 *   <li><b>Parsing parameter tambahan gagal secara diam.</b> Baris yang formatnya tidak
	 *   sesuai, {@code id} yang tidak cocok, atau nilai yang bukan angka semuanya membuat cabang
	 *   ini selesai <b>tanpa mengisi {@code nilaiBiayaBaru}</b> (galat parsing ditelan
	 *   {@code Common.tampilErrorJikaAdmin}). Karena cabang ini paling depan dalam rantai, item
	 *   dengan {@code terhubungKeNilaiTambahan} menyala juga <b>tidak pernah</b> memakai skema
	 *   {@code penghitungan}-nya sendiri, walau operator sudah memilihnya.</li>
	 *   <li><b>{@code HITUNG_TUNGGAKAN_SMT_LALU} punya dua makna berbeda.</b> Bila
	 *   {@link Kegiatan} semester sebelumnya ada, yang dipakai adalah
	 *   {@code kegiatan.getAmountTerhutang()} &mdash; sisa yang benar-benar belum dibayar. Bila
	 *   tidak ada, helper menjumlahkan seluruh baris {@link DetailBiaya} semester sebelumnya
	 *   lewat {@code hitungTotalKegiatan(null)}, yang oleh {@link DetailBiaya} dialihkan ke
	 *   {@code hitungTotal()} &mdash; yaitu <b>total tagihan rencana</b>, bukan sisa terhutang.
	 *   Mahasiswa yang sudah melunasi semester lalu di luar mekanisme {@code Kegiatan} akan
	 *   ditagih ulang penuh. Pengambilan baris biaya itu juga mematok
	 *   {@code ConstantValues.PENDAFTARAN_MAHASISWA_LAMA}, sehingga mahasiswa yang semester
	 *   sebelumnya masih berstatus baru dibandingkan dengan daftar biaya yang salah. Selain itu
	 *   nilai negatif (kelebihan bayar/diskon) dibuang oleh saringan {@code nilai > 0.01},
	 *   sehingga kompensasi tidak pernah mengurangi tunggakan.</li>
	 *   <li><b>{@code semester == 1} tidak tertangani.</b> Syarat {@code semester > 1} adalah
	 *   bagian dari kondisi cabang, bukan penjaga di dalamnya; pada semester pertama skema ini
	 *   jatuh ke keadaan "tidak ada cabang yang cocok".</li>
	 *   <li><b>{@code DIKALI_JUMLAH_MK_KONVERSI} mengabaikan semester.</b> Cabang itu memanggil
	 *   {@code KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null)} &mdash; seluruh
	 *   matakuliah konversi sepanjang masa studi, bukan yang semester ini. Bila item biayanya
	 *   aktif tiap semester, jumlah yang sama ditagihkan berulang. Bandingkan dengan
	 *   {@code DIKALI_JUMLAH_SKS_MK_KONVERSI} tepat di atasnya yang memakai {@code semester}.</li>
	 *   <li><b>Varian "SP" ikut menghitung konversi non-SP.</b> Pada
	 *   {@code DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP} dan {@code ..._DISKUSI_TEORI_SP}, loop KRS
	 *   memang disaring {@link Perkuliahan#SEMESTER_PENDEK}, tetapi loop konversi sesudahnya
	 *   memakai pemanggilan yang sama persis dengan varian reguler (tanpa penyaring SP). SKS
	 *   konversi karena itu ikut ditagih pada biaya semester pendek.</li>
	 *   <li><b>Cabang matakuliah konversi pada skema ujian tidak pernah aktif.</b> Seluruh
	 *   cabang {@code DIKALI_JUMLAH_MK_*}/{@code DIKALI_JUMLAH_SKS_U*} membuang lebih dulu baris
	 *   ber-{@code perkuliahan} {@code null} ({@code continue}), lalu tetap menulis ternary
	 *   {@code perkuliahan == null ? matakuliahKonversi : ...}. Sisi {@code matakuliahKonversi}
	 *   dari ternary itu kode mati.</li>
	 *   <li><b>{@code NullPointerException} yang mungkin.</b> Harga dasar diambil tanpa
	 *   {@link #safeDouble(Double)} di dalam rantai, sehingga {@code nilaiBiaya} {@code null}
	 *   pada item berskema perkalian melempar saat perkalian. Hal serupa berlaku pada
	 *   {@code Matakuliah.getSks()}, {@code Perkuliahan.getSemester()}, dan
	 *   {@code getMatakuliahKonversi()} yang di-unbox/di-dereferensi tanpa penjaga.</li>
	 *   <li><b>Sisa debug.</b> Cabang
	 *   {@code DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA} masih memanggil
	 *   {@code System.out.println("daftarMk -> " + ...)} yang mencetak daftar matakuliah
	 *   mahasiswa ke log server pada setiap perhitungan.</li>
	 *   <li><b>Semantik cabang "dan semester sebelumnya".</b> Meski namanya menyebut semester
	 *   sebelumnya, yang sebenarnya diuji adalah apakah sebuah matakuliah dari daftar muncul
	 *   <b>lebih dari sekali</b> ({@code c > 1}) di seluruh riwayat sampai semester ini. Item
	 *   ditagih ({@code x 1}) hanya bila ada pengulangan.</li>
	 * </ul>
	 *
	 * <h4>Biaya eksekusi</h4>
	 * <p>Hampir setiap cabang memuat ulang seluruh {@link Detailperkuliahan} mahasiswa satu per
	 * satu melalui {@link GeneralValueObject#ambilData(Class, String)} &mdash; pola N+1 yang
	 * berlipat lagi pada cabang yang menjalankan dua loop (KRS dan konversi). Method ini
	 * dipanggil per baris rincian biaya, dan pada layar daftar tagihan dipanggil untuk setiap
	 * mahasiswa; pemanggil massal wajib memikirkan caching sendiri.</p>
	 *
	 * @param detailBiaya baris rincian biaya yang akan diisi nominal dan keterangannya; bila
	 *                    {@code null}, method langsung selesai tanpa efek
	 * @param mahasiswa   mahasiswa yang menjadi konteks perhitungan; {@code null} (atau
	 *                    ber-{@code id} {@code null}) membuat nominal jatuh ke harga dasar
	 * @param semester    semester yang menjadi konteks perhitungan; {@code null} membuat nominal
	 *                    jatuh ke harga dasar
	 * @see DetailBiaya#updateKeterangan(Mahasiswa, Integer)
	 * @see #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)
	 */
	@SuppressWarnings("unchecked")
	public static void updateKeterangan(DetailBiaya detailBiaya, Mahasiswa mahasiswa, Integer semester) {

		if (detailBiaya == null) {
			return;
		}

		ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
		if (itemBiaya == null) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		if (isTanpaPenghitungan(itemBiaya)) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		if (mahasiswa == null || mahasiswa.getId() == null || semester == null) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		JenisKegiatan jenisKegiatan = detailBiaya.getJenisKegiatan();

		if (mahasiswa != null && mahasiswa.getId() != null && detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getTerhubungKeNilaiTambahan() && detailBiaya.getItemBiaya().getParameterTambahan() != null) {
			String parameterTambahanInds = (String) HibernateUtil.currentSession()
					.createCriteria(BiodataMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.desc("id")).setMaxResults(1)
					.setProjection(Projections.property("parameterTambahanInds")).uniqueResult();
			if (parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
				String[] spl = parameterTambahanInds.split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					String lbl = value.length > 0 ? value[0].trim() : "";
					String val = value.length > 1 ? value[1].trim() : "";
					if (!val.isEmpty() && Common.isNumber(val)) {
						try {
							String[] labelParts = lbl.split("->");
							String parameterId = labelParts.length > 1 ? labelParts[1].trim() : "";
							if (parameterId.equalsIgnoreCase(detailBiaya.getItemBiaya().getParameterTambahan().getId().toString())) {
								detailBiaya.setNilaiBiayaBaru(Common.numberFormat.get().parse(val.trim()).doubleValue());
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MAHASISWA)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester() < semester) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester().equals(semester)) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
				&& semester > 1) {

			Kegiatan kegiatan = mahasiswa.ambilKegiatans((semester - 1), jenisKegiatan);
			if (kegiatan != null) {
				detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (Tungggakan semester " + (semester - 1) + " senilai Rp. "
						+ Common.numberFormat.get().format(kegiatan.getAmountTerhutang()) + ")");
				detailBiaya.setNilaiBiayaBaru(kegiatan.getAmountTerhutang());
				detailBiaya.setTunggakanLalu(kegiatan.getAmountTerhutang());
			} else {
				Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
						(semester - 1), ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

				double totalTunggakan = 0.0;
				for (Object o : detailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya biaya = (DetailBiaya) o;
						Double nilai = biaya.hitungTotalKegiatan(kegiatan);
						if (nilai > 0.01) {
							totalTunggakan += nilai;
						}
					}
				}
				detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (Tungggakan semester " + (semester - 1) + " senilai Rp. "
						+ Common.numberFormat.get().format(totalTunggakan) + ")");
				detailBiaya.setNilaiBiayaBaru(totalTunggakan);
				detailBiaya.setTunggakanLalu(totalTunggakan);
			}
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			double SKSMatakuliahKonversi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					SKSMatakuliahKonversi += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) SKSMatakuliahKonversi) + " SKS Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(SKSMatakuliahKonversi * harga);
		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getSemester() != null && mahasiswa != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA)) {

			boolean lulus = detailBiaya.getSemester().equals(mahasiswa.getSemesterLulus()) && mahasiswa.getSemesterLulus() != null
					&& ConstantValues.LULUS != null && mahasiswa.getStatusKeluar() != null
					&& mahasiswa.getStatusKeluar().getId().equals(ConstantValues.LULUS.getId());

			Double harga = detailBiaya.getNilaiBiaya();
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + (lulus ? " (" + Common.numberFormat.get().format(harga)
					+ ") x 1 karena lulus di semester " + mahasiswa.getSemesterLulus() : ""));
			detailBiaya.setNilaiBiayaBaru((lulus ? 1 : 0) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (data.isEmpty() ? 0 : 1) + " Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((data.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
				&& !detailBiaya.getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
			Double harga = detailBiaya.getNilaiBiaya();
			String nama = detailBiaya.getItemBiaya().getNamaMatakuliah().trim();
			String[] spl = nama.split(";");

			Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkTertentu(semester, spl);

			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? ""
									: d.getMatakuliahKonversi().getNama() + "-" + d.getMatakuliahKonversi().getNama())
							: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
									+ d.getPerkuliahan().getMatakuliah().getNama());
					daftarMk += daftarMk.isEmpty() ? dd : ", " + dd;
				}

			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (data.isEmpty() ? 0 : 1) + " " + detailBiaya.getItemBiaya().getNamaMatakuliah() + ", sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((data.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA)
				&& !detailBiaya.getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
			Double harga = detailBiaya.getNilaiBiaya();
			String nama = detailBiaya.getItemBiaya().getNamaMatakuliah().trim();
			String[] spl = nama.split(";");

			Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkSdSmtTertentu(semester, spl);

			Map<String, Integer> daftarMk = new HashMap<String, Integer>();
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? ""
									: d.getMatakuliahKonversi().getNama() + "-" + d.getMatakuliahKonversi().getNama())
							: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
									+ d.getPerkuliahan().getMatakuliah().getNama());

					Integer c = daftarMk.get(dd.toLowerCase());
					if (c == null) {
						c = 1;
					} else {
						c++;
					}
					daftarMk.put(dd.toLowerCase(), c);
				}

			}

			// Sisa debug: mencetak daftar matakuliah mahasiswa ke stdout server pada SETIAP
			// perhitungan tagihan. Dibiarkan apa adanya karena menghapusnya berada di luar
			// lingkup dokumentasi, tetapi ini kebocoran data akademik ke log dan beban I/O
			// pada layar rekap tagihan massal.
			System.out.println("daftarMk -> " + daftarMk);

			String mk = "";
			for (String m : daftarMk.keySet()) {
				Integer c = daftarMk.get(m.toLowerCase());
				if (c != null && c > 1) {
					mk += mk.isEmpty() ? m : ", " + m;
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (mk.isEmpty() ? 0 : 1) + " " + detailBiaya.getItemBiaya().getNamaMatakuliah() + ", sbb : " + mk);
			detailBiaya.setNilaiBiayaBaru((mk.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			// Perhatikan argumen kedua: null, BUKAN semester. Berbeda dari cabang
			// DIKALI_JUMLAH_SKS_MK_KONVERSI yang menyaring per semester, cabang "jumlah MK
			// konversi" ini menghitung seluruh matakuliah konversi sepanjang masa studi. Bila
			// item biayanya aktif tiap semester, jumlah yang sama ikut ditagihkan berulang.
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null);

			double SKSMatakuliahKonversi = data.size();
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd : "," + dd;
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) SKSMatakuliahKonversi) + " MK Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(SKSMatakuliahKonversi * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSPraktek = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {
					Integer sks = d.getMatakuliahKonversi().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSPraktek * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSPraktek = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {
					Integer sks = d.getMatakuliahKonversi().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSPraktek * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSDiskusi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSDiskusi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSDiskusi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSDiskusi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_SIMULASI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSSimulasi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksSimulasi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSSimulasi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksSimulasi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSSimulasi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSSimulasi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSSimulasi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}

					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(1)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 1 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(2)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 2 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(3)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 3 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(4)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 4 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		////////////////////////////////////////////////

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true, false,
					null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		// Penangan ItemBiaya.DIKALI_JUMLAH_SKS_UAS_REMDIAL (perhatikan ejaan konstanta:
		// REMDIAL, bukan REMEDIAL -- lihat ais/database/model/ItemBiaya.java baris ~468).
		//
		// Sampai dengan perbaikan ini, blok tersebut ditulis KEMBAR PERSIS dengan blok tepat
		// di atas: menguji DIKALI_JUMLAH_SKS_UTS_REMEDIAL dan menyaring dengan getTerdapatUts(),
		// sehingga tidak pernah tercapai (else-if di atas sudah menangkap seluruh kasusnya) --
		// dan skema DIKALI_JUMLAH_SKS_UAS_REMDIAL jadi TIDAK punya cabang sama sekali di jalur
		// tagihan biasa ini. Karena rantai if/else ini tidak punya else penutup, item biaya
		// berskema itu selesai tanpa nilaiBiayaBaru terisi (gagal diam: null pada objek baru,
		// atau sisa perhitungan lama pada objek yang dipakai ulang), sementara jalur tagihan
		// bulanan ambilNominalModifikasi(...) menanganinya dengan benar -- satu item biaya bisa
		// menghasilkan nominal berbeda tergantung jalurnya. Diperbaiki setelah audit data
		// historis (lihat ais-fix-... di memori kerja) menemukan tidak ada baris item_biaya
		// dengan skema ini di DB UAT, sehingga penggantian konstanta+saringan di bawah ini
		// tidak mengubah nominal tagihan yang sudah terbit.
		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_REMDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true, false,
					null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		///////////////////////////////////////////////

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_PRAKTEK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);

			String daftarMk = "";
			int jumlahMatakuliah = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getMerupakanMkPraktek()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_DISKUSI_TEORI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);

			String daftarMk = "";
			int jumlahMatakuliah = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getMerupakanMkTeori()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if ((detailperkuliahan.getPerkuliahan() == null
							|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		} else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue() > 0 ? 1 : 0) + " SP, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((jumlahMatakuliah.intValue() > 0 ? 1 : 0) * harga);

		}

		// Rantai if/else yang dimulai di atas (mencakup ~30 skema ItemBiaya.penghitungan) tidak
		// punya else penutup: bila tidak ada skema yang cocok -- mis. ItemBiaya.
		// DIAMBIL_DARI_DENDA_PERPUSTAKAAN, yang menurut Javadoc kelas ItemBiaya memang tidak
		// dirujuk di mana pun, atau konstanta *_PERTEMUAN yang juga tidak tercakup di sini --
		// detailBiaya.setNilaiBiayaBaru(...) tidak pernah dipanggil, sehingga nilainya tetap
		// seperti sebelum pemanggilan (null pada objek baru, atau sisa perhitungan lama pada
		// objek yang dipakai ulang): gagal diam yang sama seperti yang diperbaiki pada blok
		// DIKALI_JUMLAH_SKS_UAS_REMDIAL di atas.
		//
		// Sengaja TIDAK ditambal dengan else penutup ber-safeDouble(detailBiaya.getNilaiBiaya())
		// di sini: rantai ini hanya mencakup sebagian dari total konstanta penghitungan di
		// ItemBiaya, dan menambah else penutup akan mengubah perilaku untuk SEMUA skema yang
		// belum diaudit itu sekaligus -- risiko yang melebihi cakupan perbaikan tabrakan-konstanta
		// ini. Bila ingin ditutup, audit dulu satu per satu skema mana yang benar-benar dipakai
		// dan apakah data historisnya sudah terlanjur bergantung pada nilai lama/null ini.

	}

	/**
	 * Menghitung nominal satu baris {@link PengaturanPembayaranBulanan} untuk seorang mahasiswa
	 * pada semester tertentu &mdash; jalur tagihan <b>bulanan/cicilan</b>.
	 *
	 * <p>Ini kembaran {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)} untuk skema
	 * pembayaran per bulan. Perbedaan pokoknya ada tiga: harga dasarnya diambil dari
	 * {@link PengaturanPembayaranBulanan#getNominal()} (bukan {@code DetailBiaya.nilaiBiaya}),
	 * hasilnya <b>dikembalikan</b> alih-alih ditulis ke field, dan daftar matakuliah yang ikut
	 * dihitung disaring lagi per <b>tahapan</b> pembayaran.</p>
	 *
	 * <h4>Empat gerbang pulang cepat</h4>
	 * <ol>
	 *   <li>{@code pengaturanPembayaranBulanan} {@code null} &rarr; {@code 0.0};</li>
	 *   <li>{@link ItemBiaya} pada {@code detailBiaya}-nya tanpa skema penghitungan (lihat
	 *   {@link #isTanpaPenghitungan(ItemBiaya)}) &rarr; nominal dasar apa adanya. Perhatikan
	 *   bahwa {@code detailBiaya} {@code null} juga jatuh ke sini, karena {@code itemBiaya}
	 *   ikut menjadi {@code null};</li>
	 *   <li>{@code mahasiswa} atau {@code semester} {@code null} &rarr; nominal dasar apa
	 *   adanya;</li>
	 *   <li>{@link PengaturanPembayaranBulanan#getDikalikanDenganKondisiKhusus()} tidak menyala
	 *   &rarr; nominal dasar apa adanya, seluruh rantai rumus dilewati.</li>
	 * </ol>
	 * <p>Nominal dasar sendiri sudah dinormalkan: {@code getNominal()} yang {@code null} dibaca
	 * sebagai {@code 0.0}. Method ini karena itu <b>tidak pernah mengembalikan {@code null}</b>.</p>
	 *
	 * <h4>Gerbang {@code dikalikanDenganKondisiKhusus}</h4>
	 * <p>Gerbang keempat pantas digarisbawahi karena tidak punya padanan di jalur tagihan biasa.
	 * {@code dikalikanDenganKondisiKhusus} adalah <b>checkbox per baris bulanan</b>: selama tidak
	 * dicentang, skema {@code penghitungan} yang dipilih di master {@link ItemBiaya} sama sekali
	 * tidak berlaku dan mahasiswa ditagih nominal datar. Jadi dua sumber kebenaran mengatur hal
	 * yang sama &mdash; master biaya menyatakan "kalikan jumlah SKS", baris bulanan bisa
	 * membatalkannya secara diam-diam. Menambah baris bulanan baru lewat CRUD generik (yang tidak
	 * mencentang apa pun secara bawaan) berarti membuat baris yang mengabaikan skema biayanya,
	 * dan sebaliknya mencentangnya pada baris nominal datar akan mengubah nominal itu menjadi
	 * harga satuan yang dikalikan. Tidak ada persetujuan terpisah untuk perubahan itu; jejaknya
	 * hanya revisi Envers kolom {@code dikalikandengankondisikhusus}.</p>
	 *
	 * <h4>Penyaring tahapan</h4>
	 * <p>Sebelum masuk rantai, method menghitung
	 * {@code tahapan = pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester)} dan
	 * meneruskannya sebagai penyaring ke {@code Mahasiswa.ambilDetailperkuliahan(...)}, sehingga
	 * setiap baris bulanan hanya menagih porsi matakuliah tahapannya sendiri. Dua hal yang perlu
	 * diketahui:</p>
	 * <ul>
	 *   <li>{@link PengaturanPembayaranBulanan#hitungTahap(Mahasiswa, Integer)} mengembalikan
	 *   {@code 0} bila {@code ConstantValues.aktifkanTahapanTerhubungKeKeuangan} mati (bawaan
	 *   repo), dan nilai {@code 0} diperlakukan sama dengan {@code null} oleh
	 *   {@code ambilDetailperkuliahan(...)}. Pada instalasi bawaan, penyaring ini karena itu
	 *   tidak berefek apa-apa.</li>
	 *   <li>{@code hitungTahap} memanggil {@code getRealBulan()}, yang <b>menulis balik ke kolom
	 *   terpetakan {@code realbulan}</b>. Jadi sekadar menghitung nominal sudah bisa mengotori
	 *   entity ini &mdash; perwujudan pola getter-mutasi-field yang lazim di
	 *   {@code ais/database/model/}.</li>
	 * </ul>
	 *
	 * <h4>Efek samping yang tidak tersirat dari namanya</h4>
	 * <p>Meski berawalan {@code ambil}, setiap cabang rumus memanggil
	 * {@link PengaturanPembayaranBulanan#setKeterangan(String)} dengan rincian perhitungan.
	 * Berbeda dari {@code DetailBiaya.keterangan} yang {@code @Transient},
	 * {@code PengaturanPembayaranBulanan.keterangan} adalah <b>kolom terpetakan</b>. Pada
	 * instance yang masih dikelola sesi Hibernate, dirty-checking akan menerbitkan {@code UPDATE}
	 * beserta revisi Envers hanya karena seseorang membuka layar tagihan. Catatan operator yang
	 * pernah diketik di kolom itu ikut tertimpa. Inilah alasan {@code KegiatanHelper} menandai
	 * entity ini read-only saat hitung ulang massal, dan alasan pemanggil baru sebaiknya bekerja
	 * pada salinan yang sudah detach.</p>
	 *
	 * <h4>Cakupan rumus dan bedanya dengan jalur tagihan biasa</h4>
	 * <p>Rantainya menangani 28 cabang: keluarga berbasis SKS KRS, komponen praktek/diskusi/
	 * simulasi, komponen UTS/UAS (per matakuliah maupun per SKS, reguler maupun semester
	 * pendek), remedial (jumlah matakuliah, empat varian bobot SKS, serta SKS ber-UTS dan
	 * ber-UAS), konversi, dan cabang bersyarat 0/1. Perbedaannya dengan
	 * {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)} bukan hal sepele:</p>
	 * <ul>
	 *   <li><b>[DIPERBAIKI] {@link ItemBiaya#DIKALI_JUMLAH_SKS_UAS_REMDIAL} sempat hanya
	 *   ditangani jalur ini.</b> Di jalur tagihan biasa cabangnya sempat hilang (blok
	 *   {@code DIKALI_JUMLAH_SKS_UTS_REMEDIAL} tertulis dua kali), sehingga item biaya yang sama
	 *   menghasilkan nominal berbeda tergantung ia ditagih bulanan atau tidak. Kedua jalur kini
	 *   menangani skema ini secara setara.</li>
	 *   <li><b>Jalur ini tidak menangani {@link ItemBiaya#HITUNG_TUNGGAKAN_SMT_LALU}</b> dan
	 *   tidak punya cabang "nilai dari parameter tambahan biodata". Keduanya hanya ada di jalur
	 *   tagihan biasa. Baris bulanan dengan skema itu akan mengembalikan nominal dasar tanpa
	 *   perkalian, tanpa peringatan.</li>
	 * </ul>
	 *
	 * <h4>Catatan integritas finansial</h4>
	 * <ul>
	 *   <li><b>Dua cabang remedial sempat melewatkan penyaring tahapan (sudah diperbaiki).</b>
	 *   Dari 28 cabang, 26 selalu meneruskan {@code tahapan} ke
	 *   {@code ambilDetailperkuliahan(...)}; dua cabang &mdash;
	 *   {@link ItemBiaya#DIKALI_JUMLAH_SKS_UTS_REMEDIAL} dan
	 *   {@link ItemBiaya#DIKALI_JUMLAH_SKS_UAS_REMDIAL} &mdash; sebelumnya justru mengoper
	 *   {@code null}, tampaknya karena disalin dari jalur tagihan biasa yang memang tidak punya
	 *   konsep tahapan. Pada instalasi yang mengaktifkan
	 *   {@code aktifkanTahapanTerhubungKeKeuangan}, kedua skema itu menghitung SKS remedial
	 *   <b>seluruh semester</b> pada <b>setiap</b> baris bulanan, bukan porsi tahapannya
	 *   &mdash; sehingga jumlah yang sama tertagih berulang sebanyak jumlah tahapan. Pada
	 *   instalasi bawaan (fitur tahapan mati) efeknya tidak terlihat sama sekali, yang membuat
	 *   selisih ini mudah lolos pengujian sebelum diperbaiki. Audit data historis untuk
	 *   instalasi yang sempat mengaktifkan fitur tahapan belum dilakukan.</li>
	 *   <li><b>Tidak ada {@code else} penutup.</b> Sama seperti jalur tagihan biasa, skema yang
	 *   tidak cocok dengan satu pun cabang tidak memicu galat; nominal dasar dikembalikan seolah
	 *   item itu memang berharga tetap. Menambah konstanta {@code penghitungan} baru tanpa
	 *   menambah cabang di kedua method menghasilkan kegagalan diam.</li>
	 *   <li><b>Rincian keterangan menyimpan {@code keterangan} yang formatnya tidak seragam.</b>
	 *   Cabang {@link ItemBiaya#DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG} mencetak jumlah SKS
	 *   sebagai {@code double} mentah ({@code "x 20.0 SKS"}), sedangkan cabang sejenis lain
	 *   membulatkannya lebih dulu ke {@code int}. Perbedaan kosmetik, tetapi teks inilah satu-
	 *   satunya rekaman rumus yang tersimpan permanen, sehingga selisih format menyulitkan
	 *   pembacaan ulang secara otomatis.</li>
	 *   <li><b>Warisan cacat yang sama dengan jalur tagihan biasa.</b>
	 *   {@code DIKALI_JUMLAH_MK_KONVERSI} tetap memanggil
	 *   {@code KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null)} sehingga
	 *   mengabaikan semester; varian "SP" tetap menambahkan SKS konversi non-SP; dan seluruh
	 *   cabang keluarga ujian tetap membuang baris ber-{@code perkuliahan} {@code null} lewat
	 *   {@code continue} sebelum ternary konversinya, sehingga sisi konversi ternary itu kode
	 *   mati.</li>
	 *   <li><b>Aritmetika uang memakai {@code double}.</b> Hasil dikembalikan tanpa pembulatan;
	 *   pembulatan (kalau ada) terjadi di lapisan pemanggil.</li>
	 *   <li><b>Biaya query.</b> Setiap cabang memuat {@link Detailperkuliahan} satu per satu
	 *   lewat {@link GeneralValueObject#ambilData(Class, String)}. Karena tagihan bulanan berarti
	 *   banyak baris per mahasiswa per semester, jalur inilah yang paling mahal di antara
	 *   keduanya &mdash; satu layar rekap bisa memicu ribuan query.</li>
	 * </ul>
	 *
	 * @param pengaturanPembayaranBulanan baris pengaturan pembayaran bulanan yang dihitung;
	 *                                    {@code null} menghasilkan {@code 0.0}
	 * @param mahasiswa                   mahasiswa yang ditagih; {@code null} membuat nominal
	 *                                    dasar dikembalikan apa adanya
	 * @param semester                    semester berjalan; {@code null} membuat nominal dasar
	 *                                    dikembalikan apa adanya
	 * @return nominal setelah modifikasi; {@code 0.0} adalah hasil yang sah (mis. mahasiswa tidak
	 *         mengambil SKS apa pun) dan tidak pernah {@code null}
	 * @see PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)
	 * @see #updateKeterangan(DetailBiaya, Mahasiswa, Integer)
	 */
	public static Double ambilNominalModifikasi(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {

		if (pengaturanPembayaranBulanan == null) {
			return Double.valueOf(0.0);
		}

		Double nominalModifikasi = pengaturanPembayaranBulanan.getNominal();
		if (nominalModifikasi == null) {
			nominalModifikasi = Double.valueOf(0.0);
		}

		DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
		ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
		if (isTanpaPenghitungan(itemBiaya)) {
			return nominalModifikasi;
		}

		if (mahasiswa == null || semester == null) {
			return nominalModifikasi;
		}

		Integer tahapan = pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester);
		if (pengaturanPembayaranBulanan.getDetailBiaya() != null) {

			if (pengaturanPembayaranBulanan.getDikalikanDenganKondisiKhusus()) {

				if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MAHASISWA)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester() < semester) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null
								&& d.getPerkuliahan().getSemester().equals(semester)) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getSemester() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA)) {

					boolean lulus = pengaturanPembayaranBulanan.getDetailBiaya().getSemester().equals(mahasiswa.getSemesterLulus())
							&& mahasiswa.getSemesterLulus() != null && ConstantValues.LULUS != null
							&& mahasiswa.getStatusKeluar() != null
							&& mahasiswa.getStatusKeluar().getId().equals(ConstantValues.LULUS.getId());

					Double harga = pengaturanPembayaranBulanan.getNominal();
					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
							+ (lulus ? " (" + Common.numberFormat.get().format(harga) + ") x 1 karena lulus di semester "
									+ mahasiswa.getSemesterLulus() : ""));
					nominalModifikasi = ((lulus ? 1 : 0) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (data.isEmpty() ? 0 : 1) + " Konversi, sbb : " + daftarMk);
					nominalModifikasi = ((data.isEmpty() ? 0 : 1) * harga);
				} else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
						&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
					Double harga = pengaturanPembayaranBulanan.getNominal();

					String nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim();
					String[] spl = nama.split(";");

					Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkTertentu(semester, spl);

					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? ""
											: d.getMatakuliahKonversi().getNama() + "-"
													+ d.getMatakuliahKonversi().getNama())
									: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
											+ d.getPerkuliahan().getMatakuliah().getNama());
							daftarMk += daftarMk.isEmpty() ? dd : ", " + dd;
						}

					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (data.isEmpty() ? 0 : 1) + " "
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah() + ", sbb : " + daftarMk);
					nominalModifikasi = ((data.isEmpty() ? 0 : 1) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA)
						&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					String nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim();
					String[] spl = nama.split(";");

					Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkSdSmtTertentu(semester, spl);

					Map<String, Integer> daftarMk = new HashMap<String, Integer>();
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? ""
											: d.getMatakuliahKonversi().getNama() + "-"
													+ d.getMatakuliahKonversi().getNama())
									: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
											+ d.getPerkuliahan().getMatakuliah().getNama());

							Integer c = daftarMk.get(dd.toLowerCase());
							if (c == null) {
								c = 1;
							} else {
								c++;
							}
							daftarMk.put(dd.toLowerCase(), c);
						}

					}

					// Sisa debug: mencetak daftar matakuliah mahasiswa ke stdout server pada SETIAP
					// perhitungan tagihan. Dibiarkan apa adanya karena menghapusnya berada di luar
					// lingkup dokumentasi, tetapi ini kebocoran data akademik ke log dan beban I/O
					// pada layar rekap tagihan massal.
					System.out.println("daftarMk -> " + daftarMk);

					String mk = "";
					for (String m : daftarMk.keySet()) {
						Integer c = daftarMk.get(m.toLowerCase());
						if (c != null && c > 1) {
							mk += mk.isEmpty() ? m : ", " + m;
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (mk.isEmpty() ? 0 : 1) + " "
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah() + ", sbb : " + mk);

					nominalModifikasi = ((mk.isEmpty() ? 0 : 1) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((jumlahMatakuliah > 0 ? 1 : 0)) + " jumlah matakuliah, sbb : " + daftarMk);
					nominalModifikasi = ((jumlahMatakuliah > 0 ? 1 : 0) * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					double SKSMatakuliahKonversi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							SKSMatakuliahKonversi += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) SKSMatakuliahKonversi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (SKSMatakuliahKonversi * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					// Perhatikan argumen kedua: null, BUKAN semester. Berbeda dari cabang
					// DIKALI_JUMLAH_SKS_MK_KONVERSI yang menyaring per semester, cabang "jumlah MK
					// konversi" ini menghitung seluruh matakuliah konversi sepanjang masa studi.
					// Bila item biayanya aktif tiap semester, jumlah yang sama ikut ditagihkan
					// berulang.
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null);

					double SKSMatakuliahKonversi = data.size();
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd : "," + dd;
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) SKSMatakuliahKonversi) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (SKSMatakuliahKonversi * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSPraktek = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Integer sks = d.getMatakuliahKonversi().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSPraktek * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSPraktek = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Integer sks = d.getMatakuliahKonversi().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSPraktek * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSDiskusi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSDiskusi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSDiskusi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSDiskusi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_SIMULASI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSSimulasi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksSimulasi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSSimulasi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksSimulasi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSSimulasi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSSimulasi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSSimulasi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}

							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}
					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(1)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 1 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(2)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 2 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(3)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 3 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(4)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 4 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				////////////////////////////////////////////////

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				// PENYARING TAHAPAN DIPERBAIKI. Argumen kedua ambilDetailperkuliahan(...) di bawah
				// adalah "tahapan", dan 26 dari 28 cabang di method ini mengoper variabel
				// "tahapan" hasil hitungTahap(...). Cabang ini (dan kembarannya untuk
				// DIKALI_JUMLAH_SKS_UAS_REMDIAL beberapa blok di bawah) sampai dengan perbaikan
				// ini justru mengoper null, tampaknya karena disalin dari updateKeterangan(...)
				// yang memang tidak mengenal tahapan. Akibatnya, bila
				// ConstantValues.aktifkanTahapanTerhubungKeKeuangan aktif, SKS remedial SELURUH
				// semester dihitung pada SETIAP baris bulanan -- jumlah yang sama tertagih
				// berulang sebanyak jumlah tahapan. Pada instalasi bawaan (fitur tahapan mati)
				// hitungTahap mengembalikan 0 yang diperlakukan sama dengan null, sehingga
				// selisih ini tidak terlihat sama sekali -- itu sebabnya bug ini lolos pengujian.
				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null,
							true, false, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				// Penangan DIKALI_JUMLAH_SKS_UAS_REMDIAL di jalur tagihan bulanan ini.
				//
				// PENYARING TAHAPAN DIPERBAIKI juga di sini -- lihat catatan pada cabang
				// DIKALI_JUMLAH_SKS_UTS_REMEDIAL di atas; argumen kedua kini "tahapan".
				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_REMDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null,
							true, false, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				///////////////////////////////////////////////

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_PRAKTEK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);

					String daftarMk = "";
					int jumlahMatakuliah = 0;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getMerupakanMkPraktek()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					double jml = (double) jumlahMatakuliah;

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jml * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_DISKUSI_TEORI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);

					String daftarMk = "";
					int jumlahMatakuliah = 0;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getMerupakanMkTeori()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if ((detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}
			}
		}

		return nominalModifikasi;
	
	}
}
