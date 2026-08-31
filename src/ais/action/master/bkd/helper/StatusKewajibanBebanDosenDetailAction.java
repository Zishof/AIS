package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataDosenBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.StatusKewajibanBebanDosen;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk status kewajiban beban dosen detail. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code StatusKewajibanBebanDosen
 * statusKewajibanBebanDosen}, {@code MyGrid grid}, {@code Paging paging}, {@code Textbox searchnama}, {@code
 * boolean edit}, {@code boolean add}; inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian
 * ({@code loadData()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code add}, {@code
 * edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class StatusKewajibanBebanDosenDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private StatusKewajibanBebanDosen statusKewajibanBebanDosen;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchnama;

	private boolean edit = false;
	private boolean add = false;

	public StatusKewajibanBebanDosenDetailAction(StatusKewajibanBebanDosen statusKewajibanBebanDosen) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		this.statusKewajibanBebanDosen = statusKewajibanBebanDosen;

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(StatusKewajibanBebanDosenDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link StatusKewajibanBebanDosenDetailAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link StatusKewajibanBebanDosenDetailAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see StatusKewajibanBebanDosenDetailAction
	 */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		public DosenRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Dosen dosen = (Dosen) data;

			CommonMedia.tampilkanGambarKecil(dosen).setParent(row);

			RevisiHelper.createNewRevisi(Dosen.class, dosen, dosen.getNama()).setParent(row);

			final MyTextbox keterangan = new MyTextbox(dosen.getKeterangan() == null ? "" : dosen.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					dosen.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (dosen));
				}
			});
			
			

		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(statusKewajibanBebanDosen.getId().equals(ConstantValues.DOSEN_BIASA.getId())
				? Restrictions.or(Restrictions.eq("statusKewajibanBebanDosen", statusKewajibanBebanDosen),
						Restrictions.isNull("statusKewajibanBebanDosen"))
				: Restrictions.eq("statusKewajibanBebanDosen", statusKewajibanBebanDosen))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<Dosen> dosens = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(dosens);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar " + statusKewajibanBebanDosen.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Dosen", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Dosen> dosens = session.createCriteria(Dosen.class)
						.add(Restrictions.eq("statusKewajibanBebanDosen", statusKewajibanBebanDosen)).list();

				AmbilDataDosenBanyak ambilDataDosenBanyak = new AmbilDataDosenBanyak(dosens);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataDosenBanyak);
				ambilDataDosenBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Dosen> dosens = (List<Dosen>) arg0.getData();

						for (Dosen dosen : dosens) {
							dosen.setStatusKewajibanBebanDosen(statusKewajibanBebanDosen);
							Common.refreshSaveOrUpdate(dosen);
						}

						loadData(null);
					}
				});
				ambilDataDosenBanyak.setWidth("850px");
				ambilDataDosenBanyak.setHeight("97%");
				ambilDataDosenBanyak.setVisible(true);
				ambilDataDosenBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		searchnama = new Textbox();
		searchnama.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		loadData(null);

		groupbox.appendChild(paging);
	}

}
