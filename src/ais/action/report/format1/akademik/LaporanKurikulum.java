package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.MatakuliahPrasyarat;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanKurikulum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	// Untuk Laporan Kurikulum
	private Combobox kurikulumFakultas;
	private Combobox kurikulumJurusan;
	private Combobox kurikulumJenis;

	private Center center;
	private Toolbar toolbar;
	private Combobox smt;
	private Integer s = null;
	private Kurikulum k = null;

	public LaporanKurikulum() {
		super();
		try {
			initKurikulum();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kurikulum", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKurikulum(Kurikulum k, Integer s) {
		super();
		this.k = k;
		this.s = s;
		try {
			initKurikulum();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kurikulum", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKurikulum(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKurikulum();
		init();
	}

	@SuppressWarnings("unchecked")
	private void initKurikulum() throws Exception {

		kurikulumJenis = new Combobox();
		kurikulumFakultas = new Combobox();
		kurikulumJurusan = new Combobox();
		Common.initFakultasDanJurusan(kurikulumFakultas, kurikulumJurusan, null, null);

		class SearchKurikulumEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(kurikulumJenis);
				kurikulumJenis.setSelectedItem(null);
				if (kurikulumJurusan.getSelectedItem() == null) {
					return;
				}
				Jurusan myJurusan = (Jurusan) (kurikulumJurusan.getSelectedItem() == null ? null
						: kurikulumJurusan.getSelectedItem().getValue());

				List<Kurikulum> kurikulums = HibernateUtil.currentSession().createCriteria(Kurikulum.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan)).list();

				for (Kurikulum kurikulum : kurikulums) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
					comboitem.setValue(kurikulum);
					comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
							+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
					kurikulumJenis.appendChild(comboitem);
				}
			}

		}

		SearchKurikulumEventListener searchKurikulumEventListener = new SearchKurikulumEventListener();
		kurikulumJurusan.addEventListener("onChange", searchKurikulumEventListener);
		searchKurikulumEventListener.onEvent(null);
	}

	private void init() {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKurikulum(event);

			}
		};

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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		row.appendChild(kurikulumFakultas);
		kurikulumFakultas.setWidth("90%");
		kurikulumFakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		row.appendChild(kurikulumJurusan);
		kurikulumJurusan.setWidth("90%");
		kurikulumJurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum *"));
		row.appendChild(kurikulumJenis);
		kurikulumJenis.setWidth("90%");
		kurikulumJenis.addEventListener("onChange", eventListener);
		kurikulumJenis.setReadonly(true);

		if (k != null) {
			Common.selectComboItem(true, kurikulumJenis, k);
			kurikulumJenis.setDisabled(true);
			kurikulumJurusan.getParent().setVisible(false);
			kurikulumFakultas.getParent().setVisible(false);

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(smt = new Combobox());
		smt.setWidth("90%");
		smt.addEventListener("onChange", eventListener);
		smt.setReadonly(true);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua Smt");
		comboitem.setValue(null);
		smt.appendChild(comboitem);
		smt.setSelectedItem(comboitem);

		for (int i = 1; i <= 14; i++) {
			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Smt " + i);
			comboitem.setValue(i);
			smt.appendChild(comboitem);
		}

		if (s != null) {
			Common.selectComboItem(smt, s);
			smt.setDisabled(true);

			west.setWidth("0px");
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (kurikulumJenis.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu kurikulum", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Kurikulum", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKurikulum(arg0);
			}
		}));

		onKurikulum(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (kurikulumJenis.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu kurikulum",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Kurikulum kurikulum = (Kurikulum) kurikulumJenis.getSelectedItem().getValue();

		Fakultas fakultas = kurikulum.getJurusan().getFakultas();
		Jurusan jurusan = kurikulum.getJurusan();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas", fakultas.getNama());
		parameters.put("jurusan", jurusan.getNama());
		parameters.put("jenis", kurikulum.getId());

		Integer s = (Integer) (smt.getSelectedItem() == null || smt.getSelectedItem().getValue() == null ? null
				: smt.getSelectedItem().getValue());

		parameters.put("smt", s == null ? -1 : s);

		Session session = HibernateUtil.currentSession();
		List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(
				session.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "matakuliah")
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"),
								Restrictions.eq("matakuliah.aktif", true)))
						.add(Restrictions.eq("kurikulum", kurikulum))

						.add(s == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", s))

						.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.kode")),
				KurikulumPunyaMatakuliah.class);
		List<Map> maps = new ArrayList<Map>();
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
			Map map = new HashMap();
			map.put("id", kurikulumPunyaMatakuliah.getMatakuliah().getId());
			map.put("semester", kurikulumPunyaMatakuliah.getSemester());
			map.put("kode", kurikulumPunyaMatakuliah.getMatakuliah().getKode());
			map.put("nama", kurikulumPunyaMatakuliah.getMatakuliah().getNama());
			map.put("sks", kurikulumPunyaMatakuliah.getMatakuliah().getSks());
			map.put("status", kurikulumPunyaMatakuliah.getMatakuliah().getStatus());
			String prasyarat = "";

			List<MatakuliahPrasyarat> matakuliahPrasyarats = ConstantValues.simpleList(
					session.createCriteria(MatakuliahPrasyarat.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("matakuliah", kurikulumPunyaMatakuliah.getMatakuliah())),
					MatakuliahPrasyarat.class);
			for (MatakuliahPrasyarat matakuliahPrasyarat : matakuliahPrasyarats) {
				// Prasyarat opsional: lewati baris tanpa MK prasyarat.
				if (matakuliahPrasyarat.getMatakuliahPrasyarat() == null) {
					continue;
				}

				String mk = matakuliahPrasyarat.getMatakuliahPrasyarat().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat().getNama();
				if (matakuliahPrasyarat.getMatakuliahPrasyarat2() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat3() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat4() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat5() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat6() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat7() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat8() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat9() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getNama();
				}
				if (matakuliahPrasyarat.getMatakuliahPrasyarat10() != null) {
					mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getNama();
				}

				prasyarat += prasyarat.isEmpty() ? mk : "; " + mk;
			}
			map.put("prasyarat", prasyarat);
			map.put("tahun", kurikulum.getTahun());
			map.put("nama_kur", kurikulum.getNama());
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onKurikulum(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Kurikulum",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kurikulum", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
