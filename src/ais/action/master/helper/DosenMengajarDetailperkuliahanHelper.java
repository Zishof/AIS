package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper composer ZK sisi dosen untuk menampilkan (read-only) daftar mahasiswa yang mengikuti satu
 * {@link Perkuliahan} yang diampu dosen tersebut, lengkap dengan foto, angkatan, total nilai/nilai
 * huruf, dan semester. Hanya menampilkan {@link Detailperkuliahan} yang statusnya
 * {@link Detailperkuliahan#DISETUJUI} dan belum resmi "mengikuti perkuliahan"
 * ({@code ikutiPerkuliahan} null). Tidak menyediakan tambah/hapus data (berbeda dari helper
 * "Detail*" lain di paket ini) — berfokus pada pencarian (filter NIM/nama) dan aksi cetak
 * (laporan absensi lewat {@link CommonReportHelper#onLaporanAbsensi}, laporan nilai lewat
 * {@link DetailperkuliahanForPenilaianHelper#onLaporan}, serta ekspor data grid lewat
 * {@link Common#cetakData}).
 *
 * <p>
 * Mengimplementasikan {@link DataCriteria} sehingga kriteria pencarian ({@link #initCriteria})
 * dapat dipakai ulang oleh mekanisme paging/ekspor umum ({@link Common#initPaging},
 * {@link Common#cetakData}), dan {@link DataLoader} sebagai callback muat-ulang grid.
 * </p>
 */
public class DosenMengajarDetailperkuliahanHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Perkuliahan perkuliahan;
	private Textbox nim;
	private Textbox nama;
	private boolean ispaging = false;
	private Paging paging;

	/** Membuat helper tanpa paging server-side (grid memakai paging bawaan ZK di sisi klien). */
	public DosenMengajarDetailperkuliahanHelper() {

	}

	/**
	 * @param ispaging bila {@code true}, data dimuat per halaman lewat {@link Paging} server-side
	 *                 ({@link Common#initPaging}) alih-alih memuat seluruh hasil sekaligus
	 */
	public DosenMengajarDetailperkuliahanHelper(boolean ispaging) {
		this.ispaging = ispaging;
		if (ispaging) {
			paging = new Paging();
			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});
		}
	}

	/**
	 * Perender baris grid untuk satu {@link Detailperkuliahan}: foto mahasiswa, riwayat revisi
	 * ({@link RevisiHelper}), nama, angkatan/semester mulai, total nilai + nilai huruf (atau
	 * "Belum dinilai" bila kosong), dan semester berjalan.
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) data;

			CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);

			RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
					detailperkuliahan.getMahasiswa().getNim()).setParent(row);

			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(row);
			new Label(detailperkuliahan.getMahasiswa().getTahunangkatan() + " / "
					+ detailperkuliahan.getMahasiswa().getSemesterMulai()).setParent(row);

			ais.ui.util.NilaiHurufAnalisisPopupHelper.buatLabel(detailperkuliahan.getTotalNilai() == null ? "0.0 (Belum dinilai)"
					: Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ (detailperkuliahan.getNilaiHuruf() == null
									|| detailperkuliahan.getNilaiHuruf().trim().equals("") ? "Belum dinilai"
											: detailperkuliahan.getNilaiHuruf())
							+ ")", detailperkuliahan).setParent(row);

			final Label semester = new Label(
					detailperkuliahan.getSemester() == null ? "" : detailperkuliahan.getSemester().toString());
			semester.setParent(row);

		}

	}

	/**
	 * Membangun kriteria Hibernate untuk {@link Detailperkuliahan} yang disetujui dan belum resmi
	 * mengikuti perkuliahan, milik {@link #perkuliahan} yang sedang aktif, difilter opsional
	 * berdasarkan isi textbox {@link #nim}/{@link #nama} (pencarian ILIKE anywhere).
	 *
	 * @param order bila {@code true}, hasil diurutkan berdasarkan {@code mahasiswa.nim} ascending
	 * @return criteria siap dieksekusi (belum di-list), dipakai baik untuk memuat data maupun
	 *         untuk paging/ekspor
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI));

		criteria.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
				.add(nim == null || nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("perkuliahan", perkuliahan));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	/**
	 * Memuat ulang isi grid berdasarkan {@link #initCriteria(boolean)}. Bila {@link #ispaging}
	 * aktif, hanya satu halaman ({@link Common#ROWS_COUNT_ON_PAGE} baris) yang diambil sesuai
	 * halaman aktif pada {@link #paging}; bila tidak, seluruh hasil dimuat sekaligus (paging
	 * bawaan grid sisi klien).
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		if (ispaging) {
			Common.initPaging(initCriteria(false), paging);
			List<Detailperkuliahan> myDetailperkuliahans = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(myDetailperkuliahans);
			grid.setRowRenderer(new DetailPerkuliahanRenderer());
			grid.setModelCheckMobile(strset);
		} else {

			List<Detailperkuliahan> myDetailperkuliahans = initCriteria(true).list();
			ListModel strset = new SimpleListModel(myDetailperkuliahans);
			grid.setRowRenderer(new DetailPerkuliahanRenderer());
			grid.setModelCheckMobile(strset);
		}

	}

	/** Mengganti {@link Perkuliahan} target tanpa membangun ulang seluruh UI. */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Membangun UI lengkap: judul, toolbar pencarian (NIM/nama + tombol cari), tombol cetak
	 * laporan absensi dan nilai, tombol ekspor data grid, serta grid daftar mahasiswa. Lalu
	 * memuat datanya.
	 *
	 * @param perkuliahan kelas matakuliah yang pesertanya ditampilkan
	 * @param component   komponen induk ZK; isinya dibersihkan lebih dulu lewat
	 *                    {@link Common#clear(Component)}
	 */
	public void display(final Perkuliahan perkuliahan, final Component component) {
		this.perkuliahan = perkuliahan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("NIM : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setCols(10);
		nim.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, false);
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Nilai", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				DetailperkuliahanForPenilaianHelper.onLaporan(perkuliahan);
			}
		});
		print.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "perkuliahan", "mahasiswa", "semester",
				"tahunAkademik", "persetujuan");
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setStyle("min-height: 200px;");
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
		column.setLabel("NIM");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");
		column.setWidth("10%");

		if (ispaging) {
			groupbox.appendChild(paging);
		}

		loadData(null);

	}

}
