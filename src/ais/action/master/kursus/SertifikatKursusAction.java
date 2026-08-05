package ais.action.master.kursus;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.SertifikatKursus;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;

/**
 * Daftar Sertifikat Kursus yang diterbitkan sistem (otomatis begitu peserta menyelesaikan
 * semua materi -- lihat cekDanTerbitkanSertifikat() di _kursus_service.jsp). Halaman ini
 * HANYA untuk peninjauan admin dan pencabutan (status Aktif/Dicabut), bukan input manual --
 * sertifikat tidak dibuat lewat halaman ini.
 */
public class SertifikatKursusAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	private static final long serialVersionUID = -5779730267402400329L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private boolean edit = false;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		edit = ais.common.CommonPrivilages.checkPrevilages(ais.common.CommonPrivilages.UPDATE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nomorSertifikat", "nama", "tanggalTerbit", "nilaiAkhir",
				"status" };
		Common.appendKeToolbar(Common.cetakData(SertifikatKursus.class, this, contents), null, comp);
	}

	class SertifikatKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final SertifikatKursus sert = (SertifikatKursus) arg1;
			RevisiHelper.createNewRevisi(SertifikatKursus.class, sert, sert.getNama()).setParent(arg0);

			new org.zkoss.zul.Label(sert.getNomorSertifikat() == null ? "-" : sert.getNomorSertifikat()).setParent(arg0);
			new org.zkoss.zul.Label(sert.getPesertaPunyaProdukKursus() == null || sert.getPesertaPunyaProdukKursus().getPesertaKursus() == null
					? "" : sert.getPesertaPunyaProdukKursus().getPesertaKursus().getNama()).setParent(arg0);
			new org.zkoss.zul.Label(sert.getPesertaPunyaProdukKursus() == null || sert.getPesertaPunyaProdukKursus().getProdukKursus() == null
					? "" : sert.getPesertaPunyaProdukKursus().getProdukKursus().getNama()).setParent(arg0);
			new org.zkoss.zul.Label(Common.dateFormat3.get().format(sert.getTanggalTerbit())).setParent(arg0);
			new org.zkoss.zul.Label(sert.getNilaiAkhir() == null ? "-" : String.valueOf(sert.getNilaiAkhir())).setParent(arg0);

			A verif = new A("Verifikasi Publik");
			verif.setTarget("_blank");
			verif.setHref(Common.ROOT + "/VerifikasiSertifikatKursus?kode=" + sert.getKode());
			verif.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif (tidak dicabut)");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(SertifikatKursus.AKTIF.equals(sert.getStatus()));
			checkbox.setParent(arg0);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					sert.setStatus(checkbox.isChecked() ? SertifikatKursus.AKTIF : SertifikatKursus.DICABUT);
					Common.refreshSaveOrUpdate(sert);
				}
			});
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SertifikatKursus.class);

		if (order)
			criteria.addOrder(org.hibernate.criterion.Order.desc("tanggalTerbit"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? org.hibernate.criterion.Restrictions.sqlRestriction("true")
				: org.hibernate.criterion.Restrictions.ilike("nama", searchnama.getValue().trim(),
						org.hibernate.criterion.MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SertifikatKursus> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(list);
		grid.setRowRenderer(new SertifikatKursusRenderer());
		grid.setModelCheckMobile(strset);
	}

}
