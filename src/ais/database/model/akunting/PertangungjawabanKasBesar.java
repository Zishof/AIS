package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONArray;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PertangungjawabanKasBesar — SPJ/LPJ atas dana kas besar (tabel
 * {@code akunting.pertangungjawaban_kas_besar})</h3>
 *
 * <p>Entity ini mewakili <b>satu dokumen pertanggungjawaban (LPJ/SPJ) atas dana yang sudah dicairkan
 * lewat sebuah dokumen {@link KasBesar}</b>. Isinya menjawab pertanyaan "uang kas besar yang sudah
 * cair itu dipakai untuk apa saja": rincian belanja per baris ({@link #getFormula() formula} JSON),
 * total yang benar-benar terpakai ({@link #getNilai()}), pajak yang melekat ({@link #getPajak()}),
 * sisa yang harus disetor kembali ke kas ({@link #getDikembalikan()} beserta
 * {@link #getTanggalStor()} dan {@link #getTelahDikembalikan()}), serta sumbangan pihak ketiga yang
 * ikut membiayai kegiatan ({@link #getDariSponsor()} / {@link #getNamaSponsor()}). Dokumen ini
 * berada di <b>hilir</b> alur uang, bukan di hulu: ia tidak mencairkan uang, ia
 * <i>mempertanggungjawabkan</i> uang yang sudah cair.</p>
 *
 * <h4>Posisi dalam siklus kas besar (TERVERIFIKASI dari kode)</h4>
 * <ol>
 *   <li><b>Pengajuan</b> — pengguna membuat dokumen {@link KasBesar} (menu "Kas Besar"): keperluan,
 *   nilai, rincian, satuan kerja, alur SOP.</li>
 *   <li><b>Persetujuan</b> — {@code KasBesar.disetujuiOleh}/{@code status} terisi lewat alur
 *   {@link DisposisiSop}.</li>
 *   <li><b>Pencairan</b> — {@link DaftarPengajuanTransfer} (DPC) dibuat dari dokumen kas besar yang
 *   sudah disetujui, lalu {@code ProsesTransfer} merealisasikan pembayaran lewat bank.</li>
 *   <li><b>Pemakaian &amp; pelaporan</b> — pemegang dana membelanjakan uangnya, lalu membuat
 *   <b>dokumen ini</b> lewat menu "Pertangungjawaban Kas Besar"
 *   ({@code PertangungjawabanKasBesarAction}). Relasi {@link #getKasBesar()} menunjuk ke dokumen
 *   pencairan yang dipertanggungjawabkan; sebaliknya
 *   {@code KasBesar.setPertangungjawabanKasBesar(this)} diisi balik oleh Action setelah simpan,
 *   sehingga tautannya <b>dua arah tetapi lewat dua kolom {@code ManyToOne} terpisah</b> — bukan
 *   pasangan {@code @OneToMany}/{@code mappedBy} yang sesungguhnya. Konsekuensinya kedua sisi bisa
 *   saja tidak sinkron dan tidak ada yang memaksa konsistensinya.</li>
 *   <li><b>Persetujuan LPJ</b> — layar "Persetujuan Pertangungjawaban Kas Besar"
 *   ({@code PersetujuanPertangungjawabanKasBesarAction}, subkelas tipis yang hanya memanggil
 *   {@code super(true)}) mengisi {@link #getDisetujuiOleh()} dan {@link #getStatus()}.</li>
 *   <li><b>Setoran sisa</b> — bila {@link #getDikembalikan()} &gt; 0.1 dan LPJ sudah disetujui,
 *   {@code DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(...)} membuat DPC baru berjudul
 *   "Pembayaran pengembalian kas besar …" yang nominalnya diambil dari
 *   {@link #getDikembalikan()}.</li>
 *   <li><b>Posting jurnal</b> — layar "Posting Pertangungjawaban Kas Besar"
 *   ({@code PostingPertangungjawabanKasBesarAction}) menerbitkan
 *   {@code ais.database.model.akunting.GrupTransaksi} + baris
 *   {@code ais.database.model.akunting.Transaksi}, dan menyimpan capnya di
 *   {@link #getPostingHistory()}.</li>
 * </ol>
 *
 * <h4>Bagaimana dokumen ini menjadi jurnal (TERVERIFIKASI dari mesin posting)</h4>
 * <p>Sisi paling mudah disalahsangka: <b>hampir tidak satu pun nominal jurnal dibaca langsung dari
 * kolom entity ini</b>. Mesin posting menghitung ulang sendiri dari JSON rincian. Susunan kakinya:</p>
 * <ul>
 *   <li><b>Debet belanja</b> — akun diambil dari kunci {@code workspace} pada
 *   <b>{@code kasBesar.getFormula()}</b> (JSON milik dokumen <i>hulu</i>, bukan
 *   {@link #getFormula()} dokumen ini), nominalnya {@link #getNilai()}.</li>
 *   <li><b>Kredit pajak</b> — satu baris per {@code JenisPajakBarang} pada rincian, nominalnya
 *   dihitung ulang {@code persen/100 × jumlah}. {@link #getPajak()} sendiri
 *   <b>tidak pernah dibaca</b> oleh mesin posting.</li>
 *   <li><b>Kredit pokok</b> — {@code kasBesar.getJenisKasBesar().getAkunPenerima()}.</li>
 *   <li><b>Debet kelebihan</b> — bila {@link #getDikembalikan()} tidak nol, akun cara pembayaran
 *   transfer ikut didebet sebesar nilai pengembalian. Jadi setoran sisa <b>ikut masuk jurnal
 *   utama</b>, bukan lewat {@link #getPostingHistoryPengembalian()}.</li>
 *   <li><b>{@link #getDariSponsor()} tidak dipakai sama sekali</b> di jalur posting.</li>
 *   <li><b>Tanggal jurnal adalah {@link #getTanggalPersetujuan()}</b>, bukan
 *   {@link #getTanggalTransaksi()} — lihat catatan di getter tersebut.</li>
 * </ul>
 * <p><b>Yang perlu diketahui perawat kode:</b> {@code GrupTransaksi.ambilUnik()} tidak mengenali
 * kolom {@code pertangungjawaban_kas_besar}, sehingga {@code kodeUnik} jurnal dokumen ini selalu
 * {@code NULL} dan penjaga idempotensi ("apakah jurnal ini sudah pernah dibuat") tidak pernah
 * menemukan kecocokan. Satu-satunya rem terhadap jurnal ganda adalah cap
 * {@link #getPostingHistory()} itu sendiri — karena itu kontrak pada
 * {@link #setPostingHistory(PostingHistory)} wajib dipatuhi.</p>
 *
 * <h4>PENTING — beda dengan {@code Pertangungjawaban} (LPJ uang muka pegawai)</h4>
 * <p>{@link Pertangungjawaban} dan kelas ini adalah <b>sepasang kembar salin-tempel</b>: keduanya
 * lahir dari cetakan hbm2java yang sama, {@code serialVersionUID}-nya <b>identik</b>
 * ({@code 2463821577548439808L}, nilai yang sama juga dipakai {@link KasBesar}, {@code KasKecil},
 * dan {@link JenisKasBesar}), susunan field-nya berimpit, dan bahkan salah ketik nama
 * "Pertangungjawaban" (satu huruf {@code g}) diwariskan sama. Perbedaan yang benar-benar ada, dan
 * hanya itu, adalah:</p>
 * <ul>
 *   <li><b>Dokumen hulunya</b> — {@code Pertangungjawaban.uangMuka} → {@link UangMuka} (panjar yang
 *   diberikan kepada <i>seorang pegawai</i>, dicairkan atas nama pribadi dan menjadi piutang pegawai
 *   sampai dipertanggungjawabkan), sedangkan di sini {@link #getKasBesar()} → {@link KasBesar}
 *   (permintaan dana bernilai besar yang dibayarkan keuangan pusat lewat bank untuk keperluan
 *   organisasi). Konsekuensi praktisnya: LPJ uang muka menutup <i>piutang orang</i>, LPJ kas besar
 *   menutup <i>pengeluaran unit</i> — risiko penyalahgunaannya lebih langsung karena yang
 *   dipertanggungjawabkan adalah kas organisasi, bukan panjar pribadi yang tetap tercatat sebagai
 *   tanggungan si pegawai.</li>
 *   <li><b>{@code tanggalPersetujuanManual}</b> — ada di {@link Pertangungjawaban} (dan di
 *   {@link KasBesar}), <b>tidak ada</b> di kelas ini. Tidak ada mekanisme "tanggal persetujuan
 *   diketik manual" untuk LPJ kas besar.</li>
 *   <li><b>Aliran nomor agenda</b> — lihat catatan kuirk di
 *   {@link #getNomorSuratAlurKeuangan()}: dua sisi sistem memakai aliran nomor yang
 *   <b>berbeda</b> untuk dokumen yang sama.</li>
 *   <li><b>Kaki posting pengembalian</b> — LPJ uang muka punya layar
 *   {@code PostingPertangungjawabanPengembalianAction} yang benar-benar mengisi
 *   {@code postingHistoryPengembalian}; padanan untuk kas besar <b>tidak ada</b> (lihat bagian
 *   berikut).</li>
 *   <li><b>Siklus status satu arah.</b> Ini perbedaan perilaku yang paling mudah terlewat.
 *   {@code Pertangungjawaban.getStatus()} punya cabang turun — bila penyetujunya dicabut, status
 *   kembali ke "Pengajuan" — <i>dan</i> membaca penanda penolakan dari alur SOP
 *   ({@code disposisiEnd.getAlurSop().getPenolakanAdaDiSini()}). {@link #getStatus()} di kelas ini
 *   <b>hanya punya cabang naik</b>: sekali {@link #getDisetujuiOleh()} berisi, status terkunci di
 *   {@link #DISETUJU} dan penolakan dari alur SOP tidak pernah dibaca. Persetujuan LPJ kas besar
 *   yang keliru karenanya tidak dapat dibatalkan lewat model.</li>
 *   <li><b>Panjang kolom keterangan</b> — {@link Pertangungjawaban} memetakan
 *   {@code keterangan} dengan {@code columnDefinition = "text"}; di sini tidak, sehingga kolomnya
 *   memakai default {@code varchar(255)}. Keterangan panjang yang aman di LPJ uang muka dapat
 *   ditolak database di LPJ kas besar.</li>
 * </ul>
 * <p>Semua sisanya — {@code dariSponsor}, {@code namaSponsor}, {@code dikembalikan},
 * {@code telahDikembalikan}, {@code tanggalDikembalikan}, {@code tanggalStor}, {@code pajak},
 * {@code formula}, ketiga field {@code PostingHistory}, {@code bulan}/{@code tahun},
 * {@code kodeUnik}, {@code daftarPengajuanTransfer}, {@code tanggalTransaksi} — sama persis di
 * kedua kelas. Karena itu <b>perbaikan pada salah satu hampir selalu perlu dikerjakan dua kali</b>;
 * tidak ada kelas dasar bersama yang menampung logikanya.</p>
 *
 * <h4>Dokumen "ber-kaki-ganda" — TIGA cap posting, tetapi hanya SATU yang hidup</h4>
 * <p>Entity ini membawa tiga kolom {@code PostingHistory} — {@link #getPostingHistory()},
 * {@link #getPostingHistoryPajak()}, {@link #getPostingHistoryPengembalian()} — yang masing-masing
 * dimaksudkan sebagai penanda "dokumen ini sudah dijurnal lewat kaki tersebut". Hasil penelusuran
 * seluruh repositori:</p>
 * <ul>
 *   <li><b>Kaki utama ({@code postingHistory}) — HIDUP.</b> Diisi di enam titik
 *   {@code PostingPertangungjawabanKasBesarAction} (posting per dokumen, posting massal dari layar,
 *   dan posting massal dari dasbor jurnal), dan dikosongkan kembali oleh ketiga jalur
 *   pembatalannya. Inilah satu-satunya kaki yang benar-benar menerbitkan
 *   {@code GrupTransaksi}/{@code Transaksi} untuk LPJ kas besar.</li>
 *   <li><b>Kaki pajak ({@code postingHistoryPajak}) — MATI.</b>
 *   {@link #setPostingHistoryPajak(PostingHistory)} tidak pernah dipanggil dari mana pun di luar
 *   berkas ini; kolomnya selalu {@code NULL}. Pajak dari LPJ ini tetap dijurnal, tetapi lewat
 *   dokumen {@link Pajak} tersendiri yang dibuat {@code Pajak.buat(null, this, jsonObject, null)}
 *   dan dijurnal oleh layar posting pajak — jadi capnya menempel di {@code Pajak}, bukan di
 *   sini.</li>
 *   <li><b>Kaki pengembalian ({@code postingHistoryPengembalian}) — MATI.</b>
 *   {@link #setPostingHistoryPengembalian(PostingHistory)} juga tidak pernah dipanggil dari luar.
 *   Padanannya untuk LPJ uang muka <i>sudah</i> diimplementasikan
 *   ({@code PostingPertangungjawabanPengembalianAction} mengisi
 *   {@code Pertangungjawaban.postingHistoryPengembalian} di enam titik), tetapi untuk kas besar
 *   layar itu tidak ada. Nilai {@link #getDikembalikan()} <i>tetap</i> masuk pembukuan — ia
 *   diselipkan sebagai baris debet di dalam <b>jurnal kaki utama</b> — tetapi tidak punya cap
 *   posting sendiri, sehingga tidak ada cara membedakan "sudah dijurnal sebagai pengembalian"
 *   dari "sudah dijurnal sebagai belanja".</li>
 * </ul>
 * <p><b>Kenapa ini penting untuk pemeliharaan:</b> mesin pembatalan posting sengaja menyaring
 * {@code ref IS NULL} agar hanya kaki utama yang terhapus. Komentar di
 * {@code PostingPertangungjawabanKasBesarAction.batalkanPostingSemua} menyatakannya eksplisit —
 * begitu salah satu kaki mati di atas diimplementasikan, pembatalan tanpa saringan akan ikut
 * melenyapkan jurnal kaki tersebut. Jadi ketiga field ini <b>bukan</b> field yatim yang boleh
 * dihapus: dua di antaranya adalah tempat duduk yang sudah dipesan dan sudah diperhitungkan oleh
 * mesin pembatalan.</p>
 *
 * <h4>PENTING — hampir tidak ada getter di kelas ini yang "getter polos"</h4>
 * <p>Pemetaannya memakai <b>property access</b> ({@code @Id} dipasang di {@link #getId()}), jadi
 * <b>Hibernate membaca state entity lewat getter</b> saat dirty-checking dan flush. Setiap efek
 * samping di dalam getter karenanya <b>ikut tersimpan ke database</b> untuk instance yang masih
 * <i>attached</i> — tanpa satu baris {@code setX()} pun di kode pemanggil, dan tanpa layar
 * persetujuan apa pun dilewati. Ditambah {@code dynamicUpdate = true}, hanya kolom yang berubah
 * yang ikut di-{@code UPDATE}, sehingga perubahan diam-diam ini sulit terlihat di log.</p>
 * <p><b>Getter yang benar-benar polos</b> (hanya {@code return field;} atau normalisasi
 * {@code null} tanpa menulis balik): {@link #getOlehId()}, {@link #getOleh()},
 * {@link #getTanggal_dirubah()}, {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 * {@link #getKeterangan()}, {@link #getNilai()}, {@link #getKasBesar()}, {@link #getFormula()},
 * {@link #getPostingHistory()}, {@link #getDariSponsor()}, {@link #getNamaSponsor()},
 * {@link #getDikembalikan()}, {@link #getDaftarPengajuanTransfer()},
 * {@link #getPostingHistoryPajak()}, {@link #getTelahDikembalikan()},
 * {@link #getPostingHistoryPengembalian()}, {@link #getPajak()}.</p>
 * <p><b>Getter yang menulis balik ke field (dan karenanya berpotensi ke DB):</b>
 * {@link #getAktif()}, {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 * {@link #getTanggalPersetujuan()}, {@link #getTanggalPembuatan()}, {@link #getStatus()},
 * {@link #getSatuanKerja()}, {@link #getKodeUnik()}, {@link #getDisposisiSop()},
 * {@link #getTahun()}, {@link #getNomorSuratAlurKeuangan()}, {@link #getBulan()},
 * {@link #getTanggalTransaksi()}, {@link #getTanggalDikembalikan()}, dan
 * {@link #getTanggalStor()} — 15 dari 34 getter. Tiga di antaranya menyentuh <b>data yang
 * bernilai uang atau bukti setoran</b>: {@link #getTanggalStor()} bisa <b>menghapus</b> tanggal
 * setor, {@link #getTanggalDikembalikan()} bisa <b>menerbitkan</b> tanggal pengembalian dari jam
 * dinding, dan {@link #getDisetujuiOleh()} bisa <b>menghapus</b> identitas penyetuju. Rinciannya
 * ada di Javadoc masing-masing.</p>
 *
 * <h4>Status dokumen: field {@code status} hanyalah bayangan dari {@link DisposisiSop}</h4>
 * <p>Tiga konstanta {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK} adalah satu-satunya
 * nilai yang dikenal. Namun {@link #getStatus()} <b>tidak sekadar membaca kolomnya</b>: ia
 * menurunkan status dari {@link #getDisetujuiOleh()}, yang sendirinya diturunkan dari
 * {@code disposisiSop.getDisposisiSetuju().getDiajukanOleh()}. Alur SOP-lah pemilik kebenaran;
 * kolom {@code status} hanya cache yang ditulis ulang setiap kali getter dibaca. Akibat
 * turunannya dibahas di {@link #setStatus(String)} — penolakan yang disimpan bisa berbalik
 * sendiri menjadi "Disetujui".</p>
 *
 * <h4>Pengelompokan anggota</h4>
 * <ol>
 *   <li><b>Jejak audit yang dideklarasikan ulang</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #getId()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas dokumen</b> — {@link #getKode()}, {@link #getKodeUnik()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #getNomorSuratAlurKeuangan()}, {@link #toString()}.</li>
 *   <li><b>Isi pertanggungjawaban</b> — {@link #getFormula()} (rincian JSON), {@link #getNilai()},
 *   {@link #getPajak()}, {@link #getDariSponsor()}, {@link #getNamaSponsor()},
 *   {@link #getBulan()}, {@link #getTahun()}.</li>
 *   <li><b>Dokumen hulu &amp; cakupan</b> — {@link #getKasBesar()}, {@link #getSatuanKerja()}.</li>
 *   <li><b>Alur persetujuan</b> — {@link #getDisposisiSop()}, {@link #getStatus()},
 *   {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()},
 *   {@link #getTanggalPersetujuan()}, {@link #getAktif()}.</li>
 *   <li><b>Setoran sisa dana</b> — {@link #getDikembalikan()}, {@link #getTanggalStor()},
 *   {@link #getTelahDikembalikan()}, {@link #getTanggalDikembalikan()}.</li>
 *   <li><b>Hilir (pembayaran &amp; jurnal)</b> — {@link #getDaftarPengajuanTransfer()},
 *   {@link #getTanggalTransaksi()}, {@link #getPostingHistory()},
 *   {@link #getPostingHistoryPajak()}, {@link #getPostingHistoryPengembalian()}.</li>
 * </ol>
 * <p>Kelas ini <b>tidak punya satu pun method bisnis, pabrik statis, atau query statis</b> — tidak
 * ada {@code reloadDefault()}, {@code ambilDefault()}, {@code buat()}, maupun {@code hapus()}
 * seperti pada {@link JenisKasBesar} / {@link NomorSuratAlurKeuangan} / {@link Pajak}. Seluruh
 * "logika bisnis" yang ada hidup <b>di dalam getter</b>, dan itulah sebabnya bagian
 * "getter tidak polos" di atas begitu panjang. Satu-satunya anggota statis adalah tiga konstanta
 * status dan {@link #DEFAULT_FORMULA}.</p>
 *
 * <h4>Kenapa field induk dideklarasikan ulang di sini (BUKAN bug)</h4>
 * <p>Rantai pewarisannya {@code PertangungjawabanKasBesar} → {@link DataSop} →
 * {@link ais.database.model.GeneralValueObject}, dan <b>tidak satu pun dari kedua induk itu
 * beranotasi {@code @Entity} atau {@code @MappedSuperclass}</b> — keduanya POJO abstrak biasa.
 * Konsekuensinya Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu {@code id},
 * {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta getter/setter-nya <b>harus</b>
 * dideklarasikan ulang di setiap entity konkret agar kolom-kolom audit tetap tersimpan. Duplikasi
 * ini keharusan teknis, bukan kelalaian: jangan "dirapikan" dengan menghapusnya. Yang benar-benar
 * diwarisi dan dipakai dari induk hanyalah utilitas statis
 * {@link ais.database.model.GeneralValueObject#check(Object) check(...)} (de-proxy /
 * pemuatan ulang object yang sudah <i>detached</i>) dan kontrak abstrak {@code getDisposisiSop()} /
 * {@code setDisposisiSop(...)} dari {@link DataSop}.</p>
 *
 * <h4>Catatan pengamanan yang perlu diketahui perawat kode</h4>
 * <p>Semuanya diverifikasi dari kode pemanggil, bukan dugaan. Ditulis di sini karena entity ini
 * memegang <b>nominal kas fisik</b>, sehingga perawat kode perlu tahu apa yang <i>tidak</i>
 * dijaga:</p>
 * <ul>
 *   <li><b>Gerbang persetujuan hanyalah kehadiran radio status di layar — dan itu dapat dinyalakan
 *   dari URL.</b> {@code PertangungjawabanKasBesarAction.onSave} menetapkan
 *   {@code setDisetujuiOleh(tbmuser)} — <i>pengguna sesi saat itu</i> — begitu pilihan status
 *   bernilai "Disetujui". Tidak ada pemeriksaan peran, atasan, batas nominal, maupun larangan
 *   menyetujui pengajuan sendiri di titik tersebut, dan
 *   {@code CommonPrivilages.checkPrevilages(APPROVE)} tidak pernah dipanggil di layar itu (hanya
 *   READ/CREATE/UPDATE/DELETE). Yang menentukan tampil-tidaknya pilihan status adalah bendera
 *   {@code persetujuan}, yang di {@code doAfterCompose} <b>dapat ditimpa parameter URL</b>
 *   {@code ?persetujuan=true}. Bandingkan: layar <i>posting</i>-nya justru memakai
 *   {@code checkPrevilages(APPROVE)} dengan benar.</li>
 *   <li><b>Jalur API POS dapat menyetujui dan mengubah nominal.</b>
 *   {@code PertangungjawabanKasBesarApiHelper} melayani aksi {@code pj_kas_besar_setujui}
 *   (memanggil {@link #setStatus(String)} + {@link #setDisetujuiOleh(Tbmuser)}) dan {@code simpan}
 *   ({@link #setNilai(Double)}, {@link #setPajak(Double)}, {@link #setDikembalikan(Double)},
 *   {@link #setDariSponsor(Double)}). Endpoint-nya <b>tidak anonim</b> (token wajib), tetapi
 *   penjaga hak aksinya <i>fail-open</i>: pengguna tanpa peran dianggap boleh melakukan apa
 *   saja.</li>
 *   <li><b>Cakupan satuan kerja fail-open.</b> Kueri daftar
 *   ({@code PertangungjawabanKasBesarAction.initCriteria}) membungkus filter satuan kerja dengan
 *   {@code Restrictions.or(Restrictions.isNull("satuanKerja"), …)} dan jatuh ke {@code 1=1} bila
 *   himpunan satuan kerja pengguna kosong. Dokumen yang {@code satuanKerja}-nya {@code NULL}
 *   terlihat oleh <b>semua</b> pengguna. Perhatikan {@link #getSatuanKerja()}: nilai itu tidak
 *   diisi sendiri melainkan <i>disalin dari</i> {@link #getKasBesar()}, jadi kas besar tanpa satuan
 *   kerja langsung menghasilkan LPJ yang global. Jalur API POS tidak menyaring tenant sama sekali —
 *   penyaringnya hanya parameter yang dikirim klien. Dasbor
 *   {@code DasboardPertangungjawabanKasBesar} pun menghitung agregat tanpa filter dan menyimpannya
 *   di cache ber-scope tetap {@code "ADMIN"} sehingga hasilnya dibagi lintas pengguna.</li>
 *   <li><b>Membuka layar daftar saja sudah bisa menulis nominal.</b> Renderer baris di layar
 *   posting menghitung ulang total dari rincian lalu memanggil {@link #setNilai(Double)} pada
 *   entity yang masih attached — {@code UPDATE} terbit pada flush berikutnya tanpa satu pun
 *   tombol ditekan.</li>
 *   <li><b>Pembatalan posting lewat layar meninggalkan baris jurnal yatim.</b> Kedua tombol batal
 *   di layar posting hanya menghapus {@code akunting.grup_transaksi}; baris
 *   {@code akunting.transaksi} anaknya dibiarkan dan tetap terhitung di buku besar. Hanya jalur
 *   API yang menghapus keduanya. Karena cap {@link #getPostingHistory()} ikut dikosongkan,
 *   dokumen dapat diposting ulang dan menerbitkan set baris kedua.</li>
 *   <li><b>Tombol "Hitung Ulang" menulis ulang nominal secara massal.</b> Tombol di layar daftar
 *   memuat sampai 5000 dokumen sesuai filter aktif, menghitung ulang {@code nilai}/{@code pajak}
 *   dari {@link #getFormula()}, lalu menulisnya langsung ke DB — <b>tanpa memeriksa apakah dokumen
 *   sudah berstatus "Disetujui" atau sudah dijurnal</b>. Jurnal yang sudah terbit tidak ikut
 *   dihitung ulang, sehingga dokumen dan buku besar dapat berpisah nilai.</li>
 *   <li><b>Batas nilai LPJ diperiksa terhadap nilai penuh kas besar, bukan sisanya.</b>
 *   {@code onSave} menolak simpan bila {@code kasBesar.getNilai() < totalRincian}; tidak ada
 *   penjumlahan LPJ lain atas kas besar yang sama. Beberapa LPJ atas satu dokumen pencairan
 *   masing-masing boleh mencapai nilai penuh.</li>
 * </ul>
 *
 * @see KasBesar
 * @see Pertangungjawaban
 * @see JenisKasBesar
 * @see DisposisiSop
 * @see DaftarPengajuanTransfer
 * @see Pajak
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.GeneralValueObject#check(Object)
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "pertangungjawaban_kas_besar")
public class PertangungjawabanKasBesar extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya <b>identik</b> dengan milik {@link Pertangungjawaban},
	 * {@link KasBesar}, {@code KasKecil}, dan {@link JenisKasBesar} — jejak salin-tempel dari
	 * cetakan hbm2java yang sama. Tidak berbahaya (serialisasi Java memakai nama kelas juga), tapi
	 * jangan dijadikan penanda identitas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, kolom {@code id}. Dibangkitkan database ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini. Getter polos.
	 *
	 * @return id pengguna audit, dapat {@code null} untuk baris yang belum pernah diubah
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna audit, <b>menolak nilai kosong</b>.
	 *
	 * <p><b>Kasus tepi penting:</b> bila {@code olehId} bernilai {@code null} atau hanya berisi
	 * spasi, method langsung {@code return} tanpa mengubah apa pun. Artinya jejak audit
	 * <b>tidak dapat dikosongkan kembali</b> lewat setter ini — sekali terisi, nilainya hanya bisa
	 * digantikan oleh nilai lain yang tidak kosong. Ini disengaja: interceptor audit boleh
	 * dipanggil berulang tanpa risiko menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId id pengguna audit; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna audit, <b>menolak nilai kosong</b>.
	 *
	 * <p>Perilaku dan alasannya sama persis dengan {@link #setOlehId(String)}: {@code null} atau
	 * string kosong diabaikan diam-diam sehingga jejak audit tidak bisa dihapus lewat setter.</p>
	 *
	 * @param oleh nama pengguna audit; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini. Getter polos.
	 *
	 * @return nama pengguna audit, dapat {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: menyegarkan stempel audit sebelum setiap {@code UPDATE}.
	 *
	 * <p><b>Tujuan:</b> mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari pengguna sesi aktif.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> otomatis oleh penyedia JPA/Hibernate, tidak pernah dari kode
	 * aplikasi. Karena hanya {@code @PreUpdate} (tanpa {@code @PrePersist}), baris yang baru
	 * disimpan pertama kali mengandalkan nilai awal field {@link #tanggal_dirubah} — lihat
	 * deklarasinya.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga field audit entity ini.</p>
	 *
	 * <p><b>Catatan tentang baris ini:</b> deklarasi field {@code tanggal_dirubah} ditulis pada
	 * <b>baris fisik yang sama</b> dengan method ini — gaya penulisan hasil penyisipan otomatis,
	 * bukan kesalahan, dan sengaja tidak dipecah agar {@code svn blame} tetap terbaca. Field
	 * tersebut menyimpan stempel waktu perubahan terakhir dan <b>diinisialisasi ke waktu sekarang
	 * saat objek dibuat</b> ({@code WaktuUtil.getDate()}), sehingga baris baru sudah punya nilai
	 * walau method ini baru berjalan pada {@code UPDATE} berikutnya. Nilainya juga menjadi
	 * <i>sumber</i> {@link #getTanggalStor()} ketika tanggal setor belum pernah diisi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Setter polos, menerima {@code null}.
	 *
	 * <p><b>Peringatan:</b> nilai ini juga menjadi <i>sumber</i> {@link #getTanggalStor()} saat
	 * tanggal setor belum pernah diisi — mengubahnya bisa ikut menggeser tanggal setor yang
	 * dilaporkan.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Getter polos.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada objek yang dibuat
	 *         lewat konstruktor karena field-nya sudah diinisialisasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Status awal dokumen: LPJ sudah dibuat namun belum diputuskan. Nilai literal "Pengajuan". */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Status "Disetujui". Ejaan konstantanya sengaja tanpa huruf {@code i} terakhir
	 * ({@code DISETUJU}) walau <i>nilainya</i> "Disetujui" — jangan diperbaiki, konstanta ini
	 * dirujuk lintas modul.
	 */
	public static final String DISETUJU = "Disetujui";
	/** Status "Ditolak". Lihat {@link #setStatus(String)} untuk efek sampingnya. */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Representasi teks dokumen untuk combo/label ZK dan log.
	 *
	 * <p><b>Kuirk:</b> membaca <b>field</b> {@code nama} secara langsung, bukan
	 * {@link #getNama()}. Untuk dokumen yang namanya belum diisi, {@link #getNama()} akan
	 * mengembalikan nama kas besar induknya sedangkan method ini mengembalikan
	 * {@code "123-null"}. Perbedaan ini terlihat di dropdown yang memakai {@code toString()}.</p>
	 *
	 * @return gabungan {@code id + "-" + nama} apa adanya
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nomor agenda dokumen, dibangkitkan {@code generateCode(...)} di Action saat simpan pertama. */
	private String kode;
	/** Rincian belanja dalam bentuk teks JSONArray. Lihat {@link #getFormula()}. */
	private String formula;
	/** Judul pengajuan LPJ. Lihat {@link #getNama()} untuk perilaku fallback-nya. */
	private String nama;
	/** Keterangan bebas dari pembuat dokumen. */
	private String keterangan;
	/** Dokumen pencairan kas besar yang dipertanggungjawabkan. Wajib (kolom {@code NOT NULL}). */
	private KasBesar kasBesar;
	/** Total nilai yang dipertanggungjawabkan, hasil penjumlahan rincian {@link #formula}. */
	private Double nilai;
	/** Total pajak yang melekat pada rincian. Lihat {@link #getPajak()}. */
	private Double pajak;
	/** Sisa dana yang harus disetor kembali ke kas. Lihat {@link #getDikembalikan()}. */
	private Double dikembalikan;
	/** Penanda dokumen masih berlaku. Lihat {@link #getAktif()} — hanya bisa naik, tidak turun. */
	private Boolean aktif;
	/** Pembuat dokumen. Diturunkan dari langkah awal {@link DisposisiSop} bila ada. */
	private Tbmuser dibuatOleh;
	/** Penyetuju dokumen. Diturunkan dari langkah setuju {@link DisposisiSop} bila ada. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan. Diturunkan dari langkah setuju {@link DisposisiSop} bila ada. */
	private Date tanggalPersetujuan;
	/** Waktu pembuatan. Diturunkan dari langkah awal {@link DisposisiSop} bila ada. */
	private Date tanggalPembuatan;
	/** Cache status dokumen; kebenarannya dipegang {@link DisposisiSop}. Lihat {@link #getStatus()}. */
	private String status;
	/** Unit organisasi pemilik dokumen. <b>Selalu disalin dari</b> {@link #getKasBesar()}. */
	private SatuanKerja satuanKerja;
	/** Dana pihak ketiga yang ikut membiayai kegiatan. */
	private Double dariSponsor;
	/** Nama pihak ketiga penyumbang dana. */
	private String namaSponsor;
	/** Simpul alur persetujuan SOP; sumber kebenaran status/pembuat/penyetuju dokumen ini. */
	private DisposisiSop disposisiSop;

	/** Cap posting kaki UTAMA (belanja). Satu-satunya kaki yang benar-benar dipakai. */
	private PostingHistory postingHistory;
	/**
	 * Cap posting kaki PAJAK. <b>Tidak pernah diisi</b> untuk entity ini — pajak dijurnal lewat
	 * dokumen {@link Pajak} tersendiri. Lihat catatan "kaki-ganda" di Javadoc kelas.
	 */
	private PostingHistory postingHistoryPajak;
	/**
	 * Cap posting kaki PENGEMBALIAN (setoran sisa). <b>Tidak pernah diisi</b> untuk entity ini;
	 * padanannya hanya ada untuk LPJ uang muka. Lihat catatan "kaki-ganda" di Javadoc kelas.
	 */
	private PostingHistory postingHistoryPengembalian;
	/** Tahun buku dokumen; default tahun berjalan. Lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Aliran nomor surat/agenda dokumen. Lihat kuirk di {@link #getNomorSuratAlurKeuangan()}. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Bulan buku dokumen (1–12); default bulan berjalan. Lihat {@link #getBulan()}. */
	private Integer bulan;
	/** DPC pembayaran pengembalian sisa dana, bila ada. Lihat {@link #getDaftarPengajuanTransfer()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;
	/** Tanggal transaksi untuk jurnal; diturunkan dari DPC. Lihat {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/** Penanda sisa dana sudah benar-benar diterima kembali oleh kas. */
	private Boolean telahDikembalikan;
	/** Tanggal sisa dana diterima kembali. Lihat {@link #getTanggalDikembalikan()}. */
	private Date tanggalDikembalikan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Tidak melakukan apa pun selain inisialisasi field default (khususnya
	 * {@link #tanggal_dirubah} yang langsung terisi waktu sekarang). Seluruh pengisian nilai
	 * dilakukan lewat setter oleh {@code PertangungjawabanKasBesarAction} atau
	 * {@code PertangungjawabanKasBesarApiHelper}.</p>
	 */
	public PertangungjawabanKasBesar() {
	}

	/**
	 * Mengembalikan kunci utama dokumen. Getter polos.
	 *
	 * <p><b>Catatan pemetaan:</b> {@code @Id} dipasang di getter ini, yang menetapkan seluruh
	 * kelas memakai <b>property access</b> — inilah alasan efek samping di getter lain ikut
	 * ter-flush ke database. {@code insertable = false} karena nilainya dibangkitkan kolom
	 * {@code IDENTITY} di sisi database.</p>
	 *
	 * @return id baris, {@code null} selama dokumen belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Setter polos.
	 *
	 * <p>Hanya dipakai Hibernate saat memuat/menyimpan baris; kode aplikasi tidak boleh
	 * memanggilnya karena mengubah identitas baris yang sudah persisten.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor agenda dokumen dalam bentuk sudah dirapikan.
	 *
	 * <p>String kosong/berisi spasi dinormalkan menjadi {@code null}, selebihnya dikembalikan
	 * hasil {@code trim()}. <b>Tidak menulis balik</b> ke field, jadi normalisasi ini murni pada
	 * nilai balik.</p>
	 *
	 * <p><b>Dipakai oleh:</b> {@link #getKodeUnik()} (sebagai potongan pertama kunci unik) dan
	 * {@code DaftarPengajuanTransfer} saat menurunkan kode DPC pengembalian.</p>
	 *
	 * @return nomor agenda tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menetapkan nomor agenda dokumen. Setter polos.
	 *
	 * <p><b>Kapan dipanggil:</b> {@code PertangungjawabanKasBesarAction.onSave} memanggilnya dua
	 * kali — sekali dengan isi textbox, lalu sekali lagi dengan nomor agenda hasil
	 * {@code generateCode(...)} khusus untuk dokumen baru.</p>
	 *
	 * @param kode nomor agenda baru; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul dokumen, dengan <b>fallback ke nama kas besar induknya</b>.
	 *
	 * <p><b>Cara kerja:</b> bila field {@code nama} masih {@code null}, method mengembalikan
	 * {@code getKasBesar().getNama()} (atau string kosong bila kas besar juga belum diisi);
	 * selain itu mengembalikan {@code nama.trim()}.</p>
	 *
	 * <p><b>Hal non-obvious:</b> field-nya <b>tidak</b> ditulis balik, tetapi kolomnya dipetakan
	 * {@code nullable = false} dengan property access. Artinya nilai turunan inilah yang benar-benar
	 * <b>tersimpan ke kolom</b> {@code nama} saat flush — dokumen yang judulnya dibiarkan kosong
	 * akan permanen memakai nama kas besar induk di database, sementara {@link #toString()} (yang
	 * membaca field) tetap menampilkan {@code null}.</p>
	 *
	 * <p><b>Kasus tepi:</b> memanggil ini akan memicu pemuatan relasi {@link #getKasBesar()}
	 * ({@code FetchMode.SELECT}) — satu kueri tambahan per dokumen saat merender daftar.</p>
	 *
	 * @return judul dokumen; nama kas besar induk bila judul kosong; string kosong bila keduanya
	 *         kosong. Tidak pernah {@code null}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {

		return this.nama == null ? (getKasBesar() == null ? "" : getKasBesar().getNama()) : this.nama.trim();
	}

	/**
	 * Menetapkan judul dokumen. Setter polos.
	 *
	 * @param nama judul baru; {@code null} mengaktifkan fallback di {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas dokumen. Getter polos (tanpa normalisasi maupun penyaringan).
	 *
	 * <p><b>Catatan:</b> tidak ada filter XSS di sini; pembersihan diserahkan kepada komponen ZK
	 * yang menampilkannya.</p>
	 *
	 * <p><b>Beda dengan kembarannya:</b> {@link Pertangungjawaban} memetakan kolom yang sama
	 * dengan {@code columnDefinition = "text"}; di sini anotasinya tanpa itu, sehingga kolom
	 * memakai default {@code varchar(255)}. Keterangan panjang yang aman di LPJ uang muka dapat
	 * ditolak database di LPJ kas besar.</p>
	 *
	 * @return keterangan, dapat {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen. Setter polos.
	 *
	 * @param keterangan teks keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda dokumen aktif — <b>dengan efek samping menaikkannya menjadi
	 * {@code true} untuk dokumen yang sudah disetujui</b>.
	 *
	 * <p><b>Cara kerja:</b> memanggil {@link #getStatus()}; bila hasilnya {@link #DISETUJU},
	 * field {@code aktif} <b>ditulis</b> menjadi {@code true}. Barulah nilai balik dihitung dengan
	 * default {@code true} untuk field yang masih {@code null}.</p>
	 *
	 * <p><b>Pola "satu arah" — kebalikan dari {@link KasBesar#getAktif()}.</b> Pada
	 * {@link KasBesar} getter serupa hanya bisa menurunkan {@code aktif} menjadi {@code false};
	 * di sini getter hanya bisa menaikkannya menjadi {@code true} dan tidak punya satu pun cabang
	 * yang menulis {@code false}. Karena {@code aktif} dipetakan ke kolom dengan property access,
	 * penulisan itu ikut ter-flush permanen. Akibatnya <b>LPJ kas besar yang sudah disetujui tidak
	 * dapat dinonaktifkan</b>: {@code setAktif(false)} apa pun akan dibatalkan lagi pada pembacaan
	 * getter berikutnya — termasuk pembacaan yang hanya untuk menggambar checkbox di layar. Ini
	 * relevan karena kueri daftar menyaring {@code aktif IS NULL OR aktif = true}, sehingga
	 * "menyembunyikan" dokumen yang salah setelah disetujui praktis mustahil lewat jalur ini.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@link #getStatus()} sendiri bukan getter polos — memanggil
	 * {@link #getAktif()} ikut memicu penurunan status dari {@link DisposisiSop} beserta seluruh
	 * efek sampingnya.</p>
	 *
	 * @return {@code true} bila dokumen dianggap aktif (juga untuk field yang masih {@code null})
	 */
	public Boolean getAktif() {

		if (getStatus().equals(PertangungjawabanKasBesar.DISETUJU)) {
			aktif = true;
		}

		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda dokumen aktif. Setter polos.
	 *
	 * <p><b>Peringatan:</b> untuk dokumen berstatus {@link #DISETUJU}, nilai {@code false} yang
	 * ditetapkan di sini akan ditimpa kembali menjadi {@code true} oleh {@link #getAktif()} —
	 * lihat penjelasan pola satu-arah di sana.</p>
	 *
	 * @param aktif penanda baru; {@code null} berarti "belum ditentukan" dan dibaca sebagai
	 *              {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan total nilai yang dipertanggungjawabkan, dengan {@code null} dinormalkan ke
	 * {@code 0.0}. Getter polos (tidak menulis balik).
	 *
	 * <p><b>Dari mana nilainya:</b> bukan diketik langsung. {@code PertangungjawabanKasBesarAction}
	 * menjumlahkan seluruh baris {@link #getFormula()} dengan rumus per baris
	 * {@code jumlah + (ppn% × jumlah) − (pph bila konfigurasi pph_mengurangi_lpj aktif)}, lalu
	 * memanggil {@link #setNilai(Double)}. Nilai yang sama juga ditulis ulang secara massal oleh
	 * tombol "Hitung Ulang" di layar daftar.</p>
	 *
	 * <p><b>Dipakai oleh:</b> validasi batas di {@code onSave} (dibandingkan dengan
	 * {@code kasBesar.getNilai()} — nilai <i>penuh</i> kas besar, bukan sisanya), perhitungan
	 * {@link #getDikembalikan()} pada jalur hitung ulang, seluruh laporan LPJ kas besar, dan
	 * nominal debet belanja pada jurnal kaki utama.</p>
	 *
	 * <p><b>Hal non-obvious:</b> nilai ini juga <b>ditulis ulang dari jalur render</b>. Renderer
	 * baris di layar posting menghitung ulang total dari rincian lalu memanggil
	 * {@link #setNilai(Double)} pada entity yang masih attached, sehingga sekadar membuka daftar
	 * posting dapat menerbitkan {@code UPDATE} pada kolom {@code nilai}. Selain itu ketiga jalur
	 * posting tidak seragam: dua jalur UI menghitung ulang nilai dari rincian sebelum menjurnal,
	 * sedangkan jalur API memakai nilai kolom apa adanya — dokumen yang sama bisa menghasilkan
	 * nominal jurnal berbeda tergantung jalur mana yang dipakai.</p>
	 *
	 * @return total nilai LPJ; {@code 0.0} bila belum dihitung
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menetapkan total nilai yang dipertanggungjawabkan. Setter polos, <b>tanpa validasi</b>.
	 *
	 * <p><b>Kasus tepi:</b> nilai negatif diterima apa adanya; tidak ada pemeriksaan terhadap
	 * {@code kasBesar.getNilai()} di level entity — pemeriksaan itu hanya ada di layar
	 * ({@code onSave}) dan karenanya <b>tidak berlaku</b> untuk jalur API maupun tombol
	 * "Hitung Ulang".</p>
	 *
	 * @param nilai total nilai baru; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Menetapkan pembuat dokumen. Setter polos.
	 *
	 * <p><b>Kapan dipanggil:</b> {@code onSave} mengisinya dengan pengguna sesi ketika dokumen
	 * belum punya pembuat, dan sekali lagi tepat sebelum {@code session.save(...)} untuk dokumen
	 * baru.</p>
	 *
	 * @param dibuatOleh pengguna pembuat dokumen
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pembuat dokumen, <b>dengan penulisan balik dari alur SOP</b>.
	 *
	 * <p><b>Cara kerja:</b> (1) melakukan de-proxy/muat-ulang lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}; (2) bila
	 * {@link #getDisposisiSop()} punya langkah awal ({@code getDisposisiStart()}) dengan pengaju
	 * yang diketahui, field {@code dibuatOleh} <b>ditimpa</b> oleh pengaju tersebut.</p>
	 *
	 * <p><b>Efek samping:</b> penimpaan itu adalah penulisan ke field yang dipetakan, jadi untuk
	 * instance yang masih attached nilainya ikut ter-{@code UPDATE} ke kolom {@code dibuat_oleh}.
	 * Alur SOP dengan demikian menjadi sumber kebenaran; nilai yang pernah diisi
	 * {@link #setDibuatOleh(Tbmuser)} akan kalah.</p>
	 *
	 * <p><b>Kasus tepi:</b> berbeda dengan {@link #getDisetujuiOleh()}, method ini <b>tidak</b>
	 * mengosongkan field ketika alur SOP ada tapi langkah awalnya belum terisi — nilai lama
	 * dipertahankan.</p>
	 *
	 * @return pengguna pembuat dokumen; dapat {@code null} bila belum pernah diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menetapkan penyetuju dokumen. Setter polos.
	 *
	 * <p><b>Kapan dipanggil — dan mengapa ini titik paling sensitif di kelas ini:</b>
	 * {@code PertangungjawabanKasBesarAction.onSave} memanggilnya dengan <b>pengguna sesi saat
	 * itu</b> begitu combo status pada layar bernilai "Disetujui", dan dengan {@code null} untuk
	 * status lain. Di titik tersebut <b>tidak ada</b> pemeriksaan peran, atasan, batas nominal,
	 * maupun larangan menyetujui pengajuan sendiri; {@code CommonPrivilages.APPROVE} tidak pernah
	 * diperiksa di layar itu. Pemisahan pembuat/penyetuju sepenuhnya bergantung pada tampil atau
	 * tidaknya pilihan status, yaitu pada bendera {@code persetujuan} milik layar — bendera yang
	 * pada {@code doAfterCompose} <b>dapat ditimpa dari parameter URL</b> {@code ?persetujuan=true},
	 * bukan hanya oleh subkelas {@code PersetujuanPertangungjawabanKasBesarAction} yang memanggil
	 * {@code super(true)}.</p>
	 *
	 * <p>Dipanggil juga oleh jalur API POS (aksi {@code pj_kas_besar_setujui}) yang penjaga hak
	 * aksinya <i>fail-open</i> untuk pengguna tanpa peran.</p>
	 *
	 * <p>Dipanggil juga oleh {@link #setStatus(String)} dengan {@code null} ketika status yang
	 * ditetapkan adalah {@link #DITOLAK}.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju; {@code null} untuk mencabut persetujuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan penyetuju dokumen, <b>dengan penulisan balik DAN penghapusan dari alur
	 * SOP</b>.
	 *
	 * <p><b>Cara kerja — tiga langkah:</b></p>
	 * <ol>
	 *   <li>De-proxy/muat ulang lewat
	 *   {@link ais.database.model.GeneralValueObject#check(Object)}.</li>
	 *   <li>Bila {@link #getDisposisiSop()} punya langkah setuju ({@code getDisposisiSetuju()})
	 *   dengan pengaju yang diketahui, field {@code disetujuiOleh} <b>ditimpa</b> oleh pengaju
	 *   tersebut.</li>
	 *   <li><b>Destruktif:</b> bila alur SOP ada tetapi langkah setujunya <i>belum</i> terisi,
	 *   field {@code disetujuiOleh} <b>ditulis {@code null}</b>.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang perlu diwaspadai:</b> langkah (3) berlaku juga untuk dokumen yang
	 * penyetujunya pernah diisi langsung lewat layar/API tanpa melalui SOP. Begitu dokumen
	 * semacam itu dilekatkan pada sebuah {@link DisposisiSop} yang belum mencapai langkah setuju,
	 * cukup <b>membaca</b> getter ini — misalnya saat merender satu baris di grid — untuk
	 * menghapus identitas penyetuju secara permanen, dan bersamaan dengan itu memundurkan
	 * {@link #getStatus()} kembali ke {@link #PENGAJUAN} serta mengosongkan
	 * {@link #getTanggalPersetujuan()}.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila dokumen belum/tidak lagi disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		return disetujuiOleh;
	}

	/**
	 * Menetapkan waktu persetujuan. Setter polos.
	 *
	 * <p><b>Kapan dipanggil:</b> dari {@code onSave} bersamaan dengan
	 * {@link #setDisetujuiOleh(Tbmuser)}, dan dari {@link #setStatus(String)} dengan {@code null}
	 * saat status {@link #DITOLAK}.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan; {@code null} untuk mengosongkan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan, <b>diturunkan dari alur SOP</b> dan <b>tahan
	 * {@code LazyInitializationException}</b>.
	 *
	 * <p><b>Cara kerja:</b> mengikuti pola yang sama dengan {@link #getDisetujuiOleh()} —
	 * bila langkah setuju SOP terisi, {@code tanggalPersetujuan} <b>ditimpa</b> oleh
	 * {@code disposisiSetuju.getWaktu()}; bila alur SOP ada tetapi langkah setujunya belum terisi,
	 * field <b>ditulis {@code null}</b>. Keduanya adalah penulisan ke field terpetakan sehingga
	 * bisa ikut ter-flush.</p>
	 *
	 * <p><b>Penanganan error:</b> seluruh blok dibungkus {@code try/catch} yang mencatat
	 * exception ke {@code ErrorAuditUtil} lalu melanjutkan. Alasannya didokumentasikan pada
	 * komentar di dalam kode: {@link #getDisposisiSop()} dapat mengembalikan instance
	 * canonical/bersama milik {@code AuditTimestampInterceptor} yang proxy-nya terikat ke
	 * {@code Session} lain yang sudah tertutup. Tanpa penjagaan ini, sekadar menggambar kolom
	 * "tanggal persetujuan" di grid bisa membuat seluruh halaman gagal render.</p>
	 *
	 * <p><b>Kasus tepi:</b> karena exception ditelan, nilai balik pada kondisi tersebut adalah
	 * <i>nilai kolom apa adanya</i> — belum tentu mencerminkan alur SOP terkini. Jadi tanggal yang
	 * tampil bisa "tertinggal" tanpa tanda apa pun di layar.</p>
	 *
	 * @return waktu persetujuan, atau {@code null} bila dokumen belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getDisposisiSop() bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke
			// Session lain yang sudah closed -> jangan biarkan getter ini crash, cukup
			// lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/PertangungjawabanKasBesar.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan waktu pembuatan dokumen. Setter polos.
	 *
	 * <p><b>Kapan dipanggil:</b> {@code onSave} mengisinya dengan waktu sekarang hanya ketika
	 * dokumen belum punya pembuat.</p>
	 *
	 * @param tanggalPembuatan waktu pembuatan; boleh {@code null}
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan waktu pembuatan dokumen, <b>diturunkan dari alur SOP</b> dengan fallback
	 * "sekarang".
	 *
	 * <p><b>Cara kerja:</b> bila langkah awal SOP ({@code getDisposisiStart()}) terisi, field
	 * {@code tanggalPembuatan} <b>ditimpa</b> oleh {@code disposisiStart.getWaktu()}. Nilai balik
	 * kemudian di-default ke {@code new Date()} bila field masih {@code null}.</p>
	 *
	 * <p><b>Hal non-obvious yang penting:</b> default {@code new Date()} itu <b>tidak</b> ditulis
	 * ke field, tetapi karena kolom {@code tanggal_pembuatan} dipetakan dengan property access,
	 * <b>nilai jam-dinding itulah yang tersimpan ke database</b> pada flush pertama. Kolom ini
	 * dipakai sebagai kunci filter rentang tanggal di layar daftar
	 * ({@code date(this_.tanggal_pembuatan) between …}) dan menjadi fallback terakhir
	 * {@link #getTanggalTransaksi()} — yaitu tanggal jurnal. Dengan kata lain: dokumen yang tidak
	 * pernah diberi tanggal pembuatan eksplisit akan memakai <i>waktu penyimpanan</i>, bukan
	 * tanggal kejadian sesungguhnya.</p>
	 *
	 * <p><b>Kasus tepi:</b> tidak seperti {@link #getTanggalPersetujuan()}, method ini
	 * <b>tidak</b> dibungkus {@code try/catch} — akses SOP yang detached tetap bisa melempar
	 * {@code LazyInitializationException} dari sini.</p>
	 *
	 * @return waktu pembuatan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan status dokumen, <b>diturunkan ulang dari penyetuju setiap kali dibaca</b>.
	 *
	 * <p><b>Cara kerja:</b> memanggil {@link #getDisetujuiOleh()}; bila hasilnya bukan
	 * {@code null}, field {@code status} <b>ditulis</b> menjadi {@link #DISETUJU}. Nilai balik
	 * kemudian di-default ke {@link #PENGAJUAN} untuk field yang kosong.</p>
	 *
	 * <p><b>Efek berantai:</b> karena {@link #getDisetujuiOleh()} sendiri bisa menimpa <i>dan</i>
	 * mengosongkan penyetuju berdasarkan {@link DisposisiSop}, satu pembacaan {@code getStatus()}
	 * dapat memicu perubahan pada tiga kolom sekaligus ({@code disetujui_oleh}, {@code status},
	 * dan lewat {@link #getAktif()} juga {@code aktif}).</p>
	 *
	 * <p><b>HANYA ADA CABANG NAIK — beda dari kembarannya.</b>
	 * {@code Pertangungjawaban.getStatus()} (LPJ uang muka) punya cabang turun yang mengembalikan
	 * status ke {@link #PENGAJUAN} ketika penyetujunya dicabut, <i>dan</i> membaca penanda
	 * penolakan dari alur SOP. Method ini tidak punya keduanya: sekali {@code disetujuiOleh}
	 * berisi, kolom {@code status} terkunci pada {@link #DISETUJU} dan penolakan yang dicatat di
	 * alur SOP tidak pernah diterjemahkan menjadi {@link #DITOLAK}. Persetujuan yang keliru tidak
	 * dapat dibatalkan lewat model — satu-satunya jalan adalah mengubah {@code disposisiSop}
	 * sehingga {@link #getDisetujuiOleh()} mengosongkan dirinya sendiri.</p>
	 *
	 * <p><b>Konsekuensi turunan:</b> status {@link #DITOLAK} yang disimpan lewat
	 * {@link #setStatus(String)} <b>tidak stabil</b> — lihat penjelasan lengkapnya di sana.</p>
	 *
	 * <p><b>Dipakai oleh:</b> {@link #getAktif()}, {@code onSave} (untuk memutuskan pembuatan DPC
	 * pengembalian), layar posting, dasbor, dan filter status di layar daftar.</p>
	 *
	 * @return {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK}; tidak pernah
	 *         {@code null} maupun string kosong
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		}
		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menetapkan status dokumen, <b>dengan pembersihan jejak persetujuan saat status
	 * {@link #DITOLAK}</b>.
	 *
	 * <p><b>Cara kerja:</b> bila {@code status} sama dengan {@link #DITOLAK}, method memanggil
	 * {@code setDisetujuiOleh(null)} dan {@code setTanggalPersetujuan(null)} lebih dulu, baru
	 * menyimpan nilai status. Untuk nilai lain, hanya field {@code status} yang diubah.</p>
	 *
	 * <p><b>KASUS TEPI PENTING — penolakan dapat berbalik sendiri.</b> Pembersihan di atas hanya
	 * menyentuh <i>field</i>, bukan {@link DisposisiSop}. Bila dokumen tertaut pada alur SOP yang
	 * langkah setujunya sudah terisi, pembacaan {@link #getStatus()} berikutnya akan memanggil
	 * {@link #getDisetujuiOleh()}, mengisi ulang penyetuju dari SOP, lalu menulis kembali
	 * {@code status = }{@link #DISETUJU}. Dokumen yang baru saja ditolak dengan demikian dapat
	 * tampil — dan tersimpan — sebagai disetujui tanpa ada tindakan pengguna. Menolak dokumen
	 * secara bermakna harus dilakukan di tingkat alur SOP, bukan lewat setter ini saja.</p>
	 *
	 * <p><b>Kasus tepi lain:</b> nilai {@code null} diterima dan disimpan apa adanya;
	 * {@link #getStatus()} akan membacanya sebagai {@link #PENGAJUAN}. Tidak ada validasi bahwa
	 * argumen merupakan salah satu dari ketiga konstanta — string sembarang akan tersimpan dan
	 * membuat filter status di layar daftar tidak pernah menjangkaunya.</p>
	 *
	 * @param status status baru; sebaiknya salah satu dari {@link #PENGAJUAN}, {@link #DISETUJU},
	 *               {@link #DITOLAK}
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	/**
	 * Mengembalikan dokumen pencairan {@link KasBesar} yang dipertanggungjawabkan. Getter polos.
	 *
	 * <p><b>Catatan pemetaan:</b> kolom {@code kas_besar} bersifat {@code nullable = false} —
	 * ini satu-satunya relasi wajib pada entity ini. {@code FetchMode.SELECT} (bukan
	 * {@code LAZY}) berarti relasi dimuat lewat {@code SELECT} terpisah begitu entity ini dimuat;
	 * pada daftar berisi banyak baris ini menghasilkan pola N+1.</p>
	 *
	 * <p><b>Kenapa penting:</b> nilai kas besar inilah plafon validasi di {@code onSave}, sumber
	 * {@link #getSatuanKerja()}, dan fallback {@link #getNama()}. Sebaliknya
	 * {@code KasBesar.pertangungjawabanKasBesar} menunjuk balik ke sini lewat kolom terpisah —
	 * bukan pasangan {@code mappedBy}, sehingga kedua arah harus dijaga manual.</p>
	 *
	 * @return dokumen kas besar induk; secara skema tidak boleh {@code null} untuk baris yang
	 *         sudah tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_besar", nullable = false)
	public KasBesar getKasBesar() {
		return kasBesar;
	}

	/**
	 * Menetapkan dokumen kas besar yang dipertanggungjawabkan. Setter polos.
	 *
	 * <p><b>Efek tidak langsung:</b> mengganti kas besar juga mengganti {@link #getSatuanKerja()}
	 * (yang selalu disalin dari sini) dan karenanya mengubah siapa saja yang dapat melihat dokumen
	 * ini di layar daftar. Tidak ada validasi bahwa kas besar tersebut sudah disetujui atau belum
	 * dipertanggungjawabkan dokumen lain.</p>
	 *
	 * @param kasBesar dokumen kas besar induk; wajib diisi sebelum simpan
	 */
	public void setKasBesar(KasBesar kasBesar) {
		this.kasBesar = kasBesar;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dokumen — <b>selalu disalin ulang dari kas besar
	 * induk</b>.
	 *
	 * <p><b>Cara kerja:</b> bila {@link #getKasBesar()} tidak {@code null}, field
	 * {@code satuanKerja} <b>ditimpa</b> oleh {@code kasBesar.getSatuanKerja()}; hanya bila kas
	 * besar belum diisi, nilai lama dipertahankan (setelah de-proxy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}).</p>
	 *
	 * <p><b>Efek samping:</b> penulisan tersebut ikut ter-flush ke kolom {@code satuan_kerja},
	 * jadi {@link #setSatuanKerja(SatuanKerja)} praktis tidak berpengaruh selama kas besar induk
	 * ada. Termasuk ketika kas besar induk sendiri belum punya satuan kerja: nilai {@code null}
	 * dari sana akan <b>menghapus</b> satuan kerja dokumen ini.</p>
	 *
	 * <p><b>Dampak cakupan akses:</b> kueri daftar di {@code PertangungjawabanKasBesarAction}
	 * menyaring dengan {@code Restrictions.or(Restrictions.isNull("satuanKerja"), …)}, sehingga
	 * dokumen bersatuan kerja {@code null} <b>terlihat oleh semua pengguna</b> lintas unit.
	 * Kombinasi kedua perilaku ini berarti kas besar tanpa satuan kerja secara otomatis
	 * menghasilkan LPJ yang tidak tersaring — periksa hulunya, bukan dokumen ini, bila menemui
	 * kebocoran cakupan.</p>
	 *
	 * @return satuan kerja pemilik dokumen; dapat {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getKasBesar() != null) {
			satuanKerja = getKasBesar().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik dokumen. Setter polos.
	 *
	 * <p><b>Peringatan:</b> nilai yang ditetapkan di sini akan ditimpa kembali oleh
	 * {@link #getSatuanKerja()} pada pembacaan berikutnya selama {@link #getKasBesar()} terisi.
	 * Praktis setter ini hanya berpengaruh untuk dokumen yang belum punya kas besar induk.</p>
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kunci unik dokumen yang selalu dihitung ulang. Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Tanggal setoran sisa dana ke kas. Lihat {@link #getTanggalStor()}. */
	private Date tanggalStor;

	/**
	 * Mengembalikan kunci unik dokumen — <b>dihitung ulang dan ditulis balik setiap kali
	 * dibaca</b>.
	 *
	 * <p><b>Cara kerja:</b> menyusun {@code getKode() + "_" + <id>} dengan {@code <id>} diambil
	 * dari {@code getDisposisiSop().getId()} bila alur SOP ada, atau dari {@link #getId()} bila
	 * tidak.</p>
	 *
	 * <p><b>Hal non-obvious:</b></p>
	 * <ul>
	 *   <li>Kolomnya {@code @Column(unique = true)} — nilai hasil hitungan inilah yang harus lolos
	 *   batasan unik database, bukan nilai yang pernah di-{@code set}.</li>
	 *   <li>{@link #getKode()} mengembalikan {@code null} untuk kode kosong, sehingga hasilnya
	 *   bisa berbentuk literal {@code "null_123"} — sah dan tetap unik karena id-nya ikut, tapi
	 *   membingungkan saat dibaca manusia.</li>
	 *   <li><b>Sumber id-nya berpindah:</b> dokumen yang semula belum bertaut SOP memakai id
	 *   dirinya sendiri, lalu <b>berubah kunci</b> begitu {@link DisposisiSop} dipasang. Nilai
	 *   lama tidak disimpan di mana pun.</li>
	 *   <li><b>Potensi tabrakan:</b> dua LPJ yang berbagi satu {@link DisposisiSop} <i>dan</i>
	 *   nomor agenda yang sama akan menghasilkan {@code kodeUnik} identik dan ditolak batasan
	 *   unik database saat flush — kegagalannya muncul jauh dari penyebabnya.</li>
	 * </ul>
	 *
	 * @return kunci unik dokumen; tidak pernah {@code null} (paling buruk berisi teks
	 *         {@code "null_null"} untuk dokumen yang belum tersimpan)
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menetapkan kunci unik dokumen. Setter polos — <b>praktis tidak berguna</b>.
	 *
	 * <p>Nilai apa pun yang ditetapkan di sini akan ditimpa oleh {@link #getKodeUnik()} pada
	 * pembacaan berikutnya, termasuk pembacaan yang dilakukan Hibernate saat flush. Setter ini
	 * ada semata agar kontrak JavaBean lengkap.</p>
	 *
	 * @param kodeUnik nilai yang akan segera ditimpa
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan simpul alur persetujuan SOP dokumen ini, dengan de-proxy/muat ulang.
	 *
	 * <p><b>Cara kerja:</b> melewatkan field ke
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan menyimpan hasilnya kembali
	 * ke field. Fungsi {@code check(...)} inilah yang menyelamatkan object yang sudah
	 * <i>detached</i> — bila perlu ia membuka sesi sendiri untuk memuat ulang.</p>
	 *
	 * <p><b>Kenapa method ini sentral:</b> lima getter lain
	 * ({@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
	 * {@link #getTanggalPembuatan()}, {@link #getKodeUnik()}) menurunkan nilainya dari sini.
	 * Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()}.</p>
	 *
	 * @return simpul disposisi SOP, atau {@code null} bila dokumen tidak melalui alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan simpul alur persetujuan SOP, <b>menolak nilai kosong</b>.
	 *
	 * <p><b>Cara kerja:</b> penjaga di awal baris menolak argumen {@code null} maupun argumen
	 * yang belum punya id — method langsung {@code return}. Artinya tautan SOP <b>tidak dapat
	 * dilepas kembali</b> lewat setter ini; sekali terpasang, ia hanya bisa diganti simpul lain
	 * yang sudah persisten. Ini penting karena tautan SOP-lah yang menentukan status, penyetuju,
	 * dan {@link #getKodeUnik()} dokumen.</p>
	 *
	 * <p><b>Kuirk — kode mati:</b> ekspresi ternary di badan method memeriksa ulang
	 * {@code disposisiSop == null || disposisiSop.getId() == null}, padahal penjaga di atas sudah
	 * memastikan keduanya tidak mungkin terjadi. Cabang "pertahankan nilai lama" karenanya tidak
	 * pernah terpilih dan pernyataan itu selalu setara dengan {@code this.disposisiSop =
	 * disposisiSop;}. Dibiarkan apa adanya — jangan disederhanakan tanpa menguji ulang perilaku
	 * alur SOP.</p>
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#setDisposisiSop(DisposisiSop)}.</p>
	 *
	 * @param disposisiSop simpul disposisi baru; {@code null} atau simpul tanpa id diabaikan
	 *                     diam-diam
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Nilai awal {@link #getFormula()} untuk dokumen tanpa rincian: teks JSONArray kosong
	 * ({@code "[]"}).
	 *
	 * <p><b>Kuirk:</b> {@code public static} tetapi <b>tidak {@code final}</b> — siapa pun dapat
	 * menimpanya dan mengubah rincian default seluruh dokumen baru dalam JVM ini. Pola yang sama
	 * ada di {@link KasBesar#DEFAULT_FORMULA} dan {@link Pertangungjawaban}.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan rincian belanja dalam bentuk teks JSONArray. Getter polos (tidak menulis
	 * balik) dengan default {@link #DEFAULT_FORMULA} untuk nilai kosong.
	 *
	 * <p><b>Bentuk datanya:</b> sebuah {@code JSONArray} berisi objek per baris belanja. Kunci
	 * yang benar-benar dibaca kode adalah {@code jumlah} (nominal), {@code ppn} (persen PPN), dan
	 * {@code pajak} (id {@code JenisPajakBarang} untuk PPh); kunci lain seperti {@code nama} dan
	 * {@code key} hanya dipakai untuk tampilan. Kolomnya bertipe {@code text} tanpa batas
	 * panjang.</p>
	 *
	 * <p><b>Siapa yang mengonsumsinya:</b></p>
	 * <ul>
	 *   <li>{@code PertangungjawabanKasBesarAction} — merender grid rincian, menghitung ulang
	 *   {@link #getNilai()} dan {@link #getPajak()} saat simpan, dan memanggil
	 *   {@code Pajak.buat(null, this, jsonObject, null)} untuk <b>menerbitkan dokumen
	 *   {@link Pajak}</b> per baris rincian.</li>
	 *   <li>Tombol "Hitung Ulang" di layar daftar — mengulang hal yang sama secara massal untuk
	 *   sampai 5000 dokumen sekaligus.</li>
	 * </ul>
	 *
	 * <p><b>Kasus tepi:</b> tidak ada validasi bentuk di sini. JSON yang rusak baru meledak di
	 * pemanggil ({@code new JSONArray(getFormula())}), dan pada jalur "Hitung Ulang" exception itu
	 * berada di luar {@code try} per-baris sehingga dapat menghentikan seluruh proses massal.</p>
	 *
	 * @return teks JSONArray rincian; {@code "[]"} bila belum ada rincian. Tidak pernah
	 *         {@code null}.
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? PertangungjawabanKasBesar.DEFAULT_FORMULA : formula;
	}

	/**
	 * Menetapkan rincian belanja dalam bentuk teks JSONArray. Setter polos, tanpa validasi bentuk.
	 *
	 * @param formula teks JSONArray; {@code null}/kosong mengaktifkan default
	 *                {@link #DEFAULT_FORMULA} di getter
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan cap posting <b>kaki utama</b> (jurnal belanja). Getter polos.
	 *
	 * <p><b>Semantiknya:</b> {@code null} berarti "dokumen ini belum dijurnal"; tidak {@code null}
	 * berarti sudah, dan objek {@link PostingHistory} yang ditunjuk menyimpan siapa yang memposting,
	 * kapan, dan keterangannya. Layar posting memakainya langsung sebagai filter — daftar
	 * "belum diposting" adalah {@code Restrictions.isNull("postingHistory")} dan daftar
	 * "sudah diposting" adalah kebalikannya.</p>
	 *
	 * <p><b>Siapa yang mengisinya:</b> hanya
	 * {@code PostingPertangungjawabanKasBesarAction} — pada posting per dokumen, posting massal
	 * dari layar, dan posting massal dari dasbor jurnal — serta dikosongkan kembali
	 * ({@code setPostingHistory(null)}) oleh ketiga jalur pembatalannya.</p>
	 *
	 * <p><b>Perhatian:</b> field ini <b>bukan</b> jurnalnya sendiri, melainkan hanya capnya.
	 * Jurnal sesungguhnya adalah baris {@code akunting.grup_transaksi} +
	 * {@code akunting.transaksi} yang menunjuk balik ke dokumen ini lewat kolom
	 * {@code pertangungjawaban_kas_besar}. Mengosongkan cap tanpa menghapus baris jurnal akan
	 * membuat dokumen bisa diposting dua kali.</p>
	 *
	 * @return cap posting kaki utama, atau {@code null} bila belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan (atau mengosongkan) cap posting kaki utama. Setter polos.
	 *
	 * <p><b>Kapan dipanggil:</b> enam titik di {@code PostingPertangungjawabanKasBesarAction} —
	 * tiga mengisi cap setelah jurnal terbit, tiga mengosongkannya pada pembatalan posting.</p>
	 *
	 * <p><b>Kontrak yang harus dijaga pemanggil — dan yang saat ini dilanggar:</b> pengosongan cap
	 * wajib disertai penghapusan baris {@code grup_transaksi} <i>beserta</i> baris
	 * {@code transaksi} anaknya. Dari tiga jalur pembatalan yang ada, hanya jalur API yang
	 * melakukan keduanya; <b>dua tombol batal di layar posting hanya menghapus header
	 * {@code grup_transaksi}</b> dan meninggalkan baris {@code transaksi} yatim yang tetap
	 * terhitung di buku besar. Karena cap ini sudah dikosongkan, dokumen yang sama dapat diposting
	 * ulang dan menerbitkan set baris kedua — nilai buku besar berganda. Ketiga jalur benar dalam
	 * hal saringannya ({@code ref IS NULL AND closing IS NULL}), sehingga hanya kaki utama yang
	 * tersentuh dan periode yang sudah ditutup tetap aman.</p>
	 *
	 * @param postingHistory cap posting baru, atau {@code null} untuk menandai "belum diposting"
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tahun buku dokumen, <b>dengan pengisian otomatis tahun berjalan</b>.
	 *
	 * <p><b>Cara kerja:</b> bila field masih {@code null}, ia <b>ditulis</b> dengan
	 * {@code WaktuUtil.getCalendar().get(Calendar.YEAR)} sebelum dikembalikan.</p>
	 *
	 * <p><b>Efek samping:</b> penulisan itu ikut ter-flush. Pengisian memakai <b>jam dinding saat
	 * dibaca</b>, bukan tanggal dokumen — dokumen lama yang baru pertama kali dibuka pada tahun
	 * berikutnya akan memperoleh tahun buku yang salah dan menyimpannya secara permanen.
	 * {@code WaktuUtil} dipakai (bukan {@code Calendar.getInstance()}) agar tunduk pada
	 * penyesuaian waktu terpusat aplikasi.</p>
	 *
	 * @return tahun buku dokumen; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun buku dokumen. Setter polos, tanpa validasi rentang.
	 *
	 * @param tahun tahun buku; {@code null} mengaktifkan pengisian otomatis di {@link #getTahun()}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan aliran nomor surat/agenda dokumen, dengan default statis.
	 *
	 * <p><b>Cara kerja:</b> bila field masih {@code null}, ia <b>ditulis</b> dengan konstanta
	 * statis {@code NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA}; selain itu nilai lama
	 * di-de-proxy lewat {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
	 *
	 * <p><b>KUIRK PENTING — aliran nomornya tidak cocok dengan dokumennya.</b> Kelas
	 * {@link NomorSuratAlurKeuangan} menyediakan <i>dua</i> konstanta terpisah:
	 * {@code PERTANGGUNGJAWABAN_DATA} untuk LPJ uang muka dan
	 * {@code PERTANGGUNGJAWABAN_KAS_BESAR_DATA} (kode "008") untuk LPJ kas besar. Getter ini
	 * memakai yang <b>pertama</b> — baris yang identik kata demi kata dengan
	 * {@link Pertangungjawaban}, jelas jejak salin-tempel. Sementara itu <i>pembangkit nomor
	 * agendanya</i> ({@code PertangungjawabanKasBesarAction.generateCode} dan
	 * {@code PertangungjawabanKasBesarApiHelper}) memakai yang <b>kedua</b> dengan benar.
	 * Akibatnya kolom {@code nomor_surat_alur_keuangan} dokumen ini menunjuk ke aliran surat
	 * <i>uang muka</i>, sementara nomor yang tercetak pada {@link #getKode()} berasal dari aliran
	 * <i>kas besar</i> — dua sisi yang tidak konsisten. Karena getter menulis balik, ketidakcocokan
	 * ini juga <b>tersimpan permanen</b> begitu dokumen dibaca sekali saja. Memperbaikinya
	 * berpengaruh pada dokumen lama, jadi perlu migrasi data, bukan sekadar ganti konstanta.</p>
	 *
	 * <p><b>Kasus tepi:</b> konstanta statis itu diisi saat startup oleh
	 * {@code NomorSuratAlurKeuangan.reloadDefault()}. Bila startup belum menjalankannya, field
	 * akan ditulis {@code null} dan pengisian otomatis dicoba lagi pada pembacaan berikutnya.</p>
	 *
	 * @return aliran nomor surat dokumen; dapat {@code null} bila konstanta statisnya belum
	 *         dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menetapkan aliran nomor surat/agenda dokumen. Setter polos.
	 *
	 * @param nomorSuratAlurKeuangan aliran nomor surat; {@code null} mengaktifkan default di
	 *                               {@link #getNomorSuratAlurKeuangan()}
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Mengembalikan bulan buku dokumen (1–12), <b>dengan pengisian otomatis bulan berjalan</b>.
	 *
	 * <p><b>Cara kerja:</b> bila field masih {@code null}, ia <b>ditulis</b> dengan
	 * {@code Calendar.MONTH + 1} — penambahan satu itu wajib karena {@code Calendar.MONTH}
	 * berbasis nol (Januari = 0). Nilai yang disimpan karenanya berbasis satu, sesuai harapan
	 * laporan.</p>
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getTahun()} — memakai jam dinding saat dibaca
	 * dan ikut ter-flush, sehingga dokumen lama yang baru pertama kali dibaca pada bulan lain akan
	 * memperoleh bulan buku yang salah secara permanen.</p>
	 *
	 * @return bulan buku dokumen dalam rentang 1–12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan bulan buku dokumen. Setter polos, <b>tanpa validasi rentang</b> — nilai di luar
	 * 1–12 diterima apa adanya.
	 *
	 * @param bulan bulan buku berbasis satu; {@code null} mengaktifkan pengisian otomatis di
	 *              {@link #getBulan()}
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan nominal dana pihak ketiga (sponsor) yang ikut membiayai kegiatan, dengan
	 * {@code null} dinormalkan ke {@code 0.0}. Getter polos.
	 *
	 * <p><b>Perannya dalam perhitungan:</b> pada jalur "Hitung Ulang", nilai ini
	 * <i>menambah</i> dana yang dianggap tersedia sehingga ikut memperbesar
	 * {@link #getDikembalikan()}: {@code dikembalikan = (nilai lama + dariSponsor) − nilai
	 * baru}. Artinya dana sponsor diperlakukan sebagai uang yang juga harus dipertanggungjawabkan
	 * atau disetor kembali, bukan sekadar catatan informatif.</p>
	 *
	 * <p><b>Kuirk:</b> meski ikut memperbesar kewajiban setor, nilai ini <b>tidak pernah dipakai
	 * mesin posting</b> — tidak ada kaki jurnal yang mencatat penerimaan dana sponsor. Secara
	 * pembukuan, dana sponsor hanya muncul lewat pengaruhnya pada
	 * {@link #getDikembalikan()}.</p>
	 *
	 * @return nominal dana sponsor; {@code 0.0} bila tidak ada
	 */
	public Double getDariSponsor() {
		return dariSponsor == null ? 0.0 : dariSponsor;
	}

	/**
	 * Menetapkan nominal dana sponsor. Setter polos, tanpa validasi.
	 *
	 * <p><b>Kapan dipanggil:</b> dari isian layar ({@code onSave}) dan dari jalur API
	 * ({@code PertangungjawabanKasBesarApiHelper}, dengan default {@code 0}).</p>
	 *
	 * @param dariSponsor nominal dana sponsor; boleh {@code null}
	 */
	public void setDariSponsor(Double dariSponsor) {
		this.dariSponsor = dariSponsor;
	}

	/**
	 * Mengembalikan nama pihak ketiga penyumbang dana, sudah dirapikan. Getter polos.
	 *
	 * <p>{@code null} dinormalkan menjadi string kosong dan spasi tepi dibuang, sehingga pemanggil
	 * dapat langsung memakai {@code isEmpty()} sebagai penanda "tidak ada sponsor" — itulah yang
	 * dilakukan renderer daftar untuk memutuskan menampilkan kolom sponsor atau tidak.</p>
	 *
	 * @return nama sponsor tanpa spasi tepi; string kosong bila tidak ada. Tidak pernah
	 *         {@code null}.
	 */
	public String getNamaSponsor() {
		return namaSponsor == null ? "" : namaSponsor.trim();
	}

	/**
	 * Menetapkan nama pihak ketiga penyumbang dana. Setter polos.
	 *
	 * @param namaSponsor nama sponsor; boleh {@code null}
	 */
	public void setNamaSponsor(String namaSponsor) {
		this.namaSponsor = namaSponsor;
	}

	/**
	 * Mengembalikan sisa dana yang harus disetor kembali ke kas, dengan {@code null} dinormalkan
	 * ke {@code 0.0}. Getter polos.
	 *
	 * <p><b>Dari mana nilainya — dua jalur berbeda:</b></p>
	 * <ol>
	 *   <li><b>Layar/API:</b> nilai diambil dari isian pengguna dan disimpan apa adanya lewat
	 *   {@link #setDikembalikan(Double)}. {@code onSave} hanya mewajibkan
	 *   {@link #getTanggalStor()} diisi bila nilainya &gt; 0.1 — tidak ada pemeriksaan bahwa
	 *   nilainya konsisten dengan selisih pencairan dan realisasi.</li>
	 *   <li><b>Tombol "Hitung Ulang":</b> dihitung sebagai
	 *   {@code (nilai lama + dariSponsor) − nilai hasil hitung ulang}. Rumus ini memakai nilai
	 *   <i>LPJ</i> yang lama, bukan nilai <i>kas besar</i> yang dicairkan — sehingga hasilnya
	 *   hanya bermakna pada perhitungan pertama. Pada perhitungan berikutnya, ketika nilai lama
	 *   dan nilai baru sudah sama, rumusnya menyusut menjadi sekadar {@code dariSponsor} dan
	 *   dapat <b>menimpa nilai pengembalian yang sebelumnya sudah benar</b>.</li>
	 * </ol>
	 *
	 * <p><b>Konsumen hilir:</b> {@code DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar}
	 * membuat DPC "Pembayaran pengembalian kas besar …" hanya bila nilai ini &gt; 0.1 dan dokumen
	 * sudah punya penyetuju; nominal DPC tersebut diambil langsung dari sini. Nilai ini juga
	 * menjadi sakelar {@link #getTanggalStor()}.</p>
	 *
	 * <p><b>Di jurnal:</b> berbeda dari dugaan yang wajar, pengembalian <b>tidak</b> dijurnal lewat
	 * {@link #getPostingHistoryPengembalian()} (kaki itu mati). Ia ikut masuk ke jurnal kaki utama
	 * sebagai baris <i>debet</i> pada akun cara pembayaran transfer. Kuirk yang perlu diketahui:
	 * pemeriksaan cabangnya dilakukan setelah pemotongan ke bilangan bulat, sehingga nilai
	 * pengembalian di bawah satu rupiah diperlakukan sama dengan nol dan hilang dari jurnal.</p>
	 *
	 * @return nominal sisa dana yang harus dikembalikan; {@code 0.0} bila tidak ada
	 */
	public Double getDikembalikan() {
		return dikembalikan == null ? 0.0 : dikembalikan;
	}

	/**
	 * Menetapkan nominal sisa dana yang harus disetor kembali. Setter polos, <b>tanpa
	 * validasi</b>.
	 *
	 * <p><b>Kasus tepi:</b> nilai negatif diterima. Karena ambang di seluruh kode adalah
	 * {@code > 0.1}, nilai negatif berperilaku sama dengan nol: tidak ada DPC pengembalian yang
	 * dibuat, tanggal stor dikosongkan getter-nya, dan tidak ada peringatan apa pun.</p>
	 *
	 * @param dikembalikan nominal sisa dana; boleh {@code null}
	 */
	public void setDikembalikan(Double dikembalikan) {
		this.dikembalikan = dikembalikan;
	}

	/**
	 * Mengembalikan DPC (daftar pengajuan transfer) yang membayarkan pengembalian sisa dana.
	 * Getter polos.
	 *
	 * <p><b>Siapa yang mengisinya:</b>
	 * {@code DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(this)} — dipanggil dari
	 * {@code onSave} sesudah dokumen berstatus {@link #DISETUJU}, dan dilewati bila dokumen sudah
	 * punya DPC, belum punya penyetuju, atau {@link #getDikembalikan()} tidak melebihi 0.1.
	 * Tombol "Hitung Ulang" juga menambal tautan ini untuk dokumen lama dengan mencari DPC aktif
	 * yang sudah menunjuk balik ke dokumen ini.</p>
	 *
	 * <p><b>Kenapa penting:</b> {@link #getTanggalTransaksi()} — tanggal yang dipakai jurnal —
	 * diturunkan dari DPC ini.</p>
	 *
	 * @return DPC pengembalian, atau {@code null} bila belum/tidak ada pengembalian
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menetapkan DPC pengembalian sisa dana. Setter polos.
	 *
	 * @param daftarPengajuanTransfer DPC pengembalian; boleh {@code null}
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Mengembalikan tanggal transaksi dokumen — <b>selalu diturunkan ulang dan ditulis balik</b>.
	 *
	 * <p><b>Cara kerja — tiga cabang berurutan:</b></p>
	 * <ol>
	 *   <li><b>Jalur transitori:</b> bila DPC-nya ditandai transitori dan proses transitorinya
	 *   ada, dipakai {@code prosesTransitori.getTanggalPembuatan()}.</li>
	 *   <li><b>Jalur transfer biasa:</b> bila DPC punya proses transfer, dipakai
	 *   {@code getTanggalRealisasikan()}; bila realisasi belum terjadi, mundur ke
	 *   {@code getTanggalPembuatan()} proses transfer tersebut.</li>
	 *   <li><b>Fallback:</b> {@link #getTanggalPembuatan()} dokumen ini.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> ketiga cabang <b>menulis</b> ke field {@code tanggalTransaksi},
	 * jadi nilai apa pun yang pernah ditetapkan {@link #setTanggalTransaksi(Date)} akan tergantikan
	 * — termasuk saat DPC belum direalisasikan, ketika tanggal "sementara" ikut tersimpan ke
	 * kolom.</p>
	 *
	 * <p><b>KUIRK PENTING — nilai ini dihitung tetapi tidak pernah dipakai menjurnal.</b> Namanya
	 * mengesankan "tanggal jurnal", dan secara akuntansi ia memang kandidat yang tepat karena
	 * menggambarkan <i>kapan kas benar-benar bergerak</i>. Namun
	 * {@code PostingPertangungjawabanKasBesarAction} sama sekali tidak memanggil getter ini: ketiga
	 * titik postingnya memakai {@link #getTanggalPersetujuan()} sebagai tanggal jurnal. Akibatnya
	 * jurnal LPJ kas besar jatuh pada <b>periode persetujuan administratif</b>, yang bisa berbeda
	 * bulan bahkan tahun buku dari periode realisasi kas yang dihitung di sini. Kolom
	 * {@code tanggal_transaksi} praktis menjadi kolom laporan saja — tetap ditulis ulang setiap
	 * kali getter dibaca, tetapi tidak memengaruhi buku besar.</p>
	 * <p>Perhatikan pula bahwa DPC yang ditautkan adalah DPC <i>pengembalian</i>, sehingga pada
	 * dokumen dengan pengembalian nilai yang dihitung di sini mengikuti tanggal setoran kembalinya,
	 * bukan tanggal belanja.</p>
	 *
	 * <p><b>Kasus tepi:</b> cabang pertama dan kedua mengakses rantai relasi yang cukup dalam
	 * ({@code DaftarPengajuanTransfer} → {@code TransitoriData} → {@code ProsesTransitori}) tanpa
	 * penjagaan {@code LazyInitializationException}; pemanggilan pada entity yang sudah detached
	 * dapat melempar exception.</p>
	 *
	 * @return tanggal realisasi kas untuk keperluan laporan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getTransitori()
				&& daftarPengajuanTransfer.getTransitoriData() != null
				&& daftarPengajuanTransfer.getTransitoriData().getProsesTransitori() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getTransitoriData().getProsesTransitori().getTanggalPembuatan();
		} else if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getProsesTransfer() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() == null
					? daftarPengajuanTransfer.getProsesTransfer().getTanggalPembuatan()
					: daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
		} else {
			tanggalTransaksi = getTanggalPembuatan();
		}
		return tanggalTransaksi;
	}

	/**
	 * Menetapkan tanggal transaksi. Setter polos — <b>praktis tidak berpengaruh</b>.
	 *
	 * <p>Nilai yang ditetapkan di sini akan ditimpa oleh {@link #getTanggalTransaksi()} pada
	 * pembacaan berikutnya, karena getter tersebut selalu menulis ulang ketiga cabangnya.</p>
	 *
	 * @param tanggalTransaksi tanggal transaksi yang akan segera ditimpa
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan cap posting <b>kaki pajak</b>. Getter polos.
	 *
	 * <p><b>PENTING — kaki ini tidak pernah dipakai untuk entity ini.</b> Penelusuran seluruh
	 * repositori menunjukkan {@link #setPostingHistoryPajak(PostingHistory)} tidak dipanggil dari
	 * mana pun di luar berkas ini, sehingga kolom {@code posting_history_pajak} selalu
	 * {@code NULL}. Pajak dari rincian LPJ ini <i>tetap</i> masuk pembukuan, tetapi lewat dokumen
	 * {@link Pajak} tersendiri yang diterbitkan {@code Pajak.buat(null, this, jsonObject, null)}
	 * dan dijurnal oleh layar posting pajak — capnya menempel di dokumen {@link Pajak} itu, bukan
	 * di sini.</p>
	 *
	 * <p><b>Jangan dihapus:</b> mesin pembatalan posting menyaring {@code ref IS NULL} justru
	 * dengan asumsi kaki ini suatu saat akan hidup; lihat catatan "kaki-ganda" pada Javadoc
	 * kelas.</p>
	 *
	 * @return selalu {@code null} pada keadaan kode saat ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_pajak", nullable = true)
	public PostingHistory getPostingHistoryPajak() {
		return postingHistoryPajak;
	}

	/**
	 * Menetapkan cap posting kaki pajak. Setter polos — <b>tidak pernah dipanggil dari luar berkas
	 * ini</b>.
	 *
	 * @param postingHistoryPajak cap posting kaki pajak
	 */
	public void setPostingHistoryPajak(PostingHistory postingHistoryPajak) {
		this.postingHistoryPajak = postingHistoryPajak;
	}

	/**
	 * Mengembalikan penanda bahwa sisa dana sudah benar-benar diterima kembali oleh kas, dengan
	 * {@code null} dinormalkan ke {@code false}. Getter polos.
	 *
	 * <p><b>Siapa yang mengisinya:</b> untuk entity ini, <b>tidak ada layar yang menyalakannya</b>.
	 * Padanannya pada LPJ uang muka ({@code PertangungjawabanPengembalianAction}) menyediakan
	 * checkbox untuk itu, tetapi layar tersebut hanya bekerja pada {@link Pertangungjawaban}.
	 * Satu-satunya pembaca nilai ini untuk kas besar adalah dasbor
	 * {@code DasboardPertangungjawabanKasBesar}, yang karenanya praktis selalu melihat
	 * {@code false}.</p>
	 *
	 * <p><b>Efek tidak langsung:</b> nilai {@code true} akan membuat
	 * {@link #getTanggalDikembalikan()} menerbitkan tanggal dari jam dinding.</p>
	 *
	 * @return {@code true} bila sisa dana sudah diterima kembali; {@code false} bila belum atau
	 *         belum ditentukan
	 */
	public Boolean getTelahDikembalikan() {
		return telahDikembalikan == null ? false : telahDikembalikan;
	}

	/**
	 * Menetapkan penanda sisa dana sudah diterima kembali. Setter polos.
	 *
	 * @param telahDikembalikan penanda baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setTelahDikembalikan(Boolean telahDikembalikan) {
		this.telahDikembalikan = telahDikembalikan;
	}

	/**
	 * Mengembalikan tanggal sisa dana diterima kembali — <b>menerbitkannya sendiri dari jam
	 * dinding bila belum ada</b>.
	 *
	 * <p><b>Cara kerja:</b> bila {@link #getTelahDikembalikan()} bernilai {@code true} dan field
	 * masih {@code null}, field <b>ditulis</b> dengan {@code WaktuUtil.getDate()} — waktu saat
	 * getter dipanggil.</p>
	 *
	 * <p><b>Peringatan (data bukti setoran):</b> karena kolom {@code tanggal_dikembalikan}
	 * dipetakan dengan property access, tanggal yang "ditemukan" ini <b>ikut tersimpan
	 * permanen</b>. Tanggal yang tercatat karenanya adalah <i>kapan seseorang kebetulan pertama
	 * kali membaca dokumen ini setelah penanda dinyalakan</i>, bukan kapan uangnya benar-benar
	 * diterima. Bila tanggal sesungguhnya penting, isilah lebih dulu lewat
	 * {@link #setTanggalDikembalikan(Date)}.</p>
	 *
	 * @return tanggal pengembalian, atau {@code null} selama sisa dana belum ditandai diterima
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dikembalikan")
	public Date getTanggalDikembalikan() {
		if (getTelahDikembalikan() && tanggalDikembalikan == null) {
			tanggalDikembalikan = WaktuUtil.getDate();
		}
		return tanggalDikembalikan;
	}

	/**
	 * Menetapkan tanggal sisa dana diterima kembali. Setter polos.
	 *
	 * <p>Mengisinya lebih dulu adalah satu-satunya cara mencegah {@link #getTanggalDikembalikan()}
	 * menerbitkan tanggal dari jam dinding.</p>
	 *
	 * @param tanggalDikembalikan tanggal pengembalian; boleh {@code null}
	 */
	public void setTanggalDikembalikan(Date tanggalDikembalikan) {
		this.tanggalDikembalikan = tanggalDikembalikan;
	}

	/**
	 * Mengembalikan cap posting <b>kaki pengembalian</b> (jurnal setoran sisa dana). Getter polos.
	 *
	 * <p><b>PENTING — kaki ini tidak pernah dipakai untuk entity ini.</b> Sama seperti
	 * {@link #getPostingHistoryPajak()}, {@link #setPostingHistoryPengembalian(PostingHistory)}
	 * tidak dipanggil dari mana pun di luar berkas ini; kolomnya selalu {@code NULL}. Bedanya,
	 * untuk LPJ uang muka kaki ini <b>sudah</b> hidup — {@code PostingPertangungjawabanPengembalianAction}
	 * mengisi {@code Pertangungjawaban.postingHistoryPengembalian} di enam titik. Padanan layar itu
	 * untuk kas besar belum ada, sehingga setoran sisa kas besar hanya terekam lewat DPC
	 * pengembalian ({@link #getDaftarPengajuanTransfer()}) dan tidak punya kaki jurnal
	 * sendiri di dokumen ini.</p>
	 *
	 * <p><b>Jangan dihapus:</b> komentar di
	 * {@code PostingPertangungjawabanKasBesarAction.batalkanPostingSemua} mengingatkan bahwa
	 * saringan {@code ref IS NULL} pada pembatalan posting sengaja dipasang untuk melindungi kaki
	 * ini begitu kelak diimplementasikan.</p>
	 *
	 * @return selalu {@code null} pada keadaan kode saat ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_pengembalian", nullable = true)
	public PostingHistory getPostingHistoryPengembalian() {
		return postingHistoryPengembalian;
	}

	/**
	 * Menetapkan cap posting kaki pengembalian. Setter polos — <b>tidak pernah dipanggil dari luar
	 * berkas ini</b>.
	 *
	 * @param postingHistoryPengembalian cap posting kaki pengembalian
	 */
	public void setPostingHistoryPengembalian(PostingHistory postingHistoryPengembalian) {
		this.postingHistoryPengembalian = postingHistoryPengembalian;
	}

	/**
	 * Mengembalikan total pajak yang melekat pada rincian, dengan {@code null} dinormalkan ke
	 * {@code 0.0}. Getter polos.
	 *
	 * <p><b>Dari mana nilainya:</b> dijumlahkan dari baris {@link #getFormula()} sebagai
	 * {@code Σ (JenisPajakBarang.getPersen() / 100 × jumlah)} — yaitu komponen PPh per baris.
	 * Perhatikan bahwa PPN <i>tidak</i> masuk ke sini; PPN ikut menambah {@link #getNilai()}.
	 * Konfigurasi {@code pph_mengurangi_lpj} menentukan apakah PPh dikurangkan dari
	 * {@link #getNilai()} atau tidak, tetapi tidak memengaruhi angka pada field ini.</p>
	 *
	 * <p><b>Hubungan dengan {@link Pajak}:</b> nilai ini hanyalah ringkasan. Dokumen setoran
	 * pajak yang sebenarnya diterbitkan per baris rincian oleh
	 * {@code Pajak.buat(null, this, jsonObject, null)} dan disimpan sebagai entity {@link Pajak}
	 * terpisah yang menunjuk balik ke dokumen ini.</p>
	 *
	 * <p><b>Kuirk:</b> mesin posting <b>tidak pernah membaca kolom ini</b>. Baris kredit pajak
	 * pada jurnal dihitung ulang langsung dari rincian {@link #getFormula()}, sehingga nilai di
	 * kolom ini dan nilai di jurnal dapat berbeda bila tarif master berubah setelah dokumen
	 * disimpan.</p>
	 *
	 * @return total pajak; {@code 0.0} bila belum dihitung
	 */
	public Double getPajak() {
		return pajak == null ? 0.0 : pajak;
	}

	/**
	 * Menetapkan total pajak. Setter polos, tanpa validasi.
	 *
	 * <p><b>Kapan dipanggil:</b> dari {@code onSave} (hasil penjumlahan rincian) dan dari tombol
	 * "Hitung Ulang" di layar daftar.</p>
	 *
	 * @param pajak total pajak baru; boleh {@code null}
	 */
	public void setPajak(Double pajak) {
		this.pajak = pajak;
	}
	
	/**
	 * Mengembalikan tanggal setoran sisa dana ke kas — <b>getter destruktif</b>.
	 *
	 * <p><b>Cara kerja — dua cabang, keduanya menulis:</b></p>
	 * <ol>
	 *   <li>Bila {@link #getDikembalikan()} tidak melebihi {@code 0.1} (termasuk nilai nol dan
	 *   negatif), field {@code tanggalStor} <b>ditulis {@code null}</b>.</li>
	 *   <li>Bila ada nilai yang harus dikembalikan tetapi tanggalnya masih kosong, field
	 *   <b>ditulis</b> dengan {@link #getTanggal_dirubah()} — stempel perubahan terakhir baris
	 *   ini.</li>
	 * </ol>
	 *
	 * <p><b>PERINGATAN (data bukti setoran, ikut ter-flush):</b></p>
	 * <ul>
	 *   <li>Cabang pertama <b>menghapus tanggal setor yang sudah tersimpan</b> begitu nilai
	 *   pengembalian turun ke nol — misalnya setelah tombol "Hitung Ulang" menyetel ulang
	 *   {@link #getDikembalikan()}. Penghapusan terjadi hanya dengan <i>membaca</i> dokumen,
	 *   tanpa jejak siapa yang melakukannya, dan {@code dynamicUpdate} membuat perubahannya sulit
	 *   terlihat.</li>
	 *   <li>Cabang kedua menghasilkan tanggal setor yang sebenarnya adalah <i>tanggal edit
	 *   terakhir dokumen</i>, bukan tanggal uang disetorkan. Karena {@code tanggal_dirubah}
	 *   diperbarui setiap {@code UPDATE} oleh {@link #onUpdate()}, tanggal yang "ditemukan" dapat
	 *   berbeda-beda tergantung kapan dokumen terakhir disentuh.</li>
	 * </ul>
	 *
	 * <p><b>Kontras dengan layar:</b> {@code PertangungjawabanKasBesarAction.onSave} mewajibkan
	 * pengguna mengisi tanggal stor bila {@link #getDikembalikan()} &gt; 0.1 — validasi itu
	 * menjadi sia-sia bila getter ini kemudian menghapusnya lagi karena nilai pengembalian
	 * berubah.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> {@code TemporalType.DATE} (tanpa jam), berbeda dari kolom
	 * tanggal lain di kelas ini yang memakai {@code TIMESTAMP}.</p>
	 *
	 * @return tanggal setoran sisa dana, atau {@code null} bila tidak ada yang harus disetor
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalStor() {
		if (getDikembalikan() < 0.1) {
			tanggalStor = null;
		} else if (tanggalStor == null) {
			tanggalStor = getTanggal_dirubah();
		}
		return tanggalStor;
	} 

	/**
	 * Menetapkan tanggal setoran sisa dana. Setter polos.
	 *
	 * <p><b>Kapan dipanggil:</b> dari isian datebox pada {@code onSave} dan dari jalur API.</p>
	 *
	 * <p><b>Peringatan:</b> nilai yang ditetapkan di sini akan dihapus lagi oleh
	 * {@link #getTanggalStor()} bila {@link #getDikembalikan()} tidak melebihi 0.1 — urutan
	 * pengisian karenanya penting: setel nilai pengembalian lebih dulu, baru tanggal setornya.</p>
	 *
	 * @param tanggalStor tanggal setoran; boleh {@code null}
	 */
	public void setTanggalStor(Date tanggalStor) {
		this.tanggalStor = tanggalStor;
	}
}
