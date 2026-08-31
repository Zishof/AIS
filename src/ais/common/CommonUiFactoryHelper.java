package ais.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.SkripsiAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.BukuBahanAjarHelper;
import ais.action.master.helper.PerkuliahanPunyaItemHelper;
import ais.action.master.helper.RekapitulasiAudioHelper;
import ais.action.master.helper.RekapitulasiMateriHelper;
import ais.action.master.helper.RekapitulasiTugasHelper;
import ais.action.master.helper.RekapitulasiUjianHelper;
import ais.action.master.helper.RekapitulasiVideoHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TugasKelompokHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.BukuBahanAjarMatapelajaranHelper;
import ais.action.master.sekolah.helper.JadwalPelajaranPunyaItemHelper;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaPerProdiDanAngkatan;
import ais.action.report.format1.akademik.LaporanMonitorJadwalPelajaran;
import ais.action.report.format1.sekolah.LaporanAlbumSiswa;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisKegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.ParameterUmum;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.PerkuliahanPunyaItem;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.VoKunci;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPelajaranPunyaItem;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.MatapelajaranPunyaBukuBahanAjar;
import ais.database.model.sop.DataSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyVboxStyled;
import ais.ui.util.MyWindow;



/**
 * Helper terfokus untuk common ui factory. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Common}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Logger log}, {@code String
 * COOKIE_PMB_BIODATA}, {@code String COOKIE_PMB_USERID}; pembacaan/pencarian ({@code tampilCrudError()}, {@code
 * getDeskripsiPerkuliahan()}, {@code getDeskripsiPerkuliahanHbox()}, {@code getDeskripsiPerkuliahanHbox()},
 * {@code getDeskripsiJadwalPelajaranHbox()}, {@code getDeskripsiJadwalPelajaranHbox()}); penghapusan/pembatalan
 * ({@code copyEditDeleteButtons()}, {@code copyEditDeleteButtons()}, {@code copyEditDeleteButtons()}); operasi
 * domain lain ({@code safeTrim()}, {@code isBlank()}, {@code ensureDirectory()}, {@code
 * merupakanConstraintReferensi()}, {@code createCleanButton()}, {@code createCleanButton()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 *
 * @see Common
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class CommonUiFactoryHelper extends Common {


	private static final Logger log = Logger.getLogger(CommonUiFactoryHelper.class);
	private static final String COOKIE_PMB_BIODATA = "biodataCalonMahasiswa";
	private static final String COOKIE_PMB_USERID = "userid";

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static boolean ensureDirectory(File directory) {
		if (directory == null) {
			return false;
		}
		if (directory.exists()) {
			return directory.isDirectory();
		}
		return directory.mkdirs();
	}

	private static void tampilCrudError(Exception e, String pesan) {
		Common.tampilErrorJikaAdmin(e);
		String detail = e == null || e.getMessage() == null ? "" : "\n" + e.getMessage();
		try {
			MyMessageboxConfig.show(pesan + detail);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonUiFactoryHelper.java:141");
		}
	}

	private static boolean merupakanConstraintReferensi(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof org.hibernate.exception.ConstraintViolationException) return true;
			String nama = current.getClass().getName();
			String pesan = current.getMessage();
			if ((nama != null && nama.indexOf("ConstraintViolation") >= 0)
					|| (pesan != null && (pesan.indexOf("violates foreign key constraint") >= 0
							|| pesan.indexOf("still referenced") >= 0))) return true;
			current = current.getCause();
		}
		return false;
	}




	public static String getDeskripsiPerkuliahan(Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return "";
		}

		String matkul1 = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();

		String semester1 = perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester().toString();

		if (perkuliahan.getStatusSemesterPendek() != null
				&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) {
			semester1 = semester1 + " (" + Perkuliahan.SP + ")";
		}

		Integer sks = perkuliahan.getMatakuliah() == null ? 0 : perkuliahan.getMatakuliah().getSks();

		String kelas1 = perkuliahan.getKelas();

		String dosen1 = perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama();
		String dosen2 = perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama();

		String ruang = perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama();

		String harijam = (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() == null ? false
				: perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) ? ""
						: (", Hari: " + perkuliahan.getHari() + ", " + perkuliahan.getWaktuMulai() + " s.d "
								+ perkuliahan.getWaktuSelesai());

		String groupTxt = "Matakuliah: " + matkul1 + " (" + sks + " SKS), Semester: " + semester1 + " " + kelas1
				+ (dosen1.equals("") ? "" : ", Dosen: " + dosen1) + (dosen2.equals("") ? "" : ", Dosen 2: " + dosen2)
				+ (ruang.equals("") ? "" : ", Ruang: " + ruang) + harijam + ", Program: " + perkuliahan.getProgram()
				+ " (" + perkuliahan.getTahunAjaran() + ")";

		return groupTxt;
	}



	public static Box getDeskripsiPerkuliahanHbox(Perkuliahan perkuliahan) throws Exception {
		return getDeskripsiPerkuliahanHbox(perkuliahan, true);
	}



	public static Box getDeskripsiPerkuliahanHbox(Perkuliahan perkuliahan, boolean tampilStatistik) throws Exception {
		return getDeskripsiPerkuliahanHbox(perkuliahan, tampilStatistik, true, null, null, false);
	}



	public static Box getDeskripsiJadwalPelajaranHbox(JadwalPelajaran jadwalPelajaran) throws Exception {
		return getDeskripsiJadwalPelajaranHbox(jadwalPelajaran, true);
	}



	public static Box getDeskripsiJadwalPelajaranHbox(JadwalPelajaran jadwalPelajaran, boolean tampilStatistik)
			throws Exception {
		return getDeskripsiJadwalPelajaranHbox(jadwalPelajaran, tampilStatistik, false, null);
	}



	


public static Box getDeskripsiJadwalPelajaranHbox(final JadwalPelajaran jadwalPelajaran,
			final boolean tampilStatistik, final boolean horizontal, final Row rowData) throws Exception {
		final Box hbox;

		if (Common.isMobile()) {
			hbox = new Vbox();
		} else {
			hbox = new Hbox();
		}
		if (rowData != null) {
			rowData.appendChild(hbox);
		}
		Common.displayGuruJadwalPelajaranUmum(hbox, jadwalPelajaran, true, true, null, 2);

		Integer semester1 = jadwalPelajaran.getSemester() == null ? 0 : jadwalPelajaran.getSemester();

		String kelas1 = jadwalPelajaran.getKelas() == null ? "" : jadwalPelajaran.getKelas().getNama();

		if (jadwalPelajaran.getKelasLesSiswa() != null) {
			kelas1 = jadwalPelajaran.getKelasLesSiswa().getNama();
		}

		String matkul1 = jadwalPelajaran.getMatapelajaran() == null ? "" : jadwalPelajaran.getMatapelajaran().getNama();

		String ksl = (semester1 < 1 ? "" : semester1) + "";

		try {
			ksl = (kelas1.equals("") ? "" : " " + kelas1) + " (" + jadwalPelajaran.getSekolah().getNama() + ")";
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonUiFactoryHelper.java:242");

		}

		Vbox vboxAtas = new MyVboxStyled();
		vboxAtas.setParent(rowData == null ? hbox : rowData);
		if (rowData != null) {
			vboxAtas.setWidth("100%");
		}

		Label sub1 = new Label(matkul1);
		sub1.setStyle("padding-top:10px;font-size:14px;font-weight:bold;color:black");
		sub1.setParent(vboxAtas);

		Hbox myHbox1 = new Hbox();
		myHbox1.setParent(vboxAtas);

		if (!ksl.equals("")) {
			sub1 = new Label(ksl);
			sub1.setStyle("font-size:12px;font-weight:bold;color:black");
			sub1.setParent(myHbox1);
		}

		try {
			Common.displayHariJamRuanganJadwalPelajaranUmum(vboxAtas, jadwalPelajaran);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonUiFactoryHelper.java:267");

		}

		if (jadwalPelajaran.getKurikulumPunyaMatapelajaran() != null
				&& jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah() != null) {
			RevisiHelper
					.createNewRevisi(JadwalPelajaran.class, jadwalPelajaran,
							"Kurikulum:"
									+ jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah().getNama())
					.setParent(vboxAtas);
		}

		hbox.appendChild(new Space());
		hbox.appendChild(new Space());

		final Box groupboxStyled = Common.isMobile() ? new MyVboxStyled() : new MyHboxStyled();
		groupboxStyled.setParent(rowData == null ? hbox : rowData);
		if (rowData != null) {
			groupboxStyled.setWidth("100%");
		}

		final Vbox vboxH = new Vbox();
		if (horizontal) {
			vboxH.setParent(groupboxStyled);
		}

		final Vbox vbox1 = new Vbox();
		vbox1.setWidth("90px");
		if (!horizontal) {
			vbox1.setParent(groupboxStyled);
		}
		hbox.appendChild(new Space());
		final Vbox vbox2 = new Vbox();
		vbox2.setWidth("120px");
		if (!horizontal) {
			vbox2.setParent(groupboxStyled);
		}
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// Session session = HibernateUtil.currentSession();

				Object[] jml = jadwalPelajaran.ambilJumlahPertemuanStatistik(true, true);
				// Collection<Long> mhsIds = jadwalPelajaran.ambilMahasiswaId();
				int mhsSize = jadwalPelajaran.ambilSiswaById().size();

				Collection<Pertemuan> pertemuans = (Collection<Pertemuan>) (jml == null || jml[7] == null
						? new ArrayList<Pertemuan>()
						: jml[7]);

				int jumlahUjianTotal = jml == null || jml[8] == null ? 0 : Integer.parseInt(jml[8].toString());
				int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0 : Integer.parseInt(jml[9].toString());

				Toolbarbutton sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Ujian"),
						"/img/svg/check2-all.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (jumlahUjianTotal == 0 ? "black" : "red"));

				Vbox label = new Vbox();
				label.appendChild(sub1);

				Label sub2 = new Label(Common.numberFormat.get().format(jumlahUjianTotal) + " ujian");
				sub2.setStyle("font-size:8px;color:" + (jumlahUjianTotal == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				Hbox hboxD = new Hbox();
				if (horizontal) {
					vboxH.appendChild(hboxD);
				}

				if (!horizontal) {
					label.setParent(vbox1);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Ujian", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						RekapitulasiUjianHelper.display(center, getCurrentUser(), jadwalPelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Diskusi"), "/img/svg/user-group.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (jumlahDiskusiTotal == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(jumlahDiskusiTotal) + " percakapan");
				sub2.setStyle("font-size:8px;color:" + (jumlahDiskusiTotal == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox1);
				} else {
					hboxD.appendChild(label);
				}

				final String[] contents = new String[] { "isi", "siswa.nama", "tbmuser", "parent.isi" };
				List<String> columnHeadersAdding = new ArrayList<String>();
				columnHeadersAdding.add("Lampiran");
				MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(PertemuanPunyaDiskusi.class,
						new DataCriteria() {

							@Override
							public Criteria initCriteria(boolean order) {
								// TODO Auto-generated method stub
								return HibernateUtil.currentSession().createCriteria(PertemuanPunyaDiskusi.class)
										.createAlias("pertemuan", "pertemuan").addOrder(Order.asc("id"))
										.add(Restrictions.eq("pertemuan.jadwalPelajaran", jadwalPelajaran));
							}
						}, "Download", FileFoto.icon(null), columnHeadersAdding, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Object[] objects = (Object[]) arg0.getData();
								PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) objects[0];
								XSSFRow row = (XSSFRow) objects[2];

								final XSSFCellStyle hlink_style = (XSSFCellStyle) objects[6];

								class DataAddingHelper {
									public void process(XSSFRow row, int index,
											PertemuanPunyaDiskusi pertemuanPunyaDiskusi) throws Exception {

										LampiranLain lam = LampiranLain.ambil(pertemuanPunyaDiskusi.getId(),
												LampiranLain.DISKUSI);

										XSSFCell cell = row.createCell(index);

										if (lam != null) {

											String nama = lam.getNama();

											cell.setCellStyle(hlink_style);
											cell.setCellValue(nama);
											String url = lam.createLinkUri();
											XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
													.createHyperlink(Hyperlink.LINK_URL);
											link.setAddress(url);
											cell.setHyperlink(link);
										}

									}
								}
								DataAddingHelper dataAddingHelper = new DataAddingHelper();
								dataAddingHelper.process(row, contents.length, pertemuanPunyaDiskusi);
							}
						}, false, null, "", contents);

				sub1.addEventListener("onClick", (EventListener) cetakToolbarbutton.getAttribute("eventListener"));

				int pertemuan_file_content = 0;
				int tugas_file_content = 0;
				int tugas_kelompok = 0;
				int audio_pertemuan = 0;
				int video_pertemuan = 0;
				for (Pertemuan ids : pertemuans) {
					TreeMap<Long, TugasPertemuan> tugases = ids.ambilTugasPertemuanTotal();
					TreeMap<Long, TugasKelompok> tugasesKelompok = ids.ambilTugasKelompokTotal();

					tugas_file_content += ids.getJudultugas().trim().isEmpty() ? 0 : 1;

					for (TugasPertemuan tugasPertemuan : tugases.values()) {
						tugas_file_content += tugasPertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
					}

					for (TugasKelompok tugasPertemuan : tugasesKelompok.values()) {
						tugas_kelompok += tugasPertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
					}

					tugases = null;
					tugasesKelompok = null;

					pertemuan_file_content += ids.ambilJumlahPertemuanFileContent();
					audio_pertemuan += ids.ambilJumlahAudioPertemuan();
					video_pertemuan += ids.ambilJumlahVideoPertemuan();

				}

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Materi"), "/img/svg/file-lines.svg");
				sub1.setStyle(
						"font-size:9px;font-weight:bold;color:" + (pertemuan_file_content == 0 ? "black" : "red"));
				sub1.setOrient("vertical");

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(pertemuan_file_content) + " file");
				sub2.setStyle("font-size:8px;color:" + (pertemuan_file_content == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox1);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Materi", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						RekapitulasiMateriHelper.display(center, getCurrentUser(), jadwalPelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Tugas"), "/img/svg/task-line.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (tugas_file_content == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(tugas_file_content) + " tgs");
				sub2.setStyle("font-size:8px;color:" + (tugas_file_content == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox1);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Tugas", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						RekapitulasiTugasHelper.display(center, getCurrentUser(), jadwalPelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Audio"), "/img/svg/file-audio-thin.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (audio_pertemuan == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(audio_pertemuan) + " file");
				sub2.setStyle("font-size:8px;color:" + (audio_pertemuan == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox1);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Audio", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						RekapitulasiAudioHelper.display(center, getCurrentUser(), jadwalPelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Video"), "/img/svg/camera-video.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (video_pertemuan == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(video_pertemuan) + " file");
				sub2.setStyle("font-size:8px;color:" + (video_pertemuan == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox1);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Video", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						RekapitulasiVideoHelper.display(center, getCurrentUser(), jadwalPelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				hboxD = new Hbox();
				if (horizontal) {
					vboxH.appendChild(hboxD);
				}

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Siswa"), "/img/svg/user-box-line.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (mhsSize == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(mhsSize) + " anak");
				sub2.setStyle("font-size:8px;color:" + (mhsSize == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox2);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LaporanAlbumSiswa window = new LaporanAlbumSiswa(jadwalPelajaran.getKelas());
						window.setTitle("Daftar Siswa " + jadwalPelajaran.infoSimple());
						window.setClosable(true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");
						window.onModal();
					}
				});

				int pert = pertemuans.size();
				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Pertemuan"), "/img/svg/list-check.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (pert == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(pert) + " agenda");
				sub2.setStyle("font-size:8px;color:" + (pert == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox2);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						LaporanMonitorJadwalPelajaran laporanMonitorPerkuliahan = new LaporanMonitorJadwalPelajaran(
								jadwalPelajaran);
						laporanMonitorPerkuliahan
								.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						laporanMonitorPerkuliahan.setTitle("Agenda Pertemuan " + jadwalPelajaran.infoSimple());
						laporanMonitorPerkuliahan.setBorder("none");
						laporanMonitorPerkuliahan.setClosable(true);
						laporanMonitorPerkuliahan.setHeight("95%");
						laporanMonitorPerkuliahan.setWidth("95%");

						laporanMonitorPerkuliahan.setVisible(true);
						laporanMonitorPerkuliahan.onModal();
					}
				});

				Session session = HibernateUtil.currentSession();
				int referensi = ((Number) session.createCriteria(JadwalPelajaranPunyaItem.class)
						.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();
				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Buku Referensi"),
						"/img/svg/books-thin.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (referensi == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(referensi) + " ref");
				sub2.setStyle("font-size:8px;color:" + (referensi == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox2);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Buku Referensi", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
						borderlayout1.setParent(center);

						Center center1 = new Center();
						ais.ui.util.ZkCompat.setFlex(center1, true);
						center1.setParent(borderlayout1);

						JadwalPelajaranPunyaItemHelper data = new JadwalPelajaranPunyaItemHelper();
						data.display(jadwalPelajaran, center1);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				int bukuAjar = ((Number) session.createCriteria(MatapelajaranPunyaBukuBahanAjar.class)
						.add(Restrictions.eq("matapelajaran", jadwalPelajaran.getMatapelajaran()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Buku Ajar"),
						"/img/svg/check2-circle.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (bukuAjar == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(bukuAjar) + " bundel");
				sub2.setStyle("font-size:8px;color:" + (bukuAjar == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox2);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Buku Ajar", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						BukuBahanAjarMatapelajaranHelper data = new BukuBahanAjarMatapelajaranHelper();
						data.display(jadwalPelajaran.getMatapelajaran(), center, jadwalPelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Tugas Kelompok"),
						"/img/svg/user-list-thin.svg");
				sub1.setOrient("vertical");
				sub1.setStyle("font-size:10px;font-weight:bold;color:" + (tugas_kelompok == 0 ? "black" : "red"));

				label = new Vbox();
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(tugas_kelompok) + " tgs");
				sub2.setStyle("font-size:8px;color:" + (tugas_kelompok == 0 ? "blue" : "red"));
				label.appendChild(sub2);

				if (!horizontal) {
					label.setParent(vbox2);
				} else {
					hboxD.appendChild(label);
				}

				sub1.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final MyWindow window = new MyWindow("Tugas Kelompok", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						Tbmuser tbmuser = Common.getCurrentUser();

						TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(
								tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa());
						tugasKelompokHelper.display(jadwalPelajaran, center);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}

						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				});

				if (tampilStatistik) {
					hbox.appendChild(new Space());
					hbox.appendChild(new Space());
					TampilanELearningAction.tampilkanStatistik(jadwalPelajaran, jml, mhsSize, "",
							rowData == null ? hbox : rowData);
				}
			}
		});

		return hbox;
	}


public static ParameterUmum getParameterUmum(String nama, String defaultValue) {
		return getParameterUmum(nama, defaultValue, "", "", "");
	}



	public static ParameterUmum getParameterUmum(String nama, String defaultValue, String info1, String info2,
			String info3) {

		ParameterUmum parameterUmum = new ParameterUmum();

		try {
			Session session = HibernateUtil.currentNativeSession();
			parameterUmum = (ParameterUmum) session.createCriteria(ParameterUmum.class)
					.add(Restrictions.eq("nama", nama)).setMaxResults(1).uniqueResult();
			if (parameterUmum == null) {
				session.getTransaction().begin();
				parameterUmum = new ParameterUmum();
				parameterUmum.setNama(nama);
				parameterUmum.setNilai(defaultValue);
				parameterUmum.setInfo1(info1);
				parameterUmum.setInfo2(info2);
				parameterUmum.setInfo3(info3);
				Common.refreshSaveOrUpdate(session, (parameterUmum));
				session.getTransaction().commit();
			}

			HibernateUtil.closeSession();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
		}

		return parameterUmum;
	}



	public static JenisKegiatan getJenisKegiatan(String namaKegiatan) {
		if (CommonHelperClass.jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}
		for (JenisKegiatan jenisKegiatan : CommonHelperClass.jenisKegiatansAktif) {
			if (namaKegiatan != null && jenisKegiatan.getNamaKegiatan() != null
					&& jenisKegiatan.getNamaKegiatan().equalsIgnoreCase(namaKegiatan)) {
				return jenisKegiatan;
			}
		}
		return null;
	}



	public static JenisKegiatan getJenisKodeKegiatan(String kodeJenisKegiatan) {
		if (CommonHelperClass.jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}
		for (JenisKegiatan jenisKegiatan : CommonHelperClass.jenisKegiatansAktif) {
			if (kodeJenisKegiatan != null && jenisKegiatan.getKode() != null
					&& jenisKegiatan.getKode().equalsIgnoreCase(kodeJenisKegiatan)) {
				return jenisKegiatan;
			}
		}
		return null;
	}



	public static MyToolbarbuttonConfig createCleanButton(final Bandbox bandbox, final GetEventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Bersihkan", "/img/clear_16.png");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event event) throws Exception {

				bandbox.setValue("");
				Map map = bandbox.getAttributes();
				map.clear();
				bandbox.setOpen(false);

				Thread.sleep(1000);
				if (eventListener.getEventListener() != null) {
					eventListener.getEventListener().onEvent(event);
				}
				bandbox.setValue("");
			}
		});
		return button;
	}



	public static MyToolbarbuttonConfig createCleanButton(final Bandbox bandbox, final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Bersihkan", "/img/clear_16.png");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event event) throws Exception {

				bandbox.setValue("");
				Map map = bandbox.getAttributes();
				map.clear();
				bandbox.setOpen(false);

				Thread.sleep(1000);
				if (eventListener != null) {
					eventListener.onEvent(event);
				}
				bandbox.setValue("");
			}
		});
		return button;
	}



	public static MyWindow displayWindow(String src, boolean tampilToolbar) throws Exception {
		return displayWindow(src, tampilToolbar, "95%", "95%");
	}



	public static MyWindow displayWindow(String src, boolean tampilToolbar, String lebar) throws Exception {
		return displayWindow(src, tampilToolbar, "95%", lebar);
	}



	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar)
			throws Exception {
		return displayWindow(src, tampilToolbar, tinggi, lebar, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Default empty listener
			}
		});
	}



	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener) throws Exception {
		return displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, "Tampilan Data");
	}



	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener, String judul) throws Exception {
		return displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, judul, true);
	}



	public static MyWindow displayWindow(String src, boolean tampilToolbar, String tinggi, String lebar,
			final EventListener eventListener, String judul, boolean scroll) throws Exception {
		return WindowViewerHelper.displayWindow(src, tampilToolbar, tinggi, lebar, eventListener, judul, scroll);
	}



	public static MyWindow displayWindow(Boolean image, String src, Boolean tampilToolbar, String lebar, String tinggi,
			Boolean iframe, FileFoto ff) throws Exception {
		return WindowViewerHelper.displayWindow(image, src, tampilToolbar, lebar, tinggi, iframe, ff);
	}



	public static MyWindow displayWindowIframe(String src, Boolean tampilToolbar, String lebar, String tinggi)
			throws Exception {
		return displayWindowIframe(src, tampilToolbar, lebar, tinggi, "Tampilan Data");
	}



	public static MyWindow displayWindowIframe(String src, Boolean tampilToolbar, String lebar, String tinggi,
			String judul) throws Exception {
		return WindowViewerHelper.displayWindowIframe(src, tampilToolbar, lebar, tinggi, judul);
	}



	public static void tampilkanTugasAkhir() throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswa == null) {
			MyMessageboxConfig.show("Anda harus login sebagai mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) session
				.createCriteria(MahasiswaRequestTugasAkhir.class)
				.add(Restrictions.ne("status", MahasiswaRequestTugasAkhir.GAGAL_STATUS))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
		if (mahasiswaRequestTugasAkhir == null) {
			mahasiswaRequestTugasAkhir = new MahasiswaRequestTugasAkhir();
		}
		mahasiswaRequestTugasAkhir.setMahasiswa(mahasiswa);

		MahasiswaRequestTugasAkhirAction.onAddExternal(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) arg0.getData();
				Mahasiswa mahasiswa = mahasiswaRequestTugasAkhir.getMahasiswa();
				MyMessageboxConfig.show(
						"Mahasiswa dengan NIM " + mahasiswa.getNim() + " nama " + mahasiswa.getNama()
								+ " telah berhasil melakukan permohonan sidang tugas akhir / skripsi dengan judul:\n\n"
								+ mahasiswaRequestTugasAkhir.getJudul(),
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

			}
		}, mahasiswaRequestTugasAkhir);

	}



	public static void tampilkanDaftarSkripsi() throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		if (mahasiswa == null) {
			MyMessageboxConfig.show("Anda harus login sebagai mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.setMaxResults(1).uniqueResult();
		if (skripsi == null) {
			skripsi = new Skripsi();
		}

		SkripsiAction.onAddExternal(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Skripsi skripsi = (Skripsi) arg0.getData();
				Mahasiswa mahasiswa = skripsi.getMahasiswa();
				MyMessageboxConfig.show(
						"Mahasiswa dengan NIM " + mahasiswa.getNim() + " nama " + mahasiswa.getNama()
								+ " telah berhasil melakukan pendaftaran dengan judul:\n\n" + skripsi.getJudul(),
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

			}
		}, skripsi, mahasiswa);

	}



	public static String tampilanSocialLogin() {
		HttpServletRequest servletRequest = null;
		HttpServletResponse servletResponse = null;
//		try {
//			servletRequest = RequestAndResponseContextHolder.request();
//			servletResponse = RequestAndResponseContextHolder.response();
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonUiFactoryHelper.java:1265");
//			tampilErrorJikaAdmin(e);
//		}

		return tampilanSocialLogin(servletRequest, servletResponse);
	}



	public static String tampilanSocialLogin3(HttpServletRequest req, HttpServletResponse res) {
		ConstantValues.init();
		String html = "";

		// 1. Pengecekan Remember Me
		if (ConstantValues.aktifkanRememeberMe) {
			String rememberHtml = Common.remember(req, res);

			if (rememberHtml != null && !rememberHtml.isEmpty()) {
				// Jika remember me berhasil (mengandung script redirect bawaan),
				// cegat dan ganti dengan tampilan SweetAlert2 yang lebih modern
				if (rememberHtml.contains("location.replace")) {
					String host = (req.isSecure() ? "https://" : "http://") + req.getServerName()
							+ (req.getServerPort() == 80 || req.getServerPort() == 443 ? "" : ":" + req.getServerPort())
							+ req.getContextPath();

					html += "<script>\n" + "document.addEventListener('DOMContentLoaded', function() {\n"
							+ "    Swal.fire({\n" + "        title: '" + Common.getBahasaConfig("Sesi Ditemukan")
							+ "',\n" + "        html: '" + Common.getBahasaConfig("Mengarahkan ke halaman utama...")
							+ "',\n" + "        allowOutsideClick: false,\n" + "        allowEscapeKey: false,\n"
							+ "        showConfirmButton: false,\n" + "        didOpen: () => { Swal.showLoading(); }\n"
							+ "    });\n" + "    setTimeout(function(){ location.replace('" + host
							+ "/main'); }, 1500);\n" + "});\n" + "</script>";
					return html; // Langsung kembalikan script redirect, hentikan render form
				} else {
					html += rememberHtml;
				}
			}
		}

		// 2. Jika tidak ada sesi auto-login, render form untuk AJAX
		if (html.isEmpty()) {
			String username = null;
			String password = null;

			try {
				username = Common.getKonfigurasi("username_default_demo_" + req.getServerName(), "").getNilai();
				password = Common.getKonfigurasi("password_default_demo_" + req.getServerName(), "").getNilai();
			} catch (Exception e) {
				tampilErrorJikaAdmin(e);
			}

			html += "<!--begin::Input group=-->\r\n" + "<div class=\"fv-row mb-8\">\r\n" + "    <!--begin::Email-->\r\n"
					+ "    <input type=\"text\" id=\"username\" "
					+ (username == null || username.trim().isEmpty() ? "" : "value=\"" + username + "\"")
					+ "    placeholder=\"" + Common.getBahasaConfig("ID Pengguna") + "\" name=\"j_username\"\r\n"
					+ "        autocomplete=\"off\" class=\"form-control bg-transparent\" />\r\n"
					+ "    <!--end::Email-->\r\n" + "</div>\r\n" + "<!--end::Input group=-->\r\n"

					+ "<div class=\"fv-row mb-3\">\r\n" + "    <!--begin::Password-->\r\n"
					+ "    <input type=\"password\" id=\"password\" "
					+ (password == null || password.trim().isEmpty() ? "" : "value=\"" + password + "\"")
					+ "    placeholder=\"" + Common.getBahasaConfig("Kata Sandi") + "\" name=\"j_password\"\r\n"
					+ "        autocomplete=\"off\" class=\"form-control bg-transparent\" />\r\n"
					+ "    <!--end::Password-->\r\n" + "</div>\r\n" + "<!--end::Input group=-->\r\n"

					+ "<!--begin::Wrapper-->\r\n" + "<div class=\"row g-3 mb-8\">\r\n" + "    <!--begin::Col-->\r\n"
					+ "    <div class=\"col-8\">\r\n";

			if (ConstantValues.aktifkanRememeberMe) {
				html += "        <!--begin::Accept-->\r\n" + "        <div class=\"fv-row\">\r\n"
						+ "            <label class=\"form-check form-check-inline\"> \r\n"
						+ "                <input id=\"checkbox\" value=\"true\" name=\"rememberMe\"\r\n"
						+ (ConstantValues.aktifkanRememeberMeOtomatisTerpilih ? "checked" : "")
						+ "                class=\"form-check-input\" type=\"checkbox\" /> \r\n"
						+ "                <span class=\"form-check-label fw-semibold text-gray-700 fs-base ms-1\">"
						+ Common.getBahasaConfig("Ingat akun saya") + "</span>\r\n" + "            </label>\r\n"
						+ "        </div>\r\n" + "        <!--end::Accept-->\r\n";
			}

			html += "    </div>\r\n" + "    <!--end::Col-->\r\n" + "    <!--begin::Col-->\r\n"
					+ "    <div class=\"col-4\">\r\n" + "    </div>\r\n" + "    <!--end::Col-->\r\n" + "</div>\r\n"
					+ "<!--end::Wrapper-->\r\n"

					+ "<!--begin::Submit button-->\r\n" + "<div class=\"d-grid mb-10\">\r\n"
					+ "    <button type=\"submit\" id=\"kt_sign_in_submit\" class=\"btn btn-primary\">\r\n"
					+ "        <!--begin::Indicator label-->\r\n" + "        <span class=\"indicator-label\">"
					+ Common.getBahasaConfig("Masuk") + "</span>\r\n" + "        <!--end::Indicator label-->\r\n"
					+ "        <!--begin::Indicator progress-->\r\n" + "        <span class=\"indicator-progress\">"
					+ Common.getBahasaConfig("Mohon tunggu...") + " \r\n"
					+ "            <span class=\"spinner-border spinner-border-sm align-middle ms-2\"></span>\r\n"
					+ "        </span>\r\n" + "        <!--end::Indicator progress-->\r\n" + "    </button>\r\n"
					+ "</div>\r\n" + "<!--end::Submit button-->";
		}

		return html;
	}



	public static String tampilanSocialLogin(HttpServletRequest req, HttpServletResponse res) {
		ConstantValues.init();
		String html = "";
		if (ConstantValues.aktifkanRememeberMe) {
			html += Common.remember(req, res);
		}

		if (!ais.common.ConstantValues.aktifkanLoginHanyaViaMediaSocial) {

			String login_remember_css = Common.getKonfigurasi("login_remember_css", "").getNilai();

			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(req);

			html += "<p style=\"text-align: center;" + login_remember_css + "\">\n                Username<br>\n"
					+ "                <input type='text' class=\"textinput\" name='j_username'\n"
					+ "                       value='"
					+ (perguruanTinggi != null && perguruanTinggi.getPendaftar() != null ? ""
							: ConstantValues.aktifkan_akun_demo ? "demo" : "")
					+ "' />\n            </p>\n\n" + "            <p style=\"text-align: center;" + login_remember_css
					+ "\">\n                Password<br>\n"
					+ "                <input type='password' class=\"textinput\" name='j_password' "
					+ (perguruanTinggi != null && perguruanTinggi.getPendaftar() != null ? ""
							: ConstantValues.aktifkan_akun_demo ? "value='demo123'" : "")
					+ ">\n" + "            </p>\n            <p style=\"display: none;" + login_remember_css + "\">\n"
					+ "                <label>&nbsp;</label> <input type=\"checkbox\" class=\"checkbox\"\n"
					+ "                                             name=\"_spring_security_remember_me\">Biarkan Saya Tetap\n"
					+ "                Masuk\n            </p>\n";

			if (ConstantValues.aktifkanRecapcha) {
				html += "<p style=\"text-align: center;\">";
				html += "<script src=\"https://www.google.com/recaptcha/api.js?hl=id\"></script>";
				html += "<div style=\"display: inline-block;\" class=\"g-recaptcha\" data-sitekey=\""
						+ ConstantValues.recapchaClientKey + "\"></div>";
				html += "</p>";
			}

			if (ConstantValues.aktifkanCaptchaLokal) {
				html += "<p style=\"text-align: center;\">\n<img src=\"capcha\" /><br>\n"
						+ "                <input style=\"text-align: center;\" placeholder='Masukkan teks di atas' type='text' class=\"textinput\" name='answer'\n"
						+ "                       value='' />\n            </p>\n\n";
			}

			if (ConstantValues.aktifkanRememeberMe) {
				if (req != null && res != null) {
					html += "<input type=\"checkbox\" id='rememberMe' "
							+ (ConstantValues.aktifkanRememeberMeOtomatisTerpilih ? "checked" : "")
							+ " name=\"rememberMe\" value=\"true\"><label style='font-size: 9px;" + login_remember_css
							+ "' id='labelRememberMe' for=\"rememberMe\">Ingat akun selama menggunakan browser ini, sehingga Anda tidak perlu login ulang, kecuali logout dan pilih lupakan Akun.</label>";
				}
			}

			html += "            <p style=\"text-align: center;\">\n"
					+ "                <input type=\"submit\" value=\"login\" style='"
					+ Common.getKonfigurasi("login_button_css", "").getNilai()
					+ "' class=\"btnlogin\" id='loginButton'>\n" + "            </p>";

		}

		html += "<p style=\"text-align: center;\">";
		if (ais.common.ConstantValues.aktifkanIntegrasiFacebook) {
			html += "<a style=\"color:white;\" target=\"_parent\" href=\"facebook.zul\"><img width='150px' height='32px' src=\"img/fblogin.png\"/></a>";
		}
		if (ais.common.ConstantValues.aktifkanIntegrasiGoogle) {
			html += "<a style=\"color:white;\" target=\"_parent\" href=\"google.zul\"><img width='150px'  height='32px'  src=\"img/sign_in_google.png\"/></a>";
		}
		html += "</p>";
		html += "<p style=\"text-align: center;\">";
		if (ais.common.ConstantValues.aktifkanIntegrasiTwitter) {
			html += "<a style=\"color:white;\" target=\"_parent\" href=\"twitter.zul\"><img width='150px'  height='32px' src=\"img/twitter.png\"/></a>";
		}
		if (ais.common.ConstantValues.aktifkanIntegrasiLinkedin) {
			html += "<a style=\"color:white;\" target=\"_parent\" href=\"linkedin.zul\"><img width='150px'  height='32px' src=\"img/linkedin.png\"/></a>";
		}

		html += "</p>";
		return html;
	}



	public static String tampilanSocialLogin2(HttpServletRequest req, HttpServletResponse res) {
		ConstantValues.init();
		String html = "";
		if (ConstantValues.aktifkanRememeberMe) {
			html += Common.remember(req, res);
		}

		if (html.isEmpty()) {
			String username = null;
			String password = null;

			try {
				username = Common.getKonfigurasi("username_default_demo_" + req.getServerName(), "").getNilai();
				password = Common.getKonfigurasi("password_default_demo_" + req.getServerName(), "").getNilai();
			} catch (Exception e) {
				tampilErrorJikaAdmin(e);
			}

			html += "<!--begin::Input group=-->\r\n" + "							<div class=\"fv-row mb-8\">\r\n"
					+ "								<!--begin::Email-->\r\n"
					+ "								<input type=\"text\" "
					+ (username == null || username.trim().isEmpty() ? "" : "value=\"" + username + "\"")
					+ "  placeholder=\"ID Pengguna\" name=\"j_username\"\r\n"
					+ "									autocomplete=\"off\" class=\"form-control bg-transparent\" />\r\n"
					+ "								<!--end::Email-->\r\n" + "							</div>\r\n"
					+ "							<!--end::Input group=-->\r\n"
					+ "							<div class=\"fv-row mb-3\">\r\n"
					+ "								<!--begin::Password-->\r\n"
					+ "								<input type=\"password\" "
					+ (password == null || password.trim().isEmpty() ? "" : "value=\"" + password + "\"")
					+ " placeholder=\"Password\" name=\"j_password\"\r\n"
					+ "									autocomplete=\"off\" class=\"form-control bg-transparent\" />\r\n"
					+ "								<!--end::Password-->\r\n" + "							</div>\r\n"
					+ "							<!--end::Input group=-->\r\n" + "							\r\n"
					+ "\r\n" + "							<!--begin::Wrapper-->\r\n"

					+ "							<div class=\"row g-3 mb-8\">\r\n"
					+ "								<!--begin::Col-->\r\n"
					+ "								<div class=\"col-8\">\r\n";
			if (ConstantValues.aktifkanRememeberMe) {
				html += "									<!--begin::Accept-->\r\n"
						+ "									<div class=\"fv-row\">\r\n"
						+ "										<label class=\"form-check form-check-inline\"> <input\r\n"
						+ "											id=\"checkbox\" value=\"true\" name=\"rememberMe\"\r\n"
						+

						(ConstantValues.aktifkanRememeberMeOtomatisTerpilih ? "checked" : "")

						+ "											class=\"form-check-input\" type=\"checkbox\" name=\"toc\" /> <span\r\n"
						+ "											class=\"form-check-label fw-semibold text-gray-700 fs-base ms-1\">"
						+ Common.getBahasaConfig("Ingat akun saya di browser ini") + "</span>\r\n"
						+ "										</label>\r\n"
						+ "									</div>\r\n"
						+ "									<!--end::Accept-->\r\n";
			}
			html += "								</div>\r\n" + "								<!--end::Col-->\r\n"
					+ "								<!--begin::Col-->\r\n"
					+ "								<div class=\"col-4\">\r\n"
					+ "								</div>\r\n" + "								<!--end::Col-->\r\n"
					+ "							</div>\r\n" + "							<!--end::Wrapper-->\r\n"
					+ "\r\n" + "							<!--begin::Submit button-->\r\n"
					+ "							<div class=\"d-grid mb-10\">\r\n"
					+ "								<button type=\"submit\" id=\"kt_sign_in_submit\"\r\n"
					+ "									class=\"btn btn-primary\">\r\n"
					+ "									<!--begin::Indicator label-->\r\n"
					+ "									<span class=\"indicator-label\">Sign In</span>\r\n"
					+ "									<!--end::Indicator label-->\r\n"
					+ "									<!--begin::Indicator progress-->\r\n"
					+ "									<span class=\"indicator-progress\">Please wait... <span\r\n"
					+ "										class=\"spinner-border spinner-border-sm align-middle ms-2\"></span></span>\r\n"
					+ "									<!--end::Indicator progress-->\r\n"
					+ "								</button>\r\n" + "							</div>\r\n"
					+ "							<!--end::Submit button-->";
		}
		return html;
	}



	public static Hbox copyEditDeleteButtons(boolean edit, boolean delete, GeneralValueObject obj,
			DataInitDefault dataInitDefault) {
		return copyEditDeleteButtons(edit, edit, delete, obj, dataInitDefault);
	}



	public static Hbox copyEditDeleteButtons(boolean edit, boolean copy, boolean delete, final GeneralValueObject obj,
			final DataInitDefault dataInitDefault) {
		return copyEditDeleteButtons(edit, copy, delete, obj, dataInitDefault, false);
	}



	/**
	 * <h2>Pabrik tombol aksi baris (Ubah / Copy / Hapus) yang dipakai-ulang seluruh aplikasi</h2>
	 *
	 * <p>
	 * Metode ini adalah <b>satu-satunya sumber kebenaran</b> (single source of truth) untuk
	 * merender trio tombol aksi pada setiap baris daftar/grid CRUD di e-Campus. Alih-alih tiap
	 * {@code Renderer} di puluhan/ratusan halaman membuat sendiri tombol ubah, salin, dan hapus
	 * berikut konfirmasi, ikon, gaya, dan penanganan kesalahannya, seluruh halaman cukup memanggil
	 * {@code Common.copyEditDeleteButtons(...)}. Dengan begitu perubahan tampilan maupun perilaku
	 * (mis. memperbesar ikon, menambah efek <i>hover</i>, mengubah dialog konfirmasi, atau
	 * menyesuaikan aturan kunci data) <b>cukup dilakukan di satu tempat</b> dan otomatis berlaku
	 * konsisten di mana pun. Inilah inti strategi <i>reuse</i> demi kemudahan pemeliharaan di
	 * kemudian hari: tidak ada duplikasi logika tombol, tidak ada gaya yang berbeda-beda antar
	 * halaman, dan tidak ada risiko salah satu halaman ketinggalan pembaruan.
	 * </p>
	 *
	 * <h3>Yang dihasilkan</h3>
	 * <p>
	 * Mengembalikan sebuah {@link Hbox} berisi maksimal tiga {@link MyToolbarbuttonConfig} berikon
	 * SVG: <i>edit-box-line</i> (Ubah), <i>edit-copy</i> (Copy), dan <i>trash</i> (Hapus). Wadah
	 * {@code Hbox} diberi <i>sclass</i> {@code ais-row-actions} dan tiap tombol diberi
	 * {@code ais-row-action-btn} plus penanda spesifik {@code ais-row-action-edit},
	 * {@code ais-row-action-copy}, atau {@code ais-row-action-delete}. Kontrak <i>sclass</i> inilah
	 * yang menjadi <b>jembatan ke CSS terpusat</b> di {@code /css/css_utama.css}: di sana ukuran
	 * ikon, area klik minimum, sudut membulat, jarak antar tombol, serta warna <i>hover</i> per
	 * aksi (biru untuk ubah, hijau untuk salin, merah untuk hapus) didefinisikan satu kali. Karena
	 * gaya dipindahkan ke CSS (bukan <i>inline style</i>), tampilan menjadi <b>responsif</b>: pada
	 * layar sempit/perangkat sentuh area tap diperbesar mengikuti panduan aksesibilitas (±44px),
	 * sedangkan pada desktop tombol tampil ringkas namun tetap nyaman. Hindari menimpa gaya tombol
	 * ini lewat <i>inline style</i> di pemanggil; cukup andalkan <i>sclass</i> agar tetap seragam.
	 * </p>
	 *
	 * <h3>Perilaku &amp; pengaman</h3>
	 * <ul>
	 *   <li><b>Visibilitas adaptif.</b> Tiap tombol hanya tampil bila benar-benar boleh: butuh
	 *       {@code obj != null} dan {@code dataInitDefault != null}, ditambah izin masing-masing
	 *       ({@code edit}, {@code copy}, {@code delete}). Dengan demikian pemanggil cukup meneruskan
	 *       hak akses pengguna tanpa perlu menyembunyikan tombol secara manual.</li>
	 *   <li><b>Hormati data terkunci.</b> Bila {@code obj} merupakan {@link VoKunci} dan berstatus
	 *       dikunci, tombol Hapus otomatis dinonaktifkan untuk mencegah penghapusan data terproteksi.</li>
	 *   <li><b>Ubah.</b> Memanggil {@code dataInitDefault.init(obj)} untuk membuka form edit; setiap
	 *       galat ditangkap dan ditampilkan ramah lewat {@code tampilCrudError(...)}.</li>
	 *   <li><b>Copy.</b> Meng-<i>clone</i> objek, mengosongkan id, mencatat asal salinan
	 *       ({@code setCopyDari}), lalu membuka form sebagai data baru — mempercepat input data
	 *       serupa.</li>
	 *   <li><b>Hapus.</b> Selalu meminta konfirmasi melalui {@code MyMessageboxConfig} sebelum
	 *       eksekusi, sehingga aman dari klik tak sengaja.</li>
	 * </ul>
	 *
	 * <h3>Parameter</h3>
	 * <ul>
	 *   <li>{@code edit} — izin menampilkan tombol Ubah.</li>
	 *   <li>{@code copy} — izin menampilkan tombol Copy.</li>
	 *   <li>{@code delete} — izin menampilkan tombol Hapus.</li>
	 *   <li>{@code obj} — objek data baris ({@link GeneralValueObject}) yang akan diubah/disalin/dihapus.</li>
	 *   <li>{@code dataInitDefault} — <i>callback</i> {@link DataInitDefault} yang membuka form
	 *       inisialisasi data (dipakai oleh aksi Ubah dan Copy).</li>
	 *   <li>{@code label} — bila {@code true}, tombol menampilkan teks ("Ubah"/"Copy"/"Hapus")
	 *       berorientasi vertikal di bawah ikon; bila {@code false}, hanya ikon (lebih ringkas untuk
	 *       grid padat).</li>
	 * </ul>
	 *
	 * <h3>Contoh pemakaian</h3>
	 * <pre>{@code
	 * // Di dalam RowRenderer sebuah daftar CRUD:
	 * Common.copyEditDeleteButtons(bolehUbah, bolehHapus, matakuliah, this).setParent(row);
	 * }</pre>
	 *
	 * <p>
	 * <b>Catatan pemeliharaan.</b> Bila perlu menambah aksi baru (mis. "Lihat" atau "Cetak"),
	 * tambahkan tombol di sini dengan pola <i>sclass</i> yang sama ({@code ais-row-action-btn} +
	 * penanda aksi) lalu lengkapi aturannya di {@code css_utama.css}; jangan membuat tombol aksi
	 * terpisah di tiap halaman agar konsistensi dan satu-titik-ubah tetap terjaga.
	 * </p>
	 *
	 * @param edit           izin menampilkan tombol Ubah
	 * @param copy           izin menampilkan tombol Copy
	 * @param delete         izin menampilkan tombol Hapus
	 * @param obj            objek data baris terkait (boleh {@code null} → tombol tersembunyi)
	 * @param dataInitDefault callback pembuka form inisialisasi (boleh {@code null} → tombol tersembunyi)
	 * @param label          {@code true} tampilkan teks + ikon; {@code false} hanya ikon
	 * @return {@link Hbox} berisi tombol aksi siap di-{@code setParent} ke sel baris grid
	 */
	public static Hbox copyEditDeleteButtons(boolean edit, boolean copy, boolean delete, final GeneralValueObject obj,
			final DataInitDefault dataInitDefault, boolean label) {

		boolean bolehEdit = edit && obj != null && dataInitDefault != null;
		boolean bolehCopy = copy && obj != null && dataInitDefault != null;
		boolean bolehDelete = delete && obj != null && dataInitDefault != null;

		if (obj instanceof VoKunci) {
			VoKunci kunci = (VoKunci) obj;
			if (kunci.getDikunci() != null) {
				bolehDelete = false;
			}
		}

		// === KEBAB MENU PATTERN ===
		// Hbox container tipis — hanya menampung satu tombol pemicu "⋮" (three-dots).
		// Semua item aksi (Ubah/Salin/Hapus + item tambahan dari tampilKunci dan caller)
		// ada di dalam Popup agar kolom grid tidak penuh dan layout rapi di semua layar.
		Hbox toolbar = new Hbox();
		toolbar.setSpacing("0px");
		toolbar.setAlign("center");
		toolbar.setSclass("ais-row-actions");
		toolbar.setStyle("display:flex;flex-wrap:nowrap;width:auto;align-items:center;"
				+ "justify-content:flex-end;gap:0;");

		// Popup yang muncul saat "⋮" diklik — berisi daftar item aksi vertikal.
		final org.zkoss.zul.Popup popup = new org.zkoss.zul.Popup();
		popup.setSclass("ais-row-popup");
		popup.setParent(toolbar);

		// Div konten popup (flex column) — item Toolbarbutton ditambahkan ke sini.
		final org.zkoss.zul.Div popupContent = new org.zkoss.zul.Div();
		popupContent.setSclass("ais-row-popup-content");
		popupContent.setParent(popup);

		// Simpan referensi popupContent agar tampilKunci() dan caller lain dapat
		// menambah item ke daftar aksi yang sama tanpa mengubah tiap halaman.
		toolbar.setAttribute("ais_row_actions_popup", popupContent);

		// --- Item Ubah ---
		if (bolehEdit) {
			MyToolbarbuttonConfig editItem = new MyToolbarbuttonConfig("Ubah Data", "/img/svg/edit-box-line.svg");
			editItem.setSclass("ais-row-popup-item ais-row-action-edit");
			editItem.setTooltiptext("Ubah Data");
			editItem.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (obj != null && dataInitDefault != null) {
							dataInitDefault.init(obj);
						}
					} catch (Exception e) {
						tampilCrudError(e, "Data tidak dapat dibuka untuk proses ubah.");
					}
				}
			});
			editItem.setParent(popupContent);
		}

		// --- Item Salin ---
		if (bolehCopy) {
			MyToolbarbuttonConfig copyItem = new MyToolbarbuttonConfig("Salin Data", "/img/svg/edit-copy.svg");
			copyItem.setSclass("ais-row-popup-item ais-row-action-copy");
			copyItem.setTooltiptext("Salin Data");
			copyItem.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (obj == null || dataInitDefault == null) {
							return;
						}
						GeneralValueObject copyObjt = obj.clone();
						if (copyObjt == null) {
							throw new Exception("Clone object menghasilkan nilai kosong.");
						}
						copyObjt.setId(null);
						copyObjt.setCopyDari(obj);
						dataInitDefault.init(copyObjt);
					} catch (Exception e) {
						tampilCrudError(e, "Data tidak dapat dicopy.");
					}
				}
			});
			copyItem.setParent(popupContent);
		}

		// --- Pemisah sebelum Hapus ---
		if (bolehDelete && (bolehEdit || bolehCopy)) {
			org.zkoss.zul.Div divider = new org.zkoss.zul.Div();
			divider.setSclass("ais-row-popup-divider");
			divider.setParent(popupContent);
		}

		// --- Item Hapus ---
		if (bolehDelete) {
			MyToolbarbuttonConfig deleteItem = new MyToolbarbuttonConfig("Hapus Data", "/img/svg/trash.svg");
			deleteItem.setSclass("ais-row-popup-item ais-row-popup-item-danger ais-row-action-delete");
			deleteItem.setTooltiptext("Hapus Data");
			deleteItem.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (obj == null || dataInitDefault == null) {
						return;
					}
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = MyMessageboxConfig.CANCEL;
									try {
										i = Integer.parseInt(String.valueOf(event.getData()));
									} catch (Exception e) {
										i = MyMessageboxConfig.CANCEL;
									}
									if (i == MyMessageboxConfig.OK) {
										try {

											if (obj instanceof DataSop) {
												DataSop dataSop = (DataSop) obj;
												Session session = HibernateUtil.currentSession();
												if (SopUtil.hapusDisposisi(session, dataSop.getDisposisiSop())) {
													Common.refreshDelete(session, obj);
												}
											} else if (obj instanceof ais.database.model.DiskonMahasiswa) {
												// FK detail_kegiatan.diskon_mahasiswa_data[_2|_3] → diskon_mahasiswa.id
												// Harus di-null-kan dahulu sebelum hapus agar tidak melanggar constraint
												ais.database.model.DiskonMahasiswa dm =
														(ais.database.model.DiskonMahasiswa) obj;
												org.hibernate.Session sHapus = HibernateUtil.currentSession();
												try {
													sHapus.createSQLQuery(
														"UPDATE detail_kegiatan SET diskon_mahasiswa_data=NULL WHERE diskon_mahasiswa_data=:id")
														.setParameter("id", dm.getId()).executeUpdate();
													sHapus.createSQLQuery(
														"UPDATE detail_kegiatan SET diskon_mahasiswa_data_2=NULL WHERE diskon_mahasiswa_data_2=:id")
														.setParameter("id", dm.getId()).executeUpdate();
													sHapus.createSQLQuery(
														"UPDATE detail_kegiatan SET diskon_mahasiswa_data_3=NULL WHERE diskon_mahasiswa_data_3=:id")
														.setParameter("id", dm.getId()).executeUpdate();
												} catch (Exception eClearDk) {
													System.err.println("[hapusDiskonMahasiswa] gagal clear detail_kegiatan: "
															+ eClearDk.getMessage());
												}
												Common.refreshDelete(sHapus, obj);
											} else if (obj instanceof ais.database.model.sekolah.Siswa) {
												/* Pengaturan biaya adalah konfigurasi keuangan, sehingga tidak aman
												 * dihapus otomatis bersama siswa. Cegah DELETE sebelum mencapai DB
												 * (dan sebelum FK melempar exception), lalu beri petunjuk yang jelas. */
												org.hibernate.Session sCek = HibernateUtil.currentSession();
												Long siswaId = ((ais.database.model.sekolah.Siswa) obj).getId();
												Number jumlahReferensi = (Number) sCek.createQuery(
														"select count(p.id) from PengaturanBiayaPunyaSiswa p where p.siswa.id = :id")
														.setParameter("id", siswaId).uniqueResult();
												if (jumlahReferensi != null && jumlahReferensi.longValue() > 0L) {
													MyMessageboxConfig.show("Siswa tidak dapat dihapus karena masih dipakai oleh "
															+ jumlahReferensi.longValue() + " pengaturan biaya. "
															+ "Hapus siswa dari Pengaturan Biaya terlebih dahulu.");
													return;
												}
												Common.refreshDelete(sCek, obj);
											} else {
												Common.refreshDelete(obj);
											}

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													try {
														dataInitDefault.onSearchDefault(arg0);
													} catch (Exception e) {
														tampilCrudError(e,
																"Data sudah dihapus, namun tampilan daftar gagal direfresh.");
													}
												}
											});
										} catch (Exception e) {
											if (merupakanConstraintReferensi(e)) {
												try {
													MyMessageboxConfig.show("Data tidak dapat dihapus karena masih dipakai oleh data lain. "
															+ "Hapus atau ubah data yang memakainya terlebih dahulu.");
												} catch (Exception ePesan) {
													ais.common.ErrorAuditUtil.record(ePesan,
															"auto-audit CommonUiFactoryHelper:pesan-constraint-hapus");
												}
											} else {
												tampilCrudError(e,
													"Data ini tidak dapat dihapus, karena kemungkinan masih berelasi dengan data lainnya.");
											}
										}

									}

								}
							});

				}
			});
			deleteItem.setParent(popupContent);
		}

		// --- Tombol pemicu "⋮" (three-dots) ---
		boolean adaAksi = bolehEdit || bolehCopy || bolehDelete;
		final MyToolbarbuttonConfig triggerBtn = new MyToolbarbuttonConfig("", "/img/svg/three-dots.svg");
		triggerBtn.setSclass("ais-row-action-btn ais-row-action-kebab");
		triggerBtn.setTooltiptext("Aksi");
		triggerBtn.setVisible(adaAksi);
		final org.zkoss.zul.Popup finalPopup = popup;
		triggerBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				finalPopup.open(triggerBtn, "after_end");
			}
		});
		triggerBtn.setParent(toolbar);

		return toolbar;
	}



	public static void tampilOnline(GeneralValueObject pertemuan, Component vbox) {
		UrlDisplayHelper.tampilOnline(pertemuan, vbox);
	}



	public static Button createVideoConference(final GeneralValueObject generalValueObject, Component hbox,
			boolean vertical, boolean isButton, final EventListener externalListener) throws Exception {
		return UrlDisplayHelper.createVideoConference(generalValueObject, hbox, vertical, isButton, externalListener);
	}



	public static Row tampilanScroll(Component component) {
		return UIClassHelper.tampilanScroll(component);
	}



	public static org.zkoss.zul.Center tampilanScrollTabbox(Component component) {
		return UIClassHelper.tampilanScrollTabbox(component);
	}

	public static void jadikanCenterScrollable(org.zkoss.zul.Center center) {
		UIClassHelper.jadikanCenterScrollable(center);
	}



	public static MyFormRow tampilanScroll1(Component component) {
		return UIClassHelper.tampilanScroll1(component);
	}



	public static MyFormRow tampilanScroll2(Component component) {
		return UIClassHelper.tampilanScroll2(component);
	}



	public static MyFormRow tampilanScroll3(Component component) {
		return UIClassHelper.tampilanScroll3(component);
	}



	public static MyFormRow tampilanScroll4(Component component) {
		return UIClassHelper.tampilanScroll4(component);
	}



	public static Button createVideoConrefrence(final GeneralValueObject generalValueObject, Component hbox,
			boolean vertical, boolean button, final EventListener eventListener) throws Exception {
		return UIClassHelper.createVideoConrefrence(generalValueObject, hbox, vertical, button, eventListener);
	}



	// Dipindahkan dari Common.java agar Common tetap lebih ringkas dan mudah dirawat.
