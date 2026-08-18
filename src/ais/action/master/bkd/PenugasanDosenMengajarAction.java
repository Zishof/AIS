package ais.action.master.bkd;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.DosenMengajarHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyComboitemConfig;

public class PenugasanDosenMengajarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	protected AmbilDataDosenBanbox searchdosen;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;

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

		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Common.initPrograms(searchprogram);
		Tbmuser tbmuser = Common.getCurrentUser();
		// System.out.println(users);
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			// Common.selectComboItem(searchdosen, dosen);
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setDisabled(true);
		}

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PenugasanDosenMengajarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenugasanDosenMengajar penugasanDosenMengajar = (PenugasanDosenMengajar) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						DosenMengajarHelper dosenMengajarHelper = new DosenMengajarHelper();
						dosenMengajarHelper.display(true, penugasanDosenMengajar, detail);

					}
				}
			});

			Vbox rowParalel = new Vbox();
			rowParalel.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(penugasanDosenMengajar.getDosen()).setParent(rowParalel);
			new Label(penugasanDosenMengajar.getDosen().getNama()).setParent(rowParalel);

			RevisiHelper.createNewRevisi(PenugasanDosenMengajar.class, penugasanDosenMengajar,
					penugasanDosenMengajar.getTahunAkademik()).setParent(arg0);

			new Label(penugasanDosenMengajar.getProgram()).setParent(arg0);
			new Label(penugasanDosenMengajar.getSemester()).setParent(arg0);
			new Label(penugasanDosenMengajar.getJurusan() == null ? "" : penugasanDosenMengajar.getJurusan().getNama())
					.setParent(arg0);
			new Label(penugasanDosenMengajar.getSks() == null ? ""
					: Common.numberFormat.get().format(penugasanDosenMengajar.getSks())).setParent(arg0);

			final Vbox vboxKeterangan = new Vbox();
			vboxKeterangan.setParent(arg0);

			final Hbox hboxKeterangan = new Hbox();
			hboxKeterangan.setParent(vboxKeterangan);

			new Label("No.: " + penugasanDosenMengajar.getKode()).setParent(hboxKeterangan);
			new Label("Tgl.: " + (penugasanDosenMengajar.getTanggalSuratTugas() == null ? ""
					: Common.dateFormat1.get().format(penugasanDosenMengajar.getTanggalSuratTugas())))
					.setParent(hboxKeterangan);
			new Label("Tmt.: " + (penugasanDosenMengajar.getTmtSuratTugas() == null ? ""
					: Common.dateFormat1.get().format(penugasanDosenMengajar.getTmtSuratTugas()))).setParent(hboxKeterangan);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vboxKeterangan);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, penugasanDosenMengajar.getId(),
					"sk_penugasan_pengajaran_dosen", "SK", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false);
		}

	}

	public Criteria initCriteria(boolean order) {

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("dosen", searchdosen.getAttribute("myValue"));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenugasanDosenMengajar.class)

				.add(Restrictions.isNotNull("sks")).add(criterion);

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", searchJenisSemester.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenugasanDosenMengajar> penugasanDosenMengajar = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penugasanDosenMengajar);
		grid.setRowRenderer(new PenugasanDosenMengajarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
