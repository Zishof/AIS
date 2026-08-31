package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.BukuBahanAjarAction;
import ais.action.master.helper.generic.AmbilDataBukuBahanAjarBanyak;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.DataPunyaBukuBahanAjar;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK generik untuk menampilkan dan mengelola daftar buku referensi ({@link BukuBahanAjar}
 * lewat baris relasi {@link DataPunyaBukuBahanAjar}) yang terkait pada salah satu dari lima jenis
 * entitas induk: {@link Skripsi}, {@link MahasiswaRequestTugasAkhir}, {@link JadwalUjianPMB},
 * {@link KelompokKkn}, atau {@link KelompokPkl} — pemanggil hanya mengisi salah satu parameter
 * entitas induk pada {@link #display}, sisanya {@code null}, dan seluruh query/penyimpanan kelas
 * ini otomatis menyaring/menyertakan berdasarkan kombinasi tersebut.
 *
 * <p>
 * Tombol "Ambil Buku Bahan Ajar" membuka {@link AmbilDataBukuBahanAjarBanyak} (pemilih multi-buku,
 * dengan buku yang sudah terkait dikecualikan dari daftar pilihan) dan menyimpan setiap buku
 * terpilih sebagai baris {@link DataPunyaBukuBahanAjar} baru yang terikat ke entitas induk yang
 * relevan. Setiap baris dapat dibuka ({@link MyDetail}) untuk mengunggah/mengunduh berkas lampiran
 * buku dan cover lewat {@link ais.database.model.file.LampiranLain}, menampilkan kutipan
 * ({@code BukuBahanAjarAction#tampilkanKutipan}), atau dihapus.
 * </p>
 */
public class DataPunyaBukuAjarHelper implements DataLoader {

	private MyGrid grid;
	private Skripsi skripsi;

	private org.zkoss.zk.ui.Component component;

	private Paging paging;

	private Tbmuser tbmuser;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private JadwalUjianPMB jadwalUjianPMB;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;

	public DataPunyaBukuAjarHelper() {

		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DataPunyaBukuAjarHelper}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DataPunyaBukuAjarHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DataPunyaBukuAjarHelper
	 */
	class DetailSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final DataPunyaBukuBahanAjar dataPunyaBukuBahanAjar = (DataPunyaBukuBahanAjar) data;
			final BukuBahanAjar bukuBahanAjar = dataPunyaBukuBahanAjar.getBukuBahanAjar();

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						FileBukuAjarHelper fileBukuAjarHelper = new FileBukuAjarHelper(true);

						fileBukuAjarHelper.display(bukuBahanAjar, detail);
					}

				}
			};

			detail.addEventListener("onOpen", eventListener);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			RevisiHelper.createNewRevisi(BukuBahanAjar.class, bukuBahanAjar, bukuBahanAjar.getNama()).setParent(vbox);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.BUKU, LampiranLain.BUKU, true,
					null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(vbox);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.COVER_BUKU, "Cover", true,
					null, null, false, false, false, false);

			if (bukuBahanAjar.getPengarangAdalahDosen()) {
				BukuBahanAjarAction.tampilkanInfoDosen(bukuBahanAjar, false).setParent(arg0);
			} else {
				new Label((bukuBahanAjar.getPengarang1() != null && !bukuBahanAjar.getPengarang1().trim().equals("")
						? bukuBahanAjar.getPengarang1().trim() + ", " : "")
						+ (bukuBahanAjar.getPengarang2() != null && !bukuBahanAjar.getPengarang2().trim().equals("")
								? bukuBahanAjar.getPengarang2().trim() + ", " : "")
						+ (bukuBahanAjar.getPengarang3() != null && !bukuBahanAjar.getPengarang3().trim().equals("")
								? bukuBahanAjar.getPengarang3().trim() + ", " : "")).setParent(arg0);
			}

			new Label(bukuBahanAjar.getIsbn()).setParent(arg0);
			new Label(bukuBahanAjar.getPenerbit()).setParent(arg0);
			new Label(bukuBahanAjar.getLink()).setParent(arg0);
			new Label(bukuBahanAjar.getTahun() + " / " + bukuBahanAjar.getTahunAkademik() + " / "
					+ bukuBahanAjar.getSemester()).setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kutipan", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BukuBahanAjarAction.tampilkanKutipan(bukuBahanAjar);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(tbmuser != null);
			button.addEventListener("onClick", new EventListener() {
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
									Common.refreshDelete(dataPunyaBukuBahanAjar);
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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	/**
	 * Membangun kriteria Hibernate untuk {@link DataPunyaBukuBahanAjar}: kondisi OR atas kesamaan
	 * kelima kolom entitas induk ({@code kelompokPkl}, {@code kelompokKkn},
	 * {@code mahasiswaRequestTugasAkhir}, {@code skripsi}, {@code jadwalUjianPMB}) terhadap field
	 * instance kelas ini yang sesuai. Catatan: {@code Restrictions.eq} terhadap parameter bernilai
	 * {@code null} diterjemahkan Hibernate menjadi kondisi "kolom IS NULL", bukan "diabaikan" — jadi
	 * kebenaran hasil bergantung pada kontrak pemanggil {@link #display} yang hanya mengisi tepat
	 * satu dari kelima parameter entitas induk pada satu waktu.
	 *
	 * @param order {@code true} untuk mengurutkan hasil berdasarkan id ascending
	 * @return kriteria Hibernate siap eksekusi/paginasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(DataPunyaBukuBahanAjar.class)
				.add(Restrictions.or(Restrictions.eq("kelompokPkl", kelompokPkl),
						Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
								Restrictions.or(
										Restrictions.or(
												Restrictions.eq("mahasiswaRequestTugasAkhir",
														mahasiswaRequestTugasAkhir),
										Restrictions.eq("skripsi", skripsi)),
								Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB)))));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	/**
	 * Implementasi {@link DataLoader}: memuat ulang halaman aktif daftar buku referensi ke
	 * {@link #grid}, dan bila komponen induk adalah {@link Tabpanel}, memperbarui label tab terkait
	 * dengan jumlah buku yang ditemukan.
	 *
	 * @param value tidak digunakan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);

		List<DataPunyaBukuBahanAjar> dataPunyaBukuBahanAjar = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab().setLabel("Buku Bahan Ajar "
					+ (dataPunyaBukuBahanAjar.size() == 0 ? "" : "(" + dataPunyaBukuBahanAjar.size() + ")"));
		}

		ListModel strset = new SimpleListModel(dataPunyaBukuBahanAjar);
		grid.setRowRenderer(new DetailSkripsiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun tampilan daftar buku referensi. Pemanggil mengisi tepat satu dari kelima parameter
	 * entitas induk (sisanya {@code null}) sesuai konteks pemakaian — lihat dokumentasi kelas dan
	 * catatan pada {@link #initCriteria(boolean)}.
	 *
	 * @param skripsi                     entitas induk Skripsi/TA, atau {@code null}
	 * @param mahasiswaRequestTugasAkhir  entitas induk bimbingan tugas akhir, atau {@code null}
	 * @param jadwalUjianPMB              entitas induk jadwal ujian PMB, atau {@code null}
	 * @param kelompokKkn                 entitas induk kelompok KKN, atau {@code null}
	 * @param kelompokPkl                 entitas induk kelompok PKL, atau {@code null}
	 * @param component                   komponen ZK induk tempat layar dirender (dibersihkan lebih dulu)
	 */
	public void display(final Skripsi skripsi, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final JadwalUjianPMB jadwalUjianPMB, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final org.zkoss.zk.ui.Component component) {
		this.skripsi = skripsi;
		this.kelompokKkn = kelompokKkn;
		this.kelompokPkl = kelompokPkl;
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		this.jadwalUjianPMB = jadwalUjianPMB;
		Common.clear(component);
		this.component = component;

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar Buku Referensi"));

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(tbmuser != null);
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Buku Bahan Ajar", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<BukuBahanAjar> bukuBahanAjars = HibernateUtil.currentSession()
						.createCriteria(DataPunyaBukuBahanAjar.class)
						.add(Restrictions.or(Restrictions.eq("kelompokPkl", kelompokPkl),
								Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
										Restrictions.or(
												Restrictions.or(
														Restrictions.eq("mahasiswaRequestTugasAkhir",
																mahasiswaRequestTugasAkhir),
												Restrictions.eq("skripsi", skripsi)),
										Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB)))))
						.setProjection(Projections.property("bukuBahanAjar")).list();
				AmbilDataBukuBahanAjarBanyak ambilDataBukuBahanAjarBanyak = new AmbilDataBukuBahanAjarBanyak(
						bukuBahanAjars);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataBukuBahanAjarBanyak);
				ambilDataBukuBahanAjarBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BukuBahanAjar> bukuBahanAjars = (List<BukuBahanAjar>) arg0.getData();
						for (BukuBahanAjar bukuBahanAjar : bukuBahanAjars) {
							DataPunyaBukuBahanAjar dataPunyaBukuBahanAjar = new DataPunyaBukuBahanAjar();
							dataPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
							dataPunyaBukuBahanAjar.setKeterangan("");
							dataPunyaBukuBahanAjar.setJadwalUjianPMB(jadwalUjianPMB);
							dataPunyaBukuBahanAjar.setSkripsi(skripsi);
							dataPunyaBukuBahanAjar.setKelompokKkn(kelompokKkn);
							dataPunyaBukuBahanAjar.setKelompokPkl(kelompokPkl);
							dataPunyaBukuBahanAjar.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
							Common.refreshSaveOrUpdate(dataPunyaBukuBahanAjar);
						}

						loadData(null);
					}
				});
				ambilDataBukuBahanAjarBanyak.setWidth("97%");
				ambilDataBukuBahanAjarBanyak.setHeight("97%");
				ambilDataBukuBahanAjarBanyak.setVisible(true);
				ambilDataBukuBahanAjarBanyak.onModal();

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
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Buku");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ISBN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penerbit");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Link");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		loadData(null);

		paging.setParent(groupbox);
	}

}
