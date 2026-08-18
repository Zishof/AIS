package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	protected Combobox searchJenisSemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchpilihan;

	private List<MyCheckboxConfig> kolom = new ArrayList<MyCheckboxConfig>();

	private MyIntbox searchmulai;

	private MyIntbox searchsampai;

	public DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("200px");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisSemester.appendChild(comboitem);

		searchJenisSemester.setSelectedItem(comboitem);
		searchJenisSemester.setReadonly(true);
		row.appendChild(searchJenisSemester);
		searchJenisSemester.setWidth("90%");
		searchJenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		searchpilihan = new Combobox();
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Prodi Lulus");
		comboitem.setValue("prodi_lulus");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi I");
		comboitem.setValue("prodi_1");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi II");
		comboitem.setValue("prodi_2");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi III");
		comboitem.setValue("prodi3");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi IV");
		comboitem.setValue("prodi4");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi V");
		comboitem.setValue("prodi5");
		searchpilihan.appendChild(comboitem);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
		row.appendChild(searchpilihan);
		searchpilihan.setWidth("90%");
		searchpilihan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		searchpilihan.setReadonly(true);
		searchpilihan.setSelectedIndex(1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(searchmulai = new MyIntbox(0));
		searchmulai.setWidth("90%");
		searchmulai.setWidth("90%");
		searchmulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(searchsampai = new MyIntbox(300));
		searchsampai.setWidth("90%");
		searchsampai.setWidth("90%");
		searchsampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		String[] col = new String[] { "jenis_seleksi-jenisSeleksi-Jenis Seleksi",
				"gelombang_pendaftaran-gelombangPendaftaran-Gelombang", "program-program-Program",
				"paket_registrasi_mahasiswa-paket-Paket", "jenjang-jenjang-Jenjang",
				"status_nikah-statusNikah-Status Nikah", "status_awal_mahasiswa-statusAwalMahasiswa-Kelompok",
				"jenis_sekolah_mahasiswa_baru-jenisSekolah-Jenis Pendidikan",
				"jurusan_sekolah_mahasiswa_baru-jurusanSekolah-Jurusan Pendidikan",
				"jenis_seleksi-jenisSeleksi-Jenis Seleksi", "nama_sekolah_asal-namaSekolahAsal-Nama Pendidikan",
				"tahun_kelulusan-tahunKelulusan-Tahun Lulus", "kewarganegaraan-kewarganegaraan-Warga Negara",
				"asal_negara-asalNegara-Negara", "propinsi_calon-propinsiCalon-Propinsi",
				"kota_calon-kotaCalon-Kota/Kabupaten", "kecamatan_calon_wilayah-kecamatanCalon-Kecamatan",
				"totalskor-totalSkor-Skor", "infokampusdarimana-infoKampusDariMana-Info", "agama-agama-Agama",
				"pekerjaan_orang_tua-pekerjaanAyah-Pekerjaan Ortu",
				"pendidikan_orang_tua-pendidikanOrtu-Pendidikan Ortu",
				"pendapatan_ortu-pendapatanOrtu-Pendapatan Ortu" };

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
			}
		});

		refresh.setParent(hbox);
		print.setParent(hbox);

		for (String s : col) {

			row = new MyFormRow();
			row.setParent(rows);

			String[] ss = s.split("-");
			MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(ss[2]);
			checkboxConfig.setValue(ss[0] + "-" + ss[1]);
			checkboxConfig.setParent(row);
			kolom.add(checkboxConfig);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				String tahunAkademik = (String) (DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.this.tahunAkademik
										.getSelectedItem().getValue());
				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				if (tahunAkademik == null) {
					return;
				}

				String tahun = tahunAkademik.substring(0, 4);
				DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.this.angkatan.setValue(tahun);

				Session session = HibernateUtil.currentSession();

				List<List<Object[]>> jurusansSemua = new ArrayList<List<Object[]>>();
				List<List> generalValueObjectsSemua = new ArrayList<List>();
				List<String> namadata = new ArrayList<String>();

				int maxColoumn = 0;
				int totalData = 0;
				for (MyCheckboxConfig checkboxConfig : kolom) {
					if (checkboxConfig.isChecked()) {
						String[] val = checkboxConfig.getValue().toString().split("-");
						String kolomGroup = val[1];
						String namaKolom = val[0];

						namadata.add(checkboxConfig.getLabel());

						List<Object[]> jurusans = new ArrayList<Object[]>();

						List generalValueObjects = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.groupProperty(kolomGroup))
								.add(Restrictions.isNotNull(kolomGroup))
								.add(tahun == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahun", Integer.parseInt(tahun)))
								.addOrder(Order.asc(kolomGroup))
								.setFirstResult(searchmulai.getValue() == null ? 0 : searchmulai.getValue().intValue())
								.setMaxResults(searchsampai.getValue() == null ? 0 : searchsampai.getValue().intValue())
								.list();

						Collections.sort(generalValueObjects);

						if (generalValueObjects.size() > maxColoumn) {
							maxColoumn = generalValueObjects.size();
						}

						String pilihan = (String) searchpilihan.getSelectedItem().getValue();

						String subSql = "select aaa.id , \n"
								+ "sum(case when (ver.uploaded is null or ver.uploaded=false) then 1 else 0 end) as belum_upload,  \n"
								+ "sum(case when ver.uploaded then 1 else 0 end) as telah_upload,  \n"
								+ "sum(case when (ver.verified is null or ver.verified=false) then 1 else 0 end) as belum_verified,  \n"
								+ "sum(case when ver.verified then 1 else 0 end) as telah_verified,  \n"
								+ "count(aaa.id) as total  \nfrom biodata_calon_mahasiswa aaa    \n"
								+ "inner join biodata_calon_mahasiswa_punya_verifikasi_berkas ver on (ver.biodata_calon_mahasiswa=aaa.id)  \n"
								+ "inner join gelombang_punya_verifikasi gel on (gel.gelombang = aaa.gelombang_pendaftaran and ver.verifikasi_kelengkapan_calon_mahasiswa=gel.verifikasi  ) \n"
								+ "inner join verifikasi_kelengkapan_calon_mahasiswa ver_data on (ver_data.id = gel.verifikasi)  \n"
								+ "inner join jurusan x on (aaa.prodi_1 = x.id  )      \n"
								+ "inner join fakultas y on (y.id = x.fakultas)       \nwhere ver_data.aktif "
								+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
										: "and aaa.semester_mulai='" + searchJenisSemester.getSelectedItem().getValue()
												+ "' ")
								+ (program == null ? "" : " and aaa.program = '" + program + "'")
								+ (jurusan == null ? "" : " and aaa." + pilihan + " = " + jurusan.getId())
								+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
								+ " and aaa.tahun = " + tahun + " group by aaa.id";

						String sql = "select max(y.nama) as fakultas, max(x.nama) as jurusan, ";

						for (Object obj : generalValueObjects) {
							if (obj != null && !obj.toString().isEmpty()) {
								if (obj instanceof GeneralValueObject) {
									GeneralValueObject generalValueObject = (GeneralValueObject) obj;
									if (generalValueObject.getNama() != null) {
										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " and sub_q.belum_upload>=sub_q.total then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim())
												+ " Belum Upload\", ";
										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " and sub_q.telah_upload<sub_q.total and sub_q.telah_upload>0 then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim())
												+ " Sebagian Belum Upload\", ";
										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " and sub_q.telah_upload>=sub_q.total then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim())
												+ " Telah Upload\", ";

										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " and sub_q.belum_verified>=sub_q.total then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim())
												+ " Belum Verified\", ";
										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " and sub_q.telah_verified<sub_q.total and sub_q.telah_verified>0 then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim())
												+ " Sebagian Belum Verified\", ";
										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " and sub_q.telah_verified>=sub_q.total then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim())
												+ " Telah Verified\", ";

										sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
												+ " then 1 else 0 end) as \""
												+ Common.getBahasaConfig(generalValueObject.getNama().trim()) + "\", ";
									}
								} else {
									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " and sub_q.belum_upload>=sub_q.total then 1 else 0 end) as \"" + obj
											+ " Belum Upload\", ";

									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " and sub_q.telah_upload<sub_q.total and sub_q.telah_upload>0 then 1 else 0 end) as \""
											+ obj + " Sebagian Belum Upload\", ";

									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " and sub_q.telah_upload>=sub_q.total then 1 else 0 end) as \"" + obj
											+ " Telah Upload\", ";

									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " and sub_q.belum_verified>=sub_q.total then 1 else 0 end) as \"" + obj
											+ " Belum Verified\", ";

									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " and sub_q.telah_verified<sub_q.total and sub_q.telah_verified>0 then 1 else 0 end) as \""
											+ obj + " Sebagian Belum Verified\", ";

									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " and sub_q.telah_verified>=sub_q.total then 1 else 0 end) as \"" + obj
											+ " Telah Verified\", ";

									sql += "sum(case when aaa." + namaKolom
											+ (obj instanceof String ? " ilike '"
													+ org.apache.commons.lang3.StringUtils.replace(
															org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
													+ "'" : " = " + obj.toString())
											+ " then 1 else 0 end) as \"" + obj + "\", ";
								}
							}
						}

						sql += "sum(case when aaa." + namaKolom
								+ " is not null and sub_q.belum_upload>=sub_q.total then 1 else 0 end) as \"Belum Upload\", ";
						sql += "sum(case when aaa." + namaKolom
								+ " is not null and sub_q.telah_upload<sub_q.total and sub_q.telah_upload>0 then 1 else 0 end) as \"Sebagian Belum Upload\", ";
						sql += "sum(case when aaa." + namaKolom
								+ " is not null and sub_q.telah_upload >= sub_q.total then 1 else 0 end) as \"Telah Upload\", ";

						sql += "sum(case when aaa." + namaKolom
								+ " is not null and sub_q.belum_verified>=sub_q.total then 1 else 0 end) as \"Belum Verified\", ";
						sql += "sum(case when aaa." + namaKolom
								+ " is not null and sub_q.telah_verified<sub_q.total and sub_q.telah_verified>0 then 1 else 0 end) as \"Sebagian Belum Verified\", ";
						sql += "sum(case when aaa." + namaKolom
								+ " is not null and sub_q.telah_verified>=sub_q.total then 1 else 0 end) as \"Telah Verified\", ";

						sql += " sum(case when aaa." + namaKolom
								+ " is not null then 1 else 0 end) as total from biodata_calon_mahasiswa aaa  "
								+ " inner join (" + subSql + ") sub_q on (sub_q.id=aaa.id)"
								+ " inner join jurusan x on (aaa." + pilihan + " = x.id  )    "
								+ "inner join fakultas y on (y.id = x.fakultas)      where 1=1 "
								+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
										: "and aaa.semester_mulai='" + searchJenisSemester.getSelectedItem().getValue()
												+ "' ")
								+ (program == null ? "" : " and aaa.program = '" + program + "'")
								+ (jurusan == null ? "" : " and aaa." + pilihan + " = " + jurusan.getId())
								+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
								+ " and aaa.tahun = " + tahun + " group by x.fakultas,aaa." + pilihan
								+ " order by max(y.nama), max(x.nama) ";

						System.out.println(sql);
						jurusans = Common.ambilSql(sql);
						jurusansSemua.add(jurusans);
						generalValueObjectsSemua.add(generalValueObjects);

						totalData += jurusans.size();
						totalData += 3;
					}
				}

				spreadsheet = new ais.ui.util.MySpreadsheet();
