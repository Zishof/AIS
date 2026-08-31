package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Composer ZK untuk layar "Ikut Perkuliahan" — pendaftaran mahasiswa mengikuti perkuliahan sebagai
 * peserta tambahan (di luar KRS resmi) melalui field {@link Detailperkuliahan#getIkutiPerkuliahan()}.
 * Berbeda dari {@link KrsNonPaketHelper}, kehadiran/keikutsertaan lewat jalur ini TIDAK memengaruhi
 * nilai — hanya memberi akses aktivitas dan kegiatan perkuliahan (lihat catatan pada tampilan).
 * Menampilkan grid matakuliah yang diikuti (kode/nama/SKS dengan info konversi ekivalensi bila
 * berbeda, dosen, hari, kelas, waktu, ruang) beserta panel informasi jam bentrok, tombol "Ikuti
 * Perkuliahan" untuk menambah, dan tombol hapus per baris.
 */
public class IkutPerkuliahanHelper implements DataLoader {

	private MyGrid grid;
	// private MyGrid gridKomentar;
	private MyDiv jamBentrok = new MyDiv();
	private Mahasiswa mahasiswa;
	private Integer semester;
	// private Dosen dosenPembimbingAkademik;

	private List<Long> detailperkuliahans;

	protected AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper;

	private Integer semesterPendek;
	private EventListener eventListener;

	private Integer tahapan;

	/** @param semesterPendek status semester pendek (SP) yang dilayani helper ini, atau {@code null} untuk semester reguler */
	public IkutPerkuliahanHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
		aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(Common.getCurrentUser().getMahasiswa(), null, true);
	}

	/** Row renderer grid perkuliahan yang diikuti: kode/nama/SKS (dengan info konversi ekivalensi bila berbeda), dosen, hari, semester, kelas, waktu, ruang, dan tombol hapus. */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());

			if (detailperkuliahan == null || detailperkuliahan.getIkutiPerkuliahan() == null) {
				row.setVisible(false);
				return;
			}

			Matakuliah matakuliah = detailperkuliahan.getIkutiPerkuliahan().getMatakuliah();

			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), false);
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];

			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
					: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
					: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")).setParent(row);

			new Label(detailperkuliahan.getIkutiPerkuliahan() == null ? ""
					: detailperkuliahan.getIkutiPerkuliahan().getDosen1() == null ? ""
							: detailperkuliahan.getIkutiPerkuliahan().getDosen1().getNama()).setParent(row);
			new Label(detailperkuliahan.getIkutiPerkuliahan() == null ? ""
					: (detailperkuliahan.getIkutiPerkuliahan().getHari() == null ? ""
							: detailperkuliahan.getIkutiPerkuliahan().getHari())).setParent(row);

			new Label(detailperkuliahan.getSemester() == null ? "" : detailperkuliahan.getSemester() + "")
					.setParent(row);

			new Label(detailperkuliahan.getIkutiPerkuliahan() == null ? ""
					: detailperkuliahan.getIkutiPerkuliahan().getKelas()).setParent(row);
			new Label(
					(detailperkuliahan.getIkutiPerkuliahan() == null ? ""
							: (detailperkuliahan.getIkutiPerkuliahan().getWaktuMulai() == null ? ""
									: detailperkuliahan.getIkutiPerkuliahan().getWaktuMulai()))
							+ " s.d "
							+ (detailperkuliahan.getIkutiPerkuliahan() == null ? ""
									: (detailperkuliahan.getIkutiPerkuliahan().getWaktuSelesai() == null ? ""
											: detailperkuliahan.getIkutiPerkuliahan().getWaktuSelesai())))
													.setParent(row);
			new Label(
					detailperkuliahan.getIkutiPerkuliahan() == null ? ""
							: detailperkuliahan.getIkutiPerkuliahan().getRuang() == null ? ""
									: detailperkuliahan.getIkutiPerkuliahan().getRuang().getKodeRuangan())
											.setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
												if (detailperkuliahan.getPersetujuan() != null
														&& detailperkuliahan.getPersetujuan()
																.equals(Detailperkuliahan.DISETUJUI)
														&& detailperkuliahan.getTotalNilai() > 1.0) {
													MyMessageboxConfig.show(
															"Jika nilai tidak nol, anda tidak bisa menghapus matakuliah ini",
															"Peringatan", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);
													return;
												}
											}

											List<Komentar> komentars = session.createCriteria(Komentar.class).add(
													Restrictions.eq("detailperkuliahan", detailperkuliahan.getId()))
													.list();

											for (Komentar komentar : komentars) {
												Common.refreshDelete((komentar));
											}

											Common.refreshDelete((detailperkuliahan));
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
			button.setParent(toolbar);
			toolbar.setParent(row);
			// } else {
			// new Label("").setParent(row);
			// }

		}
	}

	/**
	 * Memuat ulang daftar perkuliahan yang diikuti mahasiswa, memperbarui panel informasi jam bentrok
	 * (turut mempertimbangkan KRS resmi mahasiswa, bukan hanya perkuliahan yang diikuti), lalu
	 * memanggil {@code eventListener} yang diberikan ke {@link #display} untuk memberi tahu pemanggil.
	 *
	 * @param value bila berupa {@link Boolean} {@code true}, memaksa refresh cache saat mengambil KRS resmi
	 */
	public void loadData(Object value) {
		detailperkuliahans = Common.getIkutDetailperkuliahans(mahasiswa, semester, tahapan, null, semesterPendek,
				false);

		ListModel strset = new SimpleListModel(detailperkuliahans);
		grid.setRowRenderer(new DetailMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

		Common.clear(jamBentrok);

		List<Long> myDetailperkuliahans = new ArrayList<Long>(detailperkuliahans);
		myDetailperkuliahans.addAll(Common.getDetailperkuliahans(mahasiswa, semester, null, semesterPendek, false,
				false, (value != null && value instanceof Boolean) ? (Boolean) value : false));

		jamBentrok.appendChild(new MyCaptionStyled("Informasi Jam Bentrok"));

		List<Perkuliahan> jadwalPerkuliahanParalels = new ArrayList<Perkuliahan>();
		List<Detailperkuliahan> detailperkuliahansbaru = new ArrayList<Detailperkuliahan>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					detailperkuliahanid.toString());
			if (d != null && d.getPerkuliahan() != null) {
				detailperkuliahansbaru.add(d);
				List<Perkuliahan> jadwalparalels = d.getPerkuliahan().ambilParalelPerkuliahan();
				jadwalPerkuliahanParalels.addAll(jadwalparalels);
			}
		}

		jamBentrok.appendChild(Common.generateInformasiJamBentrok(detailperkuliahansbaru));
		jamBentrok.appendChild(
				Common.generateInformasiJamBentrokParalel(detailperkuliahansbaru, jadwalPerkuliahanParalels));
		jamBentrok.appendChild(Common.generateInformasiJamBentrokParalelParalel(jadwalPerkuliahanParalels));

		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Membangun UI layar "Ikut Perkuliahan" (toolbar Ikuti Perkuliahan/Refresh, grid, panel jam
	 * bentrok, catatan) di dalam {@code component} untuk kombinasi mahasiswa/semester/tahapan yang
	 * diberikan, lalu memuat data awal.
	 *
	 * @param mahasiswa      mahasiswa yang datanya ditampilkan
	 * @param tahunAjaran    diteruskan ke dialog "Ikuti Perkuliahan"
	 * @param semester       nomor semester yang ditampilkan
	 * @param tahapan        tahapan KRS terkait, boleh {@code null}/0
	 * @param component      container ZK yang akan diisi
	 * @param eventListener  callback yang dipanggil setiap kali {@link #loadData} selesai
	 */
	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final Integer tahapan, final Component component, final EventListener eventListener) {
		this.mahasiswa = mahasiswa;
		this.semester = semester;

		this.tahapan = tahapan;
		this.eventListener = eventListener;

		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyButtonConfig buttonPerkuliahan = new MyButtonConfig("Ikuti Perkuliahan", "/img/svg/edit-box-line.svg");

		buttonPerkuliahan.addEventListener("onClick", new EventListener() {

			private AmbilDataIkutPerkuliahanHelper ambilDataIkutPerkuliahanHelper = new AmbilDataIkutPerkuliahanHelper(
					semesterPendek);

			@Override
			public void onEvent(Event event) throws Exception {

				ambilDataIkutPerkuliahanHelper.display(mahasiswa, tahunAjaran, semester, IkutPerkuliahanHelper.this);
			}

		});
		buttonPerkuliahan.setParent(toolbar);

		MyButtonConfig button = new MyButtonConfig("Refresh", "/img/Configure.png");

		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		jamBentrok.setParent(groupbox);

		Label catatan = new Label(
				"Catatan : Anda bisa mengikuti semua perkuliahan, namun perkuliahan yang anda ikuti ini tidak berpengaruh terhadap nilai. Anda bisa mengikuti aktifitas dan kegiatan di semua perkuliahan.");
		catatan.setStyle("font-size:10px;font-weight:bold;color:blue");
		catatan.setParent(groupbox);

		loadData(null);

	}

	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

}
