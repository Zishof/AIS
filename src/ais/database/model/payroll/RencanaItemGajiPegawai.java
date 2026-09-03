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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;

/**
 * Satu baris <b>RENCANA</b> (anggaran) komponen gaji untuk satu pegawai, satu bulan, dalam satu
 * dokumen rencana gaji tahunan. Tabel: {@code payroll.rencana_item_gaji_pegawai}.
 *
 * <p>Entity ini adalah daun paling bawah dari rantai penggajian sisi <i>perencanaan</i>. Rantainya,
 * seluruhnya terverifikasi dari kode (bukan dugaan dari nama):</p>
 *
 * <pre>
 * RencanaGaji            (dokumen rencana per TAHUN; hanya kolom keterangan + tahun)
 *   +-- RencanaGajiPunyaPegawai   (satu baris per pegawai; nilai1..nilai12 + komponenGaji JSON)
 *         +-- RencanaItemGajiPegawai  &lt;-- KELAS INI (rincian per komponen gaji, per bulan)
 * </pre>
 *
 * <p>Pasangan sisi <i>realisasi</i>-nya adalah {@link PembayaranItemGajiPegawai}, yang menggantung
 * pada {@link PembayaranGajiPunyaPegawai} dan {@code PembayaranGaji}. Kedua kelas itu adalah kembar
 * salin-tempel kelas ini: daftar field, urutan method, dan nilai {@code serialVersionUID}-nya sama
 * persis; yang berbeda hanya nama tabel dan induknya ({@code rencana_gaji_punya_pegawai} vs
 * {@code pembayaran_gaji_punya_pegawai}). <b>Tidak ada FK apa pun</b> antara baris rencana dan baris
 * realisasi — keduanya berdiri sendiri, tidak saling menunjuk.</p>
 *
 * <h2>Dari mana angka rencana berasal — dan arah alirannya</h2>
 *
 * <p>Satu-satunya penulis baris kelas ini adalah {@code RencanaItemGajiPegawaiTreeModel.reset()}
 * (dipanggil dari tiga tempat: tombol "Ambil Data Pegawai" dan "Hitung Ulang" di
 * {@code RencanaGajiPunyaPegawaiAction}, serta jalur "salin rencana tahun lain" di
 * {@code RencanaGajiAction.onSave()}). Mekanismenya, per pegawai:</p>
 *
 * <ol>
 *   <li>Seluruh baris rencana milik {@link RencanaGajiPunyaPegawai} yang bersangkutan
 *       <b>DIHAPUS</b> lebih dulu dengan SQL mentah
 *       ({@code delete from payroll.rencana_item_gaji_pegawai where rencana_gaji_punya_pegawai = ?}).</li>
 *   <li>Untuk tiap bulan 1..12 dicari <b>realisasi</b> gaji terakhir yang <b>SUDAH DISETUJUI</b>
 *       ({@link PembayaranGajiPunyaPegawai} dengan {@code pembayaranGaji.bulan &lt;= bulan},
 *       {@code pembayaranGaji.tahun = tahun}, dan {@code disetujuiOleh IS NOT NULL}).</li>
 *   <li><b>Bila realisasi ditemukan</b>: nominal bulan itu diambil apa adanya dari realisasi, dan
 *       rincian komponennya disalin ke JSON {@code komponenGaji} milik induk dengan kunci
 *       {@code <kodeItemGaji>_<bulan>}. <b>Nol baris kelas ini dibuat untuk bulan tersebut.</b></li>
 *   <li><b>Bila realisasi TIDAK ditemukan</b>: nominal dihitung dari formula
 *       ({@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai}) menelusuri pohon
 *       {@link ItemGajiPegawai} milik pegawai, dan <b>di sinilah baris kelas ini dibuat</b> — satu
 *       baris per komponen gaji, dengan {@link #getBulan()}/{@link #getTahun()} terisi.</li>
 * </ol>
 *
 * <p>Konsekuensi yang perlu diketahui sebelum menyentuh kode ini:</p>
 *
 * <ul>
 *   <li><b>Arah alirannya REALISASI &rarr; RENCANA, bukan sebaliknya.</b> Tidak ada satu pun jalur
 *       di repo yang mengubah baris rencana menjadi pembayaran. Rencana adalah proyeksi yang
 *       <i>mengadopsi</i> gaji yang sudah disetujui untuk bulan-bulan yang sudah lewat, lalu
 *       memproyeksikan formula untuk bulan-bulan yang belum. Pembayaran gaji nyata dibuat lewat
 *       modul {@code PembayaranGaji} yang terpisah sama sekali.</li>
 *   <li><b>Tabelnya SENGAJA tidak lengkap.</b> Bulan yang sudah punya realisasi disetujui tidak
 *       pernah punya baris di sini. Jangan pernah menghitung total tahunan dengan menjumlahkan
 *       baris kelas ini — hasilnya akan kekurangan setiap bulan yang sudah dibayar.</li>
 *   <li><b>Satu-satunya umpan balik rencana &rarr; gaji nyata</b> lewat kunci {@code RENC_TOT_<kode>}
 *       pada JSON {@code komponenGaji} milik induk: formula gaji boleh menyebut token itu, dan
 *       {@code ItemGajiPegawaiTreeModel} akan menggantinya dengan total rencana setahun. Umpan balik
 *       itu membaca JSON induk, <b>bukan</b> baris kelas ini.</li>
 * </ul>
 *
 * <h2>Kuirk penting: baris ini praktis TIDAK PERNAH DIBACA ISINYA</h2>
 *
 * <p>Penyisiran menyeluruh repo menemukan hanya empat berkas Java yang menyebut kelas ini, dan
 * tidak satu pun benar-benar membaca nominalnya kembali:</p>
 *
 * <ul>
 *   <li>{@code RencanaItemGajiPegawaiTreeModel} — menulisnya. Seluruh method pembacanya
 *       ({@code getChildren}, {@code populateData}, {@code getChild}, {@code getChildCount},
 *       {@code deleteChilds}, {@code isLeaf}, {@code getParentCount}, {@code getParentSet},
 *       {@code getChildsSet}, {@code generateAllChildren}, {@code copyByFormat},
 *       {@code checkExistingItemGaji}) <b>nol pemanggil dari luar</b> — perancah salin-tempel dari
 *       {@code ItemGajiPegawaiTreeModel} yang tidak pernah dipasang ke komponen Tree ZK mana pun.
 *       Yang benar-benar dipanggil hanyalah {@code reset()}.</li>
 *   <li>{@code RencanaGajiPunyaPegawaiAction} dan {@code RencanaGajiAction} — hanya memanggil
 *       {@code reset()}.</li>
 *   <li>{@code ItemGajiPegawaiTreeModel.reset()} — <b>satu-satunya pembaca</b>, dan itu pun hanya
 *       {@code rowCount()}: bila pegawai punya NOL baris rencana, seluruh
 *       {@code payroll.item_gaji_pegawai} milik pegawai itu <b>DIHAPUS</b> lalu dibangun ulang dari
 *       format; bila ada baris rencana, penghapusan dilewati. Jadi keberadaan baris kelas ini
 *       berfungsi sebagai <b>kunci pelindung</b> bagi penyesuaian komponen gaji per-pegawai.
 *       Menghapus rencana (lewat tombol sampah di layar, atau karena {@code reset()} gagal di
 *       tengah jalan) diam-diam membuka kunci itu, dan penyesuaian per-pegawai bisa tersapu pada
 *       sinkronisasi format berikutnya.</li>
 * </ul>
 *
 * <p>Kedua laporan resmi rencana gaji ({@code LaporanRekapRencanaGaji} dan
 * {@code LaporanRekapRencanaGajiTahunan}) membaca {@code RencanaGajiPunyaPegawai.nilai1..12} dan
 * JSON {@code komponenGaji}, <b>tidak pernah</b> tabel ini. Kolom {@link #getAkun()},
 * {@link #getAkunDebet()}, dan {@link #getPostingHistory()} disimpan tetapi <b>tidak pernah
 * dipakai</b>: nol penulis untuk {@code postingHistory}, dan nol mesin posting yang membaca tabel
 * ini — rencana gaji memang tidak pernah dijurnal ke buku besar. Ketiganya ada karena kelas ini
 * disalin dari {@link PembayaranItemGajiPegawai}, tempat kolom-kolom itu memang bermakna.</p>
 *
 * <h2>Getter yang menulis balik (property access + dynamicUpdate)</h2>
 *
 * <p>Anotasi pemetaan dipasang pada <b>getter</b> ({@link #getId()} membawa {@code @Id}), sehingga
 * Hibernate memakai <i>property access</i>: setiap getter dipanggil Hibernate saat dirty-check dan
 * saat flush. Digabung dengan {@code dynamicUpdate = true}, setiap penugasan ke field di dalam
 * getter <b>ikut tersimpan permanen ke database</b> — membaca baris bisa mengubah baris. Tiga
 * tingkatan perilaku, dan perbedaannya penting:</p>
 *
 * <ol>
 *   <li><b>Materialisasi malas (hanya bila masih {@code null})</b> — {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getNomorUrut()}, {@link #getAktif()}, {@link #getKode()},
 *       {@link #getDefaultFormula()}: menyalin nilai dari {@link ItemGajiPegawai} sekali lalu
 *       membekukannya. Ini bermakna untuk dokumen rencana: label dan formula ikut terekam apa adanya
 *       pada saat rencana disusun, dan tidak berubah lagi walau master diubah kemudian.</li>
 *   <li><b>Snapshot akun (hanya bila masih {@code null})</b> — {@link #getAkun()} dan
 *       {@link #getAkunDebet()}: menuruni rantai {@code itemGajiPegawai.getItemGaji().getAkun()}.
 *       Perlu diingat {@code ItemGaji.getAkun()} sendiri <i>live-override</i> dari
 *       {@code KelompokItemGaji} + {@code FormatItemGaji.getSatuanKerja()}, jadi akun yang
 *       terbekukan di sini adalah akun yang berlaku <b>pada saat pembacaan pertama</b>, bukan pada
 *       saat baris dibuat.</li>
 *   <li><b>Penimpaan TANPA SYARAT</b> — {@link #getTampilkanDiSlip()} dan {@link #getSpace()}: tidak
 *       ada penjaga {@code null}, nilai tersimpan ditimpa nilai master <b>setiap kali dibaca</b>.
 *       Akibatnya kedua kolom itu tidak pernah bisa berbeda dari masternya, dan suntingan operator
 *       atas keduanya selalu terhapus diam-diam. Kuirk yang sama ada pada
 *       {@link PembayaranItemGajiPegawai} (kembar salin-tempelnya).</li>
 * </ol>
 *
 * <p><b>{@link #getNilai()} menyentuh NOMINAL dan bersifat merusak.</b> Getter itu (a) menugaskan
 * {@code 0.0} bila {@code nilai} masih {@code null}, dan (b) bila master {@code ItemGaji} bertanda
 * {@code jadikan0JikaMinus}, memaksa {@code nilai} negatif menjadi {@code 0.0} — <b>permanen</b>,
 * bukan sekadar pada nilai yang dikembalikan. Komponen rencana bernilai negatif (mis. potongan yang
 * disimpan sebagai angka negatif) bisa hilang angkanya hanya karena barisnya dirender, diekspor,
 * atau disentuh dirty-check. Nilai aslinya tidak bisa dipulihkan dari tabel utama.</p>
 *
 * <h2>Cakupan tenant dan siapa yang boleh mengubah rencana</h2>
 *
 * <p><b>Tidak ada kolom tenant sama sekali</b> pada kelas ini — bukan penyaring yang fail-open,
 * memang tidak ada kolomnya. Hal yang sama berlaku ke atas: {@link RencanaGaji} hanya punya
 * {@code keterangan} dan {@code tahun}, dan {@code RencanaGajiAction.check()} bahkan menegakkan
 * keunikan tahun secara <b>global</b> — satu dokumen rencana per tahun untuk seluruh instalasi,
 * dipakai bersama semua tenant. Satu-satunya jejak tenant yang bisa dicapai adalah lewat
 * {@link #getFormatItemGaji()} &rarr; {@code FormatItemGaji.getSatuanKerja()}, dan tidak ada satu
 * pun query di jalur rencana gaji yang memakainya sebagai penyaring.</p>
 *
 * <p><b>Gerbang hak akses</b> (terverifikasi dari kode, bukan asumsi):</p>
 *
 * <ul>
 *   <li>{@code RencanaGajiAction} (layar induk) memeriksa {@code CommonPrivilages.READ} di
 *       {@code doAfterCompose} dan menurunkan flag {@code edit}/{@code delete} ke tombol
 *       Ubah/Hapus barisnya sendiri — ini benar.</li>
 *   <li>{@code RencanaGajiPunyaPegawaiAction} (komponen detail yang menempel di setiap baris layar
 *       itu) <b>tidak memuat satu pun pemeriksaan hak akses</b>; kelas {@code CommonPrivilages}
 *       bahkan tidak diimpor, dan induknya {@code MyDetail} hanya membungkus {@code Detail} ZK.
 *       Ketiga tombolnya — "Ambil Data Pegawai", "Hitung Ulang", dan tombol sampah yang menjalankan
 *       {@code delete from payroll.rencana_gaji_punya_pegawai where rencana_gaji = ?} — aktif untuk
 *       siapa pun yang bisa membuka layar dengan hak BACA saja.</li>
 *   <li>Query pendukungnya ({@code initCriteria}) menyaring hanya berdasarkan dokumen rencana,
 *       {@code pegawai.aktif}, dan keberadaan {@code formatItemGaji} — <b>nol penyaring tenant</b>,
 *       sehingga "Hitung Ulang" dan tombol hapus bekerja atas seluruh pegawai seluruh tenant.</li>
 * </ul>
 *
 * <p>Permukaan kedua adalah Generic CRUD v2. Halaman
 * {@code WEB-INF/new/payroll/uiux/util/rencana_item_gaji_pegawai_tree_model.jsp} mencantumkan kelas
 * ini sebagai kandidat entity pertama, dan {@code GenericCrudAutoDefinitionFactory} memang memilihnya
 * (nama kelas tidak kena satu pun token {@code BLOCKED_CLASS_TOKENS}). Verifikasi rinci:</p>
 *
 * <ul>
 *   <li><b>Mutasi: TIDAK terjangkau.</b> {@code mutable} bergantung pada
 *       {@code GenericCrudExistingActionInvoker.supports(...)}, dan kelas sumber halaman ini
 *       ({@code RencanaItemGajiPegawaiTreeModel}) tidak punya field bertipe {@code MyWindow} maupun
 *       {@code boolean onSave(Event)}. Definisi jatuh ke {@code READ_ONLY}; create/update/delete
 *       dimatikan. Ini verifikasi <b>negatif yang menenangkan</b>.</li>
 *   <li><b>Baca dan ekspor: terjangkau, tanpa cakupan tenant.</b> Ekspor PDF/DOCX/PPTX tetap
 *       dihidupkan, dan {@code GenericCrudAutoEntityAdapter.scopeBindings()} hanya memasang
 *       pembatas untuk 12 nama properti tetap ({@code yayasan|sekolah|program|fakultas|jurusan|
 *       satuanKerja|mahasiswa|siswa|dosen|guru|orangTua|anggotaKoperasi}). Kelas ini tidak punya
 *       satu pun di antaranya; relasi {@code pegawai} yang dimilikinya juga <b>tidak</b> ada di
 *       daftar itu, dan {@code addScope()} menelan {@code missingProperty} secara diam-diam.
 *       Hasilnya pembatas kosong — persis kategori {@code task_7b6038ac}, yang memang sudah
 *       menyebut seluruh modul {@code payroll/*} sebagai terdampak.</li>
 *   <li>Hak baca sendiri <b>tidak</b> fail-open: {@code GenericCrudRoutePrivilegeResolver} fail-closed
 *       (mengembalikan {@code null} pada setiap kegagalan), tetapi bila tidak ada rute yang cocok,
 *       {@code GenericCrudRequestContext} tetap memakai privilese menu ZK yang aktif di <b>sesi</b> —
 *       pola pewarisan hak menu induk yang sudah dikenal di modul lain.</li>
 * </ul>
 *
 * <h2>Jejak audit (Envers)</h2>
 *
 * <p>Kelas ditandai {@code @Audited}, jadi setiap {@code session.save()} baris rencana tercatat di
 * {@code rencana_item_gaji_pegawai_aud}. Tetapi penghapusan di {@code reset()} dilakukan lewat
 * <b>SQL mentah</b> ({@code createSQLQuery(...).executeUpdate()}), yang melewati event listener
 * Hibernate dan karenanya <b>tidak pernah tercatat Envers</b>. Riwayat revisi jadi menyesatkan:
 * hanya berisi tumpukan penyisipan yang terus bertambah setiap kali "Hitung Ulang" ditekan, tanpa
 * satu pun penghapusan pasangannya.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ul>
 *   <li><b>Identitas &amp; jejak audit</b>: {@link #getId()}, {@link #setId(Long)},
 *       {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@code onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Relasi struktur</b>: {@link #getFormatItemGaji()}, {@link #getParent()},
 *       {@link #getRencanaGajiPunyaPegawai()}, {@link #getItemGajiPegawai()},
 *       {@link #getPegawai()}.</li>
 *   <li><b>Salinan atribut dari master</b> (getter menulis balik): {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getKode()}, {@link #getNomorUrut()},
 *       {@link #getAktif()}, {@link #getDefaultFormula()}, {@link #getTampilkanDiSlip()},
 *       {@link #getSpace()}.</li>
 *   <li><b>Nominal &amp; periode</b>: {@link #getNilai()}, {@link #getBulan()},
 *       {@link #getTahun()}.</li>
 *   <li><b>Sisa salinan dari kembar realisasi, tidak terpakai</b>: {@link #getAkun()},
 *       {@link #getAkunDebet()}, {@link #getPostingHistory()}, {@link #getDeep()},
 *       {@link #getJmlDipakai()}.</li>
 * </ul>
 *
 * <p><b>Catatan tentang {@link GeneralValueObject}:</b> kelas induk BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa dan Hibernate tidak memetakan propertinya. Field
 * seperti {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} yang dideklarasikan ulang di
 * sini <b>bukan duplikasi keliru</b>, melainkan keharusan teknis agar kolomnya benar-benar
 * terpetakan. Lihat {@link GeneralValueObject} untuk mekanisme {@code check()} yang dipakai getter
 * relasi di bawah.</p>
 *
 * @see GeneralValueObject
 * @see RencanaGajiPunyaPegawai
 * @see PembayaranItemGajiPegawai
 * @see ItemGajiPegawai
 * @see ItemGaji
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "rencana_item_gaji_pegawai")
public class RencanaItemGajiPegawai extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi. Nilainya sengaja identik dengan hampir seluruh entity
	 * {@code payroll} lain (termasuk {@link PembayaranItemGajiPegawai}) karena berkas-berkas itu
	 * lahir dari satu proses salin-tempel yang sama; jangan dipakai untuk membedakan kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY}. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris rencana ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> argumen {@code null} atau yang hanya berisi
	 * spasi diabaikan dan nilai lama dipertahankan (tidak ada exception, tidak ada log). Pola ini
	 * seragam di seluruh entity AIS dan bertujuan mencegah jejak audit terhapus oleh proses batch
	 * yang tidak membawa konteks pengguna — mis. utas latar {@code reset()} pada layar Rencana
	 * Gaji. Konsekuensinya, kolom ini <b>tidak bisa dikosongkan kembali</b> lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Label singkat baris rencana, dipakai komponen ZK dan pesan log.
	 *
	 * <p>Membaca <b>field</b> {@code kode} dan {@code nama} secara langsung, bukan lewat
	 * {@link #getKode()}/{@link #getNama()}. Bedanya nyata: kedua getter itu melakukan materialisasi
	 * malas dari {@link ItemGajiPegawai}, sedangkan method ini tidak. Pada baris yang baru dibaca
	 * dari database dan belum pernah getter-nya disentuh, hasilnya bisa berupa
	 * {@code "null - null"}.</p>
	 *
	 * @return gabungan {@code kode + " - " + nama} apa adanya dari field
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga kolom ini tidak dapat dikosongkan lewat setter.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris rencana ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyerahkan pembaruan stempel waktu ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris di-{@code UPDATE}.
	 *
	 * <p>Hanya berjalan pada <b>update</b>, tidak pada insert dan tidak pada penghapusan.
	 * Penghapusan massal di {@code RencanaItemGajiPegawaiTreeModel.reset()} memakai SQL mentah,
	 * sehingga tidak memicu callback ini maupun Envers.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berbagi baris dengan method ini (hasil
	 * penyuntingan massal lintas repo); nilai awalnya diambil dari {@code WaktuUtil.getDate()} saat
	 * objek dibuat, bukan saat disimpan. <b>Jangan memecah baris ini</b> tanpa alasan kuat — pola
	 * yang sama ada di puluhan entity lain dan dipakai sebagai penanda oleh perkakas penyapu.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek yang baru dibuat karena field-nya
	 *         diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama komponen gaji, disalin sekali dari master. Lihat {@link #getNama()}. */
	private String nama;
	/** Format gaji pemilik baris. Kolom {@code format_item_gaji}, NOT NULL. Lihat {@link #getFormatItemGaji()}. */
	private FormatItemGaji formatItemGaji;
	/** Induk hierarki dalam pohon komponen gaji. Kolom {@code bagian_dari}. Lihat {@link #getParent()}. */
	private RencanaItemGajiPegawai parent;
	/** Dokumen rencana per pegawai yang memiliki baris ini. Lihat {@link #getRencanaGajiPunyaPegawai()}. */
	private RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai;
	/** Komponen gaji per-pegawai yang menjadi sumber baris ini. Kolom {@code item_gaji}. Lihat {@link #getItemGajiPegawai()}. */
	private ItemGajiPegawai itemGajiPegawai;
	/** Pegawai pemilik rencana. Denormalisasi dari {@code rencanaGajiPunyaPegawai.pegawai}. Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Nominal rencana untuk komponen dan bulan ini. Lihat peringatan di {@link #getNilai()}. */
	private Double nilai;
	/** Urutan tampil dalam pohon. Kolom {@code urutan}. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Penanda aktif; dipakai sebagai penyaring oleh tree model. Lihat {@link #getAktif()}. */
	private Boolean aktif = true;
	/** Apakah komponen tampil di slip gaji. Ditimpa tanpa syarat, lihat {@link #getTampilkanDiSlip()}. */
	private Boolean tampilkanDiSlip = true;
	/** Kode komponen gaji; menjadi kunci dalam formula dan JSON komponen. Lihat {@link #getKode()}. */
	private String kode;
	/** Formula perhitungan yang dibekukan saat rencana disusun. Lihat {@link #getDefaultFormula()}. */
	private String defaultFormula;
	/** Keterangan bebas, disalin sekali dari master. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kedalaman node dalam pohon. Praktis mati — lihat {@link #getDeep()}. */
	private Integer deep;
	/** Pencacah pemakaian warisan dari master. Praktis mati — lihat {@link #getJmlDipakai()}. */
	private Long jmlDipakai = 0L;
	/** Penanda baris pemisah kosong pada slip. Ditimpa tanpa syarat, lihat {@link #getSpace()}. */
	private Boolean space = false;

	/** Akun kredit hasil snapshot dari master. Tidak pernah dipakai menjurnal — lihat {@link #getAkun()}. */
	private Akun akun;
	/** Akun debet hasil snapshot dari master. Tidak pernah dipakai menjurnal — lihat {@link #getAkunDebet()}. */
	private Akun akunDebet;

	/** Bulan rencana (1..12) yang menjadi konteks baris ini. Lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Tahun rencana. Lihat {@link #getTahun()}. */
	private Integer tahun;

	/** Cap posting akuntansi. Nol penulis di seluruh repo — lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/**
	 * Constructor tanpa argumen yang dibutuhkan Hibernate dan Generic CRUD v2.
	 *
	 * <p>Seluruh field relasi dibiarkan {@code null}; pengisian dilakukan
	 * {@code RencanaItemGajiPegawaiTreeModel.copyByItemGajiPegawai()} sesaat setelah instansiasi.</p>
	 */
	public RencanaItemGajiPegawai() {
	}

	/**
	 * Mengembalikan primary key baris rencana.
	 *
	 * <p>Kolom {@code id} bertipe {@code IDENTITY} dan ditandai {@code insertable = false} — nilainya
	 * selalu dibangkitkan database. Nomornya berurutan, jadi jangan diperlakukan sebagai identitas
	 * yang tidak bisa ditebak.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris.
	 *
	 * <p>Dipanggil eksplisit dengan {@code null} oleh {@code copyByFormat()} dan
	 * {@code copyByItemGajiPegawai()} untuk memaksa baris hasil salinan disimpan sebagai baris baru
	 * alih-alih menimpa baris sumbernya.</p>
	 *
	 * @param id id baris; {@code null} menandai objek belum tersimpan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama komponen gaji pada baris rencana ini.
	 *
	 * <p><b>Menulis balik.</b> Bila {@code nama} masih {@code null} dan {@link #getItemGajiPegawai()}
	 * tersedia, nama disalin dari master lalu <b>disimpan permanen</b> ke kolom ini pada flush
	 * berikutnya (property access + {@code dynamicUpdate}). Setelah terisi sekali, nama tidak lagi
	 * mengikuti perubahan master — pembekuan yang memang diinginkan untuk dokumen rencana, karena
	 * rencana harus tetap terbaca sebagaimana disusun.</p>
	 *
	 * <p>Kolom dipetakan {@code nullable = false}, tetapi tidak ada validasi yang menjamin master
	 * tersedia; baris tanpa {@code itemGajiPegawai} akan gagal di tingkat database, bukan di tingkat
	 * aplikasi.</p>
	 *
	 * @return nama komponen gaji, atau {@code null} bila belum terisi dan master tidak tersedia
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		itemGajiPegawai = getItemGajiPegawai();
		if (nama == null && itemGajiPegawai != null) {
			nama = itemGajiPegawai.getNama();
		}
		return this.nama;
	}

	/**
	 * Menetapkan nama komponen gaji pada baris ini.
	 *
	 * @param nama nama komponen; mengunci materialisasi malas di {@link #getNama()} bila bukan
	 *             {@code null}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan komponen gaji pada baris rencana ini.
	 *
	 * <p><b>Menulis balik</b> dengan pola yang sama seperti {@link #getNama()}: disalin dari
	 * {@link ItemGajiPegawai} hanya bila masih {@code null}, lalu dibekukan permanen.</p>
	 *
	 * @return keterangan komponen, atau {@code null} bila belum terisi dan master tidak tersedia
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		itemGajiPegawai = getItemGajiPegawai();
		if (keterangan == null && itemGajiPegawai != null) {
			keterangan = itemGajiPegawai.getKeterangan();
		}
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan komponen gaji pada baris ini.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan format gaji (kelompok aturan penggajian) pemilik baris ini.
	 *
	 * <p>Kolom {@code format_item_gaji} dipetakan {@code nullable = false}. Diisi
	 * {@code copyByItemGajiPegawai()} dari {@code itemGajiPegawai.getFormatItemGaji()}, sehingga
	 * selalu sama dengan format milik pegawai pada saat rencana dibuat.</p>
	 *
	 * <p><b>Kuirk:</b> berbeda dari hampir semua getter relasi di repo ini — dan berbeda dari
	 * kembarannya {@link PembayaranItemGajiPegawai#getFormatItemGaji()} — method ini <b>tidak</b>
	 * memanggil {@code check()}, jadi tidak ada resolusi proxy lazy. Aman dalam praktik karena
	 * {@code @ManyToOne} di sini bersifat eager ({@code @Fetch(FetchMode.SELECT)}, tanpa
	 * {@code FetchType.LAZY}), tetapi pada objek yang sudah lepas dari session tidak ada jaring
	 * pengaman seperti pada kembarannya.</p>
	 *
	 * <p>Ini juga satu-satunya jalan dari baris rencana menuju satuan kerja
	 * ({@code FormatItemGaji.getSatuanKerja()}) — dan tidak ada satu pun query di modul rencana gaji
	 * yang memakainya sebagai penyaring tenant.</p>
	 *
	 * @return format item gaji pemilik baris, atau {@code null} pada objek yang belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_item_gaji", nullable = false)
	public FormatItemGaji getFormatItemGaji() {
		return formatItemGaji;
	}

	/**
	 * Menetapkan format gaji pemilik baris ini.
	 *
	 * @param formatItemGaji format item gaji; kolomnya NOT NULL, jadi {@code null} akan gagal saat
	 *                       disimpan
	 */
	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}

	/**
	 * Mengembalikan baris induk dalam pohon komponen gaji (kolom {@code bagian_dari}).
	 *
	 * <p>{@code null} berarti baris ini berada di tingkat akar. Hierarki di sini adalah salinan
	 * hierarki {@link ItemGajiPegawai}: {@code copyByItemGajiPegawai()} membangunnya rekursif,
	 * meneruskan baris rencana yang baru dibuat sebagai induk bagi anak-anaknya.</p>
	 *
	 * <p><b>Kuirk yang sama dengan {@link #getFormatItemGaji()}:</b> tidak memanggil {@code check()},
	 * berbeda dari {@link PembayaranItemGajiPegawai#getParent()}. Perlu diperhatikan karena
	 * {@code RencanaItemGajiPegawaiTreeModel.populateData()} menaiki rantai induk dengan perulangan
	 * {@code while (parentPegawai != null)} — pada objek terlepas, rantai itu bergantung sepenuhnya
	 * pada eager fetch.</p>
	 *
	 * <p><b>Tidak ada penjaga siklus</b> di mana pun: baris yang (lewat Generic CRUD v2 atau
	 * manipulasi data langsung) menunjuk dirinya sendiri atau keturunannya sebagai induk akan
	 * membuat penelusuran rekursif tree model berputar tanpa henti.</p>
	 *
	 * @return baris induk, atau {@code null} bila baris ini berada di tingkat akar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bagian_dari", nullable = true)
	public RencanaItemGajiPegawai getParent() {
		return parent;
	}

	/**
	 * Menetapkan baris induk dalam pohon komponen gaji.
	 *
	 * @param parent baris induk, atau {@code null} untuk menempatkan baris ini di tingkat akar
	 */
	public void setParent(RencanaItemGajiPegawai parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan urutan tampil baris dalam pohon (kolom {@code urutan}).
	 *
	 * <p><b>Menulis balik</b> bila masih {@code null}: nilai disalin dari {@link ItemGajiPegawai}
	 * lalu dibekukan. Nilai yang dikembalikan disubstitusi menjadi {@code 0} bila tetap
	 * {@code null}, tetapi <b>substitusi itu tidak ikut ditulis</b> ke field — sehingga kolomnya bisa
	 * tetap {@code null} di database walaupun getter selalu memberi angka.</p>
	 *
	 * <p>Dipakai {@code RencanaItemGajiPegawaiTreeModel.getChildren()} sebagai pengurut utama
	 * ({@code Order.asc("nomorUrut")}, lalu {@code Order.asc("nama")}). Urutan ini juga menentukan
	 * komponen mana yang diproses terakhir oleh {@code copyByItemGajiPegawai()} — lihat catatan pada
	 * {@link #getNilai()} tentang bagaimana nilai komponen terakhir menjadi total bulanan.</p>
	 *
	 * @return urutan tampil; {@code 0} bila belum terisi
	 */
	@Column(name = "urutan")
	public Integer getNomorUrut() {
		itemGajiPegawai = getItemGajiPegawai();
		if (nomorUrut == null && itemGajiPegawai != null) {
			nomorUrut = itemGajiPegawai.getNomorUrut();
		}
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menetapkan urutan tampil baris dalam pohon.
	 *
	 * @param nomorUrut urutan tampil; {@code null} mengaktifkan kembali materialisasi malas dari
	 *                  master
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan penanda aktif baris rencana.
	 *
	 * <p><b>Menulis balik</b> bila masih {@code null}: nilai disalin dari {@link ItemGajiPegawai}
	 * lalu dibekukan. Berbeda dari {@link ItemGajiPegawai#getAktif()} yang menyubstitusi
	 * {@code true} untuk nilai {@code null}, method ini <b>bisa mengembalikan {@code null}</b> bila
	 * master juga tidak tersedia — pemanggil yang melakukan auto-unboxing ke {@code boolean} akan
	 * kena {@code NullPointerException}.</p>
	 *
	 * <p>Dipakai tree model sebagai penyaring: {@code getChildren()} dan {@code getChildCount()}
	 * menambahkan {@code Restrictions.eq("aktif", true)} kecuali dipanggil dengan
	 * {@code tampilkanSemua}. Karena {@code null} tidak sama dengan {@code true} di SQL, baris yang
	 * kolom {@code aktif}-nya {@code null} <b>hilang dari kedua mode penyaringan</b>.</p>
	 *
	 * @return {@code true}/{@code false}, atau {@code null} bila belum terisi dan master tidak
	 *         tersedia
	 */
	public Boolean getAktif() {
		itemGajiPegawai = getItemGajiPegawai();
		if (aktif == null && itemGajiPegawai != null) {
			aktif = itemGajiPegawai.getAktif();
		}
		return aktif;
	}

	/**
	 * Menetapkan penanda aktif baris rencana.
	 *
	 * @param aktif penanda aktif; boleh {@code null}, tetapi baris {@code null} akan luput dari
	 *              penyaring tree model
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode komponen gaji.
	 *
	 * <p><b>Menulis balik</b> bila masih {@code null}: disalin dari {@link ItemGajiPegawai} lalu
	 * dibekukan.</p>
	 *
	 * <p>Kode ini bukan sekadar label. Ia adalah kunci yang dipakai mesin formula
	 * ({@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai}) untuk mencocokkan token dalam
	 * {@link #getDefaultFormula()}, dan menjadi bagian kunci JSON {@code komponenGaji} pada induk
	 * dalam bentuk {@code <kode>_<bulan>} serta {@code RENC_TOT_<kode>}. Mengubah kode master
	 * setelah rencana disusun akan memutus pencocokan itu tanpa pesan kesalahan apa pun — formula
	 * yang menyebut kode lama cukup tidak tergantikan dan dievaluasi apa adanya.</p>
	 *
	 * @return kode komponen gaji, atau {@code null} bila belum terisi dan master tidak tersedia
	 */
	public String getKode() {
		itemGajiPegawai = getItemGajiPegawai();
		if (kode == null && itemGajiPegawai != null) {
			kode = itemGajiPegawai.getKode();
		}
		return kode;
	}

	/**
	 * Menetapkan kode komponen gaji.
	 *
	 * @param kode kode komponen; menjadi kunci pencocokan formula dan JSON komponen gaji
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan formula perhitungan komponen gaji yang berlaku bagi baris rencana ini.
	 *
	 * <p><b>Menulis balik</b> bila masih {@code null}: disalin dari {@link ItemGajiPegawai} lalu
	 * dibekukan — pembekuan yang penting, karena rencana harus tetap dapat dijelaskan dengan rumus
	 * yang dipakai saat menyusunnya, walaupun rumus master berubah kemudian.</p>
	 *
	 * <p>Nilai ini juga dapat <b>diganti sebelum baris disimpan</b>: {@code copyByItemGajiPegawai()}
	 * menerima peta {@code formulasBaru} (kunci = id {@code ItemGaji}) dan memanggil
	 * {@link #setDefaultFormula(String)} bila komponen ini termasuk di dalamnya, sehingga skenario
	 * "hitung ulang dengan asumsi rumus berbeda" bisa dijalankan tanpa mengubah master. Seluruh
	 * pemanggil yang ada saat ini mengirim {@code null} untuk peta itu.</p>
	 *
	 * <p>Berbeda dari {@link ItemGaji#getDefaultFormula()} yang menyubstitusi string kosong dan
	 * melakukan {@code trim}, method ini mengembalikan nilai apa adanya, termasuk {@code null}.</p>
	 *
	 * @return formula perhitungan, atau {@code null} bila belum terisi dan master tidak tersedia
	 */
	public String getDefaultFormula() {
		itemGajiPegawai = getItemGajiPegawai();
		if (defaultFormula == null && itemGajiPegawai != null) {
			defaultFormula = itemGajiPegawai.getDefaultFormula();
		}
		return defaultFormula;
	}

	/**
	 * Menetapkan formula perhitungan komponen gaji untuk baris ini.
	 *
	 * @param defaultFormula formula; dipakai {@code copyByItemGajiPegawai()} untuk menerapkan peta
	 *                       {@code formulasBaru} sebelum baris disimpan
	 */
	public void setDefaultFormula(String defaultFormula) {
		this.defaultFormula = defaultFormula;
	}

	/**
	 * Mengembalikan kedalaman baris dalam pohon komponen gaji.
	 *
	 * <p><b>Praktis mati.</b> Satu-satunya penulisnya adalah
	 * {@code RencanaItemGajiPegawaiTreeModel.getParentCount()}, dan method itu <b>nol pemanggil</b>
	 * di seluruh repo. Nilai kolom ini karenanya selalu {@code null} pada data yang dibuat lewat
	 * jalur normal. Kedalaman yang benar-benar dipakai untuk indentasi dihitung ulang saat render
	 * dengan menaiki rantai {@link #getParent()}.</p>
	 *
	 * @return kedalaman node, praktis selalu {@code null}
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menetapkan kedalaman baris dalam pohon.
	 *
	 * @param deep kedalaman node; tidak dibaca oleh jalur mana pun yang aktif
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan pencacah pemakaian komponen.
	 *
	 * <p><b>Mati total pada kelas ini.</b> Field diinisialisasi {@code 0L} dan tidak ada satu pun
	 * pemanggil {@link #setJmlDipakai(Long)} di repo; kolom ini hanya ikut tersalin ketika struktur
	 * {@link ItemGaji}/{@link ItemGajiPegawai} diduplikasi menjadi entity rencana.</p>
	 *
	 * @return pencacah pemakaian; praktis selalu {@code 0}
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menetapkan pencacah pemakaian komponen.
	 *
	 * @param jmlDipakai jumlah pemakaian; tidak dipakai jalur aktif mana pun
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan komponen gaji per-pegawai ({@link ItemGajiPegawai}) yang menjadi sumber baris
	 * rencana ini. Kolom {@code item_gaji}.
	 *
	 * <p>Memanggil {@code check()} dari {@link GeneralValueObject} untuk meresolusi proxy lazy
	 * ({@code FetchType.LAZY}), lalu <b>menugaskan kembali hasilnya ke field</b> — pola standar repo
	 * ini, dan alasan mengapa method ini dipanggil dari hampir semua getter lain di kelas ini
	 * sebelum mereka menyalin atributnya.</p>
	 *
	 * <p>Relasi ini adalah gerbang ke seluruh rantai master:
	 * {@code itemGajiPegawai.getItemGaji()} memberi {@link ItemGaji} global (sumber
	 * {@code jadikan0JikaMinus}, akun, dan tarif), sedangkan {@code itemGajiPegawai} sendiri
	 * membawa penyesuaian khusus pegawai. Kolomnya {@code nullable = true}, jadi semua getter yang
	 * bergantung padanya wajib memeriksa {@code null} — dan memang melakukannya.</p>
	 *
	 * @return komponen gaji per-pegawai, atau {@code null} bila baris tidak terkait master mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_gaji", nullable = true)
	public ItemGajiPegawai getItemGajiPegawai() {
		itemGajiPegawai = check(itemGajiPegawai);
		return itemGajiPegawai;
	}

	/**
	 * Menetapkan komponen gaji per-pegawai sumber baris ini.
	 *
	 * @param itemGajiPegawai komponen gaji per-pegawai; boleh {@code null}
	 */
	public void setItemGajiPegawai(ItemGajiPegawai itemGajiPegawai) {
		this.itemGajiPegawai = itemGajiPegawai;
	}

	/**
	 * Mengembalikan penanda apakah komponen ini ditampilkan pada slip gaji.
	 *
	 * <p><b>PERINGATAN — penimpaan TANPA SYARAT.</b> Berbeda dari {@link #getNama()} dan
	 * kerabatnya, method ini <b>tidak</b> memeriksa apakah nilai lokal masih {@code null}: selama
	 * {@link #getItemGajiPegawai()} tersedia, nilai tersimpan selalu ditimpa oleh nilai master pada
	 * <b>setiap pembacaan</b>. Karena pemetaan memakai property access dan
	 * {@code dynamicUpdate = true}, penimpaan itu ikut ter-flush ke database.</p>
	 *
	 * <p>Akibatnya kolom ini secara efektif <b>tidak dapat menyimpan keputusan sendiri</b> — setiap
	 * suntingan operator akan kembali mengikuti master tanpa pemberitahuan, dan menyimpannya sebagai
	 * kolom terpisah tidak memberi manfaat apa pun. Kuirk identik ada di
	 * {@link PembayaranItemGajiPegawai#getTampilkanDiSlip()} (kembar salin-tempelnya), sehingga ini
	 * jelas warisan pola, bukan keputusan lokal.</p>
	 *
	 * <p>Dibaca {@code RencanaItemGajiPegawaiTreeModel.populateData()} sebagai gerbang: baris dengan
	 * nilai {@code false} dilewati seluruhnya saat menyusun daftar tampil.</p>
	 *
	 * @return {@code true} bila komponen tampil di slip; nilai master menang atas nilai tersimpan
	 */
	public Boolean getTampilkanDiSlip() {
		itemGajiPegawai = getItemGajiPegawai();
		if (itemGajiPegawai != null) {
			tampilkanDiSlip = itemGajiPegawai.getTampilkanDiSlip();
		}
		return tampilkanDiSlip;
	}

	/**
	 * Menetapkan penanda tampil-di-slip.
	 *
	 * <p>Efeknya <b>tidak bertahan</b>: pembacaan berikutnya lewat {@link #getTampilkanDiSlip()}
	 * akan menimpanya kembali dengan nilai master selama {@link #getItemGajiPegawai()} tersedia.</p>
	 *
	 * @param tampilkanDiSlip penanda tampil di slip gaji
	 */
	public void setTampilkanDiSlip(Boolean tampilkanDiSlip) {
		this.tampilkanDiSlip = tampilkanDiSlip;
	}

	/**
	 * Mengembalikan dokumen rencana per-pegawai yang memiliki baris ini (kolom
	 * {@code rencana_gaji_punya_pegawai}).
	 *
	 * <p>Ini adalah <b>satu-satunya sumbu pengelompokan</b> yang dipakai seluruh query tree model:
	 * {@code getChildren()}, {@code getChildCount()}, {@code getParentSet()}, {@code copyByFormat()},
	 * dan {@code checkExistingItemGaji()} semuanya menyaring dengan
	 * {@code Restrictions.eq("rencanaGajiPunyaPegawai", ...)}. Lewat induk inilah baris rencana
	 * terhubung ke {@link RencanaGaji} (tahun anggaran) dan ke {@link Pegawai}.</p>
	 *
	 * <p><b>Perhatikan:</b> tidak satu pun query itu menyaring {@link #getBulan()} atau
	 * {@link #getTahun()}. Karena {@code reset()} membuat satu set baris lengkap untuk <i>setiap</i>
	 * bulan yang belum punya realisasi disetujui, satu dokumen rencana bisa memuat beberapa salinan
	 * pohon komponen yang sama — dan pembacaan lewat tree model akan menampilkan semuanya bercampur
	 * tanpa pembeda bulan. Kolomnya {@code nullable = true}, sehingga baris tanpa induk (mis. hasil
	 * penyisipan lewat jalur lain) tidak akan pernah muncul di query mana pun.</p>
	 *
	 * <p>Seperti {@link #getFormatItemGaji()} dan {@link #getParent()}, method ini tidak memanggil
	 * {@code check()}; relasinya eager.</p>
	 *
	 * @return dokumen rencana per-pegawai pemilik baris, atau {@code null} bila baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "rencana_gaji_punya_pegawai", nullable = true)
	public RencanaGajiPunyaPegawai getRencanaGajiPunyaPegawai() {
		return rencanaGajiPunyaPegawai;
	}

	/**
	 * Menetapkan dokumen rencana per-pegawai pemilik baris ini.
	 *
	 * <p>Dipanggil {@code copyByFormat()} dan {@code copyByItemGajiPegawai()} untuk memindahkan atau
	 * membuat baris di bawah dokumen rencana tujuan.</p>
	 *
	 * @param rencanaGajiPunyaPegawai dokumen rencana pemilik; {@code null} membuat baris yatim yang
	 *                                tidak terjangkau query mana pun
	 */
	public void setRencanaGajiPunyaPegawai(RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai) {
		this.rencanaGajiPunyaPegawai = rencanaGajiPunyaPegawai;
	}

	/**
	 * Mengembalikan nominal rencana untuk komponen dan bulan ini.
	 *
	 * <p><b>PERINGATAN — getter ini MENGUBAH NOMINAL secara permanen.</b> Dua penugasan dilakukan ke
	 * field, dan karena pemetaan memakai property access dengan {@code dynamicUpdate = true},
	 * keduanya ikut ter-flush ke database. Membaca baris cukup untuk mengubah baris:</p>
	 *
	 * <ol>
	 *   <li>{@code nilai == null} &rarr; ditugaskan {@code 0.0}. Relatif jinak: membedakan "belum
	 *       dihitung" dari "dihitung nol" jadi tidak mungkin lagi setelah pembacaan pertama.</li>
	 *   <li>Bila master {@code ItemGaji} bertanda {@code jadikan0JikaMinus} dan {@code nilai}
	 *       negatif &rarr; ditugaskan {@code 0.0}. <b>Merusak:</b> angka rencana negatif — misalnya
	 *       potongan atau koreksi yang memang disimpan bertanda minus — hilang permanen, hanya karena
	 *       barisnya dirender di layar, diekspor lewat Generic CRUD v2, atau tersentuh dirty-check
	 *       Hibernate. Nilai aslinya tidak dapat dipulihkan dari tabel utama; hanya tabel Envers
	 *       {@code rencana_item_gaji_pegawai_aud} yang mungkin masih menyimpan revisi sebelumnya.</li>
	 * </ol>
	 *
	 * <p>Perlu dicatat bahwa {@code copyByItemGajiPegawai()} <b>sudah</b> menerapkan penjepitan
	 * {@code jadikan0JikaMinus} yang sama sebelum memanggil {@link #setNilai(Double)}, sehingga
	 * baris yang lahir lewat jalur normal seharusnya tidak pernah negatif. Kaki kedua di atas baru
	 * menggigit pada data yang masuk lewat jalur lain, atau bila tanda {@code jadikan0JikaMinus}
	 * dinyalakan pada master <i>setelah</i> rencana tersusun — dan dalam kasus itu ia bekerja secara
	 * <b>retroaktif</b> atas seluruh baris lama begitu dibaca.</p>
	 *
	 * <p><b>Bukan total.</b> Nominal di sini adalah nilai satu komponen. Total bulanan yang benar-benar
	 * dipakai laporan disimpan pada induk ({@code RencanaGajiPunyaPegawai.nilai1..nilai12}) dan diisi
	 * {@code reset()} dari <b>nilai kembalian</b> {@code copyByItemGajiPegawai()} — yang secara
	 * struktural adalah nilai komponen akar <b>terakhir</b> menurut urutan {@link #getNomorUrut()},
	 * bukan penjumlahan. Rancangan itu hanya benar selama komponen akar terakhir memang komponen
	 * total/take-home-pay; tanda {@code ItemGaji.finalGaji} yang seharusnya menandai komponen final
	 * tidak dikonsultasi sama sekali di jalur ini.</p>
	 *
	 * @return nominal komponen; tidak pernah {@code null} setelah pemanggilan ini, dan tidak pernah
	 *         negatif bila master bertanda {@code jadikan0JikaMinus}
	 */
	public Double getNilai() {
		itemGajiPegawai = getItemGajiPegawai();
		if (nilai == null) {
			nilai = 0.0;
		}

		if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
				&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && nilai < 0.0) {
			nilai = 0.0;
		}

		return nilai;
	}

	/**
	 * Menetapkan nominal rencana untuk komponen dan bulan ini.
	 *
	 * <p>Dipanggil {@code copyByItemGajiPegawai()} dengan hasil evaluasi formula
	 * ({@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai}) yang sudah dijepit ke {@code 0.0}
	 * bila master bertanda {@code jadikan0JikaMinus}. Tidak ada validasi rentang: nilai apa pun,
	 * termasuk negatif dan {@code null}, diterima apa adanya — tetapi lihat {@link #getNilai()},
	 * yang dapat mengubahnya kembali pada pembacaan berikutnya.</p>
	 *
	 * @param nilai nominal komponen; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan akun buku besar sisi kredit untuk komponen gaji ini (kolom {@code akun}).
	 *
	 * <p><b>Menulis balik.</b> Setelah {@code check()} meresolusi proxy, bila {@code akun} masih
	 * {@code null} nilainya diambil dari rantai
	 * {@code itemGajiPegawai.getItemGaji().getAkun()} lalu <b>disimpan permanen</b> ke baris ini —
	 * snapshot, bukan pembacaan langsung berulang.</p>
	 *
	 * <p>Yang membuat snapshot ini non-obvious: {@link ItemGaji#getAkun()} <b>sendiri</b> melakukan
	 * live-override lewat {@code ambilAkun()}, yang mencari akun berdasarkan
	 * {@code KelompokItemGaji} dan {@code FormatItemGaji.getSatuanKerja()} pada saat itu juga
	 * (dengan blok {@code catch} senyap bila gagal). Jadi akun yang terbekukan di baris rencana
	 * adalah akun yang berlaku <b>pada pembacaan pertama baris ini</b>, bukan pada saat rencana
	 * disusun maupun pada saat gaji dibayarkan.</p>
	 *
	 * <p><b>Kolom ini tidak pernah dipakai.</b> Penyisiran repo tidak menemukan satu pun mesin
	 * posting, laporan, atau helper yang membaca akun dari tabel rencana — rencana gaji memang tidak
	 * pernah dijurnal. Kolomnya ada karena kelas ini disalin dari
	 * {@link PembayaranItemGajiPegawai}, tempat akun memang bermakna.</p>
	 *
	 * @return akun kredit hasil snapshot, atau {@code null} bila rantai master tidak menyediakannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		if (akun == null) {
			itemGajiPegawai = getItemGajiPegawai();
			if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
					&& itemGajiPegawai.getItemGaji().getAkun() != null) {
				akun = itemGajiPegawai.getItemGaji().getAkun();
			}
		}

		return akun;
	}

	/**
	 * Menetapkan akun buku besar sisi kredit untuk baris ini.
	 *
	 * <p>Diisi {@code copyByItemGajiPegawai()} langsung dari
	 * {@code itemGajiPegawai.getItemGaji().getAkun()} saat baris dibuat, sehingga jalur fallback di
	 * {@link #getAkun()} umumnya tidak pernah menyala pada data normal.</p>
	 *
	 * @param akun akun buku besar; boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan akun buku besar sisi debet untuk komponen gaji ini (kolom {@code akun_debet}).
	 *
	 * <p>Perilakunya identik dengan {@link #getAkun()} — <b>menulis balik</b> snapshot dari
	 * {@code itemGajiPegawai.getItemGaji().getAkunDebet()} bila masih {@code null}, dengan sumber
	 * yang juga live-override ({@code ItemGaji.ambilAkunDebet()} lewat
	 * {@code KelompokItemGaji.getAkunDebet()} dan satuan kerja format). Sama seperti kembarannya,
	 * <b>tidak ada konsumen</b> di seluruh repo.</p>
	 *
	 * @return akun debet hasil snapshot, atau {@code null} bila rantai master tidak menyediakannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_debet", nullable = true)
	public Akun getAkunDebet() {
		akunDebet = check(akunDebet);
		if (akunDebet == null) {
			itemGajiPegawai = getItemGajiPegawai();
			if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
					&& itemGajiPegawai.getItemGaji().getAkunDebet() != null) {
				akunDebet = itemGajiPegawai.getItemGaji().getAkunDebet();
			}
		}

		return akunDebet;
	}

	/**
	 * Menetapkan akun buku besar sisi debet untuk baris ini.
	 *
	 * @param akunDebet akun buku besar; boleh {@code null}
	 */
	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

	/**
	 * Mengembalikan pegawai pemilik baris rencana ini (kolom {@code pegawai}).
	 *
	 * <p>Memanggil {@code check()} untuk meresolusi proxy lazy. Kolom ini adalah
	 * <b>denormalisasi</b>: pegawai sebenarnya sudah dapat dicapai lewat
	 * {@code rencanaGajiPunyaPegawai.getPegawai()}, dan {@code copyByItemGajiPegawai()} memang
	 * mengisinya persis dari sana. Tidak ada penjaga apa pun yang memastikan keduanya tetap
	 * konsisten bila salah satunya diubah kemudian.</p>
	 *
	 * <p>Relevan untuk cakupan akses: {@code pegawai} adalah satu-satunya relasi "kepemilikan" pada
	 * kelas ini, dan nama properti itu <b>tidak termasuk</b> 12 nama yang dikenali
	 * {@code GenericCrudAutoEntityAdapter.scopeBindings()} — sehingga pembatas cakupan Generic CRUD
	 * v2 untuk entity ini kosong. Lihat pembahasan pada Javadoc kelas.</p>
	 *
	 * @return pegawai pemilik baris, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan pegawai pemilik baris rencana ini.
	 *
	 * @param pegawai pegawai pemilik; seharusnya selalu sama dengan
	 *                {@code rencanaGajiPunyaPegawai.getPegawai()}, tetapi tidak ada yang
	 *                menegakkannya
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan penanda "baris pemisah kosong" pada tampilan slip/pohon.
	 *
	 * <p><b>PERINGATAN — penimpaan TANPA SYARAT</b>, persis seperti
	 * {@link #getTampilkanDiSlip()}: selama {@link #getItemGajiPegawai()} tersedia, nilai tersimpan
	 * selalu ditimpa nilai master pada setiap pembacaan dan penimpaan itu ikut ter-flush. Kolom ini
	 * karenanya tidak pernah bisa berbeda dari masternya.</p>
	 *
	 * <p>Dibaca {@code RencanaItemGajiPegawaiTreeModel.populateData()}: baris ber-{@code space}
	 * {@code true} dilewati dari penelusuran anak <b>dan</b> dari pengambilan nominal, lalu
	 * dimasukkan ke daftar dengan nilai {@code null} — menghasilkan baris kosong sebagai pemisah
	 * visual.</p>
	 *
	 * @return {@code true} bila baris ini hanya pemisah visual; nilai master menang atas nilai
	 *         tersimpan
	 */
	public Boolean getSpace() {
		itemGajiPegawai = getItemGajiPegawai();
		if (itemGajiPegawai != null) {
			space = itemGajiPegawai.getSpace();
		}
		return space;
	}

	/**
	 * Menetapkan penanda baris pemisah kosong.
	 *
	 * <p>Efeknya <b>tidak bertahan</b>: {@link #getSpace()} akan menimpanya kembali dengan nilai
	 * master pada pembacaan berikutnya.</p>
	 *
	 * @param space penanda baris pemisah
	 */
	public void setSpace(Boolean space) {
		this.space = space;
	}

	/**
	 * Mengembalikan cap posting akuntansi yang menaungi baris ini (kolom {@code posting_history}).
	 *
	 * <p><b>Selalu {@code null} pada data nyata.</b> Tidak ada satu pun pemanggil
	 * {@link #setPostingHistory(PostingHistory)} di seluruh repo, dan tidak ada mesin posting yang
	 * membaca tabel rencana. Kolom ini murni sisa salinan dari
	 * {@link PembayaranItemGajiPegawai}, tempat cap posting memang menandai bahwa nominal gaji sudah
	 * diakui ke buku besar.</p>
	 *
	 * <p>Konsisten dengan sifat entity ini: rencana adalah <b>anggaran</b>, bukan transaksi — tidak
	 * ada jurnal yang boleh terbit darinya. Bila kolom ini kelak dihidupkan, perlu diperiksa lebih
	 * dulu bahwa {@code copyByFormat()} — yang menyalin baris lewat {@code clone()} — tidak ikut
	 * menyalin cap posting ke baris rencana baru.</p>
	 *
	 * @return cap posting; praktis selalu {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan cap posting akuntansi untuk baris ini.
	 *
	 * @param postingHistory cap posting; nol pemanggil di seluruh repo
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan bulan (1..12) yang menjadi konteks baris rencana ini.
	 *
	 * <p>Diisi {@code copyByItemGajiPegawai()} dari perulangan bulan di {@code reset()}. Nilai
	 * dikembalikan apa adanya, termasuk {@code null} — tidak ada substitusi maupun validasi
	 * rentang.</p>
	 *
	 * <p><b>Non-obvious:</b> walaupun setiap baris membawa bulannya sendiri, <b>tidak ada satu pun
	 * query di repo yang menyaring berdasarkan kolom ini</b>. Seluruh query tree model hanya
	 * menyaring {@link #getRencanaGajiPunyaPegawai()} dan {@link #getParent()}. Karena
	 * {@code reset()} membuat satu set pohon komponen untuk <i>setiap</i> bulan yang belum punya
	 * realisasi disetujui, satu dokumen rencana bisa berisi hingga dua belas salinan pohon yang sama
	 * — dan pembacaan lewat tree model akan menampilkan semuanya sebagai satu daftar datar tanpa
	 * pembeda bulan.</p>
	 *
	 * @return bulan rencana (1..12), atau {@code null} bila belum diisi
	 */
	public Integer getBulan() {
		return bulan;
	}

	/**
	 * Menetapkan bulan konteks baris rencana ini.
	 *
	 * @param bulan bulan 1..12; tidak divalidasi
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan tahun anggaran yang menjadi konteks baris rencana ini.
	 *
	 * <p>Diisi {@code copyByItemGajiPegawai()} dari argumen {@code tahun} yang diteruskan pemanggil
	 * {@code reset()} — yaitu {@code rencanaGaji.getTahun()}. Sama seperti {@link #getBulan()},
	 * nilainya dikembalikan apa adanya dan <b>tidak pernah dipakai sebagai penyaring</b>; tahun
	 * efektif selalu diambil dari dokumen {@link RencanaGaji} lewat rantai induk.</p>
	 *
	 * <p>Perlu diingat {@link RencanaGaji#getTahun()} menyubstitusi tahun berjalan bila kolomnya
	 * {@code null}, sehingga nilai yang tersalin ke sini bisa berasal dari jam sistem, bukan dari
	 * data yang benar-benar tersimpan.</p>
	 *
	 * @return tahun rencana, atau {@code null} bila belum diisi
	 */
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Menetapkan tahun anggaran konteks baris rencana ini.
	 *
	 * @param tahun tahun anggaran; tidak divalidasi terhadap tahun dokumen {@link RencanaGaji} induk
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

}
