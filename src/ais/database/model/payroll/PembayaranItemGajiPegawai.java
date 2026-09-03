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
 * <h3>PembayaranItemGajiPegawai &mdash; satu BARIS RINCIAN komponen gaji pada satu slip gaji
 * pegawai untuk satu periode (tabel {@code payroll.pembayaran_item_gaji_pegawai})</h3>
 *
 * <p>Entity ini adalah <b>daun</b> dari rantai penggajian AIS: satu baris = satu komponen gaji
 * (Gaji Pokok, Tunjangan Jabatan, Potongan BPJS, &hellip;) berikut <b>nominal aktual dalam Rupiah</b>
 * yang benar-benar dibayarkan kepada <i>satu</i> pegawai pada <i>satu</i> periode gaji. Kumpulan
 * baris inilah isi tabel slip gaji yang dicetak, dijurnal, dan diagregasi dasbor. Nominalnya
 * disimpan di {@link #getNilai()}; label komponennya di {@link #getNama()}/{@link #getKode()}.</p>
 *
 * <h3>Posisi dalam rantai penggajian (TERVERIFIKASI dari kode)</h3>
 * <p>Ada <b>tiga</b> lapis katalog di hulu entity ini, bukan dua &mdash; ini koreksi penting
 * terhadap dugaan yang wajar muncul dari nama kolomnya:</p>
 * <ol>
 *   <li>{@link ais.database.model.payroll.ItemGaji} &mdash; katalog komponen gaji tingkat
 *       instalasi (nama, kode, rumus bawaan, akun jurnal, bendera {@code jadikan0JikaMinus}).</li>
 *   <li>{@link ais.database.model.payroll.ItemGajiPegawai} &mdash; penugasan komponen katalog itu
 *       ke SEORANG pegawai (boleh menimpa rumus/urutan/kode untuk pegawai tersebut).</li>
 *   <li><b>Kelas ini</b> &mdash; hasil eksekusi rumus untuk satu periode; nominal beku.</li>
 * </ol>
 * <p><b>Kuirk penamaan kolom yang menyesatkan:</b> {@link #getItemGajiPegawai()} dipetakan ke
 * kolom bernama {@code item_gaji}, tetapi tipenya <b>{@code ItemGajiPegawai}</b>, BUKAN
 * {@code ItemGaji}. Entity ini <b>tidak punya FK langsung ke {@code ItemGaji}</b> sama sekali;
 * katalog tingkat instalasi hanya dijangkau dua lompatan lewat
 * {@code getItemGajiPegawai().getItemGaji()} &mdash; persis pola yang dipakai
 * {@link #getAkun()}, {@link #getAkunDebet()}, dan {@link #getNilai()}. Siapa pun yang membaca
 * nama kolom {@code item_gaji} pada tabel/SQL mentah akan salah menyimpulkan targetnya.</p>
 *
 * <p>Sisi hilir, {@link #getPembayaranGajiPunyaPegawai()} adalah FK ke slip gaji
 * ({@link ais.database.model.payroll.PembayaranGajiPunyaPegawai}) &mdash; TERVERIFIKASI: seluruh
 * pembaca menyaring dengan {@code Restrictions.eq("pembayaranGajiPunyaPegawai", &hellip;)}. Slip
 * itu sendiri bergantung pada {@code PembayaranGaji} (batch penggajian per satuan kerja per
 * bulan/tahun). Jadi jembatannya berbentuk <b>slip &rarr; baris rincian &rarr; penugasan komponen
 * &rarr; katalog komponen</b>, bukan jembatan many-to-many langsung slip&harr;katalog.</p>
 *
 * <h3>Bentuk POHON, bukan daftar datar</h3>
 * <p>{@link #getParent()} adalah FK ke DIRINYA SENDIRI (kolom {@code bagian_dari}), sehingga
 * rincian slip berupa pohon: komponen induk (mis. "Total Penghasilan") menaungi anak-anaknya.
 * {@code PembayaranItemGajiPegawaiTreeModel} merender pohon ini dengan indentasi enam spasi per
 * tingkat kedalaman dan menghitung ulang seluruh cabang secara rekursif. Perhatikan bahwa baris
 * induk <b>juga menyimpan nominalnya sendiri</b>; agregasi apa pun yang menjumlahkan seluruh baris
 * tanpa memandang tingkat akan menghitung ganda (dasbor menyiasatinya dengan mengelompokkan per
 * NAMA komponen, sehingga baris induk muncul sebagai kelompok tersendiri).</p>
 *
 * <h3>Snapshot &mdash; dan getter yang MENULIS BALIK</h3>
 * <p>Rancangan entity ini adalah <b>snapshot malas</b> (<i>lazy snapshot</i>): sebagian besar
 * atribut deskriptif dideklarasikan ulang di sini agar nilainya beku per periode, tetapi
 * <b>tidak diisi saat baris dibuat</b>. Yang mengisinya adalah <b>getter</b>-nya sendiri, yang
 * menyalin nilai dari {@code itemGajiPegawai} bila field lokal masih {@code null} lalu
 * <b>menyimpan hasil salinan itu ke field</b>. Karena kelas ini dipetakan Hibernate secara
 * <i>property access</i> (anotasi berada di getter) dengan {@code dynamicUpdate = true}, getter
 * inilah yang dipanggil Hibernate saat INSERT dan saat pemeriksaan kotor (<i>dirty checking</i>);
 * hasil tulis-balik otomatis ikut ter-<i>flush</i> ke basis data.</p>
 * <p>Konsekuensi yang <b>bukan</b> bug melainkan keharusan: {@code copyByItemGajiPegawaiRecursive}
 * di {@code PembayaranItemGajiPegawaiTreeModel} <b>tidak pernah memanggil {@code setNama(...)}</b>,
 * padahal kolom {@code nama} dideklarasikan {@code nullable = false}. Baris tetap tersimpan justru
 * karena {@link #getNama()} mengisi dirinya sendiri saat Hibernate membacanya untuk INSERT. Jadi
 * tulis-balik di sini adalah <b>bagian sah dari jalur simpan</b>, bukan efek samping tak sengaja.
 * Menghapus tulis-balik tanpa menambahkan {@code setNama(...)} di hulu akan mematahkan
 * penyimpanan slip.</p>
 * <p>Kelompok getter tulis-balik <b>bersyarat</b> (hanya mengisi bila masih {@code null}, jadi
 * snapshot yang sudah terisi aman): {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getNomorUrut()}, {@link #getAktif()}, {@link #getKode()}, {@link #getDefaultFormula()},
 * {@link #getAkun()}, {@link #getAkunDebet()}.</p>
 * <p>Kelompok getter yang <b>MENIMPA TANPA SYARAT</b> &mdash; ini yang non-obvious dan merusak
 * sifat snapshot: {@link #getTampilkanDiSlip()} dan {@link #getSpace()} selalu menyalin ulang dari
 * master pada setiap pembacaan, sehingga pilihan tampil/sembunyi yang dulu berlaku pada slip
 * historis <b>hilang permanen</b> begitu master diubah dan slip lama dibuka kembali.</p>
 * <p>Getter <b>destruktif atas nominal</b>: {@link #getNilai()} membulatkan nilai negatif menjadi
 * {@code 0.0} bila katalog menyalakan {@code jadikan0JikaMinus}, dan menyimpannya. Sekali dibaca,
 * angka aslinya tidak dapat dipulihkan dari baris ini. Lihat catatan lengkapnya di getter.</p>
 *
 * <h3>Jalur jurnal akuntansi</h3>
 * <p>{@link #getAkunDebet()} memasok kaki DEBET dan {@link #getAkun()} memasok kaki KREDIT jurnal
 * penggajian. Empat mesin posting memakainya dengan kriteria yang identik:
 * {@code PostingTransaksiPenggajianAction} (tombol layar, tombol massal, dan jalur API
 * {@code postingSemua}) serta {@code PostingTransaksiPembayaranGajiAction} (rekap per batch
 * {@code PembayaranGaji}). Semuanya menyaring
 * {@code gt("nilai", 0.1) AND (akun IS NOT NULL OR akun_debet IS NOT NULL)}, lalu menutup jurnal
 * dengan kredit ke akun bank pegawai atau akun cara pembayaran gaji sebesar nilai neto slip.</p>
 * <p>Dua asimetri penting antara penyaring dan getter:</p>
 * <ul>
 *   <li>Penyaringnya bekerja pada <b>kolom tersimpan</b>, sedangkan
 *       {@link #getAkun()}/{@link #getAkunDebet()} punya <i>fallback</i> ke katalog. Baris yang
 *       lolos saring karena {@code akun_debet} terisi tetapi {@code akun} masih {@code null} akan
 *       <b>menumbuhkan kaki kredit baru</b> saat getter-nya dibaca &mdash; jurnal yang terbentuk
 *       bisa berbeda dari yang tersirat di data tersimpan.</li>
 *   <li><i>Fallback</i> akun itu bersifat <b>live-read</b>, bukan snapshot: memindahkan akun pada
 *       {@link ais.database.model.payroll.ItemGaji} mengubah akun jurnal <b>slip lama</b> yang
 *       kolom akunnya masih kosong &mdash; dan tulis-baliknya membekukan pilihan baru itu secara
 *       permanen pada baris lama. Bandingkan dengan koreksi serupa yang pernah dicatat untuk
 *       {@code JenisReimbursement} (snapshot) versus Uang Muka/Kas Besar (live).</li>
 * </ul>
 * <p>Karena penyaring memakai {@code nilai &gt; 0,1}, komponen <b>potongan yang disimpan sebagai
 * angka negatif tidak pernah masuk jurnal sama sekali</b> &mdash; konvensi yang dipakai instalasi
 * adalah menyimpan potongan sebagai angka positif pada akun debet/kredit yang sesuai.</p>
 *
 * <h3>Field vestigial (ada di skema, tidak dipakai)</h3>
 * <ul>
 *   <li>{@link #getPostingHistory()} &mdash; <b>tidak pernah ditulis maupun dibaca</b> di seluruh
 *       repo (diverifikasi menyeluruh). Stempel posting yang sesungguhnya dipasang pada
 *       {@code PembayaranGajiPunyaPegawai}. Kolom {@code posting_history} di tabel ini selalu
 *       NULL. Pola yang sama muncul di {@code RencanaItemGajiPegawai}.</li>
 *   <li>{@link #getDeep()} &mdash; <b>ditulis</b> oleh {@code getParentCountRecursive} di
 *       tree model, tetapi <b>tidak pernah dibaca</b> untuk entity ini (pembaca {@code getDeep()}
 *       yang ada hanya untuk {@code ItemGajiPegawai} dan {@code ItemGaji}). Kedalaman indentasi
 *       slip dihitung ulang dengan menelusuri {@link #getParent()}, bukan dari kolom ini.</li>
 *   <li>{@link #getJmlDipakai()} &mdash; nol pembaca dan nol penulis di luar kelas ini; sisa
 *       salin-tempel dari katalog {@code ItemGaji}.</li>
 * </ul>
 *
 * <h3>Sumbu tenant &mdash; TIDAK ADA kolom tenant langsung</h3>
 * <p>Entity ini <b>tidak punya</b> properti {@code satuanKerja}, {@code sekolah}, {@code yayasan},
 * maupun properti institusi lain. Satu-satunya jalan ke tenant adalah dua lompatan:
 * {@code formatItemGaji.satuanKerja} (atau {@code pegawai.satuanKerja}). Ini punya konsekuensi
 * keamanan konkret yang dibahas di bawah.</p>
 *
 * <h3>PERINGATAN KEAMANAN &amp; PRIVASI</h3>
 * <ol>
 *   <li><b>Terjangkau CRUD generik v2 dan LOLOS penyaring tenant (perluasan
 *   {@code task_7b6038ac}).</b> Nama kelas ini terdaftar pada {@code nuiServiceEntities} di
 *   <b>lima</b> halaman New UI ({@code payroll/services/bayar_gaji_pegawai_service.jsp},
 *   {@code posting_transaksi_penggajian_service.jsp},
 *   {@code posting_transaksi_pembayaran_gaji_service.jsp},
 *   {@code helper/dasbor_penggajian_detail_helper_service.jsp}, dan
 *   {@code util/pembayaran_item_gaji_pegawai_tree_model_service.jsp}), sehingga bisa
 *   diauto-registrasi oleh {@code GenericCrudDefinitionRegistry.tryAutoRegister}. Adapter
 *   otomatisnya menyaring baris hanya lewat 12 nama properti tetap
 *   ({@code yayasan|sekolah|program|fakultas|jurusan|satuanKerja} tanpa syarat, ditambah
 *   {@code mahasiswa|siswa|dosen|guru|orangTua|anggotaKoperasi} yang bersyarat peran).
 *   <b>Tidak satu pun</b> dari nama itu ada pada entity ini, dan {@code pegawai} maupun
 *   {@code formatItemGaji} <b>tidak termasuk whitelist</b>. Akibatnya {@code scopeBindings()}
 *   mengembalikan peta kosong, {@code applyScope()} tidak menambahkan restriksi apa pun, dan
 *   operasi {@code list}/{@code get}/{@code export_xlsx}/{@code export_pdf} mengembalikan
 *   <b>SELURUH baris lintas seluruh tenant</b>. Data yang bocor bukan sekadar total gaji,
 *   melainkan <b>rincian per komponen berikut nama komponennya</b> untuk setiap pegawai di setiap
 *   instalasi &mdash; kategori data pribadi finansial paling sensitif di modul kepegawaian.
 *   Gerbang yang tersisa hanyalah login + hak BACA menu; keduanya tidak menyentuh <i>baris mana</i>
 *   yang boleh dilihat.</li>
 *   <li><b>Penghapusan lewat CRUD generik mungkin aktif.</b> Entity ini punya properti
 *   {@code aktif}, syarat yang dipakai adapter otomatis untuk mengizinkan <i>soft delete</i>
 *   ({@code canDelete} menolak hanya bila field {@code aktif} tidak ada). Sekali sebuah baris
 *   rincian dinonaktifkan, tree model &mdash; yang menyaring {@code eq("aktif", true)} kecuali
 *   dalam mode "tampilkan semua" &mdash; berhenti menampilkannya di slip.</li>
 *   <li><b>Dasbor Analisis Penggajian tanpa penyaring tenant.</b>
 *   {@code DasborPenggajianDetailHelper.panelKomponenGajiFormula} mengagregasi tabel ini
 *   {@code GROUP BY nama, pegawai} hanya dengan batas tahun/bulan &mdash; <b>nol</b> penyebutan
 *   {@code satuanKerja} di seluruh berkas helper maupun di {@code DasborAnalisisPenggajian}
 *   pemanggilnya. Panel merender nama pegawai berikut nominal per komponen (hingga 300 baris per
 *   kelompok) dan hasilnya disimpan pada cache L3 yang secara eksplisit "dipakai bersama semua
 *   pengguna". Ini instansi lain dari pola dasbor <i>fail-open</i> yang sudah tercatat berulang
 *   dalam inisiatif ini, kali ini pada data gaji.</li>
 *   <li><b>Penghapusan massal lewat SQL mentah melewati Envers.</b> Kelas ini beranotasi
 *   {@link Audited}, tetapi {@code reset()} dan {@code checkExistingItemGaji()} di tree model
 *   menghapus seluruh rincian slip dengan
 *   {@code delete from payroll.pembayaran_item_gaji_pegawai where pembayaran_gaji_punya_pegawai = &hellip;}.
 *   SQL mentah tidak melewati <i>interceptor</i> Hibernate, sehingga <b>tidak ada jejak audit</b>
 *   untuk perhitungan ulang yang mengubah nominal gaji &mdash; padahal jejak itulah alasan
 *   anotasi {@code @Audited} dipasang. Setelah penghapusan, baris dihitung ulang dan disimpan
 *   sebagai baris baru dengan ID baru, sehingga riwayat versi per baris pun terputus.</li>
 * </ol>
 *
 * <h3>Catatan teknis lain</h3>
 * <ul>
 *   <li><b>Deklarasi ulang field audit ({@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 *   BUKAN bug.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash; bukan
 *   {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate tidak memetakan
 *   properti induknya sama sekali. Setiap subclass yang butuh kolom audit <b>wajib</b>
 *   mendeklarasikannya ulang. Lihat {@link ais.database.model.GeneralValueObject}.</li>
 *   <li>{@link #toString()} membaca <b>field mentah</b> {@code kode} dan {@code nama}, bukan
 *   getter-nya, sehingga tidak memicu tulis-balik &mdash; tetapi juga menghasilkan
 *   {@code "null - null"} untuk baris yang snapshot-nya belum pernah diisi.</li>
 *   <li>{@code hashCode()} tidak di-override di rantai warisan ini meski {@code equals()}
 *   berbasis {@code id} di-override; jangan andalkan {@code HashSet}/{@code HashMap} berkunci
 *   entity untuk deduplikasi baris rincian (lihat peringatan identitas di
 *   {@link ais.database.model.GeneralValueObject}).</li>
 *   <li>{@code serialVersionUID} bernilai sama persis dengan {@code ItemGaji},
 *   {@code ItemGajiPegawai}, dan {@code FormatItemGaji} &mdash; hasil salin-tempel, tidak
 *   bermakna sebagai penanda versi.</li>
 *   <li>Duplikasi {@code copyByFormat} memakai {@link GeneralValueObject#clone()} yang
 *   <b>dangkal</b>; salinan berbagi seluruh referensi relasi dengan aslinya, dan pemanggilnya
 *   wajib mengosongkan {@code id} sendiri (dan memang melakukannya).</li>
 * </ul>
 *
 * @see ais.database.model.payroll.PembayaranGajiPunyaPegawai
 * @see ais.database.model.payroll.ItemGajiPegawai
 * @see ais.database.model.payroll.ItemGaji
 * @see ais.database.model.payroll.FormatItemGaji
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "pembayaran_item_gaji_pegawai")
public class PembayaranItemGajiPegawai extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi. Nilainya identik dengan beberapa entity payroll lain
	 * (hasil salin-tempel), jadi jangan diperlakukan sebagai versi skema yang bermakna.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data ({@code identity}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * ID pengguna yang terakhir mengubah baris rincian ini (kolom audit).
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/berspasi &mdash; nilai lama dipertahankan alih-alih dikosongkan. Pola ini sengaja
	 * dipakai agar {@code AuditTimestampInterceptor} tidak menghapus jejak audit yang sudah ada
	 * ketika sebuah pembaruan berjalan tanpa konteks pengguna (mis. tugas terjadwal).</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris rincian dalam format {@code "kode - nama"}.
	 *
	 * <p><b>Non-obvious:</b> method ini membaca <b>field mentah</b> {@code kode} dan {@code nama},
	 * BUKAN {@link #getKode()}/{@link #getNama()}. Efeknya ada dua: (a) aman dipanggil dari
	 * <i>debugger</i>/logging karena tidak memicu tulis-balik snapshot, tetapi (b) menghasilkan
	 * {@code "null - null"} untuk baris yang baru dibuat dan snapshot-nya belum pernah diisi
	 * oleh getter.</p>
	 *
	 * @return gabungan kode dan nama komponen gaji yang dipisahkan {@code " - "}
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan</b>
	 * sehingga jejak audit lama tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris rincian ini (kolom audit).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: mendelegasikan pencapan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} sebelum setiap UPDATE.
	 *
	 * <p>Dipanggil oleh penyedia persistensi, bukan oleh kode aplikasi. Interceptor mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari konteks pengguna aktif dan
	 * memperbarui {@link #setTanggal_dirubah(Date)}.</p>
	 *
	 * <p><b>Perhatikan:</b> karena kelas ini dipetakan <i>property access</i> dengan
	 * {@code dynamicUpdate = true}, setiap tulis-balik yang dilakukan getter (lihat Javadoc kelas)
	 * dapat memicu UPDATE &mdash; dan karenanya memicu kait ini &mdash; hanya karena baris
	 * <b>dibaca</b> dalam sesi Hibernate yang masih terbuka.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris rincian ini.
	 *
	 * <p>Diinisialisasi ke waktu server saat object dibuat (lewat {@code WaktuUtil.getDate()},
	 * yang menghormati penyetelan zona waktu instalasi) dan diperbarui oleh {@link #onUpdate()}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama komponen gaji (snapshot); lihat {@link #getNama()}. */
	private String nama;
	/** Format/struktur gaji pemilik baris ini; lihat {@link #getFormatItemGaji()}. */
	private FormatItemGaji formatItemGaji;
	/** Baris induk dalam pohon rincian slip; lihat {@link #getParent()}. */
	private PembayaranItemGajiPegawai parent;
	/** Slip gaji pemilik baris rincian ini; lihat {@link #getPembayaranGajiPunyaPegawai()}. */
	private PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai;
	/** Penugasan komponen gaji ke pegawai yang menjadi sumber baris ini; lihat {@link #getItemGajiPegawai()}. */
	private ItemGajiPegawai itemGajiPegawai;
	/** Pegawai penerima (denormalisasi); lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** NOMINAL AKTUAL komponen dalam Rupiah; lihat {@link #getNilai()}. */
	private Double nilai;
	/** Urutan tampil dalam slip (snapshot); lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Bendera aktif; lihat {@link #getAktif()}. */
	private Boolean aktif = true;
	/** Bendera tampil di slip; lihat {@link #getTampilkanDiSlip()}. */
	private Boolean tampilkanDiSlip = true;
	/** Kode komponen, dipakai sebagai variabel rumus (snapshot); lihat {@link #getKode()}. */
	private String kode;
	/** Rumus yang dipakai untuk menghitung {@link #getNilai()} (snapshot); lihat {@link #getDefaultFormula()}. */
	private String defaultFormula;
	/** Keterangan bebas komponen (snapshot); lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kedalaman baris dalam pohon &mdash; field VESTIGIAL; lihat {@link #getDeep()}. */
	private Integer deep;
	/** Pencacah pemakaian &mdash; field VESTIGIAL, nol pembaca dan penulis; lihat {@link #getJmlDipakai()}. */
	private Long jmlDipakai = 0L;
	/** Bendera baris pemisah kosong; lihat {@link #getSpace()}. */
	private Boolean space = false;

	/** Akun sisi KREDIT jurnal penggajian; lihat {@link #getAkun()}. */
	private Akun akun;
	/** Akun sisi DEBET jurnal penggajian; lihat {@link #getAkunDebet()}. */
	private Akun akunDebet;

	/** Bulan periode gaji (1-12); lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Tahun periode gaji; lihat {@link #getTahun()}. */
	private Integer tahun;

	/** Riwayat posting &mdash; field VESTIGIAL, tidak pernah ditulis maupun dibaca; lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Object yang dibuat lewat konstruktor ini praktis kosong: seluruh atribut deskriptif
	 * (nama, kode, keterangan, urutan, rumus) sengaja dibiarkan {@code null} agar getter-nya
	 * mengisi sendiri dari {@code itemGajiPegawai} saat pertama kali dibaca &mdash; lihat bagian
	 * "Snapshot" pada Javadoc kelas. Pemanggil satu-satunya di luar Hibernate adalah
	 * {@code PembayaranItemGajiPegawaiTreeModel.copyByItemGajiPegawaiRecursive}, yang mengisi
	 * relasi ({@code formatItemGaji}, {@code pembayaranGajiPunyaPegawai}, {@code parent},
	 * {@code itemGajiPegawai}, {@code pegawai}, {@code akun}, {@code akunDebet}), periode
	 * ({@code bulan}/{@code tahun}), dan {@code nilai} hasil eksekusi rumus.</p>
	 */
	public PembayaranItemGajiPegawai() {
	}

	/**
	 * Kunci utama baris rincian.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan tidak pernah disertakan dalam INSERT
	 * ({@code insertable = false}).</p>
	 *
	 * @return ID baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Dipanggil oleh Hibernate. Kode aplikasi memanggilnya hanya untuk satu tujuan:
	 * mengosongkan ID ({@code setId(null)}) pada hasil {@link GeneralValueObject#clone()} agar
	 * salinan tersimpan sebagai baris baru &mdash; lihat {@code copyByFormatRecursive}.</p>
	 *
	 * @param id ID baris, atau {@code null} untuk menandai baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama komponen gaji sebagaimana dicetak pada slip (mis. "Gaji Pokok", "Tunjangan Jabatan").
	 *
	 * <p><b>GETTER TULIS-BALIK.</b> Bila field lokal masih {@code null} dan
	 * {@link #getItemGajiPegawai()} tersedia, nama disalin dari penugasan komponen dan
	 * <b>disimpan ke field</b> &mdash; sehingga ikut ter-<i>flush</i> ke basis data pada
	 * penulisan berikutnya dalam sesi yang sama.</p>
	 *
	 * <p><b>Mengapa ini WAJIB ada:</b> kolom {@code nama} dideklarasikan
	 * {@code nullable = false}, tetapi jalur pembuatan baris
	 * ({@code copyByItemGajiPegawaiRecursive}) <b>tidak pernah memanggil {@link #setNama(String)}</b>.
	 * Karena entity dipetakan <i>property access</i>, Hibernate membaca getter ini saat menyusun
	 * INSERT &mdash; dan pada saat itulah nama terisi. Menghapus tulis-balik ini akan membuat
	 * seluruh penyimpanan rincian slip gagal dengan pelanggaran {@code NOT NULL}.</p>
	 *
	 * <p>Sifat pengisiannya <b>bersyarat</b>: begitu snapshot terisi, mengganti nama komponen di
	 * katalog TIDAK mengubah slip historis &mdash; itulah tujuan snapshot.</p>
	 *
	 * <p>Efek samping tambahan: method ini juga menyegarkan field {@code itemGajiPegawai} dengan
	 * hasil {@link #getItemGajiPegawai()} (resolusi proxy lazy).</p>
	 *
	 * @return nama komponen gaji; {@code null} hanya bila snapshot kosong DAN relasi ke penugasan
	 *         komponen juga kosong
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
	 * Menyetel nama komponen gaji secara eksplisit, membekukan snapshot.
	 *
	 * @param nama nama komponen
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas komponen gaji.
	 *
	 * <p><b>GETTER TULIS-BALIK bersyarat</b> dengan mekanisme identik {@link #getNama()}: mengisi
	 * dari {@link #getItemGajiPegawai()} hanya bila snapshot lokal masih {@code null}.</p>
	 *
	 * @return keterangan komponen, atau {@code null} bila tidak ada di snapshot maupun di master
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
	 * Menyetel keterangan komponen, membekukan snapshot.
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Format/struktur gaji yang menaungi baris rincian ini (kolom {@code format_item_gaji},
	 * {@code NOT NULL}).
	 *
	 * <p>{@link ais.database.model.payroll.FormatItemGaji} adalah satu-satunya entity di rantai
	 * ini yang membawa {@code satuanKerja} &mdash; jadi <b>seluruh sumbu tenant baris gaji
	 * bergantung pada relasi ini</b> (atau, alternatifnya, pada {@link #getPegawai()}). Nilainya
	 * disalin dari {@code itemGajiPegawai.getFormatItemGaji()} saat baris dibuat.</p>
	 *
	 * <p>Getter menormalkan proxy lazy lewat {@link GeneralValueObject#check(Object)}, sehingga
	 * pemanggil menerima object yang aman diakses meski sesi asalnya sudah ditutup.</p>
	 *
	 * @return format item gaji; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji", nullable = false)
	public FormatItemGaji getFormatItemGaji() {
		formatItemGaji = check(formatItemGaji);
		return formatItemGaji;
	}

	/**
	 * Menyetel format/struktur gaji pemilik baris.
	 *
	 * @param formatItemGaji format item gaji
	 */
	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}

	/**
	 * Baris rincian INDUK dalam pohon slip (kolom {@code bagian_dari}, nullable) &mdash; relasi
	 * ke tipe yang sama dengan kelas ini.
	 *
	 * <p>{@code null} berarti baris berada di tingkat teratas slip. Struktur pohon ini dipakai
	 * {@code PembayaranItemGajiPegawaiTreeModel} untuk merender indentasi (enam spasi per tingkat,
	 * dihitung dengan menelusuri rantai {@code getParent()} ke atas) dan untuk penghapusan/
	 * penyalinan rekursif.</p>
	 *
	 * <p><b>Kasus tepi:</b> baris induk menyimpan nominalnya SENDIRI, sehingga penjumlahan naif
	 * seluruh baris satu slip akan menghitung ganda subtotalnya. Tidak ada penjaga siklus pada
	 * relasi ini; rantai {@code parent} yang melingkar akan membuat penelusuran rekursif
	 * berputar tanpa henti (tidak pernah terjadi pada data yang dibuat mesin, karena induk selalu
	 * disimpan sebelum anaknya).</p>
	 *
	 * @return baris induk, atau {@code null} bila baris ini berada di tingkat teratas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian_dari", nullable = true)
	public PembayaranItemGajiPegawai getParent() {
		parent = check(parent);
		return parent;
	}

	/**
	 * Menyetel baris induk dalam pohon rincian slip.
	 *
	 * @param parent baris induk, atau {@code null} untuk menempatkan baris di tingkat teratas
	 */
	public void setParent(PembayaranItemGajiPegawai parent) {
		this.parent = parent;
	}

	/**
	 * Urutan tampil komponen dalam slip (kolom {@code urutan}).
	 *
	 * <p><b>GETTER TULIS-BALIK bersyarat</b>: mengisi dari {@link #getItemGajiPegawai()} bila
	 * snapshot masih {@code null}.</p>
	 *
	 * <p><b>Kasus tepi:</b> nilai kembalian dinormalkan menjadi {@code 0} bila tetap
	 * {@code null} &mdash; TETAPI normalisasi itu hanya berlaku pada nilai kembalian, field-nya
	 * sendiri dibiarkan {@code null}. Jadi kolom basis data bisa NULL sementara pemanggil selalu
	 * menerima angka. Pengurutan baris slip sesungguhnya dilakukan di sisi query
	 * ({@code Order.asc("nomorUrut")} pada {@code ItemGajiPegawai} saat rincian dibangkitkan),
	 * bukan dari kolom ini.</p>
	 *
	 * @return urutan tampil; {@code 0} bila tidak tersedia di snapshot maupun master
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
	 * Menyetel urutan tampil komponen dalam slip.
	 *
	 * @param nomorUrut urutan tampil
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Bendera aktif baris rincian.
	 *
	 * <p><b>GETTER TULIS-BALIK bersyarat</b>: mengisi dari {@link #getItemGajiPegawai()} bila
	 * snapshot masih {@code null}. Field diinisialisasi {@code true}, jadi tulis-balik hanya
	 * terjadi pada baris yang secara eksplisit pernah dikosongkan.</p>
	 *
	 * <p><b>Dampak:</b> tree model menyaring {@code eq("aktif", true)} kecuali dalam mode
	 * "tampilkan semua", sehingga baris yang dinonaktifkan hilang dari slip. Keberadaan properti
	 * ini juga yang membuat adapter CRUD generik otomatis mengizinkan <i>soft delete</i> atas
	 * entity ini &mdash; lihat peringatan keamanan pada Javadoc kelas.</p>
	 *
	 * @return {@code true} bila baris aktif; dapat {@code null} bila snapshot dan master
	 *         sama-sama kosong
	 */
	public Boolean getAktif() {
		itemGajiPegawai = getItemGajiPegawai();
		if (aktif == null && itemGajiPegawai != null) {
			aktif = itemGajiPegawai.getAktif();
		}
		return aktif;
	}

	/**
	 * Menyetel bendera aktif baris rincian.
	 *
	 * @param aktif {@code true} bila baris ikut ditampilkan/diproses
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Kode komponen gaji &mdash; sekaligus <b>nama variabel</b> yang dirujuk rumus komponen lain.
	 *
	 * <p><b>GETTER TULIS-BALIK bersyarat</b> dari {@link #getItemGajiPegawai()}.</p>
	 *
	 * <p><b>Peran non-obvious:</b> kode inilah yang dipakai mesin rumus sebagai pengenal variabel
	 * ({@code hitungItemGajiPegawai(kode, formula, &hellip;)}) dan sebagai kunci peta hasil pada
	 * pembangunan slip ({@code maps.put(kode + "_nama", &hellip;)},
	 * {@code maps.put(kode + "_nilai", &hellip;)}). Karena kunci peta itu tidak dijamin unik,
	 * dua komponen berkode sama dalam satu slip akan saling menimpa dalam data cetak &mdash;
	 * tidak ada penjaga keunikan kode di lapisan mana pun.</p>
	 *
	 * <p>Perhatikan bahwa {@code copyByItemGajiPegawaiRecursive} memanggil {@code getKode()}
	 * pada object yang <b>belum disimpan</b>, sehingga tulis-balik kode terjadi lebih dulu
	 * daripada INSERT-nya.</p>
	 *
	 * @return kode komponen, atau {@code null} bila tidak tersedia di snapshot maupun master
	 */
	public String getKode() {
		itemGajiPegawai = getItemGajiPegawai();
		if (kode == null && itemGajiPegawai != null) {
			kode = itemGajiPegawai.getKode();
		}
		return kode;
	}

	/**
	 * Menyetel kode komponen gaji.
	 *
	 * @param kode kode/nama variabel komponen
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Rumus yang dipakai untuk menghitung {@link #getNilai()} pada periode ini.
	 *
	 * <p><b>GETTER TULIS-BALIK bersyarat</b> dari {@link #getItemGajiPegawai()}.</p>
	 *
	 * <p>Menyimpan rumus per baris (bukan sekadar merujuk katalog) membuat slip historis tetap
	 * dapat dijelaskan meski rumus di katalog kemudian berubah. Layar "Bayar Gaji Pegawai"
	 * memanfaatkan ini terbalik: ia mengambil rumus dari baris rincian periode yang dipilih
	 * sebagai nilai awal editor, dan hanya jatuh ke rumus katalog bila periode itu belum punya
	 * rincian sama sekali.</p>
	 *
	 * <p>Nilai dapat ditimpa saat perhitungan ulang lewat peta {@code formulasBaru} yang
	 * diteruskan {@code reset()}/{@code copyByItemGajiPegawai()}, berkunci ID
	 * {@link ais.database.model.payroll.ItemGaji}.</p>
	 *
	 * @return rumus komponen, atau {@code null} bila tidak tersedia di snapshot maupun master
	 */
	public String getDefaultFormula() {
		itemGajiPegawai = getItemGajiPegawai();
		if (defaultFormula == null && itemGajiPegawai != null) {
			defaultFormula = itemGajiPegawai.getDefaultFormula();
		}
		return defaultFormula;
	}

	/**
	 * Menyetel rumus komponen untuk periode ini.
	 *
	 * @param defaultFormula ekspresi rumus
	 */
	public void setDefaultFormula(String defaultFormula) {
		this.defaultFormula = defaultFormula;
	}

	/**
	 * Kedalaman baris dalam pohon rincian &mdash; <b>field VESTIGIAL</b>.
	 *
	 * <p>Ditulis oleh {@code PembayaranItemGajiPegawaiTreeModel.getParentCountRecursive}
	 * (lewat {@link #setDeep(Integer)} + {@code Common.refreshUpdate}), tetapi <b>tidak pernah
	 * dibaca</b> untuk entity ini di seluruh repo &mdash; verifikasi menyeluruh menemukan pembaca
	 * {@code getDeep()} hanya pada {@code ItemGajiPegawai} dan {@code ItemGaji}. Indentasi slip
	 * dihitung ulang setiap kali dengan menelusuri rantai {@link #getParent()}.</p>
	 *
	 * <p>Getter murni tanpa efek samping.</p>
	 *
	 * @return kedalaman baris, atau {@code null} bila belum pernah dihitung
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menyetel kedalaman baris dalam pohon rincian.
	 *
	 * @param deep kedalaman (0 untuk tingkat teratas)
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Pencacah pemakaian &mdash; <b>field VESTIGIAL</b>.
	 *
	 * <p>Nol pembaca dan nol penulis di seluruh repo di luar kelas ini; sisa salin-tempel dari
	 * katalog {@link ais.database.model.payroll.ItemGaji}, tempat pencacah serupa memang
	 * bermakna. Nilainya selalu {@code 0} pada baris yang dibuat mesin.</p>
	 *
	 * @return selalu nilai yang tersimpan, secara praktik {@code 0}
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menyetel pencacah pemakaian (vestigial).
	 *
	 * @param jmlDipakai jumlah pemakaian
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Penugasan komponen gaji ke pegawai yang menjadi <b>sumber</b> baris rincian ini.
	 *
	 * <p><b>PERHATIKAN NAMA KOLOMNYA:</b> kolom bernama {@code item_gaji} tetapi tipenya
	 * {@link ais.database.model.payroll.ItemGajiPegawai}, BUKAN
	 * {@link ais.database.model.payroll.ItemGaji}. Entity ini tidak punya FK apa pun ke katalog
	 * {@code ItemGaji}; katalog dijangkau dua lompatan lewat
	 * {@code getItemGajiPegawai().getItemGaji()}. Penamaan ini rutin menyesatkan pembaca
	 * SQL/skema mentah.</p>
	 *
	 * <p>Relasi ini adalah <b>poros seluruh mekanisme snapshot</b>: hampir semua getter lain di
	 * kelas ini memanggilnya untuk mengisi field-nya sendiri. Kolomnya nullable &mdash; baris
	 * yatim (tanpa penugasan) tetap mungkin ada, dan pada baris seperti itu semua tulis-balik
	 * menjadi no-op sehingga nama/kode/rumus tetap sebagaimana tersimpan.</p>
	 *
	 * <p>Getter menormalkan proxy lazy lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return penugasan komponen gaji ke pegawai, atau {@code null} bila baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_gaji", nullable = true)
	public ItemGajiPegawai getItemGajiPegawai() {
		itemGajiPegawai = check(itemGajiPegawai);
		return itemGajiPegawai;
	}

	/**
	 * Menyetel penugasan komponen gaji sumber baris ini.
	 *
	 * @param itemGajiPegawai penugasan komponen ke pegawai
	 */
	public void setItemGajiPegawai(ItemGajiPegawai itemGajiPegawai) {
		this.itemGajiPegawai = itemGajiPegawai;
	}

	/**
	 * Apakah komponen ini dicetak pada slip gaji.
	 *
	 * <p><b>GETTER TULIS-BALIK TANPA SYARAT &mdash; MERUSAK SNAPSHOT.</b> Berbeda dari
	 * {@link #getNama()} dan kerabatnya, method ini <b>tidak memeriksa apakah field lokal sudah
	 * terisi</b>: setiap kali dibaca, nilainya <b>ditimpa</b> dari
	 * {@code itemGajiPegawai.getTampilkanDiSlip()}. Akibatnya keputusan tampil/sembunyi yang
	 * berlaku pada slip historis <b>hilang permanen</b> begitu master diubah dan slip lama dibuka
	 * kembali dalam sesi Hibernate yang masih terbuka &mdash; dengan {@code dynamicUpdate}, nilai
	 * timpaan itu ikut ter-<i>flush</i> ke basis data.</p>
	 *
	 * <p>Konsumen: {@code PembayaranItemGajiPegawaiTreeModel.populateDataRecursive} melewati
	 * seluruh baris yang mengembalikan {@code false} saat menyusun data cetak slip.</p>
	 *
	 * <p><b>Kasus tepi:</b> bila master mengembalikan {@code null}, nilai {@code null} itu pun
	 * ditimpakan &mdash; pemanggil yang melakukan <i>auto-unboxing</i> (seperti
	 * {@code if (gajiPegawai.getTampilkanDiSlip())} di tree model) akan melempar
	 * {@code NullPointerException}.</p>
	 *
	 * @return {@code true} bila komponen dicetak pada slip; dapat {@code null} bila master
	 *         menyimpan {@code null}
	 */
	public Boolean getTampilkanDiSlip() {
		itemGajiPegawai = getItemGajiPegawai();
		if (itemGajiPegawai != null) {
			tampilkanDiSlip = itemGajiPegawai.getTampilkanDiSlip();
		}
		return tampilkanDiSlip;
	}

	/**
	 * Menyetel bendera tampil di slip.
	 *
	 * <p><b>Peringatan:</b> nilai yang disetel di sini tidak bertahan &mdash; pembacaan berikutnya
	 * lewat {@link #getTampilkanDiSlip()} akan menimpanya kembali dari master selama
	 * {@link #getItemGajiPegawai()} tidak {@code null}.</p>
	 *
	 * @param tampilkanDiSlip {@code true} bila komponen dicetak pada slip
	 */
	public void setTampilkanDiSlip(Boolean tampilkanDiSlip) {
		this.tampilkanDiSlip = tampilkanDiSlip;
	}

	/**
	 * Slip gaji ({@link ais.database.model.payroll.PembayaranGajiPunyaPegawai}) pemilik baris
	 * rincian ini &mdash; relasi <b>induk</b> entity ini.
	 *
	 * <p>TERVERIFIKASI sebagai satu-satunya kunci pengelompokan yang dipakai seluruh konsumen:
	 * pembangunan slip, perhitungan ulang, keempat mesin posting jurnal, dan agregasi dasbor
	 * semuanya menyaring {@code Restrictions.eq("pembayaranGajiPunyaPegawai", &hellip;)}. Slip
	 * itu sendiri menunjuk {@code PembayaranGaji} (batch penggajian per satuan kerja per periode)
	 * dan {@link ais.database.model.Pegawai}.</p>
	 *
	 * <p><b>Berbeda dari relasi lain di kelas ini</b>, getter ini TIDAK memanggil
	 * {@link GeneralValueObject#check(Object)} &mdash; proxy lazy dikembalikan apa adanya. Sebagai
	 * gantinya relasi ini memakai {@code @Fetch(FetchMode.SELECT)} tanpa
	 * {@code fetch = FetchType.LAZY} eksplisit, sehingga bergantung pada default
	 * {@code @ManyToOne} (EAGER) yang diambil lewat SELECT terpisah, bukan JOIN. Mengakses
	 * properti slip dari object yang sesinya sudah tertutup karenanya lebih rawan
	 * {@code LazyInitializationException} dibanding relasi lain di kelas ini.</p>
	 *
	 * <p>Kolomnya nullable: baris rincian yatim (tanpa slip) secara skema mungkin ada dan akan
	 * luput dari semua query yang menyaring per slip.</p>
	 *
	 * @return slip gaji pemilik baris, atau {@code null} bila baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_gaji_punya_pegawai", nullable = true)
	public PembayaranGajiPunyaPegawai getPembayaranGajiPunyaPegawai() {
		return pembayaranGajiPunyaPegawai;
	}

	/**
	 * Menyetel slip gaji pemilik baris rincian.
	 *
	 * <p>Dipanggil saat baris dibangkitkan ({@code copyByItemGajiPegawaiRecursive}) dan saat
	 * rincian sebuah slip disalin ke slip lain ({@code copyByFormatRecursive}, yang mengarahkan
	 * salinan ke slip {@code target}).</p>
	 *
	 * @param pembayaranGajiPunyaPegawai slip gaji pemilik baris
	 */
	public void setPembayaranGajiPunyaPegawai(PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai) {
		this.pembayaranGajiPunyaPegawai = pembayaranGajiPunyaPegawai;
	}

	/**
	 * <b>NOMINAL AKTUAL</b> komponen gaji ini dalam Rupiah untuk periode berjalan &mdash; angka
	 * inti seluruh entity.
	 *
	 * <p>Diisi saat baris dibangkitkan dengan hasil eksekusi rumus
	 * ({@code ItemGajiPegawaiTreeModel.hitungItemGajiPegawai}) dan sesudah itu diperlakukan
	 * sebagai angka beku: dasbor, slip cetak, dan mesin posting semuanya membaca kolom tersimpan,
	 * tidak menghitung ulang.</p>
	 *
	 * <p><b>GETTER DESTRUKTIF ATAS NOMINAL &mdash; hal paling penting pada method ini.</b> Ada
	 * dua mutasi yang terjadi hanya karena baris <b>dibaca</b>:</p>
	 * <ol>
	 *   <li>{@code null} dinormalkan menjadi {@code 0.0} dan <b>disimpan ke field</b>;</li>
	 *   <li>bila katalog di hulu menyalakan {@code jadikan0JikaMinus}
	 *       ({@code itemGajiPegawai.getItemGaji().getJadikan0JikaMinus()}) dan nilainya negatif,
	 *       nilai <b>ditimpa menjadi {@code 0.0}</b> dan disimpan ke field.</li>
	 * </ol>
	 * <p>Karena entity dipetakan <i>property access</i> dengan {@code dynamicUpdate = true},
	 * Hibernate membaca getter ini saat pemeriksaan kotor; nilai hasil pembulatan itu
	 * ter-<i>flush</i> sebagai UPDATE nyata. <b>Angka aslinya tidak dapat dipulihkan dari baris
	 * ini</b> &mdash; dan karena perhitungan ulang menghapus baris dengan SQL mentah (di luar
	 * Envers), jejak audit pun tidak menolong. Konsekuensi praktisnya: menyalakan
	 * {@code jadikan0JikaMinus} di katalog akan menormalkan nominal <b>slip historis</b> menjadi
	 * nol satu per satu, seiring slip-slip itu dibuka kembali.</p>
	 *
	 * <p><b>Interaksi dengan mesin posting:</b> seluruh mesin posting menyaring
	 * {@code Restrictions.gt("nilai", 0.1)} pada <b>kolom tersimpan</b>, sehingga (a) komponen
	 * bernilai negatif tidak pernah masuk jurnal sama sekali, dan (b) pembulatan di getter terjadi
	 * <i>setelah</i> penyaringan, bukan sebelumnya. Nilai yang sama dipakai untuk kaki debet
	 * maupun kaki kredit baris tersebut (lihat {@link #getAkun()}/{@link #getAkunDebet()}).</p>
	 *
	 * <p><b>Kasus tepi:</b> pembulatan dilewati diam-diam bila {@link #getItemGajiPegawai()} atau
	 * {@code itemGajiPegawai.getItemGaji()} bernilai {@code null} &mdash; baris yatim
	 * mempertahankan nilai negatifnya.</p>
	 *
	 * @return nominal komponen dalam Rupiah; tidak pernah {@code null} setelah method ini
	 *         dipanggil
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
	 * Menyetel nominal komponen gaji untuk periode ini.
	 *
	 * <p>Dipanggil oleh {@code copyByItemGajiPegawaiRecursive} dengan hasil eksekusi rumus
	 * (yang sudah dibulatkan lebih dulu bila {@code jadikan0JikaMinus} menyala), dan oleh
	 * Hibernate saat memuat baris dari basis data.</p>
	 *
	 * <p>Setter ini murni: pembulatan negatif-menjadi-nol TIDAK dilakukan di sini, melainkan di
	 * {@link #getNilai()}.</p>
	 *
	 * @param nilai nominal komponen dalam Rupiah; boleh {@code null} (akan dinormalkan menjadi
	 *              {@code 0.0} pada pembacaan pertama)
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Akun sisi <b>KREDIT</b> jurnal penggajian untuk komponen ini (kolom {@code akun}).
	 *
	 * <p>Keempat mesin posting ({@code PostingTransaksiPenggajianAction} pada tombol layar,
	 * tombol massal, dan jalur API {@code postingSemua}; serta
	 * {@code PostingTransaksiPembayaranGajiAction}) menambahkan akun ini ke daftar kredit dengan
	 * nilai {@link #getNilai()} untuk setiap baris yang lolos saring.</p>
	 *
	 * <p><b>GETTER TULIS-BALIK bersyarat dengan <i>fallback</i> LIVE-READ ke katalog.</b> Bila
	 * kolom {@code akun} kosong, akun diambil dari
	 * {@code itemGajiPegawai.getItemGaji().getAkun()} &mdash; yaitu <b>keadaan katalog SAAT INI</b>,
	 * bukan keadaan saat slip dibuat &mdash; lalu disimpan ke field. Dua konsekuensi:</p>
	 * <ul>
	 *   <li><b>Pemindahan akun retroaktif.</b> Mengganti akun di
	 *       {@link ais.database.model.payroll.ItemGaji} mengubah akun jurnal slip lama yang kolom
	 *       akunnya masih kosong, dan tulis-balik membekukan pilihan baru itu secara permanen.
	 *       Ini pola yang sama dengan koreksi yang pernah dicatat untuk rantai akun biaya
	 *       reimbursement, hanya arahnya terbalik: di sini <i>fallback</i>-nya memang live.</li>
	 *   <li><b>Kaki jurnal yang tumbuh setelah penyaringan.</b> Penyaring posting berbunyi
	 *       {@code akun IS NOT NULL OR akun_debet IS NOT NULL} pada kolom tersimpan. Baris yang
	 *       lolos hanya karena {@code akun_debet} terisi akan tetap dibaca {@code getAkun()}-nya
	 *       oleh mesin posting &mdash; dan <i>fallback</i> dapat memunculkan kaki kredit yang
	 *       tidak tersirat pada data tersimpan, sehingga jurnal yang terbentuk berbeda dari
	 *       yang terlihat di tabel.</li>
	 * </ul>
	 *
	 * <p>Nilai awal disalin dari katalog saat baris dibuat
	 * ({@code newGaji.setAkun(itemGajiPegawai.getItemGaji().getAkun())}), jadi jalur
	 * <i>fallback</i> hanya aktif bila katalog waktu itu belum punya akun.</p>
	 *
	 * @return akun kredit komponen, atau {@code null} bila tidak tersedia di snapshot maupun
	 *         katalog
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
	 * Menyetel akun sisi kredit jurnal untuk komponen ini.
	 *
	 * @param akun akun kredit, atau {@code null} untuk mengaktifkan kembali <i>fallback</i>
	 *             katalog pada pembacaan berikutnya
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Akun sisi <b>DEBET</b> jurnal penggajian untuk komponen ini (kolom {@code akun_debet}).
	 *
	 * <p>Mekanismenya identik dengan {@link #getAkun()}: tulis-balik bersyarat dengan
	 * <i>fallback</i> live-read ke {@code itemGajiPegawai.getItemGaji().getAkunDebet()}. Seluruh
	 * peringatan pada {@link #getAkun()} berlaku sama di sini &mdash; termasuk kemungkinan
	 * munculnya kaki debet yang tidak tersirat pada data tersimpan untuk baris yang lolos saring
	 * hanya karena kolom {@code akun}-nya terisi.</p>
	 *
	 * <p>Mesin posting menutup jurnal dengan mengkredit akun bank pegawai (bila ada) atau akun
	 * cara pembayaran gaji sebesar nilai neto slip, lalu &mdash; pada jalur massal &mdash;
	 * membatalkan posting bila total debet dan kredit tidak seimbang (pembandingan memakai
	 * {@code intValue()}, sehingga selisih pecahan di bawah satu Rupiah diabaikan).</p>
	 *
	 * @return akun debet komponen, atau {@code null} bila tidak tersedia di snapshot maupun
	 *         katalog
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
	 * Menyetel akun sisi debet jurnal untuk komponen ini.
	 *
	 * @param akunDebet akun debet, atau {@code null} untuk mengaktifkan kembali <i>fallback</i>
	 *                  katalog pada pembacaan berikutnya
	 */
	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

	/**
	 * Pegawai penerima komponen gaji ini (kolom {@code pegawai}, nullable).
	 *
	 * <p><b>DENORMALISASI yang disengaja:</b> pegawai sebenarnya sudah dapat dijangkau lewat
	 * {@code getPembayaranGajiPunyaPegawai().getPegawai()}. Salinan langsung di sini ada agar
	 * query agregat dapat mengelompokkan per pegawai tanpa JOIN tambahan &mdash; dipakai persis
	 * demikian oleh {@code DasborPenggajianDetailHelper.panelKomponenGajiFormula}
	 * ({@code groupProperty("x.pegawai")}) dan sebagai penyaring langsung oleh layar "Bayar Gaji
	 * Pegawai".</p>
	 *
	 * <p><b>Tidak ada penjaga konsistensi</b> antara kolom ini dan pegawai pada slip induknya;
	 * keduanya diisi dari sumber yang sama saat baris dibuat dan sesudah itu tidak pernah
	 * disinkronkan ulang.</p>
	 *
	 * <p><b>Catatan keamanan:</b> {@code pegawai} bukan salah satu dari 12 nama properti yang
	 * dikenali penyaring tenant CRUD generik v2, sehingga keberadaan relasi ini <b>tidak</b>
	 * menolong pembatasan lintas tenant &mdash; lihat peringatan pada Javadoc kelas.</p>
	 *
	 * <p>Getter menormalkan proxy lazy lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return pegawai penerima, atau {@code null} bila kolom tidak terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai penerima komponen gaji ini.
	 *
	 * @param pegawai pegawai penerima
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Apakah baris ini merupakan <b>pemisah kosong</b> (spacer) pada slip, bukan komponen bernilai.
	 *
	 * <p><b>GETTER TULIS-BALIK TANPA SYARAT &mdash; MERUSAK SNAPSHOT</b>, persis seperti
	 * {@link #getTampilkanDiSlip()}: nilainya selalu ditimpa dari {@code itemGajiPegawai.getSpace()}
	 * pada setiap pembacaan, sehingga sifat "pemisah" pada slip historis mengikuti keadaan master
	 * saat ini, bukan saat slip dibuat.</p>
	 *
	 * <p>Konsumen: {@code populateDataRecursive} melewati penelusuran anak, indentasi, dan
	 * pengambilan nominal untuk baris pemisah &mdash; nominalnya sengaja dikirim sebagai
	 * {@code null} ke data cetak agar kolom nilai tampil kosong.</p>
	 *
	 * <p><b>Kasus tepi:</b> sama seperti {@link #getTampilkanDiSlip()}, {@code null} dari master
	 * pun ditimpakan dan dapat memicu {@code NullPointerException} pada pemanggil yang melakukan
	 * <i>auto-unboxing</i>.</p>
	 *
	 * @return {@code true} bila baris adalah pemisah kosong; dapat {@code null} bila master
	 *         menyimpan {@code null}
	 */
	public Boolean getSpace() {
		itemGajiPegawai = getItemGajiPegawai();
		if (itemGajiPegawai != null) {
			space = itemGajiPegawai.getSpace();
		}
		return space;
	}

	/**
	 * Menyetel bendera baris pemisah kosong.
	 *
	 * <p><b>Peringatan:</b> nilai yang disetel di sini tidak bertahan &mdash; pembacaan berikutnya
	 * lewat {@link #getSpace()} akan menimpanya kembali dari master selama
	 * {@link #getItemGajiPegawai()} tidak {@code null}.</p>
	 *
	 * @param space {@code true} bila baris hanya pemisah tampilan
	 */
	public void setSpace(Boolean space) {
		this.space = space;
	}

	/**
	 * Riwayat posting jurnal untuk baris ini &mdash; <b>field VESTIGIAL</b>.
	 *
	 * <p>Verifikasi menyeluruh atas seluruh repo menemukan <b>nol penulis dan nol pembaca</b> di
	 * luar pasangan getter/setter ini: kolom {@code posting_history} pada tabel ini selalu NULL.
	 * Stempel posting yang sesungguhnya dipasang pada slip
	 * ({@code PembayaranGajiPunyaPegawai.setPostingHistory(...)}) atau pada batch
	 * {@code PembayaranGaji}, dan pembatalan posting pun dikerjakan di tingkat slip (menghapus
	 * {@code akunting.grup_transaksi} yang menunjuk slip tersebut). Pola dan rancangan yang sama
	 * &mdash; termasuk kolom yang tidak pernah terpakai &mdash; ada juga pada
	 * {@code RencanaItemGajiPegawai}.</p>
	 *
	 * <p>Karena granularitas stempel berada di tingkat slip, tidak ada cara membedakan komponen
	 * mana dalam satu slip yang sudah terjurnal bila posting parsial pernah terjadi. Getter murni
	 * tanpa efek samping dan tanpa {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return selalu {@code null} pada data nyata
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel riwayat posting baris (vestigial &mdash; tidak pernah dipanggil).
	 *
	 * @param postingHistory riwayat posting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Bulan periode gaji baris ini (1-12).
	 *
	 * <p>Disalin ke baris saat rincian dibangkitkan
	 * ({@code copyByItemGajiPegawaiRecursive} memanggil {@link #setBulan(Integer)} dengan bulan
	 * yang dipilih operator). Ini <b>denormalisasi</b> terhadap
	 * {@code getPembayaranGajiPunyaPegawai().getPembayaranGaji().getBulan()}.</p>
	 *
	 * <p><b>Non-obvious:</b> meskipun tersedia, kolom ini <b>tidak dipakai sebagai penyaring
	 * periode</b> oleh konsumen mana pun &mdash; dasbor dan layar pembayaran semuanya menyaring
	 * lewat JOIN ke {@code pembayaranGaji.bulan}/{@code pembayaranGaji.tahun}. Tidak ada penjaga
	 * yang memastikan kolom ini konsisten dengan periode batch induknya; keduanya bisa berbeda
	 * tanpa terdeteksi.</p>
	 *
	 * <p>Getter murni tanpa efek samping.</p>
	 *
	 * @return bulan periode (1-12), atau {@code null} bila tidak diisi
	 */
	public Integer getBulan() {
		return bulan;
	}

	/**
	 * Menyetel bulan periode gaji baris ini.
	 *
	 * @param bulan bulan periode (1-12)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Tahun periode gaji baris ini.
	 *
	 * <p>Pasangan {@link #getBulan()}; seluruh catatan pada method tersebut &mdash; denormalisasi,
	 * tidak dipakai sebagai penyaring, tanpa penjaga konsistensi &mdash; berlaku sama di sini.</p>
	 *
	 * <p>Getter murni tanpa efek samping.</p>
	 *
	 * @return tahun periode, atau {@code null} bila tidak diisi
	 */
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Menyetel tahun periode gaji baris ini.
	 *
	 * @param tahun tahun periode
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

}
