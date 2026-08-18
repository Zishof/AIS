package ais.action.master.helper.generic;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AmbilDataPerkuliahanBandbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataTugasKelompok extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	private MyTextbox nama;

	private Tbmuser tbmuser;
	private Perkuliahan perkuliahan;

	public AmbilDataTugasKelompok(Perkuliahan perkuliahan) {
		super();
		this.perkuliahan = perkuliahan;
		tbmuser = Common.getCurrentUser();
		display();
		onSearchDefault(null);
	}

	class TugasKelompokRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final TugasKelompok tugasKelompok = (TugasKelompok) arg1;
			arg0.setAttribute("tugasKelompok", tugasKelompok);
			final Radio checkbox = new Radio();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (banboxPerkuliahan != null) {
						if (banboxPerkuliahan.getAttribute("perkuliahan") == null) {
							MyMessageboxConfig.show("Mohon maaf, perkuliahan belum dipilih. Langkah yang dapat dilakukan: (1) pilih perkuliahan dari daftar atau gunakan fitur pencarian; (2) pastikan data perkuliahan sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											banboxPerkuliahan.focus();
										}
									});
							return;
						}
					}

					if (eventListener != null) {

						if (banboxPerkuliahan != null && banboxPerkuliahan.getAttribute("perkuliahan") != null) {
							tugasKelompok.setPerkuliahan((Perkuliahan) banboxPerkuliahan.getAttribute("perkuliahan"));
						}

						Event myEvent = new Event("myEvent", event.getTarget(), tugasKelompok);
						eventListener.onEvent(myEvent);
					}
					AmbilDataTugasKelompok.this.detach();
				}
			});

			final Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new ais.ui.util.MyHtml("<h2>" + tugasKelompok.getJudul() + "</h2><br>" + tugasKelompok.getNama())
					.setParent(vbox);

			final Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hbox, tugasKelompok.getId(),
					LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN, LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN, false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false, null, false, false);

			String olehId = tugasKelompok.getOlehId();
			String oleh = tugasKelompok.getOleh();

			Common.infoDiuploadOleh(olehId, oleh, arg0);

		}

	}

	private Paging paging;
	private AmbilDataPerkuliahanBandbox banboxPerkuliahan;

	public void display() {

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(this);
		radiogroup.setHeight("100%");
		radiogroup.setWidth("100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		paging.setParent(mySouth);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();

		columns.setParent(searchgrid);

		if (perkuliahan == null) {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
		} else {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);

		}
		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul / Isi Tugas"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		if (perkuliahan == null) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Perkuliahan"));
			row.appendChild(banboxPerkuliahan = new AmbilDataPerkuliahanBandbox());
			banboxPerkuliahan.setReadonly(true);
			banboxPerkuliahan.setAttribute("perkuliahan", perkuliahan);
			banboxPerkuliahan.setValue(perkuliahan == null ? "" : Common.getDeskripsiPerkuliahan(perkuliahan));
			banboxPerkuliahan.setWidth("90%");
		}

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		center.setTitle("Pilih tugas kelompok yang sebelumnya pernah di-buat :");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(myCenter1);

		columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataTugasKelompok.this.detach();
			}
		});
		cancel.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order, Session session) {

		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		Criteria criteria = session.createCriteria(TugasKelompok.class);

		DashboardTimelinePertemuan.createCriteriaDosen(dosen, session, criteria, true, true, true, false, false, false,
				false, false, false);

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.ne("judul", "")
				: Restrictions.or(Restrictions.ilike("judul", nama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		Common.initPaging(initCriteria(false, session), paging);

		List<TugasKelompok> myTugasKelompok = initCriteria(true, session).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(myTugasKelompok);
		grid.setRowRenderer(new TugasKelompokRenderer());
		grid.setModelCheckMobile(strset);
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
