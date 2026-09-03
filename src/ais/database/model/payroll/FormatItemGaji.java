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
import ais.database.model.rab.SatuanKerja;

/**
 * <b>Format/template susunan slip gaji</b> &mdash; tabel {@code payroll.format_item_gaji}. Satu
 * baris kelas ini adalah satu "kop slip": sebuah nama skema penggajian (mis. "Gaji Guru Tetap",
 * "Gaji Staf", "Tunjangan Sertifikasi") yang menjadi <b>wadah</b> bagi seluruh komponen gaji di
 * bawahnya. Entity ini sendiri tidak menyimpan satu pun nominal, rumus, atau nomor akun &mdash;
 * ia murni identitas + pengelompokan + <b>penanda kepemilikan tenant</b>.
 *
 * <h2>Kenapa kelas sekecil ini penting</h2>
 *
 * <p>Kelas ini hanya punya tujuh kolom bisnis, tetapi ia adalah <b>satu-satunya pembawa kolom
 * tenant ({@code satuan_kerja}) di sepanjang rantai penggajian</b>. Seluruh entity komponen dan
 * transaksi gaji di paket ini tidak punya kolom {@code satuanKerja}, {@code sekolah}, maupun
 * {@code yayasan} sendiri; satu-satunya cara menjawab pertanyaan "slip ini milik unit kerja mana?"
 * adalah menelusuri FK {@code format_item_gaji} sampai ke {@link #getSatuanKerja()} di sini.
 * Terverifikasi dari kode kelas-kelas berikut (nama kolom dan {@code nullable} dibaca langsung
 * dari anotasi {@code @JoinColumn} masing-masing):</p>
 *
 * <ul>
 *   <li>{@link ItemGaji} &mdash; katalog komponen gaji per format (kolom
 *       {@code format_item_gaji}, <b>NOT NULL</b>). Kelas itu sama sekali tidak punya kolom tenant;
 *       Javadoc-nya sudah menunjuk balik ke sini sebagai satu-satunya jalur ke satuan kerja.</li>
 *   <li>{@link ItemGajiPegawai} &mdash; komponen gaji yang benar-benar melekat pada seorang
 *       pegawai (kolom {@code format_item_gaji}, <b>NOT NULL</b>).</li>
 *   <li>{@link PembayaranItemGajiPegawai} &mdash; <b>baris rincian slip gaji historis</b> (kolom
 *       {@code format_item_gaji}, <b>NOT NULL</b>). Inilah data paling sensitif yang cakupan
 *       tenant-nya bergantung pada baris kelas ini.</li>
 *   <li>{@link RencanaItemGajiPegawai} &mdash; kembaran "rencana" dari baris di atas (kolom
 *       {@code format_item_gaji}, <b>NOT NULL</b>).</li>
 *   <li>{@link PembayaranGajiPunyaPegawai} &mdash; kepala slip per pegawai per periode (kolom
 *       {@code format_item_gaji}, <b>nullable</b>; getter-nya <b>menulis balik</b> nilai warisan
 *       dari {@code pegawai.getFormatItemGaji()} bila kosong).</li>
 *   <li>{@link StandarGaji} &mdash; tabel standar/patokan gaji (kolom {@code format_item_gaji},
 *       nullable).</li>
 *   <li>{@link ais.database.model.Pegawai} &mdash; menyimpan <b>lima slot</b> format sekaligus
 *       ({@code format_item_gaji}, {@code format_item_gaji2}, {@code format_item_gaji3},
 *       {@code format_item_gaji_4}, {@code format_item_gaji_5}; perhatikan ketidakkonsistenan
 *       garis bawah pada slot 4 dan 5). Urutan slot itu dipakai
 *       {@code Pegawai.ambilBank(FormatItemGaji)} dan {@code Pegawai.ambilNoRek(FormatItemGaji)}
 *       untuk memilih rekening tujuan transfer &mdash; jadi <b>format juga menentukan ke rekening
 *       mana uang dikirim</b>, bukan sekadar tata letak kertas.</li>
 * </ul>
 *
 * <p><b>Ringkasan untuk pembaca masa depan:</b> perlakukan setiap perubahan pada baris kelas ini
 * &mdash; khususnya {@link #setSatuanKerja(SatuanKerja)} dan {@link #setAktif(Boolean)} &mdash;
 * sebagai perubahan lingkup data, bukan perubahan master biasa. Mengubah {@code satuanKerja} pada
 * satu format akan <b>seketika memindahkan cakupan tenant seluruh komponen dan seluruh baris slip
 * gaji historis</b> yang menunjuk format itu, karena tidak satu pun dari mereka menyimpan
 * tenant-nya sendiri. Tidak ada satu pun penjaga di kode yang mencegahnya (lihat bagian keamanan
 * di bawah).</p>
 *
 * <h2>Apa yang benar-benar disimpan</h2>
 *
 * <ul>
 *   <li>{@link #getNama()} &mdash; kolom {@code nama}, NOT NULL, panjang 255. Sekaligus label
 *       yang dikembalikan {@link #toString()} dan dipakai bandbox pemilih untuk mencocokkan
 *       pilihan pengguna.</li>
 *   <li>{@link #getKeterangan()} &mdash; kolom {@code keterangan}, teks bebas.</li>
 *   <li>{@link #getCabang()}, {@link #getDepartemen()}, {@link #getLevelJabatan()} &mdash; tiga
 *       relasi <b>opsional</b> ke master organisasi ({@link Cabang}, {@link Departemen},
 *       {@link LevelJabatan}) yang dipakai murni sebagai <b>penyaring pencarian</b> dan label
 *       tampilan. Terverifikasi: tidak ada satu pun kode yang memakai ketiganya untuk memutuskan
 *       format mana yang berlaku bagi seorang pegawai &mdash; pemasangan format ke pegawai
 *       dilakukan manual lewat kelima slot di {@link ais.database.model.Pegawai}. Jadi ketiga
 *       kolom ini <b>deskriptif, bukan aturan</b>.</li>
 *   <li>{@link #getSatuanKerja()} &mdash; kolom {@code satuan_kerja}, <b>nullable</b>. Lihat
 *       seluruh pembahasan tenant di halaman ini.</li>
 *   <li>{@link #getAktif()} &mdash; bendera aktif dengan default implisit {@code true}.</li>
 *   <li>{@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()} &mdash; jejak
 *       audit ringan, diisi {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
 *       {@code onUpdate()} ({@code @PreUpdate}).</li>
 * </ul>
 *
 * <p>Kelas ditandai {@code @Audited} (Hibernate Envers), jadi setiap perubahan &mdash; termasuk
 * perpindahan {@code satuan_kerja} &mdash; tercatat di {@code format_item_gaji_aud}. Riwayat itu
 * adalah satu-satunya cara merekonstruksi "slip periode X dulu milik unit kerja mana", karena
 * tabel slip sendiri tidak menyimpannya.</p>
 *
 * <h2>Siapa yang memakai, dan bagaimana</h2>
 *
 * <ul>
 *   <li><b>Layar master ZK</b> {@code ais.action.master.payroll.FormatItemGajiAction}
 *       ({@code z/x/y/pages/master/payroll/format_item_gaji.zul}) &mdash; daftar + dialog
 *       tambah/ubah + tombol <b>Copy Data</b> + tombol hapus + checkbox Aktif inline.</li>
 *   <li><b>Bandbox pemilih</b> {@code ais.action.master.payroll.helper.AmbilDataFormatItemGajiBanbox}
 *       &mdash; dipakai {@code ItemGajiAction}, {@code ItemGajiPegawaiAction}, dan
 *       {@code ItemGajiTreeAction} untuk memilih format yang sedang diedit.</li>
 *   <li><b>Slip gaji</b> {@code LaporanSlipGajiPegawaiPerOrang} dan
 *       {@code LaporanSlipGajiRealPegawaiPerOrang} &mdash; pengguna memilih salah satu dari
 *       kelima format milik pegawai ({@code pegawai.ambilFormatItemGajis()}), lalu isi kertasnya
 *       dibangun {@code ItemGajiPegawaiTreeModel(false, formatItemGaji, pegawai, ...)}. <b>Satu
 *       pegawai bisa menerima beberapa slip berbeda dalam satu periode</b>, satu per format
 *       &mdash; inilah alasan kelas ini ada dan alasan kolom format tersebar ke mana-mana.</li>
 *   <li><b>Rantai pembayaran</b> {@code BayarGajiPegawaiAction} dan {@code GajiPegawaiAction}
 *       menyaring {@code Restrictions.eq("formatItemGaji", ...)} (sering di-{@code or} dengan
 *       {@code isNull}) saat mengumpulkan komponen yang akan dibayarkan.</li>
 *   <li>{@code ais.common.InitData} mendaftarkan kelas ini di {@code initClasses(...)} bersama
 *       master lain, sehingga tabelnya ikut disiapkan saat inisialisasi instalasi.</li>
 * </ul>
 *
 * <h2>Alur "Copy Data" &mdash; satu-satunya operasi bisnis nyata di sekitar kelas ini</h2>
 *
 * <p>Tombol salin pada layar master memanggil {@code FormatItemGajiAction.onSave()} dengan
 * {@code copy=true}: baris lama di-{@code clone()} ke variabel sementara, sebuah
 * {@code new FormatItemGaji()} kosong diisi dari form, disimpan, lalu
 * {@code new ItemGajiTreeModel(true, tempFormatItemGaji).copyByFormat(null, null, formatItemGaji)}
 * <b>menyalin seluruh pohon {@link ItemGaji} milik format sumber ke format baru</b>. Jadi
 * "menyalin format" bukan operasi kosmetik &mdash; ia menggandakan puluhan baris katalog komponen
 * beserta rumusnya. Karena {@link #clone()} diwarisi dari {@link GeneralValueObject} dan tidak
 * mengosongkan {@code satuanKerja}, format hasil salinan mewarisi tenant sumbernya kecuali
 * pengguna mengubahnya di form.</p>
 *
 * <h2>Cakupan tenant &amp; hak akses &mdash; hasil verifikasi dari sisi entity ini sendiri</h2>
 *
 * <p>Karena kelas ini adalah simpul tenant seluruh domain gaji, permukaan tulisnya ditelusuri satu
 * per satu. Hasilnya campuran: beberapa jalur ternyata <b>lebih aman</b> dari dugaan, satu jalur
 * jelas <b>lebih longgar</b>.</p>
 *
 * <ol>
 *   <li><b>Layar ZK master &mdash; gerbang hak akses ADA (verifikasi positif).</b>
 *       {@code FormatItemGajiAction.doAfterCompose()} memeriksa
 *       {@code CommonPrivilages.checkPrevilages(READ)} dan menendang keluar bila gagal, lalu
 *       menurunkan {@code addB}/{@code edit}/{@code delete} dari {@code CREATE}/{@code UPDATE}/
 *       {@code DELETE}. Tombol Ubah/Copy/Hapus benar-benar dipagari bendera itu, dan checkbox
 *       "Aktif" inline pun memakai {@code checkbox.setDisabled(!edit)} &mdash; kontras dengan pola
 *       checkbox-grid-tanpa-gerbang yang ditemukan di beberapa master keuangan lain.</li>
 *   <li><b>Pemilih Satuan Kerja pada dialog &mdash; terkunci untuk pengguna biasa (verifikasi
 *       positif).</b> Dialog memakai {@code new AmbilDataSatuanKerjaBanbox(true)}; constructor itu
 *       memasang nilai satuan kerja pengguna dan memanggil {@code setDisabled(true)} bila
 *       pengguna tidak punya hak {@code getMelihatDataSatkerLain()}. Jadi operator biasa tidak
 *       bisa memilih tenant lain lewat layar ini.</li>
 *   <li><b>Fail-open cakupan pada daftar master (instance dari pola sistemik yang sudah
 *       dikenal).</b> {@code FormatItemGajiAction.initCriteria()} membangun himpunan satuan kerja
 *       lewat {@code SekolahUtil.ambilSatuanKerjas()}, lalu:
 *       {@code satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1") : ...}. Bila himpunan
 *       itu kosong &mdash; mis. tabel yayasan belum terisi, yayasan tidak teridentifikasi dari
 *       domain request, atau daftar kode satuan kerja pada role salah ketik sehingga tidak cocok
 *       satu pun &mdash; <b>seluruh format milik seluruh tenant tampil</b>, lengkap dengan tombol
 *       Ubah/Copy/Hapus yang aktif bagi pemegang hak tulis. Bentuknya persis template
 *       {@code size()==0 → "1=1"} yang sudah terdokumentasi luas di codebase ini; yang khas di
 *       sini adalah <b>radius ledakannya</b>: baris yang bisa disunting itu adalah jangkar tenant
 *       bagi seluruh riwayat slip gaji.</li>
 *   <li><b>Pengecekan nama unik bersifat GLOBAL lintas tenant (kuirk).</b>
 *       {@code checkNamaFormatItemGaji()} menghitung baris dengan {@code nama} sama <b>tanpa</b>
 *       restriksi {@code satuanKerja}. Akibatnya dua unit kerja berbeda tidak boleh memakai nama
 *       format yang sama (mis. dua sekolah sama-sama ingin "Gaji Guru Tetap"), dan pesan
 *       "sudah terdaftar" menjadi oracle kecil yang membocorkan keberadaan nama format milik
 *       tenant lain. Ini keterbatasan desain, bukan penjaga keamanan.</li>
 *   <li><b>Bandbox pemilih format tidak menyaring tenant sama sekali.</b>
 *       {@code AmbilDataFormatItemGajiBanbox.onSearchDefault()} hanya menyaring {@code aktif} dan
 *       {@code nama} ({@code ilike ANYWHERE}); pencocokan {@code ON_OK}-nya pun mencari
 *       {@code nama} persis di seluruh tabel. Konsisten dengan catatan yang sudah ditulis di
 *       {@link ItemGaji}: justru bandbox yang seharusnya membatasi lingkup malah global.</li>
 *   <li><b>Generic CRUD v2 (New UI) &mdash; MUTASI TIDAK TERJANGKAU (verifikasi negatif yang
 *       menenangkan).</b> Halaman {@code WEB-INF/new/payroll/uiux/format_item_gaji.jsp} memang
 *       mendaftarkan kelas ini sebagai kandidat entity, tetapi
 *       {@code GenericCrudAutoDefinitionFactory} menghitung
 *       {@code mutable = !restrictedClass && (actionCreate || actionUpdate)} dan kedua bendera itu
 *       berasal dari {@code GenericCrudExistingActionInvoker}, yang mensyaratkan Action sumber
 *       punya method {@code init} <b>berparameter tunggal</b>. {@code FormatItemGajiAction.init}
 *       berparameter dua ({@code FormatItemGaji}, {@code Boolean copy}), sehingga
 *       {@code compatibleInit()} mengembalikan {@code null} dan definisi jatuh ke
 *       {@code READ_ONLY}: create/update/delete dimatikan.</li>
 *   <li><b>Koreksi penting terhadap {@code task_7b6038ac} untuk kelas ini.</b> Alasan yang dipakai
 *       untuk memvonis seluruh {@code payroll/*} adalah "properti relasi entity berada di luar
 *       whitelist nama tetap". Untuk kelas ini alasan itu <b>tidak berlaku</b>:
 *       {@code GenericCrudAutoEntityAdapter.scopeBindings()} memasang pembatas untuk nama
 *       {@code yayasan|sekolah|program|fakultas|jurusan|satuanKerja} (plus enam nama aktor
 *       bersyarat peran), dan kelas ini <b>memang punya {@code satuanKerja}</b> &mdash; jadi
 *       pembacaan lewat CRUD generik benar-benar tersaring per tenant. Sisa celahnya sempit tapi
 *       nyata: {@code addScope()} membuka dengan {@code if (value == null) return;}, sehingga bila
 *       {@code Tbmuser.getSatuanKerja()} mengembalikan {@code null} (pengguna tanpa pegawai/
 *       sekolah/jurusan/fakultas yang punya satuan kerja &mdash; termasuk <b>akun {@code demo}</b>,
 *       yang di {@code Tbmuser.getSatuanKerja()} sengaja di-{@code null}-kan bila
 *       {@code ConstantValues.aktifkan_akun_demo}) pembatas tidak dipasang sama sekali dan daftar
 *       jadi lintas tenant.</li>
 *   <li><b>Pewarisan hak lewat menu induk &mdash; TERVERIFIKASI, dan di sini bentuknya paling
 *       longgar.</b> Ada permukaan ketiga yang mudah terlewat: berkas
 *       {@code WEB-INF/baru/modul/pagesmasterpayrollformatitemgajizul/index.jsp} yang isinya hanya
 *       {@code DynamicJspCrudGenerator.generate(FormatItemGaji.class)} &mdash; CRUD generik
 *       lengkap. Tiga fakta yang terverifikasi dan bergabung menjadi masalah:
 *       <ul>
 *         <li>{@code DynamicJspCrudGenerator} menyaring konteks hanya untuk nama
 *             {@code siswa|mahasiswa|fakultas|jurusan|sekolah|yayasan}
 *             ({@code addContextProps()}) &mdash; <b>{@code satuanKerja} tidak ada di daftar itu</b>,
 *             jadi daftarnya global lintas tenant;</li>
 *         <li>{@code isAutoSkipField()} hanya membuang {@code id}/{@code oleh}/{@code olehId}/
 *             {@code tanggal_dirubah}/{@code copyDari}/{@code class} dan field berbau
 *             password/token &mdash; jadi {@code satuanKerja} <b>ikut tampil sebagai isian yang
 *             bisa diubah</b> pada form otomatisnya;</li>
 *         <li>gerbang tulisnya ({@code canCreate}/{@code canEdit}/{@code canDelete}) memanggil
 *             {@code CommonPrivilages.checkPrevilages(kode, user)}, yang mengevaluasi hak terhadap
 *             {@code Common.getCurrentMenu()} &mdash; yaitu atribut sesi {@code current_menu}. Di
 *             {@code WEB-INF/baru/index.jsp} atribut itu diisi dari <b>parameter permintaan
 *             {@code menu}</b> apa adanya, tanpa pernah dicocokkan dengan halaman {@code p} yang
 *             sebenarnya dirender.</li>
 *       </ul>
 *       Gabungannya: hak UPDATE pada menu <i>mana pun</i> dapat dipakai untuk menyunting halaman
 *       <i>ini</i>, atas baris tenant mana pun, termasuk mengubah kolom {@code satuan_kerja}-nya.
 *       Inilah bentuk konkret "pewarisan hak lewat menu induk" untuk kelas ini, dan konsekuensinya
 *       jauh lebih berat daripada pada master biasa karena satu penyimpanan memindahkan cakupan
 *       tenant seluruh slip gaji yang memakai format tersebut.</li>
 *   <li><b>{@code task_66986071} (fail-open {@code bolehAksi()} pada helper API REST) &mdash;
 *       verifikasi negatif.</b> Tidak ada satu pun {@code *ApiHelper} atau rute REST yang menyentuh
 *       kelas ini; satu-satunya kemunculan di lapisan servlet adalah {@code LaporanApi} yang
 *       memakainya untuk <b>membaca</b> parameter cetak slip
 *       ({@code Common.insertProperty(FormatItemGaji.class, ...)}, dan
 *       {@code pegawai.ambilFormatItemGajis()}). Tidak ada permukaan tulis REST.</li>
 * </ol>
 *
 * <h2>Getter yang menulis balik &mdash; dan mengapa yang ini justru aman</h2>
 *
 * <p>Empat getter relasi di kelas ini memakai pola khas repo, {@code x = check(x); return x;}
 * ({@link #getCabang()}, {@link #getDepartemen()}, {@link #getLevelJabatan()},
 * {@link #getSatuanKerja()}). Penulisan balik itu <b>tidak destruktif</b>:
 * {@link GeneralValueObject#check(Object)} paling jauh menukar referensi ke instance kanonik/hasil
 * muat ulang untuk <b>id yang sama</b>, dan pada setiap kegagalan ia mengembalikan argumennya apa
 * adanya (blok {@code catch} terakhirnya berkomentar tegas: {@code check(...)} tidak boleh membuat
 * getter entity gagal). Ia <b>tidak pernah</b> mengembalikan {@code null} untuk masukan non-null
 * dan tidak pernah menukar ke entity lain. Ini penting untuk dicatat justru karena taruhannya
 * tinggi: seandainya {@link #getSatuanKerja()} bisa menulis balik nilai yang berbeda, sekadar
 * <i>membaca</i> daftar format &mdash; dengan {@code dynamicUpdate=true} dan akses properti
 * Hibernate &mdash; dapat mem-flush perpindahan tenant seluruh slip gaji ke basis data. Sejauh
 * pembacaan kode saat ini, bahaya itu <b>tidak ada</b> di kelas ini; satu-satunya jalur perpindahan
 * tenant adalah {@link #setSatuanKerja(SatuanKerja)} yang dipanggil eksplisit oleh layar penyunting.
 * Bandingkan dengan {@link PembayaranGajiPunyaPegawai#getFormatItemGaji()} yang benar-benar
 * menyalin nilai dari {@code pegawai} saat kosong &mdash; pola menulis-balik-bermakna itu ada di
 * sana, bukan di sini.</p>
 *
 * <p>Satu-satunya getter dengan logika lain adalah {@link #getAktif()}, yang memperlakukan
 * {@code null} sebagai {@code true} <b>tanpa</b> menulis balik ke field. Artinya baris lama yang
 * kolom {@code aktif}-nya masih {@code NULL} dianggap aktif, dan status itu tidak pernah
 * "dipadatkan" ke basis data hanya karena dibaca.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ul>
 *   <li><b>Identitas &amp; jejak audit</b>: {@link #getId()}, {@link #setId(Long)},
 *       {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@code onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut deskriptif</b>: {@link #getNama()}, {@link #setNama(String)},
 *       {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Dimensi organisasi (penyaring/label saja)</b>: {@link #getCabang()},
 *       {@link #setCabang(Cabang)}, {@link #getDepartemen()},
 *       {@link #setDepartemen(Departemen)}, {@link #getLevelJabatan()},
 *       {@link #setLevelJabatan(LevelJabatan)}.</li>
 *   <li><b>Cakupan tenant</b>: {@link #getSatuanKerja()}, {@link #setSatuanKerja(SatuanKerja)}.</li>
 *   <li><b>Status</b>: {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 * </ul>
 *
 * <p><b>Catatan tentang {@link GeneralValueObject}:</b> kelas induk BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa dan Hibernate tidak memetakan
 * propertinya. Karena itu field {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah} sengaja <b>dideklarasikan ulang</b> di sini; itu keharusan teknis, bukan
 * duplikasi yang perlu "dirapikan".</p>
 *
 * @see ItemGaji
 * @see ItemGajiPegawai
 * @see PembayaranItemGajiPegawai
 * @see RencanaItemGajiPegawai
 * @see PembayaranGajiPunyaPegawai
 * @see StandarGaji
 * @see ais.database.model.Pegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "format_item_gaji")
public class FormatItemGaji extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini (jejak audit ringan, di luar Envers).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> masukan {@code null} atau kosong <b>diabaikan diam-diam</b> &mdash;
	 * nilai lama dipertahankan, bukan dihapus. Ini pola berulang di seluruh model AIS dan
	 * disengaja: jejak audit tidak boleh terhapus hanya karena pemanggil (mis. proses batch tanpa
	 * konteks pengguna) tidak tahu siapa pelakunya. Konsekuensinya, kolom ini tidak bisa
	 * dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Label baris ini, yaitu {@link #getNama()} apa adanya.
	 *
	 * <p>Dipakai luas sebagai teks tampilan: item combo/bandbox pemilih format, judul kolom pada
	 * grid master, dan keluaran {@code System.out.println} di
	 * {@code AmbilDataFormatItemGajiBanbox}. Karena kolom {@code nama} bersifat NOT NULL di basis
	 * data, hasilnya {@code null} hanya mungkin untuk instance yang belum tersimpan.</p>
	 *
	 * @return nama format, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Perilakunya sama dengan {@link #setOlehId(String)}: masukan {@code null}/kosong diabaikan
	 * sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} agar
	 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} terisi otomatis dari
	 * konteks pengguna aktif tepat sebelum baris di-{@code UPDATE}.
	 *
	 * <p>Implementasi wajib dari {@code abstract} milik {@link GeneralValueObject}. Jangan
	 * dipanggil manual &mdash; ia dipicu penyedia persistence, bukan kode aplikasi. Perhatikan
	 * bahwa callback ini <b>tidak</b> berjalan pada {@code INSERT}, sehingga baris baru
	 * mengandalkan nilai awal {@link #tanggal_dirubah} di bawah.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) agar baris baru tetap punya cap waktu meski
	 * {@code @PreUpdate} belum pernah berjalan; lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis lewat {@code onUpdate()}; setel manual hanya untuk keperluan impor
	 * atau perbaikan data.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return cap waktu perubahan; tidak pernah {@code null} untuk object yang baru dibuat di JVM
	 *         ini karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama format; kolom {@code nama} NOT NULL. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas; kolom {@code keterangan}. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Dimensi cabang (opsional, penyaring/label saja). Lihat {@link #getCabang()}. */
	private Cabang cabang;
	/** Dimensi departemen (opsional, penyaring/label saja). Lihat {@link #getDepartemen()}. */
	private Departemen departemen;
	/** Dimensi level jabatan (opsional, penyaring/label saja). Lihat {@link #getLevelJabatan()}. */
	private LevelJabatan levelJabatan;
	/**
	 * Unit kerja pemilik format ini &mdash; <b>satu-satunya kolom tenant di sepanjang rantai
	 * penggajian</b>. Lihat {@link #getSatuanKerja()} dan pembahasan pada Javadoc kelas.
	 */
	private SatuanKerja satuanKerja;
	/** Bendera aktif; {@code null} diartikan aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Constructor kosong yang dibutuhkan Hibernate dan dipakai layar master saat menekan "Tambah"
	 * maupun saat menyalin format ({@code copy=true}).
	 */
	public FormatItemGaji() {
	}

	/**
	 * Primary key baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan ditandai {@code insertable = false},
	 * sehingga nilainya baru tersedia setelah {@code flush}/{@code save}. Kode pemanggil memakai
	 * {@code getId() == null} sebagai penanda "baris baru" &mdash; mis.
	 * {@code FormatItemGajiAction.init()} untuk memilih judul dialog, dan
	 * {@code checkNamaFormatItemGaji()} untuk memutuskan apakah baris sendiri perlu dikecualikan
	 * dari pemeriksaan nama kembar.</p>
	 *
	 * @return id baris, atau {@code null} untuk instance yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Hanya untuk Hibernate dan kode infrastruktur; jangan dipakai kode
	 * bisnis.
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama format &mdash; identitas yang dilihat pengguna di seluruh aplikasi.
	 *
	 * <p>Kolom {@code nama} NOT NULL, panjang 255. Nilai ini dipakai
	 * {@code AmbilDataFormatItemGajiBanbox} untuk mencocokkan ketikan pengguna secara <b>persis</b>
	 * ({@code ilike ... MatchMode.EXACT}) saat menekan Enter, jadi mengubahnya akan memutus
	 * pintasan pengetikan yang biasa dipakai operator. Keunikan namanya dijaga
	 * {@code FormatItemGajiAction.checkNamaFormatItemGaji()} &mdash; namun pemeriksaan itu
	 * <b>global lintas satuan kerja</b> (lihat Javadoc kelas).</p>
	 *
	 * @return nama format
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama format. Wajib diisi; layar master menolak menyimpan bila kosong.
	 *
	 * @param nama nama format
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tentang format ini (ditampilkan sebagai kolom pada grid master).
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Cabang yang diasosiasikan dengan format ini.
	 *
	 * <p>Relasi {@code LAZY} opsional (kolom {@code cabang}, nullable). Perannya <b>deskriptif</b>:
	 * dipakai sebagai penyaring pada layar master dan bandbox, serta sebagai label kolom grid.
	 * Tidak ada kode yang memakainya untuk memilih format bagi seorang pegawai secara otomatis.</p>
	 *
	 * <p>Getter memanggil {@link GeneralValueObject#check(Object)} lalu menulis balik hasilnya ke
	 * field. Penulisan balik itu aman: {@code check()} hanya menukar referensi ke instance kanonik
	 * atau hasil muat ulang untuk id yang sama, dan mengembalikan argumen apa adanya pada setiap
	 * kegagalan &mdash; tujuannya menghindari {@code LazyInitializationException} pada proxy yang
	 * sudah terlepas dari session.</p>
	 *
	 * @return cabang terkait, atau {@code null} bila format tidak dibatasi ke cabang tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cabang", nullable = true)
	public Cabang getCabang() {
		cabang = check(cabang);
		return cabang;
	}

	/**
	 * Menyetel cabang terkait.
	 *
	 * @param cabang cabang; {@code null} berarti format tidak dibatasi ke cabang tertentu
	 */
	public void setCabang(Cabang cabang) {
		this.cabang = cabang;
	}

	/**
	 * Departemen yang diasosiasikan dengan format ini.
	 *
	 * <p>Sama sifatnya dengan {@link #getCabang()}: relasi {@code LAZY} opsional (kolom
	 * {@code departemen}, nullable), murni penyaring pencarian dan label &mdash; bukan aturan
	 * pemilihan format. Getter juga memakai {@code check()} dengan penulisan balik yang aman.</p>
	 *
	 * @return departemen terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "departemen", nullable = true)
	public Departemen getDepartemen() {
		departemen = check(departemen);
		return departemen;
	}

	/**
	 * Menyetel departemen terkait.
	 *
	 * @param departemen departemen; boleh {@code null}
	 */
	public void setDepartemen(Departemen departemen) {
		this.departemen = departemen;
	}

	/**
	 * Level/jenjang jabatan yang diasosiasikan dengan format ini (kolom {@code level_jabatan},
	 * nullable).
	 *
	 * <p>Perannya identik dengan {@link #getCabang()} dan {@link #getDepartemen()} &mdash;
	 * penyaring dan label, bukan aturan. Pada layar master labelnya ditampilkan sebagai
	 * "Jabatan". Getter memakai {@code check()} dengan penulisan balik yang aman.</p>
	 *
	 * @return level jabatan terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "level_jabatan", nullable = true)
	public LevelJabatan getLevelJabatan() {
		levelJabatan = check(levelJabatan);
		return levelJabatan;
	}

	/**
	 * Menyetel level jabatan terkait.
	 *
	 * @param levelJabatan level jabatan; boleh {@code null}
	 */
	public void setLevelJabatan(LevelJabatan levelJabatan) {
		this.levelJabatan = levelJabatan;
	}

	/**
	 * Status aktif format ini, dengan <b>default implisit {@code true}</b>.
	 *
	 * <p>Kolom {@code aktif} boleh {@code NULL}, dan getter ini memperlakukan {@code NULL} sebagai
	 * aktif. Penting: nilai default itu <b>tidak</b> ditulis balik ke field, jadi sekadar membaca
	 * tidak pernah "memadatkan" {@code NULL} menjadi {@code true} di basis data &mdash; berbeda
	 * dari beberapa getter default di entity lain yang menulis balik.</p>
	 *
	 * <p><b>Dampak menonaktifkan format</b> lebih luas dari yang terlihat: kriteria
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} dipakai layar master
	 * <i>maupun</i> bandbox pemilih dan combo, sehingga format yang dinonaktifkan langsung hilang
	 * dari seluruh pemilih. Baris {@link ItemGaji}, {@link ItemGajiPegawai}, dan seluruh slip
	 * historis yang menunjuk format itu <b>tidak ikut dinonaktifkan</b> &mdash; mereka tetap ada di
	 * basis data tetapi formatnya tidak lagi dapat dipilih dari UI.</p>
	 *
	 * @return {@code true} bila aktif atau bila kolomnya masih {@code NULL}; {@code false} hanya
	 *         bila secara eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" inline pada grid
	 * {@code FormatItemGajiAction.FormatItemGajiRenderer} &mdash; yang, berbeda dari beberapa
	 * master keuangan lain, memang dipagari hak {@code UPDATE} lewat
	 * {@code checkbox.setDisabled(!edit)} &mdash; lalu segera disimpan dengan
	 * {@code Common.refreshSaveOrUpdate(...)}. Perubahan langsung berlaku tanpa dialog
	 * konfirmasi.</p>
	 *
	 * @param aktif status aktif; {@code null} akan dibaca sebagai aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * <b>Unit kerja (tenant) pemilik format ini</b> &mdash; kolom {@code satuan_kerja}, nullable,
	 * relasi {@code LAZY}.
	 *
	 * <p>Ini method terpenting di kelas ini. Karena {@link ItemGaji}, {@link ItemGajiPegawai},
	 * {@link PembayaranItemGajiPegawai}, {@link RencanaItemGajiPegawai}, dan
	 * {@link PembayaranGajiPunyaPegawai} <b>tidak punya kolom tenant sendiri</b>, nilai yang
	 * dikembalikan di sini adalah jawaban tunggal atas pertanyaan "seluruh komponen dan slip gaji
	 * yang menunjuk format ini milik unit kerja mana?".</p>
	 *
	 * <p><b>Nilai {@code null} bermakna khusus.</b> Pada {@code FormatItemGajiAction.initCriteria()}
	 * baris ber-{@code satuanKerja} {@code NULL} hanya ditampilkan bila
	 * {@code Common.getApakahAdmin()} bernilai benar &mdash; jadi format tanpa unit kerja efektifnya
	 * "format milik pusat/administrator". Sebaliknya, banyak query hilir menyaring format lewat
	 * {@code Restrictions.or(isNull("formatItemGaji"), eq("formatItemGaji", ...))}, sehingga baris
	 * tanpa format ikut terbawa; jangan menganggap {@code NULL} selalu berarti "tidak relevan".</p>
	 *
	 * <p><b>Penulisan balik tidak destruktif.</b> Seperti getter relasi lain di kelas ini, method
	 * ini memanggil {@link GeneralValueObject#check(Object)} lalu menyimpan hasilnya kembali ke
	 * field. {@code check()} hanya dapat menukar referensi ke instance <b>ber-id sama</b> (peta
	 * identitas entity, cache, atau muat ulang lewat session terpisah) dan pada setiap kegagalan
	 * mengembalikan argumennya apa adanya &mdash; ia tidak pernah mengembalikan {@code null} untuk
	 * masukan non-null dan tidak pernah menukar ke unit kerja lain. Ini layak ditegaskan karena
	 * taruhannya besar: dengan {@code dynamicUpdate = true} dan akses properti Hibernate, sebuah
	 * penulisan balik yang mengubah nilai akan ter-flush secara otomatis dan <b>memindahkan
	 * cakupan tenant seluruh slip gaji</b> yang memakai format ini hanya karena daftarnya dibaca.
	 * Perilaku itu tidak terjadi di sini.</p>
	 *
	 * @return unit kerja pemilik, atau {@code null} untuk format tanpa unit kerja (praktis:
	 *         format tingkat pusat, hanya terlihat oleh admin di layar master)
	 * @see #setSatuanKerja(SatuanKerja)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik format ini.
	 *
	 * <p><b>PERINGATAN &mdash; operasi berdampak lintas data.</b> Setter ini adalah satu-satunya
	 * jalur yang benar-benar memindahkan cakupan tenant di domain penggajian. Karena entity anak
	 * ({@link ItemGaji}, {@link ItemGajiPegawai}, {@link PembayaranItemGajiPegawai},
	 * {@link RencanaItemGajiPegawai}, {@link PembayaranGajiPunyaPegawai}) menyimpan FK ke format
	 * dan <b>tidak</b> menyimpan tenant sendiri, mengubah nilai di sini pada format yang sudah
	 * dipakai akan seketika memindahkan seluruh komponen gaji dan seluruh <b>baris slip gaji
	 * historis</b> yang menunjuknya ke unit kerja lain &mdash; tanpa satu baris pun di tabel anak
	 * berubah, tanpa dialog konfirmasi, dan tanpa pemeriksaan apakah format itu sudah dipakai.
	 * Satu-satunya jejaknya adalah revisi Envers di {@code format_item_gaji_aud}.</p>
	 *
	 * <p><b>Dari mana dipanggil.</b> Terverifikasi ada dua permukaan tulis:</p>
	 * <ul>
	 *   <li>{@code FormatItemGajiAction.onSave()} &mdash; nilainya diambil dari
	 *       {@code AmbilDataSatuanKerjaBanbox}. Bandbox itu <b>terkunci</b> (nilai dipreset dan
	 *       {@code setDisabled(true)}) bagi pengguna tanpa hak
	 *       {@code Tbmrole.getMelihatDataSatkerLain()}, sehingga operator biasa tidak dapat memilih
	 *       tenant lain lewat layar ini.</li>
	 *   <li>Halaman CRUD generik lama {@code WEB-INF/baru/modul/pagesmasterpayrollformatitemgajizul/
	 *       index.jsp} ({@code DynamicJspCrudGenerator}) &mdash; di sini {@code satuanKerja} tampil
	 *       sebagai isian biasa yang bisa diubah, daftarnya <b>tidak</b> disaring per tenant
	 *       ({@code addContextProps()} tidak menyertakan {@code satuanKerja}), dan gerbang tulisnya
	 *       dievaluasi terhadap {@code current_menu} sesi yang diisi dari parameter permintaan
	 *       {@code menu}. Lihat pembahasan lengkap pada Javadoc kelas.</li>
	 * </ul>
	 *
	 * @param satuanKerja unit kerja pemilik; {@code null} menjadikan format ini format tanpa unit
	 *            kerja (hanya tampil di layar master bagi admin)
	 * @see #getSatuanKerja()
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
