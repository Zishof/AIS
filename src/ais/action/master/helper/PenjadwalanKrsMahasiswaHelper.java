package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PertemuanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class PenjadwalanKrsMahasiswaHelper {

	private KrsMahasiswa krsMahasiswa;
	private MyGrid grid;
	private DataLoader dataLoader;

	private Date currDate;

	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		public PertemuanRenderer() {

		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) arg1;
			arg0.setAttribute("myValue", pertemuan);

			final Textbox catatan = new Textbox();
			catatan.setValue(pertemuan.getCatatan() == null ? "" : pertemuan.getCatatan());
			catatan.setParent(arg0);
			catatan.setWidth("90%");
			catatan.setRows(2);
			catatan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setCatatan(catatan.getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final MyDatebox tanggal = new MyDatebox();
			tanggal.setValue(pertemuan.getTanggal());
			tanggal.setWidth("90%");
			tanggal.setParent(arg0);
			tanggal.setReadonly(true);
			tanggal.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setTanggal(tanggal.getValue());
					pertemuan.setTanggalEdit(tanggal.getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			Hbox hbox = new Hbox();
			arg0.appendChild(hbox);

			final Timebox waktuMulai;
			hbox.appendChild(waktuMulai = new ais.ui.util.MyTimebox());
			waktuMulai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuMulai
						.setValue(pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanKrsMahasiswaHelper.java:107");

			}

			hbox.appendChild(new ais.ui.util.MyLabelConfig(" s.d "));
			final Timebox waktuSelesai;
			hbox.appendChild(waktuSelesai = new ais.ui.util.MyTimebox());
			waktuSelesai.setFormat(Common.timeFormat2.get().toPattern());
			try {
				waktuSelesai.setValue(
						pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
								: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenjadwalanKrsMahasiswaHelper.java:119");

			}

			waktuMulai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setWaktuMulai(
							waktuMulai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuMulai.getValue()));
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			waktuSelesai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setWaktuSelesai(
							waktuSelesai.getValue() == null ? "" : Common.timeFormat2.get().format(waktuSelesai.getValue()));
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final Combobox ujian = new Combobox();
			Common.insertCombo(ujian, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(ujian, pertemuan.getStatusPertemuan());
			ujian.setReadonly(true);
			arg0.appendChild(ujian);
			ujian.setWidth("90%");
			ujian.setReadonly(true);
			ujian.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setStatusPertemuan((StatusPertemuan) ujian.getSelectedItem().getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			currDate = pertemuan.getTanggal();

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!PenjadwalanHelper.checkBolehHapus(pertemuan)) {
						return;
					}
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											PertemuanDao pertemuanDao = DaoFactory.getInstance().getPertemuanDao();
											// pertemuanDao.beginTransaction();

											pertemuanDao.delete((pertemuan));
											// pertemuanDao.commitTransaction();
											krsMahasiswa.belum();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data jadwal pengisian KRS mahasiswa",
													e, new String[] {
															"Pastikan tidak ada data lain (mis. nilai, kehadiran) yang masih berelasi dengan data ini.",
															"Muat ulang (refresh) halaman ini lalu coba hapus kembali.",
															"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
													});
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}

	}

	@SuppressWarnings({})
	public boolean save() throws InterruptedException {

		Session session = HibernateUtil.currentSession();

		krsMahasiswa.reInitPertemuan(session);

		this.dataLoader.loadData(null);
		return true;
	}

	public void display(final KrsMahasiswa krsMahasiswa, final DataLoader dataLoader) {
		this.krsMahasiswa = krsMahasiswa;
		this.dataLoader = dataLoader;

		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(krsMahasiswa.infoSimple());

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Jadwal Konsultasi", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyFormRow row = new MyFormRow();
				row.setValign("top");
				Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
				rows.setParent(grid);
				row.setParent(rows);

				Date myDate = ais.ui.util.WaktuUtil.getDate();
				if (currDate != null) {
					Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
					myCalendar.setTime(currDate);
					myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
					currDate = myCalendar.getTime();
				} else {
					currDate = myDate;
				}

				Pertemuan pertemuan = new Pertemuan();

				pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
				pertemuan.setTanggal(currDate);
				pertemuan.setKrsMahasiswa(krsMahasiswa);
				pertemuan.setTopik("Konsultasi ke " + (grid.getRows().getChildren().size()));
				pertemuan.setWaktuMulai(Common.timeFormat2.get().format(currDate));
				pertemuan.setWaktuSelesai(Common.timeFormat2.get().format(currDate));

				// pertemuan.setMulai(currDate);
				// pertemuan.setSelesai(currDate);

				pertemuan.setKrsMahasiswa(krsMahasiswa);

				Session session = HibernateUtil.currentSession();

				session.save(pertemuan);

				onSearchDefault(event);

			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						krsMahasiswa.reInitPertemuan(HibernateUtil.currentSession());
						// FIX NPE: Event.<init> mensyaratkan nama event tidak null. arg0 bisa juga
						// null jika timer jatuh ke jalur fallback (lihat CommonTimerHelper.runFallback).
						onSearchDefault(new Event("onSearchDefault", arg0 == null ? null : arg0.getTarget(), true));
					}
				});
			}
		});

		PenjadwalanHelper.tampilTombolAturUlangWaktu(toolbar, null, null, null, null, null, krsMahasiswa, null, null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						krsMahasiswa.belum();
						onSearchDefault(arg0);
					}
				});

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "dosenTamu",
				"dosenTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };

		PenjadwalanHelper.tampilTombolDownload(toolbar, contents, null, null, null, null, null, krsMahasiswa, null,
				null);

		PenjadwalanHelper.tampilTombolHapus(toolbar, null, null, null, null, null, krsMahasiswa, null, null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						krsMahasiswa.belum();
						onSearchDefault(arg0);
					}
				});

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (save()) {
					window.detach();
				}
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan Konsultasi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onSearchDefault(Event event) {

		List<Pertemuan> pertemuan = krsMahasiswa.ambilPertemuanList(
				event != null && event.getData() instanceof Boolean ? (Boolean) event.getData() : false);
		ListModel strset = new SimpleListModel(pertemuan);
		grid.setRowRenderer(new PertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
