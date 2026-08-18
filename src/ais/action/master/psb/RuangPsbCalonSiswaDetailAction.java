package ais.action.master.psb;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataCalonSiswaBanyak;
import ais.action.master.sekolah.CalonSiswaAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class RuangPsbCalonSiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private RuangPSB ruangPSB;
	private MyGrid grid;

	private Textbox nama;

	public RuangPsbCalonSiswaDetailAction(RuangPSB ruangPSB) {
		super();
		this.ruangPSB = ruangPSB;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(RuangPsbCalonSiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class CalonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		public CalonSiswaRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final RuangGelombangPendaftaranPsbPSB ruangPaketPSB = (RuangGelombangPendaftaranPsbPSB) data;
			final CalonSiswa calonSiswa = ruangPaketPSB.getCalonSiswa();

			CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(RuangGelombangPendaftaranPsbPSB.class, ruangPaketPSB, calonSiswa.getNama())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(CalonSiswa.class, calonSiswa,
					calonSiswa.getTanggalLahir() == null ? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
							: Common.dateFormat2.get().format(calonSiswa.getTanggalLahir()))
					.setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(calonSiswa.getNoRegistrasi()).setParent(a);

			new Label(calonSiswa.getSiswa() == null ? "" : calonSiswa.getSiswa().getNomorInduk()).setParent(a);
			new Label(calonSiswa.getSiswa() == null ? "" : calonSiswa.getSiswa().getNomorIndukNasional()).setParent(a);

			new Label(calonSiswa.getSekolahAsal()).setParent(arg0);
			new Label(calonSiswa.getNamaAyah()).setParent(arg0);
			new Label(calonSiswa.getNamaIbu()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											HibernateUtil.currentSession()
													.createSQLQuery("delete from ruang_paket_pmb where calon_siswa="
															+ calonSiswa.getId())
													.executeUpdate();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<RuangGelombangPendaftaranPsbPSB> calonSiswas = initCriteria(true).list();

		ListModel strset = new SimpleListModel(calonSiswas);
		grid.setRowRenderer(new CalonSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Calon Siswa Manual", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<CalonSiswa> calonSiswas = ConstantValues
						.simpleList(HibernateUtil.currentSession().createCriteria(RuangGelombangPendaftaranPsbPSB.class)
								.addOrder(Order.asc("id")).setProjection(Projections.property("calonSiswa.id"))
								.add(Restrictions.eq("ruangPSB", ruangPSB)), CalonSiswa.class, false);

				AmbilDataCalonSiswaBanyak window = new AmbilDataCalonSiswaBanyak(calonSiswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataCalonMhs) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<CalonSiswa> calonSiswas = (List<CalonSiswa>) dataCalonMhs.getData();

								if (calonSiswas != null) {
									Session session = HibernateUtil.currentSession();
									for (CalonSiswa calonSiswa : calonSiswas) {
										RuangGelombangPendaftaranPsbPSB ruangPaketPSB = (RuangGelombangPendaftaranPsbPSB) session
												.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
												.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1)
												.uniqueResult();
										if (ruangPaketPSB == null) {
											ruangPaketPSB = new RuangGelombangPendaftaranPsbPSB();
										}
										ruangPaketPSB.setCalonSiswa(calonSiswa);
										ruangPaketPSB.setRuangPSB(ruangPSB);
										Common.refreshSaveOrUpdate(session, ruangPaketPSB);
									}

									loadData(null);
								}
							}
						});

					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());

		toolbar.appendChild(new Label("Nama/No.Reg/Ujian : "));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(8);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				
				
				Common.createDefaultTimer(new EventListener() {
					
					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				return session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
						.setProjection(Projections.property("calonSiswa")).addOrder(Order.asc("id"))
						.add(Restrictions.eq("ruangPSB", ruangPSB));
			}
		}, CalonSiswaAction.contents);
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(groupbox);
		grid.getPagingChild().setMold("os");
		grid.getPagingChild().setDetailed(true);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Lahir");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Registrasi, Ujian, NIS");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asal Sekolah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ayah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ibu");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(RuangGelombangPendaftaranPsbPSB.class).createAlias("calonSiswa", "calonSiswa")
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("calonSiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("calonSiswa.noUjian", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("calonSiswa.noRegistrasi", nama.getValue().trim(),
												MatchMode.ANYWHERE))))
				.addOrder(Order.asc("id")).add(Restrictions.eq("ruangPSB", ruangPSB));
	}

}
