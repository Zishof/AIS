package ais.action.master.employ;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class PegawaiPensiunAction extends GenericAutowireComposer implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
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
	}

	public static void main(String[] argv) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		System.out.println("dateFormat = " + dateFormat.format(ais.ui.util.WaktuUtil.getDate()));

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

			new Label(pegawai.getUsiaPensiun() + "").setParent(arg0);

			new Label(pegawai.getTanggallahir() == null ? "" : Common.dateFormat1.get().format(pegawai.getTanggallahir()))
					.setParent(arg0);
			if (pegawai.getTanggallahir() != null) {
				long now = System.currentTimeMillis();
				long lahir = pegawai.getTanggallahir().getTime();
				long selisih = now - lahir;
				final Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.setTimeInMillis(selisih);

				new Label((cal.get(Calendar.YEAR) - 1970) + " tahun " + cal.get(Calendar.MONTH) + " bulan")
						.setParent(arg0);

			} else {
				new Label("").setParent(arg0);
			}

			new Label(pegawai.getTanggalmasuk() == null ? "" : Common.dateFormat1.get().format(pegawai.getTanggalmasuk()))
					.setParent(arg0);

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

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah ke aktif", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.setTooltiptext("Ubah data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mengubah data pegawai dari pensiun ke aktif ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();

										if (pegawai.getDosen() != null) {
											session.refresh(pegawai);
											Dosen dosen = pegawai.getDosen();
											session.refresh(dosen);
											dosen.setStatusPegawai(ConstantValues.AKTIF_PEGAWAI);
											Common.refreshUpdate(session, dosen);
											Common.refreshUpdate(session, pegawai);
										} else if (pegawai.getGuru() != null) {
											session.refresh(pegawai);
											Guru dosen = pegawai.getGuru();
											session.refresh(dosen);
											dosen.setStatusPegawai(ConstantValues.AKTIF_PEGAWAI);
											Common.refreshUpdate(session, dosen);
											Common.refreshUpdate(session, pegawai);
										} else {
											session.refresh(pegawai);
											pegawai.setStatusPegawai(ConstantValues.AKTIF_PEGAWAI);
											Common.refreshUpdate(session, pegawai);
										}

										session.flush();

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
											}
										});
									}

								}
							});
				}

			});
			button.setParent(toolbar);

			toolbar.setParent(arg0);
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

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(
						parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))
				.add(satuanKerjaOnSession == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja", satuanKerjaOnSession))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("statusPegawai", ConstantValues.PENSIUN_PEGAWAI))
				.add(searchcode.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("code", searchcode.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mycode", searchcode.getText().trim(), MatchMode.ANYWHERE)));

		if (order)
			criteria.add(Restrictions
					.sqlRestriction("1=1 order by (usiapensiun - date_part('year',age(now(),tanggallahir)))"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
