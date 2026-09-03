package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
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
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.action.master.asset.util.AssetUtil;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Katalog <b>komponen gaji</b> (baris slip gaji) untuk satu format penggajian: satu record mewakili
 * satu pos seperti "Gaji Pokok", "Tunjangan Jabatan", "Tunjangan Transport", "Potongan BPJS", atau
 * "Total Diterima". Tabel: {@code payroll.item_gaji}. Entity ini adalah <b>master/katalog murni</b>
 * — ia mendefinisikan <i>apa</i> yang muncul di slip gaji, <i>berapa urutannya</i>, <i>dengan rumus
 * apa dihitung</i>, dan <i>ke akun buku besar mana dijurnal</i>, tetapi <b>tidak pernah menyimpan
 * nominal rupiah milik seorang pegawai</b> dan <b>tidak punya properti {@code pegawai} sama
 * sekali</b> (lihat catatan keamanan di bawah — ini pembeda penting terhadap entity payroll
 * lainnya).
 *
 * <h2>Kedudukan dalam rantai penggajian (empat lapis)</h2>
 * <p>Data penggajian AIS mengalir melalui empat lapis entity yang bentuknya sengaja dibuat mirip
 * (nama field hampir identik) sehingga mudah tertukar saat membaca kode:</p>
 * <ol>
 *   <li>{@link FormatItemGaji} &mdash; <b>skema slip</b>. Satu format = satu "template slip gaji"
 *   yang berlaku untuk kombinasi {@code cabang}/{@code departemen}/{@code levelJabatan}/
 *   {@code satuanKerja} tertentu. Inilah satu-satunya pemilik kolom tenant di seluruh rantai ini.</li>
 *   <li><b>{@code ItemGaji}</b> (kelas ini) &mdash; <b>baris-baris katalog</b> milik satu format.
 *   Berhierarki lewat {@link #getParent()} sehingga slip bisa punya sub-total bertingkat.</li>
 *   <li>{@link ais.database.model.payroll.ItemGajiPegawai} &mdash; <b>penyesuaian per pegawai</b>.
 *   Menambahkan kolom {@code pegawai} dan bendera {@code ikutiItemGaji}. Perhatikan: lapis ini
 *   <b>bukan salinan</b> &mdash; selama {@code ikutiItemGaji} bernilai {@code true}, getter
 *   {@code nama}/{@code kode}/{@code defaultFormula}/{@code nomorUrut}/{@code aktif}/
 *   {@code tampilkanDiSlip}/{@code space}/{@code nilaiVariableBisaDiubah}/{@code finalGaji} di sana
 *   mendelegasikan langsung ke katalog ini (live). Mematikan {@code ikutiItemGaji} baru
 *   "melepaskan" baris tersebut sehingga rumus per orang bisa berbeda (mis. Gaji Pokok yang
 *   nilainya spesifik per pegawai).</li>
 *   <li>{@link ais.database.model.payroll.RencanaItemGajiPegawai} (rencana/anggaran gaji) dan
 *   {@link ais.database.model.payroll.PembayaranItemGajiPegawai} (realisasi/slip yang dibayar dan
 *   dijurnal) &mdash; <b>baris dokumen</b>, satu per pegawai per periode, membawa nominal hasil
 *   hitung.</li>
 * </ol>
 * <p>Arah rujukan selalu turun: {@code PembayaranItemGajiPegawai} &rarr; {@code ItemGajiPegawai}
 * &rarr; {@code ItemGaji}. Kelas ini tidak menyimpan koleksi balik ke lapis manapun.</p>
 * <p><b>Kebijakan snapshot berbeda-beda per lapis</b> &mdash; ini penting saat menjawab pertanyaan
 * "kalau katalog diubah, apakah slip lama ikut berubah?":</p>
 * <ul>
 *   <li>Lapis 3 (per pegawai): <b>live</b> selama {@code ikutiItemGaji} menyala.</li>
 *   <li>Lapis 4 (rencana/pembayaran): <b>snapshot bersyarat</b>. Nominal ({@code nilai})
 *   dibekukan saat baris dibuat, dan pasangan akun disalin saat itu juga. Tetapi teks
 *   {@code nama}/{@code kode}/{@code defaultFormula} memakai pola "isi kalau masih kosong"
 *   ({@code if (defaultFormula == null) defaultFormula = ...}), jadi baris yang kolomnya belum
 *   pernah termaterialisasi akan memungut teks katalog <i>hari ini</i>. Sementara
 *   {@code tampilkanDiSlip} dan {@code space} di lapis itu <b>selalu</b> dibaca hidup dari lapis
 *   di atasnya, tidak pernah dibekukan.</li>
 * </ul>
 *
 * <h2>Kolom dan pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; label:</b> {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Penempatan di slip (tata letak):</b> {@link #getFormatItemGaji()} (pemilik),
 *   {@link #getParent()} (induk hierarki), {@link #getNomorUrut()}, {@link #getDeep()},
 *   {@link #getSpace()}, {@link #getTampilkanDiSlip()}.</li>
 *   <li><b>Perhitungan:</b> {@link #getDefaultFormula()}, {@link #getJadikan0JikaMinus()},
 *   {@link #getFinalGaji()}, {@link #getNilaiVariableBisaDiubah()}, serta konstanta variabel
 *   absensi {@link #V_TERL}, {@link #V_LEM}, {@link #V_CEP}, {@link #V_JAM}.</li>
 *   <li><b>Penjurnalan:</b> {@link #getAkun()}, {@link #getAkunDebet()}, {@link #ambilAkun()},
 *   {@link #ambilAkunDebet()}, {@link #getKelompokItemGaji()}, {@link #reloadKelompokItemGaji()},
 *   {@link #kelompokItemGajis}.</li>
 *   <li><b>Status &amp; statistik pemakaian:</b> {@link #getAktif()}, {@link #getJmlDipakai()}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 *   {@link #onUpdate()}, plus anotasi {@code @Audited} (Envers) pada kelas.</li>
 * </ul>
 *
 * <h2>Rumus perhitungan &mdash; katalog ini MEMANG membawa rumus</h2>
 * <p>Berbeda dari katalog master keuangan di paket {@code akunting} (mis. {@code JenisUangMuka},
 * {@code JenisPengeluaran}) yang hanya berisi nama + akun, katalog gaji ini <b>menyimpan rumus
 * perhitungan</b> pada kolom {@link #getDefaultFormula() default_formula}. Rumus ditulis sebagai
 * <b>ekspresi teks dengan token dipisah spasi</b> dan dievaluasi oleh mesin ekspresi
 * <b>exp4j</b> ({@code ExpressionBuilder}/{@code Expression}) di
 * {@code ais.action.master.payroll.util.ItemGajiPegawaiTreeModel#hitungItemGajiPegawai(...)},
 * diperkaya fungsi/operator kustom dari {@code LogicalUtil.ALL_FUNCTION} dan
 * {@code LogicalUtil.ALL_OPERATOR}. Perbendaharaan fungsinya: {@code if(kondisi, a, b)} (kondisi
 * dianggap benar bila bernilai persis {@code 1}), {@code round}, {@code roundup(x, n)},
 * {@code rounddown(x, n)} (pembulatan ke kelipatan {@code n}), {@code upper}/{@code lower}, serta
 * keluarga <b>ber-arity eksplisit</b> {@code sum2..sum99}, {@code max2..max99}, {@code min<N>},
 * {@code avg<N>}, {@code minnotnol<N>}, {@code ratanotnol<N>} &mdash; jadi penjumlahan tiga
 * komponen ditulis {@code sum3( A , B , C )}, bukan {@code sum(...)}. Operator pembanding yang
 * terdaftar hanya {@code >}, {@code <}, {@code >=}, {@code <=} dan {@code =}; <b>{@code !=} tidak
 * pernah didaftarkan</b> sehingga rumus yang memakainya gagal di-parse meskipun normalisasi spasi
 * memperlakukannya seolah operator. Perhatikan juga {@code %} <b>bukan</b> operator persen
 * melainkan modulo &mdash; persentase harus ditulis sebagai perkalian desimal
 * ({@code GAPOK * 0.05}). Sebuah token di dalam rumus dapat berupa:</p>
 * <ul>
 *   <li><b>angka literal</b> (mis. {@code 2500000});</li>
 *   <li><b>variabel absensi/kepegawaian</b> yang <i>disubstitusi secara tekstual</i> sebelum
 *   evaluasi &mdash; antara lain {@code V_HDR}, {@code V_THDR}, {@code V_SKT}, {@code V_IZIN},
 *   {@code V_ALPA}, {@code V_CUTI}, {@code V_TPT}, {@code V_MSK_LIBUR}, {@code V_TERLAMBAT},
 *   {@code V_CEPAT} (sumber: rekap bulanan {@code KehadiranPegawaiBulanan}); {@code GAPOK},
 *   {@code MAKAN}, {@code TRANSPORT}, {@code INSENTIF}, {@code LAIN_LAIN} (dari
 *   {@code StandarGaji} pegawai); {@code MK}, {@code MK_FIX}, {@code MASA_KERJA_THN},
 *   {@code HONOR_THN}, {@code ST_BLN} dan sejenisnya (masa kerja); {@code JUMLAH_ANAK},
 *   {@code JUMLAH_ISTRI}, {@code JUMLAH_ANAK_UMUR_<n>}, {@code PTKP_PEGAWAI} (data keluarga/pajak);
 *   variabel beban mengajar guru/dosen ({@code JML_JP}, {@code SKS_DSN_PERK}, {@code V_SUM_UTS},
 *   dst.); serta empat konstanta yang dideklarasikan di kelas ini &mdash; {@link #V_TERL},
 *   {@link #V_LEM}, {@link #V_CEP}, {@link #V_JAM} &mdash; yang di-<i>bind</i> sebagai variabel
 *   exp4j dari penjumlahan rekap harian {@code StatuskehadiranKaryawanHarian}. Selain nama tetap
 *   itu, seluruh {@code kode} milik {@code Konstanta}, {@code ParameterTambahanGajiPegawai},
 *   {@code GajiTabahan}, {@code PenilaianKpi} dan {@code JenisTransaksiPegawai} yang aktif juga
 *   ikut menjadi variabel;</li>
 *   <li><b>{@code kode} komponen gaji lain</b>. Token yang bukan angka dan bukan kata cadangan
 *   dianggap sebagai referensi ke item gaji lain: mesin mencari
 *   {@code ItemGajiPegawai} dengan {@code kode} sama, {@code pegawai} sama dan
 *   {@code formatItemGaji} sama, lalu <b>mengevaluasi rumus item tersebut secara rekursif</b>.
 *   Inilah cara "Total Gaji Kotor" ditulis sebagai {@code GP + TJAB + TTRANS}, atau potongan
 *   ditulis sebagai persentase komponen lain (mis. {@code GP * 0.04}).</li>
 * </ul>
 * <p><b>Hal non-obvious yang penting diketahui saat menulis rumus:</b></p>
 * <ul>
 *   <li><b>Spasi bersifat wajib &mdash; dan bukan sekadar soal gaya.</b> Setiap substitusi
 *   dikerjakan sebagai {@code StringUtils.replace(formula, " KODE ", " nilai ")}, yaitu pencocokan
 *   token yang <i>diapit spasi</i>. Mesin memang memanggil {@code ItemGajiPegawaiTreeModel.fixing()}
 *   yang menyisipkan spasi di sekitar {@code ( ) + - * / , %} dan operator pembanding, tetapi
 *   normalisasi itu berjalan <b>setelah</b> sebagian substitusi variabel bawaan sudah lewat &mdash;
 *   sehingga penulisan tanpa spasi bisa menghasilkan hasil yang berbeda, bukan sekadar gagal.
 *   Selain itu, layar pembayaran memecah rumus dengan {@code split(" ")} secara posisional untuk
 *   merender kotak isian per token (lihat {@link #getNilaiVariableBisaDiubah()}), jadi spasi ikut
 *   menentukan tampilan UI.</li>
 *   <li><b>Penjaga rekursi ada, tetapi berupa penghitung (bukan deteksi siklus).</b> Rujukan
 *   melingkar tidak menyebabkan {@code StackOverflowError}: rekursi dibatasi ambang 25. Di
 *   {@code ItemGajiTreeModel} ambang itu adalah kedalaman rekursi murni ({@code coba > 25} &rarr;
 *   kembalikan {@code 0.0}). Di {@code ItemGajiPegawaiTreeModel} ambangnya dipegang {@code Map}
 *   per-{@code kode} yang <b>tidak pernah dibersihkan</b> selama umur tree model, sehingga yang
 *   dihitung bukan kedalaman melainkan <b>total berapa kali kode itu dievaluasi dalam satu
 *   pemuatan pohon</b>. Konsekuensi praktisnya penting: sebuah komponen yang secara sah dirujuk
 *   oleh lebih dari 25 komponen lain (mis. Gaji Pokok pada slip yang panjang) akan <b>diam-diam
 *   bernilai {@code 0.0}</b> mulai rujukan ke-26, tanpa pesan kesalahan apa pun. Tidak ada validasi
 *   di layar simpan ({@code ItemGajiAction.onSave}) yang mencegah rumus melingkar dibuat.</li>
 *   <li><b>Urutan evaluasi ikut menentukan hasil.</b> Hasil tiap komponen dititipkan ke peta
 *   {@code dataVar} dan disubstitusi secara buta ke rumus berikutnya, sehingga dua rumus yang
 *   saling merujuk dapat memberi angka berbeda tergantung urutan node dirender.</li>
 *   <li><b>{@link #V_LEM} tumpang tindih nama.</b> Token {@code " V_LEM "} yang sudah diapit spasi
 *   lebih dulu disubstitusi tekstual dari rekap bulanan {@code KehadiranPegawaiBulanan.getLembur()};
 *   binding harian ({@code StatuskehadiranKaryawanHarian.jumlahLemburMasuk}) yang memakai konstanta
 *   {@link #V_LEM} baru kebagian bila token itu semula <i>tidak</i> diapit spasi dan baru terpisah
 *   setelah normalisasi operator. Praktisnya: dua sumber angka lembur yang berbeda bisa terpakai
 *   tergantung penulisan spasi &mdash; kuirk yang layak diwaspadai saat menyalin rumus antar
 *   instalasi. {@link #V_TERL}, {@link #V_CEP} dan {@link #V_JAM} tidak punya kembaran tekstual
 *   bernama sama, jadi ketiganya selalu berasal dari rekap harian (bandingkan
 *   {@code V_TERLAMBAT}/{@code V_CEPAT} yang berasal dari rekap bulanan &mdash; nama mirip, sumber
 *   tabel berbeda, dan tidak ada apa pun di UI yang menjelaskan perbedaannya).</li>
 *   <li>Rumus di sini hanyalah <b>nilai bawaan</b>. {@code ItemGajiPegawai} boleh menimpanya per
 *   pegawai, dan bila {@link #getNilaiVariableBisaDiubah()} bernilai {@code true} operator bahkan
 *   boleh mengetik angka lain langsung di layar pembayaran.</li>
 * </ul>
 *
 * <h2>Relasi ke {@link ais.database.model.akunting.Akun} &mdash; TERVERIFIKASI, dan tidak
 * sesederhana kelihatannya</h2>
 * <p>Entity ini memang punya dua kolom FK ke bagan akun: {@code akun} ({@link #getAkun()}) dan
 * {@code akun_debet} ({@link #getAkunDebet()}). Peran keduanya dipastikan dari mesin posting
 * {@code PostingTransaksiPembayaranGajiAction} / {@code PostingTransaksiPenggajianAction}:
 * <b>{@code akun} masuk sisi KREDIT jurnal, {@code akunDebet} masuk sisi DEBET</b> &mdash; sesuai
 * pola gaji (Dr Beban Gaji / Cr Utang Gaji atau Kas/Bank). Rantai pembacaannya:</p>
 * <pre>{@code
 * PembayaranItemGajiPegawai.getAkun()      // baris slip; bila kolomnya sendiri null ...
 *   -> ItemGajiPegawai.getItemGaji()       // ... jatuh ke katalog
 *     -> ItemGaji.getAkun()                // kelas ini
 *       -> ambilAkun()
 *         -> KelompokItemGaji.getAkun()    // peta JSON per satuan kerja
 *         -> AssetUtil.ambilDataAkun(json, formatItemGaji.getSatuanKerja())
 *           -> Akun
 * }</pre>
 * <p>Jadi akun jurnal komponen gaji <b>bukan sekadar FK yang dipilih operator</b>: sumber
 * sebenarnya adalah {@link KelompokItemGaji}, yang menyimpan pemetaan akun sebagai <b>array JSON
 * {@code {key, akun, satuanKerja}}</b> pada kolom teks {@code akun}/{@code akun_debet}. Satu
 * kelompok bisa memetakan akun berbeda untuk tiap satuan kerja, dengan satu entri ber-{@code
 * satuanKerja} {@code null} sebagai default. {@link #ambilAkun()} memilih entri yang cocok dengan
 * {@code formatItemGaji.getSatuanKerja()}. Konsekuensinya: memindahkan akun beban gaji seluruh
 * instalasi cukup dilakukan dari layar "Kelompok Item Gaji", bukan dari layar Item Gaji.</p>
 * <p><b>Tidak ada bendera "penambah" vs "pengurang".</b> Katalog ini tidak punya kolom yang
 * menyatakan sebuah komponen adalah gaji kotor atau potongan, dan tanda hasil rumus pun tidak
 * dipakai untuk menentukan arah jurnal — mesin posting menyaring {@code nilai > 0.1} dan
 * {@link #getJadikan0JikaMinus()} bahkan memaksa hasil negatif menjadi nol. Arah jurnal
 * <b>sepenuhnya ditentukan oleh slot akun mana yang terisi</b>: komponen yang hanya boleh
 * membebani satu sisi cukup dibiarkan tidak memiliki pemetaan pada slot lawannya. Kaki lawan
 * kas/bank ditambahkan terpisah oleh mesin posting dari {@code Bank.getAkun()} pegawai, atau
 * {@code CaraPembayaranGaji.getAkun()} bila bank tidak diketahui.</p>
 * <p>Pembacaan akun di jalur pembayaran bersifat <b>live-read, bukan snapshot</b>: selama kolom
 * {@code akun} pada baris {@code PembayaranItemGajiPegawai} masih {@code null}, mengubah pemetaan
 * di {@link KelompokItemGaji} akan mengubah akun yang dipakai untuk posting berikutnya. Ini
 * kebalikan dari pola snapshot pada {@code JenisReimbursement} di paket {@code akunting}.</p>
 *
 * <h2>Riwayat: tiga getter di kelas ini DULU bersifat menulis-balik (write-back) — DIPERBAIKI</h2>
 * <p>Sampai perbaikan ini, {@link #getAkun()}, {@link #getAkunDebet()} dan
 * {@link #getKelompokItemGaji()} tidak murni membaca &mdash; ketiganya menugaskan ulang field
 * instance sebelum mengembalikannya. Karena entity ini dipetakan <i>property access</i> (anotasi ada
 * di getter) dan memakai {@code dynamicUpdate = true}, setiap {@code flush} Hibernate atas instance
 * yang sekadar sudah <i>dibaca</i> (bukan disunting) menuliskan nilai baru itu ke database secara
 * permanen, tanpa aksi simpan eksplisit dari pengguna &mdash; satu klik pemilihan di
 * {@code AmbilDataItemGajiBanbox}, atau sekadar merender satu baris grid di
 * {@code ItemGajiAction.ItemGajiRenderer}, sudah cukup memicunya. Ketiga getter itu <b>sekarang
 * murni</b>: hasil resolusi (pencocokan kode kelompok, penurunan akun dari pemetaan kelompok) hanya
 * dipakai sebagai nilai kembalian, tidak pernah ditugaskan ke field yang dipetakan Hibernate. FK yang
 * dipilih operator di form Item Gaji karena itu tidak lagi bisa tertimpa hanya karena barisnya
 * dibaca; field hanya berubah lewat {@code setAkun}/{@code setAkunDebet}/{@code setKelompokItemGaji}
 * eksplisit dari alur simpan.</p>
 * <p>Perilaku <i>bisnis</i> tidak berubah: pengelompokan efektif dan akun jurnal efektif yang
 * dipakai mesin posting tetap ditentukan oleh kecocokan teks {@code kode} dan pemetaan kelompok yang
 * resolvable, persis seperti sebelumnya (lihat {@link #ambilAkun()}/{@link #ambilAkunDebet()}/
 * {@link #getKelompokItemGaji()}) &mdash; yang dihapus hanyalah efek samping penulisan permanen ke
 * FK tersimpan. Pola serupa (menimpa {@code akun} dengan {@code akunOver}) masih ada di
 * {@code Transaksi.getAkun()} paket {@code akunting}; tidak ikut disentuh oleh perbaikan ini.</p>
 *
 * <h2>Cakupan tenant (satuan kerja) &mdash; tidak ada di entity ini</h2>
 * <p>{@code ItemGaji} <b>tidak punya kolom tenant sama sekali</b>: tidak ada {@code yayasan},
 * {@code sekolah}, {@code satuanKerja}, maupun {@code pegawai}. Satu-satunya jalur ke tenant adalah
 * dua lompatan: {@code itemGaji.formatItemGaji.satuanKerja}. Akibat yang terverifikasi:</p>
 * <ul>
 *   <li>{@code ItemGajiAction.initCriteria()} menyaring hanya berdasarkan {@code kode}, {@code nama}
 *   dan (opsional, pilihan pengguna sendiri) {@code formatItemGaji} &mdash; <b>tanpa satu pun
 *   restriksi satuan kerja</b>. Daftar Item Gaji bersifat global lintas tenant.</li>
 *   <li>{@code AmbilDataItemGajiBanbox.onSearchDefault()} sama: hanya {@code nama} ilike +
 *   {@code aktif}, diurutkan {@code jmlDipakai} menurun. Bandbox pemilih pun global &mdash; dan
 *   combo format yang seharusnya membatasinya ({@code Common.insertCombo(..., FormatItemGaji.class,
 *   ...)}) juga mendaftar seluruh format lintas satuan kerja, padahal justru
 *   {@link FormatItemGaji} inilah entity pembawa tenant.</li>
 *   <li>{@code ItemGajiTreeModel} memang selalu menyaring {@code formatItemGaji}, tetapi nilainya
 *   diambil dari pilihan bandbox pengguna, bukan diturunkan dari {@code Tbmuser.getSatuanKerja()}
 *   &mdash; jadi pohon hanya sekuat pembatas combo di atas.</li>
 *   <li>Katalog kelompok ({@link #kelompokItemGajis}) dimuat sebagai <b>{@code static} se-JVM</b>
 *   tanpa penyaring tenant &mdash; wajar karena {@link KelompokItemGaji} sendiri memang tidak punya
 *   kolom tenant, tetapi berarti pemetaan akun antar-tenant hidup dalam satu ruang nama kode yang
 *   sama.</li>
 * </ul>
 * <p><b>Verifikasi terhadap {@code task_7b6038ac}</b> (Generic CRUD v2 &mdash; pembatas cakupan
 * hanya dipasang untuk properti relasi yang namanya persis ada di whitelist nama tetap
 * {@code yayasan|sekolah|program|fakultas|jurusan|satuanKerja} plus enam nama "aktor" yang
 * bersyarat peran). Hasil pemeriksaan dari sisi katalog ini sendiri:</p>
 * <ul>
 *   <li><b>Sebab yang dipakai untuk memvonis seluruh {@code payroll/*} TIDAK berlaku di sini.</b>
 *   Argumen task itu adalah "properti {@code pegawai} berada di luar whitelist"; {@code ItemGaji}
 *   <b>sama sekali tidak punya properti {@code pegawai}</b> (yang punya adalah
 *   {@link ais.database.model.payroll.ItemGajiPegawai}). Dalam hal ini entity ini memang katalog
 *   murni, sekelas {@code Akun}/{@code GrupAkun} di paket {@code akunting}.</li>
 *   <li><b>Namun entity ini tetap terjangkau, lewat mekanisme yang berbeda dan lebih dalam.</b>
 *   Seluruh properti relasinya bernama {@code formatItemGaji}, {@code kelompokItemGaji},
 *   {@code parent}, {@code akun}, {@code akunDebet} &mdash; tidak satu pun ada di whitelist mana
 *   pun. {@code GenericCrudAutoEntityAdapter.addScope(...)} menelan diam-diam kegagalan
 *   {@code getPropertyType} untuk properti yang tidak ada, sehingga peta pembatas yang dihasilkan
 *   <b>kosong sepenuhnya</b>, dan {@code applyScope}/{@code validateObjectScope} yang beriterasi di
 *   atas peta kosong itu tidak memasang satu pun restriksi. {@code GenericCrudScopeGuard} tidak
 *   menolong karena ia hanya menolak bila <i>adapter</i>-nya {@code null}, bukan bila
 *   <i>binding</i>-nya kosong. Rumusan yang tepat untuk katalog ini: <b>bukan "kena karena
 *   {@code pegawai}", melainkan "kena karena tidak punya properti cakupan apa pun"</b> &mdash;
 *   tenant-nya berjarak dua lompatan ({@code formatItemGaji.satuanKerja}) dan nama lompatan
 *   pertamanya bukan {@code satuanKerja}. Implikasi perbaikan: menambahkan {@code pegawai} ke
 *   whitelist <b>tidak akan menutup celah untuk entity ini</b>; yang diperlukan adalah menjadikan
 *   binding kosong sebagai penolakan (fail-closed).</li>
 *   <li><b>Permukaan yang benar-benar terbuka.</b> Layar New UI
 *   {@code /WEB-INF/new/payroll/services/item_gaji_service.jsp} mendaftarkan {@code "ItemGaji"} ke
 *   {@code GenericCrudDefinitionRegistry.tryAutoRegister(...)} &mdash; jalur auto-register ini
 *   melewati daftar-putih statis registry. Karena {@code ItemGajiAction} menyediakan
 *   {@code Window}, {@code boolean onSave(Event)} dan {@code init(ItemGaji)}, definisi yang
 *   terbentuk berstatus <b>FULL_CRUD</b> (create/update/export), ditambah soft-delete karena entity
 *   punya properti boolean {@code aktif}. Perlu dicatat: {@code ItemGajiAction.onSave} sendiri
 *   <b>tidak memuat pemeriksaan hak sama sekali</b> ({@code CommonPrivilages.checkPrevilages} hanya
 *   dipakai untuk mengatur <i>visibility</i> tombol), sehingga saat dijalankan tanpa layar oleh
 *   invoker generik, penjagaan berbasis tombol itu tidak berlaku lagi.</li>
 *   <li><b>Dua verifikasi negatif yang menenangkan.</b> (1) Dispatcher New UI bersifat
 *   <i>fail-closed</i> &mdash; sesi wajib ({@code 401 AUTH_REQUIRED}) dan
 *   {@code NewUiRouteGuard.isActionAuthorized} menolak rute yang tidak terpetakan
 *   ({@code 403 ACTION_FORBIDDEN}), jadi paparan ini bersifat <b>lintas tenant bagi pengguna yang
 *   sudah login</b>, bukan anonim; bypass {@code /anjungan?hanya_tampil_jsp=true} (17 halaman
 *   {@code task_1f9c66d3}) <b>tidak</b> menjangkau entity ini karena whitelist {@code p}/{@code s}
 *   di {@code anjungan.jsp} kini hanya mengizinkan dua service login dan akar include-nya berada di
 *   pohon JSP yang berbeda. (2) <b>{@code task_66986071} (fail-open {@code bolehAksi()} saat peran
 *   {@code null}) TIDAK berlaku</b> &mdash; tidak ada satu pun {@code *ApiHelper} maupun cabang
 *   {@code PosApi} yang menulis {@code ItemGaji}; satu-satunya sentuhan REST adalah
 *   {@code LaporanApi} yang membaca dan sudah terikat dengan benar ke format milik pegawai yang
 *   login.</li>
 * </ul>
 * <p>Kadar dampaknya berbeda dari entity payroll pembawa data pribadi: yang terpapar adalah
 * <i>struktur</i> slip gaji (nama komponen, rumus, akun jurnal), bukan nominal gaji seseorang.
 * Tetapi rumus itu sendiri kerap mengungkap kebijakan remunerasi, dan hak <i>tulis</i> atasnya
 * berarti kemampuan mengubah cara gaji dihitung dan dijurnal bagi seluruh pegawai pada format
 * tersebut &mdash; termasuk memindahkan beban gaji ke akun buku besar pilihan sendiri.</p>
 *
 * <h2>Catatan teknis lain</h2>
 * <ul>
 *   <li><b>Field yang tampak "dobel" bukan bug.</b> {@link GeneralValueObject} adalah POJO abstrak
 *   biasa &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate
 *   tidak memetakan properti induknya. Deklarasi ulang {@code id}, {@code kode}, {@code nama},
 *   {@code keterangan}, {@code nomorUrut}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah} di
 *   kelas ini adalah <b>keharusan teknis</b> agar kolomnya benar-benar terpetakan.</li>
 *   <li>{@code @Audited} (Envers) aktif: setiap versi baris digandakan ke tabel
 *   {@code payroll.item_gaji_aud}, dan layar daftar menampilkan tombol riwayat lewat
 *   {@code RevisiHelper.createNewRevisi}.</li>
 *   <li>{@code serialVersionUID} {@code 2463821577548439808L} identik dengan
 *   {@link FormatItemGaji}, {@link KelompokItemGaji} dan
 *   {@link ais.database.model.payroll.ItemGajiPegawai} &mdash; sisa salin-tempel generator, bukan
 *   penanda kompatibilitas yang bermakna.</li>
 *   <li>{@code equals}/{@code hashCode} diwarisi apa adanya dari {@link GeneralValueObject}
 *   ({@code equals} berbasis {@code id}, {@code hashCode} <b>tidak</b> di-override) &mdash; jangan
 *   memakai {@code HashSet}/{@code HashMap} berkunci {@code ItemGaji} untuk deduplikasi.</li>
 * </ul>
 *
 * @see FormatItemGaji
 * @see KelompokItemGaji
 * @see ais.database.model.payroll.ItemGajiPegawai
 * @see ais.database.model.payroll.PembayaranItemGajiPegawai
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "item_gaji")
public class ItemGaji extends GeneralValueObject {

	/**
	 * Nama variabel rumus untuk <b>jumlah keterlambatan</b> pegawai pada bulan/tahun yang dihitung.
	 * Nilainya di-<i>bind</i> ke mesin exp4j dari {@code sum(jumlahTerlambat)} atas rekap harian
	 * {@code StatuskehadiranKaryawanHarian} milik pegawai yang bersangkutan. Jangan dikacaukan
	 * dengan {@code V_TERLAMBAT} (nama berbeda, sumbernya rekap bulanan
	 * {@code KehadiranPegawaiBulanan}). Konstanta ini juga dipakai sebagai label kolom di layar
	 * absensi ({@code AbsensiPegawaiAction}, {@code AbsensiKehadiranPegawaiHarianHelper}) agar
	 * operator tahu nama variabel yang boleh ditulis di rumus.
	 */
	public static final String V_TERL = "V_TERL";
	/**
	 * Nama variabel rumus untuk <b>jumlah lembur</b>, di-<i>bind</i> dari
	 * {@code sum(jumlahLemburMasuk)} atas {@code StatuskehadiranKaryawanHarian}. Perhatikan tumpang
	 * tindih nama yang dijelaskan pada Javadoc kelas: token {@code " V_LEM "} yang diapit spasi
	 * sudah lebih dulu disubstitusi tekstual dari rekap bulanan, sehingga binding harian ini hanya
	 * kebagian pada penulisan yang tidak diapit spasi.
	 */
	public static final String V_LEM = "V_LEM";
	/**
	 * Nama variabel rumus untuk <b>jumlah pulang cepat</b>, di-<i>bind</i> dari
	 * {@code sum(jumlahCepatKeluar)} atas {@code StatuskehadiranKaryawanHarian}. Bukan sinonim
	 * {@code V_CEPAT}, yang mengambil angka dari rekap bulanan.
	 */
	public static final String V_CEP = "V_CEP";
	/**
	 * Nama variabel rumus untuk <b>total jam masuk</b>, di-<i>bind</i> dari
	 * {@code sum(jumlahJamMasuk)} atas {@code StatuskehadiranKaryawanHarian}. Dipakai untuk
	 * komponen gaji berbasis jam kerja (mis. honor per jam).
	 */
	public static final String V_JAM = "V_JAM";

	/**
	 * Penanda versi serialisasi. Nilainya identik dengan beberapa entity {@code payroll} lain
	 * (sisa salin-tempel generator), jadi jangan dijadikan penanda kompatibilitas yang bermakna.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code payroll.item_gaji.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini (bagian jejak audit ringan yang
	 * diisi {@code AuditTimestampInterceptor}).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. <b>Bersifat "hanya-isi"</b>: nilai {@code null} atau
	 * string kosong/spasi diabaikan diam-diam sehingga jejak lama tidak terhapus oleh proses yang
	 * kebetulan tidak membawa konteks pengguna (mis. job latar atau impor). Perilaku ini seragam di
	 * seluruh entity AIS.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks {@code "id-kode - nama"}. Format ini dipakai apa adanya sebagai label di
	 * bandbox pemilih ({@code AmbilDataItemGajiBanbox}) dan pada judul node pohon, jadi mengubahnya
	 * ikut mengubah tampilan beberapa layar.
	 *
	 * @return gabungan id, kode dan nama komponen gaji
	 */
	public String toString() {
		return id + "-" + kode + " - " + nama;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/{@code null} diabaikan agar jejak sebelumnya tidak tertimpa.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan stempel waktu/pengguna ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris di-{@code UPDATE}. Method ini
	 * memenuhi satu-satunya kontrak {@code abstract} yang diwajibkan
	 * {@link GeneralValueObject}; jangan dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat, lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Normalnya tidak dipanggil dari kode aplikasi —
	 * pengisian dilakukan otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object baru karena
	 *         diinisialisasi ke waktu server saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama komponen gaji sebagaimana tercetak di slip; lihat {@link #getNama()}. */
	private String nama;
	/** Format/skema slip pemilik baris ini — satu-satunya jalur ke satuan kerja; lihat {@link #getFormatItemGaji()}. */
	private FormatItemGaji formatItemGaji;
	/** Kelompok akun jurnal; sumber sebenarnya akun debet/kredit, lihat {@link #getKelompokItemGaji()}. */
	private KelompokItemGaji kelompokItemGaji;
	/** Induk hierarki (komponen ini menjadi bagian dari induknya); lihat {@link #getParent()}. */
	private ItemGaji parent;
	/** Urutan tampil di antara saudara sekandung; lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Bendera aktif; baris tidak aktif disembunyikan dari pohon dan bandbox. Lihat {@link #getAktif()}. */
	private Boolean aktif = true;
	/** Bila {@code true}, hasil rumus negatif dipaksa menjadi 0; lihat {@link #getJadikan0JikaMinus()}. */
	private Boolean jadikan0JikaMinus;
	/** Menandai komponen "nilai final"/take-home; lihat {@link #getFinalGaji()}. */
	private Boolean finalGaji;
	/** Bila {@code false}, komponen dihitung tetapi tidak dicetak di slip; lihat {@link #getTampilkanDiSlip()}. */
	private Boolean tampilkanDiSlip = true;
	/** Kode unik komponen — dipakai sebagai nama variabel di rumus DAN sebagai kunci pencocokan kelompok. Lihat {@link #getKode()}. */
	private String kode;
	/** Rumus perhitungan bawaan (ekspresi exp4j berbasis token bersspasi); lihat {@link #getDefaultFormula()}. */
	private String defaultFormula;
	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Cache kedalaman hierarki untuk indentasi laporan; lihat {@link #getDeep()}. */
	private Integer deep;
	/** Counter popularitas untuk pengurutan bandbox "sering dipakai"; lihat {@link #getJmlDipakai()}. */
	private Long jmlDipakai = 0L;

	/** Akun sisi KREDIT jurnal gaji; lihat {@link #getAkun()}. */
	private Akun akun;
	/** Akun sisi DEBET jurnal gaji; lihat {@link #getAkunDebet()}. */
	private Akun akunDebet;

	/** Bila {@code true}, baris hanya berfungsi sebagai pemisah/spasi visual dan dilewati perhitungan; lihat {@link #getSpace()}. */
	private Boolean space = false;
	/** Bila {@code true}, operator boleh mengetik nilai manual menimpa rumus; lihat {@link #getNilaiVariableBisaDiubah()}. */
	private Boolean nilaiVariableBisaDiubah;

	/** Konstruktor kosong yang diwajibkan Hibernate/JPA. */
	public ItemGaji() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Hanya untuk kebutuhan Hibernate/penyalinan; jangan dipakai untuk
	 * "memindahkan" data ke baris lain.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama komponen gaji sebagaimana tercetak di slip (mis. "Tunjangan Jabatan").
	 * Kolom {@code nama} bersifat {@code NOT NULL} dan divalidasi wajib-isi di
	 * {@code ItemGajiAction.onSave}.
	 *
	 * @return nama komponen gaji
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Mengisi nama komponen gaji.
	 *
	 * @param nama nama komponen; wajib terisi di lapisan UI
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas komponen gaji (dasar hukum tunjangan, catatan kebijakan, dsb.).
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas komponen gaji.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link FormatItemGaji} pemilik komponen ini, dengan resolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Penting:</b> relasi inilah satu-satunya jalur entity ini menuju tenant —
	 * {@code formatItemGaji.getSatuanKerja()} dipakai {@link #ambilAkun()}/{@link #ambilAkunDebet()}
	 * untuk memilih akun jurnal yang berlaku bagi satuan kerja tersebut. Kolom FK
	 * {@code format_item_gaji} bersifat {@code NOT NULL} dan diwajibkan di layar simpan, tetapi
	 * getter ini tetap dapat mengembalikan {@code null} untuk object baru yang belum diisi.</p>
	 *
	 * @return format/skema slip pemilik, atau {@code null} pada object yang belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji", nullable = false)
	public FormatItemGaji getFormatItemGaji() {
		formatItemGaji = check(formatItemGaji);
		return formatItemGaji;
	}

	/**
	 * Mengisi format/skema slip pemilik komponen ini. Memindahkan komponen ke format lain juga
	 * memindahkan satuan kerja efektifnya, sehingga akun jurnal hasil resolusi
	 * {@link #ambilAkun()} bisa ikut berubah.
	 *
	 * @param formatItemGaji format pemilik
	 */
	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}

	/**
	 * Mengembalikan komponen induk dalam hierarki slip (kolom {@code bagian_dari}), dengan resolusi
	 * proxy lazy. Hierarki dipakai {@code ItemGajiTreeModel} untuk merender pohon komponen dan
	 * membentuk sub-total; komponen tanpa induk adalah node akar.
	 *
	 * <p><b>Hierarki ini murni untuk tampilan dan urutan, bukan untuk perhitungan.</b> Bahasa rumus
	 * tidak mengenal notasi "jumlahkan anak-anak saya" — sub-total tetap harus ditulis manual dengan
	 * menyebut {@code kode} tiap anaknya. Indentasi pada slip pun dihitung ulang saat render dengan
	 * menaiki {@link #getParent()}, bukan dari cache {@link #getDeep()}.</p>
	 *
	 * <p><b>Tidak ada penjaga siklus</b> pada penelusuran relasi ini:
	 * {@code ItemGajiTreeModel.getParentCount()} dan {@code getParentSet()} menaiki rantai induk
	 * secara rekursif tanpa memeriksa node yang sudah dikunjungi, sehingga rantai induk melingkar
	 * berujung rekursi tak berhingga. (Bandingkan dengan rekursi <i>rumus</i> yang dijaga penghitung
	 * ambang 25 — lihat Javadoc kelas.)</p>
	 *
	 * @return komponen induk, atau {@code null} bila komponen ini node akar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian_dari", nullable = true)
	public ItemGaji getParent() {
		parent = check(parent);
		return parent;
	}

	/**
	 * Mengisi komponen induk. Tidak ada validasi apa pun (baik di sini maupun di
	 * {@code ItemGajiAction.onSave}) yang mencegah pembentukan rantai melingkar atau pemilihan induk
	 * dari format slip yang berbeda.
	 *
	 * @param parent komponen induk, atau {@code null} untuk menjadikannya node akar
	 */
	public void setParent(ItemGaji parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan nomor urut tampil di antara komponen sekandung, <b>dengan substitusi</b>: nilai
	 * {@code null} dilaporkan sebagai {@code 0}, bukan {@code null}. Substitusi ini hanya berlaku
	 * pada nilai kembalian dan tidak menulis balik ke field, sehingga tidak bersifat destruktif —
	 * berbeda dari {@link #getAktif()}, {@link #getTampilkanDiSlip()} dan {@link #getSpace()} yang
	 * memang menugaskan ulang field-nya.
	 *
	 * <p><b>Peran tersembunyi yang penting:</b> selain menentukan urutan tampil, nomor urut dipakai
	 * sebagai <i>konvensi implisit</i> untuk menemukan "baris total gaji".
	 * {@code GajiPegawaiAction} mengambil komponen akar ({@code parent} {@code null}) dengan
	 * {@code nomorUrut} <b>terbesar</b> ({@code order by nomor_urut desc}, {@code maxResults(1)})
	 * sebagai angka total. Konvensi ini berdiri sendiri dan tidak selalu sejalan dengan
	 * {@link #getFinalGaji()} — menambahkan satu komponen akar baru ber-{@code nomorUrut} lebih
	 * besar (mis. catatan/keterangan di paling bawah slip) dapat memindahkan angka yang diambil
	 * sebagai total tanpa ada yang mengubah bendera apa pun.</p>
	 *
	 * @return nomor urut, atau {@code 0} bila kolomnya kosong
	 */
	@Column(name = "urutan")
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut tampil. Diwajibkan terisi oleh {@code ItemGajiAction.onSave}; nilai kembar
	 * antar-saudara tidak dicegah dan menghasilkan urutan yang bergantung pada tie-break basis data.
	 *
	 * @param nomorUrut nomor urut tampil
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan status aktif komponen. Baris tidak aktif disaring keluar dari pohon Item Gaji
	 * dan dari bandbox pemilih, tetapi <b>tidak</b> otomatis dicabut dari baris rencana/pembayaran
	 * yang sudah terlanjur dibuat.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, getter ini menugaskan {@code true} ke
	 * field {@link #aktif} sebelum mengembalikannya. Karena entity dipetakan property-access dengan
	 * {@code dynamicUpdate}, nilai bawaan itu ikut tertulis ke basis data pada {@code flush}
	 * berikutnya — baris legacy ber-{@code aktif} {@code NULL} akan "menjadi aktif" secara permanen
	 * hanya dengan dibaca. Arahnya membuka (bukan menutup), jadi perlakukan sebagai fail-open.</p>
	 *
	 * @return {@code true} bila komponen aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengisi status aktif komponen.
	 *
	 * @param aktif {@code true} untuk mengaktifkan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode komponen gaji. Kode ini memikul <b>tiga peran sekaligus</b>, dan itulah
	 * sumber sebagian besar kejutan pada entity ini:
	 * <ol>
	 *   <li><b>Nama variabel di rumus.</b> Komponen lain merujuknya langsung, mis. rumus
	 *   {@code GP * 0.05} berarti "5% dari komponen berkode {@code GP}". Pencarian dilakukan
	 *   terhadap {@code ItemGajiPegawai} dengan {@code kode} sama pada {@code pegawai} dan
	 *   {@code formatItemGaji} yang sama.</li>
	 *   <li><b>Kunci pencocokan kelompok akun.</b> {@link #getKelompokItemGaji()} mencocokkan kode
	 *   ini (case-insensitive, setelah {@code trim}) dengan {@code kode} milik
	 *   {@link KelompokItemGaji} aktif — dan bila cocok, <b>menimpa</b> FK kelompok yang tersimpan.
	 *   Jadi memberi sebuah komponen kode yang kebetulan sama dengan kode kelompok akan mengubah
	 *   akun jurnalnya.</li>
	 *   <li><b>Label identitas</b> di daftar dan di {@link #toString()}.</li>
	 * </ol>
	 * <p>Kode wajib diisi ({@code ItemGajiAction.onSave} menolak yang kosong) tetapi
	 * <b>keunikannya tidak ditegakkan</b> — tidak ada indeks unik maupun pemeriksaan duplikat, baik
	 * di dalam satu format maupun lintas format. Dua komponen berkode sama pada satu pegawai dan
	 * satu format diselesaikan mesin rumus dengan {@code order by id desc} + {@code maxResults(1)},
	 * yakni "yang terbaru menang".</p>
	 *
	 * @return kode komponen gaji
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode komponen gaji. Perhatikan ketiga peran kode yang dijelaskan di
	 * {@link #getKode()} — mengganti kode sebuah komponen dapat sekaligus memutus rujukan rumus
	 * komponen lain dan memindahkan pengelompokan akun jurnalnya, tanpa peringatan apa pun.
	 *
	 * @param kode kode komponen gaji
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan rumus perhitungan bawaan komponen ini, <b>dinormalkan</b>: {@code null}
	 * dilaporkan sebagai string kosong dan hasilnya selalu di-{@code trim}. Normalisasi ini murni
	 * pada nilai kembalian (tidak menulis balik ke field), sehingga pemanggil dapat langsung
	 * memanggil {@code isEmpty()}/{@code split(" ")} tanpa penjagaan {@code null}.
	 *
	 * <p>Sintaks dan cara evaluasi dijelaskan lengkap pada Javadoc kelas. Ringkasnya: ekspresi
	 * exp4j dengan token dipisah spasi; token dapat berupa angka, variabel absensi/masa kerja yang
	 * disubstitusi tekstual, atau {@code kode} komponen lain yang dievaluasi secara rekursif.
	 * <b>Variabel wajib diapit spasi</b> agar tersubstitusi, dan rekursi antar-kode tidak punya
	 * penjaga siklus.</p>
	 *
	 * <p>Nilai di sini hanyalah <i>default</i>: {@code ItemGajiPegawai} yang tidak menyalakan
	 * {@code ikutiItemGaji} memakai rumusnya sendiri, dan pada pembayaran rumus dapat ditimpa lagi
	 * bila {@link #getNilaiVariableBisaDiubah()} bernilai {@code true}.</p>
	 *
	 * @return rumus bawaan yang sudah di-{@code trim}; string kosong bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	public String getDefaultFormula() {
		return defaultFormula == null ? "" : defaultFormula.trim();
	}

	/**
	 * Mengisi rumus perhitungan bawaan. Tidak ada validasi sintaks, validasi nama variabel, maupun
	 * deteksi rujukan melingkar pada jalur simpan mana pun — kesalahan penulisan baru muncul sebagai
	 * pesan kesalahan saat penghitungan slip dijalankan.
	 *
	 * @param defaultFormula ekspresi rumus, boleh {@code null} atau kosong untuk komponen yang
	 *        nilainya selalu diisi manual
	 */
	public void setDefaultFormula(String defaultFormula) {
		this.defaultFormula = defaultFormula;
	}

	/**
	 * Mengembalikan {@link Akun} untuk <b>sisi KREDIT</b> jurnal gaji komponen ini (umumnya Utang
	 * Gaji, Kas/Bank, atau akun kewajiban potongan seperti Utang BPJS).
	 *
	 * <p><b>Tidak lagi menulis-balik (diperbaiki).</b> Getter ini dulu menugaskan hasil
	 * {@link #ambilAkun()} ke field {@link #akun} sebelum mengembalikannya — karena entity ini
	 * dipetakan property-access dengan {@code dynamicUpdate = true}, penugasan itu ikut
	 * ter-{@code UPDATE} ke basis data pada {@code flush} berikutnya, menimpa akun yang dipilih
	 * operator di form Item Gaji hanya karena barisnya dibaca (renderer grid, pohon, atau bandbox
	 * pemilih). Getter ini sekarang murni: field {@link #akun} hanya diresolusi proxy-nya lewat
	 * {@link GeneralValueObject#check(Object)}, dan resolusi {@link #ambilAkun()} hanya dipakai
	 * untuk NILAI KEMBALIAN — tidak pernah ditulis ke field maupun dipersistenkan.</p>
	 *
	 * <p>Bila kelompok tidak punya pemetaan yang resolvable ({@link KelompokItemGaji#getAkun()}
	 * mengembalikan array JSON kosong), resolusi menghasilkan {@code null} dan FK per-item
	 * tersimpan itulah yang dikembalikan apa adanya.</p>
	 *
	 * <p><b>Dipanggil dari:</b> mesin posting penggajian lewat
	 * {@code PembayaranItemGajiPegawai.getAkun()} sebagai fallback ketika baris slip belum punya
	 * akun sendiri, sehingga nilai yang dikembalikan di sini benar-benar menentukan akun buku besar
	 * yang dijurnal — hasil resolusi kelompok tetap dipakai LIVE untuk keperluan itu, hanya saja
	 * kini tanpa efek samping menulis ke basis data.</p>
	 *
	 * @return akun sisi kredit hasil resolusi kelompok, atau FK tersimpan bila tidak ada pemetaan
	 *         kelompok yang cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);

		Akun a = ambilAkun();
		return a != null && a.getId() != null ? a : akun;
	}

	/**
	 * Mengisi akun sisi kredit secara langsung. Nilai FK tersimpan ini tetap dipertahankan oleh
	 * {@link #getAkun()} kecuali {@link #getKelompokItemGaji()} komponen ini punya pemetaan akun
	 * yang resolvable, dalam hal mana nilai hasil resolusi itulah yang dikembalikan (tanpa menimpa
	 * field ini — lihat Javadoc {@link #getAkun()}).
	 *
	 * @param akun akun buku besar sisi kredit, boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan kedalaman komponen ini dalam hierarki slip. Nilainya <b>cache yang
	 * dimaterialisasi</b>, bukan hasil hitung saat dibaca.
	 *
	 * <p><b>Praktisnya nyaris tidak terpakai.</b> Indentasi slip dan pohon dihitung ulang saat
	 * render dengan menaiki {@link #getParent()}; tidak ada satu pun logika bisnis yang membaca
	 * kolom ini. Yang ada hanyalah penulisnya.</p>
	 *
	 * <p><b>Kuirk terverifikasi:</b> pengisi cache ini adalah
	 * {@code ItemGajiTreeModel.getParentCount(ItemGaji, List)} yang dipanggil dari
	 * {@code LaporanItemGaji}. Versi satu-argumen itu menaiki rantai induk secara rekursif lalu
	 * menulis {@code setDeep(longs.size())} pada node <b>terakhir</b> yang dikunjungi — yakni node
	 * <i>akar</i>, bukan node yang sedang diukur. Akibatnya mencetak Laporan Item Gaji menuliskan
	 * kedalaman keturunan ke baris akarnya. Padanan yang sudah diperbaiki ada di
	 * {@code PembayaranItemGajiPegawaiTreeModel}/{@code RencanaItemGajiPegawaiTreeModel} yang
	 * memakai tanda tangan dua-argumen ({@code objToCount}, {@code objToUpdate}); dua tree model
	 * lapis katalog ({@code ItemGajiTreeModel}, {@code ItemGajiPegawaiTreeModel}) belum ikut
	 * diperbaiki. Dampaknya terbatas pada indentasi cetakan, bukan pada nominal gaji.</p>
	 *
	 * @return kedalaman hierarki hasil kalkulasi terakhir, atau {@code null} bila belum pernah
	 *         dihitung
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Mengisi cache kedalaman hierarki. Hanya dipanggil oleh tree model saat merender/mencetak;
	 * bukan data yang diisi operator.
	 *
	 * @param deep kedalaman hierarki
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan counter berapa kali komponen ini pernah dipilih lewat bandbox. Dipakai
	 * {@code AmbilDataItemGajiBanbox} untuk mengurutkan daftar pilihan ({@code order by jml_dipakai
	 * desc}) sehingga item yang sering dipakai muncul di atas — murni kenyamanan UI, tidak punya
	 * arti bisnis dan tidak mencerminkan berapa banyak pegawai yang benar-benar memakai komponen ini.
	 *
	 * @return jumlah pemilihan kumulatif; {@code 0} pada object baru
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Mengisi counter pemakaian. Dipanggil {@code AmbilDataItemGajiBanbox} dengan nilai
	 * "hitungan-dari-basis-data + 1", lalu segera dipersistenkan lewat {@code Common.refreshUpdate}.
	 * <b>Pemanggilan inilah pemicu paling sering</b> yang membuat penulisan-balik pada
	 * {@link #getAkun()}, {@link #getAkunDebet()} dan {@link #getKelompokItemGaji()} benar-benar
	 * tersimpan ke basis data.
	 *
	 * @param jmlDipakai nilai counter baru
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Menurunkan {@link Akun} sisi <b>kredit</b> dari pemetaan kelompok, tanpa menyentuh FK
	 * per-item. Alurnya: resolusi {@link #formatItemGaji} lalu {@link #getKelompokItemGaji()},
	 * kemudian menyerahkan JSON pemetaan {@link KelompokItemGaji#getAkun()} bersama
	 * {@code formatItemGaji.getSatuanKerja()} ke
	 * {@code AssetUtil.ambilDataAkun(String, SatuanKerja)}. Utilitas itu memilih entri yang
	 * {@code satuanKerja}-nya cocok persis, dan jatuh ke entri default (ber-{@code satuanKerja}
	 * {@code null}) bila tidak ada yang cocok.
	 *
	 * <p><b>Efek samping:</b> menugaskan ulang field {@link #formatItemGaji} (resolusi proxy lazy
	 * non-destruktif). Kelompok hasil {@link #getKelompokItemGaji()} disimpan ke variabel lokal,
	 * <b>bukan</b> ke field {@link #kelompokItemGaji} — perbaikan atas versi lama yang dulu menulis
	 * field itu di sini juga, menduplikasi write-back yang sudah dihapus dari getter itu sendiri.</p>
	 *
	 * <p><b>Penanganan galat:</b> seluruh kegagalan resolusi — JSON pemetaan rusak, akun yang
	 * dirujuk sudah dihapus, kegagalan sesi Hibernate — ditelan menjadi {@code null} dan hanya
	 * dicatat ke {@code ErrorAuditUtil}. Bagi pemanggil, "pemetaan tidak ada" dan "pemetaan ada
	 * tetapi gagal dibaca" tidak dapat dibedakan; keduanya berakhir memakai FK per-item apa adanya.
	 * Untuk penjurnalan gaji, ini berarti sebuah galat konfigurasi bisa lolos diam-diam menjadi
	 * jurnal ke akun yang lain.</p>
	 *
	 * @return akun kredit hasil pemetaan kelompok, atau {@code null} bila format/kelompok belum
	 *         terisi, pemetaan kosong, atau resolusi gagal
	 */
	public Akun ambilAkun() {
		Akun a = null;
		formatItemGaji = check(formatItemGaji);
		KelompokItemGaji kelompok = getKelompokItemGaji();
		if (formatItemGaji != null && kelompok != null) {
			try {
				a = AssetUtil.ambilDataAkun(kelompok.getAkun(), formatItemGaji.getSatuanKerja());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/ItemGaji.java:240");
//				e.printStackTrace();
			}
		}
		return a;
	}

	/**
	 * Kembaran {@link #ambilAkun()} untuk sisi <b>debet</b>: identik seluruhnya kecuali membaca
	 * {@link KelompokItemGaji#getAkunDebet()} alih-alih {@code getAkun()}. Seluruh catatan efek
	 * samping dan penanganan galat pada {@link #ambilAkun()} berlaku sama persis di sini.
	 *
	 * @return akun debet hasil pemetaan kelompok, atau {@code null} bila format/kelompok belum
	 *         terisi, pemetaan kosong, atau resolusi gagal
	 */
	public Akun ambilAkunDebet() {
		Akun a = null;
		formatItemGaji = check(formatItemGaji);
		KelompokItemGaji kelompok = getKelompokItemGaji();
		if (formatItemGaji != null && kelompok != null) {
			try {
				a = AssetUtil.ambilDataAkun(kelompok.getAkunDebet(), formatItemGaji.getSatuanKerja());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/ItemGaji.java:254");
//				e.printStackTrace();
			}
		}
		return a;
	}

	/**
	 * Mengembalikan {@link Akun} untuk <b>sisi DEBET</b> jurnal gaji komponen ini (umumnya akun
	 * Beban Gaji/Beban Tunjangan). Perilakunya cerminan persis {@link #getAkun()}, termasuk
	 * perbaikan yang sama: getter ini <b>tidak lagi menulis-balik</b> hasil {@link #ambilAkunDebet()}
	 * ke field {@link #akunDebet} — resolusi hanya dipakai untuk nilai kembalian.
	 *
	 * <p><b>Dipanggil dari:</b> {@code PembayaranItemGajiPegawai.getAkunDebet()} sebagai fallback
	 * ketika baris slip belum punya akun debet sendiri; nilainya dikumpulkan
	 * {@code PostingTransaksiPembayaranGajiAction}/{@code PostingTransaksiPenggajianAction} ke
	 * daftar akun sisi debet sebelum jurnal dibentuk.</p>
	 *
	 * @return akun sisi debet hasil resolusi kelompok, atau FK tersimpan bila tidak ada pemetaan
	 *         kelompok yang cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_debet", nullable = true)
	public Akun getAkunDebet() {
		akunDebet = check(akunDebet);
		Akun a = ambilAkunDebet();
		return a != null && a.getId() != null ? a : akunDebet;
	}

	/**
	 * Mengisi akun sisi debet secara langsung. Nilai FK tersimpan ini tetap dipertahankan oleh
	 * {@link #getAkunDebet()} kecuali kelompok komponen punya pemetaan akun debet yang resolvable,
	 * dalam hal mana nilai hasil resolusi itulah yang dikembalikan (tanpa menimpa field ini).
	 *
	 * @param akunDebet akun buku besar sisi debet, boleh {@code null}
	 */
	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

	/**
	 * Menyatakan apakah komponen ini dicetak pada slip gaji pegawai. Komponen dengan nilai
	 * {@code false} tetap ikut dihitung dan tetap dijurnal — ia hanya disembunyikan dari cetakan
	 * slip, misalnya untuk komponen antara/pembantu perhitungan yang tidak perlu dilihat pegawai.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, getter menugaskan {@code true} ke field
	 * dan nilai bawaan itu ikut tersimpan pada {@code flush} berikutnya (pola write-back yang sama
	 * dengan {@link #getAktif()}).</p>
	 *
	 * @return {@code true} bila komponen ditampilkan di slip; tidak pernah {@code null}
	 */
	public Boolean getTampilkanDiSlip() {
		if (tampilkanDiSlip == null) {
			tampilkanDiSlip = true;
		}
		return tampilkanDiSlip;
	}

	/**
	 * Mengatur apakah komponen ini dicetak di slip gaji.
	 *
	 * @param tampilkanDiSlip {@code true} untuk menampilkan
	 */
	public void setTampilkanDiSlip(Boolean tampilkanDiSlip) {
		this.tampilkanDiSlip = tampilkanDiSlip;
	}

	/**
	 * Menyatakan apakah baris ini sekadar <b>pemisah/spasi visual</b> pada slip, bukan komponen
	 * gaji sungguhan. Baris ber-{@code space} {@code true} dilewati saat penghitungan: ketiga tree
	 * model penggajian ({@code ItemGajiPegawaiTreeModel}, {@code RencanaItemGajiPegawaiTreeModel},
	 * {@code PembayaranItemGajiPegawaiTreeModel}) membungkus pemanggilan rumus di dalam
	 * {@code if (!gajiPegawai.getSpace())}. Bendera ini juga menurun ke lapis di bawahnya:
	 * {@code ItemGajiPegawai.getSpace()} mengambil nilai dari katalog ini bila kolomnya sendiri
	 * masih {@code null}.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, getter menugaskan {@code false} ke field
	 * dan nilai bawaan itu ikut tersimpan pada {@code flush} berikutnya.</p>
	 *
	 * @return {@code true} bila baris hanya pemisah visual; tidak pernah {@code null}
	 */
	public Boolean getSpace() {
		if (space == null) {
			space = false;
		}
		return space;
	}

	/**
	 * Menandai baris ini sebagai pemisah/spasi visual atau bukan.
	 *
	 * @param space {@code true} untuk menjadikannya baris pemisah
	 */
	public void setSpace(Boolean space) {
		this.space = space;
	}

	/**
	 * Menyatakan apakah operator boleh <b>mengetik nilai manual</b> yang menimpa hasil rumus pada
	 * layar pembayaran gaji. Bila {@code true} <i>dan</i> {@link #getDefaultFormula()} tidak kosong,
	 * {@code BayarGajiPegawaiAction}/{@code ItemGajiPegawaiAction} merender kotak isian yang bisa
	 * disunting alih-alih label statis; bila {@code false}, nilai komponen sepenuhnya ditentukan
	 * rumus. Bendera ini menurun ke {@code ItemGajiPegawai} selama {@code ikutiItemGaji} menyala.
	 *
	 * <p><b>Cara kerjanya menjelaskan mengapa spasi di rumus begitu penting:</b> layar pembayaran
	 * memecah {@link #getDefaultFormula()} dengan {@code split(" ")} lalu merender <b>satu kotak
	 * isian per token</b>. Nilai yang disunting dikumpulkan ke peta {@code formulasBaru} berkunci
	 * id item, lalu rumus disusun ulang dari token-token itu dan dievaluasi ulang. Menulis rumus
	 * tanpa spasi berarti seluruh rumus menjadi satu token tunggal yang tidak dapat disunting
	 * sebagian.</p>
	 *
	 * <p>Substitusi {@code null} &rarr; {@code false} di sini bersifat <b>tidak destruktif</b>
	 * (hanya nilai kembalian yang diganti, field tidak ditugasi ulang) dan arahnya menutup
	 * (fail-closed): kolom kosong berarti "tidak boleh diubah manual".</p>
	 *
	 * @return {@code true} bila nilai boleh ditimpa manual; tidak pernah {@code null}
	 */
	public Boolean getNilaiVariableBisaDiubah() {
		return nilaiVariableBisaDiubah == null ? false : nilaiVariableBisaDiubah;
	}

	/**
	 * Mengatur izin penimpaan nilai secara manual pada layar pembayaran.
	 *
	 * @param nilaiVariableBisaDiubah {@code true} untuk mengizinkan
	 */
	public void setNilaiVariableBisaDiubah(Boolean nilaiVariableBisaDiubah) {
		this.nilaiVariableBisaDiubah = nilaiVariableBisaDiubah;
	}

	/**
	 * Menyatakan apakah hasil rumus yang <b>negatif dipaksa menjadi 0</b>. Berguna untuk komponen
	 * potongan bersyarat yang rumusnya bisa menghasilkan angka minus (mis. sisa cicilan yang sudah
	 * lunas) agar tidak berbalik menjadi penambah gaji.
	 *
	 * <p><b>Dibaca secara live dari katalog, bukan dari snapshot.</b> Seluruh titik penghitungan
	 * memanggilnya lewat rantai {@code itemGajiPegawai.getItemGaji().getJadikan0JikaMinus()} —
	 * terverifikasi di {@code BayarGajiPegawaiAction}, {@code GajiPegawaiAction},
	 * {@code ItemGajiPegawaiAction}, ketiga tree model penggajian, serta di dalam
	 * {@code RencanaItemGajiPegawai}/{@code PembayaranItemGajiPegawai} sendiri. Artinya mengubah
	 * bendera ini di katalog langsung mengubah cara baris rencana/pembayaran yang sudah ada dihitung
	 * ulang saat layar dibuka kembali; berbeda dari {@link #getSpace()} dan {@link #getFinalGaji()}
	 * yang <i>diturunkan sekali</i> ke lapis {@code ItemGajiPegawai} lalu dibaca dari sana.</p>
	 *
	 * <p>Substitusi {@code null} &rarr; {@code false} bersifat tidak destruktif.</p>
	 *
	 * @return {@code true} bila hasil negatif dinolkan; tidak pernah {@code null}
	 */
	public Boolean getJadikan0JikaMinus() {
		return jadikan0JikaMinus == null ? false : jadikan0JikaMinus;
	}

	/**
	 * Mengatur pemaksaan hasil negatif menjadi 0.
	 *
	 * @param jadikan0JikaMinus {@code true} untuk menolkan hasil negatif
	 */
	public void setJadikan0JikaMinus(Boolean jadikan0JikaMinus) {
		this.jadikan0JikaMinus = jadikan0JikaMinus;
	}

	/**
	 * Menandai komponen ini sebagai <b>angka gaji final</b> (take-home pay) pada slip. Komponen
	 * bertanda ini diperlakukan khusus: saat pohon pembayaran dirender,
	 * {@code BayarGajiPegawaiAction} menyalin hasil hitungnya ke kolom
	 * {@code PembayaranGajiPunyaPegawai.nilaiFinal} dan <b>langsung meng-commit</b> penyimpanannya
	 * dalam transaksi tersendiri — jadi sekadar membuka layar pembayaran dapat memperbarui nilai
	 * final yang tersimpan. Pembandingnya memakai {@code hasil.intValue() !=
	 * nilaiFinal.intValue()} dengan ambang {@code hasil > 0.1}, sehingga selisih pecahan di bawah
	 * satu rupiah tidak memicu penulisan ulang.
	 *
	 * <p>Bila lebih dari satu komponen dalam satu format ditandai final, yang tersimpan adalah
	 * komponen final terakhir yang dirender — tidak ada penjaga "hanya boleh satu".</p>
	 *
	 * <p>Bendera ini menurun ke {@code ItemGajiPegawai.getFinalGaji()} bila kolom di sana masih
	 * {@code null}. Substitusi {@code null} &rarr; {@code false} bersifat tidak destruktif.</p>
	 *
	 * @return {@code true} bila komponen ini menyatakan gaji final; tidak pernah {@code null}
	 */
	public Boolean getFinalGaji() {
		return finalGaji == null ? false : finalGaji;
	}

	/**
	 * Menandai/melepas status "gaji final" komponen ini.
	 *
	 * @param finalGaji {@code true} bila komponen ini adalah angka gaji final
	 */
	public void setFinalGaji(Boolean finalGaji) {
		this.finalGaji = finalGaji;
	}

	/**
	 * Cache <b>statis se-JVM</b> berisi seluruh {@link KelompokItemGaji} yang aktif, dipakai
	 * {@link #getKelompokItemGaji()} untuk mencocokkan kelompok berdasarkan {@code kode} tanpa
	 * menyentuh basis data pada setiap pembacaan.
	 *
	 * <p><b>Hal yang perlu diwaspadai:</b></p>
	 * <ul>
	 *   <li>Bersifat {@code public} dan <b>mutable</b> — kode mana pun bisa menggantinya, dan
	 *   isinya dibagi seluruh pengguna serta seluruh tenant dalam satu JVM. Pemuatannya tidak
	 *   menyaring tenant (wajar, sebab {@link KelompokItemGaji} memang tidak punya kolom tenant).</li>
	 *   <li>Dimuat lewat {@link #reloadKelompokItemGaji()} pada saat startup
	 *   ({@code InitData.reloadDefaults}) dan setiap kali kelompok disimpan/dihapus dari
	 *   {@code KelompokItemGajiAction}. Perubahan kelompok yang dilakukan lewat jalur lain (SQL
	 *   langsung, impor, atau CRUD generik) <b>tidak</b> menyegarkan cache ini sampai instans
	 *   di-restart.</li>
	 *   <li>Tidak ada sinkronisasi: penggantian daftar oleh satu thread dapat terbaca separuh oleh
	 *   thread lain. Dalam praktik dampaknya terbatas karena daftar diganti utuh (bukan disunting
	 *   di tempat), tetapi iterasi di {@link #getKelompokItemGaji()} tetap dibungkus
	 *   {@code try/catch} per elemen.</li>
	 * </ul>
	 */
	public static List<KelompokItemGaji> kelompokItemGajis = new ArrayList<KelompokItemGaji>();

	/**
	 * Memuat ulang cache {@link #kelompokItemGajis} dari basis data: seluruh
	 * {@link KelompokItemGaji} dengan {@code aktif = true}, tanpa penyaring tenant dan tanpa batas
	 * jumlah baris.
	 *
	 * <p><b>Kapan dipanggil:</b> (1) saat startup aplikasi dari
	 * {@code InitData.reloadDefaults()}; (2) dari {@code KelompokItemGajiAction} setiap kali sebuah
	 * kelompok disimpan atau dihapus, supaya perubahan pemetaan akun langsung berlaku.</p>
	 *
	 * <p><b>Efek samping pada sesi Hibernate:</b> method ini membuka
	 * {@code HibernateUtil.currentNativeSession()}, lalu menutupnya
	 * ({@code disconnect()} + {@code close()}) dan memanggil {@code HibernateUtil.closeSession()}
	 * di luar blok {@code try}. Pemanggilan dari dalam event ZK karena itu ikut menutup sesi
	 * request yang sedang berjalan — pola yang sama sudah tercatat menimbulkan gangguan pada
	 * beberapa layar master lain (lihat catatan {@code reloadDefault()} pada
	 * {@code JenisKasBesar}/{@code JenisKasKecil}). Pemanggil di dalam alur ZK sebaiknya
	 * memperlakukan sesi lamanya sebagai sudah tidak sah setelah memanggil method ini.</p>
	 *
	 * <p><b>Penanganan galat:</b> seluruh kegagalan ditelan (dicatat ke {@code ErrorAuditUtil}).
	 * Bila pemuatan gagal, {@link #kelompokItemGajis} tetap memegang isi lamanya — atau tetap kosong
	 * bila kegagalan terjadi saat startup — dan {@link #getKelompokItemGaji()} akan diam-diam
	 * berhenti mencocokkan kelompok berdasarkan kode, sehingga akun jurnal jatuh ke FK per-item.
	 * Kegagalan ini tidak terlihat dari UI mana pun.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void reloadKelompokItemGaji() {
		try {
			Session session = HibernateUtil.currentNativeSession();
			kelompokItemGajis = ConstantValues.simpleList(
					session.createCriteria(KelompokItemGaji.class).add(Restrictions.eq("aktif", true)),
					KelompokItemGaji.class);
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/ItemGaji.java:333");
			// TODO: handle exception
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Mengembalikan {@link KelompokItemGaji} komponen ini — sumber sebenarnya akun jurnal debet dan
	 * kredit lewat {@link #ambilAkun()}/{@link #ambilAkunDebet()}.
	 *
	 * <p><b>Tidak lagi menulis-balik (diperbaiki).</b> Getter ini dulu menugaskan ulang field
	 * {@link #kelompokItemGaji} begitu menemukan kecocokan kode, dan karena kolom
	 * {@code kelompok_item_gaji} dipetakan property-access dengan {@code dynamicUpdate}, penugasan
	 * itu ikut ter-{@code UPDATE} ke basis data pada {@code flush} berikutnya — FK yang dipilih
	 * operator hilang tertimpa hanya karena barisnya dibaca. Getter ini sekarang mengembalikan hasil
	 * pencocokan kode sebagai NILAI KEMBALIAN saja; field {@link #kelompokItemGaji} tidak pernah
	 * ditugasi ulang di sini (hanya diresolusi proxy-nya lewat
	 * {@link GeneralValueObject#check(Object)}, yang non-destruktif).</p>
	 * <p>Resolusi kode tetap dipakai LIVE oleh {@link #ambilAkun()}/{@link #ambilAkunDebet()} untuk
	 * menentukan akun jurnal yang berlaku — hanya efek samping penulisannya yang dihapus. Konsekuensi
	 * yang masih berlaku:</p>
	 * <ul>
	 *   <li>Pengelompokan efektif tetap ditentukan oleh <b>kesamaan teks kode</b> bila ada kecocokan
	 *   di cache, bukan oleh FK yang dipilih operator; FK hanya dipakai bila tidak ada kelompok aktif
	 *   berkode sama.</li>
	 *   <li>Menambah sebuah {@link KelompokItemGaji} baru yang kodenya kebetulan bertabrakan dengan
	 *   kode komponen gaji yang sudah ada tetap <b>memindahkan akun jurnal EFEKTIF</b>
	 *   komponen-komponen itu (untuk keperluan penjurnalan berikutnya) — yang berbeda sekarang: FK
	 *   tersimpan pada baris {@code item_gaji} itu sendiri <b>tidak ikut berubah</b>, sehingga
	 *   dampaknya tidak permanen dan hilang begitu tabrakan kode diperbaiki/dihapus. Penjaga
	 *   tabrakan kode lintas tabel tetap diperlukan di lapisan penyimpanan
	 *   {@code KelompokItemGajiAction.checkKodeKelompokItemGaji()} — lihat Javadoc kelas.</li>
	 *   <li>Pencocokan berhenti pada kecocokan pertama ({@code break}); bila ada beberapa kelompok
	 *   aktif berkode sama, yang menang adalah yang lebih dulu muncul pada urutan pemuatan cache —
	 *   tidak deterministik dari sudut pandang pengguna.</li>
	 *   <li>Bila {@link #reloadKelompokItemGaji()} belum sempat berjalan (cache masih kosong,
	 *   mis. karena kegagalan yang ditelan saat startup), pencocokan otomatis mati dan akun jurnal
	 *   jatuh ke FK per-item — <b>hasil penjurnalan bisa berbeda antara instans yang baru
	 *   dinyalakan dan instans yang cache-nya sudah terisi</b>, tanpa indikasi apa pun di UI.</li>
	 *   <li>Tidak ada apa pun yang menjaga kesesuaian kode antara kedua tabel selain penjaga di
	 *   {@code KelompokItemGajiAction}: nilai {@code kode} di {@code item_gaji} dan
	 *   {@code kelompok_item_gaji} sepenuhnya ditentukan operator, tanpa constraint/enum/seed bawaan
	 *   di basis data.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan galat:</b> pemeriksaan tiap elemen dibungkus {@code try/catch} yang ditelan,
	 * sehingga satu baris kelompok yang bermasalah (mis. proxy lepas sesi) hanya dilewati tanpa
	 * menghentikan penelusuran.</p>
	 *
	 * @return kelompok item gaji hasil pencocokan kode, atau nilai FK tersimpan bila tidak ada yang
	 *         cocok, atau {@code null} bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_item_gaji", nullable = true)
	public KelompokItemGaji getKelompokItemGaji() {
		kelompokItemGaji = check(kelompokItemGaji);
		KelompokItemGaji resolved = kelompokItemGaji;
		if (kode != null && !kode.trim().isEmpty()) {
			for (KelompokItemGaji itemGaji : kelompokItemGajis) {
				try {
					if (itemGaji.getKode() != null && itemGaji.getAktif()
							&& itemGaji.getKode().trim().equalsIgnoreCase(kode.trim())) {
						resolved = itemGaji;
						break;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/ItemGaji.java:351");
					// TODO: handle exception
				}
			}
		}
		return resolved;
	}

	/**
	 * Mengisi kelompok item gaji secara langsung. FK tersimpan ini tetap dipertahankan oleh
	 * {@link #getKelompokItemGaji()} kecuali {@link #getKode()} komponen ini cocok dengan kode salah
	 * satu kelompok aktif, dalam hal mana kelompok hasil pencocokan itulah yang dikembalikan (tanpa
	 * menimpa field ini — lihat Javadoc {@link #getKelompokItemGaji()}).
	 *
	 * @param kelompokItemGaji kelompok pemetaan akun, boleh {@code null}
	 */
	public void setKelompokItemGaji(KelompokItemGaji kelompokItemGaji) {
		this.kelompokItemGaji = kelompokItemGaji;
	}

}
