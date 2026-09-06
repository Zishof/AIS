package ais.action.master.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.KomponenPenilaianSkripsi;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.SkripsiPunyaKomponenPenilaianSkripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper composer ZK terbesar dan paling kompleks untuk mengelola penilaian sidang
 * {@link Skripsi} (skripsi/tugas akhir), mencakup data administratif sidang (tanggal/waktu, lokasi,
 * lampiran PDF/PPT, catatan) dan penilaian per dosen penilai (ketua sidang, pembimbing 1-3,
 * penguji 1-5 — jumlah dan urutan peran ditentukan oleh {@link ais.database.model.FormatNilaiSkripsi}
 * yang dipakai skripsi tersebut).
 *
 * <p>
 * Dipanggil lewat dua titik masuk utama:
 * </p>
 * <ul>
 * <li>{@link #display(Skripsi, Component, EventListener)} — membangun panel dua kolom: kolom kiri
 * berisi form administratif sidang (status telah-sidang, jadwal, unggah PDF/PPT, lokasi, catatan
 * penting, tanpa-perbaikan, pemilihan {@link Detailperkuliahan}/{@code FormatNilai} tujuan nilai
 * masuk KRS, tombol cetak "Blanko Penilaian"/"Berita Acara", reset seluruh nilai); kolom kanan
 * berisi dashboard ringkasan nilai ({@link #buatDashboardNilai()} — HTML kustom dengan bar chart
 * per dosen dan "radar" visual bobot) diikuti grid daftar dosen penilai
 * ({@link DetailKelompokKknRenderer} — nama kelas warisan dari pola serupa di helper KKN, isinya
 * sebenarnya daftar dosen penilai skripsi, BUKAN kelompok KKN).</li>
 * <li>{@link #init(Dosen, String)} (dipicu dari tombol "Penilaian" pada baris dosen) — window modal
 * entri nilai per komponen ({@link KomponenPenilaianSkripsi}, bisa berjenjang parent-child) untuk
 * satu dosen pada satu peran ({@code jenis}), dengan catatan dosen dan lampiran per peran. Setiap
 * perubahan nilai komponen langsung memicu hitung ulang total via {@code hitungUlang} (listener
 * lokal di dalam {@code init}) yang memanggil {@link Skripsi#cariNilaiDariDosen}, menentukan nilai
 * huruf lewat {@link Common#getNilaiHuruf}, dan menyinkronkan hasil ke {@link Detailperkuliahan}
 * terkait (bila skripsi sudah tertaut ke KRS) agar transkrip ikut ter-update.</li>
 * </ul>
 *
 * <p>
 * Hak akses berlapis: field administratif dan entri nilai hanya bisa diedit oleh user non-mahasiswa
 * (staf/dosen); mengganti dosen penilai ({@link #tampilkanFormUbahDosen}) hanya untuk user
 * non-mahasiswa DAN bukan dosen (staf/admin) — mengganti dosen otomatis memindahkan riwayat nilai
 * lama (tersimpan sebagai string terenkode di {@code Skripsi#getDetailNilai()}) dari dosen lama ke
 * dosen baru lewat {@link #pindahkanDetailNilaiDosen} serta menyinkronkan
 * {@link MahasiswaRequestTugasAkhir} bila ada. Dosen hanya bisa mengedit nilai untuk perannya
 * sendiri (dicek lewat perbandingan id dosen login terhadap dosen baris). Nilai dapat disembunyikan
 * dari mahasiswa lewat flag {@code Skripsi#getSembunyikanNilaiKemahasiswa()}.
 * </p>
 */
public class PenilaianSkripsiHelper implements DataLoader {

	/**
	 * Grid ZK berisi satu baris per dosen penilai (model diisi dari
	 * {@link Skripsi#dataDosen(boolean)}, dirender oleh {@link DetailKelompokKknRenderer}). Dibuat
	 * di {@link #display(Skripsi, Component, EventListener)} dan diisi ulang tiap
	 * {@link #loadData(Object)}.
	 */
	private MyGrid grid;

	/**
	 * Skripsi yang sedang dinilai — seluruh state layar bergantung padanya. Diisi sekali di
	 * {@link #display(Skripsi, Component, EventListener)}; semua method lain memperlakukannya
	 * sebagai konteks implisit, sehingga satu instance helper hanya boleh melayani SATU skripsi.
	 */
	private Skripsi skripsi;

	/**
	 * Callback milik pemanggil yang dipicu setiap kali nilai/penugasan dosen berubah (event data
	 * berisi {@link #skripsi}), dipakai layar induk untuk menyegarkan ringkasan/daftar miliknya.
	 * Boleh {@code null} — setiap pemanggilan selalu dijaga pengecekan null.
	 */
	private EventListener eventListener;

	/** Footer grid yang menampilkan total nilai akhir skripsi; dibangun ulang tiap {@link #loadData(Object)}. */
	private Footer footerRataRataNilai;

	/** Footer grid yang menampilkan nilai huruf akhir skripsi; dibangun ulang tiap {@link #loadData(Object)}. */
	private Footer footerNilaiHuruf;

	/**
	 * Pengguna yang sedang login, disalin sekali di konstruktor. Menjadi dasar SELURUH keputusan hak
	 * akses di kelas ini: {@link #bolehKelolaNilai()}, {@link #bolehUbahDosenPenilai()},
	 * {@link #nilaiDisembunyikanUntukMahasiswa()}, serta pengecekan "dosen ini adalah saya" pada
	 * {@link #init(Dosen, String)} dan {@link DetailKelompokKknRenderer}. Bisa {@code null} bila
	 * tidak ada sesi user; semua pemakaian dijaga pengecekan null.
	 */
	private Tbmuser tbmuser = null;

	/**
	 * Menyiapkan helper dengan mengambil identitas pengguna login ({@link Common#getCurrentUser()})
	 * ke {@link #tbmuser}. Instance baru dibuat setiap kali layar penilaian dibuka (bukan
	 * singleton), sehingga snapshot user selalu segar; {@link #skripsi} baru terisi kemudian lewat
	 * {@link #display(Skripsi, Component, EventListener)}.
	 */
	public PenilaianSkripsiHelper() {
		tbmuser = Common.getCurrentUser();
	}


	/** @return {@code value}, atau string kosong bila {@code null}. */
	private String safeString(String value) {
		return value == null ? "" : value;
	}

	/** @return {@code value} (via {@link #safeString}) dengan karakter HTML khusus di-escape, untuk disisipkan aman ke dalam markup {@link #buatDashboardNilai()}. */
	private String html(String value) {
		String text = safeString(value);
		text = text.replace("&", "&amp;");
		text = text.replace("<", "&lt;");
		text = text.replace(">", "&gt;");
		text = text.replace("\"", "&quot;");
		return text;
	}

	/** @return {@code value} sebagai {@link JSONObject}, atau objek JSON kosong bila {@code value} null/kosong/tidak valid. */
	private JSONObject safeJson(String value) {
		try {
			if (value == null || value.trim().isEmpty()) {
				return new JSONObject();
			}
			return new JSONObject(value);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	/** @return {@code value}, atau {@code 0.0} bila {@code null}. */
	private Double safeDouble(Double value) {
		return value == null ? 0.0 : value;
	}

	/** @return {@code value} (via {@link #safeDouble}) diformat sesuai {@link Common#numberFormat}. */
	private String formatDouble(Double value) {
		return Common.numberFormat.get().format(safeDouble(value));
	}

	/** @return {@code true} bila user login berwenang mengelola nilai/data administratif skripsi (bukan mahasiswa maupun siswa). */
	private boolean bolehKelolaNilai() {
		return tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null;
	}

	/** @return {@code true} bila user login berwenang mengganti dosen penilai (staf/admin — bukan mahasiswa maupun dosen). */
	private boolean bolehUbahDosenPenilai() {
		return tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null;
	}

	/**
	 * Memindahkan riwayat nilai tersimpan dari {@code dosenLama} ke {@code dosenBaru} di dalam
	 * {@link Skripsi#getDetailNilai()} — string terenkode berformat entri dipisah {@code ";"}, tiap
	 * entri dipisah {@code ","} dengan elemen kedua berupa id dosen. Tidak melakukan apa pun bila
	 * salah satu dosen {@code null}/belum ber-id, atau bila keduanya sama.
	 */
	private void pindahkanDetailNilaiDosen(Dosen dosenLama, Dosen dosenBaru) {
		if (skripsi == null || dosenLama == null || dosenBaru == null || dosenLama.getId() == null
				|| dosenBaru.getId() == null || dosenLama.getId().equals(dosenBaru.getId())) {
			return;
		}

		String detailNilai = skripsi.getDetailNilai();
		if (detailNilai == null || detailNilai.trim().isEmpty()) {
			return;
		}

		StringBuilder hasil = new StringBuilder();
		for (String nilai : detailNilai.split(";")) {
			if (nilai == null || nilai.trim().isEmpty()) {
				continue;
			}
			String[] bagian = nilai.split(",");
			if (bagian.length > 1 && dosenLama.getId().toString().equals(bagian[1].trim())) {
				bagian[1] = dosenBaru.getId().toString();
				nilai = "";
				for (String item : bagian) {
					nilai += nilai.isEmpty() ? item : "," + item;
				}
			}
			hasil.append(hasil.length() == 0 ? nilai : ";" + nilai);
		}
		skripsi.setDetailNilai(hasil.toString());
	}

	/**
	 * Menyinkronkan penggantian dosen penilai ke {@link MahasiswaRequestTugasAkhir} terkait skripsi
	 * (bila ada): field {@code dosen1}/{@code dosen2}/{@code dosen3} pada permintaan tugas akhir
	 * diperbarui sesuai peran ({@code jenis}) yang cocok dengan konfigurasi
	 * {@link ais.database.model.FormatNilaiSkripsi} skripsi ini.
	 */
	private void sinkronkanRequestTugasAkhir(Dosen dosenBaru, String jenis) {
		MahasiswaRequestTugasAkhir request = skripsi == null ? null : skripsi.getMahasiswaRequestTugasAkhir();
		if (request == null || skripsi.getFormatNilaiSkripsi() == null || jenis == null) {
			return;
		}
		if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
			request.setDosen1(dosenBaru);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
			request.setDosen2(dosenBaru);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
			request.setDosen3(dosenBaru);
		}
	}

	/**
	 * Membuka window modal kecil untuk mengganti dosen penilai pada satu peran ({@code jenis}):
	 * memilih dosen baru lewat {@link AmbilDataDosenSkripsiBanbox}, lalu saat disimpan memindahkan
	 * riwayat nilai ({@link #pindahkanDetailNilaiDosen}), menyimpan penugasan baru lewat
	 * {@link Skripsi#simpanDosen(Dosen, String)}, menyinkronkan request tugas akhir
	 * ({@link #sinkronkanRequestTugasAkhir}), dan menghitung ulang total nilai
	 * ({@link #hitungUlangSemuaNilaiDosen(boolean)}).
	 *
	 * @param dosenLama dosen yang akan digantikan
	 * @param jenis     peran penilai (mis. nilai {@code dosen1}/{@code dosen2} dari
	 *                  {@code FormatNilaiSkripsi}) yang penugasannya diubah
	 * @throws Exception diteruskan dari kegagalan pembangunan UI
	 */
	private void tampilkanFormUbahDosen(final Dosen dosenLama, final String jenis) throws Exception {
		final MyWindow window = new MyWindow();
		window.setTitle("Ubah Dosen Penilai");
		window.setWidth("520px");
		window.setHeight("220px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid form = new MyGrid();
		form.setParent(center);
		form.setWidth("100%");
		Rows rows = new Rows();
		rows.setParent(form);

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label("Jenis"));
		row.appendChild(new Label(jenis == null ? "" : jenis));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label("Dosen"));
		final AmbilDataDosenSkripsiBanbox dosen = new AmbilDataDosenSkripsiBanbox();
		dosen.setWidth("95%");
		dosen.setText(dosenLama == null ? "" : dosenLama.getNama());
		dosen.setAttribute("myValue", dosenLama);
		row.appendChild(dosen);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			/**
			 * Menutup window tanpa menyimpan apa pun: penugasan dosen dan riwayat nilai dibiarkan
			 * seperti semula.
			 *
			 * @param event event {@code onClick} tombol "Batal"; isinya tidak dipakai
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			/**
			 * Mengeksekusi penggantian dosen penilai dalam satu rangkaian, berurutan dan tanpa
			 * transaksi eksplisit sendiri: memvalidasi dosen baru sudah dipilih, memindahkan riwayat
			 * nilai lama ({@link #pindahkanDetailNilaiDosen}), menulis penugasan baru
			 * ({@link Skripsi#simpanDosen(Dosen, String)}), menyinkronkan
			 * {@link MahasiswaRequestTugasAkhir} ({@link #sinkronkanRequestTugasAkhir}), menghitung
			 * ulang seluruh nilai ({@link #hitungUlangSemuaNilaiDosen(boolean)}), lalu
			 * {@code saveOrUpdate} + {@code flush}, menutup window, memuat ulang grid, dan memicu
			 * {@link #eventListener}.
			 *
			 * <p>Karena tidak ada transaksi yang membungkus keseluruhan langkah, kegagalan di
			 * tengah (mis. pada {@code flush}) dapat meninggalkan riwayat nilai sudah dipindah tetapi
			 * penugasan belum tersimpan. Dicatat apa adanya.</p>
			 *
			 * @param event event {@code onClick} tombol "Simpan"; isinya tidak dipakai
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				Dosen dosenBaru = (Dosen) dosen.getAttribute("myValue");
				if (dosenBaru == null) {
					MyMessageboxConfig.show("Dosen belum dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				pindahkanDetailNilaiDosen(dosenLama, dosenBaru);
				skripsi.simpanDosen(dosenBaru, jenis);
				sinkronkanRequestTugasAkhir(dosenBaru, jenis);
				hitungUlangSemuaNilaiDosen(false);
				HibernateUtil.currentSession().saveOrUpdate(skripsi);
				if (skripsi.getMahasiswaRequestTugasAkhir() != null) {
					HibernateUtil.currentSession().saveOrUpdate(skripsi.getMahasiswaRequestTugasAkhir());
				}
				HibernateUtil.currentSession().flush();
				window.detach();
				loadData(null);
				if (eventListener != null) {
					eventListener.onEvent(new Event("onChange", null, skripsi));
				}
			}
		});
		simpan.setParent(toolbar);

		window.onModal();
	}

	/** @return {@code true} bila nilai skripsi ini sengaja disembunyikan dari mahasiswa yang sedang login. */
	private boolean nilaiDisembunyikanUntukMahasiswa() {
		return skripsi != null && skripsi.getSembunyikanNilaiKemahasiswa() && tbmuser != null
				&& tbmuser.getMahasiswa() != null;
	}

	/** @return {@link Matakuliah} tujuan nilai skripsi (dari {@code Perkuliahan} atau matakuliah konversi pada {@link Detailperkuliahan} terkait), atau {@code null} bila tidak ada/gagal. */
	private Matakuliah getMatakuliahNilai() {
		try {
			Detailperkuliahan detailperkuliahan = skripsi == null ? null : skripsi.getDetailperkuliahan();
			return detailperkuliahan == null ? null
					: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
							: detailperkuliahan.getMatakuliahKonversi();
		} catch (Exception e) {
			return null;
		}
	}

	/** @return {@link NilaiHuruf} yang berlaku untuk {@code nilai}, berdasarkan angkatan/jurusan/fakultas mahasiswa dan tahun akademik/semester skripsi; {@code null} bila gagal ditentukan. */
	private NilaiHuruf getNilaiHuruf(Double nilai) {
		try {
			Matakuliah matakuliah = getMatakuliahNilai();
			return Common.getNilaiHuruf(nilai, skripsi.getMahasiswa().getTahunangkatan(),
					skripsi.getMahasiswa().getJurusan(), skripsi.getMahasiswa().getJurusan().getFakultas(),
					skripsi.getTahunAkademik(),
					skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
					matakuliah == null ? "" : matakuliah.getKode(),
					matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Pembantu tampilan untuk panel identitas {@link #buatDashboardNilai()}.
	 *
	 * @return nama mahasiswa pemilik skripsi, atau string kosong bila skripsi/mahasiswa belum ada
	 */
	private String getNamaMahasiswa() {
		return skripsi == null || skripsi.getMahasiswa() == null ? "" : safeString(skripsi.getMahasiswa().getNama());
	}

	/**
	 * Pembantu tampilan untuk panel identitas {@link #buatDashboardNilai()}.
	 *
	 * @return NIM mahasiswa pemilik skripsi, atau string kosong bila skripsi/mahasiswa belum ada
	 */
	private String getNimMahasiswa() {
		return skripsi == null || skripsi.getMahasiswa() == null ? "" : safeString(skripsi.getMahasiswa().getNim());
	}

	/**
	 * Pembantu tampilan untuk panel identitas {@link #buatDashboardNilai()}. Seluruh rantai
	 * dereferensi dibungkus {@code try/catch} karena mahasiswa tanpa jurusan (atau proxy Hibernate
	 * yang gagal dimuat) tidak boleh menggagalkan pembangunan dasbor.
	 *
	 * @return nama program studi/jurusan mahasiswa, atau string kosong bila tidak tersedia
	 */
	private String getNamaJurusan() {
		try {
			return skripsi.getMahasiswa().getJurusan() == null ? "" : safeString(skripsi.getMahasiswa().getJurusan().getNama());
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Pembantu tampilan untuk panel identitas {@link #buatDashboardNilai()}. Sama seperti
	 * {@link #getNamaJurusan()}, dereferensi berlapis (mahasiswa &rarr; jurusan &rarr; fakultas)
	 * dibungkus {@code try/catch} agar data tidak lengkap tidak menggagalkan dasbor.
	 *
	 * @return nama fakultas mahasiswa, atau string kosong bila tidak tersedia
	 */
	private String getNamaFakultas() {
		try {
			return skripsi.getMahasiswa().getJurusan() == null || skripsi.getMahasiswa().getJurusan().getFakultas() == null
					? "" : safeString(skripsi.getMahasiswa().getJurusan().getFakultas().getNama());
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Memetakan nama peran ({@code commonVO.getName()}, mis. label {@code dosen1}/{@code dosen2}
	 * dari {@link ais.database.model.FormatNilaiSkripsi} skripsi ini) ke kolom nilai
	 * {@link Skripsi} yang sesuai (ketua sidang, pembimbing 1-3, penguji 1-5).
	 *
	 * @return nilai dosen untuk peran tersebut, atau {@code 0.0} bila tidak cocok peran mana pun
	 */
	private Double nilaiDosen(CommonVO commonVO) {
		if (commonVO == null || skripsi == null || skripsi.getFormatNilaiSkripsi() == null) {
			return 0.0;
		}
		String jenis = commonVO.getName();
		if (jenis == null) {
			return 0.0;
		}
		if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
			return skripsi.getNilaiKetuaSidang();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
			return skripsi.getNilaiPembimbing();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
			return skripsi.getNilaiPembimbing3();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
			return skripsi.getNilaiPenguji1();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
			return skripsi.getNilaiPenguji2();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
			return skripsi.getNilaiPenguji3();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
			return skripsi.getNilaiPenguji4();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
			return skripsi.getNilaiPenguji5();
		}
		return 0.0;
	}

	/** Seperti {@link #nilaiDosen(CommonVO)}, tapi mengembalikan persentase bobot peran tersebut dari {@link ais.database.model.FormatNilaiSkripsi}, bukan nilainya. */
	private Double persenDosen(CommonVO commonVO) {
		if (commonVO == null || skripsi == null || skripsi.getFormatNilaiSkripsi() == null) {
			return 0.0;
		}
		String jenis = commonVO.getName();
		if (jenis == null) {
			return 0.0;
		}
		if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiKetuaSidang();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing3();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji1();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji2();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji3();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji4();
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
			return skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji5();
		}
		return 0.0;
	}

	/**
	 * Menyalin total nilai/IP/nilai huruf/status lulus {@link #skripsi} ke {@link Detailperkuliahan}
	 * terkait (bila skripsi sudah tertaut ke satu baris KRS), termasuk kolom "sementara" yang
	 * dihitung ulang dari nilai huruf saat ini, lalu menyimpan perubahan. Tidak melakukan apa pun
	 * bila skripsi belum tertaut ke {@code Detailperkuliahan}.
	 */
	private void sinkronkanNilaiKeDetailPerkuliahan() {
		Detailperkuliahan detailperkuliahan = skripsi == null ? null : skripsi.getDetailperkuliahan();
		if (detailperkuliahan == null) {
			return;
		}
		Matakuliah matakuliah = getMatakuliahNilai();
		Double totalSementara = skripsi.getTotalNilai();
		NilaiHuruf nilaiHuruf = null;
		try {
			nilaiHuruf = Common.getNilaiHuruf(totalSementara,
					detailperkuliahan.getMahasiswa().getTahunangkatan(),
					detailperkuliahan.getMahasiswa().getJurusan(),
					detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
					detailperkuliahan.getTahunAkademik(),
					detailperkuliahan.getPerkuliahan() == null ? null
							: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
					matakuliah == null ? "" : matakuliah.getKode(),
					matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		} catch (Exception e) {
			nilaiHuruf = getNilaiHuruf(totalSementara);
		}
		detailperkuliahan.setTotalNilai(skripsi.getTotalNilai());
		detailperkuliahan.setTotalIP(skripsi.getTotalIP());
		detailperkuliahan.setNilaiHuruf(skripsi.getNilaiHuruf());
		detailperkuliahan.setLulus(skripsi.getLulus());
		detailperkuliahan.setTotalNilaiSementara(totalSementara);
		detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
		Common.refreshUpdate(detailperkuliahan);
	}

	/** Menghitung ulang nilai huruf/IP/status lulus {@link #skripsi} dari total nilainya saat ini ({@link #getNilaiHuruf(Double)}) dan menuliskannya kembali ke {@code skripsi}. */
	private void terapkanNilaiAkhir() {
		Double total = skripsi == null ? null : skripsi.getTotalNilai();
		NilaiHuruf nilaiHuruf = getNilaiHuruf(total);
		if (nilaiHuruf != null) {
			skripsi.setTotalIP(nilaiHuruf.getNilaiDiIPK());
			skripsi.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
			skripsi.setLulus(nilaiHuruf.getLulus());
		}
	}

	/**
	 * Menghitung ulang nilai dari SETIAP dosen penilai ({@link Skripsi#dataDosen(boolean)}) lewat
	 * {@link Skripsi#cariNilaiDariDosen}, lalu menerapkan hasil akhirnya
	 * ({@link #terapkanNilaiAkhir()}), menyimpan skripsi, dan menyinkronkan ke
	 * {@link Detailperkuliahan} ({@link #sinkronkanNilaiKeDetailPerkuliahan()}). Dipanggil setelah
	 * penggantian dosen penilai agar total nilai tetap konsisten.
	 *
	 * @param refreshDetail bila {@code true}, diteruskan ke {@code cariNilaiDariDosen} untuk memaksa
	 *                      pembacaan ulang detail nilai (bukan dari cache)
	 * @return jumlah dosen yang berhasil dihitung ulang nilainya
	 */
	private int hitungUlangSemuaNilaiDosen(boolean refreshDetail) {
		int jumlah = 0;
		if (skripsi == null || skripsi.getFormatNilaiSkripsi() == null) {
			return jumlah;
		}
		try {
			HibernateUtil.currentSession().refresh(skripsi);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		List<CommonVO> dataDosen = skripsi.dataDosen(false);
		for (CommonVO commonVO : dataDosen) {
			if (commonVO == null || commonVO.getValueObject() == null || commonVO.getName() == null) {
				continue;
			}
			try {
				skripsi.cariNilaiDariDosen((Dosen) commonVO.getValueObject(), commonVO.getName(), refreshDetail);
				jumlah++;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		terapkanNilaiAkhir();
		Common.refreshUpdate(skripsi);
		sinkronkanNilaiKeDetailPerkuliahan();
		return jumlah;
	}

	/** Menampilkan pesan informasi ringkasan hasil {@link #hitungUlangSemuaNilaiDosen(boolean)}. */
	private void tampilkanPesanHitungUlang(int jumlah) {
		try {
			MyMessageboxConfig.show("Nilai berhasil dihitung ulang untuk " + jumlah
					+ " dosen. Total nilai, nilai huruf, dan data nilai mata kuliah sudah disesuaikan.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (InterruptedException e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun markup HTML+CSS custom (di-scope lewat class {@code ps-*}) berisi dashboard
	 * ringkasan penilaian sidang: kartu total nilai/nilai huruf/jumlah dosen penilai, bar
	 * perbandingan nilai per dosen (dengan bobot masing-masing), visual "radar" sederhana untuk
	 * keseimbangan bobot, dan panel identitas mahasiswa. Nilai disamarkan ("-") bila
	 * {@link #nilaiDisembunyikanUntukMahasiswa()}. Semua teks dinamis di-escape lewat {@link #html}
	 * untuk mencegah HTML injection dari data (mis. nama dosen/mahasiswa).
	 *
	 * @return komponen {@link Html} siap dipasang ke parent
	 */
	private Html buatDashboardNilai() {
		String nilaiTotal = nilaiDisembunyikanUntukMahasiswa() ? "-" : formatDouble(skripsi == null ? null : skripsi.getTotalNilai());
		String nilaiHuruf = nilaiDisembunyikanUntukMahasiswa() ? "-" : html(skripsi == null ? "" : skripsi.getNilaiHuruf());
		String statusLulus = nilaiDisembunyikanUntukMahasiswa() ? "-"
				: (skripsi != null && skripsi.getLulus() != null && skripsi.getLulus() ? "Lulus" : "Belum lulus / belum final");
		List<CommonVO> dataDosen = skripsi == null ? new ArrayList<CommonVO>() : skripsi.dataDosen(false);
		int jumlahDosen = dataDosen.size();
		double totalBobot = 0.0;
		double maxNilai = 0.0;
		StringBuilder bar = new StringBuilder();
		StringBuilder spider = new StringBuilder();
		int idx = 0;
		for (CommonVO commonVO : dataDosen) {
			Dosen dosen = commonVO == null ? null : (Dosen) commonVO.getValueObject();
			double nilai = safeDouble(nilaiDosen(commonVO));
			double persen = safeDouble(persenDosen(commonVO));
			totalBobot += persen;
			if (nilai > maxNilai) {
				maxNilai = nilai;
			}
			int width = nilai <= 0 ? 2 : (int) Math.min(100, Math.round(nilai));
			bar.append("<div class='ps-row'><div class='ps-row-title'><b>").append(html(commonVO.getName()))
					.append("</b><span>").append(formatDouble(nilai)).append(" / ").append(formatDouble(persen))
					.append("%</span></div><div class='ps-bar'><i style='width:").append(width)
					.append("%'></i></div><small>").append(html(dosen == null ? "" : dosen.getNama()))
					.append("</small></div>");
			int radius = 44;
			int angle = dataDosen.isEmpty() ? 0 : (idx * 360 / dataDosen.size());
			int length = nilai <= 0 ? 5 : (int) Math.min(44, Math.round(nilai * 44 / 100));
			spider.append("<span style='transform:rotate(").append(angle).append("deg) translateY(-")
					.append(length).append("px)'></span>");
			idx++;
		}
		String tanggalSidang = skripsi == null || skripsi.getTanggalSidang() == null ? "-"
				: Common.dateFormat4.get().format(skripsi.getTanggalSidang());
		StringBuilder html = new StringBuilder();
		html.append("<style>");
		html.append(".ps-wrap{font-family:Arial,Helvetica,sans-serif;background:#f8fafc;padding:14px;border-radius:16px;border:1px solid #e5e7eb;margin:0 0 10px 0;color:#0f172a}");
		html.append(".ps-head{display:flex;justify-content:space-between;gap:12px;align-items:stretch;flex-wrap:wrap}.ps-title{flex:2;min-width:260px;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#fff;border-radius:16px;padding:16px;box-shadow:0 8px 20px rgba(15,23,42,.12)}");
		html.append(".ps-title h2{margin:0 0 6px;font-size:18px}.ps-title p{margin:0;line-height:1.45;color:#dbeafe;font-size:12px}.ps-card{flex:1;min-width:150px;background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:13px;box-shadow:0 6px 18px rgba(15,23,42,.06)}");
		html.append(".ps-card b{display:block;font-size:20px;margin-bottom:3px}.ps-card span{font-size:11px;color:#64748b}.ps-grid{display:grid;grid-template-columns:1.5fr .9fr;gap:12px;margin-top:12px}.ps-panel{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:13px;box-shadow:0 6px 18px rgba(15,23,42,.04)}");
		html.append(".ps-panel h3{font-size:14px;margin:0 0 4px}.ps-panel p{font-size:12px;color:#64748b;margin:0 0 10px;line-height:1.45}.ps-row{margin:0 0 9px}.ps-row-title{display:flex;justify-content:space-between;gap:8px;font-size:12px}.ps-row small{font-size:10px;color:#64748b}.ps-bar{height:9px;background:#e5e7eb;border-radius:999px;overflow:hidden;margin:4px 0}.ps-bar i{display:block;height:100%;background:linear-gradient(90deg,#22c55e,#2563eb);border-radius:999px}.ps-radar{position:relative;width:160px;height:160px;border-radius:50%;margin:14px auto;background:repeating-radial-gradient(circle,#dbeafe 0,#dbeafe 1px,transparent 1px,transparent 30px),conic-gradient(from 0deg,rgba(37,99,235,.13),rgba(34,197,94,.20),rgba(37,99,235,.13));border:1px solid #bfdbfe}.ps-radar span{position:absolute;left:78px;top:78px;width:8px;height:8px;border-radius:50%;background:#2563eb;transform-origin:4px 4px}.ps-meta{display:grid;grid-template-columns:1fr 1fr;gap:8px}.ps-meta div{background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:9px;font-size:11px}.ps-meta b{display:block;font-size:12px;margin-bottom:3px}@media(max-width:900px){.ps-grid{grid-template-columns:1fr}.ps-head{display:block}.ps-card{margin-top:8px}}");
		html.append("</style>");
		html.append("<div class='ps-wrap'>");
		html.append("<div class='ps-head'><div class='ps-title'><h2>Ringkasan Penilaian Sidang</h2><p>Nilai tiap dosen, bobot, dan hasil akhir terlihat dalam satu tempat. Pengguna bisa segera tahu apakah penilaian sudah lengkap dan nilai akhir sudah sesuai.</p></div>");
		html.append("<div class='ps-card'><b>").append(nilaiTotal).append("</b><span>Total Nilai</span></div>");
		html.append("<div class='ps-card'><b>").append(nilaiHuruf).append("</b><span>Nilai Huruf</span></div>");
		html.append("<div class='ps-card'><b>").append(jumlahDosen).append("</b><span>Dosen Penilai</span></div></div>");
		html.append("<div class='ps-grid'><div class='ps-panel'><h3>Nilai per Dosen</h3><p>Perbandingan nilai membantu melihat penilaian mana yang sudah terisi dan mana yang perlu diperiksa lagi.</p>").append(bar).append("</div>");
		html.append("<div class='ps-panel'><h3>Peta Keseimbangan Nilai</h3><p>Bentuk grafik memudahkan melihat apakah nilai antar peran dosen sudah merata atau ada yang berbeda jauh.</p><div class='ps-radar'>").append(spider).append("</div><div class='ps-meta'>");
		html.append("<div><b>Status</b>").append(statusLulus).append("</div><div><b>Total Bobot</b>").append(formatDouble(totalBobot)).append("%</div>");
		html.append("<div><b>Nilai Tertinggi</b>").append(formatDouble(maxNilai)).append("</div><div><b>Tanggal Sidang</b>").append(html(tanggalSidang)).append("</div>");
		html.append("</div></div></div>");
		html.append("<div class='ps-panel' style='margin-top:12px'><h3>Data Mahasiswa</h3><p>Identitas ini memastikan nilai yang dihitung masuk ke mahasiswa dan program studi yang benar.</p><div class='ps-meta'><div><b>Mahasiswa</b>").append(html(getNamaMahasiswa())).append("</div><div><b>NIM</b>").append(html(getNimMahasiswa())).append("</div><div><b>Program Studi</b>").append(html(getNamaJurusan())).append("</div><div><b>Fakultas</b>").append(html(getNamaFakultas())).append("</div></div></div>");
		html.append("</div>");
		return new Html(html.toString());
	}

	/**
	 * Perender baris grid utama untuk satu dosen penilai (dibungkus {@link CommonVO}: nama peran +
	 * objek {@link Dosen}). Nama kelas ini warisan pola serupa dari helper KKN — isinya BUKAN
	 * kelompok KKN, melainkan satu baris "dosen + perannya" pada penilaian skripsi. Menampilkan
	 * panel catatan dosen (dapat dibuka), lampiran per peran, foto+nama dosen (dengan tombol ubah
	 * dosen bila {@link #bolehUbahDosenPenilai()}), nama peran, persentase bobot, nilai+nilai huruf
	 * (disamarkan bila {@link #nilaiDisembunyikanUntukMahasiswa()}), dan tombol buka window entri
	 * nilai ({@link #init(Dosen, String)}). Baris dosen login sendiri (bila dosen) diberi highlight
	 * latar hijau muda.
	 */
	class DetailKelompokKknRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris "dosen + peran": panel {@link MyDetail} berisi catatan dosen (JSON per
		 * peran di {@link Skripsi#getCatatanDosen()}) dan lampiran khusus peran itu, lalu foto dosen,
		 * nama dosen (plus tombol "Ubah Dosen" bila {@link #bolehUbahDosenPenilai()}), label peran,
		 * persentase bobot, nilai + nilai hurufnya, dan tombol "Penilaian" yang membuka
		 * {@link PenilaianSkripsiHelper#init(Dosen, String)}.
		 *
		 * <p><b>Peringatan pemetaan slot (bug penamaan tertukar).</b> Blok {@code if/else} di bawah
		 * mengulang lagi rantai peran &rarr; nilai/bobot yang sudah ada di {@link #nilaiDosen(CommonVO)}
		 * dan {@link #persenDosen(CommonVO)}; ini salinan ketiga dari pemetaan yang sama di file ini
		 * (salinan keempat ada di pembangun parameter "Berita Acara" pada
		 * {@link PenilaianSkripsiHelper#display(Skripsi, Component, EventListener)}). Perhatikan bahwa
		 * slot {@code dosen1} — yang <i>orang</i>-nya diambil {@link Skripsi#dataDosen(boolean)} dari
		 * kolom {@code pembimbing} — di sini dipasangkan dengan <i>nilai</i>
		 * {@link Skripsi#getNilaiKetuaSidang()} dan bobot
		 * {@code FormatNilaiSkripsi#getProsentasiNilaiKetuaSidang()}; sebaliknya slot {@code dosen2}
		 * (orang = {@code ketua_sidang}) memakai {@link Skripsi#getNilaiPembimbing()}. Inilah wujud
		 * kasat mata dari penamaan tertukar yang didokumentasikan di {@link Skripsi} dan
		 * {@link ais.database.model.FormatNilaiSkripsi}: pasangan orang/nilai memang bersilang secara
		 * <i>nama kolom</i>, tetapi dipakai bersilang secara konsisten di SETIAP tempat, sehingga
		 * angka yang tampil di baris ini tetap milik dosen yang benar. Jangan "meluruskan" salah satu
		 * sisi saja.</p>
		 *
		 * <p>Bila {@link Skripsi#getSembunyikanNilaiKemahasiswa()} aktif dan yang login mahasiswa,
		 * tiga kolom terakhir diganti "-" <b>dan tombol "Penilaian" tidak dibuat sama sekali</b> —
		 * jadi penyembunyian di sini juga menutup jalur baca detail komponen, bukan sekadar
		 * menyamarkan angka. Baris milik dosen yang sedang login diberi latar hijau muda.</p>
		 *
		 * <p>Method ini tidak menulis apa pun ke basis data; efek sampingnya murni membangun komponen
		 * ZK. Kolom persentase tetap ditampilkan apa adanya meski nilai disembunyikan.</p>
		 *
		 * @param row  baris grid yang diisi komponen anaknya
		 * @param data item model grid, selalu berupa {@link CommonVO} berisi label peran
		 *             ({@code getName()}) dan objek {@link Dosen} ({@code getValueObject()})
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final CommonVO commonVO = (CommonVO) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			JSONObject catat = safeJson(skripsi.getCatatanDosen());

			Vbox vbox = new Vbox();

			vbox.appendChild(
					new ais.ui.util.MyHtml((catat.isNull(commonVO.getName()) ? "" : catat.getString(commonVO.getName()))
							.replaceAll("\n", "<br>")));
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(),
					Skripsi.class.getName() + "_" + commonVO.getName(), "Catatan", false, new EventListener() {

						/**
						 * Callback pasca-unggah yang sengaja kosong: pada baris grid, lampiran
						 * catatan per peran cukup ditautkan oleh
						 * {@link LampiranLain#createDownloadUploadFileLain} sendiri, tidak ada state
						 * skripsi yang perlu diperbarui. Argumen tetap diabaikan.
						 *
						 * @param arg0 event unggah/hapus lampiran; tidak dipakai
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false);

			hbox.setParent(vbox);

			final Dosen dosen = (Dosen) commonVO.getValueObject();

			if (dosen != null && tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& tbmuser.getDosen().getId().equals(dosen.getId())) {
				row.setStyle("background:#eeffeb;");
			}
			CommonMedia.tampilkanGambarKecil(dosen).setParent(row);
			Hbox dosenCell = new Hbox();
			dosenCell.setSpacing("6px");
			new Label(dosen == null ? "" : dosen.getNama()).setParent(dosenCell);
			if (bolehUbahDosenPenilai()) {
				MyToolbarbuttonConfig ubahDosen = new MyToolbarbuttonConfig("Ubah Dosen", "/img/svg/edit-box-line.svg");
				ubahDosen.setTooltiptext("Ubah dosen penilai");
				ubahDosen.addEventListener("onClick", new EventListener() {
					/**
					 * Membuka form ganti dosen penilai untuk peran baris ini
					 * ({@link #tampilkanFormUbahDosen(Dosen, String)}). Tombolnya sendiri hanya
					 * dibuat bila {@link #bolehUbahDosenPenilai()}, jadi gerbang hak akses berada di
					 * sisi pembuatan komponen, bukan di dalam listener ini.
					 *
					 * @param event event {@code onClick}; isinya tidak dipakai
					 */
					@Override
					public void onEvent(Event event) throws Exception {
						tampilkanFormUbahDosen(dosen, commonVO.getName());
					}
				});
				ubahDosen.setParent(dosenCell);
			}
			ais.ui.util.MenuAksiBaris.pasang(dosenCell);
			dosenCell.setParent(row);
			new Label(commonVO.getName()).setParent(row);

			Double nilai = 0.0;
			Double persen = 0.0;
			if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
				nilai = skripsi.getNilaiKetuaSidang();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiKetuaSidang();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
				nilai = skripsi.getNilaiPembimbing();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
				nilai = skripsi.getNilaiPembimbing3();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing3();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
				nilai = skripsi.getNilaiPenguji1();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji1();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
				nilai = skripsi.getNilaiPenguji2();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji2();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
				nilai = skripsi.getNilaiPenguji3();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji3();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
				nilai = skripsi.getNilaiPenguji4();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji4();
			} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
				nilai = skripsi.getNilaiPenguji5();
				persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji5();
			}
			new Label(Common.numberFormat.get().format(persen) + " %").setParent(row);

			if (skripsi.getSembunyikanNilaiKemahasiswa() && tbmuser != null && tbmuser.getMahasiswa() != null) {
				new Label("-").setParent(row);
				new Label("-").setParent(row);
				new Label("-").setParent(row);
			} else {

				NilaiHuruf nilaiHuruf = null;

				try {
					Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
					Matakuliah matakuliah = detailperkuliahan == null ? null
							: detailperkuliahan.getPerkuliahan() != null
									? detailperkuliahan.getPerkuliahan().getMatakuliah()
									: detailperkuliahan.getMatakuliahKonversi();
					nilaiHuruf = Common.getNilaiHuruf(nilai, skripsi.getMahasiswa().getTahunangkatan(),
							skripsi.getMahasiswa().getJurusan(), skripsi.getMahasiswa().getJurusan().getFakultas(),
							skripsi.getTahunAkademik(),
							skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:469");
				}
				new Label(nilai == null ? "" : Common.numberFormat.get().format(nilai)).setParent(row);
				new Label(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf()).setParent(row);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Penilaian", "/img/svg/edit-box-line.svg");
				// Tooltip "Hapus Data" adalah sisa salin-tempel; tombol ini membuka entri nilai.
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					/**
					 * Membuka window entri nilai per komponen untuk dosen dan peran baris ini
					 * ({@link PenilaianSkripsiHelper#init(Dosen, String)}). Tombol ini muncul untuk
					 * semua pengguna yang boleh melihat nilai — termasuk mahasiswa pemilik skripsi;
					 * pembatasan hak edit dilakukan di dalam {@code init}, yang menampilkan label
					 * read-only alih-alih kotak isian bagi mahasiswa maupun dosen lain.
					 *
					 * @param event event {@code onClick}; isinya tidak dipakai
					 */
					@Override
					public void onEvent(Event event) throws Exception {

						init(dosen, commonVO.getName());

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(row);
			}
		}

	}

	/**
	 * Mengambil daftar {@link KomponenPenilaianSkripsi} yang berlaku untuk peran {@code jenis} pada
	 * jurusan/fakultas mahasiswa skripsi ini (komponen dengan jurusan/fakultas null berlaku umum),
	 * disusun sebagai peta induk → daftar anak (komponen berjenjang punya {@code parent}; komponen
	 * tanpa anak tetap masuk sebagai kunci dengan daftar kosong). Kolom kebolehan
	 * ({@code dosen1}..{@code dosen7}) yang dicek ditentukan dari kecocokan {@code jenis} terhadap
	 * label peran pada {@link ais.database.model.FormatNilaiSkripsi} skripsi ini.
	 *
	 * @param jenis label peran dosen (mis. {@code FormatNilaiSkripsi#getDosen1()})
	 * @return peta komponen induk ke daftar komponen anaknya (terurut alami/{@code Comparable})
	 */
	@SuppressWarnings("unchecked")
	private TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>> populateKomponen(String jenis) {
		String kolom = "dosen1";
		if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
			kolom = "dosen1";
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
			kolom = "dosen2";
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
			kolom = "dosen3";
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
			kolom = "dosen4";
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
			kolom = "dosen5";
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
			kolom = "dosen6";
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
			kolom = "dosen7";
		}

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianSkripsi> formatNilaiSkripsiPunyaKomponenPenilaianSkripsis = session
				.createCriteria(SkripsiPunyaKomponenPenilaianSkripsi.class)
				.setProjection(Projections.groupProperty("komponenPenilaianSkripsi"))
				.createAlias("komponenPenilaianSkripsi", "komponenPenilaianSkripsi")

				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.jurusan"),
						Restrictions.eq("komponenPenilaianSkripsi.jurusan", skripsi.getMahasiswa().getJurusan())))
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.fakultas"),
						Restrictions.eq("komponenPenilaianSkripsi.fakultas",
								skripsi.getMahasiswa().getJurusan().getFakultas())))

				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi." + kolom),
						Restrictions.eq("komponenPenilaianSkripsi." + kolom, true)))
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.aktif"),
						Restrictions.eq("komponenPenilaianSkripsi.aktif", true)))
				.add(Restrictions.eq("formatNilaiSkripsi", skripsi.getFormatNilaiSkripsi())).list();
		TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>>();
		for (KomponenPenilaianSkripsi komponenPenilaianSkripsi : formatNilaiSkripsiPunyaKomponenPenilaianSkripsis) {
			if (komponenPenilaianSkripsi.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianSkripsi.getParent())) {
					List<KomponenPenilaianSkripsi> datas = new ArrayList<KomponenPenilaianSkripsi>();
					datas.add(komponenPenilaianSkripsi);
					dataKomponenPenilaian.put(komponenPenilaianSkripsi.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianSkripsi.getParent()).add(komponenPenilaianSkripsi);
				}
			}
		}
		for (KomponenPenilaianSkripsi komponenPenilaianSkripsi : formatNilaiSkripsiPunyaKomponenPenilaianSkripsis) {
			if (komponenPenilaianSkripsi.getParent() == null
					&& !dataKomponenPenilaian.containsKey(komponenPenilaianSkripsi)) {
				List<KomponenPenilaianSkripsi> datas = new ArrayList<KomponenPenilaianSkripsi>();
				dataKomponenPenilaian.put(komponenPenilaianSkripsi, datas);
			}
		}

		return dataKomponenPenilaian;
	}

	/**
	 * Membuka window modal entri nilai untuk satu {@code dosen} pada satu peran ({@code jenis}):
	 * satu baris per {@link KomponenPenilaianSkripsi} (lewat {@link #populateKomponen(String)}),
	 * masing-masing dengan kotak input nilai (readonly bagi mahasiswa atau bagi dosen lain yang
	 * bukan pemilik baris) yang langsung menyimpan dan memicu hitung ulang total saat berubah, serta
	 * kotak catatan dosen dan lampiran per peran. Footer menampilkan total nilai peran ini beserta
	 * nilai hurufnya. Tombol "Hitung Ulang" memaksa kalkulasi ulang paksa (bypass cache); tombol
	 * "Selesai" menutup window dan memuat ulang grid pemanggil.
	 *
	 * @param dosen dosen yang nilainya dientri
	 * @param jenis peran dosen tersebut (label dari {@code FormatNilaiSkripsi})
	 * @throws Exception diteruskan dari kegagalan pembangunan UI atau akses data
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final Dosen dosen, final String jenis) throws Exception {
		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Dosen currDosen = tbmuser == null ? null : tbmuser.ambilDosen();

		addWindow.setTitle("Penilaian");
		addWindow.setWidth("850px");
		addWindow.setHeight("98%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		addWindow.appendChild(borderlayout);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid subGrid = new MyGrid();
		subGrid.setSclass("fgrid");
		subGrid.setWidth("100%");
		subGrid.setParent(center);
		subGrid.setHeight("100%");

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Komponen Penilaian"));

		Column column = new Column("Keterangan Komponen Penilaian");
		column.setWidth("40%");
		subColumns.appendChild(column);

		column = new Column("Bobot");
		column.setWidth("8%");
		subColumns.appendChild(column);

		column = new Column("Nilai");
		column.setWidth("10%");
		column.setAlign("right");
		subColumns.appendChild(column);

		final Rows subRows = new Rows();
		subRows.setParent(subGrid);

		JSONObject catat = safeJson(skripsi.getCatatanDosen());
		final Textbox catatanDosen = new Textbox(catat.isNull(jenis) ? "" : catat.getString(jenis));
		catatanDosen.setWidth("90%");
		catatanDosen.setRows(5);

		Double nilaiPembimbing = skripsi.cariNilaiDariDosen(dosen, jenis, false);
		Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
		Matakuliah matakuliah = detailperkuliahan == null ? null
				: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
		NilaiHuruf nilaiHurufpembimbing = Common.getNilaiHuruf(nilaiPembimbing,
				skripsi.getMahasiswa().getTahunangkatan(), skripsi.getMahasiswa().getJurusan(),
				skripsi.getMahasiswa().getJurusan().getFakultas(), skripsi.getTahunAkademik(),
				skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		final Footer footerTotal = new Footer(Common.numberFormat.get().format(nilaiPembimbing) + " ("
				+ (nilaiHurufpembimbing == null ? "" : nilaiHurufpembimbing.getNilaiHuruf()) + ")");

		// Listener kalkulasi bersama window ini: dipasang ke SETIAP kotak nilai, ke kotak catatan
		// dosen, dan dipanggil langsung oleh tombol "Hitung Ulang".
		final EventListener hitungUlang = new EventListener() {

			/**
			 * Inti kalkulasi window entri nilai, dipanggil setiap kali satu kotak nilai atau kotak
			 * catatan berubah, dan oleh tombol "Hitung Ulang".
			 *
			 * <p>Urutan kerjanya: hitung ulang nilai peran ini lewat
			 * {@link Skripsi#cariNilaiDariDosen(Dosen, String, Boolean)} (argumen ketiga
			 * {@code true} hanya bila event bernama {@code "Hitung Ulang"}, yaitu memaksa baca ulang
			 * detail dari basis data alih-alih memakai yang sudah ada di memori), ambil total
			 * skripsi, tentukan nilai huruf/IP/status lulus, simpan catatan dosen ke JSON
			 * {@link Skripsi#getCatatanDosen()} pada kunci peran, simpan skripsi, lalu — bila
			 * skripsi tertaut KRS — salin total/IP/nilai huruf/lulus beserta kembaran
			 * "sementara"-nya ke {@link Detailperkuliahan} dan simpan. Terakhir memicu
			 * {@link #eventListener} dan memperbarui label footer total peran ini.
			 *
			 * <p><b>Auto-save tanpa tombol Simpan:</b> perubahan satu kotak nilai langsung menembus
			 * sampai ke baris KRS/transkrip mahasiswa. Tidak ada langkah verifikasi atau persetujuan
			 * di antaranya, dan tidak ada transaksi tunggal yang membungkus rangkaian ini.</p>
			 *
			 * <p><b>Efek samping tak kentara:</b> karena catatan dosen selalu ditulis ulang dari isi
			 * kotak {@code catatanDosen}, listener ini ikut menyimpan catatan setiap kali sebuah
			 * <i>nilai</i> berubah, bukan hanya saat catatannya sendiri diubah.</p>
			 *
			 * @param arg0 event pemicu; hanya namanya yang dipakai, untuk membedakan "Hitung Ulang"
			 *             (paksa baca ulang) dari perubahan biasa
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
				Matakuliah matakuliah = detailperkuliahan == null ? null
						: detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();
				boolean refresh = arg0 != null && arg0.getName() != null
						&& arg0.getName().equalsIgnoreCase("Hitung Ulang");
				Double nilaiPembimbing = skripsi.cariNilaiDariDosen(dosen, jenis, refresh);
				Double total = skripsi.getTotalNilai();
				NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total, skripsi.getMahasiswa().getTahunangkatan(),
						skripsi.getMahasiswa().getJurusan(), skripsi.getMahasiswa().getJurusan().getFakultas(),
						skripsi.getTahunAkademik(),
						skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
						matakuliah == null ? "" : matakuliah.getKode(),
						matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

				if (nilaiHuruf != null) {
					skripsi.setTotalIP(nilaiHuruf.getNilaiDiIPK());
					skripsi.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
					skripsi.setLulus(nilaiHuruf.getLulus());
				}
				JSONObject catat = safeJson(skripsi.getCatatanDosen());
				catat.put(jenis, catatanDosen.getValue());
				skripsi.setCatatanDosen(catat.toString());
				Common.refreshUpdate(skripsi);

				if (skripsi.getDetailperkuliahan() != null) {

					detailperkuliahan.setTotalNilai(skripsi.getTotalNilai());
					detailperkuliahan.setTotalIP(skripsi.getTotalIP());
					detailperkuliahan.setNilaiHuruf(skripsi.getNilaiHuruf());
					detailperkuliahan.setLulus(skripsi.getLulus());

					Double totalSementara = skripsi.getTotalNilai();
					nilaiHuruf = Common.getNilaiHuruf(totalSementara,
							detailperkuliahan.getMahasiswa().getTahunangkatan(),
							detailperkuliahan.getMahasiswa().getJurusan(),
							detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
							detailperkuliahan.getTahunAkademik(),
							detailperkuliahan.getPerkuliahan() == null ? null
									: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

					detailperkuliahan.setTotalNilaiSementara(totalSementara);
					detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

					Common.refreshUpdate(detailperkuliahan);
				}

				if (eventListener != null) {
					eventListener.onEvent(new Event("", null, skripsi));
				}

				NilaiHuruf nilaiHurufpembimbing = Common.getNilaiHuruf(nilaiPembimbing,
						skripsi.getMahasiswa().getTahunangkatan(), skripsi.getMahasiswa().getJurusan(),
						skripsi.getMahasiswa().getJurusan().getFakultas(), skripsi.getTahunAkademik(),
						skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
						matakuliah == null ? "" : matakuliah.getKode(),
						matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

				footerTotal.setLabel(Common.numberFormat.get().format(nilaiPembimbing) + " ("
						+ (nilaiHurufpembimbing == null ? "" : nilaiHurufpembimbing.getNilaiHuruf()) + ")");
			}
		};

		TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>> dataKomponenPenilaian = populateKomponen(
				jenis);

		for (final KomponenPenilaianSkripsi parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianSkripsi> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(Common.numberFormat.get().format(parent.getBobot())));

				if ((tbmuser != null && tbmuser.getMahasiswa() != null) || (currDosen != null && !currDosen.getId().equals(dosen.getId()))) {
					row.appendChild(new Label(Common.numberFormat.get().format(skripsi.retreiveDetailNilai(parent, dosen))));
				} else {
					final MyDoublebox nilai = new MyDoublebox(skripsi.retreiveDetailNilai(parent, dosen));
					nilai.setWidth("90%");
					row.appendChild(nilai);
					row.setValign("top");
					row.setAttribute("nilai", nilai);
					row.setValign("top");
					row.setAttribute("komponen", parent);
					row.setValign("top");
					row.setAttribute("dosen", dosen);
					nilai.addEventListener("onChange", new EventListener() {

						/**
						 * Menyimpan nilai komponen tunggal (komponen tanpa anak) untuk
						 * {@code dosen} pada peran {@code jenis}, lalu memicu hitung ulang total.
						 *
						 * <p>Sebelum {@code refresh(skripsi)} dipastikan ada transaksi aktif —
						 * tanpa itu Hibernate melempar "refresh is not valid without active
						 * transaction". {@code refresh} dipakai agar entri dosen lain yang masuk
						 * bersamaan (window ini dibuka per dosen, bisa paralel) tidak tertimpa oleh
						 * salinan lama di memori. Perhatikan bahwa transaksi yang dibuka di sini
						 * tidak pernah di-commit secara eksplisit oleh listener ini; penyimpanan
						 * bersandar pada {@code Common.refreshUpdate} di dalam {@code hitungUlang}
						 * dan pada pengelolaan sesi ZK/Hibernate di lapisan luar.</p>
						 *
						 * <p>Kotak nilai ini hanya dibuat bila pengguna login berhak mengubahnya
						 * (bukan mahasiswa, dan bukan dosen selain pemilik baris); bagi yang lain
						 * yang ditampilkan hanyalah label, sehingga listener ini tidak pernah ada.
						 * Nilai yang diketik TIDAK divalidasi terhadap bobot maupun batas
						 * 0-100.</p>
						 *
						 * @param arg0 event {@code onChange} kotak nilai, diteruskan apa adanya ke
						 *             {@code hitungUlang}
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							// KE-FIX (HibernateException "refresh is not valid without active
							// transaction"): pastikan transaksi aktif sebelum refresh().
							Session sessionRefresh = HibernateUtil.currentSession();
							if (sessionRefresh.getTransaction() == null || !sessionRefresh.getTransaction().isActive()) {
								sessionRefresh.beginTransaction();
							}
							sessionRefresh.refresh(skripsi);
							skripsi.populateDetailNilai(parent, dosen, nilai.getValue(), true);
							hitungUlang.onEvent(arg0);
						}
					});
				}
			} else {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(""));
				row.appendChild(new Label(""));

				for (final KomponenPenilaianSkripsi komponenPenilaianSkripsi : datas) {

					row = new MyFormRow();
					row.setParent(subRows);
					Hbox hbox = new Hbox();
					row.appendChild(hbox);
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Label(komponenPenilaianSkripsi.getNama()));
					row.appendChild(new MyLabelAgakKecil(komponenPenilaianSkripsi.getKeterangan()));
					row.appendChild(new Label(Common.numberFormat.get().format(komponenPenilaianSkripsi.getBobot())));

					if ((tbmuser != null && tbmuser.getMahasiswa() != null)
							|| (currDosen != null && !currDosen.getId().equals(dosen.getId()))) {
						row.appendChild(new Label(Common.numberFormat.get()
								.format(skripsi.retreiveDetailNilai(komponenPenilaianSkripsi, dosen))));
					} else {

						final MyDoublebox nilai = new MyDoublebox(
								skripsi.retreiveDetailNilai(komponenPenilaianSkripsi, dosen));
						nilai.setWidth("90%");
						row.appendChild(nilai);
						row.setValign("top");
						row.setAttribute("nilai", nilai);
						row.setValign("top");
						row.setAttribute("komponen", komponenPenilaianSkripsi);
						row.setValign("top");
						row.setAttribute("dosen", dosen);
						nilai.addEventListener("onChange", new EventListener() {

							/**
							 * Kembaran listener nilai komponen tunggal, untuk komponen ANAK
							 * (sub-komponen di bawah satu komponen induk). Perilaku, penjagaan
							 * transaksi sebelum {@code refresh}, dan catatan hak akses persis sama;
							 * satu-satunya beda adalah komponen yang disimpan
							 * ({@code komponenPenilaianSkripsi}, bukan {@code parent}). Komponen
							 * induk yang punya anak sendiri tidak menerima nilai langsung — barisnya
							 * hanya judul.
							 *
							 * @param arg0 event {@code onChange} kotak nilai, diteruskan apa adanya
							 *             ke {@code hitungUlang}
							 */
							@Override
							public void onEvent(Event arg0) throws Exception {
								// KE-FIX (HibernateException "refresh is not valid without active
								// transaction"): pastikan transaksi aktif sebelum refresh().
								Session sessionRefresh = HibernateUtil.currentSession();
								if (sessionRefresh.getTransaction() == null || !sessionRefresh.getTransaction().isActive()) {
									sessionRefresh.beginTransaction();
								}
								sessionRefresh.refresh(skripsi);
								skripsi.populateDetailNilai(komponenPenilaianSkripsi, dosen, nilai.getValue(), true);
								hitungUlang.onEvent(arg0);
							}
						});
					}
				}

			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		ais.ui.util.ZkCompat.setSpans(row, "5");
		row.setParent(subRows);
		row.appendChild(new MyLabelBold("Catatan Dosen"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "5");
		row.setParent(subRows);
		if ((tbmuser != null && tbmuser.getMahasiswa() != null) || (currDosen != null && !currDosen.getId().equals(dosen.getId()))) {
			row.appendChild(new ais.ui.util.MyHtml(catatanDosen.getValue().replaceAll("\n", "<br>")));
		} else {
			row.appendChild(catatanDosen);
		}
		catatanDosen.addEventListener("onChange", hitungUlang);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "5");
		row.setParent(subRows);
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), Skripsi.class.getName() + "_" + jenis,
				"Catatan", false, new EventListener() {

					/**
					 * Callback pasca-unggah yang sengaja kosong — sama seperti kembarannya di
					 * {@link DetailKelompokKknRenderer}: lampiran catatan per peran sudah ditautkan
					 * sendiri oleh {@link LampiranLain#createDownloadUploadFileLain} dengan kunci
					 * {@code Skripsi#id} + nama peran, tidak ada state skripsi yang perlu diperbarui.
					 * Hak unggah/hapus dikendalikan oleh argumen terakhir pemanggilan ini, bukan
					 * oleh listener.
					 *
					 * @param arg0 event unggah/hapus lampiran; tidak dipakai
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false,
				!((tbmuser != null && tbmuser.getMahasiswa() != null) || (currDosen != null && !currDosen.getId().equals(dosen.getId()))));
		hbox.setParent(row);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "5");
		row.setParent(subRows);
		row.appendChild(
				new MyLabelKecil("Jika file lampiran kegiatan lebih dari satu file, zip dulu semua file tersebut"));

		Foot foot = new Foot();
		subGrid.appendChild(foot);

		Footer footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("Total");
		foot.appendChild(footer);
		foot.appendChild(footerTotal);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Menutup window entri nilai lalu menjadwalkan pemuatan ulang grid dosen penilai.
			 * Tidak ada penyimpanan di sini: seluruh perubahan sudah tersimpan seketika oleh
			 * {@code hitungUlang} pada tiap perubahan field, jadi label "Selesai" berarti "tutup",
			 * bukan "simpan".
			 *
			 * @param event event {@code onClick} tombol "Selesai"; isinya tidak dipakai
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
				Common.createDefaultTimer(new EventListener() {

					/**
					 * Dijalankan pada siklus event ZK berikutnya (lewat
					 * {@link Common#createDefaultTimer(EventListener)}) supaya {@link #loadData(Object)}
					 * membaca state skripsi SETELAH window terlepas dan penyimpanan terakhir
					 * selesai, bukan di tengah pelepasan komponen.
					 *
					 * @param arg0 event timer; tidak dipakai
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
			}
		});
		cancel.setParent(toolbar);

		cancel = new MyToolbarbuttonConfig("Hitung Ulang", "/img/Configure.gif");
		cancel.setTooltiptext("Hitung Ulang");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Menyimpan ulang SELURUH kotak nilai yang sedang tampil di window ini — dibaca dari
			 * atribut {@code "nilai"}/{@code "komponen"} yang dititipkan ke tiap baris saat
			 * dibangun — lalu memanggil {@code hitungUlang} dengan event bernama
			 * {@code "Hitung Ulang"} sehingga total dihitung ulang dari basis data, bukan dari
			 * hasil di memori.
			 *
			 * <p>Berguna untuk memulihkan total yang melenceng akibat entri bersamaan atau data
			 * detail nilai yang tidak sinkron. Baris read-only tidak punya atribut {@code "nilai"},
			 * sehingga aman dilewati.</p>
			 *
			 * @param event event {@code onClick} tombol "Hitung Ulang"; isinya tidak dipakai
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				List<Row> rows = subRows.getChildren();
				for (Row row : rows) {
					MyDoublebox nilai = (MyDoublebox) row.getAttribute("nilai");
					if (nilai != null) {
						skripsi.populateDetailNilai((KomponenPenilaianSkripsi) row.getAttribute("komponen"), dosen,
								nilai.getValue(), true);
					}
				}
				hitungUlang.onEvent(new Event("Hitung Ulang"));
			}
		});
		cancel.setParent(toolbar);

		addWindow.onModal();
	}

	/**
	 * Memuat ulang grid daftar dosen penilai ({@link Skripsi#dataDosen(boolean)}) dan footer
	 * ringkasan (total nilai, nilai huruf, checkbox "Sembunyikan nilai ke mahasiswa" bagi user
	 * berwenang). Footer nilai disamarkan bila {@link #nilaiDisembunyikanUntukMahasiswa()}.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	public void loadData(Object value) {

		ListModel strset = new SimpleListModel(skripsi.dataDosen(false));
		grid.setRowRenderer(new DetailKelompokKknRenderer());
		grid.setModelCheckMobile(strset);

		Foot foot = grid.getFoot() == null ? new Foot() : grid.getFoot();
		Common.clear(foot);
		grid.appendChild(foot);

		footerRataRataNilai = new Footer(
				skripsi.getTotalNilai() == null ? "" : Common.numberFormat.get().format(skripsi.getTotalNilai()));
		footerNilaiHuruf = new Footer(skripsi.getNilaiHuruf());

		Footer footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("");
		foot.appendChild(footer);

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig("Sembunyikan nilai ke mahasiswa");
			myCheckboxConfig.setChecked(skripsi.getSembunyikanNilaiKemahasiswa());
			footer.appendChild(myCheckboxConfig);
			myCheckboxConfig.addEventListener("onClick", new EventListener() {

				/**
				 * Menyimpan seketika bendera {@link Skripsi#getSembunyikanNilaiKemahasiswa()}.
				 * Didahului {@code refresh(skripsi)} agar perubahan lain yang masuk sementara layar
				 * terbuka tidak tertimpa oleh salinan lama di memori, lalu {@code refreshUpdate} +
				 * {@code flush} sehingga efeknya langsung terasa pada sesi mahasiswa.
				 *
				 * <p>Checkbox-nya hanya dibuat untuk pengguna non-mahasiswa dan non-siswa, jadi
				 * gerbang hak akses ada di sisi pembuatan komponen. Bendera ini bersifat dua arah —
				 * bisa dinyalakan dan dimatikan kembali oleh siapa pun yang berhak — dan tidak
				 * meninggalkan jejak audit siapa yang mengubahnya.</p>
				 *
				 * @param arg0 event {@code onClick}; targetnya dipakai untuk membaca keadaan centang
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyCheckboxConfig c = (MyCheckboxConfig) arg0.getTarget();
					Session session = HibernateUtil.currentSession();
					session.refresh(skripsi);

					skripsi.setSembunyikanNilaiKemahasiswa(c.isChecked());
					Common.refreshUpdate(session, skripsi);
					session.flush();
				}
			});
		}
		footer = new Footer("Total");
		foot.appendChild(footer);

		if (skripsi.getSembunyikanNilaiKemahasiswa() && tbmuser != null && tbmuser.getMahasiswa() != null) {
			footer = new Footer("");
			foot.appendChild(footer);
			footer = new Footer("");
			foot.appendChild(footer);
		} else {
			foot.appendChild(footerRataRataNilai);
			foot.appendChild(footerNilaiHuruf);
		}
		footer = new Footer("");
		foot.appendChild(footer);

	}

	/**
	 * Membangun UI lengkap penilaian sidang skripsi di dalam {@code component}: panel administratif
	 * sidang (kolom kiri) dan dashboard+grid dosen penilai (kolom kanan). Lihat javadoc kelas untuk
	 * rincian lengkap fitur. Otomatis mencoba menautkan skripsi ke {@link Detailperkuliahan} KRS yang
	 * sesuai bila belum tertaut dan {@code FormatNilaiSkripsi} sudah diset.
	 *
	 * @param skripsi      data skripsi yang dinilai
	 * @param component    komponen induk ZK; isinya dibersihkan lebih dulu
	 * @param eventListener callback yang dipicu setiap kali nilai berubah (event data berisi
	 *                      {@code skripsi}), dipakai pemanggil untuk menyegarkan tampilan lain
	 */
	public void display(final Skripsi skripsi, final Component component, final EventListener eventListener) {
		this.skripsi = skripsi;
		this.eventListener = eventListener;
		Common.clear(component);

		Mahasiswa mahasiswa = skripsi.getMahasiswa();

		// component bisa berupa Center (LayoutRegion — HANYA boleh SATU anak). Bungkus semuanya dalam
		// SATU Div: Center kini punya 1 anak (wrapper), dan Style + Borderlayout dimuat di dalam Div
		// (Style TIDAK boleh jadi anak Borderlayout yang hanya menerima LayoutRegion). Ini mencegah
		// "Only one child is allowed: <Center>".
		ais.ui.util.MyDiv wrapperEl = new ais.ui.util.MyDiv();
		wrapperEl.setHeight("100%");
		wrapperEl.setParent(component);

		// UI/UX (07-12): lapisan gaya modern ber-SCOPE (class eL-penilaian) tanpa mengubah logika grading.
		org.zkoss.zul.Style eLStyle = new org.zkoss.zul.Style();
		eLStyle.setContent(".eL-penilaian .z-toolbar{background:linear-gradient(180deg,#f8fafc,#eef2f7);border:1px solid #e2e8f0;border-radius:10px;padding:6px 10px;box-shadow:0 1px 2px rgba(0,0,0,.05);margin-bottom:8px}.eL-penilaian .z-toolbarbutton{color:#1d4ed8;font-weight:600}.eL-penilaian .z-toolbarbutton:hover{color:#1e40af}.eL-penilaian .z-grid,.eL-penilaian .z-groupbox{border:1px solid #e2e8f0;border-radius:12px;box-shadow:0 4px 6px -1px rgba(0,0,0,.05);background:#fff}.eL-penilaian .z-label{color:#334155}.eL-penilaian .z-textbox,.eL-penilaian .z-combobox-inp,.eL-penilaian .z-decimalbox-inp,.eL-penilaian .z-datebox-inp,.eL-penilaian .z-intbox-inp{border-radius:8px}.eL-penilaian .z-row-cnt,.eL-penilaian .z-cell{padding:6px 8px}");
		eLStyle.setParent(wrapperEl);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(wrapperEl);
		// Borderlayout wajib punya tinggi pasti; tanpa ini ia kolaps (tinggi 0) di dalam tab/detail → data nilai tidak tampil.
		borderlayout.setHeight("100%");
		borderlayout.setStyle("min-height:480px;");
		borderlayout.setSclass("eL-penilaian");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		toolbar.setHeight("40px");

		// Layout utama: Center → Grid (2 kolom) → Rows → Row → [form kiri | dashboard kanan]
		Grid outerGrid = new Grid();
		outerGrid.setWidth("100%");
		outerGrid.setParent(center);

		Columns outerCols = new Columns();
		outerCols.setParent(outerGrid);
		MyColumnConfig outerColKiri = new MyColumnConfig();
		outerColKiri.setWidth("40%");
		outerColKiri.setParent(outerCols);
		new MyColumnConfig().setParent(outerCols);

		Rows outerRows = new Rows();
		outerRows.setParent(outerGrid);
		MyFormRow outerRow = new MyFormRow();
		outerRow.setValign("top");
		outerRow.setParent(outerRows);

		Grid grid1 = new Grid();
		grid1.setSclass("fgrid");
		grid1.setParent(outerRow);
		grid1.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(grid1);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid1);

		MyFormRow infoRow = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(infoRow, "2");
		infoRow.setParent(rows);
		infoRow.appendChild(new Html("<div style='font-family:Arial,sans-serif;background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;padding:10px;margin:4px;color:#1e3a8a;font-size:12px;line-height:1.45'>Atur jadwal, file, lokasi, dan catatan sidang di sini. Data yang benar membantu panitia dan mahasiswa melihat informasi sidang tanpa bertanya ulang.</div>"));

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telah Sidang ?"));

		final MyCheckboxConfig telahSidang = new MyCheckboxConfig();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			row.appendChild(telahSidang);
		} else {
			row.appendChild(new Label(
					(skripsi.getTelahSidang() == null ? false : skripsi.getTelahSidang().equals(1)) ? "Ya" : "Tidak"));
		}
		telahSidang.setChecked(skripsi.getTelahSidang() == null ? false : skripsi.getTelahSidang().equals(1));
		telahSidang.setDisabled(safeDouble(skripsi.getTotalNilai()) > 0.1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Sidang"));

		final MyDatebox tanggalSidang = new MyDatebox(skripsi.getTanggalSidang());
		tanggalSidang.setCols(6);

		final Timebox waktuSidang = new ais.ui.util.MyTimebox();
		final Timebox waktuSampaiSidang = new ais.ui.util.MyTimebox();

		Hbox hbox = new Hbox();

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			hbox.appendChild(waktuSidang);
			hbox.appendChild(new Label("-"));
			hbox.appendChild(waktuSampaiSidang);
		} else {
			hbox.appendChild(new Label(skripsi.getWaktuSidang()));
			hbox.appendChild(new Label("-"));
			hbox.appendChild(new Label(skripsi.getWaktuSampaiSidang()));
		}

		waktuSidang.setFormat(Common.timeFormat.get().toPattern());
		waktuSampaiSidang.setFormat(Common.timeFormat.get().toPattern());
		try {
			waktuSidang.setValue(Common.timeFormat.get().parse(skripsi.getWaktuSidang()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianSkripsiHelper.java:1062");

		}
		try {
			waktuSampaiSidang.setValue(Common.timeFormat.get().parse(skripsi.getWaktuSampaiSidang()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianSkripsiHelper.java:1067");

		}

		waktuSidang.setCols(5);
		waktuSampaiSidang.setCols(5);

		Vbox vbox = new Vbox();
		row.appendChild(vbox);
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			vbox.appendChild(new Label(
					skripsi.getTanggalSidang() == null ? "" : Common.dateFormat4.get().format(skripsi.getTanggalSidang())));
			vbox.appendChild(hbox);
		} else {
			vbox.appendChild(tanggalSidang);
			vbox.appendChild(hbox);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"File (PDF) " + (Common.bolehKonfigurasi("file_pdf_dan_cover_skripsi_wajib_diupload") ? "*" : "")));
		hbox = new Hbox();
		String label_skripsi = Common.getKonfigurasi("label_skripsi", "skripsi").getNilai();
		LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId(), LampiranLain.SKRIPSI,
				label_skripsi.replaceAll(";", " atau "), true, new EventListener() {

					/**
					 * Menautkan berkas PDF naskah skripsi yang baru diunggah ke id skripsi ini.
					 *
					 * <p>Berbeda dari callback lampiran catatan yang kosong, di sini {@code ref}
					 * memang perlu ditulis ulang, dan penulisannya memakai
					 * {@link StreamingHibernateUtil} — sesi/transaksi TERPISAH dari sesi ZK biasa —
					 * karena unggahan berkas besar berjalan di luar siklus request normal. Sesi itu
					 * ditutup di blok {@code finally}; kegagalan {@code commit} tidak ditangkap dan
					 * akan naik ke pemanggil.</p>
					 *
					 * @param arg0 event unggah, datanya berupa {@link LampiranLain} yang baru dibuat
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						try {
							session.refresh(lainMahasiswa);
							lainMahasiswa.setRef(skripsi.getId());

							session.getTransaction().begin();
							session.update(lainMahasiswa);
							session.getTransaction().commit();
						} finally {
							StreamingHibernateUtil.getInstance().closeSession();
						}
					}
				});
		hbox.setParent(row);

		final MyFormRow rowUploadFilePpt = new MyFormRow();
		rowUploadFilePpt.setStyle("border:0px;background: transparent;");
		rowUploadFilePpt.setParent(rows);
		rowUploadFilePpt.appendChild(new Label(ais.common.Common.getBahasaConfig("Presentasi Sidang")));

		hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setStyle("border:0px;background: transparent;");

		LampiranLain.createDownloadUploadFileLain(hbox, skripsi.getId() == null ? -Common.randLong() : skripsi.getId(),
				Skripsi.class.getName() + "_Presentasi", "Presentasi (PPT)", false, new EventListener() {

					/**
					 * Kembaran callback unggah naskah PDF, untuk berkas presentasi sidang (PPT):
					 * menautkan {@code ref} lampiran ke id skripsi lewat sesi
					 * {@link StreamingHibernateUtil} yang terpisah.
					 *
					 * <p>Perhatikan bahwa saat komponen unggah dibangun, skripsi yang belum ber-id
					 * diberi kunci sementara acak bernilai negatif ({@code -Common.randLong()});
					 * callback inilah yang kemudian memperbaiki {@code ref}-nya menjadi id skripsi
					 * yang sebenarnya.</p>
					 *
					 * @param arg0 event unggah, datanya berupa {@link LampiranLain} yang baru dibuat
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						try {
							session.refresh(lainMahasiswa);
							lainMahasiswa.setRef(skripsi.getId());

							session.getTransaction().begin();
							session.update(lainMahasiswa);
							session.getTransaction().commit();
						} finally {
							StreamingHibernateUtil.getInstance().closeSession();
						}
					}
				});
		hbox.setParent(rowUploadFilePpt);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Sidang"));
		final Textbox lokasiUjian = new Textbox(safeString(skripsi.getLokasiUjian()));
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			row.appendChild(lokasiUjian);
		} else {
			row.appendChild(new Label(skripsi.getLokasiUjian()));
		}
		lokasiUjian.setWidth("90%");
		lokasiUjian.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Penting"));
		final Textbox catatanSeminar = new Textbox(safeString(skripsi.getCatatanPenting()));
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			row.appendChild(new ais.ui.util.MyHtml(safeString(skripsi.getCatatanPenting()).replaceAll("\n", "<br>")));
		} else {
			row.appendChild(catatanSeminar);
		}
		catatanSeminar.setWidth("90%");
		catatanSeminar.setRows(15);

		row = new MyFormRow();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(""));
		final MyCheckboxConfig tanpaPerbaikan = new MyCheckboxConfig("Tanpa Perbaikan");
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			row.appendChild(new Label(skripsi.getTanpaPerbaikan() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(tanpaPerbaikan);
		}
		tanpaPerbaikan.setChecked(skripsi.getTanpaPerbaikan());

		// Satu listener dipakai bersama oleh SEMUA field administratif sidang di kolom kiri: setiap
		// perubahan satu field menulis ulang seluruh field sekaligus.
		EventListener ubah = new EventListener() {

			/**
			 * Menyalin isi seluruh kontrol administratif sidang (tanggal, waktu mulai/sampai, catatan
			 * penting, tanpa-perbaikan, telah-sidang, lokasi) ke {@link #skripsi} lalu menyimpannya
			 * seketika — sekali lagi pola auto-save tanpa tombol Simpan.
			 *
			 * <p><b>Cara kerja hak aksesnya perlu diperhatikan:</b> listener ini selalu didaftarkan
			 * ke ketujuh kontrol, bahkan ketika pengguna adalah mahasiswa. Yang membedakan adalah
			 * kontrol tersebut TIDAK dipasang ke halaman bagi mahasiswa (yang ditampilkan hanya
			 * label), sehingga komponen tetap lepas dan ZK tidak pernah mengirimkan event
			 * kepadanya. Jadi penjagaan bertumpu pada "komponen tidak terpasang", bukan pada
			 * pemeriksaan hak di dalam listener — perlu kehati-hatian bila kelak salah satu kontrol
			 * dipasang tanpa syarat.</p>
			 *
			 * <p>Syarat tampil kontrol juga tidak seragam: sebagian memakai "bukan mahasiswa DAN
			 * bukan siswa", sebagian lagi hanya "bukan mahasiswa" (catatan penting, tanpa
			 * perbaikan), sehingga pengguna yang tertaut data siswa dapat mengedit sebagian field
			 * tetapi tidak yang lain. Dicatat apa adanya.</p>
			 *
			 * @param arg0 event {@code onChange}/{@code onClick} dari salah satu kontrol; isinya
			 *             tidak dipakai karena seluruh field dibaca ulang
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				skripsi.setTanggalSidang(tanggalSidang.getValue());
				skripsi.setWaktuSampaiSidang(waktuSampaiSidang == null || waktuSampaiSidang.getValue() == null ? null
						: Common.timeFormat.get().format(waktuSampaiSidang.getValue()));
				skripsi.setWaktuSidang(waktuSidang == null || waktuSidang.getValue() == null ? null
						: Common.timeFormat.get().format(waktuSidang.getValue()));
				skripsi.setCatatanPenting(catatanSeminar.getValue().trim());
				skripsi.setTanpaPerbaikan(tanpaPerbaikan.isChecked());
				skripsi.setTelahSidang(telahSidang.isChecked() ? 1 : 0);
				skripsi.setLokasiUjian(lokasiUjian.getValue());
				Common.refreshUpdate(skripsi);
			}
		};

		catatanSeminar.addEventListener("onChange", ubah);
		tanggalSidang.addEventListener("onChange", ubah);
		waktuSampaiSidang.addEventListener("onChange", ubah);
		waktuSidang.addEventListener("onChange", ubah);
		lokasiUjian.addEventListener("onChange", ubah);
		tanpaPerbaikan.addEventListener("onClick", ubah);
		telahSidang.addEventListener("onClick", ubah);

//		if (!Common.isMobile()) {
//			grid1 = new Grid();
//			grid1.setSclass("fgrid");
//			grid1.setParent(rowUtama);
//			grid1.setWidth("100%");
//			grid1.setHeight("100%");
//
//			rows = new Rows();
//			rows.setParent(grid1);
//
//			row = new MyFormRow();
//			row.setStyle("border:0px;background: transparent;");
//			row.setParent(rows);
//
//			MyDiv groupbox = new ais.ui.util.MyDiv();
//			groupbox.setStyle("min-height: 200px;");
//			groupbox.setParent(row);
//		}

		Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai masuk ke : ")));
		// Guard: skripsi yang belum punya FormatNilaiSkripsi membuat getKodeItemBiaya() NPE.
		// Bila format nilai belum diset, lewati pencarian KRS otomatis (detailperkuliahan
		// tetap null → bisa dipilih manual oleh admin di bawah).
		if (detailperkuliahan == null && skripsi.getFormatNilaiSkripsi() != null) {
			detailperkuliahan = Common.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa, skripsi.getSemester(),
					skripsi.getFormatNilaiSkripsi().getKodeItemBiaya());
			if (detailperkuliahan != null) {
				skripsi.setDetailperkuliahan(detailperkuliahan);
				Common.refreshUpdate(skripsi);
			}
		}

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null) {
			final AmbilDataDetailPerkuliahanBanbox ambilDataMatakuliahBanbox = new AmbilDataDetailPerkuliahanBanbox(
					mahasiswa);
			ambilDataMatakuliahBanbox.setParent(toolbar);
			ambilDataMatakuliahBanbox.setWidth("100px");
			ambilDataMatakuliahBanbox.setValue(detailperkuliahan == null ? ""
					: detailperkuliahan.getPerkuliahan() != null
							? detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
							: detailperkuliahan.getMatakuliahKonversi() != null
									? detailperkuliahan.getMatakuliahKonversi().getNama()
									: "");
			ambilDataMatakuliahBanbox.setEventListener(new EventListener() {

				/**
				 * Menetapkan baris KRS ({@link Detailperkuliahan}) tujuan masuknya nilai skripsi,
				 * menyimpannya seketika, lalu membangun ulang seluruh layar agar bagian yang
				 * bergantung pada KRS (pilihan format nilai, tombol Format Nilai, sinkronisasi
				 * nilai) ikut menyesuaikan.
				 *
				 * <p>Pemilih ini hanya dibuat untuk staf/admin (bukan mahasiswa, bukan siswa, bukan
				 * dosen). Mengalihkan KRS di sini berarti mengalihkan tujuan nilai akhir skripsi ke
				 * mata kuliah lain; nilai yang sudah tersalin ke KRS lama TIDAK dibersihkan oleh
				 * aksi ini.</p>
				 *
				 * @param arg0 event pemilihan dari banbox; nilai terpilih dibaca dari atribut
				 *             {@code "detailperkuliahan"} milik banbox
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.setDetailperkuliahan(
							(Detailperkuliahan) ambilDataMatakuliahBanbox.getAttribute("detailperkuliahan"));
					Common.refreshUpdate(skripsi);

					Common.createDefaultTimer(new EventListener() {

						/**
						 * Membangun ulang layar pada siklus event berikutnya, supaya
						 * {@link #display(Skripsi, Component, EventListener)} tidak mengganti pohon
						 * komponen yang sedang menangani event ini.
						 *
						 * @param arg0 event timer; tidak dipakai
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							display(skripsi, component, eventListener);
						}
					});
				}
			});

			if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null
					&& tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null) {
				final Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
				if (perkuliahan != null && !perkuliahan.getSembunyikanFormatPenilaian()) {
					final MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Format Nilai",
							"/img/svg/edit-box-line.svg");
					buttonFormatNilai.setParent(toolbar);
					buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);
					if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
							.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
						buttonFormatNilai.setVisible(false);
					}
					buttonFormatNilai.addEventListener("onClick", new EventListener() {

						/** Helper pengelola format penilaian perkuliahan; dibuat sekali per tombol, dipakai ulang tiap klik. */
						FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

						/**
						 * Membuka window pengaturan format penilaian milik {@link Perkuliahan} tujuan
						 * nilai skripsi ini (bukan {@code FormatNilaiSkripsi} — ini format nilai mata
						 * kuliah tempat nilai skripsi bermuara). Setelah nilai kelas dihitung ulang
						 * dari sana, layar penilaian dibangun ulang lewat rantai callback
						 * {@link TampilDetailNilaiInterface}.
						 *
						 * <p>Tombolnya hanya dibuat untuk staf/admin, dan hanya bila perkuliahan
						 * belum dikunci serta kurikulumnya bukan OBE.</p>
						 *
						 * @param event event {@code onClick}; isinya tidak dipakai
						 */
						@Override
						public void onEvent(Event event) throws Exception {

							MyWindow addWindow = new MyWindow();
							addWindow.setHeight("95%");
							addWindow.setWidth("700px");
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

							formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

								/**
								 * Dipanggil balik oleh {@link FormatPenilaianHelper} setiap kali
								 * format penilaian perkuliahan berubah: memicu perhitungan ulang
								 * nilai seluruh peserta kelas lewat
								 * {@link Common#realoadNilai}, lalu membangun ulang layar penilaian
								 * skripsi ini agar nilai yang tampil ikut segar.
								 *
								 * <p>Cakupan efeknya jauh lebih luas dari satu skripsi: yang
								 * dihitung ulang adalah nilai SATU KELAS penuh. Nama method
								 * {@code realoadNilai} adalah salah ketik yang sudah menyebar ke
								 * antarmuka bersama, jadi tidak diubah di sini.</p>
								 *
								 * @param perkuliahan kelas yang format penilaiannya baru diubah
								 */
								@Override
								public void realoadNilai(final Perkuliahan perkuliahan) {

									Common.realoadNilai(perkuliahan,
											perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
											new EventListener() {

												/**
												 * Dipicu setelah perhitungan ulang nilai kelas
												 * selesai; hanya menjadwalkan pembangunan ulang
												 * layar.
												 *
												 * @param arg0 event penyelesaian; tidak dipakai
												 */
												@Override
												public void onEvent(Event arg0) throws Exception {
													Common.createDefaultTimer(new EventListener() {

														/**
														 * Membangun ulang layar penilaian pada
														 * siklus event berikutnya.
														 *
														 * @param arg0 event timer; tidak dipakai
														 */
														@Override
														public void onEvent(Event arg0) throws Exception {
															display(skripsi, component, eventListener);
														}
													});
												}
											}, null);

								}
							});
						}

					});
				}
			}

			if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null
					&& tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null) {
				List<FormatNilai> formatNilais = Common.getFormatNilais(HibernateUtil.currentSession(),
						detailperkuliahan.getPerkuliahan());

				final Combobox formatNilai = new Combobox();

				formatNilai.setWidth("92px");
				MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
				comboitemTidakAda.setValue(null);
				formatNilai.appendChild(comboitemTidakAda);
				for (FormatNilai nilai : formatNilais) {
					if (nilai.getStatusPertemuan() != null) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setValue(nilai);
						comboitem.setLabel(
								nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)");
						formatNilai.appendChild(comboitem);
					}
				}
				formatNilai.setParent(toolbar);
				if (skripsi.getFormatNilai() == null) {
					formatNilai.setSelectedItem(comboitemTidakAda);
				} else {
					Common.selectComboItem(formatNilai, skripsi.getFormatNilai());
				}
				formatNilai.setReadonly(true);
				formatNilai.setDisabled(detailperkuliahan.getPerkuliahan().getDikunci() != null);

				final Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();

				MyToolbarbuttonConfig buttonSingkronkan = new MyToolbarbuttonConfig("Singkronkan Nilai",
						"/img/Configure.gif");
				buttonSingkronkan.setParent(toolbar);
				buttonSingkronkan.addEventListener("onClick", new EventListener() {

					/**
					 * Mendorong nilai skripsi masuk ke komponen nilai kelas: memanggil
					 * {@link ais.common.GradingHelper#hitungNilaiBerdasarkanFormatNilaiSkripsi}
					 * untuk perkuliahan tujuan, memakai slot {@link FormatNilai} yang dipilih di
					 * combobox sebelahnya.
					 *
					 * <p>Aksinya berlaku untuk SELURUH peserta kelas tersebut, bukan hanya
					 * mahasiswa ini. Tidak ada konfirmasi sebelum dijalankan, dan hasilnya tidak
					 * dilaporkan kembali ke pengguna — layar tidak berubah setelah tombol
					 * ditekan.</p>
					 *
					 * @param arg0 event {@code onClick}; isinya tidak dipakai
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilaiSkripsi(perkuliahan, skripsi.getFormatNilai());
					}
				});

				formatNilai.addEventListener("onChange", new EventListener() {

					/**
					 * Menyimpan slot {@link FormatNilai} tujuan (komponen nilai mana pada mata
					 * kuliah yang akan diisi nilai skripsi), atau {@code null} bila dipilih "Tidak
					 * Ada".
					 *
					 * <p>Penyimpanan dibungkus {@code try/catch} karena format nilai yang dipilih
					 * bisa saja sudah dihapus admin lain sesaat sebelum combobox ini disimpan (race
					 * lintas sesi) — sebelumnya kondisi itu meledak sebagai
					 * {@code ConstraintViolationException} mentah. Sekarang transaksi di-rollback,
					 * kejadiannya dicatat ke audit, dan pengguna diberi pesan yang bisa dipahami.
					 * Kegagalan {@code rollback} sendiri ikut ditangkap agar tidak menutupi galat
					 * aslinya.</p>
					 *
					 * @param arg0 event {@code onChange} combobox; pilihan dibaca dari combobox
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

						final FormatNilai fn = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
								: formatNilai.getSelectedItem().getValue());

						Session session = HibernateUtil.currentSession();
						skripsi.setFormatNilai(fn);
						try {
							Common.refreshUpdate(session, (skripsi));
						} catch (Exception eSimpan) {
							// FIX akar masalah ConstraintViolationException (pola sama dgn
							// TugasMandiriHelper): format nilai yang dipilih bisa saja sudah
							// dihapus admin lain sesaat sebelum combobox ini disimpan (race
							// condition lintas sesi) -- sebelumnya meledak mentah tanpa pesan
							// yang bisa dipahami user. Tangkap, rollback, catat, beri tahu user.
							try {
								if (session.getTransaction() != null && session.getTransaction().isActive()) {
									session.getTransaction().rollback();
								}
							} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
									"auto-audit(rollback-gagal) src/ais/action/master/helper/PenilaianSkripsiHelper.java onFormatNilaiChange"); }
							ais.common.ErrorAuditUtil.record(eSimpan,
									"PenilaianSkripsiHelper: gagal simpan format nilai untuk Skripsi id="
											+ (skripsi == null ? "null" : skripsi.getId()));
							MyMessageboxConfig.show(
									"Mohon maaf, gagal menyimpan format nilai karena ada data terkait yang tidak konsisten. "
											+ "Silakan muat ulang (refresh) halaman ini dan coba lagi. Jika masih gagal, hubungi Administrator.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
					}

				});
			}

		} else {
			new Label(detailperkuliahan == null ? ""
					: detailperkuliahan.getPerkuliahan() != null
							? detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
							: detailperkuliahan.getMatakuliahKonversi() != null
									? detailperkuliahan.getMatakuliahKonversi().getNama()
									: "")
					.setParent(toolbar);
		}

		if (skripsi.getSembunyikanNilaiKemahasiswa() && tbmuser != null && tbmuser.getMahasiswa() != null) {

		} else {

			MyToolbarbuttonConfig buttonBlanko = new MyToolbarbuttonConfig("Blanko Penilaian",
					"/img/Text-Edit-icon.png");
			buttonBlanko.setParent(toolbar);
			buttonBlanko.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				private Map masukkanParameter(CommonVO commonVO, KomponenPenilaianSkripsi komponenPenilaianSkripsi,
						Boolean induk) {
					Dosen dosen = (Dosen) commonVO.getValueObject();
					Map parameter = new HashMap();
					try {
						LampiranLain lampiranLain = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
						if (lampiranLain != null) {
							parameter.put("ttd_dsn", lampiranLain.ambilFile().getAbsolutePath());
						} else {
							parameter.put("ttd_dsn", "");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:1412");
					}
					parameter.put("nidn_dosen", dosen.getNidn());
					parameter.put("nip_dosen", dosen.getCode());
					parameter.put("nama_dosen", dosen.getNama());
					parameter.put("nama_mahasiswa", skripsi.getMahasiswa().getNama());
					parameter.put("nim_mahasiswa", skripsi.getMahasiswa().getNim());
					parameter.put("induk", induk);
					parameter.put("jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas", skripsi.getMahasiswa().getJurusan().getFakultas().getNama());

					parameter.put("judul", skripsi.getJudul());
					parameter.put("abstrak", skripsi.getAbstrack());
					parameter.put("keyword", skripsi.getKeyword());
					parameter.put("jenis", commonVO.getName());
					parameter.put("jenjang", skripsi.getMahasiswa().getJenjang() == null ? ""
							: skripsi.getMahasiswa().getJenjang().getNama());
					Double nilai = skripsi.retreiveDetailNilai(komponenPenilaianSkripsi, dosen);
					parameter.put("nilai", nilai);
					parameter.put("bobot", komponenPenilaianSkripsi.getBobot());
					parameter.put("komponen", komponenPenilaianSkripsi.getNama());
					parameter.put("hasil_kali", nilai * komponenPenilaianSkripsi.getBobot());
					parameter.put("keterangan_komponen", komponenPenilaianSkripsi.getKeterangan());
					parameter.put("keterangan_nourut", komponenPenilaianSkripsi.getNomorUrut());
					parameter.put("jenis_semester",
							skripsi.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					parameter.put("tahun_ajaran", skripsi.getTahunAkademik());
					parameter.put("tanggal_sidang", skripsi.getTanggalSidang());
					parameter.put("waktu_sidang_mulai", skripsi.getWaktuSidang());
					parameter.put("waktu_sidang_sampai", skripsi.getWaktuSidang());
					return parameter;
				}

				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void onEvent(Event event) throws Exception {
					List<CommonVO> dataDosen = skripsi.dataDosen(false);
					List<Map> maps = new ArrayList();
					Map parameter = ais.common.HashMapGenerator.getRand();
					int indexTtd = 1;
					for (CommonVO commonVO : dataDosen) {

						TreeMap<KomponenPenilaianSkripsi, List<KomponenPenilaianSkripsi>> dataKomponenPenilaian = populateKomponen(
								commonVO.getName());
						for (KomponenPenilaianSkripsi parent : dataKomponenPenilaian.keySet()) {

							List<KomponenPenilaianSkripsi> datas = dataKomponenPenilaian.get(parent);
							if (datas.isEmpty()) {
								maps.add(masukkanParameter(commonVO, parent, true));
							} else {
								for (KomponenPenilaianSkripsi komponenPenilaianSkripsi : datas) {
									maps.add(masukkanParameter(commonVO, komponenPenilaianSkripsi, false));
								}
							}
						}

						try {
							Dosen dosen = (Dosen) commonVO.getValueObject();
							LampiranLain lampiranLain = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
							if (lampiranLain != null) {
								parameter.put("ttd_dsn_" + commonVO.getName(),
										lampiranLain.ambilFile().getAbsolutePath());
								parameter.put("ttd_dsn_" + indexTtd, lampiranLain.ambilFile().getAbsolutePath());
							} else {
								parameter.put("ttd_dsn_" + commonVO.getName(), "");
								parameter.put("ttd_dsn_" + indexTtd, "");
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:1481");
						}
						indexTtd++;
					}

					try {
						LampiranLain lampiranLain = LampiranLain.ambil(skripsi.getMahasiswa().getId(),
								LampiranLain.TTD_MAHASISWA);
						if (lampiranLain != null) {
							parameter.put("ttd_mhs", lampiranLain.ambilFile().getAbsolutePath());
						} else {
							parameter.put("ttd_mhs", "");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:1495");
					}

					if (skripsi.getMahasiswaRequestTugasAkhir() != null) {
						Common.insertProperty(MahasiswaRequestTugasAkhir.class, skripsi.getMahasiswaRequestTugasAkhir(),
								parameter, "bimbingan", 0);
					}
					Common.insertProperty(Skripsi.class, skripsi, parameter, "sidang", 0);
					parameter.put("nama_mahasiswa", skripsi.getMahasiswa().getNama());
					parameter.put("nim_mahasiswa", skripsi.getMahasiswa().getNim());
					parameter.put("jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas", skripsi.getMahasiswa().getJurusan().getFakultas().getNama());
					parameter.put("jenjang", skripsi.getMahasiswa().getJenjang() == null ? ""
							: skripsi.getMahasiswa().getJenjang().getNama());
					parameter.put("judul", skripsi.getJudul());
					parameter.put("abstrak", skripsi.getAbstrack());
					parameter.put("keyword", skripsi.getKeyword());
					parameter.put("jenis_semester",
							skripsi.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					parameter.put("tahun_ajaran", skripsi.getTahunAkademik());
					parameter.put("tanggal", Common.dateFormat2.get().format(
							skripsi.getTanggalSidang() == null ? WaktuUtil.getDate() : skripsi.getTanggalSidang()));
					parameter.put("tanggal_sidang", skripsi.getTanggalSidang());
					parameter.put("waktu_sidang_mulai", skripsi.getWaktuSidang());
					parameter.put("waktu_sidang_sampai", skripsi.getWaktuSidang());

					parameter.put("tanggal_sidang", skripsi.getTanggalSidang() == null ? ""
							: Common.dateFormat2.get().format(skripsi.getTanggalSidang()));
					parameter.put("hari_tanggal_sidang", skripsi.getTanggalSidang() == null ? ""
							: Common.dateFormat6.get().format(skripsi.getTanggalSidang()));

					parameter.put("maps", maps);

					Report.generatePDFReport(Report.PDF, parameter, "Blanko_Skripsi", ais.ui.util.WaktuUtil.getDate(),
							maps);
				}

			});

			buttonBlanko = new MyToolbarbuttonConfig("Berita Acara", "/img/Document-Text-icon.png");
			buttonBlanko.setParent(toolbar);
			buttonBlanko.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				private Map masukkanParameter(CommonVO commonVO) {
					Dosen dosen = (Dosen) commonVO.getValueObject();
					Map parameter = ais.common.HashMapGenerator.getRand();

					try {
						LampiranLain lampiranLain = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
						if (lampiranLain != null) {
							parameter.put("ttd_dsn", lampiranLain.ambilFile().getAbsolutePath());
						} else {
							parameter.put("ttd_dsn", "");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:1552");
					}

					parameter.put("nidn_dosen", dosen.getNidn());
					parameter.put("nip_dosen", dosen.getCode());
					parameter.put("nama_dosen", dosen.getNama());
					parameter.put("jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("jenjang", skripsi.getMahasiswa().getJenjang() == null ? ""
							: skripsi.getMahasiswa().getJenjang().getNama());
					parameter.put("nama_jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas", skripsi.getMahasiswa().getJurusan().getFakultas().getNama());
					parameter.put("kaprodi", skripsi.getMahasiswa().getJurusan().getKaprodi() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getKaprodi().getNama());
					parameter.put("nidn_kaprodi", skripsi.getMahasiswa().getJurusan().getKaprodi() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getKaprodi().getNidn());
					parameter.put("nip_kaprodi", skripsi.getMahasiswa().getJurusan().getKaprodi() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getKaprodi().getCode());

					parameter.put("dekan", skripsi.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getFakultas().getDekan().getNama());
					parameter.put("nidn_dekan",
							skripsi.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: skripsi.getMahasiswa().getJurusan().getFakultas().getDekan().getNidn());
					parameter.put("nip_dekan", skripsi.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getFakultas().getDekan().getCode());

					Double nilai = 0.0;
					Double persen = 0.0;
					if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
						nilai = skripsi.getNilaiKetuaSidang();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiKetuaSidang();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
						nilai = skripsi.getNilaiPembimbing();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
						nilai = skripsi.getNilaiPembimbing3();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing3();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
						nilai = skripsi.getNilaiPenguji1();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji1();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
						nilai = skripsi.getNilaiPenguji2();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji2();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
						nilai = skripsi.getNilaiPenguji3();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji3();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
						nilai = skripsi.getNilaiPenguji4();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji4();
					} else if (commonVO.getName().equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
						nilai = skripsi.getNilaiPenguji5();
						persen = skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji5();
					}
					parameter.put("jenis", commonVO.getName());
					parameter.put("jenis_dosen", commonVO.getName());
					parameter.put("nilai_dosen", nilai);
					parameter.put("persen_nilai_dosen", persen);
					parameter.put("tanggal_sidang", skripsi.getTanggalSidang());
					parameter.put("waktu_sidang_mulai", skripsi.getWaktuSidang());
					parameter.put("waktu_sidang_sampai", skripsi.getWaktuSidang());
					return parameter;
				}

				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void onEvent(Event event) throws Exception {
					List<CommonVO> dataDosen = skripsi.dataDosen(false);
					List<Map> maps = new ArrayList();
					Map parameter = ais.common.HashMapGenerator.getRand();
					int indexTtd = 1;
					for (CommonVO commonVO : dataDosen) {
						maps.add(masukkanParameter(commonVO));

						try {
							Dosen dosen = (Dosen) commonVO.getValueObject();
							LampiranLain lampiranLain = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
							if (lampiranLain != null) {
								parameter.put("ttd_dsn_" + commonVO.getName(),
										lampiranLain.ambilFile().getAbsolutePath());
								parameter.put("ttd_dsn_" + indexTtd, lampiranLain.ambilFile().getAbsolutePath());
							} else {
								parameter.put("ttd_dsn_" + commonVO.getName(), "");
								parameter.put("ttd_dsn_" + indexTtd, "");
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:1637");
						}
						indexTtd++;
					}

					try {
						LampiranLain lampiranLain = LampiranLain.ambil(skripsi.getMahasiswa().getId(),
								LampiranLain.TTD_MAHASISWA);
						if (lampiranLain != null) {
							parameter.put("ttd_mhs", lampiranLain.ambilFile().getAbsolutePath());
						} else {
							parameter.put("ttd_mhs", "");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianSkripsiHelper.java:1651");
					}

					if (skripsi.getMahasiswaRequestTugasAkhir() != null) {
						Common.insertProperty(MahasiswaRequestTugasAkhir.class, skripsi.getMahasiswaRequestTugasAkhir(),
								parameter, "bimbingan");
					}
					Common.insertProperty(Skripsi.class, skripsi, parameter, "sidang");
					parameter.put("nama_mahasiswa", skripsi.getMahasiswa().getNama());
					parameter.put("nim_mahasiswa", skripsi.getMahasiswa().getNim());
					parameter.put("angkatan_mahasiswa", skripsi.getMahasiswa().getTahunangkatan());
					parameter.put("jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", skripsi.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas", skripsi.getMahasiswa().getJurusan().getFakultas().getNama());
					parameter.put("kaprodi", skripsi.getMahasiswa().getJurusan().getKaprodi() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getKaprodi().getNama());
					parameter.put("nidn_kaprodi", skripsi.getMahasiswa().getJurusan().getKaprodi() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getKaprodi().getNidn());
					parameter.put("nip_kaprodi", skripsi.getMahasiswa().getJurusan().getKaprodi() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getKaprodi().getCode());

					parameter.put("dekan", skripsi.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getFakultas().getDekan().getNama());
					parameter.put("nidn_dekan",
							skripsi.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: skripsi.getMahasiswa().getJurusan().getFakultas().getDekan().getNidn());
					parameter.put("nip_dekan", skripsi.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
							: skripsi.getMahasiswa().getJurusan().getFakultas().getDekan().getCode());

					parameter.put("judul", skripsi.getJudul());
					parameter.put("absntrak", skripsi.getAbstrack());
					parameter.put("judul_en", skripsi.getJudulen());
					parameter.put("keyword", skripsi.getKeyword());

					parameter.put("jenis_semester",
							skripsi.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					parameter.put("tahun_ajaran", skripsi.getTahunAkademik());
					parameter.put("tanggal", Common.dateFormat2.get().format(
							skripsi.getTanggalSeminar() == null ? WaktuUtil.getDate() : skripsi.getTanggalSeminar()));
					parameter.put("hari_tanggal", Common.dateFormat6.get().format(
							skripsi.getTanggalSeminar() == null ? WaktuUtil.getDate() : skripsi.getTanggalSeminar()));
					parameter.put("waktu_mulai_sidang", skripsi.getWaktuSidang());
					parameter.put("waktu_sampai_sidang", skripsi.getWaktuSampaiSidang());
					parameter.put("lulus", skripsi.getLulus());
					parameter.put("nilai_total", skripsi.getTotalNilai());
					parameter.put("nilai_huruf", skripsi.getNilaiHuruf());
					parameter.put("catatan_penting", skripsi.getCatatanPenting());
					parameter.put("tanggal_sidang", skripsi.getTanggalSidang());
					parameter.put("waktu_sidang_mulai", skripsi.getWaktuSidang());
					parameter.put("waktu_sidang_sampai", skripsi.getWaktuSidang());

					parameter.put("tanggal_sidang", skripsi.getTanggalSidang() == null ? ""
							: Common.dateFormat2.get().format(skripsi.getTanggalSidang()));
					parameter.put("hari_tanggal_sidang", skripsi.getTanggalSidang() == null ? ""
							: Common.dateFormat6.get().format(skripsi.getTanggalSidang()));

					parameter.put("maps", maps);

					Report.generatePDFReport(Report.PDF, parameter, "Berita_Acara_Sidang_Skripsi",
							ais.ui.util.WaktuUtil.getDate(), maps);
				}

			});

			MyToolbarbuttonConfig buttonHitungSemua = new MyToolbarbuttonConfig("Hitung Ulang Semua Nilai", "/img/Configure.gif");
			buttonHitungSemua.setTooltiptext("Hitung ulang nilai dari semua dosen penilai dan perbarui nilai akhir mahasiswa");
			buttonHitungSemua.setParent(toolbar);
			buttonHitungSemua.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Hitung ulang semua nilai dosen sekarang?", "Konfirmasi",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										int jumlah = hitungUlangSemuaNilaiDosen(true);
										if (eventListener != null) {
											eventListener.onEvent(new Event("", null, skripsi));
										}
										tampilkanPesanHitungUlang(jumlah);
										display(skripsi, component, eventListener);
									}
								}
							});
				}
			});
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				display(skripsi, component, eventListener);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Reset", "/img/Button-Refresh-icon.png");
		button.setVisible(Common.getApakahAdmin());
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin me-reset semua nilai dosen ?", "Question",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										skripsi.setDetailNilai(null);

										skripsi.setNilaiKetuaSidang(0.0);
										skripsi.setNilaiHuruf("");
										skripsi.setTotalIP(null);
										skripsi.setNilaikomprehensif(0.0);
										skripsi.setNilaiPembimbing(0.0);
										skripsi.setNilaiPembimbing3(0.0);
										skripsi.setNilaiPenguji1(0.0);
										skripsi.setNilaiPenguji2(0.0);
										skripsi.setNilaiPenguji3(0.0);
										skripsi.setNilaiPenguji4(0.0);
										skripsi.setNilaiPenguji5(0.0);
										skripsi.setTotalNilai(null);

										Common.refreshUpdate(skripsi);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												display(skripsi, component, eventListener);
											}
										});

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"me-reset semua nilai dosen untuk data skripsi ini",
												e,
												new String[] {
														"Periksa apakah data nilai skripsi ini masih berelasi dengan data lain (misalnya data transkrip atau kelulusan) sehingga tidak dapat direset.",
														"Muat ulang halaman kemudian ulangi proses reset nilai.",
														"Jika proses reset tetap gagal, konfirmasikan kebutuhan ini kepada Administrator." });
									}

								}

							}
						});

			}
		});

		button.setParent(toolbar);

//		groupbox2 = new ais.ui.util.MyGroupboxStyled();
//		groupbox2.setParent(center);
//		groupbox2.appendChild(new MyCaptionStyled("Nilai Sidang"));
//		groupbox2.setHeight(label_skripsi)

		// Kolom kanan: dashboard + daftar dosen penilai, masuk sebagai sel kanan outerRow.
		// Scroll ditangani oleh Center (autoscroll=true) — tidak perlu overflow per-div.
		ais.ui.util.MyDiv centerBox = new ais.ui.util.MyDiv();
		centerBox.setWidth("100%");
		centerBox.setParent(outerRow);
		buatDashboardNilai().setParent(centerBox);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(centerBox);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prosentase");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Huruf");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("8%");

		loadData(null);

	}

}
