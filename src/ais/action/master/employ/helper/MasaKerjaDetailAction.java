package ais.action.master.employ.helper;

import java.io.File;
import java.time.Period;
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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.employ.MasaKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MasaKerjaDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private MasaKerja masaKerja;
	private Paging paging;
	private MyGrid grid;

	public MasaKerjaDetailAction(MasaKerja masaKerja) {
		super();
		this.masaKerja = masaKerja;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(MasaKerjaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	class MasaKerjaDetailRenderer extends ais.ui.util.MyRowRenderer {

		public MasaKerjaDetailRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {

			final Pegawai pegawai = (Pegawai) data;
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama())).setParent(arg0);
			a.appendChild(new Label(pegawai.getPendidikan() == null ? "" : pegawai.getPendidikan().getNama()));
			a.appendChild(new Label(pegawai.getTipePegawai() == null ? "" : pegawai.getTipePegawai().getNama()));
			a.appendChild(new Label(pegawai.getTipeMasaKerja() == null ? "" : pegawai.getTipeMasaKerja().getNama()));

			new Label(pegawai.getTanggalMulaiPengalanKerja() == null ? ""
					: pegawai.ambilMasaKerjaTahunPengalamanKerja() + " thn, "
							+ pegawai.ambilMasaKerjaBulanPengalamanKerja() + " bln")
					.setParent(arg0);

			new Label(pegawai.getTanggalmasukHonorer() == null ? ""
					: pegawai.ambilMasaKerjaTahunHonorer() + " thn, " + pegawai.ambilMasaKerjaBulanHonorer() + " bln")
					.setParent(arg0);

			new Label(pegawai.getTanggalmasukSemiTetap() == null ? ""
					: pegawai.ambilMasaKerjaTahunSemiTetap() + " thn, " + pegawai.ambilMasaKerjaBulanSemiTetap()
							+ " bln")
					.setParent(arg0);

			new Label(pegawai.getTanggalmasuk() == null ? ""
					: pegawai.ambilMasaKerjaTahun() + " thn, " + pegawai.ambilMasaKerjaBulan() + " bln")
					.setParent(arg0);

			
			Period period = MasaKerjaUtil.masaKerja(pegawai);
			new Label("Masa kerja " + period.getYears() + " tahun " + period.getMonths() + " bulan " + period.getDays()
					+ " hari").setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											pegawai.setMasaKerja(null);
											Common.refreshUpdate(pegawai);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Keterangan kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan tidak ada referensi yang masih menggunakan data ini; (3) apabila kesalahan masih berlanjut, mohon menghubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("code", kode.getValue().trim(), MatchMode.ANYWHERE));
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("mycode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class).add(critKode).add(critNama)
				.add(Restrictions.eq("masaKerja", masaKerja));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> masaKerjaDetails = masaKerja == null
				|| masaKerja.getId() == null
						? new ArrayList<Pegawai>()
						: ConstantValues
								.simpleList(
										initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
												.setFirstResult(Common.ROWS_COUNT_ON_PAGE
														* (paging == null ? 0 : paging.getActivePage())),
										Pegawai.class);

		ListModel strset = new SimpleListModel(masaKerjaDetails);
		grid.setRowRenderer(new MasaKerjaDetailRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	private void display() {

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Pegawai", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Pegawai> pegawais = session.createCriteria(Pegawai.class)
						.add(Restrictions.eq("masaKerja", masaKerja)).list();

				AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
				ambilDataPegawaiBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Pegawai pegawai : pegawais) {
							session.refresh(pegawai);
							pegawai.setMasaKerja(masaKerja);
							Common.refreshSaveOrUpdate(session, pegawai);
							session.flush();
						}

						loadData(null);
					}
				});
				ambilDataPegawaiBanyak.setWidth("90%");
				ambilDataPegawaiBanyak.setHeight("97%");
				ambilDataPegawaiBanyak.setVisible(true);
				ambilDataPegawaiBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Cetak Masa Kerja", "/img/svg/printer.svg");
		cetakSksDosen.setParent(toolbar);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					List<Pegawai> masaKerjaDetails = ConstantValues.simpleList(initCriteria(true), Pegawai.class);
					List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
					for (Pegawai pegawai : masaKerjaDetails) {
						Map<String, Object> map = new java.util.HashMap<String, Object>();
						map.put("thn_pengalaman_kerja", pegawai.getTanggalMulaiPengalanKerja() == null ? null
								: pegawai.ambilMasaKerjaTahunPengalamanKerja());
						map.put("bln_pengalaman_kerja", pegawai.getTanggalMulaiPengalanKerja() == null ? null
								: pegawai.ambilMasaKerjaBulanPengalamanKerja());
						map.put("thn_honor",
								pegawai.getTanggalmasukHonorer() == null ? null : pegawai.ambilMasaKerjaTahunHonorer());
						map.put("bln_honor",
								pegawai.getTanggalmasukHonorer() == null ? null : pegawai.ambilMasaKerjaBulanHonorer());
						map.put("thn_st", pegawai.getTanggalmasukSemiTetap() == null ? null
								: pegawai.ambilMasaKerjaTahunSemiTetap());
						map.put("bln_st", pegawai.getTanggalmasukSemiTetap() == null ? null
								: pegawai.ambilMasaKerjaBulanSemiTetap());
						map.put("thn_tetap", pegawai.getTanggalmasuk() == null ? null : pegawai.ambilMasaKerjaTahun());
						map.put("bln_tetap", pegawai.getTanggalmasuk() == null ? null : pegawai.ambilMasaKerjaBulan());

						map.put("total", MasaKerjaUtil.hitung(pegawai));

						Period period = MasaKerjaUtil.masaKerja(pegawai);

						map.put("total_thn", period.getYears());
						map.put("total_bln", period.getMonths());
						map.put("total_hari", period.getDays());

						map.put("kode", pegawai.getNama());
						maps.add(map);
					}
					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("masaKerja", masaKerja.getNama());
					parameters.put("maps", maps);
					File file = Report.generateFileReport(Report.PDF, parameters, "employ/Masa_Kerja",
							ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());

					MyWindow window = new MyWindow("Masa Kerja", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("95%");
					window.setWidth("95%");
					window.onModal();
					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

		});

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new MyToolbarbuttonConfig("", "/img/svg/search.svg"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pengalaman Kerja");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Honor");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Semi Tetap");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tetap");
		column.setWidth("12%");

		column = new Column();
		column.setAlign("right");
		column.setParent(columns);
		column.setLabel("Nilai Masa Kerja (MK)");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

}
