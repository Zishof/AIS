package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();

	private Textbox nimMahasiswa = new Textbox();
	private Textbox namaMahasiswa = new Textbox();
	private Textbox kelas = new Textbox();

	private File file;

	private Combobox searchstatus;

	public DownloadMahasiswa() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
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

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mahasiswa"));
		row.appendChild(nimMahasiswa);
		nimMahasiswa.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(namaMahasiswa);
		namaMahasiswa.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus = new Combobox());
		Common.insertComboDanSemua(searchstatus, "nama", "kodeEpsbed", StatusMahasiswa.class);
		searchstatus.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "mahasiswa.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadMahasiswa.java:199");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		final String kel = kelas.getValue().trim();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_nilai_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("Mahasiswa");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama");
				rowhead.createCell(2).setCellValue("Tempat Lahir");
				rowhead.createCell(3).setCellValue("Tanggal Lahir");
				rowhead.createCell(4).setCellValue("Jenis Kelamin");
				rowhead.createCell(5).setCellValue("NIK");
				rowhead.createCell(6).setCellValue("Agama");
				rowhead.createCell(7).setCellValue("NISN");
				rowhead.createCell(8).setCellValue("Jalur Pendaftaran");
				rowhead.createCell(9).setCellValue("NPWP");
				rowhead.createCell(10).setCellValue("Kewarganegaraan");
				rowhead.createCell(11).setCellValue("Jenis Pendaftaran");
				rowhead.createCell(12).setCellValue("Tgl Masuk Kuliah");
				rowhead.createCell(13).setCellValue("Mulai semester");
				rowhead.createCell(14).setCellValue("Jalan");
				rowhead.createCell(15).setCellValue("RT");
				rowhead.createCell(16).setCellValue("RW");
				rowhead.createCell(17).setCellValue("Nama Dusun");
				rowhead.createCell(18).setCellValue("Kelurahan");
				rowhead.createCell(19).setCellValue("Kecamatan");
				rowhead.createCell(20).setCellValue("Kode Pos");
				rowhead.createCell(21).setCellValue("Jenis Tinggal");
				rowhead.createCell(22).setCellValue("Alat Transportasi");
				rowhead.createCell(23).setCellValue("Telp Rumah");
				rowhead.createCell(24).setCellValue("No HP");
				rowhead.createCell(25).setCellValue("Email");
				rowhead.createCell(26).setCellValue("Terima KPS");
				rowhead.createCell(27).setCellValue("No KPS");
				rowhead.createCell(28).setCellValue("NIK Ayah");
				rowhead.createCell(29).setCellValue("Nama Ayah");
				rowhead.createCell(30).setCellValue("Tgl Lahir Ayah");
				rowhead.createCell(31).setCellValue("Pendidikan Ayah");
				rowhead.createCell(32).setCellValue("Pekerjaan Ayah");
				rowhead.createCell(33).setCellValue("Penghasilan Ayah");
				rowhead.createCell(34).setCellValue("NIK Ibu");
				rowhead.createCell(35).setCellValue("Nama Ibu");
				rowhead.createCell(36).setCellValue("Tanggal Lahir Ibu");
				rowhead.createCell(37).setCellValue("Pendidikan Ibu");
				rowhead.createCell(38).setCellValue("Pekerjaan Ibu");
				rowhead.createCell(39).setCellValue("Penghasilan Ibu");
				rowhead.createCell(40).setCellValue("Nama Wali");
				rowhead.createCell(41).setCellValue("Tanggal Lahir wali");
				rowhead.createCell(42).setCellValue("Pendidikan Wali");
				rowhead.createCell(43).setCellValue("Pekerjaan Wali");
				rowhead.createCell(44).setCellValue("Penghasilan Wali");
				rowhead.createCell(45).setCellValue("Kode Prodi");
				rowhead.createCell(46).setCellValue("Jenis Pembiayaan");
				rowhead.createCell(47).setCellValue("Biaya Masuk");

				Session session = HibernateUtil.currentNativeSession();

				Criterion criteriaStatus = Restrictions.sqlRestriction("true");
				if (selectedStatusMahasiswa != null) {
					String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ selectedStatusMahasiswa.getId() + " and tahunakademik = '"
							+ Common.getCurrentTahunAkademik() + "' and semester%2="
							+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
					System.out.println("sql=>" + sql);
					criteriaStatus = Restrictions.sqlRestriction(sql);
				}

				List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(criteriaStatus)

						.add(kel != null && !kel.trim().isEmpty()
								? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions
								.and(nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("nim", nimMahasiswa.getValue().trim(), MatchMode.ANYWHERE),

										namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("nama", namaMahasiswa.getValue().trim(),
														MatchMode.ANYWHERE)))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))

						.addOrder(Order.asc("nim")).list();

				int size = mahasiswas.size();

				int rowIndex = 1;
				for (Mahasiswa mahasiswa : mahasiswas) {

					BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

					label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);

					row.createCell(0).setCellValue(mahasiswa.getNim());
					row.createCell(1).setCellValue(mahasiswa.getNama());
					row.createCell(2).setCellValue(mahasiswa.getTempatlahir());
					row.createCell(3).setCellValue(mahasiswa.getTanggallahir() == null ? ""
							: Common.databaseDateFormat.get().format(mahasiswa.getTanggallahir()));
					row.createCell(4).setCellValue(mahasiswa.getKelamin().equals("Laki-laki") ? "L" : "P");
					row.createCell(5).setCellValue(biodataMahasiswa.getNoIdentitas());
					row.createCell(6)
							.setCellValue(mahasiswa.getAgama() == null ? 0L : mahasiswa.getAgama().getFeeder());

					row.createCell(7).setCellValue(biodataMahasiswa.getNisn());

					row.createCell(8).setCellValue(
							mahasiswa.getJenisSeleksi() == null ? "" : mahasiswa.getJenisSeleksi().getKode());

					row.createCell(9).setCellValue(biodataMahasiswa.getNpwp());

					row.createCell(10)
							.setCellValue(mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getKode());

					row.createCell(11).setCellValue(mahasiswa.getStatusAwalMahasiswa().getFeeder());
					row.createCell(12).setCellValue(mahasiswa.getTanggalMasuk() == null ? ""
							: Common.databaseDateFormat.get().format(mahasiswa.getTanggalMasuk()));

					row.createCell(13).setCellValue(mahasiswa.getTahunangkatan()
							+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GENAP) ? "2" : "1"));

					row.createCell(14).setCellValue(biodataMahasiswa.getAlamat());
					row.createCell(15).setCellValue(biodataMahasiswa.getRt());
					row.createCell(16).setCellValue(biodataMahasiswa.getRw());
					row.createCell(17).setCellValue(biodataMahasiswa.getDusun());
					row.createCell(18).setCellValue(
							biodataMahasiswa.getKelurahan() == null || biodataMahasiswa.getKelurahan().trim().isEmpty()
									? "-" : biodataMahasiswa.getKelurahan());

					row.createCell(19).setCellValue(
							biodataMahasiswa.getKecamatan() == null ? "" : biodataMahasiswa.getKecamatan().getFeeder());
					row.createCell(20).setCellValue(biodataMahasiswa.getKodepos());
					row.createCell(21).setCellValue(biodataMahasiswa.getJenisTinggalMahasiswa() == null ? 1L
							: biodataMahasiswa.getJenisTinggalMahasiswa().getFeeder());
					row.createCell(22).setCellValue(biodataMahasiswa.getAlatTransportasiMahasiswa() == null ? 1L
							: biodataMahasiswa.getAlatTransportasiMahasiswa().getFeeder());

					row.createCell(23).setCellValue(biodataMahasiswa.getTeleponRumah());
					row.createCell(24).setCellValue(biodataMahasiswa.getHp());
					row.createCell(25).setCellValue(biodataMahasiswa.getEmail());
					row.createCell(26).setCellValue(0);
					row.createCell(27).setCellValue("");
					row.createCell(28).setCellValue(biodataMahasiswa.getNikAyah());

					row.createCell(29).setCellValue(biodataMahasiswa.getNamaAyah());
					row.createCell(30).setCellValue(biodataMahasiswa.getTanggalLahirAyah() == null ? ""
							: Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirAyah()));
					row.createCell(31)
							.setCellValue(biodataMahasiswa.getJenjangPendidikanAyah() == null
									|| biodataMahasiswa.getJenjangPendidikanAyah().getFeeder() == null ? ""
											: biodataMahasiswa.getJenjangPendidikanAyah().getFeeder().toString());
					row.createCell(32)
							.setCellValue(biodataMahasiswa.getJenisPekerjaanAyah() == null
									|| biodataMahasiswa.getJenisPekerjaanAyah().getFeeder() == null ? ""
											: biodataMahasiswa.getJenisPekerjaanAyah().getFeeder().toString());

					row.createCell(33)
							.setCellValue(biodataMahasiswa.getJenisPenghasilanAyah() == null
									|| biodataMahasiswa.getJenisPenghasilanAyah() == null ? ""
											: biodataMahasiswa.getJenisPenghasilanAyah().getFeeder().toString());

					row.createCell(34).setCellValue(biodataMahasiswa.getNikIbu());

					row.createCell(35).setCellValue(biodataMahasiswa.getNamaIbu());
					row.createCell(36).setCellValue(biodataMahasiswa.getTanggalLahirIbu() == null ? ""
							: Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirIbu()));
					row.createCell(37)
							.setCellValue(biodataMahasiswa.getJenjangPendidikanIbu() == null
									|| biodataMahasiswa.getJenjangPendidikanIbu().getFeeder() == null ? ""
											: biodataMahasiswa.getJenjangPendidikanIbu().getFeeder().toString());

					row.createCell(38)
							.setCellValue(biodataMahasiswa.getJenisPekerjaanIbu() == null
									|| biodataMahasiswa.getJenisPekerjaanIbu().getFeeder() == null ? ""
											: biodataMahasiswa.getJenisPekerjaanIbu().getFeeder().toString());
					row.createCell(39)
							.setCellValue(biodataMahasiswa.getJenisPenghasilanIbu() == null
									|| biodataMahasiswa.getJenisPenghasilanIbu().getFeeder() == null ? ""
											: biodataMahasiswa.getJenisPenghasilanIbu().getFeeder().toString());

					row.createCell(40).setCellValue(biodataMahasiswa.getNamaWali());
					row.createCell(41).setCellValue(biodataMahasiswa.getTanggalLahirWali() == null ? ""
							: Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirWali()));
					row.createCell(42)
							.setCellValue(biodataMahasiswa.getJenjangPendidikanWali() == null
									|| biodataMahasiswa.getJenjangPendidikanWali().getFeeder() == null ? ""
											: biodataMahasiswa.getJenjangPendidikanWali().getFeeder().toString());
					row.createCell(43)
							.setCellValue(biodataMahasiswa.getJenisPekerjaanWali() == null
									|| biodataMahasiswa.getJenisPekerjaanWali().getFeeder() == null ? ""
											: biodataMahasiswa.getJenisPekerjaanWali().getFeeder().toString());
					row.createCell(44)
							.setCellValue(biodataMahasiswa.getJenisPenghasilanWali() == null
									|| biodataMahasiswa.getJenisPenghasilanWali().getFeeder() == null ? ""
											: biodataMahasiswa.getJenisPenghasilanWali().getFeeder().toString());
					row.createCell(45).setCellValue(mahasiswa.getJurusan().getKodeEpsbed());

					row.createCell(46).setCellValue(mahasiswa.getJenisPembiayaanMahasiswa() == null ? 1L
							: mahasiswa.getJenisPembiayaanMahasiswa().getFeeder());

					BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
							session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1),
							BiodataCalonMahasiswa.class);
					Double biayamasuk = 0.0;

					if (biodataCalonMahasiswa != null) {
						try {
							@SuppressWarnings("rawtypes")
							Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(
									biodataCalonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU,
									mahasiswa.getJurusan(), false);

							int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
									biodataCalonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1,
									detailBiayas, false, false);

							if (countPengaturanBulanan > 0) {

								detailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
										biodataCalonMahasiswa, session, 1,
										ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, false, false);

							}

							if (!detailBiayas.isEmpty()) {
								Kegiatan kegiatan = biodataCalonMahasiswa
										.ambilKegiatans(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
								Double biaya = 0.0;
								for (Object o : detailBiayas) {
									if (o instanceof PengaturanPembayaranBulanan) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
										Double jumlah = pengaturanPembayaranBulanan.getNominal();
										biaya += jumlah;
									} else if (o instanceof DetailBiaya) {
										DetailBiaya detailBiaya = (DetailBiaya) o;

										Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
										biaya += jumlah;
									}
								}
								biayamasuk = biaya;
							} else {
								biayamasuk = 0.0;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadMahasiswa.java:511");
							biayamasuk = 0.0;
						}
					}

					if (biayamasuk < 0.01) {
						try {
							int currentSemester = 1;
							@SuppressWarnings("rawtypes")
							Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
									currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

							int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
									ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, currentSemester, detailBiayas, false,
									false);

							if (countPengaturanBulanan > 0) {

								detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
										currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA,
										countPengaturanBulanan > 0 ? "-1" : null, true, false);

							}

							if (!detailBiayas.isEmpty()) {
								Kegiatan kegiatan = mahasiswa.ambilKegiatans(currentSemester,
										ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
								Collection<DetailKegiatan> detailKegiatans = kegiatan == null
										|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(true);
								Double biaya = 0.0;
								for (Object o : detailBiayas) {
									if (o instanceof PengaturanPembayaranBulanan) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
										Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans,
												mahasiswa, currentSemester, pengaturanPembayaranBulanan);
										biaya += jumlah;
									} else if (o instanceof DetailBiaya) {
										DetailBiaya detailBiaya = (DetailBiaya) o;

										Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
										biaya += jumlah;
									}
								}
								biayamasuk = biaya;
							} else {
								biayamasuk = 0.0;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadMahasiswa.java:559");
							biayamasuk = 0.0;
						}
					}

					row.createCell(47).setCellValue(biayamasuk);

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
							} catch (Exception e) {
								// FIX "hang selamanya": sebelumnya try besar ini TIDAK punya catch di level luar,
								// sehingga exception yang lolos dari try/catch per-item di dalamnya akan menembus
								// keluar Runnable.run() tanpa pernah men-set label.setValue(""), membuat popup
								// progres macet selamanya. Sekarang ditangkap dan ditampilkan sebagai error.
								Common.tampilErrorJikaAdmin(e);
								label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
										"pengambilan data Mahasiswa dari Neo Feeder", null, e,
										new String[] {
												"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
												"Pastikan data Mahasiswa terkait sudah tersinkron dengan benar.",
												"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
										.replace("\n", " "));
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
