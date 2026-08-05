package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konsentrasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.action.master.helper.FilterLanjutHelper;

public class CariMahasiswaAction extends GenericAutowireComposer {

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
	private Decimalbox searchtahun;
	private Combobox searchkonsentrasi;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;
	private MyCheckboxConfig searchdosenPA;
	private AmbilDataDosenBanbox searchdosen;

	private MyCheckboxConfig searchFacebook;
	private MyCheckboxConfig searchGoogle;
	private MyCheckboxConfig searchTwitter;
	private MyCheckboxConfig searchLinkedin;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		Tbmuser tbmuser = Common.getCurrentUser();
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu\

		class SearchJurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchkonsentrasi);
				searchkonsentrasi.setSelectedItem(null);
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
			}

		}
		searchjurusan.addEventListener("onChange", new SearchJurusanEventListener());

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		searchstatus.setDisabled(
				tbmuser == null || tbmuser.hakAkses() == null || tbmuser.hakAkses().getRoleId() == null
						|| !tbmuser.hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR));

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.initPrograms(searchprogram);

		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			Common.clear(searchkonsentrasi);

			Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
					Restrictions.eq("jurusan", tbmuser.ambilJurusan()));
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setAttribute("dosen", mydosen);
			searchdosen.setDisabled(true);
		}
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						CariMahasiswaAction.this.session.setAttribute("selectedMahasiswa", mahasiswa);
						MyIframe include = new MyIframe(
								"/pages/master/kalender_mahasiswa.zul?selectedMahasiswa=" + mahasiswa.getId());
						include.setHeight("500px");
						include.setWidth("100%");
						detail.appendChild(include);
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(arg0);

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			mahasiswa.tampilkanHp(arg0);

			mahasiswa.tampilkanEmail(arg0);

//			Vbox vbox = new Vbox();
//			TbmuserAction.tampilkanSocialMediaProfile(vbox, mahasiswa.getSocialMediaProfile());
//			vbox.setParent(arg0);

			final Label label = new Label();
			label.setParent(arg0);
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

			String dosen = krsMahasiswa.getDosenPa() == null ? null : krsMahasiswa.getDosenPa().getNama();

			label.setValue(dosen == null ? "Tidak mempunyai dosen PA" : dosen);
			if (dosen == null) {
				label.setStyle("font-weight:bold;color:red");
			}

			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			new Label(statusMahasiswa.getNama() + "/"
					+ (mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama()))
							.setParent(arg0);

			String ttl = ((mahasiswa.getTempatlahir() == null || mahasiswa.getTempatlahir().trim().equals("") ? ""
					: mahasiswa.getTempatlahir() + ", ")
					+ (mahasiswa.getTanggallahir() == null ? ""
							: Common.dateFormat6.get().format(mahasiswa.getTanggallahir())));

			new Label(ttl).setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(searchFacebook.isChecked()
						? Restrictions.and(Restrictions.isNotNull("facebookId"), Restrictions.ne("facebookId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchGoogle.isChecked()
						? Restrictions.and(Restrictions.isNotNull("googleId"), Restrictions.ne("googleId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchTwitter.isChecked()
						? Restrictions.and(Restrictions.isNotNull("twitterId"), Restrictions.ne("twitterId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchLinkedin.isChecked()
						? Restrictions.and(Restrictions.isNotNull("linkedinId"), Restrictions.ne("linkedinId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.add(searchdosenPA.isChecked() ? Restrictions.isNull("dosen") : Restrictions.sqlRestriction("1=1"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
		criteria.add(criteriaStatus).add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", searchnim.getValue(), MatchMode.ANYWHERE))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				// .add(searchstatus.getSelectedItem() == null ||
				// searchstatus.getSelectedItem().getValue() == null ?
				// Restrictions
				// .sqlRestriction("1=1") : Restrictions.eq("status",
				// searchstatus.getSelectedItem().getValue()))
				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchkonsentrasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("konsentrasi", searchkonsentrasi.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		if (searchnama == null) {
			return;
		}

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))

				.list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
