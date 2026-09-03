package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Box;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.psb.nis.DefaultNisGenerator;
import ais.action.master.sekolah.psb.nis.NisGenerator;
import ais.common.Common;
import ais.common.CommonPSB;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.AlatTransportasiMahasiswa;
import ais.database.model.CommonVO;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.Negara;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Pekerjaan;
import ais.database.model.Propinsi;
import ais.database.model.VOSiswa;
import ais.database.model.Wilayah;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.LampiranLain;

/**
 * Identitas <b>pendaftar PPDB/PSB</b> (Penerimaan Siswa Baru) pada jenjang sekolah &mdash;
 * satu baris tabel {@code sekolah.calon_siswa} mewakili satu anak yang mendaftar pada satu
 * {@link GelombangPendaftaranPsb}. Entity ini adalah <b>simpul paling sentral seluruh modul PSB</b>:
 * hampir setiap entity lain di keluarga PSB menyimpan FK ke sini, dan hampir setiap layar,
 * laporan, servlet portal, serta jalur billing PPDB berangkat dari satu objek {@code CalonSiswa}.
 *
 * <p>Alur hidup normalnya: calon mendaftar (baris dibuat oleh {@code PPDB}/{@code CalonSiswaAction}
 * atau formulir portal publik) &rarr; melengkapi biodata + berkas &rarr; diverifikasi panitia &rarr;
 * mengikuti ujian/wawancara &rarr; membayar &rarr; {@link #getTelahDiterima() telahDiterima}
 * menjadi {@code true} &rarr; sistem menerbitkan NIS dan membuat baris {@link Siswa} lewat
 * {@code CommonPSB.onGenerateNis}. Sejak saat itu {@link #getSiswa()} terisi dan entity ini
 * berubah peran menjadi <i>arsip pendaftaran</i> dari siswa yang sudah aktif.</p>
 *
 * <h3>Posisi dalam keluarga entity PSB</h3>
 * <table border="1">
 * <caption>Relasi utama (arah panah = pemilik FK)</caption>
 * <tr><th>Sub-domain</th><th>Entity</th><th>Keterkaitan</th></tr>
 * <tr><td rowspan="4">Pendaftaran</td>
 * <td>{@link GelombangPendaftaranPsb}</td>
 * <td>FK {@code current_gelombang_pendaftaran_psb_id}. <b>Sumber kebenaran</b> untuk
 * {@link #getSekolah() sekolah}, {@link #getYayasan() yayasan},
 * {@link #getTahunMasuk() tahunMasuk}, {@link #getStatusAwalSiswa() statusAwalSiswa}
 * dan {@link #getSekolahAsal() sekolahAsal} &mdash; getter-getter itu MENIMPA nilai
 * lokal dengan nilai gelombang.</td></tr>
 * <tr><td>{@link KelompokPendaftaranPsb}</td><td>FK {@code kelompok_pendaftaran_psb}; pengelompokan
 * administratif pendaftar dalam satu gelombang.</td></tr>
 * <tr><td>{@link PaketPsb}</td><td>FK {@code paket_psb}; paket biaya/program yang dipilih
 * pendaftar.</td></tr>
 * <tr><td>{@link StatusAwalSiswa}</td><td>FK {@code status_awal_siswa}; Baru / Pindahan /
 * dsb.</td></tr>
 * <tr><td rowspan="2">Verifikasi berkas</td>
 * <td>{@link CalonSiswaPunyaVerifikasiBerkas}</td>
 * <td>Baris hasil verifikasi per jenis berkas; lampiran fisiknya (akte, KK, KTP orang tua,
 * rapor) disimpan sebagai {@link ais.database.model.file.LampiranLain} yang di-<i>key</i>
 * dengan id baris itu, bukan id entity ini.</td></tr>
 * <tr><td>{@link VerifikasiKelengkapanCalonSiswa} / {@link ParameterVerifikasiCalonSiswa}</td>
 * <td>Katalog jenis berkas dan parameter penilaian yang harus dipenuhi.</td></tr>
 * <tr><td rowspan="2">Verifikasi nilai</td>
 * <td>{@link CalonSiswaPunyaVerifikasiMatapelajaran}</td>
 * <td>Nilai rapor per mata pelajaran asal (memakai {@code MatapelajaranSekolah}, katalog
 * terpisah dari {@link Matapelajaran}).</td></tr>
 * <tr><td>{@link CalonSiswaPunyaVerifikasiParameter}</td><td>Skor per parameter
 * verifikasi.</td></tr>
 * <tr><td rowspan="3">Ujian &amp; wawancara</td>
 * <td>{@link UjianPSB} / {@link JadwalUjianPSB}</td>
 * <td>Master ujian seleksi dan jadwal sesinya; {@link #getNoUjian() noUjian} pada entity ini
 * adalah nomor peserta yang dicetak di kartu ujian.</td></tr>
 * <tr><td>{@link JadwalPertemuanPSB}</td>
 * <td>FK {@code jadwal_pertemuan_psb}; slot pertemuan tatap muka pendaftar+orang tua.
 * Layar {@code calon_siswa.zul} disisipkan sebagai iframe dari layar jadwal ini &mdash;
 * lihat catatan pewarisan hak di bawah.</td></tr>
 * <tr><td>{@link InterviewCalonSiswa} / {@link InterviewPunyaCalonSiswa}</td>
 * <td>Sesi wawancara dan penugasan pewawancara; dibaca+ditulis oleh portal pra-otentikasi
 * {@code _wawancara_service.jsp}.</td></tr>
 * <tr><td rowspan="3">Billing PPDB</td>
 * <td>{@link JenisBiayaSekolah} (flag {@code gunakanCalonSiswa}) &rarr;
 * {@link PengaturanBiaya} &rarr; {@link NominalBiaya}</td>
 * <td>Rantai konfigurasi tarif; {@link PengaturanBiayaPunyaSiswa} menentukan siapa yang
 * kena.</td></tr>
 * <tr><td>{@link Tagihan}</td><td>FK {@code calon_siswa}; kewajiban rupiah aktual. Dibangkitkan
 * lewat {@link ais.action.master.sekolah.helper.TagihanUtilCalonSiswa}.</td></tr>
 * <tr><td>{@link PembayaranSiswa} / {@link PembayaranSiswaDetail}</td>
 * <td>Kuitansi dan barisnya; dibaca kembali oleh {@link #populate(CalonSiswa)} untuk mengisi
 * enam kolom ringkasan {@code riwayat*} pada entity ini.</td></tr>
 * <tr><td rowspan="3">Lain-lain</td>
 * <td>{@link Siswa}</td>
 * <td>TIGA FK berbeda ke entity yang sama: {@link #getSiswa() siswa} (hasil penerimaan),
 * {@link #getSiswaAlumni() siswaAlumni} (jika pendaftar adalah alumni jenjang sebelumnya
 * &mdash; memasok balik nama/tanggal lahir/NIK/telepon), dan
 * {@link #getSiswaSibling() siswaSibling} (kakak/adik yang sudah bersekolah, untuk
 * diskon saudara).</td></tr>
 * <tr><td>{@link ais.database.model.Pegawai} / {@link ais.database.model.employ.Keluarga}</td>
 * <td>Jalur "anak pegawai": bila gelombang ditandai {@code hanyaUntukAnakPegawai},
 * {@link #getOrangTuaPegawai()} memasok nama/tempat/tanggal lahir ayah atau ibu dari
 * master kepegawaian.</td></tr>
 * <tr><td>{@link KelasLesSiswa} / {@link KelasLesSiswaPunyaSiswa}</td>
 * <td>Kelas les/ekstra yang dipilih saat mendaftar, disimpan sebagai daftar id ber-koma pada
 * {@link #getKelasLesDipilih()}.</td></tr>
 * </table>
 *
 * <h3>Kelompok anggota</h3>
 * <ol>
 * <li><b>Identitas baris &amp; jejak audit</b> &mdash; {@link #getId()}, {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 * {@link #toString()}.</li>
 * <li><b>Nomor identitas pendaftaran</b> &mdash; {@link #getNoRegistrasi()},
 * {@link #getNomorInduk()}, {@link #getNopendaftaran()}, {@link #getNoUjian()},
 * {@link #getKodeUniq()}, {@link #getNis()}.</li>
 * <li><b>Biodata anak</b> &mdash; nama, panggilan, jenis kelamin, tempat/tanggal lahir, agama,
 * kewarganegaraan, negara, bahasa, hobi, anak ke berapa.</li>
 * <li><b>Identitas kependudukan</b> &mdash; {@link #getNik()}, {@link #getKk()},
 * {@link #getNoAktaKelahiran()}, {@link #getNomorIndukNasional()} (NISN),
 * {@link #getNomorSeriIjazah()}, {@link #getNomorSeriSkhun()},
 * {@link #getNomorUjianNasional()}, {@link #getNoKip()}.</li>
 * <li><b>Alamat &amp; wilayah</b> &mdash; alamat siswa/ayah/ibu/wali, dusun, RT/RW, kode pos,
 * kelurahan, {@link #getKecamatanCalon()}, {@link #getKotaCalon()},
 * {@link #getPropinsiCalon()}, dan {@link #getKoordinat()} (titik GPS rumah).</li>
 * <li><b>Keluarga</b> &mdash; nama/NIK/pendidikan/pekerjaan/penghasilan/tempat+tanggal lahir
 * ayah, ibu, dan wali; jumlah saudara kandung dan tiri; status dalam keluarga.</li>
 * <li><b>Kontak</b> &mdash; {@link #getTeleponSiswa()}, {@link #getTeleponOrangTua()},
 * {@link #getTeleponWali()}, {@code hp1..hp3} ayah/ibu/wali, WhatsApp ayah/ibu/wali,
 * {@link #getAlamatEmail()}. {@link #ambilTelp()} mengumpulkan seluruhnya menjadi satu
 * himpunan.</li>
 * <li><b>Kesehatan &amp; kondisi khusus</b> &mdash; {@link #getBerat()}, {@link #getTinggi()},
 * {@link #getGolonganDarah()}, {@link #getRiwayatPenyakit()},
 * {@link #getKebutuhanKhusus()}, {@link #getKondisiSiswa()},
 * {@link #getPenerimaBantuan()}, {@link #getLayakPip()}.</li>
 * <li><b>Sekolah asal &amp; kepindahan</b> &mdash; {@link #getSekolahAsal()} dan wilayahnya,
 * {@link #getTahunLulus()}, {@link #getNoIjazah()}, serta blok
 * {@link #getMerupakanPindahan()} + tanggal/keterangan/asal/kelas pindahan.</li>
 * <li><b>Status seleksi</b> &mdash; {@link #getTelahDiterima()}, {@link #getTerverifikasi()},
 * {@link #getDitolak()}, {@link #getMengundurkanDiri()}, {@link #getPernyataan()},
 * {@link #getCetakKartu()}, {@link #getTelahLogin()}, {@link #getWaktuLogin()}.</li>
 * <li><b>Kredensial portal</b> &mdash; {@link #getPass()}, {@link #getIs_encripted()},
 * {@link #getPinPassword()}, {@link #getUserOrtu()}, {@link #getPassOrtu()},
 * {@link #urlLogin()}.</li>
 * <li><b>Parameter tambahan dinamis</b> &mdash; {@link #getParameterTambahan()},
 * {@link #getParameterTambahanInds()}, {@link #ambilDataParameterTambahan()},
 * {@link #populateParameterTambahan(java.util.List)}, {@link #ambilSkor(ParameterTambahan)},
 * {@link #getFormulaPrestasi()}, {@link #getFieldsGeneric()}.</li>
 * <li><b>Ringkasan billing (denormalisasi)</b> &mdash; enam kolom {@code riwayat*} plus mesin
 * pengisinya {@link #populatePembayaran()} / {@link #populate(CalonSiswa)}, dan
 * {@link #munculkanFormPembayaran(org.zkoss.zk.ui.event.EventListener)}.</li>
 * <li><b>Bantu UI/laporan</b> &mdash; {@link #tampilkanEmail(org.zkoss.zk.ui.Component)},
 * {@link #tampilkanHp(org.zkoss.zk.ui.Component)}, {@link #putPhoto(java.util.Map)},
 * {@link #ambilHp()}, {@link #kebutuhanKhusus(org.zkoss.zul.Box)}.</li>
 * </ol>
 *
 * <h3>Catatan teknis yang tidak terlihat dari kode</h3>
 * <ul>
 * <li><b>Pengulangan {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN bug.</b>
 * Kelas ini turun dari {@link VOSiswa} yang turun dari
 * {@link ais.database.model.GeneralValueObject}. Keduanya POJO abstrak biasa &mdash; BUKAN
 * {@code @Entity} dan BUKAN {@code @MappedSuperclass} &mdash; sehingga Hibernate tidak
 * memetakan satu pun properti milik induk. Setiap entity turunan WAJIB mendeklarasikan ulang
 * keempat properti itu supaya benar-benar tersimpan. Lihat
 * {@link ais.database.model.GeneralValueObject} untuk uraian lengkapnya.</li>
 * <li><b>Akses properti (property access), bukan field access.</b> Anotasi dipasang pada getter,
 * jadi Hibernate MEMANGGIL getter saat membaca maupun saat menghitung <i>dirty state</i>.
 * Akibatnya setiap getter yang menulis ke field-nya sendiri akan ikut ter-<i>flush</i> ke
 * database: sekadar MEMBACA baris dapat MENGUBAHnya. Daftar lengkapnya di bawah.</li>
 * <li><b>Empat properti menumpang kolom yang sama.</b> {@link #getNoRegistrasi()} adalah
 * satu-satunya yang boleh menulis ke kolom {@code nomor_induk}; {@link #getNomorInduk()} dan
 * {@link #getNopendaftaran()} memetakan kolom yang sama dengan
 * {@code insertable=false, updatable=false} (alias baca-saja untuk laporan/pencarian).
 * Serupa: {@link #getNim()} adalah alias baca-saja kolom {@code nomor_induk_nasional} yang
 * ditulis lewat {@link #getNomorIndukNasional()}, dan {@link #getNama()} alias baca-saja
 * kolom {@code nama_siswa} yang ditulis lewat {@link #getNamaSiswa()}. Menyetel properti
 * alias tidak akan pernah tersimpan &mdash; kesalahan yang mudah terjadi pada unggahan Excel.</li>
 * <li><b>Sebagian besar properti tidak beranotasi {@code @Column}.</b> Untuk properti tersebut
 * nama kolom sama persis dengan nama properti (mis. {@code riwayatPembayaran}), yang oleh
 * PostgreSQL dilipat menjadi huruf kecil ({@code riwayatpembayaran}). Ini konsisten dengan
 * temuan kolom camelCase di {@link PembayaranSiswa}.</li>
 * <li><b>{@code @Audited} (Envers).</b> Perubahan terekam di tabel audit
 * {@code new_audit.calon_siswa__audit}. Tetapi {@link #populate(CalonSiswa)} menulis lewat
 * <i>bulk HQL update</i> dan beberapa layar memakai SQL native &mdash; jalur-jalur itu
 * MELEWATI Envers, sehingga riwayat audit tidak lengkap untuk kolom {@code riwayat*},
 * {@code telahDiterima}, dan operasi massal.</li>
 * <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif.</b> Hibernate hanya menulis kolom
 * yang berubah; ini yang membuat efek samping getter tulis-balik bisa lolos tanpa disadari
 * (yang ter-UPDATE hanya satu-dua kolom, tidak tampak sebagai "penyimpanan").</li>
 * </ul>
 *
 * <h3>Getter yang MENULIS BALIK ke field (efek samping saat membaca)</h3>
 * <p>Semuanya berjalan pada objek terkelola, jadi nilainya ikut tersimpan pada flush berikutnya:
 * {@link #getNama()}, {@link #getNomorInduk()}, {@link #getSekolah()}, {@link #getYayasan()},
 * {@link #getAlamatEmail()}, {@link #getJenisKelamin()}, {@link #getNamaAyah()},
 * {@link #getNamaIbu()}, {@link #getNamaSiswa()}, {@link #getSekolahAsal()},
 * {@link #getAlamatSekolahAsal()}, {@link #getStatusAwalSiswa()}, {@link #getTahunMasuk()},
 * {@link #getTanggalLahir()}, {@link #getTeleponOrangTua()}, {@link #getTeleponSiswa()},
 * {@link #getTeleponWali()}, {@link #getTempatLahir()}, {@link #getHp1ayah()},
 * {@link #getHp1ibu()}, {@link #getTanggalLahirAyah()}, {@link #getTanggalLahirIbu()},
 * {@link #getTempatLahirAyah()}, {@link #getTempatLahirIbu()}, {@link #getPass()},
 * {@link #getIs_encripted()}, {@link #getTelahDiterima()}, {@link #getTelahLogin()},
 * {@link #getSiswa()}, {@link #getKecamatanCalon()}, {@link #getKecamatanSekolahAsal()},
 * {@link #getPropinsiSekolahAsal()}, {@link #getPenjurusanSekolah()}, {@link #getNik()},
 * {@link #getWaAyah()}, {@link #getWaIbu()}, {@link #getKebutuhanKhusus()},
 * {@link #getInfoKampusDariMana()}, {@link #getKelasLesDipilih()}, {@link #getOrangTuaPegawai()},
 * enam getter {@code riwayat*}, dan lima getter blok pindahan.</p>
 *
 * <p><b>Yang benar-benar DESTRUKTIF</b> (menghapus/menimpa data yang tidak dapat dipulihkan lewat
 * layar):</p>
 * <ul>
 * <li>{@link #getPass()} &mdash; bila kolom {@code pass} kosong dan NISN terisi, getter MENCETAK
 * kata sandi portal berisi NISN terenkripsi DES lalu menyalakan {@code is_encripted}.
 * Sekadar merender daftar calon siswa menerbitkan kredensial yang tidak pernah diminta siapa
 * pun, dan kata sandinya dapat ditebak siapa saja yang tahu NISN anak.</li>
 * <li>{@link #getOrangTuaPegawai()} &mdash; bila gelombang TIDAK ditandai
 * {@code hanyaUntukAnakPegawai} (atau pemeriksaannya melempar exception), relasi ke pegawai
 * di-{@code null}-kan. Mengubah satu centang di master gelombang karena itu MEMUTUS PERMANEN
 * tautan anak-pegawai pada seluruh pendaftar gelombang tersebut begitu barisnya tersentuh.</li>
 * <li>{@link #getTanggalPindah()}, {@link #getKeteranganPindah()},
 * {@link #getPindahanDariSekolah()}, {@link #getAlamatSekolahPindahan()},
 * {@link #getKelasSekolahPindahan()} &mdash; kelimanya MENGOSONGKAN diri begitu
 * {@link #getMerupakanPindahan()} bernilai {@code false}. Melepas centang "pindahan" lalu
 * menyimpan akan MENGHAPUS seluruh riwayat kepindahan; mencentangnya lagi tidak memulihkan
 * apa pun.</li>
 * <li>{@link #getTelahDiterima()} &mdash; menuliskan {@code true} secara otomatis begitu ada
 * riwayat pembayaran pendaftaran dan gelombang memakai
 * {@code otomatisDiterimaKetikaSudahBayarReg}. Keputusan kelulusan karena itu bisa berubah
 * hanya karena sebuah laporan membaca entity ini.</li>
 * <li>{@link #getStatusAwalSiswa()} dan {@link #getSekolahAsal()}/{@link #getAlamatSekolahAsal()}
 * &mdash; menimpa nilai yang mungkin sudah dikoreksi manual dengan nilai dari master
 * gelombang, tanpa layar peninjauan.</li>
 * <li>{@link #getSiswa()} &mdash; bila calon sudah "diterima" tetapi belum bertaut, getter
 * MENEBAK pasangan {@link Siswa} dari cache global berdasar (tahun masuk + nama +
 * tanggal lahir). Dua anak bernama sama dan lahir di hari sama pada angkatan yang sama akan
 * TERTAUT SILANG, dan tautan itu ikut tersimpan.</li>
 * <li>{@link #getCetakKartu()} &mdash; mengembalikan {@code 1} untuk nilai apa pun yang bukan
 * {@code null} (termasuk {@code 0}); pencacah "berapa kali kartu dicetak" karenanya tidak
 * pernah bisa lebih dari 1. Lihat catatan pada method itu.</li>
 * </ul>
 *
 * <h3>PERINGATAN KEAMANAN &mdash; entity ini terjangkau TANPA LOGIN</h3>
 * <p><b>1. Kunci primer entity ini adalah sumber enumerasi pra-otentikasi (TERVERIFIKASI).</b>
 * {@link #getId()} dianotasi {@code @Id @GeneratedValue(strategy = IDENTITY)} &mdash; pada
 * PostgreSQL berarti kolom {@code serial}/{@code identity} yang <b>berurutan dan rapat</b>
 * (1, 2, 3, &hellip;), bukan UUID atau nilai acak. Berkas
 * {@code /WEB-INF/baru/modul/ppdb/_sukses_login.jsp} membaca parameter URL {@code id} lalu
 * memanggil {@code GeneralValueObject.ambilData(CalonSiswa.class, id, true)} &mdash; pencarian
 * by-primary-key lewat cache entity global, tanpa penyaring tenant dan tanpa cek kepemilikan.
 * Pengguna sesi ({@code Common.getCurrentUser(request)}) hanya dipakai sebagai <i>fallback</i>
 * ketika parameter {@code id} TIDAK diberikan; bila {@code id} ada, ia MENANG atas sesi &mdash;
 * termasuk ketika tidak ada sesi sama sekali. Karena
 * {@code applicationContext-security.xml} baris&nbsp;62 memasang aturan tangkap-semua
 * {@code <intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>}, dan servlet
 * {@link ais.action.servlet.Ppdb} meneruskan {@code hanya_tampil_jsp=true} ke dispatcher
 * {@code /WEB-INF/baru/ppdb.jsp} yang meng-{@code include} berkas modul apa pun yang diminta,
 * maka:</p>
 * <pre>
 * /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=_sukses_login&amp;id=&lt;1,2,3,&hellip;&gt;
 * </pre>
 * <p>mengembalikan &mdash; tanpa autentikasi apa pun &mdash; halaman berisi nama lengkap, tempat
 * dan tanggal lahir anak, nama ayah dan ibu, nomor telepon orang tua, sekolah, penjurusan, nomor
 * registrasi, nomor ujian, jadwal pertemuan, status kelulusan, seluruh jawaban parameter tambahan,
 * serta <b>tautan unduh berkas verifikasi</b> ({@link CalonSiswaPunyaVerifikasiBerkas} +
 * {@link ais.database.model.file.LampiranLain#createLinkUri()}) &mdash; akte kelahiran, kartu
 * keluarga, KTP orang tua. Halaman itu bahkan mencetak {@code ID: <id>} secara harfiah, sehingga
 * pemetaan id&nbsp;&rarr;&nbsp;anak dapat dipanen dengan satu loop. Ini adalah temuan IDOR yang
 * sama dengan yang tercatat pada {@link CalonSiswaPunyaVerifikasiBerkas}; entity ini adalah
 * SUMBER field {@code id} yang dibocorkan.</p>
 *
 * <p><b>2. Endpoint pra-otentikasi lain pada dispatcher yang sama</b>, semuanya memakai pola
 * {@code &amp;id=<id entity ini>}: {@code _wawancara_service} (baca+tulis jadwal wawancara,
 * nama ruang video conference &mdash; lihat {@link InterviewCalonSiswa}),
 * {@code _ikut_ujian_online_service}, {@code _cetak_kartu_ujian}, dan
 * {@code _cetak_kartu_pendaftaran}.</p>
 *
 * <p><b>3. {@code _cetak_kartu_pendaftaran} bukan sekadar pembacaan &mdash; ia MENULIS.</b>
 * Berkas {@code /WEB-INF/baru/modul/ppdb/_cetak_kartu_pendaftaran.jsp} membaca {@code id},
 * memuat entity ini tanpa cek apa pun, lalu (a) memanggil
 * {@code TagihanUtilCalonSiswa.getTagihan(&hellip;, true)} yang MEMBANGKITKAN baris
 * {@link Tagihan}, (b) memanggil {@code BriCommon.onSaveBri(&hellip;)} yang MENERBITKAN permintaan
 * virtual account BRI sungguhan, (c) merakit PDF gabungan berisi kartu bayar + info VA +
 * biodata lengkap dan mengembalikan tautannya, dan (d) bila diberi {@code &amp;kirimEmail=true},
 * MENGIRIM PDF itu lewat email. Artinya pihak anonim dapat membangkitkan tagihan dan nomor VA
 * untuk anak mana pun, mengunduh biodata lengkapnya, dan memicu pengiriman email berulang.</p>
 *
 * <p><b>4. Login portal calon siswa sangat lemah, dan dapat dilewati dengan wildcard.</b>
 * {@code ais.action.master.psb.LoginCalonSiswaAction} mengotentikasi calon siswa hanya dengan
 * (nomor pendaftaran ATAU nomor ujian ATAU <i>nama</i>) + tanggal lahir, dengan PIN yang hanya
 * aktif bila dikonfigurasi. Ketiga pembandingnya memakai
 * {@code Restrictions.ilike(&hellip;, MatchMode.EXACT)} &mdash; yang tetap menghasilkan SQL
 * {@code LIKE} dan TIDAK meng-escape metakarakter. Nilai {@code %} pada kolom nama maupun PIN
 * karena itu cocok dengan APA PUN; dipadu {@code setMaxResults(1).addOrder(Order.desc("id"))},
 * satu tanggal lahir sembarang sudah cukup untuk masuk sebagai pendaftar terbaru dengan tanggal
 * lahir tersebut. <b>Ini bypass otentikasi, kategori berbeda dari IDOR.</b></p>
 *
 * <p><b>5. {@link #urlLogin()} menghasilkan tautan login tanpa kata sandi yang dapat dipalsukan.</b>
 * Isinya {@code /m?q=DES("<id>-CalonSiswa-abcdefghijklmnopqrstuvwxyz")}. Servlet
 * {@code ais.action.servlet.MServet} mendekripsi, mengambil id, dan langsung memanggil
 * {@code Common.setLogin(request, calonSiswa)} &mdash; tanpa masa berlaku, tanpa nonce, tanpa
 * pemakaian sekali habis. Plaintext-nya deterministik sepenuhnya dari id yang berurutan, dan
 * satu-satunya rahasia adalah passphrase DES {@code Common.DES_PASS_PHRASE} yang
 * <b>tertanam di kode sumber dan sama untuk seluruh instalasi AIS</b>. Siapa pun yang pernah
 * membaca kode ini dapat mencetak tautan login untuk pendaftar mana pun di instalasi mana pun.</p>
 *
 * <p><b>6. Fail-open cakupan tenant pada layar master.</b>
 * {@code CalonSiswaAction.initCriteria(boolean)} TIDAK PERNAH memasang pembatas sekolah/yayasan
 * wajib. Penyaring sekolah dan yayasan sepenuhnya berasal dari combobox pencarian, dan ketika
 * combobox itu berada pada pilihan bawaan "Semua" klausanya menjadi
 * {@code Restrictions.sqlRestriction("1=1")} &mdash; grid menampilkan pendaftar SELURUH sekolah
 * dan yayasan dalam satu instalasi. Cabang {@code jadwalPertemuanPSBData} (dipakai saat layar ini
 * disisipkan sebagai iframe) bahkan tidak punya pembatas tenant sama sekali. Pola fail-open yang
 * sama muncul di {@code doAfterCompose} ({@code sekolah != null ? eq("sekolah", sekolah) :
 * sqlRestriction("true")}).</p>
 *
 * <p><b>7. Gerbang hak akses nyaris tidak ada pada layar master.</b> Dalam 5.949 baris
 * {@code CalonSiswaAction} hanya ada TIGA pemanggilan {@code CommonPrivilages.checkPrevilages}
 * (CREATE untuk tombol tambah, UPDATE dan DELETE untuk dua boolean). Tombol-tombol berdampak
 * besar tidak digerbangi hak sama sekali, hanya oleh <i>konfigurasi global</i> atau tidak sama
 * sekali:
 * <ul>
 * <li>"Lampiran" ({@code onDownloadLampiran}) &mdash; TANPA gerbang apa pun; mengekspor foto dan
 * seluruh lampiran identitas SETIAP pendaftar yang lolos filter (yang bawaannya "Semua
 * sekolah") ke {@code /opt/ecampus/lampiran_<timestamp>/}. Pola yang sama dengan temuan
 * ekspor massal rapor pada {@link CalonSiswaPunyaVerifikasiMatapelajaran}.</li>
 * <li>"Upload Gen. NIS" &mdash; hanya digerbangi konfigurasi
 * {@code aktifkan_tombol_upload_data_calon_siswa}; menjalankan
 * {@code CommonPSB.uploadKelulusan} (kelulusan + penerbitan NIS massal dari Excel).
 * Berkasnya ditulis ke {@code getRealPath("/temp/" + media.getName())} &mdash; DI DALAM
 * webapp, dengan nama berkas dari klien.</li>
 * <li>"Semua diterima" &mdash; hanya digerbangi konfigurasi
 * {@code tampilkan_tombol_semua_diterima}; menandai {@code telahDiterima = true} untuk
 * SELURUH baris hasil {@code initCriteria} (yang fail-open lintas sekolah).</li>
 * <li>"Download Gen. NIS" &mdash; TANPA gerbang; mengekspor {@code id}, nomor registrasi, nomor
 * ujian, NIS, nama, sekolah, status diterima seluruh pendaftar ke Excel &mdash; persis
 * pemetaan id&nbsp;&rarr;&nbsp;anak yang dibutuhkan untuk menyalahgunakan temuan nomor 1.</li>
 * </ul>
 * Sebagai pembanding, tombol "Upload Data" generik JUSTRU digerbangi dengan benar
 * ({@code add.isVisible() && edit && delete &&
 * Common.bolehUploadDataKonfigurasi("hak_akses_upload_data_calon_siswa")}) &mdash; jangan
 * merusak gerbang yang sudah benar itu saat memperbaiki yang lain.</p>
 *
 * <p><b>8. Pewarisan hak lewat menu induk (dua arah).</b> Menu bawaan yang memasang layar ini
 * adalah "Calon Siswa" ({@code MenuInitializer} id 18793 &rarr;
 * {@code /pages/master/sekolah/calon_siswa.zul}). Tetapi
 * {@code JadwalPertemuanPSBAction} juga menyisipkan {@code calon_siswa.zul?jadwalPertemuanPSB=<id>}
 * sebagai {@code MyInclude} di dalam layar "Jadwal Pertemuan PSB". Karena gerbang ZK dipasang di
 * {@code doAfterCompose} berdasarkan menu yang sedang aktif, hak atas menu bernilai rendah itu
 * mewariskan akses ke SELURUH biodata pendaftar (PII anak) pada layar ini.</p>
 *
 * <h3>Data pribadi sensitif yang tersimpan LANGSUNG pada entity ini</h3>
 * <p>(bukan lewat {@link ais.database.model.file.LampiranLain}, jadi ikut terbawa pada setiap
 * ekspor Excel, cache entity global, dan setiap kebocoran di atas):</p>
 * <ul>
 * <li><b>Identitas kependudukan anak:</b> NIK ({@link #getNik()}), nomor Kartu Keluarga
 * ({@link #getKk()}), nomor akta kelahiran ({@link #getNoAktaKelahiran()}), NISN, nomor seri
 * ijazah dan SKHUN, nomor peserta Ujian Nasional, nomor KIP.</li>
 * <li><b>Identitas kependudukan orang tua/wali:</b> {@link #getNikAyah()},
 * {@link #getNikIbu()}, {@link #getNikWali()}.</li>
 * <li><b>Data kesehatan anak:</b> golongan darah, berat dan tinggi badan, riwayat penyakit
 * (kolom teks 2000 karakter), kebutuhan khusus/disabilitas, kondisi siswa.</li>
 * <li><b>Geolokasi rumah:</b> {@link #getKoordinat()} &mdash; titik GPS tempat tinggal anak
 * di bawah umur, kolom {@code text}.</li>
 * <li><b>Data ekonomi keluarga:</b> penghasilan ayah/ibu/wali (relasi dan versi teks bebas),
 * pekerjaan, pendidikan, status penerima bantuan/PIP.</li>
 * <li><b>Kontak:</b> hingga dua belas nomor telepon/WhatsApp keluarga plus alamat email.</li>
 * <li><b>Kredensial:</b> {@link #getPass()} (DES reversibel, berisi NISN),
 * {@link #getPinPassword()} dan {@link #getPassOrtu()} (disimpan apa adanya).</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see VOSiswa
 * @see GelombangPendaftaranPsb
 * @see Siswa
 * @see CalonSiswaPunyaVerifikasiBerkas
 * @see InterviewCalonSiswa
 * @see JadwalPertemuanPSB
 * @see Tagihan
 * @see ais.action.master.sekolah.CalonSiswaAction
 * @see ais.action.master.sekolah.helper.TagihanUtilCalonSiswa
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "calon_siswa", schema = "sekolah")
public class CalonSiswa extends VOSiswa {

	/**
	 * Nilai baku untuk {@link #getStatusSekolah()} bila sekolah asal berstatus negeri.
	 * Konstanta ini hanya menyediakan ejaan yang seragam &mdash; tidak ada validasi yang
	 * memaksa kolomnya berisi salah satu dari dua konstanta ini.
	 *
	 * @see #SWASTA
	 */
	public static final String NEGERI = "Negeri";
	/**
	 * Nilai baku untuk {@link #getStatusSekolah()} bila sekolah asal berstatus swasta.
	 *
	 * @see #NEGERI
	 */
	public static final String SWASTA = "Swasta";

	/**
	 * Versi serialisasi Java. Nilainya dibekukan sejak entity dibangkitkan
	 * {@code hbm2java}; JANGAN diubah karena objek {@code CalonSiswa} ikut
	 * diserialisasi ke sesi ZK dan ke cache antar-node.
	 */
	private static final long serialVersionUID = 8583487061204307799L;
	/**
	 * Kunci primer baris pendaftaran. Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Setter menolak nilai kosong secara diam-diam, sehingga jejak lama tidak pernah terhapus oleh
	 * proses yang tidak tahu identitas penggunanya.</p>
	 *
	 * @return Id pengguna terakhir yang mengubah baris ini
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Nilai {@code null} atau kosong DIABAIKAN secara diam-diam &mdash; jejak audit lama tidak pernah
	 * terhapus oleh proses latar yang tidak mengetahui identitas penggunanya.</p>
	 *
	 * @param olehId id pengguna terakhir yang mengubah baris ini
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Nilai {@code null} atau kosong DIABAIKAN secara diam-diam &mdash; jejak audit lama tidak pernah
	 * terhapus oleh proses latar yang tidak mengetahui identitas penggunanya.</p>
	 *
	 * @param oleh nama pengguna terakhir yang mengubah baris ini
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Setter menolak nilai kosong secara diam-diam, sehingga jejak lama tidak pernah terhapus oleh
	 * proses yang tidak tahu identitas penggunanya.</p>
	 *
	 * @return Nama pengguna terakhir yang mengubah baris ini
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(java.util.Date)} dari konteks pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Efek samping &amp; batasan:</b> hook ini HANYA berjalan pada jalur ORM biasa. Perubahan
	 * lewat HQL/SQL massal &mdash; termasuk {@link #populate(CalonSiswa)} yang memakai
	 * {@code update CalonSiswa set &hellip;} &mdash; TIDAK memicunya, sehingga kolom jejak audit
	 * tidak diperbarui untuk perubahan-perubahan itu.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir baris ini
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), disimpan sebagai stempel waktu lengkap.</p>
	 *
	 * <p>Diinisialisasi ke waktu saat objek dibuat, lalu diperbarui oleh {@link #onUpdate()} pada setiap
	 * {@code UPDATE}.</p>
	 *
	 * @return Stempel waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Alias baca-saja nama lengkap calon siswa. Lihat {@link #getNama()}.
	 */
	private String nama;

	/**
	 * Alias baca-saja nama lengkap calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code nama_siswa} yang SAMA dengan {@link #getNamaSiswa()}, tetapi
	 * dengan {@code insertable = false, updatable = false} &mdash; menyetel properti ini lewat
	 * {@link #setNama(String)} TIDAK PERNAH tersimpan. Kolom yang benar untuk menulis adalah
	 * {@link #setNamaSiswa(String)}.</p>
	 *
	 * <p>Properti ini ada karena {@link VOSiswa} dan sejumlah komponen bersama (combobox pencarian,
	 * laporan lintas-entity, {@code LoginCalonSiswaAction} yang mencocokkan {@code "nama"})
	 * mengharapkan nama properti {@code nama} pada baik {@link Siswa} maupun {@code CalonSiswa}.</p>
	 *
	 * <p><b>Getter tulis-balik:</b> mengisi field {@code nama} dari {@link #getNamaSiswa()} pada
	 * setiap pemanggilan, jadi ia juga memicu seluruh efek samping getter tersebut.</p>
	 *
	 * @return nama lengkap calon siswa
	 */
	@Column(name = "nama_siswa", nullable = false, insertable = false, updatable = false)
	public String getNama() {
		nama = getNamaSiswa();
		return nama;
	}

	/**
	 * Menetapkan alias baca-saja nama lengkap calon siswa.
	 *
	 * @param nama alias baca-saja nama lengkap calon siswa
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Alias baca-saja NISN (nomor induk siswa nasional). Lihat {@link #getNim()}.
	 */
	private String nim;

	/**
	 * Alias baca-saja NISN, dipakai oleh kode bersama yang mengharapkan properti bernama
	 * {@code nim}.
	 *
	 * <p>Dipetakan ke kolom {@code nomor_induk_nasional} yang SAMA dengan
	 * {@link #getNomorIndukNasional()}, dengan {@code insertable = false, updatable = false} &mdash;
	 * {@link #setNim(String)} tidak pernah tersimpan. Berbeda dari {@link #getNama()}, getter ini
	 * TIDAK menyalin nilai dari properti kembarannya, sehingga {@code getNim()} mengembalikan
	 * {@code null} kecuali Hibernate sendiri yang mengisi field saat memuat baris.</p>
	 *
	 * @return NISN sebagaimana dimuat Hibernate, atau {@code null}
	 */
	@Column(name = "nomor_induk_nasional", nullable = false, insertable = false, updatable = false)
	public String getNim() {
		return nim;
	}

	/**
	 * Menetapkan alias baca-saja NISN (nomor induk siswa nasional).
	 *
	 * @param nim alias baca-saja NISN (nomor induk siswa nasional)
	 */
	public void setNim(String nim) {
		this.nim = nim;
	}

	/**
	 * Gelombang pendaftaran PSB tempat calon ini mendaftar. Lihat {@link #getGelombangPendaftaranPsb()}.
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;
	/**
	 * Kelompok pendaftaran (pengelompokan administratif dalam satu gelombang). Lihat {@link
	 * #getKelompokPendaftaranPsb()}.
	 */
	private KelompokPendaftaranPsb kelompokPendaftaranPsb;
	/**
	 * Tanggal calon siswa mendaftar. Lihat {@link #getTanggalPendaftaran()}.
	 */
	private Date tanggalPendaftaran;
	/**
	 * Sekolah tujuan pendaftaran. Lihat {@link #getSekolah()}.
	 */
	private Sekolah sekolah;
	/**
	 * Penjurusan/program keahlian yang dipilih. Lihat {@link #getPenjurusanSekolah()}.
	 */
	private PenjurusanSekolah penjurusanSekolah;
	/**
	 * Kelas yang dipilih calon siswa saat mendaftar. Lihat {@link #getKelasSiswa()}.
	 */
	private KelasSiswa kelasSiswa;
	/**
	 * Paket PSB (paket biaya/program) yang diambil calon siswa. Lihat {@link #getPaketPsb()}.
	 */
	private PaketPsb paketPsb;
	/**
	 * Agama calon siswa. Lihat {@link #getAgama()}.
	 */
	private Agama agama;
	/**
	 * Alamat email calon siswa. Lihat {@link #getAlamatEmail()}.
	 */
	private String alamatEmail;
	/**
	 * PIN / kata sandi portal PSB yang dibagikan panitia. Lihat {@link #getPinPassword()}.
	 */
	private String pinPassword;
	/**
	 * Alamat tempat tinggal orang tua. Lihat {@link #getAlamatOrangTua()}.
	 */
	private String alamatOrangTua;
	/**
	 * Alamat tempat tinggal ayah. Lihat {@link #getAlamatAyah()}.
	 */
	private String alamatAyah;
	/**
	 * Alamat tempat tinggal ibu. Lihat {@link #getAlamatIbu()}.
	 */
	private String alamatIbu;
