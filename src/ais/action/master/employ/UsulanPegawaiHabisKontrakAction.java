package ais.action.master.employ;

import java.time.Period;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.PegawaiAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.format1.payroll.LaporanPegawaiData;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class UsulanPegawaiHabisKontrakAction extends GenericAutowireComposer implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchstatus;
	private Combobox searchselisih;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private SatuanKerja satuanKerjaOnSession;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		for (int i = 0; i < 60; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			searchselisih.appendChild(comboitem);
		}

		Common.selectComboItem(searchselisih, 1);
		if (searchselisih != null) { searchselisih.setReadonly(true); }

		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (session.getAttribute("satuanKerjaOnSession") != null) {
			satuanKerjaOnSession = (SatuanKerja) session.getAttribute("satuanKerjaOnSession");
			session.removeAttribute("satuanKerjaOnSession");
		}


		
		
		SatuanKerja satuanKerjaData = satuanKerjaOnSession;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanPegawaiData laporanPegawaiData = new LaporanPegawaiData(initCriteria(true));
				laporanPegawaiData.setParent(page.getFirstRoot());
				laporanPegawaiData.setTitle("Data Pegawai");
				laporanPegawaiData.setWidth("95%");
				laporanPegawaiData.setHeight("95%");
				laporanPegawaiData.onModal();
			}
		});
		if (print != null) { print.setParent(add.getParent()); }
	}

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		private Date sekarang = WaktuUtil.getDate();
		@SuppressWarnings("rawtypes")
		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama()).setParent(arg0);

			PegawaiAction.masakerja(pegawai).setParent(arg0);

			if (pegawai.getTanggalkeluarHonorer() != null) {

				java.time.LocalDate dt = java.time.LocalDate.now();
				java.time.LocalDate currentdate = java.time.LocalDate
						.parse(Common.databaseDateFormat.get().format(pegawai.getTanggalkeluarHonorer()));
				Period period = Period.between(dt, currentdate);

				new Label("Habis : " + period.getYears() + " thn, " + period.getMonths() + " bln, " + period.getDays()
						+ " hr").setParent(arg0);
			} else {
				new Label(ais.common.Common.getBahasaConfig("Tanggal habis kontrak belum ditentukan")).setParent(arg0);
			}

			new Label(pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama()).setParent(arg0);

			List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);
			JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
			JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
			Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ (jabatanFungsional == null ? "" : jabatanFungsional.getNama() + "<br>")
					+ (jabatanStruktural == null ? "" : jabatanStruktural.getNama() + "<br>")
					+ (jabatan == null ? "" : jabatan.getNama()) + "</font>").setParent(arg0);
			kenaikanPangkats = null;

			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

			final MyDatebox tanggalmasukHonorer = new MyDatebox(pegawai.getTanggalmasukHonorer());
			tanggalmasukHonorer.setDisabled(!edit);
			tanggalmasukHonorer.setWidth("95%");
			tanggalmasukHonorer.setParent(arg0);
			tanggalmasukHonorer.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pegawai.setTanggalmasukHonorer(tanggalmasukHonorer.getValue());
					Common.refreshUpdate(pegawai);
				}
			});

			final MyDatebox tanggalkeluarHonorer = new MyDatebox(pegawai.getTanggalkeluarHonorer());
			tanggalkeluarHonorer.setDisabled(!edit);
			tanggalkeluarHonorer.setWidth("95%");
			tanggalkeluarHonorer.setParent(arg0);
			tanggalkeluarHonorer.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pegawai.setTanggalkeluarHonorer(tanggalkeluarHonorer.getValue());
					Common.refreshUpdate(pegawai);
				}
			});
		}
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Integer selisih = (Integer) (searchselisih.getSelectedItem() == null ? 0
				: searchselisih.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(Restrictions.eq("tipeMasaKerja", TipeMasaKerja.Honorer))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		criteria.add(Restrictions.or(Restrictions.isNull("tanggalkeluarHonorer"),
				Restrictions.sqlRestriction(
						"date_part('month',age(now(),tanggalkeluarHonorer)) between -" + selisih + " and " + selisih)))
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))
				.add(satuanKerjaOnSession == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja", satuanKerjaOnSession))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("statusPegawai", searchstatus.getSelectedItem().getValue()))
				.add(searchcode.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("code", searchcode.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mycode", searchcode.getText().trim(), MatchMode.ANYWHERE)));

		if (order)
			criteria.add(
					Restrictions.sqlRestriction("1=1 order by date_part('month',age(now(),tanggalkeluarHonorer))"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawai = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Pegawai.class);
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
