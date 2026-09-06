package ais.database.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataSemuaMahasiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kelas dasar legacy untuk entity yang mendukung parameter tambahan dinamis. Ejaan {@code Astract}
 * dipertahankan demi kompatibilitas; tipe ini menjadi satu sumber kontrak penyimpanan dan
 * pembacaan nilai tambahan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String TIDAK_ADA}, {@code String TEXT},
 * {@code String ANGKA}, {@code String TEXT_ANGKA}, {@code String TANGGAL}, {@code String TANGGAL_DAN_WAKTU},
 * {@code String WAKTU}, {@code String PILIHAN_YA_TIDAK}; inisialisasi/lifecycle ({@code initComponent()}, {@code
 * initComponent()}, {@code initComponent()}); pembacaan/pencarian ({@code tampil()}, {@code
 * ambilNilaiComponent()}, {@code ambilComponent()}, {@code ambilComponent()}, {@code ambilComponentCustom()},
 * {@code ambilVal()}); mutasi data ({@code parseTanggalAman()}); operasi domain lain ({@code nzp()}, {@code
 * rangkaiAlamatPenyedia()}, {@code rangkaiJenisPekerjaanPenyedia()}, {@code nilaiPenyediaUntukLabel()}, {@code
 * isiOtomatisParameterTerkaitPenyedia()}, {@code reevaluasiSkipLogic()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * <p><b>Relasi dengan {@link ParameterTambahan} — SATU hierarki, bukan dua.</b> {@code ParameterTambahan}
 * meng-{@code extends} kelas ini. Pembagian perannya tidak lazim dan sering disalahpahami sebagai
 * "abstract vs entity nilai":</p>
 * <ul>
 * <li>Kelas ini <b>tidak menyimpan satu field instance pun</b>. Isinya hanya (a) konstanta {@code String}
 * penanda tipe inputan dan (b) sekumpulan method {@code static} pembangun/pembaca komponen ZK. Pewarisan
 * dipakai sebagai <i>constant interface</i>: berkat {@code extends}, kode lain boleh menulis
 * {@code ParameterTambahan.PILIHAN_CUSTOM} walau konstanta itu milik kelas ini.</li>
 * <li>{@link ParameterTambahan} memegang seluruh field DEFINISI parameter dan dipetakan ke tabel
 * {@code public.parameter_tambahan}.</li>
 * <li>NILAI isian tidak disimpan di kedua kelas ini. Nilai milik entity pemakai disimpan pada kolom teks
 * banyak-baris (lazimnya {@code parameterTambahanInds}) di entity pemakai itu sendiri, satu baris per
 * parameter berformat {@code kelId->ptId<=>nilai<=>url<=>keterangan}. Pembacanya adalah
 * {@link ParameterTambahan#masukkanSemuaParameterKeMap(String, java.util.Map)}.</li>
 * </ul>
 *
 * <p><b>Kelas ini adalah PABRIK KOMPONEN, bukan model data.</b> Meski berada di paket
 * {@code ais.database.model}, seluruh method publiknya {@code static} dan bekerja pada komponen ZK
 * ({@code Row}, {@code Rows}, {@code Component}). Tiga peran utamanya:</p>
 * <ol>
 * <li><b>Membangun</b> — {@link #initComponent(org.zkoss.zul.Row, org.zkoss.zul.Rows, String,
 * java.util.List, java.util.Map, Long, String, String, ParameterTambahan,
 * org.zkoss.zk.ui.event.EventListener, boolean, String)} dan {@link #ambilComponent(String,
 * ParameterTambahan, org.zkoss.zk.ui.event.EventListener, boolean)} menerjemahkan satu definisi parameter
 * menjadi baris form beserta komponen isiannya.</li>
 * <li><b>Membaca balik</b> — {@link #ambilVal(org.zkoss.zul.Row, ParameterTambahan)} dan
 * {@link #ambilValComponent(org.zkoss.zk.ui.Component, ParameterTambahan)} mengubah isi komponen kembali
 * menjadi {@code String} untuk disimpan.</li>
 * <li><b>Menampilkan</b> — {@link #tampil(org.zkoss.zul.Vbox, ParameterTambahan,
 * ais.database.model.file.LampiranLain, String)} merender nilai tersimpan dalam mode hanya-baca.</li>
 * </ol>
 *
 * <p><b>Titik paling penting untuk dipahami:</b> {@code initComponent} adalah tempat kunci namespace
 * lampiran ({@code jenis}) DIPAKAI, dan merupakan akar {@code task_484d4bd0}. Javadoc method tersebut
 * memuat penjelasan definitif mekanismenya beserta peta pemanggil yang sudah aman dan yang belum.</p>
 *
 * @see GeneralValueObject
 * @see ParameterTambahan
 */
public abstract class ParameterTambahanAstract extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Kelas ini tidak punya field instance, jadi nilainya semata memenuhi kontrak
	 * {@code Serializable} yang diwarisi dari {@link GeneralValueObject}.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Tipe: parameter TANPA komponen isian.
	 *
	 * <p>Dipakai untuk butir yang perannya murni JUDUL bagi butir-butir turunan yang menunjuknya lewat
	 * {@link ParameterTambahan#getParent()}. Karena tidak ada cabang penanganan untuk konstanta ini di
	 * {@link #ambilComponent(String, ParameterTambahan, org.zkoss.zk.ui.event.EventListener, boolean)},
	 * method itu jatuh ke cabang terakhir dan mengembalikan {@code null} — dan {@code null} itulah yang
	 * membuat {@code initComponent} melewatkan pembuatan komponen, keterangan, maupun lampiran untuk baris
	 * tersebut. Jadi "tidak ada isian" dicapai lewat ketiadaan cabang, bukan lewat cabang khusus.
	 */
	public static final String TIDAK_ADA = "Tidak ada data yang diinput";
	/** Tipe: teks bebas. Dirender {@code Textbox} dengan {@code rows} dan {@code maxlength} dari definisi. */
	public static final String TEXT = "Berupa teks";
	/**
	 * Tipe: angka. Dirender {@code MyDoublebox} dengan penjaga batas
	 * {@link ParameterTambahan#getNilaiMin()}/{@link ParameterTambahan#getNilaiMax()} pada {@code onChange}.
	 * Nilai disimpan sebagai teks hasil {@code Double.toString}.
	 */
	public static final String ANGKA = "Berupa numerik / angka";
	/** Tipe: teks yang hanya menerima karakter angka. Dirender {@code MyTextboxAngka}; disimpan apa adanya. */
	public static final String TEXT_ANGKA = "Berupa teks / angka";
	/** Tipe: tanggal. Dirender {@code MyDatebox}; disimpan memakai {@code Common.dateFormat1}. */
	public static final String TANGGAL = "Berupa tanggal";
	/** Tipe: tanggal beserta jam. Dirender {@code MyDatebox} berformat {@code Common.dateFormat}. */
	public static final String TANGGAL_DAN_WAKTU = "Berupa tanggal dan waktu";
	/** Tipe: jam saja. Dirender {@code MyTimebox}; disimpan memakai {@code Common.timeFormat}. */
	public static final String WAKTU = "Berupa waktu";
	/** Tipe: pilihan ya/tidak. Dirender {@code Combobox} dua butir; disimpan sebagai {@code "true"}/{@code "false"}. */
	public static final String PILIHAN_YA_TIDAK = "Berupa pilihan ya/tidak";
	/**
	 * Tipe: pilihan dari daftar yang ditulis pengelola pada
	 * {@link ParameterTambahan#getNilaiDataInputan()}, dipisah {@code ";"}, tiap butir boleh
	 * {@code "label:nilai"}. Ini juga tipe DEFAULT yang ditulis
	 * {@link ParameterTambahan#getTipeDataInputan()} bila kolomnya masih {@code null}.
	 */
	public static final String PILIHAN_CUSTOM = "Berupa pilihan custom";
	/**
	 * Tipe: pemilih entity generik. {@link ParameterTambahan#getNilaiDataInputan()} berisi nama kelas
	 * berkualifikasi penuh yang di-{@code Class.forName}, dan
	 * {@link ParameterTambahan#getKondisiDataInputan()} menjadi penyaring SQL mentah. Nilai disimpan
	 * sebagai id, dengan {@code "-1"} bermakna "tidak ada yang dipilih".
	 */
	public static final String PILIHAN_OBJECT = "Berupa pilihan data";
	/**
	 * Tipe: banyak pilihan (checkbox). Nilai terpilih dirangkai kembali menjadi satu {@code String} yang
	 * dipisah {@code ";"} — sehingga butir pilihan yang teksnya sendiri mengandung {@code ";"} akan rancu.
	 */
	public static final String PILIHAN_BANYAK = "Berupa banyak pilihan";
	/**
	 * Tipe: matriks satu-jawaban. Definisinya satu baris per baris matriks berformat
	 * {@code "namaBaris->kol1:nilai1;kol2:nilai2"}. Hanya SATU sel di seluruh matriks yang bisa terpilih,
	 * dan yang disimpan hanyalah nilai sel itu — bukan posisinya.
	 */
	public static final String PILIHAN_MATRIX = "Berupa pilihan matrix";
	/**
	 * Tipe: matriks banyak-jawaban. Sama seperti {@link #PILIHAN_MATRIX} tetapi setiap BARIS punya
	 * jawabannya sendiri, dan seluruh jawaban disimpan sebagai satu dokumen JSON
	 * {@code {"namabaris":{"kolom":"nilai"}}} dengan nama baris dikecilkan hurufnya.
	 */
	public static final String PILIHAN_MATRIX_BANYAK_NILAI = "Berupa pilihan matrix banyak nilai";
	/**
	 * Tipe: matriks dengan satu {@code Combobox} per baris. Penyimpanannya JSON
	 * {@code {"namabaris":"nilai"}} — perhatikan bentuknya BERBEDA dari
	 * {@link #PILIHAN_MATRIX_BANYAK_NILAI} yang bersarang dua tingkat, sehingga mengganti tipe sebuah
	 * parameter di antara keduanya membuat nilai lama tidak terbaca.
	 */
	public static final String PILIHAN_MATRIX_BANYAK_COMBO = "Berupa pilihan matrix salah satu nilai";

	/**
	 * Tipe: pemilih mahasiswa. Termasuk tujuh tipe "pemilih entity khusus" yang terdaftar di
	 * {@link #CUSTOM_PILIHAN} dan ditangani {@link #ambilComponentCustom(String, ParameterTambahan,
	 * org.zkoss.zk.ui.event.EventListener)}.
	 *
	 * <p>Nilai ketujuh tipe ini disimpan dalam bentuk GABUNGAN {@code "<id>-><label>"}, bukan id saja —
	 * label ikut disimpan agar laporan tetap bisa menampilkan nama walau entity aslinya kelak terhapus.
	 * Saat membangun ulang komponen, {@code ambilComponentCustom} memotong pada {@code "->"} dan hanya
	 * memakai bagian id.</p>
	 */
	public static final String PILIHAN_MAHASISWA = "Berupa data mahasiswa";
	/** Tipe: pemilih siswa. Lihat catatan format {@code "<id>-><label>"} pada {@link #PILIHAN_MAHASISWA}. */
	public static final String PILIHAN_SISWA = "Berupa data siswa";
	/** Tipe: pemilih dosen. Lihat catatan format {@code "<id>-><label>"} pada {@link #PILIHAN_MAHASISWA}. */
	public static final String PILIHAN_DOSEN = "Berupa data dosen";
	/** Tipe: pemilih guru. Lihat catatan format {@code "<id>-><label>"} pada {@link #PILIHAN_MAHASISWA}. */
	public static final String PILIHAN_GURU = "Berupa data guru";
	/** Tipe: pemilih pegawai. Lihat catatan format {@code "<id>-><label>"} pada {@link #PILIHAN_MAHASISWA}. */
	public static final String PILIHAN_PEGAWAI = "Berupa data pegawai";
	/**
	 * Tipe: pemilih penyedia/vendor ({@code PenyediaAsset}).
	 *
	 * <p>Satu-satunya tipe yang memicu KORELASI ANTAR-PARAMETER: begitu vendor dipilih,
	 * {@link #isiOtomatisParameterTerkaitPenyedia(ParameterTambahan, org.zkoss.zk.ui.Component,
	 * java.util.List)} mengisi otomatis parameter teks lain yang se-konteks. Juga satu-satunya tipe yang
	 * ditangani khusus {@link #tampil(org.zkoss.zul.Vbox, ParameterTambahan,
	 * ais.database.model.file.LampiranLain, String)}, yang menampilkan bagian LABEL saja dan menyembunyikan
	 * id-nya.</p>
	 */
	public static final String PILIHAN_PENYEDIA = "Berupa data penyedia";
	/** Tipe: pemilih kelas siswa. Lihat catatan format {@code "<id>-><label>"} pada {@link #PILIHAN_MAHASISWA}. */
	public static final String PILIHAN_KELAS_SISWA = "Berupa data kelas siswa";

	/**
	 * Daftar tipe yang penanganannya didelegasikan ke
	 * {@link #ambilComponentCustom(String, ParameterTambahan, org.zkoss.zk.ui.event.EventListener)} —
	 * yaitu ketujuh pemilih entity khusus ({@code PILIHAN_MAHASISWA} sampai
	 * {@code PILIHAN_KELAS_SISWA}).
	 *
	 * <p>Dipakai sebagai cabang TERAKHIR pada rantai {@code if/else} di
	 * {@link #ambilComponent(String, ParameterTambahan, org.zkoss.zk.ui.event.EventListener, boolean)}:
	 * apa pun yang tidak cocok dengan tipe lain tetapi ada di daftar ini diserahkan ke
	 * {@code ambilComponentCustom}.</p>
	 *
	 * <p><b>Peringatan: koleksi ini {@code public static} dan BISA DIUBAH.</b> Ia hanya {@code final} pada
	 * rujukannya, bukan isinya, dan tidak dibungkus {@code Collections.unmodifiableList}. Kode mana pun
	 * dalam JVM yang sama dapat menambah atau menghapus isinya, dan perubahan itu berlaku global bagi
	 * seluruh penyewa (tenant) serta seluruh permintaan. Perlakukan sebagai hanya-baca.</p>
	 */
	public static final List<String> CUSTOM_PILIHAN = new ArrayList<String>();
	/** Mengisi {@link #CUSTOM_PILIHAN} dengan ketujuh tipe pemilih entity khusus saat kelas dimuat. */
	static {
		CUSTOM_PILIHAN.add(PILIHAN_MAHASISWA);
		CUSTOM_PILIHAN.add(PILIHAN_SISWA);
		CUSTOM_PILIHAN.add(PILIHAN_DOSEN);
		CUSTOM_PILIHAN.add(PILIHAN_GURU);
		CUSTOM_PILIHAN.add(PILIHAN_PEGAWAI);
		CUSTOM_PILIHAN.add(PILIHAN_PENYEDIA);
		CUSTOM_PILIHAN.add(PILIHAN_KELAS_SISWA);
	}

	/**
	 * Merender NILAI tersimpan sebuah parameter dalam mode HANYA-BACA ke dalam sebuah {@code Vbox}.
	 *
	 * <p>Jalur ini terpisah dari {@link #ambilComponent(String, ParameterTambahan,
	 * org.zkoss.zk.ui.event.EventListener, boolean)}: yang itu membangun komponen yang (mungkin) bisa
	 * disunting, sedangkan method ini semata memformat nilai untuk dibaca — dipakai daftar, rekap, dan
	 * pratinjau.</p>
	 *
	 * <p><b>Lampiran menang atas nilai.</b> Bila {@code lampiranLain} tidak {@code null}, isi
	 * {@code vall} TIDAK ditampilkan sama sekali; yang muncul hanyalah tombol unduh. Label tombol memakai
	 * {@code vall} bila ada isinya, atau {@code lampiranLain.getUrl()} bila kosong. Tombol itu bercabang
	 * saat diklik: berkas ber-Google Drive dialihkan ke {@code downloadGDriveUrl()} pada tab baru,
	 * selebihnya diserahkan ke {@code Common.display(...)}.</p>
	 *
	 * <p>Tanpa lampiran, pemformatan bergantung pada {@link ParameterTambahan#getTipeDataInputan()}:</p>
	 * <ul>
	 * <li>{@link #PILIHAN_PENYEDIA} — nilai berbentuk {@code "<id>-><label>"} dipotong dan HANYA bagian
	 * label yang ditampilkan, sehingga id internal tidak bocor ke layar. Perlu dicatat penanganan ini
	 * khusus penyedia saja; enam tipe pemilih entity lain memakai format gabungan yang sama tetapi jatuh ke
	 * cabang terakhir sehingga id-nya IKUT TERLIHAT beserta tanda {@code "->"}.</li>
	 * <li>{@link #ANGKA} dan {@link #TEXT_ANGKA} — diformat {@code Common.numberFormat}; nilai kosong
	 * ditampilkan sebagai {@code 0}.</li>
	 * <li>{@link #TANGGAL} dan {@link #TANGGAL_DAN_WAKTU} — diurai memakai format simpan lalu ditampilkan
	 * memakai format baca ({@code dateFormat6}/{@code dateFormat61}).</li>
	 * <li>selebihnya — ditampilkan apa adanya.</li>
	 * </ul>
	 *
	 * <p>Setiap cabang pemformatan dibungkus {@code try/catch} yang, bila penguraian gagal, menampilkan
	 * teks MENTAH-nya. Jadi nilai lama yang formatnya tidak lagi cocok dengan tipe parameter tetap terbaca
	 * pengguna alih-alih memunculkan galat — pilihan yang disengaja karena tipe sebuah parameter bisa
	 * diubah pengelola setelah ada data.</p>
	 *
	 * @param vbox2             wadah tujuan; komponen hasil di-{@code append} ke sini.
	 * @param parameterTambahan definisi parameter; dipakai membaca tipe datanya. Tidak boleh {@code null}
	 *                          bila {@code lampiranLain} {@code null}.
	 * @param lampiranLain      lampiran terkait; bila tidak {@code null}, hanya tombol unduh yang dirender.
	 * @param vall              nilai tersimpan. Tidak boleh {@code null} pada jalur tanpa lampiran karena
	 *                          langsung di-{@code trim}.
	 */
	public static void tampil(Vbox vbox2, ParameterTambahan parameterTambahan, final LampiranLain lampiranLain,
			String vall) {

		if (lampiranLain != null) {
			String u = lampiranLain.getUrl();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(vall.trim().isEmpty() ? u : vall,
					"/img/svg/download.svg");
			button.setTooltiptext("Download " + (vall.trim().isEmpty() ? u : vall));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (lampiranLain.getGdrive() != null && !lampiranLain.getGdrive().isEmpty()) {
						ExecutionsCtrl.getCurrent().sendRedirect(lampiranLain.downloadGDriveUrl(), "_blank");
					} else {
						Common.display(lampiranLain);
					}
				}
			});
			button.setParent(vbox2);
		} else {
			if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PENYEDIA)
					&& vall != null && vall.contains("->")) {
				String[] bagian = vall.split("->", 2);
				vbox2.appendChild(new MyLabelKecil(bagian.length > 1 ? bagian[1] : vall));
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
				try {
					Double nilai = vall.trim().isEmpty() ? 0.0 : Double.parseDouble(vall);
					vbox2.appendChild(new MyLabelKecil(Common.numberFormat.get().format(nilai)));
				} catch (Exception e) {
					vbox2.appendChild(new MyLabelKecil(vall));
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL)) {
				try {
					Date nilai = vall.trim().isEmpty() ? null : Common.dateFormat1.get().parse(vall);
					vbox2.appendChild(new MyLabelKecil(Common.dateFormat6.get().format(nilai)));
				} catch (Exception e) {
					vbox2.appendChild(new MyLabelKecil(vall));
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL_DAN_WAKTU)) {
				try {
					Date nilai = vall.trim().isEmpty() ? null : Common.dateFormat.get().parse(vall);
					vbox2.appendChild(new MyLabelKecil(Common.dateFormat61.get().format(nilai)));
				} catch (Exception e) {
					vbox2.appendChild(new MyLabelKecil(vall));
				}
			} else {
				vbox2.appendChild(new MyLabelKecil(vall));
			}
		}
	}

	/**
	 * Pintasan {@code initComponent} 10 argumen: menurunkan mode hanya-baca dari ADA-TIDAKNYA peta
	 * lampiran.
	 *
	 * <p>Meneruskan ke {@link #initComponent(Row, Rows, String, List, Map, Long, String, String,
	 * ParameterTambahan, EventListener, boolean)} dengan {@code readonly = (lampiranLains == null)}.
	 * Kaidahnya: peta lampiran adalah wadah keluaran tempat berkas hasil unggah dititipkan, jadi pemanggil
	 * yang TIDAK menyediakannya dianggap tidak berniat menyunting apa pun.</p>
	 *
	 * <p><b>Penggandengan ini mudah menjebak.</b> Satu argumen memikul dua makna sekaligus — "kumpulkan
	 * lampiran ke sini" dan "form ini dapat disunting" — sehingga pemanggil yang sekadar ingin form yang
	 * bisa disunting tanpa lampiran TIDAK BISA memakai pintasan ini; ia akan diam-diam mendapat form
	 * hanya-baca. Untuk kasus itu pakailah kelebihan-beban yang menyatakan {@code readonly} secara
	 * eksplisit.</p>
	 *
	 * @param row               baris tujuan komponen isian.
	 * @param rows              wadah baris; tempat baris keterangan/lampiran ditambahkan.
	 * @param jenis             kunci namespace lampiran — lihat penjelasan definitif pada
	 *                          {@link #initComponent(Row, Rows, String, List, Map, Long, String, String,
	 *                          ParameterTambahan, EventListener, boolean, String)}.
	 * @param parameterRows     daftar seluruh baris parameter dalam form; boleh {@code null}.
	 * @param lampiranLains     peta keluaran lampiran; {@code null} berarti mode HANYA-BACA.
	 * @param ref               id entity pemilik, pasangan dari {@code jenis}.
	 * @param val               nilai tersimpan; kosong berarti memakai {@link ParameterTambahan#getNilaiDefault()}.
	 * @param ket               keterangan tersimpan.
	 * @param parameterTambahan definisi parameter.
	 * @param eventListener     pendengar perubahan tambahan; boleh {@code null}.
	 * @return {@code true} bila baris ini terlihat oleh pengguna sekarang.
	 */
	public static boolean initComponent(Row row, Rows rows, String jenis, List<Row> parameterRows,
			final Map<String, LampiranLain> lampiranLains, Long ref, String val, String ket,
			ParameterTambahan parameterTambahan, EventListener eventListener) {
		return initComponent(row, rows, jenis, parameterRows, lampiranLains, ref, val, ket, parameterTambahan,
				eventListener, lampiranLains == null);
	}

	/**
	 * Pintasan {@code initComponent} 11 argumen: memakai nama atribut komponen BAKU {@code "component"}.
	 *
	 * <p>Meneruskan ke {@link #initComponent(Row, Rows, String, List, Map, Long, String, String,
	 * ParameterTambahan, EventListener, boolean, String)} dengan {@code componenName = "component"}.
	 * Nama itu adalah kunci atribut tempat komponen isian dititipkan pada {@code Row}, dan harus SAMA
	 * dengan yang kelak dipakai {@link #ambilVal(Row, ParameterTambahan)} untuk mengambilnya kembali.
	 * Karena {@code ambilVal} juga memakai {@code "component"} sebagai bakunya, pasangan pintasan ini
	 * saling cocok tanpa perlu diatur.</p>
	 *
	 * <p>Kelebihan-beban yang menyatakan {@code componenName} sendiri hanya diperlukan bila SATU baris
	 * memuat lebih dari satu komponen isian dan masing-masing harus dibedakan.</p>
	 *
	 * @param row               baris tujuan komponen isian.
	 * @param rows              wadah baris; tempat baris keterangan/lampiran ditambahkan.
	 * @param jenis             kunci namespace lampiran — lihat penjelasan definitif pada
	 *                          {@link #initComponent(Row, Rows, String, List, Map, Long, String, String,
	 *                          ParameterTambahan, EventListener, boolean, String)}.
	 * @param parameterRows     daftar seluruh baris parameter dalam form; boleh {@code null}.
	 * @param lampiranLains     peta keluaran lampiran; boleh {@code null}.
	 * @param ref               id entity pemilik, pasangan dari {@code jenis}.
	 * @param val               nilai tersimpan.
	 * @param ket               keterangan tersimpan.
	 * @param parameterTambahan definisi parameter.
	 * @param eventListener     pendengar perubahan tambahan; boleh {@code null}.
	 * @param readonly          {@code true} untuk merender baris sebagai teks mati.
	 * @return {@code true} bila baris ini terlihat oleh pengguna sekarang.
	 */
	public static boolean initComponent(Row row, Rows rows, String jenis, List<Row> parameterRows,
			final Map<String, LampiranLain> lampiranLains, Long ref, String val, String ket,
			ParameterTambahan parameterTambahan, EventListener eventListener, boolean readonly) {
		String componenName = "component";
		return initComponent(row, rows, jenis, parameterRows, lampiranLains, ref, val, ket, parameterTambahan,
				eventListener, readonly, componenName);
	}

	/**
	 * null-safe trim untuk perakit teks penyedia.
	 *
	 * <p>Menyeragamkan {@code null} dan spasi berlebih menjadi string kosong, supaya keempat perakit teks
	 * penyedia di bawah bisa memakai {@code isEmpty()} tanpa memeriksa {@code null} berulang kali.</p>
	 *
	 * @param s teks masukan; boleh {@code null}.
	 * @return teks hasil {@code trim}, atau string kosong bila masukannya {@code null}.
	 */
	private static String nzp(String s) {
		return s == null ? "" : s.trim();
	}

	/**
	 * Rangkai alamat lengkap penyedia (alamat, kec/kota/prop, kode pos, telp, fax, kontak, email).
	 *
	 * <p>Menyusun satu baris teks dari potongan-potongan yang ADA saja: setiap bagian hanya ikut bila
	 * terisi, dan pemisah {@code ", "} baru ditambahkan bila sudah ada isi sebelumnya — sehingga tidak
	 * pernah muncul koma menggantung di awal atau koma ganda di tengah. Bagian relasi
	 * (kecamatan/kota/propinsi) diberi awalan singkat {@code "Kec."}, {@code "Kab/Kota."}, dan
	 * {@code "Prop."}.</p>
	 *
	 * <p>Perlu dicatat cakupannya lebih luas daripada namanya: selain alamat geografis, hasilnya juga
	 * memuat telepon, faks, nama kontak, dan surel. Ini disengaja karena isian yang meminta "alamat
	 * penyedia" pada dokumen pengadaan lazimnya memang blok kontak lengkap.</p>
	 *
	 * @param p penyedia sumber data; tidak boleh {@code null}.
	 * @return alamat gabungan; string kosong bila tidak satu pun bagian terisi.
	 */
	private static String rangkaiAlamatPenyedia(ais.database.model.asset.PenyediaAsset p) {
		String alamat = nzp(p.getAlamat());
		if (p.getKecamatan() != null) {
			String c = "Kec." + p.getKecamatan().getNama();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (p.getKota() != null) {
			String c = "Kab/Kota." + p.getKota().getNama();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (p.getPropinsi() != null) {
			String c = "Prop." + p.getPropinsi().getNama();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getKodePos()).isEmpty()) {
			String c = "Kode Pos " + p.getKodePos();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getTelp()).isEmpty()) {
			String c = "Telp. " + p.getTelp();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getFax()).isEmpty()) {
			String c = "Fax. " + p.getFax();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getKontak()).isEmpty()) {
			String c = "Kontak. " + p.getKontak();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getEmail()).isEmpty()) {
			String c = "Email. " + p.getEmail();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		return alamat;
	}

	/**
	 * Rangkai daftar jenis pekerjaan/barang-jasa penyedia (slot 1-5, dipisah koma).
	 *
	 * <p>{@code PenyediaAsset} menyimpan jenis pekerjaannya pada LIMA field bernomor yang terpisah, bukan
	 * pada koleksi — bentuk denormalisasi yang lazim di skema lama. Method ini menyalin kelimanya ke dalam
	 * satu larik lalu merangkai yang terisi saja, sehingga slot kosong di tengah tidak meninggalkan koma
	 * ganda. Urutan hasilnya mengikuti nomor slot, bukan abjad.</p>
	 *
	 * <p>Bila kelima slot kosong, dipakai CADANGAN berupa nama {@code jenisPenyediaAsset} — klasifikasi
	 * tunggal yang lebih kasar. Jadi hasil kosong hanya terjadi bila penyedia sama sekali tidak
	 * terklasifikasi.</p>
	 *
	 * @param p penyedia sumber data; tidak boleh {@code null}.
	 * @return daftar jenis pekerjaan dipisah {@code ", "}, nama jenis penyedia sebagai cadangan, atau
	 *         string kosong bila keduanya nihil.
	 */
	private static String rangkaiJenisPekerjaanPenyedia(ais.database.model.asset.PenyediaAsset p) {
		String jenis = "";
		ais.database.model.asset.JenisPekerjaanPenyedia[] slots = new ais.database.model.asset.JenisPekerjaanPenyedia[] {
				p.getJenisPekerjaanPenyedia1(), p.getJenisPekerjaanPenyedia2(), p.getJenisPekerjaanPenyedia3(),
				p.getJenisPekerjaanPenyedia4(), p.getJenisPekerjaanPenyedia5() };
		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null) {
				jenis += jenis.isEmpty() ? slots[i].getNama() : ", " + slots[i].getNama();
			}
		}
		if (jenis.isEmpty() && p.getJenisPenyediaAsset() != null) {
			jenis = p.getJenisPenyediaAsset().getNama();
		}
		return jenis;
	}

	/**
	 * Tentukan nilai isian otomatis dari penyedia berdasarkan kata kunci pada label parameter.
	 *
	 * <p>Ini adalah tabel pemetaan dari KATA KUNCI pada label parameter ke bidang data penyedia, diperiksa
	 * berurutan dengan {@code contains}. Urutannya bermakna karena kata kunci bisa saling tumpang tindih:
	 * pemeriksaan {@code "alamat"} berada paling awal, sedangkan {@code "kontak"} sengaja diletakkan paling
	 * akhir supaya label seperti "Alamat / Kontak Vendor I" tetap menghasilkan blok alamat lengkap dan
	 * bukan sekadar nama kontak.</p>
	 *
	 * <p>Beberapa kata kunci punya sinonim: PIC dikenali dari {@code "pic"}, {@code "penanggung jawab"},
	 * {@code "contact person"}, atau {@code "narahubung"}, dan bila kontak kosong dipakai nama pemilik
	 * sebagai cadangan. Telepon dikenali dari {@code "telp"}, {@code "telepon"}, atau {@code "hp"}.</p>
	 *
	 * <p><b>Label yang tidak dikenali mengembalikan {@code null}, dan itu disengaja.</b> Nilai {@code null}
	 * dibedakan dari string kosong: {@code null} berarti "jangan sentuh isian ini", sedangkan string kosong
	 * berarti "bidangnya memang kosong" dan akan MENGOSONGKAN isian. Pemanggilnya,
	 * {@link #isiOtomatisParameterTerkaitPenyedia(ParameterTambahan, org.zkoss.zk.ui.Component,
	 * java.util.List)}, melewati parameter yang hasilnya {@code null} tanpa mengubah apa pun.</p>
	 *
	 * <p><b>Kerapuhan yang perlu disadari:</b> pencocokan ini bersandar pada KATA-KATA label yang ditulis
	 * pengelola parameter, bukan pada metadata. Mengganti kata pada label — mis. "Telepon" menjadi "No.
	 * Kontak Telepon" — dapat memindahkan isian ke bidang lain atau mematikan pengisian otomatis sama
	 * sekali, tanpa perubahan kode dan tanpa pesan galat.</p>
	 *
	 * @param labelLower label parameter yang SUDAH dikecilkan hurufnya oleh pemanggil; pencocokan di sini
	 *                   tidak mengecilkan huruf lagi.
	 * @param p          penyedia sumber data; tidak boleh {@code null}.
	 * @return nilai yang cocok, atau {@code null} bila label tidak dikenali — yang berarti "jangan isi".
	 */
	private static String nilaiPenyediaUntukLabel(String labelLower, ais.database.model.asset.PenyediaAsset p) {
		if (labelLower.contains("alamat")) {
			return rangkaiAlamatPenyedia(p);
		}
		if (labelLower.contains("jenis") || labelLower.contains("barang") || labelLower.contains("jasa")
				|| labelLower.contains("pekerjaan")) {
			return rangkaiJenisPekerjaanPenyedia(p);
		}
		if (labelLower.contains("pic") || labelLower.contains("penanggung jawab")
				|| labelLower.contains("contact person") || labelLower.contains("narahubung")) {
			String pic = nzp(p.getKontak());
			return pic.isEmpty() ? nzp(p.getPemilik()) : pic;
		}
		if (labelLower.contains("email") || labelLower.contains("e-mail")) {
			return nzp(p.getEmail());
		}
		if (labelLower.contains("telp") || labelLower.contains("telepon") || labelLower.contains("hp")) {
			return nzp(p.getTelp());
		}
		if (labelLower.contains("fax")) {
			return nzp(p.getFax());
		}
		if (labelLower.contains("npwp")) {
			return nzp(p.getNpwp());
		}
		if (labelLower.contains("kontak")) {
			return nzp(p.getKontak());
		}
		return null; // label tidak dikenali -> jangan diisi
	}

	/**
	 * KORELASI ANTAR-PARAMETER: setelah pengguna memilih Penyedia (vendor) pada parameter bertipe
	 * {@link #PILIHAN_PENYEDIA}, isi otomatis parameter teks lain yang SE-KONTEKS. Konteks diambil dari
	 * label parameter vendor dengan membuang kata pengantar umum di depannya ("Nama Vendor I" →
	 * konteks "vendor i"); parameter lain dianggap se-konteks bila label-nya BERAKHIRAN konteks itu
	 * (endsWith, agar "Vendor I" tidak menular ke "Vendor II"). Nilai isian ditentukan dari kata kunci
	 * label (alamat / jenis barang-jasa / pic / telp / email / fax / npwp / kontak).
	 *
	 * <p><b>Cara konteks ditentukan.</b> Label parameter vendor dikecilkan hurufnya, lalu kata pengantar
	 * umum di depannya dibuang BERULANG-ULANG sampai tidak ada lagi yang cocok — daftar kata yang dibuang
	 * adalah {@code "nama"}, {@code "pilih"}, {@code "pilihan"}, dan {@code "data"}. Jadi "Nama Data Vendor
	 * I" pun tetap menyusut menjadi konteks {@code "vendor i"}. Bila setelah pemangkasan konteksnya kosong,
	 * method berhenti tanpa mengubah apa pun.</p>
	 *
	 * <p><b>Kenapa {@code endsWith} dan bukan {@code contains}.</b> Pencocokan memakai AKHIRAN justru untuk
	 * mencegah penularan antar vendor bernomor: konteks {@code "vendor i"} tidak boleh mengenai label
	 * "Alamat Vendor II". Perlu disadari perlindungan ini tidak sempurna — konteks {@code "vendor i"} akan
	 * tetap mengenai label yang berakhiran "vendor i" dalam bentuk lain, dan sebaliknya penomoran dengan
	 * angka Arab ("Vendor 1" vs "Vendor 11") tetap aman karena "vendor 1" bukan akhiran dari "vendor 11"
	 * hanya jika label berakhir tepat di situ.</p>
	 *
	 * <p><b>Hanya komponen masukan yang diisi.</b> Parameter sasaran diisi hanya bila komponennya turunan
	 * {@code InputElement}; komponen lain (mis. {@code Combobox} pemilih, bandbox matriks) dilewati. Setelah
	 * teks diisi, {@code onChange} DIPOSTING secara buatan agar rantai pendengar lain — termasuk evaluasi
	 * ulang skip-logic — ikut berjalan seolah pengguna sendiri yang mengetik.</p>
	 *
	 * <p><b>Komponen vendor itu sendiri selalu dilewati</b> lewat perbandingan rujukan
	 * ({@code cO == komponenVendor}), sehingga pemilih vendor tidak menimpa dirinya sendiri.</p>
	 *
	 * <p><b>Defensif berlapis.</b> Seluruh badan method dibungkus {@code try/catch} terhadap
	 * {@code Throwable}, DAN setiap baris diproses dalam {@code try/catch}-nya sendiri. Satu baris yang
	 * bermasalah karena itu tidak menghentikan pengisian baris lainnya, dan kegagalan apa pun tidak pernah
	 * memutus render form — keduanya hanya dicatat ke {@code ErrorAuditUtil}. Konsekuensinya, pengisian
	 * otomatis yang tidak berjalan TIDAK akan terlihat sebagai galat oleh pengguna; ia sekadar tidak
	 * terjadi.</p>
	 *
	 * <p>Sumber datanya adalah atribut {@code "penyediaAsset"} pada komponen vendor — atribut yang dipasang
	 * {@link #ambilComponentCustom(String, ParameterTambahan, org.zkoss.zk.ui.event.EventListener)} dan
	 * diperbarui banbox saat pengguna memilih. Bila atribut itu bukan {@code PenyediaAsset}, method berhenti
	 * diam-diam.</p>
	 *
	 * @param ptVendor       definisi parameter bertipe {@link #PILIHAN_PENYEDIA} yang baru dipilih; labelnya
	 *                       menjadi sumber konteks. {@code null} membuat method tanpa efek.
	 * @param komponenVendor komponen pemilih vendor; atribut {@code "penyediaAsset"}-nya dibaca sebagai
	 *                       sumber data. {@code null} membuat method tanpa efek.
	 * @param parameterRows  seluruh baris parameter dalam form yang akan dipindai. {@code null} membuat
	 *                       method tanpa efek.
	 * @see #nilaiPenyediaUntukLabel(String, ais.database.model.asset.PenyediaAsset)
	 */
	public static void isiOtomatisParameterTerkaitPenyedia(ParameterTambahan ptVendor, Component komponenVendor,
			List<Row> parameterRows) {
		try {
			if (ptVendor == null || komponenVendor == null || parameterRows == null) {
				return;
			}
			Object data = komponenVendor.getAttribute("penyediaAsset");
			if (!(data instanceof ais.database.model.asset.PenyediaAsset)) {
				return;
			}
			ais.database.model.asset.PenyediaAsset penyedia = (ais.database.model.asset.PenyediaAsset) data;

			String konteks = nzp(ptVendor.getLabelInputan()).toLowerCase();
			String[] prefixBuang = new String[] { "nama", "pilih", "pilihan", "data" };
			boolean berubah = true;
			while (berubah) {
				berubah = false;
				for (int i = 0; i < prefixBuang.length; i++) {
					if (konteks.startsWith(prefixBuang[i] + " ")) {
						konteks = konteks.substring(prefixBuang[i].length() + 1).trim();
						berubah = true;
					}
				}
			}
			if (konteks.isEmpty()) {
				return;
			}

			for (Row r : parameterRows) {
				try {
					if (r == null) {
						continue;
					}
					Object ptO = r.getAttribute("parameterTambahan");
					Object cO = r.getAttribute("component");
					if (!(ptO instanceof ParameterTambahan) || !(cO instanceof Component) || cO == komponenVendor) {
						continue;
					}
					ParameterTambahan pt = (ParameterTambahan) ptO;
					String label = nzp(pt.getLabelInputan()).toLowerCase();
					if (label.isEmpty() || !label.endsWith(konteks)) {
						continue;
					}
					String nilai = nilaiPenyediaUntukLabel(label, penyedia);
					if (nilai == null) {
						continue;
					}
					Component c = (Component) cO;
					if (c instanceof org.zkoss.zul.impl.InputElement) {
						((org.zkoss.zul.impl.InputElement) c).setText(nilai);
						org.zkoss.zk.ui.event.Events.postEvent("onChange", c, null);
					}
				} catch (Throwable tRow) {
					ais.common.ErrorAuditUtil.record(tRow,
							"auto-audit korelasi-vendor per-baris ParameterTambahanAstract");
				}
			}
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "auto-audit isiOtomatisParameterTerkaitPenyedia");
		}
	}

	/**
	 * Membangun SATU baris form lengkap untuk sebuah parameter tambahan: komponen isian, baris keterangan
	 * opsional, baris unggah lampiran opsional, penjagaan tampil khusus admin, dan pendaftaran skip-logic.
	 * Ini adalah implementasi PENUH yang menjadi muara seluruh kelebihan-beban {@code initComponent}.
	 *
	 * <h3>Kunci namespace {@code jenis} — REFERENSI DEFINITIF, akar {@code task_484d4bd0}</h3>
	 *
	 * <p>Method ini adalah tempat kunci namespace lampiran benar-benar DIPAKAI, dan karena itu tempat
	 * paling tepat untuk menjelaskan mekanismenya sekali untuk selamanya. Sesi/berkas lain yang menyinggung
	 * {@code task_484d4bd0} sebaiknya merujuk balik ke sini.</p>
	 *
	 * <p><b>Pertama, yang sering disalahpahami: method ini TIDAK MEMBANGUN kunci itu.</b> {@code jenis}
	 * datang sebagai ARGUMEN. Pembangunnya adalah para pemanggil, dan bentuk bakunya adalah rangkaian
	 * mentah dua id:</p>
	 * <pre>
	 *   String jenis = kelompokX.getId() + "-&gt;" + parameterTambahan.getId();
	 * </pre>
	 * <p>dengan {@code kelompokX} berupa baris "kelompok" perantara milik modul pemakai — mis.
	 * {@code KelompokParameterTambahanAlumni}, {@code KelompokParameterTambahanCalonPegawai},
	 * {@code KelompokParameterTambahanAlurSop} — dan {@code parameterTambahan} adalah definisi parameter
	 * pada baris ini. Bentuk yang sama persis juga dipakai sebagai kunci NILAI di dalam kolom
	 * {@code parameterTambahanInds} milik entity pemakai, sehingga satu rangkaian teks memikul dua peran
	 * sekaligus: kunci nilai sekaligus kunci lampiran.</p>
	 *
	 * <p><b>Kedua, bagaimana kunci itu dipakai.</b> Di dalam method ini {@code jenis} menyentuh penyimpanan
	 * lampiran di TIGA titik, dan ketiganya selalu berpasangan dengan {@code ref}:</p>
	 * <ol>
	 * <li>{@code LampiranLain.ambil(ref, jenis)} — memeriksa apakah sudah ada berkas terunggah, untuk
	 * memutuskan menampilkan tombol unduh/unggah atau sekadar tulisan "Tidak/belum diupload".</li>
	 * <li>{@code LampiranLain.createDownloadUploadFileLain(hbox, ref, jenis, ...)} — membangun widget
	 * unggah yang kelak MENULIS berkas dengan identitas {@code (ref, jenis)} itu.</li>
	 * <li>{@code lampiranLains.put(jenis, lainAlumni)} — menitipkan hasil unggah ke peta keluaran milik
	 * pemanggil, dengan {@code jenis} sebagai kuncinya.</li>
	 * </ol>
	 *
	 * <p><b>Ketiga, inilah cacatnya.</b> Identitas sebuah lampiran seluruhnya adalah pasangan
	 * {@code (ref, jenis)}. {@code ref} berisi {@link ParameterTambahan#getId()} milik ENTITY PEMILIK —
	 * mis. id sebuah {@code CatatanSiswa}, atau id sebuah {@code PerbaikanAsset}. Persoalannya, setiap
	 * entity punya urutan id-nya SENDIRI: {@code CatatanSiswa} nomor 5 dan {@code PerbaikanAsset} nomor 5
	 * sama-sama ada. Sementara itu {@code jenis} hanya memuat id kelompok dan id parameter, dan
	 * <b>TIDAK MEMUAT SATU PUN DISKRIMINATOR KELAS PEMILIK</b>. Akibatnya dua entity dari kelas yang
	 * BERBEDA dapat menghasilkan pasangan {@code (ref, jenis)} yang identik, lalu saling melihat — bahkan
	 * saling menimpa — lampiran satu sama lain. Inilah {@code task_484d4bd0}, dan ia bukan cacat teoretis:
	 * mekanisme parameter tambahan ini terkonfirmasi dipakai lebih dari dua puluh entity independen lintas
	 * modul (akademik, kepegawaian, sekolah, aset, SOP, pengaduan whistleblower), yang seluruhnya berbagi
	 * satu ruang nama lampiran yang sama.</p>
	 *
	 * <p>Perlu ditegaskan bahwa kelas ini SUDAH memuat contoh pola yang benar, hanya beberapa baris di
	 * bawah. Untuk lampiran tingkat-master — berkas yang melekat pada DEFINISI parameter, bukan pada
	 * jawaban seseorang — method ini memakai:</p>
	 * <pre>
	 *   LampiranLain.ambil(parameterTambahan.getId(), ParameterTambahanAstract.class.getName());
	 * </pre>
	 * <p>di sini {@code jenis} justru berupa NAMA KELAS berkualifikasi penuh, sehingga aman dari tabrakan
	 * sejak awal. Perbedaan perlakuan antara dua pemakaian dalam satu method yang sama inilah yang membuat
	 * cacat tersebut lolos sekian lama.</p>
	 *
	 * <p><b>Keempat, penambalannya.</b> Pola aman yang kini dipakai adalah menormalkan {@code jenis}
	 * SEBELUM diserahkan ke method ini, lewat {@code LampiranLain.resolveJenisParameterTambahan(Class<?>
	 * ownerClass, Long ref, String jenisMentah)}. Helper itu menyisipkan diskriminator kelas pemilik di
	 * depan kunci mentah:</p>
	 * <pre>
	 *   ais.database.model.sekolah.CatatanSiswa#12-&gt;34
	 * </pre>
	 * <p>Ia sengaja dirancang TANPA migrasi basis data. Urutannya: coba bentuk ber-namespace lebih dulu;
	 * bila tidak ada barisnya, coba bentuk mentah dan pakai itu bila ketemu (baris warisan tetap terbaca);
	 * bila keduanya nihil, kembalikan bentuk ber-namespace sehingga unggahan BARU aman sejak awal.
	 * Pasangannya, {@code LampiranLain.kunciNilaiParameterTambahan(String)}, memangkas kembali segalanya
	 * sampai tanda {@code '#'} TERAKHIR — diperlukan karena kunci NILAI di dalam
	 * {@code parameterTambahanInds} sengaja tetap mentah, sehingga kode yang mencocokkan keduanya harus
	 * menormalkannya lebih dulu atau pencocokannya gagal dalam senyap.</p>
	 *
	 * <p>Perlu dicatat satu celah yang tersisa pada helper itu sendiri: bila {@code ref} bernilai
	 * {@code null} ia mengembalikan {@code jenisMentah} apa adanya. Di dalam method ini {@code ref} yang
	 * {@code null} memang diganti {@code Common.refSementara()}, tetapi penggantian itu terjadi SETELAH
	 * pemanggil memanggil helper — jadi unggahan pada entity yang BELUM tersimpan tetap memakai kunci
	 * mentah.</p>
	 *
	 * <p><b>Kelima, peta pemanggil per 6 September 2026.</b> Penambalan berjalan lewat r83920-83971 dan
	 * seterusnya, tetapi BELUM tuntas:</p>
	 * <ul>
	 * <li><b>SUDAH AMAN</b> — 22 pemanggil {@code initComponent} menormalkan {@code jenis} lewat
	 * {@code resolveJenisParameterTambahan}, meliputi berkas berpola
	 * {@code ParameterTambahan*Listener} (catatan pegawai/mahasiswa/administrasi/siswa/kelas siswa/guru,
	 * mahasiswa, alumni, pengaduan, pengajuan, pengajuan pegawai, perbaikan aset, pertemuan, cuti dan izin,
	 * gaji pegawai, pengajuan transaksi pegawai, kegiatan LKP, kegiatan siswa, PMB, PSB, disposisi alur
	 * SOP) serta {@code TampilanPengumumanAkademisAction} pada jalur {@code CalonSiswa}. Sisi model dan
	 * laporannya juga sudah ikut, sehingga totalnya sekitar 84 berkas.</li>
	 * <li><b>BELUM AMAN — jalur {@code initComponent}</b>: {@code TampilanPengumumanAkademisAction} pada
	 * jalur {@code BiodataCalonMahasiswa} masih merangkai kunci secara mentah; ini kemunculan yang
	 * terlewat, karena berkas yang sama sudah ditambal pada jalur lainnya. Berikutnya
	 * {@code IsiAngketParameterUmumListener} yang bersifat CAMPURAN: hanya cabang UMUM yang dinormalkan,
	 * sedangkan cabang DOSEN dan GURU masih mentah.</li>
	 * <li><b>BELUM AMAN — jalur lain menuju {@code LampiranLain}</b> (tidak melewati method ini):
	 * {@code BroadcastHelper} pada dua tempat (catatan administrasi dan perbaikan aset),
	 * {@code IsiAngketParameterUmum} pada sisi model — yang kini justru TIDAK SEJALAN dengan sisi
	 * listener-nya sehingga URL hasil unggah gagal tersimpan, {@code SopService} pada jalur REST yang
	 * bahkan menerbitkan kunci mentah itu ke klien seluler padahal jalur ZK untuk pemilik yang sama sudah
	 * ber-namespace, serta rutin salin/cetak {@code CetakRegistrasiAction}, {@code CalonSiswaAction}, dan
	 * {@code MahasiswaAction} yang mengambil {@code jenis} langsung dari kolom nilai (yang memang tetap
	 * mentah) sehingga melewatkan baris yang sudah ber-namespace.</li>
	 * </ul>
	 *
	 * <p>Dari 24 entity pemilik yang punya kolom {@code parameterTambahanInds}, hanya
	 * {@code IsiAngketParameterUmum} yang jalur populate-nya belum memakai helper sama sekali
	 * ({@code KelompokCalonMahasiswa} tidak memakai lampiran, jadi tidak terdampak).</p>
	 *
	 * <h3>Yang dikerjakan method ini selain lampiran</h3>
	 *
	 * <p><b>Nilai awal.</b> Bila {@code val} {@code null} atau kosong, {@link ParameterTambahan#getNilaiDefault()}
	 * dipakai sebagai gantinya — jadi nilai default adalah default TAMPILAN yang baru tersimpan bila
	 * pengguna menyimpan formnya.</p>
	 *
	 * <p><b>Skip-logic.</b> Komponen didaftarkan pada atribut {@code stParamId}/{@code stComponent} milik
	 * baris, dan bila parameter punya {@link ParameterTambahan#getSyaratTampil()} juga pada
	 * {@code stSyaratParam}. Pendengar {@code onChange}/{@code onSelect} dipasang agar
	 * {@link #reevaluasiSkipLogic(java.util.List)} berjalan setiap kali nilai acuan berubah, dan sekali
	 * lagi di akhir method untuk keadaan awal. Baris turunan (keterangan/lampiran) ditandai
	 * {@code stChildOf} agar ikut tersembunyi bersama induknya. Seluruh bagian ini dibungkus
	 * {@code try/catch} yang mencatat ke {@code ErrorAuditUtil} lalu melanjutkan — skip-logic yang gagal
	 * tidak boleh memutus render form, dengan konsekuensi barisnya menjadi SELALU TAMPIL.</p>
	 *
	 * <p><b>Korelasi vendor.</b> Untuk tipe {@link #PILIHAN_PENYEDIA}, pendengar milik banbox dibungkus
	 * agar {@link #isiOtomatisParameterTerkaitPenyedia(ParameterTambahan, org.zkoss.zk.ui.Component,
	 * java.util.List)} berjalan lebih dulu, lalu pendengar lama tetap dipanggil supaya rantai yang sudah
	 * ada tidak putus. Hanya aktif bila {@code readonly} bernilai {@code false}.</p>
	 *
	 * <p><b>Penjagaan admin.</b> Bila {@link ParameterTambahan#getHanyaTampilDiAdmin()} bernilai
	 * {@code true}, baris disembunyikan dan dibekukan ({@code Common.freeze}) bagi yang bukan admin
	 * berwenang. Ini kendali TAMPILAN: barisnya tetap ada dalam struktur form dan nilainya tetap dibaca
	 * kembali oleh {@link #ambilVal(Row, ParameterTambahan)}, jadi jangan menjadikannya satu-satunya
	 * penjaga untuk data rahasia.</p>
	 *
	 * <p><b>Pendaftaran baris TANPA SYARAT.</b> {@code parameterRows.add(row)} dijalankan di luar blok
	 * {@code if (component != null)}, sehingga baris tanpa komponen isian sama sekali — mis. tipe
	 * {@link #TIDAK_ADA} — tetap terdaftar. {@link #ambilVal(Row, ParameterTambahan)} sengaja bertoleransi
	 * terhadap keadaan ini dan memperlakukannya sebagai nilai kosong yang WAJAR, bukan sebagai galat.</p>
	 *
	 * <p><b>Prasyarat.</b> {@code parameterTambahan} tidak boleh {@code null}: ia di-dereferensi tanpa
	 * penjagaan pada blok penjagaan admin yang berada di luar cabang {@code component != null}.</p>
	 *
	 * @param row               baris tujuan komponen isian; komponennya dititipkan sebagai atribut
	 *                          bernama {@code componenName}.
	 * @param rows              wadah baris; tempat baris keterangan, lampiran, dan lampiran master
	 *                          ditambahkan.
	 * @param jenis             KUNCI NAMESPACE LAMPIRAN. Harap dinormalkan pemanggil lewat
	 *                          {@code LampiranLain.resolveJenisParameterTambahan(...)}; bentuk mentah
	 *                          {@code "<kelId>-><ptId>"} rentan tabrakan lintas kelas pemilik — lihat
	 *                          uraian {@code task_484d4bd0} di atas.
	 * @param parameterRows     daftar seluruh baris parameter dalam form, dipakai skip-logic dan korelasi
	 *                          vendor. Boleh {@code null}, dan bila {@code null} kedua fitur itu mati.
	 * @param lampiranLains     peta keluaran tempat hasil unggah dititipkan dengan kunci {@code jenis}.
	 *                          Bila {@code null}, lampiran dirender sebagai tampilan mati saja.
	 * @param ref               id ENTITY PEMILIK, pasangan dari {@code jenis}. Bila {@code null} diganti
	 *                          {@code Common.refSementara()} untuk menampung unggahan pra-simpan.
	 * @param val               nilai tersimpan; {@code null}/kosong digantikan
	 *                          {@link ParameterTambahan#getNilaiDefault()}.
	 * @param ket               keterangan tersimpan, hanya dipakai bila
	 *                          {@link ParameterTambahan#getTampilkanIsianKeterangan()} bernilai {@code true}.
	 * @param parameterTambahan definisi parameter; TIDAK BOLEH {@code null}.
	 * @param eventListener     pendengar perubahan tambahan milik pemanggil; boleh {@code null}.
	 * @param readonly          {@code true} untuk merender baris sebagai teks mati dan mematikan korelasi
	 *                          vendor.
	 * @param componenName      nama atribut tempat komponen dititipkan pada {@code row}; harus sama dengan
	 *                          yang dipakai {@link #ambilVal(Row, ParameterTambahan, String)} saat membaca
	 *                          nilai kembali.
	 * @return {@code true} bila baris ini terlihat oleh pengguna saat ini; {@code false} bila seluruhnya
	 *         tersembunyi karena penjagaan admin. Dipakai pemanggil untuk memutuskan apakah judul bagian
	 *         yang menaunginya masih perlu ditampilkan.
	 * @see ParameterTambahan#masukkanSemuaParameterKeMap(String, java.util.Map)
	 * @see #ambilVal(Row, ParameterTambahan, String)
	 * @see #reevaluasiSkipLogic(java.util.List)
	 */
	public static boolean initComponent(Row row, Rows rows, final String jenis, List<Row> parameterRows,
			final Map<String, LampiranLain> lampiranLains, Long ref, String val, String ket,
			ParameterTambahan parameterTambahan, EventListener eventListener, boolean readonly, String componenName) {

		if (parameterTambahan != null && (val == null || val.trim().isEmpty())) {
			val = parameterTambahan.getNilaiDefault();
		}

		Component component = ParameterTambahanAstract.ambilComponent(val, parameterTambahan, eventListener, readonly);

		boolean tampil = false;
		if (component != null) {
			row.appendChild(component);
			row.setValign("top");
			row.setAttribute(componenName, component);
			// SKIP-LOGIC (syaratTampil): daftarkan nilai komponen & pasang pemicu live (defensif, no-op bila gagal).
			try {
				row.setAttribute("stParamId", parameterTambahan.getId());
				row.setAttribute("stComponent", component);
				String stJsonSyarat = parameterTambahan.getSyaratTampil();
				if (stJsonSyarat != null && !stJsonSyarat.trim().isEmpty()) {
					row.setAttribute("stSyaratParam", parameterTambahan);
				}
				final java.util.List<Row> prSkip = parameterRows;
				if (prSkip != null) {
					EventListener reEvalSkip = new EventListener() {
						@Override
						public void onEvent(Event evSkip) throws Exception {
							reevaluasiSkipLogic(prSkip);
						}
					};
					component.addEventListener("onChange", reEvalSkip);
					component.addEventListener("onSelect", reEvalSkip);
				}
			} catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:211");
			}
			// KORELASI ANTAR-PARAMETER (vendor/penyedia): saat sebuah parameter bertipe
			// PILIHAN_PENYEDIA dipilih (mis. "Nama Vendor I"), parameter TEKS lain yang
			// se-konteks (label berakhiran sama, mis. "Alamat / Kontak Vendor I",
			// "Jenis Barang/Jasa Vendor I", "PIC Vendor I", "Telp Vendor I", "Email Vendor I")
			// otomatis terisi dari data PenyediaAsset terpilih.
			try {
				if (parameterTambahan != null && parameterRows != null && !readonly
						&& ParameterTambahanAstract.PILIHAN_PENYEDIA.equals(parameterTambahan.getTipeDataInputan())
						&& component instanceof ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox) {
					final ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox banboxPenyedia =
							(ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox) component;
					final ParameterTambahan ptVendor = parameterTambahan;
					final java.util.List<Row> prVendor = parameterRows;
					final EventListener listenerLama = banboxPenyedia.getEventListener();
					banboxPenyedia.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event evVendor) throws Exception {
							isiOtomatisParameterTerkaitPenyedia(ptVendor, banboxPenyedia, prVendor);
							if (listenerLama != null) {
								listenerLama.onEvent(evVendor);
							}
						}
					});
				}
			} catch (Throwable tVendor) {
				ais.common.ErrorAuditUtil.record(tVendor, "auto-audit korelasi-vendor ParameterTambahanAstract.initComponent");
			}
			if (parameterTambahan.getTampilkanIsianKeterangan()) {
				MyFormRow rowKeterangan = new MyFormRow();
				if (parameterRows != null)
					parameterRows.add(rowKeterangan);
				rowKeterangan.setStyle("border:0px;background: transparent;");
				rowKeterangan.setParent(rows);
				try { rowKeterangan.setAttribute("stChildOf", parameterTambahan.getId()); } catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:219");}
				rowKeterangan.appendChild(new Label());
				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				rowKeterangan.appendChild(vbox);
				vbox.appendChild(new Label(parameterTambahan.getLabelInputanKeterangan()));

				if (lampiranLains == null) {
					vbox.appendChild(new Label(ket));
				} else {
					Textbox keterangan = new Textbox(ket);
					keterangan.setWidth("90%");
					keterangan.setRows(2);
					vbox.appendChild(keterangan);
					rowKeterangan.setAttribute("keterangan", keterangan);
					row.setValign("top");
					row.setAttribute("keterangan", keterangan);
				}
			}
			if (parameterTambahan.getHarusMenyertakanLampiran()) {
				MyFormRow rowUpload = new MyFormRow();
				if (parameterRows != null)
					parameterRows.add(rowUpload);
				rowUpload.setStyle("border:0px;background: transparent;");
				rowUpload.setParent(rows);
				try { rowUpload.setAttribute("stChildOf", parameterTambahan.getId()); } catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:244");}
				rowUpload.appendChild(new Label());

				Hbox hbox = new Hbox();
				hbox.setWidth("100%");
				hbox.setStyle("border:0px;background: transparent;");

				if (ref == null) {
					ref = Common.refSementara();
				}
				FileFotoLain fileFotoLain = LampiranLain.ambil(ref, jenis);
				if (lampiranLains == null && (fileFotoLain == null || fileFotoLain.getId() == null)) {
					hbox.appendChild(new MyLabelAgakKecilBoldMerah("Tidak/belum diupload"));
				} else {
					LampiranLain.createDownloadUploadFileLain(hbox, ref, jenis,
							parameterTambahan.getLabelInputan()
									+ (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
							false, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lainAlumni = (LampiranLain) arg0.getData();
									lampiranLains.put(jenis, lainAlumni);
								}
							}, lampiranLains, false, false, false, !readonly, null);
				}
				hbox.setParent(rowUpload);

				if (parameterTambahan.getHanyaTampilDiAdmin()) {
					boolean tidaktampil = !Common.getApakahAdmin(parameterTambahan.getKodeAdminYgBoleh());
					rowUpload.setVisible(!tidaktampil);
					tampil |= !tidaktampil;
				} else {
					tampil |= true;
				}
			}
		}

		if (parameterTambahan.getHanyaTampilDiAdmin()) {
			boolean tidaktampil = !Common.getApakahAdmin(parameterTambahan.getKodeAdminYgBoleh());
			row.setVisible(!tidaktampil);
			Common.freeze(row, tidaktampil);
			tampil |= !tidaktampil;
		} else {
			tampil |= true;
		}

		if (parameterRows != null)
			parameterRows.add(row);

		LampiranLain lampiranLain = LampiranLain.ambil(parameterTambahan.getId(),
				ParameterTambahanAstract.class.getName());
		if (lampiranLain != null) {
			MyFormRow rowLampiran = new MyFormRow();
			rowLampiran.setParent(rows);
			rowLampiran.appendChild(new Label());

			Vbox myvbox = new Vbox();
			myvbox.setParent(rowLampiran);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, parameterTambahan.getId(),
					ParameterTambahanAstract.class.getName(), "Lampiran", false, null, null, false, false, false,
					false);

			if (parameterTambahan.getHanyaTampilDiAdmin()) {
				boolean tidaktampil = !Common.getApakahAdmin(parameterTambahan.getKodeAdminYgBoleh());
				rowLampiran.setVisible(!tidaktampil);
				tampil |= !tidaktampil;
			} else {
				tampil |= true;
			}
		}

		try { if (parameterRows != null) reevaluasiSkipLogic(parameterRows); } catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:318");}
		return tampil;
	}

	/**
	 * Membaca nilai KINI dari komponen input ZK (untuk evaluasi skip-logic secara live). Combobox dibaca dari
	 * value item terpilih (mis. "Bekerja:1" utk pilihan custom, "true"/"false" utk ya/tidak); komponen lain
	 * via getValue() refleksi. Aman: kembalikan "" bila gagal.
	 */
	public static String ambilNilaiComponent(Component c) {
		if (c == null) {
			return "";
		}
		try {
			if (c instanceof Combobox) {
				Combobox cb = (Combobox) c;
				Comboitem it = cb.getSelectedItem();
				if (it != null) {
					Object v = it.getValue();
					if (v != null) {
						return String.valueOf(v);
					}
					if (it.getLabel() != null) {
						return it.getLabel();
					}
				}
				return cb.getValue() == null ? "" : cb.getValue();
			}
			java.lang.reflect.Method m = c.getClass().getMethod("getValue");
			Object v = m.invoke(c);
			return v == null ? "" : String.valueOf(v);
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * Evaluasi ulang SKIP-LOGIC (syaratTampil) untuk SELURUH baris parameter dalam satu form ZK. Dipanggil saat
	 * load (dari initComponent) dan setiap kali komponen acuan berubah (listener onChange/onSelect). Hanya baris
	 * yang PUNYA syaratTampil yang divisibilitasnya dikelola (baris lain tak disentuh, termasuk yang disembunyikan
	 * karena hanyaTampilDiAdmin). Baris turunan (keterangan/lampiran) ikut sembunyi bila induknya tersembunyi.
	 * Defensif total: bungkus try/catch agar tak pernah memutus render form.
	 */
	public static void reevaluasiSkipLogic(java.util.List<Row> parameterRows) {
		try {
			if (parameterRows == null || parameterRows.isEmpty()) {
				return;
			}
			java.util.Map<Long, String> peta = new java.util.HashMap<Long, String>();
			for (int i = 0; i < parameterRows.size(); i++) {
				Row r = parameterRows.get(i);
				if (r == null) {
					continue;
				}
				Object pid = r.getAttribute("stParamId");
				Object comp = r.getAttribute("stComponent");
				if (pid instanceof Long && comp instanceof Component) {
					peta.put((Long) pid, ambilNilaiComponent((Component) comp));
				}
			}
			java.util.Set<Long> hidden = new java.util.HashSet<Long>();
			for (int i = 0; i < parameterRows.size(); i++) {
				Row r = parameterRows.get(i);
				if (r == null) {
					continue;
				}
				Object p = r.getAttribute("stSyaratParam");
				if (p instanceof ParameterTambahan) {
					ParameterTambahan pt = (ParameterTambahan) p;
					boolean lolos = ais.common.ParameterTambahanHtmlHelper.lolosSyaratTampil(pt, peta);
					boolean adminHide = false;
					try {
						adminHide = pt.getHanyaTampilDiAdmin() && !Common.getApakahAdmin(pt.getKodeAdminYgBoleh());
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:391");
					}
					boolean visible = lolos && !adminHide;
					r.setVisible(visible);
					if (!visible && pt.getId() != null) {
						hidden.add(pt.getId());
					}
				}
			}
			for (int i = 0; i < parameterRows.size(); i++) {
				Row r = parameterRows.get(i);
				if (r == null) {
					continue;
				}
				Object childOf = r.getAttribute("stChildOf");
				if (childOf instanceof Long) {
					if (hidden.contains((Long) childOf)) {
						r.setVisible(false);
						r.setAttribute("stHidByLogic", Boolean.TRUE);
					} else if (Boolean.TRUE.equals(r.getAttribute("stHidByLogic"))) {
						r.setVisible(true);
						r.removeAttribute("stHidByLogic");
					}
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:416");
		}
	}

	public static Component ambilComponent(String val, ParameterTambahan parameterTambahan,
			EventListener eventListener) {
		return ambilComponent(val, parameterTambahan, eventListener, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Component ambilComponent(final String val, final ParameterTambahan parameterTambahan,
			final EventListener eventListener, boolean readonly) {
		Component component;

		if (parameterTambahan.getNilaiTidakBolehDiubah() || readonly) {
			if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)
			    || parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
				try {
					component = new Label(
							readonly && (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim()))
									? "{Tidak/belum diisi}"
									: Common.numberFormat.get().format(Double.parseDouble(val.trim())));
				} catch (Exception e) {
					component = new Label(readonly && (val == null || val.trim().isEmpty()) ? "{Tidak/belum diisi}"
							: val.equalsIgnoreCase("true") ? "Ya" : val.equalsIgnoreCase("false") ? "Tidak" : val);
				}
			} else {
				component = new Label(readonly && (val == null || val.trim().isEmpty()) ? "{Tidak/belum diisi}"
						: val.equalsIgnoreCase("true") ? "Ya" : val.equalsIgnoreCase("false") ? "Tidak" : val);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT)) {
			component = new Textbox(val);
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
			((Textbox) component).setRows(parameterTambahan.getJumlahBaris());
			((Textbox) component).setMaxlength(parameterTambahan.getJumlahText());

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL)) {
			Date nilai = null;
			try {
				nilai = val == null || val.trim().isEmpty() ? null : Common.dateFormat1.get().parse(val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:460"); }
			component = new MyDatebox(nilai);
			((MyDatebox) component).focus();
			((MyDatebox) component).setWidth("90%");
			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL_DAN_WAKTU)) {
			Date nilai = parseTanggalDanWaktu(val);
			component = new MyDatebox(nilai);
			((MyDatebox) component).setFormat(Common.dateFormat.get().toPattern());
			((MyDatebox) component).focus();
			((MyDatebox) component).setWidth("90%");
			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.WAKTU)) {
			Date nilai = null;
			try {
				nilai = val == null || val.trim().isEmpty() ? null : Common.timeFormat.get().parse(val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:485"); }
			component = new MyTimebox(nilai);
			((MyTimebox) component).setFormat(Common.timeFormat.get().toPattern());
			((MyTimebox) component).focus();
			((MyTimebox) component).setWidth("90%");
			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)) {
			Double nilai = null;
			try {
				// FIX NumberFormatException: nilaiStr bisa berisi string literal "null"
				// (bukan Java null) hasil serialisasi objek null di titik sebelumnya
				// (mis. String.valueOf(objekNull)). Anggap kosong, jangan parseDouble.
				nilai = (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) ? null
						: Double.parseDouble(val.trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:498"); }

			component = new MyDoublebox(nilai);
			((MyDoublebox) component).setWidth("90%");

			final Double nilailama = nilai;
			((MyDoublebox) component).addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Double valData = ((MyDoublebox) arg0.getTarget()).getValue();
					if (valData != null && parameterTambahan.getNilaiMin() > valData) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Nilai yang dimasukkan tidak boleh lebih kecil dari {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang dimasukkan; (2) masukkan nilai yang tidak kurang dari batas minimum; (3) simpan kembali data.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								Common.numberFormat.get().format(parameterTambahan.getNilaiMin()));
						((MyDoublebox) arg0.getTarget()).setValue(nilailama == null ? parameterTambahan.getNilaiMin() : nilailama);
					} else if (valData != null && parameterTambahan.getNilaiMax() < valData) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Nilai yang dimasukkan tidak boleh lebih besar dari {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang dimasukkan; (2) masukkan nilai yang tidak melebihi batas maksimum; (3) simpan kembali data.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								Common.numberFormat.get().format(parameterTambahan.getNilaiMax()));
						((MyDoublebox) arg0.getTarget()).setValue(nilailama == null ? parameterTambahan.getNilaiMax() : nilailama);
					}
				}
			});

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
			component = new MyTextboxAngka(val);
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_YA_TIDAK)) {
			Boolean nilai = null;
			try {
				nilai = val == null || val.trim().isEmpty() ? null : Boolean.parseBoolean(val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:541"); }

			component = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig("Ya");
			comboitem.setValue(true);
			component.appendChild(comboitem);
			
			comboitem = new MyComboitemConfig("Tidak");
			comboitem.setValue(false);
			component.appendChild(comboitem);
			
			((Combobox) component).setReadonly(true);
			((Combobox) component).setWidth("90%");
			Common.selectComboItem(((Combobox) component), nilai);

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_CUSTOM)) {
			component = new Combobox();
			String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
			Arrays.sort(ss);
			for (String s : ss) {
				String[] ssss = StringUtils.split(s, ":");
				MyComboitemConfig comboitem = new MyComboitemConfig(ssss[0]);
				comboitem.setValue(s);
				component.appendChild(comboitem);
			}
			((Combobox) component).setReadonly(true);
			((Combobox) component).setWidth("90%");
			Common.selectComboItem(((Combobox) component), val);

			try {
				if (((Combobox) component).getSelectedItem() == null
						|| ((Combobox) component).getSelectedItem().getValue() == null) {
					String valBaru = val;
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						if (ssss.length > 1 && ssss[1].equalsIgnoreCase(val)) {
							valBaru = s;
						}
					}
					Common.selectComboItem(((Combobox) component), valBaru);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:586"); }

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_OBJECT)) {
			final Bandbox bandbox = new Bandbox();
			try {
				final Class clazz = Class.forName(parameterTambahan.getNilaiDataInputan());
				if (val != null && !val.trim().isEmpty()) {
					GeneralValueObject o = ConstantValues.ambil(clazz.getName(), Long.parseLong(val));
					if (o != null) {
						bandbox.setValue(o.getNama());
						bandbox.setAttribute("data", o);
					}
				}

				ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
				boolean adaNama = false;
				boolean adaKode = false;
				for (String p : classMetadata.getPropertyNames()) {
					if (p.equalsIgnoreCase("nama")) {
						adaNama = true;
					} else if (p.equalsIgnoreCase("kode")) {
						adaKode = true;
					}
				}
				final boolean dnama = adaNama;
				final boolean dkode = adaKode;

				Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
				bandpopup.setParent(bandbox);
				bandpopup.setWidth("400px");
				bandpopup.setHeight("400px");

				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setWidth("100%");
				radiogroup.setHeight("100%");
				radiogroup.setParent(bandpopup);

				Panel panel = new ais.ui.util.MyPanelConfig();
				panel.setParent(radiogroup);
				panel.setWidth("100%");
				panel.setHeight("100%");
				panel.setTitle(parameterTambahan.getLabelInputan());
				panel.setBorder("none");
				panel.setStyle("border:0px;");

				Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(panelchildren);

				North north = new North();
				north.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(north, true);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(north);

				final Textbox kode = new Textbox();
				final Textbox nama = new Textbox();
				final Rows rows = new Rows();

				EventListener eventListenerCari = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(rows);
						Session sessionLocal = null;
						
						try {
							// PERBAIKAN: Menggunakan session terisolasi & memastikan ditutup di finally
							sessionLocal = HibernateUtil.getSessionFactory().openSession();
							
							Criteria c = sessionLocal.createCriteria(clazz)
									.add(parameterTambahan.getKondisiDataInputan().isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.sqlRestriction(parameterTambahan.getKondisiDataInputan()))
									.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))
									.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
									.setMaxResults(1000);

							if (dnama && dkode) {
								c.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
							} else if (dkode) {
								c.addOrder(Order.asc("kode"));
							} else if (dnama) {
								c.addOrder(Order.asc("nama"));
							} else {
								c.addOrder(Order.asc("id"));
							}

							List<GeneralValueObject> generalValueObjects = ConstantValues.simpleList(c, clazz);

							for (final GeneralValueObject generalValueObject : generalValueObjects) {
								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setParent(rows);

								String d = generalValueObject.toString();
								if (dnama) {
									d = generalValueObject.getNama();
								}

								Radio radio = new Radio(d);
								radio.setSelected(generalValueObject.getId().toString().equalsIgnoreCase(val));
								radio.setParent(row);
								radio.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										bandbox.setValue(generalValueObject.getNama());
										bandbox.setOpen(false);
										bandbox.setAttribute("data", generalValueObject);

										if (eventListener != null) {
											eventListener.onEvent(new Event("", bandbox, generalValueObject));
										}
									}
								});
							}
						} catch (Exception ex) {
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:711");
						} finally {
							if (sessionLocal != null && sessionLocal.isOpen()) {
								try { sessionLocal.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:714");}
							}
						}
					}
				};

				if (adaKode) {
					toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode")));
					toolbar.appendChild(kode);
					kode.setCols(4);
				}
				if (adaNama) {
					toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
					toolbar.appendChild(nama);
					nama.setCols(4);
				}

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setMold("paging");
				grid.setPageSize(10);
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);

				org.zkoss.zul.Column column = new org.zkoss.zul.Column();
				column.setParent(columns);
				column.setLabel("Data " + clazz.getSimpleName());

				rows.setParent(grid);

				bandbox.addEventListener("onOpen", eventListenerCari);
				kode.addEventListener("onOK", eventListenerCari);
				nama.addEventListener("onOK", eventListenerCari);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
				button.addEventListener("onClick", eventListenerCari);
				button.setParent(toolbar);
				toolbar.appendChild(Common.createCleanButton(bandbox, new GetEventListener() {
					@Override
					public void setEventListener(EventListener eventListener) {}
					@Override
					public EventListener getEventListener() {
						bandbox.setAttribute("data", null);
						return eventListener;
					}
				}));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						bandbox.setOpen(false);
					}
				});
				cancel.setParent(toolbar);

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:784");
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX)) {
			final Bandbox bandbox = new Bandbox();
			bandbox.setValue(val);
			TreeSet<String> treeSet = new TreeSet<String>();
			String[] rowsData = StringUtils.split(parameterTambahan.getNilaiDataInputan(), "\n");
			Arrays.sort(rowsData);
			for (String rowData : rowsData) {
				String[] colAtauRow = rowData.split("->");
				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");
					String col = ssss[0].trim();
					treeSet.add(col);
				}
			}

			Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
			bandpopup.setParent(bandbox);
			bandpopup.setWidth("700px");
			bandpopup.setHeight("400px");

			Radiogroup radiogroup = new Radiogroup();
			radiogroup.setWidth("100%");
			radiogroup.setHeight("100%");
			radiogroup.setParent(bandpopup);

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(radiogroup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle(parameterTambahan.getLabelInputan());
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bandbox.setOpen(false);
				}
			});
			cancel.setParent(toolbar);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			org.zkoss.zul.Column column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Parameter");

			for (String c : treeSet) {
				column = new org.zkoss.zul.Column();
				column.setParent(columns);
				column.setLabel(c);
			}

			Rows rows = new Rows();
			rows.setParent(grid);

			for (String rowData : rowsData) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				String[] colAtauRow = rowData.split("->");
				String rData = colAtauRow[0];
				row.appendChild(new Label(rData));

				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);

				for (String c : treeSet) {
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						String col = ssss[0].trim();

						if (col.equalsIgnoreCase(c)) {
							final String nilai = ssss.length > 1 ? ssss[1] : s;
							Radio radio = new Radio(nilai);
							radio.setValue(nilai);
							radio.setParent(row);
							radio.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									bandbox.setValue(nilai);
									bandbox.setOpen(false);
									bandbox.setAttribute("nilai", nilai);

									if (eventListener != null) {
										eventListener.onEvent(new Event("", bandbox, nilai));
									}
								}
							});
						}
					}
				}
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_NILAI)) {
			final Bandbox bandbox = new Bandbox();
			JSONObject temporary;
			try {
				temporary = val == null || val.isEmpty() ? new JSONObject() : new JSONObject(val);
			} catch (Exception e) {
				temporary = new JSONObject();
			}
			final JSONObject jsonObject = temporary;

			bandbox.setValue(val);
			TreeSet<String> treeSet = new TreeSet<String>();
			String[] rowsData = StringUtils.split(parameterTambahan.getNilaiDataInputan(), "\n");
			Arrays.sort(rowsData);
			for (String rowData : rowsData) {
				String[] colAtauRow = rowData.split("->");
				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");
					String col = ssss[0].trim();
					treeSet.add(col);
				}
			}

			Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
			bandpopup.setParent(bandbox);
			bandpopup.setWidth("700px");
			bandpopup.setHeight("400px");

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(bandpopup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle(parameterTambahan.getLabelInputan());
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bandbox.setOpen(false);
				}
			});
			cancel.setParent(toolbar);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			org.zkoss.zul.Column column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Parameter");

			for (String c : treeSet) {
				column = new org.zkoss.zul.Column();
				column.setParent(columns);
				column.setLabel(c);
			}

			Rows rows = new Rows();
			rows.setParent(grid);

			for (String rowData : rowsData) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				String[] colAtauRow = rowData.split("->");
				final String rData = colAtauRow[0].trim();
				row.appendChild(new Label(rData));

				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);

				for (String c : treeSet) {
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						final String col = ssss[0].trim();

						if (col.equalsIgnoreCase(c)) {
							final String nilai = ssss.length > 1 ? ssss[1] : s;
							final Radio radio = new Radio(nilai);
							radio.setValue(nilai);
							radio.setParent(row);

							try {
								String key = rData.toLowerCase();
								JSONObject rowDataNilai = jsonObject.isNull(key) ? new JSONObject()
										: jsonObject.getJSONObject(key);
								String ni = rowDataNilai.isNull(col) ? "" : rowDataNilai.getString(col);
								radio.setChecked(nilai.equalsIgnoreCase(ni));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:1044"); }

							radio.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									String key = rData.toLowerCase();
									JSONObject rowData = jsonObject.isNull(key) ? new JSONObject()
											: jsonObject.getJSONObject(key);
									rowData.put(col, nilai);
									jsonObject.put(key, rowData);
									String nil = jsonObject.toString();
									bandbox.setValue(nil);
									bandbox.setAttribute("nilai", nil);

									if (eventListener != null) {
										eventListener.onEvent(new Event("", bandbox, nil));
									}
								}
							});
						}
					}
				}
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_COMBO)) {
			final Bandbox bandbox = new Bandbox();
			JSONObject temporary;
			try {
				temporary = val == null || val.isEmpty() ? new JSONObject() : new JSONObject(val);
			} catch (Exception e) {
				temporary = new JSONObject();
			}
			final JSONObject jsonObject = temporary;

			bandbox.setValue(val);
			TreeSet<String> treeSet = new TreeSet<String>();
			String[] rowsData = StringUtils.split(parameterTambahan.getNilaiDataInputan(), "\n");
			Arrays.sort(rowsData);
			for (String rowData : rowsData) {
				String[] colAtauRow = rowData.split("->");
				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");
					String col = ssss[0].trim();
					treeSet.add(col);
				}
			}

			Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
			bandpopup.setParent(bandbox);
			bandpopup.setWidth("500px");
			bandpopup.setHeight("300px");

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(bandpopup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle(parameterTambahan.getLabelInputan());
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bandbox.setOpen(false);
				}
			});
			cancel.setParent(toolbar);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			org.zkoss.zul.Column column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Parameter");

			column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Nilai");

			Rows rows = new Rows();
			rows.setParent(grid);

			for (String rowData : rowsData) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				String[] colAtauRow = rowData.split("->");
				final String rData = colAtauRow[0].trim();
				row.appendChild(new Label(rData));

				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);

				final Combobox comboboxNilai = new Combobox();
				comboboxNilai.setWidth("95%");
				row.appendChild(comboboxNilai);
				comboboxNilai.setReadonly(true);

				for (String c : treeSet) {
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						String col = ssss[0].trim();

						if (col.equalsIgnoreCase(c)) {
							String nilai = ssss.length > 1 ? ssss[1] : s;
							Comboitem radio = new Comboitem(nilai);
							radio.setValue(nilai);
							radio.setParent(comboboxNilai);
						}
					}
				}

				try {
					String key = rData.toLowerCase();
					String rowDataNilai = jsonObject.isNull(key) ? null : jsonObject.getString(key);
					Common.selectComboItem(comboboxNilai, rowDataNilai);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:1197"); }

				comboboxNilai.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						String key = rData.toLowerCase();
						jsonObject.put(key, comboboxNilai.getSelectedItem() == null ? ""
								: comboboxNilai.getSelectedItem().getValue());

						String nil = jsonObject.toString();
						bandbox.setValue(nil);
						bandbox.setAttribute("nilai", nil);

						if (eventListener != null) {
							eventListener.onEvent(new Event("", bandbox, nil));
						}
					}
				});
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_BANYAK)) {
			component = new Vbox();
			String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
			Arrays.sort(ss);
			for (String s : ss) {
				MyCheckboxConfig comboitem = new MyCheckboxConfig(s);
				comboitem.setValue(s);
				component.appendChild(comboitem);

				if(val != null) {
					for (String g : val.split(";")) {
						if (g.trim().equalsIgnoreCase(s.trim())) {
							comboitem.setChecked(true);
						}
					}
				}

				if (eventListener != null) {
					comboitem.addEventListener("onClick", eventListener);
				}
			}

		} else if (ParameterTambahanAstract.CUSTOM_PILIHAN.contains(parameterTambahan.getTipeDataInputan())) {
			component = ParameterTambahanAstract.ambilComponentCustom(val, parameterTambahan, eventListener);
		} else {
			component = null;
		}
		
		return component;
	}

	public static Component ambilComponentCustom(String val, ParameterTambahan parameterTambahan,
			EventListener eventListener) {
		if (val != null && val.contains("->")) {
			val = val.split("->")[0];
		}
		
		Component component = new Label();
		if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MAHASISWA)) {
			component = new AmbilDataSemuaMahasiswaBanbox();
			Mahasiswa mahasiswa = (Mahasiswa) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Mahasiswa.class.getName(), Long.parseLong(val)));
			component.setAttribute("mahasiswa", mahasiswa);
			component.setAttribute("myValue", mahasiswa);
			((AmbilDataSemuaMahasiswaBanbox) component).setWidth("90%");
			((AmbilDataSemuaMahasiswaBanbox) component)
					.setValue(mahasiswa == null ? "" : mahasiswa.getNim() + "-" + mahasiswa.getNama());
			if (eventListener != null) {
				((AmbilDataSemuaMahasiswaBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_SISWA)) {
			component = new AmbilDataSiswaBanbox();
			Siswa siswa = (Siswa) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(val)));
			component.setAttribute("siswa", siswa);
			component.setAttribute("myValue", siswa);
			((AmbilDataSiswaBanbox) component)
					.setValue(siswa == null ? "" : siswa.getNomorIndukNasional() + "-" + siswa.getNama());
			((AmbilDataSiswaBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataSiswaBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_DOSEN)) {
			component = new AmbilDataDosenBanbox();
			Dosen dosen = (Dosen) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(val)));
			component.setAttribute("dosen", dosen);
			component.setAttribute("myValue", dosen);
			((AmbilDataDosenBanbox) component).setValue(dosen == null ? "" : dosen.getNama());
			((AmbilDataDosenBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataDosenBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_GURU)) {
			component = new AmbilDataGuruBanbox();
			Guru guru = (Guru) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Guru.class.getName(), Long.parseLong(val)));
			component.setAttribute("guru", guru);
			component.setAttribute("myValue", guru);
			((AmbilDataGuruBanbox) component).setValue(guru == null ? "" : guru.getNim() + "-" + guru.getNama());
			((AmbilDataGuruBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataGuruBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PEGAWAI)) {
			component = new AmbilDataPegawaiBanbox(false, true);
			Pegawai pegawai = (Pegawai) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Pegawai.class.getName(), Long.parseLong(val)));
			component.setAttribute("pegawai", pegawai);
			component.setAttribute("myValue", pegawai);
			((AmbilDataPegawaiBanbox) component)
					.setValue(pegawai == null ? "" : pegawai.getNim() + "-" + pegawai.getNama());
			((AmbilDataPegawaiBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataPegawaiBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PENYEDIA)) {
			component = new AmbilDataPenyediaAssetBanbox();
			PenyediaAsset penyedia = (PenyediaAsset) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(PenyediaAsset.class.getName(), Long.parseLong(val)));
			component.setAttribute("penyediaAsset", penyedia);
			component.setAttribute("myValue", penyedia);
			((AmbilDataPenyediaAssetBanbox) component).setValue(penyedia == null ? "" : penyedia.getNama());
			((AmbilDataPenyediaAssetBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataPenyediaAssetBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_KELAS_SISWA)) {
			component = new AmbilDataKelasSiswaBanbox();
			KelasSiswa kelasSiswa = (KelasSiswa) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(KelasSiswa.class.getName(), Long.parseLong(val)));
			component.setAttribute("kelasSiswa", kelasSiswa);
			component.setAttribute("myValue", kelasSiswa);
			((AmbilDataKelasSiswaBanbox) component)
					.setValue(kelasSiswa == null ? "" : kelasSiswa.getNim() + "-" + kelasSiswa.getNama());
			((AmbilDataKelasSiswaBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataKelasSiswaBanbox) component).setEventListener(eventListener);
			}
		}

		return component;
	}

	public static String ambilVal(Row row, ParameterTambahan parameterTambahan) {
		String componentData = "component";
		return ambilVal(row, parameterTambahan, componentData);
	}

	public static String ambilVal(Row row, ParameterTambahan parameterTambahan, String componentData) {
		if (row == null) {
			return "";
		}
		Component component = (Component) row.getAttribute(componentData);
		if (component == null) {
			/*
			 * Cadangan bila atribut komponen tidak terpasang. Susunan baku satu baris
			 * parameter adalah [0] = Label judul, [1] = komponen masukan.
			 *
			 * TETAPI baris bisa sah-sah saja hanya berisi label: {@link #initComponent}
			 * MENDAFTARKAN baris ke parameterRows TANPA SYARAT (lihat parameterRows.add(row)
			 * di method itu), termasuk ketika rantai pembuatan komponen tidak menghasilkan
			 * apa pun -- mis. tipeDataInputan yang belum punya cabang penanganan, atau null.
			 *
			 * Dahulu indeks 1 diambil langsung dan IndexOutOfBoundsException-nya ditangkap
			 * sebagai KENDALI ALUR. Hasil akhirnya memang sama (nilai kosong), tetapi setiap
			 * penyimpanan membanjiri ErrorAudit dengan stack trace penuh -- satu per baris
			 * bermasalah, per simpan; terpantau di produksi lewat
			 * BiodataMahasiswa.populateParameterTambahanAlumni. Menangkap exception juga jauh
			 * lebih mahal daripada sekadar memeriksa ukuran daftar. Karena itu jumlah anak
			 * diperiksa lebih dulu, dan ketiadaan komponen diperlakukan sebagai keadaan
			 * WAJAR (nilai kosong), bukan sebagai error.
			 */
			java.util.List<?> anak = row.getChildren();
			if (anak == null || anak.size() < 2) {
				return "";
			}
			Object kandidat = anak.get(1);
			if (!(kandidat instanceof Component)) {
				return "";
			}
			component = (Component) kandidat;
		}
		return ambilValComponent(component, parameterTambahan);
	}

	/**
	 * Membaca teks MENTAH sebuah komponen masukan ZK TANPA memicu validasi.
	 *
	 * <p><b>Kenapa perlu.</b> {@code InputElement.getText()} bukan sekadar getter: ia
	 * MEM-VALIDASI ulang isi komponen dan melempar
	 * {@code WrongValueException: You must specify a number, rather than -.} bila pengguna
	 * mengetik teks yang belum berupa angka (mis. baru mengetik tanda minus "-"). Karena
	 * {@code getText()} itu justru dipakai pada jalur PEMULIHAN error, satu ketikan "-" membuat
	 * jalur pemulihan ikut gagal sehingga teks mentahnya hilang sama sekali dan proses simpan
	 * terganggu. {@code getRawText()} mengembalikan nilai apa adanya dari klien tanpa validasi,
	 * jadi dipakai lebih dulu; {@code getText()} hanya sebagai cadangan (dibungkus try-catch),
	 * dan bila semuanya gagal hasilnya null yang diartikan "tidak ada nilai".</p>
	 */
	private static String ambilTeksMentahAman(org.zkoss.zul.impl.InputElement inputElement) {
		if (inputElement == null) {
			return null;
		}
		try {
			String raw = inputElement.getRawText();
			if (raw != null) {
				return raw;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:ambilTeksMentahAman-getRawText");
		}
		try {
			Object rawValue = inputElement.getRawValue();
			if (rawValue != null) {
				return rawValue.toString();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:ambilTeksMentahAman-getRawValue");
		}
		try {
			return inputElement.getText();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:ambilTeksMentahAman-getText");
		}
		return null;
	}

	private static Double ambilAngkaPertamaAman(String raw) {
		if (raw == null || raw.trim().length() == 0) {
			return null;
		}
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([+-]?[0-9]+([.,][0-9]+)?)").matcher(raw);
		if (!m.find()) {
			return null;
		}
		try {
			return Double.valueOf(Double.parseDouble(m.group(1).replace(",", ".")));
		} catch (Exception e) {
			return null;
		}
	}

	private static Date parseTanggalAman(String raw, java.text.DateFormat format) {
		if (raw == null || raw.trim().length() == 0 || format == null) {
			return null;
		}
		try {
			return format.parse(raw.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static Date parseTanggalDanWaktu(String raw) {
		if (raw == null || raw.trim().length() == 0) {
			return null;
		}
		Date hasil = parseTanggalAman(raw, Common.dateFormat.get());
		if (hasil != null) {
			return hasil;
		}
		String[] polaIso = new String[] { "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss" };
		for (int i = 0; i < polaIso.length; i++) {
			try {
				java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(polaIso[i]);
				format.setLenient(false);
				return format.parse(raw.trim());
			} catch (java.text.ParseException ignored) {
				// Coba pola berikutnya; input HTML datetime-local memang memakai ISO.
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String ambilValComponent(Component component, ParameterTambahan parameterTambahan) {
		String val = "";
		
		if (component == null) return val;

		try {
			if (parameterTambahan.getNilaiTidakBolehDiubah()) {
				if (component instanceof Label) {
					if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)
					    || parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
						try {
							val = Common.numberFormat.get().parse((((Label) component).getValue()).trim()).doubleValue() + "";
						} catch (Exception e) {
							val = (((Label) component).getValue()).trim();
						}
					} else {
						val = (((Label) component).getValue()).trim();
					}
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_OBJECT)) {
				GeneralValueObject generalValueObject = (GeneralValueObject) component.getAttribute("data");
				val = generalValueObject == null ? "-1" : generalValueObject.getId().toString();
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_NILAI)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_COMBO)) {
				if (component instanceof Textbox) {
					val = (((Textbox) component).getValue()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_CUSTOM)) {
				if (component instanceof Combobox) {
					val = (String) (((Combobox) component).getSelectedItem() == null ? ""
							: (((Combobox) component).getSelectedItem().getValue()));
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_BANYAK)) {
				// PERBAIKAN: Gunakan StringBuilder dibanding manipulasi string manual (+)
				StringBuilder sb = new StringBuilder();
				List<Component> components = component.getChildren();
				for (Component compo : components) {
					if (compo instanceof Checkbox) {
						Checkbox c = (Checkbox) compo;
						if (c.isChecked()) {
							if (sb.length() > 0) sb.append(";");
							sb.append(String.valueOf(c.getValue()));
						}
					}
				}
				val = sb.toString();
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL_DAN_WAKTU)) {
				if (component instanceof MyDatebox) {
					Date nilai;
					try {
						nilai = (((MyDatebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						String raw = ambilTeksMentahAman((MyDatebox) component);
						nilai = parseTanggalAman(raw, Common.dateFormat.get());
					}
					val = nilai == null ? "" : Common.dateFormat.get().format(nilai);
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.WAKTU)) {
				if (component instanceof MyTimebox) {
					Date nilai = (((MyTimebox) component).getValue());
					val = nilai == null ? "" : Common.timeFormat.get().format(nilai);
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL)) {
				if (component instanceof MyDatebox) {
					Date nilai;
					try {
						nilai = (((MyDatebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						String raw = ambilTeksMentahAman((MyDatebox) component);
						nilai = parseTanggalAman(raw, Common.dateFormat1.get());
					}
					val = nilai == null ? "" : Common.dateFormat1.get().format(nilai);
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)) {
				if (component instanceof MyDoublebox) {
					try {
						val = (((MyDoublebox) component).getValue()) + "";
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						// FIX WrongValueException "You must specify a number, rather than ...":
						// Doublebox.getValue() melempar exception bila teks mentah tak bisa
						// di-parse sbg angka (mis. nilai lama tersimpan sbg string bebas "4
						// orang" sebelum field ini jadi Doublebox). JANGAN lempar ulang --
						// akan menggagalkan seluruh populateParameterTambahan utk field lain.
						// Fallback: ambil angka di depan teks mentah, bila tak ada anggap 0.
						// FIX WrongValueException "You must specify a number, rather than -.":
						// pembacaan teks mentah TIDAK BOLEH memakai getText() secara langsung,
						// karena getText() memvalidasi ulang isinya dan ikut melempar
						// WrongValueException (mis. pengguna baru mengetik tanda minus "-"),
						// sehingga jalur pemulihan ini justru ikut gagal dan proses simpan
						// parameter tambahan alumni batal. Pakai pembaca mentah yang aman
						// (getRawText lebih dulu) -- lihat ambilTeksMentahAman.
						String raw = ambilTeksMentahAman((MyDoublebox) component);
						Double fallback = ambilAngkaPertamaAman(raw);
						if (fallback == null) {
							fallback = ambilAngkaPertamaAman(wve.getMessage());
						}
						val = fallback == null ? "" : fallback + "";
					}
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
				if (component instanceof MyTextboxAngka) {
					val = (((MyTextboxAngka) component).getValue()) + "";
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_YA_TIDAK)) {
				if (component instanceof Combobox && ((Combobox) component).getSelectedItem() != null) {
					val = ((Boolean) ((Combobox) component).getSelectedItem().getValue()) + "";
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MAHASISWA)) {
				if (component instanceof AmbilDataMahasiswaBanbox) {
					Mahasiswa mahasiswa = ((Mahasiswa) component.getAttribute("mahasiswa"));
					val = mahasiswa == null ? ""
							: mahasiswa.getId().toString() + "->" + (mahasiswa.getNim() + " " + mahasiswa.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_SISWA)) {
				if (component instanceof AmbilDataSiswaBanbox) {
					Siswa siswa = ((Siswa) component.getAttribute("siswa"));
					val = siswa == null ? ""
							: siswa.getId().toString() + "->" + siswa.getNomorIndukNasional() + " " + siswa.getNama();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_DOSEN)) {
				if (component instanceof AmbilDataDosenBanbox) {
					Dosen dosen = ((Dosen) component.getAttribute("dosen"));
					val = dosen == null ? ""
							: dosen.getId().toString() + "->" + (dosen.getNidn() + " " + dosen.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_GURU)) {
				if (component instanceof AmbilDataGuruBanbox) {
					Guru guru = ((Guru) component.getAttribute("guru"));
					val = guru == null ? ""
							: guru.getId().toString() + "->" + (guru.getKode() + " " + guru.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PEGAWAI)) {
				if (component instanceof AmbilDataPegawaiBanbox) {
					Pegawai pegawai = ((Pegawai) component.getAttribute("pegawai"));
					val = pegawai == null ? ""
							: pegawai.getId().toString() + "->" + (pegawai.getMycode() + " " + pegawai.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PENYEDIA)) {
				if (component instanceof AmbilDataPenyediaAssetBanbox) {
					PenyediaAsset penyedia = ((PenyediaAsset) component.getAttribute("penyediaAsset"));
					val = penyedia == null ? "" : penyedia.getId().toString() + "->"
							+ (penyedia.getNama() == null ? "" : penyedia.getNama().trim());
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_KELAS_SISWA)) {
				if (component instanceof AmbilDataKelasSiswaBanbox) {
					KelasSiswa kelasSiswa = ((KelasSiswa) component.getAttribute("kelasSiswa"));
					val = kelasSiswa == null ? "" : kelasSiswa.getId().toString() + "->" + (kelasSiswa.getNama()).trim();
				}
			}
		} catch (org.zkoss.zk.ui.WrongValueException wve) {
			// Nilai parameter tambahan berasal dari isian dinamis. WrongValueException di sini
			// adalah input pengguna/data lama yang tidak sesuai tipe komponen, bukan error server.
			// Biarkan nilai kosong agar field lain tetap tersimpan dan log produksi tidak banjir.
			val = "";
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:1466");
		}

		try {
			if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
				if (component instanceof Textbox) {
					val = (((Textbox) component).getValue()).trim();
				} else if (component instanceof Combobox) {
					val = (((Combobox) component).getSelectedItem() == null ? ""
							: (((Combobox) component).getSelectedItem().getValue())).toString();
				} else if (component instanceof Datebox) {
					Date nilai;
					try {
						nilai = (((Datebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						nilai = parseTanggalAman(ambilTeksMentahAman((Datebox) component), Common.dateFormat1.get());
					}
					val = nilai == null ? "" : Common.dateFormat1.get().format(nilai);
				} else if (component instanceof Doublebox) {
					Double nilai;
					try {
						nilai = (((Doublebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						nilai = ambilAngkaPertamaAman(ambilTeksMentahAman((Doublebox) component));
						if (nilai == null) {
							nilai = ambilAngkaPertamaAman(wve.getMessage());
						}
					}
					val = nilai == null ? "" : nilai + "";
				} else if (component instanceof Intbox) {
					val = (((Intbox) component).getValue()) + "";
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:1486");
		}

		if (val != null) {
			val = org.apache.commons.lang3.StringUtils.replace(val, "\n", " ");
		} else {
			val = "";
		}

		return val;
	}
}
