package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.KomponenPenilaianPkl;
import ais.database.model.pkl.PklPunyaKomponenPenilaianPkl;

/**
 * Entity <b>keanggotaan seorang mahasiswa di dalam satu kelompok PKL</b> &mdash; baris penghubung
 * (tabel asosiasi yang punya identitas sendiri) antara {@link Mahasiswa} dan
 * {@link ais.database.model.pkl.KelompokPkl}, sekaligus <b>tempat menyimpan nilai PKL</b> mahasiswa
 * tersebut.
 *
 * <p>Satu baris kelas ini menjawab pertanyaan "mahasiswa X ikut kelompok PKL Y, sudah disetujui
 * atau belum, apa hasil/laporan lapangannya, dan berapa nilainya". Ia adalah <b>hilir</b> dari
 * rantai modul PKL: {@link Pkl} (program/gelaran PKL) &rarr;
 * {@link ais.database.model.pkl.KelompokPkl} (satu tempat magang + pembimbing) &rarr;
 * <b>kelas ini</b> (anggota kelompok). Perhatikan bahwa <b>tidak ada FK langsung ke {@code pkl}</b>
 * &mdash; program PKL selalu dicapai lewat dua hop {@code kelompokPkl.pkl}, dan seluruh kode di
 * repo ini menyaringnya dengan {@code createAlias("kelompokPkl", "kelompokPkl")} +
 * {@code Restrictions.eq("kelompokPkl.pkl", pkl)}.</p>
 *
 * <h2>Posisi dalam alur PKL</h2>
 * <ol>
 * <li><b>Pendaftaran</b> &mdash; mahasiswa mendaftar program lewat
 * {@code ais.action.master.pkl.PklUntukMahasiswaAction}, menghasilkan
 * {@link ais.database.model.pkl.MahasiswaDaftarPkl} dengan kolom {@code terima}.</li>
 * <li><b>Pemilihan kelompok</b> &mdash; hanya setelah {@code terima = DITERIMA}, layar yang sama
 * menampilkan pilihan kelompok. Saat mahasiswa memilih, <b>baris kelas ini dibuat</b> dengan
 * {@code mahasiswa} + {@code kelompokPkl} terisi, tetapi {@link #getDiterima() diterima} masih
 * belum {@code true} &mdash; artinya <i>usulan</i>, bukan penempatan sah.</li>
 * <li><b>Persetujuan panitia</b> &mdash; {@code ais.action.master.helper.KelompokPklHelper}
 * menampilkan daftar anggota kelompok dengan checkbox "Diterima" yang memanggil
 * {@link #setDiterima(Boolean)}. Jalur admin langsung
 * ({@code ais.action.master.helper.AmbilDataMahasiswaKelompokPklHelper}, "tambah banyak
 * mahasiswa") membuat baris yang <b>langsung</b> {@code diterima = true} setelah memeriksa
 * {@link ais.database.model.pkl.KelompokPkl#getKuota() kuota} kelompok.</li>
 * <li><b>Pelaksanaan</b> &mdash; agenda/pertemuan lapangan dibangun
 * {@code ais.action.master.helper.AktifitasPklHelper} pada level kelompok, bukan per anggota;
 * absensi per anggota lewat {@code AbsensiHelper}/{@link Pertemuan}. Ringkasan hasil kegiatan
 * ditulis panitia ke {@link #getHasil()}.</li>
 * <li><b>Penilaian</b> &mdash; {@code ais.action.master.helper.PenilaianPklHelper} membuka form
 * per anggota, mengisi nilai tiap komponen lewat
 * {@link #populateDetailNilai(KomponenPenilaianPkl, Double, Boolean)}, menghitung rata-rata
 * berbobot lewat {@link #hitungTotalNilai(Boolean)}, mengonversinya ke huruf, lalu
 * <b>menyalinnya</b> ke {@link Detailperkuliahan} matakuliah PKL agar masuk KHS/IPK.</li>
 * <li><b>Sertifikat &amp; ekspor</b> &mdash; {@code ais.action.master.SertifikatAction} mencetak
 * sertifikat bagi anggota yang {@code diterima}; {@code EksporPesertaMahasiswaPklFeeder} dan
 * {@code DownloadAktifitasMahasiwaPklPesertaMahasiswa} mengirim keanggotaan ini ke
 * Feeder/PDDikti.</li>
 * </ol>
 *
 * <h2>Kembaran kelas ini</h2>
 * <p>Tiga kelas lain di repo ini adalah salinan struktural yang nyaris identik &mdash; perubahan
 * pada salah satunya hampir selalu perlu disalin ke yang lain:</p>
 * <ul>
 * <li>{@link MahasiswaDapatKelompokKkn} &mdash; padanan KKN. Struktur <b>sama persis</b>: field,
 * urutan method, format {@code detailNilai}, bahkan cacat yang sama (lihat "Cacat yang diketahui").
 * Perbedaannya hanya penamaan ({@code kelompokKkn} bukan {@code kelompokPkl},
 * {@code KknPunyaKomponenPenilaianKkn} bukan {@link PklPunyaKomponenPenilaianPkl}) dan tabelnya
 * ({@code mahasiswa_dapat_kelompok_kelompok_kkn}).</li>
 * <li>{@link SiswaDapatKelompokPkl} &mdash; padanan untuk peserta jalur sekolah (modul
 * {@code ais.database.model.sekolah}).</li>
 * <li>{@link Detailperkuliahan} dan {@link Skripsi} &mdash; bukan kembaran, tetapi memakai
 * <b>skema {@code detailNilai} yang sama persis</b> beserta method
 * {@code populateDetailNilai}/{@code retreiveDetailNilai}/{@code hitungTotalNilai}. Itulah sebabnya
 * helper penilaian seperti {@code DetailperkuliahanForPenilaianHelper} bisa memakai pola kode yang
 * sama untuk keempatnya.</li>
 * </ul>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Yang benar-benar diwarisi dari induk adalah
 * kumpulan utilitas statis, terutama {@link GeneralValueObject#check(Object)} untuk resolusi proxy
 * lazy (dipakai {@link #getKelompokPkl()} dan {@link #getMahasiswa()}) serta
 * {@link GeneralValueObject#write(String...)} untuk cache JSON.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name =
 * "mahasiswa_dapat_kelompok_kelompok_pkl")} &mdash; perhatikan kata <b>{@code kelompok} yang
 * tertulis dua kali</b> pada nama tabel; itu memang nama tabel yang sebenarnya di basis data
 * (kekhilafan penamaan lama yang sudah terlanjur dipakai, kembarannya di KKN juga demikian), bukan
 * salah ketik yang boleh "dirapikan".</p>
 * <p>{@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah ikut
 * dalam {@code INSERT}/{@code UPDATE}) dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code mahasiswa_dapat_kelompok_kelompok_pkl_AUD}.</p>
 * <p>Pemetaan memakai <b>property access</b> (anotasi {@code @Id} menempel pada {@link #getId()}),
 * sehingga <b>setiap pasangan getter/setter yang tidak dianotasi {@code @Transient} tetap
 * dipetakan</b> &mdash; dan di kelas ini <b>tidak ada satu pun {@code @Transient}</b>. Karena
 * {@code ais.database.hibernate.MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}
 * (nama kolom = nama properti apa adanya), properti tanpa {@code @Column} jatuh ke kolom bernama
 * persis seperti propertinya: {@code namaDosen}, {@code totalNilai}, {@code nilaiHuruf},
 * {@code totalIP}, {@code lulus}, {@code diterima}, {@code tanggal_dirubah}, {@code oleh},
 * {@code olehId}. Konfigurasi {@code hbm2ddl.auto=update} membuat kolom-kolom itu benar-benar ada
 * di tabel. Konsekuensi penting: {@link #getNamaDosen()} yang tampak seperti nilai turunan
 * sesungguhnya <b>disimpan</b> di kolom dan ikut ditulis ulang setiap kali dibaca (lihat "Pola
 * getter yang menulis balik").</p>
 * <p><b>Tidak ada unique constraint</b> pada pasangan ({@code mahasiswa}, {@code kelompok_pkl})
 * maupun ({@code mahasiswa}, program PKL). Seluruh pemanggil melindungi diri secara manual dengan
 * {@code setMaxResults(1)} + {@code uniqueResult()} atau dengan menghitung dulu apakah barisnya
 * sudah ada; artinya baris ganda <b>mungkin terbentuk</b> bila dua layar menyimpan bersamaan, dan
 * yang terbaca kemudian hanya salah satunya.</p>
 *
 * <h2>Format {@code detailNilai}</h2>
 * <p>Rincian nilai per komponen tidak disimpan di tabel anak, melainkan diserialkan menjadi satu
 * kolom {@code text}. Bentuknya: beberapa <b>ruas</b> dipisah titik koma {@code ;}, masing-masing
 * berisi lima <b>medan</b> dipisah koma:</p>
 * <pre>
 *   &lt;idKomponenPenilaianPkl&gt;,&lt;nilaiAngka&gt;,0,&lt;bobot&gt;,&lt;sudahDiverifikasi&gt;
 *   contoh: 12,80.0,0,40.0,true;13,75.0,0,60.0,false
 * </pre>
 * <ul>
 * <li><b>Medan 1</b> &mdash; id {@link KomponenPenilaianPkl} (bukan id
 * {@link PklPunyaKomponenPenilaianPkl}). Ini sumber kebingungan: beberapa variabel lokal di kelas
 * ini bernama {@code idPklPunyaKomponenPenilaianPkl} padahal isinya id komponen. Yang penting,
 * <b>seluruh method di kelas ini konsisten memakai id komponen</b>, jadi tidak ada cacat
 * fungsional di situ.</li>
 * <li><b>Medan 2</b> &mdash; nilai angka yang diberikan penilai.</li>
 * <li><b>Medan 3</b> &mdash; selalu ditulis literal {@code 0} dan <b>tidak pernah dibaca</b> oleh
 * kode mana pun; sisa skema lama.</li>
 * <li><b>Medan 4</b> &mdash; {@link KomponenPenilaianPkl#getBobot() bobot} komponen, disalin saat
 * penulisan. Di {@link #hitungTotalNilai(Boolean, List)} variabel yang menampungnya bernama
 * {@code persen}; namanya menyesatkan, isinya bobot. Karena bobot <b>disalin ke dalam string</b>,
 * mengubah bobot komponen di master <b>tidak</b> mengubah nilai yang sudah tersimpan sampai ruas
 * itu ditulis ulang lewat {@link #populateDetailNilai(KomponenPenilaianPkl, Double, Boolean)}.</li>
 * <li><b>Medan 5</b> &mdash; penanda verifikasi ({@code true}/{@code false}), dibaca
 * {@link #retreiveDetailVerifikasiNilai(PklPunyaKomponenPenilaianPkl)}.</li>
 * </ul>
 * <p>Bobot tidak perlu berjumlah 100: {@link #hitungTotalNilai(Boolean, List)} menormalkan sendiri
 * dengan membagi tiap bobot dengan total bobot yang terbaca.</p>
 *
 * <h2>Pola getter yang menulis balik</h2>
 * <p>Beberapa getter di kelas ini <b>mengubah keadaan object saat dibaca</b>. Karena Hibernate
 * melakukan dirty-check pada akhir transaksi, perubahan itu bisa berujung {@code UPDATE} yang tidak
 * diminta pemanggil &mdash; termasuk baris audit Envers baru. Daftar lengkapnya:</p>
 * <ul>
 * <li>{@link #getKelompokPkl()} dan {@link #getMahasiswa()} &mdash; menugaskan kembali hasil
 * {@code check(...)} ke field. Ini pola standar seluruh repo dan <b>tidak</b> mengubah data (object
 * yang sama, hanya proxy yang sudah teresolusi).</li>
 * <li>{@link #getLulus()} &mdash; menulis ulang field {@code lulus} agar cocok dengan master Nilai
 * Huruf, sehingga <b>mengoreksi data basi di basis data</b>. Ini disengaja.</li>
 * <li>{@link #getNamaDosen()} &mdash; menghitung ulang dan <b>menimpa</b> kolom {@code namaDosen}
 * dari daftar pembimbing kelompok setiap kali dipanggil.</li>
 * <li>{@link #toString()} &mdash; menugaskan hasil {@link #getKelompokPkl()}/{@link #getMahasiswa()}
 * ke field (efeknya sama dengan kedua getter di atas).</li>
 * <li>{@link #refreshNilaiKeDefault()} &mdash; bukan getter, tetapi dipanggil <i>dari dalam</i>
 * {@link #retreiveDetailNilai(KomponenPenilaianPkl)} dan
 * {@link #retreiveDetailVerifikasiNilai(PklPunyaKomponenPenilaianPkl)} yang bersifat "baca saja",
 * dan ia <b>dapat mengisi {@code detailNilai}</b> serta membuka session Hibernate.</li>
 * </ul>
 * <p><b>Tidak ada</b> getter di kelas ini yang menutup session Hibernate maupun menghapus data
 * (berbeda dengan beberapa entity lain di repo ini, mis. {@code JadwalUjianPMB.getRuanganYgIkut}).
 * {@link #refreshNilaiKeDefault()} dan {@link #bersihkanNilaiKeDefault()} memakai
 * {@link HibernateUtil#currentSession()} &mdash; session milik thread/permintaan yang sedang
 * berjalan &mdash; dan sengaja <b>tidak</b> menutupnya.</p>
 *
 * <h2>Sifat flag {@code diterima}</h2>
 * <p>Dua arah, bukan satu arah: checkbox di {@code KelompokPklHelper} memanggil
 * {@code setDiterima(checkbox.isChecked())} sehingga persetujuan bisa <b>dicabut</b> kembali.
 * {@link #getDiterima()} memetakan {@code null} menjadi {@code false} ("belum disetujui"), dan
 * blok yang dahulu memaksa {@code diterima = true} untuk kelompok yang tidak boleh dipilih
 * mahasiswa kini <b>dinonaktifkan</b> (masih tertinggal sebagai komentar di method tersebut).
 * Tombol hapus anggota di {@code KelompokPklHelper} sengaja disembunyikan begitu {@code diterima}
 * bernilai {@code true}.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; relasi</b> &mdash; {@link #getId()}, {@link #getKelompokPkl()},
 * {@link #getMahasiswa()}, {@link #getDetailperkuliahan()}, {@link #ambilVOPembelajaran()},
 * {@link #toString()}.</li>
 * <li><b>Status keanggotaan &amp; catatan</b> &mdash; {@link #getDiterima()},
 * {@link #getKeterangan()}, {@link #getHasil()}, {@link #getNamaDosen()}.</li>
 * <li><b>Ringkasan nilai</b> &mdash; {@link #getTotalNilai()}, {@link #getNilaiHuruf()},
 * {@link #getTotalIP()}, {@link #getLulus()} beserta setter-nya. Keempatnya <b>disimpan</b>, bukan
 * dihitung ulang saat dibaca (kecuali koreksi di {@link #getLulus()}).</li>
 * <li><b>Rincian nilai per komponen</b> &mdash; {@link #getDetailNilai()},
 * {@link #populateDetailNilai(KomponenPenilaianPkl, Double, Boolean)},
 * {@link #retreiveDetailNilai(KomponenPenilaianPkl)},
 * {@link #retreiveDetailVerifikasiNilai(PklPunyaKomponenPenilaianPkl)},
 * {@link #hitungTotalNilai(Boolean)} / {@link #hitungTotalNilai(Boolean, List)}.</li>
 * <li><b>Perawatan rincian nilai</b> &mdash; {@link #refreshNilaiKeDefault()} (isi awal dari nilai
 * lama), {@link #bersihkanNilaiKeDefault()} / {@link #bersihkanNilaiKeDefault(List)} (buang ruas
 * yatim/ganda), {@link #reloadPklPunyaKomponenPenilaianPkl(Session)} (susun ulang dari master
 * &mdash; lihat peringatan di bawah).</li>
 * <li><b>Cache berkas</b> &mdash; {@link #write()}.</li>
 * </ol>
 *
 * <h2>Cacat yang diketahui (jangan diperbaiki tanpa uji regresi)</h2>
 * <ul>
 * <li>{@link #reloadPklPunyaKomponenPenilaianPkl(Session)} menyusun Criteria atas
 * {@link PklPunyaKomponenPenilaianPkl} dengan properti {@code parent}, {@code persen}, dan
 * {@code statusPertemuan}. <b>Ketiganya tidak ada</b> pada entity tersebut (yang hanya punya
 * {@code id}, {@code nama}, {@code keterangan}, {@code pkl}, {@code komponenPenilaianPkl}), jadi
 * Hibernate akan melempar {@code QueryException: could not resolve property} bila method ini
 * dijalankan. Untungnya <b>tidak ada satu pun pemanggil</b> di seluruh pohon sumber &mdash; method
 * ini kode mati. Kembarannya {@code MahasiswaDapatKelompokKkn.reloadKknPunyaKomponenPenilaianKkn}
 * mengidap cacat yang sama persis; kemungkinan besar keduanya hasil salin-tempel dari entity
 * perkuliahan reguler yang memang punya ketiga properti itu.</li>
 * <li>{@link #bersihkanNilaiKeDefault(List)} memanggil {@code Long.parseLong} <b>tanpa</b>
 * {@code try/catch}, berbeda dari method sejenis di kelas ini yang selalu menelan
 * {@code NumberFormatException}. Satu ruas {@code detailNilai} yang rusak akan menggagalkan
 * seluruh penyimpanan nilai.</li>
 * <li>{@link #getLulus()} memaksa {@code lulus = false} bila {@code nilaiHuruf} masih
 * {@code null}, <b>menimpa</b> nilai yang mungkin sudah diset eksplisit lewat
 * {@link #setLulus(Boolean)}.</li>
 * </ul>
 *
 * @see Pkl
 * @see ais.database.model.pkl.KelompokPkl
 * @see MahasiswaDapatKelompokKkn
 * @see SiswaDapatKelompokPkl
 * @see GeneralValueObject
 * @see VOPesertaPembelajaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_dapat_kelompok_kelompok_pkl")

public class MahasiswaDapatKelompokPkl extends GeneralValueObject implements VOPesertaPembelajaran {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> supaya
	 * jejak audit lama tidak terhapus oleh layar yang menyimpan tanpa konteks pengguna.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau string
	 * kosong <b>diabaikan diam-diam</b>.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari konteks pengguna aktif dan memperbarui
	 * {@link #getTanggal_dirubah()}. Karena hook ini hanya terikat pada {@code @PreUpdate} (bukan
	 * {@code @PrePersist}), baris yang baru pertama kali di-{@code INSERT} mengandalkan nilai awal
	 * field dan setter yang dipanggil layar penyimpan.</p>
	 *
	 * <p>Jangan panggil manual dari kode aplikasi.</p>
	 *
	 * <p>Field {@code tanggal_dirubah} sengaja diinisialisasi ke waktu "sekarang" versi kampus
	 * ({@code WaktuUtil.getDate()}, sudah dikoreksi zona waktu WIB/WITA/WIT) agar baris baru pun
	 * punya stempel waktu yang masuk akal sebelum hook ini pernah berjalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; panggilan manual hanya dipakai importir
	 * yang ingin mempertahankan waktu asal data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks keanggotaan ini dalam bentuk {@code "<kelompok>-<mahasiswa>"}.
	 *
	 * <p>Dipakai komponen ZK (label, combobox, log revisi) yang menampilkan object apa adanya.
	 * Kedua bagiannya berasal dari {@code toString()} milik {@link ais.database.model.pkl.KelompokPkl}
	 * dan {@link Mahasiswa}.</p>
	 *
	 * <p><b>Efek samping:</b> method ini sengaja memanggil {@link #getKelompokPkl()} dan
	 * {@link #getMahasiswa()} (bukan membaca field langsung) lalu menugaskan hasilnya kembali ke
	 * field, sehingga proxy lazy yang belum teresolusi ikut dibereskan. Konsekuensinya
	 * {@code toString()} <b>bisa memicu query</b> &mdash; hindari memanggilnya di dalam gelung
	 * ketat atau pada object yang sudah detached tanpa session aktif.</p>
	 *
	 * @return gabungan nama kelompok dan mahasiswa dipisah tanda hubung
	 */
	public String toString() {
		kelompokPkl = getKelompokPkl();
		mahasiswa = getMahasiswa();
		return kelompokPkl + "-" + mahasiswa;
	}

	private String namaDosen;
	private KelompokPkl kelompokPkl;
	private Mahasiswa mahasiswa;
	private Detailperkuliahan detailperkuliahan;
	private String keterangan;
	private String hasil;

	private Double totalNilai;
	private String nilaiHuruf;
	private Double totalIP = 0.0;
	private Boolean lulus;

	private String detailNilai = "";
	private Boolean diterima;

	/**
	 * Menulis snapshot JSON baris ini ke berkas cache sementara lewat
	 * {@link GeneralValueObject#write(String...)}.
	 *
	 * <p>Daftar argumen bukan daftar properti yang ikut ditulis, melainkan
	 * <b>daftar nama kelas yang dikecualikan</b> dari penelusuran relasi
	 * ({@code clazzPengecualian} pada {@code Common.convertToJsonObject}). Tujuannya menahan
	 * ledakan graf: dari satu keanggotaan PKL, relasi {@link Mahasiswa} dan
	 * {@link ais.database.model.pkl.KelompokPkl} akan menyeret {@link Jurusan}, {@link Fakultas},
	 * {@link PerguruanTinggi}, {@link Dosen}, {@link Kurikulum}, dan seterusnya. Kelas ini sendiri
	 * ({@code MahasiswaDapatKelompokPkl}) juga ikut didaftarkan supaya penelusuran tidak berputar
	 * balik ke dirinya sendiri.</p>
	 *
	 * <p><b>Perhatian:</b> {@code MahasiswaDapatKelompokPkl} tidak termasuk daftar entity yang
	 * "selalu ditulis" di {@link GeneralValueObject#write(Integer, String...)}, sehingga penulisan
	 * hanya benar-benar terjadi bila kelas ini tidak terdaftar di cache konstanta. Nilai balik yang
	 * tidak {@code null} <b>bukan jaminan</b> berkasnya ada di disk.</p>
	 *
	 * @return berkas cache hasil penulisan, atau sekadar berkas penunjuk lokasi bila penulisan
	 *         dilewati
	 * @see GeneralValueObject#write(Integer, String...)
	 */
	public File write() {
		File f = write(Jurusan.class.getName(), Dosen.class.getName(), MahasiswaDapatKelompokPkl.class.getName(),
				Pegawai.class.getName(), Fakultas.class.getName(), PerguruanTinggi.class.getName(),
				LembagaPengangkat.class.getName(), TingkatKesulitanMatakuliah.class.getName(),
				Kurikulum.class.getName(), MasaPerkuliahan.class.getName(), JamPerkuliahan.class.getName(),
				JenisEvaluasi.class.getName(), Ruang.class.getName());
		return f;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Dipakai juga oleh layar yang membuat keanggotaan baru
	 * ({@code PklUntukMahasiswaAction}, {@code KelompokPklHelper},
	 * {@code AmbilDataMahasiswaKelompokPklHelper}). Setelah dibuat, pemanggil <b>wajib</b> mengisi
	 * {@link #setMahasiswa(Mahasiswa)} dan {@link #setKelompokPkl(KelompokPkl)} karena kedua kolom
	 * FK-nya {@code nullable = false}.</p>
	 */
	public MahasiswaDapatKelompokPkl() {
	}

	/**
	 * Kunci utama baris ini.
	 *
	 * @return id baris; {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Diisi Hibernate setelah {@code INSERT}; jangan diisi manual kecuali oleh
	 * importir data.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas panitia atas keanggotaan ini (mis. alasan penempatan atau catatan khusus).
	 *
	 * @return keterangan; {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas panitia.
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kelompok PKL yang diikuti mahasiswa ini.
	 *
	 * <p>Wajib diisi sebelum menyimpan ({@code nullable = false}). Diisi
	 * {@code PklUntukMahasiswaAction} saat mahasiswa memilih kelompok, atau oleh layar panitia yang
	 * memindahkan anggota antar kelompok.</p>
	 *
	 * @param kelompokPkl kelompok PKL tujuan
	 */
	public void setKelompokPkl(KelompokPkl kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

	/**
	 * Kelompok PKL yang diikuti mahasiswa ini &mdash; sekaligus satu-satunya jalan menuju program
	 * {@link Pkl} induk (lewat {@code getKelompokPkl().getPkl()}).
	 *
	 * <p>Relasi {@code LAZY}, jadi getter ini menjalankan pola standar repo:
	 * {@code kelompokPkl = check(kelompokPkl)} &mdash; hasil {@link GeneralValueObject#check(Object)}
	 * <b>ditugaskan kembali ke field</b> agar proxy yang sudah teresolusi (atau instance pengganti
	 * dari cache/session baru) dipakai seterusnya. Panggilan pada object detached pun tetap
	 * mengembalikan data karena {@code check()} punya jalur pemulihan lewat session baru; bila
	 * semua jalur gagal, argumen dikembalikan apa adanya (bisa berupa proxy yang belum
	 * terinisialisasi).</p>
	 *
	 * @return kelompok PKL; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pkl", nullable = false)
	public KelompokPkl getKelompokPkl() {
		kelompokPkl = check(kelompokPkl);
		return kelompokPkl;
	}

	/**
	 * Menetapkan mahasiswa pemilik keanggotaan ini. Wajib diisi sebelum menyimpan
	 * ({@code nullable = false}).
	 *
	 * @param mahasiswa mahasiswa anggota kelompok
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mahasiswa pemilik keanggotaan ini.
	 *
	 * <p>Relasi {@code LAZY} dengan pola resolusi proxy yang sama seperti
	 * {@link #getKelompokPkl()}: hasil {@link GeneralValueObject#check(Object)} ditugaskan kembali
	 * ke field.</p>
	 *
	 * @return mahasiswa anggota; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Nilai akhir PKL yang <b>tersimpan</b> di kolom {@code totalNilai}.
	 *
	 * <p>Getter ini <b>tidak menghitung ulang</b>. Nilainya baru berubah bila layar penilaian
	 * memanggil {@code setTotalNilai(hitungTotalNilai(true))} &mdash; lihat
	 * {@link #hitungTotalNilai(Boolean)}. Karena itu, bila bobot komponen di master diubah setelah
	 * penilaian selesai, angka di sini tetap angka lama sampai dinilai ulang.</p>
	 *
	 * @return nilai akhir 0..100; {@code 0.0} bila belum pernah dinilai (kolom {@code null})
	 */
	public Double getTotalNilai() {
		return totalNilai == null ? 0.0 : totalNilai;
	}

	/**
	 * Menyimpan nilai akhir PKL.
	 *
	 * <p>Dipanggil {@code PenilaianPklHelper} sesaat setelah
	 * {@link #hitungTotalNilai(Boolean)}, dan oleh importir nilai.</p>
	 *
	 * @param totalNilai nilai akhir 0..100
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Nilai huruf PKL yang tersimpan (mis. {@code "A"}, {@code "B+"}).
	 *
	 * <p>Hasilnya di-{@code trim} sehingga spasi sisa dari kolom {@code char}/impor tidak
	 * mengganggu pembandingan di {@link #getLulus()} dan di master {@code NilaiHuruf}.</p>
	 *
	 * @return nilai huruf tanpa spasi pinggir; {@code null} bila belum dinilai
	 */
	public String getNilaiHuruf() {
		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

	/**
	 * Menyimpan nilai huruf PKL.
	 *
	 * <p>Diisi {@code PenilaianPklHelper} dari hasil {@code Common.getNilaiHuruf(totalNilai, ...)}.
	 * Perhatikan {@link #getLulus()} membaca properti ini untuk menentukan kelulusan, jadi
	 * mengubahnya berpengaruh langsung pada status lulus.</p>
	 *
	 * @param nilaiHuruf nilai huruf sesuai master {@code NilaiHuruf}
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Menyusun ulang {@link #getDetailNilai() detailNilai} dari daftar komponen penilaian yang
	 * berlaku pada program PKL kelompok ini, sambil mempertahankan angka dan status verifikasi yang
	 * sudah ada.
	 *
	 * <p>Alurnya: {@link #refreshNilaiKeDefault()} dulu (agar data lama tidak hilang), lalu untuk
	 * tiap {@link PklPunyaKomponenPenilaianPkl} milik program disusun ruas baru
	 * {@code idKomponen,nilaiLama,0,bobot,verifikasiLama} &mdash; nilai lama dibaca
	 * {@link #retreiveDetailNilai(KomponenPenilaianPkl)} dan status verifikasi lama dibaca
	 * {@link #retreiveDetailVerifikasiNilai(PklPunyaKomponenPenilaianPkl)}. Hasilnya
	 * <b>menimpa</b> {@code detailNilai} sehingga ruas untuk komponen yang sudah dicabut dari
	 * program ikut terbuang.</p>
	 *
	 * <p><b>PERINGATAN &mdash; method ini tidak dapat dijalankan.</b> Criteria di dalamnya menyaring
	 * {@link PklPunyaKomponenPenilaianPkl} dengan properti {@code parent}, {@code persen}, dan
	 * {@code statusPertemuan}; <b>ketiganya tidak ada</b> pada entity tersebut, sehingga Hibernate
	 * akan melempar {@code QueryException: could not resolve property}. Tidak ada pemanggil di
	 * seluruh pohon sumber &mdash; ini kode mati hasil salin-tempel (kembarannya di
	 * {@link MahasiswaDapatKelompokKkn} mengidap cacat identik). Bila suatu saat method ini hendak
	 * dihidupkan, ketiga penyaring itu harus dibuang atau properti terkait ditambahkan ke entity.
	 * Untuk kebutuhan sehari-hari gunakan {@link #bersihkanNilaiKeDefault()} yang menyaring dengan
	 * benar lewat alias {@code komponenPenilaianPkl.aktif}.</p>
	 *
	 * <p>Galat per komponen ditelan dan hanya ditampilkan ke pengguna admin lewat
	 * {@code Common.tampilErrorJikaAdmin}, sehingga satu komponen bermasalah tidak menggagalkan
	 * seluruh penyusunan.</p>
	 *
	 * @param session session Hibernate aktif yang dipakai memuat daftar komponen; pemanggil yang
	 *                bertanggung jawab atas transaksi dan penutupannya
	 */
	@SuppressWarnings("unchecked")
	public void reloadPklPunyaKomponenPenilaianPkl(Session session) {

		refreshNilaiKeDefault();

		String formatbaru = "";
		List<PklPunyaKomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls = session
				.createCriteria(PklPunyaKomponenPenilaianPkl.class).add(Restrictions.eq("pkl", kelompokPkl.getPkl()))
				.add(Restrictions.isNull("parent")).add(Restrictions.gt("persen", 0.01))
				.createCriteria("statusPertemuan").add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("id")).list();
		for (PklPunyaKomponenPenilaianPkl pklPunyaKomponenPenilaianPkl : pklPunyaKomponenPenilaianPkls) {
			try {
				Double jumlah = retreiveDetailNilai(pklPunyaKomponenPenilaianPkl.getKomponenPenilaianPkl());
				Boolean verivy = retreiveDetailVerifikasiNilai(pklPunyaKomponenPenilaianPkl);
				String aformatBaru = pklPunyaKomponenPenilaianPkl.getKomponenPenilaianPkl().getId() + "," + jumlah
						+ ",0," + pklPunyaKomponenPenilaianPkl.getKomponenPenilaianPkl().getBobot() + "," + verivy;
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		detailNilai = formatbaru;
		// System.out.println("detailNilai baru => " + detailNilai);
	}

	/**
	 * Pintasan {@link #hitungTotalNilai(Boolean, List)} tanpa daftar komponen eksplisit.
	 *
	 * <p>Inilah bentuk yang dipakai layar penilaian: {@code PenilaianPklHelper} memanggil
	 * {@code setTotalNilai(hitungTotalNilai(true))} setiap kali penilai mengubah satu angka
	 * komponen, sehingga nilai akhir selalu sinkron dengan rinciannya.</p>
	 *
	 * @param gunakanPklPunyaKomponenPenilaianPklDariDatabase bila {@code true}, rincian dibersihkan
	 *        dulu terhadap daftar komponen aktif yang dibaca dari basis data (lihat
	 *        {@link #bersihkanNilaiKeDefault()}); bila {@code false}, isi {@code detailNilai}
	 *        dipakai apa adanya
	 * @return nilai akhir hasil rata-rata berbobot; {@code 0.0} bila rincian kosong
	 * @see #hitungTotalNilai(Boolean, List)
	 */
	public Double hitungTotalNilai(Boolean gunakanPklPunyaKomponenPenilaianPklDariDatabase) {
		return hitungTotalNilai(gunakanPklPunyaKomponenPenilaianPklDariDatabase, null);
	}

	/**
	 * Menghitung nilai akhir PKL sebagai <b>rata-rata berbobot</b> atas seluruh ruas
	 * {@link #getDetailNilai() detailNilai}.
	 *
	 * <p>Rumusnya {@code total = &Sigma; (nilai<sub>i</sub> &times; bobot<sub>i</sub> / &Sigma;bobot)}.
	 * Karena pembaginya adalah <b>jumlah bobot yang benar-benar terbaca</b> (bukan konstanta 100),
	 * bobot komponen tidak wajib berjumlah 100 dan komponen yang belum dinilai tetap ikut
	 * menurunkan nilai (nilainya dibaca {@code 0.0}). Bila total bobot &le; 0,001, hasilnya
	 * {@code 0.0} &mdash; pembagian nol dihindari.</p>
	 *
	 * <p>Urutan kerjanya:</p>
	 * <ol>
	 * <li>{@link #refreshNilaiKeDefault()} &mdash; mengisi rincian dari nilai lama bila rinciannya
	 * masih kosong padahal nilai akhirnya sudah ada (data warisan).</li>
	 * <li>Bila {@code gunakanPklPunyaKomponenPenilaianPklDariDatabase} bernilai {@code true},
	 * rincian dibersihkan dari ruas yatim/ganda: dengan {@code pklPunyaKomponenPenilaianPkls}
	 * bila diberikan, atau membaca sendiri dari basis data bila {@code null}.</li>
	 * <li>Setiap ruas diurai; medan 4 (bobot) dan medan 2 (nilai) diambil dengan penguraian
	 * <b>bertahan galat</b> &mdash; ruas yang berisi teks {@code "null"} atau kosong tidak
	 * melempar {@code NumberFormatException}, melainkan diperlakukan sebagai bobot tak dikenal
	 * (ruas dilewati) atau nilai {@code 0.0}.</li>
	 * <li>Ruas dengan id komponen yang sama <b>menimpa</b> satu sama lain karena ditampung dalam
	 * {@code Map} berkunci id &mdash; ruas ganda otomatis dihitung sekali (yang terakhir menang),
	 * <b>tetapi</b> bobotnya sudah terlanjur ikut dijumlahkan ke pembagi pada tiap iterasi,
	 * sehingga rincian yang mengandung ruas ganda akan menghasilkan nilai lebih rendah dari
	 * seharusnya. Bersihkan dulu dengan {@link #bersihkanNilaiKeDefault()} untuk menghindarinya.</li>
	 * </ol>
	 *
	 * <p><b>Method ini tidak menyimpan apa pun</b> ke {@code totalNilai}; pemanggil yang wajib
	 * memanggil {@link #setTotalNilai(Double)}. Efek samping yang ada hanyalah kemungkinan
	 * berubahnya {@code detailNilai} lewat kedua langkah pembersihan di atas.</p>
	 *
	 * @param gunakanPklPunyaKomponenPenilaianPklDariDatabase {@code true} untuk membersihkan
	 *        rincian terhadap daftar komponen yang berlaku sebelum menghitung
	 * @param pklPunyaKomponenPenilaianPkls daftar komponen yang dianggap berlaku; {@code null}
	 *        berarti daftar dibaca sendiri dari basis data lewat {@link #bersihkanNilaiKeDefault()}.
	 *        Diabaikan bila parameter pertama {@code false}
	 * @return nilai akhir hasil rata-rata berbobot; {@code 0.0} bila rincian kosong atau total
	 *         bobotnya nol
	 */
	public Double hitungTotalNilai(Boolean gunakanPklPunyaKomponenPenilaianPklDariDatabase,
			List<PklPunyaKomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls) {

		refreshNilaiKeDefault();
		if (gunakanPklPunyaKomponenPenilaianPklDariDatabase) {
			if (pklPunyaKomponenPenilaianPkls == null) {
				bersihkanNilaiKeDefault();
			} else {
				bersihkanNilaiKeDefault(pklPunyaKomponenPenilaianPkls);
			}
		}

		Double total = 0.0;
		String str = getDetailNilai();
		Double totalPersen = 0.0;

		if (str != null && !str.trim().isEmpty()) {
			String[] s = StringUtils.split(str, ";");
			Map<Long, Object[]> nilais = new HashMap<Long, Object[]>();
			for (String ss : s) {
				try {
					String[] sss = StringUtils.split(ss, ",");
					Long idPklPunyaKomponenPenilaianPkl = Long.parseLong(sss[0].trim());
					// Parse aman: nilai/persen bisa berisi teks "null" atau kosong (komponen belum
					// diisi) yang membuat Double.parseDouble melempar NumberFormatException.
					Double persen = null;
					if (sss.length > 3) {
						try {
							persen = Double.parseDouble(sss[3].trim());
						} catch (Exception ig) {
							persen = null;
						}
					}
					if (persen != null) {
						double n = 0.0;
						try {
							if (sss.length > 1 && sss[1] != null) {
								n = Double.parseDouble(sss[1].trim());
							}
						} catch (Exception ig) {
							n = 0.0;
						}
						nilais.put(idPklPunyaKomponenPenilaianPkl, new Object[] { n, persen });

						totalPersen += persen;

					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (totalPersen > 0.001) {
				for (Long pklPunyaKomponenPenilaianPkl : nilais.keySet()) {
					try {
						Double n = (Double) nilais.get(pklPunyaKomponenPenilaianPkl)[0];
						Double persen = (Double) nilais.get(pklPunyaKomponenPenilaianPkl)[1];
						total += (n * (persen / totalPersen));
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

		return total;
	}

	/**
	 * Menyimpan (menyisipkan atau memperbarui) nilai <b>satu komponen penilaian</b> ke dalam
	 * {@link #getDetailNilai() detailNilai}.
	 *
	 * <p>Ini pintu masuk utama penilaian: setiap kotak angka di form
	 * {@code PenilaianPklHelper} memanggil method ini pada event {@code onChange}, lalu
	 * dilanjutkan {@code setTotalNilai(hitungTotalNilai(true))}.</p>
	 *
	 * <p>Cara kerjanya bersifat <i>upsert</i> pada teks: seluruh ruas ditelusuri, ruas dengan id
	 * komponen yang cocok <b>ditulis ulang</b> dengan nilai dan bobot terkini, ruas lain disalin
	 * apa adanya. Bila komponen belum pernah ada, ruas baru ditambahkan di akhir. Ruas dengan medan
	 * pertama kosong dibuang diam-diam &mdash; pembersihan pasif atas data rusak.</p>
	 *
	 * <p><b>Aturan bisnis:</b> nilai di bawah {@code 0,01} memaksa {@code verify = false}, sehingga
	 * komponen yang belum diisi tidak bisa "terverifikasi". Perhatikan bahwa {@code jumlah}
	 * bernilai {@code null} <b>tidak</b> terkena aturan ini &mdash; ia ditulis apa adanya menjadi
	 * teks {@code "null"} pada medan 2, yang kemudian dibaca sebagai {@code 0.0} oleh
	 * {@link #hitungTotalNilai(Boolean, List)} dan
	 * {@link #retreiveDetailNilai(KomponenPenilaianPkl)}.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah field {@code detailNilai} langsung (bukan lewat setter);
	 * perubahan akan ikut ter-{@code UPDATE} pada flush berikutnya. Method ini <b>tidak</b>
	 * memperbarui {@link #getTotalNilai()}, {@link #getNilaiHuruf()}, maupun {@link #getLulus()}.
	 * Bila {@code komponenPenilaianPkl} {@code null}, method tidak melakukan apa pun.</p>
	 *
	 * @param komponenPenilaianPkl komponen yang dinilai; bila {@code null} method langsung selesai
	 * @param jumlah nilai angka untuk komponen tersebut; boleh {@code null}
	 * @param verify penanda bahwa nilai sudah diverifikasi; dipaksa {@code false} bila
	 *        {@code jumlah} lebih kecil dari {@code 0,01}
	 */
	public void populateDetailNilai(KomponenPenilaianPkl komponenPenilaianPkl, Double jumlah, Boolean verify) {
		if (jumlah != null && jumlah < 0.01) {
			verify = false;
		}
		if (komponenPenilaianPkl != null) {
			String formatBaru = "";
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						if (komponenPenilaianPkl.getId().equals(formatId)) {
							aformatBaru = komponenPenilaianPkl.getId() + "," + jumlah + ",0,"
									+ komponenPenilaianPkl.getBobot() + "," + verify;
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = komponenPenilaianPkl.getId() + "," + jumlah + ",0,"
						+ komponenPenilaianPkl.getBobot() + "," + verify;
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatBaru;
		}

	}

	/**
	 * Membuang dari {@link #getDetailNilai() detailNilai} setiap ruas yang komponennya <b>tidak
	 * lagi berlaku</b> pada program PKL kelompok ini, dengan daftar komponen dibaca sendiri dari
	 * basis data.
	 *
	 * <p>Daftar acuannya adalah seluruh {@link PklPunyaKomponenPenilaianPkl} milik
	 * {@code kelompokPkl.getPkl()} yang komponennya aktif &mdash; penyaringannya
	 * {@code komponenPenilaianPkl.aktif IS NULL OR komponenPenilaianPkl.aktif = true}, jadi
	 * komponen lama yang belum pernah diisi flag aktifnya tetap dianggap berlaku.</p>
	 *
	 * <p><b>Efek samping &amp; jebakan:</b></p>
	 * <ul>
	 * <li>Memakai {@link HibernateUtil#currentSession()} &mdash; session milik thread yang sedang
	 * berjalan. Session <b>tidak ditutup</b> di sini, dan itu memang benar: penutupan diurus daur
	 * hidup permintaan.</li>
	 * <li>Membaca field {@code kelompokPkl} <b>langsung</b>, bukan lewat {@link #getKelompokPkl()}.
	 * Bila field itu masih {@code null} (object baru yang belum diisi), method melempar
	 * {@code NullPointerException}.</li>
	 * <li>Memicu query setiap kali dipanggil; karena {@link #hitungTotalNilai(Boolean)} memanggil
	 * method ini, menghitung nilai untuk seratus mahasiswa dalam gelung berarti seratus query.</li>
	 * </ul>
	 *
	 * @see #bersihkanNilaiKeDefault(List)
	 */
	@SuppressWarnings("unchecked")
	public void bersihkanNilaiKeDefault() {
		Session session = HibernateUtil.currentSession();
		List<PklPunyaKomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls = session
				.createCriteria(PklPunyaKomponenPenilaianPkl.class)
				.createAlias("komponenPenilaianPkl", "komponenPenilaianPkl")
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianPkl.aktif"),
						Restrictions.eq("komponenPenilaianPkl.aktif", true)))
				.add(Restrictions.eq("pkl", kelompokPkl.getPkl())).list();
		bersihkanNilaiKeDefault(pklPunyaKomponenPenilaianPkls);
	}

	/**
	 * Menyaring {@link #getDetailNilai() detailNilai} terhadap daftar komponen yang diberikan
	 * pemanggil &mdash; varian tanpa query, dipakai bila daftar komponen sudah dimuat sekali untuk
	 * banyak mahasiswa (mis. saat menilai satu kelompok penuh).
	 *
	 * <p>Dua penyaringan dilakukan sekaligus:</p>
	 * <ol>
	 * <li><b>Buang yatim</b> &mdash; ruas yang id komponennya tidak ada dalam daftar acuan
	 * dihapus.</li>
	 * <li><b>Buang ganda</b> &mdash; id komponen yang sudah pernah muncul dilewati, sehingga hanya
	 * kemunculan <b>pertama</b> yang bertahan.</li>
	 * </ol>
	 *
	 * <p>Bila {@code detailNilai} kosong atau {@code null}, method tidak melakukan apa pun (field
	 * dibiarkan seperti semula, tidak dikosongkan).</p>
	 *
	 * <p><b>Jebakan:</b> penguraian {@code Long.parseLong(sss[0].trim())} di sini <b>tidak
	 * dibungkus {@code try/catch}</b>, berbeda dari method sejenis di kelas ini. Satu ruas
	 * {@code detailNilai} yang rusak (medan pertama bukan angka) akan melempar
	 * {@code NumberFormatException} keluar dari method dan menggagalkan seluruh alur penilaian yang
	 * memanggilnya.</p>
	 *
	 * @param pklPunyaKomponenPenilaianPkls daftar komponen yang dianggap berlaku; ruas di luar
	 *        daftar ini dibuang. Tidak boleh {@code null}
	 */
	public void bersihkanNilaiKeDefault(List<PklPunyaKomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls) {
		String formatbaru = "";

		if (detailNilai != null && !detailNilai.trim().isEmpty()) {
			String[] s = StringUtils.split(detailNilai, ";");

			List<Long> ids = new ArrayList<Long>();
			for (PklPunyaKomponenPenilaianPkl pklPunyaKomponenPenilaianPkl : pklPunyaKomponenPenilaianPkls) {
				ids.add(pklPunyaKomponenPenilaianPkl.getKomponenPenilaianPkl().getId());
			}

			List<Long> idPklPunyaKomponenPenilaianPkls = new ArrayList<Long>();
			for (String ss : s) {
				String[] sss = StringUtils.split(ss, ",");
				Long idPklPunyaKomponenPenilaianPkl = Long.parseLong(sss[0].trim());
				if (!idPklPunyaKomponenPenilaianPkls.contains(idPklPunyaKomponenPenilaianPkl)) {
					idPklPunyaKomponenPenilaianPkls.add(idPklPunyaKomponenPenilaianPkl);
					if (ids.contains(idPklPunyaKomponenPenilaianPkl)) {
						formatbaru += formatbaru.isEmpty() ? ss : ";" + ss;
					}
				}
			}

			detailNilai = formatbaru;
		}
	}

	/**
	 * Migrasi data warisan: membangun {@link #getDetailNilai() detailNilai} dari
	 * {@link #getTotalNilai() totalNilai} untuk baris lama yang dulu hanya menyimpan nilai akhir
	 * tanpa rincian per komponen.
	 *
	 * <p><b>Hanya berjalan bila kedua syarat terpenuhi</b>: {@code detailNilai} kosong/{@code null}
	 * <b>dan</b> {@code totalNilai} lebih besar dari {@code 1,0}. Di luar itu method langsung
	 * selesai tanpa efek apa pun &mdash; itulah sebabnya ia aman dipanggil dari mana saja, termasuk
	 * dari getter "baca saja" seperti {@link #retreiveDetailNilai(KomponenPenilaianPkl)}.</p>
	 *
	 * <p>Isinya: setiap komponen yang berlaku diberi <b>nilai yang sama</b>, yaitu {@code totalNilai}
	 * itu sendiri, dengan verifikasi {@code false}. Karena {@link #hitungTotalNilai(Boolean, List)}
	 * adalah rata-rata berbobot, memberi angka identik ke semua komponen membuat hasil hitungnya
	 * kembali sama dengan {@code totalNilai} semula &mdash; nilai lama terjaga.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah field {@code detailNilai} (akan ter-{@code UPDATE} pada flush
	 * berikutnya) dan menjalankan satu query lewat {@link HibernateUtil#currentSession()} (session
	 * milik thread, tidak ditutup di sini). Sama seperti {@link #bersihkanNilaiKeDefault()}, field
	 * {@code kelompokPkl} dibaca langsung sehingga object yang belum punya kelompok akan memicu
	 * {@code NullPointerException} &mdash; namun hanya bila kedua syarat di atas terpenuhi.</p>
	 */
	@SuppressWarnings("unchecked")
	public void refreshNilaiKeDefault() {
		if ((detailNilai == null || detailNilai.trim().isEmpty()) && totalNilai != null && totalNilai > 1.0) {
			String formatbaru = "";
			Session session = HibernateUtil.currentSession();
			List<PklPunyaKomponenPenilaianPkl> pklPunyaKomponenPenilaianPkls = session
					.createCriteria(PklPunyaKomponenPenilaianPkl.class)
					.createAlias("komponenPenilaianPkl", "komponenPenilaianPkl")
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianPkl.aktif"),
							Restrictions.eq("komponenPenilaianPkl.aktif", true)))
					.add(Restrictions.eq("pkl", kelompokPkl.getPkl())).list();

			for (PklPunyaKomponenPenilaianPkl pklPunyaKomponenPenilaianPkl : pklPunyaKomponenPenilaianPkls) {

				String aformatBaru = pklPunyaKomponenPenilaianPkl.getKomponenPenilaianPkl().getId() + "," + totalNilai
						+ ",0," + pklPunyaKomponenPenilaianPkl.getKomponenPenilaianPkl().getBobot() + ",false";
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatbaru;
		}
	}

	/**
	 * Membaca nilai angka satu komponen penilaian dari {@link #getDetailNilai() detailNilai}.
	 *
	 * <p>Dipakai form penilaian ({@code PenilaianPklHelper}) untuk mengisi kotak angka saat form
	 * dibuka, dan oleh laporan/ekspor yang menampilkan rincian nilai per komponen.</p>
	 *
	 * <p>Penelusurannya linear atas seluruh ruas, berhenti pada id komponen yang cocok. Ruas cacat
	 * (kurang dari dua medan, medan pertama kosong, atau bukan angka) dilewati diam-diam &mdash;
	 * galatnya hanya dicatat ke audit error, tidak ditampilkan.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #refreshNilaiKeDefault()} lebih dulu, sehingga
	 * pembacaan ini <b>dapat mengisi {@code detailNilai}</b> dan menjalankan query bila baris ini
	 * masih berupa data warisan.</p>
	 *
	 * @param formatIdSource komponen yang nilainya dicari; {@code null} atau ber-{@code id}
	 *        {@code null} langsung menghasilkan {@code 0.0}
	 * @return nilai angka komponen tersebut; {@code 0.0} bila komponen tidak ditemukan, medannya
	 *         kosong, atau tidak dapat diurai
	 */
	public Double retreiveDetailNilai(KomponenPenilaianPkl formatIdSource) {

		refreshNilaiKeDefault();

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length < 2 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (formatIdSource.getId().equals(formatId)) {
						return s[1] == null || s[1].trim().isEmpty() ? 0.0 : Double.parseDouble(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaDapatKelompokPkl.java:393");

				}
			}
		}

		return 0.0;
	}

	/**
	 * Membaca penanda verifikasi (medan 5) satu komponen penilaian dari
	 * {@link #getDetailNilai() detailNilai}.
	 *
	 * <p>Berbeda dari {@link #retreiveDetailNilai(KomponenPenilaianPkl)}, parameternya adalah
	 * <b>pengait</b> {@link PklPunyaKomponenPenilaianPkl}, bukan komponennya langsung; id yang
	 * dibandingkan tetap {@code formatIdSource.getKomponenPenilaianPkl().getId()} sehingga
	 * konsisten dengan skema penyimpanan.</p>
	 *
	 * <p><b>Aturan bisnis:</b> komponen yang nilainya di bawah {@code 0,01} <b>selalu</b> dianggap
	 * belum terverifikasi, berapa pun isi medan 5 &mdash; pasangan dari aturan yang sama di
	 * {@link #populateDetailNilai(KomponenPenilaianPkl, Double, Boolean)}, sekaligus jaring pengaman
	 * bagi data lama yang terlanjur ditandai terverifikasi padahal kosong.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #refreshNilaiKeDefault()} lebih dulu (lihat catatan
	 * pada {@link #retreiveDetailNilai(KomponenPenilaianPkl)}).</p>
	 *
	 * <p><b>Catatan ketahanan:</b> berbeda dari {@code retreiveDetailNilai}, di sini tidak ada
	 * pemeriksaan panjang larik &mdash; ruas yang kurang dari lima medan akan melempar
	 * {@code ArrayIndexOutOfBoundsException} yang ditelan {@code catch} dan dicatat ke audit error,
	 * lalu penelusuran lanjut ke ruas berikutnya.</p>
	 *
	 * @param formatIdSource pengait komponen yang status verifikasinya dicari; {@code null} atau
	 *        ber-{@code id} {@code null} langsung menghasilkan {@code false}
	 * @return {@code true} hanya bila ruasnya ditemukan, nilainya &ge; {@code 0,01}, dan medan 5
	 *         berbunyi {@code true}; selain itu {@code false}
	 */
	public Boolean retreiveDetailVerifikasiNilai(PklPunyaKomponenPenilaianPkl formatIdSource) {

		refreshNilaiKeDefault();

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					Long formatId = Long.parseLong(s[0]);
					if (formatIdSource.getKomponenPenilaianPkl().getId().equals(formatId)) {
						if (Double.parseDouble(s[1]) < 0.01) {
							return false;
						}
						return Boolean.parseBoolean(s[4]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaDapatKelompokPkl.java:418");

				}
			}
		}

		return false;
	}

	/**
	 * Bobot nilai huruf PKL dalam skala IPK (mis. {@code 4.0} untuk {@code "A"}).
	 *
	 * <p>Disalin {@code PenilaianPklHelper} dari master {@code NilaiHuruf}
	 * ({@code nilaiHuruf.getNilaiDiIPK()}) bersamaan dengan {@link #setNilaiHuruf(String)}, lalu
	 * diteruskan ke {@link Detailperkuliahan} agar ikut dihitung dalam IPK mahasiswa.</p>
	 *
	 * @return bobot IPK; {@code 0.0} bila kolomnya {@code null}
	 */
	public Double getTotalIP() {
		return totalIP == null ? 0.0 : totalIP;
	}

	/**
	 * Menyimpan bobot nilai huruf dalam skala IPK.
	 *
	 * @param totalIP bobot IPK sesuai master {@code NilaiHuruf}
	 */
	public void setTotalIP(Double totalIP) {
		this.totalIP = totalIP;
	}

	/**
	 * Status kelulusan PKL, <b>dikoreksi terhadap konfigurasi master Nilai Huruf setiap kali
	 * dibaca</b>.
	 *
	 * <p>Urutan penentuannya:</p>
	 * <ol>
	 * <li><b>Master Nilai Huruf (paling utama)</b> &mdash; bila {@link #getNilaiHuruf()} terisi,
	 * status diambil dari {@code ConstantValues.lulusDariNilaiHuruf(nilaiHuruf, mahasiswa)} yang
	 * memilih konfigurasi berjenjang Jurusan &rarr; Fakultas &rarr; global sesuai mahasiswa
	 * bersangkutan. Bila konfigurasi cocok ditemukan, hasilnya <b>ditulis balik</b> ke field
	 * {@code lulus} sehingga nilai basi di basis data ikut terperbaiki, lalu dikembalikan.</li>
	 * <li><b>Heuristik huruf (cadangan)</b> &mdash; hanya bila {@code lulus} masih {@code null}:
	 * huruf yang kosong atau mengandung {@code D}, {@code E}, atau {@code T} dianggap tidak lulus,
	 * selain itu lulus. Perhatikan pemeriksaannya memakai {@code contains} pada huruf yang sudah
	 * di-{@code toUpperCase}, jadi huruf gabungan seperti {@code "BD"} pun akan dianggap tidak
	 * lulus.</li>
	 * <li><b>Default</b> &mdash; bila {@code lulus} masih {@code null} dan tidak ada huruf sama
	 * sekali, dianggap {@code true}.</li>
	 * <li><b>Pemaksaan akhir</b> &mdash; bila {@code nilaiHuruf} {@code null}, {@code lulus}
	 * dipaksa {@code false}. Langkah ini <b>menimpa</b> nilai yang mungkin sudah diset eksplisit
	 * lewat {@link #setLulus(Boolean)} maupun default {@code true} pada langkah sebelumnya, jadi
	 * praktisnya: <b>tanpa nilai huruf, tidak pernah lulus</b>.</li>
	 * </ol>
	 *
	 * <p>Galat dari pencarian konfigurasi ditelan dan dicatat ke audit error &mdash; kegagalan
	 * membaca master tidak boleh membuat layar penilaian gagal render; alurnya jatuh ke
	 * heuristik.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code nilaiHuruf} (hasil {@code trim}) dan
	 * {@code lulus}. Karena {@code lulus} adalah kolom terpetakan, koreksi ini akan
	 * ter-{@code UPDATE} pada flush berikutnya beserta satu baris audit Envers &mdash; sekadar
	 * membaca status kelulusan seluruh peserta bisa menghasilkan banyak {@code UPDATE}.</p>
	 *
	 * @return {@code true} bila mahasiswa dinyatakan lulus PKL
	 */
	public Boolean getLulus() {
		nilaiHuruf = getNilaiHuruf();
		// Utamakan KONFIGURASI Nilai Huruf yang DIPEROLEH (permintaan): status lulus mengikuti master
		// Nilai Huruf (ConstantValues.lulusDariNilaiHuruf) & mengoreksi nilai tersimpan yang basi.
		try {
			if (nilaiHuruf != null && !nilaiHuruf.trim().isEmpty()) {
				Boolean cfgLulus = ais.common.ConstantValues.lulusDariNilaiHuruf(nilaiHuruf, getMahasiswa());
				if (cfgLulus != null) {
					if (lulus == null || !lulus.equals(cfgLulus)) {
						lulus = cfgLulus;
					}
					return lulus;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaDapatKelompokPkl.java:449");
		}
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.isEmpty() || nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}

		if (nilaiHuruf == null) {
			lulus = false;
		}

		return lulus;
	}

	/**
	 * Menyimpan status kelulusan PKL secara eksplisit.
	 *
	 * <p>Diisi {@code PenilaianPklHelper} dari master {@code NilaiHuruf}. Perhatikan bahwa
	 * {@link #getLulus()} dapat <b>menimpa</b> nilai yang diset di sini pada pembacaan berikutnya
	 * &mdash; master Nilai Huruf selalu menang, dan tanpa {@code nilaiHuruf} statusnya dipaksa
	 * {@code false}.</p>
	 *
	 * @param lulus status kelulusan; {@code null} berarti "belum ditentukan" dan akan diisi
	 *        {@link #getLulus()}
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Baris KRS ({@link Detailperkuliahan}) matakuliah PKL milik mahasiswa ini &mdash; jembatan
	 * agar nilai PKL masuk ke KHS dan IPK.
	 *
	 * <p>Kolomnya {@code nullable}. Bila masih kosong, perender grid di
	 * {@code PenilaianPklHelper} <b>mencarinya sendiri</b>: KRS berstatus {@code DISETUJUI} milik
	 * mahasiswa yang nama matakuliah (atau matakuliah konversinya) mengandung istilah "PKL" sesuai
	 * {@code Common.getBahasaConfig("pkl")}, diambil semester terbaru, lalu hasilnya
	 * <b>disimpan</b> ke baris ini lewat {@link #setDetailperkuliahan(Detailperkuliahan)} +
	 * {@code Common.refreshUpdate(...)}. Panitia juga dapat memilihnya manual lewat
	 * {@code AmbilDataDetailPerkuliahanBanbox}.</p>
	 *
	 * <p>Setelah tertaut, {@code PenilaianPklHelper} menyalin {@link #getTotalIP()},
	 * {@link #getNilaiHuruf()}, dan {@link #getLulus()} ke KRS tersebut setiap kali nilai
	 * berubah.</p>
	 *
	 * <p>Berbeda dari dua relasi lain di kelas ini, getter ini <b>tidak</b> memanggil
	 * {@code check(...)}: relasinya {@code EAGER} secara efektif ({@code @Fetch(FetchMode.SELECT)}
	 * tanpa {@code FetchType.LAZY}), sehingga Hibernate memuatnya lewat query terpisah saat entity
	 * dibaca.</p>
	 *
	 * @return baris KRS matakuliah PKL; {@code null} bila belum tertaut
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detailperkuliahan_id", nullable = true)
	public Detailperkuliahan getDetailperkuliahan() {
		return detailperkuliahan;
	}

	/**
	 * Menautkan keanggotaan ini ke baris KRS matakuliah PKL.
	 *
	 * @param detailperkuliahan baris KRS tujuan penyaluran nilai; boleh {@code null}
	 * @see #getDetailperkuliahan()
	 */
	public void setDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/**
	 * Rincian nilai per komponen dalam bentuk teks terserial.
	 *
	 * <p>Formatnya {@code idKomponen,nilai,0,bobot,verifikasi} per ruas, antar ruas dipisah
	 * {@code ;} &mdash; uraian lengkapnya ada di Javadoc kelas, bagian "Format
	 * {@code detailNilai}". Jangan diurai sendiri di kode pemanggil; pakai
	 * {@link #retreiveDetailNilai(KomponenPenilaianPkl)},
	 * {@link #retreiveDetailVerifikasiNilai(PklPunyaKomponenPenilaianPkl)}, dan
	 * {@link #populateDetailNilai(KomponenPenilaianPkl, Double, Boolean)}.</p>
	 *
	 * <p>Dipetakan ke kolom bertipe {@code text} sehingga panjangnya tidak dibatasi.</p>
	 *
	 * @return rincian nilai terserial; string kosong (bukan {@code null}) bila belum ada rincian
	 */
	@Column(columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai == null ? "" : detailNilai.trim();
	}

	/**
	 * Mengisi rincian nilai terserial secara langsung.
	 *
	 * <p>Hanya untuk importir/penyalin data yang sudah memegang string berformat benar; alur normal
	 * memakai {@link #populateDetailNilai(KomponenPenilaianPkl, Double, Boolean)}. Mengisi string
	 * yang formatnya salah akan menghasilkan nilai akhir yang keliru dan, lewat
	 * {@link #bersihkanNilaiKeDefault(List)}, bisa melempar {@code NumberFormatException}.</p>
	 *
	 * @param detailNilai rincian nilai terserial
	 */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/**
	 * Apakah keanggotaan ini sudah <b>disetujui</b> panitia.
	 *
	 * <p>Membedakan usulan dari penempatan sah: baris yang dibuat mahasiswa sendiri lewat
	 * {@code PklUntukMahasiswaAction} bernilai {@code false} sampai panitia mencentangnya di
	 * {@code KelompokPklHelper}, sedangkan baris yang dibuat admin lewat
	 * {@code AmbilDataMahasiswaKelompokPklHelper} langsung {@code true}. Flag ini juga dipakai
	 * sebagai penyaring di layar penilaian ({@code Restrictions.eq("diterima", true)}), penentu
	 * boleh-tidaknya sertifikat dicetak, dan penentu boleh-tidaknya baris dihapus (tombol hapus
	 * disembunyikan begitu disetujui).</p>
	 *
	 * <p>Sifatnya <b>dua arah</b> &mdash; persetujuan dapat dicabut kembali lewat
	 * {@link #setDiterima(Boolean)}.</p>
	 *
	 * <p>Blok yang dikomentari di dalam method adalah aturan lama: dahulu kelompok yang tidak boleh
	 * dipilih mahasiswa ({@code kelompokPkl.getMahasiswaBisaMemilih() == false}) otomatis dianggap
	 * disetujui. Aturan itu sengaja dinonaktifkan &mdash; sekarang semua keanggotaan menunggu
	 * persetujuan eksplisit. Jangan dihidupkan tanpa memeriksa kembali layar persetujuan.</p>
	 *
	 * @return {@code true} bila sudah disetujui; {@code false} bila belum, termasuk saat kolomnya
	 *         masih {@code null}
	 */
	public Boolean getDiterima() {
		// if (kelompokPkl != null && !kelompokPkl.getMahasiswaBisaMemilih()) {
		// diterima = true;
		// }
		return diterima == null ? false : diterima;
	}

	/**
	 * Menetapkan status persetujuan keanggotaan.
	 *
	 * <p>Dipanggil checkbox "Diterima" di {@code KelompokPklHelper} (dua arah: bisa dicentang dan
	 * dilepas), oleh penambahan massal di {@code AmbilDataMahasiswaKelompokPklHelper} (selalu
	 * {@code true}, setelah kuota kelompok diperiksa), dan oleh impor data.</p>
	 *
	 * @param diterima {@code true} bila keanggotaan disetujui
	 */
	public void setDiterima(Boolean diterima) {
		this.diterima = diterima;
	}

	/**
	 * Nama dosen pembimbing kelompok, sebagai satu string siap tampil.
	 *
	 * <p>Nilainya <b>dihitung ulang setiap kali dipanggil</b> (selama field {@code kelompokPkl}
	 * sudah terisi) dari {@code KelompokPkl.populateDosenBuNama()} &mdash; daftar nama seluruh dosen
	 * pembimbing 1..10 yang terisi &mdash; lalu kurung siku {@code [} dan {@code ]} bawaan
	 * {@code List.toString()} dibuang sehingga tersisa {@code "Nama A, Nama B"}.</p>
	 *
	 * <p><b>Efek samping penting:</b> {@code namaDosen} <b>adalah kolom terpetakan</b> (property
	 * access, tanpa {@code @Transient}), jadi hasil hitung ulang ini menimpa isi kolom dan akan
	 * ter-{@code UPDATE} pada flush berikutnya &mdash; pembacaan biasa dapat menghasilkan tulisan ke
	 * basis data beserta baris audit Envers. Kolom itu berperan sebagai <i>cache</i> penamaan:
	 * bila {@code kelompokPkl} kebetulan {@code null} (mis. object hasil proyeksi parsial), nilai
	 * tersimpan yang terakhir dikembalikan apa adanya.</p>
	 *
	 * <p>Perhatikan pula field {@code kelompokPkl} dibaca <b>langsung</b>, bukan lewat
	 * {@link #getKelompokPkl()}, sehingga proxy lazy tidak diresolusi oleh {@code check(...)} di
	 * sini; pemanggilan {@code populateDosenBuNama()} sendirilah yang memicu inisialisasi proxy.</p>
	 *
	 * @return daftar nama pembimbing dipisah koma; dapat {@code null} bila kelompok belum terisi
	 *         dan kolomnya belum pernah ditulis
	 */
	public String getNamaDosen() {
		if (kelompokPkl != null) {
			namaDosen = org.apache.commons.lang3.StringUtils.replace(
					org.apache.commons.lang3.StringUtils.replace(kelompokPkl.populateDosenBuNama().toString(), "]", ""),
					"[", "");
		}
		return namaDosen;
	}

	/**
	 * Mengisi cache nama pembimbing.
	 *
	 * <p>Praktis tidak berguna dipanggil manual: {@link #getNamaDosen()} akan menghitung ulang dan
	 * menimpanya begitu {@code kelompokPkl} terisi. Setter ini ada karena dibutuhkan Hibernate
	 * (property access) dan generator CRUD dinamis.</p>
	 *
	 * @param namaDosen daftar nama pembimbing siap tampil
	 */
	public void setNamaDosen(String namaDosen) {
		this.namaDosen = namaDosen;
	}

	/**
	 * Implementasi {@link VOPesertaPembelajaran}: objek pembelajaran yang diikuti peserta ini.
	 *
	 * <p>Mengembalikan {@link ais.database.model.pkl.KelompokPkl} &mdash; yang memang turunan
	 * {@link VOPembelajaran} &mdash; sehingga kode generik e-learning/pertemuan ({@link Pertemuan},
	 * {@code TampilanELearningAction}, {@code AbsensiHelper}) dapat memperlakukan kelompok PKL sama
	 * seperti kelas perkuliahan biasa.</p>
	 *
	 * <p><b>Catatan:</b> field {@code kelompokPkl} dibaca langsung tanpa {@code check(...)}, jadi
	 * yang dikembalikan bisa berupa proxy yang belum terinisialisasi. Komentar {@code TODO
	 * Auto-generated method stub} yang tertinggal adalah sisa generator IDE, bukan tanda
	 * implementasinya belum selesai.</p>
	 *
	 * @return kelompok PKL yang diikuti mahasiswa ini; {@code null} bila belum terisi
	 * @see VOPesertaPembelajaran#ambilVOPembelajaran()
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return kelompokPkl;
	}

	/**
	 * Ringkasan hasil/laporan kegiatan PKL mahasiswa ini, teks bebas.
	 *
	 * <p>Diisi panitia langsung dari grid anggota di {@code KelompokPklHelper} (kotak teks per
	 * baris) dan ikut tercetak pada sertifikat ({@code SertifikatAction}). Dipetakan ke kolom
	 * {@code text} sehingga panjangnya tidak dibatasi.</p>
	 *
	 * @return teks hasil kegiatan; string kosong (bukan {@code null}) bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getHasil() {
		return hasil == null ? "" : hasil.trim();
	}

	/**
	 * Mengisi ringkasan hasil/laporan kegiatan PKL.
	 *
	 * @param hasil teks hasil kegiatan
	 */
	public void setHasil(String hasil) {
		this.hasil = hasil;
	}
}
