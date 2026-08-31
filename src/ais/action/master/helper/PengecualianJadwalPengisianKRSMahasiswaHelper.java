package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PengecualianJadwalPengisianKRSMahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK berbentuk window untuk mengelola daftar
 * {@link PengecualianJadwalPengisianKRSMahasiswa} — pengecualian per mahasiswa terhadap jadwal
 * pengisian KRS (Kartu Rencana Studi) baku, mis. memberi rentang tanggal tambahan bagi mahasiswa
 * tertentu di luar periode KRS reguler.
 *
 * <p>
 * Setiap baris grid dapat diedit langsung di tempat (tahun akademik, jenis semester, tanggal
 * mulai/sampai — masing-masing auto-save saat berubah) serta dihapus per baris. Data baru
 * ditambahkan secara massal lewat {@link AmbilDataMahasiswaBanyak} (tombol "Ambil Data
 * Mahasiswa"): setiap mahasiswa terpilih diberi satu baris pengecualian baru dengan tahun akademik
 * dan jenis semester berjalan, serta tanggal mulai/sampai default hari ini. Menyediakan juga
 * cetak/unggah data massal lewat {@link Common#cetakData} dan {@link Common#uploadData}.
 * Mengimplementasikan {@link DataCriteria}/{@link DataSearchDefault} agar kriteria pencarian dapat
 * dipakai ulang mekanisme cetak/unggah umum.
 * </p>
 */
public class PengecualianJadwalPengisianKRSMahasiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Textbox nama;

	/** Menyiapkan combobox filter fakultas/jurusan (diisi opsi "Semua" + seluruh data aktif). */
	public PengecualianJadwalPengisianKRSMahasiswaHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/**
	 * Perender baris grid untuk satu {@link PengecualianJadwalPengisianKRSMahasiswa}: identitas
	 * mahasiswa (NIM, nama, jurusan, fakultas — tampilan saja) serta field yang bisa diedit
	 * langsung (tahun akademik, jenis semester, tanggal mulai, tanggal sampai — masing-masing
	 * menyimpan perubahannya begitu berubah) dan tombol hapus baris.
	 */
	class PengecualianJadwalPengisianKRSMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public PengecualianJadwalPengisianKRSMahasiswaRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengecualianJadwalPengisianKRSMahasiswa pengecualianJadwalPengisianKRSMahasiswa = (PengecualianJadwalPengisianKRSMahasiswa) arg1;

			Mahasiswa mahasiswa = pengecualianJadwalPengisianKRSMahasiswa.getMahasiswa();
			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);

			final Combobox tahunAkademik = Common.generateTahunAjaran(null);
			Common.selectComboItem(tahunAkademik, pengecualianJadwalPengisianKRSMahasiswa.getTahunAkademik());
			tahunAkademik.setParent(arg0);
			tahunAkademik.setWidth("90%");
			tahunAkademik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String tahun = (String) (tahunAkademik.getSelectedItem() == null
							|| tahunAkademik.getSelectedItem().getValue() == null ? ""
									: tahunAkademik.getSelectedItem().getValue());

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPengisianKRSMahasiswa);
					pengecualianJadwalPengisianKRSMahasiswa.setTahunAkademik(tahun);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPengisianKRSMahasiswa);
				}
			});

			// new
			// Label(pengecualianJadwalPengisianKRSMahasiswa.getTahunAkademik())
			// .setParent(arg0);

			final Combobox semester = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			semester.appendChild(comboitem);
			comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			semester.appendChild(comboitem);

			semester.setWidth("90%");
			Common.selectComboItem(semester, pengecualianJadwalPengisianKRSMahasiswa.getJenisSemester());
			semester.setParent(arg0);
			semester.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String mysemester = (String) (semester.getSelectedItem() == null ? ""
							: semester.getSelectedItem().getValue());

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPengisianKRSMahasiswa);
					pengecualianJadwalPengisianKRSMahasiswa.setJenisSemester(mysemester);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPengisianKRSMahasiswa);
				}
			});

			// new
			// Label(pengecualianJadwalPengisianKRSMahasiswa.getJenisSemester())
			// .setParent(arg0);

			final MyDatebox mulai = new MyDatebox(pengecualianJadwalPengisianKRSMahasiswa.getTanggalMulai());
			mulai.setWidth("90%");
			mulai.setParent(arg0);
			mulai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Date mymulai = mulai.getValue();

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPengisianKRSMahasiswa);
					pengecualianJadwalPengisianKRSMahasiswa.setTanggalMulai(mymulai);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPengisianKRSMahasiswa);
				}
			});

			// new Label(
			// pengecualianJadwalPengisianKRSMahasiswa.getTanggalMulai() == null
			// ? ""
			// : Common.dateFormat2
			// .get().format(pengecualianJadwalPengisianKRSMahasiswa
			// .getTanggalMulai()))
			// .setParent(arg0);

			final MyDatebox sampai = new MyDatebox(pengecualianJadwalPengisianKRSMahasiswa.getTanggalSampai());
			sampai.setWidth("90%");
			sampai.setParent(arg0);
			sampai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Date mysampai = sampai.getValue();

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPengisianKRSMahasiswa);
					pengecualianJadwalPengisianKRSMahasiswa.setTanggalSampai(mysampai);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPengisianKRSMahasiswa);
				}
			});

			// new Label(
			// pengecualianJadwalPengisianKRSMahasiswa.getTanggalSampai() ==
			// null ? ""
			// : Common.dateFormat2
			// .get().format(pengecualianJadwalPengisianKRSMahasiswa
			// .getTanggalSampai()))
			// .setParent(arg0);

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

											Common.refreshDelete(session, pengecualianJadwalPengisianKRSMahasiswa);

											loadData(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data pengecualian jadwal pengisian KRS mahasiswa",
													e,
													new String[] {
															"Periksa apakah data ini masih berelasi dengan data lain (misalnya data KRS atau penjadwalan) sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
										}

									}

								}
							});

				}

			});
		}
	}

	/**
	 * Memuat ulang grid dengan hasil {@link #initCriteria(boolean)}, dibatasi
	 * {@link Common#MAX_RESULT_50} baris.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<PengecualianJadwalPengisianKRSMahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.MAX_RESULT_50)
				.list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new PengecualianJadwalPengisianKRSMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun dan menampilkan window "Daftar pengecualian jadwal KRS mahasiswa": form filter
	 * (mahasiswa, fakultas, prodi), toolbar (ambil data mahasiswa massal, cetak, unggah, cari), dan
	 * grid berpaging yang bisa diedit langsung per baris.
	 *
	 * @throws InterruptedException tidak pernah dilempar secara eksplisit di implementasi ini
	 */
	public void display() throws InterruptedException {

		final MyWindow window = new MyWindow("Daftar pengecualian jadwal KRS mahasiswa", "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
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
		toolbar.setParent(div);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/new.gif");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataMahasiswaBanyak window = new AmbilDataMahasiswaBanyak(new ArrayList<Mahasiswa>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("700px");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
						if (mahasiswas != null) {

							String current = Common.getCurrentTahunAkademik();

							Session session = HibernateUtil.currentSession();

							for (Mahasiswa mahasiswa : mahasiswas) {
								PengecualianJadwalPengisianKRSMahasiswa pengecualianJadwalPengisianKRSMahasiswa = new PengecualianJadwalPengisianKRSMahasiswa();
								pengecualianJadwalPengisianKRSMahasiswa.setMahasiswa(mahasiswa);
								pengecualianJadwalPengisianKRSMahasiswa.setJenisSemester(
										Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
								pengecualianJadwalPengisianKRSMahasiswa.setKeterangan("");
								pengecualianJadwalPengisianKRSMahasiswa.setTahunAkademik(current);
								pengecualianJadwalPengisianKRSMahasiswa
										.setTanggalMulai(ais.ui.util.WaktuUtil.getDate());
								pengecualianJadwalPengisianKRSMahasiswa
										.setTanggalSampai(ais.ui.util.WaktuUtil.getDate());

								session.save(pengecualianJadwalPengisianKRSMahasiswa);
							}

							loadData(null);

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		String[] contents = new String[] { "id", "mahasiswa", "tahunAkademik", "jenisSemester", "tanggalMulai",
				"tanggalSampai", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PengecualianJadwalPengisianKRSMahasiswa.class, this,
				contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengecualianJadwalPengisianKRSMahasiswa.class, contents);
		toolbar.appendChild(upload);

		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("6%");

		loadData(null);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Membangun kriteria Hibernate untuk {@link PengecualianJadwalPengisianKRSMahasiswa} difilter
	 * berdasarkan NIM/nama mahasiswa (ILIKE anywhere pada kolom {@code nama} textbox), jurusan, dan
	 * fakultas.
	 *
	 * @param order bila {@code true}, hasil diurutkan berdasarkan id menurun (data terbaru dulu)
	 * @return criteria siap dieksekusi
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		// TODO Auto-generated method stub
		return HibernateUtil.currentSession().createCriteria(PengecualianJadwalPengisianKRSMahasiswa.class)
				.addOrder(Order.desc("id")).createCriteria("mahasiswa")
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan")
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
	}

	/** Delegasi ke {@link #loadData(Object)}; implementasi {@link DataSearchDefault}. */
	@Override
	public void onSearchDefault(Event event) {
		loadData(event);
	}

}
