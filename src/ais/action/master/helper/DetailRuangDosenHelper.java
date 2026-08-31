package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Ruang;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer untuk fitur "Ruang" (ruangan) master: menampilkan dan mengelola daftar
 * {@link Dosen} yang ditugaskan memakai satu {@link Ruang} tertentu (mis. ruang dosen bersama).
 * Menyediakan grid berpaging dengan filter pencarian (nama, fakultas, prodi), tombol untuk
 * menambah dosen ke ruang lewat {@link AmbilDataDosenUntukRuangHelper}, dan tombol hapus per baris
 * yang melepas relasi dosen-ruang (bukan menghapus data dosen).
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} agar dapat dipanggil balik ({@code loadData}) oleh helper
 * lain (mis. {@link AmbilDataDosenUntukRuangHelper}) setelah operasi tambah data selesai, sehingga
 * grid otomatis menyegarkan diri.
 * </p>
 */
public class DetailRuangDosenHelper implements DataLoader {

	private MyGrid grid;
	private Ruang ruang;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Textbox nama;

	/** Menyiapkan combobox filter fakultas/jurusan (termasuk opsi "Semua") lewat {@link Common#initFakultasDanJurusanDanSemua}. */
	public DetailRuangDosenHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Perender baris grid: menampilkan foto, kode/NIDN, nama, jurusan, fakultas dosen, serta tombol hapus (dengan konfirmasi) yang melepas relasi {@code dosen.ruang}. */
	class DetailDosenRenderer extends ais.ui.util.MyRowRenderer {

		public DetailDosenRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			new Label(dosen.getCode() + " / " + dosen.getNidn()).setParent(vbox);

			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);
			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
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
											Session session = HibernateUtil.currentSession();
											dosen.setRuang(null);
											Common.refreshUpdate(session, (dosen));

											loadData(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
		}

	}

	/**
	 * Memuat ulang grid dosen untuk {@link #ruang} yang sedang ditampilkan, disaring berdasarkan
	 * nama (ilike, cocok di mana saja) dan opsional fakultas/jurusan yang dipilih pada combobox
	 * pencarian. Diimplementasikan sebagai kontrak {@link DataLoader#loadData(Object)};
	 * {@code value} tidak dipakai.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = Common.getManualSession();
		List<Dosen> dosen = session.createCriteria(Dosen.class).addOrder(Order.desc("id"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(Restrictions.eq("ruang", ruang))
				.list();

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DetailDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun seluruh tampilan (form filter, toolbar, grid berpaging) ke dalam
	 * {@code component} yang diberikan, untuk menampilkan daftar dosen milik {@code ruang}.
	 * Memanggil {@link #loadData(Object)} di akhir untuk mengisi grid pertama kali.
	 *
	 * @param ruang     ruangan yang daftar dosennya akan ditampilkan/dikelola
	 * @param component kontainer ZK tujuan; isi sebelumnya dibersihkan lewat {@link Common#clear}
	 */
	public void displayDetailDosen(final Ruang ruang, final Component component) {
		this.ruang = ruang;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar dosen yang memiliki Ruang " + ruang.getNama()));

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(groupbox);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Dosen", "/img/new.gif");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyWindow window = new MyWindow();
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				AmbilDataDosenUntukRuangHelper ambilDataDosenUntukRuangHelper = new AmbilDataDosenUntukRuangHelper();
				ambilDataDosenUntukRuangHelper.display(ruang, DetailRuangDosenHelper.this, window);

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIP");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