@SuppressWarnings("unchecked")
	public static Box getDeskripsiPerkuliahanHbox(final VOPembelajaran voPembelajaran, final boolean tampilStatistik,
			final boolean horizontal, final Row rowData, final EventListener eventListener, final boolean refresh)
			throws Exception {
		final Box hbox;

		if (Common.isMobile()) {
			hbox = new Vbox();
		} else {
			hbox = new Hbox();
		}

		if (rowData != null) {
			rowData.appendChild(hbox);
		}

		// CSS Variables untuk UI Modern & Compact
		final String cardInfoStyle = "background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.05);";
		final String btnWrapperStyle = "background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 6px 4px; box-shadow: 0 1px 2px rgba(0,0,0,0.03); min-width: 70px; max-width: 90px; cursor: pointer; transition: all 0.2s ease; display: flex; flex-direction: column; align-items: center; justify-content: center;";
		final String badgeBlue = "font-size: 8px; font-weight: bold; color: #2563eb; background-color: #eff6ff; padding: 2px 6px; border-radius: 8px; margin-top: 4px; border: 1px solid #bfdbfe; display: inline-block; text-align: center;";
		final String badgeRed = "font-size: 8px; font-weight: bold; color: #dc3545; background-color: #fef2f2; padding: 2px 6px; border-radius: 8px; margin-top: 4px; border: 1px solid #fecaca; display: inline-block; text-align: center;";
		final String popupWindowStyle = "background-color: #f8fafc; border-radius: 10px;";
		final String btnCloseStyle = "font-size: 12px; font-weight: bold; color: #ffffff; background-color: #ef4444; border-radius: 6px; padding: 5px 15px; box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3); text-decoration: none; cursor: pointer; border: none;";
		final String activeColor = "color: #ef4444; font-weight: bold; font-size: 11px; text-decoration: none;";
		final String inactiveColor = "color: #475569; font-weight: 600; font-size: 11px; text-decoration: none;";

		if (voPembelajaran instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahanUmum(hbox, perkuliahan, true, true,
					null);

			Integer semester1 = perkuliahan.getSemester() == null ? 0 : perkuliahan.getSemester();
			String kelas1 = perkuliahan.getKelas();
			Integer sks = perkuliahan.getMatakuliah() == null ? 0 : perkuliahan.getMatakuliah().getSks();
			String matkul1 = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();

			String ksl = (semester1 < 1 ? "" : semester1) + "";
			try {
				ksl = (semester1 < 1 ? "" : semester1) + (kelas1.equals("") ? "" : " " + kelas1);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonUiFactoryHelper.java:1882");
			}

			Vbox vboxAtas = new MyVboxStyled();
			vboxAtas.setParent(rowData == null ? hbox : rowData);
			vboxAtas.setStyle(cardInfoStyle);
			if (rowData != null) {
				vboxAtas.setWidth("100%");
			}

			Label sub1 = new Label(matkul1 + (sks > 0 ? " (" + sks + " SKS)" : ""));
			sub1.setStyle("font-size: 14px; font-weight: 800; color: #1e40af; margin-bottom: 2px; display: block;");
			sub1.setParent(vboxAtas);

			Hbox myHbox1 = new Hbox();
			myHbox1.setAlign("center");
			myHbox1.setStyle("margin-bottom: 6px;");
			myHbox1.setParent(vboxAtas);

			if (!ksl.equals("")) {
				// Tampilkan jenis semester (Ganjil/Genap/SP) di badge "Kelas ..." supaya pengguna tahu
				// perkuliahan ini semester Ganjil, Genap, atau Semester Pendek (SP). Dipakai lintas layar
				// (Pertemuan, e-Learning, Rekap, dll.) yang memakai helper deskripsi perkuliahan ini.
				String jenisSmtLabel = Common.labelJenisSemester(perkuliahan);
				sub1 = new Label("Kelas " + ksl + (jenisSmtLabel.length() > 0 ? " (" + jenisSmtLabel + ")" : ""));
				sub1.setStyle(
						"font-size: 10px; font-weight: 700; color: #475569; background-color: #f1f5f9; padding: 3px 8px; border-radius: 6px; border: 1px solid #cbd5e1; display: inline-block;");
				sub1.setParent(myHbox1);
			}

			try {
				ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vboxAtas,
						perkuliahan);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (perkuliahan.getKurikulum() != null) {
				RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
						"Kurikulum:" + perkuliahan.getKurikulum().getNama()).setParent(vboxAtas);
			}
		} else if (voPembelajaran instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) voPembelajaran;
			hbox.appendChild(new Label());

			Vbox vboxAtas = new MyVboxStyled();
			vboxAtas.setParent(rowData == null ? hbox : rowData);
			vboxAtas.setStyle(cardInfoStyle);
			if (rowData != null) {
				vboxAtas.setWidth("100%");
			}

			SkripsiAction.tampilkanInfoMahasiswa(skripsi, new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					if (eventListener != null)
						Common.createDefaultTimer(eventListener);
				}
			}).setParent(vboxAtas);
		} else if (voPembelajaran instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) voPembelajaran;
			hbox.appendChild(new Label());

			Vbox vboxAtas = new MyVboxStyled();
			vboxAtas.setParent(rowData == null ? hbox : rowData);
			vboxAtas.setStyle(cardInfoStyle);
			if (rowData != null) {
				vboxAtas.setWidth("100%");
			}

			MahasiswaRequestTugasAkhirAction.tampilkanInfoMahasiswa(mahasiswaRequestTugasAkhir, new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					if (eventListener != null)
						Common.createDefaultTimer(eventListener);
				}
			}).setParent(vboxAtas);
		}

		hbox.appendChild(new Space());
		hbox.appendChild(new Space());

		final Box groupboxStyled = Common.isMobile() ? new MyVboxStyled() : new MyHboxStyled();
		groupboxStyled.setParent(rowData == null ? hbox : rowData);
		if (rowData != null) {
			groupboxStyled.setWidth("100%");
		}

		final Vbox vboxH = new Vbox();
		if (horizontal) {
			vboxH.setParent(groupboxStyled);
		}

		final Vbox vbox1 = new Vbox();
		vbox1.setWidth("100px");
		if (!horizontal) {
			vbox1.setParent(groupboxStyled);
		}
		hbox.appendChild(new Space());
		final Vbox vbox2 = new Vbox();
		vbox2.setWidth("130px");
		if (!horizontal) {
			vbox2.setParent(groupboxStyled);
		}

		/*
		 * Pada layar mobile, rantai box pembungkus (hbox luar + vboxH) bersifat
		 * shrink-to-fit sehingga container flex tombol aktifitas ikut menyempit dan
		 * tampil 1 tombol per baris. Paksa lebar penuh agar flow tombol dapat
		 * menyesuaikan lebar layar (lihat .ais-aktifitas-tile-flow di css_utama.css
		 * yang memaksa 2 kolom per baris di mobile).
		 */
		if (Common.isMobile()) {
			try {
				hbox.setWidth("100%");
				vboxH.setWidth("100%");
				vbox1.setWidth("100%");
				vbox2.setWidth("100%");
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/CommonUiFactoryHelper.java:2000");
			}
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Component> components = new ArrayList<Component>();
				Object[] jml = voPembelajaran.ambilJumlahPertemuanStatistik(true, true);
				int mhsSize = voPembelajaran.ambilJumlahDetailperkuliahanLangsung();

				final Collection<Pertemuan> pertemuans = (Collection<Pertemuan>) (jml == null || jml[7] == null
						? new ArrayList<Pertemuan>()
						: jml[7]);

				int jumlahUjianTotal = jml == null || jml[8] == null ? 0 : Integer.parseInt(jml[8].toString());
				int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0 : Integer.parseInt(jml[9].toString());

				Toolbarbutton sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Ujian"),
						"/img/svg/check2-all.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(jumlahUjianTotal == 0 ? inactiveColor : activeColor);

				Vbox label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				Label sub2 = new Label(Common.numberFormat.get().format(jumlahUjianTotal) + " ujian");
				sub2.setStyle(jumlahUjianTotal == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtUjian = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow window = new MyWindow("Ujian", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");
						window.setStyle(popupWindowStyle);

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setStyle("background: transparent; border: none;");
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setStyle("background: transparent; border: none; padding: 10px;");
						center.setParent(borderlayout);

						RekapitulasiUjianHelper.display(center, Common.getCurrentUser(), voPembelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle("float: right; background: transparent; border: none;");
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setStyle(btnCloseStyle);
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				};
				sub1.addEventListener("onClick", evtUjian);
				label.addEventListener("onClick", evtUjian);

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Diskusi"), "/img/svg/user-group.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(jumlahDiskusiTotal == 0 ? inactiveColor : activeColor);

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(jumlahDiskusiTotal) + " diskusi");
				sub2.setStyle(jumlahDiskusiTotal == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				final String[] contents = new String[] { "isi", "mahasiswa.nama", "tbmuser", "parent.isi" };
				List<String> columnHeadersAdding = new ArrayList<String>();
				columnHeadersAdding.add("Lampiran");
				MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(PertemuanPunyaDiskusi.class,
						new DataCriteria() {
							@Override
							public Criteria initCriteria(boolean order) {
								return HibernateUtil.currentSession().createCriteria(PertemuanPunyaDiskusi.class)
										.addOrder(Order.asc("id"))
										.add(pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("pertemuan", pertemuans));
							}
						}, "Download", FileFoto.icon(null), columnHeadersAdding, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Object[] objects = (Object[]) arg0.getData();
								PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) objects[0];
								XSSFRow row = (XSSFRow) objects[2];
								final XSSFCellStyle hlink_style = (XSSFCellStyle) objects[6];

								class DataAddingHelper {
									public void process(XSSFRow row, int index,
											PertemuanPunyaDiskusi pertemuanPunyaDiskusi) throws Exception {
										LampiranLain lam = LampiranLain.ambil(pertemuanPunyaDiskusi.getId(),
												LampiranLain.DISKUSI);
										XSSFCell cell = row.createCell(index);
										if (lam != null) {
											String nama = lam.getNama();
											cell.setCellStyle(hlink_style);
											cell.setCellValue(nama);
											String url = lam.createLinkUri();
											XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
													.createHyperlink(Hyperlink.LINK_URL);
											link.setAddress(url);
											cell.setHyperlink(link);
										}
									}
								}
								DataAddingHelper dataAddingHelper = new DataAddingHelper();
								dataAddingHelper.process(row, contents.length, pertemuanPunyaDiskusi);
							}
						}, false, null, "", contents);

				EventListener evtDiskusi = (EventListener) cetakToolbarbutton.getAttribute("eventListener");
				sub1.addEventListener("onClick", evtDiskusi);
				label.addEventListener("onClick", evtDiskusi);

				int pertemuan_file_content = 0;
				int tugas_file_content = 0;
				int tugas_kelompok = 0;
				int audio_pertemuan = 0;
				int video_pertemuan = 0;

				for (Pertemuan ids : pertemuans) {
					TreeMap<Long, TugasPertemuan> tugases = ids.ambilTugasPertemuanTotal();
					TreeMap<Long, TugasKelompok> tugasesKelompok = ids.ambilTugasKelompokTotal(refresh);

					tugas_file_content += ids.getJudultugas().trim().isEmpty() ? 0 : 1;
					for (TugasPertemuan tugasPertemuan : tugases.values()) {
						tugas_file_content += tugasPertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
					}
					for (TugasKelompok tugasPertemuan : tugasesKelompok.values()) {
						tugas_kelompok += tugasPertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
					}

					tugases = null;
					tugasesKelompok = null;

					pertemuan_file_content += ids.ambilJumlahPertemuanFileContent();
					audio_pertemuan += ids.ambilJumlahAudioPertemuan();
					video_pertemuan += ids.ambilJumlahVideoPertemuan();
				}

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Materi"), "/img/svg/file-lines.svg");
				sub1.setStyle(pertemuan_file_content == 0 ? inactiveColor : activeColor);
				sub1.setOrient("vertical");

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(pertemuan_file_content) + " file");
				sub2.setStyle(pertemuan_file_content == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtMateri = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow window = new MyWindow("Materi", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");
						window.setStyle(popupWindowStyle);

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setStyle("background: transparent; border: none;");
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setStyle("background: transparent; border: none; padding: 10px;");
						center.setParent(borderlayout);

						RekapitulasiMateriHelper.display(center, Common.getCurrentUser(), voPembelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle("float: right; background: transparent; border: none;");
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setStyle(btnCloseStyle);
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				};
				sub1.addEventListener("onClick", evtMateri);
				label.addEventListener("onClick", evtMateri);

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Tugas"), "/img/svg/task-line.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(tugas_file_content == 0 ? inactiveColor : activeColor);

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(tugas_file_content) + " tgs");
				sub2.setStyle(tugas_file_content == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtTugas = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow window = new MyWindow("Tugas", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");
						window.setStyle(popupWindowStyle);

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setStyle("background: transparent; border: none;");
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setStyle("background: transparent; border: none; padding: 10px;");
						center.setParent(borderlayout);

						RekapitulasiTugasHelper.display(center, Common.getCurrentUser(), voPembelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle("float: right; background: transparent; border: none;");
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setStyle(btnCloseStyle);
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				};
				sub1.addEventListener("onClick", evtTugas);
				label.addEventListener("onClick", evtTugas);

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Audio"), "/img/svg/file-audio-thin.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(audio_pertemuan == 0 ? inactiveColor : activeColor);

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(audio_pertemuan) + " file");
				sub2.setStyle(audio_pertemuan == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtAudio = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow window = new MyWindow("Audio", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");
						window.setStyle(popupWindowStyle);

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setStyle("background: transparent; border: none;");
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setStyle("background: transparent; border: none; padding: 10px;");
						center.setParent(borderlayout);

						RekapitulasiAudioHelper.display(center, Common.getCurrentUser(), voPembelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle("float: right; background: transparent; border: none;");
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setStyle(btnCloseStyle);
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				};
				sub1.addEventListener("onClick", evtAudio);
				label.addEventListener("onClick", evtAudio);

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Video"), "/img/svg/camera-video.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(video_pertemuan == 0 ? inactiveColor : activeColor);

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(video_pertemuan) + " file");
				sub2.setStyle(video_pertemuan == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtVideo = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow window = new MyWindow("Video", "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");
						window.setStyle(popupWindowStyle);

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setStyle("background: transparent; border: none;");
						borderlayout.setParent(window);

						Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setStyle("background: transparent; border: none; padding: 10px;");
						center.setParent(borderlayout);

						RekapitulasiVideoHelper.display(center, Common.getCurrentUser(), voPembelajaran);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setStyle("float: right; background: transparent; border: none;");
						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
						button.setStyle(btnCloseStyle);
						button.setTooltiptext("Tutup");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						button.setParent(toolbar);
						toolbar.setParent(south);

						window.setVisible(true);
						window.onModal();
					}
				};
				sub1.addEventListener("onClick", evtVideo);
				label.addEventListener("onClick", evtVideo);

				Tbmuser tbmuser = Common.getCurrentUser();
				if (voPembelajaran != null && (voPembelajaran instanceof Perkuliahan) && tbmuser != null
						&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null) {
					final Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;

					sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Sesuai RPS"),
							"/img/svg/check-circled-outline.svg");
					sub1.setOrient("vertical");
					sub1.setStyle(perkuliahan.getSemuaPertemuanSesuaiRps() ? activeColor : inactiveColor);

					label = new Vbox();
					label.setAlign("center");
					label.setStyle(btnWrapperStyle);
					label.appendChild(sub1);

					sub2 = new Label(
							perkuliahan.getSemuaPertemuanSesuaiRps() ? perkuliahan.getCatatanSesuaiRps() : "RPS");
					sub2.setStyle(perkuliahan.getSemuaPertemuanSesuaiRps() ? badgeRed : badgeBlue);
					label.appendChild(sub2);

					components.add(label);

					EventListener evtRps = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							final MyWindow window = new MyWindow("Catatan RPS", "none", false);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("500px");
							window.setWidth("350px");
							window.setStyle(popupWindowStyle);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setStyle("background: transparent; border: none;");
							borderlayout.setParent(window);

							Center center = new Center();
							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setStyle("background: transparent; border: none; padding: 10px;");
							center.setParent(borderlayout);

							MyGrid grid = new MyGrid();
							grid.setStyle(
									"border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); background: #ffffff;");
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("30%");

							column = new MyColumnConfig();
							column.setParent(columns);

							Rows rows = new Rows();
							rows.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig());
							final MyCheckboxConfig semuaPertemuanSesuaiRps;
							row.appendChild(
									semuaPertemuanSesuaiRps = new MyCheckboxConfig("Semua Pertemuan Sesuai Rps ?"));
							semuaPertemuanSesuaiRps.setChecked(perkuliahan.getSemuaPertemuanSesuaiRps());

							final Combobox kehadiran = new Combobox();
							kehadiran.setWidth("82px");

							Comboitem comboitem = new Comboitem("Belum Ditentukan");
							comboitem.setValue(0L);
							kehadiran.appendChild(comboitem);

							comboitem = new Comboitem("Sesuai");
							comboitem.setValue(1L);
							kehadiran.appendChild(comboitem);

							comboitem = new Comboitem("Tidak Sesuai");
							comboitem.setValue(2L);
							kehadiran.appendChild(comboitem);

							Common.selectComboItem(kehadiran, perkuliahan.getSemuaNilaiSesuaiRps());
							kehadiran.setReadonly(true);

							row = new MyFormRow();
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
							row.appendChild(kehadiran);
							kehadiran.setWidth("90%");

							row = new MyFormRow();
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
							final Textbox catatanSesuaiRps;
							row.appendChild(catatanSesuaiRps = new Textbox(perkuliahan.getCatatanSesuaiRps()));
							catatanSesuaiRps.setWidth("90%");
							catatanSesuaiRps.setRows(3);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setStyle("float: right; background: transparent; border: none;");
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
							button.setStyle(
									"font-size: 13px; font-weight: bold; color: #475569; background-color: #f1f5f9; border-radius: 6px; padding: 6px 15px; border: 1px solid #cbd5e1; cursor: pointer; margin-right: 8px; text-decoration: none;");
							button.setTooltiptext("Tutup");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							button.setParent(toolbar);

							MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
							save.setStyle(
									"font-size: 13px; font-weight: bold; color: #ffffff; background-color: #0ea5e9; border-radius: 6px; padding: 6px 20px; box-shadow: 0 2px 4px rgba(14, 165, 233, 0.3); text-decoration: none; cursor: pointer; border: none;");
							save.setTooltiptext("Simpan");
							save.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									perkuliahan.setSemuaPertemuanSesuaiRps(semuaPertemuanSesuaiRps.isChecked());
									perkuliahan.setCatatanSesuaiRps(catatanSesuaiRps.getValue());
									perkuliahan.setSemuaNilaiSesuaiRps((Long) kehadiran.getSelectedItem().getValue());

									Common.refreshUpdate(perkuliahan);
									window.detach();

									if (eventListener != null) {
										Common.createDefaultTimer(eventListener);
									}
								}
							});
							save.setParent(toolbar);
							toolbar.setParent(south);

							window.setVisible(true);
							window.onModal();
						}
					};
					sub1.addEventListener("onClick", evtRps);
					label.addEventListener("onClick", evtRps);
				}

				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Peserta"), "/img/svg/user-box-line.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(mhsSize == 0 ? inactiveColor : activeColor);

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(mhsSize) + " org");
				sub2.setStyle(mhsSize == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtPeserta = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (voPembelajaran != null && (voPembelajaran instanceof Perkuliahan)) {
							Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
							LaporanAlbumMahasiswaPerProdiDanAngkatan window = new LaporanAlbumMahasiswaPerProdiDanAngkatan(
									perkuliahan);
							window.setTitle("Daftar Mahasiswa " + perkuliahan.infoSimple());
							window.setStyle(popupWindowStyle);
							window.setClosable(true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("95%");
							window.setWidth("95%");
							window.onModal();
						}
					}
				};
				sub1.addEventListener("onClick", evtPeserta);
				label.addEventListener("onClick", evtPeserta);

				int pert = pertemuans.size();
				sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Pertemuan"), "/img/svg/list-check.svg");
				sub1.setOrient("vertical");
				sub1.setStyle(pert == 0 ? inactiveColor : activeColor);

				label = new Vbox();
				label.setAlign("center");
				label.setStyle(btnWrapperStyle);
				label.appendChild(sub1);

				sub2 = new Label(Common.numberFormat.get().format(pert) + " agd");
				sub2.setStyle(pert == 0 ? badgeBlue : badgeRed);
				label.appendChild(sub2);

				components.add(label);

				EventListener evtPert = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanELearningAction.prosess(voPembelajaran, false);
					}
				};
				sub1.addEventListener("onClick", evtPert);
				label.addEventListener("onClick", evtPert);

				if (voPembelajaran != null && (voPembelajaran instanceof Perkuliahan)) {
					final Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
					Session session = HibernateUtil.currentSession();
					int referensi = ((Number) session.createCriteria(PerkuliahanPunyaItem.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Buku Ref."),
							"/img/svg/books-thin.svg");
					sub1.setOrient("vertical");
					sub1.setStyle(referensi == 0 ? inactiveColor : activeColor);

					label = new Vbox();
					label.setAlign("center");
					label.setStyle(btnWrapperStyle);
					label.appendChild(sub1);

					sub2 = new Label(Common.numberFormat.get().format(referensi) + " ref");
					sub2.setStyle(referensi == 0 ? badgeBlue : badgeRed);
					label.appendChild(sub2);

					components.add(label);

					EventListener evtBukuRef = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {

							final MyWindow window = new MyWindow("Buku Referensi", "none", false);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("95%");
							window.setWidth("95%");
							window.setStyle(popupWindowStyle);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setStyle("background: transparent; border: none;");
							borderlayout.setParent(window);

							Center center = new Center();
							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setStyle("background: transparent; border: none; padding: 10px;");
							center.setParent(borderlayout);

							Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
							borderlayout1.setParent(center);

							Center center1 = new Center();
							ais.ui.util.ZkCompat.setFlex(center1, true);
							center1.setParent(borderlayout1);

							PerkuliahanPunyaItemHelper data = new PerkuliahanPunyaItemHelper();
							data.display(perkuliahan, center1);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setStyle("float: right; background: transparent; border: none;");
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
							button.setStyle(btnCloseStyle);
							button.setTooltiptext("Tutup");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							button.setParent(toolbar);
							toolbar.setParent(south);

							window.setVisible(true);
							window.onModal();
						}
					};
					sub1.addEventListener("onClick", evtBukuRef);
					label.addEventListener("onClick", evtBukuRef);

					int bukuAjar = ((Number) session.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
							.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Buku Ajar"),
							"/img/svg/check2-circle.svg");
					sub1.setOrient("vertical");
					sub1.setStyle(bukuAjar == 0 ? inactiveColor : activeColor);

					label = new Vbox();
					label.setAlign("center");
					label.setStyle(btnWrapperStyle);
					label.appendChild(sub1);

					sub2 = new Label(Common.numberFormat.get().format(bukuAjar) + " bdl");
					sub2.setStyle(bukuAjar == 0 ? badgeBlue : badgeRed);
					label.appendChild(sub2);

					components.add(label);

					EventListener evtBukuAjar = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {

							final MyWindow window = new MyWindow("Buku Ajar", "none", false);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("95%");
							window.setWidth("95%");
							window.setStyle(popupWindowStyle);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setStyle("background: transparent; border: none;");
							borderlayout.setParent(window);

							Center center = new Center();
							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setStyle("background: transparent; border: none; padding: 10px;");
							center.setParent(borderlayout);

							BukuBahanAjarHelper data = new BukuBahanAjarHelper();
							data.display(perkuliahan.getMatakuliah(), center, perkuliahan);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setStyle("float: right; background: transparent; border: none;");
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
							button.setStyle(btnCloseStyle);
							button.setTooltiptext("Tutup");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							button.setParent(toolbar);
							toolbar.setParent(south);

							window.setVisible(true);
							window.onModal();
						}
					};
					sub1.addEventListener("onClick", evtBukuAjar);
					label.addEventListener("onClick", evtBukuAjar);
				}

				if (voPembelajaran != null && (voPembelajaran instanceof Perkuliahan
						|| voPembelajaran instanceof KelompokKkn || voPembelajaran instanceof KelompokPkl)) {

					sub1 = new ais.ui.util.MyToolbarbuttonConfig(Common.getBahasa("Tugas Kelp."),
							"/img/svg/user-list-thin.svg");
					sub1.setOrient("vertical");
					sub1.setStyle(tugas_kelompok == 0 ? inactiveColor : activeColor);

					label = new Vbox();
					label.setAlign("center");
					label.setStyle(btnWrapperStyle);
					label.appendChild(sub1);

					sub2 = new Label(Common.numberFormat.get().format(tugas_kelompok) + " tgs");
					sub2.setStyle(tugas_kelompok == 0 ? badgeBlue : badgeRed);
					label.appendChild(sub2);

					components.add(label);

					EventListener evtTgsKelp = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {

							final MyWindow window = new MyWindow("Tugas Kelompok", "none", false);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("95%");
							window.setWidth("95%");
							window.setStyle(popupWindowStyle);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setStyle("background: transparent; border: none;");
							borderlayout.setParent(window);

							Center center = new Center();
							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setStyle("background: transparent; border: none; padding: 10px;");
							center.setParent(borderlayout);

							Tbmuser tbmuser = Common.getCurrentUser();

							TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(
									tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa());
							tugasKelompokHelper.display(voPembelajaran, center);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 10px;");
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setStyle("float: right; background: transparent; border: none;");
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
							button.setStyle(btnCloseStyle);
							button.setTooltiptext("Tutup");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							button.setParent(toolbar);
							toolbar.setParent(south);

							window.setVisible(true);
							window.onModal();
						}
					};
					sub1.addEventListener("onClick", evtTgsKelp);
					label.addEventListener("onClick", evtTgsKelp);
				}

				if (tampilStatistik) {
					hbox.appendChild(new Space());
					TampilanELearningAction.tampilkanStatistik(voPembelajaran, jml, mhsSize, "",
							rowData == null ? hbox : rowData);
				}

				// Merender seluruh tombol menggunakan CSS Flexbox agar ter-wrap dengan rapi dan
				// presisi
				org.zkoss.zul.Div buttonContainer = new org.zkoss.zul.Div();
				buttonContainer.setWidth("100%");
				// sclass untuk kontrol responsif (mobile = 2 kolom/baris) via css_utama.css
				buttonContainer.setSclass("ais-aktifitas-tile-flow");
				// Gunakan parameter horizontal bawaan dari getDeskripsiPerkuliahanHbox
				buttonContainer.setStyle("display: flex; flex-wrap: wrap; gap: 8px; justify-content: "
						+ (!horizontal ? "center" : "flex-start") + "; padding-top: 5px;");

				for (Component component : components) {
					if (component instanceof Button) {
						((Button) component).setOrient("vertical");
					}
					if (component.isVisible()) {
						component.setParent(buttonContainer);
					}
				}

				// Masukkan container tombol ke dalam layout utama yang sudah disiapkan
				// sebelumnya
				if (!horizontal) {
					buttonContainer.setParent(vbox1);
				} else {
					buttonContainer.setParent(vboxH);
				}

				components.clear();
				components = null;
			}
		});

		return hbox; // Wajib mengembalikan hbox utama agar judul mata kuliah dan info lainnya ikut
						// ter-render
	}


    public static List<DetailJenisPenilaian> getDetailJenisPenilaians(JadwalPelajaran jadwalPelajaran) {
        if (jadwalPelajaran == null || jadwalPelajaran.getMatapelajaran() == null) {
            return new ArrayList<DetailJenisPenilaian>();
        }

        KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran = jadwalPelajaran.getKurikulumPunyaMatapelajaran();
        JenisPenilaian jenisPenilaian = jadwalPelajaran.getMatapelajaran().getJenisPenilaian();
        if (kurikulumPunyaMatapelajaran != null && kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
                && kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian() != null) {
            jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah().getJenisPenilaian();
        }
        if (jenisPenilaian == null) {
            return new ArrayList<DetailJenisPenilaian>();
        }

        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            @SuppressWarnings("unchecked")
            List<DetailJenisPenilaian> detailJenisPenilaians = session.createCriteria(DetailJenisPenilaian.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .createAlias("jenisItemPenilaianSiswa", "jenisItemPenilaianSiswa")
                    .add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
                    .addOrder(Order.desc("jenisItemPenilaianSiswa.nomorUrut")).list();
            return detailJenisPenilaians == null ? new ArrayList<DetailJenisPenilaian>() : detailJenisPenilaians;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<DetailJenisPenilaian>();
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

}
