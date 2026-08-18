package ais.action.master.sekolah.psb;

import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.helper.AktifitasJadwalUjianPSBHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TampilanUjianCalonSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4568230033051291139L;
	protected AktifitasJadwalUjianPSBHelper aktifitasJadwalUjianPSBHelper = new AktifitasJadwalUjianPSBHelper();

	public TampilanUjianCalonSiswa() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TampilanUjianCalonSiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	private boolean materiBol = true;
	private boolean ujianBol = true;
	private boolean tugasBol = true;

	@SuppressWarnings("unchecked")
	public void init(final CalonSiswa calonSiswa) throws Exception {

		final MyCheckboxConfig materiPil = new MyCheckboxConfig("Materi");
		final MyCheckboxConfig ujianPil = new MyCheckboxConfig("Ujian");
		final MyCheckboxConfig tugasPil = new MyCheckboxConfig("Tugas");
		final Textbox cari = new Textbox();
		EventListener eventListenerReload = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				materiBol = materiPil.isChecked();
				ujianBol = ujianPil.isChecked();
				tugasBol = tugasPil.isChecked();

				init(calonSiswa);
			}
		};

		try {
			Common.clear(this);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/TampilanUjianCalonSiswa.java:89");
			// TODO: handle exception
		}
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		boolean mobile = Common.isMobile();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setHeight(mobile ? "70px" : "35px");
		Box vbox = mobile ? new Vbox() : new Hbox();
		vbox.setParent(north);

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari :")));
		hbox.appendChild(cari);
		cari.setCols(15);

		final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		hbox.appendChild(button);

		hbox = new Hbox();
		hbox.setParent(vbox);
		hbox.appendChild(materiPil);
		hbox.appendChild(ujianPil);
		hbox.appendChild(tugasPil);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		materiPil.addEventListener("onClick", eventListenerReload);
		ujianPil.addEventListener("onClick", eventListenerReload);
		tugasPil.addEventListener("onClick", eventListenerReload);

		materiPil.setChecked(materiBol);
		ujianPil.setChecked(ujianBol);
		tugasPil.setChecked(tugasBol);

		final TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();

		Session session = HibernateUtil.currentSession();
		RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
				.createCriteria(RuangGelombangPendaftaranPsbPSB.class).add(Restrictions.eq("calonSiswa", calonSiswa))
				.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (ruangGelombangPendaftaranPsbPSB != null) {
			List<JadwalUjianPSB> jadwalUjianPSBs = session.createCriteria(JadwalUjianPSB.class)
					.add(Restrictions.eq("ujianPSB", ruangGelombangPendaftaranPsbPSB.getRuangPSB().getUjianPSB()))
					.add(Restrictions.or(
							Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()),
							Restrictions.isNull("gelombangPendaftaranPsb")))
					.addOrder(Order.asc("waktuMulai")).list();

			if (!jadwalUjianPSBs.isEmpty()) {
				List<Pertemuan> pertemuans = session.createCriteria(PertemuanPunyaUjian.class)
						.setProjection(Projections.groupProperty("pertemuan")).createAlias("pertemuan", "pertemuan")

						.add(Restrictions.in("pertemuan.jadwalUjianPSB", jadwalUjianPSBs)).list();

				for (final Pertemuan pertemuan : pertemuans) {
					String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());

					keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
							? "00.00-00.00"
							: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
									+ (pertemuan.getWaktuSelesai() == null ? "00.00" : pertemuan.getWaktuSelesai())));

					pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());

				}
			}
		}

		System.out.println(" pertemuans size ->" + pertemuansa.size());

		Tbmuser tbmuser = Common.getCurrentUser();
		hbox.appendChild(DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuansa));

		final Rows rows = new Rows();
		final Paging paging = new Paging();

		Borderlayout subBorderlayout = new Borderlayout();
		subBorderlayout.setParent(center);

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(subcenter);
		grid.appendChild(rows);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth(Common.isMobile() ? "40%" : "20%");

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rows);
				Row row = new Row();row.setValign("top");
				row.setStyle("border:0px;background: transparent;font-size: x-small;");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				vbox.setWidth("100%");
				final Label label;
				final boolean refresh = arg0 != null && arg0.getTarget() == button;
				vbox.appendChild(label = new Label(ais.common.Common.getBahasaConfig("Ambil data ...")));
				Image img;
				vbox.appendChild(img = new Image("/loading_icon.gif"));
				img.setWidth("90%");

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						Common.clear(rows);
						TampilanELearningAction.loadDataMateri(cari, rows, paging, refresh, materiPil, ujianPil,
								tugasPil, label, tbmuser, pertemuansa, true, true);
					}
				});

			}
		};

		Common.createDefaultTimer(eventListener);
		button.addEventListener("onClick", eventListener);
		cari.addEventListener("onOK", eventListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				detach();
			}
		});
		cancel.setParent(toolbar);

	}

}
