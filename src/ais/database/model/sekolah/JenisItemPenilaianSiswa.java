package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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
import org.json.JSONArray;

import ais.database.model.GeneralValueObject;

/**
 * Master <b>Item Penilaian</b> pada modul sekolah &mdash; <b>butir nilai konkret</b> yang benar-benar
 * diisi guru dan benar-benar tercetak sebagai satu kolom di rapor. Dipetakan ke tabel
 * {@code sekolah.jenis_item_penilaian_siswa}.
 *
 * <p>Ini adalah <b>simpul TERAKHIR (daun) rantai penilaian siswa</b>. Tujuh lapis di atasnya hanya
 * mengelompokkan, membobot, dan mengurutkan; kelas inilah yang menyatakan <i>apa</i> yang diisi
 * (tipe isian), <i>berapa batasnya</i> ({@code nilaiMin}/{@code nilaiMax}), <i>apakah wajib</i>,
 * <i>apakah perlu lampiran</i>, dan <i>bagaimana dihitung</i> ({@code formula}). Judul dialog
 * pengelolanya adalah <i>"Tambah Item Penilaian"</i> / <i>"Ubah Item Penilaian"</i>, dengan isian
 * berlabel <i>"Kode Item Penilaian *"</i>, <i>"Nama Item Penilaian *"</i>, <i>"Menyertakan file
 * lampiran"</i>, <i>"Tampilkan isian keterangan"</i>, <i>"Tipe Data Inputan"</i>, <i>"Nilai Data
 * Inputan"</i>, <i>"Jumlah Baris"</i>, <i>"Jumlah Maksimal Teks"</i>, <i>"Nilai Maksimal"</i>,
 * <i>"Nilai Minimal"</i>, <i>"Formula"</i>, <i>"Yayasan *"</i>, <i>"Sekolah *"</i>, <i>"Kategori
 * Penilaian"</i>, <i>"Hitung rata-rata nilai siswa dalam satu kelas"</i>, <i>"Hitung rata-rata nilai
 * siswa dalam satu angkatan"</i> dan <i>"Keterangan"</i>
 * ({@code ais.action.master.sekolah.JenisItemPenilaianSiswaAction#init}).
 *
 * <h3>Posisi TERVERIFIKASI dalam rantai penilaian rapor sekolah (LENGKAP 8/8)</h3>
 * Rantai berikut dipastikan dari deklarasi kolom FK pada masing-masing entity penghubung, bukan dari
 * kemiripan nama kelas. Semua penghubung memakai pola "tabel silang + kolom {@code aktif}", bukan
 * {@code @ManyToMany}:
 * <ol>
 * <li>{@link JenisPenilaian} (tabel {@code jenis_penilaian}) &mdash; payung paling atas, sekaligus
 * <b>layar induk</b> seluruh master penilaian sekolah.</li>
 * <li>{@link DetailJenisPenilaian} (tabel {@code detail_jenis_penilaian_grup}) &mdash;
 * {@code jenisPenilaian} &harr; {@code grupPenilaian}.</li>
 * <li>{@link GrupPenilaian} (tabel {@code grup_penilaian}) &mdash; pemilik {@code formula} grup,
 * {@code jenisNilaiHuruf}, {@code adaTotal}, {@code khususTingkat}/{@code khususSemester}.</li>
 * <li>{@link DetailGrupPenilaian} (tabel {@code detail_grup_penilaian_data}) &mdash;
 * {@code grupPenilaian} &harr; {@code grupKategoriItemPenilaianSiswa}.</li>
 * <li>{@link GrupKategoriItemPenilaianSiswa} (tabel {@code grup_kategori_item_penilaian_siswa})
 * &mdash; pemilik {@code formula} kategori, {@code nilaiBolehDinputOlehGuru}, dan pembatas
 * {@code khususTingkat}/{@code khususSemester}. Lapis yang dipegang mesin hitung nilai.</li>
 * <li>{@link DetailGrupKategoriItemPenilaianSiswa} (tabel
 * {@code detail_grup_kategori_item_penilaian_siswa}) &mdash; {@code grupKategoriItemPenilaianSiswa}
 * &harr; {@code kategoriItemPenilaianSiswa}.</li>
 * <li>{@link KategoriItemPenilaianSiswa} (tabel {@code kategori_item_penilaian_siswa}) &mdash;
 * rumpun butir nilai; {@code kode}-nya adalah <b>kunci urut PRIMER</b> seluruh kolom nilai.</li>
 * <li><b>{@code JenisItemPenilaianSiswa}</b> &mdash; kelas ini, <b>DAUN</b>. Menunjuk balik ke
 * butir 7 lewat kolom FK {@code kategori_item_penilaian_siswa}
 * ({@link #getKategoriItemPenilaianSiswa()}).</li>
 * </ol>
 *
 * <p>Perhatikan asimetri penting: butir 2, 4 dan 6 adalah tabel silang eksplisit, sedangkan
 * <b>hubungan 7&rarr;8 BUKAN</b> &mdash; ia FK biasa di sisi entity ini. Konsekuensinya dijelaskan
 * pada bagian "Verifikasi bug bom-waktu" di bawah: kelas ini <b>kebal</b> terhadap mekanisme
 * "kategori hantu" karena tidak punya baris silang yang bisa dimatikan/dihidupkan ulang.
 *
 * <p><b>Keluarga ini TERPISAH dari {@link JenisNilaiSiswa}</b> (profil template cetak
 * JasperReports) dan dari {@code ais.database.model.ParameterTambahan} &mdash; lihat catatan
 * "kembaran" di bawah.
 *
 * <h3>Cara entity ini dibaca saat runtime: SATU pola, sembilan pemanggil</h3>
 * Tidak satu pun pembaca menanyakan tabel ini secara langsung untuk perhitungan. Semuanya memakai
 * pola dua langkah yang identik &mdash; hasil salin-tempel, sampai ke nama alias:
 * <ol>
 * <li>Ambil id kategori yang tercentang pada sebuah grup dari
 * {@link DetailGrupKategoriItemPenilaianSiswa} (disaring {@code isNull("aktif") OR eq("aktif",
 * true)}), dibungkus {@code ConstantValues.simpleList(..., KategoriItemPenilaianSiswa.class, false)}
 * menjadi daftar <i>stub</i> ber-{@code id} saja.</li>
 * <li>Tarik entity ini dengan {@code Restrictions.in("kategoriItemPenilaianSiswa", stub)} +
 * {@code or(isNull("aktif"), eq("aktif", true))}, <b>diurutkan
 * {@code Order.asc("kategoriItemPenilaianSiswa.kode")} lalu {@code Order.asc("nomorUrut")}</b>.</li>
 * </ol>
 *
 * <p>Sembilan pemanggil pola tersebut sudah diverifikasi: {@code ais.common.GradingHelper} (hitung
 * ulang nilai massal), {@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper} dan
 * {@code ...DetailPenilaianLesSiswaHelper} (layar isi nilai),
 * {@code ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper} (nilai ujian per
 * pertemuan), {@code ais.action.master.helper.TugasMandiriHelper} dan {@code TugasKelompokHelper}
 * (pemetaan nilai tugas e-learning),
 * {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} dan {@code LaporanRekapTotalNilai}
 * (cetak rapor/rekap), serta REST {@code ais.action.servlet.api.NilaiSiswaApi} dan
 * {@code ais.action.servlet.api.ElearningApiUtil}. Entity ini <b>hidup penuh</b>.
 *
 * <p><b>Satu penyimpangan yang layak dicatat:</b> {@code LaporanRaporSiswa} adalah satu-satunya
 * pemanggil yang menjaga daftar kategori kosong &mdash; ia memakai
 * {@code katsId.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in(...)}. Delapan
 * pemanggil lain langsung memanggil {@code Restrictions.in(...)} dengan daftar yang bisa kosong;
 * pada Hibernate/PostgreSQL hal itu menghasilkan {@code in ()} yang <b>tidak selalu</b> berperilaku
 * sama antar versi. Jangan menyalin pola tanpa penjaga tersebut ke kode baru.
 *
 * <h3>Di mana nilai siswa sesungguhnya disimpan &mdash; dan mengapa {@link #getId()} sakral</h3>
 * Nilai siswa <b>tidak</b> disimpan di rantai master ini. Ia terdenormalisasi menjadi string
 * pipa-dan-titik-koma pada kolom {@code detailNilai} milik {@code KelasSiswaPunyaSiswa} /
 * {@link KelasLesSiswaPunyaSiswa}, ditulis lewat
 * {@code VoKelasPunyaSiswa#populateDetailNilai(jenisItem, matapelajaran, grupKategori, nilai,
 * verify, semester)} dengan format per-entri:
 *
 * <pre>{@code <jenisItem.id>|<matapelajaran.id>|<nilai>|0|0|<verify>|<semester>|<grupKategori.id>}</pre>
 *
 * <p>Artinya <b>{@code id} baris entity ini adalah kunci penyimpanan seluruh nilai siswa modul
 * sekolah</b>. Tiga akibat langsung yang mudah terlewat:
 * <ul>
 * <li><b>Menghapus satu baris di sini men-yatim-kan nilai, bukan menghapusnya.</b> String
 * {@code detailNilai} tetap memuat id yang sudah tidak ada; entri itu tidak akan pernah cocok lagi
 * dan tidak tersapu oleh proses apa pun. Menonaktifkan ({@code aktif=false}) jauh lebih aman
 * daripada menghapus.</li>
 * <li><b>Membuat ulang butir yang sama menghasilkan id baru</b> &mdash; seluruh nilai lama menjadi
 * tak terjangkau meskipun kode dan namanya persis sama.</li>
 * <li><b>Kategori bukan bagian dari kunci.</b> Kuncinya {@code jenisItem} + {@code matapelajaran} +
 * {@code semester} + {@code grupKategori}. Memindahkan butir ini ke kategori lain
 * ({@link #setKategoriItemPenilaianSiswa(KategoriItemPenilaianSiswa)}) <b>tidak</b> menghilangkan
 * nilai yang sudah tersimpan &mdash; nilainya ikut pindah kolom, tanpa peringatan.</li>
 * </ul>
 *
 * <p>Perhatikan pula bahwa {@code populateDetailNilai} mengganti {@code "|"} menjadi spasi dan
 * {@code ";"} menjadi koma pada nilai yang masuk. Untuk butir bertipe {@link #TEXT} hal ini
 * <b>mengubah isi jawaban siswa/guru secara senyap</b> demi menjaga pemisah format di atas.
 *
 * <h3>{@link #getKode()}: sanitasi yang menanggung TIGA beban sekaligus</h3>
 * {@link #getKode()} membuang seluruh tanda baca ASCII kecuali {@code _} dan {@code -}, lalu
 * mengganti spasi dengan {@code _}. Terlihat kosmetik, sebenarnya <b>load-bearing</b> di tiga
 * tempat berbeda:
 * <ol>
 * <li><b>Nama parameter JasperReports.</b> {@code LaporanRaporSiswa} membentuk kunci
 * {@code gKat.getKode() + "_" + jItem.getKode()} lalu memakainya sebagai nama parameter laporan
 * ({@code ..._rata_rata_kelas}, dst). Tanda baca akan merusak nama parameter.</li>
 * <li><b>Token formula.</b> {@code GrupPenilaianUtil} mencocokkan kode ini di dalam string formula
 * (lihat bagian mesin formula di bawah).</li>
 * <li><b>Keamanan regex.</b> Substitusi formula memakai
 * {@code target.replaceAll(" " + kode + " ", ...)} &mdash; <b>{@code replaceAll} menerima
 * REGEX</b>. Semua metakarakter regex ASCII ({@code . * + ? [ ] ( ) { } | ^ $ \}) termasuk
 * {@code \p{Punct}} sehingga terbuang oleh sanitasi ini. "Merapikan" {@link #getKode()} menjadi
 * pengembali nilai mentah akan membuat kode ber-tanda-baca ditafsirkan sebagai regex.</li>
 * </ol>
 *
 * <p><b>Risiko tabrakan yang nyata:</b> karena sanitasi bersifat <i>lossy</i>, dua butir dengan kode
 * {@code "UH.1"} dan {@code "UH-1"}&hellip; berbeda ({@code -} dipertahankan), tetapi {@code "UH.1"}
 * dan {@code "UH 1"} sama-sama menjadi <b>{@code UH_1}</b>. Keduanya akan menempati kunci parameter
 * Jasper yang sama dan saling menimpa di rapor, sementara di layar master keduanya tampak sebagai
 * dua baris berbeda. Tidak ada validasi keunikan kode di mana pun.
 *
 * <p><b>Sanitasi ini juga tertulis ke database.</b> Hibernate memakai akses properti (anotasi
 * {@code @Id} ada di getter), jadi yang di-INSERT/UPDATE adalah hasil {@link #getKode()}, bukan isi
 * field. Menyimpan {@code "UH 1"} menghasilkan baris DB berisi {@code "UH_1"}, dan kode
 * {@code null} tersimpan sebagai string kosong.
 *
 * <h3>Sembilan tipe isian dan konsekuensinya</h3>
 * {@link #getTipeDataInputan()} memilih salah satu dari sembilan konstanta kelas ini. Kombinasi
 * baris dialog yang muncul dikendalikan {@code JenisItemPenilaianSiswaAction} sebagai berikut:
 * <table border="1">
 * <caption>Pengaruh tipe isian terhadap dialog dan mesin nilai</caption>
 * <tr><th>Konstanta</th><th>Baris dialog yang aktif</th><th>Catatan</th></tr>
 * <tr><td>{@link #TIDAK_ADA}</td><td>&mdash;</td><td>Butir hanya jadi label/penanda.</td></tr>
 * <tr><td>{@link #TEXT}</td><td>Jumlah Baris, Jumlah Maksimal Teks</td>
 *     <td>Isian bebas; ikut dihitung sebagai 0 pada rata-rata.</td></tr>
 * <tr><td>{@link #ANGKA}</td><td>Nilai Maksimal, Nilai Minimal</td>
 *     <td>Satu-satunya tipe yang divalidasi batas nilai.</td></tr>
 * <tr><td>{@link #TEXT_ANGKA}</td><td>&mdash;</td>
 *     <td><b>Tidak divalidasi min/max</b> meski ikut dijumlahkan.</td></tr>
 * <tr><td>{@link #TANGGAL}</td><td>&mdash;</td><td>Datebox.</td></tr>
 * <tr><td>{@link #PILIHAN_YA_TIDAK}</td><td>&mdash;</td><td>Dua opsi tetap.</td></tr>
 * <tr><td>{@link #PILIHAN_CUSTOM}</td><td>Nilai Data Inputan</td>
 *     <td>Format {@code "Ya:1;Tidak:0;Belum Tau:2"} &mdash; opsi dipisah {@code ;}, skor
 *     dipisah {@code :}.</td></tr>
 * <tr><td>{@link #PILIHAN_BANYAK}</td><td>Nilai Data Inputan</td><td>Format sama, multi-pilih.</td></tr>
 * <tr><td>{@link #FORMULA}</td><td>Formula</td><td>Nilai dihitung, bukan diinput.</td></tr>
 * </table>
 *
 * <p><b>Bug laten pada dialog:</b> {@code JenisItemPenilaianSiswaAction#init(...)} menguji
 * visibilitas baris <i>Nilai Maksimal</i>/<i>Nilai Minimal</i> dengan
 * {@code ParameterTambahan.ANGKA} dan baris <i>Jumlah Maksimal Teks</i> dengan
 * {@code ParameterTambahan.TEXT} &mdash; konstanta milik <b>kelas lain</b>
 * ({@code ais.database.model.ParameterTambahanAstract}), bukan {@link #ANGKA}/{@link #TEXT} milik
 * kelas ini. Saat ini nilainya kebetulan sama persis sehingga bekerja. Mengubah salah satu string
 * di salah satu kelas akan membuat ketiga baris itu <b>tidak pernah muncul lagi</b>, tanpa error.
 *
 * <h3>Kembaran tak-sedarah: {@code ParameterTambahan}</h3>
 * Sembilan konstanta, dan hampir seluruh field perilaku isian ({@code tipeDataInputan},
 * {@code nilaiDataInputan}, {@code harusMenyertakanLampiran}, {@code lampiranWajibDiisi},
 * {@code tampilkanIsianKeterangan}, {@code labelInputanKeterangan}, {@code jumlahBaris},
 * {@code jumlahText}, {@code nilaiMin}, {@code nilaiMax}, {@code hanyaTampilDiAdmin},
 * {@code kodeAdminYgBoleh}) adalah <b>duplikat verbatim</b> dari
 * {@code ais.database.model.ParameterTambahanAstract}/{@code ParameterTambahan} &mdash; keluarga
 * "parameter tambahan" generik yang dipakai belasan modul lain. Perbedaannya:
 * <ul>
 * <li>Kelas ini <b>menambah</b> {@link #FORMULA}; {@code ParameterTambahanAstract} tidak
 * memilikinya.</li>
 * <li>Kelas ini <b>tidak</b> memiliki {@code TANGGAL_DAN_WAKTU} maupun {@code WAKTU} yang ada di
 * sana.</li>
 * <li>Tidak ada pewarisan, interface bersama, maupun konstanta bersama di antara keduanya.</li>
 * </ul>
 * Jangan menganggap keduanya dapat dipertukarkan, dan jangan "menyatukan" konstanta tanpa
 * menelusuri seluruh pemanggil kedua keluarga.
 *
 * <h3>Mesin formula dan cache preload seluruh instalasi</h3>
 * Untuk butir bertipe {@link #FORMULA}, {@link #getFormula()} menyimpan {@code JSONArray} (string,
 * kolom {@code text}) yang diurai {@code ais.action.master.sekolah.util.GrupPenilaianUtil}.
 * Substitusinya bekerja begini:
 * <ul>
 * <li>Sumber daftar butir bukan query, melainkan <b>cache preload memori</b>
 * {@code ConstantValues.ambilBerdasarClass(JenisItemPenilaianSiswa.class)}.</li>
 * <li>Cache itu berisi <b>SELURUH baris tabel ini pada instalasi</b>, tanpa penyaring sekolah
 * maupun yayasan. Kelas ini terdaftar di {@code ais.common.DataUtil.CLASS_JANGAN_DIBERSIHKAN}
 * (baris 251) sehingga {@code InitDataHelper#doInitData} <b>selalu</b> memuatnya penuh, melewati
 * ambang "tabel terlalu besar" yang membatasi kelas lain. Sisi baiknya: bug batas-baris-preload
 * yang tercatat pada {@code PembinaSiswa} <b>tidak</b> berlaku di sini. Sisi buruknya: substitusi
 * formula mencocokkan {@link #getKode()} lintas seluruh instalasi.</li>
 * <li>Pencocokannya {@code StringUtils.contains(target, " " + kode + " ")} lalu
 * {@code target.replaceAll(" " + kode + " ", " " + nilai + " ")} &mdash; kode <b>wajib diapit
 * spasi</b> di dalam formula. Karena {@code replaceAll} ikut memakan kedua spasi pengapit, dua
 * kode yang bersebelahan tanpa operator ({@code " a b "}) hanya tersubstitusi yang pertama.</li>
 * <li>Filter kategori tetap diterapkan sebelum substitusi, jadi kode dari kategori yang tidak
 * relevan tidak ikut tersubstitusi &mdash; tetapi <b>kesamaan kode antar sekolah</b> tetap menjadi
 * risiko begitu sebuah kategori "hantu" milik sekolah lain ikut aktif (lihat
 * {@link DetailGrupKategoriItemPenilaianSiswa}).</li>
 * </ul>
 *
 * <h3>Verifikasi bug bom-waktu "aktif"/"kategori hantu" pada simpul DAUN ini</h3>
 * <ul>
 * <li><b>Mekanisme hantu sendiri &mdash; TIDAK ADA (verifikasi NEGATIF).</b> Bug b51/54/61 lahir
 * dari pola "{@code onSave} mematikan SELURUH baris tabel silang lalu menghidupkan ulang hanya yang
 * ada di peta". Entity ini <b>tidak punya tabel silang</b>: keanggotaannya pada kategori adalah FK
 * langsung ({@code kategori_item_penilaian_siswa}) yang hanya berubah lewat dialog Ubah butir ini
 * sendiri. Tidak ada layar mana pun yang menyimpan-ulang "daftar butir milik kategori", sehingga
 * tidak ada baris yang bisa dimatikan diam-diam oleh penyimpanan parent.</li>
 * <li><b>Efek WARISAN &mdash; ADA, dan justru paling terasa di sini.</b> Kelas ini adalah
 * <i>korban</i>, bukan pelaku. Begitu satu {@link KategoriItemPenilaianSiswa} lenyap dari peta
 * (bug b51/54) atau satu {@link GrupKategoriItemPenilaianSiswa} ikut mati (bug b61), langkah 2
 * pola baca di atas tidak pernah menyertakan id kategori itu &mdash; <b>seluruh butir nilai di
 * bawahnya raib dari layar isi nilai, dari rapor, dari rekap dan dari API</b>, padahal kolom
 * {@code aktif} baris-baris ini tetap {@code true}. Layar master di sini akan tetap menampilkannya
 * sebagai "Aktif". Inilah alasan gejala bug tersebut sulit dilacak: <b>bukti kerusakan ada di
 * lapis 6, gejalanya muncul di lapis 8, dan lapis 8 tampak sehat.</b></li>
 * <li><b>Jalur kebalikan &mdash; TIDAK ADA.</b> Menghapus atau menonaktifkan butir di sini tidak
 * pernah mengubah baris kategori/grup di atasnya. Tidak ada {@code cascade REMOVE} sama sekali
 * (hanya {@code PERSIST}/{@code MERGE} pada ketiga relasi).</li>
 * </ul>
 *
 * <h3>Saklar {@code aktif}</h3>
 * <ul>
 * <li>Dialog <i>Tambah/Ubah</i> <b>tidak pernah menulis</b> {@code aktif} &mdash;
 * {@code onSave(...)} hanya menulis kode, nama, nilai inputan, tipe, label, keterangan, label
 * keterangan, jumlah baris, nilai min/max, sekolah, yayasan, formula, kedua flag rata-rata, dan
 * jumlah teks. Kolom {@code aktif} hanya berubah lewat checkbox <i>"Aktif"</i> pada grid daftar
 * atau lewat unggah Excel.</li>
 * <li>Yang menyelamatkan keadaan adalah nilai bawaan {@code true} dari {@link #getAktif()}: karena
 * Hibernate membaca lewat getter, kolomnya selalu tersimpan terisi. <b>Ketergantungan tersembunyi
 * yang sama seperti pada {@link KategoriItemPenilaianSiswa}</b> &mdash; "membersihkan"
 * {@link #getAktif()} menjadi pengembali nilai mentah akan menghidupkan cabang {@code isNull} di
 * sembilan query pemanggil sekaligus.</li>
 * <li>{@code GrupPenilaianUtil#ambilPoint(...)} menyaring {@code jenisItemPenilaianSiswa.getAktif()}
 * langsung pada objek cache; menonaktifkan butir bertipe {@link #FORMULA} membuat kodenya berhenti
 * tersubstitusi &mdash; formula yang memakainya akan menghitung dengan token yang tidak tergantikan
 * (hasil bisa 0.0 senyap), bukan gagal dengan error.</li>
 * </ul>
 *
 * <h3>Hak akses: BROKEN ACCESS CONTROL &mdash; hak BACA cukup untuk CRUD penuh</h3>
 * Layar CRUD-nya {@code /pages/master/sekolah/jenis_item_penilaian_siswa.zul}
 * ({@code JenisItemPenilaianSiswaAction}). Layar itu <b>tidak terdaftar sebagai menu mandiri</b>:
 * satu-satunya penyisipannya di seluruh repo adalah {@code MyInclude} di
 * {@code JenisPenilaianAction} (baris 123), yaitu tab di dalam layar <i>Jenis Penilaian</i>.
 * Konsekuensinya bertumpuk:
 * <ul>
 * <li><b>Nol {@code checkPrevilages}.</b> Berbeda dari saudaranya
 * {@code KategoriItemPenilaianSiswaAction} yang memanggil
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} dengan benar,
 * {@code JenisItemPenilaianSiswaAction} <b>tidak memanggilnya sama sekali</b>:
 * {@code private boolean edit = true; private boolean delete = true;} di-<i>hardcode</i>.
 * {@code doBeforeCompose} hanya memanggil {@code Common.doCheckSecurity()} (cek login), bukan cek
 * hak.</li>
 * <li>Akibatnya <b>seluruh</b> kontrol berjalan tanpa gerbang: tombol <i>Tambah</i> (tanpa atribut
 * visibilitas apa pun di ZUL), tombol <i>Ubah</i>/<i>Hapus</i> per baris
 * ({@code Common.copyEditDeleteButtons(edit, delete, ...)} dengan kedua argumen selalu
 * {@code true}), checkbox <i>Aktif</i> ({@code setDisabled(!edit)} &rarr; tak pernah nonaktif),
 * checkbox <i>Isian Wajib</i>, <i>Lampiran Wajib</i>, <i>Hanya Admin</i>, {@code Intbox} nomor
 * urut, {@code Textbox} kode admin, tombol <i>Cetak</i>, dan <b>tombol Unggah Excel</b>
 * ({@code Common.uploadData(this, JenisItemPenilaianSiswa.class, contents)} dengan
 * {@code setVisible(edit && delete)} yang selalu benar).</li>
 * <li><b>Tanpa penyaring tenant.</b> {@code initCriteria(boolean)} hanya menambah pembatas
 * sekolah/yayasan bila combo pencarian kebetulan terisi; bila "=Semua=" dipilih ia memakai
 * {@code Restrictions.sqlRestriction("1=1")}. Bahkan saat terisi, pembatasnya
 * {@code isNull("sekolah") OR eq("sekolah", s)} &mdash; sengaja fail-open untuk baris global.</li>
 * <li><b>Daftar kolom unggah memuat {@code "id"}</b> ({@code JenisItemPenilaianSiswaAction#contents}).
 * Digabung dengan tiga butir di atas: seorang pengguna yang hanya diberi hak BACA pada menu
 * <i>Jenis Penilaian</i> dapat mengunggah satu berkas Excel yang menimpa baris mana pun di
 * <b>seluruh instalasi</b> berdasarkan id &mdash; termasuk mengubah {@code nilaiMax},
 * {@code formula}, atau {@code kode} butir nilai milik sekolah lain.</li>
 * </ul>
 * Ini <b>pewarisan hak lewat menu induk</b> dalam bentuk terburuknya: bukan sekadar hak menu
 * bernilai rendah yang mewarisi hak menu bernilai tinggi, melainkan hak menu apa pun yang mewarisi
 * <b>ketiadaan gerbang</b>. Perbaikan minimal adalah menambahkan tiga panggilan
 * {@code CommonPrivilages.checkPrevilages(...)} yang sudah dipakai kelas saudaranya.
 *
 * <p>Catatan hak kedua, tingkat isian: {@link #getHanyaTampilDiAdmin()} +
 * {@link #getKodeAdminYgBoleh()} membekukan sub-baris isian nilai lewat
 * {@code Common.freeze(subRows, !Common.getApakahAdmin(kode))} di
 * {@code DetailPenilaianSiswaHelper}. Ini <b>pembekuan UI, bukan gerbang server</b>; alur simpan
 * di baliknya tidak memeriksa ulang kode admin.
 *
 * <h3>Verifikasi pola arsitektur berulang milik repo ini</h3>
 * <ul>
 * <li><b>Getter destruktif/write-back &mdash; ADA, empat tingkat keparahan.</b>
 *   <ol>
 *   <li><i>Ringan (isi ulang default):</i> {@link #getTipeDataInputan()},
 *   {@link #getHarusMenyertakanLampiran()}, {@link #getLabelInputan()},
 *   {@link #getNilaiDataInputan()}, {@link #getAktif()}, {@link #getNomorUrut()},
 *   {@link #getWajibDiisi()}, {@link #getLampiranWajibDiisi()},
 *   {@link #getHanyaTampilDiAdmin()} &mdash; menulis nilai bawaan ke field saat {@code null},
 *   sehingga kolom NULL berubah menjadi terisi begitu baris tersentuh.</li>
 *   <li><i>Ringan (de-proxy):</i> {@link #getSekolah()} dan
 *   {@link #getKategoriItemPenilaianSiswa()} menulis balik hasil {@code check(...)}.</li>
 *   <li><i>Sedang (turunan silang):</i> {@link #getYayasan()} menimpa field {@code sekolah}
 *   <i>dan</i> menurunkan ulang {@code yayasan} dari {@code sekolah.getYayasan()} pada setiap
 *   pembacaan.</li>
 *   <li><b><i>Berat (penghapusan permanen):</i> {@link #getKodeAdminYgBoleh()}</b> &mdash; bila
 *   {@link #getHanyaTampilDiAdmin()} bernilai {@code false}, getter ini <b>menulis {@code ""} ke
 *   field</b>. Daftar putih kode admin yang pernah dikonfigurasi hilang permanen begitu centang
 *   <i>"Hanya Admin"</i> dilepas dan baris tersimpan; mencentangnya kembali <b>tidak</b>
 *   memulihkan daftar itu, dan gerbangnya jatuh ke "hanya peran ADMINISTRATOR"
 *   ({@code CommonCurrentSessionHelper#getApakahAdmin(String)} baris 1003-1004). Renderer grid
 *   memanggil getter ini untuk <b>setiap baris</b> tanpa syarat, dan setiap checkbox di baris yang
 *   sama memanggil {@code Common.refreshSaveOrUpdate(...)} &mdash; jadi pemicunya cukup
 *   "buka layar, centang apa saja".</li>
 *   </ol></li>
 * <li><b>{@code getNomorUrut()} non-null yang meruntuhkan {@code TreeSet} &mdash; PRASYARAT ADA,
 * DAMPAK BELUM TERJADI.</b> {@link #getNomorUrut()} tidak pernah mengembalikan {@code null} (bawaan
 * {@code 1}), dan {@link #compareTo(GeneralValueObject)} di-override hanya membandingkan
 * {@code nomorUrut}. Seluruh butir yang belum diberi nomor urut karena itu <b>saling setara</b>
 * menurut {@code compareTo} &mdash; sebuah {@code TreeSet}/{@code TreeMap} berkunci entity ini akan
 * menciut menjadi satu elemen. Penelusuran sembilan pemanggil menunjukkan semuanya memakai
 * {@code List} dari {@code ConstantValues.simpleList(...)} dengan pengurutan dikerjakan SQL, jadi
 * <b>saat ini tidak ada kerusakan aktif</b> &mdash; berbeda dari {@code SkalaKegiatanKesiswaan}
 * (b59) yang bugnya sudah nyata. Perlakukan ini sebagai ranjau: jangan pernah memasukkan entity ini
 * ke koleksi terurut tanpa comparator eksplisit.</li>
 * <li><b>{@code compareTo()} dipangkas &mdash; ADA.</b> Override di sini membuang tiga cabang
 * cadangan milik induk ({@code nim}, {@code nama}, {@code keterangan}) dan menambahkan cast
 * {@code (JenisItemPenilaianSiswa) arg0} tanpa {@code instanceof}. Membandingkan dengan entity
 * jenis lain melempar {@code ClassCastException}, padahal implementasi induk akan menanganinya
 * dengan tenang.</li>
 * <li><b>Fail-open cakupan tenant &mdash; ADA, dua bentuk.</b> Pada layar master
 * ({@code initCriteria}) berupa {@code sqlRestriction("1=1")}; pada cache preload berupa
 * ketiadaan penyaring sama sekali. Lihat bagian hak akses dan mesin formula.</li>
 * <li><b>Pewarisan hak lewat menu induk &mdash; ADA</b> (menu <i>Jenis Penilaian</i>), dengan
 * pemberat berupa nol {@code checkPrevilages}. Lihat bagian hak akses.</li>
 * <li><b>{@code getKeterangan()} tidak dipetakan &mdash; TIDAK ADA (verifikasi NEGATIF).</b> Bug
 * b56/b60/b61 (properti {@code keterangan} kehilangan {@code @Column} sehingga tidak pernah
 * tersimpan) <b>tidak berlaku</b> di sini: {@link #getKeterangan()} beranotasi
 * {@code @Column(name = "keterangan")} dengan benar, dan {@code onSave(...)} menulisnya.</li>
 * <li><b>Batas preload {@code ConstantValues} 100 baris &mdash; TIDAK BERLAKU (verifikasi
 * NEGATIF).</b> Kelas ini terdaftar di {@code CLASS_JANGAN_DIBERSIHKAN}, sehingga cabang
 * {@code GeneralValueObject.merupakanJanganDibersihkan(clazz)} pada
 * {@code InitDataHelper#doInitData} membuatnya selalu di-full-load. Bug {@code PembinaSiswa} (b58)
 * tidak menular ke sini.</li>
 * </ul>
 *
 * <h3>Hal non-obvious lain sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate
 * TIDAK memetakan properti apa pun miliknya. Maka {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan juga {@code kode}/{@code nama}/{@code keterangan} <b>harus</b>
 * dideklarasikan ulang di sini agar terpetakan. Ini KEHARUSAN TEKNIS, bukan duplikasi yang bisa
 * "dirapikan".</li>
 * <li><b>Akibat sampingannya: field induk selamanya kosong.</b> Field lokal
 * {@code kode}/{@code nama}/{@code keterangan} membayangi ({@code shadow}) field bernama sama di
 * induk. Kode apa pun yang membaca field induk secara langsung (bukan lewat getter) akan mendapat
 * {@code null}. Saat ini aman karena {@code GeneralValueObject#toString()} dan
 * {@code #compareTo(...)} mengaksesnya lewat getter.</li>
 * <li><b>{@code toString()} tidak di-override.</b> Bentuknya
 * {@code getKode() + " - " + getNama()} dari induk. Karena {@link #getKode()} tidak pernah
 * {@code null}, cabang {@code ""} pada induk tidak pernah tercapai &mdash; butir tanpa kode tetap
 * tercetak sebagai {@code " - Nama"} dengan pemisah menggantung.</li>
 * <li><b>Tiga kontrak null yang berbeda dalam satu kelas.</b> {@link #getKode()} dan
 * {@link #getNama()} tidak pernah mengembalikan {@code null} (mengembalikan {@code ""}), sedangkan
 * {@link #getKeterangan()} mengembalikan nilai mentah tanpa {@code trim} maupun penjaga null &mdash;
 * membalik jaminan non-null milik induk, sama seperti pada {@link KategoriItemPenilaianSiswa}.</li>
 * <li><b>{@link #DEFAULT_FORMULA} bukan {@code final}.</b> Ia {@code public static String} biasa
 * yang diinisialisasi sekali dari {@code new JSONArray().toString()} (yaitu {@code "[]"}). Kode
 * mana pun dapat menimpanya saat runtime dan mengubah formula bawaan seluruh butir yang belum
 * disetel, secara global untuk satu JVM.</li>
 * <li><b>Empat entity lain menunjuk ke tabel ini dengan FK bernama sama.</b>
 * {@code ais.database.model.Pertemuan}, {@code ais.database.model.PertemuanPunyaUjian},
 * {@code ais.database.model.TugasPertemuan} dan {@code ais.database.model.TugasKelompok} semuanya
 * memiliki {@code @JoinColumn(name = "jenis_item_penilaian_siswa", nullable = true)} &mdash; jalur
 * pemetaan nilai e-learning ke butir rapor. Penghapusan baris di sini juga memutus keempat relasi
 * itu.</li>
 * <li><b>Ada scaffold UI baru yang belum berisi apa-apa.</b>
 * {@code /WEB-INF/new/sekolah/uiux/jenis_item_penilaian_siswa.jsp} dan
 * {@code .../services/jenis_item_penilaian_siswa_service.jsp} dihasilkan otomatis oleh
 * {@code generate_new_jsp_scaffold.py} (6 Agu 2026) dan hanya menyetel atribut request lalu
 * mendelegasikan ke {@code dispatcher.jsp}. Tidak ada akses data di dalamnya; bukan endpoint.</li>
 * <li><b>Terlindung dari pembersihan cache.</b> Karena terdaftar di
 * {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}, instance-nya tidak dibuang pembersihan berkala dan
 * sering sudah <i>detached</i> &mdash; inilah alasan ketiga getter relasi memanggil
 * {@code GeneralValueObject#check(Object)}. Kelasnya juga disiapkan saat boot lewat
 * {@code ais.common.InitData} (baris 617), tetapi <b>tanpa penyemaian baris</b>: instalasi baru
 * mulai dengan tabel kosong, tidak ada butir nilai bawaan.</li>
 * <li><b>{@code @Audited} (Envers) aktif.</b> Setiap perubahan tercatat ke skema audit; tombol
 * <i>Revisi</i> pada grid ({@code RevisiHelper.createNewRevisi(JenisItemPenilaianSiswa.class, ...)})
 * membaca riwayat itu. Operasi massal berbasis SQL/HQL bulk akan melewati Envers.</li>
 * </ul>
 *
 * @see KategoriItemPenilaianSiswa
 * @see GrupKategoriItemPenilaianSiswa
 * @see DetailGrupKategoriItemPenilaianSiswa
 * @see GrupPenilaian
 * @see DetailGrupPenilaian
 * @see JenisPenilaian
 * @see DetailJenisPenilaian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "jenis_item_penilaian_siswa", schema = "sekolah")
public class JenisItemPenilaianSiswa extends GeneralValueObject {

	/**
	 * Tipe isian: butir ini <b>tidak meminta data apa pun</b> dari guru; perannya sekadar label
	 * atau penanda di formulir penilaian.
	 *
	 * <p>Nilai literalnya identik dengan {@code ParameterTambahanAstract.TIDAK_ADA}, tetapi kedua
	 * konstanta itu tidak berhubungan &mdash; lihat catatan "kembaran" pada dokumentasi kelas.</p>
	 */
	public static final String TIDAK_ADA = "Tidak ada data yang diinput";

	/**
	 * Tipe isian teks bebas. Mengaktifkan baris dialog <i>"Jumlah Baris"</i>
	 * ({@link #getJumlahBaris()}) dan <i>"Jumlah Maksimal Teks"</i> ({@link #getJumlahText()}).
	 *
	 * <p><b>Peringatan penyimpanan:</b> nilai bertipe teks tetap disimpan ke string
	 * {@code detailNilai} yang memakai {@code |} dan {@code ;} sebagai pemisah, sehingga kedua
	 * karakter itu diganti spasi/koma oleh {@code VoKelasPunyaSiswa#populateDetailNilai(...)}.</p>
	 */
	public static final String TEXT = "Berupa teks";

	/**
	 * Tipe isian numerik. <b>Satu-satunya tipe yang divalidasi</b> terhadap
	 * {@link #getNilaiMin()}/{@link #getNilaiMax()} oleh {@code DetailPenilaianSiswaHelper}
	 * (dan padanan Les-nya); mengaktifkan kedua baris dialog tersebut.
	 */
	public static final String ANGKA = "Berupa numerik / angka";

	/**
	 * Tipe isian campuran teks dan angka. Ikut dijumlahkan bila isinya berupa angka, tetapi
	 * <b>tidak</b> divalidasi terhadap {@link #getNilaiMin()}/{@link #getNilaiMax()} &mdash;
	 * asimetri yang disengaja atau tidak, tetapi nyata di kode helper penilaian.
	 */
	public static final String TEXT_ANGKA = "Berupa teks / angka";

	/** Tipe isian tanggal; dirender sebagai datebox pada layar isi nilai. */
	public static final String TANGGAL = "Berupa tanggal";

	/** Tipe isian dua pilihan tetap (ya/tidak); tidak memakai {@link #getNilaiDataInputan()}. */
	public static final String PILIHAN_YA_TIDAK = "Berupa pilihan ya/tidak";

	/**
	 * Tipe isian satu-pilihan dengan daftar opsi yang ditentukan admin. Opsi dan skornya diambil
	 * dari {@link #getNilaiDataInputan()} dengan format
	 * <i>{@code opsi:skor;opsi:skor;...}</i> &mdash; contoh dari teks bantuan dialog:
	 * {@code Ya:1;Tidak:0;Belum Tau:2}. Skor harus berupa angka desimal.
	 *
	 * <p>Ini juga nilai <b>bawaan</b> yang ditulis {@link #getTipeDataInputan()} saat tipe belum
	 * pernah disetel.</p>
	 */
	public static final String PILIHAN_CUSTOM = "Berupa pilihan custom";

	/**
	 * Tipe isian banyak-pilihan; memakai format daftar opsi yang sama persis dengan
	 * {@link #PILIHAN_CUSTOM} pada {@link #getNilaiDataInputan()}.
	 */
	public static final String PILIHAN_BANYAK = "Berupa banyak pilihan";

	/**
	 * Tipe isian terhitung: nilainya tidak diinput guru melainkan dievaluasi dari
	 * {@link #getFormula()} oleh {@code ais.action.master.sekolah.util.GrupPenilaianUtil}.
	 *
	 * <p>Konstanta ini <b>tidak ada</b> pada keluarga {@code ParameterTambahanAstract} &mdash;
	 * inilah pembeda utama kedua keluarga konstanta yang selebihnya identik.</p>
	 */
	public static final String FORMULA = "Berupa formula";
	/**
	 *
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Primary key {@code sekolah.jenis_item_penilaian_siswa.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir, diisi otomatis oleh interceptor audit. */
	private String oleh;
	/** Id pengguna pengubah terakhir, diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila baris belum pernah ter-UPDATE
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> {@code null} maupun string kosong/spasi
	 * diabaikan tanpa error, sehingga jejak audit yang sudah ada tidak tertimpa nilai hampa.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan aturan penolakan nilai kosong yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila baris belum pernah ter-UPDATE
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} plus deklarasi field {@code tanggal_dirubah} &mdash; keduanya
	 * sengaja berada pada satu baris fisik (gaya generator repo ini); jangan dipisah tanpa
	 * memeriksa alat yang menyisipkannya.
	 *
	 * <p>{@code onUpdate()} dipanggil kontainer persistence TEPAT SEBELUM pernyataan UPDATE
	 * dieksekusi, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif serta memperbarui
	 * {@code tanggal_dirubah}.</p>
	 *
	 * <p><b>Hanya UPDATE.</b> Tidak ada {@code @PrePersist}, sehingga baris yang baru dibuat
	 * mengandalkan nilai awal field {@code tanggal_dirubah} yang disetel
	 * {@code ais.ui.util.WaktuUtil.getDate()} pada saat object Java dibuat &mdash; yaitu waktu
	 * pembuatan instance, bukan waktu INSERT.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya tidak dipanggil manual &mdash;
	 * {@link #onUpdate()} sudah mengurusnya.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance baru karena field
	 *         diinisialisasi saat object dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan bebas; dipetakan ke kolom {@code keterangan}. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kode butir; disanitasi saat dibaca/disimpan. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama butir; disalin dari isian <i>"Nama Item Penilaian *"</i> bersama {@code labelInputan}. */
	private String nama;
	/** Salah satu dari sembilan konstanta tipe isian kelas ini. Lihat {@link #getTipeDataInputan()}. */
	private String tipeDataInputan;
	/** Daftar opsi+skor untuk tipe pilihan. Lihat {@link #getNilaiDataInputan()}. */
	private String nilaiDataInputan;
	/** Apakah isian butir ini disertai unggahan berkas lampiran. */
	private Boolean harusMenyertakanLampiran;
	/** Apakah kotak keterangan tambahan ditampilkan di samping isian nilai. */
	private Boolean tampilkanIsianKeterangan;
	/** Label yang tampil di formulir isi nilai &mdash; ini yang dilihat guru, bukan {@code nama}. */
	private String labelInputan;
	/** Label kotak keterangan tambahan; berbawaan {@code "Keterangan"}. */
	private String labelInputanKeterangan;
	/** Apakah butir ini wajib diisi. */
	private Boolean wajibDiisi;
	/** Apakah butir hanya boleh dilihat/diubah oleh admin tertentu. */
	private Boolean hanyaTampilDiAdmin;
	/** Daftar kode peran admin (dipisah koma) yang boleh mengubah. Lihat {@link #getKodeAdminYgBoleh()}. */
	private String kodeAdminYgBoleh;
	/** Apakah lampiran wajib diunggah bila {@code harusMenyertakanLampiran} bernilai benar. */
	private Boolean lampiranWajibDiisi;
	/** Urutan tampil butir dalam satu kategori; kunci urut SEKUNDER seluruh kolom nilai. */
	private Integer nomorUrut;
	/** Tinggi kotak teks (baris) untuk tipe {@link #TEXT}. */
	private Integer jumlahBaris;

	/** Cakupan sekolah; {@code null} berarti berlaku untuk semua sekolah. */
	private Sekolah sekolah;
	/** Cakupan yayasan; diturunkan ulang dari {@code sekolah} pada setiap pembacaan. */
	private Yayasan yayasan;

	/** Saklar aktif; lihat catatan bom-waktu pada dokumentasi kelas dan {@link #getAktif()}. */
	private Boolean aktif;
	/** Rumpun induk butir ini &mdash; simpul ke-7 rantai penilaian. */
	private KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa;
	/** Formula (JSON) untuk tipe {@link #FORMULA}. Lihat {@link #getFormula()}. */
	private String formula;
	/** Batas atas nilai untuk tipe {@link #ANGKA}; bawaan {@code 100.0}. */
	private Double nilaiMax;
	/** Batas bawah nilai untuk tipe {@link #ANGKA}; bawaan {@code 0.0}. */
	private Double nilaiMin;

	/** Apakah rapor menghitung rata-rata butir ini se-kelas. */
	private Boolean hitungRataRataKelas;
	/** Apakah rapor menghitung rata-rata butir ini se-angkatan. */
	private Boolean hitungRataRataAngkatan;
	/** Panjang maksimum teks untuk tipe {@link #TEXT}; bawaan {@code 100000}. */
	private Integer jumlahText;

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk membuat
	 * instance saat hidrasi entity dari hasil query; juga dipakai
	 * {@code JenisItemPenilaianSiswaAction#onAdd(Event)} untuk membuka dialog <i>Tambah</i>.
	 */
	public JenisItemPenilaianSiswa() {
	}

	/**
	 * Constructor pintas untuk membuat object "penunjuk" berisi id dan nama saja.
	 *
	 * <p>Perhatikan parameternya bertipe {@code long} primitif, sehingga tidak dapat dipakai untuk
	 * membuat instance ber-id {@code null}. Tidak ada pemanggil di dalam repo saat ini.</p>
	 *
	 * @param id   primary key yang langsung disetel
	 * @param nama nama butir; disetel langsung ke field tanpa validasi
	 */
	public JenisItemPenilaianSiswa(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p><b>Nilai ini adalah kunci penyimpanan seluruh nilai siswa modul sekolah</b> &mdash; ia
	 * menjadi segmen pertama setiap entri string {@code detailNilai} pada
	 * {@code KelasSiswaPunyaSiswa}/{@link KelasLesSiswaPunyaSiswa} (lihat
	 * {@code VoKelasPunyaSiswa#populateDetailNilai(...)}). Konsekuensi penghapusan/pembuatan ulang
	 * baris diuraikan pada dokumentasi kelas.</p>
	 *
	 * <p>Kolom dideklarasikan {@code insertable = false} karena diisi sequence/identity oleh
	 * PostgreSQL.</p>
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key secara manual. Umumnya hanya dipakai untuk membuat object penunjuk.
	 *
	 * @param id primary key; boleh {@code null}
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode butir dalam bentuk yang <b>sudah disanitasi</b>: seluruh tanda baca ASCII
	 * ({@code \p{Punct}}) dibuang kecuali {@code _} dan {@code -}, hasilnya di-{@code trim}, lalu
	 * setiap spasi diganti {@code _}. Kode {@code null} menjadi string kosong.
	 *
	 * <p><b>Sanitasi ini menanggung tiga beban sekaligus</b> (uraian lengkap pada dokumentasi
	 * kelas): nama parameter JasperReports pada {@code LaporanRaporSiswa}, token substitusi formula
	 * pada {@code GrupPenilaianUtil}, dan keamanan regex karena substitusi itu memakai
	 * {@code String#replaceAll(String, String)}. Jangan menyederhanakannya menjadi pengembali nilai
	 * mentah.</p>
	 *
	 * <p><b>Getter ini menulis ke database.</b> Hibernate memakai akses properti, jadi nilai yang
	 * di-INSERT/UPDATE adalah hasil sanitasi ini &mdash; bukan isi field. Field {@code kode}
	 * sendiri tidak diubah (ini bukan getter write-back), tetapi isi kolom DB akan berbeda dari apa
	 * yang diketik pengguna.</p>
	 *
	 * <p><b>Sanitasi bersifat lossy.</b> Dua kode berbeda dapat menghasilkan hasil yang sama
	 * (mis. {@code "UH.1"} dan {@code "UH 1"} sama-sama menjadi {@code "UH_1"}) dan akan saling
	 * menimpa sebagai parameter rapor. Tidak ada validasi keunikan kode di layar mana pun.</p>
	 *
	 * @return kode butir yang sudah disanitasi; tidak pernah {@code null}, bisa string kosong
	 */
	@Column(name = "kode", nullable = true)
	public String getKode() {
		return kode == null ? "" : org.apache.commons.lang3.StringUtils.replace(kode.replaceAll("[\\p{Punct}&&[^_-]]+", "").trim(), " ", "_");
	}

	/**
	 * Menyetel kode butir apa adanya, tanpa sanitasi maupun validasi.
	 *
	 * <p>Sanitasi terjadi kemudian, saat dibaca {@link #getKode()} &mdash; termasuk saat Hibernate
	 * membacanya untuk disimpan. Jadi nilai yang dikirim ke sini tidak selalu sama dengan yang
	 * berakhir di kolom DB.</p>
	 *
	 * @param kode kode butir mentah; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas butir ini, <b>apa adanya</b>.
	 *
	 * <p><b>Membalik kontrak induk:</b> {@code GeneralValueObject#getKeterangan()} menjamin tidak
	 * pernah {@code null}; override ini tidak. Tanpa {@code trim} dan tanpa penjaga null &mdash;
	 * pemanggil wajib menjaganya sendiri (renderer grid memang membungkusnya).</p>
	 *
	 * <p>Berbeda dari {@link JenisPenilaian} dan {@link GrupPenilaian}, properti ini
	 * <b>dipetakan dengan benar</b> ({@code @Column(name = "keterangan")}) dan ditulis oleh
	 * {@code onSave(...)}; bug "keterangan tidak pernah tersimpan" tidak berlaku di sini.</p>
	 *
	 * @return keterangan mentah, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas butir ini tanpa validasi.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama butir; {@code null} dinormalkan menjadi string kosong.
	 *
	 * <p><b>Bukan label yang dilihat guru.</b> Layar isi nilai memakai
	 * {@link #getLabelInputan()}. Dialog Tambah/Ubah mengisi <b>keduanya dari isian yang sama</b>
	 * (<i>"Nama Item Penilaian *"</i>), sehingga pada praktiknya keduanya kembar &mdash; tetapi
	 * unggahan Excel dapat membuatnya berbeda karena kolom {@code nama} dan {@code labelInputan}
	 * berdiri sendiri di {@code JenisItemPenilaianSiswaAction#contents}.</p>
	 *
	 * <p>Kolomnya {@code nullable = false} di sisi DB, tetapi penjaga di sini membuat nilai
	 * {@code null} tersimpan sebagai string kosong, bukan gagal.</p>
	 *
	 * @return nama butir; tidak pernah {@code null}
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null ? "" : nama;
	}

	/**
	 * Menyetel nama butir tanpa validasi maupun {@code trim}.
	 *
	 * @param nama nama baru; boleh {@code null} (akan dibaca sebagai {@code ""})
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan tipe isian butir ini &mdash; salah satu dari sembilan konstanta kelas ini.
	 *
	 * <p><b>Getter write-back:</b> bila field masih {@code null}, method ini <b>menulis</b>
	 * {@link #PILIHAN_CUSTOM} ke field sebelum mengembalikannya. Karena Hibernate membaca lewat
	 * getter, baris dengan kolom NULL akan tersimpan sebagai {@code "Berupa pilihan custom"} begitu
	 * tersentuh &mdash; pilihan bawaan yang tidak pernah ditampilkan sebagai keputusan sadar kepada
	 * pengguna.</p>
	 *
	 * <p>Dipanggil di seluruh helper penilaian untuk memilih cabang render/validasi, dan di
	 * renderer grid layar master untuk kolom <i>Revisi</i>.</p>
	 *
	 * @return tipe isian; tidak pernah {@code null}
	 */
	public String getTipeDataInputan() {
		if (tipeDataInputan == null) {
			tipeDataInputan = PILIHAN_CUSTOM;
		}
		return tipeDataInputan;
	}

	/**
	 * Menyetel tipe isian butir. Tidak ada validasi bahwa nilainya salah satu dari sembilan
	 * konstanta kelas ini &mdash; nilai asing akan membuat seluruh cabang {@code equals(...)} di
	 * helper penilaian meleset sehingga butir tidak dirender sama sekali.
	 *
	 * @param tipeDataInputan salah satu konstanta tipe isian kelas ini
	 */
	public void setTipeDataInputan(String tipeDataInputan) {
		this.tipeDataInputan = tipeDataInputan;
	}

	/**
	 * Menyatakan apakah pengisian butir ini disertai unggahan berkas lampiran.
	 *
	 * <p><b>Getter write-back:</b> {@code null} ditulis menjadi {@code false} sebelum
	 * dikembalikan.</p>
	 *
	 * <p>Bila {@code true}, layar isi nilai menambahkan kontrol unggah/unduh
	 * ({@code LampiranLain.createDownloadUploadFileLain(...)}) dan renderer layar master
	 * menampilkan checkbox <i>"Lampiran Wajib"</i> ({@link #getLampiranWajibDiisi()}); bila
	 * {@code false}, checkbox itu diganti label kosong sehingga nilai
	 * {@code lampiranWajibDiisi} menjadi tak terjangkau dari UI.</p>
	 *
	 * @return {@code true} bila butir ini meminta lampiran; tidak pernah {@code null}
	 */
	public Boolean getHarusMenyertakanLampiran() {
		if (harusMenyertakanLampiran == null) {
			harusMenyertakanLampiran = false;
		}
		return harusMenyertakanLampiran;
	}

	/**
	 * Menyetel apakah butir ini meminta lampiran. Ditulis dari checkbox <i>"Menyertakan file
	 * lampiran"</i> pada dialog Tambah/Ubah.
	 *
	 * @param harusMenyertakanLampiran {@code true} bila lampiran diminta; {@code null} akan dibaca
	 *                                 sebagai {@code false}
	 */
	public void setHarusMenyertakanLampiran(Boolean harusMenyertakanLampiran) {
		this.harusMenyertakanLampiran = harusMenyertakanLampiran;
	}

	/**
	 * Mengembalikan label yang benar-benar tampil di formulir isi nilai dan pada judul kontrol
	 * lampiran &mdash; <b>bukan</b> {@link #getNama()}.
	 *
	 * <p><b>Getter write-back:</b> {@code null} ditulis menjadi string kosong sebelum
	 * dikembalikan.</p>
	 *
	 * <p>Renderer layar master menampilkannya sebagai {@code getKode() + "-" + getLabelInputan()}
	 * pada kolom pertama grid.</p>
	 *
	 * @return label isian; tidak pernah {@code null}
	 */
	public String getLabelInputan() {
		if (labelInputan == null) {
			labelInputan = "";
		}
		return labelInputan;
	}

	/**
	 * Menyetel label isian. Dialog Tambah/Ubah mengisi ini dan {@code nama} dari isian yang sama.
	 *
	 * @param labelInputan label baru; boleh {@code null} (akan dibaca sebagai {@code ""})
	 */
	public void setLabelInputan(String labelInputan) {
		this.labelInputan = labelInputan;
	}

	/**
	 * Mengembalikan daftar opsi beserta skornya untuk tipe {@link #PILIHAN_CUSTOM} dan
	 * {@link #PILIHAN_BANYAK}.
	 *
	 * <p>Formatnya <i>{@code opsi:skor;opsi:skor;...}</i> &mdash; opsi dipisah titik koma, skor
	 * dipisah titik dua, dan skor harus berupa angka desimal. Contoh yang dipakai teks bantuan
	 * dialog: {@code Ya:1;Tidak:0;Belum Tau:2}. Untuk tipe selain kedua itu isinya diabaikan.</p>
	 *
	 * <p><b>Getter write-back:</b> {@code null} ditulis menjadi string kosong. Kolomnya
	 * {@code text} sehingga daftar opsi boleh panjang.</p>
	 *
	 * <p>Tidak ada validasi format sama sekali &mdash; baik di setter, di {@code onSave(...)},
	 * maupun saat diurai. Format yang salah menghasilkan opsi tanpa skor (dihitung 0) tanpa pesan
	 * kesalahan.</p>
	 *
	 * @return daftar opsi mentah; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getNilaiDataInputan() {
		if (nilaiDataInputan == null) {
			nilaiDataInputan = "";
		}
		return nilaiDataInputan;
	}

	/**
	 * Menyetel daftar opsi+skor apa adanya, tanpa validasi format.
	 *
	 * @param nilaiDataInputan daftar opsi dalam format {@code opsi:skor;opsi:skor}; boleh
	 *                         {@code null}
	 */
	public void setNilaiDataInputan(String nilaiDataInputan) {
		this.nilaiDataInputan = nilaiDataInputan;
	}

	/**
	 * Menyatakan apakah butir ini masih dipakai.
	 *
	 * <p><b>Getter write-back yang menjadi tumpuan sembilan query.</b> Bila field {@code null},
	 * method ini <b>menulis</b> {@code true} ke field sebelum mengembalikannya. Karena Hibernate
	 * membaca lewat getter, kolom {@code aktif} praktis tidak pernah NULL di database. Seluruh
	 * pembaca menyaring dengan {@code or(isNull("aktif"), eq("aktif", true))} sehingga saat ini
	 * kedua cabang setara &mdash; mengubah getter ini menjadi pengembali nilai mentah akan
	 * mengaktifkan cabang {@code isNull} secara serempak dan mengubah perilaku sembilan layar/API
	 * sekaligus.</p>
	 *
	 * <p><b>Tidak ditulis oleh dialog Tambah/Ubah.</b> {@code onSave(...)} tidak menyentuh
	 * {@code aktif}; hanya checkbox <i>"Aktif"</i> pada grid daftar (tanpa gerbang hak) dan unggahan
	 * Excel yang mengubahnya.</p>
	 *
	 * <p><b>Menonaktifkan butir bertipe {@link #FORMULA} berdampak melampaui butir itu sendiri:</b>
	 * {@code GrupPenilaianUtil#ambilPoint(...)} melewati butir non-aktif saat menyusun peta
	 * substitusi, sehingga kodenya tidak lagi tergantikan di formula butir lain &mdash; formula itu
	 * menghitung dengan token utuh dan umumnya menghasilkan {@code 0.0} tanpa error.</p>
	 *
	 * @return {@code true} bila butir masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel saklar aktif butir ini.
	 *
	 * <p>Dipanggil dari checkbox <i>"Aktif"</i> pada renderer grid layar master, yang langsung
	 * diikuti {@code Common.refreshSaveOrUpdate(...)} &mdash; perubahan tersimpan seketika tanpa
	 * tombol Simpan dan <b>tanpa pemeriksaan hak apa pun</b> ({@code setDisabled(!edit)} dengan
	 * {@code edit} yang di-hardcode {@code true}).</p>
	 *
	 * @param aktif saklar aktif; {@code null} akan dibaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan urutan tampil butir ini di dalam kategorinya.
	 *
	 * <p>Ini <b>kunci urut SEKUNDER</b> seluruh kolom nilai: sembilan query pemanggil memakai
	 * {@code addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))} lalu
	 * {@code addOrder(Order.asc("nomorUrut"))}. Kunci primernya adalah
	 * {@link KategoriItemPenilaianSiswa#getKode()}.</p>
	 *
	 * <p><b>Getter write-back:</b> {@code null} ditulis menjadi {@code 1}. Ternary
	 * {@code nomorUrut == null ? 1 : nomorUrut} pada baris {@code return} karena itu <b>kode
	 * mati</b> &mdash; cabang {@code null}-nya tidak akan pernah tercapai.</p>
	 *
	 * <p><b>Ranjau {@code TreeSet}.</b> Karena tidak pernah {@code null} dan
	 * {@link #compareTo(GeneralValueObject)} hanya membandingkan properti ini, seluruh butir yang
	 * belum diberi nomor urut saling setara menurut {@code compareTo}. Memasukkan entity ini ke
	 * {@code TreeSet}/{@code TreeMap} akan menciutkan daftar menjadi satu elemen. Seluruh pemanggil
	 * saat ini memakai {@code List} dengan pengurutan SQL, jadi belum ada kerusakan aktif &mdash;
	 * tetapi jangan pernah mengandalkan {@code compareTo} untuk deduplikasi.</p>
	 *
	 * @return nomor urut; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil butir ini.
	 *
	 * <p>Ditulis dari {@code Intbox} pada renderer grid layar master, diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} &mdash; tersimpan seketika, <b>tanpa gerbang hak</b>
	 * dan tanpa validasi nilai (negatif dan nol diterima).</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dibaca sebagai {@code 1}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Menyatakan apakah butir ini wajib diisi guru.
	 *
	 * <p><b>Getter write-back:</b> {@code null} ditulis menjadi <b>{@code true}</b> &mdash;
	 * perhatikan bawaannya "wajib", berbeda dari mayoritas flag lain di kelas ini yang berbawaan
	 * {@code false}.</p>
	 *
	 * @return {@code true} bila butir wajib diisi; tidak pernah {@code null}
	 */
	public Boolean getWajibDiisi() {
		if (wajibDiisi == null) {
			wajibDiisi = true;
		}
		return wajibDiisi;
	}

	/**
	 * Menyetel apakah butir wajib diisi.
	 *
	 * <p>Ditulis dari checkbox <i>"Isian Wajib"</i> pada renderer grid layar master (bukan dari
	 * dialog Tambah/Ubah), diikuti {@code Common.refreshSaveOrUpdate(...)} tanpa gerbang hak.</p>
	 *
	 * @param wajibDiisi {@code true} bila wajib; {@code null} akan dibaca sebagai {@code true}
	 */
	public void setWajibDiisi(Boolean wajibDiisi) {
		this.wajibDiisi = wajibDiisi;
	}

	/**
	 * Menyatakan apakah lampiran wajib diunggah.
	 *
	 * <p>Hanya bermakna bila {@link #getHarusMenyertakanLampiran()} bernilai {@code true}; layar
	 * isi nilai menambahkan tanda {@code " (*)"} pada judul kontrol lampiran saat flag ini
	 * aktif.</p>
	 *
	 * <p><b>Getter write-back dengan bawaan {@code true}</b>, sama seperti
	 * {@link #getWajibDiisi()}. Karena checkbox pengubahnya hanya dirender ketika
	 * {@link #getHarusMenyertakanLampiran()} bernilai benar, baris yang tidak memakai lampiran akan
	 * tetap menyimpan {@code true} di kolom ini tanpa ada cara mengubahnya dari UI.</p>
	 *
	 * @return {@code true} bila lampiran wajib; tidak pernah {@code null}
	 */
	public Boolean getLampiranWajibDiisi() {
		if (lampiranWajibDiisi == null) {
			lampiranWajibDiisi = true;
		}
		return lampiranWajibDiisi;
	}

	/**
	 * Menyetel apakah lampiran wajib diunggah. Ditulis dari checkbox <i>"Lampiran Wajib"</i> pada
	 * renderer grid, yang hanya muncul bila {@link #getHarusMenyertakanLampiran()} benar.
	 *
	 * @param lampiranWajibDiisi {@code true} bila wajib; {@code null} akan dibaca sebagai
	 *                           {@code true}
	 */
	public void setLampiranWajibDiisi(Boolean lampiranWajibDiisi) {
		this.lampiranWajibDiisi = lampiranWajibDiisi;
	}

	/**
	 * Menyatakan apakah isian butir ini dibekukan untuk pengguna non-admin.
	 *
	 * <p><b>Getter write-back:</b> {@code null} ditulis menjadi {@code false}.</p>
	 *
	 * <p>Bila {@code true}, {@code DetailPenilaianSiswaHelper} (dan padanan Les-nya) memanggil
	 * {@code Common.freeze(subRows, !Common.getApakahAdmin(getKodeAdminYgBoleh()))}. Perhatikan dua
	 * hal: (1) ini <b>pembekuan komponen UI</b>, bukan gerbang di alur simpan; (2) getter ini juga
	 * menjadi syarat penghapusan destruktif pada {@link #getKodeAdminYgBoleh()}.</p>
	 *
	 * @return {@code true} bila butir dibatasi untuk admin; tidak pernah {@code null}
	 */
	public Boolean getHanyaTampilDiAdmin() {
		if (hanyaTampilDiAdmin == null) {
			hanyaTampilDiAdmin = false;
		}
		return hanyaTampilDiAdmin;
	}

	/**
	 * Menyetel pembatasan admin untuk butir ini.
	 *
	 * <p><b>Efek samping berbahaya:</b> menyetel {@code false} lalu membaca
	 * {@link #getKodeAdminYgBoleh()} akan <b>menghapus permanen</b> daftar putih kode admin yang
	 * tersimpan. Renderer grid melakukan persis urutan itu pada setiap perubahan checkbox
	 * <i>"Hanya Admin"</i> (ia memanggil {@code Common.refreshSaveOrUpdate(...)} tepat
	 * sesudahnya).</p>
	 *
	 * @param hanyaTampilDiAdmin {@code true} untuk membatasi ke admin; {@code null} akan dibaca
	 *                           sebagai {@code false}
	 */
	public void setHanyaTampilDiAdmin(Boolean hanyaTampilDiAdmin) {
		this.hanyaTampilDiAdmin = hanyaTampilDiAdmin;
	}

	/**
	 * Urutan alami butir penilaian: <b>hanya</b> menurut {@link #getNomorUrut()}.
	 *
	 * <p><b>Memangkas kontrak induk.</b> {@code GeneralValueObject#compareTo(GeneralValueObject)}
	 * mencoba empat kunci berjenjang ({@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama}
	 * &rarr; {@code keterangan}) dan menelan exception. Override ini membuang ketiga cadangan itu
	 * dan menambahkan cast {@code (JenisItemPenilaianSiswa) arg0} <b>tanpa {@code instanceof}</b>,
	 * sehingga membandingkan dengan entity jenis lain melempar {@code ClassCastException} &mdash;
	 * padahal implementasi induk akan menanganinya dengan tenang.</p>
	 *
	 * <p><b>Praktis mengembalikan {@code 0} untuk mayoritas pasangan.</b> Karena
	 * {@link #getNomorUrut()} berbawaan {@code 1}, semua butir yang belum diberi nomor urut saling
	 * setara. {@code compareTo} di sini <b>tidak konsisten dengan {@code equals}</b> (yang
	 * membandingkan {@code id}), jadi jangan memakai entity ini sebagai kunci
	 * {@code TreeSet}/{@code TreeMap}. Sembilan pemanggil produksi tidak melakukannya &mdash;
	 * mereka mengandalkan {@code ORDER BY} di SQL.</p>
	 *
	 * @param arg0 entity pembanding; <b>harus</b> bertipe {@code JenisItemPenilaianSiswa}
	 * @return hasil {@code Integer#compareTo} atas kedua nomor urut
	 * @throws ClassCastException bila {@code arg0} bukan {@code JenisItemPenilaianSiswa}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((JenisItemPenilaianSiswa) arg0).getNomorUrut());
	}

	/**
	 * Mengembalikan daftar kode peran admin yang boleh mengubah isian butir ini, dipisah koma.
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; menghapus data secara permanen.</b> Bila
	 * {@link #getHanyaTampilDiAdmin()} bernilai {@code false}, method ini <b>menulis string
	 * kosong ke field</b> {@code kodeAdminYgBoleh}. Karena Hibernate membaca properti lewat getter,
	 * daftar putih yang pernah dikonfigurasi <b>hilang dari database</b> begitu baris tersimpan
	 * berikutnya; mencentang ulang <i>"Hanya Admin"</i> <b>tidak</b> memulihkannya.</p>
	 *
	 * <p>Pemicunya sangat mudah tersentuh: renderer grid layar master memanggil getter ini untuk
	 * <b>setiap baris tanpa syarat</b> (mengisi {@code MyTextbox} kode admin), dan setiap checkbox
	 * pada baris yang sama memanggil {@code Common.refreshSaveOrUpdate(...)}. Jadi cukup "buka
	 * layar, centang apa saja pada baris itu".</p>
	 *
	 * <p><b>Konsekuensi hak akses saat daftar kosong:</b>
	 * {@code CommonCurrentSessionHelper#getApakahAdmin(String)} memperlakukan {@code null}/kosong
	 * sebagai "hanya peran {@code Tbmrole.ADMINISTRATOR}". Jadi kehilangan daftar ini
	 * <b>memperketat</b>, bukan membuka &mdash; admin bidang yang dulu boleh mengubah butir tersebut
	 * mendadak terkunci, tanpa pesan apa pun.</p>
	 *
	 * <p>Nilai kembalinya selalu di-{@code trim}; {@code null} menjadi string kosong.</p>
	 *
	 * @return daftar kode peran admin dipisah koma; tidak pernah {@code null}, bisa string kosong
	 */
	public String getKodeAdminYgBoleh() {
		if (!getHanyaTampilDiAdmin()) {
			kodeAdminYgBoleh = "";
		}
		return kodeAdminYgBoleh == null ? "" : kodeAdminYgBoleh.trim();
	}

	/**
	 * Menyetel daftar kode peran admin yang boleh mengubah butir ini.
	 *
	 * <p>Formatnya daftar {@code Tbmrole.roleId} dipisah koma; pencocokannya
	 * <i>case-insensitive</i> per elemen. Renderer grid menuliskan nilai yang sudah di-{@code trim}
	 * dari {@code MyTextbox} berlabel bantuan <i>"Masukkan kode admin yg boleh ubah, jika lebih
	 * dari satu pisahkan dengan tanda koma"</i>. Tidak ada validasi bahwa kode yang dimasukkan
	 * benar-benar ada.</p>
	 *
	 * <p>Nilai yang disetel di sini akan <b>terhapus</b> bila {@link #getHanyaTampilDiAdmin()}
	 * bernilai {@code false} &mdash; setel flag itu lebih dahulu.</p>
	 *
	 * @param kodeAdminYgBoleh daftar kode peran dipisah koma; boleh {@code null}
	 */
	public void setKodeAdminYgBoleh(String kodeAdminYgBoleh) {
		this.kodeAdminYgBoleh = kodeAdminYgBoleh;
	}

	/**
	 * Menyatakan apakah kotak keterangan tambahan ditampilkan di samping isian nilai.
	 *
	 * <p>Berbeda dari mayoritas getter boolean lain di kelas ini, method ini <b>tidak</b> menulis
	 * balik ke field &mdash; ia hanya menormalkan {@code null} menjadi {@code false} pada nilai
	 * kembalinya. Kolom DB karena itu tetap boleh NULL.</p>
	 *
	 * @return {@code true} bila kotak keterangan ditampilkan; tidak pernah {@code null}
	 */
	public Boolean getTampilkanIsianKeterangan() {
		return tampilkanIsianKeterangan == null ? false : tampilkanIsianKeterangan;
	}

	/**
	 * Menyetel apakah kotak keterangan tambahan ditampilkan. Ditulis dari checkbox <i>"Tampilkan
	 * isian keterangan"</i> pada dialog Tambah/Ubah, yang juga mengatur visibilitas baris
	 * <i>"Label isian keterangan"</i> secara langsung.
	 *
	 * @param tampilkanIsianKeterangan {@code true} untuk menampilkan; {@code null} dibaca sebagai
	 *                                 {@code false}
	 */
	public void setTampilkanIsianKeterangan(Boolean tampilkanIsianKeterangan) {
		this.tampilkanIsianKeterangan = tampilkanIsianKeterangan;
	}

	/**
	 * Mengembalikan label kotak keterangan tambahan, dengan bawaan literal {@code "Keterangan"}.
	 *
	 * <p>Tidak menulis balik ke field: {@code null} maupun string kosong/spasi menghasilkan
	 * {@code "Keterangan"} pada nilai kembali, tetapi field dan kolom DB tetap apa adanya. Nilai
	 * non-kosong selalu di-{@code trim}.</p>
	 *
	 * <p>Perhatikan label ini <b>tidak diterjemahkan</b> lewat mekanisme bahasa mana pun &mdash;
	 * teksnya hardcode dalam bahasa Indonesia.</p>
	 *
	 * @return label kotak keterangan; tidak pernah {@code null} dan tidak pernah kosong
	 */
	public String getLabelInputanKeterangan() {
		return labelInputanKeterangan == null || labelInputanKeterangan.trim().isEmpty() ? "Keterangan"
				: labelInputanKeterangan.trim();
	}

	/**
	 * Menyetel label kotak keterangan tambahan apa adanya (tanpa {@code trim}).
	 *
	 * @param labelInputanKeterangan label baru; {@code null}/kosong akan dibaca sebagai
	 *                               {@code "Keterangan"}
	 */
	public void setLabelInputanKeterangan(String labelInputanKeterangan) {
		this.labelInputanKeterangan = labelInputanKeterangan;
	}

	/**
	 * Mengembalikan tinggi kotak teks (jumlah baris) untuk butir bertipe {@link #TEXT}.
	 *
	 * <p>Diteruskan langsung ke {@code Textbox#setRows(int)} oleh helper penilaian. Baris dialog
	 * <i>"Jumlah Baris"</i> hanya muncul untuk tipe {@link #TEXT}, tetapi nilainya tetap
	 * tersimpan bila tipe diubah kemudian.</p>
	 *
	 * <p>Tidak menulis balik ke field; {@code null} dinormalkan menjadi {@code 1} pada nilai
	 * kembali. Tidak ada penjaga nilai nol/negatif.</p>
	 *
	 * @return jumlah baris kotak teks; tidak pernah {@code null}
	 */
	public Integer getJumlahBaris() {
		return jumlahBaris == null ? 1 : jumlahBaris;
	}

	/**
	 * Menyetel tinggi kotak teks untuk tipe {@link #TEXT}, tanpa validasi.
	 *
	 * @param jumlahBaris jumlah baris; {@code null} dibaca sebagai {@code 1}
	 */
	public void setJumlahBaris(Integer jumlahBaris) {
		this.jumlahBaris = jumlahBaris;
	}

	/**
	 * Mengembalikan batas atas nilai yang boleh diisi untuk butir bertipe {@link #ANGKA}.
	 *
	 * <p><b>Hanya ditegakkan untuk tipe {@link #ANGKA}.</b> {@code DetailPenilaianSiswaHelper}
	 * memeriksa {@code getNilaiMax() < valData} pada event {@code onChange} kotak nilai, menampilkan
	 * peringatan, lalu <b>mengembalikan kotak ke nilai lama</b> &mdash; atau ke batas ini bila
	 * belum ada nilai lama. Tipe {@link #TEXT_ANGKA}, yang juga ikut dijumlahkan, <b>tidak</b>
	 * diperiksa.</p>
	 *
	 * <p>Ini validasi sisi UI pada event komponen; jalur simpan lain (API, unggah Excel, hitung
	 * ulang massal {@code GradingHelper}) tidak memeriksanya.</p>
	 *
	 * <p>Tidak menulis balik ke field; {@code null} dinormalkan menjadi {@code 100.0}.</p>
	 *
	 * @return batas atas nilai; tidak pernah {@code null}
	 */
	public Double getNilaiMax() {
		return nilaiMax == null ? 100.0 : nilaiMax;
	}

	/**
	 * Menyetel batas atas nilai. Tidak ada validasi bahwa nilainya lebih besar dari
	 * {@link #getNilaiMin()} &mdash; konfigurasi terbalik akan membuat setiap isian ditolak.
	 *
	 * @param nilaiMax batas atas; {@code null} dibaca sebagai {@code 100.0}
	 */
	public void setNilaiMax(Double nilaiMax) {
		this.nilaiMax = nilaiMax;
	}

	/**
	 * Mengembalikan batas bawah nilai yang boleh diisi untuk butir bertipe {@link #ANGKA}.
	 *
	 * <p>Ditegakkan dengan mekanisme dan keterbatasan yang sama persis seperti
	 * {@link #getNilaiMax()} &mdash; lihat catatan di sana.</p>
	 *
	 * <p>Tidak menulis balik ke field; {@code null} dinormalkan menjadi {@code 0.0}.</p>
	 *
	 * @return batas bawah nilai; tidak pernah {@code null}
	 */
	public Double getNilaiMin() {
		return nilaiMin == null ? 0.0 : nilaiMin;
	}

	/**
	 * Menyetel batas bawah nilai, tanpa validasi silang terhadap {@link #getNilaiMax()}.
	 *
	 * @param nilaiMin batas bawah; {@code null} dibaca sebagai {@code 0.0}
	 */
	public void setNilaiMin(Double nilaiMin) {
		this.nilaiMin = nilaiMin;
	}

	/**
	 * Mengembalikan rumpun induk butir ini &mdash; <b>simpul ke-7 rantai penilaian</b>, tepat di
	 * atas kelas ini.
	 *
	 * <p>Inilah satu-satunya penghubung entity ini ke seluruh rantai penilaian. Sembilan query
	 * pemanggil menyaring dengan {@code Restrictions.in("kategoriItemPenilaianSiswa", stubIds)} dan
	 * mengurutkan dengan {@code Order.asc("kategoriItemPenilaianSiswa.kode")} &mdash; jadi
	 * <b>memindahkan butir ke kategori lain memindahkan kolom nilainya di rapor</b>, sementara
	 * nilai siswa yang sudah tersimpan ikut terbawa (kategori bukan bagian kunci penyimpanan).</p>
	 *
	 * <p><b>Getter write-back ringan:</b> hasil {@code GeneralValueObject#check(Object)} ditulis
	 * balik ke field untuk me-resolusi proxy lazy &mdash; perlu karena instance kelas ini sering
	 * <i>detached</i> (terdaftar di {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}).</p>
	 *
	 * <p>Relasi boleh {@code null} (dialog menyediakan pilihan <i>"=Tanpa Kategori="</i>). Butir
	 * tanpa kategori <b>tidak akan pernah muncul</b> di layar isi nilai maupun rapor, karena
	 * seluruh pemanggil menyaring lewat daftar id kategori. {@code Cascade} hanya
	 * {@code PERSIST}/{@code MERGE} &mdash; tidak ada {@code REMOVE}.</p>
	 *
	 * @return kategori induk, atau {@code null} bila butir belum/tidak dikelompokkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_item_penilaian_siswa")
	public KategoriItemPenilaianSiswa getKategoriItemPenilaianSiswa() {
		kategoriItemPenilaianSiswa = check(kategoriItemPenilaianSiswa);
		return kategoriItemPenilaianSiswa;
	}

	/**
	 * Menyetel rumpun induk butir ini.
	 *
	 * <p>Dipanggil {@code onSave(...)} dari combo <i>"Kategori Penilaian"</i>, yang diisi hanya
	 * dengan kategori ber-{@code aktif = true} milik sekolah terpilih. Perhatikan combo itu memakai
	 * {@code Restrictions.eq("aktif", true)} <b>tanpa</b> cabang {@code isNull} yang dipakai
	 * sembilan query pembaca &mdash; ketergantungan tersembunyi pada
	 * {@code KategoriItemPenilaianSiswa#getAktif()} yang menulis {@code true} untuk kolom NULL.</p>
	 *
	 * @param kategoriItemPenilaianSiswa kategori induk; {@code null} berarti "Tanpa Kategori"
	 */
	public void setKategoriItemPenilaianSiswa(KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa) {
		this.kategoriItemPenilaianSiswa = kategoriItemPenilaianSiswa;
	}

	/**
	 * Mengembalikan sekolah pemilik butir ini.
	 *
	 * <p>Boleh {@code null}, dan itu <b>bermakna</b>: butir tanpa sekolah dimaksudkan berlaku
	 * lintas sekolah. Penyaring layar master memakai {@code isNull("sekolah") OR eq("sekolah", s)}
	 * untuk menghormati hal itu &mdash; efek sampingnya penyaringan bersifat <i>fail-open</i>, dan
	 * bila combo pencarian diisi "=Semua=" tidak ada penyaring tenant sama sekali
	 * ({@code sqlRestriction("1=1")}).</p>
	 *
	 * <p><b>Perhatian penting:</b> sembilan query yang benar-benar membangun rapor
	 * <b>tidak menyaring kolom ini sama sekali</b> &mdash; cakupan tenant sepenuhnya diwakilkan
	 * kepada daftar id kategori. Demikian pula cache preload {@code ConstantValues} yang dipakai
	 * mesin formula memuat seluruh baris instalasi tanpa penyaring. Kolom ini karena itu
	 * lebih merupakan metadata administratif daripada batas keamanan.</p>
	 *
	 * <p><b>Getter write-back ringan</b> (de-proxy lewat {@code check(...)}), sama seperti
	 * {@link #getKategoriItemPenilaianSiswa()}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila butir berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik butir ini.
	 *
	 * <p><b>Menormalkan "stub tanpa id" menjadi {@code null}:</b> object {@link Sekolah} yang
	 * {@code getId()}-nya {@code null} diperlakukan sama dengan {@code null}. Ini mencegah
	 * penyimpanan referensi transient yang akan gagal di tingkat FK &mdash; tetapi juga berarti
	 * kesalahan pemanggil (mengirim stub kosong) berubah menjadi "berlaku untuk semua sekolah"
	 * secara diam-diam.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau stub tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik butir ini.
	 *
	 * <p><b>Getter destruktif tingkat sedang &mdash; menurunkan ulang pada setiap pembacaan.</b>
	 * Method ini: (1) memanggil {@link #getSekolah()} dan <b>menimpa field {@code sekolah}</b>
	 * dengan hasilnya; (2) bila sekolah tidak {@code null}, <b>menimpa field {@code yayasan}</b>
	 * dengan {@code sekolah.getYayasan()}; (3) baru kemudian me-resolusi proxy dengan
	 * {@code check(...)}.</p>
	 *
	 * <p>Akibatnya nilai yayasan yang disetel eksplisit lewat
	 * {@link #setYayasan(Yayasan)} <b>tidak bertahan</b> selama {@code sekolah} terisi &mdash;
	 * kolom {@code yayasan_id} selalu menjadi turunan dari sekolah, bahkan ketika
	 * {@code onSave(...)} baru saja menulis pilihan combo <i>"Yayasan *"</i> yang berbeda. Yang
	 * disimpan Hibernate adalah hasil getter ini, bukan apa yang dipilih pengguna.</p>
	 *
	 * <p>Bila {@code sekolah} bernilai {@code null}, nilai yayasan yang disetel manual dipertahankan
	 * apa adanya.</p>
	 *
	 * @return yayasan pemilik; diturunkan dari sekolah bila sekolah terisi, {@code null} bila
	 *         keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik butir ini, dengan normalisasi "stub tanpa id" yang sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * <p>Nilai yang disetel di sini akan <b>ditimpa</b> oleh {@link #getYayasan()} pada pembacaan
	 * berikutnya selama {@code sekolah} terisi &mdash; lihat catatan di getter tersebut.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau stub tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Formula bawaan untuk butir yang belum disetel: representasi string dari {@code JSONArray}
	 * kosong, yaitu {@code "[]"}.
	 *
	 * <p><b>Bukan {@code final}.</b> Ini {@code public static String} biasa yang diinisialisasi
	 * sekali saat kelas dimuat. Kode mana pun dapat menimpanya saat runtime dan mengubah formula
	 * bawaan seluruh butir yang belum disetel &mdash; secara global untuk satu JVM. Perlakukan
	 * sebagai konstanta meskipun kompiler tidak memaksanya.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan formula butir ini sebagai string {@code JSONArray}.
	 *
	 * <p>Hanya bermakna untuk tipe {@link #FORMULA}. Isinya diurai
	 * {@code new JSONArray(getFormula())} oleh {@code JenisItemPenilaianSiswaAction#init(...)} dan
	 * dirender lewat {@code GrupPenilaianAction.reloadFormula(...)}; evaluasinya dikerjakan
	 * {@code ais.action.master.sekolah.util.GrupPenilaianUtil#hitung(...)}.</p>
	 *
	 * <p><b>Cara substitusi yang perlu diketahui sebelum menulis formula:</b> mesin formula
	 * mengganti setiap {@link #getKode()} butir lain yang muncul di dalam ekspresi dengan nilai
	 * siswa yang bersangkutan, tetapi pencocokannya
	 * {@code StringUtils.contains(target, " " + kode + " ")} &mdash; <b>kode wajib diapit spasi</b>.
	 * Penggantiannya memakai {@code String#replaceAll(String, String)} yang ikut memakan kedua spasi
	 * pengapit, sehingga dua kode bersebelahan tanpa operator ({@code " a b "}) hanya tersubstitusi
	 * yang pertama. Placeholder {@code kkm}/{@code KKM} dan kode {@code Konstanta} aktif juga
	 * disubstitusi di tahap yang sama.</p>
	 *
	 * <p>Sumber daftar butir untuk substitusi adalah cache preload
	 * {@code ConstantValues.ambilBerdasarClass(JenisItemPenilaianSiswa.class)} yang memuat
	 * <b>seluruh baris instalasi tanpa penyaring tenant</b>; penyaring kategori tetap diterapkan,
	 * tetapi kesamaan kode antar sekolah tetap menjadi risiko.</p>
	 *
	 * <p>{@code null} maupun string kosong dinormalkan menjadi {@link #DEFAULT_FORMULA}; tidak
	 * menulis balik ke field. Kolomnya {@code text}.</p>
	 *
	 * @return formula dalam bentuk string JSON; tidak pernah {@code null} dan tidak pernah kosong
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menyetel formula butir ini apa adanya, tanpa validasi JSON.
	 *
	 * <p>{@code onSave(...)} selalu menuliskan {@code array.toString()} dari editor formula &mdash;
	 * termasuk saat tipe isian bukan {@link #FORMULA}, sehingga baris non-formula pun menyimpan
	 * {@code "[]"} di kolom ini. String yang bukan JSON valid akan melempar exception saat dialog
	 * dibuka kembali.</p>
	 *
	 * @param formula string {@code JSONArray}; boleh {@code null}/kosong (dibaca sebagai
	 *                {@link #DEFAULT_FORMULA})
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Menyatakan apakah rapor menghitung rata-rata butir ini untuk seluruh siswa satu kelas.
	 *
	 * <p><b>Hanya {@code LaporanRaporSiswa} yang memakainya.</b> Bila {@code true}, laporan
	 * memuat SELURUH {@code KelasSiswaPunyaSiswa} pada kelas yang sama, mengurai nilai butir ini
	 * dari string {@code detailNilai} tiap siswa, dan menyediakan parameter Jasper
	 * {@code <kodeKategori>_<kodeItem>_rata_rata_kelas} beserta versi {@code _text}-nya.</p>
	 *
	 * <p><b>Dua kekhasan perhitungan yang mudah mengejutkan:</b> (1) hanya nilai
	 * {@code > 0.1} yang ikut dirata-rata &mdash; nilai nol dan nyaris nol dibuang dari pembilang
	 * <i>dan</i> penyebut, sehingga rata-rata kelas selalu lebih tinggi dari rata-rata aritmetika
	 * sesungguhnya; (2) nilai non-numerik diperlakukan sebagai {@code "0.0"} lalu ikut terbuang oleh
	 * aturan yang sama.</p>
	 *
	 * <p>Hasilnya di-cache per kombinasi butir/kelas/mapel/semester selama proses pencetakan.
	 * Tidak menulis balik ke field; {@code null} dinormalkan menjadi {@code false}.</p>
	 *
	 * @return {@code true} bila rata-rata kelas dihitung; tidak pernah {@code null}
	 */
	public Boolean getHitungRataRataKelas() {
		return hitungRataRataKelas == null ? false : hitungRataRataKelas;
	}

	/**
	 * Menyetel apakah rata-rata se-kelas dihitung untuk butir ini. Ditulis dari checkbox
	 * <i>"Hitung rata-rata nilai siswa dalam satu kelas"</i> pada dialog Tambah/Ubah.
	 *
	 * @param hitungRataRataKelas {@code true} untuk menghitung; {@code null} dibaca sebagai
	 *                            {@code false}
	 */
	public void setHitungRataRataKelas(Boolean hitungRataRataKelas) {
		this.hitungRataRataKelas = hitungRataRataKelas;
	}

	/**
	 * Menyatakan apakah rapor menghitung rata-rata butir ini untuk seluruh siswa satu angkatan.
	 *
	 * <p>Mekanismenya identik dengan {@link #getHitungRataRataKelas()} (termasuk ambang
	 * {@code > 0.1} dan cache), hanya berbeda cakupan: query pengumpulnya menyaring
	 * {@code siswa.tahunMasuk} <b>dan</b> {@code siswa.sekolah}, jadi cakupan tenant di sini
	 * <b>benar</b>. Parameter Jasper yang dihasilkan
	 * {@code <kodeKategori>_<kodeItem>_rata_rata_angkatan}.</p>
	 *
	 * <p><b>Catatan kinerja:</b> perhitungan ini memuat seluruh baris roster satu angkatan
	 * se-sekolah ke memori untuk setiap butir yang mengaktifkannya. Cache meringankan pengulangan
	 * dalam satu proses cetak, tetapi mengaktifkan flag ini pada banyak butir sekaligus berbiaya
	 * mahal pada instalasi besar.</p>
	 *
	 * <p>Tidak menulis balik ke field; {@code null} dinormalkan menjadi {@code false}.</p>
	 *
	 * @return {@code true} bila rata-rata angkatan dihitung; tidak pernah {@code null}
	 */
	public Boolean getHitungRataRataAngkatan() {
		return hitungRataRataAngkatan == null ? false : hitungRataRataAngkatan;
	}

	/**
	 * Menyetel apakah rata-rata se-angkatan dihitung untuk butir ini. Ditulis dari checkbox
	 * <i>"Hitung rata-rata nilai siswa dalam satu angkatan"</i> pada dialog Tambah/Ubah.
	 *
	 * @param hitungRataRataAngkatan {@code true} untuk menghitung; {@code null} dibaca sebagai
	 *                               {@code false}
	 */
	public void setHitungRataRataAngkatan(Boolean hitungRataRataAngkatan) {
		this.hitungRataRataAngkatan = hitungRataRataAngkatan;
	}

	/**
	 * Mengembalikan panjang maksimum teks untuk butir bertipe {@link #TEXT}.
	 *
	 * <p>Diteruskan ke {@code Textbox#setMaxlength(int)} oleh {@code DetailPenilaianSiswaHelper}.
	 * Baris dialog <i>"Jumlah Maksimal Teks"</i> hanya muncul untuk tipe teks.</p>
	 *
	 * <p>Bawaannya {@code 100000} &mdash; praktis "tanpa batas", dan jauh lebih besar dari kapasitas
	 * wajar kolom {@code detailNilai} yang menampung seluruh nilai satu siswa dalam satu string.
	 * Membiarkannya di nilai bawaan pada butir teks bebas berpotensi membengkakkan baris roster.</p>
	 *
	 * <p>Tidak menulis balik ke field; {@code null} dinormalkan menjadi {@code 100000}.</p>
	 *
	 * @return panjang maksimum teks; tidak pernah {@code null}
	 */
	public Integer getJumlahText() {
		return jumlahText == null ? 100000 : jumlahText;
	}

	/**
	 * Menyetel panjang maksimum teks untuk tipe {@link #TEXT}, tanpa validasi.
	 *
	 * @param jumlahText panjang maksimum; {@code null} dibaca sebagai {@code 100000}
	 */
	public void setJumlahText(Integer jumlahText) {
		this.jumlahText = jumlahText;
	}
}
