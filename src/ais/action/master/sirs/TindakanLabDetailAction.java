package ais.action.master.sirs;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataTindakanBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanLabDetail;
import ais.database.model.sirs.TransaksiMedis;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class TindakanLabDetailAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private AmbilDataTindakanBanbox searchtindakan;

	private MyTextbox nama;
	private MyTextbox keterangan;
	private AmbilDataTindakanBanbox tindakan;
	private MyTextbox normal;
	private MyTextbox satuan;

	private boolean edit = false;
	private boolean delete = false;

	private TindakanLabDetail tindakanLabDetail;
	private Toolbarbutton add;

	private String SUMBER = TransaksiMedis.SUMBER_LAB;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		if (execution.getParameter("sumber") != null && !execution.getParameter("sumber").trim().equalsIgnoreCase("")) {
			SUMBER = execution.getParameter("sumber").trim();
		}

		System.out.println("SUMBER => " + SUMBER + " =============================");

		if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_LAB)) {
			searchtindakan.setTindakanLab(true);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(null);
			searchtindakan.setTindakanRadiologi(null);
			searchtindakan.setTindakanVk(null);
			searchtindakan.setTindakanRenalUnit(null);
			searchtindakan.setTindakanGizi(null);
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_OPERASI)) {
			searchtindakan.setTindakanLab(null);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(true);
			searchtindakan.setTindakanRadiologi(null);
			searchtindakan.setTindakanVk(null);
			searchtindakan.setTindakanRenalUnit(null);
			searchtindakan.setTindakanGizi(null);
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_RADIOLOGI)) {
			searchtindakan.setTindakanLab(null);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(null);
			searchtindakan.setTindakanRadiologi(true);
			searchtindakan.setTindakanVk(null);
			searchtindakan.setTindakanRenalUnit(null);
			searchtindakan.setTindakanGizi(null);
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_VK)) {
			searchtindakan.setTindakanLab(null);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(null);
			searchtindakan.setTindakanRadiologi(null);
			searchtindakan.setTindakanVk(true);
			searchtindakan.setTindakanRenalUnit(null);
			searchtindakan.setTindakanGizi(null);
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_RENAL_UNIT)) {
			searchtindakan.setTindakanLab(null);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(null);
			searchtindakan.setTindakanRadiologi(null);
			searchtindakan.setTindakanVk(null);
			searchtindakan.setTindakanRenalUnit(true);
			searchtindakan.setTindakanGizi(null);
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_GIZI)) {
			searchtindakan.setTindakanLab(null);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(null);
			searchtindakan.setTindakanRadiologi(null);
			searchtindakan.setTindakanVk(null);
			searchtindakan.setTindakanRenalUnit(null);
			searchtindakan.setTindakanGizi(true);
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_LAIN)) {
			searchtindakan.setTindakanLab(null);
			searchtindakan.setTindakanGizi(null);
			searchtindakan.setTindakanOperasi(null);
			searchtindakan.setTindakanRadiologi(null);
			searchtindakan.setTindakanVk(null);
			searchtindakan.setTindakanRenalUnit(null);
			searchtindakan.setTindakanGizi(null);
		}

		searchtindakan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class TindakanLabDetailRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TindakanLabDetail tindakanLabDetail = (TindakanLabDetail) arg1;

			new Label(tindakanLabDetail.getTindakan() == null ? "" : tindakanLabDetail.getTindakan().getNama())
					.setParent(arg0);
			new Label(tindakanLabDetail.getTindakan() == null
					|| tindakanLabDetail.getTindakan().getJenisTindakan() == null ? ""
							: tindakanLabDetail.getTindakan().getJenisTindakan().getNama())
					.setParent(arg0);
			RevisiHelper.createNewRevisi(TindakanLabDetail.class, tindakanLabDetail, tindakanLabDetail.getNama())
					.setParent(arg0);
			new Label(tindakanLabDetail.getNormal()).setParent(arg0);
			new Label(tindakanLabDetail.getSatuan()).setParent(arg0);
			new Label(tindakanLabDetail.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(tindakanLabDetail);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(tindakanLabDetail);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
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

	public void onAdd(Event event) throws Exception {
		init(new TindakanLabDetail());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TindakanLabDetail tindakanLabDetail) {
		this.tindakanLabDetail = tindakanLabDetail;
		addWindow.setTitle("Pendataan Tindakan Detail " + SUMBER);
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Nama Tindakan (" + SUMBER + ")"));

		if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_LAB)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(true, null, null, null, null, null));
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_OPERASI)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(null, true, null, null, null, null));
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_RADIOLOGI)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(null, null, true, null, null, null));
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_VK)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(null, null, null, true, null, null));
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_RENAL_UNIT)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(null, null, null, null, true, null));
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_GIZI)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(null, null, null, null, null, true));
		} else if (SUMBER.equalsIgnoreCase(TransaksiMedis.SUMBER_LAIN)) {
			row.appendChild(tindakan = new AmbilDataTindakanBanbox(null, null, null, null, null, null));
		}
		tindakan.setValue(tindakanLabDetail.getTindakan() == null ? "" : tindakanLabDetail.getTindakan().toString());
		tindakan.setAttribute("tindakan", tindakanLabDetail.getTindakan());
		tindakan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Nama Tindakan Detail (" + SUMBER + ")"));
		row.appendChild(nama = new MyTextbox(tindakanLabDetail.getNama() == null ? "" : tindakanLabDetail.getNama()));
		nama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				tindakanLabDetail.getKeterangan() == null ? "" : tindakanLabDetail.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Batas Normal")));
		row.appendChild(normal = new MyTextbox(tindakanLabDetail.getNormal()));
		normal.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan")));
		row.appendChild(satuan = new MyTextbox(tindakanLabDetail.getSatuan()));
		satuan.setWidth("90%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (tindakan.getAttribute("tindakan") == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Nama Tindakan terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Nama Tindakan yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Nama Detail Tindakan Laboratorium terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan Nama Detail Tindakan; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (normal.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Batas Normal terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan nilai Batas Normal; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tindakanLabDetail.getId() != null) {
			tindakanLabDetail = (TindakanLabDetail) session.load(TindakanLabDetail.class, tindakanLabDetail.getId());

		}

		tindakanLabDetail.setTindakan((Tindakan) tindakan.getAttribute("tindakan"));
		tindakanLabDetail.setNormal(normal.getValue());
		tindakanLabDetail.setSatuan(satuan.getValue());
		tindakanLabDetail.setNama(nama.getValue());
		tindakanLabDetail.setKeterangan(keterangan.getValue());
		tindakanLabDetail.setSumber(SUMBER);

		Common.refreshSaveOrUpdate(session, tindakanLabDetail);

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<TindakanLabDetail> tindakanLabDetail = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tindakanLabDetail);
		grid.setRowRenderer(new TindakanLabDetailRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Tindakan tindakan = (Tindakan) this.searchtindakan.getAttribute("tindakan");

		Criteria criteria = session.createCriteria(TindakanLabDetail.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add(tindakan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tindakan", tindakan))

				.add(SUMBER.equals(TransaksiMedis.SUMBER_LAB)
						? Restrictions.or(Restrictions.eq("sumber", SUMBER), Restrictions.isNull("sumber"))
						: Restrictions.eq("sumber", SUMBER));
		return criteria;
	}

}
