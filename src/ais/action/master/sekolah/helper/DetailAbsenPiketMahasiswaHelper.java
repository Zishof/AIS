package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AbsenPiketMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Statusabsensi;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.AbsenPiketPeserta;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailAbsenPiketMahasiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;

	private Textbox nama;
	private Intbox angkatan;
	// private boolean create;

	private Paging paging;
	private AbsenPiketMahasiswa absenPiketMahasiswa;

	private List<AbsenPiketPeserta> siswa = null;

	public DetailAbsenPiketMahasiswaHelper() {

		paging = new Paging();
		Common.initPaging100(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final AbsenPiketPeserta absenPiketPeserta = (AbsenPiketPeserta) data;
			final Mahasiswa mahasiswa = absenPiketPeserta.getMahasiswa();

			final AbsenPiketDetail absenPiketMahasiswaDetail = AbsenPiketDetail.ambil(mahasiswa, null,
					absenPiketMahasiswa, "");

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(row);
			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(row);

			new Label(mahasiswa.getNama()).setParent(row);

			String ket = absenPiketMahasiswaDetail
					.retreiveAbsensiKeterangan(mahasiswa.getId() + "_" + absenPiketMahasiswa.getId());
			ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

			Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					absenPiketMahasiswaDetail.retreiveAbsensiId(mahasiswa.getId() + "_" + absenPiketMahasiswa.getId()));
			if (statusabsensi == null) {
				statusabsensi = Common.bolehKonfigurasi("absen_piket_otomatis_belum") ? ConstantValues.BELUM_ABSEN : ConstantValues.MASUK;
				absenPiketMahasiswaDetail.populate(mahasiswa.getId() + "_" + absenPiketMahasiswa.getId(), statusabsensi,
						ket, "", "", "AbsenPiketMahasiswa");
				Common.refreshUpdate(absenPiketMahasiswaDetail);
			}
			final Textbox keterangan = new Textbox(ket);
			final Radiogroup kehadiran = new Radiogroup();

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (kehadiran.getSelectedItem() == null) {
						return;
					}

					if (absenPiketMahasiswaDetail.getId() != null) {
						HibernateUtil.currentSession().refresh(absenPiketMahasiswaDetail);
					}

					Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getAttribute("value");

					absenPiketMahasiswaDetail.populate(mahasiswa.getId() + "_" + absenPiketMahasiswa.getId(),
							statusabsensi, keterangan.getValue(), "", "", "AbsenPiketMahasiswa");

					Common.refreshUpdate(absenPiketMahasiswaDetail);
				}
			};

			Vbox vbox = new Vbox();
			vbox.setWidth("95%");
			vbox.setParent(row);

			if (ConstantValues.listAbsenMahasiswa.isEmpty()) {
				ConstantValues.initKehadiran(HibernateUtil.currentSession());
			}

			Common.insertRadioItemsMyConfig(kehadiran, "nama", ConstantValues.listAbsenMahasiswa);
			Common.selectRadioItem(kehadiran, statusabsensi);

			kehadiran.addEventListener("onClick", eventListener);
			kehadiran.setParent(vbox);

			keterangan.setCols(50);
			keterangan.setRows(2);
			keterangan.setParent(vbox);
			keterangan.addEventListener("onChange", eventListener);
		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AbsenPiketPeserta.class)

				.add(Restrictions.eq("absenPiketMahasiswa", absenPiketMahasiswa))

				.createAlias("mahasiswa", "mahasiswa")

				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(angkatan == null || angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()));

		if (order) {
			criteria.addOrder(Order.asc("mahasiswa.tahunangkatan")).addOrder(Order.asc("mahasiswa.nim"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging100(initCriteria(false), paging);
		siswa = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE_100 * (paging == null ? 0 : paging.getActivePage())),
				AbsenPiketPeserta.class);

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	private String[] contentsMhs = new String[] { "mahasiswa.nim", "mahasiswa.nama", "mahasiswa.tahunangkatan",
			"mahasiswa.jurusan.nama", "siswa.jurusan.fakultas.nama" };

	public void displayDetailPA(final AbsenPiketMahasiswa absenPiketMahasiswa, final Component component,
			final MyWindow window) {
		this.absenPiketMahasiswa = absenPiketMahasiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta : ")));
		toolbar.appendChild(nama = new Textbox());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		angkatan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/user_male_add.png");
		toolbar.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
				for (AbsenPiketPeserta absenPiketPeserta : siswa) {
					mahasiswas.add(absenPiketPeserta.getMahasiswa());
				}

				AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(mahasiswas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
						if (mahasiswas != null && mahasiswas.size() != 0) {

							Session session = HibernateUtil.currentNativeSession();

							for (Mahasiswa mahasiswa : mahasiswas) {

								AbsenPiketPeserta absenPiketPeserta = (AbsenPiketPeserta) session
										.createCriteria(AbsenPiketPeserta.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("absenPiketMahasiswa", absenPiketMahasiswa))
										.setMaxResults(1).uniqueResult();

								if (absenPiketPeserta != null) {
									absenPiketPeserta = new AbsenPiketPeserta();
									absenPiketPeserta.setAbsenPiketMahasiswa(absenPiketMahasiswa);
									absenPiketPeserta.setMahasiswa(mahasiswa);

									session.getTransaction().begin();
									session.save(absenPiketMahasiswa);
									session.getTransaction().commit();
								}

							}

							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
							HibernateUtil.closeSession();
						}

						loadData(null);
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Semua hadir", "/img/svg/check2.svg");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Tutup");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin semua mahasiswa masuk kelas ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											for (AbsenPiketPeserta absenPiketPeserta : siswa) {

												AbsenPiketDetail absenPiketMahasiswaDetail = AbsenPiketDetail.ambil(
														absenPiketPeserta.getMahasiswa(), absenPiketPeserta.getSiswa(),
														absenPiketMahasiswa, "");

												Statusabsensi statusabsensi = ConstantValues.MASUK;
												String ket = absenPiketMahasiswaDetail
														.retreiveAbsensiKeterangan(absenPiketPeserta.getSiswa().getId()
																+ "_" + absenPiketMahasiswa.getId());
												ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
												absenPiketMahasiswaDetail.populate(
														absenPiketPeserta.getSiswa().getId() + "_"
																+ absenPiketMahasiswa.getId(),
														statusabsensi, ket, "", "", "AbsenPiketMahasiswa");

												Session session = HibernateUtil.currentNativeSession();
												session.getTransaction().begin();
												Common.refreshUpdate(session, absenPiketMahasiswaDetail);
												session.getTransaction().commit();
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												HibernateUtil.closeSession();

											}

											loadData(null);
										}
									});

								}

							}
						});

			}
		});

		masuk = new MyToolbarbuttonConfig("Reset", "/img/reply.png");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Reset");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin me-reset absen di kelas ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											for (AbsenPiketPeserta absenPiketPeserta : siswa) {

												AbsenPiketDetail absenPiketMahasiswaDetail = AbsenPiketDetail.ambil(
														absenPiketPeserta.getMahasiswa(), absenPiketPeserta.getSiswa(),
														absenPiketMahasiswa, "");

												Statusabsensi statusabsensi = ConstantValues.BELUM_ABSEN;
												String ket = absenPiketMahasiswaDetail
														.retreiveAbsensiKeterangan(absenPiketPeserta.getSiswa().getId()
																+ "_" + absenPiketMahasiswa.getId());
												ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
												absenPiketMahasiswaDetail.populate(
														absenPiketPeserta.getSiswa().getId() + "_"
																+ absenPiketMahasiswa.getId(),
														statusabsensi, ket, "", "", "AbsenPiketMahasiswa");

												Session session = HibernateUtil.currentNativeSession();
												session.getTransaction().begin();
												Common.refreshUpdate(session, absenPiketMahasiswaDetail);
												session.getTransaction().commit();
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												HibernateUtil.closeSession();
											}

											loadData(null);

										}
									});

								}

							}
						});

			}
		});

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Status");
		columnHeadersAdding.add("Keterangan");
		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				AbsenPiketPeserta absenPiketPeserta = (AbsenPiketPeserta) objects[0];
				XSSFRow row = (XSSFRow) objects[2];

				AbsenPiketDetail absenPiketMahasiswaDetail = AbsenPiketDetail.ambil(absenPiketPeserta.getMahasiswa(),
						absenPiketPeserta.getSiswa(), absenPiketMahasiswa, "");

				Siswa siswa = absenPiketPeserta.getSiswa();

				String ket = absenPiketMahasiswaDetail
						.retreiveAbsensiKeterangan(siswa.getId() + "_" + absenPiketMahasiswa.getId());
				ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

				Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
						absenPiketMahasiswaDetail.retreiveAbsensiId(siswa.getId() + "_" + absenPiketMahasiswa.getId()));
				if (statusabsensi == null) {
					statusabsensi = ConstantValues.BELUM_ABSEN;
				}

				row.createCell(contentsMhs.length).setCellValue(statusabsensi.getNama());
				row.createCell(contentsMhs.length + 1).setCellValue(ket);
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(AbsenPiketPeserta.class, this,
				"Download Kehadiran", "/img/print.png", columnHeadersAdding, dataAdding, true, null, "Kehadiran",
				contentsMhs);
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status Kehadiran");

		loadData(null);

	}

}
