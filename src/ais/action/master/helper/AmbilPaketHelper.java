package ais.action.master.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PilihanPaketPerJurusanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Paket;
import ais.database.model.PilihanPaketPerJurusanMhsBaru;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk memilih sekumpulan {@link Paket} (paket ujian/mata pelajaran PMB) yang berlaku
 * bagi satu {@link JurusanSekolahMahasiswaBaru} (jurusan pada pendaftaran mahasiswa baru), disimpan
 * sebagai baris relasi {@link PilihanPaketPerJurusanMhsBaru}. Jendela pencarian menampilkan seluruh
 * {@link Paket} aktif dengan kotak centang; paket yang sudah terpasang pada jurusan ditandai
 * tercentang sejak awal.
 *
 * <p>
 * {@link #save()} menghapus seluruh relasi lama milik {@link #jurusanSekolahMahasiswaBaru} lalu
 * menyimpan ulang relasi untuk paket yang tercentang pada grid saat tombol Simpan ditekan
 * (rekonsiliasi penuh, bukan diff bertahap); bidang {@link #deletedMatakuliahs} melacak baris yang
 * sempat dicentang lalu dibatalkan selama sesi jendela terbuka agar turut dihapus eksplisit.
 * </p>
 */
public class AmbilPaketHelper {

	private Paket paket;
	private JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru;
	private MyGrid grid;

	private Set<PilihanPaketPerJurusanMhsBaru> deletedMatakuliahs = new HashSet<PilihanPaketPerJurusanMhsBaru>();

	public AmbilPaketHelper() {

	}

	class PaketRenderer extends ais.ui.util.MyRowRenderer {

		private PilihanPaketPerJurusanDao pilihanPaketPerJurusanDao = DaoFactory.getInstance()
				.getPilihanPaketPerJurusanDao();

		private Session session = pilihanPaketPerJurusanDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Paket paket = (Paket) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("paket", paket);
			Integer jml = 0;

			jml = ((Number) session.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
					.add(Restrictions.isNotNull("paket")).setProjection(Projections.rowCount())
					.add(Restrictions.eq("paket", paket))
					.add(Restrictions.eq("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru))

					.uniqueResult()).intValue();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru = (PilihanPaketPerJurusanMhsBaru) HibernateUtil
							.currentSession().createCriteria(PilihanPaketPerJurusanMhsBaru.class)
							.add(Restrictions.isNotNull("paket")).add(Restrictions.eq("paket", paket))
							.add(Restrictions.eq("jurusanSekolahMahasiswaBaru", jurusanSekolahMahasiswaBaru))
							.uniqueResult();
					if (pilihanPaketPerJurusanMhsBaru != null) {
						if (!checkbox.isChecked()) {
							deletedMatakuliahs.remove(pilihanPaketPerJurusanMhsBaru);
						} else {
							deletedMatakuliahs.add(pilihanPaketPerJurusanMhsBaru);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));
			new Label(paket.getNama()).setParent(arg0);

		}

	}

	/**
	 * Menghapus seluruh {@link PilihanPaketPerJurusanMhsBaru} lama milik
	 * {@link #jurusanSekolahMahasiswaBaru}, lalu menyimpan ulang relasi untuk setiap {@link Paket}
	 * yang tercentang pada grid saat ini, dan menghapus eksplisit baris yang tercatat di
	 * {@link #deletedMatakuliahs}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		PilihanPaketPerJurusanDao pilihanPaketPerJurusanDao = DaoFactory.getInstance().getPilihanPaketPerJurusanDao();
		Session session = pilihanPaketPerJurusanDao.getCurrentSession();

		List<PilihanPaketPerJurusanMhsBaru> pilihanPaketPerJurusanMhsBaru1 = session
				.createCriteria(PilihanPaketPerJurusanMhsBaru.class).add(Restrictions.isNotNull("paket"))
				.add(Restrictions.eq("jurusanSekolahMahasiswaBaru", this.jurusanSekolahMahasiswaBaru)).list();
		for (PilihanPaketPerJurusanMhsBaru pils : pilihanPaketPerJurusanMhsBaru1) {
			session.delete(pils);
		}

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);

				if (checkbox.isChecked()) {
					Paket paket = (Paket) checkbox.getAttribute("paket");

					PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru = (PilihanPaketPerJurusanMhsBaru) session
							.createCriteria(PilihanPaketPerJurusanMhsBaru.class).add(Restrictions.isNotNull("paket"))
							.add(Restrictions.eq("paket", this.paket))
							.add(Restrictions.eq("jurusanSekolahMahasiswaBaru", this.jurusanSekolahMahasiswaBaru))
							.setMaxResults(1).uniqueResult();

					if (pilihanPaketPerJurusanMhsBaru == null) {
						pilihanPaketPerJurusanMhsBaru = new PilihanPaketPerJurusanMhsBaru();
					}

					pilihanPaketPerJurusanMhsBaru.setJurusanSekolahMahasiswaBaru(jurusanSekolahMahasiswaBaru);
					pilihanPaketPerJurusanMhsBaru.setPaket(paket);
					session.saveOrUpdate(pilihanPaketPerJurusanMhsBaru);

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilPaketHelper.java:143");
				// TODO: handle exception
			}
			// else {
			// // Paket paket = (Paket) checkbox.getAttribute("paket");
			// PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru =
			// (PilihanPaketPerJurusanMhsBaru) session
			// .createCriteria(PilihanPaketPerJurusanMhsBaru.class)
			// .add(Restrictions.eq("paket", this.paket))
			// .add(Restrictions.eq("jurusanSekolahMahasiswaBaru",
			// this.jurusanSekolahMahasiswaBaru))
			// .setMaxResults(1).uniqueResult();
			//
			// if (pilihanPaketPerJurusanMhsBaru == null) {
			// pilihanPaketPerJurusanMhsBaru = new
			// PilihanPaketPerJurusanMhsBaru();
			// }
			// session.delete(pilihanPaketPerJurusanMhsBaru);
			//
			// }

		}

		if (deletedMatakuliahs != null) {

			for (PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru : deletedMatakuliahs) {
				session.delete(pilihanPaketPerJurusanMhsBaru);
			}

		}

	}

	/**
	 * Membangun jendela pencarian dan pemilihan {@link Paket} untuk {@code jurusanSekolahMahasiswaBaru}.
	 * Tombol Simpan memanggil {@link #save()} lalu menyegarkan tampilan pemanggil lewat {@code dataLoader}.
	 *
	 * @param jurusanSekolahMahasiswaBaru jurusan PMB yang set paketnya akan dipilih ulang
	 * @param dataLoader                  dipanggil setelah simpan untuk menyegarkan tampilan pemanggil
	 * @param window                      jendela ({@link MyWindow}) yang dipakai ulang untuk menampilkan layar ini
	 */
	public void display(final JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru, final DataLoader dataLoader,
			final MyWindow window) {

		this.jurusanSekolahMahasiswaBaru = jurusanSekolahMahasiswaBaru;

		Common.clear(window);
		window.setTitle("Ambil Data Paket");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Paket");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilPaketHelper.java:275");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("20%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mengisi ulang grid dengan seluruh {@link Paket} aktif (hasil dibatasi {@code Common#MAX_RESULT}
	 * baris; kelas ini tidak menyediakan filter pencarian tambahan).
	 *
	 * @param event tidak dipakai isinya
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Paket> paket = session.createCriteria(Paket.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(paket);
		grid.setRowRenderer(new PaketRenderer());
		grid.setModelCheckMobile(strset);

	}

}
