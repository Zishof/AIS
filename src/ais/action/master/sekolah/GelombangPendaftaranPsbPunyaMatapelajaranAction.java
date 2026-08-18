package ais.action.master.sekolah;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMatapelajaranSekolahBanyak;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GelombangPendaftaranPsbPunyaMatapelajaran;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GelombangPendaftaranPsbPunyaMatapelajaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private GelombangPendaftaranPsbPunyaMatapelajaran gelombangPendaftaranPsbPunyaMatapelajaran;
	private MyWindow addWindow;
	private MyGrid grid;

	private Combobox matapelajaranSekolah;
	private Combobox searchjurusansekolah;
	private Combobox gelombangPendaftaranPsb;
	private Combobox searchgelombangPendaftaranPsb;

	private boolean edit = true;
	private boolean delete = true;

	private GelombangPendaftaranPsb selectedGelombangPendaftaranPsb;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.insertCombo(matapelajaranSekolah = new Combobox(), "nama", MatapelajaranSekolah.class);
		Common.insertCombo(searchjurusansekolah, "nama", MatapelajaranSekolah.class);
		Common.insertCombo(gelombangPendaftaranPsb = new Combobox(), "nama", "keterangan",
				GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", "keterangan", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			selectedGelombangPendaftaranPsb = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			Common.selectComboItem(searchgelombangPendaftaranPsb, selectedGelombangPendaftaranPsb);
			searchgelombangPendaftaranPsb.setDisabled(true);
		}

		onSearchDefault(null);
	}

	class PilihanGelombangPendaftaranPsbRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GelombangPendaftaranPsbPunyaMatapelajaran gelombangPendaftaranPsbPunyaMatapelajaran = (GelombangPendaftaranPsbPunyaMatapelajaran) arg1;

			new Label(gelombangPendaftaranPsbPunyaMatapelajaran.getGelombangPendaftaranPsb().getNama()).setParent(arg0);
			new Label(gelombangPendaftaranPsbPunyaMatapelajaran.getMatapelajaranSekolah() == null ? ""
					: gelombangPendaftaranPsbPunyaMatapelajaran.getMatapelajaranSekolah().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(gelombangPendaftaranPsbPunyaMatapelajaran);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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
											Common.refreshDelete(gelombangPendaftaranPsbPunyaMatapelajaran);
											onSearchDefault(event);
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
			toolbar.setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		// init(new GelombangPendaftaranPsbPunyaMatapelajaran());
		// addWindow.setVisible(true);
		// addWindow.onModal();

		List<MatapelajaranSekolah> matapelajaranSekolahs = HibernateUtil.currentSession()
				.createCriteria(GelombangPendaftaranPsbPunyaMatapelajaran.class)
				.add(selectedGelombangPendaftaranPsb == null ? Restrictions.isNull("gelombangPendaftaranPsb")
						: Restrictions.eq("gelombangPendaftaranPsb", selectedGelombangPendaftaranPsb))
				.setProjection(Projections.groupProperty("matapelajaranSekolah")).list();

		AmbilDataMatapelajaranSekolahBanyak window = new AmbilDataMatapelajaranSekolahBanyak(matapelajaranSekolahs);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("870px");
		window.setHeight("90%");

		window.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<MatapelajaranSekolah> matapelajaranSekolahs = (List<MatapelajaranSekolah>) arg0.getData();

				if (matapelajaranSekolahs != null) {
					Session session = HibernateUtil.currentSession();
					for (MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

						GelombangPendaftaranPsbPunyaMatapelajaran gelombangPendaftaranPsbPunyaMatapelajaran = new GelombangPendaftaranPsbPunyaMatapelajaran();
						gelombangPendaftaranPsbPunyaMatapelajaran.setMatapelajaranSekolah(matapelajaranSekolah);
						gelombangPendaftaranPsbPunyaMatapelajaran
								.setGelombangPendaftaranPsb(selectedGelombangPendaftaranPsb);

						session.save(gelombangPendaftaranPsbPunyaMatapelajaran);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();
	}

	private void init(GelombangPendaftaranPsbPunyaMatapelajaran pilihanGelombangPendaftaranPsbPerJurusan) {
		this.gelombangPendaftaranPsbPunyaMatapelajaran = pilihanGelombangPendaftaranPsbPerJurusan;
		addWindow.setTitle(pilihanGelombangPendaftaranPsbPerJurusan.getId() == null ? "Tambah Matapelajaran" : "Ubah Matapelajaran");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " Sekolah"));
		Common.selectComboItem(matapelajaranSekolah,
				pilihanGelombangPendaftaranPsbPerJurusan.getMatapelajaranSekolah() == null ? null
						: pilihanGelombangPendaftaranPsbPerJurusan.getMatapelajaranSekolah());
		row.appendChild(matapelajaranSekolah);
		matapelajaranSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("GelombangPendaftaranPsb"));
		Common.selectComboItem(gelombangPendaftaranPsb,
				pilihanGelombangPendaftaranPsbPerJurusan.getGelombangPendaftaranPsb() == null ? null
						: pilihanGelombangPendaftaranPsbPerJurusan.getGelombangPendaftaranPsb());
		row.appendChild(gelombangPendaftaranPsb);
		gelombangPendaftaranPsb.setWidth("90%");

		if (selectedGelombangPendaftaranPsb != null) {
			Common.selectComboItem(gelombangPendaftaranPsb, selectedGelombangPendaftaranPsb);
			gelombangPendaftaranPsb.setDisabled(true);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();
		if (gelombangPendaftaranPsbPunyaMatapelajaran.getId() != null) {
			gelombangPendaftaranPsbPunyaMatapelajaran = (GelombangPendaftaranPsbPunyaMatapelajaran) session.load(
					GelombangPendaftaranPsbPunyaMatapelajaran.class, gelombangPendaftaranPsbPunyaMatapelajaran.getId());

		}

		gelombangPendaftaranPsbPunyaMatapelajaran
				.setMatapelajaranSekolah((MatapelajaranSekolah) matapelajaranSekolah.getSelectedItem().getValue());

		gelombangPendaftaranPsbPunyaMatapelajaran.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) gelombangPendaftaranPsb.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, gelombangPendaftaranPsbPunyaMatapelajaran);
		return true;
	}

	// matapelajaranSekolah

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<GelombangPendaftaranPsbPunyaMatapelajaran> gelombangPendaftaranPsbPunyaMatapelajaran = session
				.createCriteria(GelombangPendaftaranPsbPunyaMatapelajaran.class)

				.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
				.add(Restrictions.eq("matapelajaranSekolah.aktif", true))

				.add(searchgelombangPendaftaranPsb.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("gelombangPendaftaranPsb",
								searchgelombangPendaftaranPsb.getSelectedItem().getValue()))

				.add(searchjurusansekolah.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("matapelajaranSekolah", searchjurusansekolah.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(gelombangPendaftaranPsbPunyaMatapelajaran);
		grid.setRowRenderer(new PilihanGelombangPendaftaranPsbRenderer());
		grid.setModelCheckMobile(strset);

	}

}
