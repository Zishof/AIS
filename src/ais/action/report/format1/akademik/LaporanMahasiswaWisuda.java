package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Wisuda;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanMahasiswaWisuda extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	// private Combobox program;
	//
	// private Combobox semester;
	private Combobox wisuda;
	private MyCheckboxConfig tampilkanHanyaYangSudahDisetujui;
	// private Intbox angkatan;

	private Center center;

	private Toolbar toolbar;

	// private Label myTahunAngkatan;

	private Wisuda selectedWisuda;

	public LaporanMahasiswaWisuda() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Mahasiswa Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanMahasiswaWisuda(Wisuda wisuda) {
		super();
		this.selectedWisuda = wisuda;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Mahasiswa Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanMahasiswaWisuda(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		Common.insertCombo(wisuda = new Combobox(), "wisudaKe", "moto", Wisuda.class);
		wisuda.setReadonly(true);
		// semester = new Combobox();
		// for (int i = 1; i <= 21; i++) {
		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel(i + "");
		// comboitem.setValue(i);
		// semester.appendChild(comboitem);
		// }
		// Common.selectComboItem(semester, 1);

		// angkatan = new
		// Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		// program = new Combobox();
		// for (String strProgram : Common.programs.keySet()) {
		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel(strProgram);
		// comboitem.setValue(strProgram);
		// // program.appendChild(comboitem);
		// // if (strProgram.equals("Reguler------")) {
		// // program.setSelectedItem(comboitem);
		// // }
		// }
		// Common.checkProgramString(program);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.addEventListener("onChange", eventListener);

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		// row.appendChild(program);
		// program.setWidth("90%");
		// program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wisuda ke"));
		row.appendChild(wisuda);
		// wisuda.addEventListener("onChange", eventListener);

		if (selectedWisuda != null) {
			Common.selectComboItem(wisuda, selectedWisuda);
			wisuda.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan hanya yang sudah di setujui"));
		row.appendChild(tampilkanHanyaYangSudahDisetujui = new MyCheckboxConfig());
		// tampilkanHanyaYangSudahDisetujui.addEventListener("onClick",
		// eventListener);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", eventListener);
		print.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (wisuda.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih wisuda", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "LaporanWisuda", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		// if (fakultas.getSelectedItem() ==
		// null||fakultas.getSelectedItem().getValue() == null) {
		// // MyMessageboxConfig.show("Pilih " +
		// "Fakultas",
		// // "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		//
		// if (jurusan.getSelectedItem() ==
		// null||jurusan.getSelectedItem().getValue() == null) {
		// // MyMessageboxConfig.show("Pilih " +
		// "Jurusan",
		// // "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return null;
		// }

		if (wisuda.getSelectedItem() == null || wisuda.getSelectedItem().getValue() == null) {
			// MyMessageboxConfig.show("Pilih semester", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Wisuda wisuda = (Wisuda) this.wisuda.getSelectedItem().getValue();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		// if (tahunAkademik.getValue() == null) {
		// return null;
		// }

		// Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
		// || this.jurusan.getSelectedItem().getValue()==null ?
		// null
		// : this.jurusan.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("wisuda", wisuda == null || wisuda.getId() == null ? -1L : wisuda.getId());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("tampilkanHanyaYangSudahDisetujui", tampilkanHanyaYangSudahDisetujui.isChecked() ? 1L : 0L);
		// parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		// parameters.put("semester", semester.getSelectedItem().getValue());
		// parameters.put("tahunangkatan", angkatan.getValue() == null ? -1
		// : angkatan.getValue());

		// parameters.put("maps", generateDataDanImageAlbum());
		// parameters.put("program", program.getSelectedItem() ==
		// null||program.getSelectedItem().getValue() == null ? "-1"
		// : program.getSelectedItem().getValue());

		// parameters.put("tanggal", tanggal.getValue()==null?new
		// Date():tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		Session session = HibernateUtil.currentSession();
		List<PendaftaranWisuda> pendaftaranWisudas = session.createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("wisuda", wisuda)).createAlias("mahasiswa", "mahasiswa")
				.createAlias("mahasiswa.jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
				.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("jurusan.nama"))
				.addOrder(Order.asc("mahasiswa.nim")).list();
		List<Map> maps = new ArrayList<Map>();

		for (PendaftaranWisuda pendaftaranWisuda : pendaftaranWisudas) {
			Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();

			Integer semester = mahasiswa.getSemesterLulus() == null
					|| mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < 1
							? mahasiswa.currentSemester()
							: mahasiswa.getSemesterLulus();
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, false);

			Map map = new java.util.HashMap();
			Common.insertProperty(PendaftaranWisuda.class, pendaftaranWisuda, map, "", 1, "mahasiswa");
			map.put("semester", semester);
			map.put("nama_mahasiswa", mahasiswa.getNama());
			map.put("nama", mahasiswa.getNama());
			map.put("tahunangkatan", mahasiswa.getTahunangkatan());
			map.put("nim", mahasiswa.getNim());
			map.put("jurusan", mahasiswa.getJurusan().getNama());
			map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
			map.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
			map.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
			map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
			map.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
			map.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());
			map.put("toga", pendaftaranWisuda.getUkuranToga());
			map.put("toga_text",
					pendaftaranWisuda.getUkuranToga() == null ? ""
							: pendaftaranWisuda.getUkuranToga().equals(1) ? "S"
									: pendaftaranWisuda.getUkuranToga().equals(2) ? "M"
											: pendaftaranWisuda.getUkuranToga().equals(3) ? "L" : "XL");

			map.put("dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
					: krsMahasiswa.getDosenPa().getNama());
			map.put("nip_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
					: krsMahasiswa.getDosenPa().getCode());
			map.put("nidn_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
					: krsMahasiswa.getDosenPa().getNidn());

			map.put("nama_kaprodi",
					mahasiswa.getJurusan().getKaprodi() == null ? "" : mahasiswa.getJurusan().getKaprodi().getNama());
			map.put("nip_kaprodi",
					mahasiswa.getJurusan().getKaprodi() == null ? "" : mahasiswa.getJurusan().getKaprodi().getCode());
			map.put("nidn_kaprodi",
					mahasiswa.getJurusan().getKaprodi() == null ? "" : mahasiswa.getJurusan().getKaprodi().getNidn());

			map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
			map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
			map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

			map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
			map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
			map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

			map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
			map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
			map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

			map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
			map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
			map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

			map.put("nama_kajur",
					mahasiswa.getJurusan().getGrupJurusan() == null
							|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
			map.put("nip_kajur",
					mahasiswa.getJurusan().getGrupJurusan() == null
							|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
			map.put("nidn_kajur",
					mahasiswa.getJurusan().getGrupJurusan() == null
							|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
									: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());
			Double ipmhs = krsMahasiswa.getIps();
			Double ipkmhs = krsMahasiswa.getIpk();

			Integer sksmhss = krsMahasiswa.getSksYangDiambil();
			Integer sksmhs = krsMahasiswa.getSksk();
			map.put("ipk", ipkmhs);
			map.put("ips", ipmhs);
			map.put("sksk", sksmhs);
			map.put("sks", sksmhss);

			map.put("ip_kumulatif", ipkmhs);

			map.put("ip_semester", ipmhs);
			map.put("judulSkripsi", mahasiswa.getJudulSkripsi());
			map.put("tahun_masuk", mahasiswa.getTahunangkatan());
			map.put("tahun_lulus", mahasiswa.getTahunLulus());
			map.put("tanggalYudisium", mahasiswa.getTanggalYudisium());
			map.put("tanggalLulus", mahasiswa.getTanggalLulus());
			map.put("tempatlahir", mahasiswa.getTempatlahir());
			map.put("tanggallahir", mahasiswa.getTanggallahir());
			map.put("kelamin", mahasiswa.getKelamin());
			map.put("agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());
			String alamatlengkap = mahasiswa.getAlamat();
			Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
			map.put("judisium", judisium == null ? "" : judisium.getNama());
			map.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

			map.put("no_ijazah1", mahasiswa.getNoIjazah1());
			map.put("gelar", mahasiswa.getJurusan().getGelar());

			System.out.println("map => " + map);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = mahasiswa.getTanggalLulus() == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(mahasiswa.getTanggalLulus()));
			Period period = Period.between(dt, currentdate);
			System.out.println("Years " + period.getYears()); // Years 2
			System.out.println("Months " + period.getMonths()); // Months 1
			System.out.println("Days " + period.getDays()); // Days 11

			jurusan = mahasiswa.getJurusan();
			int workDays = 0;
			LocalDate jamesBirthDay = new LocalDate(mahasiswa.getTanggalKegiatanBelajarMengajar());
			LocalDate now = new LocalDate(mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
					: mahasiswa.getTanggalLulus());
			workDays = Days.daysBetween(jamesBirthDay, now).getDays();
			map.put("lama_sudi", workDays);

			map.put("masa_studi_dan_sisa", mahasiswa.ambilMasaStudi());

			map.put("masa_studi_tahun", period.getYears());
			map.put("masa_studi_semester", workDays / 183);

			map.put("masa_studi",
					period.getYears() + " tahun, " + period.getMonths() + " bulan, " + period.getDays() + " hari. ");

			map.put("masa_studi_tahun_info",
					period.getYears() + " (" + IndonesianNumberToWords.convert(period.getYears()) + ") tahun");
			map.put("nama_cap", Common.capitailizeWord(mahasiswa.getNama()));

			map.put("bahasa_pengantar", mahasiswa.getJurusan().getBahasaPengantar());
			map.put("nama_asli", mahasiswa.getNama());
			map.put("tempat_cap", Common.capitailizeWord(mahasiswa.getTempatlahir()));
			map.put("tempat", mahasiswa.getTempatlahir());
			map.put("tanggal_lahir", mahasiswa.getTanggallahirManual());
			map.put("nim", mahasiswa.getNim());
			map.put("jenjang_syarat", mahasiswa.getJenjang().getSyarat());
			map.put("jenjang", mahasiswa.getJenjang().getKeterangan());
			map.put("jenjang_en", mahasiswa.getJenjang().getKeteranganEn());
			map.put("tanggal_lulus_id", mahasiswa.getTanggalLulus() == null ? "..........."
					: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));

			map.put("tanggal_lulus_en", mahasiswa.getTanggalLulus() == null ? "..........."
					: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());
			int tanggal_tgl = calendar.get(Calendar.DATE);
			int tahun = calendar.get(Calendar.YEAR);

			map.put("tanggal_satuan_masuk", tanggal_tgl);
			map.put("bulan_satuan_masuk", Common.monthFormat2.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
			map.put("tahun_satuan_masuk", tahun);

			if (mahasiswa.getTanggalLulus() == null) {
				map.put("tanggal_satuan_lulus", "..");
				map.put("bulan_satuan_lulus", ".....");
				map.put("tahun_satuan_lulus", "....");

				map.put("tanggal_satuan_lulus_en", "..");
				map.put("bulan_satuan_lulus_en", ".....");
				map.put("tahun_satuan_lulus_en", "....");
			} else {
				calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mahasiswa.getTanggalLulus());
				tanggal_tgl = calendar.get(Calendar.DATE);
				tahun = calendar.get(Calendar.YEAR);

				map.put("tanggal_satuan_lulus", tanggal_tgl);
				map.put("bulan_satuan_lulus", mahasiswa.getTanggalLulus() == null ? ""
						: Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
				map.put("tahun_satuan_lulus", tahun);

				// map.put("tanggal_satuan_lulus_en", tanggal==1? );
				map.put("bulan_satuan_lulus_en", mahasiswa.getTanggalLulus() == null ? ""
						: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
				map.put("tahun_satuan_lulus_en", tahun);
			}

			map.put("jurusan", mahasiswa.getJurusan().getNama());
			map.put("jurusan_en", mahasiswa.getJurusan().getNamaEn());
			map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
			map.put("sk_akreditasi", mahasiswa.getJurusan().getNoSkAkreditasi());
			map.put("fakultas_en", mahasiswa.getJurusan().getFakultas().getNamaEn());
			map.put("gelar", mahasiswa.getJurusan().getGelar());
			map.put("gelar_singkat", mahasiswa.getJurusan().getSingkatanGelar());

			map.put("no_ijazah_1", mahasiswa.getNoIjazah1());
			map.put("no_ijazah_2", mahasiswa.getNoIjazah2());
			map.put("no_akta_1", mahasiswa.getNoAkta1());
			map.put("no_akta_2", mahasiswa.getNoAkta2());
			map.put("gelar_en", mahasiswa.getJurusan().getGelarEn());
			map.put("gelar_en_singkat", mahasiswa.getJurusan().getSingkatanGelarEn());

			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

			if (biodataMahasiswa != null) {

				map.put("ayah", biodataMahasiswa.getNamaAyah());
				map.put("ibu", biodataMahasiswa.getNamaIbu());

				if (!Common.checkIsStringNull(biodataMahasiswa.getRt())) {
					alamatlengkap += " Rt " + biodataMahasiswa.getRt();
				}
				if (!Common.checkIsStringNull(biodataMahasiswa.getRw())) {
					alamatlengkap += " Rw " + biodataMahasiswa.getRw();
				}
				if (!Common.checkIsStringNull(biodataMahasiswa.getDusun())) {
					alamatlengkap += " " + biodataMahasiswa.getDusun();
				}
				if (!Common.checkIsStringNull(biodataMahasiswa.getKelurahan())) {
					alamatlengkap += " " + biodataMahasiswa.getKelurahan();
				}
				if (biodataMahasiswa.getKecamatan() != null) {
					alamatlengkap += " " + biodataMahasiswa.getKecamatan().getNama();
				}
				if (biodataMahasiswa.getKota() != null) {
					alamatlengkap += " " + biodataMahasiswa.getKota().getNama();
				}
				if (biodataMahasiswa.getPropinsi() != null) {
					alamatlengkap += " " + biodataMahasiswa.getPropinsi().getNama();
				}
				map.put("nik", biodataMahasiswa.getNoIdentitas());
				Object[] hp = new Object[] { biodataMahasiswa.getHp(), biodataMahasiswa.getTeleponRumah() };
				String noHp = (hp[0] == null || hp[0].toString().trim().equals("08100000000000000000")
						|| hp[0].toString().trim().equals("0000000000") ? "" : hp[0])
						+ (hp[1] == null || hp[1].toString().trim().isEmpty()
								|| hp[1].toString().trim().equals("00000000000000000000")
								|| hp[1].toString().trim().equals("000000000")
										? ""
										: (hp[0] == null || hp[0].toString().trim().isEmpty()
												|| hp[0].toString().trim().equals("08100000000000000000")
												|| hp[0].toString().trim().equals("0000000000") ? "" : " / ") + hp[1]);
				map.put("noHp", noHp);
			} else {
				map.put("noHp", mahasiswa.getTelp());
			}
			map.put("alamatlengkap", alamatlengkap);
			map.put("email", mahasiswa.getEmail());
			maps.add(map);
		}

		parameters.put("maps", maps);
		pendaftaranWisudas = null;

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "LaporanWisuda",
							ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Mahasiswa Wisuda", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
