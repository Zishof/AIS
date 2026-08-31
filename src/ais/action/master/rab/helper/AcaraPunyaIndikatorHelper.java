package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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

import ais.action.master.rab.util.WorkspaceSelecter;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Acara;
import ais.database.model.rab.AcaraPunyaIndikator;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Helper UI ZK modul RAB (Rencana Anggaran Biaya) untuk mencatat realisasi indikator kinerja
 * ({@link AcaraPunyaIndikator}) dari satu {@link Acara} (kegiatan) terhadap indikator yang sudah
 * ditetapkan pada item perencanaan ({@link Workspace}) terkait, lewat
 * {@link WorkspacePunyaIndikator}. Dipasang pada panel detail satu acara, menampilkan daftar
 * indikator sebagai grid dengan kolom target (read-only, dari perencanaan) dan realisasi/
 * keterangan yang dapat diedit.
 *
 * <p>
 * Tombol "Tambah Realisasi" mengambil workspace terpilih dari {@code selecter} (atau workspace
 * bawaan acara bila selecter tidak memilih apa pun), menolak bila workspace tidak aktif atau
 * belum memiliki indikator, lalu menambahkan satu baris {@link AcaraPunyaIndikator} untuk
 * <b>setiap</b> {@link WorkspacePunyaIndikator} milik workspace tersebut sekaligus. Setelah
 * indikator ditambahkan (atau bila sudah ada indikator tersimpan), komponen pemilih workspace
 * pada {@code selecter} dikunci — memastikan satu acara hanya terkait pada satu workspace/
 * kelompok indikator. Mengubah nilai realisasi/keterangan pada baris memperbarui atribut objek
 * di memori (belum tentu langsung ke database — lihat kode {@code onChange} masing-masing field);
 * tombol hapus per baris meminta konfirmasi, menghapus dari database, dan membuka kembali kunci
 * pemilih workspace bila grid menjadi kosong.
 * </p>
 */
public class AcaraPunyaIndikatorHelper {

	private MyGrid gridIndikator;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;
	private WorkspaceSelecter selecter;

	/** Membangun helper terikat pada {@code gridIndikator} dan menghitung hak tambah/ubah/hapus pengguna saat ini. */
	public AcaraPunyaIndikatorHelper(MyGrid gridIndikator) {
		this.gridIndikator = gridIndikator;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel (border layout) berisi toolbar "Tambah Realisasi" dan grid daftar
	 * indikator untuk {@code acara}, lalu memuat data indikator yang sudah tersimpan.
	 *
	 * @param acara    kegiatan yang detail realisasi indikatornya ditampilkan/dikelola
	 * @param selecter komponen pemilih workspace/item perencanaan, sumber indikator saat menambah baris baru
	 * @return border layout siap disisipkan sebagai konten panel detail
	 */
	public Borderlayout initDetail(final Acara acara,
			final WorkspaceSelecter selecter) {
		this.selecter = selecter;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Realisasi",
				"/img/new.gif");
		add.setVisible(AcaraPunyaIndikatorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Workspace workspace = (Workspace) (selecter.select() == null ? acara
						.getWorkspace() : selecter.select().getAttribute(
						"workspace"));
				if (workspace == null || !workspace.getAktif()) {
					MyMessageboxConfig
							.show("Pilih salah satu item perencanaan",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					return;
				}

				List<WorkspacePunyaIndikator> workspacePunyaIndikators = HibernateUtil
						.currentSession()
						.createCriteria(WorkspacePunyaIndikator.class)
						.add(Restrictions.eq("workspace", workspace)).list();
				if (workspacePunyaIndikators.size() == 0) {
					MyMessageboxConfig
							.show("Item perencanaan ini tidak memiliki indikator",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					return;
				}

				for (WorkspacePunyaIndikator workspacePunyaIndikator : workspacePunyaIndikators) {
					AcaraPunyaIndikator acaraPunyaIndikator = new AcaraPunyaIndikator();
					acaraPunyaIndikator.setAcara(acara);
					acaraPunyaIndikator
							.setWorkspacePunyaIndikator(workspacePunyaIndikator);

					Rows rows = gridIndikator.getRows() == null ? new Rows()
							: gridIndikator.getRows();
					rows.setParent(gridIndikator);
					Row row = new Row();row.setValign("top");
					row.setParent(rows);
					initRow(row, acaraPunyaIndikator);
				}

				if (selecter.select() != null) {
					selecter.select().setDisabled(true);
				}
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridIndikator);
		gridIndikator.setParent(center);
		gridIndikator.setWidth("100%");
		gridIndikator.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridIndikator);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Isi Indikator");
		column.setParent(columns);

		column = new MyColumnConfig("Target");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Realisasi");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Satuan");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Output Indikator");
		column.setParent(columns);

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("8%");

		loadDataDetail(acara);

		return borderlayout;
	}