Common.clear(center);spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns((maxColoumn * 7) + 9);
				spreadsheet.setMaxrows(totalData + 5);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI VERIFIKASI MAHASISWA BARU " + searchpilihan.getValue().toUpperCase() + "\n"
								+ (fakultas == null ? "" : fakultas.getNama().toUpperCase() + "\n")
								+ (jurusan == null ? "" : jurusan.getNama().toUpperCase() + "\n") + " TAHUN AKADEMIK "
								+ tahunAkademik + " "
								+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
										: searchJenisSemester.getSelectedItem().getValue())
								+ "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()));
				final String color = "#000000";

				int rowIndex = 0;

				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, true);

				for (int indexData = 0; indexData < generalValueObjectsSemua.size(); indexData++) {

					rowIndex += 7;

					List generalValueObjects = generalValueObjectsSemua.get(indexData);
					List<Object[]> jurusans = jurusansSemua.get(indexData);

					String namaData = namadata.get(indexData);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 0, namaData.toUpperCase());

					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex - 1, 0, rowIndex - 1,
							(generalValueObjects.size() * 7) + 4, true);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Fakultas");
					Utils.setColumnWidth(sheet, 0, 200);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jurusan");
					Utils.setColumnWidth(sheet, 1, 200);

					int colIndex = 2;
					for (Object obj : generalValueObjects) {
						if (colIndex > 250) {
							break;
						}
						if (obj != null && !obj.toString().isEmpty()) {
							if (obj instanceof GeneralValueObject) {
								GeneralValueObject generalValueObject = (GeneralValueObject) obj;
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6,
										generalValueObject.getNama());
							} else {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, obj);
							}

							ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 6, true);

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Belum Upload");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Sebagian Upload");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Telah Upload");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 3, "Belum Verifikasi");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 4,
									"Sebagian Verifikasi");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 5, "Telah Verifikasi");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 6, "Total");
							Utils.setColumnWidth(sheet, colIndex, 70);
							Utils.setColumnWidth(sheet, colIndex + 1, 70);
							Utils.setColumnWidth(sheet, colIndex + 2, 70);
							Utils.setColumnWidth(sheet, colIndex + 3, 70);
							Utils.setColumnWidth(sheet, colIndex + 4, 70);
							Utils.setColumnWidth(sheet, colIndex + 5, 70);
							Utils.setColumnWidth(sheet, colIndex + 6, 70);
							colIndex += 7;
						}
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, "Total");

					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, true);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Belum Upload");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Sebagian Upload");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Telah Upload");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 3, "Belum Verifikasi");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 4, "Sebagian Verifikasi");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 5, "Telah Verifikasi");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 6, "Total");

					Utils.setColumnWidth(sheet, colIndex, 70);
					Utils.setColumnWidth(sheet, colIndex + 1, 70);
					Utils.setColumnWidth(sheet, colIndex + 2, 70);
					Utils.setColumnWidth(sheet, colIndex + 3, 70);
					Utils.setColumnWidth(sheet, colIndex + 4, 70);
					Utils.setColumnWidth(sheet, colIndex + 5, 70);
					Utils.setColumnWidth(sheet, colIndex + 6, 70);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(0, rowIndex, (generalValueObjects.size() * 7) + 8, rowIndex + 1),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
						ais.ui.util.EcampusUtil.setBold(sheet,
								new Rect(0, rowIndex, (generalValueObjects.size() * 7) + 8, rowIndex + 1), true);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.java:713");

					}

					rowIndex += 2;
					colIndex = 0;

					String namaFakultas = "";
					String namaProdi = "";
					Integer[] nilaisBelumUpload = new Integer[generalValueObjects.size()];
					Integer[] nilaisSebagianUpload = new Integer[generalValueObjects.size()];
					Integer[] nilaisTelahUpload = new Integer[generalValueObjects.size()];
					Integer[] nilaisBelumVerif = new Integer[generalValueObjects.size()];
					Integer[] nilaisSebagianVerif = new Integer[generalValueObjects.size()];
					Integer[] nilaisTelahVerif = new Integer[generalValueObjects.size()];
					Integer[] nilais = new Integer[generalValueObjects.size()];

					Integer totalBelumUpload = 0;
					Integer totalSebagaianUpload = 0;
					Integer totalTelahUpload = 0;
					Integer totalBelumVerif = 0;
					Integer totalSebagianVerif = 0;
					Integer totalTelahVerif = 0;
					Integer total = 0;
					for (Object[] objects : jurusans) {
						if (objects[0] != null) {
							if (!namaFakultas.equals(objects[0].toString())) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
								namaFakultas = objects[0].toString();
							} else {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
							}

							if (!namaProdi.equals(objects[1].toString())) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1].toString());
								namaProdi = objects[1].toString();
							} else {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
							}
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tidak pilih Fakultas");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
									"Tidak pilih " + Common.getBahasaConfig("Jurusan"));
						}

						colIndex = 2;
						int index = 0;
						for (Object generalValueObject : generalValueObjects) {
							if (colIndex > 250) {
								break;
							}
							if (generalValueObject != null && !generalValueObject.toString().isEmpty()) {
								if (nilaisBelumUpload[index] == null) {
									nilaisBelumUpload[index] = 0;
								}
								Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex]))
										.intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisBelumUpload[index] += nilai0;
								colIndex++;

								if (nilaisSebagianUpload[index] == null) {
									nilaisSebagianUpload[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisSebagianUpload[index] += nilai0;
								colIndex++;

								if (nilaisTelahUpload[index] == null) {
									nilaisTelahUpload[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisTelahUpload[index] += nilai0;
								colIndex++;

								if (nilaisBelumVerif[index] == null) {
									nilaisBelumVerif[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisBelumVerif[index] += nilai0;
								colIndex++;

								if (nilaisSebagianVerif[index] == null) {
									nilaisSebagianVerif[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisSebagianVerif[index] += nilai0;
								colIndex++;

								if (nilaisTelahVerif[index] == null) {
									nilaisTelahVerif[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisTelahVerif[index] += nilai0;
								colIndex++;

								if (nilais[index] == null) {
									nilais[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilais[index] += nilai0;
								colIndex++;

								index++;
							}
						}

						Integer td = ((Number) (objects[objects.length - 7] == null ? 0 : objects[objects.length - 7]))
								.intValue();
						totalBelumUpload += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 6] == null ? 0 : objects[objects.length - 6]))
								.intValue();
						totalSebagaianUpload += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 5] == null ? 0 : objects[objects.length - 5]))
								.intValue();
						totalTelahUpload += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 4] == null ? 0 : objects[objects.length - 4]))
								.intValue();
						totalBelumVerif += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 3] == null ? 0 : objects[objects.length - 3]))
								.intValue();
						totalSebagianVerif += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 2] == null ? 0 : objects[objects.length - 2]))
								.intValue();
						totalTelahVerif += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1]))
								.intValue();
						total += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);

						rowIndex++;
					}

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(0, rowIndex - jurusans.size(), (generalValueObjects.size() * 7) + 8, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.java:874");
					}

					try {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
						colIndex = 2;
						for (int i = 0; i < nilais.length; i++) {
							int jum = nilaisBelumUpload[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilaisSebagianUpload[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilaisTelahUpload[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilaisBelumVerif[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilaisSebagianVerif[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilaisTelahVerif[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilais[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;
						}

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, totalBelumUpload);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, totalTelahUpload);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, totalSebagaianUpload);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, totalBelumVerif);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4, totalSebagianVerif);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5, totalTelahVerif);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, total);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.java:917");

					}

					try {

						ais.ui.util.EcampusUtil.setBold(sheet,
								new Rect(colIndex, rowIndex, (generalValueObjects.size() * 7) + 8, rowIndex), true);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaruBerdasarkanPerMahasiswa.java:925");

					}

				}

				Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

			}
		});

	}
}
