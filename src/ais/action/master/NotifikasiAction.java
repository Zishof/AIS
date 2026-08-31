package ais.action.master;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiNotifikasiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Notifikasi;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk notifikasi. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html dashboardHtml},
 * {@code org.zkoss.zul.Html progressHtml}, {@code MyWindow addWindow}, {@code Paging paging}, {@code MyGrid
 * grid}, {@code Textbox searchnama}, {@code Textbox searchemails}, {@code MyDatebox start};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code refreshDashboardAman()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onWa()}, {@code onTanyaJawab()}, {@code onAdd()}).
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
public class NotifikasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchemails;
	private MyDatebox start;
	private MyDatebox end;

	private Textbox nama;
	private Textbox emails;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Notifikasi notifikasi;
	private MyToolbarbuttonConfig add;

	private Tabpanel chekWa;

	public void onWa(Event event) {
		if (chekWa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(chekWa);
			MyInclude iframe = new MyInclude("/pages/master/notifikasi_wa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tanyaJawab;

	public void onTanyaJawab(Event event) {
		if (tanyaJawab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tanyaJawab);
			MyInclude iframe = new MyInclude("/pages/master/tanya_jawab.zul");
			iframe.setParent(window);
		}
	}

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

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		refreshDashboardAman();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		String[] contents = new String[] { "id", "nama", "emails", "keterangan", "waktu", "hasil", "hasilEmail" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Notifikasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Notifikasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiNotifikasiHelper revisiHelper = new RevisiNotifikasiHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link NotifikasiAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link NotifikasiAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see NotifikasiAction
	 */
	class NotifikasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Notifikasi notifikasi = (Notifikasi) arg1;

			new MyLabelKecil(notifikasi.getNama()).setParent(arg0);
			new MyLabelKecil(notifikasi.getEmails()).setParent(arg0);
			new MyLabelKecil(notifikasi.getKeterangan()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Notifikasi.class, notifikasi,
					Common.dateFormat3.get().format(notifikasi.getWaktu()))).setParent(arg0);

			a.appendChild(new MyLabelKecil(notifikasi.getHasil()));
			a.appendChild(new MyLabelKecil(notifikasi.getHasilEmail()));

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, delete, notifikasi, NotifikasiAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					try {
						JSONArray attachmentsData = null;
						JSONObject jsonObject = new JSONObject(notifikasi.getKeterangan());
						File file = null;
						List<String> aa = MailSender.kirimNotif(notifikasi, false,
								jsonObject.isNull("classData") ? null : jsonObject.getJSONObject("classData"),
								attachmentsData, false, file);
						MyMessageboxConfig.showFormat(
								"Proses pengiriman notifikasi telah selesai dilaksanakan. Berikut kami sampaikan rincian hasil pengiriman notifikasi kepada masing-masing penerima: {V1}",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, aa.toString());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

				}

			});
			button.setParent(aa);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Notifikasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		notifikasi = (Notifikasi) obj;
		init(notifikasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Notifikasi notifikasi) {
		this.notifikasi = notifikasi;
		addWindow.setTitle(notifikasi.getId() == null ? "Tambah Notifikasi" : "Ubah Notifikasi");
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

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("User ID Notifikasi"));
		row.appendChild(nama = new Textbox(notifikasi.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email Notifikasi"));
		row.appendChild(emails = new Textbox(notifikasi.getEmails()));
		emails.setWidth("90%");
		emails.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(notifikasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(5);

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
		refreshDashboardAman();
		addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();
		if (notifikasi.getId() != null) {
			notifikasi = (Notifikasi) session.load(Notifikasi.class, notifikasi.getId());

		}
		notifikasi.setNama(nama.getValue());
		notifikasi.setEmails(emails.getValue());
		notifikasi.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, notifikasi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Notifikasi.class)
				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.waktu) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchemails.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("nama", searchemails.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("emails", searchemails.getValue().trim(), MatchMode.ANYWHERE))

				);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Notifikasi> notifikasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(notifikasi);
		grid.setRowRenderer(new NotifikasiRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void refreshDashboardAman() {
		try {
			if (progressHtml != null) {
				progressHtml.setVisible(false);
			}
			if (dashboardHtml != null) {
				// Dasbor modern berbasis HTML/CSS (DashboardUiKit) dengan data dari
				// NotifikasiCache — tampil instan tanpa membebani basis data.
				dashboardHtml.setContent(ais.action.master.helper.NotifikasiDashboardHelper.buildHtml());
			}
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/NotifikasiAction.java:397");
			}
		}
	}

}