	/** Memuat baris-baris indikator tersimpan untuk {@code acara} dari database, mengunci pemilih workspace bila sudah ada indikator, dan merender baris ke grid. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Acara acara) {

		List<AcaraPunyaIndikator> acaraPunyaIndikators = acara == null
				|| acara.getId() == null ? new ArrayList<AcaraPunyaIndikator>()
				: HibernateUtil.currentSession()
						.createCriteria(AcaraPunyaIndikator.class)
						.add(Restrictions.eq("acara", acara)).list();

		if (this.selecter.select() != null) {
			this.selecter.select()
					.setDisabled(acaraPunyaIndikators.size() != 0);
		}

		Rows rows = gridIndikator.getRows() == null ? new Rows()
				: gridIndikator.getRows();
		rows.setParent(gridIndikator);

		for (AcaraPunyaIndikator acaraPunyaIndikator : acaraPunyaIndikators) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, acaraPunyaIndikator);
		}
	}

	/**
	 * Mengisi {@code row} dengan kode dan nama indikator, target (read-only, dari perencanaan),
	 * kolom realisasi dan keterangan yang dapat diedit (bila pengguna berhak ubah), serta tombol
	 * hapus (bila pengguna berhak); tombol hapus meminta konfirmasi, menghapus baris dari
	 * database, dan membuka kembali kunci pemilih workspace bila grid menjadi kosong.
	 */
	public void initRow(final Row row,
			final AcaraPunyaIndikator acaraPunyaIndikator) {
		row.setValign("top");row.setAttribute("acaraPunyaIndikator", acaraPunyaIndikator);

		new Label(acaraPunyaIndikator.getWorkspacePunyaIndikator() == null
				|| acaraPunyaIndikator.getWorkspacePunyaIndikator()
						.getIndikator() == null ? "" : acaraPunyaIndikator
				.getWorkspacePunyaIndikator().getIndikator().getKode())
				.setParent(row);

		new Label(acaraPunyaIndikator.getWorkspacePunyaIndikator() == null
				|| acaraPunyaIndikator.getWorkspacePunyaIndikator()
						.getIndikator() == null ? "" : acaraPunyaIndikator
				.getWorkspacePunyaIndikator().getIndikator().getNama())
				.setParent(row);

		new Label(acaraPunyaIndikator.getWorkspacePunyaIndikator() == null
				|| acaraPunyaIndikator.getWorkspacePunyaIndikator()
						.getNilaiTarget() == null ? ""
				: Common.numberFormat.get().format(acaraPunyaIndikator
						.getWorkspacePunyaIndikator().getNilaiTarget()))
				.setParent(row);

		final MyDoublebox realisasi = new MyDoublebox(
				acaraPunyaIndikator.getRealisasi());
		realisasi.setParent(row);
		realisasi.setDisabled(!edit);
		realisasi.setWidth("90%");
		realisasi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				acaraPunyaIndikator.setRealisasi(realisasi.getValue());
				row.setValign("top");row.setAttribute("acaraPunyaIndikator", acaraPunyaIndikator);
			}
		});

		new Label(
				acaraPunyaIndikator.getWorkspacePunyaIndikator() == null
						|| acaraPunyaIndikator.getWorkspacePunyaIndikator()
								.getSatuan() == null ? "" : acaraPunyaIndikator
						.getWorkspacePunyaIndikator().getSatuan().getNama())
				.setParent(row);

		new Label(acaraPunyaIndikator.getWorkspacePunyaIndikator() == null ? ""
				: acaraPunyaIndikator.getWorkspacePunyaIndikator().getOutput())
				.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				acaraPunyaIndikator.getKeterangan());
		keterangan.setParent(row);
		keterangan.setRows(2);
		keterangan.setDisabled(!edit);
		keterangan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				acaraPunyaIndikator.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("acaraPunyaIndikator", acaraPunyaIndikator);
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
									if (acaraPunyaIndikator.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(acaraPunyaIndikator);
									}
	row.setVisible(false);row.detach();

									if (selecter.select() != null) {
										selecter.select().setDisabled(
												gridIndikator.getRows() != null
														&& gridIndikator
																.getRows()
																.getChildren()
																.size() != 0);
									}
								}

							}
						});

			}
		});

	}

}
