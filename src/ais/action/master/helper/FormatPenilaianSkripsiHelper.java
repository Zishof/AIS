package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK berbentuk window modal untuk memilih {@link FormatNilaiSkripsi} (skema bobot
 * persentase komponen nilai skripsi: ketua sidang, pembimbing, penguji 1-5) yang akan dipakai
 * menghitung nilai akhir satu {@link Skripsi}, lalu menyimpan hasil perhitungan.
 *
 * <p>
 * Alur pemakaian: {@link #display} membuka window berisi grid pilihan format nilai (radio button
 * per baris) yang diisi lewat {@link #onSearchDefault}; user memilih satu format lalu menekan
 * "Simpan" yang memanggil {@link #save(Session)}. Perhitungan nilai akhir mengalikan tiap komponen
 * nilai pada {@code skripsi} (mis. {@code getNilaiPembimbing()}) dengan persentase bobot pada
 * format terpilih, menjumlahkannya menjadi total nilai, lalu menentukan nilai huruf lewat
 * {@link Common#getNilaiHuruf} berdasarkan tahun akademik/semester dari
 * {@link Skripsi#getDetailperkuliahan()} bila ada (fallback ke tahun akademik/semester berjalan
 * bila skripsi belum tertaut ke {@link Detailperkuliahan}).
 * </p>
 */
public class FormatPenilaianSkripsiHelper {

	// private Perkuliahan perkuliahan;
	private MyGrid grid;

	private FormatNilaiSkripsi selectedFormatNilaiSkripsi;

	private LoadSkripsiInterface loadSkripsiInterface;

	private Skripsi skripsi;

	/**
	 * Perender baris grid untuk satu {@link FormatNilaiSkripsi}: radio button pemilih (dicentang
	 * otomatis bila sama dengan format yang sedang dipakai {@link #skripsi}) dan label persentase
	 * bobot tiap komponen penilai (ketua sidang, pembimbing, penguji 1-5).
	 */
	class FormatNilaiSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		public FormatNilaiSkripsiRenderer() {

		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatNilaiSkripsi formatNilaiSkripsi = (FormatNilaiSkripsi) arg1;

			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(formatNilaiSkripsi.getId() + "");

			checkbox.setChecked(formatNilaiSkripsi.getId()
					.equals(skripsi.getFormatNilaiSkripsi() == null ? "" : skripsi.getFormatNilaiSkripsi().getId()));

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					selectedFormatNilaiSkripsi = formatNilaiSkripsi;
				}
			});

			new Label(formatNilaiSkripsi.getProsentasiNilaiKetuaSidang() + " %").setParent(arg0);
			new Label(formatNilaiSkripsi.getProsentasiNilaiPembimbing() + " %").setParent(arg0);
			new Label(formatNilaiSkripsi.getProsentasiNilaiPenguji1() + " %").setParent(arg0);
			new Label(formatNilaiSkripsi.getProsentasiNilaiPenguji2() + " %").setParent(arg0);
			new Label(formatNilaiSkripsi.getProsentasiNilaiPenguji3() + " %").setParent(arg0);
			new Label(formatNilaiSkripsi.getProsentasiNilaiPenguji4() + " %").setParent(arg0);
			new Label(formatNilaiSkripsi.getProsentasiNilaiPenguji5() + " %").setParent(arg0);
		}

	}

	/**
	 * Menghitung nilai akhir {@link #skripsi} berdasarkan {@link #selectedFormatNilaiSkripsi}
	 * (jumlah tertimbang dari nilai ketua sidang, pembimbing, dan penguji 1-5), menentukan nilai
	 * huruf serta status lulus lewat {@link Common#getNilaiHuruf}, menyimpan perubahan, lalu
	 * memicu {@link LoadSkripsiInterface#refresh()} agar tampilan pemanggil ikut diperbarui.
	 * Kegagalan hanya ditampilkan lewat {@link Common#tampilErrorJikaAdmin} tanpa membatalkan
	 * proses (method selalu mengembalikan {@code true}).
	 *
	 * @param session sesi Hibernate aktif tempat perubahan {@code skripsi} disimpan
	 * @return selalu {@code true}
	 * @throws InterruptedException tidak pernah dilempar secara eksplisit di implementasi ini
	 *                               (dideklarasikan pada signature method)
	 */
	public boolean save(Session session) throws InterruptedException {

		try {

			Double nilaiKetuaSidang = (skripsi.getNilaiKetuaSidang()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiKetuaSidang()) / 100;
			Double nilaiPembimbing = (skripsi.getNilaiPembimbing()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiPembimbing()) / 100;
			Double nilaiPenguji1 = (skripsi.getNilaiPenguji1()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiPenguji1()) / 100;
			Double nilaiPenguji2 = (skripsi.getNilaiPenguji2()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiPenguji2()) / 100;
			Double nilaiPenguji3 = (skripsi.getNilaiPenguji3()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiPenguji3()) / 100;
			Double nilaiPenguji4 = (skripsi.getNilaiPenguji4()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiPenguji4()) / 100;
			Double nilaiPenguji5 = (skripsi.getNilaiPenguji5()
					* selectedFormatNilaiSkripsi.getProsentasiNilaiPenguji5()) / 100;
			Double totalNilai = nilaiKetuaSidang + nilaiPembimbing + nilaiPenguji1 + nilaiPenguji2 + nilaiPenguji3
					+ nilaiPenguji4 + nilaiPenguji5;

			skripsi.setFormatNilaiSkripsi(selectedFormatNilaiSkripsi);

			skripsi.setTotalNilai(nilaiKetuaSidang + nilaiPembimbing + nilaiPenguji1 + nilaiPenguji2 + nilaiPenguji3
					+ nilaiPenguji4 + nilaiPenguji5);

			Detailperkuliahan detailperkuliahan = skripsi.getDetailperkuliahan();
			Matakuliah matakuliah = detailperkuliahan == null ? null
					: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
							: detailperkuliahan.getMatakuliahKonversi();

			NilaiHuruf nilaiHuruf = skripsi.getDetailperkuliahan() == null
					? Common.getNilaiHuruf(totalNilai, skripsi.getMahasiswa().getTahunangkatan(),
							skripsi.getMahasiswa().getJurusan(), skripsi.getMahasiswa().getJurusan().getFakultas(),
							Common.getCurrentTahunAkademik(),
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf())
					: Common.getNilaiHuruf(totalNilai, skripsi.getMahasiswa().getTahunangkatan(),
							skripsi.getMahasiswa().getJurusan(), skripsi.getMahasiswa().getJurusan().getFakultas(),
							skripsi.getDetailperkuliahan().getTahunAkademik(),
							skripsi.getDetailperkuliahan().getSemester() % 2 == 0 ? Perkuliahan.GENAP
									: Perkuliahan.GANJIL,
							matakuliah == null ? "" : matakuliah.getKode(),
							matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

			skripsi.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
			skripsi.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

			Common.refreshUpdate(session, (skripsi));

			this.loadSkripsiInterface.refresh();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 * Membuka window modal berisi grid pilihan {@link FormatNilaiSkripsi} untuk {@code skripsi}
	 * yang diberikan, dengan format yang sedang dipakai skripsi tersebut pra-terpilih. Tombol
	 * "Simpan" memanggil {@link #save(Session)}; tombol "Batal" menutup window tanpa perubahan.
	 *
	 * @param skripsi               data skripsi yang formatnya akan diubah
	 * @param window                window modal yang dipakai untuk menampilkan UI
	 * @param loadSkripsiInterface  callback refresh yang dipanggil setelah penyimpanan berhasil
	 */
	public void display(final Skripsi skripsi, final MyWindow window, final LoadSkripsiInterface loadSkripsiInterface) {
		this.loadSkripsiInterface = loadSkripsiInterface;
		this.skripsi = skripsi;
		this.selectedFormatNilaiSkripsi = skripsi.getFormatNilaiSkripsi();
		Common.clear(window);
		window.setTitle("Daftar Format Penilaian " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai());
		window.setWidth("600px");
		window.setHeight("400px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(window);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();
				if (save(session)) {
					window.setVisible(false);
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
			}
		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembimbing 1");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembimbing 2");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penguji 1");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penguji 2");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penguji 3");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penguji 4");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penguji 5");
		column.setWidth("25%");

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
	 * Memuat seluruh {@link FormatNilaiSkripsi} (diurutkan berdasarkan id) ke dalam grid pemilihan.
	 *
	 * @param event tidak dipakai
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<FormatNilaiSkripsi> formatNilai = session.createCriteria(FormatNilaiSkripsi.class)
				.addOrder(Order.asc("id")).list();
		ListModel strset = new SimpleListModel(formatNilai);
		grid.setRowRenderer(new FormatNilaiSkripsiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
