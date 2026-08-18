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
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailAbsenPiketHelper implements DataLoader, DataCriteria {

	private MyGrid grid;

	private Textbox nama;
	private Intbox angkatan;
	// private boolean create;

	private Paging paging;
	private AbsenPiket absenPiket;

	private List<KelasSiswaPunyaSiswa> siswa = null;

	public DetailAbsenPiketHelper() {

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
			final KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) data;
			final Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
			final KelasSiswa kelasSiswa = kelasSiswaPunyaSiswa.getKelasSiswa();

			Integer jamke = 0;

			final AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null, siswa, absenPiket,
					kelasSiswa.getAbsensi(), jamke);

			if (kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran().equals(Common.getCurrentTahunAkademik())) {

				if (siswa.getKelas() == null) {
					siswa.setKelas(kelasSiswaPunyaSiswa.getKelasSiswa());
					Common.refreshUpdate(siswa);
				}
			}
			CommonMedia.tampilkanGambarKecil(siswa).setParent(row);
			RevisiHelper.createNewRevisi(AbsenPiketDetail.class, absenPiketDetail, siswa.getNomorInduk())
					.setParent(row);

			new Label(siswa.getNama()).setParent(row);

			String ket = absenPiketDetail.retreiveAbsensiKeterangan(siswa.getId() + "_" + absenPiket.getId());
			ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

			Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					absenPiketDetail.retreiveAbsensiId(siswa.getId() + "_" + absenPiket.getId()));
			if (statusabsensi == null) {
				statusabsensi = Common.bolehKonfigurasi("absen_piket_otomatis_belum") ? ConstantValues.BELUM_ABSEN : ConstantValues.MASUK;
				absenPiketDetail.populate(siswa.getId() + "_" + absenPiket.getId(), statusabsensi, ket, "", "",
						"AbsenPiket");
				Common.refreshUpdate(absenPiketDetail);
			}
			final Textbox keterangan = new Textbox(ket);
			final Radiogroup kehadiran = new Radiogroup();

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (kehadiran.getSelectedItem() == null) {
						return;
					}

					if (absenPiketDetail.getId() != null) {
						HibernateUtil.currentSession().refresh(absenPiketDetail);
					}

					Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getAttribute("value");

					absenPiketDetail.populate(siswa.getId() + "_" + absenPiket.getId(), statusabsensi,
							keterangan.getValue(), "", "", "AbsenPiket");

					Common.refreshUpdate(absenPiketDetail);

					kelasSiswa.populate(siswa.getId() + "_" + absenPiket.getId(), statusabsensi, keterangan.getValue(),
							"", "", "AbsenPiket");
					Common.refreshUpdate(kelasSiswa);
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
		Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)

				.add(Restrictions.eq("kelasSiswa", absenPiket.getKelas()))

				.createAlias("siswa", "siswa")

				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("siswa.namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("siswa.nomorIndukNasional", nama.getValue().trim(),
												MatchMode.ANYWHERE))))
				.add(angkatan == null || angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("siswa.tahunMasuk", angkatan.getValue()));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (order) {
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.namaSiswa"))
					.addOrder(Order.desc("siswa.id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging100(initCriteria(false), paging);
		siswa = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE_100 * (paging == null ? 0 : paging.getActivePage())),
				KelasSiswaPunyaSiswa.class);

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	private String[] contents = new String[] { "siswa.nomorInduk", "siswa.nomorIndukNasional", "siswa.namaSiswa",
			"siswa.tahunMasuk", "siswa.sekolah.nama", "siswa.sekolah.yayasan" };

	private String[] contentsMhs = new String[] { "mahasiswa.nim", "mahasiswa.nama", "mahasiswa.tahunangkatan",
			"mahasiswa.jurusan.nama", "siswa.jurusan.fakultas.nama" };

	public void displayDetailPA(final AbsenPiket absenPiket, final Component component, final MyWindow window) {
		this.absenPiket = absenPiket;
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

											Integer jamke = 0;

											for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswa) {

												AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null,
														kelasSiswaPunyaSiswa.getSiswa(), absenPiket,
														kelasSiswaPunyaSiswa.getKelasSiswa().getAbsensi(), jamke);

												Statusabsensi statusabsensi = ConstantValues.MASUK;
												String ket = absenPiketDetail.retreiveAbsensiKeterangan(
														kelasSiswaPunyaSiswa.getSiswa().getId() + "_"
																+ absenPiket.getId());
												ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
												absenPiketDetail.populate(
														kelasSiswaPunyaSiswa.getSiswa().getId() + "_"
																+ absenPiket.getId(),
														statusabsensi, ket, "", "", "AbsenPiket");

												KelasSiswa kelasSiswa = kelasSiswaPunyaSiswa.getKelasSiswa();

												Session session = HibernateUtil.currentNativeSession();
												session.refresh(kelasSiswa);

												kelasSiswa.populate(
														kelasSiswaPunyaSiswa.getSiswa().getId() + "_"
																+ absenPiket.getId(),
														statusabsensi, ket, "", "", "AbsenPiket");

												session.getTransaction().begin();
												Common.refreshUpdate(session, kelasSiswa);
												Common.refreshUpdate(session, absenPiketDetail);
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
											Integer jamke = 0;
											for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswa) {

												AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null,
														kelasSiswaPunyaSiswa.getSiswa(), absenPiket,
														kelasSiswaPunyaSiswa.getKelasSiswa().getAbsensi(), jamke);

												Statusabsensi statusabsensi = ConstantValues.BELUM_ABSEN;
												String ket = absenPiketDetail.retreiveAbsensiKeterangan(
														kelasSiswaPunyaSiswa.getSiswa().getId() + "_"
																+ absenPiket.getId());
												ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
												absenPiketDetail.populate(
														kelasSiswaPunyaSiswa.getSiswa().getId() + "_"
																+ absenPiket.getId(),
														statusabsensi, ket, "", "", "AbsenPiket");

												KelasSiswa kelasSiswa = kelasSiswaPunyaSiswa.getKelasSiswa();

												Session session = HibernateUtil.currentNativeSession();
												session.refresh(kelasSiswa);

												kelasSiswa.populate(
														kelasSiswaPunyaSiswa.getSiswa().getId() + "_"
																+ absenPiket.getId(),
														statusabsensi, ket, "", "", "AbsenPiket");

												session.getTransaction().begin();
												Common.refreshUpdate(session, kelasSiswa);
												Common.refreshUpdate(session, absenPiketDetail);
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

				Integer jamke = 0;

				Object[] objects = (Object[]) arg0.getData();
				KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) objects[0];
				XSSFRow row = (XSSFRow) objects[2];

				AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null, kelasSiswaPunyaSiswa.getSiswa(),
						absenPiket, kelasSiswaPunyaSiswa.getKelasSiswa().getAbsensi(), jamke);

				Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();

				String ket = absenPiketDetail.retreiveAbsensiKeterangan(siswa.getId() + "_" + absenPiket.getId());
				ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

				Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
						absenPiketDetail.retreiveAbsensiId(siswa.getId() + "_" + absenPiket.getId()));
				if (statusabsensi == null) {
					statusabsensi = ConstantValues.BELUM_ABSEN;
				}

				row.createCell(contents.length).setCellValue(statusabsensi.getNama());
				row.createCell(contents.length + 1).setCellValue(ket);
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KelasSiswaPunyaSiswa.class, this,
				"Download Kehadiran", "/img/print.png", columnHeadersAdding, dataAdding, true, null, "Kehadiran",
				absenPiket.getKelas() == null ? contentsMhs : contents);
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
		column.setLabel("NIS");
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
