package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.temporary.IPKMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk transaksi status mahasiswa. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * tahunakademik}, {@code Combobox jenisSemester}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code File file}; inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class TransaksiStatusMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private File file;

	// public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public TransaksiStatusMahasiswa() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public TransaksiStatusMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);
		// tahunakademik.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });
		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		// jenisSemester.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				Common.selectComboItem(searchjurusan, null);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
				// initSpreadsheet();
			}
		});
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			}
		}
		// else {
		// Fakultas fakultas = (Fakultas) HibernateUtil.currentSession()
		// .createCriteria(Fakultas.class).add(Restrictions.idEq(1L))
		// .uniqueResult();
		// Jurusan jurusan = (Jurusan) HibernateUtil.currentSession()
		// .createCriteria(Jurusan.class).add(Restrictions.idEq(1L))
		// .uniqueResult();
		// Common.selectComboItem(searchfakultas, fakultas);
		// Common.insertCombo(searchjurusan, new String[]{"nama", "kodeEpsbed"},
		// "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
		// Restrictions.eq("fakultas", searchfakultas
		// .getSelectedItem().getValue()));
		// Common.selectComboItem(searchjurusan, jurusan);
		// }

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik);
		tahunakademik.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (TRLSM.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "TRLSM.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiStatusMahasiswa.java:226");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

		if (tahunakademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Akademik harus diisi");
			return;
		}

		if (jenisSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi");
			return;
		}

		Common.clear(center);
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? null
				: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();
		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).uniqueResult();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;

				XSSFRow rowhead = sheet.createRow((short) 0);
				rowhead.createCell(0).setCellValue("THSMSTRLS");
				rowhead.createCell(1).setCellValue("KDPTITRLS");
				rowhead.createCell(2).setCellValue("KDJENTRLS");
				rowhead.createCell(3).setCellValue("KDPSTTRLS");
				rowhead.createCell(4).setCellValue("NIMHSTRLS");
				rowhead.createCell(5).setCellValue("STMHSTRLS");

				rowhead.createCell(6).setCellValue("TGLLSTRLS");
				rowhead.createCell(7).setCellValue("SKSTTTRLS");
				rowhead.createCell(8).setCellValue("NLIPKTRLS");

				rowhead.createCell(9).setCellValue("NOSKRTRLS");
				rowhead.createCell(10).setCellValue("TGLRETRLS");
				rowhead.createCell(11).setCellValue("NOIJATRLS");

				rowhead.createCell(12).setCellValue("STLLSTRLS");
				rowhead.createCell(13).setCellValue("JNLLSTRLS");
				rowhead.createCell(14).setCellValue("BLAWLTRLS");
				rowhead.createCell(15).setCellValue("BLAKHTRLS");
				rowhead.createCell(16).setCellValue("NODS1TRLS");
				rowhead.createCell(17).setCellValue("NODS2TRLS");
				rowhead.createCell(18).setCellValue("NODS3TRLS");
				rowhead.createCell(19).setCellValue("NODS4TRLS");
				rowhead.createCell(20).setCellValue("NODS5TRLS");

				Session session = HibernateUtil.currentNativeSession();

				List<IPKMahasiswa> mahasiswas = session.createCriteria(IPKMahasiswa.class)
						.add(Restrictions.eq("tahunAkademik", tahunakademik.getSelectedItem().getValue()))

				.add(Restrictions.sqlRestriction("semester % 2 = "
						+ (jenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))
						.createCriteria("mahasiswa")

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

				.createAlias("jurusan", "jurusan")

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.list();

				rowIndex = 1;
				for (IPKMahasiswa ipkmhs : mahasiswas) {
					if (ipkmhs.getMahasiswa() != null) {
						System.out.println("epsbed status mhs : " + ipkmhs.getMahasiswa().getNim());
						label.setValue("Sedang memproses data " + ipkmhs.getMahasiswa().toString() + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswas.size()) + " %)");
					}

					XSSFRow row = sheet.createRow(rowIndex);
					row.createCell(0)
							.setCellValue(CommonEpsbed.getTahunSemesterPelaporan(
									(String) tahunakademik.getSelectedItem().getValue(),
									(String) jenisSemester.getSelectedItem().getValue()));
					row.createCell(1)
							.setCellValue(perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());
					row.createCell(2).setCellValue(ipkmhs.getMahasiswa().getJenjang().getJenjangEpsbed());
					row.createCell(3)
							.setCellValue(ipkmhs.getMahasiswa().getJurusan() == null
									? ipkmhs.getMahasiswa().getJurusan().getKodeEpsbed()
									: ipkmhs.getMahasiswa().getJurusan().getKodeEpsbed());
					row.createCell(4).setCellValue(ipkmhs.getMahasiswa().getNim());
					StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(ipkmhs.getMahasiswa()).getStatusMahasiswa();

					row.createCell(5).setCellValue(statusMahasiswa.getKodeEpsbed());
					row.createCell(6).setCellValue(ipkmhs.getMahasiswa().getTanggalLulus() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(ipkmhs.getMahasiswa().getTanggalLulus()));

					row.createCell(7).setCellValue(ipkmhs == null ? "0" : ipkmhs.getSksTotal().toString());
					@SuppressWarnings("resource")
					Formatter format = new Formatter();
					row.createCell(8)
							.setCellValue(ipkmhs == null ? "0" : format.format("%.2f", ipkmhs.getIpk()).toString());
					row.createCell(9).setCellValue(ipkmhs.getMahasiswa().getNoAkta1());
					row.createCell(10).setCellValue(ipkmhs.getMahasiswa().getTanggalYudisium() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(ipkmhs.getMahasiswa().getTanggalYudisium()));
					row.createCell(11).setCellValue(ipkmhs.getMahasiswa().getNoIjazah1());
					Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
							.add(Restrictions.eq("mahasiswa", ipkmhs.getMahasiswa())).uniqueResult();
					row.createCell(12).setCellValue(skripsi == null ? "" : skripsi.getJalurSkripsi());
					row.createCell(13).setCellValue(skripsi == null ? "" : skripsi.getTipeSkripsi());
					row.createCell(14)
							.setCellValue(skripsi == null ? ""
									: skripsi.getAwalBimbingan() == null ? ""
											: CommonEpsbed.dateFormatEpsbed.get().format(skripsi.getAwalBimbingan()));
					row.createCell(15)
							.setCellValue(skripsi == null ? ""
									: skripsi.getAkhirBimbingan() == null ? ""
											: CommonEpsbed.dateFormatEpsbed.get().format(skripsi.getAkhirBimbingan()));
					row.createCell(16)
							.setCellValue(skripsi == null ? ""
									: skripsi.getPembimbing() == null ? ""
											: skripsi.getPembimbing().getNidn() == null ? ""
													: skripsi.getPembimbing().getNidn());
					row.createCell(17)
							.setCellValue(skripsi == null ? ""
									: skripsi.getKetuaSidang() == null ? ""
											: skripsi.getKetuaSidang().getNidn() == null ? ""
													: skripsi.getKetuaSidang().getNidn());
					row.createCell(18)
							.setCellValue(skripsi == null ? ""
									: skripsi.getPenguji1() == null ? ""
											: skripsi.getPenguji1().getNidn() == null ? ""
													: skripsi.getPenguji1().getNidn());
					row.createCell(19)
							.setCellValue(skripsi == null ? ""
									: skripsi.getPenguji2() == null ? ""
											: skripsi.getPenguji2().getNidn() == null ? ""
													: skripsi.getPenguji2().getNidn());
					row.createCell(20)
							.setCellValue(skripsi == null ? ""
									: skripsi.getPenguji3() == null ? ""
											: skripsi.getPenguji3().getNidn() == null ? ""
													: skripsi.getPenguji3().getNidn());

					rowIndex++;
				}
				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e); 
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				mahasiswas.clear();
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
