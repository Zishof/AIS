package ais.database.model.recruitment;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.A;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.Kota;
import ais.database.model.Negara;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Propinsi;
import ais.database.model.Wilayah;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.LampiranLain;

/**
 * <p>
 * Entity <b>calon pegawai</b> (pelamar kerja) pada modul rekrutmen AIS, dipetakan ke tabel
 * {@code public.calon_pegawai}. Satu baris mewakili satu orang pelamar yang mendaftar pada
 * sebuah {@link GelombangPendaftaranPegawai} (lowongan/gelombang penerimaan), lengkap dengan
 * biodata pribadi, data orang tua/wali, alamat berjenjang
 * ({@link #getKecamatanCalon()}/{@link #getKotaCalon()}/{@link #getPropinsiCalon()}), riwayat
 * sekolah asal, isian dinamis "parameter tambahan", serta status seleksi.
 * </p>
 *
 * <p>
 * <b>Posisi dalam arsitektur.</b> Kelas ini adalah kembaran struktural dari
 * {@link ais.database.model.sekolah.CalonSiswa} (PPDB sekolah) dan
 * {@link ais.database.model.BiodataCalonMahasiswa} (PMB perguruan tinggi): tiga modul
 * penerimaan yang lahir dari template kode yang sama, sehingga banyak nama field/method di sini
 * identik dengan kedua kelas tersebut (termasuk penamaan yang menyesatkan seperti
 * {@link #getNim()} dan {@link #getSekolahAsal()} pada konteks pelamar KERJA). Alur pemakaiannya:
 * </p>
 * <ul>
 * <li>{@code ais.action.master.recruitment.CalonPegawaiAction} — layar back-office panitia
 * rekrutmen (daftar, edit, ubah status seleksi, ekspor password);</li>
 * <li>{@code ais.action.maintenance.KarirAction} + {@code ais.action.servlet.Karir} +
 * {@code ais.common.KarirConfigUtil} — portal karir publik tempat pelamar mendaftar sendiri dan
 * login;</li>
 * <li>{@link CalonPegawaiPunyaDokumen} / {@link CalonPegawaiPunyaVerifikasiBerkas} /
 * {@link VerifikasiKelengkapanCalonPegawai} — klaster verifikasi berkas lamaran;</li>
 * <li>{@link UjianPegawai} / {@link JadwalUjianPegawai} / {@link RuangPegawai} — klaster ujian
 * seleksi;</li>
 * <li>{@link #getPegawai()} — jembatan ke {@link Pegawai} bila pelamar akhirnya diangkat.</li>
 * </ul>
 * <p>
 * Entity didaftarkan di {@code hibernate.cfg.xml} baris 325 dan memakai <i>property access</i>
 * (anotasi JPA menempel di getter, bukan di field). Konsekuensinya penting untuk dibaca bersama
 * catatan {@link #getPass()} di bawah: setiap kali Hibernate memuat, men-<i>dirty check</i>, atau
 * mem-<i>flush</i> instance ini, Hibernate MEMANGGIL getter-getter tersebut, sehingga getter yang
 * punya efek samping ikut mengubah state persisten tanpa ada kode aplikasi yang secara eksplisit
 * memintanya. Kombinasi {@code dynamicInsert}/{@code dynamicUpdate} membuat hanya kolom yang
 * berubah yang ikut di-{@code UPDATE}, tetapi tidak menghalangi efek samping tersebut tersimpan.
 * </p>
 *
 * <p>
 * <b>PERINGATAN KEAMANAN — kredensial.</b> Kelas ini menyimpan DUA field bernuansa kredensial,
 * dan keduanya bermasalah dengan cara yang berbeda:
 * </p>
 * <ol>
 * <li><b>{@link #getPass()}</b> — <i>getter destruktif yang menyemai kata sandi sendiri</i>. Bila
 * kolom {@code pass} masih kosong sementara {@link #getNomorInduk()} terisi, getter ini MENULIS
 * {@code pass = Common.desEncrypter.get().encrypt(getNomorInduk().trim())} lalu menyalakan
 * {@link #getIs_encripted()}. Artinya kata sandi bawaan setiap pelamar SAMA DENGAN nomor
 * registrasinya sendiri — nilai yang tercetak di kartu pendaftaran, tampil di grid panitia, dan
 * bahkan ikut di {@link #toString()}. Enkripsinya adalah DES ({@code DesEncrypter}) dengan
 * <i>passphrase</i> {@code Common.DES_PASS_PHRASE} yang tertanam permanen di kode sumber dan sama
 * untuk semua instalasi AIS: ini enkripsi REVERSIBEL, <b>bukan</b> hash. Bandingkan dengan pola
 * yang benar pada pendaftaran tenant ({@code ais.database.model.tenant.Pendaftar}) yang memakai
 * {@code PasswordHashService.hash()} (PBKDF2-HMAC-SHA256 + salt per-pengguna, satu arah).</li>
 * <li><b>{@link #getPinPassword()}</b> — kolom PIN portal yang di kelas kembarannya
 * ({@code CalonSiswa}/{@code BiodataCalonMahasiswa}) diisi dan ditampilkan apa adanya, namun pada
 * {@code CalonPegawai} tidak punya satu pun pemanggil di seluruh WC ini: field TIDUR/YATIM yang
 * tetap dipetakan ke database.</li>
 * </ol>
 * <p>
 * Perlu ditegaskan agar tidak salah kaprah: login pelamar ke portal karir TIDAK memakai kolom
 * {@code pass} milik entity ini. Login berjalan lewat {@code Tbmuser} (akun aplikasi biasa,
 * {@code Tbmuser.userPassword}, juga DES) yang ditautkan lewat {@code Tbmuser.calonPegawai}, lalu
 * sesi diisi oleh {@code KarirConfigUtil.putKarirSession(...)}. Jadi kolom {@code pass} di sini
 * adalah kredensial DORMAN yang tetap terisi otomatis dengan rahasia yang bisa ditebak dan bisa
 * didekripsi — beban risiko tanpa manfaat fungsional.
 * </p>
 *
 * <p>
 * <b>Kaitan dengan temuan {@code task_a1e32ff3} (ekspor kata sandi seluruh populasi).</b> Temuan
 * tersebut berada di layer Action, bukan di entity ini, dan sudah diverifikasi ulang dari kode:
 * {@code CalonPegawaiAction} memasang tombol toolbar "Password penyedia / perusahaan" (sekitar
 * baris 342) yang ditambahkan lewat {@code Common.appendKeToolbar(...)} — method itu HANYA
 * menempelkan komponen ke toolbar dan tidak melakukan pemeriksaan hak apa pun, sehingga siapa pun
 * yang bisa membuka halaman (hak READ biasa) melihat dan bisa menekannya. Penanganannya membaca
 * {@code initCriteria(true)} yang TIDAK punya filter kepemilikan/gelombang/satuan kerja sama
 * sekali (hanya filter aktif + status seleksi + nama), mengambil sampai 1.048.576 baris,
 * MEMBUATKAN akun {@code Tbmuser} baru untuk pelamar yang belum punya, lalu menulis
 * {@code Common.desEncrypter.get().decrypt(tbmuser.getUserPassword())} — kata sandi TERBACA
 * JELAS — ke kolom "Password" sebuah berkas XLSX yang langsung diunduh. Dari sisi entity,
 * kontribusi kelas ini terhadap temuan itu adalah {@link #getAlamatEmail()},
 * {@link #getTeleponPegawai()}, dan {@link #getNama()} yang ikut diekspor tanpa gerbang apa pun.
 * </p>
 *
 * <p>
 * <b>Data pribadi sensitif tanpa gerbang di level entity.</b> {@link #getNik()} (NIK KTP),
 * {@link #getKk()} (nomor Kartu Keluarga), {@link #getTanggalLahir()}, {@link #getNamaIbu()}
 * (nama ibu kandung — lazim dipakai sebagai pertanyaan keamanan perbankan), alamat lengkap
 * beserta RT/RW, dan nomor telepon semuanya diekspos lewat getter polos tanpa penyamaran,
 * pembatasan, maupun pencatatan akses. Ini bukan kesalahan entity itu sendiri (gerbang otorisasi
 * memang tempatnya di layer Action/Helper), tetapi wajib dicatat karena tidak ada satu pun
 * mekanisme kompensasi di sini: tidak ada getter yang menyensor, tidak ada penanda kerahasiaan,
 * dan {@code @Audited} justru menyalin semua nilai ini ke tabel revisi Envers.
 * </p>
 *
 * <p>
 * <b>Status seleksi bersifat mutually exclusive tanpa penjaga.</b> Empat flag
 * {@link #getTelahDiterima()}, {@link #getTerverifikasi()}, {@link #getDitolak()}, dan
 * {@link #getMengundurkanDiri()} secara semantik adalah SATU status berjenjang, tetapi disimpan
 * sebagai empat boolean lepas. Tidak ada invariant di entity yang mencegah kombinasi mustahil
 * (mis. {@code telahDiterima=true} sekaligus {@code ditolak=true}); konsistensi hanya dijaga oleh
 * satu {@code EventListener} radiogroup di {@code CalonPegawaiAction} (sekitar baris 621-663) yang
 * selalu menyetel keempatnya sekaligus. Jalur tulis mana pun di luar listener itu — CRUD generik,
 * impor, skrip — bisa meninggalkan baris dalam status tak konsisten. Lihat catatan lengkap pada
 * {@link #getTelahDiterima()} mengenai gerbang UI-only pada listener tersebut.
 * </p>
 *
 * <p>
 * <b>Getter dengan efek samping di kelas ini</b> (pola berulang yang perlu diwaspadai saat
 * membaca kode): {@link #getPass()} (menyemai kata sandi), {@link #getNama()} dan
 * {@link #getNim()} dan {@link #getNomorInduk()} (menyalin nilai dari field lain),
 * {@link #getAlamatEmail()} (mengosongkan email yang dianggap tidak valid),
 * {@link #getTelahLogin()} (menyimpulkan status login dari isi parameter tambahan),
 * {@link #getKecamatan()}/{@link #getKecamatanCalon()}/{@link #getPropinsi()} (menormalkan relasi
 * wilayah), serta {@link #getIs_encripted()} dan sederet getter boolean yang mengganti
 * {@code null} menjadi nilai bawaan.
 * </p>
 *
 * <p>
 * Komentar asli generator: "Pegawai generated by hbm2java".
 * </p>
 *
 * @see GelombangPendaftaranPegawai
 * @see KelompokPendaftaranPegawai
 * @see CalonPegawaiPunyaDokumen
 * @see VerifikasiKelengkapanCalonPegawai
 * @see ais.database.model.sekolah.CalonSiswa
 * @see ais.database.model.BiodataCalonMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "calon_pegawai", schema = "public")
public class CalonPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Instance {@code CalonPegawai} disimpan sebagai attribute
	 * {@code HttpSession} ("CalonPegawai") oleh {@code KarirConfigUtil.putKarirSession(...)},
	 * sehingga benar-benar berpotensi diserialisasi bila kontainer melakukan
	 * <i>session passivation</i> atau replikasi antar-node. Nilai ini tidak boleh diubah tanpa
	 * alasan, karena perubahannya membuat sesi lama tidak bisa dipulihkan.
	 */
	private static final long serialVersionUID = 8583487061204307799L;
	/** Kunci primer tabel {@code calon_pegawai}; lihat {@link #getId()}. */
	private Long id;
	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Lihat
	 * {@link #getOleh()} dan catatan pada {@link #setOleh(String)} mengenai alasan teknis
	 * duplikasi field audit di setiap entity.
	 */
	private String oleh;
	/**
	 * Field audit bayangan: identitas ({@code userId}) pengguna terakhir yang mengubah baris ini.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini (biasanya
	 * {@code Tbmuser.userId}), apa adanya tanpa normalisasi.
	 *
	 * <p>
	 * Nilai ini diisi oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * callback {@link #onUpdate()}, bukan oleh kode layar. Nilainya bisa {@code null} untuk baris
	 * lama atau untuk baris yang dibuat oleh proses non-interaktif (pendaftaran mandiri lewat
	 * portal karir, impor, skrip) yang tidak punya konteks pengguna login.
	 * </p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, dengan <b>penjaga anti-penghapusan</b>:
	 * masukan {@code null} atau berisi spasi saja akan DIABAIKAN dan nilai lama dipertahankan.
	 *
	 * <p>
	 * Pola ini konsisten di seluruh entity AIS dan disengaja: jejak audit hanya boleh maju, tidak
	 * boleh dikosongkan oleh pemanggil yang kebetulan tidak punya konteks pengguna. Efek
	 * sampingnya, field ini TIDAK BISA di-reset lewat setter — satu-satunya cara mengosongkannya
	 * adalah UPDATE langsung ke basis data.
	 * </p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * masukan {@code null}/kosong diabaikan sehingga nilai audit lama tetap utuh.
	 *
	 * <p>
	 * <b>Catatan arsitektur (bukan bug).</b> Pasangan {@code oleh}/{@code olehId}/
	 * {@code tanggal_dirubah} sengaja diulang di hampir semua entity AIS alih-alih diwariskan
	 * dari {@link GeneralValueObject}. Alasannya teknis: {@code GeneralValueObject} adalah POJO
	 * abstrak biasa yang TIDAK dianotasi {@code @MappedSuperclass}, sehingga Hibernate tidak akan
	 * memetakan properti yang dideklarasikan di sana. Duplikasi ini keharusan pemetaan, bukan
	 * salinan-tempel yang terlewat.
	 * </p>
	 *
	 * @param oleh nama/identitas pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini, apa adanya.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan Hibernate tepat sebelum pernyataan
	 * {@code UPDATE} baris ini dikirim, dan mendelegasikan pengisian stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} (yang menyetel {@link #getTanggal_dirubah()},
	 * {@link #getOleh()}, dan {@link #getOlehId()} dari konteks pengguna aktif).
	 *
	 * <p>
	 * Perhatikan cakupannya: hanya {@code @PreUpdate} yang dipasang, TIDAK ada
	 * {@code @PrePersist}. Baris yang baru pertama kali disimpan (mis. pendaftaran mandiri lewat
	 * portal karir) karena itu tidak mendapat pengisian audit lewat jalur ini; nilai
	 * {@code tanggal_dirubah} untuk baris baru datang dari inisialisasi field
	 * {@link #getTanggal_dirubah()} di deklarasinya, sementara {@code oleh}/{@code olehId} bisa
	 * tetap {@code null} sampai ada perubahan pertama.
	 * </p>
	 *
	 * <p>
	 * Method ini {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi; ia bagian
	 * dari kontrak siklus hidup entity.
	 * </p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()} — bukan {@code new Date()}, agar tunduk pada penyesuaian zona
	 * waktu/waktu simulasi terpusat AIS), lalu diperbarui oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>
	 * Berbeda dengan {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini TIDAK punya
	 * penjaga: memanggilnya dengan {@code null} benar-benar mengosongkan stempel audit. Dalam
	 * praktiknya pemanggil satu-satunya adalah {@code AuditTimestampInterceptor}.
	 * </p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom TIMESTAMP.
	 *
	 * <p>
	 * Nama properti memakai gaya {@code snake_case} ({@code tanggal_dirubah}) alih-alih
	 * {@code camelCase} khas Java; karena tidak ada {@code @Column(name=...)}, nama kolom
	 * database mengikuti nama properti ini apa adanya. Jangan diseragamkan tanpa migrasi kolom.
	 * </p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat di
	 *         memori, tetapi bisa {@code null} untuk baris lama hasil pemuatan dari database
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * <p>
	 * Cache statik galeri lampiran per calon pegawai: kunci luar adalah {@link #getId()} calon,
	 * nilainya peta {@code URL lampiran -> deskripsi}. Diisi oleh
	 * {@link #reloadGaleries(CalonPegawai)} dan dibaca oleh layar yang menampilkan galeri berkas
	 * lamaran.
	 * </p>
	 *
	 * <p>
	 * <b>Peringatan.</b> Ini {@code public static} dan mutable, sehingga:
	 * </p>
	 * <ul>
	 * <li>dibagi oleh SELURUH sesi pengguna dalam satu JVM — dua panitia yang membuka calon yang
	 * sama saling menimpa isi peta, dan tidak ada pemisahan tenant/kepemilikan apa pun;</li>
	 * <li>tidak pernah dibersihkan (tidak ada kebijakan kedaluwarsa maupun batas ukuran), sehingga
	 * tumbuh monoton sepanjang umur aplikasi — kebocoran memori bertahap pada instalasi dengan
	 * banyak pelamar;</li>
	 * <li>berbasis {@link HashMap} tanpa sinkronisasi, sedangkan penulisannya terjadi dari thread
	 * permintaan web mana pun — akses bersamaan bisa menghasilkan pembacaan tidak konsisten.</li>
	 * </ul>
	 */
	public static Map<Long, Map<String, String>> galeries = new HashMap<Long, Map<String, String>>();

	/**
	 * Memuat ulang isi cache {@link #galeries} untuk satu calon pegawai dari basis data.
	 *
	 * <p>
	 * Prosesnya: membuka sesi Hibernate <i>streaming</i> terpisah
	 * ({@code StreamingHibernateUtil}) — sesi khusus untuk pembacaan massal agar tidak mengotori
	 * sesi transaksional layar — lalu mengambil seluruh {@link LampiranLain} yang
	 * {@code ref}-nya sama dengan id calon DAN {@code jenis}-nya diawali
	 * {@code "Galery_CalonPegawai_"} (perbandingan {@code ilike} + {@code MatchMode.START},
	 * sehingga tidak peka huruf besar/kecil). Untuk setiap lampiran, tautan unduhnya dihitung
	 * lewat {@code FileFotoLain.ambilLinkLampiranLain(...)} dan dipetakan ke deskripsi lampiran.
	 * </p>
	 *
	 * <p>
	 * Peta untuk id yang bersangkutan dibuat bila belum ada, lalu {@code clear()} terlebih dahulu
	 * sehingga hasil pemanggilan ini menggantikan (bukan menambah) isi sebelumnya. Sesi streaming
	 * ditutup di akhir jalur sukses; pada jalur gagal, transaksi di-<i>rollback</i> dan galat
	 * dicatat lewat {@code ErrorAuditUtil}.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan ketahanan.</b> Penutupan sesi hanya terjadi di jalur sukses — tidak ada blok
	 * {@code finally}. Bila {@code ambilLinkLampiranLain} melempar exception di tengah perulangan,
	 * sesi streaming milik thread tersebut ditinggalkan dalam keadaan ter-rollback tetapi tidak
	 * tertutup. Selain itu tidak ada penjagaan {@code calonPegawai == null}, sehingga pemanggilan
	 * dengan argumen {@code null} berujung {@link NullPointerException} yang tertelan blok
	 * {@code catch} dan hanya tercatat di log.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan keamanan.</b> Method ini tidak memeriksa hak akses sama sekali: siapa pun yang
	 * bisa memicu pemanggilannya untuk sebuah id akan membuat tautan berkas lamaran calon
	 * tersebut tersedia di cache statik global {@link #galeries}, yang bisa dibaca oleh sesi lain
	 * dalam JVM yang sama. Berkas lamaran termasuk data pribadi (ijazah, KTP, transkrip), sehingga
	 * gerbang otorisasi di layar pemanggil adalah satu-satunya pelindung.
	 * </p>
	 *
	 * @param calonPegawai calon pegawai yang galerinya dimuat ulang; {@link #getId()}-nya dipakai
	 *                     sebagai kunci cache dan sebagai {@code ref} lampiran
	 */
	@SuppressWarnings("unchecked")
	public static void reloadGaleries(CalonPegawai calonPegawai) {
		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("ref", calonPegawai.getId()))
					.add(Restrictions.ilike("jenis", "Galery_CalonPegawai_", MatchMode.START)).list();

			Map<String, String> data = galeries.get(calonPegawai.getId());
			if (data == null) {
				data = new HashMap<String, String>();
				galeries.put(calonPegawai.getId(), data);
			}
			data.clear();
			for (LampiranLain lampiran : lampiranLains) {
				String link = FileFotoLain.ambilLinkLampiranLain(lampiran, false, false, LampiranLain.class);
				data.put(link, lampiran.getDeskripsi());
			}

			StreamingHibernateUtil.getInstance().closeSession();
			lampiranLains = null;
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/recruitment/CalonPegawai.java:135");
		}
	}

	/**
	 * Salinan nama pelamar yang dipetakan ke kolom {@code nama_pegawai} secara READ-ONLY.
	 * Nilai sebenarnya selalu diambil dari {@link #getNamaPegawai()}; lihat {@link #getNama()}.
	 */
	private String nama;

	/**
	 * Mengembalikan nama pelamar, sebagai alias baca-saja dari {@link #getNamaPegawai()}.
	 *
	 * <p>
	 * <b>Getter dengan efek samping.</b> Setiap pemanggilan MENIMPA field {@link #nama} dengan
	 * hasil {@link #getNamaPegawai()}, jadi apa pun yang pernah dipasang lewat
	 * {@link #setNama(String)} akan hilang pada pembacaan berikutnya. Setter-nya secara efektif
	 * tidak berguna dan hanya ada agar konvensi JavaBean terpenuhi.
	 * </p>
	 *
	 * <p>
	 * Pemetaannya sengaja dibuat {@code insertable = false, updatable = false}: kolom
	 * {@code nama_pegawai} yang sama juga dipetakan oleh {@link #getNamaPegawai()}, dan hanya
	 * properti itulah yang boleh menulis. Tanpa pengaturan ini Hibernate akan menolak pemetaan
	 * karena dua properti menunjuk satu kolom yang bisa ditulis. Duplikasi properti ini ada agar
	 * kode generik lintas modul (grid, laporan, pencarian) bisa memakai nama properti seragam
	 * {@code "nama"} — perhatikan bahwa {@code CalonPegawaiAction.initCriteria(...)} memang
	 * memfilter dan mengurutkan berdasarkan properti {@code "nama"}, bukan {@code "namaPegawai"}.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan keamanan.</b> Getter inilah yang mengisi kolom "Nama Lengkap" pada berkas ekspor
	 * kata sandi massal di {@code CalonPegawaiAction}, dan juga dipakai untuk membentuk
	 * {@code userId} akun {@code Tbmuser} baru (kata pertama nama + 3 digit acak) pada jalur
	 * ekspor tersebut.
	 * </p>
	 *
	 * @return nama pelamar sebagaimana tersimpan di {@link #getNamaPegawai()}; bisa {@code null}
	 */
	@Column(name = "nama_pegawai", nullable = false, insertable = false, updatable = false)
	public String getNama() {
		nama = getNamaPegawai();
		return nama;
	}

	/**
	 * Menyetel field bayangan {@link #nama}.
	 *
	 * <p>
	 * <b>Efeknya sementara.</b> Nilai yang dipasang di sini akan ditimpa pada pemanggilan
	 * {@link #getNama()} berikutnya, dan tidak akan pernah tersimpan ke database karena
	 * pemetaannya {@code insertable = false, updatable = false}. Untuk benar-benar mengubah nama
	 * pelamar gunakan {@link #setNamaPegawai(String)}.
	 * </p>
	 *
	 * @param nama nilai yang dipasang ke field bayangan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Salinan nomor induk yang dipetakan ke kolom {@code nomor_induk_nasional}. Lihat
	 * {@link #getNim()} untuk penjelasan penamaan yang menyesatkan pada konteks rekrutmen.
	 */
	private String nim;

	/**
	 * Mengembalikan "NIM" pelamar — penamaan warisan dari kelas kembarannya di modul PMB
	 * ({@code BiodataCalonMahasiswa}); pada modul rekrutmen ini isinya sebenarnya adalah NOMOR
	 * REGISTRASI pelamar.
	 *
	 * <p>
	 * <b>Getter dengan efek samping.</b> Setiap pemanggilan menimpa {@link #nim} dengan hasil
	 * {@link #getNomorInduk()}, yang pada gilirannya menimpa dirinya sendiri dari
	 * {@link #getNoRegistrasi()}. Jadi terdapat rantai tiga properti — {@code nim} →
	 * {@code nomorInduk} → {@code noRegistrasi} — yang semuanya bermuara pada satu nilai asli
	 * {@link #noRegistrasi}. Nilai yang dipasang lewat {@link #setNim(String)} tidak bertahan.
	 * </p>
	 *
	 * <p>
	 * Berbeda dengan {@link #getNama()}, pemetaan di sini TIDAK memakai
	 * {@code insertable = false, updatable = false}, dan kolomnya pun berbeda
	 * ({@code nomor_induk_nasional}, bukan {@code nomor_induk}). Artinya kolom
	 * {@code nomor_induk_nasional} benar-benar ditulis oleh Hibernate dengan nilai hasil
	 * penyalinan otomatis di atas — sebuah kolom duplikat yang isinya selalu mengekor
	 * {@code nomor_induk} tanpa ada kode yang pernah menulisnya secara sengaja.
	 * </p>
	 *
	 * <p>
	 * <b>Relevansi keamanan.</b> Nilai inilah (lewat {@link #getNomorInduk()}) yang menjadi bahan
	 * kata sandi bawaan pada {@link #getPass()}. Nomor registrasi bukan rahasia: ia tampil di
	 * grid panitia, tercetak di kartu peserta, dan ikut serta di {@link #toString()}.
	 * </p>
	 *
	 * @return nomor registrasi pelamar; bisa {@code null} bila {@link #getNoRegistrasi()} belum
	 *         terisi
	 */
	@Column(name = "nomor_induk_nasional", nullable = false)
	public String getNim() {
		nim = getNomorInduk();
		return nim;
	}

	/**
	 * Menyetel field bayangan {@link #nim}. Nilainya akan ditimpa pada pemanggilan
	 * {@link #getNim()} berikutnya; gunakan {@link #setNoRegistrasi(String)} untuk mengubah nomor
	 * yang sebenarnya.
	 *
	 * @param nim nilai yang dipasang ke field bayangan
	 */
	public void setNim(String nim) {
		this.nim = nim;
	}

	/** Gelombang/lowongan tempat pelamar mendaftar; lihat {@link #getGelombangPendaftaranPegawai()}. */
	private GelombangPendaftaranPegawai gelombangPendaftaranPegawai;
	/** Kelompok/kategori pendaftaran pelamar; lihat {@link #getKelompokPendaftaranPegawai()}. */
	private KelompokPendaftaranPegawai kelompokPendaftaranPegawai;
	/** Waktu pendaftaran; lihat {@link #getTanggalPendaftaran()} (punya nilai bawaan dinamis). */
	private Date tanggalPendaftaran;
	/** Agama pelamar; lihat {@link #getAgama()}. */
	private Agama agama;
	/** Alamat surel pelamar; lihat {@link #getAlamatEmail()} (getter menyensor nilai tak valid). */
	private String alamatEmail;
	/**
	 * PIN/kata sandi portal untuk pelamar.
	 *
	 * <p>
	 * <b>Field tidur.</b> Pada kelas kembarannya ({@code CalonSiswa},
	 * {@code BiodataCalonMahasiswa}) field ini diisi panitia dan ditampilkan apa adanya di layar,
	 * tetapi pada {@code CalonPegawai} tidak ada satu pun pemanggil
	 * {@link #getPinPassword()}/{@link #setPinPassword(String)} di seluruh WC ini. Kolomnya tetap
	 * dipetakan dan tetap ikut direkam Envers.
	 * </p>
	 */
	private String pinPassword;
	/** Alamat orang tua pelamar; lihat {@link #getAlamatOrangTua()}. */
	private String alamatOrangTua;
	/** Alamat ayah; dipakai sebagai cadangan oleh {@link #getAlamatWali()}. */
	private String alamatAyah;
	/** Alamat ibu; lihat {@link #getAlamatIbu()}. */
	private String alamatIbu;
	/** Alamat domisili pelamar; lihat {@link #getAlamatPegawai()}. */
	private String alamatPegawai;
	/** Nama dusun pada alamat pelamar; lihat {@link #getDusunCalon()}. */
	private String dusunCalon;
	/** Nomor RT pada alamat pelamar; disimpan sebagai teks agar angka berawalan nol tidak hilang. */
	private String rt;
	/** Nomor RW pada alamat pelamar; disimpan sebagai teks agar angka berawalan nol tidak hilang. */
	private String rw;
	/** Kode pos pada alamat pelamar; lihat {@link #getKodePos()}. */
	private String kodePos;
	/** Nama kelurahan/desa pada alamat pelamar, sebagai teks bebas (bukan relasi). */
	private String kelurahanCalon;
	/** Kecamatan alamat pelamar sebagai relasi {@link Wilayah}; lihat {@link #getKecamatanCalon()}. */
	private Wilayah kecamatanCalon;
	/** Propinsi alamat pelamar; lihat {@link #getPropinsiCalon()}. */
	private Propinsi propinsiCalon;
	/** Kota/kabupaten alamat pelamar; lihat {@link #getKotaCalon()}. */
	private Kota kotaCalon;

	/** Alamat wali; bila kosong, {@link #getAlamatWali()} jatuh ke {@link #getAlamatAyah()}. */
	private String alamatWali;
	/** Urutan kelahiran pelamar dalam keluarga; lihat {@link #getAnakKe()}. */
	private Integer anakKe;
	/** Jumlah bersaudara; lihat {@link #getDariAnakKe()}. */
	private Integer dariAnakKe;
	/** Jenis kelamin sebagai teks bebas (bukan enum); lihat {@link #getJenisKelamin()}. */
	private String jenisKelamin;
	/**
	 * NIK (Nomor Induk Kependudukan / nomor KTP) pelamar — data pribadi sangat sensitif yang
	 * diekspos tanpa penyamaran lewat {@link #getNik()}.
	 */
	private String nik;
	/**
	 * Nomor Kartu Keluarga pelamar — data pribadi sensitif, diekspos apa adanya lewat
	 * {@link #getKk()}.
	 */
	private String kk;
	/** Nama ayah pelamar; lihat {@link #getNamaAyah()}. */
	private String namaAyah;
	/**
	 * Nama ibu kandung pelamar. Lazim dipakai lembaga keuangan sebagai pertanyaan verifikasi
	 * identitas, sehingga sebaiknya diperlakukan setara data rahasia meski di sini diekspos apa
	 * adanya lewat {@link #getNamaIbu()}.
	 */
	private String namaIbu;
	/** Nama pelamar — sumber nilai sebenarnya untuk {@link #getNama()}. */
	private String namaPegawai;
	/** Nomor induk; selalu disalin dari {@link #noRegistrasi} oleh {@link #getNomorInduk()}. */
	private String nomorInduk;
	/**
	 * Nomor registrasi pendaftaran — SATU-SATUNYA nilai asli pada rantai
	 * {@code nim → nomorInduk → noRegistrasi}, dan sekaligus bahan kata sandi bawaan pada
	 * {@link #getPass()}. Dihasilkan oleh {@code DefaultNoRegGeneratorPegawai}.
	 */
	private String noRegistrasi;
	/** Catatan bebas panitia mengenai pelamar; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kewarganegaraan sebagai relasi {@link Negara}; lihat {@link #getNegara()}. */
	private Negara negara;

	/** Penanda baris aktif; lihat {@link #getAktif()} (bawaan {@code true} bila {@code null}). */
	private Boolean aktif;
	/**
	 * Kata sandi terenkripsi DES.
	 *
	 * <p>
	 * <b>PERINGATAN.</b> Field ini disemai secara otomatis oleh {@link #getPass()} dengan hasil
	 * enkripsi {@link #getNomorInduk()} bila masih kosong. Baca Javadoc {@link #getPass()} untuk
	 * rincian mekanisme dan implikasinya.
	 * </p>
	 */
	private String pass;
	/**
	 * Penanda apakah {@link #pass} sudah berbentuk sandi terenkripsi. Dinyalakan sebagai EFEK
	 * SAMPING oleh {@link #getPass()}; lihat {@link #getIs_encripted()}.
	 */
	private Boolean is_encripted;

	/**
	 * Tautan ke data {@link Pegawai} bila pelamar sudah resmi diangkat menjadi pegawai; relasi
	 * satu-satu lewat kolom unik. Lihat {@link #getPegawai()}.
	 */
	private Pegawai pegawai;
	/** Flag status seleksi "diterima"; lihat {@link #getTelahDiterima()}. */
	private Boolean telahDiterima;
	/** Flag status seleksi "terverifikasi"; lihat {@link #getTerverifikasi()}. */
	private Boolean terverifikasi;
	/** Penanda pelamar sudah menyetujui pernyataan/persetujuan pendaftaran; lihat {@link #getPernyataan()}. */
	private Boolean pernyataan;
	/**
	 * Bentuk ringkas (berbasis id) dari isian dinamis pelamar; lihat
	 * {@link #getParameterTambahanInds()} dan {@link #populateParameterTambahan(List)}.
	 */
	private String parameterTambahanInds;
	/**
	 * Bentuk terbaca (berbasis label) dari isian dinamis pelamar, disimpan sebagai satu blob teks
	 * dengan pemisah baris dan {@code <=>}; lihat {@link #getParameterTambahan()}.
	 */
	private String parameterTambahan;
	/** Penanda pelamar pernah login; lihat {@link #getTelahLogin()} (disimpulkan, bukan dicatat). */
	private Boolean telahLogin;
	/** Waktu login terakhir pelamar; lihat {@link #getWaktuLogin()} (tidak ada penulis di WC ini). */
	private Date waktuLogin;
	/** Pencacah cetak kartu peserta; lihat {@link #getCetakKartu()} (getter merusak nilai asli). */
	private Integer cetakKartu;
	/** Nomor ujian seleksi; dihasilkan {@code DefaultNoUjianGeneratorPegawai}. Lihat {@link #getNoUjian()}. */
	private String noUjian;
	/** Flag status seleksi "ditolak"; lihat {@link #getDitolak()}. */
	private Boolean ditolak;
	/** Flag status seleksi "mengundurkan diri"; lihat {@link #getMengundurkanDiri()}. */
	private Boolean mengundurkanDiri;
	/** Tanggal lahir pelamar (data pribadi); lihat {@link #getTanggalLahir()}. */
	private Date tanggalLahir;
	/** Tempat lahir pelamar; lihat {@link #getTempatLahir()}. */
	private String tempatLahir;
	/**
	 * Kewarganegaraan sebagai teks bebas — berdampingan dengan relasi {@link #negara} yang
	 * menyimpan informasi serupa secara terstruktur; keduanya tidak saling disinkronkan.
	 */
	private String kewarganegaraan;
	/** Nomor telepon/HP pelamar; lihat {@link #getTeleponPegawai()} dan {@link #tampilkanHp(Component)}. */
	private String teleponPegawai;
	/**
	 * "Sekolah asal" — penamaan warisan modul PPDB/PMB; pada konteks rekrutmen diisi sebagai
	 * institusi pendidikan terakhir pelamar. Lihat {@link #getSekolahAsal()}.
	 */
	private String sekolahAsal;
	/** Alamat institusi pendidikan terakhir pelamar; lihat {@link #getAlamatSekolahAsal()}. */
	private String alamatSekolahAsal;
	/** Nomor telepon orang tua pelamar; lihat {@link #getTeleponOrangTua()}. */
	private String teleponOrangTua;

	/** Kota/kabupaten (alamat kedua, terpisah dari {@link #kotaCalon}); lihat {@link #getKota()}. */
	private Kota kota;
	/**
	 * Propinsi (alamat kedua, terpisah dari {@link #propinsiCalon}); lihat {@link #getPropinsi()}
	 * yang menurunkannya dari {@link #getKota()}.
	 */
	private Propinsi propinsi;
	/** Kecamatan (alamat kedua, terpisah dari {@link #kecamatanCalon}); lihat {@link #getKecamatan()}. */
	private Wilayah kecamatan;

	/**
	 * Representasi teks entity dalam bentuk {@code id-nomorInduk-namaPegawai}.
	 *
	 * <p>
	 * Dipakai ZK sebagai label bawaan pada komponen pemilih (combobox/listbox) yang menerima
	 * objek ini secara langsung, dan muncul pula pada keluaran log/debug.
	 * </p>
	 *
	 * <p>
	 * <b>Dua catatan penting.</b> Pertama, method ini membaca FIELD {@link #nomorInduk} dan
	 * {@link #namaPegawai} secara langsung, bukan lewat getter-nya; untuk objek yang baru dimuat
	 * dan belum pernah dibaca lewat {@link #getNomorInduk()}, bagian nomor bisa tampil
	 * {@code null} walau {@link #getNoRegistrasi()} sebenarnya terisi. Kedua, nilai ini membocorkan
	 * nomor registrasi ke tempat-tempat yang tidak diniatkan sebagai tampilan data (log aplikasi,
	 * pesan galat) — dan nomor registrasi itulah bahan kata sandi bawaan pada {@link #getPass()}.
	 * </p>
	 *
	 * @return teks gabungan id, nomor induk, dan nama pelamar
	 */
	public String toString() {
		return id + "-" + nomorInduk + "-" + namaPegawai;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat
	 * baris dari database, sekaligus dipakai kode layar untuk membuat pelamar baru (mis.
	 * {@code KarirAction} pada pendaftaran mandiri lewat portal karir).
	 *
	 * <p>
	 * Semua field dibiarkan {@code null} kecuali {@link #getTanggal_dirubah()} yang diinisialisasi
	 * di deklarasinya. Perhatikan bahwa konstruktor ini TIDAK menyemai kata sandi apa pun —
	 * penyemaian baru terjadi saat {@link #getPass()} dipanggil dan {@link #getNomorInduk()} sudah
	 * terisi, yang dalam praktiknya berarti setelah nomor registrasi dibuat.
	 * </p>
	 */
	public CalonPegawai() {
	}

	/**
	 * Konstruktor pintas yang hanya menetapkan kunci primer, berguna untuk membentuk referensi
	 * ringan ke sebuah baris (mis. sebagai nilai pembanding pada kriteria Hibernate) tanpa perlu
	 * memuat seluruh kolomnya.
	 *
	 * <p>
	 * Objek hasil konstruktor ini bukan entity terkelola dan seluruh field lainnya {@code null};
	 * jangan disimpan lewat {@code saveOrUpdate} karena akan menimpa baris asli dengan nilai
	 * kosong.
	 * </p>
	 *
	 * @param id kunci primer baris {@code calon_pegawai} yang dirujuk
	 */
	public CalonPegawai(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci primer baris {@code calon_pegawai}.
	 *
	 * <p>
	 * Nilainya dihasilkan database ({@code GenerationType.IDENTITY}, kolom serial PostgreSQL) dan
	 * karena itu dipetakan {@code insertable = false}: Hibernate tidak menyertakan kolom
	 * {@code id} dalam pernyataan {@code INSERT} dan membacanya kembali setelah baris terbentuk.
	 * Konsekuensinya, {@code getId()} bernilai {@code null} sampai objek benar-benar tersimpan —
	 * hal ini penting untuk {@link #reloadGaleries(CalonPegawai)} dan
	 * {@link #populateParameterTambahan(List)} yang memakai id sebagai {@code ref} lampiran.
	 * </p>
	 *
	 * @return kunci primer, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer secara manual.
	 *
	 * <p>
	 * Dalam pemakaian normal setter ini hanya dipanggil Hibernate. Menyetel id pada objek yang
	 * sudah terkelola akan membingungkan sesi persistensi; untuk merujuk baris lain, buat instance
	 * baru lewat {@link #CalonPegawai(Long)}.
	 * </p>
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan gelombang/lowongan penerimaan tempat pelamar ini terdaftar.
	 *
	 * <p>
	 * Relasi {@code @ManyToOne} ke {@link GelombangPendaftaranPegawai} lewat kolom
	 * {@code current_gelombang_pendaftaran_pegawai_id} — awalan {@code current_} menandakan bahwa
	 * secara historis seorang pelamar bisa berpindah gelombang, dan kolom ini menyimpan gelombang
	 * yang BERLAKU SEKARANG saja (tidak ada riwayat perpindahan di entity ini). Pengambilannya
	 * memakai {@code FetchMode.SELECT}, yaitu kueri terpisah saat properti diakses, bukan
	 * {@code JOIN} pada kueri induk.
	 * </p>
	 *
	 * <p>
	 * {@code cascade = {PERSIST, MERGE}} berarti menyimpan calon pegawai ikut menyimpan objek
	 * gelombang yang tertaut — perlu diwaspadai bila objek gelombang yang dipasang berasal dari
	 * sesi lain atau sudah dimodifikasi, karena perubahannya ikut terbawa.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan cakupan data.</b> Relasi ini adalah SATU-SATUNYA penanda "kepemilikan" pada
	 * entity, tetapi {@code CalonPegawaiAction.initCriteria(...)} sama sekali tidak memfilter
	 * berdasarkannya. Akibatnya setiap panitia yang bisa membuka layar calon pegawai melihat
	 * seluruh pelamar dari seluruh gelombang/lowongan, termasuk pada jalur ekspor kata sandi
	 * massal.
	 * </p>
	 *
	 * @return gelombang pendaftaran pelamar, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "current_gelombang_pendaftaran_pegawai_id")
	public GelombangPendaftaranPegawai getGelombangPendaftaranPegawai() {
		return this.gelombangPendaftaranPegawai;
	}

	/**
	 * Menyetel gelombang/lowongan penerimaan pelamar.
	 *
	 * @param gelombangPendaftaranPegawai gelombang yang dipilih; {@code null} diperbolehkan
	 *                                    (kolom nullable)
	 */
	public void setGelombangPendaftaranPegawai(GelombangPendaftaranPegawai gelombangPendaftaranPegawai) {
		this.gelombangPendaftaranPegawai = gelombangPendaftaranPegawai;
	}

	/**
	 * Mengembalikan agama pelamar.
	 *
	 * <p>
	 * Sebelum dikembalikan, nilai dilewatkan {@code check(...)} milik {@link GeneralValueObject}.
	 * Helper itu menangani relasi {@code FetchType.LAZY}: bila objek yang tertaut ternyata berupa
	 * <i>proxy</i> yang sesi Hibernate-nya sudah tertutup — situasi lumrah untuk entity yang
	 * disimpan di {@code HttpSession} seperti calon pegawai pada portal karir — {@code check}
	 * mengembalikan {@code null} alih-alih membiarkan {@code LazyInitializationException}
	 * meledak ke layar.
	 * </p>
	 *
	 * <p>
	 * Sebagai efek sampingnya, getter ini bisa MENIMPA field {@link #agama} menjadi {@code null}.
	 * Bila objek lalu di-{@code flush}, relasi agama yang sebenarnya ada di database bisa ikut
	 * terhapus. Ini pola getter destruktif yang berulang di banyak entity AIS dan berlaku pula
	 * pada {@link #getKecamatan()}, {@link #getKecamatanCalon()}, {@link #getKota()}, dan
	 * {@link #getPropinsi()}.
	 * </p>
	 *
	 * @return agama pelamar, atau {@code null} bila belum diisi atau proxy-nya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama_id", nullable = true)
	public Agama getAgama() {
		agama = check(agama);
		return this.agama;
	}

	/**
	 * Menyetel agama pelamar.
	 *
	 * @param agama agama yang dipilih; {@code null} diperbolehkan
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Mengembalikan tempat lahir pelamar apa adanya, tanpa normalisasi maupun validasi.
	 *
	 * <p>
	 * Disimpan sebagai teks bebas, bukan relasi ke {@link Kota}, sehingga tidak bisa diandalkan
	 * untuk agregasi/statistik. Bersama {@link #getTanggalLahir()} nilai ini membentuk pasangan
	 * tempat-tanggal lahir yang termasuk data pribadi.
	 * </p>
	 *
	 * @return tempat lahir, atau {@code null} bila belum diisi
	 */
	@Column(name = "tempat_lahir")
	public String getTempatLahir() {
		return this.tempatLahir;
	}

	/**
	 * Menyetel tempat lahir pelamar. Tidak ada pembersihan spasi maupun validasi panjang di
	 * sini; kolomnya memakai panjang bawaan (255 karakter), sehingga masukan yang lebih panjang
	 * akan ditolak database saat {@code flush}, bukan saat setter dipanggil.
	 *
	 * @param tempatLahir tempat lahir sebagai teks bebas
	 */
	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	/**
	 * Mengembalikan tanggal lahir pelamar.
	 *
	 * <p>
	 * Dipetakan {@code TemporalType.DATE} sehingga hanya bagian tanggal yang tersimpan (komponen
	 * jam diabaikan). Atribut {@code length = 13} pada {@code @Column} tidak berpengaruh apa pun
	 * untuk tipe tanggal — itu sisa keluaran generator {@code hbm2java} yang aman diabaikan.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan data pribadi.</b> Tanggal lahir termasuk pengenal pribadi yang, digabung dengan
	 * {@link #getNamaIbu()} dan {@link #getNik()} yang juga tersimpan di baris yang sama,
	 * membentuk paket identitas yang cukup untuk penyalahgunaan. Semuanya ikut direkam ke tabel
	 * revisi Envers karena kelas ini {@code @Audited}.
	 * </p>
	 *
	 * @return tanggal lahir, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir", length = 13)
	public Date getTanggalLahir() {
		return this.tanggalLahir;
	}

	/**
	 * Menyetel tanggal lahir pelamar. Tidak ada validasi kewajaran (mis. tanggal di masa depan
	 * atau usia di bawah batas minimum melamar) baik di sini maupun di layar pendaftaran mandiri.
	 *
	 * @param tanggalLahir tanggal lahir; bagian waktu akan diabaikan saat disimpan
	 */
	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	/**
	 * Mengembalikan kewarganegaraan pelamar sebagai teks bebas.
	 *
	 * <p>
	 * Informasi yang sama juga tersimpan secara terstruktur pada relasi {@link #getNegara()}.
	 * Keduanya TIDAK saling disinkronkan: mengubah salah satu tidak memperbarui yang lain, dan
	 * tidak ada aturan mana yang menjadi acuan. Bila membutuhkan nilai yang bisa diandalkan untuk
	 * pelaporan, gunakan {@link #getNegara()} yang setidaknya punya nilai bawaan.
	 * </p>
	 *
	 * @return kewarganegaraan sebagai teks, atau {@code null} bila belum diisi
	 */
	@Column(name = "kewarganegaraan")
	public String getKewarganegaraan() {
		return this.kewarganegaraan;
	}

	/**
	 * Menyetel kewarganegaraan sebagai teks bebas.
	 *
	 * @param kewarganegaraan teks kewarganegaraan
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Mengembalikan alamat surel pelamar, sudah divalidasi dan dinormalisasi.
	 *
	 * <p>
	 * <b>Getter destruktif — hati-hati.</b> Bila field {@link #alamatEmail} terisi dan tidak
	 * hanya berisi spasi, getter ini menjalankan {@code Common.isValidEmailAddress(...)} dan,
	 * jika alamat dianggap TIDAK valid, MENIMPA field tersebut dengan string kosong. Karena
	 * entity memakai <i>property access</i>, Hibernate memanggil getter ini pada saat
	 * <i>dirty check</i>/{@code flush}, sehingga pengosongan itu benar-benar tersimpan ke
	 * database: alamat surel pelamar bisa lenyap permanen hanya karena baris tersebut kebetulan
	 * dibaca oleh sesi Hibernate yang aktif. Tidak ada jejak apa pun tentang nilai lama selain
	 * tabel revisi Envers.
	 * </p>
	 *
	 * <p>
	 * Nilai kembaliannya sendiri dinormalisasi lebih lanjut: {@code null} atau alamat yang lebih
	 * pendek dari 3 karakter dikembalikan sebagai string kosong, selebihnya dikembalikan setelah
	 * {@code trim()}. Jadi getter ini tidak pernah mengembalikan {@code null} — pemanggil boleh
	 * langsung memakai {@code .isEmpty()} tanpa penjagaan, dan memang begitulah
	 * {@code CalonPegawaiAction} memakainya.
	 * </p>
	 *
	 * <p>
	 * <b>Peran dalam alur akun.</b> Alamat inilah yang dipakai {@code CalonPegawaiAction} sebagai
	 * {@code userId} akun {@code Tbmuser} pelamar pada pendaftaran mandiri (bila kosong, sistem
	 * jatuh ke "kata pertama nama + 3 digit acak"), sekaligus sebagai tujuan pengiriman surel
	 * berisi kata sandi awal dalam bentuk TERBACA JELAS, dan sebagai kolom "Email" pada berkas
	 * ekspor kata sandi massal. Pengosongan diam-diam oleh getter ini karena itu berpotensi
	 * memutus jalur pemulihan akun pelamar.
	 * </p>
	 *
	 * @return alamat surel yang sudah di-{@code trim}, atau string kosong (tidak pernah
	 *         {@code null})
	 */
	@Column(name = "alamat_email")
	public String getAlamatEmail() {
		if (alamatEmail != null && !alamatEmail.trim().isEmpty()) {
			alamatEmail = Common.isValidEmailAddress(alamatEmail) ? alamatEmail : "";
		}
		return this.alamatEmail == null || this.alamatEmail.length() < 3 ? "" : this.alamatEmail.trim();
	}

	/**
	 * Menyetel alamat surel pelamar apa adanya, tanpa validasi.
	 *
	 * <p>
	 * Validasi baru terjadi pada pembacaan lewat {@link #getAlamatEmail()} — dan pada saat itu
	 * nilai yang tidak valid tidak sekadar ditolak melainkan dihapus. Bila alamat perlu
	 * dipertahankan apa adanya untuk keperluan koreksi manual, jangan mengandalkan field ini.
	 * </p>
	 *
	 * @param alamatEmail alamat surel mentah
	 */
	public void setAlamatEmail(String alamatEmail) {
		this.alamatEmail = alamatEmail;
	}

	/**
	 * Mengembalikan alamat orang tua pelamar apa adanya.
	 *
	 * <p>
	 * Kolomnya berkapasitas 2000 karakter (teks alamat panjang), dan berbeda dengan
	 * {@link #getAlamatPegawai()} getter ini TIDAK menormalkan {@code null} menjadi string
	 * kosong — pemanggil wajib menjaga sendiri kemungkinan {@code null}.
	 * </p>
	 *
	 * @return alamat orang tua, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_orang_tua", length = 2000)
	public String getAlamatOrangTua() {
		return this.alamatOrangTua;
	}

	/**
	 * Menyetel alamat orang tua pelamar.
	 *
	 * @param alamatOrangTua teks alamat (maksimal 2000 karakter)
	 */
	public void setAlamatOrangTua(String alamatOrangTua) {
		this.alamatOrangTua = alamatOrangTua;
	}

	/**
	 * Mengembalikan alamat domisili pelamar, dengan {@code null} dinormalkan menjadi string
	 * kosong sehingga aman langsung digabung ke teks tampilan atau laporan.
	 *
	 * <p>
	 * Berbeda dengan {@link #getAlamatEmail()}, normalisasi di sini hanya pada nilai kembalian —
	 * field aslinya tidak ditimpa, jadi getter ini tidak destruktif.
	 * </p>
	 *
	 * @return alamat domisili, atau string kosong (tidak pernah {@code null})
	 */
	@Column(name = "alamat_pegawai", length = 2000)
	public String getAlamatPegawai() {
		return this.alamatPegawai == null ? "" : alamatPegawai;
	}

	/**
	 * Menyetel alamat domisili pelamar.
	 *
	 * @param alamatPegawai teks alamat (maksimal 2000 karakter)
	 */
	public void setAlamatPegawai(String alamatPegawai) {
		this.alamatPegawai = alamatPegawai;
	}

	/**
	 * Mengembalikan alamat wali pelamar, dengan <b>cadangan otomatis</b>: bila field
	 * {@link #alamatWali} kosong atau hanya berisi spasi, yang dikembalikan adalah
	 * {@link #getAlamatAyah()}.
	 *
	 * <p>
	 * Pola ini membuat nilai kembalian tidak bisa dipakai untuk membedakan "wali beralamat sama
	 * dengan ayah" dari "alamat wali memang belum diisi". Kode yang perlu tahu apakah data wali
	 * benar-benar ada harus membaca field lewat jalur lain (mis. kueri kolom langsung), bukan
	 * lewat getter ini. Perhatikan pula asimetrinya: tidak ada cadangan ke
	 * {@link #getAlamatIbu()}, sehingga keluarga dengan wali pihak ibu tidak tertangani.
	 * </p>
	 *
	 * <p>
	 * Nilai yang benar-benar ada dikembalikan setelah {@code trim()}; nilai cadangan dari
	 * {@link #getAlamatAyah()} dikembalikan apa adanya (bisa {@code null}), sehingga getter ini
	 * TETAP bisa mengembalikan {@code null}.
	 * </p>
	 *
	 * @return alamat wali, atau alamat ayah sebagai cadangan, atau {@code null} bila keduanya
	 *         kosong
	 */
	@Column(name = "alamat_wali")
	public String getAlamatWali() {
		return this.alamatWali == null || alamatWali.trim().isEmpty() ? getAlamatAyah() : alamatWali.trim();
	}

	/**
	 * Menyetel alamat wali pelamar. Menyetel {@code null} atau string kosong berarti mengaktifkan
	 * kembali perilaku cadangan pada {@link #getAlamatWali()}.
	 *
	 * @param alamatWali teks alamat wali
	 */
	public void setAlamatWali(String alamatWali) {
		this.alamatWali = alamatWali;
	}

	/**
	 * Mengembalikan urutan kelahiran pelamar dalam keluarganya ("anak ke-berapa"), dengan
	 * {@code null} dinormalkan menjadi {@code 0}.
	 *
	 * <p>
	 * Nilai {@code 0} karena itu ambigu: bisa berarti "belum diisi" maupun nilai nol yang
	 * sengaja disimpan. Karena tidak ada nilai nol yang bermakna untuk urutan anak, dalam praktik
	 * {@code 0} selalu berarti data belum lengkap.
	 * </p>
	 *
	 * @return urutan kelahiran, atau {@code 0} bila belum diisi (tidak pernah {@code null})
	 */
	@Column(name = "anak_ke")
	public Integer getAnakKe() {
		return this.anakKe == null ? 0 : anakKe;
	}

	/**
	 * Menyetel urutan kelahiran pelamar. Tidak ada validasi terhadap {@link #getDariAnakKe()},
	 * sehingga kombinasi tak masuk akal (anak ke-5 dari 2 bersaudara) dapat tersimpan.
	 *
	 * @param anakKe urutan kelahiran
	 */
	public void setAnakKe(Integer anakKe) {
		this.anakKe = anakKe;
	}

	/**
	 * Mengembalikan jumlah bersaudara pelamar ("dari berapa bersaudara"), dengan {@code null}
	 * dinormalkan menjadi {@code 0}. Berpasangan dengan {@link #getAnakKe()}; keduanya tidak
	 * saling divalidasi.
	 *
	 * @return jumlah bersaudara, atau {@code 0} bila belum diisi (tidak pernah {@code null})
	 */
	@Column(name = "dari_anak_ke")
	public Integer getDariAnakKe() {
		return this.dariAnakKe == null ? 0 : dariAnakKe;
	}

	/**
	 * Menyetel jumlah bersaudara pelamar.
	 *
	 * @param dariAnakKe jumlah bersaudara
	 */
	public void setDariAnakKe(Integer dariAnakKe) {
		this.dariAnakKe = dariAnakKe;
	}

	/**
	 * Mengembalikan jenis kelamin pelamar sebagai teks bebas.
	 *
	 * <p>
	 * Kolomnya dinyatakan {@code nullable = false} dengan panjang 9 karakter — cukup untuk
	 * "Perempuan", nilai terpanjang yang dipakai layar. Nilainya diisi dari combobox di
	 * {@code CalonPegawaiAction} sehingga dalam praktik terbatas pada dua pilihan, tetapi tidak
	 * ada enum maupun constraint di database yang menegakkannya: jalur tulis lain (impor, CRUD
	 * generik, skrip) bebas menyimpan teks apa pun sepanjang tidak melebihi 9 karakter.
	 * </p>
	 *
	 * <p>
	 * Perhatikan ketidaksesuaian kontrak: kolom tidak boleh {@code null} menurut anotasi, tetapi
	 * getter ini bisa mengembalikan {@code null} untuk objek yang baru dibuat di memori dan belum
	 * diisi. Kegagalan baru muncul saat {@code flush}, bukan lebih awal.
	 * </p>
	 *
	 * @return teks jenis kelamin, atau {@code null} bila belum diisi
	 */
	@Column(name = "jenis_kelamin", nullable = false, length = 9)
	public String getJenisKelamin() {
		return this.jenisKelamin;
	}

	/**
	 * Menyetel jenis kelamin pelamar sebagai teks bebas (maksimal 9 karakter).
	 *
	 * @param jenisKelamin teks jenis kelamin
	 */
	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	/**
	 * Mengembalikan nama ayah pelamar apa adanya. Selain sebagai data biodata, nilai ini menjadi
	 * sumber cadangan bagi {@link #getAlamatWali()} lewat {@link #getAlamatAyah()}.
	 *
	 * @return nama ayah, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ayah")
	public String getNamaAyah() {
		return this.namaAyah;
	}

	/**
	 * Menyetel nama ayah pelamar.
	 *
	 * @param namaAyah nama ayah
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Mengembalikan nama ibu kandung pelamar apa adanya.
	 *
	 * <p>
	 * <b>Catatan kerahasiaan.</b> Nama ibu kandung adalah pertanyaan verifikasi identitas baku di
	 * perbankan dan layanan publik Indonesia. Di sini ia diekspos lewat getter polos tanpa
	 * penyamaran maupun pembatasan, tersimpan berdampingan dengan {@link #getNik()},
	 * {@link #getKk()}, dan {@link #getTanggalLahir()} pada baris yang sama, serta ikut disalin
	 * ke tabel revisi Envers. Gerbang perlindungan satu-satunya ada di layer Action/Helper.
	 * </p>
	 *
	 * @return nama ibu kandung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ibu")
	public String getNamaIbu() {
		return this.namaIbu;
	}

	/**
	 * Menyetel nama ibu kandung pelamar.
	 *
	 * @param namaIbu nama ibu kandung
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Mengembalikan nama pelamar — inilah properti yang benar-benar memiliki kolom
	 * {@code nama_pegawai} dan satu-satunya yang boleh menulisnya.
	 *
	 * <p>
	 * {@link #getNama()} adalah alias baca-saja yang selalu menyalin nilainya dari sini. Bila
	 * kode Anda perlu MENGUBAH nama pelamar, gunakan {@link #setNamaPegawai(String)}, bukan
	 * {@link #setNama(String)}.
	 * </p>
	 *
	 * <p>
	 * Kolomnya {@code nullable = false} tetapi getter ini tidak menjamin apa pun untuk objek yang
	 * belum diisi; pelanggaran baru terdeteksi database saat {@code flush}.
	 * </p>
	 *
	 * @return nama pelamar, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_pegawai", nullable = false)
	public String getNamaPegawai() {
		return this.namaPegawai;
	}

	/**
	 * Menyetel nama pelamar. Ini setter yang sesungguhnya untuk kolom {@code nama_pegawai}.
	 *
	 * @param namaPegawai nama lengkap pelamar
	 */
	public void setNamaPegawai(String namaPegawai) {
		this.namaPegawai = namaPegawai;
	}

	/**
	 * Mengembalikan nomor induk pelamar, sebagai alias baca-saja dari
	 * {@link #getNoRegistrasi()}.
	 *
	 * <p>
	 * <b>Getter dengan efek samping.</b> Setiap pemanggilan menimpa field {@link #nomorInduk}
	 * dengan hasil {@link #getNoRegistrasi()}. Pemetaannya {@code insertable = false,
	 * updatable = false} karena kolom {@code nomor_induk} yang sama juga dipetakan (dan ditulis)
	 * oleh {@link #getNoRegistrasi()}.
	 * </p>
	 *
	 * <p>
	 * <b>PENTING untuk keamanan.</b> Inilah nilai yang dibaca {@link #getPass()} untuk menyemai
	 * kata sandi bawaan pelamar. Selama {@link #getNoRegistrasi()} masih {@code null}, getter ini
	 * mengembalikan {@code null} dan penyemaian kata sandi TIDAK terjadi; begitu nomor registrasi
	 * dibuat (oleh {@code DefaultNoRegGeneratorPegawai} lewat {@code CalonPegawaiAction}),
	 * pembacaan {@link #getPass()} berikutnya — termasuk pembacaan yang dilakukan Hibernate
	 * sendiri — langsung menyemai kata sandi yang isinya sama dengan nomor registrasi itu.
	 * </p>
	 *
	 * @return nomor registrasi pelamar, atau {@code null} bila belum dibuat
	 */
	@Column(name = "nomor_induk", nullable = false, insertable = false, updatable = false)
	public String getNomorInduk() {
		nomorInduk = getNoRegistrasi();
		return this.nomorInduk;
	}

	/**
	 * Menyetel field bayangan {@link #nomorInduk}.
	 *
	 * <p>
	 * Nilainya akan ditimpa pada pemanggilan {@link #getNomorInduk()} berikutnya dan tidak pernah
	 * tersimpan ke database (pemetaan baca-saja). Untuk mengubah nomor yang sesungguhnya gunakan
	 * {@link #setNoRegistrasi(String)}.
	 * </p>
	 *
	 * @param nomorInduk nilai yang dipasang ke field bayangan
	 */
	public void setNomorInduk(String nomorInduk) {
		this.nomorInduk = nomorInduk;
	}

	/**
	 * Menyetel kata sandi terenkripsi pelamar apa adanya, tanpa enkripsi, tanpa hashing, dan
	 * tanpa validasi.
	 *
	 * <p>
	 * Kontraknya menuntut pemanggil menyerahkan nilai yang SUDAH terenkripsi (mis. hasil
	 * {@code Common.desEncrypter.get().encrypt(...)}); menyerahkan kata sandi mentah ke sini akan
	 * menyimpannya terbaca jelas di kolom {@code pass} tanpa peringatan apa pun. Kontrak implisit
	 * semacam ini adalah sumber kesalahan berulang di modul-modul lain AIS.
	 * </p>
	 *
	 * <p>
	 * Setter ini juga TIDAK menyentuh {@link #setIs_encripted(Boolean)}, sehingga penanda
	 * bentuk-tersimpan bisa berbeda dari kenyataan bila pemanggil lupa menyetelnya sendiri.
	 * </p>
	 *
	 * <p>
	 * Di seluruh WC ini tidak ditemukan satu pun pemanggil {@code setPass(...)} pada
	 * {@code CalonPegawai} — satu-satunya penulis kolom {@code pass} adalah efek samping
	 * {@link #getPass()}.
	 * </p>
	 *
	 * @param pass kata sandi dalam bentuk yang sudah terenkripsi
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Mengembalikan kata sandi pelamar dalam bentuk terenkripsi DES, <b>dan menyemainya sendiri
	 * bila masih kosong</b>.
	 *
	 * <h3>Mekanisme persisnya</h3>
	 * <p>
	 * Bila field {@link #pass} bernilai {@code null} atau hanya berisi spasi, DAN
	 * {@link #getNomorInduk()} sudah terisi, getter ini menjalankan dua penugasan:
	 * </p>
	 * <ol>
	 * <li>{@code pass = Common.desEncrypter.get().encrypt(getNomorInduk().trim())} — kata sandi
	 * dibentuk dari NOMOR REGISTRASI pelamar itu sendiri;</li>
	 * <li>{@code is_encripted = true} — penanda bahwa nilai tersimpan sudah terenkripsi
	 * dinyalakan, lihat {@link #getIs_encripted()}.</li>
	 * </ol>
	 * <p>
	 * Bila salah satu syarat tidak terpenuhi, nilai lama dikembalikan apa adanya (bisa
	 * {@code null}).
	 * </p>
	 *
	 * <h3>Mengapa ini getter destruktif yang benar-benar menulis ke database</h3>
	 * <p>
	 * Entity ini memetakan anotasi JPA pada getter, artinya Hibernate memakai <i>property
	 * access</i>: Hibernate sendiri yang memanggil {@code getPass()} ketika memuat, melakukan
	 * <i>dirty check</i>, dan mem-{@code flush} objek. Jadi penyemaian di atas tidak menunggu ada
	 * kode aplikasi yang secara sengaja meminta kata sandi — cukup sebuah baris
	 * {@code calon_pegawai} ikut terbawa dalam sesi Hibernate yang aktif dan kemudian di-flush,
	 * maka kolom {@code pass} dan {@code is_encripted} terisi permanen. Anotasi
	 * {@code dynamicUpdate} tidak mencegahnya; ia hanya membuat {@code UPDATE} berisi kolom yang
	 * berubah — dan kedua kolom ini memang berubah.
	 * </p>
	 *
	 * <h3>Kualitas kriptografinya</h3>
	 * <p>
	 * {@code Common.desEncrypter} adalah {@code DesEncrypter} dengan passphrase
	 * {@code Common.DES_PASS_PHRASE} yang <b>tertanam permanen di kode sumber dan sama untuk
	 * seluruh instalasi AIS mana pun</b>. Implikasinya berlapis:
	 * </p>
	 * <ul>
	 * <li>ini ENKRIPSI, bukan HASH — nilainya dapat dikembalikan ke bentuk semula oleh siapa pun
	 * yang memegang passphrase, dan passphrase itu dapat dibaca oleh siapa pun yang punya akses
	 * ke kode (termasuk lewat riwayat kontrol versi);</li>
	 * <li>DES dengan kunci efektif 56 bit sudah lama tidak dianggap aman secara kriptografi;</li>
	 * <li>enkripsinya deterministik (tanpa IV acak, tanpa salt), sehingga dua pelamar dengan nomor
	 * registrasi sama menghasilkan ciphertext identik dan pola dapat dikenali langsung dari dump
	 * database.</li>
	 * </ul>
	 * <p>
	 * Bandingkan dengan pola yang sudah dikonfirmasi benar pada pendaftaran tenant
	 * ({@code ais.database.model.tenant.Pendaftar}), yang memakai
	 * {@code PasswordHashService.hash()} — PBKDF2-HMAC-SHA256 dengan salt per-pengguna, fungsi
	 * satu arah yang tidak bisa dibalik sama sekali. Perlu dicatat bahwa penggantian
	 * {@code DesEncrypter} ke AES-256-GCM pernah dicoba dan DIBATALKAN (2026-09-02) karena
	 * sebagian jalur login memverifikasi kata sandi dengan membandingkan ciphertext-ke-ciphertext
	 * di kueri database, pola yang hanya jalan bila enkripsinya deterministik; rinciannya ada pada
	 * Javadoc {@code Common#DES_PASS_PHRASE}.
	 * </p>
	 *
	 * <h3>Nilai rahasia yang bukan rahasia</h3>
	 * <p>
	 * Bahkan seandainya kriptografinya kuat, kata sandi bawaannya tetap tidak bernilai: isinya
	 * adalah nomor registrasi pelamar, yang tampil di grid panitia, tercetak di kartu peserta,
	 * ikut serta pada {@link #toString()} (sehingga masuk ke log dan pesan galat), dan pada banyak
	 * instalasi dibentuk secara berurutan oleh {@code DefaultNoRegGeneratorPegawai} sehingga bisa
	 * ditebak dari satu contoh saja.
	 * </p>
	 *
	 * <h3>Siapa yang menulis, siapa yang membaca</h3>
	 * <p>
	 * <b>Penulis:</b> hanya getter ini sendiri. Penelusuran seluruh WC tidak menemukan pemanggil
	 * {@code setPass(...)} pada {@code CalonPegawai}, dan tidak ada pula jalur pendaftaran, layar
	 * admin, atau skrip yang mengisinya.
	 * <br>
	 * <b>Pembaca:</b> tidak ada pemanggil {@code calonPegawai.getPass()} di kode Java mana pun —
	 * satu-satunya "pembaca" adalah Hibernate saat memetakan kolom. Login pelamar ke portal karir
	 * TIDAK melewati kolom ini: autentikasi berjalan lewat entity {@code Tbmuser} (kolom
	 * {@code userPassword}, juga DES) yang ditautkan ke calon pegawai lewat
	 * {@code Tbmuser.calonPegawai}, lalu sesi diisi oleh
	 * {@code KarirConfigUtil.putKarirSession(...)}.
	 * </p>
	 * <p>
	 * Kesimpulannya, kolom {@code pass} pada entity ini adalah <b>kredensial dorman</b>: tidak
	 * pernah dipakai untuk apa pun, tetapi tetap terisi otomatis dengan rahasia yang dapat ditebak
	 * sekaligus dapat didekripsi, untuk setiap pelamar yang barisnya pernah tersentuh Hibernate.
	 * Ia menambah permukaan serangan tanpa memberi manfaat fungsional, dan karena kelas ini
	 * {@code @Audited}, setiap penyemaiannya juga tersalin ke tabel revisi Envers.
	 * </p>
	 *
	 * <h3>Hubungan dengan temuan ekspor kata sandi massal</h3>
	 * <p>
	 * Temuan keamanan yang sudah tercatat pada {@code CalonPegawaiAction} (tombol toolbar
	 * "Password penyedia / perusahaan") adalah masalah TERPISAH dan bekerja pada kolom yang
	 * berbeda: yang diekspor di sana adalah {@code Tbmuser.userPassword} yang didekripsi menjadi
	 * teks terbaca ke dalam berkas XLSX, untuk seluruh populasi pelamar tanpa filter kepemilikan,
	 * lewat tombol yang dipasang tanpa pemeriksaan hak tambahan. Kolom {@code pass} milik entity
	 * ini tidak ikut dalam ekspor tersebut. Keduanya berbagi akar yang sama — pemakaian enkripsi
	 * reversibel berkunci global untuk menyimpan kata sandi — tetapi harus diperbaiki secara
	 * terpisah.
	 * </p>
	 *
	 * @return kata sandi terenkripsi DES; disemai dari {@link #getNomorInduk()} bila sebelumnya
	 *         kosong, atau {@code null} bila nomor registrasi juga belum ada
	 * @see #getIs_encripted()
	 * @see #setPass(String)
	 */
	@Column(name = "pass", nullable = true, length = 100)
	public String getPass() {
		if ((pass == null || pass.trim().isEmpty()) && getNomorInduk() != null && !getNomorInduk().trim().isEmpty()) {
			pass = Common.desEncrypter.get().encrypt(getNomorInduk().trim());
			is_encripted = true;
		}
		return pass;
	}

	/**
	 * Mengembalikan penanda baris aktif, dengan {@code null} diperlakukan sebagai {@code true}.
	 *
	 * <p>
	 * Artinya baris lama yang kolomnya belum pernah diisi otomatis dianggap AKTIF — pilihan
	 * bawaan yang aman untuk data warisan, tetapi berarti "tidak aktif" harus selalu dinyatakan
	 * secara eksplisit dengan {@code false}, tidak cukup dengan mengosongkan kolom.
	 * </p>
	 *
	 * <p>
	 * Perhatikan bahwa penyaringan di {@code CalonPegawaiAction.initCriteria(...)} harus menirukan
	 * logika bawaan ini pada level SQL, dan memang begitu:
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}. Bila ada kode lain yang
	 * menyaring hanya dengan {@code eq("aktif", true)}, baris warisan berkolom {@code null} akan
	 * hilang dari hasil — ketidakcocokan yang perlu diperiksa pada setiap kueri baru terhadap
	 * tabel ini. Perlu dicatat pula bahwa filter aktif di layar itu bersifat OPSIONAL (bergantung
	 * pada checkbox {@code searchaktif}), sehingga secara bawaan pelamar nonaktif pun tetap ikut
	 * terambil, termasuk pada jalur ekspor kata sandi massal.
	 * </p>
	 *
	 * @return {@code true} bila baris aktif atau kolomnya {@code null}; {@code false} hanya bila
	 *         dinonaktifkan eksplisit (tidak pernah {@code null})
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return this.aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda baris aktif. Menyetel {@code null} bukan berarti "tidak aktif" melainkan
	 * mengembalikan perilaku bawaan {@code true} pada {@link #getAktif()}.
	 *
	 * @param aktif {@code true}/{@code false}/{@code null} sesuai penjelasan di atas
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan catatan bebas panitia mengenai pelamar apa adanya.
	 *
	 * <p>
	 * Properti ini tidak punya {@code @Column}, sehingga nama kolom mengikuti nama properti
	 * ({@code keterangan}) dengan panjang bawaan 255 karakter. Isinya teks bebas tanpa struktur:
	 * dalam praktik dipakai panitia untuk mencatat alasan penolakan, hasil wawancara, atau catatan
	 * administratif lain. Karena tidak terstruktur, isinya tidak bisa dipakai untuk kueri atau
	 * pelaporan yang andal, dan bisa saja memuat penilaian personal yang sensitif tanpa pembatasan
	 * akses tersendiri.
	 * </p>
	 *
	 * @return catatan panitia, atau {@code null} bila kosong
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas panitia mengenai pelamar.
	 *
	 * @param keterangan teks catatan (maksimal 255 karakter)
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan negara/kewarganegaraan pelamar sebagai relasi, dengan nilai bawaan
	 * {@code ConstantValues.INDONESIA} bila belum diisi.
	 *
	 * <p>
	 * <b>Perbedaan penting dari getter bernilai bawaan lainnya:</b> getter ini TIDAK menugaskan
	 * kembali ke field — nilai bawaan hanya dikembalikan, tidak disimpan. Karena itu kolom
	 * {@code negara_id} di database tetap {@code NULL} meski layar selalu menampilkan
	 * "Indonesia". Kueri SQL langsung terhadap tabel ini karena itu TIDAK boleh mengasumsikan
	 * bahwa pelamar berkewarganegaraan Indonesia punya {@code negara_id} terisi; penyaringan
	 * berdasarkan negara harus menyertakan {@code IS NULL} secara eksplisit.
	 * </p>
	 *
	 * <p>
	 * Nilai teks {@link #getKewarganegaraan()} menyimpan informasi serupa secara terpisah dan
	 * tidak disinkronkan dengan relasi ini.
	 * </p>
	 *
	 * @return negara pelamar, atau {@code ConstantValues.INDONESIA} sebagai bawaan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "negara_id")
	public Negara getNegara() {
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/**
	 * Menyetel negara/kewarganegaraan pelamar. Menyetel {@code null} mengembalikan perilaku
	 * bawaan {@code ConstantValues.INDONESIA} pada {@link #getNegara()}.
	 *
	 * @param negara negara pelamar; {@code null} diperbolehkan
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Menyetel penanda apakah {@link #getPass()} tersimpan dalam bentuk terenkripsi.
	 *
	 * <p>
	 * Setter ini tidak melakukan apa pun terhadap {@link #pass}: menyalakan penanda tidak
	 * mengenkripsi nilai yang sudah ada, dan mematikannya tidak mendekripsi apa pun. Ia semata
	 * metadata yang harus dijaga konsistensinya oleh pemanggil — dan di WC ini tidak ada satu pun
	 * pemanggil untuk {@code CalonPegawai}, sehingga satu-satunya penulisnya adalah efek samping
	 * {@link #getPass()}.
	 * </p>
	 *
	 * @param is_encripted {@code true} bila kolom {@code pass} berisi ciphertext
	 */
	public void setIs_encripted(Boolean is_encripted) {
		this.is_encripted = is_encripted;
	}

	/**
	 * Mengembalikan penanda apakah kolom {@code pass} tersimpan dalam bentuk terenkripsi, dengan
	 * {@code null} dinormalkan menjadi {@code false}.
	 *
	 * <p>
	 * <b>Getter destruktif ringan.</b> Normalisasi dilakukan dengan MENUGASKAN kembali ke field
	 * ({@code is_encripted = false}), bukan sekadar mengembalikan nilai. Untuk baris warisan yang
	 * kolomnya {@code NULL}, pembacaan pertama saja sudah mengubah state objek menjadi
	 * {@code false} dan — lewat property access Hibernate — ikut tersimpan pada {@code flush}
	 * berikutnya. Pola yang sama muncul kembali pada {@link #getTelahLogin()} dan
	 * {@link #getParameterTambahan()}.
	 * </p>
	 *
	 * <p>
	 * <b>Bahaya penafsirannya.</b> Nilai {@code false} di sini berarti "belum pernah dinyatakan
	 * terenkripsi", BUKAN "isinya kata sandi mentah". Baris warisan yang kolomnya {@code NULL}
	 * akan dilaporkan {@code false} padahal isinya bisa saja ciphertext DES. Kode yang memutuskan
	 * apakah perlu mendekripsi berdasarkan penanda ini akan salah untuk baris-baris tersebut.
	 * Karena penandanya hanya dinyalakan sebagai efek samping {@link #getPass()}, satu-satunya
	 * kombinasi yang benar-benar bisa dipercaya adalah {@code true} yang berasal dari penyemaian
	 * otomatis itu.
	 * </p>
	 *
	 * <p>
	 * Properti ini juga memakai penamaan {@code snake_case} dan tanpa {@code @Column}, sehingga
	 * nama kolomnya mengikuti nama properti apa adanya.
	 * </p>
	 *
	 * @return {@code true} bila kolom {@code pass} dinyatakan terenkripsi; {@code false} bila
	 *         belum dinyatakan (tidak pernah {@code null})
	 */
	public Boolean getIs_encripted() {
		if (is_encripted == null) {
			is_encripted = false;
		}
		return is_encripted;
	}

	/**
	 * Mengembalikan status "pelamar telah DITERIMA", dengan {@code null} dinormalkan menjadi
	 * {@code false} (dikembalikan saja, field tidak ditugaskan ulang).
	 *
	 * <h3>Satu status, empat kolom</h3>
	 * <p>
	 * Status seleksi pelamar sesungguhnya adalah satu nilai berjenjang, tetapi disimpan sebagai
	 * empat boolean lepas: {@code telahDiterima}, {@link #getTerverifikasi()},
	 * {@link #getDitolak()}, dan {@link #getMengundurkanDiri()}. Semuanya {@code false} berarti
	 * "belum diproses". Tidak ada satu pun invariant di entity — tidak ada validasi di setter,
	 * tidak ada callback siklus hidup, tidak ada constraint database — yang mencegah kombinasi
	 * mustahil seperti {@code telahDiterima = true} sekaligus {@code ditolak = true}.
	 * </p>
	 * <p>
	 * Konsistensi seluruhnya bergantung pada SATU {@code EventListener} radiogroup di
	 * {@code ais.action.master.recruitment.CalonPegawaiAction} (sekitar baris 621-663) yang
	 * selalu menyetel keempat flag sekaligus untuk setiap pilihan. Jalur tulis lain — CRUD
	 * generik, impor data, skrip pemeliharaan, API — bisa menyimpan baris dalam status tak
	 * konsisten tanpa hambatan. Pembacaan di layar itu sendiri menutupi masalahnya karena
	 * memeriksa flag secara berurutan ({@code terverifikasi} lebih dulu, baru
	 * {@code telahDiterima}, lalu {@code ditolak}, lalu {@code mengundurkanDiri}), sehingga baris
	 * tak konsisten tetap tampil "wajar" di layar sambil menyimpan data yang bertentangan.
	 * </p>
	 *
	 * <h3>Gerbang persetujuan bersifat UI-only</h3>
	 * <p>
	 * Perubahan status seleksi adalah keputusan kelulusan pelamar — persis jenis aksi yang
	 * seharusnya dijaga server-side. Verifikasi terhadap kode {@code CalonPegawaiAction}
	 * menunjukkan gerbangnya hanya di tampilan: hak {@code UPDATE} dibaca sekali saat layar
	 * dibangun ({@code edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE)}) dan
	 * dipakai untuk memanggil {@code setDisabled(!edit)} pada kelima radio pilihan status.
	 * Listener {@code onClick} yang benar-benar mengubah data dan memanggil
	 * {@code Common.refreshSaveOrUpdate(calonPegawai)} TIDAK memeriksa ulang hak tersebut, tidak
	 * memeriksa apakah pengguna berhak atas gelombang/lowongan yang bersangkutan, dan tidak
	 * mencatat siapa yang memutuskan (selain stempel audit umum {@link #getOleh()}). Menonaktifkan
	 * komponen di sisi klien bukan kontrol keamanan: peristiwa yang dikirim ke server tetap
	 * diproses listener. Pola yang sama sudah dikonfirmasi di beberapa domain lain dalam basis
	 * kode ini, sehingga temuan di sini bersifat penguatan, bukan kasus tersendiri.
	 * </p>
	 * <p>
	 * Tidak ada pula pemisahan tugas: satu orang yang sama dapat memverifikasi berkas lalu
	 * menerima pelamar, dan tidak ada mekanisme persetujuan berjenjang maupun penguncian status
	 * setelah keputusan diambil — status dapat dibolak-balik berapa kali pun.
	 * </p>
	 *
	 * <h3>Apa yang dipicu status "diterima"</h3>
	 * <p>
	 * Penelusuran seluruh WC menunjukkan {@code telahDiterima} pada {@code CalonPegawai} hanya
	 * dibaca oleh {@code CalonPegawaiAction} sendiri: sebagai kriteria penyaringan daftar
	 * ({@code Restrictions.eq("telahDiterima", true)}) dan untuk memilih radio yang tersorot.
	 * Berbeda dengan kembarannya di PPDB/PMB, TIDAK ada proses otomatis yang membuat data
	 * {@link Pegawai} ketika flag ini dinyalakan — pengangkatan tetap langkah manual terpisah
	 * lewat {@link #getPegawai()}. Karena itu dampak langsung penyalahgunaan flag ini terbatas
	 * pada tampilan/pelaporan seleksi, bukan penciptaan akun pegawai; meski demikian, ia tetap
	 * catatan resmi kelulusan pelamar.
	 * </p>
	 *
	 * <p>
	 * Properti ini tidak punya {@code @Column} sehingga nama kolomnya mengikuti nama properti.
	 * </p>
	 *
	 * @return {@code true} bila pelamar berstatus diterima (tidak pernah {@code null})
	 * @see #getTerverifikasi()
	 * @see #getDitolak()
	 * @see #getMengundurkanDiri()
	 */
	public Boolean getTelahDiterima() {
		return telahDiterima == null ? false : telahDiterima;
	}

	/**
	 * Menyetel status "pelamar telah diterima".
	 *
	 * <p>
	 * Setter ini tidak mematikan flag status lain; pemanggil bertanggung jawab penuh menjaga agar
	 * hanya satu status yang menyala. Lihat {@link #getTelahDiterima()} untuk penjelasan lengkap
	 * mengenai ketiadaan invariant dan gerbang persetujuan yang hanya di sisi tampilan.
	 * </p>
	 *
	 * @param telahDiterima status diterima; {@code null} akan dibaca sebagai {@code false}
	 */
	public void setTelahDiterima(Boolean telahDiterima) {
		this.telahDiterima = telahDiterima;
	}

	/**
	 * Mengembalikan status "berkas pelamar telah TERVERIFIKASI", dengan {@code null} dinormalkan
	 * menjadi {@code false}.
	 *
	 * <p>
	 * Dalam alur seleksi, verifikasi berkas mendahului keputusan diterima/ditolak; flag ini
	 * seharusnya menjadi hasil dari klaster {@link VerifikasiKelengkapanCalonPegawai} /
	 * {@link CalonPegawaiPunyaVerifikasiBerkas} / {@link ParameterVerifikasiCalonPegawai}. Namun
	 * penulisannya justru datang dari radiogroup status yang sama di {@code CalonPegawaiAction},
	 * bukan disimpulkan dari kelengkapan berkas: seorang panitia bisa menandai pelamar
	 * "terverifikasi" tanpa satu pun dokumen benar-benar diperiksa, dan sebaliknya kelengkapan
	 * berkas yang sudah terpenuhi tidak otomatis menyalakan flag ini.
	 * </p>
	 *
	 * <p>
	 * Perhatikan bahwa pada radiogroup tersebut "Terverifikasi" adalah pilihan yang SALING
	 * MENIADAKAN dengan "Diterima": memilih salah satu mematikan yang lain. Jadi flag ini bukan
	 * tahapan yang tetap menyala setelah pelamar diterima, melainkan status sesaat yang hilang
	 * begitu keputusan berikutnya diambil — riwayat verifikasi tidak tersimpan di entity ini
	 * (hanya di tabel revisi Envers).
	 * </p>
	 *
	 * <p>
	 * Gerbang yang berlaku sama dengan {@link #getTelahDiterima()}: hanya {@code setDisabled}
	 * di sisi tampilan, tanpa pemeriksaan ulang di listener yang menyimpan.
	 * </p>
	 *
	 * @return {@code true} bila berkas dinyatakan terverifikasi (tidak pernah {@code null})
	 */
	@Column(name = "terverifikasi")
	public Boolean getTerverifikasi() {
		return terverifikasi == null ? false : terverifikasi;
	}

	/**
	 * Menyetel status verifikasi berkas pelamar. Tidak ada pemeriksaan terhadap kelengkapan
	 * dokumen yang sebenarnya, dan tidak ada flag lain yang ikut disesuaikan.
	 *
	 * @param terverifikasi status terverifikasi; {@code null} dibaca sebagai {@code false}
	 */
	public void setTerverifikasi(Boolean terverifikasi) {
		this.terverifikasi = terverifikasi;
	}

	/**
	 * Mengembalikan kelompok/kategori pendaftaran pelamar (mis. jalur atau klasifikasi posisi),
	 * apa adanya tanpa nilai bawaan.
	 *
	 * <p>
	 * Relasi {@code @ManyToOne} ke {@link KelompokPendaftaranPegawai} lewat kolom
	 * {@code kelompok_pendaftaran_pegawai} — perhatikan nama kolomnya tidak berakhiran
	 * {@code _id} seperti relasi lain di kelas ini, jadi jangan tertukar saat menulis SQL manual.
	 * Pengambilannya {@code FetchMode.SELECT} (kueri terpisah saat properti diakses).
	 * </p>
	 *
	 * <p>
	 * Berbeda dengan {@link #getAgama()}, getter ini tidak melewatkan nilainya ke
	 * {@code check(...)}, tetapi relasinya juga tidak {@code LAZY} sehingga risiko
	 * {@code LazyInitializationException} lebih kecil.
	 * </p>
	 *
	 * @return kelompok pendaftaran, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kelompok_pendaftaran_pegawai", nullable = true)
	public KelompokPendaftaranPegawai getKelompokPendaftaranPegawai() {
		return kelompokPendaftaranPegawai;
	}

	/**
	 * Menyetel kelompok/kategori pendaftaran pelamar.
	 *
	 * @param kelompokPendaftaranPegawai kelompok yang dipilih; {@code null} diperbolehkan
	 */
	public void setKelompokPendaftaranPegawai(KelompokPendaftaranPegawai kelompokPendaftaranPegawai) {
		this.kelompokPendaftaranPegawai = kelompokPendaftaranPegawai;
	}

	/**
	 * Mengembalikan data {@link Pegawai} yang terbentuk dari pelamar ini, bila ia sudah resmi
	 * diangkat.
	 *
	 * <p>
	 * Inilah jembatan antara modul rekrutmen dan modul kepegawaian. Kolom {@code pegawai_id}
	 * dinyatakan {@code unique = true}, sehingga satu baris {@link Pegawai} hanya boleh ditautkan
	 * ke satu calon pegawai — mencegah dua pelamar berbeda diklaim menjadi orang yang sama.
	 * Perhatikan bahwa keunikan itu berlaku di sisi ini saja; tidak ada penjagaan sebaliknya yang
	 * mencegah satu pelamar dibuatkan beberapa baris {@link Pegawai} lewat jalur lain.
	 * </p>
	 *
	 * <p>
	 * <b>Pengangkatan adalah langkah manual.</b> Penelusuran WC ini tidak menemukan kode yang
	 * mengisi relasi tersebut secara otomatis ketika {@link #getTelahDiterima()} dinyalakan —
	 * tidak ada pemanggil {@code calonPegawai.setPegawai(...)} pada jalur rekrutmen. Artinya
	 * status "diterima" dan keberadaan data pegawai adalah dua kenyataan terpisah yang bisa saja
	 * tidak sinkron: pelamar berstatus diterima tanpa data pegawai, atau relasi pegawai terisi
	 * pada pelamar yang statusnya ditolak. Tidak ada laporan rekonsiliasi bawaan untuk keduanya.
	 * </p>
	 *
	 * <p>
	 * {@code cascade = {PERSIST, MERGE}} berarti menyimpan calon pegawai ikut menyimpan objek
	 * {@link Pegawai} yang tertaut, termasuk perubahan yang tidak disengaja pada objek itu.
	 * </p>
	 *
	 * @return data pegawai hasil pengangkatan, atau {@code null} bila pelamar belum diangkat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pegawai_id", nullable = true, unique = true)
	public Pegawai getPegawai() {
		return pegawai;
	}

	/**
	 * Menautkan pelamar ini ke data {@link Pegawai} hasil pengangkatan.
	 *
	 * <p>
	 * Tidak ada validasi bahwa pelamar berstatus diterima, tidak ada pemeriksaan bahwa data
	 * pegawai yang ditautkan memang milik orang yang sama, dan tidak ada pencatatan siapa yang
	 * menautkan selain stempel audit umum.
	 * </p>
	 *
	 * @param pegawai data pegawai yang ditautkan; {@code null} untuk memutus tautan
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan penanda bahwa pelamar telah menyetujui pernyataan/persetujuan pendaftaran
	 * (kebenaran data, kesediaan mengikuti seleksi, dan sejenisnya), dengan {@code null}
	 * dinormalkan menjadi {@code false}.
	 *
	 * <p>
	 * Nilai {@code false} karena itu ambigu antara "menolak menyetujui" dan "belum pernah
	 * ditanya" — dan karena nilainya boolean tunggal, tidak ada catatan KAPAN dan pada VERSI
	 * pernyataan yang mana persetujuan diberikan. Untuk keperluan pembuktian (mis. sengketa
	 * keabsahan data pelamar), flag ini tidak memadai; jejak yang tersedia hanyalah tabel revisi
	 * Envers.
	 * </p>
	 *
	 * <p>
	 * Properti ini tidak punya {@code @Column} sehingga nama kolomnya mengikuti nama properti.
	 * </p>
	 *
	 * @return {@code true} bila pelamar telah menyetujui pernyataan (tidak pernah {@code null})
	 */
	public Boolean getPernyataan() {
		return pernyataan == null ? false : pernyataan;
	}

	/**
	 * Menyetel penanda persetujuan pernyataan pendaftaran.
	 *
	 * @param pernyataan status persetujuan; {@code null} dibaca sebagai {@code false}
	 */
	public void setPernyataan(Boolean pernyataan) {
		this.pernyataan = pernyataan;
	}

	/**
	 * Mengembalikan isian dinamis pelamar dalam bentuk TERBACA (berbasis label), dengan
	 * {@code null} dinormalkan menjadi string kosong.
	 *
	 * <p>
	 * <b>Format penyimpanan.</b> Seluruh isian dinamis satu pelamar dipadatkan menjadi SATU blob
	 * teks pada kolom bertipe {@code text}: setiap isian menempati satu baris (dipisah
	 * {@code \n}), dan setiap baris berisi tujuh ruas yang dipisah penanda {@code <=>} —
	 * berturut-turut label gabungan ({@code namaKelompok->labelInputan}), nilai, URL lampiran,
	 * nomor urut, id {@link ParameterTambahan}, id
	 * {@link KelompokParameterTambahanCalonPegawai}, dan keterangan. Penulisannya dilakukan
	 * {@link #populateParameterTambahan(List)}; pembacaannya oleh
	 * {@link #ambilDataParameterTambahan()}.
	 * </p>
	 *
	 * <p>
	 * <b>Konsekuensi format ini.</b> Karena isian bukan baris tabel melainkan teks, tidak ada
	 * satu pun jaminan integritas: nilai yang kebetulan memuat {@code \n} atau {@code <=>} akan
	 * merusak penguraian baris berikutnya (tidak ada pelolosan karakter di mana pun), isian tidak
	 * bisa dikueri/diagregasi lewat SQL, dan perubahan definisi parameter tidak tercermin pada
	 * data lama yang sudah membeku dalam bentuk label. Blob label ini pada dasarnya cuplikan
	 * tampilan, bukan data — pasangannya {@link #getParameterTambahanInds()} yang berbasis id
	 * adalah bentuk yang lebih layak dijadikan acuan.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif ringan.</b> Normalisasi {@code null} dilakukan dengan menugaskan
	 * kembali ke field, bukan sekadar mengembalikan nilai. Untuk baris warisan berkolom
	 * {@code NULL}, pembacaan pertama saja sudah mengubah state objek menjadi string kosong dan —
	 * lewat property access Hibernate — berpotensi tersimpan pada {@code flush} berikutnya. Efek
	 * ini bertaut langsung dengan {@link #getTelahLogin()} yang menyimpulkan status login dari
	 * hasil getter ini.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan kerahasiaan.</b> Isian dinamis pada modul rekrutmen lazim memuat data pribadi
	 * bebas (riwayat kesehatan, ekspektasi gaji, referensi, alasan keluar dari pekerjaan
	 * sebelumnya) beserta tautan lampiran. Semuanya tersimpan dalam satu kolom teks tanpa
	 * klasifikasi kerahasiaan dan ikut tersalin ke tabel revisi Envers pada setiap perubahan.
	 * </p>
	 *
	 * @return blob isian dinamis berbasis label; string kosong bila belum ada (tidak pernah
	 *         {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}
		return parameterTambahan;
	}

	/**
	 * Menyetel blob isian dinamis berbasis label secara utuh (mengganti, bukan menambah).
	 *
	 * <p>
	 * Tidak ada validasi format sama sekali: pemanggil bertanggung jawab menyusun teks sesuai
	 * skema tujuh ruas yang dijelaskan pada {@link #getParameterTambahan()}. Dalam pemakaian
	 * normal satu-satunya pemanggil adalah {@link #populateParameterTambahan(List)}.
	 * </p>
	 *
	 * <p>
	 * Menyetel string kosong secara efektif MENGHAPUS seluruh isian dinamis pelamar sekaligus,
	 * tanpa konfirmasi dan tanpa cadangan selain tabel revisi Envers.
	 * </p>
	 *
	 * @param parameterTambahan blob isian dinamis berbasis label
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan isian dinamis pelamar dalam bentuk RINGKAS berbasis id, dengan {@code null}
	 * dinormalkan menjadi string kosong (menugaskan ke field, sama seperti
	 * {@link #getParameterTambahan()}).
	 *
	 * <p>
	 * Formatnya lebih sederhana daripada versi label: setiap baris berisi
	 * {@code idKelompok->idParameter<=>nilai<=>urlLampiran}, dengan keterangan ditambahkan
	 * sebagai ruas keempat. Karena mengacu pada id (bukan label), bentuk inilah yang tetap sahih
	 * ketika label parameter diubah di kemudian hari, dan bentuk inilah yang seharusnya dipakai
	 * saat mengisi ulang formulir.
	 * </p>
	 *
	 * <p>
	 * <b>Cacat penulisan yang perlu diketahui.</b> Pada {@link #populateParameterTambahan(List)},
	 * ruas keterangan hanya ikut ditulis untuk baris KEDUA dan seterusnya — perhatikan bahwa
	 * penggabungan barisnya menempelkan {@code "<=>" + keterangan} pada cabang "bukan baris
	 * pertama" saja. Akibatnya keterangan isian pertama selalu hilang dari blob berbasis id ini
	 * (blob berbasis label tetap memuatnya). Asimetri antara kedua blob tersebut adalah cacat
	 * nyata, bukan pilihan desain.
	 * </p>
	 *
	 * @return blob isian dinamis berbasis id; string kosong bila belum ada (tidak pernah
	 *         {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}
		return parameterTambahanInds;
	}

	/**
	 * Menyetel blob isian dinamis berbasis id secara utuh (mengganti, bukan menambah), tanpa
	 * validasi format.
	 *
	 * @param parameterTambahanInds blob isian dinamis berbasis id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Menguraikan blob {@link #getParameterTambahan()} menjadi daftar {@link CommonVO} yang siap
	 * dirender di layar atau laporan.
	 *
	 * <p>
	 * Blob dipecah per baris dengan {@code split("\n")}, lalu setiap baris dipecah lagi dengan
	 * penanda {@code <=>}. Pemetaan ruas ke {@link CommonVO} adalah: ruas ke-1 menjadi
	 * {@code name} (label), ruas ke-2 menjadi {@code name1} (nilai), ruas ke-3 menjadi
	 * {@code name2} (URL lampiran), ruas ke-4 menjadi {@code nomorUrut}, dan ruas ke-5 menjadi
	 * {@code id}. Setiap ruas diambil secara defensif dengan pemeriksaan panjang larik, sehingga
	 * baris yang ruasnya kurang lengkap tetap menghasilkan objek dengan nilai kosong alih-alih
	 * melempar {@link ArrayIndexOutOfBoundsException}.
	 * </p>
	 *
	 * <p>
	 * Penguraian angka ({@code nomorUrut} dan {@code id}) dibungkus {@code try/catch} dengan nilai
	 * bawaan {@code 1}; kegagalan dicatat lewat {@code ErrorAuditUtil} tetapi tidak menghentikan
	 * proses. Ini berarti baris yang rusak diam-diam bergeser ke urutan 1 dan ber-id 1, bukan
	 * ditolak — data rusak tetap tampil dengan atribut yang salah. Di akhir, hasil diurutkan
	 * dengan {@code Collections.sort} sesuai urutan alami {@link CommonVO} (berdasarkan
	 * {@code nomorUrut}).
	 * </p>
	 *
	 * <p>
	 * <b>Perilaku pada data kosong.</b> Karena {@code "".split("\n")} menghasilkan larik berisi
	 * satu string kosong, memanggil method ini pada pelamar yang belum mengisi apa pun tetap
	 * mengembalikan daftar berisi SATU {@link CommonVO} kosong, bukan daftar kosong. Pemanggil
	 * yang memakai jumlah elemen untuk menyimpulkan "ada isian atau tidak" akan keliru; periksa
	 * isi labelnya, atau periksa {@link #getParameterTambahan()} langsung.
	 * </p>
	 *
	 * <p>
	 * Method ini murni membaca — tidak mengubah state entity dan tidak menyentuh database — namun
	 * bergantung pada {@link #getParameterTambahan()} yang punya efek samping normalisasi
	 * {@code null}.
	 * </p>
	 *
	 * @return daftar isian dinamis terurut menurut nomor urut; tidak pernah {@code null}, tetapi
	 *         bisa berisi satu elemen kosong bila belum ada isian
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/recruitment/CalonPegawai.java:536");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/recruitment/CalonPegawai.java:542");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Membaca kembali isian dinamis dari baris-baris komponen ZK di layar, lalu menuliskannya ke
	 * {@link #setParameterTambahan(String)} dan {@link #setParameterTambahanInds(String)}.
	 *
	 * <p>
	 * Ini kebalikan dari {@link #ambilDataParameterTambahan()}: dari komponen layar menjadi blob
	 * teks. Untuk setiap {@link Row}, method mengambil dua atribut yang sebelumnya ditempelkan
	 * layar — {@code "parameterTambahan"} berisi {@link ParameterTambahan} dan
	 * {@code "kelompokParameterTambahanCalonPegawai"} berisi
	 * {@link KelompokParameterTambahanCalonPegawai} — dan MELEWATI baris yang salah satu
	 * atributnya tidak ada. Nilai isian diambil lewat {@code ParameterTambahan.ambilVal(row, ...)}
	 * yang tahu cara membaca berbagai jenis komponen masukan, sedangkan keterangan dibaca dari
	 * atribut {@code "keterangan"} hanya bila ia benar-benar sebuah {@link Textbox}.
	 * </p>
	 *
	 * <p>
	 * <b>Penjagaan kunci lampiran (perbaikan penting).</b> Bila parameter menuntut lampiran
	 * ({@code getHarusMenyertakanLampiran()}), kunci jenis lampiran dihitung lewat
	 * {@code LampiranLain.resolveJenisParameterTambahan(CalonPegawai.class, getId(), ...)} —
	 * bukan dirakit sebagai teks lepas. Pemakaian resolver ini penting: ia menyertakan identitas
	 * kelas entity ke dalam kunci, sehingga isian dinamis milik modul berbeda yang kebetulan
	 * memakai pasangan id kelompok/parameter yang sama tidak lagi saling menimpa. Nilai baliknya
	 * dipakai untuk mengambil {@link LampiranLain} yang sesuai dan menyalin URL unduhnya ke blob.
	 * </p>
	 *
	 * <p>
	 * <b>Perilaku kosong yang perlu diwaspadai.</b> Bila {@code parameterRows} {@code null} atau
	 * kosong, method langsung {@code return} tanpa menyentuh apa pun — ini penjagaan
	 * <i>fail-closed</i> yang benar dan mencegah isian pelamar terhapus hanya karena layar belum
	 * selesai membangun barisnya. Namun bila daftar berisi baris-baris yang SELURUHNYA gagal
	 * memenuhi syarat atribut, method tetap berjalan sampai akhir dan menulis dua string KOSONG,
	 * yang berarti menghapus seluruh isian dinamis pelamar. Kegagalan per-baris ditelan
	 * {@code Common.tampilErrorJikaAdmin(e)} yang hanya menampilkan pesan kepada admin, sehingga
	 * pelamar biasa tidak akan tahu isiannya hilang.
	 * </p>
	 *
	 * <p>
	 * <b>Cacat keterangan pada blob berbasis id.</b> Pada penggabungan
	 * {@code parameterTambahanInds}, ruas keterangan hanya ditempelkan pada cabang "bukan baris
	 * pertama". Keterangan untuk isian pertama karena itu tidak pernah tersimpan pada blob
	 * berbasis id — lihat {@link #getParameterTambahanInds()}.
	 * </p>
	 *
	 * <p>
	 * Method ini menulis ke state entity dan bergantung pada {@link #getId()}; memanggilnya pada
	 * objek yang belum pernah disimpan membuat kunci lampiran dihitung dengan id {@code null}.
	 * </p>
	 *
	 * @param parameterRows daftar baris komponen ZK yang memuat isian dinamis; {@code null} atau
	 *                      kosong berarti tidak ada perubahan sama sekali
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
				KelompokParameterTambahanCalonPegawai kelompokParameterTambahanCalonPegawai = (KelompokParameterTambahanCalonPegawai) row
						.getAttribute("kelompokParameterTambahanCalonPegawai");
				if (parameterTambahan != null && kelompokParameterTambahanCalonPegawai != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CalonPegawai.class, getId(),
							kelompokParameterTambahanCalonPegawai.getId() + "->" + parameterTambahan.getId());

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

					String s = kelompokParameterTambahanCalonPegawai.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCalonPegawai.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCalonPegawai.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url;
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds
							: "\n" + sIds + "<=>" + (keterangan == null ? "" : keterangan.getValue().trim());
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	public String getPinPassword() {
		return pinPassword;
	}

	public void setPinPassword(String pinPassword) {
		this.pinPassword = pinPassword;
	}

	public Boolean getTelahLogin() {

		if (!getParameterTambahan().trim().isEmpty()) {
			telahLogin = true;
		}

		if (telahLogin == null) {
			telahLogin = false;
		}
		return telahLogin;
	}

	public void setTelahLogin(Boolean telahLogin) {
		this.telahLogin = telahLogin;
	}

	public Date getWaktuLogin() {
		return waktuLogin;
	}

	public void setWaktuLogin(Date waktuLogin) {
		this.waktuLogin = waktuLogin;
	}

	public Integer getCetakKartu() {
		return cetakKartu == null ? 0 : 1;
	}

	public void setCetakKartu(Integer cetakKartu) {
		this.cetakKartu = cetakKartu;
	}

	@Column(name = "telepon_pegawai")
	public String getTeleponPegawai() {
		return this.teleponPegawai == null ? "" : teleponPegawai;
	}

	public void setTeleponPegawai(String teleponPegawai) {
		this.teleponPegawai = teleponPegawai;
	}

	@Column(name = "sekolah_asal")
	public String getSekolahAsal() {
		return this.sekolahAsal;
	}

	public void setSekolahAsal(String sekolahAsal) {
		this.sekolahAsal = sekolahAsal;
	}

	public String getAlamatSekolahAsal() {
		return alamatSekolahAsal;
	}

	public void setAlamatSekolahAsal(String alamatSekolahAsal) {
		this.alamatSekolahAsal = alamatSekolahAsal;
	}

	@Column(name = "telepon_orang_tua")
	public String getTeleponOrangTua() {
		return this.teleponOrangTua;
	}

	public void setTeleponOrangTua(String teleponOrangTua) {
		this.teleponOrangTua = teleponOrangTua;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPendaftaran() {
		return tanggalPendaftaran == null ? ais.ui.util.WaktuUtil.getDate() : tanggalPendaftaran;
	}

	public void setTanggalPendaftaran(Date tanggalPendaftaran) {
		this.tanggalPendaftaran = tanggalPendaftaran;
	}

	public String getDusunCalon() {
		return dusunCalon;
	}

	public void setDusunCalon(String dusunCalon) {
		this.dusunCalon = dusunCalon;
	}

	public String getRt() {
		return rt;
	}

	public void setRt(String rt) {
		this.rt = rt;
	}

	public String getRw() {
		return rw;
	}

	public void setRw(String rw) {
		this.rw = rw;
	}

	public String getKodePos() {
		return kodePos == null ? "" : kodePos;
	}

	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	public String getKelurahanCalon() {
		return kelurahanCalon;
	}

	public void setKelurahanCalon(String kelurahanCalon) {
		this.kelurahanCalon = kelurahanCalon;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_calon_wilayah", nullable = true)
	public Wilayah getKecamatanCalon() {
		kecamatanCalon = check(kecamatanCalon);
		if (kecamatanCalon != null && kecamatanCalon.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatanCalon.getFeeder() != null
						&& kecamatanCalon.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatanCalon = w;
					break;
				}
			}

		}
		return kecamatanCalon;
	}

	public void setKecamatanCalon(Wilayah kecamatan) {
		this.kecamatanCalon = kecamatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "propinsi_calon", nullable = true)
	public Propinsi getPropinsiCalon() {
		return propinsiCalon;
	}

	public void setPropinsiCalon(Propinsi propinsiCalon) {
		this.propinsiCalon = propinsiCalon;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kota_calon", nullable = true)
	public Kota getKotaCalon() {
		return kotaCalon;
	}

	public void setKotaCalon(Kota kotaCalon) {
		this.kotaCalon = kotaCalon;
	}

	public String getNoUjian() {
		return noUjian;
	}

	public void setNoUjian(String noUjian) {
		this.noUjian = noUjian;
	}

	@Column(name = "nomor_induk", nullable = false)
	public String getNoRegistrasi() {
		return noRegistrasi;
	}

	public void setNoRegistrasi(String noRegistrasi) {
		this.noRegistrasi = noRegistrasi;
	}

	public String getAlamatAyah() {
		return alamatAyah;
	}

	public void setAlamatAyah(String alamatAyah) {
		this.alamatAyah = alamatAyah;
	}

	public String getAlamatIbu() {
		return alamatIbu;
	}

	public void setAlamatIbu(String alamatIbu) {
		this.alamatIbu = alamatIbu;
	}

	public String getNik() {
		return nik;
	}

	public void setNik(String nik) {
		this.nik = nik;
	}

	public String getKk() {
		return kk;
	}

	public void setKk(String kk) {
		this.kk = kk;
	}

	public Boolean getDitolak() {
		return ditolak == null ? false : ditolak;
	}

	public void setDitolak(Boolean ditolak) {
		this.ditolak = ditolak;
	}

	public Boolean getMengundurkanDiri() {
		return mengundurkanDiri == null ? false : mengundurkanDiri;
	}

	public void setMengundurkanDiri(Boolean mengundurkanDiri) {
		this.mengundurkanDiri = mengundurkanDiri;
	}

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

	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = true)
	public Propinsi getPropinsi() {
		propinsi = check(propinsi);
		kota = check(kota);
		if (kota != null && kota.getPropinsi() != null) {
			propinsi = kota.getPropinsi();
		}

		return propinsi;
	}

	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		kota = check(kota);
		return kota;
	}

	public void setKota(Kota kota) {
		this.kota = kota;
	}

	public void tampilkanEmail(Component vbox) {
		String email = getAlamatEmail();
		Toolbarbutton a;
		(a = new ais.ui.util.MyToolbarbuttonConfig(email)).setParent(vbox);
		if (email != null && !email.trim().isEmpty()) {
			a.setImage("/img/svg/mail-send-line.svg");
			a.setStyle("font-size:9px;");
			a.setTarget("_blank");
			a.setHref("mailto:" + email);
		}

	}

	public void tampilkanHp(Component vbox) {
		try {

			String hp = getTeleponPegawai();
			String telp = getTeleponPegawai();

			Toolbarbutton a;
			(a = new ais.ui.util.MyToolbarbuttonConfig(
					(hp == null || hp.toString().trim().equals("08100000000000000000")
							|| hp.toString().trim().equals("0000000000") ? "" : hp)
							+ (telp == null || telp.toString().trim().isEmpty()
									|| telp.toString().trim().equals("00000000000000000000")
									|| telp.toString().trim().equals("000000000")
											? ""
											: (hp == null || hp.toString().trim().isEmpty()
													|| hp.toString().trim().equals("08100000000000000000")
													|| hp.toString().trim().equals("0000000000") ? "" : " / ") + telp)))
					.setParent(vbox);

			if (telp != null && !telp.trim().isEmpty() && hp != null && hp.equals(telp)) {
				a.setLabel(hp);
			}

			if (hp == null || hp.toString().trim().isEmpty() || hp.toString().trim().equals("08100000000000000000")
					|| hp.toString().trim().equals("0000000000")) {
				hp = telp;
			}

			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		} catch (Exception e) {
			A a;
			String hp = getTeleponPegawai();
			(a = new A(hp)).setParent(vbox);
			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			CalonPegawai calonCalonPegawai = this;

			FileFotoLain fotocalonPegawai = FileFotoLain.ambil(calonCalonPegawai.getId(),
					FotoCalonPegawai.DEFAULT_JENIS, FotoCalonPegawai.class);

			if (fotocalonPegawai != null && fotocalonPegawai.ambilFile() != null) {
				parameters.put("foto", fotocalonPegawai.ambilFile().getAbsolutePath());
			} else if (fotocalonPegawai != null && fotocalonPegawai.getLink() != null
					&& fotocalonPegawai.getLink().toLowerCase().contains("dropbox")) {
				parameters.put("foto", fotocalonPegawai.dropboxLinkRaw());
			} else if (fotocalonPegawai != null && fotocalonPegawai.getGdrive() != null) {
				parameters.put("foto", fotocalonPegawai.exportGDriveUrl());
			} else if (fotocalonPegawai != null) {
				parameters.put("foto", fotocalonPegawai.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
			}

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/recruitment/CalonPegawai.java:997");
		}
	}
}
