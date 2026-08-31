package ais.action.master.psb;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Group;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.West;

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KategoriPengumuman;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk tampilan pengumuman psb. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyDatebox searchmulai}, {@code
 * MyDatebox searchsampai}, {@code Boolean readonly}, {@code Rows rows}, {@code Textbox cari}, {@code Tabpanels
 * tabpanels}, {@code Component menu}, {@code Borderlayout layoutUtama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteriaStatic()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code loadMenu()}, {@code loadData()}, {@code getReadonly()}); mutasi data ({@code
 * setReadonly()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class TampilanPengumumanPSBAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2301873239699174688L;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private Boolean readonly = false;

	private Rows rows;
	private Textbox cari;

	private Tabpanels tabpanels;
	private Component menu;

	private Borderlayout layoutUtama;

	private Tabs tabs;

	private MyWindow window;

	private Sekolah selectedSekolah;
	private Yayasan selectedYayasan;

	private int size;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		selectedSekolah = SekolahUtil.getSekolah();
		selectedYayasan = SekolahUtil.getYayasan();
		if (searchmulai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 12);
			searchmulai.setValue(calendar.getTime());
		}

		if (searchsampai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			searchsampai.setValue(calendar.getTime());
		}

		size = ((Number) initCriteria(false).setProjection(Projections.rowCount()).uniqueResult()).intValue();

		String desktopWidth = execution.getParameter("desktopWidth");
		boolean mobile = Common.isMobile() || (desktopWidth != null
				&& Integer.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE);
		if (mobile) {
			menu = new North();
			((North) menu).setHeight(size > 1 ? "280px" : "0px");
			layoutUtama.appendChild(menu);
			if (desktopWidth != null) {
				window.setWidth(desktopWidth);
			}
		} else {
			menu = new West();
			((West) menu).setWidth(size > 1 ? "280px" : "0px");
			layoutUtama.appendChild(menu);
		}

		if (menu != null) {
			if (menu instanceof North) {
				((North) menu).setSclass("psb-announcement-menu");
			} else if (menu instanceof West) {
				((West) menu).setSclass("psb-announcement-menu");
			}
			menu.setVisible(size > 1);
		}
		loadMenu();

		if (size <= 5 || !mobile) {
			List<PengumumanAkademis> listPengumumanAkademis = ConstantValues.simpleList(
					initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

			if (!listPengumumanAkademis.isEmpty()) {
				TampilanPengumumanAkademisAction.prosess(listPengumumanAkademis.get(0).getId(), tabs, tabpanels, false,
						size <= 5, cari, false);
			}
		}

	}

	private void loadMenu() {

		if (menu == null || tabpanels == null || tabs == null) {
			return;
		}

		Borderlayout subBorderlayout = new Borderlayout();
		subBorderlayout.setVisible(size > 1);
		subBorderlayout.setParent(menu);

		North subsubNorth = new North();
		subsubNorth.setParent(subBorderlayout);
		subsubNorth.setHeight(size > 1 ? "48px" : "0px");
		subsubNorth.setBorder("none");
		subsubNorth.setSclass("psb-announcement-search");

		Borderlayout subSubBorderlayout = new Borderlayout();
		subSubBorderlayout.setParent(subsubNorth);

		West subSubwest = new West();
		subSubwest.setParent(subSubBorderlayout);
		subSubwest.setWidth("70%");
		subSubwest.setBorder("none");

		cari = new Textbox();
		cari.setWidth("100%");
		cari.setTooltiptext("Cari judul pengumuman");
		cari.setParent(subSubwest);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.setWidth("34px");
		button.setSclass("psb-announcement-search-button");
		button.setTooltiptext("Cari pengumuman");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subsubcenter = new Center();
		subsubcenter.setParent(subSubBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subsubcenter, true);
		button.setParent(subsubcenter);
		subsubcenter.setBorder("none");

		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(subcenter);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setSclass("psb-announcement-list");

		rows = new Rows();
		rows.setParent(grid);
		loadData(cari.getValue());

	}

	@SuppressWarnings("unchecked")
	public void loadData(String keyword) {

		Common.clear(rows);
		
		String currentLang = null;
		try {
			currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}

		List<PengumumanAkademis> pengumumanAkademises = ConstantValues.simpleList(
				initCriteria(true).add(keyword == null || keyword.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", keyword, MatchMode.ANYWHERE)).setMaxResults(500),
				PengumumanAkademis.class);

		int jumlahkategori = 1;
		KategoriPengumuman kategoriPengumuman = new KategoriPengumuman();
		kategoriPengumuman.setId(-1L);

		for (final PengumumanAkademis pengumumanAkademis : pengumumanAkademises) {

			if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanAkademis.getKategoriPengumuman() != null
					&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
							.equals(pengumumanAkademis.getKategoriPengumuman().getId())) {
				continue;
			}

			if (pengumumanAkademis.getKategoriPengumuman() != null && (kategoriPengumuman == null
					|| !kategoriPengumuman.getId().equals(pengumumanAkademis.getKategoriPengumuman().getId()))) {
				kategoriPengumuman = pengumumanAkademis.getKategoriPengumuman();
				if (currentLang.equals(Tbmuser.INDONESIA)) {
					Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNama());
					group.setParent(rows);
				} else if (currentLang.equals(Tbmuser.ENGLISH)) {
					Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNamaEn());
					group.setParent(rows);
				}
				jumlahkategori++;
			} else if (pengumumanAkademis.getKategoriPengumuman() == null && kategoriPengumuman != null) {
				kategoriPengumuman = null;
				Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
				group.setParent(rows);
				jumlahkategori++;
			}

			final Row row = new Row();row.setValign("top");
			row.setStyle("border:0px;background: transparent;font-size: x-small;");

			row.setParent(rows);

			String text = "";
			if (currentLang.equals(Tbmuser.INDONESIA)) {
				text = pengumumanAkademis.getJudul();
			} else if (currentLang.equals(Tbmuser.ENGLISH)) {
				text = pengumumanAkademis.getJudulEn();
			}

			text = text == null || text.trim().isEmpty() ? "Pengumuman" : text.trim();
			text = text.length() > 255 ? text.substring(0, 254) + ".." : text;

			final A toolbarbutton = new A(text);
			toolbarbutton.setSclass("psb-announcement-link");
			toolbarbutton.setTooltiptext(text);
			row.setSclass("psb-announcement-row");

			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.scrollIntoView(row);
					TampilanPengumumanAkademisAction.prosess(pengumumanAkademis.getId(), tabs, tabpanels, false,
							size <= 5, cari, false);
				}
			});

		}
		if (menu instanceof North) {
			int tinggiMenu = (pengumumanAkademises.size() + jumlahkategori) * 44;
			((North) menu).setHeight(Math.min(Math.max(tinggiMenu, 96), 300) + "px");
		}
		pengumumanAkademises = null;
	}

	public static Criteria initCriteriaStatic(boolean order, Sekolah sekolah, Yayasan yayasan) {
		Session session = HibernateUtil.currentSession();

		Criterion r = Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_CALON_SISWA);

		Criteria criteria = session.createCriteria(PengumumanAkademis.class)
				.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
				.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("sekolah", sekolah))

				.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("yayasan", yayasan))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
								Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
						Restrictions.or(Restrictions.le("tanggal", ais.ui.util.WaktuUtil.getDate()),
								Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()))))

				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).add(r);
		if (order)
			criteria
					.addOrder(Order.asc("kategoriPengumuman.nomorUrut")).addOrder(Order.desc("tanggal"))
					.addOrder(Order.desc("id"));
		return criteria;
	}

	public Criteria initCriteria(boolean order) {
		return initCriteriaStatic(order, selectedSekolah, selectedYayasan);
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
