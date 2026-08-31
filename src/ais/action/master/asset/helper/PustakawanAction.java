package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PustakawanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.Pustakawan;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk pustakawan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Perpustakaan perpustakaan}, {@code
 * MyGrid grid}, {@code boolean edit}, {@code boolean add}, {@code boolean delete}; pembacaan/pencarian ({@code
 * loadData()}); operasi domain lain ({@code display()}); konfigurasi constructor: {@code add}, {@code delete},
 * {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PustakawanAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Perpustakaan perpustakaan;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	public PustakawanAction(Perpustakaan perpustakaan) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.perpustakaan = perpustakaan;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PustakawanAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PustakawanAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PustakawanAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PustakawanAction
	 */
	class PustakawanRenderer extends ais.ui.util.MyRowRenderer {

		public PustakawanRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Pustakawan pustakawan = (Pustakawan) data;

			CommonMedia.tampilkanGambarKecil(pustakawan.getTbmuser()).setParent(row);

			new Label(pustakawan.getTbmuser() == null ? "" : pustakawan.getTbmuser().getUserId()).setParent(row);

			RevisiHelper
					.createNewRevisi(Pustakawan.class, pustakawan,
							pustakawan.getTbmuser() == null ? "" : pustakawan.getTbmuser().getUserNama())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					pustakawan.getKeterangan() == null ? "" : pustakawan.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pustakawan.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pustakawan));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									Common.refreshDelete(pustakawan);

									loadData(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

								}

							}

						}
					});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<Pustakawan> pustakawans = session.createCriteria(Pustakawan.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("perpustakaan", perpustakaan)).list();

		ListModel strset = new SimpleListModel(pustakawans);
		grid.setRowRenderer(new PustakawanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pustakawan " + perpustakaan.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pengguna", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Tbmuser> tbmusers = session.createCriteria(Pustakawan.class)
						.setProjection(Projections.groupProperty("tbmuser")).add(Restrictions.isNotNull("tbmuser"))
						.add(Restrictions.eq("perpustakaan", perpustakaan)).list();

				AmbilDataTbmuserBanyak ambilDataTbmuserBanyak = new AmbilDataTbmuserBanyak(tbmusers);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTbmuserBanyak);
				ambilDataTbmuserBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						PustakawanDao pustakawanDao = DaoFactory.getInstance().getPustakawanDao();
						for (Tbmuser tbmuser : tbmusers) {
							Pustakawan pustakawan = new Pustakawan();
							pustakawan.setTbmuser(tbmuser);
							pustakawan.setKeterangan("");
							pustakawan.setPerpustakaan(perpustakaan);
							pustakawanDao.save(pustakawan);
						}

						loadData(null);
					}
				});
				ambilDataTbmuserBanyak.setWidth("850px");
				ambilDataTbmuserBanyak.setHeight("97%");
				ambilDataTbmuserBanyak.setVisible(true);
				ambilDataTbmuserBanyak.onModal();
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
		column.setLabel("Id Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
