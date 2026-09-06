package ais.action.master.sirkulasisurat.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.surat.helper.AmbilDataSuratMasukBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.sirkulasisurat.PeminjamanSuratItem;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk peminjaman surat item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PeminjamanSuratItem
 * peminjamanSuratItem}, {@code MyGrid grid}, {@code boolean edit}, {@code boolean delete}, {@code Textbox
 * barcode}, {@code String tipe}; pembacaan/pencarian ({@code loadData()}, {@code loadBarcode()}); operasi domain
 * lain ({@code display()}); konfigurasi constructor: {@code delete}, {@code edit}. Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PeminjamanSuratItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PeminjamanSuratItem peminjamanSuratItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	private String tipe;

	public PeminjamanSuratItemDetailAction(PeminjamanSuratItem peminjamanSuratItem, String tipe) {
		super();
		this.tipe = tipe;
		this.peminjamanSuratItem = peminjamanSuratItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PeminjamanSuratItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PeminjamanSuratItemDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PeminjamanSuratItemDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PeminjamanSuratItemDetailAction
	 */
	class PeminjamanSuratItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PeminjamanSuratItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PeminjamanSuratItemDetail peminjamanSuratItemDetail = (PeminjamanSuratItemDetail) data;

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			new Label(peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
					: peminjamanSuratItemDetail.getSuratMasuk().getNoSurat()).setParent(vbox);

			new Label(peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
					: peminjamanSuratItemDetail.getSuratMasuk().getKode()).setParent(vbox);

			RevisiHelper.createNewRevisi(PeminjamanSuratItemDetail.class, peminjamanSuratItemDetail,
					peminjamanSuratItemDetail.getSuratMasuk() == null ? ""
							: peminjamanSuratItemDetail.getSuratMasuk().getPerihal())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					peminjamanSuratItemDetail.getKeterangan() == null ? "" : peminjamanSuratItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(
					peminjamanSuratItemDetail.getPeminjamanSuratItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					peminjamanSuratItemDetail.setKeterangan(keterangan.getValue());
					session.update(peminjamanSuratItemDetail);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat " + tipe, "/img/eye-icon.png");
			button.setDisabled(peminjamanSuratItemDetail.getPeminjamanSuratItem().getDisetujuiOleh() == null
					|| peminjamanSuratItemDetail.getPeminjamanSuratItem().getMulai().after(WaktuUtil.getDate())
					|| peminjamanSuratItemDetail.getPeminjamanSuratItem().getSampai().before(WaktuUtil.getDate()));
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Data");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					final MyWindow window = new MyWindow("Tampilan Surat", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("95%");
					window.setWidth("95%");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Session session = StreamingHibernateUtil.getInstance().currentSession();
					List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = peminjamanSuratItemDetail.getSuratMasuk() == null
							|| peminjamanSuratItemDetail.getSuratMasuk().getId() == null
									? new ArrayList<FotoGambarSuratMasuk>()
									: session.createCriteria(FotoGambarSuratMasuk.class)
											.add(Restrictions.eq("suratMasuk",
													peminjamanSuratItemDetail.getSuratMasuk().getId()))
											.addOrder(Order.desc("id")).list();

					Rows rows = new Rows();
					rows.setParent(grid);

					for (FotoGambarSuratMasuk fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
						Row row = new Row();
						row.setValign("top");
						row.setParent(rows);
						CommonMedia.preview(fotoGambarSuratMasuk, row);
					}
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					StreamingHibernateUtil.getInstance().closeSession();

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.onModal();

				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(delete && peminjamanSuratItemDetail.getPeminjamanSuratItem().getDisetujuiOleh() == null);

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

											Common.refreshDelete(peminjamanSuratItemDetail);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
		List<PeminjamanSuratItemDetail> peminjamanSuratItemDetails = session
				.createCriteria(PeminjamanSuratItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).list();

		ListModel strset = new SimpleListModel(peminjamanSuratItemDetails);
		grid.setRowRenderer(new PeminjamanSuratItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar " + tipe + " Peminjaman");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data " + tipe, "/img/add_item.png");
		button.setDisabled(peminjamanSuratItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<SuratMasuk> suratMasuks = peminjamanSuratItem == null || peminjamanSuratItem.getId() == null
						? new ArrayList<SuratMasuk>()
						: session.createCriteria(PeminjamanSuratItemDetail.class)
								.setProjection(Projections.groupProperty("suratMasuk"))
								.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).list();

				AmbilDataSuratMasukBanyak ambilDataItemBanyak = new AmbilDataSuratMasukBanyak(suratMasuks, tipe, false,
						null, true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<SuratMasuk> items = (List<SuratMasuk>) arg0.getData();

						Session session = HibernateUtil.currentSession();
						List<String> ditolak = new ArrayList<String>();
						for (SuratMasuk suratMasuk : items) {
							if (PeminjamanSuratItemDetail.sedangDipinjamAktif(suratMasuk, peminjamanSuratItem)) {
								ditolak.add(suratMasuk.getNoSurat());
								continue;
							}
							PeminjamanSuratItemDetail peminjamanSuratItemDetail = new PeminjamanSuratItemDetail();
							peminjamanSuratItemDetail.setSuratMasuk(suratMasuk);
							peminjamanSuratItemDetail.setJumlah(1.0);
							peminjamanSuratItemDetail.setKeterangan("");
							peminjamanSuratItemDetail.setPeminjamanSuratItem(peminjamanSuratItem);
							session.save(peminjamanSuratItemDetail);
						}

						loadData(null);

						if (!ditolak.isEmpty()) {
							MyMessageboxConfig.show(
									"Dokumen surat berikut tidak ditambahkan karena sedang dipinjam aktif pada transaksi peminjaman lain yang belum dikembalikan: "
											+ Common.join(ditolak, ", "),
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				});
				ambilDataItemBanyak.setWidth("97%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Nomor Agenda/Surat").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(peminjamanSuratItem.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setDisabled(peminjamanSuratItem.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Surat/Agenda");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perihal");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		loadData(null);
	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Surat atau Nomor Agenda belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nomor Surat/Agenda; (2) scan barcode atau ketikkan nomor surat yang akan dipinjam; (3) tekan Enter atau klik tombol Cari. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		SuratMasuk suratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
				.add(Restrictions.or(Restrictions.ilike("noSurat", barcode, MatchMode.EXACT),
						Restrictions.ilike("kode", barcode, MatchMode.EXACT)))
				.setMaxResults(1).uniqueResult();

		if (suratMasuk == null) {
			MyMessageboxConfig.show("Kode surat " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (PeminjamanSuratItemDetail.sedangDipinjamAktif(suratMasuk, peminjamanSuratItem)) {
			MyMessageboxConfig.show(
					"Mohon maaf, dokumen surat " + barcode
							+ " sedang dipinjam aktif pada transaksi peminjaman lain dan belum dikembalikan. Langkah yang dapat dilakukan: (1) pastikan dokumen sudah dikembalikan pada transaksi sebelumnya; (2) hubungi peminjam sebelumnya atau Administrator bila diperlukan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			this.barcode.focus();
			this.barcode.select();
			return;
		}

		PeminjamanSuratItemDetail peminjamanSuratItemDetail = new PeminjamanSuratItemDetail();
		peminjamanSuratItemDetail.setJumlah(1.0);
		peminjamanSuratItemDetail.setKeterangan("");
		peminjamanSuratItemDetail.setPeminjamanSuratItem(peminjamanSuratItem);
		peminjamanSuratItemDetail.setSuratMasuk(suratMasuk);

		session.save(peminjamanSuratItemDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

}
