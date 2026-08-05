package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AssesorAction extends MyDetail implements DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	private MyCheckboxConfig hanyaYgAktif;

	public AssesorAction(AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.asesorPenunjangKinerjaDosen = asesorPenunjangKinerjaDosen;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AssesorAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class AsesorRenderer extends ais.ui.util.MyRowRenderer {

		public AsesorRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Asesor asesor = (Asesor) data;

			new AsesorPegawaiAction(asesor).setParent(row);

			CommonMedia.tampilkanGambarKecil(asesor.getTbmuser()).setParent(row);

			new Label(asesor.getTbmuser() == null ? "" : asesor.getTbmuser().getUserId()).setParent(row);

			RevisiHelper.createNewRevisi(Asesor.class, asesor,
					asesor.getTbmuser() == null ? "" : asesor.getTbmuser().getUserNama()).setParent(row);

			int jmlpegawai = ((Number) HibernateUtil.currentSession().createCriteria(AsesorPegawai.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("asesor", asesor)).uniqueResult())
							.intValue();
			new Label(Common.numberFormat.get().format(jmlpegawai)).setParent(row);

			final MyTextbox keterangan = new MyTextbox(asesor.getKeterangan() == null ? "" : asesor.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					asesor.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (asesor));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(asesor.getAktif());
			checkbox.setParent(row);
			row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					asesor.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(asesor);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
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

											Common.refreshDelete(asesor);

											loadData(null);

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
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<Asesor> asesors = session.createCriteria(Asesor.class)
				.add(!hanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id"))
				.add(Restrictions.eq("asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen)).list();

		ListModel strset = new SimpleListModel(asesors);
		grid.setRowRenderer(new AsesorRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar " + asesorPenunjangKinerjaDosen.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pengguna", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Tbmuser> tbmusers = session.createCriteria(Asesor.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.groupProperty("tbmuser")).add(Restrictions.isNotNull("tbmuser"))
						.add(Restrictions.eq("asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen)).list();

				AmbilDataTbmuserBanyak ambilDataTbmuserBanyak = new AmbilDataTbmuserBanyak(tbmusers);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTbmuserBanyak);
				ambilDataTbmuserBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();

						for (Tbmuser tbmuser : tbmusers) {
							Asesor asesor = new Asesor();
							asesor.setTbmuser(tbmuser);
							asesor.setKeterangan("");
							asesor.setAsesorPenunjangKinerjaDosen(asesorPenunjangKinerjaDosen);
							Common.refreshSaveOrUpdate(asesor);
						}

						loadData(null);
					}
				});
				ambilDataTbmuserBanyak.setWidth("850px");
				ambilDataTbmuserBanyak.setHeight("97%");
				ambilDataTbmuserBanyak.setVisible(true);
				ambilDataTbmuserBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		hanyaYgAktif = new MyCheckboxConfig("Hanya yg aktif");
		hanyaYgAktif.setChecked(true);
		hanyaYgAktif.setParent(toolbar);
		hanyaYgAktif.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		String[] contents = new String[] { "id", "asesor", "asesor.tbmuser.userNama", "pegawai", "pegawai.nama",
				"aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {

				return HibernateUtil.currentSession().createCriteria(AsesorPegawai.class)
						.createAlias("asesor", "asesor").createAlias("asesor.tbmuser", "tbmuser")
						.add(!hanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("asesor.aktif"),
										Restrictions.eq("asesor.aktif", true)))
						.add(Restrictions.eq("asesor.asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
						.addOrder(Order.asc("tbmuser.userNama"));
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AsesorPegawai.class, contents);
		upload.setVisible(edit && delete);
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Id Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jml Peg");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
