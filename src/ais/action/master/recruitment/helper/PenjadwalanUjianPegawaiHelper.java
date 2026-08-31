package ais.action.master.recruitment.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.helper.PenjadwalanHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PertemuanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Jendela dialog ZK untuk menjadwalkan sesi/{@link Pertemuan} ujian pada satu
 * {@link JadwalUjianPegawai} (modul rekrutmen pegawai): menampilkan grid pertemuan yang dapat
 * diedit langsung (topik, waktu mulai/selesai, status pertemuan), menambah pertemuan baru dengan
 * tanggal otomatis digeser 7 hari dari pertemuan sebelumnya, dan menyimpan seluruh perubahan
 * sekaligus (termasuk menomori ulang urutan pertemuan dan menghapus baris yang tidak lagi aktif)
 * saat tombol Simpan ditekan.
 */
public class PenjadwalanUjianPegawaiHelper {

	private JadwalUjianPegawai jadwalUjianPegawai;
	private MyGrid grid;
	private DataLoader dataLoader;

	private Date currDate;

	/** Renderer baris grid untuk {@link Pertemuan}: topik, tanggal mulai/selesai, status pertemuan (semua tersimpan langsung saat diubah), dan tombol hapus (dengan pemeriksaan kelayakan via {@link PenjadwalanHelper#checkBolehHapus}). */
	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		public PertemuanRenderer() {

		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) arg1;
			arg0.setAttribute("myValue", pertemuan);

