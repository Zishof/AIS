package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.AnggotaAction;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Anggota;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

public class LaporanKartuAnggota extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private Paging paging = new Paging();
	private Textbox cari;
	private MyCheckboxConfig depan;
	private MyCheckboxConfig belakang;
	private MyGrid grid;

	Map<Long, Anggota> map = new java.util.HashMap<Long, Anggota>();

	public LaporanKartuAnggota() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kartu Anggota", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
		borderlayout1.setParent(west);

		North north = new North();
		north.setParent(borderlayout1);
		north.setHeight("140px");
		north.setBorder("none");

		MyGrid mygrid = new MyGrid();// grid.setOddRowSclass("non-odd");
		mygrid.setWidth("100%");
		mygrid.setParent(north);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("80px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		Row row = new Row();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Depan:"));
		row.appendChild(depan = new MyCheckboxConfig("Hal. Depan"));
		depan.setChecked(true);

		row = new Row();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Belakang:"));
		row.appendChild(belakang = new MyCheckboxConfig("Hal. Belakang"));
		belakang.setChecked(true);

		row = new Row();
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari : ")));
		cari = new Textbox();
		cari.setParent(row);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new Row();
		row.setParent(rows);

		MyButtonConfig button = new MyButtonConfig("Tampilkan");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		button = new MyButtonConfig("Cari Anggota");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Center center1 = new Center();
		center1.setParent(borderlayout1);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		South south1 = new South();
		south1.setParent(borderlayout1);
		south1.setHeight("40px");

		Vbox vbox = new Vbox();
		vbox.setParent(south1);

		paging.setParent(vbox);
		paging.setHeight("30px");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center1);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);
		column = new MyColumnConfig();
		column.setWidth("45px");
		column.setParent(columns);

		column = new MyColumnConfig("Foto");
		column.setWidth("65px");
		column.setParent(columns);

		column = new MyColumnConfig("Kode");
		column.setParent(columns);

		column = new MyColumnConfig("Nama");
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "library/kartu_anggota", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onSearchDefault(null);
	}

	@SuppressWarnings("unchecked")
	protected void onSearchDefault(Object object) {
		Common.initPaging(initCriteria(false), paging);
		List<Anggota> anggota = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		List<Anggota> anggotas = new ArrayList<Anggota>();
		anggotas.addAll(map.values());
		anggotas.addAll(anggota);
		ListModel strset = new SimpleListModel(anggotas);
		grid.setRowRenderer(new AnggotaRenderer());
		grid.setModelCheckMobile(strset);
	}

	class AnggotaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Anggota anggota = (Anggota) arg1;

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setChecked(map.keySet().contains(anggota.getId()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						map.put(anggota.getId(), anggota);
					} else {
						map.remove(anggota.getId());
					}
				}
			});

			if (anggota.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(anggota.getMahasiswa()).setParent(arg0);

			} else if (anggota.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(anggota.getDosen()).setParent(arg0);

			} else if (anggota.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(anggota.getPegawai()).setParent(arg0);

			} else if (anggota.getTbmuser() != null) {
				CommonMedia.tampilkanGambarKecil(anggota.getTbmuser()).setParent(arg0);

			} else {
				new Label("").setParent(arg0);
			}

			new Label(anggota.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Anggota.class, anggota, anggota.getNama()).setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Anggota.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));

		Criterion criterion = Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE);
		criterion = Restrictions.or(criterion, Restrictions.ilike("email", cari.getValue().trim(), MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("kode", cari.getValue().trim(), MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kodeIdentitas", cari.getValue().trim(), MatchMode.ANYWHERE));

		criteria.add(map.isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.not(Restrictions.in("id", map.keySet())))
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion);
		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		List list = new ArrayList();
		for (Anggota anggota : map.values()) {
			list.add(AnggotaAction.siapkanParemeter(anggota));
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters = AnggotaAction.siapkanParemeterGambar(parameters);
		parameters.put("belakang", belakang.isChecked());
		parameters.put("depan", depan.isChecked());
		parameters.put("maps", list);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "library/kartu_anggota",
							ais.ui.util.WaktuUtil.getDate(), null, toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kartu Anggota", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