//	private String prestasiSiswa1;
//	private String prestasiSiswa2;
//	private String prestasiSiswa3;
	/**
	 * Alamat tempat tinggal calon siswa. Lihat {@link #getAlamatSiswa()}.
	 */
	private String alamatSiswa;
	/**
	 * Nama dusun pada alamat calon siswa. Lihat {@link #getDusunCalon()}.
	 */
	private String dusunCalon;
	/**
	 * Nomor RT pada alamat calon siswa. Lihat {@link #getRt()}.
	 */
	private String rt;
	/**
	 * Nomor RW pada alamat calon siswa. Lihat {@link #getRw()}.
	 */
	private String rw;
	/**
	 * Kode pos alamat calon siswa. Lihat {@link #getKodePos()}.
	 */
	private String kodePos;
	/**
	 * Nama desa/kelurahan alamat calon siswa. Lihat {@link #getKelurahanCalon()}.
	 */
	private String kelurahanCalon;
	/**
	 * Kecamatan alamat calon siswa. Lihat {@link #getKecamatanCalon()}.
	 */
	private Wilayah kecamatanCalon;
	/**
	 * Propinsi alamat calon siswa. Lihat {@link #getPropinsiCalon()}.
	 */
	private Propinsi propinsiCalon;
	/**
	 * Kabupaten/kota alamat calon siswa. Lihat {@link #getKotaCalon()}.
	 */
	private Kota kotaCalon;
	/**
	 * Alias baca-saja nomor pendaftaran. Lihat {@link #getNopendaftaran()}.
	 */
	private String nopendaftaran;
	/**
	 * Alamat tempat tinggal wali. Lihat {@link #getAlamatWali()}.
	 */
	private String alamatWali;
	/**
	 * Urutan calon siswa di antara saudara kandungnya. Lihat {@link #getAnakKe()}.
	 */
	private Integer anakKe;
	/**
	 * Jumlah bersaudara dalam keluarga. Lihat {@link #getDariAnakKe()}.
	 */
	private Integer dariAnakKe;
	/**
	 * Jenis kelamin calon siswa. Lihat {@link #getJenisKelamin()}.
	 */
	private String jenisKelamin;
	/**
	 * NIK (Nomor Induk Kependudukan) calon siswa. Lihat {@link #getNik()}.
	 */
	private String nik;
	/**
	 * NIK ayah. Lihat {@link #getNikAyah()}.
	 */
	private String nikAyah;
	/**
	 * NIK ibu. Lihat {@link #getNikIbu()}.
	 */
	private String nikIbu;
	/**
	 * NIK wali. Lihat {@link #getNikWali()}.
	 */
	private String nikWali;
	/**
	 * Nomor Kartu Keluarga. Lihat {@link #getKk()}.
	 */
	private String kk;
	/**
	 * Koordinat GPS tempat tinggal calon siswa. Lihat {@link #getKoordinat()}.
	 */
	private String koordinat;
	/**
	 * NIS (nomor induk siswa lokal) yang diterbitkan setelah calon diterima. Lihat {@link #getNis()}.
	 */
	private String nis;
	/**
	 * Nama ayah. Lihat {@link #getNamaAyah()}.
	 */
	private String namaAyah;
	/**
	 * Nama ibu. Lihat {@link #getNamaIbu()}.
	 */
	private String namaIbu;
	/**
	 * Nama lengkap calon siswa. Lihat {@link #getNamaSiswa()}.
	 */
	private String namaSiswa;
	/**
	 * Nama wali. Lihat {@link #getNamaWali()}.
	 */
	private String namaWali;
	/**
	 * Alias baca-saja nomor registrasi pendaftaran. Lihat {@link #getNomorInduk()}.
	 */
	private String nomorInduk;
	/**
	 * Nomor seri ijazah sekolah asal. Lihat {@link #getNomorSeriIjazah()}.
	 */
	private String nomorSeriIjazah;
	/**
	 * NPSN (nomor pokok sekolah nasional) sekolah asal. Lihat {@link #getNomorPokokSekolahNasional()}.
	 */
	private String nomorPokokSekolahNasional;
	/**
	 * Nomor seri SKHUN sekolah asal. Lihat {@link #getNomorSeriSkhun()}.
	 */
	private String nomorSeriSkhun;
	/**
	 * Nomor peserta Ujian Nasional di sekolah asal. Lihat {@link #getNomorUjianNasional()}.
	 */
	private String nomorUjianNasional;
	/**
	 * Nomor registrasi pendaftaran. Lihat {@link #getNoRegistrasi()}.
	 */
	private String noRegistrasi;
	/**
	 * Nomor akta kelahiran calon siswa. Lihat {@link #getNoAktaKelahiran()}.
	 */
	private String noAktaKelahiran;
	/**
	 * Tanggal acuan pencetakan dokumen pendaftaran. Lihat {@link #getPadaTanggal()}.
	 */
	private Date padaTanggal;
	/**
	 * Pekerjaan ayah. Lihat {@link #getPekerjaanAyah()}.
	 */
	private Pekerjaan pekerjaanAyah;
	/**
	 * Pekerjaan ibu. Lihat {@link #getPekerjaanIbu()}.
	 */
	private Pekerjaan pekerjaanIbu;
	/**
	 * Pekerjaan wali. Lihat {@link #getPekerjaanWali()}.
	 */
	private Pekerjaan pekerjaanWali;
	/**
	 * Nama sekolah asal calon siswa. Lihat {@link #getSekolahAsal()}.
	 */
	private String sekolahAsal;
	/**
	 * Alamat sekolah asal. Lihat {@link #getAlamatSekolahAsal()}.
	 */
	private String alamatSekolahAsal;
	/**
	 * Desa/kelurahan sekolah asal. Lihat {@link #getDesaKelurahanSekolahAsal()}.
	 */
	private String desaKelurahanSekolahAsal;
	/**
	 * Kecamatan sekolah asal. Lihat {@link #getKecamatanSekolahAsal()}.
	 */
	private Wilayah kecamatanSekolahAsal;
	/**
	 * Kabupaten/kota sekolah asal. Lihat {@link #getKotaSekolahAsal()}.
	 */
	private Kota kotaSekolahAsal;
	/**
	 * Propinsi sekolah asal. Lihat {@link #getPropinsiSekolahAsal()}.
	 */
	private Propinsi propinsiSekolahAsal;
	/**
	 * Status calon siswa dalam keluarga. Lihat {@link #getStatusDalamKeluarga()}.
	 */
	private String statusDalamKeluarga;
	/**
	 * Tahun masuk (angkatan) calon siswa. Lihat {@link #getTahunMasuk()}.
	 */
	private Integer tahunMasuk;
	/**
	 * Tanggal lahir calon siswa. Lihat {@link #getTanggalLahir()}.
	 */
	private Date tanggalLahir;
	/**
	 * Nomor telepon orang tua. Lihat {@link #getTeleponOrangTua()}.
	 */
	private String teleponOrangTua;
	/**
	 * Nomor telepon calon siswa. Lihat {@link #getTeleponSiswa()}.
	 */
	private String teleponSiswa;
	/**
	 * Nomor telepon wali. Lihat {@link #getTeleponWali()}.
	 */
	private String teleponWali;
	/**
	 * Tempat lahir calon siswa. Lihat {@link #getTempatLahir()}.
	 */
	private String tempatLahir;
	/**
	 * Tempat lahir wali. Lihat {@link #getTempatLahirWali()}.
	 */
	private String tempatLahirWali;
	/**
	 * Bahasa sehari-hari calon siswa. Lihat {@link #getBahasa()}.
	 */
	private String bahasa;
	/**
	 * Berat badan calon siswa dalam kilogram. Lihat {@link #getBerat()}.
	 */
	private Double berat;
	/**
	 * Golongan darah calon siswa. Lihat {@link #getGolonganDarah()}.
	 */
	private String golonganDarah;
	/**
	 * Hobi calon siswa (pilihan singkat). Lihat {@link #getHobby()}.
	 */
	private String hobby;
	/**
	 * Nomor HP utama ayah. Lihat {@link #getHp1ayah()}.
	 */
	private String hp1ayah;
	/**
	 * Nomor WhatsApp ayah. Lihat {@link #getWaAyah()}.
	 */
	private String waAyah;
	/**
	 * Nomor HP utama ibu. Lihat {@link #getHp1ibu()}.
	 */
	private String hp1ibu;
	/**
	 * Nomor WhatsApp ibu. Lihat {@link #getWaIbu()}.
	 */
	private String waIbu;
	/**
	 * Nomor HP kedua ayah. Lihat {@link #getHp2ayah()}.
	 */
	private String hp2ayah;
	/**
	 * Nomor HP kedua ibu. Lihat {@link #getHp2ibu()}.
	 */
	private String hp2ibu;
	/**
	 * Nomor HP ketiga ayah. Lihat {@link #getHp3ayah()}.
	 */
	private String hp3ayah;
	/**
	 * Nomor HP ketiga ibu. Lihat {@link #getHp3ibu()}.
	 */
	private String hp3ibu;
	/**
	 * Nomor WhatsApp wali. Lihat {@link #getWaWali()}.
	 */
	private String waWali;
	/**
	 * Jumlah saudara kandung. Lihat {@link #getJumlahSaudaraKandung()}.
	 */
	private Integer jumlahSaudaraKandung;
	/**
	 * Jumlah saudara tiri. Lihat {@link #getJumlahSaudaraTiri()}.
	 */
	private Integer jumlahSaudaraTiri;
	/**
	 * Kewarganegaraan calon siswa. Lihat {@link #getKewarganegaraan()}.
	 */
	private String kewarganegaraan;
	/**
	 * Kondisi khusus calon siswa. Lihat {@link #getKondisiSiswa()}.
	 */
	private String kondisiSiswa;
	/**
	 * Nama panggilan calon siswa. Lihat {@link #getPanggilan()}.
	 */
	private String panggilan;
	/**
	 * Pendidikan terakhir ayah. Lihat {@link #getPendidikanAyah()}.
	 */
	private Pendidikan pendidikanAyah;
	/**
	 * Pendidikan terakhir ibu. Lihat {@link #getPendidikanIbu()}.
	 */
	private Pendidikan pendidikanIbu;
	/**
	 * Rentang penghasilan ayah. Lihat {@link #getPenghasilanAyah()}.
	 */
	private PenghasilanOrangTuaSiswa penghasilanAyah;
	/**
	 * Rentang penghasilan ibu. Lihat {@link #getPenghasilanIbu()}.
	 */
	private PenghasilanOrangTuaSiswa penghasilanIbu;
	/**
	 * Riwayat penyakit calon siswa. Lihat {@link #getRiwayatPenyakit()}.
	 */
	private String riwayatPenyakit;
	/**
	 * Status keikutsertaan calon siswa. Lihat {@link #getStatusSiswa()}.
	 */
	private String statusSiswa;
	/**
	 * Tanggal lahir ayah. Lihat {@link #getTanggalLahirAyah()}.
	 */
	private Date tanggalLahirAyah;
	/**
	 * Tanggal lahir ibu. Lihat {@link #getTanggalLahirIbu()}.
	 */
	private Date tanggalLahirIbu;
	/**
	 * Tempat lahir ayah. Lihat {@link #getTempatLahirAyah()}.
	 */
	private String tempatLahirAyah;
	/**
	 * Tempat lahir ibu. Lihat {@link #getTempatLahirIbu()}.
	 */
	private String tempatLahirIbu;
	/**
	 * Tinggi badan calon siswa dalam sentimeter. Lihat {@link #getTinggi()}.
	 */
	private Double tinggi;
	/**
	 * Uraian hobi calon siswa dalam bentuk teks bebas. Lihat {@link #getHobbyS()}.
	 */
	private String hobbyS;
	/**
	 * Pendidikan terakhir wali. Lihat {@link #getPendidikanWali()}.
	 */
	private Pendidikan pendidikanWali;
	/**
	 * Penghasilan ayah dalam bentuk teks bebas. Lihat {@link #getPenghasilanAyahS()}.
	 */
	private String penghasilanAyahS;
	/**
	 * Penghasilan ibu dalam bentuk teks bebas. Lihat {@link #getPenghasilanIbuS()}.
	 */
	private String penghasilanIbuS;
	/**
	 * Rentang penghasilan wali. Lihat {@link #getPenghasilanWali()}.
	 */
	private PenghasilanOrangTuaSiswa penghasilanWali;
	/**
	 * NISN (nomor induk siswa nasional). Lihat {@link #getNomorIndukNasional()}.
	 */
	private String nomorIndukNasional;
	/**
	 * Kode unik pendaftaran. Lihat {@link #getKodeUniq()}.
	 */
	private String kodeUniq;
	/**
	 * Catatan bebas panitia atas pendaftaran ini. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;
	/**
	 * Tanggal lahir wali. Lihat {@link #getTanggalLahirWali()}.
	 */
	private Date tanggalLahirWali;
	/**
	 * Negara asal calon siswa. Lihat {@link #getNegara()}.
	 */
	private Negara negara;

	/**
	 * Penanda baris pendaftaran masih aktif. Lihat {@link #getAktif()}.
	 */
	private Boolean aktif;
	/**
	 * Kata sandi portal PSB milik calon siswa. Lihat {@link #getPass()}.
	 */
	private String pass;
	/**
	 * Penanda bahwa kolom kata sandi sudah berisi ciphertext. Lihat {@link #getIs_encripted()}.
	 */
	private Boolean is_encripted;
	/**
	 * Kata sandi akun orang tua. Lihat {@link #getPassOrtu()}.
	 */
	private String passOrtu;
	/**
	 * Nama pengguna akun orang tua. Lihat {@link #getUserOrtu()}.
	 */
	private String userOrtu;
	/**
	 * Yayasan penaung sekolah tujuan. Lihat {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Nomor HP utama wali. Lihat {@link #getHp1wali()}.
	 */
	private String hp1wali;
	/**
	 * Nomor HP kedua wali. Lihat {@link #getHp2wali()}.
	 */
	private String hp2wali;
	/**
	 * Nomor HP ketiga wali. Lihat {@link #getHp3wali()}.
	 */
	private String hp3wali;

	/**
	 * Baris siswa aktif yang terbit dari pendaftaran ini. Lihat {@link #getSiswa()}.
	 */
	private Siswa siswa;
	/**
	 * Penanda calon siswa sudah dinyatakan diterima. Lihat {@link #getTelahDiterima()}.
	 */
	private Boolean telahDiterima;
	/**
	 * Penanda berkas dan data calon siswa sudah diverifikasi panitia. Lihat {@link #getTerverifikasi()}.
	 */
	private Boolean terverifikasi;
	/**
	 * Penanda calon siswa menyetujui surat pernyataan pendaftaran. Lihat {@link #getPernyataan()}.
	 */
	private Boolean pernyataan;
	/**
	 * Jawaban parameter tambahan dalam bentuk berbasis id. Lihat {@link #getParameterTambahanInds()}.
	 */
	private String parameterTambahanInds;
	/**
	 * Jawaban parameter tambahan dalam bentuk berlabel. Lihat {@link #getParameterTambahan()}.
	 */
	private String parameterTambahan;
	/**
	 * Penanda calon siswa pernah masuk ke portal PSB. Lihat {@link #getTelahLogin()}.
	 */
	private Boolean telahLogin;
	/**
	 * Waktu calon siswa terakhir masuk ke portal PSB. Lihat {@link #getWaktuLogin()}.
	 */
	private Date waktuLogin;
	/**
	 * Pencacah pencetakan kartu pendaftaran. Lihat {@link #getCetakKartu()}.
	 */
	private Integer cetakKartu;

	/**
	 * Nomor peserta ujian seleksi PSB. Lihat {@link #getNoUjian()}.
	 */
	private String noUjian;

	/**
	 * Penanda calon siswa dinyatakan tidak diterima. Lihat {@link #getDitolak()}.
	 */
	private Boolean ditolak;
	/**
	 * Penanda calon siswa mengundurkan diri. Lihat {@link #getMengundurkanDiri()}.
	 */
	private Boolean mengundurkanDiri;

	/**
	 * Alasan calon siswa layak menerima PIP. Lihat {@link #getLayakPip()}.
	 */
	private String layakPip;
	/**
	 * Alasan calon siswa tidak layak menerima PIP. Lihat {@link #getTidakLayakPip()}.
	 */
	private String tidakLayakPip;
	/**
	 * Jenis tempat tinggal calon siswa dalam bentuk teks. Lihat {@link #getJenisTinggal()}.
	 */
	private String jenisTinggal;
	/**
	 * Nomor Kartu Indonesia Pintar. Lihat {@link #getNoKip()}.
	 */
	private String noKip;
	/**
	 * Alat transportasi ke sekolah dalam bentuk teks. Lihat {@link #getAlatTransportasi()}.
	 */
	private String alatTransportasi;
	/**
	 * Jenis tempat tinggal calon siswa (relasi katalog). Lihat {@link #getJenisTinggalMahasiswa()}.
	 */
	private JenisTinggalMahasiswa jenisTinggalMahasiswa;
	/**
	 * Alat transportasi ke sekolah (relasi katalog). Lihat {@link #getAlatTransportasiMahasiswa()}.
	 */
	private AlatTransportasiMahasiswa alatTransportasiMahasiswa;
	/**
	 * Status sekolah asal (Negeri atau Swasta). Lihat {@link #getStatusSekolah()}.
	 */
	private String statusSekolah;
	/**
	 * Tahun kelulusan dari sekolah asal. Lihat {@link #getTahunLulus()}.
	 */
	private Integer tahunLulus;
	/**
	 * Nomor ijazah sekolah asal. Lihat {@link #getNoIjazah()}.
	 */
	private String noIjazah;
	/**
	 * Formula penilaian prestasi calon siswa dalam bentuk JSON. Lihat {@link #getFormulaPrestasi()}.
	 */
	private String formulaPrestasi;
	/**
	 * Status awal calon siswa (Baru, Pindahan, dan seterusnya). Lihat {@link #getStatusAwalSiswa()}.
	 */
	private StatusAwalSiswa statusAwalSiswa;
	/**
	 * Slot jadwal pertemuan tatap muka yang dipilih calon siswa. Lihat {@link #getJadwalPertemuanPSB()}.
	 */
	private JadwalPertemuanPSB jadwalPertemuanPSB;

	/**
	 * Penanda calon siswa punya saudara kandung yang bersekolah di sini. Lihat {@link
	 * #getApakahMempunyaiSaudaraKandung()}.
	 */
	private Boolean apakahMempunyaiSaudaraKandung;
	/**
	 * Keterangan saudara kandung yang bersekolah di sini. Lihat {@link
	 * #getInfoMempunyaiSaudaraKandung()}.
	 */
	private String infoMempunyaiSaudaraKandung;
	/**
	 * Daftar kebutuhan khusus/disabilitas calon siswa. Lihat {@link #getKebutuhanKhusus()}.
	 */
	private String kebutuhanKhusus;
	/**
	 * Penanda calon siswa penerima bantuan pendidikan. Lihat {@link #getPenerimaBantuan()}.
	 */
	private Boolean penerimaBantuan;
	/**
	 * Baris siswa alumni jenjang sebelumnya yang merupakan orang yang sama. Lihat {@link
	 * #getSiswaAlumni()}.
	 */
	private Siswa siswaAlumni;

	/**
	 * Penanda calon siswa diasuh wali, bukan orang tua kandung. Lihat {@link #getMempunyaiWali()}.
	 */
	private Boolean mempunyaiWali;
	/**
	 * Daftar id kelas les yang dipilih calon siswa, dipisah koma. Lihat {@link #getKelasLesDipilih()}.
	 */
	private String kelasLesDipilih;

	/**
	 * Daftar id pengaturan biaya yang sudah terbayar, dipisah koma. Lihat {@link
	 * #getRiwayatPengaturanPembayaran()}.
	 */
	private String riwayatPengaturanPembayaran;
	/**
	 * Daftar id jenis biaya yang sudah terbayar, dipisah koma. Lihat {@link
	 * #getRiwayatJenisPembayaran()}.
	 */
	private String riwayatJenisPembayaran;
	/**
	 * Daftar id baris pembayaran calon siswa, dipisah koma. Lihat {@link #getRiwayatPembayaran()}.
	 */
	private String riwayatPembayaran;
	/**
	 * Uraian teks seluruh pembayaran calon siswa. Lihat {@link #getRiwayatPembayaranInfo()}.
	 */
	private String riwayatPembayaranInfo;
	/**
	 * Daftar id pembayaran biaya pendaftaran, dipisah koma. Lihat {@link
	 * #getRiwayatPembayaranPendaftaran()}.
	 */
	private String riwayatPembayaranPendaftaran;
	/**
	 * Daftar id pembayaran biaya daftar ulang, dipisah koma. Lihat {@link
	 * #getRiwayatPembayaranDaftarUlang()}.
	 */
	private String riwayatPembayaranDaftarUlang;
	/**
	 * Sumber informasi calon siswa mengenal sekolah ini. Lihat {@link #getInfoKampusDariMana()}.
	 */
	private String infoKampusDariMana;
	/**
	 * Keterangan tambahan sumber informasi sekolah. Lihat {@link #getKeteranganInfoKampusDariMana()}.
	 */
	private String keteranganInfoKampusDariMana;
	/**
	 * Nama kenalan yang merekomendasikan sekolah ini. Lihat {@link #getNamaTemanInfoKampusDariMana()}.
	 */
	private String namaTemanInfoKampusDariMana;
	/**
	 * Penanda calon siswa merupakan siswa pindahan. Lihat {@link #getMerupakanPindahan()}.
	 */
	private Boolean merupakanPindahan;
	/**
	 * Tanggal kepindahan dari sekolah sebelumnya. Lihat {@link #getTanggalPindah()}.
	 */
	private Date tanggalPindah;
	/**
	 * Alasan/keterangan kepindahan. Lihat {@link #getKeteranganPindah()}.
	 */
	private String keteranganPindah;
	/**
	 * Nama sekolah asal kepindahan. Lihat {@link #getPindahanDariSekolah()}.
	 */
	private String pindahanDariSekolah;
	/**
	 * Alamat sekolah asal kepindahan. Lihat {@link #getAlamatSekolahPindahan()}.
	 */
	private String alamatSekolahPindahan;
	/**
	 * Kelas terakhir di sekolah asal kepindahan. Lihat {@link #getKelasSekolahPindahan()}.
	 */
	private String kelasSekolahPindahan;
	/**
	 * Pegawai yayasan yang merupakan orang tua calon siswa. Lihat {@link #getOrangTuaPegawai()}.
	 */
	private Pegawai orangTuaPegawai;
	/**
	 * Baris keluarga pegawai yang menautkan calon siswa ke orang tuanya. Lihat {@link #getKeluarga()}.
	 */
	private Keluarga keluarga;
	/**
	 * Kakak atau adik calon siswa yang sudah bersekolah di sini. Lihat {@link #getSiswaSibling()}.
	 */
	private Siswa siswaSibling;
	/**
	 * Penampung bebas untuk field tambahan hasil kustomisasi instalasi. Lihat {@link
	 * #getFieldsGeneric()}.
	 */
	private String fieldsGeneric;

	/**
	 * Representasi teks ringkas baris ini: {@code <id>-<nomorInduk>-<namaSiswa>}.
	 *
	 * <p>Dipakai luas oleh komponen ZK (label combobox/listbox), pesan log, dan beberapa laporan.
	 * Membaca <b>field</b> secara langsung, bukan getter, sehingga tidak memicu satu pun efek
	 * samping tulis-balik yang dijelaskan di Javadoc kelas &mdash; tetapi juga berarti nilainya bisa
	 * berbeda dari yang dikembalikan {@link #getNomorInduk()}/{@link #getNamaSiswa()} bila field
	 * belum pernah diisi oleh getter tersebut.</p>
	 *
	 * @return gabungan id, nomor induk, dan nama siswa yang dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nomorInduk + "-" + namaSiswa;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak menginisialisasi apa pun kecuali {@link #getTanggal_dirubah()} yang sudah diberi
	 * nilai bawaan pada deklarasi field-nya. Objek hasil konstruktor ini belum punya
	 * {@link #getGelombangPendaftaranPsb() gelombang}, sehingga sebagian besar getter yang menurunkan
	 * nilai dari gelombang akan memulangkan nilai fallback.</p>
	 */
	public CalonSiswa() {
	}

	/**
	 * Konstruktor pintasan yang hanya menyetel kunci primer.
	 *
	 * <p>Dipakai sebagai <i>stub</i> untuk perbandingan dan sebagai nilai {@code Comboitem} pada
	 * beberapa layar &mdash; BUKAN untuk menyimpan baris baru. Objek hasil konstruktor ini kosong
	 * di seluruh kolom lain; menyimpannya lewat {@code session.update()} akan menimpa baris asli
	 * dengan nilai kosong.</p>
	 *
	 * @param id kunci primer baris yang diwakili
	 */
	public CalonSiswa(Long id) {
		this.id = id;
	}

	/**
	 * Merender alamat email calon siswa sebagai tombol {@code mailto:} di dalam komponen ZK.
	 *
	 * <p>Dipanggil dari renderer baris pada layar-layar daftar calon siswa. Tombol selalu dibuat;
	 * ikon, gaya, dan {@code href} hanya dipasang bila email benar-benar terisi.</p>
	 *
	 * <p><b>Efek samping penting:</b> method ini memanggil {@link #getAlamatEmail()}, yang dapat
	 * <b>MEMBANGKITKAN dan menuliskan alamat email baru</b> ke field bila kolomnya kosong dan
	 * konfigurasi {@code alamat_email_default} tidak kosong. Merender satu halaman daftar karena itu
	 * dapat mencetak alamat email untuk banyak calon siswa sekaligus. Lihat
	 * {@link #generateEmail()}.</p>
	 *
	 * @param vbox komponen induk ZK tempat tombol email dilekatkan
	 */
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

	/**
	 * Merender nomor telepon calon siswa dan orang tua sebagai tombol tautan WhatsApp.
	 *
	 * <p>Dipanggil dari renderer baris pada layar-layar daftar calon siswa. Perilakunya:</p>
	 * <ol>
	 * <li>Mengambil {@link #getTeleponSiswa()} dan {@link #getTeleponOrangTua()}.</li>
	 * <li>Menyaring nilai <i>placeholder</i> yang lazim dipakai operator untuk mengisi kolom wajib
	 * ({@code "08100000000000000000"}, {@code "0000000000"}, {@code "00000000000000000000"},
	 * {@code "000000000"}) sehingga tidak ikut ditampilkan.</li>
	 * <li>Bila nomor siswa kosong, memakai nomor orang tua sebagai gantinya.</li>
	 * <li>Menormalkan awalan menjadi format internasional ({@code 08&hellip;} &rarr; {@code
	 * +628&hellip;},
	 * {@code 0&hellip;} &rarr; {@code +62&hellip;}, dan menambahkan {@code +62} bila belum ada
	 * tanda {@code +}) lalu merakit tautan {@code https://web.whatsapp.com/send?phone=&hellip;}.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk:</b> normalisasi awalan dilakukan pada variabel lokal saja, jadi tidak ada tulis
	 * balik ke database di sini &mdash; tetapi {@link #getTeleponSiswa()} dan
	 * {@link #getTeleponOrangTua()} yang dipanggil di awal SENDIRI menulis balik nilai dari
	 * {@link #getSiswaAlumni()} bila kolomnya kosong.</p>
	 *
	 * <p>Blok {@code catch} membangun ulang tautan yang sama memakai komponen {@code A} sebagai
	 * jalur cadangan bila pembuatan {@code Toolbarbutton} gagal (mis. saat dipanggil di luar
	 * konteks ZK).</p>
	 *
	 * @param vbox komponen induk ZK tempat tombol nomor telepon dilekatkan
	 */
	public void tampilkanHp(Component vbox) {
		try {

			String hp = getTeleponSiswa();
			String telp = getTeleponOrangTua();

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
			String hp = getTeleponSiswa();
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

	/**
	 * Kunci primer baris pendaftaran ini.
	 *
	 * <p>Dipetakan ke kolom {@code id} dengan {@code @GeneratedValue(strategy = IDENTITY)} &mdash;
	 * pada PostgreSQL berarti kolom {@code serial}/{@code identity} yang nilainya <b>berurutan dan
	 * rapat</b>. Perhatikan pula {@code insertable = false}: nilai id yang disetel manual TIDAK
	 * pernah ikut pada {@code INSERT}, database selalu yang menentukan.</p>
	 *
	 * <p><b>PERINGATAN KEAMANAN.</b> Justru sifat berurutan inilah yang membuat kebocoran
	 * pra-otentikasi pada Javadoc kelas dapat dieksploitasi secara massal: nilai ini muncul sebagai
	 * parameter URL {@code id} pada {@code /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=_sukses_login},
	 * {@code &amp;s=_wawancara_service}, {@code &amp;s=_cetak_kartu_pendaftaran}, dan
	 * {@code &amp;s=_cetak_kartu_ujian}; menjadi isi tautan login tanpa kata sandi
	 * {@link #urlLogin()}; serta menjadi kunci lampiran pada
	 * {@link ais.database.model.file.LampiranLain} dan {@link ais.database.model.file.FotoCalonSiswa}.
	 * Menaikkan nilai satu per satu sudah cukup untuk menelusuri seluruh pendaftar satu instalasi.
	 * Setiap perbaikan yang mengandalkan "id sulit ditebak" akan gagal di sini.</p>
	 *
	 * @return kunci primer baris, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer baris pendaftaran.
	 *
	 * @param id kunci primer baris pendaftaran
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gelombang pendaftaran PSB tempat calon siswa ini mendaftar (kolom
	 * {@code current_gelombang_pendaftaran_psb_id}).
	 *
	 * <p>Relasi ini adalah <b>sumber kebenaran</b> bagi banyak properti lain pada kelas ini:
	 * {@link #getSekolah()}, {@link #getYayasan()}, {@link #getTahunMasuk()},
	 * {@link #getStatusAwalSiswa()}, {@link #getSekolahAsal()}, {@link #getAlamatSekolahAsal()},
	 * {@link #getPenjurusanSekolah()}, dan {@link #getTelahDiterima()} semuanya membaca nilai dari
	 * sini dan MENIMPA field lokalnya.</p>
	 *
	 * <p>Memanggil {@code check(&hellip;)} milik {@link ais.database.model.GeneralValueObject} untuk
	 * memulihkan objek yang lepas dari session Hibernate.</p>
	 *
	 * @return gelombang pendaftaran, atau {@code null} bila baris dibuat di luar alur PSB
	 * (kondisi ini dipakai {@code CalonSiswaAction} sebagai penyaring
	 * {@code isNotNull("gelombangPendaftaranPsb")} untuk menyembunyikan baris cacat)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "current_gelombang_pendaftaran_psb_id")
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return this.gelombangPendaftaranPsb;
	}

	/**
	 * Menetapkan gelombang pendaftaran PSB tempat calon ini mendaftar.
	 *
	 * @param gelombangPendaftaranPsb gelombang pendaftaran PSB tempat calon ini mendaftar
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Sekolah tujuan pendaftaran (kolom {@code sekolah_id}, {@code nullable = false}).
	 *
	 * <p><b>Getter tulis-balik.</b> Bila {@link #getGelombangPendaftaranPsb()} terisi, nilai sekolah
	 * SELALU diambil ulang dari gelombang dan ditulis ke field &mdash; kolom {@code sekolah_id} pada
	 * baris ini hanyalah salinan. Konsekuensinya: memindahkan sebuah gelombang ke sekolah lain akan
	 * memindahkan SELURUH pendaftarnya begitu barisnya tersentuh, tanpa layar konfirmasi dan tanpa
	 * jejak Envers bila perubahannya terjadi pada jalur baca.</p>
	 *
	 * @return sekolah tujuan; {@code null} hanya mungkin pada baris cacat tanpa gelombang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		if (gelombangPendaftaranPsb != null) {
			sekolah = gelombangPendaftaranPsb.getSekolah();
		}
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah tujuan pendaftaran.
	 *
	 * <p>Menolak objek yang belum tersimpan: bila argumen {@code null} ATAU {@code id}-nya
	 * {@code null}, field diisi {@code null}. Ini mencegah Hibernate mencoba meng-{@code INSERT}
	 * sekolah baru lewat {@code CascadeType.PERSIST}.</p>
	 *
	 * <p>Perlu diingat nilai yang disetel di sini dapat ditimpa kembali oleh
	 * {@link #getSekolah()} bila gelombang pendaftaran menunjuk sekolah lain.</p>
	 *
	 * @param sekolah sekolah tujuan, boleh {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Yayasan penaung sekolah tujuan (kolom {@code yayasan_id}).
	 *
	 * <p><b>Getter tulis-balik</b> dengan pola yang sama seperti {@link #getSekolah()}: bila
	 * gelombang pendaftaran terisi, yayasan selalu diambil ulang dari gelombang dan menimpa field.</p>
	 *
	 * @return yayasan penaung, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		if (gelombangPendaftaranPsb != null) {
			yayasan = gelombangPendaftaranPsb.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menetapkan yayasan penaung.
	 *
	 * <p>Menolak objek yang belum tersimpan ({@code id} masih {@code null}) dengan mengisi field
	 * {@code null}, agar {@code CascadeType.PERSIST} tidak menerbitkan baris yayasan baru.</p>
	 *
	 * @param yayasan yayasan penaung, boleh {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan agama calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code agama_id}, relasi {@code @ManyToOne} yang dimuat malas ({@code LAZY})
	 * dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Agama calon siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama_id", nullable = true)
	public Agama getAgama() {
		agama = check(agama);
		return this.agama;
	}

	/**
	 * Menetapkan agama calon siswa.
	 *
	 * @param agama agama calon siswa
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Alamat email calon siswa (kolom {@code alamat_email}).
	 *
	 * <p><b>Getter tulis-balik yang dapat MENCETAK data baru.</b> Urutan kerjanya:</p>
	 * <ol>
	 * <li>Bila email terisi tetapi tidak lolos {@code Common.isValidEmailAddress}, field
	 * DIKOSONGKAN &mdash; alamat yang salah ketik hilang permanen begitu baris dibaca.</li>
	 * <li>Nilai di bawah tiga karakter juga dianggap kosong.</li>
	 * <li>Bila masih kosong dan {@link #getSiswaAlumni()} punya email, email alumni disalin ke
	 * sini.</li>
	 * <li>Bila masih kosong, {@code nama} terisi, dan konfigurasi {@code alamat_email_default}
	 * (bawaan {@code "@eschool.id"}) tidak kosong, sebuah alamat BARU dibangkitkan lewat
	 * {@link #generateEmail()} dan ditulis ke field.</li>
	 * </ol>
	 *
	 * <p>Karena akses properti, semua penulisan itu ikut ter-flush. Merender daftar calon siswa
	 * karena itu dapat menerbitkan ratusan alamat email sintetis sekaligus. Nilai yang dikembalikan
	 * juga dibersihkan dari spasi.</p>
	 *
	 * <p><b>Perhatian tambahan:</b> cabang nomor 4 memeriksa <b>field</b> {@code nama}, bukan
	 * {@link #getNama()}. Bila getter {@code getNama()} belum pernah dipanggil pada instance ini,
	 * {@code nama} masih {@code null} dan email tidak dibangkitkan &mdash; sehingga hasilnya
	 * bergantung pada urutan pemanggilan getter, bukan pada isi database.</p>
	 *
	 * @return alamat email tanpa spasi; string kosong bila tidak ada dan pembangkitan tidak aktif
	 */
	@Column(name = "alamat_email")
	public String getAlamatEmail() {
		if (alamatEmail != null && !alamatEmail.trim().isEmpty()) {
			alamatEmail = Common.isValidEmailAddress(alamatEmail) ? alamatEmail : "";
		}
		alamatEmail = this.alamatEmail == null || this.alamatEmail.length() < 3 ? "" : this.alamatEmail.trim();

		if (alamatEmail == null || alamatEmail.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getAlamatEmail() != null) {
				alamatEmail = getSiswaAlumni().getAlamatEmail();
			}
		}

		if (nama != null && !nama.trim().isEmpty() && alamatEmail.trim().isEmpty()
				&& !Common.getKonfigurasi("alamat_email_default", "@eschool.id").getNilai().trim().isEmpty()) {
			alamatEmail = generateEmail();
		}

		return alamatEmail.replaceAll(" ", "");
	}

	/**
	 * Membangkitkan alamat email sintetis untuk calon siswa yang belum punya email.
	 *
	 * <p>Bentuknya: nama lengkap yang dibersihkan dari karakter non-alfanumerik dan spasi, dijadikan
	 * huruf kecil, ditambah tiga digit acak (100&ndash;999), ditambah sufiks domain dari konfigurasi
	 * {@code alamat_email_default} (bawaan {@code "@eschool.id"}).</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getNama()}, yang sendiri menulis balik ke field
	 * {@code nama}. Hasil method ini juga TIDAK deterministik &mdash; memanggilnya dua kali
	 * menghasilkan dua alamat berbeda, sehingga ia hanya boleh dipanggil sekali lalu disimpan
	 * (persis yang dilakukan {@link #getAlamatEmail()}).</p>
	 *
	 * <p><b>Risiko tabrakan:</b> hanya tiga digit acak yang membedakan dua anak bernama sama; tidak
	 * ada pemeriksaan keunikan ke database, dan kolom {@code alamat_email} tidak {@code unique}.</p>
	 *
	 * @return alamat email yang baru dibangkitkan
	 * @throws NullPointerException bila nama calon siswa masih {@code null}
	 */
	public String generateEmail() {
		return getNama().toLowerCase().replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", "")
				+ ThreadLocalRandom.current().nextLong(100, 999)
				+ Common.getKonfigurasi("alamat_email_default", "@eschool.id").getNilai().trim();
	}

	/**
	 * Menetapkan alamat email calon siswa.
	 *
	 * @param alamatEmail alamat email calon siswa
	 */
	public void setAlamatEmail(String alamatEmail) {
		this.alamatEmail = alamatEmail;
	}

	/**
	 * Mengembalikan alamat tempat tinggal orang tua.
	 *
	 * <p>Dipetakan ke kolom {@code alamat_orang_tua}, panjang maksimum 2000 karakter.</p>
	 *
	 * @return Alamat tempat tinggal orang tua
	 */
	@Column(name = "alamat_orang_tua", length = 2000)
	public String getAlamatOrangTua() {
		return this.alamatOrangTua;
	}

	/**
	 * Menetapkan alamat tempat tinggal orang tua.
	 *
	 * @param alamatOrangTua alamat tempat tinggal orang tua
	 */
	public void setAlamatOrangTua(String alamatOrangTua) {
		this.alamatOrangTua = alamatOrangTua;
	}

	/**
	 * Mengembalikan alamat tempat tinggal calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code alamat_siswa}, panjang maksimum 2000 karakter.</p>
	 *
	 * <p>Getter mengembalikan string kosong (bukan {@code null}) bila kolom kosong, dan memangkas spasi
	 * tepi.</p>
	 *
	 * @return Alamat tempat tinggal calon siswa
	 */
	@Column(name = "alamat_siswa", length = 2000)
	public String getAlamatSiswa() {
		return this.alamatSiswa == null ? "" : alamatSiswa.trim();
	}

	/**
	 * Menetapkan alamat tempat tinggal calon siswa.
	 *
	 * @param alamatSiswa alamat tempat tinggal calon siswa
	 */
	public void setAlamatSiswa(String alamatSiswa) {
		this.alamatSiswa = alamatSiswa;
	}

	/**
	 * Mengembalikan alamat tempat tinggal wali.
	 *
	 * <p>Dipetakan ke kolom {@code alamat_wali}.</p>
	 *
	 * <p>Bila alamat wali kosong, getter mengembalikan {@link #getAlamatAyah()} sebagai gantinya &mdash;
	 * nilai fallback ini TIDAK ditulis balik ke field, jadi kolom {@code alamat_wali} tetap kosong di
	 * database.</p>
	 *
	 * @return Alamat tempat tinggal wali
	 */
	@Column(name = "alamat_wali")
	public String getAlamatWali() {
		return this.alamatWali == null || alamatWali.trim().isEmpty() ? getAlamatAyah() : alamatWali.trim();
	}

	/**
	 * Menetapkan alamat tempat tinggal wali.
	 *
	 * @param alamatWali alamat tempat tinggal wali
	 */
	public void setAlamatWali(String alamatWali) {
		this.alamatWali = alamatWali;
	}

	/**
	 * Mengembalikan urutan calon siswa di antara saudara kandungnya.
	 *
	 * <p>Dipetakan ke kolom {@code anak_ke}.</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code 0} pada nilai yang dikembalikan, tanpa menulis
	 * balik ke field.</p>
	 *
	 * @return Urutan calon siswa di antara saudara kandungnya
	 */
	@Column(name = "anak_ke")
	public Integer getAnakKe() {
		return this.anakKe == null ? 0 : anakKe;
	}

	/**
	 * Menetapkan urutan calon siswa di antara saudara kandungnya.
	 *
	 * @param anakKe urutan calon siswa di antara saudara kandungnya
	 */
	public void setAnakKe(Integer anakKe) {
		this.anakKe = anakKe;
	}

	/**
	 * Mengembalikan jumlah bersaudara dalam keluarga.
	 *
	 * <p>Dipetakan ke kolom {@code dari_anak_ke}.</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code 0} pada nilai yang dikembalikan, tanpa menulis
	 * balik ke field.</p>
	 *
	 * @return Jumlah bersaudara dalam keluarga
	 */
	@Column(name = "dari_anak_ke")
	public Integer getDariAnakKe() {
		return this.dariAnakKe == null ? 0 : dariAnakKe;
	}

	/**
	 * Menetapkan jumlah bersaudara dalam keluarga.
	 *
	 * @param dariAnakKe jumlah bersaudara dalam keluarga
	 */
	public void setDariAnakKe(Integer dariAnakKe) {
		this.dariAnakKe = dariAnakKe;
	}

	/**
	 * Jenis kelamin calon siswa (kolom {@code jenis_kelamin}, {@code nullable = false},
	 * panjang 9).
	 *
	 * <p><b>Getter tulis-balik yang menormalkan data.</b> Nilai {@code "L"}, {@code "Laki-Laki"},
	 * atau apa pun yang mengandung {@code "laki"} diubah menjadi {@code "Laki-laki"}; {@code "P"}
	 * atau apa pun yang mengandung {@code "puan"} menjadi {@code "Perempuan"}; {@code null} menjadi
	 * string kosong. Semua penulisan itu mengenai field, jadi ikut tersimpan.</p>
	 *
	 * <p><b>Kuirk yang perlu diwaspadai:</b> nilai yang tidak dikenali (mis. {@code "-"} atau
	 * {@code "1"}) tetap dibiarkan apa adanya meskipun kolomnya dibatasi 9 karakter; dan karena
	 * {@code null} ditulis menjadi string kosong, kolom {@code nullable = false} tidak pernah gagal
	 * tetapi juga tidak pernah benar-benar terisi. Bandingkan dengan
	 * {@code ItemBiayaSekolah.getKelamin()} yang justru MENGHAPUS nilai lama yang tidak cocok
	 * persis; di sini nilai lama dipertahankan.</p>
	 *
	 * @return {@code "Laki-laki"}, {@code "Perempuan"}, string kosong, atau nilai asli yang tidak
	 * dikenali
	 */
	@Column(name = "jenis_kelamin", nullable = false, length = 9)
	public String getJenisKelamin() {

		if (jenisKelamin != null
				&& (jenisKelamin.trim().equalsIgnoreCase("L") || jenisKelamin.trim().equals("Laki-Laki"))) {
			jenisKelamin = "Laki-laki";
		} else if (jenisKelamin != null && jenisKelamin.trim().equalsIgnoreCase("P")) {
			jenisKelamin = "Perempuan";
		} else if (jenisKelamin == null) {
			jenisKelamin = "";
		}

		if (jenisKelamin.toLowerCase().contains("laki")) {
			jenisKelamin = "Laki-laki";
		} else if (jenisKelamin.toLowerCase().contains("puan")) {
			jenisKelamin = "Perempuan";
		}

		return this.jenisKelamin;
	}

	/**
	 * Menetapkan jenis kelamin calon siswa.
	 *
	 * @param jenisKelamin jenis kelamin calon siswa
	 */
	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	/**
	 * Nama ayah (kolom {@code nama_ayah}).
	 *
	 * <p><b>Getter tulis-balik.</b> Bila {@link #getOrangTuaPegawai()} terisi dan pegawai tersebut
	 * berjenis kelamin laki-laki, nama pegawai MENIMPA nilai kolom. Untuk pendaftar jalur
	 * anak-pegawai, kolom {@code nama_ayah} karena itu tidak pernah bisa berbeda dari master
	 * kepegawaian &mdash; koreksi manual akan hilang pada pembacaan berikutnya.</p>
	 *
	 * @return nama ayah; string kosong (bukan {@code null}) bila belum diisi
	 */
	@Column(name = "nama_ayah")
	public String getNamaAyah() {
		if (getOrangTuaPegawai() != null && getOrangTuaPegawai().getKelamin() != null
				&& getOrangTuaPegawai().getKelamin().equalsIgnoreCase("Laki-laki")) {
			namaAyah = getOrangTuaPegawai().getNama();
		}
		return this.namaAyah == null ? "" : namaAyah;
	}

	/**
	 * Menetapkan nama ayah.
	 *
	 * @param namaAyah nama ayah
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Nama ibu (kolom {@code nama_ibu}).
	 *
	 * <p><b>Getter tulis-balik.</b> Cerminan {@link #getNamaAyah()}: bila
	 * {@link #getOrangTuaPegawai()} terisi dan pegawai tersebut BUKAN laki-laki, nama pegawai
	 * MENIMPA nilai kolom.</p>
	 *
	 * <p><b>Kuirk:</b> pemeriksaannya {@code !equalsIgnoreCase("Laki-laki")}, sehingga pegawai yang
	 * kolom kelaminnya berisi nilai tak terduga (mis. {@code "L"}) akan diperlakukan sebagai ibu.</p>
	 *
	 * @return nama ibu; string kosong (bukan {@code null}) bila belum diisi
	 */
	@Column(name = "nama_ibu")
	public String getNamaIbu() {

		if (getOrangTuaPegawai() != null && getOrangTuaPegawai().getKelamin() != null
				&& !getOrangTuaPegawai().getKelamin().equalsIgnoreCase("Laki-laki")) {
			namaIbu = getOrangTuaPegawai().getNama();
		}

		return this.namaIbu == null ? "" : namaIbu;
	}

	/**
	 * Menetapkan nama ibu.
	 *
	 * @param namaIbu nama ibu
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Nama lengkap calon siswa (kolom {@code nama_siswa}, {@code nullable = false}).
	 *
	 * <p><b>Getter tulis-balik.</b> Bila {@link #getSiswaAlumni()} terisi dan namanya tidak kosong,
	 * nama alumni MENIMPA nilai kolom. Ini disengaja agar pendaftar jenjang lanjutan otomatis
	 * memakai nama resminya, tetapi juga berarti perbaikan ejaan nama pada layar PSB akan hilang
	 * selama tautan alumni masih ada.</p>
	 *
	 * <p>Nilai yang dikembalikan selalu dipangkas spasi tepinya dan tidak pernah {@code null}.</p>
	 *
	 * @return nama lengkap calon siswa; string kosong bila belum diisi
	 */
	@Column(name = "nama_siswa", nullable = false)
	public String getNamaSiswa() {

		if (getSiswaAlumni() != null && getSiswaAlumni().getNama() != null
				&& !getSiswaAlumni().getNama().trim().isEmpty()) {
			namaSiswa = getSiswaAlumni().getNama();
		}

		return this.namaSiswa == null ? "" : namaSiswa.trim();
	}

	/**
	 * Menetapkan nama lengkap calon siswa.
	 *
	 * @param namaSiswa nama lengkap calon siswa
	 */
	public void setNamaSiswa(String namaSiswa) {
		this.namaSiswa = namaSiswa;
	}

	/**
	 * Mengembalikan nama wali.
	 *
	 * <p>Dipetakan ke kolom {@code nama_wali}.</p>
	 *
	 * <p>Bila nama wali kosong, getter mengembalikan {@link #getNamaAyah()} sebagai gantinya (fallback
	 * baca saja, tidak ditulis balik).</p>
	 *
	 * @return Nama wali
	 */
	@Column(name = "nama_wali")
	public String getNamaWali() {

		return this.namaWali == null || namaWali.trim().isEmpty() ? getNamaAyah() : namaWali.trim();
	}

	/**
	 * Menetapkan nama wali.
	 *
	 * @param namaWali nama wali
	 */
	public void setNamaWali(String namaWali) {
		this.namaWali = namaWali;
	}

	/**
	 * Alias baca-saja nomor registrasi pendaftaran.
	 *
	 * <p>Dipetakan ke kolom {@code nomor_induk} yang SAMA dengan {@link #getNoRegistrasi()} dan
	 * {@link #getNopendaftaran()}, dengan {@code insertable = false, updatable = false}.
	 * Satu-satunya properti yang boleh MENULIS kolom itu adalah {@link #setNoRegistrasi(String)}.</p>
	 *
	 * <p><b>Getter tulis-balik:</b> mengisi field dari {@link #getNoRegistrasi()} setiap kali
	 * dipanggil.</p>
	 *
	 * <p>Nama properti ini dipakai luas: {@code LoginCalonSiswaAction} mencocokkan
	 * {@code "nomorInduk"} sebagai identitas login, dan {@code CalonSiswaAction} memakainya sebagai
	 * kolom pencarian. Karena itu jangan mengganti pemetaannya sekalipun terlihat mubazir.</p>
	 *
	 * @return nomor registrasi pendaftaran
	 */
	@Column(name = "nomor_induk", nullable = false, insertable = false, updatable = false)
	public String getNomorInduk() {
		nomorInduk = getNoRegistrasi();
		return this.nomorInduk;
	}

	/**
	 * Menetapkan alias baca-saja nomor registrasi pendaftaran.
	 *
	 * @param nomorInduk alias baca-saja nomor registrasi pendaftaran
	 */
	public void setNomorInduk(String nomorInduk) {
		this.nomorInduk = nomorInduk;
	}

	/**
	 * Mengembalikan tanggal acuan pencetakan dokumen pendaftaran.
	 *
	 * <p>Dipetakan ke kolom {@code pada_tanggal}, disimpan sebagai stempel waktu lengkap.</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan waktu saat ini sebagai pengganti (fallback baca-saja)
	 * &mdash; nilai yang dikembalikan karenanya tidak stabil antar-pemanggilan.</p>
	 *
	 * @return Tanggal acuan pencetakan dokumen pendaftaran
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "pada_tanggal")
	public Date getPadaTanggal() {
		return this.padaTanggal == null ? ais.ui.util.WaktuUtil.getDate() : padaTanggal;
	}

	/**
	 * Menetapkan tanggal acuan pencetakan dokumen pendaftaran.
	 *
	 * @param padaTanggal tanggal acuan pencetakan dokumen pendaftaran
	 */
	public void setPadaTanggal(Date padaTanggal) {
		this.padaTanggal = padaTanggal;
	}

	/**
	 * Mengembalikan pekerjaan ayah.
	 *
	 * <p>Dipetakan ke kolom {@code pekerjaan_ayah_id}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Pekerjaan ayah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_ayah_id")
	public Pekerjaan getPekerjaanAyah() {
		pekerjaanAyah = check(pekerjaanAyah);
		return this.pekerjaanAyah;
	}

	/**
	 * Menetapkan pekerjaan ayah.
	 *
	 * @param pekerjaanAyah pekerjaan ayah
	 */
	public void setPekerjaanAyah(Pekerjaan pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	/**
	 * Mengembalikan pekerjaan ibu.
	 *
	 * <p>Dipetakan ke kolom {@code pekerjaan_ibu_id}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Pekerjaan ibu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_ibu_id")
	public Pekerjaan getPekerjaanIbu() {
		pekerjaanIbu = check(pekerjaanIbu);
		return this.pekerjaanIbu;
	}

	/**
	 * Menetapkan pekerjaan ibu.
	 *
	 * @param pekerjaanIbu pekerjaan ibu
	 */
	public void setPekerjaanIbu(Pekerjaan pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Mengembalikan pekerjaan wali.
	 *
	 * <p>Dipetakan ke kolom {@code pekerjaan_wali_id}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * <p>Bila relasi pekerjaan wali kosong, getter mengembalikan {@link #getPekerjaanAyah()} sebagai
	 * fallback baca-saja.</p>
	 *
	 * @return Pekerjaan wali
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_wali_id")
	public Pekerjaan getPekerjaanWali() {
		pekerjaanWali = check(pekerjaanWali);
		return this.pekerjaanWali == null ? getPekerjaanAyah() : pekerjaanWali;
	}

	/**
	 * Menetapkan pekerjaan wali.
	 *
	 * @param pekerjaanWali pekerjaan wali
	 */
	public void setPekerjaanWali(Pekerjaan pekerjaanWali) {
		this.pekerjaanWali = pekerjaanWali;
	}

	/**
	 * Nama sekolah asal calon siswa (kolom {@code sekolah_asal}).
	 *
	 * <p><b>Getter tulis-balik.</b> Bila gelombang pendaftaran menunjuk sebuah
	 * {@code alumniDari} (gelombang khusus alumni jenjang sebelumnya), nama sekolah tersebut
	 * MENIMPA nilai kolom pada setiap pembacaan. Untuk gelombang alumni, nilai yang diketik operator
	 * karena itu tidak pernah bertahan.</p>
	 *
	 * @return nama sekolah asal, atau {@code null}
	 */
	@Column(name = "sekolah_asal")
	public String getSekolahAsal() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getAlumniDari() != null) {
			sekolahAsal = gelombangPendaftaranPsb.getAlumniDari().getNama();
		}
		return this.sekolahAsal;
	}

	/**
	 * Menetapkan nama sekolah asal calon siswa.
	 *
	 * @param sekolahAsal nama sekolah asal calon siswa
	 */
	public void setSekolahAsal(String sekolahAsal) {
		this.sekolahAsal = sekolahAsal;
	}

	/**
	 * Status awal calon siswa (kolom {@code status_awal_siswa}): Baru, Pindahan, dan seterusnya.
	 *
	 * <p><b>Getter tulis-balik berlapis tiga.</b> Urutannya:</p>
	 * <ol>
	 * <li>Bila gelombang pendaftaran terisi, statusnya diambil dari gelombang (menimpa kolom).</li>
	 * <li>Bila masih {@code null}, dipakai konstanta {@link ais.common.ConstantValues#BARU_SISWA}.</li>
	 * <li>Bila {@link #getMerupakanPindahan()} bernilai {@code true}, status DIPAKSA menjadi
	 * {@link ais.common.ConstantValues#PINDAHAN_SISWA} &mdash; cabang terakhir ini menang atas
	 * kedua cabang sebelumnya.</li>
	 * </ol>
	 *
	 * <p>Karena langkah 2 dan 3 menulis objek konstanta ke field, baris yang semula tidak punya
	 * status akan memperoleh FK baru hanya karena dibaca.</p>
	 *
	 * @return status awal calon siswa; tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_siswa")
	public StatusAwalSiswa getStatusAwalSiswa() {

		if (getGelombangPendaftaranPsb() != null) {
			statusAwalSiswa = getGelombangPendaftaranPsb().getStatusAwalSiswa();
		} else {
			statusAwalSiswa = check(statusAwalSiswa);
		}

		if (statusAwalSiswa == null) {
			statusAwalSiswa = ConstantValues.BARU_SISWA;
		}

		if (getMerupakanPindahan()) {
			statusAwalSiswa = ConstantValues.PINDAHAN_SISWA;
		}

		return statusAwalSiswa;
	}

	/**
	 * Menetapkan status awal calon siswa (Baru, Pindahan, dan seterusnya).
	 *
	 * @param statusAwalSiswa status awal calon siswa (Baru, Pindahan, dan seterusnya)
	 */
	public void setStatusAwalSiswa(StatusAwalSiswa statusAwalSiswa) {
		this.statusAwalSiswa = statusAwalSiswa;
	}

	/**
	 * Mengembalikan status calon siswa dalam keluarga.
	 *
	 * <p>Dipetakan ke kolom {@code status_dalam_keluarga}, panjang maksimum 12 karakter.</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan nilai bawaan {@code "Anak Kandung"} (fallback
	 * baca-saja, tidak ditulis balik).</p>
	 *
	 * @return Status calon siswa dalam keluarga
	 */
	@Column(name = "status_dalam_keluarga", length = 12)
	public String getStatusDalamKeluarga() {
		return this.statusDalamKeluarga == null || statusDalamKeluarga.trim().isEmpty() ? "Anak Kandung"
				: statusDalamKeluarga;
	}

	/**
	 * Menetapkan status calon siswa dalam keluarga.
	 *
	 * @param statusDalamKeluarga status calon siswa dalam keluarga
	 */
	public void setStatusDalamKeluarga(String statusDalamKeluarga) {
		this.statusDalamKeluarga = statusDalamKeluarga;
	}

	/**
	 * Tahun masuk (angkatan) calon siswa (kolom {@code tahun_masuk}, {@code nullable = false}).
	 *
	 * <p><b>Getter tulis-balik.</b> Bila gelombang pendaftaran terisi, tahun masuk gelombang
	 * MENIMPA nilai kolom. Bila hasil akhirnya masih {@code null}, getter mengembalikan tahun
	 * berjalan &mdash; nilai fallback ini TIDAK ditulis balik, sehingga kolomnya bisa tetap
	 * {@code null} di database meskipun getter selalu memulangkan angka.</p>
	 *
	 * @return tahun masuk; tahun berjalan bila belum pernah ditentukan
	 */
	@Column(name = "tahun_masuk", nullable = false)
	public Integer getTahunMasuk() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		if (gelombangPendaftaranPsb != null) {
			tahunMasuk = gelombangPendaftaranPsb.getTahunMasuk();
		}
		return this.tahunMasuk == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : this.tahunMasuk;
	}

	/**
	 * Menetapkan tahun masuk (angkatan) calon siswa.
	 *
	 * @param tahunMasuk tahun masuk (angkatan) calon siswa
	 */
	public void setTahunMasuk(Integer tahunMasuk) {
		this.tahunMasuk = tahunMasuk;
	}

	/**
	 * Tanggal lahir calon siswa (kolom {@code tanggal_lahir}).
	 *
	 * <p>Nilai ini adalah <b>salah satu dari dua faktor login portal PSB</b> (bersama nomor
	 * registrasi/nomor ujian/nama), sehingga ketepatannya berdampak langsung pada akses akun.</p>
	 *
	 * <p><b>Perbaikan yang sudah terpasang &mdash; jangan dikembalikan.</b> Komentar di dalam method
	 * mencatat regresi lama: dahulu nilai SELALU diganti dengan
	 * {@code siswaAlumni.getTanggalLahir()}, sehingga objek {@link Siswa} alumni yang di cache
	 * berisi tanggal epoch (1&nbsp;Januari&nbsp;1970) ikut merusak tanggal lahir calon siswa yang
	 * sudah benar. Sekarang: bila kolom sudah terisi, nilainya dikembalikan apa adanya; salinan dari
	 * alumni hanya dilakukan ketika kolom masih kosong DAN tanggal alumni lolos
	 * {@link #isTanggalLahirCalonValid(java.util.Date)}.</p>
	 *
	 * <p>Cabang penyalinan itu masih tulis-balik, jadi baris tanpa tanggal lahir akan memperoleh
	 * tanggal dari alumni secara permanen begitu dibaca.</p>
	 *
	 * @return tanggal lahir calon siswa, atau {@code null} bila belum diisi dan tidak ada alumni
	 * dengan tanggal yang layak
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir", length = 13)
	public Date getTanggalLahir() {
		/*
		 * Jangan overwrite nilai tanggal_lahir milik CalonSiswa jika field ini sudah
		 * terisi dari database. Sebelumnya nilai ini selalu diganti dari
		 * siswaAlumni.getTanggalLahir(), sehingga jika object Siswa alumni di cache
		 * berisi tanggal default/epoch 1970, tanggal lahir calon siswa yang benar
		 * dari tabel sekolah.calon_siswa ikut berubah saat getter dipanggil.
		 */
		if (this.tanggalLahir != null) {
			return this.tanggalLahir;
		}

		Siswa alumni = getSiswaAlumni();
		if (alumni != null && isTanggalLahirCalonValid(alumni.getTanggalLahir())) {
			tanggalLahir = alumni.getTanggalLahir();
		}

		return this.tanggalLahir;
	}

	/**
	 * Menentukan apakah sebuah tanggal lahir milik {@link Siswa} alumni layak dipakai sebagai
	 * nilai cadangan bagi calon siswa.
	 *
	 * <p>Kriterianya sederhana dan sengaja longgar: tanggal tidak {@code null} dan tahunnya lebih
	 * dari 1971. Ambang itu menyaring nilai bawaan {@code new Date(0)} / epoch yang tampil sebagai
	 * tahun 1970 &mdash; sumber bug yang dijelaskan pada {@link #getTanggalLahir()}. Bila suatu
	 * instalasi benar-benar punya data historis bertahun &le;&nbsp;1971, nilainya tetap dapat
	 * disimpan langsung pada kolom {@code tanggal_lahir} baris ini; yang ditolak hanyalah penyalinan
	 * OTOMATIS dari alumni.</p>
	 *
	 * <p>Seluruh exception ditelan dan dianggap "tidak layak", sehingga method ini tidak pernah
	 * menggagalkan pembacaan entity.</p>
	 *
	 * @param tanggal tanggal lahir kandidat dari entity alumni, boleh {@code null}
	 * @return {@code true} bila tanggal layak dipakai sebagai nilai cadangan
	 */
	private static boolean isTanggalLahirCalonValid(Date tanggal) {
		if (tanggal == null) {
			return false;
		}

		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(tanggal);
			int tahun = calendar.get(Calendar.YEAR);

			/*
			 * Antisipasi nilai default Date(0) / epoch yang tampil sebagai tahun 1970.
			 * Untuk CalonSiswa, tahun <= 1971 tidak layak menjadi fallback otomatis
			 * dari data alumni. Jika memang ada kebutuhan historis khusus, nilai tersebut
			 * tetap dapat disimpan langsung pada field tanggalLahir CalonSiswa sendiri.
			 */
			return tahun > 1971;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Menetapkan tanggal lahir calon siswa.
	 *
	 * @param tanggalLahir tanggal lahir calon siswa
	 */
	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	/**
	 * Nomor telepon orang tua.
	 *
	 * <p><b>Getter tulis-balik:</b> bila kolom kosong dan {@link #getSiswaAlumni()} punya nomor,
	 * nomor alumni disalin ke field. Nilai yang DIKEMBALIKAN dibersihkan dari semua karakter selain
	 * angka dan titik, tetapi pembersihan itu tidak ditulis balik &mdash; kolom di database tetap
	 * menyimpan bentuk aslinya (mis. {@code "0812-3456-7890"}).</p>
	 *
	 * <p>Perhatikan: karena pembersihan hanya terjadi pada nilai kembalian, perbandingan langsung
	 * antara hasil getter dan isi kolom di query SQL akan meleset.</p>
	 *
	 * @return nomor telepon berisi angka saja; string kosong bila tidak ada
	 */
	@Column(name = "telepon_orang_tua")
	public String getTeleponOrangTua() {

		if (teleponOrangTua == null || teleponOrangTua.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getTeleponOrangTua() != null) {
				teleponOrangTua = getSiswaAlumni().getTeleponOrangTua();
			}
		}

		return this.teleponOrangTua == null ? "" : teleponOrangTua.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor telepon orang tua.
	 *
	 * @param teleponOrangTua nomor telepon orang tua
	 */
	public void setTeleponOrangTua(String teleponOrangTua) {
		this.teleponOrangTua = teleponOrangTua;
	}

	/**
	 * Nomor telepon calon siswa.
	 *
	 * <p>Pola identik dengan {@link #getTeleponOrangTua()}: menyalin dari
	 * {@link #getSiswaAlumni()} bila kosong (tulis-balik), lalu mengembalikan versi yang hanya
	 * berisi angka dan titik tanpa menulis balik hasil pembersihan itu.</p>
	 *
	 * @return nomor telepon berisi angka saja; string kosong bila tidak ada
	 */
	@Column(name = "telepon_siswa")
	public String getTeleponSiswa() {

		if (teleponSiswa == null || teleponSiswa.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getTeleponSiswa() != null) {
				teleponSiswa = getSiswaAlumni().getTeleponSiswa();
			}
		}

		return this.teleponSiswa == null ? "" : teleponSiswa.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor telepon calon siswa.
	 *
	 * @param teleponSiswa nomor telepon calon siswa
	 */
	public void setTeleponSiswa(String teleponSiswa) {
		this.teleponSiswa = teleponSiswa;
	}

	/**
	 * Nomor telepon wali.
	 *
	 * <p><b>Getter tulis-balik:</b> menyalin dari {@link #getSiswaAlumni()} bila kolom kosong.
	 * Berbeda dari {@link #getTeleponSiswa()} dan {@link #getTeleponOrangTua()}, nilai yang
	 * dikembalikan TIDAK dibersihkan dan bisa {@code null} &mdash; ketidakseragaman ini nyata dan
	 * harus diantisipasi pemanggil.</p>
	 *
	 * @return nomor telepon wali apa adanya, atau {@code null}
	 */
	@Column(name = "telepon_wali")
	public String getTeleponWali() {

		if (teleponWali == null || teleponWali.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getTeleponWali() != null) {
				teleponWali = getSiswaAlumni().getTeleponWali();
			}
		}

		return this.teleponWali;
	}

	/**
	 * Menetapkan nomor telepon wali.
	 *
	 * @param teleponWali nomor telepon wali
	 */
	public void setTeleponWali(String teleponWali) {
		this.teleponWali = teleponWali;
	}

	/**
	 * Tempat lahir calon siswa (kolom {@code tempat_lahir}).
	 *
	 * <p><b>Getter tulis-balik:</b> menyalin dari {@link #getSiswaAlumni()} bila kolom kosong.</p>
	 *
	 * @return tempat lahir, atau {@code null}
	 */
	@Column(name = "tempat_lahir")
	public String getTempatLahir() {

		if (tempatLahir == null || tempatLahir.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getTempatLahir() != null) {
				tempatLahir = getSiswaAlumni().getTempatLahir();
			}
		}

		return this.tempatLahir;
	}

	/**
	 * Menetapkan tempat lahir calon siswa.
	 *
	 * @param tempatLahir tempat lahir calon siswa
	 */
	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	/**
	 * Mengembalikan bahasa sehari-hari calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code bahasa}.</p>
	 *
	 * @return Bahasa sehari-hari calon siswa
	 */
	@Column(name = "bahasa")
	public String getBahasa() {
		return this.bahasa;
	}

	/**
	 * Menetapkan bahasa sehari-hari calon siswa.
	 *
	 * @param bahasa bahasa sehari-hari calon siswa
	 */
	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	/**
	 * Mengembalikan berat badan calon siswa dalam kilogram.
	 *
	 * <p>Dipetakan ke kolom {@code berat_badan}.</p>
	 *
	 * @return Berat badan calon siswa dalam kilogram
	 */
	@Column(name = "berat_badan")
	public Double getBerat() {
		return this.berat;
	}

	/**
	 * Menetapkan berat badan calon siswa dalam kilogram.
	 *
	 * @param berat berat badan calon siswa dalam kilogram
	 */
	public void setBerat(Double berat) {
		this.berat = berat;
	}

	/**
	 * Mengembalikan golongan darah calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code golongan_darah}, panjang maksimum 14 karakter.</p>
	 *
	 * <p>Data kesehatan anak.</p>
	 *
	 * @return Golongan darah calon siswa
	 */
	@Column(name = "golongan_darah", length = 14)
	public String getGolonganDarah() {
		return this.golonganDarah;
	}

	/**
	 * Menetapkan golongan darah calon siswa.
	 *
	 * @param golonganDarah golongan darah calon siswa
	 */
	public void setGolonganDarah(String golonganDarah) {
		this.golonganDarah = golonganDarah;
	}

	/**
	 * Mengembalikan hobi calon siswa (pilihan singkat).
	 *
	 * <p>Dipetakan ke kolom {@code hobby}.</p>
	 *
	 * @return Hobi calon siswa (pilihan singkat)
	 */
	@Column(name = "hobby")
	public String getHobby() {
		return this.hobby;
	}

	/**
	 * Menetapkan hobi calon siswa (pilihan singkat).
	 *
	 * @param hobby hobi calon siswa (pilihan singkat)
	 */
	public void setHobby(String hobby) {
		this.hobby = hobby;
	}

	/**
	 * Nomor HP utama ayah (kolom {@code hp1ayah}).
	 *
	 * <p><b>Getter tulis-balik:</b> menyalin dari {@link #getSiswaAlumni()} bila kolom kosong.
	 * Nilai kembaliannya dibersihkan menjadi angka dan titik saja (tanpa ditulis balik). Nomor ini
	 * juga menjadi nilai bawaan {@link #getWaAyah()} dan {@link #getHp1wali()}.</p>
	 *
	 * @return nomor HP berisi angka saja; string kosong bila tidak ada
	 */
	@Column(name = "hp1ayah")
	public String getHp1ayah() {

		if (hp1ayah == null || hp1ayah.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getHp1ayah() != null) {
				hp1ayah = getSiswaAlumni().getHp1ayah();
			}
		}

		return this.hp1ayah == null ? "" : hp1ayah.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP utama ayah.
	 *
	 * @param hp1ayah nomor HP utama ayah
	 */
	public void setHp1ayah(String hp1ayah) {
		this.hp1ayah = hp1ayah;
	}

	/**
	 * Nomor HP utama ibu (kolom {@code hp1ibu}).
	 *
	 * <p>Pola identik dengan {@link #getHp1ayah()}: menyalin dari {@link #getSiswaAlumni()} bila
	 * kosong, lalu mengembalikan versi angka saja. Nomor ini menjadi nilai bawaan
	 * {@link #getWaIbu()}.</p>
	 *
	 * @return nomor HP berisi angka saja; string kosong bila tidak ada
	 */
	@Column(name = "hp1ibu")
	public String getHp1ibu() {

		if (hp1ibu == null || hp1ibu.trim().isEmpty()) {
			if (getSiswaAlumni() != null && getSiswaAlumni().getHp1ibu() != null) {
				hp1ibu = getSiswaAlumni().getHp1ibu();
			}
		}

		return this.hp1ibu == null ? "" : hp1ibu.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP utama ibu.
	 *
	 * @param hp1ibu nomor HP utama ibu
	 */
	public void setHp1ibu(String hp1ibu) {
		this.hp1ibu = hp1ibu;
	}

	/**
	 * Mengembalikan nomor HP kedua ayah.
	 *
	 * <p>Dipetakan ke kolom {@code hp2ayah}.</p>
	 *
	 * <p>Getter membuang semua karakter selain angka dan titik dari nilai kolom sebelum
	 * mengembalikannya; hasilnya TIDAK ditulis balik ke field.</p>
	 *
	 * @return Nomor HP kedua ayah
	 */
	@Column(name = "hp2ayah")
	public String getHp2ayah() {
		return this.hp2ayah == null ? "" : hp2ayah.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP kedua ayah.
	 *
	 * @param hp2ayah nomor HP kedua ayah
	 */
	public void setHp2ayah(String hp2ayah) {
		this.hp2ayah = hp2ayah;
	}

	/**
	 * Mengembalikan nomor HP kedua ibu.
	 *
	 * <p>Dipetakan ke kolom {@code hp2ibu}.</p>
	 *
	 * <p>Getter membuang semua karakter selain angka dan titik dari nilai kolom sebelum
	 * mengembalikannya; hasilnya TIDAK ditulis balik ke field.</p>
	 *
	 * @return Nomor HP kedua ibu
	 */
	@Column(name = "hp2ibu")
	public String getHp2ibu() {
		return this.hp2ibu == null ? "" : hp2ibu.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP kedua ibu.
	 *
	 * @param hp2ibu nomor HP kedua ibu
	 */
	public void setHp2ibu(String hp2ibu) {
		this.hp2ibu = hp2ibu;
	}

	/**
	 * Mengembalikan nomor HP ketiga ayah.
	 *
	 * <p>Dipetakan ke kolom {@code hp3ayah}.</p>
	 *
	 * <p>Getter membuang semua karakter selain angka dan titik dari nilai kolom sebelum
	 * mengembalikannya; hasilnya TIDAK ditulis balik ke field.</p>
	 *
	 * @return Nomor HP ketiga ayah
	 */
	@Column(name = "hp3ayah")
	public String getHp3ayah() {
		return this.hp3ayah == null ? "" : hp3ayah.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP ketiga ayah.
	 *
	 * @param hp3ayah nomor HP ketiga ayah
	 */
	public void setHp3ayah(String hp3ayah) {
		this.hp3ayah = hp3ayah;
	}

	/**
	 * Mengembalikan nomor HP ketiga ibu.
	 *
	 * <p>Dipetakan ke kolom {@code hp3ibu}.</p>
	 *
	 * <p>Getter membuang semua karakter selain angka dan titik dari nilai kolom sebelum
	 * mengembalikannya; hasilnya TIDAK ditulis balik ke field.</p>
	 *
	 * @return Nomor HP ketiga ibu
	 */
	@Column(name = "hp3ibu")
	public String getHp3ibu() {
		return this.hp3ibu == null ? "" : hp3ibu.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP ketiga ibu.
	 *
	 * @param hp3ibu nomor HP ketiga ibu
	 */
	public void setHp3ibu(String hp3ibu) {
		this.hp3ibu = hp3ibu;
	}

	/**
	 * Mengembalikan jumlah saudara kandung.
	 *
	 * <p>Dipetakan ke kolom {@code jumlah_saudara_kandung}.</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code 0} pada nilai yang dikembalikan, tanpa menulis
	 * balik ke field.</p>
	 *
	 * @return Jumlah saudara kandung
	 */
	@Column(name = "jumlah_saudara_kandung")
	public Integer getJumlahSaudaraKandung() {
		return this.jumlahSaudaraKandung == null ? 0 : jumlahSaudaraKandung;
	}

	/**
	 * Menetapkan jumlah saudara kandung.
	 *
	 * @param jumlahSaudaraKandung jumlah saudara kandung
	 */
	public void setJumlahSaudaraKandung(Integer jumlahSaudaraKandung) {
		this.jumlahSaudaraKandung = jumlahSaudaraKandung;
	}

	/**
	 * Mengembalikan jumlah saudara tiri.
	 *
	 * <p>Dipetakan ke kolom {@code jumlah_saudara_tiri}.</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code 0} pada nilai yang dikembalikan, tanpa menulis
	 * balik ke field.</p>
	 *
	 * @return Jumlah saudara tiri
	 */
	@Column(name = "jumlah_saudara_tiri")
	public Integer getJumlahSaudaraTiri() {
		return this.jumlahSaudaraTiri == null ? 0 : jumlahSaudaraTiri;
	}

	/**
	 * Menetapkan jumlah saudara tiri.
	 *
	 * @param jumlahSaudaraTiri jumlah saudara tiri
	 */
	public void setJumlahSaudaraTiri(Integer jumlahSaudaraTiri) {
		this.jumlahSaudaraTiri = jumlahSaudaraTiri;
	}

	/**
	 * Mengembalikan kewarganegaraan calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code kewarganegaraan}.</p>
	 *
	 * @return Kewarganegaraan calon siswa
	 */
	@Column(name = "kewarganegaraan")
	public String getKewarganegaraan() {
		return this.kewarganegaraan;
	}

	/**
	 * Menetapkan kewarganegaraan calon siswa.
	 *
	 * @param kewarganegaraan kewarganegaraan calon siswa
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Mengembalikan kondisi khusus calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code kondisi_siswa}, panjang maksimum 12 karakter.</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan nilai bawaan {@code "Lain-nya"} (fallback baca-saja,
	 * tidak ditulis balik).</p>
	 *
	 * @return Kondisi khusus calon siswa
	 */
	@Column(name = "kondisi_siswa", length = 12)
	public String getKondisiSiswa() {
		return this.kondisiSiswa == null || kondisiSiswa.trim().isEmpty() ? "Lain-nya" : kondisiSiswa.trim();
	}

	/**
	 * Menetapkan kondisi khusus calon siswa.
	 *
	 * @param kondisiSiswa kondisi khusus calon siswa
	 */
	public void setKondisiSiswa(String kondisiSiswa) {
		this.kondisiSiswa = kondisiSiswa;
	}

	/**
	 * Mengembalikan nama panggilan calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code panggilan}.</p>
	 *
	 * @return Nama panggilan calon siswa
	 */
	@Column(name = "panggilan")
	public String getPanggilan() {
		return this.panggilan;
	}

	/**
	 * Menetapkan nama panggilan calon siswa.
	 *
	 * @param panggilan nama panggilan calon siswa
	 */
	public void setPanggilan(String panggilan) {
		this.panggilan = panggilan;
	}

	/**
	 * Mengembalikan pendidikan terakhir ayah.
	 *
	 * <p>Dipetakan ke kolom {@code pendidikan_ayah_id}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Pendidikan terakhir ayah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_ayah_id", nullable = true)
	public Pendidikan getPendidikanAyah() {
		pendidikanAyah = check(pendidikanAyah);
		return this.pendidikanAyah;
	}

	/**
	 * Menetapkan pendidikan terakhir ayah.
	 *
	 * @param pendidikanAyah pendidikan terakhir ayah
	 */
	public void setPendidikanAyah(Pendidikan pendidikanAyah) {
		this.pendidikanAyah = pendidikanAyah;
	}

	/**
	 * Mengembalikan pendidikan terakhir ibu.
	 *
	 * <p>Dipetakan ke kolom {@code pendidikan_ibu_id}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Pendidikan terakhir ibu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_ibu_id", nullable = true)
	public Pendidikan getPendidikanIbu() {
		pendidikanIbu = check(pendidikanIbu);
		return this.pendidikanIbu;
	}

	/**
	 * Menetapkan pendidikan terakhir ibu.
	 *
	 * @param pendidikanIbu pendidikan terakhir ibu
	 */
	public void setPendidikanIbu(Pendidikan pendidikanIbu) {
		this.pendidikanIbu = pendidikanIbu;
	}

	/**
	 * Mengembalikan rentang penghasilan ayah.
	 *
	 * <p>Dipetakan ke kolom {@code penghasilan_ortu_ayah_id}, relasi {@code @ManyToOne} yang dimuat
	 * malas ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Rentang penghasilan ayah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penghasilan_ortu_ayah_id", nullable = true)
	public PenghasilanOrangTuaSiswa getPenghasilanAyah() {
		penghasilanAyah = check(penghasilanAyah);
		return this.penghasilanAyah;
	}

	/**
	 * Menetapkan rentang penghasilan ayah.
	 *
	 * @param penghasilanAyah rentang penghasilan ayah
	 */
	public void setPenghasilanAyah(PenghasilanOrangTuaSiswa penghasilanAyah) {
		this.penghasilanAyah = penghasilanAyah;
	}

	/**
	 * Mengembalikan rentang penghasilan ibu.
	 *
	 * <p>Dipetakan ke kolom {@code penghasilan_ortu_ibu_id}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Rentang penghasilan ibu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penghasilan_ortu_ibu_id", nullable = true)
	public PenghasilanOrangTuaSiswa getPenghasilanIbu() {
		penghasilanIbu = check(penghasilanIbu);
		return this.penghasilanIbu;
	}

	/**
	 * Menetapkan rentang penghasilan ibu.
	 *
	 * @param penghasilanIbu rentang penghasilan ibu
	 */
	public void setPenghasilanIbu(PenghasilanOrangTuaSiswa penghasilanIbu) {
		this.penghasilanIbu = penghasilanIbu;
	}

	/**
	 * Mengembalikan riwayat penyakit calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code riwayat_penyakit}, panjang maksimum 2000 karakter.</p>
	 *
	 * <p>Data kesehatan anak; kolom teks sepanjang 2000 karakter.</p>
	 *
	 * @return Riwayat penyakit calon siswa
	 */
	@Column(name = "riwayat_penyakit", length = 2000)
	public String getRiwayatPenyakit() {
		return this.riwayatPenyakit;
	}

	/**
	 * Menetapkan riwayat penyakit calon siswa.
	 *
	 * @param riwayatPenyakit riwayat penyakit calon siswa
	 */
	public void setRiwayatPenyakit(String riwayatPenyakit) {
		this.riwayatPenyakit = riwayatPenyakit;
	}

	/**
	 * Mengembalikan status keikutsertaan calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code status_siswa}, panjang maksimum 9 karakter.</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan nilai bawaan {@code "Reguler"} (fallback baca-saja,
	 * tidak ditulis balik).</p>
	 *
	 * @return Status keikutsertaan calon siswa
	 */
	@Column(name = "status_siswa", length = 9)
	public String getStatusSiswa() {
		return this.statusSiswa == null || statusSiswa.trim().isEmpty() ? "Reguler" : statusSiswa.trim();
	}

	/**
	 * Menetapkan status keikutsertaan calon siswa.
	 *
	 * @param statusSiswa status keikutsertaan calon siswa
	 */
	public void setStatusSiswa(String statusSiswa) {
		this.statusSiswa = statusSiswa;
	}

	/**
	 * Tanggal lahir ayah (kolom {@code tanggal_lahir_ayah}).
	 *
	 * <p><b>Getter tulis-balik:</b> bila {@link #getOrangTuaPegawai()} terisi, berjenis kelamin
	 * laki-laki, dan punya tanggal lahir, nilai dari master kepegawaian MENIMPA kolom ini.</p>
	 *
	 * @return tanggal lahir ayah, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir_ayah", length = 29)
	public Date getTanggalLahirAyah() {
		if (getOrangTuaPegawai() != null && getOrangTuaPegawai().getKelamin() != null
				&& getOrangTuaPegawai().getKelamin().equalsIgnoreCase("Laki-laki")
				&& getOrangTuaPegawai().getTanggallahir() != null) {
			tanggalLahirAyah = getOrangTuaPegawai().getTanggallahir();
		}
		return this.tanggalLahirAyah;
	}

	/**
	 * Menetapkan tanggal lahir ayah.
	 *
	 * @param tanggalLahirAyah tanggal lahir ayah
	 */
	public void setTanggalLahirAyah(Date tanggalLahirAyah) {
		this.tanggalLahirAyah = tanggalLahirAyah;
	}

	/**
	 * Tanggal lahir ibu (kolom {@code tanggal_lahir_ibu}).
	 *
	 * <p><b>BUG NYATA &mdash; salah sasaran penulisan.</b> Cabang tulis-baliknya memeriksa pegawai
	 * yang BUKAN laki-laki (yaitu ibu), tetapi menuliskan hasilnya ke field
	 * {@code tanggalLahirAyah}, bukan {@code tanggalLahirIbu}. Akibatnya pada pendaftar jalur
	 * anak-pegawai yang orang tuanya perempuan: tanggal lahir AYAH tertimpa tanggal lahir ibu,
	 * sementara kolom tanggal lahir ibu sendiri tidak pernah terisi dari master kepegawaian.
	 * Getter tetap memulangkan {@code this.tanggalLahirIbu} yang tidak tersentuh, sehingga gejalanya
	 * muncul di kolom lain daripada yang sedang dibaca &mdash; sulit dilacak dari layar.</p>
	 *
	 * <p>Perilaku ini DIDOKUMENTASIKAN APA ADANYA; memperbaikinya mengubah data yang sudah terlanjur
	 * tersimpan pada instalasi berjalan, jadi perbaikan harus disertai skrip pemulihan.</p>
	 *
	 * @return tanggal lahir ibu sebagaimana tersimpan, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir_ibu", length = 29)
	public Date getTanggalLahirIbu() {
		if (getOrangTuaPegawai() != null && getOrangTuaPegawai().getKelamin() != null
				&& !getOrangTuaPegawai().getKelamin().equalsIgnoreCase("Laki-laki")
				&& getOrangTuaPegawai().getTanggallahir() != null) {
			tanggalLahirAyah = getOrangTuaPegawai().getTanggallahir();
		}
		return this.tanggalLahirIbu;
	}

	/**
	 * Menetapkan tanggal lahir ibu.
	 *
	 * @param tanggalLahirIbu tanggal lahir ibu
	 */
	public void setTanggalLahirIbu(Date tanggalLahirIbu) {
		this.tanggalLahirIbu = tanggalLahirIbu;
	}

	/**
	 * Tempat lahir ayah (kolom {@code tempat_lahir_ayah}).
	 *
	 * <p><b>Getter tulis-balik:</b> bila {@link #getOrangTuaPegawai()} terisi, berjenis kelamin
	 * laki-laki, dan tempat lahirnya tidak kosong, nilai dari master kepegawaian MENIMPA kolom ini.
	 * Berbeda dari {@link #getTanggalLahirIbu()}, pasangan tempat-lahir ini menulis ke field yang
	 * benar.</p>
	 *
	 * @return tempat lahir ayah, atau {@code null}
	 */
	@Column(name = "tempat_lahir_ayah")
	public String getTempatLahirAyah() {
		if (getOrangTuaPegawai() != null && getOrangTuaPegawai().getKelamin() != null
				&& getOrangTuaPegawai().getKelamin().equalsIgnoreCase("Laki-laki")
				&& getOrangTuaPegawai().getTempatlahir() != null
				&& !getOrangTuaPegawai().getTempatlahir().trim().isEmpty()) {
			tempatLahirAyah = getOrangTuaPegawai().getTempatlahir();
		}
		return this.tempatLahirAyah;
	}

	/**
	 * Menetapkan tempat lahir ayah.
	 *
	 * @param tempatLahirAyah tempat lahir ayah
	 */
	public void setTempatLahirAyah(String tempatLahirAyah) {
		this.tempatLahirAyah = tempatLahirAyah;
	}

	/**
	 * Tempat lahir ibu (kolom {@code tempat_lahir_ibu}).
	 *
	 * <p><b>Getter tulis-balik:</b> bila {@link #getOrangTuaPegawai()} terisi, BUKAN laki-laki, dan
	 * tempat lahirnya tidak kosong, nilai dari master kepegawaian MENIMPA kolom ini.</p>
	 *
	 * @return tempat lahir ibu, atau {@code null}
	 */
	@Column(name = "tempat_lahir_ibu")
	public String getTempatLahirIbu() {
		if (getOrangTuaPegawai() != null && getOrangTuaPegawai().getKelamin() != null
				&& !getOrangTuaPegawai().getKelamin().equalsIgnoreCase("Laki-laki")
				&& getOrangTuaPegawai().getTempatlahir() != null
				&& !getOrangTuaPegawai().getTempatlahir().trim().isEmpty()) {
			tempatLahirIbu = getOrangTuaPegawai().getTempatlahir();
		}
		return this.tempatLahirIbu;
	}

	/**
	 * Menetapkan tempat lahir ibu.
	 *
	 * @param tempatLahirIbu tempat lahir ibu
	 */
	public void setTempatLahirIbu(String tempatLahirIbu) {
		this.tempatLahirIbu = tempatLahirIbu;
	}

	/**
	 * Mengembalikan tinggi badan calon siswa dalam sentimeter.
	 *
	 * <p>Dipetakan ke kolom {@code tinggi_badan}.</p>
	 *
	 * @return Tinggi badan calon siswa dalam sentimeter
	 */
	@Column(name = "tinggi_badan")
	public Double getTinggi() {
		return this.tinggi;
	}

	/**
	 * Menetapkan tinggi badan calon siswa dalam sentimeter.
	 *
	 * @param tinggi tinggi badan calon siswa dalam sentimeter
	 */
	public void setTinggi(Double tinggi) {
		this.tinggi = tinggi;
	}

	/**
	 * Mengembalikan uraian hobi calon siswa dalam bentuk teks bebas.
	 *
	 * <p>Dipetakan ke kolom {@code hobby_s}, panjang maksimum 2000 karakter.</p>
	 *
	 * <p>Versi teks bebas 2000 karakter dari {@link #getHobby()}; keduanya dipelihara terpisah.</p>
	 *
	 * @return Uraian hobi calon siswa dalam bentuk teks bebas
	 */
	@Column(name = "hobby_s", length = 2000)
	public String getHobbyS() {
		return this.hobbyS;
	}

	/**
	 * Menetapkan uraian hobi calon siswa dalam bentuk teks bebas.
	 *
	 * @param hobbyS uraian hobi calon siswa dalam bentuk teks bebas
	 */
	public void setHobbyS(String hobbyS) {
		this.hobbyS = hobbyS;
	}

	/**
	 * Mengembalikan pendidikan terakhir wali.
	 *
	 * <p>Dipetakan ke kolom {@code pendidikan_wali_id_data}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * <p>Bila relasi pendidikan wali kosong, getter mengembalikan {@link #getPendidikanAyah()} sebagai
	 * fallback baca-saja.</p>
	 *
	 * @return Pendidikan terakhir wali
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_wali_id_data")
	public Pendidikan getPendidikanWali() {
		pendidikanWali = check(pendidikanWali);
		return this.pendidikanWali == null ? getPendidikanAyah() : pendidikanWali;
	}

	/**
	 * Menetapkan pendidikan terakhir wali.
	 *
	 * @param pendidikanWali pendidikan terakhir wali
	 */
	public void setPendidikanWali(Pendidikan pendidikanWali) {
		this.pendidikanWali = pendidikanWali;
	}

	/**
	 * Mengembalikan penghasilan ayah dalam bentuk teks bebas.
	 *
	 * <p>Dipetakan ke kolom {@code penghasilan_ayah_s}, panjang maksimum 30 karakter.</p>
	 *
	 * <p>Versi teks bebas dari {@link #getPenghasilanAyah()}; keduanya dipelihara terpisah.</p>
	 *
	 * @return Penghasilan ayah dalam bentuk teks bebas
	 */
	@Column(name = "penghasilan_ayah_s", length = 30)
	public String getPenghasilanAyahS() {
		return this.penghasilanAyahS;
	}

	/**
	 * Menetapkan penghasilan ayah dalam bentuk teks bebas.
	 *
	 * @param penghasilanAyahS penghasilan ayah dalam bentuk teks bebas
	 */
	public void setPenghasilanAyahS(String penghasilanAyahS) {
		this.penghasilanAyahS = penghasilanAyahS;
	}

	/**
	 * Mengembalikan penghasilan ibu dalam bentuk teks bebas.
	 *
	 * <p>Dipetakan ke kolom {@code penghasilan_ibu_s}, panjang maksimum 30 karakter.</p>
	 *
	 * <p>Versi teks bebas dari {@link #getPenghasilanIbu()}; keduanya dipelihara terpisah.</p>
	 *
	 * @return Penghasilan ibu dalam bentuk teks bebas
	 */
	@Column(name = "penghasilan_ibu_s", length = 30)
	public String getPenghasilanIbuS() {
		return this.penghasilanIbuS;
	}

	/**
	 * Menetapkan penghasilan ibu dalam bentuk teks bebas.
	 *
	 * @param penghasilanIbuS penghasilan ibu dalam bentuk teks bebas
	 */
	public void setPenghasilanIbuS(String penghasilanIbuS) {
		this.penghasilanIbuS = penghasilanIbuS;
	}

	/**
	 * Mengembalikan rentang penghasilan wali.
	 *
	 * <p>Dipetakan ke kolom {@code penghasilan_ortu_wali_id}, relasi {@code @ManyToOne} yang dimuat
	 * malas ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * <p>Bila relasi penghasilan wali kosong, getter mengembalikan {@link #getPenghasilanAyah()} sebagai
	 * fallback baca-saja.</p>
	 *
	 * @return Rentang penghasilan wali
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penghasilan_ortu_wali_id")
	public PenghasilanOrangTuaSiswa getPenghasilanWali() {
		penghasilanWali = check(penghasilanWali);
		return this.penghasilanWali == null ? getPenghasilanAyah() : penghasilanWali;
	}

	/**
	 * Menetapkan rentang penghasilan wali.
	 *
	 * @param penghasilanWali rentang penghasilan wali
	 */
	public void setPenghasilanWali(PenghasilanOrangTuaSiswa penghasilanWali) {
		this.penghasilanWali = penghasilanWali;
	}

	/**
	 * Mengembalikan NISN (nomor induk siswa nasional).
	 *
	 * <p>Dipetakan ke kolom {@code nomor_induk_nasional}, kolomnya {@code unique}.</p>
	 *
	 * <p>Getter memulangkan {@code null} (bukan string kosong) bila NISN kosong, karena kolomnya {@code
	 * unique} &mdash; beberapa baris berisi string kosong akan saling bertabrakan pada indeks unik,
	 * sedangkan beberapa baris {@code null} tidak.</p>
	 *
	 * @return NISN (nomor induk siswa nasional)
	 */
	@Column(name = "nomor_induk_nasional", unique = true)
	public String getNomorIndukNasional() {
		return this.nomorIndukNasional == null || this.nomorIndukNasional.trim().isEmpty() ? null
				: nomorIndukNasional.trim();
	}

	/**
	 * Menetapkan NISN (nomor induk siswa nasional).
	 *
	 * @param nomorIndukNasional NISN (nomor induk siswa nasional)
	 */
	public void setNomorIndukNasional(String nomorIndukNasional) {
		this.nomorIndukNasional = nomorIndukNasional;
	}

	/**
	 * Menetapkan kata sandi portal PSB milik calon siswa.
	 *
	 * @param pass kata sandi portal PSB milik calon siswa
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Kata sandi portal PSB milik calon siswa (kolom {@code pass}, panjang 100).
	 *
	 * <p><b>Getter DESTRUKTIF &mdash; mencetak kredensial pada jalur baca.</b> Bila kolom
	 * {@code pass} kosong dan {@link #getNomorIndukNasional() NISN} terisi, getter:</p>
	 * <ol>
	 * <li>mengisi {@code pass} dengan {@code Common.desEncrypter.get().encrypt(NISN)}, dan</li>
	 * <li>menyalakan {@link #getIs_encripted() is_encripted}.</li>
	 * </ol>
	 * <p>Keduanya menulis ke field pada objek terkelola, jadi ikut tersimpan. Sekadar merender satu
	 * halaman daftar calon siswa dapat menerbitkan kata sandi untuk banyak anak sekaligus, tanpa
	 * pernah diminta operator dan tanpa memberi tahu siapa pun.</p>
	 *
	 * <p><b>PERINGATAN KEAMANAN.</b> Kata sandi yang dicetak bukan hash melainkan ciphertext DES
	 * yang dapat dibalik, dan kunci DES ({@code Common.DES_PASS_PHRASE}) tertanam di kode sumber
	 * serta sama untuk seluruh instalasi AIS. Isi rahasianya pun bukan rahasia: NISN anak, yang
	 * tercetak di banyak dokumen sekolah. Konsekuensinya kata sandi portal setiap calon siswa dapat
	 * dihitung oleh siapa pun yang mengetahui NISN-nya, dan seluruh kolom {@code pass} dapat
	 * didekripsi massal oleh siapa pun yang pernah membaca kode ini.</p>
	 *
	 * @return kata sandi terenkripsi; {@code null} bila kolom kosong DAN NISN juga kosong
	 */
	@Column(name = "pass", nullable = true, length = 100)
	public String getPass() {
		if ((pass == null || pass.trim().isEmpty()) && getNomorIndukNasional() != null
				&& !getNomorIndukNasional().trim().isEmpty()) {
			pass = Common.desEncrypter.get().encrypt(getNomorIndukNasional().trim());
			is_encripted = true;
		}
		return pass;
	}

	/**
	 * Menetapkan nama pengguna akun orang tua.
	 *
	 * @param userOrtu nama pengguna akun orang tua
	 */
	public void setUserOrtu(String userOrtu) {
		this.userOrtu = userOrtu;
	}

	/**
	 * Mengembalikan nama pengguna akun orang tua.
	 *
	 * <p>Dipetakan ke kolom {@code user_ortu}.</p>
	 *
	 * <p>Getter mengembalikan string kosong (bukan {@code null}) bila kolom kosong.</p>
	 *
	 * @return Nama pengguna akun orang tua
	 */
	@Column(name = "user_ortu")
	public String getUserOrtu() {
		return userOrtu == null ? "" : userOrtu;
	}

	/**
	 * Menetapkan kata sandi akun orang tua.
	 *
	 * @param passOrtu kata sandi akun orang tua
	 */
	public void setPassOrtu(String passOrtu) {
		this.passOrtu = passOrtu;
	}

	/**
	 * Mengembalikan kata sandi akun orang tua.
	 *
	 * <p>Dipetakan ke kolom {@code pass_ortu}.</p>
	 *
	 * <p>Getter mengembalikan string kosong (bukan {@code null}) bila kolom kosong. Nilai disimpan apa
	 * adanya (tidak di-hash).</p>
	 *
	 * @return Kata sandi akun orang tua
	 */
	@Column(name = "pass_ortu")
	public String getPassOrtu() {
		return passOrtu == null ? "" : passOrtu;
	}

	/**
	 * Mengembalikan kode unik pendaftaran.
	 *
	 * <p>Dipetakan ke kolom {@code kode_uniq}, kolomnya {@code unique}.</p>
	 *
	 * <p>Kolom {@code unique}. Dipakai sebagai kunci idempoten pada beberapa integrasi luar.</p>
	 *
	 * @return Kode unik pendaftaran
	 */
	@Column(name = "kode_uniq", unique = true)
	public String getKodeUniq() {
		return this.kodeUniq;
	}

	/**
	 * Menetapkan kode unik pendaftaran.
	 *
	 * @param kodeUniq kode unik pendaftaran
	 */
	public void setKodeUniq(String kodeUniq) {
		this.kodeUniq = kodeUniq;
	}

	/**
	 * Mengembalikan penanda baris pendaftaran masih aktif.
	 *
	 * <p>Dipetakan ke kolom {@code aktif}.</p>
	 *
	 * <p>Getter mengembalikan nilai apa adanya, termasuk {@code null} &mdash; pemanggil harus menangani
	 * tiga keadaan ({@code true}/{@code false}/{@code null}), berbeda dari kebanyakan boolean lain pada
	 * kelas ini.</p>
	 *
	 * @return Penanda baris pendaftaran masih aktif
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return this.aktif;
	}

	/**
	 * Menetapkan penanda baris pendaftaran masih aktif.
	 *
	 * @param aktif penanda baris pendaftaran masih aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan catatan bebas panitia atas pendaftaran ini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengembalikan string kosong (bukan {@code null}) bila kolom kosong. Properti ini TIDAK
	 * dianotasi {@code @Column}, sehingga nama kolomnya sama dengan nama properti.</p>
	 *
	 * @return Catatan bebas panitia atas pendaftaran ini
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Menetapkan catatan bebas panitia atas pendaftaran ini.
	 *
	 * @param keterangan catatan bebas panitia atas pendaftaran ini
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Negara asal calon siswa (kolom {@code negara_id}).
	 *
	 * <p>Bila relasi kosong, getter mengembalikan konstanta
	 * {@link ais.common.ConstantValues#INDONESIA}. Fallback itu HANYA pada nilai kembalian &mdash;
	 * field dan kolomnya tetap {@code null}, sehingga kueri SQL yang menyaring
	 * {@code negara_id = <id Indonesia>} tidak akan menemukan baris-baris tersebut meskipun getter
	 * menyatakan Indonesia.</p>
	 *
	 * @return negara asal; tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara_id")
	public Negara getNegara() {
		negara = check(negara);
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/**
	 * Menetapkan negara asal calon siswa.
	 *
	 * @param negara negara asal calon siswa
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Menetapkan penanda bahwa kolom kata sandi sudah berisi ciphertext.
	 *
	 * @param is_encripted penanda bahwa kolom kata sandi sudah berisi ciphertext
	 */
	public void setIs_encripted(Boolean is_encripted) {
		this.is_encripted = is_encripted;
	}

	/**
	 * Penanda bahwa kolom {@link #getPass()} sudah berisi ciphertext, bukan teks polos.
	 *
	 * <p><b>Getter tulis-balik:</b> nilai {@code null} diubah menjadi {@code false} pada field,
	 * sehingga membaca baris lama menandainya sebagai berubah. Nilai ini juga disetel
	 * {@code true} sebagai efek samping {@link #getPass()}.</p>
	 *
	 * <p>Nama properti memakai gaya {@code snake_case} yang tidak lazim di kelas ini; itu disengaja
	 * agar cocok dengan kolom yang sudah ada di basis data lama. Jangan diganti.</p>
	 *
	 * @return {@code true} bila kata sandi tersimpan terenkripsi; tidak pernah {@code null}
	 */
	public Boolean getIs_encripted() {
		if (is_encripted == null) {
			is_encripted = false;
		}
		return is_encripted;
	}

	/**
	 * Mengembalikan tempat lahir wali.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Bila kosong, getter mengembalikan {@link #getTempatLahirAyah()} sebagai fallback baca-saja.</p>
	 *
	 * @return Tempat lahir wali
	 */
	public String getTempatLahirWali() {
		return tempatLahirWali == null || tempatLahirWali.trim().isEmpty() ? getTempatLahirAyah() : tempatLahirWali;
	}

	/**
	 * Menetapkan tempat lahir wali.
	 *
	 * @param tempatLahirWali tempat lahir wali
	 */
	public void setTempatLahirWali(String tempatLahirWali) {
		this.tempatLahirWali = tempatLahirWali;
	}

	/**
	 * Mengembalikan tanggal lahir wali.
	 *
	 * <p>Dipetakan ke kolom {@code tanggal_lahir_wali}, disimpan sebagai tanggal tanpa jam.</p>
	 *
	 * <p>Bila kosong, getter mengembalikan {@link #getTanggalLahirAyah()} sebagai fallback
	 * baca-saja.</p>
	 *
	 * @return Tanggal lahir wali
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir_wali")
	public Date getTanggalLahirWali() {
		return tanggalLahirWali == null ? getTanggalLahirAyah() : tanggalLahirWali;
	}

	/**
	 * Menetapkan tanggal lahir wali.
	 *
	 * @param tanggalLahirWali tanggal lahir wali
	 */
	public void setTanggalLahirWali(Date tanggalLahirWali) {
		this.tanggalLahirWali = tanggalLahirWali;
	}

	/**
	 * Mengembalikan nomor HP utama wali.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Bila kosong, getter mengembalikan {@link #getHp1ayah()}. Nilai yang dikembalikan selalu
	 * dibersihkan menjadi angka saja.</p>
	 *
	 * @return Nomor HP utama wali
	 */
	public String getHp1wali() {
		return hp1wali == null || hp1wali.trim().isEmpty() ? getHp1ayah() : hp1wali.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP utama wali.
	 *
	 * @param hp1wali nomor HP utama wali
	 */
	public void setHp1wali(String hp1wali) {
		this.hp1wali = hp1wali;
	}

	/**
	 * Mengembalikan nomor HP kedua wali.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Bila kosong, getter mengembalikan {@link #getHp2ayah()}. Nilai yang dikembalikan selalu
	 * dibersihkan menjadi angka saja.</p>
	 *
	 * @return Nomor HP kedua wali
	 */
	public String getHp2wali() {
		return hp2wali == null || hp2wali.trim().isEmpty() ? getHp2ayah() : hp2wali.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP kedua wali.
	 *
	 * @param hp2wali nomor HP kedua wali
	 */
	public void setHp2wali(String hp2wali) {
		this.hp2wali = hp2wali;
	}

	/**
	 * Mengembalikan nomor HP ketiga wali.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Bila kosong, getter mengembalikan {@link #getHp3ayah()}. Nilai yang dikembalikan selalu
	 * dibersihkan menjadi angka saja.</p>
	 *
	 * @return Nomor HP ketiga wali
	 */
	public String getHp3wali() {
		return hp3wali == null || hp3wali.trim().isEmpty() ? getHp3ayah() : hp3wali.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor HP ketiga wali.
	 *
	 * @param hp3wali nomor HP ketiga wali
	 */
	public void setHp3wali(String hp3wali) {
		this.hp3wali = hp3wali;
	}

	/**
	 * Penanda calon siswa sudah dinyatakan DITERIMA.
	 *
	 * <p><b>Getter tulis-balik yang memutuskan kelulusan.</b> Dua cabangnya:</p>
	 * <ol>
	 * <li>Bila {@link #getMengundurkanDiri()} bernilai {@code true}, penanda DIPAKSA
	 * {@code false} &mdash; pengunduran diri selalu mengalahkan penerimaan.</li>
	 * <li>Bila gelombang pendaftaran menyalakan
	 * {@code otomatisDiterimaKetikaSudahBayarReg} DAN
	 * {@link #getRiwayatPembayaranPendaftaran()} tidak kosong, penanda DIPAKSA {@code true}.</li>
	 * </ol>
	 *
	 * <p>Keduanya menulis ke field. Artinya keputusan penerimaan seorang anak dapat berubah semata
	 * karena sebuah laporan, dasbor, atau API membaca baris ini &mdash; tanpa tombol, tanpa
	 * konfirmasi, dan (bila terjadi di luar transaksi ORM biasa) tanpa jejak Envers. Perubahan ini
	 * juga berantai: {@link #populate(CalonSiswa)} membaca hasilnya untuk memutuskan apakah harus
	 * menerbitkan NIS lewat {@code CommonPSB.onGenerateNis} dan memasukkan siswa ke kelas.</p>
	 *
	 * @return {@code true} bila calon siswa diterima; tidak pernah {@code null}
	 */
	public Boolean getTelahDiterima() {

		if (getMengundurkanDiri()) {
			telahDiterima = false;
		}

		if (getGelombangPendaftaranPsb() != null
				&& getGelombangPendaftaranPsb().getOtomatisDiterimaKetikaSudahBayarReg()
				&& !getRiwayatPembayaranPendaftaran().isEmpty()) {
			telahDiterima = true;
		}

		return telahDiterima == null ? false : telahDiterima;
	}

	/**
	 * Menetapkan penanda calon siswa sudah dinyatakan diterima.
	 *
	 * @param telahDiterima penanda calon siswa sudah dinyatakan diterima
	 */
	public void setTelahDiterima(Boolean telahDiterima) {
		this.telahDiterima = telahDiterima;
	}

	/**
	 * Mengembalikan kelompok pendaftaran (pengelompokan administratif dalam satu gelombang).
	 *
	 * <p>Dipetakan ke kolom {@code kelompok_pendaftaran_psb}, relasi {@code @ManyToOne} yang dimuat
	 * malas ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Kelompok pendaftaran (pengelompokan administratif dalam satu gelombang)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pendaftaran_psb", nullable = true)
	public KelompokPendaftaranPsb getKelompokPendaftaranPsb() {
		kelompokPendaftaranPsb = check(kelompokPendaftaranPsb);
		return kelompokPendaftaranPsb;
	}

	/**
	 * Menetapkan kelompok pendaftaran (pengelompokan administratif dalam satu gelombang).
	 *
	 * @param kelompokPendaftaranPsb kelompok pendaftaran (pengelompokan administratif dalam satu
	 * gelombang)
	 */
	public void setKelompokPendaftaranPsb(KelompokPendaftaranPsb kelompokPendaftaranPsb) {
		this.kelompokPendaftaranPsb = kelompokPendaftaranPsb;
	}

	/**
	 * Baris {@link Siswa} aktif yang terbit dari pendaftaran ini (kolom {@code siswa_id},
	 * {@code unique}).
	 *
	 * <p>Terisi setelah {@code CommonPSB.onGenerateNis} menerbitkan NIS dan membuat baris siswa.
	 * Selama masih {@code null}, calon siswa belum menjadi siswa.</p>
	 *
	 * <p><b>Getter tulis-balik dengan penebakan heuristik.</b> Bila {@link #getTelahDiterima()}
	 * bernilai {@code true} tetapi relasinya masih {@code null}, getter menyisir SELURUH cache
	 * {@link Siswa} milik instalasi dan menautkan baris pertama yang cocok pada tiga hal sekaligus:
	 * tahun masuk sama, nama siswa sama (tidak peka huruf besar/kecil), dan tanggal lahir sama
	 * (dibandingkan sebagai teks). Tautan hasil tebakan itu ditulis ke field dan ikut tersimpan.</p>
	 *
	 * <p><b>Risiko nyata:</b> dua anak dengan nama sama yang lahir pada hari yang sama dalam satu
	 * angkatan &mdash; kembar dengan nama mirip, atau nama yang sangat umum &mdash; akan TERTAUT
	 * SILANG secara permanen. Karena penyisirannya memakai cache global lintas tenant
	 * ({@code ConstantValues.ambilBerdasarClass}), pasangan yang keliru bahkan bisa berasal dari
	 * SEKOLAH LAIN dalam instalasi yang sama. Kolom {@code siswa_id} bersifat {@code unique},
	 * sehingga tabrakan berikutnya baru muncul sebagai kegagalan constraint di tempat lain.</p>
	 *
	 * <p>Exception di dalam loop ditelan (hanya dicatat ke audit error), jadi kegagalan sebagian
	 * tidak terlihat.</p>
	 *
	 * @return baris siswa yang bertaut, atau {@code null} bila belum ada dan tidak ada tebakan yang
	 * cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = true, unique = true)
	public Siswa getSiswa() {
		siswa = check(siswa);

		if (getTelahDiterima() && siswa == null) {
			for (Object o : ConstantValues.ambilBerdasarClass(Siswa.class).values()) {
				try {
					Siswa s = (Siswa) o;
					if (s != null && s.getTanggalLahir() != null && getTanggalLahir() != null
							&& s.getTahunMasuk().equals(getTahunMasuk())
							&& s.getNamaSiswa().equalsIgnoreCase(getNamaSiswa())
							&& Common.dateFormat8.get().format(s.getTanggalLahir())
									.equalsIgnoreCase(Common.dateFormat8.get().format(getTanggalLahir()))) {
						siswa = s;
						break;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:1367");
					// TODO: handle exception
				}
			}
		}

		return siswa;
	}

	/**
	 * Menetapkan baris siswa aktif yang terbit dari pendaftaran ini.
	 *
	 * @param siswa baris siswa aktif yang terbit dari pendaftaran ini
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan penanda calon siswa menyetujui surat pernyataan pendaftaran.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false} pada nilai yang dikembalikan.</p>
	 *
	 * @return Penanda calon siswa menyetujui surat pernyataan pendaftaran
	 */
	public Boolean getPernyataan() {
		return pernyataan == null ? false : pernyataan;
	}

	/**
	 * Menetapkan penanda calon siswa menyetujui surat pernyataan pendaftaran.
	 *
	 * @param pernyataan penanda calon siswa menyetujui surat pernyataan pendaftaran
	 */
	public void setPernyataan(Boolean pernyataan) {
		this.pernyataan = pernyataan;
	}

	/**
	 * Mengembalikan jawaban parameter tambahan dalam bentuk berlabel.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), bertipe {@code text}.</p>
	 *
	 * <p>Getter MENULIS BALIK string kosong ke field bila kolomnya {@code null}, sehingga membaca baris
	 * lama dapat menandainya sebagai berubah. Format isinya: satu baris per jawaban, kolom dipisah
	 * {@code &lt;=&gt;} dengan urutan label, nilai, url lampiran, nomor urut, id parameter, id kelompok,
	 * keterangan. Lihat {@link #ambilDataParameterTambahan()}.</p>
	 *
	 * @return Jawaban parameter tambahan dalam bentuk berlabel
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}
		return parameterTambahan;
	}

	/**
	 * Menetapkan jawaban parameter tambahan dalam bentuk berlabel.
	 *
	 * @param parameterTambahan jawaban parameter tambahan dalam bentuk berlabel
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan jawaban parameter tambahan dalam bentuk berbasis id.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), bertipe {@code text}.</p>
	 *
	 * <p>Getter MENULIS BALIK string kosong ke field bila kolomnya {@code null}. Format isinya: satu
	 * baris per jawaban, {@code
	 * <idKelompok>-&gt;<idParameter>&lt;=&gt;nilai&lt;=&gt;url&lt;=&gt;keterangan}. Ini varian berbasis
	 * id dari {@link #getParameterTambahan()} yang tahan terhadap perubahan label.</p>
	 *
	 * @return Jawaban parameter tambahan dalam bentuk berbasis id
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}
		return parameterTambahanInds;
	}

	/**
	 * Menetapkan jawaban parameter tambahan dalam bentuk berbasis id.
	 *
	 * @param parameterTambahanInds jawaban parameter tambahan dalam bentuk berbasis id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} menjadi daftar objek pandang yang siap dirender.
	 *
	 * <p>Format sumbernya: satu jawaban per baris, kolom dipisah penanda {@code &lt;=&gt;} dengan
	 * urutan <i>label</i>, <i>nilai</i>, <i>url lampiran</i>, <i>nomor urut</i>, <i>id parameter</i>,
	 * <i>id kelompok</i>, <i>keterangan</i>. Method ini hanya memakai lima kolom pertama dan
	 * memetakannya ke {@link ais.database.model.CommonVO}: {@code id} &larr; id parameter,
	 * {@code name} &larr; label, {@code name1} &larr; nilai, {@code name2} &larr; url lampiran,
	 * {@code nomorUrut} &larr; nomor urut.</p>
	 *
	 * <p>Hasilnya diurutkan memakai {@code Collections.sort} sehingga mengikuti urutan alami
	 * {@code CommonVO} (berdasarkan {@code nomorUrut}).</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b></p>
	 * <ul>
	 * <li>Kegagalan parsing angka ditelan dan diganti nilai bawaan ({@code nomorUrut = 1},
	 * {@code id = 1}), sehingga data rusak muncul sebagai baris yang tampak sah dengan id
	 * parameter {@code 1} &mdash; bukan sebagai error.</li>
	 * <li>Bila {@link #getParameterTambahan()} kosong, {@code String.split} tetap menghasilkan satu
	 * elemen kosong, sehingga daftar yang dikembalikan berisi SATU baris kosong, bukan daftar
	 * kosong. Pemanggil harus menyaringnya sendiri.</li>
	 * </ul>
	 *
	 * @return daftar objek pandang parameter tambahan, terurut; tidak pernah {@code null}
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:1426");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:1432");

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
	 * Membaca kembali nilai yang diisi pengguna pada baris-baris formulir ZK dan menyusunnya
	 * menjadi kedua kolom teks parameter tambahan.
	 *
	 * <p>Dipanggil dari layar biodata/formulir PSB ({@code CalonSiswaAction},
	 * {@code ParameterTambahanPsbListener}, dan formulir portal) saat pengguna menekan Simpan.
	 * Untuk setiap {@link org.zkoss.zul.Row} diambil atribut {@code "parameterTambahan"} dan
	 * {@code "kelompokParameterTambahanCalonSiswa"} yang sebelumnya dilekatkan saat merender baris,
	 * lalu nilainya dibaca lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)}.</p>
	 *
	 * <p>Bila parameter menandai {@code harusMenyertakanLampiran}, method mencari
	 * {@link ais.database.model.file.LampiranLain} dengan kunci ({@link #getId()},
	 * {@code "<idKelompok>-><idParameter>"}) dan menyisipkan tautan unduhnya ke dalam string.</p>
	 *
	 * <p>Dua string dibangun sekaligus lalu disimpan lewat {@link #setParameterTambahan(String)} dan
	 * {@link #setParameterTambahanInds(String)}: yang pertama berbasis LABEL (mudah dibaca manusia,
	 * tetapi rusak bila label parameter diubah), yang kedua berbasis ID (stabil).</p>
	 *
	 * <p><b>Perilaku penting:</b> daftar {@code null} atau kosong menyebabkan method keluar tanpa
	 * melakukan apa pun &mdash; jawaban lama TIDAK terhapus. Sebaliknya, daftar yang tidak kosong
	 * selalu MENIMPA seluruh isi kedua kolom; baris yang gagal diproses (exception-nya hanya
	 * ditampilkan kepada admin) hilang diam-diam dari hasil.</p>
	 *
	 * <p><b>Catatan keamanan:</b> tautan lampiran yang disematkan di sini ikut terbawa ke halaman
	 * portal {@code _sukses_login.jsp} yang terjangkau tanpa login &mdash; lihat peringatan pada
	 * Javadoc kelas.</p>
	 *
	 * @param parameterRows baris-baris formulir ZK yang membawa atribut parameter tambahan; bila
	 * {@code null} atau kosong, method tidak melakukan apa pun
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
				KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa = (KelompokParameterTambahanCalonSiswa) row
						.getAttribute("kelompokParameterTambahanCalonSiswa");
				if (parameterTambahan != null && kelompokParameterTambahanCalonSiswa != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CalonSiswa.class, getId(),
							kelompokParameterTambahanCalonSiswa.getId() + "->" + parameterTambahan.getId());

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

					String s = kelompokParameterTambahanCalonSiswa.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCalonSiswa.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCalonSiswa.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
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

	/**
	 * Mengembalikan PIN / kata sandi portal PSB yang dibagikan panitia.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Dipakai {@code LoginCalonSiswaAction} sebagai PIN portal, dibandingkan dengan {@code
	 * Restrictions.ilike(&hellip;, MatchMode.EXACT)} &mdash; perbandingan LIKE yang TIDAK meng-escape
	 * metakarakter, sehingga nilai {@code %} cocok dengan PIN apa pun. Lihat peringatan keamanan pada
	 * Javadoc kelas.</p>
	 *
	 * @return PIN / kata sandi portal PSB yang dibagikan panitia
	 */
	public String getPinPassword() {
		return pinPassword;
	}

	/**
	 * Menetapkan PIN / kata sandi portal PSB yang dibagikan panitia.
	 *
	 * @param pinPassword PIN / kata sandi portal PSB yang dibagikan panitia
	 */
	public void setPinPassword(String pinPassword) {
		this.pinPassword = pinPassword;
	}

	/**
	 * Penanda calon siswa pernah masuk ke portal PSB.
	 *
	 * <p><b>Getter tulis-balik dengan inferensi.</b> Bila {@link #getParameterTambahan()} tidak
	 * kosong, penanda DIPAKSA {@code true} &mdash; asumsinya hanya calon siswa sendiri yang bisa
	 * mengisi parameter tambahan. Asumsi itu tidak selalu benar: panitia juga dapat mengisi
	 * parameter tambahan dari layar admin, dan sejak itu baris tersebut selamanya tercatat "pernah
	 * login" meskipun anaknya belum pernah membuka portal. Laporan tingkat partisipasi portal karena
	 * itu melebih-lebihkan angkanya.</p>
	 *
	 * <p>Nilai {@code null} juga ditulis menjadi {@code false}.</p>
	 *
	 * @return {@code true} bila calon siswa dianggap pernah masuk portal; tidak pernah {@code null}
	 */
	public Boolean getTelahLogin() {

		if (!getParameterTambahan().trim().isEmpty()) {
			telahLogin = true;
		}

		if (telahLogin == null) {
			telahLogin = false;
		}
		return telahLogin;
	}

	/**
	 * Menetapkan penanda calon siswa pernah masuk ke portal PSB.
	 *
	 * @param telahLogin penanda calon siswa pernah masuk ke portal PSB
	 */
	public void setTelahLogin(Boolean telahLogin) {
		this.telahLogin = telahLogin;
	}

	/**
	 * Mengembalikan waktu calon siswa terakhir masuk ke portal PSB.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Diisi {@code LoginCalonSiswaAction} setiap kali calon siswa berhasil masuk portal.</p>
	 *
	 * @return Waktu calon siswa terakhir masuk ke portal PSB
	 */
	public Date getWaktuLogin() {
		return waktuLogin;
	}

	/**
	 * Menetapkan waktu calon siswa terakhir masuk ke portal PSB.
	 *
	 * @param waktuLogin waktu calon siswa terakhir masuk ke portal PSB
	 */
	public void setWaktuLogin(Date waktuLogin) {
		this.waktuLogin = waktuLogin;
	}

	/**
	 * Pencacah pencetakan kartu pendaftaran.
	 *
	 * <p><b>BUG NYATA:</b> implementasinya {@code return cetakKartu == null ? 0 : 1;} &mdash; nilai
	 * apa pun yang bukan {@code null} dipetakan menjadi {@code 1}, termasuk {@code 0} dan
	 * {@code 17}. Akibatnya properti ini sama sekali bukan pencacah melainkan penanda biner "pernah
	 * disentuh", dan {@link #setCetakKartu(Integer)} yang menaikkan angka tidak pernah terlihat
	 * hasilnya. Laporan "berapa kali kartu dicetak" tidak pernah bisa melampaui 1.</p>
	 *
	 * <p>Perilaku ini didokumentasikan apa adanya; mengubahnya akan mengubah tampilan laporan yang
	 * sudah berjalan.</p>
	 *
	 * @return {@code 0} bila kolom {@code null}, selain itu selalu {@code 1}
	 */
	public Integer getCetakKartu() {
		return cetakKartu == null ? 0 : 1;
	}

	/**
	 * Menetapkan pencacah pencetakan kartu pendaftaran.
	 *
	 * @param cetakKartu pencacah pencetakan kartu pendaftaran
	 */
	public void setCetakKartu(Integer cetakKartu) {
		this.cetakKartu = cetakKartu;
	}

	/**
	 * Mengembalikan tanggal calon siswa mendaftar.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), disimpan sebagai stempel waktu lengkap.</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan waktu saat ini sebagai pengganti (fallback
	 * baca-saja).</p>
	 *
	 * @return Tanggal calon siswa mendaftar
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPendaftaran() {
		return tanggalPendaftaran == null ? ais.ui.util.WaktuUtil.getDate() : tanggalPendaftaran;
	}

	/**
	 * Menetapkan tanggal calon siswa mendaftar.
	 *
	 * @param tanggalPendaftaran tanggal calon siswa mendaftar
	 */
	public void setTanggalPendaftaran(Date tanggalPendaftaran) {
		this.tanggalPendaftaran = tanggalPendaftaran;
	}

	/**
	 * Mengembalikan nama dusun pada alamat calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Nama dusun pada alamat calon siswa
	 */
	public String getDusunCalon() {
		return dusunCalon;
	}

	/**
	 * Menetapkan nama dusun pada alamat calon siswa.
	 *
	 * @param dusunCalon nama dusun pada alamat calon siswa
	 */
	public void setDusunCalon(String dusunCalon) {
		this.dusunCalon = dusunCalon;
	}

	/**
	 * Mengembalikan nomor RT pada alamat calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Nomor RT pada alamat calon siswa
	 */
	public String getRt() {
		return rt;
	}

	/**
	 * Menetapkan nomor RT pada alamat calon siswa.
	 *
	 * @param rt nomor RT pada alamat calon siswa
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Mengembalikan nomor RW pada alamat calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Nomor RW pada alamat calon siswa
	 */
	public String getRw() {
		return rw;
	}

	/**
	 * Menetapkan nomor RW pada alamat calon siswa.
	 *
	 * @param rw nomor RW pada alamat calon siswa
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Mengembalikan kode pos alamat calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Kode pos alamat calon siswa
	 */
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * Menetapkan kode pos alamat calon siswa.
	 *
	 * @param kodePos kode pos alamat calon siswa
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Mengembalikan nama desa/kelurahan alamat calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Nama desa/kelurahan alamat calon siswa
	 */
	public String getKelurahanCalon() {
		return kelurahanCalon;
	}

	/**
	 * Menetapkan nama desa/kelurahan alamat calon siswa.
	 *
	 * @param kelurahanCalon nama desa/kelurahan alamat calon siswa
	 */
	public void setKelurahanCalon(String kelurahanCalon) {
		this.kelurahanCalon = kelurahanCalon;
	}

	/**
	 * Kecamatan pada alamat calon siswa (kolom {@code kecamatan_calon_wilayah}).
	 *
	 * <p><b>Getter tulis-balik yang melakukan koreksi hierarki wilayah.</b> Bila wilayah yang
	 * tersimpan tidak punya {@code wilayahInduk} &mdash; gejala baris katalog wilayah yang belum
	 * lengkap &mdash; getter menyisir SELURUH cache {@link ais.database.model.Wilayah} mencari baris
	 * lain dengan kode {@code feeder} yang SAMA tetapi sudah punya induk, lalu menukar relasinya ke
	 * baris itu dan menuliskannya ke field.</p>
	 *
	 * <p>Ini memperbaiki data secara diam-diam pada jalur baca. Efek sampingnya: bila katalog
	 * wilayah punya dua baris berkode feeder sama (duplikat hasil impor), pasangan yang dipilih
	 * adalah yang pertama ditemui pada iterasi cache &mdash; tidak deterministik antar-restart.</p>
	 *
	 * @return kecamatan alamat calon siswa, atau {@code null}
	 */
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

	/**
	 * Menetapkan kecamatan alamat calon siswa.
	 *
	 * @param kecamatan kecamatan alamat calon siswa
	 */
	public void setKecamatanCalon(Wilayah kecamatan) {
		this.kecamatanCalon = kecamatan;
	}

	/**
	 * Mengembalikan propinsi alamat calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code propinsi_calon}, relasi {@code @ManyToOne} yang dimuat malas ({@code
	 * LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Propinsi alamat calon siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi_calon", nullable = true)
	public Propinsi getPropinsiCalon() {
		propinsiCalon = check(propinsiCalon);
		return propinsiCalon;
	}

	/**
	 * Menetapkan propinsi alamat calon siswa.
	 *
	 * @param propinsiCalon propinsi alamat calon siswa
	 */
	public void setPropinsiCalon(Propinsi propinsiCalon) {
		this.propinsiCalon = propinsiCalon;
	}

	/**
	 * Mengembalikan kabupaten/kota alamat calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code kota_calon}, relasi {@code @ManyToOne} yang dimuat malas ({@code
	 * LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Kabupaten/kota alamat calon siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_calon", nullable = true)
	public Kota getKotaCalon() {
		kotaCalon = check(kotaCalon);
		return kotaCalon;
	}

	/**
	 * Menetapkan kabupaten/kota alamat calon siswa.
	 *
	 * @param kotaCalon kabupaten/kota alamat calon siswa
	 */
	public void setKotaCalon(Kota kotaCalon) {
		this.kotaCalon = kotaCalon;
	}

	/**
	 * Alamat sekolah asal.
	 *
	 * <p><b>Getter tulis-balik</b> dengan pola yang sama seperti {@link #getSekolahAsal()}: bila
	 * gelombang menunjuk {@code alumniDari}, alamat sekolah tersebut menimpa nilai kolom.</p>
	 *
	 * <p>Properti ini tidak dianotasi {@code @Column}, sehingga nama kolomnya sama dengan nama
	 * properti.</p>
	 *
	 * @return alamat sekolah asal, atau {@code null}
	 */
	public String getAlamatSekolahAsal() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getAlumniDari() != null) {
			alamatSekolahAsal = gelombangPendaftaranPsb.getAlumniDari().getAlamat();
		}
		return alamatSekolahAsal;
	}

	/**
	 * Menetapkan alamat sekolah asal.
	 *
	 * @param alamatSekolahAsal alamat sekolah asal
	 */
	public void setAlamatSekolahAsal(String alamatSekolahAsal) {
		this.alamatSekolahAsal = alamatSekolahAsal;
	}

	/**
	 * Mengembalikan nomor peserta ujian seleksi PSB.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Dipakai {@code LoginCalonSiswaAction} sebagai salah satu identitas login portal, dan dicetak
	 * pada kartu ujian oleh {@code _cetak_kartu_ujian.jsp}.</p>
	 *
	 * @return Nomor peserta ujian seleksi PSB
	 */
	public String getNoUjian() {
		return noUjian;
	}

	/**
	 * Menetapkan nomor peserta ujian seleksi PSB.
	 *
	 * @param noUjian nomor peserta ujian seleksi PSB
	 */
	public void setNoUjian(String noUjian) {
		this.noUjian = noUjian;
	}

	/**
	 * Nomor registrasi pendaftaran &mdash; satu-satunya properti yang MENULIS ke kolom
	 * {@code nomor_induk} ({@code nullable = false}).
	 *
	 * <p>Nilainya dibangkitkan oleh generator nomor registrasi di
	 * {@code ais.action.master.sekolah.psb.noreg} saat baris pendaftaran dibuat, dicetak pada kartu
	 * pendaftaran, dan dipakai sebagai identitas login portal PSB oleh
	 * {@code LoginCalonSiswaAction}.</p>
	 *
	 * <p>Tiga properti lain memetakan kolom yang sama secara baca-saja:
	 * {@link #getNomorInduk()}, {@link #getNopendaftaran()}, dan (lewat {@link #getNama()} yang
	 * setara di kolom nama) pola alias serupa. Bila unggahan Excel menyertakan salah satu kolom
	 * alias itu, nilainya akan diabaikan tanpa peringatan.</p>
	 *
	 * @return nomor registrasi pendaftaran, atau {@code null} bila belum dibangkitkan
	 */
	@Column(name = "nomor_induk", nullable = false)
	public String getNoRegistrasi() {
		return noRegistrasi;
	}

	/**
	 * Menetapkan nomor registrasi pendaftaran.
	 *
	 * @param noRegistrasi nomor registrasi pendaftaran
	 */
	public void setNoRegistrasi(String noRegistrasi) {
		this.noRegistrasi = noRegistrasi;
	}

	/**
	 * Penjurusan/program keahlian yang dipilih (kolom {@code penjurusan_sekolah_id}).
	 *
	 * <p><b>Getter tulis-balik berprioritas.</b> Urutan sumbernya: (1) penjurusan pada
	 * {@link #getSiswa()} bila calon sudah menjadi siswa, (2) penjurusan pada
	 * {@link #getGelombangPendaftaranPsb()}, (3) nilai kolom sendiri. Dua sumber pertama MENIMPA
	 * field, sehingga pilihan penjurusan yang diisi pendaftar dapat tergantikan oleh penjurusan
	 * bawaan gelombang.</p>
	 *
	 * <p>Bila yang dibutuhkan adalah nilai kolom APA ADANYA (mis. untuk membandingkan pilihan
	 * pendaftar dengan penempatan akhir), pakai {@link #ambilPenjurusanSekolah()}.</p>
	 *
	 * @return penjurusan efektif calon siswa, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penjurusan_sekolah_id", nullable = true)
	public PenjurusanSekolah getPenjurusanSekolah() {
		if (getSiswa() != null && getSiswa().getPenjurusanSekolah() != null) {
			penjurusanSekolah = getSiswa().getPenjurusanSekolah();
		} else if (getGelombangPendaftaranPsb() != null
				&& getGelombangPendaftaranPsb().getPenjurusanSekolah() != null) {
			penjurusanSekolah = getGelombangPendaftaranPsb().getPenjurusanSekolah();
		} else {
			penjurusanSekolah = check(penjurusanSekolah);
		}
		return penjurusanSekolah;
	}

	/**
	 * Varian "mentah" dari {@link #getPenjurusanSekolah()} yang mengembalikan nilai KOLOM tanpa
	 * menurunkannya dari {@link #getSiswa()} atau {@link #getGelombangPendaftaranPsb()}.
	 *
	 * <p>Namanya sengaja tidak berawalan {@code get} agar Hibernate tidak memperlakukannya sebagai
	 * properti kedua atas kolom yang sama. Pakai method ini pada layar yang ingin menampilkan
	 * pilihan asli pendaftar.</p>
	 *
	 * <p>Tetap memanggil {@code check(&hellip;)}, jadi objek yang lepas dari session dipulihkan.</p>
	 *
	 * @return penjurusan sebagaimana tersimpan pada kolom, atau {@code null}
	 */
	public PenjurusanSekolah ambilPenjurusanSekolah() {
		penjurusanSekolah = check(penjurusanSekolah);
		return penjurusanSekolah;
	}

	/**
	 * Menetapkan penjurusan/program keahlian yang dipilih.
	 *
	 * @param penjurusanSekolah penjurusan/program keahlian yang dipilih
	 */
	public void setPenjurusanSekolah(PenjurusanSekolah penjurusanSekolah) {
		this.penjurusanSekolah = penjurusanSekolah;
	}

	/**
	 * Mengembalikan alamat tempat tinggal ayah.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Alamat tempat tinggal ayah
	 */
	public String getAlamatAyah() {
		return alamatAyah;
	}

	/**
	 * Menetapkan alamat tempat tinggal ayah.
	 *
	 * @param alamatAyah alamat tempat tinggal ayah
	 */
	public void setAlamatAyah(String alamatAyah) {
		this.alamatAyah = alamatAyah;
	}

	/**
	 * Mengembalikan alamat tempat tinggal ibu.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Alamat tempat tinggal ibu
	 */
	public String getAlamatIbu() {
		return alamatIbu;
	}

	/**
	 * Menetapkan alamat tempat tinggal ibu.
	 *
	 * @param alamatIbu alamat tempat tinggal ibu
	 */
	public void setAlamatIbu(String alamatIbu) {
		this.alamatIbu = alamatIbu;
	}

	/**
	 * NIK (Nomor Induk Kependudukan) calon siswa.
	 *
	 * <p><b>Getter tulis-balik:</b> bila kolom kosong dan {@link #getSiswaAlumni()} punya NIK,
	 * NIK alumni disalin ke field dan ikut tersimpan.</p>
	 *
	 * <p>Termasuk data pribadi paling sensitif pada entity ini: NIK anak di bawah umur, tersimpan
	 * langsung di kolom (bukan sebagai lampiran), sehingga ikut terbawa pada setiap ekspor Excel,
	 * cache entity global, dan pada kebocoran pra-otentikasi yang dijelaskan di Javadoc kelas.</p>
	 *
	 * @return NIK calon siswa, atau {@code null}
	 */
	public String getNik() {

		if ((nik == null || nik.trim().isEmpty()) && (getSiswaAlumni() != null && getSiswaAlumni().getNik() != null
				&& !getSiswaAlumni().getNik().trim().isEmpty())) {
			nik = getSiswaAlumni().getNik();
		}

		return nik;
	}

	/**
	 * Menetapkan NIK (Nomor Induk Kependudukan) calon siswa.
	 *
	 * @param nik NIK (Nomor Induk Kependudukan) calon siswa
	 */
	public void setNik(String nik) {
		this.nik = nik;
	}

	/**
	 * Mengembalikan nomor Kartu Keluarga.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Nomor Kartu Keluarga disimpan apa adanya sebagai data pribadi keluarga; ikut terbawa pada
	 * setiap ekspor Excel dan pada kebocoran pra-otentikasi yang dijelaskan di Javadoc kelas.</p>
	 *
	 * @return Nomor Kartu Keluarga
	 */
	public String getKk() {
		return kk;
	}

	/**
	 * Menetapkan nomor Kartu Keluarga.
	 *
	 * @param kk nomor Kartu Keluarga
	 */
	public void setKk(String kk) {
		this.kk = kk;
	}

	/**
	 * Nomor WhatsApp ayah.
	 *
	 * <p><b>Getter tulis-balik:</b> bila kosong, nomor diambil dari {@link #getHp1ayah()} dan
	 * ditulis ke field. Karena {@code getHp1ayah()} mengembalikan string kosong (bukan
	 * {@code null}) ketika tidak ada nomor, cabang ini juga menulis STRING KOSONG ke kolom
	 * {@code waAyah} &mdash; membedakan "belum diisi" dari "sengaja dikosongkan" menjadi tidak
	 * mungkin setelah baris pernah dibaca.</p>
	 *
	 * @return nomor WhatsApp ayah, atau nilai dari HP utama ayah
	 */
	public String getWaAyah() {
		if ((waAyah == null || waAyah.trim().isEmpty()) && getHp1ayah() != null) {
			waAyah = getHp1ayah();
		}
		return waAyah;
	}

	/**
	 * Menetapkan nomor WhatsApp ayah.
	 *
	 * @param waAyah nomor WhatsApp ayah
	 */
	public void setWaAyah(String waAyah) {
		this.waAyah = waAyah;
	}

	/**
	 * Nomor WhatsApp ibu.
	 *
	 * <p>Pola identik dengan {@link #getWaAyah()}, termasuk penulisan string kosong ke kolom saat
	 * {@link #getHp1ibu()} tidak berisi nomor.</p>
	 *
	 * @return nomor WhatsApp ibu, atau nilai dari HP utama ibu
	 */
	public String getWaIbu() {
		if ((waIbu == null || waIbu.trim().isEmpty()) && getHp1ibu() != null) {
			waIbu = getHp1ibu();
		}
		return waIbu;
	}

	/**
	 * Menetapkan nomor WhatsApp ibu.
	 *
	 * @param waIbu nomor WhatsApp ibu
	 */
	public void setWaIbu(String waIbu) {
		this.waIbu = waIbu;
	}

	/**
	 * Mengembalikan nomor WhatsApp wali.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Bila kosong, getter mengembalikan {@link #getWaAyah()} sebagai fallback baca-saja.</p>
	 *
	 * @return Nomor WhatsApp wali
	 */
	public String getWaWali() {
		return waWali == null || waWali.trim().isEmpty() ? getWaAyah() : waWali.trim();
	}

	/**
	 * Menetapkan nomor WhatsApp wali.
	 *
	 * @param waWali nomor WhatsApp wali
	 */
	public void setWaWali(String waWali) {
		this.waWali = waWali;
	}

	/**
	 * Mengembalikan desa/kelurahan sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Diisi manual; tidak tertaut ke katalog wilayah mana pun.</p>
	 *
	 * @return Desa/kelurahan sekolah asal
	 */
	public String getDesaKelurahanSekolahAsal() {
		return desaKelurahanSekolahAsal;
	}

	/**
	 * Menetapkan desa/kelurahan sekolah asal.
	 *
	 * @param desaKelurahanSekolahAsal desa/kelurahan sekolah asal
	 */
	public void setDesaKelurahanSekolahAsal(String desaKelurahanSekolahAsal) {
		this.desaKelurahanSekolahAsal = desaKelurahanSekolahAsal;
	}

	/**
	 * Kecamatan sekolah asal (kolom {@code kecamatan_kecamatan_sekolah_asal}).
	 *
	 * <p>Salinan persis logika {@link #getKecamatanCalon()}, termasuk koreksi hierarki wilayah yang
	 * tulis-balik dan sifatnya yang tidak deterministik saat ada duplikat kode {@code feeder}.
	 * Keduanya kode kembar &mdash; perbaikan pada satu harus diterapkan pada yang lain.</p>
	 *
	 * @return kecamatan sekolah asal, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_kecamatan_sekolah_asal", nullable = true)
	public Wilayah getKecamatanSekolahAsal() {

		kecamatanSekolahAsal = check(kecamatanSekolahAsal);
		if (kecamatanSekolahAsal != null && kecamatanSekolahAsal.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatanSekolahAsal.getFeeder() != null
						&& kecamatanSekolahAsal.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatanSekolahAsal = w;
					break;
				}
			}

		}

		return kecamatanSekolahAsal;
	}

	/**
	 * Menetapkan kecamatan sekolah asal.
	 *
	 * @param kecamatanSekolahAsal kecamatan sekolah asal
	 */
	public void setKecamatanSekolahAsal(Wilayah kecamatanSekolahAsal) {
		this.kecamatanSekolahAsal = kecamatanSekolahAsal;
	}

	/**
	 * Mengembalikan kabupaten/kota sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengembalikan nilai field apa adanya tanpa memanggil {@code check(&hellip;)} &mdash;
	 * berbeda dari relasi lain pada kelas ini, sehingga objek yang lepas dari session tidak
	 * dipulihkan.</p>
	 *
	 * @return Kabupaten/kota sekolah asal
	 */
	public Kota getKotaSekolahAsal() {
		return kotaSekolahAsal;
	}

	/**
	 * Menetapkan kabupaten/kota sekolah asal.
	 *
	 * @param kotaSekolahAsal kabupaten/kota sekolah asal
	 */
	public void setKotaSekolahAsal(Kota kotaSekolahAsal) {
		this.kotaSekolahAsal = kotaSekolahAsal;
	}

	/**
	 * Propinsi sekolah asal.
	 *
	 * <p><b>Getter tulis-balik:</b> bila {@code kotaSekolahAsal} terisi, propinsi selalu diturunkan
	 * dari kota tersebut dan menimpa field. Perhatikan bahwa cabang ini membaca <b>field</b>
	 * {@code kotaSekolahAsal} secara langsung, bukan {@link #getKotaSekolahAsal()}, sehingga
	 * tidak memicu pemulihan objek lepas-session &mdash; pada objek yang di-detach nilainya bisa
	 * tampak {@code null} padahal kolomnya terisi.</p>
	 *
	 * @return propinsi sekolah asal, atau {@code null}
	 */
	public Propinsi getPropinsiSekolahAsal() {
		if (kotaSekolahAsal != null) {
			propinsiSekolahAsal = kotaSekolahAsal.getPropinsi();
		}
		return propinsiSekolahAsal;
	}

	/**
	 * Menetapkan propinsi sekolah asal.
	 *
	 * @param propinsiSekolahAsal propinsi sekolah asal
	 */
	public void setPropinsiSekolahAsal(Propinsi propinsiSekolahAsal) {
		this.propinsiSekolahAsal = propinsiSekolahAsal;
	}

	/**
	 * Mengembalikan penanda calon siswa dinyatakan tidak diterima.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false} pada nilai yang dikembalikan.</p>
	 *
	 * @return Penanda calon siswa dinyatakan tidak diterima
	 */
	public Boolean getDitolak() {
		return ditolak == null ? false : ditolak;
	}

	/**
	 * Menetapkan penanda calon siswa dinyatakan tidak diterima.
	 *
	 * @param ditolak penanda calon siswa dinyatakan tidak diterima
	 */
	public void setDitolak(Boolean ditolak) {
		this.ditolak = ditolak;
	}

	/**
	 * Mengembalikan penanda calon siswa mengundurkan diri.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false} pada nilai yang dikembalikan. Nilai {@code
	 * true} memaksa {@link #getTelahDiterima()} menjadi {@code false}.</p>
	 *
	 * @return Penanda calon siswa mengundurkan diri
	 */
	public Boolean getMengundurkanDiri() {
		return mengundurkanDiri == null ? false : mengundurkanDiri;
	}

	/**
	 * Menetapkan penanda calon siswa mengundurkan diri.
	 *
	 * @param mengundurkanDiri penanda calon siswa mengundurkan diri
	 */
	public void setMengundurkanDiri(Boolean mengundurkanDiri) {
		this.mengundurkanDiri = mengundurkanDiri;
	}

	/**
	 * Mengembalikan jenis tempat tinggal calon siswa (relasi katalog).
	 *
	 * <p>Dipetakan ke kolom {@code jenis_tinggal_mahasiswa}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Jenis tempat tinggal calon siswa (relasi katalog)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_tinggal_mahasiswa", nullable = true)
	public JenisTinggalMahasiswa getJenisTinggalMahasiswa() {
		jenisTinggalMahasiswa = check(jenisTinggalMahasiswa);
		return jenisTinggalMahasiswa;
	}

	/**
	 * Menetapkan jenis tempat tinggal calon siswa (relasi katalog).
	 *
	 * @param jenisTinggalMahasiswa jenis tempat tinggal calon siswa (relasi katalog)
	 */
	public void setJenisTinggalMahasiswa(JenisTinggalMahasiswa jenisTinggalMahasiswa) {
		this.jenisTinggalMahasiswa = jenisTinggalMahasiswa;
	}

	/**
	 * Mengembalikan alat transportasi ke sekolah (relasi katalog).
	 *
	 * <p>Dipetakan ke kolom {@code alat_transportasi_mahasiswa}, relasi {@code @ManyToOne} yang dimuat
	 * malas ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Alat transportasi ke sekolah (relasi katalog)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alat_transportasi_mahasiswa", nullable = true)
	public AlatTransportasiMahasiswa getAlatTransportasiMahasiswa() {
		alatTransportasiMahasiswa = check(alatTransportasiMahasiswa);
		return alatTransportasiMahasiswa;
	}

	/**
	 * Menetapkan alat transportasi ke sekolah (relasi katalog).
	 *
	 * @param alatTransportasiMahasiswa alat transportasi ke sekolah (relasi katalog)
	 */
	public void setAlatTransportasiMahasiswa(AlatTransportasiMahasiswa alatTransportasiMahasiswa) {
		this.alatTransportasiMahasiswa = alatTransportasiMahasiswa;
	}

	/**
	 * Mengembalikan tahun kelulusan dari sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan tahun berjalan sebagai pengganti (fallback
	 * baca-saja).</p>
	 *
	 * @return Tahun kelulusan dari sekolah asal
	 */
	public Integer getTahunLulus() {
		return tahunLulus == null ? Calendar.getInstance().get(Calendar.YEAR) : tahunLulus;
	}

	/**
	 * Menetapkan tahun kelulusan dari sekolah asal.
	 *
	 * @param tahunLulus tahun kelulusan dari sekolah asal
	 */
	public void setTahunLulus(Integer tahunLulus) {
		this.tahunLulus = tahunLulus;
	}

	/**
	 * Mengembalikan nomor ijazah sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Berbeda dari {@link #getNomorSeriIjazah()}: yang ini nomor ijazah, yang itu nomor seri
	 * blangko.</p>
	 *
	 * @return Nomor ijazah sekolah asal
	 */
	public String getNoIjazah() {
		return noIjazah;
	}

	/**
	 * Menetapkan nomor ijazah sekolah asal.
	 *
	 * @param noIjazah nomor ijazah sekolah asal
	 */
	public void setNoIjazah(String noIjazah) {
		this.noIjazah = noIjazah;
	}

	/**
	 * Mengembalikan status sekolah asal (Negeri atau Swasta).
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Nilainya diharapkan salah satu dari {@link #NEGERI} atau {@link #SWASTA}, tetapi tidak ada
	 * validasi yang menegakkannya.</p>
	 *
	 * @return Status sekolah asal (Negeri atau Swasta)
	 */
	public String getStatusSekolah() {
		return statusSekolah;
	}

	/**
	 * Menetapkan status sekolah asal (Negeri atau Swasta).
	 *
	 * @param statusSekolah status sekolah asal (Negeri atau Swasta)
	 */
	public void setStatusSekolah(String statusSekolah) {
		this.statusSekolah = statusSekolah;
	}

	/**
	 * Nilai bawaan {@link #getFormulaPrestasi()}: representasi teks sebuah array JSON kosong
	 * ({@code []}), sehingga pembaca formula tidak perlu menangani {@code null}.
	 *
	 * <p><b>Kuirk:</b> field ini {@code public static} dan TIDAK {@code final} &mdash; siapa pun
	 * dapat menimpanya pada runtime dan mengubah nilai bawaan bagi SELURUH calon siswa dalam satu
	 * JVM. Perlakukan sebagai konstanta meskipun kompilator tidak menegakkannya.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan formula penilaian prestasi calon siswa dalam bentuk JSON.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), bertipe {@code text}.</p>
	 *
	 * <p>Bila kolom kosong, getter mengembalikan {@link #DEFAULT_FORMULA} (array JSON kosong) sehingga
	 * pembaca tidak perlu menangani {@code null}.</p>
	 *
	 * @return Formula penilaian prestasi calon siswa dalam bentuk JSON
	 */
	@Column(columnDefinition = "text")
	public String getFormulaPrestasi() {
		return formulaPrestasi == null || formulaPrestasi.isEmpty() ? DEFAULT_FORMULA : formulaPrestasi;
	}

	/**
	 * Menetapkan formula penilaian prestasi calon siswa dalam bentuk JSON.
	 *
	 * @param formulaPrestasi formula penilaian prestasi calon siswa dalam bentuk JSON
	 */
	public void setFormulaPrestasi(String formulaPrestasi) {
		this.formulaPrestasi = formulaPrestasi;
	}

	/**
	 * Mengembalikan slot jadwal pertemuan tatap muka yang dipilih calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code jadwal_pertemuan_psb}, relasi {@code @ManyToOne} yang dimuat malas
	 * ({@code LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Slot jadwal pertemuan tatap muka yang dipilih calon siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pertemuan_psb", nullable = true)
	public JadwalPertemuanPSB getJadwalPertemuanPSB() {
		jadwalPertemuanPSB = check(jadwalPertemuanPSB);
		return jadwalPertemuanPSB;
	}

	/**
	 * Menetapkan slot jadwal pertemuan tatap muka yang dipilih calon siswa.
	 *
	 * @param jadwalPertemuanPSB slot jadwal pertemuan tatap muka yang dipilih calon siswa
	 */
	public void setJadwalPertemuanPSB(JadwalPertemuanPSB jadwalPertemuanPSB) {
		this.jadwalPertemuanPSB = jadwalPertemuanPSB;
	}

	/**
	 * Menghitung total skor yang diperoleh calon siswa untuk SATU parameter tambahan bertipe
	 * {@link ParameterTambahan#PILIHAN_CUSTOM}.
	 *
	 * <p>Menyisir setiap baris pada {@link #getParameterTambahan()}, mengambil id parameter dari
	 * kolom kelima, memuat objek {@link ParameterTambahan} dari cache, dan &mdash; hanya bila id-nya
	 * sama dengan parameter yang diminta DAN tipe inputannya {@code PILIHAN_CUSTOM} &mdash;
	 * mengurai skor dari nilai jawaban. Nilai jawaban boleh berbentuk {@code "label:skor"} (skor
	 * diambil setelah titik dua) atau angka polos.</p>
	 *
	 * <p>Dipakai layar seleksi PSB untuk memeringkat pendaftar berdasarkan jawaban formulir
	 * (mis. prestasi, jarak rumah, status keluarga), berdampingan dengan
	 * {@link #getFormulaPrestasi()}.</p>
	 *
	 * <p><b>Kuirk:</b> seluruh kegagalan parsing ditelan dan menghasilkan skor {@code 0}, sehingga
	 * jawaban yang formatnya rusak menurunkan peringkat pendaftar tanpa pesan apa pun. Loop juga
	 * tidak berhenti setelah menemukan parameter yang dicari, jadi bila satu parameter menjawab
	 * lebih dari sekali (mungkin terjadi karena {@link #populateParameterTambahan(java.util.List)}
	 * tidak menjamin keunikan), skornya DIJUMLAHKAN.</p>
	 *
	 * @param parameterTambahanData parameter yang ingin dihitung skornya; bila {@code null} hasilnya
	 * selalu {@code 0}
	 * @return total skor untuk parameter tersebut; {@code 0} bila tidak ada jawaban yang cocok
	 */
	public Integer ambilSkor(ParameterTambahan parameterTambahanData) {
		Integer totalSkor = 0;
		if (!getParameterTambahan().isEmpty() && parameterTambahanData != null) {
			String[] splNama = getParameterTambahan().split("\n");
			for (int j = 0; j < splNama.length; j++) {
				Integer skor = 0;
				String namaCol = splNama.length > j ? splNama[j] : "";

				String[] value = namaCol.split("<=>");
				String val = value.length > 1 ? value[1].trim() : "";

				ParameterTambahan parameterTambahan = null;
				Long id = 1L;
				try {
					id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
					parameterTambahan = (ParameterTambahan) ConstantValues.ambil(ParameterTambahan.class.getName(), id);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:1913");

				}

				if (parameterTambahan != null && parameterTambahan.getId().equals(parameterTambahanData.getId())
						&& parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_CUSTOM)) {
					String[] kol = StringUtils.split(val, ":");
					if (kol.length > 1) {
						try {
							skor = Integer.parseInt(kol[1].trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:1923");

						}
					} else {
						try {
							skor = Integer.parseInt(val.trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:1929");

						}
					}
				}
				totalSkor += skor;
			}
		}
		return totalSkor;
	}

	/**
	 * Merakit <b>tautan masuk portal tanpa kata sandi</b> untuk calon siswa ini.
	 *
	 * <p>Dipakai pada email/WhatsApp pemberitahuan pendaftaran agar orang tua dapat langsung membuka
	 * portal PSB tanpa mengingat nomor registrasi. Bentuk hasilnya:</p>
	 * <pre>
	 * &lt;host&gt;/m?q=URLEncode(DES("&lt;id&gt;-CalonSiswa-abcdefghijklmnopqrstuvwxyz"))
	 * </pre>
	 *
	 * <p>Basis host diambil dari {@code Common.getRequestHostWithProtocol()}; bila konfigurasi
	 * {@code login_via_link_menggunakan_domain_masing_masing} aktif, host diganti dengan domain milik
	 * {@link #getSekolah()} yang diambil dari {@code HttpServletRequest} saat ini (dari eksekusi ZK,
	 * atau dari {@code RequestContext} bila dipanggil di luar konteks ZK &mdash; mis. dari thread
	 * pengiriman email).</p>
	 *
	 * <p><b>PERINGATAN KEAMANAN.</b> Token ini bukan token: {@code ais.action.servlet.MServet}
	 * mendekripsinya, mengambil {@link #getId()}, memuat entity, lalu memanggil
	 * {@code Common.setLogin(request, calonSiswa)} secara langsung. Tidak ada masa berlaku, tidak
	 * ada nonce, tidak ada pembatasan sekali pakai, dan tidak ada pencatatan. Plaintext-nya
	 * sepenuhnya deterministik &mdash; id yang berurutan ditambah sufiks konstanta yang sama untuk
	 * semua entity. Satu-satunya rahasia adalah passphrase DES {@code Common.DES_PASS_PHRASE} yang
	 * tertanam di kode sumber dan identik pada seluruh instalasi AIS. Siapa pun yang pernah membaca
	 * kode ini karena itu dapat mencetak tautan masuk untuk pendaftar mana pun, di instalasi mana
	 * pun, hanya dengan menaikkan angka id. Tautan yang sudah terkirim juga tidak pernah kedaluwarsa,
	 * sehingga bocornya satu email lama tetap memberi akses penuh.</p>
	 *
	 * @return URL lengkap tautan masuk portal
	 * @throws Exception bila pengkodean URL gagal atau domain sekolah tidak dapat dibaca
	 */
	public String urlLogin() throws Exception {
		String url = Common.getRequestHostWithProtocol();

		if (Common.bolehKonfigurasi("login_via_link_menggunakan_domain_masing_masing", Konfigurasi.TIDAK_AKTIF)) {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			url = "https://" + getSekolah().getDomain() + request.getContextPath();
		}

		String code = url + "/m?q=" + URLEncoder
				.encode(Common.desEncrypter.get().encrypt(getId() + "-CalonSiswa-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
		return code;
	}

	/**
	 * Mengembalikan baris siswa alumni jenjang sebelumnya yang merupakan orang yang sama.
	 *
	 * <p>Dipetakan ke kolom {@code siswa_alumni}, relasi {@code @ManyToOne} yang dimuat malas ({@code
	 * LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Baris siswa alumni jenjang sebelumnya yang merupakan orang yang sama
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_alumni", nullable = true)
	public Siswa getSiswaAlumni() {
		siswaAlumni = check(siswaAlumni);
		return siswaAlumni;
	}

	/**
	 * Menetapkan baris siswa alumni jenjang sebelumnya yang merupakan orang yang sama.
	 *
	 * @param siswaAlumni baris siswa alumni jenjang sebelumnya yang merupakan orang yang sama
	 */
	public void setSiswaAlumni(Siswa siswaAlumni) {
		this.siswaAlumni = siswaAlumni;
	}

	/**
	 * Mengembalikan kakak atau adik calon siswa yang sudah bersekolah di sini.
	 *
	 * <p>Dipetakan ke kolom {@code siswa_sibling}, relasi {@code @ManyToOne} yang dimuat malas ({@code
	 * LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * <p>Dipakai untuk memberi diskon saudara pada rantai billing; tidak mempengaruhi biodata.</p>
	 *
	 * @return Kakak atau adik calon siswa yang sudah bersekolah di sini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_sibling", nullable = true)
	public Siswa getSiswaSibling() {
		siswaSibling = check(siswaSibling);
		return siswaSibling;
	}

	/**
	 * Menetapkan kakak atau adik calon siswa yang sudah bersekolah di sini.
	 *
	 * @param siswaSibling kakak atau adik calon siswa yang sudah bersekolah di sini
	 */
	public void setSiswaSibling(Siswa siswaSibling) {
		this.siswaSibling = siswaSibling;
	}

	/**
	 * Mengembalikan nomor akta kelahiran calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Data identitas anak di bawah umur; berkas fisiknya disimpan terpisah sebagai {@link
	 * ais.database.model.file.LampiranLain} yang di-key dengan id {@link
	 * CalonSiswaPunyaVerifikasiBerkas}, bukan id entity ini.</p>
	 *
	 * @return Nomor akta kelahiran calon siswa
	 */
	public String getNoAktaKelahiran() {
		return noAktaKelahiran;
	}

	/**
	 * Menetapkan nomor akta kelahiran calon siswa.
	 *
	 * @param noAktaKelahiran nomor akta kelahiran calon siswa
	 */
	public void setNoAktaKelahiran(String noAktaKelahiran) {
		this.noAktaKelahiran = noAktaKelahiran;
	}

	/**
	 * Mengembalikan penanda calon siswa punya saudara kandung yang bersekolah di sini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false} pada nilai yang dikembalikan.</p>
	 *
	 * @return Penanda calon siswa punya saudara kandung yang bersekolah di sini
	 */
	public Boolean getApakahMempunyaiSaudaraKandung() {
		return apakahMempunyaiSaudaraKandung == null ? false : apakahMempunyaiSaudaraKandung;
	}

	/**
	 * Menetapkan penanda calon siswa punya saudara kandung yang bersekolah di sini.
	 *
	 * @param apakahMempunyaiSaudaraKandung penanda calon siswa punya saudara kandung yang bersekolah di
	 * sini
	 */
	public void setApakahMempunyaiSaudaraKandung(Boolean apakahMempunyaiSaudaraKandung) {
		this.apakahMempunyaiSaudaraKandung = apakahMempunyaiSaudaraKandung;
	}

	/**
	 * Mengembalikan keterangan saudara kandung yang bersekolah di sini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), bertipe {@code text}.</p>
	 *
	 * <p>Kolom {@code text} bebas; dipakai bila {@link #getApakahMempunyaiSaudaraKandung()} bernilai
	 * {@code true}.</p>
	 *
	 * @return Keterangan saudara kandung yang bersekolah di sini
	 */
	@Column(columnDefinition = "text")
	public String getInfoMempunyaiSaudaraKandung() {
		return infoMempunyaiSaudaraKandung;
	}

	/**
	 * Menetapkan keterangan saudara kandung yang bersekolah di sini.
	 *
	 * @param infoMempunyaiSaudaraKandung keterangan saudara kandung yang bersekolah di sini
	 */
	public void setInfoMempunyaiSaudaraKandung(String infoMempunyaiSaudaraKandung) {
		this.infoMempunyaiSaudaraKandung = infoMempunyaiSaudaraKandung;
	}

	/**
	 * Merangkai nilai sekumpulan {@link org.zkoss.zul.Checkbox} menjadi string bertanda titik
	 * koma yang siap disimpan ke {@link #setKebutuhanKhusus(String)} atau
	 * {@link #setInfoKampusDariMana(String)}.
	 *
	 * <p>Setiap kotak centang yang tercentang menyumbang {@code ";" + label huruf kecil + ";"}.
	 * Format bertitik-koma di kedua ujung itulah yang membuat pencarian substring
	 * {@code ";nilai;"} aman dari kecocokan sebagian &mdash; lihat {@link #getKebutuhanKhusus()}
	 * yang menegakkan format tersebut.</p>
	 *
	 * <p><b>Kuirk:</b> nama method ini menyebut "kebutuhan khusus" tetapi nama parameternya
	 * {@code infoKampusDariMana}; ia memang dipakai untuk KEDUA kolom, karena keduanya berbagi
	 * format penyimpanan yang sama. Anak komponen yang bukan {@code Checkbox} diabaikan lewat
	 * {@code catch} kosong, jadi kesalahan susunan layar tidak terlihat.</p>
	 *
	 * <p>Method ini {@code static} dan tidak menyentuh entity apa pun.</p>
	 *
	 * @param infoKampusDariMana komponen induk berisi kotak-kotak centang; boleh {@code null}
	 * @return rangkaian label tercentang bertanda titik koma; string kosong bila tidak ada yang
	 * tercentang
	 */
	@SuppressWarnings("unchecked")
	public static String kebutuhanKhusus(Box infoKampusDariMana) {
		String info = "";

		List<Component> c = infoKampusDariMana == null ? null : infoKampusDariMana.getChildren();
		if (c != null) {
			for (Component ccc : c) {
				try {
					Checkbox cc = (Checkbox) ccc;
					if (cc.isChecked()) {
						info += ";" + cc.getLabel().toLowerCase() + ";";
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:2019");
					// TODO: handle exception
				}
			}
		}

		return info;
	}

	/**
	 * Daftar kebutuhan khusus/disabilitas calon siswa, disimpan sebagai satu kolom {@code text}
	 * bertanda titik koma.
	 *
	 * <p><b>Getter tulis-balik yang menegakkan format.</b> Nilai {@code null} diubah menjadi string
	 * kosong, dan bila isinya tidak diawali/diakhiri {@code ";"} tanda itu DITAMBAHKAN ke field.
	 * Normalisasi ini penting karena pencarian dilakukan dengan mencocokkan substring
	 * {@code ";nilai;"}; tanpa tanda pembatas, {@code "tuna"} akan cocok dengan
	 * {@code "tunarungu"}.</p>
	 *
	 * <p>Data kesehatan/disabilitas anak &mdash; termasuk kategori paling sensitif pada entity ini.
	 * Isinya dibentuk oleh {@link #kebutuhanKhusus(org.zkoss.zul.Box)}.</p>
	 *
	 * @return daftar kebutuhan khusus bertanda titik koma; string kosong bila tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getKebutuhanKhusus() {
		if (kebutuhanKhusus == null) {
			kebutuhanKhusus = "";
		}
		if (!kebutuhanKhusus.trim().isEmpty() && !kebutuhanKhusus.startsWith(";")) {
			kebutuhanKhusus = ";" + kebutuhanKhusus;
		}
		if (!kebutuhanKhusus.trim().isEmpty() && !kebutuhanKhusus.endsWith(";")) {
			kebutuhanKhusus = kebutuhanKhusus + ";";
		}
		return kebutuhanKhusus;
	}

	/**
	 * Menetapkan daftar kebutuhan khusus/disabilitas calon siswa.
	 *
	 * @param kebutuhanKhusus daftar kebutuhan khusus/disabilitas calon siswa
	 */
	public void setKebutuhanKhusus(String kebutuhanKhusus) {
		this.kebutuhanKhusus = kebutuhanKhusus;
	}

	/**
	 * Mengembalikan koordinat GPS tempat tinggal calon siswa.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL), bertipe {@code text}.</p>
	 *
	 * <p>Titik GPS tempat tinggal anak di bawah umur, disimpan sebagai kolom {@code text}. Termasuk data
	 * paling sensitif pada entity ini.</p>
	 *
	 * @return Koordinat GPS tempat tinggal calon siswa
	 */
	@Column(columnDefinition = "text")
	public String getKoordinat() {
		return koordinat;
	}

	/**
	 * Menetapkan koordinat GPS tempat tinggal calon siswa.
	 *
	 * @param koordinat koordinat GPS tempat tinggal calon siswa
	 */
	public void setKoordinat(String koordinat) {
		this.koordinat = koordinat;
	}

	/**
	 * Mengembalikan jenis tempat tinggal calon siswa dalam bentuk teks.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Versi teks bebas dari {@link #getJenisTinggalMahasiswa()}; keduanya dipelihara terpisah dan
	 * dapat berbeda isi.</p>
	 *
	 * @return Jenis tempat tinggal calon siswa dalam bentuk teks
	 */
	public String getJenisTinggal() {
		return jenisTinggal;
	}

	/**
	 * Menetapkan jenis tempat tinggal calon siswa dalam bentuk teks.
	 *
	 * @param jenisTinggal jenis tempat tinggal calon siswa dalam bentuk teks
	 */
	public void setJenisTinggal(String jenisTinggal) {
		this.jenisTinggal = jenisTinggal;
	}

	/**
	 * Mengembalikan alat transportasi ke sekolah dalam bentuk teks.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Versi teks bebas dari {@link #getAlatTransportasiMahasiswa()}; keduanya dipelihara terpisah dan
	 * dapat berbeda isi.</p>
	 *
	 * @return Alat transportasi ke sekolah dalam bentuk teks
	 */
	public String getAlatTransportasi() {
		return alatTransportasi;
	}

	/**
	 * Menetapkan alat transportasi ke sekolah dalam bentuk teks.
	 *
	 * @param alatTransportasi alat transportasi ke sekolah dalam bentuk teks
	 */
	public void setAlatTransportasi(String alatTransportasi) {
		this.alatTransportasi = alatTransportasi;
	}

	/**
	 * Mengembalikan penanda calon siswa penerima bantuan pendidikan.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false} pada nilai yang dikembalikan.</p>
	 *
	 * @return Penanda calon siswa penerima bantuan pendidikan
	 */
	public Boolean getPenerimaBantuan() {
		return penerimaBantuan == null ? false : penerimaBantuan;
	}

	/**
	 * Menetapkan penanda calon siswa penerima bantuan pendidikan.
	 *
	 * @param penerimaBantuan penanda calon siswa penerima bantuan pendidikan
	 */
	public void setPenerimaBantuan(Boolean penerimaBantuan) {
		this.penerimaBantuan = penerimaBantuan;
	}

	/**
	 * Mengembalikan nomor Kartu Indonesia Pintar.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Nomor Kartu Indonesia Pintar; menandakan status ekonomi keluarga.</p>
	 *
	 * @return Nomor Kartu Indonesia Pintar
	 */
	public String getNoKip() {
		return noKip;
	}

	/**
	 * Menetapkan nomor Kartu Indonesia Pintar.
	 *
	 * @param noKip nomor Kartu Indonesia Pintar
	 */
	public void setNoKip(String noKip) {
		this.noKip = noKip;
	}

	/**
	 * Mengembalikan alasan calon siswa layak menerima PIP.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Diisi bila calon siswa dinilai memenuhi syarat Program Indonesia Pintar.</p>
	 *
	 * @return Alasan calon siswa layak menerima PIP
	 */
	public String getLayakPip() {
		return layakPip;
	}

	/**
	 * Menetapkan alasan calon siswa layak menerima PIP.
	 *
	 * @param layakPip alasan calon siswa layak menerima PIP
	 */
	public void setLayakPip(String layakPip) {
		this.layakPip = layakPip;
	}

	/**
	 * Mengembalikan alasan calon siswa tidak layak menerima PIP.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Diisi bila calon siswa dinilai tidak memenuhi syarat Program Indonesia Pintar.</p>
	 *
	 * @return Alasan calon siswa tidak layak menerima PIP
	 */
	public String getTidakLayakPip() {
		return tidakLayakPip;
	}

	/**
	 * Menetapkan alasan calon siswa tidak layak menerima PIP.
	 *
	 * @param tidakLayakPip alasan calon siswa tidak layak menerima PIP
	 */
	public void setTidakLayakPip(String tidakLayakPip) {
		this.tidakLayakPip = tidakLayakPip;
	}

	/**
	 * Mengembalikan NIK ayah.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return NIK ayah
	 */
	public String getNikAyah() {
		return nikAyah;
	}

	/**
	 * Menetapkan NIK ayah.
	 *
	 * @param nikAyah NIK ayah
	 */
	public void setNikAyah(String nikAyah) {
		this.nikAyah = nikAyah;
	}

	/**
	 * Mengembalikan NIK ibu.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return NIK ibu
	 */
	public String getNikIbu() {
		return nikIbu;
	}

	/**
	 * Menetapkan NIK ibu.
	 *
	 * @param nikIbu NIK ibu
	 */
	public void setNikIbu(String nikIbu) {
		this.nikIbu = nikIbu;
	}

	/**
	 * Mengembalikan NIK wali.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return NIK wali
	 */
	public String getNikWali() {
		return nikWali;
	}

	/**
	 * Menetapkan NIK wali.
	 *
	 * @param nikWali NIK wali
	 */
	public void setNikWali(String nikWali) {
		this.nikWali = nikWali;
	}

	/**
	 * Mengembalikan NIS (nomor induk siswa lokal) yang diterbitkan setelah calon diterima.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Berbeda dari {@link #getNomorIndukNasional() NISN}: NIS bersifat lokal per sekolah dan biasanya
	 * baru terisi setelah {@code CommonPSB.onGenerateNis} berjalan.</p>
	 *
	 * @return NIS (nomor induk siswa lokal) yang diterbitkan setelah calon diterima
	 */
	public String getNis() {
		return nis;
	}

	/**
	 * Menetapkan NIS (nomor induk siswa lokal) yang diterbitkan setelah calon diterima.
	 *
	 * @param nis NIS (nomor induk siswa lokal) yang diterbitkan setelah calon diterima
	 */
	public void setNis(String nis) {
		this.nis = nis;
	}

	/**
	 * Mengembalikan nomor seri ijazah sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Berbeda dari {@link #getNoIjazah()}: yang ini nomor seri blangko, yang itu nomor ijazah.</p>
	 *
	 * @return Nomor seri ijazah sekolah asal
	 */
	public String getNomorSeriIjazah() {
		return nomorSeriIjazah;
	}

	/**
	 * Menetapkan nomor seri ijazah sekolah asal.
	 *
	 * @param nomorSeriIjazah nomor seri ijazah sekolah asal
	 */
	public void setNomorSeriIjazah(String nomorSeriIjazah) {
		this.nomorSeriIjazah = nomorSeriIjazah;
	}

	/**
	 * Mengembalikan nomor seri SKHUN sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Nomor seri SKHUN sekolah asal
	 */
	public String getNomorSeriSkhun() {
		return nomorSeriSkhun;
	}

	/**
	 * Menetapkan nomor seri SKHUN sekolah asal.
	 *
	 * @param nomorSeriSkhun nomor seri SKHUN sekolah asal
	 */
	public void setNomorSeriSkhun(String nomorSeriSkhun) {
		this.nomorSeriSkhun = nomorSeriSkhun;
	}

	/**
	 * Mengembalikan nomor peserta Ujian Nasional di sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * @return Nomor peserta Ujian Nasional di sekolah asal
	 */
	public String getNomorUjianNasional() {
		return nomorUjianNasional;
	}

	/**
	 * Menetapkan nomor peserta Ujian Nasional di sekolah asal.
	 *
	 * @param nomorUjianNasional nomor peserta Ujian Nasional di sekolah asal
	 */
	public void setNomorUjianNasional(String nomorUjianNasional) {
		this.nomorUjianNasional = nomorUjianNasional;
	}

	/**
	 * Mengembalikan NPSN (nomor pokok sekolah nasional) sekolah asal.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>NPSN sekolah asal, bukan NPSN sekolah tujuan.</p>
	 *
	 * @return NPSN (nomor pokok sekolah nasional) sekolah asal
	 */
	public String getNomorPokokSekolahNasional() {
		return nomorPokokSekolahNasional;
	}

	/**
	 * Menetapkan NPSN (nomor pokok sekolah nasional) sekolah asal.
	 *
	 * @param nomorPokokSekolahNasional NPSN (nomor pokok sekolah nasional) sekolah asal
	 */
	public void setNomorPokokSekolahNasional(String nomorPokokSekolahNasional) {
		this.nomorPokokSekolahNasional = nomorPokokSekolahNasional;
	}

	/**
	 * Mengembalikan penanda calon siswa diasuh wali, bukan orang tua kandung.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false} pada nilai yang dikembalikan.</p>
	 *
	 * @return Penanda calon siswa diasuh wali, bukan orang tua kandung
	 */
	public Boolean getMempunyaiWali() {
		return mempunyaiWali == null ? false : mempunyaiWali;
	}

	/**
	 * Menetapkan penanda calon siswa diasuh wali, bukan orang tua kandung.
	 *
	 * @param mempunyaiWali penanda calon siswa diasuh wali, bukan orang tua kandung
	 */
	public void setMempunyaiWali(Boolean mempunyaiWali) {
		this.mempunyaiWali = mempunyaiWali;
	}

	/**
	 * Mengembalikan nomor telepon calon siswa dalam format internasional tanpa tanda {@code +}
	 * ({@code 62&hellip;}), siap dipakai sebagai tujuan pengiriman WhatsApp/SMS.
	 *
	 * <p>Menyaring nilai placeholder ({@code "00000000000000000000"}, {@code "000000000"}) lalu
	 * menormalkan awalan: {@code 08&hellip;} &rarr; {@code 628&hellip;}, {@code 0&hellip;} &rarr;
	 * {@code 62&hellip;}, dan menambahkan {@code 62} bila belum berawalan itu.</p>
	 *
	 * <p>Berbeda dari {@link #tampilkanHp(org.zkoss.zk.ui.Component)} yang memakai tanda {@code +}
	 * dan punya jalur cadangan ke nomor orang tua, method ini HANYA memakai
	 * {@link #getTeleponSiswa()}. Untuk calon siswa jenjang dasar yang belum punya nomor sendiri,
	 * hasilnya sering string kosong &mdash; pemanggil harus menyiapkan jalur cadangan sendiri lewat
	 * {@link #ambilTelp()}.</p>
	 *
	 * <p><b>Kuirk:</b> normalisasi dilakukan berurutan tanpa {@code else}, sehingga nomor yang sudah
	 * berawalan {@code 62} tetap aman, tetapi nomor asing berawalan {@code 0} akan salah diberi kode
	 * negara Indonesia.</p>
	 *
	 * @return nomor telepon berformat {@code 62&hellip;}, atau string kosong bila tidak ada nomor
	 * yang layak
	 */
	public String ambilHp() {
		String hp = getTeleponSiswa();
		if (hp != null && !hp.trim().isEmpty() && !(hp == null || hp.toString().trim().isEmpty()
				|| hp.toString().trim().equals("00000000000000000000") || hp.toString().trim().equals("000000000"))) {
			hp = hp.startsWith("08") ? "62" + hp.substring(1) : hp;
			hp = hp.startsWith("0") ? "62" + hp.substring(1) : hp;
			hp = !hp.startsWith("62") ? "62" + hp : hp;

		}
		return hp;
	}

	/**
	 * Menyisipkan lokasi foto calon siswa ke dalam peta parameter laporan JasperReports.
	 *
	 * <p>Dipakai oleh pencetakan kartu pendaftaran, kartu ujian, dan biodata. Urutan sumber foto:</p>
	 * <ol>
	 * <li>Berkas fisik {@link ais.database.model.file.FotoCalonSiswa} bila ada &rarr; path absolut.</li>
	 * <li>Tautan Dropbox &rarr; {@code dropboxLinkRaw()}.</li>
	 * <li>Tautan Google Drive &rarr; {@code exportGDriveUrl()}.</li>
	 * <li>Metadata lampiran lain &rarr; {@code createLinkUri()}.</li>
	 * <li>Bila tidak ada sama sekali &rarr; ikon bawaan
	 * {@code /img/administrator-icon_default.png} di dalam webapp.</li>
	 * </ol>
	 *
	 * <p>Foto dicari lewat {@code FileFotoLain.ambil(getId(), FotoCalonSiswa.DEFAULT_JENIS,
	 * FotoCalonSiswa.class)} &mdash; yaitu di-<i>key</i> dengan {@link #getId()} entity ini, tanpa
	 * pemeriksaan tenant. Ini salah satu simpul yang membuat id berurutan bernilai bagi penyerang.</p>
	 *
	 * <p>Seluruh exception ditelan (dicetak ke log dan dicatat ke audit error), sehingga kegagalan
	 * foto tidak menggagalkan pencetakan &mdash; tetapi juga tidak terlihat operator: laporan tetap
	 * terbit tanpa foto.</p>
	 *
	 * @param parameters peta parameter laporan yang akan diisi kunci {@code "foto"}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			CalonSiswa calonCalonSiswa = this;

			FileFotoLain fotocalonSiswa = FileFotoLain.ambil(calonCalonSiswa.getId(), FotoCalonSiswa.DEFAULT_JENIS,
					FotoCalonSiswa.class);

			if (fotocalonSiswa != null && fotocalonSiswa.ambilFile() != null) {
				parameters.put("foto", fotocalonSiswa.ambilFile().getAbsolutePath());
			} else if (fotocalonSiswa != null && fotocalonSiswa.getLink() != null
					&& fotocalonSiswa.getLink().toLowerCase().contains("dropbox")) {
				parameters.put("foto", fotocalonSiswa.dropboxLinkRaw());
			} else if (fotocalonSiswa != null && fotocalonSiswa.getGdrive() != null) {
				parameters.put("foto", fotocalonSiswa.exportGDriveUrl());
			} else if (fotocalonSiswa != null) {
				parameters.put("foto", fotocalonSiswa.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
			}

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/sekolah/CalonSiswa.java:2210");
		}
	}

	/**
	 * Varian "mentah" dari {@link #getKelasLesDipilih()} yang mengembalikan isi KOLOM apa adanya
	 * tanpa mengambilnya dari {@link #getSiswa()} dan tanpa membersihkan koma ganda.
	 *
	 * <p>Namanya sengaja tidak berawalan {@code get} agar Hibernate tidak memperlakukannya sebagai
	 * properti kedua atas kolom yang sama. Pakai method ini bila yang dibutuhkan adalah pilihan asli
	 * pendaftar, bukan pilihan siswa yang sudah aktif.</p>
	 *
	 * @return daftar id kelas les yang tersimpan pada kolom; string kosong bila belum diisi
	 */
	public String ambilKelasLesDipilih() {
		return kelasLesDipilih == null ? "" : kelasLesDipilih.trim();
	}

	/**
	 * Daftar id {@link KelasLesSiswa} yang dipilih calon siswa, dipisah koma.
	 *
	 * <p>Implementasi konkret dari kontrak abstrak {@link VOSiswa#getKelasLesDipilih()}; kelas induk
	 * memakainya lewat {@link VOSiswa#ambilKelasLesSiswaId()} dan
	 * {@link VOSiswa#ambilKelasLesSiswa()} untuk mengubah teks ini menjadi daftar entity.</p>
	 *
	 * <p><b>Getter tulis-balik dua cabang.</b> Bila calon sudah menjadi siswa
	 * ({@link #getSiswa()} tidak {@code null}), daftar diambil dari siswa tersebut dan MENIMPA
	 * field &mdash; pilihan asli pendaftar hilang. Bila belum, nilainya dinormalkan menjadi bentuk
	 * {@code ",id,id,"} dengan koma ganda dibersihkan; bentuk yang hanya berisi koma diubah menjadi
	 * string kosong.</p>
	 *
	 * <p><b>Kuirk:</b> pembersihan koma ganda dilakukan dengan tiga kali {@code replaceAll(",,", ",")}
	 * berurutan, bukan dengan ekspresi reguler {@code ",{2,}"}; deretan koma yang sangat panjang
	 * tidak sepenuhnya bersih. Karena {@link VOSiswa#ambilKelasLesSiswaId()} memetakan token yang
	 * bukan angka menjadi {@code -1L}, sisa koma tidak menyebabkan error tetapi menghasilkan id
	 * palsu yang lalu diabaikan saat pencarian entity.</p>
	 *
	 * @return daftar id kelas les dipisah koma; string kosong bila tidak ada pilihan
	 */
	public String getKelasLesDipilih() {
		if (getSiswa() != null) {
			kelasLesDipilih = getSiswa().getKelasLesDipilih();
		} else {
			kelasLesDipilih = (kelasLesDipilih == null || kelasLesDipilih.trim().equalsIgnoreCase(",") ? ""
					: "," + kelasLesDipilih.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
					.replaceAll(",,", ",");

			if (kelasLesDipilih.equals(",")) {
				kelasLesDipilih = "";
			} else if (kelasLesDipilih.equals(",,")) {
				kelasLesDipilih = "";
			} else if (kelasLesDipilih.equals(",,,")) {
				kelasLesDipilih = "";
			}
		}
		return kelasLesDipilih == null ? "" : kelasLesDipilih.trim();
	}

	/**
	 * Menetapkan daftar id kelas les yang dipilih calon siswa, dipisah koma.
	 *
	 * @param kelasLesDipilih daftar id kelas les yang dipilih calon siswa, dipisah koma
	 */
	public void setKelasLesDipilih(String kelasLesDipilih) {
		this.kelasLesDipilih = kelasLesDipilih;
	}

	/**
	 * Daftar id {@link PembayaranSiswaDetail} milik calon siswa ini, dipisah koma.
	 *
	 * <p>Kolom denormalisasi yang diisi {@link #populate(CalonSiswa)}; disediakan agar layar dan
	 * laporan tidak perlu menghitung ulang seluruh rantai pembayaran setiap kali.</p>
	 *
	 * <p><b>Getter tulis-balik.</b> Nilainya dinormalkan menjadi bentuk {@code ",id,id,"} lalu koma
	 * ganda dibersihkan dengan tiga kali {@code replaceAll(",,", ",")} berurutan; bentuk yang hanya
	 * berisi koma diubah menjadi string kosong. Hasil normalisasi DITULIS ke field, jadi membaca
	 * baris lama dapat menandainya sebagai berubah.</p>
	 *
	 * <p>Kelima saudara kolom ini &mdash; {@link #getRiwayatPengaturanPembayaran()},
	 * {@link #getRiwayatJenisPembayaran()}, {@link #getRiwayatPembayaranPendaftaran()},
	 * {@link #getRiwayatPembayaranDaftarUlang()}, dan {@link #getRiwayatPembayaranInfo()} &mdash;
	 * memakai pola yang persis sama.</p>
	 *
	 * @return daftar id dipisah koma; string kosong bila belum ada pembayaran
	 */
	@Column(columnDefinition = "text")
	public String getRiwayatPembayaran() {
		riwayatPembayaran = (riwayatPembayaran == null || riwayatPembayaran.trim().equalsIgnoreCase(",") ? ""
				: "," + riwayatPembayaran.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (riwayatPembayaran.equals(",")) {
			riwayatPembayaran = "";
		} else if (riwayatPembayaran.equals(",,")) {
			riwayatPembayaran = "";
		} else if (riwayatPembayaran.equals(",,,")) {
			riwayatPembayaran = "";
		}
		return riwayatPembayaran == null ? "" : riwayatPembayaran.trim();
	}

	/**
	 * Menetapkan daftar id baris pembayaran calon siswa, dipisah koma.
	 *
	 * @param riwayatPembayaran daftar id baris pembayaran calon siswa, dipisah koma
	 */
	public void setRiwayatPembayaran(String riwayatPembayaran) {
		this.riwayatPembayaran = riwayatPembayaran;
	}

	/**
	 * Daftar id {@link PembayaranSiswaDetail} yang tergolong BIAYA PENDAFTARAN, dipisah koma.
	 *
	 * <p>Diisi {@link #populate(CalonSiswa)}. Nilai TIDAK kosong pada kolom inilah yang memicu
	 * {@link #getTelahDiterima()} menyetel penerimaan otomatis, dan yang memicu penerbitan NIS bila
	 * gelombang menyalakan {@code otomatisDapatNisKetikaSudahBayarReg}. Karena itu isi kolom ini
	 * berdampak langsung pada status kelulusan, bukan sekadar catatan.</p>
	 *
	 * <p><b>Getter tulis-balik</b> dengan normalisasi koma yang sama seperti
	 * {@link #getRiwayatPembayaran()}.</p>
	 *
	 * @return daftar id dipisah koma; string kosong bila biaya pendaftaran belum dibayar
	 */
	@Column(columnDefinition = "text")
	public String getRiwayatPembayaranPendaftaran() {
		riwayatPembayaranPendaftaran = (riwayatPembayaranPendaftaran == null
				|| riwayatPembayaranPendaftaran.trim().equalsIgnoreCase(",") ? ""
						: "," + riwayatPembayaranPendaftaran.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (riwayatPembayaranPendaftaran.equals(",")) {
			riwayatPembayaranPendaftaran = "";
		} else if (riwayatPembayaranPendaftaran.equals(",,")) {
			riwayatPembayaranPendaftaran = "";
		} else if (riwayatPembayaranPendaftaran.equals(",,,")) {
			riwayatPembayaranPendaftaran = "";
		}
		return riwayatPembayaranPendaftaran == null ? "" : riwayatPembayaranPendaftaran.trim();
	}

	/**
	 * Menetapkan daftar id pembayaran biaya pendaftaran, dipisah koma.
	 *
	 * @param riwayatPembayaranPendaftaran daftar id pembayaran biaya pendaftaran, dipisah koma
	 */
	public void setRiwayatPembayaranPendaftaran(String riwayatPembayaranPendaftaran) {
		this.riwayatPembayaranPendaftaran = riwayatPembayaranPendaftaran;
	}

	/**
	 * Daftar id {@link PembayaranSiswaDetail} yang tergolong BIAYA DAFTAR ULANG, dipisah koma.
	 *
	 * <p>Diisi {@link #populate(CalonSiswa)} berdasarkan {@code jenisBiayaSekolahLulus} pada
	 * gelombang. Sama seperti saudaranya {@link #getRiwayatPembayaranPendaftaran()}, isi kolom ini
	 * dapat memicu penerbitan NIS bila gelombang menyalakan
	 * {@code otomatisDapatNisKetikaSudahBayarDaftarUlang}.</p>
	 *
	 * <p><b>Getter tulis-balik</b> dengan normalisasi koma yang sama seperti
	 * {@link #getRiwayatPembayaran()}.</p>
	 *
	 * @return daftar id dipisah koma; string kosong bila daftar ulang belum dibayar
	 */
	@Column(columnDefinition = "text")
	public String getRiwayatPembayaranDaftarUlang() {
		riwayatPembayaranDaftarUlang = (riwayatPembayaranDaftarUlang == null
				|| riwayatPembayaranDaftarUlang.trim().equalsIgnoreCase(",") ? ""
						: "," + riwayatPembayaranDaftarUlang.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (riwayatPembayaranDaftarUlang.equals(",")) {
			riwayatPembayaranDaftarUlang = "";
		} else if (riwayatPembayaranDaftarUlang.equals(",,")) {
			riwayatPembayaranDaftarUlang = "";
		} else if (riwayatPembayaranDaftarUlang.equals(",,,")) {
			riwayatPembayaranDaftarUlang = "";
		}
		return riwayatPembayaranDaftarUlang == null ? "" : riwayatPembayaranDaftarUlang.trim();
	}

	/**
	 * Menetapkan daftar id pembayaran biaya daftar ulang, dipisah koma.
	 *
	 * @param riwayatPembayaranDaftarUlang daftar id pembayaran biaya daftar ulang, dipisah koma
	 */
	public void setRiwayatPembayaranDaftarUlang(String riwayatPembayaranDaftarUlang) {
		this.riwayatPembayaranDaftarUlang = riwayatPembayaranDaftarUlang;
	}

	/**
	 * Menjalankan {@link #populate(CalonSiswa)} untuk baris ini di THREAD TERPISAH.
	 *
	 * <p>Dipanggil setelah pembayaran diterima (dari layar pembayaran, callback bank, dan
	 * {@code PembayaranOnline}) agar keenam kolom ringkasan {@code riwayat*} serta
	 * {@link #getTelahDiterima()} diperbarui tanpa menahan permintaan pengguna.</p>
	 *
	 * <p><b>Hal yang harus diketahui sebelum memakainya:</b></p>
	 * <ul>
	 * <li>Thread dibuat secara mentah ({@code new Thread(&hellip;).start()}), bukan lewat pool.
	 * Memanggil method ini di dalam loop atas banyak calon siswa akan membuat satu thread per
	 * baris.</li>
	 * <li>Ada jeda tetap 100&nbsp;milidetik sebelum kerja dimulai &mdash; upaya kasar agar transaksi
	 * pemanggil sempat commit lebih dulu. Tidak ada jaminan apa pun bahwa jeda itu cukup;
	 * bila transaksi pemanggil lebih lambat, ringkasan yang dihitung akan tertinggal satu
	 * pembayaran.</li>
	 * <li>Thread membuka session Hibernate sendiri dan memuat ULANG entity dari database
	 * ({@code session.get}), jadi objek yang dipegang pemanggil TIDAK ikut diperbarui.</li>
	 * <li>Tidak ada penanganan hasil: kegagalan hanya tercetak ke log.</li>
	 * </ul>
	 *
	 * @see #populate(CalonSiswa)
	 */
	public void populatePembayaran() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/CalonSiswa.java:2311");
				}

				CalonSiswa calonSiswa = CalonSiswa.this;
				CalonSiswa.populate(calonSiswa);
			}
		}).start();
	}

	/**
	 * Menghitung ulang seluruh kolom ringkasan pembayaran calon siswa, lalu &mdash; bila syarat
	 * gelombang terpenuhi &mdash; MENERBITKAN NIS dan memasukkan siswa ke kelas.
	 *
	 * <p>Ini mesin denormalisasi utama entity ini. Meskipun namanya terdengar seperti pembacaan
	 * saja, method ini <b>menulis ke database</b> dan dapat MENGUBAH STATUS KELULUSAN seorang anak.
	 * Dipanggil dari {@link #populatePembayaran()} (asinkron) dan langsung dari layar pembayaran,
	 * callback bank, serta tombol sinkronisasi.</p>
	 *
	 * <h4>Tahap 1 &mdash; menyusun ringkasan</h4>
	 * <p>Membuka session baru dengan {@code FlushMode.MANUAL} (agar getter tulis-balik yang tak
	 * terhitung banyaknya pada entity ini tidak ikut ter-flush di tengah jalan), memuat ulang entity
	 * berdasarkan id, lalu:</p>
	 * <ul>
	 * <li>Menyisir seluruh {@link PembayaranSiswaDetail} milik calon siswa ini dan merangkai lima
	 * string berkoma: {@link #setRiwayatPengaturanPembayaran(String)} (id
	 * {@link PengaturanBiaya}, atau {@code <id>_<bulan>_<tahun>} untuk periode Bulanan),
	 * {@link #setRiwayatJenisPembayaran(String)} (id {@link JenisBiayaSekolah}),
	 * {@link #setRiwayatPembayaran(String)} (id baris detail),
	 * {@link #setRiwayatPembayaranPendaftaran(String)}, dan
	 * {@link #setRiwayatPembayaranDaftarUlang(String)}.</li>
	 * <li>Sebuah baris dianggap "pembayaran pendaftaran" bila gelombang menyalakan
	 * {@code sesuaiKelas}, ATAU jenis biayanya sama dengan {@code jenisBiayaSekolah} gelombang,
	 * ATAU sama dengan {@code jenisBiayaSekolahTerverifikasi}. Dianggap "daftar ulang" bila sama
	 * dengan {@code jenisBiayaSekolahLulus}.</li>
	 * <li>Merangkai {@link #setRiwayatPembayaranInfo(String)}: uraian teks berisi kelas les, kelas,
	 * pengaturan biaya, nama item biaya, angsuran ke berapa, bulan, tahun, tanggal bayar, dan
	 * nominal untuk setiap pembayaran.</li>
	 * <li>Menyisir {@link Tagihan} yang sudah kedaluwarsa atau belum terbayar DAN ditandai
	 * "bukan tagihan"/bernominal nol, lalu ikut mendaftarkan pengaturan biayanya sebagai
	 * "sudah beres" &mdash; supaya tagihan yang memang tidak perlu dibayar tidak terus muncul
	 * sebagai tunggakan.</li>
	 * </ul>
	 *
	 * <h4>Tahap 2 &mdash; menyimpan</h4>
	 * <p>Perubahan disimpan dengan SATU {@code update CalonSiswa set &hellip; where id = :id}
	 * berparameter. Pendekatan bulk ini dipilih agar tidak ada satu pun getter tulis-balik yang ikut
	 * tersimpan &mdash; tetapi konsekuensinya perubahan ini <b>MELEWATI Envers</b>: riwayat audit
	 * tidak pernah mencatat kapan {@code telahDiterima} berubah lewat jalur ini, padahal itu
	 * keputusan kelulusan.</p>
	 *
	 * <h4>Tahap 3 &mdash; efek lanjutan</h4>
	 * <ul>
	 * <li>Bila gelombang menyalakan {@code otomatisDapatNisKetikaSudahBayarReg} (atau
	 * {@code otomatisDapatNisKetikaSudahBayarDaftarUlang} dengan
	 * {@code jenisBiayaSekolahLulus} terisi) dan pembayaran terkait sudah ada,
	 * {@code telahDiterima} disetel {@code true} dan
	 * {@code CommonPSB.onGenerateNis} dijalankan &mdash; menerbitkan NIS dan MEMBUAT baris
	 * {@link Siswa} baru.</li>
	 * <li>Bila baris siswa sudah ada, {@code CommonPSB.masukkanKelas} dan
	 * {@code CommonPSB.masukkanKelasLes} menempatkannya ke kelas.</li>
	 * <li>Setiap {@link KelasLesSiswa} pilihan pendaftar yang belum punya baris
	 * {@link KelasLesSiswaPunyaSiswa} akan dibuatkan barisnya, satu transaksi per kelas les.</li>
	 * </ul>
	 *
	 * <h4>Risiko yang perlu diketahui</h4>
	 * <ul>
	 * <li><b>Kelas nama generator NIS berasal dari konfigurasi.</b>
	 * {@code Class.forName(Common.getKonfigurasi("class_untuk_generate_nis",
	 * DefaultNisGenerator.class.getName()).getNilai()).newInstance()} &mdash; siapa pun yang
	 * dapat mengubah baris konfigurasi itu menentukan kelas Java mana yang di-instansiasi pada
	 * server. Ingat pula bahwa {@code Common.getKonfigurasi} MENULIS nilai bawaan ke database
	 * bila kuncinya belum ada.</li>
	 * <li><b>Tidak ada kunci idempoten.</b> Dua pemanggilan bersamaan (mis. dua callback bank
	 * berurutan) dapat sama-sama melihat {@code getSiswa() == null} dan sama-sama memicu
	 * penerbitan NIS.</li>
	 * <li><b>Kegagalan senyap.</b> Seluruh blok dibungkus {@code try/catch} yang hanya mencetak ke
	 * log; bila tahap 3 gagal, ringkasan tetap tersimpan tetapi siswa tidak pernah terbentuk.</li>
	 * <li><b>Pengelolaan session bercampur.</b> Tahap 1&ndash;2 memakai session baru yang ditutup di
	 * {@code finally}, tahap 3 memakai {@code HibernateUtil.currentNativeSession()} lalu
	 * menutupnya di dalam loop &mdash; pola yang mudah menghasilkan session tertutup ganda bila
	 * kode ini dipanggil dari dalam transaksi lain.</li>
	 * </ul>
	 *
	 * @param calonSiswa calon siswa yang ringkasannya dihitung ulang; hanya {@link #getId()}-nya
	 * yang dipakai, entity dimuat ulang dari database. Tidak melakukan apa pun
	 * bila id {@code null} atau barisnya sudah terhapus
	 */
	@SuppressWarnings("unchecked")
	public static void populate(CalonSiswa calonSiswa) {

		if (calonSiswa.getId() != null) {

			String riwayatPengaturanPembayaran = "";
			String riwayatJenisPembayaran = "";
			String riwayatPembayaran = "";
			String riwayatPembayaranPendaftaran = "";
			String riwayatPembayaranDaftarUlang = "";
			String riwayatPembayaranInfo = "";

			Session session = null;
			org.hibernate.Transaction tx = null;
			try {
				session = HibernateUtil.openSession();
				session.setFlushMode(FlushMode.MANUAL);
				calonSiswa = (CalonSiswa) session.get(CalonSiswa.class, calonSiswa.getId());
				if (calonSiswa == null) {
					return;
				}
				List<PembayaranSiswaDetail> riwayatPembayarans = session.createCriteria(PembayaranSiswaDetail.class)
						.createAlias("pembayaranSiswa", "pembayaranSiswa")
						.add(Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa)).addOrder(Order.asc("id"))
						.list();

				for (PembayaranSiswaDetail r : riwayatPembayarans) {
					Tagihan tagihan = r.getTagihan();

					if (tagihan != null && tagihan.getPembayaranSiswaDetail() != null) {

						Long pemId = r.getId();
						Long jenisId = tagihan.getPengaturanBiaya() != null
								&& tagihan.getPengaturanBiaya().getJenisBiayaSekolah() != null
										? tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getId()
										: -1L;
						Long pengId = tagihan.getPengaturanBiaya() != null ? tagihan.getPengaturanBiaya().getId() : -1L;

						if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Bulanan")) {
							riwayatPengaturanPembayaran += riwayatPengaturanPembayaran.isEmpty()
									? (pengId + "_" + tagihan.getBulan() + "_" + tagihan.getTahun())
									: "," + (pengId + "_" + tagihan.getBulan() + "_" + tagihan.getTahun());
						} else {
							riwayatPengaturanPembayaran += riwayatPengaturanPembayaran.isEmpty() ? pengId.toString()
									: "," + pengId;
						}

						riwayatJenisPembayaran += riwayatJenisPembayaran.isEmpty() ? jenisId.toString() : "," + jenisId;

						riwayatPembayaran += riwayatPembayaran.isEmpty() ? pemId.toString() : "," + pemId;
						if (calonSiswa.getGelombangPendaftaranPsb() != null
								&& (calonSiswa.getGelombangPendaftaranPsb().getSesuaiKelas()
										|| (calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah() != null
												&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah()
														.getId() != null
												&& jenisId
														.equals(calonSiswa
																.getGelombangPendaftaranPsb().getJenisBiayaSekolah()
																.getId()))
										|| (calonSiswa.getGelombangPendaftaranPsb()
												.getJenisBiayaSekolahTerverifikasi() != null
												&& calonSiswa.getGelombangPendaftaranPsb()
														.getJenisBiayaSekolahTerverifikasi().getId() != null
												&& jenisId.equals(calonSiswa.getGelombangPendaftaranPsb()
														.getJenisBiayaSekolahTerverifikasi().getId())))) {
							riwayatPembayaranPendaftaran += riwayatPembayaranPendaftaran.isEmpty() ? pemId.toString()
									: "," + pemId;
						}

						if (calonSiswa.getGelombangPendaftaranPsb() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus().getId() != null
								&& jenisId.equals(
										calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus().getId())) {
							riwayatPembayaranDaftarUlang += riwayatPembayaranDaftarUlang.isEmpty() ? pemId.toString()
									: "," + pemId;
						}

						String desc =

								(tagihan.getPengaturanBiaya() == null
										|| tagihan.getPengaturanBiaya().getKelasLesSiswa() == null ? ""
												: tagihan.getPengaturanBiaya().getKelasLesSiswa().getNama() + " ")
										+

										(tagihan.getPengaturanBiaya() == null
												|| tagihan.getPengaturanBiaya().getKelasSiswa() == null ? ""
														: tagihan.getPengaturanBiaya().getKelasSiswa().getNama() + " ")
										+

										(tagihan.getPengaturanBiaya() == null
												|| tagihan.getPengaturanBiaya().getJenisBiayaSekolah() == null ? ""
														: tagihan.getPengaturanBiaya().toString() + " ")
										+ tagihan.getItemBiayaSekolah().getNama()
										+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
												? " (ke " + tagihan.getBayarKe() + ")"
												: "")
										+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
										+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()) + ", "
										+ Common.dateFormat3.get().format(r.getPembayaranSiswa().getTanggalBayar()) + ", "
										+ Common.numberFormat.get().format(r.getNominal());

						riwayatPembayaranInfo += desc;
					}
				}
				riwayatPembayarans.clear();
				riwayatPembayarans = null;

				List<Tagihan> tagihans = session.createCriteria(Tagihan.class)
						.add(Restrictions.or(
								Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
										Restrictions
												.sqlRestriction("this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
								Restrictions.isNull("pembayaranSiswaDetail")))
						.add(Restrictions.or(Restrictions.eq("bukanTagihan", true), Restrictions.lt("nominal", 0.1)))
						.add(Restrictions.eq("calonSiswa", calonSiswa)).addOrder(Order.asc("id")).list();

				for (Tagihan tagihan : tagihans) {
					if ((tagihan.getNominal() < 0.1 || tagihan.ambilBukanTagihanData())
							&& ((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
									&& !tagihan.getNominalBiaya().getBukanTagihan())) {
//						Long jenisId = tagihan.getPengaturanBiaya() != null
//								&& tagihan.getPengaturanBiaya().getJenisBiayaSekolah() != null
//										? tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getId()
//										: -1L;
						Long pengId = tagihan.getPengaturanBiaya() != null ? tagihan.getPengaturanBiaya().getId() : -1L;

						if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Bulanan")) {
							riwayatPengaturanPembayaran += riwayatPengaturanPembayaran.isEmpty()
									? (pengId + "_" + tagihan.getBulan() + "_" + tagihan.getTahun())
									: "," + (pengId + "_" + tagihan.getBulan() + "_" + tagihan.getTahun());
						} else {
							riwayatPengaturanPembayaran += riwayatPengaturanPembayaran.isEmpty() ? pengId.toString()
									: "," + pengId;
						}

//						riwayatJenisPembayaran += riwayatJenisPembayaran.isEmpty() ? jenisId.toString() : "," + jenisId;

					}
				}
				tagihans.clear();
				tagihans = null;

				calonSiswa.setRiwayatPengaturanPembayaran(riwayatPengaturanPembayaran);
				calonSiswa.setRiwayatJenisPembayaran(riwayatJenisPembayaran);
				calonSiswa.setRiwayatPembayaran(riwayatPembayaran);
				calonSiswa.setRiwayatPembayaranPendaftaran(riwayatPembayaranPendaftaran);
				calonSiswa.setRiwayatPembayaranDaftarUlang(riwayatPembayaranDaftarUlang);
				calonSiswa.setRiwayatPembayaranInfo(riwayatPembayaranInfo);

				if ((calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getOtomatisDapatNisKetikaSudahBayarReg()
						&& calonSiswa.getSiswa() == null && !riwayatPembayaranPendaftaran.isEmpty())
						|| (calonSiswa.getGelombangPendaftaranPsb() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus().getId() != null
								&& calonSiswa.getGelombangPendaftaranPsb()
										.getOtomatisDapatNisKetikaSudahBayarDaftarUlang()
								&& calonSiswa.getSiswa() == null && !riwayatPembayaranDaftarUlang.isEmpty())) {
					calonSiswa.setTelahDiterima(true);
				}

			tx = session.beginTransaction();
			Query query = session.createQuery("update CalonSiswa set riwayatPengaturanPembayaran = :rpp, "
					+ "riwayatJenisPembayaran = :rjp, riwayatPembayaran = :rp, "
					+ "riwayatPembayaranPendaftaran = :rpd, riwayatPembayaranDaftarUlang = :rpdu, "
					+ "riwayatPembayaranInfo = :rpi, telahDiterima = :td where id = :id");
			query.setParameter("rpp", riwayatPengaturanPembayaran);
			query.setParameter("rjp", riwayatJenisPembayaran);
			query.setParameter("rp", riwayatPembayaran);
			query.setParameter("rpd", riwayatPembayaranPendaftaran);
			query.setParameter("rpdu", riwayatPembayaranDaftarUlang);
			query.setParameter("rpi", riwayatPembayaranInfo);
			query.setParameter("td", calonSiswa.getTelahDiterima());
			query.setParameter("id", calonSiswa.getId());
			query.executeUpdate();
			tx.commit();

			} catch (Exception e) {
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:2499");}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/CalonSiswa.java:2500");
			} finally {
				try { if (session != null && session.isOpen()) session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:2502");}
				HibernateUtil.closeSession();
			}

			try {
				if (calonSiswa.getTelahDiterima() && ((calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getOtomatisDapatNisKetikaSudahBayarReg()
						&& calonSiswa.getSiswa() == null && !riwayatPembayaranPendaftaran.isEmpty())
						|| (calonSiswa.getGelombangPendaftaranPsb() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus().getId() != null
								&& calonSiswa.getGelombangPendaftaranPsb()
										.getOtomatisDapatNisKetikaSudahBayarDaftarUlang()
								&& calonSiswa.getSiswa() == null && !riwayatPembayaranDaftarUlang.isEmpty()))) {
					NisGenerator nisGenerator = (NisGenerator) Class.forName(
							Common.getKonfigurasi("class_untuk_generate_nis", DefaultNisGenerator.class.getName())
									.getNilai().trim())
							.newInstance();

					CommonPSB.onGenerateNis(calonSiswa, nisGenerator);
				}

				if (calonSiswa.getTelahDiterima() && ((calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getOtomatisDapatNisKetikaSudahBayarReg()
						&& calonSiswa.getSiswa() != null && !riwayatPembayaranPendaftaran.isEmpty())
						|| (calonSiswa.getGelombangPendaftaranPsb() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus() != null
								&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus().getId() != null
								&& calonSiswa.getGelombangPendaftaranPsb()
										.getOtomatisDapatNisKetikaSudahBayarDaftarUlang()
								&& calonSiswa.getSiswa() != null && !riwayatPembayaranDaftarUlang.isEmpty()))) {
					CommonPSB.masukkanKelas(calonSiswa, calonSiswa.getSiswa());
					CommonPSB.masukkanKelasLes(calonSiswa, calonSiswa.getSiswa());
				}

				if (calonSiswa.getSiswa() != null) {
					List<KelasLesSiswa> kelasLesSiswas = calonSiswa.ambilKelasLesSiswa();
					for (KelasLesSiswa kelasLesSiswa : kelasLesSiswas) {
						try {
							 session = HibernateUtil.currentNativeSession();

							try {
								int count = ((Number) session.createCriteria(KelasLesSiswaPunyaSiswa.class)
										.setProjection(Projections.rowCount())
										.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
										.add(Restrictions.eq("siswa", calonSiswa.getSiswa())).uniqueResult())
										.intValue();

								if (count == 0) {
									KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = new KelasLesSiswaPunyaSiswa();
									kelasLesSiswaPunyaSiswa.setCalonSiswa(calonSiswa);
									kelasLesSiswaPunyaSiswa.setSiswa(calonSiswa.getSiswa());
									kelasLesSiswaPunyaSiswa.setKelasLesSiswa(kelasLesSiswa);
									kelasLesSiswaPunyaSiswa.setAktif(true);
									session.getTransaction().begin();
									session.save(kelasLesSiswaPunyaSiswa);
									session.getTransaction().commit();
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CalonSiswa.java:2560");
								// TODO: handle exception
							}

							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
						HibernateUtil.closeSession();
					}
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/CalonSiswa.java:2577");
			}

		}
	}

	/**
	 * Uraian teks seluruh pembayaran calon siswa, dirakit {@link #populate(CalonSiswa)}.
	 *
	 * <p>Setiap pembayaran menyumbang satu potongan berisi kelas les, kelas, pengaturan biaya, nama
	 * item biaya, nomor angsuran, bulan, tahun, tanggal bayar, dan nominal terformat. Dipakai untuk
	 * menampilkan riwayat pembayaran tanpa harus menelusuri ulang rantai billing.</p>
	 *
	 * <p><b>Kuirk:</b> potongan-potongan itu digabung TANPA pemisah antar-pembayaran, sehingga pada
	 * calon siswa dengan banyak pembayaran hasilnya menjadi satu kalimat panjang yang sulit dipecah
	 * kembali secara terprogram. Perlakukan sebagai teks tampilan saja, jangan diurai.</p>
	 *
	 * <p>Berbeda dari lima saudaranya, getter ini TIDAK menulis balik apa pun &mdash; hanya
	 * mengganti {@code null} dengan string kosong pada nilai kembalian.</p>
	 *
	 * @return uraian pembayaran; string kosong bila belum ada pembayaran
	 */
	@Column(columnDefinition = "text")
	public String getRiwayatPembayaranInfo() {
		return riwayatPembayaranInfo == null ? "" : riwayatPembayaranInfo;
	}

	/**
	 * Menetapkan uraian teks seluruh pembayaran calon siswa.
	 *
	 * @param riwayatPembayaranInfo uraian teks seluruh pembayaran calon siswa
	 */
	public void setRiwayatPembayaranInfo(String riwayatPembayaranInfo) {
		this.riwayatPembayaranInfo = riwayatPembayaranInfo;
	}

	/**
	 * Kelas yang DIPILIH calon siswa saat mendaftar (kolom {@code kelas_dipilih_id}).
	 *
	 * <p>Bukan kelas tempat siswa akhirnya ditempatkan &mdash; penempatan sesungguhnya dilakukan
	 * {@code CommonPSB.masukkanKelas} pada entity {@link Siswa} lewat
	 * {@link KelasSiswaPunyaSiswa}. Nama kolom {@code kelas_dipilih_id} mencerminkan makna ini
	 * lebih tepat daripada nama propertinya.</p>
	 *
	 * @return kelas pilihan pendaftar, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_dipilih_id")
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	/**
	 * Menetapkan kelas yang dipilih calon siswa saat mendaftar.
	 *
	 * @param kelasSiswa kelas yang dipilih calon siswa saat mendaftar
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * Daftar id {@link PengaturanBiaya} yang kewajibannya sudah beres, dipisah koma.
	 *
	 * <p>Diisi {@link #populate(CalonSiswa)}. Untuk pengaturan biaya berperiode Bulanan, entrinya
	 * tidak hanya id melainkan {@code <id>_<bulan>_<tahun>} agar bulan yang berbeda dapat dibedakan.
	 * Selain pembayaran nyata, kolom ini juga memuat pengaturan biaya yang tagihannya ditandai
	 * "bukan tagihan"/bernominal nol &mdash; supaya kewajiban yang memang tidak perlu dibayar tidak
	 * terus tampil sebagai tunggakan.</p>
	 *
	 * <p><b>Getter tulis-balik</b> dengan normalisasi koma yang sama seperti
	 * {@link #getRiwayatPembayaran()}.</p>
	 *
	 * @return daftar penanda pengaturan biaya dipisah koma; string kosong bila belum ada
	 */
	@Column(columnDefinition = "text")
	public String getRiwayatPengaturanPembayaran() {
		riwayatPengaturanPembayaran = (riwayatPengaturanPembayaran == null
				|| riwayatPengaturanPembayaran.trim().equalsIgnoreCase(",") ? ""
						: "," + riwayatPengaturanPembayaran.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (riwayatPengaturanPembayaran.equals(",")) {
			riwayatPengaturanPembayaran = "";
		} else if (riwayatPengaturanPembayaran.equals(",,")) {
			riwayatPengaturanPembayaran = "";
		} else if (riwayatPengaturanPembayaran.equals(",,,")) {
			riwayatPengaturanPembayaran = "";
		}
		return riwayatPengaturanPembayaran == null ? "" : riwayatPengaturanPembayaran.trim();
	}

	/**
	 * Menetapkan daftar id pengaturan biaya yang sudah terbayar, dipisah koma.
	 *
	 * @param riwayatPengaturanPembayaran daftar id pengaturan biaya yang sudah terbayar, dipisah koma
	 */
	public void setRiwayatPengaturanPembayaran(String riwayatPengaturanPembayaran) {
		this.riwayatPengaturanPembayaran = riwayatPengaturanPembayaran;
	}

	/**
	 * Daftar id {@link JenisBiayaSekolah} yang sudah pernah dibayar, dipisah koma.
	 *
	 * <p>Diisi {@link #populate(CalonSiswa)} dari rantai
	 * {@link Tagihan} &rarr; {@link PengaturanBiaya} &rarr; {@link JenisBiayaSekolah}. Berbeda dari
	 * {@link #getRiwayatPengaturanPembayaran()}, kolom ini TIDAK diisi untuk tagihan yang ditandai
	 * "bukan tagihan" &mdash; baris kodenya masih ada di {@link #populate(CalonSiswa)} tetapi
	 * dikomentari.</p>
	 *
	 * <p><b>Getter tulis-balik</b> dengan normalisasi koma yang sama seperti
	 * {@link #getRiwayatPembayaran()}.</p>
	 *
	 * @return daftar id jenis biaya dipisah koma; string kosong bila belum ada
	 */
	@Column(columnDefinition = "text")
	public String getRiwayatJenisPembayaran() {
		riwayatJenisPembayaran = (riwayatJenisPembayaran == null || riwayatJenisPembayaran.trim().equalsIgnoreCase(",")
				? ""
				: "," + riwayatJenisPembayaran.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (riwayatJenisPembayaran.equals(",")) {
			riwayatJenisPembayaran = "";
		} else if (riwayatJenisPembayaran.equals(",,")) {
			riwayatJenisPembayaran = "";
		} else if (riwayatJenisPembayaran.equals(",,,")) {
			riwayatJenisPembayaran = "";
		}
		return riwayatJenisPembayaran == null ? "" : riwayatJenisPembayaran.trim();
	}

	/**
	 * Menetapkan daftar id jenis biaya yang sudah terbayar, dipisah koma.
	 *
	 * @param riwayatJenisPembayaran daftar id jenis biaya yang sudah terbayar, dipisah koma
	 */
	public void setRiwayatJenisPembayaran(String riwayatJenisPembayaran) {
		this.riwayatJenisPembayaran = riwayatJenisPembayaran;
	}

	/**
	 * Mengembalikan paket PSB (paket biaya/program) yang diambil calon siswa.
	 *
	 * <p>Dipetakan ke kolom {@code paket_psb}, relasi {@code @ManyToOne} yang dimuat malas ({@code
	 * LAZY}) dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * @return Paket PSB (paket biaya/program) yang diambil calon siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket_psb")
	public PaketPsb getPaketPsb() {
		paketPsb = check(paketPsb);
		return paketPsb;
	}

	/**
	 * Menetapkan paket PSB (paket biaya/program) yang diambil calon siswa.
	 *
	 * @param paketPsb paket PSB (paket biaya/program) yang diambil calon siswa
	 */
	public void setPaketPsb(PaketPsb paketPsb) {
		this.paketPsb = paketPsb;
	}

	/**
	 * Sumber informasi calon siswa mengenal sekolah ini (brosur, media sosial, teman, dan
	 * seterusnya).
	 *
	 * <p>Menegakkan format bertanda titik koma yang sama dengan {@link #getKebutuhanKhusus()} dan
	 * JUGA menulis balik hasil normalisasinya ke field. Berbeda dari saudaranya itu, nilai yang
	 * DIKEMBALIKAN tambahan dijadikan huruf kecil dan dipangkas &mdash; sementara yang ditulis ke
	 * field tidak &mdash; sehingga hasil getter dan isi kolom dapat berbeda kapitalisasinya.</p>
	 *
	 * <p>Pasangannya: {@link #getKeteranganInfoKampusDariMana()} dan
	 * {@link #getNamaTemanInfoKampusDariMana()}.</p>
	 *
	 * @return daftar sumber informasi bertanda titik koma dalam huruf kecil; string kosong bila
	 * tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getInfoKampusDariMana() {
		if (infoKampusDariMana == null) {
			infoKampusDariMana = "";
		}
		if (!infoKampusDariMana.trim().isEmpty() && !infoKampusDariMana.startsWith(";")) {
			infoKampusDariMana = ";" + infoKampusDariMana;
		}
		if (!infoKampusDariMana.trim().isEmpty() && !infoKampusDariMana.endsWith(";")) {
			infoKampusDariMana = infoKampusDariMana + ";";
		}
		return infoKampusDariMana.trim().toLowerCase();
	}

	/**
	 * Menetapkan sumber informasi calon siswa mengenal sekolah ini.
	 *
	 * @param infoKampusDariMana sumber informasi calon siswa mengenal sekolah ini
	 */
	public void setInfoKampusDariMana(String infoKampusDariMana) {
		this.infoKampusDariMana = infoKampusDariMana;
	}

	/**
	 * Mengembalikan keterangan tambahan sumber informasi sekolah.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Dipakai bersama {@link #getInfoKampusDariMana()} untuk pilihan "lainnya".</p>
	 *
	 * @return Keterangan tambahan sumber informasi sekolah
	 */
	public String getKeteranganInfoKampusDariMana() {
		return keteranganInfoKampusDariMana;
	}

	/**
	 * Menetapkan keterangan tambahan sumber informasi sekolah.
	 *
	 * @param keteranganInfoKampusDariMana keterangan tambahan sumber informasi sekolah
	 */
	public void setKeteranganInfoKampusDariMana(String keteranganInfoKampusDariMana) {
		this.keteranganInfoKampusDariMana = keteranganInfoKampusDariMana;
	}

	/**
	 * Mengembalikan nama kenalan yang merekomendasikan sekolah ini.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Dipakai bersama {@link #getInfoKampusDariMana()} untuk pilihan "dari teman".</p>
	 *
	 * @return Nama kenalan yang merekomendasikan sekolah ini
	 */
	public String getNamaTemanInfoKampusDariMana() {
		return namaTemanInfoKampusDariMana;
	}

	/**
	 * Menetapkan nama kenalan yang merekomendasikan sekolah ini.
	 *
	 * @param namaTemanInfoKampusDariMana nama kenalan yang merekomendasikan sekolah ini
	 */
	public void setNamaTemanInfoKampusDariMana(String namaTemanInfoKampusDariMana) {
		this.namaTemanInfoKampusDariMana = namaTemanInfoKampusDariMana;
	}

	/**
	 * Penanda calon siswa merupakan siswa PINDAHAN dari sekolah lain.
	 *
	 * <p><b>Getter tulis-balik:</b> nilai {@code null} ditulis menjadi {@code false} ke field.</p>
	 *
	 * <p>Penanda ini adalah saklar bagi enam properti lain: {@link #getStatusAwalSiswa()} dipaksa
	 * menjadi {@code PINDAHAN_SISWA} bila {@code true}, sedangkan {@link #getTanggalPindah()},
	 * {@link #getKeteranganPindah()}, {@link #getPindahanDariSekolah()},
	 * {@link #getAlamatSekolahPindahan()}, dan {@link #getKelasSekolahPindahan()} MENGOSONGKAN
	 * dirinya bila {@code false}. Melepas centang ini karena itu menghapus permanen seluruh blok data
	 * kepindahan &mdash; lihat peringatan pada masing-masing getter.</p>
	 *
	 * @return {@code true} bila calon siswa merupakan pindahan; tidak pernah {@code null}
	 */
	public Boolean getMerupakanPindahan() {
		if (merupakanPindahan == null) {
			merupakanPindahan = false;
		}
		return merupakanPindahan;
	}

	/**
	 * Menetapkan penanda calon siswa merupakan siswa pindahan.
	 *
	 * @param merupakanPindahan penanda calon siswa merupakan siswa pindahan
	 */
	public void setMerupakanPindahan(Boolean merupakanPindahan) {
		this.merupakanPindahan = merupakanPindahan;
	}

	/**
	 * Tanggal kepindahan dari sekolah sebelumnya.
	 *
	 * <p><b>Getter DESTRUKTIF.</b> Bila {@link #getMerupakanPindahan()} bernilai {@code false},
	 * field di-{@code null}-kan; bila {@code true} tetapi kolomnya kosong, field diisi waktu SAAT
	 * INI. Keduanya ditulis ke field dan ikut tersimpan.</p>
	 *
	 * <p>Konsekuensinya: melepas centang "pindahan" lalu menyimpan akan MENGHAPUS tanggal
	 * kepindahan secara permanen, dan mencentangnya kembali akan mengisi tanggal HARI INI &mdash;
	 * bukan tanggal aslinya. Tidak ada layar yang memperingatkan hal ini.</p>
	 *
	 * @return tanggal kepindahan, atau {@code null} bila calon siswa bukan pindahan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalPindah() {
		if (!getMerupakanPindahan()) {
			tanggalPindah = null;
		} else if (tanggalPindah == null) {
			tanggalPindah = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalPindah;
	}

	/**
	 * Menetapkan tanggal kepindahan dari sekolah sebelumnya.
	 *
	 * @param tanggalPindah tanggal kepindahan dari sekolah sebelumnya
	 */
	public void setTanggalPindah(Date tanggalPindah) {
		this.tanggalPindah = tanggalPindah;
	}

	/**
	 * Alasan/keterangan kepindahan (kolom {@code text}).
	 *
	 * <p><b>Getter DESTRUKTIF:</b> bila {@link #getMerupakanPindahan()} bernilai {@code false},
	 * field DIKOSONGKAN dan penghapusan itu ikut tersimpan. Seluruh badan method dibungkus
	 * {@code try/catch} yang hanya menampilkan error kepada admin, sehingga kegagalan membaca
	 * penanda pindahan tidak menggagalkan pembacaan entity &mdash; tetapi juga berarti nilai lama
	 * bisa terhapus tanpa ada yang menyadari.</p>
	 *
	 * @return keterangan kepindahan; string kosong bila calon siswa bukan pindahan
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganPindah() {
		try {
			if (!getMerupakanPindahan()) {
				keteranganPindah = "";
			}
			if (keteranganPindah == null) {
				keteranganPindah = "";
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return keteranganPindah;
	}

	/**
	 * Menetapkan alasan/keterangan kepindahan.
	 *
	 * @param keteranganPindah alasan/keterangan kepindahan
	 */
	public void setKeteranganPindah(String keteranganPindah) {
		this.keteranganPindah = keteranganPindah;
	}

	/**
	 * Nama sekolah asal kepindahan (kolom {@code text}).
	 *
	 * <p><b>Getter DESTRUKTIF:</b> dikosongkan permanen bila {@link #getMerupakanPindahan()}
	 * bernilai {@code false}. Lihat catatan pada {@link #getMerupakanPindahan()}.</p>
	 *
	 * <p>Berbeda dari {@link #getSekolahAsal()} yang berlaku untuk semua pendaftar, properti ini
	 * khusus untuk jalur pindahan dan diisi terpisah.</p>
	 *
	 * @return nama sekolah asal kepindahan; string kosong bila bukan pindahan
	 */
	@Column(columnDefinition = "text")
	public String getPindahanDariSekolah() {

		if (!getMerupakanPindahan()) {
			pindahanDariSekolah = "";
		}

		return pindahanDariSekolah == null ? "" : pindahanDariSekolah.trim();
	}

	/**
	 * Menetapkan nama sekolah asal kepindahan.
	 *
	 * @param pindahanDariSekolah nama sekolah asal kepindahan
	 */
	public void setPindahanDariSekolah(String pindahanDariSekolah) {
		this.pindahanDariSekolah = pindahanDariSekolah;
	}

	/**
	 * Alamat sekolah asal kepindahan (kolom {@code text}).
	 *
	 * <p><b>Getter DESTRUKTIF:</b> dikosongkan permanen bila {@link #getMerupakanPindahan()}
	 * bernilai {@code false}.</p>
	 *
	 * @return alamat sekolah asal kepindahan; string kosong bila bukan pindahan
	 */
	@Column(columnDefinition = "text")
	public String getAlamatSekolahPindahan() {
		if (!getMerupakanPindahan()) {
			alamatSekolahPindahan = "";
		}

		return alamatSekolahPindahan == null ? "" : alamatSekolahPindahan.trim();
	}

	/**
	 * Menetapkan alamat sekolah asal kepindahan.
	 *
	 * @param alamatSekolahPindahan alamat sekolah asal kepindahan
	 */
	public void setAlamatSekolahPindahan(String alamatSekolahPindahan) {
		this.alamatSekolahPindahan = alamatSekolahPindahan;
	}

	/**
	 * Kelas terakhir yang ditempuh di sekolah asal kepindahan.
	 *
	 * <p><b>Getter DESTRUKTIF:</b> dikosongkan permanen bila {@link #getMerupakanPindahan()}
	 * bernilai {@code false}.</p>
	 *
	 * @return kelas terakhir di sekolah asal; string kosong bila bukan pindahan
	 */
	public String getKelasSekolahPindahan() {
		if (!getMerupakanPindahan()) {
			kelasSekolahPindahan = "";
		}

		return kelasSekolahPindahan == null ? "" : kelasSekolahPindahan.trim();
	}

	/**
	 * Menetapkan kelas terakhir di sekolah asal kepindahan.
	 *
	 * @param kelasSekolahPindahan kelas terakhir di sekolah asal kepindahan
	 */
	public void setKelasSekolahPindahan(String kelasSekolahPindahan) {
		this.kelasSekolahPindahan = kelasSekolahPindahan;
	}

	/**
	 * Pegawai yayasan yang merupakan orang tua calon siswa (kolom {@code orang_tua_pegawai}),
	 * dipakai pada gelombang khusus anak pegawai.
	 *
	 * <p>Relasi ini memasok nilai bagi {@link #getNamaAyah()}, {@link #getNamaIbu()},
	 * {@link #getTanggalLahirAyah()}, {@link #getTanggalLahirIbu()},
	 * {@link #getTempatLahirAyah()}, dan {@link #getTempatLahirIbu()} &mdash; keenamnya menimpa
	 * kolomnya sendiri dengan nilai dari master kepegawaian.</p>
	 *
	 * <p><b>Getter DESTRUKTIF.</b> Tiga langkahnya:</p>
	 * <ol>
	 * <li>Memulihkan objek lepas-session lewat {@code check(&hellip;)}.</li>
	 * <li>Bila {@link #getKeluarga()} terisi, pegawai diambil dari relasi keluarga (menimpa field).</li>
	 * <li>Bila gelombang pendaftaran TIDAK ditandai {@code hanyaUntukAnakPegawai}, field
	 * di-{@code null}-kan. Blok {@code catch} juga meng-{@code null}-kan field pada exception
	 * apa pun &mdash; termasuk kegagalan sesaat memuat gelombang.</li>
	 * </ol>
	 *
	 * <p>Langkah 3 berarti melepas satu centang di master {@link GelombangPendaftaranPsb} akan
	 * MEMUTUS PERMANEN tautan anak-pegawai pada SELURUH pendaftar gelombang itu, satu per satu,
	 * begitu barisnya tersentuh oleh layar atau laporan mana pun. Mencentangnya kembali tidak
	 * memulihkan apa pun. Sifat "fail-close pada exception" juga membuat gangguan sesaat pada
	 * database berujung penghapusan data.</p>
	 *
	 * @return pegawai orang tua, atau {@code null} bila gelombang bukan gelombang anak pegawai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "orang_tua_pegawai", nullable = true)
	public Pegawai getOrangTuaPegawai() {
		orangTuaPegawai = check(orangTuaPegawai);

		if (getKeluarga() != null && getKeluarga().getPegawai() != null) {
			orangTuaPegawai = getKeluarga().getPegawai();
		}

		try {
			gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
			if (gelombangPendaftaranPsb != null && !gelombangPendaftaranPsb.getHanyaUntukAnakPegawai()) {
				orangTuaPegawai = null;
			}
		} catch (Exception e) {
			orangTuaPegawai = null;
		}

		return orangTuaPegawai;
	}

	/**
	 * Menetapkan pegawai yayasan yang merupakan orang tua calon siswa.
	 *
	 * @param orangTuaPegawai pegawai yayasan yang merupakan orang tua calon siswa
	 */
	public void setOrangTuaPegawai(Pegawai orangTuaPegawai) {
		this.orangTuaPegawai = orangTuaPegawai;
	}

	/**
	 * Penampung bebas untuk field tambahan hasil kustomisasi per instalasi (kolom
	 * {@code fields_generic}, tipe Hibernate {@code text}).
	 *
	 * <p>Isinya TIDAK ditafsirkan sama sekali oleh kelas ini; formatnya ditentukan sepenuhnya oleh
	 * layar/laporan kustom yang menulisinya. Berbeda dari {@link #getParameterTambahan()} yang punya
	 * format baku dan pengurai sendiri, kolom ini benar-benar tak berstruktur.</p>
	 *
	 * <p>Perhatikan penulisan anotasinya yang memakai nama berkualifikasi penuh
	 * ({@code @javax.persistence.Column}, {@code @org.hibernate.annotations.Type}) &mdash; gaya ini
	 * sengaja dipakai untuk menghindari bentrok impor, bukan kelalaian.</p>
	 *
	 * @return isi penampung bebas, atau {@code null}
	 */
	@javax.persistence.Column(name = "fields_generic")
	@org.hibernate.annotations.Type(type = "text")
	public String getFieldsGeneric() {
		return fieldsGeneric;
	}

	/**
	 * Menetapkan penampung bebas untuk field tambahan hasil kustomisasi instalasi.
	 *
	 * @param fieldsGeneric penampung bebas untuk field tambahan hasil kustomisasi instalasi
	 */
	public void setFieldsGeneric(String fieldsGeneric) {
		this.fieldsGeneric = fieldsGeneric;
	}

	/**
	 * Mengumpulkan SELURUH nomor telepon dan WhatsApp yang terkait calon siswa ke dalam satu
	 * himpunan tanpa duplikat.
	 *
	 * <p>Sumbernya lima belas properti: {@link #getTeleponSiswa()},
	 * {@link #getTeleponOrangTua()}, {@link #getTeleponWali()}, {@code hp1}&ndash;{@code hp3} ayah,
	 * ibu, dan wali, serta WhatsApp ayah, ibu, dan wali. Nilai kosong dilewati; nilai dipangkas
	 * spasi tepinya.</p>
	 *
	 * <p>Dipakai sebagai daftar tujuan pengiriman notifikasi WhatsApp/SMS PSB dan sebagai daftar
	 * nomor yang boleh membalas pesan masuk.</p>
	 *
	 * <p><b>Efek samping yang tidak jelas dari namanya:</b> method ini memanggil lima belas getter,
	 * sepuluh di antaranya menulis balik ke field ({@link #getTeleponSiswa()},
	 * {@link #getTeleponOrangTua()}, {@link #getTeleponWali()}, {@link #getHp1ayah()},
	 * {@link #getHp1ibu()}, {@link #getWaAyah()}, {@link #getWaIbu()}, dan tiga getter HP wali yang
	 * memanggil getter HP ayah). Memanggil {@code ambilTelp()} pada entity terkelola karena itu
	 * dapat menyalin nomor dari alumni dan menuliskan string kosong ke kolom WhatsApp.</p>
	 *
	 * <p><b>Kuirk:</b> nomor dikumpulkan dalam bentuk apa adanya, tanpa normalisasi kode negara,
	 * sehingga {@code "08123"} dan {@code "628123"} dihitung sebagai dua nomor berbeda. Nomor
	 * placeholder ({@code "0000000000"} dan sejenisnya) TIDAK disaring di sini, berbeda dari
	 * {@link #ambilHp()} dan {@link #tampilkanHp(org.zkoss.zk.ui.Component)}.</p>
	 *
	 * @return himpunan nomor telepon yang tidak kosong; himpunan kosong bila tidak ada satu pun
	 */
	public Set<String> ambilTelp() {
		Set<String> froms = new HashSet<String>();
		CalonSiswa calonSiswa = this;
		if (calonSiswa.getTeleponSiswa() != null && !calonSiswa.getTeleponSiswa().trim().isEmpty()) {
			froms.add(calonSiswa.getTeleponSiswa().trim());
		}

		if (calonSiswa.getHp1ayah() != null && !calonSiswa.getHp1ayah().trim().isEmpty()) {
			froms.add(calonSiswa.getHp1ayah().trim());
		}

		if (calonSiswa.getHp2ayah() != null && !calonSiswa.getHp2ayah().trim().isEmpty()) {
			froms.add(calonSiswa.getHp2ayah().trim());
		}

		if (calonSiswa.getHp3ayah() != null && !calonSiswa.getHp3ayah().trim().isEmpty()) {
			froms.add(calonSiswa.getHp3ayah().trim());
		}

		if (calonSiswa.getHp1ibu() != null && !calonSiswa.getHp1ibu().trim().isEmpty()) {
			froms.add(calonSiswa.getHp1ibu().trim());
		}

		if (calonSiswa.getHp2ibu() != null && !calonSiswa.getHp2ibu().trim().isEmpty()) {
			froms.add(calonSiswa.getHp2ibu().trim());
		}

		if (calonSiswa.getHp3ibu() != null && !calonSiswa.getHp3ibu().trim().isEmpty()) {
			froms.add(calonSiswa.getHp3ibu().trim());
		}

		if (calonSiswa.getHp1wali() != null && !calonSiswa.getHp1wali().trim().isEmpty()) {
			froms.add(calonSiswa.getHp1wali().trim());
		}

		if (calonSiswa.getHp2wali() != null && !calonSiswa.getHp2wali().trim().isEmpty()) {
			froms.add(calonSiswa.getHp2wali().trim());
		}

		if (calonSiswa.getHp3wali() != null && !calonSiswa.getHp3wali().trim().isEmpty()) {
			froms.add(calonSiswa.getHp3wali().trim());
		}

		if (calonSiswa.getTeleponOrangTua() != null && !calonSiswa.getTeleponOrangTua().trim().isEmpty()) {
			froms.add(calonSiswa.getTeleponOrangTua().trim());
		}

		if (calonSiswa.getTeleponWali() != null && !calonSiswa.getTeleponWali().trim().isEmpty()) {
			froms.add(calonSiswa.getTeleponWali().trim());
		}

		if (calonSiswa.getWaAyah() != null && !calonSiswa.getWaAyah().trim().isEmpty()) {
			froms.add(calonSiswa.getWaAyah().trim());
		}
		if (calonSiswa.getWaIbu() != null && !calonSiswa.getWaIbu().trim().isEmpty()) {
			froms.add(calonSiswa.getWaIbu().trim());
		}
		if (calonSiswa.getWaWali() != null && !calonSiswa.getWaWali().trim().isEmpty()) {
			froms.add(calonSiswa.getWaWali().trim());
		}
		return froms;
	}

	/**
	 * Membangkitkan tagihan pendaftaran yang sesuai lalu membuka jendela pembayaran daring untuk
	 * calon siswa ini.
	 *
	 * <p>Dijalankan lewat {@code Common.createDefaultTimer(&hellip;, 2000)} &mdash; ditunda dua detik
	 * agar layar pemanggil sempat selesai merender. Tiga cabangnya, saling eksklusif, dipilih
	 * berdasarkan pengaturan gelombang:</p>
	 * <ol>
	 * <li><b>Gelombang {@code sesuaiKelas} dan ada kelas les terpilih.</b> Memuat setiap
	 * {@link PengaturanBiaya} yang terkait kelas les pilihan pendaftar lalu memanggil
	 * {@code TagihanUtilCalonSiswa.getTagihan(&hellip;)} untuk bulan dan tahun berjalan &mdash;
	 * yang MEMBANGKITKAN baris {@link Tagihan} bila belum ada.</li>
	 * <li><b>Belum {@link #getTerverifikasi() terverifikasi}.</b> Membangkitkan tagihan insidentil
	 * dari {@code jenisBiayaSekolah} gelombang.</li>
	 * <li><b>Sudah terverifikasi.</b> Membangkitkan tagihan insidentil dari
	 * {@code jenisBiayaSekolahTerverifikasi} gelombang &mdash; inilah mekanisme "biaya berbeda
	 * setelah lolos verifikasi".</li>
	 * </ol>
	 *
	 * <p>Setelah itu jendela {@code
	 * /pages/master/sekolah/pembayaran_online.zul?calon_siswa=<id>&amp;langsungBayar=true}
	 * dibuka. Perhatikan bahwa id entity ini masuk ke URL layar tersebut sebagai parameter biasa.</p>
	 *
	 * <p><b>Hal yang perlu diketahui:</b> cabang pertama masih meninggalkan
	 * {@code System.out.println} diagnostik yang mencetak setiap pengaturan biaya dan tagihan ke log
	 * server. Bila tidak satu pun dari ketiga syarat terpenuhi (mis. gelombang tanpa jenis biaya),
	 * method selesai TANPA membuka jendela apa pun dan tanpa pesan &mdash; dari sisi pengguna
	 * tombolnya tampak tidak berfungsi.</p>
	 *
	 * @param eventListener listener yang dipanggil saat jendela pembayaran ditutup
	 */
	public void munculkanFormPembayaran(final EventListener eventListener) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				CalonSiswa calonSiswa = CalonSiswa.this;
				if (calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getSesuaiKelas()
						&& !calonSiswa.ambilKelasLesSiswaId().isEmpty()) {

					List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(PengaturanBiaya.class)
									.add(Restrictions.in("kelasLesSiswa.id", calonSiswa.ambilKelasLesSiswaId())),
							PengaturanBiaya.class);
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
						List<Tagihan> subTagihans = TagihanUtilCalonSiswa.getTagihan(
								pengaturanBiaya.getJenisBiayaSekolah(), pengaturanBiaya, calonSiswa,
								calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR), true);
						System.out.println("pengaturanBiaya -> " + pengaturanBiaya + ", subTagihans -> " + subTagihans);
					}
					Common.displayWindow("/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId()
							+ "&langsungBayar=true", true, "95%", "95%", eventListener, "", false);
				}

				else if (calonSiswa.getGelombangPendaftaranPsb() != null && !calonSiswa.getTerverifikasi()
						&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah() != null) {

					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah(), true);

					Common.displayWindow(
							"/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId()
									+ "&langsungBayar=true",
							true, "95%", Common.isMobile() ? "100%" : "950px", eventListener, "", false);
				} else if (calonSiswa.getGelombangPendaftaranPsb() != null && calonSiswa.getTerverifikasi()
						&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi() != null) {

					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi(), true);

					Common.displayWindow(
							"/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId()
									+ "&langsungBayar=true",
							true, "95%", Common.isMobile() ? "100%" : "950px", eventListener, "", false);
				}
			}
		}, "", false, 2000);
	}

	/**
	 * Mengembalikan penanda berkas dan data calon siswa sudah diverifikasi panitia.
	 *
	 * <p>Tanpa anotasi {@code @Column} eksplisit, sehingga nama kolomnya sama dengan nama properti
	 * (dilipat menjadi huruf kecil oleh PostgreSQL).</p>
	 *
	 * <p>Getter mengganti {@code null} menjadi {@code false}. Nilai ini menentukan jenis biaya mana yang
	 * dipakai saat membangkitkan tagihan pendaftaran (lihat {@link
	 * #munculkanFormPembayaran(org.zkoss.zk.ui.event.EventListener)}).</p>
	 *
	 * @return Penanda berkas dan data calon siswa sudah diverifikasi panitia
	 */
	public Boolean getTerverifikasi() {
		return terverifikasi == null ? false : terverifikasi;
	}

	/**
	 * Menetapkan penanda berkas dan data calon siswa sudah diverifikasi panitia.
	 *
	 * @param terverifikasi penanda berkas dan data calon siswa sudah diverifikasi panitia
	 */
	public void setTerverifikasi(Boolean terverifikasi) {
		this.terverifikasi = terverifikasi;
	}

	/**
	 * Mengembalikan baris keluarga pegawai yang menautkan calon siswa ke orang tuanya.
	 *
	 * <p>Dipetakan ke kolom {@code keluarga}, relasi {@code @ManyToOne} yang dimuat malas ({@code LAZY})
	 * dengan {@code CascadeType.PERSIST} dan {@code MERGE}.</p>
	 *
	 * <p>Bila terisi, {@link #getOrangTuaPegawai()} mengambil pegawai dari relasi ini dan mengabaikan
	 * nilai kolomnya sendiri.</p>
	 *
	 * @return Baris keluarga pegawai yang menautkan calon siswa ke orang tuanya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "keluarga", nullable = true)
	public Keluarga getKeluarga() {
		keluarga = check(keluarga);
		return keluarga;
	}

	/**
	 * Menetapkan baris keluarga pegawai yang menautkan calon siswa ke orang tuanya.
	 *
	 * @param keluarga baris keluarga pegawai yang menautkan calon siswa ke orang tuanya
	 */
	public void setKeluarga(Keluarga keluarga) {
		this.keluarga = keluarga;
	}

	/**
	 * Alias baca-saja KETIGA atas kolom {@code nomor_induk}, disediakan untuk laporan dan
	 * template yang memakai penamaan {@code nopendaftaran}.
	 *
	 * <p>Sama seperti {@link #getNomorInduk()}, dipetakan dengan
	 * {@code insertable = false, updatable = false} sehingga {@link #setNopendaftaran(String)} tidak
	 * pernah tersimpan. Berbeda dari {@link #getNomorInduk()}, getter ini TIDAK menyalin nilai dari
	 * {@link #getNoRegistrasi()} &mdash; ia mengembalikan apa pun yang dimuat Hibernate, dan
	 * {@code null} pada objek yang dibuat lewat konstruktor.</p>
	 *
	 * @return nomor pendaftaran sebagaimana dimuat Hibernate, atau {@code null}
	 */
	@Column(name = "nomor_induk", nullable = false, insertable = false, updatable = false)
	public String getNopendaftaran() {
		return nopendaftaran;
	}

	/**
	 * Menetapkan alias baca-saja nomor pendaftaran.
	 *
	 * @param nopendaftaran alias baca-saja nomor pendaftaran
	 */
	public void setNopendaftaran(String nopendaftaran) {
		this.nopendaftaran = nopendaftaran;
	}
}
