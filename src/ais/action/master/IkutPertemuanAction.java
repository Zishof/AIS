package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.action.master.helper.FilterLanjutHelper;

public class IkutPertemuanAction extends GenericAutowireComposer implements DataLoader {

	/**
	 * 
	 */
	protected static final long serialVersionUID = -5779730267402400328L;
	protected MyGrid grid;
	protected Paging paging;
	protected Textbox searchnama;
	protected Combobox tahunAjaran;
	protected Combobox jenisSemester;
	protected AmbilDataMatakuliahBanbox searchmatakuliah;
	protected AmbilDataDosenBanbox searchdosen;
	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	protected Combobox searchhari;

	protected Mahasiswa mahasiswa;
	protected Dosen dosen;

	protected North mynorth;
	protected Tbmuser tbmuser;

	protected Integer semesterPendek;

	protected AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper;

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
		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		mahasiswa = Common.getCurrentUser().getMahasiswa();
		dosen = Common.getCurrentUser().getDosen();

		aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(mahasiswa, null, true);

		if (dosen != null) {
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setDisabled(true);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenisSemester.appendChild(comboitem);

		Common.initPrograms(searchprogram);

		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }

		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Common.selectComboItem(jenisSemester, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		Common.generateTahunAjaran(tahunAjaran);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	        FilterLanjutHelper.setup(comp);
}

	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			final Perkuliahan perkuliahan = (Perkuliahan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			int banyak = 1;
			try {
				banyak = Integer
						.parseInt(Common.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			aktifitasPerkuliahanHelper.initDetail(perkuliahan, groupbox, 0, banyak);
			detail.appendChild(groupbox);

			arg0.appendChild(Common.getDeskripsiPerkuliahanHbox(perkuliahan));

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		String jenisSemester = this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue().toString();

		if (mahasiswa != null) {
			Criteria criteria = session.createCriteria(Detailperkuliahan.class)
					// .add(Restrictions.eq("persetujuan",
					// Detailperkuliahan.DISETUJUI))
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.setProjection(Projections.property("ikutiPerkuliahan"))
					.createCriteria("ikutiPerkuliahan", Criteria.LEFT_JOIN)

					.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.eq("statusSemesterPendek", semesterPendek))

					.createAlias("matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("merupakan_paralel"),
							Restrictions.eq("merupakan_paralel", false)))

					.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAjaran", tahunAkademik))
					.add(jenisSemester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("ganjilGenap", jenisSemester))

					.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

					.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

					.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
									Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")))))

					.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

					.add(Restrictions.ilike("matakuliah.nama", searchnama.getValue(), MatchMode.ANYWHERE));

			if (order)
				criteria.addOrder(Order.desc("tahunAjaran"));
			if (order)
				criteria.addOrder(Order.asc("matakuliah.nama"));
			if (order)
				criteria.addOrder(Order.asc("semester"));
			if (order)
				criteria.addOrder(Order.asc("kelas"));
			return criteria;
		} else {
			Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.sqlRestriction("1!=1"))
					.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.eq("statusSemesterPendek", semesterPendek))

					.createAlias("matakuliah", "matakuliah")

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

					.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAjaran", tahunAkademik))
					.add(jenisSemester == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("ganjilGenap", jenisSemester))

					.add(dosen == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen)))

					.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
									Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")))))

					.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

					.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()));

			if (order)
				criteria.addOrder(Order.desc("tahunAjaran"));
			if (order)
				criteria.addOrder(Order.asc("matakuliah.nama"));
			if (order)
				criteria.addOrder(Order.asc("semester"));
			if (order)
				criteria.addOrder(Order.asc("kelas"));

			criteria.add(Restrictions.ilike("matakuliah.nama", searchnama.getValue(), MatchMode.ANYWHERE))

					.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

					.createCriteria("jurusan", Criteria.LEFT_JOIN)
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Perkuliahan> perkuliahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(perkuliahan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

}
