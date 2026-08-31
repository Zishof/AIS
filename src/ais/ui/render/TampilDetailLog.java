package ais.ui.render;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogLogin;
import ais.database.model.LogUserActifity;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Listener ZK yang menampilkan riwayat login pengguna secara <b>bertingkat tiga level</b> (drill-down),
 * dipasang sebagai handler event {@code "onOpen"} pada komponen {@link ais.ui.util.MyDetail}
 * (komponen mirip akordeon/expander) yang mewakili satu baris {@link ais.database.model.LogLogin}
 * di grid riwayat login. Hierarki data yang dirender:
 * <ol>
 * <li><b>{@link ais.database.model.LogLogin}</b> — satu sesi login pengguna (baris induk di grid
 * luar, di luar cakupan kelas ini; kelas ini hanya menangani isi baris {@code MyDetail}-nya).</li>
 * <li><b>{@link ais.database.model.DetailLogLogin}</b> — daftar halaman/menu yang diakses selama
 * sesi login tersebut. Dirender oleh kelas ini sendiri lewat {@link #createList()} dan
 * {@link #DetailLogLoginRenderer inner class DetailLogLoginRenderer} miliknya, ke dalam
 * {@link #detailLogGrid}. Setiap baris di grid ini juga memiliki {@link ais.ui.util.MyDetail}
 * sendiri yang, ketika dibuka, memicu level ketiga lewat
 * {@link TampilDetailUserAccessLog}.</li>
 * <li><b>{@link ais.database.model.LogUserActifity}</b> — daftar aktivitas rinci (event, keterangan,
 * gambar/ikon, waktu) yang terjadi pada satu halaman yang diakses. Dirender oleh inner class
 * {@link TampilDetailUserAccessLog} ke dalam grid terpisah, hanya dimuat ketika baris
 * {@code DetailLogLogin} yang bersangkutan dibuka pengguna.</li>
 * </ol>
 *
 * <h2>Pola "muat saat dibuka" (lazy load on expand)</h2>
 * <p>
 * Baik kelas ini maupun inner class {@link TampilDetailUserAccessLog} mengimplementasikan
 * {@link org.zkoss.zk.ui.event.EventListener} dan didaftarkan sebagai handler event
 * {@code "onOpen"} pada komponen {@link ais.ui.util.MyDetail} yang bersangkutan. Query database
 * dan pembangunan grid HANYA dijalankan ({@link #createList()}) saat komponen benar-benar dalam
 * keadaan terbuka ({@code detail.isOpen()}); pada setiap pemicuan event, isi {@code detail}
 * dikosongkan lebih dulu lewat {@code Common.clear(detail)} — baik saat dibuka (lalu diisi ulang)
 * maupun saat ditutup (dibiarkan kosong). Pola ini mencegah query dan render grid untuk SELURUH
 * baris grid induk dieksekusi sekaligus saat halaman dimuat; hanya baris yang benar-benar dibuka
 * pengguna yang membebani database.
 * </p>
 *
 * <h2>Perenderan grid ZK</h2>
 * <p>
 * Kedua level bawah dirender memakai {@link ais.ui.util.MyGrid} (subclass grid ZK aplikasi) dengan
 * mold {@code "paging"} (15 baris/halaman) dan {@link ais.ui.util.MyRowRenderer} kustom (lihat
 * inner class {@code DetailLogLoginRenderer} pada kelas ini dan pada
 * {@link TampilDetailUserAccessLog}) yang mengimplementasikan kontrak render baris ZK
 * ({@code renderer.render(Row, Object)}) untuk menyusun {@link org.zkoss.zul.Label} kolom demi
 * kolom dari properti entitas.
 * </p>
 */
public class TampilDetailLog implements EventListener {

	/**
	 * Listener level-tiga: dipasang pada {@link ais.ui.util.MyDetail} milik satu baris
	 * {@link ais.database.model.DetailLogLogin} di {@link TampilDetailLog#detailLogGrid}, dan saat
	 * dibuka menampilkan daftar {@link ais.database.model.LogUserActifity} (aktivitas rinci) yang
	 * terjadi pada kunjungan halaman tersebut. Lihat javadoc kelas induk
	 * {@link TampilDetailLog} untuk penjelasan pola "muat saat dibuka" yang dipakai bersama.
	 */
	private class TampilDetailUserAccessLog implements EventListener {

		/** Baris {@link DetailLogLogin} (kunjungan halaman) yang aktivitasnya sedang ditampilkan. */
		private DetailLogLogin detailLogLogin;
		/** Komponen expander tempat grid aktivitas dirender; dikosongkan ulang setiap event. */
		private MyDetail detail;
		/** Grid ZK yang menampilkan daftar {@link LogUserActifity} milik {@link #detailLogLogin}. */
		private MyGrid logUserActifityGrid;

		/**
		 * Perender baris ZK (mengimplementasikan kontrak {@code render(Row, Object)} dari
		 * {@link ais.ui.util.MyRowRenderer}, basis kustom aplikasi untuk
		 * {@code org.zkoss.zul.RowRenderer}) untuk satu baris {@link LogUserActifity} pada
		 * {@link #logUserActifityGrid}. Kolom yang dirender, sesuai urutan header yang didefinisikan
		 * di {@link TampilDetailUserAccessLog#createList()}: Keterangan 1 ({@code keterangan}),
		 * Keterangan 2 ({@code keterangan12}), Keterangan 3 ({@code keterangan1}), Image
		 * ({@code img}), Event ({@code event}), dan Waktu (diformat lewat
		 * {@code Common.dateFormat3}).
		 */
		class DetailLogLoginRenderer extends ais.ui.util.MyRowRenderer {

			/**
			 * Merender satu baris {@link LogUserActifity} ke {@code arg0} sebagai enam sel
			 * {@link org.zkoss.zul.Label} (lihat javadoc kelas {@link DetailLogLoginRenderer} untuk
			 * pemetaan kolomnya). Nilai {@code null} pada {@code keterangan12} ditampilkan sebagai
			 * string kosong; nilai {@code waktu} yang {@code null} juga ditampilkan kosong, selain
			 * itu diformat lewat {@code Common.dateFormat3}.
			 *
			 * @param arg0 baris grid ZK tujuan render
			 * @param arg1 data baris, di-cast ke {@link LogUserActifity}
			 * @throws Exception tidak pernah dilempar secara eksplisit oleh implementasi ini
			 */
			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final LogUserActifity logUserActifity = (LogUserActifity) arg1;

				new Label(logUserActifity.getKeterangan()).setParent(arg0);
				new Label(logUserActifity.getKeterangan12() == null ? "" : logUserActifity.getKeterangan12())
						.setParent(arg0);
				new Label(logUserActifity.getKeterangan1()).setParent(arg0);

				new Label(logUserActifity.getImg()).setParent(arg0);
				new Label(logUserActifity.getEvent()).setParent(arg0);

				new Label(
						logUserActifity.getWaktu() == null ? "" : Common.dateFormat3.get().format(logUserActifity.getWaktu()))
						.setParent(arg0);

			}
		}

		/**
		 * Membangun antarmuka grid aktivitas: kotak judul "Daftar Rincian Akses", grid
		 * {@link #logUserActifityGrid} bermold {@code "paging"} (15 baris/halaman) dengan enam
		 * kolom (Keterangan 1-3, Image, Event, Waktu — Waktu rata tengah), lalu memanggil
		 * {@link #loadDetailLogLogin()} untuk mengisi datanya. Dipanggil dari {@link #onEvent(Event)}
		 * hanya ketika {@link #detail} dalam keadaan terbuka.
		 */
		private void createList() {
			Common.clear(detail);
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(detail);
			groupbox.appendChild(new MyCaptionStyled("Daftar Rincian Akses"));

			logUserActifityGrid = new MyGrid();
			logUserActifityGrid.setMold("paging");
			logUserActifityGrid.setPageSize(15);
			logUserActifityGrid.setParent(groupbox);
			logUserActifityGrid.setWidth("100%");
			logUserActifityGrid.setHeight("100%");

			Columns columns = new Columns();

			columns.setParent(logUserActifityGrid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan 1");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan 2");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Keterangan 3");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Image");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Event");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu");
			column.setAlign("center");

			loadDetailLogLogin();
		}

		/**
		 * Mengambil seluruh {@link LogUserActifity} milik {@link #detailLogLogin} dari database
		 * (diurutkan menurun berdasarkan {@code id}, yaitu aktivitas terbaru lebih dulu) dan
		 * mengikatnya ke {@link #logUserActifityGrid} lewat {@link DetailLogLoginRenderer}.
		 */
		@SuppressWarnings("unchecked")
		private void loadDetailLogLogin() {
			Session session = HibernateUtil.currentSession();
			List<LogUserActifity> logUserActifity = session.createCriteria(LogUserActifity.class)
					.add(Restrictions.eq("detailLogLogin", detailLogLogin)).addOrder(Order.desc("id")).list();

			ListModel strset = new SimpleListModel(logUserActifity);
			logUserActifityGrid.setRowRenderer(new DetailLogLoginRenderer());
			logUserActifityGrid.setModelCheckMobile(strset);
			logUserActifityGrid.renderAll();
			logUserActifityGrid.setOddRowSclass("non-odd");

		}

		/**
		 * Membuat listener untuk satu baris {@link DetailLogLogin} tertentu.
		 *
		 * @param detail  komponen expander yang akan diisi grid aktivitas saat dibuka
		 * @param logLogin baris kunjungan halaman yang aktivitasnya akan ditampilkan
		 */
		public TampilDetailUserAccessLog(MyDetail detail, DetailLogLogin logLogin) {
			this.detailLogLogin = logLogin;
			this.detail = detail;
		}

		/**
		 * Handler event {@code "onOpen"}/{@code "onClose"} pada {@link #detail}: selalu mengosongkan
		 * isi {@code detail} lebih dulu, lalu membangun ulang grid aktivitas lewat
		 * {@link #createList()} hanya bila {@code detail} dalam keadaan terbuka.
		 *
		 * @param arg0 event ZK yang memicu pemanggilan (tidak diperiksa detailnya)
		 * @throws Exception diteruskan dari {@link #createList()}/{@link #loadDetailLogLogin()}
		 */
		@Override
		public void onEvent(Event arg0) throws Exception {
			Common.clear(detail);
			if (detail.isOpen()) {
				createList();
			}
		}

	}

	/** Sesi login yang riwayat kunjungan halamannya sedang ditampilkan. */
	private LogLogin logLogin;
	/** Komponen expander level-kedua tempat {@link #detailLogGrid} dirender. */
	private MyDetail detail;
	/** Grid ZK yang menampilkan daftar {@link DetailLogLogin} milik {@link #logLogin}. */
	private MyGrid detailLogGrid;

	/**
	 * Perender baris ZK (mengimplementasikan kontrak {@code render(Row, Object)} dari
	 * {@link ais.ui.util.MyRowRenderer}) untuk satu baris {@link DetailLogLogin} pada
	 * {@link #detailLogGrid}. Selain menampilkan kolom "Menu yang diakses" ({@code keterangan}) dan
	 * "Waktu" (diformat lewat {@code Common.dateFormat3}), method ini JUGA membangun sel pertama
	 * berisi {@link ais.ui.util.MyDetail} baru yang mendaftarkan
	 * {@link TampilDetailUserAccessLog} sebagai handler {@code "onOpen"}-nya — inilah titik
	 * penyambungan ke level ketiga (aktivitas rinci) dalam hierarki drill-down yang dijelaskan pada
	 * javadoc kelas {@link TampilDetailLog}.
	 */
	class DetailLogLoginRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link DetailLogLogin}: sel pertama berisi expander
		 * {@link ais.ui.util.MyDetail} (pintu masuk ke daftar aktivitas rinci lewat
		 * {@link TampilDetailUserAccessLog}), diikuti label "Menu yang diakses" dan "Waktu".
		 *
		 * @param arg0 baris grid ZK tujuan render
		 * @param arg1 data baris, di-cast ke {@link DetailLogLogin}
		 * @throws Exception tidak pernah dilempar secara eksplisit oleh implementasi ini
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailLogLogin logLogin = (DetailLogLogin) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new TampilDetailUserAccessLog(detail, logLogin));

			new Label(logLogin.getKeterangan()).setParent(arg0);

			new Label(logLogin.getWaktu() == null ? "" : Common.dateFormat3.get().format(logLogin.getWaktu()))
					.setParent(arg0);

		}
	}

	/**
	 * Membangun antarmuka grid kunjungan halaman: kotak judul "Daftar Rincian Kunjungan", toolbar
	 * dengan tombol cetak ({@code Common.cetakData}, memakai kriteria yang sama dengan
	 * {@link #loadDetailLogLogin()} plus kolom {@code halaman}), grid {@link #detailLogGrid}
	 * bermold {@code "paging"} (15 baris/halaman) dengan tiga kolom (expander kosong, "Menu yang
	 * diakses" 75% lebar, "Waktu" 25% lebar rata tengah), lalu memanggil
	 * {@link #loadDetailLogLogin()} untuk mengisi datanya. Dipanggil dari {@link #onEvent(Event)}
	 * hanya ketika {@link #detail} dalam keadaan terbuka.
	 */
	private void createList() {
		Common.clear(detail);
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(detail);
		groupbox.appendChild(new MyCaptionStyled("Daftar Rincian Kunjungan"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(DetailLogLogin.class)
						.add(Restrictions.eq("logLogin", logLogin)).addOrder(Order.desc("id"));
				return criteria;
			}
		}, "logLogin", "waktu", "halaman", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		detailLogGrid = new MyGrid();
		detailLogGrid.setMold("paging");
		detailLogGrid.setPageSize(15);
		detailLogGrid.setParent(groupbox);
		detailLogGrid.setWidth("100%");
		detailLogGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(detailLogGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("35px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Menu yang diakses");
		column.setWidth("75%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setAlign("center");
		column.setWidth("25%");

		loadDetailLogLogin();
	}

	/**
	 * Mengambil seluruh {@link DetailLogLogin} milik {@link #logLogin} dari database (diurutkan
	 * menurun berdasarkan {@code id}, yaitu kunjungan terbaru lebih dulu) dan mengikatnya ke
	 * {@link #detailLogGrid} lewat {@link DetailLogLoginRenderer}.
	 */
	@SuppressWarnings("unchecked")
	private void loadDetailLogLogin() {
		Session session = HibernateUtil.currentSession();
		List<DetailLogLogin> logLogins = session.createCriteria(DetailLogLogin.class)
				.add(Restrictions.eq("logLogin", logLogin)).addOrder(Order.desc("id")).list();

		ListModel strset = new SimpleListModel(logLogins);
		detailLogGrid.setRowRenderer(new DetailLogLoginRenderer());
		detailLogGrid.setModelCheckMobile(strset);
		detailLogGrid.renderAll();
		detailLogGrid.setOddRowSclass("non-odd");

	}

	/**
	 * Membuat listener untuk satu baris {@link LogLogin} tertentu.
	 *
	 * @param detail  komponen expander yang akan diisi grid kunjungan halaman saat dibuka
	 * @param logLogin sesi login yang riwayat kunjungannya akan ditampilkan
	 */
	public TampilDetailLog(MyDetail detail, LogLogin logLogin) {
		this.logLogin = logLogin;
		this.detail = detail;
	}

	/**
	 * Handler event {@code "onOpen"}/{@code "onClose"} pada {@link #detail}: selalu mengosongkan
	 * isi {@code detail} lebih dulu, lalu membangun ulang grid kunjungan halaman lewat
	 * {@link #createList()} hanya bila {@code detail} dalam keadaan terbuka.
	 *
	 * @param arg0 event ZK yang memicu pemanggilan (tidak diperiksa detailnya)
	 * @throws Exception diteruskan dari {@link #createList()}/{@link #loadDetailLogLogin()}
	 */
	@Override
	public void onEvent(Event arg0) throws Exception {
		Common.clear(detail);
		if (detail.isOpen()) {
			createList();
		}
	}

}
