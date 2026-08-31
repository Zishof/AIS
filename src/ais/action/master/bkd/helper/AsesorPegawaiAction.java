package ais.action.master.bkd.helper;

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
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.Pegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk asesor pegawai. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Asesor asesor}, {@code MyGrid grid},
 * {@code boolean edit}, {@code boolean add}, {@code boolean delete}; pembacaan/pencarian ({@code loadData()});
 * operasi domain lain ({@code display()}); konfigurasi constructor: {@code add}, {@code delete}, {@code edit}.
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class AsesorPegawaiAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Asesor asesor;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	public AsesorPegawaiAction(Asesor asesor) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.asesor = asesor;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AsesorPegawaiAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AsesorPegawaiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AsesorPegawaiAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AsesorPegawaiAction
	 */
	class AsesorPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		public AsesorPegawaiRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final AsesorPegawai asesorPegawai = (AsesorPegawai) data;

			CommonMedia.tampilkanGambarKecil(asesorPegawai.getPegawai()).setParent(row);

			RevisiHelper
					.createNewRevisi(AsesorPegawai.class, asesorPegawai,
							asesorPegawai.getPegawai() == null ? "" : asesorPegawai.getPegawai().getNama())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					asesorPegawai.getKeterangan() == null ? "" : asesorPegawai.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					asesorPegawai.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (asesorPegawai));
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
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									Common.refreshDelete(asesorPegawai);

									loadData(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
													+ e.getMessage());
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
		List<AsesorPegawai> asesorPegawais = session.createCriteria(AsesorPegawai.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("asesor", asesor)).list();

		ListModel strset = new SimpleListModel(asesorPegawais);
		grid.setRowRenderer(new AsesorPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar " + asesor.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pegawai", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Pegawai> pegawais = session.createCriteria(AsesorPegawai.class)
						.setProjection(Projections.groupProperty("pegawai")).add(Restrictions.isNotNull("pegawai"))
						.add(Restrictions.eq("asesor", asesor)).list();

				AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
				ambilDataPegawaiBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();

						for (Pegawai pegawai : pegawais) {
							AsesorPegawai asesorPegawai = new AsesorPegawai();
							asesorPegawai.setPegawai(pegawai);
							asesorPegawai.setKeterangan("");
							asesorPegawai.setAsesor(asesor);
							Common.refreshSaveOrUpdate(asesorPegawai);
						}

						loadData(null);
					}
				});
				ambilDataPegawaiBanyak.setWidth("850px");
				ambilDataPegawaiBanyak.setHeight("97%");
				ambilDataPegawaiBanyak.setVisible(true);
				ambilDataPegawaiBanyak.onModal();
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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
