package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.generic.AmbilDataPemeriksaBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.PenerbitPunyaPemeriksa;

/**
 * Helper UI ZK untuk mengelola relasi "pemeriksa" ({@link PenerbitPunyaPemeriksa}, pengguna yang
 * ditugaskan memeriksa/mereview karya) pada satu {@link DomainPenelitian} milik penerbit di
 * modul perpustakaan. Dipasang pada panel detail satu domain penelitian, menampilkan daftar
 * pemeriksa sebagai grid dengan checkbox status aktif per baris dan tombol tambah/hapus.
 *
 * <p>
 * Tombol tambah membuka dialog {@link AmbilDataPemeriksaBanyak} (pemilih pengguna multi-pilih,
 * dengan pemeriksa yang sudah terdaftar dikecualikan dari pilihan) dan langsung menyimpan baris
 * {@link PenerbitPunyaPemeriksa} baru (berstatus aktif) untuk setiap pengguna yang dipilih, bila
 * domain penelitian sudah tersimpan. Checkbox aktif per baris memperbarui status langsung ke
 * database saat diubah. Tombol hapus per baris meminta konfirmasi sebelum menghapus baris relasi.
 * Visibilitas tombol tambah/hapus mengikuti privilese pengguna saat ini ({@link CommonPrivilages}).
 * </p>
 */
public class DomainPenelitianPunyaPemeriksaHelper {

	private MyGrid gridPemeriksa;
	private boolean add = false;
	private boolean delete = false;

	/** Membangun helper terikat pada {@code gridPemeriksa} dan menghitung hak tambah/hapus pengguna saat ini. */
	public DomainPenelitianPunyaPemeriksaHelper(MyGrid gridPemeriksa) {
		this.gridPemeriksa = gridPemeriksa;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel (border layout) berisi toolbar "Tambah Pemeriksa" dan grid daftar
	 * pemeriksa untuk {@code domainPenelitian}, lalu memuat data pemeriksa yang sudah tersimpan.
	 *
	 * @param domainPenelitian domain penelitian yang detail pemeriksanya ditampilkan/dikelola
	 * @return border layout siap disisipkan sebagai konten panel detail
	 */
	@SuppressWarnings("unchecked")
	public Borderlayout initDetail(final DomainPenelitian domainPenelitian) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Pemeriksa",
				"/img/new.gif");
		add.setVisible(DomainPenelitianPunyaPemeriksaHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<Tbmuser> pemeriksas = new ArrayList<Tbmuser>();
				List<Row> myrows = gridPemeriksa.getRows().getChildren();
				for (Row row : myrows) {
					pemeriksas.add(((PenerbitPunyaPemeriksa) row
							.getAttribute("penerbitPunyaPemeriksa"))
							.getPemeriksa());
				}
				AmbilDataPemeriksaBanyak ambilDataPemeriksaBanyak = new AmbilDataPemeriksaBanyak(
						pemeriksas);
				ambilDataPemeriksaBanyak.setHeight("95%");
				ambilDataPemeriksaBanyak.setWidth("90%");
				ambilDataPemeriksaBanyak.setParent(ExecutionsCtrl
						.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataPemeriksaBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> pemeriksas = (List<Tbmuser>) arg0
								.getData();
						for (Tbmuser pemeriksa : pemeriksas) {
							PenerbitPunyaPemeriksa penerbitPunyaPemeriksa = new PenerbitPunyaPemeriksa();
							penerbitPunyaPemeriksa
									.setDomainPenelitian(domainPenelitian);
							penerbitPunyaPemeriksa.setPenerbit(domainPenelitian
									.getPenerbit());
							penerbitPunyaPemeriksa.setPemeriksa(pemeriksa);
							penerbitPunyaPemeriksa.setAktif(true);

							if (domainPenelitian.getId() != null) {
								Session session = HibernateUtil
										.currentSession();
								session.save(penerbitPunyaPemeriksa);
							}

							Rows rows = gridPemeriksa.getRows() == null ? new Rows()
									: gridPemeriksa.getRows();
							rows.setParent(gridPemeriksa);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, penerbitPunyaPemeriksa);
						}
					}
				});

				ambilDataPemeriksaBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPemeriksa);
		gridPemeriksa.setParent(center);
		gridPemeriksa.setWidth("100%");
		gridPemeriksa.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPemeriksa);

		MyColumnConfig column = new MyColumnConfig("Pemeriksa");
		column.setParent(columns);

		column = new MyColumnConfig("Aktif");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(domainPenelitian);

		return borderlayout;
	}

	/** Memuat baris-baris pemeriksa tersimpan untuk {@code domainPenelitian} dari database dan merendernya ke grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final DomainPenelitian domainPenelitian) {

		List<PenerbitPunyaPemeriksa> penerbitPunyaPemeriksas = domainPenelitian == null
				|| domainPenelitian.getId() == null ? new ArrayList<PenerbitPunyaPemeriksa>()
				: HibernateUtil
						.currentSession()
						.createCriteria(PenerbitPunyaPemeriksa.class)
						.add(Restrictions.eq("domainPenelitian",
								domainPenelitian)).list();

		Rows rows = gridPemeriksa.getRows() == null ? new Rows()
				: gridPemeriksa.getRows();
		rows.setParent(gridPemeriksa);

		for (PenerbitPunyaPemeriksa penerbitPunyaPemeriksa : penerbitPunyaPemeriksas) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, penerbitPunyaPemeriksa);
		}
	}

	/**
	 * Mengisi {@code row} dengan nama pemeriksa, checkbox status aktif (memperbarui database
	 * langsung saat diubah), dan tombol hapus (bila pengguna berhak); tombol hapus meminta
	 * konfirmasi, lalu menghapus baris relasi dari database (bila sudah tersimpan) dan melepas
	 * baris dari tampilan.
	 */
	public void initRow(final Row row,
			final PenerbitPunyaPemeriksa penerbitPunyaPemeriksa) {
		row.setValign("top");row.setAttribute("penerbitPunyaPemeriksa", penerbitPunyaPemeriksa);

		new Label(penerbitPunyaPemeriksa.getPemeriksa() == null ? ""
				: penerbitPunyaPemeriksa.getPemeriksa().toString())
				.setParent(row);

		final MyCheckboxConfig aktif = new MyCheckboxConfig();
		aktif.setChecked(penerbitPunyaPemeriksa.getAktif());
		aktif.setParent(row);
		aktif.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penerbitPunyaPemeriksa.setAktif(aktif.isChecked());
				row.setValign("top");row.setAttribute("penerbitPunyaPemeriksa",
						penerbitPunyaPemeriksa);
				if (penerbitPunyaPemeriksa.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(penerbitPunyaPemeriksa);
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (penerbitPunyaPemeriksa.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(penerbitPunyaPemeriksa);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
