package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.joda.time.Years;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;

import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;

/**
 * Entity MASTER <b>gelombang pendaftaran PSB/PPDB</b> (Penerimaan Peserta Didik Baru jenjang
 * sekolah) &mdash; tabel {@code sekolah.gelombang_pendaftaran_psb}. Satu baris entity ini adalah
 * satu "gelombang"/"batch" penerimaan siswa baru pada satu {@link Sekolah}, satu tahun ajaran,
 * dengan rentang tanggal buka&ndash;tutup sendiri.
 *
 * <p><b>Kedudukan dalam sistem.</b> Ini bukan sekadar tabel referensi. Baris entity ini adalah
 * <i>berkas konfigurasi seluruh perjalanan seorang calon siswa</i>: mulai dari formulir mana yang
 * dipakai untuk mendaftar, tombol apa saja yang muncul di portal PPDB publik, tagihan apa yang
 * ditagihkan pada tiap tahap, siapa yang boleh mendaftar (alumni/anak pegawai/saudara/rentang
 * umur), sampai kapan calon siswa berubah menjadi {@link Siswa} ber-NIS. Hampir semua keputusan
 * runtime modul PSB dibaca dari properti entity ini, bukan dari tabel konfigurasi global.</p>
 *
 * <h3>Layar dan gerbang hak akses</h3>
 * <p>Layar CRUD-nya adalah {@code /pages/master/sekolah/gelombang_pendaftaran_psb.zul} yang
 * dikendalikan {@code ais.action.master.sekolah.GelombangPendaftaranPsbAction}. Layar ini
 * <b>terdaftar sebagai menu resmi</b> (id menu {@code 54327}, judul "Gelombang Pendaftaran", di
 * bawah grup menu {@code 5702} "Penerimaan Siswa Baru") &mdash; lihat {@code
 * ais.common.MenuInitializer} dan {@code ais.common.MenuSnapshotData}.</p>
 *
 * <p><b>Verifikasi gerbang layar master &mdash; POSITIF (bergerbang benar).</b> Berbeda dengan
 * anomali yang tercatat pada layar master {@code KelasSiswa}, layar master gelombang PSB ini
 * memasang gerbang secara lengkap dan konsisten:</p>
 * <ul>
 *   <li>{@code doBeforeCompose()} memanggil {@code Common.doCheckSecurity()} (gerbang sesi/RBAC
 *       level halaman) sebelum komponen dibangun;</li>
 *   <li>tombol Tambah: {@code add.setVisible(CommonPrivilages.checkPrevilages(CREATE))};</li>
 *   <li>flag {@code edit}/{@code delete} diambil dari {@code checkPrevilages(UPDATE)} dan
 *       {@code checkPrevilages(DELETE)}, lalu diteruskan ke
 *       {@code Common.copyEditDeleteButtons(edit, delete, ...)} pada tiap baris;</li>
 *   <li>centang "Aktif" di grid di-{@code setDisabled(!edit)} &mdash; jalur mutasi cepat ikut
 *       tergerbang;</li>
 *   <li>tombol Unggah (impor massal) bahkan menuntut ketiga hak sekaligus:
 *       {@code upload.setVisible(add.isVisible() &amp;&amp; edit &amp;&amp; delete)}.</li>
 * </ul>
 * <p>Jadi pola yang tercatat untuk keluarga PSB tetap berlaku: broken access control terkonsentrasi
 * di panel DETAIL/helper, sementara layar MASTER PSB cenderung benar-gerbang.</p>
 *
 * <p><b>Namun: pewarisan hak lewat menu induk, dan ini simpul TERBESAR yang tercatat sejauh
 * ini.</b> Layar master ini menyisipkan <b>sembilan</b> layar CRUD lain sebagai tab
 * ({@code MyInclude}), dan <b>tidak satu pun</b> dari sembilan berkas ZUL itu terdaftar sebagai
 * menu di {@code MenuInitializer}/{@code MenuSnapshotData} (diperiksa satu per satu, nol hasil).
 * Artinya seluruh hak CRUD atas sembilan master di bawah ini diwarisi dari SATU hak menu
 * "Gelombang Pendaftaran":</p>
 * <ol>
 *   <li>{@code kelompok_gelombang.zul} &rarr; {@link KelompokGelombang};</li>
 *   <li>{@code format_nis.zul} &rarr; {@link FormatNis} (tab "Pengaturan NIS", lihat di bawah);</li>
 *   <li>{@code kebutuhan_khusus_siswa.zul} &rarr; {@code KebutuhanKhususSiswa};</li>
 *   <li>{@code galeri_foto_psb.zul};</li>
 *   <li>{@code verifikasi_kelengkapan_calon_siswa.zul} &rarr;
 *       {@link VerifikasiKelengkapanCalonSiswa};</li>
 *   <li>{@code parameter_verifikasi_calon_siswa.zul};</li>
 *   <li>{@code paket_psb.zul} &rarr; {@link PaketPsb};</li>
 *   <li>{@code konfigurasi_biodata_calon_siswa.zul};</li>
 *   <li>{@code matapelajaran_sekolah.zul}.</li>
 * </ol>
 * <p>Panel DETAIL tiap baris menambah delapan tab lagi yang semuanya menerima id gelombang lewat
 * <b>parameter URL</b> ({@code ujian_psb.zul?gelombangPendaftaranPsb=&lt;id&gt;},
 * {@code ruang_psb.zul}, {@code jadwal_ujian_psb.zul}, {@code jadwal_pertemuan_psb.zul},
 * {@code kelas_psb.zul}, {@code gelombang_pendaftaran_psb_punya_matapelajaran.zul},
 * {@code parameter_tambahan_gelombang.zul},
 * {@code gelombang_punya_parameter_verifikasi_calon_siswa.zul}) &mdash; bentuk yang sama dengan
 * pola IDOR parameter-URL yang sudah dieskalasi untuk keluarga Action lain.</p>
 *
 * <h3>Relasi TERVERIFIKASI</h3>
 * <p><b>Ke atas (pemilik/tenant dan pengelompokan):</b> {@link #getSekolah()} dan
 * {@link #getYayasan()} (kolom {@code sekolah_id}/{@code yayasan_id}),
 * {@link #getKelompokGelombang()} ke {@link KelompokGelombang} (kolom
 * {@code kelompok_gelombang}), {@link #getPenjurusanSekolah()} ke {@link PenjurusanSekolah},
 * {@link #getStatusAwalSiswa()} ke {@link StatusAwalSiswa}, {@link #getAlumniDari()} ke
 * {@link Sekolah} lain, dan TIGA relasi biaya ke {@link JenisBiayaSekolah}
 * ({@link #getJenisBiayaSekolah()} tahap daftar, {@link #getJenisBiayaSekolahTerverifikasi()}
 * tahap verifikasi, {@link #getJenisBiayaSekolahLulus()} tahap daftar ulang).</p>
 *
 * <p><b>Ke samping (many-to-many):</b> {@link #getVerifikasiKelengkapanCalonSiswas()} lewat tabel
 * gabung {@code sekolah.gelombang_punya_verifikasi_siswa} ({@code gelombang} &harr;
 * {@code verifikasi}).</p>
 *
 * <p><b>Ke bawah (entity anak yang memegang FK ke entity ini &mdash; diverifikasi dengan menyisir
 * seluruh {@code ais/database/model/**} untuk field {@code GelombangPendaftaranPsb}):</b>
 * {@link CalonSiswa} (pendaftar itu sendiri), {@link Siswa} (jejak gelombang asal setelah
 * diterima), {@link UjianPSB}, {@link RuangPSB}, {@link JadwalUjianPSB},
 * {@link JadwalPertemuanPSB}, {@link KelasSiswaPSB}, {@link InterviewCalonSiswa},
 * {@link GelombangPendaftaranPsbPunyaMatapelajaran} (mata pelajaran rapor yang diverifikasi),
 * {@link GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa},
 * {@link PaketPsbPunyaGelombangPendaftaranPsb} (pembatasan gelombang ke paket tertentu),
 * {@link ParameterTambahanGelombangPendaftaranPsb} (field formulir tambahan), dan
 * {@link PengaturanBiaya}.</p>
 *
 * <p><b>Relasi TAK LANGSUNG yang perlu diwaspadai:</b>
 * {@link RuangGelombangPendaftaranPsbPSB} &mdash; meski namanya menyebut gelombang, entity itu
 * <b>tidak memiliki FK</b> ke entity ini; ia mencapai gelombang lewat dua jalur berbeda
 * ({@code getRuangPSB().getGelombangPendaftaranPsb()} dan
 * {@code getCalonSiswa().getGelombangPendaftaranPsb()}) yang bisa saling bertentangan.</p>
 *
 * <h3>Tab "Pengaturan NIS" dan {@code langsungDapatNisSaatDaftar} &mdash; TERVERIFIKASI</h3>
 * <p>Dua klaim yang tercatat sebelumnya diverifikasi ulang dari sisi berkas ini dan keduanya
 * <b>benar</b>:</p>
 * <ol>
 *   <li>{@code GelombangPendaftaranPsbAction.onPengaturanNIS(Event)} menyisipkan
 *       {@code /pages/master/sekolah/format_nis.zul} ke {@code Tabpanel tabPengaturanNIS}. Layar
 *       {@link FormatNis} memang HANYA hidup sebagai tab di layar master ini &mdash; ia tidak punya
 *       entri menu sendiri, sehingga seluruh CRUD format NIS (yang menentukan identitas resmi
 *       siswa) diwarisi dari hak menu gelombang pendaftaran.</li>
 *   <li>Field {@link #getLangsungDapatNisSaatDaftar()} memang ada di entity ini dan memang salah
 *       satu pemicu pembangkitan NIS otomatis pada pendaftaran mandiri PPDB publik. Rantainya
 *       lengkap: sembilan varian formulir publik ({@code PPDB1}, {@code PPDB2},
 *       {@code PPDB_Alumni}, {@code PPDB_Simple} s.d. {@code PPDB_Simple6}) plus
 *       {@code CalonSiswaAction} membaca flag ini, lalu memuat kelas generator dari konfigurasi
 *       {@code class_untuk_generate_nis} (bawaan {@code DefaultNisGenerator}), <b>menyetel
 *       {@code calonSiswa.setTelahDiterima(true)}</b>, dan memanggil
 *       {@code CommonPSB.onGenerateNis(calonSiswa, nisGenerator, false)}. {@code onGenerateNis}
 *       kemudian mencari {@link FormatNis} aktif milik sekolah calon siswa dan merakit NIS lewat
 *       {@code CommonPSB.generateCode(FormatNis, CalonSiswa)}.</li>
 * </ol>
 * <p><b>Konsekuensi non-obvious:</b> pada jalur {@code langsungDapatNisSaatDaftar} ini
 * {@link #getKuotaDiterima()} <b>tidak diperiksa sama sekali</b>. Pemeriksaan kuota hanya ada di
 * jalur unggah massal ({@code CommonPSB} baris ~900) dan di layar admin
 * ({@code CalonSiswaAction} baris ~1647). Jadi mengaktifkan centang "Saat pertama kali daftar,
 * calon siswa wajib otomatis diteima dan mendapatkan NIS" membuat setiap pendaftar publik
 * langsung berstatus DITERIMA + ber-NIS <b>melewati pagar kuota</b>. Ini menumpuk di atas
 * kelemahan keunikan NIS yang sudah tercatat pada {@link FormatNis}.</p>
 *
 * <h3>Empat mekanisme "otomatis dapat NIS" yang hidup berdampingan</h3>
 * <p>Entity ini menyimpan EMPAT saklar berbeda yang semuanya bisa berakhir pada penerbitan NIS,
 * tanpa validasi saling-eksklusif di {@code onSave()}:
 * {@link #getLangsungDapatNisSaatDaftar()} (saat daftar),
 * {@link #getOtomatisDiterimaKetikaSudahBayarReg()} (diterima saja, tanpa NIS),
 * {@link #getOtomatisDapatNisKetikaSudahBayarReg()} (diterima + NIS setelah bayar pendaftaran),
 * dan {@link #getOtomatisDapatNisKetikaSudahBayarDaftarUlang()} (NIS setelah bayar daftar ulang).
 * Mencentang beberapa sekaligus dibolehkan.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} dan hook {@code onUpdate()} ({@code @PreUpdate}).</li>
 *   <li><b>Identitas dan periode</b> &mdash; {@link #getId()}, {@link #getNama()},
 *       {@link #getTahunAjaran()}, {@link #getTahunMasuk()}, {@link #getMulai()},
 *       {@link #getSampai()}, {@link #getAktif()}.</li>
 *   <li><b>Cakupan tenant dan pengelompokan</b> &mdash; {@link #getSekolah()},
 *       {@link #getYayasan()}, {@link #getKelompokGelombang()},
 *       {@link #getPenjurusanSekolah()}, {@link #ambilPenjurusanSekolah()}.</li>
 *   <li><b>Kebijakan biaya</b> &mdash; tiga relasi {@link JenisBiayaSekolah},
 *       {@link #getSesuaiKelas()}, {@link #getSesuaiKelasSaatDiterima()},
 *       {@link #getMunculkanTagihanSetelahDaftar()}, dan mesin
 *       {@link #chekSyaratBayar(CalonSiswa)}.</li>
 *   <li><b>Syarat kelayakan pendaftar</b> &mdash; {@link #getHarusSebagaiAlumni()} dan
 *       kelompok "alumni" ({@link #getAlumniDari()}, {@link #getTingkatDariAlumni()},
 *       {@link #getKelasDariAlumni()}, {@link #getTahunAkademikAlumni()},
 *       {@link #getTerdapatVerifikasiDenganNikAlumni()}),
 *       {@link #getHarusSebagaiSaudara()}/{@link #getTerdapatVerifikasiDenganNikSibling()},
 *       {@link #getHanyaUntukAnakPegawai()}, {@link #getSiswaPindahanBolehMendaftar()}, serta
 *       pembatas umur ({@link #getDibatasiUmur()}, {@link #getUmurminimal()},
 *       {@link #getUmurmaksimal()}, {@link #getUmurDihitungTanggal()},
 *       {@link #chekUmur(GelombangPendaftaranPsb, MyDatebox)}).</li>
 *   <li><b>Tata letak portal PPDB</b> &mdash; deretan {@code getTampil*()} yang menentukan tombol
 *       dan kartu apa yang tampil bagi calon siswa, plus {@link #getClassUntukPendaftaran()} dan
 *       {@link #getClassUntukMelengkapiBerkas()} yang memilih kelas formulir yang di-{@code
 *       Class.forName}.</li>
 *   <li><b>Penerimaan dan NIS</b> &mdash; {@link #getKuotaDiterima()},
 *       {@link #getStatusAwalSiswa()}, empat saklar NIS di atas,
 *       {@link #getTampilkanQrCodeMahasiswaSetelahDapatNim()}.</li>
 *   <li><b>Prasarana ujian</b> &mdash; {@link #chekKuotaPendaftar()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pembaca</h3>
 * <ol>
 *   <li><b>Banyak getter di kelas ini MENULIS ke field-nya sendiri (write-back), sebagian
 *       DESTRUKTIF.</b> {@link #getYayasan()} menimpa yayasan dengan
 *       {@code getSekolah().getYayasan()}; {@link #getTahunMasuk()} menimpa {@code tahunMasuk}
 *       hasil parsing {@code tahunAjaran}; {@link #getStatusAwalSiswa()} menanam
 *       {@code ConstantValues.BARU_SISWA} bila kosong; dan yang paling berbahaya
 *       {@link #getAlumniDari()} <b>menyetel field ke {@code null}</b> setiap kali
 *       {@code harusSebagaiAlumni} bernilai false. Karena entity ini {@code dynamicUpdate} dan
 *       ikut dirty-checking Hibernate, sekali saja getter itu dipanggil pada instance
 *       ter-attach lalu session di-flush, kolom {@code alumni_dari} yang tersimpan ikut
 *       terhapus permanen &mdash; membaca data bisa mengubah data.</li>
 *   <li><b>{@link #chekKuotaPendaftar()} salah nama total dan menulis ke DB dari jalur render.</b>
 *       Method itu sama sekali tidak memeriksa kuota; ia MENYEMAI baris {@link UjianPSB} "Online"
 *       dan {@link RuangPSB} "Online" (kapasitas 10000) bila belum ada. Pemanggilnya adalah
 *       {@code GelombangPendaftaranPsbRenderer.render()} &mdash; jadi sekadar MEMBUKA daftar
 *       gelombang menulis baris baru ke dua tabel, satu kali per gelombang yang tampil di
 *       halaman.</li>
 *   <li><b>{@link #chekUmur(GelombangPendaftaranPsb, MyDatebox)} FAIL-OPEN.</b> Seluruh badan
 *       pemeriksaan dibungkus {@code try/catch}, dan jalur exception jatuh ke {@code return true}
 *       (lolos). Tanggal lahir yang tidak bisa diproses karena itu MELEWATI pembatas umur, bukan
 *       ditolak.</li>
 *   <li><b>{@link #chekSyaratBayar(CalonSiswa)} mencampur domain dengan UI.</b> Method statis ini
 *       membuka {@code Session} Hibernate sendiri lalu memanggil {@code MyMessageboxConfig} &mdash;
 *       padahal ia juga dipanggil dari jalur REST {@code PsbCalonApi} yang tidak punya ZK Desktop.
 *       Di jalur itu peringatan UI-nya hilang diam-diam (ditelan {@code try/catch} di
 *       {@code tampilkanPeringatan}), sementara hasil boolean-nya tetap benar (fail-closed:
 *       exception apa pun menghasilkan {@code false}).</li>
 *   <li><b>Dua kolom mati.</b> {@link #getTampilWawancara()} dan {@link #getInfoSaatInterview()}
 *       tidak dibaca maupun ditulis oleh kode mana pun di jalur PSB (nol pemanggil di luar berkas
 *       ini). Keduanya adalah salinan dari kembaran jenjang perguruan tinggi
 *       {@code ais.database.model.GelombangPendaftaran}, yang di sana memang dipakai
 *       {@code InterviewCalonMahasiswaAction} dan punya kolom di layar PMB. Javadoc singkat yang
 *       sudah ada pada kedua getter itu bersifat aspiratif, bukan deskripsi perilaku nyata.</li>
 *   <li><b>Empat kolom syarat tanpa UI.</b> {@link #getHarusSebagaiAnakAlumni()},
 *       {@link #getHarusSebagaiSaudaraAlumni()}, {@link #getHarusSebagaiAnakPegawaiTetap()}, dan
 *       {@link #getHarusSebagaiAnakPegawaiHonorer()} tidak punya komponen di layar master
 *       (dua di antaranya bahkan dikomentari di {@code GelombangPendaftaranPsbAction}), sehingga
 *       nilainya selalu bawaan {@code false} kecuali disetel lewat impor/SQL.</li>
 *   <li><b>Bug salin-tempel di layar master (bukan di entity ini).</b>
 *       {@code GelombangPendaftaranPsbAction.init()} mengisi centang
 *       {@code terdapatVerifikasiDenganNikSibling} dari
 *       {@code getTerdapatVerifikasiDenganNikAlumni()} &mdash; properti yang SALAH &mdash; dan
 *       memberinya label "Terdapat verifikasi dengan NIK Alumni". {@code onSave()} menyimpan ke
 *       properti yang benar, jadi akibatnya nilai sibling selalu tampil mengikuti nilai alumni saat
 *       dialog dibuka dan bisa tertimpa tanpa disadari saat disimpan ulang.</li>
 *   <li><b>{@link #getKelasVerifikasiRapor()} punya lubang kondisi.</b> Nilai {@code null}
 *       mengembalikan default {@code "10:1;10:2;11:1;11:2;12:1;12:2"}, dan nilai non-kosong tanpa
 *       tanda titik dua juga mengembalikan default &mdash; tetapi string KOSONG lolos ke cabang
 *       {@code else} dan dikembalikan apa adanya sebagai string kosong.</li>
 *   <li><b>Cakupan tenant.</b> {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)} menolak
 *       objek tanpa id (menyimpan {@code null}), jadi gelombang tanpa tenant secara teknis
 *       mungkin ada. {@code initCriteria()} di layar master tidak memasang tapis
 *       sekolah/yayasan wajib &mdash; cakupannya sepenuhnya bergantung pada isi combobox
 *       pencarian yang dibangun {@code Common.initYayasanDanSekolahDanSemua}. Baris tanpa tenant
 *       tidak akan tersaring oleh tapis mana pun.</li>
 *   <li><b>{@link #getInformasi()} mengembalikan naskah panduan PPDB bawaan yang panjang bila
 *       kolomnya kosong</b>, tanpa write-back. Layar master mengisi {@code Textbox} dengan hasil
 *       getter ini, sehingga menyimpan tanpa menyunting apa pun akan MEMATERIALISASI naskah
 *       bawaan tersebut ke kolom {@code informasi}.</li>
 * </ol>
 *
 * <p><b>Catatan pewarisan.</b> Kelas ini {@code extends} {@link GeneralValueObject}, yang
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa.
 * Hibernate karena itu TIDAK memetakan properti kelas induk. Deklarasi ulang {@code id},
 * {@code oleh}, {@code olehId}, {@code tanggal_dirubah} dan sejenisnya di kelas ini
 * <b>bukan duplikasi keliru</b>, melainkan keharusan teknis agar kolom-kolom tersebut ikut
 * terpetakan. Helper {@code check(...)} yang dipakai hampir semua getter relasi berasal dari
 * kelas induk itu: ia menormalkan proxy lazy/objek detached menjadi instance kanonik yang aman
 * dibaca.</p>
 *
 * @see GeneralValueObject
 * @see CalonSiswa
 * @see FormatNis
 * @see KelompokGelombang
 * @see PaketPsbPunyaGelombangPendaftaranPsb
 * @see GelombangPendaftaranPsbPunyaMatapelajaran
 * @see GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
 * @see UjianPSB
 * @see RuangPSB
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "gelombang_pendaftaran_psb", schema = "sekolah")
public class GelombangPendaftaranPsb extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -4835727221706810019L;
	/** Kunci utama, kolom {@code id} (identity/auto-increment). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/** @return id pengguna terakhir yang mengubah baris ini; bisa {@code null} untuk data lama. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. <b>Perhatian:</b> nilai {@code null} atau kosong diabaikan
	 * diam-diam (method langsung {@code return}), sehingga atribusi audit yang sudah terisi tidak
	 * bisa dihapus lewat setter ini.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini; bisa {@code null}. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-{@code UPDATE}. Dipanggil oleh Hibernate, bukan oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat, lalu disegarkan
	 * oleh {@code AuditTimestampInterceptor} pada tiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Menyetel waktu perubahan terakhir. @param tanggal_dirubah stempel waktu baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom TIMESTAMP). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik gelombang ini (kolom {@code sekolah_id}) &mdash; sumbu cakupan tenant. */
	private Sekolah sekolah;
	/** Tanggal pendaftaran dibuka (kolom {@code mulai}, wajib). */
	private Date mulai;
	/** Nama gelombang seperti tampil ke calon siswa (kolom {@code nama}, wajib). */
	private String nama;
	/** Tanggal pendaftaran ditutup (kolom {@code sampai}, wajib). */
	private Date sampai;
	/** Tahun ajaran gelombang, format {@code "YYYY/YYYY"} (kolom {@code tahun_ajaran}, wajib). */
	private String tahunAjaran;
	/** Naskah panduan/informasi yang ditampilkan di portal PPDB (kolom {@code informasi}, TEXT). */
	private String informasi;
	/** Tahun masuk numerik, diturunkan dari bagian pertama {@link #tahunAjaran}. */
	private Integer tahunMasuk;
	/** Yayasan pemilik (kolom {@code yayasan_id}); selalu disegarkan dari {@link #sekolah}. */
	private Yayasan yayasan;
	/** Penanda gelombang masih aktif/dipakai; bawaan {@code true} bila kosong. */
	private Boolean aktif;
	/** Informasi singkat internal (tampil di kolom grid layar master). */
	private String keterangan;
	/** Paket tagihan yang wajib dibayar pada tahap PENDAFTARAN AWAL. */
	private JenisBiayaSekolah jenisBiayaSekolah;
	/** Paket tagihan yang wajib dibayar setelah berkas calon siswa TERVERIFIKASI. */
	private JenisBiayaSekolah jenisBiayaSekolahTerverifikasi;
	/** Paket tagihan DAFTAR ULANG, wajib dibayar setelah calon siswa dinyatakan diterima. */
	private JenisBiayaSekolah jenisBiayaSekolahLulus;
	/** Tampilkan form parameter tambahan pada FORMULIR PENDAFTARAN; bawaan {@code false}. */
	private Boolean tampilFormTambahanSaatRegistrasi;
	/** Tampilkan form parameter tambahan di portal setelah calon siswa login; bawaan {@code true}. */
	private Boolean tampilFormTambahanSaatLoginCalonMhs;

	/** Daftar formulir verifikasi kelengkapan berkas yang berlaku untuk gelombang ini (many-to-many). */
	private Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = new TreeSet<VerifikasiKelengkapanCalonSiswa>();
	/** Daftar kelas:semester rapor yang harus diisi calon siswa, format {@code "10:1;10:2;..."}. */
	private String kelasVerifikasiRapor;
	/** Status siswa yang diberikan saat calon siswa dikonversi menjadi {@link Siswa}. */
	private StatusAwalSiswa statusAwalSiswa;
	/** Syarat: hanya anak pegawai yang boleh mendaftar; bawaan {@code false}. */
	private Boolean hanyaUntukAnakPegawai;
	/** Syarat: calon siswa harus anak alumni. TIDAK punya komponen di layar master. */
	private Boolean harusSebagaiAnakAlumni;
	/** Syarat: calon siswa harus alumni sekolah tertentu ({@link #alumniDari}). */
	private Boolean harusSebagaiAlumni;
	/** Aktifkan verifikasi silang NIK terhadap data alumni. */
	private Boolean terdapatVerifikasiDenganNikAlumni;

	/** Sekolah asal yang diakui sebagai sumber alumni (kolom {@code alumni_dari}). */
	private Sekolah alumniDari;
	/** Tingkat asal alumni yang diterima; beberapa nilai dipisah koma, kosong = semua. */
	private String tingkatDariAlumni;
	/** Kelas asal alumni yang diterima; beberapa nilai dipisah koma, kosong = semua. */
	private String kelasDariAlumni;
	/** Tahun pelajaran kelulusan alumni yang diterima; dipisah koma, kosong = semua. */
	private String tahunAkademikAlumni;
	/** Batas jumlah calon siswa yang boleh berstatus diterima; bawaan 5000. */
	private Integer kuotaDiterima;
	/** Syarat: calon siswa harus punya saudara yang bersekolah di sini. */
	private Boolean harusSebagaiSaudara;
	/** Syarat: calon siswa harus saudara alumni. TIDAK punya komponen di layar master. */
	private Boolean harusSebagaiSaudaraAlumni;
	/** Syarat: calon siswa harus anak pegawai TETAP. TIDAK punya komponen di layar master. */
	private Boolean harusSebagaiAnakPegawaiTetap;
	/** Syarat: calon siswa harus anak pegawai HONORER. TIDAK punya komponen di layar master. */
	private Boolean harusSebagaiAnakPegawaiHonorer;
	/** Tampilkan formulir pembayaran segera setelah pendaftaran selesai. */
	private Boolean munculkanTagihanSetelahDaftar;
	/** Tagihan tahap awal mengikuti kelas/kelas les pilihan, bukan {@link #jenisBiayaSekolah}. */
	private Boolean sesuaiKelas;
	/** Terbitkan NIS + status diterima seketika saat pendaftaran; MELEWATI pagar {@link #kuotaDiterima}. */
	private Boolean langsungDapatNisSaatDaftar;
	/** Tandai calon siswa DITERIMA (tanpa NIS) begitu biaya pendaftaran lunas. */
	private Boolean otomatisDiterimaKetikaSudahBayarReg;
	/** Terbitkan NIS + status diterima begitu biaya pendaftaran lunas. */
	private Boolean otomatisDapatNisKetikaSudahBayarReg;
	/** Tagihan tahap daftar ulang mengikuti kelas pilihan, bukan {@link #jenisBiayaSekolahLulus}. */
	private Boolean sesuaiKelasSaatDiterima;
	/** Terbitkan NIS begitu biaya DAFTAR ULANG lunas. */
	private Boolean otomatisDapatNisKetikaSudahBayarDaftarUlang;

	/** Login-kan calon siswa secara otomatis begitu pendaftaran selesai; bawaan {@code true}. */
	private Boolean otomatisLoginSetelahDaftar;

	/** Nama kelas Java varian formulir pendaftaran ({@code PPDB1}, {@code PPDB_Simple3}, ...). */
	private String classUntukPendaftaran;
	/** Nama kelas Java varian formulir melengkapi berkas. */
	private String classUntukMelengkapiBerkas;
	/** Penjurusan khusus bila gelombang ini hanya untuk satu jurusan. */
	private PenjurusanSekolah penjurusanSekolah;
	/** Aktifkan pembatasan umur calon siswa. */
	private Boolean dibatasiUmur;
	/** Umur maksimal yang diperbolehkan (tahun); bawaan 27. */
	private Integer umurmaksimal;
	/** Umur minimal yang diperbolehkan (tahun); bawaan 0. */
	private Integer umurminimal;
	/** Tanggal acuan penghitungan umur; kosong = dihitung saat pendaftaran berlangsung. */
	private Date umurDihitungTanggal;

	/** Izinkan siswa pindahan ikut mendaftar; bawaan {@code true}. */
	private Boolean siswaPindahanBolehMendaftar;

	/** Tampilkan diagram alur pendaftaran di portal PPDB; bawaan {@code true}. */
	private Boolean tampilAlur;
	/** Tampilkan opsi pembayaran online (payment gateway); bawaan {@code true}. */
	private Boolean tampilPembayaranViaPaymentGateway;
	/** Tampilkan menu "Lengkapi Berkas" di portal PPDB; bawaan {@code true}. */
	private Boolean tampilLengkapiBerkas;
	/** Tampilkan kartu informasi kelulusan di portal PPDB; bawaan {@code true}. */
	private Boolean tampilInformasiKelulusan;
	/** Tampilkan menu ujian di portal PPDB; bawaan {@code true}. */
	private Boolean tampilUjian;
	/** Tampilkan tombol cetak nomor registrasi; bawaan {@code true}. */
	private Boolean tampilCetakNoReg;
	/** Tampilkan tombol cetak biodata; bawaan {@code true}. */
	private Boolean tampilCetakBiodata;
	/** Tampilkan tombol cetak kartu ujian; bawaan {@code true}. */
	private Boolean tampilCetakKartuUjian;
	/** Cetak kartu ujian hanya boleh setelah berkas terverifikasi; bawaan {@code false}. */
	private Boolean cetakKartuUjianHarusVerifikasiBerkas;
	/** Tampilkan keterangan penerimaan di portal PPDB; bawaan {@code true}. */
	private Boolean tampilKeteranganDiterima;
	/** Tampilkan tombol logout di portal PPDB; bawaan {@code true}. */
	private Boolean tampilLogout;
	/** Pengelompokan beberapa gelombang di bawah satu payung ({@link KelompokGelombang}). */
	private KelompokGelombang kelompokGelombang;
	/** Tampilkan form lampiran langsung di halaman utama portal; bawaan {@code true}. */
	private Boolean tampilFormLampiranDiHalamanUtama;
	/** Tampilkan form parameter tambahan langsung di halaman utama portal; bawaan {@code true}. */
	private Boolean tampilFormTambahanDiHalamanUtama;
	/** Aktifkan verifikasi silang NIK terhadap data saudara (sibling). */
	private Boolean terdapatVerifikasiDenganNikSibling;
	/** Tampilkan QR-Code identitas siswa setelah NIS terbit; bawaan {@code true}. */
	private Boolean tampilkanQrCodeMahasiswaSetelahDapatNim;
	/** Kolom mati pada jalur PSB: tidak dibaca/ditulis kode mana pun. Salinan dari modul PMB. */
	private Boolean tampilWawancara;
	/** Kolom mati pada jalur PSB: tidak dibaca/ditulis kode mana pun. Salinan dari modul PMB. */
	private String  infoSaatInterview;

	/**
	 * Daftar formulir verifikasi kelengkapan berkas yang berlaku untuk gelombang ini.
	 *
	 * <p>Relasi many-to-many lewat tabel gabung
	 * {@code sekolah.gelombang_punya_verifikasi_siswa} (kolom {@code gelombang} &harr;
	 * {@code verifikasi}). Hanya {@code CascadeType.MERGE} yang diaktifkan, jadi menyimpan
	 * gelombang tidak membuat baris {@link VerifikasiKelengkapanCalonSiswa} baru &mdash; hanya
	 * mengaitkan yang sudah ada. Layar master mengisi koleksi ini lewat deretan centang yang
	 * dibangun {@code initKelengkapanBerkas()} dan menggantinya utuh di {@code onSave()}.</p>
	 *
	 * @return himpunan formulir verifikasi; tidak pernah {@code null} (bawaan {@link TreeSet}
	 *         kosong).
	 */
	@ManyToMany(targetEntity = VerifikasiKelengkapanCalonSiswa.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "gelombang_punya_verifikasi_siswa", joinColumns = @JoinColumn(name = "gelombang"), inverseJoinColumns = @JoinColumn(name = "verifikasi"), schema = "sekolah")
	public Set<VerifikasiKelengkapanCalonSiswa> getVerifikasiKelengkapanCalonSiswas() {
		return verifikasiKelengkapanCalonSiswas;
	}

	/**
	 * Mengganti seluruh himpunan formulir verifikasi kelengkapan berkas.
	 *
	 * @param verifikasiKelengkapanCalonSiswas himpunan pengganti (layar master mengirim
	 *        {@code HashSet} hasil pilihan pengguna).
	 */
	public void setVerifikasiKelengkapanCalonSiswas(
			Set<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas) {
		this.verifikasiKelengkapanCalonSiswas = verifikasiKelengkapanCalonSiswas;
	}

	/** Konstruktor kosong yang diwajibkan Hibernate/JPA. */
	public GelombangPendaftaranPsb() {
	}

	/**
	 * Konstruktor lengkap warisan hbm2java untuk kolom-kolom {@code nullable = false}.
	 *
	 * @param id kunci utama.
	 * @param mulai tanggal pendaftaran dibuka.
	 * @param nama nama gelombang.
	 * @param sampai tanggal pendaftaran ditutup.
	 * @param tahunAjaran tahun ajaran format {@code "YYYY/YYYY"}.
	 */
	public GelombangPendaftaranPsb(long id, Date mulai, String nama, Date sampai, String tahunAjaran) {
		this.id = id;
		this.mulai = mulai;
		this.nama = nama;
		this.sampai = sampai;
		this.tahunAjaran = tahunAjaran;
	}

	/** @return kunci utama baris; {@code null} untuk objek yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Menyetel kunci utama. @param id nilai kunci utama. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sekolah pemilik gelombang ini &mdash; sumbu cakupan tenant utama entity.
	 *
	 * <p><b>Write-back:</b> hasil {@code check(...)} dari {@link GeneralValueObject} ditulis balik
	 * ke field, sehingga proxy lazy/objek detached digantikan instance kanonik. Tidak destruktif
	 * (nilai non-null tidak pernah menjadi null karena pemanggilan ini).</p>
	 *
	 * @return sekolah pemilik; bisa {@code null} untuk gelombang tanpa tenant.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik. Objek tanpa id dianggap belum tersimpan dan <b>disimpan sebagai
	 * {@code null}</b> &mdash; ini yang memungkinkan adanya gelombang tanpa tenant.
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id menjadikan kolom kosong.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Yayasan pemilik gelombang ini.
	 *
	 * <p><b>Getter write-back yang menimpa data:</b> bila {@link #getSekolah()} tidak
	 * {@code null}, field {@code yayasan} SELALU ditimpa dengan {@code sekolah.getYayasan()}.
	 * Nilai yayasan yang tersimpan di kolom {@code yayasan_id} karena itu tidak pernah menang atas
	 * yayasan turunan sekolah &mdash; dan karena entity {@code dynamicUpdate}, koreksi itu ikut
	 * tersimpan begitu session di-flush. Efeknya menguntungkan (mencegah yayasan menyimpang dari
	 * sekolah) tetapi tetap merupakan mutasi tersembunyi di jalur baca.</p>
	 *
	 * @return yayasan pemilik; bisa {@code null}.
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
	 * Menyetel yayasan pemilik. Sama seperti {@link #setSekolah(Sekolah)}, objek tanpa id disimpan
	 * sebagai {@code null}. Nilai ini akan ditimpa lagi oleh {@link #getYayasan()} bila sekolah
	 * terisi.
	 *
	 * @param yayasan yayasan pemilik; {@code null}/tanpa id mengosongkan kolom.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Penjurusan khusus bila gelombang ini hanya melayani satu jurusan.
	 *
	 * <p>Di layar master, baris ini hanya ditampilkan bila sekolah terpilih memiliki
	 * {@code getPenjurusanBolehDipilihSaatPsb()} bernilai {@code true} dan punya minimal satu
	 * {@link PenjurusanSekolah} aktif ber-{@code tampilkanDiPpdb}. Nilai {@code null} berarti
	 * "Semua Penjurusan".</p>
	 *
	 * @return penjurusan khusus; {@code null} bila gelombang terbuka untuk semua jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penjurusan_sekolah_id", nullable = true)
	public PenjurusanSekolah getPenjurusanSekolah() {
		penjurusanSekolah = check(penjurusanSekolah);
		return penjurusanSekolah;
	}

	/**
	 * Varian non-getter dari {@link #getPenjurusanSekolah()} dengan badan identik. Disediakan agar
	 * pemanggil dapat membaca penjurusan tanpa menyentuh nama properti Hibernate &mdash; pola yang
	 * sama dipakai {@code CalonSiswa.ambilPenjurusanSekolah()} dan
	 * {@code Siswa} saat mencari penjurusan cadangan.
	 *
	 * @return penjurusan khusus; {@code null} bila tidak dibatasi jurusan.
	 */
	public PenjurusanSekolah ambilPenjurusanSekolah() {
		penjurusanSekolah = check(penjurusanSekolah);
		return penjurusanSekolah;
	}

	/** Menyetel penjurusan khusus. @param penjurusanSekolah penjurusan, atau {@code null} untuk semua. */
	public void setPenjurusanSekolah(PenjurusanSekolah penjurusanSekolah) {
		this.penjurusanSekolah = penjurusanSekolah;
	}

	/** @return tanggal pendaftaran dibuka (kolom DATE {@code mulai}, wajib diisi di layar). */
	@Temporal(TemporalType.DATE)
	@Column(name = "mulai", nullable = false, length = 13)
	public Date getMulai() {
		return this.mulai;
	}

	/** Menyetel tanggal buka pendaftaran. @param mulai tanggal mulai. */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/** @return nama gelombang seperti tampil ke calon siswa; wajib diisi di layar master. */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/** Menyetel nama gelombang. @param nama nama gelombang. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return tanggal pendaftaran ditutup (kolom DATE {@code sampai}, wajib diisi di layar). */
	@Temporal(TemporalType.DATE)
	@Column(name = "sampai", nullable = false, length = 13)
	public Date getSampai() {
		return this.sampai;
	}

	/** Menyetel tanggal tutup pendaftaran. @param sampai tanggal selesai. */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Tahun ajaran gelombang, format {@code "YYYY/YYYY"}.
	 *
	 * <p>Bila kolom kosong, dikembalikan tahun akademik berjalan
	 * ({@code Common.getCurrentTahunAkademik()}) tanpa menuliskannya kembali ke field &mdash; jadi
	 * nilai bawaan ini ikut bergeser mengikuti waktu untuk baris yang tahun ajarannya belum
	 * pernah diisi.</p>
	 *
	 * <p>Di layar master, combobox tahun ajaran di-{@code setDisabled} bila gelombang sudah punya
	 * minimal satu {@link CalonSiswa} <b>dan</b> pengguna bukan admin &mdash; pengaman agar tahun
	 * ajaran tidak berubah setelah ada pendaftar.</p>
	 *
	 * @return tahun ajaran tersimpan, atau tahun akademik berjalan bila kosong.
	 */
	@Column(name = "tahun_ajaran", nullable = false, length = 9)
	public String getTahunAjaran() {
		return this.tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
	}

	/** Menyetel tahun ajaran. @param tahunAjaran tahun ajaran format {@code "YYYY/YYYY"}. */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Naskah panduan/informasi yang ditampilkan kepada calon siswa di portal PPDB (kolom TEXT).
	 *
	 * <p>Bila kolom kosong atau hanya berisi spasi, dikembalikan naskah panduan PPDB bawaan yang
	 * panjang (tujuh butir tentang alur, satu email per pendaftar, kesesuaian dokumen Dukcapil,
	 * ketentuan unggah PDF, tanggung jawab orang tua, verifikasi panitia, kontak panitia). Naskah
	 * itu <b>tidak</b> ditulis balik ke field &mdash; tetapi layar master mengisi {@code Textbox}
	 * dari getter ini dan {@code onSave()} menyimpan isi {@code Textbox} apa adanya, sehingga
	 * membuka lalu menyimpan gelombang tanpa menyunting apa pun akan mematerialisasi naskah bawaan
	 * tersebut ke database.</p>
	 *
	 * @return naskah informasi tersimpan, atau naskah bawaan bila kolom kosong.
	 */
	@Column(name = "informasi", columnDefinition = "text")
	public String getInformasi() {
		return this.informasi == null || informasi.trim().isEmpty()
				? "1. Baca petunjuk panduan PPDB dan ikuti alur dengan cermat.\r\n"
						+ "2. Calon peserta didik baru hanya diperkenankan mendaftar satu kali dengan satu alamat email yang masih aktif\r\n"
						+ "3. Pengisian data sesuai dengan dokumen resmi yang berlaku (akta kelahiran dan kartu keluarga)\r\n"
						+ "4. KETENTUAN UNGGAH DOKUMEN (akte kelahiran dan kartu keluarga):\r\n"
						+ " a Isi dokumen sesuai dengan data yang tercatat di Dukcapil\r\n"
						+ " b. Apabila ada perbedaan pada masing-masing dokumen, mohon diurus terlebih dahulu di Dukcapil\r\n"
						+ " c. Scan dokumen asii dan unggah dalam bentuk PDF.\r\n"
						+ "5. Kebenaran data menjadi tanggungjawab orang tua/wali murid yang mendaftar\r\n"
						+ "6. Panitia akan melakukan proses verifikasi terhadap berkas pendaftaran yang sudah dikirimkan.\r\n"
						+ "7. Informasi lebih lanjut dapat menghubungi bagian pendaftaran:\r\n" + "...."
				: informasi;
	}

	/** Menyetel naskah informasi portal PPDB. @param informasi naskah bebas (TEXT). */
	public void setInformasi(String informasi) {
		this.informasi = informasi;
	}

	/**
	 * Tahun masuk numerik &mdash; nilai turunan dari {@link #tahunAjaran}.
	 *
	 * <p><b>Getter write-back:</b> bila field {@code tahunAjaran} (dibaca langsung, bukan lewat
	 * getter, jadi fallback tahun berjalan TIDAK berlaku di sini) terisi, {@code tahunMasuk}
	 * ditimpa dengan bagian sebelum tanda garis miring. Kolom {@code tahun_masuk} di database
	 * karena itu praktis tidak pernah bisa berbeda dari tahun ajaran.</p>
	 *
	 * <p><b>Risiko:</b> {@code Integer.parseInt} dipanggil tanpa penjaga. Tahun ajaran yang tidak
	 * berformat {@code "YYYY/..."} (mis. hasil impor yang kotor) melempar
	 * {@code NumberFormatException} langsung dari sebuah getter properti Hibernate &mdash;
	 * artinya kegagalan bisa muncul saat pemuatan/serialisasi, bukan hanya saat kode aplikasi
	 * memanggilnya.</p>
	 *
	 * @return tahun masuk; {@code null} hanya bila tahun ajaran dan kolom sama-sama kosong.
	 */
	@Column(name = "tahun_masuk")
	public Integer getTahunMasuk() {
		if (tahunAjaran != null) {
			tahunMasuk = Integer.parseInt(StringUtils.split(tahunAjaran, "/")[0]);
		}
		return this.tahunMasuk;
	}

	/**
	 * Menyetel tahun masuk. Nilai apa pun akan ditimpa lagi oleh {@link #getTahunMasuk()} selama
	 * tahun ajaran terisi.
	 *
	 * @param tahunMasuk tahun masuk numerik.
	 */
	public void setTahunMasuk(Integer tahunMasuk) {
		this.tahunMasuk = tahunMasuk;
	}

	/**
	 * Penanda gelombang masih aktif.
	 *
	 * <p>Bawaan {@code true} bila kolom kosong &mdash; jadi baris lama tanpa nilai dianggap AKTIF,
	 * bukan nonaktif. Layar master menyaring dengan
	 * {@code aktif IS NULL OR aktif = true} sehingga baris tanpa nilai memang ikut tampil.</p>
	 *
	 * @return {@code true} bila gelombang aktif (termasuk bila kolom kosong).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif. Dipanggil juga dari centang "Aktif" di grid layar master, yang
	 * langsung menyimpan lewat {@code Common.refreshSaveOrUpdate}. Centang itu dinonaktifkan bila
	 * pengguna tidak punya hak UPDATE.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return informasi singkat internal; string kosong (bukan {@code null}) bila belum diisi. */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/** Menyetel informasi singkat internal. @param keterangan teks bebas. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Paket tagihan yang wajib dibayar pada tahap PENDAFTARAN AWAL (kolom
	 * {@code jenis_biaya_sekolah_id}).
	 *
	 * <p>Dibaca {@link #chekSyaratBayar(CalonSiswa)} sebagai gerbang login/lanjut calon siswa,
	 * selama calon siswa belum berstatus diterima. Di layar master, kombobox ini disembunyikan
	 * bila {@link #getSesuaiKelas()} dicentang &mdash; karena saat itu tagihan dihitung dari
	 * kelas/kelas les yang dipilih calon siswa, bukan dari paket ini. Pilihan yang tersedia
	 * dibatasi {@link JenisBiayaSekolah} ber-{@code gunakanCalonSiswa = true} milik sekolah
	 * terpilih (atau tanpa sekolah).</p>
	 *
	 * @return paket biaya tahap pendaftaran; {@code null} berarti "tidak ada kewajiban membayar
	 *         untuk login calon siswa".
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_sekolah_id")
	public JenisBiayaSekolah getJenisBiayaSekolah() {
		jenisBiayaSekolah = check(jenisBiayaSekolah);
		return jenisBiayaSekolah;
	}

	/** Menyetel paket biaya tahap pendaftaran. @param jenisBiayaSekolah paket biaya, boleh {@code null}. */
	public void setJenisBiayaSekolah(JenisBiayaSekolah jenisBiayaSekolah) {
		this.jenisBiayaSekolah = jenisBiayaSekolah;
	}

	/**
	 * Paket tagihan DAFTAR ULANG yang wajib dibayar setelah calon siswa dinyatakan diterima
	 * (kolom {@code jenis_biaya_sekolah_lulus_id}).
	 *
	 * <p>Disembunyikan di layar master bila {@link #getSesuaiKelasSaatDiterima()} dicentang.
	 * Perhatikan bahwa {@link #chekSyaratBayar(CalonSiswa)} <b>tidak</b> memeriksa paket ini
	 * &mdash; gerbang daftar ulang ditangani jalur lain (mis.
	 * {@link #getOtomatisDapatNisKetikaSudahBayarDaftarUlang()}).</p>
	 *
	 * @return paket biaya daftar ulang; {@code null} berarti tidak ada kewajiban.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_sekolah_lulus_id")
	public JenisBiayaSekolah getJenisBiayaSekolahLulus() {
		jenisBiayaSekolahLulus = check(jenisBiayaSekolahLulus);
		return jenisBiayaSekolahLulus;
	}

	/** Menyetel paket biaya daftar ulang. @param jenisBiayaSekolahLulus paket biaya, boleh {@code null}. */
	public void setJenisBiayaSekolahLulus(JenisBiayaSekolah jenisBiayaSekolahLulus) {
		this.jenisBiayaSekolahLulus = jenisBiayaSekolahLulus;
	}

	/**
	 * Tampilkan form parameter tambahan pada FORMULIR PENDAFTARAN.
	 *
	 * <p><b>Getter write-back (ringan):</b> nilai {@code null} ditulis balik menjadi
	 * {@code false}, bukan sekadar dikembalikan &mdash; jadi membaca properti ini pada instance
	 * ter-attach menandai entity kotor dan memateri nilai {@code false} ke database saat flush.
	 * Pola yang sama dipakai {@link #getTampilFormTambahanSaatLoginCalonMhs()}; getter boolean
	 * lain di kelas ini memakai bentuk terner yang tidak menulis balik.</p>
	 *
	 * @return {@code true} bila form tambahan ditampilkan saat registrasi; bawaan {@code false}.
	 */
	public Boolean getTampilFormTambahanSaatRegistrasi() {
		if (tampilFormTambahanSaatRegistrasi == null) {
			tampilFormTambahanSaatRegistrasi = false;
		}
		return tampilFormTambahanSaatRegistrasi;
	}

	/**
	 * Menyetel penampilan form tambahan saat registrasi.
	 *
	 * @param tampilFormTambahanSaatRegistrasi nilai baru.
	 */
	public void setTampilFormTambahanSaatRegistrasi(Boolean tampilFormTambahanSaatRegistrasi) {
		this.tampilFormTambahanSaatRegistrasi = tampilFormTambahanSaatRegistrasi;
	}

	/**
	 * Tampilkan form parameter tambahan di portal setelah calon siswa login.
	 *
	 * <p>Sama seperti {@link #getTampilFormTambahanSaatRegistrasi()}, getter ini
	 * <b>menulis balik</b> nilai bawaan ke field &mdash; di sini bawaannya {@code true}. Nama
	 * properti masih memakai istilah "CalonMhs" (mahasiswa) karena diwarisi dari modul PMB,
	 * tetapi pada gelombang PSB ia berlaku untuk calon SISWA.</p>
	 *
	 * @return {@code true} bila form tambahan ditampilkan setelah login; bawaan {@code true}.
	 */
	public Boolean getTampilFormTambahanSaatLoginCalonMhs() {
		if (tampilFormTambahanSaatLoginCalonMhs == null) {
			tampilFormTambahanSaatLoginCalonMhs = true;
		}
		return tampilFormTambahanSaatLoginCalonMhs;
	}

	/**
	 * Menyetel penampilan form tambahan setelah login calon siswa.
	 *
	 * @param tampilFormTambahanSaatLoginCalonMhs nilai baru.
	 */
	public void setTampilFormTambahanSaatLoginCalonMhs(Boolean tampilFormTambahanSaatLoginCalonMhs) {
		this.tampilFormTambahanSaatLoginCalonMhs = tampilFormTambahanSaatLoginCalonMhs;
	}

	/**
	 * Daftar kelas dan semester rapor yang harus diisi calon siswa, dalam format
	 * {@code "kelas:semester;kelas:semester;..."} (bawaan
	 * {@code "10:1;10:2;11:1;11:2;12:1;12:2"} &mdash; enam semester jenjang SMA).
	 *
	 * <p><b>Lubang kondisi yang perlu diketahui:</b> nilai bawaan dipakai bila kolom
	 * {@code null} <b>atau</b> bila isinya tidak kosong tetapi tidak mengandung tanda titik dua.
	 * String KOSONG lolos kedua syarat itu dan dikembalikan apa adanya sebagai string kosong
	 * &mdash; jadi mengosongkan kotak isian di layar master menghasilkan "tanpa verifikasi rapor",
	 * sementara mengisinya dengan teks sembarang justru mengembalikan enam semester bawaan.</p>
	 *
	 * @return daftar kelas:semester, atau nilai bawaan; bisa string kosong.
	 */
	public String getKelasVerifikasiRapor() {
		return kelasVerifikasiRapor == null
				|| (!kelasVerifikasiRapor.trim().isEmpty() && !StringUtils.contains(kelasVerifikasiRapor, ":"))
						? "10:1;10:2;11:1;11:2;12:1;12:2"
						: kelasVerifikasiRapor.trim();
	}

	/** Menyetel daftar kelas:semester verifikasi rapor. @param kelasVerifikasiRapor daftar berformat. */
	public void setKelasVerifikasiRapor(String kelasVerifikasiRapor) {
		this.kelasVerifikasiRapor = kelasVerifikasiRapor;
	}

	/**
	 * Status yang diberikan kepada siswa hasil konversi calon siswa gelombang ini.
	 *
	 * <p><b>Getter write-back DESTRUKTIF ringan:</b> bila kolom kosong (atau relasinya tidak bisa
	 * dinormalkan {@code check(...)}), field ditimpa dengan konstanta
	 * {@code ConstantValues.BARU_SISWA}. Karena entity ini {@code dynamicUpdate}, sekali getter
	 * ini dipanggil pada instance ter-attach dan session di-flush, kolom
	 * {@code status_awal_siswa} yang semula NULL akan terisi permanen &mdash; membaca menulis.</p>
	 *
	 * @return status awal siswa; tidak pernah {@code null} (jatuh ke {@code BARU_SISWA}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_siswa")
	public StatusAwalSiswa getStatusAwalSiswa() {
		statusAwalSiswa = check(statusAwalSiswa);
		if (statusAwalSiswa == null) {
			statusAwalSiswa = ConstantValues.BARU_SISWA;
		}
		return statusAwalSiswa;
	}

	/** Menyetel status awal siswa. @param statusAwalSiswa status; {@code null} akan diganti bawaan oleh getter. */
	public void setStatusAwalSiswa(StatusAwalSiswa statusAwalSiswa) {
		this.statusAwalSiswa = statusAwalSiswa;
	}

	/**
	 * Syarat kelayakan: calon siswa harus anak alumni.
	 *
	 * <p><b>Kolom tanpa antarmuka:</b> komponen ZK untuk properti ini <b>dikomentari</b> di
	 * {@code GelombangPendaftaranPsbAction} (baik saat membangun form maupun di {@code onSave()}),
	 * sehingga nilainya tidak pernah bisa diubah dari layar master dan praktis selalu
	 * {@code false}.</p>
	 *
	 * @return {@code true} bila syarat anak alumni diberlakukan; bawaan {@code false}.
	 */
	public Boolean getHarusSebagaiAnakAlumni() {
		return harusSebagaiAnakAlumni == null ? false : harusSebagaiAnakAlumni;
	}

	/** Menyetel syarat anak alumni. @param harusSebagaiAnakAlumni nilai baru. */
	public void setHarusSebagaiAnakAlumni(Boolean harusSebagaiAnakAlumni) {
		this.harusSebagaiAnakAlumni = harusSebagaiAnakAlumni;
	}

	/**
	 * Syarat kelayakan: calon siswa harus ALUMNI sekolah tertentu.
	 *
	 * <p>Ini saklar induk kelompok "alumni". Bila dicentang, layar master menampilkan
	 * {@link #getAlumniDari()}, {@link #getKelasDariAlumni()}, {@link #getTingkatDariAlumni()},
	 * {@link #getTahunAkademikAlumni()} dan {@link #getTerdapatVerifikasiDenganNikAlumni()}; bila
	 * tidak, seluruh baris itu disembunyikan. {@code onSave()} menolak simpan bila saklar ini
	 * dicentang tetapi "Alumni dari" belum dipilih.</p>
	 *
	 * <p>Getter ini juga menjadi syarat yang dibaca {@link #getAlumniDari()} untuk memutuskan
	 * apakah relasi sekolah asal dipertahankan atau <b>dihapus</b>.</p>
	 *
	 * @return {@code true} bila hanya alumni yang boleh mendaftar; bawaan {@code false}.
	 */
	public Boolean getHarusSebagaiAlumni() {
		return harusSebagaiAlumni == null ? false : harusSebagaiAlumni;
	}

	/** Menyetel syarat alumni. @param harusSebagaiAlumni nilai baru. */
	public void setHarusSebagaiAlumni(Boolean harusSebagaiAlumni) {
		this.harusSebagaiAlumni = harusSebagaiAlumni;
	}

	/**
	 * Syarat kelayakan: calon siswa harus punya saudara yang bersekolah di sini.
	 *
	 * <p>Saklar induk bagi {@link #getTerdapatVerifikasiDenganNikSibling()} di layar master.</p>
	 *
	 * @return {@code true} bila syarat saudara diberlakukan; bawaan {@code false}.
	 */
	public Boolean getHarusSebagaiSaudara() {
		return harusSebagaiSaudara == null ? false : harusSebagaiSaudara;
	}

	/** Menyetel syarat punya saudara. @param harusSebagaiSaudara nilai baru. */
	public void setHarusSebagaiSaudara(Boolean harusSebagaiSaudara) {
		this.harusSebagaiSaudara = harusSebagaiSaudara;
	}

	/**
	 * Syarat kelayakan: calon siswa harus saudara ALUMNI.
	 *
	 * <p><b>Kolom tanpa antarmuka</b> &mdash; komponennya dikomentari di
	 * {@code GelombangPendaftaranPsbAction}, jadi nilainya tak pernah berubah dari layar master.</p>
	 *
	 * @return {@code true} bila syarat saudara alumni diberlakukan; bawaan {@code false}.
	 */
	public Boolean getHarusSebagaiSaudaraAlumni() {
		return harusSebagaiSaudaraAlumni == null ? false : harusSebagaiSaudaraAlumni;
	}

	/** Menyetel syarat saudara alumni. @param harusSebagaiSaudaraAlumni nilai baru. */
	public void setHarusSebagaiSaudaraAlumni(Boolean harusSebagaiSaudaraAlumni) {
		this.harusSebagaiSaudaraAlumni = harusSebagaiSaudaraAlumni;
	}

	/**
	 * Syarat kelayakan: calon siswa harus anak pegawai HONORER.
	 *
	 * <p><b>Kolom tanpa antarmuka</b> di layar master gelombang PSB &mdash; tidak ada komponen ZK
	 * maupun baris {@code onSave()} untuknya, sehingga praktis selalu {@code false}. Bandingkan
	 * dengan {@link #getHanyaUntukAnakPegawai()} yang punya centang sungguhan.</p>
	 *
	 * @return {@code true} bila dibatasi anak pegawai honorer; bawaan {@code false}.
	 */
	public Boolean getHarusSebagaiAnakPegawaiHonorer() {
		return harusSebagaiAnakPegawaiHonorer == null ? false : harusSebagaiAnakPegawaiHonorer;
	}

	/** Menyetel syarat anak pegawai honorer. @param harusSebagaiAnakPegawaiHonorer nilai baru. */
	public void setHarusSebagaiAnakPegawaiHonorer(Boolean harusSebagaiAnakPegawaiHonorer) {
		this.harusSebagaiAnakPegawaiHonorer = harusSebagaiAnakPegawaiHonorer;
	}

	/**
	 * Syarat kelayakan: calon siswa harus anak pegawai TETAP.
	 *
	 * <p><b>Kolom tanpa antarmuka</b> di layar master gelombang PSB, sama seperti
	 * {@link #getHarusSebagaiAnakPegawaiHonorer()}.</p>
	 *
	 * @return {@code true} bila dibatasi anak pegawai tetap; bawaan {@code false}.
	 */
	public Boolean getHarusSebagaiAnakPegawaiTetap() {
		return harusSebagaiAnakPegawaiTetap == null ? false : harusSebagaiAnakPegawaiTetap;
	}

	/** Menyetel syarat anak pegawai tetap. @param harusSebagaiAnakPegawaiTetap nilai baru. */
	public void setHarusSebagaiAnakPegawaiTetap(Boolean harusSebagaiAnakPegawaiTetap) {
		this.harusSebagaiAnakPegawaiTetap = harusSebagaiAnakPegawaiTetap;
	}

	/**
	 * Sekolah asal yang diakui sebagai sumber alumni (kolom {@code alumni_dari}).
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; hal paling berbahaya di kelas ini.</b> Bila
	 * {@link #getHarusSebagaiAlumni()} bernilai {@code false}, field {@code alumniDari}
	 * <b>disetel ke {@code null}</b>, bukan sekadar dikembalikan sebagai {@code null}. Karena
	 * entity ini {@code @org.hibernate.annotations.Entity(dynamicUpdate = true)} dan ikut
	 * dirty-checking, satu pemanggilan getter ini pada instance ter-attach yang diikuti flush
	 * (mis. saat merender daftar, mengekspor, atau menyalin data) akan <b>menghapus permanen</b>
	 * FK {@code alumni_dari} yang tersimpan. Membatalkan centang "harus alumni" lalu
	 * mencentangnya kembali karena itu tidak mengembalikan sekolah asal yang dulu dipilih.</p>
	 *
	 * @return sekolah asal alumni bila syarat alumni aktif; selalu {@code null} bila tidak.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alumni_dari")
	public Sekolah getAlumniDari() {
		if (getHarusSebagaiAlumni()) {
			alumniDari = check(alumniDari);
		} else {
			alumniDari = null;
		}

		return alumniDari;
	}

	/** Menyetel sekolah asal alumni. @param alumniDari sekolah asal; boleh {@code null}. */
	public void setAlumniDari(Sekolah alumniDari) {
		this.alumniDari = alumniDari;
	}

	/**
	 * Nama kelas Java varian FORMULIR PENDAFTARAN yang dipakai gelombang ini.
	 *
	 * <p>Nilainya adalah nama kelas yang terdaftar di {@code ConstantValues.treeMapFormPpdb}
	 * (diisi {@code InitDataHelper.reInitClass()}), mis. {@code PPDB1}, {@code PPDB2},
	 * {@code PPDB_Alumni}, {@code PPDB_Simple} s.d. {@code PPDB_Simple8}. Portal PPDB
	 * mem-{@code Class.forName} nama ini untuk memilih tata letak formulir. Nilai {@code null}
	 * berarti "Menggunakan Form PPDB Default".</p>
	 *
	 * <p>String kosong/berisi spasi dinormalkan menjadi {@code null} agar pemanggil cukup
	 * memeriksa {@code null} saja.</p>
	 *
	 * @return nama kelas formulir pendaftaran, atau {@code null} untuk formulir bawaan.
	 */
	public String getClassUntukPendaftaran() {
		return classUntukPendaftaran == null || classUntukPendaftaran.trim().isEmpty() ? null : classUntukPendaftaran;
	}

	/** Menyetel nama kelas formulir pendaftaran. @param classUntukPendaftaran nama kelas atau {@code null}. */
	public void setClassUntukPendaftaran(String classUntukPendaftaran) {
		this.classUntukPendaftaran = classUntukPendaftaran;
	}

	/**
	 * Nama kelas Java varian formulir MELENGKAPI BERKAS, dipilih dari daftar yang sama dengan
	 * {@link #getClassUntukPendaftaran()}. Nilai {@code null} berarti "Menggunakan Form Berkas
	 * PPDB Default". String kosong dinormalkan menjadi {@code null}.
	 *
	 * @return nama kelas formulir berkas, atau {@code null} untuk formulir bawaan.
	 */
	public String getClassUntukMelengkapiBerkas() {
		return classUntukMelengkapiBerkas == null || classUntukMelengkapiBerkas.trim().isEmpty() ? null
				: classUntukMelengkapiBerkas;
	}

	/** Menyetel nama kelas formulir berkas. @param classUntukMelengkapiBerkas nama kelas atau {@code null}. */
	public void setClassUntukMelengkapiBerkas(String classUntukMelengkapiBerkas) {
		this.classUntukMelengkapiBerkas = classUntukMelengkapiBerkas;
	}

	/**
	 * Tampilkan formulir pembayaran segera setelah pendaftaran selesai.
	 *
	 * <p>Dibaca kesembilan varian formulir PPDB. Perhatikan syarat gandanya di sana: formulir
	 * pembayaran hanya dimunculkan bila flag ini {@code true} <b>dan</b>
	 * {@link #getOtomatisLoginSetelahDaftar()} {@code false} &mdash; bila calon siswa langsung
	 * di-login-kan, tagihan ditampilkan lewat portal, bukan lewat dialog.</p>
	 *
	 * @return {@code true} bila tagihan langsung dimunculkan; bawaan {@code false}.
	 */
	public Boolean getMunculkanTagihanSetelahDaftar() {
		return munculkanTagihanSetelahDaftar == null ? false : munculkanTagihanSetelahDaftar;
	}

	/** Menyetel pemunculan tagihan setelah daftar. @param munculkanTagihanSetelahDaftar nilai baru. */
	public void setMunculkanTagihanSetelahDaftar(Boolean munculkanTagihanSetelahDaftar) {
		this.munculkanTagihanSetelahDaftar = munculkanTagihanSetelahDaftar;
	}

	/**
	 * Tagihan tahap AWAL mengikuti kelas/kelas les yang dipilih calon siswa, bukan paket
	 * {@link #getJenisBiayaSekolah()}.
	 *
	 * <p>Bila dicentang, layar master menyembunyikan kombobox
	 * {@link #getJenisBiayaSekolah()} <b>dan</b> {@link #getJenisBiayaSekolahTerverifikasi()}
	 * sekaligus.</p>
	 *
	 * @return {@code true} bila tagihan awal mengikuti kelas; bawaan {@code false}.
	 */
	public Boolean getSesuaiKelas() {
		return sesuaiKelas == null ? false : sesuaiKelas;
	}

	/** Menyetel mode tagihan sesuai kelas. @param sesuaiKelas nilai baru. */
	public void setSesuaiKelas(Boolean sesuaiKelas) {
		this.sesuaiKelas = sesuaiKelas;
	}

	/**
	 * Tagihan tahap DITERIMA (daftar ulang) mengikuti kelas/kelas les pilihan, bukan paket
	 * {@link #getJenisBiayaSekolahLulus()}. Bila dicentang, kombobox paket daftar ulang
	 * disembunyikan di layar master.
	 *
	 * @return {@code true} bila tagihan daftar ulang mengikuti kelas; bawaan {@code false}.
	 */
	public Boolean getSesuaiKelasSaatDiterima() {
		return sesuaiKelasSaatDiterima == null ? false : sesuaiKelasSaatDiterima;
	}

	/** Menyetel mode tagihan daftar ulang sesuai kelas. @param sesuaiKelasSaatDiterima nilai baru. */
	public void setSesuaiKelasSaatDiterima(Boolean sesuaiKelasSaatDiterima) {
		this.sesuaiKelasSaatDiterima = sesuaiKelasSaatDiterima;
	}

	/**
	 * Login-kan calon siswa secara otomatis begitu pendaftaran selesai.
	 *
	 * <p><b>Bawaan {@code true}</b> &mdash; jadi baris lama tanpa nilai berperilaku "langsung
	 * login". Ikut menentukan apakah dialog tagihan dimunculkan (lihat
	 * {@link #getMunculkanTagihanSetelahDaftar()}).</p>
	 *
	 * @return {@code true} bila calon siswa langsung login setelah mendaftar; bawaan {@code true}.
	 */
	public Boolean getOtomatisLoginSetelahDaftar() {
		return otomatisLoginSetelahDaftar == null ? true : otomatisLoginSetelahDaftar;
	}

	/** Menyetel login otomatis setelah daftar. @param otomatisLoginSetelahDaftar nilai baru. */
	public void setOtomatisLoginSetelahDaftar(Boolean otomatisLoginSetelahDaftar) {
		this.otomatisLoginSetelahDaftar = otomatisLoginSetelahDaftar;
	}

	/**
	 * Tandai calon siswa DITERIMA (tanpa menerbitkan NIS) begitu biaya pendaftaran lunas.
	 *
	 * <p>Salah satu dari empat saklar otomatisasi penerimaan/NIS yang bisa aktif bersamaan tanpa
	 * validasi saling-eksklusif di {@code onSave()}.</p>
	 *
	 * @return {@code true} bila penerimaan otomatis setelah bayar registrasi; bawaan {@code false}.
	 */
	public Boolean getOtomatisDiterimaKetikaSudahBayarReg() {
		return otomatisDiterimaKetikaSudahBayarReg == null ? false : otomatisDiterimaKetikaSudahBayarReg;
	}

	/** Menyetel penerimaan otomatis setelah bayar registrasi. @param otomatisDiterimaKetikaSudahBayarReg nilai baru. */
	public void setOtomatisDiterimaKetikaSudahBayarReg(Boolean otomatisDiterimaKetikaSudahBayarReg) {
		this.otomatisDiterimaKetikaSudahBayarReg = otomatisDiterimaKetikaSudahBayarReg;
	}

	/**
	 * Terbitkan NIS <b>dan</b> tandai diterima begitu biaya pendaftaran lunas.
	 *
	 * <p>Versi "lebih jauh" dari {@link #getOtomatisDiterimaKetikaSudahBayarReg()}: selain status
	 * diterima, jalur ini memanggil pembangkit NIS ({@link FormatNis} lewat {@code CommonPSB}).</p>
	 *
	 * @return {@code true} bila NIS terbit otomatis setelah bayar registrasi; bawaan {@code false}.
	 */
	public Boolean getOtomatisDapatNisKetikaSudahBayarReg() {
		return otomatisDapatNisKetikaSudahBayarReg == null ? false : otomatisDapatNisKetikaSudahBayarReg;
	}

	/** Menyetel penerbitan NIS otomatis setelah bayar registrasi. @param otomatisDapatNisKetikaSudahBayarReg nilai baru. */
	public void setOtomatisDapatNisKetikaSudahBayarReg(Boolean otomatisDapatNisKetikaSudahBayarReg) {
		this.otomatisDapatNisKetikaSudahBayarReg = otomatisDapatNisKetikaSudahBayarReg;
	}

	/**
	 * Terbitkan NIS begitu biaya DAFTAR ULANG lunas &mdash; jalur NIS "paling akhir" dari empat
	 * saklar otomatisasi yang tersedia.
	 *
	 * @return {@code true} bila NIS terbit setelah bayar daftar ulang; bawaan {@code false}.
	 */
	public Boolean getOtomatisDapatNisKetikaSudahBayarDaftarUlang() {
		return otomatisDapatNisKetikaSudahBayarDaftarUlang == null ? false
				: otomatisDapatNisKetikaSudahBayarDaftarUlang;
	}

	/**
	 * Menyetel penerbitan NIS otomatis setelah bayar daftar ulang.
	 *
	 * @param otomatisDapatNisKetikaSudahBayarDaftarUlang nilai baru.
	 */
	public void setOtomatisDapatNisKetikaSudahBayarDaftarUlang(Boolean otomatisDapatNisKetikaSudahBayarDaftarUlang) {
		this.otomatisDapatNisKetikaSudahBayarDaftarUlang = otomatisDapatNisKetikaSudahBayarDaftarUlang;
	}

	/**
	 * Umur MINIMAL calon siswa (tahun penuh) yang diperbolehkan mendaftar; bawaan {@code 0}
	 * (tidak ada batas bawah efektif). Hanya berlaku bila {@link #getDibatasiUmur()} aktif.
	 *
	 * @return umur minimal dalam tahun; bawaan 0.
	 */
	public Integer getUmurminimal() {
		return umurminimal == null ? 0 : umurminimal;
	}

	/** Menyetel umur minimal. @param umurminimal umur minimal dalam tahun. */
	public void setUmurminimal(Integer umurminimal) {
		this.umurminimal = umurminimal;
	}

	/**
	 * Umur MAKSIMAL calon siswa (tahun penuh) yang diperbolehkan mendaftar; bawaan {@code 27}.
	 *
	 * <p>Angka 27 juga menjadi bawaan konfigurasi global
	 * {@code nilai_umur_calon_siswa_dibatasi} yang dipakai layar master untuk mengisi kolom ini
	 * pertama kali. Hanya berlaku bila {@link #getDibatasiUmur()} aktif.</p>
	 *
	 * @return umur maksimal dalam tahun; bawaan 27.
	 */
	public Integer getUmurmaksimal() {
		return umurmaksimal == null ? 27 : umurmaksimal;
	}

	/** Menyetel umur maksimal. @param umurmaksimal umur maksimal dalam tahun. */
	public void setUmurmaksimal(Integer umurmaksimal) {
		this.umurmaksimal = umurmaksimal;
	}

	/**
	 * Saklar induk pembatasan umur. Bila {@code false},
	 * {@link #chekUmur(GelombangPendaftaranPsb, MyDatebox)} langsung meloloskan siapa pun.
	 *
	 * <p>Nilai awalnya di layar master diambil dari konfigurasi global
	 * {@code umur_calon_mahasiswa_dibatasi} bila kolom masih {@code null}.</p>
	 *
	 * @return {@code true} bila pembatasan umur aktif; bawaan {@code false}.
	 */
	public Boolean getDibatasiUmur() {
		return dibatasiUmur == null ? false : dibatasiUmur;
	}

	/** Menyetel saklar pembatasan umur. @param dibatasiUmur nilai baru. */
	public void setDibatasiUmur(Boolean dibatasiUmur) {
		this.dibatasiUmur = dibatasiUmur;
	}

	/**
	 * Memvalidasi umur calon siswa terhadap batas umur gelombang, sekaligus MENAMPILKAN dialog
	 * peringatan ZK bila melanggar.
	 *
	 * <p><b>Alur:</b> bila {@link #getDibatasiUmur()} {@code false}, langsung lolos. Bila aktif,
	 * umur dihitung {@code Years.yearsBetween(tanggalLahir, acuan)} dengan acuan
	 * {@link #getUmurDihitungTanggal()} bila diisi, atau waktu server bila kosong. Umur yang
	 * melebihi {@link #getUmurmaksimal()} atau kurang dari {@link #getUmurminimal()} memunculkan
	 * {@code MyMessageboxConfig} berisi tiga langkah perbaikan, lalu &mdash; lewat callback
	 * {@code EventListener} &mdash; memfokuskan, menyeleksi, dan menggulirkan layar ke kotak
	 * tanggal lahir. Perbandingannya inklusif di kedua ujung: umur yang tepat sama dengan batas
	 * maksimal atau minimal dinyatakan LOLOS.</p>
	 *
	 * <p><b>FAIL-OPEN &mdash; perlu diwaspadai:</b> seluruh badan pemeriksaan dibungkus
	 * {@code try/catch}. Exception apa pun (tanggal lahir tak terbaca, tidak ada ZK Desktop,
	 * masalah lokal Joda-Time) hanya dilaporkan lewat {@code Common.tampilErrorJikaAdmin} lalu
	 * eksekusi jatuh ke {@code return true} &mdash; artinya <b>pendaftar diloloskan</b>, bukan
	 * ditolak. Method ini juga mencetak baris {@code System.out} berisi umur pada setiap
	 * pemanggilan.</p>
	 *
	 * <p><b>Pemanggil:</b> kesembilan varian formulir PPDB publik ({@code PPDB1}, {@code PPDB2},
	 * {@code PPDB_Alumni}, {@code PPDB_Simple} s.d. {@code PPDB_Simple8}). Tiap formulir
	 * memanggilnya dua kali: sekali pada {@code onChange} kotak tanggal lahir (hasilnya diabaikan,
	 * sekadar memunculkan peringatan dini) dan sekali lagi di jalur simpan (hasilnya dipakai untuk
	 * membatalkan penyimpanan).</p>
	 *
	 * <p><b>Catatan desain:</b> method domain ini terikat pada UI ZK ({@link MyDatebox},
	 * {@code Clients.scrollIntoView}) sehingga tidak dapat dipakai dari jalur REST/batch.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang yang aturan umurnya diberlakukan; tidak boleh
	 *        {@code null} (dereferensi langsung).
	 * @param tanggalLahir kotak tanggal lahir di formulir; dipakai sebagai sumber nilai sekaligus
	 *        sasaran fokus saat peringatan ditutup.
	 * @return {@code true} bila umur memenuhi syarat, batas umur tidak aktif, <b>atau</b> terjadi
	 *         exception; {@code false} hanya bila pelanggaran batas benar-benar terdeteksi.
	 */
	public static boolean chekUmur(GelombangPendaftaranPsb gelombangPendaftaranPsb, final MyDatebox tanggalLahir) {
		if (gelombangPendaftaranPsb.getDibatasiUmur()) {
			try {
				int umur = gelombangPendaftaranPsb.getUmurmaksimal();
				int umurMin = gelombangPendaftaranPsb.getUmurminimal();

				int umurCalonSiswa = Years.yearsBetween(new org.joda.time.DateTime(tanggalLahir.getValue()),
						new org.joda.time.DateTime(gelombangPendaftaranPsb.getUmurDihitungTanggal() != null
								? gelombangPendaftaranPsb.getUmurDihitungTanggal()
								: ais.ui.util.WaktuUtil.getDate()))
						.getYears();
				System.out.println("umur => " + umur + ", umurCalonSiswa =>" + umurCalonSiswa);
				if (umurCalonSiswa > umur) {
					MyMessageboxConfig.showFormatCb(
							"Mohon maaf, Bapak/Ibu. Umur maksimal calon siswa yang diperbolehkan untuk mendaftar adalah {V1} tahun, sedangkan umur yang Bapak/Ibu masukkan adalah {V2} tahun. Langkah yang dapat dilakukan: (1) periksa kembali tanggal lahir yang dimasukkan; (2) pastikan umur calon siswa tidak melebihi batas maksimal; (3) perbaiki tanggal lahir lalu ulangi proses pendaftaran.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									tanggalLahir.focus();
									tanggalLahir.select();
									Clients.scrollIntoView(tanggalLahir);
								}
							}, umur, umurCalonSiswa);
					return false;
				}
				if (umurCalonSiswa < umurMin) {
					MyMessageboxConfig.showFormatCb(
							"Mohon maaf, Bapak/Ibu. Umur minimal calon siswa yang diperbolehkan untuk mendaftar adalah {V1} tahun, sedangkan umur yang Bapak/Ibu masukkan adalah {V2} tahun. Langkah yang dapat dilakukan: (1) periksa kembali tanggal lahir yang dimasukkan; (2) pastikan umur calon siswa telah memenuhi batas minimal; (3) perbaiki tanggal lahir lalu ulangi proses pendaftaran.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									tanggalLahir.focus();
									tanggalLahir.select();
									Clients.scrollIntoView(tanggalLahir);
								}
							}, umurMin, umurCalonSiswa);
					return false;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	/**
	 * Tanggal acuan penghitungan umur calon siswa (kolom DATE).
	 *
	 * <p>Bila {@code null}, {@link #chekUmur(GelombangPendaftaranPsb, MyDatebox)} memakai waktu
	 * server saat pendaftaran berlangsung &mdash; artinya batas umur bergeser sepanjang gelombang
	 * dibuka. Mengisi kolom ini (mis. 1 Juli tahun ajaran) membuat perhitungan umur konsisten
	 * untuk semua pendaftar.</p>
	 *
	 * @return tanggal acuan umur; {@code null} berarti "dihitung saat mendaftar".
	 */
	@Temporal(TemporalType.DATE)
	public Date getUmurDihitungTanggal() {
		return umurDihitungTanggal;
	}

	/** Menyetel tanggal acuan penghitungan umur. @param umurDihitungTanggal tanggal acuan atau {@code null}. */
	public void setUmurDihitungTanggal(Date umurDihitungTanggal) {
		this.umurDihitungTanggal = umurDihitungTanggal;
	}

	/**
	 * Izinkan siswa PINDAHAN ikut mendaftar lewat gelombang ini.
	 *
	 * <p><b>Bawaan {@code true}</b> &mdash; gelombang lama tanpa nilai membuka pintu bagi siswa
	 * pindahan.</p>
	 *
	 * @return {@code true} bila siswa pindahan boleh mendaftar; bawaan {@code true}.
	 */
	public Boolean getSiswaPindahanBolehMendaftar() {
		return siswaPindahanBolehMendaftar == null ? true : siswaPindahanBolehMendaftar;
	}

	/** Menyetel izin bagi siswa pindahan. @param siswaPindahanBolehMendaftar nilai baru. */
	public void setSiswaPindahanBolehMendaftar(Boolean siswaPindahanBolehMendaftar) {
		this.siswaPindahanBolehMendaftar = siswaPindahanBolehMendaftar;
	}

	/**
	 * Syarat kelayakan: gelombang ini hanya untuk ANAK PEGAWAI. Berbeda dari
	 * {@link #getHarusSebagaiAnakPegawaiTetap()}/{@link #getHarusSebagaiAnakPegawaiHonorer()},
	 * properti ini punya centang sungguhan di layar master ("Hanya Untuk Anak Pegawai") dan
	 * disimpan oleh {@code onSave()}.
	 *
	 * @return {@code true} bila dibatasi anak pegawai; bawaan {@code false}.
	 */
	public Boolean getHanyaUntukAnakPegawai() {
		return hanyaUntukAnakPegawai == null ? false : hanyaUntukAnakPegawai;
	}

	/** Menyetel pembatasan anak pegawai. @param hanyaUntukAnakPegawai nilai baru. */
	public void setHanyaUntukAnakPegawai(Boolean hanyaUntukAnakPegawai) {
		this.hanyaUntukAnakPegawai = hanyaUntukAnakPegawai;
	}

	/** @return {@code true} bila diagram alur pendaftaran ditampilkan di portal PPDB; bawaan {@code true}. */
	public Boolean getTampilAlur() {
		return tampilAlur == null ? true : tampilAlur;
	}

	/** Menyetel penampilan alur pendaftaran. @param tampilAlur nilai baru. */
	public void setTampilAlur(Boolean tampilAlur) {
		this.tampilAlur = tampilAlur;
	}

	/** @return {@code true} bila opsi pembayaran online (payment gateway) ditampilkan; bawaan {@code true}. */
	public Boolean getTampilPembayaranViaPaymentGateway() {
		return tampilPembayaranViaPaymentGateway == null ? true : tampilPembayaranViaPaymentGateway;
	}

	/** Menyetel penampilan pembayaran online. @param tampilPembayaranViaPaymentGateway nilai baru. */
	public void setTampilPembayaranViaPaymentGateway(Boolean tampilPembayaranViaPaymentGateway) {
		this.tampilPembayaranViaPaymentGateway = tampilPembayaranViaPaymentGateway;
	}

	/** @return {@code true} bila menu "Lengkapi Berkas" ditampilkan di portal PPDB; bawaan {@code true}. */
	public Boolean getTampilLengkapiBerkas() {
		return tampilLengkapiBerkas == null ? true : tampilLengkapiBerkas;
	}

	/** Menyetel penampilan menu lengkapi berkas. @param tampilLengkapiBerkas nilai baru. */
	public void setTampilLengkapiBerkas(Boolean tampilLengkapiBerkas) {
		this.tampilLengkapiBerkas = tampilLengkapiBerkas;
	}

	/** @return {@code true} bila kartu informasi kelulusan ditampilkan di portal; bawaan {@code true}. */
	public Boolean getTampilInformasiKelulusan() {
		return tampilInformasiKelulusan == null ? true : tampilInformasiKelulusan;
	}

	/** Menyetel penampilan informasi kelulusan. @param tampilInformasiKelulusan nilai baru. */
	public void setTampilInformasiKelulusan(Boolean tampilInformasiKelulusan) {
		this.tampilInformasiKelulusan = tampilInformasiKelulusan;
	}

	/** @return {@code true} bila menu ujian ditampilkan di portal PPDB; bawaan {@code true}. */
	public Boolean getTampilUjian() {
		return tampilUjian == null ? true : tampilUjian;
	}

	/** Menyetel penampilan menu ujian. @param tampilUjian nilai baru. */
	public void setTampilUjian(Boolean tampilUjian) {
		this.tampilUjian = tampilUjian;
	}

	/** @return {@code true} bila tombol cetak nomor registrasi ditampilkan; bawaan {@code true}. */
	public Boolean getTampilCetakNoReg() {
		return tampilCetakNoReg == null ? true : tampilCetakNoReg;
	}

	/** Menyetel penampilan tombol cetak nomor registrasi. @param tampilCetakNoReg nilai baru. */
	public void setTampilCetakNoReg(Boolean tampilCetakNoReg) {
		this.tampilCetakNoReg = tampilCetakNoReg;
	}

	/** @return {@code true} bila tombol cetak biodata ditampilkan; bawaan {@code true}. */
	public Boolean getTampilCetakBiodata() {
		return tampilCetakBiodata == null ? true : tampilCetakBiodata;
	}

	/** Menyetel penampilan tombol cetak biodata. @param tampilCetakBiodata nilai baru. */
	public void setTampilCetakBiodata(Boolean tampilCetakBiodata) {
		this.tampilCetakBiodata = tampilCetakBiodata;
	}

	/**
	 * Tampilkan tombol cetak kartu ujian di portal PPDB; bawaan {@code true}. Bekerja berpasangan
	 * dengan {@link #getCetakKartuUjianHarusVerifikasiBerkas()} yang menambahkan syarat berkas
	 * sudah terverifikasi.
	 *
	 * @return {@code true} bila tombol cetak kartu ujian ditampilkan.
	 */
	public Boolean getTampilCetakKartuUjian() {
		return tampilCetakKartuUjian == null ? true : tampilCetakKartuUjian;
	}

	/** Menyetel penampilan tombol cetak kartu ujian. @param tampilCetakKartuUjian nilai baru. */
	public void setTampilCetakKartuUjian(Boolean tampilCetakKartuUjian) {
		this.tampilCetakKartuUjian = tampilCetakKartuUjian;
	}

	/** @return {@code true} bila keterangan penerimaan ditampilkan di portal; bawaan {@code true}. */
	public Boolean getTampilKeteranganDiterima() {
		return tampilKeteranganDiterima == null ? true : tampilKeteranganDiterima;
	}

	/** Menyetel penampilan keterangan diterima. @param tampilKeteranganDiterima nilai baru. */
	public void setTampilKeteranganDiterima(Boolean tampilKeteranganDiterima) {
		this.tampilKeteranganDiterima = tampilKeteranganDiterima;
	}

	/** @return {@code true} bila tombol logout ditampilkan di portal PPDB; bawaan {@code true}. */
	public Boolean getTampilLogout() {
		return tampilLogout == null ? true : tampilLogout;
	}

	/** Menyetel penampilan tombol logout. @param tampilLogout nilai baru. */
	public void setTampilLogout(Boolean tampilLogout) {
		this.tampilLogout = tampilLogout;
	}

	/** @return {@code true} bila form lampiran ditampilkan langsung di halaman utama portal; bawaan {@code true}. */
	public Boolean getTampilFormLampiranDiHalamanUtama() {
		return tampilFormLampiranDiHalamanUtama == null ? true : tampilFormLampiranDiHalamanUtama;
	}

	/** Menyetel penampilan form lampiran di halaman utama. @param tampilFormLampiranDiHalamanUtama nilai baru. */
	public void setTampilFormLampiranDiHalamanUtama(Boolean tampilFormLampiranDiHalamanUtama) {
		this.tampilFormLampiranDiHalamanUtama = tampilFormLampiranDiHalamanUtama;
	}

	/** @return {@code true} bila form parameter tambahan ditampilkan di halaman utama portal; bawaan {@code true}. */
	public Boolean getTampilFormTambahanDiHalamanUtama() {
		return tampilFormTambahanDiHalamanUtama == null ? true : tampilFormTambahanDiHalamanUtama;
	}

	/** Menyetel penampilan form tambahan di halaman utama. @param tampilFormTambahanDiHalamanUtama nilai baru. */
	public void setTampilFormTambahanDiHalamanUtama(Boolean tampilFormTambahanDiHalamanUtama) {
		this.tampilFormTambahanDiHalamanUtama = tampilFormTambahanDiHalamanUtama;
	}

	/**
	 * Tampilkan tombol "Wawancara" di portal PPDB (default: false).
	 *
	 * <p><b>VERIFIKASI: kolom MATI di jalur PSB.</b> Penyisiran seluruh {@code src/} menemukan
	 * <b>nol</b> pemanggil {@code getTampilWawancara()}/{@code setTampilWawancara(...)} di luar
	 * berkas ini &mdash; layar master gelombang PSB pun tidak membangun komponen untuknya dan
	 * {@code onSave()} tidak menyimpannya. Properti ini adalah salinan struktural dari kembaran
	 * jenjang perguruan tinggi {@code ais.database.model.GelombangPendaftaran} (modul PMB), yang
	 * di sana memang punya kolom di layar dan pembaca sungguhan. Dokumentasi baris pertama di atas
	 * dipertahankan apa adanya sebagai niat asli penulis, tetapi pembaca harus tahu bahwa
	 * perilakunya belum terpasang pada jenjang sekolah.</p>
	 *
	 * @return {@code true} bila tombol wawancara diaktifkan; bawaan {@code false}. Nilai ini tidak
	 *         berpengaruh apa pun pada perilaku sistem saat ini.
	 */
	public Boolean getTampilWawancara() {
		return tampilWawancara == null ? false : tampilWawancara;
	}

	/** Menyetel saklar tombol wawancara. Tidak dipanggil kode mana pun. @param tampilWawancara nilai baru. */
	public void setTampilWawancara(Boolean tampilWawancara) {
		this.tampilWawancara = tampilWawancara;
	}

	/**
	 * Teks/HTML informasi untuk calon siswa yang ditampilkan di halaman wawancara portal.
	 *
	 * <p><b>VERIFIKASI: kolom MATI di jalur PSB</b>, sama seperti {@link #getTampilWawancara()}
	 * &mdash; nol pemanggil di luar berkas ini. Padanan PMB-nya
	 * ({@code GelombangPendaftaran.getInfoSaatInterview()}) dibaca
	 * {@code InterviewCalonMahasiswaAction} dan dirender sebagai {@code Html}, dan ditulis dari
	 * {@code GelombangPendaftaranAction}. Perhatikan bahwa pemanggil PMB itu merender isinya
	 * sebagai HTML mentah &mdash; bila kelak jalur serupa dipasang untuk PSB, kolom ini menjadi
	 * permukaan XML/HTML injection yang perlu ditapis.</p>
	 *
	 * @return teks/HTML informasi wawancara; bisa {@code null} (tidak dinormalkan).
	 */
	@Column(name = "info_saat_interview", columnDefinition = "text")
	public String getInfoSaatInterview() {
		return infoSaatInterview;
	}

	/** Menyetel teks informasi wawancara. Tidak dipanggil kode mana pun. @param infoSaatInterview teks/HTML. */
	public void setInfoSaatInterview(String infoSaatInterview) {
		this.infoSaatInterview = infoSaatInterview;
	}

	/**
	 * Kelompok payung yang menaungi beberapa gelombang sekaligus (kolom
	 * {@code kelompok_gelombang}).
	 *
	 * <p>Layar master menyediakan kombobox berisi {@link KelompokGelombang} aktif ditambah pilihan
	 * "Tanpa Kelompok" ({@code null}), dan menampilkan nama kelompok sebagai baris tambahan pada
	 * grid. Layar CRUD kelompoknya sendiri disisipkan sebagai tab lewat
	 * {@code onKelompok(Event)} &mdash; salah satu dari sembilan layar yang mewarisi hak dari
	 * menu gelombang ini.</p>
	 *
	 * @return kelompok gelombang; {@code null} bila gelombang berdiri sendiri.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_gelombang")
	public KelompokGelombang getKelompokGelombang() {
		kelompokGelombang = check(kelompokGelombang);
		return kelompokGelombang;
	}

	/** Menyetel kelompok gelombang. @param kelompokGelombang kelompok atau {@code null}. */
	public void setKelompokGelombang(KelompokGelombang kelompokGelombang) {
		this.kelompokGelombang = kelompokGelombang;
	}

	/**
	 * Syarat tambahan cetak kartu ujian: berkas calon siswa harus sudah terverifikasi.
	 *
	 * <p>Bekerja di atas {@link #getTampilCetakKartuUjian()} &mdash; tombolnya boleh tampil,
	 * tetapi pencetakan ditolak sampai verifikasi berkas selesai.</p>
	 *
	 * @return {@code true} bila verifikasi berkas disyaratkan; bawaan {@code false}.
	 */
	public Boolean getCetakKartuUjianHarusVerifikasiBerkas() {
		return cetakKartuUjianHarusVerifikasiBerkas == null ? false : cetakKartuUjianHarusVerifikasiBerkas;
	}

	/** Menyetel syarat verifikasi berkas untuk cetak kartu ujian. @param cetakKartuUjianHarusVerifikasiBerkas nilai baru. */
	public void setCetakKartuUjianHarusVerifikasiBerkas(Boolean cetakKartuUjianHarusVerifikasiBerkas) {
		this.cetakKartuUjianHarusVerifikasiBerkas = cetakKartuUjianHarusVerifikasiBerkas;
	}

	/**
	 * Paket tagihan yang wajib dibayar setelah berkas calon siswa TERVERIFIKASI (kolom
	 * {@code jenis_biaya_sekolah_terverifikasi_id}) &mdash; tahap kedua dari tiga tahap biaya.
	 *
	 * <p>Diperiksa {@link #chekSyaratBayar(CalonSiswa)} hanya bila
	 * {@code calonSiswa.getTerverifikasi()} bernilai {@code true}. Disembunyikan di layar master
	 * bila {@link #getSesuaiKelas()} dicentang.</p>
	 *
	 * @return paket biaya tahap terverifikasi; {@code null} berarti tidak ada kewajiban.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_sekolah_terverifikasi_id")
	public JenisBiayaSekolah getJenisBiayaSekolahTerverifikasi() {
		jenisBiayaSekolahTerverifikasi = check(jenisBiayaSekolahTerverifikasi);
		return jenisBiayaSekolahTerverifikasi;
	}

	/** Menyetel paket biaya tahap terverifikasi. @param jenisBiayaSekolahTerverifikasi paket biaya, boleh {@code null}. */
	public void setJenisBiayaSekolahTerverifikasi(JenisBiayaSekolah jenisBiayaSekolahTerverifikasi) {
		this.jenisBiayaSekolahTerverifikasi = jenisBiayaSekolahTerverifikasi;
	}

	/**
	 * Mengecek syarat pembayaran Calon Siswa. Dioptimasi dengan mengeleminasi N+1
	 * Query, menggunakan StringBuilder, dan Session Management yang aman dari
	 * Memory Leak.
	 *
	 * <p><b>Peran:</b> gerbang "sudah bayar belum?" bagi calon siswa sebelum ia boleh masuk
	 * portal atau melanjutkan tahap berikutnya. Memeriksa DUA tahap biaya milik gelombang calon
	 * siswa:</p>
	 * <ol>
	 *   <li><b>PENDAFTARAN AWAL</b> &mdash; bila {@link #getJenisBiayaSekolah()} terisi dan calon
	 *       siswa BELUM berstatus diterima;</li>
	 *   <li><b>TERVERIFIKASI</b> &mdash; bila {@link #getJenisBiayaSekolahTerverifikasi()} terisi
	 *       dan calon siswa sudah terverifikasi.</li>
	 * </ol>
	 * <p>Tahap DAFTAR ULANG ({@link #getJenisBiayaSekolahLulus()}) sengaja tidak diperiksa di
	 * sini.</p>
	 *
	 * <p><b>Cara kerja (menghindari N+1):</b> satu kueri {@link Criteria} atas
	 * {@code PembayaranSiswaDetail} mengambil <i>hanya id tagihan</i> yang sudah dibayar, lewat
	 * proyeksi {@code Projections.property("tag.id")}. Pembayaran dicari dengan identitas GANDA
	 * &mdash; sebagai {@code calonSiswa} maupun sebagai {@link Siswa} (untuk siswa lama yang
	 * mendaftar jenjang baru) &mdash; digabung dengan {@code Restrictions.or}. Bila kedua
	 * identitas kosong, dipasang {@code sqlRestriction("false")} sehingga daftar terbayar pasti
	 * kosong (fail-closed). Hasil mentahnya dikonversi ke {@link Long} lewat {@code toString()}
	 * untuk menghindari ketidakcocokan tipe antar-dialek JDBC. Daftar {@link PengaturanBiaya}
	 * yang relevan diambil sekali lewat {@code PengaturanBiaya.terapkanFilterPembayaran(...)},
	 * lalu dipakai ulang untuk kedua tahap.</p>
	 *
	 * <p><b>Efek samping:</b> (a) membuka {@link Session} Hibernate BARU sendiri
	 * ({@code openSession()}), bukan session request &mdash; ditutup bertahap
	 * ({@code clear}/{@code disconnect}/{@code close}) di blok {@code finally} dengan masing-masing
	 * dibungkus {@code try/catch}; (b) menulis banyak baris log {@code System.out} bertanda
	 * {@code [DB]}/{@code [BLOKIR]} pada setiap pemanggilan; (c) <b>memunculkan dialog
	 * {@code MyMessageboxConfig}</b> lewat {@link #tampilkanPeringatan(String)} bila ada tagihan
	 * belum lunas.</p>
	 *
	 * <p><b>Catatan penting soal jalur REST:</b> method ini juga dipanggil dari
	 * {@code ais.action.servlet.api.PsbCalonApi} (login calon siswa via API, digerbangi
	 * konfigurasi {@code calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login_baru}) yang
	 * TIDAK punya ZK Desktop. Di jalur itu pemanggilan Messagebox gagal, tetapi kegagalannya
	 * ditelan {@code try/catch} di dalam {@link #tampilkanPeringatan(String)} sehingga nilai
	 * balik boolean tetap benar &mdash; hanya pesan rinci tagihannya yang hilang bagi pengguna
	 * API.</p>
	 *
	 * <p><b>Fail-closed:</b> exception apa pun (termasuk {@code NullPointerException} bila
	 * {@code calonSiswa} atau gelombangnya {@code null}) menghasilkan {@code false} &mdash; calon
	 * siswa DITAHAN, bukan diloloskan. Ini kebalikan dari
	 * {@link #chekUmur(GelombangPendaftaranPsb, MyDatebox)} yang fail-open.</p>
	 *
	 * <p><b>Pemanggil terverifikasi:</b> {@code PsbCalonApi}, {@code LoginCalonSiswaAction},
	 * {@code PSBAction} (dua titik), {@code CommonReportPsb}, dan
	 * {@code TampilanPengumumanAkademisAction}.</p>
	 *
	 * @param calonSiswa calon siswa yang diperiksa; jalur pemanggil selalu mengirim objek
	 *        non-{@code null} dengan gelombang terisi.
	 * @return {@code true} bila seluruh tagihan tahap yang berlaku sudah lunas (atau tidak ada
	 *         kewajiban); {@code false} bila ada tagihan tertunggak atau terjadi exception.
	 */
	@SuppressWarnings("unchecked")
	public static boolean chekSyaratBayar(CalonSiswa calonSiswa) {
		Session session = null;

		try {
			// LOG 1: Memulai Proses
			System.out.println("=================================================");
			System.out.println("START CEK SYARAT BAYAR (Calon Siswa ID: "
					+ (calonSiswa != null ? calonSiswa.getId() : "NULL") + ")");

			session = HibernateUtil.getSessionFactory().openSession();

			// Ambil identitas Siswa (jika calon siswa ini adalah siswa lama yang mendaftar
			// jenjang baru)
			Siswa siswa = calonSiswa != null ? calonSiswa.getSiswa() : null;

			// 1. AMBIL SEMUA ID TAGIHAN YANG SUDAH DIBAYAR (Sangat Irit Memori)
			Criteria dibayarCriteria = session.createCriteria(PembayaranSiswaDetail.class).createAlias("tagihan", "tag")
					.createAlias("pembayaranSiswa", "ps");

			// Pastikan mencakup pembayaran menggunakan ID Calon Siswa ATAU ID Siswa
			if (siswa != null && calonSiswa != null) {
				dibayarCriteria.add(Restrictions.or(Restrictions.eq("ps.siswa", siswa),
						Restrictions.eq("ps.calonSiswa", calonSiswa)));
			} else if (calonSiswa != null) {
				dibayarCriteria.add(Restrictions.eq("ps.calonSiswa", calonSiswa));
			} else if (siswa != null) {
				dibayarCriteria.add(Restrictions.eq("ps.siswa", siswa));
			} else {
				dibayarCriteria.add(Restrictions.sqlRestriction("false"));
			}

			List<Object> dibayarsRaw = dibayarCriteria.setProjection(Projections.property("tag.id")).list();

			// Konversi semua tipe data menjadi Long untuk menghindari Type Mismatch Bug
			List<Long> dibayars = new ArrayList<Long>();
			for (Object obj : dibayarsRaw) {
				if (obj != null) {
					dibayars.add(Long.valueOf(obj.toString()));
				}
			}

			// LOG 2: Cetak Array ID Tagihan yang sudah dibayar
			System.out.println("[DB] Array ID Tagihan Terbayar 'dibayars': " + dibayars.toString());

			List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(PengaturanBiaya
					.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), siswa, calonSiswa)
					.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
					.addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);

			// 2. CEK SYARAT BIAYA PENDAFTARAN AWAL
			if (calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah() != null
					&& !calonSiswa.getTelahDiterima()) {
				System.out.println("\n-> Masuk ke tahap: PENDAFTARAN AWAL");
				List<Tagihan> tagihans = TagihanUtilCalonSiswa.getTagihan(
						calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah(), pengaturanBiayas, calonSiswa,
						null, null, false, true);

				String tag = buildPesanTagihanBelumLunas(tagihans, dibayars, "PENDAFTARAN_AWAL");

				if (!tag.isEmpty()) {
					System.out.println("[BLOKIR] Ditemukan Tagihan Belum Lunas!");
					tampilkanPeringatan(tag);
					return false;
				}
			}

			// 3. CEK SYARAT BIAYA TERVERIFIKASI
			if (calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi() != null
					&& calonSiswa.getTerverifikasi()) {
				System.out.println("\n-> Masuk ke tahap: TERVERIFIKASI");
				List<Tagihan> tagihans = TagihanUtilCalonSiswa.getTagihan(
						calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi(), pengaturanBiayas,
						calonSiswa, null, null, false, true);

				String tag = buildPesanTagihanBelumLunas(tagihans, dibayars, "TERVERIFIKASI");

				if (!tag.isEmpty()) {
					System.out.println("[BLOKIR] Ditemukan Tagihan Belum Lunas!");
					tampilkanPeringatan(tag);
					return false;
				}
			}

			System.out.println("-> END SYARAT BAYAR: TRUE (LULUS)");
			System.out.println("=================================================");
			return true;

		} catch (Exception e) {
			System.out.println("ERROR chekSyaratBayar: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/GelombangPendaftaranPsb.java:878");
			return false;
		} finally {
			// 4. TUTUP SESSION DENGAN AMAN
			if (session != null && session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/GelombangPendaftaranPsb.java:885");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/GelombangPendaftaranPsb.java:889");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/GelombangPendaftaranPsb.java:893");
				}
			}
		}
	}

	/**
	 * Helper Method: Menyusun teks tagihan yang belum lunas. Telah disinkronisasi
	 * dengan logika nominal &gt; 0.1 dari referensi sistem.
	 *
	 * <p>Sebuah tagihan dianggap BELUM LUNAS hanya bila SEMUA syarat berikut terpenuhi:
	 * tagihan aktif; bukan tagihan-data ({@code ambilBukanTagihanData()} {@code false}); punya
	 * {@code NominalBiaya} yang bukan penanda "bukan tagihan"; id-nya tidak ada di daftar
	 * {@code dibayars}; belum punya {@code PembayaranSiswaDetail}; dan nominalnya di atas
	 * {@code 0.1}. Ambang {@code 0.1} itu penting: tagihan bernilai nol (gratis/beasiswa penuh)
	 * sengaja TIDAK memblokir calon siswa &mdash; ini perbaikan logika yang diberi tanda di
	 * kode.</p>
	 *
	 * <p><b>Efek samping:</b> mencetak jejak keputusan per tagihan ke {@code System.out}
	 * (satu blok per tagihan, dengan alasan skip yang eksplisit).</p>
	 *
	 * @param tagihans daftar tagihan tahap terkait; boleh {@code null} (dianggap kosong).
	 * @param dibayars daftar id tagihan yang sudah terbayar, hasil kueri proyeksi di
	 *        {@link #chekSyaratBayar(CalonSiswa)}.
	 * @param tahap label tahap ({@code "PENDAFTARAN_AWAL"} / {@code "TERVERIFIKASI"}) &mdash;
	 *        hanya dipakai untuk penandaan log.
	 * @return rangkaian "nama item + nominal terformat" dipisah {@code "; "}; string KOSONG bila
	 *         tidak ada tunggakan (dipakai pemanggil sebagai penanda lulus).
	 */
	private static String buildPesanTagihanBelumLunas(List<Tagihan> tagihans, List<Long> dibayars, String tahap) {
		StringBuilder tagBuilder = new StringBuilder();

		System.out.println("   [LOG " + tahap + "] Menemukan " + (tagihans != null ? tagihans.size() : 0)
				+ " data tagihan untuk dicek.");

		if (tagihans != null) {
			for (Tagihan tagihan : tagihans) {
				Long idTagihan = tagihan.getId();
				String namaItem = tagihan.getItemBiayaSekolah() != null ? tagihan.getItemBiayaSekolah().getNama()
						: "Item_Null";
				Double nominalTagihan = tagihan.getNominal() != null ? tagihan.getNominal() : 0.0;

				System.out.print("   => Cek Tagihan ID: " + idTagihan + " [" + namaItem + "] -> ");

				if (tagihan.getAktif() != null && tagihan.getAktif() && !tagihan.ambilBukanTagihanData()
						&& tagihan.getNominalBiaya() != null && !tagihan.getNominalBiaya().getBukanTagihan()) {

					boolean sudahDibayar = (idTagihan != null && dibayars.contains(idTagihan));
					boolean belumAdaDetail = (tagihan.getPembayaranSiswaDetail() == null);
					boolean nominalHarusDibayar = (nominalTagihan > 0.1); // PERBAIKAN LOGIKA: Jika nominal 0, jangan
																			// diblokir

					// Jika ID Tagihan tidak ada di daftar yang sudah dibayar
					if (!sudahDibayar) {
						// Jika belum ada detail pembayaran DAN Nominalnya > 0 (bukan gratis)
						if (belumAdaDetail && nominalHarusDibayar) {
							System.out.println(
									"      [!] DITANDAI SEBAGAI BELUM LUNAS (Nominal: " + nominalTagihan + ")");
							if (tagBuilder.length() > 0) {
								tagBuilder.append("; ");
							}
							tagBuilder.append(namaItem).append(" ").append(Common.numberFormat.get().format(nominalTagihan));
						} else {
							System.out.println("Di-SKIP! (Alasan - Detail Null: " + belumAdaDetail + ", Nominal > 0.1: "
									+ nominalHarusDibayar + ")");
						}
					} else {
						System.out.println("Kondisi Valid. Tagihan SUDAH ADA di list 'dibayars'.");
					}
				} else {
					System.out.println("Di-SKIP! (Status Aktif / BukanTagihan tidak valid).");
				}
			}
		}
		return tagBuilder.toString();
	}

	/**
	 * Helper Method: Menampilkan Messagebox peringatan UI secara aman.
	 *
	 * <p>Menampilkan dialog {@code MyMessageboxConfig} berisi rincian tagihan tertunggak plus
	 * tiga langkah perbaikan. Seluruh badan dibungkus {@code try/catch} yang MENELAN exception
	 * &mdash; inilah yang membuat {@link #chekSyaratBayar(CalonSiswa)} tetap aman dipanggil dari
	 * jalur REST ({@code PsbCalonApi}) yang tidak punya ZK Desktop: dialognya gagal diam-diam,
	 * keputusan boolean-nya tidak terpengaruh.</p>
	 *
	 * @param pesanTagihan rincian tagihan hasil {@link #buildPesanTagihanBelumLunas(List, List,
	 *        String)}; disisipkan ke placeholder {@code {V1}}.
	 */
	private static void tampilkanPeringatan(String pesanTagihan) {
		try {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, Bapak/Ibu. Terdapat tagihan yang harus dibayarkan, yaitu sebagai berikut:\n\n{V1}\n\nLangkah yang dapat dilakukan: (1) periksa kembali rincian tagihan tersebut; (2) lakukan pembayaran sesuai ketentuan yang berlaku; (3) ulangi proses setelah seluruh tagihan diselesaikan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, pesanTagihan);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/GelombangPendaftaranPsb.java:960");
		}
	}

	/**
	 * Daftar tingkat asal alumni yang diakui, beberapa nilai dipisah koma (mis. {@code "A,B,C"}).
	 * Kosong berarti "semua tingkat". Hanya relevan bila {@link #getHarusSebagaiAlumni()} aktif.
	 *
	 * @return daftar tingkat sudah di-{@code trim}; string kosong (bukan {@code null}) bila belum
	 *         diisi.
	 */
	public String getTingkatDariAlumni() {
		return tingkatDariAlumni == null ? "" : tingkatDariAlumni.trim();
	}

	/** Menyetel daftar tingkat asal alumni. @param tingkatDariAlumni daftar dipisah koma. */
	public void setTingkatDariAlumni(String tingkatDariAlumni) {
		this.tingkatDariAlumni = tingkatDariAlumni;
	}

	/**
	 * Aktifkan verifikasi silang NIK terhadap data ALUMNI. Barisnya di layar master hanya tampil
	 * bila {@link #getHarusSebagaiAlumni()} dicentang.
	 *
	 * @return {@code true} bila verifikasi NIK alumni aktif; bawaan {@code false}.
	 */
	public Boolean getTerdapatVerifikasiDenganNikAlumni() {
		return terdapatVerifikasiDenganNikAlumni == null ? false : terdapatVerifikasiDenganNikAlumni;
	}

	/** Menyetel verifikasi NIK alumni. @param terdapatVerifikasiDenganNikAlumni nilai baru. */
	public void setTerdapatVerifikasiDenganNikAlumni(Boolean terdapatVerifikasiDenganNikAlumni) {
		this.terdapatVerifikasiDenganNikAlumni = terdapatVerifikasiDenganNikAlumni;
	}

	/**
	 * Aktifkan verifikasi silang NIK terhadap data SAUDARA (sibling). Barisnya di layar master
	 * hanya tampil bila {@link #getHarusSebagaiSaudara()} dicentang.
	 *
	 * <p><b>Bug salin-tempel di layar master (bukan di entity ini):</b>
	 * {@code GelombangPendaftaranPsbAction.init()} mengisi centang untuk properti ini dengan
	 * {@code gelombangPendaftaranPsb.getTerdapatVerifikasiDenganNikAlumni()} &mdash; properti
	 * SAUDARA diisi dari nilai ALUMNI &mdash; dan memberinya label "Terdapat verifikasi dengan
	 * NIK Alumni" yang sama persis dengan centang di atasnya. {@code onSave()} sendiri sudah
	 * benar (memanggil {@link #setTerdapatVerifikasiDenganNikSibling(Boolean)} dari centang yang
	 * tepat). Akibatnya: nilai sibling yang tersimpan tidak pernah terlihat di layar, dan sekali
	 * dialog dibuka lalu disimpan ulang, nilai sibling diam-diam tertimpa nilai alumni.</p>
	 *
	 * @return {@code true} bila verifikasi NIK saudara aktif; bawaan {@code false}.
	 */
	public Boolean getTerdapatVerifikasiDenganNikSibling() {
		return terdapatVerifikasiDenganNikSibling == null ? false : terdapatVerifikasiDenganNikSibling;
	}

	/** Menyetel verifikasi NIK saudara. @param terdapatVerifikasiDenganNikSibling nilai baru. */
	public void setTerdapatVerifikasiDenganNikSibling(Boolean terdapatVerifikasiDenganNikSibling) {
		this.terdapatVerifikasiDenganNikSibling = terdapatVerifikasiDenganNikSibling;
	}

	/**
	 * Daftar tahun pelajaran kelulusan alumni yang diakui, dipisah koma (mis.
	 * {@code "2024/2025,2025/2026"}). Kosong berarti "semua tahun pelajaran". Wajib diisi menurut
	 * label layar bila {@link #getHarusSebagaiAlumni()} aktif, meski {@code onSave()} tidak
	 * memaksakannya.
	 *
	 * @return daftar tahun pelajaran sudah di-{@code trim}; string kosong bila belum diisi.
	 */
	public String getTahunAkademikAlumni() {
		return tahunAkademikAlumni == null ? "" : tahunAkademikAlumni.trim();
	}

	/** Menyetel daftar tahun pelajaran alumni. @param tahunAkademikAlumni daftar dipisah koma. */
	public void setTahunAkademikAlumni(String tahunAkademikAlumni) {
		this.tahunAkademikAlumni = tahunAkademikAlumni;
	}

	/**
	 * Batas jumlah calon siswa yang boleh berstatus DITERIMA pada gelombang ini; bawaan
	 * {@code 5000} bila kolom kosong.
	 *
	 * <p><b>Penegakan tidak merata &mdash; penting.</b> Kuota ini hanya diperiksa di DUA tempat:
	 * jalur unggah massal kelulusan ({@code CommonPSB}, yang menghitung
	 * {@code CalonSiswa} ber-{@code telahDiterima = true} pada gelombang yang sama lalu berhenti
	 * dengan label "Penuh") dan layar admin calon siswa ({@code CalonSiswaAction}). Jalur
	 * pendaftaran mandiri PPDB publik dengan {@link #getLangsungDapatNisSaatDaftar()} aktif
	 * <b>tidak memeriksanya sama sekali</b> &mdash; ia langsung menyetel
	 * {@code setTelahDiterima(true)} dan menerbitkan NIS. Nama method
	 * {@link #chekKuotaPendaftar()} yang terdengar seperti penegak kuota juga TIDAK memeriksa
	 * apa pun (lihat dokumentasinya).</p>
	 *
	 * @return kuota penerimaan; bawaan 5000.
	 */
	public Integer getKuotaDiterima() {
		return kuotaDiterima == null ? 5000 : kuotaDiterima;
	}

	/** Menyetel kuota penerimaan. @param kuotaDiterima jumlah maksimal calon siswa yang diterima. */
	public void setKuotaDiterima(Integer kuotaDiterima) {
		this.kuotaDiterima = kuotaDiterima;
	}

	/**
	 * Menyemai prasarana ujian bawaan untuk gelombang ini bila belum ada &mdash; <b>bukan</b>
	 * memeriksa kuota, meski namanya menyiratkan demikian.
	 *
	 * <p><b>Yang sebenarnya dilakukan:</b></p>
	 * <ol>
	 *   <li>mencari {@link UjianPSB} pertama (urut {@code id} menaik) milik gelombang ini; bila
	 *       tidak ada, MEMBUAT satu dengan {@code nama = "Online"},
	 *       {@code lokasi = "Sekolah"}, {@code tampilkanJadwalUjianDiKartuUjian = true}, lalu
	 *       {@code session.save()} + {@code session.flush()};</li>
	 *   <li>mencari {@link RuangPSB} pertama milik gelombang ini; bila tidak ada, MEMBUAT satu
	 *       dengan {@code kodeRuangan}/{@code nama} = {@code "Online"},
	 *       {@code kapasitasRuangan = 10000}, ditautkan ke ujian di atas, lalu disimpan dan
	 *       di-flush.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang mengejutkan:</b> pemanggilnya adalah
	 * {@code GelombangPendaftaranPsbRenderer.render()} di layar master &mdash; baris PERTAMA
	 * method {@code render}. Artinya sekadar MEMBUKA (atau memuat ulang, atau berpindah halaman
	 * pada) daftar gelombang pendaftaran akan menulis baris baru ke tabel {@code ujian_psb} dan
	 * {@code ruang_psb}, satu kali per gelombang yang belum punya keduanya, per baris yang
	 * tampil. Operasi tulis ini memakai {@code HibernateUtil.currentSession()} tanpa membuka
	 * transaksi eksplisit dan tanpa penanganan exception, sehingga kegagalannya merambat ke
	 * jalur render layar.</p>
	 *
	 * <p><b>Konsekuensi bagi laporan:</b> baris "Online" hasil semai ini tetap muncul di daftar
	 * ujian/ruang sekalipun sekolah tidak pernah menyelenggarakan ujian daring &mdash; hal yang
	 * sudah dicatat pada Javadoc {@link UjianPSB} dan {@link RuangPSB}. Kembaran jenjang
	 * perguruan tinggi ({@code GelombangPendaftaran.chekKuotaPendaftar()}) berperilaku sama dan
	 * dipanggil dari renderer yang sama bentuknya.</p>
	 */
	public void chekKuotaPendaftar() {
		Session session = HibernateUtil.currentSession();

		UjianPSB ujianPSB = (UjianPSB) ConstantValues
				.simpleObject(
						session.createCriteria(UjianPSB.class).addOrder(Order.asc("id"))
								.add(Restrictions.eq("gelombangPendaftaranPsb", this)).setMaxResults(1),
						UjianPSB.class);
		if (ujianPSB == null) {
			ujianPSB = new UjianPSB();
			ujianPSB.setLokasi("Sekolah");
			ujianPSB.setNama("Online");
			ujianPSB.setTampilkanJadwalUjianDiKartuUjian(true);
			ujianPSB.setGelombangPendaftaranPsb(this);
			session.save(ujianPSB);
			session.flush();

		}
		RuangPSB ruang = (RuangPSB) ConstantValues
				.simpleObject(
						session.createCriteria(RuangPSB.class).addOrder(Order.asc("id"))
								.add(Restrictions.eq("gelombangPendaftaranPsb", this)).setMaxResults(1),
						RuangPSB.class);

		if (ruang == null) {
			ruang = new RuangPSB();
			ruang.setKodeRuangan("Online");
			ruang.setNama("Online");
			ruang.setKapasitasRuangan(10000);
			ruang.setUjianPSB(ujianPSB);
			ruang.setGelombangPendaftaranPsb(this);
			session.save(ruang);
			session.flush();
		}

	}

	/**
	 * Terbitkan NIS dan tandai calon siswa DITERIMA seketika pada saat pendaftaran &mdash; saklar
	 * paling agresif dari empat mekanisme otomatisasi NIS milik entity ini.
	 *
	 * <p><b>Label di layar master:</b> "Saat pertama kali daftar, calon siswa wajib otomatis
	 * diteima dan mendapatkan NIS" (salah ketik "diteima" ada di kode asli).</p>
	 *
	 * <p><b>Rantai eksekusi TERVERIFIKASI.</b> Kesembilan varian formulir PPDB publik
	 * ({@code PPDB1}, {@code PPDB2}, {@code PPDB_Alumni}, {@code PPDB_Simple} s.d.
	 * {@code PPDB_Simple6}) beserta {@code CalonSiswaAction} menjalankan blok yang sama di akhir
	 * penyimpanan pendaftaran: bila flag ini {@code true} <b>dan</b> calon siswa belum tertaut
	 * {@link Siswa}, sistem (1) memuat kelas generator dari konfigurasi
	 * {@code class_untuk_generate_nis} lewat {@code Class.forName(...).newInstance()} &mdash;
	 * bawaannya {@code DefaultNisGenerator}; (2) menyetel
	 * {@code calonSiswa.setTelahDiterima(true)}; (3) memanggil
	 * {@code CommonPSB.onGenerateNis(calonSiswa, nisGenerator, false)} ({@code cetak = false},
	 * jadi tanpa cetak bukti PDF). {@code onGenerateNis} lalu mencari {@link FormatNis} AKTIF
	 * milik sekolah calon siswa dan merakit NIS lewat
	 * {@code CommonPSB.generateCode(FormatNis, CalonSiswa)}; bila tidak ada {@link FormatNis}
	 * aktif, generator cadangan yang dipakai. Seluruh blok itu dibungkus {@code try/catch} yang
	 * hanya mencatat error &mdash; pendaftaran tetap dianggap sukses meski NIS gagal terbit.</p>
	 *
	 * <p><b>Konsekuensi keamanan/integritas data:</b></p>
	 * <ul>
	 *   <li>{@link #getKuotaDiterima()} <b>tidak diperiksa</b> di jalur ini &mdash; pendaftar
	 *       publik ke-{@code n} tetap diterima dan ber-NIS meski kuota sudah lewat;</li>
	 *   <li>status "diterima" diberikan tanpa verifikasi berkas, tanpa pembayaran, dan tanpa
	 *       campur tangan panitia, langsung dari formulir publik anonim;</li>
	 *   <li>ia mengalirkan volume tinggi ke {@link FormatNis}, yang jaminan keunikan NIS-nya
	 *       sudah tercatat lemah &mdash; sehingga mengaktifkan flag ini memperbesar peluang NIS
	 *       kembar.</li>
	 * </ul>
	 *
	 * @return {@code true} bila NIS diterbitkan seketika saat mendaftar; bawaan {@code false}.
	 */
	public Boolean getLangsungDapatNisSaatDaftar() {
		return langsungDapatNisSaatDaftar == null ? false : langsungDapatNisSaatDaftar;
	}

	/**
	 * Menyetel saklar "langsung dapat NIS saat daftar". Disimpan dari centang di layar master
	 * ({@code onSave()}).
	 *
	 * @param langsungDapatNisSaatDaftar nilai baru.
	 */
	public void setLangsungDapatNisSaatDaftar(Boolean langsungDapatNisSaatDaftar) {
		this.langsungDapatNisSaatDaftar = langsungDapatNisSaatDaftar;
	}

	/**
	 * Tampilkan QR-Code identitas siswa di portal setelah NIS terbit; bawaan {@code true}.
	 *
	 * <p>Nama properti masih memakai istilah "Mahasiswa"/"Nim" warisan modul PMB, tetapi label
	 * layar master sudah benar: "Tampilkan QR-Code Siswa Setelah mendapat NIS".</p>
	 *
	 * @return {@code true} bila QR-Code ditampilkan; bawaan {@code true}.
	 */
	public Boolean getTampilkanQrCodeMahasiswaSetelahDapatNim() {
		return tampilkanQrCodeMahasiswaSetelahDapatNim == null ? true : tampilkanQrCodeMahasiswaSetelahDapatNim;
	}

	/** Menyetel penampilan QR-Code setelah NIS terbit. @param tampilkanQrCodeMahasiswaSetelahDapatNim nilai baru. */
	public void setTampilkanQrCodeMahasiswaSetelahDapatNim(Boolean tampilkanQrCodeMahasiswaSetelahDapatNim) {
		this.tampilkanQrCodeMahasiswaSetelahDapatNim = tampilkanQrCodeMahasiswaSetelahDapatNim;
	}

	/**
	 * Daftar kelas asal alumni yang diakui (kolom TEXT {@code kelas_dari_alumni}), beberapa nilai
	 * dipisah koma (mis. {@code "5,6"}). Kosong berarti "semua kelas". Hanya tampil di layar
	 * master bila {@link #getHarusSebagaiAlumni()} dicentang.
	 *
	 * <p>Berbeda dari {@link #getTingkatDariAlumni()}/{@link #getTahunAkademikAlumni()}, getter
	 * ini <b>tidak</b> mem-{@code trim} isinya &mdash; hanya menormalkan {@code null} menjadi
	 * string kosong.</p>
	 *
	 * @return daftar kelas asal alumni; string kosong bila belum diisi.
	 */
	@Column(name = "kelas_dari_alumni", columnDefinition = "text")
	public String getKelasDariAlumni() {
		return kelasDariAlumni == null ? "" : kelasDariAlumni;
	}

	/** Menyetel daftar kelas asal alumni. @param kelasDariAlumni daftar dipisah koma. */
	public void setKelasDariAlumni(String kelasDariAlumni) {
		this.kelasDariAlumni = kelasDariAlumni;
	}
}
