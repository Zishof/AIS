package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.MahasiswaAction;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Asrama;
import ais.database.model.AsramaPunyaMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper tampilan & pengelolaan daftar mahasiswa penghuni satu {@link Asrama} (asrama
 * mahasiswa). Menyediakan pencarian (NIM, nama, angkatan, fakultas, jurusan) di atas grid
 * paging server-side ({@link Paging} + {@link DataCriteria}), penambahan penghuni lewat
 * pencarian mahasiswa ({@code AmbilDataMahasiswaForAsramaHelper}), penghapusan per baris atau
 * pembersihan massal ("Bersihkan"), riwayat revisi per baris, dan cetak laporan.
 *
 * <p>
 * Fitur khas kelas ini adalah <b>auto-sinkronisasi keanggotaan</b> lewat {@link #syncAsrama}:
 * bila {@code asrama} dikonfigurasi dengan kriteria otomatis (jurusan dan/atau tahun angkatan
 * tertentu), setiap kali data dimuat ({@link #loadData}), sistem mengecek apakah anggota asrama
 * saat ini sudah cocok dengan kriteria tersebut — bila tidak (jumlah baris berbeda), seluruh
 * relasi lama dihapus dan digantikan dengan mahasiswa yang benar-benar memenuhi kriteria
 * jurusan/angkatan itu (replace-all otomatis, bukan penambahan/pengurangan selektif).
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} (untuk callback penyegaran setelah tambah data) dan
 * {@link DataCriteria} (agar kriteria pencarian dapat dipakai bersama oleh mekanisme paging dan
 * cetak laporan {@link Common#cetakData}).
 * </p>
 */
public class AsramaPunyaMahasiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Asrama asrama;
	private Textbox nim;
	private Textbox nama;
	private Intbox angkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	/** Menyiapkan combobox filter fakultas/jurusan dan komponen paging (memuat ulang grid saat halaman berpindah). */
	public AsramaPunyaMahasiswaHelper() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/** Merender satu baris grid: riwayat revisi, identitas mahasiswa (nama, angkatan, status, fakultas, jurusan, program), pencatat perubahan, dan tombol hapus (hanya bila pengguna punya hak {@link CommonPrivilages#DELETE}). */
	class DetailAsramaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		/** Menentukan hak hapus pengguna saat ini. */
		public DetailAsramaRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final AsramaPunyaMahasiswa asramaPunyaMahasiswa = (AsramaPunyaMahasiswa) data;

			RevisiHelper.createNewRevisi(AsramaPunyaMahasiswa.class, asramaPunyaMahasiswa,
					asramaPunyaMahasiswa.getMahasiswa().getNim()).setParent(row);

			new Label(asramaPunyaMahasiswa.getMahasiswa().getNama()).setParent(row);
			new Label(asramaPunyaMahasiswa.getMahasiswa().getTahunangkatan() + "").setParent(row);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(asramaPunyaMahasiswa.getMahasiswa())
					.getStatusMahasiswa();
			new Label(statusMahasiswa.getNama()).setParent(row);

			new Label(asramaPunyaMahasiswa.getMahasiswa().getJurusan() == null ? ""
					: asramaPunyaMahasiswa.getMahasiswa().getJurusan().getFakultas().getNama() + "").setParent(row);

			new Label(asramaPunyaMahasiswa.getMahasiswa().getJurusan() == null ? ""
					: asramaPunyaMahasiswa.getMahasiswa().getJurusan().getNama() + "").setParent(row);

			new Label(asramaPunyaMahasiswa.getMahasiswa().getProgram() + "").setParent(row);

			new Label(asramaPunyaMahasiswa.getTbmuser() == null ? "" : asramaPunyaMahasiswa.getTbmuser().getUserId())
					.setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(asramaPunyaMahasiswa);
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

		}

	}

	/**
	 * Membangun {@link Criteria} pencarian {@link AsramaPunyaMahasiswa} untuk {@link #asrama}
	 * saat ini, disaring berdasarkan jurusan/fakultas terpilih, angkatan, NIM (contains), dan
	 * nama (contains). Implementasi {@link DataCriteria}, dipakai bersama oleh
	 * {@link #loadData}, mekanisme paging server-side, dan cetak laporan.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan berdasarkan NIM menaik
	 * @return criteria siap dieksekusi (belum dipanggil {@code list()})
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AsramaPunyaMahasiswa.class);

		criteria.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("asrama", asrama));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	/**
	 * Menyinkronkan keanggotaan {@code asrama} dengan kriteria otomatisnya (jurusan dan/atau
	 * tahun angkatan yang dikonfigurasi pada entitas {@link Asrama} itu sendiri). Tidak
	 * melakukan apa pun bila {@code asrama} tidak memiliki kriteria jurusan maupun tahun
	 * angkatan. Bila ada kriteria: membandingkan jumlah anggota yang memenuhi kriteria
	 * ({@code countA}) dengan jumlah anggota asrama saat ini ({@code countB}); bila berbeda,
	 * seluruh baris {@link AsramaPunyaMahasiswa} lama untuk asrama ini dihapus lewat SQL native
	 * dan digantikan dengan baris baru untuk setiap mahasiswa yang memenuhi kriteria (dicatat
	 * dengan {@code oleh}/{@code tbmuser} pengguna saat ini dan {@code diubahDari} =
	 * {@code MahasiswaAction}). Ini adalah replace-all otomatis, bukan diff incremental — bila
	 * dipanggil berulang tanpa perubahan data, tidak ada operasi tambahan (karena
	 * {@code countA == countB}).
	 *
	 * @param asrama  asrama yang keanggotaannya disinkronkan
	 * @param session sesi Hibernate yang dipakai untuk seluruh query/operasi
	 * @param commit  bila {@code true}, method membuka & meng-commit transaksinya sendiri;
	 *                bila {@code false}, pemanggil bertanggung jawab atas transaksi
	 * @param tbmuser pengguna yang tercatat sebagai pelaku perubahan pada baris baru
	 */
	@SuppressWarnings("unchecked")
	public static void syncAsrama(Asrama asrama, Session session, boolean commit, Tbmuser tbmuser) {
		if (asrama.getJurusan() != null || asrama.getTahunAngkatan() != null) {

			int countA = ((Number) session.createCriteria(AsramaPunyaMahasiswa.class)
					.add(Restrictions.eq("asrama", asrama)).createAlias("mahasiswa", "mahasiswa")
					.add(asrama.getJurusan() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.jurusan", asrama.getJurusan()))
					.add(asrama.getTahunAngkatan() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("mahasiswa.tahunangkatan", asrama.getTahunAngkatan()))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			int countB = ((Number) session.createCriteria(AsramaPunyaMahasiswa.class)
					.add(Restrictions.eq("asrama", asrama)).setProjection(Projections.rowCount()).uniqueResult())
							.intValue();

			System.out.println("countA=>" + countA + ", countB=>" + countB);
			if (countA != countB) {
				List<Mahasiswa> mahasiswas = session.createCriteria(AsramaPunyaMahasiswa.class)
						.add(Restrictions.eq("asrama", asrama)).createAlias("mahasiswa", "mahasiswa")
						.add(asrama.getJurusan() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.jurusan", asrama.getJurusan()))
						.add(asrama.getTahunAngkatan() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.tahunangkatan", asrama.getTahunAngkatan()))
						.setProjection(Projections.groupProperty("mahasiswa")).list();
				session.createSQLQuery("delete from asrama_punya_mahasiswa where asrama=" + asrama.getId())
						.executeUpdate();

				if (commit) {
					session.getTransaction().begin();
				}
				for (Mahasiswa mahasiswa : mahasiswas) {
					AsramaPunyaMahasiswa asramaPunyaMahasiswa = new AsramaPunyaMahasiswa();
					asramaPunyaMahasiswa.setAsrama(asrama);
					asramaPunyaMahasiswa.setOleh(tbmuser.getUserId());
					asramaPunyaMahasiswa.setTbmuser(tbmuser);
					asramaPunyaMahasiswa.setMahasiswa(mahasiswa);
					asramaPunyaMahasiswa.setDiubahDari(MahasiswaAction.class.getSimpleName());
					session.save(asramaPunyaMahasiswa);
				}
				if (commit) {
					session.getTransaction().commit();
				}
			}

		}
	}

	/**
	 * Menyegarkan grid: dijalankan asinkron lewat {@link Common#createDefaultTimer(EventListener)}
	 * agar UI tidak terkunci. Menjalankan {@link #syncAsrama} lebih dulu (auto-sinkronisasi bila
	 * asrama punya kriteria otomatis), lalu memuat halaman data sesuai {@link #initCriteria} dan
	 * posisi {@link #paging} saat ini.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				Session session = HibernateUtil.currentSession();
				AsramaPunyaMahasiswaHelper.syncAsrama(asrama, session, false, tbmuser);

				Common.initPaging(initCriteria(false), paging);
				List<AsramaPunyaMahasiswa> myAsramaPunyaMahasiswas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myAsramaPunyaMahasiswas);
				grid.setRowRenderer(new DetailAsramaRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	/** @return {@code this} sebagai {@link DataLoader}, diteruskan ke helper pencarian mahasiswa agar dapat memicu {@link #loadData(Object)} setelah data ditambahkan. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun panel lengkap daftar penghuni {@code asrama} ke dalam {@code component}: toolbar
	 * pencarian, tombol Cari/Ambil Mahasiswa/Bersihkan/Cetak, dan grid paging. Bila
	 * {@code asrama} sudah punya kriteria otomatis (jurusan/tahun angkatan), filter jurusan/
	 * fakultas/angkatan pada form dikunci sesuai kriteria tersebut (tidak dapat diubah manual).
	 * Tombol "Bersihkan" menghapus SELURUH baris keanggotaan asrama ini tanpa terkecuali
	 * (berbeda dari hapus per baris).
	 *
	 * @param asrama    asrama yang penghuninya akan ditampilkan/dikelola
	 * @param component kontainer ZK yang akan diisi (isi sebelumnya dibersihkan)
	 * @param window    jendela induk, diteruskan ke helper pencarian mahasiswa
	 */
	public void display(final Asrama asrama, final Component component, final MyWindow window) {
		this.asrama = asrama;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti asrama " + asrama.getNama()));

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

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setCols(4);
		angkatan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (asrama.getJurusan() != null) {
			Common.selectComboItem(searchfakultas, asrama.getJurusan().getFakultas());
			searchfakultas.setDisabled(true);
			Common.insertCombo(searchjurusan, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", asrama.getJurusan().getFakultas()));
			Common.selectComboItem(searchjurusan, asrama.getJurusan());
			searchjurusan.setDisabled(true);
		}

		if (asrama.getTahunAngkatan() != null) {
			angkatan.setValue(asrama.getTahunAngkatan());
			angkatan.setDisabled(true);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaForAsramaHelper dataMahasiswaHelper = new AmbilDataMahasiswaForAsramaHelper(asrama);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from asrama_punya_mahasiswa where asrama = " + asrama.getId())
												.executeUpdate();

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "asrama", "mahasiswa");
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Oleh");
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

}
