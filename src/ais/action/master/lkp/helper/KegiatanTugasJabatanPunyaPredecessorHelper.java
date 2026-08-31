package ais.action.master.lkp.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaPredecessor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI (bukan entitas/aksi tersendiri) untuk mengelola daftar
 * <b>predecessor</b> (kegiatan tugas jabatan pendahulu/prasyarat) dari sebuah
 * {@link KegiatanTugasJabatan} pada modul LKP: menampilkan grid predecessor dalam tata letak
 * border, menyediakan tombol tambah (membuka dialog pemilihan banyak kegiatan lewat
 * {@link AmbilDataKegiatanTugasJabatanBanyak}, mengecualikan kegiatan yang sudah ada di grid),
 * serta tombol hapus per baris (dengan konfirmasi) yang menghapus baris
 * {@link KegiatanTugasJabatanPunyaPredecessor} dari basis data. Visibilitas tombol tambah/hapus
 * mengikuti hak akses {@link CommonPrivilages#CREATE}/{@link CommonPrivilages#DELETE} pemanggil.
 */
public class KegiatanTugasJabatanPunyaPredecessorHelper {

	private MyGrid gridKegiatanTugasJabatan;
	private boolean add = false;
	private boolean delete = false;

	/** @param gridKegiatanTugasJabatan grid yang akan diisi/dikelola helper ini */
	public KegiatanTugasJabatanPunyaPredecessorHelper(MyGrid gridKegiatanTugasJabatan) {
		this.gridKegiatanTugasJabatan = gridKegiatanTugasJabatan;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Menyusun tata letak (toolbar tambah + grid predecessor dengan kolom Kode/Nama
	 * Predecessor/Hapus) dan langsung memuat data predecessor {@code kegiatanTugasJabatan}
	 * yang sudah tersimpan.
	 *
	 * @param kegiatanTugasJabatan kegiatan tugas jabatan yang predecessor-nya dikelola
	 * @return komponen tata letak siap pakai untuk ditempelkan ke jendela detail
	 */
	public Borderlayout initDetail(final KegiatanTugasJabatan kegiatanTugasJabatan) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Predecessor", "/img/new.gif");
		add.setVisible(KegiatanTugasJabatanPunyaPredecessorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<KegiatanTugasJabatan> kegiatanTugasJabatans = new ArrayList<KegiatanTugasJabatan>();
				List<Row> myrows = gridKegiatanTugasJabatan.getRows().getChildren();
				for (Row row : myrows) {
					kegiatanTugasJabatans.add(((KegiatanTugasJabatanPunyaPredecessor) row
							.getAttribute("kegiatanTugasJabatanPunyaPredecessor")).getKegiatanTugasJabatan());
				}
				AmbilDataKegiatanTugasJabatanBanyak ambilDataKegiatanTugasJabatanBanyak = new AmbilDataKegiatanTugasJabatanBanyak(
						kegiatanTugasJabatans, kegiatanTugasJabatan.getSatuanKerja());
				ambilDataKegiatanTugasJabatanBanyak.setHeight("95%");
				ambilDataKegiatanTugasJabatanBanyak.setWidth("850px");
				ambilDataKegiatanTugasJabatanBanyak
						.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataKegiatanTugasJabatanBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<KegiatanTugasJabatan> kegiatanTugasJabatans = (List<KegiatanTugasJabatan>) arg0.getData();
						for (KegiatanTugasJabatan kegiatanTugasJabatanP : kegiatanTugasJabatans) {
							KegiatanTugasJabatanPunyaPredecessor kegiatanTugasJabatanPunyaPredecessor = new KegiatanTugasJabatanPunyaPredecessor();
							kegiatanTugasJabatanPunyaPredecessor
									.setKegiatanTugasJabatanPredecessor(kegiatanTugasJabatanP);
							kegiatanTugasJabatanPunyaPredecessor.setKegiatanTugasJabatan(kegiatanTugasJabatan);

							Rows rows = gridKegiatanTugasJabatan.getRows() == null ? new Rows()
									: gridKegiatanTugasJabatan.getRows();
							rows.setParent(gridKegiatanTugasJabatan);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, kegiatanTugasJabatanPunyaPredecessor);
						}
					}
				});

				ambilDataKegiatanTugasJabatanBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridKegiatanTugasJabatan);
		gridKegiatanTugasJabatan.setParent(center);
		gridKegiatanTugasJabatan.setWidth("100%");
		gridKegiatanTugasJabatan.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridKegiatanTugasJabatan);

		MyColumnConfig column = new MyColumnConfig("Kode Predecessor");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Nama Predecessor");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(kegiatanTugasJabatan);

		return borderlayout;
	}

	/** Memuat baris {@link KegiatanTugasJabatanPunyaPredecessor} tersimpan milik {@code kegiatanTugasJabatan} ke dalam grid (kosong bila entitas belum tersimpan). */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KegiatanTugasJabatan kegiatanTugasJabatan) {

		List<KegiatanTugasJabatanPunyaPredecessor> kegiatanTugasJabatanPunyaPredecessors = kegiatanTugasJabatan == null
				|| kegiatanTugasJabatan.getId() == null ? new ArrayList<KegiatanTugasJabatanPunyaPredecessor>()
						: HibernateUtil.currentSession().createCriteria(KegiatanTugasJabatanPunyaPredecessor.class)
								.add(Restrictions.eq("kegiatanTugasJabatan", kegiatanTugasJabatan)).list();

		Rows rows = gridKegiatanTugasJabatan.getRows() == null ? new Rows() : gridKegiatanTugasJabatan.getRows();
		rows.setParent(gridKegiatanTugasJabatan);

		for (KegiatanTugasJabatanPunyaPredecessor kegiatanTugasJabatanPunyaPredecessor : kegiatanTugasJabatanPunyaPredecessors) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, kegiatanTugasJabatanPunyaPredecessor);
		}
	}

	/**
	 * Mengisi satu baris grid dengan kode/nama predecessor dan tombol hapus (dengan dialog
	 * konfirmasi); menghapus baris database dan menyembunyikan baris UI bila dikonfirmasi.
	 *
	 * @param row                                     baris grid yang diisi
	 * @param kegiatanTugasJabatanPunyaPredecessor     data relasi predecessor untuk baris ini
	 */
	public void initRow(final Row row,
			final KegiatanTugasJabatanPunyaPredecessor kegiatanTugasJabatanPunyaPredecessor) {
		row.setValign("top");row.setAttribute("kegiatanTugasJabatanPunyaPredecessor", kegiatanTugasJabatanPunyaPredecessor);

		new Label(kegiatanTugasJabatanPunyaPredecessor.getKegiatanTugasJabatanPredecessor() == null ? ""
				: kegiatanTugasJabatanPunyaPredecessor.getKegiatanTugasJabatanPredecessor().getKode()).setParent(row);

		new Label(kegiatanTugasJabatanPunyaPredecessor.getKegiatanTugasJabatanPredecessor() == null ? ""
				: kegiatanTugasJabatanPunyaPredecessor.getKegiatanTugasJabatanPredecessor().getNama()).setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (kegiatanTugasJabatanPunyaPredecessor.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.delete(kegiatanTugasJabatanPunyaPredecessor);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
