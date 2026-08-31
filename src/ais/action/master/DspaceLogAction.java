package ais.action.master;

import java.util.Calendar;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceLog;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyToolbarbuttonConfig;

import org.zkoss.zul.Html;
import ais.action.master.helper.GenericActionDashboardHelper;
/**
 * Controller/action ZK untuk dspace log. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Html dashboardHtml}, {@code Html progressHtml}, {@code Textbox searchketerangan}, {@code MyDatebox
 * start}, {@code MyDatebox end}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code refreshDashboardSafe()}, {@code onSearchDefault()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class DspaceLogAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging; 
	private MyGrid grid;

	
	private Html dashboardHtml;
	private Html progressHtml;
private Textbox searchketerangan;
	private MyDatebox start;
	private MyDatebox end;

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
		refreshDashboardSafe();

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 7);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		
		
		String[] contents = new String[] { "id", "action", "keterangan", "error", "handle", "status" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DspaceLog.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, start, comp);

		
	}
	private void refreshDashboardSafe() {
		try {
			GenericActionDashboardHelper.refresh(dashboardHtml, progressHtml, DspaceLog.class,
					"Dasbor DSpace Log", "Ringkasan integrasi DSpace, status sinkronisasi, dan kejadian penting yang perlu diperiksa.");
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/DspaceLogAction.java:104");
			}
		}
	}



	/**
	 * Renderer lokal untuk layar/komponen {@link DspaceLogAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DspaceLogAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DspaceLogAction
	 */
	class DspaceLogRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DspaceLog dspaceLog = (DspaceLog) arg1;

			RevisiHelper.createNewRevisi(DspaceLog.class, dspaceLog, dspaceLog.getAction()).setParent(arg0);

			String link = dspaceLog.getHandle();
			if (link != null && !link.trim().isEmpty()) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				new MyLabelKecilSekali(dspaceLog.getKeterangan()).setParent(hbox);
				link = ConstantValues.DSPACE_URL_PUBLIK.replaceAll("rest", ConstantValues.DSPACE_UI + "/handle") + "/"
						+ link;
				A a = new A(link == null ? "" : link);
				a.setHref(link == null ? "" : link);
				a.setTarget("_blank");
				a.setStyle("font-size:8px;");
				a.setParent(hbox);
			} else {
				new MyLabelKecilSekali(dspaceLog.getKeterangan()).setParent(arg0);
			}

			new MyLabelKecil(dspaceLog.getError()).setParent(arg0);
			new MyLabelBold(dspaceLog.getStatus().equals(200) ? "Sukses" : "Gagal (" + dspaceLog.getStatus() + ")")
					.setParent(arg0);
			new Label(Common.dateFormat3.get().format(dspaceLog.getTanggal_dirubah())).setParent(arg0);

			Common.infoDiuploadOleh(dspaceLog.getOlehId(), dspaceLog.getOleh(), arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DspaceLog.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		GenericActionDashboardHelper.showProgress(progressHtml, 15, "Memuat data", "Membaca data sesuai filter yang aktif.");
		Common.initPaging(initCriteria(false), paging);

		List<DspaceLog> dspaceLog = initCriteria(true)
				.add(Restrictions.sqlRestriction("date(this_.tanggal_dirubah) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))

				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dspaceLog);
		grid.setRowRenderer(new DspaceLogRenderer());
		grid.setModelCheckMobile(strset);
		refreshDashboardSafe();
		GenericActionDashboardHelper.hideProgress(progressHtml);
	}

}
