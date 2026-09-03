package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.CommonVO;
import ais.database.model.JenisPengajuan;
import ais.database.model.KelompokParameterTambahanPengajuan;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>permohonan/pengajuan yang diajukan oleh (atau atas nama) seorang siswa</b>, dipetakan
 * ke tabel {@code public.pengajuan_siswa}.
 *
 * <h2>Makna persis entity ini — hasil VERIFIKASI dari kode, bukan tebakan dari nama</h2>
 *
 * <p>Tidak ada satu pun konstanta, enum, atau percabangan {@code if} di kelas ini yang mengunci
 * satu jenis permohonan tertentu (pindah sekolah, surat keterangan, dispensasi, dan seterusnya).
 * Jenis permohonan sepenuhnya ditentukan <b>data master</b> lewat relasi
 * {@link #getJenisPengajuan()} ke {@link JenisPengajuan} — tabel master berisi {@code nama},
 * konfigurasi penomoran surat ({@code NomorSurat}), berkas contoh/format pengajuan, template
 * JRXML pencetakan, dan daftar {@link KelompokParameterTambahanPengajuan} yang menentukan field
 * isian tambahan apa saja yang wajib diisi pemohon. Operator karena itu bisa menambah jenis
 * permohonan baru tanpa menyentuh kode.</p>
 *
 * <p><b>Petunjuk penggunaan nyata di lapangan.</b> Walaupun mesinnya generik, tanda-tanda di
 * layar menunjukkan pemakaian dominannya adalah <b>izin/cuti siswa</b> (khususnya konteks
 * asrama):</p>
 * <ul>
 *   <li>Layar {@code /pages/master/sekolah/pengajuan_siswa.zul} (menu "Pengajuan Siswa",
 *       {@code MenuSnapshotData} id {@code 570118}) memasang empat tab laporan yang seluruhnya
 *       bernama "…BerdasarkanIzinSiswa…"
 *       ({@code LaporanRekapitulasiBerdasarkanIzin}, {@code …IzinSiswa},
 *       {@code …IzinSiswaRekap}, {@code …IzinSiswaRekapTotal}), dan berkas laporannya
 *       dinamai {@code "Rekap_pengajuan_siswa"}.</li>
 *   <li>Judul jendela tambah di berkas {@code .zul} masih berbunyi
 *       "Tambah Pendaftaran Cuti Siswa" (sisa versi lama; saat dijalankan judul itu ditimpa
 *       {@code "Pengajuan Siswa"} oleh {@code PengajuanSiswaAction#init(...)}).</li>
 *   <li>Label form: "Keterangan / Alasan *", "Tanggal Permohonan", "Waktu Permohonan",
 *       "Tanggal Selesai", "Waktu Selesai", "No. Agenda", "Lampiran Pengajuan",
 *       "Status Persetujuan" — rentang tanggal + jam mulai/selesai adalah bentuk khas
 *       izin/dispensasi berdurasi, bukan sekadar surat sekali cetak.</li>
 *   <li>Dua template cetak tetap: {@code "Keterangan_Pengajuan"} (tombol "Permohonan") dan
 *       {@code "Persetujuan_Pengajuan"} (tombol "Persetujuan", hanya muncul bila
 *       {@link #getPersetujuan()} bernilai {@code true}), ditambah template JRXML bebas per
 *       jenis pengajuan lewat {@code LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN}.</li>
 * </ul>
 * <p>Kesimpulan: <b>bukan</b> entity "pengajuan pindah siswa" maupun "pengajuan surat" khusus,
 * melainkan <b>satu mesin permohonan siswa serba-guna berbasis data master</b> yang paling
 * sering dikonfigurasi sebagai izin/cuti.</p>
 *
 * <h2>Padanan di jenjang perguruan tinggi</h2>
 *
 * <p>Kelas ini adalah kembaran hampir kata-per-kata dari {@code ais.database.model
 * .PengajuanMahasiswa}. Keduanya dilayani listener yang sama,
 * {@code ais.action.master.helper.ParameterTambahanPengajuanListener}, yang memilih cabang
 * berdasarkan field mana yang tidak {@code null}. <b>Empat perbedaan nyata</b> yang sudah
 * diverifikasi dan penting saat menyunting salah satu sisi:</p>
 * <ol>
 *   <li>Kelas ini <b>tidak punya properti {@code semester}</b> beserta perhitungan ulangnya;
 *       konteks akademiknya hanya {@link #getTahunAkademik()}, {@link #getGanjilGenap()}, dan
 *       {@link #getTahap()}.</li>
 *   <li>{@link #getDisetujuiOleh()} dan {@link #getSetujuiTanggal()} di sini adalah getter
 *       <b>polos</b>; versi mahasiswa menurunkan keduanya dari simpul "setuju" pada
 *       {@link DisposisiSop}. Akibatnya kedua kolom di tabel ini praktis selalu {@code null} —
 *       lihat "Jejak persetujuan" di bawah.</li>
 *   <li>{@link #populateParameterTambahan(List)} di sini masih memuat empat pemanggilan
 *       {@code System.out.println} yang <b>aktif</b>; di versi mahasiswa baris-baris itu sudah
 *       dikomentari.</li>
 *   <li>{@link #getKeterangan()} di sini <b>tidak</b> memakai {@code columnDefinition = "text"},
 *       dan {@code ais.common.DatabaseTextColumnSchemaFix} hanya menyediakan
 *       {@code initPengajuanMahasiswa()} — tidak ada padanannya untuk {@code pengajuan_siswa}.
 *       Lihat catatan pada method tersebut.</li>
 * </ol>
 *
 * <h2>Rantai pewarisan &amp; pengulangan field yang DISENGAJA</h2>
 *
 * <p>{@code PengajuanSiswa} → {@link DataSop} → {@code ais.database.model.GeneralValueObject}.
 * {@link DataSop} hanya menambahkan kontrak abstrak {@code getDisposisiSop()}/
 * {@code setDisposisiSop(...)} sehingga seluruh dokumen ber-alur SOP dapat diperlakukan seragam
 * oleh mesin disposisi.</p>
 *
 * <p><b>PENTING — jangan "dibersihkan":</b>
 * {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa. Hibernate karena itu <b>tidak</b>
 * memetakan properti milik induk. Deklarasi ulang {@link #id}, {@link #oleh}, {@link #olehId},
 * dan {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi yang keliru</b>, melainkan
 * keharusan teknis agar kolom-kolom tersebut ikut terpetakan. Menghapusnya akan membuat kolom
 * audit dasar hilang dari tabel. Lihat {@link ais.database.model.GeneralValueObject} untuk
 * penjelasan lengkap pola ini.</p>
 *
 * <h2>Relasi utama (terverifikasi dari anotasi dan pemanggil)</h2>
 *
 * <ul>
 *   <li>{@link #getSiswa()} → {@link Siswa}, pemohon (kolom {@code siswa},
 *       {@code nullable = true} — baris tanpa siswa secara teknis mungkin ada, dan
 *       {@code PengajuanSiswaAction} akan melempar {@code NullPointerException} saat merender
 *       baris seperti itu karena renderer langsung memanggil {@code siswa.getNim()}).</li>
 *   <li>{@link #getJenisPengajuan()} → {@link JenisPengajuan}, penentu makna baris (kolom
 *       {@code jenis_pengajuan}, {@code nullable = false}).</li>
 *   <li>{@link #getDisposisiSop()} → {@link DisposisiSop}, simpul alur SOP berjalan.</li>
 *   <li>{@link #getDisetujuiOleh()} → {@link Tbmuser}, penyetuju (praktis tak pernah terisi di
 *       jenjang sekolah, lihat di bawah).</li>
 *   <li>Lampiran <b>tidak</b> dipetakan sebagai relasi Hibernate, melainkan dicari lewat
 *       {@link LampiranLain#ambil(Long, String)} dengan {@code ref = } id baris ini dan
 *       {@code jenis} berupa {@code "PengajuanSiswa"} (nama kelas) untuk lampiran utama, atau
 *       {@code "<kelompokId>-&gt;<parameterId>"} untuk lampiran per parameter tambahan.</li>
 *   <li><b>Tidak ada relasi {@code sekolah} maupun {@code yayasan}.</b> Cakupan tenant baris ini
 *       hanya bisa dicapai lewat {@code siswa.sekolah}/{@code siswa.yayasan} — konsekuensinya
 *       dijelaskan di "Catatan cakupan tenant".</li>
 * </ul>
 *
 * <h2>Jejak persetujuan: satu flag boolean TANPA identitas penyetuju</h2>
 *
 * <p>Ada tiga sumber status yang berbeda dan mudah tertukar:</p>
 * <ol>
 *   <li>{@link #getPersetujuan()} — flag boolean yang diisi operator dari kotak centang
 *       "Status Persetujuan" pada form; <b>defaultnya {@code true}</b> bila kolomnya
 *       {@code null}.</li>
 *   <li>{@link #getDisposisiSop()} — status menurut mesin alur SOP, bila jenis pengajuan
 *       tersebut memang dipasangi alur.</li>
 *   <li>{@link #getAktif()} — {@code false} bila disposisi tidak aktif <i>atau</i> alur berhenti
 *       di simpul penolakan.</li>
 * </ol>
 * <p><b>Celah jejak audit yang perlu disadari.</b> Kolom {@code disetuji_oleh} dan
 * {@code setujui_tanggal} ada di tabel dan dipetakan di kelas ini, tetapi penelusuran seluruh
 * repo menunjukkan {@code setDisetujuiOleh(...)}/{@code setSetujuiTanggal(...)} untuk entity ini
 * <b>tidak pernah dipanggil dari mana pun</b> (semua pemanggil yang ada milik entity akunting
 * seperti {@code KasKecil}, {@code KasBesar}, {@code DanaTalangan}), dan — berbeda dari kembaran
 * mahasiswanya — getter di sini tidak menurunkan nilainya dari SOP. Jadi persetujuan permohonan
 * siswa tersimpan hanya sebagai <b>satu bit tanpa siapa dan tanpa kapan</b>, sementara surat
 * "Persetujuan_Pengajuan" tetap dapat dicetak. Bila jenis pengajuannya tidak dipasangi alur SOP,
 * tidak ada catatan sama sekali tentang siapa yang menyetujui.</p>
 *
 * <h2>Penomoran agenda/surat</h2>
 *
 * <p>Empat properti bekerja sama: {@link #getKode()} (nomor agenda hasil format
 * {@code NomorSurat}), {@link #getIndex()} (nomor urut mentah), serta {@link #getTahun()} dan
 * {@link #getBulan()} yang <b>bukan sekadar informasi</b> melainkan kolom filter yang dipakai
 * {@code PengajuanSiswaAction#getindex(JenisPengajuan)} untuk aturan "reset urutan tiap
 * tahun/bulan". Entity ini tidak membangkitkan nomor sendiri; seluruh logikanya ada di action.</p>
 *
 * <p><b>Kuirk yang sudah terverifikasi di action</b> (dicatat di sini karena akibatnya terlihat
 * pada data entity ini):</p>
 * <ul>
 *   <li>{@code getindex(...)} menghitung <b>jumlah baris</b> ({@code Projections.rowCount()})
 *       lalu menambah satu — bukan mencari nomor terbit tertinggi. Menghapus satu permohonan
 *       membuat nomor berikutnya <b>kembar</b> dengan nomor yang sudah terbit. Ini pola yang
 *       sama persis dengan bug NIS kembar pada {@code FormatNis} (lihat tracker Javadoc).</li>
 *   <li>Nilai yang disimpan ke {@link #setIndex(Long)} adalah hasil {@code getindex(...)} yang
 *       <b>sudah</b> berisi cacah+1, lalu di-{@code ++} sekali lagi
 *       ({@code setIndex(++currentIndex)}), sedangkan {@link #setKode(String)} memakai nilai
 *       {@code getindex(...)} tanpa penambahan itu. Akibatnya angka pada kolom {@code index}
 *       selalu <b>satu lebih besar</b> daripada angka yang tercetak di dalam {@link #getKode()}.</li>
 *   <li>{@code getindex(...)} tidak memfilter sekolah/yayasan sama sekali, sehingga pencacahnya
 *       bersifat global seluruh instalasi — nomor agenda satu sekolah ikut melompat ketika
 *       sekolah lain membuat permohonan.</li>
 * </ul>
 *
 * <h2>Parameter tambahan dinamis</h2>
 *
 * <p>Field isian tambahan per jenis pengajuan tidak dipetakan sebagai kolom, melainkan disimpan
 * sebagai <b>dua kolom teks</b> berisi baris-baris yang dipisah {@code "\n"} dan kolom-kolom
 * yang dipisah {@code "&lt;=&gt;"}:</p>
 * <ul>
 *   <li>{@link #getParameterTambahan()} — versi <i>manusiawi</i>, 7 kolom:
 *       {@code namaKelompok->labelInputan <=> nilai <=> urlLampiran <=> nomorUrut <=>
 *       parameterId <=> kelompokId <=> keterangan}.</li>
 *   <li>{@link #getParameterTambahanInds()} — versi <i>berbasis id</i>, 4 kolom:
 *       {@code kelompokId->parameterId <=> nilai <=> urlLampiran <=> keterangan}. Dipakai saat
 *       membangun ulang form dan saat merender daftar, supaya isian lama terpasang kembali ke
 *       komponen yang tepat.</li>
 * </ul>
 * <p>Keduanya ditulis sekaligus oleh {@link #populateParameterTambahan(List)} dan dibaca kembali
 * oleh {@link #ambilDataParameterTambahan()}. Pola dua kolom kembar ini dipakai luas di repo
 * (mis. {@code CatatanMahasiswa}, {@code PerbaikanAsset}, {@code CutiDanIzin}).</p>
 *
 * <p><b>Tidak ada pelolosan (escaping) pemisah.</b> Nilai isian ditulis apa adanya di antara
 * pemisah {@code "\n"} dan {@code "&lt;=&gt;"}. Isian yang memuat salah satu pemisah tersebut
 * akan merusak struktur baris saat dibaca ulang — pemohon dapat memalsukan baris parameter
 * tambahan atau menimpa kolom URL lampiran hanya dengan mengetik pemisah itu di kotak isian.
 * Jangan menganggap hasil pembacaan kedua kolom ini tepercaya; validasi di sisi pemanggil.</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ol>
 *   <li><b>Jejak audit dasar</b> (deklarasi ulang wajib): {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #getId()}.</li>
 *   <li><b>Identitas permohonan</b>: {@link #getJenisPengajuan()}, {@link #getSiswa()},
 *       {@link #getKeterangan()}, {@link #getKode()}, {@link #getIndex()},
 *       {@link #toString()}.</li>
 *   <li><b>Konteks akademik</b>: {@link #getTahunAkademik()}, {@link #getGanjilGenap()},
 *       {@link #getTahap()}, {@link #getSemesterPendek()}.</li>
 *   <li><b>Waktu</b>: {@link #getTanggal()}, {@link #getTanggalSelesai()},
 *       {@link #getWaktuMulai()}, {@link #getWaktuSelesai()}, {@link #getTahun()},
 *       {@link #getBulan()}, {@link #toTglDanWaktu()}.</li>
 *   <li><b>Persetujuan/SOP</b>: {@link #getPersetujuan()}, {@link #getDisposisiSop()},
 *       {@link #getDisetujuiOleh()}, {@link #getSetujuiTanggal()}, {@link #getAktif()}.</li>
 *   <li><b>Parameter tambahan</b>: {@link #getParameterTambahan()},
 *       {@link #getParameterTambahanInds()}, {@link #ambilDataParameterTambahan()},
 *       {@link #populateParameterTambahan(List)}.</li>
 * </ol>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 *
 * <ul>
 *   <li><b>Getter kelas ini bukan getter polos.</b> Karena {@code @Id} dipasang di getter,
 *       Hibernate memakai <i>property access</i>: apa pun yang ditulis sebuah getter ke field
 *       ikut ter-flush ke database selama entity masih ter-attach. Getter yang menulis balik:
 *       {@link #getSiswa()}, {@link #getJenisPengajuan()}, {@link #getDisposisiSop()},
 *       {@link #getDisetujuiOleh()} (menulis balik hasil resolusi proxy),
 *       {@link #getPersetujuan()}, {@link #getTanggal()}, {@link #getWaktuMulai()},
 *       {@link #getParameterTambahan()}, {@link #getParameterTambahanInds()},
 *       {@link #getTahun()}, {@link #getBulan()}, {@link #getAktif()}.
 *       <b>Membaca daftar pengajuan di layar bisa mengubah isi tabel.</b>
 *       {@link #getTahunAkademik()} adalah satu-satunya pengecualian: ia mengembalikan nilai
 *       bawaan tanpa menyimpannya, sehingga kolom {@code tahun_akademik} bisa tetap {@code null}
 *       walaupun layar selalu menampilkan sebuah nilai.</li>
 *   <li><b>Setter yang menolak pengosongan.</b> {@link #setOleh(String)},
 *       {@link #setOlehId(String)}, dan {@link #setDisposisiSop(DisposisiSop)} mengabaikan
 *       argumen kosong/{@code null} secara diam-diam — nilai lama tidak bisa dihapus lewat
 *       setter.</li>
 *   <li><b>Tidak ada getter di kelas ini yang membuka atau menutup session Hibernate sendiri</b>
 *       (kelas ini tidak menyebut {@code Session} maupun {@code HibernateUtil} sama sekali).
 *       Pembukaan/penutupan session tersembunyi terjadi di dalam
 *       {@code GeneralValueObject#check(Object)} yang dipanggil getter relasi — lihat
 *       dokumentasi method tersebut untuk biaya dan jebakannya.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif, sehingga hanya kolom yang benar-benar
 *       berubah yang masuk ke SQL — ini memperkecil, tapi tidak menghilangkan, dampak efek
 *       samping getter di atas.</li>
 *   <li>{@code @Audited} (Hibernate Envers) menyalin setiap perubahan ke tabel bayangan
 *       {@code new_audit.pengajuan_siswa__audit}.</li>
 * </ul>
 *
 * <h2>Catatan cakupan tenant (fail-open) — konteks untuk pembaca masa depan</h2>
 *
 * <p>Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan}</b>. Semua
 * penyaringan multi-tenant karenanya bergantung pada pemanggil, dan hasil penelusuran
 * menunjukkan penyaringan itu bersifat <i>fail-open</i>:</p>
 * <ul>
 *   <li>{@code PengajuanSiswaAction#initCriteria(boolean)} menerjemahkan kombo "Yayasan",
 *       "Sekolah", "Tahun Akademik", "Semester", dan "Jenis Pengajuan" yang <b>kosong</b>
 *       menjadi {@code Restrictions.sqlRestriction("1=1")} — jadi bawaan layar adalah
 *       "tampilkan seluruh instalasi", bukan "tampilkan sekolah saya". Ini pola fail-open
 *       cakupan tenant yang sama seperti yang sudah berulang kali dicatat di repo ini.</li>
 *   <li>Pada Generic CRUD v2, {@code GenericCrudAutoEntityAdapter#scopeBindings()} hanya memasang
 *       restriksi properti {@code siswa} bila string peran pengguna yang sedang login memuat
 *       substring {@code "siswa"}. Untuk pengguna staf/admin sekolah, tidak ada satu pun
 *       properti whitelist yang cocok pada entity ini, sehingga jalur
 *       {@code list}/{@code get}/{@code export_xlsx}/{@code export_pdf} milik
 *       {@code webapp/WEB-INF/new/sekolah/services/pengajuan_siswa_service.jsp} berjalan tanpa
 *       batas tenant. Sifatnya sama dengan temuan terkonsolidasi pada tracker Javadoc; dicatat
 *       di sini agar penyunting berikutnya tidak menganggap entity ini aman hanya karena punya
 *       properti bernama {@code siswa}.</li>
 * </ul>
 *
 * @see JenisPengajuan
 * @see Siswa
 * @see DisposisiSop
 * @see ais.database.model.PengajuanMahasiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengajuan_siswa")
public class PengajuanSiswa extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya sengaja sama persis dengan
	 * {@code ais.database.model.PengajuanMahasiswa} (kedua kelas lahir dari generate Hibernate
	 * Tools yang sama); jangan diubah agar object yang sudah ter-serialisasi di session ZK lama
	 * tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, kolom {@code id}, dibangkitkan database ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (jejak audit sederhana). */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini (jejak audit sederhana). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (guard di awal method), sehingga nilai lama tidak bisa dikosongkan lewat setter ini. Ini
	 * pola seragam di seluruh entity repo agar interceptor audit tidak menghapus jejak yang
	 * sudah ada.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum
	 * {@code UPDATE} dijalankan, dan mendelegasikan pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Jangan dipanggil manual dari kode aplikasi. Perhatikan bahwa deklarasi field
	 * {@code tanggal_dirubah} sengaja ditempel pada baris yang sama oleh perkakas audit otomatis
	 * repo ini — nilai awalnya {@link WaktuUtil#getDate()} sehingga baris baru pun sudah punya
	 * stempel waktu sebelum sempat di-{@code update}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pengisian manual hanya dilakukan saat
	 * migrasi/impor data.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * <p>Selain sebagai jejak audit, nilai ini dipakai {@link #getTanggal()} sebagai pengganti
	 * bila tanggal permohonan kosong.</p>
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk object baru
	 *         karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini, dipakai antara lain sebagai label komponen ZK.
	 *
	 * <p><b>Kuirk:</b> mengembalikan field {@link #keterangan} apa adanya — jadi bisa
	 * mengembalikan {@code null} bila keterangan belum diisi (kolomnya {@code nullable}).
	 * Pemanggil yang merangkai string sebaiknya tidak menganggap hasilnya selalu non-null.
	 * Perhatikan juga bahwa yang dipakai adalah <b>field</b>, bukan {@link #getKeterangan()},
	 * sehingga tidak ada efek samping apa pun di sini.</p>
	 *
	 * @return isi keterangan/alasan permohonan, mungkin {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Membentuk kunci pengurutan gabungan tanggal + jam mulai + jam selesai + id, dalam bentuk
	 * satu string yang bisa dibandingkan secara leksikografis.
	 *
	 * <p>Formatnya adalah sambungan tanpa pemisah dari: tanggal {@code yyyyMMdd} (atau
	 * {@code "00000000"} bila {@link #getTanggal()} {@code null}), {@link #getWaktuMulai()} (atau
	 * {@code "00.00"}), {@link #getWaktuSelesai()} (atau {@code "00.00"}), lalu {@link #getId()}
	 * (atau {@code "0"}).</p>
	 *
	 * <p><b>Catatan hasil penelusuran:</b> pola ini disalin dari {@code Pertemuan#toTglDanWaktu()}
	 * yang di sana menjadi dasar {@code Comparator}. Di kelas ini method tersebut <b>tidak
	 * dipanggil dari mana pun</b> dan {@code PengajuanSiswa} juga tidak mengimplementasikan
	 * {@code Comparable}, sehingga praktis merupakan API yang belum terpakai. Selain itu, karena
	 * id disambung tanpa padding, urutan leksikografis akan salah begitu panjang digit id berbeda
	 * (mis. {@code "...9"} &gt; {@code "...10"}). Perbaiki bila suatu saat method ini benar-benar
	 * dipakai untuk mengurutkan.</p>
	 *
	 * <p><b>Efek samping tidak langsung:</b> method ini memanggil {@link #getTanggal()} dan
	 * {@link #getWaktuMulai()} yang keduanya menulis balik ke field — memanggilnya pada entity
	 * yang masih ter-attach dapat memicu {@code UPDATE}.</p>
	 *
	 * @return kunci pengurutan gabungan; tidak pernah {@code null}
	 */
	public String toTglDanWaktu() {
		String tgl = getTanggal() == null ? "00000000" : Common.dateFormat8.get().format(getTanggal());
		tgl += getWaktuMulai() == null ? "00.00" : getWaktuMulai();
		tgl += getWaktuSelesai() == null ? "00.00" : getWaktuSelesai();
		tgl += getId() == null ? "0" : getId().toString();
		return tgl;
	}

	/**
	 * Nomor agenda/surat hasil format {@code NomorSurat} milik {@link JenisPengajuan}; tampil di
	 * form sebagai label "No. Agenda".
	 */
	private String kode;

	/** Siswa pemohon. */
	private Siswa siswa;
	/** Tahap/termin permohonan, maknanya bebas per jenis pengajuan. */
	private Integer tahap;
	/** Uraian/alasan permohonan ("Keterangan / Alasan" pada form, wajib diisi). */
	private String keterangan;
	/** Tahun akademik, format {@code "2025/2026"}. */
	private String tahunAkademik;
	/** Penanda semester ganjil/genap; nilainya mengikuti konstanta {@code Perkuliahan}. */
	private String ganjilGenap;
	/** Flag persetujuan manual dari form (default {@code true} bila kolomnya {@code null}). */
	private Boolean persetujuan;
	/** Penanda bahwa permohonan berkaitan dengan Semester Pendek (SP). */
	private Boolean semesterPendek;
	/** Tanggal permohonan; diinisialisasi ke hari ini saat object dibuat. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal berakhir permohonan (mis. akhir masa izin/cuti). */
	private Date tanggalSelesai;
	/** Jenis permohonan — inilah yang menentukan makna baris ini. */
	private JenisPengajuan jenisPengajuan;
	/** Jam selesai, string format {@code HH.mm}. */
	private String waktuSelesai;
	/** Jam mulai, string format {@code HH.mm}. */
	private String waktuMulai;
	/** Simpul alur SOP yang sedang/terakhir memproses permohonan ini. */
	private DisposisiSop disposisiSop;
	/** Isian parameter tambahan versi manusiawi (7 kolom per baris). */
	private String parameterTambahan;
	/** Isian parameter tambahan versi berbasis id (4 kolom per baris). */
	private String parameterTambahanInds;
	/**
	 * Waktu persetujuan. Dipetakan ke kolom {@code setujui_tanggal}, tetapi <b>tidak pernah
	 * diisi</b> oleh kode aplikasi mana pun untuk entity ini.
	 */
	private Date setujuiTanggal;
	/**
	 * Pengguna penyetuju (perhatikan ejaan field yang kehilangan huruf "u" — kolomnya juga
	 * {@code disetuji_oleh}). Seperti {@link #setujuiTanggal}, <b>tidak pernah diisi</b> oleh
	 * kode aplikasi untuk entity ini.
	 */
	private Tbmuser disetujiOleh;
	/** Nomor urut mentah untuk penomoran agenda. */
	private Long index;
	/** Tahun acuan penomoran; diisi otomatis oleh {@link #getTahun()}. */
	private Integer tahun;
	/** Bulan acuan penomoran (1-12); diisi otomatis oleh {@link #getBulan()}. */
	private Integer bulan;
	/** Status aktif; dapat dipaksa {@code false} oleh {@link #getAktif()} berdasarkan SOP. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Perhatikan bahwa beberapa field sudah berisi nilai bawaan lewat inisialisasi field
	 * ({@link #tanggal} dan {@code tanggal_dirubah} diisi {@link WaktuUtil#getDate()}), jadi
	 * object baru tidak sepenuhnya kosong. {@code PengajuanSiswaAction#onAdd(...)} memanggil
	 * konstruktor ini untuk menyiapkan form permohonan baru.</p>
	 */
	public PengajuanSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolomnya {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}/sequence), jadi {@code null} berarti "belum pernah disimpan" — kondisi
	 * itu dipakai sebagai penanda "record baru" oleh {@link #getWaktuMulai()} dan oleh
	 * {@code PengajuanSiswaAction#onSave(...)} untuk memilih antara {@code save} dan
	 * {@code update}. Id ini juga menjadi {@code ref} pencarian lampiran lewat
	 * {@link LampiranLain#ambil(Long, String)}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Hanya dipakai Hibernate dan kode migrasi/impor.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nomor urut mentah penomoran agenda.
	 *
	 * <p>Diisi {@code PengajuanSiswaAction#onSave(...)} dari hasil
	 * {@code getindex(JenisPengajuan)} yang <b>sudah</b> berisi cacah+1, lalu ditambah satu lagi.
	 * Lihat "Penomoran agenda/surat" pada dokumentasi kelas untuk selisih satu angka yang timbul
	 * antara kolom ini dan {@link #getKode()}. Nilai {@code null} berarti baris lama yang dibuat
	 * sebelum fitur penomoran ada — action akan membangkitkan nomornya saat baris itu disimpan
	 * ulang.</p>
	 *
	 * @param index nomor urut mentah
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut mentah penomoran agenda.
	 *
	 * <p>Getter polos. Dipakai {@code PengajuanSiswaAction#onSave(...)} sebagai penanda "baris
	 * lama yang belum bernomor": bila {@code null} pada permohonan yang sudah ada, nomor agenda
	 * dibangkitkan ulang saat itu juga.</p>
	 *
	 * @return nomor urut, atau {@code null} bila baris belum pernah diberi nomor
	 * @see #getKode()
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan uraian/alasan permohonan.
	 *
	 * <p>Getter polos tanpa efek samping. Isinya wajib menurut validasi
	 * {@code PengajuanSiswaAction#onSave(...)} ("Keterangan atau alasan harus diisi") walaupun
	 * kolomnya sendiri {@code nullable = true} — jalur impor/unggah massal karena itu tetap bisa
	 * menghasilkan baris tanpa alasan.</p>
	 *
	 * <p><b>Perbedaan dengan kembaran mahasiswa yang perlu diwaspadai:</b> di sini tidak ada
	 * {@code columnDefinition = "text"}, dan {@code ais.common.DatabaseTextColumnSchemaFix} hanya
	 * menyediakan {@code initPengajuanMahasiswa()} untuk tabel {@code pengajuan_mahasiswa} —
	 * tidak ada padanan untuk {@code pengajuan_siswa}. Kolom ini karena itu tetap bertipe
	 * {@code varchar(255)} bawaan Hibernate, sementara kotak isian pada form dibuat empat baris
	 * tanpa batas panjang. Alasan yang panjang gagal disimpan/terpotong pada jenjang sekolah
	 * padahal berhasil pada jenjang perguruan tinggi.</p>
	 *
	 * @return uraian/alasan permohonan, mungkin {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan uraian/alasan permohonan.
	 *
	 * @param keterangan uraian/alasan permohonan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan siswa pemohon.
	 *
	 * <p>Diisi {@code PengajuanSiswaAction#onSave(...)} dari komponen pencarian siswa
	 * ({@code AmbilDataSiswaBanbox}). Bila pengguna yang login adalah siswa, action mengunci
	 * nilainya ke siswa milik akun tersebut sebelum menyimpan.</p>
	 *
	 * @param siswa siswa pemohon
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan siswa pemohon, dengan resolusi proxy lazy lebih dulu.
	 *
	 * <p><b>Efek samping:</b> hasil {@code GeneralValueObject#check(Object)} <b>ditugaskan
	 * kembali</b> ke field {@link #siswa}. Itu disengaja (pola standar seluruh entity repo ini)
	 * supaya proxy yang sudah teresolusi tidak diresolusi ulang pada pemanggilan berikutnya.
	 * Karena pemetaan memakai <i>property access</i>, penggantian object di sini juga menjadi
	 * bagian dari state yang diperiksa Hibernate saat flush.</p>
	 *
	 * <p>Relasinya {@code LAZY} dengan cascade {@code PERSIST}/{@code MERGE} pada kolom
	 * {@code siswa} yang {@code nullable}. <b>Inilah satu-satunya jalan</b> untuk mengetahui
	 * sekolah/yayasan pemilik baris ini — entity tidak punya kolom {@code sekolah} maupun
	 * {@code yayasan} sendiri. {@code PengajuanSiswaAction#initCriteria(boolean)} karena itu
	 * memakai {@code createAlias("siswa", "siswa")} (INNER JOIN), sehingga baris dengan
	 * {@code siswa} kosong tidak pernah muncul di layar walaupun tetap ada di tabel.</p>
	 *
	 * @return siswa pemohon, mungkin {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Mengembalikan tahun akademik permohonan, dengan nilai bawaan tahun akademik berjalan.
	 *
	 * <p><b>Perhatikan perbedaan dengan getter lain di kelas ini:</b> bila kolomnya {@code null},
	 * method mengembalikan {@code Common.getCurrentTahunAkademik()} <b>tanpa menuliskannya
	 * kembali</b> ke field. Jadi nilai bawaan ini hanya tampil di layar dan tidak ikut tersimpan
	 * — berbeda dari {@link #getTahun()}, {@link #getBulan()}, atau {@link #getPersetujuan()}
	 * yang menulis balik. Konsekuensinya kolom {@code tahun_akademik} bisa tetap {@code null} di
	 * database walaupun layar selalu menampilkan sebuah nilai, dan filter "Tahun Akademik" pada
	 * layar pencarian (yang membandingkan kolom mentah) tidak akan menemukan baris seperti itu.</p>
	 *
	 * @return tahun akademik format {@code "2025/2026"}; tidak pernah {@code null} selama
	 *         konfigurasi tahun akademik berjalan terisi
	 */
	@Column(name = "tahun_akademik", nullable = true)
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik permohonan.
	 *
	 * @param tahunAkademik tahun akademik format {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan penanda semester ganjil/genap permohonan.
	 *
	 * <p>Getter polos. Nilainya diisi dari kombo "Ganjil / Genap" pada form dengan konstanta
	 * {@code Perkuliahan.GANJIL}/{@code Perkuliahan.GENAP}, dan dipakai sebagai filter "Semester"
	 * pada layar pencarian.</p>
	 *
	 * @return penanda ganjil/genap, mungkin {@code null}
	 */
	@Column(name = "ganjil_genap", nullable = true)
	public String getGanjilGenap() {
		return ganjilGenap;
	}

	/**
	 * Menetapkan penanda semester ganjil/genap permohonan.
	 *
	 * @param ganjilGenap penanda ganjil/genap
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * Mengembalikan flag persetujuan permohonan, dengan nilai bawaan {@code true}.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code null}, method <b>menulis</b>
	 * {@code true} ke field, sehingga baris yang belum punya nilai akan ikut ter-{@code update}
	 * menjadi {@code true} begitu dibaca oleh entity yang ter-attach.</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari:</b> nilai bawaan ini adalah "disetujui", bukan
	 * "belum". Baris yang masuk lewat jalur selain form — mis. unggah massal Excel
	 * ({@code Common.uploadData(this, PengajuanSiswa.class, contents)} pada
	 * {@code PengajuanSiswaAction#doAfterCompose(...)}) dengan sel persetujuan dikosongkan —
	 * akan tampil dan berperilaku sebagai permohonan yang <b>sudah disetujui</b>. Jalur form
	 * sendiri aman karena {@code onSave(...)} selalu menetapkan nilai eksplisit.</p>
	 *
	 * <p><b>Apa yang dikendalikan flag ini di layar.</b> Bernilai {@code true} membuat tombol
	 * "Persetujuan" (cetak surat {@code Persetujuan_Pengajuan}) muncul, dan
	 * <b>menonaktifkan</b> tombol "Hapus" serta — khusus bagi pengguna siswa — tombol "Edit".
	 * Jadi flag ini sekaligus menjadi kunci baris.</p>
	 *
	 * <p><b>Jangan tertukar</b> dengan status menurut alur SOP, yang ada di
	 * {@link #getDisposisiSop()} dan {@link #getAktif()}. Berbeda dari kembaran mahasiswanya,
	 * kelas ini <b>tidak</b> menurunkan penyetuju/waktu setuju dari SOP.</p>
	 *
	 * @return {@code true}/{@code false}; tidak pernah {@code null}
	 */
	public Boolean getPersetujuan() {
		if (persetujuan == null) {
			persetujuan = true;
		}
		return persetujuan;
	}

	/**
	 * Menetapkan flag persetujuan permohonan.
	 *
	 * <p>Dipanggil {@code PengajuanSiswaAction#onSave(...)}: bernilai isi kotak centang bila
	 * kotak itu dirender, dan dipaksa {@code false} bila tidak. Kotak centang hanya dirender
	 * untuk pengguna non-siswa yang membuka layar tanpa parameter {@code siswa} pada URL — jadi
	 * pengguna siswa tidak pernah bisa menyetujui permohonannya sendiri lewat form.</p>
	 *
	 * @param persetujuan flag persetujuan
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Mengembalikan tanggal permohonan (presisi {@code DATE}).
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, field diisi <b>dan disimpan</b> dengan
	 * {@link #getTanggal_dirubah()} sebagai pengganti. Untuk object baru hal ini nyaris tak
	 * pernah terjadi karena {@link #tanggal} sudah diinisialisasi hari ini saat konstruksi;
	 * jalur ini terutama menyentuh baris lama hasil impor yang tanggalnya kosong. Perhatikan
	 * bahwa penggantinya bertipe {@code TIMESTAMP} sedangkan kolom ini {@code DATE} — bagian jam
	 * akan hilang saat disimpan.</p>
	 *
	 * <p>Kolom {@code tanggal} juga dipakai sebagai filter oleh
	 * {@code PengajuanSiswaAction#getindex(...)} untuk aturan "reset urutan tiap ..." dan oleh
	 * laporan {@code LaporanPengajuan} serta rekap izin siswa.</p>
	 *
	 * @return tanggal permohonan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = getTanggal_dirubah();
		}
		return tanggal;
	}

	/**
	 * Menetapkan tanggal permohonan.
	 *
	 * @param tanggal tanggal permohonan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tahap/termin permohonan.
	 *
	 * <p>Getter polos. Maknanya bergantung jenis pengajuan (mis. tahap ke berapa sebuah
	 * permohonan bertingkat); entity ini tidak memberi arti khusus pada angkanya, dan form
	 * {@code PengajuanSiswaAction} tidak menyediakan isian untuknya — kolom ini hanya terisi
	 * lewat unggah massal atau impor data.</p>
	 *
	 * @return nomor tahap, mungkin {@code null}
	 */
	public Integer getTahap() {
		return tahap;
	}

	/**
	 * Menetapkan tahap/termin permohonan.
	 *
	 * @param tahap nomor tahap
	 */
	public void setTahap(Integer tahap) {
		this.tahap = tahap;
	}

	/**
	 * Mengembalikan penanda permohonan berkaitan Semester Pendek (SP), dengan bawaan
	 * {@code false}.
	 *
	 * <p>Berbeda dengan {@link #getPersetujuan()}, nilai bawaan di sini <b>tidak</b> ditulis
	 * balik ke field — hanya dikembalikan, sehingga kolomnya boleh tetap {@code null} di
	 * database. Kemunculan kotak centang "Semester Pendek" di form dikendalikan konfigurasi
	 * aplikasi {@code terdapat_pengajuan_siswa_sp} (bawaan: tidak aktif; lihat
	 * {@code KonfigurasiNewAction}). Saat konfigurasi itu mati, baris form disembunyikan tetapi
	 * kotak centangnya tetap dibuat, sehingga {@code onSave(...)} tetap menyimpan nilai
	 * {@code false} — bukan membiarkan nilai lama.</p>
	 *
	 * @return {@code true} bila permohonan terkait SP; tidak pernah {@code null}
	 */
	public Boolean getSemesterPendek() {
		return semesterPendek == null ? false : semesterPendek;
	}

	/**
	 * Menetapkan penanda permohonan Semester Pendek.
	 *
	 * @param semesterPendek {@code true} bila permohonan terkait SP
	 */
	public void setSemesterPendek(Boolean semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/**
	 * Mengembalikan jenis permohonan, dengan resolusi proxy lazy lebih dulu.
	 *
	 * <p>Inilah relasi yang menentukan <b>makna</b> baris ini: nama jenis, aturan penomoran surat
	 * ({@code NomorSurat}), berkas contoh/format pengajuan, template JRXML pencetakan, dan daftar
	 * {@link KelompokParameterTambahanPengajuan} yang harus diisi semuanya berasal dari
	 * {@link JenisPengajuan}. Kolom {@code jenis_pengajuan} bersifat {@code nullable = false} dan
	 * {@code PengajuanSiswaAction#onSave(...)} menolak simpan bila kombonya kosong.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code GeneralValueObject#check(Object)} ditulis balik ke
	 * field {@link #jenisPengajuan} (pola standar repo, sengaja).</p>
	 *
	 * @return jenis permohonan
	 * @see JenisPengajuan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengajuan", nullable = false)
	public JenisPengajuan getJenisPengajuan() {
		jenisPengajuan = check(jenisPengajuan);
		return jenisPengajuan;
	}

	/**
	 * Menetapkan jenis permohonan.
	 *
	 * <p>Selain dari {@code onSave(...)}, setter ini juga dipanggil lebih awal oleh listener
	 * perubahan kombo "Jenis Pengajuan" pada form, karena
	 * {@code ParameterTambahanPengajuanListener} membutuhkan jenis yang sudah terpasang untuk
	 * merender baris-baris parameter tambahan yang benar.</p>
	 *
	 * @param jenisPengajuan jenis permohonan
	 */
	public void setJenisPengajuan(JenisPengajuan jenisPengajuan) {
		this.jenisPengajuan = jenisPengajuan;
	}

	/**
	 * Mengembalikan tanggal berakhirnya permohonan (presisi {@code DATE}).
	 *
	 * <p>Dipakai jenis pengajuan yang punya rentang waktu (mis. izin/cuti beberapa hari); pada
	 * daftar, tanggal ini dirender sebagai bagian "sd &lt;tanggal&gt;". Getter polos — tidak ada
	 * nilai bawaan dan tidak ada efek samping, dan tidak ada validasi yang memastikan tanggal ini
	 * tidak mendahului {@link #getTanggal()}.</p>
	 *
	 * @return tanggal selesai, mungkin {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Menetapkan tanggal berakhirnya permohonan.
	 *
	 * @param tanggalSelesai tanggal selesai
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Menetapkan jam mulai, dinormalisasi: string kosong/spasi disimpan sebagai {@code null},
	 * selain itu dipangkas spasinya.
	 *
	 * @param waktuMulai jam mulai (mis. {@code "08.00"}); kosong dianggap {@code null}
	 */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Mengembalikan jam mulai permohonan.
	 *
	 * <p><b>Efek samping khusus record baru:</b> bila {@link #getId()} masih {@code null} (baris
	 * belum pernah tersimpan) dan jam mulai belum diisi, field <b>diisi dengan jam sekarang</b>
	 * ({@code Common.timeFormat2} atas {@link WaktuUtil#getDate()}). Jadi sekadar menampilkan
	 * form pengajuan baru sudah menetapkan jam mulai; setelah baris punya id, pengisian otomatis
	 * ini tidak berlaku lagi.</p>
	 *
	 * <p><b>Kuirk pada alur simpan.</b> {@code PengajuanSiswaAction#onSave(...)} menimpa nilai
	 * ini dengan isi {@code Timebox} "Waktu Permohonan"; bila pengguna mengosongkan kotak itu,
	 * yang tersimpan adalah {@code null} — bukan jam otomatis di atas. Jam otomatis karena itu
	 * hanya bertahan bila komponen form sempat membacanya lebih dulu.</p>
	 *
	 * @return jam mulai yang sudah dipangkas, atau {@code null} bila kosong
	 */
	@Column(name = "waktu_mulai", length = 20)
	public String getWaktuMulai() {
		if (getId() == null && (waktuMulai == null || waktuMulai.trim().equals(""))) {
			waktuMulai = Common.timeFormat2.get().format(WaktuUtil.getDate());
		}
		return waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Menetapkan jam selesai, dinormalisasi sama seperti {@link #setWaktuMulai(String)}.
	 *
	 * @param waktuSelesai jam selesai; kosong dianggap {@code null}
	 */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Mengembalikan jam selesai permohonan.
	 *
	 * <p>Tidak seperti {@link #getWaktuMulai()}, getter ini <b>tidak</b> mengisi nilai bawaan —
	 * hanya menormalkan string kosong menjadi {@code null}, sehingga bebas efek samping.</p>
	 *
	 * @return jam selesai yang sudah dipangkas, atau {@code null} bila kosong
	 */
	@Column(name = "waktu_selesai", length = 20)
	public String getWaktuSelesai() {

		return waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Mengembalikan isian parameter tambahan dalam bentuk <b>berbasis id</b>.
	 *
	 * <p>Formatnya satu baris per parameter, dipisah {@code "\n"}, dengan empat kolom yang
	 * dipisah {@code "&lt;=&gt;"}:</p>
	 * <pre>
	 * kelompokId-&gt;parameterId &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
	 * </pre>
	 *
	 * <p>Bentuk ini dipakai saat form dibangun ulang dan saat renderer daftar mencocokkan nilai
	 * ke tiap {@link ParameterTambahan} — pencocokannya membandingkan kolom pertama dengan kunci
	 * {@code "<kelompokId>-&gt;<parameterId>"} yang dibentuk ulang di layar.</p>
	 *
	 * <p><b>Efek samping:</b> nilai {@code null} dinormalkan menjadi string kosong dan
	 * <b>ditulis balik</b> ke field, sehingga baris lama bisa ter-{@code update} dari
	 * {@code NULL} menjadi {@code ''} hanya karena dibaca.</p>
	 *
	 * @return isian berbasis id; tidak pernah {@code null} (minimal string kosong)
	 * @see #getParameterTambahan()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menetapkan isian parameter tambahan bentuk berbasis id.
	 *
	 * <p>Umumnya tidak dipanggil langsung; {@link #populateParameterTambahan(List)} yang
	 * mengisinya bersamaan dengan {@link #setParameterTambahan(String)}. Bila diisi manual,
	 * pastikan kedua kolom tetap konsisten satu sama lain.</p>
	 *
	 * @param parameterTambahanInds isian berbasis id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} (bentuk manusiawi) menjadi daftar
	 * {@link CommonVO} siap tampil/rekap, terurut.
	 *
	 * <p><b>Pemetaan kolom → properti {@code CommonVO}:</b></p>
	 * <table border="1" summary="pemetaan kolom parameter tambahan ke CommonVO">
	 *   <tr><th>Indeks kolom</th><th>Isi</th><th>Disimpan ke</th></tr>
	 *   <tr><td>0</td><td>{@code namaKelompok->labelInputan}</td><td>{@code name}</td></tr>
	 *   <tr><td>1</td><td>nilai isian</td><td>{@code name1}</td></tr>
	 *   <tr><td>2</td><td>URL lampiran</td><td>{@code name2}</td></tr>
	 *   <tr><td>3</td><td>nomor urut</td><td>{@code nomorUrut}</td></tr>
	 *   <tr><td>4</td><td>id {@link ParameterTambahan}</td><td>{@code id}</td></tr>
	 * </table>
	 * <p>Selain itu {@code name5} diisi bagian sebelum {@code "->"} dari kolom 0, yaitu nama
	 * kelompok parameter. Nomor urut dan id dibungkus {@code try/catch} (yang mencatat lewat
	 * {@code ais.common.ErrorAuditUtil}) sehingga data rusak jatuh ke nilai bawaan {@code 1}
	 * alih-alih melempar exception.</p>
	 *
	 * <p><b>Pengurutan.</b> {@code Collections.sort} memakai {@code CommonVO#compareTo}: karena
	 * {@code name5} di sini hampir selalu terisi, urutannya memakai perbandingan <b>string</b>
	 * {@code "namaKelompok nomorUrut"}. Efeknya nomor urut dibandingkan secara leksikografis,
	 * sehingga {@code 10} muncul sebelum {@code 2} bila satu kelompok punya lebih dari sembilan
	 * parameter.</p>
	 *
	 * <p><b>Kuirk yang perlu diantisipasi pemanggil.</b></p>
	 * <ul>
	 *   <li>Bila belum ada isian sama sekali, {@link #getParameterTambahan()} mengembalikan
	 *       string kosong dan {@code "".split("\n")} menghasilkan array berisi satu elemen
	 *       kosong. Method ini karena itu mengembalikan <b>satu {@code CommonVO} kosong</b>,
	 *       bukan list kosong. Pemanggil yang menghitung jumlah parameter harus menyaringnya
	 *       sendiri.</li>
	 *   <li>Kolom 5 (id kelompok) dan kolom 6 (keterangan) yang ditulis
	 *       {@link #populateParameterTambahan(List)} <b>tidak diurai</b> di sini; keduanya hanya
	 *       tersedia lewat {@link #getParameterTambahanInds()}.</li>
	 *   <li>Nilai isian tidak pernah di-<i>escape</i> saat ditulis, jadi isian yang memuat
	 *       {@code "\n"} atau {@code "&lt;=&gt;"} akan terurai menjadi baris/kolom tambahan
	 *       palsu di sini.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getParameterTambahan()}, yang menulis balik
	 * string kosong bila kolomnya {@code null}.</p>
	 *
	 * @return daftar parameter tambahan terurut; tidak pernah {@code null}, tetapi bisa berisi
	 *         satu elemen kosong seperti dijelaskan di atas
	 * @see #populateParameterTambahan(List)
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengajuanSiswa.java:291");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PengajuanSiswa.java:297");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Memanen nilai seluruh baris form parameter tambahan yang sedang tampil di layar ZK, lalu
	 * <b>menulis ulang kedua kolom teks</b> {@link #setParameterTambahanInds(String)} dan
	 * {@link #setParameterTambahan(String)}.
	 *
	 * <p><b>Dipanggil dari mana.</b> Dari
	 * {@code ais.action.master.helper.ParameterTambahanPengajuanListener#onSave(PengajuanSiswa)},
	 * yang sendiri dipanggil {@code PengajuanSiswaAction#onSave(...)} tepat sebelum entity
	 * disimpan. Baris-baris {@link Row} yang dioper sudah dibangun listener tersebut dan membawa
	 * dua atribut penting: {@code "parameterTambahan"} ({@link ParameterTambahan}) dan
	 * {@code "kelompokParameterTambahanPengajuan"}
	 * ({@link KelompokParameterTambahanPengajuan}); baris tanpa keduanya dilewati.</p>
	 *
	 * <p><b>Yang dikerjakan per baris.</b></p>
	 * <ol>
	 *   <li>Menyusun kunci {@code jenis = "<kelompokId>-&gt;<parameterId>"}.</li>
	 *   <li>Mengambil nilai isian lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)}
	 *       (menangani semua tipe komponen: textbox, combobox, datebox, dan seterusnya).</li>
	 *   <li>Bila parameter mewajibkan lampiran, mencari berkasnya lewat
	 *       {@link LampiranLain#ambil(Long, String)} dengan {@code ref = getId()} dan kunci
	 *       {@code jenis}, lalu menyimpan {@code createLinkUri()}-nya sebagai URL.</li>
	 *   <li>Merangkai satu baris untuk masing-masing kolom teks (7 kolom untuk bentuk manusiawi,
	 *       4 kolom untuk bentuk berbasis id).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping.</b> Kedua kolom ditimpa total — isian lama yang barisnya tidak lagi
	 * tampil di form akan hilang. Ini disengaja: form selalu merender ulang seluruh parameter
	 * milik jenis pengajuan yang dipilih. Karena itu <b>mengganti jenis pengajuan pada permohonan
	 * yang sudah tersimpan akan membuang seluruh isian parameter jenis sebelumnya.</b></p>
	 *
	 * <p><b>Jebakan yang perlu diketahui.</b></p>
	 * <ul>
	 *   <li>Pada pengajuan <b>baru</b> {@link #getId()} masih {@code null}, sehingga pencarian
	 *       lampiran memakai {@code ref} {@code null} dan URL lampiran umumnya keluar kosong pada
	 *       penyimpanan pertama; URL baru terisi pada penyuntingan berikutnya.</li>
	 *   <li>Setiap baris dibungkus {@code try/catch} yang hanya memanggil
	 *       {@code Common.tampilErrorJikaAdmin(e)}. Bagi pengguna non-admin kegagalan satu baris
	 *       <b>senyap</b> dan nilai parameter tersebut hilang tanpa peringatan.</li>
	 *   <li>Variabel lokal {@code parameterTambahanInds} sengaja menutupi (shadow) field dengan
	 *       nama sama; penulisan ke field baru terjadi lewat setter di akhir method.</li>
	 *   <li><b>Empat pemanggilan {@code System.out.println} di method ini masih aktif</b> (di
	 *       kembaran {@code PengajuanMahasiswa} baris-baris serupa sudah dikomentari). Isinya
	 *       mencakup seluruh nilai parameter tambahan permohonan — yang untuk jenis pengajuan
	 *       tertentu dapat berisi data pribadi siswa — sehingga ikut tertulis ke log aplikasi
	 *       pada setiap penyimpanan.</li>
	 *   <li>Tidak ada pelolosan pemisah: nilai yang memuat {@code "\n"} atau {@code "&lt;=&gt;"}
	 *       merusak struktur kolom teks dan dapat memalsukan baris parameter lain saat dibaca
	 *       ulang.</li>
	 * </ul>
	 *
	 * @param parameterRows baris-baris form ZK yang membawa komponen isian; {@code null} atau
	 *                      kosong membuat method tidak melakukan apa-apa (kolom lama dibiarkan
	 *                      utuh)
	 * @see #ambilDataParameterTambahan()
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				
				
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan = (KelompokParameterTambahanPengajuan) row
						.getAttribute("kelompokParameterTambahanPengajuan");
				
				System.out.println("parameterTambahan => " + parameterTambahan);
				System.out.println("kelompokParameterTambahanPengajuan => " + kelompokParameterTambahanPengajuan);
				
				if (parameterTambahan != null && kelompokParameterTambahanPengajuan != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(PengajuanSiswa.class, getId(),
							kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanPengajuan.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanPengajuan.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Mengembalikan isian parameter tambahan dalam bentuk <b>manusiawi</b> (siap dicetak/rekap).
	 *
	 * <p>Formatnya satu baris per parameter, dipisah {@code "\n"}, dengan tujuh kolom yang
	 * dipisah {@code "&lt;=&gt;"}:</p>
	 * <pre>
	 * namaKelompok-&gt;labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut
	 *   &lt;=&gt; parameterId &lt;=&gt; kelompokId &lt;=&gt; keterangan
	 * </pre>
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getParameterTambahanInds()}, nilai {@code null}
	 * dinormalkan menjadi string kosong dan ditulis balik ke field.</p>
	 *
	 * @return isian bentuk manusiawi; tidak pernah {@code null} (minimal string kosong)
	 * @see #ambilDataParameterTambahan()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menetapkan isian parameter tambahan bentuk manusiawi.
	 *
	 * <p>Umumnya diisi {@link #populateParameterTambahan(List)}; bila diisi manual, jaga
	 * konsistensinya dengan {@link #setParameterTambahanInds(String)}.</p>
	 *
	 * @param parameterTambahan isian bentuk manusiawi
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan simpul alur SOP yang sedang/terakhir memproses permohonan ini, dengan
	 * resolusi proxy lazy lebih dulu.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()} sehingga permohonan ini
	 * dapat diproses mesin disposisi bersama dokumen ber-SOP lainnya. {@link #getAktif()}
	 * menurunkan nilainya dari relasi ini, dan renderer daftar menampilkan tautan
	 * "SOP &lt;keterangan&gt; (&lt;nama sop&gt;)" yang membuka
	 * {@code TampilanAlurSopAction} bila relasinya terisi. Penghapusan baris juga lewat sini:
	 * {@code PengajuanSiswaAction} memanggil {@code SopUtil.hapusDisposisi(...)} lebih dulu.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code GeneralValueObject#check(Object)} ditulis balik ke
	 * field {@link #disposisiSop}.</p>
	 *
	 * @return simpul disposisi SOP, mungkin {@code null} bila permohonan belum/tidak masuk alur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan simpul alur SOP permohonan ini.
	 *
	 * <p><b>Guard yang penting:</b> argumen {@code null} atau disposisi yang belum punya id
	 * <b>diabaikan diam-diam</b>. Artinya tautan disposisi yang sudah ada tidak dapat dilepas
	 * lewat setter ini — disengaja, supaya penyimpanan ulang form tidak sampai memutus jejak alur
	 * persetujuan yang sedang berjalan.</p>
	 *
	 * <p><b>Catatan pemeliharaan:</b> ekspresi ternary di badan method merupakan sisa versi lama
	 * dan kini merupakan kode mati — setelah guard di atas, kondisi
	 * {@code (disposisiSop == null || disposisiSop.getId() == null)} tidak mungkin bernilai benar,
	 * sehingga cabang yang mempertahankan nilai lama tidak pernah tereksekusi dan penugasan selalu
	 * memakai argumen. Dibiarkan apa adanya di sini karena tugas dokumentasi tidak boleh mengubah
	 * logika.</p>
	 *
	 * @param disposisiSop simpul disposisi baru; diabaikan bila {@code null} atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui permohonan, dengan resolusi proxy lazy lebih dulu.
	 *
	 * <p><b>Perbedaan penting dari kembaran mahasiswa.</b> Di
	 * {@code ais.database.model.PengajuanMahasiswa}, getter senama <b>menurunkan</b> nilainya
	 * dari simpul "setuju" pada {@link DisposisiSop} dan menimpa kolomnya. Di sini tidak: getter
	 * ini hanya meresolusi proxy dan mengembalikan apa adanya nilai kolom
	 * {@code disetuji_oleh}.</p>
	 *
	 * <p><b>Akibatnya kolom ini praktis selalu {@code null}.</b> Penelusuran seluruh repo
	 * menunjukkan {@link #setDisetujuiOleh(Tbmuser)} tidak pernah dipanggil untuk entity ini
	 * (semua pemanggil {@code setDisetujuiOleh} yang ada milik entity akunting seperti
	 * {@code KasKecil}/{@code KasBesar}/{@code DanaTalangan}), dan {@code PengajuanSiswaAction}
	 * tidak menyediakan isian untuknya. Persetujuan permohonan siswa karena itu tersimpan hanya
	 * sebagai flag {@link #getPersetujuan()} tanpa identitas penyetuju — pertimbangkan hal ini
	 * bila membangun laporan atau audit yang bergantung pada kolom ini.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code GeneralValueObject#check(Object)} ditulis balik ke
	 * field {@link #disetujiOleh}.</p>
	 *
	 * @return pengguna penyetuju, hampir selalu {@code null} pada data nyata
	 * @see #getSetujuiTanggal()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);
		return disetujiOleh;
	}

	/**
	 * Menetapkan pengguna penyetuju.
	 *
	 * <p>Setter polos, dan — seperti dijelaskan pada {@link #getDisetujuiOleh()} — tidak dipanggil
	 * dari mana pun di repo untuk entity ini. Disediakan agar pemetaan Hibernate lengkap dan agar
	 * kode migrasi/impor tetap bisa mengisinya.</p>
	 *
	 * @param disetujiOleh pengguna penyetuju
	 */
	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	/**
	 * Mengembalikan waktu persetujuan (presisi {@code TIMESTAMP}).
	 *
	 * <p>Getter polos tanpa efek samping. Sama seperti {@link #getDisetujuiOleh()}, versi
	 * mahasiswa menurunkan nilai ini dari alur SOP sedangkan versi sekolah tidak, dan
	 * {@link #setSetujuiTanggal(Date)} tidak pernah dipanggil — jadi kolom
	 * {@code setujui_tanggal} praktis selalu {@code null} pada data nyata. Waktu yang benar-benar
	 * tercatat saat permohonan disetujui hanyalah {@link #getTanggal_dirubah()}, yang juga
	 * berubah untuk setiap penyuntingan lain.</p>
	 *
	 * @return waktu persetujuan, hampir selalu {@code null} pada data nyata
	 * @see #getDisetujuiOleh()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {
		return setujuiTanggal;
	}

	/**
	 * Menetapkan waktu persetujuan.
	 *
	 * <p>Setter polos yang tidak dipanggil dari mana pun di repo untuk entity ini; lihat
	 * {@link #getSetujuiTanggal()}.</p>
	 *
	 * @param setujuiTanggal waktu persetujuan
	 */
	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	/**
	 * Mengembalikan nomor agenda/surat permohonan.
	 *
	 * <p>Getter polos tanpa nilai bawaan. Nilainya dibangkitkan
	 * {@code PengajuanSiswaAction#generateCode(JenisPengajuan, boolean)} dari konfigurasi
	 * {@code NomorSurat} milik {@link JenisPengajuan}, bukan oleh entity ini, dan ditampilkan di
	 * form sebagai label "No. Agenda". Permohonan lama yang dibuat sebelum fitur penomoran ada
	 * bisa saja {@code null}; action akan mengisinya saat baris itu disimpan ulang.</p>
	 *
	 * <p><b>Peringatan keunikan:</b> tidak ada batasan {@code unique} pada kolom ini, dan
	 * pencacah di action memakai jumlah baris ({@code Projections.rowCount()}) alih-alih nomor
	 * terbit tertinggi — menghapus satu permohonan membuat nomor agenda berikutnya kembar dengan
	 * nomor yang sudah dicetak. Lihat "Penomoran agenda/surat" pada dokumentasi kelas.</p>
	 *
	 * @return nomor agenda, mungkin {@code null}
	 * @see #getIndex()
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan nomor agenda/surat permohonan.
	 *
	 * @param kode nomor agenda hasil format {@code NomorSurat}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan tahun acuan penomoran, dengan nilai bawaan tahun berjalan.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya {@code null}, field diisi tahun sekarang
	 * ({@link WaktuUtil#getCalendar()}) <b>dan ikut tersimpan</b> saat flush. Untuk baris lama
	 * hasil impor, ini berarti tahun yang tercatat adalah tahun saat data itu pertama kali
	 * dibaca, bukan tahun permohonannya — dan karena {@code getindex(...)} memfilter kolom ini,
	 * baris lama itu bisa ikut terhitung pada penomoran tahun berjalan.</p>
	 *
	 * <p>Perhatikan bahwa {@code PengajuanSiswaAction} tidak pernah memanggil
	 * {@link #setTahun(Integer)}; kolom ini terisi semata-mata karena Hibernate memanggil getter
	 * ini saat menyimpan (property access).</p>
	 *
	 * @return tahun acuan penomoran; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun acuan penomoran.
	 *
	 * @param tahun tahun acuan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan bulan acuan penomoran (1-12), dengan nilai bawaan bulan berjalan.
	 *
	 * <p><b>Efek samping</b> dan konsekuensinya sama seperti {@link #getTahun()}: bila kolomnya
	 * {@code null}, field diisi bulan sekarang dan ikut tersimpan. Perhatikan penambahan
	 * {@code + 1} — {@link Calendar#MONTH} berbasis nol, sedangkan kolom ini menyimpan
	 * 1 = Januari.</p>
	 *
	 * <p>Dipakai {@code PengajuanSiswaAction#getindex(...)} untuk aturan "reset urutan tiap
	 * bulan".</p>
	 *
	 * @return bulan acuan penomoran, 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan bulan acuan penomoran.
	 *
	 * @param bulan bulan acuan, 1-12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan status aktif permohonan, dengan <b>koreksi otomatis berdasarkan alur SOP</b>.
	 *
	 * <p><b>Cara kerja.</b> Field {@link #aktif} dipaksa {@code false} bila salah satu dari dua
	 * kondisi berikut terpenuhi:</p>
	 * <ol>
	 *   <li>{@link #getDisposisiSop()} ada tetapi disposisinya sendiri sudah tidak aktif;</li>
	 *   <li>alur berhenti di simpul akhir yang ditandai sebagai titik penolakan
	 *       ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}) — dengan kata lain
	 *       permohonan <b>ditolak</b>.</li>
	 * </ol>
	 * <p>Bila kolomnya masih {@code null}, hasilnya dianggap {@code true}. Pola persis ini dipakai
	 * berulang di entity ber-SOP lain (mis. {@code KasKecil}, {@code KasBesar},
	 * {@code DanaTalangan}, {@code PenggantianKasKecil}, {@code DaftarPengajuanTransfer}) dan di
	 * kembaran {@code PengajuanMahasiswa}, jadi perubahan perilaku di sini kemungkinan besar juga
	 * relevan di sana.</p>
	 *
	 * <p><b>Efek samping &amp; sifat satu arah.</b> Penimpaan menulis ke field yang dipetakan,
	 * sehingga membaca daftar permohonan dapat menyimpan {@code aktif = false} ke database.
	 * Perhatikan bahwa method ini <b>tidak pernah mengembalikan nilai ke {@code true}</b>: bila
	 * disposisi kemudian diaktifkan lagi atau penolakan dibatalkan, kolom {@code aktif} tetap
	 * {@code false} sampai ada yang memanggil {@link #setAktif(Boolean)} secara eksplisit. Baris
	 * pertama ({@code disposisiSop = getDisposisiSop();}) hanyalah penugasan ulang hasil resolusi
	 * proxy ke field yang sama — tidak mengubah perilaku.</p>
	 *
	 * <p><b>Catatan penting soal status "ditolak".</b> Penolakan hanya tercermin di sini, bukan
	 * pada {@link #getPersetujuan()}. Permohonan yang ditolak lewat alur SOP tetap dapat memiliki
	 * {@code persetujuan = true} — dan {@code PengajuanSiswaAction} tidak menyaring
	 * {@link #getAktif()} sama sekali, baik pada daftar maupun pada tombol cetak "Persetujuan".
	 * Jangan menyimpulkan status akhir sebuah permohonan hanya dari salah satu dari kedua nilai
	 * ini.</p>
	 *
	 * @return {@code true} bila permohonan masih berjalan/berlaku; {@code false} bila disposisinya
	 *         nonaktif atau permohonan ditolak. Tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif permohonan.
	 *
	 * <p>Satu-satunya cara mengembalikan status menjadi {@code true} setelah {@link #getAktif()}
	 * memaksanya {@code false}. Tidak dipanggil dari {@code PengajuanSiswaAction}.</p>
	 *
	 * @param aktif status aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
