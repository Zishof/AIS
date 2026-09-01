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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.KomponenPenilaianProposalSkripsi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
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
 * Helper ZK untuk layar penilaian seminar/sidang proposal skripsi (Tugas Akhir), dipakai dari
 * modul Tugas Akhir mahasiswa lewat {@link #display(MahasiswaRequestTugasAkhir, Component)}.
 * Entity utamanya adalah {@link MahasiswaRequestTugasAkhir} (satu baris = satu pengajuan Tugas
 * Akhir seorang mahasiswa, menyimpan judul, jadwal seminar, nilai dosen 1-6, total nilai, nilai
 * huruf, dan status lulus). Bobot dan susunan komponen penilaian diambil dari
 * {@link ais.database.model.FormatNilaiProposalSkripsi} (kolom {@code dosen1}..{@code dosen6}
 * menampung nama peran: dosen1-3 = pembimbing 1-3, dosen4-6 = penguji 1-3, masing-masing dengan
 * persentase bobotnya sendiri), sedangkan daftar sub-komponen nilai (mis. "Penguasaan Materi",
 * "Sistematika Penulisan") datang dari {@link KomponenPenilaianProposalSkripsi} yang terhubung ke
 * format nilai lewat {@link ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi} (query di
 * {@link #populateKomponen(String)}, difilter per jurusan/fakultas dan per kolom peran dosen).
 *
 * <p><b>Alur UI:</b> {@link #display} membangun satu window ZK dengan {@code Borderlayout}: NORTH
 * berisi toolbar aksi (pilih matakuliah tujuan nilai, ganti format nilai, singkronkan nilai,
 * cetak Blanko Penilaian/Berita Acara PDF, refresh, reset nilai untuk admin), WEST berisi form
 * "Hasil Seminar" (jadwal, tanggal/waktu, upload berkas proposal &amp; presentasi, tanggal
 * bimbingan, catatan, checkbox "Tanpa Perbaikan"), dan CENTER berisi dasbor ringkasan HTML/CSS
 * ({@link #buatDashboardNilai()}) diikuti grid "Nilai Seminar" per dosen (baris dirender oleh
 * inner class {@link DetailKelompokKknRenderer}). Mengklik tombol "Penilaian" pada satu baris
 * dosen membuka dialog modal lewat {@link #init(Dosen, String)} untuk mengisi nilai per komponen
 * dan catatan dosen tersebut.</p>
 *
 * <p><b>Kuirk penamaan:</b> inner class {@link DetailKelompokKknRenderer} jelas hasil salin-tempel
 * dari helper modul KKN (Kuliah Kerja Nyata) lain dan TIDAK pernah diubah namanya walau isinya
 * murni merender baris dosen penilai proposal skripsi — nama ini menyesatkan tapi dibiarkan apa
 * adanya karena mengubahnya bukan bagian dari perbaikan Javadoc.</p>
 *
 * <p><b>Efek samping:</b> method render/tampilan (mis. {@link #loadData(Object)},
 * {@link #buatDashboardNilai()}) bersifat baca saja terhadap DB, hanya memanipulasi komponen ZK.
 * Method yang mengubah nilai (event listener di {@link #init(Dosen, String)}, checkbox
 * "Sembunyikan nilai ke mahasiswa" di {@link #loadData(Object)}, tombol Reset di
 * {@link #display(MahasiswaRequestTugasAkhir, Component)}) langsung memanggil
 * {@code Common.refreshUpdate(...)} atau transaksi Hibernate manual sehingga tersimpan seketika
 * tanpa tombol "Simpan" terpisah — pola auto-save per perubahan field, khas modul penilaian AIS
 * lain (bandingkan {@code PenilaianSkripsiHelper}).</p>
 */
public class PenilaianProposalSkripsiHelper implements DataLoader {

	/** Grid ZK "Nilai Seminar" (satu baris per dosen penilai), dipasang ulang tiap {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Data Tugas Akhir/proposal skripsi yang sedang ditampilkan/dinilai; diisi oleh {@link #display(MahasiswaRequestTugasAkhir, Component)}. */
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	/** Footer grid yang menampilkan rata-rata/total nilai akhir; dibangun ulang di {@link #loadData(Object)}. */
	private Footer footerRataRataNilai;
	/** Footer grid yang menampilkan nilai huruf akhir; dibangun ulang di {@link #loadData(Object)}. */
	private Footer footerNilaiHuruf;
	/** Pengguna yang sedang login (dosen/mahasiswa/admin), dipakai untuk aturan tampil/sembunyi nilai dan hak edit. */
	private Tbmuser tbmuser = null;

	/**
	 * Menyiapkan helper dengan mengambil pengguna yang sedang login ({@link Common#getCurrentUser()})
	 * ke {@link #tbmuser}; identitas ini menentukan apakah baris grid disorot (dosen yang login
	 * adalah dosen pembimbing/penguji bersangkutan), apakah nilai disembunyikan (role mahasiswa),
	 * dan apakah kolom nilai dapat diedit langsung. Instance baru dibuat setiap kali layar penilaian
	 * dibuka, bukan singleton.
	 */
	public PenilaianProposalSkripsiHelper() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Row renderer grid "Nilai Seminar": satu baris = satu dosen penilai ({@link CommonVO} yang
	 * membungkus {@link Dosen} sebagai value object dan "jenis" peran — nama field dosen1..dosen6
	 * pada {@link ais.database.model.FormatNilaiProposalSkripsi} — sebagai {@code commonVO.getName()}).
	 * Nama kelas ini adalah kuirk peninggalan salin-tempel dari helper modul KKN lain; isinya
	 * murni tentang penilaian proposal skripsi, tidak ada hubungan dengan KKN.
	 *
	 * <p><b>Scope:</b> instance-nya inner class non-static, jadi terikat pada instance
	 * {@link PenilaianProposalSkripsiHelper} yang membuatnya dan memakai field {@code
	 * mahasiswaRequestTugasAkhir}/{@code tbmuser} milik kelas induk secara langsung.</p>
	 *
	 * @see PenilaianProposalSkripsiHelper
	 */
	class DetailKelompokKknRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris dosen penilai: foto, nama dosen, label peran ({@code jenis}),
		 * persentase bobot perannya, nilai numerik, dan nilai huruf hasil konversi lewat
		 * {@link Common#getNilaiHuruf}. Baris disorot hijau muda bila dosen yang login adalah
		 * dosen pada baris tersebut. Menampilkan juga catatan dosen (disimpan sebagai JSON per
		 * "jenis" di {@link MahasiswaRequestTugasAkhir#getCatatanDosen()}) dan area
		 * unggah/unduh lampiran "Catatan" via {@link LampiranLain#createDownloadUploadFileLain}.
		 * Bila {@link MahasiswaRequestTugasAkhir#getSembunyikanNilaiKemahasiswa()} aktif dan
		 * pengguna login adalah mahasiswa, kolom persentase/nilai/nilai-huruf ditampilkan sebagai
		 * "-" tanpa mengungkap angka. Tombol "Penilaian" pada baris membuka dialog input komponen
		 * nilai lewat {@link PenilaianProposalSkripsiHelper#init(Dosen, String)}. Read-only
		 * terhadap DB (tidak menulis apa pun sendiri), efek sampingnya murni membangun komponen ZK.
		 *
		 * @param row  baris grid yang akan diisi komponen anaknya.
		 * @param data item data grid, harus berupa {@link CommonVO} berisi {@link Dosen} sebagai
		 *             value object dan nama peran dosen sebagai {@code name}.
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final CommonVO commonVO = (CommonVO) data;

			final Dosen dosen = (Dosen) commonVO.getValueObject();

			if (dosen != null && tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
					&& tbmuser.getDosen().getId().equals(dosen.getId())) {
				row.setStyle("background:#eeffeb;");
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			JSONObject catat = new JSONObject(mahasiswaRequestTugasAkhir.getCatatanDosen());

			Vbox vbox = new Vbox();

			vbox.appendChild(
					new ais.ui.util.MyHtml((catat.isNull(commonVO.getName()) ? "" : catat.getString(commonVO.getName()))
							.replaceAll("\n", "<br>")));
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
					MahasiswaRequestTugasAkhir.class.getName() + "_" + commonVO.getName(), "Catatan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false);

			hbox.setParent(vbox);

			CommonMedia.tampilkanGambarKecil(dosen).setParent(row);
			new Label(dosen.getNama()).setParent(row);
			new Label(commonVO.getName()).setParent(row);

			Double nilai = 0.0;
			Double persen = 0.0;
			if (commonVO.getName().equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1())) {
				nilai = mahasiswaRequestTugasAkhir.getNilaiDosen1();
				persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getProsentasiNilaiPembimbing1();
			} else if (commonVO.getName()
					.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2())) {
				nilai = mahasiswaRequestTugasAkhir.getNilaiDosen2();
				persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getProsentasiNilaiPembimbing2();
			} else if (commonVO.getName()
					.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3())) {
				nilai = mahasiswaRequestTugasAkhir.getNilaiDosen3();
				persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getProsentasiNilaiPembimbing3();
			} else if (commonVO.getName()
					.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4())) {
				nilai = mahasiswaRequestTugasAkhir.getNilaiDosen4();
				persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getProsentasiNilaiPenguji1();
			} else if (commonVO.getName()
					.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5())) {
				nilai = mahasiswaRequestTugasAkhir.getNilaiDosen5();
				persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getProsentasiNilaiPenguji2();
			} else if (commonVO.getName()
					.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6())) {
				nilai = mahasiswaRequestTugasAkhir.getNilaiDosen6();
				persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getProsentasiNilaiPenguji3();
			}

			new Label(Common.numberFormat.get().format(persen)).setParent(row);

			if (mahasiswaRequestTugasAkhir.getSembunyikanNilaiKemahasiswa() && tbmuser != null
					&& tbmuser.getMahasiswa() != null) {
				new Label("-").setParent(row);
				new Label("-").setParent(row);
				new Label("-").setParent(row);
			} else {

				new Label(Common.numberFormat.get().format(nilai)).setParent(row);
				NilaiHuruf nilaiHuruf = null;
				try {

					Detailperkuliahan detailperkuliahan = mahasiswaRequestTugasAkhir.getDetailperkuliahan();
					Matakuliah matakuliah = detailperkuliahan == null ? null
							: detailperkuliahan.getPerkuliahan() != null
									? detailperkuliahan.getPerkuliahan().getMatakuliah()
									: detailperkuliahan.getMatakuliahKonversi();

					nilaiHuruf = Common
							.getNilaiHuruf(nilai, mahasiswaRequestTugasAkhir.getMahasiswa().getTahunangkatan(),
									mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan(),
									mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas(),
									mahasiswaRequestTugasAkhir.getTahunAkademik(),
									mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP
											: Perkuliahan.GANJIL,
									matakuliah == null ? "" : matakuliah.getKode(),
									matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:188");
				}
				new Label(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf()).setParent(row);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Penilaian", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
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
	 * Membuka dialog modal ({@link MyWindow}) untuk mengisi/melihat nilai satu dosen penilai per
	 * komponen penilaian ({@link KomponenPenilaianProposalSkripsi}, diambil lewat
	 * {@link #populateKomponen(String)}) beserta catatan dosen untuk mahasiswa yang sedang
	 * ditampilkan ({@code mahasiswaRequestTugasAkhir}). Untuk tiap komponen (atau sub-komponen bila
	 * komponen induk punya anak) ditampilkan nama, keterangan, bobot, dan kotak nilai
	 * ({@link MyDoublebox}) — kotak nilai hanya EDITABLE bila pengguna login adalah dosen
	 * bersangkutan itu sendiri (bukan mahasiswa, bukan dosen lain); selain itu nilai tampil sebagai
	 * label read-only. Setiap perubahan nilai langsung memanggil
	 * {@link MahasiswaRequestTugasAkhir#populateDetailNilai(KomponenPenilaianProposalSkripsi, Dosen, Double, boolean)}
	 * lalu menghitung ulang total lewat listener {@code hitungUlang} (yang juga memperbarui
	 * {@link Detailperkuliahan} terkait bila ada, dan menyimpan lewat {@code Common.refreshUpdate}).
	 * Catatan dosen disimpan sebagai key-value JSON per "jenis" peran dan diunggah lampiran lewat
	 * {@link LampiranLain#createDownloadUploadFileLain}. Tombol "Hitung Ulang" pada toolbar dialog
	 * memaksa penghitungan ulang total dari seluruh baris nilai yang sedang tampil; tombol "Selesai"
	 * menutup window lalu memuat ulang grid utama ({@link #loadData(Object)}) via timer.
	 *
	 * @param dosen dosen penilai yang dialognya dibuka (pemilik nilai/catatan yang ditampilkan).
	 * @param jenis nama peran dosen ini pada format nilai (nilai dari
	 *              {@code dosen1}..{@code dosen6} milik
	 *              {@link MahasiswaRequestTugasAkhir#getFormatNilaiProposalSkripsi()}), dipakai
	 *              sebagai kunci pencarian nilai/komponen dan kunci JSON catatan dosen.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final Dosen dosen, final String jenis) throws Exception {
		final MyWindow addWindow = new MyWindow();
		addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Dosen currDosen = tbmuser.ambilDosen();

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

		JSONObject catat = new JSONObject(mahasiswaRequestTugasAkhir.getCatatanDosen());
		final Textbox catatanDosen = new Textbox(catat.isNull(jenis) ? "" : catat.getString(jenis));
		catatanDosen.setWidth("90%");
		catatanDosen.setRows(5);

		Double nilaiPembimbing = mahasiswaRequestTugasAkhir.cariNilaiDariDosen(dosen, jenis, false);

		Detailperkuliahan detailperkuliahan = mahasiswaRequestTugasAkhir.getDetailperkuliahan();
		Matakuliah matakuliah = detailperkuliahan == null ? null
				: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();
		NilaiHuruf nilaiHurufpembimbing = Common.getNilaiHuruf(nilaiPembimbing,
				mahasiswaRequestTugasAkhir.getMahasiswa().getTahunangkatan(),
				mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan(),
				mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas(),
				mahasiswaRequestTugasAkhir.getTahunAkademik(),
				mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		final Footer footerTotal = new Footer(Common.numberFormat.get().format(nilaiPembimbing) + " ("
				+ (nilaiHurufpembimbing == null ? "" : nilaiHurufpembimbing.getNilaiHuruf()) + ")");

		final EventListener hitungUlang = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Detailperkuliahan detailperkuliahan = mahasiswaRequestTugasAkhir.getDetailperkuliahan();
				Matakuliah matakuliah = detailperkuliahan == null ? null
						: detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();
				boolean refresh = arg0 != null && arg0.getName() != null
						&& arg0.getName().equalsIgnoreCase("Hitung Ulang");
				Double nilaiPembimbing = mahasiswaRequestTugasAkhir.cariNilaiDariDosen(dosen, jenis, refresh);
				Double total = mahasiswaRequestTugasAkhir.getTotalNilai();
				NilaiHuruf nilaiHuruf = Common
						.getNilaiHuruf(total, mahasiswaRequestTugasAkhir.getMahasiswa().getTahunangkatan(),
								mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan(),
								mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas(),
								mahasiswaRequestTugasAkhir.getTahunAkademik(),
								mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP
										: Perkuliahan.GANJIL,
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

				if (nilaiHuruf != null) {
					mahasiswaRequestTugasAkhir.setTotalIP(nilaiHuruf.getNilaiDiIPK());
					mahasiswaRequestTugasAkhir.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
					mahasiswaRequestTugasAkhir.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
				}
				JSONObject catat = new JSONObject(mahasiswaRequestTugasAkhir.getCatatanDosen());
				catat.put(jenis, catatanDosen.getValue());
				mahasiswaRequestTugasAkhir.setCatatanDosen(catat.toString());
				Common.refreshUpdate(mahasiswaRequestTugasAkhir);

				if (mahasiswaRequestTugasAkhir.getDetailperkuliahan() != null) {
					detailperkuliahan.setTotalNilai(mahasiswaRequestTugasAkhir.getTotalNilai());
					detailperkuliahan.setTotalIP(mahasiswaRequestTugasAkhir.getTotalIP());
					detailperkuliahan.setNilaiHuruf(mahasiswaRequestTugasAkhir.getNilaiHuruf());
					detailperkuliahan.setLulus(mahasiswaRequestTugasAkhir.getLulus());

					Double totalSementara = mahasiswaRequestTugasAkhir.getTotalNilai();
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

				NilaiHuruf nilaiHurufpembimbing = Common
						.getNilaiHuruf(nilaiPembimbing, mahasiswaRequestTugasAkhir.getMahasiswa().getTahunangkatan(),
								mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan(),
								mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas(),
								mahasiswaRequestTugasAkhir.getTahunAkademik(),
								mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP
										: Perkuliahan.GANJIL,
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

				footerTotal.setLabel(Common.numberFormat.get().format(nilaiPembimbing) + " ("
						+ (nilaiHurufpembimbing == null ? "" : nilaiHurufpembimbing.getNilaiHuruf()) + ")");
			}

		};
		TreeMap<Long, List<KomponenPenilaianProposalSkripsi>> dataKomponenPenilaian = populateKomponen(jenis);
		for (Long parentId : dataKomponenPenilaian.keySet()) {

			final KomponenPenilaianProposalSkripsi parent = (KomponenPenilaianProposalSkripsi) ConstantValues
					.ambil(KomponenPenilaianProposalSkripsi.class.getName(), parentId);

			final List<KomponenPenilaianProposalSkripsi> datas = dataKomponenPenilaian.get(parentId);
			if (datas.isEmpty()) {

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(subRows);
				row.appendChild(new Label(parent.getNama()));
				row.appendChild(new MyLabelAgakKecil(parent.getKeterangan()));
				row.appendChild(new Label(Common.numberFormat.get().format(parent.getBobot())));

				if (tbmuser.getMahasiswa() != null || (currDosen != null && !currDosen.getId().equals(dosen.getId()))) {
					row.appendChild(new Label(
							Common.numberFormat.get().format(mahasiswaRequestTugasAkhir.retreiveDetailNilai(parent, dosen))));
				} else {
					final MyDoublebox nilai = new MyDoublebox(
							mahasiswaRequestTugasAkhir.retreiveDetailNilai(parent, dosen));
					nilai.setWidth("90%");
					row.appendChild(nilai);
					row.setValign("top");
					row.setAttribute("nilai", nilai);
					row.setValign("top");
					row.setAttribute("komponen", parent);
					row.setValign("top");
					row.setAttribute("dosen", dosen);
					nilai.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							HibernateUtil.currentSession().refresh(mahasiswaRequestTugasAkhir);
							mahasiswaRequestTugasAkhir.populateDetailNilai(parent, dosen, nilai.getValue(), true);
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

				for (final KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : datas) {

					row = new MyFormRow();
					row.setParent(subRows);
					Hbox hbox = new Hbox();
					row.appendChild(hbox);
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					hbox.appendChild(new Label(komponenPenilaianProposalSkripsi.getNama()));
					row.appendChild(new MyLabelAgakKecil(komponenPenilaianProposalSkripsi.getKeterangan()));
					row.appendChild(new Label(Common.numberFormat.get().format(komponenPenilaianProposalSkripsi.getBobot())));

					if (tbmuser.getMahasiswa() != null
							|| (currDosen != null && !currDosen.getId().equals(dosen.getId()))) {
						row.appendChild(new Label(Common.numberFormat.get().format(mahasiswaRequestTugasAkhir
								.retreiveDetailNilai(komponenPenilaianProposalSkripsi, dosen))));
					} else {

						final MyDoublebox nilai = new MyDoublebox(mahasiswaRequestTugasAkhir
								.retreiveDetailNilai(komponenPenilaianProposalSkripsi, dosen));
						nilai.setWidth("90%");
						row.appendChild(nilai);
						row.setValign("top");
						row.setAttribute("nilai", nilai);
						row.setValign("top");
						row.setAttribute("komponen", parent);
						row.setValign("top");
						row.setAttribute("dosen", dosen);
						nilai.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								HibernateUtil.currentSession().refresh(mahasiswaRequestTugasAkhir);
								mahasiswaRequestTugasAkhir.populateDetailNilai(komponenPenilaianProposalSkripsi, dosen,
										nilai.getValue(), true);
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
		if (tbmuser.getMahasiswa() != null || (currDosen != null && !currDosen.getId().equals(dosen.getId()))) {
			row.appendChild(new ais.ui.util.MyHtml(catatanDosen.getValue().replaceAll("\n", "<br>")));
		} else {
			row.appendChild(catatanDosen);
		}
		catatanDosen.addEventListener("onChange", hitungUlang);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "5");
		row.setParent(subRows);
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName() + "_" + jenis, "Catatan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false,
				!(tbmuser.getMahasiswa() != null || (currDosen != null && !currDosen.getId().equals(dosen.getId()))));
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
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
				Common.createDefaultTimer(new EventListener() {

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
			@Override
			public void onEvent(Event event) throws Exception {
				List<Row> rows = subRows.getChildren();
				for (Row row : rows) {
					MyDoublebox nilai = (MyDoublebox) row.getAttribute("nilai");
					if (nilai != null) {
						mahasiswaRequestTugasAkhir.populateDetailNilai(
								(KomponenPenilaianProposalSkripsi) row.getAttribute("komponen"), dosen,
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
	 * Mengambil daftar komponen penilaian proposal skripsi yang berlaku untuk satu peran dosen
	 * ({@code jenis}), lalu mengelompokkannya per komponen induk (parent-child, untuk komponen yang
	 * punya sub-komponen). Query Hibernate Criteria pada
	 * {@link ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi} memfilter:
	 * {@link KomponenPenilaianProposalSkripsi} yang jurusan/fakultasnya kosong (berlaku umum) ATAU
	 * cocok dengan jurusan/fakultas mahasiswa; kolom peran dosen ({@code dosen1}..{@code dosen6},
	 * ditentukan dari {@code jenis} lewat pencocokan dengan
	 * {@code getFormatNilaiProposalSkripsi().getDosenN()}) kosong ATAU bernilai {@code true};
	 * {@code aktif} kosong ATAU {@code true}; dan terikat ke
	 * {@link MahasiswaRequestTugasAkhir#getFormatNilaiProposalSkripsi()} yang sama.
	 *
	 * @param jenis nama peran dosen (nilai {@code dosen1}..{@code dosen6} pada format nilai) yang
	 *              menentukan kolom boolean mana pada {@link KomponenPenilaianProposalSkripsi} yang
	 *              dipakai sebagai filter keterlibatan komponen untuk peran tersebut.
	 * @return peta terurut (berdasarkan id komponen induk) dari id komponen induk ke daftar
	 *         sub-komponennya; komponen tanpa induk dan tanpa anak muncul sebagai key dengan list
	 *         kosong (ditangani sebagai "komponen tunggal" oleh {@link #init(Dosen, String)}).
	 */
	@SuppressWarnings("unchecked")
	private TreeMap<Long, List<KomponenPenilaianProposalSkripsi>> populateKomponen(String jenis) {
		String kolom = "dosen1";
		if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1())) {
			kolom = "dosen1";
		} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2())) {
			kolom = "dosen2";
		} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3())) {
			kolom = "dosen3";
		} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4())) {
			kolom = "dosen4";
		} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5())) {
			kolom = "dosen5";
		} else if (jenis.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6())) {
			kolom = "dosen6";
		}
		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianProposalSkripsi> formatNilaiProposalSkripsiPunyaKomponenPenilaianProposalSkripsis = ConstantValues
				.simpleList(
						session.createCriteria(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi.class)
								.setProjection(Projections.groupProperty("komponenPenilaianProposalSkripsi.id"))
								.createAlias("komponenPenilaianProposalSkripsi", "komponenPenilaianProposalSkripsi")

								.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.jurusan"),
										Restrictions.eq("komponenPenilaianProposalSkripsi.jurusan",
												mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan())))
								.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.fakultas"),
										Restrictions.eq("komponenPenilaianProposalSkripsi.fakultas",
												mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas())))

								.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi." + kolom),
										Restrictions.eq("komponenPenilaianProposalSkripsi." + kolom, true)))
								.add(Restrictions.or(Restrictions.isNull("komponenPenilaianProposalSkripsi.aktif"),
										Restrictions.eq("komponenPenilaianProposalSkripsi.aktif", true)))
								.add(Restrictions.eq("formatNilaiProposalSkripsi",
										mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi())),
						KomponenPenilaianProposalSkripsi.class, false);

		TreeMap<Long, List<KomponenPenilaianProposalSkripsi>> dataKomponenPenilaian = new TreeMap<Long, List<KomponenPenilaianProposalSkripsi>>();
		for (KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : formatNilaiProposalSkripsiPunyaKomponenPenilaianProposalSkripsis) {
			if (komponenPenilaianProposalSkripsi.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianProposalSkripsi.getParent().getId())) {
					List<KomponenPenilaianProposalSkripsi> datas = new ArrayList<KomponenPenilaianProposalSkripsi>();
					datas.add(komponenPenilaianProposalSkripsi);
					dataKomponenPenilaian.put(komponenPenilaianProposalSkripsi.getParent().getId(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianProposalSkripsi.getParent().getId())
							.add(komponenPenilaianProposalSkripsi);
				}
			}
		}

		for (KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : formatNilaiProposalSkripsiPunyaKomponenPenilaianProposalSkripsis) {
			if (komponenPenilaianProposalSkripsi.getParent() == null
					&& !dataKomponenPenilaian.containsKey(komponenPenilaianProposalSkripsi.getId())) {
				List<KomponenPenilaianProposalSkripsi> datas = new ArrayList<KomponenPenilaianProposalSkripsi>();
				dataKomponenPenilaian.put(komponenPenilaianProposalSkripsi.getId(), datas);
			}
		}

		return dataKomponenPenilaian;
	}

	/**
	 * Implementasi kontrak {@link DataLoader}: memuat ulang grid "Nilai Seminar" dari state
	 * {@code mahasiswaRequestTugasAkhir} saat ini. Memasang {@link DetailKelompokKknRenderer}
	 * sebagai row renderer dan model daftar dosen ({@link MahasiswaRequestTugasAkhir#dataDosen(boolean)}),
	 * membangun ulang footer grid (rata-rata nilai, nilai huruf akhir). Untuk pengguna non-mahasiswa
	 * dan non-siswa (staf/dosen/admin), menambahkan checkbox "Sembunyikan nilai ke mahasiswa" pada
	 * footer — mencentang/melepasnya langsung mem-{@code refresh} entity dari DB,
	 * mengubah {@link MahasiswaRequestTugasAkhir#setSembunyikanNilaiKemahasiswa(Boolean)}, lalu
	 * menyimpan seketika lewat {@code Common.refreshUpdate} + {@code session.flush()} (auto-save,
	 * tanpa tombol Simpan terpisah). Bila nilai sedang disembunyikan dan pengguna login adalah
	 * mahasiswa, footer rata-rata/nilai huruf ditampilkan kosong alih-alih angka asli.
	 *
	 * @param value tidak dipakai — parameter ini murni untuk memenuhi tanda tangan
	 *              {@link DataLoader#loadData(Object)}, dipanggil dengan {@code null} di semua
	 *              tempat pada file ini.
	 */
	public void loadData(Object value) {

		ListModel strset = new SimpleListModel(mahasiswaRequestTugasAkhir.dataDosen(false));
		grid.setRowRenderer(new DetailKelompokKknRenderer());
		grid.setModelCheckMobile(strset);

		Foot foot = grid.getFoot() == null ? new Foot() : grid.getFoot();
		Common.clear(foot);
		grid.appendChild(foot);

		footerRataRataNilai = new Footer(mahasiswaRequestTugasAkhir.getTotalNilai() == null ? ""
				: Common.numberFormat.get().format(mahasiswaRequestTugasAkhir.getTotalNilai()));
		footerNilaiHuruf = new Footer(mahasiswaRequestTugasAkhir.getNilaiHuruf());

		Footer footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("");
		foot.appendChild(footer);
		footer = new Footer("");
		foot.appendChild(footer);

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig("Sembunyikan nilai ke mahasiswa");
			myCheckboxConfig.setChecked(mahasiswaRequestTugasAkhir.getSembunyikanNilaiKemahasiswa());
			footer.appendChild(myCheckboxConfig);
			myCheckboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyCheckboxConfig c = (MyCheckboxConfig) arg0.getTarget();
					Session session = HibernateUtil.currentSession();
					session.refresh(mahasiswaRequestTugasAkhir);

					mahasiswaRequestTugasAkhir.setSembunyikanNilaiKemahasiswa(c.isChecked());
					Common.refreshUpdate(session, mahasiswaRequestTugasAkhir);
					session.flush();
				}
			});
		}

		footer = new Footer("Total");
		foot.appendChild(footer);

		if (mahasiswaRequestTugasAkhir.getSembunyikanNilaiKemahasiswa() && tbmuser != null
				&& tbmuser.getMahasiswa() != null) {
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
	 * Dasbor ringkasan penilaian seminar/proposal Tugas Akhir (HTML/CSS murni, TANPA JFreeChart) -
	 * meniru modul "Ringkasan Penilaian Sidang" pada PenilaianSkripsiHelper agar penilaian
	 * Tugas Akhir SERAGAM dengan menu Skripsi. Menyajikan kartu Total Nilai / Nilai Huruf / Dosen
	 * Penilai, grafik batang "Nilai per Dosen", grafik "Peta Keseimbangan Nilai" (radar CSS),
	 * ringkasan Status/Total Bobot/Nilai Tertinggi/Tanggal Seminar, serta identitas mahasiswa. Angka
	 * bersumber dari MahasiswaRequestTugasAkhir yang sedang dinilai; bila nilai disembunyikan untuk
	 * mahasiswa maka kartu nilai ditandai "-". Read-only sehingga aman dipanggil ulang tiap render.
	 *
	 * <p>Detail per dosen memakai {@link #dashNilaiDosen} dan {@link #dashPersenDosen} untuk
	 * memetakan "jenis" peran ke pasangan nilai/persentase yang tepat (dosen1-3 = pembimbing 1-3,
	 * dosen4-6 = penguji 1-3), {@link #dashFmt} untuk format angka dan {@link #dashEsc} untuk
	 * escape HTML pada teks bebas (nama dosen/mahasiswa, prodi, fakultas) sebelum disisipkan ke
	 * markup mentah lewat {@link StringBuilder}. Grafik "batang" dan "radar" murni CSS (lebar div /
	 * rotasi+translate elemen {@code <span>}), tanpa JavaScript maupun library chart.</p>
	 *
	 * @return komponen {@link org.zkoss.zul.Html} berisi markup dasbor lengkap dengan {@code
	 *         <style>} inline, siap ditempel sebagai anak komponen ZK manapun.
	 */
	private org.zkoss.zul.Html buatDashboardNilai() {
		final MahasiswaRequestTugasAkhir m = mahasiswaRequestTugasAkhir;
		ais.database.model.FormatNilaiProposalSkripsi fmt = (m == null) ? null : m.getFormatNilaiProposalSkripsi();
		boolean sembunyi = m != null && m.getSembunyikanNilaiKemahasiswa() && tbmuser != null
				&& tbmuser.getMahasiswa() != null;
		String nilaiTotal = sembunyi ? "-" : dashFmt(m == null ? null : m.getTotalNilai());
		String nilaiHuruf = sembunyi ? "-" : dashEsc(m == null ? "" : m.getNilaiHuruf());
		String statusLulus = sembunyi ? "-"
				: (m != null && m.getLulus() != null && m.getLulus() ? "Lulus" : "Belum lulus / belum final");
		java.util.List<CommonVO> dataDosen = (m == null) ? new java.util.ArrayList<CommonVO>() : m.dataDosen(false);
		int jumlahDosen = dataDosen.size();
		double totalBobot = 0.0;
		double maxNilai = 0.0;
		StringBuilder bar = new StringBuilder();
		StringBuilder spider = new StringBuilder();
		int idx = 0;
		for (CommonVO vo : dataDosen) {
			Dosen dosen = (vo == null) ? null : (Dosen) vo.getValueObject();
			double nilai = dashNilaiDosen(vo, fmt, m);
			double persen = dashPersenDosen(vo, fmt);
			totalBobot += persen;
			if (nilai > maxNilai) {
				maxNilai = nilai;
			}
			int width = nilai <= 0 ? 2 : (int) Math.min(100, Math.round(nilai));
			bar.append("<div class='ps-row'><div class='ps-row-title'><b>")
					.append(dashEsc(vo == null ? "" : vo.getName())).append("</b><span>").append(dashFmt(nilai))
					.append(" / ").append(dashFmt(persen)).append("%</span></div><div class='ps-bar'><i style='width:")
					.append(width).append("%'></i></div><small>").append(dashEsc(dosen == null ? "" : dosen.getNama()))
					.append("</small></div>");
			int angle = dataDosen.isEmpty() ? 0 : (idx * 360 / dataDosen.size());
			int length = nilai <= 0 ? 5 : (int) Math.min(44, Math.round(nilai * 44 / 100));
			spider.append("<span style='transform:rotate(").append(angle).append("deg) translateY(-")
					.append(length).append("px)'></span>");
			idx++;
		}
		String tanggalSeminar = (m == null || m.getTanggalSeminar() == null) ? "-"
				: Common.dateFormat4.get().format(m.getTanggalSeminar());
		Mahasiswa mhs = (m == null) ? null : m.getMahasiswa();
		String namaMhs = (mhs == null) ? "" : dashEsc(mhs.getNama());
		String nimMhs = (mhs == null) ? "" : dashEsc(mhs.getNim());
		String prodi = "";
		String fak = "";
		try {
			prodi = (mhs == null || mhs.getJurusan() == null) ? "" : dashEsc(mhs.getJurusan().getNama());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:714");
		}
		try {
			fak = (mhs == null || mhs.getJurusan() == null || mhs.getJurusan().getFakultas() == null) ? ""
					: dashEsc(mhs.getJurusan().getFakultas().getNama());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:719");
		}
		StringBuilder html = new StringBuilder();
		html.append("<style>.ps-wrap{font-family:Arial,Helvetica,sans-serif;background:#f8fafc;padding:14px;border-radius:16px;border:1px solid #e5e7eb;margin:0 0 10px 0;color:#0f172a}.ps-head{display:flex;justify-content:space-between;gap:12px;align-items:stretch;flex-wrap:wrap}.ps-title{flex:2;min-width:260px;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#fff;border-radius:16px;padding:16px;box-shadow:0 8px 20px rgba(15,23,42,.12)}.ps-title h2{margin:0 0 6px;font-size:18px}.ps-title p{margin:0;line-height:1.45;color:#dbeafe;font-size:12px}.ps-card{flex:1;min-width:150px;background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:13px;box-shadow:0 6px 18px rgba(15,23,42,.06)}.ps-card b{display:block;font-size:20px;margin-bottom:3px}.ps-card span{font-size:11px;color:#64748b}.ps-grid{display:grid;grid-template-columns:1.5fr .9fr;gap:12px;margin-top:12px}.ps-panel{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:13px;box-shadow:0 6px 18px rgba(15,23,42,.04)}.ps-panel h3{font-size:14px;margin:0 0 4px}.ps-panel p{font-size:12px;color:#64748b;margin:0 0 10px;line-height:1.45}.ps-row{margin:0 0 9px}.ps-row-title{display:flex;justify-content:space-between;gap:8px;font-size:12px}.ps-row small{font-size:10px;color:#64748b}.ps-bar{height:9px;background:#e5e7eb;border-radius:999px;overflow:hidden;margin:4px 0}.ps-bar i{display:block;height:100%;background:linear-gradient(90deg,#22c55e,#2563eb);border-radius:999px}.ps-radar{position:relative;width:160px;height:160px;border-radius:50%;margin:14px auto;background:repeating-radial-gradient(circle,#dbeafe 0,#dbeafe 1px,transparent 1px,transparent 30px),conic-gradient(from 0deg,rgba(37,99,235,.13),rgba(34,197,94,.20),rgba(37,99,235,.13));border:1px solid #bfdbfe}.ps-radar span{position:absolute;left:78px;top:78px;width:8px;height:8px;border-radius:50%;background:#2563eb;transform-origin:4px 4px}.ps-meta{display:grid;grid-template-columns:1fr 1fr;gap:8px}.ps-meta div{background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:9px;font-size:11px}.ps-meta b{display:block;font-size:12px;margin-bottom:3px}@media(max-width:900px){.ps-grid{grid-template-columns:1fr}.ps-head{display:block}.ps-card{margin-top:8px}}</style>");
		html.append("<div class='ps-wrap'>");
		html.append("<div class='ps-head'><div class='ps-title'><h2>Ringkasan Penilaian Seminar</h2><p>Nilai tiap dosen, bobot, dan hasil akhir terlihat dalam satu tempat. Pengguna bisa segera tahu apakah penilaian sudah lengkap dan nilai akhir sudah sesuai.</p></div>");
		html.append("<div class='ps-card'><b>").append(nilaiTotal).append("</b><span>Total Nilai</span></div>");
		html.append("<div class='ps-card'><b>").append(nilaiHuruf).append("</b><span>Nilai Huruf</span></div>");
		html.append("<div class='ps-card'><b>").append(jumlahDosen).append("</b><span>Dosen Penilai</span></div></div>");
		html.append("<div class='ps-grid'><div class='ps-panel'><h3>Nilai per Dosen</h3><p>Perbandingan nilai membantu melihat penilaian mana yang sudah terisi dan mana yang perlu diperiksa lagi.</p>").append(bar).append("</div>");
		html.append("<div class='ps-panel'><h3>Peta Keseimbangan Nilai</h3><p>Bentuk grafik memudahkan melihat apakah nilai antar peran dosen sudah merata atau ada yang berbeda jauh.</p><div class='ps-radar'>").append(spider).append("</div><div class='ps-meta'>");
		html.append("<div><b>Status</b>").append(statusLulus).append("</div><div><b>Total Bobot</b>").append(dashFmt(totalBobot)).append("%</div>");
		html.append("<div><b>Nilai Tertinggi</b>").append(dashFmt(maxNilai)).append("</div><div><b>Tanggal Seminar</b>").append(dashEsc(tanggalSeminar)).append("</div>");
		html.append("</div></div></div>");
		html.append("<div class='ps-panel' style='margin-top:12px'><h3>Data Mahasiswa</h3><p>Identitas ini memastikan nilai yang dihitung masuk ke mahasiswa dan program studi yang benar.</p><div class='ps-meta'><div><b>Mahasiswa</b>").append(namaMhs).append("</div><div><b>NIM</b>").append(nimMhs).append("</div><div><b>Program Studi</b>").append(prodi).append("</div><div><b>Fakultas</b>").append(fak).append("</div></div></div>");
		html.append("</div>");
		return new org.zkoss.zul.Html(html.toString());
	}

	/**
	 * Meng-escape karakter HTML berbahaya ({@code & < > "}) pada teks yang akan disisipkan mentah
	 * ke markup dasbor {@link #buatDashboardNilai()}, mencegah teks bebas (nama, judul, dsb.) merusak
	 * struktur HTML. {@code null} dikembalikan sebagai string kosong.
	 *
	 * @param v teks sumber, boleh {@code null}.
	 * @return teks yang sudah aman disisipkan ke HTML.
	 */
	private String dashEsc(String v) {
		if (v == null) {
			return "";
		}
		return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Memformat angka nilai untuk tampilan dasbor memakai {@link Common#numberFormat}; {@code null}
	 * diperlakukan sebagai {@code 0.0}.
	 *
	 * @param v nilai yang akan diformat, boleh {@code null}.
	 * @return representasi string angka sesuai format angka aplikasi.
	 */
	private String dashFmt(Double v) {
		return Common.numberFormat.get().format(v == null ? 0.0 : v);
	}

	/**
	 * Null-safe unboxing untuk nilai {@code Double} dasbor: {@code null} menjadi {@code 0.0}.
	 *
	 * @param v nilai sumber, boleh {@code null}.
	 * @return nilai primitif, {@code 0.0} bila {@code v} adalah {@code null}.
	 */
	private double dashSafe(Double v) {
		return v == null ? 0.0 : v;
	}

	/**
	 * Mengambil nilai numerik dosen tertentu pada dasbor berdasarkan "jenis" perannya, dengan
	 * mencocokkan {@code vo.getName()} ke {@code fmt.getDosen1()}..{@code getDosen6()} lalu
	 * mengembalikan field {@code nilaiDosenN} yang bersesuaian dari {@code m}
	 * ({@link MahasiswaRequestTugasAkhir}). Mengembalikan {@code 0.0} bila parameter tidak lengkap
	 * atau peran tidak dikenali.
	 *
	 * @param vo  value object dosen (dari {@link MahasiswaRequestTugasAkhir#dataDosen(boolean)}),
	 *            {@code name}-nya menentukan peran mana yang dicocokkan.
	 * @param fmt format nilai proposal skripsi yang memetakan nama peran ke kolom dosen1..dosen6.
	 * @param m   data Tugas Akhir sumber nilai dosen1..dosen6.
	 * @return nilai dosen untuk peran tersebut, atau {@code 0.0} bila tidak ditemukan/{@code null}.
	 */
	private double dashNilaiDosen(CommonVO vo, ais.database.model.FormatNilaiProposalSkripsi fmt,
			MahasiswaRequestTugasAkhir m) {
		if (vo == null || fmt == null || m == null || vo.getName() == null) {
			return 0.0;
		}
		String j = vo.getName();
		if (j.equals(fmt.getDosen1())) return dashSafe(m.getNilaiDosen1());
		if (j.equals(fmt.getDosen2())) return dashSafe(m.getNilaiDosen2());
		if (j.equals(fmt.getDosen3())) return dashSafe(m.getNilaiDosen3());
		if (j.equals(fmt.getDosen4())) return dashSafe(m.getNilaiDosen4());
		if (j.equals(fmt.getDosen5())) return dashSafe(m.getNilaiDosen5());
		if (j.equals(fmt.getDosen6())) return dashSafe(m.getNilaiDosen6());
		return 0.0;
	}

	/**
	 * Mengambil persentase bobot peran dosen tertentu pada dasbor, dengan mencocokkan
	 * {@code vo.getName()} ke {@code fmt.getDosen1()}..{@code getDosen6()} lalu mengembalikan
	 * persentase pembimbing (dosen1-3) atau penguji (dosen4-6) yang bersesuaian dari {@code fmt}.
	 * Mengembalikan {@code 0.0} bila parameter tidak lengkap atau peran tidak dikenali.
	 *
	 * @param vo  value object dosen, {@code name}-nya menentukan peran mana yang dicocokkan.
	 * @param fmt format nilai proposal skripsi sumber persentase bobot per peran.
	 * @return persentase bobot peran dosen tersebut, atau {@code 0.0} bila tidak ditemukan/{@code null}.
	 */
	private double dashPersenDosen(CommonVO vo, ais.database.model.FormatNilaiProposalSkripsi fmt) {
		if (vo == null || fmt == null || vo.getName() == null) {
			return 0.0;
		}
		String j = vo.getName();
		if (j.equals(fmt.getDosen1())) return dashSafe(fmt.getProsentasiNilaiPembimbing1());
		if (j.equals(fmt.getDosen2())) return dashSafe(fmt.getProsentasiNilaiPembimbing2());
		if (j.equals(fmt.getDosen3())) return dashSafe(fmt.getProsentasiNilaiPembimbing3());
		if (j.equals(fmt.getDosen4())) return dashSafe(fmt.getProsentasiNilaiPenguji1());
		if (j.equals(fmt.getDosen5())) return dashSafe(fmt.getProsentasiNilaiPenguji2());
		if (j.equals(fmt.getDosen6())) return dashSafe(fmt.getProsentasiNilaiPenguji3());
		return 0.0;
	}

	/**
	 * Titik masuk utama helper: membangun seluruh layar penilaian proposal skripsi/Tugas Akhir ke
	 * dalam {@code component} yang diberikan (mengosongkannya lebih dulu lewat
	 * {@code Common.clear(component)}). Layout memakai {@link Borderlayout} dengan tinggi tetap
	 * 1050px (dipilih agar tidak kolaps saat form dimuat dari tab custom eLearning, lihat komentar
	 * inline): NORTH = toolbar aksi, WEST 42% = form "Hasil Seminar", CENTER = dasbor
	 * ({@link #buatDashboardNilai()}) + grid "Nilai Seminar" per dosen. Method ini idempotent per
	 * pemanggilan — dipanggil ulang dari beberapa listener (Refresh, Reset, ganti matakuliah, ganti
	 * format nilai) untuk me-render ulang layar dari state {@code mahasiswaRequestTugasAkhir}
	 * terbaru.
	 *
	 * <p><b>Efek samping saat membangun toolbar</b> (bukan hanya tampilan pasif):</p>
	 * <ul>
	 * <li>Bila {@code mahasiswaRequestTugasAkhir.getDetailperkuliahan()} masih kosong, method ini
	 * MENCOBA mengisinya otomatis lewat
	 * {@code Common.checkApakahSudahMengambilKrsSeminarSkripsi(...)} dan langsung menyimpannya bila
	 * ketemu — bukan sekadar membaca.</li>
	 * <li>Tombol "Blanko Penilaian" dan "Berita Acara" masing-masing membangun peta parameter (nama
	 * dosen, NIDN/NIP, tanda tangan scan dari {@link LampiranLain#TTD_DOSEN}/{@code TTD_MAHASISWA},
	 * judul, kaprodi/dekan, dan detail per komponen nilai) lalu memanggil
	 * {@link Report#generatePDFReport} untuk mencetak PDF "Blanko_Proposal_Skripsi" dan
	 * "Berita_Acara_Proposal_Skripsi".</li>
	 * <li>Tombol "Reset" (hanya tampil untuk admin, {@code Common.getApakahAdmin()}) setelah
	 * konfirmasi akan MENGHAPUS seluruh nilai dosen1-6, nilai huruf, total IP, total nilai, dan
	 * detail nilai per komponen milik {@code mahasiswaRequestTugasAkhir} lalu menyimpannya — operasi
	 * destruktif dan tidak bisa dibatalkan selain dengan mengisi ulang nilai secara manual.</li>
	 * <li>Combobox format nilai menyimpan pilihan langsung ke DB saat berubah; kegagalan simpan
	 * (mis. format nilai sudah dihapus admin lain, race condition lintas sesi) ditangkap, transaksi
	 * di-rollback, dicatat lewat {@link ais.common.ErrorAuditUtil}, dan pengguna diberi tahu lewat
	 * {@link MyMessageboxConfig} alih-alih exception mentah.</li>
	 * </ul>
	 *
	 * @param mahasiswaRequestTugasAkhir data pengajuan Tugas Akhir/proposal skripsi yang akan
	 *                                   ditampilkan dan dinilai; disimpan ke field instance dengan
	 *                                   nama sama sehingga dipakai oleh seluruh method lain di
	 *                                   kelas ini selama instance ini hidup.
	 * @param component                 komponen ZK induk tempat layar penilaian dipasang; isi
	 *                                   sebelumnya akan dibuang.
	 */
	public void display(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Component component) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		Common.clear(component);

		Detailperkuliahan detailperkuliahan = mahasiswaRequestTugasAkhir.getDetailperkuliahan();
		Mahasiswa mahasiswa = mahasiswaRequestTugasAkhir.getMahasiswa();

		// UI/UX (07-12): FRAME disamakan dengan PenilaianSkripsiHelper (menu Skripsi) — wrapper Div
		// (Style scoped .eL-penilaian) -> Borderlayout: NORTH = bar toolbar aksi yang menempel di
		// atas, CENTER = konten penilaian (autoscroll, tinggi pasti min-height:480px agar konten
		// panjang tak terpotong). Konten tetap 1-kolom (Hasil Seminar lalu Nilai Seminar) sesuai
		// perbaikan sebelumnya; hanya bingkai/tata-letak luar yang diseragamkan.
		org.zkoss.zul.Div wrapperEl = new org.zkoss.zul.Div();
		wrapperEl.setWidth("100%");
		wrapperEl.setHeight("1050px");
		wrapperEl.setStyle("height:1050px;min-height:480px;overflow:hidden;");
		wrapperEl.setParent(component);

		org.zkoss.zul.Style eLStyle = new org.zkoss.zul.Style();
		eLStyle.setContent(".eL-penilaian .z-toolbar{background:linear-gradient(180deg,#f8fafc,#eef2f7);border:1px solid #e2e8f0;border-radius:10px;padding:6px 10px;box-shadow:0 1px 2px rgba(0,0,0,.05);margin-bottom:8px}.eL-penilaian .z-toolbarbutton{color:#1d4ed8;font-weight:600}.eL-penilaian .z-toolbarbutton:hover{color:#1e40af}.eL-penilaian .z-grid,.eL-penilaian .z-groupbox{border:1px solid #e2e8f0;border-radius:12px;box-shadow:0 4px 6px -1px rgba(0,0,0,.05);background:#fff}.eL-penilaian .z-label{color:#334155}.eL-penilaian .z-textbox,.eL-penilaian .z-combobox-inp,.eL-penilaian .z-decimalbox-inp,.eL-penilaian .z-datebox-inp,.eL-penilaian .z-intbox-inp{border-radius:8px}.eL-penilaian .z-row-cnt,.eL-penilaian .z-cell{padding:6px 8px}");
		eLStyle.setParent(wrapperEl);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(wrapperEl);
		// Tinggi pixel diperlukan saat form dimuat dari tab custom eLearning. Tinggi 100%
		// dapat kolaps pada rantai panel bersarang dan membuat akun dosen melihat layar putih.
		borderlayout.setWidth("100%");
		borderlayout.setHeight("1050px");
		borderlayout.setStyle("height:1050px;min-height:480px;");
		borderlayout.setSclass("eL-penilaian");

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		toolbar.setHeight("40px");

		// WEST = kolom kiri: form "Hasil Seminar" (mirror West pada PenilaianSkripsiHelper).
		org.zkoss.zul.West west = new org.zkoss.zul.West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("42%");
		west.setAutoscroll(true);

		// CENTER = kolom kanan: dasbor ringkasan + grafik/chart, lalu input "Nilai Seminar".
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);

		// Kontainer form Hasil Seminar (ditaruh di WEST).
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(west);

		// Kontainer kanan (CENTER): dasbor grafik/chart di atas, "Nilai Seminar" menyusul di bawah.
		// Kolom kanan (daftar dosen penilai) WAJIB bisa di-scroll: sebelumnya Vbox tanpa overflow,
		// sehingga bila jumlah dosen + dashboard nilai melebihi tinggi Center, baris terakhir terpotong
		// dan tidak ada scrollbar — dosen terakhir tak bisa dinilai. Pakai Div height 100% + overflow:auto.
		org.zkoss.zul.Div centerBox = new org.zkoss.zul.Div();
		centerBox.setWidth("100%");
		centerBox.setHeight("100%");
		centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);
		buatDashboardNilai().setParent(centerBox);

		Grid gridAtas = new Grid();
		gridAtas.setSclass("fgrid");
		gridAtas.setParent(groupbox);
		gridAtas.setWidth("100%");
		gridAtas.setHeight("100%");

		// Paksa SINGLE-COLUMN seperti tampilan mobile (yang sudah benar): layout 2-kolom lama
		// membuat grid "Nilai Seminar" terjepit & terpotong di kolom kanan (bug tampilan sempro).
		// "Hasil Seminar" tampil penuh, lalu "Nilai Seminar" penuh di bawahnya. Set true utk kembali 2-kolom.
		boolean duaKolom = false;
		if (duaKolom && !Common.isMobile()) {
			Columns columns = new Columns();
			columns.setParent(gridAtas);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);
		}

		Rows rows = new Rows();
		rows.setParent(gridAtas);
		MyFormRow rowUtama = new MyFormRow();
		rowUtama.setValign("top");
		rowUtama.setParent(rows);

		Groupbox groupbox2 = new ais.ui.util.MyGroupboxStyled();
		groupbox2.setParent(rowUtama);
		groupbox2.appendChild(new MyCaptionStyled("Hasil Seminar"));

		Grid grid1 = new Grid();
		grid1.setSclass("fgrid");
		grid1.setParent(groupbox2);
		grid1.setWidth("100%");
		// grid1.setHeight("600px");

		Columns columns = new Columns();
		columns.setParent(grid1);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid1);

		MyFormRow rowSeminar = new MyFormRow();
		rowSeminar.setStyle("border:0px;background: transparent;");
		rowSeminar.setParent(rows);
		rowSeminar.appendChild(new Label("Jadwal Proposal / Seminar "));

		// final AmbilJadwalSeminarTugasAkhirBanbox jadwalSeminarTugasAkhir =
		// new AmbilJadwalSeminarTugasAkhirBanbox();
		// jadwalSeminarTugasAkhir.setValue(mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir()
		// == null ? ""
		// : mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir().getNama());
		// jadwalSeminarTugasAkhir.setAttribute("jadwalSeminarTugasAkhir",
		// mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir());
		// jadwalSeminarTugasAkhir.setAttribute("myValue",
		// mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir());
		// jadwalSeminarTugasAkhir.setWidth("90%");
		//
		// if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {
		rowSeminar.appendChild(new Label(mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir() == null ? ""
				: mahasiswaRequestTugasAkhir.getJadwalSeminarTugasAkhir().getNama()));
		// } else {
		// rowSeminar.appendChild(jadwalSeminarTugasAkhir);
		// }

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Seminar"));

		final MyDatebox tanggalSeminar = new MyDatebox(mahasiswaRequestTugasAkhir.getTanggalSeminar());
		tanggalSeminar.setCols(6);

		final Timebox waktuSeminar = new ais.ui.util.MyTimebox();
		final Timebox waktuSampaiSeminar = new ais.ui.util.MyTimebox();

		Hbox hbox = new Hbox();

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			hbox.appendChild(waktuSeminar);
			hbox.appendChild(new Label("-"));
			hbox.appendChild(waktuSampaiSeminar);
		} else {
			hbox.appendChild(new Label(mahasiswaRequestTugasAkhir.getWaktuSeminar()));
			hbox.appendChild(new Label("-"));
			hbox.appendChild(new Label(mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar()));
		}

		waktuSeminar.setFormat(Common.timeFormat.get().toPattern());
		waktuSampaiSeminar.setFormat(Common.timeFormat.get().toPattern());
		try {
			waktuSeminar.setValue(Common.timeFormat.get().parse(mahasiswaRequestTugasAkhir.getWaktuSeminar()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:947");

		}
		try {
			waktuSampaiSeminar.setValue(Common.timeFormat.get().parse(mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:952");

		}

		waktuSeminar.setCols(5);
		waktuSampaiSeminar.setCols(5);

		Vbox vbox = new Vbox();
		row.appendChild(vbox);
		if (tbmuser.getMahasiswa() != null) {
			vbox.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? ""
					: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalSeminar())));
			vbox.appendChild(hbox);
		} else {
			vbox.appendChild(tanggalSeminar);
			vbox.appendChild(hbox);
		}

		final MyFormRow rowUpload = new MyFormRow();
		rowUpload.setStyle("border:0px;background: transparent;");
		rowUpload.setParent(rows);
		rowUpload.appendChild(new Label(ais.common.Common.getBahasaConfig("Proposal")));

		hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setStyle("border:0px;background: transparent;");

		LampiranLain.createDownloadUploadFileLain(hbox,
				mahasiswaRequestTugasAkhir.getId() == null ? -Common.randLong() : mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName(), "Proposal", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa);
						lainMahasiswa.setRef(mahasiswaRequestTugasAkhir.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					}
				});
		hbox.setParent(rowUpload);

		final MyFormRow rowUploadFilePpt = new MyFormRow();
		rowUploadFilePpt.setStyle("border:0px;background: transparent;");
		rowUploadFilePpt.setParent(rows);
		rowUploadFilePpt.appendChild(new Label(ais.common.Common.getBahasaConfig("Presentasi")));

		hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setStyle("border:0px;background: transparent;");

		LampiranLain.createDownloadUploadFileLain(hbox,
				mahasiswaRequestTugasAkhir.getId() == null ? -Common.randLong() : mahasiswaRequestTugasAkhir.getId(),
				MahasiswaRequestTugasAkhir.class.getName() + "_Presentasi", "File Presentasi (PPT)", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lainMahasiswa);
						lainMahasiswa.setRef(mahasiswaRequestTugasAkhir.getId());

						session.getTransaction().begin();
						session.update(lainMahasiswa);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					}
				});
		hbox.setParent(rowUploadFilePpt);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Bimbingan"));
		// final MyDatebox tanggalAwalBimbingan = new
		// MyDatebox(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());

		// if (tbmuser.getMahasiswa() != null) {
		row.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null ? ""
				: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan())));
		// } else {
		// row.appendChild(tanggalAwalBimbingan);
		// }

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Bimbingan"));
		// final MyDatebox tanggalAkhirBimbingan = new
		// MyDatebox(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());

		// if (tbmuser.getMahasiswa() != null) {
		row.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan() == null ? ""
				: Common.dateFormat4.get().format(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan())));
		// } else {
		// row.appendChild(tanggalAkhirBimbingan);
		// }

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Penting"));
		final Textbox catatanSeminar = new Textbox(mahasiswaRequestTugasAkhir.getCatatanSeminar());
		if (tbmuser.getMahasiswa() != null) {
			row.appendChild(
					new ais.ui.util.MyHtml(mahasiswaRequestTugasAkhir.getCatatanSeminar().replaceAll("\n", "<br>")));
		} else {
			row.appendChild(catatanSeminar);
		}
		catatanSeminar.setWidth("90%");
		catatanSeminar.setRows(10);

		row = new MyFormRow();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(""));
		final MyCheckboxConfig tanpaPerbaikan = new MyCheckboxConfig("Tanpa Perbaikan");
		if (tbmuser.getMahasiswa() != null) {
			row.appendChild(new Label(mahasiswaRequestTugasAkhir.getTanpaPerbaikan() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(tanpaPerbaikan);
		}
		tanpaPerbaikan.setChecked(mahasiswaRequestTugasAkhir.getTanpaPerbaikan());

		EventListener ubah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswaRequestTugasAkhir.setTanggalSeminar(tanggalSeminar.getValue());
				mahasiswaRequestTugasAkhir.setWaktuSampaiSeminar(
						waktuSampaiSeminar == null || waktuSampaiSeminar.getValue() == null ? null
								: Common.timeFormat.get().format(waktuSampaiSeminar.getValue()));
				mahasiswaRequestTugasAkhir
						.setWaktuSeminar(waktuSeminar == null || waktuSeminar.getValue() == null ? null
								: Common.timeFormat.get().format(waktuSeminar.getValue()));
				mahasiswaRequestTugasAkhir.setCatatanSeminar(catatanSeminar.getValue().trim());
				mahasiswaRequestTugasAkhir.setTanpaPerbaikan(tanpaPerbaikan.isChecked());
				// mahasiswaRequestTugasAkhir.setJadwalSeminarTugasAkhir(
				// (JadwalSeminarTugasAkhir)
				// jadwalSeminarTugasAkhir.getAttribute("jadwalSeminarTugasAkhir"));
				Common.refreshUpdate(mahasiswaRequestTugasAkhir);
			}
		};

		// jadwalSeminarTugasAkhir.setEventListener(ubah);
		catatanSeminar.addEventListener("onChange", ubah);
		tanggalSeminar.addEventListener("onChange", ubah);
		waktuSampaiSeminar.addEventListener("onChange", ubah);
		waktuSeminar.addEventListener("onChange", ubah);
		tanpaPerbaikan.addEventListener("onClick", ubah);
		// tanggalAwalBimbingan.addEventListener("onChange", ubah);
		// tanggalAkhirBimbingan.addEventListener("onChange", ubah);

		if (duaKolom && !Common.isMobile()) {
			grid1 = new Grid();
			grid1.setSclass("fgrid");
			grid1.setParent(rowUtama);
			grid1.setWidth("100%");
			grid1.setHeight("100%");

			rows = new Rows();
			rows.setParent(grid1);

			row = new MyFormRow();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(row);
		}

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai masuk ke : ")));
		if (detailperkuliahan == null) {
			detailperkuliahan = Common.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa,
					mahasiswaRequestTugasAkhir.getSemester(),
					mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getKodeItemBiaya());
			if (detailperkuliahan != null) {
				mahasiswaRequestTugasAkhir.setDetailperkuliahan(detailperkuliahan);
				Common.refreshUpdate(mahasiswaRequestTugasAkhir);
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

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswaRequestTugasAkhir.setDetailperkuliahan(
							(Detailperkuliahan) ambilDataMatakuliahBanbox.getAttribute("detailperkuliahan"));
					Common.refreshUpdate(mahasiswaRequestTugasAkhir);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							display(mahasiswaRequestTugasAkhir, component);
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

						FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

						@Override
						public void onEvent(Event event) throws Exception {

							MyWindow addWindow = new MyWindow();
							addWindow.setHeight("95%");
							addWindow.setWidth("700px");
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);

							formatPenilaianHelper.display(perkuliahan, addWindow, new TampilDetailNilaiInterface() {

								@Override
								public void realoadNilai(final Perkuliahan perkuliahan) {

									Common.realoadNilai(perkuliahan,
											perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															display(mahasiswaRequestTugasAkhir, component);
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
				if (mahasiswaRequestTugasAkhir.getFormatNilai() == null) {
					formatNilai.setSelectedItem(comboitemTidakAda);
				} else {
					Common.selectComboItem(formatNilai, mahasiswaRequestTugasAkhir.getFormatNilai());
				}
				formatNilai.setReadonly(true);
				formatNilai.setDisabled(detailperkuliahan.getPerkuliahan().getDikunci() != null);

				final Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();

				MyToolbarbuttonConfig buttonSingkronkan = new MyToolbarbuttonConfig("Singkronkan Nilai",
						"/img/Configure.gif");
				buttonSingkronkan.setParent(toolbar);
				buttonSingkronkan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilaiSkripsi(perkuliahan,
								mahasiswaRequestTugasAkhir.getFormatNilai());
					}
				});

				formatNilai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final FormatNilai fn = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
								: formatNilai.getSelectedItem().getValue());

						Session session = HibernateUtil.currentSession();
						mahasiswaRequestTugasAkhir.setFormatNilai(fn);
						try {
							Common.refreshUpdate(session, (mahasiswaRequestTugasAkhir));
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
									"auto-audit(rollback-gagal) src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java onFormatNilaiChange"); }
							ais.common.ErrorAuditUtil.record(eSimpan,
									"PenilaianProposalSkripsiHelper: gagal simpan format nilai untuk MahasiswaRequestTugasAkhir id="
											+ (mahasiswaRequestTugasAkhir == null ? "null" : mahasiswaRequestTugasAkhir.getId()));
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

		if (mahasiswaRequestTugasAkhir.getSembunyikanNilaiKemahasiswa() && tbmuser != null
				&& tbmuser.getMahasiswa() != null) {

		} else {

			MyToolbarbuttonConfig buttonBlanko = new MyToolbarbuttonConfig("Blanko Penilaian",
					"/img/Text-Edit-icon.png");
			buttonBlanko.setParent(toolbar);
			buttonBlanko.addEventListener("onClick", new EventListener() {

				/**
				 * Membangun satu baris parameter (satu {@link Map}) untuk template PDF "Blanko
				 * Penilaian" — satu baris per pasangan (dosen, komponen penilaian): identitas
				 * dosen (NIDN/NIP/nama, path tanda tangan scan dari
				 * {@link LampiranLain#TTD_DOSEN}), identitas mahasiswa/judul/jurusan/fakultas,
				 * serta nilai, bobot, dan hasil kali nilai x bobot untuk komponen tersebut.
				 * Dipanggil berulang per komponen di dalam {@code onEvent} sehingga hasilnya
				 * dikumpulkan menjadi daftar {@code maps} yang menjadi tabel detail laporan.
				 *
				 * @param commonVO                          value object dosen ({@code name}
				 *                                           = peran, value object = {@link Dosen}).
				 * @param komponenPenilaianProposalSkripsi   komponen penilaian yang nilainya
				 *                                           dimasukkan ke baris ini.
				 * @param induk                              {@code true} bila komponen ini adalah
				 *                                           komponen tunggal/induk tanpa
				 *                                           sub-komponen (dipakai template untuk
				 *                                           membedakan gaya baris).
				 * @return peta parameter satu baris tabel blanko penilaian.
				 */
				@SuppressWarnings({ "unchecked", "rawtypes" })
				private Map masukkanParameter(CommonVO commonVO,
						KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi, Boolean induk) {
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:1320");
					}

					parameter.put("nidn_dosen", dosen.getNidn());
					parameter.put("nip_dosen", dosen.getCode());
					parameter.put("nama_dosen", dosen.getNama());
					parameter.put("nama_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getNama());
					parameter.put("jenjang", mahasiswaRequestTugasAkhir.getMahasiswa().getJenjang() == null ? ""
							: mahasiswaRequestTugasAkhir.getMahasiswa().getJenjang().getNama());
					parameter.put("nim_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getNim());
					parameter.put("induk", induk);
					parameter.put("jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getNama());

					parameter.put("judul", mahasiswaRequestTugasAkhir.getJudul());
					parameter.put("judul1", mahasiswaRequestTugasAkhir.getJudul1());
					parameter.put("judul2", mahasiswaRequestTugasAkhir.getJudul2());
					parameter.put("judul3", mahasiswaRequestTugasAkhir.getJudul3());
					parameter.put("judul4", mahasiswaRequestTugasAkhir.getJudul4());
					parameter.put("judul5", mahasiswaRequestTugasAkhir.getJudul5());
					parameter.put("judul6", mahasiswaRequestTugasAkhir.getJudul6());
					parameter.put("judul7", mahasiswaRequestTugasAkhir.getJudul7());
					parameter.put("judul8", mahasiswaRequestTugasAkhir.getJudul8());
					parameter.put("judul9", mahasiswaRequestTugasAkhir.getJudul9());
					parameter.put("judul10", mahasiswaRequestTugasAkhir.getJudul10());
					parameter.put("jenis", commonVO.getName());

					Double nilai = mahasiswaRequestTugasAkhir.retreiveDetailNilai(komponenPenilaianProposalSkripsi,
							dosen);
					parameter.put("nilai", nilai);
					parameter.put("bobot", komponenPenilaianProposalSkripsi.getBobot());
					parameter.put("komponen", komponenPenilaianProposalSkripsi.getNama());

					parameter.put("keterangan_komponen", komponenPenilaianProposalSkripsi.getKeterangan());
					parameter.put("keterangan_nourut", komponenPenilaianProposalSkripsi.getNomorUrut());

					parameter.put("hasil_kali", nilai * komponenPenilaianProposalSkripsi.getBobot());
					parameter.put("jenis_semester",
							mahasiswaRequestTugasAkhir.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					parameter.put("tanggal",
							Common.dateFormat2.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));
					parameter.put("hari_tanggal",
							Common.dateFormat6.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));
					parameter.put("tahun_ajaran", mahasiswaRequestTugasAkhir.getTahunAkademik());
					parameter.put("tanggal_seminar", mahasiswaRequestTugasAkhir.getTanggalSeminar());
					parameter.put("waktu_seminar_mulai", mahasiswaRequestTugasAkhir.getWaktuSeminar());
					parameter.put("waktu_seminar_sampai", mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar());
					return parameter;
				}

				/**
				 * Handler tombol "Blanko Penilaian": untuk setiap dosen penilai (dari
				 * {@link MahasiswaRequestTugasAkhir#dataDosen(boolean)}) dan setiap komponen
				 * penilaiannya ({@link #populateKomponen(String)}) membangun satu baris parameter
				 * lewat {@code masukkanParameter(...)} di atas, mengumpulkan tanda tangan scan
				 * dosen ({@link LampiranLain#TTD_DOSEN}) dan mahasiswa
				 * ({@link LampiranLain#TTD_MAHASISWA}), lalu melengkapi parameter global (judul,
				 * kaprodi, dekan, jadwal, total nilai, dsb.) sebelum mencetak PDF
				 * "Blanko_Proposal_Skripsi" lewat {@link Report#generatePDFReport}. Murni baca DB
				 * dan tulis file laporan sementara; tidak mengubah data penilaian.
				 *
				 * @param event event klik tombol, tidak dipakai isinya.
				 */
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void onEvent(Event event) throws Exception {
					List<CommonVO> dataDosen = mahasiswaRequestTugasAkhir.dataDosen(false);
					List<Map> maps = new ArrayList();
					Map parameter = ais.common.HashMapGenerator.getRand();
					int indexTtd = 1;
					for (CommonVO commonVO : dataDosen) {

						TreeMap<Long, List<KomponenPenilaianProposalSkripsi>> dataKomponenPenilaian = populateKomponen(
								commonVO.getName());

						System.out.println("commonVO.getName() -> " + commonVO.getName() + ", banyak -> "
								+ dataKomponenPenilaian.size());

						for (Long parentId : dataKomponenPenilaian.keySet()) {
							KomponenPenilaianProposalSkripsi parent = (KomponenPenilaianProposalSkripsi) ConstantValues
									.ambil(KomponenPenilaianProposalSkripsi.class.getName(), parentId);

							List<KomponenPenilaianProposalSkripsi> datas = dataKomponenPenilaian.get(parentId);

							System.out.println("komponenPenilaianProposalSkripsi -> " + parent.getNama()
									+ ", banyak -> " + datas.size());

							if (datas.isEmpty()) {
								maps.add(masukkanParameter(commonVO, parent, true));
							} else {

								for (KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : datas) {
									maps.add(masukkanParameter(commonVO, komponenPenilaianProposalSkripsi, false));
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:1422");
						}
						indexTtd++;
					}

					try {
						LampiranLain lampiranLain = LampiranLain
								.ambil(mahasiswaRequestTugasAkhir.getMahasiswa().getId(), LampiranLain.TTD_MAHASISWA);
						if (lampiranLain != null) {
							parameter.put("ttd_mhs", lampiranLain.ambilFile().getAbsolutePath());
						} else {
							parameter.put("ttd_mhs", "");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:1436");
					}

					Common.insertProperty(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir, parameter,
							"bimbingan");
					parameter.put("jenjang", mahasiswaRequestTugasAkhir.getMahasiswa().getJenjang() == null ? ""
							: mahasiswaRequestTugasAkhir.getMahasiswa().getJenjang().getNama());
					parameter.put("nama_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getNama());
					parameter.put("nim_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getNim());
					parameter.put("jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getNama());
					parameter.put("kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getNama());
					parameter.put("nidn_kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getNidn());
					parameter.put("nip_kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getCode());

					parameter.put("dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getNama());
					parameter.put("nidn_dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getNidn());
					parameter.put("nip_dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getCode());

					parameter.put("judul", mahasiswaRequestTugasAkhir.getJudul());
					parameter.put("judul1", mahasiswaRequestTugasAkhir.getJudul1());
					parameter.put("judul2", mahasiswaRequestTugasAkhir.getJudul2());
					parameter.put("judul3", mahasiswaRequestTugasAkhir.getJudul3());
					parameter.put("judul4", mahasiswaRequestTugasAkhir.getJudul4());
					parameter.put("judul5", mahasiswaRequestTugasAkhir.getJudul5());
					parameter.put("judul6", mahasiswaRequestTugasAkhir.getJudul6());
					parameter.put("judul7", mahasiswaRequestTugasAkhir.getJudul7());
					parameter.put("judul8", mahasiswaRequestTugasAkhir.getJudul8());
					parameter.put("judul9", mahasiswaRequestTugasAkhir.getJudul9());
					parameter.put("judul10", mahasiswaRequestTugasAkhir.getJudul10());
					parameter.put("jenis_semester",
							mahasiswaRequestTugasAkhir.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					parameter.put("tahun_ajaran", mahasiswaRequestTugasAkhir.getTahunAkademik());
					parameter.put("tanggal",
							Common.dateFormat2.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));

					parameter.put("tanggal_1",
							Common.dateFormat6.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));

					parameter.put("waktu_mulai_seminar", mahasiswaRequestTugasAkhir.getWaktuSeminar());
					parameter.put("waktu_sampai_seminar", mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar());
					parameter.put("lulus", mahasiswaRequestTugasAkhir.getLulus());
					parameter.put("nilai_total", mahasiswaRequestTugasAkhir.getTotalNilai());
					parameter.put("nilai_huruf", mahasiswaRequestTugasAkhir.getNilaiHuruf());
					parameter.put("catatan_seminar", mahasiswaRequestTugasAkhir.getCatatanSeminar());
					parameter.put("tanggal_seminar", mahasiswaRequestTugasAkhir.getTanggalSeminar());
					parameter.put("waktu_seminar_mulai", mahasiswaRequestTugasAkhir.getWaktuSeminar());
					parameter.put("waktu_seminar_sampai", mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar());
					parameter.put("maps", maps);

					Report.generatePDFReport(Report.PDF, parameter, "Blanko_Proposal_Skripsi",
							ais.ui.util.WaktuUtil.getDate(), maps);
				}

			});

			buttonBlanko = new MyToolbarbuttonConfig("Berita Acara", "/img/Document-Text-icon.png");
			buttonBlanko.setParent(toolbar);
			buttonBlanko.addEventListener("onClick", new EventListener() {

				/**
				 * Membangun satu baris parameter untuk template PDF "Berita Acara" — satu baris
				 * per dosen (bukan per komponen seperti pada Blanko Penilaian): identitas dosen,
				 * kaprodi/dekan, serta nilai total dan persentase bobot peran dosen tersebut
				 * (dicari lewat perbandingan {@code commonVO.getName()} terhadap
				 * {@code dosen1}..{@code dosen6} pada {@link ais.database.model.FormatNilaiProposalSkripsi},
				 * sama seperti pemetaan di {@link DetailKelompokKknRenderer#render}).
				 *
				 * @param commonVO value object dosen ({@code name} = peran, value object = {@link Dosen}).
				 * @return peta parameter satu baris tabel berita acara.
				 */
				@SuppressWarnings({ "unchecked", "rawtypes" })
				private Map masukkanParameter(CommonVO commonVO) {
					Dosen dosen = (Dosen) commonVO.getValueObject();
					Map parameter = ais.common.HashMapGenerator.getRand();
					parameter.put("jenis", commonVO.getName());

					try {
						LampiranLain lampiranLain = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
						if (lampiranLain != null) {
							parameter.put("ttd_dsn", lampiranLain.ambilFile().getAbsolutePath());

						} else {
							parameter.put("ttd_dsn", "");

						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:1533");
					}

					parameter.put("nidn_dosen", dosen.getNidn());
					parameter.put("nip_dosen", dosen.getCode());
					parameter.put("nama_dosen", dosen.getNama());
					parameter.put("jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getNama());
					parameter.put("kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getNama());
					parameter.put("nidn_kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getNidn());
					parameter.put("nip_kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getCode());

					parameter.put("dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getNama());
					parameter.put("nidn_dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getNidn());
					parameter.put("nip_dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getCode());

					Double nilai = 0.0;
					Double persen = 0.0;
					if (commonVO.getName()
							.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen1())) {
						nilai = mahasiswaRequestTugasAkhir.getNilaiDosen1();
						persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
								.getProsentasiNilaiPembimbing1();
					} else if (commonVO.getName()
							.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen2())) {
						nilai = mahasiswaRequestTugasAkhir.getNilaiDosen2();
						persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
								.getProsentasiNilaiPembimbing2();
					} else if (commonVO.getName()
							.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen3())) {
						nilai = mahasiswaRequestTugasAkhir.getNilaiDosen3();
						persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
								.getProsentasiNilaiPembimbing3();
					} else if (commonVO.getName()
							.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen4())) {
						nilai = mahasiswaRequestTugasAkhir.getNilaiDosen4();
						persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
								.getProsentasiNilaiPenguji1();
					} else if (commonVO.getName()
							.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen5())) {
						nilai = mahasiswaRequestTugasAkhir.getNilaiDosen5();
						persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
								.getProsentasiNilaiPenguji2();
					} else if (commonVO.getName()
							.equals(mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getDosen6())) {
						nilai = mahasiswaRequestTugasAkhir.getNilaiDosen6();
						persen = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()
								.getProsentasiNilaiPenguji3();
					}
					parameter.put("nilai_dosen", nilai);
					parameter.put("persen_nilai_dosen", persen);
					parameter.put("tanggal_seminar", mahasiswaRequestTugasAkhir.getTanggalSeminar());
					parameter.put("waktu_seminar_mulai", mahasiswaRequestTugasAkhir.getWaktuSeminar());
					parameter.put("waktu_seminar_sampai", mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar());

					return parameter;
				}

				/**
				 * Handler tombol "Berita Acara": untuk setiap dosen penilai membangun satu baris
				 * parameter lewat {@code masukkanParameter(CommonVO)} di atas, mengumpulkan tanda
				 * tangan scan dosen/mahasiswa, melengkapi parameter global (judul, kaprodi, dekan,
				 * jadwal, total nilai, dsb.), lalu mencetak PDF "Berita_Acara_Proposal_Skripsi"
				 * lewat {@link Report#generatePDFReport}. Murni baca DB dan tulis file laporan
				 * sementara; tidak mengubah data penilaian.
				 *
				 * @param event event klik tombol, tidak dipakai isinya.
				 */
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void onEvent(Event event) throws Exception {
					List<CommonVO> dataDosen = mahasiswaRequestTugasAkhir.dataDosen(false);
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:1630");
						}
						indexTtd++;
					}

					try {
						LampiranLain lampiranLain = LampiranLain
								.ambil(mahasiswaRequestTugasAkhir.getMahasiswa().getId(), LampiranLain.TTD_MAHASISWA);
						if (lampiranLain != null) {
							parameter.put("ttd_mhs", lampiranLain.ambilFile().getAbsolutePath());
						} else {
							parameter.put("ttd_mhs", "");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianProposalSkripsiHelper.java:1644");
					}

					Common.insertProperty(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir, parameter,
							"bimbingan");
					parameter.put("jenjang", mahasiswaRequestTugasAkhir.getMahasiswa().getJenjang() == null ? ""
							: mahasiswaRequestTugasAkhir.getMahasiswa().getJenjang().getNama());
					parameter.put("nama_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getNama());
					parameter.put("nim_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getNim());
					parameter.put("angkatan_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getTahunangkatan());
					parameter.put("jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("nama_jurusan", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getNama());
					parameter.put("fakultas",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getNama());
					parameter.put("kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getNama());
					parameter.put("nidn_kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getNidn());
					parameter.put("nip_kaprodi",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getKaprodi().getCode());

					parameter.put("dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getNama());
					parameter.put("nidn_dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getNidn());
					parameter.put("nip_dekan",
							mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan() == null ? ""
									: mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getDekan()
											.getCode());

					parameter.put("judul", mahasiswaRequestTugasAkhir.getJudul());
					parameter.put("judul1", mahasiswaRequestTugasAkhir.getJudul1());
					parameter.put("judul2", mahasiswaRequestTugasAkhir.getJudul2());
					parameter.put("judul3", mahasiswaRequestTugasAkhir.getJudul3());
					parameter.put("judul4", mahasiswaRequestTugasAkhir.getJudul4());
					parameter.put("judul5", mahasiswaRequestTugasAkhir.getJudul5());
					parameter.put("judul6", mahasiswaRequestTugasAkhir.getJudul6());
					parameter.put("judul7", mahasiswaRequestTugasAkhir.getJudul7());
					parameter.put("judul8", mahasiswaRequestTugasAkhir.getJudul8());
					parameter.put("judul9", mahasiswaRequestTugasAkhir.getJudul9());
					parameter.put("judul10", mahasiswaRequestTugasAkhir.getJudul10());
					parameter.put("jenis_semester",
							mahasiswaRequestTugasAkhir.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
					parameter.put("tahun_ajaran", mahasiswaRequestTugasAkhir.getTahunAkademik());
					parameter.put("tanggal",
							Common.dateFormat2.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));
					parameter.put("hari_tanggal",
							Common.dateFormat6.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));
					parameter.put("tanggal_1",
							Common.dateFormat6.get()
									.format(mahasiswaRequestTugasAkhir.getTanggalSeminar() == null ? WaktuUtil.getDate()
											: mahasiswaRequestTugasAkhir.getTanggalSeminar()));

					parameter.put("waktu_mulai_seminar", mahasiswaRequestTugasAkhir.getWaktuSeminar());
					parameter.put("waktu_sampai_seminar", mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar());
					parameter.put("lulus", mahasiswaRequestTugasAkhir.getLulus());
					parameter.put("nilai_total", mahasiswaRequestTugasAkhir.getTotalNilai());
					parameter.put("nilai_huruf", mahasiswaRequestTugasAkhir.getNilaiHuruf());
					parameter.put("catatan_seminar", mahasiswaRequestTugasAkhir.getCatatanSeminar());

					parameter.put("tanggal_seminar", mahasiswaRequestTugasAkhir.getTanggalSeminar());
					parameter.put("waktu_seminar_mulai", mahasiswaRequestTugasAkhir.getWaktuSeminar());
					parameter.put("waktu_seminar_sampai", mahasiswaRequestTugasAkhir.getWaktuSampaiSeminar());

					parameter.put("maps", maps);

					Report.generatePDFReport(Report.PDF, parameter, "Berita_Acara_Proposal_Skripsi",
							ais.ui.util.WaktuUtil.getDate(), maps);
				}

			});
		}

		// Tombol "Refresh": muat ulang seluruh layar dari state mahasiswaRequestTugasAkhir saat ini
		// (tanpa query baru sendiri — hanya memanggil ulang display()).
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				display(mahasiswaRequestTugasAkhir, component);
			}
		});
		button.setParent(toolbar);

		// Tombol "Reset" (khusus admin, lihat Common.getApakahAdmin()): setelah konfirmasi,
		// menghapus permanen seluruh nilai dosen1-6, nilai huruf, total IP, total nilai, dan detail
		// nilai per komponen milik mahasiswaRequestTugasAkhir ini, lalu menyimpan dan me-render ulang
		// layar. Lihat Javadoc display(...) untuk detail efek samping destruktif ini.
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

										mahasiswaRequestTugasAkhir.setDetailNilai(null);

										mahasiswaRequestTugasAkhir.setNilaiDosen1(null);
										mahasiswaRequestTugasAkhir.setNilaiHuruf("");
										mahasiswaRequestTugasAkhir.setTotalIP(null);
										mahasiswaRequestTugasAkhir.setNilaiDosen2(null);
										mahasiswaRequestTugasAkhir.setNilaiDosen3(null);
										mahasiswaRequestTugasAkhir.setNilaiDosen4(null);
										mahasiswaRequestTugasAkhir.setNilaiDosen5(null);
										mahasiswaRequestTugasAkhir.setNilaiDosen6(null);
										mahasiswaRequestTugasAkhir.setTotalNilai(null);

										Common.refreshUpdate(mahasiswaRequestTugasAkhir);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												display(mahasiswaRequestTugasAkhir, component);
											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"me-reset semua nilai dosen untuk proposal skripsi ini",
												e,
												new String[] {
														"Periksa apakah data nilai proposal ini masih berelasi dengan data lain (misalnya data bimbingan atau kelulusan) sehingga tidak dapat direset.",
														"Muat ulang halaman kemudian ulangi proses reset nilai.",
														"Jika proses reset tetap gagal, konfirmasikan kebutuhan ini kepada Administrator." });
									}

								}

							}
						});

			}
		});
		button.setParent(toolbar);

		groupbox2 = new ais.ui.util.MyGroupboxStyled();
		groupbox2.setParent(centerBox);
		groupbox2.appendChild(new MyCaptionStyled("Nilai Seminar"));

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(groupbox2);

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
