package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailperkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.beasiswa.BeasiswaPunyaPersyaratan;
import ais.database.model.beasiswa.PersyaratanBeasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Jendela pemilihan {@link PersyaratanBeasiswa} (master syarat/form input beasiswa) yang akan
 * diterapkan pada satu {@link Beasiswa}. Menampilkan grid ber-checkbox dari seluruh syarat
 * beasiswa aktif (paging server-side lewat {@code AmbilDataPagingHelper}); centang mencerminkan
 * relasi {@link BeasiswaPunyaPersyaratan} yang sudah ada untuk beasiswa tersebut. Saat disimpan
 * ({@link #save()}), seluruh relasi lama untuk beasiswa ini dihapus lalu dibuat ulang dari
 * baris yang tercentang pada grid (replace-all, bukan diff incremental).
 */
public class AmbilDataSyaratBeasiswaHelper {

	/** Beasiswa yang syarat-syaratnya sedang dipilih/diedit, ditetapkan lewat konstruktor. */
	private Beasiswa beasiswa;
	/** Grid daftar syarat beasiswa yang sedang ditampilkan, diisi ulang oleh {@link #onSearchDefault(Event)}. */
	private MyGrid grid;


	/** Paging server-side per 5 baris (pola {@code AmbilDataPagingHelper}), dipasang ke {@link #grid} di {@link #display(MyWindow, EventListener)} dan dipakai {@link #onSearchDefault(Event)} lewat {@code cariDenganCriteria}. */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	// private Textbox nim;
	// private Textbox nama;
	// private Decimalbox tahunangkatan;
	// private Textbox dariNim;
	// private Textbox sampaiNim;
	//
	// private Combobox searchstatusmahasiswa = new Combobox();
	//
	// private Combobox searchfakultas = new Combobox();
	// private Combobox searchjurusan = new Combobox();

	/** Dikumpulkan oleh renderer saat checkbox baris dilepas centangnya; saat ini tidak dikonsumsi di tempat lain — {@link #save()} menghapus seluruh relasi lama lewat SQL native, bukan lewat daftar ini. */
	List<BeasiswaPunyaPersyaratan> delete = new ArrayList<BeasiswaPunyaPersyaratan>();

	/** @param beasiswa beasiswa yang syarat-syaratnya akan dipilih/diedit lewat {@link #display} */
	public AmbilDataSyaratBeasiswaHelper(Beasiswa beasiswa) {
		this.beasiswa = beasiswa;

	}

	/** Merender satu baris grid: checkbox status terpilih (dicentang bila relasi {@link BeasiswaPunyaPersyaratan} sudah ada) dan atribut syarat beasiswa (nama, label input, tipe data, nilai, wajib lampiran). */
	class PersyaratanBeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris syarat beasiswa ({@code arg1}, harus {@link PersyaratanBeasiswa}):
		 * checkbox (tercentang bila relasi {@link BeasiswaPunyaPersyaratan} untuk {@link #beasiswa}
		 * sudah ada), nama syarat, label input, tipe data, nilai data, dan status wajib lampiran.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanBeasiswa persyaratanBeasiswa = (PersyaratanBeasiswa) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("persyaratan_beasiswa", persyaratanBeasiswa);

			// Integer jml = 0;

			final BeasiswaPunyaPersyaratan beasiswaPunyaPersyaratan = (BeasiswaPunyaPersyaratan) HibernateUtil
					.currentSession().createCriteria(BeasiswaPunyaPersyaratan.class)
					.add(Restrictions.eq("beasiswa", beasiswa))
					.add(Restrictions.eq("persyaratanBeasiswa", persyaratanBeasiswa)).setMaxResults(1).uniqueResult();

			// System.out.println(jml);

			checkbox.setChecked(beasiswaPunyaPersyaratan != null);

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					if (!checkbox.isChecked()) {
						delete.add(beasiswaPunyaPersyaratan);
					}
				}
			});

			new Label(persyaratanBeasiswa.getNama()).setParent(arg0);
			new Label(persyaratanBeasiswa.getLabelInputan()).setParent(arg0);
			new Label(persyaratanBeasiswa.getTipeDataInputan()).setParent(arg0);
			new Label(persyaratanBeasiswa.getNilaiDataInputan()).setParent(arg0);
			new Label(persyaratanBeasiswa.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);
		}
	}

	/**
	 * Menyimpan pilihan syarat beasiswa: menghapus seluruh baris {@link BeasiswaPunyaPersyaratan}
	 * milik {@link #beasiswa} lewat SQL native, lalu membuat baris baru untuk setiap baris grid
	 * yang checkbox-nya tercentang. Kegagalan membaca satu baris (mis. cast gagal) diabaikan
	 * secara diam-diam agar tidak menggagalkan penyimpanan baris lain.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {

		DetailperkuliahanDao detailperkuliahanDao = DaoFactory.getInstance().getDetailperkuliahanDao();
		Session session = detailperkuliahanDao.getCurrentSession();

		session.createSQLQuery("delete from beasiswa_punya_persyaratan where beasiswa=" + beasiswa.getId())
				.executeUpdate();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					PersyaratanBeasiswa persyaratanBeasiswa = (PersyaratanBeasiswa) checkbox
							.getAttribute("persyaratan_beasiswa");

					BeasiswaPunyaPersyaratan beasiswaPunyaPersyaratan = new BeasiswaPunyaPersyaratan();
					beasiswaPunyaPersyaratan.setBeasiswa(beasiswa);
					beasiswaPunyaPersyaratan.setPersyaratanBeasiswa(persyaratanBeasiswa);
					beasiswaPunyaPersyaratan.setNama(beasiswa.getNama() + " --> " + persyaratanBeasiswa.getNama());
					session.save(beasiswaPunyaPersyaratan);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratBeasiswaHelper.java:134");
				// TODO: handle exception
			}
		}

	}

	// public boolean delete(
	// List<BeasiswaPunyaPersyaratan> beasiswaPunyaPersyaratans) {
	// for (BeasiswaPunyaPersyaratan b : beasiswaPunyaPersyaratans) {
	// try{
	// Common.refreshDelete(b);
	// }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratBeasiswaHelper.java:146");
	// Common.tampilErrorJikaAdmin(e);
	// }
	// }
	//
	// if (beasiswaPunyaPersyaratans.size() == 0) {
	// return true;
	// } else {
	// return false;
	// }
	//
	// }

	/**
	 * Membangun jendela modal berisi grid syarat beasiswa (dengan checkbox "pilih semua" pada
	 * header kolom) dan toolbar Simpan/Batal, lalu menampilkannya sebagai modal
	 * ({@link MyWindow#onModal()}). Tombol Simpan memanggil {@link #save()} sebelum menyembunyikan
	 * jendela; kedua tombol memanggil {@code eventListener} (bila diberikan) setelah selesai agar
	 * pemanggil dapat menyegarkan tampilannya.
	 *
	 * @param window        jendela yang akan diisi dan ditampilkan sebagai modal
	 * @param eventListener callback opsional dipanggil setelah jendela ditutup (baik simpan
	 *                      maupun batal)
	 */
	public void display(final MyWindow window, final EventListener eventListener) {

		Common.clear(window);
		window.setTitle("Daftar Form Input dan Persyatan Beasiswa");
		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		// panel.setTitle("Daftar Mahasiswa");
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
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

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
						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratBeasiswaHelper.java:226");

					}

					// PersyaratanBeasiswaOnCheck mahasiswaOnCheck =
					// (PersyaratanBeasiswaOnCheck) myCheckbox
					// .getAttribute("mahasiswaOnCheck");

					// mahasiswaOnCheck.onEvent(arg0);

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Label Input");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe Data");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Data");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lampiran");

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				// dataLoader.loadData(null);
				window.setVisible(false);
				if (eventListener != null) {
					eventListener.onEvent(null);
				}
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
				if (eventListener != null) {
					eventListener.onEvent(null);
				}
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memuat/menyegarkan grid dengan seluruh {@link PersyaratanBeasiswa} aktif, diurutkan berdasarkan id, memakai paging server-side {@code AmbilDataPagingHelper}. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<PersyaratanBeasiswa> persyaratanBeasiswa = pagingHelper.cariDenganCriteria(session.createCriteria(PersyaratanBeasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id"))

				.setMaxResults(Common.MAX_RESULT), PersyaratanBeasiswa.class);
		ListModel strset = new SimpleListModel(persyaratanBeasiswa);
		grid.setRowRenderer(new PersyaratanBeasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
