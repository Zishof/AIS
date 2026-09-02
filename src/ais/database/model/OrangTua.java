package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * Entity <b>data orang tua/wali</b> (tabel {@code public.orang_tua}) &mdash; satu baris mewakili
 * <i>satu keluarga</i>, bukan satu orang: satu baris memuat sekaligus data ayah, ibu, dan wali,
 * ditambah satu alamat rumah bersama dan daftar anak yang menempel pada keluarga itu.
 *
 * <p>Kelas ini adalah induk dari sisi relasi: yang menunjuk ke sini adalah
 * {@link Mahasiswa#getOrangTua()}, {@link ais.database.model.sekolah.Siswa#getOrangTua()},
 * {@link Pegawai#getOrangTua()}, dan {@link Tbmuser#getOrangTua()} &mdash; masing-masing lewat kolom
 * {@code orang_tua} di tabelnya sendiri. Jadi <b>satu baris {@code orang_tua} bisa dipakai bersama
 * beberapa anak</b> (kakak-adik satu keluarga), dan itu memang tujuannya.</p>
 *
 * <h3>Untuk apa entity ini dipakai</h3>
 * <ul>
 *   <li><b>Portal orang tua.</b> {@link Tbmuser} (akun login) punya kolom {@code orang_tua};
 *   begitu akun wali masuk, hampir semua layar sekolah/kampus menyaring datanya dengan
 *   {@code Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())} atau padanan
 *   mahasiswanya. Lihat {@link #ambilAnakSiswa()}, {@link #ambilAnakMahasiswa()},
 *   {@link #ambilAnakMahasiswaObject()} &mdash; ketiganya adalah <b>penentu ruang lingkup akses
 *   data</b>, bukan sekadar utilitas tampilan. Pemanggilnya tersebar di 150-an berkas
 *   ({@code AbsensiAction}, {@code DepositSiswaAction}, {@code BniRequestAction},
 *   {@code BsiRequestAction}, {@code DetailpertemuanHelper}, dan seterusnya).</li>
 *   <li><b>Korespondensi &amp; penagihan.</b> Nama, telepon, dan email ayah/ibu/wali dipakai
 *   pengumuman akademis, pengiriman tagihan, dan berkas pembayaran daring.</li>
 *   <li><b>Berkas personalia.</b> {@link Pegawai} juga menunjuk ke sini untuk data orang tua
 *   pegawai (tunjangan keluarga), dan {@link #getPegawai()} adalah penunjuk balik ke pegawai bila
 *   orang tua yang bersangkutan kebetulan pegawai institusi sendiri.</li>
 * </ul>
 *
 * <h3>Hubungan dengan field orang tua di {@link BiodataMahasiswa} &mdash; DUA SIMPANAN TERPISAH</h3>
 * <p>Ini bagian yang paling mudah menyesatkan. {@link BiodataMahasiswa} <b>juga</b> punya trio
 * ayah/ibu/wali sendiri ({@code namaAyah}, {@code namaIbu}, {@code namaWali}, tanggal lahir, NIK,
 * telepon, pekerjaan, pendidikan, penghasilan) di tabelnya sendiri. Keduanya
 * <b>tidak pernah disinkronkan</b>: tidak ada satu pun getter/setter di kelas ini yang membaca
 * {@link BiodataMahasiswa}, dan sebaliknya {@link BiodataMahasiswa} tidak menyentuh kelas ini sama
 * sekali. Formulir "Keluarga" di {@code ais.action.master.BiodataMahasiswaAction} (method
 * {@code initOrangTua(BiodataMahasiswa)} &mdash; namanya menipu) menulis <b>hanya</b> ke
 * {@link BiodataMahasiswa}; layar {@code ais.action.master.OrangTuaAction} menulis <b>hanya</b> ke
 * tabel ini. Konsekuensinya nama ayah seorang mahasiswa bisa berbeda di dua layar tanpa ada yang
 * menganggapnya salah.</p>
 * <p>Selisih cakupan yang perlu diketahui saat memilih sumber data:</p>
 * <ul>
 *   <li><b>Hanya ada di sini:</b> {@link #getNikWali()}, {@link #getNoKK()} (nomor Kartu
 *   Keluarga &mdash; ada di kedua entity, tetapi di sini konteksnya keluarga, bukan mahasiswa),
 *   {@link #getAnak()} (daftar anak), {@link #getPegawai()}, {@link #getAktif()},
 *   dan alamat rumah keluarga yang berdiri sendiri.</li>
 *   <li><b>Hanya ada di {@link BiodataMahasiswa}:</b> kategori {@link PekerjaanOrangTua},
 *   {@link PendidikanOrangTua}, {@code pendapatanOrtu}/{@code pendapatanOrtuIbu}, teks hasil format
 *   {@code penghasilanAyah}/{@code penghasilanIbu}, angka {@code penghasilanOrangTua}, dan
 *   {@code bersaudara}. Kelas ini <b>tidak memakai</b> {@link PekerjaanOrangTua} maupun
 *   {@link PendidikanOrangTua} sedikit pun; yang dipakai hanya {@link Pekerjaan},
 *   {@link Jenjang}, dan {@link Penghasilan} &mdash; yaitu <i>sisi pasangan kolom kembar yang
 *   lain</i>. Jadi "pekerjaan ayah" di layar biodata mahasiswa dan "pekerjaan ayah" di layar orang
 *   tua bukan hanya baris berbeda, tetapi <b>tabel acuan yang berbeda</b> dan tidak dapat
 *   dibandingkan langsung.</li>
 *   <li><b>Asimetri di dalam kelas ini sendiri:</b> {@link PendapatanOrangTua} hanya dipakai untuk
 *   <i>wali</i> ({@link #getPendapatanWali()}); ayah dan ibu tidak punya padanannya. Sebaliknya
 *   {@link Penghasilan} tersedia untuk ketiganya. Ayah dan ibu punya email
 *   ({@link #getEmailAyah()}, {@link #getEmailIbu()}), wali tidak. Ketiganya punya NIK dan telepon.
 *   Tidak ada {@code jenjangPendidikan}/{@code jenisPekerjaan} yang hilang &mdash; trio itu
 *   lengkap.</li>
 * </ul>
 * <p><b>Aturan praktis:</b> untuk pelaporan PDDikti/Feeder dan formulir registrasi mahasiswa,
 * sumber kebenarannya {@link BiodataMahasiswa}; untuk portal wali, penagihan, dan data lintas-anak
 * (termasuk siswa sekolah), sumber kebenarannya kelas ini.</p>
 *
 * <h3>Daftar anak disimpan sebagai JSON, bukan tabel relasi</h3>
 * <p>Kolom {@code anak} bertipe {@code text} dan berisi satu {@link JSONObject} datar dengan kunci
 * berpola {@code "siswa_<id>"} dan {@code "mahasiswa_<id>"}, nilainya id yang sama:</p>
 * <pre>
 *   {"siswa_1201":1201,"siswa_1355":1355,"mahasiswa_88014":88014}
 * </pre>
 * <p>Kunci dibentuk dan dibuang di {@code OrangTuaAction} ({@code jsonObject.put("siswa_" +
 * siswa.getId(), ...)} / {@code jsonObject.remove(...)}). Konsekuensi bentuk ini:</p>
 * <ul>
 *   <li>tidak ada <i>foreign key</i>, jadi id anak yang barisnya sudah dihapus tetap tertinggal di
 *   JSON dan baru ketahuan saat {@link #ambilAnakMahasiswaObject()} gagal menemukannya;</li>
 *   <li>arahnya <b>dua kali</b> disimpan: anak menunjuk induk lewat kolom {@code orang_tua},
 *   induk menunjuk anak lewat JSON ini. Keduanya harus dijaga sendiri oleh lapisan UI dan bisa
 *   saling bertentangan;</li>
 *   <li>tidak bisa di-query dari SQL biasa; semua penyaringan dilakukan di Java dengan
 *   {@code Restrictions.in(...)} atas hasil {@link #ambilAnakSiswa()}.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; representasi</b> &mdash; {@link #OrangTua()}, {@link #getId()},
 *   {@link #setId(Long)}, {@link #toString()}, {@link #getKeterangan()}, {@link #getAktif()}.</li>
 *   <li><b>Alamat rumah keluarga</b> &mdash; {@link #getAlamat()}, {@link #getDusun()},
 *   {@link #getRt()}, {@link #getRw()}, {@link #getKelurahan()}, {@link #getKodepos()},
 *   {@link #getKecamatan()}, {@link #getKota()}, {@link #getPropinsi()}.</li>
 *   <li><b>Data ayah</b> &mdash; {@link #getNamaAyah()}, {@link #getTanggalLahirAyah()},
 *   {@link #getNikAyah()}, {@link #getTelpAyah()}, {@link #getEmailAyah()},
 *   {@link #getJenisPekerjaanAyah()}, {@link #getJenjangPendidikanAyah()},
 *   {@link #getJenisPenghasilanAyah()}.</li>
 *   <li><b>Data ibu</b> &mdash; {@link #getNamaIbu()}, {@link #getTanggalLahirIbu()},
 *   {@link #getNikIbu()}, {@link #getTelpIbu()}, {@link #getEmailIbu()},
 *   {@link #getJenisPekerjaanIbu()}, {@link #getJenjangPendidikanIbu()},
 *   {@link #getJenisPenghasilanIbu()}.</li>
 *   <li><b>Data wali</b> &mdash; {@link #getNamaWali()}, {@link #getTanggalLahirWali()},
 *   {@link #getNikWali()}, {@link #getTelpWali()}, {@link #getJenisPekerjaanWali()},
 *   {@link #getJenjangPendidikanWali()}, {@link #getJenisPenghasilanWali()},
 *   {@link #getPendapatanWali()}.</li>
 *   <li><b>Kartu keluarga &amp; anak</b> &mdash; {@link #getNoKK()}, {@link #getAnak()},
 *   {@link #ambilAnakSiswa()}, {@link #ambilAnakMahasiswa()},
 *   {@link #ambilAnakMahasiswaObject()}.</li>
 *   <li><b>Penunjuk balik</b> &mdash; {@link #getPegawai()}/{@link #setPegawai(Pegawai)}.</li>
 * </ul>
 * <p>Tidak ada query statis, konstanta domain, maupun helper UI di kelas ini. Tiga method
 * {@code ambilAnak*} adalah satu-satunya method bisnis; sisanya pasangan getter/setter Hibernate,
 * sebagian dengan efek samping yang dijelaskan di bawah.</p>
 *
 * <h3>PERINGATAN: getter yang menulis ke field dan ke basis data</h3>
 * <p>Entity ini memakai <i>property access</i> ({@code @Id} terpasang di {@link #getId()}) dan
 * dipetakan {@code dynamicUpdate = true}. Artinya Hibernate membaca nilai lewat getter, sehingga
 * setiap getter yang menimpa field-nya sendiri berpotensi memicu {@code UPDATE} nyata saat flush.
 * Getter berikut melakukannya:</p>
 * <ul>
 *   <li>{@link #getAlamat()}, {@link #getDusun()} &rarr; menulis {@code ""} bila {@code null};</li>
 *   <li>{@link #getKelurahan()} &rarr; menulis {@code "-"} bila kosong, sehingga "belum diisi"
 *   tersimpan sebagai tanda hubung;</li>
 *   <li>{@link #getRt()}, {@link #getRw()}, {@link #getKodepos()} &rarr; membersihkan karakter non
 *   angka, membuang tanda hubung, dan mengganti sentinel lama ({@code "00"}, yang mengandung
 *   {@code "0000"}) menjadi {@code ""} &mdash; hasil pembersihan <b>ditulis balik ke field</b>;</li>
 *   <li>{@link #getKecamatan()} &rarr; mengganti referensi wilayah yatim dengan duplikatnya yang
 *   sehat;</li>
 *   <li>{@link #getPropinsi()} dan {@link #getKota()} &rarr; yang paling berat: <b>membuka session
 *   Hibernate sendiri, memulai transaksi, dan dapat menyimpan baris master {@link Propinsi}
 *   BARU</b>. Membaca alamat sebuah keluarga bisa menambah baris di tabel {@code propinsi}.</li>
 * </ul>
 * <p>Kabar baiknya, kedua getter berat itu <b>menutup session yang mereka buka sendiri</b> lewat
 * {@code HibernateUtil.closeSessionQuietly(session)} di blok {@code finally} (lihat komentar di
 * dalam kode: dulu memakai {@code closeSession()} tanpa argumen yang justru menutup session
 * ThreadLocal milik pemanggil, alias bocor sekaligus merusak session request). Jadi cacat "getter
 * menutup session milik thread pemanggil" yang ditemukan di beberapa entity lain <b>sudah
 * diperbaiki di berkas ini</b>. Selain itu keduanya dijaga bendera {@code transient}
 * {@code resolvingPropinsi}/{@code resolvingKota} sehingga pemanggilan berulang/rekursif dari
 * dalam dirinya sendiri langsung mengembalikan nilai field apa adanya.</p>
 *
 * <h3>Properti yang membayangi {@link GeneralValueObject}</h3>
 * <p>{@link GeneralValueObject} bukan {@code @MappedSuperclass}; ia hanya POJO abstrak yang
 * menyediakan {@code id}, {@code kode}, {@code nama}, {@code nim}, {@code keterangan},
 * {@code nomorUrut}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta accessor-nya.
 * Kelas ini <b>mendeklarasikan ulang</b> {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan {@code keterangan} sebagai field privat sendiri lalu menimpa
 * accessor-nya &mdash; pola yang sejauh audit inisiatif Javadoc ini dilakukan SELURUH entity model
 * AIS. Salinan milik induk tetap ada tetapi tidak pernah terisi. Kontrak
 * {@code equals}/{@code compareTo}/{@code check()} di induk memakai {@code getId()} sehingga tidak
 * terpengaruh &mdash; dengan satu pengecualian yang dicatat di bawah.</p>
 *
 * <h3>Kuirk yang ditemukan saat pendokumentasian (belum diperbaiki)</h3>
 * <ul>
 *   <li><b>{@link #getKeterangan()} melanggar janji induk.</b> {@link GeneralValueObject}
 *   menormalkan {@code null} menjadi {@code ""} dan Javadoc {@code compareTo} di sana menyatakan
 *   cabang {@code keterangan} "selalu terpakai". Override di sini mengembalikan {@code null} apa
 *   adanya, sehingga untuk {@code OrangTua} cabang itu bisa gugur dan dua baris tanpa keterangan
 *   dianggap setara ({@code compareTo} mengembalikan {@code 0}). Pengurutan daftar orang tua
 *   praktis tidak deterministik.</li>
 *   <li><b>{@code nama}, {@code kode}, dan {@code nim} tidak pernah terisi.</b> Kelas ini tidak
 *   menyediakan nilai untuk ketiganya, jadi helper generik yang menampilkan
 *   {@code GeneralValueObject.getNama()} akan melihat {@code null} untuk entity ini. Yang
 *   dipakai sebagai label adalah {@link #toString()}, yaitu {@code id + "-" + keterangan}.</li>
 *   <li><b>{@link #toString()} bisa berbunyi {@code "123-null"}</b> karena membaca <i>field</i>
 *   {@code keterangan} langsung (bukan lewat getter yang juga tidak menormalkan). Ia tidak menyentuh
 *   relasi lazy, jadi setidaknya aman dari {@code LazyInitializationException}.</li>
 *   <li><b>{@link #getTelpWali()} tidak menormalkan {@code null}</b>, sedangkan
 *   {@link #getTelpAyah()} dan {@link #getTelpIbu()} mengembalikan {@code ""}. Kode pemanggil yang
 *   menganggap ketiganya seragam akan kena {@code NullPointerException} pada wali saja.</li>
 *   <li><b>Mayoritas properti tanpa anotasi {@code @Column}.</b> Hanya sembilan yang dianotasi
 *   ({@code id}, {@code alamat}, {@code nama_ayah}, {@code nama_ibu}, {@code no_kk},
 *   {@code keterangan}, {@code anak}, plus kolom join). Karena
 *   {@code ais.database.hibernate.MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}
 *   (nama kolom = nama properti apa adanya, tanpa konversi ke snake_case), nama kolom fisiknya
 *   bercampur gaya: {@code nama_ayah} bersebelahan dengan {@code namaWali}, {@code nikAyah},
 *   {@code telpIbu}, {@code tanggalLahirWali}. Query SQL ad-hoc atas tabel ini wajib
 *   memperhatikannya.</li>
 *   <li><b>{@link #getKota()} dapat mencari kota di bawah propinsi {@code null}.</b> Bila nama
 *   propinsi hasil penelusuran wilayah kosong, {@code selectedPropinsi} tetap {@code null} tetapi
 *   pencarian kota tetap dijalankan dengan {@code Restrictions.eq("propinsi", null)} &mdash; kriteria
 *   yang di SQL menjadi {@code propinsi = null} dan tidak pernah cocok. Hasilnya daftar kosong,
 *   kota tidak pernah terisi, dan seluruh penelusuran mahal itu diulang pada setiap pembacaan
 *   berikutnya.</li>
 *   <li><b>Duplikasi logika wilayah.</b> Blok pencocokan Levenshtein di {@link #getPropinsi()} dan
 *   {@link #getKota()} adalah salinan satu sama lain, dan juga salinan logika yang sama di
 *   {@link BiodataMahasiswa} (di sana sudah dipisah menjadi helper
 *   {@code findOrCreatePropinsi}). Tiga salinan yang sudah mulai berbeda perilakunya.</li>
 *   <li><b>{@link #getPropinsi()} membaca field {@code kota} langsung</b>, tanpa melewati
 *   {@link GeneralValueObject#check(Object)} maupun {@link #getKota()}. Pada entity yang sudah
 *   lepas dari session, {@code kota.getPropinsi()} di sana bisa melempar
 *   {@code LazyInitializationException} &mdash; dan exception itu <b>tidak</b> tertangkap karena
 *   berada di luar blok {@code try} yang menangani pencarian.</li>
 *   <li><b>Kesalahan eja konstanta.</b> {@code ANAK_DEFULT} (seharusnya {@code ANAK_DEFAULT}),
 *   {@code private static} non-final, dapat diubah dari luar paket melalui refleksi. Nilainya
 *   {@code "{}"}.</li>
 *   <li><b>{@code @SuppressWarnings("unchecked")} yang tidak perlu</b> pada
 *   {@link #ambilAnakSiswa()}, {@link #ambilAnakMahasiswa()}, dan
 *   {@link #ambilAnakMahasiswaObject()} &mdash; ketiganya tidak melakukan operasi <i>unchecked</i>
 *   apa pun; sisa salin-tempel.</li>
 *   <li><b>Tidak ada {@code ambilAnakSiswaObject()}.</b> Padanan object hanya disediakan untuk
 *   mahasiswa, sehingga pemanggil sisi sekolah harus memuat sendiri object {@code Siswa} dari daftar
 *   id.</li>
 *   <li><b>Tiga blok {@code catch} kosong</b> pada method {@code ambilAnak*} (bertanda
 *   {@code auto-audit(empty-catch)} dari inisiatif audit lain). JSON rusak membuat method
 *   mengembalikan daftar kosong tanpa keluhan &mdash; pada portal orang tua itu berarti wali
 *   mendadak "tidak punya anak" dan seluruh datanya menghilang dari layar, bukan pesan error.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} tidak bisa mengosongkan
 *   nilai</b> &mdash; keduanya langsung {@code return} bila argumennya {@code null}/kosong.</li>
 *   <li><b>Relasi ke {@link Pegawai} dua arah lewat dua kolom terpisah</b>
 *   ({@code orang_tua.pegawai} dan {@code pegawai.orang_tua}), keduanya {@code @ManyToOne} dan
 *   tanpa {@code mappedBy}. Keselarasannya dijaga manual oleh {@code OrangTuaAction} saat menyimpan;
 *   pembaruan lewat jalur lain dapat meninggalkan pasangan yang tidak sinkron. {@link Tbmuser}
 *   memperumitnya lagi: {@code Tbmuser.getOrangTua()} <b>menimpa</b> nilai kolomnya sendiri dengan
 *   {@code pegawai.getOrangTua()} bila akun itu terhubung ke pegawai.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see BiodataMahasiswa
 * @see Mahasiswa#getOrangTua()
 * @see Tbmuser#getOrangTua()
 * @see Pegawai#getOrangTua()
 * @see ais.database.model.sekolah.Siswa#getOrangTua()
 * @see ais.action.master.OrangTuaAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "orang_tua")
public class OrangTua extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama dengan yang dipakai puluhan entity model AIS lain
	 * (konstanta bawaan generator) &mdash; tidak masalah karena mekanisme serialisasi juga
	 * mencocokkan nama kelas. Entity ini ikut diserialkan ke session ZK dan cache berkas, jadi
	 * jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;

	/**
	 * Bendera anti-rekursi untuk {@link #getPropinsi()}. Bersifat {@code transient} (tidak ikut
	 * diserialkan, bukan kolom) dan hanya bernilai {@code true} selama pemanggilan berlangsung;
	 * pemanggilan bersarang pada instance yang sama langsung mengembalikan field {@code propinsi}
	 * apa adanya tanpa mengulang penelusuran wilayah yang mahal.
	 */
	private transient boolean resolvingPropinsi;

	/**
	 * Bendera anti-rekursi untuk {@link #getKota()}. Lihat {@code resolvingPropinsi}.
	 */
	private transient boolean resolvingKota;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris orang tua ini (jejak audit ringan).
	 * Membayangi properti bernama sama di {@link GeneralValueObject}.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong diabaikan</b>
	 * (method langsung keluar), sehingga jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong diabaikan</b>
	 * (method langsung keluar), sehingga jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris orang tua ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE},
	 * dan mendelegasikan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) ke {@code AuditTimestampInterceptor.ubah(this)}. Ini implementasi
	 * satu-satunya method {@code abstract} milik {@link GeneralValueObject}; jangan dipanggil manual.
	 *
	 * <p>Perhatikan deklarasi field {@code tanggal_dirubah} menumpang di baris yang sama dengan
	 * method ini &mdash; format bawaan penyunting massal, bukan kesengajaan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir baris ini. Normalnya diisi otomatis oleh {@link #onUpdate()};
	 * pemanggilan manual hanya dipakai saat migrasi/impor data.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini. Nilai awalnya diisi waktu pembuatan object
	 * ({@code WaktuUtil.getDate()}), bukan {@code null}.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris orang tua ini, berbentuk {@code "<id>-<keterangan>"}.
	 *
	 * <p>Method ini membaca <i>field</i> {@code keterangan} secara langsung dan tidak menormalkan
	 * {@code null}, sehingga baris tanpa keterangan tampil sebagai {@code "123-null"}. Karena kelas
	 * ini tidak pernah mengisi properti {@code nama} milik {@link GeneralValueObject}, string inilah
	 * yang muncul di combobox dan daftar generik. Tidak ada relasi lazy yang disentuh, jadi aman
	 * dipanggil pada entity yang sudah <i>detached</i>.</p>
	 *
	 * @return {@code "<id>-<keterangan>"}
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	private String alamat;
	private String nikAyah;
	private String namaAyah;
	private Date tanggalLahirAyah;
	private Pekerjaan jenisPekerjaanAyah;
	private Penghasilan jenisPenghasilanAyah;
	private Penghasilan jenisPenghasilanIbu;
	private Penghasilan jenisPenghasilanWali;

	private Jenjang jenjangPendidikanAyah;
	private Jenjang jenjangPendidikanIbu;
	private Jenjang jenjangPendidikanWali;

	private String emailAyah;

	private String namaWali;
	private Date tanggalLahirWali;
	private Pekerjaan jenisPekerjaanIbu;
	private Pekerjaan jenisPekerjaanWali;
	private PendapatanOrangTua pendapatanWali;

	private String namaIbu;
	private Date tanggalLahirIbu;
	private String emailIbu;

	private String nikIbu;
	private String nikWali;

	private String telpAyah, telpIbu, telpWali;

	private String keterangan;
	private Boolean aktif;
	private Kota kota;
	private Propinsi propinsi;
	private Wilayah kecamatan;
	private String kelurahan;
	private String kodepos;
	private String rw;
	private String rt;
	private String noKK;
	private String dusun;
	private Pegawai pegawai;
	private String anak;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang langsung diisi waktu sekarang.
	 */
	public OrangTua() {
	}

	/**
	 * Kunci utama baris orang tua ini (kolom {@code id}, {@code IDENTITY}/sequence PostgreSQL).
	 * Dipakai seluruh kontrak {@code equals}/{@code hashCode}/{@code check()} di
	 * {@link GeneralValueObject}.
	 *
	 * @return id baris; {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Normalnya hanya dipanggil Hibernate; pengisian manual dipakai saat
	 * membuat object penunjuk ({@code new OrangTua(id)}-style) untuk keperluan pencarian.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Alamat rumah keluarga (kolom {@code alamat}), yaitu jalan/nomor saja &mdash; dusun, RT, RW,
	 * kelurahan, kecamatan, kota, propinsi, dan kode pos berada di properti terpisah.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method <b>menulis {@code ""} ke
	 * field</b> sebelum mengembalikannya. Karena entity memakai <i>property access</i> dan
	 * {@code dynamicUpdate = true}, string kosong itu bisa ikut tersimpan pada flush berikutnya.
	 * Hasilnya selalu sudah di-{@code trim} dan tidak pernah {@code null}.</p>
	 *
	 * @return alamat yang sudah dipangkas spasi; {@code ""} bila belum diisi
	 */
	@Column(name = "alamat")
	public String getAlamat() {
		if (alamat == null) {
			alamat = "";
		}
		return this.alamat.trim();
	}

	/**
	 * Menyetel alamat rumah keluarga. Tanpa validasi; {@code null} akan terbaca sebagai {@code ""}.
	 *
	 * @param alamat alamat rumah
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Nama lengkap ayah (kolom {@code nama_ayah}, maksimal 100 karakter). Dikembalikan apa adanya,
	 * tanpa normalisasi &mdash; berbeda dari {@link BiodataMahasiswa#getNamaIbu()} yang memangkas
	 * dan membersihkan nilainya.
	 *
	 * @return nama ayah, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ayah", length = 100)
	public String getNamaAyah() {
		return this.namaAyah;
	}

	/**
	 * Menyetel nama lengkap ayah.
	 *
	 * @param namaAyah nama ayah (maksimal 100 karakter di basis data)
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Nama lengkap ibu (kolom {@code nama_ibu}, maksimal 100 karakter). Dikembalikan apa adanya,
	 * tanpa normalisasi.
	 *
	 * @return nama ibu, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ibu", length = 100)
	public String getNamaIbu() {
		return this.namaIbu;
	}

	/**
	 * Menyetel nama lengkap ibu.
	 *
	 * @param namaIbu nama ibu (maksimal 100 karakter di basis data)
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Nama dusun/dukuh alamat rumah. Tanpa anotasi {@code @Column}, sehingga nama kolom fisiknya
	 * {@code dusun} (sama saja).
	 *
	 * <p><b>Efek samping:</b> menulis {@code ""} ke field bila masih {@code null}, sehingga string
	 * kosong bisa ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @return nama dusun; {@code ""} bila belum diisi, tidak pernah {@code null}
	 */
	public String getDusun() {
		if (dusun == null) {
			dusun = "";
		}
		return dusun;
	}

	/**
	 * Menyetel nama dusun/dukuh.
	 *
	 * @param dusun nama dusun
	 */
	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	/**
	 * Nomor Kartu Keluarga (kolom {@code no_kk}). Dikembalikan apa adanya &mdash; tidak dibersihkan
	 * dari tanda hubung/spasi, berbeda dari {@link #getNikAyah()} yang juga tidak dibersihkan tetapi
	 * berbeda dari penormalan agresif yang dilakukan {@link BiodataMahasiswa} pada kolom sejenis.
	 *
	 * @return nomor KK, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_kk")
	public String getNoKK() {
		return noKK;
	}

	/**
	 * Menyetel nomor Kartu Keluarga.
	 *
	 * @param noKK nomor KK
	 */
	public void setNoKK(String noKK) {
		this.noKK = noKK;
	}

	/**
	 * Nomor RT alamat rumah, <b>dinormalkan saat dibaca</b>.
	 *
	 * <p>Urutan pembersihan: bila nilainya bukan angka menurut {@code Common.isNumber(String)},
	 * seluruh karakter selain digit dan titik dibuang; {@code null} menjadi {@code ""}; nilai
	 * sentinel {@code "00"} (dipakai data lama untuk "tidak ada") menjadi {@code ""}; terakhir
	 * seluruh tanda hubung dibuang.</p>
	 *
	 * <p><b>Efek samping:</b> setiap tahap menulis hasilnya <b>kembali ke field</b>, jadi sekadar
	 * membaca RT dapat mengubah isi baris di basis data pada flush berikutnya. Perhatikan juga titik
	 * ({@code .}) sengaja dipertahankan oleh regex, sehingga nilai seperti {@code "0.3"} lolos.</p>
	 *
	 * @return nomor RT yang sudah dibersihkan; {@code ""} bila kosong atau bernilai sentinel
	 */
	public String getRt() {

		if (rt != null && !Common.isNumber(rt)) {
			rt = rt.replaceAll("[^\\d.]", "");
		}

		if (rt == null) {
			rt = "";
		}

		if (rt.equalsIgnoreCase("00")) {
			rt = "";
		}

		rt = org.apache.commons.lang3.StringUtils.replace(rt, "-", "");

		return rt;
	}

	/**
	 * Menyetel nomor RT. Nilai disimpan mentah; pembersihan baru terjadi saat dibaca lewat
	 * {@link #getRt()}.
	 *
	 * @param rt nomor RT
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Nomor RW alamat rumah, dinormalkan saat dibaca dengan aturan yang identik dengan
	 * {@link #getRt()} (termasuk efek samping penulisan balik ke field).
	 *
	 * @return nomor RW yang sudah dibersihkan; {@code ""} bila kosong atau bernilai sentinel
	 *         {@code "00"}
	 * @see #getRt()
	 */
	public String getRw() {

		if (rw != null && !Common.isNumber(rw)) {
			rw = rw.replaceAll("[^\\d.]", "");
		}

		if (rw == null) {
			rw = "";
		}

		if (rw.equalsIgnoreCase("00")) {
			rw = "";
		}

		rw = org.apache.commons.lang3.StringUtils.replace(rw, "-", "");

		return rw;
	}

	/**
	 * Menyetel nomor RW. Nilai disimpan mentah; pembersihan baru terjadi saat dibaca.
	 *
	 * @param rw nomor RW
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Kode pos alamat rumah, dinormalkan saat dibaca.
	 *
	 * <p>Aturannya sama dengan {@link #getRt()} kecuali pada pemeriksaan sentinel: di sini yang
	 * dibuang adalah nilai yang <b>mengandung</b> {@code "0000"} di mana pun (bukan sama dengan
	 * {@code "00"}), sehingga kode pos sah seperti {@code "40000"} atau {@code "00001"} ikut
	 * dihapus menjadi {@code ""}. Hasil akhirnya di-{@code trim}.</p>
	 *
	 * <p><b>Efek samping:</b> hasil pembersihan ditulis balik ke field, jadi penghapusan tersebut
	 * bersifat permanen setelah flush berikutnya.</p>
	 *
	 * @return kode pos yang sudah dibersihkan; {@code ""} bila kosong atau mengandung {@code "0000"}
	 */
	public String getKodepos() {

		if (kodepos != null && !Common.isNumber(kodepos)) {
			kodepos = kodepos.replaceAll("[^\\d.]", "");
		}

		if (kodepos == null) {
			kodepos = "";
		}

		if (kodepos.contains("0000")) {
			kodepos = "";
		}

		kodepos = org.apache.commons.lang3.StringUtils.replace(kodepos, "-", "");

		return kodepos.trim();
	}

	/**
	 * Menyetel kode pos. Nilai disimpan mentah; pembersihan baru terjadi saat dibaca.
	 *
	 * @param kodepos kode pos
	 */
	public void setKodepos(String kodepos) {
		this.kodepos = kodepos;
	}

	/**
	 * Nama kelurahan/desa alamat rumah.
	 *
	 * <p><b>Efek samping:</b> bila kosong atau hanya spasi, field <b>diisi {@code "-"}</b> lalu
	 * dikembalikan. Ini membuat formulir cetak dan berkas ekspor tidak pernah tampil kosong, tetapi
	 * juga berarti "belum diisi" tersimpan sebagai tanda hubung di basis data setelah flush
	 * berikutnya &mdash; dan pemeriksaan kelengkapan berbasis getter tidak akan pernah menganggap
	 * kolom ini kosong. Perilaku identik dengan {@link BiodataMahasiswa#getKelurahan()}.</p>
	 *
	 * @return nama kelurahan yang sudah di-{@code trim}, atau {@code "-"} bila belum diisi; tidak
	 *         pernah {@code null}
	 */
	public String getKelurahan() {
		if (kelurahan == null || kelurahan.trim().isEmpty()) {
			kelurahan = "-";
		}
		return kelurahan.trim();
	}

	/**
	 * Menyetel nama kelurahan/desa.
	 *
	 * @param kelurahan nama kelurahan; {@code null}/kosong akan terbaca sebagai {@code "-"}
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * Kecamatan tempat tinggal keluarga menurut pohon wilayah PDDikti/Feeder (kolom
	 * {@code kecamatan_wilayah}).
	 *
	 * <p>{@link Wilayah} adalah tabel berjenjang propinsi &rarr; kabupaten/kota &rarr; kecamatan yang
	 * dihubungkan lewat {@code wilayahInduk}, dan tiap simpul punya kode {@code feeder}. Kolom ini
	 * menunjuk simpul tingkat kecamatan sehingga kabupaten dan propinsi dapat ditelusuri ke atas
	 * &mdash; itulah yang dimanfaatkan {@link #getKota()} dan {@link #getPropinsi()}.</p>
	 *
	 * <p><b>Perbaikan otomatis data cacat.</b> Bila simpul yang tersimpan ternyata tidak punya induk
	 * (baris wilayah yatim), method memindai SELURUH cache {@link Wilayah}
	 * ({@code ConstantValues.ambilBerdasarClass(...)}) mencari simpul lain dengan kode
	 * {@code feeder} sama yang punya induk, lalu <b>mengganti referensi</b> ke simpul itu.</p>
	 *
	 * <p><b>Efek samping &amp; biaya:</b> field {@code kecamatan} ditulis ulang sehingga penggantian
	 * bisa ikut tersimpan pada flush berikutnya; pemindaian menelusuri puluhan ribu baris cache
	 * setiap kali kondisi anomali terpenuhi, dan biaya itu menular ke {@link #getKota()} serta
	 * {@link #getPropinsi()} yang memanggilnya di awal. Berbeda dari versi di
	 * {@link BiodataMahasiswa#getKecamatan()}, di sini kecocokan {@code feeder} diperiksa di dalam
	 * loop (tanpa penjagaan {@code feeder != null} di awal) dan hasil cache tidak diperiksa
	 * {@code null} lebih dulu.</p>
	 *
	 * @return simpul wilayah tingkat kecamatan, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_wilayah", nullable = true)
	public Wilayah getKecamatan() {
		kecamatan = check(kecamatan);

		if (kecamatan != null && kecamatan.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatan.getFeeder() != null
						&& kecamatan.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatan = w;
					break;
				}
			}

		}

		return kecamatan;
	}

	/**
	 * Menyetel kecamatan tempat tinggal.
	 *
	 * @param kecamatan simpul {@link Wilayah} tingkat kecamatan
	 */
	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * Propinsi alamat rumah (kolom {@code propinsi}) &mdash; <b>getter paling berat di kelas ini;
	 * dapat menulis ke basis data.</b>
	 *
	 * <p>Alurnya:</p>
	 * <ol>
	 *   <li>bila pemanggilan sedang berlangsung untuk instance yang sama ({@code resolvingPropinsi}),
	 *   langsung mengembalikan field apa adanya &mdash; penjaga anti-rekursi;</li>
	 *   <li>{@code check(propinsi)} untuk memulihkan proxy lazy yang sudah <i>detached</i>, lalu
	 *   {@link #getKecamatan()} dipanggil (dengan seluruh biayanya) dan hasilnya ditulis ke
	 *   field;</li>
	 *   <li>bila {@code kota} sudah terisi, propinsinya diambil dari kota itu &mdash; kota menang
	 *   atas nilai kolom sendiri. <b>Catatan:</b> di sini dipakai <i>field</i> {@code kota}
	 *   langsung, bukan {@link #getKota()} dan bukan {@code check(kota)}, sehingga proxy detached
	 *   dapat melempar {@code LazyInitializationException} yang <b>tidak</b> tertangkap
	 *   {@code catch} di dalam;</li>
	 *   <li>bila propinsi masih kosong tetapi kecamatan punya induk, method <b>membuka session
	 *   Hibernate baru</b> ({@code FlushMode.MANUAL}), menaiki pohon wilayah dua tingkat untuk
	 *   mendapat nama propinsi, membuang awalan {@code "Prop."}, lalu mencocokkannya dengan seluruh
	 *   baris {@link Propinsi} memakai jarak Levenshtein. Kandidat terdekat dipakai bila jaraknya
	 *   &lt; 2; <b>bila tidak ada yang cukup mirip, baris {@link Propinsi} BARU dibuat dan
	 *   di-{@code commit}</b> dengan negara {@link ConstantValues#INDONESIA}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> field {@code propinsi} dan {@code kecamatan} ditulis ulang (bisa ikut
	 * tersimpan saat flush), dan tabel master {@code propinsi} dapat bertambah baris hanya karena
	 * seseorang membuka layar data orang tua. Session yang dibuka di sini ditutup tuntas di
	 * {@code finally} lewat {@code HibernateUtil.closeSessionQuietly(session)} sehingga session
	 * milik request ZK tidak ikut tertutup. Kegagalan apa pun ditelan dan hanya dicetak ke
	 * {@code System.err}.</p>
	 *
	 * @return propinsi alamat rumah, atau {@code null} bila tidak dapat ditentukan
	 * @see #getKota()
	 * @see BiodataMahasiswa#getPropinsi()
	 */
	@SuppressWarnings("unchecked")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = true)
	public Propinsi getPropinsi() {

		if (resolvingPropinsi) {
			return propinsi;
		}
		resolvingPropinsi = true;
		try {
		propinsi = check(propinsi);
		kecamatan = getKecamatan();

		if (kota != null && kota.getPropinsi() != null) {
			propinsi = kota.getPropinsi();
		}

		else if (propinsi == null && kecamatan != null && kecamatan.getWilayahInduk() != null) {
			Session session = HibernateUtil.openSession();
			try {
				session.setFlushMode(org.hibernate.FlushMode.MANUAL);
				Wilayah wilayahKab = kecamatan.getWilayahInduk();
				Wilayah wilayahProp = wilayahKab == null ? null : wilayahKab.getWilayahInduk();

				String namaProp = wilayahProp == null ? "" : wilayahProp.getNama();
				Propinsi selectedPropinsi = null;

				if (namaProp != null && !namaProp.trim().isEmpty()) {
					namaProp = org.apache.commons.lang3.StringUtils.replace(namaProp, "Prop.", "");
					namaProp = namaProp.trim();
					List<Propinsi> propinsis = session.createCriteria(Propinsi.class).add(Restrictions.ne("nama", ""))
							.add(Restrictions.isNotNull("nama")).list();
					TreeMap<Integer, Propinsi> treeMap = new TreeMap<Integer, Propinsi>();
					for (Propinsi propinsi : propinsis) {
						String nama = propinsi.getNama();
						nama = org.apache.commons.lang3.StringUtils.replace(nama, "Prop.", "");
						nama = nama.trim();
						treeMap.put(StringUtils.getLevenshteinDistance(nama.toLowerCase(), namaProp.toLowerCase()),
								propinsi);
					}
					int firstKey = treeMap.isEmpty() ? 0 : treeMap.firstKey();
					if (!treeMap.isEmpty() && firstKey < 2) {
						selectedPropinsi = treeMap.get(firstKey);
					} else {
						selectedPropinsi = new Propinsi();
						selectedPropinsi.setNama(namaProp);
						selectedPropinsi.setNegara(ConstantValues.INDONESIA);
						session.getTransaction().begin();
						session.save(selectedPropinsi);
						session.getTransaction().commit();
					}

					propinsi = selectedPropinsi;
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				// Tutup TUNTAS session yang DIBUKA di sini (clear+disconnect+close). Sebelumnya no-arg
				// closeSession() menutup session ThreadLocal, BUKAN objek hasil openSession() -> BOCOR.
				HibernateUtil.closeSessionQuietly(session);
			}
		}

		return propinsi;
		} finally {
			resolvingPropinsi = false;
		}
	}

	/**
	 * Menyetel propinsi alamat rumah. Perlu diingat {@link #getPropinsi()} dapat <b>menimpa</b> nilai
	 * ini dengan propinsi milik {@link #getKota()} bila kota terisi.
	 *
	 * @param propinsi propinsi alamat rumah
	 */
	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * Kota/kabupaten alamat rumah (kolom {@code kota}) &mdash; <b>getter berat; dapat menulis ke
	 * basis data.</b>
	 *
	 * <p>Alurnya sejajar dengan {@link #getPropinsi()}: penjaga anti-rekursi {@code resolvingKota},
	 * {@code check(kota)}, lalu {@link #getKecamatan()}. Bila kota masih kosong tetapi kecamatan
	 * punya induk, method membuka session Hibernate sendiri ({@code FlushMode.MANUAL}) dan
	 * menjalankan <b>dua</b> pencocokan Levenshtein berurutan:</p>
	 * <ol>
	 *   <li>nama propinsi (dua tingkat di atas kecamatan, awalan {@code "Prop."} dibuang) &rarr;
	 *   baris {@link Propinsi}; <b>bila tidak ada yang cukup mirip, baris propinsi BARU dibuat dan
	 *   di-{@code commit}</b> &mdash; salinan persis blok yang sama di {@link #getPropinsi()};</li>
	 *   <li>nama kabupaten/kota (awalan {@code "Kab."} dan {@code "Kota"} dibuang) dicari di antara
	 *   {@link Kota} yang berada di bawah propinsi hasil langkah 1. Kota hanya dipakai bila jarak
	 *   terdekatnya &lt; 2; bila tidak, field {@code kota} <b>dibiarkan {@code null}</b> sehingga
	 *   seluruh penelusuran mahal ini terulang pada setiap pembacaan berikutnya.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> selain {@code kota} dan {@code kecamatan}, method ini juga menulis ke
	 * field <b>{@code propinsi}</b> &mdash; membaca kota dapat mengubah propinsi. Tabel master
	 * {@code propinsi} dapat bertambah baris. Session yang dibuka ditutup tuntas di {@code finally}
	 * lewat {@code HibernateUtil.closeSessionQuietly(session)}, jadi session request ZK aman.</p>
	 *
	 * <p><b>Kuirk:</b> bila nama propinsi hasil penelusuran kosong, {@code selectedPropinsi} tetap
	 * {@code null} tetapi pencarian kota tetap dijalankan dengan
	 * {@code Restrictions.eq("propinsi", null)} &mdash; kriteria yang di SQL menjadi
	 * {@code propinsi = null} dan tidak pernah cocok, sehingga hasilnya selalu daftar kosong.</p>
	 *
	 * @return kota/kabupaten alamat rumah, atau {@code null} bila tidak dapat ditentukan
	 * @see #getPropinsi()
	 * @see BiodataMahasiswa#getKota()
	 */
	@SuppressWarnings("unchecked")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {

		if (resolvingKota) {
			return kota;
		}
		resolvingKota = true;
		try {
		kota = check(kota);
		kecamatan = getKecamatan();

		if (kota == null && kecamatan != null && kecamatan.getWilayahInduk() != null) {
			Session session = HibernateUtil.openSession();
			try {
				session.setFlushMode(org.hibernate.FlushMode.MANUAL);
				Wilayah wilayahKab = kecamatan.getWilayahInduk();
				Wilayah wilayahProp = wilayahKab == null ? null : wilayahKab.getWilayahInduk();

				String namaProp = wilayahProp == null ? "" : wilayahProp.getNama();
				Propinsi selectedPropinsi = null;

				if (namaProp != null && !namaProp.trim().isEmpty()) {
					namaProp = org.apache.commons.lang3.StringUtils.replace(namaProp, "Prop.", "");
					namaProp = namaProp.trim();
					List<Propinsi> propinsis = session.createCriteria(Propinsi.class).add(Restrictions.ne("nama", ""))
							.add(Restrictions.isNotNull("nama")).list();
					TreeMap<Integer, Propinsi> treeMap = new TreeMap<Integer, Propinsi>();
					for (Propinsi propinsi : propinsis) {
						String nama = propinsi.getNama();
						nama = org.apache.commons.lang3.StringUtils.replace(nama, "Prop.", "");
						nama = nama.trim();
						treeMap.put(StringUtils.getLevenshteinDistance(nama.toLowerCase(), namaProp.toLowerCase()),
								propinsi);
					}
					int firstKey = treeMap.isEmpty() ? 0 : treeMap.firstKey();
					if (!treeMap.isEmpty() && firstKey < 2) {
						selectedPropinsi = treeMap.get(firstKey);
					} else {
						selectedPropinsi = new Propinsi();
						selectedPropinsi.setNama(namaProp);
						selectedPropinsi.setNegara(ConstantValues.INDONESIA);
						session.getTransaction().begin();
						session.save(selectedPropinsi);
						session.getTransaction().commit();
					}

					propinsi = selectedPropinsi;
				}

				List<Kota> kotas = session.createCriteria(Kota.class).add(Restrictions.eq("propinsi", selectedPropinsi))
						.add(Restrictions.ne("nama", "")).add(Restrictions.isNotNull("nama")).list();

				String namaKab = wilayahKab.getNama();
				namaKab = org.apache.commons.lang3.StringUtils.replace(namaKab, "Kab.", "");
				namaKab = org.apache.commons.lang3.StringUtils.replace(namaKab, "Kota", "");
				namaKab = namaKab.trim();

				TreeMap<Integer, Kota> treeMap = new TreeMap<Integer, Kota>();
				for (Kota kota : kotas) {
					String nama = kota.getNama();
					nama = org.apache.commons.lang3.StringUtils.replace(nama, "Kab.", "");
					nama = org.apache.commons.lang3.StringUtils.replace(nama, "Kota", "");
					nama = nama.trim();
					treeMap.put(StringUtils.getLevenshteinDistance(nama.toLowerCase(), namaKab.toLowerCase()), kota);
				}
				int firstKey = treeMap.isEmpty() ? 0 : treeMap.firstKey();
				if (!treeMap.isEmpty() && firstKey < 2) {
					kota = treeMap.get(treeMap.firstKey());
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				// Tutup TUNTAS session yang DIBUKA di sini (clear+disconnect+close). Sebelumnya no-arg
				// closeSession() menutup session ThreadLocal, BUKAN objek hasil openSession() -> BOCOR.
				HibernateUtil.closeSessionQuietly(session);
			}
		}
		return kota;
		} finally {
			resolvingKota = false;
		}
	}

	/**
	 * Menyetel kota/kabupaten alamat rumah. Nilai yang diisi di sini <b>menang</b> atas kolom
	 * propinsi: {@link #getPropinsi()} akan mengambil propinsi dari kota ini bila terisi.
	 *
	 * @param kota kota/kabupaten alamat rumah
	 */
	public void setKota(Kota kota) {
		this.kota = kota;
	}

	/**
	 * Keterangan bebas untuk baris keluarga ini (kolom {@code keterangan}) &mdash; dalam praktik
	 * dipakai sebagai label/nama keluarga, karena {@link #toString()} menyusun tampilannya dari
	 * kolom ini.
	 *
	 * <p><b>Membayangi {@link GeneralValueObject#getKeterangan()} dengan perilaku yang berbeda:</b>
	 * versi induk menormalkan {@code null} menjadi {@code ""}, versi ini tidak. Akibatnya cabang
	 * {@code keterangan} pada {@link GeneralValueObject#compareTo(GeneralValueObject)} &mdash; yang
	 * di induk dijamin selalu terpakai &mdash; bisa gugur untuk entity ini, dan dua baris tanpa
	 * keterangan dianggap setara saat diurutkan.</p>
	 *
	 * @return keterangan keluarga, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/label keluarga. Tanpa validasi.
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda baris masih aktif dipakai. Bernilai {@code true} bila kolom masih {@code null},
	 * sehingga data lama yang belum pernah menyentuh kolom ini otomatis dianggap aktif.
	 *
	 * <p>Berbeda dari getter default lain di kelas ini, nilai bawaan {@code true} <b>tidak</b>
	 * ditulis balik ke field &mdash; jadi getter ini tidak berefek ke basis data.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengaktifkan/menonaktifkan baris keluarga ini.
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} akan terbaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Tanggal lahir ayah (kolom {@code tanggalLahirAyah}, tipe {@code DATE} tanpa jam).
	 *
	 * @return tanggal lahir ayah, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirAyah() {
		return tanggalLahirAyah;
	}

	/**
	 * Menyetel tanggal lahir ayah.
	 *
	 * @param tanggalLahirAyah tanggal lahir ayah
	 */
	public void setTanggalLahirAyah(Date tanggalLahirAyah) {
		this.tanggalLahirAyah = tanggalLahirAyah;
	}

	/**
	 * Nama lengkap wali (pihak ketiga yang menanggung anak bila ayah/ibu tidak berperan). Tanpa
	 * anotasi {@code @Column}, jadi nama kolom fisiknya {@code namaWali} (camelCase) &mdash; berbeda
	 * gaya dari {@code nama_ayah}/{@code nama_ibu}.
	 *
	 * @return nama wali, atau {@code null} bila belum diisi
	 */
	public String getNamaWali() {
		return namaWali;
	}

	/**
	 * Menyetel nama lengkap wali.
	 *
	 * @param namaWali nama wali
	 */
	public void setNamaWali(String namaWali) {
		this.namaWali = namaWali;
	}

	/**
	 * Tanggal lahir wali (tipe {@code DATE} tanpa jam).
	 *
	 * @return tanggal lahir wali, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirWali() {
		return tanggalLahirWali;
	}

	/**
	 * Menyetel tanggal lahir wali.
	 *
	 * @param tanggalLahirWali tanggal lahir wali
	 */
	public void setTanggalLahirWali(Date tanggalLahirWali) {
		this.tanggalLahirWali = tanggalLahirWali;
	}

	/**
	 * Rentang pendapatan wali menurut daftar acuan {@link PendapatanOrangTua} (kolom
	 * {@code pendapatan_wali}).
	 *
	 * <p><b>Asimetri yang disengaja atau tidak:</b> hanya wali yang punya properti
	 * {@link PendapatanOrangTua} di entity ini; ayah dan ibu hanya punya {@link Penghasilan}
	 * ({@link #getJenisPenghasilanAyah()}, {@link #getJenisPenghasilanIbu()}). Bandingkan dengan
	 * {@link BiodataMahasiswa} yang menyediakan {@code pendapatanOrtu} (ayah),
	 * {@code pendapatanOrtuIbu} (ibu), <i>dan</i> {@code pendapatanWali}.</p>
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu agar proxy lazy yang sudah
	 * <i>detached</i> tetap terpakai tanpa {@code LazyInitializationException}; hasilnya ditulis
	 * balik ke field.</p>
	 *
	 * @return acuan rentang pendapatan wali, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_wali", nullable = true)
	public PendapatanOrangTua getPendapatanWali() {
		pendapatanWali = check(pendapatanWali);
		return pendapatanWali;
	}

	/**
	 * Menyetel rentang pendapatan wali.
	 *
	 * @param pendapatanWali acuan {@link PendapatanOrangTua}
	 */
	public void setPendapatanWali(PendapatanOrangTua pendapatanWali) {
		this.pendapatanWali = pendapatanWali;
	}

	/**
	 * Tanggal lahir ibu (tipe {@code DATE} tanpa jam).
	 *
	 * @return tanggal lahir ibu, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirIbu() {
		return tanggalLahirIbu;
	}

	/**
	 * Menyetel tanggal lahir ibu.
	 *
	 * @param tanggalLahirIbu tanggal lahir ibu
	 */
	public void setTanggalLahirIbu(Date tanggalLahirIbu) {
		this.tanggalLahirIbu = tanggalLahirIbu;
	}

	/**
	 * Alamat surel ayah, dipakai pengiriman pengumuman dan tagihan. Tidak divalidasi bentuknya di
	 * lapisan entity. Wali tidak punya padanan kolom ini.
	 *
	 * @return alamat surel ayah, atau {@code null} bila belum diisi
	 */
	public String getEmailAyah() {
		return emailAyah;
	}

	/**
	 * Menyetel alamat surel ayah.
	 *
	 * @param emailAyah alamat surel ayah
	 */
	public void setEmailAyah(String emailAyah) {
		this.emailAyah = emailAyah;
	}

	/**
	 * Jenjang pendidikan terakhir ayah menurut daftar acuan {@link Jenjang} (kolom
	 * {@code jenjang_pendidikan_ayah}).
	 *
	 * <p>Perhatikan {@link BiodataMahasiswa} memakai <b>dua</b> acuan berbeda untuk hal yang sama
	 * ({@link Jenjang} dan {@link PendidikanOrangTua}); entity ini hanya memakai {@link Jenjang}.
	 * Nilai di kedua entity tidak pernah disinkronkan.</p>
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu dan menulis hasilnya balik ke
	 * field.</p>
	 *
	 * @return jenjang pendidikan ayah, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_ayah", nullable = true)
	public Jenjang getJenjangPendidikanAyah() {
		jenjangPendidikanAyah = check(jenjangPendidikanAyah);
		return jenjangPendidikanAyah;
	}

	/**
	 * Menyetel jenjang pendidikan terakhir ayah.
	 *
	 * @param jenjangPendidikanAyah acuan {@link Jenjang}
	 */
	public void setJenjangPendidikanAyah(Jenjang jenjangPendidikanAyah) {
		this.jenjangPendidikanAyah = jenjangPendidikanAyah;
	}

	/**
	 * Jenjang pendidikan terakhir ibu menurut daftar acuan {@link Jenjang} (kolom
	 * {@code jenjang_pendidikan_ibu}). Memanggil {@link GeneralValueObject#check(Object)} lebih dulu
	 * dan menulis hasilnya balik ke field.
	 *
	 * @return jenjang pendidikan ibu, atau {@code null} bila belum diisi
	 * @see #getJenjangPendidikanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_ibu", nullable = true)
	public Jenjang getJenjangPendidikanIbu() {
		jenjangPendidikanIbu = check(jenjangPendidikanIbu);
		return jenjangPendidikanIbu;
	}

	/**
	 * Menyetel jenjang pendidikan terakhir ibu.
	 *
	 * @param jenjangPendidikanIbu acuan {@link Jenjang}
	 */
	public void setJenjangPendidikanIbu(Jenjang jenjangPendidikanIbu) {
		this.jenjangPendidikanIbu = jenjangPendidikanIbu;
	}

	/**
	 * Jenis pekerjaan ayah menurut daftar acuan {@link Pekerjaan} (kolom
	 * {@code jenis_pekerjaan_ayah}).
	 *
	 * <p><b>Bukan</b> {@link PekerjaanOrangTua} &mdash; itu acuan terpisah yang hanya dipakai
	 * {@link BiodataMahasiswa}. Kedua daftar acuan itu berisi kategori yang mirip tetapi tidak
	 * identik dan tidak punya pemetaan resmi satu sama lain, sehingga nilai dari dua layar berbeda
	 * tidak bisa dibandingkan langsung.</p>
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu dan menulis hasilnya balik ke
	 * field.</p>
	 *
	 * @return jenis pekerjaan ayah, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_ayah", nullable = true)
	public Pekerjaan getJenisPekerjaanAyah() {
		jenisPekerjaanAyah = check(jenisPekerjaanAyah);
		return jenisPekerjaanAyah;
	}

	/**
	 * Menyetel jenis pekerjaan ayah.
	 *
	 * @param jenisPekerjaanAyah acuan {@link Pekerjaan}
	 */
	public void setJenisPekerjaanAyah(Pekerjaan jenisPekerjaanAyah) {
		this.jenisPekerjaanAyah = jenisPekerjaanAyah;
	}

	/**
	 * Jenis pekerjaan ibu menurut daftar acuan {@link Pekerjaan} (kolom
	 * {@code jenis_pekerjaan_ibu}). Memanggil {@link GeneralValueObject#check(Object)} lebih dulu dan
	 * menulis hasilnya balik ke field.
	 *
	 * @return jenis pekerjaan ibu, atau {@code null} bila belum diisi
	 * @see #getJenisPekerjaanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_ibu", nullable = true)
	public Pekerjaan getJenisPekerjaanIbu() {
		jenisPekerjaanIbu = check(jenisPekerjaanIbu);
		return jenisPekerjaanIbu;
	}

	/**
	 * Menyetel jenis pekerjaan ibu.
	 *
	 * @param jenisPekerjaanIbu acuan {@link Pekerjaan}
	 */
	public void setJenisPekerjaanIbu(Pekerjaan jenisPekerjaanIbu) {
		this.jenisPekerjaanIbu = jenisPekerjaanIbu;
	}

	/**
	 * Kategori penghasilan ayah menurut daftar acuan {@link Penghasilan} (kolom
	 * {@code jenis_penghasilan_ayah}) &mdash; kategori/rentang, bukan nominal rupiah. Nominalnya
	 * hanya ada di {@link BiodataMahasiswa}. Memanggil {@link GeneralValueObject#check(Object)} lebih
	 * dulu dan menulis hasilnya balik ke field.
	 *
	 * @return kategori penghasilan ayah, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_ayah", nullable = true)
	public Penghasilan getJenisPenghasilanAyah() {
		jenisPenghasilanAyah = check(jenisPenghasilanAyah);
		return jenisPenghasilanAyah;
	}

	/**
	 * Menyetel kategori penghasilan ayah.
	 *
	 * @param jenisPenghasilanAyah acuan {@link Penghasilan}
	 */
	public void setJenisPenghasilanAyah(Penghasilan jenisPenghasilanAyah) {
		this.jenisPenghasilanAyah = jenisPenghasilanAyah;
	}

	/**
	 * Kategori penghasilan ibu menurut daftar acuan {@link Penghasilan} (kolom
	 * {@code jenis_penghasilan_ibu}). Memanggil {@link GeneralValueObject#check(Object)} lebih dulu
	 * dan menulis hasilnya balik ke field.
	 *
	 * @return kategori penghasilan ibu, atau {@code null} bila belum diisi
	 * @see #getJenisPenghasilanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_ibu", nullable = true)
	public Penghasilan getJenisPenghasilanIbu() {
		jenisPenghasilanIbu = check(jenisPenghasilanIbu);
		return jenisPenghasilanIbu;
	}

	/**
	 * Menyetel kategori penghasilan ibu.
	 *
	 * @param jenisPenghasilanIbu acuan {@link Penghasilan}
	 */
	public void setJenisPenghasilanIbu(Penghasilan jenisPenghasilanIbu) {
		this.jenisPenghasilanIbu = jenisPenghasilanIbu;
	}

	/**
	 * Kategori penghasilan wali menurut daftar acuan {@link Penghasilan} (kolom
	 * {@code jenis_penghasilan_wali}). Berdampingan dengan {@link #getPendapatanWali()} yang memakai
	 * acuan berbeda ({@link PendapatanOrangTua}) untuk hal yang sama &mdash; keduanya diisi terpisah
	 * dan tidak saling menyelaraskan.
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu dan menulis hasilnya balik ke
	 * field.</p>
	 *
	 * @return kategori penghasilan wali, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_wali", nullable = true)
	public Penghasilan getJenisPenghasilanWali() {
		jenisPenghasilanWali = check(jenisPenghasilanWali);
		return jenisPenghasilanWali;
	}

	/**
	 * Menyetel kategori penghasilan wali.
	 *
	 * @param jenisPenghasilanWali acuan {@link Penghasilan}
	 */
	public void setJenisPenghasilanWali(Penghasilan jenisPenghasilanWali) {
		this.jenisPenghasilanWali = jenisPenghasilanWali;
	}

	/**
	 * Jenjang pendidikan terakhir wali menurut daftar acuan {@link Jenjang} (kolom
	 * {@code jenjang_pendidikan_wali}). Memanggil {@link GeneralValueObject#check(Object)} lebih dulu
	 * dan menulis hasilnya balik ke field.
	 *
	 * @return jenjang pendidikan wali, atau {@code null} bila belum diisi
	 * @see #getJenjangPendidikanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_wali", nullable = true)
	public Jenjang getJenjangPendidikanWali() {
		jenjangPendidikanWali = check(jenjangPendidikanWali);
		return jenjangPendidikanWali;
	}

	/**
	 * Menyetel jenjang pendidikan terakhir wali.
	 *
	 * @param jenjangPendidikanWali acuan {@link Jenjang}
	 */
	public void setJenjangPendidikanWali(Jenjang jenjangPendidikanWali) {
		this.jenjangPendidikanWali = jenjangPendidikanWali;
	}

	/**
	 * Jenis pekerjaan wali menurut daftar acuan {@link Pekerjaan} (kolom
	 * {@code jenis_pekerjaan_wali}). Memanggil {@link GeneralValueObject#check(Object)} lebih dulu
	 * dan menulis hasilnya balik ke field.
	 *
	 * @return jenis pekerjaan wali, atau {@code null} bila belum diisi
	 * @see #getJenisPekerjaanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_wali", nullable = true)
	public Pekerjaan getJenisPekerjaanWali() {
		jenisPekerjaanWali = check(jenisPekerjaanWali);
		return jenisPekerjaanWali;
	}

	/**
	 * Menyetel jenis pekerjaan wali.
	 *
	 * @param jenisPekerjaanWali acuan {@link Pekerjaan}
	 */
	public void setJenisPekerjaanWali(Pekerjaan jenisPekerjaanWali) {
		this.jenisPekerjaanWali = jenisPekerjaanWali;
	}

	/**
	 * Nomor telepon/HP ayah, dipakai pengiriman pesan dan tagihan. <b>Dinormalkan {@code null}
	 * menjadi {@code ""}</b> tanpa menulis balik ke field (tidak berefek ke basis data), tetapi
	 * nilainya tidak dibersihkan dari spasi/tanda hubung.
	 *
	 * @return nomor telepon ayah; {@code ""} bila belum diisi, tidak pernah {@code null}
	 */
	public String getTelpAyah() {
		return telpAyah == null ? "" : telpAyah;
	}

	/**
	 * Menyetel nomor telepon/HP ayah.
	 *
	 * @param telpAyah nomor telepon ayah
	 */
	public void setTelpAyah(String telpAyah) {
		this.telpAyah = telpAyah;
	}

	/**
	 * Nomor telepon/HP ibu. Dinormalkan {@code null} menjadi {@code ""}, sama seperti
	 * {@link #getTelpAyah()}.
	 *
	 * @return nomor telepon ibu; {@code ""} bila belum diisi, tidak pernah {@code null}
	 */
	public String getTelpIbu() {
		return telpIbu == null ? "" : telpIbu;
	}

	/**
	 * Menyetel nomor telepon/HP ibu.
	 *
	 * @param telpIbu nomor telepon ibu
	 */
	public void setTelpIbu(String telpIbu) {
		this.telpIbu = telpIbu;
	}

	/**
	 * Nomor telepon/HP wali.
	 *
	 * <p><b>Tidak konsisten dengan saudaranya:</b> berbeda dari {@link #getTelpAyah()} dan
	 * {@link #getTelpIbu()} yang mengembalikan {@code ""}, getter ini mengembalikan {@code null} apa
	 * adanya. Kode yang memperlakukan ketiga nomor secara seragam
	 * ({@code getTelpWali().isEmpty()}, penggabungan string, dsb.) akan kena
	 * {@code NullPointerException} pada wali saja.</p>
	 *
	 * @return nomor telepon wali, atau {@code null} bila belum diisi
	 */
	public String getTelpWali() {
		return telpWali;
	}

	/**
	 * Menyetel nomor telepon/HP wali.
	 *
	 * @param telpWali nomor telepon wali
	 */
	public void setTelpWali(String telpWali) {
		this.telpWali = telpWali;
	}

	/**
	 * Alamat surel ibu. Tidak divalidasi bentuknya di lapisan entity.
	 *
	 * @return alamat surel ibu, atau {@code null} bila belum diisi
	 */
	public String getEmailIbu() {
		return emailIbu;
	}

	/**
	 * Menyetel alamat surel ibu.
	 *
	 * @param emailIbu alamat surel ibu
	 */
	public void setEmailIbu(String emailIbu) {
		this.emailIbu = emailIbu;
	}

	/**
	 * NIK (Nomor Induk Kependudukan) ayah, dikembalikan <b>apa adanya</b>.
	 *
	 * <p>Berbeda dari {@link BiodataMahasiswa#getNikAyah()} yang membersihkan karakter non angka dan
	 * membuang nilai sentinel, di sini tidak ada penormalan sama sekali &mdash; pembersihan
	 * sepenuhnya tanggung jawab formulir ({@code OrangTuaAction} memanggil {@code .trim()} sebelum
	 * menyetel).</p>
	 *
	 * @return NIK ayah, atau {@code null} bila belum diisi
	 */
	public String getNikAyah() {
		return nikAyah;
	}

	/**
	 * Menyetel NIK ayah. Tanpa validasi panjang maupun bentuk.
	 *
	 * @param nikAyah NIK ayah
	 */
	public void setNikAyah(String nikAyah) {
		this.nikAyah = nikAyah;
	}

	/**
	 * NIK ibu, dikembalikan apa adanya tanpa penormalan.
	 *
	 * @return NIK ibu, atau {@code null} bila belum diisi
	 * @see #getNikAyah()
	 */
	public String getNikIbu() {
		return nikIbu;
	}

	/**
	 * Menyetel NIK ibu. Tanpa validasi.
	 *
	 * @param nikIbu NIK ibu
	 */
	public void setNikIbu(String nikIbu) {
		this.nikIbu = nikIbu;
	}

	/**
	 * NIK wali, dikembalikan apa adanya tanpa penormalan. Properti ini <b>tidak punya padanan</b> di
	 * {@link BiodataMahasiswa} (di sana hanya ada NIK ayah dan ibu), jadi untuk data NIK wali entity
	 * ini adalah satu-satunya sumber.
	 *
	 * @return NIK wali, atau {@code null} bila belum diisi
	 */
	public String getNikWali() {
		return nikWali;
	}

	/**
	 * Menyetel NIK wali. Tanpa validasi.
	 *
	 * @param nikWali NIK wali
	 */
	public void setNikWali(String nikWali) {
		this.nikWali = nikWali;
	}

	/**
	 * Nilai bawaan kolom {@code anak}: JSON object kosong {@code "{}"}.
	 *
	 * <p>Perhatikan namanya salah eja ({@code DEFULT}, bukan {@code DEFAULT}) dan deklarasinya
	 * {@code static} tanpa {@code final}, sehingga secara teknis dapat diubah dari luar &mdash;
	 * dibiarkan apa adanya karena sudah telanjur dipakai.</p>
	 */
	private static String ANAK_DEFULT = new JSONObject().toString();

	/**
	 * Daftar anak yang menempel pada keluarga ini, disimpan sebagai teks JSON di kolom {@code anak}
	 * (tipe {@code text}).
	 *
	 * <p>Bentuknya satu {@link JSONObject} datar dengan kunci {@code "siswa_<id>"} dan
	 * {@code "mahasiswa_<id>"} yang nilainya id itu sendiri, mis.
	 * {@code {"siswa_1201":1201,"mahasiswa_88014":88014}}. Kunci dibentuk/dibuang di
	 * {@code ais.action.master.OrangTuaAction}; tidak ada tabel relasi dan tidak ada
	 * <i>foreign key</i>, sehingga id anak yang barisnya sudah dihapus tetap tertinggal di sini.</p>
	 *
	 * <p>Bila kolom {@code null} atau kosong, yang dikembalikan {@code "{}"} ({@code ANAK_DEFULT})
	 * &mdash; nilai bawaan ini <b>tidak</b> ditulis balik ke field, jadi getter ini bebas efek
	 * samping. Gunakan {@link #ambilAnakSiswa()}, {@link #ambilAnakMahasiswa()}, atau
	 * {@link #ambilAnakMahasiswaObject()} untuk membacanya, bukan mengurai JSON sendiri.</p>
	 *
	 * @return teks JSON daftar anak; minimal {@code "{}"}, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getAnak() {
		return anak == null || anak.isEmpty() ? ANAK_DEFULT : anak;
	}

	/**
	 * Menyetel daftar anak dalam bentuk teks JSON. Isinya tidak divalidasi di sini; JSON rusak baru
	 * ketahuan saat dibaca &mdash; dan bahkan lalu ditelan diam-diam oleh method {@code ambilAnak*}.
	 *
	 * @param anak teks JSON berpola {@code {"siswa_<id>":<id>, "mahasiswa_<id>":<id>}}
	 */
	public void setAnak(String anak) {
		this.anak = anak;
	}

	/**
	 * Mengembalikan daftar id {@link ais.database.model.sekolah.Siswa} yang tercatat sebagai anak
	 * keluarga ini, hasil penguraian kolom JSON {@link #getAnak()} (kunci berawalan
	 * {@code "siswa"}).
	 *
	 * <p><b>Ini penentu ruang lingkup akses data portal orang tua.</b> Puluhan layar sekolah
	 * memanggilnya dengan pola
	 * {@code criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()))}
	 * &mdash; antara lain {@code AbsensiAction}, {@code AbsenPiketAction},
	 * {@code ApresiasiSiswaAction}, {@code DepositSiswaAction}, {@code PembayaranSiswaAction},
	 * {@code BniRequestAction}/{@code BsiRequestAction}, dan {@code DetailpertemuanHelper}.
	 * Pemanggil hampir selalu memeriksa {@code !isEmpty()} lebih dulu, karena
	 * {@code Restrictions.in} dengan daftar kosong menghasilkan kriteria yang tidak sah.</p>
	 *
	 * <p><b>Perilaku tepi:</b></p>
	 * <ul>
	 *   <li>bila {@link #getId()} masih {@code null} (entity belum disimpan), daftar kosong
	 *   dikembalikan <b>tanpa</b> menyentuh kolom JSON sama sekali &mdash; jadi anak yang baru
	 *   ditambahkan ke object yang belum tersimpan tidak akan terlihat;</li>
	 *   <li>JSON yang rusak ditelan blok {@code catch} kosong (bertanda
	 *   {@code auto-audit(empty-catch)}) dan hasilnya juga daftar kosong. Pada portal orang tua itu
	 *   tampak sebagai wali yang mendadak "tidak punya anak", bukan sebagai pesan kesalahan;</li>
	 *   <li>kunci dicocokkan dengan {@code startsWith("siswa")}, bukan {@code "siswa_"}, sehingga
	 *   kunci lain yang kebetulan berawalan sama ikut terbaca.</li>
	 * </ul>
	 *
	 * @return daftar id siswa; kosong (bukan {@code null}) bila tidak ada anak, entity belum
	 *         tersimpan, atau JSON tidak dapat diurai
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilAnakSiswa() {
		List<Long> idAnak = new ArrayList<Long>();
		if (getId() != null) {
			try {
				JSONObject o = new JSONObject(getAnak());
				Iterator<String> keys = o.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					if (key.startsWith("siswa")) {
						idAnak.add(ais.common.CommonJSONUtil.ambilLong(o,key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/OrangTua.java:713");
				// TODO: handle exception
			}
		}

		return idAnak;
	}

	/**
	 * Mengembalikan daftar id {@link Mahasiswa} yang tercatat sebagai anak keluarga ini, hasil
	 * penguraian kolom JSON {@link #getAnak()} (kunci berawalan {@code "mahasiswa"}).
	 *
	 * <p>Kembaran {@link #ambilAnakSiswa()} untuk sisi perguruan tinggi, dengan seluruh perilaku
	 * tepi yang sama (id {@code null} &rarr; daftar kosong, JSON rusak ditelan diam-diam,
	 * pencocokan {@code startsWith}). Dipakai penyaring ruang lingkup di
	 * {@code BniRequestAction}, {@code BsiRequestAction}, {@code DetailpertemuanHelper}, dan
	 * sejenisnya.</p>
	 *
	 * @return daftar id mahasiswa; kosong (bukan {@code null}) bila tidak ada anak, entity belum
	 *         tersimpan, atau JSON tidak dapat diurai
	 * @see #ambilAnakSiswa()
	 * @see #ambilAnakMahasiswaObject()
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilAnakMahasiswa() {
		List<Long> idAnak = new ArrayList<Long>();
		if (getId() != null) {
			try {
				JSONObject o = new JSONObject(getAnak());
				Iterator<String> keys = o.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					if (key.startsWith("mahasiswa")) {
						idAnak.add(ais.common.CommonJSONUtil.ambilLong(o,key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/OrangTua.java:734");
				// TODO: handle exception
			}
		}

		return idAnak;
	}

	/**
	 * Sama dengan {@link #ambilAnakMahasiswa()} tetapi langsung mengembalikan object
	 * {@link Mahasiswa}, dimuat dari cache entity lewat
	 * {@code ConstantValues.ambil(Mahasiswa.class.getName(), id)}.
	 *
	 * <p>Id yang tidak ditemukan (baris mahasiswa sudah dihapus, atau JSON menyimpan id basi karena
	 * tidak ada <i>foreign key</i>) <b>dilewati diam-diam</b>, sehingga daftar hasilnya bisa lebih
	 * pendek daripada {@link #ambilAnakMahasiswa()}. Selisih panjang kedua daftar itulah indikator
	 * paling murah untuk mendeteksi id anak yang sudah yatim.</p>
	 *
	 * <p>Pemuatan lewat cache berarti method ini dapat memicu pembacaan basis data per id bila
	 * entity belum ter-cache. Pemanggil yang hanya butuh id (untuk {@code Restrictions.in}) sebaiknya
	 * memakai {@link #ambilAnakMahasiswa()} yang jauh lebih murah. Tidak ada padanan
	 * {@code ambilAnakSiswaObject()} &mdash; sisi sekolah harus memuat object {@code Siswa} sendiri
	 * dari daftar id.</p>
	 *
	 * @return daftar object mahasiswa anak keluarga ini; kosong (bukan {@code null}) bila tidak ada
	 *         yang cocok, entity belum tersimpan, atau JSON tidak dapat diurai
	 * @see #ambilAnakMahasiswa()
	 */
	@SuppressWarnings("unchecked")
	public List<Mahasiswa> ambilAnakMahasiswaObject() {
		List<Mahasiswa> idAnak = new ArrayList<Mahasiswa>();
		if (getId() != null) {
			try {
				JSONObject o = new JSONObject(getAnak());
				Iterator<String> keys = o.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					if (key.startsWith("mahasiswa")) {
						Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(o,key));
						if (mahasiswa != null) {
							idAnak.add(mahasiswa);
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/OrangTua.java:759");
				// TODO: handle exception
			}
		}

		return idAnak;
	}

	/**
	 * Penunjuk balik ke {@link Pegawai} bila orang tua/wali keluarga ini kebetulan pegawai
	 * institusi sendiri (kolom {@code pegawai}) &mdash; dipakai antara lain untuk potongan biaya
	 * anak pegawai dan berkas personalia.
	 *
	 * <p><b>Relasi ini ganda dan rapuh.</b> {@link Pegawai} juga punya kolom {@code orang_tua} yang
	 * menunjuk balik ke sini, dan keduanya dipetakan {@code @ManyToOne} <b>tanpa</b> {@code mappedBy}
	 * &mdash; jadi ada dua kolom terpisah yang harus dijaga konsisten secara manual.
	 * {@code ais.action.master.OrangTuaAction} melakukannya saat menyimpan (menyetel
	 * {@code pegawai.setOrangTua(orangTua)} dan mengosongkan pegawai sebelumnya bila berubah);
	 * pembaruan lewat jalur lain dapat meninggalkan pasangan yang tidak sinkron.</p>
	 *
	 * <p>{@link Tbmuser#getOrangTua()} memperumitnya lagi: bila akun pengguna terhubung ke seorang
	 * pegawai, getter di sana <b>menimpa</b> kolom {@code orang_tua} miliknya sendiri dengan
	 * {@code pegawai.getOrangTua()}. Artinya untuk akun pegawai, pasangan pegawai&harr;orang tua di
	 * sinilah yang menentukan ruang lingkup portal orang tua, bukan kolom di {@code tbmuser}.</p>
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu dan menulis hasilnya balik ke
	 * field.</p>
	 *
	 * @return pegawai yang bersangkutan, atau {@code null} bila orang tua ini bukan pegawai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel penunjuk balik ke pegawai. <b>Tidak</b> menyetel sisi sebaliknya
	 * ({@code Pegawai.setOrangTua(...)}); pemanggil wajib melakukannya sendiri agar kedua kolom
	 * tetap selaras.
	 *
	 * @param pegawai pegawai yang merupakan orang tua/wali keluarga ini; {@code null} untuk
	 *                memutus kaitan
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}
}
