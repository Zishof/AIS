package ais.database.model.payroll;

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
import ais.database.model.Pegawai;

/**
 * <b>Penugasan komponen gaji kepada seorang pegawai</b> &mdash; satu record menyatakan "pegawai X
 * memiliki komponen gaji Y pada format slip Z". Tabel: {@code payroll.item_gaji_pegawai}. Inilah
 * <b>lapis ketiga</b> rantai penggajian AIS, yaitu lapis tempat katalog gaji yang berlaku umum
 * "dipasang" ke orang per orang, dan &mdash; bila diinginkan &mdash; <b>di-override</b> untuk orang
 * tersebut saja.
 *
 * <h2>Kedudukan dalam rantai penggajian (empat lapis)</h2>
 * <ol>
 *   <li>{@link FormatItemGaji} &mdash; <b>skema slip</b> (template slip gaji per cabang/departemen/
 *   level jabatan/satuan kerja). Satu-satunya pemilik kolom tenant di seluruh rantai ini.</li>
 *   <li>{@link ais.database.model.payroll.ItemGaji} &mdash; <b>katalog</b> baris-baris slip milik
 *   satu format ("Gaji Pokok", "Tunjangan Jabatan", "Potongan BPJS", &hellip;), berhierarki, dan
 *   membawa rumus bawaan pada kolom {@code default_formula}.</li>
 *   <li><b>{@code ItemGajiPegawai}</b> (kelas ini) &mdash; <b>penugasan/override per pegawai</b>.
 *   Menambahkan kolom {@code pegawai} dan bendera {@link #ikutiItemGaji} di atas bentuk yang nyaris
 *   identik dengan katalog.</li>
 *   <li>{@link ais.database.model.payroll.RencanaItemGajiPegawai} (rencana/anggaran) dan
 *   {@link ais.database.model.payroll.PembayaranItemGajiPegawai} (realisasi/slip yang dibayar dan
 *   dijurnal) &mdash; <b>baris dokumen</b> per pegawai per periode, membawa nominal rupiah.</li>
 * </ol>
 * <p>Arah rujukan selalu ke atas: dokumen &rarr; {@code ItemGajiPegawai} &rarr; {@code ItemGaji}.
 * Kelas ini tidak menyimpan koleksi balik ke lapis manapun; anak-anaknya sendiri ditemukan lewat
 * query balik atas kolom {@code bagian_dari} ({@link #getParent()}).</p>
 * <p><b>Kebijakan snapshot lapis 4 terhadap kelas ini</b> (berguna saat menjawab "kalau penugasan
 * diubah, apakah slip lama ikut berubah?"): kedua entity dokumen memegang FK ke baris ini &mdash;
 * pada {@code PembayaranItemGajiPegawai} kolomnya bahkan bernama {@code item_gaji} meski tipenya
 * {@code ItemGajiPegawai}, jebakan penamaan yang perlu diingat. Nominal ({@code nilai}) benar-benar
 * <b>dibekukan</b> saat baris dokumen dibuat. Teks {@code nama}/{@code kode}/{@code keterangan}/
 * {@code defaultFormula} memakai pola "isi kalau masih kosong" ({@code if (kode == null) kode = ...})
 * &mdash; jadi sekali termaterialisasi ia beku, tetapi baris yang kolomnya belum pernah terisi akan
 * memungut teks dari kelas ini <i>hari ini</i>. Sedangkan {@link #getTampilkanDiSlip()} dan
 * {@link #getSpace()} di lapis dokumen ditimpa <b>tanpa syarat</b> pada setiap pembacaan &mdash;
 * kedua bendera itu tidak pernah dibekukan sama sekali.</p>
 *
 * <h2>Bendera {@link #ikutiItemGaji} &mdash; mekanisme TERVERIFIKASI</h2>
 * <p>Bendera inilah inti kelas ini, dan maknanya sudah dipastikan dari tiga sumber kode sekaligus
 * (getter di kelas ini, layar {@code ais.action.master.payroll.ItemGajiPegawaiAction}, dan mesin
 * pembangkit {@code ais.action.master.payroll.util.ItemGajiPegawaiTreeModel}):</p>
 * <ul>
 *   <li><b>Di kelas ini:</b> <b>sepuluh</b> getter berperilaku "delegasi hidup" &mdash;
 *   {@link #getNama()}, {@link #getKeterangan()}, {@link #getNomorUrut()}, {@link #getAktif()},
 *   {@link #getKode()}, {@link #getDefaultFormula()}, {@link #getTampilkanDiSlip()},
 *   {@link #getSpace()}, {@link #getNilaiVariableBisaDiubah()} dan {@link #getFinalGaji()}. Bila
 *   {@link #getIkutiItemGaji()} bernilai {@code true} <i>dan</i> {@link #getItemGaji()} tidak
 *   {@code null}, kesepuluhnya <b>mengambil nilai dari katalog</b>, bukan dari kolom baris ini
 *   sendiri. Perhatikan: setiap getter tersebut <b>tidak hanya mengembalikan</b> nilai katalog, ia
 *   juga <b>menugaskannya ke field kelas ini</b> &mdash; lihat peringatan getter destruktif di
 *   bawah.</li>
 *   <li><b>Di layar:</b> {@code ItemGajiPegawaiAction} memasang checkbox "Ikuti Data Item Gaji" dan
 *   sebuah listener {@code onClick} yang me-{@code setDisabled(...)} kotak isian Kode, Nama,
 *   Formula Penghitungan, Nomor Urut, Keterangan, "Nilai Variable Bisa Diubah", Aktif, Space dan
 *   "Final Gaji" persis mengikuti status checkbox tersebut. Jadi selama bendera menyala, operator
 *   secara visual <b>tidak bisa</b> menyunting kolom-kolom itu; mematikannya "melepaskan" baris ini
 *   sehingga rumus/label/urutan boleh berbeda dari katalog untuk pegawai bersangkutan.</li>
 *   <li><b>Di mesin pembangkit:</b> {@code ItemGajiPegawaiTreeModel.copyByItemGaji(...)} membuat
 *   baris baru dengan hanya <b>lima</b> properti terisi &mdash; {@code formatItemGaji},
 *   {@code parent}, {@code itemGaji}, {@code pegawai}, dan {@code ikutiItemGaji = true}. Kolom
 *   {@code kode}/{@code nama}/{@code defaultFormula}/{@code keterangan}/{@code urutan} dan seluruh
 *   bendera <b>sengaja dibiarkan {@code NULL}</b> di database.</li>
 * </ul>
 * <p>Gabungan ketiganya menjelaskan catatan "lapis 3 LIVE selama {@code ikutiItemGaji} menyala"
 * secara presis: pada instalasi normal, <b>mayoritas baris {@code item_gaji_pegawai} secara fisik
 * hampir kosong</b> &mdash; ia praktis hanyalah baris jembatan (pegawai, item katalog, induk,
 * format). Seluruh isi yang tampil di layar dan dipakai mesin hitung dipasok <i>saat dibaca</i>
 * dari {@code ItemGaji}. Karena itu mengubah katalog langsung berdampak ke semua pegawai yang
 * benderanya menyala, tanpa migrasi data apa pun. Baris yang benar-benar membawa data sendiri
 * hanyalah baris yang benderanya sudah dimatikan operator.</p>
 *
 * <h2>Auto-materialisasi: baris dibuat oleh layar, bukan oleh operator</h2>
 * <p>Operator jarang membuat baris di sini secara manual. {@code ItemGajiPegawaiAction.onReloadTree()}
 * memanggil {@code ItemGajiPegawaiTreeModel.checkExistingItemGaji()} <b>setiap kali pohon dimuat</b>.
 * Method itu menghitung baris ber-{@code parent} {@code NULL} untuk pasangan (pegawai, format); bila
 * hasilnya {@code 0} ia menjalankan SQL mentah
 * {@code delete from payroll.item_gaji_pegawai where pegawai = ? and format_item_gaji = ?} lalu
 * membangun ulang seluruh pohon dari katalog lewat {@code copyByItemGaji(...)}. Konsekuensi yang
 * perlu diketahui:</p>
 * <ul>
 *   <li>Sekadar <b>membuka layar</b> "Item Gaji Pegawai" untuk seorang pegawai sudah <b>menulis ke
 *   database</b> (INSERT sejumlah baris katalog) &mdash; pembacaan yang tidak murni baca.</li>
 *   <li>Bila baris akar terhapus tetapi anak-anaknya tertinggal (baris yatim), pemuatan pohon
 *   berikutnya akan <b>menghapus seluruh baris pegawai+format tersebut</b>, termasuk baris yang
 *   benderanya sudah dimatikan dan membawa rumus khusus per pegawai. Tidak ada konfirmasi apa pun
 *   untuk jalur ini.</li>
 *   <li>{@code copyByItemGaji(...)} mencari baris yang sudah ada dengan kunci
 *   ({@code itemGaji}, {@code pegawai}) dan {@code setMaxResults(1)} &mdash; jadi baris kembar untuk
 *   pasangan yang sama tidak pernah terdeteksi, hanya salah satu yang dianggap ada.</li>
 *   <li>Kedua penghapusan massal itu dijalankan sebagai <b>SQL mentah</b>
 *   ({@code session.createSQLQuery("delete from payroll.item_gaji_pegawai where ...")}), sehingga
 *   <b>melewati Envers sepenuhnya</b> meski kelas ini beranotasi {@code @Audited}. Penghapusan
 *   penugasan gaji seorang pegawai karena itu <b>tidak meninggalkan jejak audit apa pun</b>,
 *   berbeda dari penghapusan satu-satu lewat tombol Hapus yang melewati {@code session.delete(...)}.</li>
 *   <li>Exception pada kedua method ditelan ({@code printStackTrace} + {@code ErrorAuditUtil.record})
 *   tanpa pesan ke pengguna &mdash; pembangunan ulang yang gagal separuh jalan tidak terlihat dari
 *   layar.</li>
 * </ul>
 * <p>Tombol "Reset" di layar memanggil {@code ItemGajiPegawaiTreeModel.reset()}, yang melakukan hal
 * serupa tetapi dijaga syarat berbeda: penghapusan mentah hanya dijalankan bila pegawai tersebut
 * <b>belum punya satu pun baris {@code RencanaItemGajiPegawai}</b>; bila sudah punya, {@code reset()}
 * melewati penghapusan dan hanya melengkapi baris yang belum ada. Syarat itu menghitung rencana
 * pegawai <i>lintas format dan lintas periode</i>, bukan hanya format yang sedang dibuka.</p>
 *
 * <h2>PERINGATAN &mdash; getter destruktif menyentuh RUMUS dan LABEL</h2>
 * <p>Kelas ini dipetakan Hibernate dengan <b>akses property</b> (anotasi {@code @Id} berada pada
 * getter) dan {@code dynamicUpdate = true}. Artinya setiap penugasan field yang terjadi <i>di dalam
 * getter</i> ikut terbaca oleh dirty-checking Hibernate dan <b>tersimpan permanen</b> ke database
 * pada flush berikutnya, tanpa satu pun {@code setXxx()} pernah dipanggil kode pemanggil. Kesepuluh
 * getter delegasi di atas semuanya bekerja seperti ini, dan dua di antaranya menyentuh hal yang
 * benar-benar berarti secara finansial:</p>
 * <ul>
 *   <li>{@link #getDefaultFormula()} <b>menimpa rumus per pegawai dengan rumus katalog</b>. Karena
 *   {@code ItemGaji.getDefaultFormula()} mengembalikan {@code ""} (string kosong) bila kolomnya
 *   {@code null}, membaca getter ini pada baris yang benderanya menyala <b>dapat mengganti rumus
 *   khusus pegawai menjadi string kosong secara permanen</b>.</li>
 *   <li>{@link #getKode()} menimpa kode komponen. Kode adalah <b>kunci yang dipakai mesin rumus</b>
 *   untuk merujuk komponen lain ({@code GP + TJAB + TTRANS}); mengubahnya berarti mengubah
 *   identitas komponen di mata seluruh rumus lain milik pegawai itu.</li>
 * </ul>
 * <p>Selama bendera menyala, dampak praktisnya tersembunyi (nilai yang dibaca memang selalu nilai
 * katalog). Dampaknya baru muncul <b>saat bendera dimatikan</b>: baris tidak kembali ke nilai
 * aslinya, melainkan mewarisi salinan katalog yang terakhir sempat tertulis. Jalur simpan di layar
 * memperkuat efek ini &mdash; {@code ItemGajiPegawaiAction.init(...)} mengisi kotak isian dari
 * getter-getter ini, dan {@code onSave()} menuliskan kembali isi kotak tersebut lewat
 * {@code setKode()}/{@code setNama()}/{@code setDefaultFormula()}/{@code setNomorUrut()}/
 * {@code setKeterangan()}, sehingga membuka lalu menyimpan sebuah baris sudah cukup untuk
 * membekukan nilai katalog ke kolom milik baris tersebut.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; label:</b> {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Penugasan (relasi):</b> {@link #getPegawai()} (kepada siapa),
 *   {@link #getItemGaji()} (komponen katalog mana), {@link #getFormatItemGaji()} (pada skema slip
 *   mana), {@link #getParent()} (posisi dalam hierarki slip).</li>
 *   <li><b>Kendali override:</b> {@link #getIkutiItemGaji()} / {@link #setIkutiItemGaji(Boolean)}
 *   dan field publik {@link #ikutiItemGaji}.</li>
 *   <li><b>Tata letak slip:</b> {@link #getNomorUrut()}, {@link #getDeep()}, {@link #getSpace()},
 *   {@link #getTampilkanDiSlip()}.</li>
 *   <li><b>Perhitungan:</b> {@link #getDefaultFormula()}, {@link #getNilaiVariableBisaDiubah()},
 *   {@link #getFinalGaji()}.</li>
 *   <li><b>Status &amp; statistik:</b> {@link #getAktif()}, {@link #getJmlDipakai()}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, plus {@code @Audited} (Envers) pada
 *   kelas.</li>
 * </ul>
 *
 * <h2>Hal non-obvious</h2>
 * <ul>
 *   <li><b>Tidak ada kolom tenant sama sekali.</b> Baris ini mencapai satuan kerja/yayasan hanya
 *   secara tidak langsung, lewat {@link #getFormatItemGaji()} atau {@link #getPegawai()}. Tidak ada
 *   {@code sekolah}/{@code yayasan}/{@code satuanKerja} yang bisa dipakai penyaring generik.</li>
 *   <li><b>Pengulangan deklarasi field terhadap {@link ais.database.model.GeneralValueObject}
 *   adalah KEHARUSAN TEKNIS, bukan bug.</b> Induk bukan {@code @Entity} maupun
 *   {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate sama sekali tidak
 *   memetakan properti induk. {@code nama}, {@code keterangan} dan {@code nomorUrut} <b>wajib</b>
 *   dideklarasikan ulang di sini agar tersimpan.</li>
 *   <li><b>Efek samping pengulangan itu pada pengurutan.</b>
 *   {@code GeneralValueObject.compareTo(...)} memakai {@code getNomorUrut()} sebagai kunci urut
 *   PERTAMA, dan karena {@link #getNomorUrut()} di kelas ini <i>meng-override</i> milik induk
 *   sekaligus <b>tidak pernah mengembalikan {@code null}</b>, cabang pertama itu selalu menang:
 *   {@code nim}/{@code nama}/{@code keterangan} tidak pernah terpakai sebagai kunci urut. Dua baris
 *   ber-{@code urutan} sama dianggap {@code compareTo == 0}. Ini penting karena
 *   {@code ItemGajiPegawaiAction} menyimpan sel pohonnya di
 *   {@code TreeMap<ItemGajiPegawai, Treecell[]>}: dua komponen dengan nomor urut kembar akan
 *   dianggap <b>kunci yang sama</b> dan saling menimpa di peta tersebut. Tidak ada indeks unik pada
 *   kolom {@code urutan}. Sebagai bonus, memasukkan object ke {@code TreeMap} itu sendiri memanggil
 *   {@code getNomorUrut()} &mdash; jadi ikut memicu penulisan-balik katalog yang dijelaskan di
 *   atas.</li>
 *   <li><b>{@link #toString()} tidak konsisten dengan getter.</b> Ia membaca <i>field</i>
 *   {@code kode} dan {@code nama} langsung, bukan {@link #getKode()}/{@link #getNama()}. Pada baris
 *   hasil auto-materialisasi (kedua kolom {@code NULL}) hasilnya adalah literal
 *   {@code "null - null"}, bukan nama komponen dari katalog.</li>
 *   <li><b>Nama kolom.</b> Hanya {@code nama}, {@code keterangan}, {@code urutan}
 *   ({@link #getNomorUrut()}), {@code id} dan keempat kolom FK yang punya anotasi eksplisit. Sisanya
 *   dipetakan {@code ais.database.hibernate.MyNamingStrategy} (turunan
 *   {@code org.hibernate.cfg.DefaultNamingStrategy}) yang memakai <b>nama properti apa adanya</b>,
 *   sehingga kolomnya benar-benar bernama {@code kode}, {@code aktif}, {@code defaultFormula},
 *   {@code deep}, {@code jmlDipakai}, {@code ikutiItemGaji}, {@code space},
 *   {@code nilaiVariableBisaDiubah}, {@code finalGaji}, {@code tampilkanDiSlip}, {@code oleh} dan
 *   {@code olehId} &mdash; camelCase, bukan snake_case seperti kolom beranotasi di sekitarnya.</li>
 *   <li><b>Tidak ada padanan {@code jadikan0JikaMinus}.</b> Katalog {@code ItemGaji} punya bendera
 *   "bulatkan nilai negatif menjadi 0", kelas ini tidak. Jadi tidak ada cara mematikan/menyalakan
 *   perilaku itu per pegawai &mdash; ia selalu mengikuti katalog.</li>
 *   <li><b>Tidak ada {@code equals}/{@code hashCode} lokal</b>; keduanya diwarisi dari
 *   {@link ais.database.model.GeneralValueObject} (berbasis {@code id}), sehingga dua object
 *   transient sama-sama ber-{@code id} {@code null} bisa dianggap setara.</li>
 *   <li><b>Seluruh isi tabel dimuat ke memori saat startup.</b> {@code DataUtil} mendaftarkan kelas
 *   ini pada {@code CLASS_JANGAN_DIBERSIHKAN}, dan {@code InitData} memasukkannya ke daftar
 *   preload. Akibatnya {@code InitDataHelper} melewati ambang "tabel kecil" dan selalu memuat penuh
 *   tabel ini, lalu tidak pernah membersihkannya dari cache. Berbeda dari master kecil lain di
 *   daftar yang sama, volume tabel ini berskala <b>jumlah pegawai &times; jumlah komponen</b>
 *   &mdash; pertimbangkan ini saat menelusuri pemakaian heap.</li>
 *   <li><b>Baris tanpa katalog tidak pernah tampil, tetapi tetap dihitung.</b>
 *   {@code ItemGajiPegawaiTreeModel.getChildren(...)} memasang
 *   {@code createAlias("itemGaji", "itemGaji")} yang berarti <b>INNER JOIN</b>, sedangkan
 *   {@code getChildCount(...)} tidak. Baris ber-{@code item_gaji} {@code NULL} &mdash; yaitu baris
 *   yang dibuat manual lewat tombol "Item Gaji Baru" &mdash; karena itu dihitung sebagai anak tetapi
 *   tidak pernah bisa dirender.</li>
 * </ul>
 *
 * <h2>Catatan keamanan (audit-only &mdash; kode TIDAK diubah)</h2>
 * <ul>
 *   <li><b>Cakupan tenant fail-open, {@code task_7b6038ac}.</b> Entity ini <b>terjangkau</b>
 *   mekanisme "Generic CRUD v2" ({@code dispatcher.jsp} &rarr;
 *   {@code GenericCrudDefinitionRegistry.tryAutoRegister} &rarr;
 *   {@code GenericCrudAutoEntityAdapter.scopeBindings()}), yang hanya memasang pembatas tenant untuk
 *   properti relasi bernama persis dari whitelist 12 nama tetap
 *   ({@code yayasan|sekolah|program|fakultas|jurusan|satuanKerja|mahasiswa|siswa|dosen|guru|
 *   orangTua|anggotaKoperasi}). Keempat relasi kelas ini &mdash; {@code pegawai},
 *   {@code itemGaji}, {@code formatItemGaji}, {@code parent} &mdash; <b>tidak satu pun</b> ada di
 *   whitelist tersebut, sehingga pembatas yang terpasang <b>kosong</b>: {@code applyScope} tidak
 *   menambahkan satu pun {@code Restrictions}, dan {@code validateObjectScope} selalu lolos. Yang
 *   memicu kekosongan itu adalah {@code addScope(...)} yang memanggil
 *   {@code metadata.getPropertyType(property)} lalu <b>menelan {@code MappingException} diam-diam</b>
 *   pada {@code catch} kosong &mdash; ketiadaan kolom dibaca sebagai "tidak perlu disaring", bukan
 *   "tolak". Mutasi lewat jalur ini kebetulan tertutup (adapter jatuh ke mode {@code READ_ONLY}
 *   karena {@code ItemGajiPegawaiAction.init(...)} bersignature dua parameter sehingga
 *   {@code GenericCrudExistingActionInvoker.supports(...)} bernilai {@code false}), tetapi
 *   <b>daftar, detail, dan ekspor massal PDF/DOCX/PPTX tetap terbuka lintas tenant</b>.</li>
 *   <li><b>Menambahkan {@code pegawai} ke whitelist TIDAK menutup celah ini</b> &mdash; koreksi
 *   penting terhadap dugaan yang wajar muncul karena entity ini, berbeda dari katalog
 *   {@link ais.database.model.payroll.ItemGaji}, memang <i>punya</i> properti {@code pegawai}.
 *   Alasannya: {@code scopeBindings()} mengisi nilai pembatas dari <b>pengguna yang login</b>
 *   ({@code invoke(user, "getPegawai")}), bukan dari pemilik data. Hasilnya berupa <i>self-scope</i>
 *   ("hanya boleh melihat komponen gaji milik saya sendiri") yang justru mematikan fungsi layar
 *   HRD, dan untuk staf HRD yang tidak tertaut record {@code Pegawai} nilainya {@code null} sehingga
 *   {@code addScope} langsung {@code return} &mdash; kembali ke binding kosong. Lagi pula
 *   {@code addScope} hanya memeriksa properti <b>langsung</b>, tidak mendukung jalur tak-langsung
 *   {@code pegawai.satuanKerja}; jadi meskipun {@link ais.database.model.Pegawai} punya kolom
 *   tenant, mekanisme ini secara struktural tidak sanggup memakainya. Kesimpulan batch 81 untuk
 *   {@code ItemGaji} karena itu <b>berlaku sama di sini</b>, dan perbaikan yang benar tetap:
 *   jadikan binding kosong sebagai <b>PENOLAKAN</b>, bukan restriksi-nol.</li>
 *   <li><b>Jalur CRUD generik kedua yang lebih longgar.</b> Selain Generic CRUD v2, entity ini juga
 *   terjangkau lewat dispatcher lama
 *   {@code /baru?hanya_tampil_jsp=true&amp;p=pagesmasterpayrollitemgajipegawaizul&amp;s=index}, yang
 *   memanggil {@code DynamicJspCrudGenerator}. Gerbang dispatcher itu ({@code bolehAksesModulKantin})
 *   <b>fail-open untuk seluruh modul non-kantin</b>, sehingga halaman terbuka bagi setiap pengguna
 *   yang login tanpa pemeriksaan menu sama sekali. Mutasinya masih digerbangi
 *   {@code CommonPrivilages}, tetapi penyaring tenant generator itu mencocokkan berdasarkan
 *   <b>tipe</b> dan hanya mengenal enam tipe ({@code Siswa}, {@code Mahasiswa}, {@code Fakultas},
 *   {@code Jurusan}, {@code Sekolah}, {@code Yayasan}) &mdash; {@link ais.database.model.Pegawai}
 *   bukan salah satunya, jadi baca dan ekspor kembali tanpa batas tenant. Sebagai catatan, jalur
 *   New UI {@code /WEB-INF/new/payroll/services/item_gaji_pegawai_service.jsp} justru <b>aman</b>:
 *   dispatcher-nya fail-closed (401 tanpa sesi, 403 lewat {@code NewUiRouteGuard}, CSRF + wajib POST
 *   untuk mutasi) &mdash; verifikasi negatif yang menenangkan.</li>
 *   <li><b>Siapa yang bisa mengubah penugasan komponen gaji pegawai.</b> Layar ZK
 *   {@code ItemGajiPegawaiAction} sebenarnya <i>sudah</i> memakai {@code CommonPrivilages}: gerbang
 *   {@code READ} di {@code doAfterCompose()}, dan tombol Tambah/Ubah/Hapus masing-masing
 *   di-{@code setVisible(...)} sesuai {@code CREATE}/{@code UPDATE}/{@code DELETE} &mdash; jauh
 *   lebih baik daripada layar tetangganya {@code RencanaGajiPunyaPegawaiAction} yang nol import
 *   {@code CommonPrivilages}. Namun ketiga bendera itu <b>hanya dipakai untuk visibilitas</b>: tidak
 *   ada satu pun cabang {@code if (!edit) return;} di jalur eksekusi, sehingga penegakannya bergantung
 *   pada tombol tidak dirender &mdash; bukan pada handler menolak bekerja.</li>
 *   <li><b>Tombol "Reset" sama sekali tidak digerbangi.</b> Di {@code item_gaji_pegawai.zul} tombol
 *   itu bahkan <b>tidak diberi atribut {@code id}</b>, sehingga secara teknis tidak mungkin
 *   di-{@code setVisible(delete)} dari Java seperti tombol lain; ia selalu terlihat dan aktif untuk
 *   siapa pun yang bisa membuka halaman. Handler {@code onResetTree()} hanya menampilkan dialog
 *   konfirmasi &mdash; <b>nol pemeriksaan hak</b> sebelum memanggil
 *   {@code ItemGajiPegawaiTreeModel.reset()} yang menghapus mentah lalu membangun ulang seluruh
 *   penugasan komponen gaji seorang pegawai. Pemegang hak <b>BACA saja</b> karena itu dapat
 *   memusnahkan seluruh override gaji per pegawai (rumus khusus, nominal khusus) selama pegawai
 *   tersebut belum memiliki baris {@code RencanaItemGajiPegawai}. Bentuknya sekeluarga dengan tombol
 *   "Hitung Ulang" pada {@code task_11fcffa9}, tetapi ini berkas dan layar yang berbeda.</li>
 *   <li><b>Gerbang hak itu sendiri dapat dipalsukan.</b> {@code CommonPrivilages.checkPrevilages(...)}
 *   mengevaluasi hak terhadap atribut sesi {@code currentMenu}, dan
 *   {@code CommonMenuAccessHelper} hanya me-resolve ulang atribut itu terhadap URL halaman bila
 *   nilainya masih {@code null}. Sementara {@code ais.action.servlet.DisplayMenu} dan
 *   {@code webapp/WEB-INF/baru/index.jsp} sama-sama menanam {@code currentMenu} dari <b>parameter
 *   URL mentah</b> tanpa memeriksa bahwa menu itu memang milik peran pengguna. Akibatnya granularitas
 *   per-menu runtuh: hak tertinggi yang dipegang peran pengguna pada menu <i>mana pun</i> dapat
 *   diterapkan ke layar ini. Dieskalasi terpisah sebagai {@code task_9f520b16} (perluasan
 *   {@code task_9b7ff647}) karena akarnya lintas-aplikasi, bukan milik entity ini.</li>
 *   <li><b>IDOR pemilihan pegawai.</b> {@code ItemGajiPegawaiAction.doAfterCompose()} mengambil
 *   pegawai langsung dari parameter URL
 *   ({@code ConstantValues.ambil(Pegawai.class.getName(), Long.parseLong(execution.getParameter("pegawai")))})
 *   tanpa memeriksa kepemilikan tenant, lalu me-{@code setDisabled(true)} bandbox pemilih &mdash;
 *   penguncian yang murni kosmetik. Layar ini juga menulis ke entity {@link ais.database.model.Pegawai}
 *   sendiri tanpa gerbang: listener pemilihan Format memperbarui kolom {@code formatItemGaji2..5},
 *   dan timer pemuatan pohon menulis {@code pegawai.setNilaiGaji(...)}.</li>
 *   <li><b>Pemilihan pegawai tidak dibatasi tenant.</b> Pegawai dipilih lewat
 *   {@code AmbilDataPegawaiBanbox}; konstruktor tanpa argumen (yang dipakai {@code item_gaji_pegawai.zul})
 *   meneruskan {@code !Common.getApakahAdminBolehLihatSemuaPegawai()}. Untuk pengguna berperan admin
 *   &mdash; atau peran apa pun yang terdaftar pada konfigurasi
 *   {@code admin_yg_boleh_lihat_semua_data_pegawai} &mdash; scoping dimatikan seluruhnya. Scoping
 *   yang tersisa untuk non-admin adalah hierarki <i>atasan&ndash;bawahan</i>, <b>bukan</b> satuan
 *   kerja/yayasan; tidak ada satu pun filter tenant pada jalur ini.</li>
 *   <li><b>Tulis dipicu oleh hak baca.</b> Seperti dijelaskan di bagian auto-materialisasi,
 *   pemuatan pohon oleh pengguna berhak {@code READ} saja tetap meng-INSERT (dan pada kondisi baris
 *   yatim, meng-DELETE) baris di {@code payroll.item_gaji_pegawai}.</li>
 * </ul>
 *
 * @see ais.database.model.payroll.ItemGaji
 * @see ais.database.model.payroll.FormatItemGaji
 * @see ais.database.model.payroll.RencanaItemGajiPegawai
 * @see ais.database.model.payroll.PembayaranItemGajiPegawai
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "item_gaji_pegawai")
public class ItemGajiPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap dan tidak boleh diubah agar object yang sudah ter-serialisasi
	 * (mis. di sesi ZK atau cache) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, {@code IDENTITY} (auto-increment). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah melewati interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Setter ini menolak nilai kosong secara diam-diam:</b> argumen {@code null} maupun string
	 * yang hanya berisi spasi diabaikan tanpa exception, sehingga nilai lama tetap bertahan. Akibatnya
	 * jejak audit tidak pernah bisa "dikosongkan" lewat jalur ini &mdash; sifat yang disengaja agar
	 * proses batch yang tidak membawa konteks pengguna tidak menghapus atribusi yang sudah benar.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah {@code trim}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code "<kode> - <nama>"}.
	 *
	 * <p><b>Kuirk penting:</b> method ini membaca <b>field</b> {@code kode} dan {@code nama} secara
	 * langsung, <i>bukan</i> lewat {@link #getKode()}/{@link #getNama()}. Ia karena itu <b>tidak</b>
	 * ikut mekanisme delegasi {@link #ikutiItemGaji}. Pada baris hasil auto-materialisasi
	 * ({@code ItemGajiPegawaiTreeModel.copyByItemGaji}) kedua kolom memang dibiarkan {@code NULL},
	 * sehingga keluarannya adalah literal {@code "null - null"} alih-alih nama komponen dari katalog.
	 * Jangan pakai method ini untuk label yang dilihat pengguna; pakai {@link #getNama()}.</p>
	 *
	 * @return gabungan kode dan nama tersimpan, tanpa resolusi ke katalog
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan diam-diam</b>
	 * sehingga atribusi lama tidak tertimpa proses yang tidak membawa konteks pengguna.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah {@code trim}
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
	 * @return nama pengguna, atau {@code null} bila baris belum pernah melewati interceptor audit
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi jejak audit ({@link #getOleh()},
	 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}) dari konteks pengguna aktif tepat sebelum
	 * Hibernate menjalankan {@code UPDATE}.
	 *
	 * <p>Jangan dipanggil langsung &mdash; ini hook lifecycle. Karena hanya terpasang pada
	 * {@code @PreUpdate} (bukan {@code @PrePersist}), baris yang <b>baru dibuat</b> mesin
	 * auto-materialisasi tidak melewati method ini; atribusi audit barunya baru terisi pada
	 * pembaruan pertama. Perubahan yang dilakukan lewat SQL mentah &mdash; termasuk penghapusan massal
	 * di {@code ItemGajiPegawaiTreeModel.reset()}/{@code checkExistingItemGaji()} &mdash; juga
	 * melewati hook ini sepenuhnya.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object (bukan waktu simpan)
	 * dan diperbarui {@link #onUpdate()}. Kolom: {@code tanggal_dirubah}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; umumnya diisi {@link #onUpdate()}, bukan kode aplikasi
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang dibuat lewat
	 *         konstruktor karena field-nya sudah diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama komponen gaji milik baris ini. Sering {@code NULL} &mdash; lihat {@link #getNama()}. */
	private String nama;
	/** Skema slip pemilik baris ini. FK {@code format_item_gaji}, wajib. Lihat {@link #getFormatItemGaji()}. */
	private FormatItemGaji formatItemGaji;
	/** Induk hierarki dalam slip. FK {@code bagian_dari}, opsional. Lihat {@link #getParent()}. */
	private ItemGajiPegawai parent;
	/** Baris katalog yang di-"ikuti" baris ini. FK {@code item_gaji}, opsional. Lihat {@link #getItemGaji()}. */
	private ItemGaji itemGaji;
	/** Pegawai penerima komponen gaji ini. FK {@code pegawai}, opsional di skema. Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Nomor urut tampil pada slip. Kolom {@code urutan}. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Bendera aktif; default {@code true} pada object baru. Lihat {@link #getAktif()}. */
	private Boolean aktif = true;
	/** Bendera tampil di slip gaji cetak; default {@code true}. Lihat {@link #getTampilkanDiSlip()}. */
	private Boolean tampilkanDiSlip = true;
	/** Kode komponen; kunci rujukan antar-rumus. Sering {@code NULL} &mdash; lihat {@link #getKode()}. */
	private String kode;
	/** Rumus perhitungan bawaan. Sering {@code NULL} &mdash; lihat {@link #getDefaultFormula()}. */
	private String defaultFormula;
	/** Keterangan bebas. Sering {@code NULL} &mdash; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kedalaman hierarki hasil kalkulasi UI. Lihat {@link #getDeep()}. */
	private Integer deep;
	/** Penghitung pemakaian; default {@code 0}. Lihat {@link #getJmlDipakai()}. */
	private Long jmlDipakai = 0L;

	/**
	 * Bendera "ikuti data item gaji" &mdash; inti kelas ini (lihat penjelasan lengkap pada Javadoc
	 * kelas). Selama bernilai {@code true}, sepuluh getter di kelas ini mengambil nilainya dari
	 * {@link #getItemGaji()} alih-alih dari kolom baris ini sendiri.
	 *
	 * <p><b>Field ini {@code public}</b> &mdash; anomali dibanding seluruh field lain yang
	 * {@code private}. Konsekuensinya kode luar dapat membaca/menulisnya <b>tanpa melewati</b>
	 * {@link #getIkutiItemGaji()}, sehingga penormalan {@code null}&rarr;{@code true} pada getter itu
	 * bisa terlewat. Perlakukan sebagai detail historis, bukan API yang boleh dipakai: gunakan
	 * {@link #getIkutiItemGaji()}/{@link #setIkutiItemGaji(Boolean)}.</p>
	 */
	public Boolean ikutiItemGaji = true;
	/** Bendera baris pemisah/kosong pada slip; default {@code false}. Lihat {@link #getSpace()}. */
	private Boolean space = false;
	/** Bendera "nilai variabel boleh diubah operator" saat pembayaran. Lihat {@link #getNilaiVariableBisaDiubah()}. */
	private Boolean nilaiVariableBisaDiubah;
	/** Bendera penanda komponen "gaji final" (total akhir). Lihat {@link #getFinalGaji()}. */
	private Boolean finalGaji;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Object hasil konstruktor ini sudah membawa nilai bawaan {@code aktif = true},
	 * {@code tampilkanDiSlip = true}, {@code ikutiItemGaji = true}, {@code space = false},
	 * {@code jmlDipakai = 0} dan {@code tanggal_dirubah} = waktu saat ini. Jadi baris baru
	 * <b>secara bawaan mengikuti katalog</b>; itulah pula yang ditegaskan ulang
	 * {@code ItemGajiPegawaiTreeModel.copyByItemGaji(...)} lewat {@code setIkutiItemGaji(true)}.</p>
	 */
	public ItemGajiPegawai() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
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
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Dipanggil Hibernate setelah {@code INSERT}. Kode aplikasi hanya boleh memanggilnya dengan
	 * {@code null} untuk membuat salinan lepas &mdash; pola yang dipakai
	 * {@code ItemGajiPegawaiTreeModel.copyByFormat(...)} saat menyalin penugasan ke format lain.</p>
	 *
	 * @param id kunci utama, atau {@code null} untuk menjadikan object sebagai baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama komponen gaji yang berlaku untuk baris ini.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif.</b> Bila {@link #getIkutiItemGaji()} bernilai
	 * {@code true} dan katalog {@link #getItemGaji()} tersedia, nilai diambil dari
	 * {@code ItemGaji.getNama()} dan <b>ditugaskan ke field {@code nama} baris ini</b>. Karena kelas
	 * dipetakan dengan akses property dan {@code dynamicUpdate = true}, penugasan itu terbaca
	 * dirty-checking Hibernate dan tersimpan permanen pada flush berikutnya &mdash; tanpa
	 * {@link #setNama(String)} pernah dipanggil.</p>
	 * <p>Perhatikan urutan operasinya: field {@code itemGaji} lebih dulu di-refresh lewat
	 * {@link #getItemGaji()} (yang me-resolve proxy lazy), sehingga syarat {@code itemGaji != null}
	 * pada baris berikutnya dinilai terhadap katalog yang sudah ter-resolve.</p>
	 *
	 * @return nama komponen dari katalog bila bendera menyala; jika tidak, nama tersimpan baris ini
	 *         (yang pada baris hasil auto-materialisasi bernilai {@code null})
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			nama = itemGaji.getNama();
		}
		return this.nama;
	}

	/**
	 * Menetapkan nama komponen gaji khusus untuk baris ini.
	 *
	 * <p>Hanya bermakna bila {@link #getIkutiItemGaji()} dimatikan; selama bendera menyala nilai yang
	 * disimpan akan ditimpa katalog pada pembacaan {@link #getNama()} berikutnya. Layar
	 * {@code ItemGajiPegawaiAction} juga menonaktifkan kotak isian Nama selama bendera menyala.</p>
	 *
	 * @param nama nama komponen; kolom {@code nullable = false}, tetapi string kosong tetap lolos
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas yang berlaku untuk baris ini.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif</b>, mekanismenya identik {@link #getNama()}: bila
	 * bendera {@link #getIkutiItemGaji()} menyala dan katalog tersedia, keterangan katalog ditugaskan
	 * ke field baris ini dan ikut ter-flush ke database.</p>
	 *
	 * @return keterangan dari katalog bila bendera menyala; jika tidak, keterangan tersimpan baris ini
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			keterangan = itemGaji.getKeterangan();
		}
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas khusus untuk baris ini.
	 *
	 * @param keterangan teks keterangan; hanya bertahan bila {@link #getIkutiItemGaji()} dimatikan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan skema slip ({@link FormatItemGaji}) pemilik baris ini.
	 *
	 * <p>FK {@code format_item_gaji} bersifat <b>wajib</b> ({@code nullable = false}) &mdash; satu
	 * dari sedikit kolom yang benar-benar dijamin terisi. Relasi ini penting bukan hanya untuk tata
	 * letak: {@code FormatItemGaji} adalah <b>satu-satunya pemilik kolom tenant</b> di seluruh rantai
	 * penggajian, sehingga baris ini mencapai satuan kerja/yayasan hanya lewat sini (atau lewat
	 * {@link #getPegawai()}).</p>
	 * <p>Getter memanggil {@code check(...)} milik {@link ais.database.model.GeneralValueObject} untuk
	 * me-resolve proxy lazy: cache in-memory &rarr; session aktif &rarr; session baru &rarr;
	 * argumen apa adanya bila keempatnya gagal. Nilai hasil resolusi ditugaskan kembali ke field
	 * (menghindari resolusi berulang), bukan mengganti data dengan nilai lain.</p>
	 *
	 * @return skema slip pemilik, sudah ter-resolve dari proxy lazy bila memungkinkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji", nullable = false)
	public FormatItemGaji getFormatItemGaji() {
		formatItemGaji = check(formatItemGaji);
		return formatItemGaji;
	}

	/**
	 * Menetapkan skema slip pemilik baris ini.
	 *
	 * <p>Diisi {@code ItemGajiPegawaiTreeModel} saat auto-materialisasi dan
	 * {@code ItemGajiPegawaiAction.onSave()} dari bandbox "Format" di layar. Memindahkan baris ke
	 * format lain berarti memindahkannya ke skema slip &mdash; dan karenanya berpotensi ke tenant
	 * &mdash; yang berbeda, tanpa ada penjaga apa pun di kelas ini.</p>
	 *
	 * @param formatItemGaji skema slip pemilik; wajib terisi sebelum baris disimpan
	 */
	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}

	/**
	 * Mengembalikan induk hierarki baris ini dalam susunan slip (kolom {@code bagian_dari}).
	 *
	 * <p>Hierarki dipakai untuk sub-total bertingkat: "Total Tunjangan" menjadi induk dari
	 * "Tunjangan Jabatan" dan "Tunjangan Transport". Baris ber-{@code parent} {@code null} adalah
	 * <b>baris akar</b>, dan jumlah baris akar itulah yang dijadikan penanda oleh
	 * {@code ItemGajiPegawaiTreeModel.checkExistingItemGaji()} untuk memutuskan apakah pohon perlu
	 * dibangun ulang &mdash; sehingga kehilangan baris akar memicu penghapusan seluruh baris
	 * pegawai+format tersebut.</p>
	 * <p>Relasi ini <b>tidak memvalidasi apa pun</b>: tidak ada pemeriksaan bahwa induk memiliki
	 * {@code pegawai} atau {@code formatItemGaji} yang sama, dan tidak ada deteksi siklus.</p>
	 *
	 * @return baris induk, atau {@code null} bila baris ini berada di akar slip
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian_dari", nullable = true)
	public ItemGajiPegawai getParent() {
		parent = check(parent);
		return parent;
	}

	/**
	 * Menetapkan induk hierarki baris ini.
	 *
	 * @param parent baris induk, atau {@code null} untuk menjadikannya baris akar
	 */
	public void setParent(ItemGajiPegawai parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan nomor urut tampil baris ini pada slip (kolom {@code urutan}).
	 *
	 * <p><b>Getter delegasi, destruktif, dan bernilai bawaan bercabang dua.</b> Alurnya:</p>
	 * <ol>
	 *   <li>bila field {@code nomorUrut} {@code null}, ia lebih dulu <b>ditugaskan {@code 0}</b>;</li>
	 *   <li>bila {@link #getIkutiItemGaji()} menyala dan katalog tersedia, {@code nomorUrut}
	 *   <b>ditimpa</b> nilai katalog &mdash; yang boleh saja {@code null};</li>
	 *   <li>nilai kembali adalah {@code nomorUrut}, kecuali bila hasil langkah 2 membuatnya
	 *   {@code null}, yang dikembalikan sebagai <b>{@code 1}</b>.</li>
	 * </ol>
	 * <p>Perhatikan ketidakkonsistenannya: nilai bawaan yang <i>disimpan</i> adalah {@code 0},
	 * sedangkan nilai bawaan yang <i>dikembalikan</i> pada kasus katalog-null adalah {@code 1}.
	 * Penugasan pada langkah 1 dan 2 sama-sama ter-flush permanen ke database.</p>
	 * <p>Method ini meng-override {@code GeneralValueObject.getNomorUrut()} dan karena
	 * <b>tidak pernah mengembalikan {@code null}</b>, ia menjadikan {@code nomorUrut} sebagai
	 * satu-satunya kunci {@code compareTo(...)} yang pernah terpakai untuk kelas ini (lihat Javadoc
	 * kelas soal tabrakan kunci {@code TreeMap} di {@code ItemGajiPegawaiAction}).</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	@Column(name = "urutan")
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 0;
		}
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			nomorUrut = itemGaji.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menetapkan nomor urut tampil baris ini.
	 *
	 * <p>Tidak ada indeks unik pada kolom {@code urutan}, sehingga nomor kembar diperbolehkan
	 * database &mdash; dengan konsekuensi tabrakan kunci pada {@code TreeMap} yang dijelaskan di
	 * Javadoc kelas. Layar mewajibkan kolom ini terisi ({@code onSave()} menolak nilai {@code null}),
	 * tetapi tidak memeriksa keunikannya.</p>
	 *
	 * @param nomorUrut nomor urut tampil
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan status aktif komponen gaji untuk pegawai ini.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif</b>: {@code null} lebih dulu dinormalkan menjadi
	 * {@code true}, lalu bila {@link #getIkutiItemGaji()} menyala nilai katalog menimpa field dan
	 * ikut ter-flush ke database.</p>
	 * <p>Bendera ini dipakai sebagai penyaring pemuatan pohon:
	 * {@code ItemGajiPegawaiTreeModel} menambahkan {@code Restrictions.eq("aktif", true)} kecuali
	 * mode "tampilkan semua" dinyalakan. Menonaktifkan komponen di <b>katalog</b> karena itu langsung
	 * menyembunyikannya dari slip semua pegawai yang benderanya menyala.</p>
	 *
	 * @return {@code true} bila komponen aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			aktif = itemGaji.getAktif();
		}
		return aktif;
	}

	/**
	 * Menetapkan status aktif komponen gaji untuk pegawai ini.
	 *
	 * @param aktif {@code true} untuk mengaktifkan; hanya bertahan bila {@link #getIkutiItemGaji()}
	 *              dimatikan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode komponen gaji yang berlaku untuk baris ini.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif, dan ini yang paling berdampak setelah
	 * {@link #getDefaultFormula()}.</b> Kode bukan sekadar label: ia adalah <b>token yang dipakai
	 * mesin rumus</b> untuk merujuk komponen lain. {@code ItemGajiPegawaiTreeModel
	 * .hitungItemGajiPegawai(kode, ...)} menerima kode sebagai parameter pencarian, dan rumus seperti
	 * {@code GP + TJAB} diselesaikan dengan mencari {@code ItemGajiPegawai} ber-{@code kode} sama
	 * untuk pegawai dan format yang sama. Menimpa kode berarti mengubah identitas komponen di mata
	 * seluruh rumus lain milik pegawai tersebut.</p>
	 * <p>Rincian pencarian rekursif itu perlu diketahui karena longgar: kriterianya
	 * ({@code pegawai}, {@code kode}, {@code formatItemGaji}) <b>tidak menyaring {@link #getAktif()}</b>,
	 * dan bila ada lebih dari satu baris berkode sama ia memilih dengan {@code Order.desc("id")} +
	 * {@code setMaxResults(1)} &mdash; yaitu baris terbaru. Jadi kode kembar tidak menimbulkan
	 * kesalahan, hanya membuat rumus diam-diam memakai salah satu baris saja, dan komponen yang sudah
	 * dinonaktifkan tetap bisa menyumbang nilai ke rumus komponen lain.</p>
	 *
	 * @return kode dari katalog bila bendera menyala; jika tidak, kode tersimpan baris ini (yang pada
	 *         baris hasil auto-materialisasi bernilai {@code null})
	 */
	public String getKode() {
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			kode = itemGaji.getKode();
		}
		return kode;
	}

	/**
	 * Menetapkan kode komponen gaji khusus untuk baris ini.
	 *
	 * <p>Tidak ada penjaga keunikan kode di kelas ini maupun di layar &mdash; dua baris milik pegawai
	 * dan format yang sama boleh berkode identik, dan pencarian rumus hanya akan memakai salah
	 * satunya.</p>
	 *
	 * @param kode kode komponen; wajib diisi di layar kecuali baris ditandai {@link #getSpace()}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan rumus perhitungan yang berlaku untuk baris ini.
	 *
	 * <p><b>Getter paling berbahaya di kelas ini.</b> Bila {@link #getIkutiItemGaji()} menyala dan
	 * katalog tersedia, rumus katalog <b>ditugaskan ke field {@code defaultFormula} baris ini</b> dan
	 * &mdash; karena akses property + {@code dynamicUpdate} &mdash; ter-flush permanen ke database.
	 * Ada dua konsekuensi yang perlu benar-benar disadari:</p>
	 * <ul>
	 *   <li>{@code ItemGaji.getDefaultFormula()} mengembalikan <b>{@code ""} (string kosong), bukan
	 *   {@code null}</b>, bila kolom katalognya kosong. Jadi membaca getter ini pada baris yang
	 *   benderanya menyala sementara katalognya belum berumus akan <b>mengganti rumus khusus pegawai
	 *   menjadi string kosong secara permanen</b>.</li>
	 *   <li>Kerusakan itu <b>tidak terlihat selama bendera masih menyala</b> (nilai yang dibaca tetap
	 *   nilai katalog). Ia baru muncul ketika operator mematikan "Ikuti Data Item Gaji" dengan harapan
	 *   mendapatkan kembali rumus khusus pegawai &mdash; yang saat itu sudah hilang.</li>
	 * </ul>
	 * <p>Rumus dievaluasi mesin ekspresi <b>exp4j</b> di
	 * {@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai(...)}, dengan token dipisah spasi,
	 * fungsi/operator tambahan dari {@code LogicalUtil}, substitusi variabel absensi/kepegawaian, dan
	 * rujukan rekursif antar-komponen lewat {@link #getKode()}. Rincian perbendaharaan fungsi dan
	 * kuirknya didokumentasikan di {@link ais.database.model.payroll.ItemGaji}. Yang perlu diingat di
	 * sini: <b>kegagalan rumus tidak pernah membatalkan proses</b> &mdash; exception ditelan dan
	 * komponen bernilai {@code 0.0}, bahkan hasil {@code Expression.validate()} pun dibuang
	 * (variabelnya ditandai {@code @SuppressWarnings("unused")}).</p>
	 * <p><b>Waspadai nilai {@code null}.</b> Method ini boleh mengembalikan {@code null} (berbeda dari
	 * {@code ItemGaji.getDefaultFormula()} yang selalu mengembalikan minimal {@code ""}), dan
	 * {@code BayarGajiPegawaiAction} memanggil {@code getDefaultFormula().isEmpty()} <i>sebelum</i>
	 * memeriksa {@code null} pada klausa berikutnya &mdash; berujung {@code NullPointerException}
	 * untuk baris yang benderanya dimatikan tetapi rumusnya belum diisi.</p>
	 *
	 * @return rumus dari katalog bila bendera menyala; jika tidak, rumus tersimpan baris ini
	 */
	public String getDefaultFormula() {
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			defaultFormula = itemGaji.getDefaultFormula();
		}
		return defaultFormula;
	}

	/**
	 * Menetapkan rumus perhitungan khusus untuk pegawai ini.
	 *
	 * <p>Inilah alasan utama keberadaan lapis "override per pegawai": Gaji Pokok yang nominalnya
	 * spesifik per orang ditulis di sini, bukan di katalog. Nilai hanya bertahan bila
	 * {@link #getIkutiItemGaji()} dimatikan &mdash; layar pun menonaktifkan kotak isian "Formula
	 * Penghitungan" selama bendera menyala.</p>
	 * <p>Tidak ada validasi sintaksis rumus di sini maupun di {@code onSave()}; rumus yang salah baru
	 * ketahuan saat dievaluasi, dan kegagalan evaluasi umumnya berujung nilai {@code 0.0} tanpa
	 * pesan kesalahan.</p>
	 *
	 * @param defaultFormula ekspresi rumus dengan token dipisah spasi
	 */
	public void setDefaultFormula(String defaultFormula) {
		this.defaultFormula = defaultFormula;
	}

	/**
	 * Mengembalikan kedalaman hierarki baris ini pada pohon slip.
	 *
	 * <p>Getter murni &mdash; <b>tidak</b> mendelegasikan ke katalog dan tidak menulis apa pun,
	 * berbeda dari sepuluh getter di sekitarnya. Nilainya bukan turunan otomatis dari
	 * {@link #getParent()}: ia hanya berisi apa pun yang terakhir kali ditulis
	 * {@link #setDeep(Integer)}, sehingga bisa basi bila hierarki dipindah tanpa perhitungan ulang.</p>
	 * <p><b>Praktisnya kolom ini write-only.</b> Satu-satunya penulis adalah
	 * {@code ItemGajiPegawaiTreeModel.getParentCount(...)} &mdash; sebuah method ber-awalan "get" yang
	 * diam-diam melakukan {@code setDeep(...)} + {@code Common.refreshSaveOrUpdate(...)}, dan
	 * satu-satunya pemanggilnya adalah pencetakan laporan
	 * {@code ais.action.report.format1.payroll.LaporanItemGajiPegawai}. Jadi kolom ini diperbarui
	 * sebagai <b>efek samping mencetak laporan</b>. Satu-satunya pembacaan nilainya adalah
	 * perbandingan di dalam penulis itu sendiri: laporan yang bersangkutan memakai penghitung
	 * lokalnya sendiri untuk indentasi, bukan nilai kolom ini.</p>
	 *
	 * @return kedalaman hierarki hasil kalkulasi terakhir, atau {@code null} bila belum pernah dihitung
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menetapkan kedalaman hierarki baris ini.
	 *
	 * @param deep kedalaman hierarki (akar umumnya {@code 0} atau {@code 1}, tergantung pemanggil)
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan penghitung berapa kali komponen ini dipakai.
	 *
	 * <p>Getter murni, tanpa delegasi maupun penulisan. Nilai bawaan {@code 0} pada object baru,
	 * sehingga tidak pernah {@code null} kecuali kolom database memang berisi {@code NULL} pada baris
	 * lama.</p>
	 * <p><b>Properti ini mati untuk kelas ini.</b> Tidak ada satu pun pemanggil
	 * {@link #getJmlDipakai()} maupun {@link #setJmlDipakai(Long)} di seluruh repo, dan tidak ada
	 * query yang mengurutkan berdasarkan kolomnya &mdash; nilainya karena itu selalu tetap {@code 0}.
	 * Penghitung yang benar-benar dipelihara ada di <b>katalog</b>
	 * {@link ais.database.model.payroll.ItemGaji}, dinaikkan {@code AmbilDataItemGajiBanbox} setiap
	 * kali sebuah komponen dipilih operator. Field di sini adalah sisa penyalinan bentuk dari katalog,
	 * bukan fitur yang aktif.</p>
	 *
	 * @return jumlah pemakaian kumulatif; praktis selalu {@code 0} untuk kelas ini
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menetapkan penghitung pemakaian komponen ini.
	 *
	 * @param jmlDipakai jumlah pemakaian kumulatif
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan baris katalog {@link ais.database.model.payroll.ItemGaji} yang di-"ikuti" baris
	 * ini &mdash; <b>relasi terpenting kelas ini</b>.
	 *
	 * <p>FK {@code item_gaji} bersifat {@code nullable = true}. Kombinasi itu dengan
	 * {@link #getIkutiItemGaji()} menghasilkan tiga keadaan nyata:</p>
	 * <ul>
	 *   <li><b>bendera menyala + katalog terisi</b> (kasus mayoritas, dibuat auto-materialisasi):
	 *   baris berperan sebagai <i>penugasan murni</i>; seluruh label, urutan, bendera dan rumus
	 *   dipasok hidup dari katalog;</li>
	 *   <li><b>bendera mati + katalog terisi</b>: baris berperan sebagai <i>override</i>; katalog
	 *   tetap tercatat sebagai asal-usul tetapi tidak lagi memengaruhi nilai;</li>
	 *   <li><b>katalog {@code null}</b>: baris berdiri sendiri &mdash; komponen gaji yang hanya
	 *   dimiliki pegawai ini dan tidak punya padanan di katalog. Pada keadaan ini bendera
	 *   {@link #getIkutiItemGaji()} <b>tidak berpengaruh sama sekali</b> karena seluruh getter
	 *   delegasi mensyaratkan {@code itemGaji != null}.</li>
	 * </ul>
	 * <p>Getter me-resolve proxy lazy lewat {@code check(...)} (lihat
	 * {@link ais.database.model.GeneralValueObject}); karena itu ia dipanggil lebih dulu oleh
	 * kesepuluh getter delegasi sebelum mereka menguji {@code itemGaji != null}. Perhatikan biayanya:
	 * karena setiap getter delegasi memanggil method ini <i>setiap kali</i>, dan tahap terakhir
	 * {@code check(...)} dapat membuka session baru untuk object yang sudah detached, satu render
	 * pohon berisi N komponen berpotensi memicu banyak sekali resolusi berulang.</p>
	 * <p><b>Konsekuensi keadaan "katalog {@code null}" pada pemuatan pohon.</b>
	 * {@code ItemGajiPegawaiTreeModel.getChildren(...)} memakai
	 * {@code createAlias("itemGaji", "itemGaji")} &mdash; sebuah <b>INNER JOIN</b> &mdash; sedangkan
	 * {@code getChildCount(...)} tidak. Baris ber-{@code item_gaji} {@code NULL} (satu-satunya cara
	 * membuatnya adalah tombol "Item Gaji Baru" di layar, yang memang tidak pernah memanggil
	 * {@link #setItemGaji(ItemGaji)}) karena itu <b>ikut terhitung sebagai anak tetapi tidak pernah
	 * bisa dirender</b>. Laporan {@code LaporanItemGajiPegawai} bahkan memanggil
	 * {@code getItemGaji().getAkunDebet()} tanpa penjagaan {@code null}, sehingga baris seperti itu
	 * memicu {@code NullPointerException} saat dicetak.</p>
	 *
	 * @return baris katalog yang diikuti, atau {@code null} bila komponen ini khusus milik pegawai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_gaji", nullable = true)
	public ItemGaji getItemGaji() {
		itemGaji = check(itemGaji);
		return itemGaji;
	}

	/**
	 * Menetapkan baris katalog yang diikuti baris ini.
	 *
	 * <p>Diisi {@code ItemGajiPegawaiTreeModel.copyByItemGaji(...)} saat pohon dibangun dari katalog.
	 * Tidak ada penjaga yang memastikan katalog yang ditunjuk berasal dari
	 * {@link #getFormatItemGaji()} yang sama &mdash; mengarahkannya ke katalog format lain akan
	 * membuat baris ini menampilkan label/rumus milik skema slip yang berbeda.</p>
	 *
	 * @param itemGaji baris katalog, atau {@code null} untuk menjadikan komponen ini berdiri sendiri
	 */
	public void setItemGaji(ItemGaji itemGaji) {
		this.itemGaji = itemGaji;
	}

	/**
	 * Mengembalikan pegawai penerima komponen gaji ini &mdash; properti yang membedakan lapis ini
	 * dari katalog {@link ais.database.model.payroll.ItemGaji} (yang tidak punya kolom
	 * {@code pegawai} sama sekali).
	 *
	 * <p>Meskipun secara skema FK {@code pegawai} ditandai {@code nullable = true}, seluruh jalur
	 * baca/tulis nyata memperlakukannya sebagai wajib: {@code ItemGajiPegawaiTreeModel} selalu
	 * menyaring {@code Restrictions.eq("pegawai", pegawai)} pada setiap query pohon, penghitungan
	 * anak, penyalinan, maupun penghapusan mentah. Baris ber-{@code pegawai} {@code NULL} karenanya
	 * tidak akan pernah muncul di layar manapun.</p>
	 * <p><b>Catatan keamanan.</b> Properti inilah yang membuat entity ini masuk cakupan
	 * {@code task_7b6038ac}: nama {@code pegawai} tidak ada pada whitelist 12 nama properti relasi
	 * yang dipakai {@code GenericCrudAutoEntityAdapter.scopeBindings()} untuk memasang pembatas
	 * tenant, sehingga pembatasnya kosong dan hasilnya "tanpa restriksi", bukan penolakan. Berbeda
	 * dari {@link ais.database.model.payroll.ItemGaji} (yang tidak punya properti ini sama sekali),
	 * untuk entity ini menambahkan {@code pegawai} ke whitelist memang relevan &mdash; walau tetap
	 * tidak cukup selama syarat peran pada whitelist "aktor" belum diperbaiki. Lihat Javadoc kelas.</p>
	 *
	 * @return pegawai penerima, sudah ter-resolve dari proxy lazy bila memungkinkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan pegawai penerima komponen gaji ini.
	 *
	 * <p>Diisi {@code ItemGajiPegawaiTreeModel.copyByItemGaji(...)} dari pegawai yang sedang dipilih
	 * di layar. Perhatikan bahwa {@code ItemGajiPegawaiAction.onSave()} <b>tidak</b> memanggil setter
	 * ini &mdash; kepemilikan baris ditentukan sepenuhnya pada saat baris dibangkitkan, bukan saat
	 * disunting.</p>
	 *
	 * @param pegawai pegawai penerima
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan bendera "ikuti data item gaji" &mdash; saklar yang menentukan apakah baris ini
	 * berperan sebagai penugasan murni (mengikuti katalog) atau sebagai override per pegawai.
	 *
	 * <p>Getter menormalkan {@code null} menjadi {@code true} <b>dan menugaskannya ke field</b>,
	 * sehingga baris lama yang kolomnya {@code NULL} akan permanen menjadi "mengikuti katalog" begitu
	 * dibaca. Pilihan default ini penting: pada instalasi yang kolomnya belum pernah diisi, seluruh
	 * baris otomatis dianggap mengikuti katalog, bukan berdiri sendiri.</p>
	 * <p>Method ini dipanggil oleh <b>sepuluh</b> getter lain di kelas ini sebagai syarat pertama
	 * delegasi, sehingga ia praktis dieksekusi pada setiap pembacaan baris. Ia sendiri tidak
	 * mendelegasikan ke katalog &mdash; bendera ini memang milik lapis pegawai, tidak pernah
	 * diturunkan dari {@link #getItemGaji()}.</p>
	 *
	 * @return {@code true} bila baris mengikuti katalog; tidak pernah {@code null}
	 */
	public Boolean getIkutiItemGaji() {
		if (ikutiItemGaji == null) {
			ikutiItemGaji = true;
		}
		return ikutiItemGaji;
	}

	/**
	 * Menetapkan bendera "ikuti data item gaji".
	 *
	 * <p>Dipanggil dari dua tempat nyata: {@code ItemGajiPegawaiTreeModel.copyByItemGaji(...)} yang
	 * selalu menetapkan {@code true} pada baris yang baru dibangkitkan, dan
	 * {@code ItemGajiPegawaiAction.onSave()} yang meneruskan status checkbox "Ikuti Data Item Gaji".</p>
	 * <p><b>Peringatan operasional:</b> mematikan bendera ini <b>tidak</b> memulihkan nilai override
	 * yang mungkin pernah ada. Karena kesepuluh getter delegasi menulis balik nilai katalog ke kolom
	 * baris ini setiap kali dibaca (lihat Javadoc kelas), yang tersisa saat bendera dimatikan adalah
	 * salinan katalog terakhir &mdash; bukan rumus/label asli pegawai.</p>
	 *
	 * @param ikutiItemGaji {@code true} untuk mengikuti katalog, {@code false} untuk melepaskan baris
	 *                      sehingga boleh punya kode/nama/rumus/urutan sendiri
	 */
	public void setIkutiItemGaji(Boolean ikutiItemGaji) {
		this.ikutiItemGaji = ikutiItemGaji;
	}

	/**
	 * Mengembalikan bendera apakah komponen ini ditampilkan pada slip gaji cetak.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif</b> dengan pola standar (normalisasi {@code null}
	 * menjadi {@code true}, lalu timpa dari katalog bila bendera menyala).</p>
	 * <p><b>Kuirk yang perlu diketahui:</b> berbeda dari sembilan getter delegasi lainnya, layar
	 * {@code ItemGajiPegawaiAction} hanya <i>menampilkan</i> properti ini sebagai kolom pohon
	 * ("Ya"/"Tidak") dan <b>tidak</b> menyediakan kotak isian untuknya &mdash; ia tidak ikut
	 * dinonaktifkan oleh listener checkbox "Ikuti Data Item Gaji", dan {@code onSave()} tidak pernah
	 * memanggil {@link #setTampilkanDiSlip(Boolean)}. Praktisnya nilai per pegawai tidak pernah bisa
	 * diatur dari layar ini; ia selalu efektif mengikuti katalog.</p>
	 * <p>Konsumennya nyata dan menentukan: {@code ItemGajiPegawaiTreeModel.populateData(...)} memakai
	 * bendera ini sebagai <b>gerbang</b> &mdash; baris yang bernilai {@code false} tidak masuk daftar
	 * slip sama sekali. Perhatikan pula bahwa lapis dokumen
	 * ({@link ais.database.model.payroll.RencanaItemGajiPegawai} dan
	 * {@link ais.database.model.payroll.PembayaranItemGajiPegawai}) membaca bendera ini
	 * <b>hidup tanpa syarat</b>, tidak pernah membekukannya seperti nominal. Rantainya karena itu
	 * hidup sampai ujung: mengubah bendera di katalog hari ini <b>mengubah tampilan slip gaji periode
	 * yang sudah dibayar dan diposting</b>.</p>
	 *
	 * @return {@code true} bila komponen tampil di slip; tidak pernah {@code null}
	 */
	public Boolean getTampilkanDiSlip() {
		if (tampilkanDiSlip == null) {
			tampilkanDiSlip = true;
		}
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			tampilkanDiSlip = itemGaji.getTampilkanDiSlip();
		}
		return tampilkanDiSlip;
	}

	/**
	 * Menetapkan bendera tampil-di-slip untuk baris ini.
	 *
	 * <p>Tidak ada pemanggil dari layar {@code ItemGajiPegawaiAction} (lihat
	 * {@link #getTampilkanDiSlip()}); setter ini praktis hanya dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param tampilkanDiSlip {@code true} agar komponen tampil pada slip cetak
	 */
	public void setTampilkanDiSlip(Boolean tampilkanDiSlip) {
		this.tampilkanDiSlip = tampilkanDiSlip;
	}

	/**
	 * Mengembalikan bendera "baris pemisah" (spasi kosong) pada slip.
	 *
	 * <p>Baris ber-{@code space} {@code true} adalah baris tata letak murni: layar melewati validasi
	 * wajib-isi Kode dan Nama untuknya, mengosongkan kotak Formula, dan membekukan sebagian besar
	 * kotak isian lain. Pada pemuatan pohon, {@code ItemGajiPegawaiTreeModel.populateData(...)}
	 * <b>melewati perhitungan</b> untuk baris seperti ini (nilainya dibiarkan kosong) tetapi tetap
	 * mencetaknya sebagai baris kosong di slip. Sama seperti {@link #getTampilkanDiSlip()}, bendera
	 * ini dibaca <b>hidup tanpa syarat</b> oleh lapis dokumen dan tidak pernah dibekukan.</p>
	 * <p><b>Kuirk implementasi &mdash; satu-satunya getter delegasi yang tidak me-refresh katalog
	 * lebih dulu.</b> Kesembilan getter delegasi lain diawali {@code itemGaji = getItemGaji();};
	 * method ini <b>langsung menguji field {@code itemGaji}</b> apa adanya. Bila baris dimuat tetapi
	 * belum ada getter lain yang menyentuh relasinya, field itu masih berupa proxy yang belum
	 * ter-resolve &mdash; atau, pada object yang dibangun manual, masih {@code null}. Akibatnya
	 * delegasi ke katalog bisa <b>terlewat secara tidak deterministik</b>, bergantung urutan getter
	 * mana yang kebetulan dipanggil lebih dulu. Perbedaan urutan lain juga ada: normalisasi
	 * {@code null}&rarr;{@code false} dijalankan <b>setelah</b> pengambilan nilai katalog, bukan
	 * sebelumnya, sehingga katalog yang bernilai {@code null} tetap dinormalkan menjadi
	 * {@code false}.</p>
	 *
	 * @return {@code true} bila baris hanyalah pemisah kosong; tidak pernah {@code null}
	 */
	public Boolean getSpace() {
		if (getIkutiItemGaji() && itemGaji != null) {
			space = itemGaji.getSpace();
		}

		if (space == null) {
			space = false;
		}
		return space;
	}

	/**
	 * Menetapkan bendera "baris pemisah" untuk baris ini.
	 *
	 * @param space {@code true} untuk menjadikan baris ini pemisah kosong tanpa kode/nama/rumus
	 */
	public void setSpace(Boolean space) {
		this.space = space;
	}

	/**
	 * Mengembalikan bendera apakah operator boleh mengetik ulang nilai variabel komponen ini pada
	 * layar pembayaran gaji.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif.</b> Berbeda dari getter bendera lain, normalisasi
	 * di sini dilakukan <b>hanya pada nilai kembali</b> ({@code null} dikembalikan sebagai
	 * {@code false}) &mdash; field-nya sendiri dibiarkan {@code null}, sehingga tidak ada penulisan
	 * balik pada kasus itu.</p>
	 * <p>Bendera ini bermakna finansial: saat menyala, layar pembayaran memecah rumus dengan
	 * {@code split(" ")} dan merender kotak isian per token, sehingga nominal yang akhirnya masuk
	 * slip (dan jurnal) bisa berbeda dari hasil rumus. Karena delegasi, menyalakannya di katalog
	 * membuka kemampuan itu untuk seluruh pegawai yang benderanya menyala sekaligus.</p>
	 *
	 * @return {@code true} bila nilai variabel boleh diubah manual; tidak pernah {@code null}
	 */
	public Boolean getNilaiVariableBisaDiubah() {
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			nilaiVariableBisaDiubah = itemGaji.getNilaiVariableBisaDiubah();
		}
		return nilaiVariableBisaDiubah == null ? false : nilaiVariableBisaDiubah;
	}

	/**
	 * Menetapkan bendera "nilai variabel bisa diubah" untuk baris ini.
	 *
	 * @param nilaiVariableBisaDiubah {@code true} agar operator boleh mengetik nilai manual di layar
	 *                                pembayaran; hanya bertahan bila {@link #getIkutiItemGaji()}
	 *                                dimatikan
	 */
	public void setNilaiVariableBisaDiubah(Boolean nilaiVariableBisaDiubah) {
		this.nilaiVariableBisaDiubah = nilaiVariableBisaDiubah;
	}

	/**
	 * Mengembalikan bendera penanda komponen "gaji final" &mdash; yaitu baris yang mewakili nilai
	 * akhir yang diterima/dibayarkan, bukan komponen penyusun.
	 *
	 * <p><b>Getter delegasi sekaligus destruktif</b>, dengan pola normalisasi hanya-pada-nilai-kembali
	 * yang sama seperti {@link #getNilaiVariableBisaDiubah()}.</p>
	 * <p>Konsumennya membawa akibat yang nyata: baik {@code PembayaranItemGajiPegawaiTreeModel} maupun
	 * layar {@code BayarGajiPegawaiAction} menyalin nilai komponen yang benderanya menyala ke
	 * {@code PembayaranGajiPunyaPegawai.setNilaiFinal(...)} &mdash; yaitu angka <i>take-home pay</i>
	 * yang dipakai dokumen pembayaran. Karena bendera ini mengikuti katalog, menyalakannya di
	 * {@link ais.database.model.payroll.ItemGaji} berarti memindahkan komponen mana yang dianggap
	 * "nilai akhir" untuk seluruh pegawai yang benderanya menyala sekaligus. Bila lebih dari satu
	 * komponen ditandai final, yang terakhir dirender yang menang &mdash; tidak ada penjaga
	 * keunikan.</p>
	 *
	 * @return {@code true} bila baris ini adalah komponen gaji final; tidak pernah {@code null}
	 */
	public Boolean getFinalGaji() {
		itemGaji = getItemGaji();
		if (getIkutiItemGaji() && itemGaji != null) {
			finalGaji = itemGaji.getFinalGaji();
		}
		return finalGaji == null ? false : finalGaji;
	}

	/**
	 * Menetapkan bendera penanda komponen "gaji final".
	 *
	 * @param finalGaji {@code true} bila baris ini mewakili nilai akhir gaji; hanya bertahan bila
	 *                  {@link #getIkutiItemGaji()} dimatikan
	 */
	public void setFinalGaji(Boolean finalGaji) {
		this.finalGaji = finalGaji;
	}
}
