package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.IOException;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiBiodataCalonMahasiswaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class BiodataCalonMahasiswaAction extends GenericAutowireComposer implements DataLoader, DataCriteria {

	/**
	 *
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	private Paging paging;

	private MyGrid grid;
	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchtahun;
	private Combobox searchprogram;
	private Combobox searchjenjang;

	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private boolean delete = false;

	private ais.action.master.pmb.BiodataCalonMahasiswaAction addWindow;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BiodataCalonMahasiswa.class, this, "nim", "nama",
				"alamat", "tahun", "tempatlahir", "formatedtanggallahir", "kelamin", "jurusan", "status", "jenjang",
				"semesterMulai", "program", "agama", "warganegara", "negara", "statusAwalBiodataCalonMahasiswa",
				"noIjazah1", "noIjazah2", "noAkta1", "noAkta2", "tahunWisuda", "tahunLulus", "semesterLulus",
				"judulSkripsi");
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		Common.initPrograms(searchprogram);

		Common.generateTahunAjaran(searchtahun);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		MyToolbarbuttonConfig history = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		history.setTooltiptext("Telusuri riwayat perubahan, penghapusan, dan pemulihan Biodata Calon Mahasiswa");
		history.setVisible(edit);
		history.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiBiodataCalonMahasiswaHelper revisiHelper = new RevisiBiodataCalonMahasiswaHelper(
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(event);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		Common.appendKeToolbar(history, add, comp);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (order)
			criteria.addOrder(Order.desc("tahunAkademik"));
		if (order)
			criteria.addOrder(Order.asc("noRegistrasi"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("noRegistrasi", searchnim.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("noUjian", searchnim.getValue(), MatchMode.ANYWHERE)))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchtahun.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchtahun.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));

		return criteria;
	}

	class BiodataCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						BiodataCalonMahasiswaAction.this.session.setAttribute("selectedBiodataCalonMahasiswa",
								biodataCalonMahasiswa);
						MyIframe include = new MyIframe("/pages/master/kalender_biodataCalonMahasiswa.zul");
						include.setHeight("500px");
						include.setWidth("100%");
						detail.appendChild(include);
					}
				}
			});

			final org.zkoss.zul.Image myImage = new org.zkoss.zul.Image("/img/administrator-icon_default.png");
			myImage.setWidth("100%");
			// myImage.setHeight("100px");
			arg0.appendChild(myImage);
			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				FotoBiodataCalonMahasiswa fotobiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) streamingSession
						.createCriteria(FotoBiodataCalonMahasiswa.class)
						.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
						.uniqueResult();

				if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getLink() != null
						&& fotobiodataCalonMahasiswa.getLink().toLowerCase().contains("dropbox")) {
					myImage.setSrc(fotobiodataCalonMahasiswa.dropboxLinkRaw());
				} else if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getGdrive() != null) {
					myImage.setSrc(fotobiodataCalonMahasiswa.thumbnailGDriveUrl());
				} else if (fotobiodataCalonMahasiswa != null) {
					try {
						AImage aImage = new AImage(fotobiodataCalonMahasiswa.ambilFile());
						myImage.setContent(aImage);
					} catch (IOException e) {
						Common.tampilErrorJikaAdmin(e);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e1) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/BiodataCalonMahasiswaAction.java:216");
			}

			RevisiHelper
					.createNewRevisi(BiodataCalonMahasiswa.class, biodataCalonMahasiswa,
							biodataCalonMahasiswa.getNoRegistrasi())
					.setParent(arg0);

			new Label(biodataCalonMahasiswa.getNama()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getTahunAkademik()).setParent(arg0);

			new Label(biodataCalonMahasiswa.getKewarganegaraan() == null ? ""
					: biodataCalonMahasiswa.getKewarganegaraan()).setParent(arg0);

			new Label(biodataCalonMahasiswa.getAsalNegara() == null ? ""
					: biodataCalonMahasiswa.getAsalNegara().getNamaNegara()).setParent(arg0);

			new Label((biodataCalonMahasiswa.getProdi1() == null
					|| biodataCalonMahasiswa.getProdi1().getFakultas() == null ? ""
							: biodataCalonMahasiswa.getProdi1().getFakultas().getNama())
					+ "-"
					+ (biodataCalonMahasiswa.getProdi2() == null
							|| biodataCalonMahasiswa.getProdi2().getFakultas() == null ? ""
									: biodataCalonMahasiswa.getProdi2().getFakultas().getNama())).setParent(arg0);
			new Label((biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getNama())
					+ "-"
					+ (biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getNama()))
							.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow = new ais.action.master.pmb.BiodataCalonMahasiswaAction();
					addWindow.init(biodataCalonMahasiswa);
					addWindow.setHeight("95%");
					addWindow.setWidth("900px");
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

											Common.refreshDelete(biodataCalonMahasiswa);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		if (searchnama == null) {
			return;
		}

		List<BiodataCalonMahasiswa> biodataCalonMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))

				.list();

		ListModel strset = new SimpleListModel(biodataCalonMahasiswa);
		grid.setRowRenderer(new BiodataCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

}
