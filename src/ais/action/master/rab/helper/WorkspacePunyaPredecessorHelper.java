package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaPredecessor;

/**
 * Helper UI untuk mengelola relasi banyak-ke-banyak <b>predecessor</b> (workspace pendahulu yang
 * harus selesai lebih dulu) pada satu {@link Workspace} modul RAB, ditampilkan sebagai grid
 * tambah/hapus di dalam layar detail workspace. Menu tambah membuka dialog pemilihan workspace
 * ({@code AmbilDataWorkspaceBanyak}) yang mengecualikan workspace yang sudah menjadi predecessor;
 * hapus baris langsung menghapus baris {@link WorkspacePunyaPredecessor} dari database (bila sudah
 * tersimpan) setelah konfirmasi. Visibilitas tombol tambah/hapus mengikuti hak akses pengguna
 * ({@link CommonPrivilages}).
 */
public class WorkspacePunyaPredecessorHelper {

	private MyGrid gridWorkspace;
	private boolean add = false;
	private boolean delete = false;

	/** Membuat helper terikat ke {@code gridWorkspace}, menentukan visibilitas tombol tambah/hapus dari hak akses pengguna saat ini. */
	public WorkspacePunyaPredecessorHelper(MyGrid gridWorkspace) {
		this.gridWorkspace = gridWorkspace;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun tata letak grid predecessor lengkap dengan toolbar tambah, tiga kolom (kode, nama,
	 * hapus), dan langsung memuat data predecessor {@code workspace} yang sudah ada.
	 *
	 * @param workspace workspace yang predecessor-nya akan ditampilkan/dikelola
	 * @return tata letak {@link Borderlayout} siap ditempel ke komponen induk
	 */
	public Borderlayout initDetail(final Workspace workspace) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Predecessor",
				"/img/new.gif");
		add.setVisible(WorkspacePunyaPredecessorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Workspace> workspaces = new ArrayList<Workspace>();
				List<Row> myrows = gridWorkspace.getRows().getChildren();
				for (Row row : myrows) {
					workspaces.add(((WorkspacePunyaPredecessor) row
							.getAttribute("workspacePunyaPredecessor"))
							.getWorkspace());
				}
				AmbilDataWorkspaceBanyak ambilDataWorkspaceBanyak = new AmbilDataWorkspaceBanyak(
						true, workspaces);
				ambilDataWorkspaceBanyak.setHeight("95%");
				ambilDataWorkspaceBanyak.setWidth("850px");
				ambilDataWorkspaceBanyak.setParent(ExecutionsCtrl
						.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataWorkspaceBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Workspace> workspaces = (List<Workspace>) arg0
								.getData();
						for (Workspace workspaceP : workspaces) {
							WorkspacePunyaPredecessor workspacePunyaPredecessor = new WorkspacePunyaPredecessor();
							workspacePunyaPredecessor
									.setWorkspacePredecessor(workspaceP);
							workspacePunyaPredecessor.setWorkspace(workspace);

							Rows rows = gridWorkspace.getRows() == null ? new Rows()
									: gridWorkspace.getRows();
							rows.setParent(gridWorkspace);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, workspacePunyaPredecessor);
						}
					}
				});

				ambilDataWorkspaceBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridWorkspace);
		gridWorkspace.setParent(center);
		gridWorkspace.setWidth("100%");
		gridWorkspace.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridWorkspace);

		MyColumnConfig column = new MyColumnConfig("Kode Predecessor");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Nama Predecessor");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(workspace);

		return borderlayout;
	}

	/** Memuat seluruh baris predecessor tersimpan milik {@code workspace} ke grid, atau tidak menambah baris apa pun bila workspace belum tersimpan. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Workspace workspace) {

		List<WorkspacePunyaPredecessor> workspacePunyaPredecessors = workspace == null
				|| workspace.getId() == null ? new ArrayList<WorkspacePunyaPredecessor>()
				: HibernateUtil.currentSession()
						.createCriteria(WorkspacePunyaPredecessor.class)
						.add(Restrictions.eq("workspace", workspace)).list();

		Rows rows = gridWorkspace.getRows() == null ? new Rows()
				: gridWorkspace.getRows();
		rows.setParent(gridWorkspace);

		for (WorkspacePunyaPredecessor workspacePunyaPredecessor : workspacePunyaPredecessors) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, workspacePunyaPredecessor);
		}
	}

	/** Mengisi satu baris grid dengan kode+nama workspace predecessor dan tombol hapus (dengan konfirmasi) yang, saat disetujui, menghapus baris dari database bila sudah tersimpan lalu menyembunyikan/melepas baris dari grid. */
	public void initRow(final Row row,
			final WorkspacePunyaPredecessor workspacePunyaPredecessor) {
		row.setValign("top");row.setAttribute("workspacePunyaPredecessor", workspacePunyaPredecessor);

		new Label(
				workspacePunyaPredecessor.getWorkspacePredecessor() == null ? ""
						: workspacePunyaPredecessor.getWorkspacePredecessor()
								.getKode()).setParent(row);

		new Label(
				workspacePunyaPredecessor.getWorkspacePredecessor() == null ? ""
						: workspacePunyaPredecessor.getWorkspacePredecessor()
								.getNama()).setParent(row);

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
									if (workspacePunyaPredecessor.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(workspacePunyaPredecessor);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