			final Textbox topik = new Textbox();
			topik.setValue(pertemuan.getTopik() == null ? "" : pertemuan.getTopik());
			topik.setParent(arg0);
			topik.setWidth("90%");
			topik.setRows(2);
			topik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setTopik(topik.getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final MyDatebox mulai = new MyDatebox();
			mulai.setValue(pertemuan.getMulai());
			mulai.setFormat(Common.dateFormat.get().toPattern());

			mulai.setWidth("90%");
			mulai.setParent(arg0);
			mulai.setReadonly(true);
			mulai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setMulai(mulai.getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final MyDatebox sampai = new MyDatebox();
			sampai.setValue(pertemuan.getSelesai());
			sampai.setFormat(Common.dateFormat.get().toPattern());

			sampai.setWidth("90%");
			sampai.setParent(arg0);
			sampai.setReadonly(true);
			sampai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setSelesai(sampai.getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final Combobox ujian = new Combobox();
			Common.insertCombo(ujian, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(ujian, pertemuan.getStatusPertemuan());
			ujian.setReadonly(true);
			arg0.appendChild(ujian);
			ujian.setWidth("90%");
			ujian.setReadonly(true);
			ujian.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setStatusPertemuan((StatusPertemuan) ujian.getSelectedItem().getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			currDate = pertemuan.getTanggal();

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!PenjadwalanHelper.checkBolehHapus(pertemuan)) {
						return;
					}
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											PertemuanDao pertemuanDao = DaoFactory.getInstance().getPertemuanDao();
											// pertemuanDao.beginTransaction();

											pertemuanDao.delete((pertemuan));
											// pertemuanDao.commitTransaction();
											onSearchDefault(event);
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
			toolbar.setParent(arg0);

		}

	}

	/**
	 * Menyinkronkan seluruh baris grid ke database: menomori ulang urutan ({@code pertemuanKe})
	 * pertemuan aktif berdasarkan urutan tanggal, lalu untuk tiap baris pada grid — bila
	 * pertemuannya masih aktif — memperbarui/menyimpan data topik dan waktu; pertemuan yang
	 * sebelumnya ada di database tapi tidak lagi muncul di antara baris yang diproses (aktif)
	 * dihapus permanen.
	 *
	 * @return selalu {@code true}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean save() throws InterruptedException {

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		Session session = HibernateUtil.currentSession();

		List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jadwalUjianPegawai", jadwalUjianPegawai)).addOrder(Order.asc("tanggal"))
				.addOrder(Order.asc("id")).list();

		int pertemuanKe = 1;
		for (Pertemuan pertemuan : pertemuans) {
			if (pertemuan.getAktif()) {
				pertemuan.setPertemuanKe(pertemuanKe);
				pertemuanKe++;
				session.update(pertemuan);
			}
		}

		Map<Long, Pertemuan> map = new java.util.HashMap<Long, Pertemuan>();
		for (Pertemuan pertemuan : pertemuans) {
			if (pertemuan.getAktif()) {
				map.put(pertemuan.getId(), pertemuan);
			}
		}

		rows = grid.getRows();
		list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			Textbox topik = (Textbox) data.get(0);
			MyDatebox mulai = (MyDatebox) data.get(1);
			MyDatebox selesai = (MyDatebox) data.get(2);

			Pertemuan pertemuan = (Pertemuan) (row.getAttribute("myValue") == null ? new Pertemuan()
					: row.getAttribute("myValue"));
			if (pertemuan.getAktif()) {
//			pertemuan.setStatusPertemuan(ConstantValues.UJIAN_ONLINE);
				pertemuan.setTanggal(jadwalUjianPegawai.getWaktuMulai());
				pertemuan.setJadwalUjianPegawai(jadwalUjianPegawai);
				pertemuan.setMulai(mulai.getValue());
				pertemuan.setSelesai(selesai.getValue());

				pertemuan.setTopik(topik.getValue());

				if (pertemuan.getId() != null) {
					Common.refreshUpdate(session, (pertemuan));
				} else {
					pertemuan.setWaktuMulai(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuMulai()));
					pertemuan.setWaktuSelesai(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuSampai()));
					session.save(pertemuan);
				}

				map.remove(pertemuan.getId());
			}
		}

		for (Pertemuan pertemuan : map.values()) {
			session.delete(pertemuan);
		}

		this.dataLoader.loadData(null);
		return true;
	}

	/**
	 * Membuka dialog modal penjadwalan pertemuan untuk {@code jadwalUjianPegawai}: menampilkan
	 * grid pertemuan yang sudah ada, tombol tambah pertemuan baru, dan tombol Simpan/Batal.
	 * {@code dataLoader} dipanggil setelah penyimpanan untuk menyegarkan tampilan pemanggil.
	 */
	public void display(final JadwalUjianPegawai jadwalUjianPegawai, final DataLoader dataLoader) {
		this.jadwalUjianPegawai = jadwalUjianPegawai;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(jadwalUjianPegawai.infoSimple());

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		String matkul1 = jadwalUjianPegawai.getNama();

		String gelombangPendaftaranPegawai = jadwalUjianPegawai.getGelombangPendaftaranPegawai() == null ? "Semua"
				: jadwalUjianPegawai.getGelombangPendaftaranPegawai().getNama();

		String harijam = (", Waktu : " + Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuMulai()) + " s.d "
				+ Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuSampai()));

		String groupTxt = "Materi Ujian : " + matkul1 + ", Gelombang Pendaftaran : " + gelombangPendaftaranPegawai
				+ harijam;

		window.setTitle(groupTxt);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Pertemuan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyFormRow row = new MyFormRow();
				row.setValign("top");
				Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
				rows.setParent(grid);
				row.setParent(rows);

				Date myDate = ais.ui.util.WaktuUtil.getDate();
				if (currDate != null) {
					Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
					myCalendar.setTime(currDate);
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
					currDate = myCalendar.getTime();
				} else {
					currDate = myDate;
				}

				Pertemuan pertemuan = new Pertemuan();

				pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
				pertemuan.setTanggal(currDate);
				pertemuan.setJadwalUjianPegawai(jadwalUjianPegawai);
				pertemuan.setTopik("Ujian ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuMulai()));
				pertemuan.setWaktuSelesai(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuSampai()));

				// pertemuan.setMulai(jadwalUjianPegawai.getWaktuMulai());
				// pertemuan.setSelesai(jadwalUjianPegawai.getWaktuSampai());

				pertemuan.setJadwalUjianPegawai(jadwalUjianPegawai);

				Session session = HibernateUtil.currentSession();

				session.save(pertemuan);

				jadwalUjianPegawai.belum();
				onSearchDefault(event);

			}
		});
		button.setParent(toolbar);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (save()) {
					window.detach();
				}
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Materi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Selesai");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memuat ulang seluruh {@link Pertemuan} aktif milik {@link #jadwalUjianPegawai} (terurut waktu mulai) ke grid. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Pertemuan> pertemuan = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jadwalUjianPegawai", jadwalUjianPegawai)).addOrder(Order.asc("mulai"))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
